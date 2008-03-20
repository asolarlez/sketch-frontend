/**
 *
 */
package streamit.frontend;

import streamit.frontend.ToPSbitII;
import streamit.frontend.nodes.Program;
import streamit.frontend.solvers.RandomSynthesizer;
import streamit.frontend.solvers.Synthesizer;

/**
 * PSKETCH will a "null" synthesizer that returns random candidates.
 *
 * Intended for testing only.
 *
 * @author <a href="mailto:cgjones@cs.berkeley.edu">Chris Jones</a>
 */
public class NullSynthPSketch extends ToPSbitII {
	public NullSynthPSketch (String[] args) {
		super (args);
	}

	public Synthesizer createSynth(Program p){
		return new RandomSynthesizer (params, varGen);
	}

	public static void main(String[] args) {
		new NullSynthPSketch (args).run();
		System.exit(0);
	}
}
