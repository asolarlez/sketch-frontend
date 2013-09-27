package sketch.compiler.passes.types;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.SymbolTable;
import sketch.compiler.ast.core.SymbolTable.Finality;
import sketch.compiler.ast.core.exprs.ExprArrayInit;
import sketch.compiler.ast.core.exprs.ExprArrayRange;
import sketch.compiler.ast.core.exprs.ExprBinary;
import sketch.compiler.ast.core.exprs.ExprField;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.ExprUnary;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtEmpty;
import sketch.compiler.ast.core.stmts.StmtExpr;
import sketch.compiler.ast.core.stmts.StmtFor;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.main.cmdline.SketchOptions;
import sketch.compiler.passes.lowering.SymbolTableVisitor;
import sketch.util.exceptions.TypeErrorException;
import sketch.util.exceptions.UnrecognizedVariableException;

public class CheckProperFinality extends SymbolTableVisitor {

    Map<String, Finality> fieldFinality = new HashMap<String, SymbolTable.Finality>();

    int verbosity;
    public Finality getFieldFinality(String struct, String field) {
        String fname = nres.getStructName(struct) + "." + field;
        if (fieldFinality.containsKey(fname)) {
            return fieldFinality.get(fname);
        } else {
            return Finality.UNKNOWN;
        }
    }

    public void setFieldFinality(String struct, String field, Finality f) {
        String fname = nres.getStructName(struct) + "." + field;
        fieldFinality.put(fname, f);
    }

    FEReplacer markAsFinal = new FEReplacer() {
        public Object visitExprVar(ExprVar ev) {
            Finality f = symtab.lookupFinality(ev.getName(), ev);
            if (f == Finality.UNKNOWN || f == Finality.FIRSTWRITE) {
                if (verbosity > 4) {
                    System.out.println(ev.getCx() + ": Making final " + ev);
                }
                symtab.setFinality(ev.getName(), Finality.FINAL, ev);
            }
            if (f == Finality.NOTFINAL) {
                throw new TypeErrorException("Using non-final variable " + ev +
                        " for an array size expression", ev);
            }
            return ev;
        }

        @Override
        public Object visitExprField(ExprField ef) {
            /*
             * We should visit the base. If you have int[x.f.g], all of x, x.f and x.f.g
             * should be final.
             */
            ef.getLeft().accept(this);
            Type tb = getType(ef.getLeft());
            String struct = tb.toString();
            Finality f = getFieldFinality(struct, ef.getName());
            if (f == Finality.UNKNOWN) {
                if (verbosity > 4) {
                    System.out.println(ef.getCx() + ": Making final " + ef);
                }
                setFieldFinality(struct, ef.getName(), Finality.FINAL);
            }
            if (f == Finality.NOTFINAL) {
                throw new TypeErrorException("Using final field " + ef +
                        " in the LHS of an assignment.", ef);
            }
            if (f == Finality.FIRSTWRITE) {
                assert false : "This is a bug";
            }

            return ef;
        }
    };

    FEReplacer markAsNoFinal = new FEReplacer() {
        @Override
        public Object visitExprArrayInit(ExprArrayInit init) {
            return init;
        }

        @Override
        public Object visitExprArrayRange(ExprArrayRange ar) {
            ar.getBase().accept(this);
            return ar;
        }

        @Override
        public Object visitExprField(ExprField ef) {
            /*
             * We shouldn't visit the base. If you have x.f.g = t, the only field that is
             * becoming non-final is g.
             */
            Type tb = getType(ef.getLeft());
            String struct = tb.toString();
            Finality f = getFieldFinality(struct, ef.getName());
            if (f == Finality.UNKNOWN) {
                setFieldFinality(struct, ef.getName(), Finality.NOTFINAL);
            }
            if (f == Finality.FINAL) {
                throw new TypeErrorException(": Using final field " + ef +
                        " in the LHS of an assignment.", ef);
            }
            if (f == Finality.FIRSTWRITE) {
                assert false : "This is a bug";
            }
            return ef;
        }

        public Object visitExprVar(ExprVar ev) {

            Finality f = symtab.lookupFinality(ev.getName(), ev);
            if (f == Finality.UNKNOWN) {
                symtab.setFinality(ev.getName(), Finality.FIRSTWRITE, ev);
            }
            if (f == Finality.FIRSTWRITE) {
                symtab.setFinality(ev.getName(), Finality.NOTFINAL, ev);
            }
            if (f == Finality.FINAL) {
                throw new TypeErrorException("Using final variable " + ev +
                        " for the lhs of an assignment", ev);
            }
            return ev;
        }
    };

    public CheckProperFinality() {
        super(null);
        verbosity = SketchOptions.getSingleton().debugOpts.verbosity;
    }

    @Override
    public Object visitTypeArray(TypeArray ta) {
        ta.accept(markAsFinal);
        return ta;
    }

    public Object visitExprUnary(ExprUnary exp) {
        int op = exp.getOp();
        if (op == ExprUnary.UNOP_POSTDEC || op == ExprUnary.UNOP_POSTINC ||
                op == ExprUnary.UNOP_PREDEC || op == ExprUnary.UNOP_PREINC)
        {
            exp.accept(markAsNoFinal);
        }
        return exp;
    }

    @Override
    public Object visitStmtAssign(StmtAssign sa) {
        sa.getLHS().accept(markAsNoFinal);
        return sa;
    }

    public Object visitStmtFor(StmtFor stmt) {
        SymbolTable oldSymTab = symtab;
        symtab = new SymbolTable(symtab);

        boolean isCanonical = false;

        Statement newInit = null;
        String ivname = null;
        if (stmt.getInit() != null) {
            newInit = (Statement) stmt.getInit().accept(this);
            if (newInit instanceof StmtVarDecl) {
                StmtVarDecl svd = (StmtVarDecl) newInit;
                if (svd.getNumVars() == 1 && svd.getType(0).equals(TypePrimitive.inttype))
                {
                    ivname = svd.getName(0);
                    isCanonical = true;
                }
            }
        }

        final String fivname = ivname;
        class HasOtherVars extends FEReplacer {
            public boolean hasVar = false;
            public boolean hasOtherVars = false;

            public Object visitExprVar(ExprVar ev) {
                if (ev.getName().equals(fivname)) {
                    hasVar = true;
                } else {
                    hasOtherVars = true;
                }
                return ev;
            }

            void reset() {
                hasVar = false;
                hasOtherVars = false;
            }
        }

        HasOtherVars hov = new HasOtherVars();

        boolean goodCond = false;
        Expression newCond = doExpression(stmt.getCond());

        if (ivname != null && newCond instanceof ExprBinary) {
            ExprBinary eb = (ExprBinary) newCond;
            if (eb.getOp() == ExprBinary.BINOP_LE || eb.getOp() == ExprBinary.BINOP_LT) {
                if (eb.getLeft().toString().equals(ivname)) {
                    eb.getRight().accept(hov);
                    if (!hov.hasVar) {
                        goodCond = true;
                    }
                }
            }
        }

        isCanonical = isCanonical && goodCond;

        Statement tmp = stmt.getBody();
        Statement newBody = StmtEmpty.EMPTY;
        if (tmp != null) {
            newBody = (Statement) tmp.accept(this);
        }

        /**
         * If the incr is the only thing that mutates the loop variable, it's ok to treat
         * it as final within the loop. Also, we can treat the loop as canonical assuming
         * it has the right form.
         */

        hov.reset();
        if (stmt.getIncr() != null) {
            stmt.getIncr().accept(hov);
            if (hov.hasOtherVars) {
                stmt.getIncr().accept(this);
                isCanonical = false;
            }
        }

        if (isCanonical && !stmt.isCanonical()) {
            if (stmt.getIncr() != null) {
                Finality f = symtab.lookupFinality(fivname, stmt.getCond());
                if (f != Finality.NOTFINAL) {
                    if (stmt.getIncr() instanceof StmtExpr) {
                        StmtExpr se = (StmtExpr) stmt.getIncr();
                        if (se.getExpression() instanceof ExprUnary) {
                            ExprUnary eu = (ExprUnary) se.getExpression();
                            if (eu.getExpr().toString().equals(ivname)) {
                                if (eu.getOp() == ExprUnary.UNOP_POSTINC) {
                                    stmt.makeCanonical();
                                }
                                if (eu.getOp() == ExprUnary.UNOP_PREINC) {
                                    stmt.makeCanonical();
                                }
                            }
                        }
                    }
                    if (stmt.getIncr().toString().equals(ivname + " = " + ivname + " + 1"))
                    {
                        stmt.makeCanonical();
                    }
                    if (stmt.getIncr().toString().equals(ivname + " = 1 + " + ivname)) {
                        stmt.makeCanonical();
                    }
                    if (stmt.getIncr().toString().equals(ivname + " += 1")) {
                        stmt.makeCanonical();
                    }
                }
            }
        }

        symtab = oldSymTab;

        return stmt;
    }

    @Override
    public Object visitExprFunCall(ExprFunCall exp) {
        Function fun;
        try {
            fun = nres.getFun(exp.getName(), exp);
        } catch (UnrecognizedVariableException e) {
            // FIXME -- restore error noise
            throw e;
            // throw new UnrecognizedVariableException(exp + ": Function name " +
            // e.getMessage() + " not found" );
        }
        // now we create a temp (or several?) to store the result
        List<Expression> existingArgs = exp.getParams();
        List<Parameter> params = fun.getParams();

        for (int i = 0; i < params.size(); i++) {
            Parameter p = params.get(i);
            if (p.isParameterOutput()) {
                existingArgs.get(i).accept(markAsNoFinal);
            }
        }
        return super.visitExprFunCall(exp);
    }
}
