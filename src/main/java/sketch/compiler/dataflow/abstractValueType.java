package sketch.compiler.dataflow;

import java.util.List;

import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.stmts.StmtAssert;
import sketch.compiler.ast.core.typs.Type;


public abstract class abstractValueType{
	public PartialEvaluator eval;
	
	public void setPeval(PartialEvaluator eval){
		this.eval = eval;
	}
	
	abstract public abstractValue STAR(FENode star);
	abstract public abstractValue BOTTOM(); // == BOTTOM(TypePrimitive);
	abstract public abstractValue BOTTOM(Type t);	
	abstract public abstractValue BOTTOM(String label);
	/**
	 * Called by varDeclare. Used to create the state that goes on the left hand side.
	 * Usually ends up calling newLHSState();
	 * @param var the name of the variable we are declaring.
	 * @param t the type.
	 * @param mstate TODO
	 * @return
	 */
	abstract public varState cleanState(String var, Type t, MethodState mstate);

    abstract public abstractValue CONST(int v);
	abstract public abstractValue ARR(List<abstractValue> vals);
	abstract public abstractValue NULL();
	
	abstract public abstractValue plus(abstractValue v1, abstractValue v2);
	abstract public abstractValue minus(abstractValue v1, abstractValue v2);
	abstract public abstractValue times(abstractValue v1, abstractValue v2);
	abstract public abstractValue over(abstractValue v1, abstractValue v2);
	abstract public abstractValue mod(abstractValue v1, abstractValue v2);
	
	abstract public abstractValue and(abstractValue v1, abstractValue v2);
	abstract public abstractValue or(abstractValue v1, abstractValue v2);
	abstract public abstractValue xor(abstractValue v1, abstractValue v2);
	
	abstract public abstractValue shr(abstractValue v1, abstractValue v2);
	abstract public abstractValue shl(abstractValue v1, abstractValue v2);
	
	
	abstract public abstractValue gt(abstractValue v1, abstractValue v2);
	abstract public abstractValue lt(abstractValue v1, abstractValue v2);
	abstract public abstractValue ge(abstractValue v1, abstractValue v2);
	abstract public abstractValue le(abstractValue v1, abstractValue v2);
	abstract public abstractValue eq(abstractValue v1, abstractValue v2);
	
	
	abstract public abstractValue arracc(abstractValue arr, abstractValue idx);
	abstract public abstractValue arracc(abstractValue arr, abstractValue idx, abstractValue len, boolean isUnchecked);
	
	abstract public abstractValue cast(abstractValue v1, Type t);
	
	abstract public abstractValue not(abstractValue v1);
	abstract public abstractValue neg(abstractValue v1);	
	
	abstract public abstractValue ternary(abstractValue cond, abstractValue vtrue, abstractValue vfalse);
	
	/**
	 * Conditional join. 
	 * When cond is null, it is simply a join between vtrue and vfalse. 
	 * When cond is not null, then it is a conditional join, such that if cond is true, the return value should be vtrue, and otherwise it should be vfalse.
	 * Very similar to ternary, but not the same thing. Be careful with this. 
	 * @param cond
	 * @param vtrue
	 * @param vfalse
	 * @return
	 */
	abstract public abstractValue condjoin(abstractValue cond, abstractValue vtrue, abstractValue vfalse);

    abstract public void Assert(abstractValue val, StmtAssert stmt);
	/**
	 * 
	 * @param fun
	 * @param avlist Contains the abstractValue for the input parameters only.
	 * @param outSlist This is an output parameter. Needs to be set with 1 entry per output parameter.
	 * @param pathCond This is the path condition for the function call. The call only executes if path cond is true.
	 */
	abstract public void funcall(Function fun, List<abstractValue> avlist, List<abstractValue> outSlist, abstractValue pathCond);
}

