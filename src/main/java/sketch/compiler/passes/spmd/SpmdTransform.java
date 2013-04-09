package sketch.compiler.passes.spmd;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Vector;

import sketch.compiler.ast.core.FEContext;
import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Function.FunctionCreator;
import sketch.compiler.ast.core.Package;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.SymbolTable;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.ExprArrayRange;
import sketch.compiler.ast.core.exprs.ExprBinary;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.*;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.ast.cuda.typs.CudaMemoryType;
import sketch.compiler.ast.spmd.exprs.SpmdNProc;
import sketch.compiler.ast.spmd.exprs.SpmdPid;
import sketch.compiler.ast.spmd.stmts.SpmdBarrier;
import sketch.compiler.ast.spmd.stmts.StmtSpmdfork;
import sketch.compiler.main.cmdline.SketchOptions;
import sketch.compiler.passes.annotations.CompilerPassDeps;
import sketch.compiler.passes.lowering.EliminateComplexForLoops;
import sketch.compiler.passes.lowering.SymbolTableVisitor;
import sketch.compiler.passes.printers.SimpleCodePrinter;
import sketch.compiler.passes.structure.ASTObjQuery;
import sketch.compiler.passes.structure.ASTQuery;
import sketch.compiler.passes.structure.CallGraph;
import sketch.util.datastructures.TypedHashSet;
import sketch.util.fcns.CopyableIterator;

import static sketch.util.DebugOut.assertFalse;

@CompilerPassDeps(runsBefore = {}, runsAfter = { EliminateComplexForLoops.class })
public class SpmdTransform  extends SymbolTableVisitor {
    protected int SpmdMaxNProc;
    protected static final String SpmdNProc = "_spmdnproc";
    protected static final String SpmdPid = "_spmdpid";

    static final Type localBit = TypePrimitive.bittype.withMemType(CudaMemoryType.LOCAL);
    static final Type globalInt =
            TypePrimitive.inttype.withMemType(CudaMemoryType.GLOBAL);

    protected final TempVarGen varGen;
    protected SpmdCallGraph cg;

    Vector<Function> allProcFcns;
    Vector<Function> someProcFcns;
    Vector<Function> oldProcFcns;
    Map<String, Function> fcnToTransformed;

    public SpmdTransform(SketchOptions opts, TempVarGen varGen) {
        super(null);
        this.SpmdMaxNProc = opts.spmdOpts.MaxNProc;
        this.varGen = varGen;
    }

    public int getSpmdMaxNProc() { return SpmdMaxNProc; }

    @Override
    public Object visitFunction(Function fcn) {
//        System.out.println("here fcn=" + fcn + " all=" + cg.needAllProcFcn(fcn));
        if (fcnToTransformed.containsKey(fcn.getName())) {
            return fcn;
        }

        String spec = fcn.getSpecification();
        boolean hasSpec = spec != null;
        if (hasSpec && !fcnToTransformed.containsKey(spec)) {
            visitFunction(cg.getByName(spec));
        }

        if (cg.needAllProcFcn(fcn)) {
            AllProcTransform tf = new AllProcTransform(symtab);
            tf.setNres(nres);
            Function newfcn = (Function) tf.visitFunction(fcn);
            allProcFcns.add(newfcn);
            fcnToTransformed.put(fcn.getName(), newfcn);
        } else {
            Function oldfcn = (Function) super.visitFunction(fcn);
            if (cg.haveSomeProcFcn(fcn)) {
                SomeProcTransform tf = new SomeProcTransform(symtab);
                tf.setNres(nres);
                Function newfcn = (Function) tf.visitFunction(fcn);
                someProcFcns.add(newfcn);
                fcnToTransformed.put(fcn.getName(), newfcn);
                if (hasSpec) {
                    fcn = fcn.creator().spec(null).create();
                }
                if (hasSpec) {
                    oldfcn = oldfcn.creator().spec(null).create();
                }
            }
            oldProcFcns.add(oldfcn);
        }

        return fcn;
    }

    @Override
    public Object visitProgram(Program prog) {
        cg = new SpmdCallGraph(prog);
        return super.visitProgram(prog);
    }

    @Override
    public Object visitStreamSpec(Package spec) {
        oldProcFcns= new Vector<Function>();
        allProcFcns = new Vector<Function>();
        someProcFcns = new Vector<Function>();
        fcnToTransformed = new HashMap<String, Function>();

        spec = (Package) super.visitStreamSpec(spec);
        Vector<Function> allFcns = new Vector<Function>();
        allFcns.addAll(oldProcFcns);
        allFcns.addAll(allProcFcns);
        allFcns.addAll(someProcFcns);
//	System.out.println("allFcns: " + allFcns.toString());

        SimpleCodePrinter pr = new SimpleCodePrinter();
        pr.setNres(nres);

        System.out.println("before SpmdTransform:");
        spec.accept(pr);

        spec = spec.newFromFcns(allFcns);

        System.out.println("after SpmdTransform:");
        spec.accept(pr);
        return spec;
    }

    public Object visitExprFunCall(ExprFunCall exp) {
        Function target = cg.getTarget(exp);
//        System.out.println("here target=" + target);
        if (cg.isForkProcFcn(target)) {
            return new ExprFunCall(exp, "forkproc_" + exp.getName(), exp.getParams());
        } else {
            assert !cg.needAllProcFcn(target);
            return exp;
        }
    }

    TypeArray localArrayType(Type base) {
        return new TypeArray(CudaMemoryType.LOCAL_TARR, base, SpmdMaxNProc);
    }

    public class AllProcTransform extends SymbolTableVisitor {
        private final class NotTotallyGlobal extends ASTQuery {
            public Object visitExprVar(ExprVar var) {
                Type t = getType(var);
                if (t.getCudaMemType() != CudaMemoryType.GLOBAL) {
                    result = true;
                    return var;
                }
                return super.visitExprVar(var);
            }
        }

        boolean needAllProc;

        public AllProcTransform(SymbolTable symtab) {
            super(symtab);
        }

        @Override
        public Object visitSpmdPid(SpmdPid stmt) {
            ExprVar var = new ExprVar(stmt, SpmdPid);
            return var;
        }

        @Override
        public Object visitSpmdNProc(SpmdNProc stmt) {
            ExprVar var = new ExprVar(stmt, SpmdNProc);
            return var;
        }

        @Override
        public Object visitStmtVarDecl(StmtVarDecl decl) {
            if (needAllProc) {
                Type oldtyp = decl.getType(0);
                Type t = (Type) oldtyp.accept(this);
                if (t.getCudaMemType() != CudaMemoryType.GLOBAL) {
                    assert decl.getTypes().size() == 1;
                    final Type type = localArrayType(t);
                    decl = new StmtVarDecl(decl, type, decl.getName(0), decl.getInit(0));
                } else {
                    decl = new StmtVarDecl(decl, t, decl.getName(0), decl.getInit(0));
                }
            }
            return super.visitStmtVarDecl(decl);
        }

        @Override
        public Object visitFunction(Function fcn) {
            boolean forkProc = cg.isForkProcFcn(fcn);
            boolean oldNeedAllProc = needAllProc;
            needAllProc = !forkProc;
            String prefix = (forkProc ? "forkproc_":"allproc_");
            Function.FunctionCreator creator = fcn.creator().name(prefix + fcn.getName());
            if (needAllProc) {
                Vector<Parameter> params = new Vector<Parameter>();
                params.add(new Parameter(fcn, globalInt, SpmdNProc, Parameter.IN));
                for (Parameter p : fcn.getParams()) {
                    Type t = p.getType();
                    if (t.getCudaMemType() == CudaMemoryType.GLOBAL) {
                        params.add(p);
                    } else {
                        Type newt = localArrayType(t);
                        params.add(new Parameter(p, newt, p.getName(), p.getPtype()));
                    }
                }
                creator = creator.params(params);
            }
            Function f2 = creator.create();
            Function newfun = (Function) super.visitFunction(f2);

            needAllProc = oldNeedAllProc;
            return newfun;
        }

        @Override
        public Object visitStmtSpmdfork(StmtSpmdfork fork) {
            if (needAllProc) {
                assertFalse("cannot nest spmd fork! in ", fork.getCx());
                return null;
            }
            
            needAllProc = true;
            StmtVarDecl nProcDecl = new StmtVarDecl(fork, globalInt, SpmdNProc, null);
            super.visitStmtVarDecl(nProcDecl);
            final ExprVar vref = new ExprVar(fork, SpmdNProc);
            Statement nProcAssgn = (Statement)new StmtAssign(vref, fork.getNProc()).accept(this);
            ExprBinary cond = new ExprBinary(vref, "<=", new ExprConstInt(SpmdMaxNProc));
            StmtAssert assertion = new StmtAssert(fork, cond, false);
            Statement body = fork.getBody();
            //boolean oldNeedAllProc = needAllProc; we already know it's false
            StmtBlock block = (body instanceof StmtBlock) ? (StmtBlock)body : new StmtBlock(body);
            StmtBlock newBlock = new StmtBlock(fork.getCx(), nProcDecl, nProcAssgn, assertion, (Statement)this.visitStmtBlock(block));
            needAllProc = false;
            return newBlock;
        }

         /**
         * Create a loop over all processes for simple functions Expects that stmts are
         * already in somethreads form.
         */
        public Statement createProcLoop(final Vector<Statement> stmts) {
            final FEContext ctx = stmts.get(0).getCx();
            final SomeProcTransform tf = new SomeProcTransform(symtab);
            tf.setNres(nres);
            StmtBlock body = tf.visitStmtList((Vector<Statement>)stmts.clone());
            ExprVar nproc = new ExprVar(ctx, SpmdNProc);
            ExprVar pid = new ExprVar(ctx, SpmdPid);
            Vector<Statement> s = new Vector<Statement>();
            s.add(new StmtVarDecl(ctx, TypePrimitive.inttype, SpmdPid, null));
            for (int i = 0; i < SpmdMaxNProc; ++i) {
                Expression ei = new ExprConstInt(i);
                Vector<Statement> sl = new Vector<Statement>();
                sl.add(new StmtAssign(pid, ei));
                sl.addAll(body.getStmts());
                s.add(new StmtIfThen(ctx, new ExprBinary(nproc, ">", ei), new StmtBlock(
                        sl), null));
            }
            return new StmtBlock(s);
            // final ExprVar nProc = new ExprVar(ctx, SpmdNProc);
            // return new StmtFor(SpmdPid, nProc, body);
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

        protected Expression nProcLE(FENode context, int i) {
            return new ExprBinary(new ExprVar(context, SpmdNProc), "<=",
                    new ExprConstInt(i));
        }

        protected Expression underNProc(int i, Expression exp) {
            return new ExprBinary(nProcLE(exp, i), "||", exp);
        }

        protected Expression getAllOrNoneExpr(final Vector<Statement> addStmts,
                boolean value, final ExprVar arrvar, FENode ctx)
        {
            String name = varGen.nextVar("allCond");
            StmtVarDecl decl = new StmtVarDecl(ctx, TypePrimitive.bittype, name, null);
            ExprVar v = new ExprVar(ctx, name);
            Expression result =
                    new ExprBinary(new ExprArrayRange(arrvar, new ExprConstInt(0)), "==",
                            new ExprConstInt(
                            value ? 1 : 0));
            for (int i = 1; i < SpmdMaxNProc; ++i) {
                result =
                        new ExprBinary(result, "&&", underNProc(i, new ExprBinary(
                                new ExprArrayRange(arrvar, new ExprConstInt(i)), "==",
                                new ExprConstInt(value ? 1 : 0))));
            }
            Statement assgn = new StmtAssign(v, result);
            addStmts.add(decl);
            addStmts.add(assgn);
            return v;
        }

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
                CopyableIterator<Statement> prevIt = null;
                while (it.hasNext()) {
                    Vector<Statement> nextSomeProcStmts = new Vector<Statement>();

                    Statement nextAllProcStmt = null;
                    List<Statement> afterSomeProcStmts = Collections.emptyList();
                    // do a first pass -- queue up any statements that don't have all proc statements
                    // and don't call functions that have sync's
                    boolean firstStmt = true;
                    while (it.hasNext()) {
                        List<Statement> t = it.peekAllNext();
                        prevIt = it.clone();
                        Statement s = it.next();
                        // TODO xzl:
                        // two purposes:
                        // 1. when check for "StmtFor", need to run NotTotallyGlobal, so
                        // need to register var
                        // 2. (decided to disable this feature) used to provide an extra
                        // barrier when you forgot to put one for global assignment
                        // whenever you write to a global variable and only use globals,
                        // we use a pair of barriers to enclose this assignment
                        // and we only do the assignment once
                        // is this good or bad?
                        // example:
                        // global int x, y;
                        // fork { y = x+1; x = y+1; }
                        // will be break to y=x+1; x=y+1;
                        if (s instanceof StmtVarDecl) {
                            for (int i = 0; i < ((StmtVarDecl) s).getNumVars(); ++i) {
                                symtab.registerVar(((StmtVarDecl) s).getName(i),
                                        ((StmtVarDecl) s).getType(i), s,
                                        SymbolTable.KIND_LOCAL);
                            }
                        }
                        if ((new ContainsAllProcElt()).run(s)) {
                            nextAllProcStmt = s;
                            afterSomeProcStmts = t;
                            break;
                        }
                        if (firstStmt) {
                            firstStmt = false;
                        } else {
                            if ((new MustBeTheFirst()).run(s)) {
                                //System.out.println("s:" + s);
                                it = prevIt;
                                afterSomeProcStmts = it.peekAllNext();
                                break;
                            }
                        }
                        nextSomeProcStmts.add(s);
                    }

                    StmtBlock nextSomeProcBlock = new StmtBlock(nextSomeProcStmts);
                    TypedHashSet<String> vardefs =
                            (new GetVariableDeclSet()).run(nextSomeProcBlock);
                    if (!vardefs.isEmpty()) {
                        TypedHashSet<String> varused =
                                (new GetVariableRefSet()).run(new StmtBlock(afterSomeProcStmts));
                        // remove variable declarations referenced later
                        TypedHashSet<String> referencedLater = vardefs.intersect(varused);
                        if (!referencedLater.isEmpty()) {
                            Iterator<Statement> it2 = nextSomeProcStmts.iterator();
                            while (it2.hasNext()) {
                                Statement s = it2.next();
                                if ((new ContainsVarDeclWithName(referencedLater)).run(s))
                                {
                                    it2.remove();
                                    // order doesn't really matter here so long as the
                                    // declarations come first
                                    stmts.add((Statement) s.accept(this));
                                }
                            }
                        }
                    }

                    procLoopStmts.addAll(nextSomeProcStmts);

                    Statement stmt = nextAllProcStmt;
                    if (stmt == null) {
                        //assert !it.hasNext();
                        if (!procLoopStmts.isEmpty()) {
                            flushAndAdd(stmts, procLoopStmts);
                        }
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
                        flushAndAdd(stmts, procLoopStmts, cond_decl);
                        flushAndAdd(stmts, procLoopStmts, assignLoop);
                       
                        Vector<Statement> addStmts = new Vector<Statement>();
                        Expression allCond = getAllOrNoneExpr(addStmts, true, vref, stmt);
                        Expression noneCond =
                                getAllOrNoneExpr(addStmts, false, vref, stmt);
                        stmts.addAll(addStmts);

                        final Statement allProcThen = (Statement) ((StmtIfThen) stmt).getCons().accept(this);
                        
                        final Statement assertNone = new StmtAssert(stmt, noneCond, false);
                        final Statement alt = ((StmtIfThen) stmt).getAlt();
                        final Statement allProcElse = (alt == null) ? assertNone :
                          new StmtBlock(assertNone, (Statement) alt.accept(this));
                        
                        StmtIfThen topLevel = new StmtIfThen(stmt, allCond, allProcThen, allProcElse);
                        flushAndAdd(stmts, procLoopStmts, topLevel);
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

                        Vector<Statement> addStmts = new Vector<Statement>();
                        Expression allCond = getAllOrNoneExpr(addStmts, true, vref, stmt);
                        stmts.addAll(addStmts);

                        Vector<Statement> newBody =
                            new Vector<Statement>(((StmtBlock) ws.getBody()).getStmts());
                         // change the while statement so it recomputes the condition variables
                        newBody.add(cond_assn);
                        // iterate while all threads agree on c, and then check that no
                        // threads agree on c.
                        final StmtBlock nextBody = (StmtBlock) visitStmtBlock(new StmtBlock(ws.getBody(), newBody));
                        Vector<Statement> topBody =
                                new Vector<Statement>(nextBody.getStmts());
                        addStmts.remove(0); // FIXME: depend on the detail of
                                            // getAllOrNoneExpr: the first statement is
                                            // declaration
                        topBody.addAll(addStmts);
                        StmtWhile next_ws =
                                new StmtWhile(ws, allCond, new StmtBlock(ws.getBody(),
                                        topBody));
                        flushAndAdd(stmts, procLoopStmts, next_ws);

                        addStmts.clear();
                        Expression noneCond =
                                getAllOrNoneExpr(addStmts, false, vref, stmt);
                        stmts.addAll(addStmts);
                        StmtAssert none_at_end = new StmtAssert(FEContext.artificalFrom("allproc while loop", stmt), noneCond, false);
                        none_at_end.setMsg("All while conds must be false at the end");
                        flushAndAdd(stmts, procLoopStmts, none_at_end);
                    } else if (stmt instanceof StmtAssign) {
                        assert false : "the next allproc cannot be StmtAssign! " + stmt;
//                        printWarning("Assuming that subtree transformers "
//                                + "will take care of allthreads version of ",
//                                stmt.getClass());
                        // TODO xzl: this is used to provide an extra
                        // barrier when you forgot to put one for global assignment
                        // whenever you write to a global variable and only use globals,
                        // we use a pair of barriers to enclose this assignment
                        // and we only do the assignment once
                        // is this good or bad?
                        // example:
                        // global int x, y;
                        // fork { y = x+1; x = y+1; }
                        // will be break to y=x+1; x=y+1;
                        // boolean rhsLocal = (new NotTotallyGlobal()).run(((StmtAssign)
                        // stmt).getRHS());
                        // if (rhsLocal) {
                        // flushAndAdd(stmts, procLoopStmts);
                        // Vector<Statement> s = new Vector<Statement>(1);
                        // s.add((Statement) stmt.accept(this));
                        // flushAndAdd(stmts, s);
                        // } else {
                        // flushAndAdd(stmts, procLoopStmts, (Statement)
                        // stmt.accept(this));
                        // }
                    } else if (stmt instanceof StmtFor) {
                        StmtFor sf = (StmtFor) stmt;
                        StmtAssign init = (StmtAssign) sf.getInit();
                        ExprVar v = (ExprVar) init.getLHS();
                        assert getType(v).getCudaMemType() == CudaMemoryType.GLOBAL : "Not yet implemented: change the iterator " +
                                v + " to global type";
                        Expression low = init.getRHS();
                        Expression high = ((ExprBinary)sf.getCond()).getRight();
                        if (new NotTotallyGlobal().run(low) || new NotTotallyGlobal().run(high)) {
                            // TODO xzl: implement
                            assert false : "Not yet implemented: the for loop must be only use global vars as low/high, please check " +
                                    sf;
                        }
                        Statement body = sf.getBody();
                        Object nb = body.accept(this);
                        if (nb != body) {
                            // TODO xzl: check if this is correct. sf.isCanonical implies
                            // the new for loop canonical?
                            sf =
                                    new StmtFor(sf, sf.getInit(), sf.getCond(),
                                            sf.getIncr(), (Statement) nb,
                                            sf.isCanonical());
                        }
                        flushAndAdd(stmts, procLoopStmts, sf);
                    } else {
                        assert !(stmt instanceof StmtDoWhile) : "DoWhile not yet implemented in Spmd!";
                        flushAndAdd(stmts, procLoopStmts, (Statement) stmt.accept(this));
                    }
                    //System.out.println("stmts:" + stmts);
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
            exp = (ExprFunCall)super.visitExprFunCall(exp);
            Function target = cg.getTarget(exp);
            if (needAllProc) {
                assert !cg.isForkProcFcn(target);
                if (cg.needAllProcFcn(target)) {
                    Vector<Expression> nextArgs = new Vector<Expression>();
                    nextArgs.add(new ExprVar(exp, SpmdNProc));
                    nextArgs.addAll(exp.getParams());
                    return new ExprFunCall(exp, "allproc_" + exp.getName(), nextArgs);
                } else {
                    assertFalse("something wrong with spmd call graph in visitExprFunCall");
                    return null;
                }
            } else {
                if (cg.isForkProcFcn(target)) {
                    return new ExprFunCall(exp, "forkproc_" + exp.getName(), exp.getParams());
                } else {
                    assert !cg.needAllProcFcn(target);
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
        public StmtBlock visitStmtList(List<Statement> lst) {
            return (StmtBlock) (new StmtBlock(lst)).accept(this);
        }

        @Override
        public Object visitSpmdPid(SpmdPid stmt) {
            ExprVar var = new ExprVar(stmt, SpmdPid);
            return var;
        }
        
        @Override
        public Object visitSpmdNProc(SpmdNProc stmt) {
            ExprVar var = new ExprVar(stmt, SpmdNProc);
            return var;
        }

        @Override
        public Object visitExprVar(ExprVar exp) {
            exp = (ExprVar) super.visitExprVar(exp);
            if (this.getType(exp).getCudaMemType() == CudaMemoryType.LOCAL_TARR) {
                return new ExprArrayRange(exp, new ExprVar(exp, SpmdPid));
            } else {
                return exp;
            }
        }

        @Override
        public Object visitFunction(Function func) {
            Vector<Parameter> params = new Vector<Parameter>();
            params.add(new Parameter(func, globalInt, SpmdNProc, Parameter.IN));
            params.add(new Parameter(func, TypePrimitive.inttype, SpmdPid, Parameter.IN));
            params.addAll(func.getParams());
            FunctionCreator creator = func.creator().name("someproc_" + func.getName()).params(params);
            String spec = func.getSpecification();
            if (spec != null) {
                creator = creator.spec(fcnToTransformed.get(spec).getName());
            }
            Function f2 = creator.create();
            return super.visitFunction(f2);
        }

        @Override
        public Object visitExprFunCall(ExprFunCall exp) {
            exp = (ExprFunCall) super.visitExprFunCall(exp);
            Function target = cg.getTarget(exp);
            if (cg.needSomeProcFcn(target)) {
                assert !cg.isForkProcFcn(target);
                Vector<Expression> nextArgs = new Vector<Expression>();
                nextArgs.add(new ExprVar(exp, SpmdNProc));
                nextArgs.add(new ExprVar(exp, SpmdPid));
                nextArgs.addAll(exp.getParams());
                return new ExprFunCall(exp, "someproc_" + exp.getName(), nextArgs);
            } else {
                // assert target.isUninterp();
                return exp;
            }
        }
    }

    protected class MustBeTheFirst extends ASTQuery {
        @Override
        public Object visitStmtVarDecl(StmtVarDecl stmt) {
            if (stmt.getTypes().size() == 1) {
                Type t = stmt.getTypes().get(0);
                if (t instanceof TypeArray) {
                    result = true;
                }
            }
            return stmt;
        }
    }

    protected class ContainsAllProcElt extends ASTQuery {
        @Override
        public Object visitExprFunCall(ExprFunCall exp) {
            if (!result) result = cg.callsFcnWithBarrier(cg.getTarget(exp));
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
    protected final HashSet<Function> fcnsCallingBarrier = new HashSet<Function>();
    protected final TypedHashSet<Function> fcnsCalledByFork = new TypedHashSet<Function>();
    protected final TypedHashSet<Function> fcnsWithFork = new TypedHashSet<Function>();
    protected HashSet<Function> fcnsNeedAllProc;
    protected HashSet<Function> fcnsNeedSomeProc = new HashSet<Function>();
    protected HashSet<Function> fcnsHaveSomeProc = new HashSet<Function>();

    boolean insideFork = false;

    public SpmdCallGraph(Program prog) {
        super.init(prog);
    }

    public boolean isForkProcFcn(Function fcn) {
        return fcnsWithFork.contains(fcn);
    }

    public boolean callsFcnWithBarrier(Function fcn) {
        return fcnsCallingBarrier.contains(fcn);
    }

    public boolean needAllProcFcn(Function fcn) {
        return fcnsNeedAllProc.contains(fcn);
    }

    public boolean needSomeProcFcn(Function fcn) {
        return fcnsNeedSomeProc.contains(fcn);
    }

    public boolean haveSomeProcFcn(Function fcn) {
        return fcnsHaveSomeProc.contains(fcn);
    }

    @Override
    public Object visitSpmdBarrier(SpmdBarrier stmt) {
        assert enclosing != null;
        fcnsCallingBarrier.add(enclosing);
        return super.visitSpmdBarrier(stmt);
    }

    @Override
    public Object visitExprFunCall(ExprFunCall exp) {
        exp = (ExprFunCall) super.visitExprFunCall(exp);
        if (insideFork) {
            Function fun = getTarget(exp);
            fcnsCalledByFork.add(fun);
            // String spec = fun.getSpecification();
            // if (spec != null) {
            // fcnsCalledByFork.add(this.nres.getFun(spec));
            // }
        }
        return exp;
    }

    @Override
    public Object visitStmtSpmdfork(StmtSpmdfork stmt) {
//        System.out.println("in visitStmtSpmdFork");
        assert enclosing != null;
        assert !insideFork;
        insideFork = true;
//        System.out.println("withFork += " + enclosing);
        fcnsWithFork.add(enclosing);
        Object result = super.visitStmtSpmdfork(stmt);
        insideFork = false;
        return result;
    }

    @Override
    public Object visitSpmdPid(SpmdPid pid) {
        fcnsNeedSomeProc.add(enclosing);
        return pid;
    }

    @Override
    public Object visitSpmdNProc(SpmdNProc nproc) {
        fcnsNeedSomeProc.add(enclosing);
        return nproc;
    }

    @Override
    protected void buildEdges() {
        super.buildEdges();

        Queue<Function> qFork = new ArrayDeque<Function>();
        qFork.addAll(fcnsCalledByFork.asCollection());
        while (!qFork.isEmpty()) {
            Function f = qFork.remove();
            String spec = f.getSpecification();
            if (spec != null) {
                Function specfun = this.nres.getFun(spec);
                if (!fcnsCalledByFork.contains(specfun)) {
                    fcnsCalledByFork.add(specfun);
                    qFork.add(specfun);
                }
            }
            Function sketchfun = sketchOfSpec.get(f);
            if (sketchfun != null && !fcnsCalledByFork.contains(sketchfun)) {
                fcnsCalledByFork.add(sketchfun);
                qFork.add(sketchfun);
            }

            for (Function callee : closureEdges.targetsFrom(f)) {
                if (!callee.isUninterp() && !fcnsCalledByFork.contains(callee)) {
                    fcnsCalledByFork.add(callee);
                    qFork.add(callee);
                }
            }
        }

        Queue<Function> qBarrier = new ArrayDeque<Function>();
        qBarrier.addAll(fcnsCallingBarrier);
        while (!qBarrier.isEmpty()) {
            Function f = qBarrier.remove();
            for (Function caller : closureEdges.callersTo(f)) {
                if (fcnsCalledByFork.contains(caller) && !fcnsCallingBarrier.contains(caller)) {
                    fcnsCallingBarrier.add(caller);
                    qBarrier.add(caller);
                }
            }
        }
        fcnsNeedAllProc = fcnsWithFork.union(fcnsCallingBarrier).asHashSet();

        fcnsNeedSomeProc.removeAll(fcnsNeedAllProc);
        Queue<Function> qSome = new ArrayDeque<Function>();
        qSome.addAll(fcnsNeedSomeProc);
        while (!qSome.isEmpty()) {
            Function f = qSome.remove();
            if (!fcnsCalledByFork.contains(f)) {
                // assert fcnsCalledByFork.contains(f) : f.getName() +
                // " using pid/nproc but not called by fork";
                fcnsNeedSomeProc.remove(f);
                continue;
            }
            for (Function caller : closureEdges.callersTo(f)) {
                if (!fcnsNeedSomeProc.contains(caller) &&
                        !fcnsNeedAllProc.contains(caller))
                {
                    fcnsNeedSomeProc.add(caller);
                    qSome.add(caller);
                }
            }
        }
        // fcnsNeedSomeProc = fcnsCalledByFork.subtract(fcnsCallingBarrier).asHashSet();
        qSome.addAll(fcnsNeedSomeProc);
        fcnsHaveSomeProc.addAll(fcnsNeedSomeProc);
        while (!qSome.isEmpty()) {
            Function f = qSome.remove();
            String spec = f.getSpecification();
            if (spec != null) {
                Function g = getByName(spec);
                assert !g.isUninterp() : f.getName() + "is implementing an uninterp " + spec;
                if (!fcnsHaveSomeProc.contains(g)) {
                    fcnsHaveSomeProc.add(g);
                    qSome.add(g);
                }
            }
        }

/*
        System.out.println("withFork: " + fcnsWithFork);
        System.out.println("byFork: " + fcnsCalledByFork);
        System.out.println("callingBarrier: " + fcnsCallingBarrier);
        System.out.println("needAll: " + fcnsNeedAllProc);
        System.out.println("needSome: " + fcnsNeedSomeProc);
*/
    }
}

