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
import streamit.frontend.nodes.StmtBlock;
import streamit.frontend.nodes.StmtIfThen;
import streamit.frontend.nodes.StmtInsertBlock;
import streamit.frontend.nodes.StmtVarDecl;
import streamit.frontend.nodes.TempVarGen;
import streamit.frontend.nodes.TypePrimitive;
import streamit.misc.Misc;

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
 * There is a special case for nested 'insert' blocks:
 *
 *   insert S1 into { insert S2 into { ... } }
 *
 * @author <a href="mailto:cgjones@cs.berkeley.edu">Chris Jones</a>
 */
public class EliminateInsertBlocks extends FEReplacer {
	protected TempVarGen varGen;
	/**
	 * The 'depth' and 'header' variables are a bit of a hack.  When
	 * rewriting nested 'insert' blocks, we need to introduce two new
	 * statements.  However, these statements shouldn't be part of the
	 * block in which a stmt is inserted.  So all these introduced stmts are
	 * collected into 'header' and only emitted when 'depth' again reaches 0.
	 */
	protected int depth;
	protected List<Statement> header;

	public EliminateInsertBlocks (TempVarGen _varGen) {
		varGen = _varGen;
	}

	public Object visitStmtInsertBlock (StmtInsertBlock sib) {
		if (0 == depth++)
			header = new ArrayList<Statement> ();

		String where = varGen.nextVar ("_ins_where");
		Statement S = (Statement) sib.getInsertStmt ().accept (this);

		// Rewrite 'into' body
		List<Statement> oldB = sib.getIntoBlock ().getStmts ();
		if (1 == oldB.size () && (oldB.get (0) instanceof StmtInsertBlock))
			// Special case for immediately-nested 'insert' blocks
			oldB = ((StmtBlock) oldB.get (0).accept (this)).getStmts ();
		else
			oldB = ((StmtBlock) sib.getIntoBlock ().accept (this)).getStmts ();

		String maxVal = ""+ oldB.size ();
		int nBits = Misc.nBitsBinaryRepr (oldB.size ()+1);

		header.add (0,
			new StmtAssert (sib,
				new ExprBinary (
						new ExprBinary (ExprConstInt.zero, "<=",
										new ExprVar (sib, where)),
						"&&",
						new ExprBinary (new ExprVar (sib, where), "<=",
										ExprConstant.createConstant (sib, maxVal))
								)));
		header.add (0,
				new StmtVarDecl (sib, TypePrimitive.inttype, where, new ExprStar (sib, nBits)));

		List<Statement> newB = new ArrayList<Statement> ();
		for (int i = 0; i < oldB.size (); ++i) {
			Statement si = (Statement) oldB.get (i);

			newB.add (new StmtIfThen (S,
						new ExprBinary (new ExprVar (S, where),
										"==", ExprConstant.createConstant (S, ""+i)),
						S, null));
			newB.add (si);
		}
		newB.add (new StmtIfThen (S,
					new ExprBinary (new ExprVar (S, where),
								    "==", ExprConstant.createConstant (S, maxVal)),
					S, null));

		if (0 == --depth)
			newB.addAll (0, header);

		return new StmtBlock (sib, newB);
	}
}
