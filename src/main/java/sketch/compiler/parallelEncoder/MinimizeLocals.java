package streamit.frontend.parallelEncoder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.Map.Entry;

import streamit.frontend.nodes.ExprVar;
import streamit.frontend.nodes.Expression;
import streamit.frontend.nodes.FEContext;
import streamit.frontend.nodes.FENode;
import streamit.frontend.nodes.FEReplacer;
import streamit.frontend.nodes.Statement;
import streamit.frontend.nodes.StmtAssign;
import streamit.frontend.nodes.StmtBlock;
import streamit.frontend.nodes.StmtFor;
import streamit.frontend.nodes.StmtFork;
import streamit.frontend.nodes.StmtVarDecl;
import streamit.frontend.nodes.Type;

public class MinimizeLocals extends FEReplacer {

	Map<Type, Stack<String> >  freeList= new HashMap<Type, Stack<String>>();
	Map<Type, Stack<String>>  waitlist= new HashMap<Type, Stack<String>>();
	Map<String, String> replMap = new HashMap<String, String>();
	List<Statement> decls = new ArrayList<Statement>();
	boolean outermost = true;
	public void wlAdd(Type t, String s){
		if(waitlist.containsKey(s)){
			waitlist.get(t).push(s);
		}else{
			Stack<String> tmp = new Stack<String>();
			tmp.push(s);
			waitlist.put(t, tmp);
		}
	}



	public Object visitStmtFork(StmtFork loop){
    	StmtVarDecl decl = loop.getLoopVarDecl();
    	Expression niter = (Expression) loop.getIter().accept(this);
    	Statement body = (Statement) loop.getBody().accept(this);
    	if(decl == loop.getLoopVarDecl() && niter == loop.getIter() && body == loop.getBody()  ){
    		return loop;
    	}
    	return new StmtFork(loop, decl, niter, body);
    }



	public Object visitStmtFor(StmtFor stmt)
    {

        Statement newInit = null;
        if(stmt.getInit() != null){
        	newInit = stmt.getInit();
        }
        Expression newCond = doExpression(stmt.getCond());
        Statement newIncr = null;
        if(stmt.getIncr() != null){
        	newIncr = (Statement)stmt.getIncr().accept(this);
        }
        Statement newBody = (Statement)stmt.getBody().accept(this);
        if (newInit == stmt.getInit() && newCond == stmt.getCond() &&
            newIncr == stmt.getIncr() && newBody == stmt.getBody())
            return stmt;
        return new StmtFor(stmt, newInit, newCond, newIncr,
                           newBody);
    }


	public Object visitStmtBlock(StmtBlock b){
		boolean ot = outermost;
		outermost = false;

		Map<Type, Stack<String>>  tmpWL = waitlist;
		waitlist = new HashMap<Type, Stack<String>>();


		Statement o = (Statement)super.visitStmtBlock(b);

		if(ot){
			decls.add(o);
			o = new StmtBlock(b, decls);
			decls = new ArrayList<Statement>();
		}else{
			for(Iterator<Entry<Type, Stack<String>>> it = waitlist.entrySet().iterator(); it.hasNext(); ){
				Entry<Type, Stack<String>> ent = it.next();
				if(freeList.containsKey(ent.getKey())){
					freeList.get(ent.getKey()).addAll(ent.getValue());
				}else{
					freeList.put(ent.getKey(), ent.getValue());
				}
			}
			waitlist = tmpWL;
		}
		outermost = ot;
		return o;
	}



	public Object visitExprVar(ExprVar ev){
		if(replMap.containsKey(ev.getName())){
			return new ExprVar(ev, replMap.get(ev.getName()));
		}else{
			return ev;
		}
	}

	public Object visitStmtVarDecl(StmtVarDecl stmt)
    {
		FENode cx = stmt;
        List<Statement> ls = new ArrayList<Statement>();
        for (int i = 0; i < stmt.getNumVars(); i++)
        {
        	Type t = stmt.getType(i);
        	Stack<String> s = null;
        	if(freeList.containsKey(t)){
        		s = freeList.get(t);
        	}else{
        		s = new Stack<String>();
        		freeList.put(t, s);
        	}
        	if(s.size() > 0){
        		//There is a free variable of the same type.
        		String vn = s.pop();
        		wlAdd(t, vn);
        		replMap.put(stmt.getName(i), vn);
        	}else{
        		wlAdd(t, stmt.getName(i));
        		decls.add(new StmtVarDecl(cx, t, stmt.getName(i), null));
        	}
        	//String name = stmt.getName(i)
        	//waitlist.put(key, value);
            Expression oinit = stmt.getInit(i);
            if (oinit != null){
            	ls.add(new StmtAssign(new ExprVar(cx, stmt.getName(i)), oinit));

            }
        }
        if(ls.size() == 0) return null;
        return new StmtBlock(cx, ls).accept(this);
    }




}
