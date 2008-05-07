package streamit.frontend.solvers;

import java.util.ArrayList;
import java.util.List;

public class CEtrace extends CounterExample {
	public static final class step{
		final int thread;
		final int stmt;
		public step(int thread, int stmt){
			this.thread = thread;
			this.stmt = stmt;
		}
		public String toString(){
			return "(" + thread + ", " + stmt + ")";
		}
	}

	public List<step> steps;
	public List<step> blockedSteps;

	public CEtrace () {
		steps = new ArrayList<step> ();
		blockedSteps = new ArrayList<step> ();
	}

	public void addStep (int thread, int stmt) {
		steps.add (new step (thread, stmt));
	}

	public void addBlockedStep (int thread, int stmt) {
		blockedSteps.add (new step (thread, stmt));
	}

	public void addSteps (List<step> _steps) {
		steps.addAll (_steps);
	}

	public void addBlockedSteps (List<step> _steps) {
		blockedSteps.addAll (_steps);
	}

	public String toString(){
		return "(t,s)" + steps.toString() + "; [blocked: "+ blockedSteps.toString () + "]";
	}

	public boolean deadlocked () { return false; }
}
