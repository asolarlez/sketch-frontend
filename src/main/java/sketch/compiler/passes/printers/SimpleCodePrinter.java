package sketch.compiler.passes.printers;

import java.io.OutputStream;
import java.util.Comparator;
import java.util.Iterator;
import java.util.TreeSet;

import sketch.compiler.ast.core.FieldDecl;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Function.LibraryFcnType;
import sketch.compiler.ast.core.Function.PrintFcnType;
import sketch.compiler.ast.core.StreamSpec;
import sketch.compiler.ast.core.exprs.ExprTprint;
import sketch.compiler.ast.core.stmts.*;
import sketch.compiler.ast.core.typs.TypeStruct;
import sketch.compiler.ast.core.typs.TypeStruct.StructFieldEnt;
import sketch.compiler.ast.cuda.stmts.CudaSyncthreads;
import sketch.compiler.ast.promela.stmts.StmtFork;
import sketch.compiler.ast.spmd.stmts.StmtSpmdfork;
import sketch.compiler.ast.spmd.stmts.SpmdBarrier;
import sketch.util.datastructures.TprintTuple;
import sketch.util.fcns.ZipIdxEnt;
import static sketch.util.fcns.ZipWithIndex.zipwithindex;

public class SimpleCodePrinter extends CodePrinter
{
	boolean outtags = false;
    protected final boolean printLibraryFunctions;
	public SimpleCodePrinter outputTags(){

		outtags = true;
		return this;
	}

	public SimpleCodePrinter() {
		this(System.out, false);
	}

	public SimpleCodePrinter(OutputStream os, boolean printLibraryFunctions) {
		super (os);
        this.printLibraryFunctions = printLibraryFunctions;
	}

	public Object visitFunction(Function func)
	{
		if(outtags && func.getTag() != null){ out.println("T="+func.getTag()); }
		printTab(); 
		out.println("/*" + func.getCx() + "*/");
		printTab();
		out.println((func.getInfo().printType == PrintFcnType.Printfcn ? "printfcn " : "") + func.toString());
		super.visitFunction(func);
		out.flush();
		return func;
	}
	
	
    public Object visitStreamSpec(StreamSpec spec)
    {
        // Oof, there's a lot here.  At least half of it doesn't get
        // visited...

        nres.setPackage(spec);
        printLine("/* BEGIN PACKAGE " + spec.getName() + "*/");

        for (TypeStruct tsOrig : spec.getStructs()) {
            TypeStruct ts = (TypeStruct) tsOrig.accept(this);
        }

        for (Iterator iter = spec.getVars().iterator(); iter.hasNext(); )
        {
            FieldDecl oldVar = (FieldDecl)iter.next();
            FieldDecl newVar = (FieldDecl)oldVar.accept(this);

        }
        int nonNull = 0;
        
        TreeSet<Function> orderedFuncs = new TreeSet<Function>(new Comparator<Function>()
        {
            public int compare(Function o1, Function o2) {
                final int det_order =
                        o1.getInfo().determinsitic.compareTo(o2.getInfo().determinsitic);
                return det_order + (det_order == 0 ? 1 : 0) *
                        o1.getName().compareTo(o2.getName());
            }
        });
        orderedFuncs.addAll(spec.getFuncs());

        for (Function oldFunc : orderedFuncs) {
            if (oldFunc.getInfo().libraryType != LibraryFcnType.Library || printLibraryFunctions) {
                Function newFunc = (Function) oldFunc.accept(this);
            }
        }
        printLine("/* END PACKAGE " + spec.getName() + "*/");
        return spec;
    }
	
	

	public Object visitStmtFor(StmtFor stmt)
	{
		if(outtags && stmt.getTag() != null){ out.println("T="+stmt.getTag()); }
		printLine("for("+stmt.getInit()+"; "+stmt.getCond()+"; "+stmt.getIncr()+")");
		printIndentedStatement(stmt.getBody());
		return stmt;
	}

	public Object visitStmtSpmdfork(StmtSpmdfork stmt)
	{
		if(outtags && stmt.getTag() != null){ out.println("T="+stmt.getTag()); }
		printLine("spmdfork("+stmt.getNProc() + ")");
		printIndentedStatement(stmt.getBody());
		return stmt;
	}

        public Object visitSpmdBarrier(SpmdBarrier stmt)
        {
                printLine("spmdbarrier();");
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
	public Object visitStmtExpr(StmtExpr stmt)
	{
		if(outtags && stmt.getTag() != null){ out.println("T="+stmt.getTag()); }
        if (!(stmt.getExpression() instanceof ExprTprint)) {
            printLine(stmt.toString() + ";");
        }
		return super.visitStmtExpr(stmt);
	}

    public Object visitStmtFunDef(StmtFunDecl stmt) {
        printLine(stmt.toString());
        return stmt;
    }

	@Override
	public Object visitStmtReturn(StmtReturn stmt)
	{
		printLine(stmt.toString());
		return super.visitStmtReturn(stmt);
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

	@Override
	public Object visitTypeStruct(TypeStruct ts) {
	    printLine("struct " + ts.getName() + " {");
	    for (StructFieldEnt ent : ts.getFieldEntries()) {
	        printLine("    " + ent.getType().toString() + " " + ent.getName() + ";");
	    }
	    printLine("}");
	    return ts;
	}

    @Override
    public Object visitStmtMinLoop(StmtMinLoop stmtMinLoop) {
        printTab();
        print("minloop");
        printIndentedStatement(stmtMinLoop.getBody());
        return stmtMinLoop;
    }

    @Override
    public Object visitStmtMinimize(StmtMinimize stmtMinimize) {
        printLine("minimize(" + stmtMinimize.getMinimizeExpr().accept(this) + ")");
        return stmtMinimize;
    }

    @Override
    public Object visitExprTprint(ExprTprint exprTprint) {
        if (!exprTprint.expressions.isEmpty()) {
            for (ZipIdxEnt<TprintTuple> v : zipwithindex(exprTprint.expressions)) {
                String line = "\"" + v.entry.getFirst() + ": \" << " + v.entry.getSecond() + " << endl";
                if (v.isLast) {
                    line += ";";
                }
                if (v.idx == 0) {
                    printLine("cout << " + line);
                    this.indent += 1;
                } else {
                    printLine("<< " + line);
                }
            }
            this.indent -= 1;
        }
        return exprTprint;
    }
    
    @Override
    public Object visitCudaSyncthreads(CudaSyncthreads cudaSyncthreads) {
        printLine("__syncthreads();");
        return cudaSyncthreads;
    }
}
