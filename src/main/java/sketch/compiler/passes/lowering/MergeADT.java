package sketch.compiler.passes.lowering;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import sketch.compiler.ast.core.Annotation;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.NameResolver;
import sketch.compiler.ast.core.Package;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.exprs.*;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssert;
import sketch.compiler.ast.core.stmts.StmtBlock;
import sketch.compiler.ast.core.stmts.StmtIfThen;
import sketch.compiler.ast.core.stmts.StmtSpAssert;
import sketch.compiler.ast.core.stmts.StmtSwitch;
import sketch.compiler.ast.core.typs.StructDef;
import sketch.compiler.ast.core.typs.StructDef.StructFieldEnt;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.ast.core.typs.TypeStructRef;
import sketch.compiler.passes.annotations.CompilerPassDeps;
import sketch.compiler.stencilSK.VarReplacer;
import sketch.util.datastructures.HashmapList;


@CompilerPassDeps(runsBefore = {}, runsAfter = {})
public class MergeADT extends SymbolTableVisitor {
    private Map<String, StructCombinedTracker> structs;
    private Map<String, Integer> structsCount;
    public MergeADT() {
        super(null);
        structs = new HashMap<String, StructCombinedTracker>();
        structsCount = new HashMap<String, Integer>();

    }

    @Override
    public Object visitStructDef(StructDef ts) {
        return ts;
    }

    @Override
    public Object visitExprTypeCast(ExprTypeCast expr) {
        Type castedType = expr.getType();
        Type castingType = getType(expr.getExpr());
        Expression castedExpr = (Expression) expr.getExpr().accept(this);


        if (castedType.isStruct()) {
            StructDef sd = nres.getStruct(((TypeStructRef) castedType).getName());
            StructCombinedTracker tracker = structs.get(sd.getFullName());
            int lowid = tracker.low_id;
            int highid = tracker.high_id;
            Expression condition;
            ExprField condLeft = new ExprField(castedExpr, "type");
            ExprConstInt right1 = new ExprConstInt(expr.getContext(), lowid);
            Expression cond1 = new ExprBinary(ExprBinary.BINOP_GE, condLeft, right1);

            ExprConstInt right2 = new ExprConstInt(expr.getContext(), highid);
            Expression cond2 = new ExprBinary(ExprBinary.BINOP_LE, condLeft, right2);

            condition = new ExprBinary(ExprBinary.BINOP_AND, cond1, cond2);
            Statement assertStmt =
                    new StmtAssert(condition, "Struct type casting error: " +
                            expr.getCx(), false);
            addStatement(assertStmt);
        }
        return (ExprTypeCast) super.visitExprTypeCast(expr);
    }



    @Override
    public Object visitTypeStructRef(TypeStructRef t) {
        StructDef sd = nres.getStruct(t.getName());

        if (sd == nres.template) {
            return t;
        }

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
        if (!sd.isInstantiable()) {
            Expression expr = exprNew.getStar();
            if (tracker.ADT) {
                newParams.add(new ExprNamedParam(exprNew.getContext(), "type", expr));
            }
            LinkedList<String> queue = new LinkedList<String>();
            queue.add(oldType);

            while (!queue.isEmpty()) {
                boolean first = true;
                String parent = queue.removeFirst();
                List<String> children = nres.getStructChildren(parent);
                if (children.isEmpty()) {
                    StructDef str = nres.getStruct(parent);
                    StructCombinedTracker childtracker = structs.get(parent);
                    Map<String, Integer> map = new HashMap<String, Integer>();
                    for (ExprNamedParam p : exprNew.getParams()) {
                        if (!map.containsKey(p.getName()) &&
                                str.hasField(p.getName()) &&
                                getType(p.getExpr()).promotesTo(
                                        str.getFieldTypMap().get(p.getName()), nres))
                        {
                            map.put(p.getName(), 1);
                            String newName = childtracker.getNewVariable(p.getName());
                            Expression newExpr = doExpression(p.getExpr());
                            newParams.add(new ExprNamedParam(exprNew.getContext(),
                                    newName, newExpr));
                        }
                    }
                } else {
                    queue.addAll(children);
                }
            }
            // add assert statement for hole
            Expression cond =
                    new ExprBinary(exprNew, ExprBinary.BINOP_LT, expr, new ExprConstInt(
                            structsCount.get(tracker.newStruct)));

            this.addStatement(new StmtAssert(exprNew, cond, "Type length constraint",
                    StmtAssert.UBER));
            return new ExprNew(exprNew.getContext(), newType, newParams, false);

        } else {
            int id = tracker.getLowId();
            assert (id == tracker.getHighId());
            ExprConstInt expr = new ExprConstInt(exprNew.getContext(), id);
            if (tracker.ADT) {
                newParams.add(new ExprNamedParam(exprNew.getContext(), "type", expr));
            }

            for (ExprNamedParam param : exprNew.getParams()) {
                String newName = tracker.getNewVariable(param.getName());
                if (newName == null) {
                    String name = nres.getStructParentName(oldType);
                    while (newName == null && name != null) {

                        StructCombinedTracker parentTracker = structs.get(name);
                        newName = parentTracker.getNewVariable(param.getName());
                        name = nres.getStructParentName(name);
                    }
                }
                Expression newExpr = doExpression(param.getExpr());
                newParams.add(new ExprNamedParam(param.getContext(), newName, newExpr));
            }

            return new ExprNew(exprNew.getContext(), newType, newParams, false);
        }

    }


    public String newFieldName(String field, StructDef t) {
        StructCombinedTracker structTracker = structs.get(t.getFullName());
        String newField = structTracker.getNewVariable(field);
        // if field is a field of parent
        if (newField == null) {
            String name = nres.getStructParentName(t.getFullName());
            while (newField == null && name != null) {
                structTracker = structs.get(name);
                newField = structTracker.getNewVariable(field);
                name = nres.getStructParentName(name);
            }
        }
        return newField;
    }

    @Override
    public Object visitExprField(ExprField ef) {
        StructDef t = this.getStructDef(getType(ef.getLeft()));

        StructCombinedTracker structTracker = structs.get(t.getFullName());
        String field = ef.getName();

        String newField = newFieldName(field, t);
        Expression basePtr = (Expression) ef.getLeft().accept(this);

        return new ExprField(basePtr, newField);
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
                new StmtAssert(condition1, "Expression to pattern match can't be null: " +
                        stmt.getCx(),
                        false);

        stmtIfs.add(assertStmt);
        boolean first = true;
        Statement current = null;
        Statement prev = null;

        for (int i = stmt.getCaseConditions().size() - 1; i >= 0; i--) {
            String c = stmt.getCaseConditions().get(i);
            if (c != "default") {


                ExprConstInt low =
                        new ExprConstInt(stmt.getContext(),
                                structs.get(c + "@" + pkg).getLowId());
                Expression cond1 = new ExprBinary(ExprBinary.BINOP_GE, left, low);

                ExprConstInt high =
                        new ExprConstInt(stmt.getContext(),
                                structs.get(c + "@" + pkg).getHighId());

                Expression cond2 = new ExprBinary(ExprBinary.BINOP_LE, left, high);

                Expression condition = new ExprBinary(ExprBinary.BINOP_AND, cond1, cond2);
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

    class Pair<F, S> {
        private F first;
        private S second;

        public Pair(F f, S s) {
            first = f;
            second = s;
        }

        public F getFirst() {
            return first;
        }

        public S getSecond() {
            return second;
        }
    }
    public Object visitProgram(Program p) {

        nres = new NameResolver(p);
        List<Package> newStreams = new ArrayList<Package>();
        Map<String, Pair<List<Type>, List<Type>>> specialStructs =
                new HashMap<String, Pair<List<Type>, List<Type>>>();
        // first add all trackers for adts
        for (Package pkg : p.getPackages()) {
            nres.setPackage(pkg);
            List newStructs = new ArrayList();
            // Structs that should contain extra fields
            for (StmtSpAssert sa : pkg.getSpAsserts()) {
                ExprFunCall f1 = sa.getFirstFun();
                Function f = nres.getFun(f1.getName());
                List<Parameter> params = f.getParams();
                String outName = "";
                int state = 0;
                boolean first = true;
                List<Type> stateTypes = new ArrayList<Type>();
                List<Type> outTypes = new ArrayList<Type>();
                for (Parameter pp : params) {
                    if (first && pp.isParameterInput()) {
                        Type t = pp.getType();
                        assert (t.isStruct());
                        outName = ((TypeStructRef) pp.getType()).getName().split("@")[0];
                        first = false;
                    } else
                    if (pp.isParameterReference()) {
                        Type t = pp.getType();
                        assert (!t.isStruct());
                        if (t.isArray()) {
                            TypeArray ta = (TypeArray) t;
                            Type base = ta.getBase();
                            int len = ((ExprConstInt) ta.getLength()).getVal();
                            for (int i = 0; i < len; i++) {
                                stateTypes.add(base);
                            }

                        } else {
                            assert (t.promotesTo(TypePrimitive.inttype, nres));
                            stateTypes.add(TypePrimitive.inttype);
                        }
                    } else if (pp.isParameterOutput()) {
                        Type t = pp.getType();
                        if (t.isArray()) {
                            TypeArray ta = (TypeArray) t;
                            Type base = ta.getBase();
                            int len = ((ExprConstInt) ta.getLength()).getVal();
                            for (int i = 0; i < len; i++) {
                                outTypes.add(base);
                            }
                        } else {
                            outTypes.add(t);
                        }
                    }


                }
                sa.setStateCount(state);
                specialStructs.put(outName, new Pair(stateTypes, outTypes));
            }

            for (StructDef str : pkg.getStructs()) {
                if (!str.isInstantiable() && str.getParentName() == null) {
                    // then str is a parent ADT and combine it with its children.
                    createTracker(nres, str);
                } else if (str.getParentName() == null) {
                    copyStruct(str);
                }
            }
        }
        // Now add the structs
        for (Package pkg : p.getPackages()) {
            nres.setPackage(pkg);
            List newStructs = new ArrayList();
            for (StructDef str : pkg.getStructs()) {
                if (!str.isInstantiable() && str.getParentName() == null) {
                    // then str is a parent ADT and combine it with its children.
                    String name = str.getName().split("@")[0];
                    StructDef ts =
                            combineStructs(
                                    nres,
                                    str,
                                    specialStructs.containsKey(name) ? specialStructs.get(name)
                                            : null);
                    ts.setPkg(pkg.getName());
                    newStructs.add(ts);

                } else if (str.getParentName() == null) {
                    newStructs.add(super.visitStructDef(str));
                }

            }
            List allStructs = new ArrayList();
            allStructs.addAll(pkg.getStructs());
            allStructs.addAll(newStructs);

            // Package newpkg = (Package) super.visitPackage(pkg);
            Package newpkg =
                    new Package(pkg, pkg.getName(), allStructs, pkg.getVars(),
                            pkg.getFuncs(), pkg.getSpAsserts());
            newpkg = (Package) super.visitPackage(newpkg);
            // newpkg.getStructs().addAll(newStructs);
            newStreams.add(new Package(newpkg, newpkg.getName(), newStructs,
                    newpkg.getVars(), newpkg.getFuncs(), newpkg.getSpAsserts()));
        }
        Program newprog = p.creator().streams(newStreams).create();
        return newprog;
        // return super.visitProgram(newprog);
    }

    public void copyStruct(StructDef str) {
        StructCombinedTracker tracker =
                new StructCombinedTracker(str.getFullName(), str.getFullName(), 0, 0,
                        false);
        structs.put(str.getFullName(), tracker);
        for (String var : str.getFields()) {
            tracker.mapVariable(var, var);
        }
    }

    public void createTracker(NameResolver nres, StructDef str) {
        String oldName = str.getFullName();
        int count = 0;
        String newName = "combined" + oldName.split("@")[0];

        List structsList = new ArrayList();
        for (String i : nres.structNamesList()) {
            structsList.add(i.split("@")[0]);
        }
        while (structsList.contains(newName)) {
            newName = "_" + newName;
        }
        newName = newName + "_" + str.getPkg();

        count = createRecTracker(oldName, newName, count);
        structsCount.put(newName, count);
    }

    private int createRecTracker(String name, String newName, int count) {
        StructDef childStruct = nres.getStruct(name);
        List<String> children = nres.getStructChildren(name);
        int low = -1, high = -1;
        if (children.isEmpty()) {
            low = count++;
            high = low;
        } else {
            low = count;
            for (String child : children) {
                count = createRecTracker(child, newName, count);
            }
            assert (count > low);
            high = count - 1;
        }
        StructCombinedTracker tracker =
                new StructCombinedTracker(name, newName, low, high, true);
        structs.put(childStruct.getFullName(), tracker);
        return count;

    }

    StructDef globalCurStruct = null;

    public Object visitExprVar(ExprVar ev) {

        if (globalCurStruct != null) {
            return new ExprVar(ev, newFieldName(ev.getName(), globalCurStruct));
        }

        Object o = super.visitExprVar(ev);

        return o;
    }

    public StructDef combineStructs(NameResolver nres, StructDef str,
            Pair<List<Type>, List<Type>> pair)
    {
        String oldName = str.getFullName();
        String newName = structs.get(oldName).newStruct;
        StructDef ts = null;
        List names = new ArrayList();
        List types = new ArrayList();
        Annotation an = null;
        HashmapList<String, Annotation> annotations =
                new HashmapList<String, Annotation>();
        LinkedList<String> list = new LinkedList<String>();
        list.add(oldName);
        // Add annotation for immutability
        // Type checker makes sure that children are also immutable
        if (str.immutable()) {
            annotations.append("Immutable", new Annotation(str.getContext(), "Immutable",
                    ""));
        }
        str.resetImmutable();
        String type = "type";
        names.add("type");
        types.add(TypePrimitive.int32type);
        while (!list.isEmpty()) {
            String name = list.removeFirst();
            StructDef childStruct = nres.getStruct(name);
            childStruct.resetImmutable();
            StructCombinedTracker tracker = structs.get(childStruct.getFullName());
            // structs.put(childStruct.getFullName(), tracker);

            List<VarReplacer> vrs = new ArrayList<VarReplacer>();


            for (StructFieldEnt var : childStruct.getFieldEntriesInOrder()) {
                String newVarname = name.split("@")[0] + "_" + var.getName();
                tracker.mapVariable(var.getName(), newVarname);
                names.add(newVarname);
                VarReplacer vr = new VarReplacer(var.getName(), newVarname);
                vrs.add(vr);
                globalCurStruct = childStruct;
                Type newType = (Type) var.getType().accept(this);
                globalCurStruct = null;
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

        if (pair != null) {
            // Add extra fields to the tuple in the following order
            // orig fields | in state | out state | out
            // node
            List<Type> stateTypes = pair.getFirst();
            for (int k = 0; k < stateTypes.size(); k++) {
                names.add("st_in" + k);
                types.add(stateTypes.get(k).accept(this));
            }

            for (int k = 0; k < stateTypes.size(); k++) {
                names.add("st_out" + k);
                types.add(stateTypes.get(k).accept(this));
            }

            List<Type> outTypes = pair.getSecond();
            for (int k = 0; k < outTypes.size(); k++) {
                names.add("out_" + k);
                types.add(outTypes.get(k).accept(this));
            }


        }
        ts =
                StructDef.creator(str.getContext(), newName, null, true, names, types,
                        annotations).create();
        ts.setPkg(str.getPkg());
        // structs.get(str.getFullName()).newStruct = ts.getFullName();

        return ts;

    }

    public class StructCombinedTracker {
        String prevStruct;
        String newStruct;
        Map<String, String> varMapping = new HashMap<String, String>();
        int low_id, high_id;
        boolean ADT;

        public StructCombinedTracker(String prevStruct, String newStruct, int low_id,
                int high_id,
                boolean ADT)
        {
            this.prevStruct = prevStruct;
            this.newStruct = newStruct;
            this.low_id = low_id;
            this.high_id = high_id;
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

        public int getLowId() {
            return low_id;
        }

        public int getHighId() {
            return high_id;
        }

    }

}
