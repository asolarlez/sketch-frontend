package sketch.compiler.smt.passes;

import java.util.LinkedList;
import java.util.List;

import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Function.FuncType;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.exprs.ExprArrayRange;
import sketch.compiler.ast.core.exprs.ExprBinary;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssert;
import sketch.compiler.ast.core.stmts.StmtBlock;
import sketch.compiler.ast.core.stmts.StmtExpr;
import sketch.compiler.ast.core.stmts.StmtFor;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.ast.core.typs.TypePrimitive;

public class AddWrapper extends FEReplacer {
	
	
	public static final String _MAIN = "_main";
	private static final String sketchReturnVarName = "rSk";
	private static final String specReturnVarName = "rSp";

	@Override
	public Object visitFunction(Function func) {
		String specName = func.getSpecification();
		if (specName != null) {
			Function spec = getFuncNamed(specName);
			Function sketch = func;
			
			Function mainWrapper = createWrapper(sketch, spec, sketch);
			super.newFuncs.add(mainWrapper);
			
			return func;
			
		} else {
			return func;
		}
	}

	private Function createWrapper(FENode ctx, Function spec, Function sketch) {
		
		// void main(P p1, P p2) {
		// 		rSp = spec(p1, p2);
		// 		rSk = spec(p1, p2);
		// 		assert (rSp == rSk);
		// }
		
		// create body			
		List<Statement> mainBody = new LinkedList<Statement>();
		
		// copy the parameters from sketch
		List<Parameter> params = new LinkedList<Parameter>();
		List<Expression> paramsToPassSk = new LinkedList<Expression>();
		List<Expression> paramsToPassSp = new LinkedList<Expression>();
		
		ExprVar sketchReturnVar = new ExprVar(ctx, sketchReturnVarName);
		ExprVar specReturnVar = new ExprVar(ctx, specReturnVarName);
		
		StmtVarDecl specRetVar = null;
		StmtVarDecl sketchRetVar = null;
		boolean hasReturn = false;
		Type returnType = null;
		
		assert sketch.getParams().size() >= spec.getParams().size();
		
		for (Parameter p: sketch.getParams()) {
			if (!p.isParameterOutput()) { 
				params.add(new Parameter(p.getType(), p.getName(), p.getPtype()));
				paramsToPassSk.add(new ExprVar(ctx, p.getName()));
			} else {
				hasReturn = true;
				returnType = p.getType();
				
				sketchRetVar = new StmtVarDecl(sketch, p.getType(), sketchReturnVarName, null);
				paramsToPassSk.add(sketchReturnVar);
			}
		}
		
		
		int i = 0;
		for (Parameter p: spec.getParams()) {
			if (!p.isParameterOutput()) { 
				paramsToPassSp.add(paramsToPassSk.get(i));
			} else {				
				specRetVar = new StmtVarDecl(sketch, p.getType(), specReturnVarName, null);
				paramsToPassSp.add(specReturnVar);
			}
			i++;
		}
		
		
		
		
		// declare return vars for spec and sketch
		if (hasReturn) {
			
			mainBody.add(specRetVar);
			mainBody.add(sketchRetVar);
			
			mainBody.add(new StmtExpr(new ExprFunCall(sketch, sketch.getName(), paramsToPassSk)));
			mainBody.add(new StmtExpr(new ExprFunCall(sketch, spec.getName(), paramsToPassSp)));
			
			if (returnType instanceof TypeArray) {
				// insert a loop to compare each element in return value
				
				// for (i = 0; i < rSk.length; i ++) {
				// 		assert (rSk[i] == rSp[i]);
				// }
				ExprVar idxVar = new ExprVar(sketch, "idx");
				TypeArray returnTypeArray = (TypeArray) returnType;
				Expression arrayLengthExpr = returnTypeArray.getLength();
				
				StmtBlock loopBlk = new StmtBlock(
						new StmtAssert(
								new ExprBinary(ExprBinary.BINOP_EQ, 
										new ExprArrayRange(specReturnVar, idxVar), 
										new ExprArrayRange(sketchReturnVar, idxVar)), 
								"return values differ between sketch and spec", 
								false));
				StmtFor loop = new StmtFor(idxVar.getName(), arrayLengthExpr, loopBlk);
				mainBody.add(loop);
				
			} else {
				// just compare a single variable
				mainBody.add(new StmtAssert(
						new ExprBinary(ExprBinary.BINOP_EQ, specReturnVar, sketchReturnVar),
						"return values differ between sketch and spec",
						false));	
			}
			
		} else {
		
			mainBody.add(new StmtExpr(new ExprFunCall(sketch, sketch.getName(), paramsToPassSk)));
			mainBody.add(new StmtExpr(new ExprFunCall(sketch, spec.getName(), paramsToPassSp)));
			
		}
		
		StmtBlock bodyBlk = new StmtBlock(mainBody);
		Function synthesisWrapper = new Function(spec, FuncType.FUNC_BUILTIN_HELPER, _MAIN, TypePrimitive.voidtype, params, bodyBlk);
		return synthesisWrapper;
	}
	
	

}
