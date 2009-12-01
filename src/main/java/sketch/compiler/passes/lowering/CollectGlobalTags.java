package sketch.compiler.passes.lowering;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssert;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtAtomicBlock;
import sketch.compiler.ast.core.stmts.StmtBlock;
import sketch.compiler.ast.core.stmts.StmtIfThen;
import sketch.compiler.ast.core.stmts.StmtVarDecl;

public class CollectGlobalTags extends FEReplacer {
    public Set<Object> oset = new HashSet<Object>();
    public Set<String> globals;
    
    public  boolean isGlobal = false;
    
    private boolean ignoreAsserts = false;
    
    public void ignoreAsserts(){
        ignoreAsserts = true;
    }
    
    public void collectAllTags(Statement s){
        FEReplacer fer = new FEReplacer(){
            public Object visitStmtAssign(StmtAssign stmt){
                return collectTag(stmt);
            };
            public Object visitStmtAssert(StmtAssert stmt){
                return collectTag(stmt);
            };          
        };
        s.accept(fer);      
    }
    
    public Statement collectTag(Object o){
        Statement s = (Statement) o;
        if(s.getTag() != null){
            oset.add(s.getTag());
        }
        return s;
    }
    
    public CollectGlobalTags(Set<StmtVarDecl> svd){
        globals = new HashSet<String>();
        for(Iterator<StmtVarDecl> it = svd.iterator(); it.hasNext(); ){
            StmtVarDecl sv = it.next();
            for(int i=0; i<sv.getNumVars(); ++i){
                globals.add( sv.getName(i) );
            }
        }
    }
    
    
    public Object visitExprVar(ExprVar ev){
        
        if(globals.contains(ev.getName())){
            isGlobal = true;
        }
        
        return ev;      
    }
    

    @Override
    public Object visitStmtAssign(StmtAssign stmt){
        isGlobal = false;
        Object o = super.visitStmtAssign(stmt); 
        if( isGlobal )
            return collectTag(o);
        else
            return o;
    }
    
    @Override
    public Object visitStmtAtomicBlock(StmtAtomicBlock stmt){
        boolean tmp = isGlobal;
        isGlobal = false;
        int sz = oset.size();
        Object o = super.visitStmtAtomicBlock(stmt); 
        if( isGlobal || sz != oset.size()){
            collectTag(stmt.getBlock());
            collectAllTags(stmt.getBlock());
            return collectTag(o);
        }else{
            isGlobal = tmp;
            return o;
        }
    }
    
    public Object visitStmtBlock(StmtBlock sb){
        boolean tmp = isGlobal;
        isGlobal = false;
        int sz = oset.size();
        Object o = super.visitStmtBlock(sb);
        if(isGlobal || sz != oset.size()){
            return collectTag(o);
        }else{
            isGlobal = tmp;
            return o;
        }
    }
    
    public Object visitStmtIfThen(StmtIfThen stmt){     
        int sz = oset.size();
        if(stmt.getTag() != null){ 
            int t = ((Integer)stmt.getTag()).intValue();
            int x = t;
        }
        boolean gg = false;
        Statement tpart = null;
        Statement epart = null;
        Expression cpart = null;
        {
            boolean tmp = isGlobal;
            isGlobal = false;
            tpart = (Statement) stmt.getCons().accept(this);
            gg = gg || isGlobal;
            isGlobal = tmp;         
        }
        if(stmt.getAlt()!= null){
            boolean tmp = isGlobal;
            isGlobal = false;
            epart = (Statement) stmt.getAlt().accept(this);
            gg = gg || isGlobal;
            isGlobal = tmp; 
        }
        {
            boolean tmp = isGlobal;
            isGlobal = false;
            cpart = (Expression) stmt.getCond().accept(this);
            gg = gg || isGlobal;
            isGlobal = tmp;         
        }
        
        
        if(gg || sz != oset.size()){
            isGlobal = true;
            return collectTag(stmt);
        }else{
            return stmt;
        }
        
    }
    
    
    
    public Object visitExprFunCall(ExprFunCall efc){
        assert false :"NYI";
        return efc;
    }

    @Override
    public Object visitStmtAssert(StmtAssert stmt){
        Object o = super.visitStmtAssert(stmt);   
        if(ignoreAsserts){
            return o;
        }else{
            return collectTag(o);
        }
    }
    
    

}
