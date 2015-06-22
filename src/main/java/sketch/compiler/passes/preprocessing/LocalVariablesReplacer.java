package sketch.compiler.passes.preprocessing;

import java.util.ArrayList;

import sketch.compiler.ast.core.exprs.ExprConstant;
import sketch.compiler.ast.core.exprs.ExprLocalVariables;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypePrimitive;
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
        ArrayList<ExprVar> possibleVariables = this.symtab.getLocalVariablesOfType(exp.getType());
        
		// Loop through the possible variables to find if the statement with the
		// special symbol is in here
		for (ExprVar variable : possibleVariables) {
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
        
		// Get the local variables of a specific type
		ArrayList<StmtAssign> assignments = this.getLocalStmtAssign();
		
		// Create the list of the available variables to use
		ArrayList<ExprVar> availableVariables = new ArrayList<ExprVar>();

		// Loop through the possible variables
		for (int i = assignments.size() - 1; i >= 0; i--) {

			// If the variable assignment is part of the possible variables
			if (possibleVariables.contains(assignments.get(i).getLhsBase())) {

				// If the variable is currently assigned to 0
				// TODO MIGUEL this is where I'm not sure if we always want to check for 0
				if(assignments.get(i).getRHS().getIValue() == 0) { 
					// Add the variable to the list
					availableVariables.add(assignments.get(i).getLhsBase());
				}

			}

		}

		if(availableVariables.size() < 1) {
			throw new RuntimeException("You do not have any available variables to use");
		}

        // Return the first available variable. This might be the closes one to the expression
        // that needs a variables. Can try to be smarter and pick the best one that gives the
        // best performance, ie the closest one, the one that is not modified later in the code.
		return availableVariables.get(0);
    }

	/**
	 * Loops through this.newFunctions and returns the objects that are
	 * StmtAssign assigned to a variable. TODO check that it loops through only
	 * the local StmtAssign. TODO should also work for assignment to variables.
	 * 
	 * @return
	 */
	private ArrayList<StmtAssign> getLocalStmtAssign() {
		ArrayList<StmtAssign> localAssignements = new ArrayList<StmtAssign>();

		// Loop through all the statements
		for (int i = 0; i < this.newStatements.size(); i++) {
			// We are looking for assignments to get the latest value of the variables
			if (this.newStatements.get(i).getClass() == StmtAssign.class) {
				// Get the statement assignment
				StmtAssign statement = (StmtAssign) this.newStatements.get(i);
				
				// Add the variable to the local assignments
				localAssignements.add(statement);
			}

		}

		return localAssignements;
	}

	/**
	 * Return the local variables that are of the provided type.
	 * 
	 * @param type
	 * @return
	 */
	private ArrayList<ExprVar> getLocalVariablesOfType(ArrayList<ExprVar> possibleVariables, Type type) {
		ArrayList<ExprVar> variablesOfType = new ArrayList<ExprVar>();
			
		// Get the variables that are assignments
		ArrayList<StmtAssign> assignments = this.getLocalStmtAssign();
		
		// Loop through the possible variables
		for (int i = assignments.size() - 1; i >= 0; i--) {
			if (possibleVariables.contains(assignments.get(i).getLhsBase())) {

				// If the provided type is an int
				if (type.equals(TypePrimitive.int32type)) {
					
					//
					if(ExprConstant.class.isAssignableFrom(assignments.get(i).getRHS().getClass())) {
						// Add the variable to the list
						variablesOfType.add(assignments.get(i).getLhsBase());
					}
				}
			}
		}
		
		
//		// Loop through all the variables
//		for (ExprVar variable : possibleVariables) {
//			// Check that the current variable is not the variable that uses the symbol
//			if (variable.getCx().equals(exp.getCx())) {
//				System.out.println("This variable cannot be used since that is the place with the symbol");
//
//				// Go to the next iteration
//				continue;
//			}
//
//			// Loop through the local assignments from the latest
//			for (int i = localStatementAssignments.size() - 1; i >= 0; i--) {
//				
//				// Check if the left hand side is the current variable
//				if (localStatementAssignments.get(i).getLHS().equals(variable)) {
//					
//					// If the variable is assigned to 0
//					if (localStatementAssignments.get(i).getRHS().getIValue() == 0) {
//						// Add the variable to the available variables
//						availableVariables.add(variable);
//					} else {
//						// We cannot use this variable since its current value is not 0
//						break;
//					}
//					
//				}
//
//			}
		
		
		
		return variablesOfType;

	}
}
