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

package sketch.compiler.passes.lowering;
import static sketch.util.Misc.nonnull;

import java.util.List;
import java.util.Map;

import sketch.compiler.ast.core.FENullVisitor;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.StreamType;
import sketch.compiler.ast.core.SymbolTable;
import sketch.compiler.ast.core.UnrecognizedVariableException;
import sketch.compiler.ast.core.exprs.*;
import sketch.compiler.ast.core.exprs.ExprArrayRange.RangeLen;
import sketch.compiler.ast.core.exprs.ExprChoiceSelect.SelectChain;
import sketch.compiler.ast.core.exprs.ExprChoiceSelect.SelectField;
import sketch.compiler.ast.core.exprs.ExprChoiceSelect.SelectOrr;
import sketch.compiler.ast.core.exprs.ExprChoiceSelect.SelectorVisitor;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.ast.core.typs.TypeStruct;
import sketch.compiler.ast.core.typs.TypeStructRef;

/**
 * Visitor that returns the type of an expression.  This needs to be
 * created with a symbol table to help resolve the types of variables.
 * All of the visitor methods return <code>Type</code>s.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id$
 */
public class GetExprType extends FENullVisitor
{
    private final SymbolTable symTab;
    private Map structsByName;
    /** This is a mechanism for a hacky type coercion for null pointers.
     * Before structs have been eliminated, nulls have type 'null'; afterwards,
     * they will usually get type 'int'. */
    private Type nullType;

    public GetExprType(SymbolTable symTab, StreamType streamType,
            Map structsByName) {
        this(symTab, streamType, structsByName, TypePrimitive.nulltype);
    }

    public GetExprType(SymbolTable symTab, StreamType streamType,
                       Map structsByName, Type nullType)
    {
        this.symTab = nonnull(symTab);
        this.structsByName = structsByName;
        this.nullType = nullType;
    }

    public Object visitExprAlt (ExprAlt ea) {
    	Type t1 = (Type) ea.getThis().accept(this);
    	Type t2 = (Type) ea.getThat().accept(this);
    	Type ret = t1.leastCommonPromotion(t2);
    	
    	return ret;
    }

    public Object visitExprArrayRange(ExprArrayRange exp) {    	
    	Type base = (Type)exp.getBase().accept(this);
    	
		
		Expression expr = null;
		
            RangeLen range=exp.getSelection();
            Type start = (Type)range.start().accept(this);
            
            expr = range.getLenExpression();
            
        
		if(!(base instanceof TypeArray)) return null;
        // ASSERT: base is a TypeArray.

		Type baseType = ((TypeArray)base).getBase();
		
		if(range.hasLen()){
		    return new TypeArray(baseType, expr);
		}else{
		    return baseType;
		}		
    }

    public Object visitExprArrayInit(ExprArrayInit exp)
    {
    	// want to determine these about the array
    	Type base;
    	int length;

    	// get the elements
    	List elems = exp.getElements();

    	// not sure what to do for base type if array is empty... try
    	// keeping it null --BFT
    	if (elems.size()==0) {
    		base = null;
    	} else {
    		// otherwise, take promotion over all elements declared
    		base = (Type)((Expression)elems.get(0)).accept(this);

    		for (int i=1; i<elems.size(); i++) {
    			Type t = (Type)((Expression)elems.get(i)).accept(this);
    			base = base.leastCommonPromotion(t);
    		}
    	}

    	return new TypeArray(base, new ExprConstInt(elems.size()));
    }

    public Object visitExprBinary(ExprBinary exp)
    {
        // Comparison operators always return a boolean value.
        switch(exp.getOp())
        {
        case ExprBinary.BINOP_EQ:
        case ExprBinary.BINOP_NEQ:
        case ExprBinary.BINOP_LT:
        case ExprBinary.BINOP_LE:
        case ExprBinary.BINOP_GT:
        case ExprBinary.BINOP_GE:
            return TypePrimitive.booltype;
        }
        return binopType (exp.getOp (), exp.getLeft (), exp.getRight ());
    }

    public Object visitExprChoiceBinary (ExprChoiceBinary ecb) {
    	if (ecb.hasComparison ())
    		return TypePrimitive.booltype;
    	Type t = null;
    	for (int op : ecb.opsAsExprBinaryOps ()) {
    		Type nextType = binopType (op, ecb.getLeft (), ecb.getRight ());
    		t = (null == t) ? nextType : t.leastCommonPromotion (nextType);
    	}
    	return t;
    }

    public Object visitExprChoiceSelect (ExprChoiceSelect ecs) {
    	class GetSelectType extends SelectorVisitor {
    		TypeStruct base;
    		GetSelectType (TypeStruct base) { this.base = base; }

    		public Object visit (SelectField sf) {
    			return base.getType (sf.getField ());
    		}

    		public Object visit (SelectOrr so) {
       			Type t1 = (Type) so.getThis ().accept (this);
    			Type t2 = (Type) so.getThat ().accept (this);
    			Type rt = t1.leastCommonPromotion (t2);

    			return (so.getThis ().isOptional () || so.getThat ().isOptional ()) ?
    					base.leastCommonPromotion (rt) : rt;
    		}

    		public Object visit (SelectChain sc) {
    			TypeStruct oldBase = base;
    			// tf : base.first, tfn = base.first.next, tn : base.next
    			Type tf, tfn, tn = null;

    			tf = (Type) sc.getFirst ().accept (this);
    			assert tf.isStruct () : "selection on non-struct";

    			if (sc.getFirst ().isOptional ())
    				tn = (Type) sc.getNext ().accept (this);

    			base = (tf instanceof TypeStruct) ? (TypeStruct) tf
    	    			: (TypeStruct) structsByName.get (((TypeStructRef) tf).getName ());
    			tfn = (Type) sc.getNext ().accept (this);
    			base = oldBase;

    			Type rt = tfn;
    			if (sc.getFirst ().isOptional ())
    				rt = rt.leastCommonPromotion (tn);
    			if (sc.getNext ().isOptional ())
    				rt = rt.leastCommonPromotion (tf);
    			if (sc.getNext ().isOptional () && sc.getFirst ().isOptional ())
    				rt = rt.leastCommonPromotion (base);
    			return rt;
    		}
    	}

    	Type t = (Type) ecs.getObj ().accept (this);
    	ecs.assertTrue (null != t && t.isStruct (),
			"field selection of non-struct");

    	TypeStruct base = (t instanceof TypeStruct) ? (TypeStruct) t
    			: (TypeStruct) structsByName.get (((TypeStructRef) t).getName ());
    	Type selType = (Type) ecs.accept (new GetSelectType (base));

    	return !ecs.getField ().isOptional () ? selType
    			: selType.leastCommonPromotion (base);
    }

    public Object visitExprChoiceUnary (ExprChoiceUnary ecu) {
    	Type t = (Type) ecu.getExpr().accept(this);

    	if (t.equals(TypePrimitive.bittype)) {
    		if (0 != (ecu.getOps () & ExprChoiceUnary.NOT))
    			return TypePrimitive.bittype;
    		else
    			return TypePrimitive.inttype;
    	}

    	return t;
    }

    public Object visitExprComplex(ExprComplex exp)
    {
        return TypePrimitive.cplxtype;
    }

    public Object visitExprConstBoolean(ExprConstBoolean exp)
    {
        return TypePrimitive.booltype;
    }

    public Object visitExprStar(ExprStar exp){
    	if(exp.getType() != null  ){
    		return exp.getType();
    	}else{
    		return TypePrimitive.bittype;
    	}
    }

    public Object visitExprNullPtr(ExprNullPtr exp){
    	return nullType;
    }

    public Object visitExprConstChar(ExprConstChar exp)
    {
        // return new TypePrimitive(TypePrimitive.TYPE_CHAR);
        return null;
    }

    public Object visitExprConstFloat(ExprConstFloat exp)
    {
        return TypePrimitive.floattype;
    }

    public Object visitExprConstInt(ExprConstInt exp)
    {
	// return a bit type if the value is 0 or 1
	if (exp.getVal()==0 || exp.getVal()==1) {
	    return TypePrimitive.bittype;
	} else {
	    return TypePrimitive.inttype;
	}
    }

    public Object visitExprConstStr(ExprConstStr exp)
    {
        // return new TypePrimitive(TypePrimitive.TYPE_STRING);
        return null;
    }

    public Object visitExprField(ExprField exp)
    {
        Type base = (Type)exp.getLeft().accept(this);
        // If the base is a complex type, a field of it is float.
        if (base.isComplex())
            return TypePrimitive.floattype;
        else if (base instanceof TypeStruct)
            return ((TypeStruct)base).getType(exp.getName());
        else if (base instanceof TypeStructRef)
        {
            String name = ((TypeStructRef)base).getName();
            TypeStruct str = (TypeStruct)structsByName.get(name);
            assert str != null : base;
            return str.getType(exp.getName());
        }
        else
        {
            assert false : "You are trying to do a field access on a " + base + " in expr " + exp + " . " + exp;
            return null;
        }
    }

    public Object visitExprFunCall(ExprFunCall exp)
    {
    	// Has SymbolTable given us a function declaration?
    	try
    	{
    		Function fn = symTab.lookupFn(exp.getName());
    		return fn.getReturnType();
    	} catch (UnrecognizedVariableException e) {
    		// ignore
    	}

    	// "abs" returns a float.  We should probably insert other
    	// special cases here for built-in functions, but I'm not
    	// exactly sure which ones have a constant return type and
    	// which ones are polymorphic.  --BFT
    	if (exp.getName().equals("abs")) {
    		return TypePrimitive.floattype;
    	}

    	// Otherwise, we can assume that the only function calls are
    	// calls to built-in functions in the absence of helper
    	// function support in the parser.  These by and large have a
    	// signature like
    	//
    	//   template<T> T foo(T);
    	//
    	// So, if there's any arguments, return the type of the first
    	// argument; otherwise, return float as a default.
    	List params = exp.getParams();
    	if (params.isEmpty()) {
    		return TypePrimitive.floattype;
    	}
    	return ((Expression)params.get(0)).accept(this);
    }

    public Object visitExprParen (ExprParen ep) {
    	return ep.getExpr ().accept (this);
    }

    public Object visitExprRegen (ExprRegen er) {
    	return er.getExpr ().accept (this);
    }

    public Object visitExprTernary(ExprTernary exp)
    {
        // Do type unification on the two sides.
        // (Might not want to blindly assert ?:.)
        Type tb = (Type)exp.getB().accept(this);
        Type tc = (Type)exp.getC().accept(this);
        Type lub = tb.leastCommonPromotion(tc);

        exp.assertTrue (lub != null,
        		"incompatible types for '"+ exp.getB () +"', '"+ exp.getC () +"'");
        return lub;
    }

    public Object visitExprTypeCast(ExprTypeCast exp)
    {
        return exp.getType();
    }

    public Object visitExprUnary(ExprUnary exp)
    {
        // A little more solid ground here: the type of -foo and !foo
        // will be the same as type of foo, except for bits...
	Type t = (Type)exp.getExpr().accept(this);
	// if <t> is a bit, then the unary expression could promote it
	// to an int.
	if (t.equals(TypePrimitive.bittype)) {
	    switch (exp.getOp()) {
	    case ExprUnary.UNOP_NOT: {
		// it is still a bit if it is negated, I think.
		return TypePrimitive.bittype;
	    }
	    case ExprUnary.UNOP_NEG:
	    case ExprUnary.UNOP_PREINC:
	    case ExprUnary.UNOP_POSTINC:
	    case ExprUnary.UNOP_PREDEC:
	    case ExprUnary.UNOP_POSTDEC: {
		return TypePrimitive.inttype;
	    }
	    }
	}
	return t;
    }


    public Object visitExprNew(ExprNew expNew){
    	return expNew.getTypeToConstruct();

    }


    public Object visitExprVar(ExprVar exp)
    {
        // Look this up in the symbol table.
    	Type t;
    	try{
    	    assert exp != null && symTab != null;
    		t = symTab.lookupVar(exp.getName());
    	}catch(UnrecognizedVariableException e){
    		throw new UnrecognizedVariableException(exp + ": The variable " + e.getMessage() + " has not been defined.");
    	}
        return t;
    }

	private Type binopType (int op, Expression left, Expression right) {
		Type tl = (Type) left.accept(this);
    	Type tr = (Type) right.accept(this);
        switch(op){
        case ExprBinary.BINOP_RSHIFT:
        case ExprBinary.BINOP_LSHIFT:
        	if( ! (tl instanceof TypeArray) || tr == null ){        		
        		tl = new TypeArray(tl, ExprConstInt.one);
        	}
        	if(!(((TypeArray)tl).getBase() instanceof TypePrimitive) ){
				assert false: "You can only shift arrays of primitives.";
			}
        	return tl;
        }

        // The type of the expression is some type that both sides
        // promote to, otherwise.

        if( tr == null){
        	right.accept(this);
        }

        Type rv = tl.leastCommonPromotion(tr);
        
        if(rv == null && (tl instanceof TypeStructRef || tl instanceof TypeStruct || tr instanceof TypeStruct || tr instanceof TypeStructRef) && (tl == TypePrimitive.bittype || tr == TypePrimitive.bittype ||   tl == TypePrimitive.inttype || tr == TypePrimitive.inttype)){
        	return TypePrimitive.inttype;
        }


        assert rv != null : left.getCx() + ": Type ERROR: " + "The types are incompatible " + tl + " , " + tr;

        return rv;
	}

}
