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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import streamit.frontend.nodes.ExprArrayRange.Range;
import streamit.frontend.nodes.ExprArrayRange.RangeLen;

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
    private SymbolTable symTab;
    private StreamType streamType;
    private Map structsByName;

    public GetExprType(SymbolTable symTab, StreamType streamType,
                       Map structsByName)
    {
        this.symTab = symTab;
        this.streamType = streamType;
        this.structsByName = structsByName;
    }

    public Object visitExprArrayRange(ExprArrayRange exp) {
    	assert exp.getMembers().size()==1 : "Array Range expressions not yet implemented; check "+exp+" at "+exp;
    	Type base = (Type)exp.getBase().accept(this);

		List l=exp.getMembers();
		Expression expr = null;
		for(int i=0;i<l.size();i++) {
			Object obj=l.get(i);
			if(obj instanceof Range) {
				Range range = (Range) obj;
				Type start = (Type)((Range) obj).start().accept(this);
				Type end = (Type)((Range) obj).end().accept(this);
				if(expr == null){
					expr = new ExprBinary(exp, ExprBinary.BINOP_SUB, range.end(), range.start());
				}else{
					expr = new ExprBinary(exp, ExprBinary.BINOP_ADD, expr,
							new ExprBinary(exp, ExprBinary.BINOP_SUB, range.end(), range.start())
					);
				}
			}
			else if(obj instanceof RangeLen) {
				RangeLen range=(RangeLen) obj;
				Type start = (Type)range.start().accept(this);
				if(expr == null){
					expr = new ExprConstInt(range.len());
				}else{
					expr = new ExprBinary(exp, ExprBinary.BINOP_ADD, expr, new ExprConstInt(range.len()));
				}
			}
		}
		if(!(base instanceof TypeArray)) return null;
        // ASSERT: base is a TypeArray.

		Type baseType = ((TypeArray)base).getBase();



		if(expr.getIValue() == 1){
			return baseType;
		}else{
			return new TypeArray(baseType, expr);
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
		base = t.leastCommonPromotion(t);
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
        Type tl = (Type)exp.getLeft().accept(this);
    	Type tr = (Type)exp.getRight().accept(this);
        switch(exp.getOp()){
        case ExprBinary.BINOP_RSHIFT:
        case ExprBinary.BINOP_LSHIFT:
        	if( ! (tl instanceof TypeArray) || tr == null ){
        		if( !(exp.getLeft() instanceof ExprConstInt )){
        			assert false : "You can only do shift on an array or a constant for now.";
        		}
        		
        	}
        	return tl;
        }

        // The type of the expression is some type that both sides
        // promote to, otherwise.

        if( tr == null){
        	exp.getRight().accept(this);
        }

        Type rv = tl.leastCommonPromotion(tr);

        assert rv != null : "Type ERROR: " + "The types are incompatible " + tl + " , " + tr;

        return rv;
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
    	return TypePrimitive.nulltype;
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

    public Object visitExprPeek(ExprPeek exp)
    {
        return streamType.getIn();
    }

    public Object visitExprPop(ExprPop exp)
    {
        return streamType.getIn();
    }

    public Object visitExprTernary(ExprTernary exp)
    {
        // Do type unification on the two sides.
        // (Might not want to blindly assert ?:.)
        Type tb = (Type)exp.getB().accept(this);
        Type tc = (Type)exp.getC().accept(this);
        return tb.leastCommonPromotion(tc);
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
    	return expNew.typeToConstruct;

    }


    public Object visitExprVar(ExprVar exp)
    {
        // Look this up in the symbol table.
    	Type t;
    	try{
    		t = symTab.lookupVar(exp.getName());
    	}catch(UnrecognizedVariableException e){
    		throw new UnrecognizedVariableException(exp + ": The variable " + e.getMessage() + " has not been defined.");
    	}
        return t;
    }
}
