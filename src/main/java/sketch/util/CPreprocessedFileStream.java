/**
 *
 */
package sketch.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * A file input stream that is run through the C preprocessor.
 *
 * @author <a href="mailto:cgjones@cs.berkeley.edu">Chris Jones</a>
 */
public class CPreprocessedFileStream extends InputStream {
	public static final String CPP = "cpp";

	private static final int BOL = 1;
	private static final int READ = 2;
	private static final int IGNORE = 3;

	private Process p;
	private InputStream in;
	private int state = BOL;

	public CPreprocessedFileStream (String filename) throws FileNotFoundException {
		this (filename, null);
	}

	public CPreprocessedFileStream (String filename, List<String> cppDefs)
	throws FileNotFoundException {
		if (!(new File (filename)).canRead ())
			throw new FileNotFoundException ("can't read: "+ filename);

		ProcessBuilder pb;
		try {

			pb = new ProcessBuilder (CPP, "-Wno-trigraphs");
			if (cppDefs != null)
				for (String def : cppDefs)
					pb.command ().add ("-D"+ def);
			pb.command ().add (filename);
			p = pb.start ();
		} catch (IOException ioe) {
			throw new UnsupportedOperationException ("can't run 'cpp'", ioe);
		}

		in = p.getInputStream ();
	}

	@Override
	public void finalize () throws IOException {
		close ();
	}

	@Override
	public void close () throws IOException {  in.close ();  }

	@Override
	public int read () throws IOException {
		return in.read();
		/*
		loop:
		while (true) {
			int c = in.read ();

			switch (state) {
			case READ:
				switch (c) {
				case '\r': case '\n':
					state = BOL;	// fallthrough
				default:
					return c;
				}

			case BOL:
				switch (c) {
				case '\r': case '\n':
					return c;
				case '#':
					state = IGNORE;  continue loop;
				default:
					state = READ;  return c;
				}

			case IGNORE:
				switch (c) {
				case '\r': case '\n':
					state = BOL;	// fallthrough
				default:
					continue loop;
				}

			default:
				throw new IllegalStateException ();
			}
			
		}*/
	}
}
