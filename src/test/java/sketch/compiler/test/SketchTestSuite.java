package sketch.compiler.test;

import java.io.File;
import java.util.Arrays;

import junit.framework.Assert;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public abstract class SketchTestSuite extends TestSuite {
    public SketchTestSuite(BasicFileFilter testFilter) throws Exception {
        Assert.assertTrue(testFilter.directory.isDirectory());
        File[] files = testFilter.directory.listFiles();
        Arrays.sort(files);
        for (File subfile : files) {
            String name = subfile.getName();
            if (name.startsWith(testFilter.prefix) && name.endsWith(testFilter.suffix)) {
                addTest(getTestInstance(subfile.getCanonicalPath()));
            }
        }
    }

    public abstract TestCase getTestInstance(String path) throws Exception;
}
