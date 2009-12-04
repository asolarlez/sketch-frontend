package sketch.compiler.codegenerators;

import java.util.Iterator;
import java.util.List;

import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.StreamSpec;
import sketch.compiler.ast.core.StreamType;
import sketch.compiler.ast.core.SymbolTable;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.*;
import sketch.compiler.ast.core.exprs.ExprArrayRange.RangeLen;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtLoop;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.ast.core.typs.TypeStruct;
import sketch.compiler.ast.core.typs.TypeStructRef;
import sketch.compiler.codegenerators.tojava.NodesToJava;

public class NodesToC extends NodesToJava {

	private String filename;
	private boolean isBool = true;
	protected boolean addIncludes = true;


	public NodesToC(TempVarGen varGen, String filename) {
		super(false, varGen);
		this.filename=filename;
	}





	public Object visitExprBinary(ExprBinary exp)
    {
        StringBuffer result=new StringBuffer();
        String op = null;
        if(binOpLevel>0) result.append("(");
        binOpLevel++;
        Type tmptype = null;
        switch (exp.getOp())
        {
        case ExprBinary.BINOP_NEQ:
        case ExprBinary.BINOP_EQ:
        	Type t1 = getType(exp.getLeft());
        	Type t2 = getType(exp.getRight());
        	while(t1 instanceof TypeArray){ t1 = ((TypeArray)t1).getBase();}
        	while(t2 instanceof TypeArray){ t2 = ((TypeArray)t2).getBase();}
        	if(t1 == TypePrimitive.inttype || t2 == TypePrimitive.inttype){
        		tmptype = ctype;
            	ctype = TypePrimitive.inttype;
        	}else{
        		tmptype = ctype;
            	ctype = t1;
        	}

        	break;
        case ExprBinary.BINOP_LT:
        case ExprBinary.BINOP_LE:
        case ExprBinary.BINOP_GT:
        case ExprBinary.BINOP_GE:
        	tmptype = ctype;
        	ctype = TypePrimitive.inttype;
        }
        String left = (String)exp.getLeft().accept(this);

        if(exp.getOp()== ExprBinary.BINOP_LSHIFT || exp.getOp()== ExprBinary.BINOP_RSHIFT){
        	tmptype = ctype;
        	ctype = TypePrimitive.inttype;
        }
        String right = (String)exp.getRight().accept(this);
        if(tmptype != null){
        	ctype = tmptype;
        }


        switch (exp.getOp())
        {
	        case ExprBinary.BINOP_ADD: op = "+";
	        if(exp.getLeft() instanceof ExprArrayInit){
	        	ExprArrayInit eai = (ExprArrayInit) exp.getLeft();
	        	int sz = eai.getElements().size();
	        	left = "bitvec<" + sz + ">(" + left + ")";
	        }

	        if(exp.getRight() instanceof ExprArrayInit){
	        	ExprArrayInit eai = (ExprArrayInit) exp.getRight();
	        	int sz = eai.getElements().size();
	        	right = "bitvec<" + sz + ">(" + right + ")";
	        }

	        break;
	        case ExprBinary.BINOP_SUB: op = "-"; break;
	        case ExprBinary.BINOP_MUL: op = "*"; break;
	        case ExprBinary.BINOP_DIV: op = "/"; break;
	        case ExprBinary.BINOP_MOD: op = "%"; break;
	        case ExprBinary.BINOP_AND: op = "&&"; break;
	        case ExprBinary.BINOP_OR:  op = "||"; break;
	        case ExprBinary.BINOP_EQ:  op = "==";
	        left = "(" + left + ")";
	        right = "(" + right + ")";
	        break;
	        case ExprBinary.BINOP_NEQ: op = "!=";
	        left = "(" + left + ")";
	        right = "(" + right + ")";
	        break;
	        case ExprBinary.BINOP_LT:  op = "<";
	        left = "(" + left + ")";
	        right = "(" + right + ")";
	        break;
	        case ExprBinary.BINOP_LE:  op = "<=";
	        left = "(" + left + ")";
	        right = "(" + right + ")";
	        break;
	        case ExprBinary.BINOP_GT:  op = ">";
	        left = "(" + left + ")";
	        right = "(" + right + ")";
	        break;
	        case ExprBinary.BINOP_GE:  op = ">=";
	        left = "(" + left + ")";
	        right = "(" + right + ")";
	        break;
	        case ExprBinary.BINOP_BAND:op = "&";
	        if(exp.getLeft() instanceof ExprConstInt){
	        	left = "bitvec<1>(" + left + ")";
	        }
	        if(exp.getRight() instanceof ExprConstInt){
	        	right = "bitvec<1>(" + right + ")";
	        }
	        if(exp.getLeft() instanceof ExprArrayInit){
	        	ExprArrayInit eai = (ExprArrayInit) exp.getLeft();
	        	int sz = eai.getElements().size();
	        	left = "bitvec<" + sz + ">(" + left + ")";
	        }

	        if(exp.getRight() instanceof ExprArrayInit){
	        	ExprArrayInit eai = (ExprArrayInit) exp.getRight();
	        	int sz = eai.getElements().size();
	        	right = "bitvec<" + sz + ">(" + right + ")";
	        }
	        break;
	        case ExprBinary.BINOP_BOR: op = "|";
	        if(exp.getLeft() instanceof ExprConstInt){
	        	left = "bitvec<1>((unsigned)" + left + ")";
	        }
	        if(exp.getRight() instanceof ExprConstInt){
	        	right = "bitvec<1>((unsigned)" + right + ")";
	        }
	        if(exp.getLeft() instanceof ExprArrayInit){
	        	ExprArrayInit eai = (ExprArrayInit) exp.getLeft();
	        	int sz = eai.getElements().size();
	        	left = "bitvec<" + sz + ">(" + left + ")";
	        }

	        if(exp.getRight() instanceof ExprArrayInit){
	        	ExprArrayInit eai = (ExprArrayInit) exp.getRight();
	        	int sz = eai.getElements().size();
	        	right = "bitvec<" + sz + ">(" + right + ")";
	        }
	        break;
	        case ExprBinary.BINOP_BXOR:op = "^";
	        if(exp.getLeft() instanceof ExprConstInt){
	        	left = "bitvec<1>((unsigned)" + left + ")";
	        }
	        if(exp.getRight() instanceof ExprConstInt){
	        	right = "bitvec<1>((unsigned)" + right + ")";
	        }
	        if(exp.getLeft() instanceof ExprArrayInit){
	        	ExprArrayInit eai = (ExprArrayInit) exp.getLeft();
	        	int sz = eai.getElements().size();
	        	left = "bitvec<" + sz + ">((unsigned)" + left + ")";
	        }

	        if(exp.getRight() instanceof ExprArrayInit){
	        	ExprArrayInit eai = (ExprArrayInit) exp.getRight();
	        	int sz = eai.getElements().size();
	        	right = "bitvec<" + sz + ">((unsigned)" + right + ")";
	        }
	        break;
	        case ExprBinary.BINOP_RSHIFT: op = ">>";
	        if(exp.getLeft() instanceof ExprConstInt){
	        	left = "bitvec<1>((unsigned)" + left + ")";
	        }
	        if(exp.getLeft() instanceof ExprArrayInit){
	        	ExprArrayInit eai = (ExprArrayInit) exp.getLeft();
	        	int sz = eai.getElements().size();
	        	left = "bitvec<" + sz + ">((unsigned)" + left + ")";
	        }
	        right = "(unsigned)" + right;
	        break;
	        case ExprBinary.BINOP_LSHIFT: op = "<<";
	        if(exp.getLeft() instanceof ExprConstInt){
	        	left = "bitvec<1>((unsigned)" + left + ")";
	        }
	        if(exp.getLeft() instanceof ExprArrayInit){
	        	ExprArrayInit eai = (ExprArrayInit) exp.getLeft();
	        	int sz = eai.getElements().size();
	        	left = "bitvec<" + sz + ">((unsigned)" + left + ")";
	        }
	        right = "(unsigned)" + right;
	        break;
	        default: assert false : exp; break;
        }
        result.append(left);
        result.append(" ").append(op).append(" ");
        result.append(right);
        binOpLevel--;
        if(binOpLevel>0) result.append(")");
        return result.toString();
    }



	public Object visitExprConstInt(ExprConstInt exp)
    {
		if(ctype == TypePrimitive.bittype){
			return "bitvec<1>("+ exp.getVal() +"U)" ;
		}else{
			return exp.getVal() + "";
		}
    }

	public Object visitExprArrayInit(ExprArrayInit exp)
    {

		if(isBool){
			StringBuffer sb = new StringBuffer();
			sb.append("\"");

			List elems = exp.getElements();
			for (int i=0; i<elems.size(); i++) {
			    sb.append(elems.get(i));
			}

			sb.append("\"");
	        return sb.toString();
		}else{
			StringBuffer sb = new StringBuffer();
			List elems = exp.getElements();
			sb.append("fixedarr< int," + elems.size() + ">()");
			for (int i=0; i<elems.size(); i++) {
			    sb.append(".v(" + i + ","+elems.get(i)+")");
			}
	        return sb.toString();

		}
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
		if(addIncludes){
		preamble.append("#include <cstdio>\n");
		preamble.append("#include <assert.h>\n");
		preamble.append("#include \"");
		preamble.append(filename);
		preamble.append(".h\"\n");
		}
		preamble.append(ret);
		return preamble.toString();
	}

	public String outputStructure(TypeStruct struct){
    	return "";
    }



	public Object visitStreamSpec(StreamSpec spec){

		StreamType oldStreamType = streamType;
        SymbolTable oldSymTab = symtab;
        symtab = new SymbolTable(symtab);
        streamType = spec.getStreamType();

		String result = "";
		ss = spec;
        for (Iterator iter = spec.getFuncs().iterator(); iter.hasNext(); )
        {
            Function oldFunc = (Function)iter.next();
            symtab.registerFn(oldFunc);
            result += (String)oldFunc.accept(this);
        }

        symtab = oldSymTab;
        streamType = oldStreamType;
        return result;
	}

	public Object visitFunction(Function func)
    {


		SymbolTable oldSymTab = symtab;
        symtab = new SymbolTable(symtab);


        String result = indent ;
        if (!func.getName().equals(ss.getName()))
            result += convertType(func.getReturnType()) + " ";
        result += func.getName();
        String prefix = null;
        result += doParams(func.getParams(), prefix) + " ";

        if(func.isUninterp()){
        	List<Parameter> l = func.getParams();
        	result += "{ \n";
   		 	result += "\t/* This was defined as an uninterpreted function. " +
   		 			"\n\t   Add your own body here. */ \n";
   		 	for(Iterator<Parameter> it = l.iterator(); it.hasNext(); ){
   		 		Parameter p = it.next();
   		 		if(p.isParameterOutput()){
   		 			Statement r = new StmtAssign(new ExprVar(func, p.getName()), ExprConstInt.zero);
   		 			result += "\t" + (String) r.accept(this) + ";\n";
   		 		}
   		 	}
   		 	result += "\n}";
		}else{
			result += (String)func.getBody().accept(this);
		}
        result += "\n";

        symtab = oldSymTab;

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

            if(symtab != null){
	            symtab.registerVar(param.getName(),
	                    actualType(param.getType()),
	                    param,
	                    SymbolTable.KIND_FUNC_PARAM);
            }

            if (!first) result += ", ";
            if (prefix != null) result += prefix + " ";
            result += typeForParam(type, param.isParameterOutput());
            result += " ";
            result += param.getName();
            first = false;
        }
        result += ")";
        return result;
    }

    public String typeForParam(Type type, boolean isOutput){
    	String postfix = isOutput? "&" : "";
        if(type instanceof TypeArray){
        	TypeArray otype = (TypeArray)type;
        	type = ((TypeArray)type).getBase();
        	if(type.equals(TypePrimitive.bittype)){
        		return "bitvec<" + (otype).getLength() + "> " + postfix;
        	}else{
        		return "fixedarr<" + convertType(type) + ", " + (otype).getLength() + "> " + postfix;
        	}
        }else{
        	return convertType(type) + postfix;
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
        		return "fixedarr<" + convertType(type) + ", " + ((TypeArray)otype).getLength() + "> " + name;
        	}
        }
        return convertType(type) +  " " + name + postFix ;
    }


    public Object visitStmtVarDecl(StmtVarDecl stmt)
    {
        String result = "";
        for (int i = 0; i < stmt.getNumVars(); i++)
        {
        	if(i > 0){
        		result += "; ";
        	}
        	symtab.registerVar(stmt.getName(i),
                    actualType(stmt.getType(i)),
                    stmt,
                    SymbolTable.KIND_LOCAL);
            Type type = stmt.getType(i);
            if(type instanceof TypeArray){
	            if( !((TypeArray)type).getBase().equals( TypePrimitive.bittype )){
	            	isBool = false;
	            }
            }
            result += typeForDecl(type,   stmt.getName(i));
            ctype = type;
            while(ctype instanceof TypeArray){
            	ctype = ((TypeArray)ctype).getBase();
            }
            if (stmt.getInit(i) != null)
                result += " = " + (String)stmt.getInit(i).accept(this);
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

        ctype = getType(stmt.getLHS());
        while(ctype instanceof TypeArray){
        	ctype = ((TypeArray)ctype).getBase();
        }

        isLHS = true;
        String lhs = (String)stmt.getLHS().accept(this);
        isLHS = false;
        String rhs = (String)stmt.getRHS().accept(this);
        return lhs + op + rhs ;
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
			if(isLHS) {
				isLHS = false;
				Type tmptype = ctype;
				ctype = TypePrimitive.inttype;
				String tmp = (String) range.start().accept(this);
				ctype = tmptype;
				isLHS = true;
				return base.accept(this)+"["+tmp+"]";
			}else{
				Type tmptype = ctype;
				ctype = TypePrimitive.inttype;
				String tmp = (String) range.start().accept(this);
				ctype = tmptype;
				if(range.len()==1){
					return base.accept(this)+ ".get("+ tmp + ")";
				}else{
					return base.accept(this)+ ".sub<" + range.len() + ">("+ tmp + ")";
				}
			}
		}
		throw new UnsupportedOperationException("Cannot translate complicated array indexing.");
	}

	public Object visitExprField(ExprField exp)
    {
        String result = "";
        result += (String)exp.getLeft().accept(this);
        result += "->";
        result += (String)exp.getName();
        return result;
    }

	public Object visitExprTypeCast(ExprTypeCast exp)
    {

    	if( exp.getType() instanceof TypeArray ){
    		TypeArray t = (TypeArray)exp.getType();
    		assert t.getBase().equals(TypePrimitive.bittype): "TASDVAS " + exp;

    		return "bitvec<" + t.getLength() + ">(" +
            (String)exp.getExpr().accept(this) + ")";

    	}

        return "((" + convertType(exp.getType()) + ")(" +
            (String)exp.getExpr().accept(this) + "))";
    }


	@Override
	public String convertType(Type type)
	{
	    assert type instanceof TypePrimitive || type instanceof TypeStructRef || type instanceof TypeStruct;
		if(type instanceof TypeStructRef){
			return type.toString() + "*";
		}

		if(type instanceof TypePrimitive) {
			switch(((TypePrimitive)type).getType()) {
				case TypePrimitive.TYPE_INT8:  return "unsigned char";
				case TypePrimitive.TYPE_INT16: return "unsigned short int";
				case TypePrimitive.TYPE_INT32: return "int";
				case TypePrimitive.TYPE_INT64: return "unsigned long long";
	            case TypePrimitive.TYPE_BOOLEAN:
				case TypePrimitive.TYPE_BIT:   return "bitvec<1>";
	            case TypePrimitive.TYPE_SIGINT: return "int";
			}
		}
		return super.convertType(type);
	}
	public Object visitExprNullPtr(ExprNullPtr nptr){ return "NULL"; }
	public Object visitExprConstBoolean(ExprConstBoolean exp)
    {
        if (exp.getVal())
            return "bitvec<1>(1U)";
        else
            return "bitvec<1>(0U)";
    }
}
