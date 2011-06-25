package sketch.util.exceptions;

public class InternalSketchException extends SketchException {
    private static final long serialVersionUID = 4308864661089745866L;

    public InternalSketchException(String msg) {
        super(msg);
    }

    @Override
    protected String messageClass() {
        return "Internal sketch exception";
    }
}
