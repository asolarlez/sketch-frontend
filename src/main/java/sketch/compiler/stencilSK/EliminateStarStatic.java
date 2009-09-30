package sketch.compiler.stencilSK;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import sketch.compiler.ast.core.FEContext;
import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.exprs.ExprArrayInit;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprStar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.solvers.constructs.AbstractValueOracle;
import sketch.compiler.solvers.constructs.StaticHoleTracker;

public class EliminateStarStatic extends FEReplacer {

	AbstractValueOracle oracle;
	HashMap<ExprStar, Object> hole_values =
	    new HashMap<ExprStar, Object>();

	public EliminateStarStatic(AbstractValueOracle oracle){
		assert oracle.getHoleNamer() instanceof StaticHoleTracker;
		this.oracle = oracle;
		oracle.initCurrentVals();
	}

	public Object visitExprStar(ExprStar star) {
		Type t = star.getType();
		int ssz = 1;
		if(t instanceof TypeArray){
			Integer iv = ((TypeArray)t).getLength().getIValue();
			assert iv != null;
			ssz = iv;
			List<Expression> lst = new ArrayList<Expression>(ssz);
			for(int i=0; i<ssz; ++i){
				lst.add(oracle.popValueForNode(star.getDepObject(i)));
			}
			hole_values.put(star, lst);

			ExprArrayInit ainit = new ExprArrayInit(star, lst);
			return ainit;
		}else{
		    Expression value = oracle.popValueForNode(star.getDepObject(0));
		    hole_values.put(star, value);
			return value;
		}
	}

    public int getIntValue(Expression expr) {
        if (expr instanceof ExprConstInt) {
            return ((ExprConstInt) expr).getVal();
        } else {
            return -1;
        }
    }

    @SuppressWarnings( { "deprecation", "unchecked" })
    public void dump_xml() {
        System.out.println("=== BEGIN XML OUTPUT ===\n"
                + "<?xml version=\"1.0\"?>");
        System.out.println("<hole_values>");
        for (Entry<ExprStar, Object> ent : hole_values.entrySet()) {
            FEContext ctx = ent.getKey().getContext();
            System.out.print("    <hole_value line=\"" + ctx.getLineNumber()
                    + "\" col=\"" + ctx.getColumnNumber() + "\" ");
            if (ent.getKey().getType() instanceof TypeArray) {
                List<Expression> lst = (List<Expression>) ent.getValue();
                System.out.println("type=\"array\">");
                for (Expression value : lst) {
                    System.out.println("        <entry value=\""
                            + getIntValue(value) + "\" />");
                }
                System.out.println("    </hole_value>");
            } else {
                System.out.println("type=\"int\" value=\""
                        + getIntValue((Expression) ent.getValue()) + "\" />");
            }
        }
        System.out.println("</hole_values>");
        System.out.println("------------------------------");
    }
}
