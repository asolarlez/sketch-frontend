package sketch.compiler.codegenerators;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.Package;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.codegenerators.tojava.NodesToJava;
import sketch.compiler.main.cmdline.SketchOptions;

public class NodesToSuperCTest extends NodesToJava {

    private NodesToSuperCpp _converter;
    private String filename;
    private StringBuffer output;
    // private HashMap<String,Function> fMap;
    private List<String> testFuncs;
    private static final String IN = "in";
    private static final String OUTSK = "outsk";
    private static final String OUTSP = "outsp";
    private static final int NTESTS = 100;
    private final int BND;
    public NodesToSuperCTest(String filename, boolean pythonPrintStatements) {
        super(false, new TempVarGen());
        int ib = SketchOptions.getSingleton().bndOpts.inbits;
        int bnd = 1;
        for (int i = 0; i < ib; ++i) {
            bnd = bnd * 2;
        }
        BND = bnd;
        this.filename = filename;
        _converter = new NodesToSuperCpp(null, filename, pythonPrintStatements);
        output = new StringBuffer();
        testFuncs = new ArrayList<String>();
    }

    protected void writeLine(String s) {
        output.append(indent);
        output.append(s);
        output.append("\n");
    }

    @Override
    public Object visitProgram(Program prog) {
        writeLine("#include <stdio.h>");
        writeLine("#include <stdlib.h>");
        writeLine("#include <time.h>");
        writeLine("#include <iostream>");
        writeLine("#include \"vops.h\"");
        writeLine("#include \"" + filename + ".h\"\n");
        writeLine("using namespace std;\n");
        super.visitProgram(prog);
        writeLine("int main(void) {");
        addIndent();
        writeLine("srand(time(0));");
        for (Iterator<String> iter = testFuncs.iterator(); iter.hasNext();) {
            writeLine(iter.next() + "();");
        }
        writeLine("printf(\"Automated testing passed for " + filename + "\\n\");");
        writeLine("return 0;");
        unIndent();
        writeLine("}");
        return output.toString();
    }

    public Object visitStreamSpec(Package spec) {
        nres.setPackage(spec);
        for (Iterator iter = spec.getFuncs().iterator(); iter.hasNext();) {
            Function func = (Function) iter.next();
            func.accept(this);
        }
        return null;
    }

    private static int getWordsize(Type type) {
        if (type instanceof TypePrimitive) {
            switch (((TypePrimitive) type).getType()) {
                case TypePrimitive.TYPE_BIT:
                    return 16;
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

    private static boolean isBitType(Type t) {
        return t instanceof TypePrimitive &&
                ((TypePrimitive) t).getType() == TypePrimitive.TYPE_BIT;
    }

    private int getBitLength(TypeArray a) {
        Type base = a.getBase();
        assert (isBitType(base));
        Expression lenExp = a.getLength();
        return ((ExprConstInt) lenExp).getVal();
    }

    private String typeLen(Type t) {
        if (t instanceof TypeArray) {
            TypeArray array = (TypeArray) t;
            return (String) (array.getLength()).accept(_converter);
        } else {
            return "1";
        }
    }

    private boolean typeIsArr(Type t) {
        return (t instanceof TypeArray);
    }

    private int typeWS(Type t) {
        if (t instanceof TypeArray) {
            TypeArray array = (TypeArray) t;
            /*
             * if (isBitType(array.getBase())) { int len = getBitLength(array); if (len <=
             * 8) return 8; else if (len <= 16) return 16; else if (len <= 32) return 32;
             * // else if(len<=64) // return 64; else return 32; } else
             */
                return typeWS(array.getBase());
        } else {
            int q = getWordsize(t);
            if(q>16){
                q = 16;
            }
            return q;
        }
    }

    private int paddingBits(Type t) {
        if (!(t instanceof TypeArray))
            return 0;
        TypeArray array = (TypeArray) t;
        if (!isBitType(array.getBase()))
            return 0;
        int len = getBitLength(array);
        int ll = array.getLength().getIValue();
        return ll * typeWS(t) - len;
    }

    private Type baseType(Type t) {
        if (t instanceof TypeArray)
            return baseType(((TypeArray) t).getBase());
        else
            return t;
    }

    private String translateType(Type t) {
        int ws = typeWS(t);
        String ret = "unsigned ";
        switch (ws) {
            case 64:
                return ret + "long long";
            case 32:
                return ret + "int";
            case 16:
                return ret + "short";
            default:
                return ret + "char";
        }
    }

    private void declareVar(String name, Type t) {

        String line = _converter.typeForDecl(t) + " " + name;
        if (t instanceof TypeArray) {
            TypeArray ta = (TypeArray) t;
            String alen = (String) ta.getLength().accept(_converter);
            line = "if(" + alen + "==0){ continue; }\n" + indent + line;
            line +=
                    "= new " + _converter.typeForDecl(ta.getBase()) + "[" +
                            alen + "]";
        }
        /*
         * int len=typeLen(t); boolean isArr = typeIsArr(t); String line=translateType(t);
         * line+=" "+name; if(isArr) line+="["+len+"]";
         */
        line += ";";
        writeLine(line);
    }

    private void padVar(String name, Type t) {
        String len = typeLen(t);
        boolean isArr = typeIsArr(t);
        int ws = typeWS(t);
        int pad = paddingBits(t);
        if (pad > 0) {
            String line = name;
            if (isArr)
                line += "[" + len + "- 1]";
            line += "&=((1<<" + (ws - pad) + ")-1);";
            writeLine(line);
        }
    }

    private void copyVar(String namel, String namer, Type t) {
        String len = typeLen(t);
        boolean isArr = typeIsArr(t);

        if (isArr) {
            writeLine("CopyArr(" + namel + ", " + namer + ", " + len + ", " + len + ");");
        } else {
            writeLine(namel + "=" + namer + ";");
        }
    }

    private void initVar(String name, Type t, boolean random) {
        String len = typeLen(t);
        boolean isArr = typeIsArr(t);

        if (isArr) {
            writeLine("for(int _i_=0;_i_<" + len + ";_i_++) {");
            addIndent();
        }
        String line = name;
        if (isArr)
            line += "[_i_]";
        if (random) {
            if (isIntType(t)) {
                int ws = typeWS(t);
                line += "=abs(rand()";
                for (int s = 16; s < ws; s += 16) {
                    line += "+(rand()<<" + s + ")";
                }
                line += ") % " + BND;
            } else {
                line += "=((rand()%2) == 1)";
                /*
                 * for (int s = 16; s < ws; s += 16) { line += "+(rand()<<" + s + ")"; }
                 */
            }
        } else
            line += "=0U";
        line += ";";
        writeLine(line);

        if (isArr) {
            unIndent();
            writeLine("}");
        }
        // padVar(name, t);
    }

    private boolean isIntType(Type t) {
        if (t instanceof TypePrimitive) {
            return t.equals(TypePrimitive.inttype);
        }
        if (t instanceof TypeArray) {
            return isIntType(((TypeArray) t).getBase());
        }
        return false;
    }

    private void outputVar(String name, Type t) {

        if (t instanceof TypeArray) {
            writeLine("cout<<\"" + name + " = \"<<printArr(" + name + ", " + typeLen(t) +
                    ")<<endl;");
        } else {
            writeLine("cout<<\"" + name + " = \"<<" + name + "<<endl;");
        }

        /*
         * boolean isArr = typeIsArr(t); if( baseType(t).equals(TypePrimitive.bittype) ){
         * writeLine("cout<<\"" + name + " = \"<<" + name + "<<endl;"); return; } int
         * len=typeLen(t); int ws=typeWS(t); writeLine("printf(\"%5s=\",\""+name+"\");");
         * if(isArr) { writeLine("for(int z=0;z<"+len+";z++) {"); addIndent(); } String
         * line="printf(\"%0"+(ws/4)+"x\","+name; if(isArr) line+="[z]"; line+=");";
         * writeLine(line); if(isArr) { unIndent(); writeLine("}"); }
         * writeLine("printf(\"\\n\");");
         */
    }

    private void doCompare(String name1, String name2, Type t, String fname,
            List<Parameter> inPars, Parameter outPar)
    {
        String len = typeLen(t);
        boolean isArr = typeIsArr(t);
        if (isArr) {
            writeLine("for(int _i_=0;_i_<" + len + ";_i_++) {");
            addIndent();
        }
        String line = "if(" + name1;
        if (isArr)
            line += "[_i_]";
        line += "!=" + name2;
        if (isArr)
            line += "[_i_]";
        line += ") {";
        writeLine(line);
        addIndent();
        writeLine("printf(\"Automated testing failed in " + fname + "\\n\");");
        for (int i = 0; i < inPars.size(); i++)
            outputVar(inPars.get(i).getName(), inPars.get(i).getType());
        outputVar(name1, outPar.getType());
        outputVar(name2, outPar.getType());
        writeLine("exit(1);");
        unIndent();
        writeLine("}");
        if (isArr) {
            unIndent();
            writeLine("}");
        }
    }

    public String remColon(String s) {
        return s.replace('@', '_');
    }

    public String makecpp(String s) {
        int i = s.indexOf('@');
        if (i < 0) {
            return s;
        }
        String post = s.substring(0, i);
        String pre = s.substring(i + 1);
        return pre + "::" + post;
    }
    public Object visitFunction(Function func) {
        if (func.getSpecification() == null)
            return null;

        String fname = remColon(nres.getFunName(func)) + "Test";
        testFuncs.add(fname);
        Function spec = nres.getFun(func.getSpecification());
        writeLine("void " + fname + "() {");
        addIndent();
        List<Parameter> paramsList = func.getParams();
        List<Parameter> inPars = new ArrayList<Parameter>();
        List<Parameter> outPars = new ArrayList<Parameter>();
        for (Parameter p : paramsList) {
            if (p.isParameterInput()) {
                inPars.add(p);
            }
            if (p.isParameterOutput()) {
                outPars.add(p);
            }
        }

        writeLine("for(int _test_=0;_test_<" + NTESTS + ";_test_++) {");
        addIndent();
        for (Parameter inPar : inPars) {
            if (!inPar.isParameterOutput()) {
                Type inType = inPar.getType();
                _converter.visitParameter(inPar);
                declareVar(inPar.getName(), inType);
                initVar(inPar.getName(), inType, true);
            }
        }

        for (Parameter outPar : outPars) {
            Type outType = outPar != null ? outPar.getType() : null;
            declareVar(outPar.getName() + "_" + OUTSK, outType);
            declareVar(outPar.getName() + "_" + OUTSP, outType);

            if (outPar.isParameterInput()) {
                declareVar(outPar.getName(), outType);
                initVar(outPar.getName(), outType, true);
                copyVar(outPar.getName() + "_" + OUTSK, outPar.getName(), outType);
                copyVar(outPar.getName() + "_" + OUTSP, outPar.getName(), outType);
            } else {
                initVar(outPar.getName() + "_" + OUTSK, outType, false);
                initVar(outPar.getName() + "_" + OUTSP, outType, false);
            }
        }
        String specInputs = "";
        String skInputs = "";
        for (Parameter param : paramsList) {
            if (specInputs.length() > 0) {
                specInputs += ",";
                skInputs += ",";
            }
            if (!param.isParameterOutput()) {
                specInputs += param.getName();
                skInputs += param.getName();
            } else {
                specInputs += param.getName() + "_" + OUTSP;
                skInputs += param.getName() + "_" + OUTSK;
            }
        }

        writeLine(makecpp(nres.getFunName(func)) + "(" + specInputs + ");");
        writeLine(makecpp(nres.getFunName(func.getSpecification())) + "(" + skInputs +
                ");");

        for (Parameter outPar : outPars) {
            doCompare(outPar.getName() + "_" + OUTSK, outPar.getName() + "_" + OUTSP,
                    outPar.getType(), fname, inPars, outPar);
        }


        for (Parameter inPar : inPars) {
            Type inType = inPar.getType();
            if (inType instanceof TypeArray) {
                writeLine("delete[] " + inPar.getName() + ";\n");
            }
        }
        for (Parameter outPar : outPars) {
            Type outType = outPar.getType();
            if (outType instanceof TypeArray) {
                writeLine("delete[] " + outPar.getName() + "_" + OUTSK + ";\n");
                writeLine("delete[] " + outPar.getName() + "_" + OUTSP + ";\n");
            }
        }

        unIndent();
        writeLine("}");

        unIndent();
        writeLine("}\n");
        return null;
    }

}
