package sketch.compiler.parallelEncoder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtBlock;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.promela.stmts.StmtFork;

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
	public StmtFork ploop = null;
	public Function parfun = null;

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
		        		newStatements.add(new StmtVarDecl(svd, svd.getTypes(), svd.getNames(), inits));
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
		            			s = new StmtAssign(new ExprVar(svd, svd.getName(i)), svd.getInit(i));
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
	        Statement result = new StmtBlock(stmt, newStatements);
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

		if(foundploop){
			parfun = func;
		}
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
            		 preblock = new StmtBlock(stmt, newStatements);
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
        		result = new StmtBlock(stmt, newStatements);
        	}else{
        		result = newStatements.get(0);
        	}
        }
        if(foundploop && buildSuperBlock){
        	assert oldStatements.size() == 1;
        	List<Statement> slist =new ArrayList<Statement>(2);
        	slist.add(preblock);
        	slist.add(result);
        	result = new StmtBlock(stmt, slist);
        }else{
        	newStatements = oldStatements;
        }
        return result;
    }



	public Object visitStmtFork(StmtFork loop){
		foundploop = true;
		ploop = loop;
		return loop;
    }

}
