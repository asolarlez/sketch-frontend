package sketch.compiler.passes.preprocessing;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.ExprBinary;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprField;
import sketch.compiler.ast.core.exprs.ExprNullPtr;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtBlock;
import sketch.compiler.ast.core.stmts.StmtIfThen;
import sketch.compiler.ast.core.stmts.StmtSwitch;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.StructDef;
import sketch.compiler.ast.core.typs.StructDef.StructFieldEnt;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.ast.core.typs.TypeStructRef;
import sketch.compiler.passes.lowering.SymbolTableVisitor;

public class EliminateTripleEquals extends SymbolTableVisitor {
    final int DEPTH = 5;
    TempVarGen varGen;
    public EliminateTripleEquals(TempVarGen varGen) {
        super(null);
        this.varGen = varGen;
    }
    
    @Override
    public Object visitExprBinary(ExprBinary expr) {
        if (expr.getOp() == ExprBinary.BINOP_TEQ) {
            Expression left = (Expression) expr.getLeft().doExpr(this);
            Expression right = (Expression) expr.getRight().doExpr(this);
            
            TypeStructRef lt = (TypeStructRef) getType(left);
            TypeStructRef rt = (TypeStructRef) getType(right);
            TypeStructRef parent;
            if (lt.promotesTo(rt, nres)) parent = rt;
            else parent = lt;
            StructDef struct = nres.getStruct(parent.getName());
            String structName = struct.getFullName();
            List<Statement> stmts = new ArrayList<Statement>();
            Expression bitExpr = expandTripleEquals(expr, left, right, structName, struct.getPkg(), DEPTH, stmts);
            addStatements(stmts);
            return bitExpr;
        }
        return expr;
    }
    
    private Expression expandTripleEquals(FENode context, Expression left, Expression right,
            String structName, String pkgName, int depth, List<Statement> stmts) {
        if (depth == 0) {
            return ExprConstInt.zero;
        } else {
            String outName = varGen.nextVar();
            String lvName = varGen.nextVar();
            String rvName = varGen.nextVar();
            ExprVar out = new ExprVar(context, outName);
            ExprVar lv = new ExprVar(left, lvName);
            ExprVar rv = new ExprVar(right, rvName);
            
            
            TypeStructRef type = new TypeStructRef(structName, false);
            stmts.add(new StmtVarDecl(context,TypePrimitive.bittype, outName, ExprConstInt.zero));
            stmts.add(new StmtVarDecl(context, type, lvName, left));
            stmts.add(new StmtVarDecl(context, type, rvName, right));
            Expression lc = new ExprBinary(ExprBinary.BINOP_EQ, lv, new ExprNullPtr());
            Expression rc = new ExprBinary(ExprBinary.BINOP_EQ, rv, new ExprNullPtr());
            Expression cond = new ExprBinary(ExprBinary.BINOP_AND, lc, rc);
            StmtSwitch stmt = new StmtSwitch(context, lv);
            List<String> cases = getCases(structName);
            for (String c: cases) {
                Statement body = generateBody(context, c, out ,structName, pkgName, lv, rv, depth);
                stmt.addCaseBlock(c, body);
            }
            StmtIfThen st =
                    new StmtIfThen(context, cond, new StmtAssign(out, ExprConstInt.one),
                            stmt);

            stmts.add(st);
            
            Expression eq = out;
            StructDef struct  = nres.getStruct(structName);
            for (StructFieldEnt e : struct.getFieldEntriesInOrder()) {
                Expression l = new ExprField(left, e.getName());
                Expression r = new ExprField(right, e.getName());
                Expression fieldEq;
                Type t = e.getType();
                if (t.isStruct()) {
                    TypeStructRef ts = (TypeStructRef) t;
                    StructDef fieldStruct = nres.getStruct(ts.getName());
                    String name = fieldStruct.getFullName();
                    List<Statement> iStmts = new ArrayList<Statement>();
                    fieldEq = expandTripleEquals(context, l, r, name, pkgName, depth-1, iStmts);
                    stmts.addAll(iStmts);
                } else {
                    fieldEq = new ExprBinary(ExprBinary.BINOP_EQ, l, r);
                }
                
                eq = new ExprBinary(ExprBinary.BINOP_AND, eq, fieldEq);
                
            }
            return eq;
        }
    }
    
    private List<String> getCases(String structName) {
        List<String> cases = new ArrayList<String>();
        String current = structName;
        LinkedList<String> queue = new LinkedList<String>();
        queue.add(current);
        while (!queue.isEmpty()) {
            String parent = queue.removeFirst();
            List<String> children = nres.getStructChildren(parent);
            if (children.isEmpty()) {
                cases.add(parent.split("@")[0]);
            } else {
                queue.addAll(children);
            }
        }
        return cases;
    }
    
    private Statement generateBody(FENode context, String caseName, ExprVar out, String structName, String pkgName,
            ExprVar left, ExprVar right, int depth) {
        StmtSwitch stmt = new StmtSwitch(context, right);
        List<Statement> caseBody = new ArrayList<Statement>();
        
        StructDef struct = nres.getStruct(caseName+"@"+pkgName);
        Expression eq = ExprConstInt.one;
        boolean first = true;
        for (StructFieldEnt e : struct.getFieldEntriesInOrder()) {
            Expression l = new ExprField(left, e.getName());
            Expression r = new ExprField(right, e.getName());
            Expression fieldEq;
            Type type = e.getType();
            if (type.isStruct()) {
                TypeStructRef ts = (TypeStructRef) type;
                StructDef fieldStruct = nres.getStruct(ts.getName());
                String name = fieldStruct.getFullName();
                List<Statement> stmts = new ArrayList<Statement>();
                fieldEq = expandTripleEquals(context, l, r, name, pkgName, depth-1, stmts);
                caseBody.addAll(stmts);
            } else {
                fieldEq = new ExprBinary(ExprBinary.BINOP_EQ, l, r);
            }
            if (first) {
                eq = fieldEq;
                first = false;
            } else {
                eq = new ExprBinary(ExprBinary.BINOP_AND, eq, fieldEq);
            }
        }
        caseBody.add(new  StmtAssign(out, eq));
        stmt.addCaseBlock(caseName, new StmtBlock(caseBody));
        stmt.addCaseBlock("default", new StmtAssign(out, ExprConstInt.zero));
        return stmt;
    }
}
