package sketch.compiler.dataflow.simplifier;

import java.util.ArrayList;
import java.util.List;

import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.SymbolTable;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.*;
import sketch.compiler.ast.core.exprs.ExprArrayRange.RangeLen;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtBlock;
import sketch.compiler.ast.core.stmts.StmtFor;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.passes.lowering.SymbolTableVisitor;

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
	boolean agressive;

    public String addNewDeclaration(Type type,  Expression exp){
    	String newVarName = varGen.nextVar();
		StmtVarDecl rhsDecl = new StmtVarDecl(exp,type, newVarName, exp );
		symtab.registerVar(newVarName, type, null, SymbolTable.KIND_LOCAL);
		this.doStatement(rhsDecl);
		return newVarName;
    }


	public ScalarizeVectorAssignments(TempVarGen varGen){
		super(null);
		this.varGen = varGen;
	}


	/**
	 *
	 * @param varGen     generator of temporary variable names
	 * @param agressive  if true, assignments from arrays to arrays will be
	 * 					 transformed into scalarized versions
	 */
	public ScalarizeVectorAssignments(TempVarGen varGen, boolean agressive){
		super(null);
		this.varGen = varGen;
		this.agressive = agressive;
	}

	/**
	 * This class identifies whether we need to do a copy or not.
	 * The following are cases where we need to generate special code:
	 * <dl>
	 * <dd> 1) lhs and rhs are arrays of different dimensions.
	 * <dd> 2) lhs is an array and rhs is a scalar
	 * <dd> 3) lhs is an array and rhs contains a ternary/binary/unary operator.
	 * <dd> 4) lhs contains a non-trivial array range.
	 * 	</dl>
	 *
	 *
	 */
	private int needsCopy(Expression lhs, Expression rhs){
		Type lt = getType(lhs);
		if( !(lt instanceof TypeArray ) ) return 0;
		Type rt = getType(rhs);
		if( !(rt instanceof TypeArray )  ) return 2;
		if( (new OpFinder()).hasOp(rhs) ) return 3;
		if( lt.equals(rt) && !agressive ) return 0;
		
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
        private final Expression defval;
		private boolean isRHS;


        Indexify(Expression index, Expression len, Expression defval, boolean isRHS) {
			this.index = index;
			this.postStmts = new ArrayList<Statement>();
			this.preStmts = new ArrayList<Statement>();
			this.len = len;
			this.isRHS = isRHS;
            this.defval = defval;
		}

		public Object visitExprArrayInit (ExprArrayInit exp) {
			// TODO: assuming ExprArrayInit is initialized with scalar constants only.
			int len = exp.getElements ().size ();
            // TypeArray ta = (TypeArray) getType(exp);

            // Type baseType = ta.getBase();
			return new ExprTernary ("?:",
					new ExprBinary (index, "<", ExprConstant.createConstant (exp, ""+ len)),
					new ExprArrayRange (exp, index),
 defval);
		}

		public Object visitExprConstInt(ExprConstInt exp){
			assert isRHS : "this can't happen!!!";
			Integer iv = len.getIValue();
			if( iv != null && iv <= 1  ){
				return exp;
			}
			return new ExprTernary("?:",
					new ExprBinary(exp, ExprBinary.BINOP_LT, index,  new ExprConstInt(1)),
					exp,
					ExprConstInt.zero);
		}


		public Object visitExprVar(ExprVar exp) {
			if ( getType(exp) instanceof TypeArray){
				Expression arrLen = typeLen(getType(exp));
				if( arrLen.equals(len) || !isRHS){
					return new ExprArrayRange(exp, exp, index, true);
				}else{
					return new ExprTernary(exp,
							ExprTernary.TEROP_COND,
							new ExprBinary(exp,ExprBinary.BINOP_AND,
									new ExprBinary(exp, ExprBinary.BINOP_LT, index, arrLen ),
									new ExprBinary(exp, ExprBinary.BINOP_GE, index, ExprConstInt.zero )
							)
							,
							new ExprArrayRange(exp, exp, index, true),
 defval);
				}
			}else{
			    Integer iv = len.getIValue();
				if( iv != null && iv <= 1  ){
					return exp;
				}else{
					return new ExprTernary(exp,
							ExprTernary.TEROP_COND,
 new ExprBinary(
                            exp, ExprBinary.BINOP_EQ, index, ExprConstInt.zero), exp,
                            defval);

				}
			}
		}


        public Object visitExprStar(ExprStar exp) {
            ExprStar ns = (ExprStar) super.visitExprStar(exp); // new ExprStar(exp,
                                                               // exp.getSize());

			if( exp.getType() instanceof TypeArray  ){
				// ns.setType( ((TypeArray)exp.getType()).getBase()  );
				Expression arrLen = typeLen(getType(exp));
				if( arrLen.equals(len) || !isRHS){
					return new ExprArrayRange(exp, ns, index, true);
				}else{
					return new ExprTernary(exp,
							ExprTernary.TEROP_COND,
							new ExprBinary(exp,ExprBinary.BINOP_AND,
									new ExprBinary(exp, ExprBinary.BINOP_LT, index, arrLen ),
									new ExprBinary(exp, ExprBinary.BINOP_GE, index, ExprConstInt.zero )
							)
							,
							new ExprArrayRange(exp, ns, index, true),
							ExprConstInt.zero);
				}
			}else{
				assert len.getIValue()== 1 : "This can't be happening!!";
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
			RangeLen rl = exp.getSelection();
			Type t = getType(exp);
			Expression tl = typeLen(t);
			Integer itl = tl.getIValue();
			if( itl != null && itl <= 1  ){
				Expression compIndex = new ExprBinary(exp, ExprBinary.BINOP_ADD, index, (Expression)(rl.start()).accept(ScalarizeVectorAssignments.this));
				return new ExprArrayRange(exp, exp.getBase(), compIndex);
			}else{
				return new ExprArrayRange(exp, exp, index);
			}
	    }


		public Expression handleBitShift(ExprBinary exp){
			{
				assert isRHS : "Shifts are only allowed in RHS";
				assert exp.getOp() == ExprBinary.BINOP_LSHIFT || exp.getOp() == ExprBinary.BINOP_RSHIFT : "This should never happen!!";
				Type lType = getType(exp.getLeft());
				//Type rType = stv.getType(exp.getRight());
				Expression result;
				if(false){
					//This branch just assumes that an out of bounds access doesn't
					FENode context = exp;
					String newVarName = addNewDeclaration(TypePrimitive.inttype, exp.getRight());
					ExprVar oldRHS = new ExprVar(context, newVarName);
					Expression newIdx = null;
					if( exp.getOp() == ExprBinary.BINOP_RSHIFT ){
						newIdx = new ExprBinary(context, ExprBinary.BINOP_ADD, index, oldRHS , exp.getAlias());
					}else{
						newIdx = new ExprBinary(context, ExprBinary.BINOP_SUB, index, oldRHS, exp.getAlias());
					}
                    Indexify indexify =
                            new Indexify(newIdx, this.len, lType.defaultValue(), isRHS);
					Expression newVal = (Expression) exp.getLeft().accept(indexify);
					this.preStmts.addAll(indexify.preStmts);
					this.postStmts.addAll(indexify.postStmts);
					result = newVal;
				}else{
                    // In this branch, we actually emmit a test that explicitly returns
                    // the default value if the array goes out of bounds.
					FENode context = exp;
					String newVarName = addNewDeclaration(TypePrimitive.inttype, exp.getRight());
					ExprVar oldRHS = new ExprVar(context, newVarName);
					Expression newIdx = null;
					int op;
					Expression newConst = null;
					if( exp.getOp() == ExprBinary.BINOP_RSHIFT ){
						newIdx = new ExprBinary(context, ExprBinary.BINOP_ADD, index, oldRHS, exp.getAlias() );
						op = ExprBinary.BINOP_LT;
						TypeArray ta = (TypeArray) lType;
						newConst = ta.getLength();
					}else{
						newIdx = new ExprBinary(context, ExprBinary.BINOP_SUB, index, oldRHS, exp.getAlias());
						op = ExprBinary.BINOP_GE;
						newConst = new ExprConstInt(context, 0);
					}
                    Indexify indexify =
                            new Indexify(newIdx, this.len, new ExprConstInt(context, 0),
                                    isRHS);
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



		public Object visitExprTernary(ExprTernary exp){
			assert exp.getOp() == ExprTernary.TEROP_COND : "Strange operator I've never seen  " + exp + "!?";
			Integer condlen = typeLen(getType(exp.getA())).getIValue();
			assert condlen == 1 : "The type of the predicate for a conditional must be a scalar. " + exp + ": " + exp;
			Expression expB = (Expression) exp.getB().accept(this);
			Expression expC = (Expression) exp.getC().accept(this);

			return new ExprTernary(exp, exp.getOp(), exp.getA(), expB, expC );
		}

		public Object visitExprBinary(ExprBinary exp)
	    {
			if(exp.getOp() == ExprBinary.BINOP_LSHIFT || exp.getOp() == ExprBinary.BINOP_RSHIFT){
				return this.handleBitShift(exp);
			}
			if(exp.getOp() == ExprBinary.BINOP_ADD){
				FENode context = exp;
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
				StmtAssign lassign = new StmtAssign(new ExprVar(context, ldecl), left );
				StmtAssign rassign = new StmtAssign(new ExprVar(context, rdecl), right );
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
				Statement newStmt = new StmtAssign(new ExprVar(context, carry), newCarry4);
				postStmts.add(newStmt);
				return newExp;
			}

			return super.visitExprBinary(exp);
	    }
	}


	private Statement zeroOut(Expression lhs, Expression beg, Expression end, Expression zero){
		String indexName = varGen.nextVar();
        ExprVar index = new ExprVar(lhs, indexName);
        Type intType =  TypePrimitive.inttype;
        Statement init = new StmtVarDecl(lhs, intType, indexName, beg);
        symtab.registerVar(indexName, intType, null, SymbolTable.KIND_LOCAL);
        Expression cond =
            new ExprBinary(null, ExprBinary.BINOP_LT, index, end);
        Statement incr =
            new StmtAssign(index, new ExprBinary(index, "+", new ExprConstInt(1)));
        // Need to make a deep copy.  Existing machinery uses
        // addStatement(); visiting a StmtBlock will save this.
        // So, create a block containing a shallow copy, then
        // visit:
        Indexify indexifier2 = new Indexify(index, end, zero, false);
        Expression tel = (Expression) lhs.accept(indexifier2);
        List<Statement> bodyLst = new ArrayList<Statement>();
        bodyLst.addAll(indexifier2.preStmts);
        bodyLst.add(new StmtAssign(
                tel,
                zero));
        bodyLst.addAll(indexifier2.postStmts);

        Statement body = new StmtBlock(lhs, bodyLst);
        body = (Statement)body.accept(this);

        // Now generate the loop, we have all the parts.
        return new StmtFor(init, init, cond, incr, body);
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
    	if( ltlen.equals(rtlen)  ){
    		return ltlen;
    	}
    	Expression comp = new ExprBinary(ltlen, ExprBinary.BINOP_LE, ltlen, rtlen );
    	return new ExprTernary(ltlen, ExprTernary.TEROP_COND, comp, ltlen, rtlen );
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

    	Integer irtlen = rtlen.getIValue();

    	if( irtlen != null && irtlen == 1 ){
            if (rhs.equals(lt.defaultValue())) {

    			List<Statement> mainLst = new ArrayList<Statement>();
                mainLst.add(zeroOut(lhs, ExprConstInt.zero, ltlen, lt.defaultValue()));
				Statement mainBody = new StmtBlock(lhs, mainLst);
				// Now generate the loop, we have all the parts.
				addStatement(mainBody);

    			return;
    		}
    	}

        Expression minLen = rtlen; // Thanks to the assertions, we can be sure the RHS len
                                   // is always smaller than the LHS len
                                   // minLength(ltlen, rtlen);
        // We need to generate a for loop, since from our point of
        // view, the array bounds may not be constant.
        String indexName = varGen.nextVar();
        ExprVar index = new ExprVar(lhs, indexName);
        Type intType = TypePrimitive.inttype;
        Statement init =
            new StmtVarDecl(index, intType, indexName,
                            new ExprConstInt(0));
        symtab.registerVar(indexName, intType, null, SymbolTable.KIND_LOCAL);
        Expression cond =
            new ExprBinary(index, "<", minLen);
        Statement incr =
            new StmtAssign(index, new ExprBinary(index, "+", new ExprConstInt(1)));
        // Need to make a deep copy.  Existing machinery uses
        // addStatement(); visiting a StmtBlock will save this.
        // So, create a block containing a shallow copy, then
        // visit:
        Indexify indexifier = new Indexify(index, minLen, rt.defaultValue(), true);
        Expression fel = (Expression) rhs.accept(indexifier);
        Indexify indexifier2 = new Indexify(index, minLen, rt.defaultValue(), false);
        Expression tel = (Expression) lhs.accept(indexifier2);
        List<Statement> bodyLst = new ArrayList<Statement>();
        bodyLst.addAll(indexifier.preStmts);
        bodyLst.addAll(indexifier2.preStmts);
        bodyLst.add(new StmtAssign(tel, fel));
        bodyLst.addAll(indexifier.postStmts);
        bodyLst.addAll(indexifier2.postStmts);



        Statement body = new StmtBlock(lhs, bodyLst);
        body = (Statement)body.accept(this);


        List<Statement> mainLst = new ArrayList<Statement>();

        mainLst.add(new StmtFor(init, init, cond, incr, body));


        if (!(minLen.equals(ltlen))) {
        	mainLst.add (zeroOut(lhs, minLen, ltlen, getType (rhs).defaultValue ()));
        }
        
        Statement mainBody = new StmtBlock(lhs, mainLst);

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
            
            
          //not sure what the context should be in general 
           // 	z = array1 == array2;
           // ---->
           //
           //	z = 1;
           //	for (int index = 0; index < length; index = index + 1)
           // 		z = z && (array1[index] == array2[index]);
            
          if (stmt.getRHS() instanceof ExprBinary){
            	ExprBinary expr = (ExprBinary)stmt.getRHS();
            	Type lt = getType(expr.getLeft());
            	Type rt = getType(expr.getRight());
            	
            	Expression len = typeLen(lt);
            	
            	//assert typeLen(lt) == typeLen(rt);
            	
            	if (expr.getOp() == ExprBinary.BINOP_EQ && lt.isArray()){           		
            		
            		ExprVar leftVar = (ExprVar) stmt.getLHS();
            		
            		String indexName = varGen.nextVar();
            		ExprVar index = new ExprVar(leftVar, indexName);
            		
            		//create arr1[i] and arr2[i]
            		ExprArrayRange leftScalarExp = new ExprArrayRange(expr, expr.getLeft(), index, true);
            		ExprArrayRange rightScalarExp = new ExprArrayRange(expr, expr.getRight(), index, true);
            		
            		//create expression var1 && (arr1[i] == arr2[i]);
            		ExprBinary scalarEqExp = new ExprBinary(ExprBinary.BINOP_EQ, leftScalarExp, rightScalarExp);
            		ExprBinary scalarAndExp = new ExprBinary(ExprBinary.BINOP_AND, leftVar, scalarEqExp);
            		
            		// create assignments:
            		Statement decl = new StmtAssign(leftVar, ExprConstInt.one);
            		Statement body = new StmtAssign(leftVar, scalarAndExp);
            		
            		List<Statement> bodyList = new ArrayList<Statement>();
            		bodyList.add(body);
            		
            		Statement bodyBlock = new StmtBlock(expr, bodyList);
            		
            		
            		//surround with for-loop
            		Statement init = new StmtVarDecl(index, TypePrimitive.inttype, indexName, new ExprConstInt(0));
            		Expression cond = new ExprBinary(ExprBinary.BINOP_LT, index, len);
            		Statement incr = new StmtAssign(index, new ExprBinary(index, "+", new ExprConstInt(1)));
            		
            		Statement forStmt = new StmtFor(stmt, init, cond, incr, bodyBlock);
            		
            		/****/ //copy-pasted from above
            		//List<Statement> bodyLst = new ArrayList<Statement>();
                    //Statement block = new StmtBlock(expr, bodyLst);
                    //block = (Statement)block.accept(this);

                    List<Statement> mainLst = new ArrayList<Statement>();
                    
                    mainLst.add(decl);
                    mainLst.add(forStmt);                   
                    Statement mainBody = new StmtBlock(expr, mainLst);

                    // Now generate the loop, we have all the parts.
                    addStatement(mainBody);
            		/****/
                    
                    
                    //addStatement(decl);
            		//addStatement(forStmt);
            		
            		return null;
            		
            	}
            }
        }
        return result;
    }
	

}
