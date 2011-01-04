package sketch.compiler.main.cuda;

import sketch.compiler.main.seq.SequentialSketchOptions;
import sketch.util.cli.SketchCliParser;
import sketch.util.cuda.CudaThreadBlockDim;

public class CudaSketchOptions extends SequentialSketchOptions {
    protected CudaOptions cudaOpts;

    public CudaSketchOptions(final String[] inArgs) {
        super(inArgs);
    }

    @Override
    public void preinit() {
        // this.solverOpts.synth = SynthSolvers.ABC;
        // this.solverOpts.verif = VerifSolvers.ABC;
        // this.bndOpts.unrollAmnt = 32;
        super.preinit();
    }

    @Override
    public void parseCommandline(final SketchCliParser parser) {
        this.cudaOpts = new CudaOptions();
        this.cudaOpts.parse(parser);
        super.parseCommandline(parser);
    }

    public static CudaSketchOptions getSingleton() {
        assert SequentialSketchOptions._singleton != null : "no singleton instance";
        return (CudaSketchOptions) SequentialSketchOptions._singleton;
    }

    @Override
    public CudaThreadBlockDim getCudaBlockDim() {
        return this.cudaOpts.threadBlockDim;
    }
}
