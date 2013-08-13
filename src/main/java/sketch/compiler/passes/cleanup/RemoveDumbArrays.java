package sketch.compiler.passes.cleanup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.exprs.ExprArrayInit;
import sketch.compiler.ast.core.exprs.ExprArrayRange;
import sketch.compiler.ast.core.exprs.ExprArrayRange.RangeLen;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;

public class RemoveDumbArrays extends FEReplacer {
    public class FindDumbArrays extends FEReplacer {
        public Map<String, Integer> dumbArr = new HashMap<String, Integer>();

        public Object visitStmtVarDecl(StmtVarDecl svd) {
            for (int i = 0; i < svd.getNumVars(); ++i) {
                if (svd.getType(i) instanceof TypeArray) {
                    TypeArray ta = (TypeArray) svd.getType(i);
                    Integer ln = ta.getLength().getIValue();
                    Expression init = svd.getInit(i);
                    if (ln != null && (init == null || init instanceof ExprArrayInit)) {
                        if (init != null) {
                            ExprArrayInit eai = (ExprArrayInit) init;
                            if (eai.getElements().size() != ln) {
                                continue;
                            }
                        }
                        dumbArr.put(svd.getName(i), ln);
                    }
                }
            }
            return super.visitStmtVarDecl(svd);
        }

        public Object visitStmtAssign(StmtAssign sa) {
            if (sa.getLHS() instanceof ExprVar && sa.getRHS() instanceof ExprArrayInit) {
                String name = sa.getLHS().toString();
                if (dumbArr.containsKey(name)) {
                    if (dumbArr.get(name) == ((ExprArrayInit) sa.getRHS()).getElements().size())
                    {
                        sa.getRHS().accept(this);
                        return sa;
                    }
                }
            }
            return super.visitStmtAssign(sa);
        }

        @Override
        public Object visitExprArrayRange(ExprArrayRange ear) {
            if (ear.getBase() instanceof ExprVar) {
                String base = ear.getBase().toString();
                if (dumbArr.containsKey(base)) {
                    RangeLen rl = ear.getSelection();
                    if (rl.hasLen() || !(rl.start() instanceof ExprConstInt)) {
                        dumbArr.remove(base);
                    }
                }
                RangeLen range = ear.getSelection();
                Expression newStart = doExpression(range.start());
                Expression newLen = null;
                if (range.hasLen()) {
                    newLen = doExpression(range.getLenExpression());
                }
                return ear;
            } else {
                return super.visitExprArrayRange(ear);
            }
        }


        public Object visitExprVar(ExprVar ev) {
            if (dumbArr.containsKey(ev.getName())) {
                dumbArr.remove(ev.getName());
            }
            return ev;
        }
    }

    FindDumbArrays fda;

    public Object visitFunction(Function f) {
        fda = new FindDumbArrays();
        f.accept(fda);
        return super.visitFunction(f);
    }

    public Object visitStmtVarDecl(StmtVarDecl stmt) {
        List<Expression> newInits = new ArrayList<Expression>();
        List<Type> newTypes = new ArrayList<Type>();
        List<String> newNames = new ArrayList<String>();
        boolean changed = false;
        for (int i = 0; i < stmt.getNumVars(); i++) {
            String name = stmt.getName(i);
            if (fda.dumbArr.containsKey(name)) {
                changed = true;
                Type ot = stmt.getType(i);
                TypeArray t = (TypeArray) ot.accept(this);
                Expression oinit = stmt.getInit(i);
                Expression init = null;
                if (oinit != null)
                    init = doExpression(oinit);
                int n = t.getLength().getIValue();
                for (int ii = 0; ii < n; ++ii) {
                    newNames.add(name + "_" + ii);
                    newTypes.add(t.getBase());
                    if (init != null) {
                        assert (init instanceof ExprArrayInit);
                        ExprArrayInit eai = (ExprArrayInit) init;
                        newInits.add(eai.getElements().get(ii));
                    } else {
                        newInits.add(null);
                    }
                }
            } else {
                Expression oinit = stmt.getInit(i);
                Expression init = null;
                if (oinit != null)
                    init = doExpression(oinit);
                Type ot = stmt.getType(i);
                Type t = (Type) ot.accept(this);
                if (ot != t || oinit != init) {
                    changed = true;
                }
                newInits.add(init);
                newTypes.add(t);
                // NOTE xzl: don't forget to add the old name! this is a bug fix
                newNames.add(stmt.getName(i));
            }
        }
        if (!changed) {
            return stmt;
        }
        return new StmtVarDecl(stmt, newTypes, newNames, newInits);
    }

    public Object visitStmtAssign(StmtAssign stmt)
    {
        String base = stmt.getLHS().toString();
        if(fda.dumbArr.containsKey(base)){
            int N = fda.dumbArr.get(base);
            Expression newRHS = doExpression(stmt.getRHS());
            assert newRHS instanceof ExprArrayInit;
            ExprArrayInit eai = (ExprArrayInit) newRHS;
            for(int i=0; i<N; ++i){
                addStatement(new StmtAssign(stmt, new ExprVar(stmt.getLHS(), base + "_" +
                        i), eai.getElements().get(i),
                              stmt.getOp()));
            }
            return null;
        }else{
            return super.visitStmtAssign(stmt);
        }
        
    }


    public Object visitExprArrayRange(ExprArrayRange ear) {
        if (ear.getBase() instanceof ExprVar) {
            String base = ear.getBase().toString();
            if (fda.dumbArr.containsKey(base)) {
                return new ExprVar(ear, base + "_" + ear.getOffset());
            }
        }
        return super.visitExprArrayRange(ear);
    }

}
