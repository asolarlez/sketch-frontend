/**
 *
 */
package streamit.frontend.solvers;

import java.util.ArrayList;
import java.util.List;

/**
 * Statistics about a particular solver over the entire compilation run.
 *
 * These stats encompass all calls to the solver during compilation.
 *
 * @author <a href="mailto:cgjones@cs.berkeley.edu">Chris Jones</a>
 */
public class SolverStatistics extends Statistics {
	protected int ncalls;
	protected List<Long> solveTimesMs = new ArrayList<Long> ();
	protected List<Long> modelBuildingTimesMs = new ArrayList<Long> ();
	protected List<Long> solutionTimesMs = new ArrayList<Long> ();
	protected List<Long> memoryUsagesBytes = new ArrayList<Long> ();

	public void aggregate (SolutionStatistics solve) {
		ncalls++;
		solveTimesMs.add (solve.elapsedTimeMs ());
		modelBuildingTimesMs.add (solve.modelBuildingTimeMs ());
		solutionTimesMs.add (solve.solutionTimeMs ());
		memoryUsagesBytes.add (solve.maxMemoryUsageBytes ());
	}

	public void setAllStats(SolutionStatistics solve) {
		ncalls++;
		solveTimesMs.clear();
		modelBuildingTimesMs.clear();
		solutionTimesMs.clear();
		memoryUsagesBytes.clear();
		solveTimesMs.add (solve.modelBuildingTimeMs() + solve.solutionTimeMs());
		modelBuildingTimesMs.add (solve.modelBuildingTimeMs ());
		solutionTimesMs.add (solve.solutionTimeMs ());
		memoryUsagesBytes.add (solve.maxMemoryUsageBytes ());
	}
	
	public long numCalls () {
		return ncalls;
	}

	public long totalElapsedTimeMs () {
		return sum (solveTimesMs);
	}
	public long averageElapsedTimeMs () {
		return numCalls () > 0 ? totalElapsedTimeMs () / numCalls () : 0;
	}

	public long totalModelBuildingTimeMs () {
		return sum (modelBuildingTimesMs);
	}
	public long averageModelBuildingTimeMs () {
		return numCalls () > 0 ?totalModelBuildingTimeMs () / numCalls () : 0;
	}

	public long totalSolutionTimeMs () {
		return sum (solutionTimesMs);
	}
	public long averageSolutionTimeMs () {
		return numCalls () > 0 ? totalSolutionTimeMs () / numCalls () : 0;
	}

	public long maxMemoryUsageBytes () {
		return max (memoryUsagesBytes);
	}
	public long averageMemoryUsageBytes () {
		return numCalls () > 0 ? sum (memoryUsagesBytes) / numCalls () : 0;
	}

	public String toString () {
		return
"    [solver stats]\n" +
"    Number of calls -------------------> "+ numCalls () +"\n"+
"    Total elapsed time (s) ------------> "+ sec (totalElapsedTimeMs ()) +"\n"+
"    Total model building time (s) -----> "+ sec (totalModelBuildingTimeMs ()) +"\n"+
"    Total solution time (s) -----------> "+ sec (totalSolutionTimeMs ()) +"\n"+
"    Maximum memory usage (MiB) --------> "+ MiB (maxMemoryUsageBytes ()) +"\n"+
"    Average elapsed time (s) ----------> "+ sec (averageElapsedTimeMs ()) +"\n"+
"    Average model building time (s) ---> "+ sec (averageModelBuildingTimeMs ()) +"\n"+
"    Average solution time (s) ---------> "+ sec (averageSolutionTimeMs ()) +"\n"+
"    Average memory usage (MiB) --------> "+ MiB (averageMemoryUsageBytes ()) +"\n";
	}

	/** Less digested statistics> */
	public String toRawString () {
		return
"    [raw solver stats]\n" +
"    Number of calls -------------------> "+ numCalls () +"\n"+
"    Elapsed times (ms) ----------------> "+ solveTimesMs +"\n"+
"    Model building times (ms) ---------> "+ modelBuildingTimesMs +"\n"+
"    Solution times (ms) ---------------> "+ solutionTimesMs +"\n"+
"    Memory usages (bytes) -------------> "+ memoryUsagesBytes +"\n";
	}

}
