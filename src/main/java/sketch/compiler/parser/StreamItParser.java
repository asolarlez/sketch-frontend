/**
 *
 */
package streamit.frontend.parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import antlr.RecognitionException;
import antlr.TokenStreamException;

import streamit.frontend.Directive;
import streamit.frontend.nodes.Program;
import streamit.misc.CPreprocessedFileStream;

/**
 * A light wrapper around the main parser.
 *
 * Allows us to do things like preprocess input files before parsing them,
 * uniformly handle IO errors, and so forth.
 *
 * @author <a href="mailto:cgjones@cs.berkeley.edu">Chris Jones</a>
 */
public class StreamItParser {
	StreamItParserFE parser;

	/** Create a parser of FILENAME. */
	public StreamItParser (String fileName) {
		this (fileName, false);
	}

	/** Create a parser of FILENAME, and optionally run the input file through
	 * the C preprocessor before it is parsed. */
	public StreamItParser (String fileName, boolean preprocess) {
		this (fileName, new HashSet<String> (), preprocess);
	}

	/** Create a parser of FILENAME, and optionally run the input file through
	 * the C preprocessor before it is parsed. HANDLEDINCLUDES will not be
	 * included again. */
	public StreamItParser (String fileName, Set<String> handledIncludes,
						   boolean preprocess) {
		try {
			InputStream in = preprocess ? new CPreprocessedFileStream (fileName)
										: new FileInputStream (fileName);
			handledIncludes.add ((new File (fileName)).getCanonicalPath ());
			parser = new StreamItParserFE (new StreamItLex (in), handledIncludes, preprocess);
			parser.setFilename (fileName);
		} catch (FileNotFoundException fnf) {
			throw new IllegalArgumentException("File not found: "+fileName);
		} catch (IOException ioe) {
			throw new IllegalArgumentException("Something wrong with: "+fileName);
		}
	}

	public Program parse () {
		try {
			return parser.program ();
		} catch (RecognitionException e) {
			throw new IllegalStateException (e);
		} catch (TokenStreamException e) {
			throw new IllegalStateException(e);
		}
	}

	public Set<Directive> getDirectives () {
		return parser.getDirectives ();
	}
}
