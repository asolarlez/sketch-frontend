package sketch.compiler.passes.lowering;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.NameResolver;
import sketch.compiler.ast.core.Package;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.SymbolTable;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.typs.Type;

public class EliminateGenerics extends SymbolTableVisitor {
    Map<String, Function> signatures = new HashMap<String, Function>();
    Map<String, List<Function>> newfuns = new HashMap<String, List<Function>>();
    final List<String> empty = new ArrayList<String>();
    public EliminateGenerics() {
        super(null);
    }

    String signature(Function f, TypeRenamer tr) {
        StringBuffer ls = new StringBuffer();
        ls.append(f.getFullName());
        ls.append(':');
        for (Parameter p : f.getParams()) {
            ls.append(p.getType().accept(tr));
            ls.append(",");
        }
        return ls.toString();
    }

    public Object visitExprFunCall(ExprFunCall efc){
        Function f = nres.getFun(efc.getName());
        List<String> tps = f.getTypeParams();
        if (tps.isEmpty()) {
            return super.visitExprFunCall(efc);
        }
        List<Type> lt = new ArrayList<Type>();        
        for (Expression actual : efc.getParams()) {
            lt.add(getType(actual));
        }
        TypeRenamer tr = SymbolTableVisitor.getRenaming(f, lt, nres, null);
        String sig = signature(f, tr);
        if(signatures.containsKey(sig)){
            String newName =signatures.get(sig).getFullName(); 
            return new ExprFunCall(efc, newName, efc.getParams());
        }else{
            String newName = efc.getName() + tr.postfix();
            Function newSig =
                    ((Function) ((Function) f.accept(tr)).creator().name(newName).typeParams(
                            empty).create().accept(this));
            signatures.put(sig, newSig);
            String pkgname = nres.curPkg().getName();
            if (newfuns.containsKey(pkgname)) {
                newfuns.get(pkgname).add(newSig);
            } else {
                List<Function> lf = new ArrayList<Function>();
                lf.add(newSig);
                newfuns.put(pkgname, lf);
            }
            return new ExprFunCall(efc, newName, efc.getParams());
        }                
        
    }

    public Object visitProgram(Program prog) {
        nres = new NameResolver(prog);

        SymbolTable oldSymTab = symtab;
        symtab = new SymbolTable(symtab);

        List<Package> newStreams = new ArrayList<Package>();
        for (Package pk : prog.getPackages()) {
            List<Function> lf = pk.getFuncs();
            List<Function> nl = new ArrayList<Function>();
            for (Function fun : lf) {
                if (!fun.isGeneric()) {
                    nl.add(fun);
                }
            }
            Package npkg =
                    new Package(pk, pk.getName(), pk.getStructs(), pk.getVars(), nl,
                            pk.getSpAsserts());
            newStreams.add((Package) npkg.accept(this));
        }
        for (Package pk : newStreams) {
            List<Function> lf = newfuns.get(pk.getName());
            List<Function> nl = pk.getFuncs();
            if (lf != null) {
                nl.addAll(lf);
            }
        }
        symtab = oldSymTab;

        return prog.creator().streams(newStreams).create();
    }

}