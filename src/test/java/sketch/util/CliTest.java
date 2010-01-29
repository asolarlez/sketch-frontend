package sketch.util;

import org.junit.Assert;
import org.junit.Test;

import sketch.compiler.cmdline.SemanticsOptions.ArrayOobPolicy;
import sketch.compiler.main.par.ParallelSketchOptions;
import sketch.compiler.main.seq.SequentialSketchOptions;

public class CliTest {
    @Test
    public void seqTest() {
        String[] args = { "--bnd-cbits", "4" };
        final SequentialSketchOptions opts = new SequentialSketchOptions(args);
        Assert.assertTrue(opts.bndOpts.cbits == 4);
        Assert.assertTrue(!opts.bndOpts.incremental.isSet);
    }

    @Test
    public void parTest() {
        String[] args = { "--par-schedlen", "9" };
        final ParallelSketchOptions opts = new ParallelSketchOptions(args);
        Assert.assertTrue((opts.parOpts.schedlen == 9));
    }

    @Test
    public void testEnum() {
        String[] args = { "--sem-array-OOB-policy", "wrsilent_rdzero" };
        final SequentialSketchOptions opts = new SequentialSketchOptions(args);
        Assert.assertTrue((opts.semOpts.arrayOobPolicy == ArrayOobPolicy.wrsilent_rdzero));
    }
    
    @Test
    public void testOptional() {
        String[] args = { "--bnd-incremental", "3" };
        final SequentialSketchOptions opts = new SequentialSketchOptions(args);
        Assert.assertTrue(opts.bndOpts.incremental.isSet);
        Assert.assertTrue(opts.bndOpts.incremental.value.intValue() == 3);
    }
}
