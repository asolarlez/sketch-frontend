package sketch.compiler.smt.cvc3;

import java.io.IOException;
import java.io.LineNumberReader;
import java.util.StringTokenizer;

import sketch.compiler.smt.partialeval.NodeToSmtValue;

public class Cvc3BVOracle extends Cvc3Oracle {

	public Cvc3BVOracle() {
		super();
	}

	@Override
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

				int beginIdx = value.indexOf("0bin"); 
				if (beginIdx >= 0) {
					value = value.substring(beginIdx + 4);
					try {
						long l = Long.parseLong(value, 2);
						int intVal = (int) l;
						NodeToSmtValue val = NodeToSmtValue.newInt(intVal, super.mIntNumBits);
						putValueForVariable(name, val);
					} catch (NumberFormatException e) {
						// RHS of = may not be an int
						// ignore it
					}
				}
				
			}
		}
	}

}
