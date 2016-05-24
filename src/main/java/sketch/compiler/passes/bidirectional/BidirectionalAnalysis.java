package sketch.compiler.passes.bidirectional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.FieldDecl;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.NameResolver;
import sketch.compiler.ast.core.Package;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.SymbolTable;
import sketch.compiler.ast.core.SymbolTable.VarInfo;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.*;
import sketch.compiler.ast.core.exprs.ExprArrayRange.RangeLen;
import sketch.compiler.ast.core.exprs.regens.ExprAlt;
import sketch.compiler.ast.core.exprs.regens.ExprChoiceBinary;
import sketch.compiler.ast.core.exprs.regens.ExprChoiceSelect;
import sketch.compiler.ast.core.exprs.regens.ExprChoiceUnary;
import sketch.compiler.ast.core.exprs.regens.ExprParen;
import sketch.compiler.ast.core.exprs.regens.ExprRegen;
import sketch.compiler.ast.core.stmts.*;
import sketch.compiler.ast.core.typs.NotYetComputedType;
import sketch.compiler.ast.core.typs.StructDef;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.ast.core.typs.TypeFunction;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.ast.core.typs.TypeStructRef;
import sketch.compiler.ast.core.typs.TypeUnknownStructRef;
import sketch.compiler.ast.cuda.exprs.CudaBlockDim;
import sketch.compiler.ast.cuda.exprs.CudaInstrumentCall;
import sketch.compiler.ast.cuda.exprs.CudaThreadIdx;
import sketch.compiler.ast.cuda.exprs.ExprRange;
import sketch.compiler.ast.cuda.stmts.CudaSyncthreads;
import sketch.compiler.ast.cuda.stmts.StmtParfor;
import sketch.compiler.ast.promela.stmts.StmtFork;
import sketch.compiler.ast.promela.stmts.StmtJoin;
import sketch.compiler.ast.spmd.exprs.SpmdNProc;
import sketch.compiler.ast.spmd.exprs.SpmdPid;
import sketch.compiler.ast.spmd.stmts.SpmdBarrier;
import sketch.compiler.ast.spmd.stmts.StmtSpmdfork;
import sketch.compiler.passes.lowering.SymbolTableVisitor;
import sketch.compiler.stencilSK.VarReplacer;
import sketch.util.exceptions.ExceptionAtNode;
import sketch.util.exceptions.UnrecognizedVariableException;

class TopDownState {
    private Type expectedType;
    boolean inFieldDecl = false;
    boolean inParamDecl = false;
    boolean inTypeStruct = false;
    boolean inTArr = false;
    boolean inGenerator = false;
    Function currentFun = null;

    public Function getCurrentFun() {
        return currentFun;
    }

    public Function enterFunction(Function f) {
        Function tmp = currentFun;
        currentFun = f;
        inGenerator = f.isGenerator();
        return tmp;
    }

    public void exitFunction(Function f) {
        currentFun = f;
        if (f != null) {
            inGenerator = f.isGenerator();
        } else {
            inGenerator = false;
        }
    }

    public Type getExpected() {
        return expectedType;
    }

    public boolean isInGenerator() {
        return inGenerator;
    }


    public boolean isInTArr() {
        return inTArr;
    }

    public void enterTArr() {
        inTArr = true;
    }

    public void exitTArr() {
        inTArr = false;
    }

    public boolean isInTypeStruct() {
        return inTypeStruct;
    }

    public void enterTypeStruct() {
        inTypeStruct = true;
    }

    public void exitTypeStruct() {
        inTypeStruct = false;
    }

    public boolean isInParamDecl() {
        return inParamDecl;
    }

    public void enterParamDecl() {
        inParamDecl = true;
    }

    public void exitParamDecl() {
        inParamDecl = false;
    }

    public boolean isInFieldDecl() {
        return inFieldDecl;
    }

    public void enterFieldDecl() {
        inFieldDecl = true;
    }

    public void exitFieldDecl() {
        inFieldDecl = false;
    }

    public void beforeRecursion(Type expected, FENode n) {
        expectedType = expected;
    }
}

public class BidirectionalAnalysis extends SymbolTableVisitor {
    protected TopDownState tdstate = new TopDownState();
    protected List<BidirectionalPass> previsitors = new ArrayList<BidirectionalPass>();
    protected List<BidirectionalPass> postvisitors = new ArrayList<BidirectionalPass>();
    Type returnType = null;
    protected final TempVarGen varGen;
    Stack<String> funsToVisit = new Stack<String>();
    Stack<Function> queuedForAnalysis = new Stack<Function>();
    Map<String, SymbolTable> closureStore = new HashMap<String, SymbolTable>();

    public TempVarGen getVarGen() {
        return varGen;
    }

    public void addClosure(String funName, SymbolTable st) {
        closureStore.put(funName, st);
    }

    public SymbolTable swapSymTable(SymbolTable st) {
        SymbolTable old = this.symtab;
        this.symtab = st;
        return old;
    }

    public SymbolTable getClosure(String funName) {
        return closureStore.get(funName);
    }

    public BidirectionalAnalysis(TempVarGen varGen) {
        super(null);
        this.varGen = varGen;
    }

    public void addPass(BidirectionalPass bdp) {
        previsitors.add(bdp);
        bdp.registerDriver(this);
    }

    public void addPostPass(BidirectionalPass bdp) {
        postvisitors.add(bdp);
        bdp.registerDriver(this);
    }

    public Type merge(Type a, Type b) {
        return a.leastCommonPromotion(b, nres);
    }

    protected void addStatement(Statement s) {
        Statement rv = procStatement(s);
        if (rv != null)
            newStatements.add(rv);
    }

    public void doStatement(Statement st) {
        addStatement(st);
    }

    public <T extends FENode> T doT(T e) {
        T rv = e;
        for (BidirectionalPass fv : previsitors) {
            rv = (T) rv.accept(fv);
            if (rv == null) {
                break;
            }
        }
        if (rv != null) {
            rv = (T) rv.accept(this);
        }
        for (BidirectionalPass fv : postvisitors) {
            if (rv == null) {
                break;
            }
            rv = (T) rv.accept(fv);
        }
        return rv;
    }

    public Program doProgram(Program p) {
        nres = new NameResolver(p);
        SymbolTable oldSymTab = symtab;
        symtab = new SymbolTable(symtab);
        p = doT(p);
        symtab = oldSymTab;
        return p;
    }

    public Statement procStatement(Statement e) {
        return doT(e);
    }
    

    public Function doFunction(Function e) {
        return doT(e);
    }

    public Expression doExpression(Expression e) {
        return doT(e);
    }

    public Type doType(Type t) {
        Type rv = t;
        for (BidirectionalPass fv : previsitors) {
            rv = (Type) rv.accept(fv);
        }
        rv = (Type) rv.accept(this);
        return rv;
    }

    public Object visitExprAlt(ExprAlt exp) {
        Type tths = getType(exp.getThis());
        Type tthat = getType(exp.getThat());
        Type common = merge(tths, tthat);

        tdstate.beforeRecursion(common, exp);
        Expression ths = doExpression(exp.getThis());
        tdstate.beforeRecursion(common, exp);
        Expression that = doExpression(exp.getThat());
        return (ths == exp.getThis() && that == exp.getThat()) ? exp : new ExprAlt(exp, ths, that);
    }

    public Object visitExprTuple(ExprTuple exp) {
        // TODO Auto-generated method stub
        return null;
    }

    public Object visitExprArrayInit(ExprArrayInit exp) {
        Type common = TypePrimitive.bottomtype;
        for (Expression e : exp.getElements()) {
            common = merge(common, getType(e));
        }
        boolean hasChanged = false;
        List<Expression> newElements = new ArrayList<Expression>();
        for (Iterator iter = exp.getElements().iterator(); iter.hasNext();) {
            Expression element = (Expression) iter.next();
            tdstate.beforeRecursion(common, exp);
            Expression newElement = doExpression(element);
            newElements.add(newElement);
            if (element != newElement)
                hasChanged = true;
        }
        if (!hasChanged)
            return exp;
        return new ExprArrayInit(exp, newElements);
    }

    public Object visitExprADTHole(ExprADTHole exp) {
        // TODO Auto-generated method stub
        return null;
    }

    public Object visitExprTupleAccess(ExprTupleAccess exp) {
        // TODO Auto-generated method stub
        return null;
    }

    public Object visitExprArrayRange(ExprArrayRange exp) {
        Type expected = tdstate.getExpected();
        expected = new TypeArray(expected, null);
        tdstate.beforeRecursion(expected, exp);
        final Expression newBase = doExpression(exp.getBase());

        RangeLen range = exp.getSelection();

        tdstate.beforeRecursion(TypePrimitive.inttype, exp);
        Expression newStart = doExpression(range.start());
        Expression newLen = null;
        if (range.hasLen()) {
            tdstate.beforeRecursion(TypePrimitive.inttype, exp);
            newLen = doExpression(range.getLenExpression());
        }
        if (newBase != exp.getBase() || newStart != range.start() || (newLen != null && newLen != range.getLenExpression())) {
            return new ExprArrayRange(exp, newBase, new RangeLen(newStart, newLen));
        }
        return exp;

    }

    public Object visitExprBinary(ExprBinary exp) {
        switch (exp.getOp()) {
        case ExprBinary.BINOP_GE:
        case ExprBinary.BINOP_GT:
        case ExprBinary.BINOP_LE:
        case ExprBinary.BINOP_LT: {
            Type tleft = getType(exp.getLeft());
            Type tright = getType(exp.getRight());
            Type tt = merge(tleft, tright);
            tdstate.beforeRecursion(tt, exp);
            Expression left = doExpression(exp.getLeft());
            tdstate.beforeRecursion(tt, exp);
            Expression right = doExpression(exp.getRight());
            if (left == exp.getLeft() && right == exp.getRight())
                return exp;
            else
                return new ExprBinary(exp, exp.getOp(), left, right, exp.getAlias());
        }
        case ExprBinary.BINOP_LSHIFT:
        case ExprBinary.BINOP_RSHIFT: {
            tdstate.beforeRecursion(tdstate.getExpected(), exp);
            Expression left = doExpression(exp.getLeft());
            tdstate.beforeRecursion(TypePrimitive.inttype, exp);
            Expression right = doExpression(exp.getRight());
            if (left == exp.getLeft() && right == exp.getRight())
                return exp;
            else
                return new ExprBinary(exp, exp.getOp(), left, right, exp.getAlias());
        }
        case ExprBinary.BINOP_NEQ:
        case ExprBinary.BINOP_EQ: {
            Type tleft = getType(exp.getLeft());
            Type tright = getType(exp.getRight());
            Type tboth = merge(tleft, tright);
            tdstate.beforeRecursion(tboth, exp);
            Expression left = doExpression(exp.getLeft());
            tdstate.beforeRecursion(tboth, exp);
            Expression right = doExpression(exp.getRight());
            if (left == exp.getLeft() && right == exp.getRight())
                return exp;
            else
                return new ExprBinary(exp, exp.getOp(), left, right, exp.getAlias());
        }

        default: {
            Type tboth = tdstate.getExpected();
            tdstate.beforeRecursion(tboth, exp);
            Expression left = doExpression(exp.getLeft());
            tdstate.beforeRecursion(tboth, exp);
            Expression right = doExpression(exp.getRight());
            if (left == exp.getLeft() && right == exp.getRight())
                return exp;
            else
                return new ExprBinary(exp, exp.getOp(), left, right, exp.getAlias());
        }
        }
    }

    public Object visitExprChoiceBinary(ExprChoiceBinary exp) {
        Type tleft = getType(exp.getLeft());
        Type tright = getType(exp.getRight());
        Type tboth = merge(tleft, tright);
        tdstate.beforeRecursion(tboth, exp);
        Expression left = doExpression(exp.getLeft());
        tdstate.beforeRecursion(tboth, exp);
        Expression right = doExpression(exp.getRight());
        if (left == exp.getLeft() && right == exp.getRight())
            return exp;
        else
            return new ExprChoiceBinary(exp, left, exp.getOps(), right);
    }

    public Object visitExprChoiceSelect(ExprChoiceSelect exp) {
        tdstate.beforeRecursion(tdstate.getExpected(), exp);
        Expression obj = doExpression(exp.getObj());
        if (obj == exp.getObj())
            return exp;
        else
            return new ExprChoiceSelect(exp, obj, exp.getField());
    }

    public Object visitExprChoiceUnary(ExprChoiceUnary exp) {
        tdstate.beforeRecursion(tdstate.getExpected(), exp);
        Expression expr = doExpression(exp.getExpr());
        if (expr == exp.getExpr())
            return exp;
        else
            return new ExprChoiceUnary(exp, exp.getOps(), expr);
    }

    public Object visitExprConstChar(ExprConstChar exp) {
        return exp;
    }

    public Object visitExprConstFloat(ExprConstFloat exp) {
        return exp;
    }

    public Object visitExprConstInt(ExprConstInt exp) {
        return exp;
    }

    public Object visitExprLiteral(ExprLiteral exp) {
        return exp;
    }

    public Object visitExprField(ExprField exp) {
        TypeUnknownStructRef expected = new TypeUnknownStructRef();
        expected.fields.add(exp.getName());
        tdstate.beforeRecursion(expected, exp);
        Expression left = doExpression(exp.getLeft());
        if (left == exp.getLeft())
            return exp;
        else
            return new ExprField(exp, left, exp.getName(), exp.isHole());
    }

    public Object visitExprFunCall(ExprFunCall exp) {
        if (exp.getName().equals("minimize")) {
            tdstate.beforeRecursion(TypePrimitive.inttype, exp);
            if (exp.getParams().size() != 1) {
                throw new ExceptionAtNode("minimize is a keyword, and it must have exactly one integer parameter.", exp);
            }
            return super.visitExprFunCall(exp);
        }
        Function f;

        VarInfo vi = symtab.lookupVarInfo(exp.getName());
        if (vi != null) {
            if (vi.kind == SymbolTable.KIND_LOCAL_FUNCTION) {
                f = (Function) vi.origin;
            } else {
                if (vi.kind == SymbolTable.KIND_FUNC_PARAM) {
                    f = null;
                } else {
                    throw new ExceptionAtNode("Function is unknown or ambiguous " + exp.getName(), exp);
                }
            }
        } else {
            try {
                f = nres.getFun(exp.getName(), exp);
            } catch (UnrecognizedVariableException e) {
                throw new ExceptionAtNode("Function is unknown or ambiguous " + exp.getName(), exp);
            }
        }

        boolean hasChanged = false;
        List<Expression> newParams = new ArrayList<Expression>();
        if (f == null) {
            // Unknown function; we don't have a signature.
            NotYetComputedType nyc = NotYetComputedType.singleton;
            for (Expression actual : exp.getParams()) {
                tdstate.beforeRecursion(nyc, exp);
                Expression newParam = doExpression(actual);
                newParams.add(newParam);
                if (actual != newParam)
                    hasChanged = true;
            }
            if (!hasChanged)
                return exp;
            return new ExprFunCall(exp, exp.getName(), newParams);
        }



        List<Type> actualTypes = new ArrayList<Type>();
        for (Expression ap : exp.getParams()) {
            actualTypes.add(getType(ap));
        }

        TypeRenamer tren = SymbolTableVisitor.getRenaming(f, actualTypes);
        int actSz = exp.getParams().size();
        int formSz = f.getParams().size();
        if (actSz > formSz) {
            throw new ExceptionAtNode("Incorrect number of parameters", exp);
        }
        int implSz = formSz - actSz;
        boolean hadImp = (implSz > 0);
        Map<String, Integer> pm = new HashMap<String, Integer>(implSz);
        Iterator<Expression> actIt = exp.getParams().iterator();

        Map<String, Expression> repl = new HashMap<String, Expression>();
        VarReplacer vr = new VarReplacer(repl);

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

                Type formalType = vr.replace(tren.rename(formal.getType()));
                formalType = formalType.addDefaultPkg(f.getPkg(), nres);
                Type ftt = formalType;

                tdstate.beforeRecursion(formalType, exp);
                Expression newParam = doExpression(actual);
                Type paramOriType = getType(newParam);
                Type actType = paramOriType.addDefaultPkg(nres.curPkg().getName(), nres);
                Type att = actType;
                while (ftt instanceof TypeArray) {
                    if (paramOriType instanceof NotYetComputedType) {
                        return exp;
                    }
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
                        repl.put(len, actLen);
                        int idx = pm.get(len);
                        if (newParams.get(idx) == null) {
                            newParams.set(idx, actLen);
                        } else {
                            addStatement(new StmtAssert(exp, new ExprBinary(newParams.get(idx), "==", actLen),
                                    exp.getCx() + ": Inconsistent array lengths for implicit parameter " + len + ".", false));
                        }
                    }
                }
                repl.put(formal.getName(), newParam);
                newParams.add(newParam);
                if (actual != newParam)
                    hasChanged = true;
            }

        }

        if (!hasChanged)
            return exp;
        return new ExprFunCall(exp, exp.getName(), newParams);
    }

    public Object visitExprParen(ExprParen exp) {
        tdstate.beforeRecursion(tdstate.getExpected(), exp);
        Expression expr = doExpression(exp.getExpr());
        if (expr == exp.getExpr())
            return exp;
        else
            return new ExprParen(exp, expr);
    }

    public Object visitExprRegen(ExprRegen exp) {
        tdstate.beforeRecursion(tdstate.getExpected(), exp);
        Expression expr = doExpression(exp.getExpr());
        if (expr == exp.getExpr())
            return exp;
        else
            return new ExprRegen(exp, expr);
    }

    public Object visitExprStar(ExprStar star) {
        if (star.getType() != null) {
            Type t = (Type) star.getType().accept(this);
            if (t != star.getType()) {
                ExprStar s = new ExprStar(star);
                s.setType(t);
                if (star.special())
                    s.makeSpecial(star.parentHoles());
            }
        } else {
            star.setType(tdstate.getExpected());
        }
        return star;
    }

    public Object visitExprTernary(ExprTernary exp) {
        Type base = tdstate.getExpected();
        tdstate.beforeRecursion(TypePrimitive.bittype, exp);
        Expression a = doExpression(exp.getA());
        tdstate.beforeRecursion(base, exp);
        Expression b = doExpression(exp.getB());
        tdstate.beforeRecursion(base, exp);
        Expression c = doExpression(exp.getC());
        if (a == exp.getA() && b == exp.getB() && c == exp.getC())
            return exp;
        else
            return new ExprTernary(exp, exp.getOp(), a, b, c);
    }

    public Object visitExprTypeCast(ExprTypeCast exp) {
        tdstate.beforeRecursion(TypePrimitive.bottomtype, exp);
        Expression expr = doExpression(exp.getExpr());
        Type t = doType(exp.getType());
        if (expr == exp.getExpr() && t == exp.getType())
            return exp;
        else
            return new ExprTypeCast(exp, t, expr);
    }

    public Object visitExprUnary(ExprUnary exp) {
        Type base = tdstate.getExpected();
        tdstate.beforeRecursion(base, exp);
        Expression expr = doExpression(exp.getExpr());
        if (expr == exp.getExpr())
            return exp;
        else
            return new ExprUnary(exp, exp.getOp(), expr);
    }

    public Object visitExprVar(ExprVar exp) {
        return exp;
    }

    public Object visitFieldDecl(FieldDecl field) {
        tdstate.enterFieldDecl();
        for (int i = 0; i < field.getNumFields(); i++)
            symtab.registerVar(field.getName(i), field.getType(i), field, SymbolTable.KIND_GLOBAL);
        List<Expression> newInits = new ArrayList<Expression>();
        List<Type> newTypes = new ArrayList<Type>();
        for (int i = 0; i < field.getNumFields(); i++) {
            Type newType = doType(field.getType(i));
            Expression init = field.getInit(i);
            if (init != null) {
                tdstate.beforeRecursion(newType, field);
                init = doExpression(init);
            }
            newInits.add(init);
            newTypes.add(newType);
        }
        tdstate.exitFieldDecl();
        return new FieldDecl(field, newTypes, field.getNames(), newInits);
    }


    public Object visitFunction(Function func) {
        SymbolTable oldSymTab = symtab;
        symtab = new SymbolTable(symtab);
        boolean hadTPs = !func.getTypeParams().isEmpty();
        if (hadTPs) {
            nres.pushTempTypes(func.getTypeParams());
        }

        Function oldCurFun = tdstate.enterFunction(func);

        Type oldRT = returnType;
        returnType = func.getReturnType();
        Object result = null;

        try {

        List<Parameter> newParam = new ArrayList<Parameter>();
        Iterator<Parameter> it = func.getParams().iterator();
        boolean samePars = true;
        while (it.hasNext()) {
            Parameter par = it.next();
            Parameter newPar = doT(par);
            if (par != newPar)
                samePars = false;
            newParam.add(newPar);
        }

        Type rtype = doType(func.getReturnType());

        if (func.getBody() == null) {
            assert func.isUninterp() : "Only uninterpreted functions are allowed to have null bodies.";
            if (samePars && rtype == func.getReturnType())
                return func;
            return func.creator().returnType(rtype).params(newParam).create();
        }
        Statement newBody = procStatement(func.getBody());
        if (newBody == null)
            newBody = new StmtEmpty(func);
        if (newBody == func.getBody() && samePars && rtype == func.getReturnType()) {
            result = func;
        } else {
            result = func.creator().returnType(rtype).params(newParam).body(newBody).create();
        }

        } finally {
            returnType = oldRT;
            if (hadTPs) {
                nres.popTempTypes();
            }

            tdstate.exitFunction(oldCurFun);
            symtab = oldSymTab;
        }
        return result;
    }

    public StructDef getStructDef(Type t) {
        return super.getStructDef(t);
    }

    public Object visitProgram(Program prog) {

        List<Package> newStreams = new ArrayList<Package>();
        for (Package pkg : prog.getPackages()) {
            newStreams.add(doT(pkg));
        }

        return prog.creator().streams(newStreams).create();
    }

    public Object visitStmtAssign(StmtAssign stmt) {
        Type lhst = getType(stmt.getLHS());
        Type rhst = getType(stmt.getRHS());
        Type merged = merge(lhst, rhst);
        tdstate.beforeRecursion(merged, stmt);
        Expression newLHS = doExpression(stmt.getLHS());
        tdstate.beforeRecursion(merged, stmt);
        Expression newRHS = doExpression(stmt.getRHS());
        if (newLHS == stmt.getLHS() && newRHS == stmt.getRHS())
            return stmt;
        return new StmtAssign(stmt, newLHS, newRHS, stmt.getOp());
    }

    public Object visitStmtAtomicBlock(StmtAtomicBlock stmt) {
        // TODO Auto-generated method stub
        return null;
    }


    public Object visitStmtBlock(StmtBlock block) {
        SymbolTable oldSymTab = symtab;
        symtab = new SymbolTable(symtab);
        Object result = super.visitStmtBlock(block);
        symtab = oldSymTab;
        return result;
    }

    public Object visitStmtBreak(StmtBreak stmt) {
        // TODO Auto-generated method stub
        return null;
    }

    public Object visitStmtContinue(StmtContinue stmt) {
        // TODO Auto-generated method stub
        return null;
    }

    public Object visitStmtDoWhile(StmtDoWhile stmt)
    {
        Statement newBody = procStatement(stmt.getBody());
        tdstate.beforeRecursion(TypePrimitive.bittype, stmt);
        Expression newCond = doExpression(stmt.getCond());
        if (newBody == stmt.getBody() && newCond == stmt.getCond())
            return stmt;
        return new StmtDoWhile(stmt, newBody, newCond);
    }

    public Object visitStmtEmpty(StmtEmpty stmt) {
        return stmt;
    }

    public Object visitStmtExpr(StmtExpr stmt) {
        Object nextInner = stmt.getExpression();
        if (nextInner == null) {
            return null;
        } else if (nextInner instanceof Expression) {
            tdstate.beforeRecursion(TypePrimitive.bottomtype, stmt);
            Expression newExpr = doExpression((Expression) nextInner);
            if (newExpr == stmt.getExpression())
                return stmt;
            return new StmtExpr(stmt, newExpr);
        } else if (nextInner instanceof Statement) {

            return procStatement((Statement) nextInner);
        } else {
            throw new RuntimeException("unknown return value from stmt expr: " + nextInner);
        }
    }

    public Object visitStmtFor(StmtFor stmt) {
        SymbolTable oldSymTab = symtab;
        symtab = new SymbolTable(symtab);
        Object result = null;
        Statement newInit = null;
        if (stmt.getInit() != null) {
            newInit = procStatement(stmt.getInit());
        }
        tdstate.beforeRecursion(TypePrimitive.bittype, stmt);
        Expression newCond = doExpression(stmt.getCond());
        Statement newIncr = null;
        if (stmt.getIncr() != null) {
            newIncr = procStatement(stmt.getIncr());
        }
        Statement tmp = stmt.getBody();
        Statement newBody = StmtEmpty.EMPTY;
        if (tmp != null) {
            newBody = procStatement(tmp);
        }
        if (newInit == stmt.getInit() && newCond == stmt.getCond() && newIncr == stmt.getIncr() && newBody == stmt.getBody()) {
            result = stmt;
        } else {
            result = new StmtFor(stmt, newInit, newCond, newIncr, newBody, stmt.isCanonical());
        }
        symtab = oldSymTab;
        return result;
    }

    public Object visitStmtIfThen(StmtIfThen stmt) {
        tdstate.beforeRecursion(TypePrimitive.bittype, stmt);
        Expression newCond = doExpression(stmt.getCond());
        Statement newCons = stmt.getCons() == null ? null : procStatement(stmt.getCons());

        Statement newAlt = stmt.getAlt() == null ? null : procStatement(stmt.getAlt());

        if (newCond == stmt.getCond() && newCons == stmt.getCons() && newAlt == stmt.getAlt())
            return stmt;
        if (newCons == null && newAlt == null) {
            return new StmtExpr(stmt, newCond);
        }
        StmtIfThen newStmt = new StmtIfThen(stmt, newCond, newCons, newAlt);
        if (stmt.isSingleFunCall())
            newStmt.singleFunCall();
        if (stmt.isSingleVarAssign())
            newStmt.singleVarAssign();
        return newStmt;
    }

    public Object visitStmtInsertBlock(StmtInsertBlock stmt) {
        Statement newIns = procStatement(stmt.getInsertStmt()); 
        Statement newInto = procStatement(stmt.getIntoBlock());

        if (newIns == stmt.getInsertStmt() && newInto == stmt.getIntoBlock())
            return stmt;
        else if (null == newIns || null == newInto)
            return null;
        else {
            if (!(newInto.isBlock()))
                newInto = new StmtBlock(newInto);
            return new StmtInsertBlock(stmt, newIns, (StmtBlock) newInto);
        }
    }

    public Object visitStmtJoin(StmtJoin stmt) {
        // TODO Auto-generated method stub
        return null;
    }

    public Object visitStmtLoop(StmtLoop stmt) {
        tdstate.beforeRecursion(TypePrimitive.bittype, stmt);
        Expression newIter = doExpression(stmt.getIter());
        Statement newBody = procStatement(stmt.getBody());
        if (newIter == stmt.getIter() && newBody == stmt.getBody())
            return stmt;
        return new StmtLoop(stmt, newIter, newBody);
    }

    public Object visitStmtReturn(StmtReturn stmt) {
        tdstate.beforeRecursion(returnType, stmt);
        Expression newValue = stmt.getValue() == null ? null : doExpression(stmt.getValue());
        if (newValue == stmt.getValue())
            return stmt;
        return new StmtReturn(stmt, newValue);
    }

    public Object visitStmtAssert(StmtAssert stmt) {
        tdstate.beforeRecursion(TypePrimitive.bittype, stmt);
        Expression newValue = stmt.getCond() == null ? null : doExpression(stmt.getCond());
        Integer ev = newValue.getIValue();
        if (ev != null && ev == 1) {
            return null;
        }
        if (newValue == stmt.getCond()) {
            return stmt;
        }
        return new StmtAssert(stmt, newValue, stmt.getMsg(), stmt.isSuper(), stmt.isHard);
    }

    public Object visitStmtVarDecl(StmtVarDecl stmt) {
        List<Expression> newInits = new ArrayList<Expression>();
        List<Type> newTypes = new ArrayList<Type>();
        boolean changed = false;
        for (int i = 0; i < stmt.getNumVars(); i++) {
            Expression oinit = stmt.getInit(i);

            Type ot = stmt.getType(i);
            Type t = doType(ot);

            Expression init = null;
            if (oinit != null) {
                tdstate.beforeRecursion(t, stmt);
                init = doExpression(oinit);
            }

            // Bug fix: We need to store the old type in the symbol table rather
            // than new type.
            symtab.registerVar(stmt.getName(i), ot, stmt, SymbolTable.KIND_LOCAL);

            if (ot != t || oinit != init) {
                changed = true;
            }
            newInits.add(init);
            newTypes.add(t);
        }
        if (!changed) {
            return stmt;
        }
        return new StmtVarDecl(stmt, newTypes, stmt.getNames(), newInits);
    }

    public void queueForAnalysis(Function f) {
        queuedForAnalysis.push(f);
    }

    public void addFunction(Function f) {
        if (!isHighOrder(f)) {
            newFuncs.add(f);
        }
    }

    public Object visitStmtWhile(StmtWhile stmt) {
        tdstate.beforeRecursion(TypePrimitive.bittype, stmt);
        Expression newCond = doExpression(stmt.getCond());
        Statement newBody = procStatement(stmt.getBody());
        if (newCond == stmt.getCond() && newBody == stmt.getBody())
            return stmt;
        return new StmtWhile(stmt, newCond, newBody);
    }

    public Object visitStmtFunDecl(StmtFunDecl stmt) {
        Function f = doFunction(stmt.getDecl());
        symtab.registerVar(f.getName(), TypeFunction.singleton, f, SymbolTable.KIND_LOCAL_FUNCTION);
        if (f != stmt.getDecl()) {
            return new StmtFunDecl(stmt, f);
        } else {
            return stmt;
        }
    }

    boolean isHighOrder(Function fun) {
        for (Parameter p : fun.getParams()) {
            if (p.getType() instanceof TypeFunction) {
                return true;
            }
        }
        return false;
    }

    public Object visitPackage(Package spec) {
        SymbolTable oldSymTab = symtab;
        symtab = new SymbolTable(symtab);
        if (nres != null)
            nres.setPackage(spec);

        // register functions
        for (Iterator iter = spec.getFuncs().iterator(); iter.hasNext();) {
            Function func = (Function) iter.next();
            symtab.registerFn(func);
        }
        Object result = null;

        List<FieldDecl> newVars = new ArrayList<FieldDecl>();
        List<Function> oldNewFuncs = newFuncs;
        newFuncs = new ArrayList<Function>();

        boolean changed = false;

        for (FieldDecl oldVar : spec.getVars()) {
            FieldDecl newVar = doT(oldVar);
            if (oldVar != newVar)
                changed = true;
            if (newVar != null)
                newVars.add(newVar);
        }

        List<StructDef> newStructs = new ArrayList<StructDef>();
        nstructsInPkg = spec.getStructs().size();
        for (StructDef tsOrig : spec.getStructs()) {
            StructDef ts = doT(tsOrig);
            if (ts != tsOrig) {
                changed = true;
            }
            newStructs.add(ts);
        }
        nstructsInPkg = -1;

        int nonNull = 0;
        queuedForAnalysis.clear();
        for (Function oldFunc : spec.getFuncs()) {
            // High Order Functions are only analyzed on demand.
            if (!isHighOrder(oldFunc)) {
                Function newFunc = doFunction(oldFunc);
                if (oldFunc != newFunc)
                    changed = true;
                // if(oldFunc != null)++nonNull;
                if (newFunc != null)
                    newFuncs.add(newFunc);
            } else {
                changed = true;
            }
        }
        while (!queuedForAnalysis.isEmpty()) {
            changed = true;
            Function f = queuedForAnalysis.pop();
            Function newFunc = doFunction(f);
            if (newFunc != null)
                newFuncs.add(newFunc);
        }

        if (newFuncs.size() != nonNull) {
            changed = true;
        }

        List<Function> nf = newFuncs;
        // newFuncs = oldNewFuncs;
        if (!changed) {
            result = spec;
        } else {
            result = new Package(spec, spec.getName(), newStructs, newVars, nf, spec.getSpAsserts());
        }
        symtab = oldSymTab;
        return result;
    }

    public Object visitOther(FENode node) {
        // TODO Auto-generated method stub
        return null;
    }

    public Object visitType(Type t) {
        return t;
    }

    public Object visitTypePrimitive(TypePrimitive t) {
        return t;
    }

    public Object visitTypeArray(TypeArray t) {
        tdstate.enterTArr();
        Type nbase = doType(t.getBase());
        Expression nlen = null;
        if (t.getLength() != null) {
            tdstate.beforeRecursion(TypePrimitive.inttype, t.getLength());
            nlen = doExpression(t.getLength());
        }
        tdstate.exitTArr();
        if (nbase == t.getBase() && t.getLength() == nlen)
            return t;
        TypeArray newtype = new TypeArray(t.getCudaMemType(), nbase, nlen, t.getMaxlength());
        return newtype;
    }

    public Object visitStructDef(StructDef ts) {
        tdstate.enterTypeStruct();
        Object o = super.visitStructDef(ts);
        tdstate.exitTypeStruct();
        return o;
    }

    public Object visitTypeStructRef(TypeStructRef ts) {
        return ts;
    }

    public Object visitParameter(Parameter par) {
        tdstate.enterParamDecl();
        Object o = super.visitParameter(par);
        tdstate.exitParamDecl();
        return o;
    }

    public Object visitExprNew(ExprNew expNew) {
        Type nt = null;
        if (expNew.getTypeToConstruct() != null) {
            nt = doType(expNew.getTypeToConstruct());
        }

        StructDef tsr;
        if (nt instanceof TypeStructRef) {
            tsr = nres.getStruct(((TypeStructRef) nt).getName());
        } else {
            throw new ExceptionAtNode("You can only construct structs and adts", expNew);
        }

        Map<String, Expression> repl = new HashMap<String, Expression>();
        VarReplacer vr = new VarReplacer(repl);
        for (ExprNamedParam en : expNew.getParams()) {
            repl.put(en.getName(), en.getExpr());
        }

        boolean changed = false;
        List<ExprNamedParam> enl = new ArrayList<ExprNamedParam>(expNew.getParams().size());
        for (ExprNamedParam en : expNew.getParams()) {
            Expression old = en.getExpr();
            Type told = tsr.getTypeDeep(en.getName(), nres);
            told = doType(vr.replace(told));
            tdstate.beforeRecursion(told, expNew);
            Expression rhs = doExpression(old);
            if (rhs != old) {
                enl.add(new ExprNamedParam(en, en.getName(), rhs, en.getVariantName()));
                changed = true;
            } else {
                enl.add(en);
            }
        }

        if (nt != expNew.getTypeToConstruct() || changed) {
            if (!changed) {
                enl = expNew.getParams();
            }
            return new ExprNew(expNew, nt, enl, expNew.isHole(), expNew.getStar());
        } else {
            return expNew;
        }

    }

    public Object visitStmtFork(StmtFork loop) {
        // TODO Auto-generated method stub
        return null;
    }

    public Object visitStmtReorderBlock(StmtReorderBlock stmt) {
        Object o = procStatement(stmt.getBlock());
        if (o == stmt.getBlock())
            return stmt;
        else if (o == null)
            return null;
        else if (o instanceof StmtBlock)
            return new StmtReorderBlock(stmt, (StmtBlock) o);
        else
            return new StmtReorderBlock(stmt, Collections.singletonList((Statement) o));
    }

    public Object visitStmtSwitch(StmtSwitch stmt) {
        SymbolTable oldSymTab = symtab;
        symtab = new SymbolTable(symtab);

        Type t = getType(stmt.getExpr());
        if (!(t instanceof TypeStructRef)) {
            throw new ExceptionAtNode("Only ADTs are allowed as arguments to switch statements. The type of " + stmt.getExpr() + " is " + t, stmt);
        }
        tdstate.beforeRecursion(t, stmt.getExpr());
        Expression sexp = doExpression(stmt.getExpr());
        ExprVar var = (ExprVar) sexp;

        // visit each case body
        StmtSwitch newStmt = new StmtSwitch(stmt.getContext(), var);
        String name = ((TypeStructRef) t).getName();
        StructDef ts = nres.getStruct(name);
        String pkg;
        if (ts == null) {
            pkg = nres.curPkg().getName();
            throw new ExceptionAtNode("The type " + name + " is unknown in package " + pkg, stmt.getExpr());
        } else {
            pkg = ts.getPkg();
        }
        for (String caseExpr : stmt.getCaseConditions()) {
            if (!("default".equals(caseExpr) || "repeat".equals(caseExpr))) {
                SymbolTable oldSymTab1 = symtab;
                symtab = new SymbolTable(symtab);
                symtab.registerVar(var.getName(), (new TypeStructRef(caseExpr, false)).addDefaultPkg(pkg, nres));

                Statement body = procStatement(stmt.getBody(caseExpr));
                newStmt.addCaseBlock(caseExpr, body);
                symtab = oldSymTab1;
            } else {
                SymbolTable oldSymTab1 = symtab;
                symtab = new SymbolTable(symtab);
                Statement body = procStatement(stmt.getBody(caseExpr));
                newStmt.addCaseBlock(caseExpr, body);
                symtab = oldSymTab1;
            }
        }
        symtab = oldSymTab;

        return newStmt;
    }

    public Object visitExprNullPtr(ExprNullPtr nptr) {
        return nptr;
    }

    public Object visitStmtMinimize(StmtMinimize stmtMinimize) {
        final Expression previous = stmtMinimize.getMinimizeExpr();
        tdstate.beforeRecursion(TypePrimitive.inttype, stmtMinimize);
        final Expression newVariable = doExpression(previous);
        if (newVariable == previous || !(newVariable instanceof Expression)) {
            return stmtMinimize;
        } else {
            return new StmtMinimize((Expression) newVariable, stmtMinimize.userGenerated);
        }
    }

    public Object visitStmtMinLoop(StmtMinLoop stmtMinLoop) {
        final Statement newBody = procStatement(stmtMinLoop.getBody());
        if (newBody == stmtMinLoop.getBody()) {
            return stmtMinLoop;
        } else {
            return new StmtMinLoop(stmtMinLoop, newBody);
        }
    }

    public Object visitExprFieldsListMacro(ExprFieldsListMacro exp) {
        tdstate.beforeRecursion(new TypeUnknownStructRef(), exp);
        Expression left = doExpression(exp.getLeft());
        Type t = doType(exp.getType());
        if (left == exp.getLeft() && t == exp.getType()) {
            return exp;
        } else {
            return new ExprFieldsListMacro(exp, left, t);
        }
    }

    public Object visitExprSpecialStar(ExprSpecialStar exprSpecialStar) {
        // TODO Auto-generated method stub
        return null;
    }

    public Object visitCudaThreadIdx(CudaThreadIdx cudaThreadIdx) {
        // TODO Auto-generated method stub
        return null;
    }

    public Object visitCudaBlockDim(CudaBlockDim cudaBlockDim) {
        // TODO Auto-generated method stub
        return null;
    }

    public Object visitCudaSyncthreads(CudaSyncthreads cudaSyncthreads) {
        // TODO Auto-generated method stub
        return null;
    }

    public Object visitCudaInstrumentCall(CudaInstrumentCall instrumentCall) {
        // TODO Auto-generated method stub
        return null;
    }

    public Object visitExprRange(ExprRange exprRange) {
        // TODO Auto-generated method stub
        return null;
    }

    public Object visitStmtParfor(StmtParfor stmtParfor) {
        // TODO Auto-generated method stub
        return null;
    }

    public Object visitStmtImplicitVarDecl(StmtImplicitVarDecl decl) {
        // TODO Auto-generated method stub
        return null;
    }

    public Object visitExprNamedParam(ExprNamedParam exprNamedParam) {
        // TODO Auto-generated method stub
        return null;
    }

    public Object visitExprType(ExprType exprtyp) {
        // TODO Auto-generated method stub
        return null;
    }

    public Object visitStmtSpmdfork(StmtSpmdfork stmtSpmdfork) {
        // TODO Auto-generated method stub
        return null;
    }

    public Object visitSpmdBarrier(SpmdBarrier spmdBarrier) {
        // TODO Auto-generated method stub
        return null;
    }

    public Object visitSpmdPid(SpmdPid spmdpid) {
        // TODO Auto-generated method stub
        return null;
    }

    public Object visitSpmdNProc(SpmdNProc spmdnproc) {
        // TODO Auto-generated method stub
        return null;
    }

    public Object visitStmtAssume(StmtAssume stmt) {
        tdstate.beforeRecursion(TypePrimitive.bittype, stmt);
        Expression newValue = stmt.getCond() == null ? null : doExpression(stmt.getCond());
        if (newValue == stmt.getCond())
            return stmt;
        return new StmtAssume(stmt, newValue, stmt.getMsg());
    }

    public Object visitExprLocalVariables(ExprLocalVariables exprLocalVariables) {
        // TODO Auto-generated method stub
        return null;
    }

    public Object visitExprLambda(ExprLambda exprLambda) {
        // TODO Auto-generated method stub
        return null;
    }

}
