package sketch.compiler.passes.preprocessing;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import sketch.compiler.ast.core.FEContext;
import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Function.FcnType;
import sketch.compiler.ast.core.Function.PrintFcnType;
import sketch.compiler.ast.core.Package;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtBlock;
import sketch.compiler.ast.core.stmts.StmtExpr;
import sketch.compiler.passes.annotations.CompilerPassDeps;

/**
 * create an artificial nospec() function if there is a main function present (that
 * doesn't have a specification).
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
@CompilerPassDeps(runsAfter = {}, runsBefore = {})
public class MainMethodCreateNospec extends FEReplacer {
    public final Vector<Function> mainFcns = new Vector<Function>();
    public final Random rand = new Random();

    public MainMethodCreateNospec() {}

    @Override
    public Object visitStreamSpec(Package spec) {
        // see super for how to create a new one
        mainFcns.clear();
        spec = (Package) super.visitStreamSpec(spec);
        if (!mainFcns.isEmpty()) {
            ArrayList<Function> newFcns = new ArrayList<Function>();
            for (Function f : spec.getFuncs()) {
                if (f.getInfo().printType == PrintFcnType.Default) {
                    newFcns.add(f);
                } else {
                    newFcns.add(f.creator().printType(PrintFcnType.Default).create());
                }
            }
            for (Function mainFcn : mainFcns) {
                Function wrapperFcn = getWrapperFunction(mainFcn);
                newFcns.add(wrapperFcn);
                newFcns.add(getNospecFunction(wrapperFcn));
            }
            return new Package(spec, spec.getName(), spec.getStructs(),
                    spec.getVars(),
                    Collections.unmodifiableList(newFcns));
        } else {
            return spec;
        }
    }

    protected Function getNospecFunction(Function mainWrapperFcn) {
        final FEContext ctx = FEContext.artificalFrom("nospec", mainWrapperFcn);
        return Function.creator(ctx, mainWrapperFcn.getSpecification(), FcnType.Static).body(
                new StmtBlock(ctx)).params(unrefParams(mainWrapperFcn.getParams())).pkg(
                mainWrapperFcn.getPkg()).create();
    }

    List<Parameter> unrefParams(List<Parameter> pl) {
        List<Parameter> lp = new ArrayList<Parameter>();
        for (Parameter p : pl) {
            if (p.isParameterOutput()) {
                lp.add(new Parameter(p, p.getType(), p.getName(), Parameter.IN));
            } else {
                lp.add(p);
            }
        }
        return lp;
    }

    @SuppressWarnings( { "deprecation" })
    protected Function getWrapperFunction(Function mainFcn) {
        final FEContext artificalFrom = FEContext.artificalFrom("mainwrapper", mainFcn);
        Vector<Statement> stmts = new Vector<Statement>();
        Vector<Expression> vars = new Vector<Expression>();
        for (Parameter param : mainFcn.getParams()) {
            vars.add(new ExprVar(artificalFrom, param.getName()));
        }
        stmts.add(new StmtExpr(new ExprFunCall(artificalFrom, mainFcn.getName(),
                Collections.unmodifiableList(vars))));
        return Function.creator(artificalFrom, mainFcn.getName() + "__Wrapper",
                FcnType.Static).params(unrefParams(mainFcn.getParams())).spec(
                mainFcn.getName() + "__WrapperNospec").body(
                new StmtBlock(artificalFrom, stmts)).printType(
                mainFcn.getInfo().printType).pkg(mainFcn.getPkg()).create();
    }

    @Override
    public Object visitFunction(Function func) {
        if (func.isSketchHarness()) {
            Function staticReplacement =
                    func.creator().spec(null).type(FcnType.Static).create();
            mainFcns.add(staticReplacement);
            return super.visitFunction(staticReplacement);
        }
        return super.visitFunction(func);
    }
}
