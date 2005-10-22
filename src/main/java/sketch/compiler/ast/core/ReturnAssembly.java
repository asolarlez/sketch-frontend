package streamit.frontend.nodes;

import java.util.List;

/**
 * This class describes the return type of a function, which could be
 * a collection of types in the case of a function with multiple returns.
 * @author liviu
 */
public class ReturnAssembly {
	private List types;
	
	public ReturnAssembly(List typesList) {
		this.types=typesList;
	}
	
	public int count() {
		return types.size();
	}
	
	public Type getType(int x) {
		return (Type)types.get(x);
	}

	public String getName(int x) {
		return "_out_"+x;
	}
}
