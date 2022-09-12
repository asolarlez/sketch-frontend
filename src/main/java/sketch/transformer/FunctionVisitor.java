package sketch.transformer;

import java.util.Iterator;
import java.util.Set;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Package;
import sketch.compiler.ast.core.exprs.ExprFunCall;

public class FunctionVisitor extends FEReplacer {
	private Set<String> ret;

	public FunctionVisitor(Set<String> _ret) {
		ret = _ret;
	}

	private boolean has_suffix(String str, String suffix) {
		if (suffix.length() <= str.length()) {
			String potential_suffix = str.substring(str.length() - suffix.length(), str.length());
			System.out.println(str + " | " + potential_suffix + " | " + suffix);
			return potential_suffix.contentEquals(suffix);
		} else {
			return false;
		}
	}

	@Override
	public Object visitPackage(Package spec) {
		
		if (nres != null)
			nres.setPackage(spec);

		// register functions
		for (Iterator iter = spec.getFuncs().iterator(); iter.hasNext();) {
			Function func = (Function) iter.next();
			if (ret.contains(func.getName()) || has_suffix(func.getName(), "WrapperNospec")) {
				ret.add(func.getName());
				Function new_func = (Function) visitFunction(func);
				assert (new_func == func);
			}
		}

		return spec;
	}

	public Object visitExprFunCall(ExprFunCall exp) {
		ret.add(exp.getName());
		return exp;
	}

}
