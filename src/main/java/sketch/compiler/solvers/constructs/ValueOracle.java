package streamit.frontend.tosbit;

import java.io.IOException;
import java.io.LineNumberReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Map.Entry;

import streamit.frontend.nodes.ExprConstInt;
import streamit.frontend.nodes.ExprStar;
import streamit.frontend.nodes.FENode;

public class ValueOracle {
	private HoleNameTracker holeNamer;
	
	/**
	 * After sketch is resolved, this map will contain the 
	 * value of each variable in store.
	 */
	private Map<String, Boolean> valMap;
	
	
	
	public boolean allowMemoization(){
		return holeNamer.allowMemoization();	
	}
	
	public HoleNameTracker getHoleNamer(){
		return holeNamer;
	}
	
	//The following functions are to be called before resolution.
	
	private int starSizesCaped = -1;
	
	public ValueOracle(HoleNameTracker holeNamer) {
		super();		
		valMap = null;
		this.holeNamer = holeNamer;
	}
	
	/**
	 * 
	 * Register a new variable name for a node.
	 *
	 * @param node
	 * @param vname
	 */
	public String addBinding(Object node){
		return holeNamer.getName(node);		
	}	
	
	
	/**
	 * This function populates the valMap with information
	 * from the file in, about what variables got what values.
	 * @param in
	 * @throws IOException
	 */
	public void loadFromStream(LineNumberReader in) throws IOException{
		String dbRecord = null;
		valMap = new HashMap<String, Boolean>();
		while ( (dbRecord = in.readLine()) != null) {
            StringTokenizer st = new StringTokenizer(dbRecord, "\t");
            String vname = st.nextToken();
            String sval = st.nextToken();
            int val = Integer.parseInt(sval);
            valMap.put(vname, val==1);
         }
	}
	
	/**
	 * Before we begin, populating the sketch, we set all the 
	 * streams to the beginning.
	 */
	
	public void initCurrentVals(){
		holeNamer.reset();
	}
	
	
	
	public ExprConstInt popValueForNode(FENode node){
		String name = holeNamer.getName(node);	
		return getVal(node, name);
	}
	
	protected ExprConstInt getVal(FENode node, String var){
		if(node instanceof ExprStar){
			ExprStar star = (ExprStar) node;
			if(star.getSize() == 1){
				Boolean val = valMap.get(var);
				if(val != null){
					return(new ExprConstInt(node.getCx(), val.booleanValue()?1:0));
				}
			}else{
				int v = 0;
				int p = 1;
				int size=star.getSize();
				if(starSizesCaped>0 && !star.isFixed()){
					size = starSizesCaped;
				}
				for(int i=0; i<size; ++i){
					String nm = var + "_" + i;
					if( valMap.containsKey(nm) ){
						boolean val = valMap.get(nm).booleanValue();
						v += p*(val?1:0);
						p = p*2;
					}else{
						break;
					}
				}
				
				if( p == 1){
					assert v == 0;
					if( valMap.containsKey(var) ){
						boolean val = valMap.get(var).booleanValue();
						v = (val?1:0);
					}
				}
				return(new ExprConstInt(node.getCx(), v));
			}
		}else{
			Boolean val = valMap.get(var);
			if(val != null){
				return(new ExprConstInt(node.getCx(), val.booleanValue()?1:0));
			}
		}
		return new ExprConstInt(node.getCx(), -1);
	}
	

	public void capStarSizes(int size){
		starSizesCaped = size;
	}
}
