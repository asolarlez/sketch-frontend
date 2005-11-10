package test;

import java.io.*;
import java.util.*;

import antlr.*;

import junit.framework.TestCase;
import streamit.frontend.*;
import streamit.frontend.nodes.*;
import streamit.frontend.passes.*;

public class ParserTest extends TestCase 
{
	public void testRev()
	{
		doTest("test/reverse.sk");
	}
	
	public void testParams()
	{
		Program p=doTest("test/debug.sk");
		StreamSpec ss=(StreamSpec) p.getStreams().get(0);
		assertNotNull(ss);
		for(Iterator i=ss.getFuncs().iterator();i.hasNext();) {
			Function f=(Function) i.next();
			boolean hasOutputPar=false;
			for(Iterator ip=f.getParams().iterator();ip.hasNext();) {
				Parameter par=(Parameter) ip.next();
				if(par.isParameterOutput())
					hasOutputPar=true;
			}
			assertTrue(hasOutputPar);
		}
	}
	
	public void testRetFnCall() //test a function call within a return
	{
		Program p=doTest("test/debug-lt.sk");
		StreamSpec ss=(StreamSpec) p.getStreams().get(0);
		assertNotNull(ss);
		for(Iterator i=ss.getFuncs().iterator();i.hasNext();) {
			Function f=(Function) i.next();
			for(Iterator st=((StmtBlock)f.getBody()).getStmts().iterator();st.hasNext();) {
				Object o=st.next();
				if (o instanceof StmtAssign) {
					StmtAssign stexpr = (StmtAssign) o;
					if(stexpr.getRHS() instanceof ExprFunCall) {
						ExprFunCall fcall=(ExprFunCall) stexpr.getRHS();
						assertFalse(fcall.getParams().size()<=1);
					}
				}
			}
		}
	}
	
	public void testToSbit()
	{
		//new ToSBit().run(new String[]{"--output","test/outfile","test/debug.sk"});
	}
	
	private Program doTest(String name)
	{
		InputStream str=null;
		try {
			str = new FileInputStream(name);
		} catch (FileNotFoundException e) {
			fail();
		}
		assertNotNull(str);
		StreamItParserFE parser=new StreamItParserFE(new StreamItLex(str));
		Program p=null;
		try {
			p = parser.program();
		} catch (RecognitionException e) {
			fail();
		} catch (TokenStreamException e) {
			fail();
		}
		assertNotNull(p);
		p=(Program) p.accept(new FunctionParamExtension());
		p=(Program) p.accept(new ConstantReplacer(Collections.EMPTY_MAP));
		return p;
	}
}
