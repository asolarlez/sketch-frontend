package streamit.frontend.passes;

import java.util.ArrayList;
import java.util.List;

import streamit.frontend.nodes.FEReplacer;
import streamit.frontend.nodes.Statement;
import streamit.frontend.nodes.StmtAtomicBlock;
import streamit.frontend.nodes.StmtBlock;


/**
 * 
 * Gets rid of nested and empty blocks.
 * 
 * Preconditions:
 * 
 * This class assumes that all variable names are already unique.
 * 
 * 
 * @author asolar
 *
 */
public class FlattenStmtBlocks extends FEReplacer {

	void addStmts(List<Statement> oldS, List<Statement> newS){
		for(int i=0; i<oldS.size(); ++i){
			Statement s = oldS.get(i);
			if(s instanceof StmtBlock && ! (s instanceof StmtAtomicBlock)){
				StmtBlock sb = (StmtBlock) s;
				addStmts(sb.getStmts(), newS);
			}else{
				if(s != null){
					newS.add(s);
				}
			}
		}
	}
	
	@Override
	public Object visitStmtBlock(StmtBlock sb){
		List<Statement> news = new ArrayList<Statement>();
		addStmts(sb.getStmts(), news);
		return super.visitStmtBlock(new StmtBlock(sb.getCx(), news));				
	}
	
}
