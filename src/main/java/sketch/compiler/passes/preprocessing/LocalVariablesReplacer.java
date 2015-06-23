package sketch.compiler.passes.preprocessing;

import java.util.ArrayList;

import sketch.compiler.ast.core.exprs.ExprLocalVariables;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.passes.lowering.SymbolTableVisitor;

/**
 * This replacer takes a local variable expression <code>$$$</code> and replaces
 * it with an available local variable. <br>
 * <br>
 * <code>
 * int a = 0; <br>
 * int b = $$$ + 2;<br>
 * <br>
 * <br>
 * int a = 0;<br>
 * int b = a + 2; <br>
 * </code> <br>
 * 
 * @author Miguel Velez
 * @version 0.1
 */
public class LocalVariablesReplacer extends SymbolTableVisitor {

	/**
	 * Creates a local variables replacer
	 */
    public LocalVariablesReplacer() {
        super(null);
		// System.out.println("***************************Constructor in replacer");
    }

    /**
     * It returns an ExprVar from a group of local variables. Right now, it only works for ints.
     */
	@Override
    public Object visitExprLocalVariables(ExprLocalVariables exp) {
        System.out.println("***************************visit in replacer");
        
        // Create an arrayList of Expressions to store all the available variables to use
        ArrayList<Expression> possibleVariables = this.symtab.getLocalVariablesOfType(exp);
        
		// Loop through the possible variables to find if the statement with the
		// special symbol is in here
		for (Expression variable : possibleVariables) {
			// If the context of this variable is the same as the context
			// of the statement with the symbol
			if (variable.getCx().equals(exp.getCx())) {
				// Remove that variable from the possible list
				possibleVariables.remove(variable);

				// Break this loop and continue
				break;
			}
		}

		// Check if we have possible variables to use
		if (possibleVariables.size() < 1) {
			throw new RuntimeException("You do not have any possible variables to use");
        }

		// TODO this is where I need to pass to the synthesizer to decide which
		// variable to use
		return possibleVariables.get(0);
    }

}
