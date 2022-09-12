package sketch.compiler.passes.preprocessing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.exprs.ExprADTHole;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.ExprNamedParam;
import sketch.compiler.ast.core.exprs.ExprNew;
import sketch.compiler.ast.core.exprs.ExprHole;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtReturn;
import sketch.compiler.ast.core.typs.StructDef;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.ast.core.typs.TypeStructRef;
import sketch.compiler.passes.lowering.SymbolTableVisitor;
import sketch.util.exceptions.ExceptionAtNode;

/*
 * Do relevant type inference for new ??() and GUC.
 */

public class TypeInferenceForADTHoles extends SymbolTableVisitor {
    TypeStructRef ts = null;
    Type returnType = null;

    public TypeInferenceForADTHoles() {
        super(null);
    }

    @Override
    public Object visitStmtAssign(StmtAssign s) {
        Type t = getType(s.getLHS());
        if (t.isStruct()) {
            ts = (TypeStructRef) t;
        }
        Expression right = s.getRHS();
        Expression newRight = right.doExpr(this);
        if (right != newRight) {
            return new StmtAssign(s, s.getLHS(), newRight);
        }
        return s;
    }

    @Override
    public Object visitExprFunCall(ExprFunCall exp) {
        TypeStructRef ori = ts;
        Function callee = nres.getFun(exp.getName());
        List<Expression> params = exp.getParams();
        for (int i = 0; i < params.size(); i++) {
            Expression actual = params.get(i);
            Parameter p = callee.getParams().get(i);
            Type t = p.getType();
            if (t.isStruct()) {
                ts = (TypeStructRef) t;
            }
            doExpression(actual);
        }
        ts = ori;
        return super.visitExprFunCall(exp);
    }

    @Override
    public Object visitExprADTHole(ExprADTHole exp) {
        if (exp.getName() == null) {
            exp.setName(ts.getName());
        }
        return exp;
    }

    @Override
    public Object visitStmtReturn(StmtReturn stmt) {
        if (returnType.isStruct()) {
            ts = (TypeStructRef) returnType;
        }
        return super.visitStmtReturn(stmt);
    }

    @Override
    public Object visitFunction(Function f) {
        Type prevReturnType = returnType;
        returnType = f.getReturnType();
        Object o = super.visitFunction(f);
        returnType = prevReturnType;
        return o;
    }

    @Override
    public Object visitExprNew(ExprNew exp){
        if (exp.isHole() && exp.getTypeToConstruct() == null){
            if(ts != null && ts.isStruct()){
                ExprHole star = new ExprHole(exp, 5, TypePrimitive.int32type);
                exp =
                        new ExprNew(exp.getContext(), ts, exp.getParams(), true);
                exp.setStar(star);
            }else{
                throw new ExceptionAtNode("Type must be of type struct", exp);
            }
        }
        
        Type nt = null;
        if (exp.getTypeToConstruct() != null) {
            nt = (Type) exp.getTypeToConstruct().accept(this);
        }
        StructDef str = null;
        {
            assert nt instanceof TypeStructRef;
            str = nres.getStruct(((TypeStructRef) nt).getName());
        }
        boolean changed = false;
        List<ExprNamedParam> enl =
                new ArrayList<ExprNamedParam>(exp.getParams().size());
        for (ExprNamedParam en : exp.getParams()) {
            Type type;
            if (exp.isHole()) {
                type  = getFieldsMap(str).get(en.getName());
            } else {
                type = str.getFieldTypMap().get(en.getName());
            }
            StructDef cur = str;
            while (type == null) {
                cur = nres.getStruct(cur.getParentName());
                type = cur.getFieldTypMap().get(en.getName());
            }
            if (type.isStruct()){
                ts = (TypeStructRef) type;
            }
            Expression old = en.getExpr();
            Expression rhs = doExpression(old);
            if (rhs != old) {
                enl.add(new ExprNamedParam(en, en.getName(), rhs, en.getVariantName()));
                changed = true;
            } else {
                enl.add(en);
            }
        }

        if (nt != exp.getTypeToConstruct() || changed) {
            if (!changed) {
                enl = exp.getParams();
            }
            return new ExprNew(exp, nt, enl, exp.isHole(), exp.getStar());
        } else {
            return exp;
        }
        
        
    }
    Map<String, Map<String, Type>> fTypesMap = new HashMap<String, Map<String, Type>>();
    private Map<String, Type> getFieldsMap(StructDef ts) {
        String strName = ts.getFullName();
        if (fTypesMap.containsKey(strName)) {
            return fTypesMap.get(strName);
        } else {
            Map<String, Type> fieldsMap = new HashMap<String, Type>();
            LinkedList<String> queue = new LinkedList<String>();
            queue.add(strName);
            while (!queue.isEmpty()) {
                String current = queue.removeFirst();
                StructDef curStruct = nres.getStruct(current);
                List<String> children = nres.getStructChildren(current);
                queue.addAll(children);
                for (Entry<String, Type> field : curStruct.getFieldTypMap()) {
                    String name = field.getKey();
                    Type type = field.getValue();
                    if (fieldsMap.containsKey(name) && !fieldsMap.get(name).equals(type)) {
                        //throw error
                        throw new ExceptionAtNode("Two fields with name = " + name +
                                " and different types. Rename one of them.", ts);
                    } else {
                        fieldsMap.put(name, type);
                    }
                }
            }
            fTypesMap.put(strName, fieldsMap);
            return fieldsMap;
        }
    }
}
