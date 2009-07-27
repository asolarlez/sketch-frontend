// $ANTLR : "StreamItParserFE.g" -> "StreamItParserFE.java"$

	package streamit.frontend.parser;

	import streamit.frontend.nodes.*;
    import streamit.frontend.CommandLineParamManager;
    import streamit.frontend.Directive;
	import streamit.frontend.ToSBit;

	import java.util.Collections;
    import java.io.*;
    import java.util.ArrayList;
    import java.util.Iterator;
    import java.util.HashSet;
    import java.util.List;
    import java.util.Set;

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
        	List<String> incList = CommandLineParamManager.getParams().listValue("inc");
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
			_loop3003:
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
					boolean synPredMatched3000 = false;
					if (((_tokenSet_0.member(LA(1))))) {
						int _m3000 = mark();
						synPredMatched3000 = true;
						inputState.guessing++;
						try {
							{
							match(TK_static);
							return_type();
							match(ID);
							match(LPAREN);
							}
						}
						catch (RecognitionException pe) {
							synPredMatched3000 = false;
						}
						rewind(_m3000);
inputState.guessing--;
					}
					if ( synPredMatched3000 ) {
						f=function_decl();
						if ( inputState.guessing==0 ) {
							funcs.add(f);
						}
					}
					else {
						boolean synPredMatched3002 = false;
						if (((_tokenSet_0.member(LA(1))))) {
							int _m3002 = mark();
							synPredMatched3002 = true;
							inputState.guessing++;
							try {
								{
								return_type();
								match(ID);
								match(LPAREN);
								}
							}
							catch (RecognitionException pe) {
								synPredMatched3002 = false;
							}
							rewind(_m3002);
inputState.guessing--;
						}
						if ( synPredMatched3002 ) {
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
						break _loop3003;
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
		Type rt; List l; StmtBlock s; f = null; boolean isStatic=false;
		
		try {      // for error handling
			{
			switch ( LA(1)) {
			case TK_static:
			{
				match(TK_static);
				if ( inputState.guessing==0 ) {
					isStatic=true;
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
					
								if(isStatic){
									f = Function.newStatic(getContext(id), id.getText(), rt, l,
										impl==null?null:impl.getText(), s);
								}else{
									f = Function.newHelper(getContext(id), id.getText(), rt, l,
										impl==null?null:impl.getText(), s);
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
			_loop3011:
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
					break _loop3011;
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
			_loop3197:
			do {
				if ((_tokenSet_6.member(LA(1)))) {
					p=param_decl();
					match(SEMI);
					if ( inputState.guessing==0 ) {
						names.add(p.getName()); types.add(p.getType());
					}
				}
				else {
					break _loop3197;
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
				_loop3045:
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
						case TK_pop:
						case TK_peek:
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
						break _loop3045;
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
			boolean synPredMatched3119 = false;
			if (((LA(1)==LCURLY))) {
				int _m3119 = mark();
				synPredMatched3119 = true;
				inputState.guessing++;
				try {
					{
					arr_initializer();
					}
				}
				catch (RecognitionException pe) {
					synPredMatched3119 = false;
				}
				rewind(_m3119);
inputState.guessing--;
			}
			if ( synPredMatched3119 ) {
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
				_loop3061:
				do {
					if ((LA(1)==COMMA)) {
						match(COMMA);
						p=param_decl();
						if ( inputState.guessing==0 ) {
							l.add(p);
						}
					}
					else {
						break _loop3061;
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
			_loop3066:
			do {
				if ((_tokenSet_11.member(LA(1)))) {
					s=statement();
					if ( inputState.guessing==0 ) {
						l.add(s);
					}
				}
				else {
					break _loop3066;
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
	
	public final FuncWork  work_decl() throws RecognitionException, TokenStreamException {
		FuncWork f;
		
		Token  tw = null;
		Token  tpw = null;
		Token  tp = null;
		Token  id = null;
			f = null;
			Expression pop = null, peek = null, push = null;
			Statement s; FEContext c = null; String name = null;
			int type = 0;
		
		
		try {      // for error handling
			{
			switch ( LA(1)) {
			case TK_work:
			{
				tw = LT(1);
				match(TK_work);
				if ( inputState.guessing==0 ) {
					c = getContext(tw); type = Function.FUNC_WORK;
				}
				break;
			}
			case TK_prework:
			{
				tpw = LT(1);
				match(TK_prework);
				if ( inputState.guessing==0 ) {
					c = getContext(tpw);
												 type = Function.FUNC_PREWORK;
				}
				break;
			}
			case TK_phase:
			{
				tp = LT(1);
				match(TK_phase);
				id = LT(1);
				match(ID);
				if ( inputState.guessing==0 ) {
					c = getContext(tp); name = id.getText();
								                    type = Function.FUNC_PHASE;
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
			_loop3019:
			do {
				switch ( LA(1)) {
				case TK_push:
				{
					match(TK_push);
					push=right_expr();
					break;
				}
				case TK_pop:
				{
					match(TK_pop);
					pop=right_expr();
					break;
				}
				case TK_peek:
				{
					match(TK_peek);
					peek=right_expr();
					break;
				}
				default:
				{
					break _loop3019;
				}
				}
			} while (true);
			}
			s=block();
			if ( inputState.guessing==0 ) {
				f = new FuncWork(c, type, name, s, peek, pop, push);
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
	
	public final Expression  right_expr() throws RecognitionException, TokenStreamException {
		Expression x;
		
		x = null;
		
		try {      // for error handling
			x=ternaryExpr();
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				recover(ex,_tokenSet_13);
			} else {
			  throw ex;
			}
		}
		return x;
	}
	
	public final Function  init_decl() throws RecognitionException, TokenStreamException {
		Function f;
		
		Token  t = null;
		Statement s; f = null;
		
		try {      // for error handling
			t = LT(1);
			match(TK_init);
			s=block();
			if ( inputState.guessing==0 ) {
				f = Function.newInit(getContext(t), s);
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
	
	public final Statement  push_statement() throws RecognitionException, TokenStreamException {
		Statement s;
		
		Token  t = null;
		s = null; Expression x;
		
		try {      // for error handling
			t = LT(1);
			match(TK_push);
			match(LPAREN);
			x=right_expr();
			match(RPAREN);
			if ( inputState.guessing==0 ) {
				s = new StmtPush(getContext(t), x);
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
			case TK_split:
			{
				s=split_statement();
				match(SEMI);
				break;
			}
			case TK_join:
			{
				s=join_statement();
				match(SEMI);
				break;
			}
			case TK_enqueue:
			{
				s=enqueue_statement();
				match(SEMI);
				break;
			}
			case TK_push:
			{
				s=push_statement();
				match(SEMI);
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
				boolean synPredMatched3024 = false;
				if (((_tokenSet_1.member(LA(1))))) {
					int _m3024 = mark();
					synPredMatched3024 = true;
					inputState.guessing++;
					try {
						{
						data_type();
						match(ID);
						}
					}
					catch (RecognitionException pe) {
						synPredMatched3024 = false;
					}
					rewind(_m3024);
inputState.guessing--;
				}
				if ( synPredMatched3024 ) {
					s=variable_decl();
					match(SEMI);
				}
				else {
					boolean synPredMatched3026 = false;
					if (((_tokenSet_14.member(LA(1))))) {
						int _m3026 = mark();
						synPredMatched3026 = true;
						inputState.guessing++;
						try {
							{
							expr_statement();
							}
						}
						catch (RecognitionException pe) {
							synPredMatched3026 = false;
						}
						rewind(_m3026);
inputState.guessing--;
					}
					if ( synPredMatched3026 ) {
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
					recover(ex,_tokenSet_15);
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
				recover(ex,_tokenSet_15);
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
				recover(ex,_tokenSet_15);
			} else {
			  throw ex;
			}
		}
		return s;
	}
	
	public final Statement  split_statement() throws RecognitionException, TokenStreamException {
		Statement s;
		
		Token  t = null;
		s = null; SplitterJoiner sj;
		
		try {      // for error handling
			t = LT(1);
			match(TK_split);
			sj=splitter_or_joiner();
			if ( inputState.guessing==0 ) {
				s = new StmtSplit(getContext(t), sj);
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
	
	public final Statement  join_statement() throws RecognitionException, TokenStreamException {
		Statement s;
		
		Token  t = null;
		s = null; SplitterJoiner sj;
		
		try {      // for error handling
			t = LT(1);
			match(TK_join);
			sj=splitter_or_joiner();
			if ( inputState.guessing==0 ) {
				s = new StmtJoin(getContext(t), sj);
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
	
	public final Statement  enqueue_statement() throws RecognitionException, TokenStreamException {
		Statement s;
		
		Token  t = null;
		s = null; Expression x;
		
		try {      // for error handling
			t = LT(1);
			match(TK_enqueue);
			x=right_expr();
			if ( inputState.guessing==0 ) {
				s = new StmtEnqueue(getContext(t), x);
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
	
	public final StmtInsertBlock  insert_block() throws RecognitionException, TokenStreamException {
		StmtInsertBlock ib;
		
		Token  t = null;
		ib=null; Statement s; List<Statement> insert = new ArrayList<Statement> (), into = new ArrayList<Statement> ();
		
		try {      // for error handling
			t = LT(1);
			match(TK_insert);
			match(LCURLY);
			{
			_loop3069:
			do {
				if ((_tokenSet_11.member(LA(1)))) {
					s=statement();
					if ( inputState.guessing==0 ) {
						insert.add(s);
					}
				}
				else {
					break _loop3069;
				}
				
			} while (true);
			}
			match(RCURLY);
			match(TK_into);
			match(LCURLY);
			{
			_loop3071:
			do {
				if ((_tokenSet_11.member(LA(1)))) {
					s=statement();
					if ( inputState.guessing==0 ) {
						into.add(s);
					}
				}
				else {
					break _loop3071;
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
				recover(ex,_tokenSet_15);
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
			_loop3074:
			do {
				if ((_tokenSet_11.member(LA(1)))) {
					s=statement();
					if ( inputState.guessing==0 ) {
						l.add(s);
					}
				}
				else {
					break _loop3074;
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
				recover(ex,_tokenSet_15);
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
				recover(ex,_tokenSet_15);
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
			_loop3051:
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
					break _loop3051;
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
			boolean synPredMatched3102 = false;
			if (((_tokenSet_16.member(LA(1))))) {
				int _m3102 = mark();
				synPredMatched3102 = true;
				inputState.guessing++;
				try {
					{
					incOrDec();
					}
				}
				catch (RecognitionException pe) {
					synPredMatched3102 = false;
				}
				rewind(_m3102);
inputState.guessing--;
			}
			if ( synPredMatched3102 ) {
				x=incOrDec();
				if ( inputState.guessing==0 ) {
					s = new StmtExpr(x);
				}
			}
			else {
				boolean synPredMatched3105 = false;
				if (((LA(1)==REGEN||LA(1)==ID))) {
					int _m3105 = mark();
					synPredMatched3105 = true;
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
						synPredMatched3105 = false;
					}
					rewind(_m3105);
inputState.guessing--;
				}
				if ( synPredMatched3105 ) {
					s=assign_expr();
				}
				else {
					boolean synPredMatched3107 = false;
					if (((LA(1)==ID))) {
						int _m3107 = mark();
						synPredMatched3107 = true;
						inputState.guessing++;
						try {
							{
							match(ID);
							match(LPAREN);
							}
						}
						catch (RecognitionException pe) {
							synPredMatched3107 = false;
						}
						rewind(_m3107);
inputState.guessing--;
					}
					if ( synPredMatched3107 ) {
						x=func_call();
						if ( inputState.guessing==0 ) {
							s = new StmtExpr(x);
						}
					}
					else if ((LA(1)==TK_pop||LA(1)==TK_peek)) {
						x=streamit_value_expr();
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
						recover(ex,_tokenSet_17);
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
			boolean synPredMatched3086 = false;
			if (((LA(1)==TK_else))) {
				int _m3086 = mark();
				synPredMatched3086 = true;
				inputState.guessing++;
				try {
					{
					match(TK_else);
					}
				}
				catch (RecognitionException pe) {
					synPredMatched3086 = false;
				}
				rewind(_m3086);
inputState.guessing--;
			}
			if ( synPredMatched3086 ) {
				{
				match(TK_else);
				f=pseudo_block();
				}
			}
			else if ((_tokenSet_15.member(LA(1)))) {
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
				recover(ex,_tokenSet_15);
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
				recover(ex,_tokenSet_15);
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
			case TK_pop:
			case TK_peek:
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
				recover(ex,_tokenSet_15);
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
			case TK_pop:
			case TK_peek:
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
				recover(ex,_tokenSet_15);
			} else {
			  throw ex;
			}
		}
		return sb;
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
				boolean synPredMatched3035 = false;
				if (((LA(1)==LPAREN))) {
					int _m3035 = mark();
					synPredMatched3035 = true;
					inputState.guessing++;
					try {
						{
						match(LPAREN);
						match(RPAREN);
						}
					}
					catch (RecognitionException pe) {
						synPredMatched3035 = false;
					}
					rewind(_m3035);
inputState.guessing--;
				}
				if ( synPredMatched3035 ) {
					match(LPAREN);
					match(RPAREN);
					if ( inputState.guessing==0 ) {
						sj = new SJRoundRobin(getContext(tr));
					}
				}
				else {
					boolean synPredMatched3037 = false;
					if (((LA(1)==LPAREN))) {
						int _m3037 = mark();
						synPredMatched3037 = true;
						inputState.guessing++;
						try {
							{
							match(LPAREN);
							right_expr();
							match(RPAREN);
							}
						}
						catch (RecognitionException pe) {
							synPredMatched3037 = false;
						}
						rewind(_m3037);
inputState.guessing--;
					}
					if ( synPredMatched3037 ) {
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
					else if ((LA(1)==SEMI)) {
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
					recover(ex,_tokenSet_5);
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
			case TK_pop:
			case TK_peek:
			case TK_pi:
			{
				x=right_expr();
				if ( inputState.guessing==0 ) {
					l.add(x);
				}
				{
				_loop3114:
				do {
					if ((LA(1)==COMMA)) {
						match(COMMA);
						x=right_expr();
						if ( inputState.guessing==0 ) {
							l.add(x);
						}
					}
					else {
						break _loop3114;
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
			boolean synPredMatched3094 = false;
			if (((_tokenSet_1.member(LA(1))))) {
				int _m3094 = mark();
				synPredMatched3094 = true;
				inputState.guessing++;
				try {
					{
					variable_decl();
					}
				}
				catch (RecognitionException pe) {
					synPredMatched3094 = false;
				}
				rewind(_m3094);
inputState.guessing--;
			}
			if ( synPredMatched3094 ) {
				s=variable_decl();
			}
			else {
				boolean synPredMatched3096 = false;
				if (((_tokenSet_14.member(LA(1))))) {
					int _m3096 = mark();
					synPredMatched3096 = true;
					inputState.guessing++;
					try {
						{
						expr_statement();
						}
					}
					catch (RecognitionException pe) {
						synPredMatched3096 = false;
					}
					rewind(_m3096);
inputState.guessing--;
				}
				if ( synPredMatched3096 ) {
					s=expr_statement();
				}
				else {
					boolean synPredMatched3098 = false;
					if (((LA(1)==SEMI))) {
						int _m3098 = mark();
						synPredMatched3098 = true;
						inputState.guessing++;
						try {
							{
							match(SEMI);
							}
						}
						catch (RecognitionException pe) {
							synPredMatched3098 = false;
						}
						rewind(_m3098);
inputState.guessing--;
					}
					if ( synPredMatched3098 ) {
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
			case INCREMENT:
			case DECREMENT:
			case REGEN:
			case ID:
			case TK_pop:
			case TK_peek:
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
				recover(ex,_tokenSet_18);
			} else {
			  throw ex;
			}
		}
		return x;
	}
	
	public final Expression  left_expr() throws RecognitionException, TokenStreamException {
		Expression x;
		
		Token  r = null;
		x = null;
		
		try {      // for error handling
			switch ( LA(1)) {
			case ID:
			{
				x=value();
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
				recover(ex,_tokenSet_22);
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
				recover(ex,_tokenSet_17);
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
				recover(ex,_tokenSet_18);
			} else {
			  throw ex;
			}
		}
		return x;
	}
	
	public final Expression  streamit_value_expr() throws RecognitionException, TokenStreamException {
		Expression x;
		
		Token  t = null;
		Token  u = null;
		x = null;
		
		try {      // for error handling
			switch ( LA(1)) {
			case TK_pop:
			{
				t = LT(1);
				match(TK_pop);
				match(LPAREN);
				match(RPAREN);
				if ( inputState.guessing==0 ) {
					x = new ExprPop(getContext(t));
				}
				break;
			}
			case TK_peek:
			{
				u = LT(1);
				match(TK_peek);
				match(LPAREN);
				x=right_expr();
				match(RPAREN);
				if ( inputState.guessing==0 ) {
					x = new ExprPeek(getContext(u), x);
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
				recover(ex,_tokenSet_18);
			} else {
			  throw ex;
			}
		}
		return x;
	}
	
	public final Expression  value() throws RecognitionException, TokenStreamException {
		Expression x;
		
		Token  name = null;
		Token  field = null;
		Token  l = null;
		x = null; List rlist;
		
		try {      // for error handling
			name = LT(1);
			match(ID);
			if ( inputState.guessing==0 ) {
				x = new ExprVar(getContext(name), name.getText());
			}
			{
			_loop3186:
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
					break _loop3186;
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
			case LCURLY:
			case RCURLY:
			case RSQUARE:
			case COLON:
			case SEMI:
			case COMMA:
			case TK_push:
			case TK_pop:
			case TK_peek:
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
				recover(ex,_tokenSet_13);
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
			case TK_pop:
			case TK_peek:
			case TK_pi:
			{
				y=var_initializer();
				if ( inputState.guessing==0 ) {
					l.add(y);
				}
				{
				_loop3123:
				do {
					if ((LA(1)==COMMA)) {
						match(COMMA);
						y=var_initializer();
						if ( inputState.guessing==0 ) {
							l.add(y);
						}
					}
					else {
						break _loop3123;
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
				recover(ex,_tokenSet_9);
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
			_loop3128:
			do {
				if ((LA(1)==LOGIC_OR)) {
					match(LOGIC_OR);
					r=logicAndExpr();
					if ( inputState.guessing==0 ) {
						x = new ExprBinary(ExprBinary.BINOP_OR, x, r);
					}
				}
				else {
					break _loop3128;
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
	
	public final Expression  logicAndExpr() throws RecognitionException, TokenStreamException {
		Expression x;
		
		x = null; Expression r;
		
		try {      // for error handling
			x=bitwiseOrExpr();
			{
			_loop3131:
			do {
				if ((LA(1)==LOGIC_AND)) {
					match(LOGIC_AND);
					r=bitwiseOrExpr();
					if ( inputState.guessing==0 ) {
						x = new ExprBinary(ExprBinary.BINOP_AND, x, r);
					}
				}
				else {
					break _loop3131;
				}
				
			} while (true);
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
	
	public final Expression  bitwiseOrExpr() throws RecognitionException, TokenStreamException {
		Expression x;
		
		x = null; Expression r;
		
		try {      // for error handling
			x=bitwiseXorExpr();
			{
			_loop3134:
			do {
				if ((LA(1)==BITWISE_OR)) {
					match(BITWISE_OR);
					r=bitwiseXorExpr();
					if ( inputState.guessing==0 ) {
						x = new ExprBinary(ExprBinary.BINOP_BOR, x, r);
					}
				}
				else {
					break _loop3134;
				}
				
			} while (true);
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
	
	public final Expression  bitwiseXorExpr() throws RecognitionException, TokenStreamException {
		Expression x;
		
		x = null; Expression r;
		
		try {      // for error handling
			x=bitwiseAndExpr();
			{
			_loop3137:
			do {
				if ((LA(1)==BITWISE_XOR)) {
					match(BITWISE_XOR);
					r=bitwiseAndExpr();
					if ( inputState.guessing==0 ) {
						x = new ExprBinary(ExprBinary.BINOP_BXOR, x, r);
					}
				}
				else {
					break _loop3137;
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
	
	public final Expression  bitwiseAndExpr() throws RecognitionException, TokenStreamException {
		Expression x;
		
		x = null; Expression r;
		
		try {      // for error handling
			x=equalExpr();
			{
			_loop3140:
			do {
				if ((LA(1)==BITWISE_AND)) {
					match(BITWISE_AND);
					r=equalExpr();
					if ( inputState.guessing==0 ) {
						x = new ExprBinary(ExprBinary.BINOP_BAND, x, r);
					}
				}
				else {
					break _loop3140;
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
	
	public final Expression  equalExpr() throws RecognitionException, TokenStreamException {
		Expression x;
		
		x = null; Expression r; int o = 0;
		
		try {      // for error handling
			x=compareExpr();
			{
			_loop3144:
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
					break _loop3144;
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
	
	public final Expression  compareExpr() throws RecognitionException, TokenStreamException {
		Expression x;
		
		x = null; Expression r; int o = 0;
		
		try {      // for error handling
			x=shiftExpr();
			{
			_loop3148:
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
					break _loop3148;
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
	
	public final Expression  shiftExpr() throws RecognitionException, TokenStreamException {
		Expression x;
		
		x=null; Expression r; int op=0;
		
		try {      // for error handling
			x=addExpr();
			{
			_loop3152:
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
					break _loop3152;
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
	
	public final Expression  addExpr() throws RecognitionException, TokenStreamException {
		Expression x;
		
		x = null; Expression r; int o = 0;
		
		try {      // for error handling
			x=multExpr();
			{
			_loop3156:
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
					break _loop3156;
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
	
	public final Expression  multExpr() throws RecognitionException, TokenStreamException {
		Expression x;
		
		x = null; Expression r; int o = 0;
		
		try {      // for error handling
			x=inc_dec_expr();
			{
			_loop3160:
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
					break _loop3160;
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
	
	public final Expression  inc_dec_expr() throws RecognitionException, TokenStreamException {
		Expression x;
		
		Token  b = null;
		x = null;
		
		try {      // for error handling
			boolean synPredMatched3163 = false;
			if (((_tokenSet_16.member(LA(1))))) {
				int _m3163 = mark();
				synPredMatched3163 = true;
				inputState.guessing++;
				try {
					{
					incOrDec();
					}
				}
				catch (RecognitionException pe) {
					synPredMatched3163 = false;
				}
				rewind(_m3163);
inputState.guessing--;
			}
			if ( synPredMatched3163 ) {
				x=incOrDec();
			}
			else {
				boolean synPredMatched3165 = false;
				if (((LA(1)==LPAREN))) {
					int _m3165 = mark();
					synPredMatched3165 = true;
					inputState.guessing++;
					try {
						{
						match(LPAREN);
						primitive_type();
						}
					}
					catch (RecognitionException pe) {
						synPredMatched3165 = false;
					}
					rewind(_m3165);
inputState.guessing--;
				}
				if ( synPredMatched3165 ) {
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
				else if ((_tokenSet_33.member(LA(1)))) {
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
					recover(ex,_tokenSet_18);
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
			_loop3172:
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
					break _loop3172;
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
				recover(ex,_tokenSet_18);
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
			case TK_pop:
			case TK_peek:
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
			switch ( LA(1)) {
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
				x=minic_value_expr();
				break;
			}
			case TK_pop:
			case TK_peek:
			{
				x=streamit_value_expr();
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			if ( inputState.guessing==0 ) {
				if (neg) x = new ExprUnary(getContext(m), ExprUnary.UNOP_NEG, x);
			}
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
		return x;
	}
	
	public final Expression  minic_value_expr() throws RecognitionException, TokenStreamException {
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
			case LCURLY:
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
				boolean synPredMatched3180 = false;
				if (((LA(1)==ID))) {
					int _m3180 = mark();
					synPredMatched3180 = true;
					inputState.guessing++;
					try {
						{
						func_call();
						}
					}
					catch (RecognitionException pe) {
						synPredMatched3180 = false;
					}
					rewind(_m3180);
inputState.guessing--;
				}
				if ( synPredMatched3180 ) {
					x=func_call();
				}
				else if ((LA(1)==ID)) {
					x=value();
				}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
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
				recover(ex,_tokenSet_18);
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
		Token  t3 = null;
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
				if ( inputState.guessing==0 ) {
					x = new ExprStar(getContext(t2));
				}
				break;
			}
			case LCURLY:
			{
				t3 = LT(1);
				match(LCURLY);
				match(STAR);
				n1 = LT(1);
				match(NUMBER);
				match(RCURLY);
				if ( inputState.guessing==0 ) {
					x = new ExprStar(getContext(t3),Integer.parseInt(n1.getText()));
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
				recover(ex,_tokenSet_18);
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
				recover(ex,_tokenSet_34);
			} else {
			  throw ex;
			}
		}
		return l;
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
				case TK_pop:
				case TK_peek:
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
					boolean synPredMatched3193 = false;
					if (((LA(1)==NUMBER))) {
						int _m3193 = mark();
						synPredMatched3193 = true;
						inputState.guessing++;
						try {
							{
							match(NUMBER);
							}
						}
						catch (RecognitionException pe) {
							synPredMatched3193 = false;
						}
						rewind(_m3193);
inputState.guessing--;
					}
					if ( synPredMatched3193 ) {
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
				recover(ex,_tokenSet_34);
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
		"\"static\"",
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
		"TK_work",
		"TK_prework",
		"TK_phase",
		"TK_push",
		"TK_pop",
		"TK_peek",
		"TK_init",
		"TK_split",
		"TK_join",
		"TK_roundrobin",
		"TK_duplicate",
		"TK_enqueue",
		"TK_portal",
		"TK_handler",
		"TK_pi"
	};
	
	private static final long[] mk_tokenSet_0() {
		long[] data = { 274878947328L, 70369281048576L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_0 = new BitSet(mk_tokenSet_0());
	private static final long[] mk_tokenSet_1() {
		long[] data = { 1040384L, 70369281048576L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_1 = new BitSet(mk_tokenSet_1());
	private static final long[] mk_tokenSet_2() {
		long[] data = { 2L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_2 = new BitSet(mk_tokenSet_2());
	private static final long[] mk_tokenSet_3() {
		long[] data = { 0L, 536870912L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_3 = new BitSet(mk_tokenSet_3());
	private static final long[] mk_tokenSet_4() {
		long[] data = { 1924147437570L, 70369281048576L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_4 = new BitSet(mk_tokenSet_4());
	private static final long[] mk_tokenSet_5() {
		long[] data = { 0L, 8192L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_5 = new BitSet(mk_tokenSet_5());
	private static final long[] mk_tokenSet_6() {
		long[] data = { 3137536L, 70369281048576L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_6 = new BitSet(mk_tokenSet_6());
	private static final long[] mk_tokenSet_7() {
		long[] data = { 72567767433218L, 536870912L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_7 = new BitSet(mk_tokenSet_7());
	private static final long[] mk_tokenSet_8() {
		long[] data = { 198510253094996992L, 282300580954112L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_8 = new BitSet(mk_tokenSet_8());
	private static final long[] mk_tokenSet_9() {
		long[] data = { 562949953421312L, 24576L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_9 = new BitSet(mk_tokenSet_9());
	private static final long[] mk_tokenSet_10() {
		long[] data = { 281509336449024L, 8192L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_10 = new BitSet(mk_tokenSet_10());
	private static final long[] mk_tokenSet_11() {
		long[] data = { 162411275829113712L, 113112799780864L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_11 = new BitSet(mk_tokenSet_11());
	private static final long[] mk_tokenSet_12() {
		long[] data = { 162976149937320818L, 113112799780864L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_12 = new BitSet(mk_tokenSet_12());
	private static final long[] mk_tokenSet_13() {
		long[] data = { 3236962232172544L, 962072702976L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_13 = new BitSet(mk_tokenSet_13());
	private static final long[] mk_tokenSet_14() {
		long[] data = { 162129586585337856L, 825174786048L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_14 = new BitSet(mk_tokenSet_14());
	private static final long[] mk_tokenSet_15() {
		long[] data = { 162974225790923632L, 113112799780864L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_15 = new BitSet(mk_tokenSet_15());
	private static final long[] mk_tokenSet_16() {
		long[] data = { 162129586585337856L, 541065216L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_16 = new BitSet(mk_tokenSet_16());
	private static final long[] mk_tokenSet_17() {
		long[] data = { 140737488355328L, 8192L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_17 = new BitSet(mk_tokenSet_17());
	private static final long[] mk_tokenSet_18() {
		long[] data = { -3126764778790322176L, 962075197423L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_18 = new BitSet(mk_tokenSet_18());
	private static final long[] mk_tokenSet_19() {
		long[] data = { 1339205162631170L, 536870912L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_19 = new BitSet(mk_tokenSet_19());
	private static final long[] mk_tokenSet_20() {
		long[] data = { 140737488355328L, 24576L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_20 = new BitSet(mk_tokenSet_20());
	private static final long[] mk_tokenSet_21() {
		long[] data = { 140737488355328L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_21 = new BitSet(mk_tokenSet_21());
	private static final long[] mk_tokenSet_22() {
		long[] data = { -1266637395197952L, 962075197439L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_22 = new BitSet(mk_tokenSet_22());
	private static final long[] mk_tokenSet_23() {
		long[] data = { 3236962232172544L, 962072705024L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_23 = new BitSet(mk_tokenSet_23());
	private static final long[] mk_tokenSet_24() {
		long[] data = { 3236962232172544L, 962072705025L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_24 = new BitSet(mk_tokenSet_24());
	private static final long[] mk_tokenSet_25() {
		long[] data = { -9220135074622603264L, 962072705025L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_25 = new BitSet(mk_tokenSet_25());
	private static final long[] mk_tokenSet_26() {
		long[] data = { -9220135074622603264L, 962072705029L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_26 = new BitSet(mk_tokenSet_26());
	private static final long[] mk_tokenSet_27() {
		long[] data = { -9220135074622603264L, 962072705037L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_27 = new BitSet(mk_tokenSet_27());
	private static final long[] mk_tokenSet_28() {
		long[] data = { -9220135074622603264L, 962072705039L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_28 = new BitSet(mk_tokenSet_28());
	private static final long[] mk_tokenSet_29() {
		long[] data = { -9220135074622603264L, 962072705135L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_29 = new BitSet(mk_tokenSet_29());
	private static final long[] mk_tokenSet_30() {
		long[] data = { -9220135074622603264L, 962072707055L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_30 = new BitSet(mk_tokenSet_30());
	private static final long[] mk_tokenSet_31() {
		long[] data = { -9220135074622603264L, 962073100271L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_31 = new BitSet(mk_tokenSet_31());
	private static final long[] mk_tokenSet_32() {
		long[] data = { -9179602677976268800L, 962075197423L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_32 = new BitSet(mk_tokenSet_32());
	private static final long[] mk_tokenSet_33() {
		long[] data = { 36380666509659136L, 282300580888576L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_33 = new BitSet(mk_tokenSet_33());
	private static final long[] mk_tokenSet_34() {
		long[] data = { 2251799813685248L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_34 = new BitSet(mk_tokenSet_34());
	
	}
