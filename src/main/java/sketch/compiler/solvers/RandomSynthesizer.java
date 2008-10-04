/**
 *
 */
package streamit.frontend.solvers;

import streamit.frontend.CommandLineParamManager;
import streamit.frontend.nodes.TempVarGen;
import streamit.frontend.stencilSK.StaticHoleTracker;
import streamit.frontend.tosbit.AbstractValueOracle;
import streamit.frontend.tosbit.RandomValueOracle;

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

	public AbstractValueOracle nextCandidate (CounterExample cex) {
		return new RandomValueOracle (new StaticHoleTracker(varGen));
	}

	/** This method assumes that it takes 0 time to create a random solution. */
	public SolutionStatistics getLastSolutionStats () {
		return new SATSolutionStatistics ();
	}
}
