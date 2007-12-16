package streamit.frontend.experimental.preprocessor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import streamit.frontend.nodes.ExprVar;
import streamit.frontend.nodes.Expression;
import streamit.frontend.nodes.FEReplacer;
import streamit.frontend.nodes.Function;
import streamit.frontend.nodes.Parameter;
import streamit.frontend.nodes.Statement;
import streamit.frontend.nodes.StmtVarDecl;
import streamit.frontend.nodes.Type;

public class SimplifyVarNames extends FEReplacer {

	Map<String, Integer> nmMap = new HashMap<String, Integer>();
	
	String transName(String name){
		int idx = name.indexOf('_');		
		String s1 = name.substring(0, idx);
		if(s1.length() == 0){
			s1 = "s";
		}
		String s2 = name.substring(idx);
		if( nmMap.containsKey(s2)){
			s1 += "_" + nmMap.get(s2);
		}else{
			int sz = nmMap.size();
			nmMap.put(s2, sz);
			s1 += "_" + sz;
		}				
		return s1;
	}
	
	public Object visitExprVar(ExprVar exp) {
		String name = exp.getName();		
		 return new ExprVar(exp.getContext(), transName(name)); 
	}
	
    public Object visitStmtVarDecl(StmtVarDecl stmt)
    {
        List<Expression> newInits = new ArrayList<Expression>();
        List<String> newNames = new ArrayList<String>();
        List<Type> newTypes = new ArrayList<Type>();
        for (int i = 0; i < stmt.getNumVars(); i++)
        {        	
        	newNames.add(transName((String)stmt.getNames().get(i)));
            Expression init = stmt.getInit(i);
            newTypes.add( (Type) stmt.getType(i).accept(this)  ); 
            if (init != null)
                init = doExpression(init);
            newInits.add(init);
        }
        return new StmtVarDecl(stmt.getContext(), newTypes,
                               newNames, newInits);
    }
    
    public Object visitFunction(Function func)
    {
    	nmMap.clear();
    	if(func.isUninterp()) return func;
    	
        List<Parameter> params = func.getParams();
        List<Parameter> nparams = new ArrayList<Parameter>();
    	for(Iterator<Parameter> it = params.iterator(); it.hasNext(); ){
    		Parameter param = it.next();    		
    		{
    			Type ntype = (Type)param.getType().accept(this);
    			nparams.add( new Parameter(ntype, transName(param.getName()), param.getPtype()));
    		}
    	}
    	
    	
    	
    	
    	Statement newBody = (Statement)func.getBody().accept(this);
    	    	

        return  new Function(func.getContext(), func.getCls(),
                            func.getName(), func.getReturnType(),
                            nparams, func.getSpecification(), newBody);
    	
    	//state.pushVStack(new valueClass((String)null) );
    }
	
	
}
