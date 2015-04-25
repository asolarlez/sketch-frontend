package sketch.compiler.passes.lowering;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.NameResolver;
import sketch.compiler.ast.core.Package;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.SymbolTable;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.*;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssert;
import sketch.compiler.ast.core.stmts.StmtBlock;
import sketch.compiler.ast.core.stmts.StmtSpAssert;
import sketch.compiler.ast.core.typs.StructDef;
import sketch.compiler.ast.core.typs.TypeStructRef;


public class EliminateImmutableStructs extends SymbolTableVisitor {
    private HashMap<String, StructTracker> structs;
    private TempVarGen varGen;

    public EliminateImmutableStructs() {
        super(null);
        structs = new HashMap<String, StructTracker>();
    }


    /**
     * Null is unaffected.
     */
    @Override
    public Object visitExprNullPtr(ExprNullPtr nptr) {
        return nptr;
    }

    Map<String, Set<String>> usedStructNames;
    Map<String, Set<String>> createdStructNames;
    Map<String, Set<String>> modifiedStructNames;

    /**
     * Add variable declarations to the body of 'func', and rewrite its body.
     */
    public Object visitFunction(Function func) {
        boolean isMain = false;
        String funName = nres.getFunName(func.getName());
        structs.clear();
        if (mainFunctions.contains(funName)) {

            isMain = true;
            SymbolTable oldSymTab = symtab;
            symtab = new SymbolTable(symtab);

            List<Parameter> newParams = new ArrayList<Parameter>();
            for (Iterator iter = func.getParams().iterator(); iter.hasNext();) {
                Parameter param = (Parameter) iter.next();
                symtab.registerVar(param.getName(), (param.getType()), param,
                        SymbolTable.KIND_FUNC_PARAM);
                newParams.add((Parameter) param.accept(this));
            }

            List<Statement> newBodyStmts = new ArrayList<Statement>();
            Set<String> createdSN = createdStructNames.get(funName);
            for (String name : usedStructNames.get(funName)) {

                StructTracker tracker = new StructTracker(nres.getStruct(name));
                tracker.mapVariables();
                structs.put(name, tracker);
            }

            Function func2 = (Function) super.visitFunction(func);
            symtab = oldSymTab;

            if (func.isUninterp()) {
                return func2;
            }

            Statement oldBody = func2.getBody();
            newBodyStmts.add(oldBody);
            StmtBlock newBody = new StmtBlock(oldBody, newBodyStmts);

            newFuncs.add(func2.creator().params(newParams).body(newBody).create());
        }

        if (calledFunctions.contains(funName)) {
            SymbolTable oldSymTab = symtab;
            symtab = new SymbolTable(symtab);
            List<Parameter> newParams = new ArrayList<Parameter>();
            Set<String> createdSN = createdStructNames.get(funName);
            List<Statement> newBodyStmts = new LinkedList<Statement>();

            Set<String> modified = modifiedStructNames.get(funName);

            for (String name : usedStructNames.get(funName)) {
                StructTracker tracker = new StructTracker(nres.getStruct(name));
                tracker.mapVariables();
                structs.put(name, tracker);
            }

            for (Iterator iter = func.getParams().iterator(); iter.hasNext();) {
                Parameter param = (Parameter) iter.next();
                symtab.registerVar(param.getName(), (param.getType()), param,
                        SymbolTable.KIND_FUNC_PARAM);
                newParams.add((Parameter) param.accept(this));
            }

            Function func2 = (Function) super.visitFunction(func);
            symtab = oldSymTab;

            if (func.isUninterp()) {
                return func2;
            }

            Statement oldBody = func2.getBody();
            newBodyStmts.add(oldBody);
            StmtBlock newBody = new StmtBlock(oldBody, newBodyStmts);

            String newName = func.getName();

            if (isMain) {
                newName = newName + "_2";
            }

            newFuncs.add(func2.creator().name(newName).params(newParams).body(newBody).create());
        }

        return null;
    }



    /**
     * Rewrite field accesses of the form '((Foo)foo).bar' into 'Foo_bar[foo]'.
     */
    public Object visitExprField(ExprField ef) {

        StructDef t = this.getStructDef(getType(ef.getLeft()));
        if (!t.isStruct()) {
            ef.report("Trying to read field of non-struct variable.");
            throw new RuntimeException("reading field of non-struct");
        }
        if (t.immutable()) {

        StructTracker struct = structs.get(nres.getStructName(((StructDef) t).getName()));
        String field = ef.getName();
        int index = struct.getIndex(field);

        Expression basePtr = (Expression) ef.getLeft().accept(this);
            if (newStatements != null) {
                addStatement(new StmtAssert(new ExprBinary(basePtr, "!=",
                        ExprConstInt.minusone), false));
            }
        return new ExprTupleAccess(ef, basePtr, index);
        } else
            return ef;

    }

    /**
     * Rewrite the 'new' expression into a guarded pointer increment.
     */
    public Object visitExprNew(ExprNew expNew) {
        expNew.assertTrue(expNew.getTypeToConstruct().isStruct(),
                "Sorry, only structs are supported in 'new' statements.");

        String name = ((TypeStructRef) expNew.getTypeToConstruct()).getName();
        StructTracker struct = structs.get(nres.getStructName(name));
        if (!struct.struct.immutable())
            return expNew;
        Map<String, Expression> fieldExprs = new HashMap<String, Expression>();
        List<Expression> exprs = new ArrayList<Expression>();

        for (ExprNamedParam en : expNew.getParams()) {
            Expression tt = (Expression) en.getExpr().accept(this);
            fieldExprs.put(en.getName(), tt);
        }

        StructDef str = struct.struct;
        for(String field: str.getOrderedFields()){
            if (fieldExprs.containsKey(field))
                exprs.add(fieldExprs.get(field));
            else {

                exprs.add(str.getType(field).defaultValue());
            }
        }

        // this.addStatement (struct.makeAllocationGuard (expNew));
        return new ExprTuple(expNew, exprs, str.getName() + "_" + str.getPkg());
        
    }

    public Object visitStructDef(StructDef ts) {
        // return TypePrimitive.inttype;
        return ts;
    }

    @Override
    public Object visitTypeStructRef(TypeStructRef t) {
        // return TypePrimitive.inttype ;
        return t;
    }

    final Set<String> calledFunctions = new HashSet<String>();
    final Set<String> mainFunctions = new HashSet<String>();

    public Object visitProgram(Program p) {
        calledFunctions.clear();
        nres = new NameResolver(p);
        final NameResolver lnres = nres;
        GetUsedStructs gus = new GetUsedStructs(nres);
        gus.visitProgram(p);
        usedStructNames = gus.get();
        createdStructNames = gus.getCreated();
        modifiedStructNames = gus.getModified();

        for (Package pkg : p.getPackages()) {
            nres.setPackage(pkg);
            for (Function func : pkg.getFuncs()) {
                if (func.getSpecification() != null) {
                    mainFunctions.add(nres.getFunName(func.getName()));
                    mainFunctions.add(nres.getFunName(func.getSpecification()));
                }

                func.accept(new FEReplacer() {
                    @Override
                    public Object visitExprFunCall(ExprFunCall exp) {
                        calledFunctions.add(lnres.getFunName(exp.getName()));
                        return exp;
                    }
                });
            }
            for (StmtSpAssert sa : pkg.getSpAsserts()) {
                ExprFunCall f1 = sa.getFirstFun();
                calledFunctions.add(nres.getFunName(f1.getName()));
                for (Expression param : f1.getParams()) {
                    if (param instanceof ExprFunCall) {
                        ExprFunCall pa = (ExprFunCall) param;
                        calledFunctions.add(nres.getFunName(pa.getName()));
                    }
                }

                ExprFunCall f2 = sa.getSecondFun();
                calledFunctions.add(nres.getFunName(f2.getName()));
                for (Expression param : f2.getParams()) {
                    if (param instanceof ExprFunCall) {
                        ExprFunCall pa = (ExprFunCall) param;
                        calledFunctions.add(nres.getFunName(pa.getName()));
                    }
                }
            }
        }
        return super.visitProgram(p);
    }




    private class StructTracker {
        private StructDef struct;
        private Map<String, Integer> varMapping = new HashMap<String, Integer>();
        public StructTracker(StructDef struct_)
        {
            struct = struct_;
        }

        public void mapVariable(String var, int index) {
            varMapping.put(var, index);
        }

        public int getIndex(String preVar) {
            return varMapping.get(preVar);
        }

        public void mapVariables() {
            int i = 0;
            for (String field : struct.getOrderedFields()) {
                varMapping.put(field, i++);
            }
        }

    }


}
