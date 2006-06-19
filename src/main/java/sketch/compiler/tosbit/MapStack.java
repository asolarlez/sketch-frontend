package streamit.frontend.tosbit;

import java.util.HashMap;
import java.util.Iterator;

/**
 * This class maps each program variable name to its unique variable name to
 * take care of scoping issues.
 * 
 * It is a stack in order to handle nested blocks. The idea is that every time
 * you enter a nested block you push a level into this stack. This way, you handle
 * scoping apropriately.
 * 
 */

class MapStack{
	
	private HashMap<String, String> curVT; //HashMap<String, String>
	private MapStack kid;
	private int pushcount=0;
	private String lastSeen=null;
	private String lastSeenTrans=null;
	
	MapStack(){
		curVT = new HashMap<String, String>();
	}
	/**
	 * Creates the new unique name for this variable.
	 */
	public void varDeclare(String var){
		String newname = var + "_" + pushcount +"L" + curVT.size();
		lastSeen = var;
		lastSeenTrans = newname;
		curVT.put(var, newname);
	}
	public String varDeclareFresh(){		
		String newname = "_" + pushcount +"L" + curVT.size();
		lastSeen = newname;
		lastSeenTrans = newname;
		curVT.put(newname, newname);
		return newname;
	}
	
	public MapStack pushLevel(){
		++pushcount;    	
    	MapStack tmp = new MapStack();
    	tmp.kid = this;
    	tmp.pushcount = pushcount;
    	return tmp;
	}
	
	public MapStack popLevel(HashMap vars, ChangeStack changeTracker){
		Iterator it = curVT.values().iterator();
    	while(it.hasNext()){
    		String nm = (String) it.next();
    		//System.out.println("Unseting " + nm);
    		vars.remove(nm);
    		if(changeTracker != null){
    			changeTracker.currTracker.remove(nm);
    		}
    	}
		kid.pushcount = pushcount;
		return kid;
	}
	
	
	public String transName(String nm){
		if( nm == lastSeen) 
			return lastSeenTrans;
		lastSeen = nm;
		String t = (String) curVT.get(nm);
		if(t != null)
			lastSeenTrans = t;
		else
			if(kid != null)
				lastSeenTrans = kid.transName(nm);
			else
				lastSeenTrans = nm;
		return lastSeenTrans;
	}
}