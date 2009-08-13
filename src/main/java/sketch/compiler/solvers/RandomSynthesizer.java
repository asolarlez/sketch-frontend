/**
 *
 */
package sketch.compiler.solvers;

import sketch.compiler.CommandLineParamManager;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.solvers.constructs.AbstractValueOracle;
import sketch.compiler.solvers.constructs.RandomValueOracle;
import sketch.compiler.solvers.constructs.StaticHoleTracker;

/**
 * A synthesizer that uar picks a new candidate program.  There is no guarantee
 * that each candidate will be unique.
 *
 * This should never be used in a real system; it is for testing only.
 *
 * @author <a href="mailto:cgjones@cs.berkeley.edu">Chris Jones</a>
 */
public class RandomSynthesizer implements Synthesizer {
	protected CommandLineParamManager params;
	protected TempVarGen varGen;

	public RandomSynthesizer (CommandLineParamManager _params, TempVarGen _varGen) {
		params = _params;
		varGen = _varGen;
	}

	public void cleanup(){}
	public void initialize(){
	
	}
	
	public AbstractValueOracle nextCandidate (CounterExample cex) {
		return new RandomValueOracle (new StaticHoleTracker(varGen));
	}

	/** This method assumes that it takes 0 time to create a random solution. */
	public SolutionStatistics getLastSolutionStats () {
		return new SATSolutionStatistics ();
	}
}
