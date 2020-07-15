// Generated from TemplateGrammar.g4 by ANTLR 4.8

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
	static { RuntimeMetaData.checkVersion("4.8", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		STARTEXPR=1, TEXT=2, RBRACE=3, PIPE=4, DOT=5, LPAREN=6, RPAREN=7, LBRACK=8, 
		RBRACK=9, DQUESTION=10, SEMI=11, COMMA=12, ID=13, DSTRING=14, SSTRING=15, 
		DECDIGITS=16, FLOAT=17, ERR_CHAR=18;
	public static final int
		RULE_template = 0, RULE_elements = 1, RULE_element = 2, RULE_text = 3, 
		RULE_expression = 4, RULE_defaultValue = 5, RULE_defaultValueType = 6, 
		RULE_function = 7, RULE_functionArgs = 8, RULE_functionArg = 9, RULE_orExps = 10, 
		RULE_exps = 11, RULE_exp = 12;
	private static String[] makeRuleNames() {
		return new String[] {
			"template", "elements", "element", "text", "expression", "defaultValue", 
			"defaultValueType", "function", "functionArgs", "functionArg", "orExps", 
			"exps", "exp"
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
			null, "STARTEXPR", "TEXT", "RBRACE", "PIPE", "DOT", "LPAREN", "RPAREN", 
			"LBRACK", "RBRACK", "DQUESTION", "SEMI", "COMMA", "ID", "DSTRING", "SSTRING", 
			"DECDIGITS", "FLOAT", "ERR_CHAR"
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
		public AstRoot rootAst;
		public ElementsContext es;
		public TerminalNode EOF() { return getToken(TemplateGrammar.EOF, 0); }
		public ElementsContext elements() {
			return getRuleContext(ElementsContext.class,0);
		}
		public TemplateContext(ParserRuleContext parent, int invokingState) { super(parent, invokingState); }
		public TemplateContext(ParserRuleContext parent, int invokingState, TemplateType parentType) {
			super(parent, invokingState);
			this.parentType = parentType;
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

	public final TemplateContext template(TemplateType parentType) throws RecognitionException {
		TemplateContext _localctx = new TemplateContext(_ctx, getState(), parentType);
		enterRule(_localctx, 0, RULE_template);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(26);
			((TemplateContext)_localctx).es = elements(parentType);
			 ((TemplateContext)_localctx).rootAst =  new AstRoot(_localctx.parentType); _localctx.rootAst.addChildren(((TemplateContext)_localctx).es.list); 
			setState(28);
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
		public ArrayList<Ast> list = new ArrayList<>();
		public ElementContext e;
		public List<ElementContext> element() {
			return getRuleContexts(ElementContext.class);
		}
		public ElementContext element(int i) {
			return getRuleContext(ElementContext.class,i);
		}
		public ElementsContext(ParserRuleContext parent, int invokingState) { super(parent, invokingState); }
		public ElementsContext(ParserRuleContext parent, int invokingState, TemplateType parentType) {
			super(parent, invokingState);
			this.parentType = parentType;
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

	public final ElementsContext elements(TemplateType parentType) throws RecognitionException {
		ElementsContext _localctx = new ElementsContext(_ctx, getState(), parentType);
		enterRule(_localctx, 2, RULE_elements);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(35);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==STARTEXPR || _la==TEXT) {
				{
				{
				setState(30);
				((ElementsContext)_localctx).e = element(parentType);
				 _localctx.list.add(((ElementsContext)_localctx).e.ast); 
				}
				}
				setState(37);
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
		public Ast ast;
		public TextContext t;
		public ExpressionContext e;
		public TextContext text() {
			return getRuleContext(TextContext.class,0);
		}
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public ElementContext(ParserRuleContext parent, int invokingState) { super(parent, invokingState); }
		public ElementContext(ParserRuleContext parent, int invokingState, TemplateType parentType) {
			super(parent, invokingState);
			this.parentType = parentType;
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

	public final ElementContext element(TemplateType parentType) throws RecognitionException {
		ElementContext _localctx = new ElementContext(_ctx, getState(), parentType);
		enterRule(_localctx, 4, RULE_element);
		try {
			setState(44);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case TEXT:
				enterOuterAlt(_localctx, 1);
				{
				setState(38);
				((ElementContext)_localctx).t = text();
				 ((ElementContext)_localctx).ast =  new AstText((((ElementContext)_localctx).t!=null?_input.getText(((ElementContext)_localctx).t.start,((ElementContext)_localctx).t.stop):null)); _localctx.ast.addChild(new AstPrint(_localctx.ast.type, null)); 
				}
				break;
			case STARTEXPR:
				enterOuterAlt(_localctx, 2);
				{
				setState(41);
				((ElementContext)_localctx).e = expression(parentType);
				 ((ElementContext)_localctx).ast =  ((ElementContext)_localctx).e.ast.top; 
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
			setState(47); 
			_errHandler.sync(this);
			_alt = 1;
			do {
				switch (_alt) {
				case 1:
					{
					{
					setState(46);
					match(TEXT);
					}
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(49); 
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

	public static class ExpressionContext extends ParserRuleContext {
		public TemplateType parentType;
		public MinMax ast;
		public ExpsContext exps;
		public OrExpsContext orExps;
		public DefaultValueContext defaultValue;
		public FunctionContext function;
		public TerminalNode STARTEXPR() { return getToken(TemplateGrammar.STARTEXPR, 0); }
		public ExpsContext exps() {
			return getRuleContext(ExpsContext.class,0);
		}
		public OrExpsContext orExps() {
			return getRuleContext(OrExpsContext.class,0);
		}
		public TerminalNode RBRACE() { return getToken(TemplateGrammar.RBRACE, 0); }
		public DefaultValueContext defaultValue() {
			return getRuleContext(DefaultValueContext.class,0);
		}
		public FunctionContext function() {
			return getRuleContext(FunctionContext.class,0);
		}
		public ExpressionContext(ParserRuleContext parent, int invokingState) { super(parent, invokingState); }
		public ExpressionContext(ParserRuleContext parent, int invokingState, TemplateType parentType) {
			super(parent, invokingState);
			this.parentType = parentType;
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

	public final ExpressionContext expression(TemplateType parentType) throws RecognitionException {
		ExpressionContext _localctx = new ExpressionContext(_ctx, getState(), parentType);
		enterRule(_localctx, 8, RULE_expression);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(51);
			match(STARTEXPR);
			setState(52);
			((ExpressionContext)_localctx).exps = exps(parentType);
			 ((ExpressionContext)_localctx).ast =  ((ExpressionContext)_localctx).exps.ast; 
			setState(54);
			((ExpressionContext)_localctx).orExps = orExps(parentType, _localctx.ast);
			 ((ExpressionContext)_localctx).ast =  ((ExpressionContext)_localctx).orExps.ast; 
			setState(57);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==DQUESTION) {
				{
				setState(56);
				((ExpressionContext)_localctx).defaultValue = defaultValue();
				}
			}

			setState(60);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==SEMI) {
				{
				setState(59);
				((ExpressionContext)_localctx).function = function();
				}
			}


			        if( ((ExpressionContext)_localctx).function != null ) {
			          _localctx.ast.addToBottomChildrenAndSet( ((ExpressionContext)_localctx).function.func );
			        }

			        var ap = getAst(_localctx.ast.bottom.type, null, false, ((ExpressionContext)_localctx).defaultValue != null ? ((ExpressionContext)_localctx).defaultValue.v : null);
			        _localctx.ast.addToBottomChildrenAndSet( ap );
			      
			setState(63);
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

	public static class DefaultValueContext extends ParserRuleContext {
		public String v;
		public DefaultValueTypeContext defaultValueType;
		public TerminalNode DQUESTION() { return getToken(TemplateGrammar.DQUESTION, 0); }
		public DefaultValueTypeContext defaultValueType() {
			return getRuleContext(DefaultValueTypeContext.class,0);
		}
		public DefaultValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_defaultValue; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarListener ) ((TemplateGrammarListener)listener).enterDefaultValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarListener ) ((TemplateGrammarListener)listener).exitDefaultValue(this);
		}
	}

	public final DefaultValueContext defaultValue() throws RecognitionException {
		DefaultValueContext _localctx = new DefaultValueContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_defaultValue);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(65);
			match(DQUESTION);
			setState(66);
			((DefaultValueContext)_localctx).defaultValueType = defaultValueType();
			 ((DefaultValueContext)_localctx).v =  ((DefaultValueContext)_localctx).defaultValueType.v; 
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

	public static class DefaultValueTypeContext extends ParserRuleContext {
		public String v;
		public Token SSTRING;
		public Token DSTRING;
		public Token DECDIGITS;
		public Token FLOAT;
		public TerminalNode SSTRING() { return getToken(TemplateGrammar.SSTRING, 0); }
		public TerminalNode DSTRING() { return getToken(TemplateGrammar.DSTRING, 0); }
		public TerminalNode DECDIGITS() { return getToken(TemplateGrammar.DECDIGITS, 0); }
		public TerminalNode FLOAT() { return getToken(TemplateGrammar.FLOAT, 0); }
		public DefaultValueTypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_defaultValueType; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarListener ) ((TemplateGrammarListener)listener).enterDefaultValueType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarListener ) ((TemplateGrammarListener)listener).exitDefaultValueType(this);
		}
	}

	public final DefaultValueTypeContext defaultValueType() throws RecognitionException {
		DefaultValueTypeContext _localctx = new DefaultValueTypeContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_defaultValueType);
		try {
			setState(77);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case SSTRING:
				enterOuterAlt(_localctx, 1);
				{
				setState(69);
				((DefaultValueTypeContext)_localctx).SSTRING = match(SSTRING);
				 ((DefaultValueTypeContext)_localctx).v =  sStringToDString( (((DefaultValueTypeContext)_localctx).SSTRING!=null?((DefaultValueTypeContext)_localctx).SSTRING.getText():null) ); 
				}
				break;
			case DSTRING:
				enterOuterAlt(_localctx, 2);
				{
				setState(71);
				((DefaultValueTypeContext)_localctx).DSTRING = match(DSTRING);
				 ((DefaultValueTypeContext)_localctx).v =  (((DefaultValueTypeContext)_localctx).DSTRING!=null?((DefaultValueTypeContext)_localctx).DSTRING.getText():null); 
				}
				break;
			case DECDIGITS:
				enterOuterAlt(_localctx, 3);
				{
				setState(73);
				((DefaultValueTypeContext)_localctx).DECDIGITS = match(DECDIGITS);
				 ((DefaultValueTypeContext)_localctx).v =  (((DefaultValueTypeContext)_localctx).DECDIGITS!=null?((DefaultValueTypeContext)_localctx).DECDIGITS.getText():null); 
				}
				break;
			case FLOAT:
				enterOuterAlt(_localctx, 4);
				{
				setState(75);
				((DefaultValueTypeContext)_localctx).FLOAT = match(FLOAT);
				 ((DefaultValueTypeContext)_localctx).v =  (((DefaultValueTypeContext)_localctx).FLOAT!=null?((DefaultValueTypeContext)_localctx).FLOAT.getText():null); 
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

	public static class FunctionContext extends ParserRuleContext {
		public Ast func;
		public Token ID;
		public FunctionArgsContext functionArgs;
		public TerminalNode SEMI() { return getToken(TemplateGrammar.SEMI, 0); }
		public TerminalNode ID() { return getToken(TemplateGrammar.ID, 0); }
		public TerminalNode LPAREN() { return getToken(TemplateGrammar.LPAREN, 0); }
		public TerminalNode RPAREN() { return getToken(TemplateGrammar.RPAREN, 0); }
		public FunctionArgsContext functionArgs() {
			return getRuleContext(FunctionArgsContext.class,0);
		}
		public FunctionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_function; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarListener ) ((TemplateGrammarListener)listener).enterFunction(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarListener ) ((TemplateGrammarListener)listener).exitFunction(this);
		}
	}

	public final FunctionContext function() throws RecognitionException {
		FunctionContext _localctx = new FunctionContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_function);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(79);
			match(SEMI);
			setState(80);
			((FunctionContext)_localctx).ID = match(ID);
			setState(81);
			match(LPAREN);
			setState(83);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << DSTRING) | (1L << SSTRING) | (1L << DECDIGITS))) != 0)) {
				{
				setState(82);
				((FunctionContext)_localctx).functionArgs = functionArgs();
				}
			}

			setState(85);
			match(RPAREN);
			 ((FunctionContext)_localctx).func =  getFunction( (((FunctionContext)_localctx).ID!=null?((FunctionContext)_localctx).ID.getText():null), ((FunctionContext)_localctx).functionArgs != null ? ((FunctionContext)_localctx).functionArgs.ret : List.of() ); 
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

	public static class FunctionArgsContext extends ParserRuleContext {
		public ArrayList<String> ret = new ArrayList<>();
		public FunctionArgContext fa;
		public FunctionArgContext nfa;
		public List<FunctionArgContext> functionArg() {
			return getRuleContexts(FunctionArgContext.class);
		}
		public FunctionArgContext functionArg(int i) {
			return getRuleContext(FunctionArgContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(TemplateGrammar.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(TemplateGrammar.COMMA, i);
		}
		public FunctionArgsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_functionArgs; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarListener ) ((TemplateGrammarListener)listener).enterFunctionArgs(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarListener ) ((TemplateGrammarListener)listener).exitFunctionArgs(this);
		}
	}

	public final FunctionArgsContext functionArgs() throws RecognitionException {
		FunctionArgsContext _localctx = new FunctionArgsContext(_ctx, getState());
		enterRule(_localctx, 16, RULE_functionArgs);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(88);
			((FunctionArgsContext)_localctx).fa = functionArg();
			 _localctx.ret.add( ((FunctionArgsContext)_localctx).fa.ret ); 
			setState(96);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(90);
				match(COMMA);
				setState(91);
				((FunctionArgsContext)_localctx).nfa = functionArg();
				 _localctx.ret.add( ((FunctionArgsContext)_localctx).nfa.ret ); 
				}
				}
				setState(98);
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

	public static class FunctionArgContext extends ParserRuleContext {
		public String ret;
		public Token DECDIGITS;
		public Token SSTRING;
		public Token DSTRING;
		public TerminalNode DECDIGITS() { return getToken(TemplateGrammar.DECDIGITS, 0); }
		public TerminalNode SSTRING() { return getToken(TemplateGrammar.SSTRING, 0); }
		public TerminalNode DSTRING() { return getToken(TemplateGrammar.DSTRING, 0); }
		public FunctionArgContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_functionArg; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarListener ) ((TemplateGrammarListener)listener).enterFunctionArg(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarListener ) ((TemplateGrammarListener)listener).exitFunctionArg(this);
		}
	}

	public final FunctionArgContext functionArg() throws RecognitionException {
		FunctionArgContext _localctx = new FunctionArgContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_functionArg);
		try {
			setState(105);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case DECDIGITS:
				enterOuterAlt(_localctx, 1);
				{
				setState(99);
				((FunctionArgContext)_localctx).DECDIGITS = match(DECDIGITS);
				 ((FunctionArgContext)_localctx).ret =  (((FunctionArgContext)_localctx).DECDIGITS!=null?((FunctionArgContext)_localctx).DECDIGITS.getText():null); 
				}
				break;
			case SSTRING:
				enterOuterAlt(_localctx, 2);
				{
				setState(101);
				((FunctionArgContext)_localctx).SSTRING = match(SSTRING);
				 ((FunctionArgContext)_localctx).ret =  sStringToDString( (((FunctionArgContext)_localctx).SSTRING!=null?((FunctionArgContext)_localctx).SSTRING.getText():null) ); 
				}
				break;
			case DSTRING:
				enterOuterAlt(_localctx, 3);
				{
				setState(103);
				((FunctionArgContext)_localctx).DSTRING = match(DSTRING);
				 ((FunctionArgContext)_localctx).ret =  (((FunctionArgContext)_localctx).DSTRING!=null?((FunctionArgContext)_localctx).DSTRING.getText():null); 
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

	public static class OrExpsContext extends ParserRuleContext {
		public TemplateType parentType;
		public MinMax firstAst;
		public MinMax ast;
		public ArrayList<MinMax> list = new ArrayList<>();;
		public ExpsContext e1;
		public ExpsContext e2;
		public List<TerminalNode> PIPE() { return getTokens(TemplateGrammar.PIPE); }
		public TerminalNode PIPE(int i) {
			return getToken(TemplateGrammar.PIPE, i);
		}
		public List<ExpsContext> exps() {
			return getRuleContexts(ExpsContext.class);
		}
		public ExpsContext exps(int i) {
			return getRuleContext(ExpsContext.class,i);
		}
		public OrExpsContext(ParserRuleContext parent, int invokingState) { super(parent, invokingState); }
		public OrExpsContext(ParserRuleContext parent, int invokingState, TemplateType parentType, MinMax firstAst) {
			super(parent, invokingState);
			this.parentType = parentType;
			this.firstAst = firstAst;
		}
		@Override public int getRuleIndex() { return RULE_orExps; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarListener ) ((TemplateGrammarListener)listener).enterOrExps(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarListener ) ((TemplateGrammarListener)listener).exitOrExps(this);
		}
	}

	public final OrExpsContext orExps(TemplateType parentType,MinMax firstAst) throws RecognitionException {
		OrExpsContext _localctx = new OrExpsContext(_ctx, getState(), parentType, firstAst);
		enterRule(_localctx, 20, RULE_orExps);
		int _la;
		try {
			setState(122);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case PIPE:
				enterOuterAlt(_localctx, 1);
				{
				{
				setState(107);
				match(PIPE);
				setState(108);
				((OrExpsContext)_localctx).e1 = exps(parentType);
				 _localctx.list.add(firstAst); _localctx.list.add(((OrExpsContext)_localctx).e1.ast); 
				setState(116);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==PIPE) {
					{
					{
					setState(110);
					match(PIPE);
					setState(111);
					((OrExpsContext)_localctx).e2 = exps(parentType);
					_localctx.list.add(((OrExpsContext)_localctx).e2.ast);
					}
					}
					setState(118);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}

				        if( _localctx.list.isEmpty() ) ((OrExpsContext)_localctx).ast =  firstAst;
				        else {
				            var or = new AstOr(parentType);
				            for( var item : _localctx.list) {
				              item.addToBottomChildrenAndSet(getAst(item.bottom.type, null, false));
				            }
				            or.addTry(_localctx.list);
				            ((OrExpsContext)_localctx).ast =  new MinMax(or);
				        }    
				    
				}
				break;
			case RBRACE:
			case DQUESTION:
			case SEMI:
				enterOuterAlt(_localctx, 2);
				{
				 ((OrExpsContext)_localctx).ast =  firstAst; 
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

	public static class ExpsContext extends ParserRuleContext {
		public TemplateType parentType;
		public MinMax ast;
		public ExpContext id;
		public ExpContext nid;
		public List<ExpContext> exp() {
			return getRuleContexts(ExpContext.class);
		}
		public ExpContext exp(int i) {
			return getRuleContext(ExpContext.class,i);
		}
		public List<TerminalNode> DOT() { return getTokens(TemplateGrammar.DOT); }
		public TerminalNode DOT(int i) {
			return getToken(TemplateGrammar.DOT, i);
		}
		public ExpsContext(ParserRuleContext parent, int invokingState) { super(parent, invokingState); }
		public ExpsContext(ParserRuleContext parent, int invokingState, TemplateType parentType) {
			super(parent, invokingState);
			this.parentType = parentType;
		}
		@Override public int getRuleIndex() { return RULE_exps; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarListener ) ((TemplateGrammarListener)listener).enterExps(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarListener ) ((TemplateGrammarListener)listener).exitExps(this);
		}
	}

	public final ExpsContext exps(TemplateType parentType) throws RecognitionException {
		ExpsContext _localctx = new ExpsContext(_ctx, getState(), parentType);
		enterRule(_localctx, 22, RULE_exps);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			{
			setState(124);
			((ExpsContext)_localctx).id = exp(parentType);
			 ((ExpsContext)_localctx).ast =  ((ExpsContext)_localctx).id.ast; 
			setState(132);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==DOT) {
				{
				{
				setState(126);
				match(DOT);
				setState(127);
				((ExpsContext)_localctx).nid = exp(_localctx.ast.bottom.type);
				_localctx.ast.addToBottomChildrenAndSet(((ExpsContext)_localctx).nid.ast);
				}
				}
				setState(134);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
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

	public static class ExpContext extends ParserRuleContext {
		public TemplateType parentType;
		public MinMax ast;
		public Token id;
		public TerminalNode LPAREN() { return getToken(TemplateGrammar.LPAREN, 0); }
		public TerminalNode RPAREN() { return getToken(TemplateGrammar.RPAREN, 0); }
		public TerminalNode ID() { return getToken(TemplateGrammar.ID, 0); }
		public ExpContext(ParserRuleContext parent, int invokingState) { super(parent, invokingState); }
		public ExpContext(ParserRuleContext parent, int invokingState, TemplateType parentType) {
			super(parent, invokingState);
			this.parentType = parentType;
		}
		@Override public int getRuleIndex() { return RULE_exp; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarListener ) ((TemplateGrammarListener)listener).enterExp(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarListener ) ((TemplateGrammarListener)listener).exitExp(this);
		}
	}

	public final ExpContext exp(TemplateType parentType) throws RecognitionException {
		ExpContext _localctx = new ExpContext(_ctx, getState(), parentType);
		enterRule(_localctx, 24, RULE_exp);
		try {
			setState(142);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,12,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				{
				setState(135);
				((ExpContext)_localctx).id = match(ID);
				setState(136);
				match(LPAREN);
				setState(137);
				match(RPAREN);
				}
				 ((ExpContext)_localctx).ast =  getAst(_localctx.parentType, (((ExpContext)_localctx).id!=null?((ExpContext)_localctx).id.getText():null), true); 
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(140);
				((ExpContext)_localctx).id = match(ID);
				 ((ExpContext)_localctx).ast =  getAst(_localctx.parentType, (((ExpContext)_localctx).id!=null?((ExpContext)_localctx).id.getText():null), false); 
				}
				break;
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
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\3\24\u0093\4\2\t\2"+
		"\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13"+
		"\t\13\4\f\t\f\4\r\t\r\4\16\t\16\3\2\3\2\3\2\3\2\3\3\3\3\3\3\7\3$\n\3\f"+
		"\3\16\3\'\13\3\3\4\3\4\3\4\3\4\3\4\3\4\5\4/\n\4\3\5\6\5\62\n\5\r\5\16"+
		"\5\63\3\6\3\6\3\6\3\6\3\6\3\6\5\6<\n\6\3\6\5\6?\n\6\3\6\3\6\3\6\3\7\3"+
		"\7\3\7\3\7\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\5\bP\n\b\3\t\3\t\3\t\3\t\5"+
		"\tV\n\t\3\t\3\t\3\t\3\n\3\n\3\n\3\n\3\n\3\n\7\na\n\n\f\n\16\nd\13\n\3"+
		"\13\3\13\3\13\3\13\3\13\3\13\5\13l\n\13\3\f\3\f\3\f\3\f\3\f\3\f\3\f\7"+
		"\fu\n\f\f\f\16\fx\13\f\3\f\3\f\3\f\5\f}\n\f\3\r\3\r\3\r\3\r\3\r\3\r\7"+
		"\r\u0085\n\r\f\r\16\r\u0088\13\r\3\16\3\16\3\16\3\16\3\16\3\16\3\16\5"+
		"\16\u0091\n\16\3\16\2\2\17\2\4\6\b\n\f\16\20\22\24\26\30\32\2\2\2\u0095"+
		"\2\34\3\2\2\2\4%\3\2\2\2\6.\3\2\2\2\b\61\3\2\2\2\n\65\3\2\2\2\fC\3\2\2"+
		"\2\16O\3\2\2\2\20Q\3\2\2\2\22Z\3\2\2\2\24k\3\2\2\2\26|\3\2\2\2\30~\3\2"+
		"\2\2\32\u0090\3\2\2\2\34\35\5\4\3\2\35\36\b\2\1\2\36\37\7\2\2\3\37\3\3"+
		"\2\2\2 !\5\6\4\2!\"\b\3\1\2\"$\3\2\2\2# \3\2\2\2$\'\3\2\2\2%#\3\2\2\2"+
		"%&\3\2\2\2&\5\3\2\2\2\'%\3\2\2\2()\5\b\5\2)*\b\4\1\2*/\3\2\2\2+,\5\n\6"+
		"\2,-\b\4\1\2-/\3\2\2\2.(\3\2\2\2.+\3\2\2\2/\7\3\2\2\2\60\62\7\4\2\2\61"+
		"\60\3\2\2\2\62\63\3\2\2\2\63\61\3\2\2\2\63\64\3\2\2\2\64\t\3\2\2\2\65"+
		"\66\7\3\2\2\66\67\5\30\r\2\678\b\6\1\289\5\26\f\29;\b\6\1\2:<\5\f\7\2"+
		";:\3\2\2\2;<\3\2\2\2<>\3\2\2\2=?\5\20\t\2>=\3\2\2\2>?\3\2\2\2?@\3\2\2"+
		"\2@A\b\6\1\2AB\7\5\2\2B\13\3\2\2\2CD\7\f\2\2DE\5\16\b\2EF\b\7\1\2F\r\3"+
		"\2\2\2GH\7\21\2\2HP\b\b\1\2IJ\7\20\2\2JP\b\b\1\2KL\7\22\2\2LP\b\b\1\2"+
		"MN\7\23\2\2NP\b\b\1\2OG\3\2\2\2OI\3\2\2\2OK\3\2\2\2OM\3\2\2\2P\17\3\2"+
		"\2\2QR\7\r\2\2RS\7\17\2\2SU\7\b\2\2TV\5\22\n\2UT\3\2\2\2UV\3\2\2\2VW\3"+
		"\2\2\2WX\7\t\2\2XY\b\t\1\2Y\21\3\2\2\2Z[\5\24\13\2[b\b\n\1\2\\]\7\16\2"+
		"\2]^\5\24\13\2^_\b\n\1\2_a\3\2\2\2`\\\3\2\2\2ad\3\2\2\2b`\3\2\2\2bc\3"+
		"\2\2\2c\23\3\2\2\2db\3\2\2\2ef\7\22\2\2fl\b\13\1\2gh\7\21\2\2hl\b\13\1"+
		"\2ij\7\20\2\2jl\b\13\1\2ke\3\2\2\2kg\3\2\2\2ki\3\2\2\2l\25\3\2\2\2mn\7"+
		"\6\2\2no\5\30\r\2ov\b\f\1\2pq\7\6\2\2qr\5\30\r\2rs\b\f\1\2su\3\2\2\2t"+
		"p\3\2\2\2ux\3\2\2\2vt\3\2\2\2vw\3\2\2\2wy\3\2\2\2xv\3\2\2\2yz\b\f\1\2"+
		"z}\3\2\2\2{}\b\f\1\2|m\3\2\2\2|{\3\2\2\2}\27\3\2\2\2~\177\5\32\16\2\177"+
		"\u0086\b\r\1\2\u0080\u0081\7\7\2\2\u0081\u0082\5\32\16\2\u0082\u0083\b"+
		"\r\1\2\u0083\u0085\3\2\2\2\u0084\u0080\3\2\2\2\u0085\u0088\3\2\2\2\u0086"+
		"\u0084\3\2\2\2\u0086\u0087\3\2\2\2\u0087\31\3\2\2\2\u0088\u0086\3\2\2"+
		"\2\u0089\u008a\7\17\2\2\u008a\u008b\7\b\2\2\u008b\u008c\7\t\2\2\u008c"+
		"\u008d\3\2\2\2\u008d\u0091\b\16\1\2\u008e\u008f\7\17\2\2\u008f\u0091\b"+
		"\16\1\2\u0090\u0089\3\2\2\2\u0090\u008e\3\2\2\2\u0091\33\3\2\2\2\17%."+
		"\63;>OUbkv|\u0086\u0090";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}