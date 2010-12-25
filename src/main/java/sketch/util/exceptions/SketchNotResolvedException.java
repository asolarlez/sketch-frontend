package sketch.util.exceptions;

public class SketchNotResolvedException extends SketchException {
    private static final long serialVersionUID = 5675449599010105839L;

    public SketchNotResolvedException() {
        super("The sketch could not be resolved.");
    }

    @Override
    protected String messageClass() {
        return "Sketch not resolved";
    }

    @Override
    public void print() {
        System.err.println("[ERROR] [SKETCH] The sketch could not be resolved.");
        dumpStackTraceToFile();
    }
}
