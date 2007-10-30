package streamit.frontend.parallelEncoder;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import streamit.frontend.controlflow.CFG;
import streamit.frontend.controlflow.CFGNode;

public class CFGSimplifier {
	Set<String> locals;
	CFGSimplifier(Set<String> locals){
		this.locals = locals;
	}

	
	void mergeConsecutiveLocals(CFG cfg){
		/*
		for(Iterator<CFGNode> nodeIt = cfg.getNodes().iterator(); nodeIt.hasNext(); ){
			CFGNode node = nodeIt.next();
			if(allLocals(node)){
				//If all the predecessors have a single successor, add to the predecessor.
				List<CFGNode> preds = cfg.getPredecessors(node);
				for(Iterator<CFGNode> predIt = preds.iterator(); predIt.hasNext(); ){
					CFGNode pred = predIt.next();
					if(   )
				}
				
				//If single successor has only one predecessor add to successor.
			}
		}
		
		*/
		
		
		
	}
	

}

