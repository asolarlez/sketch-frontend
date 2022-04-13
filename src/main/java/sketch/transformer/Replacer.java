package sketch.transformer;

import java.util.Iterator;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Package;
import sketch.compiler.ast.core.Package.PackageCreator;
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

		PackageCreator ret = spec.creator();

		ret.clear_funcs();

		// register functions
		for (Iterator iter = spec.getFuncs().iterator(); iter.hasNext();) {
			Function func = (Function) iter.next();
			if (func.getName().contentEquals(skfunc_name)) {
				assert (!found_function);
				found_function = true;
				assert(!found_var_name);
				Function new_func = (Function) visitFunction(func);
				assert (new_func != func);
				ret.add_funcion(func);
				assert(found_var_name);
			}
			else {
				ret.add_funcion(func);
			}
		}
		assert (found_function);

		return ret.create();
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
