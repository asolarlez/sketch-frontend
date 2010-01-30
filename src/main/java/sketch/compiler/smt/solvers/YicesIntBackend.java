package sketch.compiler.smt.solvers;

import java.io.IOException;
import java.io.PrintStream;

import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.dataflow.recursionCtrl.RecursionControl;
import sketch.compiler.main.seq.SMTSketchOptions;
import sketch.compiler.smt.partialeval.FormulaPrinter;
import sketch.compiler.smt.partialeval.NodeToSmtVtype;
import sketch.compiler.smt.smtlib.SMTLIBTranslatorInt;

public class YicesIntBackend extends YicesBVBackend {

    public YicesIntBackend(SMTSketchOptions options, String tmpFilePath,
            RecursionControl rcontrol, TempVarGen varGen, int version,
            boolean tracing) throws IOException
    {
        super(options, tmpFilePath, rcontrol, varGen, version, tracing);
        // TODO Auto-generated constructor stub
    }
    
    @Override
    protected FormulaPrinter createFormulaPrinterInternal(NodeToSmtVtype formula,
            PrintStream ps)
    {
        return new SMTLIBTranslatorInt(formula, ps);
        
    }
}
