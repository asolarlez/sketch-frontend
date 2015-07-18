package sketch.compiler.ast.core.exprs;

import java.util.List;

import sketch.compiler.ast.core.FEContext;
import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.FEVisitor;
import sketch.compiler.ast.core.SymbolTable;
import sketch.compiler.ast.core.typs.Type;

/**
 * An expression that represents the desire to use local variables in the
 * context. The symbol is <code>$(type)</code>.
 * 
 * @author Miguel Velez
 * @version 0.2
 */
public class ExprLocalVariables extends Expression {

	private Type 				type;
	private SymbolTable 		symbolTableInContext;

	/**
	 * Creates a new local variable expression by passing a front end node.
	 * 
	 * @param context
	 * @param type
	 */
	public ExprLocalVariables(FENode context, Type type) {
        super(context);
        
		this.type = type;
		this.symbolTableInContext = null;
    }

	/**
	 * Creates a new local variable expression by passing a front end context.
	 * 
	 * @param context
	 * @param type
	 */
	public ExprLocalVariables(FEContext context, Type type) {
        super(context);

		this.type = type;
		this.symbolTableInContext = null;
    }

    /**
     * Calls an appropriate method in a visitor object with this as a parameter.
     *
     * @param v  visitor object
     * @return   the value returned by the method corresponding to
     *           this type in the visitor object
     */
    @Override
    public Object accept(FEVisitor visitor) {
        // Visit a local variable expression
        return visitor.visitExprLocalVariables(this);
    }

	/**
	 * Return the type of the local variable expression
	 * 
	 * @return
	 */
	public Type getType() {
		return this.type;
	}

	/**
	 * Set the type of the local variable expression
	 * 
	 * @param type
	 */
	public void setType(Type type) {
		this.type = type;
	}

	/**
	 * Return the symbol table that the expression uses to choose among the
	 * possible variables
	 * 
	 * @return
	 */
	public SymbolTable getSymbolTableInContext() {
		return this.symbolTableInContext;
	}

	/**
	 * Set the symbol table that the expression will use to derive possible
	 * variables.
	 * 
	 * @param symbolTableInContext
	 */
	public void setSymbolTableInContext(SymbolTable symbolTableInContext) {
//		// Get a clone of the symbol table and create a new table
//		SymbolTable temp = new SymbolTable((SymbolTable) symbolTableInContext.clone());
//		
//		// The constructor actually creates a new empty table with the passed parameter
//		// as the parent. So get the parent to have the same state as before.
//		this.symbolTableInContext = temp.getParent();
	}

	@Override
	public String toString() {
		if (this.getType() == null) {
			return "$(null)";
		} else {
			return "$(" + this.getType() + ")";
		}
	}

}
