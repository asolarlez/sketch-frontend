package sketch.compiler.parallelEncoder;

import java.util.HashMap;
import java.util.Map;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
/**
 * 
 * Similar to VarReplacer, but this replaces any variable appearing in its input map.
 * Similar to VarReplacer, but this one doesn't deal with variable declarations. 
 * It should be more efficient than VarReplacer when you have a large number 
 * of possile replacements to make in a small fragment of code.
 * VarReplacer should be better when you have a small set of replacements in a large piece of code.
 * 
 * 
 * @author asolar
 *
 */
public class VarSetReplacer extends FEReplacer {
	Map<String, Expression> rmap;
	public VarSetReplacer(Map<String, Expression> rmap){
		this.rmap = rmap;
	}
	public VarSetReplacer(){
		this.rmap = new HashMap<String, Expression>();
	}
	public void addPair(String name, Expression exp){
		rmap.put(name, exp);
	}
	
	public Object visitExprVar(ExprVar exp) {
		String name = exp.getName();
		if( rmap.containsKey(name) ){
			return rmap.get(name);
		}
		return exp; 
	}
}
