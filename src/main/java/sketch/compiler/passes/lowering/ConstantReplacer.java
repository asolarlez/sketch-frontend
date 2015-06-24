package sketch.compiler.passes.lowering;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.FEVisitorException;
import sketch.compiler.ast.core.FieldDecl;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.NameResolver;
import sketch.compiler.ast.core.Package;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.exprs.*;
import sketch.compiler.ast.core.exprs.regens.ExprAlt;
import sketch.compiler.ast.core.exprs.regens.ExprRegen;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtBlock;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.passes.structure.GetAssignLHS;
import sketch.util.Misc;
import sketch.util.exceptions.ExceptionAtNode;

/**
 * Takes numeric constants defined at the beginning of the program and
 * inlines their definition throughout the program. Also removes
 * the constant definition.
 * Should run AFTER FunctionParamExtension, but before all other passes.
 *
 * @author liviu
 */
public class ConstantReplacer extends FEReplacer {

    static class ShadowStack {
        final HashSet<String> shadow = new HashSet<String>();
        final ShadowStack prev;

        ShadowStack(ShadowStack prev) {
            this.prev = prev;
        }

        ShadowStack push() {
            ShadowStack s = new ShadowStack(this);
            return s;
        }

        ShadowStack pop() {
            return this.prev;
        }

        void add(String s) {
            shadow.add(s);
        }

        boolean contains(String s) {
            if (shadow.contains(s)) {
                return true;
            }
            if (prev != null) {
                return prev.contains(s);
            }
            return false;
        }
    }

    ShadowStack shadows = new ShadowStack(null);

    public void pushBlock() {
        shadows = shadows.push();
    }

    public void popBlock() {
        shadows = shadows.pop();
    }

    public void addShadow(String vname) {
        shadows.add(vname);
    }

    protected HashMap<String, Expression> constants;

    public ConstantReplacer(Map<String, Expression> subs) {
        constants = new HashMap<String, Expression>();
		if(subs != null){
			constants.putAll(subs);
		}
	}

    Expression replacement = null;
	private boolean addConstant(Type type, String name, Expression init) {
        replacement = null;
		if(init==null) return false;
		init=(Expression) init.accept(this);
        if (init instanceof ExprConstInt) {
            if (!isFinal(name)) {
                return false;
            }
            constants.put(name, init);
            replacement = init;
			return true;
		}
        if (type.equals(TypePrimitive.inttype)) {
            if (init instanceof ExprStar) {
                if (!isFinal(name)) {
                    return false;
                }
                constants.put(name, new ExprStar((ExprStar) init, true));
                // If it is ExprStar, we want to keep around the global variable
                return false;
            }
            if (init instanceof ExprRegen &&
                    ((ExprRegen) init).getExpr() instanceof ExprAlt)
            {
                ExprAlt alts = (ExprAlt) ((ExprRegen) init).getExpr();
                List<Expression> clist = new ArrayList<Expression>();
                clist = allConstants(alts, clist);
                if (clist == null) {
                    return false;
                }
                if (!isFinal(name)) {
                    replacement = newChoices(init, clist);
                    return false;
                }
                replacement = newChoices(init, clist);
                constants.put(name, replacement);
                // If it is ExprStar, we want to keep around the global variable
                return false;
            }
        }
		return false;
	}

    Expression newChoices(Expression cx, List<Expression> clist) {
        ExprStar es = new ExprStar(cx, Misc.nBitsBinaryRepr(clist.size()), true);
        return toConditional(es, clist, 0);
    }

    private Expression toConditional(Expression which, List<Expression> exps, int i) {
        return new ExprArrayRange(new ExprArrayInit(which, exps), which);
        
        /*
        if ((i + 1) == exps.size())
            return exps.get(i);
        else {
            Expression cond =
                    new ExprBinary(which, "==",
                            ExprConstant.createConstant(which, "" + i));
            return new ExprTernary("?:", cond, exps.get(i), toConditional(which, exps,
                    i + 1));
        }*/
    }

    List<Expression> allConstants(ExprAlt choices, List<Expression> le) {
        Expression e1 = choices.getThis();
        if (e1 instanceof ExprConstInt) {
            le.add(e1);
        } else {
            if (e1 instanceof ExprAlt) {
                List<Expression> t = allConstants((ExprAlt) e1, le);
                if (t == null) {
                    return null;
                }
            } else {
                return null;
            }
        }

        Expression e2 = choices.getThat();
        if (e2 instanceof ExprConstInt) {
            le.add(e2);
        } else {
            if (e2 instanceof ExprAlt) {
                List<Expression> t = allConstants((ExprAlt) e2, le);
                if (t == null) {
                    return null;
                }
            } else {
                return null;
            }
        }
        return le;
    }


	@SuppressWarnings("unchecked")
    public Object visitFieldDecl(FieldDecl field) {
		field=(FieldDecl) super.visitFieldDecl(field);
		//if it's statically computable (constant), store it in
		//our hash table and remove it from the program
		List types=new ArrayList(4);
		List names=new ArrayList(4);
		List inits=new ArrayList(4);
		for(int i=0;i<field.getNumFields();i++) {
			Type type=field.getType(i);
			String name=field.getName(i);
			Expression init=field.getInit(i);
			if(!addConstant(type,name,init)) {
			    // add it to a list to put back into the FieldDecls
                if (replacement != null) {
                    init = replacement;
                }
				types.add(type);
				names.add(name);
                inits.add(init);
            }
		}
		if(types.isEmpty()) return null;
		return new FieldDecl(field,types,names,inits);
	}


	public Object visitExprVar(ExprVar exp) {

        Expression val = constants.get(exp.getName());
        if (val == null || shadows.contains(exp.getName())) {
            return exp;
        } else {
            return val;
        }
	}


	public Object visitExprUnary(ExprUnary exp){
		exp = (ExprUnary) super.visitExprUnary(exp);

		if(exp.getExpr() instanceof ExprConstInt){
			int or = ((ExprConstInt)exp.getExpr()).getVal();
			final int v;
			switch(exp.getOp()){
			case ExprUnary.UNOP_NEG : v = -or; break;
			case ExprUnary.UNOP_NOT : v = 1 - or; break;
				default: return exp;
			}
			return new ExprConstInt(exp,v);
		}
		return exp;

	}

	@Override
	public Object visitExprBinary(ExprBinary exp) {
		//first call "recursively" to handle substitutions
		exp=(ExprBinary) super.visitExprBinary(exp);
		//then do constant folding if possible
		if(exp.getLeft() instanceof ExprConstInt && exp.getRight() instanceof ExprConstInt) {
			int l=((ExprConstInt)exp.getLeft()).getVal();
			int r=((ExprConstInt)exp.getRight()).getVal();
			final int v;
			switch(exp.getOp()) {
				case ExprBinary.BINOP_ADD: v=l+r; break;
				case ExprBinary.BINOP_SUB: v=l-r; break;
				case ExprBinary.BINOP_MUL: v=l*r; break;
				case ExprBinary.BINOP_DIV: v=l/r; break;
				case ExprBinary.BINOP_MOD: v=l%r; break;
				case ExprBinary.BINOP_LSHIFT: v=l<<r; break;
				case ExprBinary.BINOP_RSHIFT: v=l>>r; break;
				default: return exp;
			}
			return new ExprConstInt(exp,v);
		}
		return exp;
	}

	 public Object visitExprTypeCast(ExprTypeCast exp)
    {

		Expression expr = doExpression(exp.getExpr());
		Type newType = exp.getType();
        if(exp.getType()instanceof TypeArray){
        	TypeArray arr=(TypeArray) exp.getType();
        	Expression len=arr.getLength();
        	Expression newlen=(Expression) len.accept(this);
        	if(newlen!=len) {
                newType = new TypeArray(arr.getBase(), newlen, arr.getMaxlength());
        	}
        }
        if (expr == exp.getExpr() && newType == exp.getType())
            return exp;
        else
            return new ExprTypeCast(exp, newType, expr);
    }

	public Object visitFunction(Function func) {
		//before visiting the body, we check to see if we need to
		//make constant substitutions in array lengths
        pushBlock();
        try {
            List<Parameter> params = new ArrayList<Parameter>(func.getParams());
            boolean changed = false;
            for (int i = 0; i < params.size(); i++) {
                Parameter par = params.get(i);
                if (par.getType() instanceof TypeArray) {
                    TypeArray arr = (TypeArray) par.getType();
                    Expression len = arr.getLength();
                    Expression newlen = (Expression) len.accept(this);
                    if (newlen != len) {
                        params.set(i, new Parameter(par, new TypeArray(arr.getBase(),
                                newlen),
                                par.getName(), par.getPtype()));
                        changed = true;
                    }
                }
            }
            if (changed)
                func = func.creator().params(params).create();
            return super.visitFunction(func);
        } finally {
            popBlock();
        }
	}

    public Object visitParameter(Parameter par) {
        addShadow(par.getName());
        Object o = super.visitParameter(par);
        return o;
    }

	public Object visitStmtVarDecl(StmtVarDecl stmt) {
        for (int i = 0; i < stmt.getNumVars(); ++i) {
            addShadow(stmt.getName(i));
        }
        return super.visitStmtVarDecl(stmt);
	}

    public Object visitStmtBlock(StmtBlock sb) {
        pushBlock();
        try {
            Object o = super.visitStmtBlock(sb);
            return o;
        } finally {
            popBlock();
        }
    }

    GetValDefs constInfo;

    @Override
    public Object visitProgram(Program prog) {
        this.constInfo = new GetValDefs();
        constInfo.visitProgram(prog);
        nres = constInfo.getNres();
        for (Package pk : prog.getPackages()) {
            currPkg = pk.getName();
            nres.setPackage(pk);
            for (FieldDecl fd : pk.getVars()) {
                fd.accept(this);
            }
        }
        return super.visitProgram(prog);
    }

    String currPkg = null;

    public Object visitPackage(Package pkg) {
        currPkg = pkg.getName();
        for (Entry<String, String> var : constInfo.namesToPkg.entrySet()) {
            if (var.getValue() == constInfo.multiPkg) {
                constants.remove(var.getKey());
            }
        }
        return super.visitPackage(pkg);
    }

    public boolean isFinal(String name) {
        if (constInfo.result.contains(name + '@' + currPkg)) {
            return true;
        }
        return false;
    }

    /**
     * Find all the global variables that remain constant through the computation.
     */
    public static class GetValDefs extends FEReplacer {
        ShadowStack shadows = new ShadowStack(null);
        public HashSet<String> result;
        public void pushBlock() {
            shadows = shadows.push();
        }

        public void popBlock() {
            shadows = shadows.pop();
        }

        public void addShadow(String vname) {
            shadows.add(vname);
        }


        public GetValDefs() {
            result = new HashSet<String>();
        }

        public Object visitFieldDecl(FieldDecl fd) {
            for (int i = 0; i < fd.getNumFields(); ++i) {
                String name = fd.getName(i);
                result.add(addPkgName(name));
                if (namesToPkg.containsKey(name)) {
                    namesToPkg.put(name, multiPkg);
                } else {
                    namesToPkg.put(name, currPkg);
                }
            }
            return super.visitFieldDecl(fd);
        }


        @Override
        public Object visitStmtAssign(StmtAssign stmt) {
            try {
                String nm = stmt.getLhsBase().getName();
                checkAndRemove(nm);
            } catch (FEVisitorException e) {}
            return super.visitStmtAssign(stmt);
        }

        public Object visitStmtBlock(StmtBlock sb) {
            pushBlock();
            try {
                Object o = super.visitStmtBlock(sb);
                return o;
            } finally {
                popBlock();
            }
        }

        public Object visitFunction(Function f) {
            pushBlock();
            try {
                return super.visitFunction(f);
            } finally {
                popBlock();
            }
        }

        public Object visitStmtVarDecl(StmtVarDecl svd) {
            for (int i = 0; i < svd.getNumVars(); ++i) {
                addShadow(svd.getName(i));
            }
            return super.visitStmtVarDecl(svd);
        }

        public Object visitParameter(Parameter par) {
            addShadow(par.getName());
            Object o = super.visitParameter(par);
            return o;
        }

        public Object visitExprFunCall(ExprFunCall efc) {
            Function f = this.getFuncNamed(efc.getName());
            List<Parameter> pit = f != null ? f.getParams() : null;
            Parameter last = null;
            int ipcnt = 0;
            if (pit != null && pit.size() != efc.getParams().size()) {

                while (ipcnt < pit.size()) {
                    if (!pit.get(ipcnt).isImplicit()) {
                        break;
                    }
                    ++ipcnt;
                }
            }
            if (pit != null && (pit.size() - ipcnt) != efc.getParams().size()) {
                throw new ExceptionAtNode("Wrong number of parameters: " + efc, efc);
            }

            {
                int i = ipcnt;
                for (Expression e : efc.getParams()) {
                    Parameter p = pit != null ? pit.get(i) : null;
                    ++i;
                    if (f == null || p.isParameterOutput()) {
                        if (e instanceof ExprVar || e instanceof ExprArrayRange) {
                            String nm = e.accept(new GetAssignLHS()).getName();
                            checkAndRemove(nm);
                        }
                    }
                }
            }

            return super.visitExprFunCall(efc);
        }

        void checkAndRemove(String nm) {
            if (!shadows.contains(nm)) {
                String pkg = namesToPkg.get(nm);
                if (pkg == multiPkg) {
                    result.remove(nm + '@' + currPkg);
                } else {
                    result.remove(nm + '@' + pkg);
                }
            }
        }

        String currPkg;
        public final String multiPkg = "$$MULTI";
        public Map<String, String> namesToPkg = new HashMap<String, String>();

        String addPkgName(String name) {
            return name + "@" + currPkg;
        }

        public Object visitProgram(Program p) {
            nres = new NameResolver(p);
            for (Package pk : p.getPackages()) {
                currPkg = pk.getName();
                nres.setPackage(pk);
                for (FieldDecl fd : pk.getVars()) {
                    fd.accept(this);
                }
            }

            for (Package pk : p.getPackages()) {
                nres.setPackage(pk);
                currPkg = pk.getName();
                for (Function f : pk.getFuncs()) {
                    f.accept(this);
                }
            }
            return p;
        }

        @Override
        public Object visitExprUnary(ExprUnary exp) {
            switch (exp.getOp()) {
                case ExprUnary.UNOP_POSTDEC:
                case ExprUnary.UNOP_POSTINC:
                case ExprUnary.UNOP_PREDEC:
                case ExprUnary.UNOP_PREINC:
                    String nm = exp.getExpr().accept(new GetAssignLHS()).getName();
                    checkAndRemove(nm);
            }
            return super.visitExprUnary(exp);
        }
    }
}
