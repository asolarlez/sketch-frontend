package streamit.frontend.codegenerators;

import java.util.*;

import streamit.frontend.nodes.*;

/**
 * FORTRAN 77 code generator.
 * TODO: need a pass before this to "pull up" declarations and break apart
 * assignments from complex initializers (note: exclude loop index vars)
 *
 * @author liviu
 */
public class SNodesToFortran implements FEVisitor {

	private static class VarCollector extends FEReplacer {
		private final List<String> ret;
		public VarCollector(List<String> list) {
			ret=list;
		}
        public Object visitExprVar(ExprVar exp) {
	        String name=exp.getName();
	        if(!ret.contains(name)) ret.add(name);
	        return exp;
        }
	}
	private static class ReturnFinder extends FEReplacer {
		private final List<String> ret;
		public ReturnFinder() {
			ret=new ArrayList<String>();
		}
        public Object visitStmtReturn(StmtReturn stmt) {
        	Expression rval=stmt.getValue();
        	assert rval instanceof ExprVar;
	        String name=((ExprVar)rval).getName();
	        if(!ret.contains(name)) ret.add(name);
	        return stmt;
        }
        public String getReturnVarName() {
        	if(ret.size()==1) return ret.get(0);
        	return null;
        }
	}

	private static final int MAX_LINE_LEN=72;

	private final String filename;
	/** line prefix (6 spaces) */
	private String lp="      ";
	private String cline="c     ";
	private String indentStr="";
	private int lineCounter;

	private Function curFunc;
	private Map<String,Type> varTypes;
	private String outvar;
	private boolean returnsArray;
	private boolean isProcedure;
	private int indent=0;

	public SNodesToFortran(String filename) {
		this.filename=filename;
		lineCounter=1;
		varTypes=new HashMap<String,Type>();
	}

	protected int getNewLabel() {
		return (lineCounter++)*10;
	}

	protected String labeledLP(int lbl) {
		String ret=" "+lbl;
		return ret+lp.substring(ret.length());
	}

	private String getIndent() {
		if(indent*2!=indentStr.length()) {
			indentStr="";
			for(int i=0;i<indent;i++)
				indentStr+="  ";
		}
		return indentStr;
	}

	/**
	 * Takes in a line of code (without \n at the end) and returns
	 * either the same line (with \n appended) or a concatenation of
	 * several lines capped at 72 characters as per Fortran spec.
	 */
	private String lineSplit(String s) {
		if(s.length()<=MAX_LINE_LEN)
			return s+"\n";
		return s.substring(0,MAX_LINE_LEN)+"\n"+lineSplit("     +"+s.substring(MAX_LINE_LEN));
	}

	/**
	 * Returns Fortran-formatted code for the statement encoded in s.
	 * Indentation is added based on the value of the indent field.
	 */
	protected String line(String s) {
		return lineSplit(lp+getIndent()+s);
	}

	/**
	 * Returns Fortran-formatted code for the statement encoded in s and
	 * labeled with lbl.
	 * Indentation is added based on the value of the indent field.
	 */
	protected String line(int lbl,String s) {
		return lineSplit(labeledLP(lbl)+getIndent()+s);
	}

	protected String newline() {
		return "\n";
	}

	protected String getDefaultReturnVar() {
		return "out_";
	}

	protected String getArrayDataType() {
		return "real";
	}

	public Object visitProgram(Program prog)
	{
		String ret=line("program "+filename);
		ret+=line("end");
		ret+=newline();
        for (Iterator iter = prog.getStreams().iterator(); iter.hasNext(); )
            ret += (String)((StreamSpec)iter.next()).accept(this);
		return ret;
	}

	public Object visitStreamSpec(StreamSpec spec){
		String ret = "";
        for (Iterator iter = spec.getFuncs().iterator(); iter.hasNext(); )
        {
            Function func = (Function)iter.next();
            ret += (String)func.accept(this);
        }
        return ret;
	}

	private void doFindSizeParams(Type t, List<String> list) {
		if(t instanceof TypeArray) {
			VarCollector vc=new VarCollector(list);
			for(Expression e :((TypeArray)t).getDimensions()) {
				e.accept(vc);
			}
		}
	}
	protected List<String> getSizeParams(Function f) {
		List<String> ret=new ArrayList<String>();
		doFindSizeParams(f.getReturnType(), ret);
		for(Iterator it=f.getParams().iterator();it.hasNext();) {
			Parameter p=(Parameter)it.next();
			if(p.isParameterOutput()) continue;
			doFindSizeParams(p.getType(),ret);
		}
		return ret;
	}

	protected String getReturnVar(Function f) {
		ReturnFinder rf=new ReturnFinder();
		f.getBody().accept(rf);
		String ret=rf.getReturnVarName();
		if(ret!=null) return ret;
		return getDefaultReturnVar();
	}

	private String makeParamCSL(List<Parameter> list) {
		String ret="";
		boolean first=true;
		for(Parameter p:list) {
			if(first) first=false; else ret+=",";
			ret+=p.getName();
		}
		return ret;
	}

	protected String makeDeclaration(Type type, String name) {

		String bounds="";
		String typestr;
		if(type instanceof TypeArray) {
			TypeArray ta=(TypeArray) type;
			typestr=getArrayDataType();
			for(Expression dim:ta.getDimensions()) {
				String expr=(String) dim.accept(this);
				if(bounds.length()>0) bounds+=", ";
				bounds+="0:"+expr+"-1";
			}
			bounds="("+bounds+")";
		} else {
			typestr=convertType(type);
		}
		return line(typestr+" "+name+bounds);
	}

	protected String declareParams(List<Parameter> list) {
		String ret="";
		for(Parameter p: list) {
			ret+=makeDeclaration(p.getType(), p.getName());
		}
		return ret;
	}

	public Object visitFunction(Function func)
    {
		curFunc=func;
		returnsArray=(func.getReturnType() instanceof TypeArray);
		isProcedure=returnsArray || (func.getReturnType()==TypePrimitive.voidtype);
		outvar=getReturnVar(func);
		String ret="";

		List<Parameter> params=new ArrayList<Parameter>();
		for(Iterator it=func.getParams().iterator();it.hasNext();) {
			Parameter p=(Parameter)it.next();
			varTypes.put(p.getName(), p.getType());
			//if(p.isParameterOutput()) continue;
			params.add(p);
		}
		if(returnsArray) {
			params.add(new Parameter(func.getReturnType(),outvar));
		}
		for(String sizeArg: getSizeParams(func)) {
			params.add(new Parameter(TypePrimitive.inttype,sizeArg));
		}
		String paramlist="("+makeParamCSL(params)+")";

		if(isProcedure) {
			ret+=line("subroutine "+func.getName()+paramlist);
		} else {
			String rettype=convertType(func.getReturnType());
			ret+=line(rettype+" function "+func.getName()+paramlist);
		}
		ret+=declareParams(params);
		ret+=func.getBody().accept(this);
		ret+=line("end");
		ret+=newline();
        return ret;
    }


	public Object visitStmtBlock(StmtBlock stmt) {
		String ret="";
	    for(Iterator it=stmt.getStmts().iterator();it.hasNext();) {
	    	Statement child=(Statement) it.next();
	    	ret+=child.accept(this);
	    }
	    return ret;
    }

    public Object visitStmtVarDecl(StmtVarDecl stmt)
    {
    	assert stmt.getNumVars()==1:"multiple variable declarations are not allowed "+stmt+" "+stmt.getContext();

    	Type type = stmt.getType(0);
        String name=stmt.getName(0);
        assert (stmt.getInit(0)==null):"declaration initializers are not allowed "+stmt+" "+stmt.getContext();
        varTypes.put(name, type);

        //don't declare the output variable again (it is an argument to the function)
        if(name.equals(outvar)) return "";

        return makeDeclaration(type, name);
    }

	public Object visitExprFunCall(ExprFunCall exp)
    {
		assert false: "function calls not supported yet"; //TODO
		return null;
    }


    public Object visitStmtLoop(StmtLoop stmt)
    {
		assert false: "loop construct not supported yet"; //TODO
		return null;
    }

    private ExprVar genLoopVar(int idx) {
    	return new ExprVar(null,"i_"+idx);
    }

    private String generateZeroCode(String name,TypeArray type) {
    	String ret="";
    	List<Expression> dims=type.getDimensions();
    	Expression arr=new ExprVar(null,name);
    	for(int i=0;i<dims.size();i++) {
    		arr=new ExprArrayRange(arr,genLoopVar(i));
    	}
    	Statement body=new StmtAssign(null,arr,ExprConstInt.zero);
    	for(int i=dims.size()-1;i>=0;i--) {
    		ExprVar loopVar=genLoopVar(i);
    		body=new StmtFor(null,
    			new StmtVarDecl(null, TypePrimitive.inttype, loopVar.getName(), ExprConstInt.zero),
    			new ExprBinary(null, ExprBinary.BINOP_LT, loopVar, dims.get(i)),
    			new StmtExpr(new ExprUnary(null, ExprUnary.UNOP_POSTINC, loopVar)),
    			body
    		);
    	}
    	return (String) body.accept(this);
    }

	public Object visitStmtAssign(StmtAssign stmt)
	{
		while(stmt.getLHS() instanceof ExprVar) {
			ExprVar var=(ExprVar) stmt.getLHS();
			Type varType=varTypes.get(var.getName());
			if(varType instanceof TypeArray) {
				if(stmt.getRHS() instanceof ExprConstInt) {
					ExprConstInt zero=(ExprConstInt) stmt.getRHS();
					assert zero.getVal()==0: "Can't assign arbitrary stuff to an array";
					return generateZeroCode(var.getName(),(TypeArray) varType);
				} else if(stmt.getRHS() instanceof ExprVar) {
					ExprVar rvar=(ExprVar) stmt.getRHS();
					Type rvarType=varTypes.get(rvar.getName());
					assert rvarType instanceof TypeArray: "Can't assign arbitrary stuff to an array";
					//TODO: implement array copy
					//for now just use the default handler which outputs a simple "a=b"
					break;
				} else {
					assert false: "Can't assign arbitrary stuff to an array";
				}
				return null;
			}
			break;
		}
		String lhs=(String)stmt.getLHS().accept(this);
		String rhs=(String)stmt.getRHS().accept(this);
		String op;
        switch(stmt.getOp())
        {
	        case 0:
	        	return line(lhs+" = "+rhs);
	        case ExprBinary.BINOP_ADD: op="+"; break;
	        case ExprBinary.BINOP_SUB: op="-"; break;
	        case ExprBinary.BINOP_MUL: op="*"; break;
	        case ExprBinary.BINOP_DIV: op="/"; break;

	        case ExprBinary.BINOP_BOR:
	        case ExprBinary.BINOP_BAND:
	        case ExprBinary.BINOP_BXOR:
	        	assert false;
	        	return null;
	        default:
	        	throw new IllegalStateException(stmt.toString()+" opcode="+stmt.getOp());
        }
        return line(lhs+" = "+lhs+" "+op+" "+rhs);
	}

	public Object visitStmtReturn(StmtReturn stmt) {
		String ret="";
		if(returnsArray) {
			assert stmt.getValue() instanceof ExprVar;
			ExprVar var=(ExprVar) stmt.getValue();
			if(var.getName().equals(outvar)) {
				//no need to do anything
			} else {
				//TODO: copy the return value into outvar
				assert false: "array copy not implemented yet";
			}
		} else if(isProcedure) {
			assert stmt.getValue()==null;
		} else {
			//function: set the return value (assign to function name)
			StmtAssign assign=new StmtAssign(null,new ExprVar(null,curFunc.getName()),stmt.getValue());
			ret+=assign.accept(this);
		}
		ret+=line("return");
		return ret;
	}

	public Object visitExprArrayRange(ExprArrayRange exp) {
		String ret="";
	    String base=(String) exp.getAbsoluteBase().accept(this);
	    for(Expression idx: exp.getArrayIndices()) {
	    	if(ret.length()>0) ret+=", ";
	    	ret+=idx.accept(this);
	    }
	    return base+"("+ret+")";
    }

	public Object visitExprBinary(ExprBinary exp) {
		String op;
		switch(exp.getOp()) {
	        case ExprBinary.BINOP_ADD: op = "+"; break;
	        case ExprBinary.BINOP_SUB: op = "-"; break;
	        case ExprBinary.BINOP_MUL: op = "*"; break;
	        case ExprBinary.BINOP_DIV: op = "/"; break;

	        case ExprBinary.BINOP_AND: op = ".AND."; break;
	        case ExprBinary.BINOP_OR:  op = ".OR."; break;

	        case ExprBinary.BINOP_EQ:  op = ".EQ."; break;
	        case ExprBinary.BINOP_NEQ: op = ".NE."; break;
	        case ExprBinary.BINOP_LT:  op = ".LT."; break;
	        case ExprBinary.BINOP_LE:  op = ".LE."; break;
	        case ExprBinary.BINOP_GT:  op = ".GT."; break;
	        case ExprBinary.BINOP_GE:  op = ".GE."; break;

	        //special conversions
	        case ExprBinary.BINOP_BAND: op = "*"; break;
	        case ExprBinary.BINOP_BXOR: op = "+"; break;

	        case ExprBinary.BINOP_MOD:
	        {
	    		String lhs=(String) exp.getLeft().accept(this);
	    		String rhs=(String) exp.getRight().accept(this);
	    	    return "mod("+lhs+","+rhs+")";
	        }

	        case ExprBinary.BINOP_BOR:
	        case ExprBinary.BINOP_RSHIFT:
	        case ExprBinary.BINOP_LSHIFT:
	        default:
	        	assert false: "unsupported binary operator in "+exp;
	        	return null;
		}
		String lhs=(String) exp.getLeft().accept(this);
		String rhs=(String) exp.getRight().accept(this);
	    return "("+lhs+" "+op+" "+rhs+")";
    }

	public Object visitStmtFor(StmtFor stmt) {
		assert stmt.getInit() instanceof StmtVarDecl;

		StmtVarDecl decl=(StmtVarDecl) stmt.getInit();
		assert decl.getNumVars()==1;

		String indexVar=decl.getName(0);
		Expression start=decl.getInit(0);
		assert start!=null;
		assert stmt.getCond() instanceof ExprBinary;

		ExprBinary cond=(ExprBinary) stmt.getCond();
		assert cond.getLeft() instanceof ExprVar;
		assert ((ExprVar)cond.getLeft()).getName().equals(indexVar);

		Expression finish=null;
		switch(cond.getOp()) {
			case ExprBinary.BINOP_LT:
				finish=new ExprBinary(null,ExprBinary.BINOP_SUB,cond.getRight(),new ExprConstInt(1));
				break;
			case ExprBinary.BINOP_LE:
				finish=cond.getRight();
				break;
			case ExprBinary.BINOP_GT:
				finish=new ExprBinary(null,ExprBinary.BINOP_ADD,cond.getRight(),new ExprConstInt(1));
				break;
			case ExprBinary.BINOP_GE:
				finish=cond.getRight();
				break;
			default:
				assert false:"unsupported for loop termination condition";
		}

		Expression stride=null;
		if(stmt.getIncr() instanceof StmtExpr) {
			StmtExpr se=(StmtExpr) stmt.getIncr();
			assert se.getExpression() instanceof ExprUnary;
			ExprUnary expr=(ExprUnary) se.getExpression();
			switch(expr.getOp()) {
				case ExprUnary.UNOP_POSTINC:
				case ExprUnary.UNOP_PREINC:
					stride=new ExprConstInt(1);
					break;
				case ExprUnary.UNOP_POSTDEC:
				case ExprUnary.UNOP_PREDEC:
					stride=new ExprConstInt(-1);
					break;
				default:
					assert false:"unsupported loop increment";
			}
		} else if(stmt.getIncr() instanceof StmtAssign) {
			StmtAssign assign=(StmtAssign) stmt.getIncr();
			assert assign.getLHS() instanceof ExprVar;
			assert ((ExprVar)assign.getLHS()).getName().equals(indexVar);
			switch(assign.getOp()) {
				case ExprBinary.BINOP_ADD:
					stride=assign.getRHS();
					break;
				case ExprBinary.BINOP_SUB:
					stride=new ExprUnary(null,ExprUnary.UNOP_NEG,assign.getRHS());
					break;
				default:
					assert false:"unsupported loop increment";
			}
		} else {
			assert false:"unsupported loop increment";
		}

		String startstr=(String) start.accept(this);
		String finishstr=(String) finish.accept(this);
		String stridestr=(String) stride.accept(this);
		String ret="do "+indexVar+" = "+startstr+", "+finishstr;
		if(!"1".equals(stridestr)) ret+=", "+stridestr;
		ret=line(ret);
		indent++;
		ret+=stmt.getBody().accept(this);
		indent--;
		ret+=line("enddo");
	    return ret;
    }

	public Object visitStmtIfThen(StmtIfThen stmt) {
		String ret="";
		String condstr=(String) stmt.getCond().accept(this);
		ret+=line("if ("+condstr+") then");
		indent++;
		ret+=stmt.getCons().accept(this);
		indent--;
		if(stmt.getAlt()!=null) {
			ret+=line("else");
			indent++;
			ret+=stmt.getAlt().accept(this);
			indent--;
		}
		ret+=line("endif");
	    return ret;
    }

	public Object visitExprUnary(ExprUnary exp)
	{
		switch(exp.getOp()) {
			case ExprUnary.UNOP_NOT:
				return ".NOT. "+exp.getExpr().accept(this);
			case ExprUnary.UNOP_NEG:
				return "(-"+exp.getExpr().accept(this)+")";
			default:
				assert false: "Unsupported unary operator";
		}
		return null;
	}

	public Object visitExprVar(ExprVar exp) {
	    return exp.getName();
    }

	public Object visitExprConstBoolean(ExprConstBoolean exp) {
	    assert false;
	    return null;
    }

	public Object visitExprConstChar(ExprConstChar exp) {
	    assert false;
	    return null;
    }

	public Object visitExprConstFloat(ExprConstFloat exp) {
	    return ""+exp.getVal();
    }

	public Object visitExprConstInt(ExprConstInt exp) {
	    return ""+exp.getVal();
    }

	public Object visitExprConstStr(ExprConstStr exp) {
	    throw new IllegalStateException();
    }

	public Object visitExprTernary(ExprTernary exp) {
	    assert false;
	    return null;
    }

	public Object visitExprLiteral(ExprLiteral exp) {
	    return exp.getValue();
    }

	public Object visitExprField(ExprField exp) {
	    throw new IllegalStateException();
    }


	public Object visitExprArrayInit(ExprArrayInit exp) {
	    assert false;
	    return null;
    }

	public Object visitExprComplex(ExprComplex exp) {
	    throw new IllegalStateException();
    }

	public Object visitExprPeek(ExprPeek exp) {
	    throw new IllegalStateException();
    }

	public Object visitExprPop(ExprPop exp) {
	    throw new IllegalStateException();
    }

	public Object visitExprStar(ExprStar star) {
	    throw new IllegalStateException();
    }

	public Object visitExprTypeCast(ExprTypeCast exp) {
	    assert false;
	    return null;
    }

	public Object visitFieldDecl(FieldDecl field) {
	    assert false;
	    return null;
    }

	public Object visitFuncWork(FuncWork func) {
	    throw new IllegalStateException();
    }

	public Object visitOther(FENode node) {
	    throw new IllegalStateException();
    }

	public Object visitSCAnon(SCAnon creator) {
	    throw new IllegalStateException();
    }

	public Object visitSCSimple(SCSimple creator) {
	    throw new IllegalStateException();
    }

	public Object visitSJDuplicate(SJDuplicate sj) {
	    throw new IllegalStateException();
    }

	public Object visitSJRoundRobin(SJRoundRobin sj) {
	    throw new IllegalStateException();
    }

	public Object visitSJWeightedRR(SJWeightedRR sj) {
	    throw new IllegalStateException();
    }

	public Object visitStmtAdd(StmtAdd stmt) {
	    throw new IllegalStateException();
    }

	public Object visitStmtAssert(StmtAssert stmt) {
		//TODO?
	    return "";
    }

	public Object visitStmtBody(StmtBody stmt) {
	    throw new IllegalStateException();
    }

	public Object visitStmtBreak(StmtBreak stmt) {
	    assert false;
	    return null;
    }

	public Object visitStmtContinue(StmtContinue stmt) {
	    assert false;
	    return null;
    }

	public Object visitStmtDoWhile(StmtDoWhile stmt) {
	    assert false;
	    return null;
    }

	public Object visitStmtEmpty(StmtEmpty stmt) {
	    return "";
    }

	public Object visitStmtEnqueue(StmtEnqueue stmt) {
	    throw new IllegalStateException();
    }

	public Object visitStmtExpr(StmtExpr stmt) {
	    assert false;
	    return null;
    }

	public Object visitStmtJoin(StmtJoin stmt) {
	    throw new IllegalStateException();
    }

	public Object visitStmtPhase(StmtPhase stmt) {
	    throw new IllegalStateException();
    }

	public Object visitStmtPush(StmtPush stmt) {
	    throw new IllegalStateException();
    }

	public Object visitStmtSendMessage(StmtSendMessage stmt) {
	    throw new IllegalStateException();
    }

	public Object visitStmtSplit(StmtSplit stmt) {
	    throw new IllegalStateException();
    }

	public Object visitStmtWhile(StmtWhile stmt) {
	    assert false;
	    return null;
    }

	public Object visitStreamType(StreamType type) {
	    throw new IllegalStateException();
    }

	public String convertType(Type type)
	{
		assert (type instanceof TypePrimitive);
		switch(((TypePrimitive)type).getType()) {
            case TypePrimitive.TYPE_BOOLEAN:
			case TypePrimitive.TYPE_INT: return "integer";
			case TypePrimitive.TYPE_BIT:   return "real";
			case TypePrimitive.TYPE_FLOAT: return "real";
		}
		assert false;
		return null;
	}
	public Object visitType(Type t) { return null; }
    public Object visitTypePrimitive(TypePrimitive t) { return null; }
    public Object visitTypeArray(TypeArray t) { return null; }
    public Object visitParameter(Parameter par){
    	assert false :"NYI";
    	return null;
    }

    public Object visitExprNew(ExprNew expNew){
    	assert false :"No alloc in fortran.";
    	return null;
    }
    public Object visitStmtPloop(StmtPloop loop){
		throw new UnsupportedOperationException();
	}

    public Object visitStmtAnyOrderBlock(StmtAnyOrderBlock block){throw new UnsupportedOperationException();}
    public Object visitStmtAtomicBlock(StmtAtomicBlock block){throw new UnsupportedOperationException();}
    public Object visitTypeStruct(TypeStruct ts){throw new UnsupportedOperationException();}
    public Object visitExprNullPtr(ExprNullPtr nptr){ throw new UnsupportedOperationException(); }
}
