package sketch.compiler.solvers.constructs;

import java.io.IOException;
import java.io.LineNumberReader;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.exprs.ExprConstChar;
import sketch.compiler.ast.core.exprs.ExprConstFloat;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprConstant;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypePrimitive;

public class ValueOracle extends AbstractValueOracle {
	/**
	 * After sketch is resolved, this map will contain the
	 * value of each variable in store.
	 */
    protected Map<String, Number> valMap;



	protected int starSizesCaped = -1;

	public ValueOracle(HoleNameTracker holeNamer) {
		super();
		valMap = null;
		this.holeNamer = holeNamer;
	}

	public void loadFromStream(LineNumberReader in) throws IOException{
		String dbRecord = null;
        valMap = new HashMap<String, Number>();
		while ( (dbRecord = in.readLine()) != null) {
            StringTokenizer st = new StringTokenizer(dbRecord, "\t ");
            String vname = st.nextToken();
            String sval = st.nextToken();
            if (sval.contains(".")) {
                double val = Double.parseDouble(sval);
                valMap.put(vname, val);
            } else {
                int val = Integer.parseInt(sval);
                valMap.put(vname, val);
            }
         }
	}

    protected ExprConstant getVal(FENode node, String var, Type t) {
        Number val = valMap.get(var);

		if(val != null){
            if (t.equals(TypePrimitive.chartype)) {
                return ExprConstChar.createFromInt(val.intValue());
            } else {
                if (t.equals(TypePrimitive.inttype) || t.equals(TypePrimitive.bittype)) {
                    int i = val.intValue();
                    return (new ExprConstInt(node, i));
                } else {
                    double dd = val.doubleValue();
                    return new ExprConstFloat(node, dd);
                }
		    }
		}		
		return new ExprConstInt(node, -1);
	}
	
    public ExprConstant popValueForNode(FENode node, Type t) {
		String name = holeNamer.getName(node);
        return getVal(node, name, t);
	}

	public void capStarSizes(int size) {
		starSizesCaped = size;
	}
}