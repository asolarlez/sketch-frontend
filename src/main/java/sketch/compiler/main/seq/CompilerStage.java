package sketch.compiler.main.seq;

import static sketch.util.Misc.nonnull;

import java.util.HashMap;
import java.util.Vector;

import sketch.compiler.ast.core.FEVisitor;
import sketch.compiler.ast.core.Program;
import sketch.compiler.passes.annotations.CompilerPassDeps;
import sketch.util.datastructures.HashmapList;

/**
 * compiler stage with annotation-based dependency declarations
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 */
public abstract class CompilerStage {
    protected final SequentialSketchMain sketch;

    /**
     * @param sequentialSketchMain
     */
    CompilerStage(SequentialSketchMain sequentialSketchMain) {
        sketch = sequentialSketchMain;
    }

    public FEVisitor[] passes;
    protected HashmapList<FEVisitor, FEVisitor> stageRequires =
            new HashmapList<FEVisitor, FEVisitor>();
    protected Vector<FEVisitor> linearizedStages = new Vector<FEVisitor>();

    /** you probably don't want to modify this */
    public Program run(Program prog) {
        generateDeps();
        assert linearizedStages.size() == passes.length;
        for (FEVisitor pass : linearizedStages) {
            // System.out.println("[Stage " + this.getClass().getSimpleName() +
            // "] running pass " + pass.getClass().getSimpleName());
            final CompilerPassDeps passInfo = getPassInfo(pass);
            if (passInfo.debug()) {
                prog.debugDump("Before pass " + pass.getClass().getSimpleName());
            }
            prog = (Program) prog.accept(pass);
            if (passInfo.debug()) {
                prog.debugDump("After pass " + pass.getClass().getSimpleName());
            }
            sketch.runClasses.add(pass.getClass());
        }
        return prog;
    }

    protected void generateDeps() {
        // build a reverse list
        HashMap<Class<?>, FEVisitor> objectsByClass = new HashMap<Class<?>, FEVisitor>();
        for (FEVisitor pass : passes) {
            if (objectsByClass.put(pass.getClass(), pass) != null) {
                throw new RuntimeException(
                        "compiler stage provides multiple objects of type " +
                                pass.getClass());
            }
        }
        for (FEVisitor pass : passes) {
            final CompilerPassDeps deps = getPassInfo(pass);
            for (Class<? extends FEVisitor> passBeforeCurr : deps.runsAfter()) {
                final FEVisitor dependentObj = objectsByClass.get(passBeforeCurr);
                if (dependentObj != null) {
                    stageRequires.append(pass, dependentObj);
                } else {
                    assert sketch.runClasses.contains(passBeforeCurr) : "required pass " +
                            passBeforeCurr.getSimpleName() + " not run before " +
                            pass.getClass().getSimpleName() + "!";
                }
            }
            // opposite dependencies
            for (Class<? extends FEVisitor> passAfterCurr : deps.runsBefore()) {
                final FEVisitor dependingObj = objectsByClass.get(passAfterCurr);
                if (dependingObj != null) {
                    stageRequires.append(dependingObj, pass);
                } else {
                    assert !sketch.runClasses.contains(passAfterCurr) : "pass " +
                            passAfterCurr.getSimpleName() + " already run before " +
                            pass.getClass().getSimpleName() + "!";
                }
            }
        }
        for (FEVisitor pass : passes) {
            addPassToVector(pass);
        }
    }

    protected CompilerPassDeps getPassInfo(FEVisitor pass) {
        return nonnull(pass.getClass().getAnnotation(CompilerPassDeps.class), "stage " +
                pass.getClass() + " needs to be annotated.");
    }

    protected void addPassToVector(FEVisitor pass) {
        if (!linearizedStages.contains(pass)) {
            for (FEVisitor dep : stageRequires.getOrEmpty(pass)) {
                addPassToVector(dep);
            }
            linearizedStages.add(pass);
        }
    }
}