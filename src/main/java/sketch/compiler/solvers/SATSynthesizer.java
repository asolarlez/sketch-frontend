package sketch.compiler.solvers;

import java.util.*;
import java.util.Map.Entry;

import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Function.FcnType;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.StreamSpec;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.ExprArrayRange;
import sketch.compiler.ast.core.exprs.ExprArrayRange.RangeLen;
import sketch.compiler.ast.core.exprs.ExprBinary;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprNullPtr;
import sketch.compiler.ast.core.exprs.ExprUnary;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssert;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtAtomicBlock;
import sketch.compiler.ast.core.stmts.StmtBlock;
import sketch.compiler.ast.core.stmts.StmtIfThen;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.ast.promela.stmts.StmtFork;
import sketch.compiler.controlflow.CFG;
import sketch.compiler.controlflow.CFGNode;
import sketch.compiler.controlflow.CFGNode.EdgePair;
import sketch.compiler.dataflow.recursionCtrl.RecursionControl;
import sketch.compiler.main.cmdline.SketchOptions;
import sketch.compiler.main.par.ParallelSketchOptions;
import sketch.compiler.parallelEncoder.AtomizeConditionals;
import sketch.compiler.parallelEncoder.BreakParallelFunction;
import sketch.compiler.parallelEncoder.CFGforPloop;
import sketch.compiler.parallelEncoder.ExtractPreParallelSection;
import sketch.compiler.parallelEncoder.VarSetReplacer;
import sketch.compiler.passes.lowering.CollectGlobalTags;
import sketch.compiler.passes.lowering.EliminateMultiDimArrays;
import sketch.compiler.passes.lowering.SimpleLoopUnroller;
import sketch.compiler.passes.printers.SimpleCodePrinter;
import sketch.compiler.solvers.CEtrace.step;
import sketch.compiler.solvers.constructs.AbstractValueOracle;




public class SATSynthesizer implements Synthesizer {

	InteractiveSATBackend solver;
	/**
	 * Original program
	 */
	Program prog;
	/**
	 *
	 * Function containing parallel sections. We assume this is the sketch.
	 */
	Function parfun;

	Program current;
	BreakParallelFunction parts;
	Set<StmtVarDecl> locals = new HashSet<StmtVarDecl>();
	Set<Object> globalTags;
	HashMap<String, Type> varTypes = new HashMap<String, Type>();

	TempVarGen varGen;
	ExprVar assumeFlag = new ExprVar((FENode)null, "_AF");

	/**
	 * Control flow graph
	 */
	CFG cfg;
	/**
	 *
	 * For each node in the control flow graph, it specifies the tags for all the statements contained in that node.
	 */
	Map<CFGNode, Set<Object>> nodeMap;
	/**
	 * Inverse of the nodeMap.
	 */
	Map<Object, CFGNode> invNodeMap;

	List<Statement> bodyl = new ArrayList<Statement>();
	Stack<List<Statement> > bodyStack = new Stack<List<Statement>>();

	Set<StmtVarDecl> globalDecls;

	VarSetReplacer[] localRepl;
	Queue<step>[] stepQueues;

	CFGNode[] lastNode = null;

	Set<String> globals = null;

	StmtFork ploop = null;

	int nthreads;
    public final SketchOptions options;
	
	public void initialize(){
		solver.initializeSolver();
	}

	public SATSynthesizer(Program prog_p, SketchOptions options, RecursionControl rcontrol, TempVarGen varGen) {
		this.options = options;
		this.varGen = varGen;		
        solver = new InteractiveSATBackend(options, rcontrol, varGen);
		
		
		
		this.prog = prog_p;
		this.prog.accept(new SimpleCodePrinter().outputTags());

		this.prog = (Program)prog.accept(new SimpleLoopUnroller());

		ExtractPreParallelSection ps = new ExtractPreParallelSection();
		this.prog = (Program) prog.accept(ps);
		

		assert ps.parfun != null : "this is not a parallel sketch";

		parfun = ps.parfun;
		parts = new BreakParallelFunction();
		parfun.accept(parts);
        //prog.accept(new SimpleCodePrinter().outputTags());

		boolean playDumb = (options instanceof ParallelSketchOptions) &&
		    ((ParallelSketchOptions)options).parOpts.playDumb;
		if(playDumb){
			ploop = (StmtFork) parts.ploop; //.accept(new AtomizeConditionals(varGen));
		}else{
			ploop = (StmtFork) parts.ploop.accept(new AtomizeConditionals(varGen));
		}
		//ploop.accept(new SimpleCodePrinter());
		cfg = CFGforPloop.buildCFG(ploop, locals);
		nthreads = ploop.getIter().getIValue();
		
		//System.out.println(cfg.toDot());

		locals.add( new StmtVarDecl(prog, TypePrimitive.inttype, "_ind", null) );
		nodeMap = CFGforPloop.tagSets(cfg);
		invNodeMap = new HashMap<Object, CFGNode>();
		for(Iterator<Entry<CFGNode, Set<Object>>> it = nodeMap.entrySet().iterator(); it.hasNext(); ){
			Entry<CFGNode, Set<Object>> e = it.next();
			for(Iterator<Object> oit = e.getValue().iterator(); oit.hasNext(); ){
				invNodeMap.put(oit.next(), e.getKey());
			}
		}
		globalDecls = new HashSet<StmtVarDecl>();
		globalDecls.addAll(parts.globalDecls);
		globalDecls.add(new StmtVarDecl(prog, TypePrimitive.bittype, assumeFlag.getName(), ExprConstInt.one));


		bodyl.addAll(globalDecls);


		CollectGlobalTags gtags = new CollectGlobalTags(parts.globalDecls);
		ploop.accept(gtags);
		globalTags = gtags.oset;
		globals = gtags.globals;


		for(StmtVarDecl svd : locals){
			for(int i=0; i<svd.getNumVars(); ++i){
				varTypes.put(svd.getName(i), svd.getType(i));
			}
		}




		localRepl = new VarSetReplacer[nthreads];
		stepQueues = new Queue[nthreads];
		for(int i=0; i<nthreads; ++i){
			localRepl[i] = new VarSetReplacer();
			populateVarReplacer(locals.iterator(), new ExprConstInt(i), localRepl[i]);
			stepQueues[i] = new LinkedList<step>();
		}
		lastNode  = new CFGNode[nthreads];
	}


	/**
	 * Populates the VarSetReplacer with replacement rules of the form:
	 * X -> X_p[idx].
	 *
	 * @param vars
	 * @param idx
	 * @param vrepl
	 */
	public void populateVarReplacer(Iterator<StmtVarDecl> vars, Expression idx, VarSetReplacer/*out*/ vrepl){
		while(vars.hasNext()){
			StmtVarDecl svd = vars.next();
			FENode cx = svd;
			for(int i=0; i<svd.getNumVars(); ++i){
				String oname = svd.getName(i);
				vrepl.addPair(oname, new ExprArrayRange(new ExprVar(cx, oname + "_p"), idx));
			}
		}
	}


	public FENode parametrizeLocals(FENode s, int thread){
		return (FENode) s.accept(localRepl[thread]);
	}


	public void addNode(CFGNode node, int thread){

		Statement s = null;

		if(node.isExpr()){
			s = (Statement) parametrizeLocals(node.getPreStmt(), thread);
		}

		if(node.isStmt()){
			s = (Statement) parametrizeLocals(node.getStmt(), thread);
		}

		if(s != null){
			pushBlock();
			addStatement(s, thread);
			popBlock();
		}
	}


	public Statement addAssume(Expression cond){
		return new StmtIfThen( cond, cond, new StmtAssign(assumeFlag, ExprConstInt.zero), null );
	}



	public Statement addAssume(CFGNode lastNode, int thread, EdgePair ep){
		Expression cond = new ExprBinary( new ExprArrayRange(new ExprVar(lastNode.getExpr(), "_ind_p"), new ExprConstInt(thread)), "==", new ExprConstInt(ep.label.intValue()));
		return addAssume(cond);
	}



	public CFGNode firstChildWithTag(CFGNode parent, int stmt){

		Queue<CFGNode> nqueue = new LinkedList<CFGNode>();
		if(parent != null){
			nqueue.add(parent);
		} else{
			nqueue.add(cfg.getEntry()) ;
		}
		CFGNode node = null;
		while(nqueue.size() > 0){
			CFGNode cur = nqueue.poll();
			Set<Object> so = nodeMap.get(cur);
			if( so.contains(stmt)  ){
				node = cur;
				break;
			}
			for(EdgePair ep : cur.getSuccs()){
				nqueue.add(ep.node);
			}
		}
		assert node != null;
		return node;

	}


	public int findNode(CFGNode start, CFGNode node, int startCount){
		if(startCount > 4){ 
			//System.out.println("Woozing out = " + startCount);  
			return startCount + 20;
		}
		if(start == node){ return startCount; }
		CFGNode tmp = start;
		while(tmp.isStmt()){
			if(tmp == node){
				return startCount;
			}
			tmp = tmp.getSuccs().get(0).node;
		}		
		int minCount = 100000000;
		if(!tmp.isExpr()){ 
			return minCount; 
		}
		for(EdgePair ep : tmp.getSuccs()){
			int t = findNode(ep.node, node, startCount+1);
			if(t < minCount){ minCount = t; }
		}
		return minCount;
	}
	
	int findBestSucc(List<EdgePair> eplist, CFGNode node){
		int i=0;
		int minCount = 100000000;
		int minCountId = -1;
		for(Iterator<EdgePair> it = eplist.iterator(); it.hasNext(); ++i){
			EdgePair ep = it.next();
			CFGNode nxt = ep.node;
			int  t = findNode(nxt, node, 0);
			if(t < minCount){
				minCount = t;
				minCountId = i;
			}			
		}
		return minCountId;
	}
	
	public CFGNode advanceUpTo(CFGNode node, int thread, CFGNode lastNode){

		if(node != cfg.getEntry() && lastNode == null){
			lastNode = cfg.getEntry();
			addNode(lastNode, thread);
		}

		do{
			if(lastNode == null){ break; }
			if(lastNode.isExpr()){
				List<EdgePair> eplist = lastNode.getSuccs();
				List<Statement> assertStmts = new ArrayList<Statement>();
				boolean goodSucc = false;
				for(Iterator<EdgePair> it = eplist.iterator(); it.hasNext(); ){
					EdgePair ep = it.next();
					if(ep.node == node){
						goodSucc = true;
					}else{
						assertStmts.add(addAssume(lastNode, thread, ep));
					}
				}
				if(goodSucc){
					for(Iterator<Statement> it = assertStmts.iterator(); it.hasNext(); ){
						addStatement(it.next(), thread);
					}
					break;
				}else{
					assertStmts.clear();
					int i=0;
					int bestSucc = findBestSucc(eplist, node);
					for(Iterator<EdgePair> it = eplist.iterator(); it.hasNext(); ++i){
						EdgePair ep = it.next();
						CFGNode nxt = ep.node;
						//boolean found = findNode(nxt, node);
						
						/*
						for(EdgePair ep2 : nxt.getSuccs()){
							if(ep2.node == node){
								found = true;
								break;
							}
						}
						*/
						if(bestSucc == i){
							goodSucc = true;
							lastNode = nxt;
						}else{
							assertStmts.add(addAssume(lastNode, thread, ep));
						}
					}
					assert goodSucc : "None of the successors matched bestSucc=="+ bestSucc;
					for(Iterator<Statement> it = assertStmts.iterator(); it.hasNext(); ){
						addStatement(it.next(), thread);
					}
					addNode(lastNode, thread);
				}
			}

			if(lastNode.isStmt()){
				List<EdgePair> eplist = lastNode.getSuccs();
				EdgePair succ = eplist.get(0);

				if(succ.node == node){
					break;
				}
				lastNode = succ.node;

				addNode(lastNode, thread);

			}
			assert lastNode != cfg.getExit() : "This is going to be an infinite loop";
		}while(true);

		return lastNode;
	}


	public CFGNode addBlock(int stmt, int thread, CFGNode lastNode){

		if (reallyVerbose ())
			System.out.println("Step (" + stmt + ", " + thread + ")"); 
		
		assert invNodeMap.containsKey(stmt);

		CFGNode node =  firstChildWithTag(lastNode, stmt);  //invNodeMap.get( stmt );
		if(node.getStmt() != null){
			// System.out.println("Next " + thread + " -- " + node.getStmt().getOrigin().getCx() + " --> " + node.getStmt().getOrigin() + "  ==  " + node.getStmt());
		}
		
		assert node != null;

		if( node == lastNode  ){
			//Haven't advanced nodes, so I should stay here.
			return lastNode;
		}


		advanceUpTo(node, thread, lastNode);


		addNode(node, thread);

		return node;
	}


	public void pushBlock(){

		bodyStack.push(bodyl);
		bodyl = new ArrayList<Statement>();
	}
	public void popBlock(){
		List<Statement> tmpl = bodyStack.pop();
		tmpl.add( new StmtBlock((FENode)null, bodyl));
		bodyl = tmpl;
	}


	private Statement nodeReadyToExecute(CFGNode n, int thread, final Expression indVar){

		Statement s = null;
		if(n.isExpr()){ s = n.getPreStmt(); }
		if(n.isStmt()){ s = n.getStmt(); }

		class modifiedLocals extends FEReplacer{
			boolean isLeft = false;
			public HashSet<String> locals = new HashSet<String>();
			private boolean globalTaint = false;
			private HashSet<String> modLocals = new HashSet<String>();
			public Object visitStmtAssign(StmtAssign stmt){
				boolean tmpLeft = isLeft;
			 	isLeft = true;
			 	boolean gt = globalTaint;
			 	globalTaint = false;
		        Expression newLHS = doExpression(stmt.getLHS());
		        isLeft = tmpLeft;
		        boolean newgt = globalTaint;
		        globalTaint = gt;
		        Expression newRHS = doExpression(stmt.getRHS());
		        if(newgt){
		        	return null;
		        }
		        return stmt;
			}

			public Object visitStmtAssert(StmtAssert stmt){
				return new StmtIfThen(stmt, assumeFlag, stmt, null);
			}

			public Object visitExprArrayRange(ExprArrayRange exp) {
				boolean tmpLeft = isLeft;

				// This is weird, but arrays can't be parameters to functions in
				// Promela.  So we'll be conservative and always treat them as
				// LHS expressions.
				isLeft = true;
				doExpression(exp.getBase());
				isLeft = tmpLeft;

				
				{					
					{
						RangeLen range=exp.getSelection();
						tmpLeft = isLeft;
					 	isLeft = false;
						doExpression(range.start());
						isLeft = tmpLeft;
					}
				}
				return exp;
			}




			public Object visitStmtAtomicBlock(StmtAtomicBlock stmt){
				if(stmt.isCond()){
					return new StmtAssign(indVar, new ExprBinary(indVar , "&&",  stmt.getCond()));
				}
				return super.visitStmtAtomicBlock(stmt);
			}



			public Object visitStmtVarDecl(StmtVarDecl stmt)
		    {
		        for (int i = 0; i < stmt.getNumVars(); i++)
		        {
		            Expression init = stmt.getInit(i);
		            if (init != null)
		                init = doExpression(init);
		            Type t = (Type) stmt.getType(i).accept(this);
		            locals.add(stmt.getName(i));
		        }
		        return stmt;
		    }

			public Object visitExprVar(ExprVar exp) {
				if(isLeft){
					String nm = exp.getName();
					if(globals.contains(nm)){
						globalTaint = true;
					}else{
						if(!locals.contains(nm)){
							modLocals.add(nm);
						}
					}
				}
				return exp;
			}

		}


		class addTemporaries extends FEReplacer{
			private final HashSet<String> modLocals;
			public addTemporaries(HashSet<String> modLocals){
				this.modLocals = modLocals;
			}
			public Object visitExprVar(ExprVar exp) {
				String nm = exp.getName();
				if(modLocals.contains( nm )){
					return new ExprVar(exp, nm + "__t");
				}
				return exp;
			}
		}

		modifiedLocals ml = new modifiedLocals();
		s = s.doStatement(ml);
		if(s == null){
			return null;			
		}
		s = s.doStatement(new addTemporaries(ml.modLocals));

		List<Statement> ls = new ArrayList<Statement>();
		for(String vn : ml.modLocals){
			ls.add(new StmtVarDecl(s, varTypes.get(vn), vn + "__t", new ExprVar(s, vn)));
		}
		ls.add(s);
		s = (Statement) parametrizeLocals(new StmtBlock(s, ls), thread);

		return s;
	}






	private Expression getAtomicCond(CFGNode n, int thread){

		Statement s = null;
		if(n.isExpr()){ s = n.getPreStmt(); }
		if(n.isStmt()){ s = n.getStmt(); }

		nodeReadyToExecute(n, thread, new ExprVar(s, "IV"));

		final List<Expression> answer = new ArrayList<Expression>();

		class hasAtomic extends FEReplacer{
			boolean assignOnPath = false;
			Stack<Expression> estack = new Stack<Expression>();
			@Override
			public Object visitStmtAssign(StmtAssign stmt){
				assignOnPath = true;
				return stmt;
			}

			@Override
			public Object visitStmtIfThen(StmtIfThen stmt){
				estack.add(new ExprUnary("!", stmt.getCond()));
				boolean tmp = assignOnPath;
				stmt.getCons().accept(this);
				estack.pop();
				boolean tmp2 = assignOnPath;
				assignOnPath = tmp;
				if(stmt.getAlt() != null){
					estack.add(stmt.getCond());
					stmt.getAlt().accept(this);
					estack.pop();
				}
				assignOnPath = assignOnPath || tmp2;
				return stmt;
			}

			@Override
			public Object visitStmtAtomicBlock(StmtAtomicBlock stmt){
				if(stmt.isCond()){
					assert !assignOnPath : "assignments before atomics NYI";
					Expression c = stmt.getCond() ;
					for(Expression e : estack){
						c = new ExprBinary(c, "||", e);
					}
					if(answer.size() > 0){
						answer.set(0, new ExprBinary(c, "&&", answer.get(0)  ) );
					}else{
						answer.add(c);
					}
				}
				assignOnPath = true;
				return stmt;
			}
		}

		s.accept(new hasAtomic());

		if(answer.size() > 0){
			return (Expression)parametrizeLocals(answer.get(0), thread);
		}

		return ExprConstInt.one;
	}

/**
 *
 * Adds to the list ls the code to check whether the successor of
 * node n is ready to execute or not.
 *
 * @param n
 * @param thread
 * @param iv Indicator variable that says whether this thread is
 *   ready to execute or not.
 * @param ls
 */
	private void getNextAtomicCond(CFGNode n, int thread, ExprVar iv, List<Statement> ls){

		if(n.isStmt()){
			CFGNode nxt = n.getSuccs().get(0).node;

			if(nxt == cfg.getExit()){
				ls.add(new StmtAssign(iv, ExprConstInt.zero));
				return;
			}else{
				ls.add(nodeReadyToExecute(nxt, thread, iv));
				return ;
			}
		}

		if(n.isExpr()){
			for( EdgePair ep : n.getSuccs() ){
				Statement s;
				if(ep.node == cfg.getExit()){
					s = new StmtAssign(iv, ExprConstInt.zero);
				}else{
					s = nodeReadyToExecute(ep.node, thread, iv);
				}
				Expression g = new ExprBinary((Expression)parametrizeLocals(n.getExpr(), thread), "==", new ExprConstInt(ep.label));

				ls.add(new StmtIfThen(s, g, s, null));

			}
			return;
		}
		return;

	}




	private void finalAdjustment(CFGNode node, int thread){

		if(node.isStmt()){
			addNode(node.getSuccs().get(0).node, thread);
			lastNode[thread] = node.getSuccs().get(0).node;
		}

		if(node.isExpr()){
			/*
			assert false : "NYI Don't know how to modify lastNode.";
			for(EdgePair ep : node.getSuccs()){

				Expression cond = new ExprBinary((Expression)parametrizeLocals(node.getExpr(), thread), "==", new ExprConstInt(ep.label));

				Statement s = null;
				if(ep.node.isStmt()){
					s = ep.node.getStmt();
				}
				if(ep.node.isExpr()){
					s = ep.node.getPreStmt();
				}
				s = (Statement) parametrizeLocals(s, thread);
				s = preprocStatement(s, thread);
				bodyl.add(new StmtIfThen(cond, cond, s, null));

			}
			*/


		}


	}



	private Statement preprocStatement(Statement s, final int thread){

		class PreprocStmt extends FEReplacer{
			@Override
			public Object visitStmtAssert(StmtAssert stmt){
				return new StmtIfThen(stmt, assumeFlag, stmt, null);
			}
			Stack<StmtAtomicBlock> atomicCheck = new Stack<StmtAtomicBlock>();
			public Object visitStmtAtomicBlock(StmtAtomicBlock stmt){

				if(stmt.isCond()){
					assert atomicCheck.size() == 0 : "Conditional atomic can not be inside another atomic." + stmt.getCx();
					Statement s2 = stmt.getBlock().doStatement(this);



					Statement elsecond;

					if(thread >=0){

						List<Statement> mls = new ArrayList<Statement>();
						mls.add(new StmtVarDecl(stmt, TypePrimitive.bittype, "mIV", ExprConstInt.zero));
						ExprVar miv = new ExprVar(stmt, "mIV");
						for(int i=0; i<nthreads; ++i){
							if(i == thread){ continue; }
							List<Statement> ls = new ArrayList<Statement>();
							ls.add(new StmtVarDecl(stmt, TypePrimitive.bittype, "IV", ExprConstInt.one));
							ExprVar iv = new ExprVar(stmt, "IV");
							CFGNode n = lastNode[i];
							if(n != null){
								getNextAtomicCond(n, i, iv, ls);
							}else{
								ls.add(nodeReadyToExecute(cfg.getEntry(), i, iv));
							}
							ls.add(new StmtAssign(miv, new ExprBinary(miv, "||", iv)));
							mls.add(new StmtBlock(stmt, ls));
						}

						Statement allgood = new StmtAssign(assumeFlag, ExprConstInt.zero);
						Statement allbad = new StmtIfThen(stmt, assumeFlag,
								new StmtAssert(stmt, ExprConstInt.zero, "There was a deadlock.", false), null);

						Statement otherwise = new StmtIfThen(stmt, miv, allgood, allbad);
						mls.add(otherwise);
						elsecond = new StmtBlock(stmt, mls);
					}else{
						elsecond = new StmtAssert(stmt, ExprConstInt.zero, "There was a deadlock.", false);
					}
					Statement s = new StmtIfThen(stmt, stmt.getCond(), s2, elsecond);
					return s;
				}else{
					atomicCheck.add(stmt);
					Object o = super.visitStmtAtomicBlock(stmt);
					atomicCheck.pop();
					return o;
				}
			}

		};
		return s.doStatement(new PreprocStmt());
	}


	public void addStatement(Statement s, final int thread){
		Statement tmp =preprocStatement(s, thread);
		if(this.reallyVerbose())
			tmp.accept(new SimpleCodePrinter().outputTags());
		bodyl.add(tmp);
	}

	@SuppressWarnings("unchecked")
	public void closeCurrent(){

		
		List<Parameter> outPar = new ArrayList<Parameter>();
		String opname = null;
		List<Statement> lst = new ArrayList<Statement>();

		for(Iterator<Parameter> it = parfun.getParams().iterator(); it.hasNext(); ){
			Parameter p = it.next();
			if(p.isParameterOutput()){
				outPar.add(p);
				opname = p.getName();
			}else{
				lst.add(new StmtVarDecl(p, p.getType(), p.getName(), ExprConstInt.zero ));
			}
		}


		lst.addAll(bodyl);
		
		bodyl.clear();
		bodyl.addAll(globalDecls);
		
		lst.add( new StmtAssign(new ExprVar(current, opname), ExprConstInt.one) );

		Statement body = new StmtBlock(current, lst);

        final StmtBlock specBody =
                new StmtBlock(current, new StmtAssign(new ExprVar(current, opname),
                        ExprConstInt.one));
        Function spec =
                Function.creator(current, "spec", FcnType.Static).returnType(
                        TypePrimitive.inttype).params(outPar).body(specBody).create();
        // Function.newHelper(current, "spec", TypePrimitive.inttype ,outPar, new StmtAssign(new ExprVar(current, opname), ExprConstInt.one));

		Function sketch = Function.creator(current, "sketch", FcnType.Static).params(outPar).spec("spec").body(body).create();

		List<Function> funcs = new ArrayList<Function>();

		funcs.add(spec);
		funcs.add(sketch);

		List<StreamSpec> streams = Collections.singletonList(
new StreamSpec(current, "MAIN",
                        Collections.EMPTY_LIST, Collections.EMPTY_LIST, funcs));
        current =
 current.creator().streams(streams).create();


	}

	/**
	 * The input collection contains variable declarations, and the output collection contains variable' declarations such that
	 * if T X; is a declaration in the original list, T[NTHREADS] X_p will be a declaration in the output list.
	 *
	 * @param original input list
	 * @param nthreads number of threads.
	 */
	public void declArrFromScalars(Iterator<StmtVarDecl> original,   Expression nthreads){
		while(original.hasNext()){
			StmtVarDecl svd = original.next();
			FENode cx = svd;
			for(int i=0; i<svd.getNumVars(); ++i){
				String oname = svd.getName(i);
				Type ot = svd.getType(i);
				//assert svd.getInit(i) == null : "At this stage, declarations shouldn't have initializers";
				String nname = oname + "_p";
				Type nt = new TypeArray(ot, nthreads);
				Expression init ;

				Type base = nt;
				while(base instanceof TypeArray){
					base = ((TypeArray)base).getBase();
				}
				if(base instanceof TypePrimitive){
					init = ExprConstInt.zero;
				}else{
					init = ExprNullPtr.nullPtr;
				}
				addStatement(new StmtVarDecl(cx, nt, nname, init), -1);
			}
		}
	}

	
	static class dlistNode<E>{
		final E n;
		final int thread;
		dlistNode<E> lnext;
		dlistNode<E> gnext;
		dlistNode(E n, int thread){
			this.n = n;
			this.thread = thread;
			lnext = null;
			gnext = null;
		}
		public String toString(){
			return thread + ":" + n; 
		}
	}
	
	static class dlist<E>{
		dlistNode<E> head;
		dlistNode<E> tail;
		dlistNode<E>[] lheads; 
		dlistNode<E>[] ltails;
		dlist(int nt){
			lheads = new dlistNode[nt];
			ltails = new dlistNode[nt];
		}
		dlistNode<E> addNode(E n, int thread){
			int th = thread;
			dlistNode<E> t = new dlistNode<E>(n, thread);
			if(head == null){  head = t; tail = t; }
			else{ tail.gnext = t; tail = t; }
			if(lheads[th] == null){  lheads[th] = t; ltails[th] = t; }
			else{ 
				assert ltails[th].n != n : "Can't have two successive guys with the same node.";
				ltails[th].lnext = t; ltails[th] = t; }
			return t;
		}
		
		dlistNode<E> addNode(dlistNode<E> gpred, dlistNode<E> lpred,  E succ, int thread){
			int th = thread;
			dlistNode<E> rv = new dlistNode<E>(succ, thread);
			if(gpred == null){
				rv.gnext = head;
				head = rv;
				if(tail==null){
					tail = rv;
				}
			}else{
				rv.gnext = gpred.gnext;
				gpred.gnext = rv;
				if(gpred == tail){
					tail = rv;
				}
			}
			if(lpred == null){
				rv.lnext = lheads[th];
				lheads[th] = rv;
				if(ltails[th] == null){
					ltails[th] = rv;
				}
			}else{
				rv.lnext = lpred.lnext;
				lpred.lnext = rv;
				if(lpred == tail){
					ltails[th] = rv;
				}
			}
			return rv;
		}
	}
	
	
	static class parentOrder{
		dlist<FENode> sdlist;
		dlist<CFGNode> tlists;
		Map<Integer, dlistNode<FENode>>[] seenStmts;
		int nthreads;
		parentOrder(int nt){
			tlists = new dlist<CFGNode>(nt);
			sdlist = new dlist<FENode>(nt);
			this.nthreads = nt;
			seenStmts = new Map[nt];
			for(int i=0; i<nt; ++i){
				seenStmts[i] = new HashMap<Integer, dlistNode<FENode>>(); 
			}
		}
		
		void addNode(CFGNode n, int thread){
			tlists.addNode(n, thread);
			assert n.isStmt();
			FENode s = n.getStmt().getOrigin();
			int key = s.hashCode();
			if(!seenStmts[thread].containsKey(key)){
				seenStmts[thread].put(key, sdlist.addNode(s, thread));
			}
		}
		
		/**
		 * Return an immediate successor of n. If n has only one successor, return that.
		 * otherwise, return the successor that will get you to farnext faster. 
		 * @param n
		 * @param farnext
		 * @return
		 */
		CFGNode nextNode(CFGNode n, CFGNode farnext){
			List<EdgePair> succs = n.getSuccs();
			if(succs.size()==1){
				return succs.get(0).node;
			}else{
				assert false :"NYI";
				return null;
			}			
		}
		
		
		
		boolean isSucc(CFGNode pred, int predTh, CFGNode succ, int succTh){
			FENode pori = pred.getStmt().getOrigin();
			FENode sori = succ.getStmt().getOrigin();
			if(!seenStmts[predTh].containsKey(pori.hashCode())){
				return false;
			}
			if(!seenStmts[succTh].containsKey(sori.hashCode())){
				return false;
			}
			
			dlistNode<?> pnode = seenStmts[predTh].get(pori.hashCode());
			dlistNode<?> snode = seenStmts[succTh].get(sori.hashCode());
			
			return pnode.gnext == snode;			
		}
		
		
		dlistNode<CFGNode> insertSuccessor(dlistNode<CFGNode> gpred, dlistNode<CFGNode> lpred,  CFGNode succ, int thread){
			return tlists.addNode(gpred, lpred, succ, thread);
		}
		
		
		/**
		 * Currents contains the last CFG node that was added to the schedule for each thread.
		 * tstate contains the last dlistNode in the graph that was added to the schedule for each thread.
		 * current contaisn the last CFG node that was added to the schedule.
		 * @param currents
		 * @param tstate
		 * @param current
		 * @return
		 */
		dlistNode<CFGNode> selectNext(CFGNode[] currents, dlistNode<CFGNode>[] tstate,  dlistNode<CFGNode> current){
			assert currents[current.thread] == current.n;
			assert current == tstate[current.thread];
			if(current.gnext == null){ return null; }
			/*
			 * If current.next == current.lnext, I should only switch from thread i to thread j if:
			 * currents[j].next is unscheduled (it's not equal to tstate[j]) and
			 * a node between currents[j].next and tstate[j] wants to be a predecessor of current.next.
			 * a node between currents[j].next and tstate[j] wants to be a successor of current.  
			 */			
			if(current.gnext == current.lnext){
				for(int j=0; j<nthreads; ++j){
					if(j != current.thread && tstate[j].lnext != null){
						CFGNode next = nextNode(currents[j], tstate[j].lnext.n);
						while(next != tstate[j].lnext.n){
							if(isSucc(current.n, current.thread, next, j)){
								System.out.println("SUCCESS CASE 0");
								dlistNode<CFGNode> tt = insertSuccessor(current, tstate[j], next, j);
								currents[j] = tt.n;
								tstate[j] = tt;
								return tt;
							}
							CFGNode onext = nextNode(current.n, current.lnext.n);
							while(onext != current.lnext.n){
								if(isSucc(next, j, onext, current.thread)){
									System.out.println("SUCCESS CASE 1");
									dlistNode<CFGNode> tt = insertSuccessor(current, tstate[j], next, j);
									dlistNode<CFGNode> tu = insertSuccessor(tt, current, onext, current.thread);
									currents[j] = tt.n;
									tstate[j] = tt;
									return tt;
								}
								onext = nextNode(onext, current.lnext.n);
							}
							next = nextNode(next, tstate[j].lnext.n);
						}
					}
				}
			}
			
			/* 
			 * If current.next != current.lnext, I should only stay in thread i if:
			 * current.next is in thread j.
			 * a node between current and current.lnext wants to be a predecessor of a node between 
			 * currents[j] and current.next; 
			 * 
			 */
			if(current.gnext != current.lnext && (current.lnext!= null) ){
				int j = current.gnext.thread;
				dlistNode<CFGNode> lnext = current.lnext!= null ? current.lnext : tlists.ltails[current.thread]; 
				CFGNode next = nextNode(current.n, lnext.n);
				while(next != lnext.n){
					CFGNode onext = nextNode(currents[j], current.gnext.n);
					while(onext != current.gnext.n){
						if(isSucc(next, current.thread, onext , j)
						){
							System.out.println("SUCCESS CASE 2");
							dlistNode<CFGNode> tt = insertSuccessor(current, current, next, current.thread); 
							dlistNode<CFGNode> tu = insertSuccessor(tt, tstate[j], onext, j);
							currents[current.thread] = tt.n;
							tstate[current.thread] = tt;
							return tt;
						}
						onext = nextNode(onext, current.gnext.n);						
					}
					if(isSucc(next, current.thread, onext , j)
					){						
						System.out.println("SUCCESS CASE 3");
						dlistNode<CFGNode> tt = insertSuccessor(current, current, next, current.thread); 
						// dlistNode<CFGNode> tu = insertSuccessor(tt, tstate[j], onext, j); No need to insert this because onext already is part of the dag, since it's equal tu current.gnext.n
						currents[current.thread] = tt.n;
						tstate[current.thread] = tt;
						return tt;
					}
					next = nextNode(next, lnext.n);
				}
				
			}

			currents[current.gnext.thread] = current.gnext.n;
			tstate[current.gnext.thread] = current.gnext;
			return current.gnext;
		}
		
		
		List<dlistNode<CFGNode>> computeCFGOrder(CFG cfg){
			dlistNode<CFGNode>[] tstate = new dlistNode[nthreads]; 
			List<dlistNode<CFGNode>> fullSchedule = new ArrayList<dlistNode<CFGNode>>();
			CFGNode[] currents = new CFGNode[nthreads];
			
			dlistNode<CFGNode> out = tlists.head;						
			for(int i=0; i<nthreads; ++i){ 
				currents[i] = cfg.getEntry(); 
				tstate[i] = tlists.lheads[i];
				assert out.n == currents[i];
				if(i != nthreads-1){ out = out.gnext; }
				fullSchedule.add(new dlistNode<CFGNode>(cfg.getEntry(), i));
			}
						
			while(true){
				out = selectNext(currents, tstate, out);
				if(out == null) break;
				if(out.n.getStmt().getTag() != null){
					fullSchedule.add(out);
				}
				currents[out.thread] = out.n;				
			}
			return fullSchedule;
		}
		
		
	}
	
	public void processTrace(CEtrace trace, CFG cfg){
		
		
		
		
		
		parentOrder po = new parentOrder(nthreads);
		
		List<step> l = trace.steps;

		Iterator<step> sit = l.iterator();
		step cur;
		for(int i=0; i<nthreads; ++i){ 
			lastNode[i] = null; 
			po.addNode(cfg.getEntry(), i);
		}
		while(sit.hasNext()){
			cur= sit.next();
			if(cur.thread>0){				
				if( invNodeMap.containsKey(cur.stmt) ){
					int thread = cur.thread-1;					
					if( globalTags.contains(cur.stmt)  ){
						stepQueues[thread].add(cur);
						Queue<step> qs = stepQueues[thread];
						while( qs.size() > 0  ){
							step tmp = qs.remove();
							CFGNode lnode = firstChildWithTag(lastNode[thread], tmp.stmt); 
							if(lnode != cfg.getEntry()){
								// System.out.println(tmp + "--" + lnode);
								po.addNode(lnode, thread);
							}
							lastNode[thread] = lnode;
						}
					}
				}else{
					// System.out.println("NC " + cur);
				}
			}
		}
		
		l.clear();
		List<dlistNode<CFGNode>>  neworder = po.computeCFGOrder(cfg);
		for(dlistNode<CFGNode> nn: neworder){
			Set<Object> s = this.nodeMap.get(nn.n);
			Integer sid = (Integer) s.iterator().next();
			l.add(new step(nn.thread+1, sid));
		}		
	}
	
	
	
	public void insertPrePar(){
		if(current != null){
			for(Iterator<StmtVarDecl> it = globalDecls.iterator(); it.hasNext(); ){
				StmtVarDecl svd = it.next();
				for(int i=0; i<svd.getNumVars(); ++i){
					if(svd.getInit(i)!= null){
						addStatement(new StmtAssign(new ExprVar(svd, svd.getName(i)), svd.getInit(i)), -1);
					}
				}
			}
		}

		addStatement(parts.prepar, -1);

		declArrFromScalars(locals.iterator(), new ExprConstInt(nthreads));

		for(int i=0; i<nthreads; ++i){
			Expression idx = new ExprConstInt(i) ;
			Expression ilhs = new ExprArrayRange( new ExprVar(idx, ploop.getLoopVarName()  +"_p")  , idx  );
			addStatement(new StmtAssign(ilhs, idx ) , i);
		}		
	}
	
	

	public void mergeWithCurrent(CEtrace trace){
		/* if (reallyVerbose ())
			prog.accept(new SimpleCodePrinter().outputTags()); */
		pushBlock();

		insertPrePar();

		// processTrace(trace, cfg);
		

		List<step> l = trace.steps;

		/**
		 * It's tempting to believe that at this point, we should just append
		 * trace.blickedSteps to l, and that's that, but that will mess up with
		 * deadlock detection. The reason is that before adding the blocked steps,
		 * we should empty the stepQueues.
		 *
		 */

		Iterator<step> sit = l.iterator();
		step cur;


		for(int i=0; i<nthreads; ++i){ lastNode[i] = null; }

		StringBuffer sbuf = new StringBuffer();

		while(sit.hasNext()){
			cur= sit.next();
			if(cur.thread>0){
				sbuf.append(""+ cur);
				// System.out.println("T " + cur);
				if( invNodeMap.containsKey(cur.stmt) ){
					int thread = cur.thread-1;
					stepQueues[thread].add(cur);
					// System.out.println("C " + cur);
					if( globalTags.contains(cur.stmt)  ){
						Queue<step> qs = stepQueues[thread];
						while( qs.size() > 0  ){
							step tmp = qs.remove();
						//	System.out.println("t " + tmp);
							lastNode[thread] = addBlock(tmp.stmt, thread, lastNode[thread]);
						}
					}
				}else{
					// System.out.println("NC " + cur);
				}


			}
		}

		

		for(int thread=0; thread<nthreads; ++thread){
			Queue<step> qs = stepQueues[thread];
			while( qs.size() > 0  ){
				step tmp = qs.remove();
				lastNode[thread] = addBlock(tmp.stmt, thread, lastNode[thread]);
			}
		}

		for(step s : trace.blockedSteps){
			if (s.thread == 0)
				continue;

			int thread = s.thread-1;
			int stmt = s.stmt;
			sbuf.append(""+ s);
			assert invNodeMap.containsKey(stmt) : "'"+ stmt +"' not in invNodeMap";

			CFGNode node =  firstChildWithTag(lastNode[thread], stmt);  //invNodeMap.get( stmt );

			assert node != null;

			if( node != lastNode[thread]  ){
				lastNode[thread] = advanceUpTo(node, thread, lastNode[thread]);
			}
		}
		
		solver.log(sbuf.toString());
		if(schedules.containsKey(sbuf.toString())){
			throw new RuntimeException("I just saw a repeated schedule.");
		}
		schedules.put(sbuf.toString(), schedules.size());

		boolean allEnd = true;

		for(int thread=0; thread<nthreads; ++thread){
			if(lastNode[thread]!= null && lastNode[thread] != cfg.getExit()){

				boolean tmp = procLastNodes(lastNode[thread], thread);
				allEnd = allEnd && tmp;
			}
			if(lastNode[thread] == null){
				allEnd = false;
			}
		}

		if(allEnd){
			addStatement(parts.postpar, -1);
		}
		popBlock();
		closeCurrent();
	}

	Map<String, Integer> schedules = new HashMap<String, Integer>();




	boolean procLastNodes(CFGNode node, int thread){
		boolean someEnd = false;
		List<Statement> assertStmts = new ArrayList<Statement>();
		for(EdgePair ep : node.getSuccs()){
			if(ep.node == cfg.getExit() && ep.node.isEmpty()){
				someEnd = true;
			}else{
				if(node.isExpr()){
					assertStmts.add(addAssume(node, thread, ep));
				}

			}
		}
		if(!someEnd){
			finalAdjustment(node, thread);
		}else{
			for(Iterator<Statement> it = assertStmts.iterator(); it.hasNext(); ){
				addStatement(it.next(), thread);
			}
		}
		return someEnd;

	}






	public AbstractValueOracle nextCandidate(CounterExample counterExample) {

		mergeWithCurrent((CEtrace)counterExample);

		current = (Program)current.accept(new EliminateMultiDimArrays(varGen));
/*		if (reallyVerbose ())
			current.accept(new SimpleCodePrinter().outputTags()); */ 
		boolean tmp = solver.partialEvalAndSolve(current);

		return tmp ? solver.getOracle() : null;
	}



	protected boolean reallyVerbose () {
		return options.debugOpts.verbosity >= 5;
	}

	public void cleanup(){
		solver.cleanup();
	}

	public SolutionStatistics getLastSolutionStats() {		
		return solver.getLastSolutionStats();
	}
	public void activateTracing(){
		solver.activateTracing();
	}
}
