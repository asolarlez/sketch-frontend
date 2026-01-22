package sketch.compiler.passes.preprocessing;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.SymbolTable;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssert;
import sketch.compiler.ast.core.stmts.StmtFor;
import sketch.compiler.monitor.Graph;
import sketch.compiler.passes.lowering.MakeBodiesBlocks;
import sketch.compiler.passes.lowering.SymbolTableVisitor;

/**
 * Front-end visitor pass that generates and incorporates the instrumentation of
 * each RM associated with each LTL formula.
 * 
 * @author Fernando A. Galicia-Mendoza &lt;fmendoza@mit.edu&gt;
 * @version $Id$
 *
 */
//public class LTLRegression extends FEReplacer {
public class LTLFuncRMInstr extends SymbolTableVisitor {

	private Graph fa;
	private List<Statement> ltlAsserts;
	private int idST;

	public LTLFuncRMInstr(List<Statement> ltlAsserts, int idST) {
		super(null);
		this.ltlAsserts = ltlAsserts;
		this.idST = idST;
	}

	public Object visitFunction(Function func) {
		SymbolTable oldSymTab = symtab;
		symtab = new SymbolTable(symtab);
		for (Parameter p : func.getParams()) {
			symtab.registerVar(p.getName(), p.getType());
		}
		if (func.getBody() == null) {
			symtab = oldSymTab;
			return super.visitFunction(func);
		}

		Statement body = func.getBody();

		Iterator<Statement> it2 = ltlAsserts.iterator();
		Iterator<Statement> it3 = ltlAsserts.iterator();

		List<String> declare = new LinkedList<>();
		List<String> assign = new LinkedList<>();
		List<String> used = new LinkedList<>();

		while (it2.hasNext()) {
			Statement ltlCurrentLine = it2.next();
			// This pass creates a list of pre_ variables
			LTLPreVars pv = new LTLPreVars(ltlCurrentLine, declare, assign, used);
			body.accept(pv);
			declare = pv.getDeclare();
			assign = pv.getAssign();
			used = pv.getUsed();

			// This pass adds the pre_ variables assigns
			body = (Statement) body.accept(new LTLPreAssigns(ltlCurrentLine, declare, assign, used, symtab));

			declare = new LinkedList<String>();
			assign = new LinkedList<String>();
		}

		declare = new LinkedList<String>();
		used = new LinkedList<String>();
		ltlAsserts = new LinkedList<>();

		while (it3.hasNext()) {
			Statement ltlCurrentLine = it3.next();
			// This pass creates a list of pre_ variables
			LTLNowVars pv = new LTLNowVars(ltlCurrentLine, declare, used);
			body.accept(pv);
			declare = pv.getDeclare();
			used = pv.getUsed();

			// This pass adds the pre_ variables assigns
			int tmpIDST = idST;
			body = (Statement) body.accept(new LTLNewInits(ltlCurrentLine, declare, idST));
			ltlCurrentLine = (StmtAssert) ltlCurrentLine.accept(new LTLNewDecls(declare, idST));
			idST = tmpIDST == idST ? idST : idST + 1;
			ltlAsserts.add(ltlCurrentLine);

			declare = new LinkedList<String>();
		}

		Iterator<Statement> it = ltlAsserts.iterator();

		while (it.hasNext()) {
			Statement ltlCurrentLine = it.next();
			if (ltlCurrentLine instanceof StmtFor) {
				StmtFor LTLLoop = (StmtFor) ltlCurrentLine;
				Statement bodyLoop = LTLLoop.getBody();
				List<Statement> auxLTLs = new ArrayList<>();
				LTLFinite finite = new LTLFinite(auxLTLs);
				bodyLoop = (Statement) bodyLoop.accept(finite);
				Iterator<Statement> itAux = auxLTLs.iterator();
				List<Graph> automataAux = new ArrayList<>();
				while (itAux.hasNext()) {
					Statement ltlAux = itAux.next();
					// This pass transforms an LTL formula into a string format
					// that
					// LTL2BA recognizes.
					LTL2BAFormat stringFormat = new LTL2BAFormat(ltlAux);
					body.accept(stringFormat);
					// If the assert has an LTL formula as condition, the
					// following passes incorporates the RM instrumentation into
					// the current function's body.
					String ltlString = stringFormat.getLtlString();
					if (!ltlString.equals("")) {
						// Creation of the equivalent BA.
						Graph LTLFA = new Graph(ltlString, idST);
						idST++;
						// Creation of the RM associated with such a BA.
						Graph LTLFA2 = LTLFA.finiteExc();
						// Transformation of a string format to sketch code over
						// the transition labels.
						LTLFA2.castAdj(body, stringFormat.getPropNames());
						automataAux.add(LTLFA2);
					}
				}
				boolean hasLTL = false;
				// This pass parses the function body and incorporates
				// the RM instrumentation.
				LTLRMInstrumentations regressions = new LTLRMInstrumentations(hasLTL, automataAux, LTLLoop, declare);
				body = (Statement) body.accept(regressions);
				hasLTL = regressions.getHasLTL();

				if (hasLTL) {
					body = (Statement) body.accept(new LTLHaltingRet(automataAux, LTLLoop));
					body = (Statement) body.accept(new MakeBodiesBlocks());
					body = (Statement) body.accept(new LTLHalting(automataAux));
				}

			} else {
				// This pass transforms an LTL formula into a string format that
				// LTL2BA recognizes.
				LTL2BAFormat stringFormat = new LTL2BAFormat(ltlCurrentLine);
				body.accept(stringFormat);
				// If the assert has an LTL formula as condition, the following
				// passes incorporates the RM instrumentation into the current
				// function's body.
				String ltlString = stringFormat.getLtlString();
				if (!ltlString.equals("")) {
					// Creation of the equivalent BA.
					Graph LTLFA = new Graph(ltlString, idST);
					idST++;
					// Creation of the RM associated with such a BA.
					Graph LTLFA2 = LTLFA.finiteExc();
					// Transformation of a string format to sketch code over the
					// transition labels.
					LTLFA2.castAdj(body, stringFormat.getPropNames());
					boolean hasLTL = false;

					// This pass parses the function body and incorporates the
					// RM
					// instrumentation.
					LTLRMInstrumentations regressions = new LTLRMInstrumentations(hasLTL, LTLFA2, ltlCurrentLine,
							declare);
					body = (Statement) body.accept(regressions);
					hasLTL = regressions.getHasLTL();

					if (hasLTL) {
						body = (Statement) body.accept(new LTLHaltingRet(LTLFA2, ltlCurrentLine));
						body = (Statement) body.accept(new MakeBodiesBlocks());
						body = (Statement) body.accept(new LTLHalting(LTLFA2));
					}
				}
			}
		}
		symtab = oldSymTab;
		return func.creator().body((Statement) body).create();
	}
}
