package sketch.compiler.passes.spmd;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Package;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.SymbolTable;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.ExprArrayInit;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtBlock;
import sketch.compiler.ast.cuda.typs.CudaMemoryType;
import sketch.compiler.ast.spmd.exprs.SpmdPid;
import sketch.compiler.passes.annotations.CompilerPassDeps;
import sketch.compiler.passes.lowering.SymbolTableVisitor;
import sketch.compiler.passes.printers.SimpleCodePrinter;
import sketch.compiler.passes.structure.CallGraph;

import static sketch.util.DebugOut.assertFalse;

@CompilerPassDeps(runsBefore = {}, runsAfter = { SpmdTransform.class })
public class GlobalToLocalCasts extends SymbolTableVisitor {
    int SpmdMaxNProc;
    CallGraph cg;
    TempVarGen varGen;
    public GlobalToLocalCasts(TempVarGen varGen, SpmdTransform spmdTransform) {
        super(null);
        this.varGen = varGen;
        this.SpmdMaxNProc = spmdTransform.getSpmdMaxNProc();
    }

    @Override
    public Object visitProgram(Program prog) {
        cg = new CallGraph(prog);
        return super.visitProgram(prog);
    }

     @Override
    public Object visitStreamSpec(Package spec) {
        super.visitStreamSpec(spec);
        
        final SimpleCodePrinter pr1 = new SimpleCodePrinter();
        pr1.setNres(nres);
        System.out.println("before global to local casts:");
        spec.accept(pr1);

        final CallReplacer cr = new CallReplacer(symtab);
        cr.setNres(nres);
        Package result = (Package) cr.visitStreamSpec(spec);
        
        System.out.println("after global to local casts:");
        result.accept(pr1);
        
        return result;
    }

    protected class CallReplacer extends SymbolTableVisitor {
        //protected HashSet<String> visitedFcns = new HashSet<String>();

        protected Vector<Statement> statementsAfter = new Vector<Statement>();

        public CallReplacer(SymbolTable symtab) {
            super(symtab);
        }

        protected ExprArrayInit getImplicitInputParam(ExprFunCall exp, Expression e) {
            Vector<Expression> e_dupl = new Vector<Expression>();
            if (e instanceof SpmdPid) {
                for (int i = 0; i < SpmdMaxNProc; ++i) {
                    e_dupl.add(new ExprConstInt(i));
                }
            } else {
                for (int i = 0; i < SpmdMaxNProc; ++i) {
                    e_dupl.add(e);
                }
            }
            final ExprArrayInit nextParam = new ExprArrayInit(exp, e_dupl);
            return nextParam;
        }

        @Override
        public Object visitExprFunCall(ExprFunCall exp) {
            //this.visitedFcns.add(exp.getName());
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
                    if (fcnParam.isParameterReference() || fcnParam.isParameterOutput()) {
                        assertFalse(
                                "reference/output parameters aren't supported for "
                                        + "<primitive> --> local <primitive> array transformations yet. "
                                        + "Currently, the model transforms primitives to an array"
                                        + "of the primitive, indexed by thread ID. Please refactor at",
                                exp.getCx(), "with regard to parameter",
                                fcnParam.getName());
                    }
                    final Expression nextParam = getImplicitInputParam(exp, e);
                    nextParams.add(nextParam);
                } else if (mt == CudaMemoryType.LOCAL_TARR &&
                        fcnParam.getType().getCudaMemType().isLocalOrUndefined()) {
                        assertFalse("call param is LOCAL_TARR, but formal is not",
                                exp.getCx(), "with regard to parameter",
                                fcnParam.getName());
  /*                    if (fcnParam.isParameterOutput()) {
                        nextParams.add(getImplicitOutputParam(exp, e, fcnParam.getType()));
                    } else {
                        assertFalse(
                                "reference/output parameters aren't supported for "
                                        + "<primitive> --> local <primitive> array transformations yet. "
                                        + "Currently, the model transforms primitives to an array"
                                        + "of the primitive, indexed by thread ID. Please refactor at",
                                exp.getCx(), "with regard to parameter",
                                fcnParam.getName());
                    }*/
                } else {
                    nextParams.add(e);
                }
            }
            assert nextParams.size() == exp.getParams().size();

            return new ExprFunCall(exp, exp.getName(), nextParams);
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
