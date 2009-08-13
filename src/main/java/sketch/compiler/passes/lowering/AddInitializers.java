package sketch.compiler.passes.lowering;
import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.StmtVarDecl;

public class AddInitializers extends FEReplacer {

	public Object visitStmtVarDecl(StmtVarDecl svd){
		
		for(int i=0; i<svd.getNumVars(); ++i){
			if(svd.getInit(i) == null){
				Expression init = svd.getType(i).defaultValue();
				svd.setInit(i, init);								
			}
		}
		return svd;
	}
	
}
