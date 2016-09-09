package sketch.compiler.passes.bidirectional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import sketch.compiler.ast.core.FENode;
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
import sketch.compiler.dataflow.CloneHoles;
import sketch.compiler.passes.lowering.SymbolTableVisitor;

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

/**
 * This class expands GUCs into constructors of some maximum depth.
 */
public class RemoveADTHoles extends BidirectionalPass {
    TempVarGen varGen;
    FENode context;
    TypeStructRef oriType;
    int maxArrSize;
    int gucDepth;

    public RemoveADTHoles(TempVarGen varGen, int arrSize, int gucDepth) {
        this.varGen = varGen;
        this.maxArrSize = arrSize;
        this.gucDepth = gucDepth;
    }

    @Override
    public Object visitExprStar(ExprStar exp) {
		Type t = driver.tdstate.getExpected();
		if (t != null && t.isStruct()) {
            TypeStructRef ts = (TypeStructRef) t;
            oriType = ts;
            context = exp;
            List<Statement> newStmts = new ArrayList<Statement>();
            Expression e = processADTHole(ts, new ArrayList<Expression>(), newStmts);
            addStatements(newStmts);
            return e;
        }
        return exp;
    }

    @Override
    public Object visitExprADTHole(ExprADTHole exp) {
		Expression newexp = (Expression) super.visitExprADTHole(exp);
		if (!(newexp instanceof ExprADTHole)) {
			return newexp;
		} else {
			exp = (ExprADTHole) newexp;
		}
        TypeStructRef ts =  (TypeStructRef) driver.tdstate.getExpected();// new TypeStructRef(exp.getName(), false);
        context = exp;
        oriType = ts;
        List<Statement> newStmts = new ArrayList<Statement>();
		Expression e;
		if (exp.isSimple()) {
			e = processSimpleADTHole(ts, exp.getParams(), newStmts);
		} else {
			e = processADTHole(ts, exp.getParams(), newStmts);
		}
        addStatements(newStmts);
        return e;
    }

	private Expression processSimpleADTHole(TypeStructRef type,
			List<Expression> params, List<Statement> newStmts) {
		String tempVar = varGen.nextVar(type.getName().split("@")[0] + "_");
		Statement decl = (new StmtVarDecl(context, type, tempVar, null));
		newStmts.add(decl);
		symtab().registerVar(tempVar, type, decl, SymbolTable.KIND_LOCAL);
		ExprVar ev = new ExprVar(context, tempVar);

		List<ExprVar> depthHoles = new ArrayList<ExprVar>();
		createNewAdt(ev, type, params, newStmts, 1, true, depthHoles);
		return ev;
	}

    private Expression processADTHole(TypeStructRef type, List<Expression> params,
 List<Statement> newStmts)
    {
        String tempVar = varGen.nextVar(type.getName().split("@")[0] + "_");
        Statement decl = (new StmtVarDecl(context, type, tempVar, null));
        newStmts.add(decl);
		symtab().registerVar(tempVar, type, decl, SymbolTable.KIND_LOCAL);
        ExprVar ev = new ExprVar(context, tempVar);

		List<ExprVar> depthVars = new ArrayList<ExprVar>();
		getExprTree(ev, type, params, newStmts, gucDepth, depthVars);
        return ev;
    }

    /*
     * Creates a general expression tree using params as leaves.
     * 
     * If type is a TypeStructRef, the form is as below:
     * if (??) {
     *      ev = {|...|};
     * } else {
     *      ev = new ??(...);
     * }
     * 
     */
    private void getExprTree(ExprVar ev, Type type, List<Expression> params,
			List<Statement> newStmts, int depth, List<ExprVar> depthHoles)
    {
        if (type instanceof TypeStructRef) {
            TypeStructRef tt = (TypeStructRef) type;
            ExprStar hole = new ExprStar(context);
            Expression cond = hole;
            for (int i = 0; i < depthHoles.size(); i++) {
                cond =
                        new ExprBinary(ExprBinary.BINOP_OR, new ExprBinary(
                                ExprBinary.BINOP_LE, depthHoles.get(i), new ExprConstInt(i)), cond);
            }
            Statement ifBlock = getBaseExprs(tt, params, ev, depth == 1);
            List<Statement> elseStmts = new ArrayList<Statement>();
            if (depth > 1) {
                TypeStructRef oldOriType = oriType;
                oriType = tt;
                createNewAdt(ev, tt, params, elseStmts, depth, true, depthHoles);
                oriType = oldOriType;
            }
            Statement elseBlock = new StmtBlock(elseStmts);
            newStmts.add(new StmtIfThen(context, cond, ifBlock, elseBlock));
            return;
        }
        newStmts.addAll(getBaseExprs(type, params, ev, depth == 1).getStmts());
        return;
    }

    /*
     * Creates a new unknown constructor of the form new ??(...) of type tt. The
     * parameters of this unknown constructor are recursively generated expression trees.
     */
    private void createNewAdt(ExprVar ev, TypeStructRef tt, List<Expression> params,
 List<Statement> newStmts, int depth,
			boolean recursive, List<ExprVar> depthHoles)
    {
        String name = tt.getName();
		StructDef sd = nres().getStruct(name);

        if (!recursive && !sd.isInstantiable()) {
            List<String> nonRecCases = getNonRecCases(name);
            boolean first = true;
            Expression curExp = new ExprNullPtr();

            for (String c : nonRecCases) {
                TypeStructRef childType = new TypeStructRef(c, false);
                String vname = varGen.nextVar(ev.getName());
                Statement decl = (new StmtVarDecl(context, tt, vname, null));
                newStmts.add(decl);
				symtab().registerVar(vname, childType, decl,
						SymbolTable.KIND_LOCAL);
                ExprVar newV = new ExprVar(context, vname);
                createNewAdt(newV, childType, params, newStmts, 1, false, new ArrayList());
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

        List<ExprNamedParam> expParams =
                createADTParams(tt.getName(), newStmts, tt, depth, recursive, params, depthHoles);

        // Create a new adt with the exprvars above
        if (sd.isInstantiable()) {
            newStmts.add(new StmtAssign(ev, new ExprNew(context, tt, expParams, false)));
        } else {
			newStmts.add(new StmtAssign(ev, new ExprNew(context, null, expParams,
					true)));
        }
    }

    /*
     * Creates a list of params to be used in new ??(...). This function also merges
     * expression sub trees that are mutually exclusive. For ex, If type is an ADT defined
     * as follows: adt Foo { A {Foo a; Foo b;} B {Foo a; Foo b;} } For this type, we only
     * generate 2 subtrees of type Foo instead of 4.
     */
    private List<ExprNamedParam> createADTParams(String name, List<Statement> stmts,
            Type type, int depth, boolean recursive, List<Expression> params,
 List<ExprVar> depthHoles)
    {
        List<ExprNamedParam> newADTparams = new ArrayList<ExprNamedParam>();
        Map<Type, List<ExprVar>> map = new HashMap<Type, List<ExprVar>>();
        LinkedList<String> queue = new LinkedList<String>();
        queue.add(name);
        while (!queue.isEmpty()) {
            String curName = queue.pop();
			StructDef cur = nres().getStruct(curName);
            Map<Type, Integer> count = new HashMap<Type, Integer>();
            Map<String, ExprVar> varMap = new HashMap<String, ExprVar>();
            for (StructFieldEnt e : cur.getFieldEntriesInOrder()) {
                Type t = e.getType();
				if (!recursive && t.promotesTo(type, nres())) {
                    // ignore recursive fields
                    continue;
                }
                if (t.isArray()) {
                    TypeArray ta = (TypeArray) t;
                    t = (Type) ta.accept(new ArrayTypeReplacer(varMap));
                }
                ExprVar var =
                        getExprVarForParam(t, count, map, e.getName(), stmts, depthHoles,
                                depth,
                                type,
                                params);
                newADTparams.add(new ExprNamedParam(context, e.getName(), var, curName));
                varMap.put(e.getName(), var);
            }
			queue.addAll(nres().getStructChildren(curName));
        }
        return newADTparams;
    }

    private ExprVar getExprVarForParam(Type t, Map<Type, Integer> count,
            Map<Type, List<ExprVar>> map, String fName, List<Statement> stmts,
			List<ExprVar> depthHoles, int depth, Type adtType,
			List<Expression> params)
    {
        if (!count.containsKey(t)) {
            count.put(t, 0);
        }
        if (!map.containsKey(t)) {
            map.put(t, new ArrayList<ExprVar>());
        }
        int c = count.get(t);
        List<ExprVar> varsForType = map.get(t);
        if (c >= varsForType.size() || (!t.isArray() && !t.isStruct())) {
            String tempVar = varGen.nextVar(fName.split("@")[0]);
            Statement decl = (new StmtVarDecl(context, t, tempVar, null));
            stmts.add(decl);
			symtab().registerVar(tempVar, t, decl, SymbolTable.KIND_LOCAL);
            ExprVar ev = new ExprVar(context, tempVar);
            varsForType.add(ev);

			List<ExprVar> newDepths = new ArrayList<ExprVar>();
            for (int i = 0; i < depthHoles.size(); i++) {
                newDepths.add(depthHoles.get(i));
            }
			if (depth > 2 && t.promotesTo(adtType, nres())) {
				String hvar = varGen.nextVar("_h");
                ExprStar hole = new ExprStar(context, 0, depth - 2, 3);
                hole.makeSpecial(depthHoles);
				stmts.add(new StmtVarDecl(context, TypePrimitive.inttype, hvar,
						hole));
				newDepths.add(0, new ExprVar(context, hvar));

            }
            boolean done = false;
            if (t.isArray()) {
                TypeArray ta = (TypeArray) t;
                Type baseType = ta.getBase();
                if (baseType.isStruct()) {
                    List<Expression> arrelems = new ArrayList<Expression>();
                    Expression length = ta.getLength();
                    int size = length.isConstant() ? length.getIValue() : maxArrSize;

                    // TODO: is there a better way of dealing with this
                    for (int i = 0; i < size; i++) {
                        ExprVar v =
                                getExprVarForParam(baseType, count, map, fName, stmts,
                                        depthHoles,
                                        depth, adtType, params);
                        arrelems.add(v);
                    }
                    ExprStar hole = new ExprStar(context);
                    Expression cond = hole;
                    Statement ifBlock = getBaseExprs(t, params, ev, depth == 1);
                    Statement elseBlock =
                            new StmtAssign(context, ev,
                                    new ExprArrayRange(context, new ExprArrayInit(
                                            context, arrelems),
                                            new ExprArrayRange.RangeLen(
                                                    ExprConstInt.zero, length)));
                    stmts.add(new StmtIfThen(context, cond, ifBlock, elseBlock));
                    done = true;
                }

            }
            if (!done) {
                getExprTree(ev, t, params, stmts, depth - 1, newDepths);
            }
        }
        ExprVar var;
        if (!t.isArray() && !t.isStruct()) {
            var = varsForType.get(varsForType.size() - 1);
        } else {
            var = varsForType.get(c);
        }

        count.put(t, ++c);
        return var;
    }

    /*
     * Retrieves base expressions of a particular type. Base exprsesions include
     * expressions passed as parameters to GUC and ?? (for primitive types).
     */
    private StmtBlock getBaseExprs(Type type, List<Expression> params, ExprVar var,
            boolean considerNonRec)
    {
        List<Statement> stmts = new ArrayList<Statement>();
        List<Expression> baseExprs = getExprsOfType(params, type);
        boolean first = true;
        Expression curExp = null;
        for (Expression e : baseExprs) {
            String tmp = varGen.nextVar("tmp");
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

    private Expression getGeneralExprOfType(Type type, List<Expression> params,
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
                String tmp = varGen.nextVar("tmp");
                stmts.add(new StmtVarDecl(context, ta,tmp, new ExprArrayRange(context, new ExprArrayInit(context, arrelems),
                        new ExprArrayRange.RangeLen(ExprConstInt.zero, length))));
                return new ExprVar(context, tmp);

            } else {
				if (type.promotesTo(TypePrimitive.inttype, nres()))
					return new ExprStar(context);
				else
					return new ExprNullPtr();
            }
        } else if (considerNonRec && type instanceof TypeStructRef) {
            boolean rec = true;
			if (type.promotesTo(oriType, nres())) {
                rec = false;
            }
            TypeStructRef tt = (TypeStructRef) type;

            String tempVar = varGen.nextVar(tt.getName().split("@")[0] + "_");
            Statement decl = (new StmtVarDecl(context, tt, tempVar, null));
            stmts.add(decl);
			symtab().registerVar(tempVar, tt, decl, SymbolTable.KIND_LOCAL);
            ExprVar ev = new ExprVar(context, tempVar);
            TypeStructRef oldOriType = oriType;
            oriType = tt;
            createNewAdt(ev, tt, params, stmts, 1, rec, new ArrayList());
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
			if (type.promotesTo(TypePrimitive.inttype, nres())) {
				return true;
			}
			if (type.isStruct()) {
				TypeStructRef ts = (TypeStructRef) type;
				StructDef sd = nres().getStruct(ts.getName());
				if (sd.isInstantiable() && sd.getNumFields() == 0) {
					return true;
				}
			}
        }
		return false;
    }

    /*
     * Filters expressions in params that promote to type tt.
     */
    private List<Expression> getExprsOfType(List<Expression> params, Type tt) {
        List<Expression> filteredExprs = new ArrayList<Expression>();
        for (Expression exp : params) {
			Type t = driver.getType(exp);
			if (t.isStruct()
					&& (nres().isTemplate(((TypeStructRef) t).getName()) || nres()
							.getStruct(((TypeStructRef) t).getName()) == null)) {
				driver.tdstate.beforeRecursion(tt, exp);
				Expression newexp = driver.doExpression(exp);
				filteredExprs.add(newexp);
				driver.tdstate.beforeRecursion(null, null);
				continue;
			}
            if (t.isArray() && !tt.isArray()) {
                TypeArray ta = (TypeArray) t;
                Type base = ta.getBase();
				if (base.promotesTo(tt, nres())
						&& (!ta.getLength().isConstant() || ta.getLength()
								.getIValue() > 0)) {
                    if (!(exp instanceof ExprVar)) {
                        exp = (Expression) (new CloneHoles()).process(exp).accept(this);
                    }
                    filteredExprs.add(new ExprTernary(exp, ExprTernary.TEROP_COND, 
                    		new ExprBinary(exp, ExprBinary.BINOP_GT, ta.getLength(), ExprConstInt.zero),
                    		new ExprArrayRange(exp, new ExprStar(exp)), tt.defaultValue()));
                }
            }
			if (t.promotesTo(tt, nres())) {
				if (tt.isStruct() && t.equals(TypePrimitive.bottomtype)) {
					// skip
					continue;
				}
                if (!(exp instanceof ExprVar)) {
                    exp = (Expression) (new CloneHoles()).process(exp).accept(this);
                }
                if (t.isArray() && tt.isArray()) {
                    exp =
                    		new ExprTernary(exp, ExprTernary.TEROP_COND, 
                            new ExprBinary(exp, ExprBinary.BINOP_GT, ((TypeArray) t).getLength(), ExprConstInt.zero),
                             new ExprArrayRange(context, exp, 
                            		 new ExprArrayRange.RangeLen(ExprConstInt.zero, ((TypeArray) tt).getLength())),
                             tt.defaultValue());
                    		
                           ;
                }
                filteredExprs.add(exp);
            }
        }
        return filteredExprs;
    }

    /*
     * Returns a list of non recursive cases of an ADT.
     */
    private List<String> getNonRecCases(String name) {
        List<String> nonRecCases = new ArrayList<String>();
        LinkedList<String> queue = new LinkedList<String>();
        queue.add(name);
        while (!queue.isEmpty()) {
            String n = queue.removeFirst();
			List<String> children = nres().getStructChildren(n);
            if (children.isEmpty()) {
				StructDef ts = nres().getStruct(n);
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
