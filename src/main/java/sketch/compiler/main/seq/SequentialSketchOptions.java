package sketch.compiler.main.seq;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import sketch.compiler.cmdline.BoundOptions;
import sketch.compiler.cmdline.DebugOptions;
import sketch.compiler.cmdline.FrontendOptions;
import sketch.compiler.cmdline.SemanticsOptions;
import sketch.compiler.cmdline.SolverOptions;
import sketch.compiler.main.PlatformLocalization;
import sketch.util.cli.SketchCliParser;

/**
 * organized options for the sequential frontend. See ParallelSketchOptions for how to
 * inherit options effectively and concisely.
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
public class SequentialSketchOptions {
    public BoundOptions bndOpts = new BoundOptions();
    public DebugOptions debugOpts = new DebugOptions();
    public FrontendOptions feOpts = new FrontendOptions();
    public SemanticsOptions semOpts = new SemanticsOptions();
    public SolverOptions solverOpts = new SolverOptions();
    public String[] args;
    public List<String> argsAsList;
    public final String[] inArgs;
    public Vector<String> backendOptions;
    /** sketch file (corresponds to first arg) */
    public File sketchFile;
    /** nice name of the sketch */
    public String sketchName;
    public Vector<String> backendArgs;
    protected String[] currentArgs;
    protected static SequentialSketchOptions _singleton;

    public SequentialSketchOptions(String[] inArgs) {
        this.inArgs = inArgs;
        SketchCliParser parser = new SketchCliParser(inArgs);
        parseCommandline(parser);
        _singleton = this;
    }

    public void prependArgsAndReparse(String[] additionalArgs, boolean errorOnUnknown) {
        Vector<String> allArgs = new Vector<String>(Arrays.asList(additionalArgs));
        allArgs.addAll(Arrays.asList(currentArgs));
        parseCommandline(new SketchCliParser(allArgs.toArray(new String[0]),
                errorOnUnknown));
    }

    public void parseCommandline(SketchCliParser parser) {
        this.currentArgs = parser.inArgs;
        bndOpts.parse(parser);
        debugOpts.parse(parser);
        feOpts.parse(parser);
        semOpts.parse(parser);
        args = solverOpts.parse(parser).get_args();
        this.backendArgs = parser.backendArgs;
        if (args.length < 1 || args[0].equals("")) {
            parser.printHelpAndExit("no files specified");
        }
        
        // actions
        argsAsList = Arrays.asList(args);
        sketchFile = new File(args[0]);
        sketchName = sketchFile.getName().replaceFirst("\\.+$", "");
        feOpts.outputCode |= feOpts.outputTest;
    }

    @SuppressWarnings("unchecked")
    public Vector<String> getBackendOptions() {
        return (Vector<String>) backendOptions.clone();
    }

    public static SequentialSketchOptions getSingleton() {
        assert _singleton != null : "no singleton instance";
        return _singleton;
    }

    public static void resetSingleton() {
        _singleton = null;
    }

    public String getTmpSketchFilename() {
        return PlatformLocalization.getLocalization().getTempPathString(
                sketchName + ".tmp");
    }
}
