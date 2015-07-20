package sketch.compiler.passes.preprocessing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.NameResolver;
import sketch.compiler.ast.core.Package;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.ExprLambda;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.TypeFunction;

/**
 * This replacer provides support for casting of expression passed as arguments
 * to a high order function. The casting returns a lambda function from the
 * expression.
 * 
 * <br>
 * <br>
 * <code>apply(x*x); => apply((,x) -> x*x);</code>
 * 
 * @author Miguel Velez
 * @version 0.1
 */
public class ExpressionCastingReplacer extends FEReplacer {

	private Stack<String> 			functionsToVisit;
	private Map<String, Package> 	packages;
	
	/**
	 * Create a new expression casting replacer
	 */
	public ExpressionCastingReplacer() {
		this.functionsToVisit = new Stack<String>();
		this.packages = new HashMap<String, Package>();
	}
	
	/**
	 * Visit the program and go through it checking if there is any place to
	 * cast an expression as a lambda function
	 */
	public Object visitProgram(Program program) {
		// Create a name resolver
		this.nres = new NameResolver(program);

		// Loop through all packages
		for (Package programPackage : program.getPackages()) {
			// Loop through all functions
			for (Function function : programPackage.getFuncs()) {
				// If the function is a harness
				if (function.isSketchHarness()) {
					// Add it to the list of functions to visit
					this.functionsToVisit.push(function.getName());
				}
			}
		}

		// Create a map for new function
		Map<String, List<Function>> newFunctionMap = new HashMap<String, List<Function>>();

		// Loop each package
		for (Package pkg : program.getPackages()) {
			// Add this package and a list of functions
			newFunctionMap.put(pkg.getName(), new ArrayList<Function>());

			// Add this package to the list of packages
			this.packages.put(pkg.getName(), pkg);
		}

		// While there are functions to visit
		while (!this.functionsToVisit.empty()) {
			// Pop the function out of the stack and get the function
			String currentFunctionName = this.functionsToVisit.pop();
			Function current = this.nres.getFun(currentFunctionName);

			// Visit the function
			current = (Function) this.visitFunction(current);
			
			// Add the new function to the to the list of functions of this package
			newFunctionMap.get(current.getPkg()).add(current);
		}

		// Create a new list of packages
		List<Package> newPkges = new ArrayList<Package>();

		// Loop through the program packages
		for (Package pkg : program.getPackages()) {
			// Create a new package
			newPkges.add(new Package(pkg, pkg.getName(), pkg.getStructs(),
					pkg.getVars(), newFunctionMap.get(pkg.getName())));
		}

		// Create a new program with the new packages
		return program.creator().streams(newPkges).create();

	}

	/**
	 * Visit an expression function call and if there is an expression where a
	 * function should be, create a lambda function
	 */
	public Object visitExprFunCall(ExprFunCall exprFunctionCall) {
		// Find the function that is being called
		Function callee = this.nres.getFun(exprFunctionCall.getName());
		
		// If there is no function with that name
		if (callee == null) { 
			// Make the super class visit the function call since it is not
			// calling a function defined by the user. It is probably a
			// call to a function that is being passed
			return super.visitExprFunCall(exprFunctionCall);
		}
		
		// Since this is a defined function, add it to the list of
		// functions to visit
		this.functionsToVisit.push(exprFunctionCall.getName());

		// Create a list for the new actual parameters
		List<Expression> newActualParameters = new ArrayList<Expression>();

		// Get the current actual parameters
		Iterator<Expression> actualParameters = exprFunctionCall.getParams().iterator();
		
		// We will use this variable to check if the type of the
		// variable is fun
		boolean typeFunction = false;
		
		// Loop through the formal parameters of the function that is being called
		for(Parameter formalParameter : callee.getParams()) {
			// Get the next actual parameter
			Expression actualParameter = actualParameters.next();
			
			// Loop through the new statements
			for(Statement statement : this.newStatements) {
				// If the current statement is a variable declaration
				if(statement instanceof StmtVarDecl) {
					StmtVarDecl variableDeclaration = ((StmtVarDecl)statement);
					
					// TODO
					if(actualParameter.toString().equals(variableDeclaration.getName(0)) &&
							variableDeclaration.getType(0) instanceof TypeFunction) {
						// The current actual parameter is a function declaration	
						typeFunction = true;
						
						// Break and continue logic
						break;
					}

				}
			}
			
			// If the formal parameter wants a function and the corresponding
			// actual parameter is not an instance of a lambda function
			if (formalParameter.getType() instanceof TypeFunction
					&& !(actualParameter instanceof ExprLambda) && !typeFunction) {
				// We need to cast. Create a new lambda with empty formal
				// parameters, but passing the actual expression
				ExprLambda castedLambda = new ExprLambda(actualParameter.getCx(), 
						new ArrayList<ExprVar>(), actualParameter);
				
				// Get the formal parameters that are needed
				List<ExprVar> lambdaFormalParameters = castedLambda.getMissingFormalParameters();
				List<ExprVar> doubleCountedParameters = new ArrayList<ExprVar>();
				
				for(ExprVar missingFormalParameter : lambdaFormalParameters) {
					// Loop through the new statements
					for(Statement statement : this.newStatements) {
						// If the current statement is a variable declaration
						if(statement instanceof StmtVarDecl) {
							StmtVarDecl variableDeclaration = ((StmtVarDecl)statement);
							
							// If the missing parameter is actually a local variable
							if(missingFormalParameter.toString().equals(variableDeclaration.getName(0))) {
								// Add the double counted parameter
								doubleCountedParameters.add(missingFormalParameter);
								
								// Break and continue logic
								break;
							}

						}
					}
					
				}
				
				// Remove the double counted variables
				for (ExprVar doubleCountedVariable : doubleCountedParameters) {
					lambdaFormalParameters.remove(doubleCountedVariable);
				}

				// Create a new lambda
				castedLambda = new ExprLambda(actualParameter.getCx(), lambdaFormalParameters, actualParameter);
				
				// Add it to the new actual parameters
				newActualParameters.add(castedLambda);
			}
			else {
				// Add the parameter to new list
				newActualParameters.add(actualParameter);
			}
		}
		// Return a new function call
		return new ExprFunCall(exprFunctionCall, exprFunctionCall.getName(), newActualParameters);

	}

}
