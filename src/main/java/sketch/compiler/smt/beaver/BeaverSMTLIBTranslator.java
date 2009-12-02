package sketch.compiler.smt.beaver;

import java.io.PrintStream;

import sketch.compiler.smt.partialeval.NodeToSmtVtype;
import sketch.compiler.smt.smtlib.SMTLIBTranslatorBV;

public class BeaverSMTLIBTranslator extends SMTLIBTranslatorBV {
	
	public BeaverSMTLIBTranslator(NodeToSmtVtype formula, PrintStream ps, int intNumBits) {
	    super(formula, ps, intNumBits);
	}
	
	/**
	 * For some reason, beaver requires this.
	 * Adding it shouldn't hurt anything else, I would think.
	 */
	@Override
	public String prolog() {
		return super.prolog() + ":status unknown";
	}

}
