package sketch.compiler.main.passes;

import java.io.FileWriter;
import java.io.Writer;

import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.codegenerators.NodesToC;
import sketch.compiler.codegenerators.NodesToCTest;
import sketch.compiler.codegenerators.NodesToCUDA;
import sketch.compiler.codegenerators.NodesToH;
import sketch.compiler.main.seq.SequentialSketchMain;
import sketch.compiler.main.seq.SequentialSketchOptions;
import sketch.compiler.passes.printers.SimpleCodePrinter;
import sketch.compiler.passes.structure.ContainsCudaCode;

import static sketch.util.DebugOut.printDebug;
import static sketch.util.DebugOut.printError;
import static sketch.util.DebugOut.printNote;

/**
 * Print the solution to the terminal, or a testing harness
 * 
 * @author Armando Solar-Lezama
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
public class OutputCCode extends MetaStage {
    public OutputCCode(TempVarGen varGen, SequentialSketchOptions options) {
        super(varGen, options);
    }

    @Override
    public Program visitProgram(Program prog) {
        if (prog == null) {
            printError("Final code generation encountered error, skipping output");
            return prog;
        }

        String resultFile = SequentialSketchMain.getOutputFileName(options);
        final boolean tprintPyStyle = options.feOpts.tprintPython != null;
        String hcode = (String) prog.accept(new NodesToH(resultFile, tprintPyStyle));
        String ccode =
                (String) prog.accept(new NodesToC(varGen, resultFile, tprintPyStyle));

        if (!options.feOpts.outputCode && !options.feOpts.noOutputPrint) {
            prog.accept(new SimpleCodePrinter());
            // System.out.println(hcode);
            // System.out.println(ccode);
        } else if (!options.feOpts.noOutputPrint) {
            if (new ContainsCudaCode().run(prog)) {
                String cucode =
                        (String) prog.accept(new NodesToCUDA(varGen,
                                options.feOpts.outputDir + resultFile + ".cu",
                                tprintPyStyle));
                printDebug("CUDA code", cucode);
            }
            try {
                {
                    Writer outWriter =
                            new FileWriter(options.feOpts.outputDir + resultFile + ".h");
                    outWriter.write(hcode);
                    outWriter.flush();
                    outWriter.close();
                    outWriter =
                            new FileWriter(options.feOpts.outputDir + resultFile + ".cpp");
                    outWriter.write(ccode);
                    outWriter.flush();
                    outWriter.close();
                }
                if (options.feOpts.outputTest) {
                    String testcode =
                            (String) prog.accept(new NodesToCTest(resultFile,
                                    tprintPyStyle));
                    final String outputFname =
                            options.feOpts.outputDir + resultFile + "_test.cpp";
                    Writer outWriter = new FileWriter(outputFname);
                    outWriter.write(testcode);
                    outWriter.flush();
                    outWriter.close();
                    Writer outWriter2 =
                            new FileWriter(options.feOpts.outputDir + "script");
                    outWriter2.write("#!/bin/sh\n");
                    outWriter2.write("if [ -z \"$SKETCH_HOME\" ];\n"
                            + "then\n"
                            + "echo \"You need to set the \\$SKETCH_HOME environment variable to be the path to the SKETCH distribution; This is needed to find the SKETCH header files needed to compile your program.\" >&2;\n"
                            + "exit 1;\n" + "fi\n");
                    outWriter2.write("g++ -I \"$SKETCH_HOME/include\" -o " + resultFile +
                            " " + resultFile + ".cpp " + resultFile + "_test.cpp\n");

                    outWriter2.write("./" + resultFile + "\n");
                    outWriter2.flush();
                    outWriter2.close();
                    printNote("Wrote test harness to", outputFname);
                }
            } catch (java.io.IOException e) {
                throw new RuntimeException(e);
            }
        }
        return prog;
    }
}
