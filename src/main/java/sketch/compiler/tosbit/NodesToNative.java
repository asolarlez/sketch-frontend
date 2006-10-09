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

package streamit.frontend.tosbit;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import streamit.frontend.nodes.ExprArrayInit;
import streamit.frontend.nodes.ExprComplex;
import streamit.frontend.nodes.ExprConstBoolean;
import streamit.frontend.nodes.ExprField;
import streamit.frontend.nodes.ExprFunCall;
import streamit.frontend.nodes.ExprPeek;
import streamit.frontend.nodes.ExprPop;
import streamit.frontend.nodes.ExprTypeCast;
import streamit.frontend.nodes.ExprVar;
import streamit.frontend.nodes.Expression;
import streamit.frontend.nodes.FENode;
import streamit.frontend.nodes.FieldDecl;
import streamit.frontend.nodes.FuncWork;
import streamit.frontend.nodes.Function;
import streamit.frontend.nodes.Parameter;
import streamit.frontend.nodes.Program;
import streamit.frontend.nodes.SJDuplicate;
import streamit.frontend.nodes.SJRoundRobin;
import streamit.frontend.nodes.SJWeightedRR;
import streamit.frontend.nodes.Statement;
import streamit.frontend.nodes.StmtAdd;
import streamit.frontend.nodes.StmtBlock;
import streamit.frontend.nodes.StmtBody;
import streamit.frontend.nodes.StmtBreak;
import streamit.frontend.nodes.StmtContinue;
import streamit.frontend.nodes.StmtDoWhile;
import streamit.frontend.nodes.StmtEnqueue;
import streamit.frontend.nodes.StmtExpr;
import streamit.frontend.nodes.StmtFor;
import streamit.frontend.nodes.StmtIfThen;
import streamit.frontend.nodes.StmtJoin;
import streamit.frontend.nodes.StmtLoop;
import streamit.frontend.nodes.StmtPhase;
import streamit.frontend.nodes.StmtPush;
import streamit.frontend.nodes.StmtReturn;
import streamit.frontend.nodes.StmtAssert;
import streamit.frontend.nodes.StmtSendMessage;
import streamit.frontend.nodes.StmtSplit;
import streamit.frontend.nodes.StmtVarDecl;
import streamit.frontend.nodes.StreamCreator;
import streamit.frontend.nodes.StreamSpec;
import streamit.frontend.nodes.StreamType;
import streamit.frontend.nodes.SymbolTable;
import streamit.frontend.nodes.TempVarGen;
import streamit.frontend.nodes.Type;
import streamit.frontend.nodes.TypeArray;
import streamit.frontend.nodes.TypePortal;
import streamit.frontend.nodes.TypePrimitive;
import streamit.frontend.nodes.TypeStruct;
import streamit.frontend.nodes.TypeStructRef;
import streamit.frontend.tojava.ExprJavaConstructor;
import streamit.frontend.tojava.NodesToJava;
import streamit.frontend.tojava.StmtAddPhase;
import streamit.frontend.tojava.StmtIODecl;
import streamit.frontend.tojava.StmtSetTypes;

/**
 * Traverse a front-end tree and produce Java code.  This uses {@link
 * streamit.frontend.nodes.FEVisitor} directly, without going through
 * an intermediate class such as <code>FEReplacer</code>.  Every
 * method actually returns a String.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id$
 */

class VarStack{
	private HashSet curVars;
	private VarStack kid;
	public VarStack(){
		curVars = new HashSet();
		kid = null;
	}
	
	public void declare(String nm){
		curVars.add(nm);		
	}
	
	public boolean isDeclared(String nm){
		if( kid == null)
			return curVars.contains(nm);
		else{
			return curVars.contains(nm) || kid.isDeclared(nm);
		}		
	}
	
	public VarStack pushscope(){
		VarStack tmp = new VarStack();
		tmp.kid = this;
		return tmp;
	}

	public VarStack popscope(){
		return this.kid;
	}
}


public class NodesToNative extends  NodesToJava
{    
	private NodesToSBit nts;
    private  VarStack vstack;
    private MethodState fatherstate;
    // A string consisting of an even number of spaces.    
    
    
    public NodesToNative(StreamSpec ss, TempVarGen varGen, MethodState fatherstate, NodesToSBit nts)
    {
    	super( true, varGen );    
    	this.nts = nts;
    	this.setSs(ss);
    	this.fatherstate = fatherstate;
    	vstack = new VarStack();
    }


    // Convert a Type to a String.  If visitors weren't so generally
    // useless for other operations involving Types, we'd use one here.
    public String convertType(Type type)
    {
        // This is So Wrong in the greater scheme of things.
        if (type instanceof TypeArray)
        {
            TypeArray array = (TypeArray)type;
            String base = convertType(array.getBase());
            return base + "* ";
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
            case TypePrimitive.TYPE_INT: return "int";
            case TypePrimitive.TYPE_FLOAT: return "float";
            case TypePrimitive.TYPE_DOUBLE: return "double";
            case TypePrimitive.TYPE_COMPLEX: return "Complex";
            case TypePrimitive.TYPE_VOID: return "void";
            }
        }
        else if (type instanceof TypePortal)
        {
            return ((TypePortal)type).getName() + "Portal";
        }
        return null;
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
            }
        }
        else if (t instanceof TypeStruct)
            return ((TypeStruct)t).getName() + ".class";
        else if (t instanceof TypeArray)
            return "(" + makeConstructor(t) + ").getClass()";
        // Errp.
        System.err.println("typeToClass(): I don't understand " + t);
        return null;
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
            if (!first) result += ", ";
            if (prefix != null) result += prefix + " ";
            result += convertType(param.getType());
            result += " ";
            String nm = param.getName();
            vstack.declare(nm);
            result += nm;
            first = false;
        }        
        if( params.size() == 0)
        	result += "  _ISMOREPARAM";
        else
        	result += "  _ISMOREPARAMx";
        result += ")";
        return result;
    }

    // Return a representation of lhs = rhs, with no trailing semicolon.
    public String doAssignment(Expression lhs, Expression rhs,
                               SymbolTable symtab)
    {
    	assert(false) : ( "Don't know what this method does.");
    	return null;
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

    public Object visitExprComplex(ExprComplex exp)
    {
    	assert(false) : ( "NYI");
    	return null;
    }

    public Object visitExprConstBoolean(ExprConstBoolean exp)
    {
        if (exp.getVal())
            return " 1 ";
        else
            return " 0 ";
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
        if (getSs().getFuncNamed(name) != null) {
            result = name + "(";
        }
	// look for print and println statements; assume everything
	// else is a math function
	else if (name.equals("print") || name.equals("Print")) {
	    result = "printf( \"%d \\n\", ";
	    result +=  ((Expression)exp.getParams().get(0)).accept(this) + "); \n";
		return result;
	}else if (name.equals("printf") ){
	    result =  name + "(";
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
	}else if (name.equals("printB") || name.equals("PrintB")) {
	
	    result = "printB( ";
	    result +=  ((Expression)exp.getParams().get(0)).accept(this) + "); printf(\"\\n\"); \n";
		return result;
	} else if (name.equals("println")) {
	    result = "System.out.println(";
        } else if (name.equals("super")) {
            result = "super(";
        } else if (name.equals("setDelay")) {
            result = "setDelay(";
        } else if (name.startsWith("enqueue")) {
            result = name + "(";
	} else {	    
	    result =  name + "(";
	}
        boolean first = true;
        for (Iterator iter = exp.getParams().iterator(); iter.hasNext(); )
        {
            Expression param = (Expression)iter.next();
            if (!first) result += ", ";
            first = false;
            result += (String)param.accept(this);
        }
        if( exp.getParams().size() == 0){
        	result += " _STATEPARAM";        	
        }else{
        	result += " _STATEPARAMx";
        }
        result += ")";
        return result;
    }

    public Object visitExprPeek(ExprPeek exp)
    {
        String result = (String)exp.getExpr().accept(this);
        return "INPUT[" + result + "]";        
    }
    
    public Object visitExprPop(ExprPop exp)
    {
        return "*(INPUT++)";
    }

 
    public Object visitExprTypeCast(ExprTypeCast exp)
    {
        return "((" + convertType(exp.getType()) + ")(" +
            (String)exp.getExpr().accept(this) + "))";
    }

 
    public Object visitExprVar(ExprVar exp)
    {
    	String nm = exp.getName();
    	if(vstack.isDeclared(nm))
    		return nm;
    	else{
    		if( fatherstate.varHasValue( nm ) ){
    			return "" + fatherstate.varValue(nm);
    		}else
    			return "( CSTATENAME__" + nm + ") ";
    	}
    }

    public Object visitFieldDecl(FieldDecl field)
    {
    	// Assume all of the fields have the same type.    	    	
    	
    	String result = "";
        for (int i = 0; i < field.getNumFields(); i++)
        {
            //if (i > 0) result += ", ";
        	result += convertType(field.getType(i)) + "  ";
            result += field.getName(i);
            if (field.getInit(i) != null)
                result += " = " + (String)field.getInit(i).accept(this);
            result += "; ";
            result += "// " + field.getContext() + "\n";
        }        
        if (field.getContext() != null)
            result += " // " + field.getContext();
        result += "\n";
        return result;
    }

    public Object visitFunction(Function func)
    {
    	
        String result = "";
        
        if(func.getCls() == Function.FUNC_INIT){
        	result += "INIT()\n";
        	assert( func.getParams().size() == 0 ) : ( "Parameters supported only for bit->bit filters and for pipelines and splitjoins. "); 
        	//result += "INIT";
        	//result += doParams(func.getParams(), "") + "\n";
        	result += "###NATIVE_CODE_BEGIN\n";
        	result += "{\n";        	        		        	
        	result += (String)func.getBody().accept(this);
        	result += "}\n";
        	result += "###NATIVE_CODE_END\n";
        	return result;
        }
        
        if(func.getCls() == Function.FUNC_WORK){
        	result += "WORK()\n";
        	result += "// " + func.getContext() + "\n";
        	assert( func.getParams().size() == 0 ) : ( "");        	
        	result += "{\n";        	
        	result += "###NATIVE_CODE_BEGIN\n";
        	assert(((StmtBlock)func.getBody()).getStmts().size()>0) : ( "You can not have empty functions! \n" + func.getContext());
        	result += (String)func.getBody().accept(this);
        	result += "###NATIVE_CODE_END\n";
        	if( ((FuncWork)func).getPopRate() != null)
        		result += "input_RATE = " + ((FuncWork)func).getPopRate().accept(this.nts) + ";\n";
        	else
        		result += "input_RATE = 0;\n";
        	if(((FuncWork)func).getPushRate() != null)
        		result += "output_RATE = " + ((FuncWork)func).getPushRate().accept(this.nts) + ";\n";
        	else
        		result += "output_RATE = 0;\n";
        	result += "}\n";        	
        	return result;
        }
        
        result += "NATIVE_METHOD " + func.getName() + ";\n";
    	result += "###NATIVE_CODE_BEGIN\n";
        if (!func.getName().equals(getSs().getName()))
            result += convertType(func.getReturnType()) + " ";                	
        result += func.getName();
        String prefix = null;
        vstack = vstack.pushscope();
        result += doParams(func.getParams(), prefix) + " ";
        result += (String)func.getBody().accept(this);
        vstack = vstack.popscope();
        result += "\n";
    	result += "###NATIVE_CODE_END\n";
        return result;
    }
    
    public Object visitFuncWork(FuncWork func)
    {
        // Nothing special here; we get to ignore the I/O rates.
        return visitFunction(func);
    }

    public Object visitProgram(Program prog)
    {
        // Nothing special here either.  Just accumulate all of the
        // structures and streams.
        String result = "";
        for (Iterator iter = prog.getStructs().iterator(); iter.hasNext(); )
        {
            TypeStruct struct = (TypeStruct)iter.next();
            result +=  indent + "class " + struct.getName() +
                " extends Structure {\n";
            addIndent();
            for (int i = 0; i < struct.getNumFields(); i++)
            {
                String name = struct.getField(i);
                Type type = struct.getType(name);
                result += indent + convertType(type) + " " + name + ";\n";
            }
            unIndent();
            result += indent + "}\n";
        }
        for (Iterator iter = prog.getStreams().iterator(); iter.hasNext(); )
            result += (String)((StreamSpec)iter.next()).accept(this);
        return result;
    }


    public Object visitSJDuplicate(SJDuplicate sj)
    {
        assert(false) : ( "Native filters can only be regular filters, NOT splitjoins or pipelines");
        return null;
    }

    public Object visitSJRoundRobin(SJRoundRobin sj)
    {
    	assert(false) : ( "Native filters can only be regular filters, NOT splitjoins or pipelines");
        return null;        
    }

    public Object visitSJWeightedRR(SJWeightedRR sj)
    {
    	assert(false) : ( "Native filters can only be regular filters, NOT splitjoins or pipelines");
        return null;        
    }

    public Object doStreamCreator(String how, StreamCreator sc)
    {
    	assert(false) : ( "NOT SUPPORTED.");
    	return null;
    }
    
    public Object visitStmtAdd(StmtAdd stmt)
    {
        return doStreamCreator("add", stmt.getCreator());
    }
    
    
    
    public Object visitStmtFor(StmtFor stmt){
    	vstack = vstack.pushscope();
    	Object o = super.visitStmtFor(stmt);
    	vstack = vstack.popscope();
    	return o;
    }

    public Object visitStmtBlock(StmtBlock stmt)
    {
        // Put context label at the start of the block, too.
    	vstack = vstack.pushscope();
        String result = "{";
        if (stmt.getContext() != null)
            result += " // " + stmt.getContext();
        result += "\n";
        addIndent();
        for (Iterator iter = stmt.getStmts().iterator(); iter.hasNext(); )
        {
            Statement s = (Statement)iter.next();
            String line = indent;
            line += (String)s.accept(this);
	    if (!(s instanceof StmtIfThen)) {
		line += ";";
	    }
            if (s.getContext() != null)
                line += " // " + s.getContext();
            line += "\n";
            result += line;
        }
        unIndent();
        result += indent + "}";
        vstack = vstack.popscope();
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

    public Object visitStmtEnqueue(StmtEnqueue stmt)
    {
    	assert(false) : ( "NOT SUPPORTED");        
    	return null;
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

        

    public Object visitStmtJoin(StmtJoin stmt)
    {
    	assert(false) : ( "Native filters can only be regular filters, NOT splitjoins or pipelines");
        return null;        
    }
    
    public Object visitStmtLoop(StmtLoop stmt)
    {
    	assert(false) : ( "Native filters can only be regular filters, NOT splitjoins or pipelines");
        return null;        
    }

    public Object visitStmtPhase(StmtPhase stmt)
    {
    	assert(false) : ( "Native filters can only be regular filters, NOT splitjoins or pipelines");
        ExprFunCall fc = stmt.getFunCall();
        // ASSERT: the target is always a phase function.
        FuncWork target = (FuncWork)getSs().getFuncNamed(fc.getName());
        StmtExpr call = new StmtExpr(stmt.getContext(), fc);
        String peek, pop, push;
        if (target.getPeekRate() == null)
            peek = "0";
        else
            peek = (String)target.getPeekRate().accept(this);
        if (target.getPopRate() == null)
            pop = "0";
        else
            pop = (String)target.getPopRate().accept(this);
        if (target.getPushRate() == null)
            push = "0";
        else
            push = (String)target.getPushRate().accept(this);
        
        return "phase(new WorkFunction(" + peek + "," + pop + "," + push +
            ") { public void work() { " + call.accept(this) + "; } })";
    }

    public Object visitStmtPush(StmtPush stmt)
    {
    	return "*(OUTPUT++) = " +
        (String)stmt.getValue().accept(this);         
    }

    public Object visitStmtReturn(StmtReturn stmt)
    {
        if (stmt.getValue() == null) return "return";
        return "return " + (String)stmt.getValue().accept(this);
    }

    public Object visitStmtAssert(StmtAssert stmt)
    {
        /* Gilad, 2006-09-06: temporarily (?) prevent this method from
         * being used, until I figure out what it's here for. */
        assert (false) : ("shouldn't get here");
        return "assert (" + (String)stmt.getCond().accept(this) + ")";
    }

    public Object visitStmtSendMessage(StmtSendMessage stmt)
    {
    	assert(false) : ( "Native filters can only be regular filters, NOT splitjoins or pipelines");
        return null;
    }
    
    public Object visitStmtSplit(StmtSplit stmt)
    {
    	assert(false) : ( "Native filters can only be regular filters, NOT splitjoins or pipelines");
        return null;
    }

    public Object visitStmtVarDecl(StmtVarDecl stmt)
    {
        String result = "";        
        result += convertType(stmt.getType(0)) + " ";
        for (int i = 0; i < stmt.getNumVars(); i++)
        {
            if (i > 0)
                result += ", ";
            String nm = stmt.getName(i);
            
            vstack.declare(nm);
            fatherstate.varDeclare(nm);
            String lhsn = fatherstate.varGetLHSName(nm);
            result += nm;
            
            if (stmt.getInit(i) != null)
                result += " = " + (String)stmt.getInit(i).accept(this);
        }
        return result;
    }

    /**
     * For a non-anonymous StreamSpec, check to see if it has any
     * message handlers.  If it does, then generate a Java interface
     * containing the handlers named (StreamName)Interface, and
     * a portal class named (StreamName)Portal.
     */
    
    public Object visitStreamSpec(StreamSpec spec)
    {        
        String result = "// " + spec.getContext() + "\n";
        // Anonymous classes look different from non-anonymous ones.
        // This appears in two places: (a) as a top-level (named)
        // stream; (b) in an anonymous stream creator (SCAnon).
        if (spec.getName() != null)
        {	            
            // This is only public if it's the top-level stream,
            // meaning it has type void->void.	             
        	{	                
                if (spec.getType() == StreamSpec.STREAM_FILTER)
                {
                    // Need to notice now if this is a phased filter.
                    FuncWork work = spec.getWorkFunc();
                    if (work.getPushRate() == null &&
                        work.getPopRate() == null &&
                        work.getPeekRate() == null){
                    	assert(spec.getName().equals("IntToBit") || spec.getName().equals("BitToInt")) : ( "NYI");
                    	return "";
                    }else
                        result += "NativeFilter";
                }
                else
                	assert(false) : ( "Only Filters can be native.");
                
                result += " " + spec.getName() + "\n";
            }
        }
        else
        {
            // Anonymous stream:
            result += "new ";
            switch (spec.getType())
            {
            case StreamSpec.STREAM_FILTER: result += "NativeFilter";
                break;
            case StreamSpec.STREAM_PIPELINE: assert(false) : ( "Only Filters can be native.");
                break;
            case StreamSpec.STREAM_SPLITJOIN: assert(false) : ( "Only Filters can be native.");
                break;
            case StreamSpec.STREAM_FEEDBACKLOOP: assert(false) : ( "Only Filters can be native.");
                break;
            }
            result += "() \n" + indent;
            addIndent();
        }
        
        // At this point we get to ignore wholesale the stream type, except
        // that we want to save it.
        StreamSpec oldSS = getSs();
        setSs(spec);
        result += "{\n"; 
        // Output field definitions:
        for (Iterator iter = spec.getVars().iterator(); iter.hasNext(); )
        {
            FieldDecl varDecl = (FieldDecl)iter.next();
            result += (String)varDecl.accept(this);
        }
        
        // Output method definitions:
        for (Iterator iter = spec.getFuncs().iterator(); iter.hasNext(); )
            result += (String)(((Function)iter.next()).accept(this));

        setSs(oldSS);        
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
            return  "";
        }
        if (node instanceof StmtAddPhase)
        {
            StmtAddPhase ap = (StmtAddPhase)node;
            String result;
            if (ap.isInit())
                result = "addInitPhase";
            else result = "addSteadyPhase";
            result += "(";
            if (ap.getPeek() == null)
                result += "0, ";
            else
                result += (String)ap.getPeek().accept(this) + ", ";
            if (ap.getPop() == null)
                result += "0, ";
            else
                result += (String)ap.getPop().accept(this) + ", ";
            if (ap.getPush() == null)
                result += "0, ";
            else
                result += (String)ap.getPush().accept(this) + ", ";
            result += "\"" + ap.getName() + "\")";
            return result;
        }
        if (node instanceof StmtSetTypes)
        {
            StmtSetTypes sst = (StmtSetTypes)node;
            return "setIOTypes(" + typeToClass(sst.getInType()) +
                ", " + typeToClass(sst.getOutType()) + ")";
        }
        return "";
    }
}
