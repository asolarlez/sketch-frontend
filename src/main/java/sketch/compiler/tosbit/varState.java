package streamit.frontend.tosbit;
import java.util.Stack;


class varState{
	
	private String name;
	boolean hasval;
	private boolean isarray=false;
	private int arsize =-1;
	private int value;
	private int currentLHS;
	private int currentRHS;
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