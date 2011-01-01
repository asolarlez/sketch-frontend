package sketch.compiler.codegenerators;

import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.TempVarGen;

public class NodesToCPrintTest extends NodesToC {
    protected final String fcnToTest;

    public NodesToCPrintTest(TempVarGen varGen, String filename, String fcnToTest) {
        super(varGen, filename);
        this.fcnToTest = fcnToTest;
    }

    @Override
    public Object visitProgram(Program prog) {
        return "#include <stdio.h>" + "\n#include <stdlib.h>" + "\n#include <time.h>" +
                "\n" + super.visitProgram(prog) + "\nint main(void) {" + "\n    " +
                fcnToTest + "();" + "\n    return 0;" + "\n}\n";
    }
}
