package streamit.frontend;

import streamit.frontend.nodes.Program;
import streamit.frontend.passes.ConstantReplacer;
import streamit.frontend.passes.SpinPreprocessor;
import streamit.frontend.stencilSK.StaticHoleTracker;
import streamit.frontend.tosbit.ValueOracle;
import streamit.frontend.tospin.PromelaCodePrinter;

public class ToSpin extends ToSBit {

	public void generateCode () {
		prog.accept (new PromelaCodePrinter ());
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

		prog = (Program)prog.accept(new SpinPreprocessor(varGen));

		//dump (prog, "After everything constants:");

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
