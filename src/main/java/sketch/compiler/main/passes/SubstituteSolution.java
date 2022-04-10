package sketch.compiler.main.passes;

import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.main.cmdline.SketchOptions;
import sketch.compiler.solvers.constructs.ValueOracle;
import sketch.compiler.stencilSK.EliminateHoleStatic;
import sketch.transformer.SketchTransformerDriver;

/**
 * Substitute a solution into a program, and simplify the program.
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
public class SubstituteSolution extends MetaStage {
	protected final ValueOracle value_oracle;

    public SubstituteSolution(TempVarGen varGen, SketchOptions options,
            ValueOracle solution)
    {
        super("subst", "Substitute a solution (assignment to ??'s) into the sketch",
                varGen, options);
		this.value_oracle = solution;
    }

    @Override
	public Program visitProgramInner(Program program) {

		// HERE CALL FMTL TRANSOFRM AST <<<< call the interpreter here.
		// each function: init; clone; replace; concretize will be a class
		// derived from FEReplacer
		// e.g. for clone I
		// class ApplyTransformations:
		// for each line in ftml program
		// calls each of the visitors.
    	
		SketchTransformerDriver driver = new SketchTransformerDriver(value_oracle.get_code_block());
		System.out.println("IN SubstituteSolution.visitProgramInner; RUN SK_TRANSFORMER DRIVER.");
		driver.run(program);

		// BEFORE REACHING HERE THE PROGRAM TRANSFORMATION SHOULD ALREADY BE APPLIED
		System.out.println("DONE WITH APPLYING PROGRAM TRANSFORMATION.");
		assert (false);

		EliminateHoleStatic eliminate_hole = new EliminateHoleStatic(value_oracle);
		Program p = (Program) program.accept(eliminate_hole);

        if (options.feOpts.outputXml != null) {
			eliminate_hole.dump_xml(options.feOpts.outputXml);
        }

        return p;
    }
}
