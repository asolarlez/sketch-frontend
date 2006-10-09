/*
 * Created on Jun 22, 2004
 *
 * 
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package streamit.frontend.tosbit;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import streamit.frontend.nodes.ExprBinary;
import streamit.frontend.nodes.ExprConstInt;
import streamit.frontend.nodes.ExprConstBoolean;
import streamit.frontend.nodes.ExprFunCall;
import streamit.frontend.nodes.ExprStar;
import streamit.frontend.nodes.ExprVar;
import streamit.frontend.nodes.Expression;
import streamit.frontend.nodes.FEContext;
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
import streamit.frontend.nodes.StmtAssert;
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
    protected HashMap<String, StreamSpec> funsWParams;
    protected Stack<String> preFil;
    protected List<Statement> additInit;
    private ValueOracle oracle;
    private LoopMap loopmap= new LoopMap();
	protected PrintStream out;
    
    
	    public NodesToSBit (StreamSpec ss, TempVarGen varGen,
                            ValueOracle oracle, PrintStream out,
                            int maxUnroll, int maxInline)
	    {
	    	super(false, maxUnroll, maxInline);
	        this.ss = ss;	        
	        this.varGen = varGen;	         
	        this.state = new MethodState();
	        this.oracle = oracle;
	        funsWParams = new HashMap<String, StreamSpec>();
	        preFil = new Stack<String>();
	        nativeGenerator = new NodesToNative(ss, varGen, state, this);
	        this.out = out;
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
	    
	    public Object visitExprFunCall(ExprFunCall exp)
	    {	    	
	    	//String result = " ";
	    	ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    	PrintStream ps = new PrintStream(baos);
	    	String name = exp.getName();
            Function fun = ss.getFuncNamed (name);
	    	// Local function?
	    	if (fun != null) {        	
                String spec = fun.getSpecification ();
	    		if (spec != null) {
                    fun = ss.getFuncNamed (spec);
                    assert (fun != null);
	    		}

                /* Check to see whether function was already inlined to its
                 * maximum allowed number of times. */
                int numInlined = getInlineCounter (fun.getName ());

                if (numInlined == MAX_INLINE) {
                    /* Cannot inline further, plant an assertion. */
                    FEContext exprContext = exp.getContext ();
                    StmtAssert inlineAssert =
                        new StmtAssert (exprContext,
                                        new ExprConstBoolean (
                                            exprContext,
                                            false));
                    ps.print ("// MAX INLINED: BEGIN " + fun.getName () + " (" +
                              MAX_INLINE + ")\n");
                    PrintStream tmpout = out;
                    out = new PrintStream (baos);
                    inlineAssert.accept (this);
                    out = tmpout;
                    ps.print ("// MAX INLINED: END " + fun.getName () + " (" +
                              MAX_INLINE + ")\n");

                    Iterator actualParams = exp.getParams().iterator();	        		        	       	
                    Iterator formalParams = fun.getParams().iterator();
                    String tmp =
                        outParameterSetterArbitrary (formalParams,
                                                     actualParams,
                                                     false);
                    ps.print (tmp);
                } else {
                    /* Increment inline counter, unfold another level. */
                    incInlineCounter (fun.getName ());

                    state.pushLevel();

                    Iterator actualParams = exp.getParams().iterator();	        		        	       	
                    Iterator formalParams = fun.getParams().iterator();
                    String tmp = inParameterSetter(formalParams, actualParams, false);
                    ps.print(tmp);

                    ps.print("// BEGIN CALL " + fun.getName() +
                             " (" + numInlined + ")\n");

                    PrintStream tmpout = out; out = new PrintStream( baos );	    		
                    fun.getBody().accept(this);	    		
                    //result += baos.toString();	    		
                    out = tmpout;

                    ps.print("// END CALL " + fun.getName() +
                             " (" + numInlined +  ")\n");

                    actualParams = exp.getParams().iterator();	        		        	       	
                    formalParams = fun.getParams().iterator();
                    tmp =  outParameterSetter(formalParams, actualParams, false);
                    ps.print(tmp);

                    state.popLevel();

                    /* Decrement inline counter. */
                    decInlineCounter (fun.getName ());
                }
	    	}else{ 
	    		// look for print and println statements; assume everything
	    		// else is a math function
	    		if (name.equals("print")) {
	    			System.err.println("The StreamBit compiler currently doesn't allow print statements in bit->bit filters.");
	    			return "";
	    		} else if (name.equals("println")) {
	    			//result = "System.out.println(";
	    			System.err.println("The StreamBit compiler currently doesn't allow print statements in bit->bit filters.");
	    			return "";
	    		} else if (name.equals("super")) {
	    			//result = "";
	    		} else if (name.equals("setDelay")) {
	    			//result = "";
	    		} else if (name.startsWith("enqueue")) {	        	
	    			//result = "";
	    		} else {
	    			Assert(false, "The streamBit compiler currently doesn't allow bit->bit filters to call other functions. You are trying to call the function" + name);
	    			// Math.sqrt will return a double, but we're only supporting
	    			// float's now, so add a cast to float.  Not sure if this is
	    			// the right thing to do for all math functions in all cases?
	    			ps.print("(float)Math." + name + "(");
	    		}
	    	}
	    	state.pushVStack( new valueClass(baos.toString()) );
	    	return exp;    	
	    }


	    public Object visitFunction(Function func)
	    {
	        
	        if (!func.getName().equals(ss.getName()));
	        
	        if(func.getCls() == Function.FUNC_INIT){
	        	out.print("INIT()");
	        	//result += doParams(func.getParams(), "") + "\n";
	        	out.print("{\n");
	        	this.state.pushLevel();  
	        	try{
	        		func.getBody().accept(this);
	        	}finally{
	        		this.state.popLevel();
	        	}
	        	Iterator it = this.additInit.iterator();
	        	PrintStream tmpout = out; out = new PrintStream( new ByteArrayOutputStream() );
	        	while(it.hasNext()){
	        		Statement st = (Statement) it.next();
	        		st.accept(this);
	        	}
	        	this.additInit.clear();
	        	out = tmpout;
	        	out.print("}\n");	        	
	        }else if(func.getCls() == Function.FUNC_WORK){
	        	out.print("WORK()\n");
	        	Assert( func.getParams().size() == 0 , "");	        	
	        	out.print("{\n");
	        	this.state.pushLevel();
	        	try{
		        	initializeWork();
		        	Expression pushr = ((FuncWork)func).getPopRate();
		        	Expression popr = ((FuncWork)func).getPushRate();
		        	if(pushr != null){
		        		pushr.accept(this);
		        		out.print("input_RATE = " + state.popVStack() + ";\n");
		        	}else{
		        		out.print("input_RATE = 0;\n");
		        	}
		        	if(popr != null){
		        		popr.accept(this);
		        		out.print("output_RATE = " + state.popVStack() + ";\n");
		        	}else{
		        		out.print("output_RATE = 0;\n");	        	
		        	}	        	
		        	Assert(((StmtBlock)func.getBody()).getStmts().size()>0, "You can not have empty functions!\n" + func.getContext() );
		        	func.getBody().accept(this);
		        	out.print(finalizeWork());
	        	}finally{
	        		this.state.popLevel();
	        	}
	        	out.print("}\n");
	        }else{
	        	out.print(func.getName());
	        	if( func.getSpecification() != null ){
	        		out.print(" SKETCHES " + func.getSpecification()); 
	        	}
	        	out.print(doParams(func.getParams(), "") + "\n");
	        	out.print("{\n");
	        	this.state.pushLevel();  
	        	try{
	        		func.getBody().accept(this);
	        	}finally{
	        		this.state.popLevel();
	        	}
	        	out.print(postDoParams(func.getParams()));
	        	
	        	PrintStream tmpout = out; out = new PrintStream( new ByteArrayOutputStream() );
	        	Iterator it = this.additInit.iterator();
	        	while(it.hasNext()){
	        		Statement st = (Statement) it.next();
	        		st.accept(this);
	        	}
	        	this.additInit.clear();
	        	out = tmpout;
	        	
	        	out.print("}\n");	
	        	//state.pushVStack(new valueClass((String)null) );
	        }
	        
	        out.print("\n");
	        return null;
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
	    	
	    	ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    	PrintStream tmpout = out; out = new PrintStream( baos );	    	
	        for (Iterator iter = prog.getStreams().iterator(); iter.hasNext(); ){
	        	StreamSpec sp = (StreamSpec)iter.next();	        	
	        	funsWParams.put(sp.getName(), sp);	        		        	
	        }
	        for (Iterator iter = prog.getStreams().iterator(); iter.hasNext(); ){
	        	StreamSpec sp = (StreamSpec)iter.next();
	        	Function finit = sp.getFuncNamed("init");
	        	if(finit == null){
	        		sp.accept(this);
	        	}else{
		        	if(finit.getParams().size() > 0){
		        		//funsWParams.put(sp.getName(), sp);
		        	}else{	        		
		        		sp.accept(this);
		        	}	
	        	}
	        }
	        out = tmpout;
	        String tmpstr = "";
	        while( preFil.size() > 0){
	        	String otherFil = (String) preFil.pop();
	        	tmpstr = otherFil + tmpstr;	        	
	        }
	        out.print(tmpstr);
	        out.print(baos);
	        
	        return null;
	    }

	    public Object visitSCAnon(SCAnon creator)
	    {
	        return creator.getSpec().accept(this);
	    }
	    
	    public Object visitSCSimple(SCSimple creator)
	    {
	    	
	    	if( creator.getParams().size() == 0){
		        out.print("new " + creator.getName() + "(");
		        boolean first = true;
		        for (Iterator iter = creator.getParams().iterator(); iter.hasNext(); )
		        {
		            Expression param = (Expression)iter.next();
		            if (!first) out.print(", ");
		            param.accept(this);
		            out.print( state.popVStack() );
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
		        out.print( ")" );
	    	}else{
	    		String nm = creator.getName();
	    		out.print( "new " );
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
		        out.print( fullnm + "()" );
		        if( funsWParams.get(fullnm) == null){
			        StreamSpec sp = (StreamSpec) funsWParams.get(nm);
			        funsWParams.put(fullnm, sp);
			        Assert( sp != null, nm + "Is used but has not been declared!!");
			        state.pushLevel();
			        try{
				        Function finit = sp.getFuncNamed("init");
				        Iterator formalParamIterator = finit.getParams().iterator();
				        Iterator actualParamIterator = creator.getParams().iterator();
				        Assert(finit.getParams().size() == creator.getParams().size() , nm + " The number of formal parameters doesn't match the number of actual parameters!!");
				        
				        inParameterSetter(formalParamIterator,actualParamIterator, true);
				        
				        Object obj = preFil.pop();
				        String tmp = obj == null?"" : (String)obj; 
				        
				        ByteArrayOutputStream baos = new ByteArrayOutputStream();
				    	PrintStream tmpout = out; out = new PrintStream( baos );
				        
				    	sp.accept(this);
				    	
				        preFil.push( tmp + baos.toString() );
				        
				        out = tmpout;
			        }finally{
			        	state.popLevel();
			        }
		        }
	    	}
	        return null;
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
	        
	        String lhsnm = null;
	        
	        
	        boolean isArr = false;
	        int arrSize = -1;
	        if(! lhsvisitor.isNDArracc()){
	        	lhsnm = state.varGetLHSName(lhs);
	        	arrSize = state.checkArray(lhs);
		        isArr = arrSize > 0;	
	        }
	                
	        
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
	        out.print(lhsnm + op + vrhsVal);
	        return null;
	    }

	    public Object visitStmtBlock(StmtBlock stmt)
	    {
	        // Put context label at the start of the block, too.
	    	state.pushLevel();	    	
	    	try{
//		        result.append("// {");
//		        if (stmt.getContext() != null)
//		            result.append(" \t\t\t// " + stmt.getContext());
//		        result.append("\n");
		        for (Iterator iter = stmt.getStmts().iterator(); iter.hasNext(); )
		        {
		            Statement s = (Statement)iter.next();
		            out.print(" ");		            	
		            s.accept(this);
		            if(true){
			            if (!(s instanceof StmtIfThen)) {
			            	out.print(";");
			            }
			            if (s.getContext() != null)
			            	out.print(" \t\t\t// " + s.getContext());
			            out.print("\n");
		            }
		        }
//		        result.append(" // }\n");
	    	}finally{
	    		state.popLevel();
	    	}
	        return null;
	    }

	    public Object visitStmtBody(StmtBody stmt)
	    {
	        return doStreamCreator("setBody", stmt.getCreator());
	    }
	    
	    public Object visitStmtBreak(StmtBreak stmt)
	    {
	    	assert false :"NYI";
	        return "break";
	    }
	    
	    public Object visitStmtContinue(StmtContinue stmt)
	    {
	    	assert false :"NYI";
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
	    	assert false :"NYI";
	        return "/* enqueue(" + (String)stmt.getValue().accept(this) +
	            ") */";
	    }
	    
	    public Object visitStmtExpr(StmtExpr stmt)
	    {
	    	Expression exp = stmt.getExpression();
	    	exp.accept(this);
	    	String result = state.popVStack().toString();
	    	out.print(result);
		    return null;	    	
	    }

	    public Object visitStmtFor(StmtFor stmt)
	    {
	    	state.pushLevel();	    	
	    	try{
		        loopmap.pushLoop(0);		        
		        if (stmt.getInit() != null)
		            stmt.getInit().accept(this);
		        
		        Assert( stmt.getCond() != null , "For now, the condition in your for loop can't be null");
		        stmt.getCond().accept(this);
		        valueClass vcond = state.popVStack();
		        int iters = 0;		       
		        while(vcond.hasValue() && vcond.getIntValue() > 0){
		        	++iters;

		        	stmt.getBody().accept(this);
		        			        	
		        	loopmap.nextIter();
		        	if (stmt.getIncr() != null){
		        		ByteArrayOutputStream baos = new ByteArrayOutputStream();
					    PrintStream tmpout = out; out = new PrintStream( baos );
			        	stmt.getIncr().accept(this);
			        	out = tmpout;
		        	}
		        	stmt.getCond().accept(this);
			        vcond = state.popVStack();
			        Assert(iters <= (1<<13), "This is probably a bug, why would it go around so many times? " + stmt.getContext());
		        }
		        
		        loopmap.popLoop();
	    	}finally{
	    		state.popLevel();	        	
	    	}
	    	return null;
	    }

	    public Object visitStmtIfThen(StmtIfThen stmt)
	    {
	        // must have an if part...
	    		        
            Expression cond = stmt.getCond();
	        cond.accept(this);
	        valueClass vcond = state.popVStack();
	        if(vcond.hasValue()){
	        	if(vcond.getIntValue() > 0){
	        		stmt.getCons().accept(this);	        		
	        	}else{
	        		if (stmt.getAlt() != null)
	    	            stmt.getAlt().accept(this);
	        	}
	        	return null;   	
	        }

            /* Attach conditional to change tracker. */
	        state.pushChangeTracker (cond, vcond, false);
            
	        try{
	        	stmt.getCons().accept(this);
	        }catch(RuntimeException e){
	        	state.popChangeTracker();
	        	throw e;
	        }
	        ChangeStack ipms = state.popChangeTracker();
	        ChangeStack epms = null;	        
	        if (stmt.getAlt() != null){
                /* Attach inverse conditional to change tracker. */
                state.pushChangeTracker (cond, vcond, true);
	        	try{
	        		stmt.getAlt().accept(this);
	        	}catch(RuntimeException e){
		        	state.popChangeTracker();
		        	throw e;
		        }
	            epms = state.popChangeTracker();
	        }
	        if(epms != null){
	        	String tmpVar = varGen.nextVar();
	        	out.print(tmpVar + " = " + vcond.toString() + "; \n");
	        	out.print(state.procChangeTrackers(ipms, epms, tmpVar));
	        }else{	        	
	        	String tmpVar = varGen.nextVar();
	        	out.print(tmpVar + " = " + vcond.toString() + "; \n");
	        	out.print(state.procChangeTrackers(ipms, tmpVar));
	        }
	        return null;
	    }

        /**
         * Assert statement visitor. Generates a complex assertion expression which
         * takes into consideration the chain of nesting conditional expressions, and
         * composes them as premises to the given expression.
         *
         * @author Gilad Arnold
         */
        public Object visitStmtAssert (StmtAssert stmt) {
            /* Evaluate given assertion expression. */
            Expression assertCond = stmt.getCond ();
            assertCond.accept (this);
            valueClass assertVal = state.popVStack ();

            /* Compose complex expression by walking all nesting conditionals. */
            String tmpVar = varGen.nextVar ();
            String tmpLine = tmpVar + " = " + assertVal.toString ();
            int parCounter = 0;
            for (ChangeStack changeTracker = state.getChangeTracker ();
                 changeTracker != null; changeTracker = changeTracker.kid )
            {
                if (! changeTracker.hasCondVal ())
                    continue;
                valueClass nestCond = changeTracker.getCondVal ();
                tmpLine += " || (" + (changeTracker.isNegated () ? "" : "! ") +
                    "(" + nestCond.toString () + ")";
                parCounter++;
            }
            while (parCounter > 0) {
                tmpLine += ")";
                parCounter--;
            }

            tmpLine += ";\n";
            out.print (tmpLine);
            out.print ("assert (" + tmpVar + ");\n");

            return null;
        }

	    public Object visitStmtJoin(StmtJoin stmt)
	    {
	    	assert false : "NYI";
	        return "setJoiner(" + (String)stmt.getJoiner().accept(this) + ")";
	    }
	    
	    public Object visitStmtLoop(StmtLoop stmt)
	    {
            /* Generate a new variable, initialized with loop expression. */
            FEContext nvarContext = stmt.getContext ();
            String nvar = varGen.nextVar ();
            StmtVarDecl nvarDecl =
                new StmtVarDecl (nvarContext,
                                 new TypePrimitive (TypePrimitive.TYPE_INT),
                                 nvar,
                                 stmt.getIter ());
            nvarDecl.accept (this);

            /* Generate and visit an expression consisting of the new variable. */
            ExprVar nvarExp = 
                new ExprVar (nvarContext,
                             nvar);
            nvarExp.accept (this);

            /* Check result, get full (LHS) variable name. */
	    	valueClass vcond = state.popVStack();
            String nvarFull = vcond.toString ();

            /* If no known value, perform conditional unrolling of the loop. */
	    	if (! vcond.hasValue ()) {
                /* Assert loop expression does not exceed max unrolling constant. */
                StmtAssert nvarAssert =
                    new StmtAssert (nvarContext,
                                    new ExprBinary (
                                        nvarContext,
                                        ExprBinary.BINOP_LE,
                                        new ExprVar (nvarContext, nvar),
                                        new ExprConstInt (nvarContext, MAX_UNROLL)));
                nvarAssert.accept (this);

	    		int iters;
	    		loopmap.pushLoop (MAX_UNROLL);
	    		for (iters=0; iters < MAX_UNROLL; ++iters) {
                    /* Generate context condition to go with change tracker. */
                    Expression guard =
                        new ExprBinary (nvarContext,
                                        ExprBinary.BINOP_GT,
                                        new ExprVar (nvarContext, nvar),
                                        new ExprConstInt (nvarContext, iters));
                    guard.accept (this);
                    valueClass vguard = state.popVStack ();
                    assert (! vguard.hasValue ());
			        state.pushChangeTracker (guard, vguard, false);

			        ByteArrayOutputStream baos = new ByteArrayOutputStream();
			    	PrintStream tmpout = out; out = new PrintStream( baos );
			        try{
			        	stmt.getBody().accept(this);
			        }catch(ArrayIndexOutOfBoundsException er){			        	
			        	state.popChangeTracker();
			        	out = tmpout;
			        	break;
		    		}
			        loopmap.nextIter();
			        out = tmpout;
			        out.print(baos);			        
	    		}
	    		
	    		for(int i=iters-1; i>=0; --i){

	    			String cond = "(" + nvarFull + ")>" + i;
	    			// I thought this would be more efficient, but it wasn't.
//	    			String tmpVar = varGen.nextVar();
	    			//result += tmpVar + " = " + cond + "; \n";		        		    			
	    			ChangeStack ipms = state.popChangeTracker();
	    			
	    			out.print(state.procChangeTrackers(ipms, cond));
	    		}
	    		loopmap.popLoop();
		        return null;
	    	}else{
	    		loopmap.pushLoop(vcond.getIntValue());
	    		for(int i=0; i<vcond.getIntValue(); ++i){
	    			stmt.getBody().accept(this);
	    			loopmap.nextIter();
	    		}
	    		loopmap.popLoop();
	    	}
	    	return null;
	    }

	    public Object visitStmtPhase(StmtPhase stmt)
	    {
	    	assert false : "NYI";
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
	    		return null;
	    		//return lhs + " = " + ival;
	    	}else{
	    		out.print(lhs + " = " + val);
	    		return null;
	    	}	    	
	        //return pushFunction(ss.getStreamType()) + "(" +
	        //    (String)stmt.getValue().accept(this) + ")";
	    }

	    public Object visitStmtReturn(StmtReturn stmt)
	    {
	    	assert false :"This opperation should not appear here!!";
	        if (stmt.getValue() == null) return "return";
	        return "return " + (String)stmt.getValue().accept(this);
	    }

	    public Object visitStmtSendMessage(StmtSendMessage stmt)
	    {
	    	assert false :"NYI";
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
	    	assert false :"NYI";
	        return "setSplitter(" + (String)stmt.getSplitter().accept(this) + ")";
	    }

	    public Object visitStmtVarDecl(StmtVarDecl stmt)
	    {
	    	
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
			            		String loclhsn = state.varGetLHSName(nnm);
			            		if( ival.hasValue()){
			            			state.setVarValue(nnm, ival.getIntValue());
			            		}else{
			            			out.println(loclhsn + " = " + ival + ";");
			            		}
				    			++tt;
				    		}
				    		return null;
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
		                	out.print(asgn);	
		                } 	                
		            }
	            }
	        }
	        return null;
	    }

	    public Object visitStmtWhile(StmtWhile stmt)
	    {
	    	assert false : "NYI";
	        return "while (" + (String)stmt.getCond().accept(this) +
	            ") " + (String)stmt.getBody().accept(this);
	    }


	    public Object visitStreamSpec(StreamSpec spec)
	    {
	    	ByteArrayOutputStream baos = new ByteArrayOutputStream();
		    PrintStream tmpout = out; out = new PrintStream( baos );
	    	out.print( "// " + spec.getContext() + "\n" ); 
	    	
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
	    		try{
	    			nativeGenerator.visitStreamSpec(spec);
	    		}finally{
	    			state.popLevel();
	    		}
	    		out = tmpout;
	    		out.print( baos.toString() );
	    		return null;
	    	}
	    	
	    	state.pushLevel();
	    	try{
	    		if (spec.getName() != null)
	    		{	            
	    			// This is only public if it's the top-level stream,
	    			// meaning it has type void->void.	             
	    			if (false)
	    			{
	    				out.print( spec.getTypeString() + " " + spec.getName() +";\n" );	                
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
	    						out.print( "PhasedFilter" );
	    					else
	    						out.print( "Filter");
	    				}
	    				else
	    					switch (spec.getType())
	    					{
	    					case StreamSpec.STREAM_PIPELINE:
	    						out.print( "Pipeline");
	    						break;
	    					case StreamSpec.STREAM_SPLITJOIN:
	    						out.print( "SplitJoin");
	    						break;
	    					case StreamSpec.STREAM_FEEDBACKLOOP:
	    						out.print( "FeedbackLoop");
	    						break;
	    					case StreamSpec.STREAM_TABLE:
	    						out.print( "Table" );
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
	    				out.print(  " " + nm + "\n" );
	    			}
	    		}
	    		else
	    		{
	    			// Anonymous stream:
	    			out.print( "new " );
	    			switch (spec.getType())
	    			{
	    			case StreamSpec.STREAM_FILTER: out.print( "Filter");
	    			break;
	    			case StreamSpec.STREAM_PIPELINE: out.print("Pipeline");
	    			break;
	    			case StreamSpec.STREAM_SPLITJOIN: out.print("SplitJoin");
	    			break;
	    			case StreamSpec.STREAM_FEEDBACKLOOP:   out.print( "FeedbackLoop");
	    			break;
	    			case StreamSpec.STREAM_TABLE: out.print( "Table");
	    			break;
	    			}
	    			out.print("() \n" );
	    		}
	    		
	    		
	    		
	    		
	    		if(spec.getType() == StreamSpec.STREAM_TABLE){
	    			/**
	    			 * TODO: Implement Tables properly. This is a kludge, but it will
	    			 * allow me to implement AES.
	    			 */	        	
	    			
	    			//NodesToTable ntt = new NodesToTable(spec, this.varGen);
	    			Function f = spec.getFuncNamed("init");
	    			f.getBody().accept(this.nativeGenerator);
	    			out = tmpout;
		    		out.print( baos.toString() );
	    			return null;
	    		}
	    		
	    		// At this point we get to ignore wholesale the stream type, except
	    		// that we want to save it.
	    		
	    		StreamSpec oldSS = ss;
	    		ss = spec;
	    		out.print("{\n"); 
	    		// Output field definitions:
	    		
	    		
	    		additInit = new LinkedList<Statement>();
	    		
	    		for (Iterator iter = spec.getVars().iterator(); iter.hasNext(); )
	    		{
	    			FieldDecl varDecl = (FieldDecl)iter.next();
	    			varDecl.accept(this);
	    		}

	    		// Output method definitions:
	    		Function f = spec.getFuncNamed("init");
	    		if( f!= null)
	    			f.accept(this);
	    		
	    		for (Iterator iter = spec.getFuncs().iterator(); iter.hasNext(); ){
	    			f = (Function)iter.next();
	    			if( ! f.getName().equals("init") ){
	    				f.accept(this);
	    			}	        	
	    		}
//	    		TODO: DOTHIS      if( spec.getType() == StreamSpec.STREAM_TABLE ){
//	    		outputTable=;
//	    		}
	    		ss = oldSS;
	    		out.print("}\n");
	    	}finally{
	    		state.popLevel();
	    	}
	    	
	    	out = tmpout;    		
	    	
	    	if (spec.getName() != null){
	    		String tmpstr = "";
	    		while( preFil.size() > 0){
	    			String otherFil = (String) preFil.pop();
	    			tmpstr = otherFil + tmpstr;	    			
	    		}
	    		out.print(tmpstr);
	    	}
	    	out.print( baos.toString() );
	    	return null;
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
	        	assert false : "Not implemented";
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
	        	assert false : "Not implemented";
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
	            out.print( "setIOTypes(" + typeToClass(sst.getInType()) +
		                ", " + typeToClass(sst.getOutType()) + ")" );
	            return null;
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
