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
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypePrimitive;

/**
 * A function declaration in a StreamIt program.  This may be an init
 * function, work function, helper function, or message handler.  A
 * function has a class (one of the above), an optional name, an
 * optional parameter list, a return type (void for anything other
 * than helper functions), and a body.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id$
 */
public class Function extends FENode
{
    // Classes:
    
    public enum FuncType{
     FUNC_HELPER, // Used as Generator. Also helper functions for PROMELA.
     FUNC_BUILTIN_HELPER,  // Used for SMT.
     FUNC_UNINTERP, // Uninterpreted Function
     FUNC_ASYNC, //Async functions used in promela to model forks.
     FUNC_STATIC, // Static function, non-generator.
     FUNC_SKETCHMAIN,  // Harness function. Also static.
     FUNC_STENCIL
    }

    private EnumSet<FuncType> cls;
    private String name; // or null
    private Type returnType;
    private List<Parameter> params;
    private Statement body;    
    private String fImplements = null;

    /** Internal constructor to create a new Function from all parts.
     * This is public so that visitors that want to create new objects
     * can, but you probably want one of the other creator functions. */
    public Function(FENode context, FuncType cls, String name, Type returnType,
            List<Parameter> params, Statement body)
    {
        this(context, EnumSet.of(cls), name, returnType, params, null, body);
    }
    public Function(FENode context, EnumSet<FuncType>  cls, String name, Type returnType,
            List<Parameter> params, Statement body)
    {
        this(context, cls, name, returnType, params, null, body);
    }

    /**
     * Internal constructor to create a new Function from all parts. This is public so
     * that visitors that want to create new objects can, but you probably want one of the
     * other creator functions.
     * 
     * @deprecated
     */
    public Function(FEContext context, FuncType cls, String name, Type returnType,
            List<Parameter> params, Statement body)
    {
        this(context, EnumSet.of(cls), name, returnType, params, null, body);
    }
    public Function(FEContext context, EnumSet<FuncType> cls, String name, Type returnType,
            List<Parameter> params, Statement body)
    {
        this(context, cls, name, returnType, params, null, body);
    }

    public Function(FENode context, EnumSet<FuncType> cls, String name, Type returnType,
            List<Parameter> params, String fImplements, Statement body)
    {
        super(context);
        this.cls = cls;
        this.name = name;
        this.returnType = returnType;
        this.params = params;
        this.body = body;
        this.fImplements = fImplements;
    }
    public Function(FENode context, FuncType cls, String name, Type returnType,
            List<Parameter> params, String fImplements, Statement body)
    {
        this(context, EnumSet.of(cls), name, returnType, params, fImplements, body);
    }

    @SuppressWarnings("deprecation")
    private Function(FEContext context, EnumSet<FuncType> cls, String name, Type returnType,
            List<Parameter> params, String fImplements, Statement body)
    {
        super(context);
        this.cls = cls;
        this.name = name;
        this.returnType = returnType;
        this.params = params;
        this.body = body;
        this.fImplements = fImplements;
    }
    @SuppressWarnings("deprecation")
    private Function(FEContext context, FuncType cls, String name, Type returnType,
            List<Parameter> params, String fImplements, Statement body)
    {
        this(context, EnumSet.of(cls), name, returnType, params, fImplements, body);
    }
    

    public static Function newUninterp(String name, List<Parameter> params){
    	return new Function((FEContext) null, FuncType.FUNC_UNINTERP, name,TypePrimitive.voidtype , params, null);
    }

    public static Function newUninterp(String name, Type rettype, List<Parameter> params){
    	return new Function((FEContext) null, FuncType.FUNC_UNINTERP, name,rettype, params, null);
    }
    public static Function newUninterp(FENode cx, String name, Type rettype, List<Parameter> params){
    	return new Function(cx, FuncType.FUNC_UNINTERP, name,rettype, params, null);
    }
    /**
     *
     * @param cx
     * @param name
     * @param rettype
     * @param params
     * @return
     * @deprecated
     */
    public static Function newUninterp(FEContext cx, String name, Type rettype, List<Parameter> params){
    	return new Function(cx, FuncType.FUNC_UNINTERP, name,rettype, params, null);
    }




    /** Create a new helper function given its parts. */
    public static Function newHelper(FENode context, String name,
                                     Type returnType, List<Parameter> params,
                                     Statement body)
    {
        return new Function(context, FuncType.FUNC_HELPER, name, returnType,
                            params, body);
    }

    /** Create a new helper function given its parts.
     * @deprecated
     */
    public static Function newHelper(FEContext context, String name,
                                     Type returnType, List<Parameter> params,
                                     Statement body)
    {
        return new Function(context, FuncType.FUNC_HELPER, name, returnType,
                            params, body);
    }





    /** Create a new helper function given its parts. */
    public static Function newHelper(FENode context, String name,
                                     Type returnType, List<Parameter> params,
                                     String impl, Statement body)
    {
        Function f=new Function(context, FuncType.FUNC_HELPER, name, returnType,
                            params, body);
        f.fImplements=impl;
        return f;
    }

    /** Create a new helper function given its parts.
     * @deprecated
     */
    public static Function newHelper(FEContext context, String name,
                                     Type returnType, List<Parameter> params,
                                     String impl, Statement body)
    {
        Function f=new Function(context, FuncType.FUNC_HELPER, name, returnType,
                            params, body);
        f.fImplements=impl;
        return f;
    }



    /** Create a new helper function given its parts. */
    public static Function newStatic(FENode context, String name,
                                     Type returnType, List<Parameter> params,
                                     String impl, Statement body)
    {
        Function f=new Function(context, FuncType.FUNC_STATIC, name, returnType,
                            params, body);
        f.fImplements=impl;
        return f;
    }

    /** Create a new helper function given its parts. */
    public static Function newHarnessMain(FEContext context, String name, Type returnType,
            List<Parameter> params, Statement body)
    {
        return new Function(context, EnumSet.of(FuncType.FUNC_SKETCHMAIN, FuncType.FUNC_STATIC), name, returnType, params, null,
                body);
    }

    /**
     * Create a new helper function given its parts.
     * 
     * @deprecated
     */
    public static Function newStatic(FEContext context, String name, Type returnType,
            List<Parameter> params, String fImplements, Statement body)
    {
        return new Function(context, FuncType.FUNC_STATIC, name, returnType, params, fImplements,
                body);
    }

    public boolean isUninterp(){
    	return cls.contains(FuncType.FUNC_UNINTERP);
    }

    public boolean isStatic(){
    	return cls.contains(FuncType.FUNC_STATIC);
    }


    /** Returns the class of this function as an integer. */
    public EnumSet<FuncType> getCls()
    {
        return cls;
    }

    /** Returns the name of this function, or null if it is anonymous. */
    public String getName()
    {
        return name;
    }

    /** Returns the parameters of this function, as a List of Parameter
     * objects. */
    public List<Parameter> getParams()
    {
        return Collections.unmodifiableList(params);
    }

    /** Returns the return type of this function. */
    public Type getReturnType()
    {
        return returnType;
    }

    /** Returns the body of this function, as a single statement
     * (likely a StmtBlock). */
    public Statement getBody()
    {
        return body;
    }

    /**
     * Returns the specification for this function. May be null, meaning this is
     * a spec or an unbound sketch.
     */
    public String getSpecification()
    {
    	return fImplements;
    }

    /** Accepts a front-end visitor. */
    public Object accept(FEVisitor v)
    {
        return v.visitFunction(this);
    }

    public String toString() {
        final String typ =
                (this.isStatic() ? "" : (this.isSketchHarness() ? "harness "
                        : "generator "));
        final String impl = fImplements != null ? " implements " + fImplements : "";
        return typ + returnType + " " + name + "(" + params + ")" + impl;
    }

    public void makeStencil(){
        cls.add(FuncType.FUNC_STENCIL);
    }
    
    public boolean isStencil(){
        return cls.contains(FuncType.FUNC_STENCIL);
    }
    public int hashCode(){
        return name.hashCode();
    }
    public boolean equals(Object o){
        if(o instanceof Function){
        return name.equals(((Function)o).getName());
        }
        return false;
    }

    /** is a main function */
    public boolean isSketchHarness() {
        return cls.contains(FuncType.FUNC_SKETCHMAIN);
    }
}
