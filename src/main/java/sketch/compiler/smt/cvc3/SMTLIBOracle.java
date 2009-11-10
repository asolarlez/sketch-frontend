package sketch.compiler.smt.cvc3;

import java.io.IOException;
import java.io.LineNumberReader;

import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.smt.partialeval.BitVectUtil;
import sketch.compiler.smt.partialeval.NodeToSmtValue;

/**
 * This class is designed to handle cvc3-SMTLIB-specific output from the
 * cvc3-SMTLIB solver. It parses the output into a var->value map.
 * 
 * @author Lexin Shan
 * @email lshan@eecs.berkeley.edu
 * 
 */
public class SMTLIBOracle extends Cvc3Oracle {

	public SMTLIBOracle() {
		super();
	}

	@Override
	public void loadFromStream(LineNumberReader in) throws IOException {
		String line = null;

		while ((line = in.readLine()) != null) {
			if (line.contains("unsat"))
				break;

			if (!line.contains(":assumption"))
				continue;

			String[] arrays = line.split("(:assumption \\(= )|\\)| ");
			if (arrays.length != 5)
				continue;

			String varName = arrays[3];
			String valStr = arrays[4];
			// Object[] varValuePair = chooseValue(arrays[3], arrays[4]);
			// if (varValuePair == null) continue;


			if (mFormula.isHoleVariable(varName) || mFormula.isInputVariable(varName)) {
				Type varType = mFormula.getTypeForVariable(varName);
				putValueForVariable(varName,
						stringToNodeToSmtValue(valStr, varType));
			}
			

		}

	}

	protected NodeToSmtValue stringToNodeToSmtValue(String str, Type t) {

		if (t.equals(TypePrimitive.booltype)) {
			throw new IllegalStateException("Unexpected type in SMTLIBOracle");
		} else {
			
			int numBits = Integer.parseInt(str.substring(str.lastIndexOf('[') + 1,
					str.lastIndexOf(']')));
			int intValue = (int) Long.parseLong(str.substring(2, str
					.lastIndexOf('[')));
			
			if (t.equals(TypePrimitive.bittype)) {
				return NodeToSmtValue.newBit(intValue);
			} else if (t.equals(TypePrimitive.inttype)) {
				return NodeToSmtValue.newInt(intValue, numBits);
			} else if (BitVectUtil.isBitArray(t)) {
				return NodeToSmtValue.newBitArray(intValue, numBits);
			} else {
				throw new IllegalStateException(
						"Unexpected type in SMTLIBOracle");
			}
		}
	}

	private Object[] chooseValue(String n1, String n2) {
		Object[] ret = new Object[2];
		if (n1.startsWith("bv")) {
			n1 = n1.substring(n1.indexOf('v') + 1, n1.indexOf('['));
		}
		try {
			ret[1] = (int) Long.parseLong(n1);
			ret[0] = n2;
			return ret;
		} catch (NumberFormatException e) {
		}
		// parsing n1 failed

		if (n2.startsWith("bv")) {
			n2 = n2.substring(n2.indexOf('v') + 1, n2.indexOf('['));
		}

		try {
			ret[0] = n1;
			ret[1] = (int) Long.parseLong(n2);
			return ret;
		} catch (NumberFormatException e) {
		}
		return null;
	}

}
