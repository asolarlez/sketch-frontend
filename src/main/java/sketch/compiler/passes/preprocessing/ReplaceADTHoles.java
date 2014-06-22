package sketch.compiler.passes.preprocessing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.exprs.ExprNamedParam;
import sketch.compiler.ast.core.exprs.ExprNew;
import sketch.compiler.ast.core.exprs.ExprStar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.typs.StructDef;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.ast.core.typs.TypeStructRef;
import sketch.compiler.passes.lowering.SymbolTableVisitor;
import sketch.util.exceptions.ExceptionAtNode;

public class ReplaceADTHoles extends SymbolTableVisitor{
    private final int DEPTH = 5;
    public ReplaceADTHoles(){
        super(null);
    }

    @Override
    public Object visitExprStar(ExprStar exp){
        Type t = exp.getType();
        if(t.isStruct()) {
            TypeStructRef ts = (TypeStructRef) t;
            return getExprNew(exp, ts, DEPTH);

        }
        return exp;
    }
    private ExprNew getExprNew(FENode exp, TypeStructRef ts, int depth ){
        if (depth == 0) {
            return null;
        } else {
            List<ExprNamedParam> paramList = getParams(exp, ts, depth);
            ExprStar star = new ExprStar(exp, 5, TypePrimitive.int32type);
            ExprNew expNew = new ExprNew(exp, ts, paramList, true, star );
            return expNew;
        }
    }

    private List<ExprNamedParam> getParams(FENode exp, TypeStructRef ts, int depth){
        List<ExprNamedParam> paramList = new ArrayList<ExprNamedParam>();
        StructDef str = nres.getStruct(ts.getName()); 
        Map<String, Type> fieldsMap = getFieldsMap(str);
        for (Entry<String, Type> f: fieldsMap.entrySet()) {
            Type t  = f.getValue();
            Expression fieldExpr;
            if (t.isStruct()) {
                TypeStructRef fieldType = (TypeStructRef) t;
                fieldExpr = getExprNew(exp, fieldType, depth-1);
                
            } else {
                fieldExpr = new ExprStar(exp, 5,t);
            }
            ExprNamedParam field = new ExprNamedParam(exp, f.getKey(), fieldExpr);
            paramList.add(field);
        }
        return paramList;
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
