package sketch.compiler.passes.lowering;

import java.util.ArrayList;
import java.util.List;

import sketch.compiler.ast.core.SymbolTable;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.ExprArrayRange;
import sketch.compiler.ast.core.exprs.ExprArrayRange.RangeLen;
import sketch.compiler.ast.core.exprs.ExprBinary;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtBlock;
import sketch.compiler.ast.core.stmts.StmtFor;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;

public class EliminateMDCopies extends SymbolTableVisitor {
    final TempVarGen varGen;

    public EliminateMDCopies(TempVarGen varGen) {
        super(null);
        this.varGen = varGen;
    }


    public Object visitStmtVarDecl(StmtVarDecl stmt) {
        List<Expression> newInits = new ArrayList<Expression>(stmt.getNumVars());
        List<Statement> asslist = new ArrayList<Statement>();
        for (int i = 0; i < stmt.getNumVars(); i++) {
            symtab.registerVar(stmt.getName(i), (stmt.getType(i)), stmt,
                    SymbolTable.KIND_LOCAL);
            Type t = stmt.getType(i);
            if (!(t instanceof TypeArray)) {
                newInits.add(stmt.getInit(i));
                continue;
            }
            TypeArray ta = (TypeArray) t;
            if (!(ta.getBase() instanceof TypeArray)) {
                newInits.add(stmt.getInit(i));
                continue;
            }
            newInits.add(null);
            String name = stmt.getName(i);
            Expression init = stmt.getInit(i);
            if (init != null) // && !(init instanceof ExprArrayInit))
            {
                Statement assign = new StmtAssign(new ExprVar(stmt, name), init);
                asslist.add(assign);
            }
        }
        if (asslist.isEmpty()) {
            return stmt;
        }
        Statement newDecl =
                new StmtVarDecl(stmt, stmt.getTypes(), stmt.getNames(), newInits);
        addStatement(newDecl);

        for (Statement s : asslist) {
            addStatement((Statement) s.accept(this));
        }
        return null;
    }

    public Object visitStmtAssign(StmtAssign sa) {
        Type t = getType(sa.getLHS());
        // Assume all MD assignments are on arrays of the same size.
        if (!(t instanceof TypeArray)) {
            return sa;
        }
        TypeArray ta = (TypeArray) t;
        if (!(ta.getBase() instanceof TypeArray)) {
            return sa;
        }

        String nv = varGen.nextVar();
        Statement nas =
                (new StmtAssign(getRange(sa.getLHS(), new ExprVar(sa, nv)), getRange(
                        sa.getRHS(), new ExprVar(sa, nv))));

        return (Statement) (new StmtFor(nv, ta.getLength(), new StmtBlock(nas))).accept(this);
    }

    Expression getRange(Expression e1, Expression idx) {
        if (e1 instanceof ExprArrayRange) {
            ExprArrayRange ear = (ExprArrayRange) e1;
            RangeLen rl = ear.getSelection();
            if (rl.getLenExpression() != null) {
                return new ExprArrayRange(ear.getBase(), new ExprBinary(rl.start(), "+",
                        idx));
            } else {
                return new ExprArrayRange(e1, idx);
            }
        } else {
            return new ExprArrayRange(e1, idx);
        }

    }

}
