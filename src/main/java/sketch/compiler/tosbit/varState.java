package streamit.frontend.tosbit;
import java.util.Stack;

/**
 * 
 * This class defines the state of a variable. 
 * In particular, it says whether it is a regular variable or an entry
 * in an array, and it also keeps track of the read and write pointers (as in CF).
 * 
 * @author asolar
 *
 */


class varState{
	
	private String name;
	boolean hasval;
	private boolean isarray=false;
	private int arsize =-1;
	private int value;
	/** 
	 * 
	 *This variable has the idx to use next time this variable
	 *is written to. It's like the write pointer in CF.
	 *@invariant currentLHS = { currentRHS | currentRHS+1}
	 */
	private int currentLHS;
	private int currentRHS;
	
	/**
	 * The RHSstack is used to keep track of what the value of the RHS
	 * was the last time a RHS was pushed. 
	 * 
	 * This is usefull when handling IFs, because then you push the RHS before
	 * goint into the THEN part, pop, then push again to go into the ELSE part,
	 * and this way the else part can start with the same rhs count as the then part.
	 * 
	 *  We don't need a stack for the values because those are handled by the changeTracker.
	 * 
	 */
	private Stack RHSstack;
	
	public varState(int v){
		value = v;
		currentLHS = 0;
		currentRHS = 0;
		RHSstack = new Stack();
		hasval = true;
	}
	
	public varState(String nm, int v){
		this.name = nm;
		value = v;
		currentLHS = 0;
		currentRHS = 0;
		RHSstack = new Stack();
		hasval = true;
	}

	public varState(String nm){
		this.name = nm;
		value = 0;
		currentLHS = 0;
		currentRHS = 0;
		RHSstack = new Stack();
		hasval = false;
	}
	
	public void setVal(int v){
		value = v;
		hasval = true;
	}
	
	public boolean hasVal(){
		return hasval;
	}
	
	public int getVal(){
		return value;
	}
	
	public String getRHSName(){
		return name + "_" + currentRHS;
	}
	
	public String getRHSName(int temporaryRHS){
		return name + "_" + temporaryRHS;
	}
	
	public int getRHS(){
		return currentRHS;
	}
	
	public String getLHSName(){
		String rv = name + "_" + currentLHS;
		currentRHS = currentLHS;
		++currentLHS;
		return rv;		
	}
		
	public void pushRHS(){
		RHSstack.push(new Integer(currentRHS));		
	}	
	public boolean popRHS(){
		if(RHSstack.size() > 0){
			currentRHS =  ((Integer)RHSstack.pop()).intValue();
			return true;
		}else
			return false;
	}

	/**
	 * @param isarray The isarray to set.
	 */
	public void setIsarray(boolean isarray, int size) {
		this.arsize = size;
		this.isarray = isarray;
	}
	
	
	public int getArrSize(){
		return arsize;
	}

	/**
	 * @return Returns the isarray.
	 * 
	 */
	public boolean isarray() {
		return isarray;
	}		
}