package sketch.compiler.smt.cvc3;

import sketch.compiler.smt.smtlib.SMTLIBTranslator;

public class Cvc3SMTLIBTranslator extends SMTLIBTranslator {
	
	public Cvc3SMTLIBTranslator(int intNumBits) {
		super(intNumBits);
	}
	
	@Override
	public String epilog() {
		return ":cvc_command \"CHECKSAT\"\n" +
		":cvc_command \"COUNTERMODEL\"\n)"; // for CVC3 to output the
													// assignments
		
	}

}
