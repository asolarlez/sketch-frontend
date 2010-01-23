package sketch.compiler.passes.printers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Vector;

import sketch.compiler.ast.core.FEContext;
import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.util.DebugOut;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

/**
 * print function declarations
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you
 *          make changes, please consider contributing back!
 */
public class FunctionDeclPrinter extends FEReplacer {
    public Vector<FunctionInfo> values;
    public File path;

    public FunctionDeclPrinter(File outfile) {
        path = outfile;
        if (outfile.isFile()) {
            values = read();
        } else {
            values = new Vector<FunctionInfo>();
        }
    }

    /** load XML document nodes from file */
    @SuppressWarnings("unchecked")
    protected Vector<FunctionInfo> read() {
        try {
            XStream xs = getXstream();
            InputStream in = path.toURI().toURL().openStream();
            return (Vector<FunctionInfo>) xs.fromXML(in);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
            return null;
        }
    }

    public void write() {
        try {
            getXstream().toXML(values, new FileOutputStream(path));
        } catch (Exception e) {
            DebugOut.print_exception("reading file", e);
        }
    }
    
    public XStream getXstream() {
        XStream xs = new XStream(new DomDriver());
        xs.alias("fcn-info", FunctionInfo.class);
        return xs;
    }

    @Override
    public Object visitFunction(Function func) {
        FEContext cx = func.getCx();
        values.add(new FunctionInfo(cx.getFileName(), cx.getLineNumber(), func
                .getName(), func.getSpecification(), !func.isStatic()));
        return super.visitFunction(func);
    }

    public static class FunctionInfo {
        public final int lineNum;
        public final String srcFile;
        public final String name;
        public final String impl;
        public final boolean isGenerator;

        public FunctionInfo(String srcFile, int lineNum, String name,
                String impl, boolean isGenerator)
        {
            this.srcFile = srcFile;
            this.lineNum = lineNum;
            this.name = name;
            this.impl = impl;
            this.isGenerator = isGenerator;
        }
    }
}
