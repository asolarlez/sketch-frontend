/**
 *
 */
package sketch.compiler.passes.lowering;
import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.SymbolTable;
import sketch.compiler.ast.core.exprs.*;
import sketch.compiler.ast.core.exprs.ExprChoiceSelect.SelectChain;
import sketch.compiler.ast.core.exprs.ExprChoiceSelect.SelectField;
import sketch.compiler.ast.core.exprs.ExprChoiceSelect.SelectOrr;
import sketch.compiler.ast.core.exprs.ExprChoiceSelect.SelectorVisitor;
import sketch.util.ControlFlowException;

/**
 * A toolbox of queries about properties of expressions.
 *
 * @author <a href="mailto:cgjones@cs.berkeley.edu">Chris Jones</a>
 */
public class ExprTools {
	/**
	 * For now, side-effect free is defined as:
	 *   - no function calls
	 *   - no unary increment/decrement expressions
	 */
	public static boolean isSideEffectFree (Expression e) {
		try {
		e.accept (new FEReplacer () {
			private void abortIf (boolean cond) {
				if (cond)  throw new ControlFlowException ("has side effects");
			}

			public Object visitExprChoiceUnary (ExprChoiceUnary exp) {
				super.visitExprChoiceUnary (exp);
				abortIf (exp.hasSideEffects ());
				return exp;
			}

			public Object visitExprUnary (ExprUnary exp) {
				super.visitExprUnary (exp);
				abortIf (exp.hasSideEffects ());
				return exp;
			}

			public Object visitExprFunCall (ExprFunCall exp) {
				abortIf (true);
				return exp;
			}
		});
		} catch (ControlFlowException cfe) {
			return false;
		}
		return true;
	}

	public static int numGlobalReads (Expression e, SymbolTable symtab) {
		return (Integer) e.accept (new SymbolTableVisitor (symtab) {
			public Object visitExprAlt (ExprAlt exp) {
				return Math.max ((Integer) exp.getThis ().accept (this),
								 (Integer) exp.getThat ().accept (this));
			}

			public Object visitExprArrayInit (ExprArrayInit exp) {
				int count = 0;
				for (Expression e : exp.getElements ())
					count += (Integer) e.accept (this);
				return count;
			}

			public Object visitExprArrayRange (ExprArrayRange exp) {
				int count = (Integer) exp.getBase ().accept (this);
				for (Expression e : exp.getArrayIndices ())
					count += (Integer) e.accept (this);
				return count;
			}

			public Object visitExprBinary (ExprBinary exp) {
				return ((Integer) exp.getLeft ().accept (this))
					+ ((Integer) exp.getRight ().accept (this));
			}

			public Object visitExprChoiceBinary (ExprChoiceBinary exp) {
				return ((Integer) exp.getLeft ().accept (this))
					+ ((Integer) exp.getRight ().accept (this));
			}

			private boolean exprVarsOnly (Expression exp) {
				try {
					(new FEReplacer () {
						public Expression doExpression (Expression e) {
							if (!(e instanceof ExprVar
								  || e instanceof ExprAlt
								  || e instanceof ExprParen
								  || e instanceof ExprRegen))
								throw new ControlFlowException ("!exprvar");
							else
								return super.doExpression (e);
						}
					}).doExpression (exp);
					return true;
				} catch (ControlFlowException cfe) {
					return false;
				}
			}

			public Object visitExprChoiceSelect (ExprChoiceSelect ecs) {
				int numFieldReads =
					((Integer) ecs.getField ().accept (new SelectorVisitor () {
						public Object visit (SelectOrr so) {
							return Math.max ((Integer) so.getThis ().accept (this),
											 (Integer) so.getThat ().accept (this));
						}
						public Object visit (SelectChain sc) {
							return ((Integer) sc.getFirst ().accept (this))
								+ ((Integer) sc.getNext ().accept (this));
						}
						public Object visit (SelectField sf) {
							return 1;
						}
					}));

				return numFieldReads
					+ (exprVarsOnly (ecs.getObj ()) ? 0
							: (Integer) ecs.getObj ().accept (this));
			}

			public Object visitExprChoiceUnary (ExprChoiceUnary exp) {
				return exp.getExpr ().accept (this);
			}

			public Object visitExprConstChar (ExprConstChar exp) { return 0; }
			public Object visitExprConstFloat (ExprConstFloat exp) { return 0; }
			public Object visitExprConstInt (ExprConstInt exp) { return 0; }
			public Object visitExprConstStr (ExprConstStr exp) { return 0; }

			public Object visitExprField (ExprField exp) {
				return 1
					+ (exprVarsOnly (exp.getLeft ()) ? 0
							: (Integer) exp.getLeft ().accept (this));
			}

			// XXX: the semantics of this is a little weird, as we don't know
			// a priori how many global reads the function itself will make
			public Object visitExprFunCall (ExprFunCall exp) {
				throw new UnsupportedOperationException ("can't count global reads in function calls");
			}
			// see visitExprFunCall ()
			public Object visitExprNew (ExprNew exp) {
				throw new UnsupportedOperationException ("can't count global reads in constructor calls");
			}

			public Object visitExprNullPtr (ExprNullPtr exp) { return 0; }

			public Object visitExprParen (ExprParen exp) {
				return exp.getExpr ().accept (this);
			}

			public Object visitExprRegen (ExprRegen exp) {
				return exp.getExpr ().accept (this);
			}

			public Object visitExprStar (ExprStar exp) { return 0; }

			public Object visitExprTernary (ExprTernary exp) {
				return ((Integer) exp.getA ().accept (this))
					+ Math.max ((Integer) exp.getB ().accept (this),
								(Integer) exp.getC ().accept (this));
			}

			public Object visitExprTypeCast (ExprTypeCast exp) {
				return exp.getExpr ().accept (this);
			}

			public Object visitExprUnary (ExprUnary exp) {
				return exp.getExpr ().accept (this);
			}

			public Object visitExprVar (ExprVar exp) {
				return isGlobal (exp) ? 1 : 0;
			}
		});
	}
}
