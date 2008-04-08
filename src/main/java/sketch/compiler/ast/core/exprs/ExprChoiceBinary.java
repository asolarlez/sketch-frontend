/**
 *
 */
package streamit.frontend.nodes;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:cgjones@cs.berkeley.edu">Chris Jones</a>
 *
 */
public class ExprChoiceBinary extends Expression {
	public static final int ADD 	= 1<<1;
	public static final int SUB 	= 1<<2;
	public static final int MUL 	= 1<<3;
	public static final int DIV 	= 1<<4;
	public static final int MOD 	= 1<<5;
	public static final int LAND 	= 1<<6;
	public static final int LOR 	= 1<<7;
	public static final int BAND 	= 1<<8;
	public static final int BOR 	= 1<<9;
	public static final int BXOR 	= 1<<10;
	public static final int EQ 	= 1<<11;
	public static final int NEQ 	= 1<<12;
	public static final int LT 	= 1<<13;
	public static final int LTEQ 	= 1<<14;
	public static final int GT 	= 1<<15;
	public static final int GTEQ 	= 1<<16;
	public static final int LSHFT 	= 1<<17;
	public static final int RSHFT 	= 1<<18;

	private static final Map<Integer, String> opMap = new HashMap<Integer, String> ();

	private int ops;
	private Expression left, right;

	public ExprChoiceBinary (Expression left, int ops, Expression right) {
		super (left);

		if (0 == opMap.size ())  init ();

		this.left = left;
		this.right = right;
		this.ops = ops;
	}

	public String opsToString () {
		String opStr = "";
		for (Integer op : opMap.keySet ())
			if ((op & ops) != 0)
				opStr += (opStr.length()>0?"|":"")+ opMap.get (op);
		return "("+ opStr +")";
	}

	public String toString () {
		return "("+ left +" "+ opsToString () +" "+ right +")";
	}

	@Override
	public Object accept (FEVisitor v) {
		return null; //v.visitExprChoiceBinary (this);
	}

	private static void init () {
		opMap.put (ADD, "+");
		opMap.put (SUB, "-");
		opMap.put (MUL, "*");
		opMap.put (DIV, "/");
		opMap.put (MOD, "%");
		opMap.put (LAND, "&&");
		opMap.put (LOR, "||");
		opMap.put (BAND, "&");
		opMap.put (BOR, "\\|");
		opMap.put (BXOR, "^");
		opMap.put (EQ, "==");
		opMap.put (NEQ, "!=");
		opMap.put (LT, "<");
		opMap.put (LTEQ, "<=");
		opMap.put (GT, ">");
		opMap.put (GTEQ, ">=");
		opMap.put (LSHFT, "<<");
		opMap.put (RSHFT, ">>");
	}
}
