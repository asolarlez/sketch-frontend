package streamit.frontend.stencilSK;

import streamit.frontend.nodes.ExprStar;
import streamit.frontend.nodes.FEReplacer;
import streamit.frontend.tosbit.ValueOracle;

public class EliminateStarStatic extends FEReplacer {

	ValueOracle oracle;
	
	public EliminateStarStatic(ValueOracle oracle){
		assert oracle.getHoleNamer() instanceof StaticHoleTracker;
		this.oracle = oracle;
		oracle.initCurrentVals();
	}
	
	public Object visitExprStar(ExprStar star) {
		return oracle.popValueForNode(star);
	}
	
}
