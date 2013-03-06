package sketch.compiler.stencilSK;

import java.util.ArrayList;
import java.util.List;

import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtBlock;
import sketch.compiler.passes.lowering.SymbolTableVisitor;

/*
 * Currently treat all structs as final After PreprocessSketch all function calls inlined.
 * top level stencil function cannot input / output structs Not supported: pointer
 * assignment (struct X {} X p = ...; X q; q = p) array of structs struct in struct
 * (struct X { struct y; }) bugs: now fields are emitted in the order they are stored in
 * the map. but the correct order is to emit non-arrays first, then arrays, so that the
 * array size variable will occur before the array declaration
 */

public class EliminateReferenceParameters extends SymbolTableVisitor {
    private TempVarGen varGen;

    public EliminateReferenceParameters(TempVarGen varGen_) {
        super(null);
        varGen = varGen_;
    }

    @Override
    public Object visitFunction(Function func) {
        List<Parameter> params = new ArrayList<Parameter>();
        boolean changed = false;
        List<Statement> newbody = null;
        for (Parameter param : func.getParams()) {
            if (param.isParameterReference()) {
                if (changed) {
                    assert false : "Top level stencil annot have two reference parameters!";
                }
                changed = true;
                String newName = varGen.nextVar(param.getName());
                Parameter newParam =
                        new Parameter(param, param.getType(), newName, Parameter.IN);
                params.add(newParam);
                params.add(new Parameter(param, param.getType(), param.getName(),
                        Parameter.OUT));
                StmtBlock body = (StmtBlock) func.getBody();
                StmtAssign assn =
                        new StmtAssign(new ExprVar(func, param.getName()), new ExprVar(
                                func, newName));
                newbody =
                        new ArrayList<Statement>(body.getStmts().size() + 1);
                newbody.add(assn);
                newbody.addAll(body.getStmts());
            } else {
                params.add(param);
            }
        }
        if (changed) {
            return func.creator().params(params).body(new StmtBlock(newbody)).create();
        } else {
            return func;
        }
    }
}
