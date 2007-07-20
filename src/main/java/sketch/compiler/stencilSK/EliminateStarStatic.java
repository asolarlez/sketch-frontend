package streamit.frontend.stencilSK;

import java.util.ArrayList;
import java.util.List;

import streamit.frontend.experimental.abstractValue;
import streamit.frontend.nodes.ExprArrayInit;
import streamit.frontend.nodes.ExprConstInt;
import streamit.frontend.nodes.ExprStar;
import streamit.frontend.nodes.FEReplacer;
import streamit.frontend.nodes.Type;
import streamit.frontend.nodes.TypeArray;
import streamit.frontend.tosbit.ValueOracle;

public class EliminateStarStatic extends FEReplacer {

	ValueOracle oracle;
	
	public EliminateStarStatic(ValueOracle oracle){
		assert oracle.getHoleNamer() instanceof StaticHoleTracker;
		this.oracle = oracle;
		oracle.initCurrentVals();
	}
	
	public Object visitExprStar(ExprStar star) {
		Type t = star.getType();
		int ssz = 1;
		if(t instanceof TypeArray){
			Integer iv = ((TypeArray)t).getLength().getIValue();
			assert iv != null;
			ssz = iv;
			List<ExprConstInt> lst = new ArrayList<ExprConstInt>(ssz);
			for(int i=0; i<ssz; ++i){
				lst.add(oracle.popValueForNode(star.getDepObject(i)));				
			}
			
			ExprArrayInit ainit = new ExprArrayInit(star.getCx(), lst);
			return ainit;			
		}else{
			return oracle.popValueForNode(star.getDepObject(0));
		}
	}
	
}
