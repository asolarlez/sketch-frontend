package sketch.compiler.smt.passes;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.SymbolTable;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.Function.FcnType;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.ExprTernary;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.StmtBlock;
import sketch.compiler.ast.core.stmts.StmtReturn;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.passes.lowering.SymbolTableVisitor;

/**
 * Eliminate direct use of ternery expressions:
 * Transform:
 * x = a ? b : c;
 * 
 * into:
 * x = TerneryExpr_1(a, b, c);
 * 
 * int TerneryExpr(int a, int b, int c) {
 * 		return a ? b : c;
 * }
 * 
 * If we don't do that, we can not handle nested JOIN:
 * 
 * x = JOIN (JOIN a:b !a:c):d !(JOIN a:b !a:c):e
 * 
 * When one join is the condition of another join, we can not
 * modularize the translation of the assignment to x.
 * 
 * It's hard to modularize because the correct translation looks like this:
 * x = d if (a && b) || (!a && c)
 * x = e if (a && !b) || (a && !c)
 * 
 * Adding two JOIN together is equally complicated. One solution is to introduce
 * temporary variables to hold the JOIN value:
 * 
 * t = JOIN a:b !a:c
 * x = JOIN t:d !t:e
 * 
 * But introducing this kind of temporary variable can be very tricky when the JOIN
 * is in side the condition of a for-loop or incrementation. That's because you need 
 * to update the temporary variable each time you go through the loop, which requires
 * an assignment to be inserted somehwere in the loop.
 * 
 * The solution is to wrap each JOIN into a function call.
 * 
 * @author Lexin Shan
 * @email lshan@eecs.berkeley.edu
 *
 */
public class EliminateTernery extends SymbolTableVisitor {
	
	protected TempVarGen mTmpVarGen;
	
	public EliminateTernery(TempVarGen tmpVarGen) {
		super(null);
		mTmpVarGen = tmpVarGen;
	}
	
	@Override
	public Object visitExprTernary(ExprTernary exp) {
		Expression newA = (Expression) exp.getA().accept(this);
		Expression newB = (Expression) exp.getB().accept(this);
		Expression newC = (Expression) exp.getC().accept(this);
		
		if (newA == exp.getA() || 
				newB == exp.getB() || 
				newC == exp.getC()) {
				
			exp = new ExprTernary(exp, exp.getOp(), newA, newB, newC);
		}
			
		String funcName = mTmpVarGen.nextVar("terneryExpr");
		
		GetReferencedVariables grv = new GetReferencedVariables(this.symtab);
		exp.accept(grv);
		
		List<Parameter> params = new LinkedList<Parameter>();
		List<Expression> args = new LinkedList<Expression>();
		for (int i = 0; i < grv.mVarNames.size(); i++) {
			Type t = grv.mVarTypes.get(i);
			String name = grv.mVarNames.get(i);
			params.add(new Parameter(t, name));
			args.add(new ExprVar(exp, name));
		}
		
		
		StmtBlock body = new StmtBlock(new StmtReturn(exp, exp));
        Function helper =
                Function.creator(exp, funcName, FcnType.Generator).returnType(getType(exp)).params(
                        params).body(body).create();
		
		newFuncs.add(helper);
		
		ExprFunCall call = new ExprFunCall(exp, funcName, args);
		return call;
	}
	
	
	public static class GetReferencedVariables extends SymbolTableVisitor {
		ArrayList<Type> mVarTypes;
		ArrayList<String> mVarNames;
		
		public GetReferencedVariables(SymbolTable symtab) {
			super(symtab);
			mVarTypes = new ArrayList<Type>();
			mVarNames = new ArrayList<String>();
		}
		
		@Override
		public Object visitExprVar(ExprVar exp) {
			if (!mVarNames.contains(exp.getName())) {
				mVarNames.add(exp.getName());
				mVarTypes.add(getType(exp));
			}
			return super.visitExprVar(exp);
		}
	}
	

}
