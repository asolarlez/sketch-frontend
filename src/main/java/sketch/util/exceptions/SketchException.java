package sketch.util.exceptions;

import sketch.compiler.main.PlatformLocalization;
import sketch.compiler.main.other.ErrorHandling;

import static sketch.util.DebugOut.printError;

/**
 * The modern class for throwing errors in SKETCH. Use ExceptionAtNode (or subclasses) if
 * possible.
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
public abstract class SketchException extends RuntimeException {
    private static final long serialVersionUID = 2823359528414432108L;
    protected LastGoodProgram lastGoodProg;

    public SketchException(String msg) {
        super(msg);
    }

    public void print() {
        printError("[SKETCH] " + this.messageClass() + ": " + this.getMessage());
        dumpStackTraceToFile();
        if (lastGoodProg != null) {
            ErrorHandling.dumpProgramToFile(lastGoodProg);
        }
    }

    public void dumpStackTraceToFile() {
        final StackTraceElement[] stackTrace = this.getStackTrace();
        String stackTraceSting = "";
        for (StackTraceElement elt : stackTrace) {
            stackTraceSting += elt.toString() + "\n";
        }
        PlatformLocalization.getLocalization().writeDebugMsg("stack trace",
                stackTraceSting, "stacktrace.txt");
    }

    protected abstract String messageClass();

    public void setLastGoodProgram(LastGoodProgram lastGood) {
        if (this.lastGoodProg == null) {
            this.lastGoodProg = lastGood;
        }
    }
}
