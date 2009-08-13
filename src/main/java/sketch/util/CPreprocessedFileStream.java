/**
 *
 */
package sketch.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.anarres.cpp.LexerException;
import org.anarres.cpp.Preprocessor;
import org.anarres.cpp.Token;

/**
 * A file input stream that uses the jcpp library
 * @author <a href="mailto:cgjones@cs.berkeley.edu">Chris Jones</a>
 * @author gatoatigrado (nicholas tung) [ntung at ntung] rewrote to use jcpp
 */
public class CPreprocessedFileStream extends ByteArrayInputStream {
    public CPreprocessedFileStream(String filename, List<String> cppDefs)
            throws IOException, LexerException
    {
        super(preprocessFile(filename, cppDefs).getBytes());
    }

    public static String preprocessFile(String filename,
            List<String> additionalDefs) throws IOException, LexerException
    {
        Preprocessor preproc = new Preprocessor(new File(filename));
        ArrayList<String> cppDefs = new ArrayList<String>();
        cppDefs.add("__SKETCH__");
        if (additionalDefs != null) {
            cppDefs.addAll(additionalDefs);
        }
        for (String macro : cppDefs) {
            // these 5 lines taken from cpp/Main.java
            int idx = macro.indexOf('=');
            if (idx == -1) {
                System.out.println("adding macro " + macro);
                preproc.addMacro(macro);
            } else {
                System.out.println("adding macro " + macro.substring(0, idx)
                        + ", " + macro.substring(idx + 1));
                preproc.addMacro(macro.substring(0, idx), macro
                        .substring(idx + 1));
            }
        }
        StringBuilder result_builder = new StringBuilder();
        while (true) {
            Token tok = preproc.token();
            if (tok == null || tok.getType() == Token.EOF) {
                break;
            }
            result_builder.append(tok.getText());
        }
        return result_builder.toString();
    }
}
