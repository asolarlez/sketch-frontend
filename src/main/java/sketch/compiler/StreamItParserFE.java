// $ANTLR : "StreamItParserFE.g" -> "StreamItParserFE.java"$

	package streamit.frontend;

	import streamit.frontend.nodes.*;

	import java.util.Collections;
	import java.io.DataInputStream;
	import java.util.List;

	import java.util.ArrayList;

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

public class StreamItParserFE extends antlr.LLkParser       implements StreamItParserFETokenTypes
 {

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
			List funcs=new ArrayList(); Function f; FieldDecl fd;
		
		
		try {      // for error handling
			{
			_loop597:
			do {
				boolean synPredMatched596 = false;
				if (((_tokenSet_0.member(LA(1))))) {
					int _m596 = mark();
					synPredMatched596 = true;
					inputState.guessing++;
					try {
						{
						return_types();
						match(ID);
						match(LPAREN);
						}
					}
					catch (RecognitionException pe) {
						synPredMatched596 = false;
					}
					rewind(_m596);
					inputState.guessing--;
				}
				if ( synPredMatched596 ) {
					f=function_decl();
					if ( inputState.guessing==0 ) {
						funcs.add(f);
					}
				}
				else if ((_tokenSet_0.member(LA(1)))) {
					fd=field_decl();
					match(SEMI);
					if ( inputState.guessing==0 ) {
						vars.add(fd);
					}
				}
				else {
					break _loop597;
				}
				
			} while (true);
			}
			match(Token.EOF_TYPE);
			if ( inputState.guessing==0 ) {
				
							 StreamSpec ss=new StreamSpec(null, StreamSpec.STREAM_FILTER,
								new StreamType(null, TypePrimitive.bittype, TypePrimitive.bittype), "MAIN", 
								Collections.EMPTY_LIST, vars, funcs);
								streams.add(ss);
								 if (!hasError) p = new Program(null, streams, Collections.EMPTY_LIST /*structs*/);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_1);
			} else {
			  throw ex;
			}
		}
		return p;
	}
	
	public final List  return_types() throws RecognitionException, TokenStreamException {
		List l;
		
		l=new ArrayList(); Type t;
		
		try {      // for error handling
			t=data_type();
			if ( inputState.guessing==0 ) {
				l=Collections.singletonList(t);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_2);
			} else {
			  throw ex;
			}
		}
		return l;
	}
	
	public final Function  function_decl() throws RecognitionException, TokenStreamException {
		Function f;
		
		Token  id = null;
		Token  impl = null;
		List rt; List l; StmtBlock s; f = null;
		
		try {      // for error handling
			rt=return_types();
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
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			s=block();
			if ( inputState.guessing==0 ) {
				
							f = Function.newHelper(getContext(id), id.getText(), new TypeCompound(rt), l, 
								impl==null?null:impl.getText(), s); 
					
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_3);
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
			_loop602:
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
					break _loop602;
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
				consume();
				consumeUntil(_tokenSet_4);
			} else {
			  throw ex;
			}
		}
		return f;
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
				_loop634:
				do {
					if ((LA(1)==LSQUARE)) {
						l = LT(1);
						match(LSQUARE);
						{
						switch ( LA(1)) {
						case TK_peek:
						case TK_pop:
						case TK_pi:
						case TK_true:
						case TK_false:
						case LPAREN:
						case INCREMENT:
						case MINUS:
						case DECREMENT:
						case BANG:
						case NDVAL:
						case NDVAL2:
						case CHAR_LITERAL:
						case STRING_LITERAL:
						case HQUAN:
						case NUMBER:
						case ID:
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
						break _loop634;
					}
					
				} while (true);
				}
				break;
			}
			case TK_void:
			{
				match(TK_void);
				if ( inputState.guessing==0 ) {
					t = new TypePrimitive(TypePrimitive.TYPE_VOID);
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
				consume();
				consumeUntil(_tokenSet_5);
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
			switch ( LA(1)) {
			case TK_peek:
			case TK_pop:
			case TK_pi:
			case TK_true:
			case TK_false:
			case LPAREN:
			case INCREMENT:
			case MINUS:
			case DECREMENT:
			case BANG:
			case NDVAL:
			case NDVAL2:
			case CHAR_LITERAL:
			case STRING_LITERAL:
			case HQUAN:
			case NUMBER:
			case ID:
			{
				x=right_expr();
				break;
			}
			case LCURLY:
			{
				x=arr_initializer();
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
				consume();
				consumeUntil(_tokenSet_6);
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
				consume();
				consumeUntil(_tokenSet_1);
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
				consume();
				consumeUntil(_tokenSet_1);
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
			case TK_portal:
			case TK_boolean:
			case TK_float:
			case TK_bit:
			case TK_int:
			case TK_void:
			case TK_double:
			case TK_complex:
			case ID:
			{
				p=param_decl();
				if ( inputState.guessing==0 ) {
					l.add(p);
				}
				{
				_loop648:
				do {
					if ((LA(1)==COMMA)) {
						match(COMMA);
						p=param_decl();
						if ( inputState.guessing==0 ) {
							l.add(p);
						}
					}
					else {
						break _loop648;
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
				consume();
				consumeUntil(_tokenSet_7);
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
			_loop652:
			do {
				if ((_tokenSet_8.member(LA(1)))) {
					s=statement();
					if ( inputState.guessing==0 ) {
						l.add(s);
					}
				}
				else {
					break _loop652;
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
				consume();
				consumeUntil(_tokenSet_9);
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
			_loop610:
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
					break _loop610;
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
				consume();
				consumeUntil(_tokenSet_1);
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
				consume();
				consumeUntil(_tokenSet_10);
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
				consume();
				consumeUntil(_tokenSet_1);
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
				consume();
				consumeUntil(_tokenSet_4);
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
			{
				s=loop_statement();
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
				boolean synPredMatched615 = false;
				if (((_tokenSet_0.member(LA(1))))) {
					int _m615 = mark();
					synPredMatched615 = true;
					inputState.guessing++;
					try {
						{
						data_type();
						match(ID);
						}
					}
					catch (RecognitionException pe) {
						synPredMatched615 = false;
					}
					rewind(_m615);
					inputState.guessing--;
				}
				if ( synPredMatched615 ) {
					s=variable_decl();
					match(SEMI);
				}
				else {
					boolean synPredMatched617 = false;
					if (((_tokenSet_11.member(LA(1))))) {
						int _m617 = mark();
						synPredMatched617 = true;
						inputState.guessing++;
						try {
							{
							expr_statement();
							}
						}
						catch (RecognitionException pe) {
							synPredMatched617 = false;
						}
						rewind(_m617);
						inputState.guessing--;
					}
					if ( synPredMatched617 ) {
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
					consume();
					consumeUntil(_tokenSet_12);
				} else {
				  throw ex;
				}
			}
			return s;
		}
		
	public final Statement  loop_statement() throws RecognitionException, TokenStreamException {
		Statement s;
		
		Token  t = null;
		s = null; Expression exp; StmtBlock b;
		
		try {      // for error handling
			t = LT(1);
			match(TK_loop);
			match(LPAREN);
			exp=right_expr();
			match(RPAREN);
			b=block();
			if ( inputState.guessing==0 ) {
				s = new StmtLoop(getContext(t), exp, b);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_12);
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
				consume();
				consumeUntil(_tokenSet_4);
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
				consume();
				consumeUntil(_tokenSet_4);
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
				consume();
				consumeUntil(_tokenSet_4);
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
			List xs = new ArrayList(); FEContext ctx = null;
		
		try {      // for error handling
			t=data_type();
			id = LT(1);
			match(ID);
			if ( inputState.guessing==0 ) {
				ctx = getContext(id);
			}
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
			_loop640:
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
					break _loop640;
				}
				
			} while (true);
			}
			if ( inputState.guessing==0 ) {
				s = new StmtVarDecl(ctx, ts, ns, xs);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_4);
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
			boolean synPredMatched675 = false;
			if (((LA(1)==INCREMENT||LA(1)==DECREMENT||LA(1)==ID))) {
				int _m675 = mark();
				synPredMatched675 = true;
				inputState.guessing++;
				try {
					{
					incOrDec();
					}
				}
				catch (RecognitionException pe) {
					synPredMatched675 = false;
				}
				rewind(_m675);
				inputState.guessing--;
			}
			if ( synPredMatched675 ) {
				x=incOrDec();
				if ( inputState.guessing==0 ) {
					s = new StmtExpr(x);
				}
			}
			else {
				boolean synPredMatched678 = false;
				if (((LA(1)==ID))) {
					int _m678 = mark();
					synPredMatched678 = true;
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
						synPredMatched678 = false;
					}
					rewind(_m678);
					inputState.guessing--;
				}
				if ( synPredMatched678 ) {
					s=assign_expr();
				}
				else {
					boolean synPredMatched680 = false;
					if (((LA(1)==ID))) {
						int _m680 = mark();
						synPredMatched680 = true;
						inputState.guessing++;
						try {
							{
							match(ID);
							match(LPAREN);
							}
						}
						catch (RecognitionException pe) {
							synPredMatched680 = false;
						}
						rewind(_m680);
						inputState.guessing--;
					}
					if ( synPredMatched680 ) {
						x=func_call();
						if ( inputState.guessing==0 ) {
							s = new StmtExpr(x);
						}
					}
					else if ((LA(1)==TK_peek||LA(1)==TK_pop)) {
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
						consume();
						consumeUntil(_tokenSet_13);
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
			boolean synPredMatched659 = false;
			if (((LA(1)==TK_else))) {
				int _m659 = mark();
				synPredMatched659 = true;
				inputState.guessing++;
				try {
					{
					match(TK_else);
					}
				}
				catch (RecognitionException pe) {
					synPredMatched659 = false;
				}
				rewind(_m659);
				inputState.guessing--;
			}
			if ( synPredMatched659 ) {
				{
				match(TK_else);
				f=pseudo_block();
				}
			}
			else if ((_tokenSet_12.member(LA(1)))) {
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
				consume();
				consumeUntil(_tokenSet_12);
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
				consume();
				consumeUntil(_tokenSet_12);
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
				consume();
				consumeUntil(_tokenSet_4);
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
			case TK_peek:
			case TK_pop:
			case TK_pi:
			case TK_true:
			case TK_false:
			case LPAREN:
			case INCREMENT:
			case MINUS:
			case DECREMENT:
			case BANG:
			case NDVAL:
			case NDVAL2:
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
				consume();
				consumeUntil(_tokenSet_12);
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
			case TK_peek:
			case TK_pop:
			case TK_pi:
			case TK_true:
			case TK_false:
			case LPAREN:
			case INCREMENT:
			case MINUS:
			case DECREMENT:
			case BANG:
			case NDVAL:
			case NDVAL2:
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
				consume();
				consumeUntil(_tokenSet_4);
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
				boolean synPredMatched624 = false;
				if (((LA(1)==LPAREN))) {
					int _m624 = mark();
					synPredMatched624 = true;
					inputState.guessing++;
					try {
						{
						match(LPAREN);
						match(RPAREN);
						}
					}
					catch (RecognitionException pe) {
						synPredMatched624 = false;
					}
					rewind(_m624);
					inputState.guessing--;
				}
				if ( synPredMatched624 ) {
					match(LPAREN);
					match(RPAREN);
					if ( inputState.guessing==0 ) {
						sj = new SJRoundRobin(getContext(tr));
					}
				}
				else {
					boolean synPredMatched626 = false;
					if (((LA(1)==LPAREN))) {
						int _m626 = mark();
						synPredMatched626 = true;
						inputState.guessing++;
						try {
							{
							match(LPAREN);
							right_expr();
							match(RPAREN);
							}
						}
						catch (RecognitionException pe) {
							synPredMatched626 = false;
						}
						rewind(_m626);
						inputState.guessing--;
					}
					if ( synPredMatched626 ) {
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
					consume();
					consumeUntil(_tokenSet_4);
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
			case TK_peek:
			case TK_pop:
			case TK_pi:
			case TK_true:
			case TK_false:
			case LPAREN:
			case INCREMENT:
			case MINUS:
			case DECREMENT:
			case BANG:
			case NDVAL:
			case NDVAL2:
			case CHAR_LITERAL:
			case STRING_LITERAL:
			case HQUAN:
			case NUMBER:
			case ID:
			{
				x=right_expr();
				if ( inputState.guessing==0 ) {
					l.add(x);
				}
				{
				_loop687:
				do {
					if ((LA(1)==COMMA)) {
						match(COMMA);
						x=right_expr();
						if ( inputState.guessing==0 ) {
							l.add(x);
						}
					}
					else {
						break _loop687;
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
				consume();
				consumeUntil(_tokenSet_14);
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
					t = new TypePrimitive(TypePrimitive.TYPE_BOOLEAN);
				}
				break;
			}
			case TK_bit:
			{
				match(TK_bit);
				if ( inputState.guessing==0 ) {
					t = new TypePrimitive(TypePrimitive.TYPE_BIT);
				}
				break;
			}
			case TK_int:
			{
				match(TK_int);
				if ( inputState.guessing==0 ) {
					t = new TypePrimitive(TypePrimitive.TYPE_INT);
				}
				break;
			}
			case TK_float:
			{
				match(TK_float);
				if ( inputState.guessing==0 ) {
					t = new TypePrimitive(TypePrimitive.TYPE_FLOAT);
				}
				break;
			}
			case TK_double:
			{
				match(TK_double);
				if ( inputState.guessing==0 ) {
					t =  new TypePrimitive(TypePrimitive.TYPE_DOUBLE);
				}
				break;
			}
			case TK_complex:
			{
				match(TK_complex);
				if ( inputState.guessing==0 ) {
					t = new TypePrimitive(TypePrimitive.TYPE_COMPLEX);
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
				consume();
				consumeUntil(_tokenSet_15);
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
		Type t = new TypePrimitive(TypePrimitive.TYPE_VOID);
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
				consume();
				consumeUntil(_tokenSet_1);
			} else {
			  throw ex;
			}
		}
		return f;
	}
	
	public final Parameter  param_decl() throws RecognitionException, TokenStreamException {
		Parameter p;
		
		Token  id = null;
		Type t; p = null;
		
		try {      // for error handling
			t=data_type();
			id = LT(1);
			match(ID);
			if ( inputState.guessing==0 ) {
				p = new Parameter(t, id.getText());
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_16);
			} else {
			  throw ex;
			}
		}
		return p;
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
				consume();
				consumeUntil(_tokenSet_12);
			} else {
			  throw ex;
			}
		}
		return sb;
	}
	
	public final Statement  for_init_statement() throws RecognitionException, TokenStreamException {
		Statement s;
		
		s = null;
		
		try {      // for error handling
			boolean synPredMatched667 = false;
			if (((_tokenSet_0.member(LA(1))))) {
				int _m667 = mark();
				synPredMatched667 = true;
				inputState.guessing++;
				try {
					{
					variable_decl();
					}
				}
				catch (RecognitionException pe) {
					synPredMatched667 = false;
				}
				rewind(_m667);
				inputState.guessing--;
			}
			if ( synPredMatched667 ) {
				s=variable_decl();
			}
			else {
				boolean synPredMatched669 = false;
				if (((_tokenSet_11.member(LA(1))))) {
					int _m669 = mark();
					synPredMatched669 = true;
					inputState.guessing++;
					try {
						{
						expr_statement();
						}
					}
					catch (RecognitionException pe) {
						synPredMatched669 = false;
					}
					rewind(_m669);
					inputState.guessing--;
				}
				if ( synPredMatched669 ) {
					s=expr_statement();
				}
				else {
					boolean synPredMatched671 = false;
					if (((LA(1)==SEMI))) {
						int _m671 = mark();
						synPredMatched671 = true;
						inputState.guessing++;
						try {
							{
							match(SEMI);
							}
						}
						catch (RecognitionException pe) {
							synPredMatched671 = false;
						}
						rewind(_m671);
						inputState.guessing--;
					}
					if ( synPredMatched671 ) {
						if ( inputState.guessing==0 ) {
							s = new StmtEmpty(null);
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
						consume();
						consumeUntil(_tokenSet_4);
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
			case TK_peek:
			case TK_pop:
			case INCREMENT:
			case DECREMENT:
			case ID:
			{
				s=expr_statement();
				break;
			}
			case RPAREN:
			{
				if ( inputState.guessing==0 ) {
					s = new StmtEmpty(null);
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
				consume();
				consumeUntil(_tokenSet_17);
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
		x = null;
		
		try {      // for error handling
			switch ( LA(1)) {
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
				consume();
				consumeUntil(_tokenSet_18);
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
			x=value();
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_19);
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
				s = new StmtAssign(l.getContext(), l, r, o);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_13);
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
				consume();
				consumeUntil(_tokenSet_14);
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
				consume();
				consumeUntil(_tokenSet_18);
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
			switch ( LA(1)) {
			case DOT:
			{
				match(DOT);
				field = LT(1);
				match(ID);
				if ( inputState.guessing==0 ) {
					x = new ExprField(x.getContext(), x, field.getText());
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
			case TK_peek:
			case TK_pop:
			case TK_push:
			case RPAREN:
			case LCURLY:
			case RCURLY:
			case RSQUARE:
			case PLUS:
			case PLUS_EQUALS:
			case INCREMENT:
			case MINUS:
			case MINUS_EQUALS:
			case DECREMENT:
			case STAR:
			case STAR_EQUALS:
			case DIV:
			case DIV_EQUALS:
			case MOD:
			case LOGIC_AND:
			case LOGIC_OR:
			case BITWISE_AND:
			case BITWISE_OR:
			case BITWISE_XOR:
			case ASSIGN:
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
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_20);
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
					x = new ExprTernary(x.getContext(), ExprTernary.TEROP_COND,
										x, b, c);
				}
				break;
			}
			case TK_peek:
			case TK_pop:
			case TK_push:
			case RPAREN:
			case LCURLY:
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
				consume();
				consumeUntil(_tokenSet_10);
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
			case TK_peek:
			case TK_pop:
			case TK_pi:
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
				_loop694:
				do {
					if ((LA(1)==COMMA)) {
						match(COMMA);
						y=var_initializer();
						if ( inputState.guessing==0 ) {
							l.add(y);
						}
					}
					else {
						break _loop694;
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
				consume();
				consumeUntil(_tokenSet_6);
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
			_loop699:
			do {
				if ((LA(1)==LOGIC_OR)) {
					match(LOGIC_OR);
					r=logicAndExpr();
					if ( inputState.guessing==0 ) {
						x = new ExprBinary(x.getContext(), ExprBinary.BINOP_OR, x, r);
					}
				}
				else {
					break _loop699;
				}
				
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_21);
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
			x=bitwiseExpr();
			{
			_loop702:
			do {
				if ((LA(1)==LOGIC_AND)) {
					match(LOGIC_AND);
					r=bitwiseExpr();
					if ( inputState.guessing==0 ) {
						x = new ExprBinary(x.getContext(), ExprBinary.BINOP_AND, x, r);
					}
				}
				else {
					break _loop702;
				}
				
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_22);
			} else {
			  throw ex;
			}
		}
		return x;
	}
	
	public final Expression  bitwiseExpr() throws RecognitionException, TokenStreamException {
		Expression x;
		
		x = null; Expression r; int o = 0;
		
		try {      // for error handling
			x=equalExpr();
			{
			_loop706:
			do {
				if (((LA(1) >= BITWISE_AND && LA(1) <= BITWISE_XOR))) {
					{
					switch ( LA(1)) {
					case BITWISE_OR:
					{
						match(BITWISE_OR);
						if ( inputState.guessing==0 ) {
							o = ExprBinary.BINOP_BOR;
						}
						break;
					}
					case BITWISE_AND:
					{
						match(BITWISE_AND);
						if ( inputState.guessing==0 ) {
							o = ExprBinary.BINOP_BAND;
						}
						break;
					}
					case BITWISE_XOR:
					{
						match(BITWISE_XOR);
						if ( inputState.guessing==0 ) {
							o = ExprBinary.BINOP_BXOR;
						}
						break;
					}
					default:
					{
						throw new NoViableAltException(LT(1), getFilename());
					}
					}
					}
					r=equalExpr();
					if ( inputState.guessing==0 ) {
						x = new ExprBinary(x.getContext(), o, x, r);
					}
				}
				else {
					break _loop706;
				}
				
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_23);
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
			_loop710:
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
						x = new ExprBinary(x.getContext(), o, x, r);
					}
				}
				else {
					break _loop710;
				}
				
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_24);
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
			x=addExpr();
			{
			_loop714:
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
					r=addExpr();
					if ( inputState.guessing==0 ) {
						x = new ExprBinary(x.getContext(), o, x, r);
					}
				}
				else {
					break _loop714;
				}
				
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_25);
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
			_loop718:
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
						x = new ExprBinary(x.getContext(), o, x, r);
					}
				}
				else {
					break _loop718;
				}
				
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_26);
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
			x=castExpr();
			{
			_loop722:
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
					r=castExpr();
					if ( inputState.guessing==0 ) {
						x = new ExprBinary(x.getContext(), o, x, r);
					}
				}
				else {
					break _loop722;
				}
				
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_27);
			} else {
			  throw ex;
			}
		}
		return x;
	}
	
	public final Expression  castExpr() throws RecognitionException, TokenStreamException {
		Expression x;
		
		Token  l = null;
		x = null; Type t=null;
		
		try {      // for error handling
			boolean synPredMatched725 = false;
			if (((LA(1)==LPAREN))) {
				int _m725 = mark();
				synPredMatched725 = true;
				inputState.guessing++;
				try {
					{
					match(LPAREN);
					primitive_type();
					}
				}
				catch (RecognitionException pe) {
					synPredMatched725 = false;
				}
				rewind(_m725);
				inputState.guessing--;
			}
			if ( synPredMatched725 ) {
				{
				l = LT(1);
				match(LPAREN);
				t=primitive_type();
				match(RPAREN);
				}
				x=inc_dec_expr();
				if ( inputState.guessing==0 ) {
					x = new ExprTypeCast(getContext(l), t, x);
				}
			}
			else if ((_tokenSet_28.member(LA(1)))) {
				x=shiftExpr();
			}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_18);
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
			boolean synPredMatched734 = false;
			if (((LA(1)==INCREMENT||LA(1)==DECREMENT||LA(1)==ID))) {
				int _m734 = mark();
				synPredMatched734 = true;
				inputState.guessing++;
				try {
					{
					incOrDec();
					}
				}
				catch (RecognitionException pe) {
					synPredMatched734 = false;
				}
				rewind(_m734);
				inputState.guessing--;
			}
			if ( synPredMatched734 ) {
				x=incOrDec();
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
			else if ((_tokenSet_29.member(LA(1)))) {
				x=value_expr();
			}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_18);
			} else {
			  throw ex;
			}
		}
		return x;
	}
	
	public final Expression  shiftExpr() throws RecognitionException, TokenStreamException {
		Expression x;
		
		x=null; Expression amt; Type t=null;
		
		try {      // for error handling
			boolean synPredMatched729 = false;
			if (((_tokenSet_30.member(LA(1))))) {
				int _m729 = mark();
				synPredMatched729 = true;
				inputState.guessing++;
				try {
					{
					minic_value_expr();
					match(RSHIFT);
					}
				}
				catch (RecognitionException pe) {
					synPredMatched729 = false;
				}
				rewind(_m729);
				inputState.guessing--;
			}
			if ( synPredMatched729 ) {
				x=minic_value_expr();
				match(RSHIFT);
				amt=value_expr();
				if ( inputState.guessing==0 ) {
					x=new ExprBinary(x.getContext(), ExprBinary.BINOP_RSHIFT, x, amt);
				}
			}
			else {
				boolean synPredMatched731 = false;
				if (((_tokenSet_30.member(LA(1))))) {
					int _m731 = mark();
					synPredMatched731 = true;
					inputState.guessing++;
					try {
						{
						minic_value_expr();
						match(LSHIFT);
						}
					}
					catch (RecognitionException pe) {
						synPredMatched731 = false;
					}
					rewind(_m731);
					inputState.guessing--;
				}
				if ( synPredMatched731 ) {
					x=minic_value_expr();
					match(LSHIFT);
					amt=value_expr();
					if ( inputState.guessing==0 ) {
						x=new ExprBinary(x.getContext(), ExprBinary.BINOP_LSHIFT, x, amt);
					}
				}
				else if ((_tokenSet_28.member(LA(1)))) {
					x=inc_dec_expr();
				}
				else {
					throw new NoViableAltException(LT(1), getFilename());
				}
				}
			}
			catch (RecognitionException ex) {
				if (inputState.guessing==0) {
					reportError(ex);
					consume();
					consumeUntil(_tokenSet_18);
				} else {
				  throw ex;
				}
			}
			return x;
		}
		
	public final Expression  minic_value_expr() throws RecognitionException, TokenStreamException {
		Expression x;
		
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
			case TK_pi:
			case TK_true:
			case TK_false:
			case CHAR_LITERAL:
			case STRING_LITERAL:
			case HQUAN:
			case NUMBER:
			{
				x=constantExpr();
				break;
			}
			default:
				boolean synPredMatched745 = false;
				if (((LA(1)==ID))) {
					int _m745 = mark();
					synPredMatched745 = true;
					inputState.guessing++;
					try {
						{
						func_call();
						}
					}
					catch (RecognitionException pe) {
						synPredMatched745 = false;
					}
					rewind(_m745);
					inputState.guessing--;
				}
				if ( synPredMatched745 ) {
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
				consume();
				consumeUntil(_tokenSet_14);
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
			switch ( LA(1)) {
			case NDVAL:
			case NDVAL2:
			{
				x=ndvalue();
				if ( inputState.guessing==0 ) {
				}
				break;
			}
			case TK_peek:
			case TK_pop:
			case TK_pi:
			case TK_true:
			case TK_false:
			case LPAREN:
			case MINUS:
			case CHAR_LITERAL:
			case STRING_LITERAL:
			case HQUAN:
			case NUMBER:
			case ID:
			{
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
				case TK_peek:
				case TK_pop:
				case TK_pi:
				case TK_true:
				case TK_false:
				case LPAREN:
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
				switch ( LA(1)) {
				case TK_pi:
				case TK_true:
				case TK_false:
				case LPAREN:
				case CHAR_LITERAL:
				case STRING_LITERAL:
				case HQUAN:
				case NUMBER:
				case ID:
				{
					x=minic_value_expr();
					break;
				}
				case TK_peek:
				case TK_pop:
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
				consume();
				consumeUntil(_tokenSet_18);
			} else {
			  throw ex;
			}
		}
		return x;
	}
	
	public final Expression  ndvalue() throws RecognitionException, TokenStreamException {
		Expression x;
		
		Token  t = null;
		Token  t2 = null;
		x=null;
		
		try {      // for error handling
			switch ( LA(1)) {
			case NDVAL:
			{
				t = LT(1);
				match(NDVAL);
				if ( inputState.guessing==0 ) {
					x=new ExprStar(getContext(t));
				}
				break;
			}
			case NDVAL2:
			{
				t2 = LT(1);
				match(NDVAL2);
				if ( inputState.guessing==0 ) {
					x=new ExprStar(getContext(t2));
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
				consume();
				consumeUntil(_tokenSet_18);
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
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_14);
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
			{
			_loop750:
			do {
				if ((LA(1)==COMMA)) {
					match(COMMA);
					r=array_range();
					if ( inputState.guessing==0 ) {
						l.add(r);
					}
				}
				else {
					break _loop750;
				}
				
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_31);
			} else {
			  throw ex;
			}
		}
		return l;
	}
	
	public final Object  array_range() throws RecognitionException, TokenStreamException {
		Object x;
		
		Token  len = null;
		x=null; Expression start,end;
		
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
				case TK_peek:
				case TK_pop:
				case TK_pi:
				case TK_true:
				case TK_false:
				case LPAREN:
				case INCREMENT:
				case MINUS:
				case DECREMENT:
				case BANG:
				case NDVAL:
				case NDVAL2:
				case CHAR_LITERAL:
				case STRING_LITERAL:
				case HQUAN:
				case NUMBER:
				case ID:
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
					len = LT(1);
					match(NUMBER);
					if ( inputState.guessing==0 ) {
						x=new ExprArrayRange.RangeLen(start,Integer.parseInt(len.getText()));
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
				consume();
				consumeUntil(_tokenSet_32);
			} else {
			  throw ex;
			}
		}
		return x;
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
			_loop757:
			do {
				if ((_tokenSet_0.member(LA(1)))) {
					p=param_decl();
					match(SEMI);
					if ( inputState.guessing==0 ) {
						names.add(p.getName()); types.add(p.getType());
					}
				}
				else {
					break _loop757;
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
				consume();
				consumeUntil(_tokenSet_1);
			} else {
			  throw ex;
			}
		}
		return ts;
	}
	
	
	public static final String[] _tokenNames = {
		"<0>",
		"EOF",
		"<2>",
		"NULL_TREE_LOOKAHEAD",
		"\"filter\"",
		"\"pipeline\"",
		"\"splitjoin\"",
		"\"sbox\"",
		"\"feedbackloop\"",
		"\"portal\"",
		"\"to\"",
		"\"handler\"",
		"\"add\"",
		"\"split\"",
		"\"join\"",
		"\"duplicate\"",
		"\"roundrobin\"",
		"\"body\"",
		"\"loop\"",
		"\"enqueue\"",
		"\"init\"",
		"\"prework\"",
		"\"work\"",
		"\"phase\"",
		"\"peek\"",
		"\"pop\"",
		"\"push\"",
		"\"boolean\"",
		"\"float\"",
		"\"bit\"",
		"\"int\"",
		"\"void\"",
		"\"double\"",
		"\"complex\"",
		"\"struct\"",
		"\"template\"",
		"\"if\"",
		"\"else\"",
		"\"while\"",
		"\"for\"",
		"\"switch\"",
		"\"case\"",
		"\"default\"",
		"\"break\"",
		"\"continue\"",
		"\"return\"",
		"\"pi\"",
		"\"true\"",
		"\"false\"",
		"\"implements\"",
		"\"overrides\"",
		"ARROW",
		"WS",
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
		"CHAR_LITERAL",
		"STRING_LITERAL",
		"ESC",
		"DIGIT",
		"HQUAN",
		"NUMBER",
		"an identifier",
		"TK_do"
	};
	
	private static final long[] mk_tokenSet_0() {
		long[] data = { 17045651968L, 137438953472L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_0 = new BitSet(mk_tokenSet_0());
	private static final long[] mk_tokenSet_1() {
		long[] data = { 2L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_1 = new BitSet(mk_tokenSet_1());
	private static final long[] mk_tokenSet_2() {
		long[] data = { 0L, 137438953472L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_2 = new BitSet(mk_tokenSet_2());
	private static final long[] mk_tokenSet_3() {
		long[] data = { 17045651970L, 137438953472L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_3 = new BitSet(mk_tokenSet_3());
	private static final long[] mk_tokenSet_4() {
		long[] data = { 0L, 4194304L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_4 = new BitSet(mk_tokenSet_4());
	private static final long[] mk_tokenSet_5() {
		long[] data = { 2251799813685250L, 137438953472L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_5 = new BitSet(mk_tokenSet_5());
	private static final long[] mk_tokenSet_6() {
		long[] data = { 288230376151711744L, 12582912L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_6 = new BitSet(mk_tokenSet_6());
	private static final long[] mk_tokenSet_7() {
		long[] data = { 144678138029277184L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_7 = new BitSet(mk_tokenSet_7());
	private static final long[] mk_tokenSet_8() {
		long[] data = { -9079194365610663424L, 412321054724L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_8 = new BitSet(mk_tokenSet_8());
	private static final long[] mk_tokenSet_9() {
		long[] data = { -8790963852019998206L, 412321054724L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_9 = new BitSet(mk_tokenSet_9());
	private static final long[] mk_tokenSet_10() {
		long[] data = { 1657324662989783040L, 14680064L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_10 = new BitSet(mk_tokenSet_10());
	private static final long[] mk_tokenSet_11() {
		long[] data = { -9223372036804444160L, 137438953476L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_11 = new BitSet(mk_tokenSet_11());
	private static final long[] mk_tokenSet_12() {
		long[] data = { -8790963852019998208L, 412321054724L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_12 = new BitSet(mk_tokenSet_12());
	private static final long[] mk_tokenSet_13() {
		long[] data = { 72057594037927936L, 4194304L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_13 = new BitSet(mk_tokenSet_13());
	private static final long[] mk_tokenSet_14() {
		long[] data = { 3963167672203476992L, 1291837353L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_14 = new BitSet(mk_tokenSet_14());
	private static final long[] mk_tokenSet_15() {
		long[] data = { 650770146155036674L, 137438953472L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_15 = new BitSet(mk_tokenSet_15());
	private static final long[] mk_tokenSet_16() {
		long[] data = { 72057594037927936L, 12582912L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_16 = new BitSet(mk_tokenSet_16());
	private static final long[] mk_tokenSet_17() {
		long[] data = { 72057594037927936L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_17 = new BitSet(mk_tokenSet_17());
	private static final long[] mk_tokenSet_18() {
		long[] data = { 3963167672203476992L, 1090510761L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_18 = new BitSet(mk_tokenSet_18());
	private static final long[] mk_tokenSet_19() {
		long[] data = { -648518346223910912L, 1090519039L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_19 = new BitSet(mk_tokenSet_19());
	private static final long[] mk_tokenSet_20() {
		long[] data = { -648518346223910912L, 1291845631L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_20 = new BitSet(mk_tokenSet_20());
	private static final long[] mk_tokenSet_21() {
		long[] data = { 1657324662989783040L, 15728640L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_21 = new BitSet(mk_tokenSet_21());
	private static final long[] mk_tokenSet_22() {
		long[] data = { 1657324662989783040L, 15729152L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_22 = new BitSet(mk_tokenSet_22());
	private static final long[] mk_tokenSet_23() {
		long[] data = { 1657324662989783040L, 15729408L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_23 = new BitSet(mk_tokenSet_23());
	private static final long[] mk_tokenSet_24() {
		long[] data = { 1657324662989783040L, 15736576L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_24 = new BitSet(mk_tokenSet_24());
	private static final long[] mk_tokenSet_25() {
		long[] data = { 1657324662989783040L, 15785728L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_25 = new BitSet(mk_tokenSet_25());
	private static final long[] mk_tokenSet_26() {
		long[] data = { 1657324662989783040L, 16768768L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_26 = new BitSet(mk_tokenSet_26());
	private static final long[] mk_tokenSet_27() {
		long[] data = { 3963167672203476992L, 1090510593L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_27 = new BitSet(mk_tokenSet_27());
	private static final long[] mk_tokenSet_28() {
		long[] data = { -9186850658576236544L, 247799480325L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_28 = new BitSet(mk_tokenSet_28());
	private static final long[] mk_tokenSet_29() {
		long[] data = { 36521378278539264L, 247765925889L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_29 = new BitSet(mk_tokenSet_29());
	private static final long[] mk_tokenSet_30() {
		long[] data = { 36521378228207616L, 246960619520L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_30 = new BitSet(mk_tokenSet_30());
	private static final long[] mk_tokenSet_31() {
		long[] data = { 1152921504606846976L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_31 = new BitSet(mk_tokenSet_31());
	private static final long[] mk_tokenSet_32() {
		long[] data = { 1152921504606846976L, 8388608L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_32 = new BitSet(mk_tokenSet_32());
	
	}
