package sketch.compiler.smt.partialeval;

import java.util.List;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.ExprArrayRange;
import sketch.compiler.ast.core.exprs.ExprArrayRange.RangeLen;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.dataflow.simplifier.ScalarizeVectorAssignments;

/**
 * This class vectorize all array assignments but not bit arrays. The reason
 * is that bit arrays should be handled by bit vector theory in SMT
 *
 *
 *  
 * @author Lexin Shan
 * @email lshan@eecs.berkeley.edu
 *
 */
public class ScalarizeAssignmentNotBitArray extends ScalarizeVectorAssignments {

	private static class IsRangeIdxVisitor extends FEReplacer {
		private boolean isRangeIdx = false;
		
		@Override
		public Object visitExprArrayRange(ExprArrayRange exp) {
			List members = exp.getMembers();
			
			for (Object o : members) {
				if (o instanceof RangeLen) {
					isRangeIdx = true;
					return exp;
				}
			}
			
			return super.visitExprArrayRange(exp);
		}
	}
	
	public ScalarizeAssignmentNotBitArray(TempVarGen varGen) {
		super(varGen);
	}
	
	public ScalarizeAssignmentNotBitArray(TempVarGen varGen, boolean b) {
		super(varGen, b);
	}

	public Object visitStmtAssign(StmtAssign stmt) {

		Type ltype = getType(stmt.getLHS());
		if (BitVectUtil.isBitArray(ltype)) {
			IsRangeIdxVisitor v = new IsRangeIdxVisitor();
			stmt.getLHS().accept(v);
			
			// if the index of the array on the lhs is range, ie  a[1::3] = x;
			// we do not scalarize them because SMT does not support non-constant
			// extract operation.
			if (v.isRangeIdx)
				return super.visitStmtAssign(stmt);
			else
				return stmt;
		}
			

		return super.visitStmtAssign(stmt);

	}
	
	
	

}
