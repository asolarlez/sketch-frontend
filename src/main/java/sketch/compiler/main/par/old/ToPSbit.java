package streamit.frontend;

import java.util.List;

import streamit.frontend.CommandLineParamManager.POpts;
import streamit.frontend.experimental.deadCodeElimination.EliminateDeadCode;
import streamit.frontend.experimental.eliminateTransAssign.EliminateTransAssns;
import streamit.frontend.experimental.preprocessor.FlattenStmtBlocks;
import streamit.frontend.experimental.preprocessor.PreprocessSketch;
import streamit.frontend.experimental.preprocessor.SimplifyVarNames;
import streamit.frontend.experimental.simplifier.ScalarizeVectorAssignments;
import streamit.frontend.nodes.MakeBodiesBlocks;
import streamit.frontend.nodes.Program;
import streamit.frontend.parallelEncoder.LockPreprocessing;
import streamit.frontend.parallelEncoder.ProduceParallelModel;
import streamit.frontend.passes.AssembleInitializers;
import streamit.frontend.passes.ConstantReplacer;
import streamit.frontend.passes.DisambiguateUnaries;
import streamit.frontend.passes.EliminateArrayRange;
import streamit.frontend.passes.EliminateBitSelector;
import streamit.frontend.passes.EliminateMultiDimArrays;
import streamit.frontend.passes.EliminateNestedArrAcc;
import streamit.frontend.passes.EliminateStructs;
import streamit.frontend.passes.ExtractRightShifts;
import streamit.frontend.passes.ExtractVectorsInCasts;
import streamit.frontend.passes.MakeAllocsAtomic;
import streamit.frontend.passes.SemanticChecker;
import streamit.frontend.passes.SeparateInitializers;
import streamit.frontend.solvers.SpinVerifier;
import streamit.frontend.stencilSK.EliminateStarStatic;
import streamit.frontend.stencilSK.SimpleCodePrinter;
import streamit.frontend.stencilSK.StaticHoleTracker;
import streamit.frontend.tosbit.ValueOracle;
import streamit.frontend.tosbit.recursionCtrl.RecursionControl;
import streamit.frontend.tosbit.recursionCtrl.ZeroInlineRControl;

public class ToPSbit extends ToSBit {

	public ToPSbit(String[] args) {
		super(args);
		// TODO Auto-generated constructor stub
	}

	public void eliminateStar(){
		finalCode=(Program)beforeUnvectorizing.accept(new EliminateStarStatic(oracle));
		dump(finalCode, "after elim star");
		finalCode=(Program)finalCode.accept(new PreprocessSketch( varGen, params.flagValue("unrollamnt"), visibleRControl(), true ));
		dump(finalCode, "after postproc");
		finalCode = (Program)finalCode.accept(new FlattenStmtBlocks());
		dump(finalCode, "after flattening");
		finalCode = (Program)finalCode.accept(new EliminateTransAssns());
		//System.out.println("=========  After ElimTransAssign  =========");
		//dump(finalCode, "Before DCE");
		finalCode = (Program)finalCode.accept(new EliminateDeadCode(params.hasFlag("keepasserts")));
		finalCode = (Program)finalCode.accept(new SimplifyVarNames());
		finalCode = (Program)finalCode.accept(new AssembleInitializers());

	}



	/**
	 * This function produces a recursion control that is used by all transformations that are not user visible.
	 * In particular, the conversion to boolean.
	 * @return
	 */
	public RecursionControl internalRControl(){
		return new ZeroInlineRControl();
	}


	protected void backendParameters(List<String> commandLineOptions){
		super.backendParameters(commandLineOptions);
		commandLineOptions.add("-inlineamnt");
		commandLineOptions.add( "" + (params.flagValue("schedlen")+1) );
		commandLineOptions.add("-mergeFunctions");
	}


	public void lowerIRToJava()
	{
		prog = (Program)prog.accept(new EliminateBitSelector(varGen));

		prog = (Program)prog.accept(new EliminateArrayRange(varGen));
		beforeUnvectorizing = prog;

		prog = (Program)prog.accept(new MakeBodiesBlocks());
		//dump (prog, "MBB:");
		prog = (Program)prog.accept(new MakeAllocsAtomic(varGen));
		prog = (Program)prog.accept(new EliminateStructs(varGen, params.flagValue("heapsize")));
		prog = (Program)prog.accept(new DisambiguateUnaries(varGen));
		//dump (prog, "After eliminating structs:");
		ProduceParallelModel ppm = new ProduceParallelModel(this.varGen, params.flagValue("schedlen"), params.flagValue("locklen") );
		prog = (Program) prog.accept(ppm);
		prog = (Program) prog.accept(new FlattenStmtBlocks());
		dump (prog, "After producing parallel model:");
		prog = (Program)prog.accept(new EliminateMultiDimArrays());
		//dump (prog, "After second elimination of multi-dim arrays:");
		prog = (Program)prog.accept(new ExtractRightShifts(varGen));
		prog = (Program)prog.accept(new ExtractVectorsInCasts(varGen));
		prog = (Program)prog.accept(new SeparateInitializers());
		//dump (prog, "SeparateInitializers:");
		//prog = (Program)prog.accept(new NoRefTypes());
		prog = (Program)prog.accept(new ScalarizeVectorAssignments(varGen));
		if(params.flagEquals("showphase", "preproc"))
			prog.accept(new SimpleCodePrinter());

		prog = (Program)prog.accept(new EliminateNestedArrAcc());
		//dump (prog, "After lowerIR:");
	}


	public void run()
	{
		parseProgram();

		prog = (Program)prog.accept(new LockPreprocessing());
		dump(prog, "first");
		prog = (Program)prog.accept(new ConstantReplacer(params.varValues("D")));
		//dump (prog, "After replacing constants:");
		if (!SemanticChecker.check(prog))
			throw new IllegalStateException("Semantic check failed");

		prog=preprocessProgram(prog); // perform prereq transformations
		dump (prog, "After preprocessing:");
		//prog.accept(new SimpleCodePrinter());
		// RenameBitVars is buggy!! prog = (Program)prog.accept(new RenameBitVars());
		// if (!SemanticChecker.check(prog))
		//	throw new IllegalStateException("Semantic check failed");

		if (prog == null)
			throw new IllegalStateException();

		oracle = new ValueOracle( new StaticHoleTracker(varGen)/* new SequentialHoleTracker(varGen) */);
		boolean solved = partialEvalAndSolve().successful();
		if (!solved)  System.out.println ("Backend timed out; controls may be synthesized, though.");
		try {
		eliminateStar();
		} catch (Throwable t) {
			if (!solved) System.err.println ("Controls probably weren't synthesized");
			throw new RuntimeException (t);
		}

		if (!solved) {
			// We have controls, but they are not verified.  Give the user the
			// chance to check them in SPIN.
			System.out.println ("-----------------------------------------------------------------------------");
			System.out.println (" Holes were filled, but not verified; trying to verify with SPIN");
			System.out.println ("-----------------------------------------------------------------------------");

			SpinVerifier sv = new SpinVerifier (varGen, finalCode);
			solved = null == sv.verify (oracle);

			if (solved) {
				System.out.println ("The program successfully verifies!");
			} else {
				System.err.println ("UH-OH!  The synthesized controls are not valid.");
				System.err.println ("Spin says:");
				//System.err.println (sv.getOutput ());
				System.out.println ("Try manually checking the following code with 'xspin'");
				ToSpin.printCode (finalCode);
			}
		}
		//generateCode();
		System.out.println("DONE");

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
	}


	public static void main(String[] args)
	{
		new ToPSbit(args).run();
		System.exit(0);
	}

}
