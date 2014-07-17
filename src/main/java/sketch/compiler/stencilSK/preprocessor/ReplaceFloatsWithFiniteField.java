package sketch.compiler.stencilSK.preprocessor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.NameResolver;
import sketch.compiler.ast.core.Package;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.ExprArrayInit;
import sketch.compiler.ast.core.exprs.ExprArrayRange;
import sketch.compiler.ast.core.exprs.ExprBinary;
import sketch.compiler.ast.core.exprs.ExprConstFloat;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.ExprTypeCast;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssume;
import sketch.compiler.ast.core.stmts.StmtBlock;
import sketch.compiler.ast.core.stmts.StmtExpr;
import sketch.compiler.ast.core.stmts.StmtFor;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.main.cmdline.SketchOptions;
import sketch.util.exceptions.ExceptionAtNode;

public class ReplaceFloatsWithFiniteField extends ReplaceFloatsWithBits {

    public final ExprConstInt BASE = ExprConstInt.createConstant(7);

    public final ExprArrayInit DIVTABLE;

    public ReplaceFloatsWithFiniteField(TempVarGen varGen) {
        super(varGen);
        List<Expression> le = new ArrayList<Expression>();
        le.add(ExprConstInt.createConstant(0));
        le.add(ExprConstInt.createConstant(1));
        le.add(ExprConstInt.createConstant(4));
        le.add(ExprConstInt.createConstant(5));
        le.add(ExprConstInt.createConstant(2));
        le.add(ExprConstInt.createConstant(3));
        le.add(ExprConstInt.createConstant(6));
        DIVTABLE = new ExprArrayInit((FENode) null, le);
    }

    Set<String> toCheck = new HashSet<String>();

    public Object visitProgram(Program p) {
        nres = new NameResolver(p);
        for (Package pk : p.getPackages()) {
            nres.setPackage(pk);
            for (Function f : pk.getFuncs()) {
                if (f.getSpecification() != null) {
                    String name = f.getSpecification();
                    Function spec = nres.getFun(name);
                    toCheck.add(spec.getFullName());
                }
            }
        }
        return super.visitProgram(p);
    }

    TempVarGen vgen = new TempVarGen("_fff");

    Statement assumeSizeOLD(Expression toConstrain, TypeArray ta) {
        Type base = ta.getBase();
        String iter = vgen.nextVar();
        Statement body;
        if (base instanceof TypeArray) {
            body =
                    assumeSize(new ExprArrayRange(toConstrain, new ExprVar(toConstrain,
                            iter)), (TypeArray) base);
        } else {
            body =
                    new StmtAssume(toConstrain, new ExprBinary(new ExprArrayRange(
                            toConstrain, new ExprVar(toConstrain, iter)), "<", BASE),
                            "FLOATS < " + BASE);
        }
        Statement loop = new StmtFor(iter, ta.getLength(), body);
        return loop;
    }

    Statement assumeSize(Expression toConstrain, TypeArray ta) {
        int size = SketchOptions.getSingleton().bndOpts.arr1dSize;
        Type base = ta.getBase();
        String iter = vgen.nextVar();
        Statement body;
        assert base instanceof TypePrimitive : "NYI";
       {
            Expression ii = new ExprVar(toConstrain, iter);
            body =
                    new StmtAssume(toConstrain, 
                            new ExprBinary(new ExprBinary(ii, ">=", ta.getLength()), "||", new ExprBinary(new ExprArrayRange(
                                    toConstrain, ii), "<", BASE) )
                    ,
                            "FLOATS < " + BASE);
        }
        Statement loop = new StmtFor(iter, new ExprConstInt(size), body);
        return loop;
    }


    @Override
    public Object visitFunction(Function f) {
        if (toCheck.contains(f.getFullName())) {
            List<Statement> assumes = new ArrayList<Statement>();
            for (Parameter param : f.getParams()) {
                if (param.getType().equals(TypePrimitive.doubletype) ||
                        param.getType().equals(TypePrimitive.floattype))
                {
                    assumes.add(new StmtAssume(param, new ExprBinary(new ExprVar(param,
                            param.getName()), "<", BASE), "FLOATS < " + BASE));
                    continue;
                }
                if (param.getType() instanceof TypeArray) {
                    TypeArray ta = (TypeArray) param.getType();
                    assumes.add(assumeSize(new ExprVar(param, param.getName()), ta));
                }
            }
            if (assumes.size() == 0) {
                return super.visitFunction(f);
            }
            Function nf = (Function) super.visitFunction(f);
            assumes.add(nf.getBody());
            return nf.creator().body(new StmtBlock(assumes)).create();
        } else {
            return super.visitFunction(f);
        }
    }

    // private Expression getCondition(List<Statement> stmts, Parameter p) {
    // Type t = p.getType();
    // if (t.isArray()) {
    // Type bt = ((TypeArray) t).getBase();
    // assert !bt.isArray() : "ReplaceFloat must run after EliminateMultiDim!";
    // }
    // return null;
    // }
    // @Override
    // public Object visitFunction(Function func) {
    // Function f = (Function) super.visitFunction(func);
    // Expression cond = null;
    // List<Statement> stmts = new Vector<Statement>();
    // for (Parameter p : f.getParams()) {
    // Expression e = getCondition(stmts, p);
    // if (e != null) {
    // if (cond == null) {
    // cond = e;
    // } else {
    // cond = new ExprBinary(cond, "&&", e);
    // }
    // }
    // }
    //
    // if (cond == null) {
    // return f;
    // } else {
    // stmts.add(new StmtIfThen(f, cond, f.getBody(), null));
    // return f.creator().body(new StmtBlock(stmts)).create();
    // }
    // }

    public Object visitExprBinary(ExprBinary exp) {
        Type ltype = getType(exp.getLeft());
        Type rtype = getType(exp.getRight());

        if (!isFloat(ltype) && !isFloat(rtype)) {
            return super.visitExprBinary(exp);
        }
        Expression left = doExpression(exp.getLeft());
        Expression right = doExpression(exp.getRight());
        int newOp = exp.getOp();

        switch (exp.getOp()) {
            case ExprBinary.BINOP_ADD:
            case ExprBinary.BINOP_MUL: {
                Expression ebase =
                        new ExprBinary(exp, newOp, left, right, exp.getAlias());
                return new ExprBinary(ebase, "%", BASE);
            }
            case ExprBinary.BINOP_SUB: {
                Expression ebase =
                        new ExprBinary(exp, ExprBinary.BINOP_ADD, left, BASE,
                                exp.getAlias());
                ebase =
                        new ExprBinary(exp, ExprBinary.BINOP_SUB, ebase, right,
                                exp.getAlias());
                return new ExprBinary(ebase, "%", BASE);
            }
            case ExprBinary.BINOP_DIV: {
                Expression goodright = new ExprBinary(right, "%", BASE);
                Expression ebase =
                        new ExprArrayRange(goodright, this.DIVTABLE, goodright);

                ebase =
                        new ExprBinary(exp, ExprBinary.BINOP_MUL, left, ebase,
                                exp.getAlias());
                ebase = new ExprBinary(ebase, "%", BASE);
                /*
                 * ebase = new ExprTernary(exp, ExprTernary.TEROP_COND, new ExprBinary(
                 * right, "!=", ExprConstInt.zero), ebase, ExprConstInt.zero);
                 */
                return ebase;
            }
            case ExprBinary.BINOP_EQ:
            case ExprBinary.BINOP_NEQ:
                // TODO xzl: should we do this?
                // Expression goodleft = new ExprBinary(left, "%", BASE);
                // Expression goodright = new ExprBinary(right, "%", BASE);
                return new ExprBinary(exp, newOp, left, right, exp.getAlias());
            default:
                assert false : "You can't apply this floating point operation if you are doing floating-point to boolean replacement." +
                        exp + " " + exp.getOp();
        }
        return null;
    }


    public Object visitExprTypeCast(ExprTypeCast exp) {
        Expression expr = doExpression(exp.getExpr());
        Type told = getType(exp.getExpr());
        Type tnew = exp.getType();
        if (told.equals(TypePrimitive.inttype) && isFloat(tnew)) {
            throw new ExceptionAtNode(
                    "You can't cast from ints to doubles/floats if you are using --fe-fpencoding AS_FFIELD." +
                            exp, exp);
        }
        if (tnew.equals(TypePrimitive.inttype) && isFloat(told)) {
            throw new ExceptionAtNode(
                    "You can't cast from doubles/floats to int if you are using --fe-fpencoding AS_FFIELD." +
                            exp, exp);
        }
        if (expr == exp.getExpr() && tnew == exp.getType())
            return exp;
        else
            return new ExprTypeCast(exp, tnew, expr);
    }

    public Object visitExprConstFloat(ExprConstFloat fexp) {
        String name = null;
        double flc = fexp.getVal();
        if (flc <= epsilon && flc >= -epsilon) {
            return ExprConstInt.zero;
        }

        for (int i = 1; i < BASE.getVal(); ++i) {
            flc = fexp.getVal() - (1.0 / ((double) i));
            if (flc <= epsilon && flc >= -epsilon) {
                return DIVTABLE.getElements().get(i);
            }
            flc = fexp.getVal() - ((double) i);
            if (flc <= epsilon && flc >= -epsilon) {
                return ExprConstInt.createConstant(i);
            }
        }

        Float fl = new Float(fexp.getVal());
        if (floatConstants.containsKey(fl)) {
            name = floatConstants.get(fl).getName();
        } else {
            name = fName(fl);
            floatConstants.put(fl, newFloatFunction(name));
        }
        List<Expression> pl = new ArrayList<Expression>(1);
        ExprVar ev = new ExprVar(fexp, varGen.nextVar());
        pl.add(ev);
        addStatement(new StmtVarDecl(fexp, replType(), ev.getName(), null));
        addStatement(new StmtExpr(new ExprFunCall(fexp, name, pl)));
        return ev;
    }

    public Type replType() {
        return TypePrimitive.inttype;
    }
}
