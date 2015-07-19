package sketch.compiler.passes.preprocessing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.passes.lowering.SymbolTableVisitor;
import sketch.compiler.stencilSK.VarReplacer;
import sketch.util.exceptions.ExceptionAtNode;

public class EliminateEmptyArrayLen extends SymbolTableVisitor {
    public EliminateEmptyArrayLen() {
        super(null);
    }

    public Object visitStmtVarDecl(StmtVarDecl stmt) {
        stmt = (StmtVarDecl) super.visitStmtVarDecl(stmt);
        boolean changed = false;
        List<Type> newTypes = new ArrayList<Type>();
        List<String> newNames = new ArrayList<String>();
        List<Expression> newInits = new ArrayList<Expression>();

        for (int i = 0; i < stmt.getNumVars(); i++) {
            Type t = stmt.getType(i);
            if (t.isArray() && ((TypeArray) t).getLength() == null) {
                Expression exp = stmt.getInit(i);
                Type newType = getType(exp);
                if (!newType.isArray()) {
                    throw new ExceptionAtNode(stmt, "Types mismatch");
                }
                TypeArray ta = (TypeArray) newType;
                if (exp instanceof ExprFunCall && !ta.getLength().isConstant()) {
                    ExprFunCall fc = (ExprFunCall) exp;
                    Map<String, Expression> varMap = new HashMap<String, Expression>();
                    Function fn = nres.getFun(fc.getName(), exp);
                    List<Expression> params = fc.getParams();

                    Iterator<Parameter> actParams = fn.getParams().iterator();
                    if (fn.getParams().size() > params.size()) {
                        int dif = fn.getParams().size() - params.size();
                        for (int k = 0; k < dif; ++k) {
                            Parameter par = actParams.next();
                        }
                    }

                    for (Expression p : params) {
                        Parameter act = actParams.next();
                        Type actType = act.getType();
                        Type pType = getType(p);
                        if (actType.isArray()) {
                            TypeArray arr = (TypeArray) actType;
                            if (arr.getLength() instanceof ExprVar) {
                                varMap.put(((ExprVar) arr.getLength()).getName(),
                                        ((TypeArray) pType).getLength());
                            }
                            
                        }
                        varMap.put(act.getName(), p);
                    }
                    VarReplacer vr = new VarReplacer(varMap);
                    newType = (Type) newType.accept(vr);
                }

                newTypes.add(newType);
                newNames.add(stmt.getName(i));
                newInits.add(stmt.getInit(i));
                changed = true;
            } else {
                newTypes.add(t);
                newNames.add(stmt.getName(i));
                newInits.add(stmt.getInit(i));
            }
        }

        if (changed) {
            return new StmtVarDecl(stmt, newTypes, newNames, newInits);
        } else {
            return stmt;
        }
    }

}
