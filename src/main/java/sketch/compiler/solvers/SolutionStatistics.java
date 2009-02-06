package streamit.frontend.solvers;

/**
 * Statistics about a particular call to a solver (synthesizer or verifier).
 *
 * @author <a href="mailto:cgjones@cs.berkeley.edu">Chris Jones</a>
 */
public abstract class SolutionStatistics extends Statistics {
	protected long solutionTimeMs;

	public abstract boolean successful ();

	public abstract long elapsedTimeMs ();
	public abstract long modelBuildingTimeMs ();
	public abstract long maxMemoryUsageBytes ();

	public String toString () {
		return
"      [solution stats]\n"+
"      successful? ---------------------> "+ successful () +"\n"+
"      elapsed time (s) ----------------> "+ sec (elapsedTimeMs ()) +"\n"+
"      model building time (s) ---------> "+ sec (modelBuildingTimeMs ()) +"\n"+
"      solution time (s) ---------------> "+ sec (solutionTimeMs ()) +"\n"+
"      max memory usage (MiB) ----------> "+ MiB (maxMemoryUsageBytes ()) + "\n";
	}

	public long solutionTimeMs() {
		return this.solutionTimeMs;
	}

	public void setSolutionTimeMs(long timeMs) {
		this.solutionTimeMs = timeMs;
	}
}
