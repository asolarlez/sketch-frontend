package sketch.compiler.smt.partialeval;

import java.util.ArrayList;
import java.util.List;

import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.dataflow.MethodState;
import sketch.compiler.dataflow.abstractValue;
import sketch.compiler.dataflow.varState;
import sketch.compiler.smt.GeneralStatistics;

/**
 * This class is for translating to SMT formulas that blast array elements into 
 * individual variables
 * 
 * @author Lexin Shan
 * @email lshan@eecs.berkeley.edu
 *
 */
public class BlastArrayVtype extends NodeToSmtVtype {

	public BlastArrayVtype(
			int intNumBits,
			int inBits,
			int cBits,
			GeneralStatistics stat,
			TempVarGen varGen) {
		super(intNumBits, inBits, cBits, stat, varGen);
		
	}


	@Override
	public varState cleanState(String var, Type t, MethodState mstate) {
		SmtType type = SmtType.create(t, getNumBitsForType(t));
		String properName = var; 
		NodeToSmtState ret = new BlastArrayState(properName, type, this);
		return ret;
	}
	
	protected NodeToSmtValue handleNormalArrayConstAccess(NodeToSmtValue arr,
            NodeToSmtValue idx, NodeToSmtValue len, boolean isUnchecked)
    {
        int iidx = idx.getIntVal();
        int size = arr.getVectValue().size();
        if((iidx < 0 || iidx >= size)  )
            throw new ArrayIndexOutOfBoundsException("ARRAY OUT OF BOUNDS !(0<=" + iidx + " < " + size+") ");
        
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
        return (NodeToSmtValue) arr.getVectValue().get(iidx);
    }
	
	/**
	 * Partial eval the array-access node when the index expression is BOTTOM
	 */
	@Override
	protected NodeToSmtValue handleNormalArrayRawAccess(NodeToSmtValue arr,
	        NodeToSmtValue idx, NodeToSmtValue len, boolean isUnchecked)
	{
	    return rawArraccRecursiveNormalArray(arr, idx, 0, arr.getVectValue().size(), len.getIntVal());
	}
	
	protected NodeToSmtValue rawArraccRecursiveNormalArray(NodeToSmtValue arr, NodeToSmtValue idx, int i, int size, int len) {
		
		if (i + len - 1 == size - 1) {
			// the last element
			return (NodeToSmtValue) arr.getVectValue().get(i);
		} else {		
			return condjoin(eq(idx, CONST(i)), (NodeToSmtValue) arr.getVectValue().get(i), 
			        rawArraccRecursiveNormalArray(arr, idx, i+1, size, len));
		}
	}
	
	

}
