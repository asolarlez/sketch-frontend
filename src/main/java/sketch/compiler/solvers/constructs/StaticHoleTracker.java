package streamit.frontend.stencilSK;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import streamit.frontend.nodes.ExprFunCall;
import streamit.frontend.nodes.FENode;
import streamit.frontend.nodes.StmtFor;
import streamit.frontend.nodes.StmtLoop;
import streamit.frontend.nodes.TempVarGen;
import streamit.frontend.tosbit.HoleNameTracker;

public class StaticHoleTracker implements HoleNameTracker {
	private Map<FENode, String> store;
	TempVarGen varGen;
	public StaticHoleTracker(TempVarGen varGen){
		this.varGen = varGen;
		store = new HashMap<FENode, String>();
	}
	
	public String getName(FENode hole) {
		
		String vname = null;
		if(store.containsKey(hole)){
			vname = store.get(hole);
		}else{
			
			vname = "H_" + varGen.nextVar();			
			store.put(hole, vname);			
		}
		return vname;		
	}

	public void pushFor(StmtFor floop) {
		// TODO Auto-generated method stub

	}

	public void pushFunCall(ExprFunCall call) {
		// TODO Auto-generated method stub

	}

	public void pushLoop(StmtLoop loop) {
		// TODO Auto-generated method stub

	}

	public void regLoopIter() {
		// TODO Auto-generated method stub

	}

	public void reset() {
		// TODO Auto-generated method stub

	}

}
