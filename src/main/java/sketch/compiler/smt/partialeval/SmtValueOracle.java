package sketch.compiler.smt.partialeval;

import java.io.IOException;
import java.io.LineNumberReader;
import java.util.LinkedList;
import java.util.List;

import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.exprs.ExprArrayInit;
import sketch.compiler.ast.core.exprs.ExprConstBoolean;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprStar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.smt.SmtOracle;
import sketch.compiler.smt.smtlib.SMTLIBTranslator;
import sketch.util.IPredicate;
import sketch.util.Numerical;

public abstract class SmtValueOracle extends SmtOracle {

	/**
	 * An mOutputOracle that returns 0 for any query
	 * 
	 * @author Lexin Shan
	 * @email lshan@eecs.berkeley.edu
	 * 
	 */
	public static class AllZeroOracle extends SmtValueOracle {
		
		
		public AllZeroOracle() {
			super();
		}

		@Override
		public void loadFromStream(LineNumberReader in) throws IOException {
			throw new IllegalStateException("API bug");
		}

		@Override
		public Expression popValueForNode(FENode node) {
			ExprStar star = (ExprStar) node;
			SmtType smtType = SmtType.create(star.getType(), star.getSize());
			NodeToSmtValue ntsv = NodeToSmtVtype.defaultValue(smtType);
			return nodeToExpression(node, ntsv);
		}


		@Override
		public NodeToSmtValue getValueForVariable(String arg0, SmtType smtType) {
			if (arg0.startsWith("H__"))
				return NodeToSmtVtype.defaultValue(smtType);
			return null;
		}
		
		public NodeToSmtValue getValueForVariable(NodeToSmtValue lhs) {
			return NodeToSmtVtype.defaultValue(lhs.getSmtType());
		}
		
	}

	public SmtValueOracle() {
		super();
	}

	@Override
	public Expression nodeToExpression(FENode context, NodeToSmtValue value) {
		assert value.isConst() && (value.isInt() || value.isBit() || BitVectUtil.isBitArray(value.getType()));
		long v = value.getIntVal();

		if (value.getType().equals(TypePrimitive.inttype)) {

			return new ExprConstInt(context, (int) v);

		} else if (value.getType().equals(TypePrimitive.booltype)) {
			return new ExprConstBoolean(context, (v == 0 ? false : true));

		} else if (value.getType().equals(TypePrimitive.bittype)) {
			return new ExprConstInt(context, (int) v);
		} else if (value.isBitArray()) {
			int vectSize = value.getNumBits();
			List<Expression> initVals = new LinkedList<Expression>();
			if (vectSize <= 32) {
				for (int i = 0; i < vectSize; i++)
					initVals.add(new ExprConstInt(Numerical.getBit(value.getIntVal(), i)));
			}

			return new ExprArrayInit(context, initVals);
		} else {
			throw new IllegalStateException("unexpected type: "
					+ value.getType() + " in SmtValueOracle.nodeToExpression()");
		}
	}

	@Override
	public String toString() {
		return toString(IPredicate.PassThrough);
	}

	public String toString(IPredicate<String> predicate) {
		StringBuffer sb = new StringBuffer();
		sb.append("Array:\t");
		sb.append(arrayValueMap.toString());
		sb.append('\n');
		sb.append("Vars:\t");

		for (String varName : super.valMap.keySet()) {
			if (predicate.accept(varName)) {
				NodeToSmtValue ntsv = super.valMap.get(varName);

				sb.append("(= ");
				sb.append(varName);
				sb.append(' ');

				if (ntsv.isBottom()) {
					sb.append(ntsv.toString());

				} else if (ntsv.isConst()){
					if (ntsv.isBit())
						sb.append(SMTLIBTranslator.intToSMTLIB(
								ntsv.getIntVal(), 1));
					else if (ntsv.isInt())
						sb.append(SMTLIBTranslator.intToSMTLIB(
								ntsv.getIntVal(), ntsv.getNumBits()));
					else if (ntsv.isBitArray())
						sb.append(SMTLIBTranslator.intToSMTLIB(
								ntsv.getIntVal(), ntsv.getNumBits()));
					else if (ntsv.isVect())
						sb.append(ntsv.obj.toString());
				} else if (ntsv.isLabel()) {
					sb.append(ntsv.toString());
				}
				sb.append(")\n");
			}
		}

		return sb.toString();
	}

}
