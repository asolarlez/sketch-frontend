/**
 *
 */
package streamit.frontend.solvers;

/**
 * @author <a href="mailto:cgjones@cs.berkeley.edu">Chris Jones</a>
 */
public class SpinSolutionStatistics extends SolutionStatistics {
	// The call to SPIN is considered successful if the program is verified.
	protected boolean success;

	// Stats for call to SPIN code generator
	protected long cgenTimeMs;
	protected long cgenMemBytes;

	// Stats for call to compiler
	protected long compilerTimeMs;
	protected long compilerMemBytes;

	// Stats for call to generated SPIN model checker
	protected long spinTimeMs;
	protected long spinTotalMemBytes;
	protected long spinActualStateMemBytes;
	protected long spinEquivStateMemBytes;
	protected float spinStateCompressionPct;
	protected long spinNumStates;
	protected long spinStateExplorationRate;	/* states/sec */

	public SpinSolutionStatistics () { }

	public long elapsedTimeMs () {
		return cgenTimeMs + compilerTimeMs + spinTimeMs;
	}

	public long maxMemoryUsageBytes () {
		// I hate Java.
		return Math.max (cgenMemBytes,
				Math.max (compilerMemBytes, spinTotalMemBytes));
	}

	public long modelBuildingTimeMs () {
		return cgenTimeMs + compilerTimeMs;
	}

	public long solutionTimeMs () {
		return spinTimeMs;
	}

	public boolean successful () {
		return success;
	}

	public String toString () {
		return super.toString () +
"      [SPIN-specific stats]\n"+
"      codegen time (s) ----------------> "+ sec (cgenTimeMs) +"\n"+
"      codegen memory (MiB) ------------> "+ MiB (cgenMemBytes) +"\n"+
"      compiler time (s) ---------------> "+ sec (compilerTimeMs) +"\n"+
"      compiler memory (MiB) -----------> "+ MiB (compilerMemBytes) +"\n"+
"      total SPIN time (s) -------------> "+ sec (spinTimeMs) +"\n"+
"      total SPIN mem (MiB) ------------> "+ MiB (spinTotalMemBytes) +"\n"+
"      actual state mem (MiB) ----------> "+ MiB (spinActualStateMemBytes) +"\n"+
"      equiv state mem (MiB) -----------> "+ MiB (spinEquivStateMemBytes) +"\n"+
"      state compression (%) -----------> "+ spinStateCompressionPct +"\n"+
"      # states explored ---------------> "+ spinNumStates +"\n"+
"      state exploration rate (states/s)> "+ spinStateExplorationRate + "\n";
	}
}
