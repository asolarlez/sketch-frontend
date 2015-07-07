package sketch.compiler.passes.preprocessing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.SymbolTable;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.*;
import sketch.compiler.ast.core.exprs.regens.ExprAlt;
import sketch.compiler.ast.core.exprs.regens.ExprRegen;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtBlock;
import sketch.compiler.ast.core.stmts.StmtIfThen;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.StructDef;
import sketch.compiler.ast.core.typs.StructDef.StructFieldEnt;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.ast.core.typs.TypeStructRef;
import sketch.compiler.passes.lowering.SymbolTableVisitor;

class Clone extends FEReplacer {

    public Object visitExprStar(ExprStar es) {
        ExprStar newStar = new ExprStar(es);
        es.renewName();
        if (es.special())
            newStar.makeSpecial(es.parentHoles());
        return newStar;
    }

    public Expression process(Expression e) {
        return (Expression) e.accept(this);
    }

    public Object visitExprNew(ExprNew exp) {
        ExprNew nexp = (ExprNew) super.visitExprNew(exp);
        if (nexp.isHole()) {
            ExprStar newStar = (ExprStar) nexp.getStar().accept(this);
        }
        return nexp;
    }

    public Object visitExprField(ExprField exp) {
        if (exp.isHole()) {
            return new ExprField(exp, exp.getLeft(), exp.getName(), true);
        }
        return exp;
    }

}

class ArrayTypeReplacer extends SymbolTableVisitor {
    Map<String, ExprVar> varMap;

    public ArrayTypeReplacer(Map<String, ExprVar> varMap) {
        super(null);
        this.varMap = varMap;
    }

    @Override
    public Object visitExprVar(ExprVar exp) {
        String name = exp.getName();
        assert (varMap.containsKey(name));
        return varMap.get(name);
    }

    @Override
    public Object visitTypeArray(TypeArray ta) {
        Type base = ta.getBase();
        base = (Type) base.accept(this);

        Expression length = (Expression) ta.getLength().doExpr(this);
        return new TypeArray(base, length);
    }
}
public class RemoveExprGet extends SymbolTableVisitor {
    TempVarGen varGen;
    FENode context;
    TypeStructRef oriType;
    int maxArrSize;

    public RemoveExprGet(TempVarGen varGen, int arrSize) {
        super(null);
        this.varGen = varGen;
        this.maxArrSize = arrSize;
    }

    @Override
    public Object visitExprGet(ExprGet exp) {

        context = exp;
        oriType = new TypeStructRef(exp.getName(), false);
        List<Statement> newStmts = new ArrayList<Statement>();
        Expression e = processExprGet(exp, newStmts);
        addStatements(newStmts);
        return e;
    }

    private Expression processExprGet(ExprGet exp, List<Statement> newStmts) {
        String tempVar = varGen.nextVar(exp.getName().split("@")[0] + "_");
        Type tt = new TypeStructRef(exp.getName(), false);

        Statement decl = (new StmtVarDecl(exp, tt, tempVar, null));
        newStmts.add(decl);
        // ExprStar hole = new ExprStar(context, 0, exp.getDepth() - 1, 3);
        // hole.makeSpecial(new ArrayList<ExprStar>());
        symtab.registerVar(tempVar, tt, decl, SymbolTable.KIND_LOCAL);
        ExprVar ev = new ExprVar(exp, tempVar);
        assert (tt instanceof TypeStructRef);
        List<ExprStar> depthVars = new ArrayList<ExprStar>();
        // depthVars.add(hole);
        getExpr(ev, (TypeStructRef) tt, exp.getParams(), newStmts, exp.getDepth(),
                depthVars, 1);
        return ev;
    }

    private void getExpr(ExprVar ev, Type type, List<Expression> params,
            List<Statement> newStmts, int depth, List<ExprStar> d, int ht)
    {
        if (type.isArray()) {
            TypeArray ta = (TypeArray) type;
            Type baseType = ta.getBase();
            if (baseType.isStruct()) {
                assert false;
                ExprStar hole = new ExprStar(context);
                Expression cond = hole;
                for (int i = 0; i < d.size(); i++) {
                    cond = new ExprBinary(ExprBinary.BINOP_OR, new ExprBinary(ExprBinary.BINOP_LE, d.get(i),
 new ExprConstInt(i)),
                                    cond);
                }
                Statement ifBlock = getBaseExprs(type, params, ev, depth == 1);
                List<Statement> elseStmts = new ArrayList<Statement>();
                if (depth > 1) {
                    // hole.makeSpecial();
                    List<Expression> arrelems = new ArrayList<Expression>();
                    Expression length = ta.getLength();
                    int size = length.isConstant() ? length.getIValue() : maxArrSize;

                    // TODO: is there a better way of dealing with this
                    for (int i = 0; i < size; i++) {
                        String tempVar = varGen.nextVar(ev.getName().split("@")[0]);
                        Statement decl =
                                (new StmtVarDecl(context, baseType, tempVar, null));
                        elseStmts.add(decl);
                        symtab.registerVar(tempVar, baseType, decl,
                                SymbolTable.KIND_LOCAL);
                        ExprVar newV = new ExprVar(context, tempVar);
                        getExpr(newV, baseType, params, elseStmts, depth, d, ht);
                        arrelems.add(newV);
                    }
                    elseStmts.add(new StmtAssign(context, ev,
                            new ExprArrayRange(context, new ExprArrayInit(context, arrelems),
                                    new ExprArrayRange.RangeLen(ExprConstInt.zero, length))));
                }
                Statement elseBlock = new StmtBlock(elseStmts);
                newStmts.add(new StmtIfThen(context, cond, ifBlock, elseBlock));
                return;
            }
        }
        if (type instanceof TypeStructRef) {
            TypeStructRef tt = (TypeStructRef) type;
            ExprStar hole = new ExprStar(context);
            Expression cond = hole;
            for (int i = 0; i < d.size(); i++) {
                cond =
                        new ExprBinary(ExprBinary.BINOP_OR, new ExprBinary(
                                ExprBinary.BINOP_LE, d.get(i), new ExprConstInt(i)), cond);
            }
            Statement ifBlock = getBaseExprs(tt, params, ev, depth == 1);
            List<Statement> elseStmts = new ArrayList<Statement>();
            if (depth > 1) {
                TypeStructRef oldOriType = oriType;
                oriType = tt;
                createNewAdt(ev, tt, params, elseStmts, depth, true, d, ht);
                oriType = oldOriType;
            }
            Statement elseBlock = new StmtBlock(elseStmts);
            newStmts.add(new StmtIfThen(context, cond, ifBlock, elseBlock));
            return;
            // }
        }
        newStmts.addAll(getBaseExprs(type, params, ev, depth == 1).getStmts());
        return;

    }

    private void createNewAdt(ExprVar ev, TypeStructRef tt, List<Expression> params,
            List<Statement> newStmts, int depth, boolean recursive, List<ExprStar> d,
            int ht)
    {
        String name = tt.getName();
        StructDef sd = nres.getStruct(name);

        if (!recursive && !sd.isInstantiable()) {
            List<String> nonRecCases = getNonRecCases(name);
            boolean first = true;
            Expression curExp = new ExprNullPtr();

            for (String c : nonRecCases) {
                TypeStructRef childType = new TypeStructRef(c, false);
                String vname = varGen.nextVar(ev.getName());
                Statement decl = (new StmtVarDecl(context, tt, vname, null));
                newStmts.add(decl);
                symtab.registerVar(vname, childType, decl, SymbolTable.KIND_LOCAL);
                ExprVar newV = new ExprVar(context, vname);
                createNewAdt(newV, childType, params, newStmts, 1, false,
                        new ArrayList(), 1);
                if (first) {
                    curExp = newV;
                    first = false;
                } else {
                    curExp = new ExprAlt(context, curExp, newV);
                }

            }
            if (curExp instanceof ExprAlt) {
                curExp = new ExprRegen(context, curExp);
            }

            newStmts.add(new StmtAssign(context, ev, curExp));
            return;

        }

        Map<Type, List<ExprVar>> exprVarsMap =
                findParamVars(tt.getName(), newStmts, tt, depth, recursive, params, d, ht);

        //for (Entry<Type, List<ExprVar>> list : exprVarsMap.entrySet()) {
        //    for (ExprVar e : list.getValue()) {
        //        getExpr(e, list.getKey(), params, newStmts, depth - 1);
        //    }
        //}

        // Create a new adt with the exprvars above
        List<ExprNamedParam> expParams = new ArrayList<ExprNamedParam>();
        LinkedList<String> queue = new LinkedList<String>();
        queue.add(name);
        while (!queue.isEmpty()) {
            String curName = queue.pop();
            StructDef cur = nres.getStruct(curName);
            Map<Type, Integer> count = new HashMap<Type, Integer>();
            Map<String, ExprVar> varMap = new HashMap<String, ExprVar>();
            for (StructFieldEnt e : cur.getFieldEntriesInOrder()) {
                Type t = e.getType();
                if (t.isArray()) {
                    TypeArray ta = (TypeArray) t;
                    t = (Type) ta.accept(new ArrayTypeReplacer(varMap));
                }
                if (!count.containsKey(t)) {
                    count.put(t, 0);
                }
                int c = count.get(t);
                List<ExprVar> varsForType = exprVarsMap.get(t);
                if (varsForType == null) {
                    continue;
                }

                ExprVar var;
                if (!t.isArray() && !t.isStruct()) {
                    if (varsForType.isEmpty())
                        assert (false);
                    var = varsForType.remove(0);
                } else {
                    if (c >= varsForType.size())
                        assert (false);
                    var = varsForType.get(c);
                }
                if (t.isArray()) {
                    TypeArray ta = (TypeArray) t;
                    Type baseType = ta.getBase();
                    if (baseType.isStruct()) {
                        List<Expression> arrelems = new ArrayList<Expression>();
                        Expression length = ta.getLength();
                        int size = length.isConstant() ? length.getIValue() : maxArrSize;

                        // TODO: is there a better way of dealing with this
                        for (int i = 0; i < size; i++) {
                            if (!count.containsKey(baseType)) {
                                count.put(baseType, 0);
                            }
                            int c1 = count.get(baseType);
                            count.put(baseType, ++c1);

                        }
                    }
                }
                expParams.add(new ExprNamedParam(context, e.getName(), var));
                varMap.put(e.getName().split("@")[0], var);
                count.put(t, ++c);
            }
            queue.addAll(nres.getStructChildren(curName));

        }
        if (sd.isInstantiable()) {
            newStmts.add(new StmtAssign(ev, new ExprNew(context, tt, expParams, false)));
        } else {
            newStmts.add(new StmtAssign(ev, new ExprNew(context, null, expParams, true)));
        }
    }

    private Map<Type, List<ExprVar>> findParamVars(String name, List<Statement> stmts,
            Type type, int depth, boolean recursive, List<Expression> params,
            List<ExprStar> d, int ht)
    {
        Map<Type, List<ExprVar>> map = new HashMap<Type, List<ExprVar>>();
        LinkedList<String> queue = new LinkedList<String>();
        queue.add(name);
        while (!queue.isEmpty()) {
            String curName = queue.pop();
            StructDef cur = nres.getStruct(curName);
            Map<Type, Integer> count = new HashMap<Type, Integer>();
            Map<String, ExprVar> varMap = new HashMap<String, ExprVar>();
            for (StructFieldEnt e : cur.getFieldEntriesInOrder()) {
                Type t = e.getType();
                if (!recursive && t.promotesTo(type, nres)) {
                    // ignore recursive fields
                    continue;
                }
                if (t.isArray()) {
                    TypeArray ta = (TypeArray) t;
                    t = (Type) ta.accept(new ArrayTypeReplacer(varMap));
                }
                if (!count.containsKey(t)) {
                    count.put(t, 0);
                }
                if (!map.containsKey(t)) {
                    map.put(t, new ArrayList<ExprVar>());
                }
                int c = count.get(t);
                List<ExprVar> varsForType = map.get(t);
                if (c >= varsForType.size() || (!t.isArray() && !t.isStruct())) {
                    String tempVar = varGen.nextVar(e.getName().split("@")[0]);
                    Statement decl = (new StmtVarDecl(context, t, tempVar, null));
                    stmts.add(decl);
                    symtab.registerVar(tempVar, t, decl, SymbolTable.KIND_LOCAL);
                    ExprVar ev = new ExprVar(context, tempVar);
                    varsForType.add(ev);

                    List<ExprStar> newDepths = new ArrayList<ExprStar>();
                    for (int i = 0; i < d.size(); i++) {
                        newDepths.add(d.get(i));
                    }
                    if (depth > 2 && t.promotesTo(type, nres)) {
                        ExprStar hole = new ExprStar(context, 0, depth - 2, 3);
                        hole.makeSpecial(d);
                        newDepths.add(0, hole);

                    }
                    boolean done = false;
                    if (t.isArray()) {
                        TypeArray ta = (TypeArray) t;
                        Type baseType = ta.getBase();
                        if (baseType.isStruct()) {
                            List<Expression> arrelems = new ArrayList<Expression>();
                            Expression length = ta.getLength();
                            int size =
                                    length.isConstant() ? length.getIValue() : maxArrSize;

                            // TODO: is there a better way of dealing with this
                            for (int i = 0; i < size; i++) {
                                if (!count.containsKey(baseType)) {
                                    count.put(baseType, 0);
                                }
                                if (!map.containsKey(baseType)) {
                                    map.put(baseType, new ArrayList<ExprVar>());
                                }
                                int c1 = count.get(baseType);
                                List<ExprVar> varsForType1 = map.get(baseType);
                                if (c1 >= varsForType1.size()) {
                                    String tempVar1 = varGen.nextVar(e.getName().split("@")[0]);
                                    Statement decl1 = (new StmtVarDecl(context, baseType, tempVar1, null));
                                    stmts.add(decl1);
                                    symtab.registerVar(tempVar, baseType, decl1, SymbolTable.KIND_LOCAL);
                                    ExprVar ev1 = new ExprVar(context, tempVar1);
                                    varsForType1.add(ev1);

                                    List<ExprStar> newDepths1 = new ArrayList<ExprStar>();
                                    for (int i1 = 0; i1 < d.size(); i1++) {
                                        newDepths1.add(d.get(i1));
                                    }
                                    if (depth > 2 && baseType.promotesTo(type, nres)) {
                                        ExprStar hole = new ExprStar(context, 0, depth - 2, 3);
                                        hole.makeSpecial(d);
                                        newDepths1.add(0, hole);
                                    }
                                    getExpr(ev1, baseType, params, stmts, depth - 1, newDepths1, ht + 1);
                               
                                }
                                arrelems.add(varsForType1.get(c1));
                                count.put(baseType, ++c1);
                            }
                            ExprStar hole = new ExprStar(context);
                            Expression cond = hole;
                            Statement ifBlock = getBaseExprs(t, params, ev, depth == 1);
                            Statement elseBlock =
                                    new StmtAssign(context, ev,
                                            new ExprArrayRange(context,
                                                    new ExprArrayInit(context, arrelems),
                                                    new ExprArrayRange.RangeLen(
                                                            ExprConstInt.zero, length)));
                            stmts.add(new StmtIfThen(context, cond, ifBlock, elseBlock));
                            done = true;
                        }

                    }
                    if (!done) {
                        getExpr(ev, t, params, stmts, depth - 1, newDepths, ht + 1);
                    }
                }
                if (!t.isArray() && !t.isStruct()) {
                    varMap.put(e.getName().split("@")[0],
                            varsForType.get(varsForType.size() - 1));
                } else {
                    varMap.put(e.getName().split("@")[0], varsForType.get(c));
                }
                count.put(t, ++c);
            }
            queue.addAll(nres.getStructChildren(curName));
        }
        return map;
    }

    private StmtBlock getBaseExprs(Type type, List<Expression> params, ExprVar var,
            boolean considerNonRec)
    {
        List<Statement> stmts = new ArrayList<Statement>();
        List<Expression> baseExprs = getExprsOfType(params, type);
        boolean first = true;
        Expression curExp = null;
        for (Expression e : baseExprs) {
            // if (e instanceof ExprGet) {
            // e = processExprGet((ExprGet) e, stmts);
            // }
            String tmp = varGen.nextVar();
            stmts.add(new StmtVarDecl(context, type, tmp, e));
            if (first) {
                curExp = new ExprVar(context, tmp);
                first = false;
            } else {
                curExp = new ExprAlt(context, curExp, new ExprVar(context, tmp));
            }
        }

        Expression finExp = getGeneralExprOfType(type, params, stmts, considerNonRec);
        if (finExp != null) {
            if (curExp == null) {
                stmts.add(new StmtAssign(var, finExp));
            } else {
                stmts.add(new StmtAssign(var, new ExprRegen(context, new ExprAlt(curExp,
                        finExp))));
            }
        } else {
            if (curExp == null) {
                stmts.add(new StmtAssign(var, new ExprNullPtr()));
            } else {
                stmts.add(new StmtAssign(var, new ExprRegen(context, curExp)));
            }
        }
        return new StmtBlock(stmts);
    }

    private Expression getGeneralExprOfType(Type type,
 List<Expression> params,
            List<Statement> stmts, boolean considerNonRec)
    {
        if (checkType(type)) {
            if (type.isArray()) {
                TypeArray ta = (TypeArray) type;
                List<Expression> arrelems = new ArrayList<Expression>();
                Expression length = ta.getLength();
                int size = length.isConstant() ? length.getIValue() : maxArrSize;
                // TODO: is there a better way of dealing with this
                for (int i = 0; i < size; i++) {
                    arrelems.add(getGeneralExprOfType(ta.getBase(), params, stmts,
                            considerNonRec));
                }
                String tmp = varGen.nextVar();
                stmts.add(new StmtVarDecl(context, ta,tmp, new ExprArrayRange(context, new ExprArrayInit(context, arrelems),
                        new ExprArrayRange.RangeLen(ExprConstInt.zero, length))));
                return new ExprVar(context, tmp);

            } else {
                return new ExprStar(context);
            }
        } else if (considerNonRec && type instanceof TypeStructRef) {
            boolean rec = true;
            if (type.promotesTo(oriType, nres)) {
                rec = false;
            }
            TypeStructRef tt = (TypeStructRef) type;

            String tempVar = varGen.nextVar(tt.getName().split("@")[0] + "_");
            Statement decl = (new StmtVarDecl(context, tt, tempVar, null));
            stmts.add(decl);
            symtab.registerVar(tempVar, tt, decl, SymbolTable.KIND_LOCAL);
            ExprVar ev = new ExprVar(context, tempVar);
            System.out.println("Creating a depth 1 " + tt + " tree");
            TypeStructRef oldOriType = oriType;
            oriType = tt;
            createNewAdt(ev, tt, params, stmts, 1, rec, new ArrayList(), 1);
            oriType = oldOriType;
            return ev;

        }
        return type.defaultValue();
    }

    private boolean checkType(Type type) {
        if (type.isArray()) {
            TypeArray t = (TypeArray) type;
            return checkType(t.getBase());
        } else {
            return type.promotesTo(TypePrimitive.inttype, nres);
        }
    }

    private List<Expression> getExprsOfType(List<Expression> params, Type tt) {
        List<Expression> filteredExprs = new ArrayList<Expression>();
        for (Expression exp : params) {
            Type t = getType(exp);
            if (t.isArray() && !tt.isArray()) {
                TypeArray ta = (TypeArray) t;
                Type base = ta.getBase();
                if (base.promotesTo(tt, nres)) {
                    if (!(exp instanceof ExprVar)) {
                        exp = (Expression) (new Clone()).process(exp).accept(this);
                    }
                    filteredExprs.add(new ExprArrayRange(exp, new ExprStar(exp)));
                }
            }
            if (t.promotesTo(tt, nres)) {
                if (!(exp instanceof ExprVar)) {
                    exp = (Expression) (new Clone()).process(exp).accept(this);
                }
                if (t.isArray() && tt.isArray()) {
                    exp =
                            new ExprArrayRange(context, exp, new ExprArrayRange.RangeLen(
                                    ExprConstInt.zero, ((TypeArray) tt).getLength()));
                }
                filteredExprs.add(exp);
            }
        }
        return filteredExprs;
    }

    private List<String> getNonRecCases(String name) {
        List<String> nonRecCases = new ArrayList<String>();
        LinkedList<String> queue = new LinkedList<String>();
        queue.add(name);
        while (!queue.isEmpty()) {
            String n = queue.removeFirst();
            List<String> children = nres.getStructChildren(n);
            if (children.isEmpty()) {
                StructDef ts = nres.getStruct(n);
                boolean nonRec = true;
                for (StructFieldEnt f : ts.getFieldEntriesInOrder()) {
                    if (!checkType(f.getType())) {
                        nonRec = false;
                        break;
                    }
                }
                if (nonRec) {
                    nonRecCases.add(n);
                }

            } else {
                queue.addAll(children);
            }
        }
        return nonRecCases;

    }
}
