package sketch.compiler.passes.preprocessing;

import java.util.ArrayList;
import java.util.List;

import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.ExprBinary;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprConstant;
import sketch.compiler.ast.core.exprs.ExprLambda;
import sketch.compiler.ast.core.exprs.ExprLocalVariables;
import sketch.compiler.ast.core.exprs.ExprStar;
import sketch.compiler.ast.core.exprs.ExprTernary;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.exprs.regens.ExprRegen;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssert;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtBlock;
import sketch.compiler.ast.core.stmts.StmtLoop;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.passes.lowering.SymbolTableVisitor;
import sketch.util.Misc;
import sketch.util.exceptions.ExceptionAtNode;

/**
 * This replacer takes a local variable expression <code>$(type)</code> and
 * replaces it with an available local variable. <br>
 * <br>
 * <code>
 * int a = 0; <br>
 * int b = $(type) + 2;<br>
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

	private TempVarGen tempVarGen;
	protected List<Statement> globalDeclarations;
	List<Expression> possibleVariables;

	/**
	 * Creates a local variables replacer
	 */
	public LocalVariablesReplacer(TempVarGen varGen) {
        super(null);

		this.tempVarGen = varGen;
		this.globalDeclarations = new ArrayList<Statement>();
		this.possibleVariables = new ArrayList<Expression>();
    }

    /**
     * It returns an ExprVar from a group of local variables. Right now, it only works for ints.
     */
	@Override
    public Object visitExprLocalVariables(ExprLocalVariables exp) {
		// System.out.println("***************************visit in replacer");
        
        // Create an arrayList of Expressions to store all the available variables to use
		List<Expression> variablesInScope = this.symtab.getLocalVariablesOfType(exp);

		// Loop through the variables in scope
		for (Expression variable : variablesInScope) {
			// If there is a variable in the scope that has the same name
			// as a formal parameter of a lambda expression, don't add it
			if (!this.possibleVariables.contains(variable)) {
				// Add the variable
				this.possibleVariables.add(variable);
			}
		}

		// Check if we have possible variables to use
		if (this.possibleVariables.size() < 1) {
			throw new ExceptionAtNode("You do not have any possible variables to use of type: " + exp.getType(), exp);
        }
		
		// Start building a regex of variables with "{|"
		StringBuilder variablesRegex = new StringBuilder("{|");

		// Loop through the possible variables adding them to the regex
		for (Expression variable : this.possibleVariables) {
			variablesRegex.append(variable + "|");
		}

		// Close the regex with "}"
		variablesRegex.append("}");
		
		// Return the regex with the possible variables and let future passes do the rest
		return new ExprRegen(exp, variablesRegex.toString());

		//		// Genereate a regex so that the synthesizer figures out which variable to use
//		return this.getVariableConditional(exp, possibleVariables);
    }
	
	/**
	 * Loop through the variables of the lambda expression since those should be
	 * considered when solving.
	 * 
	 * @param exprLambda
	 * @return
	 */
	@Override
	public Object visitExprLambda(ExprLambda exprLambda) {
		// Loop through the formal parameters
		for (ExprVar formalParameter : exprLambda.getParameters()) {
			// Add them to the list of possible variables
			this.possibleVariables.add(formalParameter);
		}

		return super.visitExprLambda(exprLambda);
	}

	@Override
	public Object visitFunction(Function func) {
		// Initialized the global declarations list
		this.globalDeclarations = new ArrayList<Statement>();
		
		// Visit a function and return a new function
		Function newFunction = (Function) super.visitFunction(func);
		
		// If there are global declarations
		if (this.globalDeclarations.size() > 0) {
			// Add the body of the new function to the global declarations
			this.globalDeclarations.add(newFunction.getBody());

			// Return a new function whose body is the global declarations
			return newFunction.creator().body(new StmtBlock(this.globalDeclarations)).create();
		}
		
		// Return new function
		return newFunction;
	}

	public Object visitStmtLoop(StmtLoop loop) {
		// Save the current global declarations in a temp variable
		List<Statement> tmpGlobalDeclarations = this.globalDeclarations;
		
		// Initialize the global declarations to a new lis
		this.globalDeclarations = new ArrayList<Statement>();
		
		// Visit a statement loop and return a new one
		StmtLoop newLoop = (StmtLoop) super.visitStmtLoop(loop);
				
		// If there are global declarations
		if (this.globalDeclarations.size() > 0) {
			// Add the new loop body in the global declarations
			this.globalDeclarations.add(newLoop.getBody());
			
			// Create a new loop whose body is the global declarations
			newLoop = new StmtLoop(newLoop, newLoop.getIter(), new StmtBlock(this.globalDeclarations));
		}
		
		// Reset the global declarations 
		this.globalDeclarations = tmpGlobalDeclarations;
		
		// Return the new loop
		return newLoop;
	}

	/**
	 * Generates a regex expression based on the paremeters passed.
	 * 
	 * @param possibleVariables
	 * @return
	 */
	private Object getVariableConditional(FENode context, ArrayList<Expression> possibleVariables) {
		// If there is only 1 possible variable, just return it
		if (possibleVariables.size() == 1) {
			return possibleVariables.get(0);
		}

		// Create a new temp variable
		ExprVar tempVariable = this.generateChoiceExpression(possibleVariables.size(), context, "_variableInScope");

		// Return a conditional built using the temp variable
		return this.toConditional(tempVariable, possibleVariables, 0);
	}

	/**
	 * Create a variable to represent the unknown value and create expressions that will be used to find
	 * that unknown.
	 * 
	 * @param numberOfChoices
	 * @param context
	 * @param prefix
	 * @return
	 */
	private ExprVar generateChoiceExpression(int numberOfChoices, FENode context, String prefix) {
		// Create a variable that represents the unknown expressions
		ExprVar variable = new ExprVar(context, this.tempVarGen.nextVar(prefix));

		// Create a variable declaration, initialized to null, using the newly
		// created variable
		StmtVarDecl statement = new StmtVarDecl(context, TypePrimitive.inttype, variable.getName(), null);
		
		// Add this statement to the global declarations
		this.globalDeclarations.add(statement);

		// Create a star expression from the variable
		ExprStar startExpression = new ExprStar(variable, Misc.nBitsBinaryRepr(numberOfChoices));
		
		// Set the type of the start expression
		startExpression.setType(TypePrimitive.inttype);
		
		// Add a new statement assignment of the variable set to the star
		this.globalDeclarations.add(new StmtAssign(variable, startExpression));
		
		// Create a binary expression 0 <= variable
		ExprBinary leftBinary = new ExprBinary(ExprConstInt.zero, "<=", variable);
		
		// Create a binary expression variable < numberOfChoices
		ExprBinary rightBinary = new ExprBinary(variable,"<", ExprConstant.createConstant(variable, "" + numberOfChoices));
		
		// Create a nested binary expression to find the value of the unknown variable
		ExprBinary binary = new ExprBinary(leftBinary, "&&", rightBinary);
		
		// Create an assert statement of newly create binary expression
		StmtAssert assertion = new StmtAssert(context, binary, "regen " + startExpression.getSname(), StmtAssert.UBER);

		// Add the assertion to the global declarations
		this.globalDeclarations.add(assertion);

		// Return the variable of the unknown expression
		return variable;
	}
	
	/**
	 * Build a recursive conditional using the temp variable and the possible variables
	 * 
	 * @param variable
	 * @param possibleVariables
	 * @param i
	 * @return
	 */
	private Expression toConditional (ExprVar variable, List<Expression> possibleVariables, int i) {
		// If this is the last possible variable
        if ((i+1) == possibleVariables.size ())
			// Return the variable
            return possibleVariables.get (i);
        else {
			// If there are more variables, create an expression of the variables
        	Expression constant = ExprConstant.createConstant(variable, "" + i);
        	
			// Create a new binary expression
			Expression conditional = new ExprBinary(variable, "==", constant);
			
			// Recursively create the false expression
			Expression falseExpression = this.toConditional(variable, possibleVariables, i + 1);
			
			// Build a return a ternary expression
			return new ExprTernary("?:", conditional, possibleVariables.get(i), falseExpression);
        }
    }

}
