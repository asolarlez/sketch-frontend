package sketch.compiler.solvers.constructs;

import java.util.HashMap;
import java.util.Map;

import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.ExprStar;
import sketch.compiler.ast.core.stmts.StmtFor;
import sketch.compiler.ast.core.stmts.StmtLoop;

public class StaticHoleTracker implements HoleNameTracker {
	public static final String HOLE_PREFIX = "H_";
	protected Map<Object, String> store;
	protected TempVarGen varGen;
	
	public boolean allowMemoization(){
		return true;	
	}
	
	public StaticHoleTracker(TempVarGen varGen){
		this.varGen = varGen;
		store = new HashMap<Object, String>();
	}
	
	public String getName(Object hole) {
		if(hole instanceof ExprStar){
			return ((ExprStar)hole).getSname();
		}
		String vname = null;
		if(store.containsKey(hole)){
			vname = store.get(hole);
		}else{
			vname = HOLE_PREFIX + varGen.nextVar();			
			store.put(hole, vname);			
		}
		return vname;		
	}

	public void pushFor(StmtFor floop) {


	}

	public void pushFunCall(ExprFunCall call) {


	}

	public void pushLoop(StmtLoop loop) {


	}

	public void regLoopIter() {


	}

	public void reset() {


	}

}
