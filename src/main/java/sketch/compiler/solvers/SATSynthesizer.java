package streamit.frontend.solvers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;
import java.util.Map.Entry;

import streamit.frontend.CommandLineParamManager;
import streamit.frontend.controlflow.CFG;
import streamit.frontend.controlflow.CFGNode;
import streamit.frontend.controlflow.CFGNode.EdgePair;
import streamit.frontend.nodes.ExprArrayRange;
import streamit.frontend.nodes.ExprBinary;
import streamit.frontend.nodes.ExprConstInt;
import streamit.frontend.nodes.ExprFunCall;
import streamit.frontend.nodes.ExprNullPtr;
import streamit.frontend.nodes.ExprUnary;
import streamit.frontend.nodes.ExprVar;
import streamit.frontend.nodes.Expression;
import streamit.frontend.nodes.FEContext;
import streamit.frontend.nodes.FENode;
import streamit.frontend.nodes.FEReplacer;
import streamit.frontend.nodes.Function;
import streamit.frontend.nodes.Parameter;
import streamit.frontend.nodes.Program;
import streamit.frontend.nodes.Statement;
import streamit.frontend.nodes.StmtAssert;
import streamit.frontend.nodes.StmtAssign;
import streamit.frontend.nodes.StmtAtomicBlock;
import streamit.frontend.nodes.StmtBlock;
import streamit.frontend.nodes.StmtExpr;
import streamit.frontend.nodes.StmtFork;
import streamit.frontend.nodes.StmtIfThen;
import streamit.frontend.nodes.StmtReturn;
import streamit.frontend.nodes.StmtVarDecl;
import streamit.frontend.nodes.StreamSpec;
import streamit.frontend.nodes.StreamType;
import streamit.frontend.nodes.TempVarGen;
import streamit.frontend.nodes.Type;
import streamit.frontend.nodes.TypeArray;
import streamit.frontend.nodes.TypePrimitive;
import streamit.frontend.parallelEncoder.AtomizeConditionals;
import streamit.frontend.parallelEncoder.BreakParallelFunction;
import streamit.frontend.parallelEncoder.CFGforPloop;
import streamit.frontend.parallelEncoder.ExtractPreParallelSection;
import streamit.frontend.parallelEncoder.VarSetReplacer;
import streamit.frontend.passes.CollectGlobalTags;
import streamit.frontend.passes.EliminateMultiDimArrays;
import streamit.frontend.solvers.CEtrace.step;
import streamit.frontend.stencilSK.SimpleCodePrinter;
import streamit.frontend.tosbit.ValueOracle;
import streamit.frontend.tosbit.recursionCtrl.RecursionControl;




public class SATSynthesizer extends SATBackend implements Synthesizer {

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

	StmtFork ploop = null;

	int nthreads;

	public SATSynthesizer(Program prog, CommandLineParamManager params, RecursionControl rcontrol, TempVarGen varGen){
		super(params, rcontrol, varGen);
		this.prog = prog;
		ExtractPreParallelSection ps = new ExtractPreParallelSection();
		this.prog = (Program) prog.accept(ps);
		//this.prog.accept(new SimpleCodePrinter().outputTags());
		parfun = ps.parfun;
		parts = new BreakParallelFunction();
		parfun.accept(parts);
		//this.prog.accept(new SimpleCodePrinter().outputTags());

		ploop = (StmtFork) parts.ploop.accept(new AtomizeConditionals(varGen));
		//ploop.accept(new SimpleCodePrinter());
		cfg = CFGforPloop.buildCFG(ploop, locals);
		nthreads = ploop.getIter().getIValue();

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

	public CFGNode addBlock(int stmt, int thread, CFGNode lastNode){


		assert invNodeMap.containsKey(stmt);

		CFGNode node = invNodeMap.get( stmt );

		assert node != null;

		if( node == lastNode  ){
			//Haven't advanced nodes, so I should stay here.
			return lastNode;
		}


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
				assert goodSucc : "None of the successors matched";
				for(Iterator<Statement> it = assertStmts.iterator(); it.hasNext(); ){
					addStatement(it.next(), thread);
				}

				break;
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
		}while(true);

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



	private Expression getAtomicCond(CFGNode n, int thread){

		Statement s = null;
		if(n.isExpr()){ s = n.getPreStmt(); }
		if(n.isStmt()){ s = n.getStmt(); }

		final List<Expression> answer = new ArrayList<Expression>();

		class hasAtomic extends FEReplacer{

			@Override
			public Object visitStmtAtomicBlock(StmtAtomicBlock stmt){
				if(stmt.isCond()){
					answer.add(stmt.getCond());
				}
				return stmt;
			}
		}

		s.accept(new hasAtomic());

		if(answer.size() > 0){
			return (Expression)parametrizeLocals(answer.get(0), thread);
		}

		return ExprConstInt.one;
	}


	private Expression getNextAtomicCond(CFGNode n, int thread){

		if(n.isStmt()){
			CFGNode nxt = n.getSuccs().get(0).node;

			if(nxt == cfg.getExit()){
				return ExprConstInt.zero;
			}else{
				return getAtomicCond(nxt, thread);
			}
		}

		if(n.isExpr()){
			Expression rv = null;
			for( EdgePair ep : n.getSuccs() ){
				Expression node = null;
				if(ep.node == cfg.getExit()){
					node = ExprConstInt.zero;
				}else{
					node = getAtomicCond(ep.node, thread);
				}
				Expression g = new ExprBinary((Expression)parametrizeLocals(n.getExpr(), thread), "==", new ExprConstInt(ep.label));
				g = new ExprBinary(g , "&&", node);
				if(rv == null){
					rv = g;
				}else{
					rv = new ExprBinary(rv , "||", g);
				}
			}
			return rv;
		}
		return null;

	}




	private void finalAdjustment(CFGNode node, int thread){

		if(node.isStmt()){
			addNode(node.getSuccs().get(0).node, thread);
		}

		if(node.isExpr()){

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




					Expression gcond = null;

					for(int i=0; i<nthreads; ++i){
						if(i == thread){ continue; }
						CFGNode n = lastNode[i];
						Expression cond;
						if(n != null){
							cond = getNextAtomicCond(n, i);
						}else{
							cond = getAtomicCond(cfg.getEntry(), i);
						}

						if(gcond == null){ gcond = cond; }else
						{ gcond = new ExprBinary(gcond, "||", cond  );  }
					}

					Statement allgood = new StmtAssign(assumeFlag, ExprConstInt.zero);
					Statement allbad = new StmtIfThen(stmt, assumeFlag,
							new StmtAssert(stmt, ExprConstInt.zero, "There was a deadlock."), null);

					Statement otherwise = new StmtIfThen(stmt, gcond, allgood, allbad);

					Statement s = new StmtIfThen(stmt, stmt.getCond(), s2, otherwise);
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
		bodyl.add(preprocStatement(s, thread));
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
		lst.add( new StmtAssign(new ExprVar(current, opname), ExprConstInt.one) );

		Statement body = new StmtBlock(current, lst);

		Function spec = Function.newHelper(current, "spec", TypePrimitive.inttype ,outPar, new StmtAssign(new ExprVar(current, opname), ExprConstInt.one));

		Function sketch = Function.newHelper(current, "sketch", TypePrimitive.inttype ,
				outPar, "spec", body);

		List<Function> funcs = new ArrayList<Function>();

		funcs.add(spec);
		funcs.add(sketch);

		List<StreamSpec> streams = Collections.singletonList(
				new StreamSpec(current, StreamSpec.STREAM_FILTER, null, "MAIN",Collections.EMPTY_LIST , Collections.EMPTY_LIST ,funcs));
		current = new Program(current,streams, Collections.EMPTY_LIST);


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


	public void mergeWithCurrent(CEtrace trace){
		if (reallyVerbose ())
			prog.accept(new SimpleCodePrinter().outputTags());
		pushBlock();

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


		List<step> l = trace.steps;
		Iterator<step> sit = l.iterator();
		step cur;


		for(int i=0; i<nthreads; ++i){ lastNode[i] = null; }

		StringBuffer sbuf = new StringBuffer();

		while(sit.hasNext()){
			cur= sit.next();
			if(cur.thread>0){
				sbuf.append(""+ cur);				
				if( invNodeMap.containsKey(cur.stmt) ){
					int thread = cur.thread-1;
					stepQueues[thread].add(cur);

					if( globalTags.contains(cur.stmt)  ){
						Queue<step> qs = stepQueues[thread];
						while( qs.size() > 0  ){
							step tmp = qs.remove();
							lastNode[thread] = addBlock(tmp.stmt, thread, lastNode[thread]);
						}
					}
				}


			}
		}
		
		log(sbuf.toString());
		if(schedules.containsKey(sbuf.toString())){			
			throw new RuntimeException("I just saw a repeated schedule.");
		}
		schedules.put(sbuf.toString(), schedules.size());


		for(int thread=0; thread<nthreads; ++thread){
			Queue<step> qs = stepQueues[thread];
			while( qs.size() > 0  ){
				step tmp = qs.remove();
				lastNode[thread] = addBlock(tmp.stmt, thread, lastNode[thread]);
			}
		}

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
		if (reallyVerbose ())
			current.accept(new SimpleCodePrinter());
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






	public ValueOracle nextCandidate(CounterExample counterExample) {

		mergeWithCurrent((CEtrace)counterExample);

		current = (Program)current.accept(new EliminateMultiDimArrays());
		boolean tmp = partialEvalAndSolve(current);

		return tmp ? getOracle() : null;
	}



	protected boolean reallyVerbose () {
		return params.flagValue ("verbosity") >= 5;
	}

}
