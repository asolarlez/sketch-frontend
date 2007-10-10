/**
 *
 */
package streamit.frontend.tospin;

import java.io.OutputStream;

import streamit.frontend.nodes.FENode;
import streamit.frontend.nodes.FieldDecl;
import streamit.frontend.nodes.Function;
import streamit.frontend.nodes.StmtAdd;
import streamit.frontend.nodes.StmtAnyOrderBlock;
import streamit.frontend.nodes.StmtAssert;
import streamit.frontend.nodes.StmtAssign;
import streamit.frontend.nodes.StmtAtomicBlock;
import streamit.frontend.nodes.StmtBlock;
import streamit.frontend.nodes.StmtBody;
import streamit.frontend.nodes.StmtBreak;
import streamit.frontend.nodes.StmtContinue;
import streamit.frontend.nodes.StmtDoWhile;
import streamit.frontend.nodes.StmtEmpty;
import streamit.frontend.nodes.StmtEnqueue;
import streamit.frontend.nodes.StmtExpr;
import streamit.frontend.nodes.StmtFor;
import streamit.frontend.nodes.StmtIfThen;
import streamit.frontend.nodes.StmtJoin;
import streamit.frontend.nodes.StmtLoop;
import streamit.frontend.nodes.StmtPloop;
import streamit.frontend.nodes.StmtPush;
import streamit.frontend.nodes.StmtReturn;
import streamit.frontend.nodes.StmtSendMessage;
import streamit.frontend.nodes.StmtSplit;
import streamit.frontend.nodes.StmtVarDecl;
import streamit.frontend.nodes.StmtWhile;
import streamit.frontend.passes.CodePrinter;

/**
 * @author Chris Jones
 */
public class PromelaCodePrinter extends CodePrinter {
	public PromelaCodePrinter() {
		this (System.out);
	}

	public PromelaCodePrinter (OutputStream os) {
		super (os);
	}

	public Object visitFunction(Function func)
	{
		printTab(); out.println(func.toString());
		super.visitFunction(func);
		out.flush();
		return func;
	}

	public Object visitStmtFor(StmtFor stmt)
	{
		printLine("for("+stmt.getInit()+";"+stmt.getCond()+";"+stmt.getIncr()+")");
		printIndentedStatement(stmt.getBody());
		return stmt;
	}

	@Override
	public Object visitStmtIfThen(StmtIfThen stmt)
	{
    	printLine("if(" + stmt.getCond() + ")");
    	printIndentedStatement(stmt.getCons());
    	if(stmt.getAlt() != null){
    		printLine("else");
    		printIndentedStatement(stmt.getAlt());
    	}
		return stmt;
	}

	@Override
	public Object visitStmtWhile(StmtWhile stmt)
	{
    	printLine("while(" + stmt.getCond() + ")");
    	printIndentedStatement(stmt.getBody());
		return stmt;
	}

	@Override
	public Object visitStmtDoWhile(StmtDoWhile stmt)
	{
		printLine("do");
		printIndentedStatement(stmt.getBody());
    	printLine("while(" + stmt.getCond() + ")");
		return stmt;
	}

	@Override
	public Object visitStmtLoop(StmtLoop stmt)
	{
    	printLine("loop(" + stmt.getIter() + ")");
    	printIndentedStatement(stmt.getBody());
		return stmt;
	}
	@Override
	public Object visitStmtPloop(StmtPloop stmt)
	{
    	printLine("ploop(" +  stmt.getLoopVarDecl() + "; "  + stmt.getIter() + ")");
    	printIndentedStatement(stmt.getBody());
		return stmt;
	}

	@Override
	public Object visitStmtBlock(StmtBlock stmt)
	{
		printLine("{");
		indent++;
		super.visitStmtBlock(stmt);
		indent--;
		printLine("}");
		return stmt;
	}

	@Override
	public Object visitStmtAdd(StmtAdd stmt)
	{
		assertEliminated (stmt);
		return null;
	}

	@Override
	public Object visitStmtAssert(StmtAssert stmt)
	{
		printLine(stmt.toString());
		return super.visitStmtAssert(stmt);
	}

	@Override
	public Object visitStmtAssign(StmtAssign stmt)
	{
		printLine(stmt.toString()  + ';');
		return super.visitStmtAssign(stmt);
	}

	@Override
	public Object visitStmtBody(StmtBody stmt)
	{
		printLine(stmt.toString());
		return super.visitStmtBody(stmt);
	}

	@Override
	public Object visitStmtBreak(StmtBreak stmt)
	{
		printLine(stmt.toString());
		return super.visitStmtBreak(stmt);
	}

	@Override
	public Object visitStmtContinue(StmtContinue stmt)
	{
		printLine(stmt.toString());
		return super.visitStmtContinue(stmt);
	}

	@Override
	public Object visitStmtEmpty(StmtEmpty stmt)
	{
		printLine(stmt.toString());
		return super.visitStmtEmpty(stmt);
	}

	@Override
	public Object visitStmtEnqueue(StmtEnqueue stmt)
	{
		assertEliminated (stmt);
		return null;
	}

	@Override
	public Object visitStmtExpr(StmtExpr stmt)
	{
		printLine(stmt.toString());
		return super.visitStmtExpr(stmt);
	}

	@Override
	public Object visitStmtJoin(StmtJoin stmt)
	{
		assertEliminated (stmt);
		return null;
	}

	@Override
	public Object visitStmtPush(StmtPush stmt)
	{
		assertEliminated (stmt);
		return null;
	}

	@Override
	public Object visitStmtReturn(StmtReturn stmt)
	{
		assertEliminated (stmt);
		return null;
	}

	@Override
	public Object visitStmtSendMessage(StmtSendMessage stmt)
	{
		assertEliminated (stmt);
		return null;
	}

	@Override
	public Object visitStmtSplit(StmtSplit stmt)
	{
		assertEliminated (stmt);
		return null;
	}

	@Override
	public Object visitStmtVarDecl(StmtVarDecl stmt)
	{
		printLine(stmt.toString() + ';');
		return super.visitStmtVarDecl(stmt);
	}

	@Override
	public Object visitFieldDecl(FieldDecl field)
	{
		printLine(field.toString());
		return super.visitFieldDecl(field);
	}

	public Object visitStmtAnyOrderBlock(StmtAnyOrderBlock block){
		printLine("anyorder");
		block.getBlock().accept(this);
		return block;
	}

	public Object visitStmtAtomicBlock(StmtAtomicBlock block){
		printLine("atomic");
		visitStmtBlock (block);
		return block;
	}


	protected void assertEliminated (FENode node) {
		node.report ("internal error; I should have been eliminated.");
		throw new RuntimeException ();
	}
}
