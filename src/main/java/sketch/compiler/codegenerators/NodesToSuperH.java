package sketch.compiler.codegenerators;

import java.util.Iterator;
import java.util.Map.Entry;

import sketch.compiler.ast.core.Annotation;
import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Package;
import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.SymbolTable;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.typs.StructDef;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.util.datastructures.TypedHashMap;

public class NodesToSuperH extends NodesToSuperCpp {

    // private NodesToSuperCpp _converter;
    private String filename;
    private String preIncludes;

    public NodesToSuperH(String filename) {
        super(null, filename);
        // _converter = new NodesToSuperCpp(null, filename, pythonPrintStatements);
        this.filename = filename;
        this.addIncludes = false;
    }

    public String outputStructure(StructDef struct){
        String result = "";
        SymbolTable oldSymTab = symtab;
        symtab = new SymbolTable(symtab);

        for (Annotation a : struct.getAnnotation("NeedsInclude")) {
            preIncludes += a.contents() + "\n";
        }

        result += indent + "class " + escapeCName(struct.getName()) + "{\n  public:\n";
        addIndent();
        for (String field : struct.getOrderedFields()) {
            Type ftype = struct.getType(field);
            result +=
 indent + typeForDecl(ftype) + " " + field + ";\n";
        }
        
//        result += indent + escapeCName(struct.getName()) + "(){\n";
//        addIndent();
//        for (Entry<String, Type> entry : struct) {
//            String init;            
//            if(entry.getValue() instanceof TypeArray){
//                TypeArray ta = (TypeArray) entry.getValue();
//                String typename = typeForDecl(ta.getBase());
//                String lenString = (String)ta.getLength().accept(this);
//                init = "new " + typename + "[" + lenString + "]";
//                init += "; memset(" + entry.getKey() + ", " + ta.getBase().defaultValue().accept(this) 
//                + ", sizeof(" + typename + ")*" + lenString + ")";  
//            }else{
//                init = (String) entry.getValue().defaultValue().accept(this);
//            }
//            result +=
//                    indent + entry.getKey() + " = " + init + ";\n";
//        }
//        unIndent();
//        result += indent + "}\n";

        if (struct.getNumFields() != 0) {
            result += indent + escapeCName(struct.getName()) + "(){}\n";
        }
        result += indent + escapeCName(struct.getName()) + "(";
        boolean first = true;
        for (String field : struct.getOrderedFields()) {
            Type ftype = struct.getType(field);
            if (first) {
                first = false;
            } else {
                result += ", ";
            }
            result += indent + typeForDecl(ftype) + " " + field + "_";
            symtab.registerVar(field + "_", (ftype),
                    struct, SymbolTable.KIND_LOCAL);
            if (ftype instanceof TypeArray) {
                result += ", int " + field + "_len";
            }
        }
        result += "){\n";
        addIndent();

        final TypedHashMap<String, Type> fmap = struct.getFieldTypMap();
        FEReplacer fer = new FEReplacer() {
            public Object visitExprVar(ExprVar ev) {
                if (fmap.containsKey(ev.getName())) {
                    return new ExprVar(ev, ev.getName() + "_");
                } else {
                    return ev;
                }
            }
        };

        for (String field : struct.getOrderedFields()) {
            Type ftype = struct.getType(field);

            if (ftype instanceof TypeArray) {
                TypeArray ta = (TypeArray) ftype;
                String typename = typeForDecl(ta.getBase());
                String lenString =
                        (String) ((Expression) ta.getLength().accept(fer)).accept(this);
                result +=
                        indent + field + " = " + "new " + typename + "[" +
                                lenString + "]" + ";\n";
                result +=
                        indent +
 "CopyArr(" + field + ", " + field +
                                "_, " +
 lenString +
                                ", " + field + "_len ); \n";
            } else {
                result += indent + field + " = " + " " + field + "_;\n";
            }
        }

        unIndent();
        result += indent + "}\n";

        result += indent + "~" + escapeCName(struct.getName()) + "(){\n";
        addIndent();

        for (Entry<String, Type> entry : struct) {
            if (entry.getValue() instanceof TypeArray) {
                result += indent + "delete[] " + entry.getKey() + ";\n";
            }
        }

        unIndent();
        result += indent + "}\n";

        for (Annotation a : struct.getAnnotation("Native")) {
            String s = a.rawParams.substring(1, a.rawParams.length() - 1);
            s = s.replace("\\\"", "\"");
            s = s.replace("\\\\", "\\");
            result += s + "\n";
        }

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

    public Object visitStreamSpec(Package spec) {
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
        for (Annotation a : func.getAnnotation("NeedsInclude")) {
            preIncludes += a.contents() + "\n";
        }
        String result = indent + "extern ";
        result += convertType(func.getReturnType()) + " ";
        result += escapeCName(func.getName());
        String prefix = null;
        result += doParams(func.getParams(), prefix) + ";\n";
        return result;
    }

}
