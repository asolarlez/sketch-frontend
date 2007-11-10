package streamit.frontend;

import streamit.frontend.experimental.simplifier.ScalarizeVectorAssignments;
import streamit.frontend.nodes.MakeBodiesBlocks;
import streamit.frontend.nodes.Program;
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
import streamit.frontend.passes.SeparateInitializers;
import streamit.frontend.passes.SpinPreprocessor;
import streamit.frontend.stencilSK.SimpleCodePrinter;
import streamit.frontend.tospin.PromelaCodePrinter;

public class ToSpin extends ToSBit {

	public void generateCode () {
		prog.accept (new PromelaCodePrinter (varGen));
	}


	/**
	 * Transform front-end code to have the Java syntax.  Goes through
	 * a series of lowering passes to convert an IR tree from the
	 * "new" syntax to the "old" Java syntax understood by the main
	 * StreamIt compiler.  Conversion directed towards the StreamIt
	 * Java library, as opposed to the compiler, has slightly
	 * different output, mostly centered around phased filters.
	 *
	 * @param libraryFormat  true if the program is being converted
	 *        to run under the StreamIt Java library
	 * @param varGen  object to generate unique temporary variable names
	 * @returns the converted IR tree
	 */
	public void lowerIRToJava()
	{
		prog = (Program)prog.accept (new MakeAllocsAtomic (varGen));
		//dump (prog, "After making allocations atomic");

		prog = (Program)prog.accept(new EliminateBitSelector(varGen));

		prog = (Program)prog.accept(new EliminateArrayRange(varGen));
		beforeUnvectorizing = prog;

		prog = (Program)prog.accept(new MakeBodiesBlocks());
		//dump (prog, "MBB:");
		prog = (Program)prog.accept(new EliminateStructs(varGen));
		prog = (Program)prog.accept(new DisambiguateUnaries(varGen));
		//dump (prog, "After eliminating structs:");
		prog = (Program)prog.accept(new EliminateMultiDimArrays());
		//dump (prog, "After second elimination of multi-dim arrays:");
		prog = (Program)prog.accept(new ExtractRightShifts(varGen));
		prog = (Program)prog.accept(new ExtractVectorsInCasts(varGen));
		prog = (Program)prog.accept(new SeparateInitializers());
		//dump (prog, "SeparateInitializers:");
		//prog = (Program)prog.accept(new NoRefTypes());
		prog = (Program)prog.accept(new ScalarizeVectorAssignments(varGen, true));

		if( params.hasFlag("showpartial")  ) prog.accept(new SimpleCodePrinter());

		prog = (Program)prog.accept(new EliminateNestedArrAcc());
		//dump (prog, "After lowerIR:");
	}

	/**
	 * Prepare the AST for Promela code generation.
	 */
	public void spinPreprocess () {
		prog = (Program)prog.accept(new SpinPreprocessor(varGen));
		//dump (prog, "After everything constants:");
	}

	public void run()
	{
		parseProgram();

		prog = (Program)prog.accept(new ConstantReplacer(params.varValues("D")));
		//dump (prog, "After replacing constants:");
		//if (!SemanticChecker.check(prog))
		//	throw new IllegalStateException("Semantic check failed");

		prog=preprocessProgram(prog); // perform prereq transformations
		//prog.accept(new SimpleCodePrinter());
		// RenameBitVars is buggy!! prog = (Program)prog.accept(new RenameBitVars());
		//if (!SemanticChecker.check(prog))
		//	throw new IllegalStateException("Semantic check failed");

		lowerIRToJava ();
		spinPreprocess ();
		generateCode ();

		System.out.println("DONE");
	}


	protected ToSpin(String[] args){
		super(args);
	}

	public static void main(String[] args)
	{
		new ToSpin(args).run();
		System.exit(0);
	}
}
