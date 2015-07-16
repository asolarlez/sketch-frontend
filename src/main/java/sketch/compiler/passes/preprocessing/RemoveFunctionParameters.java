package sketch.compiler.passes.preprocessing;

import java.util.*;

import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.FieldDecl;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.NameResolver;
import sketch.compiler.ast.core.Package;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.SymbolTable;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.ExprArrayInit;
import sketch.compiler.ast.core.exprs.ExprArrayRange;
import sketch.compiler.ast.core.exprs.ExprArrayRange.RangeLen;
import sketch.compiler.ast.core.exprs.ExprField;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.ExprLambda;
import sketch.compiler.ast.core.exprs.ExprUnary;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.exprs.regens.ExprRegen;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtBlock;
import sketch.compiler.ast.core.stmts.StmtEmpty;
import sketch.compiler.ast.core.stmts.StmtFunDecl;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.StructDef;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.ast.core.typs.TypeFunction;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.passes.annotations.CompilerPassDeps;
import sketch.compiler.passes.lowering.SymbolTableVisitor;
import sketch.compiler.passes.structure.CallGraph;
import sketch.util.Pair;
import sketch.util.exceptions.ExceptionAtNode;
import sketch.util.exceptions.TypeErrorException;

@CompilerPassDeps(runsBefore = {}, runsAfter = {})
public class RemoveFunctionParameters extends FEReplacer {
	final TempVarGen 			varGen;

	private Map<String, ExprLambda> 	localLambda = new HashMap<String, ExprLambda>();
	private Map<ExprVar, Expression> 	lambdaReplace = new HashMap<ExprVar, Expression>();
	private Map<String, List<ExprVar>> 	lambdaFunctionsNeededVariables = new HashMap<String, List<ExprVar>>();
	private Map<String, String> 		lambdaRenameMap = new HashMap<String, String>();

	Map<String, SymbolTable> 	tempSymtables = new HashMap<String, SymbolTable>();
	Map<String, NewFunInfo> 	extractedInnerFuns = new HashMap<String, NewFunInfo>();
	Map<String, List<String>> 	equivalences = new HashMap<String, List<String>>();
	Map<String, String> 		reverseEquiv = new HashMap<String, String>();
	Map<String, Function> 		funToReplace = new HashMap<String, Function>();
	Map<String, Function> 		newFunctions = new HashMap<String, Function>();
	Map<String, Package> 		pkges;
	Map<String, String> 		nfnMemoize = new HashMap<String, String>();
	Set<String> 				visited = new HashSet<String>();
	Stack<String> 				funsToVisit = new Stack<String>();
	InnerFunReplacer 			hoister = new InnerFunReplacer(false);
	int 						nfcnt = 0;

	private int tempFunctionsCount;

	public RemoveFunctionParameters(TempVarGen varGen) {
		this.varGen = varGen;
		this.tempFunctionsCount = 0;
	}

	/**
	 * Check the function parameters. If they are type function, then store it
	 * to be replace
	 * 
	 * @param fun
	 */
	private void checkFunParameters(Function fun) {
		// Loop through the parameters
		for (Parameter p : fun.getParams()) {
			// If the parameter is type fun
			if (p.getType() instanceof TypeFunction) {
				// Store the function to be replace later
				funToReplace.put(nres.getFunName(fun.getName()), fun);

				break;
			}
		}
	}

	String getPkgName(String fname) {
		int i = fname.indexOf("@");
		return fname.substring(i + 1);
	}

	String getNameSufix(String fname) {
		int i = fname.indexOf("@");
		return fname.substring(0, i >= 0 ? i : fname.length());
	}

	/**
	 * Creating a new function name from the function call
	 * 
	 * @param efc
	 * @param orig
	 * @return
	 */
	String newFunName(ExprFunCall efc, Function orig) {
		String name = newNameCore(efc, orig);

		String oldName = name;
		String newName = name;
		if (nfnMemoize.containsKey(oldName)) {
			return nfnMemoize.get(oldName);
		}
		while (nres.getFun(newName) != null) {
			newName = oldName + (++nfcnt);
		}
		nfnMemoize.put(oldName, newName);
		return newName;
	}

	private String newNameCore(ExprFunCall efc, Function orig) {
		String name = orig.getName();
		Iterator<Parameter> fp = orig.getParams().iterator();

		if (efc.getParams().size() != orig.getParams().size()) {
			// Give user the benefit of the doubt and assume the mismatch is
			// purely due to
			// implicit parameters.
			int diff = orig.getParams().size() - efc.getParams().size();
			if (diff < 0) {
				throw new TypeErrorException(
						"Incorrect number of parameters to function " + orig,
						efc);
			}
			for (int i = 0; i < diff; ++i) {
				if (!fp.hasNext()) {
					throw new TypeErrorException(
							"Incorrect number of parameters to function "
									+ orig,
							efc);
				}
				Parameter p = fp.next();
				if (!p.isImplicit()) {
					throw new TypeErrorException(
							"Incorrect number of parameters to function "
									+ orig,
							efc);
				}
			}
		}

		for (Expression actual : efc.getParams()) {
			Parameter p = fp.next();

			// If the actual parameter is a lambda expression
			if(actual.getClass() == ExprLambda.class) {
				// Add a tempt string to the end of the function name
				name += "_temp" + this.tempFunctionsCount;

				// Increment the count of temp function
				this.tempFunctionsCount++;
			}
			else if (p.getType() instanceof TypeFunction) {
				name += "_" + actual.toString();
			}
		}
		return name;
	}

	void addEquivalence(String old, String newName) {
		if (!equivalences.containsKey(old)) {
			equivalences.put(old, new ArrayList<String>());
		}
		equivalences.get(old).add(newName);
		reverseEquiv.put(newName, old);
	}

	ExprFunCall replaceCall(ExprFunCall efc, Function orig, String nfn) {
		List<Expression> params = new ArrayList<Expression>();
		Iterator<Parameter> fp = orig.getParams().iterator();
		if (orig.getParams().size() > efc.getParams().size()) {
			int dif = orig.getParams().size() - efc.getParams().size();
			for (int i = 0; i < dif; ++i) {
				fp.next();
			}
		}

		for (Expression actual : efc.getParams()) {
			Parameter p = fp.next();
			if (!(p.getType() instanceof TypeFunction)) {
				params.add(doExpression(actual));
			}
		}
		return new ExprFunCall(efc, nfn, params);
	}

	/**
	 * Create a new call
	 * 
	 * @param efc
	 * @param orig
	 * @param nfn
	 * @return
	 */
	Function createCall(final ExprFunCall efc, Function orig, final String nfn) {
		List<Expression> existingArgs = efc.getParams();
		List<Parameter> params = orig.getParams();
		Iterator<Parameter> it = params.iterator();
		int starti = 0;
		if (params.size() != existingArgs.size()) {
			while (starti < params.size()) {
				if (!params.get(starti).isImplicit()) {
					break;
				}
				++starti;
				it.next();
			}
		}

		if ((params.size() - starti) != existingArgs.size()) {
			throw new ExceptionAtNode("Wrong number of parameters", efc);
		}

		final String cpkg = nres.curPkg().getName();
		for (Expression actual : efc.getParams()) { 
			Parameter formal = it.next();
			if (formal.getType() instanceof TypeFunction) {
				// If the function is found, good! Otherwise, have to create function
				Function fun = nres.getFun(actual.toString());

				// If the function is null AND the actual is a lambda expression
				if (fun == null && actual instanceof ExprLambda) {
					// Create a new function similar to the original, that is, where the 
					// function is being called
					fun = this.createTempFunction(orig, nfn, cpkg, orig.getParams());

					// If this function already has some variables that it needs
					if(this.lambdaFunctionsNeededVariables.containsKey(fun.getName())) {
						// Get the current formal parameters
						List<ExprVar> formalParameters = 
								this.lambdaFunctionsNeededVariables.get(fun.getName());
						
						// Append the ones that it needs
						formalParameters.addAll(((ExprLambda) actual).getVariablesInScopeInExpression());						
						
						// Add all the needed parameters
						this.lambdaFunctionsNeededVariables.put(fun.getName(), formalParameters);
					}
					else {
						// Get a list of the variables needed in this new function
						this.lambdaFunctionsNeededVariables.put(fun.getName(), 
								((ExprLambda) actual).getVariablesInScopeInExpression());												
					}


				}
				// If the actual parameters is a variable
				else if(actual instanceof ExprVar) {
					// Cast it as an expr var
					ExprVar lambdaVariable = (ExprVar) actual;
					
					// If there is a local lambda function that was defined previously
					// with the same name
					if(localLambda.containsKey(lambdaVariable.getName())) {
						// Create a special function
						fun = this.createTempFunction(orig, nfn, cpkg, orig.getParams());

						// Get the lambda expression
						ExprLambda lambda = localLambda.get(lambdaVariable.getName());

						// Visit this lambda in case the expression uses other functions or lambdas 
						lambda = (ExprLambda) this.doExpression(lambda);
												
						// Get a list of the variables needed in this new function
						this.lambdaFunctionsNeededVariables.put(fun.getName(), 
								((ExprLambda) lambda).getVariablesInScopeInExpression());		
					}
				}
				else if(fun == null) {
					throw new ExceptionAtNode("Function " + actual + " does not exist", efc);
				}

				Type t = fun.getReturnType();
				List<String> tps = fun.getTypeParams();
				for (String ct : tps) {
					if (ct.equals(t.toString())) {
						throw new ExceptionAtNode(
								"Functions with generic return types cannot be passed as function parameters: "
										+ fun,
								efc);
					}
				}
			}

		}

		FEReplacer renamer = new FunctionParamRenamer(nfn, efc, cpkg);

		return (Function) orig.accept(renamer);
	}

	/**
	 * Create a temporary function when there is a lambda expression. The new
	 * function will be similar to the one where the lambda expression function
	 * is being called.
	 * 
	 * @param origin
	 * @param name
	 * @param currentPackage
	 * @param parameters
	 * @return
	 */
	private Function createTempFunction(Function origin, String name, String currentPackage, List<Parameter> parameters) {
		return origin.creator()
				.returnType(origin.getReturnType())
				.params(parameters)
				.name(name)
				.pkg(currentPackage)
				.create();
	}


	public Object visitProgram(Program p) {
		// p = (Program) p.accept(new SpecializeInnerFunctions());
		// p.debugDump("After specializing inners");
		// p = (Program) p.accept(new InnerFunReplacer());

		nres = new NameResolver(p);
		// Set the name resolver in hoister
		hoister.setNres(nres);
		// Register the global variables of the program in the hoister
		hoister.registerGlobals(p);

		// Loop each package
		for (Package pkg : p.getPackages()) {
			// Set the package in the name resolver
			nres.setPackage(pkg);
			Set<String> nameChk = new HashSet<String>();

			// Loop through each function
			for (Function fun : pkg.getFuncs()) {
				// Check the function parameters
				checkFunParameters(fun);
				
				// Check if name check has the same name as the current function
				if (nameChk.contains(fun.getName())) {
					throw new ExceptionAtNode("Duplicated Name in Package", fun);
				}

				// Add the function name to name check
				nameChk.add(fun.getName());

				// If the function is a harness
				if (fun.isSketchHarness()) {
					// Add the harness to the functions to visit
					funsToVisit.add(nres.getFunName(fun.getName()));
				}

				// If the function implements some other function
				if (fun.getSpecification() != null) {

					String spec = nres.getFunName(fun.getSpecification());
					if (spec == null)
						throw new ExceptionAtNode(
								"Function " + fun.getSpecification()
										+ ", the spec of " + fun.getName()
										+ " is can not be found. did you put the wrong name?",
								fun);

					funsToVisit.add(spec);
					funsToVisit.add(nres.getFunName(fun.getName()));
				}
			}
		}
		// We visited all packages and all functions

		Map<String, List<Function>> nflistMap = new HashMap<String, List<Function>>();
		pkges = new HashMap<String, Package>();

		// Loop each package
		for (Package pkg : p.getPackages()) {
			nflistMap.put(pkg.getName(), new ArrayList<Function>());
			pkges.put(pkg.getName(), pkg);
		}

		// While we have function to visit.
		while (!funsToVisit.isEmpty()) {
			// Get the latest function
			String fname = funsToVisit.pop();
			String pkgName = getPkgName(fname);

			Function next = nres.getFun(fname);

			// If visited function does not contains the current function
			if (!visited.contains(fname)) {
				String chkname = fname;

				if (this.reverseEquiv.containsKey(fname)) {
					chkname = reverseEquiv.get(fname);
				}

				if (tempSymtables.containsKey(chkname)) {
					hoister.setSymtab(tempSymtables.get(chkname));
				} else {
					// Setting the symbol table of the hoister
					hoister.setSymtab(tempSymtables.get("pkg:" + next.getPkg()));
				}

				next = (Function) next.accept(hoister);
				Function nf = (Function) next.accept(this);
				visited.add(fname);
				nflistMap.get(pkgName).add(nf);
			}
		}
		List<Package> newPkges = new ArrayList<Package>();
		for (Package pkg : p.getPackages()) {
			newPkges.add(new Package(pkg, pkg.getName(), pkg.getStructs(),
					pkg.getVars(), nflistMap.get(pkg.getName())));
		}
		// This is where the new program is created
		Program np = p.creator().streams(newPkges).create();

		Program aftertc = (Program) np.accept(new ThreadClosure());
		
		Program afterLambdaClosure = (Program) aftertc.accept(new LambdaThread());

		return afterLambdaClosure.accept(new FixPolymorphism());

	}

	/**
	 * Visit a package returns null
	 */
	public Object visitPackage(Package spec) {
		return null;
	}

	/**
	 * Just checking that the type of the variables are not fun
	 */
	public Object visitStmtVarDecl(StmtVarDecl svd) {
		for (int i = 0; i < svd.getNumVars(); ++i) {
			if (svd.getType(i) instanceof TypeFunction && svd.getInit(0) instanceof ExprLambda) {
				// Map the function call to the lambda expression
				this.localLambda.put(svd.getName(0), (ExprLambda) svd.getInit(0));
				
				// Map the new name with the old
				lambdaRenameMap.put(svd.getName(0), svd.getName(0) + tempFunctionsCount);
				
				// Increment the number of temp functions
				tempFunctionsCount++;

				return null;
				// TODO MIGUEL be careful since now we are allowing fun as type
				// throw new ExceptionAtNode(
				// "You can not declare a variable with fun type.", svd);
			}
			
			// By this point, the variable is not of type fun, so if the assignment is a lambda, 
			// then there is an error
			if(svd.getInit(0) instanceof ExprLambda) {
				throw new TypeErrorException("You are assigning a lambda expression to an invalid type: " + svd, svd);
			}
			
		}

		Object o = super.visitStmtVarDecl(svd);

		return o;
	}
	
	public Object visitExprVar(ExprVar ev) {
		// If the is a local lambda expression, then there are some
		// variables that will be mapped to actual values
       if(this.lambdaReplace.containsKey(ev)) {
			// Get the replacement value
			Expression hold = this.lambdaReplace.get(ev);

			// Check if the replaced value is the same object as the original.
			if (ev.equals(hold)) {
				// If that is the case, it means we are at a leaf replacement
				// so there is nothing else to replace.
				return hold;
			}

			// This replaced value can be mapped to other variables,
			// so keep checking for replacements
			while (this.lambdaReplace.containsKey(hold)) {
				// visit the expression
				hold = this.doExpression(hold);
			}

			// Return the actual parameter
			return hold;
        }            
        else {
            return ev;
        }
    }
	
	public Object visitStmtAssign(StmtAssign stmt) {
		// Get the left side of the statement assignment
		Expression left = stmt.getLHS();
		
		if(left instanceof ExprVar) {
			// Check if there is a statement assignment to a lambda expression previously defined
			if(this.localLambda.containsKey(((ExprVar) left).getName())) {
				throw new ExceptionAtNode("Shadowing of lambda expressions is not allowed: " + stmt, stmt);
			}			
		}
		
		
		return super.visitStmtAssign(stmt);
	}

	public Object visitExprFunCall(ExprFunCall efc) {
		if (efc.getName().equals("minimize")) {
			return super.visitExprFunCall(efc);
		}

		String name = nres.getFunName(efc.getName());
		if (name == null) {
			// If there is a local lambda expression
			if (this.localLambda.containsKey(efc.getName())) {
				// Return that inlined version of the lambda expression
				 return this.inlineLocalLambda(efc, this.localLambda.get(efc.getName()));
			}

			throw new ExceptionAtNode("Function " + efc.getName()
					+ " either does not exist, or is ambiguous.", efc);
		}

		// If this function call is one that we need to replace. Most likely a fun
		if (funToReplace.containsKey(name)) {
			// Get the function to replace
			Function orig = funToReplace.get(name);
			// Get the new function name
			String nfn = newFunName(efc, orig);

			// If new function already has this new function
			if (newFunctions.containsKey(nfn)) {
				return replaceCall(efc, orig, nfn);
			} else {
				// Create a new call of this function
				Function newFun = createCall(efc, orig, getNameSufix(nfn));
				nres.registerFun(newFun);
				String newName = nres.getFunName(newFun.getName());
				addEquivalence(name, newName);
				newFunctions.put(newName, newFun);
				funsToVisit.push(newName);
				return replaceCall(efc, orig, nfn);
			}
		} else {
			if (!visited.contains(name)) {
				String pkgName = getPkgName(name);
				if (pkges != null && pkges.get(pkgName) == null) {
					throw new ExceptionAtNode(
							"Package named " + pkgName + " does not exist.",
							efc);
				}
				if (nres.getFun(name) == null) {
					throw new ExceptionAtNode(
							"Function " + efc.getName()
									+ " either does not exist, or is ambiguous.",
							efc);
				}
				funsToVisit.push(name);
			}
			return super.visitExprFunCall(efc);
		}
	}

	/**
	 * Inline a previously defined lambda expression.
	 * 
	 * @param functionCall
	 * @param exprLambda
	 * @return
	 */
	private Object inlineLocalLambda(ExprFunCall functionCall, ExprLambda exprLambda) {
		// If the number of function call parameters does not match the length of
		// formal parameters of the lambda expression
		if(exprLambda.getParameters().size() != functionCall.getParams().size()) {
			throw new ExceptionAtNode("The number of lambda parameters does not match "
					+ "the number of parameters in the function call: " 
					+ exprLambda.getParameters() + " - " + functionCall.getParams(), functionCall);
		}

		// Replacements should be local, so save the current map
		Map<ExprVar, Expression> oldLambdaReplace = this.lambdaReplace;
		
		// create a new map
		this.lambdaReplace = new HashMap<ExprVar, Expression>();

		// Loop through the formal parameters of the lambda and the actual parameters
		// of the call mapping them.
		for (int i = 0; i < exprLambda.getParameters().size(); i++) {
			this.lambdaReplace.put(exprLambda.getParameters().get(i), functionCall.getParams().get(i));
		}
		
		// Visit the expression in case there needs to be some replacement before getting
		// the previous replacement map
		Expression newExpression = this.doExpression(exprLambda.getExpression());
		
		// Restore the replacement map
		this.lambdaReplace = oldLambdaReplace;

		// Check if there are any replacements left
		newExpression = this.doExpression(newExpression);

		// Return a new expression where all the variables are replaced with
		// actual parameters
		return newExpression;
	}

	private static final class FunctionParamRenamer extends FEReplacer {
        private final String nfn;
        private final ExprFunCall efc;
        private final String cpkg;
        private final Map<String, String> rmap = new HashMap<String, String>();
		private final Map<String, ExprLambda> lambdaMap = new HashMap<String, ExprLambda>();
		private final Map<ExprVar, Expression> lambdaReplace = new HashMap<ExprVar, Expression>();

        private FunctionParamRenamer(String nfn, ExprFunCall efc, String cpkg)
        {
            this.nfn = nfn;
            this.efc = efc;
            this.cpkg = cpkg;
        }

        public Object visitStmtFunDecl(StmtFunDecl sfd) {
            Function f = sfd.getDecl();
            Statement s = (Statement) f.getBody().accept(this);

            return new StmtFunDecl(sfd, f.creator().body(s).create());
        }

        public Object visitFunction(Function func) {

            List<Parameter> newParam = new ArrayList<Parameter>();

            boolean samePars = true;

            Iterator<Parameter> fp = func.getParams().iterator();
            if (func.getParams().size() > this.efc.getParams().size()) {
                int dif = func.getParams().size() - this.efc.getParams().size();
                for (int i = 0; i < dif; ++i) {
                    Parameter par = fp.next();
                    Parameter newPar = (Parameter) par.accept(this);
                    if (par != newPar)
                        samePars = false;
                    newParam.add(newPar);
                }
            }
            
            for (Expression actual : this.efc.getParams()) {
				Parameter par = fp.next();
				Parameter newPar = (Parameter) par.accept(this);
				if (!(par.getType() instanceof TypeFunction)) {
					if (par != newPar)
						samePars = false;
					newParam.add(newPar);
				}
				// If actual is a lambda expression
				else if (actual instanceof ExprLambda) {
					samePars = false;

					// Map the parameter with the lambda expression
					this.lambdaMap.put(par.getName(), (ExprLambda) actual);
            	}
            	else {
        			samePars = false;
        			this.rmap.put(par.getName(), actual.toString());           	
            	}
            	
            }

            Type rtype = (Type) func.getReturnType().accept(this);

            if (func.getBody() == null) {
                assert func.isUninterp() : "Only uninterpreted functions are allowed to have null bodies.";
                if (samePars && rtype == func.getReturnType())
                    return func;
                return func.creator().returnType(rtype).pkg(this.cpkg).params(newParam).create();
            }
            Statement newBody = (Statement) func.getBody().accept(this);
            if (newBody == null)
                newBody = new StmtEmpty(func);
            if (newBody == func.getBody() && samePars && rtype == func.getReturnType())
                return func;
            return func.creator().returnType(rtype).params(newParam).body(newBody).name(
                    this.nfn).pkg(this.cpkg).create();
        }

        public Object visitExprFunCall(ExprFunCall efc) {
            boolean hasChanged = false;
            List<Expression> newParams = new ArrayList<Expression>();
            for (Expression param : efc.getParams()) {
                Expression newParam = doExpression(param);
                newParams.add(newParam);
                if (param != newParam)
                    hasChanged = true;
            }
            if (this.rmap.containsKey(efc.getName())) { 
                return new ExprFunCall(efc, this.rmap.get(efc.getName()), newParams);
            } else {
				// If the lambda map contains a function call to a lambda expression
				if (this.lambdaMap.containsKey(efc.getName())) {
					// Get the lambda expression
					ExprLambda lambda = this.lambdaMap.get(efc.getName());
					
					// If the number of function call parameters does not match the length of
					// formal parameters of the lambda expression
					if(lambda.getParameters().size() != efc.getParams().size()) {
						throw new ExceptionAtNode("The number of lambda parameters does not match "
								+ "the number of parameters in the function call: " 
								+ lambda.getParameters() + " - " + efc.getParams(), lambda);
					}

					// Loop through the formal parameters of the lambda and the actual parameters
					// of the call mapping them.
					for (int i = 0; i < lambda.getParameters().size(); i++) {
						this.lambdaReplace.put(lambda.getParameters().get(i), efc.getParams().get(i));
					}

					// Return a new expression where all the variables are replaced with
					// actual parameters
					return this.doExpression(lambda.getExpression());
					
				} else if (hasChanged) {
                    return new ExprFunCall(efc, efc.getName(), newParams);
                } else {
                    return efc;
                }
            }
        }

        public Object visitExprVar(ExprVar ev) {
            if (this.rmap.containsKey(ev.getName())) {
                return new ExprVar(ev, this.rmap.get(ev.getName()));
            }
            // If three is an actual parameter that corresponds to a lambda variable
            else if(this.lambdaReplace.containsKey(ev)) {
            	// Return the actual parameter
            	return this.lambdaReplace.get(ev);          	
            }            
            else {
                return ev;
            }
        }
    }

    /**
     * this FEReplacer does a complex job: 1. flatten all functions so that there is no
     * inner functions 2. all inner functions are hoisted out, so we need to pass
     * parameters in the scope of their containing functions, and care must be taken to
     * add "ref" if the modified vars 3. all parameters that are "fun" are now removed, by
     * specializing the callee. Example: <code>
     *   void twice(fun f) {
     *     f(); f();
     *   }
     *   harness void main() {
     *     int x = 0;
     *     void addone() {
     *       x++;
     *     }
     *     twice(addone);
     *     assert x == 2;
     *   }
     *   =>
     *   void addone1(ref int x) {
     *     x++;
     *   }
     *   void twice_addone1(ref int x) {
     *     addone1(x); addone1(x);
     *   }
     *   harness void main() {
     *     int x = 0;
     *     twice_addone1(x);
     *     assert x == 2;
     *   }
     * </code>
     * 
     * @author asolar, tim
     */
    static final class ParamInfo {
        final Type pt;
        // whether this variable has been changed
        // should be ORed when merging
        boolean changed;

        // the variables that this param depends on
        // currently capture the type relation
        // example: int [x] y;
        // then dependence of y contains x
        final TreeSet<String> dependence;

        public ParamInfo(Type pt, boolean changed, TreeSet<String> dependence) {
            this.pt = pt;
            this.changed = changed;
            this.dependence = dependence;
        }

        @Override
        public ParamInfo clone() {
            return new ParamInfo(this.pt, this.changed,
                    (TreeSet<String>) this.dependence.clone());
        }

        @Override
        public String toString() {
            return (this.changed ? "@" : "") + this.pt.toString();
        }
    }

    /**
     * Information about functions created as a result of hoisting out an inner function.
     * 
     * @author asolar
     */
    class NewFunInfo {
        public final String funName;
        public final String containingFunction;

        public final HashMap<String, ParamInfo> paramsToAdd;

        public HashMap<String, ParamInfo> cloneParamsToAdd() {
            HashMap<String, ParamInfo> c = new HashMap<String, ParamInfo>();
            for (Map.Entry<String, ParamInfo> e : paramsToAdd.entrySet()) {
                c.put(e.getKey(), e.getValue().clone());
            }
            return c;
        }

        NewFunInfo(String funName, String containingFunction) {
            this.funName = funName;
            this.containingFunction = containingFunction;
            paramsToAdd = new HashMap<String, ParamInfo>();
        }

        @Override
        public String toString() {
            return paramsToAdd.toString();
        }
    }

    /**
     * This is the very last step: after all inner functions have been hoisted out and
     * function params removed, we need to pass around variables that are used in inner
     * functions. "Thread" the closures.
     * 
     * @author asolar
     */
    class ThreadClosure extends FEReplacer {
        // funName => (varName => varInfo)
        Map<String, Map<String, ParamInfo>> funsToVisit =
                new HashMap<String, Map<String, ParamInfo>>();
        Map<String, List<Parameter>> addedParams = new HashMap<String, List<Parameter>>();

        Map<String, ParamInfo> mergePI(Map<String, ParamInfo> lhs,
                Map<String, ParamInfo> rhs)
        {
            for (Map.Entry<String, ParamInfo> e : rhs.entrySet()) {
                String var = e.getKey();
                ParamInfo info = e.getValue();
                ParamInfo merger = lhs.get(var);
                if (merger == null) {
                    lhs.put(var, info.clone());
                } else {
                    assert info.pt.equals(merger.pt);
                    merger.changed |= info.changed;
                    merger.dependence.addAll(info.dependence);
                }
            }
            return lhs;
        }

        public Object visitProgram(Program prog){
            CallGraph cg = new CallGraph(prog);
			nres = new NameResolver(prog);
            for(Map.Entry<String, NewFunInfo> eif : extractedInnerFuns.entrySet() ){ 
                String key = eif.getKey();
                NewFunInfo nfi = eif.getValue();
                Set<String> visited = new HashSet<String>();
                Stack<String> toVisit = new Stack<String>(); 
				if (equivalences.containsKey(key)) { 
                    for(String fn : equivalences.get(key)){
                        toVisit.push(fn);
                        if (funsToVisit.containsKey(fn)) {
                            funsToVisit.put(fn,
                                    mergePI(nfi.cloneParamsToAdd(), funsToVisit.get(fn)));
                        } else {
                            funsToVisit.put(fn, nfi.cloneParamsToAdd());
                        }
                    }
                }else{
                    toVisit.push(key);
					if (funsToVisit.containsKey(key)) { 
                        funsToVisit.put(key,
                                mergePI(nfi.cloneParamsToAdd(), funsToVisit.get(key)));
                    } else {
						funsToVisit.put(key, nfi.cloneParamsToAdd()); 
                    }
                }
				while (!toVisit.isEmpty()) { 
                    String cur = toVisit.pop();
					if (visited.contains(cur)) { 
                        continue;
                    }
                    visited.add(cur);
                    Set<Function> callers = cg.callersTo(nres.getFun(cur)); 
					for (Function caller : callers) { 
                        String callerName = nres.getFunName(caller);
                        String callerOriName = callerName;
						if (reverseEquiv.containsKey(callerName)) { 
							callerOriName = reverseEquiv.get(callerName); 
						}
                        if (!callerName.equals(nfi.containingFunction)) { 
                            toVisit.push(callerName); 
                            if (funsToVisit.containsKey(callerName)) { 
                                // funsToVisit.get(callerName).addAll(nfi.paramsToAdd);
                                // should merge correctly
                                Map<String, ParamInfo> c =
                                        funsToVisit.get(callerName);
                                for (Map.Entry<String, ParamInfo> e : nfi.paramsToAdd.entrySet())
                                {
                                    String var = e.getKey();
                                    ParamInfo info = e.getValue();
                                    ParamInfo merger = c.get(var);
                                    if (merger == null) {
                                        c.put(var, info.clone());
                                    } else {
                                        assert info.pt.equals(merger.pt);
                                        merger.changed |= info.changed;
                                        merger.dependence.addAll(info.dependence);
                                    }
                                }
                            } else {
                            	// Get the current function
								Function currentFunction = cg.getByName(cur);
								
								// Loop through the formal parameters of the current function
								for(Parameter parameter: currentFunction.getParams()) {
									// If the current parameter is a reference and the function that calls 
									// the current function needs some variables
									if(parameter.isParameterReference() && 
											lambdaFunctionsNeededVariables.containsKey(caller.getName())) {
										
										// Loop through each variable that is needed
										for(ExprVar variable : lambdaFunctionsNeededVariables.get(caller.getName())) {
											// Add a parameter to add to the caller 
											TreeSet<String> dependent = new TreeSet<String>();
											nfi.paramsToAdd.put(variable.getName(),
													new ParamInfo(parameter.getType(), true, dependent));											
										}


									}
								}

								funsToVisit.put(callerName,
										nfi.cloneParamsToAdd());
                            }
                        }
                    }
                }
                
            }
            return super.visitProgram(prog);
        }

        private List<Parameter> getAddedParams(String funName, boolean isGenerator) {
            List<Parameter> result = addedParams.get(funName);
            if (result == null) {
                Map<String, ParamInfo> params = funsToVisit.get(funName);
                HashMap<String, Integer> indeg = new HashMap<String, Integer>();
                HashMap<String, List<String>> outedge =
                        new HashMap<String, List<String>>();
                Queue<String> readyToPut = new ArrayDeque<String>(params.size());

                for (Map.Entry<String, ParamInfo> entry : params.entrySet()) {
                    String dependent = entry.getKey();
                    Set<String> dependence = entry.getValue().dependence;
                    indeg.put(dependent, dependence.size());
                    if (dependence.size() == 0) {
                        readyToPut.add(dependent);
                    }
                    for (String var : dependence) {
                        List<String> e = outedge.get(var);
                        if (e == null) {
                            e = new ArrayList<String>();
                            outedge.put(var, e);
                        }
                        e.add(dependent);
                    }
                }

                result = new ArrayList<Parameter>();
                while (!readyToPut.isEmpty()) {
                    String name = readyToPut.remove();
                    List<String> e = outedge.get(name);
                    if (e != null) {
                        for (String dependent : e) {
                            int deg = indeg.get(dependent);
                            if (deg == 1) {
                                readyToPut.add(dependent);
                            } else {
                                indeg.put(dependent, deg - 1);
                            }
                        }
                    }
                    ParamInfo info = params.get(name);
                    boolean makeRef = info.changed;
                    if (isGenerator) {
                        makeRef = makeRef || info.pt instanceof TypeArray;
                    }
                    result.add(new Parameter(null, info.pt, name, makeRef ? Parameter.REF
                            : Parameter.IN));

                }

                addedParams.put(funName, result);
            }
            return result;
        }

		public Object visitExprFunCall(ExprFunCall efc) {
            String name = nres.getFunName(efc.getName());
            Function f = nres.getFun(efc.getName());
            if (funsToVisit.containsKey(name)) {
                List<Parameter> addedParams = getAddedParams(name, f.isGenerator());
                if (addedParams.size() != 0) {
                    List<Expression> pl = new ArrayList<Expression>(efc.getParams());
                    for (Parameter p : addedParams) {
                        pl.add(new ExprVar(efc, p.getName()));
                        
                        // If the function that we are calling needs a variable
                        if(lambdaFunctionsNeededVariables.containsKey(efc.getName())) {
                        	// Loop through the variables needed
                        	for(ExprVar variable : lambdaFunctionsNeededVariables.get(efc.getName())) {
                        		// If a needed variable is the same as the current parameter that we just added
                        		if(variable.getName() == p.getName()) {
                        			// Delete it from the needed variables
                        			lambdaFunctionsNeededVariables.get(efc.getName()).remove(variable);
									
                        			// No need to look further since we added 1 variable
                        			break;
                        		}
                        	}
                        	
                        }
                        
                    }
                    efc = new ExprFunCall(efc, efc.getName(), pl);
                }
            }
            return super.visitExprFunCall(efc);
        }

        public Object visitFunction(Function fun) {
            String name = nres.getFunName(fun.getName());
            if (funsToVisit.containsKey(name)) {
                List<Parameter> pl = new ArrayList<Parameter>(fun.getParams());
                List<Parameter> newps = getAddedParams(name, fun.isGenerator());
                pl.addAll(newps);

                fun = fun.creator().params(pl).create();
            }
            return super.visitFunction(fun);
        }

    } // end of ThreadClosure

    class SpecializeInnerFunctions extends FEReplacer {


        Stack<Map<String, Pair<Function, Pair<List<Statement>, Set<String>>>>> postponed =
                new Stack<Map<String, Pair<Function, Pair<List<Statement>, Set<String>>>>>();

        Pair<Function, Pair<List<Statement>, Set<String>>> isPostponed(String name) {
            for (Map<String, Pair<Function, Pair<List<Statement>, Set<String>>>> m : postponed)
            {
                if (m.containsKey(name)) {
                    return m.get(name);
                }
            }
            return null;
        }

        @Override
        public Object visitStmtBlock(StmtBlock sb) {
            postponed.push(new HashMap<String, Pair<Function, Pair<List<Statement>, Set<String>>>>());
            Object o = super.visitStmtBlock(sb);
            postponed.pop();
            return o;
        }

        public Object visitExprFunCall(ExprFunCall efc) {
            String name = efc.getName();
            Pair<Function, Pair<List<Statement>, Set<String>>> pf = isPostponed(name);
            if (pf == null) {
                return super.visitExprFunCall(efc);
            }
            String nfn = newNameCore(efc, pf.getFirst());
            Set<String> nset = pf.getSecond().getSecond();
            if (nset.contains(nfn)) {

            } else {
                nset.add(nfn);
                FunctionParamRenamer renamer =
                        new FunctionParamRenamer(nfn, efc, nres.curPkg().getName());
                Function newf = (Function) pf.getFirst().accept(renamer);
                List<Statement> ls = pf.getSecond().getFirst();
                ls.add(new StmtFunDecl(efc, newf));
            }
            return replaceCall(efc, pf.getFirst(), nfn);

        }

        @Override
        public Object visitStmtFunDecl(StmtFunDecl sfd) {
            Function f = sfd.getDecl();
            boolean found = false;
            for (Parameter p : f.getParams()) {
                if (p.getType() instanceof TypeFunction) {
                    found = true;
                    break;
                }
            }
            if (found == true) {
                postponed.peek().put(f.getName(),
                        new Pair<Function, Pair<List<Statement>, Set<String>>>(f,
                                new Pair<List<Statement>, Set<String>>(newStatements,
                                        new HashSet<String>())));
                return null;
            } else {
                return super.visitStmtFunDecl(sfd);
            }
        }

    }

	/**
	 * There are some functions that can no longer be polymorphic after we
	 * specialize them based on their function parameters because the function
	 * parameters impose certain constraints on the outputs. This class will
	 * specialize the types for these functions by making them less generic.
	 */
    class FixPolymorphism extends SymbolTableVisitor {
		/**
		 * The tren type renamer keeps track of what generic types should be
		 * specialized and to what.
		 */
        TypeRenamer tren;

		/**
		 * namesset keeps track of the original type parameters.
		 */
        Set<String> namesset;

		/**
		 * elimset keeps track of which type parameters are no longer necessary
		 * because they are being eliminated.
		 */
        Set<String> elimset;

        Map<String, Function> doneFunctions = new HashMap<String, Function>();

        public FixPolymorphism() {
            super(null);
        }


        public Object visitFunction(Function f) {
            if (f.getTypeParams().isEmpty()) {
                return f;
            }
            if (doneFunctions.containsKey(f.getFullName())) {
                return doneFunctions.get(f.getFullName());
            }
            TypeRenamer oldtren = tren;
            Set<String> oldnamesset = namesset;
            Set<String> oldelimset = elimset;
            tren = new TypeRenamer();
            namesset = new HashSet<String>(f.getTypeParams());
            elimset = new HashSet<String>();

            Function fout = (Function) super.visitFunction(f);
            List<String> nl = new ArrayList<String>();
            for (String s : f.getTypeParams()) {
                if (elimset.contains(s)) {
                    continue;
                }
                nl.add(s);
            }
            Function rf = (Function) fout.creator().typeParams(nl).create().accept(tren);
            tren = oldtren;
            namesset = oldnamesset;
            elimset = oldelimset;
            doneFunctions.put(f.getFullName(), rf);
            return rf;
        }

        public Object visitExprFunCall(ExprFunCall efc) {
			// We may have to unify based on function parameters
            Function f = nres.getFun(efc.getName());

            if (doneFunctions.containsKey(f.getFullName())) {
                f = doneFunctions.get(f.getFullName());
            } else {
                f = (Function) f.accept(this);
            }

            Set<String> calleenamesset = new HashSet<String>(f.getTypeParams());
            if (f == null) {
                throw new ExceptionAtNode("Function not defined", efc);
            }

            Iterator<Parameter> pit = f.getParams().iterator();
            if (f.getParams().size() != efc.getParams().size()) {
                int dif = f.getParams().size() - efc.getParams().size();
                for (int i = 0; i < dif; ++i) {
                    Parameter p = pit.next();
                    if (!p.isImplicit()) {
                        throw new ExceptionAtNode("Bad param number", efc);
                    }
                }
            }
            for (Expression actual : efc.getParams()) {
                Type atype = getType(actual);
                Parameter p = pit.next();
                Type ptype = p.getType();
                while (atype instanceof TypeArray) {
                    atype = ((TypeArray) atype).getBase();
                    ptype = ((TypeArray) ptype).getBase();
                }
                Type ptypebase = ptype;
                while (ptypebase instanceof TypeArray) {
                    ptypebase = ((TypeArray) ptypebase).getBase();
                }
                String aname = atype.toString();
                if (namesset.contains(aname)) {
                    // This means the argument is polymorphic; if the
                    // callee is not polymorphic, it means that when we specialized the
                    // call we should have restricted the polymorphism, so we have to add
                    // now.
                    if (!calleenamesset.contains(ptypebase.toString())) {
						unifyGeneric(aname, ptype, f);
                    }
                }
            }
            return super.visitExprFunCall(efc);
        }

		public Object visitStmtVarDecl(StmtVarDecl svd) {

			for (int i = 0; i < svd.getNumVars(); ++i) {
				Type left = svd.getType(i);
				Expression eright = svd.getInit(i);
				if (eright != null) {
					Type right = getType(eright);
					checkAndUnify(left, right, svd);
				}
			}

			return super.visitStmtVarDecl(svd);
		}

		public Object visitStmtAssign(StmtAssign sa) {
			Type left = getType(sa.getLHS());
			Type right = getType(sa.getRHS());

			checkAndUnify(left, right, sa);

			return super.visitStmtAssign(sa);
		}

		void checkAndUnify(Type left, Type right, FENode ctxt) {
			while (left instanceof TypeArray) {
				left = ((TypeArray) left).getBase();
				right = ((TypeArray) right).getBase();
			}
			String lname = left.toString();
			if (namesset.contains(lname)) {
				unifyGeneric(lname, right, ctxt);
			}
		}

		void unifyGeneric(String genericName, Type newType, FENode ctxt) {
			elimset.add(genericName);
			if (newType instanceof TypeArray) {
				throw new ExceptionAtNode(
						"Generics can not resolve to an array type "
								+ genericName + "->" + newType,
						ctxt);
			}
			if (tren.tmap.containsKey(genericName)) {
				Type lcp = tren.tmap.get(genericName)
						.leastCommonPromotion(newType, nres);
				tren.tmap.put(genericName, lcp);
			} else {
				tren.tmap.put(genericName, newType);
			}
		}

    }

    /**
     * This is the last step of lambda expressions. Once a temp function was created out
     * of a lambda expression, we check to make sure that the new functions have all
     * the variables that they need. If lambda expressions need a variable, we pass those
     * variables to the temp variables.
     * 
     * @author Miguel Velez
     * @version 0.1
     */
	private class LambdaThread extends SymbolTableVisitor {

		private Map<String, List<Parameter>> tempFunctionsParametersNeeded = new HashMap<String, List<Parameter>>();

		public LambdaThread() {
			super(null);
		}

		public Object visitFunction(Function function) {
			// if there is a temp function that needs parameters
			if(this.tempFunctionsParametersNeeded.containsKey(function.getName())) {
				// New formal parameters
				List<Parameter> formalParamters = new ArrayList<Parameter>();
				
				// Get the current formal parameters
				formalParamters.addAll(function.getParams());
				
				// Loop through all the parameters that the function needs
				for(Parameter parameter : this.tempFunctionsParametersNeeded.get(function.getName())) {
					// add the parameter to the formal parameters
					formalParamters.add(parameter);
				}
				
				// Create a new function with the new parameters
				function = createTempFunction(function, function.getName(), function.getPkg(), formalParamters);
			}
			
			// Visit and return the function
			return (Function) super.visitFunction(function);
		}

		public Object visitExprFunCall(ExprFunCall exprFunctionCall) {
			// If this is a lambda call and it needs variables
			if(lambdaFunctionsNeededVariables.containsKey(exprFunctionCall.getName())) {
				// Get the variables that are needed in this call
				List<ExprVar> variablesNeeded = lambdaFunctionsNeededVariables.get(exprFunctionCall.getName());
				
				// Lists for holding the parameters
				List<Expression> actualParameters = new ArrayList<Expression>();
				List<Parameter> formalParameters = new ArrayList<Parameter>();
				
				// Add the current actual parameters
				actualParameters.addAll(exprFunctionCall.getParams());		 
						 			
				// Loop through the variables needed
				for(ExprVar variable : variablesNeeded) {
					// If this variable is already added
					if(actualParameters.contains(variable)) {
						// Skip this variable
						continue;
					}
					
					// Add the variable to the actual parameters
					actualParameters.add(variable);
					
					// Get the type of variable
					Type type = this.symtab.lookupVar(variable);
					
					// Add the parameter to the list of formal parameters of the function declaration
					formalParameters.add(new Parameter(variable, type, variable.getName()));
				}
				
				// Create a new function call with the new actual parameters
				exprFunctionCall = new ExprFunCall(exprFunctionCall, exprFunctionCall.getName(), actualParameters);
				
				// Add the formal parameter to be replaced in the function call
				this.tempFunctionsParametersNeeded.put(exprFunctionCall.getName(), formalParameters);
			}
			
			// Visit and return the function call
			return (ExprFunCall) super.visitExprFunCall(exprFunctionCall);
		}

	}

    /**
     * Hoist out inner functions, and extract the information about which inner function
     * used what variables defined in its containing function (the "closure"). This
     * visitor will define the value of <code> extractedInnerFuns </code>.
     * 
     * @author asolar, tim
     */
    class InnerFunReplacer extends SymbolTableVisitor {
        boolean 		isGenerator = false;
        final boolean 	recursive;
        boolean 		topLevel = true;
        int 			nfcnt = 0;
        FunReplMap 		frmap = new FunReplMap(null);
		Function curFun;
        
        InnerFunReplacer(boolean recursive) {
            super(null);
            this.recursive = recursive;
        }

        public void registerGlobals(Program p) {

            for (Package pkg : p.getPackages()) {

                SymbolTable st = new SymbolTable(null);
                symtab = st;
                for (FieldDecl fd : pkg.getVars()) {
                    fd.accept(this);
                }
                tempSymtables.put("pkg:" + pkg.getName(), st);
            }
            symtab = null;
        }

        /**
         * This is a leveled lookup table of the hoisted functions. If "f" is hoisted out
         * to "f2", then when we visit "f(x)" we need to replace it with "f2(x)". But
         * local variable in the deeper block might shadow "f", so FunReplMap needs to be
         * like a symtab.
         * 
         * @author asolar, tim
         */
        class FunReplMap {
            FunReplMap 			parent = null;
            Map<String, String> frmap = new HashMap<String, String>();

            FunReplMap(FunReplMap parent) {
                this.parent = parent;
            }

            String findRepl(String old) {
                if (frmap.containsKey(old))
                    return frmap.get(old);
                if (parent != null) {
                    return parent.findRepl(old);
                }
                return null;
            }

            void declRepl(String old, String notold) {
                frmap.put(old, notold);
            }

            @Override
            public String toString() {
                return frmap.toString() +
                        (parent == null ? "" : (" : " + parent.toString()));
            }
        }

        public Object visitFunction(Function fun) {
            boolean tmpIsGen = isGenerator;
            isGenerator = fun.isGenerator();
            FunReplMap tmp = frmap;
            frmap = new FunReplMap(tmp);
            Function tmpf = curFun;
            curFun = fun;
            for (Parameter p : fun.getParams()) {

                frmap.declRepl(p.getName(), null);

            }
            Object o = super.visitFunction(fun);
            curFun = tmpf;
            frmap = tmp;
            isGenerator = tmpIsGen;
            return o;
        }

        public Object visitStmtVarDecl(StmtVarDecl svd) {
            final InnerFunReplacer localIFR = this; 
            final TempVarGen varGen = RemoveFunctionParameters.this.varGen;
            FEReplacer remREGinDecl = new FEReplacer() {
                public Object visitTypeArray(TypeArray t){
                    Type nbase = (Type)t.getBase().accept(this);
                    Expression nlen = null;
                    if (t.getLength() != null) {
                        if(t.getLength() instanceof ExprRegen){
                            String nname = varGen.nextVar();
                            localIFR.addStatement((Statement) (new StmtVarDecl(
                                    t.getLength(), TypePrimitive.inttype, nname,
                                    t.getLength())).accept(localIFR));
                            nlen = new ExprVar(t.getLength(), nname);
                        }else{
                            nlen = t.getLength();
                        }
                    }
                    if(nbase == t.getBase() &&  t.getLength() == nlen ) return t;
                    return new TypeArray(nbase, nlen, t.getMaxlength());
                }
            };

            for (int i = 0; i < svd.getNumVars(); ++i) {
            	// If the statement is a lambda expression
            	if (svd.getType(i) instanceof TypeFunction && svd.getInit(0) instanceof ExprLambda) {
    				// Visit the lambda in case some of the values in the expression need to change
            		ExprLambda lambda = (ExprLambda) this.doExpression(svd.getInit(0));
    				
            		// Map the function call to the lambda expression
    				localLambda.put(svd.getName(0) + tempFunctionsCount,  lambda);
    				
					// Map the new name with the old
    				lambdaRenameMap.put(svd.getName(0), svd.getName(0) + tempFunctionsCount);
    				
					// Increment the number of temp functions
    				tempFunctionsCount++;
    				
					return null;

    			}
            	else {
            		frmap.declRepl(svd.getName(i), null);            		
            	}	
            }
            List<Type> newTypes = new ArrayList<Type>();
            boolean changed = false;

            for (int i = 0; i < svd.getNumVars(); i++) {

                if (symtab.hasVar(svd.getName(i))) {
                    throw new ExceptionAtNode("Shadowing of variables is not allowed.",
                            svd);
                }

                Type ot = svd.getType(i);
                Type t = (Type) ot.accept(remREGinDecl);
                if (ot != t) {
                    changed = true;
                }
                newTypes.add(t);
            }
            if (!changed) {
                return super.visitStmtVarDecl(svd);
            }
            return super.visitStmtVarDecl(new StmtVarDecl(svd, newTypes, svd.getNames(),
                    svd.getInits()));

        }

		public Object visitExprVar(ExprVar exprVar) {
			if (lambdaRenameMap.containsKey(exprVar.getName())) {
				return new ExprVar(exprVar, lambdaRenameMap.get(exprVar.getName()));
			}

			return super.visitExprVar(exprVar);
		}

        public Object visitExprFunCall(ExprFunCall efc) {
        	
            String oldName = efc.getName();
            String newName = frmap.findRepl(oldName);
            if (newName == null) {
				// Check if this is a call to a lambda that change names
				newName = lambdaRenameMap.get(oldName);
				if (newName == null) {
					newName = oldName;
				}
            }

			// If there is a local lambda expression
			if (localLambda.containsKey(newName)) {
				// Return that inlined version of the lambda expression
				return inlineLocalLambda(efc, localLambda.get(newName));
			}

            List<Expression> actuals = new ArrayList<Expression>();
            for (Expression actual : efc.getParams()) {
                    String nm = frmap.findRepl(actual.toString());
                    if (nm == null) {
                        actuals.add((Expression) actual.accept(this));
                    } else {
                        actuals.add(new ExprVar(actual, nm));
                    }
            }
            return new ExprFunCall(efc, newName, actuals);
        }

        public Object visitStmtBlock(StmtBlock stmt) {
            FunReplMap tmp = frmap;
            frmap = new FunReplMap(tmp);
            Object o = super.visitStmtBlock(stmt);
            frmap = tmp;
            return o;
        }

        public Object visitStmtFunDecl(StmtFunDecl sfd) {

            if (!topLevel && !recursive) {
                return super.visitStmtFunDecl(sfd);
            }

            String pkg = nres.curPkg().getName();
            String oldName = sfd.getDecl().getName();
            String newName = oldName + (++nfcnt);
            while (nres.getFun(newName) != null) {
                newName = oldName + (++nfcnt);
            }
            String te = frmap.findRepl(oldName);
            if (te != null) {
                throw new ExceptionAtNode("You can not redefine the inner function " +
                        oldName + " in the same scope", sfd);
            }
            frmap.declRepl(oldName, newName);
            Function f = sfd.getDecl();

            if (isGenerator && !f.isGenerator()) {
                throw new ExceptionAtNode(
                        "You can not define a non-generator function inside a generator",
                        sfd);
            }
            if (f.isSketchHarness()) {
                throw new ExceptionAtNode(
                        "You can not define a harness inside another function", sfd);
            }

            Function newFun = f.creator().name(newName).pkg(pkg).create();
            nres.registerFun(newFun);

            boolean oldTL = topLevel;
            topLevel = false;
                newFun = (Function) newFun.accept(this);
            topLevel = oldTL;
            if (recursive) {
                newFuncs.add(newFun);
            }
                // newFunctions.put(newName, newFun);
                // funsToVisit.push(newName);



            // NOTE xzl: overwrite the incorrect newFun with the correct newFun with
            // processed body. This is needed for later funInfo(fun) to work properly if
            // "fun" calls "newFun", because it inlines "newFun" to "fun" when
            // extracting the used set of "fun", and if "newFun" is in the old
            // form, newFun.body will refer to old unhoisted function names which nres
            // does not know about. Also notice that we cannot simply registerFun(newFun)
            // again.
            nres.reRegisterFun(newFun);

            checkFunParameters(newFun);

            tempSymtables.put(nres.getFunName(newFun), this.symtab);

            NewFunInfo nfi = funInfo(newFun);
            extractedInnerFuns.put(nfi.funName, nfi);
            return null;
        }


		NewFunInfo funInfo(Function f) {
            // get the new function info
            // i.e. the used variables that are in f's containing function
            // among the used variables, some are modified, we also track if a used var is
            // changed. curFun is the function that lexically encloses f.
            final String theNewFunName = nres.getFunName(f.getName());
            final NewFunInfo nfi =
                    new NewFunInfo(theNewFunName, nres.getFunName(curFun.getName()));
            SymbolTableVisitor stv = new SymbolTableVisitor(null) {
                boolean isAssignee = false;
                TreeSet<String> dependent = null;

                public Object visitStmtFunDecl(StmtFunDecl decl) {
                    // just ignore the inner function declaration
                    // because it will not affect the used/modified set
                    symtab.registerVar(decl.getDecl().getName(), TypeFunction.singleton,
                            decl, SymbolTable.KIND_LOCAL);
                    SymbolTable oldSymTab = symtab;
                    symtab = new SymbolTable(symtab);
                    for (Parameter p : decl.getDecl().getParams()) {
                        p.accept(this);
                    }

                    decl.getDecl().getBody().accept(this);
                    symtab = oldSymTab;
                    return decl;
                }

                public Object visitExprVar(ExprVar exp) {
                    final String name = exp.getName();

                    if (dependent != null) {
                        dependent.add(name);
                    }
                    Type t = symtab.lookupVarNocheck(exp);
					if (t == null) {
                        // if t is not null,
                        // it's local to stmtblock (thus should not be considered
                        // closure-passed variable that's defined outside the inner
                        // function)

                        // TODO xzl: Is this really sound? need to check
                        // If name is a hoisted function, consider it will be called, and
                        // inline it to get an over-estimation of the used/modified sets
                        // Note that this is still sound, even in the case "twice" is
                        // opaque:
                        // int x; void f() { void g(ref x) {...}; twice(g, x); }
                        // Although the ideal reasoning is that g modifies x, and we
                        // cannot know that, for twice(g, x) to modify x, twice's
                        // signature must have a "ref" for x, so just by looking at the
                        // call to "twice" we know that "x" is modified
                        /*
                         * Function hoistedFun = nres.getFun(name); if (hoistedFun !=
                         * null) { String fullName = nres.getFunName(name); if
                         * (fullName.equals(theNewFunName)) { // We are processing
                         * theNewFunName to get its NewFunInfo, // so we don't need to
                         * inline itself. It is not in the // symtab chain, so we must
                         * return early otherwise the // lookup will throw exception.
                         * return exp; } if (extractedInnerFuns.containsKey(fullName)) {
                         * // hoistedFun.accept(this); return exp; } return exp; }
                         */

                        Type pt = InnerFunReplacer.this.symtab.lookupVarNocheck(exp);
                        if (pt == null) {
                            return exp;
                        }

                        if (isInParam) {
                            throw new ExceptionAtNode(
                                    "You cannot use a captured variable in an array length expression: " +
                                            exp, exp);
                        }
                        int kind =
                                InnerFunReplacer.this.symtab.lookupKind(exp.getName(),
                                        exp);
                        if (kind == SymbolTable.KIND_GLOBAL) {
                            return exp;
                        }

                        TreeSet<String> oldDependent = dependent;
                        ParamInfo info = nfi.paramsToAdd.get(name);
                        if (info == null) {
                            dependent = new TreeSet<String>();
                            nfi.paramsToAdd.put(name, new ParamInfo(pt, isAssignee,
                                    dependent));
                        } else {
                            dependent = info.dependence;
                            if (isAssignee) {
                                info.changed = true;
                            }
                        }
                        // we should also visit the type of the variable.
                        boolean oldIsA = isAssignee;
                        isAssignee = false;
                        pt.accept(this);
                        isAssignee = oldIsA;
                        dependent = oldDependent;
					}
                    return exp;
                }

                public Object visitStructDef(StructDef ts) {
                    return ts;
                }

                public Object visitExprField(ExprField ef) {
                    // TODO xzl: field should not be considered assignee?
                    boolean oldIsA = isAssignee;
                    isAssignee = false;
                    ef.getLeft().accept(this);
                    isAssignee = oldIsA;
                    return ef;
                }

                public Object visitExprArrayRange(ExprArrayRange ear) {
                    ear.getBase().accept(this);
                    boolean oldIsA = isAssignee;
                    RangeLen rl = ear.getSelection();
                    isAssignee = false;
                    rl.start().accept(this);
                    if (rl.hasLen()) {
                        rl.getLenExpression().accept(this);
                    }
                    isAssignee = oldIsA;
                    return ear;
                }

                public Object visitExprUnary(ExprUnary exp) {
                    int op = exp.getOp();
                    if (op == ExprUnary.UNOP_POSTDEC || op == ExprUnary.UNOP_POSTINC ||
                            op == ExprUnary.UNOP_PREDEC || op == ExprUnary.UNOP_PREINC)
                    {
                        boolean oldIsA = isAssignee;
                        isAssignee = true;
                        exp.getExpr().accept(this);
                        isAssignee = oldIsA;
                    }
                    return exp;
                }

                boolean isInParam = false;

                public Object visitParameter(Parameter p) {
                    boolean op = isInParam;
                    isInParam = true;
                    Object o = super.visitParameter(p);
                    isInParam = op;
                    return o;
                }

                public Object visitExprArrayInit(ExprArrayInit init) {
                    boolean oldIsA = isAssignee;
                    isAssignee = false;
                    super.visitExprArrayInit(init);
                    isAssignee = oldIsA;
                    return init;
                }

                public Object visitStmtAssign(StmtAssign stmt) {
                    boolean oldIsA = isAssignee;
                    isAssignee = true;
                    stmt.getLHS().accept(this);
                    isAssignee = oldIsA;
                    stmt.getRHS().accept(this);
                    return stmt;
                }

                public Object visitExprFunCall(ExprFunCall efc) {
                    final String name = efc.getName();
                    Function fun = nres.getFun(name);
                    // NOTE the function passed to funInfo() is not in extractedInnerFuns
                    // yet, so it will not be inlined here, which is the correct behavior.
                    if (extractedInnerFuns.containsKey(nres.getFunName(name))) {
                        // FIXME xzl:
                        // this is raises a problem of lexical v.s. dynamic scope.
                        // <code>
                        // int x=0, y=0;
                        // void f() { x++; }
                        // void g() { int x=0; f(); y++; }
                        // </code>
                        // Then the current approach determines that g does not modify
                        // outer x, which is wrong under lexical scope.
                        //
                        // Also note that this is not the only place of inlining
                        // see below the "existingArgs" code.
                        //
                        // the old comment:
                        // This is necessary, it's essentially inlining every inner
                        // function
                        // if you have f(...) { g(...) { } ; h(...) { ... g() ... } }
                        // g will be inlined to h
                        // why is this reasonable? because you cannot have cyclic relation
                        // (there's strict order for inner function)
                        // you might ask, later will will propagate information along call
                        // edges
                        // why do we need this?
                        // because the propagate takes advantage of this to simplify
                        // the initial condition
                        // it always put extractedInner[fun].nfi as the start point
                        // rather than the joined result
                        // in the above example, when propagate h's information,
                        // it always start from extractedInner[h].nfi
                        // but not extractedInner[h].nfi JOIN extractedInner[g].nfi
                        // and this requires g() already inlined inside h()
                        // fun.accept(this);
                    }
                    // return super.visitExprFunCall(efc);
                    if (fun == null) {
                        Type t = this.symtab.lookupVar(efc.getName(), efc);
                        if (t == null || (!(t instanceof TypeFunction))) {
                            throw new ExceptionAtNode("Function " + efc.getName() +
                                    " has not been defined when used", efc);
                        }
                        // at this moment, we don't know about fun's signature,
                        // so we assume that all arguments might be changed for soundness
                        List<Expression> existingArgs = efc.getParams();
                        final boolean oldIsA = isAssignee;
                        for (Expression e : existingArgs) {
                            isAssignee = true;
                            // if (!(e instanceof ExprVar)) {
                                e.accept(this);
                            // }
                            isAssignee = oldIsA;
                        }
                        return efc;
                    }
                    List<Expression> existingArgs = efc.getParams();
                    List<Parameter> params = fun.getParams();
                    int starti = 0;
                    if (params.size() != existingArgs.size()) {
                        while (starti < params.size()) {
                            if (!params.get(starti).isImplicit()) {
                                break;
                            }
                            ++starti;
                        }
                    }

                    if ((params.size() - starti) != existingArgs.size()) {
                        throw new ExceptionAtNode("Wrong number of parameters", efc);
                    }
                    final boolean oldIsA = isAssignee;
                    for (int i = starti; i < params.size(); i++) {
                        Parameter p = params.get(i);
                        isAssignee = p.isParameterOutput();
                        // NOTE xzl: if this arg is a function, it will be inlined.
                        if (!(p.getType() instanceof TypeFunction)) {
                            existingArgs.get(i - starti).accept(this);
                        }
                        isAssignee = oldIsA;
                    }
                    return efc;
                }
            };
            stv.setNres(nres);
            f.accept(stv);
            return nfi;
        }

    } // end of InnerFunReplacer

}
