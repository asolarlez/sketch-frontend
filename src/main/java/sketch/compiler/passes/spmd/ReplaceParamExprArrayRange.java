package sketch.compiler.passes.spmd;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Package;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.SymbolTable;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.ExprArrayRange;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtBlock;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.passes.annotations.CompilerPassDeps;
import sketch.compiler.passes.lowering.SymbolTableVisitor;
import sketch.compiler.passes.structure.CallGraph;

@CompilerPassDeps(runsBefore = {}, runsAfter = { GlobalToLocalCasts.class })
public class ReplaceParamExprArrayRange extends SymbolTableVisitor {
    TempVarGen varGen;
    CallGraph cg;
    public ReplaceParamExprArrayRange(TempVarGen varGen) {
        super(null);
        this.varGen = varGen;
    }

    @Override
    public Object visitProgram(Program prog) {
        cg = new CallGraph(prog);
        return super.visitProgram(prog);
    }

     @Override
    public Object visitStreamSpec(Package spec) {
        super.visitStreamSpec(spec);
        
//        final CodePrinterVisitor pr1 = new CodePrinterVisitor();
//        pr1.setNres(nres);
//        System.out.println("before replace param range:");
//        spec.accept(pr1);

        final CallReplacer cr = new CallReplacer(symtab);
        cr.setNres(nres);
        Package result = (Package) cr.visitStreamSpec(spec);
        
//        System.out.println("after replace param range:");
//        final CodePrinterVisitor pr2 = new CodePrinterVisitor();
//        pr2.setNres(nres);
//        result.accept(pr2);
        
        return result;
    }

    protected class CallReplacer extends SymbolTableVisitor {
        protected Vector<Statement> statementsAfter = new Vector<Statement>();

        public CallReplacer(SymbolTable symtab) {
            super(symtab);
        }

        @Override
        public Object visitExprFunCall(ExprFunCall exp) {
            Vector<Expression> nextParams = new Vector<Expression>();
            Function funcSigParams = cg.getTarget(exp);
            Iterator<Parameter> iter = funcSigParams.getParams().iterator();
            boolean changed = false;
            for (Expression e : exp.getParams()) {
                Parameter fcnParam = iter.next();
                if ((e instanceof ExprArrayRange) && fcnParam.isParameterOutput()) {
                    ExprArrayRange range = (ExprArrayRange) e;
                    Expression len = range.getSelection().getLenExpression();
                    TypeArray baseType = (TypeArray) getType(range.getBase());
                    Type type = (len == null) ? baseType.getBase() : baseType.createWithLength(len);
                    //System.out.println("range: " + range + " base: " + range.getBase() + " type: " + type);
                    //System.out.println("selection: " + range.getSelection() + "len: " + range.getSelection().getLenExpression());
                    String varName = varGen.nextVar("tmp" + fcnParam.getName());
                    ExprVar var = new ExprVar(exp, varName);
                    StmtVarDecl decl = new StmtVarDecl(exp, type, varName, e);
                    StmtAssign assgn = new StmtAssign(exp, e, var);
                    // register in symtab
                    super.visitStmtVarDecl(decl);
                    addStatement(decl);
 
                    nextParams.add(var);
                    changed = true;
                    statementsAfter.add(assgn);
                } else {
                    nextParams.add(e);
                }
            }
            if (changed) {
                assert nextParams.size() == exp.getParams().size();
                return new ExprFunCall(exp, exp.getName(), nextParams);
            } else {
                return exp;
            }
        }

        @Override
        public Object visitStmtBlock(StmtBlock oldBlock) {
            List<Statement> oldStatements = newStatements;
            Vector<Statement> nextStatements = new Vector<Statement>();
            newStatements = new ArrayList<Statement>();
            assert statementsAfter.isEmpty();
            boolean changed = false;
            for (Statement s : oldBlock.getStmts()) {
                newStatements.add((Statement) s.accept(this));
                if (!changed) {
                    changed = newStatements.size() != 1 || newStatements.get(0) != s;
                }
                nextStatements.addAll(newStatements);
                if (!statementsAfter.isEmpty()) {
                    changed = true;
                    nextStatements.addAll(statementsAfter);
                    statementsAfter.clear();
                }
                newStatements.clear();
            }
            newStatements = oldStatements;
            if (changed) {
                //System.out.println("nextStatements: " + nextStatements);
                StmtBlock newBlock = new StmtBlock(oldBlock, nextStatements);
//                System.out.println("newblock: " + newBlock);
                return newBlock;
            } else {
                return oldBlock;
            }
        }
    }
}
