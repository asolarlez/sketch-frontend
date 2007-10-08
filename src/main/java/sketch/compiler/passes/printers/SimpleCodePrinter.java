package streamit.frontend.stencilSK;

import java.io.*;

import streamit.frontend.nodes.*;

public class SimpleCodePrinter extends FEReplacer
{
	private static final int tabWidth=2;
	private final PrintWriter out;
	private int indent=0;

	public SimpleCodePrinter() {
		this(System.out);
	}

	public SimpleCodePrinter(OutputStream os) {
		out=new PrintWriter(os);
	}

	private String pad="";
	private void printTab() {
		if(indent*tabWidth!=pad.length()) {
			StringBuffer b=new StringBuffer();
			for(int i=0;i<indent*tabWidth;i++)
				b.append(' ');
			pad=b.toString();
		}
		out.print(pad);
	}
	private void printLine(String s) {
		printTab();
		out.println(s);
	}

	private void printIndentedStatement(Statement s) {
		if(s==null) return;
		if(s instanceof StmtBlock)
			s.accept(this);
		else {
			indent++;
			s.accept(this);
			indent--;
		}
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
		printLine(stmt.toString());
		return super.visitStmtAdd(stmt);
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
		printLine(stmt.toString());
		return super.visitStmtEnqueue(stmt);
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
		printLine(stmt.toString());
		return super.visitStmtJoin(stmt);
	}

	@Override
	public Object visitStmtPush(StmtPush stmt)
	{
		printLine(stmt.toString());
		return super.visitStmtPush(stmt);
	}

	@Override
	public Object visitStmtReturn(StmtReturn stmt)
	{
		printLine(stmt.toString());
		return super.visitStmtReturn(stmt);
	}

	@Override
	public Object visitStmtSendMessage(StmtSendMessage stmt)
	{
		printLine(stmt.toString());
		return super.visitStmtSendMessage(stmt);
	}

	@Override
	public Object visitStmtSplit(StmtSplit stmt)
	{
		printLine(stmt.toString());
		return super.visitStmtSplit(stmt);
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


}
