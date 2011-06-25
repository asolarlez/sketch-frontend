package sketch.compiler.passes.cuda;

import static sketch.util.Misc.nonnull;

import java.util.Vector;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.passes.annotations.CompilerPassDeps;

@CompilerPassDeps(runsBefore = { GenerateAllOrSomeThreadsFunctions.class }, runsAfter = { FlattenStmtBlocks2.class })
public class SplitAssignFromVarDef extends FEReplacer {
    @Override
    public Object visitStmtVarDecl(StmtVarDecl stmt) {
        Vector<Statement> nextStatements = new Vector<Statement>();
        for (int a = 0; a < stmt.getNames().size(); a++) {
            nextStatements.add(new StmtVarDecl(stmt, stmt.getType(a), stmt.getName(a),
                    null));
            if (stmt.getInit(a) != null) {
                nextStatements.add(new StmtAssign(stmt,
                        new ExprVar(stmt, stmt.getName(a)), stmt.getInit(a)));
            }
        }
        if (nextStatements.size() > 1) {
            addStatements(nextStatements.subList(0, nextStatements.size() - 1));
        }
        return nonnull(nextStatements.lastElement());
    }
}
