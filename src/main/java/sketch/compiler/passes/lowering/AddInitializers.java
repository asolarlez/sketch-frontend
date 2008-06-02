package streamit.frontend.passes;

import streamit.frontend.nodes.Expression;
import streamit.frontend.nodes.FEReplacer;
import streamit.frontend.nodes.StmtVarDecl;

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
