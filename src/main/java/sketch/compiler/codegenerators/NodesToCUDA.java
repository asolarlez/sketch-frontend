package sketch.compiler.codegenerators;

import sketch.compiler.ast.core.TempVarGen;

public class NodesToCUDA extends NodesToC {
    public NodesToCUDA(TempVarGen varGen, String filename) {
        super(varGen, filename);
    }
}
