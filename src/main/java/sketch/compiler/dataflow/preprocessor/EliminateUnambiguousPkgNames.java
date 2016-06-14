package sketch.compiler.dataflow.preprocessor;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.typs.TypeStructRef;

public class EliminateUnambiguousPkgNames extends FEReplacer {

	@Override
	public Object visitTypeStructRef(TypeStructRef t) {
		String name = t.getName();
		String shortName = name.split("@")[0];
		String defaultName = nres.getStructName(shortName);
		if (defaultName != null && defaultName.equals(name)) {
			return new TypeStructRef(shortName, false);
		}
		return t;
	}
}
