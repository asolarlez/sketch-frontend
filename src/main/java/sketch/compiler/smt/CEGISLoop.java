package sketch.compiler.smt;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.logging.Logger;

import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.cmdline.SMTOptions.SMTSolver;
import sketch.compiler.dataflow.recursionCtrl.RecursionControl;
import sketch.compiler.main.PlatformLocalization;
import sketch.compiler.main.seq.SMTSketchOptions;
import sketch.compiler.smt.beaver.BeaverBVBackend;
import sketch.compiler.smt.cvc3.Cvc3BVBackend;
import sketch.compiler.smt.cvc3.Cvc3Backend;
import sketch.compiler.smt.cvc3.Cvc3SMTLIBBackend;
import sketch.compiler.smt.partialeval.AssertionFailedException;
import sketch.compiler.smt.partialeval.FormulaPrinter;
import sketch.compiler.smt.partialeval.NodeToSmtVtype;
import sketch.compiler.smt.partialeval.SmtValueOracle;
import sketch.compiler.smt.solvers.SMTBackend;
import sketch.compiler.smt.solvers.STPBackend;
import sketch.compiler.smt.solvers.STPYicesBackend;
import sketch.compiler.smt.solvers.YicesBVBackend;
import sketch.compiler.smt.solvers.YicesIntBackend;
import sketch.compiler.smt.solvers.Z3BVBackend;
import sketch.compiler.solvers.SolutionStatistics;
import sketch.util.Stopwatch;

public class CEGISLoop {

    public static final String SYNTHESIS_TIME = "Synthesis Time(ms)";
    public static final String FINAL_SYNTHESIS_TIME = "Final Iteration Synthesis Time(ms)";
    public static final String VERIFICATION_TIME = "Verificaiton Time(ms)";
    public static final String CEGIS_ITR = "CEGIS Iterations";
    public static final String SOLUTION_TIME = "Solution Time(ms)";
    public static final String LOOP_TIME = "CEGIS Loop Time(ms)";
   

	protected Program mProg;
	protected GeneralStatistics mStat;
	protected SmtValueOracle mBestOracle;
	protected final SMTSketchOptions options;
	protected RecursionControl mRControl;
	private String mProgramName;
	protected TempVarGen mTmpVarGen;
	private Stopwatch mLoopTimer;
	
	
	protected SmtValueOracle mAllZerosOracle;
	protected int mIntNumBits;
    private long finalSynTime;
	
	private static Logger log = Logger.getLogger(CEGISLoop.class.getCanonicalName());
	
	public CEGISLoop(String programName, SMTSketchOptions options, GeneralStatistics stat, RecursionControl rControl) {
		mProgramName = programName;
        this.options = options;
		mRControl = rControl;
		
		mTmpVarGen = new TempVarGen("__sa");
		mStat = stat;
		mLoopTimer = new Stopwatch();
		
		mIntNumBits = options.bndOpts.intbits;

		
	}

	public void start(NodeToSmtVtype vtype, SMTBackend solver) {
		mLoopTimer.start();
		ArrayList<SmtValueOracle> observations = new ArrayList<SmtValueOracle>();

		SmtValueOracle curOracle = new SmtValueOracle.AllZeroOracle(solver.getSMTTranslator());
		mBestOracle = curOracle;
		mStat.incrementLong(CEGIS_ITR, 1);
		try {
			
			while (true) {
				log.fine("Iteration: " + mStat.getLong(CEGIS_ITR));
				
				// Verification
				log.fine("Verifier: ");
				curOracle = verifyPE(solver, vtype, mBestOracle);	
				mLoopTimer.start();
				
				if (curOracle == null) { // Is unsatisfiable, the bestOracle is
					// good
					break;
				}
				
				log.fine(curOracle.toString());
				if (observations.contains(curOracle))
					throw new SolverFailedException("Verifier discovered a counterexample that was discovered previously:" + curOracle.toString());
				observations.add(curOracle);

				// Synthesis
				log.fine("Synthesizer: ");
				curOracle = synthesisPE(solver, vtype, observations);
				mLoopTimer.start();
				
				if (curOracle == null) { // Is unsatisfiable
					mBestOracle = null;
					break;
				} else {
					mBestOracle = curOracle;
				}

				log.fine(curOracle.toString());
				
				mStat.incrementLong(CEGIS_ITR, 1);
			}
			
		
		} catch (IOException e) {
			e.printStackTrace();
			mLoopTimer.start();
		} catch (InterruptedException e) {
			e.printStackTrace();
			mLoopTimer.start();
		} catch (Exception e) {
			e.printStackTrace();
			mLoopTimer.start();
		} catch (SolverFailedException e) {
            mBestOracle = null;
            mLoopTimer.start();
            throw e;
        } catch (Throwable e) {
            e.printStackTrace();
            mLoopTimer.start();
		} finally {
			mLoopTimer.stop();
		}

		
		mStat.incrementLong(LOOP_TIME, mLoopTimer.toValue());
		mStat.incrementLong(FINAL_SYNTHESIS_TIME, finalSynTime);
	}

	public GeneralStatistics getStat() {
		return mStat;
	}

	/**
	 * 
	 * @return an SmtValueOracle object if the sketch can be resolved null if
	 *         the sketch is unsatisfiable or there is an error in solver
	 */
	public SmtValueOracle getSolution() {
		return mBestOracle;
	}

	/*
	 * Private methods
	 */
	/**
	 * Do one round of partial evaluation. Each round of partial evaluation
	 * creates a new SMTBackend object, SmtValueOracle object
	 * 
	 * 
	 * @param holevalues
	 *            Values for variables we want to set in the formula. These are
	 *            the observations if we are in synthesis or the holes if we are
	 *            in verification (in which case the set has only one element).
	 * @param isSynthesis
	 *            <tt>true</tt> if we are in the synthesis phase; <tt>false</tt>
	 *            if we are in the verification phase.
	 */
	private SmtValueOracle verifyPE(SMTBackend solver, NodeToSmtVtype vtype, SmtValueOracle holevalues)
			throws IOException, InterruptedException, SolverFailedException {

		try {
			solver.init();
            String tmpFile =
                PlatformLocalization.getLocalization().getTempPathString(
                        mProgramName + "-v" + mStat.getLong(CEGIS_ITR) + ".smt");
			solver.setTmpFilePath(tmpFile);
			
			log.fine("Generating formula");
			PrintStream ps = new PrintStream(solver.createStreamToSolver());
			FormulaPrinter printer = solver.createFormulaPrinter(vtype, ps);
			printer.printVerificaitonFormula(holevalues);
			ps.close();
			
		} catch (ArrayIndexOutOfBoundsException e) {
			return mAllZerosOracle;
		} catch (AssertionFailedException e) {
			// if an assert fails in verification, the sketch
			// can be failed with any input.
			return mAllZerosOracle;
		} catch (RuntimeException e) {
			// In PartialEvaluator.visitStmtIfThenElse, it wraps any Throwable (such as
			// AssertionFailedException into RuntimeException, so we need it handle it here
			// ideal, i don't want to wrap it.
			return mAllZerosOracle;
		} finally {
			mLoopTimer.stop();
		}
			
		log.fine("Solving...");
		SolutionStatistics stat = solver.solve(vtype);
		mStat.incrementLong(VERIFICATION_TIME, stat.solutionTimeMs());
			
		if (!stat.successful())
			return null;
		
		return solver.getOracle();
	}
	
	private SmtValueOracle synthesisPE(SMTBackend solver, NodeToSmtVtype vtype, ArrayList<SmtValueOracle> observations)
			throws IOException, InterruptedException, SolverFailedException {
	
		try {
			solver.init();
            String tmpFile =
                    PlatformLocalization.getLocalization().getTempPathString(
                            mProgramName + "-s" + mStat.getLong(CEGIS_ITR) + ".smt");
            solver.setTmpFilePath(tmpFile);
			
			PrintStream ps = new PrintStream(solver.createStreamToSolver());
//			FormulaPrinter printer = vtype.new FormulaPrinter(ps, true);
			
			FormulaPrinter printer = solver.createFormulaPrinter(vtype, ps);
			printer.printSynthesisFormula(observations);
			ps.close();
			
		} catch (AssertionFailedException e) {
			// if any assert fail in partial-eval during synthesis,
			// the sketch is deemed to be buggy
			log.info(e.getMessage());
			return null;
		} catch (ArrayIndexOutOfBoundsException e) {
			log.finest("ArrayIndexOutOfBound in partial eval");
			return null;
		} finally {
			mLoopTimer.stop();
		}
		
		SolutionStatistics stat = solver.solve(vtype);
		mStat.incrementLong(SYNTHESIS_TIME, stat.solutionTimeMs());
		finalSynTime = stat.solutionTimeMs();
		
		if (!stat.successful())
			return null;

		return solver.getOracle();
	}

	public SMTBackend selectBackend(SMTSolver backend, boolean bv,
			boolean tracing, boolean isSynthesis) throws IOException {
        String tmpFile = PlatformLocalization.getLocalization().getTempPathString(
                        mProgramName + "-" + (isSynthesis ? "s" : "v") +
                        mStat.getLong(CEGIS_ITR));

		if (options.debugOpts.fakeSolver) {
			return new FakeSolverBackend(options,
					options.feOpts.keepTmp ? tmpFile + ".fake" : null,
					mRControl, mTmpVarGen, tracing);
		} else if (backend == SMTSolver.cvc3) {
			if (bv)
				return new Cvc3BVBackend(options,
						options.feOpts.keepTmp ? tmpFile + ".cvc3"
								: null, mRControl, mTmpVarGen, tracing);
			else
				return new Cvc3Backend(options,
						options.feOpts.keepTmp ? tmpFile + ".cvc3"
								: null, mRControl, mTmpVarGen, tracing);
		} else if (backend == SMTSolver.cvc3smtlib) {
			return new Cvc3SMTLIBBackend(
			        options,
					options.feOpts.keepTmp ? tmpFile + ".smtlib" : null,
					mRControl, mTmpVarGen, tracing);
		} else if (backend == SMTSolver.z3) {
			return new Z3BVBackend(
			        options,
					options.feOpts.keepTmp ? tmpFile + ".smtlib" : null,
					mRControl, mTmpVarGen, tracing);
		} else if (backend == SMTSolver.beaver) {
			return new BeaverBVBackend(
			        options,
					options.feOpts.keepTmp ? tmpFile + ".smtlib" : null,
					mRControl, mTmpVarGen, tracing);
		} else if (backend == SMTSolver.yices) {
			return new YicesBVBackend(
			        options,
					options.feOpts.keepTmp ? tmpFile + ".smtlib" : null,
					mRControl, mTmpVarGen, 1, tracing);
		} else if (backend == SMTSolver.yices2) {
		    if (bv)
		        return new YicesBVBackend(
		                options,
                    options.feOpts.keepTmp ? tmpFile + ".smtlib" : null,
                    mRControl, mTmpVarGen, 2, tracing);
		    else
              return new YicesIntBackend(
                      options,
                      options.feOpts.keepTmp ? tmpFile + ".smtlib" : null,
                      mRControl, mTmpVarGen, 2, tracing);
		        
		} else if (backend == SMTSolver.stp) {
//			return new StpSmtlibBackend(
			return new STPBackend(
			        options,
					options.feOpts.keepTmp ? tmpFile + ".stp" : null,
					mRControl, mTmpVarGen, tracing);
		} else if (backend == SMTSolver.stpyices2) {
	            return new STPYicesBackend(
	                    options,
	                    options.feOpts.keepTmp ? tmpFile + ".stp" : null,
	                    mRControl, mTmpVarGen, tracing);
		} else {
		    
			// other solvers for example
			return null;
		}
	}
}
