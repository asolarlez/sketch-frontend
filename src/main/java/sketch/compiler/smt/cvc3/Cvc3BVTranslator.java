package sketch.compiler.smt.cvc3;

import java.io.PrintStream;

import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.smt.partialeval.NodeToSmtVtype;

public class Cvc3BVTranslator extends Cvc3Translator {
	
	protected int intBits;

	public Cvc3BVTranslator(NodeToSmtVtype formula, PrintStream ps, int numIntBits) {
		super(formula, ps);
		intBits = numIntBits;
	}

	@Override
	protected void initOpCode() {
		opStrMap.put(OpCode.PLUS, "BVPLUS");
		opStrMap.put(OpCode.MINUS, "BVSUB");
		opStrMap.put(OpCode.OVER, "NOT SUPPORTED DIVISION");
		opStrMap.put(OpCode.TIMES, "BVMULT");

		opStrMap.put(OpCode.EQUALS, "=");
		opStrMap.put(OpCode.NOT_EQUALS, "/=");
		opStrMap.put(OpCode.LT, "BVLT");
		opStrMap.put(OpCode.LEQ, "BVLE");
		opStrMap.put(OpCode.GT, "BVGT");
		opStrMap.put(OpCode.GEQ, "BVGE");
		opStrMap.put(OpCode.NEG, "BVUMINUS");

		opStrMap.put(OpCode.AND, "AND");
		opStrMap.put(OpCode.OR, "OR");
		opStrMap.put(OpCode.XOR, "XOR");
		opStrMap.put(OpCode.NOT, "NOT");
	}
	
	/**
	 * Cutdown or pad the width of the binaryRep to the desiredSize
	 * 
	 * If binaryRep is negative
	 * @param binaryRep
	 * @param desiredSize
	 * @return
	 */
	private String toFixedWidthBinaryString(int x, int desiredSize) {
		String binaryRep = Integer.toBinaryString(x);
		int length = binaryRep.length();
		if (length > desiredSize) {
			assert binaryRep.startsWith("1");
			// it's a negative number
			binaryRep = binaryRep.substring(length - desiredSize, length);
			return binaryRep;
		}
		if (binaryRep.length() < desiredSize) {
			StringBuffer sb = new StringBuffer();
			
			for (int i = binaryRep.length();
				i<desiredSize; i++)
				sb.append('0');
			return sb.toString() + binaryRep;
		}
		return binaryRep;
	}
	
	
	@Override
	public String getTypeForSolver(Type t) {
		if (t.equals(TypePrimitive.inttype) || t.equals(TypePrimitive.nulltype)) {
			return "BITVECTOR(32)";
		}
		return super.getTypeForSolver(t);
	}
	
	@Override
	public String getIntLiteral(int i, int numBits) {
		String binaryRep = "0bin" + toFixedWidthBinaryString(i, numBits);
		return binaryRep;
	}

}
