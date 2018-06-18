/*
 * Copyright 2003 by the Massachusetts Institute of Technology.
 *
 * Permission to use, copy, modify, and distribute this
 * software and its documentation for any purpose and without
 * fee is hereby granted, provided that the above copyright
 * notice appear in all copies and that both that copyright
 * notice and this permission notice appear in supporting
 * documentation, and that the name of M.I.T. not be used in
 * advertising or publicity pertaining to distribution of the
 * software without specific, written prior permission.
 * M.I.T. makes no representations about the suitability of
 * this software for any purpose.  It is provided "as is"
 * without express or implied warranty.
 */

package sketch.compiler.ast.core.exprs;

import java.util.List;
import java.util.Vector;

import sketch.compiler.ast.core.FEContext;
import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.FEVisitor;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.main.cmdline.SketchOptions;

/**
 * An integer-valued constant with an unknown value ("??" in the program, the hole), whose
 * value should be resolve by the Sketch Solver to satisfy all the constraints.
 * 
 * @author David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id$
 */
public class ExprStar extends Expression
{
    public enum Kind {
        NORMAL, ANGELIC, COUNTER
    }
	private int size;
    private int rangelow;
    private int rangehigh;
	private double frangelow;
	private double frangehigh;
    private boolean hasrange = false;

	public Expression vectorSize;
	Vector<FENode> depObjects;
	/** fixed domain of values */
	private boolean isFixed;
	private Type type;
	public int INT_SIZE=5;
    public final boolean isGlobal;
    protected String starName = "ANON";
    public boolean typeWasSetByScala = false;
	private static int NEXT_UID=0;
	private static String HOLE_BASE="H__";
    private static String ANGJ_BASE = "AH__";
	
    private Kind kind = Kind.NORMAL;
    private boolean isSpecial = false;
    private List<ExprStar> parentHoles; // required for special depth holes

    // private Expression exprMax = null;

	public String getSname(){ return starName; }
	public void renewName(){ starName = HOLE_BASE + (NEXT_UID++); }
	public void extendName(String ext){ starName += ext; } 
	
    public ExprStar(ExprStar old, boolean isGlobal) {
        super(old);
        size = old.size;
        isFixed = old.isFixed;
        type = old.type;
        this.isGlobal = isGlobal;
        INT_SIZE = old.INT_SIZE;
        vectorSize = old.vectorSize;
        rangelow = old.rangelow;
        rangehigh = old.rangehigh;
		frangelow = old.frangelow;
		frangehigh = old.frangehigh;
        hasrange = old.hasrange;
        this.starName = old.starName;
        this.kind = old.kind;
        this.isSpecial = old.isSpecial;
        this.parentHoles = old.parentHoles;

    }

	public ExprStar(ExprStar old)
    {
        super(old);
        size = old.size;
        isFixed = old.isFixed;
        type = old.type;
        isGlobal = old.isGlobal;
        INT_SIZE = old.INT_SIZE;
        vectorSize = old.vectorSize;
        rangelow = old.rangelow;
        rangehigh = old.rangehigh;
		frangelow = old.frangelow;
		frangehigh = old.frangehigh;
        hasrange = old.hasrange;
        this.starName = old.starName;
        this.kind = old.kind;
        // this.exprMax = old.exprMax;
    }

    /** Create a new ExprConstInt with a specified value. */
    public ExprStar(FENode context)
    {
        this(context.getCx(), Kind.NORMAL);
    }

    public ExprStar(FEContext context) {
        this(context, Kind.NORMAL);
    }

    /** Create a new ExprConstInt with a specified value.
     * @deprecated
     */
    public ExprStar(FEContext context, Kind kind)
    {
        super(context);
        size = 1;
        isGlobal = false;
        isFixed = false;
        this.kind = kind;
        // this.exprMax = max;
        if (kind == Kind.COUNTER) {
            this.starName = HOLE_BASE;
        } else {
        this.starName = (kind == Kind.ANGELIC ? ANGJ_BASE : HOLE_BASE) + (NEXT_UID++);
        }
    }

    /**
     *
     */
    public ExprStar(FENode context, int size)
    {
        this(context.getCx(), size);
    }

    /**
    *
    */
    public ExprStar(FENode context, int rstart, int rend, int size) {
        super(context);
        this.size = size;
        isGlobal = false;
        isFixed = true;
        rangelow = rstart;
        rangehigh = rend;
        hasrange = true;
        this.starName = HOLE_BASE + (NEXT_UID++);
    }

        /**
     * @deprecated
     */
    public ExprStar(FEContext context, int size) {
        this(context, size, Kind.NORMAL);
    }

    
    public ExprStar(FENode context, int size, boolean isGlobal) {
        super(context);
        isFixed = true;
        this.isGlobal = isGlobal;
        this.size = size;
        this.kind = Kind.NORMAL;
        // this.exprMax = max;
        if (kind == Kind.COUNTER) {
            this.starName = HOLE_BASE;
        } else {
            this.starName = (kind == Kind.ANGELIC ? ANGJ_BASE : HOLE_BASE) + (NEXT_UID++);
        }
    }
    
    /**
     * @deprecated
     */
    public ExprStar(FEContext context, int size, Kind kind)
    {
        super(context);
        isFixed = true;
        isGlobal = false;
        this.size = size;
        this.kind = kind;
        // this.exprMax = max;
        if (kind == Kind.COUNTER) {
            this.starName = HOLE_BASE;
        } else {
            this.starName = (kind == Kind.ANGELIC ? ANGJ_BASE : HOLE_BASE) + (NEXT_UID++);
        }
    }

    /**
     * @deprecated
     */
    public ExprStar(FEContext context, int rstart, int rend) {
        super(context);
        isFixed = true;
        isGlobal = false;
        this.size = 1;
        rangelow = rstart;
        rangehigh = rend;
        hasrange = true;
        this.starName = HOLE_BASE + (NEXT_UID++);
    }

	public ExprStar(FEContext context, double rstart, double rend) {
		super(context);
		isFixed = true;
		isGlobal = false;
		this.size = 1;
		frangelow = rstart;
		frangehigh = rend;
		hasrange = true;
		this.starName = HOLE_BASE + (NEXT_UID++);
	}

    @Deprecated
    public ExprStar(FEContext ctx, Type typ, int domainsize) {
        super(ctx);
        isGlobal = false;
        this.type = typ;
        this.size = domainsize;// (int) Math.ceil(Math.log(domainsize) / Math.log(2));
        isFixed = true;
        this.starName = HOLE_BASE + (NEXT_UID++);
        this.typeWasSetByScala = true;
    }

    public ExprStar(FENode context, int size, Type typ) {
        this(context, size);
        this.setType(typ);
    }

    // public ExprStar createWithExprMax(Expression max) {
    // if (isFixed) {
    // return new ExprStar(this.getCx(), size, true, max);
    // } else {
    // return new ExprStar(this.getCx(), true, max);
    // }
    // }

	public FENode getDepObject(int i){
		Type t = type;
		if(type instanceof TypePrimitive){
			return this;
		}else{
			assert type instanceof TypeArray;
			t = ((TypeArray)type).getBase();
		}

		if(depObjects == null){
			depObjects = new Vector<FENode>();
			depObjects.setSize(i+1);
		}
		if(depObjects.size() <= i){
			depObjects.setSize(i+1);
		}
		if(depObjects.get(i) == null){
			ExprStar es = new ExprStar(this);
			es.extendName("_" + i);
			es.type = t;
			depObjects.set(i, es);
		}
		return depObjects.get(i);
	}

    /** Accept a front-end visitor. */
    public Object accept(FEVisitor v)
    {
        return v.visitExprStar(this);
    }

    public boolean equals(Object other)
    {
    	return this == other;
        /*if (!(other instanceof ExprStar))
            return false;
        return true; */
    }

    private String detailName() {
        if (isAngelicMax()) {
            return "**/*" + getSname() /* + (exprMax == null ? "" : "@" + exprMax) */
                    + "*/";
        } else {
            return "??" + "/*" + getSname() + "*/";
        }
    }

    public Object toCode() {
        assert isAngelicMax() : "only AGMAX stars can be generated to java!";
        return detailName();
    }

    public String toString()
    {
        if (isAngelicMax()) {
            return "**/*" + getSname() /* + (exprMax == null ? "" : "@" + exprMax) */
                    + "*/";
        } else {
            if (getType() != null) {
                return "??" + "/* " + getSname() + getType() + ":" + size + " */";
            } else {
                return "??" + "/*" + getSname() + "*/";
            }
        }

    }

	/**
	 * @param size The size to set.
	 */
	public void setSize(int size) {
		this.size = size;
	}

	/**
	 * @return Returns the size.
	 */
	public int getSize() {
		return size;
	}

	/**
	 * @param isFixed The isFixed to set.
	 */
	public void setFixed(boolean isFixed) {
		this.isFixed = isFixed;
	}

	/**
	 * @return Returns the isFixed.
	 */
	public boolean isFixed() {
		return isFixed;
	}

	/**
	 * @param type the type to set
	 */
	public void setType(Type type) {
        if (type == null) {
            return;
        }
		this.type = type;

		Type tt = type;
		while(tt instanceof TypeArray){
			tt = ((TypeArray)tt).getBase();
		}
        if (((tt.equals(TypePrimitive.inttype) || tt.equals(TypePrimitive.chartype))) &&
                !isFixed)
        {
			setSize( SketchOptions.getSingleton().bndOpts.cbits  );
		}
	}

	/**
	 * @return the type
	 */
	public Type getType() {
		return type;
	}

    public boolean hasRange() {
        return this.hasrange;
    }
    public boolean isAngelicMax() {
        return kind == Kind.ANGELIC;
    }

    public boolean isCounter() {
        return kind == Kind.COUNTER;
    }

    public int lowerBound() {
        return this.rangelow;
    }

    public int upperBound() {
        return this.rangehigh;
    }

	public double fLowerBound() {
		return this.frangelow;
	}

	public double fUpperBound() {
		return this.frangehigh;
	}

	public void makeSpecial() {
		isSpecial = true;
	}

    public void makeSpecial(List<ExprStar> parentDepths) {
        isSpecial = true;
        this.parentHoles = parentDepths;
    }

    public List<ExprStar> parentHoles() {
        return this.parentHoles;
    }

    public boolean special() {
        return isSpecial;
    }
    // public Expression getExprMax() {
    // return exprMax;
    // }
}
