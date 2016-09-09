package sketch.compiler.solvers.constructs;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import antlr.RecognitionException;
import antlr.TokenStreamException;
import sketch.compiler.ast.core.FEContext;
import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.exprs.ExprConstChar;
import sketch.compiler.ast.core.exprs.ExprConstFloat;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprConstant;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.parser.StreamItLex;
import sketch.compiler.parser.StreamItParserFE;

public class ValueOracle extends AbstractValueOracle {
	/**
	 * After sketch is resolved, this map will contain the
	 * value of each variable in store.
	 */
    protected Map<String, Expression> valMap;



	protected int starSizesCaped = -1;

	public ValueOracle(HoleNameTracker holeNamer) {
		super();
		valMap = null;
		this.holeNamer = holeNamer;
	}

	public void loadFromStream(LineNumberReader in) throws IOException{
		String dbRecord = null;
        valMap = new HashMap<String, Expression>();
		while ( (dbRecord = in.readLine()) != null) {
            StringTokenizer st = new StringTokenizer(dbRecord, "\t ");
            String vname = st.nextToken();
            String sval = st.nextToken();
            if (sval.equals("(")) {
                StreamItParserFE parse = new StreamItParserFE(new StreamItLex(new StringReader(dbRecord.replace(vname, ""))), null, false, null);
                parse.setFilename("Generated");
                Expression exp = null;
                try {
                    exp = parse.prefix_expr();
                } catch (RecognitionException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (TokenStreamException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                valMap.put(vname, exp);
            } else {
                if (sval.contains(".")) {
                    double val = Double.parseDouble(sval);
                    valMap.put(vname, new ExprConstFloat((FEContext) null, val));
                } else {
                    int val = Integer.parseInt(sval);
                    valMap.put(vname, new ExprConstInt(val));
                }
            }
         }
	}

    protected ExprConstant getVal(FENode node, String var, Type t) {
        Expression val = valMap.get(var);

		if(val != null){
            if (t.equals(TypePrimitive.chartype)) {
                return ExprConstChar.createFromInt(val.getIValue());
            } else {
                return (ExprConstant) val;
		    }
		}		
		return new ExprConstInt(node, -1);
	}
	
    public ExprConstant popValueForNode(FENode node, Type t) {
		String name = holeNamer.getName(node);
        return getVal(node, name, t);
	}

    public Expression generatorHole(String name) {
        return valMap.get(name);
    }

	public void capStarSizes(int size) {
		starSizesCaped = size;
	}
}