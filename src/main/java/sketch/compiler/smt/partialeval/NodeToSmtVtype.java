package sketch.compiler.smt.partialeval;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

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
import sketch.compiler.smt.stp.STPTranslator;
import sketch.compiler.solvers.constructs.AbstractValueOracle;
import sketch.compiler.solvers.constructs.StaticHoleTracker;

public abstract class NodeToSmtVtype extends TypedVtype implements ISuffixSetter {
	
	public class DebugPrinter extends STPTranslator {

		public DebugPrinter(int intNumBits) {
			super(intNumBits);
		}
		
		@Override
		public String getStr(NodeToSmtValue ntsv) {
			if (ntsv instanceof VarNode) {
				NodeToSmtValue def = mSimpleDefs.get(ntsv);
				if (def == null) {
					// undefined var
					return super.getStr(ntsv);
				} else {
					return getStr(def);
				}
			}
			return super.getStr(ntsv);
		}
		
	}
	public class FormulaPrinter {

		boolean mIsSynthesis;
		PrintStream out;
		
		public FormulaPrinter(PrintStream out, boolean isSynthesis) {
			
			mIsSynthesis = isSynthesis;
			this.out = out;
			
		}
		
		public void printRenamableDeclarations() {
			addComment("");
			addComment("Renamable Variable Declarations");
			addComment("");
			
			for (VarNode p : mRenamableVars) {				
				out.println(mTrans.getDefineVar(p.getSmtType(), p.getRHSName()));
			}
		}
		
		public void printFixedNameDeclarations() {
			addComment("");
			addComment("Fixed Name Variable Declarations");
			addComment("");

			for (VarNode p : mFixedNameVars) {				
				out.println(mTrans.getDefineVar(p.getSmtType(), p.getRHSName()));
			}
		}
		
		public void printDAG() {
			
			addComment("");
			addComment("DAG");
			addComment("");
			
			for (NodeToSmtValue eq : mEq) {
				if (eq instanceof LabelNode) {
					addComment(eq.toString());
				} else {
					String defStr = mTrans.getAssert(mTrans.getStr(eq));
					out.println(defStr);
				}
			}
		
		}
		
		/**
		 * Helper that generates the correctness condition for the entire program
		 * at the end.
		 */
		protected void generateCorrectnessConditions(boolean negated) {
			// print out the correctness conditions
			addComment("");
			addComment("Correctness Conditions");
			addComment("");

			if (!negated)
				// output a disjunction of all asserts
				for (NodeToSmtValue predicate : mAsserts) {
					out.println(mTrans.getAssert(mTrans.getStr(predicate)));
				}

			else {
				if (mAsserts.size() > 0) {

					NodeToSmtValue[] negatedAsserts = new NodeToSmtValue[mAsserts.size()];
					
					int i = 0;
					for (NodeToSmtValue oneAssert : mAsserts) {
						negatedAsserts[i] = not(oneAssert);
						i++;
					}
					
					out.println(mTrans.getAssert(mTrans.getStr(or(negatedAsserts))));
				} else {
					// if there is no assertion in a verification phase, that means the program
					// can never fail, in which case, verification can fail immediately

					out.println(mTrans.getAssert(mTrans.getStr(CONST(false))));
				}
			}
		}
		
		public void printSynthesisFormula(ArrayList<SmtValueOracle> observations) {
			
			try {
				
				out.println(mTrans.prolog());
//				println(mTrans.getComment("intbits=" + mIntBits + " cbits=" + mcbits + " inbits=" + minbits));
				
				printFixedNameDeclarations();
				
				for (int currObserIdx = 0; currObserIdx < observations.size(); currObserIdx++) {
					String[] comment = {"Formula for observation " + currObserIdx };
					
					setSuffix("");
					HashMap<NodeToSmtValue, NodeToSmtValue> valueAssignments = useOracle(observations.get(currObserIdx), true);
					setSuffix("s" + currObserIdx);
					
					addBlockComment(comment);
					
					printRenamableDeclarations();
					printValueAssignments(valueAssignments);
					printDAG();
					generateCorrectnessConditions(false);
					
				}
				
				out.println(mTrans.epilog());
				
			} catch (AssertionFailedException e) {
				throw e;
			} catch (ArrayIndexOutOfBoundsException e) {
				throw e;
			} finally {
				
			}
		}
		
		public void printVerificaitonFormula(SmtValueOracle candidate) {
		
			String[] comment = {"Verification formulas"};
			try {
				
				HashMap<NodeToSmtValue, NodeToSmtValue> valueAssignments = useOracle(candidate, false);
				
				
				setSuffix("");
				out.println(mTrans.prolog());
				printFixedNameDeclarations();
//				println(mTrans.getComment("intbits=" + mIntBits + " cbits=" + mcbits + " inbits=" + minbits));
				
				addBlockComment(comment);
				
				printRenamableDeclarations();
				printValueAssignments(valueAssignments);
				printDAG();
				generateCorrectnessConditions(true);
				out.println(mTrans.epilog());
				
		
			} catch (AssertionFailedException e) {
				throw e;
			} catch (ArrayIndexOutOfBoundsException e) {
				throw e;
			} finally {

			}
		
		}
		
		public HashMap<NodeToSmtValue, NodeToSmtValue> useOracle(SmtValueOracle oracle, boolean isSynthesis) {
			
			Collection<VarNode> inlets;
			if (isSynthesis) {
//				mSharedFalseValue.obj = 0;
				inlets = mInputs;
			} else {
//				mSharedFalseValue.obj = 1;
				inlets = mHoles;
			}
			
			HashMap<NodeToSmtValue, NodeToSmtValue> valueAssignments = new HashMap<NodeToSmtValue, NodeToSmtValue>();
			
			
			for (VarNode var : inlets) {
				// CAUTION, for synthesis, oracle is from verification, no suffix
				// the way to solve this problem is to retrieve the values from the oracles first.
				
				// for verification, oracle is from synthesis, has suffix, but we
				// only care about the hole variables, which don't have suffix, so
				// it's ok.
				NodeToSmtValue rhs = oracle.getValueForVariable(var);
				
				NodeToSmtValue lhs = var;
				valueAssignments.put(lhs, rhs);
			}
			
			return valueAssignments;
		}
		
		public void printValueAssignments(HashMap<NodeToSmtValue, NodeToSmtValue> valueAssignments) {
			addComment("");
			addComment("Input/Hole assignments");
			addComment("");
			for (NodeToSmtValue lhs :  valueAssignments.keySet()) {
				NodeToSmtValue rhs = valueAssignments.get(lhs);
				out.println(mTrans.getAssert(mTrans.getStr(eq(lhs, rhs))));
			}
		}
		
		/**
		 * Add comment to the formula file
		 * @param msg
		 */
		public void addComment(String msg) {
			out.println(mTrans.getComment(msg));
		}	
	}
	
	public static final boolean NO_INTERMEDIATE = true;
	
	protected TempVarGen tmpVarGen;
	protected AbstractValueOracle oracle;
	
	protected int intNumBits;
	protected int mInBits;
	protected int mCBits;
	
	public static final boolean USE_STRUCT_HASHING = true;
	private static Logger log = Logger.getLogger(NodeToSmtVtype.class.getCanonicalName());

	
	/*
	 * Getters & Setters
	 */
	
	public int getIntNumBits() { return intNumBits; }
	
	public StaticHoleTracker getHoleNamer() { return mHoleNamer; }
	
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
			SMTTranslator smtTran,
			int intNumBits,
			int inBits,
			int cBits,
			TempVarGen tmpVarGen) {
		super(smtTran);
		
		this.mTrans = smtTran;
		this.intNumBits = intNumBits;
		mCBits = cBits;
		mInBits = inBits;
		this.tmpVarGen = tmpVarGen;
		
		mRenamableVars = new HashSet<VarNode>();
		mFixedNameVars = new HashSet<VarNode>();
		mEq = new LinkedList<NodeToSmtValue>();
		mCache = new HashMap<NodeToSmtValue, NodeToSmtValue>(100000);
		mStructHash = new HashMap<NodeToSmtValue, NodeToSmtValue>(100000);
		mFuncHash = new HashMap<NodeToSmtValue, List<NodeToSmtValue>>();
		mSimpleDefs = new HashMap<NodeToSmtValue, NodeToSmtValue>();
		mUses = new HashMap<NodeToSmtValue, Integer>();
		mAsserts = new LinkedList<NodeToSmtValue>();
		mHoles = new HashSet<VarNode>();
		mInputs = new HashSet<VarNode>();
		
		
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
		declareRenamableVar(newNode);
		return newNode;
	}
	
	public VarNode STATE_ELE_DEFAULT(String label, Type realType, int numBits, int rhsIdx) {
		VarNode newNode = NodeToSmtValue.newStateArrayEleDefault(label, realType, numBits, rhsIdx);
		
		newNode = (VarNode) checkCache(newNode);
		declareRenamableVar(newNode);
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
		
		// whenever we create a new OpNode, check structural hashing first
		if (USE_STRUCT_HASHING && mStructHash.containsKey(newNode)) {
			// if that structure already has a name var, use that
			NodeToSmtValue varNode = mStructHash.get(newNode);
			return varNode;
		} else {
			// if that structure has not been assigned to any var, check cache.
			
			NodeToSmtValue inCache = checkCache(newNode);
			
			return inCache;
		}
		
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
		int size = BitVectUtil.vectSize(ntsvArr.getType());
		
		if (!ntsvIdx.isConst() || !ntsvLen.isConst()) {
			// index is bottom or length is bottom
			return rawArracc(ntsvArr, ntsvIdx, ntsvLen);
			
		} else {
			// constant index and length
			
			int iidx = idx.getIntVal() ;
			size = ntsvArr.isBitArray() ? BitVectUtil.vectSize(ntsvArr.getType()) : arr.getVectValue().size();
			if( !isUnchecked && (iidx < 0 || iidx >= size)  )
				throw new ArrayIndexOutOfBoundsException("ARRAY OUT OF BOUNDS !(0<=" + iidx + " < " + size+") ");
			
			if(iidx < 0 || iidx >= size)
				// index out of bound
				return outOfBounds((NodeToSmtValue) arr);
			
			if (BitVectUtil.isBitArray(ntsvArr.getType())) {
				// if it's bit aray
				return extract(idx.getIntVal() + len.getIntVal() - 1, idx.getIntVal(), ntsvArr);

			} else {
				// if it's normal array
				
				if(len != null){
					assert len.hasIntVal() : "NYI";
					int ilen = len.getIntVal();
					if(ilen != 1){
						List<abstractValue> lst = new ArrayList<abstractValue>(ilen);
						for(int i=0; i<ilen; ++i){
							lst.add(  arracc(arr, plus(idx, CONST(i)), null, isUnchecked)  );
						}
						return ARR( lst );
					}
				}
				return arr.getVectValue().get(idx.getIntVal());
			}
		}
	}
	
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
			// if v1 is var v2 is const, add v2.const to v1
			
			// if v2 is var v1 is const, add v1.const to v2
			
			// otherwise, return a node with (v1 + v2) + v1.const + v2.const)
			return mergeTwoValuesToBottom(OpCode.PLUS, ntsv1, ntsv2);
		}
	}

	@Override
	public NodeToSmtValue minus(abstractValue v1, abstractValue v2) {
		NodeToSmtValue ntsv1 = (NodeToSmtValue) v1;
		NodeToSmtValue ntsv2 = (NodeToSmtValue) v2;

		if (v1.hasIntVal() && v2.hasIntVal()) {
			return CONST(v1.getIntVal() - v2.getIntVal());
		} else {
			return mergeTwoValuesToBottom(OpCode.MINUS, ntsv1, ntsv2);
		}
	}

	@Override
	public NodeToSmtValue times(abstractValue v1, abstractValue v2) {
		NodeToSmtValue ntsv1 = (NodeToSmtValue) v1;
		NodeToSmtValue ntsv2 = (NodeToSmtValue) v2;

		if (v1.hasIntVal() && v2.hasIntVal()) {
			return CONST(v1.getIntVal() * v2.getIntVal());
		} else {
			return mergeTwoValuesToBottom(OpCode.TIMES, ntsv1, ntsv2);
		}
	}

	@Override
	public NodeToSmtValue over(abstractValue v1, abstractValue v2) {
		NodeToSmtValue ntsv1 = (NodeToSmtValue) v1;
		NodeToSmtValue ntsv2 = (NodeToSmtValue) v2;

//		if (v2.hasIntVal() && v2.getIntVal() == 0)
		state.Assert(not(eq(v2, CONST(0))), "divide by zero", false);
		
		if (v2.hasIntVal() && v2.getIntVal() == 1)
			return ntsv1;
		
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
	
	Map<NodeToSmtValue,List<NodeToSmtValue>> mFuncHash;
	
	public List<NodeToSmtValue> getHashedFuncCall(NodeToSmtValue funccall) {
		List<NodeToSmtValue> ret = mFuncHash.get(funccall);
//		if (ret == null) {
//			DebugPrinter dp = new DebugPrinter(intNumBits);
//			System.err.print("NOT FOUND func call hash: ");
//			System.err.print(funccall.getName());
//			System.err.print(" ");
//			FuncNode fNode = (FuncNode) funccall;
//			for (NodeToSmtValue arg : fNode.getOperands()) {
//				System.err.print(dp.getStr(arg));
//				System.err.print(", ");
//			}
//			System.err.print(funccall);
//			System.err.println();
//		}
//		
//		if (ret != null)
//			System.err.println("Found func call: " + funccall);
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
	
	public static NodeToSmtValue defaultValue(SmtType t) {
		if (t.getRealType().equals(TypePrimitive.inttype))
			return NodeToSmtValue.newInt(0, t.getNumBits());
		else if (t.getRealType().equals(TypePrimitive.bittype))
			return bit0;
		else if (t.getRealType().equals(TypePrimitive.booltype))
			return bool0;
		else if (BitVectUtil.isBitArray(t.getRealType())) {
			TypeArray ta = (TypeArray) t.getRealType();
			int size = ((ExprConstInt) ta.getLength()).getVal();
			NodeToSmtValue bitArr = NodeToSmtValue.newBitArray(0, size);
			return bitArr;
		} else if (t.getRealType() instanceof TypeArray) {
			TypeArray starType2 = (TypeArray) t.getRealType();
//			ExprConstInt size = (ExprConstInt) starType2.getLength();
			throw new IllegalStateException("Not implemented - SmtValueOracle.defaultValue()");
//			return NodeToSmtValue.newListOf(defaultValue(starType2.getBase()), size.getVal());	
		} else {
			throw new IllegalStateException("unexpected starType");
		}
	}
	
	

	SMTTranslator mTrans;
	
	StaticHoleTracker mHoleNamer;
	HashSet<VarNode> mRenamableVars;
	HashSet<VarNode> mFixedNameVars;
	List<NodeToSmtValue> mEq;
	HashMap<NodeToSmtValue, NodeToSmtValue> mSimpleDefs;
	HashMap<NodeToSmtValue, Integer> mUses;
	LinkedList<NodeToSmtValue> mAsserts;
	HashSet<VarNode> mHoles;
	HashSet<VarNode> mInputs;
	HashMap<NodeToSmtValue, NodeToSmtValue> mCache;
	
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
	
	int savedNodes = 0;
	protected NodeToSmtValue checkCache(NodeToSmtValue node) {
		// OpNode is not cached because OpNode
		// may represent a big tree of expression, which causes
		// the recursive hashCode() and equals() calls to take
		// a long time. It's ok to waste a bit of memory here.
		if (node instanceof OpNode) {
			return node;
		}
		
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
		// even if def is CONST true, we still want to assert it
		// because that will leave a record that dest is defined instead of
		// being treated as undefined variables in the future
		String varName = dest.getRHSName();
		def = referenceVar(def);
		dest = (VarNode) referenceVar(dest);
		
		assert isVarDeclared(dest) : varName + " is defined but not declared";
		
		log.finer("defining " + varName);
		
		mEq.add(eq(dest, def));
		mSimpleDefs.put(dest, def);
		mStructHash.put(def, dest);
	}
	
	public void addComment(String c) {
		mEq.add(LABEL(TypePrimitive.voidtype, 0, c));
	}
	
	/**
	 * Add block comment
	 * @param msgs
	 */
	public void addBlockComment(String[] msgs) {
		mEq.add(LABEL(TypePrimitive.voidtype, 0, mTrans.getBlockComment(msgs)));
	}
	
	/**
	 * Define a variable whose name will not be changed. The variables
	 * whose name don't change are always holes.
	 * 
	 * @param node
	 * 
	 */
	public void declareFixedNameVar(VarNode node) {
		
		if (!isVarDeclared(node)) {
			String varName = node.getRHSName();
			log.finer("declaring fixed name " + varName);
			VarNode nodeToStore = null;
			
			// variables should not be renamed
			nodeToStore = node;
			mFixedNameVars.add(nodeToStore);
			
		}
	}
	
	/**
	 * Declare a variable whose name depends on the observation 
	 * suffix. These include all normal variables and input variables.
	 * 
	 * @param node
	 */
	public void declareRenamableVar(VarNode node) {
	
		if (!isVarDeclared(node)) {
			String varName = node.getRHSName();
			log.finer("declaring renamambe " + varName);
			VarNode nodeToStore = node;
			nodeToStore.setSuffixSetter(this);
			
			// renamable variables
//			nodeToStore = new ProxyNode(node, this);
			mRenamableVars.add(nodeToStore);
			
		}
	}
	
	/**
	 * Add the node to the list of holes without any checking
	 * @param hole
	 */
	public void declareHole(VarNode hole) {
		mHoles.add(hole);
		declareFixedNameVar(hole);
	}
	
	/**
	 * Add the node to thelist of input without any checking
	 * @param input
	 */
	public void declareInput(VarNode input) {
		mInputs.add(input);
		declareRenamableVar(input);
	}
	
	
//	public NodeToSmtValue newHole(ExprStar star, Type t) {
//		
//		String cvar = mHoleNamer.getName(star);
//	
//		if (!isHoleVariable(cvar)) {
//			
//			VarNode holeValue = NodeToSmtValue.newHole(cvar, t,
//					// ExprStar.getSize() is incorrect for bit[], special case that
//					BitVectUtil.isBitArray(t) ? getNumBitsForType(t) : star.getSize());
//			declareHole(holeValue);
//			return holeValue;
//		} else {
//			return getVarNode(cvar);
//		}
//	}
	
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

		if (original instanceof OpNode || 
				original instanceof ConstNode ||
				original instanceof LabelNode) {
			return original;
		} else {

			NodeToSmtValue def = mSimpleDefs.get(original);
			NodeToSmtValue toUse;
			if (NO_INTERMEDIATE && def instanceof VarNode) {
				// use def instead of original
				toUse = def;
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
		}
	}
	
	/**
	 * Look up the NodeToSmtValue object in all the declared variables 
	 * 
	 * @param varName
	 * @return
	 */
	public NodeToSmtValue getVarNode(String varName) {
		
		for (VarNode renamable : mRenamableVars)
			if (renamable.getRHSName().equals(varName))
				return renamable;
		
		for (VarNode fixed : mFixedNameVars)
			if (fixed.getRHSName().equals(varName))
				return fixed;
		
//		if (mRenamableVars.containsKey(varName))
//			return mRenamableVars.get(varName);
//		
//		if (mFixedNameVars.containsKey(varName))
//			return mFixedNameVars.get(varName);
		
		return null;
	}
	
	public SmtType getTypeForVariable(String varName) {
		return getVarNode(varName).getSmtType();
	}
	
	public void finalize() {
//		guardModAndDivide();
	}
	
	public void optimize() {
		log.fine("Optimizing DAG");
		constraintUndefinedVariables();
		int numRemoved = removeUnusedVariables();
		
		log.fine(" - Saved Node creation: " + savedNodes);
		log.fine(" - Removed " + numRemoved + " unused variables"); 
		
//		Toolbox.pause();
	}
	
	public boolean isVarDeclared(NodeToSmtValue var) {
		return mRenamableVars.contains(var) || 
			mFixedNameVars.contains(var);
//		return mRenamableVars.containsKey(varName) || 
//			mFixedNameVars.containsKey(varName);
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
	
	protected void constraintUndefinedVariables() {
		NodeToSmtValue constant = CONST(-1);
		for (VarNode var : mRenamableVars) {
			if (mUses.containsKey(var) && 
					!isVarDefined(var) &&
					!mInputs.contains(var)) {
				String varName = var.getRHSName();
				// if a variable is used but never defined and is not one of the inputs. 
				// we give it a constant
				log.finer("Removing declared but undefined " + varName);

				addDefinition(var, CONST(0));
			}
		}
	}
	
	protected int removeUnusedVariables() {
		Set<NodeToSmtValue> setToRemove = new HashSet<NodeToSmtValue>();
		int numRemoved = 0;
		
		log.fine("Before cache size:" + mCache.size());
		
		for (VarNode var : mRenamableVars) {
			
			// if a renamable variable is not an input and not used
			// do not print the declaration
			
			if (!mInputs.contains(var) && 
					!mUses.containsKey(var)) {
				String varName = var.getRHSName();
				log.finer("Removing unused var " + varName);
				setToRemove.add(var);
			}
		}
		
		for (NodeToSmtValue var : setToRemove) {
			mRenamableVars.remove(var);
			mCache.remove(var);
		}
		
		numRemoved = setToRemove.size();
		setToRemove.clear();
		
		for (NodeToSmtValue var : mFixedNameVars) {
			
			// if a renamable variable is not an input and not used
			// do not print the declaration
			if (!mHoles.contains(var) && 
					!mUses.containsKey(var)) {
				setToRemove.add(var);
			}
		}
		
		for (NodeToSmtValue var : setToRemove) {
			mFixedNameVars.remove(var);
			mCache.remove(var);	
		}
		log.fine("After cache size:" + mCache.size());
		
		numRemoved += setToRemove.size();
		return numRemoved;
	}

	
}
