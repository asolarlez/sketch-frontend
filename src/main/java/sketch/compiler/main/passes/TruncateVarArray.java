package sketch.compiler.main.passes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Package;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.ExprArrayRange;
import sketch.compiler.ast.core.exprs.ExprBinary;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssume;
import sketch.compiler.ast.core.stmts.StmtBlock;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.main.cmdline.SketchOptions;

public class TruncateVarArray extends FEReplacer {
    private int arrSize;
    private TempVarGen varGen;
    private final List<Expression> constInts = new ArrayList<Expression>();

    public TruncateVarArray(SketchOptions options, TempVarGen varGen) {
        this.arrSize = options.bndOpts.arrSize;
        this.varGen = varGen;
        for (int i = 0; i < arrSize; ++i) {
            constInts.add(new ExprConstInt(i));
        }
    }

    @Override
    public Object visitStreamSpec(Package p) {
        HashSet<String> tochange = new HashSet<String>();
        List<Function> funcs = p.getFuncs();
        for (int i = 0; i < funcs.size(); ++i) {
            Function f = funcs.get(i);
            String spec = f.getSpecification();
            if (spec != null) {
                tochange.add(spec);
            }
        }

        for (int i = 0; i < funcs.size(); ++i) {
            Function f = funcs.get(i);
            String name = f.getName();
            if (tochange.contains(name) || f.getFcnType() == Function.FcnType.Harness) {
                f = addAssumes(f);
                funcs.set(i, f);
            }
        }

        return p;
    }

    private Function addAssumes(Function f) {
        Map<Expression, Expression> lenVars = new HashMap<Expression, Expression>();
        List<Statement> stmts = new ArrayList<Statement>();
        for (Parameter p : f.getParams()) {
            Type t = p.getType();
            if (t.isArray()) {
                TypeArray ta = (TypeArray) t;
                Type tb = ta.getBase();
                if (!tb.isArray()) {
                    Expression len = ta.getLength();
                    if (!len.isConstant()) {
                        Expression v = lenVars.get(len);
                        if (v == null) {
                            String vname = varGen.nextVar("valen_" + p.getName());
                            v = new ExprVar(p, vname);
                            lenVars.put(len, v);
                            stmts.add(new StmtVarDecl(p, TypePrimitive.inttype, vname,
                                    len));
                        }
                        Expression zero = tb.defaultValue();
                        Expression arr = new ExprVar(p, p.getName());
                        for (int j=1; j<arrSize; ++j) {
                            Expression jexp = constInts.get(j);
                            Expression cond1 =
                                    new ExprBinary(p, ExprBinary.BINOP_GT, v, jexp);
                            Expression cond2 =
                                    new ExprBinary(p, ExprBinary.BINOP_EQ, zero,
                                            new ExprArrayRange(arr, jexp));
                            stmts.add(new StmtAssume(p, new ExprBinary(p,
                                    ExprBinary.BINOP_OR, cond1, cond2), "trunc " +
                                    p.getName() +
                                    " " + j));
                        }
                    }
                }
            }
        }

        if (stmts.isEmpty()) {
            return f;
        } else {
            stmts.addAll(((StmtBlock) f.getBody()).getStmts());
            return f.creator().body(new StmtBlock(stmts)).create();
        }
    }
}
