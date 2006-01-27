package streamit.frontend.tosbit;

import java.util.Iterator;
import java.util.Stack;

public class LoopMap {
	private Stack<Integer> loopBounds;
	private Stack<Integer> currentIter;	
	public LoopMap() {
		super();
		loopBounds = new Stack<Integer>();
		currentIter = new Stack<Integer>();	
	}
	
	public void pushLoop(int iters){
		loopBounds.push(iters);
		currentIter.push(0);
	}
	
	public void popLoop(){
		loopBounds.pop();
		currentIter.pop();
	}
	
	public void nextIter(){
		int i = currentIter.pop();
		currentIter.push(i+1);
		if( loopBounds.peek() >= (i+1) ){
			loopBounds.pop();
			loopBounds.push(i+2);
		}
	}
	
	public int[] getCoords(){
		int [] rval = new int[currentIter.size()];
		int i=0;
		for(Iterator<Integer> it = currentIter.iterator(); it.hasNext(); i++ ){
			rval[i] = it.next();
		}
		return rval;
	}
	
	public int[] getBounds(){
		int [] rval = new int[loopBounds.size()];
		int i=0;
		for(Iterator<Integer> it = loopBounds.iterator(); it.hasNext(); i++ ){
			rval[i] = it.next();
		}
		return rval;		
	}
	
	public int[] extendBounds(int[] oldBounds){
		if(oldBounds == null){
			return getBounds();
		}
		assert oldBounds.length == loopBounds.size() : "Bad parameter";		
		int i=0;
		for(Iterator<Integer> it = loopBounds.iterator(); it.hasNext(); i++ ){
			int cur = it.next();
			if( oldBounds[i] <  cur)
				oldBounds[i] = cur; 
		}
		return oldBounds;
	}
}
