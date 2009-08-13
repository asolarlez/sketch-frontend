package sketch.compiler.codegenerators;

import java.util.Iterator;

import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.StreamSpec;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeStruct;

public class NodesToH extends NodesToC {

	private NodesToC _converter;
	private String filename; 
	
	public NodesToH(String filename) {
		super(null,filename);
		_converter=new NodesToC(null,filename);
		this.filename=filename;
		this.addIncludes = false;
	}
	
	public String outputStructure(TypeStruct struct){
    	String result = "";
    	result += indent + "class " + struct.getName() + "{\n  public:";
    	addIndent();
    	for (int i = 0; i < struct.getNumFields(); i++)
    	{
    		String name = struct.getField(i);
    		Type type = struct.getType(name);
    		result += indent + typeForDecl(type, name) + ";\n";
    	}
    	unIndent();
    	result += indent + "};\n";
    	return result;
    }
	
	
	@Override
	public Object visitProgram(Program prog)
	{
		String defname=filename.toUpperCase()+"_H";
		defname = defname.replace('.', '_');
		String ret="#ifndef "+defname+"\n";
		ret+="#define "+defname+"\n\n";
		ret+="#include \"bitvec.h\"\n";
		ret+="#include \"fixedarr.h\"\n";
		
		for (Iterator iter = prog.getStructs().iterator(); iter.hasNext(); )
        {
            TypeStruct struct = (TypeStruct)iter.next();
            ret += "class " + struct.getName() + "; \n";            
        }
		
		ret+=super.visitProgram(prog);
		ret+="\n#endif\n";
		return ret;
	}

	public Object visitStreamSpec(StreamSpec spec){
		String result = "";
		ss = spec;
		for (Iterator iter = spec.getFuncs().iterator(); iter.hasNext(); )
		{
			Function oldFunc = (Function)iter.next();            
			result += (String)oldFunc.accept(this);            
		}
		return result;
	}
	
	public Object visitFunction(Function func)
	{
		String result = indent + "extern ";
		result += _converter.convertType(func.getReturnType()) + " ";
		result += func.getName();
		String prefix = null;
		result += _converter.doParams(func.getParams(), prefix) + ";\n";
		return result;
    }

}
