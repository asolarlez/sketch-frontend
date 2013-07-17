package sketch.compiler.passes.lowering;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Function.FunctionCreator;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.SymbolTable;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.*;
import sketch.compiler.ast.core.exprs.ExprArrayRange.RangeLen;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssert;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtFor;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.StructDef;
import sketch.compiler.ast.core.typs.StructDef.StructFieldEnt;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.ast.core.typs.TypeStructRef;
import sketch.compiler.ast.cuda.typs.CudaMemoryType;
import sketch.compiler.passes.annotations.CompilerPassDeps;

/*
 * Currently treat all structs as final. Not supported: pointer assignment (struct X {} X
 * p = ...; X q; q = p) array of structs struct in struct (struct X { struct y; })
 */

@CompilerPassDeps(runsBefore = {}, runsAfter = {})
public class EliminateFinalStructs extends SymbolTableVisitor {
    private TempVarGen varGen;
    // max size for 1d arrays
    int iMaxArrSz;
    Expression maxArrSz;

    // struct var name => (field => corresponding var)
    Map<String, Map<String, ExprVar>> structs;

    public EliminateFinalStructs(TempVarGen varGen_, int maxArrSize) {
        super(null);
        varGen = varGen_;
        structs = new HashMap<String, Map<String, ExprVar>>();
        iMaxArrSz = maxArrSize;
        maxArrSz = new ExprConstInt(iMaxArrSz);
    }

    // return the core type of an array, collect the array lengths in order and add them
    // to "lens". meanwhile, add max lengths to maxlens. either can be empty.
    Type extractCoreType(Type t, List<Expression> lens, List<Integer> maxlens)
    {
        Type coretyp = t;
        while (coretyp.isArray()) {
            TypeArray ta = (TypeArray) coretyp;
            Expression len = ta.getLength();
            if (lens != null) {
                lens.add(len);
            }
            if (maxlens != null) {
                Integer maxlen = len.getIValue();
                if (maxlen == null) {
                    maxlen = ta.getMaxlength();
                }
                if (maxlen <= 0) {
                    maxlen = iMaxArrSz;
                }
                maxlens.add(maxlen);
            }
            coretyp = ((TypeArray) coretyp).getBase();
        }
        return coretyp;
    }

    private Type constructType(CudaMemoryType memtyp, Type coretyp, int multiplicity)
    {
        assert multiplicity >= 1;
        Type t = coretyp;
        if (multiplicity > 1) {
            t = new TypeArray(t, new ExprConstInt(multiplicity));
        }
        t = t.withMemType(memtyp);
        return t;
    }

    /** do not use */
    @Deprecated
    protected Expression constructEar(ExprVar v, List<RangeLen> indices) {
        if (indices.isEmpty()) {
            return v;
        } else {
            Expression e = v;
            for (RangeLen i : indices) {
                e = new ExprArrayRange(v, e, i, false);
            }
            return e;
        }
    }

    @Override
    public Object visitFunction(Function f) {
        List<Parameter> newp = new ArrayList<Parameter>();
        boolean changed = false;

        for (Parameter p : f.getParams()) {
            Type type = p.getType();
            List<Integer> maxlens = new ArrayList<Integer>();
            Type t = extractCoreType(type, null, maxlens);
            if (t.isStruct()) {
                Collection<DeclPair> result = new ArrayList<DeclPair>();
                expandStructDecl(result, p, type, type.getCudaMemType(), p.getName(), t,
                        maxlens, 1);
                declToParam(newp, result, p.getPtype());
                changed = true;
            } else {
                newp.add((Parameter) super.visitParameter(p));
            }
        }

        Statement body = f.getBody();
        Statement newbody = body == null ? null : (Statement) body.accept(this);

        if (changed || newbody != body) {
            FunctionCreator creator = f.creator();
            if (changed) {
                creator = creator.params(newp);
            }
            if (newbody != body) {
                creator = creator.body(newbody);
            }
            return creator.create();
        }

        return f;
    }

    @Override
    public Object visitStmtVarDecl(StmtVarDecl decl) {
        if (decl.getNumVars() == 1) {
            String name = decl.getName(0);
            Type type = decl.getType(0);
            List<Expression> lens = new ArrayList<Expression>();
            List<Integer> maxlens = new ArrayList<Integer>();
            Type t = extractCoreType(type, lens, null);
            if (t.isStruct()) {
                Collection<DeclPair> result = new ArrayList<DeclPair>();
                expandStructDecl(result, decl, type, type.getCudaMemType(), name, t,
                        maxlens, 1);
                declToVarDecl(result);
                return null;
            }
        }
        return super.visitStmtVarDecl(decl);
    }

    /** do not use */
    @Deprecated
    static Expression getConst(Type t) {
        if (t.isArray()) {
            t = ((TypeArray) t).getAbsoluteBase();
        }
        assert t instanceof TypePrimitive;
        return ((TypePrimitive) t).defaultValue();
    }

    class DeclPair {
        Type t;
        String name;
        FENode ctx;

        DeclPair(Type t, String name, FENode context) {
            this.t = t;
            this.name = name;
            this.ctx = context;
        }
        
        void addStmtVarDecl() {
            addStatement(new StmtVarDecl(ctx, t, name, null));

            // TODO xzl: do we need this?
            // Expression init =
            // t.isArray() ? new ExprArrayInit(ctx, getConst(t)) : getConst(t);
            // addStatement(new StmtAssign(new ExprVar(ctx, name), init));
        }

        Parameter toParam(int ptype) {
            return new Parameter(ctx, t, name, ptype);
        }
        
        // TODO xzl: do we need this?
        @Deprecated
        int outputToRefer(int pt) {
            if (pt == Parameter.OUT && t.isArray()) {
                return Parameter.REF;
            }
            return pt;
        }
    }

    private void declToVarDecl(Collection<DeclPair> c) {
        for (DeclPair d : c) {
            d.addStmtVarDecl();
        }
    }
    
    private void declToParam(Collection<Parameter> p, Collection<DeclPair> c, int ptype) {
        for (DeclPair d : c) {
            p.add(d.toParam(ptype));
        }
    }

    private static int mult(Collection<Integer> a) {
        int result = 1;
        for (Integer i : a) {
            result *= i;
        }
        return result;
    }

    // is the var "name" an array?
    // if name is an array, lens will be its length, otherwise lens = null or
    // empty
    // multiplicity: how many copies we need to create = \prod {maxlens of this and all
    // parents}
    private void expandStructDecl(Collection<DeclPair> result, FENode decl,
            Type origType, CudaMemoryType memtyp, String name, Type typ,
            List<Integer> maxlens, int multiplicity)
    {
        symtab.registerVar(name, origType, decl, SymbolTable.KIND_LOCAL);
        if (typ.isStruct()) {

            StructDef t = nres.getStruct(((TypeStructRef) typ).getName());

            Map<String, ExprVar> fields = new HashMap<String, ExprVar>();
            assert !structs.containsKey(name) : "re-define variable " + name;
            structs.put(name, fields);
            for (StructFieldEnt en : t.getFieldEntriesInOrder()) {
                String field = en.getName();
                String varName = varGen.nextVar(name + "_" + field);
                ExprVar var = new ExprVar(decl, varName);
                fields.put(field, var);
                Type fieldType = en.getType();

                List<Integer> fmaxlens = new ArrayList<Integer>();
                Type ft = extractCoreType(fieldType, null, fmaxlens);
                expandStructDecl(result, decl, fieldType, memtyp, varName, ft, fmaxlens,
                        multiplicity * mult(maxlens));
            }
        } else {
            // use multiplicity!
            Type _t = (Type) constructType(memtyp, typ, multiplicity).accept(this);
            result.add(new DeclPair(_t, name, decl));
        }
    }


    static class LocationStep {
        boolean isField;

        // TypeStruct ts;
        String field;

        RangeLen index;

        LocationStep(RangeLen i) {
            isField = false;
            index = i;
        }

        LocationStep(/* TypeStruct t, */String f) {
            isField = true;
            // ts = t;
            field = f;
        }
    }

    class VarChanger extends FEReplacer {
        Map<String, ExprVar> root;
        Set<String> shouldFix;

        VarChanger(Map<String, ExprVar> r, Set<String> fix) {
            root = r;
            shouldFix = fix;
        }

        @Override
        public Object visitExprField(ExprField ef) {
            ExprVar left = (ExprVar) ef.getLeft().accept(this);
            ExprVar v = structs.get(left.getName()).get(ef.getName());
            shouldFix.add(v.getName());
            return v;
        }

        @Override
        public Object visitExprArrayRange(ExprArrayRange ear) {
            assert false : "arrary range not allowed!";
            return null;
        }

        @Override
        public Object visitExprVar(ExprVar ev) {
            ExprVar v = root.get(ev.getName());
            if (v == null) {
                return ev;
            } else {
                shouldFix.add(v.getName());
                return v;
            }
        }
    }
    
    class ArrayFixer extends FEReplacer {
        List<RangeLen> indices;
        Set<String> shouldFix;
        
        ArrayFixer(List<RangeLen> i, Set<String> fix) {
            indices = i;
            shouldFix = fix;
        }
        
        @Override
        public Object visitExprArrayRange(ExprArrayRange ear) {
            assert false : "arrary range not allowed!";
            return null;
        }

        @Override
        public Object visitExprField(ExprField ef) {
            assert false : "field not allowed!";
            return null;
        }

        @Override
        public Object visitExprVar(ExprVar ev) {
            if (shouldFix.contains(ev.getName())) {
                return constructEar(ev, indices);
            } else {
                return ev;
            }
        }
    }

    FENode fixRoot(FENode node, Map<String, ExprVar> root, List<RangeLen> indices) {
        Set<String> shouldFix = new HashSet<String>();
        node = (FENode) node.accept(new VarChanger(root, shouldFix));
        if (indices.isEmpty()) {
            return node;
        } else {
            return (FENode) node.accept(new ArrayFixer(indices, shouldFix));
        }
    }

    static class LocationInfo {
        ExprVar var;
        Type typ;
        List<RangeLen> indices;
        List<Expression> remainingLens;
    }

    // exp: ExprArrayRange or ExprField
    private boolean locate(Expression exp, LocationInfo loc,
            Collection<Statement> addStmts)
    {
        boolean visitedField = false;
        List<LocationStep> steps = new ArrayList<LocationStep>();
        Expression e = exp;
        while (true) {
            if (e instanceof ExprArrayRange) {
                ExprArrayRange ear = (ExprArrayRange) e;
                RangeLen index = ear.getSelection();
                if (index.hasLen()) {
                    assert !visitedField : "you cannot apply field selector to an array range!";
                }
                steps.add(new LocationStep(index));
                e = ear.getBase();
            } else if (e instanceof ExprField) {
                visitedField = true;
                ExprField ef = (ExprField) e;
                steps.add(new LocationStep(ef.getName()));
                e = ef.getLeft();
            } else if (e instanceof ExprVar) {
                break;
            } else {
                assert false : "EliminateFinalStructs.locate encounter error core expr " + e;
            }
        }

        ExprVar var = (ExprVar) e;
        final List<RangeLen> indices = new ArrayList<RangeLen>();
        List<Expression> lens = new ArrayList<Expression>();
        Type typ = extractCoreType(this.getType(var), lens, null);
        if (!typ.isStruct()) {
            assert !visitedField : "you cannot visit a field without the outermost struct!";
            loc.var = var;
            loc.typ = typ;
            loc.indices = indices;
            loc.remainingLens = lens;
            return false;
        }
        if (!lens.isEmpty()) {
            for (int i = 0; i < lens.size(); ++i) {
                lens.set(i, (Expression) lens.get(i).accept(this));
            }
        }
        // ts: the struct type for the next field operator

        StructDef ts = nres.getStruct(((TypeStructRef) typ).getName());

        boolean outerRange = false;
        for (int i = steps.size() - 1; i >= 0; --i) {
            LocationStep step = steps.get(i);
            if (step.isField) {
                assert !outerRange : "you cannot apply field selector to an array range!";
                assert ts != null : "you must have a struct type!";
                assert lens.isEmpty() : "you have not used all the indices before coming to a field!";

                final String rootVar = var.getName();
                final Map<String, ExprVar> root = structs.get(rootVar);

                var = structs.get(var.getName()).get(step.field);
                typ = extractCoreType(this.getType(var), lens, null);
                
                for (int j=0; j<lens.size(); ++j) {
                    if (lens.get(j).getIValue() == null) {
                        Expression newlen =
                                (Expression) fixRoot(lens.get(j), root, indices);
                        lens.set(j, newlen);
                    }
                }
                if (typ.isStruct()) {
                    ts = nres.getStruct(((TypeStructRef) typ).getName());
                } else {
                    ts = null;
                }
            } else {
                assert !lens.isEmpty() : "you don't have sufficient indices when coming to an ear!";
                RangeLen index = step.index;
                Expression start = (Expression) index.start().accept(this);
                if (index.hasLen()) {
                    outerRange = true;
                    Expression len = (Expression) index.getLenExpression().accept(this);
                    addStmts.add(new StmtAssert(new ExprBinary(new ExprBinary(start, "+",
                            len), "<=", lens.get(0)), "start+len must <= size", false));
                    index = new RangeLen(start, len);
                } else {
                    addStmts.add(new StmtAssert(new ExprBinary(start, "<", lens.get(0)),
                            "start must < size", false));
                    index = new RangeLen(start);
                }
                indices.add(index);
                lens.remove(0);
            }
        }
        loc.var = var;
        loc.typ = typ;
        loc.indices = indices;
        loc.remainingLens = lens;
        return true;
    }


    @Override
    public Object visitExprField(ExprField ef) {
        LocationInfo loc = new LocationInfo();
        List<Statement> addStmts = new ArrayList<Statement>();
        boolean result = locate(ef, loc, addStmts);
        assert result : "locate must return true for ExprField!";
        this.addStatements(addStmts);
        if (!loc.remainingLens.isEmpty()) {
            assert loc.remainingLens.size() == 1 : "don't know how to handle multi-dim ear!";
            for (Expression len : loc.remainingLens) {
                loc.indices.add(new RangeLen(new ExprConstInt(0), len));
            }
        }
        return constructEar(loc.var, loc.indices);
    }

    @Override
    public Object visitExprArrayRange(ExprArrayRange ear) {
        LocationInfo loc = new LocationInfo();
        List<Statement> addStmts = new ArrayList<Statement>();
        boolean result = locate(ear, loc, addStmts);
        if (result) {
            if (!loc.remainingLens.isEmpty()) {
                assert loc.remainingLens.size() == 1 : "don't know how to handle multi-dim ear!";
                for (Expression len : loc.remainingLens) {
                    loc.indices.add(new RangeLen(new ExprConstInt(0), len));
                }
            }
            return constructEar(loc.var, loc.indices);
        } else {
            return super.visitExprArrayRange(ear);
        }
    }

    @Override
    public Object visitStmtAssign(StmtAssign stmt) {
        Expression lhs = stmt.getLHS();
        Expression rhs = stmt.getRHS();
        LocationInfo lhsLoc = new LocationInfo();
        LocationInfo rhsLoc = new LocationInfo();
        List<Statement> addStmts = new ArrayList<Statement>();
        if (rhs instanceof ExprNullPtr) {
            return null;
        }
        if (rhs instanceof ExprNew) {
            boolean result = locate(lhs, lhsLoc, addStmts);
            assert result : "for new expr lhs must be complex!";
            assert lhsLoc.remainingLens.isEmpty() : "new expr cannot apply to array!";
            StructDef ts = nres.getStruct(((TypeStructRef) lhsLoc.typ).getName());

            for (ExprNamedParam en : ((ExprNew) rhs).getParams()) {
                String field = en.getName();
                Expression value = en.getExpr();
                List<Expression> lens = new ArrayList<Expression>();
                Type vtype = getType(value);
                Type coretyp = extractCoreType(vtype, lens, null);
                
                if (vtype.isArray()) {
                    Type rtype = ts.getType(field);
                    assert rtype.isArray() : "you can only assign array to array!";
                    List<Integer> maxlens = new ArrayList<Integer>();
                    Type rct = extractCoreType(rtype, null, maxlens);
                    assert rct.equals(coretyp) : "array base type mismatch in assignment!";
                    for (int i = 0; i < lens.size(); ++i) {
                        this.addStatement(new StmtAssert(new ExprBinary(lens.get(i),
                                "<=", new ExprConstInt(maxlens.get(i))),
                                "embedded array size must < max size", false));
                    }
                }
                if (coretyp.isStruct()) {
                    locate(rhs, rhsLoc, addStmts);
                    this.addStatements(addStmts);
                    expandAssign(lhsLoc, rhsLoc);
                } else {
                    this.addStatements(addStmts);
                    value = (Expression) value.accept(this);
                    addAssign(lhsLoc, field, value, lens);
                }
            }
            return null;
        } else {
            Type t = this.getType(lhs);
            if (!t.isArray() && !t.isStruct()) {
                return super.visitStmtAssign(stmt);
            }
            boolean a = locate(lhs, lhsLoc, addStmts);
            if (!a) {
                return super.visitStmtAssign(stmt);
            }
            boolean b = locate(rhs, rhsLoc, addStmts);
            // assert a == b : "both sides must be the same type!";
            this.addStatements(addStmts);
            expandAssign(lhsLoc, rhsLoc);
            return null;
        }

    }

    private void addAssign(LocationInfo lhsLoc, String field, Expression value,
            List<Expression> lens)
    {
        ExprVar v = structs.get(lhsLoc.var.getName()).get(field);
        Expression lhs = constructEar(v, lhsLoc.indices);
        if (lens.isEmpty()) {
            this.addStatement(new StmtAssign(lhs, value));
        } else {
            List<ExprVar> iter = new ArrayList<ExprVar>();
            for (int i = 0; i < lens.size(); ++i) {
                ExprVar var = new ExprVar(lhs, varGen.nextVar("iter"));
                iter.add(var);
                lhs = new ExprArrayRange(lhs, var);
                value = new ExprArrayRange(value, var);
            }
            Statement stmt = new StmtAssign(lhs, value);
            for (int i = lens.size() - 1; i >= 0; --i) {
                stmt = new StmtFor(iter.get(i).getName(), lens.get(i), stmt);
            }
            this.addStatement(stmt);
        }
    }

    private void expandAssign(LocationInfo lhsLoc, LocationInfo rhsLoc)
    {
        ExprVar lhs = lhsLoc.var;
        ExprVar rhs = rhsLoc.var;
        if (lhsLoc.typ.isStruct()) {
            assert rhsLoc.typ.isStruct() : "both sides must have same type";
            StructDef t = nres.getStruct(((TypeStructRef) lhsLoc.typ).getName());
            for (StructFieldEnt en : t.getFieldEntriesInOrder()) {
                String field = en.getName();
                Type typ = en.getType();
                if (typ.isArray()) {
                    typ = ((TypeArray) typ).getAbsoluteBase();
                }
                LocationInfo newlhs = new LocationInfo();
                newlhs.var = structs.get(lhs.getName()).get(field);
                newlhs.typ = typ;
                newlhs.indices = lhsLoc.indices;
                LocationInfo newrhs = new LocationInfo();
                newrhs.var = structs.get(rhs.getName()).get(field);
                newrhs.typ = typ;
                newrhs.indices = rhsLoc.indices;
                expandAssign(newlhs, newrhs);
            }
        } else {
            assert !rhsLoc.typ.isStruct() : "both sides must have same type";
            this.addStatement(new StmtAssign(constructEar(lhs, lhsLoc.indices),
                    constructEar(rhs, rhsLoc.indices)));
        }
    }

    private void expandPassParam(Collection<Expression> c, LocationInfo loc) {
        ExprVar rhs = loc.var;
        if (loc.typ.isStruct()) {
            StructDef t = nres.getStruct(((TypeStructRef) loc.typ).getName());
            for (StructFieldEnt en : t.getFieldEntriesInOrder()) {
                String field = en.getName();
                Type typ = en.getType();
                if (typ.isArray()) {
                    typ = ((TypeArray) typ).getAbsoluteBase();
                }
                LocationInfo newloc = new LocationInfo();
                newloc.var = structs.get(rhs.getName()).get(field);
                newloc.typ = typ;
                newloc.indices = loc.indices;
                expandPassParam(c, newloc);
            }
        } else {
            c.add(constructEar(rhs, loc.indices));
        }
    }

    @Override
    public Object visitExprFunCall(ExprFunCall efc) {
        boolean changed = false;

        List<Expression> param = efc.getParams();
        List<Expression> newp = new ArrayList<Expression>();

        List<Statement> addStmts = new ArrayList<Statement>();
        LocationInfo loc = new LocationInfo();
        for (Expression p : param) {
            if (!(p instanceof ExprArrayInit)) {
                Type t = getType(p);
                boolean r = (t.isArray() || t.isStruct()) && locate(p, loc, addStmts);
                if (r) {
                    changed = true;
                    expandPassParam(newp, loc);
                    continue;
                }
            }
            Expression e = (Expression) p.accept(this);
            if (e != p) {
                changed = true;
            }
            newp.add(e);
        }

        if (changed) {
            this.addStatements(addStmts);
            return new ExprFunCall(efc, efc.getName(), newp);
        } else {
            return efc;
        }
    }
}