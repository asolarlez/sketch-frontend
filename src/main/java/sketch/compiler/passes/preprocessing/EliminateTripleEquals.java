package sketch.compiler.passes.preprocessing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Package;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.SymbolTable;
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
import sketch.compiler.ast.core.stmts.*;
import sketch.compiler.ast.core.typs.StructDef;
import sketch.compiler.ast.core.typs.StructDef.StructFieldEnt;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.ast.core.typs.TypeStructRef;
import sketch.compiler.passes.lowering.GetExprType;
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

/**
 * This class replaces === with relevant equals function calls that recursively compares
 * two different ADT values.
 */
public class EliminateTripleEquals extends SymbolTableVisitor {
    int depth;
    TempVarGen varGen;
    Map<String, String> equalsFuns = new HashMap<String, String>();
    List<Function> newFuns;

    public EliminateTripleEquals(TempVarGen varGen, int depth) {
        super(null);
        this.varGen = varGen;
        this.depth = depth;
    }

    boolean converted = false;

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

			Type lt = getType(left);
			Type rt = (TypeStructRef) getType(right);
			Type parent;
            if (lt.promotesTo(rt, nres)) parent = rt;
            else if (rt.promotesTo(lt, nres))
                parent = lt;
            else
                throw new ExceptionAtNode("=== Types don't match", expr);

            String funName = createEqualsFun((TypeStructRef) parent, expr);

            List<Expression> pm = new ArrayList<Expression>();
            pm.add(left);
            pm.add(right);
            pm.add(ExprConstInt.createConstant(depth));

            return new ExprFunCall(expr, funName, pm, null);
        }

        if (expr.getOp() == ExprBinary.BINOP_EQ) {
            ExprFunCall efc = eqRewriteHelper(expr);
            if (efc != null) {
                return efc;
            }
        }
        if (expr.getOp() == ExprBinary.BINOP_NEQ) {
            ExprFunCall efc = eqRewriteHelper(new ExprBinary(ExprBinary.BINOP_EQ, expr.getLeft(), expr.getRight()));
            if (efc != null) {
                return new ExprUnary(expr, ExprUnary.UNOP_NOT, efc);
            }
        }

        return super.visitExprBinary(expr);
    }

    public Object visitStmtWhile(StmtWhile stmt) {
        converted = false;
        Expression newCond = doExpression(stmt.getCond());
        if (converted) {
            String vname = varGen.nextVar();
            StmtVarDecl vd = new StmtVarDecl(stmt.getCond(), TypePrimitive.bittype, vname, newCond);
            addStatement(vd);
            Statement newBody = (Statement) stmt.getBody().accept(this);
            Expression nvar = new ExprVar(stmt.getCond(), vname);
            newBody = new StmtBlock(newBody, new StmtAssign(nvar, newCond));
            return new StmtWhile(stmt, nvar, newBody);
        } else {
            Statement newBody = (Statement) stmt.getBody().accept(this);
            if (newCond == stmt.getCond() && newBody == stmt.getBody())
                return stmt;
            return new StmtWhile(stmt, newCond, newBody);
        }
    }
    
    public Object visitStmtDoWhile(StmtDoWhile stmt) {
        converted = false;
        Expression newCond = doExpression(stmt.getCond());
        if (converted) {
            String vname = varGen.nextVar();
            StmtVarDecl vd = new StmtVarDecl(stmt.getCond(), TypePrimitive.bittype, vname, null);
            addStatement(vd);
            Statement newBody = (Statement) stmt.getBody().accept(this);
            Expression nvar = new ExprVar(stmt.getCond(), vname);
            newBody = new StmtBlock(newBody, new StmtAssign(nvar, newCond));
            return new StmtDoWhile(stmt, newBody, nvar);
        } else {
            Statement newBody = (Statement) stmt.getBody().accept(this);
            if (newCond == stmt.getCond() && newBody == stmt.getBody())
                return stmt;
            return new StmtWhile(stmt, newCond, newBody);
        }
    }
    
    public Object visitStmtFor(StmtFor stmt) {
        SymbolTable oldSymTab = symtab;
        symtab = new SymbolTable(symtab);
        
        
        Statement newInit = null;
        if(stmt.getInit() != null){
            newInit = (Statement) stmt.getInit().accept(this);
        }
        
        
        converted = false;
        
        Expression newCond = doExpression(stmt.getCond());
        boolean condConverted = converted;
        converted = false;
        Statement newIncr = null;
        if(stmt.getIncr() != null){
            newIncr = (Statement) stmt.getIncr().accept(this);
        }
        boolean incrConverted = converted;
        
        Statement tmp = stmt.getBody();
        Statement newBody = StmtEmpty.EMPTY; 
        if(tmp != null){ 
            newBody = (Statement) tmp.accept(this);
        }       
        
        if(condConverted || incrConverted){
            addStatement(newInit);
            
            if(condConverted){
                String vname = varGen.nextVar();
                Expression oldCond = newCond;
                StmtVarDecl vd = new StmtVarDecl(stmt.getCond(), TypePrimitive.bittype, vname, newCond);
                addStatement(vd);
                newCond = new ExprVar(stmt.getCond(), vname);
                newBody = new StmtBlock(newBody, new StmtAssign(newCond, oldCond));
            }
            
            newBody = new StmtBlock(newBody, newIncr);
            
            symtab = oldSymTab;
            
            return new StmtWhile(stmt, newCond, newBody);
        }else{
            
            
           
            symtab = oldSymTab;
                        
            if (newInit == stmt.getInit() && newCond == stmt.getCond() &&
                    newIncr == stmt.getIncr() && newBody == stmt.getBody())
                return stmt;
            return new StmtFor(stmt, newInit, newCond, newIncr, newBody, stmt.isCanonical());   
            
        }        
    }
    
    

    private ExprFunCall eqRewriteHelper(ExprBinary expr) {
        Type lt = getType(expr.getLeft());
        if (lt instanceof TypeStructRef && !(expr.getRight() instanceof ExprNullPtr)) {
            TypeStructRef ltr = (TypeStructRef) lt;
            StructDef sdef = nres.getStruct(ltr.getName());
            if (sdef.immutable()) {
                Expression left = (Expression) expr.getLeft().doExpr(this);
                Expression right = (Expression) expr.getRight().doExpr(this);

                Type rt = getType(right);
                Type parent;
                if (lt.promotesTo(rt, nres))
                    parent = rt;
                else if (rt.promotesTo(lt, nres))
                    parent = lt;
                else
                    throw new ExceptionAtNode("== Types don't match", expr);

                String funName = createEqualsFun((TypeStructRef) parent, expr);

                List<Expression> pm = new ArrayList<Expression>();
                pm.add(left);
                pm.add(right);
                pm.add(ExprConstInt.createConstant(depth));

                return new ExprFunCall(expr, funName, pm, null);
            }
        }
        return null;
    }


    private String createEqualsFun(TypeStructRef type, FENode ctx) {
        converted = true;
        StructDef struct = nres.getStruct(type.getName());
        String structName = struct.getFullName();
        String structFullName = structName;
        if (type.hasTypeParams()) {
            structFullName += "<";
            boolean fst = true;
            for (Type t : type.getTypeParams()) {
                if (!fst) {
                    structFullName += ",";
                }
                fst = false;
                structFullName += t.toString();
            }
            structFullName += ">";
        }

        if (equalsFuns.containsKey(structFullName)) {
            return equalsFuns.get(structFullName);
        }
        String fname = varGen.nextVar("equals" + "_" + struct.getName());
        equalsFuns.put(structFullName, fname);
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

        genEqualsBody(ctx, new ExprVar(ctx, left), new ExprVar(ctx, right), new ExprVar(ctx, bnd), structName, struct.getPkg(), stmts, type);
        fc.body(new StmtBlock(stmts));
        fc.annotation("IsEquals", "");
        newFuns.add(fc.create());
        return fname;
    }

    private void genEqualsBody(FENode ctx, ExprVar left, ExprVar right,
            ExprVar bnd, String structName, String pkgName, List<Statement> stmts, TypeStructRef basetype)
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

        if (nres.getStructChildren(structName).isEmpty()) {
            stmts.add(new StmtReturn(ctx, ExprConstInt.one));
        } else {
            StmtSwitch stmt = new StmtSwitch(ctx, left);
            List<String> cases = getCases(structName);
            for (String c : cases) {
                Statement body =
                        generateBody(ctx, c, structName, pkgName, left, right, bnd, basetype);
                stmt.addCaseBlock(c, body);
            }
            stmts.add(stmt);
        }
    }

    private Expression checkFieldEqual(FENode context, Expression l, Expression r,
            Type t, List<Statement> stmts, String pkgName, ExprVar bnd, Expression lorig)
    {
        if (t.isStruct()) {
            TypeStructRef tref = (TypeStructRef) t;
            StructDef sdef = nres.getStruct(tref.getName());

            if (sdef.immutable()) {
                String funName = createEqualsFun((TypeStructRef) t, context);

                List<Expression> pm = new ArrayList<Expression>();
                pm.add(l);
                pm.add(r);
                pm.add(new ExprBinary(context, ExprBinary.BINOP_SUB, bnd, ExprConstInt.one));

                return new ExprFunCall(context, funName, pm, null);
            } else {
                return new ExprBinary(context, ExprBinary.BINOP_EQ, l, r);
            }
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
            ExprVar left, ExprVar right, ExprVar bnd, TypeStructRef basetype) {
        StmtSwitch stmt = new StmtSwitch(context, right);
        List<Statement> caseBody = new ArrayList<Statement>();

        StructDef struct = nres.getStruct(caseName+"@"+pkgName);
        Expression eq = ExprConstInt.one;
        boolean first = true;
        
        final Map<String, Type> rmap = GetExprType.replMap(struct, basetype, context);
        FEReplacer trep = new FEReplacer() {
            public Object visitTypeStructRef(TypeStructRef tsr) {
                if (rmap.containsKey(tsr.getName())) {
                    return rmap.get(tsr.getName());
                }
                return super.visitTypeStructRef(tsr);
            }
        };
        
        for (StructFieldEnt e : struct.getFieldEntriesInOrder()) {
            Expression l = new ExprField(left, e.getName());
            Expression r = new ExprField(right, e.getName());
            Type type = e.getType();
            if (rmap != null) {
                type = (Type) type.accept(trep);
            }
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
