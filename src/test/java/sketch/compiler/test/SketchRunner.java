/**
 * 
 */
package sketch.compiler.test;

import junit.framework.TestCase;

import org.junit.internal.runners.statements.FailOnTimeout;
import org.junit.internal.runners.statements.InvokeMethod;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

/**
 * junit test case to run a sketch
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you
 *          make changes, please consider contributing back!
 */
public abstract class SketchRunner extends TestCase {
    public static final long timeout = 60000;
    public final String path;
    public Statement stmt;

    public SketchRunner(final String path) throws Exception {
        this.path = path;
        this.stmt =
                new InvokeMethod(new FrameworkMethod(getClass().getMethod(
                        "runTestStatement")), this);
        this.stmt = new FailOnTimeout(stmt, timeout);
        setName(toString());
    }

    @Override
    public String toString() {
        return getClass().getName() + " [path=" + path + "]";
    }

    @Override
    protected final void runTest() throws Throwable {
        this.stmt.evaluate();
    }

    public void runTestStatement() {
        System.err.println("[SKETCH] running test " + path);
        String[] args = { path };
        runSketch(args);
    }

    protected abstract void runSketch(String[] args);
}
