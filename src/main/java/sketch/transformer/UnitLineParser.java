package sketch.transformer;

import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UnitLineParser {

	int at = 0;
	Vector<String> _tokens;
	Pattern identifier_pattern = Pattern.compile("[a-zA-Z_][a-zA-Z_0-9]*");
	Pattern string_pattern = Pattern.compile("\"[^\n\"]*\"");

	public UnitLineParser(Vector<String> __tokens) {
		_tokens = __tokens;
	}

	private String next_token() {
		String ret = _tokens.get(at);
		at++;

		System.out.println("next_token: " + ret);
		return ret;
	}

	private void backtrack() {
		System.out.println("backtrack");
		at--;
	}

	private void assert_next_token(String must_be_next_token) {
		String at_token = next_token();
		assert (has_pattern(at_token, Pattern.compile(must_be_next_token)));
	}

	private Boolean has_pattern(String str, String pattern)
	{
		return has_pattern(str, Pattern.compile(pattern));
	}
	
	private Boolean has_pattern(String str, Pattern pattern)
	{
		Matcher m = pattern.matcher(str);
		Vector<String> tokens = new Vector<String>();

		String concat = new String();
		while (m.find()) {
			String token = m.group(0);
			tokens.add(token);
			concat = concat.concat(token);
		}

		return tokens.size() == 1;
	}

	public Param parse_param() {
		System.out.println("parsing parse_param");
		String at_token = next_token();
		if (has_pattern(at_token, string_pattern)) {
			System.out.println("parsing str");
			Param ret = new StringParam(at_token);
			if (has_pattern(next_token(), ":")) {
				Param right = parse_param();
				return new PairParam(ret, right);
			} else {
				backtrack();
				return ret;
			}
		} else if (has_pattern(at_token, identifier_pattern)) {
			return new IdentifierParam(new Identifier(at_token));
		} else if (has_pattern(at_token, "\\[")) {
			// parse vector
			Vector<Param> params = parse_params();
			assert_next_token("\\]");
			return new VectorParam(params);
		} else if (has_pattern(at_token, "\\{")) {
			// parse map
			Vector<Param> params = parse_params();
			assert_next_token("\\}");

			Map<String, Param> map = new TreeMap<String, Param>();
			for (Param param : params) {
				PairParam pair_param = (PairParam) param;
				map.put(((StringParam) pair_param.get_left()).get_string(), pair_param.get_right());
			}

			return new MapParam(map);
		} else {
			backtrack();
			return null;
		}
	}

	private Vector<Param> parse_params()
	{

		System.out.println("parsing parse_params");
		Vector<Param> params = new Vector<Param>();
		do
		{
			Param param = parse_param();
			if (param == null) {
				next_token();
				break;
			}
			params.add(param);
		}while(has_pattern(next_token(), ","));

		backtrack();

		return params;
	}

	public FunctionCall parse_function_call() {

		String at_token = next_token();
		assert (has_pattern(at_token, identifier_pattern));
		Identifier function_identifier = new Identifier(at_token);
		assert_next_token("\\(");
		Vector<Param> params = parse_params();
		assert_next_token("\\)");
		return new FunctionCall(function_identifier, params);
	}

	public UnitLine parse() {

		String at_token = next_token();

		if (has_pattern(at_token, "return")) {
			at_token = next_token();
			assert (has_pattern(at_token, identifier_pattern));
			
			return new ReturnLine(new IdentifierExpression(new Identifier(at_token)));
		} else {
			assert (has_pattern(at_token, identifier_pattern));

			Identifier object_identifier = new Identifier(at_token);

			at_token = next_token();

			if (has_pattern(at_token, "\\.")) {
				// function call

				FunctionCall function_call = parse_function_call();

				assert_next_token(";");

				System.out.println("DONE WITH function_call.");

				return new ExpressionLine(
						new FunctionCallExpression(new ObjectFunctionCall(object_identifier, function_call)));

			} else if (has_pattern(at_token, "=")) {
				// assignment

				at_token = next_token();
				assert (has_pattern(at_token, identifier_pattern));
				Identifier second_object_identifier = new Identifier(at_token);

				at_token = next_token();

				if (has_pattern(at_token, "\\.")) {

					FunctionCall function_call = parse_function_call();

					return new AssignmentLine(object_identifier, new FunctionCallExpression(
							new ObjectFunctionCall(second_object_identifier, function_call)));
				} else {
					assert (has_pattern(at_token, "\\("));
					backtrack();
					backtrack();

					FunctionCall function_call = parse_function_call();

					assert_next_token(";");

					System.out.println("DONE WITH Assignment.");

					return new AssignmentLine(object_identifier, new FunctionCallExpression(function_call));
				}

			} else {
				assert (false);
			}
		}

		assert (false);
		return null;
	}
}
