package sketch.compiler.parallelEncoder;

import java.util.ArrayList;
import java.util.List;

import sketch.compiler.ast.core.FieldDecl;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Package;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.StmtAssert;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.dataflow.varState;
import sketch.compiler.dataflow.preprocessor.PreprocessSketch;
import sketch.compiler.dataflow.recursionCtrl.ZeroInlineRControl;

public class ParallelPreprocessor extends PreprocessSketch {

	public ParallelPreprocessor(){
		super(null, 1, new ZeroInlineRControl());
	}

	public String transName(String name){
		return name;
	}



	@Override
    protected List<Function> functionsToAnalyze(Package spec){
	    return new ArrayList<Function>(spec.getFuncs());
    }

    @Override
    public Object visitStmtAssert(StmtAssert stmt){
    	 /* Evaluate given assertion expression. */
        Expression assertCond = stmt.getCond();
        assertCond.accept (this);
        Expression ncond = exprRV;
        return isReplacer ? new StmtAssert(stmt, ncond, stmt.getMsg(), stmt.isSuper(),
                stmt.getAssertMax(), stmt.isHard)
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

}
