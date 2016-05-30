package sketch.compiler.passes.bidirectional;

import java.util.*;
import java.util.Map.Entry;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Package;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.SymbolTable;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtBlock;
import sketch.compiler.ast.core.stmts.StmtEmpty;
import sketch.compiler.ast.core.stmts.StmtFunDecl;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.ast.core.typs.TypeFunction;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.passes.lowering.SymbolTableVisitor;
import sketch.compiler.passes.lowering.SymbolTableVisitor.TypeRenamer;
import sketch.util.Pair;
import sketch.util.exceptions.ExceptionAtNode;
import sketch.util.exceptions.TypeErrorException;

public class RemoveFunctionParameters extends BidirectionalPass {


    /***
     * When we hoist a function, we need to remember the symboltable at the
     * point of hoisting so that later we will remember it.
     * 
     */
    Map<String, SymbolTable> tempSymtables = new HashMap<String, SymbolTable>();

    Map<String, List<String>> equivalences = new HashMap<String, List<String>>();
    Map<String, String> reverseEquiv = new HashMap<String, String>();
    Map<String, Function> funToReplace = new HashMap<String, Function>();
    Set<String> newFunctions = new HashSet<String>();
    Map<String, Package> pkges;
    Map<String, String> nfnMemoize = new HashMap<String, String>();
    Set<String> visited = new HashSet<String>();
    boolean hasArrayUnifs = false;

    int nfcnt = 0;

    private int tempFunctionsCount;

    public RemoveFunctionParameters() {
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
                funToReplace.put(nres().getFunName(fun.getName()), fun);

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
        while (nres().getFun(newName) != null) {
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
                throw new TypeErrorException("Incorrect number of parameters to function " + orig, efc);
            }
            for (int i = 0; i < diff; ++i) {
                if (!fp.hasNext()) {
                    throw new TypeErrorException("Incorrect number of parameters to function " + orig, efc);
                }
                Parameter p = fp.next();
                if (!p.isImplicit()) {
                    throw new TypeErrorException("Incorrect number of parameters to function " + orig, efc);
                }
            }
        }
        
        Set<String> tparams = new HashSet<String>(orig.getTypeParams());
        Map<String, Type> tmap = new TreeMap<String, Type>();
        for (Expression actual : efc.getParams()) {
            Parameter p = fp.next();
            if (p.getType() instanceof TypeFunction) {
                name += "_" + actual.toString();
            }
            Type actType = driver.getType(actual);
            Map<String, Type> lmap = p.getType().unify(actType, tparams);
            for (Entry<String, Type> st : lmap.entrySet()) {
                if (tmap.containsKey(st.getKey())) {
                    tmap.put(st.getKey(), tmap.get(st.getKey()).leastCommonPromotion(st.getValue(), nres()));
                } else {
                    tmap.put(st.getKey(), st.getValue());
                }
            }
        }
        for (Entry<String, Type> st : tmap.entrySet()) {
            if (st.getValue() instanceof TypeArray) {
                hasArrayUnifs = true;
            }
            name += st.getKey() + "_" + st.getValue().cleanName();
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

    class RefreshParams extends FEReplacer {
        Map<String, Expression> rmap = new TreeMap<String, Expression>();

        public Object visitTypeArray(TypeArray ta) {
            Type base = (Type) ta.getBase().accept(this);
            Expression len = ta.getLength();
            ExprVar vg = new ExprVar(len, driver.varGen.nextVar());
            rmap.put(vg.getName(), len);
            return new TypeArray(base, vg);
        }
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

        TypeRenamer tren = null;
        RefreshParams rparam = null;
        if (!orig.getTypeParams().isEmpty()) {
            List<Type> actualTypes = new ArrayList<Type>();
            for (Expression ap : efc.getParams()) {
                actualTypes.add(driver.getType(ap));
            }
            tren = SymbolTableVisitor.getRenaming(orig, actualTypes, nres(), tdstate().getExpected());

            rparam = new RefreshParams();

            for (Entry<String, Type> et : tren.tmap.entrySet()) {
                et.getValue().accept(rparam);
            }

            for (Entry<String, Expression> ee : rparam.rmap.entrySet()) {
                params.add(ee.getValue());
            }
        }

        for (Expression actual : efc.getParams()) {
            Parameter p = fp.next();
            if (!(p.getType() instanceof TypeFunction)) {
                params.add(actual);
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


        final String cpkg = nres().curPkg().getName();
        for (Expression actual : efc.getParams()) {
            Parameter formal = it.next();
            if (formal.getType() instanceof TypeFunction) {
                // If the function is found, good! Otherwise, have to create
                // function
                Function fun = nres().getFun(actual.toString());
                if (fun == null) {
                    throw new ExceptionAtNode("Function " + actual + " does not exist", efc);
                }
            }

        }

        FEReplacer renamer = new FunctionParamRenamer(nfn, efc, cpkg);

        return (Function) orig.accept(renamer);
    }


    public Object visitProgram(Program p) {
        // p = (Program) p.accept(new SpecializeInnerFunctions());
        // p.debugDump("After specializing inners");
        // p = (Program) p.accept(new InnerFunReplacer());
        // We visited all packages and all functions


        Map<String, List<Function>> nflistMap = new HashMap<String, List<Function>>();
        pkges = new HashMap<String, Package>();

        // Loop each package
        for (Package pkg : p.getPackages()) {
            nres().setPackage(pkg);
            for (Function fun : pkg.getFuncs()) {
                // Check the function parameters
                checkFunParameters(fun);
            }
            nflistMap.put(pkg.getName(), new ArrayList<Function>());
            pkges.put(pkg.getName(), pkg);
        }

        return p;
    }

    /**
     * Visit a package returns null
     * 
     * public Object visitPackage(Package spec) { return null; }
     */



    class PostCheck extends BidirectionalPass {
        public Object visitExprFunCall(ExprFunCall efc) {
            if (efc.getName().equals("minimize")) {
                return super.visitExprFunCall(efc);
            }

            String name = nres().getFunName(efc.getName());
            if (name == null) {
                return efc;
            }

            Function orig = nres().getFun(name);

            hasArrayUnifs = false;
            String nfn = newFunName(efc, orig);

            // If this function call is one that we need to replace. Most likely
            // a
            // fun
            if (driver.needsSpecialization(orig) || hasArrayUnifs) {
                // Get the function to replace
                // Function orig = funToReplace.get(name);
                // Get the new function name

                // If new function already has this new function
                if (newFunctions.contains(nfn)) {
                    return replaceCall(efc, orig, nfn);
                } else {
                    // Create a new call of this function
                    Function newFun = createCall(efc, orig, getNameSufix(nfn));
                    nres().registerFun(newFun);
                    String newName = nres().getFunName(newFun.getName());
                    addEquivalence(name, newName);
                    newFunctions.add(nfn);
                    SymbolTable tmp = driver.getClosure(name);
                    if (tmp != null) {
                        SymbolTable reserve = driver.swapSymTable(tmp);
                        Function post = driver.doFunction(newFun);
                        driver.addFunction(post);
                        driver.swapSymTable(reserve);
                    } else {
                        driver.queueForAnalysis(newFun);
                    }

                    return replaceCall(efc, orig, nfn);
                }
            } else {
                return efc;
            }
        }
    }

    public BidirectionalPass getPostPass() {
        return new PostCheck();
    }


    private final class FunctionParamRenamer extends FEReplacer {
        private final String nfn;
        private final ExprFunCall efc;
        private final String cpkg;
        private final Map<String, String> rmap = new HashMap<String, String>();


        private FunctionParamRenamer(String nfn, ExprFunCall efc, String cpkg) {
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

            TypeRenamer tren = null;
            RefreshParams rparam = null;
            if (!func.getTypeParams().isEmpty()) {
                List<Type> actualTypes = new ArrayList<Type>();
                for (Expression ap : efc.getParams()) {
                    actualTypes.add(driver.getType(ap));
                }
                tren = SymbolTableVisitor.getRenaming(func, actualTypes, nres(), tdstate().getExpected());

                rparam = new RefreshParams();

                for (Entry<String, Type> et : tren.tmap.entrySet()) {
                    et.setValue((Type) et.getValue().accept(rparam));
                }

                for (Entry<String, Expression> ee : rparam.rmap.entrySet()) {
                    newParam.add(new Parameter(ee.getValue(), TypePrimitive.inttype, ee.getKey(), Parameter.IN, false));
                }
            }



            for (Expression actual : this.efc.getParams()) {
                Parameter par = fp.next();
                Parameter newPar = (Parameter) par.accept(this);
                if (!(par.getType() instanceof TypeFunction)) {
                    if (tren != null) {
                        newPar = new Parameter(newPar, tren.rename(newPar.getType()), newPar.getName(), newPar.getPtype(), newPar.isImplicit());
                    }
                    if (par != newPar)
                        samePars = false;
                    newParam.add(newPar);
                }
                // If actual is a lambda expression
                else {
                    samePars = false;
                    this.rmap.put(par.getName(), actual.toString());
                }

            }

            Type rtype = (Type) func.getReturnType().accept(this);
            if (tren != null) {
                rtype = tren.rename(rtype);
            }

            if (func.getBody() == null) {
                assert func.isUninterp() : "Only uninterpreted functions are allowed to have null bodies.";
                if (samePars && rtype == func.getReturnType())
                    return func;
                return func.creator().returnType(rtype).pkg(this.cpkg).params(newParam).create();
            }
            Statement newBody = (Statement) func.getBody().accept(this);
            List<String> newTP = Collections.EMPTY_LIST;
            if (tren != null) {
                newBody = (Statement) newBody.accept(tren);
                Function enclosing = tdstate().getCurrentFun();
                newTP = enclosing.getTypeParams();
            }
            if (newBody == null)
                newBody = new StmtEmpty(func);
            if (newBody == func.getBody() && samePars && rtype == func.getReturnType())
                return func;
            return func.creator().returnType(rtype).params(newParam).body(newBody).name(this.nfn).pkg(this.cpkg).typeParams(newTP).create();
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
                if (hasChanged) {
                    return new ExprFunCall(efc, efc.getName(), newParams);
                } else {
                    return efc;
                }
            }
        }

        public Object visitExprVar(ExprVar ev) {
            if (this.rmap.containsKey(ev.getName())) {
                return new ExprVar(ev, this.rmap.get(ev.getName()));
            } else {
                return ev;
            }
        }
    }

    /**
     * this FEReplacer does a complex job: 1. flatten all functions so that
     * there is no inner functions 2. all inner functions are hoisted out, so we
     * need to pass parameters in the scope of their containing functions, and
     * care must be taken to add "ref" if the modified vars 3. all parameters
     * that are "fun" are now removed, by specializing the callee. Example:
     * <code>
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

    class SpecializeInnerFunctions extends FEReplacer {



        Stack<Map<String, Pair<Function, Pair<List<Statement>, Set<String>>>>> postponed = new Stack<Map<String, Pair<Function, Pair<List<Statement>, Set<String>>>>>();

        Pair<Function, Pair<List<Statement>, Set<String>>> isPostponed(String name) {
            for (Map<String, Pair<Function, Pair<List<Statement>, Set<String>>>> m : postponed) {
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
                FunctionParamRenamer renamer = new FunctionParamRenamer(nfn, efc, nres.curPkg().getName());
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
                        new Pair<Function, Pair<List<Statement>, Set<String>>>(f, new Pair<List<Statement>, Set<String>>(newStatements, new HashSet<String>())));
                return null;
            } else {
                return super.visitStmtFunDecl(sfd);
            }
        }

    }

}

