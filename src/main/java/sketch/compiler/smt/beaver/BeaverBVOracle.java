package sketch.compiler.smt.beaver;

import java.io.IOException;
import java.io.LineNumberReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import sketch.compiler.smt.partialeval.NodeToSmtValue;
import sketch.compiler.smt.partialeval.SmtValueOracle;

public class BeaverBVOracle extends SmtValueOracle {
	
	private static final String regex = "bv(\\d+)";
	private Pattern pattern;
	
	public BeaverBVOracle() {
		super();
		pattern = Pattern.compile(regex);
	}

	/**
	 * Beaver's models (in their default form) are like:
	 *   a_3L1_4L1_1 = bv20[32]
	 * The lhs is the name.  The value is the number
	 * after the bv and before the size.
	 */
	@Override
	public void loadFromStream(LineNumberReader in) throws IOException {
		String line;
		while ((line = in.readLine()) != null) {
			String[] parts = line.split(" ");
			assert (parts.length == 3);
			assert (parts[1] == "=");
			String name = parts[0];
			// Extract the value
			Matcher matcher = pattern.matcher(parts[2]);
			if (matcher.find()) {
				String valStr = matcher.group(1);
				int value = (int)Long.parseLong(valStr);
				NodeToSmtValue ntsv = NodeToSmtValue.newInt(value, mIntNumBits);
				putValueForVariable(name, ntsv);
			}
		}
		// TODO: Do error handling here?  Other oracles don't.
	}
}
