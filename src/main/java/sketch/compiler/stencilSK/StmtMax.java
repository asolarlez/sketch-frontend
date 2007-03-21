package streamit.frontend.stencilSK;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import streamit.frontend.nodes.*;


/**
 * Represents a max statement of the form:
 * int[dim] lhsvar = max{ primPred(lhsvar) && secPred(lhsvar) };
 */
public class StmtMax extends Statement{
	public static String TMP_VAR = "_tmp";
	int dim;
	String lhsvar;	
	String indvar;
	/**
	 * Primary constraints.
	 * They are of the form expr(idx)==t
	 */
	List<Expression> primC;
	/**
	 * Secondary constraints.
	 * They are of the form idx < something
	 */
	List<Expression> secC;
	/**
	 * Tertiary constraints.
	 * They are pretty arbitrary.
	 */
	List<Expression> terC;
	
	
	/**
	 * If the StmtMax was successfully converted to a set of assignments, 
	 * these will be stored in the maxAssign field.
	 */
	Statement maxAssign = null;
	
	
	public Statement toAST(){
		if( maxAssign != null)
			return maxAssign;
		return this;
	}
	
	public Object accept(FEVisitor visitor){
		assert false;
		return null;
	}
	StmtMax(int dim, String lhsvar, String indvar){super(null);
		this.indvar = indvar;
		this.dim = dim;
		this.lhsvar = lhsvar;
		this.primC = new ArrayList<Expression>();
		this.secC = new ArrayList<Expression>();
		this.terC = new ArrayList<Expression>();
	}
	String getVar(){ assert false; return null;}
	public String toString(){
		if( maxAssign != null )
			return maxAssign.toString();
		String rv = "int[" + dim + "] " + lhsvar + "= max{";
		for(Iterator<Expression> eit = primC.iterator(); eit.hasNext(); ){
			rv += eit.next().toString() + " & ";
		}
		for(Iterator<Expression> eit = secC.iterator(); eit.hasNext(); ){
			rv += eit.next().toString() + " & ";
		}
		for(Iterator<Expression> eit = terC.iterator(); eit.hasNext(); ){
			rv += eit.next().toString() + " & ";
		}
		rv += "}; \n";
		rv += "bit " + indvar + " = " + lhsvar + " != null";
		return rv;
	}
	
	private Statement ltStmtAssign(List<Expression> elist, ExprArrayRange ea, int idx){
		assert elist.size() != 0;		
		if(elist.size() == 1){			
			return new StmtAssign(null, ea, elist.get(0));
		}
		int sz = elist.size();
		List<Statement> statements = new ArrayList<Statement>();
		String lname = this.lhsvar + idx + "_tmp";
		ExprVar base = new ExprVar(null, lname);
		statements.add(  new StmtVarDecl(null, new TypeArray( TypePrimitive.inttype, new ExprConstInt(sz) ), lname, null) );
		
		{	int i=0;
			for(Iterator<Expression> it = elist.iterator(); it.hasNext(); ++i ){
				Expression exp = it.next();
				ExprArrayRange lea = new ExprArrayRange(base, new ExprConstInt(i));
				StmtAssign ass = new StmtAssign(null, lea, exp);
				statements.add(ass);
			}
		}
		for(int i=1; i<sz; ++i){
			// lname[i] = lname[i]<lname[i-1]? lname[i] : lname[i-1];
			ExprArrayRange lhs = new ExprArrayRange(base, new ExprConstInt(i));
			ExprArrayRange rhsi = new ExprArrayRange(base, new ExprConstInt(i));
			ExprArrayRange rhsim1 = new ExprArrayRange(base, new ExprConstInt(i-1));
			ExprArrayRange rhsip = new ExprArrayRange(base, new ExprConstInt(i));
			ExprArrayRange rhsim1p = new ExprArrayRange(base, new ExprConstInt(i-1));
			ExprBinary comp = new ExprBinary( null, ExprBinary.BINOP_LT, rhsi, rhsim1);
			ExprTernary ter = new ExprTernary(null, ExprTernary.TEROP_COND, comp, rhsip, rhsim1p);
			StmtAssign ass = new StmtAssign(null, lhs, ter);
			statements.add(ass);
		}
		StmtAssign ass = new StmtAssign(null, ea,  new ExprArrayRange(base, new ExprConstInt(sz-1)));
		statements.add( ass  );		
		return new StmtBlock(null, statements);
	}
	
	// v1 < v2; 
    public Expression lexCompare(ExprVar v1, ExprVar v2, int [] subs, int jj, int N){
    	assert jj<N;    	
		ExprArrayRange aa1 = new ExprArrayRange(null, v1, new ExprConstInt(subs[jj]));
		ExprArrayRange aa2 = new ExprArrayRange(null, v2, new ExprConstInt(subs[jj]));
		Expression tmp = new ExprBinary(null,ExprBinary.BINOP_LT, aa1, aa2);
		//tmp = idx[2*jj+1] < par_jj
		Expression eq =  new ExprBinary(null,ExprBinary.BINOP_EQ, aa1,aa2);
		Expression out;
		if( jj+1 < N){
			Expression andExp = new ExprBinary(null, ExprBinary.BINOP_AND, eq, lexCompare(v1, v2, subs, jj+1, N));
			out = new ExprBinary(null, ExprBinary.BINOP_OR, tmp, andExp);
		// out = tmp || (eq &&  buildSecondaryConstr(iterIt))
		}else{
			out = tmp;
		// out = tmp;
		}
		return out;
    }

	
	
	/**
	 * Calls the symbolic solver to produce a sequence of gurarded assignments
	 * to replace the max statement.
	 * 
	 * @return whether or not the symbolic solver succeeded in replacing the max.
	 */
	public boolean resolve(){
		ResolveMax rmax = new ResolveMax(this);
		rmax.run();
		rmax.removeDependencies();
		for(int i=0; i<dim; ++i){
			if( rmax.expArr[i] == null && ( rmax.meArr[i] == null || rmax.tainted[i]!= null ) )
				return false;
		}
		List<Statement> statements = new ArrayList<Statement>();		
		ExprVar base = new ExprVar(null, this.lhsvar);
		
		
		// int lhsvar[dim];
		statements.add(  new StmtVarDecl(null, new TypeArray( TypePrimitive.inttype, new ExprConstInt(dim) ), this.lhsvar, null) );
		// int indvar; 
		statements.add(  new StmtVarDecl(null, TypePrimitive.bittype, this.indvar, null) );
		//First, we set all the entries that are known.		
		
		int size = 1;
		int[] multis = new int[dim]; 
		int multisSize = 0;
		for(int i=0; i<dim; ++i){			
			if( rmax.expArr[i] != null){
				ExprArrayRange ea = new ExprArrayRange(base, new ExprConstInt(i));	
				// lhsvar[i] =  rmax.expArr[i];
				StmtAssign ass = new StmtAssign(null, ea, rmax.expArr[i]);
				statements.add(ass);
			}else{
				ExprArrayRange ea = new ExprArrayRange(base, new ExprConstInt(i));	
				// lhsvar[i] =  0;
				StmtAssign ass = new StmtAssign(null, ea, new ExprConstInt(0));
				statements.add(ass);
				
				assert rmax.meArr[i].size() > 1;
				size = size * rmax.meArr[i].size();	
				multis[multisSize] = i;
				++multisSize;
			}
		}
		//Finally, we check that lhsvar satisfies the conditions.
		ExprVar indicator  = new ExprVar(null, this.indvar);
		Expression cond = null;
		for(Iterator<Expression> eit = primC.iterator(); eit.hasNext(); ){
			if( cond == null)
				cond = eit.next();
			else
				cond = new ExprBinary(null, ExprBinary.BINOP_AND, cond, eit.next());
		}
		for(Iterator<Expression> eit = secC.iterator(); eit.hasNext(); ){
			if( cond == null)
				cond = eit.next();
			else
				cond = new ExprBinary(null, ExprBinary.BINOP_AND, cond, eit.next());
		}
		List<Statement> slist = new ArrayList<Statement>();
		{
			StmtAssign ass = new StmtAssign(null, indicator,cond);
			slist.add(ass);
		}
		cond = null;
		for(Iterator<Expression> eit = terC.iterator(); eit.hasNext(); ){
			if( cond == null)
				cond = eit.next();
			else
				cond = new ExprBinary(null, ExprBinary.BINOP_AND, cond, eit.next());
		}
		if( cond != null){
			StmtAssign ass = new StmtAssign(null, indicator,cond);			
			StmtIfThen sit = new StmtIfThen(null, indicator, ass, null);
			slist.add(sit);
		}				
		Statement check = new StmtBlock(null, slist);		
		if(size > 1){
						 			
			ExprVar tmpvar  = new ExprVar(null, TMP_VAR);
			Statement check2 = (Statement) check.accept( new VarReplacer(this.lhsvar, tmpvar));
			
			
			List<Statement> aslist = new ArrayList<Statement>();
			for(int j=0; j<multisSize; ++j){
				ExprArrayRange ea = new ExprArrayRange(tmpvar, new ExprConstInt(multis[j]));
				ExprArrayRange ba = new ExprArrayRange(base, new ExprConstInt(multis[j]));
				StmtAssign ass = new StmtAssign(null, ba, ea);
				aslist.add(ass);				
			}
			// lhsvar[multis] = tmp[multis];
			Statement idxUpdate =  new StmtBlock(null, aslist);
			
			// lhsvar[multis] < tmp[multis]
			Expression lcomp = lexCompare(base, tmpvar, multis, 0, multisSize);			
			StmtIfThen lessthanif = new StmtIfThen(null, lcomp, idxUpdate , null);
			StmtIfThen indvif = new StmtIfThen(null, indicator, lessthanif  , null); 
			// indvif := 
			//     if( indvar ){ if( lhsvar[multis] < tmp[multis] ){ lhsvar[multis] = tmp[multis] } }						
			int[] current= new int[multisSize];
			for(int i=0; i<multisSize; ++i) current[i] = 0;
			Expression indTot = null;
			for(int i=0; i<size; ++i){
				List<Statement> blist = new ArrayList<Statement>();
				// int[dim] tmp = lhsvar;
				blist.add(  new StmtVarDecl(null, new TypeArray( TypePrimitive.inttype, new ExprConstInt(dim) ), TMP_VAR, base) );
				for(int j=0; j<multisSize; ++j){
					// tmp[multis[j]] = rmax.meArr[multis[j]].get( current[j] ); 
					ExprArrayRange ea = new ExprArrayRange(tmpvar, new ExprConstInt(multis[j]));	
					StmtAssign ass = new StmtAssign(null, ea, rmax.meArr[multis[j]].get( current[j] ));
					blist.add(ass);
				}
				Expression indI = new ExprVar(null, this.indvar+"_"+i) ;
				//This declaration goes outside the block.
				statements.add(  new StmtVarDecl(null, TypePrimitive.bittype, this.indvar+"_"+i, null) );
				// check2; i.e. assign to indvar;
				blist.add((Statement)check2.accept(new VarReplacer(this.indvar, indI)));				
				// if( indvar ){ if (tmp>idx) idx = tmp }
				blist.add((Statement)indvif.accept(new VarReplacer(this.indvar, indI )));
				
				int xx = 0;	boolean more = true;
				while(more && xx < multisSize){
					++current[xx];
					if( current[xx] >= rmax.meArr[multis[xx]].size()){
						current[xx] = 0;
						xx++;
					}else{
						more = false;
					}
				}
				assert !(xx == multisSize) || (i==size-1);
				Statement multiUpdate =  new StmtBlock(null, blist);
				statements.add(multiUpdate);
				if(indTot == null){
					indTot = indI;
				}else{
					indTot = new ExprBinary(null, ExprBinary.BINOP_OR, indI, indTot );
				}
			}
			statements.add(new StmtAssign(null, indicator,indTot)	);
		}else{
			statements.add(check);
		}

		maxAssign = new StmtBlock(null, statements);
		return true;
	}
	
	
}