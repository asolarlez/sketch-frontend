package streamit.frontend.experimental;

import java.util.List;

import streamit.frontend.nodes.FENode;
import streamit.frontend.nodes.Function;
import streamit.frontend.nodes.Type;


public abstract class abstractValueType{
	public PartialEvaluator eval;
	
	public void setPeval(PartialEvaluator eval){
		this.eval = eval;
	}
	
	abstract public abstractValue STAR(FENode star);
	abstract public abstractValue BOTTOM(); // == BOTTOM(TypePrimitive);
	abstract public abstractValue BOTTOM(Type t);	
	/**
	 * Called by varDeclare. Used to create the state that goes on the left hand side.
	 * @param var the name of the variable we are declaring.
	 * @param t the type.
	 * @return
	 */
	abstract public varState cleanState(String var, Type t);
	abstract public abstractValue CONST(int v);
	abstract public abstractValue ARR(List<abstractValue> vals);
	
	
	abstract public abstractValue plus(abstractValue v1, abstractValue v2);
	abstract public abstractValue minus(abstractValue v1, abstractValue v2);
	abstract public abstractValue times(abstractValue v1, abstractValue v2);
	abstract public abstractValue over(abstractValue v1, abstractValue v2);
	abstract public abstractValue mod(abstractValue v1, abstractValue v2);
	
	abstract public abstractValue and(abstractValue v1, abstractValue v2);
	abstract public abstractValue or(abstractValue v1, abstractValue v2);
	abstract public abstractValue xor(abstractValue v1, abstractValue v2);
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
	
	/**
	 * Conditional join. 
	 * When cond is null, it is simply a join between vtrue and vfalse. 
	 * When cond is not null, then it is a conditional join, such that if cond is true, the return value should be vtrue, and otherwise it should be vfalse. 
	 * @param cond
	 * @param vtrue
	 * @param vfalse
	 * @return
	 */
	abstract public abstractValue condjoin(abstractValue cond, abstractValue vtrue, abstractValue vfalse);
	abstract public void Assert(abstractValue val);
	abstract public void funcall(Function fun, List<abstractValue> avlist, List<abstractValue> outSlist);
}

