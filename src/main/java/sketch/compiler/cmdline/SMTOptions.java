package sketch.compiler.cmdline;

import sketch.util.cli.CliAnnotatedOptionGroup;
import sketch.util.cli.CliParameter;

/**
 * options for the SMT solver
 * 
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
public class SMTOptions extends CliAnnotatedOptionGroup {
    public SMTOptions() {
        super("smt", "SMT (satisfiability modulo theory) solver options");
    }

    @CliParameter(help = "Use theory of array.")
    public boolean theoryOfArray = false;
    @CliParameter(help = "Function hashing.")
    public boolean funcHash = false;
    @CliParameter(help = "Canonicalize arithmetics.")
    public boolean canon = false;
    @CliParameter(help = "Enable Common Subexpession Elimination.")
    public boolean cse = false;
    @CliParameter(help = "Enable Full Common Subexpession Elimination.")
    public boolean cse2 = false;
    @CliParameter(help = "Linearize arithmetics.")
    public boolean linear = false;
    @CliParameter(help = "Use LET construct.")
    public boolean useLet = false;
    @CliParameter(help = "Choose the backend of the SMT code. options: 'cvc3', 'cvc3smtlib'")
    public SMTSolver backend = SMTSolver.cvc3;
    @CliParameter(help = "Path to the SMT solver executable.")
    public String solverpath = "cvc3";
    @CliParameter(help = "model of integers to use. options: 'integer', 'bv'.")
    public IntModel intmodel = IntModel.bv;

    public enum SMTSolver {
        cvc3, cvc3smtlib, z3, beaver, yices, yices2, stp, stpyices2
    }

    public enum IntModel {
        integer, bv;
    }
}
