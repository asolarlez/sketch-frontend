package sketch.compiler.smt;

import java.io.PrintStream;

/**
 * A class that provides facilities for printing 
 * @author Lexin Shan
 * @email lshan@eecs.berkeley.edu
 *
 */
public abstract class PrintStreamTranslator  {
	
	PrintStream out;

	// for debugging purpose
	int currLineNum = 0;
	
	public PrintStreamTranslator(PrintStream ps) {
		this.out = ps;
		this.currLineNum = 1;
	}
	/*
	 * Utilities
	 */
	protected void print(Object o) {
		this.out.print(o.toString());
	}

	protected void println(Object o) {
		this.out.println(o.toString());
		currLineNum++;
	}
	
}
