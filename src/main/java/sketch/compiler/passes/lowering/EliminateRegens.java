/**
 *
 */
package streamit.frontend.passes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;

import streamit.frontend.nodes.ExprAlt;
import streamit.frontend.nodes.ExprArrayRange;
import streamit.frontend.nodes.ExprBinary;
import streamit.frontend.nodes.ExprChoiceBinary;
import streamit.frontend.nodes.ExprChoiceSelect;
import streamit.frontend.nodes.ExprChoiceUnary;
import streamit.frontend.nodes.ExprConstBoolean;
import streamit.frontend.nodes.ExprConstInt;
import streamit.frontend.nodes.ExprConstant;
import streamit.frontend.nodes.ExprField;
import streamit.frontend.nodes.ExprFunCall;
import streamit.frontend.nodes.ExprNullPtr;
import streamit.frontend.nodes.ExprParen;
import streamit.frontend.nodes.ExprRegen;
import streamit.frontend.nodes.ExprStar;
import streamit.frontend.nodes.ExprTernary;
import streamit.frontend.nodes.ExprUnary;
import streamit.frontend.nodes.ExprVar;
import streamit.frontend.nodes.Expression;
import streamit.frontend.nodes.FENode;
import streamit.frontend.nodes.FEReplacer;
import streamit.frontend.nodes.Statement;
import streamit.frontend.nodes.StmtAssert;
import streamit.frontend.nodes.StmtAssign;
import streamit.frontend.nodes.StmtBlock;
import streamit.frontend.nodes.StmtIfThen;
import streamit.frontend.nodes.StmtVarDecl;
import streamit.frontend.nodes.TempVarGen;
import streamit.frontend.nodes.TypePrimitive;
import streamit.frontend.nodes.ExprChoiceSelect.SelectChain;
import streamit.frontend.nodes.ExprChoiceSelect.SelectField;
import streamit.frontend.nodes.ExprChoiceSelect.SelectOrr;
import streamit.frontend.nodes.ExprChoiceSelect.SelectorVisitor;
import streamit.misc.Misc;

/**
 * @author <a href="mailto:cgjones@cs.berkeley.edu">Chris Jones</a>
 */
public class EliminateRegens extends SymbolTableVisitor {
	TempVarGen varGen;
	boolean lhs = false;

	public EliminateRegens (TempVarGen varGen)  {
		super (null);
		this.varGen = varGen;
	}

	public Object visitExprRegen (ExprRegen er) {
		List<Expression> exps = explodeRegen (er);
		if (exps.size () == 1)
			return exps.get (0);

		ExprVar which = makeNDChoice (exps.size (), er, "_whichexpr");
		return toConditional (which, exps, 0);
	}

	private Expression toConditional (ExprVar which, List<Expression> exps, int i) {
		if ((i+1) == exps.size ())
			return exps.get (i);
		else {
			Expression cond = new ExprBinary (which, "==", ExprConstant.createConstant (which, ""+i));
			return new ExprTernary ("?:", cond, exps.get (i), toConditional (which, exps, i+1));
		}
	}

	public Object visitStmtAssign (StmtAssign sa) {
		if (sa.getLHS () instanceof ExprRegen)
			return translateRegenAssn ((ExprRegen) sa.getLHS (),
					sa.getRHS ()/*(Expression) sa.getRHS ().accept (this)*/);
		else
			return super.visitStmtAssign (sa);
	}

	public Object translateRegenAssn (ExprRegen lhs, Expression rhs) {
		List<Expression> lhses = explodeRegen (lhs);
		if (lhses.size () == 1)
			return new StmtAssign (lhses.get (0), (Expression) rhs.accept (this));

		// General idea:
		//   1.) Build set of possible LHSes
		//   2.) [OPTIONAL] Minimize possible LHSes
		//   3.) [OPTIONAL] Save the RHS to a temporary variable; dangerous
		//		 because it might introduce new program behaviors
		//   4.) Make a "switch" statement to choose to which LHS to assign
		ExprVar whichLhs = makeNDChoice (lhses.size (), lhs, "_whichlhs");

		for (int i = 0; i < lhses.size (); ++i) {
			Expression l = lhses.get (i);
			Expression cond = new ExprBinary (whichLhs, "==",
					ExprConstant.createConstant (whichLhs, ""+ i));

			List<Statement> oldStmts = newStatements;
			newStatements = new ArrayList<Statement> ();

			newStatements.add (new StmtAssign (l, (Expression) rhs.accept (this)));
			StmtIfThen s = new StmtIfThen (lhs, cond, new StmtBlock (lhs, newStatements), null);

			newStatements = oldStmts;

			addStatement (s);
		}

		return null;
	}

	protected ExprVar makeNDChoice (int n, FENode cx, String pfx) {
		ExprVar which = new ExprVar (cx, varGen.nextVar (pfx));

		addStatement (new StmtVarDecl (cx, TypePrimitive.inttype, which.getName (), null));
		addStatement (new StmtAssign (which, new ExprStar (which, Misc.nBitsBinaryRepr (n))));
		addStatement (new StmtAssert (
			new ExprBinary (
				new ExprBinary (ExprConstInt.zero, "<=", which),
				"&&",
				new ExprBinary (which, "<", ExprConstant.createConstant (which, ""+ n)))));

		return which;
	}

	protected List<Expression> explodeRegen (ExprRegen r) {
		// Steps:
		//   1.) Find all paths through regen, keeping ExprParens
		//   2.) Fix up associativity problems
		//   3.) Remove ExprParens
		List<Expression> exps = (List<Expression>) r.accept (new DoExprGen ());
		for (int i = 0; i < exps.size (); ++i) {
			Expression e = exps.get (i);
			e = (Expression) e.accept (new FixOpPrec ());
			e = (Expression) e.accept (new StripExprParens ());
			exps.set (i, e);
		}
		return exps;
	}

	@SuppressWarnings("unchecked")
	private class DoExprGen extends FEReplacer {
		public Object visitExprRegen (ExprRegen er) {
			return er.getExpr ().accept (this);
		}

		public Object visitExprAlt (ExprAlt ea) {
			List<Expression> e = (List<Expression>) ea.getThis ().accept (this);
			e.addAll ((List<Expression>) ea.getThat ().accept (this));
			return e;
		}

		@Override
		public Object visitExprArrayRange(ExprArrayRange ear){
			List<Expression> exps = new ArrayList<Expression> ();
			List<Expression> bases = (List<Expression>) ear.getBase().accept(this);
			List<Expression> offsets = (List<Expression>) ear.getOffset().accept(this);
			for(Expression base: bases)
				for(Expression offset: offsets){
					exps.add(new ExprArrayRange(base, offset));
				}
			return exps;
		}
		
		private void fcconstruct(ExprFunCall efc, List<Expression> plist, int lidx, List<Expression> actuals, List<Expression> exprs){
			if(plist.size() > lidx){
				Expression cur = plist.get(lidx);
				List<Expression> tmp = (List<Expression>)cur.accept(this);
				for(Expression e: tmp){
					List<Expression> nl = new ArrayList<Expression>(actuals);
					nl.add(e);
					fcconstruct(efc, plist, lidx+1, nl, exprs);
				}
			}else{
				exprs.add(new ExprFunCall(efc, efc.getName(), actuals));				
			}
		}
		
		@Override
		public Object visitExprFunCall(ExprFunCall efc){
			List<Expression> plist = efc.getParams();
			List<Expression> exprs = new ArrayList<Expression>();
			fcconstruct(efc, plist,0, new ArrayList<Expression>(), exprs);			
			return exprs;
		}
		
		public Object visitExprChoiceBinary (ExprChoiceBinary ecb) {
			List<Expression> lefts = (List<Expression>) ecb.getLeft ().accept (this);
			List<Expression> rights = (List<Expression>) ecb.getRight ().accept (this);

			List<Expression> exps = new ArrayList<Expression> ();
			for (Expression left : lefts)
				for (Expression right : rights)
					for (int op : ecb.opsAsExprBinaryOps ())
						exps.add (new ExprBinary (op, left, right));
			return exps;
		}

		public Object visitExprChoiceUnary (ExprChoiceUnary ecu) {
			List<Expression> exprs = (List<Expression>) ecu.getExpr ().accept (this);

			List<Expression> exps = new ArrayList<Expression> ();
			for (Expression expr : exprs) {
				if (ecu.opOptional ())
					exps.add (expr);
				for (int op : ecu.opsAsExprUnaryOps ())
					exps.add (new ExprUnary (expr, op, expr));
			}
			return exps;
		}

		public Object visitExprChoiceSelect (ExprChoiceSelect ecs) {
			Object o = ecs.accept (new SelectorVisitor () {
				public Object visit (SelectField sf) {
					List<String> f = new ArrayList<String> ();
					Set<List<String>> s = new HashSet<List<String>> ();
					f.add (sf.getField ());
					s.add (f);
					return s;
				}

				public Object visit (SelectOrr so) {
					Set<List<String>> s1 = (Set<List<String>>) so.getThis ().accept (this);
					Set<List<String>> s2 = (Set<List<String>>) so.getThat ().accept (this);
					s1.addAll (s2);
					if (so.getThis ().isOptional () && so.getThat ().isOptional ())
						s1.add (new ArrayList<String> ());
					return s1;
				}

				public Object visit (SelectChain sc) {
					Set<List<String>> sf = (Set<List<String>>) sc.getFirst ().accept (this);
					Set<List<String>> sn = (Set<List<String>>) sc.getNext ().accept (this);
					Set<List<String>> rets = new HashSet<List<String>> ();
					boolean firstOpt = sc.getFirst ().isOptional ();
					boolean nextOpt = sc.getNext ().isOptional ();
					for (List<String> f : sf) {
						if (firstOpt)
							rets.add (f);
						for (List<String> n : sn) {
							List<String> fn = new ArrayList<String> (f);
							fn.addAll (n);
							rets.add (fn);
						}
					}
					for (List<String> n : sn)
						if (nextOpt)
							rets.add (n);
					if (firstOpt && nextOpt)
						rets.add (new ArrayList<String> ());
					return rets;
				}
			});
			Set<List<String>> sf = (Set<List<String>>) o;

			List<Expression> objs = (List<Expression>) ecs.getObj ().accept (this);
			List<Expression> exps = new ArrayList<Expression> ();
			boolean fieldOpt = ecs.getField ().isOptional ();

			for (Expression obj : objs) {
				if (fieldOpt)
					exps.add (obj);
				for (List<String> s : sf) {
					Expression e = obj;
					for (String f : s)
						e = new ExprField (e, f);
					exps.add (e);
				}
			}

			return exps;
		}

		public Object visitExprConstBoolean (ExprConstBoolean exp) {
			List<Expression> exps = new ArrayList<Expression> ();
			exps.add (exp);
			return exps;
		}

		public Object visitExprConstInt (ExprConstInt exp) {
			List<Expression> exps = new ArrayList<Expression> ();
			exps.add (exp);
			return exps;
		}

		public Object visitExprNullPtr (ExprNullPtr exp) {
			List<Expression> exps = new ArrayList<Expression> ();
			exps.add (exp);
			return exps;
		}

		public Object visitExprParen (ExprParen ep) {
			List<Expression> exps = (List<Expression>) ep.getExpr ().accept (this);
			for (int i = 0; i < exps.size (); ++i)
				exps.set (i, new ExprParen (ep, exps.get (i)));
			return exps;
		}

		public Object visitExprTernary (ExprTernary et) {
			List<Expression> as = (List<Expression>) et.getA ().accept (this);
			List<Expression> bs = (List<Expression>) et.getB ().accept (this);
			List<Expression> cs = (List<Expression>) et.getC ().accept (this);

			List<Expression> exps = new ArrayList<Expression> ();
			for (Expression a : as)
				for (Expression b : bs)
					for (Expression c : cs)
						exps.add (new ExprTernary ("?:", a, b, c));
			return exps;
		}

		public Object visitExprStar (ExprStar exp) {
			List<Expression> exps = new ArrayList<Expression> ();
			exps.add (exp);
			return exps;
		}

		public Object visitExprVar (ExprVar exp) {
			List<Expression> exps = new ArrayList<Expression> ();
			exps.add (exp);
			return exps;
		}
	}

	/**
	 * Assumes that:
	 *  - the parser has taken care of prefix/postfix operator
	 *      precedence/associativity, and that
	 *  - all binary operators are left associative.
	 */
	private class FixOpPrec extends FEReplacer {
		class opinfo {
			int op, prec;
			opinfo (int op, int prec) { this.op = op; this.prec = prec; }
		}

		public Object visitExprBinary (ExprBinary eb) {
			// Idea:
			//  1.) Do an inorder traversal of child ExprBinaries to pick up
			// 		full expr, visiting "leaf" nodes along the way
			//  2.) Rebalance the tree
			Queue<opinfo> opQ = new LinkedList<opinfo> ();
			Queue<Expression> opndQ = new LinkedList<Expression> ();
			flatten (opQ, opndQ, eb);

			Stack<opinfo> ops = new Stack<opinfo> ();
			Stack<Expression> opnds = new Stack<Expression> ();
			opQ.add (new opinfo (-1, -1));	// "flush" operator
			while (!opndQ.isEmpty ()) {
				opnds.push (opndQ.remove ());
				opinfo oi = opQ.remove ();
				while (!ops.isEmpty () && oi.prec <= ops.peek ().prec) {
					Expression r = opnds.pop ();
					Expression l = opnds.pop ();
					opnds.push (new ExprBinary (ops.pop ().op, l, r));
				}
				ops.push (oi);
			}
			assert opnds.size () == 1;
			return opnds.pop ();
		}

		void flatten (Queue<opinfo> opQ, Queue<Expression> opndQ, Expression e) {
			if (!(e instanceof ExprBinary))
				opndQ.add ((Expression) e.accept (this));
			else {
				ExprBinary eb = (ExprBinary) e;
				flatten (opQ, opndQ, eb.getLeft ());
				opQ.add (new opinfo (eb.getOp (), eb.getOpPrec ()));
				flatten (opQ, opndQ, eb.getRight ());
			}
		}
	}


	private class StripExprParens extends FEReplacer {
		public Object visitExprParen (ExprParen ep) {
			return ep.getExpr ().accept (this);
		}
	}
}
