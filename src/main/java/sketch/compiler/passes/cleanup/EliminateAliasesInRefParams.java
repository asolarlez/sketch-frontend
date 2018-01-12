package sketch.compiler.passes.cleanup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.ExprArrayRange;
import sketch.compiler.ast.core.exprs.ExprField;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtExpr;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.stencilSK.VarReplacer;

public class EliminateAliasesInRefParams extends FEReplacer {

    TempVarGen varGen;
    List<Statement> after = new ArrayList<Statement>();

    public EliminateAliasesInRefParams(TempVarGen varGen) {
        this.varGen = varGen;
    }

    final ExprVar FromHeap = new ExprVar((FENode) null, "FROM_HEAP");

    ExprVar getBaseVar(Expression exp) {
        if (exp instanceof ExprVar) {
            return (ExprVar) exp;
        }
        if (exp instanceof ExprArrayRange) {
            return getBaseVar(((ExprArrayRange) exp).getAbsoluteBaseExpr());
        }
        if (exp instanceof ExprField) {
            return FromHeap;
        }
        return null;
    }

    public Object visitExprFunCall(ExprFunCall efc) {
        Function fun = nres.getFun(efc.getName());
        List<Expression> newParams = new ArrayList<Expression>(fun.getParams().size());
        Map<Expression, Integer> pset = new HashMap<Expression, Integer>();
        Iterator<Expression> eit = efc.getParams().iterator();
        for (Parameter p : fun.getParams()) {
            Expression actual = eit.next();

            {
                ExprVar ev = getBaseVar(actual);
                if (ev != null && pset.containsKey(ev)) {
                    pset.put(ev, pset.get(ev) + 1);
                } else {
                    pset.put(ev, 1);
                }
            }
        }
        boolean hasChanged = false;
        eit = efc.getParams().iterator();

        Map<String, Expression> repl = new HashMap<String, Expression>();
        VarReplacer vr = new VarReplacer(repl);

        for (Parameter p : fun.getParams()) {
            Expression actual = eit.next();
            repl.put(p.getName(), actual);
            if (p.isParameterOutput()) {
                ExprVar ev = getBaseVar(actual);
                if (ev == null) {
                    newParams.add(actual);
                    continue;
                }
                if (ev == FromHeap) {
                    addTemp(p, actual, newParams, vr);
                    hasChanged = true;
                    continue;
                }
                int cnt = pset.get(ev);
                if (cnt > 1) {
                    addTemp(p, actual, newParams, vr);
                    hasChanged = true;
                } else {
                    newParams.add(actual);
                }
            } else {
                newParams.add(actual);
            }
        }


        if (!hasChanged)
            addStatement(new StmtExpr(efc));
        else
            addStatement(new StmtExpr(new ExprFunCall(efc, efc.getName(), newParams, efc.getTypeParams())));
        addStatements(after);
        after.clear();
        return null;
    }

    void addTemp(Parameter p, Expression actual, List<Expression> newParams,
            VarReplacer vr)
    {
        String name = varGen.nextVar(p.getName());
        addStatement(new StmtVarDecl(actual, (Type) p.getType().accept(vr), name, actual));
        Expression ev = new ExprVar(actual, name);
        newParams.add(ev);
        after.add(new StmtAssign(actual, ev));
    }
}
