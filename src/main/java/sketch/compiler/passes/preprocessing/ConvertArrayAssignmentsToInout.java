package sketch.compiler.passes.preprocessing;

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
import sketch.util.datastructures.TypedHashSet;

@CompilerPassDeps(runsAfter = {}, runsBefore = {})
public class ConvertArrayAssignmentsToInout extends FEReplacer {
    protected Vector<String> inputParameters;
    protected TypedHashSet<String> parametersToInout;
    protected Collection<String> specFcns;

    @Override
    public Object visitFunction(Function fcn) {
        if (fcn.isSketchHarness() || fcn.getSpecification() != null ||
                specFcns.contains(fcn.getName()) || !fcn.isStatic())
        {
            return fcn;
        } else {
            this.parametersToInout = new TypedHashSet<String>();
            this.inputParameters = new Vector<String>();
            for (Parameter param : fcn.getParams()) {
                if (!param.isParameterOutput()) {
                    this.inputParameters.add(param.getName());
                }
            }
            super.visitFunction(fcn);
            if (parametersToInout.isEmpty()) {
                return fcn;
            } else {
                Vector<Parameter> newParams = new Vector<Parameter>();
                for (Parameter param : fcn.getParams()) {
                    if (parametersToInout.contains(param.getName())) {
                        newParams.add(new Parameter(param.getType(), param.getName(),
                                Parameter.REF));
                    } else {
                        newParams.add(param);
                    }
                }
                return new Function(fcn, fcn.getCls(), fcn.getName(),
                        fcn.getReturnType(), newParams, fcn.getSpecification(),
                        fcn.getBody());
            }
        }
    }

    public Object visitStmtAssign(StmtAssign stmt) {
        if (stmt.getLHS() instanceof ExprArrayRange) {
            ExprArrayRange v = (ExprArrayRange) stmt.getLHS();
            if (v.getBase() instanceof ExprVar) {
                ExprVar v2 = (ExprVar) (v.getBase());
                if (inputParameters.contains(v2.getName())) {
                    parametersToInout.add(v2.getName());
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
