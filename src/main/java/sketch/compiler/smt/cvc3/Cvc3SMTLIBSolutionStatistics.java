package sketch.compiler.smt.cvc3;

import sketch.util.ProcessStatus;

public class Cvc3SMTLIBSolutionStatistics extends Cvc3SolutionStatistics {

	public Cvc3SMTLIBSolutionStatistics(ProcessStatus status) {
		super(status);
		String out = status.out;
		if (status.err.isEmpty() && out.contains("sat"))
			this.success = true;
		else
			this.success = false;
		
	}
	
	

}
