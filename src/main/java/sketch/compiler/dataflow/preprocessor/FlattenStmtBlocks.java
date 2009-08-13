package sketch.compiler.dataflow.preprocessor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAtomicBlock;
import sketch.compiler.ast.core.stmts.StmtBlock;
import sketch.compiler.ast.core.stmts.StmtDoWhile;
import sketch.compiler.ast.core.stmts.StmtFor;
import sketch.compiler.ast.core.stmts.StmtIfThen;
import sketch.compiler.ast.core.stmts.StmtLoop;
import sketch.compiler.ast.core.stmts.StmtWhile;
import sketch.compiler.ast.promela.stmts.StmtFork;

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
	 @Override
	 public Object visitStmtWhile(StmtWhile stmt)
	    {
		 boolean oldIsWithinBlock = isWithinBlock;
		 isWithinBlock = false;
		 Object o = super.visitStmtWhile(stmt);
		 isWithinBlock = oldIsWithinBlock;
		 return o;
	    }
	 
	 @Override
	 public Object visitStmtDoWhile(StmtDoWhile stmt)
	    {
		 boolean oldIsWithinBlock = isWithinBlock;
		 isWithinBlock = false;
		 Object o = super.visitStmtDoWhile(stmt);
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
		        Statement result = new StmtBlock(stmt, newStatements);
		        newStatements = oldStatements;
		        return result;
	        }
	        
	        return null;
	    }

}
