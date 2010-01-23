package sketch.compiler.main.other;

import java.io.File;

import sketch.compiler.CommandLineParamManager;
import sketch.compiler.main.seq.SequentialSketchMain;
import sketch.compiler.passes.printers.FunctionDeclPrinter;

public class ParseFunctions extends SequentialSketchMain {
    public ParseFunctions(String[] args) {
        super(args);
    }

    @Override
    public void run() {
        parseProgram();
        FunctionDeclPrinter printer = new FunctionDeclPrinter(new File("function_list.xml"));
        prog.accept(printer);
        printer.write();
    }

    public static void main(String[] args) {
        checkJavaVersion(1, 6);
        try {
            CommandLineParamManager.reset_singleton();
            new ParseFunctions(args).run();
        } catch (RuntimeException e) {
            System.err.println("[ERROR] [SKETCH] Failed with exception "
                    + e.getMessage());
            throw e;
        }
    }
}
