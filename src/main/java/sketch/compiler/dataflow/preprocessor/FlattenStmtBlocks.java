package streamit.frontend.experimental.preprocessor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import streamit.frontend.nodes.Expression;
import streamit.frontend.nodes.FEReplacer;
import streamit.frontend.nodes.Statement;
import streamit.frontend.nodes.StmtAssert;
import streamit.frontend.nodes.StmtAtomicBlock;
import streamit.frontend.nodes.StmtBlock;
import streamit.frontend.nodes.StmtFor;
import streamit.frontend.nodes.StmtIfThen;
import streamit.frontend.nodes.StmtLoop;
import streamit.frontend.nodes.StmtFork;
import streamit.frontend.nodes.StmtVarDecl;

public class FlattenStmtBlocks extends FEReplacer {
	
	boolean isWithinBlock = false;
			
	
	public Object visitStmtAtomicBlock (StmtAtomicBlock ab) {
		 boolean oldIsWithinBlock = isWithinBlock;
		 isWithinBlock = false;
		 Object o = super.visitStmtAtomicBlock(ab);
		 isWithinBlock = oldIsWithinBlock;
		 return o;
	}
	

    public Object visitStmtFork(StmtFork loop){
    	boolean oldIsWithinBlock = isWithinBlock;
		 isWithinBlock = false;
		 Object o = super.visitStmtFork(loop);
		 isWithinBlock = oldIsWithinBlock;
		 return o;
    }
	
	
	
	public Object visitStmtLoop(StmtLoop stmt)
    {
		 boolean oldIsWithinBlock = isWithinBlock;
		 isWithinBlock = false;
		 Object o = super.visitStmtLoop(stmt);
		 isWithinBlock = oldIsWithinBlock;
		 return o;
    }
	
	 public Object visitStmtFor(StmtFor stmt)
	    {
		 boolean oldIsWithinBlock = isWithinBlock;
		 isWithinBlock = false;
		 Object o = super.visitStmtFor(stmt);
		 isWithinBlock = oldIsWithinBlock;
		 return o;
	    }
	 public Object visitStmtIfThen(StmtIfThen stmt){
		 boolean oldIsWithinBlock = isWithinBlock;
		 isWithinBlock = false;
		 Object o = super.visitStmtIfThen(stmt);
		 isWithinBlock = oldIsWithinBlock;
		 return o;
		 
	    }
	
	 
	 
	 
	  public Object visitStmtBlock(StmtBlock stmt)
	    {
		  
		  List<Statement> oldStatements = newStatements;
		  if( !isWithinBlock  ){
			  newStatements = new ArrayList<Statement>();  
		  }
	        
		  boolean oldIsWithinBlock = isWithinBlock;
		  isWithinBlock = true;
	        for (Iterator iter = stmt.getStmts().iterator(); iter.hasNext(); )
	        {
	            Statement s = (Statement)iter.next();
	            // completely ignore null statements, causing them to
	            // be dropped in the output
	            if (s == null)
	                continue;
	            try{
	            	doStatement(s);
	            }catch(RuntimeException e){
	            	newStatements = oldStatements;
	            	throw e;
	            }
	        }	        
	        isWithinBlock = oldIsWithinBlock;
	        
	        if(!isWithinBlock){
		        Statement result = new StmtBlock(stmt.getCx(), newStatements);
		        newStatements = oldStatements;
		        return result;
	        }
	        
	        return null;
	    }

}
