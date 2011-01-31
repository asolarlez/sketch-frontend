package sketch.compiler.passes.preprocessing;

import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.stmts.StmtImplicitVarDecl;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.passes.annotations.CompilerPassDeps;
import sketch.compiler.passes.lowering.SymbolTableVisitor;

import static sketch.util.DebugOut.assertFalse;
import static sketch.util.DebugOut.printWarning;

/**
 * warn on e.g. "v := 1" (could be bit or int)
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
@CompilerPassDeps(runsBefore = {}, runsAfter = {})
public class WarnAmbiguousImplicitVarDecl extends SymbolTableVisitor {
    public WarnAmbiguousImplicitVarDecl() {
        super(null);
    }

    public Object visitStmtImplicitVarDecl(StmtImplicitVarDecl decl) {
        Type typ = getType(decl.getInitExpr());
        if (typ == null) {
            assertFalse("Could not deduce implicit declaration type for", typ);
        }
        if (typ.equals(TypePrimitive.bittype) &&
                decl.getInitExpr() instanceof ExprConstInt)
        {
            printWarning("assuming const int is an integer", decl.getName());
            return (new StmtVarDecl(decl, TypePrimitive.inttype, decl.getName(),
                    decl.getInitExpr())).accept(this);
        }

        // register the variable
        super.visitStmtImplicitVarDecl(decl);
        return decl;
    };
}
