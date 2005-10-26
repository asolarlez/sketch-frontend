package streamit.frontend.nodes;

import java.util.*;

/**
 * Container for a group of types; used as return "type" for functions that
 * return multiple values.
 *  
 * @author liviu
 */
public class TypeCompound extends Type {
	private final List fTypes;
	
	public TypeCompound(List types) {
		fTypes=new ArrayList(types);
	}
	
	public List getTypes() {
		return fTypes;
	}
}
