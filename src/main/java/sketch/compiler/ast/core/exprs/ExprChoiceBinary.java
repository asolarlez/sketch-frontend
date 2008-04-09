/**
 *
 */
package streamit.frontend.nodes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

	private static final Set<OpInfo> opinfo = new HashSet<OpInfo> ();

	private int ops;
	private Expression left, right;

	public ExprChoiceBinary (Expression left, int ops, Expression right) {
		this (left, left, ops, right);
	}

	public ExprChoiceBinary (FENode cx, Expression left, int ops, Expression right) {
		super (cx);

		if (0 == opinfo.size ())  init ();

		this.left = left;
		this.right = right;
		this.ops = ops;
	}

	public Expression getLeft () { return left; }
	public Expression getRight () { return right; }
	public int getOps () { return ops; }

	public boolean hasComparison () {
		return ((ops & EQ) != 0)   || ((ops & NEQ) != 0)  || ((ops & LT) != 0)
			|| ((ops & LTEQ) != 0) || ((ops & GT) != 0)   || ((ops & GTEQ) != 0);
	}

	public List<Integer> opsAsExprBinaryOps () {
		List<Integer> ret = new ArrayList<Integer> ();
		for (OpInfo o : opinfo)
			if (0 != (ops & o.op))
				ret.add (o.exprBinaryOp);
		return ret;
	}

	public String opsToString () {
		String opStr = "";
		for (OpInfo o : opinfo)
			if ((ops & o.op) != 0)
				opStr += (opStr.length()>0?"|":"")+ o.opStr;
		return "("+ opStr +")";
	}

	public String toString () {
		return "("+ left +" "+ opsToString () +" "+ right +")";
	}

	@Override
	public Object accept (FEVisitor v) {
		return v.visitExprChoiceBinary (this);
	}

	private static void init () {
		opinfo.add (new OpInfo (ADD, ExprBinary.BINOP_ADD, "+"));
		opinfo.add (new OpInfo (SUB, ExprBinary.BINOP_SUB, "-"));
		opinfo.add (new OpInfo (MUL, ExprBinary.BINOP_MUL, "*"));
		opinfo.add (new OpInfo (DIV, ExprBinary.BINOP_DIV, "/"));
		opinfo.add (new OpInfo (MOD, ExprBinary.BINOP_MOD, "%"));
		opinfo.add (new OpInfo (LAND, ExprBinary.BINOP_AND, "&&"));
		opinfo.add (new OpInfo (LOR, ExprBinary.BINOP_OR, "||"));
		opinfo.add (new OpInfo (BAND, ExprBinary.BINOP_BAND, "&"));
		opinfo.add (new OpInfo (BOR, ExprBinary.BINOP_BOR, "\\|"));
		opinfo.add (new OpInfo (BXOR, ExprBinary.BINOP_BXOR, "^"));
		opinfo.add (new OpInfo (EQ, ExprBinary.BINOP_EQ, "=="));
		opinfo.add (new OpInfo (NEQ, ExprBinary.BINOP_NEQ, "!="));
		opinfo.add (new OpInfo (LT, ExprBinary.BINOP_LT, "<"));
		opinfo.add (new OpInfo (LTEQ, ExprBinary.BINOP_LE, "<="));
		opinfo.add (new OpInfo (GT, ExprBinary.BINOP_GT, ">"));
		opinfo.add (new OpInfo (GTEQ, ExprBinary.BINOP_GE, ">="));
		opinfo.add (new OpInfo (LSHFT, ExprBinary.BINOP_LSHIFT, "<<"));
		opinfo.add (new OpInfo (RSHFT, ExprBinary.BINOP_RSHIFT, ">>"));
	}

	private static class OpInfo {
		int op, exprBinaryOp;
		String opStr;
		OpInfo (int op, int ebOp, String str) {
			this.op = op;  exprBinaryOp = ebOp;  opStr = str;
		}
		@Override
		public int hashCode () {  return op;  }
	}
}
