package sketch.compiler.passes.cuda;

import static sketch.util.Misc.nonnull;

import java.util.Vector;

import sketch.compiler.Directive.InstrumentationDirective;
import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.ExprArrayRange;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.ExprNew;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtBlock;
import sketch.compiler.ast.core.stmts.StmtExpr;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.TypeStructRef;
import sketch.compiler.ast.cuda.exprs.CudaInstrumentCall;
import sketch.compiler.passes.annotations.CompilerPassDeps;
import sketch.compiler.passes.lowering.GlobalsToParams;
import sketch.compiler.passes.structure.CallGraph;
import sketch.util.datastructures.TypedHashMap;
import sketch.util.exceptions.ExceptionAtNode;

/**
 * lower CudaInstrument AST nodes to reads and writes. hopefully they're more primitive by
 * now.
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
@CompilerPassDeps(runsBefore = { GenerateAllOrSomeThreadsFunctions.class, GlobalsToParams.class }, runsAfter = { })
public class LowerInstrumentation extends FEReplacer {
    protected final TypedHashMap<String, InstrumentationDirective> directivesByName =
            new TypedHashMap<String, InstrumentationDirective>();
    protected final TempVarGen varGen;

    protected CallGraph callGraph;
    protected InstrumentationDirective activeInstrumentation;
    protected String instrumentationStructInstName;
    protected String instrumentedVar;
    protected ExprVar instrumentationStructInst;

    public LowerInstrumentation(TempVarGen varGen,
            Vector<InstrumentationDirective> directives)
    {
        this.varGen = varGen;
        for (InstrumentationDirective d : directives) {
            directivesByName.put(d.name, d);
        }
    }

    @Override
    public Object visitProgram(Program prog) {
        this.callGraph = new CallGraph(prog);
        return super.visitProgram(prog);
    }

    @Override
    public Object visitStmtBlock(StmtBlock stmt) {
        StmtBlock newBlock = (StmtBlock) super.visitStmtBlock(stmt);
        if (this.activeInstrumentation != null) {
            Vector<Statement> stmts = new Vector<Statement>(newBlock.getStmts());
            stmts.add(new StmtExpr(new ExprFunCall(stmt, activeInstrumentation.end,
                    instrumentationStructInst)));
            newBlock = new StmtBlock(newBlock, stmts);
        }
        this.activeInstrumentation = null;
        return newBlock;
    }

    @Override
    public Object visitStmtAssign(StmtAssign stmt) {
        if (activeInstrumentation != null) {
            if (stmt.getLHS() instanceof ExprArrayRange) {
                ExprArrayRange access = (ExprArrayRange) stmt.getLHS();
                if (access.getAbsoluteBase().getName().equals(instrumentedVar)) {
                    if (!(access.getBase() instanceof ExprVar)) {
                        throw new ExceptionAtNode("multi-dimensional array "
                                + "instrumentation is not yet supported", stmt);
                    }
                    addExprStatement(new ExprFunCall(stmt, activeInstrumentation.write,
                            instrumentationStructInst, access.getOffset()));
                }
            }
        }
        return super.visitStmtAssign(stmt);
    }

    @Override
    public Object visitCudaInstrumentCall(CudaInstrumentCall instrumentCall) {
        final InstrumentationDirective directive =
                nonnull(directivesByName.get(instrumentCall.getImplName()),
                        "no matching directive \"" + instrumentCall.getImplName() + "\"");
        this.activeInstrumentation = directive;
        this.instrumentedVar = instrumentCall.getToImplement().getName();
        this.instrumentationStructInstName =
                varGen.nextVar("instr_" + directive.name + "_" +
                        instrumentCall.getToImplement().getName());
        final TypeStructRef structref = new TypeStructRef(directive.struct);
        addStatement(new StmtVarDecl(instrumentCall, structref,
                instrumentationStructInstName, null));
        this.instrumentationStructInst =
                new ExprVar(instrumentCall, instrumentationStructInstName);
        addStatement(new StmtAssign(instrumentCall, instrumentationStructInst,
                new ExprNew(instrumentCall, structref)));
        return null;// super.visitCudaInstrumentCall(instrumentCall);
    }
}
