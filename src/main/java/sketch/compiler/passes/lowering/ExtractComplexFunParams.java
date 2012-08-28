package sketch.compiler.passes.lowering;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtExpr;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeStructRef;
import sketch.compiler.stencilSK.VarReplacer;

public class ExtractComplexFunParams extends FEReplacer {
    final TempVarGen varGen;

    public ExtractComplexFunParams(TempVarGen varGen) {
        this.varGen = varGen;
    }
    public Object visitExprFunCall(ExprFunCall exp) {
        Function fun = nres.getFun(exp.getName(), exp);
        List<Expression> args = new ArrayList<Expression>(fun.getParams().size());
        List<Expression> existingArgs = exp.getParams();

        List<Parameter> params = fun.getParams();

        List<Expression> tempVars = new ArrayList<Expression>();
        List<Statement> refAssigns = new ArrayList<Statement>();

        Map<String, Expression> pmap = new HashMap<String, Expression>();
        VarReplacer vrep = new VarReplacer(pmap);

        for (int i = 0; i < params.size(); i++) {
            Parameter p = params.get(i);
            int ptype = p.getPtype();
            Expression oldArg = null; // (p.getType() instanceof TypeStruct || p.getType()
                                      // instanceof TypeStructRef) ? new ExprNullPtr() :
                                      // getDefaultValue(p.getType()) ;
            {
                oldArg = (Expression) existingArgs.get(i);
            }

            if (oldArg != null && oldArg instanceof ExprVar ||
                    (oldArg instanceof ExprConstInt && !p.isParameterOutput()))
            {
                args.add(oldArg);
                pmap.put(p.getName(), oldArg);
            } else {
                String tempVar = varGen.nextVar(p.getName());
                Type tt = (Type) p.getType().accept(vrep);
                if (tt instanceof TypeStructRef) {
                    TypeStructRef tsf = (TypeStructRef) tt;
                    tt = tsf.addDefaultPkg(fun.getPkg());
                }
                Statement decl = new StmtVarDecl(exp, tt, tempVar, oldArg);
                ExprVar ev = new ExprVar(exp, tempVar);
                args.add(ev);
                pmap.put(p.getName(), oldArg);
                addStatement(decl);
                if (ptype == Parameter.OUT) {
                    tempVars.add(ev);
                }
                if (ptype == Parameter.REF) {
                    assert ev != null;
                    refAssigns.add(new StmtAssign(oldArg, ev));
                }
            }
        }

        ExprFunCall newcall = new ExprFunCall(exp, exp.getName(), args);
        addStatement(new StmtExpr(newcall));
        addStatements(refAssigns);
        return null;
    }

}
