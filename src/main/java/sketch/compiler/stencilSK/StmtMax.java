package sketch.compiler.stencilSK;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import sketch.compiler.ast.core.FEContext;
import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.FEVisitor;
import sketch.compiler.ast.core.exprs.ExprArrayRange;
import sketch.compiler.ast.core.exprs.ExprBinary;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprTernary;
import sketch.compiler.ast.core.exprs.ExprUnary;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtBlock;
import sketch.compiler.ast.core.stmts.StmtIfThen;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.stmts.StmtWhile;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.ast.core.typs.TypePrimitive;


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

	public static class vInfo{
	    Expression start;
	    Expression pred;
	    String var;
	    public vInfo(Expression start, Expression pred, String var){
	      this.start = start;
	      this.pred = pred;
	      this.var = var;
	    }
	}
	
	List<vInfo> vlist = new ArrayList<StmtMax.vInfo>();

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

    StmtMax(FENode ctxt, int dim, String lhsvar, String indvar) {
        super(ctxt);
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
			return new StmtAssign(ea, elist.get(0));
		}
		int sz = elist.size();
		List<Statement> statements = new ArrayList<Statement>();
		String lname = this.lhsvar + idx + "_tmp";
		ExprVar base = new ExprVar((FEContext) null, lname);
		statements.add(  new StmtVarDecl((FEContext) null, new TypeArray( TypePrimitive.inttype, new ExprConstInt(sz) ), lname, null) );

		{	int i=0;
			for(Iterator<Expression> it = elist.iterator(); it.hasNext(); ++i ){
				Expression exp = it.next();
				ExprArrayRange lea = new ExprArrayRange(base, new ExprConstInt(i));
				StmtAssign ass = new StmtAssign(lea, exp);
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
			ExprBinary comp = new ExprBinary( rhsi, "<", rhsim1);
			ExprTernary ter = new ExprTernary("?:", comp, rhsip, rhsim1p);
			StmtAssign ass = new StmtAssign(lhs, ter);
			statements.add(ass);
		}
		StmtAssign ass = new StmtAssign(ea,  new ExprArrayRange(base, new ExprConstInt(sz-1)));
		statements.add( ass  );
		return new StmtBlock((FEContext) null, statements);
	}

	// v1 < v2;
    public Expression lexCompare(ExprVar v1, ExprVar v2, int [] subs, int jj, int N){
    	assert jj<N;
		ExprArrayRange aa1 = new ExprArrayRange(v1, new ExprConstInt(subs[jj]));
		ExprArrayRange aa2 = new ExprArrayRange(v2, new ExprConstInt(subs[jj]));
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
	public boolean resolveBis(){
		ResolveMax rmax = new ResolveMax(this);
		rmax.run();
		rmax.removeDependencies();
		for(int i=0; i<dim; ++i){
			if( rmax.expArr[i] == null && ( rmax.meArr[i] == null || rmax.tainted[i]!= null ) )
				return false;
		}
		List<Statement> statements = new ArrayList<Statement>();
		ExprVar base = new ExprVar((FEContext) null, this.lhsvar);


		// int lhsvar[dim];
		statements.add(  new StmtVarDecl((FEContext) null, new TypeArray( TypePrimitive.inttype, new ExprConstInt(dim) ), this.lhsvar, null) );
		// int indvar;
		statements.add(  new StmtVarDecl((FEContext) null, TypePrimitive.bittype, this.indvar, null) );
		//First, we set all the entries that are known.

		int size = 1;
		int[] multis = new int[dim];
		int multisSize = 0;
		for(int i=0; i<dim; ++i){
			if( rmax.expArr[i] != null){
				ExprArrayRange ea = new ExprArrayRange(base, new ExprConstInt(i));
				// lhsvar[i] =  rmax.expArr[i];
				StmtAssign ass = new StmtAssign(ea, rmax.expArr[i]);
				statements.add(ass);
			}else{
				ExprArrayRange ea = new ExprArrayRange(base, new ExprConstInt(i));
				// lhsvar[i] =  0;
				StmtAssign ass = new StmtAssign(ea, ExprConstInt.zero);
				statements.add(ass);

				assert rmax.meArr[i].size() > 1;
				size = size * rmax.meArr[i].size();
				multis[multisSize] = i;
				++multisSize;
			}
		}
		//Finally, we check that lhsvar satisfies the conditions.
		ExprVar indicator  = new ExprVar((FEContext) null, this.indvar);
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
			StmtAssign ass = new StmtAssign(indicator,cond);
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
			StmtAssign ass = new StmtAssign(indicator,cond);
			StmtIfThen sit = new StmtIfThen(indicator, indicator, ass, null);
			slist.add(sit);
		}
		Statement check = new StmtBlock((FEContext) null, slist);
		if(size > 1){

			ExprVar tmpvar  = new ExprVar((FEContext) null, TMP_VAR);
			Statement check2 = (Statement) check.accept( new VarReplacer(this.lhsvar, tmpvar));


			List<Statement> aslist = new ArrayList<Statement>();
			for(int j=0; j<multisSize; ++j){
				ExprArrayRange ea = new ExprArrayRange(tmpvar, new ExprConstInt(multis[j]));
				ExprArrayRange ba = new ExprArrayRange(base, new ExprConstInt(multis[j]));
				StmtAssign ass = new StmtAssign(ba, ea);
				aslist.add(ass);
			}
			// lhsvar[multis] = tmp[multis];
			Statement idxUpdate =  new StmtBlock((FEContext) null, aslist);

			// lhsvar[multis] < tmp[multis]
			Expression lcomp = lexCompare(base, tmpvar, multis, 0, multisSize);
			StmtIfThen lessthanif = new StmtIfThen(lcomp, lcomp, idxUpdate , null);
			StmtIfThen indvif = new StmtIfThen(indicator, indicator, lessthanif  , null);
			// indvif :=
			//     if( indvar ){ if( lhsvar[multis] < tmp[multis] ){ lhsvar[multis] = tmp[multis] } }
			int[] current= new int[multisSize];
			for(int i=0; i<multisSize; ++i) current[i] = 0;
			Expression indTot = null;
			for(int i=0; i<size; ++i){
				List<Statement> blist = new ArrayList<Statement>();
				// int[dim] tmp = lhsvar;
				blist.add(  new StmtVarDecl((FEContext) null, new TypeArray( TypePrimitive.inttype, new ExprConstInt(dim) ), TMP_VAR, base) );
				for(int j=0; j<multisSize; ++j){
					// tmp[multis[j]] = rmax.meArr[multis[j]].get( current[j] );
					ExprArrayRange ea = new ExprArrayRange(tmpvar, new ExprConstInt(multis[j]));
					StmtAssign ass = new StmtAssign(ea, rmax.meArr[multis[j]].get( current[j] ));
					blist.add(ass);
				}
				Expression indI = new ExprVar((FEContext) null, this.indvar+"_"+i) ;
				//This declaration goes outside the block.
				statements.add(  new StmtVarDecl((FEContext) null, TypePrimitive.bittype, this.indvar+"_"+i, null) );
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
				Statement multiUpdate =  new StmtBlock((FEContext) null, blist);
				statements.add(multiUpdate);
				if(indTot == null){
					indTot = indI;
				}else{
					indTot = new ExprBinary(null, ExprBinary.BINOP_OR, indI, indTot );
				}
			}
			statements.add(new StmtAssign(indicator,indTot)	);
		}else{
			statements.add(check);
		}

		maxAssign = new StmtBlock((FEContext) null, statements);
		return true;
	}


	
	   /**
     * Calls the symbolic solver to produce a sequence of gurarded assignments
     * to replace the max statement.
     *
     * @return whether or not the symbolic solver succeeded in replacing the max.
     */
    public boolean resolve(){
        // TODO: change this to use AGMAX. TODO xzl
        ResolveMax rmax = new ResolveMax(this);
        rmax.run();
        rmax.removeDependencies();
        for(int i=0; i<dim; ++i){
            if( rmax.expArr[i] == null && ( rmax.meArr[i] == null || rmax.tainted[i]!= null ) )
                return false;
        }
        List<Statement> statements = new ArrayList<Statement>();
        ExprVar base = new ExprVar((FEContext) null, this.lhsvar);


        // int lhsvar[dim];
        statements.add(  new StmtVarDecl((FEContext) null, new TypeArray( TypePrimitive.inttype, new ExprConstInt(dim) ), this.lhsvar, null) );
        // int indvar;
        statements.add(  new StmtVarDecl((FEContext) null, TypePrimitive.bittype, this.indvar, ExprConstInt.zero) );
        //First, we set all the entries that are known.

        class ukvarInfo{
            ExprVar iv;
            Statement incr;
            Statement init;
            Expression cond;
            int i;
            Statement ass;
        }
        List<ukvarInfo> fl = new ArrayList<ukvarInfo>();
       
        for(int i=0; i<dim; ++i){
            if( rmax.expArr[i] != null){
                ExprArrayRange ea = new ExprArrayRange(base, new ExprConstInt(i));
                // lhsvar[i] =  rmax.expArr[i];
                StmtAssign ass = new StmtAssign(ea, rmax.expArr[i]);
                statements.add(ass);
            }else{
                ExprArrayRange ea = new ExprArrayRange(base, new ExprConstInt(i));
                // lhsvar[i] =  0;
                ExprVar iv = new ExprVar(base, "T_"+i);
                ukvarInfo uv = new ukvarInfo();
                uv.iv = iv;
                uv.i = i;
                StmtAssign ass = new StmtAssign(ea, iv);
                uv.ass =ass;
                vInfo vinf = vlist.get(i/2);
                Expression init = ((ExprBinary)vinf.pred).getRight();
                init = new ExprBinary(init, "-", ExprConstInt.one);
                uv.init = new StmtVarDecl(base, TypePrimitive.inttype, "T_"+i, init);
                uv.cond = new ExprBinary(iv, ">=", vinf.start);
                uv.incr = new StmtAssign(iv, new ExprBinary(iv, "-", ExprConstInt.one));
                fl.add(0,uv);
            }
        }
        //Finally, we check that lhsvar satisfies the conditions.
        ExprVar indicator  = new ExprVar((FEContext) null, this.indvar);
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
            StmtAssign ass = new StmtAssign(indicator,cond);
            slist.add(ass);
        }
        cond = null;
        Expression indCond = null;
        for (Expression exp : terC) {
            if (hasImportantVars(exp)) {
                if (cond == null)
                    cond = exp;
                else
                    cond = new ExprBinary(null, ExprBinary.BINOP_AND, cond, exp);
            } else {
                if (indCond == null)
                    indCond = exp;
                else
                    indCond = new ExprBinary(null, ExprBinary.BINOP_AND, indCond, exp);
            }
        }
        if (indCond != null) {
            if( cond == null)
                cond = indCond;
            else
                cond = new ExprBinary(null, ExprBinary.BINOP_AND, cond, indCond);
        }

        if( cond != null){
            StmtAssign ass = new StmtAssign(indicator,cond);
            StmtIfThen sit = new StmtIfThen(indicator, indicator, ass, null);
            slist.add(sit);
        }
        
        if(fl.isEmpty()){
            Statement check = new StmtBlock((FEContext) null, slist);
            statements.add(check);
        }else{
            for(ukvarInfo uv : fl){
                slist.add(0, uv.ass);
            }
            Statement check = new StmtBlock((FEContext) null, slist);
            for(ukvarInfo uv : fl){
                List<Statement> bl = new ArrayList<Statement>();
                bl.add(check);                
                bl.add(uv.incr);
                StmtWhile wh = new StmtWhile(this, new ExprBinary(uv.cond, "&&", new ExprUnary("!", indicator)), new StmtBlock(bl));
                
                List<Statement> b2 = new ArrayList<Statement>();
                b2.add(uv.init);
                b2.add(wh);
                check = new StmtBlock(b2);
            }
            if (indCond != null) {
                check = new StmtIfThen(check, indCond, check, null);
            }
            statements.add(check);
        }
                
        maxAssign = new StmtBlock((FEContext) null, statements);
        return true;
    }

    boolean hasImportantVars(Expression exp) {
        final String lvar = lhsvar;
        class Search extends FEReplacer {
            boolean found = false;

            public Object visitExprVar(ExprVar ev) {
                if (ev.getName().equals(lvar)) {
                    found = true;
                }
                return ev;
            }
        }
        ;
        Search s = new Search();
        exp.accept(s);
        return s.found;
    }
	
	
	
}