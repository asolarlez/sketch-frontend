package streamit.frontend.passes;

import java.util.*;

import streamit.frontend.nodes.*;
import streamit.frontend.nodes.ExprArrayRange.*;

/**
 * Code generation pass that gets rid of the "bit" data type, using appropriate
 * C types instead. Performs big integer arithmetic on variables that are too
 * large to fit into a single primitive type.
 * 
 * @author liviu
 */
public class BitTypeRemover extends SymbolTableVisitor {

	/**
	 * Symbol table that stores the type of a variable prior to substitution.
	 */
    protected SymbolTable preSymtab;

    public BitTypeRemover() {
		super(null);	
	}

    private static boolean isBitType(Type t)
    {
    	return t instanceof TypePrimitive && ((TypePrimitive)t).getType()==TypePrimitive.TYPE_BIT;
    }

    private static boolean isBitArrayType(Type t)
    {
    	return t instanceof TypeArray && isBitType(((TypeArray)t).getBase());
    }

	private Type convertType(Type type)
	{
		if(type instanceof TypeArray) {
			TypeArray array=(TypeArray)type;
			Type baseType=array.getBase();
			if(baseType instanceof TypeArray) {
				Type newBase=convertType(baseType);
				if(newBase==baseType) return type;
				return new TypeArray(newBase,array.getLength());
			}
			if(isBitType(baseType)) {
				Expression len=array.getLength();
				if(len instanceof ExprConstInt) {
					int val=((ExprConstInt)len).getVal();
					if(val<=8)
						return TypePrimitive.int8type;
					else if(val<=16)
						return TypePrimitive.int16type;
					else if(val<=32)
						return TypePrimitive.int32type;
					else if(val<=64)
						return TypePrimitive.int64type;
					//ok, looks like we'll need an array to store the large integer
					return new TypeArray(TypePrimitive.int32type,
							new ExprConstInt(len.getContext(),(val-1)/32+1));
				}
			}
		}
		else if(isBitType(type)) {
			return TypePrimitive.booltype;
		}
		//fallback case: do nothing
		return type;
	}

	@Override
	public Object visitFunction(Function func)
	{
		SymbolTable oldPST=preSymtab;
		preSymtab=new SymbolTable(preSymtab);
		boolean change=false;
		List newParams=new ArrayList(func.getParams().size()+1);
        for(Iterator iter = func.getParams().iterator(); iter.hasNext();)
        {
            Parameter param = (Parameter)iter.next();
            Type oldType=param.getType();
            preSymtab.registerVar(param.getName(),oldType);
            Type newType=convertType(oldType);
            if(oldType!=newType) {
            	change=true;
            	newParams.add(new Parameter(newType,param.getName(),param.isParameterOutput()));
            }
            else
            	newParams.add(param);
        }
        if(change)
        	func=new Function(func.getContext(),func.getCls(),func.getName(),func.getReturnType(),newParams,func.getSpecification(),func.getBody());
		Object ret=super.visitFunction(func);
		preSymtab=oldPST;
		return ret;
	}

	@Override
	public Object visitStmtVarDecl(StmtVarDecl stmt)
	{
		boolean change=false; 
		int n=stmt.getNumVars();
		List types=new ArrayList(n+1);
		for(int i=0;i<n;i++) {
			Type oldType=stmt.getType(i);
			preSymtab.registerVar(stmt.getName(i),oldType);
			Type newType=convertType(oldType);
			if(newType!=oldType) change=true;
			types.add(newType);
		}
		if(change)
			stmt=new StmtVarDecl(stmt.getContext(),types,stmt.getNames(),stmt.getInits());
		return super.visitStmtVarDecl(stmt);
	}	

	private Expression convertArrayRange(ExprArrayRange oldExpr, Expression start, int len)
	{
		Expression base=oldExpr.getBase();
		assert base instanceof ExprVar;
		TypeArray pseudotype=(TypeArray) preSymtab.lookupVar((ExprVar)base);
		if(!isBitArrayType(pseudotype)) return oldExpr;

		Type type=symtab.lookupVar((ExprVar)base);
		if(type instanceof TypeArray) {
			throw new UnsupportedOperationException("Cannot deal with big integers at this time.");
		}
		assert type instanceof TypePrimitive;
		if(len>1) {
			throw new UnsupportedOperationException("Cannot deal with array ranges at this time.");
		}
		else {
			Expression sel=new ExprBinary(oldExpr.getContext(),ExprBinary.BINOP_BAND,
					new ExprBinary(oldExpr.getContext(),ExprBinary.BINOP_RSHIFT,
						oldExpr.getBase(),
						start
					),
					new ExprConstInt(oldExpr.getContext(),1)
				);
			return sel;
		}
	}
	
	public Object visitExprArrayRange(ExprArrayRange exp) {
		List ranges=exp.getMembers();
		if(ranges.size()==0) throw new IllegalStateException();
		if(ranges.size()>1) throw new UnsupportedOperationException("Multi-range indexing not currently supported.");
		Object o=ranges.get(0);
		if(o instanceof RangeLen) 
		{
			RangeLen range=(RangeLen) o;
			return convertArrayRange(exp,range.start,range.len);
		}
		else
		{
			Range range=(Range) o;
			if(!(range.start instanceof ExprConstInt) || !(range.end instanceof ExprConstInt))
				throw new UnsupportedOperationException("Cannot deal with array range indexing if the indexes are not statically computable.");
			int start=((ExprConstInt)range.start).getVal();
			int end=((ExprConstInt)range.end).getVal();
			int len=end-start+1;
			assert len>0;
			return convertArrayRange(exp,range.start,len);
		}
		//throw new UnsupportedOperationException("Cannot translate complicated array indexing.");
	}
	
}
