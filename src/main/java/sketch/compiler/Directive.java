/**
 *
 */
package streamit.frontend;

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
			opts = args.split (" ");
		}

		public String[] options () { return opts; }

		public int hashCode () { return name ().hashCode () ^ opts.hashCode (); }
		public boolean equals (Object o) {
			return (o instanceof OptionsDirective)
				   && opts.equals (((OptionsDirective)o).options ());
		}

		public String name () { return NAME; }
	}
}
