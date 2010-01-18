package sketch.compiler.smt.solvers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

import sketch.compiler.solvers.SolutionStatistics;

public class STPSolutionStatistics extends SolutionStatistics {

	private boolean success;
	public final static boolean newSTP = true;
	
	public STPSolutionStatistics(String stdout, String stderr) {
		this.success = stdout.contains("Invalid");
		
		if (newSTP) {
		    BufferedReader sr = new BufferedReader(new StringReader(stderr));
		    String line = null;
		    long sum = 0;
		    try {
                while ((line = sr.readLine()) != null) {
                    int start = line.indexOf('[');
                    int end = line.indexOf("ms]");
                    if (start >= 0 && end >=0 && end > start) {
                        long time = Integer.parseInt(line.substring(start+1, end));
                        sum += time;
                    }
                }
            } catch (NumberFormatException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
		    this.setSolutionTimeMs(sum);
		    
		} else {
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
