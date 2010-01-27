package sketch.compiler.main.seq;

import sketch.compiler.cmdline.BoundOptions;
import sketch.compiler.cmdline.DebugOptions;
import sketch.compiler.cmdline.FrontendOptions;
import sketch.compiler.cmdline.SemanticsOptions;
import sketch.compiler.cmdline.SolverOptions;
import sketch.util.cli.CliParser;

public class SequentialSketchOptions {
    public BoundOptions bndOpts;
    public DebugOptions dbgOpts;
    public FrontendOptions feOpts;
    public SemanticsOptions semOpts;
    public SolverOptions slvOpts;

    public SequentialSketchOptions(String[] args) {
        CliParser parser = new CliParser(args);
        bndOpts = new BoundOptions();
        dbgOpts = new DebugOptions();
        feOpts = new FrontendOptions();
        semOpts = new SemanticsOptions();
        slvOpts = new SolverOptions();
        bndOpts.parse(parser);
        dbgOpts.parse(parser);
        feOpts.parse(parser);
        semOpts.parse(parser);
        slvOpts.parse(parser);
    }
}
