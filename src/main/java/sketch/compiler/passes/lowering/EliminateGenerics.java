package sketch.compiler.passes.lowering;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.NameResolver;
import sketch.compiler.ast.core.Package;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.SymbolTable;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.exprs.regens.ExprAlt;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtSwitch;
import sketch.compiler.ast.core.typs.StructDef;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeStructRef;
import sketch.compiler.dataflow.CloneHoles;
import sketch.compiler.passes.preprocessing.RemoveADTHoles;

// Eliminate generics and simultaneously expands repeat case and ADT holes
public class EliminateGenerics extends RemoveADTHoles {
	Map<String, String> signatures = new HashMap<String, String>();
    Map<String, List<Function>> newfuns = new HashMap<String, List<Function>>();
    final List<String> empty = new ArrayList<String>();
	final boolean onlyGenerators;
    public EliminateGenerics() {
		super(null, 0, 0);
		onlyGenerators = false;
	}

	public EliminateGenerics(boolean onlyGenerators, TempVarGen varGen,
			int arrSize, int gucDepth) {
		super(varGen, arrSize, gucDepth);
		this.onlyGenerators = onlyGenerators;
    }

    String signature(Function f, TypeRenamer tr) {
        StringBuffer ls = new StringBuffer();
        ls.append(f.getFullName());
        ls.append(':');
        for (Parameter p : f.getParams()) {
            ls.append(p.getType().accept(tr));
            ls.append(",");
        }
		// also append out type
		ls.append(f.getReturnType().accept(tr));
        return ls.toString();
    }

    public Object visitExprFunCall(ExprFunCall efc){
        Function f = nres.getFun(efc.getName());
        if (onlyGenerators && !f.isGenerator()) {
			return super.visitExprFunCall(efc);
        }
        List<String> tps = f.getTypeParams();
        if (tps.isEmpty()) {
            return super.visitExprFunCall(efc);
        }
        List<Type> lt = new ArrayList<Type>();        
        for (Expression actual : efc.getParams()) {
            lt.add(getType(actual));
        }
<<<<<<< mine

        TypeRenamer tr = SymbolTableVisitor.getRenaming(f, lt);
		// Unify return type
		Type retType = f.getReturnType();
		retType = tr.rename(retType);
		if (expType != null && tps.contains(retType.toString())) {
			tr.tmap.put(retType.toString(), expType);
		}
		if (tr.tmap.isEmpty()) {
			return super.visitExprFunCall(efc);
		}
=======
        TypeRenamer tr = SymbolTableVisitor.getRenaming(f, lt, nres, null);
>>>>>>> theirs
        String sig = signature(f, tr);
        if(signatures.containsKey(sig)){
			String newName = signatures.get(sig);
            return new ExprFunCall(efc, newName, efc.getParams());
        }else{
            String newName = efc.getName() + tr.postfix();
			signatures.put(sig, newName);
            Function newSig =
                    ((Function) f.accept(tr)).creator().name(newName).typeParams(
                            empty).create();
			nres.registerFun(newSig);
			newSig = (Function) newSig.accept(this);

            String pkgname = nres.curPkg().getName();
            if (newfuns.containsKey(pkgname)) {
                newfuns.get(pkgname).add(newSig);
            } else {
                List<Function> lf = new ArrayList<Function>();
                lf.add(newSig);
                newfuns.put(pkgname, lf);
            }
            return new ExprFunCall(efc, newName, efc.getParams());
        }                
        
    }

    public Object visitProgram(Program prog) {
        nres = new NameResolver(prog);

        SymbolTable oldSymTab = symtab;
        symtab = new SymbolTable(symtab);

        List<Package> newStreams = new ArrayList<Package>();
        for (Package pk : prog.getPackages()) {
            List<Function> lf = pk.getFuncs();
            List<Function> nl = new ArrayList<Function>();
            for (Function fun : lf) {
                if (!fun.isGeneric()) {
                    nl.add(fun);
                }
            }
            Package npkg =
                    new Package(pk, pk.getName(), pk.getStructs(), pk.getVars(), nl,
                            pk.getSpAsserts());
            newStreams.add((Package) npkg.accept(this));
        }
        for (Package pk : newStreams) {
            List<Function> lf = newfuns.get(pk.getName());
            List<Function> nl = pk.getFuncs();
            if (lf != null) {
                nl.addAll(lf);
            }
        }
        symtab = oldSymTab;

        return prog.creator().streams(newStreams).create();
    }

	@Override
	public Object visitStmtSwitch(StmtSwitch stmt) {
		SymbolTable oldSymTab = symtab;
		symtab = new SymbolTable(symtab);
		ExprVar var = (ExprVar) stmt.getExpr().accept(this);

		StmtSwitch newStmt = new StmtSwitch(stmt.getContext(), var);
		TypeStructRef tres = (TypeStructRef) getType(var);
		StructDef ts = nres.getStruct(tres.getName());
		String pkg;
		if (ts == null)
			pkg = nres.curPkg().getName();
		else
			pkg = ts.getPkg();

		LinkedList<String> queue = new LinkedList<String>();
		String name = nres.getStruct(tres.getName()).getFullName();
		if (nres.isTemplate(tres.getName()))
			return super.visitStmtSwitch(stmt);
		queue.add(name);
		// List<String> children = nres.getStructChildren(tres.getName());
		if (stmt.getCaseConditions().size() == 0) {
			return super.visitStmtSwitch(stmt);
		}
		for (String c : stmt.getCaseConditions()) {
			if ("repeat".equals(c)) {

				while (!queue.isEmpty()) {

					String parent = queue.removeFirst();
					String caseName = parent.split("@")[0];
					if (!newStmt.getCaseConditions().contains(caseName)) {
						List<String> children = nres.getStructChildren(parent);
						if (children.isEmpty()) {
							SymbolTable oldSymTab1 = symtab;
							symtab = new SymbolTable(symtab);
							symtab.registerVar(var.getName(),
									(new TypeStructRef(caseName, false))
											.addDefaultPkg(pkg, nres));

							Statement body = (Statement) stmt.getBody(c)
									.accept(this);
							body = (Statement) (new CloneHoles()).process(body)
									.accept(this);
							newStmt.addCaseBlock(caseName, body);
							symtab = oldSymTab1;
						} else {
							queue.addAll(children);

						}
					}
				}

				return newStmt;
			} else {
				SymbolTable oldSymTab1 = symtab;
				symtab = new SymbolTable(symtab);
				symtab.registerVar(var.getName(),
						(new TypeStructRef(c, false)).addDefaultPkg(pkg, nres));
				Statement body = (Statement) stmt.getBody(c).accept(this);
				body = (Statement) (new CloneHoles()).process(body)
						.accept(this);
				newStmt.addCaseBlock(c, body);
				symtab = oldSymTab1;
			}
		}
		return newStmt;

	}

	public Object visitExprAlt(ExprAlt exp) {
		Expression ths = doExpression(exp.getThis());
		Expression that = doExpression(exp.getThat());
		Type lt = getType(ths);
		Type rt = getType(that);
		Type et = expType;
		if (!lt.promotesTo(et, nres)) {
			return that;
		}
		if (!rt.promotesTo(et, nres)) {
			return ths;
		}
		return (ths == exp.getThis() && that == exp.getThat()) ? exp
				: new ExprAlt(exp, ths, that);
	}

}
