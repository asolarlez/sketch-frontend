package sketch.compiler.passes.lowering;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.NameResolver;
import sketch.compiler.ast.core.Package;
import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtBlock;
import sketch.compiler.ast.core.stmts.StmtDoWhile;
import sketch.compiler.ast.core.stmts.StmtFor;
import sketch.compiler.ast.core.stmts.StmtIfThen;
import sketch.compiler.ast.core.stmts.StmtWhile;


/**
 * Collects mutually exclusive function calls that are on different branches of if-else
 * and switch statements. These are then merged in the backend.
 */
public class CollectFunCallsToCombine extends SymbolTableVisitor {
    int id = 1;
    List<String> recFuns;
    public CollectFunCallsToCombine() {
        super(null);
        recFuns = new ArrayList<String>();
    }

    class CheckRecursive extends FEReplacer {
        final List<String> parents;
        boolean isRec;
        NameResolver nres;

        public CheckRecursive(List<String> names, NameResolver nres) {
            this.parents = names;
            this.isRec = false;
            this.nres = nres;
        }

        @Override
        public Object visitExprFunCall(ExprFunCall exp) {
            if (this.parents.contains(exp.getName())) {
                isRec = true;
            } else if (recFuns.contains(exp.getName())) {
                isRec = true;
            } else {
                List<String> newParents = new ArrayList<String>();
                newParents.addAll(parents);
                newParents.add(exp.getName());

                Function fun = nres.getFun(exp.getName());
                if (fun.getBody() != null) {
                    CheckRecursive cr = new CheckRecursive(newParents, nres);
                    nres.getFun(exp.getName()).getBody().accept(cr);
                    if (cr.isRec) {
                        recFuns.add(exp.getName());
                        isRec = true;
                    }
                }
            }
            return exp;
        }
    }

    @Override
    public Object visitProgram(Program p) {
        nres = new NameResolver(p);
        for (Package pkg : p.getPackages()) {
            nres.setPackage(pkg);
            for (Function f : pkg.getFuncs()) {
                if (!recFuns.contains(f.getName())) {
                    List<String> parents = new ArrayList<String>();
                    parents.add(f.getName());
                    if (f.getBody() != null) {
                        CheckRecursive cr = new CheckRecursive(parents, nres);
                        f.getBody().accept(cr);
                        if (cr.isRec)
                            recFuns.add(f.getName());
                    }
                }
            }
        }

        return super.visitProgram(p);
    }

    @Override
    public Object visitFunction(Function fn) {
        if (fn.hasAnnotation("DontCombine"))
            return fn;
        Statement newBody =
                (fn.getBody() != null) ? (Statement) fn.getBody().accept(this) : null;
        if (newBody == null)
            return fn;
        for (String fname : recFuns) {
            CollectFunCallsFromBody cfc = new CollectFunCallsFromBody(fname, id);
            newBody = (Statement) fn.getBody().accept(cfc);
            id = cfc.id;
        }
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
