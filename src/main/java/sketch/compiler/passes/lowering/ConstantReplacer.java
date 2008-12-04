package streamit.frontend.passes;

import java.util.*;

import streamit.frontend.nodes.*;
import streamit.frontend.nodes.ExprArrayRange.RangeLen;

/**
 * Takes numeric constants defined at the beginning of the program and
 * inlines their definition throughout the program. Also removes
 * the constant definition.
 * Should run AFTER FunctionParamExtension, but before all other passes.
 *
 * @author liviu
 */
public class ConstantReplacer extends FEReplacer {

	private HashMap<String,Integer> constants;

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
			constants.put(name,((ExprConstInt)init).getVal());
			return true;
		}
		return false;
	}

	@Override
	public Object visitExprArrayRange(ExprArrayRange exp)
	{
		List newMembers=new ArrayList(exp.getMembers().size()+1);
		boolean change=false;
		for(Iterator members=exp.getMembers().iterator();members.hasNext();)
		{
			Object m=members.next();
			if(m instanceof RangeLen) {
				RangeLen range=(RangeLen) m;
				if(range.hasLenExpression()) {
					Expression l=(Expression) range.getLenExpression().accept(this);
					assert l instanceof ExprConstInt : "Range Length expressions can not be Variables. They must be constants";
					range=new RangeLen(range.start(),((ExprConstInt)l).getVal());
					change=true;
				}
				newMembers.add(range);
			}
			else
				newMembers.add(m);
		}
		if(change) exp=new ExprArrayRange(exp.getBase(),newMembers);
		return super.visitExprArrayRange(exp);
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
				types.add(type);
				names.add(name);
				inits.add(init);
			}
		}
		if(types.isEmpty()) return null;
		return new FieldDecl(field,types,names,inits);
	}


	public Object visitExprVar(ExprVar exp) {
		// TODO we should not be rewritign l-values right?  Add the code below?
		// if (exp.isLValue()) return exp;
		Integer val=constants.get(exp.getName());
		if(val==null) return exp;
		return new ExprConstInt(exp,val);
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
			func=new Function(func,func.getCls(),func.getName(),func.getReturnType(),params,func.getSpecification(),func.getBody());
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

}
