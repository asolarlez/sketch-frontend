package sketch.util.exceptions;


public class SketchNotResolvedException extends SketchSolverException {
    private static final long serialVersionUID = 5675449599010105839L;

    public SketchNotResolvedException(String backendTempPath, String message) {
        super(message + "The sketch could not be resolved.");
        this.setBackendTempPath(backendTempPath);
    }

    @Override
    protected String messageClass() {
        return "Sketch Not Resolved Error";
    }

}
