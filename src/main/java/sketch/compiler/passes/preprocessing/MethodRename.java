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
    
    @Override
    public Object visitFunction(Function func)
    {
        Function rv = (Function) super.visitFunction(func);
        if(oldToNew.containsKey(rv.getName())){
            return rv.creator().name(oldToNew.get(rv.getName())).create();
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
