package streamit.frontend.tosbit;

import java.util.HashMap;
import streamit.frontend.nodes.Expression;
import streamit.frontend.tosbit.valueClass;


class ChangeStack{
	HashMap currTracker; //<String, int>
	ChangeStack kid;

    /* Gilad, 2005-08-11: allow both syntactic expression and value of conditional
     * to be attached to tracker block; that is until I decide which one of them
     * is really needed...
     */
    private Expression condExpr;
    private valueClass condVal;

	ChangeStack (Expression newCondExpr, valueClass newCondVal){
		kid = null;
		currTracker = new HashMap ();
        condExpr = newCondExpr;
        condVal = newCondVal;
	}

    ChangeStack () {
        this (null, null);
    }
	
	boolean knowsAbout(String var){
		varValue i = (varValue)currTracker.get(var);
		if(i != null)
			return true;
		else
			return kid != null && kid.knowsAbout(var); 
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

    boolean hasCondExpr () {
        return (condExpr != null);
    }

    boolean hasCondVal () {
        return (condVal != null);
    }

    Expression getCondExpr () {
        return condExpr;
    }

    valueClass getCondVal () {
        return condVal;
    }
}
