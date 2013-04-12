package sketch.compiler.ast.core;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import sketch.compiler.ast.core.typs.StructDef;
import sketch.util.exceptions.UnrecognizedVariableException;

/**
 * NameResolver helps translate the name of a struct, function, or variable from the
 * simple name (without package name) to the full name (with package name). The full name
 * will be unique and help distinguish the same simple name in different packages.
 */
public class NameResolver {
    final Map<String, String> pkgForStruct = new HashMap<String, String>();
    final Map<String, String> pkgForFun = new HashMap<String, String>();
    final Map<String, String> pkgForVar = new HashMap<String, String>();

    final Map<String, StructDef> structMap = new HashMap<String, StructDef>();
    final Map<String, Function> funMap = new HashMap<String, Function>();
    final Map<String, FieldDecl> varMap = new HashMap<String, FieldDecl>();
    Package pkg;

    public Package curPkg() {
        return pkg;
    }
    public Collection<String> structNamesList() {
        return structMap.keySet();
    }

    public NameResolver() {

    }
    public NameResolver(Program p) {
        populate(p);
    }
    public void setPackage(Package pkg) {
        this.pkg = pkg;
    }

    public String compound(String a, String b) {
        return b + "@" + a;
    }

    private <T> void registerStuff(Map<String, String> pkgForThing,
            Map<String, T> thingMap, T stuff, String name)
    {
        if (pkgForThing.containsKey(name)) {
            pkgForThing.put(name, null);
        } else {
            pkgForThing.put(name, pkg.getName());
        }
        thingMap.put(compound(pkg.getName(), name), stuff);
    }

    public void registerStruct(StructDef ts) {
        registerStuff(pkgForStruct, structMap, ts, ts.getName());
    }

    public void registerFun(Function f) {
        registerStuff(pkgForFun, funMap, f, f.getName());
    }

    public void registerVar(FieldDecl fd) {
        for (int i = 0; i < fd.getNumFields(); ++i) {
            registerStuff(pkgForVar, varMap, fd, fd.getName(i));
        }
    }

    public <T> String getFullName(String name, Map<String, String> pkgForThing,
            Map<String, T> chkMap, String defPkg)
    {
        if (name.indexOf("@") > 0) {
            return name;
        }
        if (name.indexOf("@") == name.length() - 1) {
            name = name.substring(0, name.length() - 1);
        }
        String pkgName = pkgForThing.get(name);
        if (pkgName == null) {
            String cpkgNm = name + "@" + defPkg;
            if (chkMap.containsKey(cpkgNm)) {
                return cpkgNm;
            }
            // System.err.println("Name " + name + " is ambiguous.");
            return null;
        }
        return name + "@" + pkgName;
    }

    public FieldDecl getVar(String name) {
        String full = getFullName(name, pkgForVar, varMap, this.pkg.getName());
        if (full == null) {
            return null;
        }
        FieldDecl f = varMap.get(full);

        return f;
    }

    public Function getFun(String name) {
        String full = getFullName(name, pkgForFun, funMap, this.pkg.getName());
        if (full == null) {
            return null;
        }
        Function f = funMap.get(full);

        return f;
    }

    public Function getFun(String name, FENode errSource) {
        Function f = getFun(name);
        if (f == null)
            throw new UnrecognizedVariableException(name, errSource);
        return f;
    }


    public String getFunName(Function f) {
        if (pkgForFun.containsKey(f.getName())) {
            return f.getName() + "@" + pkgForFun.get(f.getName());
        } else {
            throw new RuntimeException("NYI");
        }
    }

    /**
     * Returns a unique name for a structure. Disambiguates between structs in different
     * namespaces.
     * 
     * @param name
     * @return
     */
    public String getStructName(String name, String defPkg) {
        return getFullName(name, pkgForStruct, structMap, defPkg);
    }

    public String getStructName(String name) {
        return getStructName(name, this.pkg.getName());
    }

    public String getFunName(String name, String defPkg) {
        return getFullName(name, pkgForFun, funMap, defPkg);
    }

    public String getFunName(String name) {
        return getFunName(name, this.pkg.getName());
    }

    public StructDef getStruct(String name) {
        String full = getFullName(name, pkgForStruct, structMap, this.pkg.getName());
        if (full == null) {
            return null;
        }
        return structMap.get(full);
    }

    public void populate(Package pkg) {
        this.pkg = pkg;
        for (StructDef ts : pkg.getStructs()) {
            registerStruct(ts);
        }
        for (Function f : pkg.getFuncs()) {
            registerFun(f);
        }
        for (FieldDecl fd : pkg.getVars()) {
            registerVar(fd);
        }
    }

    public void populate(Program p) {
        for (Package pkg : p.getPackages()) {
            populate(pkg);
        }
    }

}