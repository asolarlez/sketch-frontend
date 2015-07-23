package sketch.compiler.passes.preprocessing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import sketch.compiler.ast.core.stmts.StmtFunDecl;
import sketch.compiler.ast.core.typs.TypeFunction;
import sketch.util.exceptions.ExceptionAtNode;

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

	private Function						currentFunction;
	private Stack<String> 					functionsToVisit;
	private Map<String, Package> 			packages;
	private Set<String> 					visitedFunctions;
	private Map<Function, List<Function>> 	functionsInnerFunctions;
		
	/**
	 * Create a new expression casting replacer
	 */
	public ExpressionCastingReplacer() {
		this.currentFunction = null;
		this.functionsToVisit = new Stack<String>();
		this.packages = new HashMap<String, Package>();
		this.visitedFunctions = new HashSet<String>();
		this.functionsInnerFunctions = new HashMap<Function, List<Function>>();
	}
	
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
			this.packages.put(pkg.getName(), pkg);
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
			this.currentFunction = function;

			this.functionsInnerFunctions.put(this.currentFunction, new ArrayList<Function>());

			// Visit this function
			function = (Function) function.accept(this);
			

			// Add it to the set of visited functions
			this.visitedFunctions.add(functionName);

			// Add it to the map of packages and their new function
			newPackageFunctionList.get(pkgName).add(function);
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
	 * Visit an expression function call to put the functions in the list of
	 * functions to visit.
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

		// Get the function name from the list of function
		functionName = this.nres.getFunName(exprFunctionCall.getName());
		
		if(functionName == null) {
			return super.visitExprFunCall(exprFunctionCall);			
		}
		
		Function callee = this.nres.getFun(functionName);
		Iterator<Parameter> formalParameters = callee.getParams().iterator();
		Parameter formalParameter = null;
		
		// Loop through each actual parameter
		for (Expression actualParameter : exprFunctionCall.getParams()) {
			formalParameter = formalParameters.next();
					
			// If the actual parameter is a variable
			if(formalParameter.getType() instanceof TypeFunction) {
				if(actualParameter instanceof ExprLambda) {
					continue;
				}
				
				if(actualParameter instanceof ExprVar) {
					ExprVar variable = (ExprVar) actualParameter;
					
					// Check if there is a function with that name
					functionName = this.nres.getFunName(variable.getName());
					
					// If the function was found
					if (functionName != null) {
						continue;
					}
					
					// Check if there is a function with that name
					List<Function> innerFunctions = this.functionsInnerFunctions.get(this.currentFunction);
					
					for(Function function : innerFunctions) {
						if (function.getName().equals(variable.getName())) {
							functionName = function.getName();
							break;
						}
					}
					
					// If the function was found
					if (functionName != null) {
						continue;
					}
				}
				
				System.out.println("I think we need to cas t");

			}
        }
		
		return super.visitExprFunCall(exprFunctionCall);		

	}

	public Object visitStmtFunDecl(StmtFunDecl stmtFuncDecl) {
		// Register that the current function has a local function
		this.functionsInnerFunctions.get(this.currentFunction).add(stmtFuncDecl.getDecl());
		
		// Get the current function
		Function oldFunction = this.currentFunction;
		
		// The new current function is this local function
		this.currentFunction = stmtFuncDecl.getDecl();
		
		// Create a new entry of local functions since it might have some
		this.functionsInnerFunctions.put(this.currentFunction, new ArrayList<Function>());
		
		// Visit this local function
		stmtFuncDecl = (StmtFunDecl) super.visitStmtFunDecl(stmtFuncDecl);
		
		// Reset the old function
		this.currentFunction = oldFunction;
		
		// Return the stmtFunDecl
		return stmtFuncDecl;
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
