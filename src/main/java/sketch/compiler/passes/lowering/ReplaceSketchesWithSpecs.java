package sketch.compiler.passes.lowering;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Package;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.Expression;

public class ReplaceSketchesWithSpecs extends FEReplacer {

    protected Package ss;
    
    public Object visitStreamSpec(Package spec)
    {

        Package oldSS = ss;
        ss = spec;
        Object o = super.visitStreamSpec(spec);
        ss = oldSS;
        return o;
    }
    
    public Object visitExprFunCall(ExprFunCall exp)
    {       
        String name = exp.getName();
        // Local function?
        Function fun = nres.getFun(name);
        if(fun.getSpecification()!= null){
            List<Expression> newParams = new ArrayList<Expression>();
            for (Iterator<Expression> iter = exp.getParams().iterator(); iter.hasNext(); )
            {
                Expression param = (Expression)iter.next();
                Expression newParam = doExpression(param);
                newParams.add(newParam);            
            }            
            return new ExprFunCall(exp, fun.getSpecification(), newParams);
        }else{
            return super.visitExprFunCall(exp);
        }
    }
    
    
}
