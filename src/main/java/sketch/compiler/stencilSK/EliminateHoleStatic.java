package sketch.compiler.stencilSK;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import sketch.compiler.ast.core.FEContext;
import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.NameResolver;
import sketch.compiler.ast.core.Package;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.exprs.ExprArrayInit;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.ExprNamedParam;
import sketch.compiler.ast.core.exprs.ExprNew;
import sketch.compiler.ast.core.exprs.ExprHole;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtExpr;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.StructDef;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.ast.core.typs.TypeStructRef;
import sketch.compiler.passes.lowering.SymbolTableVisitor;
import sketch.compiler.solvers.constructs.AbstractValueOracle;
import sketch.compiler.solvers.constructs.StaticHoleTracker;
import sketch.util.DebugOut;
import sketch.util.exceptions.ExceptionAtNode;

public class EliminateHoleStatic extends SymbolTableVisitor {

    AbstractValueOracle oracle;
    HashMap<ExprHole, Object> hole_values = new HashMap<ExprHole, Object>();
    HashMap<String, HashMap<Integer, String>> idsMap =
            new HashMap<String, HashMap<Integer, String>>();

    public EliminateHoleStatic(AbstractValueOracle oracle) {
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
            while (str.getParentName() != null) {
                parentName = str.getParentName();
                str = nres.getStruct(parentName);
            }
            if (!idsMap.containsKey(parentName)) {

                HashMap<Integer, String> ids = new HashMap<Integer, String>();
                LinkedList<String> list = new LinkedList<String>();
                list.add(parentName);
                int count = 0;
                while (!list.isEmpty()) {
                    String name = list.removeFirst();
                    StructDef childStruct = nres.getStruct(name);
                    List<String> children = nres.getStructChildren(name);
                    if (children.isEmpty())
                        ids.put(count++, name);
                    for (String child : children) {
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
				String variantName = p.getVariantName();
				if (variantName != null) {
					variantName = variantName.split("@")[0];
				}
				if ((variantName == null || (variantName != null && variantName
						.equals(str.getName())))
						&& !map.containsKey(p.getName())
						&&
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

    public Object visitExprStar(ExprHole star) {
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

		} else {
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

    public Object visitProgram(Program p) {

        if (oracle.hasFunctions()) {
            List<Package> pkgs = p.getPackages();
            Package pkg = pkgs.get(0);
            for (Function f : oracle.getFunctions()) {
                f.setPkg(pkg.getName());
                pkg.getFuncs().add(f);
            }
        }

        return super.visitProgram(p);
    }

    int icnt = 0;

    @Override
    public Object visitExprFunCall(final ExprFunCall efc) {
        Function f = nres.getFun(efc.getName());

        if (f.hasAnnotation("Gen")) {
            Expression texp = oracle.generatorHole(efc.getName());
            if (texp == null) {
                String fullName = nres.getFunName(efc.getName());
                texp = oracle.generatorHole(fullName.replace("@", "_"));
            }
            if (texp == null) {
                return super.visitExprFunCall(efc);
            }
            final Expression exp = texp;
            Map<String, Expression> repl = new HashMap<String, Expression>();
            int i = 0;
            Iterator<Parameter> pit = f.getParams().iterator();
            Expression ev = null;
            for (Expression param : efc.getParams()) {
                Parameter formal = pit.next();
                if (formal.isParameterOutput()) {
                    ev = param;
                } else {
                    repl.put("IN_" + i, (Expression) param.accept(this));
                }
                ++i;

            }
            /*
             * if(exp instanceof ExprFunCall){ ExprVar ev = new ExprVar(efc,
             * "__OUT" + icnt); ++icnt;
             * 
             * addStatement(new StmtVarDecl(efc, TypePrimitive.floattype,
             * ev.getName(), null)); addStatement() }
             */
            VarReplacer vr = new VarReplacer(repl);

            final NameResolver lnres = nres;
            final EliminateHoleStatic ths = this;

            FEReplacer elimFuns = new FEReplacer() {
                @Override
                public Object visitExprFunCall(ExprFunCall fcall) {
                    Function f = lnres.getFun(fcall.getName());
					if (f == null) {
						throw new ExceptionAtNode("Backend returned a bad expression " + exp, efc);
					}
                    int fpsize = f.getParams().size();
					int fcallsize = fcall.getParams().size();

					if (fpsize == fcallsize) {
						return super.visitExprFunCall(fcall);
					} else if (fpsize == fcallsize + 1) {
						Parameter out = f.getParams().get(fpsize - 1);
						assert out.isParameterOutput() : ("Not an output parameter: " + out);
						String outname = out.getName();
						ths.addStatement(new StmtVarDecl(efc, out.getType(), outname, null));

						List<Expression> params = new ArrayList<Expression>();
						for (Expression old : fcall.getParams()) {
							params.add((Expression) old.accept(this));
						}
						Expression rvar = new ExprVar(efc, outname);
						// TODO:make sure there are no local name conflicts with
						// outname
						params.add(rvar);

						ths.addStatement(new StmtExpr(new ExprFunCall(efc, fcall.getName(), params)));

						return rvar;
					} else {
						throw new ExceptionAtNode("Backend returned a bad expression " + exp, efc);
                    }
                    // Declare return var;

                    // add extra parameter to fcall.
                    // return the fresh var we created.

                }
            };

            Expression exp2 = (Expression) exp.accept(vr);
            exp2 = (Expression) exp2.accept(elimFuns);
            addStatement(new StmtAssign(ev, exp2));
            return null;
        } else {
            return super.visitExprFunCall(efc);
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
        for (Entry<ExprHole, Object> ent : hole_values.entrySet()) {
            FEContext ctx = ent.getKey().getContext();
            out.print("    <hole_value line=\"" + ctx.getLineNumber() + "\" col=\"" +
                    ctx.getColumnNumber() + "\"  name=\"" + ent.getKey().getHoleName() +
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
