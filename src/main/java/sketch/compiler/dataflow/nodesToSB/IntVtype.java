package sketch.compiler.dataflow.nodesToSB;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.exprs.ExprStar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.StmtAssert;
import sketch.compiler.ast.core.stmts.StmtAssume;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.dataflow.MethodState;
import sketch.compiler.dataflow.abstractValue;
import sketch.compiler.dataflow.abstractValueType;
import sketch.compiler.dataflow.varState;
import sketch.util.DebugOut;

public class IntVtype extends abstractValueType {



    public abstractValue STAR(FENode star) {
        if (star instanceof ExprStar && ((ExprStar) star).isAngelicMax()) {
            return BOTTOM("**", true);
		}
        return BOTTOM("?", true);
	}

	public abstractValue BOTTOM(Type t){
		if( t instanceof TypePrimitive ){
			return BOTTOM();
		}
		assert false;
		return null;
	}

    public abstractValue TUPLE(List<abstractValue> vals, String name) {
        return new IntAbsValue(vals, true);
    }
	public abstractValue ARR(List<abstractValue> vals){
		return new IntAbsValue(vals);
	}

	public abstractValue BOTTOM(){
		return new IntAbsValue();
	}

	public abstractValue BOTTOM(String label) {
	    return BOTTOM(label, false);
	}

    public abstractValue BOTTOM(String label, boolean knownGeqZero) {
        return new IntAbsValue(label, knownGeqZero);
    }

	public abstractValue CONST(int v){
		return new IntAbsValue(v);
	}

	public abstractValue NULL(){
		return BOTTOM("null", true);
	}

	public abstractValue CONST(boolean v){
		return new IntAbsValue(v);
	}


    public void Assert(abstractValue val, StmtAssert stmt) {
		 if( val.hasIntVal() ){
			 if(val.getIntVal() == 0){
                DebugOut.printWarning("This assertion will fail unconditionally when you call this function: " +
                        stmt.getMsg());
			 }
		 }
	}

    public void Assume(abstractValue val, StmtAssume stmt) {
        // TODO xzl:
        // currently do nothing, can we utilize the assumption?
    }

	public varState cleanState(String var, Type t, MethodState mstate){
		return new IntState(t, this);
	}

	private String opStr(abstractValue v1, abstractValue v2, String op) { 
		return "(" + v1.toString() + " " + op + " " + v2.toString() + ")";
	}

	public abstractValue plus(abstractValue v1, abstractValue v2) {
		if( v1.hasIntVal() && v2.hasIntVal() ){
			return CONST( v1.getIntVal() + v2.getIntVal() );
		}else{
            return BOTTOM(opStr(v1, v2, "+"), v1.knownGeqZero() && v2.knownGeqZero());
		}
	}

	public abstractValue minus(abstractValue v1, abstractValue v2) {
		if( v1.hasIntVal() && v2.hasIntVal() ){
			return CONST( v1.getIntVal() - v2.getIntVal() );
		}else{
			return BOTTOM( opStr(v1, v2, "-") );
		}
	}

	public abstractValue times(abstractValue v1, abstractValue v2) {
		if( v1.hasIntVal() && v2.hasIntVal() ){
			return CONST( v1.getIntVal() * v2.getIntVal() );
		}else{
            return BOTTOM(opStr(v1, v2, "*"), v1.knownGeqZero() && v2.knownGeqZero());
		}
	}

	public abstractValue over(abstractValue v1, abstractValue v2) {
		if( v1.hasIntVal() && v2.hasIntVal() ){
			return CONST( v1.getIntVal() / v2.getIntVal() );
		}else{
            return BOTTOM(opStr(v1, v2, "/"), v1.knownGeqZero() && v2.knownGeqZero());
		}
	}

	public abstractValue mod(abstractValue v1, abstractValue v2) {
		if( v1.hasIntVal() && v2.hasIntVal() ){
			return CONST( v1.getIntVal() % v2.getIntVal() );
		}else{
            return BOTTOM(opStr(v1, v2, "%"), true);
		}
	}

	protected boolean intToBool(int v) {
		if(v>0)
			return true;
		else
			return false;
	}

	protected int boolToInt(boolean b) {
		if(b)
			return 1;
		else
			return 0;
	}


	public abstractValue shr(abstractValue v1, abstractValue v2){
        return BOTTOM(v1 + ">>" + v2, v1.knownGeqZero());
	}

	public abstractValue shl(abstractValue v1, abstractValue v2){
        return BOTTOM(v1 + "<<" + v2, v1.knownGeqZero());
	}


	public abstractValue and(abstractValue v1, abstractValue v2) {
		if( v1.hasIntVal() && v2.hasIntVal() ){
			return CONST( boolToInt(intToBool(v1.getIntVal()) && intToBool(v2.getIntVal())) );
		}else{
			if( v1.hasIntVal() ){
				if(v1.getIntVal() == 0){
					return CONST(0);
				}else{
					assert v1.getIntVal()==1;
					return v2;
				}
			}
			if( v2.hasIntVal()){
				if(v2.getIntVal() == 0){
					return CONST(0);
				}else{
					assert v2.getIntVal()==1;
					return v1;
				}
			}
            return BOTTOM(opStr(v1, v2, "&"), v1.knownGeqZero() && v2.knownGeqZero());
		}
	}

	public abstractValue or(abstractValue v1, abstractValue v2) {
		if( v1.hasIntVal() && v2.hasIntVal() ){
			return CONST( boolToInt(intToBool(v1.getIntVal()) || intToBool(v2.getIntVal())) );
		}else{
			if( v1.hasIntVal() ){
				if(v1.getIntVal() == 1){
					return CONST(1);
				}else{
					assert v1.getIntVal()==0;
					return v2;
				}
			}
			if( v2.hasIntVal()){
				if(v2.getIntVal() == 1){
					return CONST(1);
				}else{
					assert v2.getIntVal()==0;
					return v1;
				}
			}
            return BOTTOM(opStr(v1, v2, "|"), v1.knownGeqZero() && v2.knownGeqZero());
		}
	}

	public abstractValue xor(abstractValue v1, abstractValue v2) {
		if( v1.hasIntVal() && v2.hasIntVal() ){
			return CONST( boolToInt(intToBool(v1.getIntVal()) ^ intToBool(v2.getIntVal())) );
		}else{
            return BOTTOM(opStr(v1, v2, "^"), v1.knownGeqZero() && v2.knownGeqZero());
		}
	}

	public abstractValue gt(abstractValue v1, abstractValue v2) {
		if( v1.hasIntVal() && v2.hasIntVal() ){
			return CONST( v1.getIntVal() > v2.getIntVal() );
		}else{
            return BOTTOM(opStr(v1, v2, ">"), true);
		}
	}

	public abstractValue lt(abstractValue v1, abstractValue v2) {
		if( v1.hasIntVal() && v2.hasIntVal() ){
			return CONST( v1.getIntVal() < v2.getIntVal() );
		}else{
            return BOTTOM(opStr(v1, v2, "<"), true);
		}
	}

	public abstractValue ge(abstractValue v1, abstractValue v2) {
		if( v1.hasIntVal() && v2.hasIntVal() ){
			return CONST( v1.getIntVal() >= v2.getIntVal() );
		}else{
            return BOTTOM(opStr(v1, v2, ">="), true);
		}
	}

	public abstractValue le(abstractValue v1, abstractValue v2) {
		if( v1.hasIntVal() && v2.hasIntVal() ){
			return CONST( v1.getIntVal() <= v2.getIntVal() );
		}else{
            return BOTTOM(opStr(v1, v2, "<="), true);
		}
	}

	public abstractValue eq(abstractValue v1, abstractValue v2) {
		if( v1.hasIntVal() && v2.hasIntVal() ){
			return CONST( v1.getIntVal() == v2.getIntVal() );
		}else{
            return BOTTOM(opStr(v1, v2, "=="), true);
		}
	}

    public abstractValue tupleacc(abstractValue arr, abstractValue idx) {
        return BOTTOM("((" + arr + ").[" + idx + "])");
    }
	public abstractValue arracc(abstractValue arr, abstractValue idx) {
		assert false; return null;
	}
	
	
	public abstractValue outOfBounds(){
		return BOTTOM("OUT OF BOUNDS");
	}

	public abstractValue arracc(abstractValue arr, abstractValue idx, abstractValue len, boolean isUnchecked) {
		if(  arr.isBottom()  ){
            return BOTTOM("(" + arr + "[|" + idx + "|])");
		}
		if(len != null){
            if (len.hasIntVal()) {
                int ilen = len.getIntVal();
                if (ilen != 1) {
                    List<abstractValue> lst = new ArrayList<abstractValue>(ilen);
                    for (int i = 0; i < ilen; ++i) {
                        lst.add(arracc(arr, plus(idx, CONST(i)), null, isUnchecked));
                    }
                    return ARR(lst);
                }
            } else {
                return BOTTOM("(" + arr + "[" + idx + "])");
            }
		}

		if( idx.hasIntVal() ){
			int iidx = idx.getIntVal() ;
			if(arr.isVect()){
    			int size = arr.getVectValue().size();
    			 if( !isUnchecked && (iidx < 0 || iidx >= size)  )
    				throw new ArrayIndexOutOfBoundsException("ARRAY OUT OF BOUNDS !(0<=" + iidx + " < " + size+") ");
    			if(iidx < 0 || iidx >= size)
    				return outOfBounds();
    			return arr.getVectValue().get(idx.getIntVal());
			}else{
                return BOTTOM("(" + arr + "[|" + idx + "|])");
			}
		}else{
			return rawArracc(arr, idx);
		}
	}


	protected abstractValue rawArracc(abstractValue arr, abstractValue idx){
        return BOTTOM("(" + arr + "[|" + idx + "|])");
	}

	public abstractValue cast(abstractValue v1, Type type) {

		if(type.equals(TypePrimitive.inttype) && !v1.isVect()){
			return v1;
		}

		if(v1.isVect() && type.equals( TypePrimitive.inttype ) ){
			//Casting a bit-vector into an integer.
			List<abstractValue> lst = v1.getVectValue();
			String result =  "( $$";
			int i = 0;
	    	int val=0;
	    	int t = 1;
	    	boolean hasValue = true;
			for(Iterator<abstractValue> it = lst.iterator(); it.hasNext(); ){
				abstractValue o = it.next();
	    		if(!o.hasIntVal()){
	    			result += " " + o;
	    			hasValue = false;
	    		}else{
	    			int curv =  o.getIntVal();
	    			result += " " + curv;
	    			assert curv == 1 || curv == 0 : "Casting only works for boolean arrays!!" + v1;
	    			if( curv == 1 ) val += t;
	    			t = t*2;
	    		}
	    		++i;
			}
			result += " $$ )";
			if(hasValue){
				return CONST(val);
	    	}else{
	    		return BOTTOM(result);
	    	}
		}


		if(type instanceof TypeArray ){

            TypeArray t = (TypeArray) type;
            Expression elen = t.getLength();

            if (!v1.isVect() && elen != null) {
				List<abstractValue> vls = new ArrayList<abstractValue>(1);
				vls.add(v1);
				v1 = ARR(vls);
			}

            Integer len = elen == null ? null : elen.getIValue();
			if(len != null){
				int mlen = len;
				List<abstractValue> lst1 = v1.getVectValue();
				if( mlen >= lst1.size()  ) mlen = lst1.size();
				List<abstractValue> lst2 = lst1.subList(0, mlen);
				for(int j=mlen; j<len; ++j){
					lst2.add(CONST(0));
				}
				return ARR(lst2);
			}else{
				return v1;
			}
		}

		if(v1.isBottom() ){
			return v1;
		}
        return BOTTOM(type.toString());
	}

	public abstractValue not(abstractValue v1) {
		if( v1.hasIntVal() ){
			return CONST( 1-v1.getIntVal()  );
		}else{
			return BOTTOM( "( ! " + v1 + ")" );
		}
	}

	public abstractValue neg(abstractValue v1) {
		if( v1.hasIntVal() ){
			return CONST( -v1.getIntVal()  );
		}else{
			return BOTTOM( "( -" + v1 + ")" );
		}
	}

	public abstractValue join(abstractValue v1, abstractValue v2) {
		if( v1.equals(v2) ){
			return v1;
		}else{
			return this.BOTTOM();
		}
	}


	public abstractValue ternary(abstractValue cond, abstractValue vtrue, abstractValue vfalse) {
		assert (cond != null) : "API usage bug";

		if( cond.hasIntVal() ){
			if( cond.getIntVal() != 0){
				return vtrue;
			}else{
				return vfalse;
			}
		}else{
            return BOTTOM("(" + cond + "? (" + vtrue + ") : (" + vfalse + ") )",
                    vtrue.knownGeqZero() && vfalse.knownGeqZero());
		}
	}

	public abstractValue condjoin(abstractValue cond, abstractValue vtrue, abstractValue vfalse) {
		if(cond == null) return join(vtrue, vfalse);

		if( cond.hasIntVal() ){
			if( cond.getIntVal() != 0){
				return vtrue;
			}else{
				return vfalse;
			}
		}else{
            if (vtrue.equals(vfalse)) {
                return vtrue;
            }
            return BOTTOM("(" + cond + "? (" + vtrue + ") : (" + vfalse + ") )",
                    vtrue.knownGeqZero() && vfalse.knownGeqZero());
		}
	}

    public void funcall(Function fun, List<abstractValue> avlist,
            List<abstractValue> outSlist, abstractValue patchCond, MethodState state,
            int clusterId)
    {
		Iterator<Parameter> formalParams = fun.getParams().iterator();
    	while(formalParams.hasNext()){
    		Parameter param = formalParams.next();
    		if( param.isParameterOutput()){
    			outSlist.add(BOTTOM());
    		}
    	}
	}

}
