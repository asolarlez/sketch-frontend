package streamit.frontend.codegenerators;

import java.util.*;

import streamit.frontend.nodes.*;
import streamit.frontend.nodes.ExprArrayRange.RangeLen;
import streamit.frontend.tojava.NodesToJava;

public class NodesToC extends NodesToJava {

	private String filename; 	
	private boolean isBool = true;
	
	public NodesToC(TempVarGen varGen, String filename) {
		super(false, varGen);
		this.filename=filename;		
	}
			
	
	public Object visitExprArrayInit(ExprArrayInit exp)
    {
		
		if(isBool){
			StringBuffer sb = new StringBuffer();
			sb.append("\"");
		
			List elems = exp.getElements();
			for (int i=0; i<elems.size(); i++) {
			    sb.append((String)((Expression)elems.get(i)).accept(this));
			}
	
			sb.append("\"");
	        return sb.toString();
		}
		return super.visitExprArrayInit(exp);
    }
	
	
	
	@Override
	public Object visitExprUnary(ExprUnary exp)
	{
        if(exp.getOp()==ExprUnary.UNOP_BNOT)
        	return "~"+exp.getExpr().accept(this);
		return super.visitExprUnary(exp);
	}

	@Override
	public Object visitProgram(Program prog)
	{
		String ret=(String)super.visitProgram(prog);
		StringBuffer preamble=new StringBuffer();
		preamble.append("#include <cstdio>\n");
		preamble.append("#include <assert.h>\n");
		preamble.append("#include \"");
		preamble.append(filename);
		preamble.append(".h\"\n");
		preamble.append(ret);
		return preamble.toString();
	}

	public Object visitStreamSpec(StreamSpec spec){
		String result = "";
		ss = spec;
        for (Iterator iter = spec.getFuncs().iterator(); iter.hasNext(); )
        {
            Function oldFunc = (Function)iter.next();            
            result += (String)oldFunc.accept(this);            
        }
        return result;
	}
	
	public Object visitFunction(Function func)
    {
        String result = indent ;
        if (!func.getName().equals(ss.getName()))
            result += convertType(func.getReturnType()) + " ";
        result += func.getName();
        String prefix = null;
        result += doParams(func.getParams(), prefix) + " ";
        result += (String)func.getBody().accept(this);
        result += "\n";
        return result;
    }
	

	
    public String doParams(List params, String prefix)
    {
        String result = "(";
        boolean first = true;
        for(Iterator iter = params.iterator(); iter.hasNext();)
        {
            Parameter param = (Parameter)iter.next();
            Type type = param.getType();
            
            
            if (!first) result += ", ";
            if (prefix != null) result += prefix + " ";
            result += typeForParam(type);            
            result += " ";
            result += param.getName();
            first = false;
        }
        result += ")";
        return result;
    }

    public String typeForParam(Type type){
        if(type instanceof TypeArray){
        	TypeArray otype = (TypeArray)type;
        	type = ((TypeArray)type).getBase();
        	if(type.equals(TypePrimitive.bittype)){
        		return "bitvec<" + (otype).getLength() + "> ";
        	}else{
        		return convertType(type) + "*";
        	}
        }else{
        	return convertType(type) + "&";
        }
    }
    
    
    
    public String typeForDecl(Type type, String name){
    	String postFix = "";
        if(type instanceof TypeArray){
        	Type otype = type;
        	type = ((TypeArray)type).getBase();
        	if(type.equals(TypePrimitive.bittype)){
        		return "bitvec<" + ((TypeArray)otype).getLength() + "> " + name + postFix ;
        	}else{
        		postFix = "[" + ((TypeArray)otype).getLength() +  "]";        	
        		return convertType(type) +  " " + name + postFix ;
        	}
        }
        return convertType(type) +  " " + name + postFix ;
    }
    
    
    public Object visitStmtVarDecl(StmtVarDecl stmt)
    {
        String result = "";        
        for (int i = 0; i < stmt.getNumVars(); i++)
        {            
            Type type = stmt.getType(i);
            if(type instanceof TypeArray){
	            if( ((TypeArray)type).getBase().equals( TypePrimitive.inttype )){
	            	isBool = false;
	            }
            }
            result += typeForDecl(type,   stmt.getName(i));
            if (stmt.getInit(i) != null)
                result += " = " + (String)stmt.getInit(i).accept(this);
            result += ";\n";
            isBool = true;
        }
        return result;
    }
	
	public Object visitExprFunCall(ExprFunCall exp)
    {
		String result = "";
        String name = exp.getName();        
        result = name + "(";         
        boolean first = true;
        for (Iterator iter = exp.getParams().iterator(); iter.hasNext(); )
        {
            Expression param = (Expression)iter.next();
            if (!first) result += ", ";
            first = false;
            result += (String)param.accept(this);
        }
        result += ")";
        return result;        
    }
	
	
    public Object visitStmtLoop(StmtLoop stmt)
    {
    	
    	String result ;
    	String itNum = this.varGen.nextVar();
    	String var = this.varGen.nextVar();
    	result = "int "+itNum + " = " + stmt.getIter().accept(this) + "; \n";
    	result += indent + "for (int " + var + " = 0; ";
    	result += var + " < " +  itNum + "; ++" + var;         
        result += ") ";
        result += (String)stmt.getBody().accept(this);
        return result;
    }

	@Override
	public Object visitStmtAssign(StmtAssign stmt)
	{
        String op;
        switch(stmt.getOp())
        {
	        case 0: op = " = "; break;
	        case ExprBinary.BINOP_ADD: op = " += "; break;
	        case ExprBinary.BINOP_SUB: op = " -= "; break;
	        case ExprBinary.BINOP_MUL: op = " *= "; break;
	        case ExprBinary.BINOP_DIV: op = " /= "; break;
	        case ExprBinary.BINOP_BOR: op = " |= "; break;
	        case ExprBinary.BINOP_BAND: op = " &= "; break;
	        case ExprBinary.BINOP_BXOR: op = " ^= "; break;
	        default: throw new IllegalStateException(stmt.toString()+" opcode="+stmt.getOp());
        }
        // Assume both sides are the right type.
        isLHS = true;
        String lhs = (String)stmt.getLHS().accept(this);
        isLHS = false;
        String rhs = (String)stmt.getRHS().accept(this);
        return lhs + op + rhs + ";";
	}

	boolean isLHS = false;
	
	
	public Object visitExprArrayRange(ExprArrayRange exp){
		Expression base=exp.getBase();
		List ranges=exp.getMembers();
		if(ranges.size()==0) throw new IllegalStateException();
		if(ranges.size()>1) throw new UnsupportedOperationException("Multi-range indexing not currently supported.");
		Object o=ranges.get(0);
		if(o instanceof RangeLen) 
		{
			RangeLen range=(RangeLen) o;
			if(range.len()==1 || isLHS) {
				return base.accept(this)+"["+range.start().accept(this)+"]"; 
			}else{
				return base.accept(this)+ ".sub<" + range.len() + ">("+range.start().accept(this) + ")";
			}
		}
		throw new UnsupportedOperationException("Cannot translate complicated array indexing.");
	}
	
	@Override
	public String convertType(Type type)
	{
		assert type instanceof TypePrimitive;
		if(type instanceof TypePrimitive) {
			switch(((TypePrimitive)type).getType()) {
				case TypePrimitive.TYPE_INT8:  return "unsigned char";
				case TypePrimitive.TYPE_INT16: return "unsigned short int";
				case TypePrimitive.TYPE_INT32: return "unsigned int";
				case TypePrimitive.TYPE_INT64: return "unsigned long long";
	            case TypePrimitive.TYPE_BOOLEAN:
				case TypePrimitive.TYPE_BIT:   return "bitvec<1>";
	            case TypePrimitive.TYPE_SIGINT: return "int";
			}
		}		
		return super.convertType(type);
	}

}
