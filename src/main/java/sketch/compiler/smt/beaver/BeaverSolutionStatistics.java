package sketch.compiler.smt.beaver;

import sketch.compiler.solvers.SolutionStatistics;

// TODO: Make more robust?
public class BeaverSolutionStatistics extends SolutionStatistics {
	
	boolean success;
	
	public BeaverSolutionStatistics(String output) {
		success = output.contains("sat");
	}

	@Override
	public long elapsedTimeMs() {
		// TODO Auto-generated method stub
		return 0;
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
	public long solutionTimeMs() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean successful() {
		return success;
	}

}
