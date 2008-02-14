package streamit.misc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;

/**
 * An line-by-line iterator for reading streams.
 *
 * @author Chris Jones
 */
public class LineReader implements Iterable<String>, Iterator<String> {
	private BufferedReader in;
	private String nextLine;

	public LineReader (InputStream _in) {
		in = new BufferedReader (new InputStreamReader (_in));
	}

	public Iterator<String> iterator () {
		return this;
	}

	public boolean hasNext () {
		try {  nextLine = in.readLine ();  }
		catch (IOException ioe) {  throw new RuntimeException (ioe);  }
		return null != nextLine;
	}

	public String next () {
		return nextLine;
	}

	public void remove () {  throw new UnsupportedOperationException ();  }
}
