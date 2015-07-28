package sketch.compiler.passes.lowering;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.ExprArrayRange;
import sketch.compiler.ast.core.exprs.ExprArrayRange.RangeLen;
import sketch.compiler.ast.core.exprs.ExprNullPtr;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
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

	String lhsname = null;

	public Object visitStmtAssign(StmtAssign stmt)
    {
	    if(stmt.getLHS() instanceof ExprVar){
	        lhsname = stmt.getLHS().toString();
	    }
	    
		Expression newRHS = doExpression(stmt.getRHS());
		
		lhsname = null;
		
		Expression newLHS;
		Statement postAssign=null;
		if( stmt.getLHS() instanceof ExprArrayRange ){
			ExprArrayRange arng = (ExprArrayRange) stmt.getLHS();
			
			RangeLen rl = arng.getSelection();
			if(rl.hasLen()){
				TypeArray arrType = (TypeArray) getType(arng.getBase());
				Type baseType = arrType.getBase();
                Type type =
                        new TypeArray(baseType, rl.getLenExpression(),
                                arrType.getMaxlength());

				Expression newBase=doExpression(arng.getBase());
				Expression newIndex = doExpression(rl.start());
				Expression newLen = doExpression(rl.getLenExpression());
				if( newIndex != rl.start() || newLen != rl.getLenExpression() || newBase != arng.getBase() ){					
					arng = new ExprArrayRange(arng, newBase, new RangeLen(newIndex, newLen));
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



    public Object visitExprArrayRange(ExprArrayRange exp) {
        if (exp.getBase() instanceof ExprNullPtr)
            return new ExprNullPtr();
    	RangeLen rl =  exp.getSelection();
    	if( !rl.hasLen() ){
    		Expression newBase=doExpression(exp.getBase());
    		Expression newIndex = doExpression(rl.start());
    		return new ExprArrayRange(exp, newBase, newIndex);
    	}else{
			TypeArray arrType = (TypeArray) getType(exp.getBase());
			Type baseType = arrType.getBase();
            Type type =
                    new TypeArray(baseType, doExpression(rl.getLenExpression()),
                            arrType.getMaxlength());

			Expression newBase=doExpression(exp.getBase());
			Expression newIndex = doExpression(rl.start());
			Expression newLen = doExpression(rl.getLenExpression());
			if( newIndex != rl.start() ||  newLen != rl.getLenExpression() || newBase != exp.getBase() ){				
				exp = new ExprArrayRange(exp, newBase, new RangeLen(newIndex, newLen));
			}
			String newName = varGen.nextVar(lhsname);
			StmtVarDecl decl = new StmtVarDecl(exp, type, newName, null);
			this.addStatement(decl);
			Statement assign = new StmtAssign(new ExprVar(exp, newName), exp);
			this.addStatement(assign);
			return new ExprVar(exp, newName);
    	}
    }
}
