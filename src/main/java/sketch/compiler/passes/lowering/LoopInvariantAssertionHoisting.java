package sketch.compiler.passes.lowering;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import sketch.compiler.ast.core.FENullVisitor;
import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.NameResolver;
import sketch.compiler.ast.core.Package;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.exprs.ExprArrayRange;
import sketch.compiler.ast.core.exprs.ExprArrayRange.RangeLen;
import sketch.compiler.ast.core.exprs.ExprBinary;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssert;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtBlock;
import sketch.compiler.ast.core.stmts.StmtFor;
import sketch.compiler.ast.core.stmts.StmtIfThen;
import sketch.compiler.ast.core.stmts.StmtReturn;
import sketch.compiler.ast.core.stmts.StmtWhile;

public class LoopInvariantAssertionHoisting extends FEReplacer {

    static class ContainsReturn extends RuntimeException {
        static final ContainsReturn cr = new ContainsReturn();
    }

    Map<String, CaptureInvariants> ics =
            new HashMap<String, LoopInvariantAssertionHoisting.CaptureInvariants>();

    class CaptureInvariants extends FEReplacer {
        Map<String, Set<Statement>> loopsForVar;
        Set<String> calls;
        List<Statement> cloops;
        String cfun;

        CaptureInvariants(NameResolver nres) {
            this.nres = nres;
        }

        public Object visitFunction(Function func) {
            cfun = func.getName();
            cloops = new ArrayList<Statement>();
            calls = new HashSet<String>();
            loopsForVar = new HashMap<String, Set<Statement>>();
            return super.visitFunction(func);
        }

        public Object visitStmtFor(StmtFor stmt) {
            cloops.add(stmt);
            Object o = super.visitStmtFor(stmt);
            cloops.remove(cloops.size() - 1);
            return o;
        }

        public Object visitStmtWhile(StmtWhile stmt) {
            cloops.add(stmt);
            Object o = super.visitStmtWhile(stmt);
            cloops.remove(cloops.size() - 1);
            return o;
        }

        void addLoops(String s) {
            Set<Statement> sset = null;
            if (loopsForVar.containsKey(s)) {
                sset = loopsForVar.get(s);
            } else {
                sset = new HashSet<Statement>();
                loopsForVar.put(s, sset);
            }
            sset.addAll(cloops);
        }

        public void registerAssign(Expression lhs) {
            while (true) {
                if (lhs instanceof ExprVar) {
                    addLoops(lhs.toString());
                    return;
                }
                if (lhs instanceof ExprArrayRange) {
                    lhs = ((ExprArrayRange) lhs).getBase();
                    continue;
                }
                assert false;
            }
        }

        public Object visitStmtAssign(StmtAssign stmt) {
            Expression lhs = stmt.getLHS();
            registerAssign(lhs);
            return stmt;
        }

        public Object visitExprFunCall(ExprFunCall efc) {
            Function f = nres.getFun(efc.getName());
            Iterator<Parameter> ip = f.getParams().iterator();
            for (Expression e : efc.getParams()) {
                Parameter p = ip.next();
                if (p.isParameterOutput()) {
                    registerAssign(e);
                }
            }
            calls.add(f.getName());
            return efc;
        }
    }

    public Object visitProgram(Program p) {
        nres = new NameResolver(p);
        for (Package pk : p.getPackages()) {
            for (Function f : pk.getFuncs()) {
                CaptureInvariants ci = new CaptureInvariants(nres);
                f.accept(ci);
                ics.put(f.getName(), ci);
            }
        }
        return super.visitProgram(p);
    }

    CaptureInvariants ic;

    public Object visitStmtReturn(StmtReturn r){
        if(inLoop()){
            throw ContainsReturn.cr;
        }
        return r;
    }

    public Object visitFunction(Function f) {
        ic = ics.get(f.getName());
        cloops = new ArrayList<Statement>();
        preLoop = new ArrayList<Statement>();
        return super.visitFunction(f);
    }

    List<Statement> cloops;
    List<Statement> preLoop;

    boolean variantPathCond = false;

    public Object visitStmtIfThen(StmtIfThen stmt) {
        // NOTE xzl: very important fix. Asserts with variant path condition inside loops
        // cannot be hoisted.
        boolean oldVariantPathCond = variantPathCond;
        try {
            if (stmt.getCond().accept(h) == null) {
                variantPathCond = true;
            }
            cloops.add(0, stmt);
            Object o;
            try {
                o = super.visitStmtIfThen(stmt);
            } finally {
                cloops.remove(0);
            }
            return o;
        } finally {
            variantPathCond = oldVariantPathCond;
        }
    }

    public Object visitStmtFor(StmtFor stmt) {
        boolean oldVariantPathCond = variantPathCond;
        try {
            variantPathCond = false;
            List<Statement> old = preLoop;
            try {
                preLoop = new ArrayList<Statement>();
                cloops.add(0, stmt);
                Object o = null;
                try {
                    o = super.visitStmtFor(stmt);
                } finally {
                    cloops.remove(0);
                }
                if (cloops.size() == 0) {
                    List<Statement> toadd = preLoop;
                    if (!toadd.isEmpty()) {
                        Statement s =
                                new StmtIfThen(stmt, stmt.getICond(),
                                        new StmtBlock(toadd), null);
                        addStatement(s);
                    }
                    preLoop = old;
                } else {
                    List<Statement> temp = preLoop;
                    preLoop = old;
                    List<Statement> toadd = new ArrayList<Statement>();
                    for (Statement s : temp) {
                        Statement ns = (Statement) s.accept(this);
                        if (ns != null) {
                            toadd.add(ns);
                        }
                    }
                    if (!toadd.isEmpty()) {
                        Statement s =
                                new StmtIfThen(stmt, stmt.getICond(),
                                        new StmtBlock(toadd), null);
                        addStatement(s);
                    }
                }
                return o;
            } catch (ContainsReturn cr) {
                preLoop = old;
                return stmt;
            }
        } finally {
            variantPathCond = oldVariantPathCond;
        }
    }

    boolean inLoop() {
        for (Statement s : cloops) {
            if (s instanceof StmtFor) {
                return true;
            }
        }
        return false;
    }

    public Object visitStmtAssert(StmtAssert sa) {
        if (variantPathCond || !inLoop()) {
            return sa;
        }
        Expression e = (Expression) sa.getCond().accept(h);
        if (e == null) {
            return sa;
        } else {
            preLoop.add(new StmtAssert(sa, e, sa.getMsg(), sa.isSuper()));
            return null;
        }
    }

    Hoister h = new Hoister();

    class Hoister extends FENullVisitor {

        boolean isInvariant(String s) {
            Set<Statement> varset = ic.loopsForVar.get(s);
            if (varset == null) {
                return true;
            }
            for (Statement st : cloops) {
                if (varset.contains(st)) {
                    return false;
                }
            }
            return true;
        }

        public Object visitExprVar(ExprVar ev) {
            if (isInvariant(ev.toString())) {
                return ev;
            } else {
                return null;
            }
        }

        public Object visitExprArrayRange(ExprArrayRange ear) {
            final Expression newBase = (Expression) (ear.getBase()).accept(this);

            RangeLen range = ear.getSelection();
            Expression newStart = (Expression) (range.start()).accept(this);
            if (range.getLenExpression() == null) {
                if (newBase != null && newStart != null) {
                    return new ExprArrayRange(ear, newBase, new RangeLen(newStart, null));
                }
                return null;
            } else {
                Expression len = (Expression) range.getLenExpression().accept(this);
                if (newBase != null && newStart != null && len != null) {
                    return new ExprArrayRange(ear, newBase, new RangeLen(newStart, len));
                }
                return null;
            }

        }

        public Object visitExprConstInt(ExprConstInt ec) {
            return ec;
        }

        Expression checkLoopIndVarMax(Expression e1) {
            if (e1 instanceof ExprVar) {
                for (Statement s : cloops) {
                    if (s instanceof StmtFor) {
                        StmtFor fl = (StmtFor) s;
                        if (fl.isCanonical()) {
                            if (fl.getIndVar().equals(e1.toString())) {
                                return fl.getRangeMax();
                            }
                        }
                    }
                    if (s instanceof StmtIfThen) {
                        return null;
                    }
                }
            }
            return null;
        }

        Expression checkLoopIndVarMin(Expression e1) {
            if (e1 instanceof ExprVar) {
                for (Statement s : cloops) {
                    if (s instanceof StmtFor) {
                        StmtFor fl = (StmtFor) s;
                        if (fl.isCanonical()) {
                            if (fl.getIndVar().equals(e1.toString())) {
                                return fl.getRangeMin();
                            }
                        }
                    }
                    if (s instanceof StmtIfThen) {
                        return null;
                    }
                }
            }
            return null;
        }

        public Object visitExprBinary(ExprBinary eb) {

            Expression el = (Expression) eb.getLeft().accept(this);
            Expression er = (Expression) eb.getRight().accept(this);
            if (el != null && er != null) {
                return new ExprBinary(eb.getOp(), el, er);
            }

            if (eb.getOp() == ExprBinary.BINOP_LT) {
                if (el == null && er != null) {
                    el = checkLoopIndVarMax(eb.getLeft());
                    if (el != null) {
                        return new ExprBinary(eb.getOp(), el, er);
                    }
                }
            }
            if (eb.getOp() == ExprBinary.BINOP_GE) {
                if (el == null && er != null) {
                    el = checkLoopIndVarMin(eb.getLeft());
                    if (el != null) {
                        return new ExprBinary(eb.getOp(), el, er);
                    }
                }
            }
            return null;
        }

    }

}
