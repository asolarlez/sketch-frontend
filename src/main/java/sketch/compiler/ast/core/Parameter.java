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

package streamit.frontend.nodes;



/**
 * A formal parameter to a function or stream.  This is a pair of a
 * string name and a <code>Type</code>.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id$
 */
public class Parameter
{
	public static int IN = 0;
	public static int OUT = 1;
	public static int REF = 2;
	
    private final Type type;
    private final String name;
    private final int partype;
    
    /** Creates a new Parameter with the specified type and name. */
    public Parameter(Type type, String name)
    {
    	this(type,name,IN);
    }
    
    public Parameter(Type type, String name, int ptype)
    {
        this.type = type;
        this.name = name;
        this.partype = ptype;
    }
    
    public int getPtype(){
    	return partype;
    }
    
    public boolean isParameterOutput(){
    	return partype == OUT;
    }
    
    /** Returns the type of this. */
    public Type getType()
    {
        return type;
    }
    
    /** Returns the name of this. */
    public String getName()
    {
        return name;
    }
    
    public String toString()
    {
    	
    	return (partype==OUT? "!":"") +  type.toString()+" "+name;
    }
    
    public Object accept(FEVisitor v){
    	return v.visitParameter(this);
    }
    
}
