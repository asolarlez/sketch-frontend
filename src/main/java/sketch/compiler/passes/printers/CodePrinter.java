package streamit.frontend.passes;

import java.io.OutputStream;
import java.io.PrintWriter;

import streamit.frontend.nodes.FEReplacer;
import streamit.frontend.nodes.Statement;
import streamit.frontend.nodes.StmtBlock;

public class CodePrinter extends FEReplacer {

	private static final int	tabWidth	= 2;
	protected final PrintWriter	out;
	protected int	indent	= 0;
	protected String	pad	= "";

	public CodePrinter (OutputStream os) {
		super ();
		out = new PrintWriter (os);
	}

	protected void printTab () {
		if(indent*tabWidth!=pad.length()) {
			StringBuffer b=new StringBuffer();
			for(int i=0;i<indent*tabWidth;i++)
				b.append(' ');
			pad=b.toString();
		}
		out.print(pad);
	}

	protected void printLine (String s) {
		printTab();
		out.println(s);
	}

	protected void printIndentedStatement (Statement s) {
		if(s==null) return;
		if(s instanceof StmtBlock)
			s.accept(this);
		else {
			indent++;
			s.accept(this);
			indent--;
		}
	}

}