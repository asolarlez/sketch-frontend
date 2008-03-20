/**
 *
 */
package streamit.frontend.nodes;

import java.util.List;

/**
 * Represents the sugar construct:
 *
 *   insert S into { s1; s2; ...; sn; }
 *
 * The semantics of this construct is to execute S at exactly one of the
 * following points, chosen nondeterministically:
 *
 *  0. before s1
 *  1. after s1
 *  2. after s2
 *  ...
 *  n. after sn
 *
 * @author Chris Jones
 */
public class StmtInsertBlock extends Statement {

	protected Statement insert;
	protected StmtBlock into;

	/** Insert _INSERT into the block _INTO. */
	public StmtInsertBlock (FENode context, List<Statement>_insert, List<Statement> _into) {
		super (context);

		insert = new StmtBlock (_insert.get (0), _insert);
		into = new StmtBlock (_into.get (0), _into);
	}

	/**
	 * @param context
	 * @deprecated
	 */
	public StmtInsertBlock (FEContext context, List<Statement> _insert, List<Statement> _into) {
		super (context);

		insert = new StmtBlock (_insert.get (0), _insert);
		into = new StmtBlock (_into.get (0), _into);
	}

	public StmtInsertBlock (FENode context, Statement _insert, StmtBlock _into) {
		super (context);

		insert = _insert;
		into = _into;
	}

	public Object accept (FEVisitor v) {
		return v.visitStmtInsertBlock(this);
	}

	public Statement getInsertStmt () { return insert; }
	public StmtBlock getIntoBlock ()  { return into; }


    public String toString () {
    	return "insert {\n" + getInsertStmt () + "\n} into {\n" + getIntoBlock () + "\n}";
    }
}
