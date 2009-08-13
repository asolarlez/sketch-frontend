package sketch.compiler.passes.lowering;
import java.util.ArrayList;
import java.util.List;

import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.ExprArrayRange;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.exprs.ExprArrayRange.RangeLen;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;

/**
 * This class isolates array range reads and writes so that
 * expressions of the form arr[a::b] = complicated
 * become
 * tmp = complicated;
 * arr[a::b] = tmp;
 *
 * Also, things like
 *
 *  arr = fun_of(x[a::b]);
 *
 *  become
 *  tmp = x[a::b];
 *  arr = func_of(tmp);
 *
 *
 * This way, assignments to array ranges can assume that the rhs is simply an arrya variable of the same size,
 * and binary expressions, etc, can assume they have no array ranges.
 * @author asolar
 *
 */


public class EliminateArrayRange extends SymbolTableVisitor {

	private TempVarGen varGen;

	public EliminateArrayRange(TempVarGen varGen) {
		super(null);
		this.varGen = varGen;
	}



	public Object visitStmtAssign(StmtAssign stmt)
    {
		Expression newRHS = doExpression(stmt.getRHS());

		Expression newLHS;
		Statement postAssign=null;
		if( stmt.getLHS() instanceof ExprArrayRange ){
			ExprArrayRange arng = (ExprArrayRange) stmt.getLHS();
			assert arng.getMembers().size() == 1 && arng.getMembers().get(0) instanceof RangeLen : "Complex indexing not yet implemented.";
			RangeLen rl = (RangeLen)arng.getMembers().get(0);
			if(rl.len() != 1){
				TypeArray arrType = (TypeArray) getType(arng.getBase());
				Type baseType = arrType.getBase();
				Type type = new TypeArray(baseType, new ExprConstInt(rl.len()));

				Expression newBase=doExpression(arng.getBase());
				Expression newIndex = doExpression(rl.start());
				if( newIndex != rl.start() || newBase != arng.getBase() ){
					List lst = new ArrayList();
					lst.add( new RangeLen(newIndex, rl.len()) );
					arng = new ExprArrayRange(newBase, lst);
				}
				String newName = varGen.nextVar();
				StmtVarDecl decl = new StmtVarDecl(arng, type, newName, null);
				addStatement(decl);
				Statement assign = new StmtAssign(new ExprVar(arng, newName), newRHS);
				addStatement(assign);
				return new StmtAssign(stmt, arng, new ExprVar(stmt, newName), stmt.getOp());
			}else{
				newLHS = doExpression(stmt.getLHS());
			}
		}else{
			newLHS = doExpression(stmt.getLHS());
		}

        if (newLHS == stmt.getLHS() && newRHS == stmt.getRHS())
            return stmt;
        return new StmtAssign(stmt, newLHS, newRHS,
                              stmt.getOp());
    }



	public Object visitExprArrayRange(ExprArrayRange exp){
    	assert exp.getMembers().size() == 1 && exp.getMembers().get(0) instanceof RangeLen : "Complex indexing not yet implemented.";
    	RangeLen rl = (RangeLen)exp.getMembers().get(0);
    	if( rl.len() == 1 ){
    		Expression newBase=doExpression(exp.getBase());
    		Expression newIndex = doExpression(rl.start());
    		return new ExprArrayRange(exp, newBase, newIndex);
    	}else{
			TypeArray arrType = (TypeArray) getType(exp.getBase());
			Type baseType = arrType.getBase();
			Type type = new TypeArray(baseType, new ExprConstInt(rl.len()));

			Expression newBase=doExpression(exp.getBase());
			Expression newIndex = doExpression(rl.start());
			if( newIndex != rl.start() || newBase != exp.getBase() ){
				List lst = new ArrayList();
				lst.add( new RangeLen(newIndex, rl.len()) );
				exp = new ExprArrayRange(newBase, lst);
			}
			String newName = varGen.nextVar();
			StmtVarDecl decl = new StmtVarDecl(exp, type, newName, null);
			this.addStatement(decl);
			Statement assign = new StmtAssign(new ExprVar(exp, newName), exp);
			this.addStatement(assign);
			return new ExprVar(exp, newName);
    	}
    }
}
