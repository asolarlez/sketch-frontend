package sketch.compiler.stencilSK;

import java.util.List;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.stmts.Statement;
/**
 * This class makes sure that the names in the parameter list for both the spec
 * and the sketch match.
 * @author asolar
 *
 */
public class MatchParamNames extends FEReplacer {

	
	
    public Object visitFunction(Function func)
    {
    	Statement newBody = func.getBody();
    	List<Parameter>  newParams = func.getParams();
    	if( func.getSpecification() != null ){
    		Function spec = this.getFuncNamed(func.getSpecification());
    		assert func.getParams().size() == spec.getParams().size() : spec.toString() + ": The size of the parameter lists for spec and sketch must match. "; 
    		List<Parameter> p1 = func.getParams();
    		List<Parameter> p2 = spec.getParams();    		
    		for(int i=0; i< p2.size() ; ++i ){
    			//TODO There should be a type check here.
    			String oldName = p1.get(i).getName();
    			String newName = p2.get(i).getName();
    			if( !oldName.equals(newName) ){
    				newBody = (Statement) newBody.accept( new VarReplacer(oldName, newName ) );
    				newParams = p2;
    			}
    		}    		
    	}
        if (newBody == func.getBody()) return func;
        return func.creator().params(newParams).body(newBody).create();
    }
}
