/**
 *
 */
package streamit.frontend.passes;

import java.io.PrintWriter;
import java.util.List;
import java.util.Stack;

import streamit.frontend.nodes.ExprArray;
import streamit.frontend.nodes.ExprArrayInit;
import streamit.frontend.nodes.ExprArrayRange;
import streamit.frontend.nodes.ExprBinary;
import streamit.frontend.nodes.ExprComplex;
import streamit.frontend.nodes.ExprConstBoolean;
import streamit.frontend.nodes.ExprConstChar;
import streamit.frontend.nodes.ExprConstFloat;
import streamit.frontend.nodes.ExprConstInt;
import streamit.frontend.nodes.ExprConstStr;
import streamit.frontend.nodes.ExprField;
import streamit.frontend.nodes.ExprFunCall;
import streamit.frontend.nodes.ExprNew;
import streamit.frontend.nodes.ExprNullPtr;
import streamit.frontend.nodes.ExprPeek;
import streamit.frontend.nodes.ExprPop;
import streamit.frontend.nodes.ExprStar;
import streamit.frontend.nodes.ExprTernary;
import streamit.frontend.nodes.ExprTypeCast;
import streamit.frontend.nodes.ExprUnary;
import streamit.frontend.nodes.ExprVar;
import streamit.frontend.nodes.Expression;
import streamit.frontend.nodes.FENode;
import streamit.frontend.nodes.FieldDecl;
import streamit.frontend.nodes.Function;
import streamit.frontend.nodes.Parameter;
import streamit.frontend.nodes.Program;
import streamit.frontend.nodes.Statement;
import streamit.frontend.nodes.StmtAssert;
import streamit.frontend.nodes.StmtAssign;
import streamit.frontend.nodes.StmtAtomicBlock;
import streamit.frontend.nodes.StmtBlock;
import streamit.frontend.nodes.StmtBreak;
import streamit.frontend.nodes.StmtContinue;
import streamit.frontend.nodes.StmtDoWhile;
import streamit.frontend.nodes.StmtEmpty;
import streamit.frontend.nodes.StmtExpr;
import streamit.frontend.nodes.StmtFor;
import streamit.frontend.nodes.StmtFork;
import streamit.frontend.nodes.StmtIfThen;
import streamit.frontend.nodes.StmtInsertBlock;
import streamit.frontend.nodes.StmtLoop;
import streamit.frontend.nodes.StmtReorderBlock;
import streamit.frontend.nodes.StmtReturn;
import streamit.frontend.nodes.StmtVarDecl;
import streamit.frontend.nodes.StmtWhile;
import streamit.frontend.nodes.StreamSpec;
import streamit.frontend.nodes.StreamType;
import streamit.frontend.nodes.TypeArray;
import streamit.frontend.nodes.TypePrimitive;
import streamit.frontend.nodes.TypeStruct;
import streamit.frontend.nodes.TypeStructRef;

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
abstract public class CodePrinterVisitor extends SymbolTableVisitor {
	protected PrintWriter currOut;
	protected Stack<PrintWriter> savedOuts;
	protected int currIndent;

	protected final String tab = "  ";

	public CodePrinterVisitor () {
		this (new PrintWriter (System.out));
	}

	public CodePrinterVisitor (PrintWriter out) {
		super (null);
		currOut = out;
	}

	/**
	 * Save the current writer to which code is being printed, and make NEWOUT
	 * the new writer to which to print code.
	 */
	public void pushWriter (PrintWriter newOut) {
		savedOuts.push (currOut);
		currOut = newOut;
	}

	/**
	 * Restore the last-saved writer, to which subsequent code will be printed.
	 */
	public void popWriter () {
		currOut = savedOuts.pop ();
	}

	public void indent () {
		currIndent++;
	}

	public void dedent () {
		currIndent--;
	}

	public void printTab () {
		String t = "";
		for (int i = currIndent; i > 0; --i)
			t += tab;
		print (t);
	}

	public void print (String s) {
		currOut.print (s);
	}

	/** Print S followed by a line terminator. */
	public void printLine (String s) {
		currOut.println(s);
	}

	/** Print S, indented to the current depth, followed by a line terminator. */
	public void printIndentedLine (String s) {
		printTab();
		printLine (s);
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

	// === EXPRESSIONS ===

	public Object visitExprArray (ExprArray ea) {
		ea.getBase ().accept (this);
		print ("[");
		ea.getOffset ().accept (this);
		print ("]");
		return ea;
	}

	public Object visitExprArrayInit (ExprArrayInit eai) {
		List<Expression> elems = eai.getElements ();

		print ("{ ");
		elems.get (0).accept (this);
		for (int i = 1; i < elems.size (); ++i) {
			print (", ");
			elems.get (i).accept (this);
		}
		print (" }");

		return eai;
	}

	public Object visitExprArrayRange (ExprArrayRange ear) {
		List members = ear.getMembers ();

		ear.getBase ().accept (this);
		print ("[");
		// TODO: this doesn't properly visit the Range and RangeLen children
		// of the array range
		print (""+ members.get (0));
		for (int i = 1; i < members.size (); ++i)
			print (", "+ members.get (i));
		print ("]");

		return ear;
	}

	public Object visitExprBinary (ExprBinary eb) {
		print ("(");
		eb.getLeft ().accept (this);
		print (eb.getOpString ());
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
		if (params.size () > 0)
			params.get (0).accept (this);
		for (int i = 1; i < params.size (); ++i) {
			print (", ");
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
		for (int i = 0; i < fd.getNumFields (); ++i) {
			Expression init = fd.getInit (i);

			printTab ();
			fd.getType (i).accept (this);
			print (" "+ fd.getName (i));
			if (null != init) {
				print (" = ");
				init.accept (this);
			}
			printLine (";");
		}
		return fd;
	}

	public Object visitFunction (Function f) {
		List<Parameter> params = f.getParams ();

		printTab ();
		f.getReturnType ().accept (this);
		print (" ("+ f.getName ());
		if (params.size () > 0)
			params.get (0).accept (this);
		for (int i = 0; i < params.size (); ++i) {
			print (", ");
			params.get (i).accept (this);
		}
		print (")");
		f.getBody ().accept (this);

		return f;
	}

	public Object visitParameter (Parameter p) {
		if (p.isParameterReference ())
			print ("ref ");
		p.getType ().accept (this);
		print (" "+ p.getName ());
		return p;
	}

	public Object visitProgram (Program p) {
		for (TypeStruct s : (List<TypeStruct>) p.getStructs ())
			s.accept (this);
		for (StreamSpec s : (List<StreamSpec>) p.getStreams ())
			s.accept (this);
		return p;
	}

	public Object visitStmtAssert (StmtAssert sa) {
		String msg = sa.getMsg ();

		printTab ();
		print ("assert ");
		sa.getCond ().accept (this);
		if (null != msg)
			print (": "+ msg);
		printLine (";");

		return sa;
	}

	public Object visitStmtAssign (StmtAssign sa) {
		printTab ();
		sa.getLHS ().accept (this);
		print (" = ");
		sa.getRHS ().accept (this);
		printLine (";");
		return sa;
	}

	public Object visitStmtAtomicBlock (StmtAtomicBlock sab) {
		printIndentedLine ("atomic");
		visitStmtBlock (sab);
		return sab;
	}

	public Object visitStmtBlock (StmtBlock sb) {
		printIndentedLine ("{");
		indent ();
		for (Statement s : sb.getStmts ())
			s.accept (this);
		dedent ();
		printIndentedLine ("}");
		return sb;
	}

	public Object visitStmtBreak (StmtBreak sb) {
		printIndentedLine ("break;");
		return sb;
	}

	public Object visitStmtContinue (StmtContinue sc) {
		printIndentedLine ("continue;");
		return sc;
	}

	public Object visitStmtDoWhile (StmtDoWhile sdw) {
		printIndentedLine ("do");
		sdw.getBody ().accept (this);
		print ("while (");
		sdw.getCond ().accept (this);
		printLine (");");
		return sdw;
	}

	public Object visitStmtEmpty (StmtEmpty se) {
		printIndentedLine (";");
		return se;
	}

	public Object visitStmtExpr (StmtExpr se) {
		printTab ();
		se.accept (this);
		printLine (";");
		return se;
	}

	public Object visitStmtFor (StmtFor sf) {
		printTab ();
		printLine ("for (");
		indent ();
		sf.getInit ().accept (this);
		printTab ();  sf.getCond ().accept (this);  printLine (";");
		sf.getIncr ().accept (this);
		dedent ();
		sf.getBody ().accept (this);
		return sf;
	}

	public Object visitStmtFork (StmtFork sf) {
		return null;  // TODO
	}

	public Object visitStmtIfThen (StmtIfThen sit) {
		return null;  // TODO
	}

	public Object visitStmtInsertBlock (StmtInsertBlock sib) {
		return null;  // TODO
	}

	public Object visitStmtLoop (StmtLoop sl) {
		return null;  // TODO
	}

	public Object visitStmtReorderBlock (StmtReorderBlock srb) {
		return null;  // TODO
	}

	public Object visitStmtReturn (StmtReturn sr) {
		return null;  // TODO
	}

	public Object visitStmtVarDecl (StmtVarDecl svd) {
		return null;  // TODO
	}

	public Object visitStmtWhile (StmtWhile sw) {
		return null;  // TODO
	}

	public Object visitStreamSpec (StreamSpec ss) {
		return null;  // TODO
	}

	public Object visitStreamType (StreamType st) {
		return null;  // TODO
	}

	public Object visitTypeArray (TypeArray ta) {
		return null;  // TODO
	}

	public Object visitTypePrimitive (TypePrimitive tp) {
		return null;  // TODO
	}

	public Object visitTypeStruct (TypeStruct ts) {
		return null;  // TODO
	}

	public Object visitTypeStructRef (TypeStructRef tsr) {
		return null;  // TODO
	}
}
