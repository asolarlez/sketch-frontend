package sketch.compiler.passes.lowering;

import java.util.Iterator;

import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtBlock;
import sketch.compiler.ast.core.stmts.StmtDoWhile;
import sketch.compiler.ast.core.stmts.StmtFor;
import sketch.compiler.ast.core.stmts.StmtIfThen;
import sketch.compiler.ast.core.stmts.StmtWhile;

public class CollectFunCallsToCombine extends SymbolTableVisitor {
    int id = 1;
    public CollectFunCallsToCombine() {
        super(null);
    }

    @Override
    public Object visitFunction(Function fn) {
        if (fn.hasAnnotation("DontCombine"))
            return fn;
        Statement newBody =
                (fn.getBody() != null) ? (Statement) fn.getBody().accept(this) : null;
        if (newBody == null)
            return fn;
        CollectFunCallsFromBody cfc = new CollectFunCallsFromBody(fn.getName(), id);
        newBody = (Statement) fn.getBody().accept(cfc);
        id = cfc.id;
        if (newBody != fn.getBody()) {
            return fn.creator().body(newBody).create();
        } else {
            return fn;
        }
    }
    
    private class CollectFunCallsFromBody extends SymbolTableVisitor {
        String funName;
        int id;

        public CollectFunCallsFromBody(String name, int id)
        {
            super(null);
            funName = name;
            this.id = id;
        }

        @Override
        public Object visitStmtIfThen(StmtIfThen stmt) {
            int tmp = id;
            int consFinalId;
            int altFinalId;
            // Process cons
            processStatement(stmt.getCons());

            consFinalId = id;
            id = tmp;
            // Process alt
            processStatement(stmt.getAlt());

            altFinalId = id;
            id = Math.max(consFinalId, altFinalId);
            return stmt;
        }

        private void processStatement(Statement stmt) {
            if (stmt == null)
                return;
            FlattenStmtBlocks f = new FlattenStmtBlocks();
            stmt = (Statement) stmt.accept(f);
            if (stmt.isBlock()) {
                Iterator<Statement> itr = ((StmtBlock) stmt).getStmts().iterator();
                while (itr.hasNext()) {
                    Statement s = itr.next();
                    GetFunCall c = new GetFunCall(funName, this);
                    s.doStatement(c);

                }
            } else {
                // deal with single statements
                GetFunCall c = new GetFunCall(funName, this);
                stmt.doStatement(c);

            }
        }

    }

    /*
     * This class extracts the recursive funCall from a statement block.
     */
    private class GetFunCall extends SymbolTableVisitor {
        String funName;
        CollectFunCallsFromBody cfc;

        public GetFunCall(String name, CollectFunCallsFromBody cfc) {
            super(null);
            funName = name;
            this.cfc = cfc;
        }

        @Override
        public Object visitExprFunCall(ExprFunCall exp) {
            if (exp.getName().equals(funName))
                exp.setClusterId(cfc.id++);
            return exp;

        }

        @Override
        public Object visitStmtIfThen(StmtIfThen stmt) {
            stmt.accept(cfc);
            return stmt;
        }

        // Ignore function calls in these statements because they are tricky to merge
        @Override
        public Object visitStmtFor(StmtFor stmt) {
            return stmt;
        }

        @Override
        public Object visitStmtDoWhile(StmtDoWhile stmt) {
            return stmt;
        }

        @Override
        public Object visitStmtWhile(StmtWhile stmt) {
            return stmt;
        }

        @Override
        public Object visitFunction(Function f) {
            return f;
        }
    }

}
