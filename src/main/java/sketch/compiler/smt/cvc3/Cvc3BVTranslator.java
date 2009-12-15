package sketch.compiler.smt.cvc3;

import java.io.PrintStream;

import sketch.compiler.smt.partialeval.NodeToSmtVtype;
import sketch.compiler.smt.solvers.STPTranslator;

public class Cvc3BVTranslator extends STPTranslator {
	
	protected int intBits;

	public Cvc3BVTranslator(NodeToSmtVtype formula, PrintStream ps, int numIntBits) {
		super(formula, ps, numIntBits);
	}
	
	@Override
    public String epilog() {
        return "CHECKSAT;\n" +
            "COUNTERMODEL;";
    }
}
