package sketch.compiler.passes.cuda;

import static sketch.util.DebugOut.printWarning;

import java.util.Arrays;
import java.util.Vector;

import sketch.compiler.ast.core.FEContext;
import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.StreamSpec;
import sketch.compiler.ast.core.SymbolTable;
import sketch.compiler.ast.core.TempVarGen;
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
import sketch.util.cuda.CudaThreadBlockDim;
import sketch.util.exceptions.ExceptionAtNode;

/**
 * Generate functions which all threads will enter.
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
@CompilerPassDeps(runsBefore = {}, runsAfter = { SplitAssignFromVarDef.class })
public class GenerateAllOrSomeThreadsFunctions extends SymbolTableVisitor {
    protected CudaThreadBlockDim cudaBlockDim;
    protected Vector<Function> oldThreadFcns;
    protected Vector<Function> allThreadsFcns;
    protected Vector<Function> someThreadsFcns;
    protected Vector<String> specFcns;

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
        if (fcn.getSpecification() != null || specFcns.contains(fcn.getName())) {
            oldThreadFcns.add((Function) super.visitFunction(fcn));
        } else {
            allThreadsFcns.add((Function) new AllThreadsTransform(symtab).visitFunction(fcn));
            someThreadsFcns.add((Function) new SomeThreadsTransform(symtab).visitFunction(fcn));
        }
        return fcn;
    }

    public Function createThreadDeltaFcn(FENode ctx, String name, boolean value) {
        Parameter param =
                new Parameter(new TypeArray(CudaMemoryType.GLOBAL, TypePrimitive.bittype,
                        cudaBlockDim.all()), "arg");
        ExprBinary curr = null;
        for (int a = 0; a < cudaBlockDim.all(); a++) {
            final ExprArrayRange deref =
                    new ExprArrayRange(new ExprVar(ctx, param.getName()),
                            new ExprConstInt(a));
            ExprBinary next = new ExprBinary(deref, "==", new ExprConstBoolean(value));
            if (curr == null) {
                curr = next;
            } else {
                curr = new ExprBinary(curr, "&&", next);
            }
        }
        return Function.newStatic(ctx, name, TypePrimitive.bittype, Arrays.asList(param),
                null, new StmtReturn(ctx, curr));
    }

    @Override
    public Object visitStreamSpec(StreamSpec spec) {
        oldThreadFcns = new Vector<Function>();
        allThreadsFcns = new Vector<Function>();
        someThreadsFcns = new Vector<Function>();
        specFcns = new Vector<String>();
        for (Function f : spec.getFuncs()) {
            if (f.getSpecification() != null) {
                specFcns.add(f.getSpecification());
            }
        }

        spec = (StreamSpec) super.visitStreamSpec(spec);
        Vector<Function> allFcns = new Vector<Function>();
        allFcns.addAll(oldThreadFcns);
        allFcns.addAll(allThreadsFcns);
        allFcns.addAll(someThreadsFcns);
        allFcns.add(createThreadDeltaFcn(spec, "__threadAll", true));
        allFcns.add(createThreadDeltaFcn(spec, "__threadNone", false));

        for (Function f : allFcns) {
            assert f != null;
        }

        return new StreamSpec(spec, spec.getType(), spec.getStreamType(), spec.getName(),
                spec.getParams(), spec.getVars(), allFcns);
    }

    // [start] Anything here is for transforming the harness function
    @Override
    public Object visitExprFunCall(ExprFunCall exp) {
        return new ExprFunCall(exp, "allthreads_" + exp.getName(), exp.getParams());
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
            return new Function(func, func.getCls(), "allthreads_" + func.getName(),
                    func.getReturnType(), func.getParams(), func.getSpecification(),
                    func.getBody());
        }

        /**
         * Create a loop over all threads for simple functions Expects that stmts_ are
         * already in somethreads form.
         */
        @SuppressWarnings("unchecked")
        public Statement createThreadLoop(final Vector<Statement> stmts_) {
            Vector<Statement> stmts = (Vector<Statement>) stmts_.clone();
            final Statement ctx = stmts.get(0);
            ExprVar allExpr = new ExprVar(ctx, "ThreadIdx_All");
            StmtVarDecl xDecl =
                    new StmtVarDecl(ctx, TypePrimitive.inttype, "ThreadIdx_X",
                            cudaBlockDim.getXFromAll(allExpr));
            StmtVarDecl yDecl =
                    new StmtVarDecl(ctx, TypePrimitive.inttype, "ThreadIdx_Y",
                            cudaBlockDim.getYFromAll(allExpr));
            StmtVarDecl zDecl =
                    new StmtVarDecl(ctx, TypePrimitive.inttype, "ThreadIdx_Z",
                            cudaBlockDim.getZFromAll(allExpr));

            // load the symbol table, create assignments to x, y, and z from all
            stmts.insertElementAt(xDecl, 0);
            stmts.insertElementAt(yDecl, 0);
            stmts.insertElementAt(zDecl, 0);

            return new StmtFor("ThreadIdx_All", new ExprConstInt(cudaBlockDim.all()),
                    new StmtBlock(stmts));
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
            final Vector<Statement> statements = new Vector<Statement>();
            final Vector<Statement> threadLoopStmts = new Vector<Statement>();

            for (Statement stmt : block.getStmts()) {
                boolean containsAllThreadsElt = ContainsAllThreadsElt.run(stmt);

                if (stmt instanceof CudaSyncthreads) {
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

                    ExprFunCall allCond = new ExprFunCall(c, "__threadAll", vref);
                    ExprFunCall noneCond = new ExprFunCall(c, "__threadNone", vref);
                    final Statement allthreadsAlt =
                            (Statement) ((StmtIfThen) stmt).getAlt().accept(this);
                    final Statement allthreadsThen =
                            (Statement) ((StmtIfThen) stmt).getCons().accept(this);

                    StmtIfThen someThreadsCond =
                            new StmtIfThen(stmt, vref, ((StmtIfThen) stmt).getCons(),
                                    ((StmtIfThen) stmt).getAlt());
                    final Statement someThreadsLoop =
                            createThreadLoop(new Vector<Statement>(
                                    Arrays.asList(somethreads(someThreadsCond))));
                    StmtIfThen notAllLevel =
                            new StmtIfThen(stmt, noneCond, allthreadsAlt, someThreadsLoop);
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
                    ExprFunCall allCond = new ExprFunCall(c, "__threadAll", vref);
                    ExprFunCall noneCond = new ExprFunCall(c, "__threadNone", vref);
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
                } else if (containsAllThreadsElt) {
                    flushAndAdd(statements, threadLoopStmts,
                            (Statement) stmt.accept(this));
                    printWarning("Assuming that subtree transformers "
                            + "will take care of loop nest for", stmt.getClass());
                } else {
                    threadLoopStmts.add(somethreads(stmt));
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
            return new ExprFunCall(exp, "allthreads_" + exp.getName(), exp.getParams());
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
        @SuppressWarnings("deprecation")
        public SomeThreadsTransform(SymbolTable symtab) {
            super(symtab);

            // initialize symbol table
            for (String name : CudaThreadBlockDim.indexNames) {
                super.visitStmtVarDecl(new StmtVarDecl(new FEContext("---"),
                        TypePrimitive.inttype, name, null));
            }
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
                    new Function(func, func.getCls(), "somethreads_" + func.getName(),
                            func.getReturnType(), params, func.getSpecification(),
                            func.getBody());
            return super.visitFunction(f2);
        }

        @Override
        public Object visitCudaThreadIdx(CudaThreadIdx cudaThreadIdx) {
            return new ExprVar(cudaThreadIdx, "ThreadIdx_" +
                    cudaThreadIdx.getIndexName().toUpperCase());
        }

        @Override
        public Object visitExprFunCall(ExprFunCall exp) {
            Vector<Expression> nextArgs = new Vector<Expression>();
            for (String threadIndexName : CudaThreadBlockDim.indexNames) {
                nextArgs.add(new ExprVar(exp, threadIndexName));
            }
            nextArgs.addAll(exp.getParams());
            return new ExprFunCall(exp, "somethreads_" + exp.getName(), nextArgs);
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

    protected static class ContainsAllThreadsElt extends FEReplacer {
        boolean contains = false;

        @Override
        public Object visitExprFunCall(ExprFunCall exp) {
            contains = true;
            return exp;
        }

        @Override
        public Object visitStmtVarDecl(StmtVarDecl stmt) {
            contains = true;
            return stmt;
        }

        @Override
        public Object visitCudaSyncthreads(CudaSyncthreads stmt) {
            contains = true;
            return stmt;
        }

        public static boolean run(FENode n) {
            ContainsAllThreadsElt inst = new ContainsAllThreadsElt();
            n.accept(inst);
            return inst.contains;
        }
    }
}
