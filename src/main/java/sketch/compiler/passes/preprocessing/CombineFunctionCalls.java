package sketch.compiler.passes.preprocessing;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.NameResolver;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.SymbolTable;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.ExprArrayRange;
import sketch.compiler.ast.core.exprs.ExprArrayRange.RangeLen;
import sketch.compiler.ast.core.exprs.ExprBinary;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.ExprTernary;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.*;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.passes.lowering.FlattenStmtBlocks;
import sketch.compiler.passes.lowering.SymbolTableVisitor;

public class CombineFunctionCalls extends SymbolTableVisitor {
    private TempVarGen varGen;

    public CombineFunctionCalls(TempVarGen varGen_) {
        super(null);
        varGen = varGen_;
    }

    @Override
    public Object visitFunction(Function fn) {
        List<Parameter> newParam = new ArrayList<Parameter>();
        Iterator<Parameter> it = fn.getParams().iterator();
        boolean samePars = true;
        while (it.hasNext()) {
            Parameter par = it.next();
            Parameter newPar = (Parameter) par.accept(this);
            if (par != newPar)
                samePars = false;
            newParam.add(newPar);
        }

        Type rtype = (Type) fn.getReturnType().accept(this);

        Statement newBody =
                (fn.getBody() != null) ? (Statement) fn.getBody().accept(this) : null;
        if (newBody == null)
            return fn;
        BreakIfThenStatements bif =
                new BreakIfThenStatements(nres, varGen, fn.getName(), symtab);

        newBody = (Statement) fn.getBody().accept(bif);
        if (newBody != fn.getBody()) {
            return fn.creator().body(newBody).create();
        }else{
            return fn;
        }
    }

    private class BreakIfThenStatements extends SymbolTableVisitor {
        TempVarGen varGen;
        String funName;

        // SymbolTable symtab;

        public BreakIfThenStatements(NameResolver nr, TempVarGen vg, String name,
                SymbolTable st)
        {
            super(st);
            nres = nr;
            varGen = vg;
            funName = name;
            // symtab =st;
        }

        private class GlobalizeVar extends SymbolTableVisitor {
            List stmts;

            public GlobalizeVar(SymbolTable symtab) {
                super(symtab);
                stmts = new ArrayList();

            }

            @Override
            public Object visitStmtVarDecl(StmtVarDecl stmt) {
                List inits = new ArrayList();
                for (int i = 0; i < stmt.getNames().size(); i++) {
                    inits.add(null);
                }
                stmts.add(new StmtVarDecl(stmt.getContext(), stmt.getTypes(),
                        stmt.getNames(), inits));
                List<Statement> newStmts = new ArrayList<Statement>();
                for (int i = 0; i < stmt.getNames().size(); i++) {
                    if (stmt.getInits().get(i) != null) {
                        newStmts.add(new StmtAssign(new ExprVar(stmt.getContext(),
                                stmt.getNames().get(i)),
                                (Expression) stmt.getInits().get(i)));
                    }
                }
                return new StmtBlock(newStmts);
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

            public CollectFunCalls(String name, SymbolTable symtab) {
                super(symtab);
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
                    CollectFunCalls c = new CollectFunCalls(funName, symtab);
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
                List statements = new ArrayList();
                CollectFunCalls c = new CollectFunCalls(funName, symtab);
                stmt.doStatement(c);
                ExprFunCall function = c.getFunction();
                if (function == null) {
                    statements.add(stmt);
                } else {
                    blocks.add(new StmtBlock(statements));
                    funCallStatements.add(stmt);
                    funCalls.add(function);
                    statements = new ArrayList();
                }
            }
            return new List[] { blocks, funCallStatements, funCalls };

        }


        @Override
        public Object visitStmtIfThen(StmtIfThen stmt) {

            List Statements = new ArrayList();
            String condVar = varGen.nextVar("");
            Statements.add(new StmtVarDecl(stmt.getContext(), TypePrimitive.bittype,
                    condVar, stmt.getCond()));
            List consBlocks = new ArrayList(), consFunCallStatements = new ArrayList(), consFunCalls =
                    new ArrayList();
            if (stmt.getCons() != null) {
                Statement cons = (Statement) stmt.getCons().accept(this);
                GlobalizeVar g = new GlobalizeVar(symtab);
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
                GlobalizeVar g = new GlobalizeVar(symtab);
                alt = (Statement) alt.accept(g);
                Statements.addAll(g.varDeclStmts());
                List[] altParts = divideBlock(alt);
                altBlocks = altParts[0];
                altFunCallStatements = altParts[1];
                altFunCalls = altParts[2];
            }


            List newCons = new ArrayList();
            List newAlt = new ArrayList();

            Statement prevConFunStatement = null;
            Statement prevAltFunStatement = null;
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

                    if (altFunCallStatements.get(i).getClass() == StmtIfThen.class) {
                        alt = 2;
                    } else {
                        alt = 1;
                    }
                    newAlt.add(altBlocks.get(i));

                } else
                    alt = 0;


                List<String> paramVars = new ArrayList<String>();
                List<Type> paramTypes = new ArrayList<Type>();

                Statement finalFunCall = null;
                prevConFunStatement = null;
                prevAltFunStatement = null;
                List prevConStmts = new ArrayList();
                List prevAltStmts = new ArrayList();

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

                List<Parameter> params = nres.getFun(funName).getParams();
                List<Expression> paramExprs = new ArrayList<Expression>();
                for (int l = 0; l < params.size(); l++) {
                    Parameter pm = params.get(l);
                    // generate new variable
                    String newParamVar = varGen.nextVar(pm.getName());
                    paramVars.add(newParamVar);
                    if (pm.getType().isArray()) {
                        TypeArray type = (TypeArray) pm.getType();
                        List dims = new ArrayList();
                        for (int _i = 0; _i < type.getDimensions().size(); _i++) {
                            Expression dim = type.getDimension(_i);
                            if (dim.isConstant()) {
                                dims.add(dim);
                            } else {
                               if(con>0 && alt >0){
                                    Expression le =
                                            ((TypeArray) getType(conParamExps.get(l))).getDimension(_i);

                                    Expression ri =
                                            ((TypeArray) getType(altParamExps.get(l))).getDimension(_i);
                                    Expression cond =
                                            new ExprBinary(ExprBinary.BINOP_GE, le, ri);
                                    dims.add(new ExprTernary("?:", cond, le, ri));
                               }else{
                                   dims.add(dim);
                               }
                            }

                        }

                        Type t = type.getAbsoluteBase();

                        while (!dims.isEmpty()) {
                            t = new TypeArray(t, (Expression) dims.remove(0));

                        }
                        paramTypes.add(t);

                    } else {
                        paramTypes.add(pm.getType());
                    }

                    ExprVar left = new ExprVar(stmt.getContext(), newParamVar);

                    if (pm.getType().isArray()) {
                        TypeArray type = (TypeArray) pm.getType();
                        List<RangeLen> dims = new ArrayList<RangeLen>();
                        for (int _i = 0; _i < type.getDimensions().size(); _i++) {
                            Expression dim = type.getDimension(_i);
                            if (dim.isConstant()) {
                                dims.add(new RangeLen(ExprConstInt.zero, dim));
                            } else {
                               for (int _j = 0; _j < params.size();_j++){
                                   //change this
                                   if(params.get(_j).getName() == ((ExprVar)dim).getName()){
                                        dims.add(new RangeLen(ExprConstInt.zero,
                                                new ExprVar(pm.getContext(),
                                                        paramVars.get(_j))));
                                       break;
                                   }
                                           
                               }
                               
                            }

                        }

                        Expression t = left;

                        t = new ExprArrayRange(pm, t, dims, false);


                        paramExprs.add(t);
                        if (pm.getPtype() > 0) {
                            if (con > 0) {
                            if (((Expression) conParamExps.get(l)).isLValue()) {
                                prevConStmts.add(new StmtAssign(conParamExps.get(l), t));
                            }
                            }
                            if (alt > 0) {
                            if (altParamExps.get(l).isLValue()) {
                                prevAltStmts.add(new StmtAssign(altParamExps.get(l), t));
                            }
                            }

                        }

                    }else{
                        if (pm.getPtype() > 0) {

                            if (con > 0) {
                                if (((Expression) conParamExps.get(l)).isLValue()) {
                                    prevConStmts.add(new StmtAssign(conParamExps.get(l),
                                            new ExprVar(pm.getContext(), newParamVar)));
                                }
                            }
                            if (alt > 0) {
                                if (altParamExps.get(l).isLValue()) {
                                    prevAltStmts.add(new StmtAssign(altParamExps.get(l),
                                            new ExprVar(pm.getContext(), newParamVar)));
                                }
                            }
                        }
                        paramExprs.add(left);
                    }

                    if (con > 0)
                            newCons.add(new StmtAssign(left, conParamExps.get(l)));
                    if (alt > 0)
                            newAlt.add(new StmtAssign(left, altParamExps.get(l)));



                }

                prevConFunStatement = new StmtBlock(prevConStmts);
                prevAltFunStatement = new StmtBlock(prevAltStmts);

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
                        newAlt.add(new StmtAssign(left, ExprConstInt.one));
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
                for (int a = 0; a < paramTypes.size(); a++) {
                    Statements.add(new StmtVarDecl(stmt.getContext(), paramTypes.get(a),
                            paramVars.get(a), null));
                }

                StmtIfThen s =
                        new StmtIfThen(stmt.getContext(), new ExprVar(stmt.getContext(),
                                condVar), new StmtBlock(newCons), new StmtBlock(newAlt));
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
                    new StmtIfThen(stmt.getContext(), new ExprVar(stmt.getContext(),
                            condVar), new StmtBlock(
                            newCons), new StmtBlock(newAlt));
            Statements.add(s);
            return new StmtBlock(Statements);

        }
    }

}
