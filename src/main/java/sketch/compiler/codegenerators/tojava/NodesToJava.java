/*
 * Copyright 2003 by the Massachusetts Institute of Technology.
 *
 * Permission to use, copy, modify, and distribute this
 * software and its documentation for any purpose and without
 * fee is hereby granted, provided that the above copyright
 * notice appear in all copies and that both that copyright
 * notice and this permission notice appear in supporting
 * documentation, and that the name of M.I.T. not be used in
 * advertising or publicity pertaining to distribution of the
 * software without specific, written prior permission.
 * M.I.T. makes no representations about the suitability of
 * this software for any purpose.  It is provided "as is"
 * without express or implied warranty.
 */

package sketch.compiler.codegenerators.tojava;

import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import sketch.compiler.ast.core.*;
import sketch.compiler.ast.core.exprs.*;
import sketch.compiler.ast.core.exprs.ExprArrayRange.RangeLen;
import sketch.compiler.ast.core.stmts.*;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.ast.core.typs.TypeStruct;
import sketch.compiler.ast.core.typs.TypeStructRef;
import sketch.compiler.ast.cuda.stmts.CudaSyncthreads;
import sketch.compiler.ast.promela.stmts.StmtFork;
import sketch.compiler.ast.promela.stmts.StmtJoin;
import sketch.compiler.passes.lowering.GetExprType;
import sketch.compiler.passes.lowering.SymbolTableVisitor;
import sketch.compiler.passes.streamit_old.SCSimple;
import sketch.compiler.passes.streamit_old.SJDuplicate;
import sketch.compiler.passes.streamit_old.SJRoundRobin;
import sketch.compiler.passes.streamit_old.SJWeightedRR;

/**
 * Traverse a front-end tree and produce Java code.  This uses {@link
 * sketch.compiler.nodes.FEVisitor} directly, without going through
 * an intermediate class such as <code>FEReplacer</code>.  Every
 * method actually returns a String.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id$
 */
public class NodesToJava extends SymbolTableVisitor
{
    protected StreamSpec ss;
    protected Type ctype;

    /**
	 * @param ss The ss to set.
	 */
	public void setSs(StreamSpec ss) {
		this.ss = ss;
	}
	/**
	 * @return Returns the ss.
	 */
	public StreamSpec getSs() {
		return ss;
	}

	/**
	 * Whether or not to annotate every line with "//<sourcefile>:<sourceline>"
	 */
	private boolean printSourceLines;

    // A string consisting of an even number of spaces.
    protected String indent;
    private boolean libraryFormat;
    protected TempVarGen varGen;
    protected int binOpLevel=0;

    public NodesToJava(boolean libraryFormat, TempVarGen varGen) {
    	this(libraryFormat, varGen, false);
    }

    public NodesToJava(boolean libraryFormat, TempVarGen varGen, boolean printSourceLines)
    {
    	super(null);
        this.ss = null;
        this.indent = "";
        this.libraryFormat = libraryFormat;
        this.varGen = varGen;
        this.printSourceLines=printSourceLines;
    }

    // Add two spaces to the indent.
    protected void addIndent()
    {
        indent += "  ";
    }

    // Remove two spaces from the indent.
    protected void unIndent()
    {
        indent = indent.substring(2);
    }

    // Convert a Type to a String.  If visitors weren't so generally
    // useless for other operations involving Types, we'd use one here.
    protected static String _convertType(Type type)
    {
        // This is So Wrong in the greater scheme of things.
        if (type instanceof TypeArray)
        {
            TypeArray array = (TypeArray)type;
            String base = _convertType(array.getBase());
            return base + "[]";
        }
        else if (type instanceof TypeStruct)
	{
	    return ((TypeStruct)type).getName();
	}
	else if (type instanceof TypeStructRef)
        {
	    return ((TypeStructRef)type).getName();
        }
        else if (type instanceof TypePrimitive)
        {
            switch (((TypePrimitive)type).getType())
            {
            case TypePrimitive.TYPE_BOOLEAN: return "boolean";
            case TypePrimitive.TYPE_BIT: return "int";
            case TypePrimitive.TYPE_SIGINT:
            case TypePrimitive.TYPE_INT: return "int";
            case TypePrimitive.TYPE_FLOAT: return "float";
            case TypePrimitive.TYPE_DOUBLE: return "double";
            case TypePrimitive.TYPE_COMPLEX: return "Complex";
            case TypePrimitive.TYPE_VOID: return "void";
            default: assert false : type; return null;
            }
        }
        else
        {
            return type.toString();
        }
    }

    public String convertType(Type type) {
    	return _convertType(type);
    }

    // Do the same conversion, but including array dimensions.
    public String convertTypeFull(Type type)
    {
        if (type instanceof TypeArray)
        {
            TypeArray array = (TypeArray)type;
            return convertTypeFull(array.getBase()) + "[" +
                (String)array.getLength().accept(this) + "]";
        }
        return convertType(type);
    }

    // Get a constructor for some type.
    public String makeConstructor(Type type)
    {
        if (type instanceof TypeArray)
            return "new " + convertTypeFull(type);
        else
            return "new " + convertTypeFull(type) + "()";
    }

    // Get a Java Class object corresponding to a type.
    public String typeToClass(Type t)
    {
        if (t instanceof TypePrimitive)
        {
            switch (((TypePrimitive)t).getType())
            {
            case TypePrimitive.TYPE_BOOLEAN:
                return "Boolean.TYPE";
            case TypePrimitive.TYPE_BIT:
                return "Integer.TYPE";
            case TypePrimitive.TYPE_INT:
                return "Integer.TYPE";
            case TypePrimitive.TYPE_FLOAT:
                return "Float.TYPE";
            case TypePrimitive.TYPE_DOUBLE:
                return "Double.TYPE";
            case TypePrimitive.TYPE_VOID:
                return "Void.TYPE";
            case TypePrimitive.TYPE_COMPLEX:
                return "Complex.class";
            default:
                assert false : t;
                return null;
            }
        }
        else if (t instanceof TypeStruct)
            return ((TypeStruct)t).getName() + ".class";
        else if (t instanceof TypeArray)
            return "(" + makeConstructor(t) + ").getClass()";
        else
        {
            assert false : t;
            return null;
        }
    }

    // Helpers to get function names for stream types.
    public static String pushFunction(StreamType st)
    {
        return annotatedFunction("output.push", st.getOut());
    }

    public static String popFunction(StreamType st)
    {
        return annotatedFunction("input.pop", st.getIn());
    }

    public static String peekFunction(StreamType st)
    {
        return annotatedFunction("input.peek", st.getIn());
    }

    private static String annotatedFunction(String name, Type type)
    {
        String prefix = "", suffix = "";
        // Check for known suffixes:
        if (type instanceof TypePrimitive)
        {
            switch (((TypePrimitive)type).getType())
            {
            case TypePrimitive.TYPE_BOOLEAN:
                suffix = "Boolean";
                break;
            case TypePrimitive.TYPE_BIT:
                suffix = "Int";
                break;
            case TypePrimitive.TYPE_INT:
                suffix = "Int";
                break;
            case TypePrimitive.TYPE_FLOAT:
                suffix = "Float";
                break;
            case TypePrimitive.TYPE_DOUBLE:
                suffix = "Double";
                break;
            case TypePrimitive.TYPE_COMPLEX:
                if (name.startsWith("input"))
                    prefix  = "(Complex)";
                break;
            default:
                assert false : type;
            }
        }
        else if (name.startsWith("input"))
        {
            prefix = "(" + _convertType(type) + ")";
        }
        return prefix + name + suffix;
    }

    // Return a representation of a list of Parameter objects.
    public String doParams(List params, String prefix)
    {
        String result = "(";
        boolean first = true;
        for (Iterator iter = params.iterator(); iter.hasNext(); )
        {
            Parameter param = (Parameter)iter.next();
            symtab.registerVar(param.getName(),
                    actualType(param.getType()),
                    param,
                    SymbolTable.KIND_FUNC_PARAM);
            if (!first) result += ", ";
            if (prefix != null) result += prefix + " ";
            result += convertType(param.getType());
            result += " ";
            result += param.getName();
            first = false;
        }
        result += ")";
        return result;
    }
    // Return a representation of lhs = rhs, with no trailing semicolon.
    public String doAssignment(Expression lhs, Expression rhs,
                               SymbolTable symtab)
    {
        GetExprType eType = new GetExprType(symtab, ss.getStreamType(),
                                            new java.util.HashMap());
        Type lhsType = (Type)lhs.accept(eType);
        // Might want to special-case structures and arrays;
        // ignore for now.
        return lhs.accept(this) + " = " + rhs.accept(this);
    }


    public Object visitExprArrayInit(ExprArrayInit exp)
    {
	StringBuffer sb = new StringBuffer();
	sb.append("{");

	List elems = exp.getElements();
	for (int i=0; i<elems.size(); i++) {
	    sb.append((String)((Expression)elems.get(i)).accept(this));
	    if (i!=elems.size()-1) {
		sb.append(",");
	    }
	    // leave blank line for multi-dim arrays
	    if (exp.getDims()>1) {
		sb.append("\n");
	    }
	}

	sb.append("}");

        return sb.toString();
    }

    public Object visitExprBinary(ExprBinary exp)
    {
        StringBuffer result=new StringBuffer();
        String op = null;
        if(binOpLevel>0) result.append("(");
        binOpLevel++;
        result.append((String)exp.getLeft().accept(this));
        switch (exp.getOp())
        {
	        case ExprBinary.BINOP_ADD: op = "+"; break;
	        case ExprBinary.BINOP_SUB: op = "-"; break;
	        case ExprBinary.BINOP_MUL: op = "*"; break;
	        case ExprBinary.BINOP_DIV: op = "/"; break;
	        case ExprBinary.BINOP_MOD: op = "%"; break;
	        case ExprBinary.BINOP_AND: op = "&&"; break;
	        case ExprBinary.BINOP_OR:  op = "||"; break;
	        case ExprBinary.BINOP_EQ:  op = "=="; break;
	        case ExprBinary.BINOP_NEQ: op = "!="; break;
	        case ExprBinary.BINOP_LT:  op = "<"; break;
	        case ExprBinary.BINOP_LE:  op = "<="; break;
	        case ExprBinary.BINOP_GT:  op = ">"; break;
	        case ExprBinary.BINOP_GE:  op = ">="; break;
	        case ExprBinary.BINOP_BAND:op = "&"; break;
	        case ExprBinary.BINOP_BOR: op = "|"; break;
	        case ExprBinary.BINOP_BXOR:op = "^"; break;
	        case ExprBinary.BINOP_RSHIFT: op = ">>"; break;
	        case ExprBinary.BINOP_LSHIFT: op = "<<"; break;
	        default: assert false : exp; break;
        }
        result.append(" ").append(op).append(" ");
        result.append((String)exp.getRight().accept(this));
        binOpLevel--;
        if(binOpLevel>0) result.append(")");
        return result.toString();
    }

    public Object visitExprComplex(ExprComplex exp)
    {
        // We should never see one of these at this point.
        assert false : exp;
        // If we do, print something vaguely intelligent:
        String r = "";
        String i = "";
        if (exp.getReal() != null) r = (String)exp.getReal().accept(this);
        if (exp.getImag() != null) i = (String)exp.getImag().accept(this);
        return "/* (" + r + ")+i(" + i + ") */";
    }

    public Object visitExprConstBoolean(ExprConstBoolean exp)
    {
        if (exp.getVal())
            return "true";
        else
            return "false";
    }

    public Object visitExprConstChar(ExprConstChar exp)
    {
        return "'" + exp.getVal() + "'";
    }

    public Object visitExprConstFloat(ExprConstFloat exp)
    {
        return Double.toString(exp.getVal()) + "f";
    }

    public Object visitExprConstInt(ExprConstInt exp)
    {
        return Integer.toString(exp.getVal());
    }

    public Object visitExprConstStr(ExprConstStr exp)
    {
        return exp.getVal();
    }

    public Object visitExprLiteral(ExprLiteral exp)
    {
        return exp.getValue();
    }

    public Object visitExprField(ExprField exp)
    {
        String result = "";
        result += (String)exp.getLeft().accept(this);
        result += ".";
        result += (String)exp.getName();
        return result;
    }

    public Object visitExprFunCall(ExprFunCall exp)
    {
	String result;
        String name = exp.getName();
        // Local function?
        if (ss.getFuncNamed(name) != null) {
            result = name + "(";
        }
	// look for print and println statements; assume everything
	// else is a math function
	else if (name.equals("print")) {
	    result = "System.out.println(";
	} else if (name.equals("println")) {
	    result = "System.out.println(";
        } else if (name.equals("super")) {
            result = "super(";
        } else if (name.equals("setDelay")) {
            result = "setDelay(";
        } else if (name.startsWith("enqueue")) {
            result = name + "(";
	} else {
	    // Math.sqrt will return a double, but we're only supporting
	    // float's now, so add a cast to float.  Not sure if this is
	    // the right thing to do for all math functions in all cases?
	    result = "(float)Math." + name + "(";
	}
        boolean first = true;
        for (Iterator iter = exp.getParams().iterator(); iter.hasNext(); )
        {
            Expression param = (Expression)iter.next();
            if (!first) result += ", ";
            first = false;
            result += (String)param.accept(this);
        }
        result += ")";
        return result;
    }

    public Object visitExprNew (ExprNew newe) {
    	// TODO: this may not be "Java clean"
    	return newe.toString ();
    }

    public Object visitExprTernary(ExprTernary exp)
    {
        String a = (String)exp.getA().accept(this);
        String b = (String)exp.getB().accept(this);
        String c = (String)exp.getC().accept(this);
        switch (exp.getOp())
        {
        case ExprTernary.TEROP_COND:
            return "(" + a + " ? " + b + " : " + c + ")";
        default:
            assert false : exp;
            return null;
        }
    }

    public Object visitExprTypeCast(ExprTypeCast exp)
    {
        return "((" + convertType(exp.getType()) + ")(" +
            (String)exp.getExpr().accept(this) + "))";
    }

    public Object visitExprUnary(ExprUnary exp)
    {
        String child = (String)exp.getExpr().accept(this);
        switch(exp.getOp())
        {
        case ExprUnary.UNOP_NOT: return "!" + child;
        case ExprUnary.UNOP_BNOT: return "~" + child;
        case ExprUnary.UNOP_NEG: return "-" + child;
        case ExprUnary.UNOP_PREINC: return "++" + child;
        case ExprUnary.UNOP_POSTINC: return child + "++";
        case ExprUnary.UNOP_PREDEC: return "--" + child;
        case ExprUnary.UNOP_POSTDEC: return child + "--";
        default: assert false : exp; return null;
        }
    }

    public Object visitExprVar(ExprVar exp)
    {
        return exp.getName();
    }

    public Object visitFieldDecl(FieldDecl field)
    {
        // Assume all of the fields have the same type.
        String result = indent + convertType(field.getType(0)) + " ";
        for (int i = 0; i < field.getNumFields(); i++)
        {
        	symtab.registerVar(field.getName(i),
                    actualType(field.getType(i)),
                    field,
                    SymbolTable.KIND_FIELD);
            if (i > 0) result += ", ";
            result += field.getName(i);
            if (field.getInit(i) != null)
                result += " = " + (String)field.getInit(i).accept(this);
        }
        result += ";";
        if (printSourceLines && field != null)
            result += " // " + field;
        result += "\n";
        return result;
    }

    public Object visitFunction(Function func)
    {
    	SymbolTable oldSymTab = symtab;
        symtab = new SymbolTable(symtab);

        String result = indent + "public ";
        if (!func.getName().equals(ss.getName()))
            result += convertType(func.getReturnType()) + " ";
        result += func.getName();
        String prefix = null;
        
        result += doParams(func.getParams(), prefix) + " ";
        result += (String)func.getBody().accept(this);
        result += "\n";
        symtab = oldSymTab;
        return result;
    }



    public String outputStructure(TypeStruct struct){
    	String result = "";
    	result += indent + "class " + struct.getName() +
        " extends Structure {\n";
    	addIndent();
        for (Entry<String, Type> entry : struct) {
            result +=
                    indent + convertType(entry.getValue()) + " " + entry.getKey() + ";\n";
        }
    	unIndent();
    	result += indent + "}\n";
    	return result;
    }


    public Object visitProgram(Program prog)
    {
        // Nothing special here either.  Just accumulate all of the
        // structures and streams.
        String result = "";
        for (Iterator iter = prog.getStructs().iterator(); iter.hasNext(); )
        {
            TypeStruct struct = (TypeStruct)iter.next();
            result += outputStructure(struct);
            structsByName.put(struct.getName(), struct);
        }
        for (Iterator iter = prog.getStreams().iterator(); iter.hasNext(); )
            result += (String)((StreamSpec)iter.next()).accept(this);
        return result;
    }


    public Object visitSCSimple(SCSimple creator)
    {
        String result;
        if (libraryFormat)
        {
            // Magic for builtins.
            if (creator.getName().equals("Identity") ||
                creator.getName().equals("FileReader") ||
                creator.getName().equals("FileWriter"))
                result = "new " + creator.getName() + "(";
            else
                result = creator.getName() + ".__construct(";
        }
        else
            result = "new " + creator.getName() + "(";
        boolean first = true;
        for (Iterator iter = creator.getParams().iterator(); iter.hasNext(); )
        {
            Expression param = (Expression)iter.next();
            if (!first) result += ", ";
            result += (String)param.accept(this);
            first = false;
        }
        for (Iterator iter = creator.getTypes().iterator(); iter.hasNext(); )
        {
            Type type = (Type)iter.next();
            if (!first) result += ", ";
            result += typeToClass(type);
            first = false;
        }
        result += ")";
        return result;
    }

    public Object visitSJDuplicate(SJDuplicate sj)
    {
    	switch(sj.getType()){
    		case SJDuplicate.DUP:
    			return "DUPLICATE()";
    		case SJDuplicate.XOR:
    			return "XOR()";
    		case SJDuplicate.OR:
    			return "OR()";
    		case SJDuplicate.AND:
    			return "AND()";
    		default:
    			return "DUPLICATE()";
    	}
    }

    public Object visitSJRoundRobin(SJRoundRobin sj)
    {
        return "ROUND_ROBIN(" + (String)sj.getWeight().accept(this) + ")";
    }

    public Object visitSJWeightedRR(SJWeightedRR sj)
    {
        String result = "WEIGHTED_ROUND_ROBIN(";
        boolean first = true;
        for (Iterator iter = sj.getWeights().iterator(); iter.hasNext(); )
        {
            Expression weight = (Expression)iter.next();
            if (!first) result += ", ";
            result += (String)weight.accept(this);
            first = false;
        }
        result += ")";
        return result;
    }

    public Object doStreamCreator(String how, StreamCreator sc)
    {
        // If the stream creator involves registering with a portal,
        // we need a temporary variable.
        List portals = sc.getPortals();
        if (portals.isEmpty())
            return how + "(" + (String)sc.accept(this) + ")";
        String tempVar = varGen.nextVar();
        // Need run-time type of the creator.  Assert that only
        // named streams can be added to portals.
        SCSimple scsimple = (SCSimple)sc;
        String result = scsimple.getName() + " " + tempVar + " = " +
            (String)sc.accept(this);
        result += ";\n" + indent + how + "(" + tempVar + ")";
        for (Iterator iter = portals.iterator(); iter.hasNext(); )
        {
            Expression portal = (Expression)iter.next();
            result += ";\n" + indent + (String)portal.accept(this) +
                ".regReceiver(" + tempVar + ")";
        }
        return result;
    }

    public Object visitStmtAdd(StmtAdd stmt)
    {
        return doStreamCreator("add", stmt.getCreator());
    }

    public Object visitStmtAssign(StmtAssign stmt)
    {
        String op;
        switch(stmt.getOp())
        {
        case ExprBinary.BINOP_ADD: op = " += "; break;
        case ExprBinary.BINOP_SUB: op = " -= "; break;
        case ExprBinary.BINOP_MUL: op = " *= "; break;
        case ExprBinary.BINOP_DIV: op = " /= "; break;
        case 0: op = " = "; break;
        default: assert false: stmt; op = " = "; break;
        }
        // Assume both sides are the right type.
        return (String)stmt.getLHS().accept(this) + op +
            (String)stmt.getRHS().accept(this);
    }

    public Object visitStmtBlock(StmtBlock stmt)
    {

    	SymbolTable oldSymTab = symtab;
        symtab = new SymbolTable(symtab);

        // Put context label at the start of the block, too.
        String result = "{";
        if (printSourceLines && stmt != null)
            result += " // " + stmt;
        result += "\n";
        addIndent();
        for (Iterator iter = stmt.getStmts().iterator(); iter.hasNext(); )
        {
            Statement s = (Statement)iter.next();
            String line = indent;
            line += (String)s.accept(this);
		    if(!(s instanceof StmtIfThen ||
		         s instanceof StmtFor ||
		         s instanceof StmtWhile)) {
		    	line += ";";
		    }
            if (printSourceLines && s != null)
                line += " // " + s;
            line += "\n";
            result += line;
        }
        unIndent();
        result += indent + "}";
        symtab = oldSymTab;
        return result;
    }

    public Object visitStmtBody(StmtBody stmt)
    {
        return doStreamCreator("setBody", stmt.getCreator());
    }

    public Object visitStmtBreak(StmtBreak stmt)
    {
        return "break";
    }

    public Object visitStmtContinue(StmtContinue stmt)
    {
        return "continue";
    }

    public Object visitStmtDoWhile(StmtDoWhile stmt)
    {
        String result = "do ";
        result += (String)stmt.getBody().accept(this);
        result += "while (" + (String)stmt.getCond().accept(this) + ")";
        return result;
    }

    public Object visitStmtEmpty(StmtEmpty stmt)
    {
        return "";
    }

    public Object visitStmtExpr(StmtExpr stmt)
    {
        String result = (String)stmt.getExpression().accept(this);
        // Gross hack to strip out leading class casts,
        // since they'll illegal (JLS 14.8).
        if (result.charAt(0) == '(' &&
            Character.isUpperCase(result.charAt(1)))
            result = result.substring(result.indexOf(')') + 1);
        return result;
    }

    public Object visitStmtFor(StmtFor stmt)
    {
        String result = "for (";
        if (stmt.getInit() != null)
            result += (String)stmt.getInit().accept(this);
        result += "; ";
        if (stmt.getCond() != null){
        	ctype = TypePrimitive.bittype;
            result += (String)stmt.getCond().accept(this);
        }
        result += "; ";
        if (stmt.getIncr() != null)
            result += (String)stmt.getIncr().accept(this);
        result += ") ";
        result += (String)stmt.getBody().accept(this);
        return result;
    }

    public Object visitStmtIfThen(StmtIfThen stmt)
    {
        // must have an if part...
        assert stmt.getCond() != null;
        ctype = TypePrimitive.bittype;
        String result = "if (" + (String)stmt.getCond().accept(this) + ") ";
        if(stmt.getCons() == null){
        	result += "{}\n";
        }else{
        	result += (String)stmt.getCons().accept(this);
        	 if(! (stmt.getCons() instanceof StmtBlock ) ){
             	result += ";";
             }
        }
        if (stmt.getAlt() != null){
            result += " else " + (String)stmt.getAlt().accept(this);
            if(! (stmt.getAlt() instanceof StmtBlock ) ){
            	result += ";";
            }
        }
        return result;
    }

    public Object visitStmtJoin(StmtJoin stmt)
    {
        assert stmt.getJoiner() != null;
        return "setJoiner(" + (String)stmt.getJoiner().accept(this) + ")";
    }

    public Object visitStmtLoop(StmtLoop stmt)
    {
    	throw new UnsupportedOperationException();
//        assert stmt.getCreator() != null;
//        return doStreamCreator("setLoop", stmt.getCreator());
    }

    public Object visitStmtReturn(StmtReturn stmt)
    {
        if (stmt.getValue() == null) return "return";
        return "return " + (String)stmt.getValue().accept(this);
    }

    public Object visitStmtAssert(StmtAssert stmt)
    {
        return "assert (" + (String)stmt.getCond().accept(this) + ")";
    }

    public Object visitStmtVarDecl(StmtVarDecl stmt)
    {


        String result = "";
        // Hack: if the first variable name begins with "_final_", the
        // variable declaration should be final.
        if (stmt.getName(0).startsWith("_final_"))
            result += "final ";
        result += convertType(stmt.getType(0)) + " ";
        for (int i = 0; i < stmt.getNumVars(); i++)
        {
        	symtab.registerVar(stmt.getName(i),
                    actualType(stmt.getType(i)),
                    stmt,
                    SymbolTable.KIND_LOCAL);
            if (i > 0)
                result += ", ";
            result += stmt.getName(i);
            if (stmt.getInit(i) != null)
                result += " = " + (String)stmt.getInit(i).accept(this);
        }
        return result;
    }

    public Object visitStmtWhile(StmtWhile stmt)
    {
        assert stmt.getCond() != null;
        assert stmt.getBody() != null;
        return "while (" + (String)stmt.getCond().accept(this) +
            ") " + (String)stmt.getBody().accept(this);
    }

    public Object visitStreamSpec(StreamSpec spec)
    {

        String result = "";
        // At this point we get to ignore wholesale the stream type, except
        // that we want to save it.
        StreamSpec oldSS = ss;
        ss = spec;

        // Output field definitions:
        for (Iterator iter = spec.getVars().iterator(); iter.hasNext(); )
        {
            FieldDecl varDecl = (FieldDecl)iter.next();
            result += (String)varDecl.accept(this);
        }

        // Output method definitions:
        for (Iterator iter = spec.getFuncs().iterator(); iter.hasNext(); ){
        	Function func = (Function)iter.next();
		    symtab.registerFn(func);
            result += (String)((func).accept(this));
        }

        ss = oldSS;
        unIndent();
        result += "}\n";
        return result;
    }

    public Object visitStreamType(StreamType type)
    {
        // Nothing to do here.
        return "";
    }

    public Object visitOther(FENode node)
    {
        if (node instanceof ExprJavaConstructor)
        {
            ExprJavaConstructor jc = (ExprJavaConstructor)node;
            return makeConstructor(jc.getType());
        }
        if (node instanceof StmtIODecl)
        {
            StmtIODecl io = (StmtIODecl)node;
            String result = io.getName() + " = new Channel(" +
                typeToClass(io.getType()) + ", " +
                (String)io.getRate1().accept(this);
            if (io.getRate2() != null)
                result += ", " + (String)io.getRate2().accept(this);
            result += ")";
            return result;
        }
        if (node instanceof StmtSetTypes)
        {
            StmtSetTypes sst = (StmtSetTypes)node;
            return "setIOTypes(" + typeToClass(sst.getInType()) +
                ", " + typeToClass(sst.getOutType()) + ")";
        }
        else
        {
            assert false : node;
            return "";
        }
    }

	public Object visitExprStar(ExprStar star) {
		//throw new UnsupportedOperationException();
		return null;
	}

	public Object visitStmtFork(StmtFork loop){
		throw new UnsupportedOperationException();
	}

	public Object visitExprArrayRange(ExprArrayRange exp) {
		Expression base=exp.getBase();		
		{
			RangeLen range=exp.getSelection();
			if(!range.hasLen()) {
				return base.accept(this)+"["+range.start().accept(this)+"]";
			}else{
				return base.accept(this)+"["+range.start().accept(this)+"::" + range.getLenExpression() + "]";
			}
		}
		
	}
	public Object visitType(Type t) { return null; }
    public Object visitTypePrimitive(TypePrimitive t) { return null; }
    public Object visitTypeArray(TypeArray t) { return null; }
    public Object visitParameter(Parameter par){ return null; }
    public Object visitTypeStruct(TypeStruct ts){return null;}
    public Object visitStmtReorderBlock(StmtReorderBlock block){return null;}
    public Object visitStmtAtomicBlock(StmtAtomicBlock block){return null;}
    public Object visitExprNullPtr(ExprNullPtr nptr){ return "null"; }

    public Object visitCudaSyncthreads(CudaSyncthreads cudaSyncthreads) {
        return "__syncthreads()";
    }
    
    @Override
    public Object visitStmtMinimize(StmtMinimize stmtMinimize) {
        return "minimize(" + stmtMinimize.getMinimizeExpr().accept(this) + ")";
    }
}
