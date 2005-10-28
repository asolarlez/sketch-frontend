/**
 * 
 */
package streamit.frontend.nodes;

import java.util.List;

/**
 * An array-range reference. A[0:2] means the first 3 elements of A, and
 * A[0:1,4:6] means elements 0,1,4,5,6 of A.
 * 
 * @author liviu
 */
public class ExprArrayRange extends Expression 
{
	public static class Range 
	{
		public final Expression start,end;
		public Range(Expression start)
		{
			this.start=start;
			this.end=start;
		}
		public Range(Expression start, Expression end)
		{
			this.start=start;
			this.end=end;
		}
		public static Range makeRange(Expression startOffset, Expression length)
		{
			Expression endOff=new ExprBinary(length.getContext(),ExprBinary.BINOP_ADD,startOffset,length);
			return new Range(startOffset,endOff);
		}
		public String toString()
		{
			return start+":"+end;
		}
	}

	private Expression base;
	private List members;
	
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

	public Expression getBase() {
		return base;
	}
}
