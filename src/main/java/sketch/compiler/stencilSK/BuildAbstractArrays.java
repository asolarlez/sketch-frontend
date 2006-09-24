package streamit.frontend.stencilSK;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import streamit.frontend.nodes.StmtAssign;
import streamit.frontend.nodes.StmtVarDecl;
import streamit.frontend.nodes.SymbolTable;
import streamit.frontend.tosbit.PartialEvaluator;

public class BuildAbstractArrays extends PartialEvaluator {
	Map<String, AbstractArray> inVars;
	   /**
     * The current symbol table.  Functions in this class keep the
     * symbol table up to date; calling
     * <code>super.visitSomething</code> from a derived class will
     * update the symbol table if necessary and recursively visit
     * children.
     */
    protected SymbolTable symtab;
/*
    
    public Object visitStmtVarDecl(StmtVarDecl stmt)
    {
        for (int i = 0; i < stmt.getNumVars(); i++){
            symtab.registerVar(stmt.getName(i),
                               actualType(stmt.getType(i)),
                               stmt,
                               SymbolTable.KIND_LOCAL);
            
            
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
        return super.visitStmtVarDecl(stmt);
    }
    
    
    
    
    public Object visitFieldDecl(FieldDecl field)
    {
        for (int i = 0; i < field.getNumFields(); i++){
            symtab.registerVar(field.getName(i),
                               actualType(field.getType(i)),
                               field,
                               SymbolTable.KIND_FIELD);
            
        }
        return super.visitFieldDecl(field);
    }
    
    protected Type actualType(Type type)
    {       
        return type;
    }

    public Object visitFunction(Function func)
    {
        SymbolTable oldSymTab = symtab;
        symtab = new SymbolTable(symtab);
        for (Iterator iter = func.getParams().iterator(); iter.hasNext(); )
        {
            Parameter param = (Parameter)iter.next();
            symtab.registerVar(param.getName(),
                               actualType(param.getType()),
                               param,
                               SymbolTable.KIND_FUNC_PARAM);
        }
        Object result = super.visitFunction(func);
        symtab = oldSymTab;
        return result;
    }
    
    
    */
    
	public BuildAbstractArrays(Map<String, AbstractArray> inVars){
		super(true);
		this.inVars = inVars;		
		for(Iterator<Entry<String, AbstractArray>> it = inVars.entrySet().iterator(); it.hasNext(); ){
			it.next().getValue().makeDefault(3);
			
			
		}
	}
	
	
	
    public Object visitStmtAssign(StmtAssign stmt){
    	return null;
    	/**
    	 * if( isArrayAccess){
    	 *   if( Array is grid ){
    	 *   	if( the rhs has an input grid){
    	 *   		Extract expression, get all dependencies, feed to the AbstractArray.
    	 *      }else{
    	 *      	ignore
    	 *      }
    	 *   }else{
    	 *   	assert false.
    	 *   }
    	 * }else{
    	 *   record the assignment, move on.
    	 * }
    	 */
    	
    	
    	/*
        String op;
        //this.state.markVectorStack();	        
        stmt.getRHS().accept(this);
        valueClass vrhsVal = state.popVStack();     
        
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
        */
    }

	
	
}
