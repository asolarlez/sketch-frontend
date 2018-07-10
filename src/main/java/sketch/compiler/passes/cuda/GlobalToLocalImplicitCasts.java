package sketch.compiler.passes.cuda;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import static sketch.util.DebugOut.assertFalse;

import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Package;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.SymbolTable;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.ExprArrayInit;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtBlock;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.cuda.typs.CudaMemoryType;
import sketch.compiler.main.cmdline.SketchOptions;
import sketch.compiler.passes.annotations.CompilerPassDeps;
import sketch.compiler.passes.cuda.GenerateAllOrSomeThreadsFunctions.AllThreadsTransform;
import sketch.compiler.passes.lowering.SymbolTableVisitor;
import sketch.compiler.passes.structure.CallGraph;
import sketch.util.cuda.CudaThreadBlockDim;
import sketch.util.datastructures.TypedHashSet;

/**
 * implicitly upcast e.g. "x : Int" to "{ x, x, x ... } : Int[NTHREADS]" for function
 * arguments
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
@CompilerPassDeps(runsBefore = {}, runsAfter = { GenerateAllOrSomeThreadsFunctions.class })
public class GlobalToLocalImplicitCasts extends SymbolTableVisitor {
    protected Package spec;
    protected CudaThreadBlockDim cudaBlockDim;
    protected CallGraph cg;
    protected final TempVarGen varGen;
    protected final SketchOptions options;

    public GlobalToLocalImplicitCasts(TempVarGen varGen, SketchOptions opts) {
        super(null);
        this.varGen = varGen;
        this.options = opts;
        this.cudaBlockDim = new CudaThreadBlockDim(2, 1, 1); // opts.cudaOpts.threadBlockDim;
    }

    @Override
    public Object visitProgram(Program prog) {
        cg = new CallGraph(prog);
        return super.visitProgram(prog);
    }

    @Override
    public Object visitPackage(Package spec) {
        super.visitPackage(spec);

        final CallReplacer cr = new CallReplacer(symtab);
        cr.setNres(nres);
        return cr.visitPackage(spec);
    }

    protected class CallReplacer extends SymbolTableVisitor {
        protected TypedHashSet<String> visitedFcns = new TypedHashSet<String>();

        protected Vector<Statement> statementsAfter = new Vector<Statement>();

        public CallReplacer(SymbolTable symtab) {
            super(symtab);
        }

        protected ExprArrayInit getImplicitInputParam(ExprFunCall exp, Expression e) {
            Vector<Expression> e_dupl = new Vector<Expression>();
            for (int a = 0; a < cudaBlockDim.all(); a++) {
                e_dupl.add(e);
            }
            final ExprArrayInit nextParam = new ExprArrayInit(exp, e_dupl);
            return nextParam;
        }

        @Override
        public Object visitExprFunCall(ExprFunCall exp) {
            this.visitedFcns.add(exp.getName());
            // for local expressions that aren't already vectors, copy them many times
            Vector<Expression> nextParams = new Vector<Expression>();
            Function funcSigParams = cg.getTarget(exp);
            Iterator<Parameter> iter = funcSigParams.getParams().iterator();
            for (Expression e : exp.getParams()) {
                Parameter fcnParam = iter.next();
                final CudaMemoryType mt = getType(e).getCudaMemType();
                if (mt != CudaMemoryType.LOCAL_TARR &&
                        fcnParam.getType().getCudaMemType() == CudaMemoryType.LOCAL_TARR)
                {
                    final Expression nextParam;
                    if (fcnParam.isParameterReference() || fcnParam.isParameterOutput()) {
                        assertFalse(
                                "reference/output parameters aren't supported for "
                                        + "<primitive> --> local <primitive> array transformations yet. "
                                        + "Currently, the model transforms primitives to an array"
                                        + "of the primitive, indexed by thread ID. Please refactor at",
                                exp.getCx(), "with regard to parameter",
                                fcnParam.getName());
                    } // else
                      // if (fcnParam.isParameterOutput()) {
                      // nextParam = getImplicitOutputParam(exp, e);
                      // } else {
                    nextParam = getImplicitInputParam(exp, e);
                    // }
                    nextParams.add(nextParam);
                } else if (mt == CudaMemoryType.LOCAL_TARR &&
                        fcnParam.getType().getCudaMemType().isLocalOrUndefined())
                {
                    if (fcnParam.isParameterOutput()) {
                        nextParams.add(getImplicitOutputParam(exp, e, fcnParam.getType()));
                    } else {
                        assertFalse(
                                "reference/output parameters aren't supported for "
                                        + "<primitive> --> local <primitive> array transformations yet. "
                                        + "Currently, the model transforms primitives to an array"
                                        + "of the primitive, indexed by thread ID. Please refactor at",
                                exp.getCx(), "with regard to parameter",
                                fcnParam.getName());
                    }
                } else {
                    nextParams.add(e);
                }
            }
            assert nextParams.size() == exp.getParams().size();

            return new ExprFunCall(exp, exp.getName(), nextParams, null);
        }

        protected Expression getImplicitOutputParam(ExprFunCall exp, Expression e,
                Type typ)
        {
            String tmpVarname = varGen.nextVar(exp.getName() + "_outparam");
            final StmtVarDecl tmpDecl =
                    new StmtVarDecl(e, typ.withMemType(CudaMemoryType.LOCAL), tmpVarname,
                            null);
            // register in symtab
            super.visitStmtVarDecl(tmpDecl);
            addStatement(tmpDecl);
            final GenerateAllOrSomeThreadsFunctions enclosing =
                    new GenerateAllOrSomeThreadsFunctions(options, varGen);
            AllThreadsTransform loopCode = enclosing.new AllThreadsTransform(symtab);
            Statement assignToAllLocals =
                    loopCode.createThreadLoop(new StmtAssign(exp, e, new ExprVar(e,
                            tmpVarname)));
            statementsAfter.add(assignToAllLocals);
            return new ExprVar(e, tmpVarname);
        }

        public Object visitStmtBlock(StmtBlock oldBlock) {
            List<Statement> oldStatements = newStatements;
            Vector<Statement> nextStatements = new Vector<Statement>();
            newStatements = new ArrayList<Statement>();
            assert statementsAfter.isEmpty();

            boolean changed = false;
            for (Statement s : oldBlock.getStmts()) {
                newStatements.add((Statement) s.accept(this));
                if (newStatements.size() != 1 || newStatements.get(0) != s ||
                        !statementsAfter.isEmpty())
                {
                    changed = true;
                }
                nextStatements.addAll(newStatements);
                nextStatements.addAll(statementsAfter);
                statementsAfter.clear();
                newStatements.clear();
            }
            newStatements = oldStatements;
            if (changed) {
                return new StmtBlock(oldBlock, nextStatements);
            } else {
                return oldBlock;
            }
        }
    }
}
