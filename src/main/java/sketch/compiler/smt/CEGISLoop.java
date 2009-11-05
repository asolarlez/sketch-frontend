package sketch.compiler.smt;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.logging.Logger;

import sketch.compiler.CommandLineParamManager;
import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.dataflow.recursionCtrl.RecursionControl;
import sketch.compiler.smt.beaver.BeaverBVBackend;
import sketch.compiler.smt.cvc3.Cvc3BVBackend;
import sketch.compiler.smt.cvc3.Cvc3Backend;
import sketch.compiler.smt.cvc3.Cvc3SMTLIBBackend;
import sketch.compiler.smt.partialeval.AssertionFailedException;
import sketch.compiler.smt.partialeval.NodeToSmtVtype;
import sketch.compiler.smt.partialeval.SmtValueOracle;
import sketch.compiler.smt.partialeval.NodeToSmtVtype.FormulaPrinter;
import sketch.compiler.smt.stp.STPBackend;
import sketch.compiler.smt.yices.YicesBVBackend;
import sketch.compiler.smt.z3.Z3BVBackend;
import sketch.compiler.solvers.SolutionStatistics;
import sketch.compiler.solvers.Statistics;
import sketch.util.Stopwatch;

public class CEGISLoop {

	public static class CEGISStat extends Statistics {
		private long mSynTime;
		private long mVeriTime;
		private int mNumIter;
		private long mLoop;

		public long getSynthesisTime() {
			return mSynTime;
		}

		public long getVerificationTime() {
			return mVeriTime;
		}
		
		public long getSolutionTimeMs() {
			return mVeriTime + mSynTime;
		}
		
		public long getLoopTimeMs() {
			return mLoop;
		}

		public long getIterations() {
			return mNumIter;
		}
		
		@Override
		public String toString() {
			StringBuffer sb = new StringBuffer();
			sb.append("[CEGIS Solution Stat]\n");
			sb.append("Number of Iterations   ");
			sb.append(getIterations());
			sb.append('\n');
			sb.append("Synthesis Time         ");
			sb.append(getSynthesisTime());
			sb.append('\n');
			sb.append("Verification Time      ");
			sb.append(getVerificationTime());
			sb.append('\n');
			sb.append("Solution Time      ");
			sb.append(getSolutionTimeMs());
			sb.append('\n');
			sb.append("Loop Time      ");
			sb.append(getLoopTimeMs());
			sb.append('\n');
			return sb.toString();
		}
	}

	protected Program mProg;
	protected CEGISStat mStat;
	protected SmtValueOracle mBestOracle;
	protected CommandLineParamManager mParams;
	protected RecursionControl mRControl;
	private String mProgramName;
	protected TempVarGen mTmpVarGen;
	private Stopwatch mLoopTimer;
	
	
	protected SmtValueOracle mAllZerosOracle;
	protected int mIntNumBits;
	
	private static Logger log = Logger.getLogger(CEGISLoop.class.getCanonicalName());
	
	public CEGISLoop(String programName, CommandLineParamManager params, RecursionControl rControl) {
		mProgramName = programName;
		mParams = params;
		mRControl = rControl;
		
		mTmpVarGen = new TempVarGen("__sa");
		mStat = new CEGISStat();
		mLoopTimer = new Stopwatch();
		
		mIntNumBits = params.flagValue("intbits");
		mAllZerosOracle = new SmtValueOracle.AllZeroOracle();
		
	}

	public void start(NodeToSmtVtype vtype, SMTBackend solver) {
		mLoopTimer.start();
		ArrayList<SmtValueOracle> observations = new ArrayList<SmtValueOracle>();

		SmtValueOracle curOracle = mAllZerosOracle;
		mBestOracle = curOracle;
		
		try {
			
			while (true) {
				log.fine("Iteration: " + mStat.mNumIter);
				
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
				
				mStat.mNumIter++;
			}
			
		} catch (SolverFailedException e) {
			e.printStackTrace();
			mBestOracle = null;
			mLoopTimer.start();
		} catch (IOException e) {
			e.printStackTrace();
			mLoopTimer.start();
		} catch (InterruptedException e) {
			e.printStackTrace();
			mLoopTimer.start();
		} catch (Exception e) {
			e.printStackTrace();
			mLoopTimer.start();
		} finally {
			mLoopTimer.stop();
		}

		
		mStat.mLoop += mLoopTimer.toValue();
	}

	public CEGISStat getStat() {
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
			String tmpFile = mParams.sValue("tmpdir") + File.separator + mProgramName + "-v" + mStat.mNumIter + ".smtlib";
			solver.tmpFilePath = tmpFile;
			
			PrintStream ps = new PrintStream(solver.createStreamToSolver());
			FormulaPrinter printer = vtype.new FormulaPrinter(ps, false);
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
			
		
		SolutionStatistics stat = solver.solve(vtype);
		mStat.mVeriTime += stat.solutionTimeMs();
			
		if (!stat.successful())
			return null;		
		
		return solver.getOracle();
	}
	
	private SmtValueOracle synthesisPE(SMTBackend solver, NodeToSmtVtype vtype, ArrayList<SmtValueOracle> observations)
			throws IOException, InterruptedException, SolverFailedException {
	
		try {
			solver.init();
			String tmpFile = mParams.sValue("tmpdir") + File.separator + mProgramName + "-s" + mStat.mNumIter + ".smtlib";
			solver.tmpFilePath = tmpFile;
			
			PrintStream ps = new PrintStream(solver.createStreamToSolver());
			FormulaPrinter printer = vtype.new FormulaPrinter(ps, true);
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
		mStat.mSynTime += stat.solutionTimeMs();
		if (!stat.successful())
			return null;

		return solver.getOracle();
	}

	public SMTBackend selectBackend(String backend, boolean bv,
			boolean tracing, boolean isSynthesis) throws IOException {
		String tmpFile = mParams.sValue("tmpdir") + File.separator + mProgramName + "-" + 
		(isSynthesis ? "s" : "v") + mStat.mNumIter;
		
		if (mParams.hasFlag("fakesolver")) {
			return new FakeSolverBackend(mParams,
					mParams.hasFlag("keeptmpfiles") ? tmpFile + ".fake" : null,
					mRControl, mTmpVarGen, tracing);
		} else if (backend.equals("cvc3")) {
			if (bv)
				return new Cvc3BVBackend(mParams,
						mParams.hasFlag("keeptmpfiles") ? tmpFile + ".cvc3"
								: null, mRControl, mTmpVarGen, tracing);
			else
				return new Cvc3Backend(mParams,
						mParams.hasFlag("keeptmpfiles") ? tmpFile + ".cvc3"
								: null, mRControl, mTmpVarGen, tracing);
		} else if (backend.equals("cvc3smtlib")) {
			return new Cvc3SMTLIBBackend(
					mParams,
					mParams.hasFlag("keeptmpfiles") ? tmpFile + ".smtlib" : null,
					mRControl, mTmpVarGen, tracing);
		} else if (backend.equals("z3")) {
			return new Z3BVBackend(
					mParams,
					mParams.hasFlag("keeptmpfiles") ? tmpFile + ".smtlib" : null,
					mRControl, mTmpVarGen, tracing);
		} else if (backend.equals("beaver")) {
			return new BeaverBVBackend(
					mParams,
					mParams.hasFlag("keeptmpfiles") ? tmpFile + ".smtlib" : null,
					mRControl, mTmpVarGen, tracing);
		} else if (backend.equals("yices")) {
			return new YicesBVBackend(
					mParams,
					mParams.hasFlag("keeptmpfiles") ? tmpFile + ".smtlib" : null,
					mRControl, mTmpVarGen, tracing);
		} else if (backend.equals("stp")) {
//			return new StpSmtlibBackend(
			return new STPBackend(
					mParams,
					mParams.hasFlag("keeptmpfiles") ? tmpFile + ".stp" : null,
					mRControl, mTmpVarGen, tracing);
		} else {
			// other solvers for example
			return null;
		}
	}
}
