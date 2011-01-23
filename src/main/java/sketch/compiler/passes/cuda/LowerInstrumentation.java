package sketch.compiler.passes.cuda;

import java.util.Vector;

import sketch.compiler.Directive.InstrumentationDirective;
import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.ExprArrayRange;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.ExprNew;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtBlock;
import sketch.compiler.ast.core.stmts.StmtExpr;
import sketch.compiler.ast.core.stmts.StmtReturn;
import sketch.compiler.ast.core.typs.TypeStructRef;
import sketch.compiler.ast.cuda.exprs.CudaInstrumentCall;
import sketch.compiler.ast.cuda.stmts.CudaSyncthreads;
import sketch.compiler.ast.cuda.typs.CudaMemoryType;
import sketch.compiler.dataflow.preprocessor.FlattenStmtBlocks;
import sketch.compiler.passes.annotations.CompilerPassDeps;
import sketch.compiler.passes.lowering.GlobalsToParams;
import sketch.compiler.passes.structure.CallGraph;
import sketch.util.datastructures.TypedHashMap;
import sketch.util.exceptions.ExceptionAtNode;
import static sketch.util.Misc.nonnull;

/**
 * lower CudaInstrument AST nodes to reads and writes. hopefully they're more primitive by
 * now.
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
@CompilerPassDeps(runsBefore = { GenerateAllOrSomeThreadsFunctions.class,
        GlobalsToParams.class }, runsAfter = {})
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
        return super.visitProgram((Program) (new FlattenStmtBlocks()).visitProgram(prog));
    }

    @Override
    public Object visitStmtBlock(StmtBlock stmt) {
        InstrumentationDirective oldInstrumentation = this.activeInstrumentation;
        StmtBlock newBlock = (StmtBlock) super.visitStmtBlock(stmt);
        if (this.activeInstrumentation != oldInstrumentation) {
            Vector<Statement> stmts = new Vector<Statement>(newBlock.getStmts());
            if (!(stmts.lastElement() instanceof StmtReturn)) {
                if (activeInstrumentation.end != null) {
                    stmts.add(endCall(stmt));
                }
            }
            newBlock = new StmtBlock(newBlock, stmts);
            this.activeInstrumentation = oldInstrumentation;
        }
        return newBlock;
    }

    protected StmtExpr endCall(FENode node) {
        return new StmtExpr(new ExprFunCall(node, activeInstrumentation.end,
                instrumentationStructInst));
    }

    @Override
    public Object visitStmtReturn(StmtReturn stmt) {
        if (activeInstrumentation != null) {
            if (activeInstrumentation.end != null) {
                addStatement(endCall(stmt));
            }
        }
        return super.visitStmtReturn(stmt);
    }

    public Object visitCudaSyncthreads(CudaSyncthreads cudaSyncthreads) {
        if (activeInstrumentation != null && activeInstrumentation.syncthreads != null) {
            addStatement(new StmtExpr(new ExprFunCall(cudaSyncthreads,
                    activeInstrumentation.syncthreads, instrumentationStructInst)));
        }
        return cudaSyncthreads;
    };

    @Override
    public Object visitStmtAssign(StmtAssign stmt) {
        // Compute RHS through superclass. LHS is handled manually below.
        Expression rhs = doExpression(stmt.getRHS());
        if (rhs != stmt.getRHS()) {
            stmt = new StmtAssign(stmt, stmt.getLHS(), rhs, stmt.getOp());
        }

        if (activeInstrumentation != null) {
            if (stmt.getLHS() instanceof ExprArrayRange) {
                // run LHS recursively in case an array access is used in determining
                // indices
                ExprArrayRange access = (ExprArrayRange) stmt.getLHS();
                Expression offset = doExpression(access.getOffset());
                if (offset != access.getOffset()) {
                    access = new ExprArrayRange(access, access.getBase(), offset);
                }
                if (access.getAbsoluteBase().getName().equals(instrumentedVar)) {
                    if (!(access.getBase() instanceof ExprVar)) {
                        throw new ExceptionAtNode("multi-dimensional array "
                                + "instrumentation is not yet supported", stmt);
                    }
                    // support +=, etc. -- read the element first
                    if (stmt.getOp() != 0) {
                        addExprStatement(new ExprFunCall(stmt,
                                activeInstrumentation.read, instrumentationStructInst,
                                access.getOffset()));
                    }

                    addExprStatement(new ExprFunCall(stmt, activeInstrumentation.write,
                            instrumentationStructInst, access.getOffset()));
                }
            } else {
                Expression lhs = doExpression(stmt.getLHS());
                if (lhs != stmt.getLHS()) {
                    stmt = new StmtAssign(stmt, lhs, stmt.getRHS(), stmt.getOp());
                }
            }
        }

        return stmt;
    }

    @Override
    public Object visitExprArrayRange(ExprArrayRange exp) {
        if (activeInstrumentation != null) {
            if (exp.getAbsoluteBase().getName().equals(instrumentedVar)) {
                if (!(exp.getBase() instanceof ExprVar)) {
                    throw new ExceptionAtNode("multi-dimensional array "
                            + "instrumentation is not yet supported", exp);
                }
                addExprStatement(new ExprFunCall(exp, activeInstrumentation.read,
                        instrumentationStructInst, exp.getOffset()));
            }
        }
        return super.visitExprArrayRange(exp);
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
        final TypeStructRef structref =
                new TypeStructRef(CudaMemoryType.GLOBAL, directive.struct);
        this.instrumentationStructInst = instrumentCall.getImplVariable();
        addStatement(new StmtAssign(instrumentCall, instrumentationStructInst,
                new ExprNew(instrumentCall, structref)));
        return null;// super.visitCudaInstrumentCall(instrumentCall);
    }
}
