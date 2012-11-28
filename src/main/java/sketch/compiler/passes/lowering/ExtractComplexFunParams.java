package sketch.compiler.passes.lowering;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.ExprArrayRange;
import sketch.compiler.ast.core.exprs.ExprArrayRange.RangeLen;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtExpr;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.ast.core.typs.TypeStructRef;
import sketch.compiler.stencilSK.VarReplacer;

public class ExtractComplexFunParams extends SymbolTableVisitor {
    final TempVarGen varGen;

    public ExtractComplexFunParams(TempVarGen varGen) {
        super(null);
        this.varGen = varGen;
    }
    public Object visitExprFunCall(ExprFunCall exp) {
        Function fun = nres.getFun(exp.getName(), exp);
        List<Expression> args = new ArrayList<Expression>(fun.getParams().size());
        List<Expression> existingArgs = exp.getParams();
        boolean isUninterp = fun.isUninterp();
        List<Parameter> params = fun.getParams();

        List<Expression> tempVars = new ArrayList<Expression>();
        List<Statement> refAssigns = new ArrayList<Statement>();

        Map<String, Expression> pmap = new HashMap<String, Expression>();
        VarReplacer vrep = new VarReplacer(pmap);
        int i = 0;
        for (Parameter p : params) {
            int ptype = p.getPtype();
            Expression oldArg = null;
            Expression oriOldArg = null;
            {
                oldArg = (Expression) existingArgs.get(i);
                oriOldArg = oldArg;
            }
            Type t = getType(oldArg);
            if (t instanceof TypeArray && isUninterp && p.isParameterInput()) {
                TypeArray ta = (TypeArray) t;
                oldArg =
                        new ExprArrayRange(oldArg, oldArg, new RangeLen(
                                ExprConstInt.zero, ta.getLength()));
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
                    tt = tsf.addDefaultPkg(fun.getPkg(), nres);
                }
                Statement decl = new StmtVarDecl(exp, tt, tempVar, oldArg);
                ExprVar ev = new ExprVar(exp, tempVar);
                args.add(ev);
                pmap.put(p.getName(), oriOldArg);
                addStatement(decl);
                if (ptype == Parameter.OUT) {
                    tempVars.add(ev);
                }
                if (ptype == Parameter.REF) {
                    assert ev != null;
                    refAssigns.add(new StmtAssign(oriOldArg, ev));
                }
            }
            ++i;
        }

        ExprFunCall newcall = new ExprFunCall(exp, exp.getName(), args);
        addStatement(new StmtExpr(newcall));
        addStatements(refAssigns);
        return null;
    }

}
