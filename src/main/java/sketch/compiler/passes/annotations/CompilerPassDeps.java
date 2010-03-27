package sketch.compiler.passes.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import sketch.compiler.ast.core.FEVisitor;

/**
 * The annotated compiler pass depends on
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface CompilerPassDeps {
    /** what you want to use in most cases */
    Class<? extends FEVisitor>[] runsAfter();

    /**
     * it's less likely, though definitely possible you need to run before a certain stage
     */
    Class<? extends FEVisitor>[] runsBefore();
}
