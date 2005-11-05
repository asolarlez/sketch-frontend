package streamit.frontend.tosbit;

import java.util.Iterator;
import java.util.List;

import streamit.frontend.nodes.ExprFunCall;
import streamit.frontend.nodes.Expression;
import streamit.frontend.nodes.Function;
import streamit.frontend.nodes.Parameter;
import streamit.frontend.nodes.StmtVarDecl;
import streamit.frontend.nodes.StreamSpec;
import streamit.frontend.nodes.TempVarGen;
import streamit.frontend.nodes.Type;
import streamit.frontend.nodes.TypeArray;
import streamit.frontend.nodes.TypePrimitive;
import streamit.frontend.tojava.NodesToJava;

public class NodesToC extends NodesToJava {

	public NodesToC(boolean libraryFormat, TempVarGen varGen) {
		super(libraryFormat, varGen);
		// TODO Auto-generated constructor stub
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
        if (func.getCls() == Function.FUNC_INIT) prefix = "final";
        result += doParams(func.getParams(), prefix) + " ";
        result += (String)func.getBody().accept(this);
        result += "\n";
        return result;
    }
	

    public String doParams(List params, String prefix)
    {
        String result = "(";
        boolean first = true;
        for (Iterator iter = params.iterator(); iter.hasNext(); )
        {
            Parameter param = (Parameter)iter.next();
            Type type = param.getType();
            String postFix = "";
            if(type instanceof TypeArray){
            	postFix = "[]";
            	type = ((TypeArray)type).getBase();
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
        // Hack: if the first variable name begins with "_final_", the
        // variable declaration should be final.
        if (stmt.getName(0).startsWith("_final_"))
            result += "final ";
        Type type = stmt.getType(0);
        String postFix = "";
        if(type instanceof TypeArray){
        	postFix = "[" + ((TypeArray)type).getLength() +  "]";
        	type = ((TypeArray)type).getBase();
        }
        result += convertType(type) +  " ";
        for (int i = 0; i < stmt.getNumVars(); i++)
        {
            if (i > 0)
                result += ", ";
            result += stmt.getName(i)+ postFix ;
            if (stmt.getInit(i) != null)
                result += " = " + (String)stmt.getInit(i).accept(this);
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
	
	

}
