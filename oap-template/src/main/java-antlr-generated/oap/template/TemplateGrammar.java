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
		STARTEXPR=1, TEXT=2, LBRACE=3, RBRACE=4, PIPE=5, DOT=6, LPAREN=7, RPAREN=8, 
		LBRACK=9, RBRACK=10, DQUESTION=11, SEMI=12, COMMA=13, ID=14, DSTRING=15, 
		SSTRING=16, DECDIGITS=17, FLOAT=18, ERR_CHAR=19, CRBRACE=20, CCOMMA=21, 
		CID=22, CDSTRING=23, CSSTRING=24, CDECDIGITS=25, CFLOAT=26, CERR_CHAR=27;
	public static final int
		RULE_template = 0, RULE_elements = 1, RULE_element = 2, RULE_text = 3, 
		RULE_expression = 4, RULE_defaultValue = 5, RULE_defaultValueType = 6, 
		RULE_function = 7, RULE_functionArgs = 8, RULE_functionArg = 9, RULE_orExps = 10, 
		RULE_exps = 11, RULE_exp = 12, RULE_concatenation = 13, RULE_citems = 14, 
		RULE_citem = 15;
	private static String[] makeRuleNames() {
		return new String[] {
			"template", "elements", "element", "text", "expression", "defaultValue", 
			"defaultValueType", "function", "functionArgs", "functionArg", "orExps", 
			"exps", "exp", "concatenation", "citems", "citem"
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
			null, "STARTEXPR", "TEXT", "LBRACE", "RBRACE", "PIPE", "DOT", "LPAREN", 
			"RPAREN", "LBRACK", "RBRACK", "DQUESTION", "SEMI", "COMMA", "ID", "DSTRING", 
			"SSTRING", "DECDIGITS", "FLOAT", "ERR_CHAR", "CRBRACE", "CCOMMA", "CID", 
			"CDSTRING", "CSSTRING", "CDECDIGITS", "CFLOAT", "CERR_CHAR"
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
		public ElementsContext elements;
		public ElementsContext elements() {
			return getRuleContext(ElementsContext.class,0);
		}
		public TerminalNode EOF() { return getToken(TemplateGrammar.EOF, 0); }
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
			setState(32);
			((TemplateContext)_localctx).elements = elements(parentType);
			 ((TemplateContext)_localctx).rootAst =  new AstRoot(_localctx.parentType); _localctx.rootAst.addChildren(((TemplateContext)_localctx).elements.list); 
			setState(34);
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
		public ElementContext element;
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
			setState(41);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==STARTEXPR || _la==TEXT) {
				{
				{
				setState(36);
				((ElementsContext)_localctx).element = element(parentType);
				 _localctx.list.add(((ElementsContext)_localctx).element.ast); 
				}
				}
				setState(43);
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
		public ExpressionContext expression;
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
			setState(50);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case TEXT:
				enterOuterAlt(_localctx, 1);
				{
				setState(44);
				((ElementContext)_localctx).t = text();
				 ((ElementContext)_localctx).ast =  new AstText((((ElementContext)_localctx).t!=null?_input.getText(((ElementContext)_localctx).t.start,((ElementContext)_localctx).t.stop):null)); _localctx.ast.addChild(new AstPrint(_localctx.ast.type, null)); 
				}
				break;
			case STARTEXPR:
				enterOuterAlt(_localctx, 2);
				{
				setState(47);
				((ElementContext)_localctx).expression = expression(parentType);
				 ((ElementContext)_localctx).ast =  ((ElementContext)_localctx).expression.ast.top; 
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
			setState(53); 
			_errHandler.sync(this);
			_alt = 1;
			do {
				switch (_alt) {
				case 1:
					{
					{
					setState(52);
					match(TEXT);
					}
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(55); 
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
		public MaxMin ast;
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
			setState(57);
			match(STARTEXPR);
			setState(58);
			((ExpressionContext)_localctx).exps = exps(parentType);
			 ((ExpressionContext)_localctx).ast =  ((ExpressionContext)_localctx).exps.ast; 
			setState(60);
			((ExpressionContext)_localctx).orExps = orExps(parentType, _localctx.ast);
			 ((ExpressionContext)_localctx).ast =  ((ExpressionContext)_localctx).orExps.ast; 
			setState(63);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==DQUESTION) {
				{
				setState(62);
				((ExpressionContext)_localctx).defaultValue = defaultValue();
				}
			}

			setState(66);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==SEMI) {
				{
				setState(65);
				((ExpressionContext)_localctx).function = function();
				}
			}


			        if( ((ExpressionContext)_localctx).function != null ) {
			          _localctx.ast.addToBottomChildrenAndSet( ((ExpressionContext)_localctx).function.func );
			        }

			        _localctx.ast.addLeafs( () -> getAst(_localctx.ast.bottom.type, null, false, ((ExpressionContext)_localctx).defaultValue != null ? ((ExpressionContext)_localctx).defaultValue.v : null) );
			      
			setState(69);
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
			setState(71);
			match(DQUESTION);
			setState(72);
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
			setState(83);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case SSTRING:
				enterOuterAlt(_localctx, 1);
				{
				setState(75);
				((DefaultValueTypeContext)_localctx).SSTRING = match(SSTRING);
				 ((DefaultValueTypeContext)_localctx).v =  sStringToDString( (((DefaultValueTypeContext)_localctx).SSTRING!=null?((DefaultValueTypeContext)_localctx).SSTRING.getText():null) ); 
				}
				break;
			case DSTRING:
				enterOuterAlt(_localctx, 2);
				{
				setState(77);
				((DefaultValueTypeContext)_localctx).DSTRING = match(DSTRING);
				 ((DefaultValueTypeContext)_localctx).v =  (((DefaultValueTypeContext)_localctx).DSTRING!=null?((DefaultValueTypeContext)_localctx).DSTRING.getText():null); 
				}
				break;
			case DECDIGITS:
				enterOuterAlt(_localctx, 3);
				{
				setState(79);
				((DefaultValueTypeContext)_localctx).DECDIGITS = match(DECDIGITS);
				 ((DefaultValueTypeContext)_localctx).v =  (((DefaultValueTypeContext)_localctx).DECDIGITS!=null?((DefaultValueTypeContext)_localctx).DECDIGITS.getText():null); 
				}
				break;
			case FLOAT:
				enterOuterAlt(_localctx, 4);
				{
				setState(81);
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
			setState(85);
			match(SEMI);
			setState(86);
			((FunctionContext)_localctx).ID = match(ID);
			setState(87);
			match(LPAREN);
			setState(89);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << DSTRING) | (1L << SSTRING) | (1L << DECDIGITS))) != 0)) {
				{
				setState(88);
				((FunctionContext)_localctx).functionArgs = functionArgs();
				}
			}

			setState(91);
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
		public FunctionArgContext functionArg;
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
			setState(94);
			((FunctionArgsContext)_localctx).functionArg = functionArg();
			 _localctx.ret.add( ((FunctionArgsContext)_localctx).functionArg.ret ); 
			setState(102);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(96);
				match(COMMA);
				setState(97);
				((FunctionArgsContext)_localctx).functionArg = functionArg();
				 _localctx.ret.add( ((FunctionArgsContext)_localctx).functionArg.ret ); 
				}
				}
				setState(104);
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
			setState(111);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case DECDIGITS:
				enterOuterAlt(_localctx, 1);
				{
				setState(105);
				((FunctionArgContext)_localctx).DECDIGITS = match(DECDIGITS);
				 ((FunctionArgContext)_localctx).ret =  (((FunctionArgContext)_localctx).DECDIGITS!=null?((FunctionArgContext)_localctx).DECDIGITS.getText():null); 
				}
				break;
			case SSTRING:
				enterOuterAlt(_localctx, 2);
				{
				setState(107);
				((FunctionArgContext)_localctx).SSTRING = match(SSTRING);
				 ((FunctionArgContext)_localctx).ret =  sStringToDString( (((FunctionArgContext)_localctx).SSTRING!=null?((FunctionArgContext)_localctx).SSTRING.getText():null) ); 
				}
				break;
			case DSTRING:
				enterOuterAlt(_localctx, 3);
				{
				setState(109);
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
		public MaxMin firstAst;
		public MaxMin ast;
		public ArrayList<MaxMin> list = new ArrayList<>();;
		public ExpsContext exps;
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
		public OrExpsContext(ParserRuleContext parent, int invokingState, TemplateType parentType, MaxMin firstAst) {
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

	public final OrExpsContext orExps(TemplateType parentType,MaxMin firstAst) throws RecognitionException {
		OrExpsContext _localctx = new OrExpsContext(_ctx, getState(), parentType, firstAst);
		enterRule(_localctx, 20, RULE_orExps);
		int _la;
		try {
			setState(128);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case PIPE:
				enterOuterAlt(_localctx, 1);
				{
				{
				setState(113);
				match(PIPE);
				setState(114);
				((OrExpsContext)_localctx).exps = exps(parentType);
				 _localctx.list.add(firstAst); _localctx.list.add(((OrExpsContext)_localctx).exps.ast); 
				setState(122);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==PIPE) {
					{
					{
					setState(116);
					match(PIPE);
					setState(117);
					((OrExpsContext)_localctx).exps = exps(parentType);
					_localctx.list.add(((OrExpsContext)_localctx).exps.ast);
					}
					}
					setState(124);
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
				            ((OrExpsContext)_localctx).ast =  new MaxMin(or);
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
		public MaxMin ast;
		public ExpContext exp;
		public ConcatenationContext concatenation;
		public List<ExpContext> exp() {
			return getRuleContexts(ExpContext.class);
		}
		public ExpContext exp(int i) {
			return getRuleContext(ExpContext.class,i);
		}
		public ConcatenationContext concatenation() {
			return getRuleContext(ConcatenationContext.class,0);
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
			setState(149);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case ID:
				enterOuterAlt(_localctx, 1);
				{
				{
				setState(130);
				((ExpsContext)_localctx).exp = exp(parentType);
				 ((ExpsContext)_localctx).ast =  ((ExpsContext)_localctx).exp.ast; 
				setState(138);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==DOT) {
					{
					{
					setState(132);
					match(DOT);
					setState(133);
					((ExpsContext)_localctx).exp = exp(_localctx.ast.bottom.type);
					_localctx.ast.addToBottomChildrenAndSet(((ExpsContext)_localctx).exp.ast);
					}
					}
					setState(140);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
				setState(142);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==LBRACE) {
					{
					setState(141);
					((ExpsContext)_localctx).concatenation = concatenation(parentType);
					}
				}

				 if( ((ExpsContext)_localctx).concatenation != null ) _localctx.ast.addToBottomChildrenAndSet( ((ExpsContext)_localctx).concatenation.ast ); 
				}
				break;
			case LBRACE:
				enterOuterAlt(_localctx, 2);
				{
				setState(146);
				((ExpsContext)_localctx).concatenation = concatenation(parentType);
				 ((ExpsContext)_localctx).ast =  new MaxMin( ((ExpsContext)_localctx).concatenation.ast ); 
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

	public static class ExpContext extends ParserRuleContext {
		public TemplateType parentType;
		public MaxMin ast;
		public Token ID;
		public TerminalNode ID() { return getToken(TemplateGrammar.ID, 0); }
		public TerminalNode LPAREN() { return getToken(TemplateGrammar.LPAREN, 0); }
		public TerminalNode RPAREN() { return getToken(TemplateGrammar.RPAREN, 0); }
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
			setState(158);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,14,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				{
				setState(151);
				((ExpContext)_localctx).ID = match(ID);
				setState(152);
				match(LPAREN);
				setState(153);
				match(RPAREN);
				}
				 ((ExpContext)_localctx).ast =  getAst(_localctx.parentType, (((ExpContext)_localctx).ID!=null?((ExpContext)_localctx).ID.getText():null), true); 
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(156);
				((ExpContext)_localctx).ID = match(ID);
				 ((ExpContext)_localctx).ast =  getAst(_localctx.parentType, (((ExpContext)_localctx).ID!=null?((ExpContext)_localctx).ID.getText():null), false); 
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

	public static class ConcatenationContext extends ParserRuleContext {
		public TemplateType parentType;
		public AstConcatenation ast;
		public CitemsContext citems;
		public TerminalNode LBRACE() { return getToken(TemplateGrammar.LBRACE, 0); }
		public CitemsContext citems() {
			return getRuleContext(CitemsContext.class,0);
		}
		public TerminalNode CRBRACE() { return getToken(TemplateGrammar.CRBRACE, 0); }
		public ConcatenationContext(ParserRuleContext parent, int invokingState) { super(parent, invokingState); }
		public ConcatenationContext(ParserRuleContext parent, int invokingState, TemplateType parentType) {
			super(parent, invokingState);
			this.parentType = parentType;
		}
		@Override public int getRuleIndex() { return RULE_concatenation; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarListener ) ((TemplateGrammarListener)listener).enterConcatenation(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarListener ) ((TemplateGrammarListener)listener).exitConcatenation(this);
		}
	}

	public final ConcatenationContext concatenation(TemplateType parentType) throws RecognitionException {
		ConcatenationContext _localctx = new ConcatenationContext(_ctx, getState(), parentType);
		enterRule(_localctx, 26, RULE_concatenation);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(160);
			match(LBRACE);
			setState(161);
			((ConcatenationContext)_localctx).citems = citems(parentType);
			 ((ConcatenationContext)_localctx).ast =  new AstConcatenation(parentType, ((ConcatenationContext)_localctx).citems.list); 
			setState(163);
			match(CRBRACE);
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

	public static class CitemsContext extends ParserRuleContext {
		public TemplateType parentType;
		public ArrayList<Ast> list = new ArrayList<Ast>();
		public CitemContext citem;
		public List<CitemContext> citem() {
			return getRuleContexts(CitemContext.class);
		}
		public CitemContext citem(int i) {
			return getRuleContext(CitemContext.class,i);
		}
		public List<TerminalNode> CCOMMA() { return getTokens(TemplateGrammar.CCOMMA); }
		public TerminalNode CCOMMA(int i) {
			return getToken(TemplateGrammar.CCOMMA, i);
		}
		public CitemsContext(ParserRuleContext parent, int invokingState) { super(parent, invokingState); }
		public CitemsContext(ParserRuleContext parent, int invokingState, TemplateType parentType) {
			super(parent, invokingState);
			this.parentType = parentType;
		}
		@Override public int getRuleIndex() { return RULE_citems; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarListener ) ((TemplateGrammarListener)listener).enterCitems(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarListener ) ((TemplateGrammarListener)listener).exitCitems(this);
		}
	}

	public final CitemsContext citems(TemplateType parentType) throws RecognitionException {
		CitemsContext _localctx = new CitemsContext(_ctx, getState(), parentType);
		enterRule(_localctx, 28, RULE_citems);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(165);
			((CitemsContext)_localctx).citem = citem(parentType);
			 _localctx.list.add(((CitemsContext)_localctx).citem.ast.top); ((CitemsContext)_localctx).citem.ast.addToBottomChildrenAndSet(getAst(((CitemsContext)_localctx).citem.ast.bottom.type, null, false)); 
			setState(173);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==CCOMMA) {
				{
				{
				setState(167);
				match(CCOMMA);
				setState(168);
				((CitemsContext)_localctx).citem = citem(parentType);
				 _localctx.list.add(((CitemsContext)_localctx).citem.ast.top); ((CitemsContext)_localctx).citem.ast.addToBottomChildrenAndSet(getAst(((CitemsContext)_localctx).citem.ast.bottom.type, null, false)); 
				}
				}
				setState(175);
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

	public static class CitemContext extends ParserRuleContext {
		public TemplateType parentType;
		public MaxMin ast;
		public Token CID;
		public Token CDSTRING;
		public Token CSSTRING;
		public Token CDECDIGITS;
		public Token CFLOAT;
		public TerminalNode CID() { return getToken(TemplateGrammar.CID, 0); }
		public TerminalNode CDSTRING() { return getToken(TemplateGrammar.CDSTRING, 0); }
		public TerminalNode CSSTRING() { return getToken(TemplateGrammar.CSSTRING, 0); }
		public TerminalNode CDECDIGITS() { return getToken(TemplateGrammar.CDECDIGITS, 0); }
		public TerminalNode CFLOAT() { return getToken(TemplateGrammar.CFLOAT, 0); }
		public CitemContext(ParserRuleContext parent, int invokingState) { super(parent, invokingState); }
		public CitemContext(ParserRuleContext parent, int invokingState, TemplateType parentType) {
			super(parent, invokingState);
			this.parentType = parentType;
		}
		@Override public int getRuleIndex() { return RULE_citem; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarListener ) ((TemplateGrammarListener)listener).enterCitem(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarListener ) ((TemplateGrammarListener)listener).exitCitem(this);
		}
	}

	public final CitemContext citem(TemplateType parentType) throws RecognitionException {
		CitemContext _localctx = new CitemContext(_ctx, getState(), parentType);
		enterRule(_localctx, 30, RULE_citem);
		try {
			setState(186);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case CID:
				enterOuterAlt(_localctx, 1);
				{
				setState(176);
				((CitemContext)_localctx).CID = match(CID);
				 ((CitemContext)_localctx).ast =  getAst(_localctx.parentType, (((CitemContext)_localctx).CID!=null?((CitemContext)_localctx).CID.getText():null), false); 
				}
				break;
			case CDSTRING:
				enterOuterAlt(_localctx, 2);
				{
				setState(178);
				((CitemContext)_localctx).CDSTRING = match(CDSTRING);
				 ((CitemContext)_localctx).ast =  new MaxMin(new AstText(sdStringToString((((CitemContext)_localctx).CDSTRING!=null?((CitemContext)_localctx).CDSTRING.getText():null)))); 
				}
				break;
			case CSSTRING:
				enterOuterAlt(_localctx, 3);
				{
				setState(180);
				((CitemContext)_localctx).CSSTRING = match(CSSTRING);
				 ((CitemContext)_localctx).ast =  new MaxMin(new AstText(sdStringToString((((CitemContext)_localctx).CSSTRING!=null?((CitemContext)_localctx).CSSTRING.getText():null)))); 
				}
				break;
			case CDECDIGITS:
				enterOuterAlt(_localctx, 4);
				{
				setState(182);
				((CitemContext)_localctx).CDECDIGITS = match(CDECDIGITS);
				 ((CitemContext)_localctx).ast =  new MaxMin(new AstText(String.valueOf((((CitemContext)_localctx).CDECDIGITS!=null?((CitemContext)_localctx).CDECDIGITS.getText():null)))); 
				}
				break;
			case CFLOAT:
				enterOuterAlt(_localctx, 5);
				{
				setState(184);
				((CitemContext)_localctx).CFLOAT = match(CFLOAT);
				 ((CitemContext)_localctx).ast =  new MaxMin(new AstText(String.valueOf((((CitemContext)_localctx).CFLOAT!=null?((CitemContext)_localctx).CFLOAT.getText():null)))); 
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

	public static final String _serializedATN =
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\3\35\u00bf\4\2\t\2"+
		"\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13"+
		"\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\3\2\3\2"+
		"\3\2\3\2\3\3\3\3\3\3\7\3*\n\3\f\3\16\3-\13\3\3\4\3\4\3\4\3\4\3\4\3\4\5"+
		"\4\65\n\4\3\5\6\58\n\5\r\5\16\59\3\6\3\6\3\6\3\6\3\6\3\6\5\6B\n\6\3\6"+
		"\5\6E\n\6\3\6\3\6\3\6\3\7\3\7\3\7\3\7\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b"+
		"\5\bV\n\b\3\t\3\t\3\t\3\t\5\t\\\n\t\3\t\3\t\3\t\3\n\3\n\3\n\3\n\3\n\3"+
		"\n\7\ng\n\n\f\n\16\nj\13\n\3\13\3\13\3\13\3\13\3\13\3\13\5\13r\n\13\3"+
		"\f\3\f\3\f\3\f\3\f\3\f\3\f\7\f{\n\f\f\f\16\f~\13\f\3\f\3\f\3\f\5\f\u0083"+
		"\n\f\3\r\3\r\3\r\3\r\3\r\3\r\7\r\u008b\n\r\f\r\16\r\u008e\13\r\3\r\5\r"+
		"\u0091\n\r\3\r\3\r\3\r\3\r\3\r\5\r\u0098\n\r\3\16\3\16\3\16\3\16\3\16"+
		"\3\16\3\16\5\16\u00a1\n\16\3\17\3\17\3\17\3\17\3\17\3\20\3\20\3\20\3\20"+
		"\3\20\3\20\7\20\u00ae\n\20\f\20\16\20\u00b1\13\20\3\21\3\21\3\21\3\21"+
		"\3\21\3\21\3\21\3\21\3\21\3\21\5\21\u00bd\n\21\3\21\2\2\22\2\4\6\b\n\f"+
		"\16\20\22\24\26\30\32\34\36 \2\2\2\u00c5\2\"\3\2\2\2\4+\3\2\2\2\6\64\3"+
		"\2\2\2\b\67\3\2\2\2\n;\3\2\2\2\fI\3\2\2\2\16U\3\2\2\2\20W\3\2\2\2\22`"+
		"\3\2\2\2\24q\3\2\2\2\26\u0082\3\2\2\2\30\u0097\3\2\2\2\32\u00a0\3\2\2"+
		"\2\34\u00a2\3\2\2\2\36\u00a7\3\2\2\2 \u00bc\3\2\2\2\"#\5\4\3\2#$\b\2\1"+
		"\2$%\7\2\2\3%\3\3\2\2\2&\'\5\6\4\2\'(\b\3\1\2(*\3\2\2\2)&\3\2\2\2*-\3"+
		"\2\2\2+)\3\2\2\2+,\3\2\2\2,\5\3\2\2\2-+\3\2\2\2./\5\b\5\2/\60\b\4\1\2"+
		"\60\65\3\2\2\2\61\62\5\n\6\2\62\63\b\4\1\2\63\65\3\2\2\2\64.\3\2\2\2\64"+
		"\61\3\2\2\2\65\7\3\2\2\2\668\7\4\2\2\67\66\3\2\2\289\3\2\2\29\67\3\2\2"+
		"\29:\3\2\2\2:\t\3\2\2\2;<\7\3\2\2<=\5\30\r\2=>\b\6\1\2>?\5\26\f\2?A\b"+
		"\6\1\2@B\5\f\7\2A@\3\2\2\2AB\3\2\2\2BD\3\2\2\2CE\5\20\t\2DC\3\2\2\2DE"+
		"\3\2\2\2EF\3\2\2\2FG\b\6\1\2GH\7\6\2\2H\13\3\2\2\2IJ\7\r\2\2JK\5\16\b"+
		"\2KL\b\7\1\2L\r\3\2\2\2MN\7\22\2\2NV\b\b\1\2OP\7\21\2\2PV\b\b\1\2QR\7"+
		"\23\2\2RV\b\b\1\2ST\7\24\2\2TV\b\b\1\2UM\3\2\2\2UO\3\2\2\2UQ\3\2\2\2U"+
		"S\3\2\2\2V\17\3\2\2\2WX\7\16\2\2XY\7\20\2\2Y[\7\t\2\2Z\\\5\22\n\2[Z\3"+
		"\2\2\2[\\\3\2\2\2\\]\3\2\2\2]^\7\n\2\2^_\b\t\1\2_\21\3\2\2\2`a\5\24\13"+
		"\2ah\b\n\1\2bc\7\17\2\2cd\5\24\13\2de\b\n\1\2eg\3\2\2\2fb\3\2\2\2gj\3"+
		"\2\2\2hf\3\2\2\2hi\3\2\2\2i\23\3\2\2\2jh\3\2\2\2kl\7\23\2\2lr\b\13\1\2"+
		"mn\7\22\2\2nr\b\13\1\2op\7\21\2\2pr\b\13\1\2qk\3\2\2\2qm\3\2\2\2qo\3\2"+
		"\2\2r\25\3\2\2\2st\7\7\2\2tu\5\30\r\2u|\b\f\1\2vw\7\7\2\2wx\5\30\r\2x"+
		"y\b\f\1\2y{\3\2\2\2zv\3\2\2\2{~\3\2\2\2|z\3\2\2\2|}\3\2\2\2}\177\3\2\2"+
		"\2~|\3\2\2\2\177\u0080\b\f\1\2\u0080\u0083\3\2\2\2\u0081\u0083\b\f\1\2"+
		"\u0082s\3\2\2\2\u0082\u0081\3\2\2\2\u0083\27\3\2\2\2\u0084\u0085\5\32"+
		"\16\2\u0085\u008c\b\r\1\2\u0086\u0087\7\b\2\2\u0087\u0088\5\32\16\2\u0088"+
		"\u0089\b\r\1\2\u0089\u008b\3\2\2\2\u008a\u0086\3\2\2\2\u008b\u008e\3\2"+
		"\2\2\u008c\u008a\3\2\2\2\u008c\u008d\3\2\2\2\u008d\u0090\3\2\2\2\u008e"+
		"\u008c\3\2\2\2\u008f\u0091\5\34\17\2\u0090\u008f\3\2\2\2\u0090\u0091\3"+
		"\2\2\2\u0091\u0092\3\2\2\2\u0092\u0093\b\r\1\2\u0093\u0098\3\2\2\2\u0094"+
		"\u0095\5\34\17\2\u0095\u0096\b\r\1\2\u0096\u0098\3\2\2\2\u0097\u0084\3"+
		"\2\2\2\u0097\u0094\3\2\2\2\u0098\31\3\2\2\2\u0099\u009a\7\20\2\2\u009a"+
		"\u009b\7\t\2\2\u009b\u009c\7\n\2\2\u009c\u009d\3\2\2\2\u009d\u00a1\b\16"+
		"\1\2\u009e\u009f\7\20\2\2\u009f\u00a1\b\16\1\2\u00a0\u0099\3\2\2\2\u00a0"+
		"\u009e\3\2\2\2\u00a1\33\3\2\2\2\u00a2\u00a3\7\5\2\2\u00a3\u00a4\5\36\20"+
		"\2\u00a4\u00a5\b\17\1\2\u00a5\u00a6\7\26\2\2\u00a6\35\3\2\2\2\u00a7\u00a8"+
		"\5 \21\2\u00a8\u00af\b\20\1\2\u00a9\u00aa\7\27\2\2\u00aa\u00ab\5 \21\2"+
		"\u00ab\u00ac\b\20\1\2\u00ac\u00ae\3\2\2\2\u00ad\u00a9\3\2\2\2\u00ae\u00b1"+
		"\3\2\2\2\u00af\u00ad\3\2\2\2\u00af\u00b0\3\2\2\2\u00b0\37\3\2\2\2\u00b1"+
		"\u00af\3\2\2\2\u00b2\u00b3\7\30\2\2\u00b3\u00bd\b\21\1\2\u00b4\u00b5\7"+
		"\31\2\2\u00b5\u00bd\b\21\1\2\u00b6\u00b7\7\32\2\2\u00b7\u00bd\b\21\1\2"+
		"\u00b8\u00b9\7\33\2\2\u00b9\u00bd\b\21\1\2\u00ba\u00bb\7\34\2\2\u00bb"+
		"\u00bd\b\21\1\2\u00bc\u00b2\3\2\2\2\u00bc\u00b4\3\2\2\2\u00bc\u00b6\3"+
		"\2\2\2\u00bc\u00b8\3\2\2\2\u00bc\u00ba\3\2\2\2\u00bd!\3\2\2\2\23+\649"+
		"ADU[hq|\u0082\u008c\u0090\u0097\u00a0\u00af\u00bc";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}