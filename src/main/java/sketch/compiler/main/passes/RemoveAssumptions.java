package sketch.compiler.main.passes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Package;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtBlock;
import sketch.compiler.ast.core.stmts.StmtExpr;
import sketch.compiler.ast.core.stmts.StmtIfThen;
import sketch.compiler.passes.annotations.CompilerPassDeps;

@CompilerPassDeps(runsAfter = {}, runsBefore = {})
public class RemoveAssumptions extends FEReplacer {
    private Set<String> topFuncs;
    private List<Expression> assumptions;
    
    public final static String nopName = InsertAssumptions.nopName;

    @Override
    public Object visitStmtBlock(StmtBlock sb) {
        if (sb.getStmts().size() == 1 && sb.getStmts().get(0) instanceof StmtIfThen) {
            StmtIfThen s = (StmtIfThen) sb.getStmts().get(0);
            if (s.getAlt() instanceof StmtBlock &&
                    ((StmtBlock) s.getAlt()).getStmts().size() == 1)
            {
                Statement t = ((StmtBlock) s.getAlt()).getStmts().get(0);
                if (t instanceof StmtExpr &&
                        ((StmtExpr) t).getExpression() instanceof ExprFunCall)
                {
                    ExprFunCall c = (ExprFunCall) ((StmtExpr) t).getExpression();
                    if (c.getName() == nopName) {
                        return new StmtBlock(s.getCons());
                    }
                }
            }
        }
        return sb;
    }

    public Function changeFunction(Function f) {
        return (Function) f.accept(this);
    }

    @Override
    public Object visitStreamSpec(Package p) {
        List<Function> fs = p.getFuncs();
        topFuncs = new HashSet<String>(fs.size());
        for (Function f : fs) {
            if (f.isSketchHarness()) {
                topFuncs.add(f.getName());
            }
            String g = f.getSpecification();
            if (g != null) {
                topFuncs.add(f.getName());
                topFuncs.add(g);
            }
        }
        
        List<Function> newfs = new ArrayList<Function>(fs.size());
        for (Function f : fs) {
            if (f.getName() != nopName) {
                Function newf = topFuncs.contains(f.getName()) ? changeFunction(f) : f;
                newfs.add(newf);
            }
        }

        return new Package(p, p.getName(), p.getStructs(), p.getVars(), newfs,
                new ArrayList<Expression>());
    }
}
