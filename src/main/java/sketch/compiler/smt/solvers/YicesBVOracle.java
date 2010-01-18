package sketch.compiler.smt.solvers;

import java.io.IOException;
import java.io.LineNumberReader;
import java.util.regex.Pattern;

import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.smt.partialeval.BitVectUtil;
import sketch.compiler.smt.partialeval.FormulaPrinter;
import sketch.compiler.smt.partialeval.NodeToSmtValue;
import sketch.compiler.smt.partialeval.NodeToSmtVtype;
import sketch.compiler.smt.partialeval.SmtType;
import sketch.compiler.smt.partialeval.SmtValueOracle;
import sketch.compiler.smt.partialeval.VarNode;

public class YicesBVOracle extends SmtValueOracle {

	private static final String regex = "0b(\\d+)\\)|(true)|(false)";
	private Pattern pattern;
	
	public YicesBVOracle(FormulaPrinter fPrinter) {
		super(fPrinter);
		pattern = Pattern.compile(regex);
	}

	/**
	 * yices' models are like:
	 *  (= x_3L2_4L2_3 0b00000000000000000000000001011010)
	 */
	@Override
	public void loadFromStream(LineNumberReader in) throws IOException {
		String line;
		while ((line = in.readLine()) != null) {
			if (line.startsWith("(=")) {
				String[] parts = line.split(" ");
				assert (parts.length == 3);
				assert (parts[0].equals("(="));
				
				if (parts[1].startsWith("(")) {
					// theory of array
					// example: 
					// (= (myarray 0b00000000000000000000000000000000)
					//   0b00000000000000000000000000000001)
					
					String arrayName = parts[1].substring(1);
					arrayName = mFPrinter.stripVariableName(arrayName);
					
					// ignore the ??? in yices2 output for now
					if (parts[2].startsWith("?"))
					    continue;
					
					// parse the index
					String idxStr = parts[2].substring(2, parts[2].length()-1);
					int iidx = (int) Long.parseLong(idxStr, 2);
					// trim away the ) at the second line
//					String secondLine = in.readLine().trim();
//					secondLine = secondLine.substring(2, secondLine.length()-1);
					
					String valStr = parts[3].substring(0, parts[3].length()-1);
					if (mFPrinter.isHoleVariable(arrayName) || mFPrinter.isInputVariable(arrayName)) {
					    
					    SmtType ta = mFPrinter.getTypeForVariable(arrayName);
					    NodeToSmtValue val = stringToNodeToSmtValue(valStr, ta);
					    NodeToSmtValue idx = mFPrinter.getFormula().CONST(iidx); 
					    
	                    NodeToSmtValue arr;
	                    if (!valMap.containsKey(arrayName)) {
	                        arr = NodeToSmtValue.newParam(arrayName + "_seed", ta.getRealType(), ta.getNumBits());
	                    } else {
	                        arr = valMap.get(arrayName);
	                    }
	                    arr = (NodeToSmtValue) mFPrinter.getFormula().arrupd(arr, idx, val);
	                    putValueForVariable(arrayName, arr);
					}
					
					
				} else {
					// normal variable-value pair
					// example: (= s_11_28L3_3 0b1)
					String name = parts[1];
					name = mFPrinter.stripVariableName(name);
					// Extract the value
					String valStr = parts[2].substring(0, parts[2].length()-1);
					if (mFPrinter.isHoleVariable(name) || mFPrinter.isInputVariable(name)) {
						NodeToSmtValue ntsv = stringToNodeToSmtValue(valStr, mFPrinter.getTypeForVariable(name));
						putValueForVariable(name, ntsv);
					}
						

				}
			}
		}
	}
	
	/**
	 * Convert a string to NodeToSmtValue
	 * @param str a string that begins with 'bv', followed by the decimal representation
	 * @return
	 */
	protected NodeToSmtValue stringToNodeToSmtValue(String str, SmtType smtType) {
		Type t = smtType.getRealType();
		
		int bvSize;
		int intValue;
		if (str.equals("true"))
            return NodeToSmtValue.newBool(true);
        if (str.equals("false"))
            return NodeToSmtValue.newBool(false);
        if (str.startsWith("0b")) {
            str = str.substring(2);
            bvSize = str.length();
            
            if (bvSize > 32) { // 0b + 32bits
                str = "bvbin" + str;
                return NodeToSmtValue.newLabel(BitVectUtil.newBitArrayType(bvSize), bvSize, str);
            }
            
            intValue = (int) Long.parseLong(str, 2);
            if (t instanceof TypeArray) {
                if (BitVectUtil.isBitArray(t)) {
                    return NodeToSmtValue.newBitArray(intValue, str.length());
                } else {
                    TypeArray ta = (TypeArray) t;
                    t = ta.getBase();
                }
            }
        } else {
            intValue = (int) Long.parseLong(str);
        }
		
		
		
		if (t == TypePrimitive.inttype)
			return NodeToSmtValue.newInt(intValue, str.length());
		else if (t == TypePrimitive.bittype) {
			return NodeToSmtValue.newBit(intValue); 
		} else {
			assert false : "unexpected case";
			return null;
		}
	}

	@Override
	public NodeToSmtValue getValueForVariable(String var, SmtType smtType) {
	    
	    NodeToSmtValue val = super.getValueForVariable(var, smtType);
	    if (val == null) {
	        VarNode varNode = mFPrinter.getVarNode(var);
	        VarNode root = mFormula.getEquivalenceSet().find(varNode);
	        if (root != varNode)
	            return getValueForVariable(root);
	        else
	            return NodeToSmtVtype.defaultValue(smtType);
	    }
	    return val;
	}

}
