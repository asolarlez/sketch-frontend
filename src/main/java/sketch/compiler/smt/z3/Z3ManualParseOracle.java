package sketch.compiler.smt.z3;

import java.io.IOException;
import java.io.LineNumberReader;

import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.smt.partialeval.BitVectUtil;
import sketch.compiler.smt.partialeval.NodeToSmtValue;
import sketch.compiler.smt.partialeval.SmtType;
import sketch.compiler.smt.partialeval.SmtValueOracle;

/**
 * Manually disassemble the output lines into variable-value pair
 * 
 * The output line is of the form:
 * *2 {var1 var2 var3} -> 0:bv32
 * 
 * @author lshan
 *
 */
public class Z3ManualParseOracle extends SmtValueOracle {

	public Z3ManualParseOracle() {
		super();
	}

	@Override
	public void loadFromStream(LineNumberReader in) throws IOException {
		String line = null;
		
		while ((line=in.readLine()) != null) {
			int rBrIdx = line.indexOf('}');
			int lBrIdx = line.indexOf('{');
			
			if (rBrIdx < 0) continue;
			String varNamesStr = line.substring(lBrIdx+1, rBrIdx).trim();
			String[] varNames = varNamesStr.split(" ");
			
			int arrowIdx = line.indexOf("->");
			if (arrowIdx < 0) // if no array, that means the variable is free to be anything
				continue;
			String valueStr = line.substring(arrowIdx+2).trim();
			
			for (String varName : varNames) {

				if (mFormula.isHoleVariable(varName) ||
						mFormula.isInputVariable(varName)) {
					SmtType varType = mFormula.getTypeForVariable(varName);
					NodeToSmtValue ntsv = stringToNodeToSmtValue(valueStr, varType);
					putValueForVariable(varName, ntsv);
				}
			}	
		}
	}
	
	
	private NodeToSmtValue stringToNodeToSmtValue(String valueStr, SmtType smtType) {
		NodeToSmtValue ntsv;
		Type t = smtType.getRealType();
		if (t.equals(TypePrimitive.booltype)) {
			boolean boolValue = Boolean.parseBoolean(valueStr);
			ntsv = NodeToSmtValue.newBool(boolValue);
			
		} else {
			int intValue;
			String[] fields = valueStr.split(":bv");
			int numBits = Integer.parseInt(fields[1]);
			String value = fields[0];
			
			if (numBits <= 32) {
				intValue = (int) Long.parseLong(fields[0]);
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