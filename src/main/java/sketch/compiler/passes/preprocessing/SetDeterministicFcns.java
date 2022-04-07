package sketch.compiler.passes.preprocessing;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.Function.FcnSourceDeterministic;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.ExprHole;
import sketch.compiler.passes.annotations.CompilerPassDeps;
import sketch.compiler.passes.structure.ASTObjQuery;
import sketch.util.datastructures.TypedHashSet;

/**
 * set function information as deterministic or not
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
@CompilerPassDeps(runsBefore = {}, runsAfter = { MainMethodCreateNospec.class })
public class SetDeterministicFcns extends FEReplacer {
    public TypedHashSet<Function> determinsticFunctions = new TypedHashSet<Function>();

    public Object visitFunction(Function func) {
        if (func.getInfo().determinsitic == FcnSourceDeterministic.Unknown) {
            final FcnSourceDeterministic deterministicInfo =
                    (new IsNonDeterministicBase()).run(func);
            if (deterministicInfo != FcnSourceDeterministic.Unknown) {
                func = func.creator().deterministicType(deterministicInfo).create();
            }
            if (deterministicInfo == FcnSourceDeterministic.Deterministic) {
                determinsticFunctions.add(func);
            }
        }
        return func;
    };

    @Override
    public Object visitProgram(Program prog) {
        int oldsz = -1;
        Program result = prog;
        while (oldsz != determinsticFunctions.size()) {
            oldsz = determinsticFunctions.size();
            result = (Program) super.visitProgram(result);
        }
        return result;
    }

    public class IsNonDeterministicBase extends ASTObjQuery<FcnSourceDeterministic> {
        public IsNonDeterministicBase() {
            super(FcnSourceDeterministic.Deterministic);
        }

        @Override
        public Object visitExprFunCall(ExprFunCall exp) {
            for (Function f : determinsticFunctions) {
                if (f.getName().equals(exp.getName())) {
                    return exp;
                }
            }
            if (result == FcnSourceDeterministic.Deterministic) {
                result = FcnSourceDeterministic.Unknown;
            }
            return exp;
        }

        public Object visitExprStar(ExprHole star) {
            result = FcnSourceDeterministic.Nondeterministic;
            return star;
        };
    }
}
