package sketch.compiler.solvers.constructs;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import antlr.RecognitionException;
import antlr.TokenStreamException;
import sketch.compiler.ast.core.FEContext;
import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.exprs.ExprArrayRange;
import sketch.compiler.ast.core.exprs.ExprConstChar;
import sketch.compiler.ast.core.exprs.ExprConstFloat;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprConstant;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtReturn;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.parser.StreamItLex;
import sketch.compiler.parser.StreamItParserFE;
import sketch.transformer.CodeBlock;


public class ValueOracle extends AbstractValueOracle {
	/**
	 * After sketch is resolved, this map will contain the value of each
	 * variable in store.
	 */
	protected Map<String, Expression> valMap;
	protected List<Function> funlist;

	protected int starSizesCaped = -1;

	public ValueOracle(HoleNameTracker holeNamer) {
		super();
		valMap = null;
		this.holeNamer = holeNamer;
	}

	public boolean hasFunctions() {
		return !funlist.isEmpty();
	}

	public List<Function> getFunctions() {
		return funlist;
	}

	private void parseExpression(String vname, String dbRecord) {
		StreamItParserFE parse = new StreamItParserFE(new StreamItLex(new StringReader(dbRecord.replace(vname, ""))),
				null, false, null);
		parse.setFilename("Generated");
		Expression exp = null;
		try {
			exp = parse.prefix_expr();
		} catch (RecognitionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TokenStreamException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		valMap.put(vname, exp);
	}

	private void parseCompound(String vname, LineNumberReader in) throws IOException {
		String dbRecord = null;
		StringBuffer sbuf = new StringBuffer();
		sbuf.append('{');
		while ((dbRecord = in.readLine()) != null) {
			if (dbRecord.contains("}"))
				break;
			sbuf.append(dbRecord);
		}
		sbuf.append('}');
		System.out.println(sbuf.toString());
		StreamItParserFE parse = new StreamItParserFE(new StreamItLex(new StringReader(sbuf.toString())), null, false,
				null);
		parse.setFilename("Generated");
		Statement s = null;
		try {
			s = parse.block();
		} catch (RecognitionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TokenStreamException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		final List<Expression> params = new ArrayList<Expression>();
		params.add(new ExprVar(s, "IN_0"));
		params.add(new ExprVar(s, "IN_1"));
		// params.add(new ExprVar(s, "_OUT"));
		final List<Parameter> formals = new ArrayList<Parameter>();
		formals.add(new Parameter(s, TypePrimitive.inttype, "COUNT"));
		Type t = new TypeArray(TypePrimitive.floattype, new ExprVar(s, "COUNT"));
		formals.add(new Parameter(s, t, "INPUT"));
		formals.add(new Parameter(s, TypePrimitive.floattype, "_OUT", Parameter.OUT));
		final ExprVar basein = new ExprVar(s, "INPUT");

		FEReplacer fer = new FEReplacer() {
			Set<String> s = new HashSet<String>();

			public Object visitStmtReturn(StmtReturn ret) {
				return new StmtAssign(new ExprVar(ret, "_OUT"), (Expression) ret.getValue().accept(this));
			}

			public Object visitExprVar(ExprVar ev) {
				String nm = ev.getName();
				if (nm.length() > 6 && nm.substring(0, 6).equals("INPUT_")) {
					return new ExprArrayRange(basein, new ExprConstInt(Integer.parseInt(nm.substring(6))));
				}
				return ev;
			}

		};
		s = (Statement) s.accept(fer);
		Function newfun = Function.creator(s.getCx(), "_tfun" + funlist.size(), Function.FcnType.Generator).body(s)
				.params(formals).returnType(TypePrimitive.floattype).create();
		funlist.add(newfun);

		valMap.put(vname, new ExprFunCall(s, newfun.getName(), params));
	}

	public void read_ftml_program_language(LineNumberReader in) throws IOException {
		String line = null;

		CodeBlock code_block = new CodeBlock();
		
		while ((line = in.readLine()) != null) {
			System.out.println("LINE: " + line);
			Matcher m = Pattern.compile("(\"[^\n\"]*\"|[a-zA-Z_][a-zA-Z_0-9]*|=|\\.|:|;|\\(|\\)|\\[|\\]|,|\\{|\\})")
					.matcher(line);
			Vector<String> tokens = new Vector<String>();

			String concat = new String();
			System.out.print("TOKE: ");
			while (m.find()) {
				String token = m.group(1);
				System.out.print("<| " + m.group(1) + " |>");
				tokens.add(token);
				concat = concat.concat(token);
			}
			System.out.println();
			
			System.out.println("CONCAT: " +  concat);
			System.out.println("ORIGIN: " + line.replace(" ", ""));
			
			
			assert (concat.contentEquals(line.replace(" ", "")));


//			assert (false);

//			Tokens tokens = new Tokens(line);
//
//			if(tokens.length == 1)
//			{
//				code_block.push_back(new ExpressionLine(tokens[0]));
//			}
//			else if(tokens.length == 2)
//			{
//				code_block.push_back(new AssignmentLine(tokens[0], tokens[1]));
//			}
//			else
//			{
//				assert(false);
//			}
		}

		System.out.println(dir.getAbsolutePath());

		System.out.println("DONE");

		assert (false);
	}

	public void loadFromStream(LineNumberReader in) throws IOException {
		String dbRecord = null;
		valMap = new HashMap<String, Expression>();
		funlist = new ArrayList<Function>();
		while ((dbRecord = in.readLine()) != null) {
			StringTokenizer st = new StringTokenizer(dbRecord, "\t ");
			String vname = st.nextToken();
			String sval = st.nextToken();
			if (sval.equals("(")) {
				parseExpression(vname, dbRecord);
			} else {
				if (sval.equals("{")) {
					parseCompound(vname, in);
				} else {
					if (sval.contains(".")) {
						double val = Double.parseDouble(sval);
						valMap.put(vname, new ExprConstFloat((FEContext) null, val));
					} else {
						int val = Integer.parseInt(sval);
						valMap.put(vname, new ExprConstInt(val));
					}
				}
			}
		}
	}

	protected ExprConstant getVal(FENode node, String var, Type t) {
		Expression val = valMap.get(var);

		if (val == null) {
			val = new ExprConstInt(node, 0);
		}

		if (t.equals(TypePrimitive.chartype)) {
			return ExprConstChar.createFromInt(val.getIValue());
		} else {
			if (t.equals(TypePrimitive.floattype) || t.equals(TypePrimitive.doubletype)) {
				if (val instanceof ExprConstInt) {
					return new ExprConstFloat((double) val.getIValue());
				}
			}
			return (ExprConstant) val;
		}

	}

	public ExprConstant popValueForNode(FENode node, Type t) {
		String name = holeNamer.getName(node);
		return getVal(node, name, t);
	}

	public Expression generatorHole(String name) {
		return valMap.get(name);
	}

	public void capStarSizes(int size) {
		starSizesCaped = size;
	}
}