// Generated from TemplateGrammar.g4 by ANTLR 4.13.0

package oap.template;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast", "CheckReturnValue"})
public class TemplateGrammar extends TemplateGrammarAdaptor {
	static { RuntimeMetaData.checkVersion("4.13.0", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		STARTESCEXPR=1, STARTEXPR=2, TEXT=3, LBRACE=4, RBRACE=5, EXPRESSION=6;
	public static final int
		RULE_template = 0, RULE_elements = 1, RULE_element = 2, RULE_text = 3, 
		RULE_comment = 4, RULE_expression = 5, RULE_expressionContent = 6;
	private static String[] makeRuleNames() {
		return new String[] {
			"template", "elements", "element", "text", "comment", "expression", "expressionContent"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "STARTESCEXPR", "STARTEXPR", "TEXT", "LBRACE", "RBRACE", "EXPRESSION"
		};
	}
	private static final String[] _SYMBOLIC_NAMES = makeSymbolicNames();
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}

	@Override
	public String getGrammarFileName() { return "TemplateGrammar.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }


		public TemplateGrammar(TokenStream input, Map<String, List<Method>> builtInFunction, ErrorStrategy errorStrategy) {
			this(input);
			
			this.builtInFunction = builtInFunction;
			this.errorStrategy = errorStrategy;
		}


	public TemplateGrammar(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@SuppressWarnings("CheckReturnValue")
	public static class TemplateContext extends ParserRuleContext {
		public TemplateType parentType;
		public Map<String,String> aliases;
		public AstRoot rootAst;
		public ElementsContext elements;
		public ElementsContext elements() {
			return getRuleContext(ElementsContext.class,0);
		}
		public TerminalNode EOF() { return getToken(TemplateGrammar.EOF, 0); }
		public TemplateContext(ParserRuleContext parent, int invokingState) { super(parent, invokingState); }
		public TemplateContext(ParserRuleContext parent, int invokingState, TemplateType parentType, Map<String,String> aliases) {
			super(parent, invokingState);
			this.parentType = parentType;
			this.aliases = aliases;
		}
		@Override public int getRuleIndex() { return RULE_template; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarListener ) ((TemplateGrammarListener)listener).enterTemplate(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarListener ) ((TemplateGrammarListener)listener).exitTemplate(this);
		}
	}

	public final TemplateContext template(TemplateType parentType,Map<String,String> aliases) throws RecognitionException {
		TemplateContext _localctx = new TemplateContext(_ctx, getState(), parentType, aliases);
		enterRule(_localctx, 0, RULE_template);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(14);
			((TemplateContext)_localctx).elements = elements(parentType, aliases);
			 ((TemplateContext)_localctx).rootAst =  new AstRoot(_localctx.parentType); _localctx.rootAst.addChildren(((TemplateContext)_localctx).elements.list); 
			setState(16);
			match(EOF);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ElementsContext extends ParserRuleContext {
		public TemplateType parentType;
		public Map<String,String> aliases;
		public ArrayList<Ast> list = new ArrayList<>();
		public ElementContext element;
		public List<ElementContext> element() {
			return getRuleContexts(ElementContext.class);
		}
		public ElementContext element(int i) {
			return getRuleContext(ElementContext.class,i);
		}
		public ElementsContext(ParserRuleContext parent, int invokingState) { super(parent, invokingState); }
		public ElementsContext(ParserRuleContext parent, int invokingState, TemplateType parentType, Map<String,String> aliases) {
			super(parent, invokingState);
			this.parentType = parentType;
			this.aliases = aliases;
		}
		@Override public int getRuleIndex() { return RULE_elements; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarListener ) ((TemplateGrammarListener)listener).enterElements(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarListener ) ((TemplateGrammarListener)listener).exitElements(this);
		}
	}

	public final ElementsContext elements(TemplateType parentType,Map<String,String> aliases) throws RecognitionException {
		ElementsContext _localctx = new ElementsContext(_ctx, getState(), parentType, aliases);
		enterRule(_localctx, 2, RULE_elements);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(23);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 14L) != 0)) {
				{
				{
				setState(18);
				((ElementsContext)_localctx).element = element(parentType, aliases);
				 _localctx.list.add(((ElementsContext)_localctx).element.ast); 
				}
				}
				setState(25);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ElementContext extends ParserRuleContext {
		public TemplateType parentType;
		public Map<String,String> aliases;
		public Ast ast;
		public TextContext t;
		public CommentContext comment;
		public ExpressionContext expression;
		public TextContext text() {
			return getRuleContext(TextContext.class,0);
		}
		public CommentContext comment() {
			return getRuleContext(CommentContext.class,0);
		}
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public ElementContext(ParserRuleContext parent, int invokingState) { super(parent, invokingState); }
		public ElementContext(ParserRuleContext parent, int invokingState, TemplateType parentType, Map<String,String> aliases) {
			super(parent, invokingState);
			this.parentType = parentType;
			this.aliases = aliases;
		}
		@Override public int getRuleIndex() { return RULE_element; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarListener ) ((TemplateGrammarListener)listener).enterElement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarListener ) ((TemplateGrammarListener)listener).exitElement(this);
		}
	}

	public final ElementContext element(TemplateType parentType,Map<String,String> aliases) throws RecognitionException {
		ElementContext _localctx = new ElementContext(_ctx, getState(), parentType, aliases);
		enterRule(_localctx, 4, RULE_element);
		try {
			setState(35);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case TEXT:
				enterOuterAlt(_localctx, 1);
				{
				setState(26);
				((ElementContext)_localctx).t = text();
				 ((ElementContext)_localctx).ast =  new AstText((((ElementContext)_localctx).t!=null?_input.getText(((ElementContext)_localctx).t.start,((ElementContext)_localctx).t.stop):null)); 
				}
				break;
			case STARTESCEXPR:
				enterOuterAlt(_localctx, 2);
				{
				setState(29);
				((ElementContext)_localctx).comment = comment();
				 ((ElementContext)_localctx).ast =  new AstText((((ElementContext)_localctx).comment!=null?_input.getText(((ElementContext)_localctx).comment.start,((ElementContext)_localctx).comment.stop):null).substring(1));
				}
				break;
			case STARTEXPR:
				enterOuterAlt(_localctx, 3);
				{
				setState(32);
				((ElementContext)_localctx).expression = expression(aliases);

				        var lexerExp = new TemplateLexerExpression( CharStreams.fromString( ((ElementContext)_localctx).expression.content ) );
				        var grammarExp = new TemplateGrammarExpression( new BufferedTokenStream( lexerExp ), builtInFunction, errorStrategy );
				        if( errorStrategy == ErrorStrategy.ERROR ) {
				            lexerExp.addErrorListener( ThrowingErrorListener.INSTANCE );
				            grammarExp.addErrorListener( ThrowingErrorListener.INSTANCE );
				        }
				        
					    try { 
				            ((ElementContext)_localctx).ast =  new AstExpression(grammarExp.expression( _localctx.parentType ).ast.top, ((ElementContext)_localctx).expression.content);
				        } catch ( TemplateException e ) {
				            var newException = new TemplateException( ((ElementContext)_localctx).expression.content, e.getCause() );
				            newException.setStackTrace( e.getStackTrace() );
				            throw newException;
				        } catch ( Exception e ) {
				            var newException = new TemplateException( ((ElementContext)_localctx).expression.content, e );
				            newException.setStackTrace( e.getStackTrace() );
				            throw newException;
				        }
					 
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class TextContext extends ParserRuleContext {
		public List<TerminalNode> TEXT() { return getTokens(TemplateGrammar.TEXT); }
		public TerminalNode TEXT(int i) {
			return getToken(TemplateGrammar.TEXT, i);
		}
		public TextContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_text; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarListener ) ((TemplateGrammarListener)listener).enterText(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarListener ) ((TemplateGrammarListener)listener).exitText(this);
		}
	}

	public final TextContext text() throws RecognitionException {
		TextContext _localctx = new TextContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_text);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(38); 
			_errHandler.sync(this);
			_alt = 1;
			do {
				switch (_alt) {
				case 1:
					{
					{
					setState(37);
					match(TEXT);
					}
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(40); 
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,2,_ctx);
			} while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER );
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class CommentContext extends ParserRuleContext {
		public TerminalNode STARTESCEXPR() { return getToken(TemplateGrammar.STARTESCEXPR, 0); }
		public ExpressionContentContext expressionContent() {
			return getRuleContext(ExpressionContentContext.class,0);
		}
		public TerminalNode RBRACE() { return getToken(TemplateGrammar.RBRACE, 0); }
		public CommentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_comment; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarListener ) ((TemplateGrammarListener)listener).enterComment(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarListener ) ((TemplateGrammarListener)listener).exitComment(this);
		}
	}

	public final CommentContext comment() throws RecognitionException {
		CommentContext _localctx = new CommentContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_comment);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(42);
			match(STARTESCEXPR);
			setState(43);
			expressionContent();
			setState(44);
			match(RBRACE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ExpressionContext extends ParserRuleContext {
		public Map<String,String> aliases;
		public String content;
		public ExpressionContentContext expressionContent;
		public TerminalNode STARTEXPR() { return getToken(TemplateGrammar.STARTEXPR, 0); }
		public ExpressionContentContext expressionContent() {
			return getRuleContext(ExpressionContentContext.class,0);
		}
		public TerminalNode RBRACE() { return getToken(TemplateGrammar.RBRACE, 0); }
		public ExpressionContext(ParserRuleContext parent, int invokingState) { super(parent, invokingState); }
		public ExpressionContext(ParserRuleContext parent, int invokingState, Map<String,String> aliases) {
			super(parent, invokingState);
			this.aliases = aliases;
		}
		@Override public int getRuleIndex() { return RULE_expression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarListener ) ((TemplateGrammarListener)listener).enterExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarListener ) ((TemplateGrammarListener)listener).exitExpression(this);
		}
	}

	public final ExpressionContext expression(Map<String,String> aliases) throws RecognitionException {
		ExpressionContext _localctx = new ExpressionContext(_ctx, getState(), aliases);
		enterRule(_localctx, 10, RULE_expression);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(46);
			match(STARTEXPR);
			setState(47);
			((ExpressionContext)_localctx).expressionContent = expressionContent();
			setState(48);
			match(RBRACE);
			 
			        ((ExpressionContext)_localctx).content =  (((ExpressionContext)_localctx).expressionContent!=null?_input.getText(((ExpressionContext)_localctx).expressionContent.start,((ExpressionContext)_localctx).expressionContent.stop):null);
			        var alias = aliases.get( _localctx.content );
			        if( alias != null ) ((ExpressionContext)_localctx).content =  alias;  
			    
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ExpressionContentContext extends ParserRuleContext {
		public List<TerminalNode> EXPRESSION() { return getTokens(TemplateGrammar.EXPRESSION); }
		public TerminalNode EXPRESSION(int i) {
			return getToken(TemplateGrammar.EXPRESSION, i);
		}
		public List<TerminalNode> LBRACE() { return getTokens(TemplateGrammar.LBRACE); }
		public TerminalNode LBRACE(int i) {
			return getToken(TemplateGrammar.LBRACE, i);
		}
		public List<TerminalNode> RBRACE() { return getTokens(TemplateGrammar.RBRACE); }
		public TerminalNode RBRACE(int i) {
			return getToken(TemplateGrammar.RBRACE, i);
		}
		public ExpressionContentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_expressionContent; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarListener ) ((TemplateGrammarListener)listener).enterExpressionContent(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarListener ) ((TemplateGrammarListener)listener).exitExpressionContent(this);
		}
	}

	public final ExpressionContentContext expressionContent() throws RecognitionException {
		ExpressionContentContext _localctx = new ExpressionContentContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_expressionContent);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(52); 
			_errHandler.sync(this);
			_alt = 1;
			do {
				switch (_alt) {
				case 1:
					{
					{
					setState(51);
					_la = _input.LA(1);
					if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 112L) != 0)) ) {
					_errHandler.recoverInline(this);
					}
					else {
						if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
						_errHandler.reportMatch(this);
						consume();
					}
					}
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(54); 
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,3,_ctx);
			} while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER );
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static final String _serializedATN =
		"\u0004\u0001\u00069\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001\u0002"+
		"\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004\u0007\u0004\u0002"+
		"\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0001\u0000\u0001\u0000\u0001"+
		"\u0000\u0001\u0000\u0001\u0001\u0001\u0001\u0001\u0001\u0005\u0001\u0016"+
		"\b\u0001\n\u0001\f\u0001\u0019\t\u0001\u0001\u0002\u0001\u0002\u0001\u0002"+
		"\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002"+
		"\u0003\u0002$\b\u0002\u0001\u0003\u0004\u0003\'\b\u0003\u000b\u0003\f"+
		"\u0003(\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0005\u0001"+
		"\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0006\u0004\u00065\b"+
		"\u0006\u000b\u0006\f\u00066\u0001\u0006\u0000\u0000\u0007\u0000\u0002"+
		"\u0004\u0006\b\n\f\u0000\u0001\u0001\u0000\u0004\u00066\u0000\u000e\u0001"+
		"\u0000\u0000\u0000\u0002\u0017\u0001\u0000\u0000\u0000\u0004#\u0001\u0000"+
		"\u0000\u0000\u0006&\u0001\u0000\u0000\u0000\b*\u0001\u0000\u0000\u0000"+
		"\n.\u0001\u0000\u0000\u0000\f4\u0001\u0000\u0000\u0000\u000e\u000f\u0003"+
		"\u0002\u0001\u0000\u000f\u0010\u0006\u0000\uffff\uffff\u0000\u0010\u0011"+
		"\u0005\u0000\u0000\u0001\u0011\u0001\u0001\u0000\u0000\u0000\u0012\u0013"+
		"\u0003\u0004\u0002\u0000\u0013\u0014\u0006\u0001\uffff\uffff\u0000\u0014"+
		"\u0016\u0001\u0000\u0000\u0000\u0015\u0012\u0001\u0000\u0000\u0000\u0016"+
		"\u0019\u0001\u0000\u0000\u0000\u0017\u0015\u0001\u0000\u0000\u0000\u0017"+
		"\u0018\u0001\u0000\u0000\u0000\u0018\u0003\u0001\u0000\u0000\u0000\u0019"+
		"\u0017\u0001\u0000\u0000\u0000\u001a\u001b\u0003\u0006\u0003\u0000\u001b"+
		"\u001c\u0006\u0002\uffff\uffff\u0000\u001c$\u0001\u0000\u0000\u0000\u001d"+
		"\u001e\u0003\b\u0004\u0000\u001e\u001f\u0006\u0002\uffff\uffff\u0000\u001f"+
		"$\u0001\u0000\u0000\u0000 !\u0003\n\u0005\u0000!\"\u0006\u0002\uffff\uffff"+
		"\u0000\"$\u0001\u0000\u0000\u0000#\u001a\u0001\u0000\u0000\u0000#\u001d"+
		"\u0001\u0000\u0000\u0000# \u0001\u0000\u0000\u0000$\u0005\u0001\u0000"+
		"\u0000\u0000%\'\u0005\u0003\u0000\u0000&%\u0001\u0000\u0000\u0000\'(\u0001"+
		"\u0000\u0000\u0000(&\u0001\u0000\u0000\u0000()\u0001\u0000\u0000\u0000"+
		")\u0007\u0001\u0000\u0000\u0000*+\u0005\u0001\u0000\u0000+,\u0003\f\u0006"+
		"\u0000,-\u0005\u0005\u0000\u0000-\t\u0001\u0000\u0000\u0000./\u0005\u0002"+
		"\u0000\u0000/0\u0003\f\u0006\u000001\u0005\u0005\u0000\u000012\u0006\u0005"+
		"\uffff\uffff\u00002\u000b\u0001\u0000\u0000\u000035\u0007\u0000\u0000"+
		"\u000043\u0001\u0000\u0000\u000056\u0001\u0000\u0000\u000064\u0001\u0000"+
		"\u0000\u000067\u0001\u0000\u0000\u00007\r\u0001\u0000\u0000\u0000\u0004"+
		"\u0017#(6";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}