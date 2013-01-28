package sketch.compiler.ast.core;


/**
 * Customizable annotation attached to a <code>TypeStruct</code> or <code>Function</code>
 * node. Interested <code>FEVisitor</code>s can process interesting
 * <code>Annotation</code> while other <code>FEVisitor</code>s just ignore it.
 * 
 * @version $Id$
 */
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

