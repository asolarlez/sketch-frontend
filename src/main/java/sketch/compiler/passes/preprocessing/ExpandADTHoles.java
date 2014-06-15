package sketch.compiler.passes.preprocessing;

import java.util.LinkedList;

import sketch.compiler.ast.core.NameResolver;
import sketch.compiler.ast.core.SymbolTable;
import sketch.compiler.ast.core.exprs.ExprNew;
import sketch.compiler.ast.core.exprs.ExprStar;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtSwitch;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.ast.core.typs.TypeStructRef;
import sketch.compiler.passes.lowering.SymbolTableVisitor;
import sketch.util.exceptions.ExceptionAtNode;


public class ExpandADTHoles extends SymbolTableVisitor {
    TypeStructRef ts = null;

    private class ReplaceExprNew extends SymbolTableVisitor {
        Type ts;

        public ReplaceExprNew(Type ts, NameResolver nr, SymbolTable symtab) {
            super(symtab);
            this.ts = ts;
            nres = nr;
        }

        @Override
        public Object visitExprNew(ExprNew exp){

            if (exp.isHole()){
                if(ts.isStruct()){
                    TypeStructRef t = (TypeStructRef) ts;


                    ExprNew newExp =
                            new ExprNew(exp.getContext(), ts, exp.getParams(), true);
                    newExp.setStar(new ExprStar(exp, 5, TypePrimitive.int32type));

                    return newExp;

                }else{
                    //throw error
                    throw new ExceptionAtNode("Type must be of type struct", exp);
                }
            }


            return exp;

        }
    }

    public ExpandADTHoles() {
        super(null);
    }

    @Override
    public Object visitStmtAssign(StmtAssign s) {
        Type t = getType(s.getLHS());
        if (t.isStruct()) {
            ts = (TypeStructRef) t;

            // List<String> children = nres.getStructChildren(t.getName());
            LinkedList<String> queue = new LinkedList<String>();
            queue.add(ts.getName());
            Statement prev = null;
            ReplaceExprNew r = new ReplaceExprNew(ts, nres, symtab);
            StmtAssign newS = (StmtAssign) s.accept(r);
            return newS;

        } else {
            ReplaceExprNew r = new ReplaceExprNew(t, nres, symtab);
            StmtAssign newS = (StmtAssign) s.accept(r);
            return newS;

        }


    }

    @Override
    public Object visitStmtSwitch(StmtSwitch stmt) {

        StmtSwitch newStmt = new StmtSwitch(stmt.getContext(), stmt.getExpr());
        for (String caseExpr : stmt.getCaseConditions()) {

            Statement body = null;

            body = (Statement) stmt.getBody(caseExpr).accept(this);

            body = (Statement) (new CloneHoles()).process(body).accept(this);
            newStmt.addCaseBlock(caseExpr, body);

        }

        return newStmt;

    }



}
