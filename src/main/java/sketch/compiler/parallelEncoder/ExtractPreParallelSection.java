package streamit.frontend.parallelEncoder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import streamit.frontend.nodes.ExprConstInt;
import streamit.frontend.nodes.ExprVar;
import streamit.frontend.nodes.Expression;
import streamit.frontend.nodes.FEReplacer;
import streamit.frontend.nodes.Function;
import streamit.frontend.nodes.Statement;
import streamit.frontend.nodes.StmtAssign;
import streamit.frontend.nodes.StmtBlock;
import streamit.frontend.nodes.StmtPloop;
import streamit.frontend.nodes.StmtVarDecl;

/***
 * 
 * The purpose of this class is to divide all functions into 
 * a pre-parallel section and a post-parallel section.
 * 
 * @author asolar
 *
 */

public class ExtractPreParallelSection extends FEReplacer {

	protected boolean foundploop = false;	
	public StmtPloop ploop = null;
	
	static class MoveMisplacedDeclarations extends FEReplacer{
		final Map<String, StmtVarDecl> decls;
		final Set<String> undecld;
		MoveMisplacedDeclarations(Map<String, StmtVarDecl> decls, Set<String> undecld){
			this.decls = decls;
			this.undecld = undecld;
		}
		
		public Object visitStmtBlock(StmtBlock stmt)
	    {			
	        List<Statement> oldStatements = newStatements;
	        newStatements = new ArrayList<Statement>();
	        if(oldStatements == null){
	        	for(Iterator<String> it = undecld.iterator(); it.hasNext(); ){
	        		String name = it.next();
	        		if(decls.containsKey(name)){
		        		StmtVarDecl svd = decls.get(name);
		        		ArrayList<Expression> inits = new ArrayList<Expression>(svd.getNumVars());	        		
		        		for(int i=0; i<svd.getNumVars(); ++i) inits.add(ExprConstInt.zero);
		        		newStatements.add(new StmtVarDecl(svd.getCx(), svd.getTypes(), svd.getNames(), inits));
	        		}
	        	}
	        }
	        for (Iterator iter = stmt.getStmts().iterator(); iter.hasNext(); )
	        {
	            Statement s = (Statement)iter.next();
	            // completely ignore null statements, causing them to
	            // be dropped in the output
	            
	            if(s instanceof StmtVarDecl){
	            	StmtVarDecl svd = (StmtVarDecl)s;
	            	boolean found = false;
	            	for(int i=0; i<svd.getNumVars(); ++i){
	            		if( undecld.contains(svd.getName(i)) ){
	            			found = true;
	            			break;
	            		}
	            	}
	            	if(found){
	            		for(int i=0; i<svd.getNumVars(); ++i){
		            		if( svd.getInit(i) != null ){
		            			s = new StmtAssign(s.getCx(), new ExprVar(null, svd.getName(i)), svd.getInit(i));
		            			try{
		            			doStatement(s);
		            			}catch(RuntimeException e){
		        	            	newStatements = oldStatements;
		        	            	throw e;
		        	            }
		            		}
		            	}
	            		
	            		continue;
	            	}
	            }
	            if (s == null)
	                continue;
	            try{
	            	doStatement(s);
	            }catch(RuntimeException e){
	            	newStatements = oldStatements;
	            	throw e;
	            }
	        }
	        Statement result = new StmtBlock(stmt.getContext(), newStatements);
	        newStatements = oldStatements;
	        return result;
	    }
		
		
	}
	
	static class FindMisdeclaredVars extends FEReplacer{
		
		Map<String, StmtVarDecl> decls = new HashMap<String, StmtVarDecl>();
		Map<String, StmtVarDecl> alldecls = new HashMap<String, StmtVarDecl>();
		Set<String> undecld = new HashSet<String>();
		public Object visitStmtVarDecl(StmtVarDecl decl){
			for (int i = 0; i < decl.getNumVars(); i++)
	        {
				decls.put(decl.getName(i), decl);
				alldecls.put(decl.getName(i), decl);
				if(decl.getInit(i) != null){
					decl.getInit(i).accept(this);
				}
	        }
			return decl;
		}
		public Object visitStmtBlock(StmtBlock stmt)
	    {
			Map<String, StmtVarDecl> oldDecls = decls;
			decls = new HashMap<String, StmtVarDecl>(decls);
			Object o = super.visitStmtBlock(stmt);
			decls = oldDecls;
			assert o == stmt : "This is an invariant. This FEReplacer shouldn't modify the program";
			return o;
	    }
		public Object visitExprVar(ExprVar exp){
			String name = exp.getName();
			if(!decls.containsKey(name)){
				undecld.add(name);
			}
			return exp;
		}	
	}
	
	
	public Object visitExprVar(ExprVar exp){		
		return exp;
	}
	
	@Override
	public Object visitStmtVarDecl(StmtVarDecl decl){
		return decl;
	}
	
	public Object visitFunction(Function func)
    {
		foundploop = false;
		func = (Function)super.visitFunction(func);
		///After this stage, the blocks have been broken, but all the declarations will be in the
		///wrong place, so we will fix it in two stages. First, we identify the declarations that
		///are in the wrong place, and then we move the declarations.
		FindMisdeclaredVars mdv = new FindMisdeclaredVars();
		func = (Function)func.accept(mdv);
		
		///And now, we do the change of the declarations.
		MoveMisplacedDeclarations mmd = new MoveMisplacedDeclarations(mdv.alldecls, mdv.undecld);
		func = (Function)func.accept(mmd);		
    	return func;
    }
	
	public Object visitStmtBlock(StmtBlock stmt)
    {
		if(stmt.getStmts().size() == 0){
			return null;
		}		
        List<Statement> oldStatements = newStatements;
        
        boolean buildSuperBlock = false;
        if(oldStatements == null){
        	buildSuperBlock = true;
        	oldStatements = new ArrayList<Statement>();
        }
        newStatements = new ArrayList<Statement>();
        StmtBlock preblock = null;
        for (Iterator iter = stmt.getStmts().iterator(); iter.hasNext(); )
        {
            Statement s = (Statement)iter.next();
            // completely ignore null statements, causing them to
            // be dropped in the output
            if (s == null)
                continue;
            try{            	
            	 Statement result = (Statement)s.accept(this);
            	 if(foundploop && preblock == null){
            		 assert result != null;
            		 preblock = new StmtBlock(stmt.getContext(), newStatements);
            		 newStatements = new ArrayList<Statement>();
            		 oldStatements.add(preblock);
            		 addStatement(result);
            		 
            	 }else{
            		 if (result != null)
            			 addStatement(result);
            	 }
            }catch(RuntimeException e){
            	newStatements = oldStatements;
            	throw e;
            }
        }
        
        Statement result = null;
        if(newStatements.size() > 0){
        	if(newStatements.size() > 1){
        		result = new StmtBlock(stmt.getContext(), newStatements);
        	}else{
        		result = newStatements.get(0);
        	}
        }
        if(foundploop && buildSuperBlock){
        	assert oldStatements.size() == 1;
        	List<Statement> slist =new ArrayList<Statement>(2);        	
        	slist.add(preblock);        	
        	slist.add(result);        	
        	result = new StmtBlock(stmt.getCx(), slist);        	
        }else{
        	newStatements = oldStatements;
        }
        return result;
    }
	
	
	
	public Object visitStmtPloop(StmtPloop loop){		
		foundploop = true;
		ploop = loop;
		return loop;
    }
	
}
