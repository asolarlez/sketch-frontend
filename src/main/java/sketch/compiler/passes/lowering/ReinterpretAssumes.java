package sketch.compiler.passes.lowering;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Function.FcnType;
import sketch.compiler.ast.core.NameResolver;
import sketch.compiler.ast.core.Package;
import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.StmtAssert;
import sketch.compiler.ast.core.stmts.StmtAssume;
import sketch.util.datastructures.HashmapList;

public class ReinterpretAssumes extends FEReplacer {

    class HasAssumes extends FEReplacer {
        public boolean hasAssumes = false;

        protected Expression doExpression(Expression expr) {
            return expr;
        }

        public Object visitStmtAssume(StmtAssume sa) {
            hasAssumes = true;
            return sa;
        }

    }

    class ReplaceAssumes extends FEReplacer {
        public boolean didReplacement = false;
        protected Expression doExpression(Expression expr) {
            return expr;
        }
        public Object visitStmtAssume(StmtAssume sa) {
            didReplacement = true;
            return new StmtAssert(sa, sa.getCond(), sa.getMsg(), false);
        }

    }

    ReplaceAssumes ra = new ReplaceAssumes();
    HasAssumes ha = new HasAssumes();
    public Function createNoAssumeClone(Function f) {
        ra.didReplacement = false;
        Function clone = (Function) f.accept(ra);
        if (ra.didReplacement) {
            return clone;
        }else{
            return null;
        }
    }

    public class ReplaceFunsInWrapper extends FEReplacer {
        public Object visitExprFunCall(ExprFunCall efc) {
            String name = efc.getName();
            if (renameMap.containsKey(name)) {
                // This assumes that all the preprocessing is done, so the params won't
                // have any functions in them.
                return new ExprFunCall(efc, renameMap.get(name), efc.getParams());
            } else {
                ha.hasAssumes = false;
                Function orig = nres.getFun(name);
                orig.accept(ha);
                if(ha.hasAssumes){
                    // All functions called by the wrapper will be in the same package as
                    // it.
                    String newName = addNewClone(orig);
                    return new ExprFunCall(efc, newName, efc.getParams());
                }else{
                    return efc;
                }                                
            }
        }
    }

    public Object visitProgram(Program prog) {
        assert prog != null : "FEReplacer.visitProgram: argument null!";
        nres = new NameResolver(prog);
        List<Package> newPkgs = new ArrayList<Package>();
        for (Package ssOrig : prog.getPackages()) {
            newPkgs.add((Package) ssOrig.accept(this));
        }
        
        ReplaceFunsInWrapper rfuns = new ReplaceFunsInWrapper();
        rfuns.setNres(nres);
        List<Package> newNewPkgs = new ArrayList<Package>();
        for (Package pk : newPkgs) {
            nres.setPackage(pk);
            List<Function> lf = new ArrayList<Function>();
             for(Function f : pk.getFuncs()){
                 //What follows is a big hack to deal with the _Wrapper that may surround harnesses.
                if (f.getFcnType() == FcnType.Wrapper) {
                     lf.add((Function) f.accept(rfuns));
                 }else{
                     lf.add(f);
                 }
             }

            Iterable<Function> flist = funsForPkg.get(pk.getName());
            if (flist != null) {
                for (Function nf : flist) {
                    lf.add(nf);
                }
            }
            newNewPkgs.add(new Package(pk, pk.getName(), pk.getStructs(), pk.getVars(),
                    lf, pk.getSpAsserts()));
        }
        
        return prog.creator().streams(newNewPkgs).create();
    }

    Map<String, String> renameMap = new HashMap<String, String>();
    HashmapList<String, Function> funsForPkg = new HashmapList<String, Function>();

    String addNewClone(Function f) {
        String oldName = f.getName();
        int i = 0;
        String freshName = oldName + i;
        while (nres.getFun(freshName) != null) {
            ++i;
            freshName = oldName + i;
        }
        f = f.creator().name(freshName).create();
        funsForPkg.append(f.getPkg(), f);
        renameMap.put(oldName, freshName);
        return freshName;
    }

    public Function visitFunction(Function func) {
        // We are going to replace assumes with asserts for all functions.
        // however, if the function is a harness or a spec, we will keep a separate copy
        // that has the assumes around.
        // The catch is that we can't tell if a function is a spec until we see its
        // implementation.
        // If the spec is encountered before the implementation, it will have already been
        // transformed,
        // but nres will have the original copy, so the function can get that original and
        // add it as a new clone.
        if (func.isSketchHarness()) {
            Function clone = createNoAssumeClone(func);
            if (clone != null) {
                addNewClone(func);
                return clone.creator().type(FcnType.Static).create(); // the version with
                                                                      // no assumes is no
                                                                      // longer a harness.
            }
            return func;
        }
        if (func.getSpecification() != null) {
            Function spec = nres.getFun(func.getSpecification());
            if (renameMap.containsKey(spec.getName())) {
                // The spec was either a harness or a spec to another function and had
                // assumes so we already have a renamed copy.
                func = func.creator().spec(renameMap.get(spec.getName())).create();
            } else {
                // There are two possibilities; either the spec had assumes, in which case
                // we want to add the original version (which is stored in nrses) as a
                // renamed clone.
                // Or it didn't have assumes, in which case we want to leave it alone.
                ha.hasAssumes = false;
                spec.accept(ha);
                if (ha.hasAssumes) {
                    String newName = addNewClone(spec);
                    func = func.creator().spec(newName).create();
                }                                              
            }
            // The implementaion must accept all
            // the inputs accepted by the spec.
            // we can check this by turning all the implementation's assumptions into
            // assertions.
            // so there is no need to have a version with assumptions, so just let the
            // common case handle the implementation.
        }
        Function clone = createNoAssumeClone(func);
        if (clone != null) {

            func = clone;
        }
        return func;
    }

}
