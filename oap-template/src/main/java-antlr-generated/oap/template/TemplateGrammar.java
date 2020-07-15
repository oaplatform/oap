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
		RBRACK=9, DQUESTION=10, SEMI=11, ID=12, DSTRING=13, SSTRING=14, ERR_CHAR=15, 
		FUNCTIONNAME=16, FADECDIGITS=17, FADSTRING=18, FASSTRING=19, FACOMMA=20, 
		FALPAREN=21, FARPAREN=22, FAERR_CHAR=23;
	public static final int
		RULE_template = 0, RULE_elements = 1, RULE_element = 2, RULE_text = 3, 
		RULE_expression = 4, RULE_defaultValue = 5, RULE_function = 6, RULE_functionArgs = 7, 
		RULE_functionArg = 8, RULE_orExps = 9, RULE_exps = 10, RULE_exp = 11;
	private static String[] makeRuleNames() {
		return new String[] {
			"template", "elements", "element", "text", "expression", "defaultValue", 
			"function", "functionArgs", "functionArg", "orExps", "exps", "exp"
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
			"LBRACK", "RBRACK", "DQUESTION", "SEMI", "ID", "DSTRING", "SSTRING", 
			"ERR_CHAR", "FUNCTIONNAME", "FADECDIGITS", "FADSTRING", "FASSTRING", 
			"FACOMMA", "FALPAREN", "FARPAREN", "FAERR_CHAR"
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
			setState(24);
			((TemplateContext)_localctx).es = elements(parentType);
			 ((TemplateContext)_localctx).rootAst =  new AstRoot(_localctx.parentType); _localctx.rootAst.addChildren(((TemplateContext)_localctx).es.list); 
			setState(26);
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
			setState(33);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==STARTEXPR || _la==TEXT) {
				{
				{
				setState(28);
				((ElementsContext)_localctx).e = element(parentType);
				 _localctx.list.add(((ElementsContext)_localctx).e.ast); 
				}
				}
				setState(35);
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
			setState(42);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case TEXT:
				enterOuterAlt(_localctx, 1);
				{
				setState(36);
				((ElementContext)_localctx).t = text();
				 ((ElementContext)_localctx).ast =  new AstText((((ElementContext)_localctx).t!=null?_input.getText(((ElementContext)_localctx).t.start,((ElementContext)_localctx).t.stop):null)); _localctx.ast.addChild(new AstPrint(_localctx.ast.type, null)); 
				}
				break;
			case STARTEXPR:
				enterOuterAlt(_localctx, 2);
				{
				setState(39);
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
			setState(45); 
			_errHandler.sync(this);
			_alt = 1;
			do {
				switch (_alt) {
				case 1:
					{
					{
					setState(44);
					match(TEXT);
					}
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(47); 
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
		public ExpsContext es;
		public OrExpsContext ores;
		public DefaultValueContext dv;
		public FunctionContext f;
		public TerminalNode STARTEXPR() { return getToken(TemplateGrammar.STARTEXPR, 0); }
		public TerminalNode RBRACE() { return getToken(TemplateGrammar.RBRACE, 0); }
		public ExpsContext exps() {
			return getRuleContext(ExpsContext.class,0);
		}
		public OrExpsContext orExps() {
			return getRuleContext(OrExpsContext.class,0);
		}
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
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(49);
			match(STARTEXPR);
			setState(50);
			((ExpressionContext)_localctx).es = exps(parentType);
			 ((ExpressionContext)_localctx).ast =  ((ExpressionContext)_localctx).es.ast; 
			setState(52);
			((ExpressionContext)_localctx).ores = orExps(parentType, _localctx.ast);
			 ((ExpressionContext)_localctx).ast =  ((ExpressionContext)_localctx).ores.ast; 
			setState(54);
			((ExpressionContext)_localctx).dv = defaultValue();
			setState(55);
			((ExpressionContext)_localctx).f = function();

			        if( ((ExpressionContext)_localctx).f.func != null ) {
			          _localctx.ast.addToBottomChildrenAndSet( ((ExpressionContext)_localctx).f.func );
			        }

			        var ap = getAst(_localctx.ast.bottom.type, null, false, ((ExpressionContext)_localctx).dv.v);
			        _localctx.ast.addToBottomChildrenAndSet( ap );
			      
			setState(57);
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
		public String v = null;
		public Token s;
		public Token d;
		public TerminalNode DQUESTION() { return getToken(TemplateGrammar.DQUESTION, 0); }
		public TerminalNode SSTRING() { return getToken(TemplateGrammar.SSTRING, 0); }
		public TerminalNode DSTRING() { return getToken(TemplateGrammar.DSTRING, 0); }
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
			setState(67);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case DQUESTION:
				enterOuterAlt(_localctx, 1);
				{
				setState(59);
				match(DQUESTION);
				setState(64);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case SSTRING:
					{
					setState(60);
					((DefaultValueContext)_localctx).s = match(SSTRING);
					 ((DefaultValueContext)_localctx).v =  sStringToDString( (((DefaultValueContext)_localctx).s!=null?((DefaultValueContext)_localctx).s.getText():null) ); 
					}
					break;
				case DSTRING:
					{
					setState(62);
					((DefaultValueContext)_localctx).d = match(DSTRING);
					 ((DefaultValueContext)_localctx).v =  (((DefaultValueContext)_localctx).d!=null?((DefaultValueContext)_localctx).d.getText():null); 
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				}
				break;
			case RBRACE:
			case SEMI:
				enterOuterAlt(_localctx, 2);
				{
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
		public Token id;
		public FunctionArgsContext fa;
		public TerminalNode SEMI() { return getToken(TemplateGrammar.SEMI, 0); }
		public TerminalNode FALPAREN() { return getToken(TemplateGrammar.FALPAREN, 0); }
		public TerminalNode FARPAREN() { return getToken(TemplateGrammar.FARPAREN, 0); }
		public TerminalNode FUNCTIONNAME() { return getToken(TemplateGrammar.FUNCTIONNAME, 0); }
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
		enterRule(_localctx, 12, RULE_function);
		try {
			setState(77);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case SEMI:
				enterOuterAlt(_localctx, 1);
				{
				setState(69);
				match(SEMI);
				setState(70);
				((FunctionContext)_localctx).id = match(FUNCTIONNAME);
				setState(71);
				match(FALPAREN);
				setState(72);
				((FunctionContext)_localctx).fa = functionArgs();
				setState(73);
				match(FARPAREN);
				 ((FunctionContext)_localctx).func =  getFunction( (((FunctionContext)_localctx).id!=null?((FunctionContext)_localctx).id.getText():null), ((FunctionContext)_localctx).fa.ret ); 
				}
				break;
			case RBRACE:
				enterOuterAlt(_localctx, 2);
				{
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
		public List<TerminalNode> FACOMMA() { return getTokens(TemplateGrammar.FACOMMA); }
		public TerminalNode FACOMMA(int i) {
			return getToken(TemplateGrammar.FACOMMA, i);
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
		enterRule(_localctx, 14, RULE_functionArgs);
		int _la;
		try {
			setState(91);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case FADECDIGITS:
			case FADSTRING:
			case FASSTRING:
				enterOuterAlt(_localctx, 1);
				{
				setState(79);
				((FunctionArgsContext)_localctx).fa = functionArg();
				 _localctx.ret.add( ((FunctionArgsContext)_localctx).fa.ret ); 
				setState(87);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==FACOMMA) {
					{
					{
					setState(81);
					match(FACOMMA);
					setState(82);
					((FunctionArgsContext)_localctx).nfa = functionArg();
					 _localctx.ret.add( ((FunctionArgsContext)_localctx).nfa.ret ); 
					}
					}
					setState(89);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
				break;
			case FARPAREN:
				enterOuterAlt(_localctx, 2);
				{
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

	public static class FunctionArgContext extends ParserRuleContext {
		public String ret;
		public Token d;
		public Token ss;
		public Token ds;
		public TerminalNode FADECDIGITS() { return getToken(TemplateGrammar.FADECDIGITS, 0); }
		public TerminalNode FASSTRING() { return getToken(TemplateGrammar.FASSTRING, 0); }
		public TerminalNode FADSTRING() { return getToken(TemplateGrammar.FADSTRING, 0); }
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
		enterRule(_localctx, 16, RULE_functionArg);
		try {
			setState(99);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case FADECDIGITS:
				enterOuterAlt(_localctx, 1);
				{
				setState(93);
				((FunctionArgContext)_localctx).d = match(FADECDIGITS);
				 ((FunctionArgContext)_localctx).ret =  (((FunctionArgContext)_localctx).d!=null?((FunctionArgContext)_localctx).d.getText():null); 
				}
				break;
			case FASSTRING:
				enterOuterAlt(_localctx, 2);
				{
				setState(95);
				((FunctionArgContext)_localctx).ss = match(FASSTRING);
				 ((FunctionArgContext)_localctx).ret =  sStringToDString( (((FunctionArgContext)_localctx).ss!=null?((FunctionArgContext)_localctx).ss.getText():null) ); 
				}
				break;
			case FADSTRING:
				enterOuterAlt(_localctx, 3);
				{
				setState(97);
				((FunctionArgContext)_localctx).ds = match(FADSTRING);
				 ((FunctionArgContext)_localctx).ret =  (((FunctionArgContext)_localctx).ds!=null?((FunctionArgContext)_localctx).ds.getText():null); 
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
		enterRule(_localctx, 18, RULE_orExps);
		int _la;
		try {
			setState(116);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case PIPE:
				enterOuterAlt(_localctx, 1);
				{
				{
				setState(101);
				match(PIPE);
				setState(102);
				((OrExpsContext)_localctx).e1 = exps(parentType);
				 _localctx.list.add(firstAst); _localctx.list.add(((OrExpsContext)_localctx).e1.ast); 
				setState(110);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==PIPE) {
					{
					{
					setState(104);
					match(PIPE);
					setState(105);
					((OrExpsContext)_localctx).e2 = exps(parentType);
					_localctx.list.add(((OrExpsContext)_localctx).e2.ast);
					}
					}
					setState(112);
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
		enterRule(_localctx, 20, RULE_exps);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			{
			setState(118);
			((ExpsContext)_localctx).id = exp(parentType);
			 ((ExpsContext)_localctx).ast =  ((ExpsContext)_localctx).id.ast; 
			setState(126);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==DOT) {
				{
				{
				setState(120);
				match(DOT);
				setState(121);
				((ExpsContext)_localctx).nid = exp(_localctx.ast.bottom.type);
				_localctx.ast.addToBottomChildrenAndSet(((ExpsContext)_localctx).nid.ast);
				}
				}
				setState(128);
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
		enterRule(_localctx, 22, RULE_exp);
		try {
			setState(136);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,12,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				{
				setState(129);
				((ExpContext)_localctx).id = match(ID);
				setState(130);
				match(LPAREN);
				setState(131);
				match(RPAREN);
				}
				 ((ExpContext)_localctx).ast =  getAst(_localctx.parentType, (((ExpContext)_localctx).id!=null?((ExpContext)_localctx).id.getText():null), true); 
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(134);
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
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\3\31\u008d\4\2\t\2"+
		"\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13"+
		"\t\13\4\f\t\f\4\r\t\r\3\2\3\2\3\2\3\2\3\3\3\3\3\3\7\3\"\n\3\f\3\16\3%"+
		"\13\3\3\4\3\4\3\4\3\4\3\4\3\4\5\4-\n\4\3\5\6\5\60\n\5\r\5\16\5\61\3\6"+
		"\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\7\3\7\3\7\3\7\3\7\5\7C\n\7\3\7"+
		"\5\7F\n\7\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\5\bP\n\b\3\t\3\t\3\t\3\t\3\t"+
		"\3\t\7\tX\n\t\f\t\16\t[\13\t\3\t\5\t^\n\t\3\n\3\n\3\n\3\n\3\n\3\n\5\n"+
		"f\n\n\3\13\3\13\3\13\3\13\3\13\3\13\3\13\7\13o\n\13\f\13\16\13r\13\13"+
		"\3\13\3\13\3\13\5\13w\n\13\3\f\3\f\3\f\3\f\3\f\3\f\7\f\177\n\f\f\f\16"+
		"\f\u0082\13\f\3\r\3\r\3\r\3\r\3\r\3\r\3\r\5\r\u008b\n\r\3\r\2\2\16\2\4"+
		"\6\b\n\f\16\20\22\24\26\30\2\2\2\u008e\2\32\3\2\2\2\4#\3\2\2\2\6,\3\2"+
		"\2\2\b/\3\2\2\2\n\63\3\2\2\2\fE\3\2\2\2\16O\3\2\2\2\20]\3\2\2\2\22e\3"+
		"\2\2\2\24v\3\2\2\2\26x\3\2\2\2\30\u008a\3\2\2\2\32\33\5\4\3\2\33\34\b"+
		"\2\1\2\34\35\7\2\2\3\35\3\3\2\2\2\36\37\5\6\4\2\37 \b\3\1\2 \"\3\2\2\2"+
		"!\36\3\2\2\2\"%\3\2\2\2#!\3\2\2\2#$\3\2\2\2$\5\3\2\2\2%#\3\2\2\2&\'\5"+
		"\b\5\2\'(\b\4\1\2(-\3\2\2\2)*\5\n\6\2*+\b\4\1\2+-\3\2\2\2,&\3\2\2\2,)"+
		"\3\2\2\2-\7\3\2\2\2.\60\7\4\2\2/.\3\2\2\2\60\61\3\2\2\2\61/\3\2\2\2\61"+
		"\62\3\2\2\2\62\t\3\2\2\2\63\64\7\3\2\2\64\65\5\26\f\2\65\66\b\6\1\2\66"+
		"\67\5\24\13\2\678\b\6\1\289\5\f\7\29:\5\16\b\2:;\b\6\1\2;<\7\5\2\2<\13"+
		"\3\2\2\2=B\7\f\2\2>?\7\20\2\2?C\b\7\1\2@A\7\17\2\2AC\b\7\1\2B>\3\2\2\2"+
		"B@\3\2\2\2CF\3\2\2\2DF\3\2\2\2E=\3\2\2\2ED\3\2\2\2F\r\3\2\2\2GH\7\r\2"+
		"\2HI\7\22\2\2IJ\7\27\2\2JK\5\20\t\2KL\7\30\2\2LM\b\b\1\2MP\3\2\2\2NP\3"+
		"\2\2\2OG\3\2\2\2ON\3\2\2\2P\17\3\2\2\2QR\5\22\n\2RY\b\t\1\2ST\7\26\2\2"+
		"TU\5\22\n\2UV\b\t\1\2VX\3\2\2\2WS\3\2\2\2X[\3\2\2\2YW\3\2\2\2YZ\3\2\2"+
		"\2Z^\3\2\2\2[Y\3\2\2\2\\^\3\2\2\2]Q\3\2\2\2]\\\3\2\2\2^\21\3\2\2\2_`\7"+
		"\23\2\2`f\b\n\1\2ab\7\25\2\2bf\b\n\1\2cd\7\24\2\2df\b\n\1\2e_\3\2\2\2"+
		"ea\3\2\2\2ec\3\2\2\2f\23\3\2\2\2gh\7\6\2\2hi\5\26\f\2ip\b\13\1\2jk\7\6"+
		"\2\2kl\5\26\f\2lm\b\13\1\2mo\3\2\2\2nj\3\2\2\2or\3\2\2\2pn\3\2\2\2pq\3"+
		"\2\2\2qs\3\2\2\2rp\3\2\2\2st\b\13\1\2tw\3\2\2\2uw\b\13\1\2vg\3\2\2\2v"+
		"u\3\2\2\2w\25\3\2\2\2xy\5\30\r\2y\u0080\b\f\1\2z{\7\7\2\2{|\5\30\r\2|"+
		"}\b\f\1\2}\177\3\2\2\2~z\3\2\2\2\177\u0082\3\2\2\2\u0080~\3\2\2\2\u0080"+
		"\u0081\3\2\2\2\u0081\27\3\2\2\2\u0082\u0080\3\2\2\2\u0083\u0084\7\16\2"+
		"\2\u0084\u0085\7\b\2\2\u0085\u0086\7\t\2\2\u0086\u0087\3\2\2\2\u0087\u008b"+
		"\b\r\1\2\u0088\u0089\7\16\2\2\u0089\u008b\b\r\1\2\u008a\u0083\3\2\2\2"+
		"\u008a\u0088\3\2\2\2\u008b\31\3\2\2\2\17#,\61BEOY]epv\u0080\u008a";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}