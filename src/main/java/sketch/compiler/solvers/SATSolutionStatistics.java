/**
 *
 */
package streamit.frontend.solvers;

/**
 * Basic statistics from the SAT backend.
 *
 * @author <a href="mailto:cgjones@cs.berkeley.edu">Chris Jones</a>
 */
public class SATSolutionStatistics extends SolutionStatistics {
	protected boolean success;
	protected long elapsedTimeMs;
	protected long modelBuildingTimeMs;
	protected long solutionTimeMs;
	protected long maxMemUsageBytes;

	public long elapsedTimeMs () 	   {  return elapsedTimeMs;  }
	public long maxMemoryUsageBytes () {  return maxMemUsageBytes;  }
	public long modelBuildingTimeMs () {  return modelBuildingTimeMs;  }
	public long solutionTimeMs ()      {  return solutionTimeMs;  }
	public boolean successful () 	   {  return success;  }
}
