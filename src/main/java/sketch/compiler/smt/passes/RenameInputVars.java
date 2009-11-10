package sketch.compiler.smt.passes;

import java.util.HashSet;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.exprs.ExprVar;

public class RenameInputVars extends FEReplacer {
	
	private static class FindSpecName extends FEReplacer{
		protected String sketchName;
		
		@Override
		public Object visitFunction(Function func) {
			if (func.getSpecification() != null)
				sketchName = func.getSpecification();
			return func;
		}
	}
	
	private final static String inputPrefix = "I___";
	private HashSet<String> paramsInCurrFunc;
	private String specName;

	public RenameInputVars() {
		
	}
	
	@Override
	public Object visitProgram(Program prog) {
		FindSpecName fsn = new FindSpecName();
		prog.accept(fsn);
		this.specName = fsn.sketchName;
		
		return super.visitProgram(prog);
	}
	@Override
	public Object visitFunction(Function f) {
		this.paramsInCurrFunc = new HashSet<String>();
		
		if (f.getSpecification() != null) {	
			return super.visitFunction(f); 	
			
		} else if (f.getName().equals(specName)) {
			return super.visitFunction(f);
		}
		 
		return f;
	}
	
	@Override
	public Object visitParameter(Parameter par) {	
		paramsInCurrFunc.add(par.getName());
		return new Parameter(par.getType(), addPrefix(par.getName()));
	}

	private String addPrefix(String par) {
		return inputPrefix + par;
	}
	
	@Override
	public Object visitExprVar(ExprVar exp) {
		if (paramsInCurrFunc.contains(exp.getName())) {
			return new ExprVar(exp, addPrefix(exp.getName()));
		} else {
			return super.visitExprVar(exp);
		}
	}
	
	
	
}
