package sketch.compiler.cmdline;

import sketch.util.cli.CliAnnotatedOptionGroup;
import sketch.util.cli.CliOptional;
import sketch.util.cli.CliParameter;

/**
 * Bounded model checking options, e.g. the number of bits for integer holes.
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
public class BoundOptions extends CliAnnotatedOptionGroup {
    public BoundOptions() {
        super("bnd", "bounds for bounded model checking");
    }

    @CliParameter(help = "This flag is also used for recursion control. It bounds "
            + "inlining based on the idea that if a function calls itself recureively ten "
            + "times, we want to inline it less than a function that calls itself "
            + "recursively only once. In this case, n is the maximum value of "
            + "the branching factor, which is the number of times a function "
            + "calls itself recursively, times the amount of inlining.")
    public int branchAmnt = 15;
    @CliParameter(help = "The number of bits to use for integer holes.")
    public int cbits = 5;

    @CliParameter(help = "The number of bits to use for minimize bound.")
    public int mbits = 5;

    @CliParameter(help = "The number of bits to use for integer inputs.")
    public int inbits = 5;

    @CliParameter(help = "The number of bits to use for integer angelic ctrls.")
    public int angelicbits = -1;

    @CliParameter(help = "The max length of angelic array.")
    public int angelicArrsz = -1;

    @CliParameter(help = "(Deprecated) System will ignore this flag.")
    public int heapSize = 11;

    @CliParameter(help = "Max size of DAG")
    public int dagSize = -1;

    @CliParameter(help = "Tells the solver to incrementally grow the size of integer "
            + "holes from 1 to n bits.")
    public CliOptional<Integer> incremental = new CliOptional<Integer>(5);

    @CliParameter(help = "Bounds inlining to n levels of recursion, so each "
            + "function can appear at most n times in the stack.")
    public int inlineAmnt = 5;

    @CliParameter(help = "Determine whether inline-amnt will bound the number of times a "
            + "callsite will appear on the stack (CALLSITE) or the number of times a call name will appear on the stack (CALLNAME).")
    public BoundMode boundMode = BoundMode.CALLNAME;

    public enum BoundMode {
        CALLSITE, CALLNAME
    }

    @CliParameter(help = "The unroll ammount for loops.")
    public int unrollAmnt = 8;
    
    @CliParameter(help = "Max Size for arrays of unknown size.")
    public int arrSize = 32;
    
    @CliParameter(help = "Max Size for one dimensional arrays of unknown size.")
    public int arr1dSize = 32;
    
    @CliParameter(help = "Initial value to start with when minimizing "
            + "expressions/cost functions")
    public int costEstimate = 32;

    @CliParameter(help = "Maximum absolute value of integers modeled by the system; -1 means there is no bound.")
    public int intRange = -1;

    @CliParameter(help = "Maximum depth of src tuples")
    public int srcTupleDepth = 2;
    
    @CliParameter(help = "Maximum depth for ADT equality comparisons")
    public int eqDepth = 6;

    @CliParameter(help = "Maximum depth of angelic tuples")
    public int angTupleDepth = 1;

    @CliParameter(help = "Maximum depth of GUC")
    public int gucDepth = 3;

}
