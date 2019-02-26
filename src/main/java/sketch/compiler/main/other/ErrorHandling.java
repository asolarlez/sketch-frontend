package sketch.compiler.main.other;

import java.io.File;

import static sketch.util.DebugOut.printDebug;

import sketch.compiler.ast.core.Program;
import sketch.compiler.main.PlatformLocalization;
import sketch.util.exceptions.LastGoodProgram;

/**
 * A few miscellany error handling functions for main methods
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
public class ErrorHandling {
    public static void checkJavaVersion(int... gt_tuple) {
    	return;
    	/*
    	 * 
    	 * After Java 10, java versioning became hard to parse.
    	 * Plus, nobody has java < 6 anymore.
        String java_version = System.getProperty("java.version");
		String[] version_numbers = java_version.split("\\.");
		if (version_numbers.length < gt_tuple.length) {
			// This happens with java version 10.
			return;
		}
        for (int a = 0; a < gt_tuple.length; a++) {
            int real_version = Integer.parseInt(version_numbers[a]);
            if (real_version < gt_tuple[a]) {
                String required = "";
                for (int c = 0; c < gt_tuple.length; c++) {
                    required +=
                            String.valueOf(gt_tuple[c]) +
                                    ((c != gt_tuple.length - 1) ? "." : "");
                }
                System.err.println("your java version is out of date. Version " +
                        required + " required");
                System.exit(1);
            }
        }
        */
    }

    public static void dumpProgramToFile(LastGoodProgram lastGoodProg) {
        String name = lastGoodProg.stageName;
        Program prog = lastGoodProg.prog;
        if (prog == null) {
            printDebug("[SKETCH] program null.");
        } else {
            try {
                final PlatformLocalization loc = PlatformLocalization.getLocalization();
                File out_file = loc.getTempPath("error-last-program.txt");
                prog.debugDump(out_file);
                printDebug("[SKETCH] Last good program, from before stage", name +
                        ", dumped to: ", out_file);
            } catch (Throwable e2) {}
        }
    }

    public static void handleErr(Throwable e) {
        System.err.println("[ERROR] [SKETCH] Failed with " +
                e.getClass().getSimpleName() + " exception; message: " + e.getMessage());
    }
}
