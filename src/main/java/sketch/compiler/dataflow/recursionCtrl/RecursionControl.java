package streamit.frontend.tosbit.recursionCtrl;
import streamit.frontend.nodes.ExprFunCall;
import streamit.frontend.nodes.Function;
import streamit.frontend.nodes.Statement;



public abstract class RecursionControl{
	public abstract void pushFunCall(ExprFunCall fc, Function fun);
	public abstract void popFunCall(ExprFunCall fun);
	public abstract int inlineLevel(ExprFunCall fun);
	public abstract boolean testBlock(Statement stmt);
	public abstract void doneWithBlock(Statement stmt);
	public abstract boolean testCall(ExprFunCall fc);
	
}