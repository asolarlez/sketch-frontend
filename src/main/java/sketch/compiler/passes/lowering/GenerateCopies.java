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

package streamit.frontend.passes;

import java.util.ArrayList;
import java.util.List;

import streamit.frontend.nodes.ExprArray;
import streamit.frontend.nodes.ExprArrayInit;
import streamit.frontend.nodes.ExprArrayRange;
import streamit.frontend.nodes.ExprBinary;
import streamit.frontend.nodes.ExprConstInt;
import streamit.frontend.nodes.ExprField;
import streamit.frontend.nodes.ExprPeek;
import streamit.frontend.nodes.ExprPop;
import streamit.frontend.nodes.ExprStar;
import streamit.frontend.nodes.ExprTernary;
import streamit.frontend.nodes.ExprVar;
import streamit.frontend.nodes.Expression;
import streamit.frontend.nodes.FEContext;
import streamit.frontend.nodes.FEReplacer;
import streamit.frontend.nodes.Statement;
import streamit.frontend.nodes.StmtAssign;
import streamit.frontend.nodes.StmtBlock;
import streamit.frontend.nodes.StmtFor;
import streamit.frontend.nodes.StmtLoop;
import streamit.frontend.nodes.StmtPush;
import streamit.frontend.nodes.StmtVarDecl;
import streamit.frontend.nodes.SymbolTable;
import streamit.frontend.nodes.TempVarGen;
import streamit.frontend.nodes.Type;
import streamit.frontend.nodes.TypeArray;
import streamit.frontend.nodes.TypePrimitive;
import streamit.frontend.nodes.TypeStruct;
import streamit.frontend.nodes.TypeStructRef;
import streamit.frontend.nodes.ExprArrayRange.Range;
import streamit.frontend.nodes.ExprArrayRange.RangeLen;

/**
 * Generate code to copy structures and arrays elementwise.  In StreamIt,
 * assigning one composite object to another copies all of its members
 * (there are no references); this pass makes that copying explicit.
 * It also generates temporary variables for push, pop, and peek
 * statements to ensure that languages with references do not see
 * false copies.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id$
 */


class UpgradeStarToInt extends FEReplacer{
	private final SymbolTableVisitor stv;
	boolean upToInt;
	UpgradeStarToInt(SymbolTableVisitor stv, boolean upToInt){		
		this.stv = stv;
		this.upToInt = upToInt;
	}
	
	public Object visitExprStar(ExprStar star) {
		if(upToInt){
			star.setSize(5);
		}else{
			star.setSize(1);
		}
		return star;
	}
	
    public Object visitExprTernary(ExprTernary exp)
    {
    	boolean oldBit = upToInt;
    	upToInt = false;
        Expression a = doExpression(exp.getA());
        upToInt = oldBit;
        Expression b = doExpression(exp.getB());
        Expression c = doExpression(exp.getC());
        if (a == exp.getA() && b == exp.getB() && c == exp.getC())
            return exp;
        else
            return new ExprTernary(exp.getContext(), exp.getOp(), a, b, c);
    }
    public Object visitExprBinary(ExprBinary exp)
    {		
		switch(exp.getOp()){
        case ExprBinary.BINOP_GE:
        case ExprBinary.BINOP_GT:
        case ExprBinary.BINOP_LE:
        case ExprBinary.BINOP_LT:{
        	boolean oldBit = upToInt;
        	upToInt = true;
        	Expression left = doExpression(exp.getLeft());
            Expression right = doExpression(exp.getRight());
            upToInt = oldBit;
            if (left == exp.getLeft() && right == exp.getRight())
                return exp;
            else
                return new ExprBinary(exp.getContext(), exp.getOp(), left, right);
        }
        default:
        	return super.visitExprBinary(exp);
		}		
    }
    public Object visitStmtLoop(StmtLoop stmt)
    {
    	boolean oldBit = upToInt;
    	upToInt = true;
        Expression newIter = doExpression(stmt.getIter());
        upToInt = oldBit;        
        Statement newBody = (Statement)stmt.getBody().accept(this);
        if (newIter == stmt.getIter() && newBody == stmt.getBody())
            return stmt;
        return new StmtLoop(stmt.getContext(), newIter, newBody);
    }
    
    public Object visitExprArray(ExprArray exp){
    	boolean oldBit = upToInt;
    	upToInt = true;
    	Expression offset = doExpression(exp.getOffset());
        upToInt = oldBit;    	    	
    	Expression base = doExpression(exp.getBase());        
        if (base == exp.getBase() && offset == exp.getOffset())
            return exp;
        else
            return new ExprArray(exp.getContext(), base, offset);
    }
    public Object visitExprArrayRange(ExprArrayRange exp){
    	boolean change=false;
		Expression newBase=doExpression(exp.getBase());
		if(newBase!=exp.getBase()) change=true;
		List l=exp.getMembers();
		List<Object> newList=new ArrayList<Object>();
		for(int i=0;i<l.size();i++) {
			Object obj=l.get(i);
			if(obj instanceof Range) {
				Expression newStart = null;
				Expression newEnd = null;
				Range range=(Range) obj;
				{				
					boolean oldBit = upToInt;
			    	upToInt = true;
					newStart=doExpression(range.start);
					upToInt = oldBit;
				}
				{					
					boolean oldBit = upToInt;
			    	upToInt = true;
			    	newEnd=doExpression(range.end);
			    	upToInt = oldBit;
				}
				newList.add(new Range(newStart,newEnd));
				if(newStart!=range.start) change=true;
				else if(newEnd!=range.end) change=true;
			}
			else if(obj instanceof RangeLen) {
				RangeLen range=(RangeLen) obj;
				Expression newStart = null;
				{				
					boolean oldBit = upToInt;
			    	upToInt = true;
			    	newStart=doExpression(range.start);
			    	upToInt = oldBit;
				}
				newList.add(new RangeLen(newStart,range.len));
				if(newStart!=range.start) change=true;
			}
		}
		if(!change) return exp;
		return new ExprArrayRange(newBase,newList);
    }
}






class Indexify extends FEReplacer{
	private final Expression index;
	private final GenerateCopies stv;
	public final List<Statement> postStmts;
	public final List<Statement> preStmts;
	
	Indexify(Expression index, GenerateCopies stv){
		this.index = index;
		this.stv = stv;		
		this.postStmts = new ArrayList<Statement>();
		this.preStmts = new ArrayList<Statement>();
	}
	public Object visitExprVar(ExprVar exp) {
		if ( stv.getType(exp) instanceof TypeArray)
			return new ExprArray(exp.getContext(), exp, index);
		else
			return exp;
	}
	
	public Object visitExprArray(ExprArray exp){
		//Since arrays are all one dimensional, 
		//This will be of type int, but just in case
		//as a precausion.
		if ( stv.getType(exp) instanceof TypeArray)
			return new ExprArray(exp.getContext(), exp, index);
		else
			return exp;
    }
	
	public Object visitExprArrayRange(ExprArrayRange exp){
		//Since arrays are all one dimensional, 
		//This will be of type int, but just in case
		//as a precausion.
		if ( stv.getType(exp) instanceof TypeArray)
			return new ExprArray(exp.getContext(), exp, index);
		else
			return exp;
    }	
	
	
	public Expression handleBitShift(ExprBinary exp){
		{
			assert exp.getOp() == ExprBinary.BINOP_LSHIFT || exp.getOp() == ExprBinary.BINOP_RSHIFT : "This should never happen!!";
			Type lType = stv.getType(exp.getLeft());
			Type rType = stv.getType(exp.getRight());
			Expression result;
			if(rType.isNonDet()){
				FEContext context = exp.getContext();
				String newVarName = this.stv.addNewDeclaration(TypePrimitive.inttype, exp.getRight());								
				ExprVar oldRHS = new ExprVar(context, newVarName);
				Expression newIdx = null;
				if( exp.getOp() == ExprBinary.BINOP_LSHIFT ){
					newIdx = new ExprBinary(context, ExprBinary.BINOP_ADD, index, oldRHS );					
				}else{
					newIdx = new ExprBinary(context, ExprBinary.BINOP_SUB, index, oldRHS);
				}
				Indexify indexify = new Indexify(newIdx, stv );
				Expression newVal = (Expression) exp.getLeft().accept(indexify);
				this.preStmts.addAll(indexify.preStmts);
				this.postStmts.addAll(indexify.postStmts);			
				result = newVal;								
			}else{
				FEContext context = exp.getContext();
				String newVarName = this.stv.addNewDeclaration(TypePrimitive.inttype, exp.getRight());								
				ExprVar oldRHS = new ExprVar(context, newVarName);
				Expression newIdx = null;
				int op;
				Expression newConst = null;
				if( exp.getOp() == ExprBinary.BINOP_LSHIFT ){
					newIdx = new ExprBinary(context, ExprBinary.BINOP_ADD, index, oldRHS );
					op = ExprBinary.BINOP_LT;
					TypeArray ta = (TypeArray) lType;					
					newConst = ta.getLength();
				}else{
					newIdx = new ExprBinary(context, ExprBinary.BINOP_SUB, index, oldRHS);
					op = ExprBinary.BINOP_GE;
					newConst = new ExprConstInt(context, 0);					
				}
				Indexify indexify = new Indexify(newIdx, stv );
				Expression newVal = (Expression) exp.getLeft().accept(indexify);
				this.preStmts.addAll(indexify.preStmts);
				this.postStmts.addAll(indexify.postStmts);			
				result = new ExprTernary(context,
						ExprTernary.TEROP_COND, 
						new ExprBinary(context, op, newIdx, newConst ),
						newVal,
						new ExprConstInt(context, 0)
				);
			}
			return 	result;
		}			
	}
    
    
	
	public Object visitExprBinary(ExprBinary exp)
    {
		if(exp.getOp() == ExprBinary.BINOP_LSHIFT || exp.getOp() == ExprBinary.BINOP_RSHIFT){			
			return this.handleBitShift(exp);
		}
		if(exp.getOp() == ExprBinary.BINOP_ADD){
			FEContext context = exp.getContext();
			Type lType = stv.getType(exp.getLeft());
			if(lType instanceof TypeArray ){
				lType = ((TypeArray)lType).getBase();
			}
			Type rType = stv.getType(exp.getRight());
			if(rType instanceof TypeArray ){
				rType = ((TypeArray)rType).getBase();
			}
			
			String carry = stv.addNewDeclaration(lType, new ExprConstInt(context, 0));
			String ldecl = stv.addNewDeclaration(lType, new ExprConstInt(context, 0));
			String rdecl = stv.addNewDeclaration(rType, new ExprConstInt(context, 0));
			
			Expression left = doExpression(exp.getLeft());
			Expression right = doExpression(exp.getRight());
			StmtAssign lassign = new StmtAssign(context,new ExprVar(context, ldecl), left );
			StmtAssign rassign = new StmtAssign(context,new ExprVar(context, rdecl), right );
			this.preStmts.add(rassign);
			this.preStmts.add(lassign);
			left = new ExprVar(context, ldecl);
			right = new ExprVar(context, rdecl);
			
			Expression newExp = new ExprBinary(context, ExprBinary.BINOP_BXOR, left, right);
			newExp = new ExprBinary(context, ExprBinary.BINOP_BXOR, newExp, new ExprVar(context, carry));
			Expression newCarry1 = new ExprBinary(context, ExprBinary.BINOP_BAND, left , new ExprVar(context, carry));
			Expression newCarry2 = new ExprBinary(context, ExprBinary.BINOP_BOR, left , new ExprVar(context, carry));
			Expression newCarry3 = new ExprBinary(context, ExprBinary.BINOP_BAND, right , newCarry2);
			Expression newCarry4 = new ExprBinary(context, ExprBinary.BINOP_BOR, newCarry3 , newCarry1);
			Statement newStmt = new StmtAssign(context,new ExprVar(context, carry), newCarry4);
			postStmts.add(newStmt);
			return newExp;			
		}
		
		return super.visitExprBinary(exp);		        
    }
}

public class GenerateCopies extends SymbolTableVisitor
{
    private TempVarGen varGen;
   
    
    public String addNewDeclaration(Type type,  Expression exp){
    	String newVarName = varGen.nextVar();
		StmtVarDecl rhsDecl = new StmtVarDecl(exp.getContext(),type, newVarName, exp );
		symtab.registerVar(newVarName, type, null, SymbolTable.KIND_LOCAL);
		this.doStatement(rhsDecl);	
		return newVarName;
    }
    
    /**
     * Create a new copy generator.
     *
     * @param varGen  global temporary variable generator
     */
    public GenerateCopies(TempVarGen varGen)
    {
        super(null);
        this.varGen = varGen;
    }

    /**
     * Checks if variables of type type can be implemented as
     * a reference in Java or elsewhere.  This is true for arrays,
     * structures, and complex numbers.
     */
    private boolean needsCopy(Type type)
    {
        if (type instanceof TypeArray)
            return true;
        if (type instanceof TypeStruct)
            return true;
        if (type instanceof TypeStructRef)
            return true;
        if (type.isComplex())
            return true;
        return false;
    }

    /**
     * Checks if the result of the given expression can be implemented
     * as a reference in Java or elsewhere.
     */
    private boolean needsCopy(Expression expr)
    {
	// don't generate copies for array initializers, since we
	// currently assume that they specify every literal in the
	// array (they don't contain variable references).
	if (expr instanceof ExprArrayInit) {
	    return false;
	} else {
	    return needsCopy(getType(expr));
	}
    }

    /**
     * Use <code>addStatement</code> to add a statement assigning
     * <code>expr</code> to a new temporary, and return the expression
     * for the temporary.
     *
     * @param expr  expression to copy
     * @param deep  if true, generate a deep copy, as in {@link makeCopy}.
     * @return      variable expression for the temporary
     */
    private Expression assignToTemp(Expression expr, boolean deep)
    {
        String tempName = varGen.nextVar();
        Expression tempVar = new ExprVar(expr.getContext(), tempName);
        Type type = getType(expr);
        addVarDecl(expr.getContext(), type, tempName);
        if (deep)
            makeCopy(expr, tempVar);
        else
            addStatement(new StmtAssign(expr.getContext(), tempVar, expr));
        return tempVar;
    }

    /**
     * Use <code>addStatement</code> to generate a deep copy of the
     * (idempotent) expression in <code>from</code> into the (lvalue)
     * expression in <code>to</code>.
     */
    private void makeCopy(Expression from, Expression to)
    {
        // Assume that from and to have the same type.  What are we copying?
        Type type = getType(to);
        if (type instanceof TypeArray)
            makeCopyArray(from, to, (TypeArray)type);
        else if (type instanceof TypeStruct)
            makeCopyStruct(from, to, (TypeStruct)type);
        else if (type.isComplex())
            makeCopyComplex(from, to);
        else
            addStatement(new StmtAssign(to.getContext(), to, from));
    }

    private void makeCopyArray(Expression from, Expression to, TypeArray type)
    {
        // We need to generate a for loop, since from our point of
        // view, the array bounds may not be constant.
        String indexName = varGen.nextVar();
        ExprVar index = new ExprVar(null, indexName);
        Type intType = new TypePrimitive(TypePrimitive.TYPE_INT);        
        Statement init =
            new StmtVarDecl(null, intType, indexName,
                            new ExprConstInt(null, 0));
        symtab.registerVar(indexName, intType, null, SymbolTable.KIND_LOCAL);
        Expression cond =
            new ExprBinary(null, ExprBinary.BINOP_LT, index, type.getLength());
        Statement incr =
            new StmtAssign(null, index,
                           new ExprBinary(null, ExprBinary.BINOP_ADD,
                                          index, new ExprConstInt(null, 1)));
        // Need to make a deep copy.  Existing machinery uses
        // addStatement(); visiting a StmtBlock will save this.
        // So, create a block containing a shallow copy, then
        // visit:
        Indexify indexifier = new Indexify(index, this);
        Expression fel = (Expression) from.accept(indexifier);
        Indexify indexifier2 = new Indexify(index, this);
        Expression tel = (Expression) to.accept(indexifier2); 
        List<Statement> bodyLst = new ArrayList<Statement>();
        bodyLst.addAll(indexifier.preStmts);
        bodyLst.addAll(indexifier2.preStmts);
        bodyLst.add(new StmtAssign(null,
                tel,
                fel));
        bodyLst.addAll(indexifier.postStmts);
        bodyLst.addAll(indexifier2.postStmts);
        Statement body =
            new StmtBlock(null,    bodyLst);
        body = (Statement)body.accept(this);

        // Now generate the loop, we have all the parts.
        addStatement(new StmtFor(null, init, cond, incr, body));
    }

    private void makeCopyStruct(Expression from, Expression to,
                                TypeStruct type)
    {
        for (int i = 0; i < type.getNumFields(); i++)
        {
            String fname = type.getField(i);
            makeCopy(new ExprField(from.getContext(), from, fname),
                     new ExprField(to.getContext(), to, fname));
        }
    }

    private void makeCopyComplex(Expression from, Expression to)
    {
        addStatement
            (new StmtAssign(to.getContext(),
                            new ExprField(to.getContext(), to, "real"),
                            new ExprField(from.getContext(), from, "real")));
        addStatement
            (new StmtAssign(to.getContext(),
                            new ExprField(to.getContext(), to, "imag"),
                            new ExprField(from.getContext(), from, "imag")));
    }

    public Object visitExprPeek(ExprPeek expr)
    {
        Expression result = (Expression)super.visitExprPeek(expr);
        if (needsCopy(result))
            result = assignToTemp(result, false);
        return result;
    }
    
    public Object visitExprPop(ExprPop expr)
    {
        Expression result = (Expression)super.visitExprPop(expr);
        if (needsCopy(result))
            result = assignToTemp(result, false);
        return result;
    }

    
    
    
    private Type matchTypes(Statement stmt,String lhsn, Type lt, Type rt){
    	if( lt != null && rt != null && lhsn != null){
    		if(!lt.isNonDet() && rt.isNonDet()){
    			/*
    			 * In this case, the lhs of the assignment is a deterministic
    			 * type, but the rhs is not. In this case, we promote the
    			 * type of the var on the lhs.
    			 */
    			symtab.upgradeVar(lhsn, lt.makeNonDet());
    			lt = symtab.lookupVar(lhsn);
    		}
    	}
    	assert !(lt != null && rt != null &&
                !(rt.promotesTo(lt))) : (stmt.getContext() +
                       "BUG, this should never happen");
    	
        assert !( lt == null || rt == null):(stmt.getContext() +
            "BUG, this should never happen");
        return lt;
    }
    
    
    public Object visitStmtVarDecl(StmtVarDecl stmt)
    {
    	Object result = super.visitStmtVarDecl(stmt); 
        for (int i = 0; i < stmt.getNumVars(); i++){
        	Expression ie = stmt.getInit(i);
        	if(ie != null){
        		Type rt = getType(ie);        		
        		Type ftype = matchTypes(stmt, stmt.getName(i), actualType(stmt.getType(i)), rt);
        		upgradeStarToInt(ie, ftype);
        	}
        }
        return result;
    }

    public Object visitStmtLoop(StmtLoop stmt){    	
    	Expression ie = stmt.getIter();
    	ie.accept(new UpgradeStarToInt(this, true) );    	
    	return super.visitStmtLoop(stmt);    	
    }
    
    public Object visitExprArrayRange(ExprArrayRange exp){
    	assert exp.getMembers().size() == 1 && exp.getMembers().get(0) instanceof RangeLen : "Complex indexing not yet implemented.";    	    	
		Expression newBase=doExpression(exp.getBase());
		RangeLen rl = (RangeLen)exp.getMembers().get(0);
		assert rl.len == 1 : "Complex indexing not yet implemented.";
		Expression newIndex = doExpression(rl.start);
		return new ExprArray(exp.getContext(), newBase, newIndex);
    }
    
    public void upgradeStarToInt(Expression exp, Type ftype){
    	if(ftype.isNonDet()){
     	   Type base = ftype;
     	   if(ftype instanceof TypeArray){
     		   base = ((TypeArray)ftype).getBase();
     	   }
     	   if(base.equals(TypePrimitive.ndinttype)){
     		  exp.accept(new UpgradeStarToInt(this, true) );
     	   }else{
     		  exp.accept(new UpgradeStarToInt(this, false) );
     	   }
        }
    }
    
    public Object visitStmtAssign(StmtAssign stmt)
    {
    	
	   Type lt = getType(stmt.getLHS());
       Type rt = getType(stmt.getRHS());
       String lhsn = null;
       Expression lhsExp = stmt.getLHS();
       if(lhsExp instanceof ExprArray){
       	lhsExp = ((ExprArray)stmt.getLHS()).getBase();
       }
       if(lhsExp instanceof ExprArrayRange){
          	lhsExp = ((ExprArrayRange)stmt.getLHS()).getBase();
          }
       if(lhsExp instanceof ExprVar){
       	lhsn = ( (ExprVar) lhsExp).getName();
       }                    
       Type ftype = matchTypes(stmt, lhsn, lt, rt);
       upgradeStarToInt(stmt.getRHS(), ftype);
        // recurse:
        Statement result = (Statement)super.visitStmtAssign(stmt);
        if (result instanceof StmtAssign) // it probably is:
        {
            stmt = (StmtAssign)result;
            if (needsCopy(stmt.getRHS()) || needsCopy(stmt.getLHS()))
            {
                // drops op!  If there are compound assignments
                // like "a += b" here, we lose.  There shouldn't be,
                // though, since those operators aren't well-defined
                // for structures and arrays and this should be run
                // after complex prop.
                makeCopy(stmt.getRHS(), stmt.getLHS());
                return null;
            }
        }
        return result;
    }

    public Object visitStmtPush(StmtPush expr)
    {
        Expression value = (Expression)expr.getValue().accept(this);
        if (needsCopy(value))
            value = assignToTemp(value, true);
        return new StmtPush(expr.getContext(), value);
    }
    
}
