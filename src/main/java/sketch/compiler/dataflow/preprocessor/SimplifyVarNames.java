package sketch.compiler.dataflow.preprocessor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.Type;

public class SimplifyVarNames extends FEReplacer {

	Map<String, Integer> nmMap = new HashMap<String, Integer>();
	Map<String, String> newNm = new HashMap<String, String>();
	
	String transName(String name){
	    
	    if(newNm.containsKey(name)){
	        return newNm.get(name);
	    }else{
	        int idx = name.indexOf('_');       
	        String s1 = name.substring(0, idx);
	        if(s1.length() == 0){
	            if(name.contains("_out")){
	                s1 = "_out";
	            }else{
	                s1 = "s";
	                if(!nmMap.containsKey(s1)){
	                    nmMap.put(s1, 0);
	                }
	            }
	        }
	        if(nmMap.containsKey(s1)){
	            int id = nmMap.get(s1).intValue();
	            nmMap.put(s1, id+1);
	            s1 += "_" + id;
	            newNm.put(name, s1);
	            return s1;
	        }else{
	            nmMap.put(s1, 0);
	            newNm.put(name, s1);
                return s1;
	        }
	    }	    
	}
	
	public Object visitExprVar(ExprVar exp) {
		String name = exp.getName();		
		 return new ExprVar(exp, transName(name)); 
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
        return new StmtVarDecl(stmt, newTypes,
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
    	    	

        return  new Function(func, func.getCls(),
                            func.getName(), func.getReturnType(),
                            nparams, func.getSpecification(), newBody);
    	
    	//state.pushVStack(new valueClass((String)null) );
    }
	
	
}
