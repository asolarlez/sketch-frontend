package sketch.compiler.smt.solvers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import sketch.compiler.solvers.SolutionStatistics;

public class YicesSolutionStatistics extends SolutionStatistics {

	private boolean success;

	public YicesSolutionStatistics(String output, String err) {
		success = !output.contains("unsat") && err.equals("");

		if (success) {
			Pattern p = Pattern.compile("([0-9]*[.]?[0-9]*) secs");
			Matcher matcher = p.matcher(output);

			if (matcher.find()) {
				Float sec = Float.parseFloat(matcher.group(1));
				long ms = (long) (sec * 1000L);
				setSolutionTimeMs(ms);
			}
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
