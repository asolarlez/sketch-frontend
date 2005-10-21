package test;

import java.io.*;

import antlr.*;

import junit.framework.TestCase;
import streamit.frontend.*;
import streamit.frontend.nodes.Program;

public class ParserTest extends TestCase 
{
	public void testIt()
	{
		try {
			InputStream str = new FileInputStream("test/reverse.sk");
			doTest(str);
		} catch (FileNotFoundException e) {
			fail();
		}
	}
	
	private void doTest(InputStream str)
	{
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
	}
}
