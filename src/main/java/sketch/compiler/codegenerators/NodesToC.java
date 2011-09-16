package sketch.compiler.codegenerators;

import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import sketch.compiler.ast.core.FieldDecl;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.StreamSpec;
import sketch.compiler.ast.core.SymbolTable;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.*;
import sketch.compiler.ast.core.exprs.ExprArrayRange.RangeLen;
import sketch.compiler.ast.core.exprs.ExprTprint.CudaType;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtFor;
import sketch.compiler.ast.core.stmts.StmtLoop;
import sketch.compiler.ast.core.stmts.StmtMinimize;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.stmts.StmtVarDecl.VarDeclEntry;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.ast.core.typs.TypeStruct;
import sketch.compiler.ast.core.typs.TypeStructRef;
import sketch.compiler.ast.cuda.exprs.CudaThreadIdx;
import sketch.compiler.codegenerators.tojava.NodesToJava;
import sketch.util.datastructures.TprintTuple;
import sketch.util.fcns.ZipIdxEnt;
import sketch.util.wrapper.ScRichString;
import sketch.compiler.ast.spmd.stmts.StmtSpmdfork;

import static sketch.util.DebugOut.assertFalse;

import static sketch.util.fcns.ZipWithIndex.zipwithindex;

public class NodesToC extends NodesToJava {

	protected String filename;
	private boolean isBool = true;

	protected boolean addIncludes = true;
	// FIXME hack for bad code generation
    private boolean convertBoolConstants;
    protected final boolean pythonPrintStatements;

    public NodesToC(TempVarGen varGen, String filename, boolean pythonPrintStatements) {
		super(false, varGen);
		this.filename=filename;
        this.pythonPrintStatements = pythonPrintStatements;
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

    // TODO: Is this the place to make the change for test 23?
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
          // NOTE(JY): Changed the cast from unsigned int to unsigned long so
          // that it doesn't lose precision on 64-bit systems.
	        if(exp.getLeft() instanceof ExprArrayInit){
	        	ExprArrayInit eai = (ExprArrayInit) exp.getLeft();
	        	int sz = eai.getElements().size();
	        	left = "bitvec<" + sz + ">((unsigned long)" + left + ")";
	        }

	        if(exp.getRight() instanceof ExprArrayInit){
	        	ExprArrayInit eai = (ExprArrayInit) exp.getRight();
	        	int sz = eai.getElements().size();
	        	right = "bitvec<" + sz + ">((unsigned long)" + right + ")";
	        }
	        break;
          // NOTE(JY): Fixed compiler operator precedence problem with ISO C++
          // by calling operator as method.
	        case ExprBinary.BINOP_RSHIFT: op = ".operator >>";
	        if(exp.getLeft() instanceof ExprConstInt){
	        	left = "bitvec<1>((unsigned)" + left + ")";
	        }
	        if(exp.getLeft() instanceof ExprArrayInit){
	        	ExprArrayInit eai = (ExprArrayInit) exp.getLeft();
	        	int sz = eai.getElements().size();
	        	left = "bitvec<" + sz + ">((unsigned)" + left + ")";
	        }
          // Need parens here because it's an argument to the operator >>
          // method.
	        right = "((unsigned)" + right + ")";
	        break;
          // NOTE(JY): Same operator fix here.
	        case ExprBinary.BINOP_LSHIFT: op = ".operator <<";
	        if(exp.getLeft() instanceof ExprConstInt){
	        	left = "bitvec<1>((unsigned)" + left + ")";
	        }
	        if(exp.getLeft() instanceof ExprArrayInit){
	        	ExprArrayInit eai = (ExprArrayInit) exp.getLeft();
	        	int sz = eai.getElements().size();
	        	left = "bitvec<" + sz + ">((unsigned)" + left + ")";
	        }
          // Need parents here because it's an argument to the operator <<
          // method.
	        right = "((unsigned)" + right + ")";
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
        if (ctype.equals(TypePrimitive.bittype)) {
            return "bitvec<1>(" + exp.getVal() + "U)";
		}else{
			return exp.getVal() + "";
		}
    }

	public Object visitExprArrayInit(ExprArrayInit exp)
    {

		if(isBool){
            assert getType(exp.getElements().get(0)).equals(TypePrimitive.bittype) : "bit initializer not a bit";
			StringBuffer sb = new StringBuffer();
			sb.append("((char*) \"");

			List elems = exp.getElements();
			for (int i=0; i<elems.size(); i++) {
			    sb.append(elems.get(i));
			}

			sb.append("\")");
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


        SymbolTable oldSymTab = symtab;
        symtab = new SymbolTable(symtab);
        nres.setPackage(spec);

		String result = "";

        
        for (Iterator iter = spec.getStructs().iterator(); iter.hasNext();) {
            TypeStruct struct = (TypeStruct) iter.next();
            result += outputStructure(struct);
        }

		for(FieldDecl v : spec.getVars()){
		    result += v.accept(this);
        }
		
		for (Iterator iter = spec.getFuncs().iterator(); iter.hasNext(); )
        {
            Function oldFunc = (Function)iter.next();
            symtab.registerFn(oldFunc);
            result += (String)oldFunc.accept(this);
        }
        

        
        
        symtab = oldSymTab;

        return result;
	}

    public String escapeCName(String s) {
        if (s.equals("main")) {
            return "main_c_escape";
        } else if (s.equals("operator")) {
            return "operator_c_escape";
        } else {
            return s;
        }
    }

	public Object visitFunction(Function func)
    {
		SymbolTable oldSymTab = symtab;
        symtab = new SymbolTable(symtab);

        String result = indent ;

        result += convertType(func.getReturnType()) + " ";
        result += escapeCName(func.getName());
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
   		 			Statement r = new StmtAssign(new ExprVar(func, escapeCName(p.getName())), ExprConstInt.zero);
   		 			result += "\t" + (String) r.accept(this) + ";\n";
   		 		}
   		 	}
   		 	result += "\n}";
		}else{
      // NOTE(JY): Inserted code to handle the empty body case.
			String body = (String)func.getBody().accept(this);
      if (body.length() == 0) {
        result += "{}";
      } else {
        result += body;
      }
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
	            symtab.registerVar(escapeCName(param.getName()),
	                    actualType(param.getType()),
	                    param,
	                    SymbolTable.KIND_FUNC_PARAM);
            }

            if (!first) result += ", ";
            if (prefix != null) result += prefix + " ";
            result += typeForParam(type, param.isParameterOutput());
            result += " ";
            result += escapeCName(param.getName());
            first = false;
        }
        result += ")";
        return result;
    }

    public String typeForParam(Type type, boolean isOutput){
        return convertType(type) + (isOutput ? "&" : "");
    }



    public String typeForDecl(Type type) {
        return convertType(type) +  " ";
    }

    public Object visitStmtVarDecl(StmtVarDecl stmt) {
        Vector<String> decls = new Vector<String>();
        for (VarDeclEntry decl : stmt) {
            symtab.registerVar(escapeCName(decl.getName()), actualType(decl.getType()), stmt,
                    SymbolTable.KIND_LOCAL);
            Type type = decl.getType();
            if (type instanceof TypeArray) {
                if (!((TypeArray) type).getBase().equals(TypePrimitive.bittype)) {
                    isBool = false;
                }
            }
            String result = typeForDecl(type);
            boolean oldIsBool = this.isBool;
            setCtype(type);
            if (decl.getInit() != null) {
                result +=
                        " " +
                                processAssign(decl.getVarRefToName(stmt), decl.getInit(),
                                        type, "=");
            } else {
                result += " " + escapeCName(decl.getName());
            }
            decls.add(result);
            this.isBool = oldIsBool;
        }
        return (new ScRichString(", ")).join(decls);
    }

	public Object visitExprFunCall(ExprFunCall exp)
    {
		String result = "";
        String name = escapeCName(exp.getName());
        result = name + "(";
        boolean first = true;
        for (Iterator iter = exp.getParams().iterator(); iter.hasNext();) {
            Expression param = (Expression) iter.next();
            boolean oldIsBool = this.isBool;
            this.isBool = false;
            if (!first)
                result += ", ";
            first = false;
            result += (String) param.accept(this);
            this.isBool = oldIsBool;

        }
        result += ")";
        return result;
    }

  // JY: We need to print the bodies of for loops.
  public Object visitStmtFor(StmtFor stmt) {
    String result = ""; 
    result += indent + "for (";
    Statement init = stmt.getInit();
    if (init != null) {
      result += (String)init.accept(this);
    }
    result += ";";
    Expression cond = stmt.getCond();
    if (cond != null) {
      result += (String)cond.accept(this);
    }
    result += ";";
    Statement incr = stmt.getIncr();
    if (incr != null) {
      result += (String)incr.accept(this);
    }
    result += ")";
    String body = (String)stmt.getBody().accept(this);
    if (body.length() == 0) { 
      result += "{}";
    } else { 
      result += body;
    }
    return result;
  }

  @Override
  public Object visitStmtSpmdfork(StmtSpmdfork stmt) {
    String result = indent + "spmdfork (" + stmt.getLoopVarName();
    result += stmt.getNProc().accept(this);
    result += stmt.getBody().accept(this);
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
    String body = (String)stmt.getBody().accept(this);
    if (body.length() == 0) {
      result += "{}";
    } else {
      result += body;
    }
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

        // method also used by StmtVarDecl
        return processAssign(stmt.getLHS(), stmt.getRHS(), getType(stmt.getLHS()), op);
    }

    private String processAssign(Expression lhs, Expression rhs, Type lhsType, String op)
    {
        boolean oldIsBool = isBool;
        setCtype(lhsType);

        isLHS = true;
        String lhsStr = (String) lhs.accept(this);
        isLHS = false;
        String rhsStr = (String) rhs.accept(this);

        if (lhsType instanceof TypeArray) {
            Type otype = lhsType;
            lhsType = ((TypeArray) lhsType).getBase();
            if (!lhsType.equals(TypePrimitive.bittype)) {
                rhsStr =
                        "(fixedarr<" + convertType(lhsType) + ", " +
                                ((TypeArray) otype).getLength() + ">)" + rhsStr;
            }
        } else if (lhsType.equals(TypePrimitive.bittype)) {
            assert ctype.equals(TypePrimitive.bittype);
//            boolean oldConvertBoolConstants = this.convertBoolConstants;
//            this.convertBoolConstants = true;
//            rhsStr = "CASTHERE " + getType(rhs) + rhsStr;
//            this.convertBoolConstants = oldConvertBoolConstants;
        }
        
        this.isBool = oldIsBool;

        return lhsStr + op + rhsStr;
    }

    protected void setCtype(Type type) {
        ctype = type;

        /*
         * FIXME -- what's going on here??
         */
        while (ctype instanceof TypeArray) {
            ctype = ((TypeArray) ctype).getBase();
        }

        /** generate char arrays for bit vector constants */
        if (type instanceof TypeArray) {
            if (((TypeArray) type).getBase().equals(TypePrimitive.bittype)) {
                isBool = true;
            }
        }
    }

  boolean isLHS = false;


  public Object visitExprArrayRange(ExprArrayRange exp){
    Expression base=exp.getBase();
    
    
    
    {
      RangeLen range=exp.getSelection();
      if(isLHS) {
        isLHS = false;
        Type tmptype = ctype;
        ctype = TypePrimitive.inttype;
				String tmp = (String) range.start().accept(this);
				ctype = tmptype;
				isLHS = true;
				return base.accept(this)+"["+tmp+"]"; //+postfix;
			}else{
				Type tmptype = ctype;
				ctype = TypePrimitive.inttype;
				String tmp = (String) range.start().accept(this);
				ctype = tmptype;
        String lhs = (String)base.accept(this);
        Type curType = (Type)getType(exp.getBase());
        boolean isBitvec =
          ((curType instanceof TypeArray) &&
          ((TypeArray)curType).getBase().equals(TypePrimitive.bittype)) 
          ||
          curType.equals(TypePrimitive.bittype);
            if (!isBitvec && !range.hasLen()) {
              return lhs + ".get("+ tmp + ")";
				} else{
                    if (range.getLenExpression() == null) {
                        return lhs + ".sub<1>(" + tmp + ")";
                    }
					return lhs + ".sub<" + range.getLenExpression() + ">("+ tmp + ")";
				}
			}
		}		
	}

	public Object visitExprField(ExprField exp)
    {
        String result = "";
        result += (String)exp.getLeft().accept(this);
        // NOTE(JY): We want to access the val() field whenever we have a
        // fixedarrRef, which we get from indexing into a non-bittype array.
        if (isLHS && exp.getLeft() instanceof ExprArrayRange) {
          Expression lhsBase = ((ExprArrayRange)exp.getLeft()).getBase();
          Type baseType = (Type)getType(lhsBase);
          if (baseType instanceof TypeArray) {
            if (!((TypeArray)baseType).getBase().equals(TypePrimitive.bittype)) {
              result += ".val()";
            }
          }
        }
        result += "->";
        result += escapeCName((String)exp.getName());
        return result;
    }

	public Object visitExprTypeCast(ExprTypeCast exp) {
        String exprInner = (String)exp.getExpr().accept(this);
        if (exp.getType() instanceof TypeArray) {
            return convertType(exp.getType()) + "(" + exprInner + ")";
        }
        return "((" + convertType(exp.getType()) + ")(" + exprInner + "))";
    }


	@Override
	public String convertType(Type type)
	{
        if (type instanceof TypeStructRef) {
			return type.toString() + "*";

        } else if (type instanceof TypePrimitive) {
			switch(((TypePrimitive)type).getType()) {
				case TypePrimitive.TYPE_INT8:  return "unsigned char";
				case TypePrimitive.TYPE_INT16: return "unsigned short int";
				case TypePrimitive.TYPE_INT32: return "int";
				case TypePrimitive.TYPE_INT64: return "unsigned long long";
	            case TypePrimitive.TYPE_BOOLEAN:
				case TypePrimitive.TYPE_BIT:   return "bitvec<1>";
	            case TypePrimitive.TYPE_SIGINT: return "int";
			}

        } else if (type instanceof TypeArray) {
            TypeArray t = (TypeArray)type;
            Type typBase = t.getBase();
            if (typBase.equals(TypePrimitive.bittype)) {
                return "bitvec<" + t.getLength() + ">";
            } else {
                return "fixedarr<" + convertType(typBase) + ", " + t.getLength() + ">";
            }

		} else {
            assertFalse("unknown type to convert: ", type);
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

    @Override
    public Object visitExprTprint(ExprTprint exprTprint) {
        StringBuilder result = new StringBuilder();
        result.append("cout");
        for (ZipIdxEnt<TprintTuple> v : zipwithindex(exprTprint.expressions)) {
            if (v.idx > 0) {
                result.append("\n" + this.indent);
            }
            final String name;
            final int nexpr = exprTprint.expressions.size();
            if (pythonPrintStatements) {
                name = tprintFmtPy(nexpr, v.idx == 0, v.entry.getFirst());
                result.append(" << \"" + name + "\" << " + v.entry.getSecond());
            } else {
                name = tprintFmt(nexpr, v.idx == 0, v.entry.getFirst());
                result.append(" << \"" + name + "\" << " + v.entry.getSecond());
            }
        }
        if (pythonPrintStatements) {
            result.append(" << \"),\" << endl");
        } else {
            result.append(" << endl");
        }
        return result.toString();
    }

    public String tprintFmt(int nexpr, boolean isFirst, final String name) {
        if (nexpr <= 1) {
            return ScRichString.padLeft(name, 30) + ": ";
        } else {
            return (isFirst ? "" : " ") + name + " ";
        }
    }

    public String tprintFmtPy(int nexpr, boolean isFirst, final String name) {
        if (isFirst) {
            return "    " + pyClassName(name) + "(";
        } else {
            return ", " + pyFieldName(name) + "=";
        }
    }

    /** capitalized name */
    public static String pyClassName(String name) {
        String result = String.valueOf(name.charAt(0)).toUpperCase();
        boolean nextCapital = false;
        for (int a = 1; a < name.length(); a++) {
            char c = name.charAt(a);
            if (c == ' ') {
                nextCapital = true;
            } else if (nextCapital) {
                result += Character.toUpperCase(c);
                nextCapital = false;
            } else {
                result += c;
            }
        }
        return result;
    }

    public static String pyFieldName(String name) {
        return name.replace(' ', '_').toLowerCase();
    }

    @Override
    public Object visitCudaThreadIdx(CudaThreadIdx cudaThreadIdx) {
        return cudaThreadIdx.toString();
    }

    @Override
    public Object visitStmtMinimize(StmtMinimize stmtMinimize) {
        return (new ExprTprint(stmtMinimize, CudaType.Unknown, new TprintTuple("[line " +
                stmtMinimize.getCx().getLineNumber() + "] minimize() value",
                stmtMinimize.getMinimizeExpr()))).accept(this);
    }
}
