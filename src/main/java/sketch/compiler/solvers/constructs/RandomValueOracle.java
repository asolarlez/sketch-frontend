/**
 *
 */
package sketch.compiler.solvers.constructs;

import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprConstant;
import sketch.compiler.ast.core.exprs.ExprHole;
import sketch.compiler.ast.core.typs.Type;

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

    public ExprConstInt popValueForNode(FENode node, Type t) {
		String name = getHoleNamer ().getName (node);
		if (!valCache.containsKey (name))
			valCache.put (name, getVal (node, name));
		return valCache.get (name);
	}

	protected ExprConstInt getVal (FENode node, String name) {
		assert (node instanceof ExprHole) : "Unexpected hole type";

		int bitWidth = ((ExprHole) node).getSize ();
		int val = rand.nextInt (1 << bitWidth);

		//System.out.print (val +",");

		return (ExprConstInt) ExprConstant.createConstant (node, ""+val);
	}
}
