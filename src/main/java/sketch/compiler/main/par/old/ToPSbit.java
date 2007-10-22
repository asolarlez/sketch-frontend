package streamit.frontend;

import streamit.frontend.CommandLineParamManager.POpts;
import streamit.frontend.experimental.simplifier.ScalarizeVectorAssignments;
import streamit.frontend.nodes.MakeBodiesBlocks;
import streamit.frontend.nodes.Program;
import streamit.frontend.parallelEncoder.ProduceParallelModel;
import streamit.frontend.passes.ConstantReplacer;
import streamit.frontend.passes.DisambiguateUnaries;
import streamit.frontend.passes.EliminateArrayRange;
import streamit.frontend.passes.EliminateBitSelector;
import streamit.frontend.passes.EliminateMultiDimArrays;
import streamit.frontend.passes.EliminateNestedArrAcc;
import streamit.frontend.passes.EliminateStructs;
import streamit.frontend.passes.ExtractRightShifts;
import streamit.frontend.passes.ExtractVectorsInCasts;
import streamit.frontend.passes.SemanticChecker;
import streamit.frontend.passes.SeparateInitializers;
import streamit.frontend.stencilSK.SimpleCodePrinter;
import streamit.frontend.stencilSK.StaticHoleTracker;
import streamit.frontend.tosbit.ValueOracle;

public class ToPSbit extends ToSBit {

	public ToPSbit(String[] args) {
		super(args);
		// TODO Auto-generated constructor stub
	}
	
	
	public void lowerIRToJava()
	{
		prog = (Program)prog.accept(new EliminateBitSelector(varGen));

		prog = (Program)prog.accept(new EliminateArrayRange(varGen));
		beforeUnvectorizing = prog;
		
		prog = (Program)prog.accept(new MakeBodiesBlocks());
		//dump (prog, "MBB:");
		prog = (Program)prog.accept(new EliminateStructs(varGen));
		prog = (Program)prog.accept(new DisambiguateUnaries(varGen));
		//dump (prog, "After eliminating structs:");
		ProduceParallelModel ppm = new ProduceParallelModel(this.varGen, params.flagValue("schedlen"));
		prog = (Program) prog.accept(ppm);
		dump (prog, "After producing parallel model:");
		prog = (Program)prog.accept(new EliminateMultiDimArrays());
		dump (prog, "After second elimination of multi-dim arrays:");
		prog = (Program)prog.accept(new ExtractRightShifts(varGen));
		prog = (Program)prog.accept(new ExtractVectorsInCasts(varGen));
		prog = (Program)prog.accept(new SeparateInitializers());
		//dump (prog, "SeparateInitializers:");
		//prog = (Program)prog.accept(new NoRefTypes());
		prog = (Program)prog.accept(new ScalarizeVectorAssignments(varGen));
		if( params.hasFlag("showpartial")  ) prog.accept(new SimpleCodePrinter());

		prog = (Program)prog.accept(new EliminateNestedArrAcc());
		//dump (prog, "After lowerIR:");
	}
	
	
	public void run()
	{
		parseProgram();

		prog = (Program)prog.accept(new ConstantReplacer(params.varValues("D")));
		//dump (prog, "After replacing constants:");
		if (!SemanticChecker.check(prog))
			throw new IllegalStateException("Semantic check failed");

		prog=preprocessProgram(prog); // perform prereq transformations
		//prog.accept(new SimpleCodePrinter());
		// RenameBitVars is buggy!! prog = (Program)prog.accept(new RenameBitVars());
		// if (!SemanticChecker.check(prog))
		//	throw new IllegalStateException("Semantic check failed");
		
		if (prog == null)
			throw new IllegalStateException();

		oracle = new ValueOracle( new StaticHoleTracker(varGen)/* new SequentialHoleTracker(varGen) */);
		partialEvalAndSolve();
		eliminateStar();

		generateCode();
		System.out.println("DONE");

	}
	
	
	protected void setCommandLineParams(){
		super.setCommandLineParams();
		params.setAllowedParam("schedlen", new POpts(POpts.NUMBER,
				"--schedlen  n \t Sets the length of the schedule for the parallel sections to n.",
				"10", null) );
	}
	
	
	public static void main(String[] args)
	{
		new ToPSbit(args).run();
		System.exit(0);
	}

}
