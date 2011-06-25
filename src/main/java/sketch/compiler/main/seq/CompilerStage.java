package sketch.compiler.main.seq;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Vector;

import sketch.compiler.ast.core.FEVisitor;
import sketch.compiler.ast.core.Program;
import sketch.compiler.passes.annotations.CompilerPassDeps;
import sketch.util.datastructures.HashmapList;

import static sketch.util.DebugOut.printDebug;
import static sketch.util.DebugOut.printNote;

import static sketch.util.Misc.nonnull;

/**
 * compiler stage with annotation-based dependency declarations
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 */
public abstract class CompilerStage {
    protected final SequentialSketchMain sketch;
    protected final String name;

    /**
     * @param sequentialSketchMain
     */
    CompilerStage(SequentialSketchMain sequentialSketchMain) {
        sketch = sequentialSketchMain;
        this.name = this.getClass().getSimpleName();
    }

    public Vector<FEVisitor> passes = new Vector<FEVisitor>();
    /** pass $1 depends on pass $2, i.e. $2 must run before $1 */
    protected HashmapList<FEVisitor, FEVisitor> stageRequires =
            new HashmapList<FEVisitor, FEVisitor>();
    protected Vector<FEVisitor> linearizedStages = new Vector<FEVisitor>();

    protected void addPasses(FEVisitor... passes2) {
        passes.addAll(Arrays.asList(passes2));
    }

    /** you probably don't want to modify this */
    public Program run(Program prog) {
        if (sketch.options.debugOpts.printPasses) {
            printNote("Running stage", name);
        }
        generateDeps();
        assert linearizedStages.size() == passes.size();
        for (FEVisitor pass : linearizedStages) {
            String passName = pass.getClass().getSimpleName();
            if (sketch.options.debugOpts.printPasses) {
                printNote("Running pass", pass.getClass().getSimpleName());
            }
            // System.out.println("[Stage " + this.getClass().getSimpleName() +
            // "] running pass " + pass.getClass().getSimpleName());
            final CompilerPassDeps passInfo = getPassInfo(pass);
            if (passInfo.debug() ||
                    sketch.options.debugOpts.dumpBefore.contains(passName))
            {
                prog.debugDump("Before pass " + pass.getClass().getSimpleName());
            }
            prog = (Program) prog.accept(pass);
            if (passInfo.debug() || sketch.options.debugOpts.dumpAfter.contains(passName))
            {
                prog.debugDump("After pass " + pass.getClass().getSimpleName());
            }
            sketch.runClasses.add(pass.getClass());
        }
        prog = postRun(prog);
        return prog;
    }

    protected Program postRun(Program prog) {
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
                    if (!sketch.runClasses.contains(passBeforeCurr)) {
                        printDebug("Broken check: required pass " +
                                passBeforeCurr.getSimpleName() + " not run before " +
                                pass.getClass().getSimpleName() + "!");
                    }
                }
            }
            // opposite dependencies
            for (Class<? extends FEVisitor> passAfterCurr : deps.runsBefore()) {
                final FEVisitor dependingObj = objectsByClass.get(passAfterCurr);
                if (dependingObj != null) {
                    stageRequires.append(dependingObj, pass);
                } else {
                    if (sketch.runClasses.contains(passAfterCurr)) {
                        printDebug("Broken check: pass " + passAfterCurr.getSimpleName() +
                                " already run before " + pass.getClass().getSimpleName() +
                                "!");
                    }
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