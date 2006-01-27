package streamit.frontend.tosbit;
import java.util.Iterator;
import java.util.List;

public class valueClass{
	public static final int NULL=0;
	public static final int INT=1;
	public static final int LIST=2;
	private final String name;
	private final int type;
	private final Object obj;
	
	
	public valueClass(String name){
		this.obj = null;
		this.type = NULL;
		this.name = name;
		assert name != null : "This should never happen!!!! Name should never be null.";
	}
	
	public valueClass(){
		this.obj = null;
		this.type = NULL;
		this.name = null;
	}
	public valueClass(Object obj, int type){
		this.obj = obj;
		this.type = type;
		this.name = null;
	}
	
	public valueClass(List<valueClass> obj){
		this.obj = obj;
		this.type = LIST;
		this.name = null;
	}	
	public valueClass(int obj){
		this.obj = obj;
		this.type = INT;
		this.name = null;
	}
	public boolean hasValue(){
		return type != NULL;
	}
	public boolean isVect(){
		return type == LIST;
	}
	public int getIntValue(){
		assert type == INT : "Incorrect value type";
		return ((Integer)this.obj).intValue();
	}
	
	@SuppressWarnings("unchecked")
	public List<valueClass> getVectValue(){
		assert type == LIST : "Incorrect value type";
		return (List<valueClass>)obj;
	}
	public String toString(){
		switch(type){
		case INT: return obj.toString();
		case LIST: {
			String rval = "$ ";
			for(Iterator<valueClass> it = getVectValue().iterator(); it.hasNext(); ){
				rval += it.next().toString() + " ";
			}
			rval += "$";
			return rval;
		}
		case NULL: return name;
		}
		return " ";
	}
}