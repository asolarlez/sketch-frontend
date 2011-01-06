package sketch.compiler.passes.cuda;

import static sketch.util.DebugOut.printWarning;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import sketch.compiler.ast.core.FEContext;
import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.StreamSpec;
import sketch.compiler.ast.core.SymbolTable;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.Function.FcnType;
import sketch.compiler.ast.core.exprs.ExprArrayRange;
import sketch.compiler.ast.core.exprs.ExprBinary;
import sketch.compiler.ast.core.exprs.ExprConstBoolean;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.*;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.ast.cuda.exprs.CudaThreadIdx;
import sketch.compiler.ast.cuda.stmts.CudaSyncthreads;
import sketch.compiler.ast.cuda.typs.CudaMemoryType;
import sketch.compiler.main.seq.SequentialSketchOptions;
import sketch.compiler.passes.annotations.CompilerPassDeps;
import sketch.compiler.passes.lowering.SymbolTableVisitor;
import sketch.compiler.passes.structure.ASTObjQuery;
import sketch.compiler.passes.structure.ASTQuery;
import sketch.util.cuda.CudaThreadBlockDim;
import sketch.util.datastructures.TypedHashSet;
import sketch.util.exceptions.ExceptionAtNode;
import sketch.util.fcns.CopyableIterator;

/**
 * Generate functions which all threads will enter.
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
@CompilerPassDeps(runsBefore = {}, runsAfter = { SplitAssignFromVarDef.class,
        FlattenStmtBlocks2.class })
public class GenerateAllOrSomeThreadsFunctions extends SymbolTableVisitor {
    protected CudaThreadBlockDim cudaBlockDim;
    protected CudaCallGraph cg;
    protected Vector<Function> oldThreadFcns;
    protected Vector<Function> allThreadsFcns;
    protected Vector<Function> someThreadsFcns;

    protected final TempVarGen varGen;

    public GenerateAllOrSomeThreadsFunctions(SequentialSketchOptions opts,
            TempVarGen varGen)
    {
        super(null);
        this.varGen = varGen;
        this.cudaBlockDim = opts.getCudaBlockDim();
    }

    @Override
    public Object visitFunction(Function fcn) {
        if (fcn.isParallel()) {
            allThreadsFcns.add((Function) new AllThreadsTransform(symtab).visitFunction(fcn));
            someThreadsFcns.add((Function) new SomeThreadsTransform(symtab).visitFunction(fcn));
        } else {
            oldThreadFcns.add((Function) super.visitFunction(fcn));
        }
        return fcn;
    }

    public Function createThreadDeltaFcn(FENode ctx, String name, boolean value) {
        Parameter param =
                new Parameter(new TypeArray(CudaMemoryType.GLOBAL, TypePrimitive.bittype,
                        cudaBlockDim.all()), "arg");
        final ExprVar arrvar = new ExprVar(ctx, param.getName());
        ExprBinary curr = getAllOrNoneExpr(value, arrvar);
        return Function.creator(ctx, name, FcnType.Static).returnType(
                TypePrimitive.bittype).params(Arrays.asList(param)).body(
                new StmtReturn(ctx, curr)).create();
    }

    protected ExprBinary getAllOrNoneExpr(boolean value, final ExprVar arrvar) {
        ExprBinary curr = null;
        for (int a = 0; a < cudaBlockDim.all(); a++) {
            final ExprArrayRange deref = new ExprArrayRange(arrvar, new ExprConstInt(a));
            ExprBinary next = new ExprBinary(deref, "==", new ExprConstBoolean(value));
            if (curr == null) {
                curr = next;
            } else {
                curr = new ExprBinary(curr, "&&", next);
            }
        }
        return curr;
    }

    @Override
    public Object visitProgram(Program prog) {
        cg = new CudaCallGraph(prog);
        return super.visitProgram(prog);
    }

    @Override
    public Object visitStreamSpec(StreamSpec spec) {
        oldThreadFcns = new Vector<Function>();
        allThreadsFcns = new Vector<Function>();
        someThreadsFcns = new Vector<Function>();

        spec = (StreamSpec) super.visitStreamSpec(spec);
        Vector<Function> allFcns = new Vector<Function>();
        allFcns.addAll(oldThreadFcns);
        allFcns.addAll(allThreadsFcns);
        allFcns.addAll(someThreadsFcns);

        for (Function f : allFcns) {
            assert f != null;
        }

        return spec.newFromFcns(allFcns);
    }

    // [start] Anything here is for transforming the harness function
    @Override
    public Object visitExprFunCall(ExprFunCall exp) {
        if (cg.getTarget(exp).isParallel()) {
            return new ExprFunCall(exp, "allthreads_" + exp.getName(), exp.getParams());
        } else {
            return exp;
        }
    }

    // [end]

    /**
     * All subtrees visited by this class are assumed to be nodes where all threads are
     * executing.
     */
    public class AllThreadsTransform extends SymbolTableVisitor {
        public AllThreadsTransform(SymbolTable symtab) {
            super(symtab);
        }

        @Override
        public Object visitStmtVarDecl(StmtVarDecl decl) {
            if (decl.getType(0).getCudaMemType() == CudaMemoryType.LOCAL) {
                assert decl.getTypes().size() == 1;
                final Type type = localArrayType(decl.getType(0));
                decl = new StmtVarDecl(decl, type, decl.getName(0), decl.getInit(0));
            }
            return super.visitStmtVarDecl(decl);
        }

        @Override
        public Object visitParameter(Parameter par) {
            if (par.getType().getCudaMemType() == CudaMemoryType.LOCAL) {
                final Type type = localArrayType(par.getType());
                par = new Parameter(type, par.getName(), par.getPtype());
            }
            return super.visitParameter(par);
        }

        private TypeArray localArrayType(Type base) {
            return new TypeArray(CudaMemoryType.LOCAL_TARR, base, cudaBlockDim.all());
        }

        @Override
        public Object visitFunction(Function fcn) {
            Function func = (Function) super.visitFunction(fcn);
            return func.creator().name("allthreads_" + func.getName()).create();
        }

        /**
         * Create a loop over all threads for simple functions Expects that stmts_ are
         * already in somethreads form.
         */
        @SuppressWarnings("unchecked")
        public Statement createThreadLoop(final Vector<Statement> stmts_) {
            final SomeThreadsTransform tf = new SomeThreadsTransform(symtab);
            Vector<Statement> stmts =
                    new Vector<Statement>(
                            tf.visitStmtList((Vector<Statement>) stmts_.clone()));
            final Statement ctx = stmts.get(0);
            ExprVar allExpr = new ExprVar(ctx, "ThreadIdx_All");

            // load the symbol table, create assignments to x, y, and z from all
            if (tf.usedThreadIndices.contains("X")) {
                stmts.add(0, new StmtVarDecl(ctx, TypePrimitive.inttype, "ThreadIdx_X",
                        cudaBlockDim.getXFromAll(allExpr)));
            }
            if (tf.usedThreadIndices.contains("Y")) {
                stmts.add(0, new StmtVarDecl(ctx, TypePrimitive.inttype, "ThreadIdx_Y",
                        cudaBlockDim.getYFromAll(allExpr)));
            }
            if (tf.usedThreadIndices.contains("Z")) {
                stmts.add(0, new StmtVarDecl(ctx, TypePrimitive.inttype, "ThreadIdx_Z",
                        cudaBlockDim.getZFromAll(allExpr)));
            }

            return new StmtFor("ThreadIdx_All", new ExprConstInt(cudaBlockDim.all()),
                    new StmtBlock(stmts));
        }

        public Statement createThreadLoop(Statement... stmts) {
            return createThreadLoop(new Vector<Statement>(Arrays.asList(stmts)));
        }

        /** should be closure visitStmtBlock */
        public void flushAndAdd(final Vector<Statement> statements,
                final Vector<Statement> stmtsWithoutFcnCall, Statement... toAdd)
        {
            if (!stmtsWithoutFcnCall.isEmpty()) {
                statements.add(createThreadLoop(stmtsWithoutFcnCall));
                stmtsWithoutFcnCall.clear();
            }
            for (Statement s : toAdd) {
                statements.add(s);
            }
        }

        final Type localBit = TypePrimitive.bittype.withMemType(CudaMemoryType.LOCAL);

        @Override
        public Object visitStmtBlock(StmtBlock block) {
            // final accumulator
            final Vector<Statement> statements = new Vector<Statement>();
            // somethreads logic that will be looped
            final Vector<Statement> threadLoopStmts = new Vector<Statement>();

            CopyableIterator<Statement> it =
                    new CopyableIterator<Statement>(block.getStmts());
            while (it.hasNext()) {
                Vector<Statement> nextSomethreadsStatments = new Vector<Statement>();
                Statement nextAllthreadsStatement = null;
                List<Statement> afterSomethreadsStatements = Collections.emptyList();

                // do a first pass -- queue up any statements that don't have sync's or
                // returns,
                // and don't call functions that have sync's
                while (it.hasNext()) {
                    List<Statement> t = it.peekAllNext();
                    Statement s = it.next();
                    if ((new ContainsAllthreadsElt()).run(s)) {
                        nextAllthreadsStatement = s;
                        afterSomethreadsStatements = t;
                        break;
                    } else {
                        nextSomethreadsStatments.add(s);
                    }
                }

                // find all referenced variables after ...
                TypedHashSet<String> varrefs =
                        (new GetVariableRefSet()).run(new StmtBlock(
                                afterSomethreadsStatements));

                // find all variables declared in somethreads section
                TypedHashSet<String> vardefs =
                        (new GetVariableDeclSet()).run(new StmtBlock(
                                nextSomethreadsStatments));

                // remove variable declarations referenced later
                TypedHashSet<String> referencedLater =
                        vardefs.intersect(varrefs.asCollection());
                if (!referencedLater.isEmpty()) {
                    Iterator<Statement> it2 = nextSomethreadsStatments.iterator();
                    while (it2.hasNext()) {
                        Statement s = it2.next();
                        if ((new ContainsVarDeclWithName(referencedLater)).run(s)) {
                            it2.remove();
                            // order doesn't really matter here so long as this is first
                            statements.add((Statement) s.accept(this));
                        }
                    }
                }

                // add remaining stuff to somethreads queue
                threadLoopStmts.addAll(nextSomethreadsStatments);

                // process the next allthreads statement
                Statement stmt = nextAllthreadsStatement;

                if (stmt == null) {
                    assert !it.hasNext();
                } else if (stmt instanceof CudaSyncthreads) {
                    flushAndAdd(statements, threadLoopStmts);
                } else if (stmt instanceof StmtIfThen) {
                    String newVarName = varGen.nextVar("cond");

                    // save to a variable, and transform it so it becomes thread local
                    StmtVarDecl cond_decl =
                            new StmtVarDecl(stmt, localBit, newVarName, null);
                    final Expression c = ((StmtIfThen) stmt).getCond();
                    final ExprVar vref = new ExprVar(stmt, newVarName);
                    StmtAssign cond_assn = new StmtAssign(vref, c);
                    StmtBlock condAsVar = new StmtBlock(cond_decl, cond_decl, cond_assn);
                    StmtBlock asBlock = (StmtBlock) visitStmtBlock(condAsVar);
                    flushAndAdd(statements, threadLoopStmts, asBlock.getStmts().toArray(
                            new Statement[0]));

                    ExprBinary allCond = getAllOrNoneExpr(true, vref);
                    ExprBinary noneCond = getAllOrNoneExpr(false, vref);
                    final Statement alt = ((StmtIfThen) stmt).getAlt();
                    final Statement allthreadsAlt =
                            (alt == null) ? null : (Statement) alt.accept(this);
                    final Statement allthreadsThen =
                            (Statement) ((StmtIfThen) stmt).getCons().accept(this);

                    StmtIfThen someThreadsCond =
                            new StmtIfThen(stmt, vref, ((StmtIfThen) stmt).getCons(),
                                    ((StmtIfThen) stmt).getAlt());
                    final Statement someThreadsLoop = createThreadLoop(someThreadsCond);
                    Statement notAllLevel =
                            (alt == null) ? someThreadsLoop : new StmtIfThen(stmt,
                                    noneCond, allthreadsAlt, someThreadsLoop);
                    StmtIfThen topLevel =
                            new StmtIfThen(stmt, allCond, allthreadsThen, notAllLevel);
                    flushAndAdd(statements, threadLoopStmts, topLevel);
                } else if (stmt instanceof StmtWhile) {
                    StmtWhile ws = (StmtWhile) stmt;
                    Expression c = ws.getCond();
                    String newVarName = varGen.nextVar("while_cond");

                    // save to a variable, and transform it so it becomes thread local
                    StmtVarDecl cond_decl =
                            new StmtVarDecl(stmt, localBit, newVarName, null);
                    final ExprVar vref = new ExprVar(stmt, newVarName);
                    StmtAssign cond_assn = new StmtAssign(vref, c);
                    StmtBlock condAsVar = new StmtBlock(cond_decl, cond_decl, cond_assn);
                    StmtBlock firstAsBlock = (StmtBlock) visitStmtBlock(condAsVar);
                    flushAndAdd(statements, threadLoopStmts,
                            firstAsBlock.getStmts().toArray(new Statement[0]));

                    // change the while statement so it recomputes the condition variables
                    ExprBinary allCond = getAllOrNoneExpr(true, vref);
                    ExprBinary noneCond = getAllOrNoneExpr(false, vref);
                    Vector<Statement> stmts =
                            new Vector<Statement>(((StmtBlock) ws.getBody()).getStmts());
                    stmts.add(cond_assn);

                    // iterate while all threads agree on c, and then check that no
                    // threads agree on c.
                    final StmtBlock nextBody =
                            (StmtBlock) visitStmtBlock(new StmtBlock(ws.getBody(), stmts));
                    StmtWhile next_ws = new StmtWhile(ws, allCond, nextBody);
                    StmtAssert none_at_end = new StmtAssert(noneCond, false);
                    flushAndAdd(statements, threadLoopStmts, next_ws, none_at_end);
                } else {
                    printWarning("Assuming that subtree transformers "
                            + "will take care of loop nest for", stmt.getClass());
                    flushAndAdd(statements, threadLoopStmts,
                            (Statement) stmt.accept(this));
                }
            }
            flushAndAdd(statements, threadLoopStmts);
            return new StmtBlock(statements);
        }

        protected Statement somethreads(Statement stmt) {
            return (Statement) stmt.accept(new SomeThreadsTransform(symtab));
        }

        @Override
        public Object visitExprFunCall(ExprFunCall exp) {
            Function callee = cg.getTarget(exp);
            switch (callee.getInfo().cudaType) {
                case DeviceInline:
                    return new ExprFunCall(exp, "allthreads_" + exp.getName(),
                            exp.getParams());
                case Serial:
                    return exp;
                case Global:
                    throw new ExceptionAtNode("Use \"device\" to designate "
                            + "CUDA subfunctions, not \"global\"", exp);
                default:
                    throw new ExceptionAtNode(
                            "Cannot call a non-cuda function from a CUDA function. "
                                    + "Use \"serial\" if you are intending to do this for code "
                                    + "that will disappear before CUDA code generation.",
                            exp);
            }
        }

        @Override
        public Object visitCudaThreadIdx(CudaThreadIdx cudaThreadIdx) {
            throw new ExceptionAtNode(
                    "Cuda thread index should be removed by AllThreadsTransform",
                    cudaThreadIdx);
        }

        @Override
        public Object visitCudaSyncthreads(CudaSyncthreads cudaSyncthreads) {
            return new StmtEmpty(cudaSyncthreads);
        }
    }

    public class SomeThreadsTransform extends SymbolTableVisitor {
        public final TypedHashSet<String> usedThreadIndices = new TypedHashSet<String>();

        @SuppressWarnings("deprecation")
        public SomeThreadsTransform(SymbolTable symtab) {
            super(symtab);

            // initialize symbol table
            for (String name : CudaThreadBlockDim.indexNames) {
                super.visitStmtVarDecl(new StmtVarDecl(new FEContext("---"),
                        TypePrimitive.inttype, name, null));
            }
        }

        /** visit a list of nodes / statements / etc. */
        public List<Statement> visitStmtList(List<Statement> lst) {
            return ((StmtBlock) (new StmtBlock(lst)).accept(this)).getStmts();
        }

        @Override
        public Object visitExprVar(ExprVar exp) {
            exp = (ExprVar) super.visitExprVar(exp);
            if (this.getType(exp).getCudaMemType() == CudaMemoryType.LOCAL_TARR) {
                return new ExprArrayRange(exp, new ExprVar(exp, "ThreadIdx_All"));
            } else {
                return exp;
            }
        }

        @Override
        public Object visitFunction(Function func) {
            Vector<Parameter> params = new Vector<Parameter>();
            for (String threadIndexName : CudaThreadBlockDim.indexNames) {
                params.add(new Parameter(TypePrimitive.inttype, threadIndexName,
                        Parameter.IN));
            }
            params.addAll(func.getParams());
            Function f2 =
                    func.creator().name("somethreads_" + func.getName()).params(params).create();
            return super.visitFunction(f2);
        }

        @Override
        public Object visitCudaThreadIdx(CudaThreadIdx cudaThreadIdx) {
            final String letter = cudaThreadIdx.getIndexName().toUpperCase();
            usedThreadIndices.add(letter);
            return new ExprVar(cudaThreadIdx, "ThreadIdx_" + letter);
        }

        @Override
        public Object visitExprFunCall(ExprFunCall exp) {
            Function target = cg.getTarget(exp);
            if (!target.isParallel()) {
                return new StmtAssert(new ExprConstBoolean(false), false);
            } else {
                usedThreadIndices.addAll(Arrays.asList("X", "Y", "Z"));
                Vector<Expression> nextArgs = new Vector<Expression>();
                for (String threadIndexName : CudaThreadBlockDim.indexNames) {
                    nextArgs.add(new ExprVar(exp, threadIndexName));
                }
                nextArgs.addAll(exp.getParams());
                return new ExprFunCall(exp, "somethreads_" + exp.getName(), nextArgs);
            }
        }

        @Override
        public Object visitCudaSyncthreads(CudaSyncthreads cudaSyncthreads) {
            return new StmtAssert(new ExprConstBoolean(false), false);
        }

        @Override
        public Object visitStmtIfThen(StmtIfThen stmt) {
            return super.visitStmtIfThen(stmt);
        }
    }

    protected class ContainsAllthreadsElt extends ASTQuery {
        @Override
        public Object visitExprFunCall(ExprFunCall exp) {
            if (!result & cg.callsFcnWithSyncthreads(exp)) {
                result = true;
            }
            return exp;
        }

        @Override
        public Object visitCudaSyncthreads(CudaSyncthreads stmt) {
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
            result |= !names.intersect(stmt.getNames()).isEmpty();
            return stmt;
        }
    }
}
