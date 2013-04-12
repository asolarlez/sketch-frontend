package sketch.util.exceptions;

public class ProgramParseException extends SketchException {
    private static final long serialVersionUID = 8478299044940341339L;

    public ProgramParseException(String msg) {
        super(msg);
    }

    @Override
    protected String messageClass() {
        return "Program Parse Error";
    }
}
