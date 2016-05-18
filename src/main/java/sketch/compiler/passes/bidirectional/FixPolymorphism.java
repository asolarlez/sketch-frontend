package sketch.compiler.passes.bidirectional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.passes.lowering.SymbolTableVisitor;
import sketch.compiler.passes.lowering.SymbolTableVisitor.TypeRenamer;
import sketch.util.exceptions.ExceptionAtNode;

/**
 * There are some functions that can no longer be polymorphic after we
 * specialize them based on their function parameters because the function
 * parameters impose certain constraints on the outputs. This class will
 * specialize the types for these functions by making them less generic.
 */
class FixPolymorphism extends SymbolTableVisitor {
    /**
     * The tren type renamer keeps track of what generic types should be
     * specialized and to what.
     */
    TypeRenamer tren;

    /**
     * namesset keeps track of the original type parameters.
     */
    Set<String> namesset;

    /**
     * elimset keeps track of which type parameters are no longer necessary
     * because they are being eliminated.
     */
    Set<String> elimset;

    Map<String, Function> doneFunctions = new HashMap<String, Function>();

    public FixPolymorphism() {
        super(null);
    }

    public Object visitFunction(Function f) {
        if (f.getTypeParams().isEmpty()) {
            return f;
        }
        if (doneFunctions.containsKey(f.getFullName())) {
            return doneFunctions.get(f.getFullName());
        }
        TypeRenamer oldtren = tren;
        Set<String> oldnamesset = namesset;
        Set<String> oldelimset = elimset;
        tren = new TypeRenamer();
        namesset = new HashSet<String>(f.getTypeParams());
        elimset = new HashSet<String>();

        Function fout = (Function) super.visitFunction(f);
        List<String> nl = new ArrayList<String>();
        for (String s : f.getTypeParams()) {
            if (elimset.contains(s)) {
                continue;
            }
            nl.add(s);
        }
        Function rf = (Function) fout.creator().typeParams(nl).create().accept(tren);
        substitute(oldtren, tren);
        tren = oldtren;
        namesset = oldnamesset;
        elimset = oldelimset;
        doneFunctions.put(f.getFullName(), rf);
        return rf;
    }

    private void substitute(TypeRenamer oldtren, TypeRenamer tren) {
        if (oldtren == null)
            return;
        for (String k : oldtren.tmap.keySet()) {
            String t = oldtren.tmap.get(k).toString();
            if (tren.tmap.containsKey(t)) {
                oldtren.tmap.put(k, tren.tmap.get(t));
            }
        }

    }

    public Object visitExprFunCall(ExprFunCall efc) {
        // We may have to unify based on function parameters
        Function f = nres.getFun(efc.getName());

        if (doneFunctions.containsKey(f.getFullName())) {
            f = doneFunctions.get(f.getFullName());
        } else {
            f = (Function) f.accept(this);
        }

        Set<String> calleenamesset = new HashSet<String>(f.getTypeParams());
        if (f == null) {
            throw new ExceptionAtNode("Function not defined", efc);
        }

        Iterator<Parameter> pit = f.getParams().iterator();
        if (f.getParams().size() != efc.getParams().size()) {
            int dif = f.getParams().size() - efc.getParams().size();
            for (int i = 0; i < dif; ++i) {
                Parameter p = pit.next();
                if (!p.isImplicit()) {
                    throw new ExceptionAtNode("Bad param number", efc);
                }
            }
        }
        for (Expression actual : efc.getParams()) {
            Type atype = getType(actual);
            Parameter p = pit.next();
            Type ptype = p.getType();
            while (atype instanceof TypeArray) {
                atype = ((TypeArray) atype).getBase();
                ptype = ((TypeArray) ptype).getBase();
            }
            Type ptypebase = ptype;
            while (ptypebase instanceof TypeArray) {
                ptypebase = ((TypeArray) ptypebase).getBase();
            }
            String aname = atype.toString();
            if (namesset.contains(aname)) {
                // This means the argument is polymorphic; if the
                // callee is not polymorphic, it means that when we
                // specialized the
                // call we should have restricted the polymorphism, so we
                // have to add
                // now.
                if (!calleenamesset.contains(ptypebase.toString())) {
                    unifyGeneric(aname, ptype, f);
                }
            }
        }
        return super.visitExprFunCall(efc);
    }

    public Object visitStmtVarDecl(StmtVarDecl svd) {

        for (int i = 0; i < svd.getNumVars(); ++i) {
            Type left = svd.getType(i);
            Expression eright = svd.getInit(i);
            if (eright != null) {
                Type right = getType(eright);
                checkAndUnify(left, right, svd);
            }
        }

        return super.visitStmtVarDecl(svd);
    }

    public Object visitStmtAssign(StmtAssign sa) {
        Type left = getType(sa.getLHS());
        Type right = getType(sa.getRHS());

        checkAndUnify(left, right, sa);

        return super.visitStmtAssign(sa);
    }

    void checkAndUnify(Type left, Type right, FENode ctxt) {
        while (left instanceof TypeArray) {
            left = ((TypeArray) left).getBase();
            right = ((TypeArray) right).getBase();
        }
        String lname = left.toString();
        if (lname.equals(right.toString())) {
            return;
        }
        if (namesset.contains(lname)) {
            unifyGeneric(lname, right, ctxt);
        }
    }

    void unifyGeneric(String genericName, Type newType, FENode ctxt) {
        elimset.add(genericName);
        if (newType instanceof TypeArray) {
            throw new ExceptionAtNode("Generics can not resolve to an array type " + genericName + "->" + newType, ctxt);
        }
        if (tren.tmap.containsKey(genericName)) {
            Type lcp = tren.tmap.get(genericName).leastCommonPromotion(newType, nres);
            tren.tmap.put(genericName, lcp);
        } else {
            tren.tmap.put(genericName, newType);
        }
    }

}