package sketch.transformer;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Package;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.ExprHole;

public class Declarer extends FEReplacer {

	String skfunc_name;
	Set<String> hole_names;
	Set<String> processed_hole_names = new HashSet<String>();
	Map<String, String> port_var_to_port_val;
	Set<String> processed_port_vars = new HashSet<String>();
	boolean found_function = false;

	public Declarer(String _skfunc_name, Vector<String> _hole_names, Map<String, String> _port_var_to_port_val) {
		skfunc_name = _skfunc_name;
		hole_names = new HashSet<String>(_hole_names);
		assert (hole_names.size() == _hole_names.size());
		port_var_to_port_val = _port_var_to_port_val;
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
				assert (processed_hole_names.size() == 0);
				assert (processed_port_vars.size() == 0);
				visitFunction(func);
				assert (processed_hole_names.size() == hole_names.size());
				assert (processed_port_vars.size() == port_var_to_port_val.size());
			}
		}
		assert (found_function);

		return spec;
	}

	@Override
	public Object visitExprStar(ExprHole star) {
		String hole_name = star.getHoleName();
		assert (hole_names.contains(hole_name));
		assert (!processed_hole_names.contains(hole_name));
		processed_hole_names.add(hole_name);
		return star;
	}

	public Object visitExprFunCall(ExprFunCall exp) {
		String port_var = exp.getName();
		assert(port_var_to_port_val.containsKey(port_var));
		String port_val = exp.getName();
		assert (port_var_to_port_val.get(port_var).contentEquals(port_val));
		processed_port_vars.add(port_var);
		return exp;
	}

}
