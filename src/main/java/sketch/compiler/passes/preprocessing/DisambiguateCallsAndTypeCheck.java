package sketch.compiler.passes.preprocessing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import static sketch.util.DebugOut.printError;

import sketch.compiler.ast.core.FEContext;
import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.FieldDecl;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.NameResolver;
import sketch.compiler.ast.core.Package;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.SymbolTable;
import sketch.compiler.ast.core.exprs.*;
import sketch.compiler.ast.core.exprs.ExprArrayRange.RangeLen;
import sketch.compiler.ast.core.exprs.regens.ExprAlt;
import sketch.compiler.ast.core.exprs.regens.ExprChoiceBinary;
import sketch.compiler.ast.core.exprs.regens.ExprChoiceSelect;
import sketch.compiler.ast.core.exprs.regens.ExprChoiceSelect.SelectChain;
import sketch.compiler.ast.core.exprs.regens.ExprChoiceSelect.SelectField;
import sketch.compiler.ast.core.exprs.regens.ExprChoiceSelect.SelectOrr;
import sketch.compiler.ast.core.exprs.regens.ExprChoiceSelect.SelectorVisitor;
import sketch.compiler.ast.core.exprs.regens.ExprChoiceUnary;
import sketch.compiler.ast.core.stmts.*;
import sketch.compiler.ast.core.typs.NotYetComputedType;
import sketch.compiler.ast.core.typs.StructDef;
import sketch.compiler.ast.core.typs.StructDef.StructFieldEnt;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.ast.core.typs.TypeComparisonResult;
import sketch.compiler.ast.core.typs.TypeFunction;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.ast.core.typs.TypeStructRef;
import sketch.compiler.passes.lowering.SymbolTableVisitor;
import sketch.util.ControlFlowException;
import sketch.util.exceptions.ExceptionAtNode;
import sketch.util.exceptions.UnrecognizedVariableException;

public class DisambiguateCallsAndTypeCheck extends SymbolTableVisitor {

    public DisambiguateCallsAndTypeCheck() {
        super(null);
    }

    public boolean good = true;

    protected void report(FENode node, String message) {
        (new ExceptionAtNode(message, node)).printNoStacktrace();
        good = false;
    }

    protected void report(FEContext ctx, String message) {
        good = false;
        printError(ctx + ":", message);
    }

    /** Report incompatible alternative field selections. */
    protected void report(ExprChoiceSelect exp, Type t1, Type t2, String f1, String f2) {
        report(exp, "incompatible types '" + t1 + "', '" + t2 + "'" +
                " in alternative selections '" + f1 + "', '" + f2 + "'");
    }

    /**
     * @param localNames
     * @param streamNames
     * @param name
     * @param ctx
     */
    private void checkADupFieldName(Map<String, FEContext> localNames, String name,
            FEContext ctx, String moreMsg)
    {
        if (localNames.containsKey(name)) {
            FEContext octx = (FEContext) localNames.get(name);
            report(ctx, "Duplicate declaration of '" + name + "'");
            report(octx, "(also declared here). " + moreMsg);
        } else {
            localNames.put(name, ctx);

        }
    }

    private void propogateImmutabilityToChildren(String name) {
        List<String> children = nres.getStructChildren(name);
        for (String child : children) {
            StructDef ts = nres.getStruct(child);
            if (!ts.immutable()) {
                ts.setImmutable();

            }
            propogateImmutabilityToChildren(child);
        }

    }

    public void checkImmutability(Program prog) {

        for (Package spec : prog.getPackages()) {
            for (StructDef ts : spec.getStructs()) {
                if (ts.immutable()) {
                    propogateImmutabilityToChildren(ts.getFullName());
                }

            }
        }

    }

    private Program checkReplaceFunCall(Program prog) {
        List<Package> newStreams = new ArrayList<Package>();
        for (Package pkg : prog.getPackages()) {
            List<StmtSpAssert> newSpAssertStmts = new ArrayList<StmtSpAssert>();
            for (StmtSpAssert sa : pkg.getSpAsserts()) {
                if (true || checkSpAssert(sa, pkg.getName())) {
                    newSpAssertStmts.add(sa);
                }
            }
            newStreams.add(new Package(pkg, pkg.getName(), pkg.getStructs(),
                    pkg.getVars(), pkg.getFuncs(), newSpAssertStmts));
        }
        return prog.creator().streams(newStreams).create();
    }


    private boolean checkSpAssert(StmtSpAssert sa, String pkg) {
        // TODO: should also check that both lhs and rhs take same type of inputs and produces same type output
        ExprFunCall lhs = sa.getFirstFun();
        if (lhs.getParams().get(0) instanceof ExprFunCall) {
            // nested funcall
            Type t = getType(lhs.getParams().get(0));
            if (!t.isStruct()) report (sa.getContext(), "Not yet supported");
            String fname = nres.getFunName(lhs.getName());
            Function f = nres.getFun(fname);
            String expName = f.getParams().get(0).getName();
            CheckFunction cf =
                    new CheckFunction((TypeStructRef) t, f, expName, new SymbolTable(
                            symtab), nres, pkg);
            f.getBody().accept(cf);
            if (!cf.checkPassed()) {
                System.err.println("Optimization not applicable because of " +
                        cf.failContext());
                return false;
            }
        } 
        return true;
    }

    private class CheckFunction extends SymbolTableVisitor {
        final TypeStructRef type;
        final String fname;
        Map<String, Boolean> varsToTrack;
        boolean safe;
        boolean finalSafe;
        boolean fieldAccess;
        FEContext failContext;
        String pkg;
        public CheckFunction(TypeStructRef t, Function func, String expName,
                SymbolTable symtab, NameResolver nres, String pkg)
        {
            super(symtab);
            this.type = t;
            this.fname = func.getName();
            varsToTrack = new HashMap<String, Boolean>();
            varsToTrack.put(expName, false);
            safe = true;
            finalSafe = true;
            fieldAccess = false;
            this.nres = nres;
            this.pkg = pkg;

            for (Parameter par : func.getParams()) {
                this.symtab.registerVar(par.getName(), par.getType(), par,
                        SymbolTable.KIND_FUNC_PARAM);
            }
        }

        public boolean checkPassed() {
            return safe && finalSafe;
        }

        public FEContext failContext() {
            return failContext;
        }

        @Override
        public Object visitStmtAssign(StmtAssign stmt) {
            if (!safe || !finalSafe)
                return stmt;
            super.visitStmtAssign(stmt);
            safe = true;
            finalSafe = true;
            fieldAccess = false;
            Expression rhs = stmt.getRHS().doExpr(this);
            Expression lhs = stmt.getLHS();
            if (!safe) {
                // unsafe expr can only be assigned to a var
                if ((lhs instanceof ExprVar)) {
                    varsToTrack.put(((ExprVar) lhs).getName(), fieldAccess);
                    safe = true;
                    fieldAccess = false;
                } else {
                    finalSafe = false;
                    failContext = stmt.getContext();
                }
            } 
            return stmt;
        }

        @Override
        public Object visitStmtVarDecl(StmtVarDecl stmt) {
            if (!safe || !finalSafe)
                return stmt;
            super.visitStmtVarDecl(stmt);
            safe = true;
            finalSafe = true;
            fieldAccess = false;
            int n = stmt.getNumVars();
            for (int i = 0; i < n; i++) {
                if (!safe || !finalSafe)
                    return stmt;
                fieldAccess = false;
                if (stmt.getInit(i) != null) {
                    Expression init = stmt.getInit(i).doExpr(this);
                    if (!safe) {
                        varsToTrack.put(stmt.getName(i), fieldAccess);
                        safe = true;
                        fieldAccess = false;
                    }
                }
            }
            return stmt;
        }

        @Override
        public Object visitStmtSwitch(StmtSwitch stmt) {
            ExprVar var = (ExprVar) stmt.getExpr();
            if (varsToTrack.containsKey(var.getName()) && varsToTrack.get(var.getName()))
            {
                // switch on a field access is bad
                safe = false;
                finalSafe = false;
                return stmt;
            }
            List<String> cases = new ArrayList<String>();
            for (String caseExpr : stmt.getCaseConditions()) {
                if (caseExpr != "default" && caseExpr != "repeat") {
                    cases.add(caseExpr);
                    SymbolTable oldSymTab1 = symtab;
                    symtab = new SymbolTable(symtab);
                    symtab.registerVar(var.getName(),
                            (new TypeStructRef(caseExpr, false)).addDefaultPkg(pkg, nres));

                    Statement body = (Statement) stmt.getBody(caseExpr).accept(this);
                    symtab = oldSymTab1;
                } else {
                    if (caseExpr == "default" && varsToTrack.containsKey(var.getName()))
                    {
                        TypeStructRef tt = (TypeStructRef) (getType(var));

                        boolean hasRec = false;
                        StructDef cur = nres.getStruct(tt.getName());
                        for (StructFieldEnt f : cur.getFieldEntriesInOrder()) {
                            if (f.getType().promotesTo(type, nres)) {
                                hasRec = true;
                            }
                        }
                        if (!hasRec) {
                            List<String> defCases = findDefaultCases(cases, tt.getName());

                        for (String c : defCases) {
                            if (hasRecursiveFields(c))
                                    hasRec = true;
                            }
                        }

                        if (!hasRec) {
                            // can remove the var from unsafe var list for this case
                            varsToTrack.remove(var.getName());
                            Statement body =
                                    (Statement) stmt.getBody(caseExpr).accept(this);
                            varsToTrack.put(var.getName(), false);

                        } else {
                            Statement body =
                                    (Statement) stmt.getBody(caseExpr).accept(this);
                        }
                    } else {
                        Statement body = (Statement) stmt.getBody(caseExpr).accept(this);
                    }
                }
            }
            return stmt;
        }

        private List<String> findDefaultCases(List<String> cases, String string) {
            List<String> allCases = new ArrayList<String>();

            LinkedList<String> queue = new LinkedList<String>();
            queue.add(string);
            while (!queue.isEmpty()) {
                String n = queue.removeFirst();
                List<String> children = nres.getStructChildren(n);
                if (children.isEmpty()) {
                    allCases.add(n.split("@")[0]);
                } else {
                    queue.addAll(children);
                }
            }

            for (String c : cases) {
                allCases.remove(c);
            }
            return allCases;
        }

        @Override
        public Object visitExprBinary(ExprBinary exp) {
            if (!safe || !finalSafe)
                return exp;
            if (exp.getOp() == ExprBinary.BINOP_EQ) {
                if (exp.getLeft() instanceof ExprVar) {
                    ExprVar lvar = (ExprVar) exp.getLeft();
                    if (varsToTrack.containsKey(lvar.getName())) {
                        if (varsToTrack.get(lvar.getName())) {
                            safe = false;
                            finalSafe = false;
                        }
                    }
                } else {
                    exp.getLeft().doExpr(this);
                }
                if (!safe || !finalSafe)
                    return exp;
                if (exp.getRight() instanceof ExprVar) {
                    ExprVar rvar = (ExprVar) exp.getRight();
                    if (varsToTrack.containsKey(rvar.getName())) {
                        if (varsToTrack.get(rvar.getName())) {
                            safe = false;
                            finalSafe = false;
                        }
                    }
                } else {
                    exp.getRight().doExpr(this);
                }
                return exp;
            }

            return super.visitExprBinary(exp);
        }

        @Override
        public Object visitExprField(ExprField exp) {
            if (!safe || !finalSafe)
                return exp;
            Type t = getType(exp);
            if (!t.promotesTo(type, nres)) {
                safe = true;
                return exp;
            }
            super.visitExprField(exp);
            if (!safe)
                fieldAccess = true;
            return exp;
        }

        @Override
        public Object visitExprVar(ExprVar exp) {
            if (!safe || !finalSafe)
                return exp;

            if (varsToTrack.containsKey(exp.getName())) {
                TypeStructRef t = (TypeStructRef) (getType(exp));
                if (varsToTrack.get(exp.getName()) || hasRecursiveFields(t.getName())) {
                    safe = false;
                    failContext = exp.getContext();
                }
            }
            return exp;
        }

        private boolean hasRecursiveFields(String name) {
            LinkedList<String> queue = new LinkedList<String>();
            queue.addLast(name);
            while (!queue.isEmpty()) {
                String n = queue.removeFirst();
                StructDef cur = nres.getStruct(n);
                for (StructFieldEnt f : cur.getFieldEntriesInOrder()) {
                    if (f.getType().promotesTo(type, nres)) {
                        return true;
                    }
                }

                queue.addAll(nres.getStructChildren(n));
            }
            return false;
        }

        @Override
        public Object visitExprFunCall(ExprFunCall exp) {
            if (!safe || !finalSafe)
                return exp;
            if (exp.getName().equals(fname)) {
                List<Expression> params = exp.getParams();
                // first params can be unsafe
                for (int i = 1; i < params.size(); i++) {
                    params.get(i).doExpr(this);
                    if (!safe) {
                        finalSafe = false;
                        failContext = exp.getContext();
                    }
                }
            } else {
                super.visitExprFunCall(exp);
                if (!safe) {
                    finalSafe = false;
                    failContext = exp.getContext();
                }
            }
            return exp;
        }
    }

    /**
     * Checks that no structures have duplicated field names. In particular, a field in a
     * structure or filter can't have the same name as another field in the same structure
     * or parent structure or filter. Also checks that no mutable structs are fields in an
     * immutable struct.
     * 
     * @param prog
     *            parsed program object to check
     * @param streamNames
     *            map from top-level stream and structure names to FEContexts in which
     *            they are defined
     */
    public void checkDupFieldNames(Program prog) {
        // System.out.println("checkDupFieldNames");

        for (Package spec : prog.getPackages()) {

            Map<String, FEContext> structNames = new HashMap<String, FEContext>();
            for (StructDef ts : spec.getStructs()) {
                checkADupFieldName(structNames, ts.getName(), ts.getContext(),
                        "Two structs in the same package can't share a name.");
                Map<String, FEContext> fieldNames = new HashMap<String, FEContext>();
                StructDef current = ts;
                Set<String> checkRepeats = new HashSet<String>();
                while (current != null) {
                    for (Entry<String, Type> entry : current) {
                        if (ts.immutable()) {
                            if (entry.getValue().isStruct()) {
                                TypeStructRef tt = (TypeStructRef) entry.getValue();
                                StructDef fieldStruct = nres.getStruct(tt.getName());
                                if (!fieldStruct.immutable()) {
                                    report(ts.getContext(),
                                            "Mutable structs are not allowed in immutable structs");
                                }
                            }
                        }
                        checkADupFieldName(fieldNames, entry.getKey(),
                                current.getContext(),
                                "Two fields in the same struct can't share a name.");
                        if (entry.getKey().equals(current.getName())) {
                            report(ts.getContext(),
                                    "Field can not have the same name as class: '" +
                                            entry.getKey() + "'");
                        }
                    }
                    String cn = current.getFullName();
                    if (checkRepeats.contains(cn)) {
                        throw new ExceptionAtNode(
                                "Cycle in subtyping relation involving struct type " + cn,
                                current);
                    }
                    checkRepeats.add(cn);
                    String parent = current.getParentName();
                    if (parent != null) {
                        current = nres.getStruct(parent);
                    } else {
                        current = null;
                    }
                }
            }
            // ADT
            // To check for parent structs.
            for (StructDef ts : spec.getStructs()) {
                String pname = ts.getParentName();
                if (pname != null) {
                    pname = pname.substring(0, pname.indexOf('@'));
                    if (!structNames.containsKey(pname)) {
                        report(ts.getContext(),
                                "Parent struct must exist and be defined within the same package");
                    }
                }
            }

            for (FieldDecl field : spec.getVars()) {
                for (int i = 0; i < field.getNumFields(); i++)
                    checkADupFieldName(structNames, field.getName(i), field.getCx(),
                            "Global variables, structs and functions can not share the same name.");
            }
            for (Function func : spec.getFuncs()) {
                // Some functions get alternate names if their real
                // name is null:
                String name = func.getName();
                if (name == null) {
                    report(func, "Functions must have names");
                }
                if (name != null)
                    checkADupFieldName(structNames, name, func.getCx(),
                            "Global variables, structs and functions can not share the same name.");
            }
        }
    }

    /**
     * @param map
     * @param name
     * @param ctx
     */
    private void checkAPackageName(Map<String, FEContext> map, String name, FEContext ctx)
    {
        if (map.containsKey(name)) {
            FEContext octx = (FEContext) map.get(name);
            report(ctx, "Multiple declarations of '" + name + "'");
            report(octx, "as a stream or structure");
        } else {
            map.put(name, ctx);
        }
    }

    /**
     * Checks that the provided program does not have duplicate names of structures or
     * streams.
     * 
     * @param prog
     *            parsed program object to check
     * @returns a map from structure names to <code>FEContext</code> objects showing where
     *          they are declared
     */
    public Map checkPackageNames(Program prog) {
        // maps names to FEContexts
        Map<String, FEContext> names = new HashMap<String, FEContext>();

        // System.out.println("checkStreamNames");

        // Add built-in streams:
        FEContext ctx = new FEContext("<built-in>");
        names.put("Identity", ctx);
        names.put("FileReader", ctx);
        names.put("FileWriter", ctx);

        for (Iterator iter = prog.getPackages().iterator(); iter.hasNext();) {
            Package spec = (Package) iter.next();
            checkAPackageName(names, spec.getName(), spec.getCx());
        }

        return names;
    }

    boolean inFieldDecl = false;

    public Object visitStmtVarDecl(StmtVarDecl stmt) {
        // Check: none of the locals shadow other variables.
        curcx = stmt.getCx();
        for (int i = 0; i < stmt.getNumVars(); i++) {
            String name = stmt.getName(i);
            if (symtab.hasVar(name)) {
                report(stmt, "Shadowing of variables is not allowed");
            }
        }
        return TcheckStmtVarDecl(stmt);
    }

    public Object visitExprVar(ExprVar ev) {
        if (inFieldDecl) {
            report(ev,
                    "Constant integers and regex choices of constant integers are the only global variables you can use as initializers to other global variables.");
        }
        return ev;
    }

    /**
     * Explicit type cast checking for ADTs
     */
    public Object visitExprTypeCast(ExprTypeCast exp) {
        Type castedType = (Type) exp.getType().accept(this);
        Expression newExpr = exp.getExpr().doExpr(this);
        if (castedType.isStruct()) {
            Type curType = getType(newExpr);
            // Make sure that curType is a super type of castedType
            if (!castedType.promotesTo(curType, nres)) {
                return new ExprNullPtr();
                // report(exp, "Invalid explicit typecasting");
            }
        }
        if (newExpr == exp.getExpr() && castedType == exp.getType())
            return exp;
        else
            return new ExprTypeCast(exp, castedType, newExpr);
    }
    public Object visitFieldDecl(FieldDecl field) {
        curcx = field.getCx();
        // check that array sizes match
        for (int i = 0; i < field.getNumFields(); i++) {
            Type type = field.getType(i);
            Expression init = field.getInit(i);
            if (type instanceof TypeArray && init != null) {
                // check that initializer is array initializer
                // (I guess it could also be conditional expression? Don't bother.)
                if (init instanceof ExprHole) {
                    // don't do anything, the star will take on whatever type it needs to
                } else if (!(init instanceof ExprArrayInit)) {
                    report(field, "array initialized to non-array type");
                } else {
                    // check that lengths match
                    Expression lengthExpr = ((TypeArray) type).getLength();
                    // only check it if we have resolved it
                    /*
                    if (lengthExpr instanceof ExprConstInt) {
                        int length = ((ExprConstInt) lengthExpr).getVal();
                        if (length != ((ExprArrayInit) init).getElements().size()) {
                            report(field, "declared array length does not match "
                                    + "array initializer");
                        }
                    }*/
                }
            }

            if (type instanceof TypeStructRef) {
                StructDef sd = nres.getStruct(((TypeStructRef) type).getName());
                if (sd == null) {
                    report(field, "Type " + type + " is ambiguous or undefined.");
                }
            }

        }
        assert !inFieldDecl;
        inFieldDecl = true;
        Object o = super.visitFieldDecl(field);
        inFieldDecl = false;
        return o;
    }

    boolean inTypeStruct = false;


    Set<String> structsWithVLAs = new HashSet<String>();
    public Object visitStructDef(StructDef ts) {

        for (Entry<String, Type> en : ts) {
            if (en.getValue() instanceof TypeFunction) {
                report(ts.getContext(),
                        "Function Types not allowed as fields in a struct");
            }
            if (en.getValue() instanceof TypeStructRef) {
                TypeStructRef tr = (TypeStructRef) en.getValue();
                if (tr.isUnboxed()) {
                    report(ts.getContext(),
                            "Temporary structures are not allowed as fields in other structures.");
                }
            }
            if (en.getValue() instanceof TypeArray) {
                TypeArray ta = (TypeArray) en.getValue();
                while (true) {
                    if (!(ta.getLength() instanceof ExprConstInt)) {
                        structsWithVLAs.add(ts.getFullName());
                        break;
                    }
                    if (ta.getBase() instanceof TypeArray) {
                        ta = (TypeArray) ta.getBase();
                    } else {
                        break;
                    }
                }
            }

        }

        boolean tmpts = inTypeStruct;
        inTypeStruct = true;
        Object o = super.visitStructDef(ts);
        inTypeStruct = tmpts;
        return o;
    }

    boolean inParamDecl = false;


    public Object visitExprBinary(ExprBinary expr) {
        // System.out.println("checkBasicTyping::SymbolTableVisitor::visitExprBinary");

        boolean isLeftArr = false;
        boolean isRightArr = false;

        Expression left = doExpression(expr.getLeft());
        Expression right = doExpression(expr.getRight());

        Type lt = getType(left);
        Type rt = getType(right);
        if (lt instanceof TypeArray) {
            lt = ((TypeArray) lt).getBase();
            isLeftArr = true;
        }
        if (rt instanceof TypeArray) {
            rt = ((TypeArray) rt).getBase();
            isRightArr = true;
        }
        if (lt != null && rt != null) {
            typecheckBinaryExpr(expr, expr.getOp(), lt, isLeftArr,
                    expr.getLeft() instanceof ExprConstInt, rt, isRightArr);
        }

        if (left == expr.getLeft() && right == expr.getRight())
            return expr;
        else
            return new ExprBinary(expr, expr.getOp(), left, right, expr.getAlias());
    }



    protected boolean hasFunctions(Expression e) {
        class funFinder extends FEReplacer {
            boolean found = false;

            public Object visitExprFunCall(ExprFunCall efc) {
                found = true;
                return efc;
            }
        }
        funFinder f = new funFinder();
        e.accept(f);
        return f.found;
    }

    boolean inTArr = false;

    public Object visitTypeArray(TypeArray ta) {
        boolean oldita = inTArr;
        inTArr = true;
        try {
            if (inTypeStruct) {
                TypeArray o = (TypeArray) super.visitTypeArray(ta);
                return o;
            }
            if (inParamDecl) {
                TypeArray o = (TypeArray) super.visitTypeArray(ta);
                Expression len = o.getLength();
                if (hasFunctions(len)) {
                    report(ta.getLength(),
                            "Array types in argument lists and return types can not have function calls in their lenght.");
                }
                return o;
            }

            return super.visitTypeArray(ta);
        } finally {
            inTArr = oldita;
        }
    }

    FEContext curcx = null;

    public Object visitTypeStructRef(TypeStructRef tr) {
        if (tr.isUnboxed()) {
            String name = nres.getStructName(tr.getName());
            if (structsWithVLAs.contains(name)) {
                report(curcx,
                        "Structures with variable length arrays can not be temporary structures.");
            }
        }
        StructDef sd = nres.getStruct(tr.getName());
        if (sd == null) {
            report(curcx, "The structure " + tr.getName() + " is undefined or ambiguous");
        }
        return tr;
    }

    public Object visitProgram(Program prog) {
        nres = new NameResolver(prog);
        checkImmutability(prog);
        checkDupFieldNames(prog);
        checkPackageNames(prog);

        Program newProg = (Program) super.visitProgram(prog);
        return checkReplaceFunCall(newProg);
    }

    public Object visitExprFunCall(ExprFunCall exp) {
        Function f;
        try {
            f = nres.getFun(exp.getName(), exp);
        } catch (UnrecognizedVariableException e) {
            report(exp, "unknown function " + exp.getName());
            throw e;
        }

        if (inTArr && !f.isGenerator()) {
            // report(exp, "Function call not allowed in array length expression.");
        }

        boolean hasChanged = false;
        List<Expression> newParams = new ArrayList<Expression>();
        List<Type> actualTypes = new ArrayList<Type>();
        for (Expression ap : exp.getParams()) {
            actualTypes.add(getType(ap));
        }
        TypeRenamer tren = SymbolTableVisitor.getRenaming(f, actualTypes, nres, null);
        int actSz = exp.getParams().size();
        int formSz = f.getParams().size();
        if (actSz > formSz) {
            throw new ExceptionAtNode("Incorrect number of parameters", exp);
        }
        int implSz = formSz - actSz;
        boolean hadImp = (implSz > 0);
        Map<String, Integer> pm = new HashMap<String, Integer>(implSz);
        Iterator<Expression> actIt = exp.getParams().iterator();
        for (Parameter formal : f.getParams()) {
            if (hadImp && formal.isImplicit()) {
                hasChanged = true;
                pm.put(formal.getName(), newParams.size());
                newParams.add(null);
                --implSz;
            } else {
                if (implSz != 0) {
                    throw new ExceptionAtNode("Incorrect number of parameters", exp);
                }
                Expression actual = actIt.next();
                if (actual instanceof ExprNamedParam) {
                    throw new ExceptionAtNode("Named function parameters not supported. ", actual);
                }

                Type formalType = tren.rename(formal.getType());
                formalType = formalType.addDefaultPkg(f.getPkg(), nres);
                Type ftt = formalType;
                Expression newParam = doExpression(actual);
                Type actType = getType(newParam).addDefaultPkg(nres.curPkg().getName(), nres);
                Type att = actType;
                while (ftt instanceof TypeArray) {
                    TypeArray ta = (TypeArray) ftt;
                    Expression actLen = null;
                    if (att instanceof TypeArray) {
                        TypeArray ata = (TypeArray) att;
                        actLen = ata.getLength();
                        att = ata.getBase();
                    } else {
                        actLen = ExprConstInt.one;
                    }
                    ftt = ta.getBase();
                    String len = ta.getLength().toString();
                    if (pm.containsKey(len)) {
                        int idx = pm.get(len);
                        if (newParams.get(idx) == null) {
                            newParams.set(idx, actLen);
                        } else {
                            addStatement(new StmtAssert(exp, new ExprBinary(newParams.get(idx), "==", actLen),
                                    exp.getCx() + ": Inconsistent array lengths for implicit parameter " + len + ".", false));
                        }
                    }
                }

                if (formal.isParameterReference() && newParam instanceof ExprField) {
                    TypeStructRef parent =
                            (TypeStructRef) getType(((ExprField) newParam).getLeft());
                    if (nres.getStruct(parent.getName()).immutable()) {
                        report(exp,
                                "Bad parameter: Field of an immutable struct cannot be a ref parameter");
                    }
                }

                if (formal.getType().isStruct()) {
                    TypeStructRef tsr = (TypeStructRef) formal.getType();
                    if (actType.isStruct()) {
                        TypeStructRef tsrActual = (TypeStructRef) actType;

                        if (tsrActual.isUnboxed() && !tsr.isUnboxed()) {
                            report(exp,
                                    "You cannot pass a Temporary Structure to a function expecting a standard structure: Formal type=" + formal + "\n Actual type=" + actType + "  " + f);
                        }

                    }
                }


                boolean typeCheck = true;
                if (newParam instanceof ExprNew) {
                    if (((ExprNew) newParam).isHole())
                        typeCheck = false;
                }
                if (newParam instanceof ExprADTHole) {
                    typeCheck = false;
                }
                if (typeCheck) {

                    if (actType == null || !actType.promotesTo(formalType, nres)) {
                        report(exp, "Bad parameter type: Formal type=" + formal + "\n Actual type=" + actType + "  " + f);
                    }
                    if (ftt instanceof TypeStructRef) {
                        TypeStructRef tref = ((TypeStructRef) ftt);
                        if (tref.getName() == null) {
                            report(exp, "Bad parameter type: Formal type=" + formal + "\n Actual type=" + actType + "  " + f);
                        }
                        if (formal.isParameterReference() && !tref.isUnboxed()) {
                            if (!tref.equals(att)) {
                                report(exp, "For ref parameters the types must match exactly: Formal type=" + formal + "\n Actual type=" + actType + "  " + f);
                            }
                        }
                    }

                }
                if (newParam instanceof ExprNamedParam) {
                    throw new ExceptionAtNode("Named function parameters not supported. ", newParam);
                }

                newParams.add(newParam);
                if (actual != newParam)
                    hasChanged = true;
            }

        }
        Map<String, Type> newTP = doCallTypeParams(exp);
        if (newTP != exp.getTypeParams()) {
            hasChanged = true;
        }
        if (!hasChanged)
            return exp;
        return new ExprFunCall(exp, exp.getName(), newParams, newTP);
    }

    public Object visitExprUnary(ExprUnary expr) {
        // System.out.println("checkBasicTyping::SymbolTableVisitor::visitExprUnary");
        expr = (ExprUnary) super.visitExprUnary(expr);
        Type ot = getType((Expression) expr.getExpr());
        boolean isArr = false;
        if (ot instanceof TypeArray) {
            ot = ((TypeArray) ot).getBase();
            isArr = true;
        }
        if (ot != null) {
            typecheckUnaryExpr(expr.getExpr(), expr.getOp(), ot);
        }

        return expr;
    }

    private Type currentFunctionReturn = null;

    public Object checkTypeFunction(Function f) {
        SymbolTable oldSymTab = symtab;
        symtab = new SymbolTable(symtab);
        boolean tmpipd = inParamDecl;
        inParamDecl = true;
        for (Parameter p : f.getParams()) {
            p.accept(this);
        }
        f.getReturnType().accept(this);
        inParamDecl = tmpipd;
        symtab = oldSymTab;

        Object o = super.visitFunction(f);
        return o;
    }

    public Object visitFunction(Function func) {
        // System.out.println("checkBasicTyping::SymbolTableVisitor::visitFunction: " +
        // func.getName());

        boolean hadTPs = !func.getTypeParams().isEmpty();
        if (hadTPs) {
            nres.pushTempTypes(func.getTypeParams());
        }
        try {
        curcx = func.getCx();
        currentFunctionReturn = func.getReturnType();

        if (func.isUninterp()) {
            Type rt = func.getReturnType();
            if ((rt instanceof TypeArray && !(((TypeArray) rt).getAbsoluteBase() instanceof TypePrimitive)) ||
                    rt instanceof TypeStructRef)
            {
                report(func, "Uninterpreted functions can not return structs. The type " +
                        rt + " is a struct.");
            }
        }

        if (func.isSketchHarness()) {

            for (Parameter f1 : func.getParams()) {
                if (f1.getType() instanceof TypeStructRef) {
                        // TODO: should only allow immutable structs
                    //report(func,
                     //       "A harness function can not have a structure or array of structures as input: " +
                        // f1);
                    return super.visitFunction(func);
                }
                if (f1.getType() instanceof TypeArray) {
                    if (((TypeArray) f1.getType()).getAbsoluteBase() instanceof TypeStructRef)
                    {
                            // report(func,
                            // "A harness function can not have a structure or array of structures as input: "
                            // +
                            // f1);
                        return super.visitFunction(func);
                    }
                }
            }
        }

        if (func.getSpecification() != null) {

            Function parent = null;

            // check spec presence
            try {
                parent = nres.getFun(func.getSpecification(), func);
            } catch (UnrecognizedVariableException e) {
                report(func, "Spec of " + func.getName() + "() not found");
                return super.visitFunction(func);
            }

            // check parameters
            Iterator formals1 = func.getParams().iterator();
            Iterator formals2 = parent.getParams().iterator();
            if (func.getParams().size() != parent.getParams().size()) {
                report(func, "Number of parameters of spec and sketch don't match:\n" +
                        parent + " vs.  " + func);
                return super.visitFunction(func);
            }

            // Vector<Pair<Parameter, Parameter>> knownEqVars =
            // new Vector<Pair<Parameter, Parameter>>();
            while (formals1.hasNext()) {
                Parameter f1 = (Parameter) formals1.next();
                Parameter f2 = (Parameter) formals2.next();

                Type f1t = f1.getType().addDefaultPkg(func.getPkg(), nres);
                Type f2t = f2.getType().addDefaultPkg(parent.getPkg(), nres);

                if (f1t.compare(f2t) == TypeComparisonResult.NEQ) {
                    report(func, "Parameters of spec and sketch don't match: " + f1 +
                            " vs. " + f2 + " (" + f1t + "!=" + f2t + ")");
                    return super.visitFunction(func);
                }

                if (f1.getType() instanceof TypeStructRef) {
                    TypeStructRef tr = (TypeStructRef) f1.getType();
                    if (!tr.isUnboxed()) {
                        report(func,
                                "A harness function can not have a structure or array of structures as input: " +
                                        f1);
                        return super.visitFunction(func);
                    }
                }
                if (f1.getType() instanceof TypeArray) {
                    if (((TypeArray) f1.getType()).getAbsoluteBase() instanceof TypeStructRef)
                    {
                        report(func,
                                "A harness function can not have a structure or array of structures as input: " +
                                        f1);
                        return super.visitFunction(func);
                    }
                }
            }

            // check return value
            Type frt = func.getReturnType().addDefaultPkg(func.getPkg(), nres);
            Type prt = parent.getReturnType().addDefaultPkg(parent.getPkg(), nres);
            if (!frt.equals(prt)) {
                report(func, "Return type of sketch & function are not compatible: " +
                        frt + " vs. " + prt);
                return super.visitFunction(func);
            }
            if (func.getReturnType() instanceof TypeStructRef) {
                TypeStructRef tr = (TypeStructRef) func.getReturnType();
                if (!tr.isUnboxed()) {
                    report(func,
                            "A function with an implements modifier can not return a reference. Return type = " +
                                    tr);
                }
            }
        }

        hasReturn = false;
        Object tmp = checkTypeFunction(func);
        if (!hasReturn && !func.getReturnType().equals(TypePrimitive.voidtype) &&
                !func.isUninterp())
        {
            report(func,
                    "The function " + func.getName() +
                    " doesn't have any return statements. It should return an " +
                    func.getReturnType());
        }
        return tmp;
        } finally {
            if (hadTPs) {
                nres.popTempTypes();
            }
        }
    }

    public boolean hasReturn;



    protected void typecheckUnaryExpr(Expression expr, int op, Type ot) {
        Type bittype = TypePrimitive.bittype;

        switch (op) {
            case ExprUnary.UNOP_NEG:
                if (!(ot.promotesTo(TypePrimitive.inttype, nres) || ot.promotesTo(
                        TypePrimitive.doubletype, nres)))
                {
                    report(expr, "can only negate ints and floats/doubles, not " + ot);
                }
                break;

            case ExprUnary.UNOP_BNOT:
                // you can negate a bit, since 0 and 1
                // literals always count as bits.
                // However, the resulting negation will be
                // an int.
                if (!bittype.promotesTo(ot, nres))
                    report(expr, "cannot bitwise negate " + ot);
                break;

            case ExprUnary.UNOP_NOT:
                if (!ot.promotesTo(bittype, nres))
                    report(expr, "cannot take boolean not of " + ot);
                break;

            case ExprUnary.UNOP_PREDEC:
            case ExprUnary.UNOP_PREINC:
            case ExprUnary.UNOP_POSTDEC:
            case ExprUnary.UNOP_POSTINC:
                if (!expr.isLValue())
                    report(expr, "increment/decrement of non-lvalue");
                // same as negation, regarding bits
                if (!bittype.promotesTo(ot, nres))
                    report(expr, "cannot perform ++/-- on " + ot);
                break;
        }
    }

    private void typecheckBinaryExpr(FENode expr, int op, Type lt, boolean isLeftArr,
            boolean isLeftConst, Type rt, boolean isRightArr)
    {
        // Already failed for some other reason
        if (lt == null || rt == null)
            return;

        Type ct = lt.leastCommonPromotion(rt, nres);
        if (op == ExprBinary.BINOP_LSHIFT || op == ExprBinary.BINOP_RSHIFT) {
            if (lt instanceof TypeArray) {
                if (!(((TypeArray) lt).getBase() instanceof TypePrimitive)) {
                    report(expr, "You can only shift arrays of primitives.");
                    return;
                }
                ct = lt;
            } else {
                ct = new TypeArray(lt, ExprConstInt.one);
            }
        }
        Type floattype = TypePrimitive.floattype;
        if (ct == null) {
            report(expr, "incompatible types in binary expression: " + lt + " and " + rt +
                    " are incompatible.");
            return;
        }
        // Check whether ct is an appropriate type.
        switch (op) {
            // Arithmetic operations:
            case ExprBinary.BINOP_DIV:
            case ExprBinary.BINOP_MUL:
            case ExprBinary.BINOP_SUB:
                if (isLeftArr || isRightArr) {
                    report(expr,
                            "Except for bit-vector addition, arithmetic on array types is not supported." +
                                    ct);
                }
            case ExprBinary.BINOP_ADD:
                if (!(ct.promotesTo(TypePrimitive.doubletype, nres) || ct.promotesTo(
                        TypePrimitive.inttype, nres)))
                    report(expr, "cannot perform arithmetic on " + ct);
                if (isLeftArr || isRightArr) {
                    if (!ct.equals(TypePrimitive.bittype)) {
                        report(expr, "Array addition only works for bit-vectors.");
                    }
                }
                break;

                // Bitwise and integer operations:
            case ExprBinary.BINOP_BAND:
            case ExprBinary.BINOP_BOR:
            case ExprBinary.BINOP_BXOR:
                if (!ct.promotesTo(TypePrimitive.bittype, nres))
                    report(expr, "cannot perform bitwise operations on " + ct);
                break;

            case ExprBinary.BINOP_MOD:
                if (!ct.promotesTo(TypePrimitive.inttype, nres))
                    report(expr, "cannot perform % on " + ct);
                break;

                // Boolean operations:
            case ExprBinary.BINOP_AND:
            case ExprBinary.BINOP_OR:
                if (!ct.promotesTo(TypePrimitive.bittype, nres))
                    report(expr, "cannot perform boolean operations on " + ct);
                break;

                // Comparison operations:
            case ExprBinary.BINOP_GE:
            case ExprBinary.BINOP_GT:
            case ExprBinary.BINOP_LE:
            case ExprBinary.BINOP_LT:
                if (!ct.promotesTo(floattype, nres) &&
                        !ct.promotesTo(TypePrimitive.inttype, nres))
                    report(expr, "cannot compare non-real type " + ct);
                if (isLeftArr || isRightArr)
                    report(expr, "Comparissons are not supported for array types" + expr);
                break;

                // Equality, can compare anything:
            case ExprBinary.BINOP_EQ:
            case ExprBinary.BINOP_NEQ:
                break;
                // TODO: Make correct rule for SELECT.
            case ExprBinary.BINOP_SELECT:
                break;

            case ExprBinary.BINOP_LSHIFT:
            case ExprBinary.BINOP_RSHIFT:
                if (!isLeftArr && !isLeftConst)
                    report(expr, "Can only shift array types for now. " + ct);
                break;
                // And now we should have covered everything.
            case ExprBinary.BINOP_TEQ:
                checkTripleEquals(expr, lt, rt);
                break;
            default:
                report(expr, "semantic checker missed a binop type");
                break;
        }

        // return expr;
    }

    private void checkTripleEquals(FENode expr, Type lt, Type rt) {
		if (!(lt.equals(TypePrimitive.nulltype) || lt.isStruct())
				|| !(rt.equals(TypePrimitive.nulltype) || rt.isStruct()))
            report(expr, "Triple equals only operates on structs");
        if (! lt.promotesTo(rt, nres) && ! rt.promotesTo(lt, nres))
            report(expr, "Triple equals operates on same struct types");
    }

    public Object visitExprChoiceBinary(ExprChoiceBinary exp) {
        exp = (ExprChoiceBinary) super.visitExprChoiceBinary(exp);
        Expression left = exp.getLeft(), right = exp.getRight();
        boolean isLeftArr = false;
        boolean isRightArr = false;
        Type lt = getType((Expression) left.accept(this));
        Type rt = getType((Expression) right.accept(this));
        if (lt instanceof TypeArray) {
            lt = ((TypeArray) lt).getBase();
            isLeftArr = true;
        }
        if (rt instanceof TypeArray) {
            rt = ((TypeArray) rt).getBase();
            isRightArr = true;
        }

        // TODO: this type check is lazy in that it doesn't respect the
        // associativity and precedence of the operations in 'exp'.
        List<Integer> ops = exp.opsAsExprBinaryOps();
        for (int op : ops)
            typecheckBinaryExpr(exp, op, lt, isLeftArr, left instanceof ExprConstInt, rt,
                    isRightArr);

        return exp;
    }

    public Object visitStmtAssert(StmtAssert stmt) {
        Object result = super.visitStmtAssert(stmt);

        // check that the associated condition is promotable to a boolean
        Type ct = getType(stmt.getCond());
        Type bt = TypePrimitive.bittype;

        if (!ct.promotesTo(bt, nres))
            report(stmt, "assert must be passed a boolean");

        return result;
    }

    // Control Statements

    public Object visitStmtDoWhile(StmtDoWhile stmt) {
        // check the condition
        stmt = (StmtDoWhile) super.visitStmtDoWhile(stmt);
        Type cond = getType(stmt.getCond());
        if (!cond.promotesTo(TypePrimitive.bittype, nres))
            report(stmt, "Condition clause is not a promotable to a bit");

        // should really also check whether any variables are modified in the loop body

        return stmt;
    }

    private List<String> getAllChildren(String p) {
        List<String> children = new ArrayList<String>();
        for (String child : nres.getStructChildren(p)) {
            children.add(child);
            children.addAll(getAllChildren(child));
        }
        return children;
    }

    // ADT
    private boolean isExhaustive(List<String> children, LinkedList<String> cases) {
        if (children == null || children.isEmpty()) {
            return false;
        }
        Set<String> cset = new HashSet<String>();
        for (String t : cases) {
            if (cset.contains(t)) {
                return false;
            }
            cset.add(t);
        }

        boolean isExhaustive = true;
        for (String child : children) {
            if (cset.contains(child.split("@")[0])) {
                // check for mutually exclusive i.e. cases should not contain any children
                // of child

                for (String c : getAllChildren(child)) {
                    if (cases.contains(c.split("@")[0]))
                        return false;

                }
            }
            else {
                if (!isExhaustive(nres.getStructChildren(child), cases)) {
                    return false;
                }
            }
        }

        return isExhaustive;
    }

    private boolean checkCaseExpr(String caseExpr, List<String> children) {
        List<String> childrenWOpackage = new ArrayList();
        for (String c : children) {
            childrenWOpackage.add(c.split("@")[0]);
        }
        if (childrenWOpackage == null || childrenWOpackage.isEmpty()) {
            return false;
        }

        if (!childrenWOpackage.contains(caseExpr)) {
            for (String child : children) {
                if (checkCaseExpr(caseExpr, nres.getStructChildren(child))) {
                    return true;

                }
            }

        } else {
            return true;
        }

        return false;

    }

    // ADT
    public Object visitStmtSwitch(StmtSwitch stmt) {
        ExprVar var = (ExprVar) stmt.getExpr().accept(this);

        StmtSwitch newStmt = new StmtSwitch(stmt.getContext(), var);
        // Exhaustive cases
        if (!symtab.lookupVar(var).isStruct()) {
            report(stmt, "ExprVar in switch statement must be of type struct");
        }
        TypeStructRef tres = (TypeStructRef) (symtab.lookupVar(var));
        // NOTE xzl: need to add the package suffix of the matched var to the case types
        String pkg = nres.getStruct(tres.getName()).getPkg();

        String curName = tres.getName();
        if (nres.isTemplate(curName)) {
            return super.visitStmtSwitch(stmt);
        }
        SymbolTable oldSymTab = symtab;
        symtab = new SymbolTable(symtab);

        // String parentName = curName;
        // while (parentName != null) {
        // curName = parentName;
        // parentName = nres.getStructParentName(parentName);
        // }
        List<String> children = nres.getStructChildren(curName);

        if (children == null || children.isEmpty()) {
            report(stmt, "Pattern matching on variable " + var + " of type " + tres +
                    " but that type has no variants");
        }
        if (!stmt.getCaseConditions().contains("default") &&
                !stmt.getCaseConditions().contains("repeat"))
        {
            if (!isExhaustive(children, stmt.getCaseConditions())) {
                report(stmt, "Switch cases must be exclusive and exhaustive");
            }
        }


        // visit each case body
        for (String caseExpr : stmt.getCaseConditions()) {
            if (!("default".equals(caseExpr) || "repeat".equals(caseExpr))) {
                if (!checkCaseExpr(caseExpr, children)) {
                    report(stmt, "Case must be a variant of the type " + tres);
                }
                SymbolTable oldSymTab1 = symtab;
                symtab = new SymbolTable(symtab);
                symtab.registerVar(var.getName(),
                        new TypeStructRef(caseExpr, false).addDefaultPkg(pkg, nres));

                Statement body = (Statement) stmt.getBody(caseExpr).accept(this);
                newStmt.addCaseBlock(caseExpr, body);
                symtab = oldSymTab1;
            } else {
                Statement body = (Statement) stmt.getBody(caseExpr).accept(this);
                newStmt.addCaseBlock(caseExpr, body);
            }
        }

        symtab = oldSymTab;
        return newStmt;

    }

    public Object visitStmtFor(StmtFor stmt) {
        SymbolTable oldSymTab = symtab;
        symtab = new SymbolTable(symtab);
        Statement newInit = null;
        if (stmt.getInit() == null) {
            report(stmt, "For loops without initializer not supported.");
        } else {
            newInit = (Statement) stmt.getInit().accept(this);
        }
        Expression newCond = doExpression(stmt.getCond());

        Type cond = getType(newCond);
        if (!cond.promotesTo(TypePrimitive.bittype, nres))
            report(stmt, "Condition clause is not a proper conditional");

        Statement newIncr = null;
        if (stmt.getIncr() != null) {
            newIncr = (Statement) stmt.getIncr().accept(this);
        }
        Statement tmp = stmt.getBody();
        Statement newBody = StmtEmpty.EMPTY;
        if (tmp != null) {
            newBody = (Statement) tmp.accept(this);
        }
        symtab = oldSymTab;
        if (newInit == stmt.getInit() && newCond == stmt.getCond() &&
                newIncr == stmt.getIncr() && newBody == stmt.getBody())
            return stmt;
        return new StmtFor(stmt, newInit, newCond, newIncr, newBody, stmt.isCanonical());
    }

    private boolean isIncrByOne(Statement incr) {
        if (incr instanceof StmtAssign) {
            StmtAssign sa = (StmtAssign) incr;
            String indName = sa.getLHS().toString();

            if (!(sa.getRHS() instanceof ExprBinary)) {
                return false;
            }
            ExprBinary rhsbin = (ExprBinary) sa.getRHS();

            Integer rhsrhs = rhsbin.getRight().getIValue();
            if (!(rhsbin.getOp() == ExprBinary.BINOP_ADD || rhsbin.getOp() == ExprBinary.BINOP_SUB) ||
                    rhsrhs == null ||
                    rhsrhs != 1 ||
                    !rhsbin.getLeft().toString().equals(indName))
            {
                return false;
            }
        } else {
            if (incr instanceof StmtExpr) {
                StmtExpr se = (StmtExpr) incr;
                if (se.getExpression() instanceof ExprUnary &&
                        (((ExprUnary) se.getExpression()).getOp() == ExprUnary.UNOP_POSTINC ||
                        ((ExprUnary) se.getExpression()).getOp() == ExprUnary.UNOP_PREINC ||
                        ((ExprUnary) se.getExpression()).getOp() == ExprUnary.UNOP_PREDEC || ((ExprUnary) se.getExpression()).getOp() == ExprUnary.UNOP_POSTDEC))
                {

                } else {
                    return false;
                }
            } else {
                return false;
            }
        }
        return true;
    }

    public Object visitStmtIfThen(StmtIfThen stmt) {
        // check the condition
        stmt = (StmtIfThen) super.visitStmtIfThen(stmt);
        Type cond = getType(stmt.getCond());
        if (!cond.promotesTo(TypePrimitive.bittype, nres))
            report(stmt, "Condition clause is not a proper conditional");

        return stmt;
    }

    public Object visitStmtLoop(StmtLoop stmt) {
        // variable in loop should promote to an int
        stmt = (StmtLoop) super.visitStmtLoop(stmt);
        Type cond = getType(stmt.getIter());
        if (!cond.promotesTo(TypePrimitive.inttype, nres))
            report(stmt, "Iteration count is not convertable to an integer");

        return (stmt);
    }

    public Object visitExprAlt(ExprAlt ea) {
        Type lt = getType((Expression) ea.getThis().accept(this));
        Type rt = getType((Expression) ea.getThat().accept(this));

        if (lt != null && rt != null && null == lt.leastCommonPromotion(rt, nres))
            report(ea, "alternatives have incompatible types '" + lt + "', '" + rt + "'");
        return ea;
    }


    public Object visitExprChoiceSelect(ExprChoiceSelect exp) {
        exp = (ExprChoiceSelect) super.visitExprChoiceSelect(exp);
        final ExprChoiceSelect e = exp;
        class SelectorTypeChecker extends SelectorVisitor {
            StructDef base;

            SelectorTypeChecker(StructDef base) {
                this.base = base;
            }

            public Object visit(SelectField sf) {
                String f = sf.getField();
                if (f.equals("")) {
                    return new NotYetComputedType();
                }
                StructDef current = base;
                boolean err = true;
                while (current.getParentName() != null) {
                    if (current.hasField(f)) {
                        err = false;
                        break;
                    } else {
                        current = nres.getStruct(current.getParentName());
                    }
                }
                if (err && !current.hasField(f)) {
                    report(e, "struct " + base.getName() + " has no field '" + f + "'");
                    throw new ControlFlowException("selcheck");
                }
                return current.getType(f);
            }

            public Object visit(SelectOrr so) {
                Type t1 = (Type) so.getThis().accept(this);
                Type t2 = (Type) so.getThat().accept(this);
                Type rt = t1.leastCommonPromotion(t2, nres);

                if (null == rt) {
                    report(e, t1, t2, so.getThis().toString(), so.getThat().toString());
                    throw new ControlFlowException("selcheck");
                }

                if (null != rt &&
                        (so.getThis().isOptional() || so.getThat().isOptional()))
                {
                    Type tmp = base.leastCommonPromotion(rt, nres);
                    if (null == tmp) {
                        report(e,
                                "not selecting '" + so.getThis() + "' or '" +
                                        so.getThat() + "' yields a type '" + base +
                                        "' that is incompatible with '" + rt + "'");
                        throw new ControlFlowException("selcheck");
                    }
                    rt = tmp;
                }

                return rt;
            }

            public Object visit(SelectChain sc) {
                Type tfn, tf, tn = null;
                StructDef oldBase = base;

                tf = (Type) sc.getFirst().accept(this);

                if (!tf.isStruct()) {
                    report(e, "selecting " + sc.getFirst() +
                            " yields a non-structure type on which" + " the selection " +
                            sc.getNext() + " was to be done");
                    throw new ControlFlowException("selcheck");
                }

                if (sc.getFirst().isOptional())
                    tn = (Type) sc.getNext().accept(this);

                base = nres.getStruct(((TypeStructRef) tf).getName());
                tfn = (Type) sc.getNext().accept(this);
                base = oldBase;

                Type rt = tfn;
                if (sc.getFirst().isOptional()) {
                    rt = rt.leastCommonPromotion(tn, nres);
                    if (null == rt) {
                        report(e, tfn, tn, sc.getFirst().toString() +
                                sc.getNext().toString(), "");
                        throw new ControlFlowException("selcheck");
                    }
                }
                if (sc.getNext().isOptional()) {
                    rt = rt.leastCommonPromotion(tf, nres);
                    if (null == rt) {
                        report(e, "not selecting '" + sc.getNext() + "'" +
                                " yields a type '" + tf + "' that is " +
                                " incompatible with another possible selection");
                        throw new ControlFlowException("selcheck");
                    }
                }
                if (sc.getNext().isOptional() && sc.getFirst().isOptional()) {
                    rt = base.leastCommonPromotion(rt, nres);
                    if (null == rt) {
                        report(e,
                                "not selecting both '" + sc.getFirst() + "' and '" +
                                        sc.getNext() + " yields type '" + base + "'," +
                                        " which is incompatible with selecting " +
                                " either or both");
                        throw new ControlFlowException("selcheck");
                    }
                }
                return rt;
            }
        }

        Type lt = getType(exp.getObj());

        if (!lt.isStruct()) {
            report(exp, "field reference of a non-structure type");
        } else {
            StructDef base = nres.getStruct(((TypeStructRef) lt).getName());
            Type selType = null;

            try {
                selType = (Type) exp.accept(new SelectorTypeChecker(base));
            } catch (ControlFlowException cfe) {}

            if (selType != null && exp.getField().isOptional())
                if (null == base.leastCommonPromotion(selType, nres))
                    report(exp, lt, selType, "", exp.getField().toString());
        }

        return exp;
    }



    public Object visitExprTernary(ExprTernary expr) {
        // System.out.println("checkBasicTyping::SymbolTableVisitor::visitExprTernary");
        expr = (ExprTernary) super.visitExprTernary(expr);
        Type at = getType((Expression) expr.getA().accept(this));
        Type bt = getType((Expression) expr.getB().accept(this));
        Type ct = getType((Expression) expr.getC().accept(this));

        if (at != null) {
            if (!at.promotesTo(TypePrimitive.bittype, nres))
                report(expr, "first part of ternary expression " + "must be a bit");
        }

        if (bt != null && ct != null) {
            Type xt = bt.leastCommonPromotion(ct, nres);
            if (xt == null)
                report(expr, "incompatible second and third types "
                        + "in ternary expression");
        }

        return (expr);
    }

    public Object visitExprNew(ExprNew expNew) {
        expNew = (ExprNew) super.visitExprNew(expNew);
        if (!expNew.isHole()) {
            TypeStructRef nt = (TypeStructRef) expNew.getTypeToConstruct().accept(this);
            StructDef ts = nres.getStruct(nt.getName());
            if (ts == null) {
                report(expNew, "Trying to instantiate a struct that doesn't exist");
            }
            // ADT
            if (!ts.isInstantiable()) {
                report(expNew,
                        "Struct representing an Algebraic Data Type cannot be instantiated");
            }


            for (ExprNamedParam en : expNew.getParams()) {
                Expression rhs = doExpression(en.getExpr());
                // ADT
                // Changed this to check if the parent has the field
                StructDef current = ts;
                boolean err = true;
                while (current.getParentName() != null) {
                    if (current.hasField(en.getName())) {
                        err = false;
                        break;
                    } else {
                        current = nres.getStruct(current.getParentName());
                    }
                }

                if (err && !current.hasField(en.getName()))
                    report(expNew,
                            "The struct does not have a field named " + en.getName());

                Type rhsType = getType(rhs);
                Type lhsType = current.getType(en.getName());
                if (rhsType == null || lhsType == null) {
                    return expNew;
                }
                lhsType = lhsType.addDefaultPkg(ts.getPkg(), nres);
                matchTypes(expNew, lhsType, rhsType);

            }
        }
        // TODO Do more
        return expNew;
    }

    public Object visitExprField(ExprField expr) {
        // System.out.println("checkBasicTyping::SymbolTableVisitor::visitExprField");

        Expression newLeft = (Expression) expr.getLeft().accept(this);

        Type lt = getType(newLeft);


        // Either lt is a structure type, or it's null, or it's an error.
        if (lt == null) {
            // pass
        } else if (lt instanceof TypeStructRef) {
            StructDef ts = getStructDef(lt);
            if (ts.immutable())
                expr.setIsLValue(false);
            String rn = expr.getName();
            if (!expr.isHole()) {
                boolean found = false;
                // Changed for ADT
                StructDef current = ts;
                if (current == null) {
                    return expr;
                }
                outerloop: while (current.getParentName() != null) {
                    for (Entry<String, Type> entry : current) {
                        if (entry.getKey().equals(rn)) {
                            found = true;
                            break outerloop;
                        }
                    }
                    current = nres.getStruct(current.getParentName());
                }
                for (Entry<String, Type> entry : current) {
                    if (entry.getKey().equals(rn)) {
                        found = true;
                        break;
                    }
                }

                if (!found)
                    report(expr, "structure " + ts.getFullName() +
                            " does not have a field named " + "'" + rn + "'");
            }
        } else {
            report(expr, "field reference of a non-structure type");
        }

        if (newLeft == expr.getLeft())
            return expr;
        else
            return new ExprField(expr, newLeft, expr.getName(), expr.isHole());
    }

    public Object visitExprArrayRange(ExprArrayRange expr) {
        // System.out.println("checkBasicTyping::SymbolTableVisitor::visitExprArrayRange");

        Type bt = getType((Expression) expr.getBase().accept(this));
        if (bt != null) {
            if (!(bt instanceof TypeArray))
                report(expr, "array access with a non-array base");
        } else {
            report(expr, "array access with a non-array base");
        }
        RangeLen rl = expr.getSelection();
        Type ot = getType((Expression) rl.start().accept(this));
        if (ot != null) {
            if (!ot.promotesTo(TypePrimitive.inttype, nres))
                report(expr, "array index must be an int");
        } else {
            report(expr, "array index must be an int");
        }
        return super.visitExprArrayRange(expr);
    }

    public Object visitExprArrayInit(ExprArrayInit expr) {
        // System.out.println("checkBasicTyping::SymbolTableVisitor::visitExprArrayInit");
        /*
         * // check for uniform length and dimensions among all children. List elems =
         * expr.getElements(); // only worry about it if we have elements if
         * (elems.size()>0) { Expression first = (Expression)elems.get(0); // if one is an
         * array, they should all be // arrays of the same length and dimensions if (first
         * instanceof ExprArrayInit) { ExprArrayInit firstArr = (ExprArrayInit)first; for
         * (int i=1; i<elems.size(); i++) { ExprArrayInit other =
         * (ExprArrayInit)elems.get(i); if (firstArr.getDims() != other.getDims()) {
         * report(expr, "non-uniform number of array " +
         * "dimensions in array initializer"); } if (firstArr.getElements().size() !=
         * other.getElements().size()) { report(expr, "two rows of a multi-dimensional " +
         * "array are initialized to different " +
         * "lengths (arrays must be rectangular)"); } } } else { // if first element is
         * not array, no other // element should be an array for (int i=1; i<elems.size();
         * i++) { if (elems.get(i) instanceof ExprArrayInit) { report(expr,
         * "non-uniform number of array " + "dimensions in array initializer"); } } } }
         */
        List<Expression> elems = expr.getElements();
        if (elems.size() > 0) {
            Expression first = elems.get(0);
            Type t = getType(first);
            for (int i = 1; i < elems.size(); ++i) {
                t = getType(elems.get(i)).leastCommonPromotion(t, nres);
                if (t == null) {
                    report(expr, "Inconsistent types in array initializer");
                }
            }
        }

        return super.visitExprArrayInit(expr);
    }


    public void matchTypes(FENode stmt, Type lt, Type rt) {

        if (lt != null && rt != null && !(rt.promotesTo(lt, nres)))
            report(stmt, "right-hand side of assignment must " +
                    "be promotable to left-hand side's type " + lt + "!>=" + rt);
        if (lt == null || rt == null)
            report(stmt, "This assignments involves a bad type");
    }

    public Object visitStmtAssign(StmtAssign stmt) {
        // System.out.println("checkBasicTyping::SymbolTableVisitor::visitStmtAssign");

        Expression newLHS = doExpression(stmt.getLHS());
        Expression newRHS = doExpression(stmt.getRHS());

        if (!stmt.getLHS().isLValue())
            report(stmt, "assigning to non-lvalue");
        Type lt = getType(newLHS);
        Type rt = getType(newRHS);
        String lhsn = null;
        Expression lhsExp = newLHS;

        if (lhsExp instanceof ExprArrayRange) {
            lhsExp = ((ExprArrayRange) newLHS).getBase();
        }
        if (lhsExp instanceof ExprVar) {
            lhsn = ((ExprVar) lhsExp).getName();
        }
        boolean typeCheck = true;
        if (newRHS instanceof ExprNew) {
            if (((ExprNew) newRHS).isHole())
                typeCheck = false;
        }
        if (newRHS instanceof ExprADTHole) {
            typeCheck = false;
        }
        if (typeCheck) {
            matchTypes(stmt, lt, rt);
        }
        if (newLHS == stmt.getLHS() && newRHS == stmt.getRHS())
            return stmt;
        return new StmtAssign(stmt, newLHS, newRHS, stmt.getOp());
    }

    public Object TcheckStmtVarDecl(StmtVarDecl stmt) {
        // System.out.println("checkBasicTyping::SymbolTableVisitor::visitStmtVarDecl");

        StmtVarDecl result = (StmtVarDecl) super.visitStmtVarDecl(stmt);
        for (int i = 0; i < result.getNumVars(); i++) {
            Type t = result.getType(i);
            if (t instanceof TypeArray) {
                t = ((TypeArray) t).getAbsoluteBase();
            }
            if (t instanceof TypeStructRef) {
                StructDef sd = nres.getStruct(((TypeStructRef) t).getName());
                if (sd == null) {
                    report(stmt, "Type " + t + " does not exist or is ambiguous");
                }
            }

            Expression ie = result.getInit(i);
            if (ie != null) {
                Type rt = getType(ie);
                boolean typeCheck = true;
                if (ie instanceof ExprNew) {
                    if (((ExprNew) ie).isHole())
                        typeCheck = false;
                }
                if (ie instanceof ExprADTHole) {
                    typeCheck = false;
                }
                if (typeCheck) {
                    matchTypes(result, (result.getType(i)), rt);
                }
            }
        }
        return result;
    }

    public Object visitStmtWhile(StmtWhile stmt) {
        // check the condition
        stmt = (StmtWhile) super.visitStmtWhile(stmt);
        Type cond = getType(stmt.getCond());
        if (!cond.promotesTo(TypePrimitive.bittype, nres))
            report(stmt, "Condition clause is not a proper conditional");

        return stmt;
    }

    public Object visitStmtReturn(StmtReturn stmt) {
        // Check that the return value can be promoted to the
        // function return type
        // System.out.println("checkBasicTyping::SymbolTableVisitor::visitStmtReturn");
        // System.out.println("Return values: " + currentFunctionReturn + " vs. " +
        // getType(stmt.getValue()));

        stmt = (StmtReturn) super.visitStmtReturn(stmt);
        Type rt = getType(stmt.getValue());
        if (rt != null && !rt.promotesTo(currentFunctionReturn, nres))
            report(stmt,
                    "Return value incompatible with declared function return value: " +
                            currentFunctionReturn + " vs. " + getType(stmt.getValue()));
        hasReturn = true;
        return (stmt);
    }

    public Object visitExprChoiceUnary(ExprChoiceUnary exp) {

        exp = (ExprChoiceUnary) super.visitExprChoiceUnary(exp);
        Type ot = getType(exp.getExpr());
        boolean isArr = false;
        if (ot instanceof TypeArray) {
            ot = ((TypeArray) ot).getBase();
            isArr = true;
        }
        List<Integer> ops = exp.opsAsExprUnaryOps();
        for (int op : ops)
            typecheckUnaryExpr(exp.getExpr(), op, ot);

        return exp;
    }
}
