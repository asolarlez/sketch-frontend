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
            newStar.makeSpecial();
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
        String tempVar = varGen.nextVar(exp.getName().split("@")[0]);
        Type tt = new TypeStructRef(exp.getName(), false);

        Statement decl = (new StmtVarDecl(exp, tt, tempVar, null));
        newStmts.add(decl);
        String depthVar = varGen.nextVar("depth");
        newStmts.add(new StmtVarDecl(exp, TypePrimitive.inttype, depthVar,
                new ExprConstInt(exp.getDepth())));
        symtab.registerVar(tempVar, tt, decl, SymbolTable.KIND_LOCAL);
        symtab.registerVar(depthVar, TypePrimitive.inttype);
        ExprVar ev = new ExprVar(exp, tempVar);
        ExprVar d = new ExprVar(exp, depthVar);
        assert (tt instanceof TypeStructRef);
        getExpr(ev, (TypeStructRef) tt, exp.getParams(), newStmts, exp.getDepth(), d);
        return ev;
    }

    private void getExpr(ExprVar ev, Type type, List<Expression> params,
            List<Statement> newStmts, int depth, Expression d)
    {
        if (type.isArray()) {
            TypeArray ta = (TypeArray) type;
            Type baseType = ta.getBase();
            if (baseType.isStruct()) {
                ExprStar hole = new ExprStar(context);
                Expression dCon =
                        new ExprBinary(ExprBinary.BINOP_LE, d, ExprConstInt.zero);
                Statement ifBlock = getBaseExprs(type, params, ev);
                List<Statement> elseStmts = new ArrayList<Statement>();
                if (depth > 1) {
                    hole.makeSpecial();
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
                        getExpr(newV, baseType, params, elseStmts, depth, d);
                        arrelems.add(newV);
                    }
                    elseStmts.add(new StmtAssign(context, ev,
                            new ExprArrayRange(context, new ExprArrayInit(context, arrelems),
                                    new ExprArrayRange.RangeLen(ExprConstInt.zero, length))));
                }
                Statement elseBlock = new StmtBlock(elseStmts);
                newStmts.add(new StmtIfThen(context, new ExprBinary(ExprBinary.BINOP_OR,
                        dCon, hole), ifBlock, elseBlock));
                return;
            }
        }
        if (type instanceof TypeStructRef) {
            // if (type.promotesTo(oriType, nres)) {
            TypeStructRef tt = (TypeStructRef) type;
            ExprStar hole = new ExprStar(context);
            Expression dCon = new ExprBinary(ExprBinary.BINOP_LE, d, ExprConstInt.zero);
            Statement ifBlock = getBaseExprs(tt, params, ev);
            List<Statement> elseStmts = new ArrayList<Statement>();
            if (depth > 1) {
                hole.makeSpecial();
                createNewAdt(ev, tt, params, elseStmts, depth, true, d);
            }
            Statement elseBlock = new StmtBlock(elseStmts);
            newStmts.add(new StmtIfThen(context, new ExprBinary(ExprBinary.BINOP_OR,
                    dCon, hole), ifBlock, elseBlock));
            return;
            // }
        }
        newStmts.add(getBaseExprs(type, params, ev));
        return;

    }

    private void createNewAdt(ExprVar ev, TypeStructRef tt, List<Expression> params,
            List<Statement> newStmts, int depth, boolean recursive, Expression d)
    {
        Map<Type, List<ExprVar>> exprVarsMap =
                findParamVars(tt.getName(), newStmts, tt, depth, recursive, params, d);

        //for (Entry<Type, List<ExprVar>> list : exprVarsMap.entrySet()) {
        //    for (ExprVar e : list.getValue()) {
        //        getExpr(e, list.getKey(), params, newStmts, depth - 1);
        //    }
        //}

        // Create a new adt with the exprvars above
        String name = tt.getName();
        StructDef sd = nres.getStruct(name);
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
                if (c >= varsForType.size())
                    assert (false);
                expParams.add(new ExprNamedParam(context, e.getName(), varsForType.get(c)));
                varMap.put(e.getName().split("@")[0], varsForType.get(c));
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
            Type type, int depth, boolean recursive, List<Expression> params, Expression d)
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
                if (c >= varsForType.size()) {
                    String tempVar = varGen.nextVar(e.getName().split("@")[0]);
                    Statement decl = (new StmtVarDecl(context, t, tempVar, null));
                    stmts.add(decl);
                    symtab.registerVar(tempVar, t, decl, SymbolTable.KIND_LOCAL);
                    ExprVar ev = new ExprVar(context, tempVar);
                    varsForType.add(ev);

                    Expression newDepth =
                            new ExprBinary(ExprBinary.BINOP_SUB, d, ExprConstInt.one);
                    getExpr(ev, t, params, stmts, depth - 1, newDepth);
                }
                varMap.put(e.getName().split("@")[0], varsForType.get(c));
                count.put(t, ++c);
            }
            queue.addAll(nres.getStructChildren(curName));
        }
        return map;
    }

    private Statement getBaseExprs(Type type, List<Expression> params, ExprVar var)
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

        Expression finExp = getGeneralExprOfType(type, params, stmts);
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
            List<Expression> params, List<Statement> stmts)
    {
        if (checkType(type)) {
            if (type.isArray()) {
                TypeArray ta = (TypeArray) type;
                List<Expression> arrelems = new ArrayList<Expression>();
                Expression length = ta.getLength();
                int size = length.isConstant() ? length.getIValue() : maxArrSize;
                // TODO: is there a better way of dealing with this
                for (int i = 0; i < size; i++) {
                    arrelems.add(getGeneralExprOfType(ta.getBase(), params, stmts));
                }
                String tmp = varGen.nextVar();
                stmts.add(new StmtVarDecl(context, ta,tmp, new ExprArrayRange(context, new ExprArrayInit(context, arrelems),
                        new ExprArrayRange.RangeLen(ExprConstInt.zero, length))));
                return new ExprVar(context, tmp);

            } else {
                return new ExprStar(context);
            }
        } else if (type instanceof TypeStructRef) {
            if (type.equals(oriType)) {
                return null;
            }
            TypeStructRef tt = (TypeStructRef) type;

            String tempVar = varGen.nextVar(tt.getName().split("@")[0]);
            Statement decl = (new StmtVarDecl(context, tt, tempVar, null));
            stmts.add(decl);
            symtab.registerVar(tempVar, tt, decl, SymbolTable.KIND_LOCAL);
            ExprVar ev = new ExprVar(context, tempVar);
            createNewAdt(ev, tt, params, stmts, 1, false, ExprConstInt.one);
            return ev;

        }
        return null;
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
            if (t.promotesTo(tt, nres)) {
                if (!(exp instanceof ExprVar)) {
                    exp = (Expression) (new Clone()).process(exp).accept(this);
                }
                filteredExprs.add(exp);
            }
        }
        return filteredExprs;
    }
}
