// $ANTLR 2.7.7 (2006-11-01): "StreamItParserFE.g" -> "StreamItParserFE.java"$

package sketch.compiler.parser;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.Map;
import java.util.HashMap;

import sketch.compiler.Directive;
import sketch.compiler.ast.core.FEContext;
import sketch.compiler.ast.core.FieldDecl;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.Annotation;
import sketch.util.datastructures.HashmapList;

import sketch.compiler.ast.core.Package;


import sketch.compiler.ast.core.exprs.*;
import sketch.compiler.ast.core.exprs.regens.*;
import sketch.compiler.ast.core.stmts.*;
import sketch.compiler.ast.core.typs.*;
import sketch.compiler.ast.cuda.exprs.*;
import sketch.compiler.ast.cuda.stmts.*;
import sketch.compiler.ast.cuda.typs.*;

import sketch.compiler.ast.promela.stmts.StmtFork;
import sketch.compiler.main.cmdline.SketchOptions;

import sketch.compiler.ast.spmd.stmts.StmtSpmdfork;

import static sketch.util.DebugOut.assertFalse;

import antlr.TokenBuffer;
import antlr.TokenStreamException;
import antlr.TokenStreamIOException;
import antlr.ANTLRException;
import antlr.LLkParser;
import antlr.Token;
import antlr.TokenStream;
import antlr.RecognitionException;
import antlr.NoViableAltException;
import antlr.MismatchedTokenException;
import antlr.SemanticException;
import antlr.ParserSharedInputState;
import antlr.collections.impl.BitSet;
@SuppressWarnings("deprecation")
public class StreamItParserFE extends antlr.LLkParser       implements StreamItParserFETokenTypes
 {

	private Set<String> processedIncludes=new HashSet<String> ();
    private Set<Directive> directives = new HashSet<Directive> ();
    private boolean preprocess;
    private List<String> cppDefs;

	public StreamItParserFE(StreamItLex lexer, Set<String> includes,
                            boolean preprocess, List<String> cppDefs)
	{
		this(lexer);
		processedIncludes = includes;
        this.preprocess = preprocess;
        this.cppDefs = cppDefs;
	}

	public static void main(String[] args)
	{
		try
		{
			DataInputStream dis = new DataInputStream(System.in);
			StreamItLex lexer = new StreamItLex(dis);
			StreamItParserFE parser = new StreamItParserFE(lexer);
			parser.program();
		}
		catch (Exception e)
		{
			e.printStackTrace(System.err);
		}
	}

	public FEContext getContext(Token t)
	{
		int line = t.getLine();
		if (line == 0) line = -1;
		int col = t.getColumn();
		if (col == 0) col = -1;
		return new FEContext(getFilename(), line, col);
	}

	private boolean hasError = false;

	public void reportError(RecognitionException ex)
	{
		hasError = true;
		super.reportError(ex);
	}

	public void reportError(String s)
	{
		hasError = true;
		super.reportError(s);
	}

    public void handleInclude(String name, List<Package> namespace)
    {
        try {
            List<String> incList = Arrays.asList(
                    SketchOptions.getSingleton().feOpts.inc);
        	Iterator<String> lit = null;
        	if(incList != null){ lit = incList.iterator(); }
        	File f = new File (name);
        	String errMsg = "";
        	while(!f.canRead()){
        		if(lit != null && lit.hasNext()){
        			String tmp = lit.next(); 
        			errMsg += "\n\t" +  f.getCanonicalPath();
        			f = new File (tmp, name);	
        		}else{
        			errMsg += "\n\t" + f.getCanonicalPath();
        			throw new IllegalArgumentException ("File not found: "+ name + "\n" + 
        					"Searched the following paths:" + errMsg + "\n");
        		}
        	}
            name = f.getCanonicalPath ();
        } catch (IOException ioe) {
            throw new IllegalArgumentException ("File not found: "+ name);
        }
        if (processedIncludes.contains(name))
            return;
        processedIncludes.add(name);
        StreamItParser parser =
            new StreamItParser (name, processedIncludes, preprocess, cppDefs);
        Program p = parser.parse ();
		assert p != null;		
		
		namespace.addAll(p.getPackages());		
        directives.addAll (parser.getDirectives ());
    }

    private void handlePragma (String pragma, String args) {
        directives.add (Directive.make (pragma, args));
    }

    public Set<Directive> getDirectives () {  return directives;  }


protected StreamItParserFE(TokenBuffer tokenBuf, int k) {
  super(tokenBuf,k);
  tokenNames = _tokenNames;
}

public StreamItParserFE(TokenBuffer tokenBuf) {
  this(tokenBuf,2);
}

protected StreamItParserFE(TokenStream lexer, int k) {
  super(lexer,k);
  tokenNames = _tokenNames;
}

public StreamItParserFE(TokenStream lexer) {
  this(lexer,2);
}

public StreamItParserFE(ParserSharedInputState state) {
  super(state,2);
  tokenNames = _tokenNames;
}

	public final Program  program() throws RecognitionException, TokenStreamException {
		Program p;
		
		Token  id = null;
		p = null; List vars = new ArrayList();  
			Expression assumption = null;
			List<Function> funcs=new ArrayList(); Function f;
			List<Package> namespaces = new ArrayList<Package>();
		FieldDecl fd; TypeStruct ts; List<TypeStruct> structs = new ArrayList<TypeStruct>();
		String file = null;
		String pkgName = null;
		FEContext pkgCtxt = null;
		
		
		try {      // for error handling
			{
			_loop9:
			do {
				switch ( LA(1)) {
				case TK_struct:
				{
					ts=struct_decl();
					if ( inputState.guessing==0 ) {
						structs.add(ts);
					}
					break;
				}
				case TK_include:
				{
					file=include_stmt();
					if ( inputState.guessing==0 ) {
						handleInclude (file, namespaces);
					}
					break;
				}
				case TK_package:
				{
					match(TK_package);
					id = LT(1);
					match(ID);
					match(SEMI);
					if ( inputState.guessing==0 ) {
						pkgCtxt = getContext(id); pkgName = (id.getText());
					}
					break;
				}
				case TK_pragma:
				{
					pragma_stmt();
					break;
				}
				case TK_assume:
				{
					match(TK_assume);
					assumption=right_expr();
					match(SEMI);
					if ( inputState.guessing==0 ) {
						if (assumption != null) { }
					}
					break;
				}
				default:
					boolean synPredMatched6 = false;
					if (((_tokenSet_0.member(LA(1))) && (_tokenSet_1.member(LA(2))))) {
						int _m6 = mark();
						synPredMatched6 = true;
						inputState.guessing++;
						try {
							{
							annotation_list();
							{
							_loop5:
							do {
								switch ( LA(1)) {
								case TK_serial:
								{
									match(TK_serial);
									break;
								}
								case TK_harness:
								{
									match(TK_harness);
									break;
								}
								case TK_generator:
								{
									match(TK_generator);
									break;
								}
								case TK_stencil:
								{
									match(TK_stencil);
									break;
								}
								case TK_model:
								{
									match(TK_model);
									break;
								}
								default:
								{
									break _loop5;
								}
								}
							} while (true);
							}
							return_type();
							match(ID);
							match(LPAREN);
							}
						}
						catch (RecognitionException pe) {
							synPredMatched6 = false;
						}
						rewind(_m6);
inputState.guessing--;
					}
					if ( synPredMatched6 ) {
						f=function_decl();
						if ( inputState.guessing==0 ) {
							funcs.add(f);
						}
					}
					else {
						boolean synPredMatched8 = false;
						if (((_tokenSet_0.member(LA(1))) && (_tokenSet_1.member(LA(2))))) {
							int _m8 = mark();
							synPredMatched8 = true;
							inputState.guessing++;
							try {
								{
								return_type();
								match(ID);
								match(LPAREN);
								}
							}
							catch (RecognitionException pe) {
								synPredMatched8 = false;
							}
							rewind(_m8);
inputState.guessing--;
						}
						if ( synPredMatched8 ) {
							f=function_decl();
							if ( inputState.guessing==0 ) {
								funcs.add(f);
							}
						}
						else if ((_tokenSet_2.member(LA(1))) && (_tokenSet_3.member(LA(2)))) {
							fd=field_decl();
							match(SEMI);
							if ( inputState.guessing==0 ) {
								vars.add(fd);
							}
						}
					else {
						break _loop9;
					}
					}}
				} while (true);
				}
				match(Token.EOF_TYPE);
				if ( inputState.guessing==0 ) {
					
								if(pkgName == null){
									pkgName="ANONYMOUS";
								}
								for(TypeStruct struct : structs){
									struct.setPkg(pkgName);	
								}
								for(Function fun : funcs){
									fun.setPkg(pkgName);	
								}
								 Package ss=new Package(pkgCtxt, 
									pkgName,
									structs, vars, funcs);
									namespaces.add(ss);
					if (!hasError) {
					if (p == null) {
					p = Program.emptyProgram();
					}
					p =
					p.creator().streams(namespaces).create();
					}
					
				}
			}
			catch (RecognitionException ex) {
				if (inputState.guessing==0) {
					reportError(ex);
					recover(ex,_tokenSet_4);
				} else {
				  throw ex;
				}
			}
			return p;
		}
		
	public final HashmapList<String, Annotation>   annotation_list() throws RecognitionException, TokenStreamException {
		HashmapList<String, Annotation>  amap;
		
		
			 amap=null; Annotation an;
		
		try {      // for error handling
			{
			_loop64:
			do {
				if ((LA(1)==AT)) {
					an=annotation();
					if ( inputState.guessing==0 ) {
						if(amap==null){amap = new HashmapList<String, Annotation>();} amap.append(an.tag, an);
					}
				}
				else {
					break _loop64;
				}
				
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_5);
			} else {
			  throw ex;
			}
		}
		return amap;
	}
	
	public final Type  return_type() throws RecognitionException, TokenStreamException {
		Type t;
		
		t=null;
		
		try {      // for error handling
			t=data_type();
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_6);
			} else {
			  throw ex;
			}
		}
		return t;
	}
	
	public final Function  function_decl() throws RecognitionException, TokenStreamException {
		Function f;
		
		Token  id = null;
		Token  impl = null;
		
		Type rt;
		List l;
		StmtBlock s;
		HashmapList<String, Annotation> amap;
		f = null;
		boolean isHarness = false;
		boolean isLibrary = false;
		boolean isPrintfcn = false;
		boolean isGenerator = false;
		boolean isDevice = false;
		boolean isGlobal = false;
		boolean isSerial = false;
		boolean isStencil = false;
		boolean isModel = false;
		
		
		try {      // for error handling
			amap=annotation_list();
			{
			_loop67:
			do {
				switch ( LA(1)) {
				case TK_serial:
				{
					match(TK_serial);
					if ( inputState.guessing==0 ) {
						isSerial = true;
					}
					break;
				}
				case TK_harness:
				{
					match(TK_harness);
					if ( inputState.guessing==0 ) {
						isHarness = true;
					}
					break;
				}
				case TK_generator:
				{
					match(TK_generator);
					if ( inputState.guessing==0 ) {
						isGenerator = true;
					}
					break;
				}
				case TK_stencil:
				{
					match(TK_stencil);
					if ( inputState.guessing==0 ) {
						isStencil = true;
					}
					break;
				}
				case TK_model:
				{
					match(TK_model);
					if ( inputState.guessing==0 ) {
						isModel = true;
					}
					break;
				}
				default:
				{
					break _loop67;
				}
				}
			} while (true);
			}
			rt=return_type();
			id = LT(1);
			match(ID);
			l=param_decl_list();
			{
			switch ( LA(1)) {
			case TK_implements:
			{
				match(TK_implements);
				impl = LT(1);
				match(ID);
				break;
			}
			case LCURLY:
			case SEMI:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			{
			switch ( LA(1)) {
			case LCURLY:
			{
				s=block();
				if ( inputState.guessing==0 ) {
					
					assert !(isGenerator && isHarness) : "The generator and harness keywords cannot be used together";
					Function.FunctionCreator fc = Function.creator(getContext(id), id.getText(), Function.FcnType.Static).returnType(
					rt).params(l).body(s).annotations(amap);
					
					// function type
					if (isGenerator) {
					fc = fc.type(Function.FcnType.Generator);
					} else if (isHarness) {
					assert impl == null : "harness functions cannot have implements";
					fc = fc.type(Function.FcnType.Harness);
					} else if (isModel) {
					assert impl == null : "A model can not implement another model or a concrete function";
					fc = fc.type(Function.FcnType.Model);
					} else if (impl != null) {
					fc = fc.spec(impl.getText());
					}
					
					// library type
					if (isLibrary) {
					fc = fc.libraryType(Function.LibraryFcnType.Library);
					}
					
					// print function
					if (isPrintfcn) {
					fc = fc.printType(Function.PrintFcnType.Printfcn);
					}
					
					// cuda type annotations
					if ((isDevice && isGlobal) || (isGlobal && isSerial) || (isDevice && isSerial)) {
					assertFalse("Only one of \"global\", \"device\", or \"serial\" qualifiers is allowed.");
					}
					if (isDevice) {
					fc = fc.cudaType(Function.CudaFcnType.DeviceInline);
					} else if (isGlobal) {
					fc = fc.cudaType(Function.CudaFcnType.Global);
					} else if (isSerial) {
					fc = fc.cudaType(Function.CudaFcnType.Serial);
					}
					
					// stencil type
					if (isStencil) {
					fc = fc.solveType(Function.FcnSolveType.Stencil);
					}
					
					f = fc.create();
						
				}
				break;
			}
			case SEMI:
			{
				match(SEMI);
				if ( inputState.guessing==0 ) {
					f = Function.creator(getContext(id), id.getText(), Function.FcnType.Uninterp).returnType(rt).params(l).annotations(amap).create();
				}
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_7);
			} else {
			  throw ex;
			}
		}
		return f;
	}
	
	public final FieldDecl  field_decl() throws RecognitionException, TokenStreamException {
		FieldDecl f;
		
		Token  id = null;
		Token  id2 = null;
		f = null; Type t; Expression x = null;
			List ts = new ArrayList(); List ns = new ArrayList();
			List xs = new ArrayList(); FEContext ctx = null;
		
		try {      // for error handling
			t=data_type();
			id = LT(1);
			match(ID);
			{
			switch ( LA(1)) {
			case ASSIGN:
			{
				match(ASSIGN);
				x=var_initializer();
				break;
			}
			case SEMI:
			case COMMA:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			if ( inputState.guessing==0 ) {
				ctx = getContext(id); ts.add(t); ns.add(id.getText()); xs.add(x);
			}
			{
			_loop17:
			do {
				if ((LA(1)==COMMA)) {
					if ( inputState.guessing==0 ) {
						x = null;
					}
					match(COMMA);
					id2 = LT(1);
					match(ID);
					{
					switch ( LA(1)) {
					case ASSIGN:
					{
						match(ASSIGN);
						x=var_initializer();
						break;
					}
					case SEMI:
					case COMMA:
					{
						break;
					}
					default:
					{
						throw new NoViableAltException(LT(1), getFilename());
					}
					}
					}
					if ( inputState.guessing==0 ) {
						ts.add(t); ns.add(id2.getText()); xs.add(x);
					}
				}
				else {
					break _loop17;
				}
				
			} while (true);
			}
			if ( inputState.guessing==0 ) {
				f = new FieldDecl(ctx, ts, ns, xs);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_8);
			} else {
			  throw ex;
			}
		}
		return f;
	}
	
	public final TypeStruct  struct_decl() throws RecognitionException, TokenStreamException {
		TypeStruct ts;
		
		Token  t = null;
		Token  id = null;
		ts = null; Parameter p; List names = new ArrayList();
			Annotation an=null;
			HashmapList<String, Annotation> annotations = new HashmapList<String, Annotation>();
			List types = new ArrayList();
		
		try {      // for error handling
			t = LT(1);
			match(TK_struct);
			id = LT(1);
			match(ID);
			match(LCURLY);
			{
			_loop235:
			do {
				switch ( LA(1)) {
				case TK_boolean:
				case TK_float:
				case TK_bit:
				case TK_int:
				case TK_void:
				case TK_double:
				case TK_fun:
				case TK_char:
				case TK_ref:
				case TK_global:
				case ID:
				{
					p=param_decl();
					match(SEMI);
					if ( inputState.guessing==0 ) {
						names.add(p.getName()); types.add(p.getType());
					}
					break;
				}
				case AT:
				{
					an=annotation();
					if ( inputState.guessing==0 ) {
						annotations.append(an.tag, an);
					}
					break;
				}
				default:
				{
					break _loop235;
				}
				}
			} while (true);
			}
			match(RCURLY);
			if ( inputState.guessing==0 ) {
				ts = TypeStruct.creator(getContext(t), id.getText(), names, types, annotations).create();
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_9);
			} else {
			  throw ex;
			}
		}
		return ts;
	}
	
	public final String  include_stmt() throws RecognitionException, TokenStreamException {
		String f;
		
		Token  fn = null;
		f = null;
		
		try {      // for error handling
			match(TK_include);
			fn = LT(1);
			match(STRING_LITERAL);
			match(SEMI);
			if ( inputState.guessing==0 ) {
				f = fn.getText ();  f = f.substring (1, f.length () - 1);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_9);
			} else {
			  throw ex;
			}
		}
		return f;
	}
	
	public final void pragma_stmt() throws RecognitionException, TokenStreamException {
		
		Token  p = null;
		Token  a = null;
		String args = "";
		
		try {      // for error handling
			match(TK_pragma);
			p = LT(1);
			match(ID);
			{
			switch ( LA(1)) {
			case STRING_LITERAL:
			{
				a = LT(1);
				match(STRING_LITERAL);
				if ( inputState.guessing==0 ) {
					args = a.getText ().substring (1, a.getText ().length ()-1);
				}
				break;
			}
			case SEMI:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			match(SEMI);
			if ( inputState.guessing==0 ) {
				handlePragma (p.getText (), args);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_9);
			} else {
			  throw ex;
			}
		}
	}
	
	public final Expression  right_expr() throws RecognitionException, TokenStreamException {
		Expression x;
		
		x = null;
		
		try {      // for error handling
			switch ( LA(1)) {
			case TK_new:
			case TK_null:
			case TK_true:
			case TK_false:
			case LPAREN:
			case LCURLY:
			case INCREMENT:
			case MINUS:
			case DECREMENT:
			case BANG:
			case NDVAL:
			case NDVAL2:
			case REGEN:
			case CHAR_LITERAL:
			case STRING_LITERAL:
			case HQUAN:
			case NUMBER:
			case ID:
			{
				x=right_expr_not_agmax();
				break;
			}
			case NDANGELIC:
			{
				x=agmax_expr();
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_10);
			} else {
			  throw ex;
			}
		}
		return x;
	}
	
	public final Type  data_type() throws RecognitionException, TokenStreamException {
		Type t;
		
		Token  prefix = null;
		Token  id = null;
		Token  l = null;
		Token  n = null;
		Token  num = null;
		t = null; Vector<Expression> params = new Vector<Expression>(); Vector<Integer> maxlens = new Vector<Integer>(); int maxlen = 0; Expression x; boolean isglobal = false;
		
		try {      // for error handling
			switch ( LA(1)) {
			case TK_boolean:
			case TK_float:
			case TK_bit:
			case TK_int:
			case TK_double:
			case TK_fun:
			case TK_char:
			case TK_global:
			case ID:
			{
				{
				switch ( LA(1)) {
				case TK_global:
				{
					match(TK_global);
					if ( inputState.guessing==0 ) {
						isglobal = true;
					}
					break;
				}
				case TK_boolean:
				case TK_float:
				case TK_bit:
				case TK_int:
				case TK_double:
				case TK_fun:
				case TK_char:
				case ID:
				{
					break;
				}
				default:
				{
					throw new NoViableAltException(LT(1), getFilename());
				}
				}
				}
				{
				switch ( LA(1)) {
				case TK_boolean:
				case TK_float:
				case TK_bit:
				case TK_int:
				case TK_double:
				case TK_fun:
				case TK_char:
				{
					t=primitive_type();
					break;
				}
				case ID:
				{
					{
					if ((LA(1)==ID) && (LA(2)==AT)) {
						prefix = LT(1);
						match(ID);
						match(AT);
					}
					else if ((LA(1)==ID) && (LA(2)==LPAREN||LA(2)==LSQUARE||LA(2)==ID)) {
					}
					else {
						throw new NoViableAltException(LT(1), getFilename());
					}
					
					}
					id = LT(1);
					match(ID);
					if ( inputState.guessing==0 ) {
						t = new TypeStructRef(prefix != null ? (prefix.getText() + "@" + id.getText() )  : id.getText());
					}
					break;
				}
				default:
				{
					throw new NoViableAltException(LT(1), getFilename());
				}
				}
				}
				{
				_loop52:
				do {
					if ((LA(1)==LSQUARE)) {
						l = LT(1);
						match(LSQUARE);
						{
						{
						switch ( LA(1)) {
						case TK_new:
						case TK_null:
						case TK_true:
						case TK_false:
						case LPAREN:
						case LCURLY:
						case INCREMENT:
						case MINUS:
						case DECREMENT:
						case BANG:
						case NDVAL:
						case NDVAL2:
						case NDANGELIC:
						case REGEN:
						case CHAR_LITERAL:
						case STRING_LITERAL:
						case HQUAN:
						case NUMBER:
						case ID:
						{
							if ( inputState.guessing==0 ) {
								maxlen = 0;
							}
							{
							x=expr_named_param();
							{
							switch ( LA(1)) {
							case LESS_COLON:
							{
								match(LESS_COLON);
								n = LT(1);
								match(NUMBER);
								if ( inputState.guessing==0 ) {
									maxlen = Integer.parseInt(n.getText());
								}
								break;
							}
							case RSQUARE:
							case COMMA:
							{
								break;
							}
							default:
							{
								throw new NoViableAltException(LT(1), getFilename());
							}
							}
							}
							if ( inputState.guessing==0 ) {
								params.add(x); maxlens.add(maxlen);
							}
							}
							break;
						}
						case RSQUARE:
						case COMMA:
						{
							if ( inputState.guessing==0 ) {
								throw new SemanticException("missing array bounds in type declaration", getFilename(), l.getLine());
							}
							break;
						}
						default:
						{
							throw new NoViableAltException(LT(1), getFilename());
						}
						}
						}
						{
						_loop51:
						do {
							if ((LA(1)==COMMA)) {
								if ( inputState.guessing==0 ) {
									maxlen = 0;
								}
								match(COMMA);
								x=expr_named_param();
								{
								switch ( LA(1)) {
								case LESS_COLON:
								{
									match(LESS_COLON);
									num = LT(1);
									match(NUMBER);
									if ( inputState.guessing==0 ) {
										maxlen = Integer.parseInt(num.getText());
									}
									break;
								}
								case RSQUARE:
								case COMMA:
								{
									break;
								}
								default:
								{
									throw new NoViableAltException(LT(1), getFilename());
								}
								}
								}
								if ( inputState.guessing==0 ) {
									params.add(x); maxlens.add(maxlen);
								}
							}
							else {
								break _loop51;
							}
							
						} while (true);
						}
						}
						match(RSQUARE);
						if ( inputState.guessing==0 ) {
							
							while (!params.isEmpty()) {
							t = new TypeArray(t, params.lastElement(), maxlens.lastElement());
							params.remove(params.size() - 1);
							maxlens.remove(maxlens.size() - 1);
							}
							
						}
					}
					else {
						break _loop52;
					}
					
				} while (true);
				}
				if ( inputState.guessing==0 ) {
					
					if (isglobal) { t = t.withMemType(CudaMemoryType.GLOBAL); } 
					
				}
				break;
			}
			case TK_void:
			{
				match(TK_void);
				if ( inputState.guessing==0 ) {
					t =  TypePrimitive.voidtype;
				}
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_11);
			} else {
			  throw ex;
			}
		}
		return t;
	}
	
	public final Expression  var_initializer() throws RecognitionException, TokenStreamException {
		Expression x;
		
		x = null;
		
		try {      // for error handling
			boolean synPredMatched151 = false;
			if (((LA(1)==LCURLY) && (_tokenSet_12.member(LA(2))))) {
				int _m151 = mark();
				synPredMatched151 = true;
				inputState.guessing++;
				try {
					{
					arr_initializer();
					}
				}
				catch (RecognitionException pe) {
					synPredMatched151 = false;
				}
				rewind(_m151);
inputState.guessing--;
			}
			if ( synPredMatched151 ) {
				x=arr_initializer();
			}
			else if ((_tokenSet_13.member(LA(1))) && (_tokenSet_14.member(LA(2)))) {
				x=right_expr();
			}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_15);
			} else {
			  throw ex;
			}
		}
		return x;
	}
	
	public final Statement  statement() throws RecognitionException, TokenStreamException {
		Statement s;
		
		Token  tb = null;
		Token  tc = null;
		Token  t = null;
		s = null;
		
		try {      // for error handling
			switch ( LA(1)) {
			case TK_loop:
			case TK_repeat:
			{
				s=loop_statement();
				break;
			}
			case TK_minrepeat:
			{
				s=minrepeat_statement();
				break;
			}
			case TK_fork:
			{
				s=fork_statement();
				break;
			}
			case TK_spmdfork:
			{
				s=spmdfork_statement();
				break;
			}
			case TK_parfor:
			{
				s=parfor_statement();
				break;
			}
			case TK_insert:
			{
				s=insert_block();
				break;
			}
			case TK_reorder:
			{
				s=reorder_block();
				break;
			}
			case TK_atomic:
			{
				s=atomic_block();
				break;
			}
			case LCURLY:
			{
				s=block();
				break;
			}
			case TK_break:
			{
				tb = LT(1);
				match(TK_break);
				match(SEMI);
				if ( inputState.guessing==0 ) {
					s = new StmtBreak(getContext(tb));
				}
				break;
			}
			case TK_continue:
			{
				tc = LT(1);
				match(TK_continue);
				match(SEMI);
				if ( inputState.guessing==0 ) {
					s = new StmtContinue(getContext(tc));
				}
				break;
			}
			case TK_if:
			{
				s=if_else_statement();
				break;
			}
			case TK_while:
			{
				s=while_statement();
				break;
			}
			case TK_do:
			{
				s=do_while_statement();
				match(SEMI);
				break;
			}
			case TK_for:
			{
				s=for_statement();
				break;
			}
			case TK_assert:
			case TK_h_assert:
			{
				s=assert_statement();
				match(SEMI);
				break;
			}
			case TK_assert_max:
			{
				s=assert_max_statement();
				match(SEMI);
				break;
			}
			case TK_return:
			{
				s=return_statement();
				match(SEMI);
				break;
			}
			case SEMI:
			{
				t = LT(1);
				match(SEMI);
				if ( inputState.guessing==0 ) {
					s=new StmtEmpty(getContext(t));
				}
				break;
			}
			default:
				boolean synPredMatched20 = false;
				if (((_tokenSet_0.member(LA(1))) && (_tokenSet_1.member(LA(2))))) {
					int _m20 = mark();
					synPredMatched20 = true;
					inputState.guessing++;
					try {
						{
						return_type();
						match(ID);
						match(LPAREN);
						}
					}
					catch (RecognitionException pe) {
						synPredMatched20 = false;
					}
					rewind(_m20);
inputState.guessing--;
				}
				if ( synPredMatched20 ) {
					s=fdecl_statement();
				}
				else {
					boolean synPredMatched22 = false;
					if (((_tokenSet_2.member(LA(1))) && (_tokenSet_3.member(LA(2))))) {
						int _m22 = mark();
						synPredMatched22 = true;
						inputState.guessing++;
						try {
							{
							data_type();
							match(ID);
							}
						}
						catch (RecognitionException pe) {
							synPredMatched22 = false;
						}
						rewind(_m22);
inputState.guessing--;
					}
					if ( synPredMatched22 ) {
						s=variable_decl();
						match(SEMI);
					}
					else {
						boolean synPredMatched24 = false;
						if (((_tokenSet_16.member(LA(1))) && (_tokenSet_17.member(LA(2))))) {
							int _m24 = mark();
							synPredMatched24 = true;
							inputState.guessing++;
							try {
								{
								expr_statement();
								}
							}
							catch (RecognitionException pe) {
								synPredMatched24 = false;
							}
							rewind(_m24);
inputState.guessing--;
						}
						if ( synPredMatched24 ) {
							s=expr_statement();
							match(SEMI);
						}
						else {
							boolean synPredMatched28 = false;
							if (((_tokenSet_0.member(LA(1))) && (_tokenSet_1.member(LA(2))))) {
								int _m28 = mark();
								synPredMatched28 = true;
								inputState.guessing++;
								try {
									{
									annotation_list();
									{
									_loop27:
									do {
										switch ( LA(1)) {
										case TK_device:
										{
											match(TK_device);
											break;
										}
										case TK_serial:
										{
											match(TK_serial);
											break;
										}
										case TK_harness:
										{
											match(TK_harness);
											break;
										}
										case TK_generator:
										{
											match(TK_generator);
											break;
										}
										case TK_library:
										{
											match(TK_library);
											break;
										}
										case TK_printfcn:
										{
											match(TK_printfcn);
											break;
										}
										case TK_stencil:
										{
											match(TK_stencil);
											break;
										}
										case TK_model:
										{
											match(TK_model);
											break;
										}
										default:
										{
											break _loop27;
										}
										}
									} while (true);
									}
									return_type();
									match(ID);
									match(LPAREN);
									}
								}
								catch (RecognitionException pe) {
									synPredMatched28 = false;
								}
								rewind(_m28);
inputState.guessing--;
							}
							if ( synPredMatched28 ) {
								s=fdecl_statement();
							}
						else {
							throw new NoViableAltException(LT(1), getFilename());
						}
						}}}}
					}
					catch (RecognitionException ex) {
						if (inputState.guessing==0) {
							reportError(ex);
							recover(ex,_tokenSet_18);
						} else {
						  throw ex;
						}
					}
					return s;
				}
				
	public final Statement  loop_statement() throws RecognitionException, TokenStreamException {
		Statement s;
		
		Token  t1 = null;
		Token  t2 = null;
		s = null; Expression exp; Statement b; Token x=null;
		
		try {      // for error handling
			{
			switch ( LA(1)) {
			case TK_loop:
			{
				t1 = LT(1);
				match(TK_loop);
				if ( inputState.guessing==0 ) {
					x=t1;
				}
				break;
			}
			case TK_repeat:
			{
				t2 = LT(1);
				match(TK_repeat);
				if ( inputState.guessing==0 ) {
					x=t2;
				}
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			match(LPAREN);
			exp=right_expr();
			match(RPAREN);
			b=pseudo_block();
			if ( inputState.guessing==0 ) {
				s = new StmtLoop(getContext(x), exp, b);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_18);
			} else {
			  throw ex;
			}
		}
		return s;
	}
	
	public final Statement  minrepeat_statement() throws RecognitionException, TokenStreamException {
		Statement s;
		
		Token  t1 = null;
		s = null; Statement b; Token x=null;
		
		try {      // for error handling
			{
			t1 = LT(1);
			match(TK_minrepeat);
			if ( inputState.guessing==0 ) {
				x=t1;
			}
			}
			b=pseudo_block();
			if ( inputState.guessing==0 ) {
				s = new StmtMinLoop(getContext(x), b);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_18);
			} else {
			  throw ex;
			}
		}
		return s;
	}
	
	public final Statement  fork_statement() throws RecognitionException, TokenStreamException {
		Statement s;
		
		Token  t = null;
		s = null; Statement ivar; Expression exp; Statement b;
		
		try {      // for error handling
			t = LT(1);
			match(TK_fork);
			match(LPAREN);
			ivar=variable_decl();
			match(SEMI);
			exp=right_expr();
			match(RPAREN);
			b=pseudo_block();
			if ( inputState.guessing==0 ) {
				s = new StmtFork(getContext(t), (StmtVarDecl) ivar, exp, b);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_18);
			} else {
			  throw ex;
			}
		}
		return s;
	}
	
	public final Statement  spmdfork_statement() throws RecognitionException, TokenStreamException {
		Statement s;
		
		Token  t = null;
		Token  v = null;
		s = null; String ivar=null; Expression exp; Statement b;
		
		try {      // for error handling
			t = LT(1);
			match(TK_spmdfork);
			match(LPAREN);
			{
			if ((LA(1)==ID) && (LA(2)==SEMI)) {
				v = LT(1);
				match(ID);
				if ( inputState.guessing==0 ) {
					ivar=v.getText();
				}
				match(SEMI);
			}
			else if ((_tokenSet_13.member(LA(1))) && (_tokenSet_19.member(LA(2)))) {
			}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			
			}
			exp=right_expr();
			match(RPAREN);
			b=pseudo_block();
			if ( inputState.guessing==0 ) {
				s = new StmtSpmdfork(getContext(t), ivar, exp, b);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_18);
			} else {
			  throw ex;
			}
		}
		return s;
	}
	
	public final Statement  parfor_statement() throws RecognitionException, TokenStreamException {
		Statement s;
		
		Token  t = null;
		s = null; Expression ivar; Expression exp; Statement b;
		
		try {      // for error handling
			t = LT(1);
			match(TK_parfor);
			match(LPAREN);
			ivar=var_expr();
			match(LARROW);
			exp=range_exp();
			match(RPAREN);
			b=pseudo_block();
			if ( inputState.guessing==0 ) {
				s = new StmtParfor(getContext(t), ivar, exp, b);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_18);
			} else {
			  throw ex;
			}
		}
		return s;
	}
	
	public final StmtInsertBlock  insert_block() throws RecognitionException, TokenStreamException {
		StmtInsertBlock ib;
		
		Token  t = null;
		ib=null; Statement s; List<Statement> insert = new ArrayList<Statement> (), into = new ArrayList<Statement> ();
		
		try {      // for error handling
			t = LT(1);
			match(TK_insert);
			match(LCURLY);
			{
			_loop86:
			do {
				if ((_tokenSet_20.member(LA(1)))) {
					s=statement();
					if ( inputState.guessing==0 ) {
						insert.add(s);
					}
				}
				else {
					break _loop86;
				}
				
			} while (true);
			}
			match(RCURLY);
			match(TK_into);
			match(LCURLY);
			{
			_loop88:
			do {
				if ((_tokenSet_20.member(LA(1)))) {
					s=statement();
					if ( inputState.guessing==0 ) {
						into.add(s);
					}
				}
				else {
					break _loop88;
				}
				
			} while (true);
			}
			match(RCURLY);
			if ( inputState.guessing==0 ) {
				ib = new StmtInsertBlock(getContext(t), insert, into);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_18);
			} else {
			  throw ex;
			}
		}
		return ib;
	}
	
	public final StmtReorderBlock  reorder_block() throws RecognitionException, TokenStreamException {
		StmtReorderBlock sb;
		
		Token  t = null;
		sb=null; Statement s; List l = new ArrayList();
		
		try {      // for error handling
			match(TK_reorder);
			t = LT(1);
			match(LCURLY);
			{
			_loop91:
			do {
				if ((_tokenSet_20.member(LA(1)))) {
					s=statement();
					if ( inputState.guessing==0 ) {
						l.add(s);
					}
				}
				else {
					break _loop91;
				}
				
			} while (true);
			}
			match(RCURLY);
			if ( inputState.guessing==0 ) {
				sb = new StmtReorderBlock(getContext(t), l);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_18);
			} else {
			  throw ex;
			}
		}
		return sb;
	}
	
	public final StmtAtomicBlock  atomic_block() throws RecognitionException, TokenStreamException {
		StmtAtomicBlock ab;
		
		Token  t = null;
		ab=null; Expression c = null; StmtBlock b = null;
		
		try {      // for error handling
			t = LT(1);
			match(TK_atomic);
			{
			switch ( LA(1)) {
			case LPAREN:
			{
				match(LPAREN);
				c=right_expr();
				match(RPAREN);
				break;
			}
			case LCURLY:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			b=block();
			if ( inputState.guessing==0 ) {
				ab = new StmtAtomicBlock (getContext (t), b.getStmts (), c);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_18);
			} else {
			  throw ex;
			}
		}
		return ab;
	}
	
	public final StmtBlock  block() throws RecognitionException, TokenStreamException {
		StmtBlock sb;
		
		Token  t = null;
		sb=null; Statement s; List l = new ArrayList();
		
		try {      // for error handling
			t = LT(1);
			match(LCURLY);
			{
			_loop83:
			do {
				if ((_tokenSet_20.member(LA(1)))) {
					s=statement();
					if ( inputState.guessing==0 ) {
						l.add(s);
					}
				}
				else {
					break _loop83;
				}
				
			} while (true);
			}
			match(RCURLY);
			if ( inputState.guessing==0 ) {
				sb = new StmtBlock(getContext(t), l);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_7);
			} else {
			  throw ex;
			}
		}
		return sb;
	}
	
	public final Statement  fdecl_statement() throws RecognitionException, TokenStreamException {
		Statement s;
		
		s=null; Function f = null;
		
		try {      // for error handling
			f=function_decl();
			if ( inputState.guessing==0 ) {
				s = new StmtFunDecl(f.getCx(), f);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_18);
			} else {
			  throw ex;
			}
		}
		return s;
	}
	
	public final Statement  variable_decl() throws RecognitionException, TokenStreamException {
		Statement s;
		
		Token  id = null;
		Token  id2 = null;
		s = null; Type t; Expression x = null;
			List ts = new ArrayList(); List ns = new ArrayList();
			List xs = new ArrayList();
		
		try {      // for error handling
			t=data_type();
			id = LT(1);
			match(ID);
			{
			switch ( LA(1)) {
			case ASSIGN:
			{
				match(ASSIGN);
				x=var_initializer();
				break;
			}
			case SEMI:
			case COMMA:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			if ( inputState.guessing==0 ) {
				ts.add(t); ns.add(id.getText()); xs.add(x);
			}
			{
			_loop59:
			do {
				if ((LA(1)==COMMA)) {
					if ( inputState.guessing==0 ) {
						x = null;
					}
					match(COMMA);
					id2 = LT(1);
					match(ID);
					{
					switch ( LA(1)) {
					case ASSIGN:
					{
						match(ASSIGN);
						x=var_initializer();
						break;
					}
					case SEMI:
					case COMMA:
					{
						break;
					}
					default:
					{
						throw new NoViableAltException(LT(1), getFilename());
					}
					}
					}
					if ( inputState.guessing==0 ) {
						ts.add(t); ns.add(id2.getText()); xs.add(x);
					}
				}
				else {
					break _loop59;
				}
				
			} while (true);
			}
			if ( inputState.guessing==0 ) {
				s = new StmtVarDecl(getContext (id), ts, ns, xs);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_8);
			} else {
			  throw ex;
			}
		}
		return s;
	}
	
	public final Statement  expr_statement() throws RecognitionException, TokenStreamException {
		Statement s;
		
		s = null; Expression x;
		
		try {      // for error handling
			boolean synPredMatched120 = false;
			if (((_tokenSet_16.member(LA(1))) && (_tokenSet_21.member(LA(2))))) {
				int _m120 = mark();
				synPredMatched120 = true;
				inputState.guessing++;
				try {
					{
					incOrDec();
					}
				}
				catch (RecognitionException pe) {
					synPredMatched120 = false;
				}
				rewind(_m120);
inputState.guessing--;
			}
			if ( synPredMatched120 ) {
				x=incOrDec();
				if ( inputState.guessing==0 ) {
					s = new StmtExpr(x);
				}
			}
			else {
				boolean synPredMatched123 = false;
				if (((_tokenSet_22.member(LA(1))) && (_tokenSet_23.member(LA(2))))) {
					int _m123 = mark();
					synPredMatched123 = true;
					inputState.guessing++;
					try {
						{
						left_expr();
						{
						switch ( LA(1)) {
						case ASSIGN:
						{
							match(ASSIGN);
							break;
						}
						case PLUS_EQUALS:
						{
							match(PLUS_EQUALS);
							break;
						}
						case MINUS_EQUALS:
						{
							match(MINUS_EQUALS);
							break;
						}
						case STAR_EQUALS:
						{
							match(STAR_EQUALS);
							break;
						}
						case DIV_EQUALS:
						{
							match(DIV_EQUALS);
							break;
						}
						default:
						{
							throw new NoViableAltException(LT(1), getFilename());
						}
						}
						}
						}
					}
					catch (RecognitionException pe) {
						synPredMatched123 = false;
					}
					rewind(_m123);
inputState.guessing--;
				}
				if ( synPredMatched123 ) {
					s=assign_expr();
				}
				else {
					boolean synPredMatched126 = false;
					if (((LA(1)==ID) && (LA(2)==LPAREN||LA(2)==AT))) {
						int _m126 = mark();
						synPredMatched126 = true;
						inputState.guessing++;
						try {
							{
							match(ID);
							{
							switch ( LA(1)) {
							case AT:
							{
								match(AT);
								match(ID);
								break;
							}
							case LPAREN:
							{
								break;
							}
							default:
							{
								throw new NoViableAltException(LT(1), getFilename());
							}
							}
							}
							match(LPAREN);
							}
						}
						catch (RecognitionException pe) {
							synPredMatched126 = false;
						}
						rewind(_m126);
inputState.guessing--;
					}
					if ( synPredMatched126 ) {
						x=func_call();
						if ( inputState.guessing==0 ) {
							s = new StmtExpr(x);
						}
					}
					else {
						throw new NoViableAltException(LT(1), getFilename());
					}
					}}
				}
				catch (RecognitionException ex) {
					if (inputState.guessing==0) {
						reportError(ex);
						recover(ex,_tokenSet_24);
					} else {
					  throw ex;
					}
				}
				return s;
			}
			
	public final Statement  if_else_statement() throws RecognitionException, TokenStreamException {
		Statement s;
		
		Token  u = null;
		s = null; Expression x; Statement t, f = null;
		
		try {      // for error handling
			u = LT(1);
			match(TK_if);
			match(LPAREN);
			x=right_expr();
			match(RPAREN);
			t=pseudo_block();
			{
			boolean synPredMatched106 = false;
			if (((LA(1)==TK_else) && (_tokenSet_20.member(LA(2))))) {
				int _m106 = mark();
				synPredMatched106 = true;
				inputState.guessing++;
				try {
					{
					match(TK_else);
					}
				}
				catch (RecognitionException pe) {
					synPredMatched106 = false;
				}
				rewind(_m106);
inputState.guessing--;
			}
			if ( synPredMatched106 ) {
				{
				match(TK_else);
				f=pseudo_block();
				}
			}
			else if ((_tokenSet_18.member(LA(1))) && (_tokenSet_25.member(LA(2)))) {
			}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			
			}
			if ( inputState.guessing==0 ) {
				s = new StmtIfThen(getContext(u), x, t, f);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_18);
			} else {
			  throw ex;
			}
		}
		return s;
	}
	
	public final Statement  while_statement() throws RecognitionException, TokenStreamException {
		Statement s;
		
		Token  t = null;
		s = null; Expression x; Statement b;
		
		try {      // for error handling
			t = LT(1);
			match(TK_while);
			match(LPAREN);
			x=right_expr();
			match(RPAREN);
			b=pseudo_block();
			if ( inputState.guessing==0 ) {
				s = new StmtWhile(getContext(t), x, b);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_18);
			} else {
			  throw ex;
			}
		}
		return s;
	}
	
	public final Statement  do_while_statement() throws RecognitionException, TokenStreamException {
		Statement s;
		
		Token  t = null;
		s = null; Expression x; Statement b;
		
		try {      // for error handling
			t = LT(1);
			match(TK_do);
			b=pseudo_block();
			match(TK_while);
			match(LPAREN);
			x=right_expr();
			match(RPAREN);
			if ( inputState.guessing==0 ) {
				s = new StmtDoWhile(getContext(t), b, x);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_8);
			} else {
			  throw ex;
			}
		}
		return s;
	}
	
	public final Statement  for_statement() throws RecognitionException, TokenStreamException {
		Statement s;
		
		Token  t = null;
		s = null; Expression x=null; Statement a, b, c;
		
		try {      // for error handling
			t = LT(1);
			match(TK_for);
			match(LPAREN);
			a=for_init_statement();
			match(SEMI);
			{
			switch ( LA(1)) {
			case TK_new:
			case TK_null:
			case TK_true:
			case TK_false:
			case LPAREN:
			case LCURLY:
			case INCREMENT:
			case MINUS:
			case DECREMENT:
			case BANG:
			case NDVAL:
			case NDVAL2:
			case NDANGELIC:
			case REGEN:
			case CHAR_LITERAL:
			case STRING_LITERAL:
			case HQUAN:
			case NUMBER:
			case ID:
			{
				x=right_expr();
				break;
			}
			case SEMI:
			{
				if ( inputState.guessing==0 ) {
					x = ExprConstInt.one;
				}
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			match(SEMI);
			b=for_incr_statement();
			match(RPAREN);
			c=pseudo_block();
			if ( inputState.guessing==0 ) {
				s = new StmtFor(getContext(t), a, x, b, c, false);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_18);
			} else {
			  throw ex;
			}
		}
		return s;
	}
	
	public final StmtAssert  assert_statement() throws RecognitionException, TokenStreamException {
		StmtAssert s;
		
		Token  t1 = null;
		Token  t2 = null;
		Token  ass = null;
		s = null; Expression x;
		
		try {      // for error handling
			{
			switch ( LA(1)) {
			case TK_assert:
			{
				t1 = LT(1);
				match(TK_assert);
				break;
			}
			case TK_h_assert:
			{
				t2 = LT(1);
				match(TK_h_assert);
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			x=right_expr();
			{
			switch ( LA(1)) {
			case COLON:
			{
				match(COLON);
				ass = LT(1);
				match(STRING_LITERAL);
				break;
			}
			case SEMI:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			if ( inputState.guessing==0 ) {
				
						String msg = null;
						Token t = t1;
						if(t==null){ t=t2;}
						FEContext cx =getContext(t); 
						if(ass!=null){
							String ps = ass.getText();
					        ps = ps.substring(1, ps.length()-1);
							msg =cx + "   "+ ps;	
						}
						s = new StmtAssert(cx, x, msg, t2!=null);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_8);
			} else {
			  throw ex;
			}
		}
		return s;
	}
	
	public final StmtAssert  assert_max_statement() throws RecognitionException, TokenStreamException {
		StmtAssert s;
		
		Token  t = null;
		Token  defer = null;
		Token  ass = null;
		s = null; Expression cond; ExprVar var;
		
		try {      // for error handling
			t = LT(1);
			match(TK_assert_max);
			{
			switch ( LA(1)) {
			case BACKSLASH:
			{
				defer = LT(1);
				match(BACKSLASH);
				break;
			}
			case TK_new:
			case TK_null:
			case TK_true:
			case TK_false:
			case LPAREN:
			case LCURLY:
			case INCREMENT:
			case MINUS:
			case DECREMENT:
			case BANG:
			case NDVAL:
			case NDVAL2:
			case NDANGELIC:
			case REGEN:
			case CHAR_LITERAL:
			case STRING_LITERAL:
			case HQUAN:
			case NUMBER:
			case ID:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			cond=right_expr();
			{
			switch ( LA(1)) {
			case COLON:
			{
				match(COLON);
				ass = LT(1);
				match(STRING_LITERAL);
				break;
			}
			case SEMI:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			if ( inputState.guessing==0 ) {
				
					FEContext cx = getContext(t);
					String msg = null;
					if (ass != null) {
						String ps = ass.getText();
						ps = ps.substring(1, ps.length()-1);
						msg = cx + "   "+ ps;
					}
					s = StmtAssert.createAssertMax(cx, cond, msg, (defer!=null));
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_8);
			} else {
			  throw ex;
			}
		}
		return s;
	}
	
	public final StmtReturn  return_statement() throws RecognitionException, TokenStreamException {
		StmtReturn s;
		
		Token  t = null;
		s = null; Expression x = null;
		
		try {      // for error handling
			t = LT(1);
			match(TK_return);
			{
			switch ( LA(1)) {
			case TK_new:
			case TK_null:
			case TK_true:
			case TK_false:
			case LPAREN:
			case LCURLY:
			case INCREMENT:
			case MINUS:
			case DECREMENT:
			case BANG:
			case NDVAL:
			case NDVAL2:
			case NDANGELIC:
			case REGEN:
			case CHAR_LITERAL:
			case STRING_LITERAL:
			case HQUAN:
			case NUMBER:
			case ID:
			{
				x=right_expr();
				break;
			}
			case SEMI:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			if ( inputState.guessing==0 ) {
				s = new StmtReturn(getContext(t), x);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_8);
			} else {
			  throw ex;
			}
		}
		return s;
	}
	
	public final StmtBlock  pseudo_block() throws RecognitionException, TokenStreamException {
		StmtBlock sb;
		
		sb=null; Statement s; List l = new ArrayList();
		
		try {      // for error handling
			s=statement();
			if ( inputState.guessing==0 ) {
				l.add(s);
			}
			if ( inputState.guessing==0 ) {
				sb = new StmtBlock(s.getContext(), l);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_18);
			} else {
			  throw ex;
			}
		}
		return sb;
	}
	
	public final Expression  var_expr() throws RecognitionException, TokenStreamException {
		Expression x;
		
		Token  name = null;
		x = null; List rlist;
		
		try {      // for error handling
			name = LT(1);
			match(ID);
			if ( inputState.guessing==0 ) {
				x = new ExprVar(getContext(name), name.getText());
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_26);
			} else {
			  throw ex;
			}
		}
		return x;
	}
	
	public final Expression  range_exp() throws RecognitionException, TokenStreamException {
		Expression e;
		
		Token  t = null;
		e = null; Expression from; Expression until; Expression by = new ExprConstInt(1);
		
		try {      // for error handling
			from=right_expr();
			t = LT(1);
			match(TK_until);
			until=right_expr();
			{
			switch ( LA(1)) {
			case TK_by:
			{
				match(TK_by);
				by=right_expr();
				break;
			}
			case RPAREN:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			if ( inputState.guessing==0 ) {
				
			}
			if ( inputState.guessing==0 ) {
				e = new ExprRange(getContext(t), from, until, by);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_27);
			} else {
			  throw ex;
			}
		}
		return e;
	}
	
	public final Type  primitive_type() throws RecognitionException, TokenStreamException {
		Type t;
		
		t = null;
		
		try {      // for error handling
			{
			switch ( LA(1)) {
			case TK_boolean:
			{
				match(TK_boolean);
				if ( inputState.guessing==0 ) {
					t = TypePrimitive.bittype;
				}
				break;
			}
			case TK_bit:
			{
				match(TK_bit);
				if ( inputState.guessing==0 ) {
					t = TypePrimitive.bittype;
				}
				break;
			}
			case TK_int:
			{
				match(TK_int);
				if ( inputState.guessing==0 ) {
					t = TypePrimitive.inttype;
				}
				break;
			}
			case TK_float:
			{
				match(TK_float);
				if ( inputState.guessing==0 ) {
					t = TypePrimitive.floattype;
				}
				break;
			}
			case TK_double:
			{
				match(TK_double);
				if ( inputState.guessing==0 ) {
					t = TypePrimitive.doubletype;
				}
				break;
			}
			case TK_fun:
			{
				match(TK_fun);
				if ( inputState.guessing==0 ) {
					t = TypeFunction.singleton;
				}
				break;
			}
			case TK_char:
			{
				match(TK_char);
				if ( inputState.guessing==0 ) {
					t = TypePrimitive.chartype;
				}
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_28);
			} else {
			  throw ex;
			}
		}
		return t;
	}
	
	public final  Expression  expr_named_param() throws RecognitionException, TokenStreamException {
		 Expression x ;
		
		Token  id = null;
		x = null; Token t = null;
		
		try {      // for error handling
			{
			if ((LA(1)==ID) && (LA(2)==ASSIGN)) {
				id = LT(1);
				match(ID);
				match(ASSIGN);
			}
			else if ((_tokenSet_13.member(LA(1))) && (_tokenSet_29.member(LA(2)))) {
			}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			
			}
			if ( inputState.guessing==0 ) {
				t = id;
			}
			x=right_expr();
			if ( inputState.guessing==0 ) {
				
				if (t != null) {
				x = new ExprNamedParam(getContext(t), t.getText(), x);
				}
				
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_30);
			} else {
			  throw ex;
			}
		}
		return x ;
	}
	
	public final Annotation  annotation() throws RecognitionException, TokenStreamException {
		Annotation an;
		
		Token  atc = null;
		Token  id = null;
		Token  slit = null;
		
			an = null;
		
		
		try {      // for error handling
			atc = LT(1);
			match(AT);
			id = LT(1);
			match(ID);
			match(LPAREN);
			{
			switch ( LA(1)) {
			case STRING_LITERAL:
			{
				slit = LT(1);
				match(STRING_LITERAL);
				break;
			}
			case RPAREN:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			match(RPAREN);
			if ( inputState.guessing==0 ) {
				
					an = Annotation.newAnnotation(getContext(atc), id.getText(), slit.getText());
				
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_31);
			} else {
			  throw ex;
			}
		}
		return an;
	}
	
	public final List  param_decl_list() throws RecognitionException, TokenStreamException {
		List l;
		
		l = new ArrayList(); List l2; Parameter p;
		
		try {      // for error handling
			match(LPAREN);
			{
			switch ( LA(1)) {
			case LSQUARE:
			{
				l2=impl_param();
				if ( inputState.guessing==0 ) {
					l.addAll(l2);
				}
				break;
			}
			case TK_boolean:
			case TK_float:
			case TK_bit:
			case TK_int:
			case TK_void:
			case TK_double:
			case TK_fun:
			case TK_char:
			case TK_ref:
			case TK_global:
			case RPAREN:
			case ID:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			{
			switch ( LA(1)) {
			case TK_boolean:
			case TK_float:
			case TK_bit:
			case TK_int:
			case TK_void:
			case TK_double:
			case TK_fun:
			case TK_char:
			case TK_ref:
			case TK_global:
			case ID:
			{
				p=param_decl();
				if ( inputState.guessing==0 ) {
					l.add(p);
				}
				{
				_loop75:
				do {
					if ((LA(1)==COMMA)) {
						match(COMMA);
						p=param_decl();
						if ( inputState.guessing==0 ) {
							l.add(p);
						}
					}
					else {
						break _loop75;
					}
					
				} while (true);
				}
				break;
			}
			case RPAREN:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			match(RPAREN);
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_32);
			} else {
			  throw ex;
			}
		}
		return l;
	}
	
	public final List  impl_param() throws RecognitionException, TokenStreamException {
		List l;
		
		Token  id = null;
		Token  id2 = null;
		Parameter p; l = new ArrayList();
		
		try {      // for error handling
			match(LSQUARE);
			match(TK_int);
			id = LT(1);
			match(ID);
			if ( inputState.guessing==0 ) {
				p= new Parameter(getContext(id), TypePrimitive.inttype, id.getText(), Parameter.IN, true); l.add(p);
			}
			{
			_loop78:
			do {
				if ((LA(1)==COMMA)) {
					match(COMMA);
					match(TK_int);
					id2 = LT(1);
					match(ID);
					if ( inputState.guessing==0 ) {
						p= new Parameter(getContext(id), TypePrimitive.inttype, id2.getText(), Parameter.IN, true); l.add(p);
					}
				}
				else {
					break _loop78;
				}
				
			} while (true);
			}
			match(RSQUARE);
			match(COMMA);
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_33);
			} else {
			  throw ex;
			}
		}
		return l;
	}
	
	public final Parameter  param_decl() throws RecognitionException, TokenStreamException {
		Parameter p;
		
		Token  id = null;
		Type t; p = null; boolean isRef=false;
		
		try {      // for error handling
			{
			switch ( LA(1)) {
			case TK_ref:
			{
				match(TK_ref);
				if ( inputState.guessing==0 ) {
					isRef=true;
				}
				break;
			}
			case TK_boolean:
			case TK_float:
			case TK_bit:
			case TK_int:
			case TK_void:
			case TK_double:
			case TK_fun:
			case TK_char:
			case TK_global:
			case ID:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			t=data_type();
			id = LT(1);
			match(ID);
			if ( inputState.guessing==0 ) {
				p = new Parameter(getContext(id), t, id.getText(), isRef? Parameter.REF : Parameter.IN, false);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_34);
			} else {
			  throw ex;
			}
		}
		return p;
	}
	
	public final Statement  for_init_statement() throws RecognitionException, TokenStreamException {
		Statement s;
		
		s = null;
		
		try {      // for error handling
			boolean synPredMatched114 = false;
			if (((_tokenSet_2.member(LA(1))) && (_tokenSet_3.member(LA(2))))) {
				int _m114 = mark();
				synPredMatched114 = true;
				inputState.guessing++;
				try {
					{
					variable_decl();
					}
				}
				catch (RecognitionException pe) {
					synPredMatched114 = false;
				}
				rewind(_m114);
inputState.guessing--;
			}
			if ( synPredMatched114 ) {
				s=variable_decl();
			}
			else {
				boolean synPredMatched116 = false;
				if (((_tokenSet_16.member(LA(1))) && (_tokenSet_17.member(LA(2))))) {
					int _m116 = mark();
					synPredMatched116 = true;
					inputState.guessing++;
					try {
						{
						expr_statement();
						}
					}
					catch (RecognitionException pe) {
						synPredMatched116 = false;
					}
					rewind(_m116);
inputState.guessing--;
				}
				if ( synPredMatched116 ) {
					s=expr_statement();
				}
				else if ((LA(1)==SEMI)) {
					if ( inputState.guessing==0 ) {
						s = new StmtEmpty((FEContext)null);
					}
				}
				else {
					throw new NoViableAltException(LT(1), getFilename());
				}
				}
			}
			catch (RecognitionException ex) {
				if (inputState.guessing==0) {
					reportError(ex);
					recover(ex,_tokenSet_8);
				} else {
				  throw ex;
				}
			}
			return s;
		}
		
	public final Statement  for_incr_statement() throws RecognitionException, TokenStreamException {
		Statement s;
		
		s = null;
		
		try {      // for error handling
			switch ( LA(1)) {
			case TK_new:
			case LPAREN:
			case INCREMENT:
			case DECREMENT:
			case REGEN:
			case ID:
			{
				s=expr_statement();
				break;
			}
			case RPAREN:
			{
				if ( inputState.guessing==0 ) {
					s = new StmtEmpty((FEContext) null);
				}
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_27);
			} else {
			  throw ex;
			}
		}
		return s;
	}
	
	public final Expression  incOrDec() throws RecognitionException, TokenStreamException {
		Expression x;
		
		Token  i = null;
		Token  d = null;
		x = null; Expression bound = null; Type t = null;
		
		try {      // for error handling
			switch ( LA(1)) {
			case TK_new:
			case LPAREN:
			case REGEN:
			case ID:
			{
				x=left_expr();
				{
				switch ( LA(1)) {
				case INCREMENT:
				{
					match(INCREMENT);
					if ( inputState.guessing==0 ) {
						x = new ExprUnary(x.getContext(), ExprUnary.UNOP_POSTINC, x);
					}
					break;
				}
				case DECREMENT:
				{
					match(DECREMENT);
					if ( inputState.guessing==0 ) {
						x = new ExprUnary(x.getContext(), ExprUnary.UNOP_POSTDEC, x);
					}
					break;
				}
				default:
				{
					throw new NoViableAltException(LT(1), getFilename());
				}
				}
				}
				break;
			}
			case INCREMENT:
			{
				i = LT(1);
				match(INCREMENT);
				x=left_expr();
				if ( inputState.guessing==0 ) {
					x = new ExprUnary(getContext(i), ExprUnary.UNOP_PREINC, x);
				}
				break;
			}
			case DECREMENT:
			{
				d = LT(1);
				match(DECREMENT);
				x=left_expr();
				if ( inputState.guessing==0 ) {
					x = new ExprUnary(getContext(d), ExprUnary.UNOP_PREDEC, x);
				}
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_35);
			} else {
			  throw ex;
			}
		}
		return x;
	}
	
	public final Expression  left_expr() throws RecognitionException, TokenStreamException {
		Expression x;
		
		Token  field = null;
		Token  l = null;
		x = null; Vector<ExprArrayRange.RangeLen> rl;
		
		try {      // for error handling
			x=uminic_value_expr();
			{
			_loop144:
			do {
				switch ( LA(1)) {
				case DOT:
				{
					match(DOT);
					field = LT(1);
					match(ID);
					if ( inputState.guessing==0 ) {
						x = new ExprField(x, x, field.getText());
					}
					break;
				}
				case LSQUARE:
				{
					l = LT(1);
					match(LSQUARE);
					rl=array_range();
					if ( inputState.guessing==0 ) {
						x = new ExprArrayRange(x, x, rl);
					}
					match(RSQUARE);
					break;
				}
				default:
				{
					break _loop144;
				}
				}
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_36);
			} else {
			  throw ex;
			}
		}
		return x;
	}
	
	public final Statement  assign_expr() throws RecognitionException, TokenStreamException {
		Statement s;
		
		s = null; Expression l, r; int o = 0;
		
		try {      // for error handling
			l=left_expr();
			{
			switch ( LA(1)) {
			case ASSIGN:
			{
				match(ASSIGN);
				if ( inputState.guessing==0 ) {
					o = 0;
				}
				break;
			}
			case PLUS_EQUALS:
			{
				match(PLUS_EQUALS);
				if ( inputState.guessing==0 ) {
					o = ExprBinary.BINOP_ADD;
				}
				break;
			}
			case MINUS_EQUALS:
			{
				match(MINUS_EQUALS);
				if ( inputState.guessing==0 ) {
					o = ExprBinary.BINOP_SUB;
				}
				break;
			}
			case STAR_EQUALS:
			{
				match(STAR_EQUALS);
				if ( inputState.guessing==0 ) {
					o = ExprBinary.BINOP_MUL;
				}
				break;
			}
			case DIV_EQUALS:
			{
				match(DIV_EQUALS);
				if ( inputState.guessing==0 ) {
					o = ExprBinary.BINOP_DIV;
				}
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			r=right_expr();
			if ( inputState.guessing==0 ) {
				s = new StmtAssign(l, r, o); s.resetOrigin();
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_24);
			} else {
			  throw ex;
			}
		}
		return s;
	}
	
	public final Expression  func_call() throws RecognitionException, TokenStreamException {
		Expression x;
		
		Token  prefix = null;
		Token  name = null;
		x = null; List l;
		
		try {      // for error handling
			prefix = LT(1);
			match(ID);
			{
			switch ( LA(1)) {
			case AT:
			{
				match(AT);
				name = LT(1);
				match(ID);
				break;
			}
			case LPAREN:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			l=func_call_params();
			if ( inputState.guessing==0 ) {
				x = new ExprFunCall(getContext(prefix), prefix.getText()+ (name != null ? ("@" + name.getText()) : ""), l);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_37);
			} else {
			  throw ex;
			}
		}
		return x;
	}
	
	public final List  func_call_params() throws RecognitionException, TokenStreamException {
		List l;
		
		l = new ArrayList(); Expression x;
		
		try {      // for error handling
			match(LPAREN);
			{
			switch ( LA(1)) {
			case TK_new:
			case TK_null:
			case TK_true:
			case TK_false:
			case LPAREN:
			case LCURLY:
			case INCREMENT:
			case MINUS:
			case DECREMENT:
			case BANG:
			case NDVAL:
			case NDVAL2:
			case NDANGELIC:
			case REGEN:
			case CHAR_LITERAL:
			case STRING_LITERAL:
			case HQUAN:
			case NUMBER:
			case ID:
			{
				x=expr_named_param();
				if ( inputState.guessing==0 ) {
					l.add(x);
				}
				{
				_loop134:
				do {
					if ((LA(1)==COMMA)) {
						match(COMMA);
						x=expr_named_param();
						if ( inputState.guessing==0 ) {
							l.add(x);
						}
					}
					else {
						break _loop134;
					}
					
				} while (true);
				}
				break;
			}
			case RPAREN:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			match(RPAREN);
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_37);
			} else {
			  throw ex;
			}
		}
		return l;
	}
	
	public final List  constr_params() throws RecognitionException, TokenStreamException {
		List l;
		
		l = new ArrayList(); Expression x;
		
		try {      // for error handling
			match(LPAREN);
			{
			switch ( LA(1)) {
			case ID:
			{
				x=expr_named_param_only();
				if ( inputState.guessing==0 ) {
					l.add(x);
				}
				{
				_loop138:
				do {
					if ((LA(1)==COMMA)) {
						match(COMMA);
						x=expr_named_param();
						if ( inputState.guessing==0 ) {
							l.add(x);
						}
					}
					else {
						break _loop138;
					}
					
				} while (true);
				}
				break;
			}
			case RPAREN:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			match(RPAREN);
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_38);
			} else {
			  throw ex;
			}
		}
		return l;
	}
	
	public final  Expression  expr_named_param_only() throws RecognitionException, TokenStreamException {
		 Expression x ;
		
		Token  id = null;
		x = null; Token t = null;
		
		try {      // for error handling
			id = LT(1);
			match(ID);
			match(ASSIGN);
			x=right_expr();
			if ( inputState.guessing==0 ) {
				x = new ExprNamedParam(getContext(id), id.getText(), x);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_39);
			} else {
			  throw ex;
			}
		}
		return x ;
	}
	
	public final Expression  uminic_value_expr() throws RecognitionException, TokenStreamException {
		Expression x;
		
		Token  r = null;
		x = null;
		
		try {      // for error handling
			switch ( LA(1)) {
			case LPAREN:
			{
				match(LPAREN);
				x=right_expr();
				match(RPAREN);
				break;
			}
			case TK_new:
			{
				x=constructor_expr();
				break;
			}
			case ID:
			{
				x=var_expr();
				break;
			}
			case REGEN:
			{
				r = LT(1);
				match(REGEN);
				if ( inputState.guessing==0 ) {
					x = new ExprRegen (getContext (r), r.getText ());
				}
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_38);
			} else {
			  throw ex;
			}
		}
		return x;
	}
	
	public final Vector<ExprArrayRange.RangeLen>  array_range() throws RecognitionException, TokenStreamException {
		Vector<ExprArrayRange.RangeLen> x;
		
		x=null; Expression start,end,l;
		
		try {      // for error handling
			start=expr_named_param();
			if ( inputState.guessing==0 ) {
				
				assert (!(start instanceof ExprNamedParam));
				x = new Vector<ExprArrayRange.RangeLen>();
				x.add(new ExprArrayRange.RangeLen(start));
				
			}
			{
			_loop227:
			do {
				if ((LA(1)==COMMA)) {
					match(COMMA);
					start=expr_named_param();
					if ( inputState.guessing==0 ) {
						x.add(new ExprArrayRange.RangeLen(start));
					}
				}
				else {
					break _loop227;
				}
				
			} while (true);
			}
			{
			switch ( LA(1)) {
			case COLON:
			{
				match(COLON);
				{
				switch ( LA(1)) {
				case TK_new:
				case TK_null:
				case TK_true:
				case TK_false:
				case LPAREN:
				case LCURLY:
				case INCREMENT:
				case MINUS:
				case DECREMENT:
				case BANG:
				case NDVAL:
				case NDVAL2:
				case NDANGELIC:
				case REGEN:
				case CHAR_LITERAL:
				case STRING_LITERAL:
				case HQUAN:
				case NUMBER:
				case ID:
				{
					end=right_expr();
					if ( inputState.guessing==0 ) {
						
								  		assert x.size() == 1 : "cannot mix comma indices and array ranges yet";
								  		x.set(0, new ExprArrayRange.RangeLen(start,
								  			new ExprBinary(end, "-", start)));
					}
					break;
				}
				case COLON:
				{
					match(COLON);
					l=right_expr();
					if ( inputState.guessing==0 ) {
						
								    	assert x.size() == 1 : "cannot mix comma indices and array ranges yet";
								    	x.set(0, new ExprArrayRange.RangeLen(start,l));
					}
					break;
				}
				default:
				{
					throw new NoViableAltException(LT(1), getFilename());
				}
				}
				}
				break;
			}
			case RSQUARE:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_40);
			} else {
			  throw ex;
			}
		}
		return x;
	}
	
	public final Expression  right_expr_not_agmax() throws RecognitionException, TokenStreamException {
		Expression x;
		
		x = null;
		
		try {      // for error handling
			x=ternaryExpr();
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_10);
			} else {
			  throw ex;
			}
		}
		return x;
	}
	
	public final Expression  ternaryExpr() throws RecognitionException, TokenStreamException {
		Expression x;
		
		x = null; Expression b, c;
		
		try {      // for error handling
			x=logicOrExpr();
			{
			switch ( LA(1)) {
			case QUESTION:
			{
				match(QUESTION);
				b=ternaryExpr();
				match(COLON);
				c=ternaryExpr();
				if ( inputState.guessing==0 ) {
					x = new ExprTernary(x, ExprTernary.TEROP_COND,
										x, b, c);
				}
				break;
			}
			case EOF:
			case TK_until:
			case TK_by:
			case RPAREN:
			case RCURLY:
			case RSQUARE:
			case COLON:
			case SEMI:
			case COMMA:
			case LESS_COLON:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_10);
			} else {
			  throw ex;
			}
		}
		return x;
	}
	
	public final Expression  agmax_expr() throws RecognitionException, TokenStreamException {
		Expression x;
		
		Token  t = null;
		Token  n = null;
		x = null;
		
		try {      // for error handling
			t = LT(1);
			match(NDANGELIC);
			{
			switch ( LA(1)) {
			case LPAREN:
			{
				match(LPAREN);
				n = LT(1);
				match(NUMBER);
				match(RPAREN);
				break;
			}
			case EOF:
			case TK_until:
			case TK_by:
			case RPAREN:
			case RCURLY:
			case RSQUARE:
			case COLON:
			case SEMI:
			case COMMA:
			case LESS_COLON:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			if ( inputState.guessing==0 ) {
				
						if (n != null) {
							x = new ExprStar(getContext(t), Integer.parseInt(n.getText()), true);
						} else {
							x = new ExprStar(getContext(t), true);
						}
					
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_10);
			} else {
			  throw ex;
			}
		}
		return x;
	}
	
	public final Expression  arr_initializer() throws RecognitionException, TokenStreamException {
		Expression x;
		
		Token  lc = null;
		ArrayList l = new ArrayList();
		x = null;
		Expression y;
		
		try {      // for error handling
			lc = LT(1);
			match(LCURLY);
			{
			switch ( LA(1)) {
			case TK_new:
			case TK_null:
			case TK_true:
			case TK_false:
			case LPAREN:
			case LCURLY:
			case INCREMENT:
			case MINUS:
			case DECREMENT:
			case BANG:
			case NDVAL:
			case NDVAL2:
			case NDANGELIC:
			case REGEN:
			case CHAR_LITERAL:
			case STRING_LITERAL:
			case HQUAN:
			case NUMBER:
			case ID:
			{
				y=var_initializer();
				if ( inputState.guessing==0 ) {
					l.add(y);
				}
				{
				_loop158:
				do {
					if ((LA(1)==COMMA)) {
						match(COMMA);
						y=var_initializer();
						if ( inputState.guessing==0 ) {
							l.add(y);
						}
					}
					else {
						break _loop158;
					}
					
				} while (true);
				}
				break;
			}
			case RCURLY:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			match(RCURLY);
			if ( inputState.guessing==0 ) {
				x = new ExprArrayInit(getContext(lc), l);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_37);
			} else {
			  throw ex;
			}
		}
		return x;
	}
	
	public final Expression  type_or_var_initializer() throws RecognitionException, TokenStreamException {
		Expression x;
		
		x = null;
		
		try {      // for error handling
			boolean synPredMatched154 = false;
			if (((LA(1)==LCURLY) && (_tokenSet_12.member(LA(2))))) {
				int _m154 = mark();
				synPredMatched154 = true;
				inputState.guessing++;
				try {
					{
					arr_initializer();
					}
				}
				catch (RecognitionException pe) {
					synPredMatched154 = false;
				}
				rewind(_m154);
inputState.guessing--;
			}
			if ( synPredMatched154 ) {
				x=arr_initializer();
			}
			else if ((_tokenSet_13.member(LA(1))) && (_tokenSet_41.member(LA(2)))) {
				x=right_expr();
			}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_4);
			} else {
			  throw ex;
			}
		}
		return x;
	}
	
	public final Expression  logicOrExpr() throws RecognitionException, TokenStreamException {
		Expression x;
		
		x = null; Expression r; int o = 0;
		
		try {      // for error handling
			x=logicAndExpr();
			{
			_loop163:
			do {
				if ((LA(1)==LOGIC_OR)) {
					match(LOGIC_OR);
					r=logicAndExpr();
					if ( inputState.guessing==0 ) {
						x = new ExprBinary(ExprBinary.BINOP_OR, x, r);
					}
				}
				else {
					break _loop163;
				}
				
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_42);
			} else {
			  throw ex;
			}
		}
		return x;
	}
	
	public final Expression  logicAndExpr() throws RecognitionException, TokenStreamException {
		Expression x;
		
		x = null; Expression r;
		
		try {      // for error handling
			x=bitwiseOrExpr();
			{
			_loop166:
			do {
				if ((LA(1)==LOGIC_AND)) {
					match(LOGIC_AND);
					r=bitwiseOrExpr();
					if ( inputState.guessing==0 ) {
						x = new ExprBinary(ExprBinary.BINOP_AND, x, r);
					}
				}
				else {
					break _loop166;
				}
				
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_43);
			} else {
			  throw ex;
			}
		}
		return x;
	}
	
	public final Expression  bitwiseOrExpr() throws RecognitionException, TokenStreamException {
		Expression x;
		
		x = null; Expression r;
		
		try {      // for error handling
			x=bitwiseXorExpr();
			{
			_loop169:
			do {
				if ((LA(1)==BITWISE_OR)) {
					match(BITWISE_OR);
					r=bitwiseXorExpr();
					if ( inputState.guessing==0 ) {
						x = new ExprBinary(ExprBinary.BINOP_BOR, x, r);
					}
				}
				else {
					break _loop169;
				}
				
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_44);
			} else {
			  throw ex;
			}
		}
		return x;
	}
	
	public final Expression  bitwiseXorExpr() throws RecognitionException, TokenStreamException {
		Expression x;
		
		x = null; Expression r;
		
		try {      // for error handling
			x=bitwiseAndExpr();
			{
			_loop172:
			do {
				if ((LA(1)==BITWISE_XOR)) {
					match(BITWISE_XOR);
					r=bitwiseAndExpr();
					if ( inputState.guessing==0 ) {
						x = new ExprBinary(ExprBinary.BINOP_BXOR, x, r);
					}
				}
				else {
					break _loop172;
				}
				
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_45);
			} else {
			  throw ex;
			}
		}
		return x;
	}
	
	public final Expression  bitwiseAndExpr() throws RecognitionException, TokenStreamException {
		Expression x;
		
		x = null; Expression r;
		
		try {      // for error handling
			x=equalExpr();
			{
			_loop175:
			do {
				if ((LA(1)==BITWISE_AND)) {
					match(BITWISE_AND);
					r=equalExpr();
					if ( inputState.guessing==0 ) {
						x = new ExprBinary(ExprBinary.BINOP_BAND, x, r);
					}
				}
				else {
					break _loop175;
				}
				
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_46);
			} else {
			  throw ex;
			}
		}
		return x;
	}
	
	public final Expression  equalExpr() throws RecognitionException, TokenStreamException {
		Expression x;
		
		x = null; Expression r; int o = 0;
		
		try {      // for error handling
			x=compareExpr();
			{
			_loop179:
			do {
				if ((LA(1)==EQUAL||LA(1)==NOT_EQUAL)) {
					{
					switch ( LA(1)) {
					case EQUAL:
					{
						match(EQUAL);
						if ( inputState.guessing==0 ) {
							o = ExprBinary.BINOP_EQ;
						}
						break;
					}
					case NOT_EQUAL:
					{
						match(NOT_EQUAL);
						if ( inputState.guessing==0 ) {
							o = ExprBinary.BINOP_NEQ;
						}
						break;
					}
					default:
					{
						throw new NoViableAltException(LT(1), getFilename());
					}
					}
					}
					r=compareExpr();
					if ( inputState.guessing==0 ) {
						x = new ExprBinary(o, x, r);
					}
				}
				else {
					break _loop179;
				}
				
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_47);
			} else {
			  throw ex;
			}
		}
		return x;
	}
	
	public final Expression  compareExpr() throws RecognitionException, TokenStreamException {
		Expression x;
		
		x = null; Expression r; int o = 0;
		
		try {      // for error handling
			x=shiftExpr();
			{
			_loop183:
			do {
				if (((LA(1) >= LESS_THAN && LA(1) <= MORE_EQUAL))) {
					{
					switch ( LA(1)) {
					case LESS_THAN:
					{
						match(LESS_THAN);
						if ( inputState.guessing==0 ) {
							o = ExprBinary.BINOP_LT;
						}
						break;
					}
					case LESS_EQUAL:
					{
						match(LESS_EQUAL);
						if ( inputState.guessing==0 ) {
							o = ExprBinary.BINOP_LE;
						}
						break;
					}
					case MORE_THAN:
					{
						match(MORE_THAN);
						if ( inputState.guessing==0 ) {
							o = ExprBinary.BINOP_GT;
						}
						break;
					}
					case MORE_EQUAL:
					{
						match(MORE_EQUAL);
						if ( inputState.guessing==0 ) {
							o = ExprBinary.BINOP_GE;
						}
						break;
					}
					default:
					{
						throw new NoViableAltException(LT(1), getFilename());
					}
					}
					}
					r=shiftExpr();
					if ( inputState.guessing==0 ) {
						x = new ExprBinary(o, x, r);
					}
				}
				else {
					break _loop183;
				}
				
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_48);
			} else {
			  throw ex;
			}
		}
		return x;
	}
	
	public final Expression  shiftExpr() throws RecognitionException, TokenStreamException {
		Expression x;
		
		x=null; Expression r; int op=0;
		
		try {      // for error handling
			x=addExpr();
			{
			_loop187:
			do {
				if ((LA(1)==LSHIFT||LA(1)==RSHIFT)) {
					{
					switch ( LA(1)) {
					case LSHIFT:
					{
						match(LSHIFT);
						if ( inputState.guessing==0 ) {
							op=ExprBinary.BINOP_LSHIFT;
						}
						break;
					}
					case RSHIFT:
					{
						match(RSHIFT);
						if ( inputState.guessing==0 ) {
							op=ExprBinary.BINOP_RSHIFT;
						}
						break;
					}
					default:
					{
						throw new NoViableAltException(LT(1), getFilename());
					}
					}
					}
					r=addExpr();
					if ( inputState.guessing==0 ) {
						x = new ExprBinary(op, x, r);
					}
				}
				else {
					break _loop187;
				}
				
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_49);
			} else {
			  throw ex;
			}
		}
		return x;
	}
	
	public final Expression  addExpr() throws RecognitionException, TokenStreamException {
		Expression x;
		
		x = null; Expression r; int o = 0;
		
		try {      // for error handling
			x=multExpr();
			{
			_loop191:
			do {
				if ((LA(1)==PLUS||LA(1)==MINUS||LA(1)==SELECT)) {
					{
					switch ( LA(1)) {
					case PLUS:
					{
						match(PLUS);
						if ( inputState.guessing==0 ) {
							o = ExprBinary.BINOP_ADD;
						}
						break;
					}
					case MINUS:
					{
						match(MINUS);
						if ( inputState.guessing==0 ) {
							o = ExprBinary.BINOP_SUB;
						}
						break;
					}
					case SELECT:
					{
						match(SELECT);
						if ( inputState.guessing==0 ) {
							o = ExprBinary.BINOP_SELECT;
						}
						break;
					}
					default:
					{
						throw new NoViableAltException(LT(1), getFilename());
					}
					}
					}
					r=multExpr();
					if ( inputState.guessing==0 ) {
						x = new ExprBinary(o, x, r);
					}
				}
				else {
					break _loop191;
				}
				
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_50);
			} else {
			  throw ex;
			}
		}
		return x;
	}
	
	public final Expression  multExpr() throws RecognitionException, TokenStreamException {
		Expression x;
		
		x = null; Expression r; int o = 0;
		
		try {      // for error handling
			x=inc_dec_expr();
			{
			_loop195:
			do {
				if ((LA(1)==STAR||LA(1)==DIV||LA(1)==MOD)) {
					{
					switch ( LA(1)) {
					case STAR:
					{
						match(STAR);
						if ( inputState.guessing==0 ) {
							o = ExprBinary.BINOP_MUL;
						}
						break;
					}
					case DIV:
					{
						match(DIV);
						if ( inputState.guessing==0 ) {
							o = ExprBinary.BINOP_DIV;
						}
						break;
					}
					case MOD:
					{
						match(MOD);
						if ( inputState.guessing==0 ) {
							o = ExprBinary.BINOP_MOD;
						}
						break;
					}
					default:
					{
						throw new NoViableAltException(LT(1), getFilename());
					}
					}
					}
					r=inc_dec_expr();
					if ( inputState.guessing==0 ) {
						x = new ExprBinary(o, x, r);
					}
				}
				else {
					break _loop195;
				}
				
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_51);
			} else {
			  throw ex;
			}
		}
		return x;
	}
	
	public final Expression  inc_dec_expr() throws RecognitionException, TokenStreamException {
		Expression x;
		
		Token  b = null;
		x = null;
		
		try {      // for error handling
			boolean synPredMatched198 = false;
			if (((_tokenSet_16.member(LA(1))) && (_tokenSet_21.member(LA(2))))) {
				int _m198 = mark();
				synPredMatched198 = true;
				inputState.guessing++;
				try {
					{
					incOrDec();
					}
				}
				catch (RecognitionException pe) {
					synPredMatched198 = false;
				}
				rewind(_m198);
inputState.guessing--;
			}
			if ( synPredMatched198 ) {
				x=incOrDec();
			}
			else {
				boolean synPredMatched200 = false;
				if (((LA(1)==LPAREN) && (_tokenSet_52.member(LA(2))))) {
					int _m200 = mark();
					synPredMatched200 = true;
					inputState.guessing++;
					try {
						{
						match(LPAREN);
						primitive_type();
						}
					}
					catch (RecognitionException pe) {
						synPredMatched200 = false;
					}
					rewind(_m200);
inputState.guessing--;
				}
				if ( synPredMatched200 ) {
					x=castExpr();
				}
				else if ((LA(1)==BANG)) {
					b = LT(1);
					match(BANG);
					x=value_expr();
					if ( inputState.guessing==0 ) {
						x = new ExprUnary(getContext(b),
																		ExprUnary.UNOP_NOT, x);
					}
				}
				else if ((_tokenSet_53.member(LA(1))) && (_tokenSet_54.member(LA(2)))) {
					x=value_expr();
				}
				else {
					throw new NoViableAltException(LT(1), getFilename());
				}
				}
			}
			catch (RecognitionException ex) {
				if (inputState.guessing==0) {
					reportError(ex);
					recover(ex,_tokenSet_35);
				} else {
				  throw ex;
				}
			}
			return x;
		}
		
	public final Expression  castExpr() throws RecognitionException, TokenStreamException {
		Expression x;
		
		Token  l = null;
		Token  sq = null;
		x = null; Type t = null; Expression bound = null;
		
		try {      // for error handling
			l = LT(1);
			match(LPAREN);
			t=primitive_type();
			{
			_loop207:
			do {
				if ((LA(1)==LSQUARE)) {
					sq = LT(1);
					match(LSQUARE);
					bound=right_expr();
					if ( inputState.guessing==0 ) {
						t = new TypeArray(t, bound);
					}
					match(RSQUARE);
				}
				else {
					break _loop207;
				}
				
			} while (true);
			}
			match(RPAREN);
			x=value_expr();
			if ( inputState.guessing==0 ) {
				x = new ExprTypeCast(getContext(l), t, x);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_35);
			} else {
			  throw ex;
			}
		}
		return x;
	}
	
	public final Expression  value_expr() throws RecognitionException, TokenStreamException {
		Expression x;
		
		Token  m = null;
		x = null; boolean neg = false;
		
		try {      // for error handling
			{
			{
			switch ( LA(1)) {
			case MINUS:
			{
				m = LT(1);
				match(MINUS);
				if ( inputState.guessing==0 ) {
					neg = true;
				}
				break;
			}
			case TK_new:
			case TK_null:
			case TK_true:
			case TK_false:
			case LPAREN:
			case LCURLY:
			case NDVAL:
			case NDVAL2:
			case REGEN:
			case CHAR_LITERAL:
			case STRING_LITERAL:
			case HQUAN:
			case NUMBER:
			case ID:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			{
			x=minic_value_expr();
			}
			if ( inputState.guessing==0 ) {
				if (neg) x = new ExprUnary(getContext(m), ExprUnary.UNOP_NEG, x);
			}
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_35);
			} else {
			  throw ex;
			}
		}
		return x;
	}
	
	public final Expression  minic_value_expr() throws RecognitionException, TokenStreamException {
		Expression x;
		
		Token  field = null;
		Token  l = null;
		x = null; Vector<ExprArrayRange.RangeLen> rl;
		
		try {      // for error handling
			x=tminic_value_expr();
			{
			_loop214:
			do {
				switch ( LA(1)) {
				case DOT:
				{
					match(DOT);
					field = LT(1);
					match(ID);
					if ( inputState.guessing==0 ) {
						x = new ExprField(x, x, field.getText());
					}
					break;
				}
				case LSQUARE:
				{
					l = LT(1);
					match(LSQUARE);
					rl=array_range();
					if ( inputState.guessing==0 ) {
						x = new ExprArrayRange(x, x, rl);
					}
					match(RSQUARE);
					break;
				}
				default:
				{
					break _loop214;
				}
				}
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_35);
			} else {
			  throw ex;
			}
		}
		return x;
	}
	
	public final Expression  tminic_value_expr() throws RecognitionException, TokenStreamException {
		Expression x;
		
		Token  r = null;
		x = null;
		
		try {      // for error handling
			switch ( LA(1)) {
			case LPAREN:
			{
				match(LPAREN);
				x=right_expr();
				match(RPAREN);
				break;
			}
			case TK_new:
			{
				x=constructor_expr();
				break;
			}
			case TK_null:
			case TK_true:
			case TK_false:
			case NDVAL:
			case NDVAL2:
			case CHAR_LITERAL:
			case STRING_LITERAL:
			case HQUAN:
			case NUMBER:
			{
				x=constantExpr();
				break;
			}
			case LCURLY:
			{
				x=arr_initializer();
				break;
			}
			case REGEN:
			{
				r = LT(1);
				match(REGEN);
				if ( inputState.guessing==0 ) {
					x = new ExprRegen (getContext (r), r.getText ());
				}
				break;
			}
			default:
				boolean synPredMatched220 = false;
				if (((LA(1)==ID) && (LA(2)==LPAREN||LA(2)==AT))) {
					int _m220 = mark();
					synPredMatched220 = true;
					inputState.guessing++;
					try {
						{
						func_call();
						}
					}
					catch (RecognitionException pe) {
						synPredMatched220 = false;
					}
					rewind(_m220);
inputState.guessing--;
				}
				if ( synPredMatched220 ) {
					x=func_call();
				}
				else if ((LA(1)==ID) && (_tokenSet_37.member(LA(2)))) {
					x=var_expr();
				}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_37);
			} else {
			  throw ex;
			}
		}
		return x;
	}
	
	public final Expression  constructor_expr() throws RecognitionException, TokenStreamException {
		Expression x;
		
		Token  n = null;
		x = null; Type t; List l;
		
		try {      // for error handling
			n = LT(1);
			match(TK_new);
			t=data_type();
			l=constr_params();
			if ( inputState.guessing==0 ) {
				x = new ExprNew( getContext(n), t, l);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_38);
			} else {
			  throw ex;
			}
		}
		return x;
	}
	
	public final Expression  constantExpr() throws RecognitionException, TokenStreamException {
		Expression x;
		
		Token  h = null;
		Token  n = null;
		Token  c = null;
		Token  s = null;
		Token  t = null;
		Token  f = null;
		Token  t1 = null;
		Token  t2 = null;
		x = null; Expression n1=null, n2=null;
		
		try {      // for error handling
			switch ( LA(1)) {
			case HQUAN:
			{
				h = LT(1);
				match(HQUAN);
				if ( inputState.guessing==0 ) {
					String tmp = h.getText().substring(2);
								   Integer iti = new Integer(
						 (int ) ( ( Long.parseLong(tmp, 16) - (long) Integer.MIN_VALUE )
							  % ( (long)Integer.MAX_VALUE - (long) Integer.MIN_VALUE + 1)
							  + Integer.MIN_VALUE) );
									x = ExprConstant.createConstant(getContext(h), iti.toString() );
				}
				break;
			}
			case NUMBER:
			{
				n = LT(1);
				match(NUMBER);
				if ( inputState.guessing==0 ) {
					x = ExprConstant.createConstant(getContext(n), n.getText());
				}
				break;
			}
			case CHAR_LITERAL:
			{
				c = LT(1);
				match(CHAR_LITERAL);
				if ( inputState.guessing==0 ) {
					x = ExprConstChar.create(c.getText());
				}
				break;
			}
			case STRING_LITERAL:
			{
				s = LT(1);
				match(STRING_LITERAL);
				if ( inputState.guessing==0 ) {
					x = new ExprArrayInit(getContext(s), ExprConstChar.createFromString(s.getText()));
				}
				break;
			}
			case TK_true:
			{
				t = LT(1);
				match(TK_true);
				if ( inputState.guessing==0 ) {
					x = ExprConstInt.one;
				}
				break;
			}
			case TK_false:
			{
				f = LT(1);
				match(TK_false);
				if ( inputState.guessing==0 ) {
					x = ExprConstInt.zero;
				}
				break;
			}
			case TK_null:
			{
				match(TK_null);
				if ( inputState.guessing==0 ) {
					x = ExprNullPtr.nullPtr;
				}
				break;
			}
			case NDVAL:
			{
				t1 = LT(1);
				match(NDVAL);
				if ( inputState.guessing==0 ) {
					x = new ExprStar(getContext(t1));
				}
				break;
			}
			case NDVAL2:
			{
				t2 = LT(1);
				match(NDVAL2);
				{
				switch ( LA(1)) {
				case LPAREN:
				{
					match(LPAREN);
					n1=value_expr();
					{
					switch ( LA(1)) {
					case COMMA:
					{
						match(COMMA);
						n2=value_expr();
						break;
					}
					case RPAREN:
					{
						break;
					}
					default:
					{
						throw new NoViableAltException(LT(1), getFilename());
					}
					}
					}
					match(RPAREN);
					break;
				}
				case EOF:
				case TK_until:
				case TK_by:
				case RPAREN:
				case RCURLY:
				case LSQUARE:
				case RSQUARE:
				case PLUS:
				case MINUS:
				case STAR:
				case DIV:
				case MOD:
				case LOGIC_AND:
				case LOGIC_OR:
				case BITWISE_AND:
				case BITWISE_OR:
				case BITWISE_XOR:
				case EQUAL:
				case NOT_EQUAL:
				case LESS_THAN:
				case LESS_EQUAL:
				case MORE_THAN:
				case MORE_EQUAL:
				case QUESTION:
				case COLON:
				case SEMI:
				case COMMA:
				case DOT:
				case LSHIFT:
				case RSHIFT:
				case SELECT:
				case LESS_COLON:
				{
					break;
				}
				default:
				{
					throw new NoViableAltException(LT(1), getFilename());
				}
				}
				}
				if ( inputState.guessing==0 ) {
					if(n1 != null){
						Integer in1 = n1.getIValue();
						  if(n2 == null){            	  	
						  	x = new ExprStar(getContext(t2),in1);
						  }else{
						  	Integer in2 = n2.getIValue();
						  	x = new ExprStar(getContext(t2),in1, in2);
						  } 
						}else{
						  x = new ExprStar(getContext(t2)); 
						}
					
				}
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_37);
			} else {
			  throw ex;
			}
		}
		return x;
	}
	
	
	public static final String[] _tokenNames = {
		"<0>",
		"EOF",
		"<2>",
		"NULL_TREE_LOOKAHEAD",
		"\"atomic\"",
		"\"fork\"",
		"\"insert\"",
		"\"into\"",
		"\"loop\"",
		"\"repeat\"",
		"\"minrepeat\"",
		"\"new\"",
		"\"null\"",
		"\"reorder\"",
		"\"assume\"",
		"\"boolean\"",
		"\"float\"",
		"\"bit\"",
		"\"int\"",
		"\"void\"",
		"\"double\"",
		"\"fun\"",
		"\"char\"",
		"\"struct\"",
		"\"ref\"",
		"\"if\"",
		"\"else\"",
		"\"while\"",
		"\"for\"",
		"\"switch\"",
		"\"case\"",
		"\"default\"",
		"\"break\"",
		"\"do\"",
		"\"continue\"",
		"\"return\"",
		"\"true\"",
		"\"false\"",
		"\"parfor\"",
		"\"until\"",
		"\"by\"",
		"\"implements\"",
		"\"assert\"",
		"\"assert_max\"",
		"\"h_assert\"",
		"\"generator\"",
		"\"harness\"",
		"\"model\"",
		"\"global\"",
		"\"serial\"",
		"\"spmdfork\"",
		"\"stencil\"",
		"\"include\"",
		"\"pragma\"",
		"\"package\"",
		"ARROW",
		"LARROW",
		"WS",
		"LINERESET",
		"SL_COMMENT",
		"ML_COMMENT",
		"LPAREN",
		"RPAREN",
		"LCURLY",
		"RCURLY",
		"LSQUARE",
		"RSQUARE",
		"PLUS",
		"PLUS_EQUALS",
		"INCREMENT",
		"MINUS",
		"MINUS_EQUALS",
		"DECREMENT",
		"STAR",
		"STAR_EQUALS",
		"DIV",
		"DIV_EQUALS",
		"MOD",
		"LOGIC_AND",
		"LOGIC_OR",
		"BITWISE_AND",
		"BITWISE_OR",
		"BITWISE_XOR",
		"ASSIGN",
		"DEF_ASSIGN",
		"EQUAL",
		"NOT_EQUAL",
		"LESS_THAN",
		"LESS_EQUAL",
		"MORE_THAN",
		"MORE_EQUAL",
		"QUESTION",
		"COLON",
		"SEMI",
		"COMMA",
		"DOT",
		"BANG",
		"LSHIFT",
		"RSHIFT",
		"NDVAL",
		"NDVAL2",
		"SELECT",
		"NDANGELIC",
		"AT",
		"BACKSLASH",
		"LESS_COLON",
		"REGEN",
		"CHAR_LITERAL",
		"STRING_LITERAL",
		"ESC",
		"DIGIT",
		"HQUAN",
		"NUMBER",
		"an identifier",
		"TK_device",
		"TK_library",
		"TK_printfcn"
	};
	
	private static final long[] mk_tokenSet_0() {
		long[] data = { 3342515356794880L, 563499709235200L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_0 = new BitSet(mk_tokenSet_0());
	private static final long[] mk_tokenSet_1() {
		long[] data = { 3342515356794880L, 563499709235202L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_1 = new BitSet(mk_tokenSet_1());
	private static final long[] mk_tokenSet_2() {
		long[] data = { 281474985066496L, 562949953421312L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_2 = new BitSet(mk_tokenSet_2());
	private static final long[] mk_tokenSet_3() {
		long[] data = { 7831552L, 563499709235202L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_3 = new BitSet(mk_tokenSet_3());
	private static final long[] mk_tokenSet_4() {
		long[] data = { 2L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_4 = new BitSet(mk_tokenSet_4());
	private static final long[] mk_tokenSet_5() {
		long[] data = { 3342515356794880L, 562949953421312L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_5 = new BitSet(mk_tokenSet_5());
	private static final long[] mk_tokenSet_6() {
		long[] data = { 0L, 562949953421312L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_6 = new BitSet(mk_tokenSet_6());
	private static final long[] mk_tokenSet_7() {
		long[] data = { -6881504288846123150L, 567898292617505L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_7 = new BitSet(mk_tokenSet_7());
	private static final long[] mk_tokenSet_8() {
		long[] data = { 0L, 536870912L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_8 = new BitSet(mk_tokenSet_8());
	private static final long[] mk_tokenSet_9() {
		long[] data = { 34867712756793346L, 563499709235200L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_9 = new BitSet(mk_tokenSet_9());
	private static final long[] mk_tokenSet_10() {
		long[] data = { 4611687667694829570L, 2200902303749L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_10 = new BitSet(mk_tokenSet_10());
	private static final long[] mk_tokenSet_11() {
		long[] data = { 2305843009213693952L, 562949953421312L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_11 = new BitSet(mk_tokenSet_11());
	private static final long[] mk_tokenSet_12() {
		long[] data = { -6917528821482645504L, 1016330996154721L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_12 = new BitSet(mk_tokenSet_12());
	private static final long[] mk_tokenSet_13() {
		long[] data = { -6917528821482645504L, 1016330996154720L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_13 = new BitSet(mk_tokenSet_13());
	private static final long[] mk_tokenSet_14() {
		long[] data = { -6917247346497579008L, 1017047985679211L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_14 = new BitSet(mk_tokenSet_14());
	private static final long[] mk_tokenSet_15() {
		long[] data = { 0L, 1610612737L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_15 = new BitSet(mk_tokenSet_15());
	private static final long[] mk_tokenSet_16() {
		long[] data = { 2305843009213696000L, 567347999932704L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_16 = new BitSet(mk_tokenSet_16());
	private static final long[] mk_tokenSet_17() {
		long[] data = { -6917247346497579008L, 1016882899981810L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_17 = new BitSet(mk_tokenSet_17());
	private static final long[] mk_tokenSet_18() {
		long[] data = { -6913029486246121616L, 567898292617505L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_18 = new BitSet(mk_tokenSet_18());
	private static final long[] mk_tokenSet_19() {
		long[] data = { -2305561328070191104L, 1017046375066475L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_19 = new BitSet(mk_tokenSet_19());
	private static final long[] mk_tokenSet_20() {
		long[] data = { -6913029486313230480L, 567898292617504L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_20 = new BitSet(mk_tokenSet_20());
	private static final long[] mk_tokenSet_21() {
		long[] data = { -6917247346497579008L, 1016333143638370L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_21 = new BitSet(mk_tokenSet_21());
	private static final long[] mk_tokenSet_22() {
		long[] data = { 2305843009213696000L, 567347999932416L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_22 = new BitSet(mk_tokenSet_22());
	private static final long[] mk_tokenSet_23() {
		long[] data = { -6917247346497579008L, 1016333144167922L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_23 = new BitSet(mk_tokenSet_23());
	private static final long[] mk_tokenSet_24() {
		long[] data = { 4611686018427387904L, 536870912L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_24 = new BitSet(mk_tokenSet_24());
	private static final long[] mk_tokenSet_25() {
		long[] data = { -6881504082687688718L, 1017982948480499L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_25 = new BitSet(mk_tokenSet_25());
	private static final long[] mk_tokenSet_26() {
		long[] data = { 4683745261732757506L, 2366525931519L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_26 = new BitSet(mk_tokenSet_26());
	private static final long[] mk_tokenSet_27() {
		long[] data = { 4611686018427387904L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_27 = new BitSet(mk_tokenSet_27());
	private static final long[] mk_tokenSet_28() {
		long[] data = { 6917529027641081856L, 562949953421314L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_28 = new BitSet(mk_tokenSet_28());
	private static final long[] mk_tokenSet_29() {
		long[] data = { -2305561328070191104L, 1019246740499311L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_29 = new BitSet(mk_tokenSet_29());
	private static final long[] mk_tokenSet_30() {
		long[] data = { 4611686018427387904L, 2200365432836L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_30 = new BitSet(mk_tokenSet_30());
	private static final long[] mk_tokenSet_31() {
		long[] data = { 3342515373572096L, 563499709235201L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_31 = new BitSet(mk_tokenSet_31());
	private static final long[] mk_tokenSet_32() {
		long[] data = { -9223369837831520256L, 536870912L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_32 = new BitSet(mk_tokenSet_32());
	private static final long[] mk_tokenSet_33() {
		long[] data = { 4611967493429231616L, 562949953421312L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_33 = new BitSet(mk_tokenSet_33());
	private static final long[] mk_tokenSet_34() {
		long[] data = { 4611686018427387904L, 1610612736L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_34 = new BitSet(mk_tokenSet_34());
	private static final long[] mk_tokenSet_35() {
		long[] data = { 4611687667694829570L, 2364377918029L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_35 = new BitSet(mk_tokenSet_35());
	private static final long[] mk_tokenSet_36() {
		long[] data = { 4611687667694829570L, 2364378447869L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_36 = new BitSet(mk_tokenSet_36());
	private static final long[] mk_tokenSet_37() {
		long[] data = { 4611687667694829570L, 2366525401679L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_37 = new BitSet(mk_tokenSet_37());
	private static final long[] mk_tokenSet_38() {
		long[] data = { 4611687667694829570L, 2366525931519L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_38 = new BitSet(mk_tokenSet_38());
	private static final long[] mk_tokenSet_39() {
		long[] data = { 4611686018427387904L, 1073741824L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_39 = new BitSet(mk_tokenSet_39());
	private static final long[] mk_tokenSet_40() {
		long[] data = { 0L, 4L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_40 = new BitSet(mk_tokenSet_40());
	private static final long[] mk_tokenSet_41() {
		long[] data = { -6917247346497579006L, 1017046375066475L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_41 = new BitSet(mk_tokenSet_41());
	private static final long[] mk_tokenSet_42() {
		long[] data = { 4611687667694829570L, 2201036521477L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_42 = new BitSet(mk_tokenSet_42());
	private static final long[] mk_tokenSet_43() {
		long[] data = { 4611687667694829570L, 2201036554245L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_43 = new BitSet(mk_tokenSet_43());
	private static final long[] mk_tokenSet_44() {
		long[] data = { 4611687667694829570L, 2201036570629L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_44 = new BitSet(mk_tokenSet_44());
	private static final long[] mk_tokenSet_45() {
		long[] data = { 4611687667694829570L, 2201036701701L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_45 = new BitSet(mk_tokenSet_45());
	private static final long[] mk_tokenSet_46() {
		long[] data = { 4611687667694829570L, 2201036963845L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_46 = new BitSet(mk_tokenSet_46());
	private static final long[] mk_tokenSet_47() {
		long[] data = { 4611687667694829570L, 2201037029381L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_47 = new BitSet(mk_tokenSet_47());
	private static final long[] mk_tokenSet_48() {
		long[] data = { 4611687667694829570L, 2201043320837L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_48 = new BitSet(mk_tokenSet_48());
	private static final long[] mk_tokenSet_49() {
		long[] data = { 4611687667694829570L, 2201169149957L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_49 = new BitSet(mk_tokenSet_49());
	private static final long[] mk_tokenSet_50() {
		long[] data = { 4611687667694829570L, 2226938953733L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_50 = new BitSet(mk_tokenSet_50());
	private static final long[] mk_tokenSet_51() {
		long[] data = { 4611687667694829570L, 2364377907277L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_51 = new BitSet(mk_tokenSet_51());
	private static final long[] mk_tokenSet_52() {
		long[] data = { 7831552L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_52 = new BitSet(mk_tokenSet_52());
	private static final long[] mk_tokenSet_53() {
		long[] data = { -6917528821482645504L, 1016051823280192L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_53 = new BitSet(mk_tokenSet_53());
	private static final long[] mk_tokenSet_54() {
		long[] data = { -2305559678802749438L, 1019247277370223L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_54 = new BitSet(mk_tokenSet_54());
	
	}
