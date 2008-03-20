package streamit.frontend;

import java.util.List;

import streamit.frontend.CommandLineParamManager.POpts;
import streamit.frontend.experimental.deadCodeElimination.EliminateDeadCode;
import streamit.frontend.experimental.eliminateTransAssign.EliminateTransAssns;
import streamit.frontend.experimental.preprocessor.FlattenStmtBlocks;
import streamit.frontend.experimental.preprocessor.PreprocessSketch;
import streamit.frontend.experimental.preprocessor.SimplifyVarNames;
import streamit.frontend.nodes.Program;
import streamit.frontend.parallelEncoder.LockPreprocessing;
import streamit.frontend.passes.AddLastAssignmentToFork;
import streamit.frontend.passes.AssembleInitializers;
import streamit.frontend.passes.AtomizeStatements;
import streamit.frontend.passes.ConstantReplacer;
import streamit.frontend.passes.EliminateConditionals;
import streamit.frontend.passes.EliminateLockUnlock;
import streamit.frontend.passes.MakeAllocsAtomic;
import streamit.frontend.passes.NumberStatements;
import streamit.frontend.passes.ProtectArrayAccesses;
import streamit.frontend.passes.SemanticChecker;
import streamit.frontend.passes.SimpleLoopUnroller;
import streamit.frontend.passes.ProtectArrayAccesses.FailurePolicy;
import streamit.frontend.solvers.CompilationStatistics;
import streamit.frontend.solvers.CounterExample;
import streamit.frontend.solvers.SATSynthesizer;
import streamit.frontend.solvers.SolverStatistics;
import streamit.frontend.solvers.SpinVerifier;
import streamit.frontend.solvers.Synthesizer;
import streamit.frontend.solvers.Verifier;
import streamit.frontend.spin.Configuration;
import streamit.frontend.spin.Configuration.StateCompressionPolicy;
import streamit.frontend.stencilSK.EliminateStarStatic;
import streamit.frontend.stencilSK.SimpleCodePrinter;
import streamit.frontend.stencilSK.StaticHoleTracker;
import streamit.frontend.tosbit.RandomValueOracle;
import streamit.frontend.tosbit.ValueOracle;

public class ToPSbitII extends ToSBit {

	protected CompilationStatistics stats;
	protected boolean success = false;

	public ToPSbitII(String[] args){
		super(args);
		stats = new CompilationStatistics (createSynthStats (), createVerifStats ());
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
		if( params.hasFlag("verbosity") ){
			commandLineOptions.add("-verbosity");
			commandLineOptions.add( "" + params.flagValue("verbosity") );
		}
	}



	public void run() {
		try {
			parseProgram();

			prog = (Program)prog.accept(new LockPreprocessing());
			prog = (Program)prog.accept(new ConstantReplacer(params.varValues("D")));
			//dump (prog, "After replacing constants:");
			if (!SemanticChecker.check(prog))
				throw new IllegalStateException("Semantic check failed");

			prog=preprocessProgram(prog); // perform prereq transformations
			//dump (prog, "After preprocessing");
			// RenameBitVars is buggy!! prog = (Program)prog.accept(new RenameBitVars());
			// if (!SemanticChecker.check(prog))
			// throw new IllegalStateException("Semantic check failed");

			if (prog == null)
				throw new IllegalStateException();

			synthVerifyLoop();
			finalCode = postprocessProgram (prog);
			generateCode(finalCode);
		}
		finally {
			stats.finished (success);
			log (1, ""+ stats);

			if (success) {
				log (0, "[PSKETCH] DONE!");
			} else {
				System.err.println ("[PSKETCH] Error: couldn't synthesize sketch.");
				// TODO: real error message
			}
		}
	}

	public void synthVerifyLoop(){
		lowerIRToJava();

		Synthesizer synth = createSynth(prog);
		Verifier verif = createVerif(prog);

		ValueOracle ora = randomOracle(prog);

		success = false;
		do {
			System.out.println ("Iteration "+ stats.numIterations ());

			CounterExample cex = verif.verify( ora );
			stats.calledVerifier (verif.getLastSolutionStats ());
			if (cex == null) {
				success = true;
				break;
			}

			ora = synth.nextCandidate(cex);
			stats.calledSynthesizer (synth.getLastSolutionStats ());
			if (ora == null) {
				success = false;
				break;
			}
		} while (true);

		oracle = ora;
	}

	protected Program preprocessProgram(Program lprog) {
		lprog = super.preprocessProgram(lprog);
		lprog = (Program) lprog.accept (new EliminateConditionals(varGen));
		dump (lprog, "Before AtomizeStatements:");
		lprog = (Program) lprog.accept (new AtomizeStatements(varGen));
		dump (lprog, "AtomizeStatements:");

		return lprog;
	}

	public void lowerIRToJava() {
		prog = (Program) prog.accept (new MakeAllocsAtomic (varGen));

		super.lowerIRToJava();

		prog = (Program) prog.accept (new EliminateConditionals(varGen));
		prog = (Program) prog.accept(new ProtectArrayAccesses(
				FailurePolicy.ASSERTION, varGen));
		//prog = (Program) prog.accept(new ProtectArrayAccesses(
		//		  FailurePolicy.WRSILENT_RDZERO, varGen));

		prog = (Program) prog.accept(new SimpleLoopUnroller());
		prog = (Program) prog.accept(new EliminateLockUnlock(10, "_lock"));
		prog = (Program) prog.accept( new PreprocessSketch( varGen, params.flagValue("unrollamnt"), visibleRControl() ) );
		// dump(prog, "after preproc 2.");
		prog = (Program) prog.accept(new AddLastAssignmentToFork());
		prog = (Program) prog.accept(new NumberStatements());
		
	}

	public Program postprocessProgram (Program p) {
		p = (Program) p.accept (new EliminateStarStatic (oracle));

		p=(Program)p.accept(new PreprocessSketch( varGen, params.flagValue("unrollamnt"), visibleRControl(), true ));

		p = (Program)p.accept(new FlattenStmtBlocks());
		if(params.flagEquals("showphase", "postproc")) dump(p, "After partially evaluating generated code.");
		p = (Program)p.accept(new EliminateTransAssns());
		//System.out.println("=========  After ElimTransAssign  =========");
		if(params.flagEquals("showphase", "taelim")) dump(p, "After Eliminating transitive assignments.");
		p = (Program)p.accept(new EliminateDeadCode(params.hasFlag("keepasserts")));
		//System.out.println("=========  After ElimDeadCode  =========");
		//finalCode.accept( new SimpleCodePrinter() );
		p = (Program)p.accept(new SimplifyVarNames());
		p = (Program)p.accept(new AssembleInitializers());
		if(params.flagEquals("showphase", "final")) dump(p, "After Dead Code elimination.");
		return p;
	}

	public void generateCode (Program p) {
		//p.accept (new SimpleCodePrinter ());
		prog.accept (new SimpleCodePrinter ());
	}

	public Synthesizer createSynth(Program p){
		SATSynthesizer syn = new SATSynthesizer(p, params, internalRControl(), varGen );
		backendParameters(syn.commandLineOptions);
		return syn;
	}
	public SolverStatistics createSynthStats () {
		return new SolverStatistics ();
	}


	public Verifier createVerif(Program p){
		int verbosity = params.flagValue ("verbosity");
		boolean cleanup = !params.hasFlag ("keeptmpfiles");
		Configuration config = new Configuration ();

		config.detectCycles (false);
		config.stateCompressionPolicy (StateCompressionPolicy.LOSSLESS_COLLAPSE);
		config.checkChannelAssertions (false);
		// we enforce bounds
		config.checkArrayBounds (false);

		return new SpinVerifier (varGen, p, config, verbosity, cleanup,
								 params.flagValue ("vectorszGuess"));
	}
	public SolverStatistics createVerifStats () {
		return new SolverStatistics ();
	}

	public ValueOracle randomOracle(Program p){
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
	}

	public static void main(String[] args)
	{
		new ToPSbitII (args).run();
		System.exit(0);
	}
}
