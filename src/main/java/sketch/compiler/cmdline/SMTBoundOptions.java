package sketch.compiler.cmdline;

import sketch.util.cli.CliParameter;

public class SMTBoundOptions extends BoundOptions {
    @CliParameter(help = "Specify the number of bits to use for integers.")
    public int intbits = 32;
}
