package sketch.compiler.main.passes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import sketch.compiler.ast.core.FEContext;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Function.FcnInfo;
import sketch.compiler.ast.core.Function.FcnType;
import sketch.compiler.ast.core.Function.FunctionCreator;
import sketch.compiler.ast.core.Package;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.exprs.ExprBinary;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.StmtBlock;
import sketch.compiler.ast.core.stmts.StmtEmpty;
import sketch.compiler.ast.core.stmts.StmtExpr;
import sketch.compiler.ast.core.stmts.StmtIfThen;
import sketch.compiler.passes.annotations.CompilerPassDeps;
import sketch.compiler.passes.lowering.SymbolTableVisitor;
import sketch.compiler.passes.structure.ASTQuery;

@CompilerPassDeps(runsAfter = {}, runsBefore = {})
public class InsertAssumptions extends SymbolTableVisitor {
    private Set<String> topFuncs;
    private List<Expression> assumptions;
    
    public final static String nopName = "_NopFunc";
    private final static FEContext nopContext = new FEContext(nopName, -1);
    
    private StmtExpr nopCall;
    private StmtBlock nopBranch;
    private FcnInfo nopInfo = new FcnInfo(FcnType.Static);
    private FunctionCreator nopCreator = Function.creator(nopContext, nopName,
            FcnType.Static).body(new StmtBlock(new StmtEmpty(nopContext))).params(
            new ArrayList<Parameter>());

    public InsertAssumptions() {
        super(null);
    }

    @Override
    public Object visitStmtBlock(StmtBlock sb) {
        Expression cond = null;
        for (Expression as : assumptions) {
            if (!new ASTQuery() {
                @Override
                public Object visitExprVar(ExprVar v) {
                    if (!symtab.hasVar(v.getName())) {
                        result = true;
                    }
                    return v;
                }
            }.run(as))
            {
                if (cond == null) {
                    cond = as;
                } else {
                    cond = new ExprBinary(cond, "&&", as);
                }
            }
        }
        if (cond == null) {
            return sb;
        } else {
            return new StmtBlock(new StmtIfThen(sb, cond, sb, nopBranch));
        }
    }

    public Function changeFunction(Function f) {
        return (Function) f.accept(this);
    }

    @Override
    public Object visitStreamSpec(Package p) {
        assumptions = p.getAssumptions();
        if (assumptions.isEmpty()) {
            return p;
        }

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
        
        List<Function> newfs = new ArrayList<Function>(fs.size() + 1);
        Function nopFunction = nopCreator.pkg(p.getName()).create();
        nopCall =
                new StmtExpr(new ExprFunCall(nopContext, nopName,
                        new ArrayList<Expression>()));
        nopBranch = new StmtBlock(nopCall);

        newfs.add(nopFunction);
        for (Function f : fs) {
            Function newf = topFuncs.contains(f.getName()) ? changeFunction(f) : f;
            newfs.add(newf);
        }

        return new Package(p, p.getName(), p.getStructs(), p.getVars(), newfs,
                new ArrayList<Expression>());
    }
}
