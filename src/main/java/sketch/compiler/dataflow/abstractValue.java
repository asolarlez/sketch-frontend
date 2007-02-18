package streamit.frontend.experimental;

import java.util.List;

public abstract class  abstractValue{

	abstract public boolean isVect();
	abstract public List<abstractValue> getVectValue();
	abstract public boolean isBottom();
	abstract public int getIntVal();
	abstract public boolean hasIntVal();
	abstract public void update(abstractValue v);
	abstract public abstractValue clone();
}
