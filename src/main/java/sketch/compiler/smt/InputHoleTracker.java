package sketch.compiler.smt;

import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.ExprStar;
import sketch.compiler.solvers.constructs.StaticHoleTracker;

public class InputHoleTracker extends StaticHoleTracker {

	public static final String INPUT_PREFIX = "I_";
	
	protected TempVarGen holeVarGen;
	protected TempVarGen inputVarGen;
	
	public InputHoleTracker() {
		super(null);
		this.holeVarGen = new TempVarGen();
		this.inputVarGen = new TempVarGen();
	}
	
	public void newHoleVarGen() {
		
		this.store.clear();
	}
	
	@Override
	public String getName(Object hole) {
		
		String vname = null;
		if(store.containsKey(hole)){
			vname = store.get(hole);
			
		}else{
			if (hole instanceof ExprStar) {
				vname = StaticHoleTracker.HOLE_PREFIX+ holeVarGen.nextVar();
				
			} else if (hole instanceof Function) {
				vname = ((Function) hole).getName() + inputVarGen.nextVar();
			} 
			store.put(hole, vname);

			
		}
		return vname;
	}
}
