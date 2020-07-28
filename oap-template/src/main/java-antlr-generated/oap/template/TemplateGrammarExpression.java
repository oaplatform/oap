// Generated from TemplateGrammarExpression.g4 by ANTLR 4.8

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
public class TemplateGrammarExpression extends TemplateGrammarAdaptor {
	static { RuntimeMetaData.checkVersion("4.8", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		BLOCK_COMMENT=1, HORZ_WS=2, VERT_WS=3, LBRACE=4, RBRACE=5, PIPE=6, DOT=7, 
		LPAREN=8, RPAREN=9, LBRACK=10, RBRACK=11, DQUESTION=12, SEMI=13, COMMA=14, 
		STAR=15, SLASH=16, PERCENT=17, PLUS=18, MINUS=19, DSTRING=20, SSTRING=21, 
		DECDIGITS=22, FLOAT=23, BOOLEAN=24, ID=25, ERR_CHAR=26, C_HORZ_WS=27, 
		C_VERT_WS=28, CERR_CHAR=29;
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
			"FLOAT", "BOOLEAN", "ID", "ERR_CHAR", "C_HORZ_WS", "C_VERT_WS", "CERR_CHAR"
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
	public String getGrammarFileName() { return "TemplateGrammarExpression.g4"; }

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

			        _localctx.ast.addLeafs( () -> getAst(_localctx.ast.bottom.type, null, false, ((ExpressionContext)_localctx).defaultValue != null ? ((ExpressionContext)_localctx).defaultValue.v : null) );

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
			setState(63);
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
			setState(66);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==MINUS) {
				{
				setState(65);
				match(MINUS);
				}
			}

			setState(68);
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
			setState(70);
			match(SEMI);
			setState(71);
			((FunctionContext)_localctx).ID = match(ID);
			setState(72);
			match(LPAREN);
			setState(74);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << DSTRING) | (1L << SSTRING) | (1L << DECDIGITS))) != 0)) {
				{
				setState(73);
				((FunctionContext)_localctx).functionArgs = functionArgs();
				}
			}

			setState(76);
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
			setState(79);
			((FunctionArgsContext)_localctx).functionArg = functionArg();
			 _localctx.ret.add( ((FunctionArgsContext)_localctx).functionArg.ret ); 
			setState(87);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(81);
				match(COMMA);
				setState(82);
				((FunctionArgsContext)_localctx).functionArg = functionArg();
				 _localctx.ret.add( ((FunctionArgsContext)_localctx).functionArg.ret ); 
				}
				}
				setState(89);
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
		public TerminalNode DECDIGITS() { return getToken(TemplateGrammarExpression.DECDIGITS, 0); }
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
			setState(96);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case DECDIGITS:
				enterOuterAlt(_localctx, 1);
				{
				setState(90);
				((FunctionArgContext)_localctx).DECDIGITS = match(DECDIGITS);
				 ((FunctionArgContext)_localctx).ret =  (((FunctionArgContext)_localctx).DECDIGITS!=null?((FunctionArgContext)_localctx).DECDIGITS.getText():null); 
				}
				break;
			case SSTRING:
				enterOuterAlt(_localctx, 2);
				{
				setState(92);
				((FunctionArgContext)_localctx).SSTRING = match(SSTRING);
				 ((FunctionArgContext)_localctx).ret =  sStringToDString( (((FunctionArgContext)_localctx).SSTRING!=null?((FunctionArgContext)_localctx).SSTRING.getText():null) ); 
				}
				break;
			case DSTRING:
				enterOuterAlt(_localctx, 3);
				{
				setState(94);
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
			setState(113);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case PIPE:
				enterOuterAlt(_localctx, 1);
				{
				{
				setState(98);
				match(PIPE);
				setState(99);
				((OrExpsContext)_localctx).exps = exps(parentType);
				 _localctx.list.add(firstAst); _localctx.list.add(((OrExpsContext)_localctx).exps.ast); 
				setState(107);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==PIPE) {
					{
					{
					setState(101);
					match(PIPE);
					setState(102);
					((OrExpsContext)_localctx).exps = exps(parentType);
					_localctx.list.add(((OrExpsContext)_localctx).exps.ast);
					}
					}
					setState(109);
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
			setState(141);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case ID:
				enterOuterAlt(_localctx, 1);
				{
				{
				setState(115);
				((ExpsContext)_localctx).exp = exp(parentType);
				 ((ExpsContext)_localctx).ast =  ((ExpsContext)_localctx).exp.ast; 
				setState(123);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,10,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(117);
						match(DOT);
						setState(118);
						((ExpsContext)_localctx).exp = exp(_localctx.ast.bottom.type);
						_localctx.ast.addToBottomChildrenAndSet(((ExpsContext)_localctx).exp.ast);
						}
						} 
					}
					setState(125);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,10,_ctx);
				}
				}
				setState(127);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==DOT) {
					{
					setState(126);
					match(DOT);
					}
				}

				setState(130);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==LBRACE) {
					{
					setState(129);
					((ExpsContext)_localctx).concatenation = concatenation(_localctx.ast.bottom.type);
					}
				}

				 if( ((ExpsContext)_localctx).concatenation != null ) _localctx.ast.addToBottomChildrenAndSet( ((ExpsContext)_localctx).concatenation.ast ); 
				setState(134);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << STAR) | (1L << SLASH) | (1L << PERCENT) | (1L << PLUS) | (1L << MINUS))) != 0)) {
					{
					setState(133);
					((ExpsContext)_localctx).math = math(_localctx.ast.bottom.type);
					}
				}

				 if( ((ExpsContext)_localctx).math != null ) _localctx.ast.addToBottomChildrenAndSet( ((ExpsContext)_localctx).math.ast ); 
				}
				break;
			case LBRACE:
				enterOuterAlt(_localctx, 2);
				{
				setState(138);
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
		public TerminalNode ID() { return getToken(TemplateGrammarExpression.ID, 0); }
		public TerminalNode LPAREN() { return getToken(TemplateGrammarExpression.LPAREN, 0); }
		public TerminalNode RPAREN() { return getToken(TemplateGrammarExpression.RPAREN, 0); }
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
		try {
			setState(150);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,15,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				{
				setState(143);
				((ExpContext)_localctx).ID = match(ID);
				setState(144);
				match(LPAREN);
				setState(145);
				match(RPAREN);
				}
				 ((ExpContext)_localctx).ast =  getAst(_localctx.parentType, (((ExpContext)_localctx).ID!=null?((ExpContext)_localctx).ID.getText():null), true); 
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(148);
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
		public TerminalNode LBRACE() { return getToken(TemplateGrammarExpression.LBRACE, 0); }
		public CitemsContext citems() {
			return getRuleContext(CitemsContext.class,0);
		}
		public TerminalNode RBRACE() { return getToken(TemplateGrammarExpression.RBRACE, 0); }
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
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(152);
			match(LBRACE);
			setState(153);
			((ConcatenationContext)_localctx).citems = citems(parentType);
			 ((ConcatenationContext)_localctx).ast =  new AstConcatenation(parentType, ((ConcatenationContext)_localctx).citems.list); 
			setState(155);
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
			setState(157);
			((CitemsContext)_localctx).citem = citem(parentType);
			 _localctx.list.add(((CitemsContext)_localctx).citem.ast.top); ((CitemsContext)_localctx).citem.ast.addToBottomChildrenAndSet(getAst(((CitemsContext)_localctx).citem.ast.bottom.type, null, false)); 
			setState(165);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(159);
				match(COMMA);
				setState(160);
				((CitemsContext)_localctx).citem = citem(parentType);
				 _localctx.list.add(((CitemsContext)_localctx).citem.ast.top); ((CitemsContext)_localctx).citem.ast.addToBottomChildrenAndSet(getAst(((CitemsContext)_localctx).citem.ast.bottom.type, null, false)); 
				}
				}
				setState(167);
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
			setState(178);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case ID:
				enterOuterAlt(_localctx, 1);
				{
				setState(168);
				((CitemContext)_localctx).ID = match(ID);
				 ((CitemContext)_localctx).ast =  getAst(_localctx.parentType, (((CitemContext)_localctx).ID!=null?((CitemContext)_localctx).ID.getText():null), false); 
				}
				break;
			case DSTRING:
				enterOuterAlt(_localctx, 2);
				{
				setState(170);
				((CitemContext)_localctx).DSTRING = match(DSTRING);
				 ((CitemContext)_localctx).ast =  new MaxMin(new AstText(sdStringToString((((CitemContext)_localctx).DSTRING!=null?((CitemContext)_localctx).DSTRING.getText():null)))); 
				}
				break;
			case SSTRING:
				enterOuterAlt(_localctx, 3);
				{
				setState(172);
				((CitemContext)_localctx).SSTRING = match(SSTRING);
				 ((CitemContext)_localctx).ast =  new MaxMin(new AstText(sdStringToString((((CitemContext)_localctx).SSTRING!=null?((CitemContext)_localctx).SSTRING.getText():null)))); 
				}
				break;
			case DECDIGITS:
				enterOuterAlt(_localctx, 4);
				{
				setState(174);
				((CitemContext)_localctx).DECDIGITS = match(DECDIGITS);
				 ((CitemContext)_localctx).ast =  new MaxMin(new AstText(String.valueOf((((CitemContext)_localctx).DECDIGITS!=null?((CitemContext)_localctx).DECDIGITS.getText():null)))); 
				}
				break;
			case FLOAT:
				enterOuterAlt(_localctx, 5);
				{
				setState(176);
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
			setState(180);
			((MathContext)_localctx).mathOperation = mathOperation();
			setState(181);
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
			setState(187);
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
				setState(185);
				match(DECDIGITS);
				}
				break;
			case FLOAT:
				enterOuterAlt(_localctx, 3);
				{
				setState(186);
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
			setState(189);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << STAR) | (1L << SLASH) | (1L << PERCENT) | (1L << PLUS) | (1L << MINUS))) != 0)) ) {
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
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\3\37\u00c2\4\2\t\2"+
		"\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13"+
		"\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\3\2\3\2"+
		"\5\2%\n\2\3\2\3\2\3\2\3\2\3\2\5\2,\n\2\3\2\5\2/\n\2\3\2\3\2\3\3\3\3\3"+
		"\3\3\3\3\4\3\4\3\4\3\4\3\4\3\4\3\4\3\4\3\4\3\4\3\4\5\4B\n\4\3\5\5\5E\n"+
		"\5\3\5\3\5\3\6\3\6\3\6\3\6\5\6M\n\6\3\6\3\6\3\6\3\7\3\7\3\7\3\7\3\7\3"+
		"\7\7\7X\n\7\f\7\16\7[\13\7\3\b\3\b\3\b\3\b\3\b\3\b\5\bc\n\b\3\t\3\t\3"+
		"\t\3\t\3\t\3\t\3\t\7\tl\n\t\f\t\16\to\13\t\3\t\3\t\3\t\5\tt\n\t\3\n\3"+
		"\n\3\n\3\n\3\n\3\n\7\n|\n\n\f\n\16\n\177\13\n\3\n\5\n\u0082\n\n\3\n\5"+
		"\n\u0085\n\n\3\n\3\n\5\n\u0089\n\n\3\n\3\n\3\n\3\n\3\n\5\n\u0090\n\n\3"+
		"\13\3\13\3\13\3\13\3\13\3\13\3\13\5\13\u0099\n\13\3\f\3\f\3\f\3\f\3\f"+
		"\3\r\3\r\3\r\3\r\3\r\3\r\7\r\u00a6\n\r\f\r\16\r\u00a9\13\r\3\16\3\16\3"+
		"\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\5\16\u00b5\n\16\3\17\3\17\3\17"+
		"\3\17\3\20\3\20\3\20\5\20\u00be\n\20\3\21\3\21\3\21\2\2\22\2\4\6\b\n\f"+
		"\16\20\22\24\26\30\32\34\36 \2\3\3\2\21\25\2\u00cc\2$\3\2\2\2\4\62\3\2"+
		"\2\2\6A\3\2\2\2\bD\3\2\2\2\nH\3\2\2\2\fQ\3\2\2\2\16b\3\2\2\2\20s\3\2\2"+
		"\2\22\u008f\3\2\2\2\24\u0098\3\2\2\2\26\u009a\3\2\2\2\30\u009f\3\2\2\2"+
		"\32\u00b4\3\2\2\2\34\u00b6\3\2\2\2\36\u00bd\3\2\2\2 \u00bf\3\2\2\2\"#"+
		"\7\3\2\2#%\b\2\1\2$\"\3\2\2\2$%\3\2\2\2%&\3\2\2\2&\'\5\22\n\2\'(\b\2\1"+
		"\2()\5\20\t\2)+\b\2\1\2*,\5\4\3\2+*\3\2\2\2+,\3\2\2\2,.\3\2\2\2-/\5\n"+
		"\6\2.-\3\2\2\2./\3\2\2\2/\60\3\2\2\2\60\61\b\2\1\2\61\3\3\2\2\2\62\63"+
		"\7\16\2\2\63\64\5\6\4\2\64\65\b\3\1\2\65\5\3\2\2\2\66\67\7\27\2\2\67B"+
		"\b\4\1\289\7\26\2\29B\b\4\1\2:;\5\b\5\2;<\b\4\1\2<B\3\2\2\2=>\7\31\2\2"+
		">B\b\4\1\2?@\7\32\2\2@B\b\4\1\2A\66\3\2\2\2A8\3\2\2\2A:\3\2\2\2A=\3\2"+
		"\2\2A?\3\2\2\2B\7\3\2\2\2CE\7\25\2\2DC\3\2\2\2DE\3\2\2\2EF\3\2\2\2FG\7"+
		"\30\2\2G\t\3\2\2\2HI\7\17\2\2IJ\7\33\2\2JL\7\n\2\2KM\5\f\7\2LK\3\2\2\2"+
		"LM\3\2\2\2MN\3\2\2\2NO\7\13\2\2OP\b\6\1\2P\13\3\2\2\2QR\5\16\b\2RY\b\7"+
		"\1\2ST\7\20\2\2TU\5\16\b\2UV\b\7\1\2VX\3\2\2\2WS\3\2\2\2X[\3\2\2\2YW\3"+
		"\2\2\2YZ\3\2\2\2Z\r\3\2\2\2[Y\3\2\2\2\\]\7\30\2\2]c\b\b\1\2^_\7\27\2\2"+
		"_c\b\b\1\2`a\7\26\2\2ac\b\b\1\2b\\\3\2\2\2b^\3\2\2\2b`\3\2\2\2c\17\3\2"+
		"\2\2de\7\b\2\2ef\5\22\n\2fm\b\t\1\2gh\7\b\2\2hi\5\22\n\2ij\b\t\1\2jl\3"+
		"\2\2\2kg\3\2\2\2lo\3\2\2\2mk\3\2\2\2mn\3\2\2\2np\3\2\2\2om\3\2\2\2pq\b"+
		"\t\1\2qt\3\2\2\2rt\b\t\1\2sd\3\2\2\2sr\3\2\2\2t\21\3\2\2\2uv\5\24\13\2"+
		"v}\b\n\1\2wx\7\t\2\2xy\5\24\13\2yz\b\n\1\2z|\3\2\2\2{w\3\2\2\2|\177\3"+
		"\2\2\2}{\3\2\2\2}~\3\2\2\2~\u0081\3\2\2\2\177}\3\2\2\2\u0080\u0082\7\t"+
		"\2\2\u0081\u0080\3\2\2\2\u0081\u0082\3\2\2\2\u0082\u0084\3\2\2\2\u0083"+
		"\u0085\5\26\f\2\u0084\u0083\3\2\2\2\u0084\u0085\3\2\2\2\u0085\u0086\3"+
		"\2\2\2\u0086\u0088\b\n\1\2\u0087\u0089\5\34\17\2\u0088\u0087\3\2\2\2\u0088"+
		"\u0089\3\2\2\2\u0089\u008a\3\2\2\2\u008a\u008b\b\n\1\2\u008b\u0090\3\2"+
		"\2\2\u008c\u008d\5\26\f\2\u008d\u008e\b\n\1\2\u008e\u0090\3\2\2\2\u008f"+
		"u\3\2\2\2\u008f\u008c\3\2\2\2\u0090\23\3\2\2\2\u0091\u0092\7\33\2\2\u0092"+
		"\u0093\7\n\2\2\u0093\u0094\7\13\2\2\u0094\u0095\3\2\2\2\u0095\u0099\b"+
		"\13\1\2\u0096\u0097\7\33\2\2\u0097\u0099\b\13\1\2\u0098\u0091\3\2\2\2"+
		"\u0098\u0096\3\2\2\2\u0099\25\3\2\2\2\u009a\u009b\7\6\2\2\u009b\u009c"+
		"\5\30\r\2\u009c\u009d\b\f\1\2\u009d\u009e\7\7\2\2\u009e\27\3\2\2\2\u009f"+
		"\u00a0\5\32\16\2\u00a0\u00a7\b\r\1\2\u00a1\u00a2\7\20\2\2\u00a2\u00a3"+
		"\5\32\16\2\u00a3\u00a4\b\r\1\2\u00a4\u00a6\3\2\2\2\u00a5\u00a1\3\2\2\2"+
		"\u00a6\u00a9\3\2\2\2\u00a7\u00a5\3\2\2\2\u00a7\u00a8\3\2\2\2\u00a8\31"+
		"\3\2\2\2\u00a9\u00a7\3\2\2\2\u00aa\u00ab\7\33\2\2\u00ab\u00b5\b\16\1\2"+
		"\u00ac\u00ad\7\26\2\2\u00ad\u00b5\b\16\1\2\u00ae\u00af\7\27\2\2\u00af"+
		"\u00b5\b\16\1\2\u00b0\u00b1\7\30\2\2\u00b1\u00b5\b\16\1\2\u00b2\u00b3"+
		"\7\31\2\2\u00b3\u00b5\b\16\1\2\u00b4\u00aa\3\2\2\2\u00b4\u00ac\3\2\2\2"+
		"\u00b4\u00ae\3\2\2\2\u00b4\u00b0\3\2\2\2\u00b4\u00b2\3\2\2\2\u00b5\33"+
		"\3\2\2\2\u00b6\u00b7\5 \21\2\u00b7\u00b8\5\36\20\2\u00b8\u00b9\b\17\1"+
		"\2\u00b9\35\3\2\2\2\u00ba\u00be\3\2\2\2\u00bb\u00be\7\30\2\2\u00bc\u00be"+
		"\7\31\2\2\u00bd\u00ba\3\2\2\2\u00bd\u00bb\3\2\2\2\u00bd\u00bc\3\2\2\2"+
		"\u00be\37\3\2\2\2\u00bf\u00c0\t\2\2\2\u00c0!\3\2\2\2\25$+.ADLYbms}\u0081"+
		"\u0084\u0088\u008f\u0098\u00a7\u00b4\u00bd";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}