/**
 *
 */
package streamit.frontend.passes;

import java.util.List;

import streamit.frontend.nodes.ExprConstant;
import streamit.frontend.nodes.ExprNew;
import streamit.frontend.nodes.FEContext;
import streamit.frontend.nodes.FENode;
import streamit.frontend.nodes.FieldDecl;
import streamit.frontend.nodes.StmtVarDecl;
import streamit.frontend.nodes.Type;
import streamit.frontend.nodes.TypePrimitive;
import streamit.frontend.nodes.TypeStruct;

/**
 * Replaces 'new [struct]()' expressions with pointers into [struct] arrays.
 * Also converts variables of type [struct] into ints.  For example:
 * <code>
 *   struct Foo { ... }
 *   ...
 *   Foo f1 = new Foo (); --> int f1 = Foo.nextIndex++;
 *   Foo f2 = f1;     	  --> int f2 = f1;
 * </code>
 *
 * Preconditions to doing this rewrite:
 *   (1) all loops have been unrolled
 *   (2) all recursion has been eliminated
 *   (3) type checking has already been done
 *
 * @author Chris Jones
 *
 */
public class EliminateNewsAndStructVars extends SymbolTableVisitor {
	public EliminateNewsAndStructVars () {
		super(null);
	}

    public Object visitExprNew (ExprNew expNew){
    	if (false == expNew.getTypeToConstruct().isStruct()) {
    		report (expNew,
    				"Sorry, only structs are supported in new () statements.");
    		throw new RuntimeException ("unsupported type in 'new' statement.");
    	}

    	TypeStruct struct = (TypeStruct) expNew.getTypeToConstruct ();
    	return ExprConstant.createConstant(expNew.getCx (),
    									   ""+ struct.newInstance ());
    }

    public Object visitStmtVarDecl (StmtVarDecl decl) {
    	for (int i = 0; i < decl.getNumVars (); i++) {
    		Type t = decl.getType (i);

    		if (t.isStruct ()) {
    			decl.setType (i, TypePrimitive.inttype);
    		}
    	}

    	return decl;
    }

    public Object visitFieldDecl (FieldDecl decl) {
    	for (int i = 0; i < decl.getNumFields (); i++) {
    		Type t = decl.getType (i);

    		if (t.isStruct ()) {
    			decl.setType (i, TypePrimitive.inttype);
    		}
    	}

    	return decl;
    }

    /**
     * TODO: copied from SemanticChecker.  Needs to be refactored into utility
     * class.
     * @param node
     * @param message
     */
	protected void report(FENode node, String message) {
		report(node.getContext(), message);
	}

	/**
	 * TODO: copied from SemanticChecker.  Needs to be refactored into utility
	 * class.
	 * @param ctx
	 * @param message
	 */
	protected void report(FEContext ctx, String message) {
		System.err.println(ctx + ": " + message);
	}
}
