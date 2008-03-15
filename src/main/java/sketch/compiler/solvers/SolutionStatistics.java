package streamit.frontend.solvers;

public abstract class SolutionStatistics extends Statistics {
	public abstract boolean successful ();

	public abstract long elapsedTimeMs ();
	public abstract long modelBuildingTimeMs ();
	public abstract long solutionTimeMs ();
	public abstract long maxMemoryUsageBytes ();

	public String toString () {
		return
"      [solution stats]\n"+
"      successful? ---------------------> "+ successful () +"\n"+
"      elapsed time (s) ----------------> "+ sec (elapsedTimeMs ()) +"\n"+
"      model building time (s) ---------> "+ sec (modelBuildingTimeMs ()) +"\n"+
"      solution time (s) ---------------> "+ sec (solutionTimeMs ()) +"\n"+
"      max memory usage (MiB) ----------> "+ MiB (maxMemoryUsageBytes ()) + "\n";
	}
}
