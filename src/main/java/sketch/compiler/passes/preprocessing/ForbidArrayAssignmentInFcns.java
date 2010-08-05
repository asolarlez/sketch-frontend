package sketch.compiler.passes.preprocessing;

import static sketch.util.DebugOut.printFailure;

import java.util.Collection;
import java.util.Vector;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.exprs.ExprArrayRange;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.passes.annotations.CompilerPassDeps;
import sketch.compiler.passes.structure.GetImplementedFcns;

@CompilerPassDeps(runsAfter = {}, runsBefore = {})
public class ForbidArrayAssignmentInFcns extends FEReplacer {
    protected Vector<String> inputParameters;
    protected Collection<String> specFcns;

    @Override
    public Object visitFunction(Function fcn) {
        if (fcn.isSketchHarness() || fcn.getSpecification() != null ||
                specFcns.contains(fcn.getName()) || !fcn.isStatic())
        {
            return fcn;
        } else {
            this.inputParameters = new Vector<String>();
            for (Parameter param : fcn.getParams()) {
                if (!param.isParameterOutput()) {
                    this.inputParameters.add(param.getName());
                }
            }
            return super.visitFunction(fcn);
        }
    }

    public Object visitStmtAssign(StmtAssign stmt) {
        if (stmt.getLHS() instanceof ExprArrayRange) {
            ExprArrayRange v = (ExprArrayRange)stmt.getLHS();
            if (v.getBase() instanceof ExprVar) {
                ExprVar v2 = (ExprVar) (v.getBase());
                if (inputParameters.contains(v2.getName())) {
                    printFailure("Assignments to input arrays are not allowed!",
                            "(except on generator or main/harness functions).");
                    printFailure("Error source:", stmt.getCx().toString());
                    System.exit(1);
                }
            }
        }
        return stmt;
    }

    @Override
    public Object visitProgram(Program prog) {
        final GetImplementedFcns if_ = new GetImplementedFcns();
        if_.visitProgram(prog);
        this.specFcns = if_.sketchToSpec.values();
        return super.visitProgram(prog);
    }
}
