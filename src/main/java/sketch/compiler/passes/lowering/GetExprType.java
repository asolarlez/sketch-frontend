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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import sketch.compiler.ast.core.FENullVisitor;
import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.NameResolver;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.SymbolTable;
import sketch.compiler.ast.core.SymbolTable.VarInfo;
import sketch.compiler.ast.core.exprs.*;
import sketch.compiler.ast.core.exprs.ExprArrayRange.RangeLen;
import sketch.compiler.ast.core.exprs.regens.ExprAlt;
import sketch.compiler.ast.core.exprs.regens.ExprChoiceBinary;
import sketch.compiler.ast.core.exprs.regens.ExprChoiceSelect;
import sketch.compiler.ast.core.exprs.regens.ExprChoiceSelect.SelectChain;
import sketch.compiler.ast.core.exprs.regens.ExprChoiceSelect.SelectField;
import sketch.compiler.ast.core.exprs.regens.ExprChoiceSelect.SelectOrr;
import sketch.compiler.ast.core.exprs.regens.ExprChoiceSelect.SelectorVisitor;
import sketch.compiler.ast.core.exprs.regens.ExprChoiceUnary;
import sketch.compiler.ast.core.exprs.regens.ExprParen;
import sketch.compiler.ast.core.exprs.regens.ExprRegen;
import sketch.compiler.ast.core.typs.NotYetComputedType;
import sketch.compiler.ast.core.typs.StructDef;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.ast.core.typs.TypeFunction;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.ast.core.typs.TypeStructRef;
import sketch.compiler.ast.cuda.exprs.CudaThreadIdx;
import sketch.compiler.ast.spmd.exprs.SpmdNProc;
import sketch.compiler.ast.spmd.exprs.SpmdPid;
import sketch.compiler.passes.lowering.SymbolTableVisitor.TypeRenamer;
import sketch.compiler.stencilSK.VarReplacer;
import sketch.util.exceptions.ExceptionAtNode;
import sketch.util.exceptions.TypeErrorException;
import sketch.util.exceptions.UnrecognizedVariableException;

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
    private NameResolver nres;
    /** This is a mechanism for a hacky type coercion for null pointers.
     * Before structs have been eliminated, nulls have type 'null'; afterwards,
     * they will usually get type 'int'. */
    private Type nullType;
    int i = 0;

    public NameResolver getNres() {
        return nres;
    }

    public GetExprType(SymbolTable symTab,
 NameResolver nres) {
        this(symTab, nres, TypePrimitive.nulltype);
    }

    public GetExprType(SymbolTable symTab,
 NameResolver nres, Type nullType)
    {
        this.symTab = nonnull(symTab);
        this.nres = nres;
        this.nullType = nullType;
    }

    public Object visitExprAlt (ExprAlt ea) {
    	Type t1 = (Type) ea.getThis().accept(this);
    	Type t2 = (Type) ea.getThat().accept(this);
        Type ret = t1.leastCommonPromotion(t2, nres);
    	
    	return ret;
    }


    public Object visitExprArrayRange(ExprArrayRange exp) {    	
    	Type base = (Type)exp.getBase().accept(this);
        if (base == null) {
            return null;
        }
    	
        if (base.equals(new NotYetComputedType()))
            return base;
		Expression expr = null;
            RangeLen range=exp.getSelection();
            Type start = (Type)range.start().accept(this);
            
            expr = range.getLenExpression();
            
        
		if(!(base instanceof TypeArray)) return null;
        // ASSERT: base is a TypeArray.

		Type baseType = ((TypeArray)base).getBase();
		
		if(range.hasLen()){
            return new TypeArray(baseType, expr, ((TypeArray) base).getMaxlength());
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
            base = TypePrimitive.bottomtype;
    	} else {
    		// otherwise, take promotion over all elements declared
    		base = (Type)((Expression)elems.get(0)).accept(this);

    		for (int i=1; i<elems.size(); i++) {
    			Type t = (Type)((Expression)elems.get(i)).accept(this);
                if (base == null) {
                    throw new ExceptionAtNode("Inconsistent types in array initializer",
                            exp);
                }
                base = base.leastCommonPromotion(t, nres);
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
        case ExprBinary.BINOP_TEQ:
             return TypePrimitive.bittype;
        }
        return binopType (exp.getOp (), exp.getLeft (), exp.getRight ());
    }

    public Object visitExprChoiceBinary (ExprChoiceBinary ecb) {
    	if (ecb.hasComparison ())
            return TypePrimitive.bittype;
    	Type t = null;
    	for (int op : ecb.opsAsExprBinaryOps ()) {
    		Type nextType = binopType (op, ecb.getLeft (), ecb.getRight ());
            t = (null == t) ? nextType : t.leastCommonPromotion(nextType, nres);
    	}
    	return t;
    }

    public Object visitExprChoiceSelect (ExprChoiceSelect ecs) {
    	class GetSelectType extends SelectorVisitor {
    		StructDef base;
    		GetSelectType (StructDef base) { this.base = base; }

    		public Object visit (SelectField sf) {
                String f = sf.getField();
                if (f.equals("")) {
                    return new NotYetComputedType();
                }
                StructDef current = base;
                boolean err = true;
                while (current.getParentName() != null) {
                    if (current.hasField(f)) {
                        err = false;
                        break;
                    } else {
                        current = nres.getStruct(current.getParentName());
                    }
                }

                return current.getType(f);
    		}

    		public Object visit (SelectOrr so) {
       			Type t1 = (Type) so.getThis ().accept (this);
    			Type t2 = (Type) so.getThat ().accept (this);
                Type rt = t1.leastCommonPromotion(t2, nres);

    			return (so.getThis ().isOptional () || so.getThat ().isOptional ()) ?
 base.leastCommonPromotion(
                        rt, nres) : rt;
    		}

    		public Object visit (SelectChain sc) {
    			StructDef oldBase = base;
    			// tf : base.first, tfn = base.first.next, tn : base.next
    			Type tf, tfn, tn = null;

    			tf = (Type) sc.getFirst ().accept (this);
    			assert tf.isStruct () : "selection on non-struct";

    			if (sc.getFirst ().isOptional ())
    				tn = (Type) sc.getNext ().accept (this);

                base = nres.getStruct(((TypeStructRef) tf).getName());
    			tfn = (Type) sc.getNext ().accept (this);
    			base = oldBase;

    			Type rt = tfn;
    			if (sc.getFirst ().isOptional ())
                    rt = rt.leastCommonPromotion(tn, nres);
    			if (sc.getNext ().isOptional ())
                    rt = rt.leastCommonPromotion(tf, nres);
    			if (sc.getNext ().isOptional () && sc.getFirst ().isOptional ())
                    rt = base.leastCommonPromotion(rt, nres);
    			return rt;
    		}
    	}

    	Type t = (Type) ecs.getObj ().accept (this);
    	ecs.assertTrue (null != t && t.isStruct (),
			"field selection of non-struct");

        StructDef base = nres.getStruct(((TypeStructRef) t).getName());
    	Type selType = (Type) ecs.accept (new GetSelectType (base));

    	return !ecs.getField ().isOptional () ? selType
 : base.leastCommonPromotion(
                selType, nres);
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


    public Object visitExprStar(ExprStar exp){
    	if(exp.getType() != null  ){
    		return exp.getType();
    	}else{
            return TypePrimitive.bottomtype;
    	}
    }

    /**
	 * Returns the type of the local variable expression based on the context.
	 */
	public Object visitExprLocalVariables(ExprLocalVariables exprLocalVariables) {
		// If the expression already has a type
		if (exprLocalVariables.getType() != null) {
			// Return that type
			return exprLocalVariables.getType();
		} else {
			// Else return a bottom type
			return TypePrimitive.bottomtype;
		}
	}

    public Object visitExprNullPtr(ExprNullPtr exp){
    	return nullType;
    }

    public Object visitExprConstChar(ExprConstChar exp)
 {
        return TypePrimitive.chartype;
    }

    public Object visitExprConstFloat(ExprConstFloat exp)
    {
        if (exp.getType() == ExprConstFloat.FloatType.Double) {
            return TypePrimitive.doubletype;
        }
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

    public Object visitExprTuple(ExprTuple exp) {
        return new TypeStructRef(exp.getName(), false);
    }

    public Object visitExprTupleAccess(ExprTupleAccess exp) {
		final ExprTupleAccess fexp = exp;

        Type base = (Type) exp.getBase().accept(this);

        if (!(base instanceof TypeStructRef))
            return null;
		final StructDef ts = nres.getStruct(((TypeStructRef) base).getName());
        assert ts != null : "Missing struct information" + base;
        int index = exp.getIndex();
		FEReplacer repVars = new FEReplacer() {
			public Object visitExprVar(ExprVar ev) {
				int idx = ts.getOrderedFields().indexOf(ev.getName());
				return new ExprTupleAccess(fexp, fexp.getBase(), idx);
			}
		};
		Type tt = ts.getType(ts.getOrderedFields().get(index));
		return (Type) tt.accept(repVars);
    }

    public Object visitExprFieldsListMacro(ExprFieldsListMacro exp) {
        return new TypeArray(exp.getType(), null);
    }

    public Object visitExprField(ExprField exp)
    {
        final ExprField fexp = exp;
        Type base = (Type)exp.getLeft().accept(this);
        StructDef ts = null;
        if (base instanceof TypeStructRef)
        {
            String name = ((TypeStructRef)base).getName();
            ts = nres.getStruct(name);
            assert ts != null : "GetExprType: missing struct information" + base;
        }
        else
        {
            throw new TypeErrorException(
                    ": You are trying to do a field access on a " + base + " in expr " +
 exp + " . " + exp, exp);
        }
        FEReplacer repVars = new FEReplacer() {
            public Object visitExprVar(ExprVar ev) {
                return new ExprField(fexp.getLeft(), ev.getName());
            }
        };
        // ADT
        if (exp.isHole()) {
            return new NotYetComputedType();
        }
        StructDef current = ts;
        while (current.getParentName() != null) {
            if (current.hasField(exp.getName())) {
                break;
            }
            current = nres.getStruct(current.getParentName());
        }
        Type ttt = current.getType(exp.getName());
        if (ttt != null) {
            return (Type) ttt.accept(repVars);
        } else {
            return null;
        }
    }


    public Object visitExprFunCall(ExprFunCall exp)
    {
    	// Has SymbolTable given us a function declaration?
    	try
    	{
            VarInfo vi = symTab.lookupVarInfo(exp.getName());
            Function fn = null;
            if (vi != null) {
                if (vi.kind == SymbolTable.KIND_LOCAL_FUNCTION) {
                    fn = (Function) vi.origin;
                } else {
                    if (vi.kind == SymbolTable.KIND_FUNC_PARAM) {
                        return NotYetComputedType.singleton;
                    } else {
                        throw new ExceptionAtNode("Function is unknown or ambiguous " + exp.getName(), exp);
                    }
                }
            } else {
                fn = nres.getFun(exp.getName(), exp);
            }
            Type retType = fn.getReturnType();
            if (!fn.getTypeParams().isEmpty() &&
                    !(fn.getReturnType() instanceof TypePrimitive))
            {
                List<Type> lt = new ArrayList<Type>();
                for (Expression ep : exp.getParams()) {
                    lt.add((Type) ep.accept(this));
                }
                TypeRenamer tr = SymbolTableVisitor.getRenaming(fn, lt, nres, null);
                retType = tr.rename(retType);
            }

            if (retType instanceof TypeStructRef) {
                TypeStructRef tsr = (TypeStructRef) retType;
                return tsr.addDefaultPkg(fn.getPkg(), nres);
            }

            if (retType instanceof TypeArray) {
                Map<String, Expression> mm = new HashMap<String, Expression>();
                Iterator<Parameter> ip = fn.getParams().iterator();
                for (Expression p : exp.getParams()) {
                    Parameter formal = ip.next();
                    mm.put(formal.getName(), p);
                }
                VarReplacer vr = new VarReplacer(mm);
                return retType.accept(vr);
            }

            return retType;
    	} catch (UnrecognizedVariableException e) {
    		// ignore
    	}


    	return null;
    }

    // TODO: deal with packages
    public Object visitExprADTHole(ExprADTHole exp) {
        return new NotYetComputedType();
    }

    public Object visitExprParen (ExprParen ep) {
    	return ep.getExpr ().accept (this);
    }

    public Object visitExprRegen (ExprRegen er) {
    	return er.getExpr ().accept (this);
    }


    public Object visitExprTernary(ExprTernary exp)
    {
        i++;
        // Do type unification on the two sides.
        // (Might not want to blindly assert ?:.)
        Type tb = (Type)exp.getB().accept(this);
        Type tc = (Type)exp.getC().accept(this);
        Type lub;
        if (tb == null) {
            assert tc != null;
            return tc;
        } else if (tc == null) {
            assert tb != null;
            return tb;
        } else if (tb.equals(new NotYetComputedType())) {
            lub = tc;
        } else if (tc.equals(new NotYetComputedType())) {
            lub = tb;
        } else {
            lub = tb.leastCommonPromotion(tc, nres);
            exp.assertTrue(lub != null, "incompatible types for '" + exp.getB() + "', '" +
                    exp.getC() + "'");
        }
        // what if both are null?

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
        Type t = expNew.getTypeToConstruct();
        if (t == null)
            return new NotYetComputedType();
        if(t instanceof TypeStructRef){
            return ((TypeStructRef)t).addDefaultPkg(nres.curPkg().getName(), nres);
        }
        return t;

    }


    public Object visitExprVar(ExprVar exp)
    {

        Type t = symTab.lookupVarNocheck(exp);
        if (t == null) {
            Function f = nres.getFun(exp.getName());
            if (f == null) {
                throw new UnrecognizedVariableException(exp.getName(), exp);
            } else {
                return TypeFunction.singleton;
            }
        } else {
            return t;
        }
    }

    public Object visitExprLambda(ExprLambda elam) {
        return TypeFunction.singleton;
    }

    @Override
    public Object visitExprNamedParam(ExprNamedParam exprNamedParam) {
        return exprNamedParam.getExpr().accept(this);
    }

	private Type binopType (int op, Expression left, Expression right) {
		Type tl = (Type) left.accept(this);
    	Type tr = (Type) right.accept(this);
        if (tl == null || tr == null) {
            return null;
        }
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
        case ExprBinary.BINOP_ADD:
        case ExprBinary.BINOP_SUB:
        case ExprBinary.BINOP_MUL:
        case ExprBinary.BINOP_DIV:
        case ExprBinary.BINOP_MOD:

            Type rv = tl.leastCommonPromotion(tr, nres);
            if (rv != null) {
                if (rv.equals(TypePrimitive.bittype)) {
                    return TypePrimitive.inttype;
                }
                return rv;
            }
        }

        // The type of the expression is some type that both sides
        // promote to, otherwise.

        if( tr == null){
        	right.accept(this);
        }

        Type rv = tl.leastCommonPromotion(tr, nres);
        
        if (rv == null &&
                (tl instanceof TypeStructRef || tr instanceof TypeStructRef) &&
                (tl == TypePrimitive.bittype || tr == TypePrimitive.bittype ||
                        tl == TypePrimitive.inttype || tr == TypePrimitive.inttype))
        {
        	return TypePrimitive.inttype;
        }


        assert rv != null : left.getCx() + ": Type ERROR: " + "The types are incompatible " + tl + " , " + tr;

        return rv;
	}
	
	@Override
	public Object visitCudaThreadIdx(CudaThreadIdx cudaThreadIdx) {
	    return TypePrimitive.inttype;
	}
	@Override
	public Object visitSpmdPid(SpmdPid pid) {
	    return TypePrimitive.inttype;
	}

    @Override
    public Object visitSpmdNProc(SpmdNProc spmdnproc) {
        return TypePrimitive.inttype;
    }
}
