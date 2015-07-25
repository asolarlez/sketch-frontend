package sketch.compiler.passes.preprocessing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Stack;

import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.NameResolver;
import sketch.compiler.ast.core.Package;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.exprs.ExprArrayInit;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.ExprLambda;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.StmtFunDecl;
import sketch.compiler.ast.core.typs.TypeFunction;
import sketch.compiler.passes.lowering.SymbolTableVisitor;
import sketch.util.exceptions.ExceptionAtNode;

/**
 * This replacer provides support for casting of expression passed as arguments
 * to a high order function. The casting returns a lambda function from the
 * expression.
 * 
 * <br>
 * <br>
 * <code>apply(x*x); => apply((,randomName) -> x*x);</code>
 * 
 * @author Miguel Velez
 * @version 0.1
 */
public class ExpressionCastingReplacer extends SymbolTableVisitor {

	private Function						currentFunction;
	private Stack<String> 					functionsToVisit;
	private Map<String, Package> 			nameToPackage;
	private Set<String> 					visitedFunctions;
	private Map<Function, List<Function>> 	functionToLocalFunctions;
	private Map<String, List<ExprLambda>> 	functionToCastedExpression;
	private Map<String, String> 			parameterToFunction;
		
	/**
	 * Create a new expression casting replacer
	 */
	public ExpressionCastingReplacer() {
		super(null);

		this.currentFunction = null;
		this.functionsToVisit = new Stack<String>();
		this.nameToPackage = new HashMap<String, Package>();
		this.visitedFunctions = new HashSet<String>();
		this.functionToLocalFunctions = new HashMap<Function, List<Function>>();
		this.functionToCastedExpression = new HashMap<String, List<ExprLambda>>();
		this.parameterToFunction = new HashMap<String, String>();
	}
	
	/**
	 * Visit the program to determine the order that functions are going to
	 * be visited. After all functions are visited, a new program is created.
	 */
	public Object visitProgram(Program program) {
		this.nres = new NameResolver(program);

		// Loop each package
		for (Package pkg : program.getPackages()) {
			// Set the package in the name resolver
			nres.setPackage(pkg);

			// Create function set
			Set<String> functionSet = new HashSet<String>();

			// Loop through each function
			for (Function function : pkg.getFuncs()) {
				// Check if name check has the same name as the current function
				if (functionSet.contains(function.getName())) {
					throw new ExceptionAtNode("Duplicated name: " + function.getName() + 
							" in package: " + pkg.getName(), function);
				}

				// Add the function name to name check
				functionSet.add(function.getName());

				// If the function is a harness
				if (function.isSketchHarness()) {
					// Add the harness to the functions to visit
					this.functionsToVisit.push(this.nres.getFunName(function.getName()));
				}

				// If the function implements some other function
				if (function.getSpecification() != null) {

					String specification = nres.getFunName(function.getSpecification());
					if (specification == null)
						throw new ExceptionAtNode("Function " + function.getSpecification()
										+ ", the spec of " + function.getName()
										+ " is can not be found. Did you put the "
										+ "wrong name?", function);

					// Pust both the function and the implementation to visit
					this.functionsToVisit.push(specification);
					this.functionsToVisit.push(nres.getFunName(function.getName()));
				}
			}
		}
		
		// Create a map of packages and their new functions
		Map<String, List<Function>> newPackageFunctionList = new HashMap<String, List<Function>>();

		// Loop each package
		for (Package pkg : program.getPackages()) {
			// Add package with an empty list of function
			newPackageFunctionList.put(pkg.getName(), new ArrayList<Function>());
			this.nameToPackage.put(pkg.getName(), pkg);
		}
		
		// While we have function to visit.
		while (!this.functionsToVisit.isEmpty()) {
			// Get the latest function information
			String functionName = this.functionsToVisit.pop();
			String pkgName = this.getPkgName(functionName);
			Function function = nres.getFun(functionName);

			// If the function has been visited
			if (this.visitedFunctions.contains(functionName)) {
				// Get the next iteration of the loop
				continue;
			}

			// Set that this is the current function
			this.currentFunction = function;

			// Create a new list of local functions for this function
			this.functionToLocalFunctions.put(this.currentFunction,
					new ArrayList<Function>());

			// Visit this function
			function = (Function) function.accept(this);
			
			// Add it to the set of visited functions
			this.visitedFunctions.add(functionName);

			// Add it to the map of packages and their new function
			newPackageFunctionList.get(pkgName).add(function);
		}
		
		// Loop through each package
		for (Package pkg : program.getPackages()) {
			for (Function function : pkg.getFuncs()) {
				// If the are some functions that we did not visit
				if (!this.visitedFunctions.contains(function.getFullName())) {
					// Add them to the new package list. We do this since the
					// sketch might be using another file with generators that
					// have not been used yet, but will be used later. So we
					// need to include all of those functions
					newPackageFunctionList.get(pkg.getName()).add(function);
				}
			}
		}

		// Create a list of new packages
		List<Package> newPkges = new ArrayList<Package>();
		
		// Loop through each package
		for (Package pkg : program.getPackages()) {
			// Create a new package with the new functions
			newPkges.add(new Package(pkg, pkg.getName(), 
					pkg.getStructs(), pkg.getVars(), 
					newPackageFunctionList.get(pkg.getName())));
		}
		
		// Create a new program with the new packages
		Program newProgram = program.creator().streams(newPkges).create();
		return newProgram;
	}

	/**
	 * Visit an expression function call that does a fairly complicated job.
	 * 
	 * <br>
	 * <br>
	 * 
	 * Store the function in the list of functions to visit.
	 * 
	 * <br>
	 * <br>
	 * 
	 * Loop through the parameters to add any other functions in the list of
	 * functions to visit
	 * 
	 * <br>
	 * <br>
	 * 
	 * Check if a function call should be casted to a lambda and add formal
	 * parameters to a created lambda.
	 */
	public Object visitExprFunCall(ExprFunCall exprFunctionCall) {
		// Get the function name from the list of function
		String functionName = this.nres.getFunName(exprFunctionCall.getName());

		// If the function was found
		if (functionName != null) {
			// If the function has not been visited before
			if (!this.visitedFunctions.contains(functionName)) {
				// Add it to the list of functions to visit
				this.functionsToVisit.push(functionName);
			}
		}
		
		// Loop through each actual parameter
		for (Expression actualParameter : exprFunctionCall.getParams()) {
			// If the actual parameter is a variable
			if(actualParameter instanceof ExprVar) {
				// Get the variable
				ExprVar variable = (ExprVar) actualParameter;
				
				// Check if there is a function with that name
				functionName = this.nres.getFunName(variable.getName());
				
				// If the function was found
				if (functionName != null) {
					// If the function has not been visited before
					if (!this.visitedFunctions.contains(functionName)) {
						// Add it to the list of functions to visit
						this.functionsToVisit.push(functionName);
					}
				}
				
			}
        }
		
		// If this is a function call to a formal parameter that had
		// an expression casted to a lambda
		if (this.parameterToFunction.containsKey(exprFunctionCall.getName())) {
			// Create a new random object
			Random random = new Random();

			// Get the function with the position of the parameter that 
			// we are referencing
			String functionParameter = this.parameterToFunction.get(exprFunctionCall.getName());
			
			// Get the list of the casted lambdas for this specific parameter in the function
			List<ExprLambda> castedLambdas = this.functionToCastedExpression.get(functionParameter);
			
			// Get the number of parameters that this function call uses
			int numberParamerters = exprFunctionCall.getParams().size();

			// Loop through the casted lambda
			for(ExprLambda lambda : castedLambdas) {
				// Create a new list for variables
				List<ExprVar> variables = new ArrayList<ExprVar>();
				
				// Repeat the following process for the number of variables needed
				for(int i = 0; i < numberParamerters; i++) {
					// Create a new variable with a random name
					variables.add(new ExprVar(exprFunctionCall,"dummyVariable" + random.nextInt()));
				}
				
				// Set the parameters of the lambda to these variables
				lambda.setParameteres(variables);
				
			}
					
		}

		// Get the function name from the list of functions
		functionName = this.nres.getFunName(exprFunctionCall.getName());
		
		// If the function was not found
		if(functionName == null) {
			// Visit the function call since it is probably a call to a
			// formal parameter that does not need any processing
			return super.visitExprFunCall(exprFunctionCall);			
		}
		
		// Get the function that we are calling
		Function callee = this.nres.getFun(functionName);
		// Get its formal parameters
		Iterator<Parameter> calleFormalParameters = callee.getParams().iterator();
		
		Parameter formalParameter = null;
		List<Expression> newParams = new ArrayList<Expression>();
		
		// Index of the formal parameters
		int i = -1;

		// Loop through each actual parameter
		for (Expression actualParameter : exprFunctionCall.getParams()) {
			// Increase the position of the formal parameter. If this is
			// the first iteration through the loop, this is 0.
			i++;

			// Get the next formal parameter
			formalParameter = calleFormalParameters.next();
					
			// If the formal parameter is not fun type
			// or the actual parameter is a lambda
			// or the actual parameter is an array initializer
			if (!(formalParameter.getType() instanceof TypeFunction)
					|| actualParameter instanceof ExprLambda
					|| actualParameter instanceof ExprArrayInit) {
				// Visit the actual parameter and add it to the
				// list of new parameters
				newParams.add(this.doExpression(actualParameter));

				// We do not need any further work in this case
				continue;
			}
			
			// If the actual parameter is a variable
			if(actualParameter instanceof ExprVar) {
				// Get the variable
				ExprVar variable = (ExprVar) actualParameter;
				
				// Check if there is a function with that name
				functionName = this.nres.getFunName(variable.getName());

				// Check if there is a local function with that name
				List<Function> innerFunctions = this.functionToLocalFunctions.get(this.currentFunction);
				
				// Loop through the inner functions
				for(Function function : innerFunctions) {
					// If the variable is a reference to a local function
					if (function.getName().equals(variable.getName())) {
						// Set the function name and break
						functionName = function.getName();
						break;
					}
				}
				
				// There is a local variable with this name that
				// is a lambda function
				if (this.symtab.hasVar(variable.getName()) && this.symtab
						.lookupVar(variable) instanceof TypeFunction) {
					functionName = variable.getName();
				}

				// If the function was found somewhere
				if (functionName != null) {
					// Visit the actual parameter and add it to the
					// list of new parameters
					newParams.add(this.doExpression(actualParameter));

					// We do not need any further work in this case
					continue;
				}
			}

			// If all the above checks have failed. It means that we need
			// to cast. Keep in mind that we need to cast only where we 
			// do not use the parameters inside the function.
			// Create a new lambda with empty formal parameters and the
			// actual parameter assigned as the expression
			ExprLambda castedLambda = new ExprLambda(actualParameter,new ArrayList<ExprVar>(), actualParameter);

			// Get the variables used in the expression that are not in the
			// formal parameters. In this case, it will be all of them
			List<ExprVar> variablesUsedInExpression = castedLambda.getMissingFormalParameters();
			
			// Loop through each variable
			for(ExprVar variable : variablesUsedInExpression) {
				// If this variable is not in scope of this call
				if(!this.symtab.hasVar(variable.getName())) {
					// Throw an exception since we can only cast expressions
					// with variables that are in scope
					throw new ExceptionAtNode("You are passing an expression to a"
							+ " high-order function and you are using a variable "
									+ "that is not defined within scope: "
									+ variable
							, actualParameter);
				}
			}
			
			// If the parameter of this function does not have any casted expressions
			if (!this.functionToCastedExpression.containsKey(exprFunctionCall.getName() + i)) {
				// Create a new entry for this parameter
				this.functionToCastedExpression.put(exprFunctionCall.getName() + i, new ArrayList<ExprLambda>());
			}
			
			// Map the lambda to this parameter
			this.functionToCastedExpression.get(exprFunctionCall.getName() + i).add(castedLambda);

			// Add the lambda to the list of new parameters
			newParams.add(castedLambda);
        }
		
		// Return the new expression function call
		return new ExprFunCall(exprFunctionCall, exprFunctionCall.getName(), newParams);		

	}

	/**
	 * Visit function declaration statements or local functions, to work on them
	 * and make sure that we keep track of them. If a function call calls a
	 * local function, it will generate an error, since the name replacer does
	 * not know of this local variables. So we register all the local functions
	 * of each function to avoid this error. Keep in mind that local functions
	 * are not added to the list of functions to visit, since this will hoist
	 * that function. We do visit and process it, but we do not add it to the
	 * list of functions to visit.
	 */
	public Object visitStmtFunDecl(StmtFunDecl stmtFuncDecl) {
		// Register that the current function has a local function
		this.functionToLocalFunctions.get(this.currentFunction).add(stmtFuncDecl.getDecl());
		
		// Get the current function
		Function oldFunction = this.currentFunction;
		
		// The new current function is this local function
		this.currentFunction = stmtFuncDecl.getDecl();
		
		// Create a new entry of local functions since it might have some
		this.functionToLocalFunctions.put(this.currentFunction, new ArrayList<Function>());
		
		// Visit this local function
		stmtFuncDecl = (StmtFunDecl) super.visitStmtFunDecl(stmtFuncDecl);
		
		// Reset the old function
		this.currentFunction = oldFunction;
		
		// Return the stmtFunDecl
		return stmtFuncDecl;
	}

	/**
	 * Visit the function to loop through its parameters. If they are of type
	 * fun and we casted an expression being passed to that parameter, we put
	 * the name of the variable with its position in the formal parameters to
	 * later know how many parameters the new lambda needs.
	 * 
	 * <br>
	 * <br>
	 * 
	 * <code>
	 * int apply(fun f) {<br>
	 * &nbsp;&nbsp;	return f(3,4,5); <br>
	 * } <br>
	 * <br>
	 * harness void main() { <br>
	 * &nbsp;&nbsp; int a = 15; <br>
	 * &nbsp;&nbsp; assert apply(a) == 15; <br>
	 * } <br>
	 * </code>
	 * 
	 * <br>
	 * 
	 * The casted lambda should have 3 formal parameters since the call to f
	 * uses 3 parameters. At this point, the casted lambda was created, but it
	 * does not have any formal parameters. Now that we now the name of the
	 * parameter, we can later add random variables to the formal parameter so
	 * that the call is valid.
	 * 
	 */
	public Object visitFunction(Function function) {
		// Index to determine position of the formal parameters
		int i = -1;
		// Loop through the parameters
		for (Parameter p : function.getParams()) {
			// Increase the position of the formal parameters. If this is the
			// first iteration through the loop, it will be 0
			i++;
			// If the parameter is type fun
			if (p.getType() instanceof TypeFunction) {
				// We might have done some casting. Check if this function had any
				// casted expressions at this position
				if (this.functionToCastedExpression
						.containsKey(function.getName() + i)) {
					// Map the name of the parameter to the function name with the lambdas
					this.parameterToFunction.put(p.getName(),
							function.getName() + i);
				}
			}
		}

		// Visit the function
		return super.visitFunction(function);
	}

	/**
	 * Get the package name from the function name
	 * 
	 * @param functionName
	 * @return
	 */
	private String getPkgName(String functionName) {
		int i = functionName.indexOf("@");
		return functionName.substring(i + 1);
	}
	
}
