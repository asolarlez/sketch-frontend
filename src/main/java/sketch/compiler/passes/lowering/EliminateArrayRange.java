package streamit.frontend.passes;

import java.util.ArrayList;
import java.util.List;

import streamit.frontend.nodes.ExprArray;
import streamit.frontend.nodes.ExprArrayRange;
import streamit.frontend.nodes.ExprConstInt;
import streamit.frontend.nodes.ExprVar;
import streamit.frontend.nodes.Expression;
import streamit.frontend.nodes.FEReplacer;
import streamit.frontend.nodes.Statement;
import streamit.frontend.nodes.StmtAssign;
import streamit.frontend.nodes.StmtVarDecl;
import streamit.frontend.nodes.TempVarGen;
import streamit.frontend.nodes.Type;
import streamit.frontend.nodes.TypeArray;
import streamit.frontend.nodes.ExprArrayRange.RangeLen;

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
				StmtVarDecl decl = new StmtVarDecl(arng.getContext(), type, newName, null);
				addStatement(decl);	
				Statement assign = new StmtAssign(arng.getContext(), new ExprVar(arng.getContext(), newName), newRHS);
				addStatement(assign);
				return new StmtAssign(stmt.getContext(), arng, new ExprVar(stmt.getContext(), newName), stmt.getOp());				
			}else{
				newLHS = doExpression(stmt.getLHS());
			}
		}else{
			newLHS = doExpression(stmt.getLHS());
		}
				                
        if (newLHS == stmt.getLHS() && newRHS == stmt.getRHS())
            return stmt;
        return new StmtAssign(stmt.getContext(), newLHS, newRHS,
                              stmt.getOp());
    }
	
	
	
	public Object visitExprArrayRange(ExprArrayRange exp){
    	assert exp.getMembers().size() == 1 && exp.getMembers().get(0) instanceof RangeLen : "Complex indexing not yet implemented.";
    	RangeLen rl = (RangeLen)exp.getMembers().get(0);
    	if( rl.len() == 1 ){
    		Expression newBase=doExpression(exp.getBase());
    		Expression newIndex = doExpression(rl.start());	
    		return new ExprArrayRange(exp.getContext(), newBase, newIndex);
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
			StmtVarDecl decl = new StmtVarDecl(exp.getContext(), type, newName, null);
			this.addStatement(decl);
			Statement assign = new StmtAssign(exp.getContext(), new ExprVar(exp.getContext(), newName), exp);
			this.addStatement(assign);
			return new ExprVar(exp.getContext(), newName);						
    	}
    }
}
