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
import sketch.compiler.ast.core.exprs.ExprBinary;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprTypeCast;
import sketch.compiler.ast.core.exprs.ExprUnary;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.passes.structure.ASTObjQuery;
import sketch.compiler.passes.structure.GetAssignLHS;

/**
 * Takes numeric constants defined at the beginning of the program and
 * inlines their definition throughout the program. Also removes
 * the constant definition.
 * Should run AFTER FunctionParamExtension, but before all other passes.
 *
 * @author liviu
 */
public class ConstantReplacer extends FEReplacer {

    protected HashMap<String, Integer> constants;
    protected HashSet<String> assignedVars;

	public ConstantReplacer(Map<String, Integer> subs) {
		constants=new HashMap<String,Integer>();
		if(subs != null){
			constants.putAll(subs);
		}
	}

	private boolean addConstant(Type type, String name, Expression init) {
		if(init==null) return false;
		init=(Expression) init.accept(this);
		if(init instanceof ExprConstInt) {
			if(constants.get(name)!=null) return false;
            if (assignedVars.contains(name)) {
                return false;
            }
			constants.put(name,((ExprConstInt)init).getVal());
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

    /** can be overridden by subclasses */
    public Expression replaceConstantExpr(ExprVar exp, int val) {
        return new ExprConstInt(exp, val);
    }

	public Object visitExprVar(ExprVar exp) {
		// TODO we should not be rewritign l-values right?  Add the code below?
		// if (exp.isLValue()) return exp;
		Integer val=constants.get(exp.getName());
        if (val == null) {
            return exp;
        } else {
            return replaceConstantExpr(exp, val);
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
        		newType = new TypeArray(arr.getBase(),newlen);
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
		List<Parameter> params=new ArrayList<Parameter>(func.getParams());
		boolean changed=false;
		for(int i=0;i<params.size();i++) {
			Parameter par=params.get(i);
			if(par.getType() instanceof TypeArray) {
				TypeArray arr=(TypeArray) par.getType();
				Expression len=arr.getLength();
				Expression newlen=(Expression) len.accept(this);
				if(newlen!=len) {
					params.set(i,new Parameter(new TypeArray(arr.getBase(),newlen),par.getName(), par.getPtype()));
					changed=true;
				}
			}
		}
		if(changed)
		    func = func.creator().params(params).create();
		return super.visitFunction(func);
	}

	public Object visitStmtVarDecl(StmtVarDecl stmt) {
		List<Type> types=stmt.getTypes();
		for(int i=0;i<types.size();i++) {
			Type t=types.get(i);
			if(t instanceof TypeArray) {
				TypeArray arr=(TypeArray) t;
				Expression len=arr.getLength();
				Expression newlen=(Expression) len.accept(this);
				if(newlen!=len) {
					// TODO Is it legal to update an AST?  Don't we need to generate a new types List rather than set its elements?
					types.set(i,new TypeArray(arr.getBase(),newlen));
				}
			}
		}
		return super.visitStmtVarDecl(stmt);
	}

    @Override
    public Object visitProgram(Program prog) {
        this.assignedVars = (new GetValDefs()).run(prog);
        return super.visitProgram(prog);
    }

    public static class GetValDefs extends ASTObjQuery<HashSet<String>> {
        public GetValDefs() {
            super(new HashSet<String>());
        }

        @Override
        public Object visitStmtAssign(StmtAssign stmt) {
            try {
                result.add(stmt.getLhsBase().getName());
            } catch (FEVisitorException e) {}
            return super.visitStmtAssign(stmt);
        }

        @Override
        public Object visitExprUnary(ExprUnary exp) {
            switch (exp.getOp()) {
                case ExprUnary.UNOP_POSTDEC:
                case ExprUnary.UNOP_POSTINC:
                case ExprUnary.UNOP_PREDEC:
                case ExprUnary.UNOP_PREINC:
                    result.add(exp.getExpr().accept(new GetAssignLHS()).getName());
            }
            return super.visitExprUnary(exp);
        }
    }
}
