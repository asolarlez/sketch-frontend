package streamit.frontend.experimental.nodesToSB;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import streamit.frontend.experimental.PartialEvaluator;
import streamit.frontend.experimental.abstractValue;
import streamit.frontend.nodes.ExprConstInt;
import streamit.frontend.nodes.ExprFunCall;
import streamit.frontend.nodes.Expression;
import streamit.frontend.nodes.Function;
import streamit.frontend.nodes.Parameter;
import streamit.frontend.nodes.Statement;
import streamit.frontend.nodes.StmtAssert;
import streamit.frontend.nodes.StmtBlock;
import streamit.frontend.nodes.StreamSpec;
import streamit.frontend.nodes.TempVarGen;
import streamit.frontend.nodes.Type;
import streamit.frontend.nodes.TypeArray;
import streamit.frontend.nodes.TypePortal;
import streamit.frontend.nodes.TypePrimitive;
import streamit.frontend.nodes.TypeStruct;
import streamit.frontend.nodes.TypeStructRef;
import streamit.frontend.tosbit.ValueOracle;
import streamit.frontend.tosbit.recursionCtrl.RecursionControl;
/**
 * This class translates the ast into a boolean function which is output to a file.
 * The format is suitable for the SBitII backend solver.<BR>
 * Preconditions:
 * <DL>
 * <DT>  Arrays
 * <DD>  * Only 1 dimensional arrays allowed.
 * <DD>  * The [] operator can only be used on an array variable.
 * <DD>  * binary operators work only for scalars.
 * <DD>  * Array to array assignments are supported. (arr1 = arr2)
 * <DD>  * Array ranges with len != 1 are not supported.
 * <DD>  * Scalar to array assignments are supported.
 * </DL>
 * 
 * @author asolar
 */
public class ProduceBooleanFunctions extends PartialEvaluator {
	public ProduceBooleanFunctions(TempVarGen varGen, 
			ValueOracle oracle, PrintStream out, int maxUnroll, RecursionControl rcontrol){
		super(new NtsbVtype(oracle, out), varGen, false, maxUnroll, rcontrol);		
	}
	
	private String convertType(Type type) {
	    // This is So Wrong in the greater scheme of things.
	    if (type instanceof TypeArray)
	    {
	        TypeArray array = (TypeArray)type;
	        String base = convertType(array.getBase());
	        abstractValue iv = (abstractValue) array.getLength().accept(this);	        
	        return base + "[" + iv + "]";
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
	
	
	List<String> opnames;
	List<Integer> opsizes;
	
	
	public void doParams(List<Parameter> params) {
		PrintStream out = ((NtsbVtype)this.vtype).out;
	    boolean first = true;
	    
	    out.print("(");

    	for(Iterator<Parameter> iter = params.iterator(); iter.hasNext(); ){
    		Parameter param = iter.next();
    		
    		if (!first) out.print(", ");
    		first = false;
	        if(param.isParameterOutput()) out.print("! ");
	        out.print(convertType(param.getType()) + " ");
	        String lhs = param.getName();
    		state.varDeclare(lhs , param.getType());
    		IntAbsValue inval = (IntAbsValue)state.varValue(lhs);
    		
    		if( param.getType() instanceof TypeArray ){
	        	TypeArray ta = (TypeArray) param.getType();
	        	IntAbsValue tmp = (IntAbsValue)  ta.getLength().accept(this);	        	
	        	report(tmp.hasIntVal(), "The array size must be a compile time constant !! \n" );
	        	assert inval.isVect() : "If it is not a vector, something is really wrong.\n" ;
	        	int sz = tmp.getIntVal();
	        	if(param.isParameterOutput()){
	        		opsizes.add(sz);
	        	}
	        	for(int tt=0; tt<sz; ++tt){	        		
	        		String nnm = inval.getVectValue().get(tt).toString();	        		
	        		if(param.isParameterOutput()){
	        			String opname = "_p_" + nnm + " ";
	        			opnames.add(opname);
	        			out.print(opname);
	        		}else{
	        			out.print(nnm + " ");
	        		}
	        	}
	        }else{
	        	if(param.isParameterOutput()){
	        		opsizes.add(1);
	        		String opname = "_p_" + inval.toString() + " ";
	        		opnames.add(opname);
	        		out.print(opname);
	        	}else{
	        		out.print(inval.toString() + " ");
	        	}
	        }
    		
    		
    	}
    	out.print(")");	    
	}
	
	public void doOutParams(List<Parameter> params) {
		PrintStream out = ((NtsbVtype)this.vtype).out;
	    boolean first = true;
	    Iterator<Integer> opsz = opsizes.iterator();
	    Iterator<String> opnm = opnames.iterator();
    	for(Iterator<Parameter> iter = params.iterator(); iter.hasNext(); ){
    		Parameter param = iter.next();
    		first = false;
	        String lhs = param.getName();
	        if(param.isParameterOutput()){	        
	        	IntAbsValue inval = (IntAbsValue)state.varValue(lhs);
	        	assert opsz.hasNext() : "This can't happen.";
	        	int sz = opsz.next();
	        	for(int tt=0; tt<sz; ++tt){
	        		String nnm = null;
	        		if( inval.isVect() ){
	        			nnm = inval.getVectValue().get(tt).toString();
	        		}else{
	        			assert tt == 0;
	        			nnm = inval.toString();
	        		}
	        		String onm = opnm.next();
	        		out.println(onm + " = " + nnm + ";");
	        	}
	        }
    	}
	}
	
	
	public Object visitFunction(Function func)
    {
		System.out.println("Analyzing " + func.getName());
		
		((NtsbVtype)this.vtype).out.print(func.getName());
    	if( func.getSpecification() != null ){
    		((NtsbVtype)this.vtype).out.print(" SKETCHES " + func.getSpecification()); 
    	}    	
    	
    	List<Integer> tmpopsz = opsizes;
    	List<String> tmpopnm = opnames;
    	
    	opsizes = new ArrayList<Integer>();
    	opnames = new ArrayList<String>();
    	
    	doParams(func.getParams());

    	
		((NtsbVtype)this.vtype).out.println("{");		    	
    	state.beginFunction(func.getName());
    	
    	Statement newBody = (Statement)func.getBody().accept(this);
    	
    	state.endFunction();
    	
    	doOutParams(func.getParams());
    	
		((NtsbVtype)this.vtype).out.println("}");
		
		
		opsizes = tmpopsz;
		opnames = tmpopnm;
		
		return func;
    }
	
	
    public Object visitStreamSpec(StreamSpec spec)
    {  
    	((NtsbVtype)this.vtype).out.println("Filter " + spec.getName() + " {");
    	Object tmp = super.visitStreamSpec(spec);
    	((NtsbVtype)this.vtype).out.println("}");
    	return spec;
    	
    }
	
    
	public Object visitExprFunCall(ExprFunCall exp)
	{		
    	String name = exp.getName();
    	// Local function?
		Function fun = ss.getFuncNamed(name);
		if(fun.getSpecification()!= null){
			assert false : "The substitution of sketches for their respective specs should have been done in a previous pass.";
		}
    	if (fun != null) {   
    		if( fun.isUninterp()  ){    			
    			return super.visitExprFunCall(exp);
    		}else{
	    		if (rcontrol.testCall(exp)) {
	                /* Increment inline counter. */
	            	rcontrol.pushFunCall(exp, fun);  
	    		
					List<Statement>  oldNewStatements = newStatements;
					newStatements = new ArrayList<Statement> ();					
					state.pushLevel();
					try{
			    		{
			    			Iterator<Expression> actualParams = exp.getParams().iterator();	        		        	       	
			    			Iterator<Parameter> formalParams = fun.getParams().iterator();
			    			inParameterSetter(exp.getCx() ,formalParams, actualParams, false);
			    		}
			    		Statement body = (Statement) fun.getBody().accept(this);
			    		addStatement(body);
			    		{
			    			Iterator<Expression> actualParams = exp.getParams().iterator();	        		        	       	
			    			Iterator<Parameter> formalParams = fun.getParams().iterator();
			    			outParameterSetter(formalParams, actualParams, false);
			    		}			    		
		    		}finally{
		    			state.popLevel();
		    			newStatements = oldNewStatements;
		    		}
		    		rcontrol.popFunCall(exp);
	    		}else{
	    			/* System.out.println("        Stopped recursion:  " + fun.getName());
	    			funcsToAnalyze.add(fun);	    			
	    			return super.visitExprFunCall(exp); */
	    			StmtAssert sas = new StmtAssert(exp.getCx(), new ExprConstInt(0));
	    			sas.setMsg( (exp.getCx()!=null?exp.getCx().toString() : "" ) + exp.getName()  );
	    			sas.accept(this); 
	    		}
	    		exprRV = exp;
	    		return vtype.BOTTOM();
    		}
    	}
    	exprRV = null;
    	return vtype.BOTTOM();    	
    }
	
}
