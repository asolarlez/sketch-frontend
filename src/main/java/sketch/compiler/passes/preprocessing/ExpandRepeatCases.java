package sketch.compiler.passes.preprocessing;

import java.util.LinkedList;
import java.util.List;

import sketch.compiler.ast.core.SymbolTable;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtSwitch;
import sketch.compiler.ast.core.typs.StructDef;
import sketch.compiler.ast.core.typs.TypeStructRef;
import sketch.compiler.dataflow.CloneHoles;
import sketch.compiler.passes.lowering.SymbolTableVisitor;

/**
 * This class expands repeat_case construct by creating a new case for each variant and
 * instantiates the holes in the body differently for each variant.
 */
public class ExpandRepeatCases extends SymbolTableVisitor {

	public ExpandRepeatCases() {
		super(null);

	}

	@Override
	public Object visitStmtSwitch(StmtSwitch stmt){
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
		for(String c : stmt.getCaseConditions()) {
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

							Statement body = (Statement) stmt.getBody(c).accept(this);
							body = (Statement) (new CloneHoles()).process(body)
									.accept(
											this);
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
				body = (Statement) (new CloneHoles()).process(body).accept(this);
				newStmt.addCaseBlock(c, body);
				symtab = oldSymTab1;
			}
		}
		return newStmt;

	}

}
