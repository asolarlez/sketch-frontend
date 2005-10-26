package test;

import java.io.*;
import java.util.Iterator;

import antlr.*;

import junit.framework.TestCase;
import streamit.frontend.*;
import streamit.frontend.nodes.*;
import streamit.frontend.passes.FunctionParamExtension;

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
		return p;
	}
}
