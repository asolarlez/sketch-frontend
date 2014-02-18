/**
 *
 */
package sketch.compiler.ast.core.exprs;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.FEVisitor;
import sketch.compiler.passes.structure.GetAssignLHS;

/**
 * An array-range reference. A[0:2] means the first 3 elements of A, and A[4::2] means
 * elements 4 and 5 of A. See Sketch language manual for more information.
 * 
 * @author liviu
 */
public class ExprArrayRange extends Expression
{
    /**
     * Represents the indices in {@link ExprArrayRange}. Includes the start index and the
     * length of the desired sub-array.
     */
	public static class RangeLen
	{
		private final Expression start;		
		private final Expression lenExpr;		
		public RangeLen(Expression start)
		{
			this(start, null);
		}
		public RangeLen(Expression start, int len)
		{
			this.start=start;			
			this.lenExpr= ExprConstInt.createConstant(len);
		}
		public RangeLen(Expression start, Expression len)
		{
			this.start=start;			
			this.lenExpr=len;			
		}
		public Expression start() {return start;}		
		public Expression getLenExpression() {
			return lenExpr;
		}
		public boolean hasLen() {return lenExpr!=null;}
		public String toString()
		{			
			if(lenExpr != null) return start+"::"+lenExpr; 
			return start.toString();
		}
	}


	private Expression base;
	private RangeLen index;
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
        this(base, base, Collections.singletonList(new RangeLen(offset)));
	}

	/*
	 * TODO: why do all these methods take FENode parameters that are never
	 * used???
	 */
	public ExprArrayRange(FENode node, Expression base, Expression offset)
	{
        this(node, base, Collections.singletonList(new RangeLen(offset)));
	}

    public ExprArrayRange(FENode node, Expression base, List<RangeLen> rl)
	{
		this(node, base, rl, false);
	}

    /**
     * NOTE -- vector of array ranges for comma arrays. Since arr[x, y] = (arr[x])[y], we
     * want to set (arr[x]) as the new base, and y as the index.
     */
    public ExprArrayRange(FENode node, Expression base, List<RangeLen> rl,
            boolean unchecked)
    {
        super(node);
        if (rl.size() == 1) {
            this.base = base;
        } else {
            this.base = new ExprArrayRange(node, base, rl.subList(0, rl.size() - 1));
        }

        // is there a mistake here
        this.index = rl.get(rl.size() - 1);
        setUnchecked(unchecked);
	}

	
	public ExprArrayRange(FENode node, Expression base, Expression offset, boolean unchecked)
	{
        this(node, base, Collections.singletonList(new RangeLen(offset)), unchecked);
	}

    public ExprArrayRange(FENode node, Expression nbase, RangeLen rangeLen,
            boolean unchecked2)
    {
        this(node, nbase, Collections.singletonList(rangeLen), unchecked2);
    }

    public ExprArrayRange(FENode node, Expression base2, RangeLen flatRl) {
        this(node, base2, Collections.singletonList(flatRl));
    }

    public Expression getOffset() {
		RangeLen rl = index;
		assert !rl.hasLen();
		return rl.start;
	}

	public RangeLen getSelection(){
	    return index;
	}
	
	


	/* (non-Javadoc)
	 * @see sketch.compiler.nodes.FENode#accept(sketch.compiler.nodes.FEVisitor)
	 */
	public Object accept(FEVisitor v)
	{
		return v.visitExprArrayRange(this);
	}

	

	public String toString()
	{
		StringBuffer ret=new StringBuffer();
		ret.append(base);
		ret.append('[');
		ret.append(index);		
		ret.append(']');
		return ret.toString();
	}

	public List<RangeLen> getArraySelections () {
		List<RangeLen> sels = new ArrayList<RangeLen> ();
		
		Expression base = getBase ();
		if (base instanceof ExprArrayRange) {
			sels.addAll (((ExprArrayRange) base).getArraySelections ());
		}		

		sels.add (index);

		return sels;
	}

	public List<Expression> getArrayIndices() {
        List<Expression> indices = new ArrayList<Expression>();
        Expression base= getBase();
        if(base instanceof ExprArrayRange) {
        	indices.addAll(((ExprArrayRange) base).getArrayIndices());
        }
        RangeLen rl=index;
        assert !rl.hasLen(): "In stencil mode, array ranges (a[1::2]) are not allowed";
        indices.add(rl.start());
        return indices;
    }

    public ExprVar getAbsoluteBase() {
        return (new GetAssignLHS()).visitExprArrayRange(this);
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
		RangeLen r=index;
		if(r.hasLen()) return null;
		return r.start;
	}

	public boolean hasSingleIndex() {
		return getSingleIndex()!=null;
	}

	@Override public boolean isLValue () {
		return true;
	}

    public boolean equals(Object other) {
        if (!(other instanceof ExprArrayRange))
            return false;
        return this.toString().equals(other.toString());
    }

    public Expression getBaseAndIndices(List<RangeLen> indices) {
        Expression base = this.base;
        indices.add(this.index);
        while (base instanceof ExprArrayRange) {
            ExprArrayRange e = (ExprArrayRange) base;
            indices.add(e.index);
            base = e.base;
        }

        return base;
    }
}
