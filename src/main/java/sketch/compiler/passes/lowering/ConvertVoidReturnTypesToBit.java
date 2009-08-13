/**
 *
 */
package sketch.compiler.passes.lowering;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.typs.TypePrimitive;

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
			new Function (f, f.getCls (), f.getName (),
					TypePrimitive.bittype, f.getParams (), f.getBody ());
	}
}
