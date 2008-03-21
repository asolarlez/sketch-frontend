/**
 *
 */
package streamit.frontend.tosbit;

import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import streamit.frontend.nodes.ExprConstInt;
import streamit.frontend.nodes.ExprConstant;
import streamit.frontend.nodes.ExprStar;
import streamit.frontend.nodes.FENode;

/**
 * An oracle that assigns a random value to each control value.
 *
 * @author Chris Jones
 */
public class RandomValueOracle extends ValueOracle {
	protected Map<String, ExprConstInt> valCache = new TreeMap<String, ExprConstInt> ();
	protected Random rand;

	public RandomValueOracle (HoleNameTracker hnt) {
		this (hnt, -1);
	}
	public RandomValueOracle (HoleNameTracker hnt, long seed) {
		super (hnt);
		rand = (seed != -1) ? new Random (seed) : new Random ();
	}

	public ExprConstInt popValueForNode(FENode node){
		String name = getHoleNamer ().getName (node);
		if (!valCache.containsKey (name))
			valCache.put (name, getVal (node, name));
		return valCache.get (name);
	}

	protected ExprConstInt getVal (FENode node, String name) {
		assert (node instanceof ExprStar) : "Unexpected hole type";

		int bitWidth = ((ExprStar) node).getSize ();
		int val = rand.nextInt (1 << bitWidth);

		//System.out.print (val +",");

		return (ExprConstInt) ExprConstant.createConstant (node, ""+val);
	}
}
