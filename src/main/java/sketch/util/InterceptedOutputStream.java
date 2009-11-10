package sketch.util;

import java.io.IOException;
import java.io.OutputStream;

/**
 * A class that allows data passing through the 'source' stream to be intercepted and copied 
 * into another 'destination' stream.
 * Closing this stream only closes the original stream but not the destination stream
 * @author Lexin Shan
 * @email lshan@eecs.berkeley.edu
 *
 */
public class InterceptedOutputStream extends OutputStream {

	protected OutputStream src;
	protected OutputStream dest;
	
	/**
	 * Closing this stream only closes the source stream
	 * but not the destination stream. This design allows using
	 * System.out as a destination stream to do logging.
	 */
	public void close() throws IOException {
		src.close();
	}

	public boolean equals(Object obj) {
		return src.equals(obj);
	}

	public void flush() throws IOException {
		src.flush();
		dest.flush();
	}

	public int hashCode() {
		return src.hashCode();
	}
	
	public InterceptedOutputStream(OutputStream src, OutputStream dest) {
		this.src = src;
		this.dest = dest;
	}
	
	@Override
	public void write(int b) throws IOException {
		src.write(b);
		dest.write(b);
	}
	
	public String getString() {
		return "InterceptedOutputStream: " + src.toString() + " + " + dest.toString();
	}

}
