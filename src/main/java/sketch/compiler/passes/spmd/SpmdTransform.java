public class SpmdTransform  extends SymbolTableVisitor {
    public static final SpmdMaxNProc = 16;
    public static final String SpmdNProcVar = "spmdnproc";
    public static final String SpmdPidVar = "spmdpid";

    protected final TempVarGen varGen;
    protected SpmdCallGraph cg;

    public SpmdTransform(SketchOptions opts, TempVarGen varGen) {
        super(null);
        this.varGen = varGen;
    }

    @Override
    public Object visitFunction(Function fcn) {
        if (cg.needAllProcFcn(fcn)) {
            allProcFcns.add((Function) new AllProcTransform(symtab).visitFunction(fcn));
        } else {
            oldProcFcns.add((Function) super.visitFunction(fcn));
        }
        if (cg.needSomeProcFunction(fcn)) {
            someProcFcns.add((Function) new SomeProcTransform(symtab).visitFunction(fcn));
        }
        return fcn;
    }

    @Override
    public Object visitProgram(Program prog) {
        cg = new SpmdCallGraph(prog);
        return super.visitProgram(prog);
    }

    @Override
    public Object visitStreamSpec(StreamSpec spec) {
        oldProcFcns= new Vector<Function>();
        allProcFcns = new Vector<Function>();
        someProcFcns = new Vector<Function>();

        spec = (StreamSpec) super.visitStreamSpec(spec);
        Vector<Function> allFcns = new Vector<Function>();
        allFcns.addAll(oldProcFcns);
        allFcns.addAll(allProcFcns);
        allFcns.addAll(someProcFcns);
        return spec.newFromFcns(allFcns);
    }

    public Object visitExprFunCall(ExprFunCall exp) {
        if (cg.isForkProcFcn(exp)) {
            return new ExprFunCall(exp, "forkproc_" + exp.getName(), exp.getParams());
        } else {
            assert !cg.needAllProcFcn(exp);
            return exp;
        }
    }

    public class AllProcTransform extends SymbolTableVisitor {
        boolean needAllProc;

        public AllThreadsTransform(SymbolTable symtab) {
            super(symtab);
        }

        @Override
        public Object visitStmtVarDecl(StmtVarDecl decl) {
            if (needAllProc) {
                assert decl.getTypes().size() == 1;
                final Type type = localArrayType(decl.getType(0));
                decl = new StmtVarDecl(decl, type, decl.getName(0), decl.getInit(0));
            }
            return super.visitStmtVarDecl(decl);
        }

        @Override
        public Object visitParameter(Parameter par) {
            if (needAllProc) {
                final Type type = localArrayType(par.getType());
                par = new Parameter(type, par.getName(), par.getPtype());
            }
            return super.visitParameter(par);
        }

        private TypeArray localArrayType(Type base) {
            return new TypeArray(CudaMemoryType.LOCAL_TARR, base, SpmdMaxNProc);
        }

        @Override
        public Object visitFunction(Function fcn) {
            boolean forkProc = cg.isForkProcFcn(fcn);
            boolean oldNeedAllProc = needAllProc;
            needAllProc = !forkProc;
            Function func = (Function) super.visitFunction(fcn);
            needAllProc = oldNeedAllProc;
            return func.creator().name((forkProc ? "forkproc_":"allproc_") + func.getName()).create();
        }

        @Override
        public Object visitStmtSpmdfork(StmtSpmdfork fork) {
            if (needAllProc) {
                assertFalse("cannot nest spmd fork! in ", fork.getCx());
                return null;
            }
            
            needAllProc = true;
            StmtVarDecl nProcDecl = new StmtVarDecl(fork, TypePrimitive.inttype, SpmdNProcVar, null);
            super.visitStmtVarDecl(nProcDecl);
            final ExprVar vref = new ExprVar(stmt, SpmdNProcVar);
            Statement nProcAssgn = (Statement)new StmtAssign(vref, fork.getNProc()).accept(this);
            Statement body = fork.getBody();
            //boolean oldNeedAllProc = needAllProc; we already know it's false
            StmtBlock block = (body instanceof StmtBlock) ? body : new StmtBlock(body);
            StmtBlock newBlock = new StmtBlock(fork.getCx(), nProcDecl, nProcAssgn, this.visitStmtBlock(block));
            needAllProc = false;
            return newBlock;
        }

         /**
         * Create a loop over all processes for simple functions Expects that stmts are
         * already in somethreads form.
         */
        @SuppressWarnings("unchecked")
        public Statement createProcLoop(final Vector<Statement> stmts) {
            final SomeThreadsTransform tf = new SomeProcTransform(symtab);
            StmtBlock body = tf.visitStmtList((Vector<Statement>) stmts_.clone());
            final Context ctx = stmts.get(0).getCx();
            final ExprVar nProc = new ExprVar(ctx, SpmdNProcVar);
 
            return new StmtFor(ctx, SpmdPidVar, nProc, body);
        }

        public void flushAndAdd(final Vector<Statement> statements,
                final Vector<Statement> stmtsWithoutFcnCall, Statement... toAdd)
        {
            if (!stmtsWithoutFcnCall.isEmpty()) {
                statements.add(createProcLoop(stmtsWithoutFcnCall));
                stmtsWithoutFcnCall.clear();
            }
            for (Statement s : toAdd) {
                statements.add(s);
            }
        }

        final Type localBit = TypePrimitive.bittype;
        
        @SuppressWarnings("deprecation")
        @Override
        public Object visitStmtBlock(StmtBlock block) {
            if (needAllProc) {
                SymbolTable oldSymTab = symtab;
                symtab = new SymbolTable(symtab);              
                // final accumulator
                final Vector<Statement> stmts = new Vector<Statement>();
                final Vector<Statement> procLoopStmts = new Vector<Statement>();
                CopyableIterator<Statement> it =
                    new CopyableIterator<Statement>(block.getStmts());
                while (it.hasNext()) {
                    Vector<Statement> nextSomeProcStmts = new Vector<Statement>();

                    Satement nextAllProcStmt = null;
                    List<Statement> afterSomeProcStmts = Collections.emptyList();
                    // do a first pass -- queue up any statements that don't have all proc statements
                    // and don't call functions that have sync's
                    while (it.hasNext()) {
                        List<Statement> t = it.peekAllNext();
                        Statement s = it.next();
                        if ((new ContainsAllProcElt()).run(s)) {
                            nextAllProcStmt = s;
                            afterSomeProcStmts = t;
                            break;
                        } else {
                            nextSomeProcStmts.add(s);
                        }
                    }

                    TypedHashSet<String> varrefs = (new GetVariableRefSet()).run(new StmtBlock(afterSomeProcStmts));
                    TypedHashSet<String> vardefs = (new GetVariableDeclSet()).run(new StmtBlock(nextSomeProcStmts));
                    // remove variable declarations referenced later
                    TypedHashSet<String> referencedLater = vardefs.intersect(varrefs);
                    if (!referencedLater.isEmpty()) {
                        Iterator<Statement> it2 = nextSomeProcStmts.iterator();
                        while (it2.hasNext()) {
                            Statement s = it2.next();
                            if ((new ContainsVarDeclWithName(referencedLater)).run(s)) {
                                it2.remove();
                                // order doesn't really matter here so long as the
                                // declarations come first
                                stmts.add((Statement) s.accept(this));
                            }
                        }
                    }

                    procLoopStmts.addAll(nextSomeProcStmts);

                    Statement stmt = nextAllProcStmt;
                    if (stmt == null) {
                        assert !it.hasNext();
                    } else if (stmt instanceof SpmdBarrier) {
                        flushAndAdd(stmts, procLoopStmts);
                    } else if (stmt instanceof StmtIfThen) {
                        String newVarName = varGen.nextVar("cond");
                        // save to a variable, and transform it so it becomes thread local
                        StmtVarDecl cond_decl =
                            (StmtVarDecl) (new StmtVarDecl(stmt, localBit, newVarName, null)).accept(this);
                        final Expression c = ((StmtIfThen) stmt).getCond();
                        final ExprVar vref = new ExprVar(stmt, newVarName);
                        StmtAssign cond_assn = new StmtAssign(vref, c);
                        Statement[] assignLoop = visitStatementsAsBlock(cond_assn);
                        flushAndAdd(statements, threadLoopStmts, cond_decl);
                        flushAndAdd(statements, threadLoopStmts, assignLoop);
                        
                        ExprBinary allCond = getAllOrNoneExpr(true, vref);
                        ExprBinary noneCond = getAllOrNoneExpr(false, vref);
                        final Statement allProcThen = (Statement) ((StmtIfThen) stmt).getCons().accept(this);
                        
                        final Statement assertNone = new StmtAssert(stmt, noneCond, false);
                        final Statement alt = ((StmtIfThen) stmt).getAlt();
                        final Statement allProcElse = (alt == null) ? assertNone :
                          new StmtBlock(assertNone, (Statement) alt.accept(this);
                        
                        StmtIfThen topLevel = new StmtIfThen(stmt, allCond, allProcThen, allProcElse);
                        flushAndAdd(statements, procLoopStmts, topLevel);
                    } else if (stmt instanceof StmtWhile) {
                        StmtWhile ws = (StmtWhile) stmt;
                        Expression c = ws.getCond();
                        String newVarName = varGen.nextVar("while_cond");

                        // save to a variable, and transform it so it becomes thread local
                        StmtVarDecl cond_decl = (StmtVarDecl) (new StmtVarDecl(stmt, localBit, newVarName, null)).accept(this);
                        final ExprVar vref = new ExprVar(stmt, newVarName);
                        StmtAssign cond_assn = new StmtAssign(vref, c);
                        Statement[] assignLoop = visitStatementsAsBlock(cond_assn);
                        flushAndAdd(stmts, procLoopStmts, cond_decl);
                        flushAndAdd(stmts, procLoopStmts, assignLoop);

                        // change the while statement so it recomputes the condition variables
                        ExprBinary allCond = getAllOrNoneExpr(true, vref);
                        ExprBinary noneCond = getAllOrNoneExpr(false, vref);
                        Vector<Statement> newBody =
                            new Vector<Statement>(((StmtBlock) ws.getBody()).getStmts());
                        newBody.add(cond_assn);

                        // iterate while all threads agree on c, and then check that no
                        // threads agree on c.
                        final StmtBlock nextBody = (StmtBlock) visitStmtBlock(new StmtBlock(ws.getBody(), newBody));
                        StmtWhile next_ws = new StmtWhile(ws, allCond, nextBody);
                        StmtAssert none_at_end = new StmtAssert(FEContext.artificalFrom("allproc while loop", stmt), noneCond, false);
                        flushAndAdd(statements, procLoopStmts, next_ws, none_at_end);
                    } else {
                        printWarning("Assuming that subtree transformers "
                                + "will take care of allthreads version of ",
                                stmt.getClass());
                        flushAndAdd(stmts, procLoopStmts, (Statement)stmt.accept(this));
                    }
                }
                flushAndAdd(stmts, procLoopStmts);
                symtab = oldSymTab;
                return new StmtBlock(stmts);
            } else {
                return super.visitStmtBlock(block);
            }
        }

        @Override
        public Object visitExprFunCall(ExprFunCall exp) {
            if (needAllProc) {
                assert !cg.isForkProcFcn(exp);
                if (cg.needAllProcFcn(exp)) {
                    return new ExprFunCall(exp, "allproc_" + exp.getName(), exp.getParams());
                } else {
                    AssertFalse("something wrong with spmd call graph in visitExprFunCall");
                    return null;
                }
            } else {
                if (cg.isForkProcFcn(exp)) {
                    return new ExprFunCall(exp, "forkproc_" + exp.getName(), exp.getParams());
                } else {
                    assert !cg.needAllProcFcn(exp);
                    return exp;
                }
            }
        }
    }

    public class SomeProcTransform extends SymbolTableVisitor {
        public SomeProcTransform(SymbolTable symtab) {
            super(symtab);
        }

        /** visit a list of nodes / statements / etc. */
        public List<Statement> visitStmtList(List<Statement> lst) {
            return (StmtBlock) (new StmtBlock(lst)).accept(this);
        }
        
        @Override
        public Object visitExprVar(ExprVar exp) {
            exp = (ExprVar) super.visitExprVar(exp);
            if (this.getType(exp).getCudaMemType() == CudaMemoryType.LOCAL_TARR) {
                return new ExprArrayRange(exp, new ExprVar(exp, SpmdPidVar));
            } else {
                return exp;
            }
        }

        @Override
        public Object visitFunction(Function func) {
            Vector<Parameter> params = new Vector<Parameter>();
            params.add(new Parameter(TypePrimitive.inttype, SpmdNProcVar, Parameter.IN));
            params.add(new Parameter(TypePrimitive.inttype, SpmdPidVar, Parameter.IN));
            params.addAll(func.getParams());
            Function f2 = func.creator().name("somethreads_" + func.getName()).params(params).create();
            return super.visitFunction(f2);
        }

        @Override
        public Object visitExprFunCall(ExprFunCall exp) {
            exp = (ExprFunCall) super.visitExprFunCall(exp);
            assert cg.needSomeProcFcn(exp) && !cg.isForkProcFcn(exp);
            Vector<Expression> nextArgs = new Vector<Expression>();
            nextArgs.add(new ExprVar(exp, SpmdNProcVar));
            nextArgs.add(new ExprVar(exp, SpmdPidVar));
            nextArgs.addAll(exp.getParams());
            return new ExprFunCall(exp, "somethreads_" + exp.getName(), nextArgs);
        }
    }

    protected class ContainsAllProcElt extends ASTQuery {
        @Override
        public Object visitExprFunCall(ExprFunCall exp) {
            if (!result) result = cg.callsFcnWithBarrier(exp);
            return exp;
        }

        @Override
        public Object visitSpmdBarrier(SpmdBarrier stmt) {
            result = true;
            return stmt;
        }

        @Override
        public Object visitStmtReturn(StmtReturn stmt) {
            result = true;
            return stmt;
        }
    }

    protected static class GetVariableDeclSet extends ASTObjQuery<TypedHashSet<String>> {
        public GetVariableDeclSet() {
            super(new TypedHashSet<String>());
        }

        @Override
        public Object visitStmtVarDecl(StmtVarDecl stmt) {
            result.addAll(stmt.getNames());
            return stmt;
        }
    }

    protected static class GetVariableRefSet extends ASTObjQuery<TypedHashSet<String>> {
        public GetVariableRefSet() {
            super(new TypedHashSet<String>());
        }

        @Override
        public Object visitExprVar(ExprVar exp) {
            result.add(exp.getName());
            return exp;
        }
    }

    protected static class ContainsVarDeclWithName extends ASTQuery {
        protected final TypedHashSet<String> names;

        public ContainsVarDeclWithName(TypedHashSet<String> names) {
            this.names = names;
        }

        @Override
        public Object visitStmtVarDecl(StmtVarDecl stmt) {
            if (!result) result = !names.intersect(stmt.getNames()).isEmpty();
            return stmt;
        }
    }
}

class SpmdCallGraph extends CallGraph {
    protected final TypedHashSet<Function> fcnsCallingBarrier = new TypedHashSet<Function>();
    protected final TypedHashSet<Function> fcnsCalledByFork = new TypedHashSet<Function>();
    protected final TypedHashSet<Function> fcnsWithFork = new TypedHashSet<Function>();
    protected final TypedHashSet<Function> fcnsNeedAllProc;
    protected final TypedHashSet<Function> fcnsNeedSomeProc;

    boolean insideFork = false;

    public SpmdCallGraph(Program prog) {
        super(prog);
    }

    public isForkProcFcn(Function fcn) {
        return fcnsWithFork.contains(fcn);
    }

    public needAllProcFcn(Function fcn) {
        return fcnsNeedAllProc.contains(fcn);
    }

    public needSomeProcFcn(Function fcn) {
        return fcnsNeedSomeProc.contains(fcn);
    }

    @Override
    public Object visitSpmdBarrier(SpmdBarrier stmt) {
        assert enclosing != null;
        fcnsCallingBarrier.add(enclosing);
        return super.visitSpmdBarrier(stmt);
    }

    @Override
    public Object visitExprFunCall(ExprFunCall exp) {
        if (insideFork) {
            fcnsCalledByFork.add(getTarget(exp));
        }
    }

    @Override
    public Object visitStmtSpmdFork(StmtSpmdFork stmt) {
        assert enclosing != null;
        assert !insideFork;
        insideFork = true;
        fcnsWithFork.add(enclosing);
        Object result = super.visitStmtSpmdFork();
        insideFork = false;
        return result;
    }

    @Override
    protected void buildEdges() {
        super.buildEdges();

        Queue<Function> qFork = new Queue<Function>();
        qFork.addAll(fcnsCalledByFork);
        while (!qFork.isEmpty()) {
            Function f = qFork.remove();
            for (Function callee : closureEdges.targetsFrom(f)) {
                if (!fcnsCalledByFork.contains(callee)) {
                    fcnsCalledByFork.add(callee);
                    qFork.add(callee);
                }
            }
        }

        Queue<Function> qBarrier = new Queue<Function>();
        qBarrier.addAll(fcnsCallingBarrier);
        while (!qBarrier.isEmpty()) {
            Function f = qBarrier.remove();
            for (Function caller : closureEdges.callersTo(f)) {
                if (fcnsCalledByFork.contains(caller) && !fcnsCallingBarrier.contains(caller)) {
                    qBarrier.add(caller);
                }
            }
        }

        fcnsNeedAllProc = fcnsWithFork.union(fcnsCallingBarrier);
        fcnsNeedSomeProc = fcnsCalledByFork.subtract(fcnsCallingBarrier);
    }
}

