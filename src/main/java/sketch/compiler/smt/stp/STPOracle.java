package sketch.compiler.smt.stp;

import java.io.IOException;
import java.io.LineNumberReader;
import java.util.HashMap;

import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.smt.partialeval.BitVectUtil;
import sketch.compiler.smt.partialeval.NodeToSmtValue;
import sketch.compiler.smt.partialeval.SmtType;
import sketch.compiler.smt.partialeval.SmtValueOracle;

public class STPOracle extends SmtValueOracle {

	protected HashMap<String, String> transferMap;
	
	public STPOracle() {
		super();
		transferMap = new HashMap<String, String>();
	}

	@Override
	public void loadFromStream(LineNumberReader in) throws IOException {
		String line;

		
		while ((line = in.readLine()) != null) {
			try {
			if (line.startsWith("ASSERT(")) {
				line = line.substring("ASSERT(".length(), line.lastIndexOf(')'));
				line = line.trim();
				
				String[] fields;
				if (line.contains("<=>"))
					// windows version, BOOLEAN var value assignment uses this notation
					fields = line.split("<=>"); 
				else
					// mac or linux version
					fields = line.split("="); // 
				
				String varName = fields[0].trim();
				String value = fields[1].trim();
				
				transferMap.put(varName, value);
				
				
			}
			} catch (Exception e) {
				// there can be output format that we dont understand
			}
		}
		
		for (String varName : transferMap.keySet()) {
			if ((mFormula.isHoleVariable(varName) || mFormula.isInputVariable(varName))) {
				String value = traceDown(varName);
				if (value == null)
					continue;
				NodeToSmtValue ntsv = stringToNodeToSmtValue(value, mFormula.getTypeForVariable(varName));
				
				if (ntsv != null)
					putValueForVariable(varName, ntsv);
			}
		}
	}
	
	protected String traceDown(String varName) {
		
		String ret = transferMap.get(varName);
//		if (ret.equals("TRUE") ||
//				ret.equals("FALSE") ||
//				ret.startsWith("0b") ||
//				ret.startsWith("0hex") ||
//				ret.startsWith("0x"))
//			return ret;
//		else
//
		if (transferMap.containsKey(ret))
			return traceDown(ret);
		else
			return ret;
	}
	
	protected NodeToSmtValue stringToNodeToSmtValue(String value, SmtType smtType) {
		Type t = smtType.getRealType();
		if (value.equals("TRUE"))
			return NodeToSmtValue.newBool(true);
		if (value.equals("FALSE"))
			return NodeToSmtValue.newBool(false);
		
		int intValue;
		int bvSize;
	
			if (value.startsWith("0b")) {
				if (value.startsWith("0bin")) {
					bvSize = value.length() - 4;
					value = value.substring(4);
				} else {
					bvSize = value.length() - 2;
					value = value.substring(2);
				}
				
				if (bvSize > 32) { // 0bin + 32bits
					return NodeToSmtValue.newLabel(BitVectUtil.newBitArrayType(bvSize), bvSize, value);
				}
				
				intValue = (int) Long.parseLong(value, 2);
				
			} else if (value.startsWith("0hex")) {
				bvSize = (value.length() - 4) * 4;
				if (bvSize > 32) { // 0bin + 32bits
					return NodeToSmtValue.newLabel(BitVectUtil.newBitArrayType(bvSize), bvSize, value);
				}
				value = value.substring(4);
				intValue = (int) Long.parseLong(value, 16);
			} else if (value.startsWith("0x")) {
				bvSize = (value.length() - 2) * 4;
				if (bvSize > 32) { // 0bin + 32bits
					return NodeToSmtValue.newLabel(BitVectUtil.newBitArrayType(bvSize), bvSize, value);
				}
				value = value.substring(2);
				intValue = (int) Long.parseLong(value, 16);
			} else {
				// it's probably the case where STP returns ASSERT var1 = var2;
				// which means the values for var1 and var2 don't matter
				return null;
			}
	
		if (t == TypePrimitive.inttype)
			return NodeToSmtValue.newInt(intValue, bvSize);
		else if (t == TypePrimitive.bittype)
			return NodeToSmtValue.newBit(intValue); 
		else if (BitVectUtil.isBitArray(t)) {
			return NodeToSmtValue.newBitArray(intValue, BitVectUtil.vectSize(t));
			
		} else {
			assert false : "unexpected case";
			return null;
		}
	}

}
