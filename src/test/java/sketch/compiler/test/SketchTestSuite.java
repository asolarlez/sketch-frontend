package sketch.compiler.test;

import static sketch.compiler.main.PlatformLocalization.path;

import java.io.File;
import java.util.Arrays;

import junit.framework.Assert;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public abstract class SketchTestSuite extends TestSuite {
    public SketchTestSuite(String test_type) throws Exception {
        File directory = path("src", "test", "sk", test_type);
        Assert.assertTrue(directory.isDirectory());
        File[] files = directory.listFiles();
        Arrays.sort(files);
        for (File subfile : files) {
            String name = subfile.getName();
            if (name.startsWith("miniTest") && name.endsWith(".sk")) {
                addTest(getTestInstance(subfile.getCanonicalPath()));
            }
        }
    }

    public abstract TestCase getTestInstance(String path) throws Exception;
}
