package sketch.compiler.smt.partialeval;

import java.util.HashMap;

import sketch.compiler.ast.core.typs.Type;

public class SmtType extends Type {
	
	private static final boolean USE_CACHE = true;
	
	private int mNumBits;
	
	private Type mRealType;
	
	private static HashMap<Type, HashMap<Integer, SmtType>> cache = new HashMap<Type, HashMap<Integer,SmtType>>();
	private static int typesSaved = 0;
	
	public static SmtType create(Type realType, int numBits) {

		if (USE_CACHE) {
			HashMap<Integer, SmtType> inner = cache.get(realType);
			SmtType smtType = null;

			if (inner != null) {
				smtType = inner.get(numBits);
				if (smtType == null) {
					smtType = new SmtType(realType, numBits);
					inner.put(numBits, smtType);
					return smtType;
				} else {
					typesSaved++;
					return smtType;
				}

			} else {
				inner = new HashMap<Integer, SmtType>();
				cache.put(realType, inner);
				smtType = new SmtType(realType, numBits);
				inner.put(numBits, smtType);
				return smtType;
			}
		} else {
			return new SmtType(realType, numBits);
		}
	}
	
	private SmtType(Type realType, int numBits) {
		assert realType != null : "realType can not be null";
		assert !(realType instanceof SmtType) : "realType can not be another SmtType";

		mRealType = realType;
		mNumBits = numBits;
	}
	
	public int getNumBits() {
		return mNumBits;
	}
	
	public Type getRealType() {
		return mRealType;
	}
	
	@Override
	public Type leastCommonPromotion(Type that) {
		SmtType that2 = (SmtType) that;
		Type commonType = this.mRealType.leastCommonPromotion(that2.mRealType);
		
		int max = Math.max(this.mNumBits, that2.mNumBits); 
		return SmtType.create(commonType, max);
	}
	
	@Override
	public boolean promotesTo(Type that) {
		return this.getRealType().promotesTo(((SmtType) that).getRealType());
	}
	
	

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof SmtType) {
			SmtType t2 = (SmtType) obj;
			return this.getRealType().equals(t2.getRealType());
			
		} else {
			return this.getRealType().equals(obj);
		}
	}
	
	@Override
	public int hashCode() {
		return mRealType.hashCode() ^ mNumBits;
	}
	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(mRealType.toString());
		sb.append(' ');
		sb.append(mNumBits);
		return sb.toString();
	}
}
