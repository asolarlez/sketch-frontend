package sketch.compiler.codegenerators;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import sketch.compiler.ast.core.Annotation;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Package;
import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.SymbolTable;
import sketch.compiler.ast.core.typs.StructDef;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.util.Pair;

public class NodesToSuperH extends NodesToSuperCpp {

    // private NodesToSuperCpp _converter;
    private String filename;
    private String preIncludes;
    private String creators;

    protected static Map<String, String> typeVars = new HashMap<String, String>();

    public NodesToSuperH(String filename) {
        super(null, filename);
        // _converter = new NodesToSuperCpp(null, filename, pythonPrintStatements);
        this.filename = filename;
        this.addIncludes = false;
    }



    public String outputStructure(StructDef struct) {
        String result = "";
        SymbolTable oldSymTab = symtab;
        symtab = new SymbolTable(symtab);

        for (Annotation a : struct.getAnnotation("NeedsInclude")) {
            preIncludes += a.contents() + "\n";
        }

        result += indent + "class " + escapeCName(struct.getName());
        if (nres.getStructParentName(struct.getName())!=null){
            result +=
                    " : public " +
                            nres.getStructParentName(struct.getName()).split("@")[0];
        }
        result += "{\n  public:\n";
        addIndent();

        for (Annotation a : struct.getAnnotation("Native")) {
            String s = a.rawParams.substring(1, a.rawParams.length() - 1);
            s = s.replace("\\\"", "\"");
            s = s.replace("\\\\", "\\");
            result += indent + s + "\n";
        }

        List<Pair<String, TypeArray>> fl = new ArrayList<Pair<String, TypeArray>>();
        if (!struct.isInstantiable() &&
                nres.getStructParentName(struct.getName()) == null)
        {
            //ADT change as the name might already be taken
            // add field for type
            String var = "type";
            List varNames = new ArrayList();

            String children = "";
            LinkedList<String> list = new LinkedList<String>();
            list.add(struct.getName());
            while (!list.isEmpty()) {
                String parent = list.removeFirst();
                varNames.addAll(nres.getStruct(parent).getFields());
                for (String child : nres.getStructChildren(parent)) {
                    list.add(child);
                    children += child.split("@")[0].toUpperCase() + "_type, ";
                }
            }
            children = children.substring(0, children.length() - 2);
            while (varNames.contains(var)) {
                var = "_" + var;
            }
            String typeVar = "_kind";
            while (varNames.contains(typeVar)) {
                typeVar = "_" + typeVar;
            }
            typeVars.put(struct.getFullName(), var);
            result += indent + "typedef enum {" + children + "} " + typeVar + ";\n"; // add
                                                                                     // _kind
            result += indent + typeVar + " " + var + ";\n";
        }
        for (String field : struct.getOrderedFields()) {
            Type ftype = struct.getType(field);
            if (ftype instanceof TypeArray) {
                fl.add(new Pair<String, TypeArray>(field, (TypeArray) ftype));
                continue;
            }
            result += indent + typeForDecl(ftype) + " " + field + ";\n";
        }
        int cnt = 0;
        int sz = fl.size();
        for (Pair<String, TypeArray> af : fl) {
            ++cnt;
            if (cnt == sz) {
                result +=
                        indent + typeForDecl(af.getSecond().getBase()) + " " +
                                af.getFirst() + "[];\n";
            } else {
                result +=
                        indent + typeForDecl(af.getSecond()) + " " + af.getFirst() +
                                ";\n";
            }
        }

        if (struct.getNumFields() != 0) {
            result += indent + escapeCName(struct.getName()) + "(){}\n";
        }

        if (struct.isInstantiable()) {

            result += indent + "static " + escapeCName(struct.getName()) + "* create(";
            boolean first = true;
            StructDef current = struct;
            List fieldNames = new ArrayList();
            while (current != null) {
                for (String field : current.getOrderedFields()) {
                    if (!fieldNames.contains(field)) {
                        fieldNames.add(field);
                        Type ftype = current.getType(field);
                        if (first) {
                            first = false;
                        } else {
                            result += ", ";
                        }
                        result += indent + typeForDecl(ftype) + " " + field + "_";
                        symtab.registerVar(field + "_", (ftype), current,
                                SymbolTable.KIND_LOCAL);
                        if (ftype instanceof TypeArray) {
                            result += ", int " + field + "_len";
                        }
                    }
                }
                String parent;
                if ((parent = nres.getStructParentName(current.getName())) != null) {
                    current = nres.getStruct(parent);
                } else {
                    current = null;
                }
            }
            result += ");\n";
        }

        result += indent + "~" + escapeCName(struct.getName()) + "(){\n";
        result += indent + "}\n";

        result += indent + "void operator delete(void* p){ free(p); }\n";

        unIndent();
        result += indent + "};\n";
        symtab = oldSymTab;
        return result;
    }

    @Override
    public Object visitProgram(Program prog) {
        String defname = filename.toUpperCase() + "_H";
        defname = defname.replace('.', '_');
        String ret = "#ifndef " + defname + "\n";
        ret += "#define " + defname + "\n\n";
        ret += "#include <cstring>\n\n";
        ret += "#include \"vops.h\"\n\n";

        for (Package pkg : prog.getPackages()) {
            ret += "namespace " + pkg.getName() + "{\n";
            for (StructDef ts : pkg.getStructs()) {
                ret += "  class " + ts.getName() + ";\n";
            }
            ret += "}\n";
        }

        ret += super.visitProgram(prog);
        ret += "\n#endif\n";
        return ret;
    }

    public Object visitPackage(Package spec) {
        String result = "";
        nres.setPackage(spec);
        preIncludes = "";
        result += "namespace " + spec.getName() + "{\n";
        for (Iterator iter = spec.getStructs().iterator(); iter.hasNext();) {
            StructDef struct = (StructDef) iter.next();
            result += "class " + escapeCName(struct.getName()) + "; \n";
        }

        for (Iterator iter = spec.getStructs().iterator(); iter.hasNext();) {
            StructDef struct = (StructDef) iter.next();
            result += outputStructure(struct);
        }

        for (Iterator iter = spec.getFuncs().iterator(); iter.hasNext();) {
            Function oldFunc = (Function) iter.next();
            result += (String) oldFunc.accept(this);
        }
        result += "}\n";
        return preIncludes + result;
    }

    public Object visitFunction(Function func) {
        setNres(nres);
        String result = "";

        result += indent + "extern ";
        result += convertType(func.getReturnType()) + " ";
        result += escapeCName(func.getName());
        String prefix = null;
        result += doParams(func.getParams(), prefix) + ";\n";
        return result;
    }

}
