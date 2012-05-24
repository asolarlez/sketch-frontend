/**
 *
 */
package sketch.compiler.passes.lowering;
import java.util.ArrayList;
import java.util.List;

import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.SymbolTable;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.ExprArrayInit;
import sketch.compiler.ast.core.exprs.ExprArrayRange;
import sketch.compiler.ast.core.exprs.ExprArrayRange.RangeLen;
import sketch.compiler.ast.core.exprs.ExprBinary;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprConstant;
import sketch.compiler.ast.core.exprs.ExprStar;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssert;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtBlock;
import sketch.compiler.ast.core.stmts.StmtFor;
import sketch.compiler.ast.core.stmts.StmtIfThen;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;

import static sketch.util.DebugOut.assertFalse;

/**
 * Rewrites array types and array accesses so that we are left with
 * one-dimensional arrays only.  E.g.:
 * <code>
 * bit[2][4] x = 0;
 * bit y = x[1][2];
 *
 * // is rewritten to
 *
 * bit[2*4] x = 0;
 * bit y = x[1*4 + 2];
 * </code>
 *
 * @author Chris Jones
 *
 */
public class EliminateMultiDimArrays extends SymbolTableVisitor {
    TempVarGen varGen;
	public EliminateMultiDimArrays (TempVarGen varGen) {	    
		super (null);
		this.varGen = varGen;
	}
	
	public Object visitExprArrayInit(ExprArrayInit eai){
	    Type ta = getType(eai);
	    if(!(ta instanceof TypeArray )){ return eai; }
	    TypeArray taar = (TypeArray)ta;
	    if(!(taar.getBase() instanceof TypeArray )){ return eai; }
	    String nv = varGen.nextVar();
	    addStatement((Statement)new StmtVarDecl(eai, ta, nv, null).accept(this));
	    int i=0; 
	    for(Expression e : eai.getElements()){
	        addStatement((Statement)new StmtAssign(new ExprArrayRange(new ExprVar(eai, nv), ExprConstInt.createConstant(i)), e).accept(this));
	        i++;
	    }
	    return new ExprVar(eai, nv);
	}
	
	public Object visitStmtAssign(StmtAssign sa){
	    Type ta = getType(sa.getLHS());
	    Type tb = getType(sa.getRHS());
	    if(ta.equals(tb)){
	        return super.visitStmtAssign(sa);
	    }
	    if(!(ta instanceof TypeArray)){
	        return super.visitStmtAssign(sa);
	    }
	    TypeArray taar = (TypeArray)ta;
	    if(!(taar.getBase() instanceof TypeArray)){
            return super.visitStmtAssign(sa);
        }
	    Expression tblen;
	    if(tb instanceof TypeArray){
	        tblen = ((TypeArray)tb).getLength();
	    }else{
	        tblen = ExprConstInt.one;
	    }
	    String nv = varGen.nextVar();	    
	    String rhv = varGen.nextVar();
	    
	    addStatement((Statement)new StmtVarDecl(sa.getRHS(), tb, rhv, null).accept(this));
	    Expression rhexp = new ExprVar(sa.getRHS(), rhv);
	    addStatement((Statement) new StmtAssign(rhexp, sa.getRHS()).accept(this));
	     
	    
	    StmtAssign nas;
	    if(tb instanceof TypeArray){
	        nas = new StmtAssign(
	            new ExprArrayRange(sa.getLHS(), new ExprVar(sa, nv)),
	            new ExprArrayRange(rhexp, new ExprVar(sa, nv)));
	    }else{
	        nas = new StmtAssign(
	                new ExprArrayRange(sa.getLHS(), new ExprVar(sa, nv)),
	                rhexp);
	    }
	    StmtAssign nasdef = new StmtAssign(
                new ExprArrayRange(sa.getLHS(), new ExprVar(sa, nv)),
                taar.defaultValue());
	    	            
	    StmtIfThen sit = new StmtIfThen(sa, new ExprBinary(new ExprVar(sa, nv), "<", tblen), 
	            new StmtBlock(nas), 
	            new StmtBlock(nasdef)
	    );
	    return new StmtFor(nv, taar.getLength(), new StmtBlock(sit) ).accept(this);
	    
	}
	
    @Override
    public Object visitExprStar(ExprStar star) {
        Type ts = star.getType();
        if (ts instanceof TypeArray) {
            TypeArray tsa = (TypeArray) ts;
            List<Expression> dims = tsa.getDimensions();
            if (dims.size() > 1) {
                int total = 1;
                for (Expression e : dims) {
                    if (!(e instanceof ExprConstInt)) {
                        assertFalse("dimension variable not exprconstint type");
                    }
                    total *= ((ExprConstInt) e).getVal();
                }
                star.setType(new TypeArray(tsa.getAbsoluteBase(), new ExprConstInt(total)));
                return star;
            }
        }
        return super.visitExprStar(star);
    }

	/** Flatten multi-dimensional array types */
	public Object visitTypeArray (TypeArray t) {
		TypeArray ta = (TypeArray) super.visitTypeArray (t);

		if (ta.getBase ().isArray ()) {
			TypeArray base = (TypeArray) ta.getBase ();
			List<Expression> dims = base.getDimensions ();

			dims.add (ta.getLength ());
			return new TypeArray (base.getBase (),
				new ExprBinary (base.getLength (),
								ExprBinary.BINOP_MUL,
								base.getLength (),
								ta.getLength ()),
					dims,
                    ta.getMaxlength() * base.getMaxlength());
		} else {
			return ta;
		}
	}

	public Object visitExprArrayRange (ExprArrayRange ear) {
		// Do the recursive rewrite
		Expression base = 			doExpression (ear.getAbsoluteBaseExpr ());
		List<RangeLen> oldIndices = ear.getArraySelections ();
		List<RangeLen> indices =	new ArrayList<RangeLen> ();

		for (RangeLen rl : oldIndices) {
			Expression newStart = doExpression (rl.start ());
			Expression newLen = doExpression(rl.getLenExpression());
			if(newLen != rl.getLenExpression() || newStart != rl.start ())
				rl = new RangeLen (newStart, newLen);
			indices.add(rl);
		}
		
		Type xt = getType (ear.getAbsoluteBaseExpr ());
		List<Expression> dims = xt instanceof TypeArray? 
			((TypeArray) xt).getDimensions () : null;
		if (dims ==null || (1 == dims.size () && indices.size() == 1 )) {
			return new ExprArrayRange (ear, base, indices.get (0));
		}

		// And then flatten indexes involving multi-dim arrays
		if (false == isSupportedIndex (indices)) {
			ear.report ("sorry, slices like x[0::2][2] are not supported");
			throw new RuntimeException ();
		}

		Expression idx = ExprConstant.createConstant (ear, "0");
		for (RangeLen rl : indices) {
            Expression cond =
                    new ExprBinary(new ExprBinary(rl.start(), ">=", ExprConstInt.zero),
                            "&&", new ExprBinary(rl.start(), "<",
                                    dims.get(dims.size() - 1)));
            addStatement(new StmtAssert(cond, idx.getCx() + ": Array out of bounds",
                    false));
			dims.remove (dims.size ()-1);
			idx = new ExprBinary (idx, ExprBinary.BINOP_ADD,
					idx,
					new ExprBinary (idx,
									ExprBinary.BINOP_MUL,
					 	  		    product (dims, idx),
					 	  		    rl.start ()));
		}
		Expression lastLen = indices.get (indices.size ()-1).getLenExpression(); 
		Expression size = null;
		if(lastLen != null){
			size = new ExprBinary (idx, ExprBinary.BINOP_MUL,
			        lastLen,
					product (dims, idx));
		}else{
		    if(dims.size() > 0){
		        size = product (dims, idx);
		    }
		}
		RangeLen flatRl = new RangeLen (idx, size);

		return new ExprArrayRange (ear, base, flatRl);
	}

	/**
	 * Return an Expression representing the product of the elements in EXPRS.
	 *
	 * TODO: refactor to somewhere else
	 */
	private Expression product (List<Expression> exprs, FENode cx) {
		Expression prod = ExprConstant.createConstant (cx, "1");
		for (Expression e : exprs)
			prod = new ExprBinary (prod, ExprBinary.BINOP_MUL, prod, e);
		return prod;
	}
	
	
	   @Override
	    public Object visitParameter(Parameter par){
	        Type t = (Type) par.getType().accept(this);

	        symtab.registerVar(par.getName(),
	                actualType(par.getType()),
	                par,
	                SymbolTable.KIND_FUNC_PARAM);
	        
	        if( t == par.getType()){
	            return par;
	        }else{
	            return new Parameter(t, par.getName(), par.getPtype() );
	        }
	    }
	

	/**
	 * Currently, we only support multi-dimensional indexes of the following
	 * forms:
	 *
	 * T[1][2]... arr;  arr[0];
	 * T[1]...[N] arr;  arr[0][0];
	 * T[1]...[N] arr;  arr[0][0::2];
	 *
	 * This function returns true iff INDICES represent one of those forms.
	 */
	private boolean isSupportedIndex (List<RangeLen> indices) {
		for (int i = 0; i < (indices.size () - 1); ++i)
			if (indices.get (i).hasLen())
				return false;
		return true;
	}
}

