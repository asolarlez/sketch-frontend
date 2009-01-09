/**
 *
 */
package streamit.frontend.nodes;

import java.util.*;

/**
 * An array-range reference. A[0:2] means the first 3 elements of A, and
 * A[0:1,4:6] means elements 0,1,4,5,6 of A, and A[4::2] means elements 4 and 5
 * of A.
 *
 * @author liviu
 */
public class ExprArrayRange extends Expression  implements ExprArray
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
			this.lenExpr= null;
		}
		public RangeLen(Expression start, Expression len)
		{
			this.start=start;
			Integer i = len.getIValue();
			if(i!= null){
				this.len = i;
				this.lenExpr = null;
			}else{
				this.len=0;
				this.lenExpr=len;
			}
		}
		public Expression start() {return start;}
		public int len()
		{
			if(lenExpr!=null) throw new IllegalStateException("RangeLen len parameter has not been resolved to an int");
			return len;
		}
		public Expression getLenExpression() {
			if(lenExpr != null){ return lenExpr; }
			return new ExprConstInt(len);			
		}
		public boolean hasLenExpression() {return lenExpr!=null;}
		public String toString()
		{
			if(len==1) return start.toString();
			return start+"::"+len;
		}
	}

	private Expression base;
	private List members;
	 private boolean unchecked=false;
	/**
	 * @param unchecked The unchecked to set.
	 */
	public void setUnchecked(boolean unchecked) {
		this.unchecked = unchecked;
	}

	/**
	 * @return Returns the unchecked.
	 */
	public boolean isUnchecked() {
		return unchecked;
	}
	public ExprArrayRange(Expression base, Expression offset)
	{
		this(base, Collections.singletonList(new RangeLen(offset)));
	}

	/*
	 * TODO: why do all these methods take FENode parameters that are never
	 * used???
	 */
	public ExprArrayRange(FENode node, Expression base, Expression offset)
	{
		this(node, base, Collections.singletonList(new RangeLen(offset)));
	}
	public ExprArrayRange(FENode node, Expression base, RangeLen rl)
	{
		this(node, base, Collections.singletonList(rl));
	}
	public ExprArrayRange(FENode node, Expression base, RangeLen rl, boolean unchecked)
	{
		this(node, base, Collections.singletonList(rl), unchecked);
	}

	public ExprArrayRange(FENode node, Expression base, Expression offset, boolean unchecked)
	{
		this(node, base, Collections.singletonList(new RangeLen(offset)));
		setUnchecked(unchecked);
	}


	public Expression getOffset(){
		assert members.size() == 1;
		assert members.get(0) instanceof RangeLen;
		RangeLen rl = (RangeLen)members.get(0);
		assert rl.len == 1;
		return rl.start;
	}


	/**
	 * Construct a new array range Expression. "members" must be a
	 * list containing Range and RangeLen objects.
	 */
	public ExprArrayRange(FENode cx, Expression base, List members)
	{
		super(cx);
		this.base=base;
		this.members=members;
		if(members.isEmpty()) throw new IllegalArgumentException();
	}
	public ExprArrayRange(FENode cx, Expression base, List members, boolean unchecked)
	{
		super(cx);
		this.base=base;
		this.members=members;
		if(members.isEmpty()) throw new IllegalArgumentException();
		setUnchecked(unchecked);
	}


	/**
	 * Construct a new array range Expression. "members" must be a
	 * list containing Range and RangeLen objects.
	 */
	public ExprArrayRange(Expression base, List members)
	{
		super(base);
		this.base=base;
		this.members=members;
		if(members.isEmpty()) throw new IllegalArgumentException();
	}
	public ExprArrayRange(Expression base, List members, boolean unchecked)
	{
		super(base);
		this.base=base;
		this.members=members;
		if(members.isEmpty()) throw new IllegalArgumentException();
		setUnchecked(unchecked);
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

	public List<RangeLen> getArraySelections () {
		List<RangeLen> sels = new ArrayList<RangeLen> ();
		List memb = getMembers ();

		Expression base = getBase ();
		if (base instanceof ExprArrayRange) {
			sels.addAll (((ExprArrayRange) base).getArraySelections ());
		}

		if (memb.size () > 1) {
			report ("sorry, mult-index (e.g., a[1, 2]) is not supported");
			assert false;
		}

		sels.add ((RangeLen) memb.get (0));

		return sels;
	}

	public List<Expression> getArrayIndices() {
        List<Expression> indices = new ArrayList<Expression>();
        Expression base= getBase();
        if(base instanceof ExprArrayRange) {
        	indices.addAll(((ExprArrayRange) base).getArrayIndices());
        }
        List memb= getMembers();
        assert memb.size()==1: "In stencil mode, we permit only single-element indexing, i.e. no a[1,3,4]";
        assert memb.get(0) instanceof RangeLen: "In stencil mode, array ranges (a[1:4]) are not allowed";
        RangeLen rl=(RangeLen) memb.get(0);
        assert rl.len()==1: "In stencil mode, array ranges (a[1::2]) are not allowed";
        indices.add(rl.start());
        return indices;
    }



	public ExprVar getAbsoluteBase(){
		Expression base= getBase();
		if (base instanceof ExprArrayRange) {
        	return ((ExprArrayRange)base).getAbsoluteBase();
        } else if (base instanceof ExprVar) {
        	return ((ExprVar)base);
        } else {
        	report ("unexpected array base: "+ this + ", of type");
        	throw new RuntimeException ();
        }
	}

	/**
	 * @return the bottom-level object being indexed; e.g:
	 *   "x.f[2][2]".getAbsoluteBaseExpr () --> "x.f"
	 */
	public Expression getAbsoluteBaseExpr () {
		return (getBase () instanceof ExprArrayRange) ?
					((ExprArrayRange)getBase ()).getAbsoluteBaseExpr ()
					: getBase ();
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

	@Override public boolean isLValue () {
		return true;
	}
}
