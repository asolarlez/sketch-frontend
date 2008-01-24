package streamit.frontend.solvers;

import java.util.List;

public class CEtrace extends CounterExample {
	public static final class step{
		final int thread;
		final int stmt;
		public step(int thread, int stmt){
			this.thread = thread;
			this.stmt = stmt;
		}
	}
	public List<step> steps; 
}
