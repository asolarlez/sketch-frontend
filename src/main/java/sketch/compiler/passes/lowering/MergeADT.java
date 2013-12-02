package sketch.compiler.passes.lowering;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import sketch.compiler.ast.core.Annotation;
import sketch.compiler.ast.core.NameResolver;
import sketch.compiler.ast.core.Package;
import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.exprs.ExprBinary;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprField;
import sketch.compiler.ast.core.exprs.ExprNamedParam;
import sketch.compiler.ast.core.exprs.ExprNew;
import sketch.compiler.ast.core.exprs.ExprNullPtr;
import sketch.compiler.ast.core.exprs.ExprTypeCast;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssert;
import sketch.compiler.ast.core.stmts.StmtBlock;
import sketch.compiler.ast.core.stmts.StmtIfThen;
import sketch.compiler.ast.core.stmts.StmtSwitch;
import sketch.compiler.ast.core.typs.StructDef;
import sketch.compiler.ast.core.typs.StructDef.StructFieldEnt;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.ast.core.typs.TypeStructRef;
import sketch.compiler.passes.annotations.CompilerPassDeps;
import sketch.compiler.stencilSK.VarReplacer;
import sketch.util.datastructures.HashmapList;


@CompilerPassDeps(runsBefore = {}, runsAfter = {})
public class MergeADT extends SymbolTableVisitor {
    private Map<String, StructCombinedTracker> structs;
    private int i = 0;

    public MergeADT() {
        super(null);
        structs = new HashMap<String, StructCombinedTracker>();

    }

    @Override
    public Object visitStructDef(StructDef ts) {
        return ts;
    }

    @Override
    public Object visitExprTypeCast(ExprTypeCast expr) {
        Type castedType = expr.getType();
        Expression castedExpr = expr.getExpr();
        Type castingType = getType(castedExpr);

        if (castedType.isStruct()) {
            StructDef sd = nres.getStruct(((TypeStructRef) castedType).getName());
            int id = structs.get(sd.getFullName()).getId();
            Expression condition;
            ExprField condLeft = new ExprField(castedExpr, "type");
            ExprConstInt condRight = new ExprConstInt(expr.getContext(), id);
            condition = new ExprBinary(ExprBinary.BINOP_EQ, condLeft, condRight);
            Statement assertStmt =
                    new StmtAssert(condition, "Struct type casting error", false);
            addStatement(assertStmt);
        }
        return (ExprTypeCast) super.visitExprTypeCast(expr);
    }



    @Override
    public Object visitTypeStructRef(TypeStructRef t) {
        // change this
        StructDef sd = nres.getStruct(t.getName());
        String oldName = sd.getFullName();
        StructCombinedTracker tracker = structs.get(oldName);
        TypeStructRef newType = new TypeStructRef(tracker.getNewName(), false);
        return newType;

    }

    @Override
    public Object visitExprNew(ExprNew exprNew) {
        String oldType = ((TypeStructRef) exprNew.getTypeToConstruct()).getName();
        StructDef sd = nres.getStruct(oldType);
        oldType = sd.getFullName();
        StructCombinedTracker tracker = structs.get(oldType);
        TypeStructRef newType = new TypeStructRef(tracker.getNewName(), false);
        List newParams = new ArrayList();
        ExprConstInt expr = new ExprConstInt(exprNew.getContext(), tracker.getId());
        if (tracker.ADT) {
            newParams.add(new ExprNamedParam(exprNew.getContext(), "type", expr));
        }

        for (ExprNamedParam param : exprNew.getParams()) {
            String newName = tracker.getNewVariable(param.getName());
            if (newName == null) {
                String name = nres.getStructParentName(oldType);
                while (newName == null && name != null) {
                    tracker = structs.get(name+"@"+sd.getPkg());
                    newName = tracker.getNewVariable(param.getName());
                    name = nres.getStructParentName(name);
                }
            }
            Expression newExpr = doExpression(param.getExpr());
            newParams.add(new ExprNamedParam(param.getContext(), newName, newExpr));
        }

        return new ExprNew(exprNew.getContext(), newType, newParams);

    }



    @Override
    public Object visitExprField(ExprField ef) {
        StructDef t = this.getStructDef(getType(ef.getLeft()));


        StructCombinedTracker structTracker = structs.get(t.getFullName());
        String field = ef.getName();

        String newField = structTracker.getNewVariable(field);
        // if field is a field of parent
        if (newField == null) {
            String name = nres.getStructParentName(t.getName());
            while (newField == null && name != null) {
                structTracker = structs.get(name + "@" + t.getPkg());
                newField = structTracker.getNewVariable(field);
                name = nres.getStructParentName(name);
            }
        }

        return new ExprField(ef.getLeft(), newField);
    }

    @Override
    public Object visitStmtSwitch(StmtSwitch stmt) {
        // change to if else statements
        stmt = (StmtSwitch) super.visitStmtSwitch(stmt);
        ExprVar var = stmt.getExpr();
        String pkg = this.getStructDef(getType(var)).getPkg();
        ExprField left = new ExprField(var, "type");
        List stmtIfs = new ArrayList();

        Expression condition1;
        Expression condLeft = var;
        Expression condRight = new ExprNullPtr();
        condition1 = new ExprBinary(ExprBinary.BINOP_NEQ, condLeft, condRight);
        Statement assertStmt =
                new StmtAssert(condition1, "Expression to pattern match can't be null",
                        false);

        stmtIfs.add(assertStmt);
        boolean first = true;
        Statement current = null;
        Statement prev = null;

        for (int i = stmt.getCaseConditions().size() - 1; i >= 0; i--) {
            String c = stmt.getCaseConditions().get(i);
            if (c != "default") {
                Expression condition;

                ExprConstInt right =
                        new ExprConstInt(stmt.getContext(),
                                structs.get(c + "@" + pkg).getId());
                condition = new ExprBinary(ExprBinary.BINOP_EQ, left, right);
                current =
                        new StmtIfThen(stmt.getContext(), condition, stmt.getBody(c),
                                prev);
                prev = current;
            } else {
                prev = stmt.getBody(c);
                current = prev;
            }

        }    
        stmtIfs.add(current);


        return new StmtBlock(stmtIfs);

    }

    public Object visitProgram(Program p) {

        nres = new NameResolver(p);
        List<Package> newStreams = new ArrayList<Package>();


        for (Package pkg : p.getPackages()) {
            nres.setPackage(pkg);
            List newStructs = new ArrayList();
            for (StructDef str : pkg.getStructs()) {
                if (!str.isInstantiable() && str.getParentName() == null) {
                    // then str is a parent ADT and combine it with its children.
                    StructDef ts = combineStructs(nres, str);
                    ts.setPkg(pkg.getName());
                    newStructs.add(ts);

                }
 else if (str.getParentName() == null) {
                    copyStruct(str);

                }
            }
            newStructs.addAll(pkg.getStructs());
            Package newpkg =
                    new Package(pkg, pkg.getName(), newStructs,
                            pkg.getVars(),
                            pkg.getFuncs());
            // newpkg.getStructs().addAll(newStructs);
            newStreams.add(newpkg);
        }
        Program newprog = p.creator().streams(newStreams).create();
        return super.visitProgram(newprog);
    }

    public void copyStruct(StructDef str) {
        StructCombinedTracker tracker =
                new StructCombinedTracker(str.getFullName(), str.getFullName(), i++,
                        false);
        structs.put(str.getFullName(), tracker);
        for (String var : str.getFields()) {
            tracker.mapVariable(var, var);
        }
    }

    public StructDef combineStructs(NameResolver nres, StructDef str) {
        String oldName = str.getFullName();

        String newName = "combined" + oldName.split("@")[0];
        List structsList = new ArrayList();
        for (String i : nres.structNamesList()) {
            structsList.add(i.split("@")[0]);
        }
        while (structsList.contains(newName)) {
            newName = "_" + newName;
        }
        StructDef ts = null;
        List names = new ArrayList();
        List types = new ArrayList();
        Annotation an = null;
        HashmapList<String, Annotation> annotations =
                new HashmapList<String, Annotation>();
        LinkedList<String> list = new LinkedList<String>();
        list.add(oldName);
        String type = "type";
        names.add("type");
        types.add(TypePrimitive.int32type);
        while (!list.isEmpty()) {
            String name = list.removeFirst();
            StructDef childStruct = nres.getStruct(name);
            StructCombinedTracker tracker =
                    new StructCombinedTracker(name, newName, i++, true);
            structs.put(childStruct.getFullName(), tracker);

            List<VarReplacer> vrs = new ArrayList<VarReplacer>();


            for (StructFieldEnt var : childStruct.getFieldEntriesInOrder()) {
                String newVarname = name.split("@")[0] + "_" + var.getName();
                tracker.mapVariable(var.getName(), newVarname);
                names.add(newVarname);
                VarReplacer vr = new VarReplacer(var.getName(), newVarname);
                vrs.add(vr);
                Type newType = (Type) var.getType().accept(this);
                if (newType.isArray()) {
                    for (VarReplacer v : vrs) {
                        newType = (Type) newType.accept(v);
                    }
                }

                types.add(newType);
            }
            for (String child : nres.getStructChildren(name)) {
                list.add(child);
            }

        }

        ts =
                StructDef.creator(str.getContext(), newName, null, true, names, types,
                        annotations).create();

        return ts;

    }

    public class StructCombinedTracker {
        String prevStruct;
        String newStruct;
        Map<String, String> varMapping = new HashMap<String, String>();
        int id;
        boolean ADT;

        public StructCombinedTracker(String prevStruct, String newStruct, int id,
                boolean ADT)
        {
            this.prevStruct = prevStruct;
            this.newStruct = newStruct;
            this.id = id;
            this.ADT = ADT;
        }

        public void mapVariable(String prevVar, String newVar) {
            varMapping.put(prevVar, newVar);
        }

        public String getNewVariable(String preVar) {
            return varMapping.get(preVar);
        }

        public String getNewName() {
            return newStruct;
        }

        public int getId() {
            return id;
        }

    }

}
