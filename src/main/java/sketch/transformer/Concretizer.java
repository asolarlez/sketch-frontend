package sketch.transformer;

import java.util.Iterator;
import java.util.Map;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.NameResolver;
import sketch.compiler.ast.core.Package;
import sketch.compiler.ast.core.Package.PackageCreator;
import sketch.compiler.ast.core.Program;
import sketch.compiler.solvers.constructs.ValueOracle;
import sketch.compiler.stencilSK.EliminateHoleStatic;

public class Concretizer extends FEReplacer {

	String skfunc_name;
	EliminateHoleStatic concretizer;
	Map<String, String> hole_vals;
	boolean found_function = false;

	public Concretizer(String _skfunc_name, Map<String, String> _hole_vals, Program program) {
		skfunc_name = _skfunc_name;
		hole_vals = _hole_vals;

		ValueOracle value_oracle = new ValueOracle();
		value_oracle.load_from_map_string_string(hole_vals);

		concretizer = new EliminateHoleStatic(value_oracle);

		setNres(new NameResolver(program));

		concretizer.setNres(getNres());

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
				Function new_func = (Function) concretizer.visitFunction(func);
				if (new_func == func) {
					assert (hole_vals.isEmpty());
				} else {
					assert (!hole_vals.isEmpty());
				}
				ret.add_funcion(func);
			} else {
				ret.add_funcion(func);
			}
		}
		assert (found_function);

		return ret.create();
	}

}

