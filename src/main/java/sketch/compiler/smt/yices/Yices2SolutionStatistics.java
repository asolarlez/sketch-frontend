package sketch.compiler.smt.yices;

import sketch.compiler.solvers.SolutionStatistics;

public class Yices2SolutionStatistics extends SolutionStatistics {
    
    private boolean success;
    
    public Yices2SolutionStatistics(String output, String err, long solutionTime) {
        success = !output.contains("unsat") && err.equals("");
        solutionTimeMs = solutionTime;
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
