// Generated from TemplateGrammar.g4 by ANTLR 4.9.3

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

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class TemplateGrammar extends TemplateGrammarAdaptor {
	static { RuntimeMetaData.checkVersion("4.9.3", RuntimeMetaData.VERSION); }

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
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << STARTESCEXPR) | (1L << STARTEXPR) | (1L << TEXT))) != 0)) {
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
					        MaxMin ast = grammarExp.expression( _localctx.parentType ).ast;
				            ((ElementContext)_localctx).ast =  new AstExpression(ast.top, ((ElementContext)_localctx).expression.content, ast.castType != null ? FieldType.parse( ast.castType ) : null );
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
					if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << LBRACE) | (1L << RBRACE) | (1L << EXPRESSION))) != 0)) ) {
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
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\3\b;\4\2\t\2\4\3\t"+
		"\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\3\2\3\2\3\2\3\2\3\3\3\3\3\3"+
		"\7\3\30\n\3\f\3\16\3\33\13\3\3\4\3\4\3\4\3\4\3\4\3\4\3\4\3\4\3\4\5\4&"+
		"\n\4\3\5\6\5)\n\5\r\5\16\5*\3\6\3\6\3\6\3\6\3\7\3\7\3\7\3\7\3\7\3\b\6"+
		"\b\67\n\b\r\b\16\b8\3\b\2\2\t\2\4\6\b\n\f\16\2\3\3\2\6\b\28\2\20\3\2\2"+
		"\2\4\31\3\2\2\2\6%\3\2\2\2\b(\3\2\2\2\n,\3\2\2\2\f\60\3\2\2\2\16\66\3"+
		"\2\2\2\20\21\5\4\3\2\21\22\b\2\1\2\22\23\7\2\2\3\23\3\3\2\2\2\24\25\5"+
		"\6\4\2\25\26\b\3\1\2\26\30\3\2\2\2\27\24\3\2\2\2\30\33\3\2\2\2\31\27\3"+
		"\2\2\2\31\32\3\2\2\2\32\5\3\2\2\2\33\31\3\2\2\2\34\35\5\b\5\2\35\36\b"+
		"\4\1\2\36&\3\2\2\2\37 \5\n\6\2 !\b\4\1\2!&\3\2\2\2\"#\5\f\7\2#$\b\4\1"+
		"\2$&\3\2\2\2%\34\3\2\2\2%\37\3\2\2\2%\"\3\2\2\2&\7\3\2\2\2\')\7\5\2\2"+
		"(\'\3\2\2\2)*\3\2\2\2*(\3\2\2\2*+\3\2\2\2+\t\3\2\2\2,-\7\3\2\2-.\5\16"+
		"\b\2./\7\7\2\2/\13\3\2\2\2\60\61\7\4\2\2\61\62\5\16\b\2\62\63\7\7\2\2"+
		"\63\64\b\7\1\2\64\r\3\2\2\2\65\67\t\2\2\2\66\65\3\2\2\2\678\3\2\2\28\66"+
		"\3\2\2\289\3\2\2\29\17\3\2\2\2\6\31%*8";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}