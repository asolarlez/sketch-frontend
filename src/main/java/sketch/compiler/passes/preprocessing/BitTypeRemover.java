package sketch.compiler.passes.preprocessing;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.SymbolTable;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.*;
import sketch.compiler.ast.core.exprs.ExprArrayRange.RangeLen;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtExpr;
import sketch.compiler.ast.core.stmts.StmtFor;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.passes.lowering.GetExprType;
import sketch.compiler.passes.lowering.SymbolTableVisitor;

/**
 * Code generation pass that gets rid of the "bit" data type, using appropriate C types
 * instead. Performs big integer arithmetic on variables that are too large to fit into a
 * single primitive type. The only bit types left are those corresponding to boolean
 * variables.
 * 
 * @author liviu
 */
public class BitTypeRemover extends SymbolTableVisitor
{

	/**
	 * Symbol table that stores the type of a variable prior to substitution.
	 */
    protected SymbolTable preSymtab;
    /**
     * Unique temporary variable generator
     */
    protected TempVarGen varGen;
    /**
     * True if processing syntactic children of a variable declaration.
     */
    private boolean processingDeclaration=false;


    private class Indexify extends FEReplacer{
    	private final Expression index;
    	public final List<Statement> postStmts;
    	public final List<Statement> preStmts;
    	private final int ws;
    	Indexify(Expression index, int ws){
    		this.ws = ws;
    		this.index = index;
    		this.postStmts = new ArrayList<Statement>();
    		this.preStmts = new ArrayList<Statement>();
    	}

    	public Expression indexify(Expression exp){
    		Expression nexp = (Expression)exp.accept(this);
    		BitTypeRemover.this.addStatements(preStmts);
    		return nexp;
    	}

    	public Object visitExprArrayInit(ExprArrayInit arr){
    		List<Expression> elems =  arr.getElements();
    		if( elems.size() <= ws ){
    			return BitTypeRemover.this.makeHexString(arr, 0, ws);
    		}else{
    			FENode context  = arr;
    			String newVarName = varGen.nextVar();
    			Type type = new TypeArray(TypePrimitive.bittype, new ExprConstInt(elems.size())  );
    			StmtVarDecl rhsDecl = new StmtVarDecl(context,type, newVarName, null );
    			ExprVar ev = new ExprVar(context, newVarName);
    			StmtAssign ass = new StmtAssign(ev, arr  );
    			this.preStmts.add( (Statement) rhsDecl.accept(BitTypeRemover.this)  );
    			this.preStmts.add( (Statement) ass.accept(BitTypeRemover.this)  );
    			return new ExprArrayRange(arr, ev, new RangeLen( index) );
    		}
    	}

    	public Object visitExprVar(ExprVar exp) {
    		if (getVarType(exp) instanceof TypeArray)
    			return new ExprArrayRange(exp, exp, new RangeLen( index));
    		else
    			return exp;
    	}
    }


    private static class LinearRange {
    	public LinearRange(int multi, Expression cnt, int offset, int length) {
    		multiplier=multi;
    		count=cnt;
    		startOffset=offset;
    		len=length;
    	}
    	public int multiplier;
    	public Expression count;
    	public int startOffset;
    	public int len;
    }

    public BitTypeRemover(TempVarGen varGen) {
		super(null);
		this.varGen=varGen;
	}

    private static boolean isBitType(Type t)
    {
    	return t instanceof TypePrimitive && ((TypePrimitive)t).getType()==TypePrimitive.TYPE_BIT;
    }

    private static boolean isBitArrayType(Type t)
    {
    	return t instanceof TypeArray && isBitType(((TypeArray)t).getBase());
    }

    private static boolean isArray(Type t)
    {
    	return t instanceof TypeArray;
    }

    private static int getWordsize(Type type)
    {
    	if(type instanceof TypePrimitive) {
    		switch(((TypePrimitive)type).getType()) {
    			case TypePrimitive.TYPE_BIT:
    				return 1;
    			case TypePrimitive.TYPE_INT8:
    				return 8;
    			case TypePrimitive.TYPE_INT16:
    				return 16;
    			case TypePrimitive.TYPE_INT32:
    				return 32;
    			case TypePrimitive.TYPE_INT64:
    				return 64;
    		}
    	}
    	return 0;
    }

    private static int getArrayWordsize(Type t)
    {
    	if(!(t instanceof TypeArray)) return 0;
    	final Type baseType=((TypeArray)t).getBase();
    	if(baseType instanceof TypePrimitive)
    		return getWordsize(baseType);
    	else
    		return getArrayWordsize(baseType);
    }

    private static int getBitlength(TypeArray t)
    {
    	assert(isBitType(t.getBase()));
    	Integer i = t.getLength().getIValue();
    	assert i != null : "Array lengths should be computable at compile time";
    	return i.intValue();
    }

	private Type convertType(Type type)
	{
		if(type instanceof TypeArray) {
			TypeArray array=(TypeArray)type;
			Type baseType=array.getBase();
			if(baseType instanceof TypeArray) {
				Type newBase=convertType(baseType);
				if(newBase==baseType) return type;
                return new TypeArray(newBase, array.getLength(), array.getMaxlength());
			}
			if(isBitType(baseType)) {
				Expression len=array.getLength();
				Integer ival = len.getIValue();
				if(ival != null) {
					int val=ival.intValue();
					Type base = TypePrimitive.int32type;
					int sz = 32;
				 if(val<=8)
					{	base = TypePrimitive.int8type; sz = 8; }
					else if(val<=16)
					{	base = TypePrimitive.int16type; sz = 16; }
					else if(val<=32)
					{	base = TypePrimitive.int32type; sz = 32; }
					/*else if(val<=64)
					{	base = TypePrimitive.int64type; sz = 64; } */
					//ok, looks like we'll need an array to store the large integer
					return new TypeArray(base,
							new ExprConstInt(len,((val-1)/sz)+1));
				}
			}
		}
		else if(isBitType(type)) {
            return TypePrimitive.bittype;
		}
		else if(type.equals(TypePrimitive.inttype))
			return TypePrimitive.siginttype;
		//fallback case: do nothing
		return type;
	}

    private Type getVarType(Expression e)
    {
    	if(e instanceof ExprVar) {
            return symtab.lookupVar(((ExprVar) e).getName(), e);
    	}
    	else if(e instanceof ExprArrayRange) {
    		return getVarType(((ExprArrayRange)e).getBase());
    	}
    	return null;
    }

    public Type getPType(Expression expr)
    {
        // To think about: should we cache GetExprType objects?
        GetExprType get = new GetExprType(preSymtab, nres);
        Type type = (Type)expr.accept(get);
        return (type);
    }

    private Type getVarPType(Expression e)
    {
    	if(e instanceof ExprVar) {
            return preSymtab.lookupVar(((ExprVar) e).getName(), e);
    	}
    	else if(e instanceof ExprArrayRange) {
    		return getVarPType(((ExprArrayRange)e).getBase());
    	}
    	return null;
    }

    private static Integer staticEvaluation(Expression e)
    {
    	if(e instanceof ExprConstInt)
    		return ((ExprConstInt)e).getVal();
    	return null;
    }

    private LinearRange analyzeRange(RangeLen range) {
    	Expression start=range.start();
    	if(start instanceof ExprBinary) {
    		ExprBinary binExp=(ExprBinary) start;
    		if(binExp.getOp()==ExprBinary.BINOP_ADD) {
    			if(binExp.getLeft() instanceof ExprBinary && ((ExprBinary)binExp.getLeft()).getOp()==ExprBinary.BINOP_MUL) {
    				LinearRange subrange=analyzeRange(new RangeLen(binExp.getLeft(),range.getLenExpression()));
    				if(subrange!=null && binExp.getRight() instanceof ExprConstInt) {
    					subrange.startOffset+=((ExprConstInt)binExp.getRight()).getVal();
    					return subrange;
    				}
    			}
    			else if(binExp.getRight() instanceof ExprBinary && ((ExprBinary)binExp.getRight()).getOp()==ExprBinary.BINOP_MUL) {
    				LinearRange subrange=analyzeRange(new RangeLen(binExp.getRight(),range.getLenExpression()));
    				if(subrange!=null && binExp.getLeft() instanceof ExprConstInt) {
    					subrange.startOffset+=((ExprConstInt)binExp.getLeft()).getVal();
    					return subrange;
    				}
    			}
    		}
    		else if(binExp.getOp()==ExprBinary.BINOP_MUL) {
				if(binExp.getLeft() instanceof ExprConstInt && binExp.getRight() instanceof ExprConstInt) {
					return new LinearRange(0,null,((ExprConstInt)binExp.getLeft()).getVal()*((ExprConstInt)binExp.getRight()).getVal(),range.getLenExpression().getIValue());
				}
				else if(binExp.getLeft() instanceof ExprConstInt) {
					return new LinearRange(((ExprConstInt)binExp.getLeft()).getVal(),binExp.getRight(),0,range.getLenExpression().getIValue());
				}
				else if(binExp.getRight() instanceof ExprConstInt) {
					return new LinearRange(((ExprConstInt)binExp.getRight()).getVal(),binExp.getLeft(),0,range.getLenExpression().getIValue());
				}
    		}
    	}
    	return null;
    }

	@Override
	public Object visitFunction(Function func)
	{
		SymbolTable oldPST=preSymtab;
		preSymtab=new SymbolTable(preSymtab);
		boolean change=false;
		List newParams=new ArrayList(func.getParams().size()+1);
        for(Iterator iter = func.getParams().iterator(); iter.hasNext();)
        {
            Parameter param = (Parameter)iter.next();
            Type oldType=param.getType();
            preSymtab.registerVar(param.getName(),oldType);
            Type newType=convertType(oldType);
            if(oldType!=newType) {
            	change=true;
                newParams.add(new Parameter(param, param.getSrcTupleDepth(), newType,
                        param.getName(),
                        param.getPtype()));
            }
            else
            	newParams.add(param);
        }
        if(change)
            func = func.creator().params(newParams).create();
		Object ret=super.visitFunction(func);
		preSymtab=oldPST;
		return ret;
	}

	@Override
	public Object visitStmtVarDecl(StmtVarDecl stmt)
	{
		processingDeclaration=true;
		boolean change=false;
		int n=stmt.getNumVars();
		List<Type> types=new ArrayList<Type>(n+1);
		List<Expression> inits = new ArrayList<Expression>(n);
		for(int i=0;i<n;i++) {
			Type oldType=stmt.getType(i);
			preSymtab.registerVar(stmt.getName(i),oldType);
			Type newType=convertType(oldType);
			if(newType!=oldType) change=true;
			types.add(newType);
			Expression init = stmt.getInit(i);
			if( init != null  && init instanceof ExprArrayInit){
				init = convertArrayInit((ExprArrayInit)init, oldType, newType);
			}
			inits.add(init);
		}
		if(change)
			stmt=new StmtVarDecl(stmt,types,stmt.getNames(),inits);
		stmt=(StmtVarDecl) super.visitStmtVarDecl(stmt);
		processingDeclaration=false;
		return stmt;
	}

	private Expression convertArrayRange(ExprArrayRange oldExpr, Expression start, int len)
	{
		Expression base=oldExpr.getBase();
		assert base instanceof ExprVar;
		TypeArray pseudotype=(TypeArray) preSymtab.lookupVar((ExprVar)base);
		if(!isBitArrayType(pseudotype)) return (Expression) super.visitExprArrayRange(oldExpr);

		Type type=symtab.lookupVar((ExprVar)base);
		final boolean isArray=type instanceof TypeArray;
		int ws=(isArray)?getArrayWordsize(type):getWordsize(type);
		Expression index = this.doExpression(start);
		if(isArray) {
			base=new ExprArrayRange(base, base,
				new RangeLen(new ExprBinary(oldExpr,ExprBinary.BINOP_DIV,
						index, new ExprConstInt(oldExpr,ws)))
			);

		}
		//assert type instanceof TypePrimitive;
		if(len>1) {
			throw new UnsupportedOperationException("Cannot deal with array ranges at this point in the code. It should have been handled earlier.");
		}
		else {
			if(isArray) {
				index=new ExprBinary(oldExpr,ExprBinary.BINOP_MOD,
						index, new ExprConstInt(oldExpr,ws)
				);
			}
			Expression sel=new ExprBinary(oldExpr,ExprBinary.BINOP_BAND,
				new ExprBinary(oldExpr,ExprBinary.BINOP_RSHIFT,
					base,index),
				new ExprConstInt(oldExpr,1)
			);
			return sel;
		}
	}

	public Object visitExprArrayRange(ExprArrayRange exp)
	{
		
		{
			RangeLen range=exp.getSelection();
			return convertArrayRange(exp,range.start(),range.getLenExpression().getIValue());
		}		
		//throw new UnsupportedOperationException("Cannot translate complicated array indexing.");
	}

	private Statement translateSingleWordAssignment(StmtAssign stmt, Expression lhsBase, Type lhsType, int ws, Expression index, int len)
	{
		return translateSingleWordAssignment(stmt, lhsBase, lhsType, ws, index, len, null);
	}

	private Statement translateSingleWordAssignment(StmtAssign stmt, Expression lhsBase, Type lhsType, int ws, Expression index, int len, Expression end)
	{
		final boolean isArray=lhsType instanceof TypeArray; //true if we're dealing with an array of integers (big integer); false if a single integer

		//first compute the "new" lhs expression (the word that will be modified)
		final Expression newLhs;
		if(isArray) {
			Expression wsExp=new ExprConstInt(index,ws);
			newLhs=new ExprArrayRange(stmt, lhsBase,new RangeLen(
				new ExprBinary(index,ExprBinary.BINOP_DIV,index,wsExp)
			));
		}
		else {
			//lhs is an array that fits into a single word
			newLhs=lhsBase;
		}
		//then the new rhs
		Expression newRhs=(Expression) stmt.getRHS().accept(this);

		final Type rhsType = getType(stmt.getRHS());
		if( rhsType instanceof TypeArray ){
			newRhs=new ExprArrayRange(stmt, newRhs,new RangeLen(
					new ExprConstInt(stmt,0)));
		}



		if(len==1)
		{
			final Expression bitSelector;
			if(isArray) {
				Expression wsExp=new ExprConstInt(index,ws);
				bitSelector=new ExprBinary(index,ExprBinary.BINOP_MOD,index,wsExp);
			}
			else {
				bitSelector=index;
			}
			return new StmtExpr(new ExprFunCall(stmt,"SK_BITASSIGN",Arrays.asList(new Expression[] {
				newLhs,bitSelector,newRhs
			})));
		}

		if(isArray) {
			Expression wsExp=new ExprConstInt(index,ws);
			index=new ExprBinary(index,ExprBinary.BINOP_MOD,index,wsExp);
		}

		final Expression bitMask;
		if(len<=0) {
			bitMask=new ExprFunCall(stmt,"SK_ONES_SE",Arrays.asList(new Expression[] {
					index,end
				}));
		}
		else {
			bitMask=new ExprFunCall(stmt,"SK_ONES_SL",Arrays.asList(new Expression[] {
				index,new ExprConstInt(index,len)
			}));
		}
		final String tempName=varGen.nextVar();
		if(lhsType instanceof TypeArray){
			lhsType = ((TypeArray)lhsType).getBase();
		}
		addStatement(new StmtVarDecl(stmt,lhsType,tempName,bitMask));

		return new StmtExpr(new ExprFunCall(stmt,"SK_COPYBITS",Arrays.asList(new Expression[] {
			newLhs,new ExprVar(index,tempName),
			new ExprBinary(index,ExprBinary.BINOP_LSHIFT,newRhs,index)
		})));
	}

	private Statement makeForLoop(String var,Expression ubound, Statement body)
	{
		return makeForLoop(var,new ExprConstInt(body,0),ubound,body);
	}

	private Statement makeForLoop(String var,Expression lbound, Expression ubound, Statement body)
	{
		Statement init=new StmtVarDecl(body,
			Collections.singletonList(TypePrimitive.siginttype),
			Collections.singletonList(var),
			Collections.singletonList(lbound));
		Statement incr=new StmtExpr(new ExprUnary(body,ExprUnary.UNOP_POSTINC,
			new ExprVar(body,var)));
		Expression cond=new ExprBinary(body,ExprBinary.BINOP_LT,
			new ExprVar(body,var),ubound);
        return new StmtFor(body, init, cond, incr, body, false);
	}

	private long computeMask(ExprArrayInit exp, int word, int ws)
	{
		List<Expression> bits=exp.getElements();
		int nb=bits.size();
		long mask=0;
		long p=1;
		for(int i=word*ws;i<nb && i<(word+1)*ws;i++,p*=2) {
			long bit=((ExprConstInt)bits.get(i)).getVal();
			assert(bit==0 || bit==1);
			mask=mask+bit*p;
		}
		return mask;
	}



	private ExprArrayInit convertArrayInit(ExprArrayInit exp, Type oldType, Type newType){
		if( isBitArrayType(oldType)){
			int ws = getArrayWordsize(newType);
			List<Expression> oldElems = exp.getElements();
			List<Expression> newElems = new ArrayList<Expression>((oldElems.size()-1)/ws + 1);
			for(int i=0; i<oldElems.size(); i += ws){
				newElems.add( makeHexString(exp, i/ws, ws));
			}
			return new ExprArrayInit( exp, newElems );
		}else{
			return exp;
		}
	}


	private Expression makeHexString(ExprArrayInit exp, int word, int ws)
	{
		long mask=computeMask(exp,word,ws);
		if(mask<=32) return new ExprConstInt(exp,(int)mask);
		Expression val=new ExprLiteral(exp,"0x"+Long.toHexString(mask)+(ws==64?"ull":"u"));
		return val;
	}

	private Statement callSK_bitArrayCopyInv(Expression lhs, Expression start, int len, Expression rhs, int ws, int rhws, Expression lhsLen  ){
		// len == rhsws && lhsws == lhslen && start % rhsws == 0;
		FENode context = lhs;
		boolean cond = (len % rhws) == 0;
		Integer lhsLenInt = lhsLen.getIValue();
		cond = cond && (lhsLenInt != null && ws*(len/rhws) == lhsLenInt.intValue());
		Expression smodRWS = new ExprBinary(context, ExprBinary.BINOP_MOD, start, new ExprConstInt(context, rhws) );
		Integer smodInt = smodRWS.getIValue();
		cond = cond && (smodInt != null && smodInt.intValue() == 0);
		if(cond){
			if( len == rhws ){
			Expression zero = ExprConstInt.zero;
			Expression lhsNew = new ExprArrayRange(lhs, lhs,new RangeLen(zero)); ;// lhs[0];
			Expression sdivRWS = new ExprBinary(context, ExprBinary.BINOP_DIV, start, new ExprConstInt(context, rhws) );
			Expression rhsNew =new ExprArrayRange(rhs, rhs,new RangeLen(sdivRWS)); ; // rhs[start/rhsws];
			return new StmtAssign(lhsNew, rhsNew); // lhsNew = rhsNew;
			}else{
				int sz = len/rhws;
				String var=varGen.nextVar();
				ExprVar evar = new ExprVar(context,var);
				Expression lhsNew = new ExprArrayRange(lhs, lhs,new RangeLen(evar));// lhs[0];
				Expression sdivRWS = new ExprBinary(context, ExprBinary.BINOP_DIV, start, new ExprConstInt(context, rhws) );
				sdivRWS = new ExprBinary(context, ExprBinary.BINOP_ADD, sdivRWS, evar );
				Expression rhsNew =new ExprArrayRange(rhs, rhs,new RangeLen(sdivRWS)); ; // rhs[start/rhsws];
				Statement body= new StmtAssign(lhsNew, rhsNew); // lhsNew = rhsNew;
				return makeForLoop(var, new ExprConstInt(sz) ,body);
			}
		}

		Expression end = new ExprBinary(context,ExprBinary.BINOP_ADD,start,new ExprConstInt(context,len-1));
		return callSK_bitArrayCopyInv(lhs, start, end, rhs, ws, rhws, lhsLen);
	}
	private Statement callSK_bitArrayCopyInv(Expression lhs, Expression start, Expression end, Expression rhs, int ws, int rhws, Expression lhsLen  ){
		FENode context = lhs;
		return new StmtExpr(new ExprFunCall(lhs,"SK_bitArrayCopyInv",Arrays.asList(new Expression[] {
				lhs,start,end,rhs,new ExprConstInt(context,ws), new ExprConstInt(context,rhws), lhsLen
			})));
	}

	private Statement callSK_bitArrayCopy(Expression lhs, Expression start, int len, Expression rhs, int ws){
		// len == rhsws &&  start % rhsws == 0;
		FENode context = lhs;
		boolean cond = len == ws;
		Expression smodRWS = new ExprBinary(context, ExprBinary.BINOP_MOD, start, new ExprConstInt(context, ws) );
		Integer smodInt = smodRWS.getIValue();
		cond = cond && (smodInt != null && smodInt.intValue() == 0);
		if(cond){
			Expression zero = ExprConstInt.zero;
			Expression sdivRWS = new ExprBinary(context, ExprBinary.BINOP_DIV, start, new ExprConstInt(context, ws) );
			Expression lhsNew = new ExprArrayRange(lhs, lhs,new RangeLen(sdivRWS)); ;// lhs[0];
			Expression rhsNew =new ExprArrayRange(rhs, rhs,new RangeLen(zero)); ; // rhs[start/rhsws];
			return new StmtAssign(lhsNew, rhsNew); // lhsNew = rhsNew;
		}
		Expression end = new ExprBinary(context,ExprBinary.BINOP_ADD,start,new ExprConstInt(context,len-1));
		return callSK_bitArrayCopy(lhs, start, end, rhs, ws);
	}

	private Statement callSK_bitArrayCopy(Expression lhs, Expression start, Expression end, Expression rhs, int ws){
		FENode context = lhs;
		return new StmtExpr(new ExprFunCall(context,"SK_bitArrayCopy",Arrays.asList(new Expression[] {
				lhs,start,end,rhs,new ExprConstInt(context,ws)
			})));
	}



	@Override
	public Object visitStmtAssign(StmtAssign stmt)
	{
		final Expression lhs=stmt.getLHS(); //either a variable or an [indexed] array
		final Type lhsPType=getVarPType(lhs);
		if(lhsPType == null){
			System.out.println("AAAARGH");
			final Type lhsPType2=getVarPType(lhs);
		}
		assert lhsPType!=null;
		if(!isBitArrayType(lhsPType)) {
//				if(stmt.getRHS() instanceof ExprArrayInit) return stmt;
			return super.visitStmtAssign(stmt);
		}

		final Type lhsType=getVarType(lhs);
		assert lhsType!=null;
		int ws=(lhsType instanceof TypeArray)?getArrayWordsize(lhsType):getWordsize(lhsType);
		assert ws>1;

		//assume RHS contains no arithmetic/bitwise operations when LHS is an array range
		if(lhs instanceof ExprArrayRange)
		{
			final ExprArrayRange expArray=(ExprArrayRange) lhs;
			if(expArray.hasSingleIndex()) {
				// special case: a[i]=x --> always affects a single word; no loops
				Expression index=expArray.getSingleIndex();
				return translateSingleWordAssignment(stmt,expArray.getBase(),lhsType,ws,index,1);
			}
			else {
				// general case: a[i::j]=x
			    RangeLen ro=expArray.getSelection();				
				{
					RangeLen range=(RangeLen) ro;
	                int len = range.getLenExpression().getIValue();
					if(!((lhsType instanceof TypeArray)))
						return translateSingleWordAssignment(stmt,expArray.getBase(),lhsType,ws,range.start(),len);

					if(staticEvaluation(range.start())!=null) {
						int start=staticEvaluation(range.start()).intValue();					
						int end=start+len-1;
						if(start/ws==end/ws) //special case: the entire range is contained within a word
							return translateSingleWordAssignment(stmt,expArray.getBase(),lhsType,ws,range.start(),len);
					}
					return callSK_bitArrayCopy(expArray.getBase(), range.start(), len, stmt.getRHS(), ws);
				}				
			}
		}
		else if(stmt.getRHS() instanceof ExprArrayRange)
		{
			final ExprArrayRange expArray=(ExprArrayRange) stmt.getRHS();
			if(expArray.hasSingleIndex()) {
				return super.visitStmtAssign(stmt);
			}
			else
			{
				final Type rhsType=getVarType(expArray);
				assert rhsType!=null;
				int rhws=(rhsType instanceof TypeArray)?getArrayWordsize(rhsType):getWordsize(rhsType);
				assert rhws>1;

				
				{
					RangeLen range=expArray.getSelection();
//					LinearRange lr=analyzeRange(range);
//					if(lr!=null && lr.multiplier==ws)
//					{
//						if(lr.startOffset+lr.len<=ws)
//						{
//							if(lr.startOffset==0 && lr.len==ws) {
//								return new StmtAssign(stmt,lhs,new ExprArrayRange(
//									expArray.getBase(),Collections.singletonList(new RangeLen(lr.count))
//								));
//							}
//						}
//					}
					if(staticEvaluation(range.start())!=null) {
						int start=staticEvaluation(range.start()).intValue();
						int len = range.getLenExpression().getIValue();
						int end=start+len-1;
						if(start/rhws==end/rhws && len<=ws) //special case: the entire range is contained within a word (and fits in the lhs word)
						{
							Expression newRhs;
							final boolean isArrayRHS=rhsType instanceof TypeArray; //true if we're dealing with an array of integers (big integer); false if a single integer
							final boolean isArrayLHS=lhsType instanceof TypeArray;
							{
								//compute the "new" rhs expression (the word that we will read from)
								if(isArrayRHS) {
									Expression wsExp=new ExprConstInt(rhws);
									newRhs=new ExprArrayRange(expArray, expArray.getBase(),new RangeLen(
										new ExprBinary(stmt,ExprBinary.BINOP_DIV,new ExprConstInt(start),wsExp)
									));
								}else {
									//lhs is an array that fits into a single word
									newRhs=expArray.getBase();
								}
								if(start%rhws!=0)
									newRhs=new ExprBinary(newRhs,ExprBinary.BINOP_RSHIFT,newRhs,new ExprConstInt(start%rhws));
							}

							if( end-start < ws ){
								final Expression bitMask;
									bitMask=new ExprFunCall(stmt,"SK_ONES",Arrays.asList(new Expression[] {
											new ExprConstInt(end-start+1)
										}));
									newRhs=new ExprBinary(newRhs,ExprBinary.BINOP_BAND ,newRhs,bitMask);
							}


							Expression zero = ExprConstInt.zero;
							Expression newLhs = lhs;
							{
								if(isArrayLHS) {
									Expression wsExp=new ExprConstInt(lhs,ws);
									newLhs=new ExprArrayRange(lhs, lhs,new RangeLen(zero));
								}
							}
							return new StmtAssign(newLhs,newRhs);
						}
					}
					return callSK_bitArrayCopyInv(stmt.getLHS(), range.start(), range.getLenExpression().getIValue(), expArray.getBase(), ws, rhws, ((TypeArray)lhsPType).getLength());
				}
				
			}
		}
		else if(lhs instanceof ExprVar)
		{
			if(stmt.getRHS() instanceof ExprVar)
			{
				if(lhsType instanceof TypeArray)
				{
					Expression rhs = stmt.getRHS();
					final Type rhsPType=getVarPType(rhs);
					assert rhsPType!=null;
					final Type rhsType=getVarType(rhs);
					Expression lengthLHS=((TypeArray)lhsType).getLength();
					Expression lengthpLHS=((TypeArray)lhsPType).getLength();

					if( rhsPType instanceof TypeArray ){
						//Expression lengthRHS=((TypeArray)rhsType).getLength();
						Expression lengthpRHS=((TypeArray)rhsPType).getLength();
						Integer llen = lengthpLHS.getIValue();

						assert rhsType!=null;
						int rhws=(rhsType instanceof TypeArray)?getArrayWordsize(rhsType):getWordsize(rhsType);
						assert rhws>1;

						if(rhsPType.equals(lhsPType) && llen != null && llen.intValue() % ws == 0){
							String var=varGen.nextVar();
							Statement body=new StmtAssign(
								new ExprArrayRange(lhs, lhs,new RangeLen(new ExprVar(stmt,var))),
								new ExprArrayRange(stmt, stmt.getRHS(),new RangeLen(new ExprVar(stmt,var)))
							);
							return makeForLoop(var,lengthLHS,body);
						}else{
							return callSK_bitArrayCopyInv(stmt.getLHS(), ExprConstInt.zero, lengthpRHS, stmt.getRHS(), ws, rhws, lengthpLHS);
						}
					}else{
						assert false : "NYI";
					}
				}
			}
			else if(stmt.getRHS() instanceof ExprConstInt)
			{
				int value=((ExprConstInt)stmt.getRHS()).getVal();
				if(lhsType instanceof TypeArray)
				{
					Expression length=((TypeArray)lhsType).getLength();
					String var=varGen.nextVar();
					Expression arrayElem=new ExprArrayRange(lhs, lhs,new RangeLen(new ExprVar(stmt,var)));
					Expression zero=new ExprConstInt(stmt,0);
					String allones="0x"+Long.toHexString((1<<ws)-1)+(32>=64?"ull":"u");
					Expression one=new ExprLiteral(stmt,allones);

					if(value==0) {
						Statement body=new StmtAssign(arrayElem,zero);
						return makeForLoop(var,length,body);
					}
					else {
						//Expression one=new ExprConstInt(stmt,1);
						//Expression firstElem=new ExprArrayRange(lhs,Collections.singletonList(new RangeLen(new ExprConstInt(stmt,0))));
						//addStatement(new StmtAssign(stmt,firstElem,stmt.getRHS()));
						Statement body=new StmtAssign(arrayElem,one);
						return makeForLoop(var,length,body);
					}
				}else{
					int len = getBitlength( (TypeArray) lhsPType  );
					String allones="0x"+Long.toHexString((1<<len)-1)+(len>=32?"ull":"u");
					Expression cval=new ExprLiteral(stmt,allones);
					if( value == 1){
						Statement body=new StmtAssign(lhs,cval);
						return body;
					}else{
						return super.visitStmtAssign(stmt);
					}

				}
			}
			else if(stmt.getRHS() instanceof ExprArrayInit)
			{
				ExprArrayInit exp=(ExprArrayInit) stmt.getRHS();
				if(exp.getDims()!=1) throw new UnsupportedOperationException("Cannot handle multidimensional array initializers");
				int nb=exp.getElements().size();
				if(nb<=ws) {
					Expression left = lhs;
					if(lhsType instanceof TypeArray)
					{
						Expression zero=new ExprConstInt(stmt,0);
						left = new ExprArrayRange(left, left,new RangeLen(zero));
					}
					Expression val=makeHexString(exp,0,ws);
					return new StmtAssign(left,val);
				}
				else {
					assert lhsType instanceof TypeArray: "This can't happen";
					String name=((ExprVar)lhs).getName();
					for(int k=0;k<nb/ws;k++) {
						Expression elem=new ExprArrayRange(lhs, new ExprVar(lhs,name),new RangeLen(new ExprConstInt(lhs,k)));
						Expression val=makeHexString(exp,k,ws);
						addStatement(new StmtAssign(elem,val));
					}
					return null;
				}
			}
			else if(stmt.getRHS() instanceof ExprBinary)
			{
				final ExprBinary binExp=(ExprBinary)stmt.getRHS();
				if(lhsType instanceof TypeArray)
				{
					//it's something like a=b+c where a,b,c are arrays

					switch(binExp.getOp()) {
						case ExprBinary.BINOP_BAND:
						case ExprBinary.BINOP_BOR:
						case ExprBinary.BINOP_BXOR:
						{
							if(lhsType instanceof TypeArray){
								Expression length=((TypeArray)lhsType).getLength();
								assert ((TypeArray)lhsPType).getLength().getIValue().intValue() % ws == 0;
								String var=varGen.nextVar();
								Expression indexVar = new ExprVar(stmt,var);
								Expression newBinExp = (new Indexify(indexVar, ws)).indexify(binExp);
								Statement body=new StmtAssign(
									new ExprArrayRange(lhs, lhs,new RangeLen(new ExprVar(stmt,var))),
									newBinExp
								);
								return makeForLoop(var,length,body);
							}else{
								return super.visitStmtAssign(stmt);
							}
						}
						case ExprBinary.BINOP_ADD:
						case ExprBinary.BINOP_SUB:
						case ExprBinary.BINOP_MUL:
						case ExprBinary.BINOP_LSHIFT:
						case ExprBinary.BINOP_RSHIFT:
						{
							assert lhsType instanceof TypeArray && ((TypeArray)lhsType).getLength().getIValue() == 1 : "NYI";
							int pad=ws-getBitlength((TypeArray) lhsPType);
							Expression zero=new ExprConstInt(stmt,0);
							Expression lhsZero = new ExprArrayRange(lhs, lhs,new RangeLen(zero));
							Expression left = doExpression(binExp.getLeft());
							Expression right = doExpression(binExp.getRight());
							if( getType(left) instanceof TypeArray){
								left = new ExprArrayRange(left, left,new RangeLen(zero));
							}
							if( getType(right) instanceof TypeArray){
								right = new ExprArrayRange(right, right,new RangeLen(zero));
							}

							Statement body=new StmtAssign(
									lhsZero,
									new ExprBinary(binExp.getOp(),
										left, right)
							);
							addStatement(body);
							if(pad>0){
								return new StmtAssign(stmt,lhsZero,new ExprLiteral(stmt,"0x"+Long.toHexString((1<<(ws-pad))-1)),ExprBinary.BINOP_BAND);
							}else{
								return null;
							}
						}

						default :return super.visitStmtAssign(stmt);
					}
				}
			}else if(stmt.getRHS() instanceof ExprUnary){
				final ExprUnary unExp=(ExprUnary)stmt.getRHS();
				if(lhsType instanceof TypeArray)
				{
					//it's something like a=b+c where a,b,c are arrays
					Expression length=((TypeArray)lhsType).getLength();
					assert ((TypeArray)lhsPType).getLength().getIValue().intValue() % ws == 0;
					assert unExp.getOp() == ExprUnary.UNOP_NOT || unExp.getOp() == ExprUnary.UNOP_BNOT : "Not yet implemented";
					String var=varGen.nextVar();
					Statement body=new StmtAssign(
							new ExprArrayRange(lhs, lhs,new RangeLen(new ExprVar(stmt,var))),
							new ExprUnary(stmt,ExprUnary.UNOP_BNOT,
								new ExprArrayRange(unExp, unExp.getExpr(),new RangeLen(new ExprVar(stmt,var)))
							)
						);
					return makeForLoop(var,length,body);
				}
			}else if(stmt.getRHS() instanceof ExprTypeCast){
				if(lhsType instanceof TypeArray)
				{
					Expression rhs = stmt.getRHS();
					Expression rhsBase = ((ExprTypeCast) rhs).getExpr();

					final Type rhsPType= ((ExprTypeCast) rhs).getType();
					assert rhsPType!=null;
					final Type rhsType= convertType(rhsPType);
					Expression rhsNewCast = new ExprTypeCast(stmt, rhsType, rhsBase  );
					Expression lengthLHS=((TypeArray)lhsType).getLength();
					Expression lengthpLHS=((TypeArray)lhsPType).getLength();
					assert rhsType!=null;
					int rhws=(rhsType instanceof TypeArray)?getArrayWordsize(rhsType):getWordsize(rhsType);
					assert rhws>1;
					if( rhsPType instanceof TypeArray ){
						//Expression lengthRHS=((TypeArray)rhsType).getLength();
						Expression lengthpRHS=((TypeArray)rhsPType).getLength();
						Integer llen = lengthpLHS.getIValue();
						if(rhsPType.equals(lhsPType) && llen != null && llen.intValue() % ws == 0){
							String var=varGen.nextVar();
							Statement body=new StmtAssign(
								new ExprArrayRange(lhs, lhs,new RangeLen(new ExprVar(stmt,var))),
								new ExprArrayRange(rhsNewCast, rhsNewCast,new RangeLen(new ExprVar(stmt,var)))
							);
							return makeForLoop(var,lengthLHS,body);
						}else{
							return callSK_bitArrayCopyInv(stmt.getLHS(), ExprConstInt.zero,lengthpRHS, rhsNewCast, ws, rhws, lengthpLHS);
						}
					}else{
						assert false : "NYI";
					}
				}
			}
		}

		return super.visitStmtAssign(stmt);
	}

	 public Object visitExprTypeCast(ExprTypeCast exp)
    {
        Expression expr = doExpression(exp.getExpr());
        if(getType(expr) instanceof TypeArray){
        	Expression zero=new ExprConstInt(exp,0);
        	expr = new ExprArrayRange(expr, expr,new RangeLen(zero));
        }
        if (expr == exp.getExpr())
            return exp;
        else
            return new ExprTypeCast(exp, exp.getType(), expr);
    }

	@Override
	public Object visitExprArrayInit(ExprArrayInit exp)
	{
		if(processingDeclaration)
			return exp;
		else { //it's "??" that stands for an integer constant; compute it
			assert(exp.getElements().size()<=64);
			return makeHexString(exp,0,64);
		}
	}

}
