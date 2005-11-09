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
import java.util.Map;
import java.util.Stack;

import streamit.frontend.nodes.*;
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
public class NodesToSBit implements FEVisitor{
	    protected StreamSpec ss;
	    // A string consisting of an even number of spaces.
	    private String indent;
	    private TempVarGen varGen;
	    protected MethodState state;
	    private boolean isLHS;
	    private NodesToNative nativeGenerator;
	    private HashMap funsWParams;
	    protected Stack preFil;
	    protected List additInit;
	    private ValueOracle oracle;
	    
	    private void Assert(boolean t, String s){
	    	if(!t){
	    		System.err.println(s);
	    		System.err.println( ss.getContext() );
	    		throw new RuntimeException(s);
	    	}
	    }
	    private int boolToInt(boolean b){	    	
	    	if(b)
	    		return 1;
	    	else 
	    		return 0;
	    }
	    private boolean intToBool(int v){
	    	if(v>0)
	    		return true;
	    	else
	    		return false;
	    }
	    public NodesToSBit(StreamSpec ss, TempVarGen varGen, ValueOracle oracle)
	    {
	        this.ss = ss;
	        this.indent = "";
	        this.varGen = varGen;
	        this.isLHS = false;	 
	        this.state = new MethodState();
	        this.oracle = oracle;
	        funsWParams = new HashMap();
	        preFil = new Stack();
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
	    private void addIndent() 
	    {
	        //indent += "  ";
	    }
	    
	    // Remove two spaces from the indent.
	    private void unIndent()
	    {
	        //indent = indent.substring(2);
	    }

	    // Convert a Type to a String.  If visitors weren't so generally
	    // useless for other operations involving Types, we'd use one here.
	    public String convertType(Type type)
	    {
	        // This is So Wrong in the greater scheme of things.
	        if (type instanceof TypeArray)
	        {
	            TypeArray array = (TypeArray)type;
	            String base = convertType(array.getBase());
	            String len = (String) array.getLength().accept(this);
	            Integer i = state.popVStack();
	            assert i != null : "Array length in a type must be static. " + len;
	            return base + "[" + i + "]";
	        }
	        else if (type instanceof TypeStruct)
		{
		    return ((TypeStruct)type).getName();
		}
		else if (type instanceof TypeStructRef)
	        {
		    return ((TypeStructRef)type).getName();
	        }
	        else if (type instanceof TypePrimitive)
	        {
	            switch (((TypePrimitive)type).getType())
	            {
	            case TypePrimitive.TYPE_BOOLEAN: return "boolean";
	            case TypePrimitive.TYPE_BIT: return "bit";
	            case TypePrimitive.TYPE_INT: return "int";
	            case TypePrimitive.TYPE_FLOAT: return "float";
	            case TypePrimitive.TYPE_DOUBLE: return "double";
	            case TypePrimitive.TYPE_COMPLEX: return "Complex";
	            case TypePrimitive.TYPE_VOID: return "void";
	            }
	        }
	        else if (type instanceof TypePortal)
	        {
	            return ((TypePortal)type).getName() + "Portal";
	        }
	        return null;
	    }

	    // Do the same conversion, but including array dimensions.
	    public String convertTypeFull(Type type)
	    {
	        if (type instanceof TypeArray)
	        {
	            TypeArray array = (TypeArray)type;
	            String len = (String)array.getLength().accept(this);
	            Integer i = state.popVStack();
	            assert  i != null: "This should never happen";
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
		            	Integer tmp = state.popVStack();
		            	Assert(tmp != null, "The array size must be a compile time constant !! \n" );		            	
		            	for(int tt=0; tt<tmp.intValue(); ++tt){
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
	    
	    // Return a representation of a list of Parameter objects.
	    public String doParams(List params, String prefix)
	    {
	        String result = "(";
	        boolean first = true;
	        for (Iterator iter = params.iterator(); iter.hasNext(); )
	        {
	            Parameter param = (Parameter)iter.next();
	            if (!first) result += ", ";
	            
	            if(param.isParameterOutput()) result += "! ";
	            
	            if (prefix != null) result += prefix + " ";
	            result += convertType(param.getType());
	            result += " ";
	            
	            String lhs = param.getName();
	            state.varDeclare(lhs);
	            String lhsn = state.varGetLHSName(lhs);	            
	            if( param.getType() instanceof TypeArray ){
	            	TypeArray ta = (TypeArray) param.getType();
	            	ta.getLength().accept(this);
	            	Integer tmp = state.popVStack();
	            	Assert(tmp != null, "The array size must be a compile time constant !! \n" );
	            	state.makeArray(lhs, tmp.intValue());
	            	for(int tt=0; tt<tmp.intValue(); ++tt){
	            		String nnm = lhs + "_idx_" + tt;
	            		state.varDeclare(nnm);
	            		String tmplhsn = state.varGetLHSName(nnm);
	            		if(param.isParameterOutput()){
	            			state.varDeclare("_p_"+nnm);
		            		String tmplhsn2 = state.varGetLHSName("_p_"+nnm);
		            		result += tmplhsn2 + "  ";
	            		}else{
	            			result += tmplhsn + "  ";
	            		}
	            	}
	            }else{
	            	if(param.isParameterOutput()){
	            		state.varDeclare("_p_"+lhs);
	            		String tmplhsn2 = state.varGetLHSName("_p_"+lhs);
	            		result += tmplhsn2;
	            	}else{
	            		result += lhsn;
	            	}
	            }
	            first = false;
	        }
	        result += ")";
	        return result;
	    }

	    // Return a representation of lhs = rhs, with no trailing semicolon.
	    public String doAssignment(Expression lhs, Expression rhs,
	                               SymbolTable symtab)
	    {
	        // We can use a null stream type here since the left-hand
	        // side shouldn't contain pushes, pops, or peeks.	        
            // Might want to special-case structures and arrays;
            // ignore for now.
	    	this.isLHS = true;
	    	String lhss = (String) lhs.accept(this);
	    	this.isLHS = false;	    	
	    	lhss = state.varGetLHSName(lhss);
	    	
	    	Integer vlhs = state.popVStack();

	    	String rhss = (String) rhs.accept(this);
	    	if( state.topOfStackIsVector() ){
	    		List lst= this.state.vectorPopVStack();
	    		Iterator it = lst.iterator();
	    		int idx = 0;
	    		while( it.hasNext() ){
	    			Integer i = (Integer) it.next();
	    			state.setVarValue(lhss + "_idx_" + idx, i.intValue());
	    			++idx;
	    		}
	    		return "";
	    	}else{
		    	Integer vrhs =  state.popVStack();
		    	if(vrhs != null){
		    		state.setVarValue(lhss, vrhs.intValue());
		    		return  lhss + " = " + vrhs;
		    	}else{
		    		return  lhss + " = " + rhss;
		    	}
	    	}
	    }
	    
	    
	    public Object visitExprArrayInit(ExprArrayInit exp)
	    {
			List intelems = new LinkedList();
			List elems = exp.getElements();
			for (int i=0; i<elems.size(); i++) {
				((Expression)elems.get(i)).accept(this);
				Integer vrhs =  state.popVStack();
		    	assert vrhs != null; 
		    	intelems.add(vrhs);			 	
			}			
			state.vectorPushVStack(intelems);
			//state.pushVStack(null);
			return null;
	    }
	    
	    
	    public Object visitExprArray(ExprArray exp)
	    {
	    	//Assert(false, "NYI");	    	
	    	Assert(exp.getBase() instanceof ExprVar, "Currently only 1 dimensional arrays are supported. \n" + exp.getContext());
	    	ExprVar base = (ExprVar)exp.getBase();
	    	
	    	boolean ilhs = this.isLHS;
	    	this.isLHS = false;
	    	
	    	String vname =  base.getName();	    	
	    	String ofstStr = (String) exp.getOffset().accept(this);
	    	Integer ofst = state.popVStack();
	    	if( ofst != null){
		    	Assert(ofst != null, "The array index must be computable at compile time. \n" + exp.getContext());
		    	int size = state.checkArray(vname);
		    	if(ofst >= size || ofst < 0){
		    		if(this.isLHS){
		    			Assert(false, "ARRAY OUT OF BOUNDS !(0<=" + ofst + " < " + size);
		    			return null;
		    		}else{		    			
		    			if(!exp.isUnchecked())throw new ArrayIndexOutOfBoundsException(exp.getContext() + ": ARRAY OUT OF BOUNDS !(0<=" + ofst + " < " + size);
	    				state.pushVStack( new Integer(0) );
	    				return "0";		    			
		    		}
		    	}
		    	vname = vname + "_idx_" + ofst;
		    	this.isLHS = ilhs;
		    	
		    	if( state.varHasValue( vname) ){
		    		state.pushVStack( new Integer(state.varValue(vname)) );	    		
		    	}else{
		    		state.pushVStack(null);
		    	}
		    	String rval = null;
		    	if(this.isLHS)
		    		rval = vname;
		    	else{		    		
		    		rval =  state.varGetRHSName( vname  );		    		
		    	}
		    	return rval;
	    	}else{
	    		//Assert(ofst != null, "The array index must be computable at compile time. \n" + exp.getContext());
	    		Assert( !this.isLHS, "Array indexing of non-deterministic value is only allowed in the RHS of an assignment; sorrry." );
	    		int arrSize = state.checkArray(vname);
	    		String baseName = vname;
	    		vname = "($ ";
	    		for(int i=0; i< arrSize; ++i ){
	    			if( i!= 0) vname += " ";
	    			String tmpname = baseName + "_idx_" + i;
	    			if(state.varHasValue(tmpname)){
	    				tmpname =  " " + state.varValue(tmpname);
	    			}else{
	    				tmpname = state.varGetRHSName(tmpname);
	    			}
	    			vname = vname + tmpname;
	    		}
	    		vname = vname + "$" +  "[" + ofstStr + "])";	    		
	    		state.pushVStack(null);
	    		return vname;
	    		/*if(this.isLHS)
		    		return vname;
		    	else
		    		return state.varGetRHSName( vname );
		    	*/
	    	}
	    }
	    
		public Object visitExprArrayRange(ExprArrayRange exp) {
			assert false : "At this stage, there shouldn't be any ArrayRange expressions";
			return null;
		}
	    
	    public Object visitExprBinary(ExprBinary exp)
	    {
	        String result;
	        String op = null;
	        result = "(";
	        String lhsStr =(String)exp.getLeft().accept(this); 	        
	        Integer lhs = state.popVStack();	        
	        String rhsStr = (String)exp.getRight().accept(this);
	        Integer rhs = state.popVStack();
	        boolean hasv = lhs != null && rhs != null;
	        if( lhs != null)
	        	lhsStr = lhs.toString();
	        if( rhs != null)
	        	rhsStr = rhs.toString();
	        
	        int newv=0;
	        
	        switch (exp.getOp())
	        {
	        case ExprBinary.BINOP_ADD: op = "+"; if(hasv) newv = lhs.intValue() + rhs.intValue(); break;
	        case ExprBinary.BINOP_SUB: op = "-"; if(hasv) newv = lhs.intValue() - rhs.intValue(); break;
	        case ExprBinary.BINOP_MUL: op = "*"; if(hasv) newv = lhs.intValue() * rhs.intValue(); break;
	        case ExprBinary.BINOP_DIV: op = "/"; if(hasv) newv = lhs.intValue() / rhs.intValue(); break;
	        case ExprBinary.BINOP_MOD: op = "%"; if(hasv) newv = lhs.intValue() % rhs.intValue(); break;
	        case ExprBinary.BINOP_AND: op = "&&"; if(hasv) newv = boolToInt( intToBool(lhs.intValue()) && intToBool(rhs.intValue())); break;
	        case ExprBinary.BINOP_OR:  op = "||"; if(hasv) newv = boolToInt( intToBool(lhs.intValue()) || intToBool(rhs.intValue())); break;
	        case ExprBinary.BINOP_EQ:  op = "=="; if(hasv) newv = boolToInt(lhs.intValue() == rhs.intValue()); break;
	        case ExprBinary.BINOP_NEQ: op = "!="; if(hasv) newv = boolToInt(lhs.intValue() != rhs.intValue()); break;
	        case ExprBinary.BINOP_LT:  op = "<"; if(hasv) newv = boolToInt(lhs.intValue() < rhs.intValue()); break;
	        case ExprBinary.BINOP_LE:  op = "<="; if(hasv) newv = boolToInt(lhs.intValue() <= rhs.intValue()); break;
	        case ExprBinary.BINOP_GT:  op = ">"; if(hasv) newv = boolToInt(lhs.intValue() > rhs.intValue()); break;
	        case ExprBinary.BINOP_GE:  op = ">="; if(hasv) newv = boolToInt(lhs.intValue() >= rhs.intValue()); break;
	        case ExprBinary.BINOP_BAND:op = "&"; if(hasv) newv = boolToInt( intToBool(lhs.intValue()) && intToBool(rhs.intValue())); break;
	        case ExprBinary.BINOP_BOR: op = "|"; if(hasv) newv = boolToInt( intToBool(lhs.intValue()) || intToBool(rhs.intValue()));; break;
	        case ExprBinary.BINOP_BXOR:op = "^"; if(hasv) newv = boolToInt(lhs.intValue() != rhs.intValue()); break;
	        case ExprBinary.BINOP_SELECT:{
	        	op = "{|}";
	        	if(hasv && lhs.intValue() == rhs.intValue()){
	        		newv = lhs.intValue();
	        	}else{
	        		hasv = false;
	        		state.pushVStack(null);
	        		String cvar = state.varDeclare();
	        		oracle.addBinding(exp, cvar);
	        		if(lhs != null && rhs != null){
	        			if(lhs.intValue() == 1){
	        				return "<" + cvar + ">";
	        			}else{
	        				return "! <" + cvar + ">";
	        			}
	        		}
	        		if(rhs == null && lhs == null){	        			
	        			return "( <" + cvar +"> ? " + lhsStr + " : " + rhsStr + ")";  
	        		}
	        		if(rhs == null && lhs != null){
	        			return "( <" + cvar +"> ? " + lhs + " : " + rhsStr + ")";  
	        		}
	        		if(rhs != null && lhs == null){
	        			return "( <" + cvar +"> ? " + lhsStr + " : " + rhs + ")";  
	        		}
	        	}
	        }
	        	
	        }	        
	        result += lhsStr + " " + op + " ";
	        result += rhsStr;
	        result += ")";
	        if(hasv){
	        	state.pushVStack(new Integer(newv));	
	        	result = "" + newv;
	        }else{
	        	state.pushVStack(null);
	        }	        	
	        return result;
	    }

	    public Object visitExprComplex(ExprComplex exp)
	    {
	        // This should cause an assertion failure, actually.
	    	Assert(false, "NYS");
	        String r = "";
	        String i = "";
	        if (exp.getReal() != null) r = (String)exp.getReal().accept(this);
	        if (exp.getImag() != null) i = (String)exp.getImag().accept(this);
	        return "/* (" + r + ")+i(" + i + ") */";
	    }

	    public Object visitExprConstBoolean(ExprConstBoolean exp)
	    {
	        if (exp.getVal()){
	        	state.pushVStack(new Integer(1));
	            return "true";
	        }else{
	        	state.pushVStack(new Integer(0));
	            return "false";
	        }
	    }

	    public Object visitExprConstChar(ExprConstChar exp)
	    {
	    	Assert(false, "NYS");
	        return "'" + exp.getVal() + "'";
	    }

	    public Object visitExprConstFloat(ExprConstFloat exp)
	    {
	    	Assert(false, "NYS");
	        return Double.toString(exp.getVal()) + "f";
	    }

	    public Object visitExprConstInt(ExprConstInt exp)
	    {
	    	state.pushVStack(new Integer(exp.getVal()));
	        return Integer.toString(exp.getVal());
	    }
	    
	    public Object visitExprConstStr(ExprConstStr exp)
	    {
	    	Assert(false, "NYS");
	        return exp.getVal();
	    }

	    public Object visitExprField(ExprField exp)
	    {
	    	Assert(false, "NYS");
	        String result = "";
	        result += (String)exp.getLeft().accept(this);
	        result += ".";
	        result += (String)exp.getName();
	        return result;
	    }

	    public Object visitExprFunCall(ExprFunCall exp)
	    {	    	
	    	String result = " ";
	        String name = exp.getName();
	        // Local function?
	        if (ss.getFuncNamed(name) != null) {
	        	
	        	state.pushLevel();
	        	
	        	Function fun = ss.getFuncNamed(name);	 
	        	{
		        	Iterator actualParams = exp.getParams().iterator();	        		        	       	
		        	Iterator formalParams = fun.getParams().iterator();
		        	result += inParameterSetter(formalParams, actualParams, false);
	        	}
	        	result += "// BEGIN CALL " + fun.getName() + "\n";
	            result += (String) fun.getBody().accept(this);
	            result += "// END CALL " + fun.getName() + "\n";
	            {
	            	Iterator actualParams = exp.getParams().iterator();	        		        	       	
		        	Iterator formalParams = fun.getParams().iterator();
		        	result += outParameterSetter(formalParams, actualParams, false);
	            }
	            state.popLevel();
	        }
		// look for print and println statements; assume everything
		// else is a math function
		else if (name.equals("print")) {
			System.err.println("The StreamBit compiler currently doesn't allow print statements in bit->bit filters.");
		    return "";
		} else if (name.equals("println")) {
		    result = "System.out.println(";
			System.err.println("The StreamBit compiler currently doesn't allow print statements in bit->bit filters.");
		    return "";
	        } else if (name.equals("super")) {
	            result = "";
	        } else if (name.equals("setDelay")) {
	            result = "";
	        } else if (name.startsWith("enqueue")) {	        	
	            result = "";
		} else {
			Assert(false, "The streamBit compiler currently doesn't allow bit->bit filters to call other functions. You are trying to call the function" + name);
		    // Math.sqrt will return a double, but we're only supporting
		    // float's now, so add a cast to float.  Not sure if this is
		    // the right thing to do for all math functions in all cases?
		    result = "(float)Math." + name + "(";
		}	      
	        state.pushVStack(null);
	        return result;
	    }

	    public Object visitExprPeek(ExprPeek exp)
	    {
	    	int poppos = state.varValue("POP_POS");
	        String result = (String)exp.getExpr().accept(this);
	        Integer arg = state.popVStack();
	        state.pushVStack(null);
	        Assert(arg != null, "I can not tell at compile time where you are peeking. " + result);
	        return "INPUT_" + (arg.intValue()+poppos);
	        //return peekFunction(ss.getStreamType()) + "(" + result + ")";
	    }
	    
	    public Object visitExprPop(ExprPop exp)
	    {
	    	int poppos = state.varValue("POP_POS");
	    	state.pushVStack(null);
	    	state.setVarValue("POP_POS", poppos+1);
	    	return "INPUT_" +  poppos;
	        //return popFunction(ss.getStreamType()) + "()";
	    }

	    public Object visitExprTernary(ExprTernary exp)
	    {
	        String a = (String)exp.getA().accept(this);
	        Integer aval = state.popVStack();	        
	        switch (exp.getOp())
	        {
	        case ExprTernary.TEROP_COND:	        	
        		if(aval != null){
        			if( intToBool(aval.intValue()) ){
        				String b = (String)exp.getB().accept(this);
        		        Integer bval = state.popVStack();        		        
        				if(bval != null){
        					state.pushVStack(new Integer(  bval.intValue() ));
        					return bval.toString();
        				}else{
        					state.pushVStack(null);
        					return b;
        				}
        			}else{
        				String c = (String)exp.getC().accept(this);
        		        Integer cval = state.popVStack();
        				if(cval != null){
        					state.pushVStack(new Integer(  cval.intValue() ));
        					return cval.toString();
        				}else{
        					state.pushVStack(null);
        					return c;
        				}
        			}
        		}else{
        			String b = (String)exp.getB().accept(this);
    		        Integer bval = state.popVStack();
    		        String c = (String)exp.getC().accept(this);
    		        Integer cval = state.popVStack();
        			state.pushVStack(null);
        			return "(" + a + " ? " + b + " : " + c + ")";
        		}
	        }
			state.pushVStack(null);
	        return null;
	    }

	    public Object visitExprTypeCast(ExprTypeCast exp)
	    {
	    	//Assert(false, "NYI");
	        //return "((" + convertType(exp.getType()) + ")(" +
	    	//For now, we don't do any casting at all. 
	          return (String)exp.getExpr().accept(this);
	    }

	    public Object visitExprUnary(ExprUnary exp)
	    {
	        String child = (String)exp.getExpr().accept(this);
	        Integer vchild = state.popVStack();
	        boolean hv = vchild != null; 
	        int i=0, j=0;
	        j = (i=i+1);
	        switch(exp.getOp())
	        {
	        case ExprUnary.UNOP_NOT: 
	        	if( hv ){ 
	        		state.pushVStack(new Integer(1-vchild.intValue()));	        		
	        	}else{
	        		state.pushVStack(null);
	        	}
	        return "!" + child;
	        
	        case ExprUnary.UNOP_NEG: 
	        	if( hv ){ 
	        		state.pushVStack(new Integer(-vchild.intValue()));	        		
	        	}else{
	        		state.pushVStack(null);
	        	}
	        return "-" + child;
	        case ExprUnary.UNOP_PREINC:  
	        	if( hv ){ 
	        		this.isLHS = true;
	        		String childb = (String)exp.getExpr().accept(this);
	        		this.isLHS = false;
	        		vchild = state.popVStack();
	        		state.pushVStack(new Integer(vchild.intValue()+1));
	        		state.setVarValue(childb, vchild.intValue()+1 );
	        		return "(" + state.varGetLHSName(childb) + "=" + ( vchild.intValue()+1 ) + ")";
	        	}else{
	        		state.pushVStack(null);
	        	}
	        	return "++" + child;
	        case ExprUnary.UNOP_POSTINC:
	        	if( hv ){ 
	        		this.isLHS = true;
	        		String childb = (String)exp.getExpr().accept(this);
	        		this.isLHS = false;
	        		vchild = state.popVStack();
	        		state.pushVStack(new Integer(vchild.intValue()));
	        		state.setVarValue(childb, vchild.intValue()+1 );
	        		return "ERROR"; // "(" + state.varGetLHSName(childb) + "=" + ( vchild.intValue()+1 ) + ") - 1";
	        	}else{
	        		state.pushVStack(null);
	        	}
	        	return child + "++";
	        case ExprUnary.UNOP_PREDEC:  
	        	if( hv ){ 
	        		this.isLHS = true;
	        		String childb = (String)exp.getExpr().accept(this);
	        		this.isLHS = false;
	        		vchild = state.popVStack();
	        		state.pushVStack(new Integer(vchild.intValue()-1));
	        		state.setVarValue(childb, vchild.intValue()-1 );
	        		return "(" + state.varGetLHSName(childb) + "=" + ( vchild.intValue()-1 ) + ")";
	        	}else{
	        		state.pushVStack(null);
	        	}
	         	return "--" + child;
	        case ExprUnary.UNOP_POSTDEC: 
	        	if( hv ){ 
	        		this.isLHS = true;
	        		String childb = (String)exp.getExpr().accept(this);
	        		this.isLHS = false;
	        		vchild = state.popVStack();
	        		state.pushVStack(new Integer(vchild.intValue()));
	        		state.setVarValue(childb, vchild.intValue()-1 );
	        		return "ERROR"; // "(" + state.varGetLHSName(childb) + "=" + ( vchild.intValue()-1 ) + ") + 1";
	        	}else{
	        		state.pushVStack(null);
	        	}
	        	return child + "--";
	        }
	        return null;
	    }

	    public Object visitExprVar(ExprVar exp)
	    {
	    	String vname =  exp.getName();
	    	Integer intValue;
	    	if( state.varHasValue( vname ) ){
	    		intValue = new Integer(state.varValue(vname)) ;	    		
	    	}else{
	    		intValue = null;
	    	}
	    	int sz = state.checkArray(vname);
	    	if( sz >= 0 ){
	    		List nlist = new LinkedList();
 	    		for(int i=0; i<sz; ++i){
 	    			String lnm = vname + "_idx_" + i;
 	    			if( state.varHasValue( lnm) ){
 	    				nlist.add(new Integer( state.varValue(lnm )));
 	    			}else{
 	    				nlist.add(null);
 	    			}
 	    		}
	    		state.vectorPushVStack( nlist );
	    	}else{
	    		state.pushVStack(intValue);	    		
	    	}
	    	if(this.isLHS)
	    		return exp.getName();
	    	else{
	    		if(sz >=0){
	    			return exp.getName();	
	    		}else{
	    			return state.varGetRHSName( exp.getName() );
	    		}
	    	}
	    }

	    public Object visitFieldDecl(FieldDecl field)
	    {
	        // Assume all of the fields have the same type.
	    	//Assert(false, "We don't allow filters to have state! Sorry.");
	    	//return "//We don't allow filters to have state. \n";
	    	
	        String result = indent + convertType(field.getType(0)) + " ";
	        for (int i = 0; i < field.getNumFields(); ++i)
	        {
	            if (i > 0) result += ", ";
	            this.isLHS = true;
	            String lhs = field.getName(i);
	            this.isLHS = false;	 
	            state.varDeclare(lhs);	 
	            String lhsn = state.varGetLHSName(lhs);
	            if( field.getType(i) instanceof TypeArray ){
	            	TypeArray ta = (TypeArray) field.getType(i);
	            	ta.getLength().accept(this);
	            	Integer tmp = state.popVStack();
	            	Assert(tmp != null, "The array size must be a compile time constant !! \n" + field.getContext());
	            	state.makeArray(lhs, tmp.intValue());
	            	for(int tt=0; tt<tmp.intValue(); ++tt){
	            		String nnm = lhs + "_idx_" + tt;
	            		state.varDeclare(nnm);
	            		String tmplhsn = state.varGetLHSName(nnm);
	            	}
	            }
	         
	            if (field.getInit(i) != null){	
	            	additInit.
					add(new StmtAssign(field.getContext(),
							new ExprVar(field.getContext(), lhs),
							field.getInit(i)));
	    	        /*String rhs =(String)field.getInit(i).accept(this);	    	        
	    	        Integer vrhs = state.popVStack();
	    	        if( vrhs != null){
	    	        	state.setVarValue(lhs, vrhs.intValue());
	    	        	result += "";
	    	        }else{
	    	        	//result += lhs + " = " + rhs;
	    	        } */	                
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
	        		result += "input_RATE = " + pushr.accept(this) + ";\n";
	        		state.popVStack();
	        	}else{
	        		result += "input_RATE = 0;\n";
	        	}
	        	if(popr != null){
	        		result += "output_RATE = " + popr.accept(this) + ";\n";
	        		state.popVStack();
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
	        	state.pushVStack(null);
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
                	if(this.state.topOfStackIsVector()){
                		List l = state.vectorPopVStack();
                		int xx=0;
                		int xpon=1;                		                		
                		for(Iterator it = l.iterator(); it.hasNext(); ){
                			Integer i = (Integer)it.next();
                			xx += xpon *  i.intValue();
                			xpon = xpon * 3;
                		}
                		fullnm +=  xx;
                	}else{
                		Integer pv = state.popVStack();
                		assert pv != null: "This is not supposed to happen";
                		fullnm += pv;
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

        String inParameterSetter(Iterator formalParamIterator, Iterator actualParamIterator, boolean checkError){
        	String result = "";
	        while(actualParamIterator.hasNext()){	        	
	        	Expression actualParam = (Expression)actualParamIterator.next();			        	
	        	Parameter formalParam = (Parameter) formalParamIterator.next();
	        	
	        	//this.state.markVectorStack();
	        	
	        	String apnm = (String) actualParam.accept(this);
	        	
	        	
	    		
	        	if( this.state.topOfStackIsVector() ){	
	        		List lst= state.vectorPopVStack();
	        		
	        		List<String> rhsNames = new LinkedList<String>();
	        		for(int i=0; i<lst.size(); ++i){
	        			rhsNames.add(state.varGetRHSName(apnm + "_idx_" + i));
	        		}
	        		
	        		String formalParamName = formalParam.getName();
		        	state.varDeclare(formalParamName);
		    		String lhsname = state.varGetLHSName(formalParamName);
		    		
		    		
		    		Iterator it = lst.iterator();
		    		Iterator<String> rhsNamesIter = rhsNames.iterator();
		    		int idx = 0;
		    		state.makeArray(formalParamName, lst.size());
		    		while( it.hasNext() ){
		    			Integer i = (Integer) it.next();
		    			String lpnm = formalParamName + "_idx_" + idx;
		    			state.varDeclare(lpnm);
			    		lhsname = state.varGetLHSName(lpnm);
			    		if( !formalParam.isParameterOutput() ){
				    		if(i == null){
				    			result += lhsname + " = " + rhsNamesIter.next() + ";\n";
				    		}else{
				    			state.setVarValue(lpnm, i.intValue());
				    		}
			    		}
		    			++idx;
		    		}
		    	}else{
		    		String formalParamName = formalParam.getName();
		        	state.varDeclare(formalParamName);
		    		String lhsname = state.varGetLHSName(formalParamName);
		    		Integer value = state.popVStack();
		    		Assert(value != null || !checkError, "I must be able to determine the values of the parameters at compile time.");
		    		if( !formalParam.isParameterOutput() ){
			    		if(value == null){
			    			result += lhsname + " = " + apnm + ";\n";
			    		}else{
			    			state.setVarValue(formalParamName, value.intValue());
			    		}
		    		}
		    	}
	        }
	        return result;
        }
        
        
        String outParameterSetter(Iterator formalParamIterator, Iterator actualParamIterator, boolean checkError){
        	String result = "";
	        while(actualParamIterator.hasNext()){	        	
	        	Expression actualParam = (Expression)actualParamIterator.next();			        	
	        	Parameter formalParam = (Parameter) formalParamIterator.next();
	        	
	        	//this.state.markVectorStack();
	        	this.isLHS = true;
	        	String apnm = (String) actualParam.accept(this);
	        	this.isLHS = false;
	        	
	        	String formalParamName = formalParam.getName();	        	
	    		
	        	if( this.state.topOfStackIsVector() ){				        
		    		List lst= state.vectorPopVStack();
		    		Iterator it = lst.iterator();
		    		int idx = 0;		    		
		    		while( it.hasNext() ){
		    			Integer i = (Integer) it.next();
		    			String formalName = formalParamName + "_idx_" + idx;		    						    		
			    		if( formalParam.isParameterOutput() ){
			    			String rhsname = state.varGetRHSName(formalName);
				    		if(i == null){
				    			result += state.varGetLHSName(apnm+"_idx_"+idx) + " = " + rhsname + ";\n";
				    		}else{
				    			state.setVarValue(formalName, i.intValue());
				    		}
			    		}
		    			++idx;
		    		}
		    	}else{
		    		String lhsname = state.varGetLHSName(apnm);
		    		Integer value = state.popVStack();
		    		Assert(value != null || !checkError, "I must be able to determine the values of the parameters at compile time.");
		    		if( formalParam.isParameterOutput() ){
			    		if(value == null){
			    			result += lhsname + " = " + state.varGetRHSName(formalParamName) + ";\n";
			    		}else{
			    			state.setVarValue(formalParamName, value.intValue());
			    		}
		    		}
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
	        result += ";\n" + indent + how + "(" + tempVar + ")";
	        for (Iterator iter = portals.iterator(); iter.hasNext(); )
	        {
	            Expression portal = (Expression)iter.next();
	            result += ";\n" + indent + (String)portal.accept(this) +
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
	        
	        Integer vrhs = null;
	        String rhs = (String)stmt.getRHS().accept(this);
	        	        
	        List rhsLst = null;
	        if( this.state.topOfStackIsVector() ){
	        	rhsLst= state.vectorPopVStack();
	        }else{
	        	vrhs = state.popVStack();		        
		        if(vrhs != null){
		        	rhs = vrhs.toString();
		        }		        	
	        }
	        
	        
	        
	        
	        this.isLHS = true;
	        String lhs = (String)stmt.getLHS().accept(this);
	        this.isLHS = false;
	        String lhsnm = state.varGetLHSName(lhs);
	        
	        int arrSize = state.checkArray(lhs);
	        boolean isArr = arrSize > 0;
	        
	        Integer vlhs = null;
	        String rlhs = null;  	        

	        
	        if(isArr && this.state.topOfStackIsVector() ){	        	
	        	state.vectorPopVStack();	
	        }else{
	        	assert isArr == this.state.topOfStackIsVector();
	        	vlhs = state.popVStack();
		        rlhs = lhs;  
		        if(vlhs != null){
		        	rlhs = vlhs.toString();
		        }else{
		        	rlhs = state.varGetRHSName(rlhs);
		        }	        	
	        }
	        
	        
	        
	        
	        boolean hv = vlhs != null && vrhs != null;
	        
	        	
	        
	        
	        switch(stmt.getOp())
	        {
	        case ExprBinary.BINOP_ADD: 	        	
	        	op = " = " + rlhs + "+";
	        	assert !isArr : "Operation not yet defined for arrays:" + op;
	        	if(hv){
	        		state.setVarValue(lhs, vlhs.intValue() + vrhs.intValue());
	        	}
	        break;
	        case ExprBinary.BINOP_SUB: 
	        	op = " = "+ rlhs + "-";
	        	assert !isArr : "Operation not yet defined for arrays:" + op;
		        if(hv){
	        		state.setVarValue(lhs, vlhs.intValue() - vrhs.intValue());
	        	}
	        break;
	        case ExprBinary.BINOP_MUL: 
	        	op = " = "+ rlhs + "*";
	        	assert !isArr : "Operation not yet defined for arrays:" + op;
		        if(hv){
	        		state.setVarValue(lhs, vlhs.intValue() * vrhs.intValue());
	        	}
	        break;
	        case ExprBinary.BINOP_DIV: 
	        	op = " = "+ rlhs + "/"; 
	        	assert !isArr : "Operation not yet defined for arrays:" + op;
		        if(hv){
	        		state.setVarValue(lhs, vlhs.intValue() / vrhs.intValue());
	        	}
	        break;
	        default: op = " = ";
		        if( rhsLst != null ){
		        	assert isArr: "This should not happen, you are trying to assign an array to a non-array";
		    		List lst= rhsLst;
		    		Iterator it = lst.iterator();
		    		int idx = 0;
		    		while( it.hasNext() ){
		    			Integer i = (Integer) it.next();
		    			state.setVarValue(lhs + "_idx_" + idx, i.intValue());
		    			++idx;
		    		}
		    		return "";
		    	}else if(vrhs != null){
	        		state.setVarValue(lhs, vrhs.intValue());	
	        		return "";
	        	}
	        }
	        // Assume both sides are the right type.
	        if(hv) 
	        	return "";
	        else
	        	state.unsetVarValue(lhs);
	        return lhsnm + op + rhs;
	    }

	    public Object visitStmtBlock(StmtBlock stmt)
	    {
	        // Put context label at the start of the block, too.
	    	state.pushLevel();
	        String result = "// {";
	        if (stmt.getContext() != null)
	            result += " \t\t\t// " + stmt.getContext();
	        result += "\n";
	        addIndent();
	        for (Iterator iter = stmt.getStmts().iterator(); iter.hasNext(); )
	        {
	            Statement s = (Statement)iter.next();
	            String line = indent;
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
	        unIndent();
	        result += indent + " // }\n";
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
	        String result = (String)stmt.getExpression().accept(this);
	        Integer tmp = state.popVStack();
	        // Gross hack to strip out leading class casts,
	        // since they'll illegal (JLS 14.8).
	        if (result.length() > 0 && (result.charAt(0) == '(' &&
	            Character.isUpperCase(result.charAt(1))))
	            result = result.substring(result.indexOf(')') + 1);
	        return result;
	    }

	    public Object visitStmtFor(StmtFor stmt)
	    {
	    	state.pushLevel();
	        String init = "";
	        String result = "";
	        if (stmt.getInit() != null)
	            init = (String)stmt.getInit().accept(this);	        
	        String cond;
	        Assert( stmt.getCond() != null , "For now, the condition in your for loop can't be null");
	        cond = (String)stmt.getCond().accept(this);
	        Integer vcond = state.popVStack();
	        int iters = 0;
	        while(vcond != null && vcond.intValue() > 0){
	        	++iters;
	        	result += (String)stmt.getBody().accept(this);
	        	String incr;
	        	if (stmt.getIncr() != null)
		        	incr = (String)stmt.getIncr().accept(this);
	        	cond = (String)stmt.getCond().accept(this);
		        vcond = state.popVStack();
		        Assert(iters < 300, "This is probably a bug, why would it go around so many times?");
	        }
	        state.popLevel();
	        return result;
	    }

	    public Object visitStmtIfThen(StmtIfThen stmt)
	    {
	        // must have an if part...
	    	
	        String result = "";
	        String cond = (String)stmt.getCond().accept(this);
	        Integer vcond = state.popVStack();
	        if(vcond != null){
	        	if(vcond.intValue() > 0){
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
	        	result += state.procChangeTrackers(ipms, epms, cond);
	        }else{
	        	result = ipart;	        	
	        	result += state.procChangeTrackers(ipms, cond);
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
	    	String iter =  (String) stmt.getIter().accept(this);
	    	Integer vcond = state.popVStack();
	    	if(vcond == null){
	    		
	    		String nvar = state.varDeclare(); 
	    		result += nvar + " = " + "(" + iter + ");\n"; 
	    		int LUNROLL=8;
	    		int iters;
	    		for(iters=0; iters<LUNROLL; ++iters){			        		        
			        state.pushChangeTracker();
			        String ipart = "";
			        try{
			        	
			        	ipart = (String)stmt.getBody().accept(this);
			        }catch(ArrayIndexOutOfBoundsException er){			        	
			        	state.popChangeTracker();
			        	break;
		    		}
			        result += ipart;
	    		}
	    		
	    		for(int i=iters-1; i>=0; --i){
	    			String cond = "(" + nvar + ")>" + i;
	    			ChangeStack ipms = state.popChangeTracker();	
	    			result += state.procChangeTrackers(ipms, cond);
	    		}
		        return result;
	    	}else{
	    		for(int i=0; i<vcond.intValue(); ++i){
	    			result += (String)stmt.getBody().accept(this);
	    		}
	    		
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
	    	Integer ival = state.popVStack();
	    	 
	    	int pos = state.varValue("PUSH_POS");
	    	state.setVarValue("PUSH_POS", pos+1);
	    	String otpt = "OUTPUT_" + pos;	    	
			String lhs =  state.varGetLHSName(otpt);
	    	if(ival != null){
	    		state.setVarValue(otpt, ival.intValue());
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
	        
	        result += ";\n" + indent + receiver + "." + stmt.getName() + "(";
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
	            	Integer tmp = state.popVStack();
	            	Assert(tmp != null, "The array size must be a compile time constant !! \n" + stmt.getContext());
	            	state.makeArray(nm, tmp.intValue());
	            	//this.state.markVectorStack();
	            	if( stmt.getInit(i) != null){
		            	stmt.getInit(i).accept(this);	            	
		            	if( this.state.topOfStackIsVector() ){
				    		List lst= state.vectorPopVStack();
				    		Iterator it = lst.iterator();
				    		int tt = 0;
				    		while( it.hasNext() ){
				    			Integer ival = (Integer) it.next();
				    			String nnm = nm + "_idx_" + tt;
				    			state.varDeclare(nnm);
			            		String tmplhsn = state.varGetLHSName(nnm);
				    			state.setVarValue(nnm, ival.intValue());
				    			++tt;
				    		}
				    		return "";
				    	}else{
				    		Integer val = state.popVStack();
			            	for(int tt=0; tt<tmp.intValue(); ++tt){
			            		String nnm = nm + "_idx_" + tt;
			            		state.varDeclare(nnm);
			            		String tmplhsn = state.varGetLHSName(nnm);
			            		if(val != null){
			            			state.setVarValue(tmplhsn, val.intValue());
			            		}
			            	}
				    	}
	            	}else{
	            		for(int tt=0; tt<tmp.intValue(); ++tt){
		            		String nnm = nm + "_idx_" + tt;
		            		state.varDeclare(nnm);
		            		String tmplhsn = state.varGetLHSName(nnm);		            		
		            	}
	            	}
	            }else{
		            
		            if (stmt.getInit(i) != null){      	
		                 String asgn = lhsn + " = " + (String)stmt.getInit(i).accept(this) + "; \n";
		                Integer tmp = state.popVStack();
		                if(tmp != null){
		                	state.setVarValue(nm, tmp.intValue());
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

	    /**
	     * For a non-anonymous StreamSpec, check to see if it has any
	     * message handlers.  If it does, then generate a Java interface
	     * containing the handlers named (StreamName)Interface, and
	     * a portal class named (StreamName)Portal.
	     */
	    private String maybeGeneratePortal(StreamSpec spec)
	    {
	        List handlers = new java.util.ArrayList();
	        for (Iterator iter = spec.getFuncs().iterator(); iter.hasNext(); )
	        {
	            Function func = (Function)iter.next();
	            if (func.getCls() == Function.FUNC_HANDLER)
	                handlers.add(func);
	        }
	        if (handlers.isEmpty())
	            return null;
	        
	        // Okay.  Assemble the interface:
	        StringBuffer result = new StringBuffer();
	        result.append(indent + "interface " + spec.getName() +
	                      "Interface {\n");
	        addIndent();
	        for (Iterator iter = handlers.iterator(); iter.hasNext(); )
	        {
	            Function func = (Function)iter.next();
	            result.append(indent + "public ");
	            result.append(convertType(func.getReturnType()) + " ");
	            result.append(func.getName());
	            result.append(doParams(func.getParams(), null));
	            result.append(";\n");
	        }
	        unIndent();
	        result.append(indent + "}\n");
	        
	        // Assemble the portal:
	        result.append(indent + "class " + spec.getName() +
	                      "Portal extends Portal implements " + spec.getName() +
	                      "Interface {\n");
	        addIndent();
	        for (Iterator iter = handlers.iterator(); iter.hasNext(); )
	        {
	            Function func = (Function)iter.next();
	            result.append(indent + "public ");
	            result.append(convertType(func.getReturnType()) + " ");
	            result.append(func.getName());
	            result.append(doParams(func.getParams(), null));
	            result.append(" { }\n");
	        }
	        unIndent();
	        result.append(indent + "}\n");

	        return result.toString();
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
	            result += "() \n" + indent;
	            addIndent();
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
	        
	        
	        additInit = new LinkedList();
	        
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
	        unIndent();
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
	            state.pushVStack(null);
	            return makeConstructor(jc.getType());
	        }
	        if (node instanceof StmtIODecl)
	        {
	            StmtIODecl io = (StmtIODecl)node;
	            String result = "";
	            if (io.getRate1() != null && false){
	            	result += io.getName() + "_RATE="+
	                (String)io.getRate1().accept(this) + ";\n";
	            	Integer iv = state.popVStack();
	            	Assert( iv != null, "The compiler must be able to determine the IO rate at compile time. \n" + io.getContext() );
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
			state.pushVStack(null);
			String cvar = state.varDeclare();
			oracle.addBinding(star, cvar);
			if(star.getSize() > 1)
				return "<" + cvar + "  " + star.getSize() + ">";
			else
				return "<" + cvar + ">";
		}
}
