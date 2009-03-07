package streamit.frontend.stencilSK;

import java.io.*;

import streamit.frontend.nodes.*;
import streamit.frontend.passes.CodePrinter;

public class SimpleCodePrinter extends CodePrinter
{
	boolean outtags = false;
	public SimpleCodePrinter outputTags(){

		outtags = true;
		return this;
	}

	public SimpleCodePrinter() {
		this(System.out);
	}

	public SimpleCodePrinter(OutputStream os) {
		super (os);
	}

	public Object visitFunction(Function func)
	{
		if(outtags && func.getTag() != null){ out.println("T="+func.getTag()); }
		printTab(); 
		out.println("/*" + func.getCx() + "*/");
		printTab();
		out.println(func.toString());
		super.visitFunction(func);
		out.flush();
		return func;
	}

	public Object visitStmtFor(StmtFor stmt)
	{
		if(outtags && stmt.getTag() != null){ out.println("T="+stmt.getTag()); }
		printLine("for("+stmt.getInit()+";"+stmt.getCond()+";"+stmt.getIncr()+")");
		printIndentedStatement(stmt.getBody());
		return stmt;
	}

	@Override
	public Object visitStmtIfThen(StmtIfThen stmt)
	{
		if(outtags && stmt.getTag() != null){ out.println("T="+stmt.getTag()); }
    	printLine("if(" + stmt.getCond() + ")/*" + stmt.getCx() + "*/");
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
		if(outtags && stmt.getTag() != null){ out.println("T="+stmt.getTag()); }
    	printLine("while(" + stmt.getCond() + ")");
    	printIndentedStatement(stmt.getBody());
		return stmt;
	}

	@Override
	public Object visitStmtDoWhile(StmtDoWhile stmt)
	{
		if(outtags && stmt.getTag() != null){ out.println("T="+stmt.getTag()); }
		printLine("do");
		printIndentedStatement(stmt.getBody());
    	printLine("while(" + stmt.getCond() + ")");
		return stmt;
	}

	@Override
	public Object visitStmtLoop(StmtLoop stmt)
	{
		if(outtags && stmt.getTag() != null){ out.println("T="+stmt.getTag()); }
    	printLine("loop(" + stmt.getIter() + ")");
    	printIndentedStatement(stmt.getBody());
		return stmt;
	}
	@Override
	public Object visitStmtFork(StmtFork stmt)
	{
		if(outtags && stmt.getTag() != null){ out.println("T="+stmt.getTag()); }
    	printLine("fork(" +  stmt.getLoopVarDecl() + "; "  + stmt.getIter() + ")");
    	printIndentedStatement(stmt.getBody());
		return stmt;
	}

	@Override
	public Object visitStmtBlock(StmtBlock stmt)
	{
		if(outtags && stmt.getTag() != null){ out.println("T="+stmt.getTag()); }
		printLine("{");
		indent++;
		super.visitStmtBlock(stmt);
		indent--;
		printLine("}");
		out.flush();
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
		if(outtags && stmt.getTag() != null){ out.println("T="+stmt.getTag()); }
		printLine(stmt.toString() + ";");
		return super.visitStmtAssert(stmt);
	}

	@Override
	public Object visitStmtAssign(StmtAssign stmt)
	{
		if(outtags && stmt.getTag() != null){ out.println("T="+stmt.getTag()); }
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
		if(outtags && stmt.getTag() != null){ out.println("T="+stmt.getTag()); }
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
		if(outtags && stmt.getTag() != null){ out.println("T="+stmt.getTag()); }
		printLine(stmt.toString() + ';');
		return super.visitStmtVarDecl(stmt);
	}

	@Override
	public Object visitFieldDecl(FieldDecl field)
	{
		printLine(field.toString());
		return super.visitFieldDecl(field);
	}

	public Object visitStmtReorderBlock(StmtReorderBlock block){
		printLine("reorder");
		block.getBlock().accept(this);
		return block;
	}

	public Object visitStmtInsertBlock (StmtInsertBlock sib) {
		printLine ("insert");
		sib.getInsertStmt ().accept (this);
		printLine ("into");
		sib.getIntoBlock ().accept (this);
		return sib;
	}

	public Object visitStmtAtomicBlock(StmtAtomicBlock block){
		if(outtags && block.getTag() != null){ out.println("T="+block.getTag()); }
		if(block.isCond()){
			printLine("atomic(" + block.getCond().accept(this) + ")");
		}else{
			printLine("atomic");
		}
		visitStmtBlock (block.getBlock());
		return block;
	}


}
