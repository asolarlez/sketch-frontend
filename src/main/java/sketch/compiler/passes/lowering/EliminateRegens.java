/**
 *
 */
package streamit.frontend.passes;

import streamit.frontend.nodes.TempVarGen;

/**
 * @author <a href="mailto:cgjones@cs.berkeley.edu">Chris Jones</a>
 *
 */
public class EliminateRegens extends SymbolTableVisitor {
	TempVarGen varGen;

	public EliminateRegens (TempVarGen varGen)  {
		super (null);
		this.varGen = varGen;

		System.out.println ("PASSED TYPE CHECKING WITHOUT ERRORS!");
		System.exit (0);
	}
}
