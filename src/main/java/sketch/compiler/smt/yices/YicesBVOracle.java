package sketch.compiler.smt.yices;

import java.io.IOException;
import java.io.LineNumberReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.smt.partialeval.BitVectUtil;
import sketch.compiler.smt.partialeval.NodeToSmtValue;
import sketch.compiler.smt.partialeval.SmtType;
import sketch.compiler.smt.partialeval.SmtValueOracle;
import sketch.compiler.smt.partialeval.VarNode;

public class YicesBVOracle extends SmtValueOracle {

	private static final String regex = "0b(\\d+)\\)|(true)|(false)";
	private Pattern pattern;
	
	public YicesBVOracle() {
		super();
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
					Map<Integer, NodeToSmtValue> arrayStore = arrayValueMap.get(arrayName);
					
					if (arrayStore == null) {
						arrayStore = new HashMap<Integer, NodeToSmtValue>();
						arrayValueMap.put(arrayName, arrayStore);
					}
					
					// parse the index
					String idxStr = parts[2].substring(2, parts[2].length()-1);
					int idx = (int) Long.parseLong(idxStr, 2);
					
					// trim away the ) at the second line
					String secondLine = in.readLine().trim();
					secondLine = secondLine.substring(2, secondLine.length()-1);
					
					NodeToSmtValue ntsv = stringToNodeToSmtValue(secondLine, mFormula.getTypeForVariable(arrayName));
					arrayStore.put(idx, ntsv);
					
				} else {
					// normal variable-value pair
					// example: (= s_11_28L3_3 0b1)
					String name = parts[1];
					// Extract the value
					Matcher matcher = pattern.matcher(parts[2]);
					if (matcher.find()) {
						String valStr;
						if (matcher.group(1) != null)
							valStr = matcher.group(1);
						else if (matcher.group(2) != null)
							valStr = matcher.group(2);
						else
							valStr = matcher.group(3);
					
						if (mFormula.isHoleVariable(name) || mFormula.isInputVariable(name)) {
							NodeToSmtValue ntsv = stringToNodeToSmtValue(valStr, mFormula.getTypeForVariable(name));
							putValueForVariable(name, ntsv);
						}
						
					}
				}
			}
		}
		// TODO: Do error handling here?  Other oracles don't.
	}
	
	/**
	 * Convert a string to NodeToSmtValue
	 * @param str a string that begins with 'bv', followed by the decimal representation
	 * @return
	 */
	protected NodeToSmtValue stringToNodeToSmtValue(String str, SmtType smtType) {
		Type t = smtType.getRealType();
		if (str.equals("true"))
			return NodeToSmtValue.newBool(true);
		if (str.equals("false"))
			return NodeToSmtValue.newBool(false);
		
		int intValue = (int) Long.parseLong(str, 2);
		if (t == TypePrimitive.inttype)
			return NodeToSmtValue.newInt(intValue, str.length());
		else if (t == TypePrimitive.bittype)
			return NodeToSmtValue.newBit(intValue); 
		else if (BitVectUtil.isBitArray(t)) {
			return NodeToSmtValue.newBitArray(intValue, str.length());
			
		} else {
			assert false : "unexpected case";
			return null;
		}
	}

	@Override
	public NodeToSmtValue getValueForVariable(String var, SmtType smtType) {
	    
	    NodeToSmtValue val = super.getValueForVariable(var, smtType);
	    if (val == null) {
	        VarNode varNode = mFormula.getVarNode(var);
	        VarNode root = mFormula.getEquivalenceSet().find(varNode);
	        return getValueForVariable(root);
	    }
	    return val;
	}

}
