package sketch.compiler.stencilSK;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import sketch.compiler.ast.core.FEContext;
import sketch.compiler.ast.core.exprs.ExprArrayInit;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprNamedParam;
import sketch.compiler.ast.core.exprs.ExprNew;
import sketch.compiler.ast.core.exprs.ExprStar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.typs.StructDef;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.ast.core.typs.TypeStructRef;
import sketch.compiler.passes.lowering.SymbolTableVisitor;
import sketch.compiler.solvers.constructs.AbstractValueOracle;
import sketch.compiler.solvers.constructs.StaticHoleTracker;
import sketch.util.DebugOut;

public class EliminateStarStatic extends SymbolTableVisitor {

    AbstractValueOracle oracle;
    HashMap<ExprStar, Object> hole_values = new HashMap<ExprStar, Object>();
    HashMap<String, HashMap<Integer, String>> idsMap =
            new HashMap<String, HashMap<Integer, String>>();

    public EliminateStarStatic(AbstractValueOracle oracle) {
        super(null);
        assert oracle.getHoleNamer() instanceof StaticHoleTracker;
        this.oracle = oracle;
        oracle.initCurrentVals();
    }

    public Object visitExprNew(ExprNew exp) {
        exp = (ExprNew) super.visitExprNew(exp);
        if (exp.isHole()) {
            int value = ((ExprConstInt) exp.getStar().accept(this)).getVal();

            // replace type with the correct struct
            TypeStructRef t = (TypeStructRef) exp.getTypeToConstruct();
            StructDef str = nres.getStruct(t.getName());
            String parentName = str.getFullName();
            // while (str.getParentName() != null) {
            // parentName = str.getParentName();
            // str = nres.getStruct(parentName);
            // }
            if (!idsMap.containsKey(parentName)) {

                HashMap<Integer, String> ids = new HashMap<Integer, String>();
                LinkedList<String> list = new LinkedList<String>();
                list.add(parentName);
                int count = -1;
                while (!list.isEmpty()) {
                    String name = list.removeFirst();
                    StructDef childStruct = nres.getStruct(name);
                    ids.put(count++, name);
                    for (String child : nres.getStructChildren(name)) {
                        list.add(child);
                    }
                }
                idsMap.put(parentName, ids);
            }
            String newName = idsMap.get(parentName).get(value);
            TypeStructRef newType = new TypeStructRef(newName, false);

            // Remove extra fields

            List<ExprNamedParam> newParams = new ArrayList<ExprNamedParam>();
            str = nres.getStruct(newName);
            Map<String, Integer> map = new HashMap<String, Integer>();
            for (ExprNamedParam p : exp.getParams()) {
                if (!map.containsKey(p.getName()) &&
                        str.hasField(p.getName()) &&
                        getType(p.getExpr()).promotesTo(
                                str.getFieldTypMap().get(p.getName()), nres))
                {
                    map.put(p.getName(), 1);
                    newParams.add(p);
                }
            }
            return new ExprNew(exp.getContext(), newType, newParams, false);
        }
        return exp;
    }

    public Object visitExprStar(ExprStar star) {
        if (star.isAngelicMax()) {
            return star;
        }
        Type t = star.getType();
        int ssz = 1;
        if (t instanceof TypeArray) {
            Integer iv = ((TypeArray) t).getLength().getIValue();
            assert iv != null;
            ssz = iv;
            List<Expression> lst = new ArrayList<Expression>(ssz);
            for (int i = 0; i < ssz; ++i) {
                lst.add(oracle.popValueForNode(star.getDepObject(i),
                        ((TypeArray) t).getBase()));
            }
            hole_values.put(star, lst);

            ExprArrayInit ainit = new ExprArrayInit(star, lst);
            return ainit;

        }
 else {
            if (t.equals(TypePrimitive.chartype)) {
                System.out.println("Is CT");
            }
            Expression value = oracle.popValueForNode(star.getDepObject(0), t);
            hole_values.put(star, value);
            return value;
        }
    }

    public int getIntValue(Expression expr) {
        if (expr instanceof ExprConstInt) {
            return ((ExprConstInt) expr).getVal();
        } else {
            return -1;
        }
    }



    @SuppressWarnings( { "deprecation", "unchecked" })
    public void dump_xml(String filename) {
        PrintStream out = null;
        if (filename.equals("--")) {
            out = System.out;
            out.println("=== BEGIN XML OUTPUT ===");
        } else {
            try {
                final File file = new File(filename);
                File pf = file.getParentFile();
                if (pf != null && !pf.exists()) {
                    pf.mkdirs();
                }
                out = new PrintStream(file, "UTF-8");
            } catch (Exception e) {
                DebugOut.fail_exception("opening print stream for xml out", e);
            }
        }

        out.println("<?xml version=\"1.0\"?>");
        out.println("<hole_values>");
        for (Entry<ExprStar, Object> ent : hole_values.entrySet()) {
            FEContext ctx = ent.getKey().getContext();
            out.print("    <hole_value line=\"" + ctx.getLineNumber() + "\" col=\"" +
                    ctx.getColumnNumber() + "\"  name=\"" + ent.getKey().getSname() +
                    "\" ");
            if (ent.getKey().getType() instanceof TypeArray) {
                List<Expression> lst = (List<Expression>) ent.getValue();
                out.println("type=\"array\">");
                for (Expression value : lst) {
                    out.println("        <entry value=\"" + getIntValue(value) + "\" />");
                }
                out.println("    </hole_value>");
            } else {
                out.println("type=\"int\" value=\"" +
                        getIntValue((Expression) ent.getValue()) + "\" />");
            }
        }
        out.println("</hole_values>");
        if (filename == "--") {
            out.println("------------------------------");
        }
    }
}
