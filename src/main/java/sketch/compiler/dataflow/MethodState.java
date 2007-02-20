package streamit.frontend.experimental;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.Map.Entry;


import streamit.frontend.nodes.Expression;
import streamit.frontend.nodes.Type;






public class MethodState {
	public class ChangeTracker{
		protected ChangeTracker kid;
		protected abstractValue condition;
		protected Map<String, varState> deltas;
		
		
		public void remove(String var){
			deltas.remove(var);
		}
		
		ChangeTracker(abstractValue cond, boolean isNegated){
			deltas = new HashMap<String, varState>();
			condition = cond;
			if( isNegated){
				condition = vtype.not(condition);
			}
		}
		
		public boolean hasCondVal(){
			return condition != null;
		}
		
		public abstractValue getCondVal(){
			return condition;
		}
		/*
		ChangeTracker pushChangeTracker(abstractValue cond, boolean isNegated){
			ChangeTracker tmp = new ChangeTracker( cond,  isNegated);
			tmp.kid = this;
			return tmp;
		}
		*/
		public void setVarValue(String var, abstractValue val){
			varState current = null;
			if( !deltas.containsKey(var) ){
				current = UTvarState(var);
				current = current.getDeltaClone(vtype);
				deltas.put(var, current );				
			}else{
				current = deltas.get(var);
			}
			current.update(val, vtype);
		}
		
		public void setVarValue(String var, abstractValue idx, abstractValue val){
			varState current = null;
			if( !deltas.containsKey(var) ){
				current =  UTvarState(var);
				current = current.getDeltaClone(vtype);
				deltas.put(var, current );				
			}else{
				current = deltas.get(var);
			}
			current.update(idx, val, vtype);
		}
		
		boolean knowsAbout(String var){
			varState i = deltas.get(var);
			if(i != null)
				return true;
			else
				return kid != null && kid.knowsAbout(var); 
		}
		
		
		public abstractValue varValue(String var){
			varState i = deltas.get(var);
			if( i != null){
				return i.state(vtype);
			}else{
				return kid.varValue(var);
			}
		}
		
		public varState varState(String var){
			varState i = deltas.get(var);
			if( i != null){
				return i;
			}else{
				return kid.varState(var);
			}
		}

	}
	
	abstractValueType vtype;
	
	/**
	 * Maps the unique name of a variable (the one returned by the varTranslator)
	 * into a varState.
	 * 
	 * Varstates are created when you call varGetLHSName for the first time.
	 */
	private HashMap<String, varState> vars;
	/**
	 * Translates the name of a variable from it's program name
	 * to a unique name that takes scope into account.
	 */
	private MapStack varTranslator;
	/**
	 * Used for handling if statements. 
	 * Keeps track of what variables are changed, so that at the
	 * join point you can merge the values for any variables that 
	 * changed on either branch.
	 */
	private ChangeTracker changeTracker;
	
	public MethodState(abstractValueType vtype){
		// System.out.println("New Method State for new method.");
		vars = new HashMap<String, varState>();					
		changeTracker = null;
		varTranslator = new MapStack();
		this.vtype = vtype; 
	}
	
	public String transName(String nm){       	
    	String otpt = varTranslator.transName(nm);
    	// System.out.println(nm + " = " +  otpt);
    	return  otpt;
    }
	
	private void UTsetVarValue(String var, abstractValue val){		
		varState tv =  vars.get(var);
		assert(tv != null) : ( " This should never happen, because before seting the value of "+ var + ", you should have requested a LHS name. Or, alternatively, if this is a ++ increment, then you can't increment if it doesn't have an initial value, in which case tv would also not be null.");
		if(changeTracker != null){
			changeTracker.setVarValue(var, val);
		}else{
			tv.update(val, vtype);			
		}
	}
	
	private void UTsetVarValue(String var, abstractValue idx, abstractValue val){		
		if(changeTracker != null){
			changeTracker.setVarValue(var, idx, val);
		}else{
			varState tv =  vars.get(var);			
			assert(tv != null) : ( " This should never happen, because before seting the value of "+ var + ", you should have requested a LHS name. Or, alternatively, if this is a ++ increment, then you can't increment if it doesn't have an initial value, in which case tv would also not be null.");
			tv.update(idx, val, vtype);
		}
	}
	private abstractValue UTvarValue(String var){
		varState i =  UTvarState(var);
		return i.state(vtype);
	}
	
	private varState UTvarState(String var){
		varState i =  vars.get(var);		
		if(changeTracker == null){	
			assert i != null : "The variable " + var + " is causing problems";		
			return i;
		}else{
			if( changeTracker.knowsAbout(var) ){
				return changeTracker.varState(var);
			}else{
				assert(i != null) : ( "The value of " + var + " is input dependent, but it's not supposed to be.\n");
				return i;
			}
		}
	}
	
	
	public void procChangeTrackers (ChangeTracker ch1){
		Iterator<Entry<String, varState>> it2 = ch1.deltas.entrySet().iterator();
		while(it2.hasNext()){
			Entry<String, varState> me =  it2.next();
			varState av2 = me.getValue();
			varState oldstate = this.UTvarState(me.getKey());
			varState merged = oldstate.condjoin(ch1.condition, av2, vtype);
			if( merged.isArr() ){
				for(Iterator<Entry<Integer, abstractValue>> ttt = merged.iterator(); ttt.hasNext(); ){
					Entry<Integer, abstractValue> tmp  = ttt.next();
					this.UTsetVarValue(me.getKey(), vtype.CONST(tmp.getKey()),  tmp.getValue() );
				}
			}else{
				this.UTsetVarValue(me.getKey(),   merged.state(vtype) );					
			}
		}
	}
	public void procChangeTrackers (ChangeTracker ch1, ChangeTracker ch2){
		Iterator<Entry<String, varState>> it = ch1.deltas.entrySet().iterator();
		while(it.hasNext()){
			Entry<String, varState> me =  it.next();
			String nm = me.getKey();
			varState av1 = me.getValue();
			if(ch2.deltas.containsKey( nm )){
				//This means the me.getKey() was modified on both branches.				
				varState av2 = ch2.deltas.get( nm );
				varState merged = av2.condjoin(ch1.condition, av1, vtype);
				
				if( merged.isArr() ){
					for(Iterator<Entry<Integer, abstractValue>> ttt = merged.iterator(); ttt.hasNext(); ){
						Entry<Integer, abstractValue> tmp  = ttt.next();
						this.UTsetVarValue(me.getKey(), vtype.CONST(tmp.getKey()),  tmp.getValue() );
					}
				}else{
					this.UTsetVarValue(me.getKey(),   merged.state(vtype) );					
				}
				ch2.deltas.remove(me.getKey());
			}else{
				varState oldstate = this.UTvarState(me.getKey());
				varState merged = oldstate.condjoin(ch1.condition, av1, vtype);
				if( merged.isArr() ){
					for(Iterator<Entry<Integer, abstractValue>> ttt = merged.iterator(); ttt.hasNext(); ){
						Entry<Integer, abstractValue> tmp  = ttt.next();
						this.UTsetVarValue(me.getKey(), vtype.CONST(tmp.getKey()),  tmp.getValue() );
					}
				}else{
					this.UTsetVarValue(me.getKey(),   merged.state(vtype) );					
				}
			}
		}
		//Now, at this point, we have removed from ch2 all the items
		//that were also in ch1. So all the ones that are left in ms2
		//Are the ones that are in ms2 alone. 
		//Once again, if we wanted to be conservative, we would just
		//unset them all, but we'll see if we can get away with being nice
		//and only unset them if they are actually different from what they were originally.
		//This is checked, just as before, by checkAndUnset(...);
		procChangeTrackers(ch2);
	}
	
	
	public boolean compareChangeTrackers(ChangeTracker ch1, ChangeTracker ch2){
		{
			Iterator<Entry<String, varState>> it = ch1.deltas.entrySet().iterator();
			while(it.hasNext()){
				Entry<String, varState> me =  it.next();
				String nm = me.getKey();
				varState av1 = me.getValue();
				if(ch2.deltas.containsKey( nm )){
					//This means the me.getKey() was modified on both branches.				
					varState av2 = ch2.deltas.get( nm );
					
					if( !av1.compare(av2, vtype) ) return false;				
				}else{
					varState av2 = this.UTvarState(me.getKey());
					if( !av1.compare(av2, vtype) ) return false;
				}
			}
		}
		
		{
			Iterator<Entry<String, varState>> it = ch2.deltas.entrySet().iterator();
			while(it.hasNext()){
				Entry<String, varState> me =  it.next();
				String nm = me.getKey();
				varState av2 = me.getValue();
				if(!ch1.deltas.containsKey( nm )){
					varState av1 = this.UTvarState(me.getKey());
					if( !av1.compare(av2, vtype) ) return false;
				}
			}
		}
		return true;
	}
	
	
	public void Assert(abstractValue val){
        /* Compose complex expression by walking all nesting conditionals. */        
        for (ChangeTracker tmpTracker = changeTracker;
        		tmpTracker != null; tmpTracker = tmpTracker.kid )
        {
            if (! tmpTracker.hasCondVal ())
                continue;
            abstractValue nestCond = vtype.not(tmpTracker.getCondVal ());
            val = vtype.or(val, nestCond );
        }        
		vtype.Assert(val);
	}
	
	public void setVarValue(String var, abstractValue idx, abstractValue val){
		var = this.transName(var);
		if( idx != null)
			UTsetVarValue(var, idx, val);
		else
			UTsetVarValue(var, val);
	}
	
	
	public ChangeTracker popChangeTracker(){
		ChangeTracker tmp = changeTracker;
		changeTracker = changeTracker.kid;
		return tmp;
	}
	
	
	
	public void pushChangeTracker (abstractValue cond, boolean isNegated){	
		/* Add new change tracker layer, including nesting conditional
		 * expression and value. */
		ChangeTracker oldChangeTracker = changeTracker;
		changeTracker = new ChangeTracker (cond, isNegated);
		changeTracker.kid = oldChangeTracker;		
	}
	
	
	public void varDeclare(String var, Type t){
//		 System.out.println("DECLARED " + var);
		assert var != null : "NOO!!";
		String newname = varTranslator.varDeclare(var);	
		assert !vars.containsKey(newname): "You are redeclaring variable "  + var + ":" + t + "   " + newname;
		varState	tv = vtype.cleanState(newname, t);
		vars.put(newname, tv);
	}

	public abstractValue varValue(String var){
		assert var != null : "NOO!!";
		var = this.transName(var);
		return UTvarValue(var);		
	}
		
	public void setVarValue(String var, abstractValue val){
		var = this.transName(var);
		UTsetVarValue(var,val);
	}
	
	public void pushLevel(){    	
    	varTranslator = varTranslator.pushLevel();    	    	    	
    }
	
	public void popLevel(){
    	varTranslator = varTranslator.popLevel(vars, changeTracker);
    }
	
	public void beginFunction(String fname){
		pushLevel();
	}
	
	public void endFunction(){
		popLevel();
	}
	
}
