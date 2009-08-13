/**
 *
 */
package sketch.util;

import java.io.IOException;
import java.io.OutputStream;

/**
 * An output stream that is functionally equivalent to /dev/null.
 *
 * @author <a href="mailto:cgjones@cs.berkeley.edu">Chris Jones</a>
 */
public class NullStream extends OutputStream {
	public static final NullStream INSTANCE = new NullStream ();

	public NullStream () { }

	public void close () throws IOException { }
	public void flush () throws IOException { }
	public void write (byte[] b) throws IOException { }
	public void write (byte[] b, int off, int len) throws IOException { }
	public void write (int b) throws IOException { }
}
