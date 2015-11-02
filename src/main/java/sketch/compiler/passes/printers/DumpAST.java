package sketch.compiler.passes.printers;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.StructDef;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.ast.core.typs.TypeStructRef;
import sketch.util.datastructures.TypedHashMap;

public class DumpAST extends FEReplacer {

    int id = 0;
    PrintStream out = System.out;

    public Object visitStructDef(StructDef ts) {
        int lid = id++;
        out.print("(" + lid + "");
        out.print(" StructDef " + ts.getName());

        boolean changed = false;
        TypedHashMap<String, Type> map = new TypedHashMap<String, Type>();
        fields = new HashSet<String>();

        StructDef sdf = ts;

        for (Entry<String, Type> entry : sdf) {
            fields.add(entry.getKey());
        }
        out.println(" " + lid + ")");
        return ts;
    }

    public Object visitParameter(Parameter par) {
        int lid = id++;
        out.print("(" + lid + "");
        out.print(" Parameter ");

        out.print(par);

        out.print(" " + lid + ")");
        return par;
    }

    public Object visitTypeStructRef(TypeStructRef ts) {
        int lid = id++;
        out.print("(" + lid + " ");

        out.print("TypeStructRef ");
        if (ts.isUnboxed()) {
            out.print("Unboxed ");
        }
        out.print(ts.getName());
        out.print(" " + lid + ")");
        return ts;
    }

    public Object visitTypePrimitive(TypePrimitive t) {
        return visitType(t);
    }

    public Object visitTypeArray(TypeArray t) {
        int lid = id++;
        out.print("(" + lid + " ");

        out.print("TypeArray ");
        t.getBase().accept(this);

        out.print(" " + lid + ")");
        return t;
    }

    public Object visitType(Type t) {
        int lid = id++;
        out.print("(" + lid + " ");

        out.print(t);

        out.print(" " + lid + ")");
        return t;
    }

    public Object visitStmtVarDecl(StmtVarDecl stmt) {
        int lid = id++;
        out.print("(" + lid + " ");
        out.print("StmtVarDecl ");
        List<Expression> newInits = new ArrayList<Expression>();
        List<Type> newTypes = new ArrayList<Type>();
        boolean changed = false;
        for (int i = 0; i < stmt.getNumVars(); i++) {

            Expression oinit = stmt.getInit(i);
            Expression init = null;
            if (oinit != null)
                init = doExpression(oinit);
            Type ot = stmt.getType(i);
            ot.accept(this);

        }
        out.print(" " + lid + ")");
        return stmt;
    }

    public Object visitFunction(Function func) {
        int lid = id++;
        out.print("(" + lid + "");
        out.print(" Function " + func.getName() + " ");
        if (func.isGenerator()) {
            out.print(" generator ");
        }

        List<Parameter> newParam = new ArrayList<Parameter>();
        Iterator<Parameter> it = func.getParams().iterator();
        boolean samePars = true;
        while (it.hasNext()) {
            Parameter par = it.next();
            Parameter newPar = (Parameter) par.accept(this);
            if (par != newPar)
                samePars = false;
            newParam.add(newPar);
        }

        Type rtype = (Type) func.getReturnType().accept(this);

        if (func.getBody() == null) {
            assert func.isUninterp() : "Only uninterpreted functions are allowed to have null bodies.";
            if (samePars && rtype == func.getReturnType())
                return func;
            return func.creator().returnType(rtype).params(newParam).create();
        }
        Statement newBody = (Statement) func.getBody().accept(this);
        out.println(" " + lid + ")");
        return func;
    }

}
