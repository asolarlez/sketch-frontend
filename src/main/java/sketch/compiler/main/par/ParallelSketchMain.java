package sketch.compiler.main.par;

import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.cmdline.SolverOptions.ReorderEncoding;
import sketch.compiler.dataflow.deadCodeElimination.EliminateDeadCode;
import sketch.compiler.dataflow.eliminateTransAssign.EliminateTransAssns;
import sketch.compiler.dataflow.preprocessor.FlattenStmtBlocks;
import sketch.compiler.dataflow.preprocessor.PreprocessSketch;
import sketch.compiler.dataflow.preprocessor.SimplifyVarNames;
import sketch.compiler.dataflow.preprocessor.TypeInferenceForStars;
import sketch.compiler.main.seq.SequentialSketchMain;
import sketch.compiler.parallelEncoder.LockPreprocessing;
import sketch.compiler.passes.lowering.*;
import sketch.compiler.passes.lowering.SemanticChecker.ParallelCheckOption;
import sketch.compiler.passes.printers.SimpleCodePrinter;
import sketch.compiler.solvers.CompilationStatistics;
import sketch.compiler.solvers.CounterExample;
import sketch.compiler.solvers.SATSynthesizer;
import sketch.compiler.solvers.SolverStatistics;
import sketch.compiler.solvers.SpinVerifier;
import sketch.compiler.solvers.Synthesizer;
import sketch.compiler.solvers.Verifier;
import sketch.compiler.solvers.constructs.AbstractValueOracle;
import sketch.compiler.solvers.constructs.RandomValueOracle;
import sketch.compiler.solvers.constructs.StaticHoleTracker;
import sketch.compiler.spin.Configuration;
import sketch.compiler.spin.Configuration.StateCompressionPolicy;
import sketch.compiler.stencilSK.EliminateStarStatic;

public class ParallelSketchMain extends SequentialSketchMain {
    

	protected static final int MIN_BACKEND_VERBOSITY = 6;

	protected CompilationStatistics stats;
	protected boolean success = false;

	protected Program forCodegen = null;
	
	public final ParallelSketchOptions options;
	
	public ParallelSketchMain(String[] args) {
		super(new ParallelSketchOptions(args));
		this.options = (ParallelSketchOptions) super.options;
		stats = new CompilationStatistics (createSynthStats (), createVerifStats ());
	}

	@Override
	public boolean isParallel () {
		return true;
	}

	
	public String benchmarkName(){
		String rv =super.benchmarkName();
        if (options.parOpts.playDumb) {
			rv += "dumb";
		}
        if (options.parOpts.playRandom) {
			rv += "random";
		}
		return rv;
	}


	public void run() {
		System.out.println("Benchmark = " + benchmarkName());
		
        if (options.parOpts.playDumb) {
			System.out.println("playDumb = YES;");
		}else{
            if (options.parOpts.playRandom) {
				System.out.println("playDumb = RAND;");
			}else{
				System.out.println("playDumb = NO;");	
			}
						
		}
		try {
			parseProgram();

			prog = (Program)prog.accept(new LockPreprocessing());
			//dump (prog, "After replacing constants:");
			ParallelCheckOption parallelCheck = isParallel() ? ParallelCheckOption.PARALLEL : ParallelCheckOption.SERIAL;
            if (!SemanticChecker.check(prog, parallelCheck, true))
				throw new IllegalStateException("Semantic check failed");

			prog=preprocessProgram(prog); // perform prereq transformations
			//dump (prog, "After preprocessing");
			// RenameBitVars is buggy!! prog = (Program)prog.accept(new RenameBitVars());
			// if (!SemanticChecker.check(prog))
			// throw new IllegalStateException("Semantic check failed");

			if (prog == null)
				throw new IllegalStateException();

			synthVerifyLoop();

			if(!success){
				return;
			}

			finalCode = postprocessProgram (beforeUnvectorizing);//postprocessProgram (forCodegen);
			generateCode(finalCode);
		}
		finally {
			stats.finished (success);
			log (1, ""+ stats);

			if (success) {
				log (0, "[PSKETCH] DONE!");
			} else {
				System.err.println ("[ERROR] [PSKETCH] Error: couldn't synthesize sketch.");
				// TODO: real error message
			}
		}
	}

	public void synthVerifyLoop(){
		lowerIRToJava();
		Synthesizer synth = createSynth(prog);
		Verifier verif = createVerif(prog);

		AbstractValueOracle ora = randomOracle(prog);

		if (!isSketch (prog)) {
			System.out.println ("Verifying non-sketched program ...");
			success = (null == verif.verify (ora));
			stats.calledVerifier (verif.getLastSolutionStats ());
			if (success)  oracle = ora;	// is this necessary?
			return;
		}

		success = false;
		int i=0;
		do {
			System.out.println ("Iteration "+ (i++) /* stats.numIterations ()*/);

			CounterExample cex = verif.verify( ora );
			stats.calledVerifier (verif.getLastSolutionStats ());
			if (cex == null) {
				success = true;
				break;
			}
			
            if (options.parOpts.playRandom) {
				ora = new RandomValueOracle (new StaticHoleTracker(varGen));
			}else{
				ora = synth.nextCandidate(cex);				
				stats.calledSynthesizer (synth.getLastSolutionStats ());
				if (ora == null) {
					success = false;
					break;
				}
			}
		} while (true);
		synth.cleanup();
		stats.finalSynthStats(synth.getLastSolutionStats());
		oracle = ora;
	}

	protected Program preprocessProgram(Program lprog) {
	    boolean useInsertEncoding =
            (options.solverOpts.reorderEncoding == ReorderEncoding.exponential);

		//dump (lprog, "before:");

		lprog = (Program) lprog.accept (new SeparateInitializers ());
		lprog = (Program) lprog.accept (new BlockifyRewriteableStmts ());
		lprog = (Program)lprog.accept(new EliminateRegens(varGen));
		//dump (lprog, "~regens");

        lprog = (Program) lprog.accept(new BoundUnboundedLoops(varGen,
                        options.bndOpts.unrollAmnt));
		lprog = (Program) lprog.accept (new AtomizeStatements(varGen));
		lprog = (Program) lprog.accept (new EliminateConditionals(varGen, TypePrimitive.nulltype));
		//dump (lprog, "bounded, ~conditionals, atomized");

		//prog = (Program)prog.accept(new NoRefTypes());
		
		//lprog = (Program)lprog.accept(new ProtectInsertROBlocks());
		
		lprog = (Program)lprog.accept(new EliminateReorderBlocks(varGen, useInsertEncoding));
		//dump (lprog, "~reorderblocks:");
		lprog = (Program)lprog.accept(new EliminateInsertBlocks(varGen));
		dump (lprog, "~insertblocks:");
		lprog = (Program)lprog.accept (new BoundUnboundedLoops (varGen, options.bndOpts.unrollAmnt));
		//dump (lprog, "bounded loops");
		//dump (prog, "bef fpe:");
		lprog = (Program)lprog.accept(new FunctionParamExtension(true));
		//dump (lprog, "fpe:");
		lprog = (Program)lprog.accept(new DisambiguateUnaries(varGen));
		//dump (lprog, "tifs:");
		lprog = (Program)lprog.accept(new TypeInferenceForStars());
		//dump (lprog, "tifs:");
		lprog = (Program) lprog.accept (new EliminateMultiDimArrays (varGen));
		//dump (lprog, "After first elimination of multi-dim arrays:");
		lprog = (Program) lprog.accept (new EliminateConditionals(varGen, TypePrimitive.nulltype));

		lprog = (Program) lprog.accept( new PreprocessSketch( varGen, options.bndOpts.unrollAmnt, visibleRControl() ) );

		forCodegen = lprog;

		if (showPhaseOpt("preproc")) {
		    dump (lprog, "After Preprocessing");
		}

		return lprog;
	}

	public void lowerIRToJava() {
		prog = (Program) prog.accept (new MakeAllocsAtomic (varGen));
		Program tmp = prog; 
		prog = (Program) prog.accept( new PreprocessSketch( varGen, options.bndOpts.unrollAmnt, visibleRControl(), true, true ) );
		super.lowerToSketch();
		beforeUnvectorizing = tmp;
		prog = (Program) prog.accept (new EliminateConditionals(varGen));
		// dump (prog, "elim conds");
//		prog = (Program) prog.accept(new ProtectArrayAccesses(
//				FailurePolicy.ASSERTION, varGen));
		//prog = (Program) prog.accept(new ProtectArrayAccesses(
		//		  FailurePolicy.WRSILENT_RDZERO, varGen));
		//dump (prog, "protect array accesses");

		prog = (Program) prog.accept(new UnrollLocalLoops());
		prog = (Program) prog.accept(new EliminateLockUnlock(10, "_lock"));
		//dump(prog, "after elimlocks.");
		prog = (Program) prog.accept( new PreprocessSketch( varGen, options.bndOpts.unrollAmnt, visibleRControl(), false, true ) );
		//dump(prog, "after preproc 2.");

		prog = (Program) prog.accept (new SeparateInitializers ());

		prog = (Program) prog.accept (new TrimDumbDeadCode ());

		prog = (Program) prog.accept(new AddLastAssignmentToFork());

		prog = (Program) prog.accept (new EliminateTransAssns ());
		prog = (Program) prog.accept (new EliminateDeadCode (true));
		//dump (prog, "after dead code/trans assn elim");

        if (options.parOpts.simplifySpin) { // probably not terribly useful
			//prog = MinimizeLocalVariables.go (prog);
			//dump(prog, "after reg alloc");
			prog = (Program) prog.accept (new SeparateInitializers());
			prog = (Program) prog.accept (new HoistDeclarations ());
			//dump (prog, "hoisted decls");
		}
		
		prog = (Program) prog.accept(new ScrubStructType());

		prog = (Program) prog.accept(new NumberStatements());


		
		prog = (Program) prog.accept(new EliminateZeroArrays());
		
		prog = MergeLocalStatements.go (prog);
		
		// dump(prog, "after eza.");
		if (options.parOpts.simplifySpin) {
			prog = MergeLocalStatements.go (prog);
			//dump (prog, "merged local stmts");
		}

        if (showPhaseOpt("lowering"))
			dump (prog, "After lowering to intermediate representation:");
	}

	public Program postprocessProgram (Program p) {
		p = (Program) p.accept (new EliminateStarStatic (oracle));

        p = (Program) p.accept(new PreprocessSketch(varGen,
                        options.bndOpts.unrollAmnt, visibleRControl(p), true));
		//p = (Program) p.accept (new Preprocessor (varGen));

		p = (Program)p.accept(new FlattenStmtBlocks());
        if (showPhaseOpt("postproc"))
            dump(p, "After partially evaluating generated code.");
		p = (Program)p.accept(new EliminateTransAssns());
		//System.out.println("=========  After ElimTransAssign  =========");
        if (showPhaseOpt("taelim"))
            dump(p, "After Eliminating transitive assignments.");
		p = (Program)p.accept(new EliminateDeadCode(options.feOpts.keepAsserts));
		//dump (p, "After ElimDeadCode");
		p = (Program)p.accept(new SimplifyVarNames());
		p = (Program)p.accept(new AssembleInitializers());
        if (showPhaseOpt("final"))
            dump(p, "After Dead Code elimination.");
		return p;
	}

	public void generateCode (Program p) {
		p.accept (new SimpleCodePrinter ());
		//prog.accept (new SimpleCodePrinter ());
		//outputCCode ();
	}

	public Synthesizer createSynth(Program p){
		SATSynthesizer syn = new SATSynthesizer(p, options, internalRControl(), varGen );

		if(options.debugOpts.trace){
			syn.activateTracing();
		}

		backendParameters();
		
		syn.initialize();
		return syn;
	}
	public SolverStatistics createSynthStats () {
		return new SolverStatistics ();
	}


	public Verifier createVerif(Program p){
		int verbosity = options.debugOpts.verbosity;
		boolean cleanup = !options.feOpts.keepTmp;
		Configuration config = new Configuration ();

		config.bitWidth (options.bndOpts.cbits);
		config.detectCycles (false);
		config.stateCompressionPolicy (StateCompressionPolicy.LOSSLESS_COLLAPSE);
		config.checkChannelAssertions (false);
		// we enforce bounds
		config.checkArrayBounds (false);

		SpinVerifier sv = new SpinVerifier (varGen, p, config, verbosity, cleanup,
								 options.parOpts.vectorszGuess);

		if(options.parOpts.simplifySpin){
			sv.simplifyBeforeSolving();
		}
		return sv;
	}
	public SolverStatistics createVerifStats () {
		return new SolverStatistics ();
	}

	public AbstractValueOracle randomOracle(Program p){
		if (options.solverOpts.seed != 0)
			return new RandomValueOracle (new StaticHoleTracker(varGen),
			        options.solverOpts.seed);
		else
			return new RandomValueOracle (new StaticHoleTracker(varGen));
	}

	public static void main(String[] args)
	{
	    checkJavaVersion(1, 6);
		new ParallelSketchMain (args).run();
	}
}
