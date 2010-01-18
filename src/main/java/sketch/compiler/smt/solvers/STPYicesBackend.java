package sketch.compiler.smt.solvers;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import sketch.compiler.CommandLineParamManager;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.dataflow.recursionCtrl.RecursionControl;
import sketch.compiler.smt.SolverFailedException;
import sketch.compiler.smt.partialeval.FormulaPrinter;
import sketch.compiler.smt.partialeval.NodeToSmtVtype;
import sketch.compiler.smt.partialeval.SmtValueOracle;
import sketch.compiler.solvers.SolutionStatistics;
import sketch.util.SynchronousTimedProcess;


public class STPYicesBackend extends SMTBackend {
    
    STPBackend backend1;
    YicesBVBackend backend2;
    
    boolean isBackend1 = false;
    
    public STPYicesBackend(CommandLineParamManager params, String tmpFilePath,
            RecursionControl rcontrol, TempVarGen varGen, boolean tracing)
            throws IOException
    {
        super(params, tmpFilePath, rcontrol, varGen, tracing);
        solverPath = params.sValue("smtpath");
        int idx = solverPath.indexOf(File.pathSeparator);
        
        backend1 = new STPBackend(params, tmpFilePath, rcontrol, varGen, tracing);
        backend2 = new YicesBVBackend(params, tmpFilePath, rcontrol, varGen, 2, tracing);
        backend1.solverPath = solverPath.substring(0, idx);
        backend2.solverPath = solverPath.substring(idx+1, solverPath.length());
    }
    
    @Override
    public void init() {
        isBackend1 = !isBackend1;
        if (isBackend1)
            backend1.init();
        else
            backend2.init();
    }

    @Override
    protected FormulaPrinter createFormulaPrinterInternal(
            NodeToSmtVtype formula, PrintStream ps)
    {
        if (isBackend1)
            return backend1.createFormulaPrinter(formula, ps);
        else
            return backend2.createFormulaPrinter(formula, ps);
    }

    @Override
    protected SynchronousTimedProcess createSolverProcess() throws IOException {
        if (isBackend1)
            return backend1.createSolverProcess();
        else
            return backend2.createSolverProcess();
    }

    @Override
    public OutputStream createStreamToSolver() throws IOException {
        if (isBackend1)
            return backend1.createStreamToSolver();
        else
            return backend2.createStreamToSolver();
    }

    @Override
    public SmtValueOracle createValueOracle() {
        if (isBackend1)
            return backend1.createValueOracle();
        else
            return backend2.createValueOracle();
    }

    @Override
    public SolutionStatistics solve(NodeToSmtVtype formula) throws IOException,
            InterruptedException, SolverFailedException
    {
        SolutionStatistics stat;
        if (isBackend1)
            stat = backend1.solve(formula);
        else
            stat = backend2.solve(formula);
        
        return stat;
    }
    
    @Override
    public SmtValueOracle getOracle() {
        if (isBackend1)
            return backend1.getOracle();
        else
            return backend2.getOracle();
    }
}
