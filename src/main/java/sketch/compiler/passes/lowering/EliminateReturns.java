package sketch.compiler.passes.lowering;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.SymbolTable;
import sketch.compiler.ast.core.exprs.ExprArrayRange;
import sketch.compiler.ast.core.exprs.ExprBinary;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprField;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.*;
import sketch.compiler.ast.core.typs.TypePrimitive;



public class EliminateReturns extends SymbolTableVisitor{
    
    private boolean inRetStmt = false;
    
    public EliminateReturns() {
        this(null);
    }

    public EliminateReturns(SymbolTable symtab) {
        super(symtab);        
    }
    
    private String getReturnFlag() {
        return "_has_out_";
    }
    
    protected Expression getFalseLiteral() {
        return ExprConstInt.zero;
    }
    
    private Statement conditionWrap(Statement s){
        
        if(!inRetStmt){
            
            Statement ret=new StmtIfThen(s,
                    new ExprBinary(s, ExprBinary.BINOP_EQ,
                        new ExprVar(s, getReturnFlag()),
                        ExprConstInt.zero),
                    s,
                    null);
            return ret;
        }else{
            return s;
        }
    }
    

    protected boolean hasRet(FENode n){     
        class ReturnFinder extends FEReplacer{
            public boolean hasRet = false;
            public Object visitStmtReturn(StmtReturn stmt){
                hasRet  = true;
                return stmt;
            }           
        };
        
        ReturnFinder hf = new ReturnFinder();
        n.accept(hf);
        return hf.hasRet;
    }
    
    
    private boolean globalEffects(Statement s){
        if(s instanceof StmtAssert){
            return true;
        }else{
            
            class findge extends FEReplacer{
                public boolean ge = false;
                public Object visitExprField(ExprField ef){
                    ge = true;
                    return ef;
                }
                @Override
                public Object visitExprArrayRange(ExprArrayRange exp){
                    exp.getBase().accept(this);
                    return exp;
                }

                @Override
                public Object visitExprVar(ExprVar ev){
                    if(currentRefParams.contains(ev.getName())){
                        ge = true;
                    }
                    return ev;
                }

                @Override
                public Object visitExprFunCall(ExprFunCall exp){
                    ge = true;
                    return exp;
                }
            }
            findge f = new findge();
            s.accept(f);            
            return f.ge;
        }
    }

    
    public Object visitExprFunCall(ExprFunCall exp) {
        // first let the superclass process the parameters (which may be function calls)
        exp=(ExprFunCall) super.visitExprFunCall(exp);
        addStatement( conditionWrap(new StmtExpr(exp)));
        return ExprConstInt.zero;
    }
    
    

    @Override
    public Object visitStmtAssign(StmtAssign stmt){
        Statement s = (Statement) super.visitStmtAssign(stmt);
        if(globalEffects(s)){
            FENode cx=stmt;
            Statement ret=new StmtIfThen(cx,
                    new ExprBinary(cx, ExprBinary.BINOP_EQ,
                        new ExprVar(cx, getReturnFlag()),
                        new ExprConstInt(cx, 0)),
                    s,
                    null);
            return ret;
        }else{
            return s;
        }
    }

    
    @Override
    public Object visitStmtAssert(StmtAssert sa){
        Statement s = (Statement) super.visitStmtAssert(sa);
        return conditionWrap(s);
    }
    
    Set<String> currentRefParams = new HashSet<String>();
    
    @Override
    public Object visitParameter(Parameter p){
        if(p.isParameterOutput()){
            currentRefParams.add(p.getName());
        }
        return p;
    }
    
    public Object visitFunction(Function func) {
        if(func.isUninterp() ) return func;

        
        func=(Function) super.visitFunction(func);
                
        
        List<Statement> stmts=new ArrayList<Statement>(((StmtBlock)func.getBody()).getStmts());
        //add a declaration for the "return flag"
        stmts.add(0,new StmtVarDecl(func.getBody(),TypePrimitive.bittype,getReturnFlag(),new ExprConstInt(0)));
        func=new Function(func,func.getCls(),func.getName(),
                func.getReturnType(), func.getParams(),
                func.getSpecification(), new StmtBlock(func,stmts));
        return func;
    }
    
    @Override
    public Object visitStmtWhile(StmtWhile stmt)
    {
        Statement body=stmt.getBody();
        Expression cond = stmt.getCond();
        if(body!=null && !(body instanceof StmtBlock))
            body=new StmtBlock(stmt,Collections.singletonList(body));
        
        //if(hasRet(body)){
            cond = new ExprBinary(cond, "&&", new ExprBinary(
                    new ExprVar(cond, getReturnFlag()), "==",
                    getFalseLiteral()) );
        //}
        
        if(body!=stmt.getBody() || cond != stmt.getCond())
            stmt=new StmtWhile(stmt,cond, body);        
        return super.visitStmtWhile(stmt);
    }
    
    @Override
    public Object visitStmtDoWhile(StmtDoWhile stmt)
    {
        Statement body=stmt.getBody();
        Expression cond = stmt.getCond();
        if(body!=null && !(body instanceof StmtBlock))
            body=new StmtBlock(stmt,Collections.singletonList(body));
        
        //if(hasRet(body)){
            cond = new ExprBinary(cond, "&&", new ExprBinary(
                    new ExprVar(cond, getReturnFlag()), "==",
                    getFalseLiteral()) );
        //}
        
        if(body!=stmt.getBody() || cond != stmt.getCond())
            stmt=new StmtDoWhile(stmt,body,cond);
        return super.visitStmtDoWhile(stmt);
    }

    @Override
    public Object visitStmtFor(StmtFor stmt)
    {
        Statement body=stmt.getBody();
        if(body!=null && !(body instanceof StmtBlock))
            body=new StmtBlock(stmt,Collections.singletonList(body));
        
        Expression cond = stmt.getCond();
        
        if(SimpleLoopUnroller.decideForLoop(stmt)<0 &&  hasRet(body)){
            cond = new ExprBinary(cond, "&&", new ExprBinary(
                    new ExprVar(cond, getReturnFlag()), "==",
                    getFalseLiteral()) );
            stmt=new StmtFor(stmt,stmt.getInit(),cond,stmt.getIncr(),body);
            return super.visitStmtFor(stmt);
        }else{
            if(body!=stmt.getBody() || cond != stmt.getCond())
                stmt=new StmtFor(stmt,stmt.getInit(),cond,stmt.getIncr(),body);
            return conditionWrap((Statement) super.visitStmtFor(stmt));
        }
        
    }
    @Override
    public Object visitStmtIfThen(StmtIfThen stmt)
    {
        Statement cons=stmt.getCons();
        Statement alt=stmt.getAlt();
        if(cons!=null && !(cons instanceof StmtBlock))
            cons=new StmtBlock(stmt,Collections.singletonList(cons));
        if(alt!=null && !(alt instanceof StmtBlock))
            alt=new StmtBlock(stmt,Collections.singletonList(alt));
        if(cons!=stmt.getCons() || alt!=stmt.getAlt())
            stmt=new StmtIfThen(stmt,stmt.getCond(),cons,alt);
        if( globalEffects(stmt) ){
            return conditionWrap( (Statement)
            super.visitStmtIfThen(stmt) );
        }else{
            return super.visitStmtIfThen(stmt);
        }
    }
    
    @Override
    public Object visitStmtReturn(StmtReturn stmt) {
        FENode cx=stmt;
        List<Statement> oldns = newStatements;
        boolean oldInrs = inRetStmt;
        inRetStmt = true;
        this.newStatements = new ArrayList<Statement> ();       
        stmt=(StmtReturn) super.visitStmtReturn(stmt);
        
        newStatements.add(new StmtAssign(cx, new ExprVar(cx, getReturnFlag()), new ExprConstInt(cx, 1), 0));
        Statement ret=new StmtIfThen(cx,
            new ExprBinary(cx, ExprBinary.BINOP_EQ,
                new ExprVar(cx, getReturnFlag()),
                new ExprConstInt(cx, 0)),
            new StmtBlock(cx,newStatements),
            null);
        newStatements = oldns;
        inRetStmt = oldInrs;
        return ret;
    }
}
