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

package sketch.compiler.ast.core;
import sketch.compiler.ast.core.typs.Type;

/**
 * A formal parameter to a function. Each parameter consists of a string name, a
 * <code>Type</code>, and a integer Ptype which indicates wether the parameter is
 * input-only, output-only, or both.
 * 
 * @author Armando Solar-Lezama, derived from code by David Maze
 *         &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id$
 */
public class Parameter extends FENode implements Comparable<Parameter>
{
    // NOTE -- don't change these, or modify ScAstModel.gm in Skalch project
	public final static int IN = 0;
	public final static int OUT = 1;
	public final static int REF = 2;

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
    	super((FENode)null);
    	assert type != null;
        this.type = type;
        this.name = name;
        this.partype = ptype;
    }

    /**
     *
     * Whether the parameter is an input parameter (IN), output parameter (OUT) or a reference parameter (REF).
     *
     * @return
     */
    public int getPtype(){
    	return partype;
    }

    /**
     * Whether this parameter is an output parameter.
     * Reference parameters are implicitly output parameters.
     * @return
     */
    public boolean isParameterOutput(){
    	return partype == OUT || partype == REF;
    }

    /**
     * Whether this parameter is an input parameter.
     * The reference parameters are implicitly input parameters.
     * @return
     */
    public boolean isParameterInput(){
    	return partype == IN || partype == REF;
    }

    /** Is this parameter a reference parameter? */
    public boolean isParameterReference () {
    	return partype == REF;
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

    public String toString() {
        return (partype == OUT ? "!" : (partype == REF ? "@" : "")) + type.toString() +
                " " + name;
    }

    public Object accept(FEVisitor v){
    	return v.visitParameter(this);
    }

    public int compareTo(Parameter p) {
        return name.compareTo(p.name);
    }

}
