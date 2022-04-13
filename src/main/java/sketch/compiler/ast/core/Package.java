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
import sketch.compiler.ast.core.stmts.StmtSpAssert;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.StructDef;

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
    private final List<StructDef> structs;
    private final List<StmtSpAssert> specialAsserts;


    public String toString(){
        String res = "package " + name + '\n';
        for (StructDef ts : structs) {
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
 List<StructDef> structs, List vars,
            List<Function> funcs, List<StmtSpAssert> specialAsserts)
    {
        super(context);
        this.name = name;
        this.structs = structs;
        this.vars = vars;
        this.funcs = funcs;
        this.specialAsserts = specialAsserts;
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
 List<StructDef> structs,
 List vars,
            List<Function> funcs, List<StmtSpAssert> specialAsserts)
    {
        super(context);

        this.name = name;

        this.structs = structs;
        this.vars = vars;
        this.funcs = funcs;
        this.specialAsserts = specialAsserts;
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
    public List<StructDef> getStructs() {
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
        return v.visitPackage(this);
    }

    public Package newFromFcns(List<Function> fcns) {
        return new Package(this, this.getName(),
 structs, this.getVars(),
                Collections.unmodifiableList(fcns), specialAsserts);
    }

    public Package merge(Package s2) {
        assert this.getName().equals(s2.getName());
        List<StructDef> ns = new ArrayList<StructDef>(structs);
        ns.addAll(s2.structs);
        List<FieldDecl> fd = new ArrayList<FieldDecl>(vars);
        fd.addAll(s2.vars);
        List<Function> fs = new ArrayList<Function>(funcs);
        fs.addAll(s2.funcs);
        List<StmtSpAssert> sa = new ArrayList<StmtSpAssert>(specialAsserts);
        sa.addAll(s2.specialAsserts);
        return new Package(this, name, ns, fd, fs, sa);
    }

    public Package newFromFcns(List<Function> fcns, List<StructDef> structs) {
        return new Package(this, this.getName(),
                structs, this.getVars(),
                Collections.unmodifiableList(fcns), specialAsserts);
    }

    public List<StmtSpAssert> getSpAsserts() {
        return specialAsserts;
    }

	public static class PackageCreator {

		private FENode context;
		private String name;
		private List<FieldDecl> vars;
		private List<Function> funcs;
		private List<StructDef> structs;
		private List<StmtSpAssert> specialAsserts;

		PackageCreator(Package source) {
			this.context = source.getOrigin(); // no need to clone
			this.name = source.name; // no need to clone
			this.vars = new ArrayList<FieldDecl>(source.vars);
			this.funcs = new ArrayList<Function>(source.funcs);
			this.structs = new ArrayList<StructDef>(source.structs);
			this.specialAsserts = new ArrayList<StmtSpAssert>(source.specialAsserts);
		}

		public PackageCreator add_funcion(Function new_function)
		{
			funcs.add(new_function);
			return this;
		}

		public Package create() {
			return new Package(context, name, structs, vars, funcs, specialAsserts);
		}
	}

	public PackageCreator creator() {
		return new PackageCreator(this);
	}
}
