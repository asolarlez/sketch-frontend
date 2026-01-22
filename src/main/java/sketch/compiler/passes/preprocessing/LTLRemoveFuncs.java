package sketch.compiler.passes.preprocessing;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.main.seq.LTLWhileForever;

public class LTLRemoveFuncs extends FEReplacer {

	public Object visitFunction(Function func) {
		Statement body = func.getBody();
		if (body == null)
			return func;
		LTLWhileForever forever = new LTLWhileForever();
		body.accept(forever);
		if (forever.getIsInf()) {
			return null;
		}
		return super.visitFunction(func);
	}

}
