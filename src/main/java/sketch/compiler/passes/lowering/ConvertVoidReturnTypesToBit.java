/**
 *
 */
package streamit.frontend.passes;

import streamit.frontend.nodes.Function;
import streamit.frontend.nodes.Type;
import streamit.frontend.nodes.TypePrimitive;

/**
 * Convert functions that return type 'void' to ones that return type 'bit'.
 *
 * @author Chris Jones
 */
public class ConvertVoidReturnTypesToBit extends SymbolTableVisitor {
	public ConvertVoidReturnTypesToBit () {
		super (null);
	}

	public Object visitFunction (Function f) {
		return !(f.getReturnType ().equals (TypePrimitive.voidtype)) ? f :
			new Function (f.getCx (), f.getCls (), f.getName (),
					TypePrimitive.bittype, f.getParams (), f.getBody ());
	}
}
