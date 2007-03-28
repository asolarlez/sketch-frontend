package streamit.frontend.stencilSK;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import streamit.frontend.nodes.ExprArray;
import streamit.frontend.nodes.ExprArrayInit;
import streamit.frontend.nodes.ExprArrayRange;
import streamit.frontend.nodes.ExprBinary;
import streamit.frontend.nodes.ExprConstInt;
import streamit.frontend.nodes.ExprField;
import streamit.frontend.nodes.ExprFunCall;
import streamit.frontend.nodes.ExprTernary;
import streamit.frontend.nodes.ExprUnary;
import streamit.frontend.nodes.ExprVar;
import streamit.frontend.nodes.Expression;
import streamit.frontend.nodes.FieldDecl;
import streamit.frontend.nodes.Function;
import streamit.frontend.nodes.Parameter;
import streamit.frontend.nodes.Program;
import streamit.frontend.nodes.Statement;
import streamit.frontend.nodes.StmtAssert;
import streamit.frontend.nodes.StmtAssign;
import streamit.frontend.nodes.StmtDoWhile;
import streamit.frontend.nodes.StmtExpr;
import streamit.frontend.nodes.StmtFor;
import streamit.frontend.nodes.StmtIfThen;
import streamit.frontend.nodes.StmtLoop;
import streamit.frontend.nodes.StmtVarDecl;
import streamit.frontend.nodes.StmtWhile;
import streamit.frontend.nodes.Type;
import streamit.frontend.nodes.TypeArray;
import streamit.frontend.nodes.TypePrimitive;
import streamit.frontend.nodes.TypeStruct;
import streamit.frontend.nodes.UnrecognizedVariableException;
import streamit.frontend.nodes.ExprArrayRange.RangeLen;
import streamit.frontend.passes.SemanticChecker;
import streamit.frontend.passes.SymbolTableVisitor;

public class StencilSemanticChecker extends SemanticChecker {

	public StencilSemanticChecker() {
		super();
		// TODO Auto-generated constructor stub
	}

	public static boolean check(Program prog)
	{    	
		StencilSemanticChecker checker = new StencilSemanticChecker();
		Map streamNames = checker.checkStreamNames(prog);
		checker.checkDupFieldNames(prog, streamNames);
		//       checker.checkStreamCreators(prog, streamNames);
		//checker.checkStreamTypes(prog);//I don't need this one in StreamBit
		//checker.checkFunctionValidity(prog);
		checker.checkStatementPlacement(prog);
		checker.checkVariableUsage(prog);
		checker.checkBasicTyping(prog);
		//checker.checkStreamConnectionTyping(prog);//I don't want this one in StreamBit
		checker.checkStatementCounts(prog);
		//       checker.checkIORates(prog);
		return checker.good;
	}

	// Need to do a little bit more control flow checks
	// also need to check that any array we loop over is of unspecified size (eg all of size N: N is int)

	public void checkControlStatements(Program prog)
	{
		prog.accept(new SymbolTableVisitor(null) {

			public Object visitStmtDoWhile(StmtDoWhile stmt)
			{
				// check the condition
				Type cond = getType(stmt.getCond());
				if (!cond.promotesTo(new TypePrimitive(TypePrimitive.TYPE_BIT)))
					report (stmt, "Condition clause is not a promotable to a bit");

				// should really also check whether any variables are modified in the loop body

				return super.visitStmtDoWhile(stmt);
			}

			public Object visitStmtFor(StmtFor stmt)
			{
				// check the condition
				Type cond = getType(stmt.getCond());
				if (!cond.promotesTo(new TypePrimitive(TypePrimitive.TYPE_BIT)))
					report (stmt, "Condition clause is not a proper conditional");

				// also need to check that the variable is incremented by 1
				StmtExpr incr = (StmtExpr)stmt.getIncr();
				ExprUnary incrExpr = (ExprUnary) incr.getExpression();	

				if (incrExpr.getOp() != ExprUnary.UNOP_POSTINC && 
						incrExpr.getOp() != ExprUnary.UNOP_POSTDEC)
					report (stmt, "Increment expression should be of the form (var){++,--}: " + incrExpr);

				// again, should check for variable usage

				return super.visitStmtFor(stmt);
			}

			public Object visitStmtIfThen(StmtIfThen stmt)
			{
				// check the condition
				Type cond = getType(stmt.getCond());
				if (!cond.promotesTo(new TypePrimitive(TypePrimitive.TYPE_BIT)))
					report (stmt, "Condition clause is not a proper conditional");

				return super.visitStmtIfThen(stmt);
			}

			public Object visitStmtLoop(StmtLoop stmt)
			{
				// variable in loop should promote to an int
				Type cond = getType(stmt.getIter());
				if (!cond.promotesTo(new TypePrimitive(TypePrimitive.TYPE_INT)))
					report (stmt, "Iteration count is not convertable to an integer");

				return super.visitStmtLoop(stmt);
			}

			public Object visitStmtWhile(StmtWhile stmt)
			{
				// check the condition
				Type cond = getType(stmt.getCond());
				if (!cond.promotesTo(new TypePrimitive(TypePrimitive.TYPE_BIT)))
					report (stmt, "Condition clause is not a proper conditional");

				return super.visitStmtWhile(stmt);
			}

		});
	}

}
