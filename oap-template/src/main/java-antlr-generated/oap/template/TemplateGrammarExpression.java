// Generated from java-escape by ANTLR 4.11.1

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
public class TemplateGrammarExpression extends TemplateGrammarAdaptor {
	static { RuntimeMetaData.checkVersion("4.11.1", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		BLOCK_COMMENT=1, HORZ_WS=2, VERT_WS=3, LBRACE=4, RBRACE=5, PIPE=6, DOT=7, 
		LPAREN=8, RPAREN=9, LBRACK=10, RBRACK=11, DQUESTION=12, SEMI=13, COMMA=14, 
		STAR=15, SLASH=16, PERCENT=17, PLUS=18, MINUS=19, DSTRING=20, SSTRING=21, 
		DECDIGITS=22, FLOAT=23, BOOLEAN=24, ID=25, CAST_TYPE=26, ERR_CHAR=27, 
		C_HORZ_WS=28, C_VERT_WS=29, CERR_CHAR=30;
	public static final int
		RULE_expression = 0, RULE_defaultValue = 1, RULE_defaultValueType = 2, 
		RULE_longRule = 3, RULE_function = 4, RULE_functionArgs = 5, RULE_functionArg = 6, 
		RULE_orExps = 7, RULE_exps = 8, RULE_exp = 9, RULE_concatenation = 10, 
		RULE_citems = 11, RULE_citem = 12, RULE_math = 13, RULE_number = 14, RULE_mathOperation = 15;
	private static String[] makeRuleNames() {
		return new String[] {
			"expression", "defaultValue", "defaultValueType", "longRule", "function", 
			"functionArgs", "functionArg", "orExps", "exps", "exp", "concatenation", 
			"citems", "citem", "math", "number", "mathOperation"
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
			null, "BLOCK_COMMENT", "HORZ_WS", "VERT_WS", "LBRACE", "RBRACE", "PIPE", 
			"DOT", "LPAREN", "RPAREN", "LBRACK", "RBRACK", "DQUESTION", "SEMI", "COMMA", 
			"STAR", "SLASH", "PERCENT", "PLUS", "MINUS", "DSTRING", "SSTRING", "DECDIGITS", 
			"FLOAT", "BOOLEAN", "ID", "CAST_TYPE", "ERR_CHAR", "C_HORZ_WS", "C_VERT_WS", 
			"CERR_CHAR"
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
	public String getGrammarFileName() { return "java-escape"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }


		public TemplateGrammarExpression(TokenStream input, Map<String, List<Method>> builtInFunction, ErrorStrategy errorStrategy) {
			this(input);
			
			this.builtInFunction = builtInFunction;
			this.errorStrategy = errorStrategy;
		}


	public TemplateGrammarExpression(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ExpressionContext extends ParserRuleContext {
		public TemplateType parentType;
		public MaxMin ast;
		public String comment = null;;
		public Token BLOCK_COMMENT;
		public ExpsContext exps;
		public OrExpsContext orExps;
		public DefaultValueContext defaultValue;
		public FunctionContext function;
		public ExpsContext exps() {
			return getRuleContext(ExpsContext.class,0);
		}
		public OrExpsContext orExps() {
			return getRuleContext(OrExpsContext.class,0);
		}
		public TerminalNode BLOCK_COMMENT() { return getToken(TemplateGrammarExpression.BLOCK_COMMENT, 0); }
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
			if ( listener instanceof TemplateGrammarExpressionListener ) ((TemplateGrammarExpressionListener)listener).enterExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarExpressionListener ) ((TemplateGrammarExpressionListener)listener).exitExpression(this);
		}
	}

	public final ExpressionContext expression(TemplateType parentType) throws RecognitionException {
		ExpressionContext _localctx = new ExpressionContext(_ctx, getState(), parentType);
		enterRule(_localctx, 0, RULE_expression);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(34);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==BLOCK_COMMENT) {
				{
				setState(32);
				((ExpressionContext)_localctx).BLOCK_COMMENT = match(BLOCK_COMMENT);
				 ((ExpressionContext)_localctx).comment =  (((ExpressionContext)_localctx).BLOCK_COMMENT!=null?((ExpressionContext)_localctx).BLOCK_COMMENT.getText():null); 
				}
			}

			setState(36);
			((ExpressionContext)_localctx).exps = exps(parentType);
			 ((ExpressionContext)_localctx).ast =  ((ExpressionContext)_localctx).exps.ast; 
			setState(38);
			((ExpressionContext)_localctx).orExps = orExps(parentType, _localctx.ast);
			 ((ExpressionContext)_localctx).ast =  ((ExpressionContext)_localctx).orExps.ast; 
			setState(41);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==DQUESTION) {
				{
				setState(40);
				((ExpressionContext)_localctx).defaultValue = defaultValue();
				}
			}

			setState(44);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==SEMI) {
				{
				setState(43);
				((ExpressionContext)_localctx).function = function();
				}
			}


			        if( ((ExpressionContext)_localctx).function != null ) {
			          _localctx.ast.addToBottomChildrenAndSet( ((ExpressionContext)_localctx).function.func );
			        }

			        _localctx.ast.addLeafs( () -> getAst(_localctx.ast.bottom.type, null, false, ((ExpressionContext)_localctx).defaultValue != null ? ((ExpressionContext)_localctx).defaultValue.v : null, null ) );

			        if( _localctx.comment != null ) {
			            _localctx.ast.setTop( new AstComment( parentType, _localctx.comment ) );
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
	public static class DefaultValueContext extends ParserRuleContext {
		public String v;
		public DefaultValueTypeContext defaultValueType;
		public TerminalNode DQUESTION() { return getToken(TemplateGrammarExpression.DQUESTION, 0); }
		public DefaultValueTypeContext defaultValueType() {
			return getRuleContext(DefaultValueTypeContext.class,0);
		}
		public DefaultValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_defaultValue; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarExpressionListener ) ((TemplateGrammarExpressionListener)listener).enterDefaultValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarExpressionListener ) ((TemplateGrammarExpressionListener)listener).exitDefaultValue(this);
		}
	}

	public final DefaultValueContext defaultValue() throws RecognitionException {
		DefaultValueContext _localctx = new DefaultValueContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_defaultValue);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(48);
			match(DQUESTION);
			setState(49);
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

	@SuppressWarnings("CheckReturnValue")
	public static class DefaultValueTypeContext extends ParserRuleContext {
		public String v;
		public Token SSTRING;
		public Token DSTRING;
		public LongRuleContext longRule;
		public Token FLOAT;
		public Token BOOLEAN;
		public TerminalNode SSTRING() { return getToken(TemplateGrammarExpression.SSTRING, 0); }
		public TerminalNode DSTRING() { return getToken(TemplateGrammarExpression.DSTRING, 0); }
		public LongRuleContext longRule() {
			return getRuleContext(LongRuleContext.class,0);
		}
		public TerminalNode FLOAT() { return getToken(TemplateGrammarExpression.FLOAT, 0); }
		public TerminalNode BOOLEAN() { return getToken(TemplateGrammarExpression.BOOLEAN, 0); }
		public TerminalNode LBRACK() { return getToken(TemplateGrammarExpression.LBRACK, 0); }
		public TerminalNode RBRACK() { return getToken(TemplateGrammarExpression.RBRACK, 0); }
		public DefaultValueTypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_defaultValueType; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarExpressionListener ) ((TemplateGrammarExpressionListener)listener).enterDefaultValueType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarExpressionListener ) ((TemplateGrammarExpressionListener)listener).exitDefaultValueType(this);
		}
	}

	public final DefaultValueTypeContext defaultValueType() throws RecognitionException {
		DefaultValueTypeContext _localctx = new DefaultValueTypeContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_defaultValueType);
		try {
			setState(66);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case SSTRING:
				enterOuterAlt(_localctx, 1);
				{
				setState(52);
				((DefaultValueTypeContext)_localctx).SSTRING = match(SSTRING);
				 ((DefaultValueTypeContext)_localctx).v =  sStringToDString( (((DefaultValueTypeContext)_localctx).SSTRING!=null?((DefaultValueTypeContext)_localctx).SSTRING.getText():null) ); 
				}
				break;
			case DSTRING:
				enterOuterAlt(_localctx, 2);
				{
				setState(54);
				((DefaultValueTypeContext)_localctx).DSTRING = match(DSTRING);
				 ((DefaultValueTypeContext)_localctx).v =  (((DefaultValueTypeContext)_localctx).DSTRING!=null?((DefaultValueTypeContext)_localctx).DSTRING.getText():null); 
				}
				break;
			case MINUS:
			case DECDIGITS:
				enterOuterAlt(_localctx, 3);
				{
				setState(56);
				((DefaultValueTypeContext)_localctx).longRule = longRule();
				 ((DefaultValueTypeContext)_localctx).v =  (((DefaultValueTypeContext)_localctx).longRule!=null?_input.getText(((DefaultValueTypeContext)_localctx).longRule.start,((DefaultValueTypeContext)_localctx).longRule.stop):null); 
				}
				break;
			case FLOAT:
				enterOuterAlt(_localctx, 4);
				{
				setState(59);
				((DefaultValueTypeContext)_localctx).FLOAT = match(FLOAT);
				 ((DefaultValueTypeContext)_localctx).v =  (((DefaultValueTypeContext)_localctx).FLOAT!=null?((DefaultValueTypeContext)_localctx).FLOAT.getText():null); 
				}
				break;
			case BOOLEAN:
				enterOuterAlt(_localctx, 5);
				{
				setState(61);
				((DefaultValueTypeContext)_localctx).BOOLEAN = match(BOOLEAN);
				 ((DefaultValueTypeContext)_localctx).v =  (((DefaultValueTypeContext)_localctx).BOOLEAN!=null?((DefaultValueTypeContext)_localctx).BOOLEAN.getText():null); 
				}
				break;
			case LBRACK:
				enterOuterAlt(_localctx, 6);
				{
				setState(63);
				match(LBRACK);
				setState(64);
				match(RBRACK);
				 ((DefaultValueTypeContext)_localctx).v =  "java.util.List.of()"; 
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
	public static class LongRuleContext extends ParserRuleContext {
		public TerminalNode DECDIGITS() { return getToken(TemplateGrammarExpression.DECDIGITS, 0); }
		public TerminalNode MINUS() { return getToken(TemplateGrammarExpression.MINUS, 0); }
		public LongRuleContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_longRule; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarExpressionListener ) ((TemplateGrammarExpressionListener)listener).enterLongRule(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarExpressionListener ) ((TemplateGrammarExpressionListener)listener).exitLongRule(this);
		}
	}

	public final LongRuleContext longRule() throws RecognitionException {
		LongRuleContext _localctx = new LongRuleContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_longRule);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(69);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==MINUS) {
				{
				setState(68);
				match(MINUS);
				}
			}

			setState(71);
			match(DECDIGITS);
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
	public static class FunctionContext extends ParserRuleContext {
		public Ast func;
		public Token ID;
		public FunctionArgsContext functionArgs;
		public TerminalNode SEMI() { return getToken(TemplateGrammarExpression.SEMI, 0); }
		public TerminalNode ID() { return getToken(TemplateGrammarExpression.ID, 0); }
		public TerminalNode LPAREN() { return getToken(TemplateGrammarExpression.LPAREN, 0); }
		public TerminalNode RPAREN() { return getToken(TemplateGrammarExpression.RPAREN, 0); }
		public FunctionArgsContext functionArgs() {
			return getRuleContext(FunctionArgsContext.class,0);
		}
		public FunctionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_function; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarExpressionListener ) ((TemplateGrammarExpressionListener)listener).enterFunction(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarExpressionListener ) ((TemplateGrammarExpressionListener)listener).exitFunction(this);
		}
	}

	public final FunctionContext function() throws RecognitionException {
		FunctionContext _localctx = new FunctionContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_function);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(73);
			match(SEMI);
			setState(74);
			((FunctionContext)_localctx).ID = match(ID);
			setState(75);
			match(LPAREN);
			setState(77);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (((_la) & ~0x3f) == 0 && ((1L << _la) & 16252928L) != 0) {
				{
				setState(76);
				((FunctionContext)_localctx).functionArgs = functionArgs();
				}
			}

			setState(79);
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

	@SuppressWarnings("CheckReturnValue")
	public static class FunctionArgsContext extends ParserRuleContext {
		public ArrayList<String> ret = new ArrayList<>();
		public FunctionArgContext functionArg;
		public List<FunctionArgContext> functionArg() {
			return getRuleContexts(FunctionArgContext.class);
		}
		public FunctionArgContext functionArg(int i) {
			return getRuleContext(FunctionArgContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(TemplateGrammarExpression.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(TemplateGrammarExpression.COMMA, i);
		}
		public FunctionArgsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_functionArgs; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarExpressionListener ) ((TemplateGrammarExpressionListener)listener).enterFunctionArgs(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarExpressionListener ) ((TemplateGrammarExpressionListener)listener).exitFunctionArgs(this);
		}
	}

	public final FunctionArgsContext functionArgs() throws RecognitionException {
		FunctionArgsContext _localctx = new FunctionArgsContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_functionArgs);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(82);
			((FunctionArgsContext)_localctx).functionArg = functionArg();
			 _localctx.ret.add( ((FunctionArgsContext)_localctx).functionArg.ret ); 
			setState(90);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(84);
				match(COMMA);
				setState(85);
				((FunctionArgsContext)_localctx).functionArg = functionArg();
				 _localctx.ret.add( ((FunctionArgsContext)_localctx).functionArg.ret ); 
				}
				}
				setState(92);
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
	public static class FunctionArgContext extends ParserRuleContext {
		public String ret;
		public Token DECDIGITS;
		public Token FLOAT;
		public Token SSTRING;
		public Token DSTRING;
		public TerminalNode DECDIGITS() { return getToken(TemplateGrammarExpression.DECDIGITS, 0); }
		public TerminalNode MINUS() { return getToken(TemplateGrammarExpression.MINUS, 0); }
		public TerminalNode FLOAT() { return getToken(TemplateGrammarExpression.FLOAT, 0); }
		public TerminalNode SSTRING() { return getToken(TemplateGrammarExpression.SSTRING, 0); }
		public TerminalNode DSTRING() { return getToken(TemplateGrammarExpression.DSTRING, 0); }
		public FunctionArgContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_functionArg; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarExpressionListener ) ((TemplateGrammarExpressionListener)listener).enterFunctionArg(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarExpressionListener ) ((TemplateGrammarExpressionListener)listener).exitFunctionArg(this);
		}
	}

	public final FunctionArgContext functionArg() throws RecognitionException {
		FunctionArgContext _localctx = new FunctionArgContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_functionArg);
		try {
			setState(107);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,7,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(93);
				((FunctionArgContext)_localctx).DECDIGITS = match(DECDIGITS);
				 ((FunctionArgContext)_localctx).ret =  (((FunctionArgContext)_localctx).DECDIGITS!=null?((FunctionArgContext)_localctx).DECDIGITS.getText():null); 
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(95);
				match(MINUS);
				setState(96);
				((FunctionArgContext)_localctx).DECDIGITS = match(DECDIGITS);
				 ((FunctionArgContext)_localctx).ret =  "-" + (((FunctionArgContext)_localctx).DECDIGITS!=null?((FunctionArgContext)_localctx).DECDIGITS.getText():null); 
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(98);
				((FunctionArgContext)_localctx).FLOAT = match(FLOAT);
				 ((FunctionArgContext)_localctx).ret =  (((FunctionArgContext)_localctx).FLOAT!=null?((FunctionArgContext)_localctx).FLOAT.getText():null); 
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(100);
				match(MINUS);
				setState(101);
				((FunctionArgContext)_localctx).FLOAT = match(FLOAT);
				 ((FunctionArgContext)_localctx).ret =  "-" + (((FunctionArgContext)_localctx).FLOAT!=null?((FunctionArgContext)_localctx).FLOAT.getText():null); 
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(103);
				((FunctionArgContext)_localctx).SSTRING = match(SSTRING);
				 ((FunctionArgContext)_localctx).ret =  sStringToDString( (((FunctionArgContext)_localctx).SSTRING!=null?((FunctionArgContext)_localctx).SSTRING.getText():null) ); 
				}
				break;
			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(105);
				((FunctionArgContext)_localctx).DSTRING = match(DSTRING);
				 ((FunctionArgContext)_localctx).ret =  (((FunctionArgContext)_localctx).DSTRING!=null?((FunctionArgContext)_localctx).DSTRING.getText():null); 
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

	@SuppressWarnings("CheckReturnValue")
	public static class OrExpsContext extends ParserRuleContext {
		public TemplateType parentType;
		public MaxMin firstAst;
		public MaxMin ast;
		public ArrayList<MaxMin> list = new ArrayList<>();;
		public ExpsContext exps;
		public List<TerminalNode> PIPE() { return getTokens(TemplateGrammarExpression.PIPE); }
		public TerminalNode PIPE(int i) {
			return getToken(TemplateGrammarExpression.PIPE, i);
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
			if ( listener instanceof TemplateGrammarExpressionListener ) ((TemplateGrammarExpressionListener)listener).enterOrExps(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarExpressionListener ) ((TemplateGrammarExpressionListener)listener).exitOrExps(this);
		}
	}

	public final OrExpsContext orExps(TemplateType parentType,MaxMin firstAst) throws RecognitionException {
		OrExpsContext _localctx = new OrExpsContext(_ctx, getState(), parentType, firstAst);
		enterRule(_localctx, 14, RULE_orExps);
		int _la;
		try {
			setState(124);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case PIPE:
				enterOuterAlt(_localctx, 1);
				{
				{
				setState(109);
				match(PIPE);
				setState(110);
				((OrExpsContext)_localctx).exps = exps(parentType);
				 _localctx.list.add(firstAst); _localctx.list.add(((OrExpsContext)_localctx).exps.ast); 
				setState(118);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==PIPE) {
					{
					{
					setState(112);
					match(PIPE);
					setState(113);
					((OrExpsContext)_localctx).exps = exps(parentType);
					_localctx.list.add(((OrExpsContext)_localctx).exps.ast);
					}
					}
					setState(120);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}

				        if( _localctx.list.isEmpty() ) ((OrExpsContext)_localctx).ast =  firstAst;
				        else {
				            var or = new AstOr(parentType);
				            for( var item : _localctx.list) {
				              item.addToBottomChildrenAndSet(getAst(item.bottom.type, null, false, null));
				            }
				            or.addTry(_localctx.list);
				            ((OrExpsContext)_localctx).ast =  new MaxMin(or);
				        }    
				    
				}
				break;
			case EOF:
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

	@SuppressWarnings("CheckReturnValue")
	public static class ExpsContext extends ParserRuleContext {
		public TemplateType parentType;
		public MaxMin ast;
		public ExpContext exp;
		public ConcatenationContext concatenation;
		public MathContext math;
		public List<ExpContext> exp() {
			return getRuleContexts(ExpContext.class);
		}
		public ExpContext exp(int i) {
			return getRuleContext(ExpContext.class,i);
		}
		public List<TerminalNode> DOT() { return getTokens(TemplateGrammarExpression.DOT); }
		public TerminalNode DOT(int i) {
			return getToken(TemplateGrammarExpression.DOT, i);
		}
		public ConcatenationContext concatenation() {
			return getRuleContext(ConcatenationContext.class,0);
		}
		public MathContext math() {
			return getRuleContext(MathContext.class,0);
		}
		public ExpsContext(ParserRuleContext parent, int invokingState) { super(parent, invokingState); }
		public ExpsContext(ParserRuleContext parent, int invokingState, TemplateType parentType) {
			super(parent, invokingState);
			this.parentType = parentType;
		}
		@Override public int getRuleIndex() { return RULE_exps; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarExpressionListener ) ((TemplateGrammarExpressionListener)listener).enterExps(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarExpressionListener ) ((TemplateGrammarExpressionListener)listener).exitExps(this);
		}
	}

	public final ExpsContext exps(TemplateType parentType) throws RecognitionException {
		ExpsContext _localctx = new ExpsContext(_ctx, getState(), parentType);
		enterRule(_localctx, 16, RULE_exps);
		int _la;
		try {
			int _alt;
			setState(152);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,14,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				{
				setState(126);
				((ExpsContext)_localctx).exp = exp(parentType);
				 ((ExpsContext)_localctx).ast =  ((ExpsContext)_localctx).exp.ast; 
				setState(134);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,10,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(128);
						match(DOT);
						setState(129);
						((ExpsContext)_localctx).exp = exp(_localctx.ast.bottom.type);
						_localctx.ast.addToBottomChildrenAndSet(((ExpsContext)_localctx).exp.ast);
						}
						} 
					}
					setState(136);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,10,_ctx);
				}
				}
				setState(138);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==DOT) {
					{
					setState(137);
					match(DOT);
					}
				}

				setState(141);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==LBRACE || _la==CAST_TYPE) {
					{
					setState(140);
					((ExpsContext)_localctx).concatenation = concatenation(_localctx.ast.bottom.type);
					}
				}

				 if( ((ExpsContext)_localctx).concatenation != null ) _localctx.ast.addToBottomChildrenAndSet( ((ExpsContext)_localctx).concatenation.ast ); 
				setState(145);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (((_la) & ~0x3f) == 0 && ((1L << _la) & 1015808L) != 0) {
					{
					setState(144);
					((ExpsContext)_localctx).math = math(_localctx.ast.bottom.type);
					}
				}

				 if( ((ExpsContext)_localctx).math != null ) _localctx.ast.addToBottomChildrenAndSet( ((ExpsContext)_localctx).math.ast ); 
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(149);
				((ExpsContext)_localctx).concatenation = concatenation(parentType);
				 ((ExpsContext)_localctx).ast =  new MaxMin( ((ExpsContext)_localctx).concatenation.ast ); 
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

	@SuppressWarnings("CheckReturnValue")
	public static class ExpContext extends ParserRuleContext {
		public TemplateType parentType;
		public MaxMin ast;
		public Token CAST_TYPE;
		public Token ID;
		public FunctionArgsContext functionArgs;
		public TerminalNode ID() { return getToken(TemplateGrammarExpression.ID, 0); }
		public TerminalNode LPAREN() { return getToken(TemplateGrammarExpression.LPAREN, 0); }
		public TerminalNode RPAREN() { return getToken(TemplateGrammarExpression.RPAREN, 0); }
		public TerminalNode CAST_TYPE() { return getToken(TemplateGrammarExpression.CAST_TYPE, 0); }
		public FunctionArgsContext functionArgs() {
			return getRuleContext(FunctionArgsContext.class,0);
		}
		public ExpContext(ParserRuleContext parent, int invokingState) { super(parent, invokingState); }
		public ExpContext(ParserRuleContext parent, int invokingState, TemplateType parentType) {
			super(parent, invokingState);
			this.parentType = parentType;
		}
		@Override public int getRuleIndex() { return RULE_exp; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarExpressionListener ) ((TemplateGrammarExpressionListener)listener).enterExp(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarExpressionListener ) ((TemplateGrammarExpressionListener)listener).exitExp(this);
		}
	}

	public final ExpContext exp(TemplateType parentType) throws RecognitionException {
		ExpContext _localctx = new ExpContext(_ctx, getState(), parentType);
		enterRule(_localctx, 18, RULE_exp);
		int _la;
		try {
			setState(170);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,18,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				{
				setState(155);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==CAST_TYPE) {
					{
					setState(154);
					((ExpContext)_localctx).CAST_TYPE = match(CAST_TYPE);
					}
				}

				setState(157);
				((ExpContext)_localctx).ID = match(ID);
				setState(158);
				match(LPAREN);
				setState(160);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (((_la) & ~0x3f) == 0 && ((1L << _la) & 16252928L) != 0) {
					{
					setState(159);
					((ExpContext)_localctx).functionArgs = functionArgs();
					}
				}

				setState(162);
				match(RPAREN);
				}
				 ((ExpContext)_localctx).ast =  getAst(_localctx.parentType, (((ExpContext)_localctx).ID!=null?((ExpContext)_localctx).ID.getText():null), true, ((ExpContext)_localctx).functionArgs != null ? ((ExpContext)_localctx).functionArgs.ret : List.of(), ((ExpContext)_localctx).CAST_TYPE != null ? getCastType((((ExpContext)_localctx).CAST_TYPE!=null?((ExpContext)_localctx).CAST_TYPE.getText():null)) : null ); 
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(166);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==CAST_TYPE) {
					{
					setState(165);
					((ExpContext)_localctx).CAST_TYPE = match(CAST_TYPE);
					}
				}

				setState(168);
				((ExpContext)_localctx).ID = match(ID);
				 ((ExpContext)_localctx).ast =  getAst(_localctx.parentType, (((ExpContext)_localctx).ID!=null?((ExpContext)_localctx).ID.getText():null), false, ((ExpContext)_localctx).CAST_TYPE != null ? getCastType((((ExpContext)_localctx).CAST_TYPE!=null?((ExpContext)_localctx).CAST_TYPE.getText():null)) : null ); 
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

	@SuppressWarnings("CheckReturnValue")
	public static class ConcatenationContext extends ParserRuleContext {
		public TemplateType parentType;
		public AstConcatenation ast;
		public Token CAST_TYPE;
		public CitemsContext citems;
		public TerminalNode LBRACE() { return getToken(TemplateGrammarExpression.LBRACE, 0); }
		public CitemsContext citems() {
			return getRuleContext(CitemsContext.class,0);
		}
		public TerminalNode RBRACE() { return getToken(TemplateGrammarExpression.RBRACE, 0); }
		public TerminalNode CAST_TYPE() { return getToken(TemplateGrammarExpression.CAST_TYPE, 0); }
		public ConcatenationContext(ParserRuleContext parent, int invokingState) { super(parent, invokingState); }
		public ConcatenationContext(ParserRuleContext parent, int invokingState, TemplateType parentType) {
			super(parent, invokingState);
			this.parentType = parentType;
		}
		@Override public int getRuleIndex() { return RULE_concatenation; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarExpressionListener ) ((TemplateGrammarExpressionListener)listener).enterConcatenation(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarExpressionListener ) ((TemplateGrammarExpressionListener)listener).exitConcatenation(this);
		}
	}

	public final ConcatenationContext concatenation(TemplateType parentType) throws RecognitionException {
		ConcatenationContext _localctx = new ConcatenationContext(_ctx, getState(), parentType);
		enterRule(_localctx, 20, RULE_concatenation);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(173);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==CAST_TYPE) {
				{
				setState(172);
				((ConcatenationContext)_localctx).CAST_TYPE = match(CAST_TYPE);
				}
			}

			setState(175);
			match(LBRACE);
			setState(176);
			((ConcatenationContext)_localctx).citems = citems(parentType);

			        try {
			        com.google.common.base.Preconditions.checkArgument(  ((ConcatenationContext)_localctx).CAST_TYPE == null || oap.template.LogConfiguration.FieldType.parse( (((ConcatenationContext)_localctx).CAST_TYPE!=null?((ConcatenationContext)_localctx).CAST_TYPE.getText():null).substring(1, (((ConcatenationContext)_localctx).CAST_TYPE!=null?((ConcatenationContext)_localctx).CAST_TYPE.getText():null).length() - 1) ).equals( new oap.template.LogConfiguration.FieldType( String.class )));
			        ((ConcatenationContext)_localctx).ast =  new AstConcatenation(parentType, ((ConcatenationContext)_localctx).citems.list);
			        } catch ( java.lang.ClassNotFoundException e) {
			          throw new TemplateException( e.getMessage(), e );
			        }
			      
			setState(178);
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
		public List<TerminalNode> COMMA() { return getTokens(TemplateGrammarExpression.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(TemplateGrammarExpression.COMMA, i);
		}
		public CitemsContext(ParserRuleContext parent, int invokingState) { super(parent, invokingState); }
		public CitemsContext(ParserRuleContext parent, int invokingState, TemplateType parentType) {
			super(parent, invokingState);
			this.parentType = parentType;
		}
		@Override public int getRuleIndex() { return RULE_citems; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarExpressionListener ) ((TemplateGrammarExpressionListener)listener).enterCitems(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarExpressionListener ) ((TemplateGrammarExpressionListener)listener).exitCitems(this);
		}
	}

	public final CitemsContext citems(TemplateType parentType) throws RecognitionException {
		CitemsContext _localctx = new CitemsContext(_ctx, getState(), parentType);
		enterRule(_localctx, 22, RULE_citems);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(180);
			((CitemsContext)_localctx).citem = citem(parentType);
			 _localctx.list.add(((CitemsContext)_localctx).citem.ast.top); ((CitemsContext)_localctx).citem.ast.addToBottomChildrenAndSet(getAst(((CitemsContext)_localctx).citem.ast.bottom.type, null, false, null)); 
			setState(188);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(182);
				match(COMMA);
				setState(183);
				((CitemsContext)_localctx).citem = citem(parentType);
				 _localctx.list.add(((CitemsContext)_localctx).citem.ast.top); ((CitemsContext)_localctx).citem.ast.addToBottomChildrenAndSet(getAst(((CitemsContext)_localctx).citem.ast.bottom.type, null, false, null)); 
				}
				}
				setState(190);
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
	public static class CitemContext extends ParserRuleContext {
		public TemplateType parentType;
		public MaxMin ast;
		public Token ID;
		public Token DSTRING;
		public Token SSTRING;
		public Token DECDIGITS;
		public Token FLOAT;
		public TerminalNode ID() { return getToken(TemplateGrammarExpression.ID, 0); }
		public TerminalNode DSTRING() { return getToken(TemplateGrammarExpression.DSTRING, 0); }
		public TerminalNode SSTRING() { return getToken(TemplateGrammarExpression.SSTRING, 0); }
		public TerminalNode DECDIGITS() { return getToken(TemplateGrammarExpression.DECDIGITS, 0); }
		public TerminalNode FLOAT() { return getToken(TemplateGrammarExpression.FLOAT, 0); }
		public CitemContext(ParserRuleContext parent, int invokingState) { super(parent, invokingState); }
		public CitemContext(ParserRuleContext parent, int invokingState, TemplateType parentType) {
			super(parent, invokingState);
			this.parentType = parentType;
		}
		@Override public int getRuleIndex() { return RULE_citem; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarExpressionListener ) ((TemplateGrammarExpressionListener)listener).enterCitem(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarExpressionListener ) ((TemplateGrammarExpressionListener)listener).exitCitem(this);
		}
	}

	public final CitemContext citem(TemplateType parentType) throws RecognitionException {
		CitemContext _localctx = new CitemContext(_ctx, getState(), parentType);
		enterRule(_localctx, 24, RULE_citem);
		try {
			setState(201);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case ID:
				enterOuterAlt(_localctx, 1);
				{
				setState(191);
				((CitemContext)_localctx).ID = match(ID);
				 ((CitemContext)_localctx).ast =  getAst(_localctx.parentType, (((CitemContext)_localctx).ID!=null?((CitemContext)_localctx).ID.getText():null), false, null); 
				}
				break;
			case DSTRING:
				enterOuterAlt(_localctx, 2);
				{
				setState(193);
				((CitemContext)_localctx).DSTRING = match(DSTRING);
				 ((CitemContext)_localctx).ast =  new MaxMin(new AstText(sdStringToString((((CitemContext)_localctx).DSTRING!=null?((CitemContext)_localctx).DSTRING.getText():null)))); 
				}
				break;
			case SSTRING:
				enterOuterAlt(_localctx, 3);
				{
				setState(195);
				((CitemContext)_localctx).SSTRING = match(SSTRING);
				 ((CitemContext)_localctx).ast =  new MaxMin(new AstText(sdStringToString((((CitemContext)_localctx).SSTRING!=null?((CitemContext)_localctx).SSTRING.getText():null)))); 
				}
				break;
			case DECDIGITS:
				enterOuterAlt(_localctx, 4);
				{
				setState(197);
				((CitemContext)_localctx).DECDIGITS = match(DECDIGITS);
				 ((CitemContext)_localctx).ast =  new MaxMin(new AstText(String.valueOf((((CitemContext)_localctx).DECDIGITS!=null?((CitemContext)_localctx).DECDIGITS.getText():null)))); 
				}
				break;
			case FLOAT:
				enterOuterAlt(_localctx, 5);
				{
				setState(199);
				((CitemContext)_localctx).FLOAT = match(FLOAT);
				 ((CitemContext)_localctx).ast =  new MaxMin(new AstText(String.valueOf((((CitemContext)_localctx).FLOAT!=null?((CitemContext)_localctx).FLOAT.getText():null)))); 
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
	public static class MathContext extends ParserRuleContext {
		public TemplateType parentType;
		public MaxMin ast;
		public MathOperationContext mathOperation;
		public NumberContext number;
		public MathOperationContext mathOperation() {
			return getRuleContext(MathOperationContext.class,0);
		}
		public NumberContext number() {
			return getRuleContext(NumberContext.class,0);
		}
		public MathContext(ParserRuleContext parent, int invokingState) { super(parent, invokingState); }
		public MathContext(ParserRuleContext parent, int invokingState, TemplateType parentType) {
			super(parent, invokingState);
			this.parentType = parentType;
		}
		@Override public int getRuleIndex() { return RULE_math; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarExpressionListener ) ((TemplateGrammarExpressionListener)listener).enterMath(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarExpressionListener ) ((TemplateGrammarExpressionListener)listener).exitMath(this);
		}
	}

	public final MathContext math(TemplateType parentType) throws RecognitionException {
		MathContext _localctx = new MathContext(_ctx, getState(), parentType);
		enterRule(_localctx, 26, RULE_math);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(203);
			((MathContext)_localctx).mathOperation = mathOperation();
			setState(204);
			((MathContext)_localctx).number = number();
			 ((MathContext)_localctx).ast =  new MaxMin(new AstMath(_localctx.parentType, (((MathContext)_localctx).mathOperation!=null?_input.getText(((MathContext)_localctx).mathOperation.start,((MathContext)_localctx).mathOperation.stop):null), (((MathContext)_localctx).number!=null?_input.getText(((MathContext)_localctx).number.start,((MathContext)_localctx).number.stop):null))); 
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
	public static class NumberContext extends ParserRuleContext {
		public TerminalNode DECDIGITS() { return getToken(TemplateGrammarExpression.DECDIGITS, 0); }
		public TerminalNode FLOAT() { return getToken(TemplateGrammarExpression.FLOAT, 0); }
		public NumberContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_number; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarExpressionListener ) ((TemplateGrammarExpressionListener)listener).enterNumber(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarExpressionListener ) ((TemplateGrammarExpressionListener)listener).exitNumber(this);
		}
	}

	public final NumberContext number() throws RecognitionException {
		NumberContext _localctx = new NumberContext(_ctx, getState());
		enterRule(_localctx, 28, RULE_number);
		try {
			setState(210);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case EOF:
			case PIPE:
			case DQUESTION:
			case SEMI:
				enterOuterAlt(_localctx, 1);
				{
				}
				break;
			case DECDIGITS:
				enterOuterAlt(_localctx, 2);
				{
				setState(208);
				match(DECDIGITS);
				}
				break;
			case FLOAT:
				enterOuterAlt(_localctx, 3);
				{
				setState(209);
				match(FLOAT);
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
	public static class MathOperationContext extends ParserRuleContext {
		public TerminalNode STAR() { return getToken(TemplateGrammarExpression.STAR, 0); }
		public TerminalNode SLASH() { return getToken(TemplateGrammarExpression.SLASH, 0); }
		public TerminalNode PERCENT() { return getToken(TemplateGrammarExpression.PERCENT, 0); }
		public TerminalNode PLUS() { return getToken(TemplateGrammarExpression.PLUS, 0); }
		public TerminalNode MINUS() { return getToken(TemplateGrammarExpression.MINUS, 0); }
		public MathOperationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_mathOperation; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarExpressionListener ) ((TemplateGrammarExpressionListener)listener).enterMathOperation(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarExpressionListener ) ((TemplateGrammarExpressionListener)listener).exitMathOperation(this);
		}
	}

	public final MathOperationContext mathOperation() throws RecognitionException {
		MathOperationContext _localctx = new MathOperationContext(_ctx, getState());
		enterRule(_localctx, 30, RULE_mathOperation);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(212);
			_la = _input.LA(1);
			if ( !(((_la) & ~0x3f) == 0 && ((1L << _la) & 1015808L) != 0) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
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

	public static final String _serializedATN =
		"\u0004\u0001\u001e\u00d7\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001"+
		"\u0002\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004\u0007\u0004"+
		"\u0002\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002\u0007\u0007\u0007"+
		"\u0002\b\u0007\b\u0002\t\u0007\t\u0002\n\u0007\n\u0002\u000b\u0007\u000b"+
		"\u0002\f\u0007\f\u0002\r\u0007\r\u0002\u000e\u0007\u000e\u0002\u000f\u0007"+
		"\u000f\u0001\u0000\u0001\u0000\u0003\u0000#\b\u0000\u0001\u0000\u0001"+
		"\u0000\u0001\u0000\u0001\u0000\u0001\u0000\u0003\u0000*\b\u0000\u0001"+
		"\u0000\u0003\u0000-\b\u0000\u0001\u0000\u0001\u0000\u0001\u0001\u0001"+
		"\u0001\u0001\u0001\u0001\u0001\u0001\u0002\u0001\u0002\u0001\u0002\u0001"+
		"\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001"+
		"\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0003\u0002C\b"+
		"\u0002\u0001\u0003\u0003\u0003F\b\u0003\u0001\u0003\u0001\u0003\u0001"+
		"\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0003\u0004N\b\u0004\u0001"+
		"\u0004\u0001\u0004\u0001\u0004\u0001\u0005\u0001\u0005\u0001\u0005\u0001"+
		"\u0005\u0001\u0005\u0001\u0005\u0005\u0005Y\b\u0005\n\u0005\f\u0005\\"+
		"\t\u0005\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0001"+
		"\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0001"+
		"\u0006\u0001\u0006\u0001\u0006\u0003\u0006l\b\u0006\u0001\u0007\u0001"+
		"\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0005"+
		"\u0007u\b\u0007\n\u0007\f\u0007x\t\u0007\u0001\u0007\u0001\u0007\u0001"+
		"\u0007\u0003\u0007}\b\u0007\u0001\b\u0001\b\u0001\b\u0001\b\u0001\b\u0001"+
		"\b\u0005\b\u0085\b\b\n\b\f\b\u0088\t\b\u0001\b\u0003\b\u008b\b\b\u0001"+
		"\b\u0003\b\u008e\b\b\u0001\b\u0001\b\u0003\b\u0092\b\b\u0001\b\u0001\b"+
		"\u0001\b\u0001\b\u0001\b\u0003\b\u0099\b\b\u0001\t\u0003\t\u009c\b\t\u0001"+
		"\t\u0001\t\u0001\t\u0003\t\u00a1\b\t\u0001\t\u0001\t\u0001\t\u0001\t\u0003"+
		"\t\u00a7\b\t\u0001\t\u0001\t\u0003\t\u00ab\b\t\u0001\n\u0003\n\u00ae\b"+
		"\n\u0001\n\u0001\n\u0001\n\u0001\n\u0001\n\u0001\u000b\u0001\u000b\u0001"+
		"\u000b\u0001\u000b\u0001\u000b\u0001\u000b\u0005\u000b\u00bb\b\u000b\n"+
		"\u000b\f\u000b\u00be\t\u000b\u0001\f\u0001\f\u0001\f\u0001\f\u0001\f\u0001"+
		"\f\u0001\f\u0001\f\u0001\f\u0001\f\u0003\f\u00ca\b\f\u0001\r\u0001\r\u0001"+
		"\r\u0001\r\u0001\u000e\u0001\u000e\u0001\u000e\u0003\u000e\u00d3\b\u000e"+
		"\u0001\u000f\u0001\u000f\u0001\u000f\u0000\u0000\u0010\u0000\u0002\u0004"+
		"\u0006\b\n\f\u000e\u0010\u0012\u0014\u0016\u0018\u001a\u001c\u001e\u0000"+
		"\u0001\u0001\u0000\u000f\u0013\u00e9\u0000\"\u0001\u0000\u0000\u0000\u0002"+
		"0\u0001\u0000\u0000\u0000\u0004B\u0001\u0000\u0000\u0000\u0006E\u0001"+
		"\u0000\u0000\u0000\bI\u0001\u0000\u0000\u0000\nR\u0001\u0000\u0000\u0000"+
		"\fk\u0001\u0000\u0000\u0000\u000e|\u0001\u0000\u0000\u0000\u0010\u0098"+
		"\u0001\u0000\u0000\u0000\u0012\u00aa\u0001\u0000\u0000\u0000\u0014\u00ad"+
		"\u0001\u0000\u0000\u0000\u0016\u00b4\u0001\u0000\u0000\u0000\u0018\u00c9"+
		"\u0001\u0000\u0000\u0000\u001a\u00cb\u0001\u0000\u0000\u0000\u001c\u00d2"+
		"\u0001\u0000\u0000\u0000\u001e\u00d4\u0001\u0000\u0000\u0000 !\u0005\u0001"+
		"\u0000\u0000!#\u0006\u0000\uffff\uffff\u0000\" \u0001\u0000\u0000\u0000"+
		"\"#\u0001\u0000\u0000\u0000#$\u0001\u0000\u0000\u0000$%\u0003\u0010\b"+
		"\u0000%&\u0006\u0000\uffff\uffff\u0000&\'\u0003\u000e\u0007\u0000\')\u0006"+
		"\u0000\uffff\uffff\u0000(*\u0003\u0002\u0001\u0000)(\u0001\u0000\u0000"+
		"\u0000)*\u0001\u0000\u0000\u0000*,\u0001\u0000\u0000\u0000+-\u0003\b\u0004"+
		"\u0000,+\u0001\u0000\u0000\u0000,-\u0001\u0000\u0000\u0000-.\u0001\u0000"+
		"\u0000\u0000./\u0006\u0000\uffff\uffff\u0000/\u0001\u0001\u0000\u0000"+
		"\u000001\u0005\f\u0000\u000012\u0003\u0004\u0002\u000023\u0006\u0001\uffff"+
		"\uffff\u00003\u0003\u0001\u0000\u0000\u000045\u0005\u0015\u0000\u0000"+
		"5C\u0006\u0002\uffff\uffff\u000067\u0005\u0014\u0000\u00007C\u0006\u0002"+
		"\uffff\uffff\u000089\u0003\u0006\u0003\u00009:\u0006\u0002\uffff\uffff"+
		"\u0000:C\u0001\u0000\u0000\u0000;<\u0005\u0017\u0000\u0000<C\u0006\u0002"+
		"\uffff\uffff\u0000=>\u0005\u0018\u0000\u0000>C\u0006\u0002\uffff\uffff"+
		"\u0000?@\u0005\n\u0000\u0000@A\u0005\u000b\u0000\u0000AC\u0006\u0002\uffff"+
		"\uffff\u0000B4\u0001\u0000\u0000\u0000B6\u0001\u0000\u0000\u0000B8\u0001"+
		"\u0000\u0000\u0000B;\u0001\u0000\u0000\u0000B=\u0001\u0000\u0000\u0000"+
		"B?\u0001\u0000\u0000\u0000C\u0005\u0001\u0000\u0000\u0000DF\u0005\u0013"+
		"\u0000\u0000ED\u0001\u0000\u0000\u0000EF\u0001\u0000\u0000\u0000FG\u0001"+
		"\u0000\u0000\u0000GH\u0005\u0016\u0000\u0000H\u0007\u0001\u0000\u0000"+
		"\u0000IJ\u0005\r\u0000\u0000JK\u0005\u0019\u0000\u0000KM\u0005\b\u0000"+
		"\u0000LN\u0003\n\u0005\u0000ML\u0001\u0000\u0000\u0000MN\u0001\u0000\u0000"+
		"\u0000NO\u0001\u0000\u0000\u0000OP\u0005\t\u0000\u0000PQ\u0006\u0004\uffff"+
		"\uffff\u0000Q\t\u0001\u0000\u0000\u0000RS\u0003\f\u0006\u0000SZ\u0006"+
		"\u0005\uffff\uffff\u0000TU\u0005\u000e\u0000\u0000UV\u0003\f\u0006\u0000"+
		"VW\u0006\u0005\uffff\uffff\u0000WY\u0001\u0000\u0000\u0000XT\u0001\u0000"+
		"\u0000\u0000Y\\\u0001\u0000\u0000\u0000ZX\u0001\u0000\u0000\u0000Z[\u0001"+
		"\u0000\u0000\u0000[\u000b\u0001\u0000\u0000\u0000\\Z\u0001\u0000\u0000"+
		"\u0000]^\u0005\u0016\u0000\u0000^l\u0006\u0006\uffff\uffff\u0000_`\u0005"+
		"\u0013\u0000\u0000`a\u0005\u0016\u0000\u0000al\u0006\u0006\uffff\uffff"+
		"\u0000bc\u0005\u0017\u0000\u0000cl\u0006\u0006\uffff\uffff\u0000de\u0005"+
		"\u0013\u0000\u0000ef\u0005\u0017\u0000\u0000fl\u0006\u0006\uffff\uffff"+
		"\u0000gh\u0005\u0015\u0000\u0000hl\u0006\u0006\uffff\uffff\u0000ij\u0005"+
		"\u0014\u0000\u0000jl\u0006\u0006\uffff\uffff\u0000k]\u0001\u0000\u0000"+
		"\u0000k_\u0001\u0000\u0000\u0000kb\u0001\u0000\u0000\u0000kd\u0001\u0000"+
		"\u0000\u0000kg\u0001\u0000\u0000\u0000ki\u0001\u0000\u0000\u0000l\r\u0001"+
		"\u0000\u0000\u0000mn\u0005\u0006\u0000\u0000no\u0003\u0010\b\u0000ov\u0006"+
		"\u0007\uffff\uffff\u0000pq\u0005\u0006\u0000\u0000qr\u0003\u0010\b\u0000"+
		"rs\u0006\u0007\uffff\uffff\u0000su\u0001\u0000\u0000\u0000tp\u0001\u0000"+
		"\u0000\u0000ux\u0001\u0000\u0000\u0000vt\u0001\u0000\u0000\u0000vw\u0001"+
		"\u0000\u0000\u0000wy\u0001\u0000\u0000\u0000xv\u0001\u0000\u0000\u0000"+
		"yz\u0006\u0007\uffff\uffff\u0000z}\u0001\u0000\u0000\u0000{}\u0006\u0007"+
		"\uffff\uffff\u0000|m\u0001\u0000\u0000\u0000|{\u0001\u0000\u0000\u0000"+
		"}\u000f\u0001\u0000\u0000\u0000~\u007f\u0003\u0012\t\u0000\u007f\u0086"+
		"\u0006\b\uffff\uffff\u0000\u0080\u0081\u0005\u0007\u0000\u0000\u0081\u0082"+
		"\u0003\u0012\t\u0000\u0082\u0083\u0006\b\uffff\uffff\u0000\u0083\u0085"+
		"\u0001\u0000\u0000\u0000\u0084\u0080\u0001\u0000\u0000\u0000\u0085\u0088"+
		"\u0001\u0000\u0000\u0000\u0086\u0084\u0001\u0000\u0000\u0000\u0086\u0087"+
		"\u0001\u0000\u0000\u0000\u0087\u008a\u0001\u0000\u0000\u0000\u0088\u0086"+
		"\u0001\u0000\u0000\u0000\u0089\u008b\u0005\u0007\u0000\u0000\u008a\u0089"+
		"\u0001\u0000\u0000\u0000\u008a\u008b\u0001\u0000\u0000\u0000\u008b\u008d"+
		"\u0001\u0000\u0000\u0000\u008c\u008e\u0003\u0014\n\u0000\u008d\u008c\u0001"+
		"\u0000\u0000\u0000\u008d\u008e\u0001\u0000\u0000\u0000\u008e\u008f\u0001"+
		"\u0000\u0000\u0000\u008f\u0091\u0006\b\uffff\uffff\u0000\u0090\u0092\u0003"+
		"\u001a\r\u0000\u0091\u0090\u0001\u0000\u0000\u0000\u0091\u0092\u0001\u0000"+
		"\u0000\u0000\u0092\u0093\u0001\u0000\u0000\u0000\u0093\u0094\u0006\b\uffff"+
		"\uffff\u0000\u0094\u0099\u0001\u0000\u0000\u0000\u0095\u0096\u0003\u0014"+
		"\n\u0000\u0096\u0097\u0006\b\uffff\uffff\u0000\u0097\u0099\u0001\u0000"+
		"\u0000\u0000\u0098~\u0001\u0000\u0000\u0000\u0098\u0095\u0001\u0000\u0000"+
		"\u0000\u0099\u0011\u0001\u0000\u0000\u0000\u009a\u009c\u0005\u001a\u0000"+
		"\u0000\u009b\u009a\u0001\u0000\u0000\u0000\u009b\u009c\u0001\u0000\u0000"+
		"\u0000\u009c\u009d\u0001\u0000\u0000\u0000\u009d\u009e\u0005\u0019\u0000"+
		"\u0000\u009e\u00a0\u0005\b\u0000\u0000\u009f\u00a1\u0003\n\u0005\u0000"+
		"\u00a0\u009f\u0001\u0000\u0000\u0000\u00a0\u00a1\u0001\u0000\u0000\u0000"+
		"\u00a1\u00a2\u0001\u0000\u0000\u0000\u00a2\u00a3\u0005\t\u0000\u0000\u00a3"+
		"\u00a4\u0001\u0000\u0000\u0000\u00a4\u00ab\u0006\t\uffff\uffff\u0000\u00a5"+
		"\u00a7\u0005\u001a\u0000\u0000\u00a6\u00a5\u0001\u0000\u0000\u0000\u00a6"+
		"\u00a7\u0001\u0000\u0000\u0000\u00a7\u00a8\u0001\u0000\u0000\u0000\u00a8"+
		"\u00a9\u0005\u0019\u0000\u0000\u00a9\u00ab\u0006\t\uffff\uffff\u0000\u00aa"+
		"\u009b\u0001\u0000\u0000\u0000\u00aa\u00a6\u0001\u0000\u0000\u0000\u00ab"+
		"\u0013\u0001\u0000\u0000\u0000\u00ac\u00ae\u0005\u001a\u0000\u0000\u00ad"+
		"\u00ac\u0001\u0000\u0000\u0000\u00ad\u00ae\u0001\u0000\u0000\u0000\u00ae"+
		"\u00af\u0001\u0000\u0000\u0000\u00af\u00b0\u0005\u0004\u0000\u0000\u00b0"+
		"\u00b1\u0003\u0016\u000b\u0000\u00b1\u00b2\u0006\n\uffff\uffff\u0000\u00b2"+
		"\u00b3\u0005\u0005\u0000\u0000\u00b3\u0015\u0001\u0000\u0000\u0000\u00b4"+
		"\u00b5\u0003\u0018\f\u0000\u00b5\u00bc\u0006\u000b\uffff\uffff\u0000\u00b6"+
		"\u00b7\u0005\u000e\u0000\u0000\u00b7\u00b8\u0003\u0018\f\u0000\u00b8\u00b9"+
		"\u0006\u000b\uffff\uffff\u0000\u00b9\u00bb\u0001\u0000\u0000\u0000\u00ba"+
		"\u00b6\u0001\u0000\u0000\u0000\u00bb\u00be\u0001\u0000\u0000\u0000\u00bc"+
		"\u00ba\u0001\u0000\u0000\u0000\u00bc\u00bd\u0001\u0000\u0000\u0000\u00bd"+
		"\u0017\u0001\u0000\u0000\u0000\u00be\u00bc\u0001\u0000\u0000\u0000\u00bf"+
		"\u00c0\u0005\u0019\u0000\u0000\u00c0\u00ca\u0006\f\uffff\uffff\u0000\u00c1"+
		"\u00c2\u0005\u0014\u0000\u0000\u00c2\u00ca\u0006\f\uffff\uffff\u0000\u00c3"+
		"\u00c4\u0005\u0015\u0000\u0000\u00c4\u00ca\u0006\f\uffff\uffff\u0000\u00c5"+
		"\u00c6\u0005\u0016\u0000\u0000\u00c6\u00ca\u0006\f\uffff\uffff\u0000\u00c7"+
		"\u00c8\u0005\u0017\u0000\u0000\u00c8\u00ca\u0006\f\uffff\uffff\u0000\u00c9"+
		"\u00bf\u0001\u0000\u0000\u0000\u00c9\u00c1\u0001\u0000\u0000\u0000\u00c9"+
		"\u00c3\u0001\u0000\u0000\u0000\u00c9\u00c5\u0001\u0000\u0000\u0000\u00c9"+
		"\u00c7\u0001\u0000\u0000\u0000\u00ca\u0019\u0001\u0000\u0000\u0000\u00cb"+
		"\u00cc\u0003\u001e\u000f\u0000\u00cc\u00cd\u0003\u001c\u000e\u0000\u00cd"+
		"\u00ce\u0006\r\uffff\uffff\u0000\u00ce\u001b\u0001\u0000\u0000\u0000\u00cf"+
		"\u00d3\u0001\u0000\u0000\u0000\u00d0\u00d3\u0005\u0016\u0000\u0000\u00d1"+
		"\u00d3\u0005\u0017\u0000\u0000\u00d2\u00cf\u0001\u0000\u0000\u0000\u00d2"+
		"\u00d0\u0001\u0000\u0000\u0000\u00d2\u00d1\u0001\u0000\u0000\u0000\u00d3"+
		"\u001d\u0001\u0000\u0000\u0000\u00d4\u00d5\u0007\u0000\u0000\u0000\u00d5"+
		"\u001f\u0001\u0000\u0000\u0000\u0017\"),BEMZkv|\u0086\u008a\u008d\u0091"+
		"\u0098\u009b\u00a0\u00a6\u00aa\u00ad\u00bc\u00c9\u00d2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}