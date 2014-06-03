package sketch.compiler.dataflow.preprocessor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.NameResolver;
import sketch.compiler.ast.core.Package;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.exprs.*;
import sketch.compiler.ast.core.exprs.ExprArrayRange.RangeLen;
import sketch.compiler.ast.core.stmts.*;
import sketch.compiler.ast.core.typs.StructDef;
import sketch.compiler.ast.core.typs.StructDef.StructFieldEnt;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.ast.core.typs.TypeStructRef;
import sketch.compiler.passes.lowering.SymbolTableVisitor;
import sketch.compiler.stencilSK.VarReplacer;
import sketch.util.exceptions.ExceptionAtNode;
import sketch.util.fcns.ZipIdxEnt;

import static sketch.util.DebugOut.printNote;

import static sketch.util.fcns.ZipWithIndex.zipwithindex;
/**
 * This visitor distinguishes between int stars and bit stars, and labels each star with its
 * appropriate type.
 *
 *
 * @author asolar
 *
 */
public class TypeInferenceForStars extends SymbolTableVisitor {



    class UpgradeStarToInt extends FEReplacer {
        private final SymbolTableVisitor stv;
        Type type;

        UpgradeStarToInt(SymbolTableVisitor stv, Type type, NameResolver nres) {
            this.stv = stv;
            this.type = type;
            this.nres = nres;
        }

        public Object visitExprField(ExprField exp) {
            if (exp.isHole()) {
                exp.setTypeOfHole(type);
            }
            return super.visitExprField(exp);
        }
        public Object visitExprStar(ExprStar star) {
            if (!star.typeWasSetByScala) {
                // NOTE -- don't kill better types by Scala compiler / Skalch grgen output
                star.setType(type);
            } else {
                printNote("skipping setting star type", star, type);
            }
            return star;
        }

        public Object visitExprConstFloat(ExprConstFloat ecf) {
            ecf.setType(type);

            return ecf;
        }

        public Object visitExprTernary(ExprTernary exp) {
            Type oldType = type;
            type = TypePrimitive.bittype;
            Expression a = doExpression(exp.getA());
            type = oldType;
            Expression b = doExpression(exp.getB());
            Expression c = doExpression(exp.getC());
            if (a == exp.getA() && b == exp.getB() && c == exp.getC())
                return exp;
            else
                return new ExprTernary(exp, exp.getOp(), a, b, c);
        }

        public Object visitTypeArray(TypeArray t) {
            Type nbase = (Type) t.getBase().accept(this);
            Expression nlen = null;
            if (t.getLength() != null) {
                Type oldType = type;
                type = TypePrimitive.inttype;
                nlen = (Expression) t.getLength().accept(this);
                type = oldType;
            }
            if (nbase == t.getBase() && t.getLength() == nlen)
                return t;
            return new TypeArray(nbase, nlen, t.getMaxlength());
        }

        public Object visitExprBinary(ExprBinary exp) {
            switch (exp.getOp()) {
                case ExprBinary.BINOP_GE:
                case ExprBinary.BINOP_GT:
                case ExprBinary.BINOP_LE:
                case ExprBinary.BINOP_LT: {
                    Type oldType = type;
                    Type tleft = getType(exp.getLeft());
                    Type tright = getType(exp.getRight());
                    Type tt = tright.leastCommonPromotion(tleft, nres);
                    type = tt; // TypePrimitive.inttype;
                    Expression left = doExpression(exp.getLeft());
                    Expression right = doExpression(exp.getRight());
                    type = oldType;
                    if (left == exp.getLeft() && right == exp.getRight())
                        return exp;
                    else
                        return new ExprBinary(exp, exp.getOp(), left, right,
                                exp.getAlias());
                }
                case ExprBinary.BINOP_LSHIFT:
                case ExprBinary.BINOP_RSHIFT: {
                    Expression left = doExpression(exp.getLeft());
                    Type oldType = type;
                    type = TypePrimitive.inttype;
                    Expression right = doExpression(exp.getRight());
                    type = oldType;
                    if (left == exp.getLeft() && right == exp.getRight())
                        return exp;
                    else
                        return new ExprBinary(exp, exp.getOp(), left, right,
                                exp.getAlias());
                }
                case ExprBinary.BINOP_NEQ:
                case ExprBinary.BINOP_EQ: {
                    Type tleft = stv.getType(exp.getLeft());
                    Type tright = stv.getType(exp.getRight());
                    Type tboth = tleft.leastCommonPromotion(tright, nres);
                    Type oldType = type;
                    type = tboth;
                    Expression left = doExpression(exp.getLeft());
                    Expression right = doExpression(exp.getRight());
                    type = oldType;
                    if (left == exp.getLeft() && right == exp.getRight())
                        return exp;
                    else
                        return new ExprBinary(exp, exp.getOp(), left, right,
                                exp.getAlias());
                }

                default:
                    return super.visitExprBinary(exp);
            }
        }

        public Object visitStmtLoop(StmtLoop stmt) {
            Type oldType = type;
            type = TypePrimitive.inttype;
            Expression newIter = doExpression(stmt.getIter());
            type = oldType;
            Statement newBody = (Statement) stmt.getBody().accept(this);
            if (newIter == stmt.getIter() && newBody == stmt.getBody())
                return stmt;
            return new StmtLoop(stmt, newIter, newBody);
        }

        public Object visitExprNew(ExprNew expNew) {

            if (expNew.isHole())
                return expNew;
            Type nt = (Type) expNew.getTypeToConstruct().accept(this);
            StructDef ts = null;
            {
                assert nt instanceof TypeStructRef;
                ts =
                        TypeInferenceForStars.this.nres.getStruct(((TypeStructRef) nt).getName());
            }

            boolean changed = false;
            List<ExprNamedParam> enl =
                    new ArrayList<ExprNamedParam>(expNew.getParams().size());
            for (ExprNamedParam en : expNew.getParams()) {
                Expression old = en.getExpr();
                Type oldType = type;
                type = ts.getFieldTypMap().get(en.getName());
                StructDef cur = ts;
                while (type == null) {
                    cur = nres.getStruct(cur.getParentName());
                    type = cur.getFieldTypMap().get(en.getName());
                }
                Expression rhs = doExpression(old);
                if (rhs != old) {
                    enl.add(new ExprNamedParam(en, en.getName(), rhs));
                    changed = true;
                } else {
                    enl.add(en);
                }
                type = oldType;
            }

            if (nt != expNew.getTypeToConstruct() || changed) {
                if (!changed) {
                    enl = expNew.getParams();
                }
                return new ExprNew(expNew, nt, enl, false);
            } else {
                return expNew;
            }
        }

        public Object visitExprArrayInit(ExprArrayInit eai) {
            Type oldType = type;
            assert type instanceof TypeArray;
            type = ((TypeArray) type).getBase();
            List<Expression> le = new ArrayList<Expression>();
            boolean change = false;
            for (Expression e : eai.getElements()) {
                Expression newElem = doExpression(e);
                le.add(newElem);
                if (newElem != e) {
                    change = true;
                }
            }
            type = oldType;
            if (change) {
                return new ExprArrayInit(eai, le);
            }
            return eai;
        }

        public Object visitExprArrayRange(ExprArrayRange exp) {
            boolean change = false;
            Type oType = type;
            RangeLen range = exp.getSelection();
            Expression l = range.getLenExpression();
            if (l == null) {
                l = ExprConstInt.one;
                type = new TypeArray(type, new ExprBinary(range.start(), "+", l));
            }
            Expression newBase = doExpression(exp.getBase());
            type = oType;
            if (newBase != exp.getBase())
                change = true;

            Expression newStart = null;
            {
                Type oldType = type;
                type = TypePrimitive.inttype;
                newStart = doExpression(range.start());
                type = oldType;
            }
            if (newStart != range.start())
                change = true;

            Expression newLen = null;
            if (range.hasLen()) {
                Type oldType = type;
                type = TypePrimitive.inttype;
                newLen = doExpression(range.getLenExpression());
                type = oldType;
            }
            if (range.getLenExpression() != newLen)
                change = true;

            if (!change)
                return exp;
            return new ExprArrayRange(exp, newBase, new RangeLen(newStart, newLen));
        }
    }

	public TypeInferenceForStars(){
		super(null);
	}
	
	@Override
    public Object visitPackage(Package spec) {
	    return super.visitPackage(spec);
	}

	public Object visitStmtAtomicBlock(StmtAtomicBlock stmt){
		if(stmt.isCond()){
			Expression ie = stmt.getCond();
            ie.accept(new UpgradeStarToInt(this, TypePrimitive.bittype, nres));
		}
		return super.visitStmtAtomicBlock(stmt);
	}
	
    public Object visitStmtIfThen(StmtIfThen stmt){
    	Expression ie = stmt.getCond();
        ie.accept(new UpgradeStarToInt(this, TypePrimitive.bittype, nres));
    	return super.visitStmtIfThen(stmt);
    }

    public Object visitStmtWhile(StmtWhile stmt){
      Expression ie = stmt.getCond();
        ie.accept(new UpgradeStarToInt(this, TypePrimitive.bittype, nres));
      return super.visitStmtWhile(stmt);
    }

    public Object visitStmtDoWhile(StmtDoWhile stmt){
      Expression ie = stmt.getCond();
        ie.accept(new UpgradeStarToInt(this, TypePrimitive.bittype, nres));
      return super.visitStmtDoWhile(stmt);
    }

    @Override
    public Object visitStmtAssert(StmtAssert a){
        a.getCond().accept(new UpgradeStarToInt(this, TypePrimitive.bittype, nres));
    	return a;
    }


    public Object visitStmtFor(StmtFor stmt)
    {

        Statement newInit = null;
        if (stmt.getInit() != null) {
            newInit = (Statement) stmt.getInit().accept(this);
        }
        if (stmt.getCond() != null) {
            stmt.getCond().accept(new UpgradeStarToInt(this, TypePrimitive.bittype, nres));
        }
        Expression newCond = stmt.getCond();
        Statement newIncr = null;
        if (stmt.getIncr() != null) {
            newIncr = (Statement) stmt.getIncr().accept(this);
        }
        Statement tmp = stmt.getBody();
        Statement newBody = StmtEmpty.EMPTY;
        if (tmp != null) {
            newBody = (Statement) tmp.accept(this);
        }

        if (newInit == stmt.getInit() && newCond == stmt.getCond() &&
                newIncr == stmt.getIncr() && newBody == stmt.getBody())
            return stmt;
        return new StmtFor(stmt, newInit, newCond, newIncr, newBody, stmt.isCanonical());
    }

    public Object visitStmtLoop(StmtLoop stmt){
    	Expression ie = stmt.getIter();
        ie.accept(new UpgradeStarToInt(this, TypePrimitive.inttype, nres));
    	return super.visitStmtLoop(stmt);
    }

    private Type matchTypes(Statement stmt,String lhsn, Type lt, Type rt){
//    	if((lt != null && rt != null && !rt.promotesTo(lt)))
//    	{
//        	if((lt != null && rt != null && !rt.promotesTo(lt)))
//        		System.out.println("CRAP");
//    	}
    	stmt.assertTrue (
    			lt !=null && rt != null,
    			"internal error: " + lt + "   " + rt);
    	stmt.assertTrue (
rt.promotesTo(lt, nres),
    			"Type mismatch " + lt +" !>= " + rt);
        return lt;
    }
    public void upgradeStarToInt(Expression exp, Type ftype){
        exp.accept(new UpgradeStarToInt(this, ftype, nres));
    }
	public Object visitStmtAssign(StmtAssign stmt)
    {
	   Type lt = getType(stmt.getLHS());
       Type rt = getType(stmt.getRHS());
       String lhsn = null;
       Expression lhsExp = stmt.getLHS();
       while(lhsExp instanceof ExprArrayRange){
          	lhsExp = ((ExprArrayRange)lhsExp).getBase();
       }
       if(lhsExp instanceof ExprVar){
       	lhsn = ( (ExprVar) lhsExp).getName();
       }
       Type ftype = matchTypes(stmt, lhsn, lt, rt);
       upgradeStarToInt(stmt.getRHS(), ftype);
       upgradeStarToInt(stmt.getLHS(), ftype);
        // recurse:
        Statement result = (Statement)super.visitStmtAssign(stmt);
        return result;
    }
	public Object visitStmtVarDecl(StmtVarDecl stmt)
    {
    	Object result = super.visitStmtVarDecl(stmt);
        for (int i = 0; i < stmt.getNumVars(); i++){
        	Expression ie = stmt.getInit(i);
        	if(ie != null){
        		Type rt = getType(ie);
                Type ftype = matchTypes(stmt, stmt.getName(i), (stmt.getType(i)), rt);
        		upgradeStarToInt(ie, ftype);
        	}
        }
        return result;
    }
	
	@Override
	public Object visitExprFunCall(ExprFunCall exp) {
	    exp = (ExprFunCall) super.visitExprFunCall(exp);
        Function callee = nres.getFun(exp.getName());
        Map<String, Expression> repl = new HashMap<String, Expression>();
        VarReplacer vr = new VarReplacer(repl);
	    for (ZipIdxEnt<Expression> arg : zipwithindex(exp.getParams())) {
            Expression actual = arg.entry;
            Parameter p = callee.getParams().get(arg.idx);
            Type t = p.getType();
            repl.put(p.getName(), actual);
            upgradeStarToInt(actual, (Type) t.accept(vr));
	    }
	    return exp;
	}

    private void addFieldsToMap(Map<String, Type> fm, StructDef sdef) {
        for (StructFieldEnt fld : sdef.getFieldEntries()) {
            if (fm.containsKey(fld.getName())) {
                Type t1 = fm.get(fld.getName());
                Type t2 = fld.getType();
                if (t1.equals(t2)) {
                    continue;
                }
                fm.put(fld.getName(), null);
            } else {
                fm.put(fld.getName(), fld.getType());
            }
        }
    }

    Map<String, Map<String, Type>> ftypeMaps = new HashMap<String, Map<String, Type>>();

    public Object visitExprNew(ExprNew expNew) {
        // But make sure that new ?? doesn't contain any ??s in its parameters.

        if (expNew.isHole()) {
            TypeStructRef t = (TypeStructRef) expNew.getTypeToConstruct();
            Map<String, Type> mst;
            if (ftypeMaps.containsKey(t.getName())) {
                mst = ftypeMaps.get(t.getName());
            } else {
                mst = new HashMap<String, Type>();
                ftypeMaps.put(t.getName(), mst);
                addFieldsToMap(mst, nres.getStruct(t.getName()));
                List<String> sc = nres.getStructChildren(t.getName());
                for (String cs : sc) {
                    StructDef sdef = nres.getStruct(cs);
                    addFieldsToMap(mst, sdef);
                }
            }
            Map<String, Expression> repl = new HashMap<String, Expression>();
            VarReplacer vr = new VarReplacer(repl);
            for (ExprNamedParam en : expNew.getParams()) {
                Expression actual = en.getExpr();
                repl.put(en.getName(), actual);
            }
            for (ExprNamedParam en : expNew.getParams()) {
                String name = en.getName();
                if (!mst.containsKey(name)) {
                    throw new ExceptionAtNode("Unknown field " + name, expNew);
                }
                Type ftype = mst.get(en.getName());
                Expression actual = en.getExpr();
                if (ftype == null) {
                    if (actual instanceof ExprStar) {
                        throw new ExceptionAtNode("The type of field " + name +
                                " is ambiguous. Can't resolve the type of the hole.",
                                expNew);
                    }
                    continue;
                }
                upgradeStarToInt(actual, (Type) ftype.accept(vr));
            }

            return expNew;
        }
        TypeStructRef nt = (TypeStructRef) expNew.getTypeToConstruct().accept(this);
        StructDef sd = nres.getStruct(nt.getName());
        Map<String, Expression> repl = new HashMap<String, Expression>();
        VarReplacer vr = new VarReplacer(repl);
        for (ExprNamedParam en : expNew.getParams()) {
            Expression actual = en.getExpr();
            repl.put(en.getName(), actual);
        }
        for (ExprNamedParam en : expNew.getParams()) {
            // ADT
            StructDef current = sd;
            Type t = null;
            while (current != null) {
                t = current.getType(en.getName());
                if (t != null)
                    break;
                String parent;
                if ((parent = nres.getStructParentName(current.getFullName())) != null) {
                    current = nres.getStruct(parent);
                } else {
                    current = null;
                }
            }
            Expression actual = en.getExpr();
            upgradeStarToInt(actual, (Type) t.accept(vr));

        }
        return expNew;
    }

	@Override
	public Object visitTypeArray(TypeArray ta){
	    Expression ie = ta.getLength();
        ie.accept(new UpgradeStarToInt(this, TypePrimitive.inttype, nres));
	    return super.visitTypeArray(ta);
	}
}

