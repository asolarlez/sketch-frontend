package sketch.compiler.ast.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import sketch.compiler.ast.core.typs.StructDef;
import sketch.compiler.ast.core.typs.Type;
import sketch.util.datastructures.HashmapList;
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
    // ADT
    final Map<String, String> structParent = new HashMap<String, String>();
    final Map<String, List<String>> structChildren = new HashMap<String, List<String>>();

    final Map<String, Function> funMap = new HashMap<String, Function>();
    final Map<String, FieldDecl> varMap = new HashMap<String, FieldDecl>();
    Stack<Set<String>> tempStructNames = new Stack<Set<String>>();
    Package pkg;

    public void pushTempTypes(List<String> tempTypes) {
        tempStructNames.push(new HashSet(tempTypes));
    }

    public void popTempTypes() {
        tempStructNames.pop();
    }

    public boolean isTemplate(String name) {
        for (Set<String> sts : tempStructNames) {
            if (sts.contains(name)) {
                return true;
            }
        }
        return false;
    }

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
        if (b.contains("@")) {
            return b;
        }
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

    // ADT
    public void registerStructParent(String structName, String parentName) {
        structName = compound(pkg.getName(), structName);
        if (parentName != null) {
            parentName = compound(pkg.getName(), parentName);
        }
        structParent.put(structName, parentName);
        if (parentName != null) {
            if (structChildren.containsKey(parentName)) {
                structChildren.get(parentName).add(structName);
            } else {
                structChildren.put(parentName, new ArrayList());
                structChildren.get(parentName).add(structName);

            }
        }

    }



    public void registerStruct(StructDef ts) {
        registerStuff(pkgForStruct, structMap, ts, ts.getName());
        registerStructParent(ts.getName(), ts.getParentName());
    }

    public void registerFun(Function f) {
        registerStuff(pkgForFun, funMap, f, f.getName());
    }

    public void reRegisterFun(Function f) {
        String name = f.getName();
        assert pkgForFun.get(name).equals(pkg.getName());
        funMap.put(compound(pkg.getName(), name), f);
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
        for (Set<String> sts : tempStructNames) {
            if (sts.contains(name)) {
                return name;
            }
        }
        return getFullName(name, pkgForStruct, structMap, defPkg);
    }

    public String getStructName(String name) {
        for (Set<String> sts : tempStructNames) {
            if (sts.contains(name)) {
                return name;
            }
        }
        return getStructName(name, this.pkg.getName());
    }

    // ADT
    public String getStructParentName(String name) {
        name = getFullName(name, pkgForStruct, structMap, this.pkg.getName());
        if (structParent.containsKey(name))
            return structParent.get(name);
        else
            return null;
    }

    // ADT
    public List<String> getStructChildren(String name) {
        name = getFullName(name, pkgForStruct, structMap, this.pkg.getName());
        if (structChildren.containsKey(name)) {
            return structChildren.get(name);
        } else {
            return new ArrayList<String>();
        }
    }

    public String getFunName(String name, String defPkg) {
        return getFullName(name, pkgForFun, funMap, defPkg);
    }

    public String getFunName(String name) {
        return getFunName(name, this.pkg.getName());
    }

    public final StructDef template = StructDef.creator(null, "__TEMPLATE__", null,
            false, new ArrayList<String>(), new ArrayList<Type>(),
            new HashmapList<String, Annotation>()).create();
    public StructDef getStruct(String name) {
        for (Set<String> sts : tempStructNames) {
            if (sts.contains(name)) {
                return template;
            }
        }
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