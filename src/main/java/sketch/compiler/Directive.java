/**
 *
 */
package sketch.compiler;

import sketch.util.datastructures.TypedHashMap;
import static sketch.util.Misc.nonnull;

/**
 * An odd little class for interpreting compiler directives, such as
 * <code>
 *   #pragma options "--heapsize 10 --reorderEncoding exponential"
 * </code>
 *
 * This class is responsible for recognizing and storing the directives.
 *
 * @author <a href="mailto:cgjones@cs.berkeley.edu">Chris Jones</a>
 */
public abstract class Directive {
	public Directive (String pragma, String args) {  }

	public String name () {  return null;  }

	public static Directive make (String pragma, String args) {
		if (OptionsDirective.NAME.equals (pragma))
			return new OptionsDirective (pragma, args);
		else if (InstrumentationDirective.NAME.equals(pragma))
		    return new InstrumentationDirective(pragma, args);
		else
			throw new IllegalArgumentException ("unknown pragma '"+ pragma +"'");
	}

	/**
	 * A pragma to set within a source file the SKETCH options normally
	 * passed through the command line.
	 *
	 * @author <a href="mailto:cgjones@cs.berkeley.edu">Chris Jones</a>
	 */ 
	public static class OptionsDirective extends Directive {
		private static final String NAME = "options";
		private String[] opts;

		public OptionsDirective (String pragma, String args) {
			super (pragma, args);
			opts = args.split ("\\s+");
		}

		public String[] options () { return opts; }

		public int hashCode () { return name ().hashCode () ^ opts.hashCode (); }
		public boolean equals (Object o) {
			return (o instanceof OptionsDirective)
				   && opts.equals (((OptionsDirective)o).options ());
		}

		public String name () { return NAME; }
	}

	/**
	 * instrument reads and writes of an array in cuda functions
	 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
	 * @license This file is licensed under BSD license, available at
	 *          http://creativecommons.org/licenses/BSD/. While not required, if you
	 *          make changes, please consider contributing back!
	 */
    public static class InstrumentationDirective extends Directive {
        public static final String NAME = "instrumentation";
        public static final String USAGE = "instrumentation usage: #pragma " +
        		"instrumentation \"name=X struct=Y init=initFcn read=readFcn " +
        		"write=writeFcn [end=endFcn] [syncthreads=syncthreadsFcn]\"";

        public final String name;
        public final String struct;
        public final String init;
        public final String read;
        public final String write;
        /** may be null if no end function is to be called */
        public final String end;
        /** may be null if no syncthreads function is to be called */
        public final String syncthreads;

        public InstrumentationDirective (String pragma, String args) {
            super (pragma, args);
            TypedHashMap<String, String> namedValues = new TypedHashMap<String, String>();
            for (String s : args.split("\\s+")) {
                String[] keyvalue = s.split("=", 2);
                assert keyvalue.length == 2 : USAGE;
                namedValues.put(keyvalue[0], keyvalue[1]);
            }
            this.name = nonnull(namedValues.get("name"), USAGE);
            this.struct = nonnull(namedValues.get("struct"), USAGE);
            this.init = nonnull(namedValues.get("init"), USAGE);
            this.read = nonnull(namedValues.get("read"), USAGE);
            this.write = nonnull(namedValues.get("write"), USAGE);
            this.end = namedValues.get("end");
            this.syncthreads = namedValues.get("syncthreads");
        }
    }
}
