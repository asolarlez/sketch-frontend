/**
 *
 */
package streamit.frontend.passes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import streamit.frontend.nodes.FEReplacer;
import streamit.frontend.nodes.Program;
import streamit.frontend.nodes.Statement;
import streamit.frontend.nodes.StmtAssert;
import streamit.frontend.nodes.StmtAssign;
import streamit.frontend.nodes.StmtAtomicBlock;
import streamit.frontend.nodes.StmtBlock;
import streamit.frontend.nodes.StmtEmpty;
import streamit.frontend.nodes.StmtExpr;
import streamit.frontend.nodes.StmtReturn;
import streamit.frontend.parallelEncoder.BreakParallelFunction;
import streamit.frontend.parallelEncoder.ExtractPreParallelSection;

/**
 * Converts runs of statements with only local effects, plus at most statement
 * with global effects, into a single atomic block.  This pass will also
 * incidentally flatten nested statement blocks.
 *
 * The new atomic block is then labeled with the tag of the last statement to
 * be executed.
 *
 * This class cowardly only merges statements within a basic block.  It will,
 * however, merge adjacent (non-conditional) atomic blocks with at most one
 * shared, global effect.
 *
 * Depends on variables being named uniquely.  Better results when variable
 * declarations have been hoisted.
 *
 * @author <a href="mailto:cgjones@cs.berkeley.edu">Chris Jones</a>
 */
public class MergeLocalStatements extends FEReplacer {
	Set<Object> globalStmtTags = new HashSet<Object> ();

	public static Program go (Program p) {
		// TODO: the functionality here should probably be factored out into
		// 'SymbolTableVisitor'-like class

		ExtractPreParallelSection ps = new ExtractPreParallelSection ();
		BreakParallelFunction parts = new BreakParallelFunction ();

		p.accept (ps);
		ps.parfun.accept (parts);

		CollectGlobalTags gtags = new CollectGlobalTags (parts.globalDecls);
		gtags.ignoreAsserts();
		parts.ploop.accept (gtags);

		return (Program) p.accept (new MergeLocalStatements (gtags.oset));
	}

	public MergeLocalStatements (Set<Object> globalStmtTags) {
		this.globalStmtTags = globalStmtTags;
	}

	/** We assume that blocks are already flattened, and thus we only need to
	 * keep as long of a run as possible */
	public Object visitStmtBlock (StmtBlock sb) {
		List<Statement> oldStmts = newStatements;
		newStatements = new ArrayList<Statement> ();

		atomicify (sb.getStmts ());

		StmtBlock nb = new StmtBlock (sb, newStatements);
		newStatements = oldStmts;
		return nb;
	}

	/** Run through the statements in S, merging adjacent stmts with at most
	 * one global effect into AtomicBlocks, which are added to 'into'. */
	public void atomicify (List<Statement> S) {
		Stack<Statement> work = new Stack<Statement> ();
		List<Statement> atomicRun = new ArrayList<Statement> ();

		addWork (work, S);

		while (!work.empty ()) {
			Statement s = work.pop ();
			boolean simple = isSimpleStmt (s);
			boolean global = isGlobalStmt (s);

			if (s instanceof StmtBlock) {
				addWork (work, ((StmtBlock) s).getStmts ());
				continue;
			} else if (s instanceof StmtAtomicBlock && simple && !global) {
				addWork (work, ((StmtAtomicBlock) s).getBlock ().getStmts ());
				continue;
			}

			if (simple)
				atomicRun.add (s);

			if (global || !simple) {
				addAtomicRun (atomicRun);
				atomicRun = new ArrayList<Statement> ();
			}

			if (!simple)
				doStatement (s);
		}

		addAtomicRun (atomicRun);
		//newStatements.addAll (atomicRun);
	}

	public void addAtomicRun (List<Statement> run) {
		if (run.size () > 0) {
			StmtAtomicBlock b = new StmtAtomicBlock (run.get (0), run);
			b.setTag (run.get (run.size () - 1).getTag ());
			addStatement (b);
		}
	}

	protected void addWork (Stack<Statement> work, List<Statement> S) {
		for (int i = S.size () - 1; i >= 0; --i) {
			work.push (S.get (i));
		}
	}

	protected boolean isSimpleStmt (Statement s) {
		return (s instanceof StmtAssert)
			    || (s instanceof StmtAssign)
			    || (s instanceof StmtAtomicBlock && !((StmtAtomicBlock)s).isCond ())
			    || (s instanceof StmtEmpty)
			    || (s instanceof StmtExpr)
			    || (s instanceof StmtReturn);
	}

	protected boolean isGlobalStmt (Statement s) {
		return globalStmtTags.contains (s.getTag ());
	}
}
