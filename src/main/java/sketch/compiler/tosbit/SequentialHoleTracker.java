/**
 * 
 */
package streamit.frontend.tosbit;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import streamit.frontend.nodes.ExprFunCall;
import streamit.frontend.nodes.FENode;
import streamit.frontend.nodes.StmtFor;
import streamit.frontend.nodes.StmtLoop;
import streamit.frontend.nodes.TempVarGen;

/**
 * @author asolar
 *
 */
public class SequentialHoleTracker implements HoleNameTracker {

	TempVarGen varGen;
	/**
	 * store maps each hole to a stream of variables that
	 * contain values for this hole. 
	 */
	private Map<Object, List<String>> store;
	private Map<Object, Iterator<String>> currentVal;
	
	public boolean allowMemoization(){
		return false;	
	}
	
	public SequentialHoleTracker(TempVarGen varGen){
		this.varGen = varGen;
		store = new HashMap<Object, List<String>>();
	}
	
	public String getNameFirst(Object hole) {
		String vname = "H_" + varGen.nextVar();
		if(store.containsKey(hole)){
			store.get(hole).add(vname);
		}else{
			List<String> lst = new LinkedList<String>();
			lst.add(vname);
			store.put(hole, lst);
		}
		return vname;
	}

	
	public String getNameOthers(Object hole) {		
		Iterator<String> cit = currentVal.get(hole);
		assert cit.hasNext() : "This can't happen";		
		return cit.next();
	}
	
	
	public String getName(Object hole){
		if( currentVal == null )
			return getNameFirst(hole);
		else
			return getNameOthers(hole);
	}
	
	
	
	public void pushFor(StmtFor floop) {
		//Nothing to do here.

	}

	public void pushFunCall(ExprFunCall call) {
		//Nothing to do here.

	}

	public void pushLoop(StmtLoop loop) {
		//Nothing to do here.

	}

	public void regLoopIter() {
		//Nothing to do here.

	}
	
	public void reset() {
		currentVal = new HashMap<Object, Iterator<String>>();
		for(Iterator<Entry<Object, List<String>>> it = store.entrySet().iterator(); it.hasNext(); ){
			Entry<Object, List<String>> ent = it.next();
			currentVal.put(ent.getKey(),ent.getValue().iterator());
		}
	}

}
