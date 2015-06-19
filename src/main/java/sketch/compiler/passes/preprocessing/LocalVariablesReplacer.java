package sketch.compiler.passes.preprocessing;

import java.util.ArrayList;

import sketch.compiler.ast.core.exprs.ExprLocalVariables;
import sketch.compiler.ast.core.exprs.ExprVar;
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
        System.out.println("***************************Constructor in replacer");
    }

    /**
     * It returns an ExprVar from a group of local variables. Right now, it only works for ints.
     */
	@Override
    public Object visitExprLocalVariables(ExprLocalVariables exp) {
        System.out.println("***************************visit in replacer");
        
        // Create an arrayList of ExprVar to store all the available variables to use
        ArrayList<ExprVar> availableVariables = this.symtab.getLocalVariables();
        
        // Check if we have available variables to use
        if (availableVariables.size() < 1) {
            throw new RuntimeException("You do not have any available variables to use");
        }
        
        // Loop through all the variables just to test
        for (ExprVar variable : availableVariables) {
            System.out.println("Available variables: " + variable.getName());
			System.out.println("Type: " + this.symtab.lookupVar(variable));
			
			if (variable.getCx().equals(exp.getCx())) {
				System.out.println("This variable cannot be used since that is the place with the symbol");
				continue;
			}
			System.out.println(this.newStatements.lastIndexOf(variable));
        }
        
        // Return the first available variable. This might be the closes one to the expression
        // that needs a variables. Can try to be smarter and pick the best one that gives the
        // best performance, ie the closest one, the one that is not modified later in the code.
        return availableVariables.get(0);
    }
}
