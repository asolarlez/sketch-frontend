package sketch.compiler.passes.lowering;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.FEVisitorException;
import sketch.compiler.ast.core.FieldDecl;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.exprs.ExprArrayRange;
import sketch.compiler.ast.core.exprs.ExprBinary;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.ExprStar;
import sketch.compiler.ast.core.exprs.ExprTypeCast;
import sketch.compiler.ast.core.exprs.ExprUnary;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtBlock;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.passes.structure.ASTObjQuery;
import sketch.compiler.passes.structure.GetAssignLHS;
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
    protected HashSet<String> constVars;

    public ConstantReplacer(Map<String, Expression> subs) {
        constants = new HashMap<String, Expression>();
		if(subs != null){
			constants.putAll(subs);
		}
	}

	private boolean addConstant(Type type, String name, Expression init) {
		if(init==null) return false;
		init=(Expression) init.accept(this);
		if(init instanceof ExprConstInt) {
			if(constants.get(name)!=null) return false;
            if (!constVars.contains(name)) {
                return false;
            }
            constants.put(name, init);
			return true;
		}
        if (type.equals(TypePrimitive.inttype) && init instanceof ExprStar) {
            if (constants.get(name) != null)
                return false;
            if (!constVars.contains(name)) {
                return false;
            }
            constants.put(name, new ExprStar((ExprStar) init, true));
            return true;
        }
		return false;
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

    @Override
    public Object visitProgram(Program prog) {
        this.constVars = (new GetValDefs()).run(prog);
        return super.visitProgram(prog);
    }

    /**
     * Find all the global variables that remain constant through the computation.
     */
    public static class GetValDefs extends ASTObjQuery<HashSet<String>> {
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


        public GetValDefs() {
            super(new HashSet<String>());
        }

        public Object visitFieldDecl(FieldDecl fd) {
            for (int i = 0; i < fd.getNumFields(); ++i) {
                result.add(fd.getName(i));
            }
            return super.visitFieldDecl(fd);
        }


        @Override
        public Object visitStmtAssign(StmtAssign stmt) {
            try {
                String nm = stmt.getLhsBase().getName();
                if (!shadows.contains(nm)) {
                    result.remove(nm);
                }
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
                            if (!shadows.contains(nm)) {
                                result.remove(nm);
                            }
                        }
                    }
                }
            }

            return super.visitExprFunCall(efc);
        }

        @Override
        public Object visitExprUnary(ExprUnary exp) {
            switch (exp.getOp()) {
                case ExprUnary.UNOP_POSTDEC:
                case ExprUnary.UNOP_POSTINC:
                case ExprUnary.UNOP_PREDEC:
                case ExprUnary.UNOP_PREINC:
                    String nm = exp.getExpr().accept(new GetAssignLHS()).getName();
                    if (!shadows.contains(nm)) {
                        result.remove(nm);
                    }
            }
            return super.visitExprUnary(exp);
        }
    }
}
