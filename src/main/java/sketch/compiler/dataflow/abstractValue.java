package sketch.compiler.dataflow;

import java.util.List;
/**
 * 
 * This is a value for abstract interpretation. 
 * 
 * @author asolar
 *
 */
public abstract class  abstractValue{
	protected boolean isVolatile = false;
	public boolean isVolatile(){ return isVolatile; }
	public void makeVolatile(){ isVolatile = true; }
	public void nonVolatile(){ isVolatile = false; }
	
	
	abstract public boolean isVect();
	abstract public List<abstractValue> getVectValue();
	abstract public boolean isBottom();
	abstract public int getIntVal();
    public boolean knownGeqZero() { return false; }
	abstract public boolean hasIntVal();
	abstract public void update(abstractValue v);
	abstract public abstractValue clone();
}
