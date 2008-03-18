/**
 *
 */
package streamit.frontend.passes;

import java.util.ArrayList;
import java.util.List;

import streamit.frontend.nodes.ExprBinary;
import streamit.frontend.nodes.ExprConstInt;
import streamit.frontend.nodes.ExprConstant;
import streamit.frontend.nodes.ExprStar;
import streamit.frontend.nodes.ExprVar;
import streamit.frontend.nodes.FEReplacer;
import streamit.frontend.nodes.Statement;
import streamit.frontend.nodes.StmtAssert;
import streamit.frontend.nodes.StmtAssign;
import streamit.frontend.nodes.StmtBlock;
import streamit.frontend.nodes.StmtIfThen;
import streamit.frontend.nodes.StmtInsertBlock;
import streamit.frontend.nodes.StmtVarDecl;
import streamit.frontend.nodes.TempVarGen;
import streamit.frontend.nodes.TypePrimitive;

/**
 * Lowers 'insert' blocks as follows:
 *
 *   insert S into { s0; s1; s2; ...; sn; }
 *
 * ==>
 *
 *   int where = ??;
 *   assert 0 <= where <= n+1;
 *   if (0 == where)  S;
 *   s1;
 *   if (1 == where)  S;
 *   s2
 *   ...
 *   if (n == where)  S;
 *   sn;
 *   if (n+1 == where)  S;
 *
 * @author <a href="mailto:cgjones@cs.berkeley.edu">Chris Jones</a>
 */
public class EliminateInsertBlocks extends FEReplacer {
	protected TempVarGen varGen;

	public EliminateInsertBlocks (TempVarGen _varGen) {
		varGen = _varGen;
	}

	public Object visitStmtInsertBlock (StmtInsertBlock sib) {
		String where = varGen.nextVar ("_ins_where");
		Statement S = (Statement) sib.getInsertStmt ().accept (this);
		List<Statement> oldB = sib.getIntoBlock ().getStmts ();
		List<Statement> newB = new ArrayList<Statement> ();
		String maxVal = ""+ oldB.size ();

		addStatement (new StmtVarDecl (sib, TypePrimitive.inttype, where, null));
		addStatement (new StmtAssign (new ExprVar (sib, where), new ExprStar (sib)));
		addStatement (new StmtAssert (sib,
				new ExprBinary (
						new ExprBinary (ExprConstInt.zero, "<=",
										new ExprVar (sib, where)),
						"&&",
						new ExprBinary (new ExprVar (sib, where), "<=",
										ExprConstant.createConstant (sib, maxVal))
								)));

		for (int i = 0; i < oldB.size (); ++i) {
			Statement si = (Statement) oldB.get (i).accept (this);

			newB.add (new StmtIfThen (si,
						new ExprBinary (new ExprVar (si, where),
										"==", ExprConstant.createConstant (si, ""+i)),
						S, null));
			newB.add (si);
		}
		newB.add (new StmtIfThen (sib,
					new ExprBinary (new ExprVar (sib, where),
								    "==", ExprConstant.createConstant (sib, maxVal)),
					S, null));

		return new StmtBlock (sib, newB);
	}
}
