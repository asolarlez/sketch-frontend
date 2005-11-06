package streamit.frontend.passes;

import java.util.*;

import streamit.frontend.nodes.*;

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
	
	public ConstantReplacer() {
		constants=new HashMap<String,Integer>();
	}

	private boolean addConstant(Type type, String name, Expression init) {
		if(init==null) return false;
		init=(Expression) init.accept(this);
		if(init instanceof ExprConstInt) {
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
				types.add(type);
				names.add(name);
				inits.add(init);
			}
		}
		if(types.isEmpty()) return null;
		return new FieldDecl(field.getContext(),types,names,inits);
	}

	
	public Object visitExprVar(ExprVar exp) {
		Integer val=constants.get(exp.getName());
		if(val==null) return exp;
		return new ExprConstInt(exp.getContext(),val);
	}

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
			return new ExprConstInt(exp.getContext(),v);
		}
		return exp;
	}

	public Object visitFunction(Function func) {
		//before visiting the body, we check to see if we need to
		//make constant substitutions in array lengths
		List<Parameter> params=func.getParams();
		for(int i=0;i<params.size();i++) {
			Parameter par=params.get(i);
			if(par.getType() instanceof TypeArray) {
				TypeArray arr=(TypeArray) par.getType();
				Expression len=arr.getLength();
				Expression newlen=(Expression) len.accept(this);
				if(newlen!=len) {
					params.set(i,new Parameter(new TypeArray(arr.getBase(),newlen),par.getName()));
				}
			}
		}
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
					types.set(i,new TypeArray(arr.getBase(),newlen));
				}
			}
		}
		return super.visitStmtVarDecl(stmt);
	}

}
