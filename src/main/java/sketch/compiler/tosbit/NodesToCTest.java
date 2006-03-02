package streamit.frontend.tosbit;

import java.util.*;

import streamit.frontend.nodes.*;
import streamit.frontend.tojava.NodesToJava;

public class NodesToCTest extends NodesToJava {

	private NodesToC _converter;
	private String filename;
	private StringBuffer output;
	private HashMap<String,Function> fMap;
	private List<String> testFuncs;
	private static final String IN="in";
	private static final String OUTSK="outsk";
	private static final String OUTSP="outsp";
	private static final int NTESTS=100;

	public NodesToCTest(String filename) {
		super(false, new TempVarGen());
		this.filename=filename;
		_converter=new NodesToC(null,filename);
		output=new StringBuffer();
		fMap=new HashMap<String,Function>();
		testFuncs=new ArrayList<String>();
	}
	
	private void writeLine(String s) {
		output.append(indent);
		output.append(s);
		output.append("\n");
	}
	
	@Override
	public Object visitProgram(Program prog)
	{
		writeLine("#include <stdio.h>");
		writeLine("#include <stdlib.h>");
		writeLine("#include <time.h>");
		writeLine("#include \""+filename+".h\"\n");
		super.visitProgram(prog);
		writeLine("int main(void) {");
		addIndent();
		writeLine("srand(time(0));");
		for(Iterator<String> iter=testFuncs.iterator();iter.hasNext();) {
			writeLine(iter.next()+"();");
		}
		writeLine("printf(\"Automated testing passed\");");
		writeLine("return 0;");
		unIndent();
		writeLine("}");
		return output.toString();
	}

	public Object visitStreamSpec(StreamSpec spec){
		ss = spec;
		for (Iterator iter = spec.getFuncs().iterator(); iter.hasNext(); ) {
			Function func = (Function)iter.next();            
			fMap.put(func.getName(),func);
		}
		for (Iterator iter = spec.getFuncs().iterator(); iter.hasNext(); ) {
			Function func = (Function)iter.next();            
			func.accept(this);            
		}
        return null;
	}
	
    private static int getWordsize(Type type)
    {
    	if(type instanceof TypePrimitive) {
    		switch(((TypePrimitive)type).getType()) {
    			case TypePrimitive.TYPE_BIT:
    				return 1;
    			case TypePrimitive.TYPE_INT8:
    				return 8;
    			case TypePrimitive.TYPE_INT16:
    				return 16;
    			case TypePrimitive.TYPE_INT32:
    				return 32;
    			case TypePrimitive.TYPE_INT64:
    				return 64;
    		}
    	}
    	return 0;
    }
    
    private int typeLen(Type t) {
		if(t instanceof TypeArray) {
			TypeArray array=(TypeArray)t;
			return ((ExprConstInt)array.getLength()).getVal();
		}
		else {
			return 1;
		}
    }
    
    private int typeWS(Type t) {
		if(t instanceof TypeArray)
			return typeWS(((TypeArray)t).getBase());
		else
			return getWordsize(t);
    }
    
    private Type baseType(Type t) {
		if(t instanceof TypeArray)
			return baseType(((TypeArray)t).getBase());
    	else
    		return t;
    }
    
    private void declareVar(String name, Type t) {
    	int len=typeLen(t);
    	String line=_converter.convertType(baseType(t));
    	if(len>1) line+="*";
    	line+=" "+name+";";
    	writeLine(line);
    }

    private void initVar(String name, Type t,boolean random) {
    	int len=typeLen(t);
    	int ws=typeWS(t);
    	if(len>1) {
    		writeLine("for(int i=0;i<"+len+";i++) {");
    		addIndent();
    	}
    	String line=name;
    	if(len>1) line+="[i]";
    	if(random) {
	    	line+="=rand()";
	    	for(int s=16;s<ws;s+=16) {
	    		line+="+rand()<<"+s;
	    	}
    	}
    	else
    		line+="=0";
    	line+=";";
    	writeLine(line);
    	if(len>1) {
    		unIndent();
    		writeLine("}");
    	}
    }

    private void doCompare(String name1, String name2, Type t, String fname) {
    	int len=typeLen(t);
    	if(len>1) {
    		writeLine("for(int i=0;i<"+len+";i++) {");
    		addIndent();
    	}
    	String line="if("+name1;
    	if(len>1) line+="[i]";
    	line+="!="+name2;
    	if(len>1) line+="[i]";
    	line+=") {";
    	writeLine(line);
    	addIndent();
    		writeLine("printf(\"Automated testing failed in "+fname+"\\n\");");
writeLine("printf(\"%x %x %x\\n\",in,outsk,outsp);");
    		writeLine("exit(1);");
    	unIndent();
    	writeLine("}");
    	if(len>1) {
    		unIndent();
    		writeLine("}");
    	}
    }
    
	public Object visitFunction(Function func)
    {
		if(func.getSpecification()==null) return null;
		String fname=func.getName()+"Test";
		testFuncs.add(fname);
		Function spec=fMap.get(func.getSpecification());
		writeLine("void "+fname+"() {");
		addIndent();
		List params=func.getParams();
		if(params.size()==2) {
			Type inType=((Parameter) params.get(0)).getType();
			Type outType=((Parameter) params.get(1)).getType();
			declareVar(IN,inType);
			declareVar(OUTSK,outType);
			declareVar(OUTSP,outType);
			writeLine("for(int test=0;test<"+NTESTS+";test++) {");
			addIndent();
			initVar(IN,inType,true);
			initVar(OUTSK,outType,false);
			initVar(OUTSP,outType,false);
			writeLine(func.getName()+"("+IN+","+OUTSK+");");
			writeLine(func.getSpecification()+"("+IN+","+OUTSP+");");
			doCompare(OUTSK,OUTSP,outType,fname);
			unIndent();
			writeLine("}");
		}
		unIndent();
		writeLine("}\n");
		return null;
    }

}
