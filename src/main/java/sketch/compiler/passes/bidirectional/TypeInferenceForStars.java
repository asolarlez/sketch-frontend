package sketch.compiler.passes.bidirectional;

import sketch.compiler.ast.core.exprs.ExprStar;
import sketch.compiler.ast.core.typs.Type;

import static sketch.util.DebugOut.printNote;
/**
 * This visitor distinguishes between int stars and bit stars, and labels each star with its
 * appropriate type.
 *
 *
 * @author asolar
 *
 */
public class TypeInferenceForStars extends BidirectionalPass {

    @Override
	public Object visitExprStar(ExprStar star) {
		Type type = driver.tdstate.getExpected();
		if (!star.typeWasSetByScala) {
			// NOTE -- don't kill better types by Scala compiler / Skalch grgen
			// output
			star.setType(type);
		} else {
			printNote("skipping setting star type", star, type);
        }
		return star;
    }

}

