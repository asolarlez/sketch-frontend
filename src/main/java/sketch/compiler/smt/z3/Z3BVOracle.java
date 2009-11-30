package sketch.compiler.smt.z3;

import java.io.IOException;
import java.io.LineNumberReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.smt.partialeval.FormulaPrinter;
import sketch.compiler.smt.partialeval.NodeToSmtValue;
import sketch.compiler.smt.partialeval.SmtValueOracle;

/**
 * Use regular expression to to parse the output lines into
 * variable-value pairs.
 * 
 * There are bugs with the regular expression, which cause
 * StackOverFlowException on certain tests. This class is currently
 * not being used. Use Z3ManualParseOrable instead.
 * 
 * @author lshan
 *
 */
public class Z3BVOracle extends SmtValueOracle {
	private final static boolean DEBUG = false;
	
	private final static String regex = "\\*" + "([0-9]+)" + // line number
	   "\\s*\\{" + 
	   "(([\\_0-9A-Za-z]+\\s*)+)" + // variable name, there could be multiple names with space inbetween
	   "\\}\\s*\\-\\>\\s" + 
	   "(true|false|([0-9]+)\\:bv([0-9]+))"; // value:bv[bitvector size]
	
//	\*([0-9]+)\s*\{(([\_0-9A-Za-z]+\s*)+)\}\s*\-\>\s(true|false|([0-9]+)\:bv([0-9]+))
	
	
	Pattern pattern;

	public Z3BVOracle(FormulaPrinter fPrinter) {
		super(fPrinter);
		pattern = Pattern.compile(regex);
		
	}
	
	@Override
	public void loadFromStream(LineNumberReader in) throws IOException {
		String line;
		
		while ((line = in.readLine()) != null) {
//			if (!(line.contains(getHolePrefix()))) {
//				continue;
//			}
			// The kind of string to match is: *2 {sk__has_out__3L0_4L0_1} -> 0:bv32
			// the parts we need to extract is: 
			// 2, the line number
			// sk__has_out__3L0_4L0_1, the variable name
			// 0, the value in bv32
			Matcher matcher = pattern.matcher(line);
			
			if (matcher.find()) {
				String oneOrMoreVarNames = matcher.group(2);
				String valueStr =  matcher.group(4);
				
				if (DEBUG) {
					System.out.println("Found a match: " + matcher.group(0));
				     System.out.println("Line number: " + matcher.group(1));
				     System.out.println("Variable: " + matcher.group(2));
				     System.out.println("Value: " + matcher.group(4)); // true|false|100:bv32
				}
				
				Type t = null;
				if (valueStr.equals("true") || valueStr.equals("false")) {
					t = TypePrimitive.booltype;
				} else if (matcher.group(6).equals("1")){
					t = TypePrimitive.bittype;
				} else {
					t = TypePrimitive.inttype;
				}	
				
				if (oneOrMoreVarNames.contains(" ")) {
					String[] varNames = oneOrMoreVarNames.split("\\s");
					
					for (String varName : varNames) {
						NodeToSmtValue ntsv = parseToValue(matcher.group(5), t);
						putValueForVariable(varName, ntsv);
					}
				} else {
					String varName = oneOrMoreVarNames;
					NodeToSmtValue ntsv = parseToValue(matcher.group(5), t);
					putValueForVariable(varName, ntsv);
				}
				
				
			} else {
				if (DEBUG)
					System.out.println("Unmatched:" + line);
			}
			
		}

	}

	private NodeToSmtValue parseToValue(String valueStr, Type t) {
		NodeToSmtValue ntsv;
		if (t.equals(TypePrimitive.inttype)) {
			int intValue = parseStringToInt(valueStr);
			ntsv = NodeToSmtValue.newInt(intValue, valueStr.length());
		} else if (t.equals(TypePrimitive.bittype)) {
			int intValue = parseStringToInt(valueStr);
			ntsv = NodeToSmtValue.newBit(intValue);
		} else {
			boolean boolValue = Boolean.parseBoolean(valueStr);
			ntsv = NodeToSmtValue.newBool(boolValue);
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
