package sketch.compiler.codegenerators;

import java.util.Iterator;
import java.util.Map.Entry;

import sketch.compiler.ast.core.Annotation;
import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.StreamSpec;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.ast.core.typs.TypeStruct;
import sketch.util.datastructures.TypedHashMap;

public class NodesToSuperH extends NodesToSuperCpp {

    // private NodesToSuperCpp _converter;
    private String filename;
    private String preIncludes;

    public NodesToSuperH(String filename, boolean pythonPrintStatements) {
        super(null, filename, pythonPrintStatements);
        // _converter = new NodesToSuperCpp(null, filename, pythonPrintStatements);
        this.filename = filename;
        this.addIncludes = false;
    }

    public String outputStructure(TypeStruct struct){
        String result = "";
        for (Annotation a : struct.getAnnotation("NeedsInclude")) {
            preIncludes += a.contents() + "\n";
        }

        result += indent + "class " + escapeCName(struct.getName()) + "{\n  public:\n";
        addIndent();
        for (Entry<String, Type> entry : struct) {
            result +=
                    indent + typeForDecl(entry.getValue()) + " " + entry.getKey() + ";\n";
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

        result += indent + escapeCName(struct.getName()) + "(";
        boolean first = true;
        for (Entry<String, Type> entry : struct) {
            if (first) {
                first = false;
            } else {
                result += ", ";
            }
            result += indent + typeForDecl(entry.getValue()) + " _" + entry.getKey();
            if (entry.getValue() instanceof TypeArray) {
                result += ", int " + entry.getKey() + "_len";
            }
        }
        result += "){\n";
        addIndent();

        final TypedHashMap<String, Type> fmap = struct.getFieldTypMap();
        FEReplacer fer = new FEReplacer() {
            public Object visitExprVar(ExprVar ev) {
                if (fmap.containsKey(ev.getName())) {
                    return new ExprVar(ev, "_" + ev.getName());
                } else {
                    return ev;
                }
            }
        };

        for (Entry<String, Type> entry : struct) {

            if (entry.getValue() instanceof TypeArray) {
                TypeArray ta = (TypeArray) entry.getValue();
                String typename = typeForDecl(ta.getBase());
                String lenString =
                        (String) ((Expression) ta.getLength().accept(fer)).accept(this);
                result +=
                        indent + entry.getKey() + " = " + "new " + typename + "[" +
                                lenString + "]" + ";\n";
                result +=
                        indent +
                        "CopyArr(" + entry.getKey() + ", _" + entry.getKey() + ", " +
 lenString + ", " + entry.getKey() + "_len ); \n";
            } else {
                result += indent + entry.getKey() + " = " + " _" + entry.getKey() + ";\n";
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
        return result;
    }

    @Override
    public Object visitProgram(Program prog) {
        String defname = filename.toUpperCase() + "_H";
        defname = defname.replace('.', '_');
        String ret = "#ifndef " + defname + "\n";
        ret += "#define " + defname + "\n\n";
        ret += "#include <cstring>\n\n";
        ret += super.visitProgram(prog);
        ret += "\n#endif\n";
        return ret;
    }

    public Object visitStreamSpec(StreamSpec spec) {
        String result = "";
        nres.setPackage(spec);
        preIncludes = "";
        result += "namespace " + spec.getName() + "{\n";
        for (Iterator iter = spec.getStructs().iterator(); iter.hasNext();) {
            TypeStruct struct = (TypeStruct) iter.next();
            result += "class " + escapeCName(struct.getName()) + "; \n";
        }

        for (Iterator iter = spec.getStructs().iterator(); iter.hasNext();) {
            TypeStruct struct = (TypeStruct) iter.next();
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
        String result = indent + "extern ";
        result += convertType(func.getReturnType()) + " ";
        result += escapeCName(func.getName());
        String prefix = null;
        result += doParams(func.getParams(), prefix) + ";\n";
        return result;
    }

}
