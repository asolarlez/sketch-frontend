package sketch.compiler.codegenerators;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Package;
import sketch.compiler.ast.core.typs.StructDef;
import sketch.compiler.passes.printers.SimpleCodePrinter;

public class GenerateDocs extends FEReplacer {

    PrintStream out;
    SimpleCodePrinter scp;

    public GenerateDocs(String file) throws FileNotFoundException {
        out = new PrintStream(new File(file));
        scp = new SimpleCodePrinter(out, true);
    }

    public void printCleanComment(String comment) {
        if(comment.contains("/**")){
            comment = comment.replaceAll("/[\\*]*", "");
            comment = comment.replaceAll("\n[ \t\r]*[\\*]*", "\n");
            out.println(comment);
        }
    }

    public Object visitFunction(Function f) {
        if (f.hasAnnotation("Private")) {
            return f;
        }
        out.println("\\item ");
        out.println("\\begin{lstlisting}");
        out.println(f.toString());
        out.println("\\end{lstlisting}");
        if (f.getCx().hasComment()) {
            printCleanComment(f.getCx().getComment());
        }
        out.println("  ");
        return f;
    }

    public Object visitStructDef(StructDef sd) {
        if (sd.hasAnnotation("Private")) {
            return sd;
        }
        out.println("\\paragraph{Structure " + sd.getName() + "}");
        out.println("~~~~~~~  ");
        out.println("\\begin{lstlisting}");
        scp.moreIndent().moreIndent();
        sd.accept(scp);
        scp.lessIndent().lessIndent();
        out.println("\\end{lstlisting}");
        if (sd.getCx().hasComment()) {
            printCleanComment(sd.getCx().getComment());
        }
        return sd;
    }

    public Object visitPackage(Package p) {
        out.println("\\subsection{Package " + p.getName() + "}");
        out.println("\\seclabel{lib:" + p.getName() + "}");
        if (p.getCx().hasComment()) {
            printCleanComment(p.getCx().getComment());
        }
        out.println("\\noindent\\textbf{Location : " + p.getCx().getFileName() + "}");

        if (!p.getStructs().isEmpty()) {
            out.println("\\subsubsection{Datastructures}");
            for (StructDef sd : p.getStructs()) {
                sd.accept(this);
            }
        }

        if (!p.getFuncs().isEmpty()) {
            out.println("\\subsubsection{Functions}");
            out.println("\\begin{itemize}");
            for (Function sd : p.getFuncs()) {
                sd.accept(this);
            }
            out.println("\\end{itemize}");
        }

        return p;
    }

}
