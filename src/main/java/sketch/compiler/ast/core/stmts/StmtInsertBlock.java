/**
 *
 */
package sketch.compiler.ast.core.stmts;
import java.util.List;

import sketch.compiler.ast.core.FEContext;
import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.FEVisitor;

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

    @Override
    public int size() {
        int sz_isrt = insert == null ? 0 : insert.size();
        int sz_into = into == null ? 0 : into.size();
        return sz_isrt + sz_into;
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
