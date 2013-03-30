package sketch.compiler.cmdline;

import sketch.util.cli.CliAnnotatedOptionGroup;
import sketch.util.cli.CliParameter;

/**
 * Options for the frontend, such as where to find cegis.
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
public class FrontendOptions extends CliAnnotatedOptionGroup {
    public FrontendOptions() {
        super("fe", "miscellaneous frontend options");
    }

    @CliParameter(help = "Path to the 'cegis' binary, overriding default search paths.")
    public String cegisPath = null;
    @CliParameter(metavar = "VAR=val", inlinesep = ",", help = "If the program contains a global variable VAR, it sets its value to val.")
    public String[] def = new String[0];
    @CliParameter(help = "Directory to search for include files.")
    public String[] inc = new String[0];
    @CliParameter(help = "Forces code generation. Even if the sketch fails to resolve, "
            + "this flag will force the synthesizer to produce code from the latest known control values.")
    public boolean forceCodegen;
    @CliParameter(help = "DEPRACATED: The synthesizer guarantees that all asserts will succeed. "
            + "For this reason, all asserts are removed from generated code by default. "
            + "However, sometimes it is useful for debugging purposes to keep the assertions around.")
    public boolean keepAsserts;
    @CliParameter(help = "Remove all assertions from the code, since the synthesizer knows they will all succeed.")
    public boolean killAsserts;

    @CliParameter(help = "Keep intermediate files. Useful for debugging the compiler.")
    public boolean keepTmp;
    @CliParameter(help = "Temporary output directory used to communicate with backend solver.")
    public String output = null;
    @CliParameter(help = "Use this flag if you want the compiler to produce C code.")
    public boolean outputCode;

    @CliParameter(help = "Determine which encoding to use for floats/doubles. \n"
            + "With AS_BIT, it will encode floats with a single bit. \n"
            + "With AS_FFIELD, it will encode them using the field of integers mod 7. \n"
            + "With AS_FIXPOINT, it will encode them using fixedpoint arithmetic. \n")
    public FloatEncoding fpencoding = FloatEncoding.AS_BIT;

    @CliParameter(help = "Set the name of the output C files. By default it is the "
            + "name of the first input file.")
    public String outputProgName;
    @CliParameter(metavar = "FILE", help = "Output the values of holes as XML to a file, \"--\" for stdout")
    public String outputXml = null;
    @CliParameter(help = "Set the directory where you want the generated code to be written.")
    public String outputDir = "./";
    @CliParameter(help = "Produce also a harness to test the generated C code.")
    public boolean outputTest;
    // @CliParameter(help = "Enable Fortran output")
    // public boolean outputFortran;
    @CliParameter(shortname="n", help = "Do not print out code (useful when using printfcn's")
    public boolean noOutputPrint = false;
    @CliParameter(shortname = "y", help = "Print Python code from tprint statements to file")
    public String tprintPython = null;

    public enum FloatEncoding {
        AS_BIT, AS_FFIELD, AS_FIXPOINT
    }

    @CliParameter(help = "Add assume a[i]==0 for i>length(a) for any varlength array a")
    public boolean truncVarArr;

    @CliParameter(help = "Eliminate final structs and structs with no alias")
    public boolean elimFinalStructs;
}
