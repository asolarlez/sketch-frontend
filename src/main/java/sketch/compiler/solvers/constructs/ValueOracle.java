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
	private Map<FENode, List<String>> store;	
	private Map<FENode, Iterator<String>> currentVal;
	private Map<String, Boolean> valMap;
	private int starSizesCaped = -1;
	
	public void initCurrentVals(){
		currentVal = new HashMap<FENode, Iterator<String>>();
		for(Iterator<Entry<FENode, List<String>>> it = store.entrySet().iterator(); it.hasNext(); ){
			Entry<FENode, List<String>> ent = it.next();
			currentVal.put(ent.getKey(),ent.getValue().iterator());
		}
	}
	
	public void capStarSizes(int size){
		starSizesCaped = size;
	}
	
	public ValueOracle() {
		super();
		store = new HashMap<FENode, List<String>>();
		valMap = new HashMap<String, Boolean>();
	}
	
	public void addBinding(FENode node, String vname){
		if(store.containsKey(node)){
			store.get(node).add(vname);
		}else{
			List<String> lst = new LinkedList<String>();
			lst.add(vname);
			store.put(node, lst);
		}
	}	
	
	public ExprConstInt popValueForNode(FENode node){
		Iterator<String> cit = currentVal.get(node);
		assert cit.hasNext() : "This can't happen";		
		return getVal(node, cit.next());
	}
	
	public ExprConstInt getVal(FENode node, String var){
		if(node instanceof ExprStar){
			ExprStar star = (ExprStar) node;
			if(star.getSize() == 1){
				Boolean val = valMap.get(var);
				if(val != null){
					return(new ExprConstInt(node.getContext(), val.booleanValue()?1:0));
				}
			}else{
				int v = 0;
				int p = 1;
				int size=star.getSize();
				if(starSizesCaped>0 && !star.isFixed()){
					size = starSizesCaped;
				}
				for(int i=0; i<size; ++i){
					boolean val = valMap.get(var + "_" + i).booleanValue();
					v += p*(val?1:0);
					p = p*2;
				}
				return(new ExprConstInt(node.getContext(), v));
			}
		}else{
			Boolean val = valMap.get(var);
			if(val != null){
				return(new ExprConstInt(node.getContext(), val.booleanValue()?1:0));
			}
		}
		return new ExprConstInt(node.getContext(), -1);
	}
	
	public List<ExprConstInt> getVarsForNode(FENode node){
		List<ExprConstInt> result= new LinkedList<ExprConstInt>();
		List<String> varsForNode = store.get(node);
		if(varsForNode == null) return result;
		Iterator<String> it = varsForNode.iterator();
		while(it.hasNext()){
			String var = it.next();
			result.add(getVal(node, var));
		}
		return result;
	}
	
	
	public void loadFromStream(LineNumberReader in) throws IOException{
		String dbRecord = null;
		
		while ( (dbRecord = in.readLine()) != null) {
            StringTokenizer st = new StringTokenizer(dbRecord, "\t");
            String vname = st.nextToken();
            String sval = st.nextToken();
            int val = Integer.parseInt(sval);
            valMap.put(vname, val==1);
         }
	}
	
}
