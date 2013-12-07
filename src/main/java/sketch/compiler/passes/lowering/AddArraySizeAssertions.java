package sketch.compiler.passes.lowering;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.exprs.ExprBinary;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.ExprNamedParam;
import sketch.compiler.ast.core.exprs.ExprNew;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.StmtAssert;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.StructDef;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.ast.core.typs.TypeStructRef;
import sketch.compiler.parallelEncoder.VarSetReplacer;
import sketch.util.exceptions.ExceptionAtNode;

public class AddArraySizeAssertions extends SymbolTableVisitor {

    public AddArraySizeAssertions() {
        super(null);
    }

    public void addCheck(Type l, Type r, boolean isUnivariant, FENode cx) {
        if (l instanceof TypeArray && r instanceof TypeArray) {
            TypeArray la = (TypeArray) l;
            TypeArray ra = ((TypeArray) r);
            Expression rlen = ra.getLength();
            Expression llen = la.getLength();
            Integer illen = llen.getIValue();
            Integer irlen = rlen.getIValue();
            if (illen == null || irlen == null) {
                if (la.getLength().equals(rlen)) {
                    return;
                }
                Expression e;
                if (isUnivariant) {
                    e = new ExprBinary(llen, "==", rlen);
                } else {
                    e = new ExprBinary(llen, ">=", rlen);
                }

                addStatement(new StmtAssert(e, "Array Length Mismatch " + cx.getCx(),
                        false));
                addCheck(la.getBase(), ra.getBase(), isUnivariant, cx);
            } else {
                if (isUnivariant) {
                    if (illen.intValue() != irlen.intValue()) {
                        throw new ExceptionAtNode(
                                "Array length mismatch. Remember reference parameters are univariant.",
                                cx);
                    }
                } else {
                    if (illen.intValue() < irlen.intValue()) {
                        throw new ExceptionAtNode("Array length mismatch. ", cx);
                    }
                }
            }
        }
    }

    void addArrLenCheck(FENode cxn, TypeArray t) {
        Expression len = t.getLength();
        Integer ilen = len.getIValue();
        if (ilen != null) {
            if (ilen < 0) {
                throw new ExceptionAtNode("Negative array lengths are not allowed: " +
                        len + "  ", cxn);
            }
        } else {
            Expression e = new ExprBinary(len, ">=", ExprConstInt.zero);
            addStatement(new StmtAssert(e, "Negative array lengths not allowed " + len +
                    "  " +
                    cxn.getCx(), false));
        }
        if (t.getBase() instanceof TypeArray) {
            addArrLenCheck(cxn, (TypeArray) t.getBase());
        }
    }

    public Object visitStmtVarDecl(StmtVarDecl svd) {
        for (int i = 0; i < svd.getNumVars(); ++i) {
            if (svd.getType(i) instanceof TypeArray) {
                addArrLenCheck(svd, (TypeArray) svd.getType(i));
            }

            if (svd.getInit(i) != null) {
                addCheck(svd.getType(i), getType(svd.getInit(i)), false, svd);
            }
        }
        return super.visitStmtVarDecl(svd);
    }

    public Object visitExprFunCall(ExprFunCall efc) {
        Function f = nres.getFun(efc.getName());
        Iterator<Expression> actuals = efc.getParams().iterator();
        Map<String, Expression> rmap = new HashMap<String, Expression>();
        VarSetReplacer vsr = new VarSetReplacer(rmap);
        for (Parameter p : f.getParams()) {
            Expression actual = actuals.next();
            addCheck((Type) p.getType().accept(vsr), getType(actual),
                    p.isParameterReference(), efc);
            rmap.put(p.getName(), actual);
        }
        return super.visitExprFunCall(efc);
    }

    public Object visitExprNew(ExprNew expNew) {
        TypeStructRef nt = (TypeStructRef) expNew.getTypeToConstruct();
        StructDef ts = nres.getStruct(nt.getName());
        Map<String, Expression> rmap = new HashMap<String, Expression>();
        VarSetReplacer vsr = new VarSetReplacer(rmap);
        for (ExprNamedParam en : expNew.getParams()) {
            rmap.put(en.getName(), doExpression(en.getExpr()));
        }
        for (ExprNamedParam en : expNew.getParams()) {
            StructDef current = ts;
            Type t = null;
            while (current != null) {
                t = current.getType(en.getName());
                if (t != null)
                    break;
                String parent;
                if ((parent = nres.getStructParentName(current.getName())) != null) {
                    current = nres.getStruct(parent);
                } else {
                    current = null;
                }
            }
            addCheck((Type) t.accept(vsr), getType(en.getExpr()), false, expNew);
        }

        return expNew;
    }

    public Object visitStmtAssign(StmtAssign sa) {
        Type l = getType(sa.getLHS());
        Type r = getType(sa.getRHS());
        addCheck(l, r, false, sa);
        return super.visitStmtAssign(sa);
    }
}
