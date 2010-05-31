package sketch.compiler.passes.optimization;

import java.util.Collections;
import java.util.List;
import java.util.Vector;

import sketch.compiler.ast.core.FEContext;
import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.FieldDecl;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.StreamSpec;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.ExprBinary;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.ExprStar;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssert;
import sketch.compiler.ast.core.stmts.StmtBlock;
import sketch.compiler.ast.core.stmts.StmtExpr;
import sketch.compiler.ast.core.stmts.StmtLoop;
import sketch.compiler.ast.core.stmts.StmtMinLoop;
import sketch.compiler.ast.core.stmts.StmtMinimize;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.passes.annotations.CompilerPassDeps;
import sketch.compiler.passes.lowering.GlobalsToParams;
import sketch.compiler.passes.preprocessing.MainMethodCreateNospec;

/**
 * replace special constructs
 * 
 * <pre>
 * minloop {
 *     body
 * }
 * </pre>
 * 
 * with (a bit messy, due to some bugs with globals)
 * 
 * <pre>
 * int bnd1 = ??;
 * int bnd2 = ??;
 * 
 * // loop usage
 * var local = ??;
 * assert local == bnd1;
 * repeat(local) {
 *     body
 * }
 * 
 * void setLoopBounds() {
 *     minimize (bnd1 + bnd2 + ...)
 * }
 * 
 * void myfunction implements ... {
 *     setLoopBounds();
 * }
 * </pre>
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
@CompilerPassDeps(runsAfter = { MainMethodCreateNospec.class }, runsBefore = { GlobalsToParams.class })
public class ReplaceMinLoops extends FEReplacer {
    protected final TempVarGen vargen;
    protected Function minimizeFcn = null;
    protected StmtMinLoop firstMinStmt = null;
    protected Vector<String> glblvars = new Vector<String>();

    public ReplaceMinLoops(TempVarGen varGen) {
        this.vargen = varGen;
    }

    @Override
    public Object visitStreamSpec(StreamSpec spec) {
        spec = (StreamSpec) (new LoopReplacer()).visitStreamSpec(spec);

        if (glblvars.isEmpty()) {
            // don't do anything if there weren't any global functions
            return spec;
        }

        spec = (StreamSpec) super.visitStreamSpec(spec);

        // add global vars
        final Vector<FieldDecl> vars = new Vector<FieldDecl>(spec.getVars());
        for (String name : glblvars) {
            vars.add(new FieldDecl(firstMinStmt, TypePrimitive.inttype, name,
                    new ExprStar(firstMinStmt, 3)));
        }

        // add static minimize statement
        final Vector<Function> fcns = new Vector<Function>(spec.getFuncs());
        fcns.add(getMinimizeFcn());

        return new StreamSpec(spec, spec.getType(), spec.getStreamType(), spec.getName(),
                spec.getParams(), vars, fcns);
    }

    @Override
    public Object visitFunction(Function fcn) {
        fcn = (Function) super.visitFunction(fcn);
        if (fcn.getSpecification() != null) {
            StmtBlock body = (StmtBlock) fcn.getBody();
            Vector<Statement> stmts = new Vector<Statement>(body.getStmts());

            final List<Expression> args = Collections.emptyList();
            final StmtExpr minFcnCall =
                    new StmtExpr(new ExprFunCall(body, getMinimizeFcn().getName(), args));
            stmts.insertElementAt(minFcnCall, 0);
            body = new StmtBlock(stmts);

            return new Function(fcn, fcn.getCls(), fcn.getName(), fcn.getReturnType(),
                    fcn.getParams(), fcn.getSpecification(), body);
        } else {
            return fcn;
        }
    }

    @SuppressWarnings("deprecation")
    public Function getMinimizeFcn() {
        if (minimizeFcn == null) {
            final FEContext ctx =
                    FEContext.artificalFrom("global_init_fcn", firstMinStmt);

            String fcnName = vargen.nextVar("minLoopInit");

            Expression expr = new ExprVar(ctx, glblvars.get(0));
            for (String name : glblvars.subList(1, glblvars.size())) {
                expr = new ExprBinary(new ExprVar(ctx, name), "+", expr);
            }

            StmtBlock body = new StmtBlock(new StmtMinimize(expr));

            final List<Parameter> params = Collections.emptyList();
            minimizeFcn =
                    Function.newStatic(ctx, fcnName, TypePrimitive.voidtype, params,
                            null, body);
        }
        return minimizeFcn;
    }

    public class LoopReplacer extends FEReplacer {
        @Override
        public Object visitStmtMinLoop(StmtMinLoop stmtMinLoop) {
            if (firstMinStmt == null) {
                firstMinStmt = stmtMinLoop;
            }
            String name = vargen.nextVar("bnd");
            String localName = vargen.nextVar("bndlocal");
            glblvars.add(name);
            final StmtVarDecl vardecl =
                    new StmtVarDecl(stmtMinLoop, TypePrimitive.inttype, localName,
                            new ExprStar(stmtMinLoop));
            final StmtAssert stmtAssert =
                    new StmtAssert(new ExprBinary(new ExprVar(stmtMinLoop, name), "==",
                            new ExprVar(stmtMinLoop, localName)), false);
            this.addStatement(vardecl);
            this.addStatement(stmtAssert);
            return new StmtLoop(stmtMinLoop, new ExprVar(stmtMinLoop, localName),
                    (Statement) stmtMinLoop.getBody().accept(this));
        }
    }
}
