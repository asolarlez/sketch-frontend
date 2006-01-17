/*
 * Created on Jun 22, 2004
 *
 * 
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package streamit.frontend.tosbit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import streamit.frontend.nodes.ExprArray;
import streamit.frontend.nodes.ExprArrayInit;
import streamit.frontend.nodes.ExprArrayRange;
import streamit.frontend.nodes.ExprBinary;
import streamit.frontend.nodes.ExprComplex;
import streamit.frontend.nodes.ExprConstBoolean;
import streamit.frontend.nodes.ExprConstChar;
import streamit.frontend.nodes.ExprConstFloat;
import streamit.frontend.nodes.ExprConstInt;
import streamit.frontend.nodes.ExprConstStr;
import streamit.frontend.nodes.ExprField;
import streamit.frontend.nodes.ExprFunCall;
import streamit.frontend.nodes.ExprPeek;
import streamit.frontend.nodes.ExprPop;
import streamit.frontend.nodes.ExprStar;
import streamit.frontend.nodes.ExprTernary;
import streamit.frontend.nodes.ExprTypeCast;
import streamit.frontend.nodes.ExprUnary;
import streamit.frontend.nodes.ExprVar;
import streamit.frontend.nodes.Expression;
import streamit.frontend.nodes.FENode;
import streamit.frontend.nodes.FENullVisitor;
import streamit.frontend.nodes.FEVisitor;
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
import streamit.frontend.nodes.SymbolTable;
import streamit.frontend.nodes.TempVarGen;
import streamit.frontend.nodes.Type;
import streamit.frontend.nodes.TypeArray;
import streamit.frontend.nodes.TypePortal;
import streamit.frontend.nodes.TypePrimitive;
import streamit.frontend.nodes.TypeStruct;
import streamit.frontend.nodes.TypeStructRef;
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
	    //private boolean isLHS;
	    private NodesToNative nativeGenerator;
	    private HashMap funsWParams;
	    protected Stack preFil;
	    protected List additInit;
	    private ValueOracle oracle;
	    public int LUNROLL=8;
	    
	    public class LHSvisitor extends FENullVisitor{
	    	public Object visitExprArray(ExprArray exp)
	 	    {
	 	    	Assert(exp.getBase() instanceof ExprVar, "Currently only 1 dimensional arrays are supported. \n" + exp.getContext());
	 	    	ExprVar base = (ExprVar)exp.getBase();	 	    		 	    	 	    		 	    	
	 	    	String vname =  base.getName();
	 	    	exp.getOffset().accept(NodesToSBit.this);
	 	    	valueClass ofst = state.popVStack();
	 	    	if( ofst.hasValue()){
	 		    	int ofstV = ofst.getIntValue();
	 		    	int size = state.checkArray(vname);
	 		    	if(ofstV >= size || ofstV < 0){	 		    		
	 		    		Assert(false, "ARRAY OUT OF BOUNDS !(0<=" + ofstV + " < " + size);
	 		    		return null;
	 		    	}
	 		    	vname = vname + "_idx_" + ofstV;
	 		    	if( state.varHasValue(vname) ){
	 		    		state.pushVStack( new valueClass(state.varValue(vname)) );	    		
	 		    	}else{
	 		    		state.pushVStack( new valueClass(vname) );
	 		    	}
	 		    	String rval = vname;	 		    	
	 		    	return rval;
	 	    	}else{
	 	    		Assert( false, "Array indexing of non-deterministic value is only allowed in the RHS of an assignment; sorrry." );	 	    	
	 	    	}
	 	    	return null;
	 	    }	
		    public Object visitExprVar(ExprVar exp)
		    {
		    	String vname =  exp.getName();
		    	valueClass intValue;
		    	if( state.varHasValue( vname ) ){
		    		intValue = new valueClass(state.varValue(vname)) ;	    		
		    	}else{
		    		intValue = new valueClass(vname);
		    	}
		    	int sz = state.checkArray(vname);
		    	if( sz >= 0 ){
		    		List<valueClass> nlist = new LinkedList<valueClass>();
	 	    		for(int i=0; i<sz; ++i){
	 	    			String lnm = vname + "_idx_" + i;
	 	    			if( state.varHasValue( lnm) ){
	 	    				nlist.add(new valueClass( state.varValue(lnm )));
	 	    			}else{
	 	    				nlist.add(new valueClass(lnm));
	 	    			}
	 	    		}
		    		state.pushVStack( new valueClass(nlist) );
		    	}else{
		    		state.pushVStack(intValue);
		    	}
		    	return exp.getName();		    	
		    }	    	
	    }
	    
	    
	    
	    
	    
	    
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
	            int i = state.popVStack().getIntValue();	            
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
	            	valueClass tmp = state.popVStack();
	            	Assert(tmp.hasValue(), "The array size must be a compile time constant !! \n" );
	            	state.makeArray(lhs, tmp.getIntValue());
	            	for(int tt=0; tt<tmp.getIntValue(); ++tt){
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
	    	
	    	String lhss = (String) lhs.accept(new LHSvisitor());
	    		    	
	    	lhss = state.varGetLHSName(lhss);
	    	valueClass vlhs = state.popVStack();

	    	String rhss = (String) rhs.accept(this);
	    	valueClass rhsVal = state.popVStack();
	    	if( rhsVal.isVect() ){
	    		List<valueClass> lst= rhsVal.getVectValue();
	    		Iterator<valueClass> it = lst.iterator();
	    		int idx = 0;
	    		while( it.hasNext() ){
	    			valueClass i = it.next();
	    			state.setVarValue(lhss + "_idx_" + idx, i.getIntValue());
	    			++idx;
	    		}
	    		return "";
	    	}else{
	    		valueClass vrhs =  rhsVal;
		    	if(vrhs.hasValue()){
		    		state.setVarValue(lhss, vrhs.getIntValue());
		    		return  lhss + " = " + vrhs;
		    	}else{
		    		return  lhss + " = " + rhss;
		    	}
	    	}
	    }
	    
	    
	    public Object visitExprArrayInit(ExprArrayInit exp)
	    {
			List<valueClass> intelems = new LinkedList<valueClass>();
			List elems = exp.getElements();
			for (int i=0; i<elems.size(); i++) {
				((Expression)elems.get(i)).accept(this);
				valueClass vrhs =  state.popVStack();
		    	assert vrhs.hasValue(); 
		    	intelems.add(vrhs);
			}			
			state.pushVStack(new valueClass(intelems));
			//state.pushVStack(null);
			return null;
	    }
	    
	    
	    public Object visitExprArray(ExprArray exp)
	    {
	    	//Assert(false, "NYI");	    	
	    	Assert(exp.getBase() instanceof ExprVar, "Currently only 1 dimensional arrays are supported. \n" + exp.getContext());
	    	ExprVar base = (ExprVar)exp.getBase();	    		    		    	
	    	
	    	String vname =  base.getName();	    	
	    	String ofstStr = (String) exp.getOffset().accept(this);
	    	valueClass ofst = state.popVStack();
	    	if( ofst.hasValue()){
		    	Assert(ofst != null, "The array index must be computable at compile time. \n" + exp.getContext());
		    	int ofstV = ofst.getIntValue();
		    	int size = state.checkArray(vname);
		    	if(ofstV >= size || ofstV < 0){		    		
		    		if(!exp.isUnchecked())throw new ArrayIndexOutOfBoundsException(exp.getContext() + ": ARRAY OUT OF BOUNDS !(0<=" + ofst.getIntValue() + " < " + size);
	    			state.pushVStack( new valueClass(0) );
	    			return "0";
		    	}
		    	vname = vname + "_idx_" + ofstV;		    	
		    	String rval = state.varGetRHSName( vname  );
		    	
		    	if( state.varHasValue( vname) ){
		    		state.pushVStack( new valueClass(state.varValue(vname)) );	    		
		    	}else{
		    		state.pushVStack( new valueClass(rval) );
		    	}
		    			    	
		    	return rval;
	    	}else{
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
	    		state.pushVStack( new valueClass(vname));
	    		return vname;
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
	        valueClass lhs = state.popVStack();	        
	        String rhsStr = (String)exp.getRight().accept(this);
	        valueClass rhs = state.popVStack();
	        boolean hasv = lhs.hasValue() && rhs.hasValue();
	        if( lhs.hasValue())
	        	lhsStr = ""+ lhs.getIntValue();
	        if( rhs.hasValue())
	        	rhsStr = ""+ rhs.getIntValue();
	        
	        int newv=0;
	        
	        switch (exp.getOp())
	        {
	        case ExprBinary.BINOP_ADD: op = "+"; if(hasv) newv = lhs.getIntValue() + rhs.getIntValue(); break;
	        case ExprBinary.BINOP_SUB: op = "-"; if(hasv) newv = lhs.getIntValue() - rhs.getIntValue(); break;
	        case ExprBinary.BINOP_MUL: op = "*"; if(hasv) newv = lhs.getIntValue() * rhs.getIntValue(); break;
	        case ExprBinary.BINOP_DIV: op = "/"; if(hasv) newv = lhs.getIntValue() / rhs.getIntValue(); break;
	        case ExprBinary.BINOP_MOD: op = "%"; if(hasv) newv = lhs.getIntValue() % rhs.getIntValue(); break;
	        case ExprBinary.BINOP_AND: op = "&&"; if(hasv) newv = boolToInt( intToBool(lhs.getIntValue()) && intToBool(rhs.getIntValue())); break;
	        case ExprBinary.BINOP_OR:  op = "||"; if(hasv) newv = boolToInt( intToBool(lhs.getIntValue()) || intToBool(rhs.getIntValue())); break;
	        case ExprBinary.BINOP_EQ:  op = "=="; if(hasv) newv = boolToInt(lhs.getIntValue() == rhs.getIntValue()); break;
	        case ExprBinary.BINOP_NEQ: op = "!="; if(hasv) newv = boolToInt(lhs.getIntValue() != rhs.getIntValue()); break;
	        case ExprBinary.BINOP_LT:  op = "<"; if(hasv) newv = boolToInt(lhs.getIntValue() < rhs.getIntValue()); break;
	        case ExprBinary.BINOP_LE:  op = "<="; if(hasv) newv = boolToInt(lhs.getIntValue() <= rhs.getIntValue()); break;
	        case ExprBinary.BINOP_GT:  op = ">"; if(hasv) newv = boolToInt(lhs.getIntValue() > rhs.getIntValue()); break;
	        case ExprBinary.BINOP_GE:  op = ">="; if(hasv) newv = boolToInt(lhs.getIntValue() >= rhs.getIntValue()); break;
	        case ExprBinary.BINOP_BAND:op = "&"; if(hasv) newv = boolToInt( intToBool(lhs.getIntValue()) && intToBool(rhs.getIntValue())); break;
	        case ExprBinary.BINOP_BOR: op = "|"; if(hasv) newv = boolToInt( intToBool(lhs.getIntValue()) || intToBool(rhs.getIntValue()));; break;
	        case ExprBinary.BINOP_BXOR:op = "^"; if(hasv) newv = boolToInt(lhs.getIntValue() != rhs.getIntValue()); break;
	        case ExprBinary.BINOP_SELECT:{
	        	op = "{|}";
	        	if(hasv && lhs.getIntValue() == rhs.getIntValue()){
	        		newv = lhs.getIntValue();
	        	}else{
	        		hasv = false;
	        		String rval = null;
	        		String cvar = state.varDeclare();
	        		oracle.addBinding(exp, cvar);
	        		if(lhs.hasValue() && rhs.hasValue()){
	        			if(lhs.getIntValue() == 1){
	        				rval =  "<" + cvar + ">";
	        			}else{
	        				rval =  "! <" + cvar + ">";
	        			}
	        		}
	        		if(!rhs.hasValue() && !lhs.hasValue()){	        			
	        			rval =  "( <" + cvar +"> ? " + lhsStr + " : " + rhsStr + ")";  
	        		}
	        		if(!rhs.hasValue() && lhs.hasValue()){
	        			rval =  "( <" + cvar +"> ? " + lhs + " : " + rhsStr + ")";  
	        		}
	        		if(rhs.hasValue() && !lhs.hasValue()){
	        			rval =  "( <" + cvar +"> ? " + lhsStr + " : " + rhs + ")";  
	        		}
	        		state.pushVStack(new valueClass(rval));
	        		return rval;
	        	}
	        }

	        }	        
	        result += lhsStr + " " + op + " ";
	        result += rhsStr;
	        result += ")";
	        if(hasv){
	        	state.pushVStack(new valueClass(newv));	
	        	result = "" + newv;
	        }else{
	        	state.pushVStack(new valueClass(result));
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
	        	state.pushVStack(new valueClass(1));
	            return "true";
	        }else{
	        	state.pushVStack(new valueClass(0));
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
	    	state.pushVStack(new valueClass(exp.getVal()));
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
	        state.pushVStack( new valueClass(result) );
	        return result;
	    }

	    public Object visitExprPeek(ExprPeek exp)
	    {
	    	int poppos = state.varValue("POP_POS");
	        String result = (String)exp.getExpr().accept(this);
	        valueClass arg = state.popVStack();	        
	        Assert(arg.hasValue(), "I can not tell at compile time where you are peeking. " + result);
	        result = "INPUT_" + (arg.getIntValue()+poppos);
	        state.pushVStack(new valueClass(result));
	        return result;
	        //return peekFunction(ss.getStreamType()) + "(" + result + ")";
	    }
	    
	    public Object visitExprPop(ExprPop exp)
	    {
	    	int poppos = state.varValue("POP_POS");
	    	String result = "INPUT_" +  poppos; 
	    	state.setVarValue("POP_POS", poppos+1);
	    	state.pushVStack(new valueClass(result));
	    	return result;
	        //return popFunction(ss.getStreamType()) + "()";
	    }

	    public Object visitExprTernary(ExprTernary exp)
	    {
	        String a = (String)exp.getA().accept(this);
	        valueClass aval = state.popVStack();	        
	        switch (exp.getOp())
	        {
	        case ExprTernary.TEROP_COND:	        	
        		if(aval.hasValue()){
        			if( intToBool(aval.getIntValue()) ){
        				String b = (String)exp.getB().accept(this);
        				valueClass bval = state.popVStack();        		        
        				if(bval.hasValue()){
        					state.pushVStack(new valueClass(  bval.getIntValue() ));
        					return bval.toString();
        				}else{
        					state.pushVStack(new valueClass(b));
        					return b;
        				}
        			}else{
        				String c = (String)exp.getC().accept(this);
        				valueClass cval = state.popVStack();
        				if(cval.hasValue()){
        					state.pushVStack(new valueClass(  cval.getIntValue() ));
        					return cval.toString();
        				}else{
        					state.pushVStack( new valueClass(c) );
        					return c;
        				}
        			}
        		}else{
        			String b = (String)exp.getB().accept(this);
    		        valueClass bval = state.popVStack();
    		        String c = (String)exp.getC().accept(this);
    		        valueClass cval = state.popVStack();        			
        			String rval = "(" + a + " ? " + b + " : " + c + ")";
        			state.pushVStack( new valueClass(rval) );
        			return rval;
        		}
	        }
			state.pushVStack(new valueClass((String)null));
	        return null;
	    }

	    public Object visitExprTypeCast(ExprTypeCast exp)
	    {
	    		    		    	
	    	if(! exp.getType().equals(TypePrimitive.inttype) ){
	    		Assert( exp.getType() instanceof TypeArray, "WHAT ARE YOU TRYING TO DO!!!");
	    		return (String)exp.getExpr().accept(this);
	    	}
	    	
	    	String arrName = (String)exp.getExpr().accept(this);
	    	valueClass rhsVal = state.popVStack();
	    	if( rhsVal.isVect() ){
	    		String result = "( $$";
	    		List<valueClass> rhsLst;
	        	rhsLst= rhsVal.getVectValue();
	        	int size = state.checkArray(arrName);
	        	Assert(size == rhsLst.size(), "I don't exist");
	        	Iterator<valueClass> it = rhsLst.iterator();
	        	int i = 0;
	        	int val=0;
	        	boolean hasValue=true;
	        	while(it.hasNext()){
	        		valueClass o = it.next();
	        		if(!o.hasValue()){
	        			String lnm = arrName + "_idx_" + i;
	        			result += " " + state.varGetRHSName(lnm);
	        			hasValue = false;
	        		}else{	        			
	        			int curv =  o.getIntValue();
	        			result += " " + o.getIntValue();
	        			Assert(curv == 1 || curv == 0, "Only boolean arrays please!!");
	        			val = val*2;
	        			val = val + curv;
	        		}
	        		++i;
	        	}
	        	result += " $$ )";
	        	if(hasValue){
	        		state.pushVStack(new valueClass(val));
	        		result = " " + val;
	        	}else{
	        		state.pushVStack(new valueClass(result));
	        	}
	        	return result;
	        }else{	        	
	        	Assert(false, "We only allow casting of array expressions");
	        }
	        return null;
	    }

	    public Object visitExprUnary(ExprUnary exp)
	    {
	        String child = (String)exp.getExpr().accept(this);
	        valueClass vchild = state.popVStack();
	        boolean hv = vchild.hasValue(); 
	        int i=0, j=0;
	        j = (i=i+1);
	        switch(exp.getOp())
	        {
	        case ExprUnary.UNOP_NOT: 
	        	if( hv ){ 
	        		state.pushVStack(new valueClass(1-vchild.getIntValue()));	        		
	        	}else{
	        		state.pushVStack( new valueClass("!" + child) );
	        	}
	        return "!" + child;
	        
	        case ExprUnary.UNOP_NEG: 
	        	if( hv ){ 
	        		state.pushVStack(new valueClass(-vchild.getIntValue()));	        		
	        	}else{
	        		state.pushVStack(new valueClass("-" + child));
	        	}
	        return "-" + child;
	        case ExprUnary.UNOP_PREINC:  
	        	if( hv ){ 	        		
	        		String childb = (String)exp.getExpr().accept( new LHSvisitor());	        		
	        		vchild = state.popVStack();
	        		state.pushVStack(new valueClass(vchild.getIntValue()+1));
	        		state.setVarValue(childb, vchild.getIntValue()+1 );
	        		return "(" + state.varGetLHSName(childb) + "=" + ( vchild.getIntValue()+1 ) + ")";
	        	}else{
	        		state.pushVStack(new valueClass("++" + child));
	        	}
	        	return "++" + child;
	        case ExprUnary.UNOP_POSTINC:
	        	if( hv ){ 	        		
	        		String childb = (String)exp.getExpr().accept( new LHSvisitor());	        		
	        		vchild = state.popVStack();
	        		state.pushVStack(new valueClass(vchild.getIntValue()));
	        		state.setVarValue(childb, vchild.getIntValue()+1 );
	        		return "ERROR"; // "(" + state.varGetLHSName(childb) + "=" + ( vchild.intValue()+1 ) + ") - 1";
	        	}else{
	        		state.pushVStack(new valueClass(child + "++"));
	        	}
	        	return child + "++";
	        case ExprUnary.UNOP_PREDEC:  
	        	if( hv ){
	        		String childb = (String)exp.getExpr().accept( new LHSvisitor());
	        		vchild = state.popVStack();
	        		state.pushVStack(new valueClass(vchild.getIntValue()-1));
	        		state.setVarValue(childb, vchild.getIntValue()-1 );
	        		return "(" + state.varGetLHSName(childb) + "=" + ( vchild.getIntValue()-1 ) + ")";
	        	}else{
	        		state.pushVStack( new valueClass("--" + child) );
	        	}
	         	return "--" + child;
	        case ExprUnary.UNOP_POSTDEC: 
	        	if( hv ){ 
	        		String childb = (String)exp.getExpr().accept( new LHSvisitor() );
	        		vchild = state.popVStack();
	        		state.pushVStack(new valueClass(vchild.getIntValue()));
	        		state.setVarValue(childb, vchild.getIntValue()-1 );
	        		return "ERROR"; // "(" + state.varGetLHSName(childb) + "=" + ( vchild.intValue()-1 ) + ") + 1";
	        	}else{
	        		state.pushVStack(new valueClass(child + "--"));
	        	}
	        	return child + "--";
	        }
	        return null;
	    }

	    public Object visitExprVar(ExprVar exp)
	    {
	    	String vname =  exp.getName();
	    	valueClass intValue;
	    	if( state.varHasValue( vname ) ){
	    		intValue = new valueClass(state.varValue(vname)) ;	    		
	    	}else{
	    		intValue = new valueClass(state.varGetRHSName( exp.getName() ));
	    	}
	    	int sz = state.checkArray(vname);
	    	if( sz >= 0 ){
	    		List<valueClass> nlist = new LinkedList<valueClass>();
 	    		for(int i=0; i<sz; ++i){
 	    			String lnm = vname + "_idx_" + i;
 	    			if( state.varHasValue( lnm) ){
 	    				nlist.add(new valueClass( state.varValue(lnm )));
 	    			}else{
 	    				nlist.add(new valueClass(state.varGetRHSName(lnm)));
 	    			}
 	    		}
	    		state.pushVStack( new valueClass(nlist) );
	    	}else{
	    		state.pushVStack(intValue);	    		
	    	}
    		if(sz >=0){
    			return exp.getName();	
    		}else{
    			return state.varGetRHSName( exp.getName() );
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
	            
	            String lhs = field.getName(i);
	            	 
	            state.varDeclare(lhs);	 
	            String lhsn = state.varGetLHSName(lhs);
	            if( field.getType(i) instanceof TypeArray ){
	            	TypeArray ta = (TypeArray) field.getType(i);
	            	ta.getLength().accept(this);
	            	valueClass tmp = state.popVStack();
	            	Assert(tmp.hasValue(), "The array size must be a compile time constant !! \n" + field.getContext());
	            	state.makeArray(lhs, tmp.getIntValue());
	            	for(int tt=0; tt<tmp.getIntValue(); ++tt){
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

        String inParameterSetter(Iterator formalParamIterator, Iterator actualParamIterator, boolean checkError){
        	String result = "";
	        while(actualParamIterator.hasNext()){	        	
	        	Expression actualParam = (Expression)actualParamIterator.next();			        	
	        	Parameter formalParam = (Parameter) formalParamIterator.next();
	        	
	        	//this.state.markVectorStack();
	        	
	        	String apnm = (String) actualParam.accept(this);
	        	
	        	valueClass apnmVal = state.popVStack();
	    		
	        	if( apnmVal.isVect() ){	
	        		List<valueClass> lst= apnmVal.getVectValue();
	        		assert formalParam.getType() instanceof TypeArray : "This should never happen!!";
	        		((TypeArray)formalParam.getType()).getLength().accept(this);
	        		valueClass sizeInt = state.popVStack();
	        		Assert(sizeInt.hasValue(), "I must know array bounds for parameters at compile time");
	        		int size = sizeInt.getIntValue();	        		
	        		
	        		List<String> rhsNames = new ArrayList<String>();
	        		for(int i=0; i<lst.size(); ++i){
	        			rhsNames.add(state.varGetRHSName(apnm + "_idx_" + i));
	        		}
	        		
	        		if(lst.size()<size){	        			
	        			while(lst.size()<size){
	        				lst.add(new valueClass(0));
	        				rhsNames.add("0");
	        			}
	        			assert lst.size() == size :"Just to make sure";
	        		}
	        		
	        		String formalParamName = formalParam.getName();
		        	state.varDeclare(formalParamName);
		    		String lhsname = state.varGetLHSName(formalParamName);
		    		
		    		
		    		Iterator<valueClass> it = lst.iterator();
		    		Iterator<String> rhsNamesIter = rhsNames.iterator();
		    		int idx = 0;
		    		state.makeArray(formalParamName, lst.size());
		    		while( it.hasNext() ){
		    			valueClass i =  it.next();
		    			String lpnm = formalParamName + "_idx_" + idx;
		    			state.varDeclare(lpnm);
			    		lhsname = state.varGetLHSName(lpnm);
			    		String rhsName = rhsNamesIter.next();
			    		if( !formalParam.isParameterOutput() ){
				    		if(!i.hasValue()){
				    			result += lhsname + " = " + rhsName + ";\n";
				    		}else{
				    			state.setVarValue(lpnm, i.getIntValue());
				    		}
			    		}
		    			++idx;
		    		}
		    	}else{
		    		String formalParamName = formalParam.getName();
		        	state.varDeclare(formalParamName);
		    		String lhsname = state.varGetLHSName(formalParamName);		    		
		    		Assert(apnmVal.hasValue() || !checkError, "I must be able to determine the values of the parameters at compile time.");
		    		if( !formalParam.isParameterOutput() ){
			    		if(!apnmVal.hasValue()){
			    			result += lhsname + " = " + apnm + ";\n";
			    		}else{
			    			int value = apnmVal.getIntValue();
			    			state.setVarValue(formalParamName, value);
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
	        	
	        	String apnm = (String) actualParam.accept( new LHSvisitor());	        	
	        	
	        	String formalParamName = formalParam.getName();	        	
	    		valueClass formalParamVal = state.popVStack();
	        	if( formalParamVal.isVect() ){				        
		    		List<valueClass> lst= formalParamVal.getVectValue();
		    		Iterator<valueClass> it = lst.iterator();
		    		int idx = 0;		    		
		    		while( it.hasNext() ){
		    			valueClass i =  it.next();
		    			String formalName = formalParamName + "_idx_" + idx;		    						    		
			    		if( formalParam.isParameterOutput() ){
			    			String rhsname = state.varGetRHSName(formalName);
				    		if(!i.hasValue()){
				    			result += state.varGetLHSName(apnm+"_idx_"+idx) + " = " + rhsname + ";\n";
				    		}else{
				    			state.setVarValue(formalName, i.getIntValue());
				    		}
			    		}
		    			++idx;
		    		}
		    	}else{
		    		String lhsname = state.varGetLHSName(apnm);
		    		Assert(formalParamVal.hasValue() || !checkError, "I must be able to determine the values of the parameters at compile time.");		    				    		
		    		if( formalParam.isParameterOutput() ){
			    		if(!formalParamVal.hasValue()){
			    			result += lhsname + " = " + state.varGetRHSName(formalParamName) + ";\n";
			    		}else{
			    			Integer value = formalParamVal.getIntValue();
			    			state.setVarValue(formalParamName, value);
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
	        List<valueClass> rhsLst = null;
	        valueClass vrhsVal = state.popVStack();	
	        if( vrhsVal.isVect() ){
	        	rhsLst= vrhsVal.getVectValue();
	        }else{
	        	if(vrhsVal.hasValue()){
	        		vrhs = new Integer(vrhsVal.getIntValue());
	        	}else{
	        		vrhs = null;
	        	}
		        if(vrhs != null){
		        	rhs = vrhs.toString();
		        }		        	
	        }
	        
	        String lhs = (String)stmt.getLHS().accept( new LHSvisitor() );
	        
	        String lhsnm = state.varGetLHSName(lhs);
	        
	        int arrSize = state.checkArray(lhs);
	        boolean isArr = arrSize > 0;
	        
	        Integer vlhs = null;
	        String rlhs = null;  	        
	        
	        valueClass vlhsVal = state.popVStack();
	        
	        if(isArr && vlhsVal.isVect() ){	        	
	        		
	        }else{
	        	assert isArr == vlhsVal.isVect();
	        	if(vlhsVal.hasValue())
	        		vlhs = vlhsVal.getIntValue();
	        	else
	        		vlhs = null;
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
	        valueClass tmp = state.popVStack();
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
	        valueClass vcond = state.popVStack();
	        int iters = 0;
	        while(vcond.hasValue() && vcond.getIntValue() > 0){
	        	++iters;
	        	result += (String)stmt.getBody().accept(this);
	        	String incr;
	        	if (stmt.getIncr() != null)
		        	incr = (String)stmt.getIncr().accept(this);
	        	cond = (String)stmt.getCond().accept(this);
		        vcond = state.popVStack();
		        Assert(iters <= (1<<13), "This is probably a bug, why would it go around so many times? ");
	        }
	        state.popLevel();
	        return result;
	    }

	    public Object visitStmtIfThen(StmtIfThen stmt)
	    {
	        // must have an if part...
	    	
	        String result = "";
	        String cond = (String)stmt.getCond().accept(this);
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
	    	valueClass vcond = state.popVStack();
	    	if(!vcond.hasValue()){
	    		String nvar = state.varDeclare(); 
	    		result += nvar + " = " + "(" + iter + ");\n"; 	    		
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
	    		for(int i=0; i<vcond.getIntValue(); ++i){
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
			            		String tmplhsn = state.varGetLHSName(nnm);
				    			state.setVarValue(nnm, ival.getIntValue());
				    			++tt;
				    		}
				    		return "";
				    	}else{
				    		Integer val = null;
				    		if(vclass.hasValue())
				    			vclass.getIntValue();
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
		            		String tmplhsn = state.varGetLHSName(nnm);		            		
		            	}
	            	}
	            }else{
		            
		            if (stmt.getInit(i) != null){      	
		                 String asgn = lhsn + " = " + (String)stmt.getInit(i).accept(this) + "; \n";
		                valueClass tmp = state.popVStack();
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
			return rval;
		}
}
