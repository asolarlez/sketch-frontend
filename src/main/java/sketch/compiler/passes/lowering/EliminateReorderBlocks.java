package streamit.frontend.passes;

import java.util.Collections;
import java.util.List;

import streamit.frontend.nodes.FEReplacer;
import streamit.frontend.nodes.Statement;
import streamit.frontend.nodes.StmtInsertBlock;
import streamit.frontend.nodes.StmtReorderBlock;
import streamit.frontend.nodes.StmtBlock;

/**
 *
 * Replaces the Reorder nodes with equivalent 'insert' blocks.
 *
 * @author asolar
 *
 */
public class EliminateReorderBlocks extends FEReplacer {
	 public Object visitStmtReorderBlock (StmtReorderBlock srb) {
		 List<Statement> B = srb.getStmts ();

		 if (1 >= B.size ())
			 return srb.getBlock ().accept (this);	// no reorder for 1 stmt

		 StmtBlock into = new StmtBlock (srb,
				 Collections.singletonList ((Statement)
						 (new StmtReorderBlock (srb, B.subList (1, B.size ()))).accept (this)));
		 Statement insert = (Statement) B.get (0).accept (this);

		 return new StmtInsertBlock (srb, insert, into);
	 }
}
