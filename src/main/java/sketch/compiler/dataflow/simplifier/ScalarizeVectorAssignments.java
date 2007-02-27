package streamit.frontend.experimental.simplifier;

import java.util.ArrayList;
import java.util.List;

import streamit.frontend.nodes.ExprArrayRange;
import streamit.frontend.nodes.ExprBinary;
import streamit.frontend.nodes.ExprConstInt;
import streamit.frontend.nodes.ExprStar;
import streamit.frontend.nodes.ExprTernary;
import streamit.frontend.nodes.ExprTypeCast;
import streamit.frontend.nodes.ExprUnary;
import streamit.frontend.nodes.ExprVar;
import streamit.frontend.nodes.Expression;
import streamit.frontend.nodes.FEContext;
import streamit.frontend.nodes.FENode;
import streamit.frontend.nodes.FEReplacer;
import streamit.frontend.nodes.Statement;
import streamit.frontend.nodes.StmtAssign;
import streamit.frontend.nodes.StmtBlock;
import streamit.frontend.nodes.StmtFor;
import streamit.frontend.nodes.StmtVarDecl;
import streamit.frontend.nodes.SymbolTable;
import streamit.frontend.nodes.TempVarGen;
import streamit.frontend.nodes.Type;
import streamit.frontend.nodes.TypeArray;
import streamit.frontend.nodes.TypePrimitive;
import streamit.frontend.nodes.ExprArrayRange.RangeLen;
import streamit.frontend.passes.SymbolTableVisitor;
/**
 * 
 * Scalarizes vector assignments to satisfy the preconditions of 
 * {@link streamit.frontend.experimental.nodesToSB.ProduceBooleanFunctions ProduceBooleanFunctions}.
 * 
 *  Preconditions:
 *  * non-trivial range expressions only appear in simple assignments.
 *  
 *  Postconditions:
 *  * All explicit array ranges have been eliminated.
 *  * binary operators work only for scalars.
 *  * 
 * 
 * @author asolar
 *
 */
public class ScalarizeVectorAssignments extends SymbolTableVisitor {
	TempVarGen varGen;
	

    public String addNewDeclaration(Type type,  Expression exp){
    	String newVarName = varGen.nextVar();
		StmtVarDecl rhsDecl = new StmtVarDecl(exp.getContext(),type, newVarName, exp );
		symtab.registerVar(newVarName, type, null, SymbolTable.KIND_LOCAL);
		this.doStatement(rhsDecl);	
		return newVarName;
    }
    
    
	public ScalarizeVectorAssignments(TempVarGen varGen){
		super(null);
		this.varGen = varGen;
	}
	
	/**
	 * This class identifies whether we need to do a copy or not. 
	 * The following are cases where we need to generate special code:
	 * <dl>
	 * <dd> 1) lhs and rhs are arrays of different dimensions.
	 * <dd> 2) lhs is an array and rhs is a scalar
	 * <dd> 3) lhs is an array and rhs contains a ternary/binary/unary operator.
	 * <dd> 4) lhs contains a non-trivial array range.
	 * </dl>
	 * 
	 * 
	 */
	private int needsCopy(Expression lhs, Expression rhs){
		Type lt = getType(lhs);
		if( !(lt instanceof TypeArray ) ) return 0;
		Type rt = getType(rhs);
		if( !(rt instanceof TypeArray )  ) return 2;
		if( (new OpFinder()).hasOp(rhs) ) return 3;
		if( lt.equals(rt) ) return 0;
		return 1; //lhs and rhs are arrays of different dimensions.
	}
	
	private static class OpFinder extends FEReplacer{
		private boolean hasBinop = false;
		public Object visitExprUnary(ExprUnary binop){
			hasBinop = true;
			return binop;
		}
		public Object visitExprTernary(ExprTernary binop){
			hasBinop = true;
			return binop;
		}
		public Object visitExprBinary(ExprBinary binop){
			hasBinop = true;
			return binop;
		}
		public boolean hasOp(FENode node){
			hasBinop = false;
			node.accept(this);
			return hasBinop;
		}
	}
	
	
	class Indexify extends FEReplacer{
		private final Expression index;		
		public final List<Statement> postStmts;
		public final List<Statement> preStmts;
		private final Expression len;
		private boolean isRHS;
		
		
		Indexify(Expression index, Expression len, boolean isRHS){
			this.index = index;			
			this.postStmts = new ArrayList<Statement>();
			this.preStmts = new ArrayList<Statement>();
			this.len = len;
			this.isRHS = isRHS;
		}
		
		
		public Object visitExprConstInt(ExprConstInt exp){
			assert isRHS : "this can't happen!!!";
			Integer iv = len.getIValue();
			if( iv != null && iv <= 1  ){
				return exp;
			}
			return new ExprTernary(exp.getCx(),
				ExprTernary.TEROP_COND, 
				new ExprBinary(exp.getCx(), ExprBinary.BINOP_LT, index,  new ExprConstInt(1) )
				,
				 exp,
				new ExprConstInt(0));
		}
		
		
		public Object visitExprVar(ExprVar exp) {
			if ( getType(exp) instanceof TypeArray){
				Expression arrLen = typeLen(getType(exp));
				if( arrLen.equals(len) || !isRHS){
					return new ExprArrayRange(exp.getContext(), exp, index, true);
				}else{
					return new ExprTernary(exp.getCx(),
							ExprTernary.TEROP_COND, 
							new ExprBinary(exp.getCx(),ExprBinary.BINOP_AND,
									new ExprBinary(exp.getCx(), ExprBinary.BINOP_LT, index, arrLen ),
									new ExprBinary(exp.getCx(), ExprBinary.BINOP_GE, index, new ExprConstInt(0) )
							)
							,
							new ExprArrayRange(exp.getContext(), exp, index, true),
							new ExprConstInt(0));
				}
			}else{
				assert len.getIValue()== 1 : "This can't be happening!!";
				return exp;
			}
		}
		
		
		public Object visitExprStar(ExprStar exp){
			ExprStar ns = new ExprStar(exp.getContext(), exp.getSize());
			if( exp.getType() instanceof TypeArray  ){
				ns.setType( ((TypeArray)exp.getType()).getBase()  );
			}else{
				ns.setTag(exp.getType());
			}
			return ns;
		}
		
		public Object visitExprTypeCast(ExprTypeCast exp)
	    {
			if(exp.getType() instanceof TypePrimitive){
				return exp;
			}else{
				return doExpression(exp.getExpr());
			}        
	    }
		
		public Object visitExprArrayRange(ExprArrayRange exp){
			assert exp.getMembers().size() == 1 && exp.getMembers().get(0) instanceof RangeLen : "Complex indexing not yet implemented.";
			RangeLen rl = (RangeLen)exp.getMembers().get(0);
			Expression compIndex = new ExprBinary(exp.getContext(), ExprBinary.BINOP_ADD, index, (Expression)(rl.start()).accept(ScalarizeVectorAssignments.this));
			return new ExprArrayRange(exp.getContext(), exp.getBase(), compIndex);		
	    }	
		
		
		public Expression handleBitShift(ExprBinary exp){
			{
				assert isRHS : "Shifts are only allowed in RHS";
				assert exp.getOp() == ExprBinary.BINOP_LSHIFT || exp.getOp() == ExprBinary.BINOP_RSHIFT : "This should never happen!!";
				Type lType = getType(exp.getLeft());
				//Type rType = stv.getType(exp.getRight());
				Expression result;
				if(true){
					//This branch just assumes that an out of bounds access doesn't 
					FEContext context = exp.getContext();
					String newVarName = addNewDeclaration(TypePrimitive.inttype, exp.getRight());								
					ExprVar oldRHS = new ExprVar(context, newVarName);
					Expression newIdx = null;
					if( exp.getOp() == ExprBinary.BINOP_LSHIFT ){
						newIdx = new ExprBinary(context, ExprBinary.BINOP_ADD, index, oldRHS , exp.getAlias());					
					}else{
						newIdx = new ExprBinary(context, ExprBinary.BINOP_SUB, index, oldRHS, exp.getAlias());
					}
					Indexify indexify = new Indexify(newIdx, this.len, isRHS);
					Expression newVal = (Expression) exp.getLeft().accept(indexify);
					this.preStmts.addAll(indexify.preStmts);
					this.postStmts.addAll(indexify.postStmts);			
					result = newVal;								
				}else{
					//In this branch, we actually emmit a test that explicitly returns zero if the array goes out of bounds.
					FEContext context = exp.getContext();
					String newVarName = addNewDeclaration(TypePrimitive.inttype, exp.getRight());								
					ExprVar oldRHS = new ExprVar(context, newVarName);
					Expression newIdx = null;
					int op;
					Expression newConst = null;
					if( exp.getOp() == ExprBinary.BINOP_LSHIFT ){
						newIdx = new ExprBinary(context, ExprBinary.BINOP_ADD, index, oldRHS, exp.getAlias() );
						op = ExprBinary.BINOP_LT;
						TypeArray ta = (TypeArray) lType;					
						newConst = ta.getLength();
					}else{
						newIdx = new ExprBinary(context, ExprBinary.BINOP_SUB, index, oldRHS, exp.getAlias());
						op = ExprBinary.BINOP_GE;
						newConst = new ExprConstInt(context, 0);					
					}
					Indexify indexify = new Indexify(newIdx, this.len, isRHS );
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
				Type lType = getType(exp.getLeft());
				if(lType instanceof TypeArray ){
					lType = ((TypeArray)lType).getBase();
				}
				Type rType = getType(exp.getRight());
				if(rType instanceof TypeArray ){
					rType = ((TypeArray)rType).getBase();
				}
				
				String carry = addNewDeclaration(lType, new ExprConstInt(context, 0));
				String ldecl = addNewDeclaration(lType, new ExprConstInt(context, 0));
				String rdecl = addNewDeclaration(rType, new ExprConstInt(context, 0));
				
				Expression left = doExpression(exp.getLeft());
				Expression right = doExpression(exp.getRight());
				StmtAssign lassign = new StmtAssign(context,new ExprVar(context, ldecl), left );
				StmtAssign rassign = new StmtAssign(context,new ExprVar(context, rdecl), right );
				this.preStmts.add(rassign);
				this.preStmts.add(lassign);
				left = new ExprVar(context, ldecl);
				right = new ExprVar(context, rdecl);
				
				Expression newExp = new ExprBinary(context, ExprBinary.BINOP_BXOR, left, right, exp.getAlias());
				newExp = new ExprBinary(context, ExprBinary.BINOP_BXOR, newExp, new ExprVar(context, carry), exp.getAlias());
				Expression newCarry1 = new ExprBinary(context, ExprBinary.BINOP_BAND, left , new ExprVar(context, carry), exp.getAlias());
				Expression newCarry2 = new ExprBinary(context, ExprBinary.BINOP_BOR, left , new ExprVar(context, carry), exp.getAlias());
				Expression newCarry3 = new ExprBinary(context, ExprBinary.BINOP_BAND, right , newCarry2, exp.getAlias());
				Expression newCarry4 = new ExprBinary(context, ExprBinary.BINOP_BOR, newCarry3 , newCarry1, exp.getAlias());
				Statement newStmt = new StmtAssign(context,new ExprVar(context, carry), newCarry4);
				postStmts.add(newStmt);
				return newExp;			
			}
			
			return super.visitExprBinary(exp);		        
	    }
	}


	private void zeroOut(Expression lhs, Expression beg, Expression end, List<Statement> mainLst){
		String indexName = varGen.nextVar();
        ExprVar index = new ExprVar(null, indexName);
        Type intType = new TypePrimitive(TypePrimitive.TYPE_INT);        
        Statement init =
            new StmtVarDecl(null, intType, indexName,
                            beg);
        symtab.registerVar(indexName, intType, null, SymbolTable.KIND_LOCAL);
        Expression cond =
            new ExprBinary(null, ExprBinary.BINOP_LT, index, end);
        Statement incr =
            new StmtAssign(null, index,
                           new ExprBinary(null, ExprBinary.BINOP_ADD,
                                          index, new ExprConstInt(null, 1)));
        // Need to make a deep copy.  Existing machinery uses
        // addStatement(); visiting a StmtBlock will save this.
        // So, create a block containing a shallow copy, then
        // visit:        
        Expression fel =  new ExprConstInt(null, 0);
        Indexify indexifier2 = new Indexify(index,  end, false);
        Expression tel = (Expression) lhs.accept(indexifier2);          
        List<Statement> bodyLst = new ArrayList<Statement>();
        bodyLst.addAll(indexifier2.preStmts);
        bodyLst.add(new StmtAssign(null,
                tel,
                fel));
        bodyLst.addAll(indexifier2.postStmts);
        
        Statement body =
            new StmtBlock(null,    bodyLst);
        body = (Statement)body.accept(this);

        // Now generate the loop, we have all the parts.
        mainLst.add(new StmtFor(null, init, cond, incr, body));
        
		return;
	}
	
	
	Expression typeLen(Type t){
		if(t instanceof TypeArray){
			return ((TypeArray)t).getLength();
		}
		if(t instanceof TypePrimitive){
			return new ExprConstInt(1);
		}
		return null;
	}
	
	/**
	 * Return the min between the RHS length and the LHS length.
	 * 
	 * @param ltlen
	 * @param rtlen
	 * @return
	 */
	Expression minLength(Expression ltlen, Expression rtlen){
		Integer rtlenI = rtlen.getIValue();
    	Integer ltlenI = ltlen.getIValue();
    	
    	if( rtlenI != null & ltlenI != null ){
    		if(rtlenI >= ltlenI  ){
    			return ltlen;
    		}else{
    			return rtlen;
    		}
    	}
    	
    	Expression comp = new ExprBinary(ltlen.getCx(), ExprBinary.BINOP_LE, ltlen, rtlen );
    	return new ExprTernary(ltlen.getCx(), ExprTernary.TEROP_COND, comp, ltlen, rtlen );
	}
	
	/**
	 * The ncReason is the reason why you need a copy. 
	 * @param lhs
	 * @param rhs
	 * @param ncReason
	 */
    private void makeCopy(Expression lhs, Expression rhs, int ncReason){
    	
    	Type rt = getType(rhs);
    	Type lt = getType(lhs);
    	Expression rtlen = typeLen(rt);
    	
    	Expression ltlen = typeLen(lt);
    	
    	
    	
    	Expression minLen = minLength(ltlen, rtlen);
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
            new ExprBinary(null, ExprBinary.BINOP_LT, index, minLen);
        Statement incr =
            new StmtAssign(null, index,
                           new ExprBinary(null, ExprBinary.BINOP_ADD,
                                          index, new ExprConstInt(null, 1)));
        // Need to make a deep copy.  Existing machinery uses
        // addStatement(); visiting a StmtBlock will save this.
        // So, create a block containing a shallow copy, then
        // visit:
        Indexify indexifier = new Indexify(index, minLen, true);
        Expression fel = (Expression) rhs.accept(indexifier);
        Indexify indexifier2 = new Indexify(index, minLen, false);
        Expression tel = (Expression) lhs.accept(indexifier2); 
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

        
        List<Statement> mainLst = new ArrayList<Statement>();
        
        mainLst.add(new StmtFor(null, init, cond, incr, body));
        

        if( minLen !=  ltlen){
        	zeroOut(lhs, minLen, ltlen, mainLst);
        }
        Statement mainBody =
            new StmtBlock(null,    mainLst);
       
        
        // Now generate the loop, we have all the parts.
        addStatement(mainBody);
    }
	
	
	
	
	
	
	
	public Object visitStmtAssign(StmtAssign stmt)
    {    	
        Statement result = (Statement)super.visitStmtAssign(stmt);
        if (result instanceof StmtAssign) // it probably is:
        {
            stmt = (StmtAssign)result;
            int ncReason = needsCopy( stmt.getLHS(), stmt.getRHS()); 
            if ( ncReason != 0 ){
                makeCopy(stmt.getLHS(), stmt.getRHS(), ncReason);
                return null;
            }
        }
        return result;
    }

}
