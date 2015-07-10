package sketch.compiler.passes.lowering;

import java.util.ArrayList;
import java.util.List;

import sketch.compiler.ast.core.SymbolTable;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.ExprTypeCast;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssert;
import sketch.compiler.ast.core.stmts.StmtBlock;
import sketch.compiler.ast.core.stmts.StmtSwitch;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.TypeStructRef;
import sketch.compiler.stencilSK.VarReplacer;

public class OptimizeADT extends SymbolTableVisitor {
    protected TempVarGen varGen;

    public OptimizeADT(TempVarGen varGen) {
        super(null);
        this.varGen = varGen;
    }

    @Override
    public Object visitStmtSwitch(StmtSwitch stmt) {
        stmt = (StmtSwitch) super.visitStmtSwitch(stmt);
        List<String> cases = stmt.getCaseConditions();
        int nCases = cases.size();
        assert nCases > 0 : "StmtSwitch should have >0 cases!";
        if (nCases == 1) {
            // there is only one case, replace the whole thing with the single body.
            String singleCaseName = cases.get(0);
            Statement body = stmt.getBody(singleCaseName);
            if ("default".equals(singleCaseName)) {
                return body;
            }

            ExprVar cond = stmt.getExpr();
            TypeStructRef otype = (TypeStructRef) getType(cond);
            String pkg = nres.getStruct(otype.getName()).getPkg();
            TypeStructRef ntype =
                    (new TypeStructRef(singleCaseName, false)).addDefaultPkg(pkg, nres);

            if (otype.equals(ntype)) {
                return body;
            }
            if (body instanceof StmtAssert) {
                StmtAssert sa = (StmtAssert) body;
                if (sa.getCond().isConstant()) {
                    return sa;
                }
            }
            List<Statement> block = new ArrayList<Statement>();
            String oldName = cond.getName();
            String newName = varGen.nextVar(oldName + "_" + singleCaseName);

            SymbolTable oldSymTab1 = symtab;
            try {
                symtab = new SymbolTable(symtab);
                symtab.registerVar(newName, ntype);
                ExprTypeCast cast = new ExprTypeCast(cond, ntype, cond);
                StmtVarDecl varDecl = new StmtVarDecl(cond, ntype, newName, cast);
                block.add(varDecl);
                VarReplacer vr = new VarReplacer(oldName, newName);
                body = (Statement) body.accept(vr);
                if (body instanceof StmtBlock) {
                    block.addAll(((StmtBlock) body).getStmts());
                } else {
                    block.add(body);
                }
                return new StmtBlock(body, block);
            } finally {
                symtab = oldSymTab1;
            }
        }
        return stmt;
    }
}
