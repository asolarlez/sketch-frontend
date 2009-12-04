package sketch.compiler.smt.cvc3;

import java.io.PrintStream;

import sketch.compiler.smt.partialeval.NodeToSmtVtype;
import sketch.compiler.smt.smtlib.SMTLIBTranslatorBV;

public class Cvc3SMTLIBTranslator extends SMTLIBTranslatorBV {
	
	public Cvc3SMTLIBTranslator(NodeToSmtVtype formula, PrintStream ps, int intNumBits) {
		super(formula, ps, intNumBits);
	}
	
	@Override
	public String epilog() {
		return ":cvc_command \"CHECKSAT\"\n" +
		":cvc_command \"COUNTERMODEL\"\n)"; // for CVC3 to output the
													// assignments
		
	}

}
