package streamit.frontend.nodes;

import java.util.HashMap;
import java.util.Map;

public class ExprChoiceUnary extends Expression {
	public static final int NEG 	= 1<<1;
	public static final int NOT 	= 1<<2;
	public static final int BNOT 	= 1<<3;
	public static final int PREINC 	= 1<<4;
	public static final int PREDEC 	= 1<<5;
	public static final int POSTINC	= 1<<6;
	public static final int POSTDEC = 1<<7;
	public static final int NONE 	= 1<<31;

	private static final Map<Integer, String> opMap = new HashMap<Integer, String> ();

	private int ops;
	private Expression expr;

	public ExprChoiceUnary (int ops, Expression expr) {
		super (expr);

		if (0 == opMap.size ())  init ();

		assert opsConsistent ();

		this.expr = expr;
		this.ops = ops;
	}

	public Expression getExpr () {  return expr;  }
	public int getOps () {  return ops;  }

	public boolean postfix () {
		return (ops & POSTINC) != 0 || (ops & POSTDEC) != 0;
	}

	public boolean opOptional () {
		return (ops & NONE) != 0;
	}

	public boolean opsConsistent () {
		if ((ops & POSTINC) != 0 || (ops & POSTDEC) != 0) {
			return (ops & PREINC) == 0 && (ops & PREDEC) == 0;
		} else if ((ops & PREINC) != 0 || (ops & PREDEC) != 0) {
			return (ops & POSTINC) == 0 && (ops & POSTDEC) == 0;
		} else {
			return true;
		}
	}

	public String opsToString () {
		String opStr = "";
		for (Integer op : opMap.keySet ())
			if ((ops & op) != 0)
				opStr += (opStr.length()>0?"|":"")+ opMap.get (op);
		return "("+ opStr +")"+ (opOptional () ? "?" : "");
	}

	public String toString () {
		return "("+
			(postfix () ? (expr + opsToString ()) : (opsToString () + expr))
			  +")";
	}

	@Override
	public Object accept (FEVisitor v) {
		return null; //v.visitExprChoiceUnary (v);
	}

	private static void init () {
		opMap.put (NEG, "-");
		opMap.put (NOT, "!");
		opMap.put (BNOT, "~");
		opMap.put (PREINC, "++");
		opMap.put (PREDEC, "--");
		opMap.put (POSTINC, "++");
		opMap.put (POSTDEC, "--");
	}
}
