package streamit.frontend.nodes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ExprChoiceUnary extends Expression {
	public static final int NEG 	= 1<<1;
	public static final int NOT 	= 1<<2;
	public static final int BNOT 	= 1<<3;
	public static final int PREINC 	= 1<<4;
	public static final int PREDEC 	= 1<<5;
	public static final int POSTINC	= 1<<6;
	public static final int POSTDEC = 1<<7;
	public static final int NONE 	= 1<<31;

	private static final Set<OpInfo> opinfo = new HashSet<OpInfo> ();

	private int ops;
	private Expression expr;

	public ExprChoiceUnary (int ops, Expression expr) {
		this (expr, ops, expr);
	}

	public ExprChoiceUnary (FENode cx, int ops, Expression expr) {
		super (cx);

		if (0 == opinfo.size ())  init ();

		assert opsConsistent ();

		this.expr = expr;
		this.ops = ops;
	}

	public Expression getExpr () {  return expr;  }
	public int getOps () {  return ops;  }

    public boolean hasIncrOrDecr () {
        return 0 != (ops & PREINC)
        	|| 0 != (ops & POSTINC)
        	|| 0 != (ops & PREDEC)
        	|| 0 != (ops & POSTDEC);
    }

    public boolean hasSideEffects () {
    	return hasIncrOrDecr ();
    }

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

	public List<Integer> opsAsExprUnaryOps () {
		List<Integer> ret = new ArrayList<Integer> ();
		for (OpInfo o : opinfo)
			if (0 != (ops & o.op))
				ret.add (o.exprUnaryOp);
		return ret;
	}

	public String opsToString () {
		String opStr = "";
		for (OpInfo o : opinfo)
			if ((ops & o.op) != 0)
				opStr += (opStr.length()>0?"|":"")+ o.strOp;
		return "("+ opStr +")"+ (opOptional () ? "?" : "");
	}

	public String toString () {
		return "("+
			(postfix () ? (expr + opsToString ()) : (opsToString () + expr))
			  +")";
	}

	@Override
	public Object accept (FEVisitor v) {
		return v.visitExprChoiceUnary (this);
	}

	private static void init () {
		opinfo.add (new OpInfo (NEG, ExprUnary.UNOP_NEG, "-"));
		opinfo.add (new OpInfo (NOT, ExprUnary.UNOP_NOT, "!"));
		opinfo.add (new OpInfo (BNOT, ExprUnary.UNOP_BNOT, "~"));
		opinfo.add (new OpInfo (PREINC, ExprUnary.UNOP_PREINC, "++"));
		opinfo.add (new OpInfo (PREDEC, ExprUnary.UNOP_PREDEC, "--"));
		opinfo.add (new OpInfo (POSTINC, ExprUnary.UNOP_POSTINC, "++"));
		opinfo.add (new OpInfo (POSTDEC, ExprUnary.UNOP_POSTDEC, "--"));
	}

	private static class OpInfo {
		int op, exprUnaryOp;
		String strOp;
		OpInfo (int op, int euOp, String sop) {
			this.op = op; exprUnaryOp = euOp;  strOp = sop;
		}
		@Override
		public int hashCode () {  return op;  }
	}
}
