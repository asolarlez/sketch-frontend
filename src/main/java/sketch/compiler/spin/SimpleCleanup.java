package streamit.frontend.spin;

import java.util.ArrayList;
import java.util.List;

import streamit.frontend.experimental.varState;
import streamit.frontend.experimental.preprocessor.PreprocessSketch;
import streamit.frontend.nodes.ExprVar;
import streamit.frontend.nodes.Expression;
import streamit.frontend.nodes.FieldDecl;
import streamit.frontend.nodes.Function;
import streamit.frontend.nodes.StmtAssert;
import streamit.frontend.nodes.StmtAssign;
import streamit.frontend.nodes.StreamSpec;
import streamit.frontend.nodes.Type;
import streamit.frontend.tosbit.recursionCtrl.ZeroInlineRControl;

public class SimpleCleanup extends PreprocessSketch {

	public SimpleCleanup(){
		super(null, 1, new ZeroInlineRControl());
	}

	public String transName(String name){
		return name;
	}



	@Override
    protected List<Function> functionsToAnalyze(StreamSpec spec){
	    return new ArrayList<Function>(spec.getFuncs());
    }

    @Override
    public Object visitStmtAssert(StmtAssert stmt){
    	 /* Evaluate given assertion expression. */
        Expression assertCond = stmt.getCond();
        assertCond.accept (this);
        Expression ncond = exprRV;
        return isReplacer ? new StmtAssert(stmt, ncond, stmt.getMsg(), stmt.isSuper())
        		: stmt
        		;
    }



    @Override
    public Object visitFieldDecl(FieldDecl field){
    	List<Type> types = isReplacer? new ArrayList<Type>() : null;
    	List<String> names = isReplacer? new ArrayList<String>() : null;
    	List<Expression> inits = isReplacer? new ArrayList<Expression>() : null;
        for (int i = 0; i < field.getNumFields(); ++i)
        {
            String lhs = field.getName(i);
            state.varDeclare(lhs, field.getType(i));
            varState vs = state.UTvarState(state.transName(lhs));
            vs.makeVolatile();
            Expression nexpr = null;
            if (field.getInit(i) != null){
				(new StmtAssign(
						new ExprVar(field, lhs),
						field.getInit(i))).accept(this);
				nexpr = exprRV; //This may be a bit risky, but will work for now.
            }else{
            	nexpr = null;
            }
            if(isReplacer){
            	types.add(field.getType(i));
            	names.add(transName(lhs));
            	inits.add( nexpr );
            }
        }
        return isReplacer? new FieldDecl(field, types, names, inits) :field;
    }

	@Override
	public Object visitFunction(Function f){

		if(f.getName().contains("_fork_thread_")){
			return super.visitFunction(f);
		}
		return f;
	}

}
