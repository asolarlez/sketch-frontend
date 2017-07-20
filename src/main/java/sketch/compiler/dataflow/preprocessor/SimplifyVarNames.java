package sketch.compiler.dataflow.preprocessor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.NameResolver;
import sketch.compiler.ast.core.Package;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.StructDef;
import sketch.compiler.ast.core.typs.Type;

public class SimplifyVarNames extends FEReplacer {

	Map<String, Integer> nmMap = new HashMap<String, Integer>();
	Map<String, String> newNm = new HashMap<String, String>();
	
    public Set<String> fields = null;

    public Object visitStructDef(StructDef ts) {

        fields = new HashSet<String>();
        StructDef sdf = ts;
        while (sdf != null) {
            for (Entry<String, Type> entry : sdf) {
                fields.add(entry.getKey());
            }
            String pn = sdf.getParentName();
            if (pn != null) {
                sdf = nres.getStruct(pn);
            } else {
                sdf = null;
            }
        }
        Object o = super.visitStructDef(ts);

        fields = null;
        return o;
    }

    public Object visitProgram(Program prog) {
        nres = new NameResolver(prog);
        for (Package ssOrig : prog.getPackages()) {
            for (StructDef ts : ssOrig.getStructs()) {
                String name;
            }
        }
        return super.visitProgram(prog);
    }

	String transName(String name){
	    
	    if(newNm.containsKey(name)){
	        return newNm.get(name);
        } else {
            int prevlast = -1;
            int last = -1;
            int cur = name.indexOf('_');
            while (cur != -1) {
                prevlast = last;
                last = cur;
                cur = name.indexOf('_', last + 1);
            }

            // FIXME xzl: why do we require at least two '_' ?
            if (prevlast < 0) {
                prevlast = last;
            }
			String s1;
			if (prevlast == 0) {
				s1 = name;
			} else {
            assert prevlast > 0;
				s1 = name.substring(0, prevlast);
			}
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
                do {
	            int id = nmMap.get(s1).intValue();
	            nmMap.put(s1, id+1);
	            s1 += "_" + id;
                } while (nmMap.containsKey(s1));
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
        if (fields != null && fields.contains(name))
            return exp;
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
                nparams.add(new Parameter(param, param.getSrcTupleDepth(), ntype,
                        transName(param.getName()),
                        param.getPtype()));
    		}
    	}
    	
    	
    	
    	
    	Statement newBody = (Statement)func.getBody().accept(this);

    	return func.creator().params(nparams).body(newBody).create();
    }
	
	
}
