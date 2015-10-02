package sketch.compiler.passes.preprocessing;

import java.util.*;
import java.util.Map.Entry;

import sketch.compiler.ast.core.*;
import sketch.compiler.ast.core.SymbolTable.VarInfo;
import sketch.compiler.ast.core.Package;
import sketch.compiler.ast.core.exprs.*;
import sketch.compiler.ast.core.exprs.ExprArrayRange.RangeLen;
import sketch.compiler.ast.core.exprs.regens.ExprRegen;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssert;
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
import sketch.compiler.ast.core.typs.TypeStructRef;
import sketch.compiler.passes.annotations.CompilerPassDeps;
import sketch.compiler.passes.lowering.SymbolTableVisitor;
import sketch.compiler.passes.structure.CallGraph;
import sketch.compiler.stencilSK.VarReplacer;
import sketch.util.Pair;
import sketch.util.exceptions.ExceptionAtNode;
import sketch.util.exceptions.TypeErrorException;

@CompilerPassDeps(runsBefore = {}, runsAfter = {})
public class RemoveFunctionParameters extends FEReplacer {
    final TempVarGen varGen;

    private Map<String, ExprLambda> localLambda = new HashMap<String, ExprLambda>();
    private Map<ExprVar, Expression> lambdaReplace = new HashMap<ExprVar, Expression>();
	private Map<String, Map<ExprVar, ExprVar>> lambdaFunctionsNeededVariables = 
			new HashMap<String, Map<ExprVar, ExprVar>>();
    private Map<String, String> lambdaRenameMap = new HashMap<String, String>();
    private ExprLambda currentExprLambda = null;

    Map<String, SymbolTable> tempSymtables = new HashMap<String, SymbolTable>();
    Map<String, NewFunInfo> extractedInnerFuns = new HashMap<String, NewFunInfo>();
    Map<String, List<String>> equivalences = new HashMap<String, List<String>>();
    Map<String, String> reverseEquiv = new HashMap<String, String>();
    Map<String, Function> funToReplace = new HashMap<String, Function>();
    Map<String, Function> newFunctions = new HashMap<String, Function>();
    Map<String, Package> pkges;
    Map<String, String> nfnMemoize = new HashMap<String, String>();
    Set<String> visited = new HashSet<String>();
    Stack<String> funsToVisit = new Stack<String>();
    InnerFunReplacer hoister = new InnerFunReplacer(false);
    int nfcnt = 0;

    private int tempFunctionsCount;

    public RemoveFunctionParameters(TempVarGen varGen) {
        this.varGen = varGen;
        this.tempFunctionsCount = 0;
    }

    /**
     * Check the function parameters. If they are type function, then store it to be
     * replace
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
                        "Incorrect number of parameters to function " + orig, efc);
            }
            for (int i = 0; i < diff; ++i) {
                if (!fp.hasNext()) {
                    throw new TypeErrorException(
                            "Incorrect number of parameters to function " + orig, efc);
                }
                Parameter p = fp.next();
                if (!p.isImplicit()) {
                    throw new TypeErrorException(
                            "Incorrect number of parameters to function " + orig, efc);
                }
            }
        }

        for (Expression actual : efc.getParams()) {
            Parameter p = fp.next();

            // If the actual parameter is a lambda expression
            if (actual.getClass() == ExprLambda.class) {
                // Add a tempt string to the end of the function name
                name += "_temp" + this.tempFunctionsCount;

                // Increment the count of temp function
                this.tempFunctionsCount++;
            } else if (p.getType() instanceof TypeFunction) {
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
                    if (this.lambdaFunctionsNeededVariables.containsKey(fun.getName())) {
                        // Get the current formal parameters
                        Map<ExprVar, ExprVar> formalParameters =
                                this.lambdaFunctionsNeededVariables.get(fun.getName());

                        // Gret the list of missing parameters
                        Map<ExprVar, ExprVar> needed =
                                ((ExprLambda) actual).getMissingFormalParameters();

                        // Loop through each variable needed
                        for (Entry<ExprVar, ExprVar> entry : needed.entrySet()) {
							// If the variable is not included yet
							if (!formalParameters.containsKey(entry.getKey())) {
                                // Add it
								formalParameters.put(entry.getKey(), entry.getValue());
                            }
                        }

                        // Add all the needed parameters
                        this.lambdaFunctionsNeededVariables.put(fun.getName(),
                                formalParameters);
                    } else {
                        // Get a list of the variables needed in this new function
                        this.lambdaFunctionsNeededVariables.put(fun.getName(),
                                ((ExprLambda) actual).getMissingFormalParameters());
                    }

                }
                // If the actual parameters is a variable
                else if (actual instanceof ExprVar) {
                    // Cast it as an expr var
                    ExprVar lambdaVariable = (ExprVar) actual;

                    // If there is a local lambda function that was defined previously
                    // with the same name
                    if (localLambda.containsKey(lambdaVariable.getName())) {
                        // Create a special function
                        fun = this.createTempFunction(orig, nfn, cpkg, orig.getParams());

                        // Get the lambda expression
                        this.currentExprLambda =
                                localLambda.get(lambdaVariable.getName());

                        // Visit this lambda in case the expression uses other functions
                        // or lambdas
                        this.currentExprLambda =
                                (ExprLambda) this.doExpression(this.currentExprLambda);

                        // Get a list of the variables needed in this new function
                        this.lambdaFunctionsNeededVariables.put(
                                fun.getName(),
                                ((ExprLambda) this.currentExprLambda).getMissingFormalParameters());
                    }
                } else if (fun == null) {
					throw new ExceptionAtNode("Function " + actual + " does not exist", efc);
                }

                Type t = fun.getReturnType();
                List<String> tps = fun.getTypeParams();
                for (String ct : tps) {
                    if (ct.equals(t.toString())) {
                        throw new ExceptionAtNode(
                                "Functions with generic return types cannot be passed as function parameters: " +
                                        fun, efc);
                    }
                }
            }

        }

		FEReplacer renamer = new FunctionParamRenamer(nfn, efc, cpkg,
					this.lambdaFunctionsNeededVariables.get(nfn));

        // Set the current lambda expression to null
        this.currentExprLambda = null;

        return (Function) orig.accept(renamer);
    }

    /**
     * Create a temporary function when there is a lambda expression. The new function
     * will be similar to the one where the lambda expression function is being called.
     * 
     * @param origin
     * @param name
     * @param currentPackage
     * @param parameters
     * @return
     */
    private Function createTempFunction(Function origin, String name,
            String currentPackage, List<Parameter> parameters)
    {
        return origin.creator().returnType(origin.getReturnType()).params(parameters).name(
                name).pkg(currentPackage).create();
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
                        throw new ExceptionAtNode("Function " + fun.getSpecification() +
                                ", the spec of " + fun.getName() +
                                " is can not be found. did you put the wrong name?", fun);

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
			nres.setPackage(pkges.get(pkgName));
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
            newPkges.add(new Package(pkg, pkg.getName(), pkg.getStructs(), pkg.getVars(),
                    nflistMap.get(pkg.getName()), pkg.getSpAsserts()));
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
     * Visit an exprUnary an check if its part of a lambda expression. If it is, check
     * that it is not modifying a parameter of a lambda.
     */
    public Object visitExprUnary(ExprUnary exprUnary) {
        // If we are not analyzing a lambda expression
        if (this.currentExprLambda == null) {
            // Call the super class to visit the unary expression
            return super.visitExprUnary(exprUnary);
        }

        // Loop through the formal parameters of the lambda expression
        for (ExprVar formalParameter : this.currentExprLambda.getParameters()) {
            // If the unary expression has a formal parameter
            if (formalParameter.equals(exprUnary.getExpr())) {
                // Thrown exception since we cannot modify a formal parameter of
                // a lambda expression
                throw new ExceptionAtNode("You cannot have an unary expression of "
                        + "a formal parameter in a lambda", exprUnary);
            }
        }

        return super.visitExprUnary(exprUnary);

    }

    /**
     * Just checking that the type of the variables are not fun
     */
    public Object visitStmtVarDecl(StmtVarDecl svd) {
        for (int i = 0; i < svd.getNumVars(); ++i) {
            if (svd.getType(i) instanceof TypeFunction &&
                    svd.getInit(0) instanceof ExprLambda) {         	
            	// Map the function call to the lambda expression
                this.localLambda.put(svd.getName(0), (ExprLambda) svd.getInit(0));

                // Map the new name with the old
                lambdaRenameMap.put(svd.getName(0), svd.getName(0) + tempFunctionsCount);

                // Increment the number of temp functions
                tempFunctionsCount++;

                return null;
				// TODO be careful since now we are allowing fun as type

                // throw new ExceptionAtNode(
                // "You can not declare a variable with fun type.", svd);
            }

            // By this point, the variable is not of type fun, so if the assignment is a
            // lambda,
            // then there is an error
            if (svd.getInit(0) instanceof ExprLambda) {
                throw new TypeErrorException(
                        "You are assigning a lambda expression to an invalid type: " +
                                svd, svd);
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

        if (left instanceof ExprVar) {
            // Check if there is a statement assignment to a lambda expression previously
            // defined
            if (this.localLambda.containsKey(((ExprVar) left).getName())) {
                throw new ExceptionAtNode(
                        "Shadowing of lambda expressions is not allowed: " + stmt, stmt);
            }
        }

        return super.visitStmtAssign(stmt);
    }

    public Object visitExprLocalVariables(ExprLocalVariables exprLocalVariables) {
        // if (this.hoister.getSymbolTable().getParent() != null) {
        // // Set the symbol table to this one
        // exprLocalVariables.setSymbolTableInContext(this.hoister.getSymbolTable());
        // }

        return super.visitExprLocalVariables(exprLocalVariables);
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

            throw new ExceptionAtNode("Function " + efc.getName() +
                    " either does not exist, or is ambiguous.", efc);
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
                    throw new ExceptionAtNode("Package named " + pkgName +
                            " does not exist.", efc);
                }
                if (nres.getFun(name) == null) {
                    throw new ExceptionAtNode("Function " + efc.getName() +
                            " either does not exist, or is ambiguous.", efc);
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
        if (exprLambda.getParameters().size() != functionCall.getParams().size()) {
            throw new ExceptionAtNode("The number of lambda parameters does not match " +
                    "the number of parameters in the function call: " + exprLambda.getParameters()
                    + " - " + functionCall.getParams(), functionCall);
        }

        // Set the current lambda
        this.currentExprLambda = exprLambda;

        // Replacements should be local, so save the current map
        Map<ExprVar, Expression> oldLambdaReplace = this.lambdaReplace;

        // create a new map
        this.lambdaReplace = new HashMap<ExprVar, Expression>();

        // Loop through the formal parameters of the lambda and the actual parameters
        // of the call mapping them.
        for (int i = 0; i < exprLambda.getParameters().size(); i++) {
        	// Get the actual parameter
			Expression actualParameter = functionCall.getParams().get(i);
			
//			// if the parameter is a function call
//			if(actualParameter instanceof ExprFunCall) {
//				// We need to do some processing since the function should be called
//				// once, but the lambda will be inlined.
//				// If the new Statements list is null
//				if(this.newStatements == null) {
//					// Initialize it
//					newStatements = new ArrayList<Statement>();					
//				}
//
//				// Get the function being called
//				Function callee = this.nres.getFun(((ExprFunCall) actualParameter).getName());
//
//				// If the function was not found
//				if (callee == null) {
//					// Check the function rename map
//					String newName = hoister.frmap.findRepl(((ExprFunCall) actualParameter).getName());
//
//					// Search a function with the new name
//					callee = this.nres.getFun(newName);
//
//					// If the function was not found
//					if (callee == null) {
//						throw new ExceptionAtNode("Function " + functionCall.getName() +
//			                    " either does not exist, or is ambiguous.", functionCall);
//					}
//				}
//										
//				// Create a new function call
//				ExprFunCall newFunctionCall = new ExprFunCall(callee, callee.getName(),
//						((ExprFunCall) functionCall.getParams().get(i)).getParams());
//
//				// Add a variable declaration to the statements
//				Random random = new Random();
//				String randomVariableName = "v" + random.nextInt();
//				
//				this.addStatement(new StmtVarDecl(functionCall, callee.getReturnType(),
//						randomVariableName, newFunctionCall));
//
//				// The actual parameter now is just a variable
//				actualParameter = new ExprVar(functionCall, randomVariableName);
//				
////				// Register the variable in the symbol table
////				this.hoister.symtab.registerVar(randomVariableName, callee.getReturnType(), 
////						actualParameter, SymbolTable.KIND_LOCAL);
//			}

			// Add the actual parameter that will be replaced when inlining the lambda
            this.lambdaReplace.put(exprLambda.getParameters().get(i),actualParameter);
        }

        // Visit the expression in case there needs to be some replacement before getting
        // the previous replacement map
        Expression newExpression = this.doExpression(exprLambda.getExpression());

        // Restore the replacement map
        this.lambdaReplace = oldLambdaReplace;

        // // Check if there are any replacements left
        // newExpression = this.doExpression(newExpression);

        // Set the current lambda to null
        this.currentExprLambda = null;

        // Return a new expression where all the variables are replaced with
        // actual parameters
        return newExpression;
    }

    private static final class FunctionParamRenamer extends FEReplacer {
        private final String nfn;
        private final ExprFunCall efc;
        private final String cpkg;
        private final Map<String, String> rmap = new HashMap<String, String>();
        private final Map<String, ExprLambda> lambdaMap =
                new HashMap<String, ExprLambda>();
        private final Map<ExprVar, Expression> lambdaReplace =
                new HashMap<ExprVar, Expression>();
		private final Map<ExprVar, ExprVar> variablesNeeded;
		private boolean lambda;

		private FunctionParamRenamer(String nfn, ExprFunCall efc, String cpkg, Map<ExprVar, ExprVar> variablesNeeded)
        {
            this.nfn = nfn;
            this.efc = efc;
            this.cpkg = cpkg;
			this.variablesNeeded = variablesNeeded;
			this.lambda = false;
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
                } else {
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

                    // If the number of function call parameters does not match the length
                    // of
                    // formal parameters of the lambda expression
                    if (lambda.getParameters().size() != efc.getParams().size()) {
                        throw new ExceptionAtNode(
                                "The number of lambda parameters does not match " +
                                        "the number of parameters in the function call: " +
                                        lambda.getParameters() + " - " + efc.getParams(),
                                lambda);
                    }

                    // Loop through the formal parameters of the lambda and the actual
                    // parameters
                    // of the call mapping them.
                    for (int i = 0; i < lambda.getParameters().size(); i++) {
                        this.lambdaReplace.put(lambda.getParameters().get(i),
                                efc.getParams().get(i));
                    }

                    // Return a new expression where all the variables are replaced with
                    // actual parameters
					this.lambda = true;

					Expression newLambda = this.doExpression(lambda.getExpression());

					this.lambda = false;

					return newLambda;

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
				// If we are in a lambda and this variable is needed
			} else if (this.lambda && this.variablesNeeded.containsKey(ev)) {
				// Return the fresh variable that maps to this needed variable
				return this.variablesNeeded.get(ev);
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
        final String name;
        final String uniqueName;
        final Type paramType;
        // whether this variable has been changed
        // should be ORed when merging.
        // If it has been changed, then it will become a reference parameter.
        boolean changed;

        /**
         * Indicates that this is a parameter that is only being passed through
         * this function. it neither originated here nor is this its final
         * destination.
         */
        boolean passthrough;

        // the variables that this param depends on
        // currently capture the type relation
        // example: int [x] y;
        // then dependence of y contains x
        final TreeSet<String> dependence;

        public ParamInfo(Type pt, String name, String uniqueName,
                boolean changed,
                TreeSet<String> dependence) {
            this.paramType = pt;
            this.changed = changed;
            this.dependence = dependence;
            this.name = name;
            this.uniqueName = uniqueName;
        }

        boolean isPassthrough() {
            return passthrough;
        }

        ParamInfo notPassthrough() {
            passthrough = false;
            return this;
        }

        ParamInfo makePassthrough() {
            passthrough = true;
            return this;
        }

        public String uniqueName() {
            return uniqueName;
        }

        @Override
        public ParamInfo clone() {
            return new ParamInfo(this.paramType, this.name, this.uniqueName,
                    this.changed,
                    (TreeSet<String>) this.dependence.clone());
        }

        public ParamInfo clone(VarReplacer rep) {
            TreeSet<String> newdep = new TreeSet<String>();
            for (String s : this.dependence) {
                String t = rep.find(s).toString();
                if (t != null) {
                    newdep.add(t);
                } else {
                    newdep.add(s);
                }
            }

            return new ParamInfo((Type) this.paramType.accept(rep), this.name, this.uniqueName, this.changed, newdep);
        }

        @Override
        public String toString() {
            return (this.changed ? "@" : "") + this.paramType.toString() + "(" + uniqueName + ")[" + dependence + "]";
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
        public final Set<String> typeParamsToAdd;
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
            typeParamsToAdd = new TreeSet<String>();
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

        Set<String> enclosingAdded;

        Map<String, ParamInfo> mergePI(Map<String, ParamInfo> lhs,
                Map<String, ParamInfo> rhs)
        {
            for (Map.Entry<String, ParamInfo> e : rhs
                    .entrySet())
            {
                String var = e.getValue().name;
                ParamInfo calleeInfo = e.getValue();
                ParamInfo callerInfo = lhs.get(var);
                if (callerInfo == null) {
                    boolean renameInType = false;
                    Map<String, Expression> repl = renamesFromDeps(calleeInfo.dependence, lhs, rhs);
                    if (repl.size() > 0) {
                        VarReplacer vr = new VarReplacer(repl);
                        lhs.put(var, calleeInfo.clone(vr).makePassthrough());
                    } else {
                        lhs.put(var, calleeInfo.clone().makePassthrough());
                    }
                } else {
                    if (callerInfo.uniqueName().equals(calleeInfo.uniqueName())) {
                        assert calleeInfo.paramType.equals(callerInfo.paramType);
                        callerInfo.changed |= calleeInfo.changed;
                        callerInfo.dependence
                                .addAll(calleeInfo.dependence);
                    } else {
                        // Everyone who depents on me has
                        // already had their types fixed, so
                        // its ok.
                        Map<String, Expression> repl = renamesFromDeps(calleeInfo.dependence, lhs, rhs);
                        if (repl.size() > 0) {
                            lhs.put(e.getValue().uniqueName, calleeInfo.clone(new VarReplacer(repl)).makePassthrough());
                        } else {
                            lhs.put(e.getValue().uniqueName, calleeInfo.clone().makePassthrough());
                        }
                    }
                }
            }
            return lhs;
        }

        Map<String, Expression> renamesFromDeps(TreeSet<String> dependence,
                Map<String, ParamInfo> pinfosOfCaller,
                Map<String, ParamInfo> pinfosOfCallee) {
            Map<String, Expression> repl = new HashMap<String, Expression>();

            for (String dep : dependence) {
                ParamInfo localDep = pinfosOfCaller.get(dep);
                ParamInfo passthroughDep = pinfosOfCallee.get(dep);
                if (localDep != null) {
                    if (!passthroughDep.uniqueName.equals(localDep.uniqueName)) {
                        repl.put(dep, new ExprVar((FEContext) null,
                                passthroughDep.uniqueName));
                    }
                }
            }
            return repl;
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
                            System.out.println("CHECKHERE1");
                            funsToVisit.put(fn,
                                    mergePI(nfi.cloneParamsToAdd(), funsToVisit.get(fn)));
                        } else {
                            funsToVisit.put(fn, nfi.cloneParamsToAdd());
                        }
                    }
                }else{
                    toVisit.push(key);
                    if (funsToVisit.containsKey(key)) {
                        System.out.println("CHECKHERE2");
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
                                Map<String, ParamInfo> pinfosOfCaller =
                                        funsToVisit.get(callerName);
                                mergePI(pinfosOfCaller, nfi.paramsToAdd);
                            } else {
                                // Get the current function
                                Function currentFunction = cg.getByName(cur);

                                // Loop through the formal parameters of the current
                                // function
                                for (Parameter parameter : currentFunction.getParams()) {
                                    // If the current parameter is a reference and the
                                    // function that calls
                                    // the current function needs some variables
                                    if (parameter.isParameterReference() &&
                                            lambdaFunctionsNeededVariables.containsKey(caller.getName()))
                                    {

                                        // Loop through each variable that is needed
										for (Entry<ExprVar, ExprVar> entry : lambdaFunctionsNeededVariables
												.get(caller.getName()).entrySet())
                                        {
                                            // Add a parameter to add to the caller
                                            TreeSet<String> dependent =
                                                    new TreeSet<String>();
											nfi.paramsToAdd.put(entry.getKey().getName(),
													new ParamInfo(
                                                    parameter.getType(),
 entry.getKey().getName(),
															entry.getKey().getName(),
                                                            true, dependent));
                                        }


                                    }
                                }

                                funsToVisit.put(callerName,
                                        renameParams(nfi.cloneParamsToAdd()));
                            }
                        }
                    }
                }
                
            }
            return super.visitProgram(prog);
        }

        HashMap<String, ParamInfo> renameParams(
                HashMap<String, ParamInfo> torename) {
            HashMap<String, ParamInfo> newmap = new HashMap<String, ParamInfo>();
            for (Entry<String, ParamInfo> entry : torename.entrySet()) {
                newmap.put(entry.getValue().name, entry.getValue()
                        .makePassthrough());
            }
            return newmap;
        }

        private List<Parameter> getAddedParams(String funName,
                boolean isGenerator, boolean isCall) {
            List<Parameter> result = null; // addedParams.get(funName);
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

                Map<String, Expression> vmap = new HashMap<String, Expression>();
                FEReplacer repl = new VarReplacer(vmap);
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
                        makeRef = makeRef || info.paramType instanceof TypeArray;
                    }
                    String tname = name;
                    if (isCall) {
                        if (enclosingAdded.contains(info.uniqueName())) {
                            tname = info.uniqueName();
                        } else {
                            tname = info.name;
                        }

                    } else {
                        if (info.isPassthrough()) {
                            tname = info.uniqueName();
                            vmap.put(name, new ExprVar((FEContext) null,
                                    info.uniqueName()));
                        } else {
                            tname = name;
                        }
                    }
                    result.add(new Parameter(null, (Type) info.paramType
                            .accept(repl), tname,
                            makeRef ? Parameter.REF
                            : Parameter.IN));

                }

                addedParams.put(funName, result);
            }
            return result;
        }

        /**
         * Visit the parameter to check if it needs to be renamed to another
         * name. This happens when a lambda that needs a value threaded calls an
         * inner function
         */
        public Object visitParameter(Parameter parameter, Function function) {
            // If the function call needs variables
            if (lambdaFunctionsNeededVariables.containsKey(function.getName())) {
                // Get the needed variables
                Map<ExprVar, ExprVar> neededVariables = lambdaFunctionsNeededVariables
                        .get(function.getName());
                
                ExprVar tempVariable = new ExprVar(parameter.getContext(), parameter.getName());

                // If this parameter is a needed variable
                if (neededVariables.containsKey(tempVariable)) {
                    // Create a new parameter
                    parameter = new Parameter(parameter, parameter.getSrcTupleDepth(),
                            parameter.getType(), neededVariables.get(tempVariable).getName(),
                            parameter.getPtype());
                }
            }

            // Check the super visitor
            return super.visitParameter(parameter);
        }

        public Object visitExprFunCall(ExprFunCall efc) {
            String name = nres.getFunName(efc.getName());
            Function f = nres.getFun(efc.getName());
            if (funsToVisit.containsKey(name)) {
                List<Parameter> addedParams = getAddedParams(name,
                        f.isGenerator(), true);
                if (addedParams.size() != 0) {
                    List<Expression> pl = new ArrayList<Expression>(efc.getParams());
                    for (Parameter p : addedParams) {
                        pl.add(new ExprVar(efc, p.getName()));
                        
                        // If the function that we are calling needs a variable
                        if(lambdaFunctionsNeededVariables.containsKey(efc.getName())) {
                            // Loop through the variables needed
							for (Entry<ExprVar, ExprVar> entry : lambdaFunctionsNeededVariables.get(efc.getName())
									.entrySet()) {
                                // If a needed variable is the same as the current
                                // parameter that we just added
								if (entry.getKey().getName() == p.getName()) {
                                    // Delete it from the needed variables
                                    lambdaFunctionsNeededVariables.get(efc.getName()).remove(entry);

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
            enclosingAdded = new HashSet<String>();
            if (funsToVisit.containsKey(name)) {
                List<Parameter> pl = new ArrayList<Parameter>(fun.getParams());
                List<Parameter> newps = getAddedParams(name, fun.isGenerator(),
                        false);
                for (Parameter p : newps) {
                    p = (Parameter) this.visitParameter(p, fun);
                    enclosingAdded.add(p.getName());
                    pl.add(p);
                }
                // pl.addAll(newps);

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
 new FunctionParamRenamer(nfn, efc, nres.curPkg().getName(), null);
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
     * There are some functions that can no longer be polymorphic after we specialize them
     * based on their function parameters because the function parameters impose certain
     * constraints on the outputs. This class will specialize the types for these
     * functions by making them less generic.
     */
    class FixPolymorphism extends SymbolTableVisitor {
        /**
         * The tren type renamer keeps track of what generic types should be specialized
         * and to what.
         */
        TypeRenamer tren;

        /**
         * namesset keeps track of the original type parameters.
         */
        Set<String> namesset;

        /**
         * elimset keeps track of which type parameters are no longer necessary because
         * they are being eliminated.
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
            substitute(oldtren, tren);
            tren = oldtren;
            namesset = oldnamesset;
            elimset = oldelimset;
            doneFunctions.put(f.getFullName(), rf);
            return rf;
        }

        private void substitute(TypeRenamer oldtren, TypeRenamer tren) {
            if (oldtren == null)
                return;
            for (String k : oldtren.tmap.keySet()) {
                String t = oldtren.tmap.get(k).toString();
                if (tren.tmap.containsKey(t)) {
                    oldtren.tmap.put(k, tren.tmap.get(t));
                }
            }

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
            if (lname.equals(right.toString())) {
                return;
            }
            if (namesset.contains(lname)) {
                unifyGeneric(lname, right, ctxt);
            }
        }

        void unifyGeneric(String genericName, Type newType, FENode ctxt) {
            elimset.add(genericName);
            if (newType instanceof TypeArray) {
                throw new ExceptionAtNode("Generics can not resolve to an array type " +
                        genericName + "->" + newType, ctxt);
            }
            if (tren.tmap.containsKey(genericName)) {
                Type lcp = tren.tmap.get(genericName).leastCommonPromotion(newType, nres);
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

		private Function currentFunction = null;
        private boolean callingLocalFunction = false;
        private Map<String, List<Parameter>> tempFunctionsParametersNeeded =
                new HashMap<String, List<Parameter>>();

        public LambdaThread() {
            super(null);
        }

        public Object visitFunction(Function function) {
			// Get the previous function
			Function oldFunction = this.currentFunction;

			// The current function is this one
			this.currentFunction = function;

            // if there is a temp function that needs parameters
            if (this.tempFunctionsParametersNeeded.containsKey(function.getName())) {
                // New formal parameters
                List<Parameter> formalParamters = new ArrayList<Parameter>();

                // Get the current formal parameters
                formalParamters.addAll(function.getParams());

                // Loop through all the parameters that the function needs
                for (Parameter parameter : this.tempFunctionsParametersNeeded.get(function.getName()))
                {
                    // add the parameter to the formal parameters
                    formalParamters.add(parameter);

					// Add it to the symbol table
					this.symtab.registerVar(parameter.getName(), parameter.getType());
				}

                // Create a new function with the new parameters
                function =
                        createTempFunction(function, function.getName(),
                                function.getPkg(), formalParamters);
            }

			// Visit the function
			Function result = (Function) super.visitFunction(function);

			// Restore the previous current function
			this.currentFunction = oldFunction;

			// Return this function
			return result;
        }

        public Object visitExprFunCall(ExprFunCall exprFunctionCall) {
            // If this is a lambda call and it needs variables
            if (lambdaFunctionsNeededVariables.containsKey(exprFunctionCall.getName())) {
                // Get the variables that are needed in this call
				Map<ExprVar, ExprVar> variablesNeeded =
                        lambdaFunctionsNeededVariables.get(exprFunctionCall.getName());

                // Lists for holding the parameters
                List<Expression> actualParameters = new ArrayList<Expression>();
                List<Parameter> formalParameters = new ArrayList<Parameter>();

                // Add the current actual parameters
                actualParameters.addAll(exprFunctionCall.getParams());

                // Get the callee formal parameters
                List<Parameter> calleeFormalParameters =
                        this.nres.getFun(exprFunctionCall.getName()).getParams();

                // If the current function needs some variables
				if (lambdaFunctionsNeededVariables.containsKey(this.currentFunction.getName())) {
					// We might need to rename some of the variables that are being threaded.
					// This happens when a function deep in the call graph needs a variable from
					// a function high in the call graph
					Map<ExprVar, ExprVar> newNeededVariables = new HashMap<ExprVar, ExprVar>();
					
					// Loop through the need variables of the current function
					for (Entry<ExprVar, ExprVar> oldEntry : lambdaFunctionsNeededVariables
							.get(this.currentFunction.getName()).entrySet()) {
						// If there is a variable that is needed both in the current
						// function and the function call
						if(variablesNeeded.containsKey(oldEntry.getKey())) {
							// Rename the needed variable
							newNeededVariables.put(oldEntry.getValue(), variablesNeeded.get(oldEntry.getKey()));
						}
						else {
							// Put the variables without any change
							newNeededVariables.put(oldEntry.getKey(), oldEntry.getValue());
						}
					}
					
					// Put the new needed variables for this function call
					lambdaFunctionsNeededVariables.put(exprFunctionCall.getName(), newNeededVariables);
					
					// Get the variables that are needed in this call
					variablesNeeded = lambdaFunctionsNeededVariables.get(exprFunctionCall.getName());
				}
                
                // Loop through the variables needed
				for (Entry<ExprVar, ExprVar> entry : variablesNeeded.entrySet()) {

                    // TODO This check is to make sure that we are not double
                    // adding
                    // variables
                    // If this variable is already added
                    // if(actualParameters.contains(variable)) {
                    // // Skip this variable
                    // continue;
                    // }

                    // Loop through the formal parameters of the callee
                    for (Parameter formalParameter : calleeFormalParameters) {
                        // If this variables that we are trying to thread is
                        // already defined in this function
						if (formalParameter.getName().equals(entry.getKey().getName())) {
                            // Get the function that we are calling
                            Function function =
                                    this.nres.getFun(exprFunctionCall.getName());

                            // Visit it
                            function = (Function) function.accept(this);
                            // // Get the name of the lammbda that we are calling,
                            // // which is
                            // String functionCallName = exprFunctionCall.getName();
                            // functionCallName =
                            // functionCallName.substring(functionCallName.indexOf("_"));
                            // functionCallName = functionCallName.substring(1);
                            //
                            // If we are calling a local function
                            if (this.callingLocalFunction) {
                                // Don't go any further
                                break;
                            }

                            // Throw exception
                            throw new ExceptionAtNode(
                                    "You are inlining a lambda function that has"
                                            + " variables already defined in the original function",
                                    exprFunctionCall);
                        }

                    }

                    // If we are calling a local function
                    if (this.callingLocalFunction) {
                        // Don't go any further
                        break;
                    }

					// if (tempFunctionsParametersNeeded.containsKey(key))

                    // Add the variable to the actual parameters
					actualParameters.add(entry.getKey());

                    // Get the type of variable
					Type type = this.symtab.lookupVar(entry.getKey());

                    // Add the parameter to the list of formal parameters of the function
                    // declaration
					formalParameters.add(new Parameter(entry.getValue(), type, entry.getValue().getName()));

                    // Reset variable to default
                    this.callingLocalFunction = false;
                }

                // Create a new function call with the new actual parameters
                exprFunctionCall =
                        new ExprFunCall(exprFunctionCall, exprFunctionCall.getName(),
                                actualParameters);

                // Add the formal parameter to be replaced in the function call
                this.tempFunctionsParametersNeeded.put(exprFunctionCall.getName(),
                        formalParameters);
            }

            if (extractedInnerFuns.containsKey(exprFunctionCall.getName() + "@" +
                    nres.curPkg().getName()))
            {
                this.callingLocalFunction = true;
            }

            // Visit and return the function call
            return (ExprFunCall) super.visitExprFunCall(exprFunctionCall);
        }

    }

    private static class NOpair {
        Object origin;
        String name;

        NOpair(String name, Object origin) {
            this.name = name;
            this.origin = origin;
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
        boolean isGenerator = false;
        final boolean recursive;
        boolean topLevel = true;
        int nfcnt = 0;
        FunReplMap frmap = new FunReplMap(null);
        Function curFun;
        Set<String> allVarNames = new HashSet<String>();
        int nparcnt = 0;
        boolean inLambda = false;

        Map<String, List<NOpair>> uniqueNames = new HashMap<String, List<NOpair>>();

        String makeUnique(String name, Object origin) {
            List<NOpair> lp = uniqueNames.get(name);
            if (lp == null) {
                lp = new ArrayList<NOpair>();
                uniqueNames.put(name, lp);
            } else {
                for (NOpair np : lp) {
                    if (np.origin == origin) {
                        return np.name;
                    }
                }
            }

            String out = name + nparcnt;
            ++nparcnt;
            while (allVarNames.contains(out)) {
                out = name + nparcnt;
                ++nparcnt;
            }
            allVarNames.add(out);
            lp.add(new NOpair(out, origin));
            return out;
        }
        
        InnerFunReplacer(boolean recursive) {
            super(null);
            this.recursive = recursive;
        }

        public void registerGlobals(Program p) {

            FEReplacer allnames = new FEReplacer(){
                public Object visitStmtVarDecl(StmtVarDecl svd){
                    for(int i=0; i<svd.getNumVars(); ++i){
                        allVarNames.add(svd.getName(i));
                    }
                    return svd;
                }
                public Object visitStmtAssign(StmtAssign sa){
                    return sa;
                }
                public Object visitStmtAssert(StmtAssert sa){
                    return sa;
                }

                public Object visitParameter(Parameter p) {
                    allVarNames.add(p.getName());
                    return p;
                }
            };
            
            for (Package pkg : p.getPackages()) {

                SymbolTable st = new SymbolTable(null);
                symtab = st;
                for (FieldDecl fd : pkg.getVars()) {
                    fd.accept(this);
                    for(int i=0; i<fd.getNumFields(); ++i){
                        allVarNames.add(fd.getName(i));
                    }
                }
                tempSymtables.put("pkg:" + pkg.getName(), st);
                p.accept(allnames);
            }
                        
            
            
            symtab = null;
        }

        /**
         * This function computes the NewFunInfo for a given nested function.
         * It's main job is to identify variables that will have to be threaded
         * through the closure.
         * 
         * @author Armando
         * 
         */
        private class LocalFunctionAnalyzer extends SymbolTableVisitor {
            private final NewFunInfo nfi;
            boolean isAssignee = false;
            TreeSet<String> dependent = null;
            boolean isInParam = false;

            private LocalFunctionAnalyzer(SymbolTable symtab, NewFunInfo nfi) {
                super(symtab);
                this.nfi = nfi;
            }

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

                    VarInfo vi = InnerFunReplacer.this.symtab.lookupVarInfo(exp
                            .getName());
                    Type pt = null;
                    if (vi != null) {
                        pt = vi.type;
                    }
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

                    if (pt instanceof TypeStructRef) {
                        if (typeParams.size() > 0 && typeParams.contains(((TypeStructRef) pt).getName())) {
                            nfi.typeParamsToAdd.add(((TypeStructRef) pt).getName());
                        }
                    }

                    TreeSet<String> oldDependent = dependent;
                    ParamInfo info = this.nfi.paramsToAdd.get(name);
                    if (info == null) {
                        dependent = new TreeSet<String>();
                        this.nfi.paramsToAdd.put(name, new ParamInfo(pt, name,
                        		makeUnique(name, vi.origin), 
                                isAssignee,
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

            public Object visitParameter(Parameter p) {
                boolean op = isInParam;
                isInParam = true;
                Type pt = p.getType();
                if (pt instanceof TypeStructRef) {
                    if (typeParams.size() > 0 && typeParams.contains(((TypeStructRef) pt).getName())) {
                        nfi.typeParamsToAdd.add(((TypeStructRef) pt).getName());
                    }
                }
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
            FunReplMap parent = null;
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

        List<String> typeParams = new ArrayList<String>();

        public Object visitFunction(Function fun) {
            boolean tmpIsGen = isGenerator;
            isGenerator = fun.isGenerator();
            FunReplMap tmp = frmap;
            frmap = new FunReplMap(tmp);
            Function tmpf = curFun;
            curFun = fun;
            int pos = typeParams.size();
            typeParams.addAll(fun.getTypeParams());
            for (Parameter p : fun.getParams()) {

                frmap.declRepl(p.getName(), null);

            }
            Object o = super.visitFunction(fun);
            typeParams.subList(pos, typeParams.size()).clear();
            assert typeParams.size() == pos;
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
                if (svd.getType(i) instanceof TypeFunction &&
                        svd.getInit(0) instanceof ExprLambda) {
					// If this function name has been declared before
                	if (symtab.hasVar(svd.getName(i))) {
                        throw new ExceptionAtNode("Shadowing of variables is not allowed.", svd);
                    }
                	
                	// Visit the lambda in case some of the values in the expression need
                    // to change
                    currentExprLambda = (ExprLambda) svd.getInit(0);
                    currentExprLambda = (ExprLambda) this.doExpression(currentExprLambda);

                    // Map the function call to the lambda expression
                    localLambda.put(svd.getName(0) + tempFunctionsCount,
                            currentExprLambda);

                    // Map the new name with the old
                    lambdaRenameMap.put(svd.getName(0), svd.getName(0) +
                            tempFunctionsCount);
                    
					// Register this function variable in the symbol table
                    symtab.registerVar(svd.getName(0), svd.getType(0), svd, SymbolTable.KIND_LOCAL);

                    // Increment the number of temp functions
                    tempFunctionsCount++;

                    // Set the current lambda to null
                    currentExprLambda = null;

                    return null;

                } else {
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
            // If there is a variable that needs to be replaced
            if (lambdaRenameMap.containsKey(exprVar.getName())) {
                // Replace the variable
                return new ExprVar(exprVar, lambdaRenameMap.get(exprVar.getName()));
            }
			// If we are in a lambda and the current function needs a variable
            else if (this.inLambda && lambdaFunctionsNeededVariables.containsKey(curFun.getName())) {
				// If the variable is needed
            	if(lambdaFunctionsNeededVariables.get(curFun.getName()).containsKey(exprVar)) {
					// Return the fresh variable that maps to this variable
            		return lambdaFunctionsNeededVariables.get(curFun.getName()).get(exprVar);            	
            	}
    		}

            // Visit using the main class method since there might be so
            // replacing needed
			return RemoveFunctionParameters.this.visitExprVar(exprVar);
        }

        public Object visitExprLocalVariables(ExprLocalVariables exprLocalVariables) {
            // // Set the symbol table to this one
            // exprLocalVariables.setSymbolTableInContext(this.symtab);

            return super.visitExprLocalVariables(exprLocalVariables);
        }

        /**
         * Visit an exprUnary an check if its part of a lambda expression. If it is, check
         * that it is not modifying a parameter of a lambda.
         */
        public Object visitExprUnary(ExprUnary exprUnary) {
            // If we are not analyzing a lambda expression
            if (currentExprLambda == null) {
                // Call the super class to visit the unary expression
                return super.visitExprUnary(exprUnary);
            }

            // Loop through the formal parameters of the lambda expression
            for (ExprVar formalParameter : currentExprLambda.getParameters()) {
                // If the unary expression has a formal parameter
                if (formalParameter.equals(exprUnary.getExpr())) {
                    // Thrown exception since we cannot modify a formal parameter of
                    // a lambda expression
                    throw new ExceptionAtNode("You cannot have an unary expression of "
                            + "a formal parameter in a lambda", exprUnary);
                }
            }

            return super.visitExprUnary(exprUnary);

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
				inLambda = true;

				Object inlineLambda = inlineLocalLambda(efc, localLambda.get(newName));
                // Return that inlined version of the lambda expression

				inLambda = false;

				return inlineLambda;
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

            if (nfi.typeParamsToAdd.size() > 0) {
                newFun.getTypeParams().addAll(nfi.typeParamsToAdd);
            }

            extractedInnerFuns.put(nfi.funName, nfi);
            return null;
        }

		private Object inlineLocalLambda(ExprFunCall functionCall, ExprLambda exprLambda) {
			// If the number of function call parameters does not match the
			// length of formal parameters of the lambda expression
			if (exprLambda.getParameters().size() != functionCall.getParams().size()) {
				throw new ExceptionAtNode("The number of lambda parameters does not match "
											+ "the number of parameters in the function call: "
											+ exprLambda.getParameters() + " - "
											+ functionCall.getParams(),
						functionCall);
			}

			// Set the current lambda
			currentExprLambda = exprLambda;

			// Replacements should be local, so save the current map
			Map<ExprVar, Expression> oldLambdaReplace = lambdaReplace;

			// create a new map
			lambdaReplace = new HashMap<ExprVar, Expression>();

			// Loop through the formal parameters of the lambda and the actual
			// parameters
			// of the call mapping them.
			for (int i = 0; i < exprLambda.getParameters().size(); i++) {
				// Get the actual parameter
				Expression actualParameter = functionCall.getParams().get(i);

				// If the actual parameter is a function call
				if (actualParameter instanceof ExprFunCall) {
					// We need to do some processing since the function must be
					// called once, but the lambda might inline it multiple times.
					// So we need to make a function call and assign it to a variable,
					// to only call it once.
					// If the new statements list is null
					if (newStatements == null) {
						// Create a new list
						newStatements = new ArrayList<Statement>();
					}

					// Get the function being called
					Function callee = this.nres.getFun(((ExprFunCall) actualParameter).getName());

					// If the function was not found
					if (callee == null) {
						// Check the function rename map
						String newName = hoister.frmap.findRepl(((ExprFunCall) actualParameter).getName());

						// Search a function with the new name
						callee = this.nres.getFun(newName);

						// If the function was not found
						if (callee == null) {
							throw new ExceptionAtNode("Function " + functionCall.getName() +
				                    " either does not exist, or is ambiguous.", functionCall);
						}
					}
											
					// Create a new function call
					ExprFunCall newFunctionCall = new ExprFunCall(callee, callee.getName(),
							((ExprFunCall) functionCall.getParams().get(i)).getParams());

					// Add a variable declaration to the statements
					Random random = new Random();
					String randomVariableName = "v" + Math.abs(random.nextInt());
					
					this.addStatement(new StmtVarDecl(functionCall, callee.getReturnType(),
							randomVariableName, newFunctionCall));

					// The actual parameter now is just a variable
					actualParameter = new ExprVar(functionCall, randomVariableName);
					
					// Register the variable in the symbol table
					symtab.registerVar(randomVariableName, callee.getReturnType(), 
							actualParameter, SymbolTable.KIND_LOCAL);
					
				}

				// Add the actual parameter that will be replace when inlining the lambda
				lambdaReplace.put(exprLambda.getParameters().get(i), actualParameter);
			}

			// Visit the expression in case there needs to be some replacement
			// before getting
			// the previous replacement map
			Expression newExpression = this.doExpression(exprLambda.getExpression());

			// Restore the replacement map
			lambdaReplace = oldLambdaReplace;

			// // Check if there are any replacements left
			// newExpression = this.doExpression(newExpression);

			// Set the current lambda to null
			currentExprLambda = null;

			// Return a new expression where all the variables are replaced with
			// actual parameters
			return newExpression;
		}


        NewFunInfo funInfo(Function f) {
            // get the new function info
            // i.e. the used variables that are in f's containing function
            // among the used variables, some are modified, we also track if a used var is
            // changed. curFun is the function that lexically encloses f.
            final String theNewFunName = nres.getFunName(f.getName());
            final NewFunInfo nfi =
                    new NewFunInfo(theNewFunName, nres.getFunName(curFun.getName()));
            SymbolTableVisitor stv = new LocalFunctionAnalyzer(null, nfi);
            stv.setNres(nres);
            f.accept(stv);
            return nfi;
        }

    } // end of InnerFunReplacer

}

