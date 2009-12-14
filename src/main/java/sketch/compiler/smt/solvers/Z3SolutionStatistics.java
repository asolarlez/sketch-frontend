package sketch.compiler.smt.solvers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import sketch.compiler.solvers.SolutionStatistics;

public class Z3SolutionStatistics extends SolutionStatistics {
	
	boolean success;
	
	public Z3SolutionStatistics(String output, String err) {
		success = !output.contains("unsat");
		
		Pattern timingPat = Pattern.compile("time:\\s*([0-9]*.[0-9]*) secs");
		Matcher m = timingPat.matcher(output);
		if (m.find()) {
			this.solutionTimeMs = (long) (Float.parseFloat(m.group(1)) * 1000f);
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
	public long solutionTimeMs() {
		return this.solutionTimeMs;
	}

	@Override
	public boolean successful() {
		return success;
	}

}
