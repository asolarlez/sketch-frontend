package sketch.transformer;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Package;
import sketch.compiler.ast.core.Package.PackageCreator;

public class Slicer extends FEReplacer {

	Set<String> keep_funcs;
	Set<String> visited_funcs = new TreeSet<String>();

	public Slicer(Set<String> _keep_funcs) {
		keep_funcs = _keep_funcs;
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
			if (keep_funcs.contains(func.getName())) {
				ret.add_funcion(func);
				visited_funcs.add(func.getName());
			}
		}

		assert (visited_funcs.size() == keep_funcs.size());

		return ret.create();
	}
}