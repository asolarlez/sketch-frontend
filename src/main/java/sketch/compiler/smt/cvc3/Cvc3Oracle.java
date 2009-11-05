package sketch.compiler.smt.cvc3;

import java.io.IOException;
import java.io.LineNumberReader;
import java.util.StringTokenizer;

import sketch.compiler.smt.partialeval.NodeToSmtValue;
import sketch.compiler.smt.partialeval.SmtValueOracle;

/**
 * This class is designed to handle CVC3-specific output from the
 * CVC3 solver. It parses the output into a var->value map.
 * 
 * @author Lexin Shan
 * @email lshan@eecs.berkeley.edu
 *
 */
public class Cvc3Oracle extends SmtValueOracle {

	public Cvc3Oracle() {
		super();
	}

	/**
	 * This function populates the valMap with information from the file in,
	 * about what variables got what values.
	 * 
	 * @param in
	 * @throws IOException
	 */
	public void loadFromStream(LineNumberReader in) throws IOException {
		String line = null;
		while ((line = in.readLine()) != null) {
			
			StringTokenizer st = new StringTokenizer(line, "() ;");
			String dummy;
			dummy = st.nextToken(); // ASSERT
			if (dummy.equals("ASSERT")) {
				String name = st.nextToken();
				dummy = st.nextToken(); // = sign
				String value = st.nextToken();
				
				try {
					putValueForVariable(name, NodeToSmtValue.newInt(Integer.parseInt(value), super.mIntNumBits));
				} catch (NumberFormatException e) {
					// RHS of = may not be an int
					// ignore it
				}
			}
		}
	}
	
}