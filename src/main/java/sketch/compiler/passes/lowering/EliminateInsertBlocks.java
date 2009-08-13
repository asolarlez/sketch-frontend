/**
 *
 */
package sketch.compiler.passes.lowering;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.ExprBinary;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprConstant;
import sketch.compiler.ast.core.exprs.ExprStar;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssert;
import sketch.compiler.ast.core.stmts.StmtAtomicBlock;
import sketch.compiler.ast.core.stmts.StmtBlock;
import sketch.compiler.ast.core.stmts.StmtIfThen;
import sketch.compiler.ast.core.stmts.StmtInsertBlock;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.util.Misc;

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
	protected List<Statement> headerDecls;

	public EliminateInsertBlocks (TempVarGen _varGen) {
		varGen = _varGen;
	}

	Stack<String> svars = new Stack<String>();
	
	int twopow(int n){
		return 1 << n;
	}
	public void addAssertion(Statement cx, List<Statement> slist, Expression cond, int idx){
		int l = 0;
		int lp = svars.size();
		Expression e = null;
		for(String sv : svars){
			int diff = lp-l; ++l;
			if(idx/twopow(diff-1)%2 == 1){
				if(e == null){
					e = new ExprBinary(cx, new ExprVar(cx,sv), "==", new ExprConstInt(idx/twopow(diff)));
				}else{
					e = new ExprBinary(cx, e, "&&", 
						new ExprBinary(cx, new ExprVar(cx,sv), "==", new ExprConstInt(idx/twopow(diff)))
					);
				}
			}
		}
		if(e != null){
			slist.add(new StmtIfThen(cx, cond, new StmtAssert(e, "insert assert", true), null)); 			
		}
	}
	
	public Object visitStmtInsertBlock (StmtInsertBlock sib) {
		if (0 == depth++){
			header = new ArrayList<Statement> ();
			headerDecls = new ArrayList<Statement> ();
		}

		String where = varGen.nextVar ("_ins_where");		
		Statement S = (Statement) sib.getInsertStmt ().accept (this);

		// Rewrite 'into' body
		List<Statement> oldB = sib.getIntoBlock ().getStmts ();
		if (1 == oldB.size () && (oldB.get (0) instanceof StmtInsertBlock)){
			// Special case for immediately-nested 'insert' blocks			
			oldB = ((StmtBlock) oldB.get (0).accept (this)).getStmts ();
		}else
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
								), true));
		headerDecls.add (0,
				new StmtVarDecl (sib, TypePrimitive.inttype, where, new ExprStar (sib, nBits)));

		List<Statement> newB = new ArrayList<Statement> ();
		for (int i = 0; i < oldB.size (); ++i) {
			Statement si = (Statement) oldB.get (i);
			Expression eb = new ExprBinary (new ExprVar (S, where),
					"==", ExprConstant.createConstant (S, ""+i)); 
			newB.add (new StmtIfThen (S, eb, S, null));
			addAssertion(sib, header, eb, i);
			newB.add (si);
		}		
		Expression eb = new ExprBinary (new ExprVar (S, where),
			    "==", ExprConstant.createConstant (S, maxVal)); 
		newB.add (new StmtIfThen (S,eb, S, null));
		addAssertion(sib, header, eb, oldB.size());

		svars.push(where);
		if (0 == --depth){
			newB.add(0, new StmtAtomicBlock(sib, header));
			newB.addAll (0, headerDecls);			
			svars.clear();
		}

		return new StmtBlock (sib, newB);
	}
}
