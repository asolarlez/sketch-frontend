package sketch.compiler.smt.stp;

import sketch.compiler.solvers.SolutionStatistics;

public class STPSolutionStatistics extends SolutionStatistics {

	private boolean success;
	
	public STPSolutionStatistics(String stdout, String stderr) {
		this.success = stdout.contains("Invalid");
		
		String prefix = "CPU time              :";
		int cpuPrefixIdx = stderr.indexOf(prefix);
		
		if (cpuPrefixIdx >= 0) {
		    int startIdx = cpuPrefixIdx + prefix.length();
		    int endIdx = stderr.indexOf("s", cpuPrefixIdx);
		
		    String timeStr = stderr.substring(startIdx, endIdx);
		    this.setSolutionTimeMs((long) (Float.parseFloat(timeStr) * 1000));
		} else {
		    this.setSolutionTimeMs(0);
		}

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
	public boolean successful() {
		return success;
	}

}
