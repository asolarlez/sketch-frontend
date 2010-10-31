package sketch.compiler.passes.cuda;

import static sketch.util.DebugOut.printFailure;
import static sketch.util.DebugOut.printWarning;

import java.util.Vector;

import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.StreamSpec;
import sketch.compiler.ast.core.SymbolTable;
import sketch.compiler.ast.core.exprs.ExprArrayRange;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtBlock;
import sketch.compiler.ast.core.stmts.StmtFor;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
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

/**
 * Generate functions which all threads will enter.
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
@CompilerPassDeps(runsBefore = {}, runsAfter = {}, debug = true)
public class GenerateAllOrSomeThreadsFunctions extends SymbolTableVisitor {
    protected CudaThreadBlockDim cudaBlockDim;
    protected Vector<Function> oldThreadFcns;
    protected Vector<Function> allThreadsFcns;
    protected Vector<Function> someThreadsFcns;

    public GenerateAllOrSomeThreadsFunctions(SequentialSketchOptions opts) {
        super(null);
        this.cudaBlockDim = opts.getCudaBlockDim();
    }

    // @Override
    // public Object visitProgram(Program prog) {
    // prog = (Program) (new AllThreadsTransform(symtab)).visitProgram(prog);
    // return prog;
    // }

    @Override
    public Object visitFunction(Function fcn) {
        if (fcn.getSpecification() != null) {
            oldThreadFcns.add((Function) super.visitFunction(fcn));
        } else {
            allThreadsFcns.add((Function) new AllThreadsTransform(symtab).visitFunction(fcn));
            someThreadsFcns.add((Function) new SomeThreadsTransform(symtab).visitFunction(fcn));
        }
        return fcn;
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
            decl = (StmtVarDecl) super.visitStmtVarDecl(decl);
            if (decl.getType(0).getCudaMemType() == CudaMemoryType.LOCAL) {
                assert decl.getTypes().size() == 0;
                final Type type = localArrayType(decl.getType(0));
                return new StmtVarDecl(decl, type, decl.getName(0), decl.getInit(0));
            }
            return decl;
        }

        @Override
        public Object visitParameter(Parameter par) {
            par = (Parameter) super.visitParameter(par);
            if (par.getType().getCudaMemType() == CudaMemoryType.LOCAL) {
                final Type type = localArrayType(par.getType());
                return new Parameter(type, par.getName(), par.getPtype());
            } else {
                return par;
            }
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

        /** Create a loop over all threads for simple functions */
        public Statement createThreadLoop(Vector<Statement> stmts) {
            ExprVar allExpr = new ExprVar(stmts.get(0), "ThreadIdx_All");
            StmtVarDecl xDecl =
                    new StmtVarDecl(stmts.get(0), TypePrimitive.inttype, "ThreadIdx_X",
                            cudaBlockDim.getXFromAll(allExpr));
            StmtVarDecl yDecl =
                    new StmtVarDecl(stmts.get(0), TypePrimitive.inttype, "ThreadIdx_Y",
                            cudaBlockDim.getYFromAll(allExpr));
            StmtVarDecl zDecl =
                    new StmtVarDecl(stmts.get(0), TypePrimitive.inttype, "ThreadIdx_Z",
                            cudaBlockDim.getZFromAll(allExpr));
            stmts.insertElementAt(xDecl, 0);
            stmts.insertElementAt(yDecl, 1);
            stmts.insertElementAt(zDecl, 2);
            return new StmtFor("ThreadIdx_All", new ExprConstInt(cudaBlockDim.all()),
                    new StmtBlock(stmts));
        }

        @Override
        public Object visitStmtBlock(StmtBlock block) {
            Vector<Statement> statements = new Vector<Statement>();
            Vector<Statement> stmtsWithoutFcnCall = new Vector<Statement>();

            for (Statement stmt : block.getStmts()) {
                boolean containsFcn = ContainsFcnCallOrVarDef.run(stmt);
                if (containsFcn) {
                    // flush simple statement buffer
                    if (!stmtsWithoutFcnCall.isEmpty()) {
                        statements.add(createThreadLoop(stmtsWithoutFcnCall));
                        stmtsWithoutFcnCall = new Vector<Statement>();
                    }

                    printWarning("Assuming that subtree transformers "
                            + "will take care of loop nest for", stmt.getClass());
                    statements.add((Statement) stmt.accept(this));
                } else if (stmt instanceof CudaSyncthreads) {
                    printFailure("don't know what to do with syncthreads yet.");
                } else {
                    stmtsWithoutFcnCall.add((Statement) stmt.accept(new SomeThreadsTransform(
                            symtab)));
                }
            }
            if (!stmtsWithoutFcnCall.isEmpty()) {
                statements.add(createThreadLoop(stmtsWithoutFcnCall));
            }

            return new StmtBlock(statements);
        }

        @Override
        public Object visitExprFunCall(ExprFunCall exp) {
            return new ExprFunCall(exp, "allthreads_" + exp.getName(), exp.getParams());
        }
    }

    public class SomeThreadsTransform extends SymbolTableVisitor {
        public SomeThreadsTransform(SymbolTable symtab) {
            super(symtab);
        }

        @Override
        public Object visitExprVar(ExprVar exp) {
            exp = (ExprVar) super.visitExprVar(exp);
            if (this.getType(exp).getCudaMemType() == CudaMemoryType.LOCAL) {
                return new ExprArrayRange(exp, new ExprVar(exp, "ThreadIdx_All"));
            } else {
                return exp;
            }
        }

        @Override
        public Object visitFunction(Function func) {
            Vector<Parameter> params = new Vector<Parameter>(func.getParams());
            params.insertElementAt(new Parameter(TypePrimitive.inttype, "ThreadIdx_X",
                    Parameter.IN), 0);
            params.insertElementAt(new Parameter(TypePrimitive.inttype, "ThreadIdx_Y",
                    Parameter.IN), 1);
            params.insertElementAt(new Parameter(TypePrimitive.inttype, "ThreadIdx_Z",
                    Parameter.IN), 2);
            params.insertElementAt(new Parameter(TypePrimitive.inttype, "ThreadIdx_All",
                    Parameter.IN), 3);
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
    }

    protected static class ContainsFcnCallOrVarDef extends FEReplacer {
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

        public static boolean run(FENode n) {
            ContainsFcnCallOrVarDef inst = new ContainsFcnCallOrVarDef();
            n.accept(inst);
            return inst.contains;
        }
    }
}
