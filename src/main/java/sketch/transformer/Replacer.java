package sketch.transformer;

import java.util.Iterator;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Package;
import sketch.compiler.ast.core.exprs.ExprFunCall;

public class Replacer extends FEReplacer {

	String skfunc_name;
	String var_name;
	String new_val;

	boolean found_function = false;
	boolean found_var_name = false;

	public Replacer(String _skfunc_name, String _var_name, String _new_val) {
		skfunc_name = _skfunc_name;
		var_name = _var_name;
		new_val = _new_val;
	}

	@Override
	public Object visitPackage(Package spec) {

		if (nres != null)
			nres.setPackage(spec);

		// register functions
		for (Iterator iter = spec.getFuncs().iterator(); iter.hasNext();) {
			Function func = (Function) iter.next();
			if (func.getName().contentEquals(skfunc_name)) {
				assert (!found_function);
				found_function = true;
				assert(!found_var_name);
				visitFunction(func);
				assert(found_var_name);
			}
		}
		assert (found_function);

		return spec;
	}

	public Object visitExprFunCall(ExprFunCall exp) {
		if (exp.getName().contentEquals(var_name)) {
			found_var_name = true;
			return exp.creator().name(new_val).create();
		} else {
			return exp;
		}
	}


}
