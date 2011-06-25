package sketch.compiler.passes.structure;

import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Program;

/**
 * Whether any of the functions are annotated "stencil".
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
public class ContainsStencilFunction extends ASTQuery {
    @Override
    public Object visitProgram(Program prog) {
        CallGraph cg = new CallGraph(prog);
        for (Function f : cg.allFcns) {
            if (f.isStencil()) {
                this.result = true;
            }
        }
        return prog;
    }
}
