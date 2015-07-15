package sketch.compiler.passes.preprocessing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import sketch.compiler.ast.core.FEReplacer;
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
import sketch.compiler.ast.core.exprs.ExprUnary;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.*;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.passes.lowering.FlattenStmtBlocks;
import sketch.compiler.passes.lowering.SymbolTableVisitor;

/**
 * Not used anymore Older version of combining function calls directly in the frontend.
 */
public class CombineFunctionCalls extends SymbolTableVisitor {
    private TempVarGen varGen;

    public CombineFunctionCalls(TempVarGen varGen_) {
        super(null);
        varGen = varGen_;
    }

    @Override
    public Object visitFunction(Function fn) {
        if (fn.hasAnnotation("DontCombine"))
            return fn;
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
                new BreakIfThenStatements(nres, varGen, fn.getName(), symtab, true);

        newBody = (Statement) fn.getBody().accept(bif);
        if (newBody != fn.getBody()) {
            return fn.creator().body(newBody).create();
        } else {
            return fn;
        }
    }

    private class BreakIfThenStatements extends SymbolTableVisitor {
        TempVarGen varGen;
        String funName;
        boolean isOuterAndOneBranch;
        Map<String, Type> typeMap = new HashMap<String, Type>();
        public BreakIfThenStatements(NameResolver nr, TempVarGen vg, String name,
                SymbolTable st, boolean isOuter)
        {
            super(st);
            nres = nr;
            varGen = vg;
            funName = name;
            this.isOuterAndOneBranch = isOuter;
        }

        private class ReplaceExpression extends FEReplacer {
            Map<String, Expression> trackVar;

            public ReplaceExpression(Map<String, Expression> trackMap)
            {
                trackVar = trackMap;

            }

            public Object visitExprVar(ExprVar exp) {
                if (trackVar.containsKey(exp.getName())) {
                    Expression newExp =
                            (Expression) trackVar.get(exp.getName()).accept(this);

                    return newExp;
                }
                return exp;
            }


        }
        private class GlobalizeVar extends SymbolTableVisitor {
            List stmts;
            Map<String, Expression> trackVar = new HashMap<String, Expression>();
            TempVarGen vg;
            Expression cond;

            public GlobalizeVar(SymbolTable symtab, TempVarGen vg, NameResolver nr,
                    Expression cond)
            {
                super(symtab);
                this.vg = vg;
                nres = nr;
                stmts = new ArrayList();
                this.cond = cond;

            }

            @Override
            public Object visitStmtVarDecl(StmtVarDecl stmt) {
                List types = new ArrayList();
                List inits = new ArrayList();
                List names = new ArrayList();


                List<Statement> newStmts = new ArrayList<Statement>();
                for (int i = 0; i < stmt.getNames().size(); i++) {
                    names.add(stmt.getName(i));
                    inits.add(null);
                    if (stmt.getType(i).isArray()) {
                        TypeArray t =(TypeArray) (stmt.getType(i));
                        List<Expression> newDims = new ArrayList<Expression>();
                        for(Expression dim: t.getDimensions()){
                            ReplaceExpression re = new ReplaceExpression(trackVar);
                            Expression newExp = (Expression) dim.accept(re);
                            if (newExp != dim) {
                                String newVar = varGen.nextVar();
                                stmts.add(new StmtVarDecl(dim.getContext(),
                                        getType(newExp), newVar, null));
                                // Is there any side effect because of this??
                                StmtIfThen conditionalAssign =
                                        new StmtIfThen(
                                                dim.getContext(),
                                                cond,
                                                new StmtAssign(new ExprVar(
                                                        dim.getContext(), newVar), newExp),
                                                null);
                                conditionalAssign.singleVarAssign();
                                stmts.add(conditionalAssign);
                                newDims.add(new ExprVar(dim.getContext(), newVar));

                            } else {
                                newDims.add(dim);
                            }

                        }
                        Type newType = t.getAbsoluteBase();

                        while (!newDims.isEmpty()) {
                            newType =
                                    new TypeArray(newType, (Expression) newDims.remove(0));
                        }
                        types.add(newType);

                    } else {
                        types.add(stmt.getType(i));
                    }
                    if (stmt.getInit(i) != null)
                        trackVar.put(stmt.getName(i), stmt.getInit(i));
                    if (stmt.getInits().get(i) != null) {
                        newStmts.add(new StmtAssign(new ExprVar(stmt.getContext(),
                                stmt.getName(i)), (Expression) stmt.getInit(i)));
                    }
                }
                stmts.add(new StmtVarDecl(stmt.getContext(), types, names, inits));
                return new StmtBlock(newStmts);
            }

            public Object visitStmtAssign(StmtAssign stmt) {
                stmt = (StmtAssign) super.visitStmtAssign(stmt);
                Expression lhs = stmt.getLHS();
                if (lhs instanceof ExprVar) {
                    String name = ((ExprVar) lhs).getName();
                    trackVar.put(name, stmt.getRHS());
                }
                return stmt;
            }

            @Override
            public Object visitStmtIfThen(StmtIfThen stmt) {
                if (stmt.isSingleVarAssign()) {
                    // TODO: should combine conditions
                    stmt.getCons().accept(this);
                }
                return stmt;
            }

            // Ignore declarations in these calls - because, they are anyways not broken
            // up.
            // TODO: Check if everything is covered.

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

            // Switch statements should not exist at this point.
            @Override
            public Object visitStmtSwitch(StmtSwitch stmt) {
                return stmt;
            }

            @Override
            public Object visitFunction(Function f) {
                return f;
            }

            public List varDeclStmts() {
                return stmts;
            }


        }

        /*
         * This class extracts the recursive funCall from a statement block.
         */
        private class GetFunCall extends SymbolTableVisitor {
            private ExprFunCall f;
            String funName;

            public GetFunCall(String name, SymbolTable symtab) {
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
                //What does atomic mean?
                if (stmt.isSingleFunCall()) {
                    stmt.getCons().accept(this);
                }
                return stmt;
            }
            //Ignore function calls in these statements because they are tricky to merge
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

            //Switch statements should not exist at this point.
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

        /*
         * Divides a statement block at recursive function call statements.
         */
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
                    GetFunCall c = new GetFunCall(funName, symtab);
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
                GetFunCall c = new GetFunCall(funName, symtab);
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

            // Extract the condition inorder to avoid re-executing the condition
            // multiple times.
            String condVar = varGen.nextVar("");
            Statements.add(new StmtVarDecl(stmt.getContext(), TypePrimitive.bittype,
                    condVar, stmt.getCond()));


            List consBlocks = new ArrayList(), consFunCallStatements = new ArrayList(), consFunCalls =
                    new ArrayList();

            if (stmt.getAlt() != null)
                isOuterAndOneBranch = false;
            if (stmt.getCons() != null) {
                // Recurse on cons
                Statement cons = (Statement) stmt.getCons().accept(this);

                // If all outer if-then statements have only one branch, then no need to
                // merge.
                if (isOuterAndOneBranch) {
                    return new StmtIfThen(stmt, stmt.getCond(), cons, null);
                }

                // Extract init stmts from cons
                Expression cond = new ExprVar(stmt.getContext(), condVar);
                GlobalizeVar g = new GlobalizeVar(symtab, varGen, nres, cond);
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
                // Recurse on alt
                Statement alt = (Statement) stmt.getAlt().accept(this);

                // Extract init statements from alt
                Expression cond =
                        new ExprUnary(stmt.getContext(), ExprUnary.UNOP_NOT, new ExprVar(
                                stmt.getContext(), condVar));
                GlobalizeVar g = new GlobalizeVar(symtab, varGen, nres, cond);
                alt = (Statement) alt.accept(g);
                Statements.addAll(g.varDeclStmts());

                List[] altParts = divideBlock(alt);
                altBlocks = altParts[0];
                altFunCallStatements = altParts[1];
                altFunCalls = altParts[2];
            }


            List newCons = new ArrayList();
            List newAlt = new ArrayList();
            Statement prevConStatements = null;
            Statement prevAltStatements = null;

            int conSize = consFunCalls.size();
            int altSize = altFunCalls.size();
            int max = Math.max(conSize, altSize);
            int con = 0, alt = 0;

            for (int i = 0; i < max; i++) {

                newCons = new ArrayList();
                if (prevConStatements != null) {
                    newCons.add(prevConStatements);
                    prevConStatements = null;
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
                if (prevAltStatements != null) {
                    newAlt.add(prevAltStatements);
                    prevAltStatements = null;
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


                StmtIfThen blocks =
                        new StmtIfThen(stmt.getContext(), new ExprVar(stmt.getContext(),
                                condVar), new StmtBlock(newCons), new StmtBlock(newAlt));
                Statements.add(blocks);

                newCons = new ArrayList();
                newAlt = new ArrayList();

                // Variables to hold parameters of function.
                List<String> paramVars = new ArrayList<String>();
                List<Type> paramTypes = new ArrayList<Type>();

                Statement finalFunCall = null;
                prevConStatements = null;
                prevAltStatements = null;

                List prevConStmtsList = new ArrayList();
                List prevAltStmtsList = new ArrayList();

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

                // find a better way for doing this
                for (Entry<String, Type> entry : typeMap.entrySet()) {
                    symtab.registerVar(entry.getKey(), entry.getValue());
                }

                List<Parameter> params = nres.getFun(funName).getParams();

                List<Expression> paramExprs = new ArrayList<Expression>();

                List<Statement> conParamStmts = new ArrayList<Statement>();
                List<Statement> altParamStmts = new ArrayList<Statement>();
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
                                if (con > 0 && alt > 0) {
                                    // Expression for maximum of two sizes
                                    Expression le =
                                            ((TypeArray) getType(conParamExps.get(l))).getDimension(_i);
                                    Expression ri =
                                            ((TypeArray) getType(altParamExps.get(l))).getDimension(_i);
                                    Expression cond =
                                            new ExprBinary(ExprBinary.BINOP_GE, le, ri);
                                    dims.add(new ExprTernary("?:", cond, le, ri));
                                } else if (con > 0) {
                                    dims.add(((TypeArray) getType(conParamExps.get(l))).getDimension(_i));
                                } else {
                                    dims.add(((TypeArray) getType(altParamExps.get(l))).getDimension(_i));
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
                                for (int _j = 0; _j < params.size(); _j++) {
                                    // change this
                                    if (params.get(_j).getName() == ((ExprVar) dim).getName())
                                    {
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
                            // ref or output param

                            if (con > 0) {
                                if (((Expression) conParamExps.get(l)).isLValue()) {
                                    prevConStmtsList.add(new StmtAssign(
                                            conParamExps.get(l),
                                            t));
                                }
                            }
                            if (alt > 0) {
                                if (altParamExps.get(l).isLValue()) {
                                    prevAltStmtsList.add(new StmtAssign(
                                            altParamExps.get(l),
                                            t));
                                }
                            }

                        }

                    }else{
                        if (pm.getPtype() > 0) {

                            if (con > 0) {
                                if (((Expression) conParamExps.get(l)).isLValue()) {
                                    prevConStmtsList.add(new StmtAssign(
                                            conParamExps.get(l),
                                            new ExprVar(pm.getContext(), newParamVar)));
                                }
                            }
                            if (alt > 0) {
                                if (altParamExps.get(l).isLValue()) {
                                    prevAltStmtsList.add(new StmtAssign(
                                            altParamExps.get(l),
                                            new ExprVar(pm.getContext(), newParamVar)));
                                }
                            }
                        }
                        paramExprs.add(left);
                    }
                    if (con > 0)
                        conParamStmts.add(new StmtAssign(left, conParamExps.get(l)));
                    if (alt > 0)
                        altParamStmts.add(new StmtAssign(left, altParamExps.get(l)));


                }
                if(con ==1){
                    newCons.add(new StmtBlock(conParamStmts));
                }
                if(con ==2){
                    newCons.add(new StmtIfThen(stmt,
                            ((StmtIfThen) consFunCallStatements.get(i)).getCond(),
                            new StmtBlock(conParamStmts), null));
                }
                if(alt ==1){
                    newAlt.add(new StmtBlock(altParamStmts));
                }
                if(alt ==2){
                    newAlt.add(new StmtIfThen(stmt,
                            ((StmtIfThen) altFunCallStatements.get(i)).getCond(),
                            new StmtBlock(altParamStmts), null));
                }

                prevConStatements = new StmtBlock(prevConStmtsList);
                prevAltStatements = new StmtBlock(prevAltStmtsList);

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
                    ((StmtIfThen) finalFunCall).singleFunCall();

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
                    ((StmtIfThen) finalFunCall).singleFunCall();

                }

                for (int a = 0; a < paramTypes.size(); a++) {
                    StmtVarDecl st = new StmtVarDecl(stmt.getContext(), paramTypes.get(a),
                            paramVars.get(a), null);
                    Statements.add(st);
                    typeMap.put(paramVars.get(a), paramTypes.get(a));
                    symtab.registerVar(paramVars.get(a), paramTypes.get(a));
                }

                StmtIfThen s =
                        new StmtIfThen(stmt.getContext(), new ExprVar(stmt.getContext(),
                                condVar), new StmtBlock(newCons), new StmtBlock(newAlt));
                Statements.add(s);
                Statements.add(finalFunCall);


                newCons = new ArrayList();
            }

            if (prevConStatements != null) {
                newCons.add(prevConStatements);
                prevConStatements = null;
            }
            if (consBlocks.size() > 0) {
                newCons.add(consBlocks.get(consBlocks.size() - 1));
            }
            newAlt = new ArrayList();
            if (prevAltStatements != null) {
                newAlt.add(prevAltStatements);
                prevAltStatements = null;
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
