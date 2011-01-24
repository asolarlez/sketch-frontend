package sketch.compiler.passes.lowering;

import sketch.compiler.ast.core.stmts.StmtImplicitVarDecl;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.passes.annotations.CompilerPassDeps;

import static sketch.util.DebugOut.assertFalse;

/**
 * replace implicit variable declarations by explicit ones
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
@CompilerPassDeps(runsBefore = {}, runsAfter = {})
public class ReplaceImplicitVarDecl extends SymbolTableVisitor {
    public ReplaceImplicitVarDecl() {
        super(null);
    }

    public Object visitStmtImplicitVarDecl(StmtImplicitVarDecl decl) {
        Type typ = getType(decl.getInitExpr());
        if (typ == null) {
            assertFalse("Could not deduce implicit declaration type for", typ);
        }
        return new StmtVarDecl(decl, typ, decl.getName(), decl.getInitExpr());
    };
}
