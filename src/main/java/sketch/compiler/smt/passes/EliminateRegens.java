package sketch.compiler.smt.passes;
import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.ExprBinary;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprConstant;
import sketch.compiler.ast.core.exprs.ExprStar;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.stmts.StmtAssert;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.util.Misc;

public class EliminateRegens extends sketch.compiler.passes.lowering.EliminateRegens {

	public EliminateRegens(TempVarGen varGen) {
		super(varGen);
	
	}

	@Override
	protected ExprVar makeNDChoice (int n, FENode cx, String pfx) {
		ExprVar which = new ExprVar (cx, varGen.nextVar (pfx));

		globalDecls.add(new StmtVarDecl (cx, TypePrimitive.inttype, which.getName (), null));
		// the main purpose of this class is to account for the fact that in SMT
		// negative integers are allowed. So we need an additional bit than 
		// the number of choices for the star
		int nBits = Misc.nBitsBinaryRepr (n) + 1;
		ExprStar es = new ExprStar (which, nBits);
		globalDecls.add(new StmtAssign (which, es));
		globalDecls.add(new StmtAssert (
			new ExprBinary (
				new ExprBinary (ExprConstInt.zero, "<=", which),
				"&&",
				new ExprBinary (which, "<", ExprConstant.createConstant (which, ""+ n))), "regen " + es.getSname(), true));

		return which;
	}
}
