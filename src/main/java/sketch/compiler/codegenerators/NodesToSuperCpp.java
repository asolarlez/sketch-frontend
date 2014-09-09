package sketch.compiler.codegenerators;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.Vector;

import sketch.compiler.ast.core.*;
import sketch.compiler.ast.core.Package;
import sketch.compiler.ast.core.exprs.*;
import sketch.compiler.ast.core.exprs.ExprArrayRange.RangeLen;
import sketch.compiler.ast.core.stmts.*;
import sketch.compiler.ast.core.stmts.StmtVarDecl.VarDeclEntry;
import sketch.compiler.ast.core.typs.StructDef;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.ast.core.typs.TypeStructRef;
import sketch.compiler.ast.spmd.exprs.SpmdNProc;
import sketch.compiler.ast.spmd.exprs.SpmdPid;
import sketch.compiler.ast.spmd.stmts.SpmdBarrier;
import sketch.compiler.ast.spmd.stmts.StmtSpmdfork;
import sketch.compiler.codegenerators.tojava.NodesToJava;
import sketch.compiler.parallelEncoder.VarSetReplacer;
import sketch.compiler.stencilSK.VarReplacer;
import sketch.util.Pair;
import sketch.util.datastructures.TypedHashMap;
import sketch.util.wrapper.ScRichString;

import static sketch.util.DebugOut.assertFalse;

public class NodesToSuperCpp extends NodesToJava {

    protected String filename;
    protected boolean isBool = true;
    protected String preStmt = null;
    protected String postBlock = null;
    protected Stack<String> pbStack = new Stack<String>();
    protected boolean hasReturned = false;
    protected boolean addIncludes = true;

    protected String outputCreator(StructDef struct) {
        List<Pair<String, TypeArray>> fl = new ArrayList<Pair<String, TypeArray>>();
        final TypedHashMap<String, Type> fmap = new TypedHashMap<String, Type>();
        {
            StructDef current = struct;
            while (current != null) {
                fmap.putAll(current.getFieldTypMap());
                String parent;
                if ((parent = nres.getStructParentName(current.getName())) != null) {
                    current = nres.getStruct(parent);
                } else {
                    current = null;
                }
            }
        }

        FEReplacer fer = new FEReplacer() {
            public Object visitExprVar(ExprVar ev) {
                if (fmap.containsKey(ev.getName())) {
                    return new ExprVar(ev, ev.getName() + "_");
                } else {
                    return ev;
                }
            }
        };

        String className = escapeCName(struct.getName());

        String result = "";
        int arrayAbove = 0;
        result += indent + className + "* " + className + "::create(";
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
                        if (current.isInstantiable()) {
                            arrayAbove++;
                        }
                        fl.add(new Pair<String, TypeArray>(field, (TypeArray) ftype));
                        result += ", int " + field + "_len";
                    }
                }
            }
            String parent;
            if ((parent = nres.getStructParentName(current.getFullName())) != null) {
                current = nres.getStruct(parent);
            } else {
                current = null;
            }
        }
        result += "){\n";
        addIndent();

        String cinit = "";
        String msize = "";
        String scalarOffset = "0";
        String lastSize = "";
        String lastField = "";
        int cnt = 0;
        int sz = fl.size();
        List<String> offsets = new ArrayList<String>(fl.size());
        current = struct;
        fieldNames = new ArrayList();
        while (current != null) {
            for (String field : current.getOrderedFields()) {
                if (!fieldNames.contains(field)) {
                    fieldNames.add(field);
                    Type ftype = current.getType(field);
                    if (ftype instanceof TypeArray) {
                        ++cnt;
                        TypeArray ta = (TypeArray) ftype;
                        String lenString =
                                (String) ((Expression) ta.getLength().accept(fer)).accept(this);
                        if (cnt < sz) {
                            scalarOffset += "+ sizeof(" + typeForDecl(ftype) + ")";
                        }
                        offsets.add(msize);
                        result +=
                                indent + "int tlen_" + field + " = " + lenString + "; \n";
                        lastSize =
                                " + sizeof(" + typeForDecl(ta.getBase()) + ")*" +
                                        "tlen_" + field;
                        lastField = field;
                        msize += lastSize;
                        continue;
                    } else {
                        String t = " + sizeof(" + typeForDecl(ftype) + ")";
                        scalarOffset += t;
                        cinit += indent + "rv->" + field + " = " + " " + field + "_;\n";
                    }
                }
            }
            String parent;
            if ((parent = nres.getStructParentName(current.getFullName())) != null) {
                current = nres.getStruct(parent);
            } else {
                current = null;
            }
        }

        result +=
                indent + "void* temp= malloc( sizeof(" + className + ")  " + msize +
                        "); \n";

        result += indent + className + "* rv = new (temp)" + className + "();\n";
        result += cinit;

        Iterator<String> offstIt = offsets.iterator();
        cnt = 0;
        for (Pair<String, TypeArray> af : fl) {
            String field = af.getFirst();
            ++cnt;
            if (cnt != arrayAbove) {
                result +=
                        indent + "rv->" + field + "= (" + typeForDecl(af.getSecond()) +
                                ") (((char*)&(rv->" + lastField + "))  " + lastSize +
                                offstIt.next() + "); \n";
            }
            result +=
                    indent + "CopyArr(rv->" + field + ", " + field + "_, tlen_" + field +
                            ", " + field + "_len ); \n";

        }
        String parent = nres.getStructParentName(struct.getFullName());
        if (parent != null) {
            while (nres.getStructParentName(parent) != null) {
                parent = nres.getStructParentName(parent);
            }
            result +=
                    indent + "rv->" + NodesToSuperH.typeVars.get(parent) + "= " +
                            struct.getName().toUpperCase() + "_type;\n";
        }
        result += indent + "return rv;\n";
        unIndent();
        result += indent + "}\n";

        return result;
    }

    public void addPostBlock(String s) {
        if (postBlock == null) {
            postBlock = s;
        } else {
            postBlock += s;
        }
    }

    public void addPreStmt(String s) {
        if (preStmt == null) {
            preStmt = s;
        } else {
            preStmt += s;
        }
    }

    public Object visitStmtBlock(StmtBlock stmt) {

        SymbolTable oldSymTab = symtab;
        symtab = new SymbolTable(symtab);

        // Put context label at the start of the block, too.
        String result = "{";
        if (printSourceLines && stmt != null)
            result += " // " + stmt;
        result += "\n";
        addIndent();
        pbStack.push(postBlock);
        postBlock = null;
        String oldPS = preStmt;
        preStmt = null;
        for (Iterator iter = stmt.getStmts().iterator(); iter.hasNext();) {
            Statement s = (Statement) iter.next();
            String line = indent;
            line += (String) s.accept(this);
            if (!(s instanceof StmtIfThen || s instanceof StmtFor ||
                    s instanceof StmtWhile || s instanceof StmtSwitch))
            {
                line += ";";
            }
            if (preStmt != null) {
                line = preStmt + line;
                preStmt = null;
            }
            if (printSourceLines && s != null)
                line += " // " + s;
            line += "\n";
            result += line;
        }
        if (postBlock != null) {
            if (!hasReturned) {
                result += postBlock;
            }
            postBlock = null;
        }
        preStmt = oldPS;
        postBlock = pbStack.pop();
        unIndent();
        result += indent + "}";
        symtab = oldSymTab;
        return result;
    }

    public Object visitStmtReturn(StmtReturn stmt) {
        String rv = "";
        if (postBlock != null) {
            rv += postBlock;
            postBlock = null;
        }
        for (String s : pbStack) {
            if (s != null) {
                rv += s;
            }
        }
        addPreStmt(rv);
        if (stmt.getValue() == null)
            rv = "return";
        else
            rv = "return " + (String) stmt.getValue().accept(this);
        hasReturned = true;
        return rv;
    }

    public NodesToSuperCpp(TempVarGen varGen, String filename) {
        super(false, varGen);
        this.filename = filename;
    }



    protected String getOpLenStr(Expression exp) {
        String llenString;
        String lls;
        Type latype = getType(exp);
        if (latype instanceof TypeArray) {
            llenString = (String) ((TypeArray) latype).getLength().accept(this);
            lls = (String) exp.accept(this);
        } else {
            String ntmp = newTmp();
            addPreStmt(indent + convertType(latype) + "  " + ntmp + "[1] = {" +
                    exp.accept(this) + "};\n");
            lls = ntmp;
            llenString = "1";
        }
        return lls + ", " + llenString;
    }

    protected String doBinopFun(String pref, ExprBinary exp) {
        return pref + getOpLenStr(exp.getLeft()) + ", " + getOpLenStr(exp.getRight()) +
                ")";
    }

    protected String doArrCompare(ExprBinary exp) {
        return doBinopFun("arrCompare(", exp);
    }

    protected String doUnaryBnot(Expression exp) {
        String nvar = newTmp();
        TypeArray latype = (TypeArray) getType(exp);
        String lenString = (String) latype.getLength().accept(this);
        String typename = typeForDecl(latype.getBase());
        String result =
                indent + "bool* " + nvar + "= new " + typename + "[" + lenString +
                        "]; \n";
        addPreStmt(result);
        addPostBlock(indent + "delete[] " + nvar + ";\n");
        return "bitneg(" + nvar + ", " + lenString + ", " + getOpLenStr(exp) + ")";
    }

    protected String doPlus(ExprBinary exp) {
        String nvar = newTmp();
        TypeArray latype = (TypeArray) getType(exp);
        String lenString = (String) latype.getLength().accept(this);
        String typename = typeForDecl(latype.getBase());
        String result =
                indent + "bool* " + nvar + "= new " + typename + "[" + lenString +
                        "]; \n";
        addPreStmt(result);
        addPostBlock(indent + "delete[] " + nvar + ";\n");

        return doBinopFun("SumArr(" + nvar + ", " + lenString + ", ", exp);
    }

    protected String newTempArray(TypeArray latype, String lenString) {
        String nvar = newTmp();
        String typename = typeForDecl(latype.getBase());
        String result =
                indent + typename + "* " + nvar + "= new " + typename + "[" + lenString +
                        "]; \n";
        addPreStmt(result);
        addPostBlock(indent + "delete[] " + nvar + ";\n");
        return nvar;
    }

    protected String doBitOps(ExprBinary exp) {
        TypeArray latype = (TypeArray) getType(exp);
        String lenString = (String) latype.getLength().accept(this);
        String nvar = newTempArray(latype, lenString);
        String opstr = "";
        switch (exp.getOp()) {
            case ExprBinary.BINOP_BAND:
                opstr = "logical_and<bool>()";
                break;
            case ExprBinary.BINOP_BOR:
                opstr = "logical_or<bool>()";
                break;
            case ExprBinary.BINOP_BXOR:
                opstr = "not_equal_to<bool>()";
                break;
        }
        return doBinopFun("bitwise(" + opstr + ", " + nvar + ", " + lenString + ", ", exp);
    }

    protected String doShifts(ExprBinary exp){
        String nvar = newTmp();
        TypeArray latype = (TypeArray) getType(exp);
        String lenString = (String) latype.getLength().accept(this);
        String typename = typeForDecl(latype.getBase());
        String result =
                indent + typename + "* " + nvar + "= new " + typename + "[" + lenString +
                        "]";
        addPostBlock(indent + "delete[] " + nvar + ";\n");
        String ols = getOpLenStr(exp.getLeft());
        result +=
                "; CopyArr<" + typename + ">(" + nvar + "," + ols + ", " + lenString +
                        "); \n";

        addPreStmt(result);
        if (exp.getOp() == ExprBinary.BINOP_LSHIFT) {
            result = "shL(";
        } else {
            result = "shR(";
        }
        result += nvar + ", " + lenString + ", " + exp.getRight().accept(this) + ")";
        return result;
    }

    public Object visitExprBinary(ExprBinary exp) {
        if (exp.getOp() == ExprBinary.BINOP_LSHIFT ||
                exp.getOp() == ExprBinary.BINOP_RSHIFT)
        {
            if (getType(exp) instanceof TypeArray)
                return doShifts(exp);
        }
        if (exp.getOp() == ExprBinary.BINOP_ADD) {
            if (getType(exp) instanceof TypeArray)
                return doPlus(exp);
        }
        if (exp.getOp() == ExprBinary.BINOP_EQ) {
            if (getType(exp.getLeft()) instanceof TypeArray ||
                    getType(exp.getRight()) instanceof TypeArray)
                return doArrCompare(exp);
        }

        if (exp.getOp() == ExprBinary.BINOP_BAND || exp.getOp() == ExprBinary.BINOP_BOR ||
                exp.getOp() == ExprBinary.BINOP_BXOR)
        {
            if (getType(exp.getLeft()) instanceof TypeArray ||
                    getType(exp.getRight()) instanceof TypeArray)
                return doBitOps(exp);
        }

        StringBuffer result = new StringBuffer();
        String op = null;
        if (binOpLevel > 0)
            result.append("(");
        binOpLevel++;
        Type tmptype = null;
        switch (exp.getOp()) {
            case ExprBinary.BINOP_NEQ:
            case ExprBinary.BINOP_EQ:
                Type t1 = getType(exp.getLeft());
                Type t2 = getType(exp.getRight());
                while (t1 instanceof TypeArray) {
                    t1 = ((TypeArray) t1).getBase();
                }
                while (t2 instanceof TypeArray) {
                    t2 = ((TypeArray) t2).getBase();
                }
                if (t1 == TypePrimitive.inttype || t2 == TypePrimitive.inttype) {
                    tmptype = ctype;
                    ctype = TypePrimitive.inttype;
                } else {
                    tmptype = ctype;
                    ctype = t1;
                }

                break;
            case ExprBinary.BINOP_LT:
            case ExprBinary.BINOP_LE:
            case ExprBinary.BINOP_GT:
            case ExprBinary.BINOP_GE:
                tmptype = ctype;
                ctype = TypePrimitive.inttype;
        }
        String left = (String) exp.getLeft().accept(this);


        String right = (String) exp.getRight().accept(this);
        if (tmptype != null) {
            ctype = tmptype;
        }

        switch (exp.getOp()) {
            case ExprBinary.BINOP_ADD:
                op = "+";
                break;
            case ExprBinary.BINOP_SUB:
                op = "-";
                break;
            case ExprBinary.BINOP_MUL:
                op = "*";
                break;
            case ExprBinary.BINOP_DIV:
                op = "/";
                break;
            case ExprBinary.BINOP_MOD:
                op = "%";
                break;
            case ExprBinary.BINOP_AND:
                op = "&&";
                break;
            case ExprBinary.BINOP_OR:
                op = "||";
                break;
            case ExprBinary.BINOP_EQ:
                op = "==";
                left = "(" + left + ")";
                right = "(" + right + ")";
                break;
            case ExprBinary.BINOP_NEQ:
                op = "!=";
                left = "(" + left + ")";
                right = "(" + right + ")";
                break;
            case ExprBinary.BINOP_LT:
                op = "<";
                left = "(" + left + ")";
                right = "(" + right + ")";
                break;
            case ExprBinary.BINOP_LE:
                op = "<=";
                left = "(" + left + ")";
                right = "(" + right + ")";
                break;
            case ExprBinary.BINOP_GT:
                op = ">";
                left = "(" + left + ")";
                right = "(" + right + ")";
                break;
            case ExprBinary.BINOP_GE:
                op = ">=";
                left = "(" + left + ")";
                right = "(" + right + ")";
                break;
            case ExprBinary.BINOP_BAND:
                op = "&";
                break;
            case ExprBinary.BINOP_BOR:
                op = "|";

                break;
            case ExprBinary.BINOP_BXOR:
                op = "^";

                break;
            case ExprBinary.BINOP_RSHIFT:
                op = " >>";
                left = "(" + left + ")";
                right = "(" + right + ")";
                break;
            // NOTE(JY): Same operator fix here.
            case ExprBinary.BINOP_LSHIFT:
                op = "<<";
                left = "(" + left + ")";
                right = "(" + right + ")";
                break;
            default:
                assert false : exp;
                break;
        }
        result.append(left);
        result.append(" ").append(op).append(" ");
        result.append(right);
        binOpLevel--;
        if (binOpLevel > 0)
            result.append(")");
        return result.toString();
    }

    public Object visitExprConstInt(ExprConstInt exp) {
        return exp.getVal() + "";
    }

    int tcnt = 0;

    String newTmp() {
        return "_tt" + (tcnt++);
    }
    public Object visitExprArrayInit(ExprArrayInit exp) {
        if (exp.getElements().size() == 0) {
            return "NULL";
        }
        TypeArray t = (TypeArray) getType(exp);
        String nvar = newTmp();
        String tmp =
                indent + convertType(t.getBase()) + " " + nvar + "[" +
                        t.getLength().accept(this) + "] = {";
        boolean isFrst = true;
        for (Expression e : exp.getElements()) {
            if (!isFrst) {
                tmp += ", ";
            }
            isFrst = false;
            tmp += e.accept(this);
        }
        tmp += "};\n";

        addPreStmt(tmp);
        return nvar;
    }

    public String getCppName(TypeStructRef tsr) {
        return procName(nres.getStructName(tsr.getName()));
    }

    public String getCppFunName(String name) {
        String s = nres.getFunName(name);
        if (s == null) {
            return name;
        }
        int i = s.indexOf('@');
        if (i < 0) {
            return s;
        }
        String post = s.substring(0, i);
        String pre = s.substring(i + 1);
        pre = escapeCName(pre);
        if (pre.equals(nres.curPkg().getName())) {
            return post;
        }
        return pre + "::" + post;
    }

    public String procName(String s) {
        int i = s.indexOf('@');
        if (i < 0) {
            return s;
        }
        String post = s.substring(0, i);
        String pre = s.substring(i + 1);
        if (pre.equals(nres.curPkg().getName())) {
            return post;
        }
        return pre + "::" + post;
    }

    @Override
    public Object visitExprUnary(ExprUnary exp) {
        if (exp.getOp() == ExprUnary.UNOP_BNOT) {
            if (getType(exp.getExpr()) instanceof TypeArray) {
                return doUnaryBnot(exp.getExpr());
            } else {
                return "~" + exp.getExpr().accept(this);
            }
        }
        if (exp.getOp() == ExprUnary.UNOP_NOT) {
            if (getType(exp.getExpr()) instanceof TypeArray) {
                return doUnaryBnot(exp.getExpr());
            }
        }
        return super.visitExprUnary(exp);
    }


    @Override
    public Object visitProgram(Program prog) {
        nres = new NameResolver(prog);


        String ret = (String) super.visitProgram(prog);
        StringBuffer preamble = new StringBuffer();
        if (addIncludes) {
            preamble.append("#include <cstdio>\n");
            preamble.append("#include <assert.h>\n");
            preamble.append("#include <iostream>\n");
            preamble.append("using namespace std;\n");
            preamble.append("#include \"vops.h\"\n");
            preamble.append("#include \"");
            preamble.append(filename);
            preamble.append(".h\"\n");
        }
        preamble.append(ret);
        // System.out.println(preamble.toString());
        return preamble.toString();
    }

    public String outputStructure(StructDef struct) {
        if (struct.isInstantiable()) {
            return this.outputCreator(struct);
        } else {
            return "";
        }
    }

    public Object visitExprNew(ExprNew en) {

        StructDef struct =
                nres.getStruct(((TypeStructRef) en.getTypeToConstruct()).getName());
        String res =
                this.getCppName((TypeStructRef) en.getTypeToConstruct()) + "::create(";
        Map<String, Expression> pe = new HashMap<String, Expression>();

        for (ExprNamedParam enp : en.getParams()) {
            pe.put(enp.getName(), enp.getExpr());
        }
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
                        res += ", ";
                    }
                    if (ftype instanceof TypeArray) {
                        if (pe.containsKey(field)) {
                            Type tp = getType(pe.get(field));
                            if (tp instanceof TypeArray) {
                                TypeArray t = (TypeArray) tp;
                                res +=
                                        pe.get(field).accept(this) + ", " +
                                                t.getLength().accept(this);
                            } else {
                                TypeArray tarr = (TypeArray) ftype;
                                String nvar = newTmp();
                                String typename = typeForDecl(tarr.getBase());
                                String result =
                                        indent + typename + " " + nvar + "= " +
                                                pe.get(field).accept(this) + ";\n";

                                addPreStmt(result);
                                res += "&" + nvar + ", 1";
                            }
                        } else {
                            res += "NULL, 0";
                        }
                    } else {
                        if (pe.containsKey(field)) {
                            res += pe.get(field).accept(this);
                        } else {
                            res += ftype.defaultValue().accept(this);
                        }
                    }
                }
            }
            String parent;
            if ((parent = nres.getStructParentName(current.getFullName())) != null) {
                current = nres.getStruct(parent);
            } else {
                current = null;
            }
        }
        res += ")";
        return res;
    }

    public Object visitPackage(Package spec) {

        SymbolTable oldSymTab = symtab;
        symtab = new SymbolTable(symtab);
        nres.setPackage(spec);

        String result = "namespace " + spec.getName() + "{\n\n";

        for (Iterator iter = spec.getStructs().iterator(); iter.hasNext();) {
            StructDef struct = (StructDef) iter.next();
            result += outputStructure(struct);
        }

        for (FieldDecl v : spec.getVars()) {
            result += v.accept(this);
        }

        for (Iterator iter = spec.getFuncs().iterator(); iter.hasNext();) {
            Function oldFunc = (Function) iter.next();
            symtab.registerFn(oldFunc);
            result += (String) oldFunc.accept(this);
        }

        symtab = oldSymTab;
        result += "\n}\n";
        return result;
    }

    public String escapeCName(String s) {
        if (s.equals("main")) {
            return "main_c_escape";
        } else if (s.equals("operator")) {
            return "operator_c_escape";
        } else {
            return s;
        }
    }

    public String nativeCode(Function func) {
        assert func.hasAnnotation("Native");
        String rv = "";
        for (Annotation a : func.getAnnotation("Native")) {
            rv += a.contents();
        }
        return rv;
    }

    public Object visitFunction(Function func) {
        SymbolTable oldSymTab = symtab;
        symtab = new SymbolTable(symtab);

        String result = indent;

        Vector<Annotation> va = func.getAnnotation("NeedsInclude");
        if (!va.isEmpty()) {
            result += "}\n";
            for (Annotation a : va) {
                result += a.contents() + "\n";
            }
            result += "namespace " + nres.curPkg().getName() + "{\n";
        }

        result += convertType(func.getReturnType()) + " ";
        result += escapeCName(func.getName());
        String prefix = null;
        result += doParams(func.getParams(), prefix) + " ";
        if (func.hasAnnotation("Native")) {
            result += nativeCode(func);
        } else {
            if (func.isUninterp()) {
                {
                    List<Parameter> l = func.getParams();
                    result += "{ \n";
                    result +=
                            "\t/* This was defined as an uninterpreted function. "
                                    + "\n\t   Add your own body here. */ \n";
                    for (Iterator<Parameter> it = l.iterator(); it.hasNext();) {
                        Parameter p = it.next();
                        if (p.isParameterOutput()) {
                            Statement r =
                                    new StmtAssign(new ExprVar(func,
                                            escapeCName(p.getName())), ExprConstInt.zero);
                            result += "\t" + (String) r.accept(this) + ";\n";
                        }
                    }
                    result += "\n}";
                }
            } else {
                // NOTE(JY): Inserted code to handle the empty body case.
                String body = (String) func.getBody().accept(this);
                if (body.length() == 0) {
                    result += "{}";
                } else {
                    result += body;
                }
            }
        }
        result += "\n";

        symtab = oldSymTab;

        return result;
    }

    public Object visitParameter(Parameter param) {
        Type type = param.getType();
        if (symtab != null) {
            symtab.registerVar(escapeCName(param.getName()), (param.getType()),
                    param, SymbolTable.KIND_FUNC_PARAM);
        }
        String result = typeForParam(type, param.isParameterOutput());
        result += " ";
        result += escapeCName(param.getName());
        if (param.getType() instanceof TypeArray) {
            TypeArray ta = (TypeArray) param.getType();
            result += "/* len = " + ta.getLength().accept(this) + " */";
        }
        return result;
    }

    public String doParams(List params, String prefix) {
        String result = "(";
        boolean first = true;
        for (Iterator iter = params.iterator(); iter.hasNext();) {
            Parameter param = (Parameter) iter.next();

            if (!first)
                result += ", ";
            if (prefix != null)
                result += prefix + " ";
            result += (String) param.accept(this);
            first = false;
        }
        result += ")";
        return result;
    }

    public String typeForParam(Type type, boolean isOutput) {
        return convertType(type) + (isOutput && !(type instanceof TypeArray) ? "&" : "");
    }

    public String typeForDecl(Type type) {
        return convertType(type) + " ";
    }

    @Override
    public Object visitStmtVarDecl(StmtVarDecl stmt) {
        Vector<String> decls = new Vector<String>();
        for (VarDeclEntry decl : stmt) {
            symtab.registerVar(escapeCName(decl.getName()), (decl.getType()),
                    stmt, SymbolTable.KIND_LOCAL);
            Type type = decl.getType();
            if (type instanceof TypeArray) {
                if (!((TypeArray) type).getBase().equals(TypePrimitive.bittype)) {
                    isBool = false;
                }
            }
            String result = typeForDecl(type);
            boolean oldIsBool = this.isBool;
            setCtype(type);
            Expression rhs;
            Type rhsType;
            if (decl.getInit() != null) {
                rhs = peelUnusedCast(decl.getInit());
                rhsType = getType(rhs);
            } else {

                rhs = type.defaultValue();
                rhsType = getType(rhs);
            }
            result +=
                    " " +
                            processAssign(decl.getVarRefToName(stmt), rhs, type, rhsType,
                                    "=", true);
            decls.add(result);
            this.isBool = oldIsBool;
        }
        return (new ScRichString(";\n" + indent)).join(decls);
    }

    public Object visitExprFunCall(ExprFunCall exp) {
        final String funName = exp.getName();
        String result = "";
        String name = getCppFunName(funName);
        result = name + "(";
        boolean first = true;
        Iterator<Expression> actuals = exp.getParams().iterator();

        Map<String, Expression> rmap = new HashMap<String, Expression>();
        VarSetReplacer vsr = new VarSetReplacer(rmap);

        Function f = nres.getFun(funName);
        for (Parameter p : f.getParams()) {
            if (!first)
                result += ", ";
            first = false;

            Expression actual =  actuals.next();
            Type parType = (Type) p.getType().accept(vsr);
            parType = parType.addDefaultPkg(f.getPkg(), nres);
            Type actType = getType(actual);
            rmap.put(p.getName(), actual);
            String partxt = (String) actual.accept(this);
            if (parType instanceof TypeArray) {
                if (!parType.equals(actType) && !p.isParameterOutput()) {
                    TypeArray latype = (TypeArray) parType;
                    String lenString = (String) latype.getLength().accept(this);
                    String nvar = newTempArray(latype, lenString);
                    String t =
                            "CopyArr<" + convertType(latype.getBase()) + ">(" + nvar +
                                    "," + partxt + ", " + lenString;
                    if (actType instanceof TypeArray) {
                        TypeArray ratype = (TypeArray) actType;
                        t += ", " + ratype.getLength().accept(this) + ");\n";
                    } else {
                        t += ");\n";
                    }
                    addPreStmt(indent + t);
                    partxt = nvar;
                }
            }
            result += partxt;


        }
        result += ")";
        return result;
    }

    public Object visitStmtSwitch(StmtSwitch stmt) {
        String result = "";
        result += "switch(";
        String var = (String) stmt.getExpr().accept(this);
        result += var;
        String name = ((TypeStructRef) getType(stmt.getExpr())).getName();
        StructDef sd = nres.getStruct(name);
        name = sd.getFullName();
        while (sd.getParentName() != null) {
            name = sd.getParentName();
            sd = nres.getStruct(name);
        }
        result += "->" + NodesToSuperH.typeVars.get(name) + "){\n";
        name = name.split("@")[0];
        for (String c : stmt.getCaseConditions()) {
            // brakects around cases and constants with type.
            if (c != "default") {
                result +=
                        indent + "case " + sd.getPkg() + "::" + name + "::" +
                                c.toUpperCase() + "_type" +
                                ":\n" +
                                indent + indent;
                String newVar = "_" + var;
                while (symtab.hasVar(newVar)) {
                    newVar = "_" + newVar;
                }
                result += "{\n" + indent + indent;
                result +=
                        sd.getPkg() + "::" + c + "* " + newVar + " = (" + sd.getPkg() +
                                "::" + c + "*)  " + var + ";\n";// semicolon
                VarReplacer vr = new VarReplacer(var, newVar);
                SymbolTable oldSymTab = symtab;
                symtab = new SymbolTable(oldSymTab);
                symtab.registerVar(newVar, new TypeStructRef(c, false));
                Statement bodyStatement = (Statement) stmt.getBody(c).accept(vr);
                if (!bodyStatement.isBlock()) {
                    bodyStatement = new StmtBlock(bodyStatement);
                }
                String body = (String) bodyStatement.accept(this);
                int x = body.indexOf("{");
                int y = body.lastIndexOf("}");
                if (x != -1 && y != -1) {
                    result += body.substring(x + 1, y);
                } else {
                    result += body;
                }
                result += "\n" + indent + indent + "break;\n";
                result += indent + "}\n";
                symtab = oldSymTab;
            } else {
                result += indent + c + ":\n" + indent + indent;

                result += "{\n" + indent + indent;
                Statement bodyStatement = stmt.getBody(c);
                if (!bodyStatement.isBlock()) {
                    bodyStatement = new StmtBlock(bodyStatement);
                }
                String body = (String) bodyStatement.accept(this);
                int x = body.indexOf("{");
                int y = body.lastIndexOf("}");

                if (x != -1 && y != -1) {
                    result += body.substring(x + 1, y);
                } else {
                    result += body;
                }
                result += "\n" + indent + indent + "break;\n";
                result += indent + "}\n";
            }

        }
        result += "\n }";

        return result;
    }

    // JY: We need to print the bodies of for loops.
    public Object visitStmtFor(StmtFor stmt) {
        String result = "";
        result += "for (";
        Statement init = stmt.getInit();
        if (init != null) {
            result += (String) init.accept(this);
        }
        result += ";";
        Expression cond = stmt.getCond();
        if (cond != null) {
            result += (String) cond.accept(this);
        }
        result += ";";
        Statement incr = stmt.getIncr();
        if (incr != null) {
            result += (String) incr.accept(this);
        }
        result += ")";
        String body = (String) stmt.getBody().accept(this);
        if (body.length() == 0) {
            result += "{}";
        } else {
            result += body;
        }
        hasReturned = false;
        return result;
    }

    @Override
    public Object visitStmtSpmdfork(StmtSpmdfork stmt) {
        return stmt.getBody().accept(this);
    }

    @Override
    public Object visitSpmdBarrier(SpmdBarrier stmt) {
        return "spmdbarrier();";
    }

    @Override
    public Object visitSpmdPid(SpmdPid stmt) {
        return "spmdpid";
    }

    @Override
    public Object visitSpmdNProc(SpmdNProc spmdnproc) {
        return "spmdnproc";
    }

    public Object visitStmtWhile(StmtWhile sw) {
        Object o = super.visitStmtWhile(sw);
        hasReturned = true;
        return o;
    }

    public Object visitStmtIfThen(StmtIfThen stmt) {
        Object o = super.visitStmtIfThen(stmt);
        hasReturned = true;
        return o;
    }

    public Object visitStmtDoWhile(StmtDoWhile stmt) {
        Object o = super.visitStmtDoWhile(stmt);
        hasReturned = true;
        return o;
    }

    public Object visitStmtLoop(StmtLoop stmt) {
        throw new RuntimeException("You can not have loop constructs in code generation!");
    }

    @Override
    public Object visitStmtAssign(StmtAssign stmt) {
        String op;
        switch (stmt.getOp()) {
            case 0:
                op = " = ";
                break;
            case ExprBinary.BINOP_ADD:
                op = " += ";
                break;
            case ExprBinary.BINOP_SUB:
                op = " -= ";
                break;
            case ExprBinary.BINOP_MUL:
                op = " *= ";
                break;
            case ExprBinary.BINOP_DIV:
                op = " /= ";
                break;
            case ExprBinary.BINOP_BOR:
                op = " |= ";
                break;
            case ExprBinary.BINOP_BAND:
                op = " &= ";
                break;
            case ExprBinary.BINOP_BXOR:
                op = " ^= ";
                break;
            default:
                throw new IllegalStateException(stmt.toString() + " opcode=" +
                        stmt.getOp());
        }
        // Assume both sides are the right type.

        // method also used by StmtVarDecl
        Expression rhs = peelUnusedCast(stmt.getRHS());
        Type rhsType = getType(rhs);
        return processAssign(stmt.getLHS(), rhs, getType(stmt.getLHS()), rhsType, op,
                false);
    }

    int assignType(Expression lhs, Type lhsType, Expression rhs, boolean isInit) {
        if (isInit) {
            addPostBlock(indent + "delete[] " + lhs + ";\n");
            return INIT_ASSIGN;
        }
        return 0;
    }

    final int COPY_ASSIGN = 0;
    final int ALIAS_ASSIGN = 1;
    final int INIT_ASSIGN = 2;

    protected String processAssign(Expression lhs, Expression rhs, Type lhsType,
            Type rhsType, String op, boolean isInit)
    {
        boolean oldIsBool = isBool;
        setCtype(lhsType);

        isLHS = true;
        String lhsStr = (String) lhs.accept(this);
        isLHS = false;
        String rhsStr = (String) rhs.accept(this);

        if (lhsType instanceof TypeArray) {
            TypeArray latype = (TypeArray) lhsType;
            String typename = typeForDecl(latype.getBase());
            int at = assignType(lhs, lhsType, rhs, isInit);
            switch (at) {
                case INIT_ASSIGN: {
                    String lenString = (String) latype.getLength().accept(this);
                    String result = lhsStr + "= new " + typename + "[" + lenString + "]";
                    String rhslen;
                    if (rhsType instanceof TypeArray) {
                        TypeArray ratype = (TypeArray) rhsType;
                        result +=
                                "; CopyArr<" + typename + ">(" + lhsStr + "," + rhsStr +
                                        ", " + lenString + ", " +
                                        ratype.getLength().accept(this) + ")";
                    } else {
                        result +=
                                "; CopyArr<" + typename + ">(" + lhsStr + "," + rhsStr +
                                        ", " + lenString + ")";
                    }

                    return result;
                }
                case COPY_ASSIGN: {
                    String result;
                    if (rhsType instanceof TypeArray) {
                        TypeArray ratype = (TypeArray) rhsType;
                        result =
                                "CopyArr<" + typename + ">(" + lhsStr + "," + rhsStr +
                                        ", " + latype.getLength().accept(this) + ", " +
                                        ratype.getLength().accept(this) + ")";
                    } else {
                        result =
                                "CopyArr<" + typename + ">(" + lhsStr + "," + rhsStr +
                                        ", " + latype.getLength().accept(this) + ")";
                    }
                    return result;
                }
                case ALIAS_ASSIGN: {
                    String result = lhsStr + "=" + rhsStr;
                    return result;
                }
            }
        }

        this.isBool = oldIsBool;

        return lhsStr + op + rhsStr;
    }

    private void setCtype(Type type) {
        ctype = type;

        /*
         * FIXME -- what's going on here??
         */
        while (ctype instanceof TypeArray) {
            ctype = ((TypeArray) ctype).getBase();
        }

        /** generate char arrays for bit vector constants */
        if (type instanceof TypeArray) {
            if (((TypeArray) type).getBase().equals(TypePrimitive.bittype)) {
                isBool = true;
            }
        }
    }

    protected boolean isLHS = false;

    public Object visitExprArrayRange(ExprArrayRange exp) {
        Expression base = exp.getBase();

        {
            RangeLen range = exp.getSelection();
            isLHS = false;
            Type tmptype = ctype;
            ctype = TypePrimitive.inttype;
            String tmp = (String) range.start().accept(this);
            ctype = tmptype;
            isLHS = true;
            if (range.hasLen()) {
                return "(" + base.accept(this) + "+ " + tmp + ")";
            } else {
                String ba = (String) base.accept(this);
                if (ba.equals("NULL")) {
                    return "NULL";
                } else {
                    return "(" + base.accept(this) + "[" + tmp + "])";
                }
            }
        }
    }

    public Object visitExprField(ExprField exp) {
        String result = "";
        result += (String) exp.getLeft().accept(this);
        result += "->";
        result += escapeCName((String) exp.getName());
        return result;
    }

    public Expression peelUnusedCast(Expression exp) {
        if (exp instanceof ExprTypeCast) {
            ExprTypeCast etc = ((ExprTypeCast) exp);
            Expression inner = peelUnusedCast(etc.getExpr());
            if (etc.getType() instanceof TypeArray) {
                return inner;
            } else {
                if (inner == etc.getExpr()) {
                    return exp;
                } else {
                    return new ExprTypeCast(exp, etc.getType(), inner);
                }
            }
        } else {
            return exp;
        }
    }

    public Object visitExprTypeCast(ExprTypeCast exp) {
        String exprInner = (String) exp.getExpr().accept(this);
        Type etype = getType(exp.getExpr());
        if (exp.getType() instanceof TypeArray) {
            // return convertType(exp.getType()) + "(" + exprInner + ")";
            TypeArray latype = (TypeArray) exp.getType();
            String lenString = (String) latype.getLength().accept(this);
            String nvar = newTempArray(latype, lenString);
            String t =
                    "CopyArr<" + convertType(latype.getBase()) + ">(" + nvar + "," +
                            exprInner + ", " + lenString;
            if (etype instanceof TypeArray) {
                TypeArray ratype = (TypeArray) etype;
                t += ", " + ratype.getLength().accept(this) + ");\n";
            } else {
                t += ");\n";
            }
            addPreStmt(indent + t);
            return nvar;
        }

        if (etype instanceof TypeArray) {
            TypeArray ta = (TypeArray) etype;
            if (ta.getBase().equals(TypePrimitive.bittype) &&
                    exp.getType().equals(TypePrimitive.inttype))
            {
                return "bvToInt(" + exprInner + ", " + ta.getLength().accept(this) + ")";
            }
        }
        return "((" + convertType(exp.getType()) + ")(" + exprInner + "))";
    }

    public Object visitStmtAssume(StmtAssume stmt) {
        return "if(!(" + (String) stmt.getCond().accept(this) +
                ")){ throw AssumptionFailedException();  }";
    }

    @Override
    public String convertType(Type type) {
        if (type instanceof TypeStructRef) {

            return getCppName((TypeStructRef) type) + "*";

        } else if (type instanceof TypePrimitive) {
            switch (((TypePrimitive) type).getType()) {
                case TypePrimitive.TYPE_INT8:
                    return "unsigned char";
                case TypePrimitive.TYPE_INT16:
                    return "unsigned short int";
                case TypePrimitive.TYPE_INT32:
                    return "int";
                case TypePrimitive.TYPE_INT64:
                    return "unsigned long long";
                case TypePrimitive.TYPE_BIT:
                    return "bool";
                case TypePrimitive.TYPE_SIGINT:
                    return "int";
                case TypePrimitive.TYPE_NULLPTR:
                    return "void *";
            }

        } else if (type instanceof TypeArray) {
            TypeArray t = (TypeArray) type;
            Type typBase = t.getBase();
            return convertType(typBase) + "*";
        } else {
            assertFalse("unknown type to convert: ", type);
        }

        return super.convertType(type);
    }

    public Object visitExprNullPtr(ExprNullPtr nptr) {
        return "NULL";
    }


}
