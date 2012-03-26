package sketch.compiler.codegenerators;

import java.util.Iterator;
import java.util.Map.Entry;

import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.StreamSpec;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.ast.core.typs.TypeStruct;

public class NodesToSuperH extends NodesToSuperCpp {

    // private NodesToSuperCpp _converter;
    private String filename;

    public NodesToSuperH(String filename, boolean pythonPrintStatements) {
        super(null, filename, pythonPrintStatements);
        // _converter = new NodesToSuperCpp(null, filename, pythonPrintStatements);
        this.filename = filename;
        this.addIncludes = false;
    }

    public String outputStructure(TypeStruct struct){
        String result = "";
        result += indent + "class " + escapeCName(struct.getName()) + "{\n  public:\n";
        addIndent();
        for (Entry<String, Type> entry : struct) {
            result +=
                    indent + typeForDecl(entry.getValue()) + " " + entry.getKey() + ";\n";
        }
        result += indent + escapeCName(struct.getName()) + "(){\n";
        addIndent();
        for (Entry<String, Type> entry : struct) {
            String init;            
            if(entry.getValue() instanceof TypeArray){
                TypeArray ta = (TypeArray) entry.getValue();
                String typename = typeForDecl(ta.getBase());
                String lenString = (String)ta.getLength().accept(this);
                init = "new " + typename + "[" + lenString + "]";
                init += "; memset(" + entry.getKey() + ", " + ta.getBase().defaultValue().accept(this) 
                + ", sizeof(" + typename + ")*" + lenString + ")";  
            }else{
                init = (String) entry.getValue().defaultValue().accept(this);
            }
            result +=
                    indent + entry.getKey() + " = " + init + ";\n";
        }
        unIndent();
        result += indent + "}\n";
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
        return result;
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
