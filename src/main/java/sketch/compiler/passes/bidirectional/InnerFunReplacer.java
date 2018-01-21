package sketch.compiler.passes.bidirectional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import sketch.compiler.ast.core.FEContext;
import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.FieldDecl;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Package;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.SymbolTable;
import sketch.compiler.ast.core.SymbolTable.VarInfo;
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
import sketch.compiler.ast.core.stmts.StmtAssert;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtFunDecl;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.StructDef;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.ast.core.typs.TypeFunction;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.ast.core.typs.TypeStructRef;
import sketch.compiler.passes.lowering.SymbolTableVisitor;
import sketch.compiler.stencilSK.VarReplacer;
import sketch.util.exceptions.ExceptionAtNode;

class NOpair {
    Object origin;
    String name;

    NOpair(String name, Object origin) {
        this.name = name;
        this.origin = origin;
    }
}

class ParamInfo {
    final String name;
    final String uniqueName;
    final Type paramType;
    // whether this variable has been changed
    // should be ORed when merging.
    // If it has been changed, then it will become a reference parameter.
    boolean changed;

    /**
     * Indicates that this is a parameter that is only being passed through this
     * function. it neither originated here nor is this its final destination.
     */
    boolean passthrough;

    // the variables that this param depends on
    // currently capture the type relation
    // example: int [x] y;
    // then dependence of y contains x
    final TreeSet<String> dependence;

    public ParamInfo(Type pt, String name, String uniqueName, boolean changed, TreeSet<String> dependence) {
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
        return new ParamInfo(this.paramType, this.name, this.uniqueName, this.changed, (TreeSet<String>) this.dependence.clone());
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
 * Information about functions created as a result of hoisting out an inner
 * function.
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
 * Hoist out inner functions, and extract the information about which inner
 * function used what variables defined in its containing function (the
 * "closure"). This visitor will define the value of
 * <code> extractedInnerFuns </code>.
 * 
 * @author asolar, tim
 */
public class InnerFunReplacer extends BidirectionalPass {

    Map<String, NewFunInfo> extractedInnerFuns = new HashMap<String, NewFunInfo>();

    int nfcnt = 0;
    Set<String> allVarNames = new HashSet<String>();
    int nparcnt = 0;

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

    public InnerFunReplacer() {
    }

    public void registerGlobals(Program p) {
        FEReplacer allnames = new FEReplacer() {
            public Object visitStmtVarDecl(StmtVarDecl svd) {
                for (int i = 0; i < svd.getNumVars(); ++i) {
                    allVarNames.add(svd.getName(i));
                }
                return svd;
            }

            public Object visitStmtAssign(StmtAssign sa) {
                return sa;
            }

            public Object visitStmtAssert(StmtAssert sa) {
                return sa;
            }

            public Object visitParameter(Parameter p) {
                allVarNames.add(p.getName());
                return p;
            }
        };

        for (Package pkg : p.getPackages()) {

            SymbolTable st = new SymbolTable(null);
            for (FieldDecl fd : pkg.getVars()) {
                for (int i = 0; i < fd.getNumFields(); i++)
                    st.registerVar(fd.getName(i), fd.getType(i), fd, SymbolTable.KIND_GLOBAL);
                for (int i = 0; i < fd.getNumFields(); ++i) {
                    allVarNames.add(fd.getName(i));
                }
            }
            p.accept(allnames);
        }
    }

    /**
     * This function computes the NewFunInfo for a given nested function. It's
     * main job is to identify variables that will have to be threaded through
     * the closure.
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
            symtab.registerVar(decl.getDecl().getName(), TypeFunction.singleton, decl, SymbolTable.KIND_LOCAL);
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
                // If name is a hoisted function, consider it will be called,
                // and
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


                VarInfo vi = InnerFunReplacer.this.symtab().lookupVarInfo(exp.getName());
                Type pt = null;
                if (vi != null) {
                    pt = vi.type;
                }
                if (pt == null) {
                    return exp;
                }

                if (isInParam) {
                    throw new ExceptionAtNode("You cannot use a captured variable in an array length expression: " + exp, exp);
                }
                int kind = vi.kind;
                if (kind == SymbolTable.KIND_GLOBAL || vi.type instanceof TypeFunction) {
                    return exp;
                }

                pt = normalizeType(pt);

                TreeSet<String> oldDependent = dependent;
                ParamInfo info = this.nfi.paramsToAdd.get(name);
                if (info == null) {
                    dependent = new TreeSet<String>();
                    this.nfi.paramsToAdd.put(name, new ParamInfo(pt, name, makeUnique(name, vi.origin), isAssignee, dependent));
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

        private Type normalizeType(Type pt) {
            if (pt instanceof TypeStructRef) {
                String lname = ((TypeStructRef) pt).getName();
                if (nres.isTemplate(lname)) {
                    nfi.typeParamsToAdd.add(((TypeStructRef) pt).getName());
                } else {
                    TypeStructRef tsr = (TypeStructRef) pt;
                    if (tsr.hasTypeParams()) {
                        List<Type> newt = new ArrayList<Type>();
                        boolean changed = false;
                        for (Type tp : tsr.getTypeParams()) {
                            Type normalized = normalizeType(tp);
                            newt.add(normalized);
                            if (normalized != tp) {
                                changed = true;
                            }
                        }
                        if (!changed) {
                            newt = tsr.getTypeParams();
                        }
                        String nm = nres.getStructName(lname);
                        if (!nm.equals(lname) || changed) {
                            pt = new TypeStructRef(nm, ((TypeStructRef) pt).isUnboxed(), newt);
                        }
                    } else {
                        String nm = nres.getStructName(lname);
                        if (!nm.equals(lname)) {
                            pt = new TypeStructRef(nm, ((TypeStructRef) pt).isUnboxed());
                        }
                    }
                }
            }
            return pt;
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
            if (op == ExprUnary.UNOP_POSTDEC || op == ExprUnary.UNOP_POSTINC || op == ExprUnary.UNOP_PREDEC || op == ExprUnary.UNOP_PREINC) {
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
                if (nres.isTemplate(pt.toString())) {
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

            Function fun = null;
            VarInfo vi = InnerFunReplacer.this.symtab().lookupVarInfo(name);
            if (vi != null && vi.kind == SymbolTable.KIND_LOCAL_FUNCTION) {
                fun = (Function) vi.origin;
            } else {
                fun = nres.getFun(name);
            }

            // NOTE the function passed to funInfo() is not in
            // extractedInnerFuns
            // yet, so it will not be inlined here, which is the correct
            // behavior.
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
                // why is this reasonable? because you cannot have cyclic
                // relation
                // (there's strict order for inner function)
                // you might ask, later will will propagate information along
                // call
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
                if (vi == null || vi.kind != SymbolTable.KIND_FUNC_PARAM) {
                    Type t = this.symtab.lookupVar(efc.getName(), efc);
                    if (t == null || (!(t instanceof TypeFunction))) {
                        throw new ExceptionAtNode("Function " + efc.getName() + " has not been defined when used", efc);
                    }
                }
                // at this moment, we don't know about fun's signature,
                // so we assume that all arguments might be changed for
                // soundness
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
                } else {
                    Expression e = existingArgs.get(i - starti);
                    if (e instanceof ExprLambda) {
                        e.accept(this);
                    }
                }
                isAssignee = oldIsA;
            }
            return efc;
        }
    }






    public Object visitStmtVarDecl(StmtVarDecl svd) {
        final InnerFunReplacer localIFR = this;
        final TempVarGen varGen = driver.getVarGen();
        /**
         * TODO: This pass removes REGens in generators. Should be made more
         * general, or maybe it's unnecessary?
         */
        FEReplacer remREGinDecl = new FEReplacer() {
            public Object visitTypeArray(TypeArray t) {
                Type nbase = (Type) t.getBase().accept(this);
                Expression nlen = null;
                if (t.getLength() != null) {
                    if (t.getLength() instanceof ExprRegen) {
                        String nname = varGen.nextVar();
                        localIFR.addStatement((Statement) (new StmtVarDecl(t.getLength(), TypePrimitive.inttype, nname, t.getLength())).accept(localIFR));
                        nlen = new ExprVar(t.getLength(), nname);
                    } else {
                        nlen = t.getLength();
                    }
                }
                if (nbase == t.getBase() && t.getLength() == nlen)
                    return t;
                return new TypeArray(nbase, nlen, t.getMaxlength());
            }
        };

        List<Type> newTypes = new ArrayList<Type>();
        boolean changed = false;

        for (int i = 0; i < svd.getNumVars(); i++) {

            if (driver.getSymbolTable().hasVar(svd.getName(i))) {
                FEContext cx = driver.getSymbolTable().varCx(svd.getName(i));
                String prev = "";
                if (cx != null) {
                    prev = " previously declared in " + cx;
                }
                throw new ExceptionAtNode("Shadowing of variables is not allowed (" + svd.getName(i) + prev + ").", svd);
            }

            Type ot = svd.getType(i);
            Type t = (Type) ot.accept(remREGinDecl);
            if (ot != t) {
                changed = true;
            }
            newTypes.add(t);
        }
        if (!changed) {
            return svd;
        }
        return new StmtVarDecl(svd, newTypes, svd.getNames(), svd.getInits());
    }


    class PostCheck extends BidirectionalPass {
        public Object visitExprFunCall(ExprFunCall efc) {
            String oldName = efc.getName();
            String newName = oldName;
            {
                VarInfo vi = symtab().lookupVarInfo(oldName);
                if (vi != null) {
                    if (vi.kind == SymbolTable.KIND_LOCAL_FUNCTION) {
                        newName = ((Function) vi.origin).getName();
                    }
                }
            }
            List<Expression> actuals = new ArrayList<Expression>();
            for (Expression actual : efc.getParams()) {
                Expression arg = (actual);
                VarInfo vi = symtab().lookupVarInfo(actual.toString());
                if (vi != null) {
                    if (vi.kind == SymbolTable.KIND_LOCAL_FUNCTION) {
                        String argfname = ((Function) vi.origin).getName();
                        arg = new ExprVar(actual, argfname);
                    }
                }
                actuals.add(arg);
            }
            return new ExprFunCall(efc, newName, actuals, efc.getTypeParams());
        }
    }

    public BidirectionalPass getPostPass() {
        return new PostCheck();
    }


    public Object visitStmtFunDecl(StmtFunDecl sfd) {


        String pkg = nres().curPkg().getName();
        String oldName = sfd.getDecl().getName();
        String newName = oldName + (++nfcnt);
        while (nres().getFun(newName) != null) {
            newName = oldName + (++nfcnt);
        }

        if (symtab().hasVar(oldName)) {
            throw new ExceptionAtNode("Shadowing of variables is not allowed " + oldName + " ", sfd);
        }

        Function f = sfd.getDecl();

        for (Parameter pr : sfd.getDecl().getParams()) {
            if (symtab().hasVar(pr.getName())) {
                throw new ExceptionAtNode("Shadowing of variables is not allowed:" + pr, sfd);
            }
        }

        if (tdstate().isInGenerator() && !f.isGenerator()) {
            throw new ExceptionAtNode("You can not define a non-generator function inside a generator", sfd);
        }

        if (f.isSketchHarness()) {
            throw new ExceptionAtNode("You can not define a harness inside another function", sfd);
        }

        List<String> parentTpList = tdstate().getCurrentFun().getTypeParams();
        Set<String> parentTp = new HashSet<String>(parentTpList);

        for (String tp : f.getTypeParams()) {
            if (parentTp.contains(tp)) {
                throw new ExceptionAtNode("Shadowing of type parameters is not allowed " + tp, sfd);
            }
        }

        if (f.isGeneric()) {
            parentTpList = new ArrayList<String>(parentTpList);
            parentTpList.addAll(f.getTypeParams());
        }

        Function newFun = f.creator().name(newName).pkg(pkg).typeParams(parentTpList).create();
        nres().registerFun(newFun);

        symtab().registerVar(oldName, TypeFunction.singleton, newFun, SymbolTable.KIND_LOCAL_FUNCTION);

        driver.addClosure(newFun.getFullName(), symtab().shallowClone());
        NewFunInfo nfi = funInfo(newFun);

        if (!driver.needsSpecialization(newFun)) {
            newFun = driver.doFunction(newFun);
            nres().reRegisterFun(newFun);
        }

        addFunction(newFun);
        /*
         * if (nfi.typeParamsToAdd.size() > 0) {
         * newFun.getTypeParams().addAll(nfi.typeParamsToAdd); }
         */
        extractedInnerFuns.put(nfi.funName, nfi);
        return null;
    }


    NewFunInfo funInfo(Function f) {
        // get the new function info
        // i.e. the used variables that are in f's containing function
        // among the used variables, some are modified, we also track if a used
        // var is
        // changed. curFun is the function that lexically encloses f.
        final String theNewFunName = nres().getFunName(f.getName());
        final NewFunInfo nfi = new NewFunInfo(theNewFunName, nres().getFunName(tdstate().getCurrentFun().getName()));
        SymbolTableVisitor stv = new LocalFunctionAnalyzer(null, nfi);
        stv.setNres(nres());
        f.accept(stv);
        return nfi;
    }

    public Object visitProgram(Program p) {

        // Register the global variables of the program in the hoister
        registerGlobals(p);

        return p;
    }

} // end of InnerFunReplacer

