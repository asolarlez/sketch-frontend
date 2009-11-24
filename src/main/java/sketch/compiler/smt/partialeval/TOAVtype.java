package sketch.compiler.smt.partialeval;

import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.dataflow.MethodState;
import sketch.compiler.dataflow.varState;
import sketch.compiler.smt.SMTTranslator.OpCode;

/**
 * Emit formula to use Theory Of Array
 * 
 * @author Lexin Shan
 * @email lshan@eecs.berkeley.edu
 *
 */
public class TOAVtype extends NodeToSmtVtype {
	
	public TOAVtype( 
			int intNumBits,
            int inBits,
            int cBits,
			TempVarGen varGen) {
		super(intNumBits, inBits, cBits, varGen);
	}	

	@Override
	public varState cleanState(String var, Type t, MethodState mstate) {
	    SmtType type = SmtType.create(t, getNumBitsForType(t));
	    String properName = var;
		NodeToSmtState ret = new TOAState(properName, type, this);
		return ret;
	}

    @Override
    protected NodeToSmtValue handleNormalArrayRawAccess(NodeToSmtValue arr,
            NodeToSmtValue idx, NodeToSmtValue len, boolean isUnchecked)
    {
        return theoryOfArrayAccess(arr, idx);
    }

    @Override
    protected NodeToSmtValue handleNormalArrayConstAccess(NodeToSmtValue arr,
            NodeToSmtValue idx, NodeToSmtValue len, boolean isUnchecked)
    {
        int iidx = idx.getIntVal();
        
        int size = BitVectUtil.vectSize(arr.getType());
        
        if((iidx < 0 || iidx >= size)  )
            throw new ArrayIndexOutOfBoundsException("ARRAY OUT OF BOUNDS !(0<=" + iidx + " < " + size+") ");
        
        if (arr.obj != null) {
            return (NodeToSmtValue) arr.getVectValue().get(iidx);
        }
        return theoryOfArrayAccess(arr, idx);
    }
    
    protected NodeToSmtValue theoryOfArrayAccess(NodeToSmtValue arr, NodeToSmtValue idx) {
        
        Type eleType = TypedVtype.getElementType(arr.getType());
        
        return BOTTOM(eleType, OpCode.ARRACC, arr, idx);
    }
}
