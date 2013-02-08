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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.TypeStruct;

/**
 * Similar to a namespace in C++, contains private fields (global variables local to the
 * package), structures and functions. A <code>Package</code> may or may not have a name;
 * if there is no name, this is an anonymous package. It also has a list of global
 * variable declarations (currently they are called global field declaration and are all
 * <code>FieldDecl</code> nodes), a list of function declarations (as
 * <code>Function</code> objects), and a list of <code>TypeStruct</code> definitions.
 * 
 * @author Armando Solar-Lezama from code by David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id$
 */
public class Package extends FENode
{

    private String name;
    private final List<FieldDecl> vars;
    private final List<Function> funcs;
    private final List<TypeStruct> structs;

    public String toString(){
        String res = "package " + name + '\n';
        for (TypeStruct ts : structs) {
            res += "   struct " + ts.getName() + '\n';
        }
        for(Function f: funcs){
            res += "   " + f.getSummary() + ":" + f.getCx() + '\n';
        }
        return res;
    }

    /**
     * Creates a new stream specification given its name, a list of variables, and a list
     * of functions.
     * 
     * @param context
     *            front-end context indicating file and line number for the specification
     * @param name
     *            string name of the object
     * @param vars
     *            list of <code>StmtVarDecl</code> that are fields of a filter stream
     * @param funcs
     *            list of <code>Function</code> that are member functions of the stream
     *            object
     */
    public Package(FENode context, String name,
 List<TypeStruct> structs, List vars,
            List<Function> funcs)
    {
        super(context);
        this.name = name;
        this.structs = structs;
        this.vars = vars;
        this.funcs = funcs;
    }

    /**
     * Creates a new stream specification given its name, a list of
     * variables, and a list of functions.
     *
     * @param context  front-end context indicating file and line
     *                 number for the specification
     * @param type     STREAM_* constant indicating the type of
     *                 stream object
     * @param name     string name of the object
     * @param params   list of <code>Parameter</code> that are formal
     *                 parameters to the stream object
     * @param vars     list of <code>StmtVarDecl</code> that are
     *                 fields of a filter stream
     * @param funcs    list of <code>Function</code> that are member
     *                 functions of the stream object
     * @deprecated
     */
    public Package(FEContext context, String name,
 List<TypeStruct> structs,
            List vars, List<Function> funcs)
    {
        super(context);

        this.name = name;

        this.structs = structs;
        this.vars = vars;
        this.funcs = funcs;
    }



    



    /**
     * Returns the name of this, or null if this is an anonymous stream.
     *
     * @return  string name of the object, or null for an anonymous stream
     */
    public String getName()
    {
        return name;
    }



    /**
     * Returns the field variables declared in this, as a list of
     * Statements.  Each of the statements will probably be a
     * {@link StmtVarDecl}.
     *
     * @return  list of {@link Statement}
     */
    public List<FieldDecl> getVars()
    {
        return vars;
    }

    /**
     * Returns the functions declared in this, as a list of Functions.
     *
     * @return  list of {@link Function}
     */
    public List<Function> getFuncs()
    {
        return funcs;
    }




    /** Returns the list of structures declared in this. */
    public List<TypeStruct> getStructs() {
        return structs;
    }

    /**
     * Accept a front-end visitor.
     *
     * @param v  front-end visitor to accept
     * @return   object returned from the visitor
     * @see      FEVisitor#visitStreamSpec
     */
    public Object accept(FEVisitor v)
    {
        return v.visitStreamSpec(this);
    }

    public Package newFromFcns(List<Function> fcns) {
        return new Package(this, this.getName(),
                structs, this.getVars(), Collections.unmodifiableList(fcns));
    }

    public Package merge(Package s2) {
        assert this.getName().equals(s2.getName());
        List<TypeStruct> ns = new ArrayList<TypeStruct>(structs);
        ns.addAll(s2.structs);
        List<FieldDecl> fd = new ArrayList<FieldDecl>(vars);
        fd.addAll(s2.vars);
        List<Function> fs = new ArrayList<Function>(funcs);
        fs.addAll(s2.funcs);
        return new Package(this, name, ns, fd, fs);
    }

    public Package newFromFcns(List<Function> fcns, List<TypeStruct> structs) {
        return new Package(this, this.getName(),
                structs, this.getVars(),
                Collections.unmodifiableList(fcns));
    }
}
