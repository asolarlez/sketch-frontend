/**
 *
 */
package sketch.compiler.solvers;

/**
 * Basic statistics from the SAT backend.
 *
 * @author <a href="mailto:cgjones@cs.berkeley.edu">Chris Jones</a>
 */
public class SATSolutionStatistics extends SolutionStatistics {
	protected boolean success;
	protected long elapsedTimeMs;
	protected long modelBuildingTimeMs;
	protected long maxMemUsageBytes;

	protected long numNodesInitial;
	protected long numNodesFinal;
	protected long numControls;
	protected long numControlBits;

    public double probability;

	public long elapsedTimeMs () 	   {  return elapsedTimeMs;  }
	public long maxMemoryUsageBytes () {  return maxMemUsageBytes;  }
	public long modelBuildingTimeMs () {  return modelBuildingTimeMs;  }
	public long solutionTimeMs ()      {  return solutionTimeMs;  }
	public boolean successful () 	   {  return success;  }

	public String toString () {
		return super.toString () +
"      [SAT-specific solution stats]\n"+
"      initial number of nodes ---------> "+ numNodesInitial +"\n"+
"      number of nodes after opts ------> "+ numNodesFinal +"\n"+
"      number of controls --------------> "+ numControls +"\n"+
"      total number of control bits ----> "+ numControlBits +"\n";
	}
}
