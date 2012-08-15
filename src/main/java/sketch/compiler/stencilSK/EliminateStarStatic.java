package sketch.compiler.stencilSK;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import sketch.compiler.ast.core.FEContext;
import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.exprs.ExprArrayInit;
import sketch.compiler.ast.core.exprs.ExprConstChar;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprStar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.solvers.constructs.AbstractValueOracle;
import sketch.compiler.solvers.constructs.StaticHoleTracker;
import sketch.util.DebugOut;

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
        if (star.isAngelicMax()) {
            return star;
        }
		Type t = star.getType();
		int ssz = 1;
		if(t instanceof TypeArray){
			Integer iv = ((TypeArray)t).getLength().getIValue();
			assert iv != null;
			ssz = iv;
			List<Expression> lst = new ArrayList<Expression>(ssz);
			for(int i=0; i<ssz; ++i){
                lst.add(oracle.popValueForNode(star.getDepObject(i),
                        ((TypeArray) t).getBase()));
			}
			hole_values.put(star, lst);

			ExprArrayInit ainit = new ExprArrayInit(star, lst);
            return ainit;

        }
		else{
            if (t.equals(TypePrimitive.chartype)) {
                System.out.println("Is CT");
            }
            Expression value = oracle.popValueForNode(star.getDepObject(0), t);
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

    public char getCharValue(Expression expr) {
        if (expr instanceof ExprConstChar) {
            return ((ExprConstChar) expr).getVal();
        } else {
            char tmp = 0;
            return tmp;
        }
    }

    @SuppressWarnings( { "deprecation", "unchecked" })
    public void dump_xml(String filename) {
        PrintStream out = null;
        if (filename == "--") {
            out = System.out;
            out.println("=== BEGIN XML OUTPUT ===");
        } else {
            try {
                final File file = new File(filename);
                if (!file.getParentFile().exists()) {
                    file.getParentFile().mkdirs();
                }
                out = new PrintStream(file, "UTF-8");
            } catch (Exception e) {
                DebugOut.fail_exception("opening print stream for xml out", e);
            }
        }

        out.println("<?xml version=\"1.0\"?>");
        out.println("<hole_values>");
        for (Entry<ExprStar, Object> ent : hole_values.entrySet()) {
            FEContext ctx = ent.getKey().getContext();
            out.print("    <hole_value line=\"" + ctx.getLineNumber() + "\" col=\"" +
                    ctx.getColumnNumber() + "\" ");
            if (ent.getKey().getType() instanceof TypeArray) {
                List<Expression> lst = (List<Expression>) ent.getValue();
                out.println("type=\"array\">");
                for (Expression value : lst) {
                    out.println("        <entry value=\"" + getIntValue(value) + "\" />");
                }
                out.println("    </hole_value>");
            } else {
                out.println("type=\"int\" value=\"" +
                        getIntValue((Expression) ent.getValue()) + "\" />");
            }
        }
        out.println("</hole_values>");
        if (filename == "--") {
            out.println("------------------------------");
        }
    }
}
