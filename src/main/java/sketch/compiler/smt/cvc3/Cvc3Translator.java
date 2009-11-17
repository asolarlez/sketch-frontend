package sketch.compiler.smt.cvc3;

import java.util.List;

import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.dataflow.abstractValue;
import sketch.compiler.smt.SMTTranslator;
import sketch.compiler.smt.partialeval.LinearNode;
import sketch.compiler.smt.partialeval.NodeToSmtValue;
import sketch.compiler.smt.partialeval.SmtType;

public class Cvc3Translator extends SMTTranslator {
	
	public Cvc3Translator() {
		super();
	}

	@Override
	public String getAssert(String predicate) {
		return "ASSERT " + predicate + ";";
	}

	@Override
	public String getDefineVar(String type, String varName) {
		return varName + " : " + type + ";";
	}

	@Override
	public String prolog() {
		return "";
	}

	@Override
	public String epilog() {
		return "CHECKSAT;\n" +
			"COUNTERMODEL;";
	}

	@Override
	protected void initOpCode() {

		opStrMap.put(OpCode.PLUS, "+");
		opStrMap.put(OpCode.MINUS, "-");
		opStrMap.put(OpCode.OVER, "/");
		opStrMap.put(OpCode.TIMES, "*");

		opStrMap.put(OpCode.EQUALS, "=");
		opStrMap.put(OpCode.NOT_EQUALS, "/=");
		opStrMap.put(OpCode.LT, "<");
		opStrMap.put(OpCode.LEQ, "<=");
		opStrMap.put(OpCode.GT, ">");
		opStrMap.put(OpCode.GEQ, ">=");
		opStrMap.put(OpCode.NEG, "-");

		opStrMap.put(OpCode.AND, "AND");
		opStrMap.put(OpCode.OR, "OR");
		opStrMap.put(OpCode.XOR, "XOR");
		opStrMap.put(OpCode.NOT, "NOT");

	}

	@Override
	public String getFalseLiteral() {
		return "0bin0";
	}

	@Override
	public String getTrueLiteral() {
		return "0bin1";
	}

	public String getTypeForSolver(Type t) {
		
		if (t instanceof TypePrimitive) {
			if (t == TypePrimitive.bittype) {
				return "BITVECTOR(1)";
			} else if (t == TypePrimitive.booltype) {
				return "BITVECTOR(1)";
			} else if (t == TypePrimitive.nulltype || t == TypePrimitive.inttype){
				return "INT";	
			}
			
		} else if (t instanceof TypeArray) {
			TypeArray arrType = (TypeArray) t;
			
			// if the type is BIT[W], just use bit vector
			if (arrType.getBase() == TypePrimitive.bittype)
				return "BITVECTOR(" + arrType.getDimension(0) + ")";
			
			String idxTypeForSolver = getTypeForSolver(TypePrimitive.inttype);
			String baseTypeForSolver = getTypeForSolver(arrType.getBase());
			return "ARRAY " + idxTypeForSolver + " OF " + baseTypeForSolver;
			
		}
		assert false : "NOT implemented";
		return null;
	}

	@Override
	public String getUnaryExpr(OpCode op, String cond) {
		return "(NOT " + cond + ")";
	}


	@Override
	public String getComment(String msg) {
		return "% " + msg;
	}

	@Override
	public String getFuncCall(String funName, List<abstractValue> avlist,
			List<abstractValue> outSlist) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getIntLiteral(int i, int numBits) {
		// even though this class is not for BV, but the case of integer translation
		// we still need to deal with bitvector of size 1, because CVC3 uses that
		// as boolean
		if (numBits == 1)
			return "0bin" + i;
		return String.valueOf(i);
	}

	@Override
	public String getNaryExpr(sketch.compiler.smt.SMTTranslator.OpCode op,
			NodeToSmtValue... opnds) {
		throw new IllegalStateException("getNaryExpr is not implemented");
	}

	@Override
	public String getBitArrayLiteral(int i, int numBits) {
		throw new IllegalStateException("getBitArrayLiteral is not implemented");
	}


	@Override
	public String getBitArrayLiteral(int numBits, int... arr) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getTypeForSolver(SmtType t) {
		// TODO Auto-generated method stub
		return null;
	}

    @Override
    public String getStrForLinearNode(LinearNode linNode) {
        // TODO Auto-generated method stub
        return null;
    }


}
