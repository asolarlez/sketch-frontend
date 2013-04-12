package sketch.util.exceptions;

public class UnsupportedSketchException extends SketchException {
    private static final long serialVersionUID = 4308864661089745866L;

    public UnsupportedSketchException(String msg) {
        super(msg);
    }

    @Override
    protected String messageClass() {
        return "Unsupported Sketch Error";
    }
}
