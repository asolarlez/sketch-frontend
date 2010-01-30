/**
 *
 */
package sketch.compiler.main.par;

import sketch.compiler.ast.core.Program;
import sketch.compiler.solvers.RandomSynthesizer;
import sketch.compiler.solvers.Synthesizer;

/**
 * PSKETCH will a "null" synthesizer that returns random candidates.
 *
 * Intended for testing only.
 *
 * @author <a href="mailto:cgjones@cs.berkeley.edu">Chris Jones</a>
 */
public class RandomParallelSketchMain extends ParallelSketchMain {
	public RandomParallelSketchMain (String[] args) {
		super (args);
	}

	public Synthesizer createSynth(Program p) {
		return new RandomSynthesizer (options, varGen);
	}

	public static void main(String[] args) {
		new RandomParallelSketchMain (args).run();
	}
}
