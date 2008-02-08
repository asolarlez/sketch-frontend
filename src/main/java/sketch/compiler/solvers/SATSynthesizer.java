package streamit.frontend.solvers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import streamit.frontend.CommandLineParamManager;
import streamit.frontend.controlflow.CFG;
import streamit.frontend.controlflow.CFGNode;
import streamit.frontend.nodes.Function;
import streamit.frontend.nodes.Program;
import streamit.frontend.nodes.Statement;
import streamit.frontend.nodes.StmtVarDecl;
import streamit.frontend.nodes.TempVarGen;
import streamit.frontend.parallelEncoder.BreakParallelFunction;
import streamit.frontend.parallelEncoder.CFGforPloop;
import streamit.frontend.parallelEncoder.ExtractPreParallelSection;
import streamit.frontend.solvers.CEtrace.step;
import streamit.frontend.stencilSK.SimpleCodePrinter;
import streamit.frontend.tosbit.ValueOracle;
import streamit.frontend.tosbit.recursionCtrl.RecursionControl;

public class SATSynthesizer extends SATBackend implements Synthesizer {

	Program prog;
	Function parfun;
	Program current;
	BreakParallelFunction parts;
	Set<StmtVarDecl> locals = new HashSet<StmtVarDecl>();
	CFG cfg;
	Map<CFGNode, Set<Object>> nodeMap;	
	List<Statement> bodyl = new ArrayList<Statement>();

	Map<Integer, CFGNode> pctrs;
	
	public SATSynthesizer(Program prog, CommandLineParamManager params, RecursionControl rcontrol, TempVarGen varGen){
		super(params, rcontrol, varGen);
		this.prog = prog;
		ExtractPreParallelSection ps = new ExtractPreParallelSection();
		this.prog = (Program) prog.accept(ps);
		parfun = ps.parfun;		
		parts = new BreakParallelFunction();
		parfun.accept(parts);		
		cfg = CFGforPloop.buildCFG(parts.ploop, locals);
		nodeMap = CFGforPloop.tagSets(cfg);		
		bodyl.addAll(parts.globalDecls);		
	}
	
	
	public void addBlock(step cur){
		
		
		
		
		
		
		
		
	}
	
	
	public void mergeWithCurrent(CEtrace trace){
		prog.accept(new SimpleCodePrinter());
		bodyl.add(parts.prepar);
		List<step> l = trace.steps;
		Iterator<step> sit = l.iterator();
		step cur;
		pctrs = new HashMap<Integer, CFGNode>();
		final int PRE=0, PAR=1, POST=2;
		int state = PRE;
		while(sit.hasNext()){
			cur= sit.next();
			if(cur.thread>0){
				state = PAR;
				
				addBlock(cur);
				
			}else{
				if(state == PRE){
				}else{
					state = POST;
				}
			}
		}
		
	}
	
	
	public ValueOracle nextCandidate(CounterExample counterExample) {
		
		mergeWithCurrent((CEtrace)counterExample);
		
		boolean tmp = partialEvalAndSolve(current);
		
		return tmp ? getOracle() : null;
	}

}
