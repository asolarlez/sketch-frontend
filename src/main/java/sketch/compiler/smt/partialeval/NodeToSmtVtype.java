package sketch.compiler.smt.partialeval;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import sketch.compiler.CommandLineParamManager;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprStar;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.dataflow.abstractValue;
import sketch.compiler.smt.SMTTranslator;
import sketch.compiler.smt.SMTTranslator.OpCode;
import sketch.compiler.solvers.constructs.AbstractValueOracle;
import sketch.compiler.solvers.constructs.StaticHoleTracker;
import sketch.util.DisjointSet;

public abstract class NodeToSmtVtype extends TypedVtype implements ISuffixSetter {
	
	public class DebugPrinter extends FormulaVisitor {

	    StringBuffer sb = new StringBuffer();
	   
		public DebugPrinter() { }
		
		@Override
		public Object visitConstNode(ConstNode constNode) {
		    sb.append(constNode.getIntVal());
		    return constNode;
		}
		
		@Override
		public Object visitVarNode(VarNode varNode) {
		    sb.append(varNode.getRHSName());
		    return varNode;
		}

		public Object visitLinearNode(LinearNode ln) {
		    
	        sb.append(ln.getCoeff(null));
	        
	        for (VarNode v : ln.getVars()) {
	            sb.append("+");
	           sb.append(ln.getCoeff(v));
	           sb.append('*');
	           v.accept(this);
	           
	        }
	        return ln;
		}
		
		@Override
		public Object visitLabelNode(LabelNode labelNode) {
		    sb.append(labelNode.toString());
		    return labelNode;
		}
		
		@Override
		public Object visitOpNode(OpNode opNode) {
	        
	        if (opNode.getOpcode() == OpCode.IF_THEN_ELSE) {
	            sb.append('(');
                opNode.getOperands()[0].accept(this);
                
	            sb.append(" ? ");
	            opNode.getOperands()[1].accept(this);
	            
	            sb.append(" : ");
	            opNode.getOperands()[2].accept(this);
                
	            sb.append(')');
	        } else {
	            sb.append('(');
	            for (NodeToSmtValue opnd : opNode.getOperands()) {
	                sb.append(OpNode.getCanonicalOp(opNode.getOpcode()));
	                opnd.accept(this);
	                
	            }
	            sb.append(')');    
	        }
	        return opNode;
		}
		
		
	}
	
	public class NodeReplacer extends FormulaVisitor {
	    
	    @Override
	    public Object visitOpNode(OpNode opNode) {
	        
            // if the current structure is found in structural hash
	        // return the single var that's equivalent to it
            if (mStructHash.containsKey(opNode)) {
                structSimplified++;
                return mStructHash.get(opNode);
            }
	        
	        return replaceChildren(opNode);
	    }

        public Object replaceChildren(OpNode opNode) {
            boolean changed = false;
	        NodeToSmtValue[] newOpnds = new NodeToSmtValue[opNode.getOperands().length];
	        
	        int i = 0;
	        for (NodeToSmtValue oldNode : opNode.getOperands()) {
	            NodeToSmtValue newNode = (NodeToSmtValue) oldNode.accept(this);
	            
	            if (newNode != oldNode) {
	                changed = true;
	            }
	            newOpnds[i] = newNode;
	            
	            i++;
	        }
	        
	        if (changed) {
	            OpNode newOpNode = new OpNode(opNode.getType(), opNode.getNumBits(), opNode.getOpcode(), newOpnds);
	            return checkCache(newOpNode);
	        } else {
	            return opNode;
	        }
        }
	}
	
	
	
	public static final boolean USE_STRUCT_HASHING = true;
	private int structHashingUsed = 0;
	public static final boolean FUNCCALL_HASHING = CommandLineParamManager.getParams().hasFlag("funchash");
	private int funccallInlined = 0;
	public static final boolean FLAT = false;
	private int structSimplified = 0;
	public static final boolean CANONICALIZE = CommandLineParamManager.getParams().hasFlag("canon");;

	
	protected TempVarGen tmpVarGen;
	protected AbstractValueOracle oracle;
	
	protected int intNumBits;
	protected int mInBits;
	protected int mCBits;
	
	private static Logger log = Logger.getLogger(NodeToSmtVtype.class.getCanonicalName());

	protected DisjointSet<VarNode> mTransitiveSet;
	
	/*
	 * Getters & Setters
	 */
	
	public int getIntNumBits() { return intNumBits; }
	
	public StaticHoleTracker getHoleNamer() { return mHoleNamer; }
	
	public DisjointSet<VarNode> getEquivalenceSet() {
	    return mTransitiveSet;
	}
	
	/**
	 * Constructor
	 * 
	 * @param mOutputOracle
	 * @param smtFormula
	 * @param smtTran
	 * @param intNumBits
	 * @param tmpVarGen
	 * @param out 
	 */
	public NodeToSmtVtype(
			int intNumBits,
			int inBits,
			int cBits,
			TempVarGen tmpVarGen) {
		super();
		
		this.intNumBits = intNumBits;
		mCBits = cBits;
		mInBits = inBits;
		this.tmpVarGen = tmpVarGen;
		
		mInputs = new HashSet<VarNode>();
		mLocalVars = new HashSet<VarNode>();
		
		mEq = new LinkedList<NodeToSmtValue>();
		mCache = new HashMap<NodeToSmtValue, NodeToSmtValue>(100000);
		mStructHash = new HashMap<NodeToSmtValue, NodeToSmtValue>(100000);
		mFuncHash = new HashMap<NodeToSmtValue, List<NodeToSmtValue>>();
		mSimpleDefs = new HashMap<NodeToSmtValue, NodeToSmtValue>();
		mUses = new HashMap<NodeToSmtValue, Integer>();
		mAsserts = new LinkedList<NodeToSmtValue>();
		mHoles = new HashSet<VarNode>();
		mArraySeeds = new HashSet<VarNode>();
		
		mTransitiveSet = new DisjointSet<VarNode>();
		
		
		mHoleNamer = new StaticHoleTracker(tmpVarGen);
		mSharedFalseValue = CONST(false);
	}
	
	/*
	 * vtype operations
	 */
	
//	@Override
//	public NodeToSmtValue BOTTOM() {
//		return new NodeToSmtValue();
//	}
//	@Override
//	public abstractValue BOTTOM(String label) {
//		return new NodeToSmtValue(label, null);
//	}
	
	public NodeToSmtValue ARR(List<abstractValue> vals) {
		boolean isBitArray = true;
		int base = 1;
		int accu = 0;
		int bit = 0;
		
		NodeToSmtValue[] arr = new NodeToSmtValue[vals.size()];
		int i = 0;
		boolean allConst = true;
		for (abstractValue av : vals) {
			NodeToSmtValue ntsv = (NodeToSmtValue) av;
			
			if (!ntsv.isConst()) {
				allConst = false;
			} else {
				bit = ntsv.getIntVal();
				accu = accu + bit * base;
				base = base << 1;
			}
			
			if (!ntsv.isBit())
				isBitArray = false;
			
			arr[i] = ntsv;
			i++;
			
		}
		
		if (isBitArray) {
			if (allConst && vals.size() <= 32) {
				NodeToSmtValue newNode = NodeToSmtValue.newBitArray(accu, vals.size());
				return checkCache(newNode);
	
			} else {
				NodeToSmtValue newNode = NodeToSmtValue.newList(arr);
				return checkCache(newNode);
			}
		} else {
			NodeToSmtValue newNode = NodeToSmtValue.newList(arr);
			return checkCache(newNode);
		}
	}
	
	public NodeToSmtValue ARR(abstractValue val, int size) {
		NodeToSmtValue newNode = NodeToSmtValue.newListOf((NodeToSmtValue) val, size);
		return checkCache(newNode);
	}
	
	public VarNode STATE_DEFAULT(String label, Type realType, int numBits, int rhsIdx) {
		VarNode newNode = NodeToSmtValue.newStateDefault(label, realType, numBits, rhsIdx);
		newNode = (VarNode) checkCache(newNode);
		declareLocalVar(newNode);
		
		return newNode;
	}
	
	public VarNode STATE_ELE_DEFAULT(String label, Type realType, int numBits, int rhsIdx) {
		VarNode newNode = NodeToSmtValue.newStateArrayEleDefault(label, realType, numBits, rhsIdx);
		newNode = (VarNode) checkCache(newNode);
		declareLocalVar(newNode);
	
        return newNode;
	}
	/**
	 * create a BOTTOM NodeToSmtValue object with the specified label
	 * and type. This method will use the default numBits for the type.
	 */
	public NodeToSmtValue BOTTOM(Type type, OpCode opcode, NodeToSmtValue...operands) {
		// whenever we create a new OpNode, check structural hashing first
		
		NodeToSmtValue[] newOpnds = new NodeToSmtValue[operands.length];
		for (int i = 0 ; i < newOpnds.length; i++) {
			newOpnds[i] = referenceVar(operands[i]);	
		}
		NodeToSmtValue newNode = NodeToSmtValue.newBottom(type, getNumBitsForType(type), opcode, newOpnds);
		NodeToSmtValue nInSH = checkStructuralHash(newNode);
//		if (nInSH == newNode) {
//		    addTempDefinition(newNode);
//		    newNode = checkStructuralHash(newNode);
//		}
		return nInSH;
	}

	private boolean isLinearizable(NodeToSmtValue ntsv1) {
        return ntsv1 instanceof LinearNode || ntsv1 instanceof ConstNode || ntsv1 instanceof VarNode;
    }
	
	public NodeToSmtValue LINEAR_PLUS(NodeToSmtValue v1, NodeToSmtValue v2, boolean isMinus) {
	    NodeToSmtValue newNode;
	    // Const and Var
	    // Const and Linear
	    // Linear and Linear
	    // Var and Linear
	    LinearNode l1;
	    LinearNode l2;

	    l1 = LINEARIZE(v1);
	    l2 = LINEARIZE(v2);
	    
	    newNode = new LinearNode(l1, l2, isMinus);
	    newNode = checkCache(newNode);
	    return checkStructuralHash(newNode);
	}
	
	public NodeToSmtValue LINEAR_MULT(NodeToSmtValue lineraizableVar, ConstNode c) {
	    
	    LinearNode lin = LINEARIZE(lineraizableVar);
	    NodeToSmtValue newNode = new LinearNode(lin, c);
	    
	    newNode = checkCache(newNode);
	    return checkStructuralHash(newNode);
	}

    private LinearNode LINEARIZE(NodeToSmtValue v2) {
        LinearNode l2;
        NodeToSmtValue def;
        if (v2 instanceof ConstNode) {
	        l2 = new LinearNode((ConstNode) v2);
	    } else if (v2 instanceof VarNode) {
	        def = findOriginalDef((VarNode) v2);
	        if (def instanceof LinearNode)
	            l2 = (LinearNode) def;
	        else
	            l2 = new LinearNode((VarNode) v2);
	    } else
	        l2 = (LinearNode) v2;
        
        l2 = (LinearNode) checkCache(l2);
        return l2;
    }
	
	@Override
	public NodeToSmtValue CONST(boolean v) {
		NodeToSmtValue newNode = NodeToSmtValue.newBool(v);
		return checkCache(newNode);
	}
	
	@Override
	public NodeToSmtValue CONST(int v) {
		NodeToSmtValue newNode = NodeToSmtValue.newInt(v, intNumBits);
		return checkCache(newNode);
	}
	
	@Override
	public NodeToSmtValue CONSTBIT(int v) {
		NodeToSmtValue newNode = NodeToSmtValue.newBit(v);
		return checkCache(newNode);
	}
	
	@Override
	public NodeToSmtValue CONSTBITARRAY(int intVal, int size) {
		NodeToSmtValue newNode = NodeToSmtValue.newBitArray(intVal, size);
		return checkCache(newNode);
	}
	
	public NodeToSmtValue LABEL(Type realLabelType, int numBits, String label) {
		NodeToSmtValue newNode = NodeToSmtValue.newLabel(realLabelType, numBits, label);
		return checkCache(newNode);
	}
	
	public NodeToSmtValue FUNCCALL(Type realRetType, String funcName, NodeToSmtValue...args) {
		
		NodeToSmtValue[] newOpnds = new NodeToSmtValue[args.length];
		for (int i = 0 ; i < newOpnds.length; i++) {
			newOpnds[i] = referenceVar(args[i]);	
		}
		NodeToSmtValue newNode = NodeToSmtValue.newFuncCall(realRetType, getNumBitsForType(realRetType), funcName, newOpnds);
		return checkCache(newNode);
	}
	
	@Override
	public void Assert(abstractValue val, String msg) {
		NodeToSmtValue ntsv = (NodeToSmtValue) val;

		if (val.hasIntVal()) {
			if (val.getIntVal() == 0)
				throw new AssertionFailedException("Assertion failure: " + msg);
		} else {
			addAssert(ntsv);
		}
	}
	
	@Override
	public abstractValue arracc(abstractValue arr, abstractValue idx,
			abstractValue len, boolean isUnchecked) {

		NodeToSmtValue ntsvArr = (NodeToSmtValue) arr;
		NodeToSmtValue ntsvIdx = (NodeToSmtValue) idx;
		NodeToSmtValue ntsvLen = (NodeToSmtValue) len;
		
		if (ntsvArr.isBitArray()) {
		    return bitArrayAccess(ntsvArr, ntsvIdx, ntsvLen, isUnchecked);
		} else {
		    return normalArrayAccess(ntsvArr, ntsvIdx, ntsvLen, isUnchecked);
		}
	}
	
	public abstractValue arrupd(NodeToSmtValue arr, NodeToSmtValue idx, NodeToSmtValue val){
	    return BOTTOM(arr.getType(), OpCode.ARRUPD, arr, idx, val);
	}
	
	protected NodeToSmtValue bitArrayAccess(NodeToSmtValue ntsvArr, NodeToSmtValue ntsvIdx,
            NodeToSmtValue len, boolean isUnchecked) {
	    
	    int size = BitVectUtil.vectSize(ntsvArr.getType());
	    
	    if (!ntsvIdx.isConst() || !len.isConst()) {
            // index is bottom or length is bottom
            return rawArraccRecursiveBitArray(ntsvArr, ntsvIdx, 0, 
                    size, len.getIntVal());
            
        } else {
            int iidx = ntsvIdx.getIntVal();
            
            if((iidx < 0 || iidx >= size)  )
                throw new ArrayIndexOutOfBoundsException("ARRAY OUT OF BOUNDS !(0<=" + iidx + " < " + size+") ");
            
            return extract(ntsvIdx.getIntVal() + len.getIntVal() - 1, ntsvIdx.getIntVal(), ntsvArr);
        }
	    
	}
	
	protected NodeToSmtValue rawArraccRecursiveBitArray(NodeToSmtValue arr, NodeToSmtValue idx, int i, int size, int len) {
        if (i + len - 1 == size - 1) {
            // the last element
            return extract(i+len-1, i, arr);
        } else {
            return condjoin(eq(idx, CONST(i)), extract(i+len-1, i, arr), 
                        rawArraccRecursiveBitArray(arr, idx, i+1, size, len));
        }
    }
	
	protected NodeToSmtValue normalArrayAccess(NodeToSmtValue arr, NodeToSmtValue idx,
	        NodeToSmtValue len, boolean isUnchecked) {
	    if (!idx.isConst() || !len.isConst()) {
            // index is bottom or length is bottom
            return handleNormalArrayRawAccess(arr, idx, len, isUnchecked);
            
        } else {
            return handleNormalArrayConstAccess(arr, idx, len, isUnchecked);
        }
	}

	protected abstract NodeToSmtValue handleNormalArrayConstAccess(NodeToSmtValue arr,
            NodeToSmtValue idx, 
            NodeToSmtValue len, 
            boolean isUnchecked);
	
	protected abstract NodeToSmtValue handleNormalArrayRawAccess(NodeToSmtValue arr, 
	        NodeToSmtValue idx,
	        NodeToSmtValue len, 
	        boolean isUnchecked);
	
	@Override
	public abstractValue outOfBounds(TypedValue arr) {
		NodeToSmtValue ntsvArr = (NodeToSmtValue) arr;
		Type elemType = getElementType(ntsvArr.getType());
		// FIXME what's the numBits for an array type?
		SmtType smtType = SmtType.create(elemType, intNumBits);
		
		return defaultValue(smtType);
	}
	
	@Override
	public NodeToSmtValue condjoin(abstractValue cond, abstractValue vtrue,
			abstractValue vfalse) {
		NodeToSmtValue ntsvCond = (NodeToSmtValue) cond;
		NodeToSmtValue ntsvTrue = (NodeToSmtValue) vtrue;
		NodeToSmtValue ntsvFalse = (NodeToSmtValue) vfalse;
		int maxNumBits = Math.max(ntsvTrue.getNumBits(), ntsvFalse.getNumBits());
		
		if (ntsvTrue.getNumBits() < maxNumBits)
			ntsvTrue = padIfNotWideEnough(ntsvTrue, maxNumBits);
		
		if (ntsvFalse.getNumBits() < maxNumBits)
			ntsvFalse = padIfNotWideEnough(ntsvFalse, maxNumBits);
		
		if (ntsvCond.hasIntVal()) {
			if (ntsvCond.getIntVal() == 0)
				return ntsvFalse;
			else
				return ntsvTrue;
		} else {
			return BOTTOM(ntsvTrue.getType(), 
				OpCode.IF_THEN_ELSE,
						ntsvCond,
						ntsvTrue, 
						ntsvFalse
				);
		}
	}
	
	
	@Override
	public NodeToSmtValue plus(abstractValue v1, abstractValue v2) {
		NodeToSmtValue ntsv1 = (NodeToSmtValue) v1;
		NodeToSmtValue ntsv2 = (NodeToSmtValue) v2;

		if (v1.hasIntVal() && v2.hasIntVal()) {
			return CONST(v1.getIntVal() + v2.getIntVal());
		} else {
		    
	        
			if (CANONICALIZE &&
			        (isLinearizable(ntsv1)) &&
                    (isLinearizable(ntsv2))) {
			    // if they are either ConstNode or LinearNode or VarNode
		        
			    NodeToSmtValue lin = LINEAR_PLUS(ntsv1, ntsv2, false);

			    return lin;
			} else {
			    // if either one is OpNode, nothing we can do.
			    return mergeTwoValuesToBottom(OpCode.PLUS, ntsv1, ntsv2);
			}
		}
	}

	@Override
	public NodeToSmtValue minus(abstractValue v1, abstractValue v2) {
		NodeToSmtValue ntsv1 = (NodeToSmtValue) v1;
		NodeToSmtValue ntsv2 = (NodeToSmtValue) v2;

		if (v1.hasIntVal() && v2.hasIntVal()) {
			return CONST(v1.getIntVal() - v2.getIntVal());
		} else {
		    int maxNumBits = Math.max(ntsv1.getNumBits(), ntsv2.getNumBits());
	        
	        if (ntsv1.getNumBits() < maxNumBits)
	            ntsv1 = padIfNotWideEnough(ntsv1, maxNumBits);
	        
	        if (ntsv2.getNumBits() < maxNumBits)
	            ntsv2 = padIfNotWideEnough(ntsv2, maxNumBits);
	        
		    if (CANONICALIZE &&
                    (isLinearizable(ntsv1)) &&
                    (isLinearizable(ntsv2))) {
                // if they are either ConstNode or LinearNode or VarNode
                
//		        PrintStream ps = System.err;
//                ps.println(ntsv1 + "\t" + ntsv2);
                NodeToSmtValue lin = LINEAR_PLUS(ntsv1, ntsv2, true);
//                ps.println(lin);
                return lin;
            } else {
                // if either one is OpNode, nothing we can do.
                return mergeTwoValuesToBottom(OpCode.MINUS, ntsv1, ntsv2);
            }
			
		}
	}

	@Override
	public NodeToSmtValue times(abstractValue v1, abstractValue v2) {
		NodeToSmtValue ntsv1 = (NodeToSmtValue) v1;
		NodeToSmtValue ntsv2 = (NodeToSmtValue) v2;

		if (v1.hasIntVal() && v2.hasIntVal()) {
			return CONST(v1.getIntVal() * v2.getIntVal());
		} else if (CANONICALIZE) {
		        if (ntsv1 instanceof ConstNode && 
		                isLinearizable(ntsv2)) {
		            return LINEAR_MULT(ntsv2, (ConstNode) ntsv1);
		        } else if (isLinearizable(ntsv1) &&
		                ntsv2 instanceof ConstNode) {
		            return LINEAR_MULT(ntsv1, (ConstNode) ntsv2);
                }
		} 
		return mergeTwoValuesToBottom(OpCode.TIMES, ntsv1, ntsv2);
		
	}

	@Override
	public NodeToSmtValue over(abstractValue v1, abstractValue v2) {
		NodeToSmtValue ntsv1 = (NodeToSmtValue) v1;
		NodeToSmtValue ntsv2 = (NodeToSmtValue) v2;

//		if (v2.hasIntVal() && v2.getIntVal() == 0)
		state.Assert(not(eq(v2, CONST(0))), "divide by zero", false);
		
		
		if (v1.hasIntVal() && v2.hasIntVal()) {
			return CONST(v1.getIntVal() / v2.getIntVal());
		} else {
			return mergeTwoValuesToBottom(OpCode.OVER, ntsv1, ntsv2);
		}
	}

	public NodeToSmtValue and(NodeToSmtValue...opnds) {
		return BOTTOM(opnds[0].getType(),
				OpCode.AND, opnds
				);
	}
	
	@Override
	public NodeToSmtValue and(abstractValue v1, abstractValue v2) {
		NodeToSmtValue ntsv1 = (NodeToSmtValue) v1;
		NodeToSmtValue ntsv2 = (NodeToSmtValue) v2;

		if (ntsv1.isConst() && ntsv2.isConst()) {
			if (ntsv1.isBool())
				return CONST(ntsv1.getBoolVal() && ntsv2.getBoolVal());
			else
				return mergeTwoValuesToBottom(OpCode.NUM_AND, ntsv1, ntsv2);
		}
		
		// if ntsv1 is constant and true, return ntsv2
		if (ntsv1.isConst() && 
				((ntsv1.isBool() && ntsv1.getBoolVal()) ||
					ntsv1.isBit() && ntsv1.getIntVal() != 0))
			return ntsv2;
		
		// if ntsv2 is constant and true, return ntsv1
		if (ntsv2.isConst() && 
				((ntsv2.isBool() && ntsv2.getBoolVal()) ||
					ntsv2.isBit() && ntsv2.getIntVal() != 0))
			return ntsv1;
		
		// if ntsv1 is constant and false, return false
		if (ntsv1.isConst()) {
			if (ntsv1.isBool() && !ntsv1.getBoolVal())
				return CONST(false);
			if (ntsv1.isBit() && ntsv1.getIntVal() == 0)
				return CONSTBIT(0);
		}
		// if ntsv2 is constant and false, return false
		if (ntsv2.isConst()) {
			if (ntsv2.isBool() && !ntsv2.getBoolVal()) 
				return CONST(false);
			if (ntsv2.isBit() && ntsv2.getIntVal() == 0)
				return CONSTBIT(0);
		}

		
		if (ntsv2.isBool()) {
			assert ntsv2.isBool() : "Inconstent type between AND operands";
			return mergeTwoValuesToBottom(OpCode.AND, ntsv1, ntsv2);
		} else {
			return mergeTwoValuesToBottom(OpCode.NUM_AND, ntsv1, ntsv2);
		}
		
	}
	
	public NodeToSmtValue or(NodeToSmtValue...opnds) {
		return BOTTOM(opnds[0].getType(),
				OpCode.OR, opnds
				);
	}

	@Override
	public NodeToSmtValue or(abstractValue v1, abstractValue v2) {
		NodeToSmtValue ntsv1 = (NodeToSmtValue) v1;
		NodeToSmtValue ntsv2 = (NodeToSmtValue) v2;

		if (ntsv1.isConst() && ntsv2.isConst()) {
			if (ntsv1.isBool())
				return CONST(ntsv1.getBoolVal() || ntsv2.getBoolVal());
			else
				return mergeTwoValuesToBottom(OpCode.NUM_OR, ntsv1, ntsv2);
		}
		
		if (ntsv1.isConst()) {
			if (ntsv1.isBool() && ntsv1.getBoolVal())
				return CONST(true);
			if (ntsv1.isBit() && ntsv1.getIntVal() != 0)
				return CONSTBIT(1);
		}
		
		if (ntsv2.isConst()) {
			if (ntsv2.isBool() && ntsv2.getBoolVal())
				return CONST(true);
			if (ntsv2.isBit() && ntsv2.getIntVal() != 0)
				return CONSTBIT(1);
		}
		
		if (ntsv1.isConst() && 
				((ntsv1.isBool() && !ntsv1.getBoolVal()) ||
					ntsv1.isBit() && ntsv1.getIntVal() == 0))
			return ntsv2;
		
		if (ntsv2.isConst() && 
				((ntsv2.isBool() && !ntsv2.getBoolVal()) ||
					ntsv2.isBit() && ntsv2.getIntVal() == 0))
			return ntsv1;


		if (ntsv2.isBool()) {
			assert ntsv2.isBool() : "Inconstent type between OR operands";
			return mergeTwoValuesToBottom(OpCode.OR, ntsv1, ntsv2);
		} else {
			return mergeTwoValuesToBottom(OpCode.NUM_OR, ntsv1, ntsv2);
		}
		
	}

	@Override
	public NodeToSmtValue xor(abstractValue v1, abstractValue v2) {

		NodeToSmtValue ntsv1 = (NodeToSmtValue) v1;
		NodeToSmtValue ntsv2 = (NodeToSmtValue) v2;

		// if both are constants
		if (ntsv1.isConst() && ntsv2.isConst()) {
			if (ntsv1.isBool()) {
				assert ntsv2.isBool() : "Inconstent type between XOR operands";
				return CONST(ntsv1.getBoolVal() ^ ntsv2.getBoolVal());
			} else {
				return mergeTwoValuesToBottom(OpCode.NUM_XOR, ntsv1, ntsv2);
			}
		}
		
		if (ntsv1.isBool()) {
			assert ntsv2.isBool() : "Inconstent type between XOR operands";
			return mergeTwoValuesToBottom(OpCode.XOR, ntsv1, ntsv2);
		} else {
			return mergeTwoValuesToBottom(OpCode.NUM_XOR, ntsv1, ntsv2);
		}

	}
	
	@Override
	public NodeToSmtValue eq(abstractValue v1, abstractValue v2) {
		NodeToSmtValue ntsv1 = (NodeToSmtValue) v1;
		NodeToSmtValue ntsv2 = (NodeToSmtValue) v2;

		if (v1.hasIntVal() && v2.hasIntVal()) {
			return CONST(v1.getIntVal() == v2.getIntVal());
		}

		return mergeTwoValuesToBottomWithType(OpCode.EQUALS, ntsv1, ntsv2, TypePrimitive.booltype);
	}
	
    public NodeToSmtValue assign(abstractValue v1, abstractValue v2) {
        NodeToSmtValue ntsv1 = (NodeToSmtValue) v1;
        NodeToSmtValue ntsv2 = (NodeToSmtValue) v2;

        if (v1.hasIntVal() && v2.hasIntVal()) {
            return CONST(v1.getIntVal() == v2.getIntVal());
        }

        NodeToSmtValue newNode = NodeToSmtValue.newBottom(TypePrimitive.booltype, 
                getNumBitsForType(TypePrimitive.booltype), 
                OpCode.EQUALS, ntsv1, ntsv2);    
        
        return checkStructuralHash(newNode);
    }

	@Override
	public NodeToSmtValue le(abstractValue v1, abstractValue v2) {
		NodeToSmtValue ntsv1 = (NodeToSmtValue) v1;
		NodeToSmtValue ntsv2 = (NodeToSmtValue) v2;

		if (v1.hasIntVal() && v2.hasIntVal()) {
			return CONST(v1.getIntVal() <= v2.getIntVal());
		}

		return mergeTwoValuesToBottomWithType(OpCode.LEQ, ntsv1, ntsv2, TypePrimitive.booltype);
	}

	@Override
	public NodeToSmtValue lt(abstractValue v1, abstractValue v2) {
		NodeToSmtValue ntsv1 = (NodeToSmtValue) v1;
		NodeToSmtValue ntsv2 = (NodeToSmtValue) v2;
		if (v1.hasIntVal() && v2.hasIntVal()) {
			return CONST(v1.getIntVal() < v2.getIntVal());
		}

		return mergeTwoValuesToBottomWithType(OpCode.LT, ntsv1, ntsv2, TypePrimitive.booltype);

	}

	@Override
	public NodeToSmtValue ge(abstractValue v1, abstractValue v2) {
		NodeToSmtValue ntsv1 = (NodeToSmtValue) v1;
		NodeToSmtValue ntsv2 = (NodeToSmtValue) v2;

		if (v1.hasIntVal() && v2.hasIntVal()) {
			return CONST(v1.getIntVal() >= v2.getIntVal());
		}
		
		return mergeTwoValuesToBottomWithType(OpCode.GEQ, ntsv1, ntsv2, TypePrimitive.booltype);
	}

	@Override
	public NodeToSmtValue gt(abstractValue v1, abstractValue v2) {
		NodeToSmtValue ntsv1 = (NodeToSmtValue) v1;
		NodeToSmtValue ntsv2 = (NodeToSmtValue) v2;

		if (v1.hasIntVal() && v2.hasIntVal()) {
			return CONST(v1.getIntVal() > v2.getIntVal());
		}
		
		return mergeTwoValuesToBottomWithType(OpCode.GT, ntsv1, ntsv2, TypePrimitive.booltype);

	}
	
	@Override
	public NodeToSmtValue mod(abstractValue v1, abstractValue v2) {
		NodeToSmtValue ntsv1 = (NodeToSmtValue) v1;
		NodeToSmtValue ntsv2 = (NodeToSmtValue) v2;
		
		state.Assert(not(eq(v2, CONST(0))), "mod by zero", false);
		
		if (v1.hasIntVal() && v2.hasIntVal()) {
			return CONST(v1.getIntVal() % v2.getIntVal());
		} else {
			return mergeTwoValuesToBottom(OpCode.MOD, ntsv1, ntsv2);
		}
	}

	@Override
	public NodeToSmtValue neg(abstractValue v1) {
		NodeToSmtValue ntsv = (NodeToSmtValue) v1;
		if (v1.hasIntVal()) {
			return CONST(-v1.getIntVal());
		} else {
			return BOTTOM(ntsv.getType(),
					OpCode.NEG, ntsv
					);
		}
	}

	// UNARY OPERATIONS
	@Override
	public NodeToSmtValue not(abstractValue v1) {
		NodeToSmtValue ntsv = (NodeToSmtValue) v1;

		if (ntsv.isConst()) {
			// constant
			if (ntsv.isBool())
				return ntsv.getBoolVal() ? CONST(false) : CONST(true);
			else if (ntsv.isBit())
				return ntsv.getIntVal() == 0 ? CONSTBIT(1) : CONSTBIT(0);
			else if (ntsv.isInt())
				return ntsv.getIntVal() == 0 ? CONST(1) : CONST(0);
			else if (ntsv.isBitArray())
				return CONSTBITARRAY(~ ntsv.getIntVal(), ntsv.getNumBits());
			else
				throw new IllegalStateException("unexptected constant type in TypedVtype.not()");
		} else {
			// not a constant
			if (ntsv.isBool())
				return BOTTOM(ntsv.getType(),
						OpCode.NOT, ntsv);
			else
				return BOTTOM(ntsv.getType(),
						OpCode.NUM_NOT, ntsv);
		}
	}
	
	// SHIFTS
	@Override
	public abstractValue shl(abstractValue v1, abstractValue v2) {
		NodeToSmtValue ntsv1 = (NodeToSmtValue) v1;
		NodeToSmtValue ntsv2 = (NodeToSmtValue) v2;
		
		int leftBits = ntsv1.getNumBits();
		int rightBits = ntsv2.getNumBits();
		
		NodeToSmtValue left = ntsv1;
		NodeToSmtValue right;
		if (leftBits < rightBits)
			right = extract(leftBits-1, 0, ntsv2);
		else if (leftBits > rightBits)
			right = concatToNewType(CONSTBITARRAY(0, leftBits-rightBits), ntsv2, ntsv1.getType());
		else
			right = ntsv2;
		
		return BOTTOM(ntsv1.getType(), OpCode.LSHIFT, left, right);
	}
	
	@Override
	public abstractValue shr(abstractValue v1, abstractValue v2) {
		NodeToSmtValue ntsv1 = (NodeToSmtValue) v1;
		NodeToSmtValue ntsv2 = (NodeToSmtValue) v2;
		
		int leftBits = ntsv1.getNumBits();
		int rightBits = ntsv2.getNumBits();
		
		NodeToSmtValue left = ntsv1;
		NodeToSmtValue right;
		if (leftBits < rightBits)
			right = extract(leftBits-1, 0, ntsv2);
		else if (leftBits > rightBits)
			right = concatToNewType(CONSTBITARRAY(0, leftBits-rightBits), ntsv2, ntsv1.getType());
		else
			right = ntsv2;
		
		return BOTTOM(ntsv1.getType(), OpCode.RSHIFT, left, right);
	}
	
	/**
	 * extract end+1-start bits from fromValue
	 * @param end inclusive
	 * @param start inclusive
	 * @param fromValue
	 * @return
	 */
	public NodeToSmtValue extract(int end, int start, NodeToSmtValue fromValue) {
		Type newType = (end == start) ? TypePrimitive.bittype : BitVectUtil.newBitArrayType(end - start + 1);
		return BOTTOM(newType, OpCode.EXTRACT, fromValue, CONST(start), CONST(end));
	}
	
	public NodeToSmtValue concatToNewType(NodeToSmtValue first, NodeToSmtValue second, Type newType) {
		assert first.isBitArray() || first.isBit() || first.isInt(): "concat can only be applied to bitarray";
		assert second.isBitArray() || second.isBit() || second.isInt(): "concat can only be applied to bitarray";
		
		return BOTTOM(newType,
				OpCode.CONCAT, first, second);
	}
	
	public NodeToSmtValue concat(NodeToSmtValue first, NodeToSmtValue second) {
//		assert first.isBitArray() || first.isBit() : "concat can only be applied to bitarray";
//		assert second.isBitArray() || second.isBit() : "concat can only be applied to bitarray";
		
//		if (first.hasIntVal() && first.getIntVal() == 0) {
//			return new NodeToSmtValue(second.name, 
//					second.type, 
//					second.smtStatus, 
//					first.getNumBits() + second.getNumBits(), 
//					second.obj, ((NodeToSmtValue) second).lhsIdx);
//		} else {
			return BOTTOM(new TypeArray(TypePrimitive.bittype, new ExprConstInt(first.getNumBits() + second.getNumBits())),
					OpCode.CONCAT, first, second);
//		}
	}
	
	public NodeToSmtValue padIfNotWideEnough(NodeToSmtValue ntsvVal,
			int numBits) {
		if (ntsvVal.getNumBits() < numBits)
			return concat(CONSTBITARRAY(0, numBits - ntsvVal.getNumBits()),
				ntsvVal);
		else
			return ntsvVal;
	}
	
	@Override
	public abstractValue cast(abstractValue v1, Type targetType) {
		assert targetType != null : "cast target type can not be null";
		NodeToSmtValue ntsv1 = (NodeToSmtValue) v1;

		// bit bool int
		// bit N/A b!=bv0 concat
		// bool b?bv1:bv0 N/A b?1:0
		// int i!=0?bv1:bv0 i != 0 N/A

		// no op
		if (ntsv1.getType().equals(targetType))
			return v1;

		if (targetType == TypePrimitive.nulltype)
			return CONST(-1);

		if (ntsv1.isBit()) {
			if (targetType.equals(TypePrimitive.booltype))
				return castFromBitToBool(ntsv1);
			else if (targetType.equals(TypePrimitive.inttype))
				return castFromBitToInt(ntsv1);
			else if (BitVectUtil.isBitArray(targetType)) {
				return castFromBitToBitArray(ntsv1, targetType);
			}

		} else if (ntsv1.isBool()) {
			if (targetType.equals(TypePrimitive.bittype))
				return castFromBoolToBit(ntsv1);
			else if (targetType.equals(TypePrimitive.inttype))
				return castFromBoolToInt(ntsv1);
		} else if (ntsv1.isInt()) {
			if (targetType.equals(TypePrimitive.bittype))
				return castFromIntToBit(ntsv1);
			else if (targetType.equals(TypePrimitive.booltype))
				return castFromIntToBool(ntsv1);
			else if (BitVectUtil.isBitArray(targetType))
				return castFromIntToBitArray(targetType, ntsv1);
		}
		if (ntsv1.isBitArray()) {
			if (targetType.equals(TypePrimitive.inttype))
				return castFromBitArrayToInt(ntsv1);
			
			else if (BitVectUtil.isBitArray(targetType)) {
				return castFromBitArrayToBitArray(targetType, ntsv1);
			}
		}

		// no op
		if (ntsv1.getNumBits() == intNumBits
				&& targetType.equals(TypePrimitive.inttype))
			return ntsv1;

		throw new UnsupportedOperationException("Unexpected cast");
	}

	private abstractValue castFromBitToBool(NodeToSmtValue ntsv1) {
		if (ntsv1.isConst())
			// cast from bit to bool
			return ntsv1.getIntVal() != 0 ? CONST(true) : CONST(false);
		else
			// return b != bv0
			return eq(ntsv1, CONSTBIT(1));
	}

	private abstractValue castFromBitToInt(NodeToSmtValue ntsv1) {
		if (ntsv1.isConst())
			// cast from bit to int
			return ntsv1.getIntVal() != 0 ? CONST(1) : CONST(0);
		else {
			return concatToNewType(CONSTBITARRAY(0, intNumBits-1), ntsv1, TypePrimitive.inttype);
		}
	}

	private abstractValue castFromBitToBitArray(NodeToSmtValue ntsv1, Type targetType) {
		int bvSize = BitVectUtil.vectSize(targetType);
		if (ntsv1.isConst()) {
			return CONSTBITARRAY(ntsv1.getIntVal(), bvSize);
		} else {
			// cast bit to bit array is the same as assign the bit to 0th element
			// and then pad the rest with 0
			if (bvSize == 1)
				return ntsv1;
			else
				return BOTTOM(targetType,
					OpCode.CONCAT, CONSTBITARRAY(0, bvSize - 1), ntsv1);
		}
	}

	private abstractValue castFromBoolToBit(NodeToSmtValue ntsv1) {
		// cast from bool to bit
		if (ntsv1.isConst()) 
			return ntsv1.getBoolVal() ? CONSTBIT(1) : CONSTBIT(0);
		else {
			// return b ? bv1:bv0
			NodeToSmtValue bitVar = (NodeToSmtValue) condjoin(ntsv1, CONSTBIT(1), CONSTBIT(0));
			return bitVar;
		}
	}

	private abstractValue castFromBoolToInt(NodeToSmtValue ntsv1) {
		if (ntsv1.isConst())
			// cast from bool to int
			return ntsv1.getBoolVal() ? CONST(1) : CONST(0);
		else {
			// return b ? 1 : 0
			NodeToSmtValue intVar = (NodeToSmtValue) condjoin(ntsv1, CONST(1), CONST(0));
			return intVar;
		}
	}

	private abstractValue castFromIntToBit(NodeToSmtValue ntsv1) {
		if (ntsv1.isConst())
			// cast from int to bit
			return ntsv1.getIntVal() != 0 ? CONSTBIT(1) : CONSTBIT(0);
		else {
			// return i == 0 ? bv0 : bv1
			NodeToSmtValue bitVar = (NodeToSmtValue) condjoin(eq(ntsv1, CONST(0)), CONSTBIT(0), CONSTBIT(1));
			return bitVar;
		}
	}

	private abstractValue castFromIntToBool(NodeToSmtValue ntsv1) {
		if (ntsv1.isConst())
			// cast from int to bool
			return ntsv1.getIntVal() != 0 ? CONST(true) : CONST(false);
		else
			// return (i != 0)
			return not(eq(ntsv1, CONST(0)));
	}
	
	private abstractValue castFromIntToBitArray(Type targetType, NodeToSmtValue ntsv1) {
		int desiredWidth = BitVectUtil.vectSize(targetType);
		if (desiredWidth < ntsv1.getNumBits()) {
			ntsv1 = extract(desiredWidth-1, 0, ntsv1);
		} else if (desiredWidth > ntsv1.getNumBits()) {
			ntsv1 = padIfNotWideEnough(ntsv1, desiredWidth);
		}
		
		
		if (ntsv1.isConst())
			// cast from int to bitarray
			return CONSTBITARRAY(ntsv1.getIntVal(), desiredWidth);
		else {
			OpNode opnode = (OpNode) ntsv1;
			// return (i != 0)
			
			return BOTTOM(targetType, 
					opnode.getOpcode(), opnode.getOperands()
					);
		}
	}
	

	private abstractValue castFromBitArrayToBitArray(Type targetType,
			NodeToSmtValue ntsv1) {
		// cast from bit array to bit array. 
		// if they are of different size, pad zeros
		int desiredSize = BitVectUtil.vectSize(targetType);
		int currentSize = BitVectUtil.vectSize(ntsv1.getType());
		
		if (desiredSize < currentSize) {
			return extract(desiredSize-1 , 0, ntsv1);
		} else {
			return BOTTOM(targetType, 
					OpCode.CONCAT, CONSTBITARRAY(0, desiredSize - currentSize), ntsv1);	
		}
	}

	private abstractValue castFromBitArrayToInt(NodeToSmtValue ntsv1) {
		if (ntsv1.isConst())
			// cast from bit[] to int
			return CONST(ntsv1.getIntVal());
		else {
			// use concat
			// for i = 0 to size
			// tmpNew = new tmp var name
			// (define tmpNew)
			// tmpNew = (concat tmpPrev ntsv[i])
			// tmpPrev = tmpNew
			// return tmpNew
			int arrSize = BitVectUtil.vectSize(ntsv1.getType());
			if (arrSize < intNumBits) {
				int lengthToPad = intNumBits - arrSize;
				return concatToNewType(CONSTBITARRAY(0, lengthToPad), ntsv1, TypePrimitive.inttype);
			} else if (arrSize > intNumBits) {
				return extract(intNumBits-1, 0, ntsv1);
			} else {
				return ntsv1;
			}
		}
		
		
	}

	PrintStream ps = null;
	
	public List<NodeToSmtValue> getHashedFuncCall(NodeToSmtValue funccall) {
		List<NodeToSmtValue> ret = mFuncHash.get(funccall);
		
		if (ps == null) {
            try {
                ps = new PrintStream("/tmp/log.txt");
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
		}

//		if (ret == null) {
//			ps.print("NOT FOUND func call hash: ");
//			ps.print(funccall.getName());
//			ps.print(" ");
//			FuncNode fNode = (FuncNode) funccall;
//			for (NodeToSmtValue arg : fNode.getOperands()) {
//			    DebugPrinter dp = new DebugPrinter();
//			    arg.accept(dp);
//				ps.print(dp.sb.toString());
//				ps.print(", ");
//			}
//			ps.print(funccall);
//			ps.println();
//		}
		
		if (ret == null)
		    funccallInlined++;
		return ret;
	}

	public void putHashedFuncCall(NodeToSmtValue funccall, List<NodeToSmtValue> outletNodes) {
		mFuncHash.put(funccall, outletNodes);
	}
	
	
	
	/*
	 * Helpers
	 */
	private NodeToSmtValue mergeTwoValuesToBottom(OpCode opcode,
			NodeToSmtValue ntsv1, NodeToSmtValue ntsv2) {
	    int maxNumBits = Math.max(ntsv1.getNumBits(), ntsv2.getNumBits());
        
        if (ntsv1.getNumBits() < maxNumBits)
            ntsv1 = padIfNotWideEnough(ntsv1, maxNumBits);
        
        if (ntsv2.getNumBits() < maxNumBits)
            ntsv2 = padIfNotWideEnough(ntsv2, maxNumBits);
        
		return BOTTOM(getCommonType(ntsv1.getType(), ntsv2.getType()), 
				opcode, ntsv1, ntsv2
				);
	}
	
	private NodeToSmtValue mergeTwoValuesToBottomWithType(OpCode opcode,
			NodeToSmtValue ntsv1, NodeToSmtValue ntsv2, Type resultType) {
		int maxNumBits = Math.max(ntsv1.getNumBits(), ntsv2.getNumBits());
		
		if (ntsv1.getNumBits() < maxNumBits)
			ntsv1 = padIfNotWideEnough(ntsv1, maxNumBits);
		
		if (ntsv2.getNumBits() < maxNumBits)
			ntsv2 = padIfNotWideEnough(ntsv2, maxNumBits);
		return BOTTOM(resultType, 
				opcode, ntsv1, ntsv2
				);
	}
	
	protected Type getCommonType(Type type1, Type type2) {
		
		// TODO consider incorporating this with SmtType
		// special handling for converting bit[] to int
		
		if (promotesTo(type1, type2))
			return type2;
		
		if (promotesTo(type2, type1))
			return type1;
		
		return null;
	}
	
	protected boolean promotesTo(Type ths, Type that) {
		if (BitVectUtil.isBitArray(ths)) {
			int m = BitVectUtil.vectSize(ths);
			// bit[m] can promote to int if m <= width(int)
			if (that.equals(TypePrimitive.inttype)) {
				return m <= getNumBitsForType(that);
			}
			
		} else if (ths.equals(TypePrimitive.inttype)) {
			
			// int can promote to bit[N] if width(int) <= n
			if (BitVectUtil.isBitArray(that)) {
				int n = BitVectUtil.vectSize(that);
				return getNumBitsForType(TypePrimitive.inttype) <= n;
			}
		}
		
		return ths.promotesTo(that);
	}
	
	public int getNumBitsForType(Type t) {
		if (t.equals(TypePrimitive.inttype))
			return intNumBits;
		else if (t.equals(TypePrimitive.bittype)
				|| t.equals(TypePrimitive.booltype))
			return 1;
		else if (BitVectUtil.isBitArray(t))
			return BitVectUtil.vectSize(t);
		else if (t instanceof TypeArray)
			return -1;
		else 
			return -1;
	}
	
	
	// Static methods
	protected static NodeToSmtValue bit0 = NodeToSmtValue.newBit(0);
	protected static NodeToSmtValue bool0 = NodeToSmtValue.newBool(false);
	
	/**
	 * Returns the default value of a certain type.
	 * @param t
	 * @return the default value of type t.
	 *         null if t is TypeArray
	 */
	public static NodeToSmtValue defaultValue(SmtType t) {
		if (t.getRealType().equals(TypePrimitive.inttype))
			return NodeToSmtValue.newInt(0, t.getNumBits());
		else if (t.getRealType().equals(TypePrimitive.bittype))
			return bit0;
		else if (t.getRealType().equals(TypePrimitive.booltype))
			return bool0;
		else if (BitVectUtil.isBitArray(t.getRealType())) {
			TypeArray ta = (TypeArray) t.getRealType();
			int size = BitVectUtil.vectSize(ta);
			NodeToSmtValue bitArr = NodeToSmtValue.newBitArray(0, size);
			return bitArr;
		} else if (t.getRealType() instanceof TypeArray) {
			return null;	
		} else {
			throw new IllegalStateException("unexpected starType");
		}
	}
	
	

	SMTTranslator mTrans;
	
	StaticHoleTracker mHoleNamer;
	HashSet<VarNode> mLocalVars;
	LinkedList<NodeToSmtValue> mEq;
	HashMap<NodeToSmtValue, NodeToSmtValue> mSimpleDefs;
	HashMap<NodeToSmtValue, Integer> mUses;
	LinkedList<NodeToSmtValue> mAsserts;
	HashSet<VarNode> mHoles;
	HashSet<VarNode> mInputs;
	HashSet<VarNode> mArraySeeds;
	
	HashMap<NodeToSmtValue, NodeToSmtValue> mCache;
	
	/**
	 * Function Call Hashing
	 * Maps from a FuncNode (which represents a calling context)
	 * to a list of NodeToSmtValue (which are the outputs of that
	 * call
	 */
    Map<NodeToSmtValue,List<NodeToSmtValue>> mFuncHash;
	
	/**
	 * mStructHash is a map that maps from a OpNode to VarNode
	 * Use it to find a simple representation of a given subtree
	 */
	HashMap<NodeToSmtValue, NodeToSmtValue> mStructHash;
	
	NodeToSmtValue mSharedFalseValue;
	
	
	String suffix = "";
	
	public String getSuffix() {
		return suffix;
	}
	
	public void setSuffix(String suffix) {
		this.suffix = suffix;
	}
	
	public boolean isHole(VarNode varNode) {
	    return mHoles.contains(varNode);
	}
	
	public boolean isInput(VarNode varNode) {
        return mInputs.contains(varNode);
    }
	
	public boolean isArraySeed(VarNode varNode) {
	    return mArraySeeds.contains(varNode);
	}
	
	public boolean isLocalVar(VarNode varNode) {
	    return mLocalVars.contains(varNode);
	}
	
	int savedNodes = 0;
	protected NodeToSmtValue checkCache(NodeToSmtValue node) {
		
//	    if (node instanceof OpNode)
//	        return node;
	    
		NodeToSmtValue inCache = mCache.get(node);
		if (inCache == null) {
			inCache = node;
			mCache.put(node, node);	
		} else {
			savedNodes++;
//			if (mCache.size() %10000 == 0)
//				System.out.println(mCache.size());
		}
		
		return inCache;
	}

	/**
	 * Add an assert to this formula
	 * @param predicate
	 */
	public void addAssert(NodeToSmtValue predicate) {
		mAsserts.add(referenceVar(predicate));
	}
	
	public void addDefinition(VarNode dest, NodeToSmtValue def) {
	    addDefinition(dest, def, false);
	}
	
	public void addConstraint(VarNode dest, NodeToSmtValue def) {
	    addDefinition(dest, def, true);
	}
	
	protected void addDefinition(VarNode dest, NodeToSmtValue def, boolean toFront) {
		// even if def is CONST true, we still want to assert it
		// because that will leave a record that dest is defined instead of
		// being treated as undefined variables in the future
		String varName = dest.getRHSName();
		if (def instanceof OpNode)
		    def = referenceVar(def);
		else if (def instanceof VarNode) {
		    mTransitiveSet.union((VarNode) def, dest);
		}
		dest = (VarNode) referenceVar(dest);
		

		
		log.finer("defining " + varName);
		
		if (toFront)
		    mEq.addFirst(dest);
		else
		    mEq.addLast(dest);
		
		mSimpleDefs.put(dest, def);
		if (!mStructHash.containsKey(def))
		    mStructHash.put(def, dest);
	}
	
	public void addTempDefinition(NodeToSmtValue def) {
	    String varName = tmpVarGen.nextVar("tn");
	    VarNode dest = STATE_DEFAULT(varName, def.getType(), def.getNumBits(), 0);
	    addDefinition(dest, def);
	}
	
	public NodeToSmtValue getDefinition(VarNode varDefined) {
        if (mSimpleDefs.containsKey(varDefined))
            return mSimpleDefs.get(varDefined);
        return null;
    }
	
	public NodeToSmtValue checkStructuralHash(NodeToSmtValue newNode) {
        // whenever we create a new OpNode, check structural hashing first
        if (USE_STRUCT_HASHING && mStructHash.containsKey(newNode)) {
            // if that structure already has a name var, use that
            NodeToSmtValue varNode = mStructHash.get(newNode);
            structHashingUsed++;
            return varNode;
        } else {
            // if that structure has not been assigned to any var, check cache.
            NodeToSmtValue inCache = checkCache(newNode);
            return inCache;
        }
    }
	
	public void addComment(String c) {
		mEq.add(LABEL(TypePrimitive.voidtype, 0, c));
	}

	
	public void declareLocalVar(VarNode node) {
//	    if (!isVarDeclared(node)) {
            String varName = node.getRHSName();
            log.finer("declaring local " + varName);
            VarNode nodeToStore = node;
            nodeToStore.setSuffixSetter(this);
            
            // renamable variables
            mLocalVars.add(nodeToStore);
//        }
	}
	
	/**
	 * Add the node to the list of holes without any checking
	 * @param hole
	 */
	public void declareHole(VarNode hole) {
	    log.finer("declaring fixed name hole " + hole.getRHSName());
		mHoles.add(hole);
	}
	
	/**
	 * Add the node to the list of input without any checking
	 * @param input
	 */
	public void declareInput(VarNode input) {
	    log.finer("declaring renamable input " + input.getRHSName());
		mInputs.add(input);
		input.setSuffixSetter(this);
	}
	
	/**
	 * Add the node to the list of varialbes that are real
	 * variables
	 * 
	 * It currently stores the seed array variable
	 * @param var
	 */
	public void declareArraySeedVariable(VarNode var) {
	    mArraySeeds.add(var);
	}
	
	public NodeToSmtValue newHole(ExprStar star, Type t) {
		
		String cvar = mHoleNamer.getName(star);
		VarNode newNode = NodeToSmtValue.newHole(cvar, t,
				// ExprStar.getSize() is incorrect for bit[], special case that
				BitVectUtil.isBitArray(t) ? getNumBitsForType(t) : star.getSize());
		
		newNode = (VarNode) checkCache(newNode);
		
		if (!mHoles.contains(newNode)) {
			declareHole(newNode);
		}
		return newNode;		
	}
	
	public NodeToSmtValue newParam(Parameter param, int i, Type paramType) {
		String varName = param.getName() + "_idx" + i;
		
		VarNode paramVal = NodeToSmtValue.newParam(varName, paramType, 
				paramType.equals(TypePrimitive.inttype) ? mInBits : getNumBitsForType(paramType));
		declareInput(paramVal);
		return paramVal;
	}
	
	public NodeToSmtValue newParam(Parameter param, Type paramType) {
		String varName = param.getName();
		
		VarNode paramVal = NodeToSmtValue.newParam(varName, paramType, 
				paramType.equals(TypePrimitive.inttype) ? mInBits : getNumBitsForType(paramType));
		declareInput(paramVal);
		return paramVal;
	}
	
	/**
	 * Reference the specified variable. (use count will be incremented)
	 * Find the proxy node for the original node. If there is no proxy 
	 * node declared for it, the original is returned.
	 * 
	 * @param original
	 * @return
	 */
	protected NodeToSmtValue referenceVar(NodeToSmtValue original) {

		if (original instanceof OpNode) {
		    return original;
		    
		} else if (original instanceof ConstNode ||
				original instanceof LabelNode) {
			return original;
			
		} else if (original instanceof VarNode) {

			NodeToSmtValue toUse;
			if (FLAT) {
				// use def instead of original
				toUse = findOriginalDef((VarNode) original);
			} else {
				// use original
				toUse = original;
			}
			
			Integer uses = mUses.get(toUse);
			if (uses == null) {
				uses = 0;
			}
			uses++;
			mUses.put(toUse, uses);

			return toUse;
		} else {
		    return original;
		}
	}
	
//	protected NodeToSmtValue referenceRHS(NodeToSmtValue original) {
//	    NodeToSmtValue referenced = referenceLHS(original);
////	    return referenced;
//	    if (CANONICALIZE && isLinearizable(referenced))
//	        return LINEAR(referenced);
//	    else
//	        return referenced;
//	}
	
	
	
	public void finalize() {
//		guardModAndDivide();
	}
	
	public void optimize() {
	    int numRemoved = 0;
	    
	    log.info("Optimizing DAG");
		simplifyExpressionTrees();
		constraintUndefinedVariables();
//		numRemoved = removeUnusedVariables();
		
		log.info(" - Saved Node creation: " + savedNodes);
		log.info(" - Removed " + numRemoved + " unused variables");
		log.info(" - Structural Hashing Used: " + structHashingUsed + " (size = " + mStructHash.size() +")");
		log.info(" - Func Call Inlined: " + funccallInlined);
		log.info(" - Struct Simplified: " + structSimplified);
//		Toolbox.pause();
	}
	
	
	public boolean isVarDefined(NodeToSmtValue var) {
		return mSimpleDefs.containsKey(var);
	}
	
	public boolean isInputVariable(String varName) {
		for (VarNode inputVar : mInputs) {
			if (inputVar.getRHSName().equals(varName))
				return true;
		}
		return false;
//		return mInputs.containsKey(varName);
	}
	
	public boolean isHoleVariable(String varName) {
		for (VarNode holeVar : mHoles) {
			if (holeVar.getRHSName().equals(varName))
				return true;
		}
		return false;
//		return mHoles.containsKey(varName);
	}
	
	/*
	 * Private methods
	 */
	
	/**
	 * This method finds any OpNode that satisfy the following forms:
	 * 
	 * c = a / b; or c = a % b;
	 * 
	 * and replace it with:
	 * 
	 * if (b != 0) then (c = a / b) else false;
	 * 
	 * Notice the last false is not permanent. For synthesis, it's false.
	 * For verification, it has to be true;
	 * 
	 */
//	protected void guardModAndDivide() {
//		HashSet<String> simpleDefsToRemove = new HashSet<String>();
//		for (String varName : mSimpleDefs.keySet()) {
//			NodeToSmtValue def = mSimpleDefs.get(varName);
//			FindSecondOperands visitor = new FindSecondOperands();
//			visitor.visit(def);
//			if (visitor.found()) {
//				List<NodeToSmtValue> container = visitor.getContainer();
//				
//				NodeToSmtValue[] conds = new NodeToSmtValue[container.size()];
//				int i = 0;
//				for (NodeToSmtValue toGuard : container) {
//					conds[i] = not(eq(toGuard, CONST(0)));
//					i++;
//				}
//				NodeToSmtValue disjunction = and(conds);
//				NodeToSmtValue ite = condjoin(disjunction, def, mSharedFalseValue);
//				mSimpleDefs.put(varName, ite);
//			
//			}
//		}
//		
//		for (String varName : simpleDefsToRemove) {
//			mSimpleDefs.remove(varName);
//		}
//		
//	}
	
//	private static class FindSecondOperands extends DAGVisitor {
//		List<NodeToSmtValue> secondOperands;
//		
//		protected List<NodeToSmtValue> getContainer() {
//			if (secondOperands == null)
//				secondOperands = new LinkedList<NodeToSmtValue>();
//			return secondOperands;
//		}
//		
//		public boolean found() {
//			return secondOperands != null;
//		}
//
//		@Override
//		void over(OpNode arg0) {
//			getContainer().add(arg0.operands[1]);
//			super.over(arg0);
//		}
//		
//		@Override
//		void mod(OpNode arg0) {
//			getContainer().add(arg0.operands[1]);
//			super.mod(arg0);
//		}
//	}
	
	protected void simplifyExpressionTrees() {
	    if (!FLAT) return;
	    
	    // simplfy the assignments
	    NodeReplacer nr = new NodeReplacer();
	    for (NodeToSmtValue var : mEq) {
	        NodeToSmtValue def = mSimpleDefs.get(var);
	        
	        if (def instanceof OpNode) {
	            NodeToSmtValue newDef = (NodeToSmtValue) nr.replaceChildren((OpNode) def);
	            mSimpleDefs.put(var, newDef);
	        }
	    }
	    
	    // simplify the asserts
	    LinkedList<NodeToSmtValue> oldAsserts = mAsserts;
	    mAsserts = new LinkedList<NodeToSmtValue>(); 
	    for (NodeToSmtValue predicate : oldAsserts) {
	        if (predicate instanceof OpNode) {
                NodeToSmtValue newPredicate = (NodeToSmtValue) nr.replaceChildren((OpNode) predicate);
                mAsserts.add(newPredicate);
            }
	    }
	}

	
	protected void constraintUndefinedVariables() {
		NodeToSmtValue constant = CONST(-1);
		
		for (VarNode var : mLocalVars) {
			if (mUses.containsKey(var) && 
					!isVarDefined(var) &&
					!mInputs.contains(var)) {
				String varName = var.getRHSName();
				// if a variable is used but never defined and is not one of the inputs. 
				// we give it a constant
				log.finer("Removing declared but undefined " + varName);

				NodeToSmtValue defaultVal = defaultValue(var.getSmtType());
				if (defaultVal != null)
				    addConstraint(var, defaultVal);
			}
		}
	}
	
	
	
	/**
	 * Follow the definition of src until it reaches
	 * src = def;
	 * 
	 * 1) def is an undefined VarNode (they are the input params)
	 * 2) def is not longer a VarNode
	 * 
	 * @param src
	 * @return
	 */
	protected NodeToSmtValue findOriginalDef(VarNode src) {
	    NodeToSmtValue def = getDefinition(src);
	    
	    if (def == null)
	        return src;
	    
	    if (def instanceof VarNode)
	        return findOriginalDef((VarNode) def);
	    else
	        return def;
	}

	
}
