package sketch.compiler.stencilSK;

import java.util.Map;

import sketch.compiler.ast.core.Program;
import sketch.compiler.passes.lowering.SemanticChecker;

public class StencilSemanticChecker extends SemanticChecker {

	public StencilSemanticChecker() {
		super();
		// TODO Auto-generated constructor stub
	}

	public static boolean check(Program prog)
	{    	
		StencilSemanticChecker checker = new StencilSemanticChecker();
		Map streamNames = checker.checkStreamNames(prog);
		checker.checkDupFieldNames(prog, streamNames);
		//       checker.checkStreamCreators(prog, streamNames);
		//checker.checkStreamTypes(prog);//I don't need this one in StreamBit
		//checker.checkFunctionValidity(prog);
		checker.checkStatementPlacement(prog);
		checker.checkVariableUsage(prog);
		checker.checkBasicTyping(prog);
		//checker.checkStreamConnectionTyping(prog);//I don't want this one in StreamBit
		checker.checkStatementCounts(prog);
		//       checker.checkIORates(prog);
		return checker.good;
	}

}
