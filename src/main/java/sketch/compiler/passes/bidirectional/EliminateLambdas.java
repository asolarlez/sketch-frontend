package sketch.compiler.passes.bidirectional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Function.FcnType;
import sketch.compiler.ast.core.Package;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.SymbolTable;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.ExprLambda;
import sketch.compiler.ast.core.exprs.ExprUnary;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.StmtBlock;
import sketch.compiler.ast.core.stmts.StmtFunDecl;
import sketch.compiler.ast.core.stmts.StmtReturn;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.NotYetComputedType;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeFunction;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.passes.lowering.SymbolTableVisitor;
import sketch.util.exceptions.ExceptionAtNode;

public class EliminateLambdas extends BidirectionalPass {
    Type nyd = new NotYetComputedType();
    boolean needsCleanup = false;

    public class CleanupTypes extends SymbolTableVisitor {
        Map<String, List<Type>> funToParamTypes = new HashMap<String, List<Type>>();
        CleanupTypes() {
            super(null);
        }

        public Object visitExprFunCall(ExprFunCall efc) {
            if (funToParamTypes.containsKey(efc.getName())) {
                List<Type> lt = funToParamTypes.get(efc.getName());
                Iterator<Type> typeit = lt.iterator();
                int idx = 0;
                for (Expression actual : efc.getParams()) {
                    Type current = typeit.next();
                    lt.set(idx, current.leastCommonPromotion(getType(actual), nres));
                    ++idx;
                }
            }
            return super.visitExprFunCall(efc);
        }

        public Object visitProgram(Program p){
            if (!needsCleanup) {
                return p;
            }
            for (Package pkg : p.getPackages()) {
                for (Function f : pkg.getFuncs()) {
                    if (f.getParams().size() > 0 && f.getParams().get(0).getType().equals(nyd)) {
                        List<Type> newList = new ArrayList<Type>(f.getParams().size());
                        for (Parameter param : f.getParams()) {
                            newList.add(TypePrimitive.bottomtype);
                        }
                        funToParamTypes.put(f.getName(), newList);
                    }
                }
            }

            Program rv = (Program) super.visitProgram(p);
            
            FEReplacer fer = new SymbolTableVisitor(null) {
                Type rettype;

                public Object visitStmtReturn(StmtReturn sr) {
                    Type rt = getType(sr.getValue());
                    rettype = rt;
                    return sr;
                }

                public Object visitFunction(Function f){
                    
                    if(funToParamTypes.containsKey(f.getName())){
                        List<Type> lt = funToParamTypes.get(f.getName());
                        Iterator<Type> typeit = lt.iterator();
                        List<Parameter> plist = new ArrayList<Parameter>(f.getParams().size());
                        SymbolTable oldSymTab = symtab;
                        symtab = new SymbolTable(symtab);
                        for(Parameter par : f.getParams()){
                            Type t = typeit.next();
                            Parameter newpar = new Parameter(par, t, par.getName(), par.getPtype(), false);
                            newpar.accept(this);
                            plist.add(newpar);
                        }
                        f.getBody().accept(this);
                        symtab = oldSymTab;
                        return f.creator().params(plist).returnType(rettype).create();
                    }else{
                        return f;
                    }
                }                                
            };
            
            return rv.accept(fer);
        }

    }

    public FEReplacer getCleanup() {
        return new CleanupTypes();
    }

    private void fleshLambda(String freshVarName, ExprLambda elambda) {

        checkLambda(elambda);


        List<String> tparams = new ArrayList<String>();
        List<Parameter> lparam = new ArrayList<Parameter>();

        SymbolTable st = new SymbolTable(symtab());
        SymbolTableVisitor stv = new SymbolTableVisitor(st);





        for(ExprVar ev : elambda.getParameters()){
            String tname = driver.getVarGen().nextVar("T_");
            Parameter p = new Parameter(ev, nyd, ev.getName(), Parameter.IN, false);
            p.accept(stv);
            lparam.add(p);
            tparams.add(tname);
            needsCleanup = true;
        }

        Type rettype = stv.getType(elambda.getExpression());
        Function nf = Function.creator(elambda, freshVarName, FcnType.Generator).returnType(rettype)// .typeParams(tparams)
                .params(lparam)
                .body(new StmtBlock(new StmtReturn(elambda.getExpression(), elambda.getExpression()))).create();

        addStatement(new StmtFunDecl(elambda, nf));

    }

    public Object visitExprLambda(ExprLambda elambda) {

        String freshVarName = driver.getVarGen().nextVar("lam");
        fleshLambda(freshVarName, elambda);
        return new ExprVar(elambda, freshVarName);
    }

    public Object visitStmtVarDecl(StmtVarDecl stmt) {

        List<Expression> newInits = new ArrayList<Expression>();
        List<Type> newTypes = new ArrayList<Type>();
        boolean changed = false;
        for (int i = 0; i < stmt.getNumVars(); i++) {
            Type ot = stmt.getType(i);
            Expression oinit = stmt.getInit(i);
            if (ot instanceof TypeFunction) {
                changed = true;
                if (oinit == null) {
                    throw new ExceptionAtNode("Local fun variables must be initialized right away because they are final", stmt);
                }
                fleshLambda(stmt.getName(i), (ExprLambda) oinit);
            } else {
                newInits.add(oinit);
                newTypes.add(ot);
            }
        }
        if (!changed) {
            return stmt;
        }
        if (newInits.size() == 0) {
            return null;
        }
        return new StmtVarDecl(stmt, newTypes, stmt.getNames(), newInits);

    }

    public void checkLambda(final ExprLambda elambda) {

        FEReplacer fer = new FEReplacer() {
            /**
             * Visit an exprUnary an check if its part of a lambda expression.
             * If it is, check that it is not modifying a parameter of a lambda.
             */
            public Object visitExprUnary(ExprUnary exprUnary) {

                // Loop through the formal parameters of the lambda expression
                for (ExprVar formalParameter : elambda.getParameters()) {
                    // If the unary expression has a formal parameter
                    if (formalParameter.equals(exprUnary.getExpr())) {
                        // Thrown exception since we cannot modify a formal
                        // parameter of
                        // a lambda expression
                        throw new ExceptionAtNode("You cannot have an unary expression of " + "a formal parameter in a lambda", exprUnary);
                    }
                }

                return super.visitExprUnary(exprUnary);

            }

        };
        elambda.accept(fer);

    }

}
