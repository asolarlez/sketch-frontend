package sketch.compiler.main.seq;

import sketch.compiler.cmdline.SMTBoundOptions;
import sketch.compiler.cmdline.SMTOptions;
import sketch.util.cli.SketchCliParser;

public class SMTSketchOptions extends SequentialSketchOptions {
    public SMTOptions smtOpts;
    public SMTBoundOptions bndOpts;

    public SMTSketchOptions(String[] inArgs) {
        super(inArgs);
    }

    @Override
    public void parseCommandline(SketchCliParser parser) {
        super.bndOpts = bndOpts = new SMTBoundOptions();
        smtOpts = new SMTOptions();
        smtOpts.parse(parser);
        super.parseCommandline(parser);
    }

    public static SMTSketchOptions getSingleton() {
        assert _singleton != null : "no singleton instance";
        return (SMTSketchOptions) _singleton;
    }
}
