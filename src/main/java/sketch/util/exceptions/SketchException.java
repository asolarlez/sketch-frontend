package sketch.util.exceptions;

import sketch.compiler.main.PlatformLocalization;

import static sketch.util.DebugOut.printError;

public abstract class SketchException extends RuntimeException {
    private static final long serialVersionUID = 2823359528414432108L;

    public SketchException(String msg) {
        super(msg);
    }

    public void print() {
        printError("[ERROR] [SKETCH] " + this.messageClass() + ": " + this.getMessage());
        dumpStackTraceToFile();
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
}
