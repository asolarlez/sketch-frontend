package sketch.compiler.dataflow.cflowChecks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.StreamSpec;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtReturn;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.promela.stmts.StmtFork;
import sketch.compiler.dataflow.PartialEvaluator;
import sketch.compiler.dataflow.recursionCtrl.BaseRControl;

public class PerformFlowChecks extends PartialEvaluator {
    Cfctype cftype;
    
    public void report(FENode n, String msg){
        System.err.println( n.getCx() + ": " + msg );
        throw new IllegalStateException("Semantic check failed");
    }
    
    public PerformFlowChecks(){     
        super(new Cfctype(), new TempVarGen() , false, 1, (new BaseRControl(10)));
        cftype = (Cfctype) this.vtype;
    }
    
     public Object visitStmtAssign(StmtAssign stmt)
        {           
            CfcValue rhs = null;            
            rhs = (CfcValue) stmt.getRHS().accept(this);
            
            if(! rhs.maybeinit()){ report(stmt,  "There is a variable in the rhs of the assignment that may not have been initialized. All variables must be statically initialized."); }
            return super.visitStmtAssign(stmt);
        }
     
     
     /**
     *
     * This method must necessarily push a parallel section.
     * This implementation is the most conservative implementation for this methods.
     * In some cases, we can be more liberal and pass a subset of the variables
     * that we want to make volatile in the fork. For things like constant propagation,
     * we only need to make volatile those variables that are modified in the fork.
     *
     * For now, I am making this very liberal. Nothing is made volatile.
     *
     */
    protected void startFork(StmtFork loop){
        state.pushParallelSection(Collections.EMPTY_SET);
    }
     
     
     @Override
     public Object visitStmtReturn(StmtReturn stmt)
        {           
            CfcValue rhs = null;      
            Expression e = stmt.getValue();
            if(e != null){
                rhs = (CfcValue) e.accept(this);
                if(! rhs.maybeinit()){ report(stmt,  "There is a variable in the return expression that may not have been initialized. All variables must be statically initialized."); }
            }
            return super.visitStmtReturn(stmt);
        }
                
            

        public Object visitStmtVarDecl(StmtVarDecl stmt)
        {
            List<Type> types = isReplacer? new ArrayList<Type>() : null;
            List<String> names = isReplacer? new ArrayList<String>() : null;
            List<Expression> inits = isReplacer? new ArrayList<Expression>() : null;
            for (int i = 0; i < stmt.getNumVars(); i++)
            {
                String nm = stmt.getName(i);
                Type vt = (Type)stmt.getType(i).accept(this);
                state.varDeclare(nm, vt);
                Expression ninit = null;
                if( stmt.getInit(i) != null ){
                    CfcValue init = (CfcValue) stmt.getInit(i).accept(this);
                    ninit = exprRV;
                    if(! init.maybeinit()){ report(stmt,  "There is a variable in the initializer that may not have been itself initialized. All variables must be statically initialized."); }
                    state.setVarValue(nm, init);
                }
                /* else{
                    state.setVarValue(nm, this.vtype.BOTTOM("UNINITIALIZED"));
                } */
                if( isReplacer ){
                    types.add(vt);
                    names.add(transName(nm));
                    inits.add(ninit);
                }
            }
            return isReplacer? new StmtVarDecl(stmt, types, names, inits) : stmt;
        }
     
     
     
    @Override
    public Object visitParameter(Parameter param){
        state.outVarDeclare(param.getName() , param.getType());
        if(param.isParameterInput()){
            state.setVarValue(param.getName(), Cfctype.allinit);
        }
        if(isReplacer){
            Type ntype = (Type)param.getType().accept(this);
             return new Parameter(ntype, transName(param.getName()), param.getPtype());
        }else{
            return param;
        }
    }
    
    
    
    public Object visitFunction(Function func) {
        state.beginFunction(func.getName());
        
        
        List<Parameter> nparams = isReplacer ? new ArrayList<Parameter>() : null;
        for(Parameter param : func.getParams() ){            
            Parameter p  = (Parameter) param.accept(this);
            if(isReplacer){
                nparams.add(p);
            }           
        }
        
        Statement newBody = (Statement)func.getBody().accept(this);

        
        for(Parameter param : func.getParams() ){ 
            if (!param.isParameterInput()) {
                CfcValue v = (CfcValue) state.varValue(param.getName());
                if (!v.maybeinit()) {
                    report(param,
                            "There are some paths under which the return value will not be set.");
                }
            }
        }
        
        state.endFunction();

        return isReplacer ? func.creator().params(nparams).body(newBody).create() : null;
    }
    
    protected List<Function> functionsToAnalyze(StreamSpec spec){
        return new ArrayList<Function>(spec.getFuncs());      
    }
    
}
