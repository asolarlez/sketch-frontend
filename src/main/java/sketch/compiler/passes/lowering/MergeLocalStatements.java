/**
 *
 */
package sketch.compiler.passes.lowering;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssert;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtAtomicBlock;
import sketch.compiler.ast.core.stmts.StmtBlock;
import sketch.compiler.ast.core.stmts.StmtEmpty;
import sketch.compiler.ast.core.stmts.StmtExpr;
import sketch.compiler.ast.core.stmts.StmtReturn;
import sketch.compiler.parallelEncoder.BreakParallelFunction;
import sketch.compiler.parallelEncoder.ExtractPreParallelSection;

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
	/*
	public Object visitStmtIfThen(StmtIfThen stmt){
		Statement tpart = peelBlock((Statement) stmt.getCons().accept(this));
		Statement epart = null;
		if(stmt.getAlt() != null)
			epart = peelBlock((Statement) stmt.getAlt().accept(this));
		if(isSimpleStmt(tpart) && isSimpleStmt(epart)){
			if(tpart instanceof StmtAtomicBlock && !((StmtAtomicBlock)tpart).isCond()){
				tpart = ((StmtAtomicBlock)tpart).getBlock();
			}
			if(epart != null && epart instanceof StmtAtomicBlock && !((StmtAtomicBlock)epart).isCond()){
				epart = ((StmtAtomicBlock)epart).getBlock();
			}
			if(!(tpart instanceof StmtBlock)){
				Object oldTag = tpart.getTag();
				tpart = new StmtBlock(tpart);
				tpart.setTag(oldTag);
			}
			if(epart != null && !(epart instanceof StmtBlock)){
				Object oldTag = epart.getTag();
				epart = new StmtBlock(epart);
				epart.setTag(oldTag);
			}
			return new StmtAtomicBlock(stmt, Collections.singletonList(new StmtIfThen(stmt, stmt.getCond(), tpart, epart)));
		}
		if(tpart != stmt.getCons() || epart != stmt.getAlt()){
			if(!(tpart instanceof StmtBlock)){
				Object oldTag = tpart.getTag();
				tpart = new StmtBlock(tpart);
				tpart.setTag(oldTag);
			}
			if(epart != null && !(epart instanceof StmtBlock)){
				Object oldTag = epart.getTag();
				epart = new StmtBlock(epart);
				epart.setTag(oldTag);
			}
			return new StmtIfThen(stmt, stmt.getCond(), tpart, epart);
		}else{
			return stmt;
		}
	}
	*/
	
	Statement peelBlock(Statement s){
		if(s instanceof StmtBlock){
			StmtBlock sb = (StmtBlock) s;
			if(sb.getStmts().size() == 1){
				return peelBlock(sb.getStmts().get(0));
			}
		}
		return s;
	}

	/** Run through the statements in S, merging adjacent stmts with at most
	 * one global effect into AtomicBlocks, which are added to 'into'. */
	public void atomicify (List<Statement> S) {
		Stack<Statement> work = new Stack<Statement> ();
		List<Statement> atomicRun = new ArrayList<Statement> ();

		addWork (work, S);

		while (!work.empty ()) {
			Statement s = work.pop ();
			
			/*
			if(s instanceof StmtIfThen){
				s = (Statement) s.accept(this);
			}
			*/
			boolean simple = isSimpleStmt (s);
			boolean global = isGlobalStmt (s);

			if (s instanceof StmtBlock) {
				addWork (work, ((StmtBlock) s).getStmts ());
				continue;
			} 

			if (simple){
				if (s instanceof StmtAtomicBlock) {					
					atomicRun.addAll (((StmtAtomicBlock) s).getBlock ().getStmts ());
				}else{
					atomicRun.add (s);	
				}
			}

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
			if(run.size() == 1 && run.get(0) instanceof StmtAtomicBlock){
				addStatement(run.get(0));
			}else{
				StmtAtomicBlock b = new StmtAtomicBlock (run.get (0), run);
				b.setTag (run.get (run.size () - 1).getTag ());
				addStatement (b);
			}
		}
	}

	protected void addWork (Stack<Statement> work, List<Statement> S) {
		for (int i = S.size () - 1; i >= 0; --i) {
			work.push (S.get (i));
		}
	}

	protected boolean isSimpleStmt (Statement s) {
		if(s == null) return true;
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
