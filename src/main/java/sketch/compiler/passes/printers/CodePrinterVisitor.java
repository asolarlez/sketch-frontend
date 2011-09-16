/**
 *
 */
package sketch.compiler.passes.printers;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map.Entry;
import java.util.Stack;

import sketch.compiler.ast.core.FieldDecl;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.StreamSpec;
import sketch.compiler.ast.core.SymbolTable;
import sketch.compiler.ast.core.exprs.*;
import sketch.compiler.ast.core.stmts.*;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.ast.core.typs.TypeStruct;
import sketch.compiler.ast.core.typs.TypeStructRef;
import sketch.compiler.ast.promela.stmts.StmtFork;
import sketch.compiler.passes.lowering.SymbolTableVisitor;
import sketch.util.NullStream;
import sketch.compiler.ast.spmd.stmts.StmtSpmdfork;
import sketch.compiler.ast.spmd.stmts.SpmdBarrier;

/**
 * A parent class for code printers that strictly adhere to the visitor pattern.
 *
 * Use the CodePrinter class if the strict visitor pattern is too awkward for
 * your needs.
 *
 * This default CodePrinterVisitor prints the code using a C/Java-like syntax.
 *
 * @author <a href="mailto:cgjones@cs.berkeley.edu">Chris Jones</a>
 * @see CodePrinter
 */
public class CodePrinterVisitor extends SymbolTableVisitor {
	private static final class PrinterState {
		PrintWriter out;
		int line;
		PrinterState (PrintWriter _out, int _line) { out = _out; line = _line; }
	}

	private static final String NL = System.getProperty ("line.separator");

	private PrintWriter currOut;
	private Stack<PrinterState> savedOuts = new Stack<PrinterState> ();
	private int currIndent;
	private int line = 1;

	protected final String tab = "  ";

	public CodePrinterVisitor () {
		this (new PrintWriter (System.out));
	}

	public CodePrinterVisitor (PrintWriter out) {
		super (null);
		assert null != out;
		currOut = out;
	}

	/**
	 * Save the current writer to which code is being printed, and make NEWOUT
	 * the new writer to which to print code.
	 */
	public void pushWriter (PrintWriter newOut) {
		assert null != newOut;
		savedOuts.push (new PrinterState (currOut, line));
		currOut = newOut;
	}

	/**
	 * Restore the last-saved writer, to which subsequent code will be printed.
	 */
	public PrintWriter popWriter () {
		PrintWriter lastOut = currOut;
		PrinterState savedState = savedOuts.pop ();
		currOut = savedState.out;
		line = savedState.line;
		return lastOut;
	}

	protected static PrintWriter devNull = new PrintWriter (NullStream.INSTANCE);

	/** Set the output stream to /dev/null. */
	public void quiet () {
		pushWriter (devNull);
	}

	/** Undoes the most recent quiet() operation. */
	public void unquiet () {
		assert devNull == currOut;
		popWriter ();
	}

	public void indent () {
		currIndent++;
	}

	public void dedent () {
		currIndent--;
	}

	/** Return the current line number being printed. */
	public int getLineNumber () {
		return line;
	}

	public void printTab () {
		String t = "";
		for (int i = currIndent; i > 0; --i)
			t += tab;
		print (t);
	}

	public void print (String s) {
		currOut.print (s);
		currOut.flush ();
		updateLineCount (s);
	}

	/** Print S followed by a line terminator. */
	public void println (String s) {		
		currOut.println (s);
		currOut.flush ();
		updateLineCount (s);
		++line;
	}

	/** Print S, indented to the current depth, followed by a line terminator. */
	public void printlnIndent (String s) {
		printTab ();
		println (s);
	}

	public void printIndentedStatement (Statement s) {
		if (null == s) ;
		else if (s instanceof StmtBlock)
			s.accept (this);
		else {
			indent ();
			s.accept (this);
			dedent ();
		}
	}

	protected void updateLineCount (String s) {
		for (int i = s.indexOf (NL, 0); i >= 0; ++line, i = s.indexOf (NL, i+1));
	}

	// === EXPRESSIONS ===


	public Object visitExprArrayInit (ExprArrayInit eai) {
		List<Expression> elems = eai.getElements ();

		print ("{ ");
		for (int i = 0; i < elems.size (); ++i) {
			print ((i != 0) ? ", " : "");
			elems.get (i).accept (this);
		}
		print (" }");

		return eai;
	}

	public Object visitExprArrayRange (ExprArrayRange ear) {
		

		ear.getBase ().accept (this);
		print ("[");
		print(ear.getSelection().toString());
		print ("]");

		return ear;
	}

	public Object visitExprBinary (ExprBinary eb) {
		print ("(");
		eb.getLeft ().accept (this);
		print (" "+ eb.getOpString ()+" ");
		eb.getRight ().accept (this);
		print (")");

		return eb;
	}

	public Object visitExprComplex (ExprComplex ec) {
		print ("/* Complex: "+ ec + "*/");
		return ec;
	}

	public Object visitExprConstBoolean (ExprConstBoolean ecb) {
		print (ecb.getVal () ? "true" : "false");
		return ecb;
	}

	public Object visitExprConstChar (ExprConstChar ecc) {
		print ("'"+ ecc.getVal () +"'");
		return ecc;
	}

	public Object visitExprConstFloat (ExprConstFloat ecf) {
		print (""+ ecf.getVal ());
		return ecf;
	}

	public Object visitExprConstInt (ExprConstInt eci) {
		print (""+ eci.getVal ());
		return eci;
	}

	public Object visitExprConstStr (ExprConstStr ecs) {
		print (ecs.getVal ());
		return ecs;
	}

	public Object visitExprField (ExprField ef) {
		ef.getLeft ().accept (this);
		print ("."+ ef.getName ());
		return ef;
	}

	public Object visitExprFunCall (ExprFunCall efc) {
		List<Expression> params = efc.getParams ();

		print (efc.getName ()+ "(");
		for (int i = 0; i < params.size (); ++i) {
			print ((i != 0) ? ", " : "");
			params.get (i).accept (this);
		}

		return efc;
	}

	public Object visitExprNew (ExprNew en) {
		print ("new ");
		en.getTypeToConstruct ().accept (this);
		print ("()");
		return en;
	}

	public Object visitExprNullPtr (ExprNullPtr enp) {
		print ("null");
		return enp;
	}

	public Object visitExprStar (ExprStar es) {
		// TODO: doesn't properly visit children of ExprStar
		print (""+ es);
		return es;
	}

	public Object visitExprTernary (ExprTernary et) {
		et.getA ().accept (this);
		print (" ? ");
		et.getB ().accept (this);
		print (" : ");
		et.getC ().accept (this);
		return et;
	}

	public Object visitExprTypeCast (ExprTypeCast etc) {
		print ("((");
		etc.getType ().accept (this);
		print (") ");
		etc.getExpr ().accept (this);
		print (")");
		return etc;
	}

	public Object visitExprUnary (ExprUnary eu) {
		String[] prePostOp = new String[2];
		eu.fillPrePostOpStr (prePostOp);

		if (prePostOp[0] != "") {
			print (prePostOp[0]);
			eu.getExpr ().accept (this);
		} else {
			eu.getExpr ().accept (this);
			print (prePostOp[1]);
		}

		return eu;
	}

	public Object visitExprVar (ExprVar ev) {
		print (ev.getName ());
		return ev;
	}

	// === STATEMENTS ETC. ===

	public Object visitFieldDecl (FieldDecl fd) {
		quiet ();  super.visitFieldDecl (fd);  unquiet ();

		for (int i = 0; i < fd.getNumFields (); ++i) {
			Expression init = fd.getInit (i);

			printTab ();
			fd.getType (i).accept (this);
			print (" "+ fd.getName (i));
			if (null != init) {
				print (" = ");
				init.accept (this);
			}
			println (";");
		}
		return fd;
	}

	public Object visitFunction (Function f) {
		SymbolTable oldSymtab = symtab;
		symtab = new SymbolTable (symtab);

		List<Parameter> params = f.getParams ();

		printTab ();
		f.getReturnType ().accept (this);
		print (" ("+ f.getName ());
		for (int i = 0; i < params.size (); ++i) {
			print ((i != 0) ? ", " : "");
			params.get (i).accept (this);
		}
		print (")");
		f.getBody ().accept (this);

		symtab = oldSymtab;

		return f;
	}

	public Object visitParameter (Parameter p) {
		quiet ();  super.visitParameter (p);  unquiet ();

		if (p.isParameterReference ())
			print ("ref ");
		p.getType ().accept (this);
		print (" "+ p.getName ());
		return p;
	}

	public Object visitProgram (Program p) {
		SymbolTable oldSymtab = symtab;
		symtab = new SymbolTable (symtab);


		for (StreamSpec s : (List<StreamSpec>) p.getStreams ())
			s.accept (this);

		symtab = oldSymtab;

		return p;
	}

	public Object visitStmtAssert (StmtAssert sa) {
		String msg = sa.getMsg ();

		printTab ();
		print ("assert ");
		sa.getCond ().accept (this);
		if (null != msg)
			print (": "+ msg);
		println (";");

		return sa;
	}

	public Object visitStmtAssign (StmtAssign sa) {
		printTab ();
		sa.getLHS ().accept (this);
		print (" = ");
		sa.getRHS ().accept (this);
		println (";");
		return sa;
	}

	public Object visitStmtAtomicBlock (StmtAtomicBlock sab) {
		printlnIndent ("atomic");
		visitStmtBlock (sab.getBlock());
		return sab;
	}

	public Object visitStmtBlock (StmtBlock sb) {
		SymbolTable oldSymtab = symtab;
		symtab = new SymbolTable (symtab);

		printlnIndent ("{");
		indent ();
		for (Statement s : sb.getStmts ())
			s.accept (this);
		dedent ();
		printlnIndent ("}");

		symtab = oldSymtab;

		return sb;
	}

	public Object visitStmtBreak (StmtBreak sb) {
		printlnIndent ("break;");
		return sb;
	}

	public Object visitStmtContinue (StmtContinue sc) {
		printlnIndent ("continue;");
		return sc;
	}

	public Object visitStmtDoWhile (StmtDoWhile sdw) {
		printlnIndent ("do");
		sdw.getBody ().accept (this);
		print ("while (");
		sdw.getCond ().accept (this);
		println (");");
		return sdw;
	}

	public Object visitStmtEmpty (StmtEmpty se) {
		printlnIndent (";");
		return se;
	}

	public Object visitStmtExpr (StmtExpr se) {
		printTab ();
		se.getExpression ().accept (this);
		println (";");
		return se;
	}

	public Object visitStmtSpmdfork(StmtSpmdfork sf) {
		SymbolTable oldSymtab = symtab;
		symtab = new SymbolTable (symtab);

		printTab ();
		print("spmdfork (" + sf.getLoopVarName() + "; ");
		sf.getNProc().accept (this); 
		println(")");
		sf.getBody ().accept (this);

		symtab = oldSymtab;

		return sf;
	}

	public Object visitStmtFor (StmtFor sf) {
		SymbolTable oldSymtab = symtab;
		symtab = new SymbolTable (symtab);

		printTab ();
		println ("for (");
		indent ();
		sf.getInit ().accept (this);
		printTab ();  sf.getCond ().accept (this);  println (";");
		sf.getIncr ().accept (this);
		dedent ();
		printlnIndent (")");
		sf.getBody ().accept (this);

		symtab = oldSymtab;

		return sf;
	}

	public Object visitStmtFork (StmtFork sf) {
		SymbolTable oldSymtab = symtab;
		symtab = new SymbolTable (symtab);

		printTab ();
		println ("fork (");
		indent ();
		sf.getLoopVarDecl ().accept (this);
		printTab (); sf.getIter ().accept (this);
		dedent ();
		printlnIndent (")");
		sf.getBody ().accept (this);

		symtab = oldSymtab;

		return sf;
	}

	public Object visitStmtIfThen (StmtIfThen sit) {
		printTab ();
		print ("if (");
		sit.getCond ().accept (this);
		println (")");
		sit.getCons ().accept (this);
		if (null != sit.getAlt ()) {
			print ("else");
			sit.getAlt ().accept (this);
		}
		return sit;
	}

	public Object visitStmtInsertBlock (StmtInsertBlock sib) {
		printTab ();
		println ("insert");
		sib.getInsertStmt ().accept (this);
		printlnIndent ("into");
		sib.getIntoBlock ().accept (this);
		return sib;
	}

	public Object visitStmtLoop (StmtLoop sl) {
		printTab ();
		print ("loop (");
		sl.getIter ().accept (this);
		println (")");
		sl.getBody ().accept (this);
		return sl;
	}

	public Object visitStmtReorderBlock (StmtReorderBlock srb) {
		printTab ();
		println ("reorder");
		srb.getBlock ().accept (this);
		return srb;
	}

	public Object visitStmtReturn (StmtReturn sr) {
		Expression val = sr.getValue ();

		printTab ();
		print ("return");
		if (null != val) {
			print (" ");
			val.accept (this);
		}
		println (";");

		return sr;
	}

	public Object visitStmtVarDecl (StmtVarDecl svd) {
		quiet ();  super.visitStmtVarDecl (svd);  unquiet ();

		for (int i = 0; i < svd.getNumVars (); ++i) {
			Expression init = svd.getInit (i);

			printTab ();
			svd.getType (i).accept (this);
			print (" "+ svd.getName (i));
			if (null != init) {
				print (" = ");
				init.accept (this);
			}
			println (";");
		}
		return svd;
	}

	public Object visitStmtWhile (StmtWhile sw) {
		printTab ();
		print ("while (");
		sw.getCond ().accept (this);
		print (")");
		sw.getBody ().accept (this);
		return sw;
	}

	public Object visitStreamSpec (StreamSpec ss) {
        SymbolTable oldSymtab = symtab;
        symtab = new SymbolTable (symtab);

        for (TypeStruct s : ss.getStructs())
            s.accept(this);

		// TODO: doesn't fully visit the stream spec
		for (FieldDecl f : ss.getVars ())
			f.accept (this);
		
		for (Function f : ss.getFuncs ())
			f.accept (this);

		symtab = oldSymtab;

		return ss;
	}



	public Object visitTypeArray (TypeArray ta) {
		ta.getBase ().accept (this);
		print ("[");
		ta.getLength ().accept (this);
		print ("]");
		return ta;
	}

	public Object visitTypePrimitive (TypePrimitive tp) {
		print (""+ tp);
		return tp;
	}

	public Object visitTypeStruct (TypeStruct ts) {
		printlnIndent ("struct "+ ts.getName ()+ " {");
		indent ();
        for (Entry<String, Type> fld : ts) {
            printTab();
            fld.getValue().accept(this);
            println(" " + fld.getKey() + ";");
        }
		dedent ();
		printlnIndent ("}");
		return ts;
	}

	public Object visitTypeStructRef (TypeStructRef tsr) {
		print (tsr.getName ());
		return tsr;
	}
}
