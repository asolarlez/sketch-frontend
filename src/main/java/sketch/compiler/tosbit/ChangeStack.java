package streamit.frontend.tosbit;
import java.util.HashMap;


class ChangeStack{
	HashMap currTracker; //<String, int>
	ChangeStack kid;
	ChangeStack(){
		kid = null;
		currTracker = new HashMap();
	}
	boolean varHasValue(String var){
		varValue i = (varValue)currTracker.get(var);
		if(i != null)
			return i.hasValue;
		else
			return kid != null && kid.varHasValue(var); 
	}
	
	int varValue(String var){
		varValue i = (varValue)currTracker.get(var);
		if( i != null){
			assert(i.hasValue) : ("This variable has been set to top at this level, and consequently doesn't have value even though it exists.");
			return i.getValue();
		}else{
			return kid.varValue(var);
		}
	}	
	void setVarValue(String var, int v){
		varValue i = (varValue)currTracker.get(var);
		if(i == null)
			currTracker.put(var, new varValue(v) );
		else{
			i.setValue(v);
		}
	}
	public void unsetVarValue(String var, varState vs){
		varValue i = (varValue)currTracker.get(var);
		if(i!= null){
			i.hasValue = false;
		}else{
			i = new varValue();
			i.lastLHS = vs.getRHS();
			i.hasValue = false;
			currTracker.put(var, i);
		}
		/*
		if(kid != null){
			kid.unsetVarValue(var);
		}
		*/
	}
	
	void declVarLHS(String var, varState vs){
		varValue i = (varValue)currTracker.get(var);		
		if(i== null){
			i = new varValue();
			i.lastLHS = vs.getRHS();
			currTracker.put(var, i);
		}else{
			i.lastLHS = vs.getRHS();
		}
	}
}