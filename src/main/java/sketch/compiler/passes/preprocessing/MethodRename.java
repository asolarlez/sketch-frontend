package sketch.compiler.passes.preprocessing;

import java.util.Map;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.exprs.ExprFunCall;

public class MethodRename extends FEReplacer {
    Map<String, String> oldToNew;
    public MethodRename(Map<String, String> oldToNew){
        this.oldToNew = oldToNew;
    }
    
    public String maybeSub(String s) {
        if (s == null) {
            return s;
        }
        if (oldToNew.containsKey(s)) {
            return oldToNew.get(s);
        } else {
            return s;
        }
    }

    @Override
    public Object visitFunction(Function func)
    {
        Function rv = (Function) super.visitFunction(func);
        String spec = rv.getSpecification();
        if (oldToNew.containsKey(rv.getName()) ||
                (spec != null && oldToNew.containsKey(spec)))
        {
            return rv.creator().name(maybeSub(rv.getName())).spec(maybeSub(spec)).create();
        }else{
            return rv;
        }        
    }
    
    public Object visitExprFunCall(ExprFunCall exp)
    {
        ExprFunCall efc = (ExprFunCall) super.visitExprFunCall(exp);
        if(oldToNew.containsKey(efc.getName())){
            return new ExprFunCall(efc, oldToNew.get(efc.getName()), efc.getParams());
        }else{
            return efc;
        }
    }
    
}
