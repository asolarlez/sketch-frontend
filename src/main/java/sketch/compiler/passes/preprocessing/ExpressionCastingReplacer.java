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
 * <code>apply(x*x); => apply((,x) -> x*x);</code>
 * 
 * @author Miguel Velez
 * @version 0.1
 */
public class ExpressionCastingReplacer extends SymbolTableVisitor {

	private Function						currentFunction;
	private Stack<String> 					functionsToVisit;
	private Map<String, Package> 			packages;
	private Set<String> 					visitedFunctions;
	private Map<Function, List<Function>> 	functionsInnerFunctions;
	private Map<String, ExprLambda>			functionCastedFunctionsMap;
	private Map<String, String>				formalFunctionMap;
		
	/**
	 * Create a new expression casting replacer
	 */
	public ExpressionCastingReplacer() {
		super(null);

		this.currentFunction = null;
		this.functionsToVisit = new Stack<String>();
		this.packages = new HashMap<String, Package>();
		this.visitedFunctions = new HashSet<String>();
		this.functionsInnerFunctions = new HashMap<Function, List<Function>>();
		this.functionCastedFunctionsMap = new HashMap<String, ExprLambda>();
		this.formalFunctionMap = new HashMap<String, String>();
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
		
		// Loop through each package
		for (Package pkg : program.getPackages()) {
			for (Function function : pkg.getFuncs()) {
				if (!this.visitedFunctions.contains(function.getFullName())) {
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
		
		if (this.formalFunctionMap.containsKey(exprFunctionCall.getName())) {
			Random random = new Random();

			String lambdaName = this.formalFunctionMap.get(exprFunctionCall.getName());
			ExprLambda castedLambda = this.functionCastedFunctionsMap.get(lambdaName);
					
			int numberFormalParamerters = exprFunctionCall.getParams().size();
			
			List<ExprVar> variables = new ArrayList<ExprVar>();
			
			for(int i = 0; i < numberFormalParamerters; i++) {
				variables.add(new ExprVar(exprFunctionCall,
						"dummyVariable" + random.nextInt()));
			}
			
			castedLambda.setParameteres(variables);
		}

		// Get the function name from the list of function
		functionName = this.nres.getFunName(exprFunctionCall.getName());
		
		if(functionName == null) {
			return super.visitExprFunCall(exprFunctionCall);			
		}
		
		Function callee = this.nres.getFun(functionName);
		Iterator<Parameter> formalParameters = callee.getParams().iterator();
		Parameter formalParameter = null;
		
		List<Expression> newParams = new ArrayList<Expression>();
		
		int i = -1;

		// Loop through each actual parameter
		for (Expression actualParameter : exprFunctionCall.getParams()) {
			i++;

			formalParameter = formalParameters.next();
					
			// If the actual parameter is a variable
			if (!(formalParameter.getType() instanceof TypeFunction)) {
				newParams.add(this.doExpression(actualParameter));

				continue;
			}
			
			if(actualParameter instanceof ExprLambda) {
				newParams.add(this.doExpression(actualParameter));
				
				continue;
			}
			
			if (actualParameter instanceof ExprArrayInit) {
				newParams.add(this.doExpression(actualParameter));

				continue;
			}
			
			// If the actual parameter is a variable
			if(actualParameter instanceof ExprVar) {
				// Get the variable
				ExprVar variable = (ExprVar) actualParameter;
				
				// Check if there is a function with that name
				functionName = this.nres.getFunName(variable.getName());

				// Check if there is a function with that name
				List<Function> innerFunctions = this.functionsInnerFunctions.get(this.currentFunction);
				
				// Loop through the inner functions
				for(Function function : innerFunctions) {
					// If the variable is a reference to a local function
					if (function.getName().equals(variable.getName())) {
						// Set the function name and break
						functionName = function.getName();
						break;
					}
				}
				
				if (this.symtab.hasVar(variable.getName()) && this.symtab
						.lookupVar(variable) instanceof TypeFunction) {
					functionName = variable.getName();
				}

				// If the function was found
				if (functionName != null) {
					newParams.add(this.doExpression(actualParameter));
					continue;
				}
			}

			// We need to cast only where you do not use the parameters inside
			// the function
			ExprLambda castedLambda = new ExprLambda(actualParameter,new ArrayList<ExprVar>(), actualParameter);

			List<ExprVar> variablesUsedInExpression = castedLambda.getMissingFormalParameters();
			
			for(ExprVar variable : variablesUsedInExpression) {
				if(!this.symtab.hasVar(variable.getName())) {
					throw new ExceptionAtNode("You are passing an expression to a"
							+ " high-order function and you are using a variable "
									+ "that is not defined within scope: "
									+ variable
							, actualParameter);
				}
			}
			
			this.functionCastedFunctionsMap.put(exprFunctionCall.getName() + i, castedLambda);

			newParams.add(castedLambda);
			
        }
		
		return new ExprFunCall(exprFunctionCall, exprFunctionCall.getName(), newParams);		

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

	public Object visitFunction(Function function) {
		int i = -1;
		// Loop through the parameters
		for (Parameter p : function.getParams()) {
			i++;
			// If the parameter is type fun
			if (p.getType() instanceof TypeFunction) {
				if(this.functionCastedFunctionsMap.containsKey(function.getName()+ i)) {
					this.formalFunctionMap.put(p.getName(), function.getName()+ i);					
				}
			}
		}

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
