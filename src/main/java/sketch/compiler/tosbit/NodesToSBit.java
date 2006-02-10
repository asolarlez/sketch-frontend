/*
 * Created on Jun 22, 2004
 *
 * 
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package streamit.frontend.tosbit;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import streamit.frontend.nodes.ExprBinary;
import streamit.frontend.nodes.ExprConstInt;
import streamit.frontend.nodes.ExprFunCall;
import streamit.frontend.nodes.ExprStar;
import streamit.frontend.nodes.ExprVar;
import streamit.frontend.nodes.Expression;
import streamit.frontend.nodes.FENode;
import streamit.frontend.nodes.FieldDecl;
import streamit.frontend.nodes.FuncWork;
import streamit.frontend.nodes.Function;
import streamit.frontend.nodes.Parameter;
import streamit.frontend.nodes.Program;
import streamit.frontend.nodes.SCAnon;
import streamit.frontend.nodes.SCSimple;
import streamit.frontend.nodes.SJDuplicate;
import streamit.frontend.nodes.SJRoundRobin;
import streamit.frontend.nodes.SJWeightedRR;
import streamit.frontend.nodes.Statement;
import streamit.frontend.nodes.StmtAdd;
import streamit.frontend.nodes.StmtAssign;
import streamit.frontend.nodes.StmtBlock;
import streamit.frontend.nodes.StmtBody;
import streamit.frontend.nodes.StmtBreak;
import streamit.frontend.nodes.StmtContinue;
import streamit.frontend.nodes.StmtDoWhile;
import streamit.frontend.nodes.StmtEmpty;
import streamit.frontend.nodes.StmtEnqueue;
import streamit.frontend.nodes.StmtExpr;
import streamit.frontend.nodes.StmtFor;
import streamit.frontend.nodes.StmtIfThen;
import streamit.frontend.nodes.StmtJoin;
import streamit.frontend.nodes.StmtLoop;
import streamit.frontend.nodes.StmtPhase;
import streamit.frontend.nodes.StmtPush;
import streamit.frontend.nodes.StmtReturn;
import streamit.frontend.nodes.StmtSendMessage;
import streamit.frontend.nodes.StmtSplit;
import streamit.frontend.nodes.StmtVarDecl;
import streamit.frontend.nodes.StmtWhile;
import streamit.frontend.nodes.StreamCreator;
import streamit.frontend.nodes.StreamSpec;
import streamit.frontend.nodes.StreamType;
import streamit.frontend.nodes.TempVarGen;
import streamit.frontend.nodes.Type;
import streamit.frontend.nodes.TypeArray;
import streamit.frontend.nodes.TypePrimitive;
import streamit.frontend.nodes.TypeStruct;
import streamit.frontend.tojava.ExprJavaConstructor;
import streamit.frontend.tojava.StmtAddPhase;
import streamit.frontend.tojava.StmtIODecl;
import streamit.frontend.tojava.StmtSetTypes;


/**
 * The purpose of this class is to generate streamBit friendly input from a filter description.
 * @author asolar
 *
 *  
 * 
 */

public class NodesToSBit extends PartialEvaluator{
    // A string consisting of an even number of spaces.    
    private TempVarGen varGen;
    //private boolean isLHS;
    private NodesToNative nativeGenerator;
    private HashMap<String, StreamSpec> funsWParams;
    protected Stack<String> preFil;
    protected List<Statement> additInit;
    private ValueOracle oracle;
    public int LUNROLL=8;
    private LoopMap loopmap= new LoopMap();
	
    
    
	    public NodesToSBit(StreamSpec ss, TempVarGen varGen, ValueOracle oracle)
	    {
	    	super(false);
	        this.ss = ss;	        
	        this.varGen = varGen;	         
	        this.state = new MethodState();
	        this.oracle = oracle;
	        funsWParams = new HashMap<String, StreamSpec>();
	        preFil = new Stack<String>();
	        nativeGenerator = new NodesToNative(ss, varGen, state, this);	        
	    }
	    public String finalizeWork(){
	    	String result = "";	    	
	    	int noutputs = state.varValue("PUSH_POS");
	    	for(int i=0; i< noutputs; ++i){
	    		String nm = "OUTPUT_" + i;
	    		result += nm + " = ";	    		
	    		result += state.varGetRHSName(nm) + "; \n";
	    		state.unsetVarValue(nm);
	    	}	    	
	    	return result;
	    }
	    
	    public void initializeWork(){	    
	        state.varDeclare("POP_POS");
	    	state.varDeclare("PUSH_POS");
	    	
	    	state.varGetLHSName("POP_POS");
	        state.varGetLHSName("PUSH_POS");
	    	
	        state.setVarValue("POP_POS", 0 );
	        state.setVarValue("PUSH_POS", 0 );
	    }
	    // Add two spaces to the indent.	    

	    // Do the same conversion, but including array dimensions.
	    public String convertTypeFull(Type type)
	    {
	        if (type instanceof TypeArray)
	        {
	            TypeArray array = (TypeArray)type;
	            array.getLength().accept(this);
	            int i = state.popVStack().getIntValue();	            
	            return convertTypeFull(array.getBase()) + "[" + i + "]";
	        }
	        return convertType(type);
	    }

	    // Get a constructor for some type.
	    public String makeConstructor(Type type)
	    {
	        if (type instanceof TypeArray)
	            return "new " + convertTypeFull(type);
	        else
	            return "new " + convertTypeFull(type) + "()";
	    }

	    // Get a Java Class object corresponding to a type.
	    public String typeToClass(Type t)
	    {
	        if (t instanceof TypePrimitive)
	        {
	            switch (((TypePrimitive)t).getType())
	            {
	            case TypePrimitive.TYPE_BOOLEAN:
	                return "Boolean.TYPE";
	            case TypePrimitive.TYPE_BIT:
	                return "Integer.TYPE";
	            case TypePrimitive.TYPE_INT:
	                return "Integer.TYPE";
	            case TypePrimitive.TYPE_FLOAT:
	                return "Float.TYPE";
	            case TypePrimitive.TYPE_DOUBLE:
	                return "Double.TYPE";
	            case TypePrimitive.TYPE_VOID:
	                return "Void.TYPE";
	            case TypePrimitive.TYPE_COMPLEX:
	                return "Complex.class";
	            }
	        }
	        else if (t instanceof TypeStruct)
	            return ((TypeStruct)t).getName() + ".class";
	        else if (t instanceof TypeArray)
	            return "(" + makeConstructor(t) + ").getClass()";
	        // Errp.
	        System.err.println("typeToClass(): I don't understand " + t);
	        return null;
	    }

	    // Helpers to get function names for stream types.
	    public  String pushFunction(StreamType st)
	    {
	        return annotatedFunction("output.push", st.getOut());
	    }
	    
	    public  String popFunction(StreamType st)
	    {
	        return annotatedFunction("input.pop", st.getIn());
	    }
	    
	    public  String peekFunction(StreamType st)
	    {
	        return annotatedFunction("input.peek", st.getIn());
	    }
	    
	    private  String annotatedFunction(String name, Type type)
	    {
	        String prefix = "", suffix = "";
	        // Check for known suffixes:
	        if (type instanceof TypePrimitive)
	        {
	            switch (((TypePrimitive)type).getType())
	            {
	            case TypePrimitive.TYPE_BOOLEAN:
	                suffix = "Boolean";
	                break;
	            case TypePrimitive.TYPE_BIT:
	                suffix = "Int";
	                break;
	            case TypePrimitive.TYPE_INT:
	                suffix = "Int";
	                break;
	            case TypePrimitive.TYPE_FLOAT:
	                suffix = "Float";
	                break;
	            case TypePrimitive.TYPE_DOUBLE:
	                suffix = "Double";
	                break;
	            case TypePrimitive.TYPE_COMPLEX:
	                if (name.startsWith("input"))
	                    prefix  = "(Complex)";
	                break;
	            }
	        }
	        else if (name.startsWith("input"))
	        {
	            prefix = "(" + convertType(type) + ")";
	        }
	        return prefix + name + suffix;
	    }

	    
	    public String postDoParams(List params){
	    	String result = "";	        
	        for (Iterator iter = params.iterator(); iter.hasNext(); )
	        {
	            Parameter param = (Parameter)iter.next();
	            if(param.isParameterOutput()){
		            if( param.getType() instanceof TypeArray ){
		            	String lhs = param.getName();
		            	TypeArray ta = (TypeArray) param.getType();
		            	ta.getLength().accept(this);
		            	valueClass tmp = state.popVStack();
		            	Assert(tmp.hasValue(), "The array size must be a compile time constant !! \n" );		            	
		            	for(int tt=0; tt<tmp.getIntValue(); ++tt){
		            		String nnm = lhs + "_idx_" + tt;		            		
			            	result += state.varGetRHSName("_p_"+nnm) + " = " + state.varGetRHSName(nnm) + ";\n";
			            	//NOTE: This is not a bug. I call getRHSName for _p_nnm even though it is in the LHS
			            	//because we need to get the same value that we got when we declared it.
		            	}
		            }else{
		            	String lhs = param.getName();
		            	result += state.varGetRHSName("_p_"+lhs) + " = " + state.varGetRHSName(lhs) + ";\n";
		            }
	            }
	        }
	        return result;
	    }
	    
	    public Object visitExprBinary(ExprBinary exp)
	    {
	    	if(exp.getOp() == ExprBinary.BINOP_SELECT ){	
	    		Expression rvalE = exp;
		        Expression left = (Expression)exp.getLeft().accept(this);
		        valueClass lhs = state.popVStack();	        
		        
		        Expression right = (Expression) exp.getRight().accept(this);
		        valueClass rhs = state.popVStack();
		        
		        boolean hasv = lhs.hasValue() && rhs.hasValue();		        
	        	if(hasv && lhs.getIntValue() == rhs.getIntValue()){
	        		int newv=0;	        		
	        		newv = lhs.getIntValue();
		        	state.pushVStack(new valueClass(newv));			        	
		        	if( this.isReplacer ){
		        		rvalE = new ExprConstInt(newv);
		        	}
			        return rvalE;
	        	}else{
	        		hasv = false;
	        		String rval = null;
	        		String cvar = state.varDeclare();	        		
	        		oracle.addBinding(exp.getAlias(), cvar);
	        		if(lhs.hasValue() && rhs.hasValue()){
	        			if(lhs.getIntValue() == 1){
	        				rval =  "<" + cvar + ">";
	        			}else{
	        				rval =  "! <" + cvar + ">";
	        			}
	        		}else{
	        			rval =  "( <" + cvar +"> ? " + lhs + " : " + rhs + ")";
	        		}
	        		state.pushVStack(new valueClass(rval));	        		
	        		if(this.isReplacer && (left != exp.getLeft() || right != exp.getRight())){
	        			rvalE = new ExprBinary(exp.getContext(), exp.getOp(), left, right, exp.getAlias());
	        		}  	        		
	        		return rvalE;
	        	}
	    	}else{
	    		return super.visitExprBinary(exp);
	    	}	        
	    }

	    


	    public Object visitFieldDecl(FieldDecl field)
	    {
	        // Assume all of the fields have the same type.
	    	//Assert(false, "We don't allow filters to have state! Sorry.");
	    	//return "//We don't allow filters to have state. \n";
	    	
	        String result =  convertType(field.getType(0)) + " ";
	        for (int i = 0; i < field.getNumFields(); ++i)
	        {
	            if (i > 0) result += ", ";
	            
	            String lhs = field.getName(i);
	            state.varDeclare(lhs);
	            state.varGetLHSName(lhs);
	            if( field.getType(i) instanceof TypeArray ){
	            	TypeArray ta = (TypeArray) field.getType(i);
	            	ta.getLength().accept(this);
	            	valueClass tmp = state.popVStack();
	            	Assert(tmp.hasValue(), "The array size must be a compile time constant !! \n" + field.getContext());
	            	state.makeArray(lhs, tmp.getIntValue());
	            	for(int tt=0; tt<tmp.getIntValue(); ++tt){
	            		String nnm = lhs + "_idx_" + tt;
	            		state.varDeclare(nnm);
	            		state.varGetLHSName(nnm);
	            	}
	            }
	         
	            if (field.getInit(i) != null){	
	            	additInit.
					add(new StmtAssign(field.getContext(),
							new ExprVar(field.getContext(), lhs),
							field.getInit(i)));   
	            }else{	            	
	            	//Assert(false, "Vars should be initialized");
	            }
	        }
	        result += ";";
	        result = "";
	        if (field.getContext() != null)
	            result += " // " + field.getContext();
	        result += "\n";
	        return result;	        
	    }


	    public Object visitFunction(Function func)
	    {
	        String result ="";
	        if (!func.getName().equals(ss.getName()));
	        
	        if(func.getCls() == Function.FUNC_INIT){
	        	result += "INIT()";
	        	//result += doParams(func.getParams(), "") + "\n";
	        	result += "{\n";
	        	this.state.pushLevel();       		        	
	        	result += (String)func.getBody().accept(this);
	        	this.state.popLevel();
	        	Iterator it = this.additInit.iterator();
	        	while(it.hasNext()){
	        		Statement st = (Statement) it.next();
	        		st.accept(this);
	        	}
	        	this.additInit.clear();
	        	result += "}\n";	        	
	        }else if(func.getCls() == Function.FUNC_WORK){
	        	result += "WORK()\n";
	        	Assert( func.getParams().size() == 0 , "");	        	
	        	result += "{\n";
	        	this.state.pushLevel();
	        	initializeWork();
	        	Expression pushr = ((FuncWork)func).getPopRate();
	        	Expression popr = ((FuncWork)func).getPushRate();
	        	if(pushr != null){
	        		pushr.accept(this);
	        		result += "input_RATE = " + state.popVStack() + ";\n";
	        	}else{
	        		result += "input_RATE = 0;\n";
	        	}
	        	if(popr != null){
	        		popr.accept(this);
	        		result += "output_RATE = " + state.popVStack() + ";\n";
	        	}else{
	        		result += "output_RATE = 0;\n";	        	
	        	}	        	
	        	Assert(((StmtBlock)func.getBody()).getStmts().size()>0, "You can not have empty functions!\n" + func.getContext() );
	        	result += (String)func.getBody().accept(this);
	        	result += finalizeWork();
	        	this.state.popLevel();
	        	result += "}\n";
	        }else{
	        	result += func.getName();
	        	if( func.getSpecification() != null ){
	        		result += " SKETCHES " + func.getSpecification(); 
	        	}
	        	result += doParams(func.getParams(), "") + "\n";
	        	result += "{\n";
	        	this.state.pushLevel();       		        	
	        	result += (String)func.getBody().accept(this);
	        	this.state.popLevel();
	        	result += postDoParams(func.getParams());
	        	Iterator it = this.additInit.iterator();
	        	while(it.hasNext()){
	        		Statement st = (Statement) it.next();
	        		st.accept(this);
	        	}
	        	this.additInit.clear();
	        	result += "}\n";	
	        	//state.pushVStack(new valueClass((String)null) );
	        }
	        
	        result += "\n";
	        return result;
	    }
	    
	    public Object visitFuncWork(FuncWork func)
	    {
	        // Nothing special here; we get to ignore the I/O rates.
	        return visitFunction(func);
	    }

	    public Object visitProgram(Program prog)
	    {
	        // Nothing special here either.  Just accumulate all of the
	        // structures and streams.
	        String result = "";
	        for (Iterator iter = prog.getStreams().iterator(); iter.hasNext(); ){
	        	StreamSpec sp = (StreamSpec)iter.next();	        	
	        	funsWParams.put(sp.getName(), sp);	        		        	
	        }
	        for (Iterator iter = prog.getStreams().iterator(); iter.hasNext(); ){
	        	StreamSpec sp = (StreamSpec)iter.next();
	        	Function finit = sp.getFuncNamed("init");
	        	if(finit == null){
	        		String code= (String)sp.accept(this);	        		
	        		result += code;
	        	}else{
		        	if(finit.getParams().size() > 0){
		        		//funsWParams.put(sp.getName(), sp);
		        	}else{	        		
		        		String code= (String)sp.accept(this);	        		
		        		result += code;
		        		
		        	}	
	        	}
	        }
	        
	        while( preFil.size() > 0){
	        	String otherFil = (String) preFil.pop();
	        	result = otherFil + result;
	        }
	        
	        return result;
	    }

	    public Object visitSCAnon(SCAnon creator)
	    {
	        return creator.getSpec().accept(this);
	    }
	    
	    public Object visitSCSimple(SCSimple creator)
	    {
	    	String result;
	    	if( creator.getParams().size() == 0){
		        result = "new " + creator.getName() + "(";
		        boolean first = true;
		        for (Iterator iter = creator.getParams().iterator(); iter.hasNext(); )
		        {
		            Expression param = (Expression)iter.next();
		            if (!first) result += ", ";
		            result += (String)param.accept(this);
		            first = false;
		        }
		        /*
		        for (Iterator iter = creator.getTypes().iterator(); iter.hasNext(); )
		        {
		            Type type = (Type)iter.next();
		            if (!first) result += ", ";
		            result += typeToClass(type);
		            first = false;
		        }*/
		        result += ")";
	    	}else{
	    		String nm = creator.getName();
	    		result = "new "   ;
	    		String fullnm = nm;
	    		
		        for (Iterator iter = creator.getParams().iterator(); iter.hasNext(); )
		        {
		        	
		            Expression param = (Expression)iter.next();
		            fullnm += "_";
		            //this.state.markVectorStack();
		            param.accept(this);	
		            valueClass paramVal = state.popVStack();
                	if(paramVal.isVect()){
                		List<valueClass> l = paramVal.getVectValue();
                		int xx=0;
                		int xpon=1;                		                		
                		for(Iterator it = l.iterator(); it.hasNext(); ){
                			Integer i = (Integer)it.next();
                			xx += xpon *  i.intValue();
                			xpon = xpon * 3;
                		}
                		fullnm +=  xx;
                	}else{
                		int pv = paramVal.getIntValue();                		
                		fullnm += "" + pv;
                	}
		        }	    				        
		        fullnm += "___";
		        result += fullnm + "()";
		        if( funsWParams.get(fullnm) == null){
		        	
		        	
		        	
			        StreamSpec sp = (StreamSpec) funsWParams.get(nm);
			        funsWParams.put(fullnm, sp);
			        Assert( sp != null, nm + "Is used but has not been declared!!");
			        state.pushLevel();
			        Function finit = sp.getFuncNamed("init");
			        Iterator formalParamIterator = finit.getParams().iterator();
			        Iterator actualParamIterator = creator.getParams().iterator();
			        Assert(finit.getParams().size() == creator.getParams().size() , nm + " The number of formal parameters doesn't match the number of actual parameters!!");
			        
			        
			        inParameterSetter(formalParamIterator,actualParamIterator, true);
			        
			        String tmp = (String) preFil.pop();
			        tmp += sp.accept(this);
			        preFil.push(tmp);
			        state.popLevel();
		        }
	    	}
	        return result;
	    }

        
	    public Object visitSJDuplicate(SJDuplicate sj)
	    {
	    	switch(sj.getType()){
	    		case SJDuplicate.DUP:
	    			return "DUPLICATE()";
	    		case SJDuplicate.XOR:
	    			return "XOR()";
	    		case SJDuplicate.OR:
	    			return "OR()";
	    		case SJDuplicate.AND:
	    			return "AND()";
	    		default:
	    			return "DUPLICATE()";
	    	}
	    }

	    public Object visitSJRoundRobin(SJRoundRobin sj)
	    {
	        return "ROUND_ROBIN(" + (String)sj.getWeight().accept(this) + ")";
	    }

	    public Object visitSJWeightedRR(SJWeightedRR sj)
	    {
	    	String result = "ROUND_ROBIN(";
	        boolean first = true;
	        for (Iterator iter = sj.getWeights().iterator(); iter.hasNext(); )
	        {
	            Expression weight = (Expression)iter.next();
	            if (!first) result += ", ";
	            result += (String)weight.accept(this);
	            first = false;
	        }
	        result += ")";
	        return result;	        
	    }

	    public Object doStreamCreator(String how, StreamCreator sc)
	    {	    	
	        // If the stream creator involves registering with a portal,
	        // we need a temporary variable.
	        List portals = sc.getPortals();
	        if (portals.isEmpty()){	        	
	        	if( sc instanceof SCSimple){
	        		String nm = ((SCSimple)sc).getName();
	        		if( nm.equals("IntToBit") || nm.equals("BitToInt")){	        			
	        			return "";
	        		}
	        	} 	
	            return how + "(" + (String)sc.accept(this) + ")";
	        }
	        String tempVar = varGen.nextVar();
	        // Need run-time type of the creator.  Assert that only
	        // named streams can be added to portals.
	        SCSimple scsimple = (SCSimple)sc;
	        String result = scsimple.getName() + " " + tempVar + " = " +
	            (String)sc.accept(this);
	        result += ";\n" + how + "(" + tempVar + ")";
	        for (Iterator iter = portals.iterator(); iter.hasNext(); )
	        {
	            Expression portal = (Expression)iter.next();
	            result += ";\n" + (String)portal.accept(this) +
	                ".regReceiver(" + tempVar + ")";
	        }
	        return result;
	    }
	    public Object visitStmtEmpty(StmtEmpty stmt)
	    {
	        return "";
	    }
	    
	    public Object visitStmtAdd(StmtAdd stmt)
	    {
	        return doStreamCreator("add", stmt.getCreator());
	    }
	    
	    public Object visitStmtAssign(StmtAssign stmt)
	    {
	    	
	        String op;
	        //this.state.markVectorStack();	        
	        stmt.getRHS().accept(this);
	        valueClass vrhsVal = state.popVStack();
	        List<valueClass> rhsLst = null;
	        	
	        if( vrhsVal.isVect() ){
	        	rhsLst= vrhsVal.getVectValue();
	        }
	        
	        LHSvisitor lhsvisitor = new LHSvisitor();
	        String lhs = (String)stmt.getLHS().accept( lhsvisitor );
	        
	        stmt.getLHS(); 
			valueClass vlhsVal = null;
			if(stmt.getOp() != 0){
				stmt.getLHS().accept(this);
				vlhsVal = state.popVStack();
			}
				        
	        
	        String lhsnm = state.varGetLHSName(lhs);
	        
	        int arrSize = state.checkArray(lhs);
	        boolean isArr = arrSize > 0;	        
	        
	        boolean hv = (vlhsVal == null || vlhsVal.hasValue()) && vrhsVal.hasValue() && !lhsvisitor.isNDArracc();
	        
	        switch(stmt.getOp())
	        {
	        case ExprBinary.BINOP_ADD: 	        	
	        	op = " = " + vlhsVal + "+";
	        	assert !isArr : "Operation not yet defined for arrays:" + op;
	        	if(hv){
	        		state.setVarValue(lhs, vlhsVal.getIntValue() + vrhsVal.getIntValue());
	        	}
	        break;
	        case ExprBinary.BINOP_SUB: 
	        	op = " = "+ vlhsVal + "-";
	        	assert !isArr : "Operation not yet defined for arrays:" + op;
		        if(hv){
	        		state.setVarValue(lhs, vlhsVal.getIntValue() - vrhsVal.getIntValue());
	        	}
	        break;
	        case ExprBinary.BINOP_MUL:
	        	op = " = "+ vlhsVal + "*";
	        	assert !isArr : "Operation not yet defined for arrays:" + op;
		        if(hv){
	        		state.setVarValue(lhs, vlhsVal.getIntValue() * vrhsVal.getIntValue());
	        	}
	        break;
	        case ExprBinary.BINOP_DIV:
	        	op = " = "+ vlhsVal + "/";
	        	assert !isArr : "Operation not yet defined for arrays:" + op;
		        if(hv){
	        		state.setVarValue(lhs, vlhsVal.getIntValue() / vrhsVal.getIntValue());
	        	}
	        break;
	        default: op = " = ";
		        if( rhsLst != null ){
		        	assert isArr: "This should not happen, you are trying to assign an array to a non-array";
		    		List<valueClass>  lst= rhsLst;
		    		Iterator<valueClass>  it = lst.iterator();
		    		int idx = 0;
		    		while( it.hasNext() ){
		    			int i = it.next().getIntValue();
		    			state.setVarValue(lhs + "_idx_" + idx, i);
		    			++idx;
		    		}
		    		return "";
		    	}else if(hv){
	        		state.setVarValue(lhs, vrhsVal.getIntValue());	
	        		return "";
	        	}
	        }
	        // Assume both sides are the right type.
	        if(hv) 
	        	return "";
	        else{
	        	if(lhsvisitor.isNDArracc()){
	        		lhsnm = lhsvisitor.getLHSString();
	        		lhsvisitor.unset();
	        	}else{
	        		state.unsetVarValue(lhs);
	        	}
	        }
	        return lhsnm + op + vrhsVal;
	    }

	    public Object visitStmtBlock(StmtBlock stmt)
	    {
	        // Put context label at the start of the block, too.
	    	state.pushLevel();
	        String result = "// {";
	        if (stmt.getContext() != null)
	            result += " \t\t\t// " + stmt.getContext();
	        result += "\n";
	        for (Iterator iter = stmt.getStmts().iterator(); iter.hasNext(); )
	        {
	            Statement s = (Statement)iter.next();
	            String line = " ";
	            line += (String)s.accept(this);
	            if(line.length() > 0){
		            if (!(s instanceof StmtIfThen)) {
		            	line += ";";
		            }
		            if (s.getContext() != null)
		                line += " \t\t\t// " + s.getContext();
		            line += "\n";
	            }
	            result += line;
	        }
	        result += " // }\n";
	        state.popLevel();
	        return result;
	    }

	    public Object visitStmtBody(StmtBody stmt)
	    {
	        return doStreamCreator("setBody", stmt.getCreator());
	    }
	    
	    public Object visitStmtBreak(StmtBreak stmt)
	    {
	        return "break";
	    }
	    
	    public Object visitStmtContinue(StmtContinue stmt)
	    {
	        return "continue";
	    }

	    public Object visitStmtDoWhile(StmtDoWhile stmt)
	    {
	    	Assert(false, "NYS");
	        String result = "do ";
	        result += (String)stmt.getBody().accept(this);
	        result += "while (" + (String)stmt.getCond().accept(this) + ")";
	        return result;
	    }

	    public Object visitStmtEnqueue(StmtEnqueue stmt)
	    {
	        // Errk: this doesn't become nice Java code.
	        return "/* enqueue(" + (String)stmt.getValue().accept(this) +
	            ") */";
	    }
	    
	    public Object visitStmtExpr(StmtExpr stmt)
	    {
	    	Expression exp = stmt.getExpression();
	    	exp.accept(this);
	    	String result = state.popVStack().toString();
		    return result;	    	
	    }

	    public Object visitStmtFor(StmtFor stmt)
	    {
	    	state.pushLevel();
	        loopmap.pushLoop(0);
	        String result = "";
	        if (stmt.getInit() != null)
	            stmt.getInit().accept(this);
	        
	        Assert( stmt.getCond() != null , "For now, the condition in your for loop can't be null");
	        stmt.getCond().accept(this);
	        valueClass vcond = state.popVStack();
	        int iters = 0;
	        while(vcond.hasValue() && vcond.getIntValue() > 0){
	        	++iters;
	        	result += (String)stmt.getBody().accept(this);	 
	        	loopmap.nextIter();
	        	if (stmt.getIncr() != null)
		        	stmt.getIncr().accept(this);
	        	stmt.getCond().accept(this);
		        vcond = state.popVStack();
		        Assert(iters <= (1<<13), "This is probably a bug, why would it go around so many times? ");
	        }
	        loopmap.popLoop();
	        state.popLevel();
	        return result;
	    }

	    public Object visitStmtIfThen(StmtIfThen stmt)
	    {
	        // must have an if part...
	    	
	        String result = "";
	        stmt.getCond().accept(this);
	        valueClass vcond = state.popVStack();
	        if(vcond.hasValue()){
	        	if(vcond.getIntValue() > 0){
	        		result = (String)stmt.getCons().accept(this);	        		
	        	}else{
	        		if (stmt.getAlt() != null)
	    	            result = (String)stmt.getAlt().accept(this);
	        	}
	        	return result;   	
	        }
	        state.pushChangeTracker();
	        String ipart = (String)stmt.getCons().accept(this);
	        ChangeStack ipms = state.popChangeTracker();
	        ChangeStack epms = null;
	        String epart="";
	        
	        if (stmt.getAlt() != null){
	        	state.pushChangeTracker();
	            epart = (String)stmt.getAlt().accept(this);
	            epms = state.popChangeTracker();
	        }	        
	        if(epms != null){
	        	result = ipart;
	        	result += epart;
	        	//I thought this would be more efficient, but it wasn't
	        	//String tmpVar = varGen.nextVar();
	        	//result += tmpVar + " = " + vcond.toString() + "; \n";
	        	result += state.procChangeTrackers(ipms, epms, vcond.toString());
	        }else{
	        	result = ipart;
	        	//String tmpVar = varGen.nextVar();
	        	//result += tmpVar + " = " + vcond.toString() + "; \n";
	        	result += state.procChangeTrackers(ipms, vcond.toString());
	        }
	        return result;
	    }

	    public Object visitStmtJoin(StmtJoin stmt)
	    {
	        return "setJoiner(" + (String)stmt.getJoiner().accept(this) + ")";
	    }
	    
	    public Object visitStmtLoop(StmtLoop stmt)
	    {

	    	String result = "";
	    	stmt.getIter().accept(this);
	    	valueClass vcond = state.popVStack();
	    	if(!vcond.hasValue()){
	    		String nvar = state.varDeclare();
	    		result += nvar + " = " + "(" + vcond + ");\n"; 	    		
	    		int iters;
	    		loopmap.pushLoop(LUNROLL);
	    		for(iters=0; iters<LUNROLL; ++iters){			        		        
			        state.pushChangeTracker();
			        String ipart = "";
			        try{			        	
			        	ipart = (String)stmt.getBody().accept(this);
			        }catch(ArrayIndexOutOfBoundsException er){			        	
			        	state.popChangeTracker();
			        	break;
		    		}
			        loopmap.nextIter();
			        result += ipart;
	    		}
	    		
	    		for(int i=iters-1; i>=0; --i){

	    			String cond = "(" + nvar + ")>" + i;
	    			// I thought this would be more efficient, but it wasn't.
//	    			String tmpVar = varGen.nextVar();
	    			//result += tmpVar + " = " + cond + "; \n";		        	
	    			
	    			ChangeStack ipms = state.popChangeTracker();
	    			result += state.procChangeTrackers(ipms, cond);
	    		}
	    		loopmap.popLoop();
		        return result;
	    	}else{
	    		loopmap.pushLoop(vcond.getIntValue());
	    		for(int i=0; i<vcond.getIntValue(); ++i){
	    			result += (String)stmt.getBody().accept(this);
	    			loopmap.nextIter();
	    		}
	    		loopmap.popLoop();
	    	}
	    	return result;
	    }

	    public Object visitStmtPhase(StmtPhase stmt)
	    {
	        ExprFunCall fc = stmt.getFunCall();
	        // ASSERT: the target is always a phase function.
	        FuncWork target = (FuncWork)ss.getFuncNamed(fc.getName());
	        StmtExpr call = new StmtExpr(stmt.getContext(), fc);
	        String peek, pop, push;
	        if (target.getPeekRate() == null)
	            peek = "0";
	        else
	            peek = (String)target.getPeekRate().accept(this);
	        if (target.getPopRate() == null)
	            pop = "0";
	        else
	            pop = (String)target.getPopRate().accept(this);
	        if (target.getPushRate() == null)
	            push = "0";
	        else
	            push = (String)target.getPushRate().accept(this);
	        
	        return "phase(new WorkFunction(" + peek + "," + pop + "," + push +
	            ") { public void work() { " + call.accept(this) + "; } })";
	    }

	    public Object visitStmtPush(StmtPush stmt)
	    {
	    	String val = (String)stmt.getValue().accept(this);
	    	valueClass ival = state.popVStack();
	    	 
	    	int pos = state.varValue("PUSH_POS");
	    	state.setVarValue("PUSH_POS", pos+1);
	    	String otpt = "OUTPUT_" + pos;	    	
			String lhs =  state.varGetLHSName(otpt);
	    	if(ival.hasValue()){
	    		state.setVarValue(otpt, ival.getIntValue());
	    		return "";
	    		//return lhs + " = " + ival;
	    	}else{
	    		return lhs + " = " + val;
	    	}	    	
	        //return pushFunction(ss.getStreamType()) + "(" +
	        //    (String)stmt.getValue().accept(this) + ")";
	    }

	    public Object visitStmtReturn(StmtReturn stmt)
	    {
	        if (stmt.getValue() == null) return "return";
	        return "return " + (String)stmt.getValue().accept(this);
	    }

	    public Object visitStmtSendMessage(StmtSendMessage stmt)
	    {
	        String receiver = (String)stmt.getReceiver().accept(this);
	        String result = "";

	        // Issue one of the latency-setting statements.
	        if (stmt.getMinLatency() == null)
	        {
	            if (stmt.getMaxLatency() == null)
	                result += receiver + ".setAnyLatency()";
	            else
	                result += receiver + ".setMaxLatency(" +
	                    (String)stmt.getMaxLatency().accept(this) + ")";
	        }
	        else
	        {
	            // Hmm, don't have an SIRLatency for only minimum latency.
	            // Wing it.
	            Expression max = stmt.getMaxLatency();
	            if (max == null)
	                max = new ExprBinary(null, ExprBinary.BINOP_MUL,
	                                     stmt.getMinLatency(),
	                                     new ExprConstInt(null, 100));
	            result += receiver + ".setLatency(" +
	                (String)stmt.getMinLatency().accept(this) + ", " +
	                (String)max.accept(this) + ")";
	        }
	        
	        result += ";\n" + receiver + "." + stmt.getName() + "(";
	        boolean first = true;
	        for (Iterator iter = stmt.getParams().iterator(); iter.hasNext(); )
	        {
	            Expression param = (Expression)iter.next();
	            if (!first) result += ", ";
	            first = false;
	            result += (String)param.accept(this);
	        }
	        result += ")";
	        return result;
	    }

	    public Object visitStmtSplit(StmtSplit stmt)
	    {
	        return "setSplitter(" + (String)stmt.getSplitter().accept(this) + ")";
	    }

	    public Object visitStmtVarDecl(StmtVarDecl stmt)
	    {
	        String result = "";	        
	        // Hack: if the first variable name begins with "_final_", the
	        // variable declaration should be final.
	        	        
	        for (int i = 0; i < stmt.getNumVars(); i++)
	        {
	            String nm = stmt.getName(i);
	            state.varDeclare(nm);
	            String lhsn = state.varGetLHSName(nm);
	            Type vt = stmt.getType(i);
	            if( vt instanceof TypeArray){
	            	TypeArray at = (TypeArray)vt;
	            	at.getLength().accept(this);
	            	valueClass tmp = state.popVStack();
	            	Assert(tmp.hasValue(), "The array size must be a compile time constant !! \n" + stmt.getContext());
	            	state.makeArray(nm, tmp.getIntValue());
	            	//this.state.markVectorStack();
	            	if( stmt.getInit(i) != null){
		            	stmt.getInit(i).accept(this);
		            	valueClass vclass = state.popVStack();
		            	if( vclass.isVect() ){
				    		List<valueClass> lst= vclass.getVectValue();
				    		Iterator<valueClass> it = lst.iterator();
				    		int tt = 0;
				    		while( it.hasNext() ){
				    			valueClass ival =  it.next();
				    			String nnm = nm + "_idx_" + tt;
				    			state.varDeclare(nnm);
			            		state.varGetLHSName(nnm);
				    			state.setVarValue(nnm, ival.getIntValue());
				    			++tt;
				    		}
				    		return "";
				    	}else{
				    		Integer val = null;
				    		if(vclass.hasValue())
				    			val = vclass.getIntValue();
			            	for(int tt=0; tt<tmp.getIntValue(); ++tt){
			            		String nnm = nm + "_idx_" + tt;
			            		state.varDeclare(nnm);
			            		String tmplhsn = state.varGetLHSName(nnm);
			            		if(val != null){
			            			state.setVarValue(tmplhsn, val.intValue());
			            		}
			            	}
				    	}
	            	}else{
	            		for(int tt=0; tt<tmp.getIntValue(); ++tt){
		            		String nnm = nm + "_idx_" + tt;
		            		state.varDeclare(nnm);
		            		state.varGetLHSName(nnm);		            		
		            	}
	            	}
	            }else{
		            
		            if (stmt.getInit(i) != null){     
		            	stmt.getInit(i).accept(this);
		            	valueClass tmp = state.popVStack();
		                String asgn = lhsn + " = " + tmp + "; \n";		                
		                if(tmp.hasValue()){
		                	state.setVarValue(nm, tmp.getIntValue());
		                }else{//Because the variable is new, we don't have to unset it if it is null. It must already be unset.
		                	result += asgn;	
		                } 	                
		            }
	            }
	        }
	        return result;
	    }

	    public Object visitStmtWhile(StmtWhile stmt)
	    {
	        return "while (" + (String)stmt.getCond().accept(this) +
	            ") " + (String)stmt.getBody().accept(this);
	    }

	    

	    public Object visitStreamSpec(StreamSpec spec)
	    {
	        String result = "// " + spec.getContext() + "\n"; 
	        
	        // Anonymous classes look different from non-anonymous ones.
	        // This appears in two places: (a) as a top-level (named)
	        // stream; (b) in an anonymous stream creator (SCAnon).
	        //StreamType st = spec.getStreamType();

	        //Assert( ((TypePrimitive)st.getOut()).getType() == TypePrimitive.TYPE_BIT, "Only bit types for now.");
	        //Assert( ((TypePrimitive)st.getIn()).getType() == TypePrimitive.TYPE_BIT, "Only bit types for now.");
	        StreamType st = spec.getStreamType();
	        
	        if( st != null && 
	        ( ( 
	    (  ((TypePrimitive)st.getIn()).getType() !=
                TypePrimitive.TYPE_BIT &&
				((TypePrimitive)st.getIn()).getType() !=
	                TypePrimitive.TYPE_VOID	
	    )|| 
				((TypePrimitive)st.getOut()).getType() !=
	                TypePrimitive.TYPE_BIT ) &&   spec.getType() == StreamSpec.STREAM_FILTER ) ){
	        	state.pushLevel();
	        	result = (String) nativeGenerator.visitStreamSpec(spec);
	        	state.popLevel();
	        	return result;
	        }
	        
	        state.pushLevel();
	        if (spec.getName() != null)
	        {	            
	            // This is only public if it's the top-level stream,
	            // meaning it has type void->void.	             
	            if (false)
	            {
	                result += spec.getTypeString() + " " + spec.getName() +";\n";	                
	            }
	            else
	            {	                
	                if (spec.getType() == StreamSpec.STREAM_FILTER)
	                {
	                    // Need to notice now if this is a phased filter.
	                    FuncWork work = spec.getWorkFunc();
	                    if (work!=null && (work.getPushRate() == null &&
	                        work.getPopRate() == null &&
	                        work.getPeekRate() == null) )
	                        result += "PhasedFilter";
	                    else
	                        result += "Filter";
	                }
	                else
	                    switch (spec.getType())
	                    {
	                    case StreamSpec.STREAM_PIPELINE:
	                        result += "Pipeline";
	                        break;
	                    case StreamSpec.STREAM_SPLITJOIN:
	                        result += "SplitJoin";
	                        break;
	                    case StreamSpec.STREAM_FEEDBACKLOOP:
	                        result += "FeedbackLoop";
	                        break;
	                    case StreamSpec.STREAM_TABLE:
	                        result += "Table";
	                        break;
	                    }
	                String nm = spec.getName();
	                Function f = spec.getFuncNamed("init");
	                Iterator it = f.getParams().iterator();
	                while(it.hasNext()){
	                	Parameter p = (Parameter) it.next();
	                	
	                	int sz = state.checkArray(p.getName());
	                	if(sz > 0){
	                	//if( p.getType() instanceof TypeArray){
	                	//	((TypeArray)p.getType()).getLength().accept(this);	                		
	                	//	int sz = state.popVStack().intValue(); 
	                		int xx=0;
	                		int xpon=1;
	                		for(int i=0; i<sz; ++i){
	                			xx += xpon *  state.varValue(p.getName()+ "_idx_" + i);
	                			xpon = xpon * 3;
	                		}
	                		nm += "_" + xx;
	                	}else{
	                		int i = state.varValue(p.getName());	                	
	                		nm += "_" + i;
	                	}
	                }
	                if( f.getParams().size()>0)
	                	nm += "___";
	                result += " " + nm + "\n";
	            }
	        }
	        else
	        {
	            // Anonymous stream:
	            result += "new ";
	            switch (spec.getType())
	            {
	            case StreamSpec.STREAM_FILTER: result += "Filter";
	                break;
	            case StreamSpec.STREAM_PIPELINE: result += "Pipeline";
	                break;
	            case StreamSpec.STREAM_SPLITJOIN: result += "SplitJoin";
	                break;
	            case StreamSpec.STREAM_FEEDBACKLOOP: result += "FeedbackLoop";
	                break;
	            case StreamSpec.STREAM_TABLE: result += "Table";
                	break;
	            }
	            result += "() \n" ;
	        }
	        	        	        
	        
	        
	        
	        if(spec.getType() == StreamSpec.STREAM_TABLE){
	        	/**
	        	 * TODO: Implement Tables properly. This is a kludge, but it will
	        	 * allow me to implement AES.
	        	 */	        	
	        	
	        	//NodesToTable ntt = new NodesToTable(spec, this.varGen);
	        	Function f = spec.getFuncNamed("init");
	        	result += f.getBody().accept(this.nativeGenerator);
	        	return result;
	        }

	        // At this point we get to ignore wholesale the stream type, except
	        // that we want to save it.
	        
	        StreamSpec oldSS = ss;
	        ss = spec;
	        result += "{\n"; 
	        // Output field definitions:
	        
	        
	        additInit = new LinkedList<Statement>();
	        
	        for (Iterator iter = spec.getVars().iterator(); iter.hasNext(); )
	        {
	            FieldDecl varDecl = (FieldDecl)iter.next();
	            result += (String)varDecl.accept(this);
	        }
	        preFil.push("");    		
	        // Output method definitions:
	        Function f = spec.getFuncNamed("init");
			if( f!= null)
				result += (String)(f.accept(this));
			
	        for (Iterator iter = spec.getFuncs().iterator(); iter.hasNext(); ){
	        	f = (Function)iter.next();
	        	if( ! f.getName().equals("init") ){
	        		result += (String)(f.accept(this));
	        	}	        	
	        }
//	  TODO: DOTHIS      if( spec.getType() == StreamSpec.STREAM_TABLE ){
//	        	outputTable=;
//	        }
	        ss = oldSS;
	        result += "}\n";
	        state.popLevel();
	        if (spec.getName() != null){
		        while( preFil.size() > 0){
		        	String otherFil = (String) preFil.pop();
		        	result = otherFil + result;
		        }
	        }
	        return result;
	    }
	    
	    public Object visitStreamType(StreamType type)
	    {
	        // Nothing to do here.
	        return "";
	    }
	    
	    public Object visitOther(FENode node)
	    {
	        if (node instanceof ExprJavaConstructor)
	        {
	            ExprJavaConstructor jc = (ExprJavaConstructor)node;
	            String rval = makeConstructor(jc.getType()); 
	            state.pushVStack(new valueClass(rval));
	            return rval;
	        }
	        if (node instanceof StmtIODecl)
	        {
	            StmtIODecl io = (StmtIODecl)node;
	            String result = "";
	            if (io.getRate1() != null && false){
	            	result += io.getName() + "_RATE="+
	                (String)io.getRate1().accept(this) + ";\n";
	            	valueClass iv = state.popVStack();
	            	Assert( iv.hasValue(), "The compiler must be able to determine the IO rate at compile time. \n" + io.getContext() );
	            }
	            if (io.getRate2() != null){
	                //result += "\n "+ io.getName() + "_RATE2=" + (String)io.getRate2().accept(this)+ ";\n";
	            }
	            return result;
	        }
	        if (node instanceof StmtAddPhase)
	        {
	            StmtAddPhase ap = (StmtAddPhase)node;
	            String result;
	            if (ap.isInit())
	                result = "addInitPhase";
	            else result = "addSteadyPhase";
	            result += "(";
	            if (ap.getPeek() == null)
	                result += "0, ";
	            else
	                result += (String)ap.getPeek().accept(this) + ", ";
	            if (ap.getPop() == null)
	                result += "0, ";
	            else
	                result += (String)ap.getPop().accept(this) + ", ";
	            if (ap.getPush() == null)
	                result += "0, ";
	            else
	                result += (String)ap.getPush().accept(this) + ", ";
	            result += "\"" + ap.getName() + "\")";
	            return result;
	        }
	        if (node instanceof StmtSetTypes)
	        {
	            StmtSetTypes sst = (StmtSetTypes)node;
	            return "setIOTypes(" + typeToClass(sst.getInType()) +
	                ", " + typeToClass(sst.getOutType()) + ")";
	        }
	        return "";
	    }
		public Object visitExprStar(ExprStar star) {			
			String cvar = state.varDeclare();
			oracle.addBinding(star, cvar);
			String isFixed = star.isFixed()? " *" : "";
			String rval;
			if(star.getSize() > 1)
				rval =  "<" + cvar + "  " + star.getSize() + isFixed+ ">";
			else
				rval =  "<" + cvar +  ">";
			state.pushVStack(new valueClass(rval));
			return star;
		}
}
