package sketch.compiler.smt.smtlib;

import java.io.PrintStream;
import java.util.List;

import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.ast.core.typs.TypeStruct;
import sketch.compiler.ast.core.typs.TypeStructRef;
import sketch.compiler.dataflow.abstractValue;
import sketch.compiler.smt.partialeval.BitVectUtil;
import sketch.compiler.smt.partialeval.FormulaPrinter;
import sketch.compiler.smt.partialeval.LinearNode;
import sketch.compiler.smt.partialeval.NodeToSmtValue;
import sketch.compiler.smt.partialeval.NodeToSmtVtype;
import sketch.compiler.smt.partialeval.SmtType;
import sketch.compiler.smt.partialeval.VarNode;

/**
 * This class translates NodeToSmtValue and NodeToSmtState into SMTLIB formulas
 * This class is stateless
 * 
 * @author Lexin Shan
 * @email lshan@eecs.berkeley.edu
 * 
 */
public class SMTLIBTranslatorBV extends FormulaPrinter {
	private static final String PREDICATE_FLAG = "#boolean#";

	protected int mIntNumBits;
	
	public SMTLIBTranslatorBV(NodeToSmtVtype formula, PrintStream ps, int intNumBIts) {
	    super(formula, ps);
		mIntNumBits = intNumBIts;
	}

	@Override
	public String getAssert(String predicate) {
		return ":assumption\n" + predicate;
	}

	@Override
	public String getDefineVar(String type, String varName) {
		if (type.equals(PREDICATE_FLAG)) {
			return (":extrapreds ((" + varName + "))");
		} else {
			return (":extrafuns ((" + varName + " " + type + "))");
		}
	}
	
	@Override
	public String prolog() {
		
		StringBuffer sb = new StringBuffer();
		sb.append("(benchmark BenchmarkName.smt"); sb.append('\n');
		sb.append(":logic QF_AUFBV"); sb.append('\n');
		sb.append(":extrafuns ((myarray BitVec[32] BitVec[32]))"); sb.append('\n');
		sb.append(":extrafuns ((mychar BitVec[32]))"); sb.append('\n');
		sb.append(":extrafuns ((myindex BitVec[32]))"); sb.append('\n');
		return sb.toString();
	
	}

	@Override
	public String epilog() {
		return ")";
	}

	public String getAssignment(String dest, String src, String idx,
			String newVal) {
		return "(store " + src + " " + idx + " " + newVal
				+ ")";
	}


	public String getArrayAcess(String base, String idx) {
		return "(select " + base + " " + idx + ")";
	}
	
	@Override
	public String getStr(NodeToSmtValue ntsv) {
	    
	    if (ntsv instanceof VarNode) {
	        VarNode vn = (VarNode) ntsv;
	        
	        if (mFormula.isArraySeed(vn)) {
	            return super.getStr(vn);
	        }
	        
	        if (mFormula.isLocalVar(vn) || mFormula.isArraySeed(vn) ||
	                (mFormula.isHole(vn) && !isSynthesis) ||
	                (mFormula.isInput(vn) && isSynthesis)){
	            return "?" + super.getStr(vn);
	            
	        } else {
	            return super.getStr(vn);
	        }
	        
	    }
	    return super.getStr(ntsv);
	}
	
	public String getStrForLinearNode(LinearNode linNode) {
	    StringBuffer sb = new StringBuffer();
        // +(o1, +(o2, c))
        for (VarNode vv : linNode.getVars()) {
            // +
            
            sb.append('(');
            sb.append(opStrMap.get(OpCode.PLUS));
            sb.append(' ');
            
            if (linNode.getCoeff(vv) == 1) {
                // 1*a = a
                sb.append(getStr(vv));
                sb.append(' ');
            } else {
                // *
                
                sb.append('(');
                sb.append(opStrMap.get(OpCode.TIMES));
                sb.append(' ');
                sb.append(getStr(vv));
                sb.append(' ');
                
                sb.append(getIntLiteral(linNode.getCoeff(vv), linNode.getNumBits()));
                sb.append(") ");
                
            }
        }
        sb.append(getIntLiteral(linNode.getCoeff(null), linNode.getNumBits()));
        for (int j = 0; j < linNode.getNumTerms(); j++)
            sb.append(')');
        return sb.toString();
    }
	
	@Override
	public String getNaryExpr(OpCode op,
			NodeToSmtValue... opnds) {
		
		StringBuffer sb = new StringBuffer();
//		if (op == OpCode.MOD)
//			throw new IllegalStateException("mod is not yet supported in SMTLIB");
		
		if (op == OpCode.EXTRACT) {
			if (opnds.length == 2)
				// Get String representation that accesses one bit of a bitvector
				return String.format("(extract[%d:%d] %s)", opnds[1].getIntVal(), opnds[1].getIntVal(), getStr(opnds[0]));
			else if (opnds.length == 3)	
				// Get String representation that accesses a range in a bitvector
				return String.format("(extract[%d:%d] %s)", opnds[2].getIntVal(), opnds[1].getIntVal(), getStr(opnds[0]));
			
		} else if (op == OpCode.REPEAT) {
			sb.append("(repeat[");
			sb.append(getStr(opnds[0]));
			sb.append("] ");
			sb.append(getStr(opnds[1]));
			sb.append(')');
			return sb.toString();
		
		} else if (op == OpCode.ARRNEW) {
			sb.append("bvbin");
			boolean isBitArray = true;
			for (int i = opnds.length-1; i >= 0; i--) {
				NodeToSmtValue elem = opnds[i];
				
				isBitArray = isBitArray && elem.isBit();
				sb.append(elem.getIntVal());
			}
			return sb.toString();
			
		} else {
		 	// other operations, including array update
			sb.append('(');
			sb.append(getOp(op));
			sb.append(' ');
			for (NodeToSmtValue opnd : opnds) {
				sb.append(' ');
				sb.append(getStr(opnd));
//				sb.append('\n');
			}
			sb.append(')');
			return sb.toString();
		}
		
		throw new IllegalStateException(op + " is not yet implemented");
	}

	@Override
	public String getUnaryExpr(OpCode op, String cond) {
		return "(" + getOp(op)  + " " + cond + ")";
	}

	@Override
	public String getTypeForSolver(SmtType type) {
		Type t = type.getRealType();
		if (t instanceof TypePrimitive) {
			if (t == TypePrimitive.bittype) {
				return "BitVec[1]";
			} else if (t ==TypePrimitive.booltype) {
				return PREDICATE_FLAG;
			} else if (t == TypePrimitive.nulltype || 
			        t == TypePrimitive.inttype ||
			        t instanceof TypeStruct || 
			        t instanceof TypeStructRef) {
				return "BitVec[" + type.getNumBits() + "]";
			}
			
		} else if (t instanceof TypeArray) {
			TypeArray arrType = (TypeArray) t;
			// if the type is BIT[W], just use bit vector
			if (arrType.getBase() == TypePrimitive.bittype)
				return "BitVec[" + BitVectUtil.vectSize(arrType) + "]";
				
			return "Array[" + mIntNumBits + ":" + mIntNumBits + "]";
		} 
		assert false : "NOT implemented";
		return null;
	}

	@Override
	protected void initOpCode() {

		opStrMap.put(OpCode.PLUS, "bvadd");
		opStrMap.put(OpCode.MINUS, "bvsub");
		opStrMap.put(OpCode.TIMES, "bvmul");
		opStrMap.put(OpCode.OVER, "bvudiv");
		opStrMap.put(OpCode.MOD, "bvsmod");

		opStrMap.put(OpCode.EQUALS, "="); // logical equals
		// TODO maybe i need a separate numerical equals here

		opStrMap.put(OpCode.LT, "bvslt");
		opStrMap.put(OpCode.LEQ, "bvsle");
		opStrMap.put(OpCode.GT, "bvsgt");
		opStrMap.put(OpCode.GEQ, "bvsge");

		
		opStrMap.put(OpCode.OR, "or");
		opStrMap.put(OpCode.AND, "and");
		opStrMap.put(OpCode.NOT, "not"); 
		opStrMap.put(OpCode.XOR, "xor");
		
		opStrMap.put(OpCode.NUM_NOT, "bvnot");
		opStrMap.put(OpCode.NUM_OR, "bvor");
		opStrMap.put(OpCode.NUM_AND, "bvand");
		opStrMap.put(OpCode.NEG, "bvneg");
		opStrMap.put(OpCode.NUM_EQ, "bvcomp");
		opStrMap.put(OpCode.NUM_XOR, "bvxor");
		opStrMap.put(OpCode.LSHIFT, "bvshl");
		opStrMap.put(OpCode.RSHIFT, "bvlshr");
		
		opStrMap.put(OpCode.CONCAT, "concat");
		opStrMap.put(OpCode.EXTRACT, "extract");
		opStrMap.put(OpCode.REPEAT, "repeat");
		opStrMap.put(OpCode.IF_THEN_ELSE, "ite");
		
		opStrMap.put(OpCode.ARRUPD, "store");
		opStrMap.put(OpCode.ARRACC, "select");
		// not equals needs to be done with special case in binary operator
	}
	
	@Override
	public String getIntLiteral(int i, int numBits) {
		return intToSmtLibBV(i, numBits);
	}

	@Override
	public String getFalseLiteral() {
		return "false";
	}

	@Override
	public String getTrueLiteral() {
		return "true";
	}
	
	@Override
	public String getBitArrayLiteral(int i, int numBits) {
		return intToSmtLibBV(i, numBits);
	}

	@Override
	public String getComment(String msg) {
		return ("; " + msg);
	}

	@Override
	public String getFuncCall(String funName, List<abstractValue> avlist,
			List<abstractValue> outSlist) {
		
		StringBuffer sb = new StringBuffer();
		sb.append('(');
		sb.append(funName);
		for (abstractValue av : avlist) {
			sb.append(' ');
			sb.append(getStr((NodeToSmtValue) av));
		}
		sb.append(')');
		
		return sb.toString();
	}
	
	public static String intToSmtLibBV(int i, int numBits) {
		long longVal = i;
		if (i < 0) {
			longVal = 4294967296L + longVal;
		}
		return "bv" + longVal + "[" + numBits + "]";
	}

	@Override
	public String getBitArrayLiteral(int numBits, int... arr) {
		StringBuffer sb = new StringBuffer();
		sb.append("bv");
		for (int i = arr.length - 1; i >= 0; i--) {
			sb.append(arr[i]);
		}
		sb.append('[');
		sb.append(numBits);
		sb.append(']');
		return sb.toString();
	}

    @Override
    public String getLetFormula(NodeToSmtValue formula) {
        return getStr(formula);
    }

    @Override
    public String getLetHead() {
        return ":assumption";
    }

    @Override
    public String getLetLine(NodeToSmtValue dest, NodeToSmtValue def) {
        StringBuffer sb = new StringBuffer();
        sb.append("(let (");
        sb.append(getStr(dest));
        sb.append(" ");
        sb.append(getStr(def));
        sb.append(")");
        return sb.toString();
    }

    @Override
    public String getLetTail(int numLets) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < numLets; i++)
            sb.append(')');
        return sb.toString();
    }

}