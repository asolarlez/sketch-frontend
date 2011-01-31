package sketch.compiler.passes.lowering;

import java.util.Arrays;
import java.util.Vector;

import sketch.compiler.ast.core.SymbolTable;
import sketch.compiler.ast.core.exprs.ExprArrayRange;
import sketch.compiler.ast.core.exprs.ExprArrayRange.CommaIndex;
import sketch.compiler.ast.core.exprs.ExprBinary;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprNamedParam;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.StmtAssert;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.stmts.StmtVarDecl.VarDeclEntry;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.ast.core.typs.TypeCommaArray;
import sketch.compiler.passes.annotations.CompilerPassDeps;
import sketch.util.exceptions.ExceptionAtNode;
import sketch.util.fcns.ZipEnt;
import sketch.util.fcns.ZipIdxEnt;

import static sketch.util.DebugOut.assertFalse;

import static sketch.util.fcns.Zip.zip;
import static sketch.util.fcns.ZipWithIndex.zipwithindex;

/**
 * flatten mutli-dimensional arrays
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
@CompilerPassDeps(runsBefore = {}, runsAfter = {})
public class FlattenCommaMultidimArrays extends SymbolTableVisitor {
    public FlattenCommaMultidimArrays(SymbolTable symtab) {
        super(symtab);
    }

    @Override
    public Object visitStmtVarDecl(StmtVarDecl stmt) {
        // NOTE -- we will still register the variable as a comma array.
        StmtVarDecl decl = (StmtVarDecl) super.visitStmtVarDecl(stmt);

        Vector<VarDeclEntry> next = new Vector<VarDeclEntry>();
        boolean changed = false;
        for (VarDeclEntry e : decl) {
            if (e.getType() instanceof TypeCommaArray) {
                TypeCommaArray t = (TypeCommaArray) e.getType();
                Type nextType = new TypeArray(t.getBase(), t.getProdLength());
                next.add(e.nextWithType(nextType));
                changed = true;
            } else {
                next.add(e);
            }
        }

        if (changed) {
            return new StmtVarDecl(decl, next);
        } else {
            return stmt;
        }
    }

    @Override
    public Object visitExprArrayRange(ExprArrayRange exp) {
        Type t = getType(exp.getBase());
        if (t instanceof TypeCommaArray) {
            TypeCommaArray ta = (TypeCommaArray) t;
            assert exp.getMembers().size() == 1;
            Object inner = exp.getMembers().get(0);
            Expression[] indexExprs = new Expression[ta.getLengthParams().size()];

            if (inner instanceof CommaIndex) {
                boolean usingNames = false;
                boolean usingIndices = false;
                for (ZipIdxEnt<Expression> e : zipwithindex((CommaIndex) inner)) {
                    if (e.entry instanceof ExprNamedParam) {
                        ExprNamedParam en = (ExprNamedParam) e.entry;
                        indexExprs[ta.namedIndexToPositional(en.getName())] = e.entry;
                        usingNames = true;
                    } else {
                        indexExprs[e.idx] = e.entry;
                        usingIndices = true;
                    }
                }
                if (usingNames && usingIndices) {
                    assertFalse("indexing into array uses both names and expressions");
                }
            } else {
                assertFalse("unsupported inner index type",
                        inner.getClass().getSimpleName());
            }

            // create the array using the flattened indexing
            // each step with (oldIdx, N) transforms oldIdx -> oldIdx * N + newIdx
            Expression linearizedExpression = null;
            for (ZipEnt<Expression, Expression> es : zip(Arrays.asList(indexExprs),
                    ta.getLengthParams()))
            {
                final Expression off_i = es.left.thisOrNamedExpr();
                final Expression N_i = es.right.thisOrNamedExpr();
                if (es.left == null) {
                    throw new ExceptionAtNode("index expression null", exp);
                }
                if (!(N_i instanceof ExprConstInt)) {
                    assertFalse("type of array constant not reduce to constant int!", N_i);
                }

                // add assertion
                doStatement(new StmtAssert(exp, new ExprBinary(exp, off_i, "<", N_i),
                        false));
                doStatement(new StmtAssert(exp, new ExprBinary(exp, off_i, ">=",
                        new ExprConstInt(0)), false));

                // next node as in spec @ top of loop
                if (linearizedExpression != null) {
                    linearizedExpression =
                            new ExprBinary(exp, ExprBinary.BINOP_MUL,
                                    linearizedExpression, N_i);
                    linearizedExpression =
                            new ExprBinary(exp, ExprBinary.BINOP_ADD,
                                    linearizedExpression, off_i);
                } else {
                    linearizedExpression = off_i;
                }
            }
            assert linearizedExpression != null;

            return new ExprArrayRange(exp, exp.getBase(), linearizedExpression);
        }
        return super.visitExprArrayRange(exp);
    }
}
