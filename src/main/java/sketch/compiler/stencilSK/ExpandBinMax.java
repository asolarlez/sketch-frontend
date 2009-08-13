package sketch.compiler.stencilSK;
import java.util.ArrayList;
import java.util.List;

import sketch.compiler.ast.core.FEContext;
import sketch.compiler.ast.core.exprs.ExprArrayRange;
import sketch.compiler.ast.core.exprs.ExprBinary;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtBlock;
import sketch.compiler.ast.core.stmts.StmtIfThen;

public class ExpandBinMax {

	public ExpandBinMax() {
		super();
		// TODO Auto-generated constructor stub
	}



	public Expression comp(int pos, int dim, ExprVar v1, ExprVar v2){
		ExprArrayRange ear1 = new ExprArrayRange(v1, new ExprConstInt(pos));
		ExprArrayRange ear2 = new ExprArrayRange(v2, new ExprConstInt(pos));
		Expression tmp = new ExprBinary(null,ExprBinary.BINOP_LT, ear1, ear2);
		Expression eq =  new ExprBinary(null,ExprBinary.BINOP_EQ, ear1, ear2);
		Expression out;
		if(pos<dim-1){
			Expression andExp = new ExprBinary(null, ExprBinary.BINOP_AND, eq, comp(pos+1, dim,  v1, v2));
			out = new ExprBinary(null, ExprBinary.BINOP_OR, tmp, andExp);
		// out = tmp || (eq &&  comp(iterIt))
		}else{
			out = tmp;
		// out = tmp;
		}
		return out;
	}


	public Statement processMax(int dim, ExprVar v1, ExprVar v2, String gv2, int id){
		Expression cond1 = new ExprVar(v1, gv2);
		Expression cond2 = comp(0, dim, v1, v2);

		StmtAssign as1 = new StmtAssign(new ExprVar(v1, ArrFunction.IND_VAR), new ExprConstInt(id+1));
		StmtAssign as2 = new StmtAssign(v1, v2);
		List<Statement> lst = new ArrayList<Statement>(2);
		lst.add(as1);
		lst.add(as2);

		StmtIfThen if2 = new StmtIfThen(cond2, cond2,  new StmtBlock((FEContext) null, lst), null);
		StmtIfThen if1 = new StmtIfThen(cond1, cond1,  if2, null);
		return if1;
	}




}
