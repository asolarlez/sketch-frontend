package sketch.compiler.main.par;

import sketch.compiler.cmdline.ParallelOptions;
import sketch.compiler.main.seq.SequentialSketchOptions;
import sketch.util.cli.SketchCliParser;

/**
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
public class ParallelSketchOptions extends SequentialSketchOptions {
    public ParallelOptions parOpts;

    public ParallelSketchOptions(String[] inArgs) {
        super(inArgs);
    }

    @Override
    public void parseCommandline(SketchCliParser parser) {
        parOpts = new ParallelOptions();
        parOpts.parse(parser);
        super.parseCommandline(parser);
    }

    public static ParallelSketchOptions getSingleton() {
        assert _singleton != null : "no singleton instance";
        return (ParallelSketchOptions) _singleton;
    }
}
