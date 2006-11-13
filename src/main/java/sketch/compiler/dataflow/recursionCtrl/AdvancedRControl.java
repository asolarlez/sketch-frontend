package streamit.frontend.tosbit.recursionCtrl;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import streamit.frontend.nodes.ExprFunCall;
import streamit.frontend.nodes.FEReplacer;
import streamit.frontend.nodes.Function;
import streamit.frontend.nodes.Program;
import streamit.frontend.nodes.Statement;
import streamit.frontend.nodes.StmtFor;
import streamit.frontend.nodes.StmtIfThen;
import streamit.frontend.nodes.StreamSpec;

public class AdvancedRControl extends RecursionControl {
	
	Stack<Integer> bfStack;
	int branchingTheshold;
	private int MAX_INLINE;
	Map<String, FunInfo> funmap;
	
	/*
	 * For each function, we must keep the following information:
	 * - Current recursion depth
	 * - Does it make subcalls
	 */
	private static class FunInfo{
		int rdepth;
		final boolean isTerminal;
		FunInfo(boolean isTerminal){
			this.isTerminal = isTerminal;
			rdepth = 0;
		}
		public String toString(){
			return "(" + rdepth + "," + (isTerminal? "T" : "NT") + ")";
		}
	}
	
	
	
	private class PopFunMap extends FEReplacer{
		String currentFun;
		int currentCalls;
		StreamSpec ss;
		PopFunMap(){
			funmap = new HashMap<String, FunInfo>();
		}
		 public Object visitStreamSpec(StreamSpec spec){
			 ss = spec;
			 return super.visitStreamSpec(spec);
		 }
		public Object visitFunction(Function func){
			if(func.getSpecification() != null){
				func = ss.getFuncNamed(func.getSpecification());
			}
			currentFun = func.getName();
			currentCalls = 0;
			Object obj = super.visitFunction(func);
			funmap.put(currentFun, new FunInfo(currentCalls==0));
			return obj;
		}
		
		public Object visitExprFunCall(ExprFunCall exp)
	    {
			currentCalls++;
			return super.visitExprFunCall(exp);
	    }
	}
	
	
	private class CheckBFandCalls extends FEReplacer{
		int bfactor = 0;
		boolean forbiddenCalls = false;
		public Object visitStmtIfThen(StmtIfThen stmt)
	    {
			//We don't want to look into If Statements. We don't know if they'll execute.
			return stmt;
	    }
		
		public Object visitStmtFor(StmtFor stmt)
	    {
			//We don't want to look into Loops either. We don't know if they'll execute.
			return stmt;
	    }
		public Object visitExprFunCall(ExprFunCall exp)
	    {
			if( !(funmap.get(exp.getName()).isTerminal )){
				++bfactor;
			}
			if( ! testCall(exp) ){
				forbiddenCalls = true;
			}
			return exp;
	    }
		
	}
	
	
	
	public AdvancedRControl(int branchingThreshold, int maxInline, Program prog){
		this.branchingTheshold = branchingThreshold;
		bfStack = new Stack<Integer>();
		bfStack.push(1);
		prog.accept(new PopFunMap());
		MAX_INLINE = maxInline;		
	}
	
	
	
	private boolean bfactorTest(int bf){
		int p = bfStack.peek();	
		p = p*bf;
		if( p  > branchingTheshold){
			return false;
		}else{
			bfStack.push(p);
			return true;
		}
	}
		
	public void doneWithBlock(Statement stmt) {
		bfStack.pop();

	}

	int tt = 0;
	@Override
	public int inlineLevel(ExprFunCall fun) {
		FunInfo fi = funmap.get(fun.getName());
		return fi.rdepth;		
	}

	@Override
	public void popFunCall(ExprFunCall fun) {
		FunInfo fi = funmap.get(fun.getName());
		fi.rdepth--;
		--tt;
	}


	public void pushFunCall(ExprFunCall fc, Function fun) {
		FunInfo fi = funmap.get(fc.getName());
		if( ! fi.isTerminal ){
			for(int i=0; i<tt; ++i) System.out.print("  ");
			System.out.println(fc.getName());
		}
		++tt;
		fi.rdepth++;
	}


	public boolean testBlock(Statement stmt) {
		/* First, we check if the block is legal. I.e. if it has any
		 * function calls that will surpass their max iteration depth.
		 */		
		CheckBFandCalls check = new CheckBFandCalls();
		stmt.accept(check);
		if( ! check.forbiddenCalls ){
			/*
			 *  If it is, then we check the branching factor. That's the number
			 *  of non-terminal calls made by the block.  
			 */
			int bfactor = check.bfactor;
			/*
			 * Then we test the cummulative branching factor. This is the 
			 * product of all the elements in bfStack * bfactor. 
			 * If it is larger than 
			 * a threshold, we return false. Otherwise, we push bfactor into the
			 * bfStack and return true.
			 * 
			 */						
			return bfactorTest(bfactor);			
		}
		return false;
	}


	public boolean testCall(ExprFunCall fc) {
		FunInfo fi = funmap.get(fc.getName());		
		if( fi.rdepth < MAX_INLINE ){
			return true;
		}else{
			return false;
		}		
	}

}
