/**
 *
 */
package streamit.frontend.solvers;

/**
 * @author <a href="mailto:cgjones@cs.berkeley.edu">Chris Jones</a>
 */
public class CompilationStatistics extends Statistics {
	protected long totalElapsedTimeMs;
	protected boolean resolved;
	/** An iteration is defined as a call to the synthesizer. */
	protected int nIterations;
	protected SolverStatistics synthStats;
	protected SolverStatistics verifStats;

	/** Frontend stats */
	protected long feTimeMs;
	protected long feMaxMemBytes;
	protected long startTimeMs;
	private MemorySamplerThread mt = new MemorySamplerThread ();

	public CompilationStatistics (SolverStatistics _synthStats,
								  SolverStatistics _verifStats) {
		startTimeMs = System.currentTimeMillis ();
		mt.start ();
		synthStats = _synthStats;
		verifStats = _verifStats;
	}

	public void finished (boolean _resolved) {
		resolved = _resolved;

		synchronized (mt) {
			feMaxMemBytes = mt.maxMemBytes;
			mt.stop = true;
			mt.notify ();
		}
		boolean done = false;
		while (!done)
			try {  mt.join ();  done = true;  } catch (InterruptedException ie) { }

		totalElapsedTimeMs = System.currentTimeMillis () - startTimeMs;
		feTimeMs = totalElapsedTimeMs
			- synthStats.totalElapsedTimeMs ()
			- verifStats.totalElapsedTimeMs ();
	}

	public void calledSynthesizer (SolutionStatistics solve) {
		nIterations++;
		synthStats.aggregate (solve);
	}

	public void calledVerifier (SolutionStatistics solve) {
		verifStats.aggregate (solve);
	}

	public int numIterations () 	  {  return nIterations;  }
	public long totalElapsedTimeMs () {  return totalElapsedTimeMs;  }

	public long maxMemoryUsageBytes () {
		return feMaxMemBytes
			+ Math.max (synthStats.maxMemoryUsageBytes (),
						verifStats.maxMemoryUsageBytes ());
	}

	public String toString () {
		return
"\n===== Compilation statistics =====\n"+
"  Solved? -----------------------------> "+ resolved +"\n"+
"  Number of iterations ----------------> "+ nIterations +"\n"+
"  Maximum memory usage (MiB) ----------> "+ MiB (maxMemoryUsageBytes ()) +"\n"+
"  Total elapsed time (s) --------------> "+ sec (totalElapsedTimeMs) +"\n"+
"    % frontend ............ "+ percent (feTimeMs, totalElapsedTimeMs) +"\n"+
"    % verification ........ "+ percent (verifStats.totalElapsedTimeMs (), totalElapsedTimeMs) +"\n"+
"    % synthesis ........... "+ percent (synthStats.totalElapsedTimeMs (), totalElapsedTimeMs) +"\n"+
"\n"+
"  Synthesizer statistics:  \n"+ synthStats +
"\n"+
"  Verifier statistics:     \n"+ verifStats +
"\n"+
"  Frontend statistics:     \n"+
"    Total elapsed time (s) ------------> "+ sec (feTimeMs) +"\n"+
"    Max memory usage (MiB) ------------> "+ MiB (feMaxMemBytes) +"\n";
	}

	private static final class MemorySamplerThread extends Thread {
		public long maxMemBytes;
		public boolean stop;

		public synchronized void run () {
			while (!stop) {
				maxMemBytes = Math.max (maxMemBytes,
										Runtime.getRuntime ().totalMemory ());
				try {  wait (1000);  } catch (InterruptedException ie) { }
			}
		}
	}

}
