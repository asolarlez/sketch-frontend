/**
 * 
 */
package streamit.frontend.nodes;

import java.util.List;

/**
 * An array-range reference. A[0:2] means the first 3 elements of A, and
 * A[0:1,4:6] means elements 0,1,4,5,6 of A, and A[4::2] means elements 4 and 5
 * of A. 
 * 
 * @author liviu
 */
public class ExprArrayRange extends Expression 
{
	public static class Range 
	{
		private final Expression start,end;
		public Range(Expression start, Expression end)
		{
			this.start=start;
			this.end=end;
		}
		public String toString()
		{
			return start+":"+end;
		}
		public Expression start() {return start;}
		public Expression end() {return end;}
	}
	public static class RangeLen 
	{
		private final Expression start;
		private final int len;
		private final Expression lenExpr;
		public RangeLen(Expression start)
		{
			this(start,1);
		}
		public RangeLen(Expression start, int len)
		{
			this.start=start;
			this.len=len;
			this.lenExpr=null;
		}
		public RangeLen(Expression start, Expression len)
		{
			this.start=start;
			this.len=0;
			this.lenExpr=len;
		}
		public Expression start() {return start;}
		public int len()
		{
			if(lenExpr!=null) throw new IllegalStateException("RangeLen len parameter has not been resolved to an int"); 
			return len;
		}
		public Expression getLenExpression() {return lenExpr;}
		public boolean hasLenExpression() {return lenExpr!=null;}
		public String toString()
		{
			if(len==1) return start.toString();
			return start+"::"+len;
		}
	}

	private Expression base;
	private List members;
	
	/**
	 * Construct a new array range Expression. "members" must be a
	 * list containing Range and RangeLen objects.
	 */
	public ExprArrayRange(Expression base, List members)
	{
		super(base.getContext());
		this.base=base;
		this.members=members;
		if(members.isEmpty()) throw new IllegalArgumentException();
	}
	
	/* (non-Javadoc)
	 * @see streamit.frontend.nodes.FENode#accept(streamit.frontend.nodes.FEVisitor)
	 */
	public Object accept(FEVisitor v) 
	{
		return v.visitExprArrayRange(this);
	}

	/**
	 * Returns a list containing Range and RangeLen objects. You must
	 * use instanceof to handle them separately.
	 */
	public List getMembers() {
		return members;
	}

	public String toString()
	{
		StringBuffer ret=new StringBuffer();
		ret.append(base);
		ret.append('[');
		ret.append(members.get(0));
		for(int i=1;i<members.size();i++)
		{
			ret.append(','); 
			ret.append(members.get(i));
		}
		ret.append(']');
		return ret.toString();
	}

	
	public ExprVar getAbsoluteBase(){
		Expression base= getBase();
		if(base instanceof ExprArrayRange) {
        	return ((ExprArrayRange)base).getAbsoluteBase();
        }
		if(base instanceof ExprVar) {
        	return ((ExprVar)base);
        }
		throw new RuntimeException("This should not happen");		
	}

	
	
	public Expression getBase() {
		return base;
	}
	
	public Expression getSingleIndex() {
		if(members.size()!=1) return null;
		Object o=members.get(0);
		if(!(o instanceof RangeLen)) return null;
		RangeLen r=(RangeLen) o;
		if(r.len!=1) return null;
		return r.start;
	}
	
	public boolean hasSingleIndex() {
		return getSingleIndex()!=null;
	}
	
}
