/**
 *
 */
package streamit.frontend.parser;

import java.io.IOException;
import java.io.StringReader;

import xtc.parser.ParseException;
import xtc.parser.Result;

/**
 * A wrapper around the generated parser for regular-expression expression
 * generators, regens.
 *
 * @author <a href="mailto:cgjones@cs.berkeley.edu">Chris Jones</a>
 */
public class RegenParser {
	public static String parse (String s) {
		try {
			StringReader in = new StringReader (s);
			Regen parser = new Regen (in, "\""+s+"\"");
			Result result = parser.pRegen (0);
			return parser.value (result).toString ();
		} catch (IOException ioe) {
			throw new RuntimeException ("WTF?  IO error reading a string?", ioe);
		} catch (ParseException pe) {
			throw new RuntimeException ("", pe);
		}
	}

	public static void main (String[] args) {
		String[] tests = {
// Regression checks
"1",
"x",
"1+2",
" 1 + 2 ",
"1+2+3",
"1+2*3",
"(x)",
"(x+y)",
"(x)+y",
"1?2:3",
"x.next",
"(x).next",
"x[1]",
"x[1][1]",
"x[1+2]",
"x[1].next[2]",
"1|2",
"1&2",
"1&&2",
"1||2",
"x (+|*) y",
"x (++|--)",
"(++|--|~) x",
"x (++|--)",
"(++|--|~) x (++|--)",
"++x--++",
"(++)x(--)(++)",
"!? x",
"(!)? x",
"x.y.z.a",
"x++?y:z",
"x++?++?y--?:z",
// Examples from PLDI08
" tail(.next)? | (tmp|newEntry).next ",
"(tail|tmp|newEntry)(.next)? | null",
" x==y | x!=y | false ",
" p(.next)?.taken ",
" (tprev|cur|prev)(.next)? ",
" (!)? (null|cur|prev)(.next)? =="+
"	          (null|cur|prev)(.next)?",
" (!)? ((null|cur|prev)(.next)? =="+
"			  (null|cur|prev)(.next)?)",
" (null|cur|prev)(.next)? (==|!=)"+
"	          (null|cur|prev)(.next)?",
" head(.next|.prev)? ",
" prevHead(.next)?(.next)? ",
" prevHead(.next(.next)?)? ",
" (tmp|prevHead)(.next)? ",
" (!)? (a==b | (a|b)==?? | c | d) ",

// Bug in Rats!.  Rats!
"x \\| 1",
	};

		for (String test : tests)  test (test);
	}

	public static void test (String in) {
		System.out.println (parse (in));
	}
}
