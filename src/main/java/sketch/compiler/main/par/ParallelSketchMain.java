package streamit.frontend;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Map;

import streamit.frontend.nodes.Program;
import streamit.frontend.nodes.Statement;
import streamit.frontend.passes.AtomizeStatements;
import streamit.frontend.passes.ConstantReplacer;
import streamit.frontend.passes.EliminateMultiDimArrays;
import streamit.frontend.passes.NumberStatements;
import streamit.frontend.passes.ProtectArrayAccesses;
import streamit.frontend.passes.SemanticChecker;
import streamit.frontend.solvers.Synthesizer;
import streamit.frontend.solvers.Verifier;
import streamit.frontend.tosbit.ValueOracle;



public class ToPSbitII extends ToSBit {


	
	
	public Synthesizer createSynth(Program p){
		return null;
	}
	
	public Verifier createVerif(Program p){
		return null;
	}
	
	public ValueOracle randomOracle(Program p){
		return null;
	}
	
	public void lowerIRToJava()
	{
		super.lowerIRToJava();
		prog = (Program) prog.accept(new ProtectArrayAccesses(varGen));
		prog = (Program) prog.accept(new NumberStatements());
	}
	
	protected Program preprocessProgram(Program lprog) {
		lprog = super.preprocessProgram(lprog);
		lprog = (Program) lprog.accept (new AtomizeStatements(varGen));
		return lprog;
	}
	
	public void synthVerifyLoop(){
		lowerIRToJava();
	
		Synthesizer synth = createSynth(prog);
		Verifier verif = createVerif(prog);
		
		ValueOracle ora = randomOracle(prog);
		boolean success = false;
		do{
			
			Program cex = verif.verify( ora );
			if(cex == null){
				success = true;
				break;
				//we are done;
			}
			
			ora = synth.nextCandidate(cex);
			if(ora == null){
				success = false;
				break;
			}
		}while(true);
		
		oracle = ora;
		
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

		
		synthVerifyLoop();
						
		
		eliminateStar();
		generateCode();
		System.out.println("DONE");

	}
	
	
	public ToPSbitII(String[] args){
		super(args);
	}
	
	
	
	public static void main(String[] args)
	{
		new ToPSbitII (args).run();
		System.exit(0);
	}
}
