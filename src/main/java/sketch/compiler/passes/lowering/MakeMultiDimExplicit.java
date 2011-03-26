package sketch.compiler.passes.lowering;

import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.ExprArrayInit;
import sketch.compiler.ast.core.exprs.ExprArrayRange;
import sketch.compiler.ast.core.exprs.ExprBinary;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprConstant;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtBlock;
import sketch.compiler.ast.core.stmts.StmtFor;
import sketch.compiler.ast.core.stmts.StmtIfThen;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;

public class MakeMultiDimExplicit extends SymbolTableVisitor {

    
    TempVarGen varGen;
    public MakeMultiDimExplicit(TempVarGen varGen) {        
        super (null);
        this.varGen = varGen;
    }
    
    public Object visitExprArrayInit(ExprArrayInit eai){
        Type ta = getType(eai);
        if(!(ta instanceof TypeArray )){ return eai; }
        TypeArray taar = (TypeArray)ta;
        if(!(taar.getBase() instanceof TypeArray )){ return eai; }
        String nv = varGen.nextVar();
        addStatement((Statement)new StmtVarDecl(eai, ta, nv, null).accept(this));
        int i=0; 
        for(Expression e : eai.getElements()){
            addStatement((Statement)new StmtAssign(new ExprArrayRange(new ExprVar(eai, nv), ExprConstInt.createConstant(i)), e).accept(this));
            i++;
        }
        return new ExprVar(eai, nv);
    }
    
    public Object visitStmtAssign(StmtAssign sa){
        Type ta = getType(sa.getLHS());
        Type tb = getType(sa.getRHS());
        if(ta.equals(tb)){
            return super.visitStmtAssign(sa);
        }
        if(!(ta instanceof TypeArray)){
            return super.visitStmtAssign(sa);
        }
        TypeArray taar = (TypeArray)ta;
        if(!(taar.getBase() instanceof TypeArray)){
            return super.visitStmtAssign(sa);
        }
        Expression tblen;
        if(tb instanceof TypeArray){
            tblen = ((TypeArray)tb).getLength();
        }else{
            tblen = ExprConstInt.one;
        }
        String nv = varGen.nextVar();       
        
        Expression rhexp;
        if(sa.getRHS() instanceof ExprConstant){
            rhexp = sa.getRHS(); 
        }else{
            String rhv = varGen.nextVar();
            addStatement((Statement)new StmtVarDecl(sa.getRHS(), tb, rhv, null).accept(this));
            rhexp = new ExprVar(sa.getRHS(), rhv);
            addStatement((Statement) new StmtAssign(rhexp, sa.getRHS()).accept(this));
        }
        
        
        
         
        
        StmtAssign nas;
        if(tb instanceof TypeArray){
            nas = new StmtAssign(
                new ExprArrayRange(sa.getLHS(), new ExprVar(sa, nv)),
                new ExprArrayRange(rhexp, new ExprVar(sa, nv)));
        }else{
            nas = new StmtAssign(
                    new ExprArrayRange(sa.getLHS(), new ExprVar(sa, nv)),
                    rhexp);
        }
        Statement sit;
        if(rhexp.equals(taar.defaultValue())){
            sit = new StmtBlock(nas);
        }else{
            StmtAssign nasdef = new StmtAssign(
                    new ExprArrayRange(sa.getLHS(), new ExprVar(sa, nv)),
                    taar.defaultValue());
                            
            sit = new StmtIfThen(sa, new ExprBinary(new ExprVar(sa, nv), "<", tblen), 
                    new StmtBlock(nas), 
                    new StmtBlock(nasdef)
            );    
        }
        
        return new StmtFor(nv, taar.getLength(), new StmtBlock(sit) ).accept(this);
        
    }
}
