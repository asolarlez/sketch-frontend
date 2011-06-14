package sketch.compiler.main.passes;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.codegenerators.NodesToC;
import sketch.compiler.codegenerators.NodesToCPrintTest;
import sketch.compiler.codegenerators.NodesToH;
import sketch.compiler.main.PlatformLocalization;
import sketch.compiler.main.seq.SequentialSketchMain;
import sketch.compiler.main.seq.SequentialSketchOptions;
import sketch.compiler.passes.structure.GetPrintFcns;
import sketch.compiler.passes.structure.GetTprintIdentifiers;
import sketch.compiler.passes.structure.TprintIdentifier;
import sketch.util.ProcessStatus;
import sketch.util.SynchronousTimedProcess;
import sketch.util.datastructures.TypedHashSet;
import sketch.util.exceptions.ExceptionAtNode;

import static sketch.util.DebugOut.printNote;

/**
 * Create a C++ harness and execute it, in order to obtain output for tprint calls in
 * printfcn's
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
public class RunPrintFunctions extends MetaStage {
    public RunPrintFunctions(TempVarGen varGen, SequentialSketchOptions options) {
        super(varGen, options);
    }

    @Override
    public Program visitProgramInner(Program serializedCode) {
        String resultFile = SequentialSketchMain.getOutputFileName(options);
        final boolean tprintPyStyle = options.feOpts.tprintPython != null;
        StringBuilder pyCode = new StringBuilder();

        // initial python code
        pyCode.append("class Message(object):\n"
                + "    def __init__(self, value, **kwargs):\n"
                + "        self.value = value\n"
                + "        [setattr(self, k, v) for k, v in kwargs.items()]\n"
                + "    def __repr__(self):\n"
                + "        return \"%s%s\" %(self.__class__.__name__, self.__dict__)\n"
                + "    __str__ = __repr__\n");
        TypedHashSet<String> s = new TypedHashSet<String>();
        StringBuilder isInstanceFcns = new StringBuilder();
        for (TprintIdentifier id : (new GetTprintIdentifiers()).run(serializedCode)) {
            if (s.add(id.id())) {
                String clsName = NodesToC.pyClassName(id.id());
                pyCode.append("class " + clsName + "(Message): pass\n");
                final String isInstName = NodesToC.pyFieldName(id.id());
                isInstanceFcns.append("is" + (isInstName.contains("_") ? "_" : "") +
                        isInstName + " = lambda a: isinstance(a, " + clsName + ")\n");
            }
        }
        pyCode.append(isInstanceFcns);
        pyCode.append("\n");

        for (Function f : GetPrintFcns.run(serializedCode)) {
            String serHcode =
                    (String) serializedCode.accept(new NodesToH(resultFile, tprintPyStyle));
            if (f.getParams().size() != 0) {
                throw new ExceptionAtNode("printfcn's cannot have parameters", f);
            }
            String testCode =
                    (String) serializedCode.accept(new NodesToCPrintTest(varGen,
                            resultFile, f.getName(), tprintPyStyle));
            String ccfilename = options.getTmpFilename(resultFile + ".cc");
            String hfilename = options.getTmpFilename(resultFile + ".h");

            try {
                PlatformLocalization pl = PlatformLocalization.getLocalization();
                String bitVecH = pl.new ResolveRuntime("bitvec.h").resolve();
                String fixedArrH = pl.new ResolveRuntime("fixedarr.h").resolve();
                FileUtils.copyFile(new File(bitVecH),
                        new File(options.getTmpFilename("bitvec.h")));
                FileUtils.copyFile(new File(fixedArrH),
                        new File(options.getTmpFilename("fixedarr.h")));
                FileWriter cc_writer = new FileWriter(ccfilename);
                FileWriter h_writer = new FileWriter(hfilename);
                cc_writer.write(testCode);
                h_writer.write(serHcode);
                cc_writer.close();
                h_writer.close();

                SynchronousTimedProcess cxx =
                        new SynchronousTimedProcess(options.sktmpdir().getAbsolutePath(),
                                0, "g++", "-g", "-O0", "-o", "testPrintBin", ccfilename);
                assert cxx.run(true).exitCode == 0 : "g++ failed; command line:\n" + cxx;

                SynchronousTimedProcess runOutput =
                        new SynchronousTimedProcess(options.sktmpdir().getAbsolutePath(),
                                0, options.getTmpFilename("testPrintBin"));
                final ProcessStatus runOutputStatus = runOutput.run(true);
                assert runOutputStatus.exitCode == 0 : "running print function failed; command line:\n" +
                        runOutput;
                final String out = runOutputStatus.out;
                final String cleanedName = f.getName().replace("__Wrapper", "");
                if (tprintPyStyle) {
                    pyCode.append(cleanedName + "_messages = [\n");
                    pyCode.append(out.substring(0, out.length() - 2));
                    pyCode.append(" ]\n\n");
                } else {
                    printNote("print output from function " + cleanedName + "\n" + out +
                            "------------------------------");
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        // final output of py code
        if (tprintPyStyle) {
            try {
                FileWriter fw = new FileWriter(options.feOpts.tprintPython);
                fw.write(pyCode.toString());
                fw.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return serializedCode;
    }

}
