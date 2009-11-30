package sketch.compiler.smt.z3;

import java.io.IOException;
import java.io.LineNumberReader;

import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.smt.partialeval.BitVectUtil;
import sketch.compiler.smt.partialeval.FormulaPrinter;
import sketch.compiler.smt.partialeval.NodeToSmtValue;
import sketch.compiler.smt.partialeval.SmtType;
import sketch.compiler.smt.stp.STPOracle;

/**
 * Manually disassemble the output lines into variable-value pair
 * 
 * The output line is of the form:
 * *2 {var1 var2 var3} -> 0:bv32
 * 
 * @author lshan
 *
 */
public class Z3ManualParseOracle2_0 extends STPOracle {
		
	public Z3ManualParseOracle2_0(FormulaPrinter fPrinter) {
		super(fPrinter);
	}

	@Override
	public void loadFromStream(LineNumberReader in) throws IOException {
		String line = null;
		
		while ((line=in.readLine()) != null) {
			
			int arrowIdx = line.indexOf(" -> ");
			if (arrowIdx < 0) // if no array, that means the variable is free to be anything
				continue;
			
			String varName = line.substring(0, arrowIdx);
			String valueStr = line.substring(arrowIdx+4).trim();
			
			transferMap.put(varName, valueStr);
		}
		
		for (String varName : transferMap.keySet()) {
			if ((mFPrinter.isHoleVariable(varName) || mFPrinter.isInputVariable(varName))) {
				String value = traceDown(varName);
				if (value == null)
					continue;
				NodeToSmtValue ntsv = stringToNodeToSmtValue(value, mFPrinter.getTypeForVariable(varName));
				
				if (ntsv != null)
					putValueForVariable(varName, ntsv);
			}
		}
	}
	
	
	protected NodeToSmtValue stringToNodeToSmtValue(String valueStr, SmtType smtType) {
		NodeToSmtValue ntsv;
		Type t = smtType.getRealType();
		if (t.equals(TypePrimitive.booltype)) {
			boolean boolValue = Boolean.parseBoolean(valueStr);
			ntsv = NodeToSmtValue.newBool(boolValue);
			
		} else {
			
			if (!valueStr.startsWith("bv")) return null;
			int intValue;
			String[] fields = valueStr.split("bv|\\[|\\]");
			int numBits = Integer.parseInt(fields[2]);
			String value = fields[1];
			
			if (numBits <= 32) {
				intValue = (int) Long.parseLong(value);
			} else {
				return NodeToSmtValue.newLabel( 
						BitVectUtil.newBitArrayType(numBits), 
						numBits,
						"bv" + value + "[" + numBits + "]");
			}
			
			
			if (t.equals(TypePrimitive.inttype)) {
				ntsv = NodeToSmtValue.newInt(intValue, numBits);
			} else if (t.equals(TypePrimitive.bittype)) {
				ntsv = NodeToSmtValue.newBit(intValue);
			} else if (BitVectUtil.isBitArray(t)) {
				ntsv = NodeToSmtValue.newBitArray(intValue, numBits);
			} else {
				throw new IllegalStateException("unexpected type in Z3ManualParseOracle.parseToValue");
			}
		}
		
		return ntsv;
	}
	
	protected static int parseStringToInt(String str) {
		int intVal = -1;
		try {
			long l = Long.parseLong(str);
			intVal = (int) l;
			
		} catch(NumberFormatException e) {
			e.printStackTrace();
		}
		return intVal;
	}

}
