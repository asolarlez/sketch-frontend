package sketch.compiler.main.par;

import java.util.List;

import sketch.compiler.CommandLineParamManager;
import sketch.compiler.CommandLineParamManager.POpts;
import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.dataflow.deadCodeElimination.EliminateDeadCode;
import sketch.compiler.dataflow.eliminateTransAssign.EliminateTransAssns;
import sketch.compiler.dataflow.preprocessor.FlattenStmtBlocks;
import sketch.compiler.dataflow.preprocessor.PreprocessSketch;
import sketch.compiler.dataflow.preprocessor.SimplifyVarNames;
import sketch.compiler.dataflow.preprocessor.TypeInferenceForStars;
import sketch.compiler.main.seq.SequentialSketchMain;
import sketch.compiler.parallelEncoder.LockPreprocessing;
import sketch.compiler.passes.lowering.*;
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

	public ParallelSketchMain(String[] args){
		super(args);
		stats = new CompilationStatistics (createSynthStats (), createVerifStats ());
	}

	@Override
	public boolean isParallel () {
		return true;
	}

	protected void backendParameters(List<String> commandLineOptions){
		if( params.hasFlag("inbits") ){
			commandLineOptions.add("-overrideInputs");
			commandLineOptions.add( "" + params.flagValue("inbits") );
		}
		if( params.hasFlag("seed") ){
			commandLineOptions.add("-seed");
			commandLineOptions.add( "" + params.flagValue("seed") );
		}
		if( params.hasFlag("cex")){
			commandLineOptions.add("-showinputs");
		}
		if( params.hasFlag("verbosity") && params.flagValue ("verbosity") >= MIN_BACKEND_VERBOSITY){
			commandLineOptions.add("-verbosity");
			commandLineOptions.add( "" + params.flagValue("verbosity") );
		} else {
			commandLineOptions.add("-verbosity");
			commandLineOptions.add(""+ MIN_BACKEND_VERBOSITY);
		}
		if(params.hasFlag("synth")){
			commandLineOptions.add("-synth");
			commandLineOptions.add( "" + params.sValue("synth") );
		}
		if(params.hasFlag("verif")){
			commandLineOptions.add("-verif");
			commandLineOptions.add( "" + params.sValue("verif") );
		}
	}

	
	public String benchmarkName(){
		String rv =super.benchmarkName();
		if(params.hasFlag("playDumb")){
			rv += "dumb";
		}
		if(params.hasFlag("playRandom")){
			rv += "random";
		}
		return rv;
	}


	public void run() {
		System.out.println("Benchmark = " + benchmarkName());
		
		if( params.hasFlag("playDumb")){
			System.out.println("playDumb = YES;");
		}else{
			if(params.hasFlag("playRandom")){
				System.out.println("playDumb = RAND;");
			}else{
				System.out.println("playDumb = NO;");	
			}
						
		}
		try {
			parseProgram();

			prog = (Program)prog.accept(new LockPreprocessing());
			prog = (Program)prog.accept(new ConstantReplacer(params.varValues("D")));
			//dump (prog, "After replacing constants:");
			if (!SemanticChecker.check(prog, isParallel ()))
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
			
			if(params.hasFlag("playRandom")){
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
		boolean useInsertEncoding = params.flagEquals ("reorderEncoding", "exponential");

		//dump (lprog, "before:");

		lprog = (Program) lprog.accept (new SeparateInitializers ());
		lprog = (Program) lprog.accept (new BlockifyRewriteableStmts ());
		lprog = (Program)lprog.accept(new EliminateRegens(varGen));
		//dump (lprog, "~regens");

		lprog = (Program) lprog.accept (new BoundUnboundedLoops (varGen, params.flagValue ("unrollamnt")));
		lprog = (Program) lprog.accept (new AtomizeStatements(varGen));
		lprog = (Program) lprog.accept (new EliminateConditionals(varGen, TypePrimitive.nulltype));
		//dump (lprog, "bounded, ~conditionals, atomized");

		//prog = (Program)prog.accept(new NoRefTypes());
		
		//lprog = (Program)lprog.accept(new ProtectInsertROBlocks());
		
		lprog = (Program)lprog.accept(new EliminateReorderBlocks(varGen, useInsertEncoding));
		//dump (lprog, "~reorderblocks:");
		lprog = (Program)lprog.accept(new EliminateInsertBlocks(varGen));
		dump (lprog, "~insertblocks:");
		lprog = (Program)lprog.accept (new BoundUnboundedLoops (varGen, params.flagValue ("unrollamnt")));
		//dump (lprog, "bounded loops");
		//dump (prog, "bef fpe:");
		lprog = (Program)lprog.accept(new FunctionParamExtension(true));
		//dump (lprog, "fpe:");
		lprog = (Program)lprog.accept(new DisambiguateUnaries(varGen));
		//dump (lprog, "tifs:");
		lprog = (Program)lprog.accept(new TypeInferenceForStars());
		//dump (lprog, "tifs:");
		lprog = (Program) lprog.accept (new EliminateMultiDimArrays ());
		//dump (lprog, "After first elimination of multi-dim arrays:");
		lprog = (Program) lprog.accept (new EliminateConditionals(varGen, TypePrimitive.nulltype));

		lprog = (Program) lprog.accept( new PreprocessSketch( varGen, params.flagValue("unrollamnt"), visibleRControl() ) );

		forCodegen = lprog;

		if(params.flagEquals("showphase", "preproc")) dump (lprog, "After Preprocessing");

		return lprog;
	}

	public void lowerIRToJava() {
		prog = (Program) prog.accept (new MakeAllocsAtomic (varGen));
		Program tmp = prog; 
		prog = (Program) prog.accept( new PreprocessSketch( varGen, params.flagValue("unrollamnt"), visibleRControl(), true, true ) );
		super.lowerIRToJava();
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
		prog = (Program) prog.accept( new PreprocessSketch( varGen, params.flagValue("unrollamnt"), visibleRControl(), false, true ) );
		//dump(prog, "after preproc 2.");

		prog = (Program) prog.accept (new SeparateInitializers ());

		prog = (Program) prog.accept (new TrimDumbDeadCode ());

		prog = (Program) prog.accept(new AddLastAssignmentToFork());

		prog = (Program) prog.accept (new EliminateTransAssns ());
		prog = (Program) prog.accept (new EliminateDeadCode (true));
		//dump (prog, "after dead code/trans assn elim");

		if (params.hasFlag ("simplifySpin")) {	// probably not terribly useful
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
		if (params.hasFlag ("simplifySpin")) {
			prog = MergeLocalStatements.go (prog);
			//dump (prog, "merged local stmts");
		}

		if (params.flagEquals ("showphase", "lowering"))
			dump (prog, "After lowering to intermediate representation:");
	}

	public Program postprocessProgram (Program p) {
		p = (Program) p.accept (new EliminateStarStatic (oracle));

		p=(Program)p.accept(new PreprocessSketch( varGen, params.flagValue("unrollamnt"), visibleRControl(p), true ));

		//p = (Program) p.accept (new Preprocessor (varGen));

		p = (Program)p.accept(new FlattenStmtBlocks());
		if(params.flagEquals("showphase", "postproc")) dump(p, "After partially evaluating generated code.");
		p = (Program)p.accept(new EliminateTransAssns());
		//System.out.println("=========  After ElimTransAssign  =========");
		if(params.flagEquals("showphase", "taelim")) dump(p, "After Eliminating transitive assignments.");
		p = (Program)p.accept(new EliminateDeadCode(params.hasFlag("keepasserts")));
		//dump (p, "After ElimDeadCode");
		p = (Program)p.accept(new SimplifyVarNames());
		p = (Program)p.accept(new AssembleInitializers());
		if(params.flagEquals("showphase", "final")) dump(p, "After Dead Code elimination.");
		return p;
	}

	public void generateCode (Program p) {
		p.accept (new SimpleCodePrinter ());
		//prog.accept (new SimpleCodePrinter ());
		//outputCCode ();
	}

	public Synthesizer createSynth(Program p){
		SATSynthesizer syn = new SATSynthesizer(p, params, internalRControl(), varGen );

		if(params.hasFlag("trace")){
			syn.activateTracing();
		}

		backendParameters(syn.commandLineOptions());
		
		syn.initialize();
		return syn;
	}
	public SolverStatistics createSynthStats () {
		return new SolverStatistics ();
	}


	public Verifier createVerif(Program p){
		int verbosity = params.flagValue ("verbosity");
		boolean cleanup = !params.hasFlag ("keeptmpfiles");
		Configuration config = new Configuration ();

		config.bitWidth (params.flagValue ("cbits"));
		config.detectCycles (false);
		config.stateCompressionPolicy (StateCompressionPolicy.LOSSLESS_COLLAPSE);
		config.checkChannelAssertions (false);
		// we enforce bounds
		config.checkArrayBounds (false);

		SpinVerifier sv = new SpinVerifier (varGen, p, config, verbosity, cleanup,
								 params.flagValue ("vectorszGuess"));

		if(params.hasFlag("simplifySpin")){
			sv.simplifyBeforeSolving();
		}
		return sv;
	}
	public SolverStatistics createVerifStats () {
		return new SolverStatistics ();
	}

	public AbstractValueOracle randomOracle(Program p){
		if (params.hasFlag ("seed"))
			return new RandomValueOracle (new StaticHoleTracker(varGen),
					params.flagValue ("seed"));
		else
			return new RandomValueOracle (new StaticHoleTracker(varGen));
	}

	protected void setCommandLineParams(){
		super.setCommandLineParams();
		params.setAllowedParam("schedlen", new POpts(POpts.NUMBER,
				"--schedlen  n \t Sets the length of the schedule for the parallel sections to n.",
				"10", null) );

		params.setAllowedParam("locklen", new POpts(POpts.NUMBER,
				"--locklen  n \t This is another one of those parameters that have to do with the way\n" +
				"             \t things are implemented. The locks array has to be of a static size. \n" +
				"             \t When you lock on a pointer, the pointer is transformed based on some \n" +
				"             \t strange function, and the resulting value is used to index the lock array. \n" +
				"             \t If that index is out of bounds, your sketch will not resolve, so you use this \n" +
				"             \t parameter to make that lock array larger.",
				"10", null) );

		params.setAllowedParam("vectorszGuess", new POpts(POpts.NUMBER,
				"--vectorszGuess  n \t An initial guess for how many bytes SPIN should use for its\n" +
				"             \t automaton state vector.  The vector size is automatically increased\n" +
				"             \t as necessary, but a good initial guess can reduce the number of calls\n" +
				"             \t to SPIN.",
				""+ SpinVerifier.VECTORSZ_GUESS, null) );

		params.setAllowedParam("simplifySpin", new POpts(POpts.FLAG,
				"--simplifySpin       \t Do simplification on the program before running spin.",
				null, null) );

		params.setAllowedParam("playDumb", new POpts(POpts.FLAG,
				"--playDumb       \t This flag makes the solver really dumb. Don't use it unless you want to see how slow a dumb solver can be.",
				null, null) );
		
		params.setAllowedParam("playRandom", new POpts(POpts.FLAG,
				"--playDumb       \t This flag makes the solver pick candidates at random instead of doing inductive synthesis.",
				null, null) );

	}

	public static void main(String[] args)
	{
	    CommandLineParamManager.reset_singleton();
		new ParallelSketchMain (args).run();
	}
}
