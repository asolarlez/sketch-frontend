package sketch.compiler.smt.cvc3;

import sketch.compiler.solvers.SolutionStatistics;
import sketch.util.ProcessStatus;

/**
 * This class is designed to collect solver statistics for CVC3 
 * @author Lexin Shan
 * @email lshan@eecs.berkeley.edu
 *
 */
public class Cvc3SolutionStatistics extends SolutionStatistics {
	
	boolean success = false;
	public Cvc3SolutionStatistics(ProcessStatus status) {
		String out = status.out;
		if (status.killedByTimeout)
			this.success = false;
		else if (out.contains("Satisfiable"))
			this.success = true;
		else
			this.success = false;
	}

	@Override
	public long elapsedTimeMs() {
		return this.solutionTimeMs;
	}

	@Override
	public long maxMemoryUsageBytes() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long modelBuildingTimeMs() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean successful() {
		return success;
	}

}
