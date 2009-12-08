package sketch.compiler.smt;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.logging.Logger;

import sketch.compiler.CommandLineParamManager;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.dataflow.recursionCtrl.RecursionControl;
import sketch.compiler.smt.partialeval.BlastArrayVtype;
import sketch.compiler.smt.partialeval.FormulaPrinter;
import sketch.compiler.smt.partialeval.NodeToSmtVtype;
import sketch.compiler.smt.partialeval.SmtValueOracle;
import sketch.compiler.smt.partialeval.TOAVtype;
import sketch.compiler.solvers.SolutionStatistics;
import sketch.util.SynchronousTimedProcess;

public abstract class SMTBackend {

	protected final CommandLineParamManager params;
	String solverErrorStr;
	protected final RecursionControl rcontrol;
	protected final TempVarGen varGen;
	
	private SynchronousTimedProcess stp;
	private OutputStream streamToSolver;
	protected SmtValueOracle mOracle;
	protected FormulaPrinter mTrans;
	
	
	protected boolean tracing = false;
	protected boolean outputTmpFile;
	protected String tmpFilePath; // input file to the solver
	
	protected Logger logger;
	
	// from outside
	protected int mIntNumBits;
	
	/*
	 * Abstract Methods
	 */
	
	/**
	 * Creates a process for the solver
	 */
	protected abstract SynchronousTimedProcess createSolverProcess() throws IOException;
	
	/**
	 * Creates an AbstractValueOracle for this backend
	 */
	protected abstract SmtValueOracle createValueOracle();
	
	protected abstract FormulaPrinter createFormulaPrinterInternal(NodeToSmtVtype formula, PrintStream ps);
	
	
	/**
	 * Creates an output stream for the solver
	 * @return
	 * @throws FileNotFoundException 
	 */
	protected abstract OutputStream createStreamToSolver() throws IOException;
	
	public NodeToSmtVtype createFormula(int intBits, int inBits, int cBits, boolean useTheoryOfArray, GeneralStatistics stat, TempVarGen tmpVarGen) {
	    if (useTheoryOfArray) {
	        return new TOAVtype(  
	                intBits,
	                inBits,
	                cBits,
	                stat,
	                tmpVarGen);
	    } else
    		return new BlastArrayVtype(  
    				intBits,
    				inBits,
    				cBits,
    				stat,
    				tmpVarGen);
	}
	
	/*
	 * Getters and Setters
	 */
	/**
	 * 
	 * @return
	 */
	public SynchronousTimedProcess getSolverProcess() {
		if (this.stp == null) {
			try {
				this.stp = createSolverProcess();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return this.stp;
	}

	/**
	 * Returns the Oracle for this backend
	 */
	public SmtValueOracle getOracle() {
		
		return this.mOracle;
	}
	
	public FormulaPrinter createFormulaPrinter(NodeToSmtVtype formula, PrintStream ps) {
	    
	    mTrans = createFormulaPrinterInternal(formula, ps);
        
	    return mTrans;
	}
	
	/**
	 * Returns the SMTTranslator object needed for input to this backend
	 * @return
	 */
	public FormulaPrinter getSMTTranslator() {
	    return mTrans;
	}
	
	/**
	 * Returns the output stream for the solver
	 * @return
	 * @throws FileNotFoundException
	 */
	protected OutputStream getOutputStreamToSolver() {
		
		if (streamToSolver == null) {
			try {
				streamToSolver = createStreamToSolver();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Can't create output stream to solver", e);
			}
		}
		return streamToSolver;
	}
	
	public String getTmpFilePath() {
		assert outputTmpFile : "API bug";
		return tmpFilePath;
	}
	
	public void setIntNumBits(int intNumBits) {
		mIntNumBits = intNumBits;
	}
	

	/*
	 * Constructors
	 */
	public SMTBackend(CommandLineParamManager params, String tmpFilePath,
			RecursionControl rcontrol, TempVarGen varGen, boolean tracing) throws IOException {
		this.params = params;
		this.rcontrol = rcontrol;
		this.varGen = varGen;
		this.tracing = tracing;
		
		this.outputTmpFile = tmpFilePath != null;
		this.tmpFilePath = tmpFilePath;
	}
	
	public void init() {
		this.stp = null;
		this.streamToSolver = null;
		this.mOracle = null;
	}
	
	
	/**
	 * Solve the formulas and get the result into bestOracle
	 * @param stp
	 * @return
	 * @throws InterruptedException 
	 */
	public abstract SolutionStatistics solve(NodeToSmtVtype formula) throws IOException, InterruptedException, SolverFailedException;

	protected boolean verbose() {
		return params.flagValue("verbosity") >= 3;
	}

	// TODO: duplication is absurd now, need to use the Logger class
	protected void log(String msg) {
		log(3, msg);
	}

	protected void log(int level, String msg) {
		if (params.flagValue("verbosity") >= level)
			System.out.println("[SATBackend] " + msg);
	}

	
}
