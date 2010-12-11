/*
 * Copyright 2003 by the Massachusetts Institute of Technology.
 *
 * Permission to use, copy, modify, and distribute this
 * software and its documentation for any purpose and without
 * fee is hereby granted, provided that the above copyright
 * notice appear in all copies and that both that copyright
 * notice and this permission notice appear in supporting
 * documentation, and that the name of M.I.T. not be used in
 * advertising or publicity pertaining to distribution of the
 * software without specific, written prior permission.
 * M.I.T. makes no representations about the suitability of
 * this software for any purpose.  It is provided "as is"
 * without express or implied warranty.
 */

package sketch.util;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Misc extends AssertedClass
{
    public static int MAX (int a, int b)
    {
        return (a > b ? a : b);
    }

    public static int MIN (int a, int b)
    {
        return (a < b ? a : b);
    }

    /** The number of bits needed to represent N in binary. */
    public static int nBitsBinaryRepr (int N) {
    	assert N >= 1;
    	int nBits = 32 - Integer.numberOfLeadingZeros(N - 1); // ceiling (lg (size))
		nBits = nBits > 0 ? nBits : 1;	// lg(1) is a special case
		return nBits;
    }

    
    public static String readStreamUntil (InputStream in, String keyword, boolean logAllOutput) throws IOException {		
		OutputStream out;
		if(logAllOutput){
			out = new ByteArrayOutputStream ();
		}else{
			out = new TruncatedOutputStream ();
		}
		Misc.readStreamUntil(in, keyword, out);
		return out.toString ();
	}

    public static void readStreamUntil(InputStream in, String keyword, OutputStream _out)
            throws IOException
    {
        PrintStream out = new PrintStream(_out);
        final BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        String line;
        while ((line = reader.readLine()) != null) {
            out.println(line);
            if (line.contains(keyword)) {
                return;
            }
        }
    }

	/** Read all of IN into a string and return the string. */
	public static String readStream (InputStream in, boolean logAllOutput, PrintStream altOut) throws IOException {
		
		OutputStream out;
		if(logAllOutput){
			out = new ByteArrayOutputStream ();
		}else{
			out = new TruncatedOutputStream ();
		}
		Misc.dumpStreamTo (in, out, false, altOut);
		return out.toString ();
	}

    /**
     * Dump the stream IN to the stream _OUT, optionally with line numbers.
     * 
     * @throws IOException
     */
    public static void dumpStreamTo(InputStream in, OutputStream _out,
            boolean withLineNumbers, PrintStream altOut) throws IOException
    {
        int lineno = 1;
        PrintStream out = new PrintStream(_out);
        final BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        String line;
        while ((line = reader.readLine()) != null) {
            if (withLineNumbers) {
                line = String.format("[%3d] %s", lineno, line);
                lineno += 1;
            }
            out.println(line);
            if (altOut != null) {
                altOut.println(line);
            }
        }
    }

	/** Returns null if the pattern wasn't found, otherwise returns a list
	 * of the matched groups. */
	public static List<String> search (String S, String regex) {
		Matcher m = Pattern.compile (regex, Pattern.MULTILINE).matcher (S);
		List<String> groups = null;

		if (m.find ()) {
			groups = new ArrayList<String> ();
			for (int i = 1; i <= m.groupCount (); ++i)
				groups.add (m.group (i));
		}

		return groups;
	}
	
	public static <T> T nonnull(T obj) {
	    assert obj != null : "nonnull assert";
	    return obj;
	}
	
    public static <T> T nonnull(T obj, String message) {
        if (obj == null) {
            DebugOut.printFailure(message);
        }
        return obj;
    }
}
