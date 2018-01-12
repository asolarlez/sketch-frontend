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
import sketch.compiler.ast.core.typs.StructDef;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeStructRef;

public class EliminateGenerics extends SymbolTableVisitor {
    Map<String, Function> signatures = new HashMap<String, Function>();
    Map<String, List<Function>> newfuns = new HashMap<String, List<Function>>();
    Map<String, StructDef> structs = new HashMap<String, StructDef>();
    Map<String, List<StructDef>> newstructs = new HashMap<String, List<StructDef>>();

    final List<String> empty = new ArrayList<String>();
    public EliminateGenerics() {
        super(null);
    }

    StructDef newStructDef(TypeStructRef tsr) {
        StructDef sd = nres.getStruct(tsr.getName());
        final Map<String, Type> ms = GetExprType.replMap(sd, tsr, null);

        TypeRenamer tr = new TypeRenamer();
        tr.tmap = ms;
        String newname = sd.getName() + tr.postfix();
        sd = ((StructDef) sd.accept(tr)).creator().typeargs(null).name(newname).create();
        structs.put(tsr.toString(), sd);
        sd = ((StructDef) sd.accept(this));
        structs.put(tsr.toString(), sd);
        return sd;
    }

    public Object visitTypeStructRef(TypeStructRef tsr) {
        TypeStructRef newref = (TypeStructRef) super.visitTypeStructRef(tsr);
        if (newref.hasTypeParams()) {
            String name = newref.toString();
            if (structs.containsKey(name)) {
                return new TypeStructRef(structs.get(name).getName(), tsr.isUnboxed(), null);
            } else {
                StructDef sd = newStructDef(newref);
                String pkgname = nres.curPkg().getName();
                if (newstructs.containsKey(pkgname)) {
                    newstructs.get(pkgname).add(sd);
                } else {
                    List<StructDef> lsd = new ArrayList<StructDef>();
                    lsd.add(sd);
                    newstructs.put(pkgname, lsd);
                }
                return new TypeStructRef(sd.getName(), tsr.isUnboxed(), null);
            }
        } else {
            return newref;
        }
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

        TypeRenamer tr = new TypeRenamer(doCallTypeParams(efc));

        String sig = signature(f, tr);
        if(signatures.containsKey(sig)){
            String newName =signatures.get(sig).getFullName(); 
            return new ExprFunCall(efc, newName, efc.getParams(), null);
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
            return new ExprFunCall(efc, newName, efc.getParams(), null);
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

            List<StructDef> nsdl = new ArrayList<StructDef>();
            for (StructDef sd : pk.getStructs()) {
                if (!sd.hasTypeargs()) {
                    nsdl.add(sd);
                }
            }

            Package npkg =
                    new Package(pk, pk.getName(), nsdl, pk.getVars(), nl,
                            pk.getSpAsserts());
            newStreams.add((Package) npkg.accept(this));
        }
        for (Package pk : newStreams) {
            List<Function> lf = newfuns.get(pk.getName());
            List<Function> nl = pk.getFuncs();
            if (lf != null) {
                nl.addAll(lf);
            }
            List<StructDef> lsd = newstructs.get(pk.getName());
            List<StructDef> nd = pk.getStructs();
            if (lsd != null) {
                nd.addAll(lsd);
            }
        }
        symtab = oldSymTab;

        return prog.creator().streams(newStreams).create();
    }

}
