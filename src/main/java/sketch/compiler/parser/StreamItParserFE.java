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

import sketch.compiler.Directive;
import sketch.compiler.ast.core.FEContext;
import sketch.compiler.ast.core.FieldDecl;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.SplitterJoiner;
import sketch.compiler.ast.core.StreamSpec;
import sketch.compiler.ast.core.StreamType;
import sketch.compiler.ast.core.exprs.*;
import sketch.compiler.ast.core.stmts.*;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.ast.core.typs.TypePortal;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.ast.core.typs.TypeStruct;
import sketch.compiler.ast.core.typs.TypeStructRef;
import sketch.compiler.ast.promela.stmts.StmtFork;
import sketch.compiler.main.seq.SequentialSketchOptions;
import sketch.compiler.passes.streamit_old.SJDuplicate;
import sketch.compiler.passes.streamit_old.SJRoundRobin;
import sketch.compiler.passes.streamit_old.SJWeightedRR;

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

    public void handleInclude(String name, List funcs, List vars, List structs)
    {
        try {
            List<String> incList = Arrays.asList(
                    SequentialSketchOptions.getSingleton().feOpts.inc);
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
		assert p.getStreams().size() == 1;

		StreamSpec ss = (StreamSpec) p.getStreams().get(0);
		funcs.addAll(ss.getFuncs());
		vars.addAll(ss.getVars());
		structs.addAll(p.getStructs());
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
  this(tokenBuf,1);
}

protected StreamItParserFE(TokenStream lexer, int k) {
  super(lexer,k);
  tokenNames = _tokenNames;
}

public StreamItParserFE(TokenStream lexer) {
  this(lexer,1);
}

public StreamItParserFE(ParserSharedInputState state) {
  super(state,1);
  tokenNames = _tokenNames;
}

	public final Program  program() throws RecognitionException, TokenStreamException {
		Program p;
		
		p = null; List vars = new ArrayList();  List streams = new ArrayList();
			List funcs=new ArrayList(); Function f;
		FieldDecl fd; TypeStruct ts; List<TypeStruct> structs = new ArrayList<TypeStruct>();
		String file = null;
		
		
		try {      // for error handling
			{
			_loop8:
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
						handleInclude (file, funcs, vars, structs);
					}
					break;
				}
				case TK_pragma:
				{
					pragma_stmt();
					break;
				}
				default:
					boolean synPredMatched5 = false;
					if (((_tokenSet_0.member(LA(1))))) {
						int _m5 = mark();
						synPredMatched5 = true;
						inputState.guessing++;
						try {
							{
							{
							switch ( LA(1)) {
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
							case TK_boolean:
							case TK_float:
							case TK_bit:
							case TK_int:
							case TK_void:
							case TK_double:
							case TK_complex:
							case ID:
							case TK_portal:
							{
								break;
							}
							default:
							{
								throw new NoViableAltException(LT(1), getFilename());
							}
							}
							}
							return_type();
							match(ID);
							match(LPAREN);
							}
						}
						catch (RecognitionException pe) {
							synPredMatched5 = false;
						}
						rewind(_m5);
inputState.guessing--;
					}
					if ( synPredMatched5 ) {
						f=function_decl();
						if ( inputState.guessing==0 ) {
							funcs.add(f);
						}
					}
					else {
						boolean synPredMatched7 = false;
						if (((_tokenSet_0.member(LA(1))))) {
							int _m7 = mark();
							synPredMatched7 = true;
							inputState.guessing++;
							try {
								{
								return_type();
								match(ID);
								match(LPAREN);
								}
							}
							catch (RecognitionException pe) {
								synPredMatched7 = false;
							}
							rewind(_m7);
inputState.guessing--;
						}
						if ( synPredMatched7 ) {
							f=function_decl();
							if ( inputState.guessing==0 ) {
								funcs.add(f);
							}
						}
						else if ((_tokenSet_1.member(LA(1)))) {
							fd=field_decl();
							match(SEMI);
							if ( inputState.guessing==0 ) {
								vars.add(fd);
							}
						}
					else {
						break _loop8;
					}
					}}
				} while (true);
				}
				match(Token.EOF_TYPE);
				if ( inputState.guessing==0 ) {
					
								 StreamSpec ss=new StreamSpec((FEContext) null, StreamSpec.STREAM_FILTER,
									new StreamType((FEContext) null, TypePrimitive.bittype, TypePrimitive.bittype), "MAIN",
									Collections.EMPTY_LIST, vars, funcs);
									streams.add(ss);
									 if (!hasError) p = new Program(null, Collections.singletonList(ss), structs);
				}
			}
			catch (RecognitionException ex) {
				if (inputState.guessing==0) {
					reportError(ex);
					recover(ex,_tokenSet_2);
				} else {
				  throw ex;
				}
			}
			return p;
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
				recover(ex,_tokenSet_3);
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
		Type rt; List l; StmtBlock s; f = null; boolean isHarness=false; boolean isGenerator=false;
		
		try {      // for error handling
			{
			switch ( LA(1)) {
			case TK_harness:
			{
				match(TK_harness);
				if ( inputState.guessing==0 ) {
					isHarness=true;
				}
				break;
			}
			case TK_boolean:
			case TK_float:
			case TK_bit:
			case TK_int:
			case TK_void:
			case TK_double:
			case TK_complex:
			case TK_generator:
			case ID:
			case TK_portal:
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
			case TK_generator:
			{
				match(TK_generator);
				if ( inputState.guessing==0 ) {
					isGenerator=true;
				}
				break;
			}
			case TK_boolean:
			case TK_float:
			case TK_bit:
			case TK_int:
			case TK_void:
			case TK_double:
			case TK_complex:
			case ID:
			case TK_portal:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
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
								if (isGenerator) {
					f = Function.newHelper(getContext(id), id.getText(), rt, l,
					impl==null ? null : impl.getText(), s);
								} else if (isHarness) {
					assert impl == null;
									f = Function.newHarnessMain(getContext(id), id.getText(), rt, l, s);
								} else {
					f = Function.newStatic(getContext(id), id.getText(), rt, l,
					impl==null ? null : impl.getText(), s);
								}
						
				}
				break;
			}
			case SEMI:
			{
				match(SEMI);
				if ( inputState.guessing==0 ) {
					f = Function.newUninterp(getContext(id),id.getText(), rt, l);
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
				recover(ex,_tokenSet_4);
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
			_loop16:
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
					break _loop16;
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
				recover(ex,_tokenSet_5);
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
			List types = new ArrayList();
		
		try {      // for error handling
			t = LT(1);
			match(TK_struct);
			id = LT(1);
			match(ID);
			match(LCURLY);
			{
			_loop205:
			do {
				if ((_tokenSet_6.member(LA(1)))) {
					p=param_decl();
					match(SEMI);
					if ( inputState.guessing==0 ) {
						names.add(p.getName()); types.add(p.getType());
					}
				}
				else {
					break _loop205;
				}
				
			} while (true);
			}
			match(RCURLY);
			if ( inputState.guessing==0 ) {
				ts = new TypeStruct(getContext(t), id.getText(), names, types);
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
				recover(ex,_tokenSet_4);
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
				recover(ex,_tokenSet_4);
			} else {
			  throw ex;
			}
		}
	}
	
	public final Type  data_type() throws RecognitionException, TokenStreamException {
		Type t;
		
		Token  id = null;
		Token  l = null;
		Token  pn = null;
		t = null; Expression x;
		
		try {      // for error handling
			switch ( LA(1)) {
			case TK_boolean:
			case TK_float:
			case TK_bit:
			case TK_int:
			case TK_double:
			case TK_complex:
			case ID:
			{
				{
				switch ( LA(1)) {
				case TK_boolean:
				case TK_float:
				case TK_bit:
				case TK_int:
				case TK_double:
				case TK_complex:
				{
					t=primitive_type();
					break;
				}
				case ID:
				{
					id = LT(1);
					match(ID);
					if ( inputState.guessing==0 ) {
						t = new TypeStructRef(id.getText());
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
				_loop43:
				do {
					if ((LA(1)==LSQUARE)) {
						l = LT(1);
						match(LSQUARE);
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
						case REGEN:
						case CHAR_LITERAL:
						case STRING_LITERAL:
						case HQUAN:
						case NUMBER:
						case ID:
						case TK_pi:
						{
							x=right_expr();
							if ( inputState.guessing==0 ) {
								t = new TypeArray(t, x);
							}
							break;
						}
						case RSQUARE:
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
						match(RSQUARE);
					}
					else {
						break _loop43;
					}
					
				} while (true);
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
			case TK_portal:
			{
				match(TK_portal);
				match(LESS_THAN);
				pn = LT(1);
				match(ID);
				match(MORE_THAN);
				if ( inputState.guessing==0 ) {
					t = new TypePortal(pn.getText());
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
				recover(ex,_tokenSet_7);
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
			boolean synPredMatched118 = false;
			if (((LA(1)==LCURLY))) {
				int _m118 = mark();
				synPredMatched118 = true;
				inputState.guessing++;
				try {
					{
					arr_initializer();
					}
				}
				catch (RecognitionException pe) {
					synPredMatched118 = false;
				}
				rewind(_m118);
inputState.guessing--;
			}
			if ( synPredMatched118 ) {
				x=arr_initializer();
			}
			else if ((_tokenSet_8.member(LA(1)))) {
				x=right_expr();
			}
			else {
				throw new NoViableAltException(LT(1), getFilename());
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
		return x;
	}
	
	public final StreamType  stream_type_decl() throws RecognitionException, TokenStreamException {
		StreamType st;
		
		Token  t = null;
		st = null; Type in, out;
		
		try {      // for error handling
			in=data_type();
			t = LT(1);
			match(ARROW);
			out=data_type();
			if ( inputState.guessing==0 ) {
				st = new StreamType(getContext(t), in, out);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_2);
			} else {
			  throw ex;
			}
		}
		return st;
	}
	
	public final StreamSpec  struct_stream_decl(
		StreamType st
	) throws RecognitionException, TokenStreamException {
		StreamSpec ss;
		
		Token  id = null;
		ss = null; int type = 0;
			List params = Collections.EMPTY_LIST; Statement body;
		
		try {      // for error handling
			{
			switch ( LA(1)) {
			case TK_pipeline:
			{
				match(TK_pipeline);
				if ( inputState.guessing==0 ) {
					type = StreamSpec.STREAM_PIPELINE;
				}
				break;
			}
			case TK_splitjoin:
			{
				match(TK_splitjoin);
				if ( inputState.guessing==0 ) {
					type = StreamSpec.STREAM_SPLITJOIN;
				}
				break;
			}
			case TK_feedbackloop:
			{
				match(TK_feedbackloop);
				if ( inputState.guessing==0 ) {
					type = StreamSpec.STREAM_FEEDBACKLOOP;
				}
				break;
			}
			case TK_sbox:
			{
				match(TK_sbox);
				if ( inputState.guessing==0 ) {
					type = StreamSpec.STREAM_TABLE;
				}
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			id = LT(1);
			match(ID);
			{
			switch ( LA(1)) {
			case LPAREN:
			{
				params=param_decl_list();
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
			body=block();
			if ( inputState.guessing==0 ) {
				ss = new StreamSpec(st.getContext(), type, st, id.getText(),
								params, body);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_2);
			} else {
			  throw ex;
			}
		}
		return ss;
	}
	
	public final List  param_decl_list() throws RecognitionException, TokenStreamException {
		List l;
		
		l = new ArrayList(); Parameter p;
		
		try {      // for error handling
			match(LPAREN);
			{
			switch ( LA(1)) {
			case TK_boolean:
			case TK_float:
			case TK_bit:
			case TK_int:
			case TK_void:
			case TK_double:
			case TK_complex:
			case TK_ref:
			case ID:
			case TK_portal:
			{
				p=param_decl();
				if ( inputState.guessing==0 ) {
					l.add(p);
				}
				{
				_loop60:
				do {
					if ((LA(1)==COMMA)) {
						match(COMMA);
						p=param_decl();
						if ( inputState.guessing==0 ) {
							l.add(p);
						}
					}
					else {
						break _loop60;
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
				recover(ex,_tokenSet_10);
			} else {
			  throw ex;
			}
		}
		return l;
	}
	
	public final StmtBlock  block() throws RecognitionException, TokenStreamException {
		StmtBlock sb;
		
		Token  t = null;
		sb=null; Statement s; List l = new ArrayList();
		
		try {      // for error handling
			t = LT(1);
			match(LCURLY);
			{
			_loop65:
			do {
				if ((_tokenSet_11.member(LA(1)))) {
					s=statement();
					if ( inputState.guessing==0 ) {
						l.add(s);
					}
				}
				else {
					break _loop65;
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
				recover(ex,_tokenSet_12);
			} else {
			  throw ex;
			}
		}
		return sb;
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
			case TK_fork:
			{
				s=fork_statement();
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
				boolean synPredMatched23 = false;
				if (((_tokenSet_1.member(LA(1))))) {
					int _m23 = mark();
					synPredMatched23 = true;
					inputState.guessing++;
					try {
						{
						data_type();
						match(ID);
						}
					}
					catch (RecognitionException pe) {
						synPredMatched23 = false;
					}
					rewind(_m23);
inputState.guessing--;
				}
				if ( synPredMatched23 ) {
					s=variable_decl();
					match(SEMI);
				}
				else {
					boolean synPredMatched25 = false;
					if (((_tokenSet_13.member(LA(1))))) {
						int _m25 = mark();
						synPredMatched25 = true;
						inputState.guessing++;
						try {
							{
							expr_statement();
							}
						}
						catch (RecognitionException pe) {
							synPredMatched25 = false;
						}
						rewind(_m25);
inputState.guessing--;
					}
					if ( synPredMatched25 ) {
						s=expr_statement();
						match(SEMI);
					}
				else {
					throw new NoViableAltException(LT(1), getFilename());
				}
				}}
			}
			catch (RecognitionException ex) {
				if (inputState.guessing==0) {
					reportError(ex);
					recover(ex,_tokenSet_14);
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
				recover(ex,_tokenSet_14);
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
				recover(ex,_tokenSet_14);
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
			_loop68:
			do {
				if ((_tokenSet_11.member(LA(1)))) {
					s=statement();
					if ( inputState.guessing==0 ) {
						insert.add(s);
					}
				}
				else {
					break _loop68;
				}
				
			} while (true);
			}
			match(RCURLY);
			match(TK_into);
			match(LCURLY);
			{
			_loop70:
			do {
				if ((_tokenSet_11.member(LA(1)))) {
					s=statement();
					if ( inputState.guessing==0 ) {
						into.add(s);
					}
				}
				else {
					break _loop70;
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
				recover(ex,_tokenSet_14);
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
			_loop73:
			do {
				if ((_tokenSet_11.member(LA(1)))) {
					s=statement();
					if ( inputState.guessing==0 ) {
						l.add(s);
					}
				}
				else {
					break _loop73;
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
				recover(ex,_tokenSet_14);
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
				recover(ex,_tokenSet_14);
			} else {
			  throw ex;
			}
		}
		return ab;
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
			_loop49:
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
					break _loop49;
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
				recover(ex,_tokenSet_5);
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
			boolean synPredMatched101 = false;
			if (((_tokenSet_13.member(LA(1))))) {
				int _m101 = mark();
				synPredMatched101 = true;
				inputState.guessing++;
				try {
					{
					incOrDec();
					}
				}
				catch (RecognitionException pe) {
					synPredMatched101 = false;
				}
				rewind(_m101);
inputState.guessing--;
			}
			if ( synPredMatched101 ) {
				x=incOrDec();
				if ( inputState.guessing==0 ) {
					s = new StmtExpr(x);
				}
			}
			else {
				boolean synPredMatched104 = false;
				if (((_tokenSet_15.member(LA(1))))) {
					int _m104 = mark();
					synPredMatched104 = true;
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
						synPredMatched104 = false;
					}
					rewind(_m104);
inputState.guessing--;
				}
				if ( synPredMatched104 ) {
					s=assign_expr();
				}
				else {
					boolean synPredMatched106 = false;
					if (((LA(1)==ID))) {
						int _m106 = mark();
						synPredMatched106 = true;
						inputState.guessing++;
						try {
							{
							match(ID);
							match(LPAREN);
							}
						}
						catch (RecognitionException pe) {
							synPredMatched106 = false;
						}
						rewind(_m106);
inputState.guessing--;
					}
					if ( synPredMatched106 ) {
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
						recover(ex,_tokenSet_16);
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
			boolean synPredMatched85 = false;
			if (((LA(1)==TK_else))) {
				int _m85 = mark();
				synPredMatched85 = true;
				inputState.guessing++;
				try {
					{
					match(TK_else);
					}
				}
				catch (RecognitionException pe) {
					synPredMatched85 = false;
				}
				rewind(_m85);
inputState.guessing--;
			}
			if ( synPredMatched85 ) {
				{
				match(TK_else);
				f=pseudo_block();
				}
			}
			else if ((_tokenSet_14.member(LA(1)))) {
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
				recover(ex,_tokenSet_14);
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
				recover(ex,_tokenSet_14);
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
				recover(ex,_tokenSet_5);
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
			case REGEN:
			case CHAR_LITERAL:
			case STRING_LITERAL:
			case HQUAN:
			case NUMBER:
			case ID:
			case TK_pi:
			{
				x=right_expr();
				break;
			}
			case SEMI:
			{
				if ( inputState.guessing==0 ) {
					x = new ExprConstBoolean(getContext(t), true);
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
				s = new StmtFor(getContext(t), a, x, b, c);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_14);
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
				recover(ex,_tokenSet_5);
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
			case REGEN:
			case CHAR_LITERAL:
			case STRING_LITERAL:
			case HQUAN:
			case NUMBER:
			case ID:
			case TK_pi:
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
				recover(ex,_tokenSet_5);
			} else {
			  throw ex;
			}
		}
		return s;
	}
	
	public final Expression  right_expr() throws RecognitionException, TokenStreamException {
		Expression x;
		
		x = null;
		
		try {      // for error handling
			x=ternaryExpr();
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_17);
			} else {
			  throw ex;
			}
		}
		return x;
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
				recover(ex,_tokenSet_14);
			} else {
			  throw ex;
			}
		}
		return sb;
	}
	
	public final Statement  minrepeat_statement() throws RecognitionException, TokenStreamException {
		Statement s;
		
		Token  t2 = null;
		s = null; Statement b; Token x=null;
		
		try {      // for error handling
			{
			t2 = LT(1);
			match(TK_minrepeat);
			if ( inputState.guessing==0 ) {
				x=t2;
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
				recover(ex,_tokenSet_2);
			} else {
			  throw ex;
			}
		}
		return s;
	}
	
	public final SplitterJoiner  splitter_or_joiner() throws RecognitionException, TokenStreamException {
		SplitterJoiner sj;
		
		Token  tr = null;
		Token  td = null;
		Token  tag = null;
		sj = null; Expression x; List l;
		
		try {      // for error handling
			switch ( LA(1)) {
			case TK_roundrobin:
			{
				tr = LT(1);
				match(TK_roundrobin);
				{
				boolean synPredMatched34 = false;
				if (((LA(1)==LPAREN))) {
					int _m34 = mark();
					synPredMatched34 = true;
					inputState.guessing++;
					try {
						{
						match(LPAREN);
						match(RPAREN);
						}
					}
					catch (RecognitionException pe) {
						synPredMatched34 = false;
					}
					rewind(_m34);
inputState.guessing--;
				}
				if ( synPredMatched34 ) {
					match(LPAREN);
					match(RPAREN);
					if ( inputState.guessing==0 ) {
						sj = new SJRoundRobin(getContext(tr));
					}
				}
				else {
					boolean synPredMatched36 = false;
					if (((LA(1)==LPAREN))) {
						int _m36 = mark();
						synPredMatched36 = true;
						inputState.guessing++;
						try {
							{
							match(LPAREN);
							right_expr();
							match(RPAREN);
							}
						}
						catch (RecognitionException pe) {
							synPredMatched36 = false;
						}
						rewind(_m36);
inputState.guessing--;
					}
					if ( synPredMatched36 ) {
						match(LPAREN);
						x=right_expr();
						match(RPAREN);
						if ( inputState.guessing==0 ) {
							sj = new SJRoundRobin(getContext(tr), x);
						}
					}
					else if ((LA(1)==LPAREN)) {
						l=func_call_params();
						if ( inputState.guessing==0 ) {
							sj = new SJWeightedRR(getContext(tr), l);
						}
					}
					else if ((LA(1)==EOF)) {
						if ( inputState.guessing==0 ) {
							sj = new SJRoundRobin(getContext(tr));
						}
					}
					else {
						throw new NoViableAltException(LT(1), getFilename());
					}
					}
					}
					break;
				}
				case TK_duplicate:
				{
					td = LT(1);
					match(TK_duplicate);
					{
					switch ( LA(1)) {
					case LPAREN:
					{
						match(LPAREN);
						match(RPAREN);
						break;
					}
					case EOF:
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
						sj = new SJDuplicate(getContext(td));
					}
					break;
				}
				case ID:
				{
					tag = LT(1);
					match(ID);
					{
					switch ( LA(1)) {
					case LPAREN:
					{
						match(LPAREN);
						match(RPAREN);
						break;
					}
					case EOF:
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
						
									if( tag.getText().equals("xor") ){
										sj = new SJDuplicate(getContext(tag), SJDuplicate.XOR );
									}else if( tag.getText().equals("or") ){
										sj = new SJDuplicate(getContext(tag), SJDuplicate.OR );
									}else if( tag.getText().equals("and") ){
										sj = new SJDuplicate(getContext(tag), SJDuplicate.AND );
									}else{
										assert false: tag.getText()+ " is not a valid splitter";
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
					recover(ex,_tokenSet_2);
				} else {
				  throw ex;
				}
			}
			return sj;
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
			case REGEN:
			case CHAR_LITERAL:
			case STRING_LITERAL:
			case HQUAN:
			case NUMBER:
			case ID:
			case TK_pi:
			{
				x=right_expr();
				if ( inputState.guessing==0 ) {
					l.add(x);
				}
				{
				_loop113:
				do {
					if ((LA(1)==COMMA)) {
						match(COMMA);
						x=right_expr();
						if ( inputState.guessing==0 ) {
							l.add(x);
						}
					}
					else {
						break _loop113;
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
				recover(ex,_tokenSet_18);
			} else {
			  throw ex;
			}
		}
		return l;
	}
	
	public final Type  primitive_type() throws RecognitionException, TokenStreamException {
		Type t;
		
		t = null;
		
		try {      // for error handling
			switch ( LA(1)) {
			case TK_boolean:
			{
				match(TK_boolean);
				if ( inputState.guessing==0 ) {
					t = TypePrimitive.booltype;
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
			case TK_complex:
			{
				match(TK_complex);
				if ( inputState.guessing==0 ) {
					t = TypePrimitive.cplxtype;
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
				recover(ex,_tokenSet_19);
			} else {
			  throw ex;
			}
		}
		return t;
	}
	
	public final Function  handler_decl() throws RecognitionException, TokenStreamException {
		Function f;
		
		Token  id = null;
		List l; Statement s; f = null;
		Type t = TypePrimitive.voidtype;
		int cls = Function.FUNC_HANDLER;
		
		try {      // for error handling
			match(TK_handler);
			id = LT(1);
			match(ID);
			l=param_decl_list();
			s=block();
			if ( inputState.guessing==0 ) {
				f = new Function(getContext(id), cls, id.getText(), t, l, s);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_2);
			} else {
			  throw ex;
			}
		}
		return f;
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
			case TK_complex:
			case ID:
			case TK_portal:
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
				p = new Parameter(t, id.getText(), isRef? Parameter.REF : Parameter.IN);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_20);
			} else {
			  throw ex;
			}
		}
		return p;
	}
	
	public final Statement  for_init_statement() throws RecognitionException, TokenStreamException {
		Statement s;
		
		Token  t = null;
		s = null;
		
		try {      // for error handling
			boolean synPredMatched93 = false;
			if (((_tokenSet_1.member(LA(1))))) {
				int _m93 = mark();
				synPredMatched93 = true;
				inputState.guessing++;
				try {
					{
					variable_decl();
					}
				}
				catch (RecognitionException pe) {
					synPredMatched93 = false;
				}
				rewind(_m93);
inputState.guessing--;
			}
			if ( synPredMatched93 ) {
				s=variable_decl();
			}
			else {
				boolean synPredMatched95 = false;
				if (((_tokenSet_13.member(LA(1))))) {
					int _m95 = mark();
					synPredMatched95 = true;
					inputState.guessing++;
					try {
						{
						expr_statement();
						}
					}
					catch (RecognitionException pe) {
						synPredMatched95 = false;
					}
					rewind(_m95);
inputState.guessing--;
				}
				if ( synPredMatched95 ) {
					s=expr_statement();
				}
				else {
					boolean synPredMatched97 = false;
					if (((LA(1)==SEMI))) {
						int _m97 = mark();
						synPredMatched97 = true;
						inputState.guessing++;
						try {
							{
							match(SEMI);
							}
						}
						catch (RecognitionException pe) {
							synPredMatched97 = false;
						}
						rewind(_m97);
inputState.guessing--;
					}
					if ( synPredMatched97 ) {
						if ( inputState.guessing==0 ) {
							s = new StmtEmpty(getContext(t));
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
						recover(ex,_tokenSet_5);
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
				recover(ex,_tokenSet_21);
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
				recover(ex,_tokenSet_22);
			} else {
			  throw ex;
			}
		}
		return x;
	}
	
	public final Expression  left_expr() throws RecognitionException, TokenStreamException {
		Expression x;
		
		x = null;
		
		try {      // for error handling
			x=minic_value_exprnofo();
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_23);
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
				recover(ex,_tokenSet_16);
			} else {
			  throw ex;
			}
		}
		return s;
	}
	
	public final Expression  func_call() throws RecognitionException, TokenStreamException {
		Expression x;
		
		Token  name = null;
		x = null; List l;
		
		try {      // for error handling
			name = LT(1);
			match(ID);
			l=func_call_params();
			if ( inputState.guessing==0 ) {
				x = new ExprFunCall(getContext(name), name.getText(), l);
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
		return x;
	}
	
	public final Expression  minic_value_exprnofo() throws RecognitionException, TokenStreamException {
		Expression x;
		
		Token  field = null;
		Token  l = null;
		x = null; List rlist;
		
		try {      // for error handling
			x=uminic_value_expr();
			{
			_loop181:
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
					rlist=array_range_list();
					if ( inputState.guessing==0 ) {
						x = new ExprArrayRange(x, rlist);
					}
					match(RSQUARE);
					break;
				}
				default:
				{
					break _loop181;
				}
				}
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_23);
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
			case RPAREN:
			case RCURLY:
			case RSQUARE:
			case COLON:
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
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_17);
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
			case REGEN:
			case CHAR_LITERAL:
			case STRING_LITERAL:
			case HQUAN:
			case NUMBER:
			case ID:
			case TK_pi:
			{
				y=var_initializer();
				if ( inputState.guessing==0 ) {
					l.add(y);
				}
				{
				_loop122:
				do {
					if ((LA(1)==COMMA)) {
						match(COMMA);
						y=var_initializer();
						if ( inputState.guessing==0 ) {
							l.add(y);
						}
					}
					else {
						break _loop122;
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
				recover(ex,_tokenSet_25);
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
			_loop127:
			do {
				if ((LA(1)==LOGIC_OR)) {
					match(LOGIC_OR);
					r=logicAndExpr();
					if ( inputState.guessing==0 ) {
						x = new ExprBinary(ExprBinary.BINOP_OR, x, r);
					}
				}
				else {
					break _loop127;
				}
				
			} while (true);
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
	
	public final Expression  logicAndExpr() throws RecognitionException, TokenStreamException {
		Expression x;
		
		x = null; Expression r;
		
		try {      // for error handling
			x=bitwiseOrExpr();
			{
			_loop130:
			do {
				if ((LA(1)==LOGIC_AND)) {
					match(LOGIC_AND);
					r=bitwiseOrExpr();
					if ( inputState.guessing==0 ) {
						x = new ExprBinary(ExprBinary.BINOP_AND, x, r);
					}
				}
				else {
					break _loop130;
				}
				
			} while (true);
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
		return x;
	}
	
	public final Expression  bitwiseOrExpr() throws RecognitionException, TokenStreamException {
		Expression x;
		
		x = null; Expression r;
		
		try {      // for error handling
			x=bitwiseXorExpr();
			{
			_loop133:
			do {
				if ((LA(1)==BITWISE_OR)) {
					match(BITWISE_OR);
					r=bitwiseXorExpr();
					if ( inputState.guessing==0 ) {
						x = new ExprBinary(ExprBinary.BINOP_BOR, x, r);
					}
				}
				else {
					break _loop133;
				}
				
			} while (true);
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
		return x;
	}
	
	public final Expression  bitwiseXorExpr() throws RecognitionException, TokenStreamException {
		Expression x;
		
		x = null; Expression r;
		
		try {      // for error handling
			x=bitwiseAndExpr();
			{
			_loop136:
			do {
				if ((LA(1)==BITWISE_XOR)) {
					match(BITWISE_XOR);
					r=bitwiseAndExpr();
					if ( inputState.guessing==0 ) {
						x = new ExprBinary(ExprBinary.BINOP_BXOR, x, r);
					}
				}
				else {
					break _loop136;
				}
				
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_29);
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
			_loop139:
			do {
				if ((LA(1)==BITWISE_AND)) {
					match(BITWISE_AND);
					r=equalExpr();
					if ( inputState.guessing==0 ) {
						x = new ExprBinary(ExprBinary.BINOP_BAND, x, r);
					}
				}
				else {
					break _loop139;
				}
				
			} while (true);
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
		return x;
	}
	
	public final Expression  equalExpr() throws RecognitionException, TokenStreamException {
		Expression x;
		
		x = null; Expression r; int o = 0;
		
		try {      // for error handling
			x=compareExpr();
			{
			_loop143:
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
					break _loop143;
				}
				
			} while (true);
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
		return x;
	}
	
	public final Expression  compareExpr() throws RecognitionException, TokenStreamException {
		Expression x;
		
		x = null; Expression r; int o = 0;
		
		try {      // for error handling
			x=shiftExpr();
			{
			_loop147:
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
					break _loop147;
				}
				
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_32);
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
			_loop151:
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
					break _loop151;
				}
				
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_33);
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
			_loop155:
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
					break _loop155;
				}
				
			} while (true);
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
		return x;
	}
	
	public final Expression  multExpr() throws RecognitionException, TokenStreamException {
		Expression x;
		
		x = null; Expression r; int o = 0;
		
		try {      // for error handling
			x=inc_dec_expr();
			{
			_loop159:
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
					break _loop159;
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
	
	public final Expression  inc_dec_expr() throws RecognitionException, TokenStreamException {
		Expression x;
		
		Token  b = null;
		x = null;
		
		try {      // for error handling
			boolean synPredMatched162 = false;
			if (((_tokenSet_13.member(LA(1))))) {
				int _m162 = mark();
				synPredMatched162 = true;
				inputState.guessing++;
				try {
					{
					incOrDec();
					}
				}
				catch (RecognitionException pe) {
					synPredMatched162 = false;
				}
				rewind(_m162);
inputState.guessing--;
			}
			if ( synPredMatched162 ) {
				x=incOrDec();
			}
			else {
				boolean synPredMatched164 = false;
				if (((LA(1)==LPAREN))) {
					int _m164 = mark();
					synPredMatched164 = true;
					inputState.guessing++;
					try {
						{
						match(LPAREN);
						primitive_type();
						}
					}
					catch (RecognitionException pe) {
						synPredMatched164 = false;
					}
					rewind(_m164);
inputState.guessing--;
				}
				if ( synPredMatched164 ) {
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
				else if ((_tokenSet_36.member(LA(1)))) {
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
					recover(ex,_tokenSet_22);
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
			_loop171:
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
					break _loop171;
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
				recover(ex,_tokenSet_22);
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
			case TK_pi:
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
				recover(ex,_tokenSet_22);
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
		x = null; List rlist;
		
		try {      // for error handling
			x=tminic_value_expr();
			{
			_loop178:
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
					rlist=array_range_list();
					if ( inputState.guessing==0 ) {
						x = new ExprArrayRange(x, rlist);
					}
					match(RSQUARE);
					break;
				}
				default:
				{
					break _loop178;
				}
				}
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_22);
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
			case TK_pi:
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
				boolean synPredMatched189 = false;
				if (((LA(1)==ID))) {
					int _m189 = mark();
					synPredMatched189 = true;
					inputState.guessing++;
					try {
						{
						func_call();
						}
					}
					catch (RecognitionException pe) {
						synPredMatched189 = false;
					}
					rewind(_m189);
inputState.guessing--;
				}
				if ( synPredMatched189 ) {
					x=func_call();
				}
				else if ((LA(1)==ID)) {
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
				recover(ex,_tokenSet_25);
			} else {
			  throw ex;
			}
		}
		return x;
	}
	
	public final List  array_range_list() throws RecognitionException, TokenStreamException {
		List l;
		
		l=new ArrayList(); Object r;
		
		try {      // for error handling
			r=array_range();
			if ( inputState.guessing==0 ) {
				l.add(r);
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
		return l;
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
				boolean synPredMatched184 = false;
				if (((LA(1)==ID))) {
					int _m184 = mark();
					synPredMatched184 = true;
					inputState.guessing++;
					try {
						{
						func_call();
						}
					}
					catch (RecognitionException pe) {
						synPredMatched184 = false;
					}
					rewind(_m184);
inputState.guessing--;
				}
				if ( synPredMatched184 ) {
					x=func_call();
				}
				else if ((LA(1)==ID)) {
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
				recover(ex,_tokenSet_24);
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
			l=func_call_params();
			if ( inputState.guessing==0 ) {
				x = new ExprNew( getContext(n), t);
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
		return x;
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
				recover(ex,_tokenSet_24);
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
		Token  pi = null;
		Token  t = null;
		Token  f = null;
		Token  t1 = null;
		Token  t2 = null;
		Token  n1 = null;
		x = null;
		
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
					x = new ExprConstChar(getContext(c), c.getText());
				}
				break;
			}
			case STRING_LITERAL:
			{
				s = LT(1);
				match(STRING_LITERAL);
				if ( inputState.guessing==0 ) {
					x = new ExprConstStr(getContext(s), s.getText());
				}
				break;
			}
			case TK_pi:
			{
				pi = LT(1);
				match(TK_pi);
				if ( inputState.guessing==0 ) {
					x = new ExprConstFloat(getContext(pi), Math.PI);
				}
				break;
			}
			case TK_true:
			{
				t = LT(1);
				match(TK_true);
				if ( inputState.guessing==0 ) {
					x = new ExprConstBoolean(getContext(t), true);
				}
				break;
			}
			case TK_false:
			{
				f = LT(1);
				match(TK_false);
				if ( inputState.guessing==0 ) {
					x = new ExprConstBoolean(getContext(f), false);
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
					n1 = LT(1);
					match(NUMBER);
					match(RPAREN);
					break;
				}
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
						  x = new ExprStar(getContext(t2),Integer.parseInt(n1.getText())); 
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
				recover(ex,_tokenSet_25);
			} else {
			  throw ex;
			}
		}
		return x;
	}
	
	public final Object  array_range() throws RecognitionException, TokenStreamException {
		Object x;
		
		Token  len = null;
		x=null; Expression start,end,l;
		
		try {      // for error handling
			start=right_expr();
			if ( inputState.guessing==0 ) {
				x=new ExprArrayRange.RangeLen(start);
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
				case REGEN:
				case CHAR_LITERAL:
				case STRING_LITERAL:
				case HQUAN:
				case NUMBER:
				case ID:
				case TK_pi:
				{
					end=right_expr();
					if ( inputState.guessing==0 ) {
						x=new ExprArrayRange.Range(start,end);
					}
					break;
				}
				case COLON:
				{
					match(COLON);
					{
					boolean synPredMatched200 = false;
					if (((LA(1)==NUMBER))) {
						int _m200 = mark();
						synPredMatched200 = true;
						inputState.guessing++;
						try {
							{
							match(NUMBER);
							}
						}
						catch (RecognitionException pe) {
							synPredMatched200 = false;
						}
						rewind(_m200);
inputState.guessing--;
					}
					if ( synPredMatched200 ) {
						len = LT(1);
						match(NUMBER);
						if ( inputState.guessing==0 ) {
							x=new ExprArrayRange.RangeLen(start,Integer.parseInt(len.getText()));
						}
					}
					else if ((_tokenSet_8.member(LA(1)))) {
						l=right_expr();
						if ( inputState.guessing==0 ) {
							x=new ExprArrayRange.RangeLen(start,l);
						}
					}
					else {
						throw new NoViableAltException(LT(1), getFilename());
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
		"\"boolean\"",
		"\"float\"",
		"\"bit\"",
		"\"int\"",
		"\"void\"",
		"\"double\"",
		"\"complex\"",
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
		"\"implements\"",
		"\"assert\"",
		"\"h_assert\"",
		"\"generator\"",
		"\"harness\"",
		"\"include\"",
		"\"pragma\"",
		"ARROW",
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
		"REGEN",
		"CHAR_LITERAL",
		"STRING_LITERAL",
		"ESC",
		"DIGIT",
		"HQUAN",
		"NUMBER",
		"an identifier",
		"TK_pipeline",
		"TK_splitjoin",
		"TK_feedbackloop",
		"TK_sbox",
		"TK_roundrobin",
		"TK_duplicate",
		"TK_portal",
		"TK_handler",
		"TK_pi"
	};
	
	private static final long[] mk_tokenSet_0() {
		long[] data = { 1649269522432L, 277025390592L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_0 = new BitSet(mk_tokenSet_0());
	private static final long[] mk_tokenSet_1() {
		long[] data = { 2080768L, 277025390592L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_1 = new BitSet(mk_tokenSet_1());
	private static final long[] mk_tokenSet_2() {
		long[] data = { 2L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_2 = new BitSet(mk_tokenSet_2());
	private static final long[] mk_tokenSet_3() {
		long[] data = { 0L, 2147483648L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_3 = new BitSet(mk_tokenSet_3());
	private static final long[] mk_tokenSet_4() {
		long[] data = { 8246341386242L, 277025390592L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_4 = new BitSet(mk_tokenSet_4());
	private static final long[] mk_tokenSet_5() {
		long[] data = { 0L, 32768L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_5 = new BitSet(mk_tokenSet_5());
	private static final long[] mk_tokenSet_6() {
		long[] data = { 6275072L, 277025390592L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_6 = new BitSet(mk_tokenSet_6());
	private static final long[] mk_tokenSet_7() {
		long[] data = { 290271069732866L, 2147483648L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_7 = new BitSet(mk_tokenSet_7());
	private static final long[] mk_tokenSet_8() {
		long[] data = { 794040960840374272L, 1103393718272L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_8 = new BitSet(mk_tokenSet_8());
	private static final long[] mk_tokenSet_9() {
		long[] data = { 2251799813685248L, 98304L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_9 = new BitSet(mk_tokenSet_9());
	private static final long[] mk_tokenSet_10() {
		long[] data = { 1125968626319360L, 32768L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_10 = new BitSet(mk_tokenSet_10());
	private static final long[] mk_tokenSet_11() {
		long[] data = { 649926149759036272L, 277042200576L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_11 = new BitSet(mk_tokenSet_11());
	private static final long[] mk_tokenSet_12() {
		long[] data = { 652186195928804210L, 277042200576L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_12 = new BitSet(mk_tokenSet_12());
	private static final long[] mk_tokenSet_13() {
		long[] data = { 648799821318064128L, 2164260864L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_13 = new BitSet(mk_tokenSet_13());
	private static final long[] mk_tokenSet_14() {
		long[] data = { 652177949589498738L, 277042200576L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_14 = new BitSet(mk_tokenSet_14());
	private static final long[] mk_tokenSet_15() {
		long[] data = { 281474976712704L, 2164260864L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_15 = new BitSet(mk_tokenSet_15());
	private static final long[] mk_tokenSet_16() {
		long[] data = { 562949953421312L, 32768L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_16 = new BitSet(mk_tokenSet_16());
	private static final long[] mk_tokenSet_17() {
		long[] data = { 11821949021847552L, 114688L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_17 = new BitSet(mk_tokenSet_17());
	private static final long[] mk_tokenSet_18() {
		long[] data = { -1688849860263934L, 10223615L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_18 = new BitSet(mk_tokenSet_18());
	private static final long[] mk_tokenSet_19() {
		long[] data = { 5356820650524674L, 2147483648L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_19 = new BitSet(mk_tokenSet_19());
	private static final long[] mk_tokenSet_20() {
		long[] data = { 562949953421312L, 98304L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_20 = new BitSet(mk_tokenSet_20());
	private static final long[] mk_tokenSet_21() {
		long[] data = { 562949953421312L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_21 = new BitSet(mk_tokenSet_21());
	private static final long[] mk_tokenSet_22() {
		long[] data = { 5938559058641420288L, 10092479L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_22 = new BitSet(mk_tokenSet_22());
	private static final long[] mk_tokenSet_23() {
		long[] data = { -6192449487634432L, 10092543L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_23 = new BitSet(mk_tokenSet_23());
	private static final long[] mk_tokenSet_24() {
		long[] data = { -1688849860263936L, 10223615L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_24 = new BitSet(mk_tokenSet_24());
	private static final long[] mk_tokenSet_25() {
		long[] data = { 5943062658268790784L, 10223551L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_25 = new BitSet(mk_tokenSet_25());
	private static final long[] mk_tokenSet_26() {
		long[] data = { 11821949021847552L, 122880L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_26 = new BitSet(mk_tokenSet_26());
	private static final long[] mk_tokenSet_27() {
		long[] data = { 11821949021847552L, 122884L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_27 = new BitSet(mk_tokenSet_27());
	private static final long[] mk_tokenSet_28() {
		long[] data = { 11821949021847552L, 122886L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_28 = new BitSet(mk_tokenSet_28());
	private static final long[] mk_tokenSet_29() {
		long[] data = { 11821949021847552L, 122902L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_29 = new BitSet(mk_tokenSet_29());
	private static final long[] mk_tokenSet_30() {
		long[] data = { 11821949021847552L, 122934L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_30 = new BitSet(mk_tokenSet_30());
	private static final long[] mk_tokenSet_31() {
		long[] data = { 11821949021847552L, 122942L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_31 = new BitSet(mk_tokenSet_31());
	private static final long[] mk_tokenSet_32() {
		long[] data = { 11821949021847552L, 123326L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_32 = new BitSet(mk_tokenSet_32());
	private static final long[] mk_tokenSet_33() {
		long[] data = { 11821949021847552L, 131006L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_33 = new BitSet(mk_tokenSet_33());
	private static final long[] mk_tokenSet_34() {
		long[] data = { 11821949021847552L, 1703870L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_34 = new BitSet(mk_tokenSet_34());
	private static final long[] mk_tokenSet_35() {
		long[] data = { 173951535607185408L, 10092478L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_35 = new BitSet(mk_tokenSet_35());
	private static final long[] mk_tokenSet_36() {
		long[] data = { 145522614499022848L, 1103393456128L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_36 = new BitSet(mk_tokenSet_36());
	private static final long[] mk_tokenSet_37() {
		long[] data = { 9007199254740992L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_37 = new BitSet(mk_tokenSet_37());
	
	}
