package sketch.compiler.smt;

import java.util.Comparator;
import java.util.PriorityQueue;

import sketch.compiler.smt.partialeval.NodeToSmtValue;
import sketch.util.Pair;
import sketch.util.IPredicate.HoleVarPredicate;

public class HoleSorter {
	private Integer[] orderToHoleValueMap;


	public HoleSorter(SmtOracle oracle) {
		PriorityQueue<Pair<String, Integer>> holeIdQueue = 
			new PriorityQueue<Pair<String, Integer>>(10, 
					new Comparator<Pair<String, Integer>>() {
			    
                    public int compare(Pair<String, Integer> o1,
                            Pair<String, Integer> o2) {
                        String n1 = o1.getFirst();
                        String n2 = o2.getFirst();
                        if (n1.length() != n2.length())
                            return n1.length() - n2.length();
                        return n1.compareTo(n2);
                    }	
		});
		
		
		for (String var : oracle.keySet()) {
			if (var.startsWith(HoleVarPredicate.HOLE_PREFIX)) {
				String holeId = var.substring(var.indexOf(HoleVarPredicate.HOLE_PREFIX));
				
				NodeToSmtValue v = oracle.getValueForVariable(var, null);
				holeIdQueue.add(new Pair<String, Integer>(holeId, v.getIntVal()));
			
			}
		}
		
		orderToHoleValueMap = new Integer[holeIdQueue.size()];
		for (int i = 0; i < orderToHoleValueMap.length; i++) {
			orderToHoleValueMap[i] = holeIdQueue.poll().getSecond();
		}
	}

	public int getHoleValueByOrder(int order) { 
		return orderToHoleValueMap[order];
	}
}
