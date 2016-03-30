package sketch.compiler.passes.preprocessing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.SymbolTable;
import sketch.compiler.ast.core.exprs.ExprADTHole;
import sketch.compiler.ast.core.exprs.ExprBinary;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.ExprNamedParam;
import sketch.compiler.ast.core.exprs.ExprNew;
import sketch.compiler.ast.core.exprs.ExprStar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtReturn;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.StructDef;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.ast.core.typs.TypeStructRef;
import sketch.compiler.passes.lowering.SymbolTableVisitor;
import sketch.util.exceptions.ExceptionAtNode;

/*
 * Do relevant type inference for new ??() and GUC.
 */

public class TypeInferenceForADTHoles extends SymbolTableVisitor {
	protected Type expType = null;
    Type returnType = null;

    public TypeInferenceForADTHoles() {
        super(null);
    }

    @Override
    public Object visitStmtAssign(StmtAssign s) {
        Type t = getType(s.getLHS());
		expType = t;
        Expression right = s.getRHS();
        Expression newRight = right.doExpr(this);
		expType = null;
        if (right != newRight) {
            return new StmtAssign(s, s.getLHS(), newRight);
        }
        return s;
    }

	@Override
	public Object visitStmtVarDecl(StmtVarDecl s) {
		List<Expression> newInits = new ArrayList<Expression>();
		List<Type> newTypes = new ArrayList<Type>();
		boolean changed = false;
		for (int i = 0; i < s.getNumVars(); i++) {
			Type ot = s.getType(i);
			expType = ot;
			Expression oinit = s.getInit(i);
			Expression init = null;
			if (oinit != null)
				init = doExpression(oinit);
			expType = null;
			Type nt = (Type) ot.accept(this);
			symtab.registerVar(s.getName(i), ot, s, SymbolTable.KIND_LOCAL);
			if (ot != nt || oinit != init) {
				changed = true;
			}
			newInits.add(init);
			newTypes.add(nt);
		}
		if (!changed) {
			return s;
		}
		return new StmtVarDecl(s, newTypes, s.getNames(), newInits);
	}

    @Override
    public Object visitExprFunCall(ExprFunCall exp) {
		Type ori = expType;
        Function callee = nres.getFun(exp.getName());
        List<Expression> params = exp.getParams();
        for (int i = 0; i < params.size(); i++) {
            Expression actual = params.get(i);
            Parameter p = callee.getParams().get(i);
            Type t = p.getType();
			expType = t;
            doExpression(actual);
			expType = null;
        }
		expType = ori;
        return super.visitExprFunCall(exp);
    }

    @Override
    public Object visitExprADTHole(ExprADTHole exp) {
        if (exp.getName() == null) {
			if (expType.promotesTo(TypePrimitive.inttype, nres)) {
				return new ExprStar(exp);
			}
			if (expType.isStruct()) {
				TypeStructRef ts = (TypeStructRef) expType;
				exp = new ExprADTHole(exp, exp.getParams(), exp.isSimple());
				exp.setName(ts.getName());
				expType = null;
			}
        }
        return exp;
    }

    @Override
    public Object visitStmtReturn(StmtReturn stmt) {
		expType = returnType;
        return super.visitStmtReturn(stmt);
    }

    @Override
    public Object visitFunction(Function f) {
        Type prevReturnType = returnType;
        returnType = f.getReturnType();
        Object o = super.visitFunction(f);
        returnType = prevReturnType;
		expType = null;
        return o;
    }

    @Override
    public Object visitExprNew(ExprNew exp){
        if (exp.isHole() && exp.getTypeToConstruct() == null){
			if (expType != null && expType.isStruct()) {
                ExprStar star = new ExprStar(exp, 5, TypePrimitive.int32type);
                exp =
 new ExprNew(exp.getContext(), expType, exp.getParams(),
						true);
                exp.setStar(star);
				expType = null;
            }else{
                throw new ExceptionAtNode("Type must be of type struct", exp);
            }
        }
        
        Type nt = null;
        if (exp.getTypeToConstruct() != null) {
            nt = (Type) exp.getTypeToConstruct().accept(this);
        }
        StructDef str = null;
        {
            assert nt instanceof TypeStructRef;
            str = nres.getStruct(((TypeStructRef) nt).getName());
        }
        boolean changed = false;
        List<ExprNamedParam> enl =
                new ArrayList<ExprNamedParam>(exp.getParams().size());
        for (ExprNamedParam en : exp.getParams()) {
            Type type;
            if (exp.isHole()) {
                type  = getFieldsMap(str).get(en.getName());
            } else {
                type = str.getFieldTypMap().get(en.getName());
            }
            StructDef cur = str;
            while (type == null) {
                cur = nres.getStruct(cur.getParentName());
                type = cur.getFieldTypMap().get(en.getName());
            }
			expType = type;
            Expression old = en.getExpr();
            Expression rhs = doExpression(old);
			expType = null;
            if (rhs != old) {
                enl.add(new ExprNamedParam(en, en.getName(), rhs, en.getVariantName()));
                changed = true;
            } else {
                enl.add(en);
            }
        }

        if (nt != exp.getTypeToConstruct() || changed) {
            if (!changed) {
                enl = exp.getParams();
            }
            return new ExprNew(exp, nt, enl, exp.isHole(), exp.getStar());
        } else {
            return exp;
        }
        
        
    }

    Map<String, Map<String, Type>> fTypesMap = new HashMap<String, Map<String, Type>>();
    private Map<String, Type> getFieldsMap(StructDef ts) {
        String strName = ts.getFullName();
        if (fTypesMap.containsKey(strName)) {
            return fTypesMap.get(strName);
        } else {
            Map<String, Type> fieldsMap = new HashMap<String, Type>();
            LinkedList<String> queue = new LinkedList<String>();
            queue.add(strName);
            while (!queue.isEmpty()) {
                String current = queue.removeFirst();
                StructDef curStruct = nres.getStruct(current);
                List<String> children = nres.getStructChildren(current);
                queue.addAll(children);
                for (Entry<String, Type> field : curStruct.getFieldTypMap()) {
                    String name = field.getKey();
                    Type type = field.getValue();
                    if (fieldsMap.containsKey(name) && !fieldsMap.get(name).equals(type)) {
                        //throw error
                        throw new ExceptionAtNode("Two fields with name = " + name +
                                " and different types. Rename one of them.", ts);
                    } else {
                        fieldsMap.put(name, type);
                    }
                }
            }
            fTypesMap.put(strName, fieldsMap);
            return fieldsMap;
        }
    }

	@Override
	public Object visitExprBinary(ExprBinary exp) {
		Expression left = doExpression(exp.getLeft());
		Type t = getType(left);
		expType = t;
		Expression right = doExpression(exp.getRight());
		expType = null;
		if (left == exp.getLeft() && right == exp.getRight())
			return exp;
		else
			return new ExprBinary(exp, exp.getOp(), left, right, exp.getAlias());
	}
}
