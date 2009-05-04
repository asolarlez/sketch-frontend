package streamit.frontend.tosbit;

import java.io.IOException;
import java.io.LineNumberReader;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import streamit.frontend.nodes.ExprConstInt;
import streamit.frontend.nodes.FENode;

public class ValueOracle extends AbstractValueOracle {
	/**
	 * After sketch is resolved, this map will contain the
	 * value of each variable in store.
	 */
	protected Map<String, Integer> valMap;



	protected int starSizesCaped = -1;

	public ValueOracle(HoleNameTracker holeNamer) {
		super();
		valMap = null;
		this.holeNamer = holeNamer;
	}

	public void loadFromStream(LineNumberReader in) throws IOException{
		String dbRecord = null;
		valMap = new HashMap<String, Integer>();
		while ( (dbRecord = in.readLine()) != null) {
            StringTokenizer st = new StringTokenizer(dbRecord, "\t");
            String vname = st.nextToken();
            String sval = st.nextToken();
            int val = Integer.parseInt(sval);
            valMap.put(vname, val);
         }
	}

	protected ExprConstInt getVal(FENode node, String var){
		Integer val = valMap.get(var);
		if(val != null){
			return(new ExprConstInt(node, val.intValue()));
		}		
		return new ExprConstInt(node, -1);
	}
	
	public ExprConstInt popValueForNode(FENode node) {
		String name = holeNamer.getName(node);
		return getVal(node, name);
	}

	public void capStarSizes(int size) {
		starSizesCaped = size;
	}
}