/**
 *
 */
package sketch.compiler.passes.lowering;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.stmts.*;
import sketch.compiler.parallelEncoder.BreakParallelFunction;
import sketch.compiler.parallelEncoder.ExtractPreParallelSection;

/**
 * Converts runs of statements with only local effects, plus at most statement
 * with global effects, into a single atomic block.  This pass will also
 * incidentally flatten nested statement blocks.
 *
 * The new atomic block is then labeled with the tag of the last statement to
 * be executed.
 *
 * This class cowardly only merges statements within a basic block.  It will,
 * however, merge adjacent (non-conditional) atomic blocks with at most one
 * shared, global effect.
 *
 * Depends on variables being named uniquely.  Better results when variable
 * declarations have been hoisted.
 *
 * @author <a href="mailto:cgjones@cs.berkeley.edu">Chris Jones</a>
 */
public class MergeLocalStatements extends FEReplacer {
    Set<Object> globalStmtTags = new HashSet<Object> ();

    public static Program go (Program p) {
        // TODO: the functionality here should probably be factored out into
        // 'SymbolTableVisitor'-like class

        ExtractPreParallelSection ps = new ExtractPreParallelSection ();
        BreakParallelFunction parts = new BreakParallelFunction ();

        p.accept (ps);
        ps.parfun.accept (parts);

        CollectGlobalTags gtags = new CollectGlobalTags (parts.globalDecls);
        gtags.ignoreAsserts();
        parts.ploop.accept (gtags);

        return (Program) p.accept (new MergeLocalStatements (gtags.oset));
    }

    public MergeLocalStatements (Set<Object> globalStmtTags) {
        this.globalStmtTags = globalStmtTags;
    }

    /** We assume that blocks are already flattened, and thus we only need to
     * keep as long of a run as possible */
    public Object visitStmtBlock (StmtBlock sb) {
        List<Statement> oldStmts = newStatements;
        newStatements = new ArrayList<Statement> ();

        atomicify (sb.getStmts ());

        StmtBlock nb = new StmtBlock (sb, newStatements);
        newStatements = oldStmts;
        return nb;
    }
    /*
    public Object visitStmtIfThen(StmtIfThen stmt){
        Statement tpart = peelBlock((Statement) stmt.getCons().accept(this));
        Statement epart = null;
        if(stmt.getAlt() != null)
            epart = peelBlock((Statement) stmt.getAlt().accept(this));
        if(isSimpleStmt(tpart) && isSimpleStmt(epart)){
            if(tpart instanceof StmtAtomicBlock && !((StmtAtomicBlock)tpart).isCond()){
                tpart = ((StmtAtomicBlock)tpart).getBlock();
            }
            if(epart != null && epart instanceof StmtAtomicBlock && !((StmtAtomicBlock)epart).isCond()){
                epart = ((StmtAtomicBlock)epart).getBlock();
            }
            if(!(tpart instanceof StmtBlock)){
                Object oldTag = tpart.getTag();
                tpart = new StmtBlock(tpart);
                tpart.setTag(oldTag);
            }
            if(epart != null && !(epart instanceof StmtBlock)){
                Object oldTag = epart.getTag();
                epart = new StmtBlock(epart);
                epart.setTag(oldTag);
            }
            return new StmtAtomicBlock(stmt, Collections.singletonList(new StmtIfThen(stmt, stmt.getCond(), tpart, epart)));
        }
        if(tpart != stmt.getCons() || epart != stmt.getAlt()){
            if(!(tpart instanceof StmtBlock)){
                Object oldTag = tpart.getTag();
                tpart = new StmtBlock(tpart);
                tpart.setTag(oldTag);
            }
            if(epart != null && !(epart instanceof StmtBlock)){
                Object oldTag = epart.getTag();
                epart = new StmtBlock(epart);
                epart.setTag(oldTag);
            }
            return new StmtIfThen(stmt, stmt.getCond(), tpart, epart);
        }else{
            return stmt;
        }
    }
    */
    
    
    int countAtomics(Statement s){
        if(s == null){ return 0; }
        class lcounter extends FEReplacer{
            int lcount;
            public Object visitStmtBlock(StmtBlock sb){
                for(Statement s : sb.getStmts()){
                    if(s instanceof StmtBlock){
                        visitStmtBlock((StmtBlock)s);
                        continue;
                    }
                    if(s instanceof StmtVarDecl){
                        continue;
                    }
                    if(s instanceof StmtAtomicBlock){
                        ++lcount;
                        if(((StmtAtomicBlock)s).isCond()){
                            lcount += 5;
                        }
                        continue;
                    }
                    lcount += 10;
                }
                return sb;
            }
        };
        lcounter ln = new lcounter();
        s.accept(ln);
        return ln.lcount;
    }
    
    
    boolean doIfStmt(StmtIfThen stmt,  Stack<Statement> work, List<Statement> atomicRun){
        //Assumes the if condition is not global. Global 
        //if conditions have already been extracted.
        Statement tpart = (Statement) stmt.getCons().accept(this);
        Statement epart = null;
        if(stmt.getAlt() != null){
          epart = (Statement) stmt.getAlt().accept(this);
        }
        int tcount = countAtomics(tpart);
        int ecount = countAtomics(epart);
        Statement newif = new StmtIfThen(stmt, stmt.getCond(), tpart, epart);
        newif.setTag(stmt.getTag());
        if(ecount <= 1 && tcount <= 1){
            StmtAtomicBlock sab = new StmtAtomicBlock(stmt, newif, null);
            sab.setTag(stmt.getTag());
            work.push(sab);
            return false;
        }else{
            addAtomicRun(atomicRun);
            this.newStatements.add( newif );            
            return true;
        }
    }
    
    Statement peelBlock(Statement s){
        if(s instanceof StmtBlock){
            StmtBlock sb = (StmtBlock) s;
            if(sb.getStmts().size() == 1){
                return peelBlock(sb.getStmts().get(0));
            }
        }
        return s;
    }

    /** Run through the statements in S, merging adjacent stmts with at most
     * one global effect into AtomicBlocks, which are added to 'into'. */
    public void atomicify (List<Statement> S) {
        Stack<Statement> work = new Stack<Statement> ();
        List<Statement> atomicRun = new ArrayList<Statement> ();

        addWork (work, S);

        while (!work.empty ()) {
            Statement s = work.pop ();
            
            
            if (s instanceof StmtBlock) {
                addWork (work, ((StmtBlock) s).getStmts ());
                continue;
            } 
            
            if(s instanceof StmtVarDecl){
                StmtVarDecl svd = (StmtVarDecl) s;
                String str = svd.getName(0);                
                if(svd.getInit(0) != null){
                    work.push(new StmtAssign(new ExprVar(svd, svd.getName(0)), svd.getInit(0)));
                    doStatement(new StmtVarDecl(svd, svd.getType(0), svd.getName(0), null) );
                }else{
                    doStatement(svd);
                }
                continue;
            }
            
            if(s instanceof StmtIfThen){
                boolean tt = doIfStmt((StmtIfThen) s, work, atomicRun);
                if(tt){ atomicRun = new ArrayList<Statement> (); }
                continue;
            }
            
            boolean simple = isSimpleStmt (s);
            boolean global = isGlobalStmt (s);


            if (simple){
                if (s instanceof StmtAtomicBlock) {                 
                    atomicRun.addAll (((StmtAtomicBlock) s).getBlock ().getStmts ());
                }else{
                    atomicRun.add (s);  
                }
            }

            if (global || !simple) {
                addAtomicRun (atomicRun);
                atomicRun = new ArrayList<Statement> ();
            }

            if (!simple)
                doStatement (s);
        }

        addAtomicRun (atomicRun);
        //newStatements.addAll (atomicRun);
    }

    public void addAtomicRun (List<Statement> run) {
        class tmpc{
            public Integer getTag(Statement s){
                if(s instanceof StmtIfThen){
                    StmtIfThen sif = ((StmtIfThen)s);
                    if(sif.getAlt() != null){
                        return getTag(sif.getAlt());
                    }else{
                        return getTag(sif.getCons());
                    }
                }
                if(s instanceof StmtBlock){
                    StmtBlock sb = (StmtBlock)s;
                    return getTag(sb.getStmts().get(sb.getStmts().size()-1));
                }
                return (Integer) s.getTag();
            }
        }
        tmpc tt = new tmpc();
        if (run.size () > 0) {
            if(run.size() == 1 && run.get(0) instanceof StmtAtomicBlock){
                addStatement(run.get(0));
            }else{
                StmtAtomicBlock b = new StmtAtomicBlock (run.get (0), run);
                Integer ii =  tt.getTag(run.get (run.size () - 1));
                b.setTag (ii);
                addStatement (b);
            }
        }
    }

    protected void addWork (Stack<Statement> work, List<Statement> S) {
        for (int i = S.size () - 1; i >= 0; --i) {
            work.push (S.get (i));
        }
    }

    protected boolean isSimpleStmt (Statement s) {
        if(s == null) return true;
        return (s instanceof StmtAssert)
                || (s instanceof StmtAssign)
                || (s instanceof StmtAtomicBlock && !((StmtAtomicBlock)s).isCond ())
                || (s instanceof StmtEmpty)
                || (s instanceof StmtExpr)
                || (s instanceof StmtReturn);
    }

    protected boolean isGlobalStmt (Statement s) {
        return globalStmtTags.contains (s.getTag ());
    }
}
