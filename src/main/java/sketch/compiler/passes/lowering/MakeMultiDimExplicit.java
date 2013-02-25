package sketch.compiler.passes.lowering;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.ExprArrayInit;
import sketch.compiler.ast.core.exprs.ExprArrayRange;
import sketch.compiler.ast.core.exprs.ExprBinary;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprConstant;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssert;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtBlock;
import sketch.compiler.ast.core.stmts.StmtFor;
import sketch.compiler.ast.core.stmts.StmtIfThen;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.stencilSK.VarReplacer;

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
    
    public Object visitExprFunCall(ExprFunCall exp) {
        Function f = nres.getFun(exp.getName());
        boolean hasChanged = false;
        List<Expression> newParams = new ArrayList<Expression>();
        Iterator<Parameter> ip = f.getParams().iterator();

        Map<String, Expression> pmap = new HashMap<String, Expression>();
        VarReplacer vrep = new VarReplacer(pmap);

        for (Expression param : exp.getParams()) {
            Parameter p = ip.next();
            Expression newParam = doExpression(param);
            pmap.put(p.getName(), newParam);
            Type tleft = (Type) p.getType().accept(vrep);
            Type tright = getType(newParam);
            if (needsWork(tleft, tright)) {
                String rhv = varGen.nextVar();
                addStatement((Statement) new StmtVarDecl(newParam, tleft, rhv, null).accept(this));
                Expression tmp = new ExprVar(newParam, rhv);
                addStatement((Statement) new StmtAssign(tmp, newParam).accept(this));
                newParam = tmp;
            }
            newParams.add(newParam);
            if (param != newParam)
                hasChanged = true;
        }
        if (!hasChanged)
            return exp;
        return new ExprFunCall(exp, exp.getName(), newParams);
    }

    public boolean needsWork(Type tleft, Type tright) {
        if(tleft.equals(tright)){
            return false;
        }
        if(!(tleft instanceof TypeArray)){
            return false;
        }
        TypeArray taar = (TypeArray)tleft;
        if(!(taar.getBase() instanceof TypeArray)){
            return false;
        }
        return true;
    }

    public Object visitStmtAssign(StmtAssign sa) {
        Type tleft = getType(sa.getLHS());
        Type tright = getType(sa.getRHS());
        if (!needsWork(tleft, tright)) {
            return super.visitStmtAssign(sa);
        }
        TypeArray taar = (TypeArray) tleft;

        Expression tblen;
        if(tright instanceof TypeArray){
            tblen = ((TypeArray)tright).getLength();
        }else{
            tblen = ExprConstInt.one;
        }
        String nv = varGen.nextVar();       
        
        Expression rhexp;
        if(sa.getRHS() instanceof ExprConstant){
            rhexp = sa.getRHS(); 
        }else{
            String rhv = varGen.nextVar();
            addStatement((Statement)new StmtVarDecl(sa.getRHS(), tright, rhv, null).accept(this));
            rhexp = new ExprVar(sa.getRHS(), rhv);
            addStatement((Statement) new StmtAssign(rhexp, sa.getRHS()).accept(this));
        }
        
        
        
         
        
        StmtAssign nas;
        if(tright instanceof TypeArray){
            nas = new StmtAssign(
                new ExprArrayRange(sa.getLHS(), new ExprVar(sa, nv)),
                new ExprArrayRange(rhexp, new ExprVar(sa, nv)));
        }else{
            nas = new StmtAssign(
                    new ExprArrayRange(sa.getLHS(), new ExprVar(sa, nv)),
                    rhexp);
        }
        Statement sit;
        boolean eqq = tblen.equals(taar.getLength());
        if (!eqq && tright instanceof TypeArray) {
            addStatement(new StmtAssert(sa,
                    new ExprBinary(taar.getLength(), ">=", tblen), sa.getCx() + ": " +
                            "Array size missmatch", false));
        }

        if (rhexp.equals(taar.defaultValue()) || eqq) {
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
