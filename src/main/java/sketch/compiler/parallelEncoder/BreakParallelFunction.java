package sketch.compiler.parallelEncoder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtBlock;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.promela.stmts.StmtFork;


/**
 * 
 * Takes a parallel function and extracts the ploop, the pre-parallel section, and the post-parallel section.
 * It also collects all declarations of global variables.
 * 
 * This class depends on {@link ExtractPreParallelSection}. 
 * 
 * @author asolar
 *
 */
public class BreakParallelFunction extends FEReplacer {

	public Set<StmtVarDecl> globalDecls = new HashSet<StmtVarDecl>();
	public StmtFork ploop = null;
	public Statement prepar = null;
	public Statement postpar = null;
	boolean foundploop = false;
	
	static final int STAGE_DECL=0;
	static final int STAGE_PREPAR=1;
	static final int STAGE_POSTPAR=2;
	
	
	public Object visitStmtFork(StmtFork loop){		
		foundploop = true;
		ploop = loop;
		return null;
    }
	
	
	public Object visitStmtBlock(StmtBlock stmt)
    {
		if(newStatements != null){
			return super.visitStmtBlock(stmt);
		}
		///This is the highest level block.
		///At this level, declarations are globals, and there should
		///be only two blocks.
		List<Statement> oldStatements = newStatements;
        newStatements = new ArrayList<Statement>();
        int stage = STAGE_DECL;
        int i=0;
        for (Iterator iter = stmt.getStmts().iterator(); iter.hasNext();++i )
        {
            Statement s = (Statement)iter.next();
            
            if(s instanceof StmtVarDecl){
            	assert stage == STAGE_DECL;
            	globalDecls.add((StmtVarDecl)s);
            	continue;
            }
            if (s == null)
                continue;
            try{
            	 if(s instanceof StmtBlock){
                 	if(stage == STAGE_DECL){
                 		//this block may either be PREPAR or POSTPAR.
                 		assert !foundploop;
                 		  Statement tmp = (Statement) s.accept(this);
                 		if(foundploop){
                 			postpar = tmp;
                 			stage = STAGE_POSTPAR;
                 		}else{
                 			prepar = tmp;
                 			stage = STAGE_PREPAR;
                 		}
                 	}else if(stage == STAGE_PREPAR){
                 		//Since there is only one prepar, this must be POSTPAR.    
                 		assert !foundploop;
                 		postpar = (Statement) s.accept(this);
                 		assert foundploop;
                 		stage = STAGE_POSTPAR;
                 	}else{
                 		assert stage != STAGE_POSTPAR : "There can be nothing after postpar";
                 	}
                 }
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
