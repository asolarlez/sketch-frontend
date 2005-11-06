package streamit.frontend.tosbit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import streamit.frontend.nodes.ExprArray;
import streamit.frontend.nodes.ExprArrayInit;
import streamit.frontend.nodes.ExprBinary;
import streamit.frontend.nodes.ExprConstInt;
import streamit.frontend.nodes.ExprFunCall;
import streamit.frontend.nodes.ExprStar;
import streamit.frontend.nodes.ExprUnary;
import streamit.frontend.nodes.ExprVar;
import streamit.frontend.nodes.Expression;
import streamit.frontend.nodes.FEContext;
import streamit.frontend.nodes.FENode;
import streamit.frontend.nodes.FEReplacer;
import streamit.frontend.nodes.FieldDecl;
import streamit.frontend.nodes.Function;
import streamit.frontend.nodes.Parameter;
import streamit.frontend.nodes.Statement;
import streamit.frontend.nodes.StmtAssign;
import streamit.frontend.nodes.StmtBlock;
import streamit.frontend.nodes.StmtFor;
import streamit.frontend.nodes.StmtLoop;
import streamit.frontend.nodes.StmtVarDecl;
import streamit.frontend.nodes.StreamSpec;
import streamit.frontend.nodes.StreamType;
import streamit.frontend.nodes.TempVarGen;
import streamit.frontend.nodes.Type;
import streamit.frontend.nodes.TypeArray;
import streamit.frontend.nodes.TypePrimitive;

class FindIndetNodes extends FEReplacer{
	public final Map<FENode, String> nodes;
	private final StreamSpec curSpec;
	private Set<Function> visitedFunctions;
	public FindIndetNodes(StreamSpec curSpec){
		nodes = new HashMap<FENode, String>();
		this.curSpec = curSpec;
		visitedFunctions = new HashSet<Function>();
	}
	public Object visitExprStar(ExprStar star) {
		nodes.put(star, "_oracle_" + nodes.size());
		return null;
	}
	public Object visitExprBinary(ExprBinary exp)
    {
		if(exp.getOp()==ExprBinary.BINOP_SELECT){
			nodes.put(exp, "_oracle_" + nodes.size());
		}
		return super.visitExprBinary(exp);
    }
	public Object visitExprFunCall(ExprFunCall exp)
    {	
		Function fun = curSpec.getFuncNamed(exp.getName());
		assert fun != null : "Calling undefined function!!";
		Object obj = super.visitExprFunCall(exp);
		if(!visitedFunctions.contains(fun)){
			visitedFunctions.add(fun);
			fun.accept(this);			
		}
		return obj;
    }
}



public class EliminateIndeterminacy extends FEReplacer {
	private TempVarGen varGen;
	private ValueOracle oracle;
	private FindIndetNodes nodeFinder;
	private List<Function> newFuncs;
	private StreamSpec curSpec;
	private final Map<String, Function> visitedFunctions;
	
	private void addFunction(Function fun){
		newFuncs.add(fun);		
	}
	
	public EliminateIndeterminacy(ValueOracle oracle, TempVarGen varGen) {
		super();
		this.oracle = oracle;
		this.varGen = varGen;
		visitedFunctions = new HashMap<String, Function>();
	}
	
	public void addOracleVars(List<Statement> stmts){
		Iterator<Entry<FENode, String>> names = nodeFinder.nodes.entrySet().iterator();		
		while(names.hasNext()){
			Entry<FENode, String> current = names.next();
			FEContext context = current.getKey().getContext();
			List<ExprConstInt> values = this.oracle.getVarsForNode(current.getKey());
			Type t = new TypeArray(TypePrimitive.inttype, new ExprConstInt(context, values.size()));
			ExprArrayInit init = new ExprArrayInit(context, values);
			Statement stmt =
	            new StmtVarDecl(context, t, current.getValue(),init);			
			stmts.add(stmt);
			stmt = new StmtVarDecl(context, TypePrimitive.inttype, current.getValue()+"_i", new ExprConstInt(context, 0));
			stmts.add(stmt);
		}
	}
	
	public void addOracleParams(List<Parameter> stmts){
		Iterator<Entry<FENode, String>> names = nodeFinder.nodes.entrySet().iterator();		
		while(names.hasNext()){
			Entry<FENode, String> current = names.next();
			FEContext context = current.getKey().getContext();
			List<ExprConstInt> values = this.oracle.getVarsForNode(current.getKey());
			Type t = new TypeArray(TypePrimitive.inttype, new ExprConstInt(context, values.size()));
			Parameter param = new Parameter(t, current.getValue());			
			stmts.add(param);
			param = new Parameter(TypePrimitive.inttype, current.getValue()+"_i");			
			stmts.add(param);
		}
	}
	
	 public Object visitStmtLoop(StmtLoop loop)
	 {		 
		 FEContext context = loop.getContext();
		 String indexName = varGen.nextVar();
	     ExprVar index = new ExprVar(null, indexName);
	     Type intType = new TypePrimitive(TypePrimitive.TYPE_INT);        
	     Statement init =
	            new StmtVarDecl(null, intType, indexName,
	                            new ExprConstInt(null, 0));
	     //symtab.registerVar(indexName, intType, null, SymbolTable.KIND_LOCAL);
	     Expression iters = (Expression)loop.getIter().accept(this);
	     String iterName = varGen.nextVar();
	     Statement iterDecl =
	            new StmtVarDecl(null, intType, iterName,
	                            iters);
	     this.doStatement(iterDecl);
	     Expression cond =
	     new ExprBinary(null, ExprBinary.BINOP_LT, index, new ExprVar(context, iterName));
	        Statement incr =
	            new StmtAssign(null, index,
	                           new ExprBinary(null, ExprBinary.BINOP_ADD,
	                                          index, new ExprConstInt(null, 1)));
	        
	        
	        Statement body = (Statement) loop.getBody().accept(this);
	            	        

	        // Now generate the loop, we have all the parts.
	        return new StmtFor(context, init, cond, incr, body);

		 
		 
	 }
	
	 public Object visitExprFunCall(ExprFunCall exp)
	    {	        
		 	FEContext context = exp.getContext();
	        List<Expression> newParams = new ArrayList<Expression>();
	        for (Iterator iter = exp.getParams().iterator(); iter.hasNext(); )
	        {
	            Expression param = (Expression)iter.next();
	            Expression newParam = doExpression(param);
	            newParams.add(newParam);	            
	        }
	        
	        
	        String oldName = exp.getName();
	        String newName = oldName; 	        
	        Function oldF = curSpec.getFuncNamed(oldName);	 	        	        
	        
	        assert oldF != null : "function " + oldName + "is undefined!!";
	        
	        FindIndetNodes findNdet =new FindIndetNodes(curSpec); 
	        oldF.accept(findNdet);
	        Iterator<Entry<FENode, String>> names = findNdet.nodes.entrySet().iterator();		
			while(names.hasNext()){				
				Entry<FENode, String> current = names.next();
				{
					Expression newParam = new ExprVar(context, current.getValue());
	            	newParams.add(newParam);
				}
				{
					Expression newParam = new ExprVar(context, current.getValue()+"_i");
	            	newParams.add(newParam);
				}	            
			}
			if(!visitedFunctions.containsKey(newName)){
				visitedFunctions.put(newName, null);
				Function newF = new Function(oldF.getContext(), oldF.getCls(), newName, oldF.getReturnType(), oldF.getParams(),oldF.getSpecification(), oldF.getBody() );	        
		        newF = (Function)newF.accept(this);
		        addFunction(newF);	
		        visitedFunctions.put(newName, newF);
	        }	        
	        return new ExprFunCall(exp.getContext(), newName, newParams);
	    }
	  
	 
	 public Object visitStreamSpec(StreamSpec spec)
	    {	        
	    	StreamSpec oldCspec = curSpec;
	    	curSpec = spec;
	        StreamType newST = null;
	        if (spec.getStreamType() != null)
	            newST = (StreamType)spec.getStreamType().accept(this);
	        List<FieldDecl> newVars = new ArrayList<FieldDecl>();
	        List<Function> oldFuncs = newFuncs;
	        newFuncs = new ArrayList<Function>();
	        boolean changed = false;
	        for (Iterator iter = spec.getVars().iterator(); iter.hasNext(); )
	        {
	            FieldDecl oldVar = (FieldDecl)iter.next();
	            FieldDecl newVar = (FieldDecl)oldVar.accept(this);
	            if (oldVar != newVar) changed = true;
	            newVars.add(newVar);
	        }
	        
	        SelectFunctionsToAnalyze funSelector = new SelectFunctionsToAnalyze();
	        List<Function> funcs = funSelector.selectFunctions(spec);
	        
	        for (Iterator<Function>  iter = funcs.iterator(); iter.hasNext(); )
	        {
	            Function oldFunc = iter.next();
	            Function newFunc = (Function)oldFunc.accept(this);
	            if (oldFunc != newFunc) changed = true;
	            newFuncs.add(newFunc);
	            
	        }
	        StreamSpec result;
	        if (!changed && newST == spec.getStreamType()){
	        	result = spec;
	        }else{
	        	result = new StreamSpec(spec.getContext(), spec.getType(),
	                              newST, spec.getName(), spec.getParams(),
	                              newVars, newFuncs);
	        }
	        
	        newFuncs = oldFuncs;
	        curSpec = oldCspec;
	        return result;
	        
	    }
	 
	 
	 
	public Object visitFunction(Function func)
    {
		if(func.getSpecification() != null){
			Statement fBody = func.getBody();
			FindIndetNodes oldNodeFinder = nodeFinder;
			nodeFinder =new FindIndetNodes(curSpec); 
			fBody.accept(nodeFinder);			
	        List<Statement> stmts = new ArrayList<Statement>();
	        addOracleVars(stmts);
	        stmts.add(fBody);	        
	        Statement result = new StmtBlock(func.getContext(), stmts);        
	        Statement newBody = (Statement)result.accept(this);
	        
	        nodeFinder = oldNodeFinder;
	        return new Function(func.getContext(), func.getCls(),
	                            func.getName(), func.getReturnType(),
	                            func.getParams(), func.getSpecification(), newBody);
		}else{
			Statement fBody = func.getBody();
			FindIndetNodes oldNodeFinder = nodeFinder;
			nodeFinder =new FindIndetNodes(curSpec); 
			fBody.accept(nodeFinder);			
	        List<Parameter> params = new ArrayList<Parameter>();
	        params.addAll(func.getParams());
	        addOracleParams(params);
	        Statement newBody = (Statement)fBody.accept(this);	        
	        nodeFinder = oldNodeFinder;
	        return new Function(func.getContext(), func.getCls(),
	                            func.getName(), func.getReturnType(),
	                            params, func.getSpecification(), newBody);
		}
    }
	
	public Object visitExprStar(ExprStar star) {
		//assert star.getSize() == 1 : "Int not yet implemented for this stage";
		FEContext context = star.getContext();
		String varName = nodeFinder.nodes.get(star);
		assert varName != null : "This can't happen!!!";
		ExprUnary index = new ExprUnary(context, ExprUnary.UNOP_POSTINC, new ExprVar(context, varName + "_i"));
		ExprArray ea = new ExprArray(context, new ExprVar(context, varName), index);
		return ea;
	}
	
	public Object visitExprBinary(ExprBinary exp)
    {
		if(exp.getOp() == ExprBinary.BINOP_SELECT){
			FEContext context = exp.getContext();
			String varName = nodeFinder.nodes.get(exp);
			ExprUnary index = new ExprUnary(context, ExprUnary.UNOP_POSTINC, new ExprVar(context, varName + "_i"));
			ExprArray ea = new ExprArray(context, new ExprVar(context, varName), index);
			
			Type intType = new TypePrimitive(TypePrimitive.TYPE_INT);   
		     String iterName = varGen.nextVar();
		     Statement iterDecl =
		            new StmtVarDecl(null, intType, iterName,
		                            ea);
			addStatement(iterDecl);			
			Expression left = doExpression(exp.getLeft());
			Expression newLeft = new ExprBinary(context, ExprBinary.BINOP_BAND, left, new ExprVar(context, iterName));
	        Expression right = doExpression(exp.getRight());
	        Expression newRight = new ExprBinary(context, ExprBinary.BINOP_BAND, right, 
	        		new ExprUnary(context, ExprUnary.UNOP_NEG, new ExprVar(context, iterName)));
	        Expression result = new ExprBinary(context, ExprBinary.BINOP_BOR, newLeft, newRight);
	        return result;
		}else{
			return super.visitExprBinary(exp);
		}
    }

}
