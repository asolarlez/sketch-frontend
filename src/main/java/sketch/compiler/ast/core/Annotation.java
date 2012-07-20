package sketch.compiler.ast.core;



public class Annotation {
    private final FEContext context;
    public final String tag;
    public final String rawParams;

    public String contents() {
        String s = rawParams.substring(1, rawParams.length() - 1);
        s = s.replace("\\\"", "\"");
        s = s.replace("\\\\", "\\");
        return s;
    }

    public Annotation(FEContext context, String tag, String rawParams) {
        this.context = context;
        this.tag = tag;
        this.rawParams = rawParams;
    }

    public String toString() {
        return "@" + tag + "(" + rawParams + ")";
    }

    public static Annotation newAnnotation(FEContext context, String tag, String params) {
        return new Annotation(context, tag, params);
    }
}

