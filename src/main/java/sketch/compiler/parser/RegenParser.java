/**
 *
 */
package streamit.frontend.parser;

import java.io.IOException;
import java.io.StringReader;

import streamit.frontend.nodes.Expression;
import streamit.frontend.nodes.FEContext;

import xtc.parser.ParseException;

/**
 * A wrapper around the generated parser for regular-expression expression
 * generators, regens.
 *
 * @author <a href="mailto:cgjones@cs.berkeley.edu">Chris Jones</a>
 */
public class RegenParser {
	public static Expression parse (String s) {
		return parse (s, new FEContext ("'"+ s +"'", 1, 1));
	}
	public static Expression parse (String s, FEContext cx) {
		try {
			StringReader in = new StringReader (s);
			Regen parser = new Regen (in, cx.getFileName ());
			parser.setCx (cx);
			return (Expression) parser.value (parser.pRegen (0));
		} catch (IOException ioe) {
			throw new RuntimeException ("WTF?  IO error reading a string?", ioe);
		} catch (ParseException pe) {
			throw new RuntimeException ("Error parsing regen", pe);
		}
	}

	public static void main (String[] args) {
		String[][] tests = {
// Regression checks
{ "1",
  "1" },
{ "x",
  "x" },
{ "1+2",
  "(1 (+) 2)" },
{ " 1 + 2 ",
  "(1 (+) 2)" },
{ "1+2+3",
  "((1 (+) 2) (+) 3)"},
{ "1+2*3",
  "((1 (+) 2) (*) 3)"},
{ "1*(2+3)",
  "(1 (*) ((2 (+) 3)))"},
{ "(x)",
  "(x)"},
{ "(x+y)",
  "((x (+) y))"},
{ "(x)+y",
  "((x) (+) y)"},
{ "1?2:3",
  "(1 ? 2 : 3)"},
{ "x.next",
  "x.next"},
{ "(x).next",
  "(x).next"},
//"x[1]"
//"x[1][1]"
//"x[1+2]"
//"x[1].next[2]"
{ "1|2",
  "(1 | 2)"},
{ "1&2",
  "(1 (&) 2)"},
{ "1&&2",
  "(1 (&&) 2)"},
{ "1||2",
  "(1 (||) 2)"},
{ "x (+|*) y",
  "(x (*|+) y)"},
{ "x (++|--)",
  "(x(++|--))"},
{ "(++|--|~) x",
  "((--|~|++)x)"},
{ "(++|--|~) x (++|--)",
  "((--|~|++)(x(++|--)))"},
{ "++x--++",
  "((++)((x(--))(++)))"},
{ "(++)x(--)(++)",
  "((++)((x(--))(++)))"},
{ "!? x",
  "((!)?x)"},
{ "(!)? x",
  "((!)?x)"},
{ "x.y.z.a",
  "x.y.z.a"},
{ "x++?y:z",
  "((x(++)) ? y : z)"},
{ "x++?++?y--?:z",
  "(((x(++)?)(++)) ? (y(--)?) : z)"},
{ "x++--?++?y--?:z",
  "((((x(++))(--)?)(++)) ? (y(--)?) : z)"},
{ "x++?--?++?y--?++?:--?z++?--?--?a++?--?:++?--?b++?",
  "((((x(++)?)(--)?)(++)) ? ((y(--)?)(++)?) : (((--)?(((z(++)?)(--)?)(--))) ? ((a(++)?)(--)?) : ((++)?((--)?(b(++)?)))))"
},
{ "a.b(.c)?c:d",
  "(a.b.c ? c : d)"
},
{ "a.b(.c)?c(.d)?:e(.f)?g(.h):i",
  "(a.b.c ? c(.d)? : (e.f ? g.h : i))"
},
{ "a(.b|.c|.d)?",
  "a((.b | .c) | .d)?"
},
// XXX/cgjones: checking whether these are variables or constants needs to be
// done manually, in the debugger
{ "true",
  "true"
},
{ "false",
  "false"
},
{ "null",
  "null"
},
// Examples from PLDI08
{ " tail(.next)? | (tmp|newEntry).next ",
  "(tail(.next)? | ((tmp | newEntry)).next)"},
{ "(tail|tmp|newEntry)(.next)? | null",
  "((((tail | tmp) | newEntry))(.next)? | null)"},
{ " x==y | x!=y | false ",
  "(((x (==) y) | (x (!=) y)) | false)"},
{ " p(.next)?.taken ",
  "p(.next)?.taken"},
{ " (tprev|cur|prev)(.next)? ",
  "(((tprev | cur) | prev))(.next)?"},
{ " (!)? (null|cur|prev)(.next)? =="+
 "	          (null|cur|prev)(.next)?",
  "(((!)?(((null | cur) | prev))(.next)?) (==) (((null | cur) | prev))(.next)?)"},
{ " (!)? ((null|cur|prev)(.next)? =="+
 "			  (null|cur|prev)(.next)?)",
  "((!)?(((((null | cur) | prev))(.next)? (==) (((null | cur) | prev))(.next)?)))"},
{ " (null|cur|prev)(.next)? (==|!=)"+
 "	          (null|cur|prev)(.next)?",
  "((((null | cur) | prev))(.next)? (!=|==) (((null | cur) | prev))(.next)?)"},
{ " head(.next|.prev)? ",
  "head(.next | .prev)?"},
{ " prevHead(.next)?(.next)? ",
  "prevHead(.next)?(.next)?"},
{ " prevHead(.next(.next)?)? ",
  "prevHead(.next(.next)?)?"},
{ " (tmp|prevHead)(.next)? ",
  "((tmp | prevHead))(.next)?"},
{ " (!)? (a==b | (a|b)==?? | c | d) ",
  "((!)?(((((a (==) b) | (((a | b)) (==) ??)) | c) | d)))"},

// Bug in Rats!.  Needs to be worked around manually by changing generated code.
{ "x \\| 1",
  "(x (\\|) 1)"},
	};

		for (String[] test : tests) {
			String in = test[0], exp = test[1];
			String res = parse (in).toString ();

			if (!res.equals (test[1]))
				System.err.println ("For input '"+ test[0] +"'\n"+
					"   result '"+ res +"'\n"+
					"   !match '"+ exp +"'");
		}
	}

}
