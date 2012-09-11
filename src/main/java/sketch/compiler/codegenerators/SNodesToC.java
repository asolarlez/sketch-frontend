package sketch.compiler.codegenerators;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.Package;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.ExprBinary;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.ExprUnary;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtLoop;
import sketch.compiler.ast.core.stmts.StmtReturn;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.codegenerators.tojava.NodesToJava;

public class SNodesToC extends NodesToJava {

	private static final class ArrayGen {
		private final int dim;
		private final String indent="\t\t\t";
		private final String arrayType;
		private final String arrayPtrType;
		private StringBuffer buf;
		
		public ArrayGen(int d) {
			dim=d;
			arrayType=getArrayTypeName();
			arrayPtrType=arrayType+"Ptr";
		}
		
		public String getArrayTypeName() {
			return "Array"+dim+"D";
		}
		
		public void generateCode(StringBuffer b) {
			buf=b;
			
			buf.append("class ");
			buf.append(arrayPtrType);
			buf.append(" {\n");

			buf.append("\tfriend class ");
			buf.append(arrayType);
			buf.append(";\n");

			buf.append("\tprivate:\n");

			buf.append("\t\t");
			buf.append(dataType(dim));
			buf.append(" data;\n");

			buf.append(makeDimFields(dim));

			buf.append("\t\t");
			buf.append(arrayPtrType);
			buf.append("(");
			buf.append(dataType(dim));
			buf.append(" d, ");
			buf.append(ctorParams(dim));
			buf.append(");\n");

			buf.append("};\n");

			buf.append("\n");

			buf.append("class ");
			buf.append(arrayType);
			buf.append(" {\n");

			buf.append("\tprivate:\n");

			buf.append("\t\t");
			buf.append(dataType(dim));
			buf.append(" data;\n");

			buf.append("\t\tvoid clear() {\n");

			buf.append("\t\t\tif(data==0) return;\n");

			buf.append(makeDeleteCode(dim));

			buf.append("\t\t\tdata=0;\n");

			buf.append("\t\t}\n");

			buf.append("\tpublic:\n");

			buf.append(makeDimFields(dim));

			buf.append("\t\t");
			buf.append(arrayType);
			buf.append("(");
			buf.append(ctorParams(dim));
			buf.append("):");
			buf.append(ctorInits(dim));
			buf.append(" {\n");

			buf.append(makeAllocCode(dim));

			buf.append("\t\t\t");
			buf.append(loopNest(dim));
			buf.append("data");
			buf.append(indexNest(dim));
			buf.append("=0;\n");

			buf.append("\t\t}\n");

			buf.append("\t\t");
			buf.append(arrayType);
			buf.append("(");
			buf.append(arrayPtrType);
			buf.append(" p):");
			buf.append(ctorInits2(dim));
			buf.append(" {\n");

			buf.append("\t\t\tdata=p.data;\n");

			buf.append("\t\t}\n");

			buf.append("\t\t~");
			buf.append(arrayType);
			buf.append("() {clear();}\n");

			buf.append("\t\t");
			buf.append(dataType(dim-1));
			buf.append("& operator[] (int i) {return data[i];}\n");

			buf.append("\t\t");
			buf.append(arrayType);
			buf.append("& operator= (");
			buf.append(arrayType);
			buf.append("& other) {\n");

			buf.append(makeAssertCode(dim));

			buf.append("\t\t\t");
			buf.append(loopNest(dim));
			buf.append("data");
			buf.append(indexNest(dim));
			buf.append("=other.data");
			buf.append(indexNest(dim));
			buf.append(";\n");

			buf.append("\t\t\treturn *this;\n");

			buf.append("\t\t}\n");

			buf.append("\t\t");
			buf.append(arrayType);
			buf.append("& operator= (");
			buf.append(arrayPtrType);
			buf.append(" other) {\n");

			buf.append(makeAssertCode(dim));

			buf.append("\t\t\tdata=other.data;\n");

			buf.append("\t\t\treturn *this;\n");

			buf.append("\t\t}\n");

			buf.append("\t\t");
			buf.append(arrayPtrType);
			buf.append(" makePtr() {\n");

			buf.append("\t\t\t");
			buf.append(arrayPtrType);
			buf.append(" ret(data,");
			buf.append(dimFields(dim));
			buf.append(");\n");

			buf.append("\t\t\tdata=0;\n");

			buf.append("\t\t\treturn ret;\n");

			buf.append("\t\t}\n");

			buf.append("};\n");

			buf.append("\n");

			buf.append("");
			buf.append(arrayPtrType);
			buf.append("::");
			buf.append(arrayPtrType);
			buf.append("(");
			buf.append(dataType(dim));
			buf.append(" d, ");
			buf.append(ctorParams(dim));
			buf.append("):");
			buf.append(ctorInits(dim));
			buf.append(" {data=d;}\n");
		}

		private String dataType(int dim) {
			String ret="double";
			for(int i=0;i<dim;i++) ret+="*";
			return ret;
		}
		
		private String forLoop(int d) {
			//for(int i0=0;i0<dim0;i0++)
			return "for(int i"+d+"=0;i"+d+"<dim"+d+";i"+d+"++) "; 
		}

		private String loopNest(int dim) {
			//for(int i0=0;i0<dim0;i0++) for(int i1=0;i1<dim1;i1++) 
			String ret="";
			for(int i=0;i<dim;i++)
				ret+=forLoop(i);
			return ret;
		}

		private String indexNest(int dim) {
			//[i0][i1]
			String ret="";
			for(int i=0;i<dim;i++)
				ret+="[i"+i+"]";
			return ret;
		}

		private String makeAllocCode(int dim) {
			//data=new double**[dim0];
			//for(int i0=0;i0<dim0;i0++) data[i0]=new double*[dim1];
			//for(int i0=0;i0<dim0;i0++) for(int i1=0;i1<dim1;i1++) data[i0][i1]=new double[dim2];
			String ret="";
			for(int d=0;d<dim;d++) {
				ret+=indent;
				ret+=loopNest(d);
				ret+="data"+indexNest(d)+"=new "+dataType(dim-1-d)+"[dim"+d+"];\n";
			}
			return ret;
		}

		private String makeDeleteCode(int dim) {
			//for(int i0=0;i0<dim0;i0++) for(int i1=0;i1<dim1;i1++) delete[] data[i0][i1];
			//for(int i0=0;i0<dim0;i0++) delete[] data[i0];
			//delete[] data;
			String ret="";
			for(int d=dim-1;d>=0;d--) {
				ret+=indent;
				ret+=loopNest(d);
				ret+="delete[] data"+indexNest(d)+";\n";
			}
			return ret;
		}

		private String makeAssertCode(int dim) {
			//assert(dim0==other.dim0);
			String ret="";
			for(int i=0;i<dim;i++) {
				ret+=indent+"assert(dim"+i+"==other.dim"+i+");\n";
			}
			return ret;
		}

		private String makeDimFields(int dim) {
			//const int dim0;
			String ret="";
			String indent=this.indent.substring(1);
			for(int i=0;i<dim;i++) {
				ret+=indent+"const int dim"+i+";\n";
			}
			return ret;
		}
		
		private String dimFields(int dim) {
	        //dim0,dim1
			String ret="";
			for(int i=0;i<dim;i++) {
				if(i>0) ret+=",";
				ret+="dim"+i;
			}
			return ret;
        }

		private String ctorParams(int dim) {
			//int d0, int d1
			String ret="";
			for(int i=0;i<dim;i++) {
				if(i>0) ret+=", ";
				ret+="int d"+i;
			}
			return ret;
		}

		private String ctorInits(int dim) {
			//dim0(d0),dim1(d1)
			String ret="";
			for(int i=0;i<dim;i++) {
				if(i>0) ret+=",";
				ret+="dim"+i+"(d"+i+")";
			}
			return ret;
		}

		private String ctorInits2(int dim) {
			//dim0(p.dim0),dim1(p.dim1)
			String ret="";
			for(int i=0;i<dim;i++) {
				if(i>0) ret+=",";
				ret+="dim"+i+"(p.dim"+i+")";
			}
			return ret;
		}

	}
	
	private static Map<String,String> macroDefinitions=null;

	private final String filename; 
	private final List<String> usedMacros;
	/** List containing the dimensionalities of array types used in the program */
	private final List<Integer> usedArrayTypes;
	private boolean curFunctionReturnsArray;
	
	public SNodesToC(TempVarGen varGen, String filename) {
		super(false, varGen);
		this.filename=filename;
		usedMacros=new ArrayList<String>();
		usedArrayTypes=new ArrayList<Integer>();
		if(macroDefinitions==null) {
			macroDefinitions=new HashMap<String,String>();
		}
	}
	
	private void requireMacro(String m) {
		if(usedMacros.contains(m)) return;
		assert macroDefinitions.containsKey(m);
		usedMacros.add(m);
	}

	private void requireArray(int dim) {
		if(usedArrayTypes.contains(dim)) return;
		usedArrayTypes.add(dim);
	}
	
	protected String generateArrayImpl(int dim) {
		StringBuffer buf=new StringBuffer();
		ArrayGen ag=new ArrayGen(dim);
		ag.generateCode(buf);
		return buf.toString();
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
		preamble.append("\n");
		for(String macroName: usedMacros) {
			preamble.append(macroDefinitions.get(macroName));
			preamble.append("\n");
		}
		for(Integer dim: usedArrayTypes) {
			String code=generateArrayImpl(dim);
			preamble.append(code);
			preamble.append("\n");
		}
		preamble.append(ret);
		return preamble.toString();
	}

	public Object visitStreamSpec(Package spec){
		String result = "";
        nres.setPackage(spec);
        for (Iterator iter = spec.getFuncs().iterator(); iter.hasNext(); )
        {
            Function oldFunc = (Function)iter.next();            
            result += (String)oldFunc.accept(this);            
        }
        return result;
	}
	
	public Object visitFunction(Function func)
    {
		curFunctionReturnsArray=(func.getReturnType() instanceof TypeArray);
        String result = indent ;

        result += convertType(func.getReturnType(), true) + " ";
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
            if(param.isParameterOutput()) continue;
            Type type = param.getType();
            String postFix = "";
            if(type instanceof TypeArray){
            	postFix = "&";
            }
            
            if (!first) result += ", ";
            if (prefix != null) result += prefix + " ";
            result += convertType(type) + postFix;
            if(param.getType() instanceof TypePrimitive){
            	result += "&";
            }
            result += " ";
            result += param.getName();
            first = false;
        }
        result += ")";
        return result;
    }

    public Object visitStmtVarDecl(StmtVarDecl stmt)
    {
        String result = "";
        Type type = stmt.getType(0);
        result += convertType(type) +  " ";
        boolean isArray=false;
        
        String postFix = "";
        if(type instanceof TypeArray){
        	isArray=true;
        	List<Expression> dims=((TypeArray)type).getDimensions();
        	for(int i=0;i<dims.size();i++) {
        		if(i>0) postFix+=",";
        		postFix+=(String)dims.get(i).accept(this);
        	}
        	postFix = "(" + postFix +  ")";
        }
        
        for (int i = 0; i < stmt.getNumVars(); i++)
        {
            if (i > 0)
                result += ", ";
            result += stmt.getName(i)+ postFix ;
            if (stmt.getInit(i) != null) {
	            if(isArray) {
	            	assert stmt.getInit(i) instanceof ExprConstInt : stmt.getInit(i) + " not a constant int";
	            	assert ((ExprConstInt)stmt.getInit(i)).getVal()==0;
	            } else { 
	                result += " = " + (String)stmt.getInit(i).accept(this);
	            }
            }
        }
        return result;
    }
	
	public Object visitExprFunCall(ExprFunCall exp)
    {
		String result = "";
        String name = exp.getName();
        if(macroDefinitions.containsKey(name)) {
        	requireMacro(name);
        }
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
	        case 0: op = " ??= "; break;
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
        return (String)stmt.getLHS().accept(this) + op +
            (String)stmt.getRHS().accept(this);
	}

	public Object visitStmtReturn(StmtReturn stmt) {
		String ret=(String) super.visitStmtReturn(stmt);
		if(curFunctionReturnsArray)
			ret+=".makePtr()";
		return ret;
	}

	public String convertType(Type type) {
		return convertType(type,false);
	}
	
	public String convertType(Type type, boolean isReturnType)
	{
		if(type instanceof TypePrimitive) {
			switch(((TypePrimitive)type).getType()) {
				case TypePrimitive.TYPE_INT8:  return "char";
				case TypePrimitive.TYPE_INT16: return "short int";
				case TypePrimitive.TYPE_INT32: return "int";
				case TypePrimitive.TYPE_INT64: return "long long";
				case TypePrimitive.TYPE_BIT:   return "char";
	            case TypePrimitive.TYPE_SIGINT: return "int";
			}
		}
		if(type instanceof TypeArray){
			TypeArray ta = (TypeArray) type;
			List<Expression> idx=ta.getDimensions();
			int dim=idx.size();
			requireArray(dim);
			return new ArrayGen(dim).getArrayTypeName()+(isReturnType?"Ptr":"");
		}
		return super.convertType(type);
	}

}
