package sketch.compiler.passes.spmd;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Function.FunctionCreator;
import sketch.compiler.ast.core.Package;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.exprs.ExprArrayRange;
import sketch.compiler.ast.core.exprs.ExprField;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtBlock;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.spmd.stmts.SpmdBarrier;
import sketch.compiler.ast.spmd.stmts.StmtSpmdfork;
import sketch.compiler.passes.annotations.CompilerPassDeps;
import sketch.compiler.passes.lowering.SymbolTableVisitor;

/*
 * Currently treat all structs as final After PreprocessSketch all function calls inlined.
 * top level stencil function cannot input / output structs Not supported: pointer
 * assignment (struct X {} X p = ...; X q; q = p) array of structs struct in struct
 * (struct X { struct y; }) bugs: now fields are emitted in the order they are stored in
 * the map. but the correct order is to emit non-arrays first, then arrays, so that the
 * array size variable will occur before the array declaration
 */

@CompilerPassDeps(runsBefore = {}, runsAfter = {})
public class ChangeGlobalStateType extends SymbolTableVisitor {
    // functionName => (varName => LocalStateType)
    Map<String, Map<String, Type>> taint;
    Map<String, Type> currentT;
    
    Set<String> visitedFunc;

    // backward ordering
    List<Function> order;
    Map<String, Function> lookupFunc;

    BackwardTaint backTainter = new BackwardTaint();

    boolean cTainted(ExprVar v) {
        return currentT.containsKey(v.getName());
    }

    class BackwardTaint extends FEReplacer {
        @Override
        public Object visitStreamSpec(Package p) {
            List<Function> funcs = p.getFuncs();
            int n = funcs.size();
            lookupFunc = new HashMap<String, Function>(n);
            visitedFunc = new HashSet<String>(n);
            order = new ArrayList<Function>(n);
            taint = new HashMap<String, Map<String, Type>>(n);
            for (Function f : funcs) {
                lookupFunc.put(f.getName(), f);
            }
            for (Function f : funcs) {
                visitFunction(f);
            }
            return p;
        }

        @Override
        public Object visitFunction(Function func) {
            String name = func.getName();
            if (visitedFunc.contains(name)) {
                return func;
            }
            visitedFunc.add(name);
            // currentT = taint.get(name);
            assert !taint.containsKey(name) : "should not contain " + name;
            currentT = new HashMap<String, Type>();
            taint.put(name, currentT);
            // if (name.startsWith("movein")) {
            // List<Parameter> p = func.getParams();
            // Parameter global = p.get(p.size() - 1);
            // for (Parameter param : p) {
            // assert param.isParameterInput() :
            // "only input parameters allowed in movein!";
            // }
            // currentT.put(global.getName(), func.getReturnType());
            // }

            if (name.startsWith("moveout") || name.startsWith("movein")) {
                List<Parameter> p = func.getParams();
                Parameter local = p.get(p.size() - 1);
                Parameter global = p.get(p.size() - 2);
                if (name.startsWith("moveout")) {
                    assert local.isParameterInput() && global.isParameterOutput() : "wrong param type for moveout";
                } else {
                    assert local.isParameterOutput() && global.isParameterInput() : "wrong param type for movein";
                }
                for (int i = 0; i < p.size() - 2; ++i) {
                    assert p.get(i).isParameterInput() : "only one output allowed!";
                }
                currentT.put(global.getName(), local.getType());
            } else {
                Statement body = func.getBody();
                if (body != null) {
                    body.accept(this);
                }
            }

            order.add(func);
            return func;
        }
        
        @Override
        public Object visitStmtBlock(StmtBlock sb) {
            List<Statement> s = sb.getStmts();
            for (int i = s.size() - 1; i >= 0; --i) {
                s.get(i).accept(this);
            }
            return sb;
        }

        @Override
        public Object visitStmtAssign(StmtAssign s) {
            Object lhs = s.getLHS().accept(this);
            if (lhs instanceof ExprVar) {
                if (cTainted(((ExprVar) lhs))) {
                    System.out.println("Warning: manipulating global var " + lhs + " " +
                            s.getCx());
                }
            }
            Object rhs = s.getRHS().accept(this);
            if (rhs instanceof ExprVar) {
                if (cTainted(((ExprVar) rhs))) {
                    System.out.println("Warning: manipulating global var " + rhs + " " +
                            s.getCx());
                }
            }
            return s;
        }

        @Override
        public Object visitExprField(ExprField e) {
            Object lhs = e.getLeft().accept(this);
            if (lhs instanceof ExprVar) {
                if (cTainted(((ExprVar) lhs))) {
                    System.out.println("Warning: manipulating global var " + lhs + " " +
                            e.getCx());
                }
            }
            return e;
        }

        @Override
        public Object visitExprArrayRange(ExprArrayRange e) {
            Object lhs = e.getBase().accept(this);
            if (lhs instanceof ExprVar) {
                if (cTainted(((ExprVar) lhs))) {
                    System.out.println("Warning: manipulating global var " + lhs + " " +
                            e.getCx());
                }
            }
            return e;
        }

        @Override
        public Object visitExprFunCall(ExprFunCall e) {
            String callee = e.getName();
            // if (callee.startsWith("movein")) {
            // return e;
            // }
            Function f = lookupFunc.get(callee);
            if (f != null) {
                if (!visitedFunc.contains(callee)) {
                    Map<String, Type> c = currentT;
                    visitFunction(f);
                    currentT = c;
                }
                List<Parameter> p = f.getParams();
                List<Expression> arg = e.getParams();
                for (int i=0; i<p.size(); ++i) {
                    Object a = arg.get(i).accept(this);
                    if (a instanceof ExprVar) {
                        Type t = taint.get(callee).get(p.get(i).getName());
                        if (t != null) {
                            String vname = ((ExprVar) a).getName();
                            Type t0 = currentT.get(vname);
                            if (t0 == null) {
                                currentT.put(vname, t);
                            } else {
                                assert t.equals(t0) : "conflict tainted type!";
                            }
                        }
                    }
                }
            }
                    
            return e;
        }
    }
    
    public ChangeGlobalStateType() {
        super(null);
    }
    
    @Override
    public Object visitStreamSpec(Package p) {
        backTainter.visitStreamSpec(p);
        for (int i = order.size() - 1; i >= 0; --i) {
            Function f = order.get(i);
            String name = f.getName();
            if (!(name.startsWith("movein") || name.startsWith("moveout"))) {
                lookupFunc.put(name, (Function) visitFunction(f));
            }
        }
        List<Function> funcs = p.getFuncs();

        for (int i = 0; i < funcs.size(); ++i) {
            Function f = funcs.get(i);
            String name = f.getName();
            // NOTE xzl: don't remove movein and moveout
            // if (name.startsWith("movein") || name.startsWith("moveout")) {
            // funcs.remove(i);
            // } else {
            f = lookupFunc.get(name);
            if (f != null) {
                funcs.set(i, f);
            }
        }

        return p;
    }

    @Override
    public Object visitFunction(Function f) {
        String fname = f.getName();
        if (fname.startsWith("movein") || fname.startsWith("moveout")) {
            return null;
        }
        currentT = taint.get(f.getName());
        int size = currentT.size();
        if (size == 0) {
            return f;
        }

        List<Parameter> param = f.getParams();
        List<Parameter> newp = new ArrayList<Parameter>(param.size());
        for (Parameter p : param) {
            String name = p.getName();
            Type t = currentT.get(name);
            if (t != null) {
                newp.add(new Parameter(p, t, name, p.getPtype()));
            } else {
                newp.add(p);
            }
        }

        FunctionCreator creator = f.creator().params(newp);
        Statement body = f.getBody();
        if (body != null) {
            Statement newbody = (Statement) body.accept(this);
            if (newbody != body) {
                creator = creator.body(newbody);
            }
        }

        return creator.create();
    }

    @Override
    public Object visitStmtVarDecl(StmtVarDecl s) {
        int n = s.getNumVars();
        List<Type> type = new ArrayList<Type>(n);
        boolean changed = false;
        for (int i = 0; i < n; ++i) {
            String var = s.getName(i);
            Type t = currentT.get(var);
            if (t != null) {
                assert s.getInit(i) == null : "cannot initialize tainted var!";
                changed = true;
                type.add(t);
            } else {
                type.add(s.getType(i));
                if (s.getInit(i) != null) {
                    s.setInit(i, (Expression) s.getInit(i).accept(this));
                }
            }
        }

        if (changed) {
            return new StmtVarDecl(s.getCx(), type, s.getNames(), s.getInits());
        }
        return s;
    }

    @Override
    public Object visitStmtAssign(StmtAssign s) {
        Object lhs = s.getLHS().accept(this);
        if (lhs instanceof ExprVar) {
            assert !cTainted(((ExprVar) lhs)) : "cannot manipulate tainted var!";
        }
        Expression rhs = s.getRHS();
        if (rhs instanceof ExprVar) {
            assert !cTainted(((ExprVar) rhs)) : "cannot manipulate tainted var!";
        }
        Object newrhs = rhs.accept(this);
        if (newrhs == null) {
            return null;
        } else if (newrhs == rhs) {
            return s;
        } else {
            return new StmtAssign(s, (Expression) lhs, (Expression) newrhs);
        }
    }

    @Override
    public Object visitExprField(ExprField e) {
        Object lhs = e.getLeft().accept(this);
        if (lhs instanceof ExprVar) {
            assert !cTainted(((ExprVar) lhs)) : "cannot manipulate tainted var!";
        }
        return e;
    }

    @Override
    public Object visitExprArrayRange(ExprArrayRange e) {
        Object lhs = e.getBase().accept(this);
        if (lhs instanceof ExprVar) {
            assert !cTainted(((ExprVar) lhs)) : "cannot manipulate tainted var!";
        }
        return e;
    }

    @Override
    public Object visitExprFunCall(ExprFunCall e) {
        String callee = e.getName();
        if (callee.startsWith("movein")) {
            List<Expression> arg = e.getParams();
            Expression local = arg.get(arg.size() - 1);
            Expression global = arg.get(arg.size() - 2);
            assert local instanceof ExprVar && global instanceof ExprVar : "moveout must assign two vars!";
            addStatement(new StmtAssign(e, local, global));
            return null;
        } else if (callee.startsWith("moveout")) {
            List<Expression> arg = e.getParams();
            Expression local = arg.get(arg.size() - 1);
            Expression global = arg.get(arg.size() - 2);
            assert local instanceof ExprVar && global instanceof ExprVar : "moveout must assign two vars!";
            addStatement(new StmtAssign(e, global, local));
            return null;
        }
        Function f = lookupFunc.get(callee);
        Map<String, Type> c = taint.get(callee);
        if (f != null) {
            List<Parameter> p = f.getParams();
            List<Expression> arg = e.getParams();
            for (int i = 0; i < p.size(); ++i) {
                Object a = arg.get(i).accept(this);
                if (a instanceof ExprVar) {
                    Type t = currentT.get(((ExprVar) a).getName());
                    if (t != null) {
                        String vname = p.get(i).getName();
                        Type t0 = c.get(vname);
                        if (t0 == null) {
                            c.put(vname, t);
                        } else {
                            assert t.equals(t0) : "conflict tainted type!";
                        }
                    }
                }
            }
        }

        return e;
    }

    @Override
    public Object visitStmtSpmdfork(StmtSpmdfork s) {
        return s.getBody().accept(this);
    }
    
    @Override
    public Object visitSpmdBarrier(SpmdBarrier s) {
        return null;
    }
}
