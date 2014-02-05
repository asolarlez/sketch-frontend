package sketch.compiler.passes.preprocessing;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.NameResolver;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.ExprBinary;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.*;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.passes.lowering.FlattenStmtBlocks;
import sketch.compiler.passes.lowering.SymbolTableVisitor;

public class CombineFunctionCalls extends FEReplacer {
    private TempVarGen varGen;

    public CombineFunctionCalls(TempVarGen varGen_) {
        varGen = varGen_;
    }

    @Override
    public Object visitFunction(Function fn) {
        Statement newBody = (Statement) fn.getBody().accept(this);
        BreakIfThenStatements bif = new BreakIfThenStatements(nres, varGen, fn.getName());
        newBody = (Statement) fn.getBody().accept(bif);
        if (newBody != fn.getBody()) {
            return fn.creator().body(newBody).create();
        }else{
            return fn;
        }
    }

    private class BreakIfThenStatements extends FEReplacer {
        TempVarGen varGen;
        String funName;

        public BreakIfThenStatements(NameResolver nr, TempVarGen vg, String name) {
            nres = nr;
            varGen = vg;
            funName = name;
        }

        private class GlobalizeVar extends SymbolTableVisitor {
            List stmts;

            public GlobalizeVar() {
                super(null);
                stmts = new ArrayList();

            }

            @Override
            public Object visitStmtVarDecl(StmtVarDecl stmt) {
                stmts.add(stmt);
                return null;
            }

            @Override
            public Object visitStmtIfThen(StmtIfThen stmt) {
                Expression newCond = doExpression(stmt.getCond());
                Statement newCons =
                        stmt.getCons() == null ? null
                                : (Statement) stmt.getCons().accept(this);
                Statement newAlt =
                        stmt.getAlt() == null ? null : (Statement) stmt.getAlt().accept(
                                this);
                if (newCond == stmt.getCond() && newCons == stmt.getCons() &&
                        newAlt == stmt.getAlt())
                    return stmt;
                if (newCons == null && newAlt == null) {
                    return new StmtExpr(stmt, newCond);
                }
                stmt = new StmtIfThen(stmt, newCond, newCons, newAlt);
                stmt.isAtomic();
                return stmt;

            }

            public List varDeclStmts() {
                return stmts;
            }
        }

        private class CollectFunCalls extends SymbolTableVisitor {
            // private List functions;
            private ExprFunCall f;
            String funName;

            public CollectFunCalls(String name) {
                super(null);
                f = null;
                funName = name;

            }

            @Override
            public Object visitExprFunCall(ExprFunCall exp) {
                if (exp.getName().equals(funName))
                    f = exp;
                return exp;

            }

            @Override
            public Object visitStmtIfThen(StmtIfThen stmt) {
                if (stmt.isAtomic()) {
                    stmt.getCons().accept(this);
                }
                return stmt;
            }

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
            public Object visitStmtSwitch(StmtSwitch stmt) {
                return stmt;
            }

            @Override
            public Object visitFunction(Function f) {
                return f;
            }

            public ExprFunCall getFunction() {
                return f;
            }
        }



        private List[] divideBlock(Statement stmt) {
            FlattenStmtBlocks f = new FlattenStmtBlocks();
            stmt = (Statement) stmt.accept(f);
            List blocks = new ArrayList();
            List funCallStatements = new ArrayList();
            List funCalls = new ArrayList();
            if (stmt.isBlock()) {
                Iterator<Statement> itr = ((StmtBlock) stmt).getStmts().iterator();
                List statements = new ArrayList();
                while (itr.hasNext()) {
                    Statement s = itr.next();
                    CollectFunCalls c = new CollectFunCalls(funName);
                    s.doStatement(c);
                    ExprFunCall function = c.getFunction();
                    if (function == null) {
                        statements.add(s);
                    } else {
                        blocks.add(new StmtBlock(statements));
                        funCallStatements.add(s);
                        funCalls.add(function);
                        statements = new ArrayList();
                    }

                }
                blocks.add(new StmtBlock(statements));

            } else {
                // deal with single statements
            }
            return new List[] { blocks, funCallStatements, funCalls };

        }

        /*
         * private int[] maximumMatching(List list1, List list2) { int l1 = list1.size();
         * int l2 = list2.size(); int[] matching = new int[l1]; for (int i = 0; i < l1;
         * i++) { if (i < l2) { matching[i] = i; } else { matching[i] = -1; } } return
         * matching; }
         */
        @Override
        public Object visitStmtIfThen(StmtIfThen stmt) {

            List Statements = new ArrayList();

            List consBlocks = new ArrayList(), consFunCallStatements = new ArrayList(), consFunCalls =
                    new ArrayList();
            if (stmt.getCons() != null) {
                Statement cons = (Statement) stmt.getCons().accept(this);
                GlobalizeVar g = new GlobalizeVar();
                cons = (Statement) cons.accept(g);
                Statements.addAll(g.varDeclStmts());

                List[] consParts = divideBlock(cons);
                consBlocks = consParts[0];
                consFunCallStatements = consParts[1];
                consFunCalls = consParts[2];
            }
            List altBlocks = new ArrayList(), altFunCallStatements = new ArrayList(), altFunCalls =
                    new ArrayList();
            if (stmt.getAlt() != null) {
                Statement alt = (Statement) stmt.getAlt().accept(this);
                GlobalizeVar g = new GlobalizeVar();
                alt = (Statement) alt.accept(g);
                Statements.addAll(g.varDeclStmts());
                List[] altParts = divideBlock(alt);
                altBlocks = altParts[0];
                altFunCallStatements = altParts[1];
                altFunCalls = altParts[2];
            }

            // if (consFunCalls.isEmpty() || altFunCalls.isEmpty()) {
            // Statements.add(stmt);
            // return new StmtBlock(Statements);
            // }

            List newCons = new ArrayList();
            List newAlt = new ArrayList();
            // int[] matching = maximumMatching(consFunCalls, altFunCalls);
            // int prevAlt = -1;
            Statement prevConFunStatement = null;
            Statement prevAltFunStatement = null;
            // newCons.add(consBlocks.get(0));
            int conSize = consFunCalls.size();
            int altSize = altFunCalls.size();
            int max = Math.max(conSize, altSize);
            int con = 0, alt = 0;
            for (int i = 0; i < max; i++) {
                newCons = new ArrayList();
                if (prevConFunStatement != null) {
                    newCons.add(prevConFunStatement);
                    prevConFunStatement = null;
                }
                if (i < conSize) {

                    // newCons.add(consFunCallStatements.get(i));
                    if (consFunCallStatements.get(i).getClass() == StmtIfThen.class) {
                        con = 2;
                    } else {
                        con = 1;
                    }
                    newCons.add(consBlocks.get(i));
                } else
                    con = 0;
                newAlt = new ArrayList();
                if (prevAltFunStatement != null) {
                    newAlt.add(prevAltFunStatement);
                    prevAltFunStatement = null;
                }
                if (i < altSize) {

                    // int a = matching[i];

                    if (altFunCallStatements.get(i).getClass() == StmtIfThen.class) {
                        alt = 2;
                    } else {
                        alt = 1;
                    }
                    newAlt.add(altBlocks.get(i));
                    // for (int j = prevAlt + 1; j < a; j++) {
                    // newAlt.add(altFunCallStatements.get(j));
                    // newAlt.add(altBlocks.get(j + 1));
                    // }
                    // prevAlt = a;
                } else
                    alt = 0;

                // Map<String, List<String>> mapFunToName =
                // new HashMap<String, List<String>>();
                // Map<String, List<String>> mapFunToParams =
                // new HashMap<String, List<String>>();
                List<String> paramVars = new ArrayList<String>();
                List<Type> paramTypes = new ArrayList<Type>();
                List paramInits = new ArrayList();
                Statement finalFunCall = null;
                prevConFunStatement = null;
                prevAltFunStatement = null;

                ExprFunCall consFunction = null;
                List<Expression> conParamExps = null;
                if (con > 0) {
                    consFunction = (ExprFunCall) consFunCalls.get(i);
                    conParamExps = consFunction.getParams();
                }

                ExprFunCall altFunction = null;
                List<Expression> altParamExps = null;
                if (alt > 0) {
                    altFunction = (ExprFunCall) altFunCalls.get(i);
                    altParamExps = altFunction.getParams();
                }
                // String name = consFunction.getName();
                List<Parameter> params = nres.getFun(funName).getParams();
                List<Expression> paramExprs = new ArrayList<Expression>();
                for (int l = 0; l < params.size(); l++) {
                    Parameter pm = params.get(l);
                    // generate new variable
                    String newParamVar = varGen.nextVar(pm.getName());
                    paramVars.add(newParamVar);
                    paramTypes.add(pm.getType());
                    paramInits.add(null);
                    ExprVar left = new ExprVar(stmt.getContext(), newParamVar);


                    paramExprs.add(left);
                    if (l == params.size() - 1) {

                        prevConFunStatement =
                                (con > 0) ? new StmtAssign(conParamExps.get(l),
                                        new ExprVar(pm.getContext(), newParamVar)) : null;
                        prevAltFunStatement =
                                (alt > 0) ? new StmtAssign(altParamExps.get(l),
                                        new ExprVar(pm.getContext(), newParamVar)) : null;
                    } else {
                        if (con > 0)
                            newCons.add(new StmtAssign(left, conParamExps.get(l)));
                        if (alt > 0)
                            newAlt.add(new StmtAssign(left, altParamExps.get(l)));
                    }


                }

                if (con == 1 && alt == 1) {
                    finalFunCall =
                            new StmtExpr(new ExprFunCall(
                                    ((ExprFunCall) (consFunction)).getContext(), funName,
                                    paramExprs));
                } else if (con == 1 || alt == 1) {
                    String checkVar = varGen.nextVar("");
                    ExprVar left = new ExprVar(stmt.getContext(), checkVar);
                    Statements.add(new StmtVarDecl(stmt.getContext(),
                            TypePrimitive.bittype, checkVar, ExprConstInt.zero));
                    if (con == 1)
                        newCons.add(new StmtAssign(left, ExprConstInt.one));
                    if (alt == 1)
                        newAlt.add(new StmtAssign(left, ExprConstInt.zero));
                    Expression cond = ExprConstInt.zero;
                    if (con == 2) {
                        cond = ((StmtIfThen) consFunCallStatements.get(i)).getCond();
                    }
                    if (alt == 2) {
                        cond = ((StmtIfThen) altFunCallStatements.get(i)).getCond();
                    }
                    cond = new ExprBinary(ExprBinary.BINOP_OR, cond, left);
                    Statement f =
                            new StmtExpr(new ExprFunCall(stmt.getContext(), funName,
                                    paramExprs));
                    finalFunCall = new StmtIfThen(stmt.getContext(), cond, f, null);
                    ((StmtIfThen) finalFunCall).setAtomic();

                } else {
                    Expression cond1 = ExprConstInt.zero;
                    Expression cond2 = ExprConstInt.zero;
                    if (con == 2) {
                        cond1 = ((StmtIfThen) consFunCallStatements.get(i)).getCond();
                    }
                    if (alt == 2) {
                        cond2 = ((StmtIfThen) altFunCallStatements.get(i)).getCond();
                    }
                    cond1 = new ExprBinary(ExprBinary.BINOP_OR, cond1, cond2);
                    Statement f =
                            new StmtExpr(new ExprFunCall(stmt.getContext(), funName,
                                    paramExprs));
                    finalFunCall = new StmtIfThen(stmt.getContext(), cond1, f, null);
                    ((StmtIfThen) finalFunCall).setAtomic();

                }

                Statements.add(new StmtVarDecl(stmt.getContext(), paramTypes, paramVars,
                        paramInits));

                StmtIfThen s =
                        new StmtIfThen(stmt.getContext(), stmt.getCond(), new StmtBlock(
                                newCons), new StmtBlock(newAlt));
                Statements.add(s);
                Statements.add(finalFunCall);


                newCons = new ArrayList();


            }
            if (prevConFunStatement != null) {
                newCons.add(prevConFunStatement);
                prevConFunStatement = null;
            }
            if (consBlocks.size() > 0) {
            newCons.add(consBlocks.get(consBlocks.size() - 1));
            }
            newAlt = new ArrayList();
            if (prevAltFunStatement != null) {
                newAlt.add(prevAltFunStatement);
                prevAltFunStatement = null;
            }
            if (altBlocks.size() > 0)
            newAlt.add(altBlocks.get(altBlocks.size() - 1));
            StmtIfThen s =
                    new StmtIfThen(stmt.getContext(), stmt.getCond(), new StmtBlock(
                            newCons), new StmtBlock(newAlt));
            Statements.add(s);
            return new StmtBlock(Statements);

        }
    }

}
