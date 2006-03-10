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
    
    private static boolean isBitType(Type t)
    {
    	return t instanceof TypePrimitive && ((TypePrimitive)t).getType()==TypePrimitive.TYPE_BIT;
    }

    private int getBitLength(TypeArray a) 
    {
		Type base=a.getBase();
		assert(isBitType(base));
		Expression lenExp=a.getLength();
		return ((ExprConstInt)lenExp).getVal();
    }
    
    private int typeLen(Type t) {
		if(t instanceof TypeArray) {
			TypeArray array=(TypeArray)t;
			if(isBitType(array.getBase())) {
				int len=getBitLength(array);
				if(len<=64) return 1;
				else return (len-1)/32+1;
			}
			else
				return ((ExprConstInt)array.getLength()).getVal();
		}
		else {
			return 1;
		}
    }
    
    private int typeWS(Type t) {
		if(t instanceof TypeArray) {
			TypeArray array=(TypeArray)t;
			if(isBitType(array.getBase())) {
				int len=getBitLength(array);
				if(len<=8)
					return 8;
				else if(len<=16)
					return 16;
				else if(len<=32)
					return 32;
				else if(len<=64)
					return 64;
				else
					return 32;
			}
			else
				return typeWS(array.getBase());
		}
		else
			return getWordsize(t);
    }
    
    private int paddingBits(Type t) {
    	if(!(t instanceof TypeArray)) return 0;
		TypeArray array=(TypeArray)t;
    	if(!isBitType(array.getBase())) return 0;
		int len=getBitLength(array);
    	return typeLen(t)*typeWS(t)-len;
    }
    
    private Type baseType(Type t) {
		if(t instanceof TypeArray)
			return baseType(((TypeArray)t).getBase());
    	else
    		return t;
    }
    
    private String translateType(Type t) {
    	int ws=typeWS(t);
    	String ret="unsigned ";
    	switch(ws) {
    		case 64: return ret+"long long";
    		case 32: return ret+"int";
    		case 16: return ret+"short";
    		default: return ret+"char";
    	}
    }
    
    private void declareVar(String name, Type t) {
    	int len=typeLen(t);
    	String line=translateType(t);
    	line+=" "+name;
    	if(len>1) line+="["+len+"]";
    	line+=";";
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
	    		line+="+(rand()<<"+s+")";
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
    	int pad=paddingBits(t);
    	if(pad>0) {
    		line=name;
    		if(len>1) line+="["+(len-1)+"]";
    		line+="&=((1<<"+(ws-pad)+")-1);";
    		writeLine(line);
    	}
    }

    private void outputVar(String name, Type t) 
    {
    	int len=typeLen(t);
    	int ws=typeWS(t);
    	writeLine("printf(\"%5s=\",\""+name+"\");");
    	if(len>1) {
    		writeLine("for(int z=0;z<"+len+";z++) {");
    		addIndent();
    	}
    	String line="printf(\"%0"+(ws/4)+"x\","+name;
    	if(len>1) line+="[z]";
   		line+=");";
    	writeLine(line);
    	if(len>1) {
    		unIndent();
    		writeLine("}");
    	}
    	writeLine("printf(\"\\n\");");
    }
    
    private void doCompare(String name1, String name2, Type t, String fname, List<Parameter> inPars,Parameter outPar) {
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
    		for(int i=0;i<inPars.size();i++)
    			outputVar(IN+i,inPars.get(i).getType());
			outputVar(OUTSK,outPar.getType());
			outputVar(OUTSP,outPar.getType());
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
		List<Parameter> paramsList=func.getParams();
		List<Parameter> inPars=new ArrayList<Parameter>();
		Parameter outPar=paramsList.get(paramsList.size()-1);
		for(int i=0;i<paramsList.size()-1;i++)
			inPars.add(paramsList.get(i));
		
		for(int i=0;i<inPars.size();i++) {
			Type inType=inPars.get(0).getType();
			declareVar(IN+i,inType);
		}
		Type outType=outPar.getType();
		declareVar(OUTSK,outType);
		declareVar(OUTSP,outType);
		writeLine("for(int _test_=0;_test_<"+NTESTS+";_test_++) {");
		addIndent();
		for(int i=0;i<inPars.size();i++) {
			Type inType=inPars.get(0).getType();
			initVar(IN+i,inType,true);
		}
		initVar(OUTSK,outType,false);
		initVar(OUTSP,outType,false);
		String strInputs="";
		for(int i=0;i<inPars.size();i++) strInputs+=IN+i+",";
		writeLine(func.getName()+"("+strInputs+OUTSK+");");
		writeLine(func.getSpecification()+"("+strInputs+OUTSP+");");
		doCompare(OUTSK,OUTSP,outType,fname,inPars,outPar);
		unIndent();
		writeLine("}");

		unIndent();
		writeLine("}\n");
		return null;
    }

}
