package sketch.compiler.passes.optimization;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.ExprStar;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtBlock;
import sketch.compiler.ast.core.stmts.StmtIfThen;
import sketch.compiler.ast.core.stmts.StmtLoop;
import sketch.compiler.ast.core.stmts.StmtMinLoop;
import sketch.compiler.ast.core.stmts.StmtMinimize;
import sketch.compiler.ast.core.stmts.StmtReturn;
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

    // protected StmtMinLoop firstMinStmt = null;

    public ReplaceMinLoops(TempVarGen varGen) {
        this.vargen = varGen;
    }

    @Override
    public Object visitStmtMinLoop(StmtMinLoop stmtMinLoop) {
		Statement body = stmtMinLoop.getBody();
		if (cannotBeRepeated(body)) {
			return body.accept(this);
		}
        String localName = vargen.nextVar("bndlocal");

        final StmtVarDecl vardecl =
                new StmtVarDecl(stmtMinLoop, TypePrimitive.inttype, localName,
                        new ExprStar(stmtMinLoop));
        final ExprVar ev = new ExprVar(stmtMinLoop, localName);
        final StmtMinimize smin = new StmtMinimize(ev, false);
        this.addStatement(vardecl);
        this.addStatement(smin);
        return new StmtLoop(stmtMinLoop, new ExprVar(stmtMinLoop, localName),
				(Statement) body.accept(this));
    }
    
	// A statement cannot be repeated if it definitely returns
	private boolean cannotBeRepeated(Statement stmt) {
		if (stmt == null) return false;
		if (stmt.isBlock()) {
			StmtBlock bl = (StmtBlock) stmt;
			for (int i = 0; i < bl.getStmts().size(); i++) {
				if (cannotBeRepeated(bl.getStmts().get(i))) return true;
			}
		} else {
			if (stmt instanceof StmtReturn) {
				return true;
			}
			if (stmt instanceof StmtIfThen) {
				StmtIfThen sif = (StmtIfThen) stmt;
				Expression cond = sif.getCond();
				if (cond.isConstant()) {
					if (cond.getIValue() == 1 && cannotBeRepeated(sif.getCons())) 
						return true;
					if (cond.getIValue() == 0 && cannotBeRepeated(sif.getAlt()))
						return true;
				} else {
					if (cannotBeRepeated(sif.getCons())
							&& cannotBeRepeated(sif.getAlt()))
						return true;
				}
			}
		}
		return false;
	}
}
