package streamit.frontend.tosbit;

class varValue{
	int lastLHS;
	private int value;
	boolean hasValue;
	varValue(){
		lastLHS=0;
		value = 0;
		hasValue = false;
	}
	varValue(int val){
		lastLHS=0;
		value = val;
		hasValue = true;
	}
	varValue(int val, int lhs){
		lastLHS=lhs;
		value = val;
		hasValue = true;
	}
	public boolean equals(Object o){
		varValue vv = (varValue)o;
		return (vv.hasValue && hasValue) && value == vv.value;
	}
	public void setValue(int value) {
		this.value = value;
		this.hasValue = true;
	}
	public int getValue() {
		return value;
	}
}