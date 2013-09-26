package sketch.compiler.codegenerators;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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

        result += indent + "class " + escapeCName(struct.getName()) + "{\n  public:\n";
        addIndent();

        for (Annotation a : struct.getAnnotation("Native")) {
            String s = a.rawParams.substring(1, a.rawParams.length() - 1);
            s = s.replace("\\\"", "\"");
            s = s.replace("\\\\", "\\");
            result += indent + s + "\n";
        }

        List<Pair<String, TypeArray>> fl = new ArrayList<Pair<String, TypeArray>>();
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



        result += indent + "static " + escapeCName(struct.getName()) + "* create(";
        boolean first = true;
        for (String field : struct.getOrderedFields()) {
            Type ftype = struct.getType(field);
            if (first) {
                first = false;
            } else {
                result += ", ";
            }
            result += indent + typeForDecl(ftype) + " " + field + "_";
            symtab.registerVar(field + "_", (ftype), struct, SymbolTable.KIND_LOCAL);
            if (ftype instanceof TypeArray) {
                result += ", int " + field + "_len";
            }
        }
        result += ");\n";

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
