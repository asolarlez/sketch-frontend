package sketch.compiler.passes.preprocessing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Package;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.ExprArrayRange;
import sketch.compiler.ast.core.exprs.ExprBinary;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprField;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.ExprNullPtr;
import sketch.compiler.ast.core.exprs.ExprUnary;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtBlock;
import sketch.compiler.ast.core.stmts.StmtFor;
import sketch.compiler.ast.core.stmts.StmtIfThen;
import sketch.compiler.ast.core.stmts.StmtReturn;
import sketch.compiler.ast.core.stmts.StmtSwitch;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.StructDef;
import sketch.compiler.ast.core.typs.StructDef.StructFieldEnt;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.ast.core.typs.TypeStructRef;
import sketch.compiler.passes.lowering.SymbolTableVisitor;
import sketch.util.exceptions.ExceptionAtNode;

class ArrayTypeReplacer1 extends SymbolTableVisitor {

    Expression parentStruct;

    public ArrayTypeReplacer1(Expression varMap) {
        super(null);
        this.parentStruct = varMap;
    }

    @Override
    public Object visitExprVar(ExprVar exp) {
        String name = exp.getName();
        return new ExprField(parentStruct, name);
    }
}
public class EliminateTripleEquals extends SymbolTableVisitor {
    final int DEPTH = 5;
    TempVarGen varGen;
    Map<String, String> equalsFuns = new HashMap<String, String>();
    List<Function> newFuns;
    public EliminateTripleEquals(TempVarGen varGen) {
        super(null);
        this.varGen = varGen;
    }
    
    @Override
    public Object visitPackage(Package pkg) {
        nres.setPackage(pkg);
        newFuns = new ArrayList<Function>();
        pkg = (Package) super.visitPackage(pkg);
        newFuns.addAll(pkg.getFuncs());
        return new Package(pkg, pkg.getName(), pkg.getStructs(), pkg.getVars(), newFuns,
                pkg.getSpAsserts());
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
            else if (rt.promotesTo(lt, nres))
                parent = lt;
            else
                throw new ExceptionAtNode("=== Types don't match", expr);

            String funName = createEqualsFun((TypeStructRef) parent, expr);

            List<Expression> pm = new ArrayList<Expression>();
            pm.add(left);
            pm.add(right);
            pm.add(ExprConstInt.createConstant(DEPTH));

            return new ExprFunCall(expr, funName, pm);
        }
        return super.visitExprBinary(expr);
    }
    
    private String createEqualsFun(TypeStructRef type, FENode ctx) {
        StructDef struct = nres.getStruct(type.getName());
        String structName = struct.getFullName();

        if (equalsFuns.containsKey(structName)) {
            return equalsFuns.get(structName);
        }
        String fname = varGen.nextVar("equals" + "_" + struct.getName());
        equalsFuns.put(structName, fname);
        Function.FunctionCreator fc =
                Function.creator(ctx, fname, Function.FcnType.Static);

        fc.returnType(TypePrimitive.bittype);
        fc.pkg(nres.curPkg().getName());
        String left = varGen.nextVar("left");
        String right = varGen.nextVar("right");
        String bnd = varGen.nextVar("bnd");

        List<Parameter> params = new ArrayList<Parameter>();
        params.add(new Parameter(ctx, type, left));
        params.add(new Parameter(ctx, type, right));
        params.add(new Parameter(ctx, TypePrimitive.inttype, bnd));

        fc.params(params);

        List<Statement> stmts = new ArrayList<Statement>();

        genEqualsBody(ctx, new ExprVar(ctx, left), new ExprVar(ctx, right), new ExprVar(
                ctx, bnd), structName, struct.getPkg(),
                        stmts);
        fc.body(new StmtBlock(stmts));
        newFuns.add(fc.create());
        return fname;
    }

    private void genEqualsBody(FENode ctx, ExprVar left, ExprVar right,
            ExprVar bnd, String structName, String pkgName, List<Statement> stmts)
    {
        // base case when bnd <= 0 => return 0
        ExprBinary cond =
                new ExprBinary(ctx, ExprBinary.BINOP_LE, bnd, ExprConstInt.zero);
        StmtIfThen baseCaseIf =
                new StmtIfThen(ctx, cond, new StmtReturn(ctx, ExprConstInt.zero), null);
        stmts.add(baseCaseIf);

        // deal with nulls
        Expression leftNull =
                new ExprBinary(ExprBinary.BINOP_EQ, left, new ExprNullPtr());
        Expression rightNull =
                new ExprBinary(ExprBinary.BINOP_EQ, right, new ExprNullPtr());
        Expression bothNull = new ExprBinary(ExprBinary.BINOP_AND, leftNull, rightNull);

        StmtIfThen bothNullIf =
                new StmtIfThen(ctx, bothNull, new StmtReturn(ctx, ExprConstInt.one), null);
        stmts.add(bothNullIf);

        StmtIfThen leftNullIf =
                new StmtIfThen(ctx, leftNull, new StmtReturn(ctx, ExprConstInt.zero),
                        null);
        stmts.add(leftNullIf);

        StmtIfThen rightNullIf =
                new StmtIfThen(ctx, rightNull, new StmtReturn(ctx, ExprConstInt.zero),
                        null);
        stmts.add(rightNullIf);

        // Check high level fields
        StructDef struct = nres.getStruct(structName);
        for (StructFieldEnt e : struct.getFieldEntriesInOrder()) {
            Expression l = new ExprField(left, e.getName());
            Expression r = new ExprField(right, e.getName());
            Type t = e.getType();
            Expression fieldEq = checkFieldEqual(ctx, l, r, t, stmts, pkgName, bnd, left);
            
            stmts.add(new StmtIfThen(ctx,
                    new ExprUnary(ctx, ExprUnary.UNOP_NOT, fieldEq), new StmtReturn(ctx,
                            ExprConstInt.zero), null));
        }
        
        TypeStructRef type = new TypeStructRef(structName, false);
            
        StmtSwitch stmt = new StmtSwitch(ctx, left);
        List<String> cases = getCases(structName);
        for (String c : cases) {
            Statement body = generateBody(ctx, c, structName, pkgName, left, right, bnd);
            stmt.addCaseBlock(c, body);
        }
        stmts.add(stmt);
    }
    
    private Expression checkFieldEqual(FENode context, Expression l, Expression r,
            Type t, List<Statement> stmts, String pkgName, ExprVar bnd, Expression lorig)
    {
        if (t.isStruct()) {
            String funName = createEqualsFun((TypeStructRef) t, context);

            List<Expression> pm = new ArrayList<Expression>();
            pm.add(l);
            pm.add(r);
            pm.add(new ExprBinary(context, ExprBinary.BINOP_SUB, bnd, ExprConstInt.one));

            return new ExprFunCall(context, funName, pm);
        } else {
            if (t.isArray()) {
                // bit x = true;
                // for (int i = 0; i < sz; i++) {
                // x = x && (l[i] == r[i])
                // }
                TypeArray ta = (TypeArray) t;
                Expression length =
                        (Expression) ta.getLength().doExpr(new ArrayTypeReplacer1(lorig));
                String tmp = varGen.nextVar();
                ExprVar fe = new ExprVar(context, tmp);
                stmts.add(new StmtVarDecl(context, TypePrimitive.bittype, tmp,
                        ExprConstInt.one));
                String i = varGen.nextVar();
                ExprVar ivar = new ExprVar(context, i);
                Statement init =
                        new StmtVarDecl(context, TypePrimitive.inttype, i,
                                ExprConstInt.zero);
                Expression cond =
 new ExprBinary(ExprBinary.BINOP_LT, ivar, length);
                Statement incr =
                        new StmtAssign(ivar, ExprConstInt.one, ExprBinary.BINOP_ADD);

                List<Statement> newStmts = new ArrayList<Statement>();
                Expression recEquals =
                        checkFieldEqual(context, new ExprArrayRange(l, ivar),
                                new ExprArrayRange(r, ivar), ta.getBase(), newStmts,
                                pkgName, bnd, lorig);
                Statement body =
                        new StmtAssign(fe, new ExprBinary(ExprBinary.BINOP_AND, fe,
                                recEquals));
                newStmts.add(body);
                Statement forLoop =
                        new StmtFor(context, init, cond, incr, new StmtBlock(newStmts),
                                true);
                stmts.add(forLoop);
                return fe;
            }
            return new ExprBinary(ExprBinary.BINOP_EQ, l, r);
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
    
    private Statement generateBody(FENode context, String caseName,  String structName, String pkgName,
            ExprVar left, ExprVar right, ExprVar bnd) {
        StmtSwitch stmt = new StmtSwitch(context, right);
        List<Statement> caseBody = new ArrayList<Statement>();
        
        StructDef struct = nres.getStruct(caseName+"@"+pkgName);
        Expression eq = ExprConstInt.one;
        boolean first = true;
        for (StructFieldEnt e : struct.getFieldEntriesInOrder()) {
            Expression l = new ExprField(left, e.getName());
            Expression r = new ExprField(right, e.getName());
            Type type = e.getType();
            Expression fieldEq =
                    checkFieldEqual(context, l, r, type, caseBody, pkgName, bnd, left);

            if (first) {
                eq = fieldEq;
                first = false;
            } else {
                eq = new ExprBinary(ExprBinary.BINOP_AND, eq, fieldEq);
            }
        }
        caseBody.add(new  StmtReturn(context, eq));
        stmt.addCaseBlock(caseName, new StmtBlock(caseBody));
        stmt.addCaseBlock("default", new StmtReturn(context, ExprConstInt.zero));
        return new StmtBlock(stmt);
    }
}
