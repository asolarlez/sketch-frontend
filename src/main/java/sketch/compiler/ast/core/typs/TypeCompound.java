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
	
	public TypeCompound(Type t) {
		this(Collections.singletonList(t));
	}
	
	public TypeCompound(List types) {
		fTypes=new ArrayList(types);
	}
	
	public List getTypes() {
		return fTypes;
	}
	
	public String toString() {
		if(fTypes.size()==1) return fTypes.get(0).toString();
		StringBuffer ret=new StringBuffer();
		ret.append('['); ret.append(fTypes.get(0));
		for(int i=0;i<fTypes.size();i++) {
			ret.append(',');
			ret.append(fTypes.get(i));
		}
		ret.append(']');
		return ret.toString();
	}
}
