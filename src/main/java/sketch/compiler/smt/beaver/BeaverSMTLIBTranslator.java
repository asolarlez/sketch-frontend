package sketch.compiler.smt.beaver;

import sketch.compiler.smt.smtlib.SMTLIBTranslator;

public class BeaverSMTLIBTranslator extends SMTLIBTranslator {
	
	public BeaverSMTLIBTranslator(int intNumBits) {
		super(intNumBits);
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
