package sketch.compiler.passes.lowering;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.SymbolTable;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.ExprArrayRange;
import sketch.compiler.ast.core.exprs.ExprArrayRange.RangeLen;
import sketch.compiler.ast.core.exprs.ExprBinary;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.ExprUnary;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssert;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtBlock;
import sketch.compiler.ast.core.stmts.StmtExpr;
import sketch.compiler.ast.core.stmts.StmtFor;
import sketch.compiler.ast.core.stmts.StmtIfThen;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.ast.core.typs.TypePrimitive;

public class MakeLoopsRecursive extends SymbolTableVisitor {
    private static final class ParameterizeCodeRegion extends FEReplacer {
        private final SymbolTable st;
        private final Map<String, Parameter> pmap;
        boolean isLHS;

        private ParameterizeCodeRegion(SymbolTable st, Map<String, Parameter> pmap) {
            this.st = st;
            this.pmap = pmap;
        }

        @Override
        public Object visitExprArrayRange(ExprArrayRange exp) {
            final Expression newBase = doExpression(exp.getBase());

            boolean tilhs = isLHS;
            isLHS = false;
            RangeLen range = exp.getSelection();
            Expression newStart = doExpression(range.start());
            Expression newLen = null;
            if (range.hasLen()) {
                newLen = doExpression(range.getLenExpression());
            }
            isLHS = tilhs;
            return exp;
        }

        @Override
        public Object visitStmtAssign(StmtAssign stmt) {
            boolean tilhs = isLHS;

            isLHS = true;
            doExpression(stmt.getLHS());
            isLHS = false;
            Expression newRHS = doExpression(stmt.getRHS());
            isLHS = tilhs;
            return stmt;
        }

        @Override
        public Object visitExprFunCall(ExprFunCall exp) {

            Function f = nres.getFun(exp.getName());
            Iterator<Parameter> pit = f.getParams().iterator();
            boolean tilhs = isLHS;
            for (Expression param : exp.getParams()) {
                Parameter formal = pit.next();
                if (formal.isParameterOutput()) {
                    isLHS = true;
                }
                doExpression(param);
                isLHS = tilhs;
            }
            return exp;
        }

        @Override
        public Object visitExprVar(ExprVar ev) {
            Type t = this.st.lookupVarNocheck(ev);
            if (t != null) {

                if (t instanceof TypeArray) {
                    TypeArray ta = (TypeArray) t;
                    t = new TypeArray(ta.getBase(), null);
                }

                if (this.pmap.containsKey(ev.getName())) {
                    if (isLHS) {
                        Parameter tp = this.pmap.get(ev.getName());
                        if (!tp.isParameterInput()) {
                            this.pmap.put(ev.getName(), new Parameter(ev, t,
                                    ev.getName(), Parameter.REF));
                        }
                    }
                } else {
                    int kind = Parameter.IN;
                    if (isLHS) {
                        kind = Parameter.REF;
                    }
                    this.pmap.put(ev.getName(), new Parameter(ev, t, ev.getName(), kind));
                }
            }
            return ev;
        }
    }

    TempVarGen vargen;
    private final int maxiters;

    public MakeLoopsRecursive(TempVarGen vargen, int maxiters) {
        super(null);
        this.vargen = vargen;
        this.maxiters = maxiters;
    }

    List<Parameter> extractParams(StmtFor stmt) {
        SymbolTable old = this.symtab;
        this.symtab = new SymbolTable(this.symtab);
        stmt.getInit().accept(this);
        final SymbolTable st = this.symtab;
        final Map<String, Parameter> pmap = new HashMap<String, Parameter>();
        List<Parameter> p = new LinkedList<Parameter>();
        FEReplacer fer = new ParameterizeCodeRegion(st, pmap);
        fer.setNres(nres);
        if (stmt.getCond() != null) {
            stmt.getCond().accept(fer);
        }
        if (stmt.getBody() != null) {
            stmt.getBody().accept(fer);
        }
        if (stmt.getIncr() != null) {
            stmt.getIncr().accept(fer);
        }
        for (Entry<String, Parameter> eit : pmap.entrySet()) {
            if (eit.getValue().getType() instanceof TypeArray) {
                p.add(eit.getValue());
            } else {
                p.add(0, eit.getValue());
            }
        }
        this.symtab = old;
        return p;
    }

    protected boolean checkFor(StmtFor stmt) {
        return true;
    }

    public Object visitStmtFor(StmtFor stmt){
        if(checkFor(stmt)){
            if(stmt.isCanonical()){
                String itname = vargen.nextVar("iter");
                String funName = vargen.nextVar("loop");
                List<Parameter> params = extractParams(stmt);
                params.add(0, new Parameter(stmt, TypePrimitive.inttype, itname));
                List<Statement> body = new ArrayList<Statement>();
                SymbolTable old = this.symtab;
                this.symtab = new SymbolTable(old);
                stmt.getInit().accept(this);
                body.add(new StmtIfThen(stmt, stmt.getCond(),
                        (Statement) stmt.getBody().accept(this), null));
                this.symtab = old;
                body.add(stmt.getIncr());
                List<Expression> actuals = new ArrayList<Expression>();
                List<Expression> firstCall = new ArrayList<Expression>();
                boolean first = true;
                for (Parameter p : params) {
                    if (first) {
                        first = false;
                        actuals.add(new ExprBinary(new ExprVar(stmt, p.getName()), "+",
                                ExprConstInt.one));
                        firstCall.add(ExprConstInt.one);
                    } else {
                        Expression e = new ExprVar(stmt, p.getName());
                        actuals.add(e);
                        firstCall.add(e);
                    }
                }
                ExprFunCall sfc = new ExprFunCall(stmt, funName, actuals);
                Statement call = new StmtExpr(sfc);
                body.add(new StmtIfThen(stmt, new ExprBinary(new ExprVar(stmt, itname),
 "<", new ExprConstInt(
                                stmt, maxiters)),
                        call,
                        new StmtAssert(
                                new ExprUnary("!", stmt.getCond()),
                                "The loop in" +
                                        stmt.getCx() +
                                        " was unrolled " +
                                        maxiters +
                                        " times but that was not enough. You can change the unroll amount with the --bnd-unroll-amnt flag.",
                                false)));
                StmtBlock sb = new StmtBlock(body);

                Function f =
                        Function.creator(stmt, funName, Function.FcnType.Static).body(sb).params(
                                params).pkg(nres.curPkg().getName()).create();
                nres.registerFun(f);
                newFuncs.add(f);
                StmtBlock rv =
                        new StmtBlock(stmt.getInit(), new StmtExpr(new ExprFunCall(stmt,
                                funName, firstCall)));
                return rv;
            } else {
                return super.visitStmtFor(stmt);
            }
        }else{
            return super.visitStmtFor(stmt);
        }                
    }

}
