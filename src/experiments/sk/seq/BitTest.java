package test;

import java.io.*;
import java.util.*;

import antlr.*;

import junit.framework.TestCase;
import streamit.frontend.*;
import streamit.frontend.nodes.*;
import streamit.frontend.passes.*;
import streamit.frontend.tojava.DoComplexProp;
import streamit.frontend.tosbit.*;
import streamit.frontend.tosbit.NodesToC;

public class BitTest extends TestCase 
{
	public void test1()
	{
		doTest("test/bittest1.sk");
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
		TempVarGen varGen = new TempVarGen();
		p=(Program) p.accept(new FunctionParamExtension());
		p=(Program) p.accept(new ConstantReplacer(Collections.EMPTY_MAP));
		p=(Program) p.accept(new BitTypeRemover(new TempVarGen()));
        //p=(Program) p.accept(new DoComplexProp(varGen));
		//p.accept(new ProduceBooleanFunctions(null, varGen, new ValueOracle()));
        String ccode = (String)p.accept(new NodesToC(false,varGen));
		System.out.println(ccode);
		return p;
	}
}
