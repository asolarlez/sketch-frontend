package sketch.transformer;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Package;
import sketch.compiler.ast.core.Package.PackageCreator;
import sketch.compiler.ast.core.exprs.ExprHole;

public class Cloner extends FEReplacer {

	String skfunc_name;
	String clone_name;
	Map<String, String> hole_rename_map;
	Set<String> processed_hole_names = new HashSet<String>();
	boolean found_function = false;

	public Cloner(String _skfunc_name, String _clone_name, Map<String, String> _hole_rename_map) {
		skfunc_name = _skfunc_name;
		clone_name = _clone_name;
		hole_rename_map = _hole_rename_map;
	}

	@Override
	public Object visitPackage(Package spec) {

		if (nres != null)
			nres.setPackage(spec);

		PackageCreator ret = spec.creator();

		// register functions
		for (Iterator iter = spec.getFuncs().iterator(); iter.hasNext();) {
			Function func = (Function) iter.next();
			if (func.getName().contentEquals(skfunc_name)) {
				assert (!found_function);
				found_function = true;

				Function clone = func.creator().name(clone_name).create();

				assert (processed_hole_names.size() == 0);
				visitFunction(clone);
				assert (processed_hole_names.size() == hole_rename_map.size());

				ret.add_funcion(clone);

			}
		}
		assert (found_function);

		return ret.create();
	}

	@Override
	public Object visitExprStar(ExprHole star) {
		String hole_name = star.getHoleName();
		assert (hole_rename_map.containsKey(hole_name));
		assert (!processed_hole_names.contains(hole_name));

		ExprHole ret = new ExprHole(star);
		ret.rename(hole_rename_map.get(hole_name));

		processed_hole_names.add(hole_name);

		return ret;
	}

}
