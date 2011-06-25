package sketch.util.exceptions;

import static sketch.util.DebugOut.printDebug;

/**
 * An exception that happens in the backend (cegis solver).
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
public class SketchSolverException extends SketchException {
    public SketchSolverException(String msg) {
        super(msg);
    }

    public SketchSolverException(String msg, Throwable base) {
        super(msg, base);
    }

    private static final long serialVersionUID = 2001930087906267129L;
    protected String tmpSketchFilename;

    @Override
    protected String messageClass() {
        return "SketchSolverException";
    }

    public void setBackendTempPath(String tmpSketchFilename) {
        this.tmpSketchFilename = tmpSketchFilename;
    }

    @Override
    public void subclassPrintEnd() {
        super.subclassPrintEnd();
        if (this.tmpSketchFilename != null) {
            printDebug("Backend solver input file at", this.tmpSketchFilename);
        }
    }
}
