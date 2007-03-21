package streamit.frontend.experimental.preprocessor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import streamit.frontend.nodes.Expression;
import streamit.frontend.nodes.FEReplacer;
import streamit.frontend.nodes.Statement;
import streamit.frontend.nodes.StmtAssert;
import streamit.frontend.nodes.StmtBlock;
import streamit.frontend.nodes.StmtFor;
import streamit.frontend.nodes.StmtIfThen;

public class FlattenStmtBlocks extends FEReplacer {
	
	boolean isWithinBlock = false;
			
	
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
		        Statement result = new StmtBlock(stmt.getContext(), newStatements);
		        newStatements = oldStatements;
		        return result;
	        }
	        
	        return null;
	    }

}
