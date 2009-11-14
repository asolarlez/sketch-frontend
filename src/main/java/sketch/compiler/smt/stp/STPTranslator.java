package sketch.compiler.smt.stp;
import java.util.List;

import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.ast.core.typs.TypeStruct;
import sketch.compiler.ast.core.typs.TypeStructRef;
import sketch.compiler.dataflow.abstractValue;
import sketch.compiler.smt.SMTTranslator;
import sketch.compiler.smt.partialeval.BitVectUtil;
import sketch.compiler.smt.partialeval.LabelNode;
import sketch.compiler.smt.partialeval.LinearNode;
import sketch.compiler.smt.partialeval.NodeToSmtValue;
import sketch.compiler.smt.partialeval.OpNode;
import sketch.compiler.smt.partialeval.SmtType;
import sketch.compiler.smt.partialeval.VarNode;


public class STPTranslator extends SMTTranslator {

	protected StringBuffer mSB;
	protected int mIntNumBits;
	
	public STPTranslator(int intNumBits) {
		super();
		mIntNumBits = intNumBits;
	}
	
	@Override
	public String epilog() {
		return "QUERY(FALSE);";
	}
	
	@Override
	public String getAssert(String predicate) {
		return "ASSERT " + predicate + ";";
	}

	
	@Override
	public String getBitArrayLiteral(int i, int numBits) {
		return getIntLiteral(i, numBits);
	}

	@Override
	public String getComment(String msg) {
		return "% " + msg;
	}

	@Override
	public String getDefineVar(String type, String varName) {
		StringBuffer sb = new StringBuffer();
		sb.append(varName);
		sb.append(" : ");
		sb.append(type);
		sb.append(';');
		return sb.toString();
	}

	@Override
	public String getFalseLiteral() {
		return "FALSE";
	}

	@Override
	public String getFuncCall(String funName, List<abstractValue> avlist,
			List<abstractValue> outSlist) {
		throw new IllegalStateException("API bug STPTranslator.getFuncCall()");
	}

	@Override
	public String getIntLiteral(int i, int numBits) {
		StringBuffer sb = new StringBuffer();
		sb.append("0bin");
		
		String binStr = Integer.toBinaryString(i);
		if (binStr.length() < numBits) {
			for (int j = 0; j < numBits - binStr.length(); j++)
				sb.append('0');
		} else if (binStr.length() > numBits) {
			binStr = binStr.substring(binStr.length() - numBits);
		}
		sb.append(binStr);
		
		return sb.toString();
	}
	
	public String getStr(NodeToSmtValue ntsv) {
		mSB = new StringBuffer(1024);
		getStrInternal(ntsv);
		return mSB.toString();
	}
	
	protected String getStrInternal(NodeToSmtValue ntsv) {
		if (ntsv.isConst()) {
			if (ntsv.isInt()) {
			    String t = getStrAsInt(ntsv);
				sbAppend(t);
			}
			else if (ntsv.isBool())
			    sbAppend(getStrAsBool(ntsv));
			else if (ntsv.isBit())
			    sbAppend(getStrAsBit(ntsv));
			else if (ntsv.isBitArray())
			    sbAppend(getStrAsBitArray(ntsv));
			else
				throw new IllegalStateException("there is an unknown constant type");
		
			return mSB.toString();
		} else {
			if (ntsv instanceof VarNode) {
				VarNode varNode = (VarNode) ntsv;
				sbAppend(varNode.getRHSName());
			} else if (ntsv instanceof OpNode) {
				OpNode node = (OpNode) ntsv;
				getNaryExpr(node.getOpcode(), node.getOperands());
			} else if (ntsv instanceof LabelNode) {
			    sbAppend(ntsv.toString());
			} else if (ntsv instanceof LinearNode) {
			    LinearNode linNode = (LinearNode) ntsv;
			    getStrForLinearNode(linNode);
			}
			return mSB.toString();
		}
	}

    private StringBuffer sbAppend(String t) {
//        System.out.print(t);
        return mSB.append(t);
    }
    
    private StringBuffer sbAppend(char t) {
        return mSB.append(t);
    }

    private void getStrForLinearNode(LinearNode linNode) {
        // +(o1, +(o2, c))
        for (VarNode vv : linNode.getVars()) {
            // +
            sbAppend(opStrMap.get(OpCode.PLUS));
            sbAppend('(');
            sbAppend(linNode.getNumBits()+"");
            sbAppend(',');
            
            if (linNode.getCoeff(vv) == 1) {
                getStrInternal(vv);
                sbAppend(',');
            } else {
                // *
                sbAppend(opStrMap.get(OpCode.TIMES));
                sbAppend('(');
                sbAppend(linNode.getNumBits()+"");
                sbAppend(',');
                getStrInternal(vv);
                sbAppend(",");
                sbAppend(getIntLiteral(linNode.getCoeff(vv), linNode.getNumBits()));
                sbAppend(")");
                sbAppend(',');
            }
        }
        sbAppend(getIntLiteral(linNode.getCoeff(null), linNode.getNumBits()));
        for (int j = 0; j < linNode.getNumTerms(); j++)
            sbAppend(')');
    }

	
	public String getNaryExpr(sketch.compiler.smt.SMTTranslator.OpCode op,
			NodeToSmtValue... opnds) {
		
		String operator = opStrMap.get(op);
		
		if (op == OpCode.AND ||
				op == OpCode.OR ||
				op == OpCode.XOR ||
				op == OpCode.NUM_AND ||
				op == OpCode.NUM_OR)
				{
		
			insertBetween(operator, opnds);
			return null;
			
		} else if (op == OpCode.NOT ||
				op == OpCode.NUM_NOT ||
				op == OpCode.NEG) {
			
			sbAppend(operator);
			sbAppend('(');
			getStrInternal(opnds[0]);
			sbAppend(')');
			return null;
				
		} else if (op == OpCode.EQUALS ||
				op == OpCode.CONCAT) {
			
			if (op == OpCode.EQUALS && opnds[0].isBool() && opnds[1].isBool()) {
				getStrInternal(opnds[0]);
				sbAppend(" <=> ");
				getStrInternal(opnds[1]);
				return null;
			} else { 
				sbAppend('(');
				insertBetween(operator, opnds);
				sbAppend(')');
				return null;
			}
			
		} else if (op == OpCode.RSHIFT) {
			assert opnds[1].isConst() : "second operand of shift has to be constant";
			sbAppend('(');
			getStrInternal(opnds[0]);
			sbAppend(" >> ");
			sbAppend(opnds[1].getIntVal()+"");
			sbAppend(')');
			return null;
		
		} else if (op == OpCode.LSHIFT) {
			assert opnds[1].isConst() : "second operand of shift has to be constant";
			sbAppend('(');
			getStrInternal(opnds[0]);
			sbAppend(" << ");
			sbAppend(opnds[1].getIntVal()+"");
			sbAppend(')');
			return null;
		} else if (op == OpCode.PLUS ||
				op == OpCode.TIMES ||
				op == OpCode.MINUS ||
				op == OpCode.OVER ||
				op == OpCode.MOD) {
			sbAppend('(');
			sbAppend(operator);
			sbAppend('(');
			sbAppend(opnds[0].getNumBits()+"");
			sbAppend(',');
			getStrInternal(opnds[0]);
			sbAppend(",");
			getStrInternal(opnds[1]);
			sbAppend("))");
			return null;
		
		} else if (op == OpCode.GEQ || 
				op == OpCode.GT ||
				op == OpCode.LEQ ||
				op == OpCode.LT ||
				op == OpCode.NUM_XOR) {
			sbAppend('(');
			sbAppend(operator);
			sbAppend('(');
			getStrInternal(opnds[0]);
			sbAppend(',');
			getStrInternal(opnds[1]);
			sbAppend("))");
			return null;
		
		} else if (op == OpCode.EXTRACT) {
			if (opnds.length == 2) {
				// Get String representation that accesses one bit of a bitvector
				// %s[%d:%d] 
				getStrInternal(opnds[0]);
				sbAppend(String.format("[%d:%d]", opnds[1].getIntVal(), opnds[1].getIntVal()));
				return null;
			} else if (opnds.length == 3) {
				// Get String representation that accesses a range in a bitvector
				// "%s[%d:%d]" 
				getStrInternal(opnds[0]);
				sbAppend(String.format("[%d:%d]", opnds[2].getIntVal(), opnds[1].getIntVal()));
				return null;
			}
			
		} else if (op ==  OpCode.IF_THEN_ELSE) {
		
			sbAppend("(IF ");
			getStrInternal(opnds[0]);
			sbAppend(" THEN ");
			getStrInternal(opnds[1]);
			sbAppend(" ELSE ");
			getStrInternal(opnds[2]);
			sbAppend(" ENDIF)");
			return null;
			
		} else if (op == OpCode.ARRNEW) {
			getNaryExpr(OpCode.CONCAT, opnds);
			return null;
		}
//			return String.format("(%s(%s, %s))", operator, getStrInternal(opnds[0]), getStrInternal(opnds[1]));	
	
		throw new IllegalStateException(op + " is not yet implemented");
	}

	@Override
	public String getTrueLiteral() {
		return "TRUE";
	}

	@Override
	public String getTypeForSolver(SmtType type) {
		Type t = type.getRealType();
		if (t instanceof TypePrimitive) {
			if (t == TypePrimitive.bittype) {
				return "BITVECTOR(1)";
			} else if (t ==TypePrimitive.booltype) {
				return "BOOLEAN";
			} else if (t == TypePrimitive.nulltype || 
					t == TypePrimitive.inttype ||
					t instanceof TypeStruct || 
					t instanceof TypeStructRef){
				return "BITVECTOR(" + type.getNumBits() + ")";	
			}
			
		} else if (t instanceof TypeArray) {
			TypeArray arrType = (TypeArray) t;
			// if the type is BIT[W], just use bit vector
			if (arrType.getBase() == TypePrimitive.bittype)
				return "BITVECTOR(" + BitVectUtil.vectSize(arrType)  + ")";
				
			return "Array[" + mIntNumBits + ":" + mIntNumBits + "]";
		}
		
		assert false : "NOT implemented";
		return null;
	}

	@Override
	public String getUnaryExpr(sketch.compiler.smt.SMTTranslator.OpCode op,
			String opnd) {
		return String.format("%s(%s)", opStrMap.get(op), opnd);
	}

	@Override
	protected void initOpCode() {
		opStrMap.put(OpCode.PLUS, "BVPLUS");
		opStrMap.put(OpCode.TIMES, "BVMULT");
		opStrMap.put(OpCode.MINUS, "BVSUB");
		opStrMap.put(OpCode.OVER, "SBVDIV");
		opStrMap.put(OpCode.MOD, "SBVMOD");
		
		opStrMap.put(OpCode.EQUALS, "=");
		
		opStrMap.put(OpCode.LT, "SBVLT");
		opStrMap.put(OpCode.LEQ, "SBVLE");
		opStrMap.put(OpCode.GT, "SBVGT");
		opStrMap.put(OpCode.GEQ, "SBVGE");

		
		opStrMap.put(OpCode.OR, "OR");
		opStrMap.put(OpCode.AND, "AND");
		opStrMap.put(OpCode.NOT, "NOT"); 
		opStrMap.put(OpCode.XOR, "XOR");
		
		opStrMap.put(OpCode.NUM_NOT, "~");
		opStrMap.put(OpCode.NUM_OR, "|");
		opStrMap.put(OpCode.NUM_AND, "&");
		opStrMap.put(OpCode.NEG, "BVUMINUS");
		opStrMap.put(OpCode.NUM_EQ, "=");
		opStrMap.put(OpCode.NUM_XOR, "BVXOR");
		opStrMap.put(OpCode.LSHIFT, "<<");
		opStrMap.put(OpCode.RSHIFT, ">>");
		
		opStrMap.put(OpCode.CONCAT, "@");
		opStrMap.put(OpCode.EXTRACT, "[:]");
		
		
//		opStrMap.put(OpCode.REPEAT, "repeat"); // not supported
//		opStrMap.put(OpCode.IF_THEN_ELSE, "ite"); // not supported
	}

	@Override
	public String prolog() {
		return "";
	}
	
	// helpers
	protected void insertBetween(String operator, NodeToSmtValue...opnds) {
		getStrInternal(opnds[0]);
		for (int i = 1; i < opnds.length; i++) {
			if (opnds.length > 2) 
				sbAppend('\n');
			else 
				sbAppend(' ');
			sbAppend(operator);
			sbAppend(' ');
			getStrInternal(opnds[i]);
		}
	}

	@Override
	public String getBitArrayLiteral(int numBits, int... arr) {
		StringBuffer sb = new StringBuffer();
		sb.append("0bin");
		for (int i = arr.length - 1; i >= 0; i--) {
			sb.append(arr[i]);
		}
		return sb.toString();
	}

}
