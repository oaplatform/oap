// Generated from TemplateGrammarExpression.g4 by ANTLR 4.13.0

package oap.template;

import oap.template.tree.*;
import oap.template.tree.Math;
import oap.template.tree.WithCondition;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import oap.util.Lists;


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
	static { RuntimeMetaData.checkVersion("4.13.0", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		DEFAULT=1, IF=2, THEN=3, ELSE=4, END=5, WITH=6, VAR_ID=7, ROOT=8, BLOCK_COMMENT=9, 
		HORZ_WS=10, VERT_WS=11, LBRACE=12, RBRACE=13, DOT=14, LPAREN=15, RPAREN=16, 
		LBRACK=17, RBRACK=18, DQUESTION=19, SEMI=20, COMMA=21, STAR=22, SLASH=23, 
		PERCENT=24, PLUS=25, MINUS=26, DSTRING=27, SSTRING=28, DECDIGITS=29, FLOAT=30, 
		BOOLEAN=31, ID=32, CAST_TYPE=33, ERR_CHAR=34, C_HORZ_WS=35, C_VERT_WS=36, 
		CERR_CHAR=37;
	public static final int
		RULE_expression = 0, RULE_ifCode = 1, RULE_withCode = 2, RULE_exprsCode = 3, 
		RULE_ifCondition = 4, RULE_defaultValue = 5, RULE_defaultValueType = 6, 
		RULE_longRule = 7, RULE_function = 8, RULE_functionArgs = 9, RULE_functionArg = 10, 
		RULE_orExprs = 11, RULE_exprs = 12, RULE_expr = 13, RULE_concatenation = 14, 
		RULE_citems = 15, RULE_citem = 16, RULE_math = 17, RULE_number = 18, RULE_mathOperation = 19;
	private static String[] makeRuleNames() {
		return new String[] {
			"expression", "ifCode", "withCode", "exprsCode", "ifCondition", "defaultValue", 
			"defaultValueType", "longRule", "function", "functionArgs", "functionArg", 
			"orExprs", "exprs", "expr", "concatenation", "citems", "citem", "math", 
			"number", "mathOperation"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, null, "'if'", "'then'", "'else'", "'end'", "'with'", null, "'$'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "DEFAULT", "IF", "THEN", "ELSE", "END", "WITH", "VAR_ID", "ROOT", 
			"BLOCK_COMMENT", "HORZ_WS", "VERT_WS", "LBRACE", "RBRACE", "DOT", "LPAREN", 
			"RPAREN", "LBRACK", "RBRACK", "DQUESTION", "SEMI", "COMMA", "STAR", "SLASH", 
			"PERCENT", "PLUS", "MINUS", "DSTRING", "SSTRING", "DECDIGITS", "FLOAT", 
			"BOOLEAN", "ID", "CAST_TYPE", "ERR_CHAR", "C_HORZ_WS", "C_VERT_WS", "CERR_CHAR"
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

	@SuppressWarnings("CheckReturnValue")
	public static class ExpressionContext extends ParserRuleContext {
		public Expression ret;
		public Token BLOCK_COMMENT;
		public Token CAST_TYPE;
		public IfCodeContext ifCode;
		public WithCodeContext withCode;
		public ExprsCodeContext exprsCode;
		public DefaultValueContext defaultValue;
		public FunctionContext function;
		public IfCodeContext ifCode() {
			return getRuleContext(IfCodeContext.class,0);
		}
		public WithCodeContext withCode() {
			return getRuleContext(WithCodeContext.class,0);
		}
		public ExprsCodeContext exprsCode() {
			return getRuleContext(ExprsCodeContext.class,0);
		}
		public TerminalNode BLOCK_COMMENT() { return getToken(TemplateGrammarExpression.BLOCK_COMMENT, 0); }
		public TerminalNode CAST_TYPE() { return getToken(TemplateGrammarExpression.CAST_TYPE, 0); }
		public DefaultValueContext defaultValue() {
			return getRuleContext(DefaultValueContext.class,0);
		}
		public FunctionContext function() {
			return getRuleContext(FunctionContext.class,0);
		}
		public TerminalNode IF() { return getToken(TemplateGrammarExpression.IF, 0); }
		public IfConditionContext ifCondition() {
			return getRuleContext(IfConditionContext.class,0);
		}
		public ExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
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

	public final ExpressionContext expression() throws RecognitionException {
		ExpressionContext _localctx = new ExpressionContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_expression);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(41);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==BLOCK_COMMENT) {
				{
				setState(40);
				((ExpressionContext)_localctx).BLOCK_COMMENT = match(BLOCK_COMMENT);
				}
			}

			setState(44);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==CAST_TYPE) {
				{
				setState(43);
				((ExpressionContext)_localctx).CAST_TYPE = match(CAST_TYPE);
				}
			}

			setState(49);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case IF:
				{
				setState(46);
				((ExpressionContext)_localctx).ifCode = ifCode();
				}
				break;
			case WITH:
				{
				setState(47);
				((ExpressionContext)_localctx).withCode = withCode();
				}
				break;
			case VAR_ID:
			case ROOT:
			case LBRACE:
			case ID:
				{
				setState(48);
				((ExpressionContext)_localctx).exprsCode = exprsCode();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(52);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==DQUESTION) {
				{
				setState(51);
				((ExpressionContext)_localctx).defaultValue = defaultValue();
				}
			}

			setState(55);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==SEMI) {
				{
				setState(54);
				((ExpressionContext)_localctx).function = function();
				}
			}

			setState(59);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==IF) {
				{
				setState(57);
				match(IF);
				setState(58);
				ifCondition();
				}
			}


			        ((ExpressionContext)_localctx).ret =  new Expression(
			          (((ExpressionContext)_localctx).BLOCK_COMMENT!=null?((ExpressionContext)_localctx).BLOCK_COMMENT.getText():null),
			          (((ExpressionContext)_localctx).CAST_TYPE!=null?((ExpressionContext)_localctx).CAST_TYPE.getText():null) != null ? (((ExpressionContext)_localctx).CAST_TYPE!=null?((ExpressionContext)_localctx).CAST_TYPE.getText():null).substring( 1, (((ExpressionContext)_localctx).CAST_TYPE!=null?((ExpressionContext)_localctx).CAST_TYPE.getText():null).length() - 1 ) : null,
			          ((ExpressionContext)_localctx).ifCode != null ? ((ExpressionContext)_localctx).ifCode.ret : null,
			          ((ExpressionContext)_localctx).withCode != null ? ((ExpressionContext)_localctx).withCode.ret : null,
			          ((ExpressionContext)_localctx).exprsCode != null ? ((ExpressionContext)_localctx).exprsCode.ret : null,
			          ((ExpressionContext)_localctx).defaultValue != null ? ((ExpressionContext)_localctx).defaultValue.ret : null,
			          ((ExpressionContext)_localctx).function != null ? ((ExpressionContext)_localctx).function.ret : null );
			      
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
	public static class IfCodeContext extends ParserRuleContext {
		public IfCondition ret;
		public IfConditionContext ifCondition;
		public ExprsContext thenCode;
		public ExprsContext elseCode;
		public TerminalNode IF() { return getToken(TemplateGrammarExpression.IF, 0); }
		public IfConditionContext ifCondition() {
			return getRuleContext(IfConditionContext.class,0);
		}
		public TerminalNode THEN() { return getToken(TemplateGrammarExpression.THEN, 0); }
		public TerminalNode END() { return getToken(TemplateGrammarExpression.END, 0); }
		public List<ExprsContext> exprs() {
			return getRuleContexts(ExprsContext.class);
		}
		public ExprsContext exprs(int i) {
			return getRuleContext(ExprsContext.class,i);
		}
		public TerminalNode ELSE() { return getToken(TemplateGrammarExpression.ELSE, 0); }
		public IfCodeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_ifCode; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarExpressionListener ) ((TemplateGrammarExpressionListener)listener).enterIfCode(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarExpressionListener ) ((TemplateGrammarExpressionListener)listener).exitIfCode(this);
		}
	}

	public final IfCodeContext ifCode() throws RecognitionException {
		IfCodeContext _localctx = new IfCodeContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_ifCode);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(63);
			match(IF);
			setState(64);
			((IfCodeContext)_localctx).ifCondition = ifCondition();
			setState(65);
			match(THEN);
			setState(66);
			((IfCodeContext)_localctx).thenCode = exprs();
			setState(69);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==ELSE) {
				{
				setState(67);
				match(ELSE);
				setState(68);
				((IfCodeContext)_localctx).elseCode = exprs();
				}
			}

			setState(71);
			match(END);

			        ((IfCodeContext)_localctx).ret =  new IfCondition( ((IfCodeContext)_localctx).ifCondition.ret, ((IfCodeContext)_localctx).thenCode.ret, ((IfCodeContext)_localctx).elseCode != null ? ((IfCodeContext)_localctx).elseCode.ret : null );
			      
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
	public static class WithCodeContext extends ParserRuleContext {
		public WithCondition ret;
		public ExprsContext scopePath;
		public ExprsCodeContext bodyExprs;
		public TerminalNode WITH() { return getToken(TemplateGrammarExpression.WITH, 0); }
		public TerminalNode LPAREN() { return getToken(TemplateGrammarExpression.LPAREN, 0); }
		public TerminalNode RPAREN() { return getToken(TemplateGrammarExpression.RPAREN, 0); }
		public TerminalNode END() { return getToken(TemplateGrammarExpression.END, 0); }
		public ExprsContext exprs() {
			return getRuleContext(ExprsContext.class,0);
		}
		public ExprsCodeContext exprsCode() {
			return getRuleContext(ExprsCodeContext.class,0);
		}
		public WithCodeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_withCode; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarExpressionListener ) ((TemplateGrammarExpressionListener)listener).enterWithCode(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarExpressionListener ) ((TemplateGrammarExpressionListener)listener).exitWithCode(this);
		}
	}

	public final WithCodeContext withCode() throws RecognitionException {
		WithCodeContext _localctx = new WithCodeContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_withCode);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(74);
			match(WITH);
			setState(75);
			match(LPAREN);
			setState(76);
			((WithCodeContext)_localctx).scopePath = exprs();
			setState(77);
			match(RPAREN);
			setState(78);
			((WithCodeContext)_localctx).bodyExprs = exprsCode();
			setState(79);
			match(END);

			        ((WithCodeContext)_localctx).ret =  new WithCondition( ((WithCodeContext)_localctx).scopePath.ret, ((WithCodeContext)_localctx).bodyExprs.ret );
			      
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
	public static class ExprsCodeContext extends ParserRuleContext {
		public ArrayList<Exprs> ret = new ArrayList<>();
		public ExprsContext exprs;
		public OrExprsContext orExprs;
		public ExprsContext exprs() {
			return getRuleContext(ExprsContext.class,0);
		}
		public OrExprsContext orExprs() {
			return getRuleContext(OrExprsContext.class,0);
		}
		public ExprsCodeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_exprsCode; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarExpressionListener ) ((TemplateGrammarExpressionListener)listener).enterExprsCode(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarExpressionListener ) ((TemplateGrammarExpressionListener)listener).exitExprsCode(this);
		}
	}

	public final ExprsCodeContext exprsCode() throws RecognitionException {
		ExprsCodeContext _localctx = new ExprsCodeContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_exprsCode);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(82);
			((ExprsCodeContext)_localctx).exprs = exprs();
			setState(83);
			((ExprsCodeContext)_localctx).orExprs = orExprs();

			        _localctx.ret.add( ((ExprsCodeContext)_localctx).exprs.ret );
			        _localctx.ret.addAll( ((ExprsCodeContext)_localctx).orExprs.ret );
			      
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
	public static class IfConditionContext extends ParserRuleContext {
		public Exprs ret;
		public ExprsContext exprs;
		public ExprsContext exprs() {
			return getRuleContext(ExprsContext.class,0);
		}
		public IfConditionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_ifCondition; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarExpressionListener ) ((TemplateGrammarExpressionListener)listener).enterIfCondition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarExpressionListener ) ((TemplateGrammarExpressionListener)listener).exitIfCondition(this);
		}
	}

	public final IfConditionContext ifCondition() throws RecognitionException {
		IfConditionContext _localctx = new IfConditionContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_ifCondition);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(86);
			((IfConditionContext)_localctx).exprs = exprs();
			 ((IfConditionContext)_localctx).ret =  ((IfConditionContext)_localctx).exprs.ret; 
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
		public String ret;
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
		enterRule(_localctx, 10, RULE_defaultValue);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(89);
			match(DQUESTION);
			setState(90);
			((DefaultValueContext)_localctx).defaultValueType = defaultValueType();
			 ((DefaultValueContext)_localctx).ret =  ((DefaultValueContext)_localctx).defaultValueType.ret; 
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
		public String ret;
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
		enterRule(_localctx, 12, RULE_defaultValueType);
		try {
			setState(107);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case SSTRING:
				enterOuterAlt(_localctx, 1);
				{
				setState(93);
				((DefaultValueTypeContext)_localctx).SSTRING = match(SSTRING);
				 ((DefaultValueTypeContext)_localctx).ret =  sdStringToString( (((DefaultValueTypeContext)_localctx).SSTRING!=null?((DefaultValueTypeContext)_localctx).SSTRING.getText():null) ); 
				}
				break;
			case DSTRING:
				enterOuterAlt(_localctx, 2);
				{
				setState(95);
				((DefaultValueTypeContext)_localctx).DSTRING = match(DSTRING);
				 ((DefaultValueTypeContext)_localctx).ret =  sdStringToString((((DefaultValueTypeContext)_localctx).DSTRING!=null?((DefaultValueTypeContext)_localctx).DSTRING.getText():null)); 
				}
				break;
			case MINUS:
			case DECDIGITS:
				enterOuterAlt(_localctx, 3);
				{
				setState(97);
				((DefaultValueTypeContext)_localctx).longRule = longRule();
				 ((DefaultValueTypeContext)_localctx).ret =  (((DefaultValueTypeContext)_localctx).longRule!=null?_input.getText(((DefaultValueTypeContext)_localctx).longRule.start,((DefaultValueTypeContext)_localctx).longRule.stop):null); 
				}
				break;
			case FLOAT:
				enterOuterAlt(_localctx, 4);
				{
				setState(100);
				((DefaultValueTypeContext)_localctx).FLOAT = match(FLOAT);
				 ((DefaultValueTypeContext)_localctx).ret =  (((DefaultValueTypeContext)_localctx).FLOAT!=null?((DefaultValueTypeContext)_localctx).FLOAT.getText():null); 
				}
				break;
			case BOOLEAN:
				enterOuterAlt(_localctx, 5);
				{
				setState(102);
				((DefaultValueTypeContext)_localctx).BOOLEAN = match(BOOLEAN);
				 ((DefaultValueTypeContext)_localctx).ret =  (((DefaultValueTypeContext)_localctx).BOOLEAN!=null?((DefaultValueTypeContext)_localctx).BOOLEAN.getText():null); 
				}
				break;
			case LBRACK:
				enterOuterAlt(_localctx, 6);
				{
				setState(104);
				match(LBRACK);
				setState(105);
				match(RBRACK);
				 ((DefaultValueTypeContext)_localctx).ret =  "[]"; 
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
		enterRule(_localctx, 14, RULE_longRule);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(110);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==MINUS) {
				{
				setState(109);
				match(MINUS);
				}
			}

			setState(112);
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
		public Func ret;
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
		enterRule(_localctx, 16, RULE_function);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(114);
			match(SEMI);
			setState(115);
			((FunctionContext)_localctx).ID = match(ID);
			setState(116);
			match(LPAREN);
			setState(118);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 2080374784L) != 0)) {
				{
				setState(117);
				((FunctionContext)_localctx).functionArgs = functionArgs();
				}
			}

			setState(120);
			match(RPAREN);
			 ((FunctionContext)_localctx).ret =  new Func( (((FunctionContext)_localctx).ID!=null?((FunctionContext)_localctx).ID.getText():null), ((FunctionContext)_localctx).functionArgs != null ? ((FunctionContext)_localctx).functionArgs.ret : List.of() ); 
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
		enterRule(_localctx, 18, RULE_functionArgs);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(123);
			((FunctionArgsContext)_localctx).functionArg = functionArg();
			 _localctx.ret.add( ((FunctionArgsContext)_localctx).functionArg.ret ); 
			setState(131);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(125);
				match(COMMA);
				setState(126);
				((FunctionArgsContext)_localctx).functionArg = functionArg();
				 _localctx.ret.add( ((FunctionArgsContext)_localctx).functionArg.ret ); 
				}
				}
				setState(133);
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
		enterRule(_localctx, 20, RULE_functionArg);
		try {
			setState(148);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,11,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(134);
				((FunctionArgContext)_localctx).DECDIGITS = match(DECDIGITS);
				 ((FunctionArgContext)_localctx).ret =  (((FunctionArgContext)_localctx).DECDIGITS!=null?((FunctionArgContext)_localctx).DECDIGITS.getText():null); 
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(136);
				match(MINUS);
				setState(137);
				((FunctionArgContext)_localctx).DECDIGITS = match(DECDIGITS);
				 ((FunctionArgContext)_localctx).ret =  "-" + (((FunctionArgContext)_localctx).DECDIGITS!=null?((FunctionArgContext)_localctx).DECDIGITS.getText():null); 
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(139);
				((FunctionArgContext)_localctx).FLOAT = match(FLOAT);
				 ((FunctionArgContext)_localctx).ret =  (((FunctionArgContext)_localctx).FLOAT!=null?((FunctionArgContext)_localctx).FLOAT.getText():null); 
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(141);
				match(MINUS);
				setState(142);
				((FunctionArgContext)_localctx).FLOAT = match(FLOAT);
				 ((FunctionArgContext)_localctx).ret =  "-" + (((FunctionArgContext)_localctx).FLOAT!=null?((FunctionArgContext)_localctx).FLOAT.getText():null); 
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(144);
				((FunctionArgContext)_localctx).SSTRING = match(SSTRING);
				 ((FunctionArgContext)_localctx).ret =  sStringToDString( (((FunctionArgContext)_localctx).SSTRING!=null?((FunctionArgContext)_localctx).SSTRING.getText():null) ); 
				}
				break;
			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(146);
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
	public static class OrExprsContext extends ParserRuleContext {
		public ArrayList<Exprs> ret = new ArrayList<Exprs>();
		public ExprsContext exprs;
		public List<TerminalNode> DEFAULT() { return getTokens(TemplateGrammarExpression.DEFAULT); }
		public TerminalNode DEFAULT(int i) {
			return getToken(TemplateGrammarExpression.DEFAULT, i);
		}
		public List<ExprsContext> exprs() {
			return getRuleContexts(ExprsContext.class);
		}
		public ExprsContext exprs(int i) {
			return getRuleContext(ExprsContext.class,i);
		}
		public OrExprsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_orExprs; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarExpressionListener ) ((TemplateGrammarExpressionListener)listener).enterOrExprs(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarExpressionListener ) ((TemplateGrammarExpressionListener)listener).exitOrExprs(this);
		}
	}

	public final OrExprsContext orExprs() throws RecognitionException {
		OrExprsContext _localctx = new OrExprsContext(_ctx, getState());
		enterRule(_localctx, 22, RULE_orExprs);
		int _la;
		try {
			setState(163);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case DEFAULT:
				enterOuterAlt(_localctx, 1);
				{
				{
				setState(150);
				match(DEFAULT);
				setState(151);
				((OrExprsContext)_localctx).exprs = exprs();
				 _localctx.ret.add( ((OrExprsContext)_localctx).exprs.ret ); 
				setState(159);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==DEFAULT) {
					{
					{
					setState(153);
					match(DEFAULT);
					setState(154);
					((OrExprsContext)_localctx).exprs = exprs();
					 _localctx.ret.add( ((OrExprsContext)_localctx).exprs.ret ); 
					}
					}
					setState(161);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
				}
				break;
			case EOF:
			case IF:
			case END:
			case DQUESTION:
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

	@SuppressWarnings("CheckReturnValue")
	public static class ExprsContext extends ParserRuleContext {
		public Exprs ret = new Exprs();
		public ExprContext expr;
		public MathContext math;
		public Token VAR_ID;
		public ConcatenationContext concatenation;
		public TerminalNode ROOT() { return getToken(TemplateGrammarExpression.ROOT, 0); }
		public List<TerminalNode> DOT() { return getTokens(TemplateGrammarExpression.DOT); }
		public TerminalNode DOT(int i) {
			return getToken(TemplateGrammarExpression.DOT, i);
		}
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public MathContext math() {
			return getRuleContext(MathContext.class,0);
		}
		public TerminalNode VAR_ID() { return getToken(TemplateGrammarExpression.VAR_ID, 0); }
		public ConcatenationContext concatenation() {
			return getRuleContext(ConcatenationContext.class,0);
		}
		public ExprsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_exprs; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarExpressionListener ) ((TemplateGrammarExpressionListener)listener).enterExprs(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarExpressionListener ) ((TemplateGrammarExpressionListener)listener).exitExprs(this);
		}
	}

	public final ExprsContext exprs() throws RecognitionException {
		ExprsContext _localctx = new ExprsContext(_ctx, getState());
		enterRule(_localctx, 24, RULE_exprs);
		int _la;
		try {
			int _alt;
			setState(259);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,25,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(165);
				match(ROOT);
				setState(166);
				match(DOT);
				setState(167);
				((ExprsContext)_localctx).expr = expr();
				 _localctx.ret.rootScoped = true; _localctx.ret.exprs.add( ((ExprsContext)_localctx).expr.ret ); 
				setState(175);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==DOT) {
					{
					{
					setState(169);
					match(DOT);
					setState(170);
					((ExprsContext)_localctx).expr = expr();
					 _localctx.ret.exprs.add( ((ExprsContext)_localctx).expr.ret ); 
					}
					}
					setState(177);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(179);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 130023424L) != 0)) {
					{
					setState(178);
					((ExprsContext)_localctx).math = math();
					}
				}

				 if( ((ExprsContext)_localctx).math != null ) _localctx.ret.math = ((ExprsContext)_localctx).math.ret; 
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(183);
				((ExprsContext)_localctx).VAR_ID = match(VAR_ID);
				 _localctx.ret.varName = (((ExprsContext)_localctx).VAR_ID!=null?((ExprsContext)_localctx).VAR_ID.getText():null).substring( 1 ); 
				setState(191);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==DOT) {
					{
					{
					setState(185);
					match(DOT);
					setState(186);
					((ExprsContext)_localctx).expr = expr();
					 _localctx.ret.exprs.add( ((ExprsContext)_localctx).expr.ret ); 
					}
					}
					setState(193);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(195);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 130023424L) != 0)) {
					{
					setState(194);
					((ExprsContext)_localctx).math = math();
					}
				}

				 if( ((ExprsContext)_localctx).math != null ) _localctx.ret.math = ((ExprsContext)_localctx).math.ret; 
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(198);
				((ExprsContext)_localctx).expr = expr();
				 _localctx.ret.exprs.add( ((ExprsContext)_localctx).expr.ret ); 
				setState(201);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 130023424L) != 0)) {
					{
					setState(200);
					((ExprsContext)_localctx).math = math();
					}
				}

				 if( ((ExprsContext)_localctx).math != null ) _localctx.ret.math = ((ExprsContext)_localctx).math.ret; 
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(205);
				((ExprsContext)_localctx).expr = expr();
				 _localctx.ret.exprs.add( ((ExprsContext)_localctx).expr.ret ); 
				setState(208);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==DOT) {
					{
					setState(207);
					match(DOT);
					}
				}

				setState(210);
				((ExprsContext)_localctx).concatenation = concatenation();
				 _localctx.ret.concatenation = ((ExprsContext)_localctx).concatenation.ret; 
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(213);
				((ExprsContext)_localctx).expr = expr();
				 _localctx.ret.exprs.add( ((ExprsContext)_localctx).expr.ret ); 
				setState(221);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,20,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(215);
						match(DOT);
						setState(216);
						((ExprsContext)_localctx).expr = expr();

						        _localctx.ret.exprs.add( ((ExprsContext)_localctx).expr.ret );
						      
						}
						} 
					}
					setState(223);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,20,_ctx);
				}
				setState(224);
				match(DOT);
				setState(225);
				((ExprsContext)_localctx).expr = expr();
				 _localctx.ret.exprs.add( ((ExprsContext)_localctx).expr.ret ); 
				setState(228);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 130023424L) != 0)) {
					{
					setState(227);
					((ExprsContext)_localctx).math = math();
					}
				}

				 if( ((ExprsContext)_localctx).math != null ) _localctx.ret.math = ((ExprsContext)_localctx).math.ret; 
				}
				break;
			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(232);
				((ExprsContext)_localctx).expr = expr();
				 _localctx.ret.exprs.add( ((ExprsContext)_localctx).expr.ret ); 
				setState(240);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,22,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(234);
						match(DOT);
						setState(235);
						((ExprsContext)_localctx).expr = expr();
						 _localctx.ret.exprs.add( ((ExprsContext)_localctx).expr.ret ); 
						}
						} 
					}
					setState(242);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,22,_ctx);
				}
				setState(243);
				match(DOT);
				setState(244);
				((ExprsContext)_localctx).expr = expr();
				 _localctx.ret.exprs.add( ((ExprsContext)_localctx).expr.ret ); 
				setState(247);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==DOT) {
					{
					setState(246);
					match(DOT);
					}
				}

				setState(249);
				((ExprsContext)_localctx).concatenation = concatenation();
				 _localctx.ret.concatenation = ((ExprsContext)_localctx).concatenation.ret; 
				setState(252);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 130023424L) != 0)) {
					{
					setState(251);
					((ExprsContext)_localctx).math = math();
					}
				}

				 if( ((ExprsContext)_localctx).math != null ) _localctx.ret.math = ((ExprsContext)_localctx).math.ret; 
				}
				break;
			case 7:
				enterOuterAlt(_localctx, 7);
				{
				setState(256);
				((ExprsContext)_localctx).concatenation = concatenation();
				 _localctx.ret.concatenation = ((ExprsContext)_localctx).concatenation.ret; 
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
	public static class ExprContext extends ParserRuleContext {
		public Expr ret;
		public Token ID;
		public FunctionArgsContext functionArgs;
		public TerminalNode ID() { return getToken(TemplateGrammarExpression.ID, 0); }
		public TerminalNode LPAREN() { return getToken(TemplateGrammarExpression.LPAREN, 0); }
		public TerminalNode RPAREN() { return getToken(TemplateGrammarExpression.RPAREN, 0); }
		public FunctionArgsContext functionArgs() {
			return getRuleContext(FunctionArgsContext.class,0);
		}
		public ExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_expr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarExpressionListener ) ((TemplateGrammarExpressionListener)listener).enterExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarExpressionListener ) ((TemplateGrammarExpressionListener)listener).exitExpr(this);
		}
	}

	public final ExprContext expr() throws RecognitionException {
		ExprContext _localctx = new ExprContext(_ctx, getState());
		enterRule(_localctx, 26, RULE_expr);
		int _la;
		try {
			setState(271);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,27,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				{
				setState(261);
				((ExprContext)_localctx).ID = match(ID);
				setState(262);
				match(LPAREN);
				setState(264);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 2080374784L) != 0)) {
					{
					setState(263);
					((ExprContext)_localctx).functionArgs = functionArgs();
					}
				}

				setState(266);
				match(RPAREN);
				}
				 ((ExprContext)_localctx).ret =  new Expr((((ExprContext)_localctx).ID!=null?((ExprContext)_localctx).ID.getText():null), true, ((ExprContext)_localctx).functionArgs != null ? ((ExprContext)_localctx).functionArgs.ret : List.of() ); 
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(269);
				((ExprContext)_localctx).ID = match(ID);
				 ((ExprContext)_localctx).ret =  new Expr((((ExprContext)_localctx).ID!=null?((ExprContext)_localctx).ID.getText():null), false, List.of() ); 
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
		public Concatenation ret;
		public CitemsContext citems;
		public TerminalNode LBRACE() { return getToken(TemplateGrammarExpression.LBRACE, 0); }
		public CitemsContext citems() {
			return getRuleContext(CitemsContext.class,0);
		}
		public TerminalNode RBRACE() { return getToken(TemplateGrammarExpression.RBRACE, 0); }
		public ConcatenationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
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

	public final ConcatenationContext concatenation() throws RecognitionException {
		ConcatenationContext _localctx = new ConcatenationContext(_ctx, getState());
		enterRule(_localctx, 28, RULE_concatenation);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(273);
			match(LBRACE);
			setState(274);
			((ConcatenationContext)_localctx).citems = citems();
			 ((ConcatenationContext)_localctx).ret =  new Concatenation( ((ConcatenationContext)_localctx).citems.ret ); 
			setState(276);
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
		public ArrayList<Object> ret = new ArrayList<>();
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
		public CitemsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
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

	public final CitemsContext citems() throws RecognitionException {
		CitemsContext _localctx = new CitemsContext(_ctx, getState());
		enterRule(_localctx, 30, RULE_citems);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(278);
			((CitemsContext)_localctx).citem = citem();
			 _localctx.ret.add(((CitemsContext)_localctx).citem.ret); 
			setState(286);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(280);
				match(COMMA);
				setState(281);
				((CitemsContext)_localctx).citem = citem();
				 _localctx.ret.add(((CitemsContext)_localctx).citem.ret); 
				}
				}
				setState(288);
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
		public Object ret;
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
		public CitemContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
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

	public final CitemContext citem() throws RecognitionException {
		CitemContext _localctx = new CitemContext(_ctx, getState());
		enterRule(_localctx, 32, RULE_citem);
		try {
			setState(299);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case ID:
				enterOuterAlt(_localctx, 1);
				{
				setState(289);
				((CitemContext)_localctx).ID = match(ID);
				 ((CitemContext)_localctx).ret =  new Expr( (((CitemContext)_localctx).ID!=null?((CitemContext)_localctx).ID.getText():null), false, List.of() ); 
				}
				break;
			case DSTRING:
				enterOuterAlt(_localctx, 2);
				{
				setState(291);
				((CitemContext)_localctx).DSTRING = match(DSTRING);
				 ((CitemContext)_localctx).ret =  sdStringToString( (((CitemContext)_localctx).DSTRING!=null?((CitemContext)_localctx).DSTRING.getText():null) ); 
				}
				break;
			case SSTRING:
				enterOuterAlt(_localctx, 3);
				{
				setState(293);
				((CitemContext)_localctx).SSTRING = match(SSTRING);
				 ((CitemContext)_localctx).ret =  sdStringToString( (((CitemContext)_localctx).SSTRING!=null?((CitemContext)_localctx).SSTRING.getText():null) ); 
				}
				break;
			case DECDIGITS:
				enterOuterAlt(_localctx, 4);
				{
				setState(295);
				((CitemContext)_localctx).DECDIGITS = match(DECDIGITS);
				 ((CitemContext)_localctx).ret =  String.valueOf( (((CitemContext)_localctx).DECDIGITS!=null?((CitemContext)_localctx).DECDIGITS.getText():null) ); 
				}
				break;
			case FLOAT:
				enterOuterAlt(_localctx, 5);
				{
				setState(297);
				((CitemContext)_localctx).FLOAT = match(FLOAT);
				 ((CitemContext)_localctx).ret =  String.valueOf( (((CitemContext)_localctx).FLOAT!=null?((CitemContext)_localctx).FLOAT.getText():null) ); 
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
		public Math ret;
		public MathOperationContext mathOperation;
		public NumberContext number;
		public MathOperationContext mathOperation() {
			return getRuleContext(MathOperationContext.class,0);
		}
		public NumberContext number() {
			return getRuleContext(NumberContext.class,0);
		}
		public MathContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
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

	public final MathContext math() throws RecognitionException {
		MathContext _localctx = new MathContext(_ctx, getState());
		enterRule(_localctx, 34, RULE_math);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(301);
			((MathContext)_localctx).mathOperation = mathOperation();
			setState(302);
			((MathContext)_localctx).number = number();
			 ((MathContext)_localctx).ret =  new Math( (((MathContext)_localctx).mathOperation!=null?_input.getText(((MathContext)_localctx).mathOperation.start,((MathContext)_localctx).mathOperation.stop):null), (((MathContext)_localctx).number!=null?_input.getText(((MathContext)_localctx).number.start,((MathContext)_localctx).number.stop):null) ); 
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
		enterRule(_localctx, 36, RULE_number);
		try {
			setState(308);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case EOF:
			case DEFAULT:
			case IF:
			case THEN:
			case ELSE:
			case END:
			case RPAREN:
			case DQUESTION:
			case SEMI:
				enterOuterAlt(_localctx, 1);
				{
				}
				break;
			case DECDIGITS:
				enterOuterAlt(_localctx, 2);
				{
				setState(306);
				match(DECDIGITS);
				}
				break;
			case FLOAT:
				enterOuterAlt(_localctx, 3);
				{
				setState(307);
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
		enterRule(_localctx, 38, RULE_mathOperation);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(310);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 130023424L) != 0)) ) {
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
		"\u0004\u0001%\u0139\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001\u0002"+
		"\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004\u0007\u0004\u0002"+
		"\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002\u0007\u0007\u0007\u0002"+
		"\b\u0007\b\u0002\t\u0007\t\u0002\n\u0007\n\u0002\u000b\u0007\u000b\u0002"+
		"\f\u0007\f\u0002\r\u0007\r\u0002\u000e\u0007\u000e\u0002\u000f\u0007\u000f"+
		"\u0002\u0010\u0007\u0010\u0002\u0011\u0007\u0011\u0002\u0012\u0007\u0012"+
		"\u0002\u0013\u0007\u0013\u0001\u0000\u0003\u0000*\b\u0000\u0001\u0000"+
		"\u0003\u0000-\b\u0000\u0001\u0000\u0001\u0000\u0001\u0000\u0003\u0000"+
		"2\b\u0000\u0001\u0000\u0003\u00005\b\u0000\u0001\u0000\u0003\u00008\b"+
		"\u0000\u0001\u0000\u0001\u0000\u0003\u0000<\b\u0000\u0001\u0000\u0001"+
		"\u0000\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001"+
		"\u0001\u0003\u0001F\b\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001"+
		"\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001"+
		"\u0002\u0001\u0002\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001"+
		"\u0004\u0001\u0004\u0001\u0004\u0001\u0005\u0001\u0005\u0001\u0005\u0001"+
		"\u0005\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0001"+
		"\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0001"+
		"\u0006\u0001\u0006\u0001\u0006\u0003\u0006l\b\u0006\u0001\u0007\u0003"+
		"\u0007o\b\u0007\u0001\u0007\u0001\u0007\u0001\b\u0001\b\u0001\b\u0001"+
		"\b\u0003\bw\b\b\u0001\b\u0001\b\u0001\b\u0001\t\u0001\t\u0001\t\u0001"+
		"\t\u0001\t\u0001\t\u0005\t\u0082\b\t\n\t\f\t\u0085\t\t\u0001\n\u0001\n"+
		"\u0001\n\u0001\n\u0001\n\u0001\n\u0001\n\u0001\n\u0001\n\u0001\n\u0001"+
		"\n\u0001\n\u0001\n\u0001\n\u0003\n\u0095\b\n\u0001\u000b\u0001\u000b\u0001"+
		"\u000b\u0001\u000b\u0001\u000b\u0001\u000b\u0001\u000b\u0005\u000b\u009e"+
		"\b\u000b\n\u000b\f\u000b\u00a1\t\u000b\u0001\u000b\u0003\u000b\u00a4\b"+
		"\u000b\u0001\f\u0001\f\u0001\f\u0001\f\u0001\f\u0001\f\u0001\f\u0001\f"+
		"\u0005\f\u00ae\b\f\n\f\f\f\u00b1\t\f\u0001\f\u0003\f\u00b4\b\f\u0001\f"+
		"\u0001\f\u0001\f\u0001\f\u0001\f\u0001\f\u0001\f\u0001\f\u0005\f\u00be"+
		"\b\f\n\f\f\f\u00c1\t\f\u0001\f\u0003\f\u00c4\b\f\u0001\f\u0001\f\u0001"+
		"\f\u0001\f\u0003\f\u00ca\b\f\u0001\f\u0001\f\u0001\f\u0001\f\u0001\f\u0003"+
		"\f\u00d1\b\f\u0001\f\u0001\f\u0001\f\u0001\f\u0001\f\u0001\f\u0001\f\u0001"+
		"\f\u0001\f\u0005\f\u00dc\b\f\n\f\f\f\u00df\t\f\u0001\f\u0001\f\u0001\f"+
		"\u0001\f\u0003\f\u00e5\b\f\u0001\f\u0001\f\u0001\f\u0001\f\u0001\f\u0001"+
		"\f\u0001\f\u0001\f\u0005\f\u00ef\b\f\n\f\f\f\u00f2\t\f\u0001\f\u0001\f"+
		"\u0001\f\u0001\f\u0003\f\u00f8\b\f\u0001\f\u0001\f\u0001\f\u0003\f\u00fd"+
		"\b\f\u0001\f\u0001\f\u0001\f\u0001\f\u0001\f\u0003\f\u0104\b\f\u0001\r"+
		"\u0001\r\u0001\r\u0003\r\u0109\b\r\u0001\r\u0001\r\u0001\r\u0001\r\u0001"+
		"\r\u0003\r\u0110\b\r\u0001\u000e\u0001\u000e\u0001\u000e\u0001\u000e\u0001"+
		"\u000e\u0001\u000f\u0001\u000f\u0001\u000f\u0001\u000f\u0001\u000f\u0001"+
		"\u000f\u0005\u000f\u011d\b\u000f\n\u000f\f\u000f\u0120\t\u000f\u0001\u0010"+
		"\u0001\u0010\u0001\u0010\u0001\u0010\u0001\u0010\u0001\u0010\u0001\u0010"+
		"\u0001\u0010\u0001\u0010\u0001\u0010\u0003\u0010\u012c\b\u0010\u0001\u0011"+
		"\u0001\u0011\u0001\u0011\u0001\u0011\u0001\u0012\u0001\u0012\u0001\u0012"+
		"\u0003\u0012\u0135\b\u0012\u0001\u0013\u0001\u0013\u0001\u0013\u0000\u0000"+
		"\u0014\u0000\u0002\u0004\u0006\b\n\f\u000e\u0010\u0012\u0014\u0016\u0018"+
		"\u001a\u001c\u001e \"$&\u0000\u0001\u0001\u0000\u0016\u001a\u0155\u0000"+
		")\u0001\u0000\u0000\u0000\u0002?\u0001\u0000\u0000\u0000\u0004J\u0001"+
		"\u0000\u0000\u0000\u0006R\u0001\u0000\u0000\u0000\bV\u0001\u0000\u0000"+
		"\u0000\nY\u0001\u0000\u0000\u0000\fk\u0001\u0000\u0000\u0000\u000en\u0001"+
		"\u0000\u0000\u0000\u0010r\u0001\u0000\u0000\u0000\u0012{\u0001\u0000\u0000"+
		"\u0000\u0014\u0094\u0001\u0000\u0000\u0000\u0016\u00a3\u0001\u0000\u0000"+
		"\u0000\u0018\u0103\u0001\u0000\u0000\u0000\u001a\u010f\u0001\u0000\u0000"+
		"\u0000\u001c\u0111\u0001\u0000\u0000\u0000\u001e\u0116\u0001\u0000\u0000"+
		"\u0000 \u012b\u0001\u0000\u0000\u0000\"\u012d\u0001\u0000\u0000\u0000"+
		"$\u0134\u0001\u0000\u0000\u0000&\u0136\u0001\u0000\u0000\u0000(*\u0005"+
		"\t\u0000\u0000)(\u0001\u0000\u0000\u0000)*\u0001\u0000\u0000\u0000*,\u0001"+
		"\u0000\u0000\u0000+-\u0005!\u0000\u0000,+\u0001\u0000\u0000\u0000,-\u0001"+
		"\u0000\u0000\u0000-1\u0001\u0000\u0000\u0000.2\u0003\u0002\u0001\u0000"+
		"/2\u0003\u0004\u0002\u000002\u0003\u0006\u0003\u00001.\u0001\u0000\u0000"+
		"\u00001/\u0001\u0000\u0000\u000010\u0001\u0000\u0000\u000024\u0001\u0000"+
		"\u0000\u000035\u0003\n\u0005\u000043\u0001\u0000\u0000\u000045\u0001\u0000"+
		"\u0000\u000057\u0001\u0000\u0000\u000068\u0003\u0010\b\u000076\u0001\u0000"+
		"\u0000\u000078\u0001\u0000\u0000\u00008;\u0001\u0000\u0000\u00009:\u0005"+
		"\u0002\u0000\u0000:<\u0003\b\u0004\u0000;9\u0001\u0000\u0000\u0000;<\u0001"+
		"\u0000\u0000\u0000<=\u0001\u0000\u0000\u0000=>\u0006\u0000\uffff\uffff"+
		"\u0000>\u0001\u0001\u0000\u0000\u0000?@\u0005\u0002\u0000\u0000@A\u0003"+
		"\b\u0004\u0000AB\u0005\u0003\u0000\u0000BE\u0003\u0018\f\u0000CD\u0005"+
		"\u0004\u0000\u0000DF\u0003\u0018\f\u0000EC\u0001\u0000\u0000\u0000EF\u0001"+
		"\u0000\u0000\u0000FG\u0001\u0000\u0000\u0000GH\u0005\u0005\u0000\u0000"+
		"HI\u0006\u0001\uffff\uffff\u0000I\u0003\u0001\u0000\u0000\u0000JK\u0005"+
		"\u0006\u0000\u0000KL\u0005\u000f\u0000\u0000LM\u0003\u0018\f\u0000MN\u0005"+
		"\u0010\u0000\u0000NO\u0003\u0006\u0003\u0000OP\u0005\u0005\u0000\u0000"+
		"PQ\u0006\u0002\uffff\uffff\u0000Q\u0005\u0001\u0000\u0000\u0000RS\u0003"+
		"\u0018\f\u0000ST\u0003\u0016\u000b\u0000TU\u0006\u0003\uffff\uffff\u0000"+
		"U\u0007\u0001\u0000\u0000\u0000VW\u0003\u0018\f\u0000WX\u0006\u0004\uffff"+
		"\uffff\u0000X\t\u0001\u0000\u0000\u0000YZ\u0005\u0013\u0000\u0000Z[\u0003"+
		"\f\u0006\u0000[\\\u0006\u0005\uffff\uffff\u0000\\\u000b\u0001\u0000\u0000"+
		"\u0000]^\u0005\u001c\u0000\u0000^l\u0006\u0006\uffff\uffff\u0000_`\u0005"+
		"\u001b\u0000\u0000`l\u0006\u0006\uffff\uffff\u0000ab\u0003\u000e\u0007"+
		"\u0000bc\u0006\u0006\uffff\uffff\u0000cl\u0001\u0000\u0000\u0000de\u0005"+
		"\u001e\u0000\u0000el\u0006\u0006\uffff\uffff\u0000fg\u0005\u001f\u0000"+
		"\u0000gl\u0006\u0006\uffff\uffff\u0000hi\u0005\u0011\u0000\u0000ij\u0005"+
		"\u0012\u0000\u0000jl\u0006\u0006\uffff\uffff\u0000k]\u0001\u0000\u0000"+
		"\u0000k_\u0001\u0000\u0000\u0000ka\u0001\u0000\u0000\u0000kd\u0001\u0000"+
		"\u0000\u0000kf\u0001\u0000\u0000\u0000kh\u0001\u0000\u0000\u0000l\r\u0001"+
		"\u0000\u0000\u0000mo\u0005\u001a\u0000\u0000nm\u0001\u0000\u0000\u0000"+
		"no\u0001\u0000\u0000\u0000op\u0001\u0000\u0000\u0000pq\u0005\u001d\u0000"+
		"\u0000q\u000f\u0001\u0000\u0000\u0000rs\u0005\u0014\u0000\u0000st\u0005"+
		" \u0000\u0000tv\u0005\u000f\u0000\u0000uw\u0003\u0012\t\u0000vu\u0001"+
		"\u0000\u0000\u0000vw\u0001\u0000\u0000\u0000wx\u0001\u0000\u0000\u0000"+
		"xy\u0005\u0010\u0000\u0000yz\u0006\b\uffff\uffff\u0000z\u0011\u0001\u0000"+
		"\u0000\u0000{|\u0003\u0014\n\u0000|\u0083\u0006\t\uffff\uffff\u0000}~"+
		"\u0005\u0015\u0000\u0000~\u007f\u0003\u0014\n\u0000\u007f\u0080\u0006"+
		"\t\uffff\uffff\u0000\u0080\u0082\u0001\u0000\u0000\u0000\u0081}\u0001"+
		"\u0000\u0000\u0000\u0082\u0085\u0001\u0000\u0000\u0000\u0083\u0081\u0001"+
		"\u0000\u0000\u0000\u0083\u0084\u0001\u0000\u0000\u0000\u0084\u0013\u0001"+
		"\u0000\u0000\u0000\u0085\u0083\u0001\u0000\u0000\u0000\u0086\u0087\u0005"+
		"\u001d\u0000\u0000\u0087\u0095\u0006\n\uffff\uffff\u0000\u0088\u0089\u0005"+
		"\u001a\u0000\u0000\u0089\u008a\u0005\u001d\u0000\u0000\u008a\u0095\u0006"+
		"\n\uffff\uffff\u0000\u008b\u008c\u0005\u001e\u0000\u0000\u008c\u0095\u0006"+
		"\n\uffff\uffff\u0000\u008d\u008e\u0005\u001a\u0000\u0000\u008e\u008f\u0005"+
		"\u001e\u0000\u0000\u008f\u0095\u0006\n\uffff\uffff\u0000\u0090\u0091\u0005"+
		"\u001c\u0000\u0000\u0091\u0095\u0006\n\uffff\uffff\u0000\u0092\u0093\u0005"+
		"\u001b\u0000\u0000\u0093\u0095\u0006\n\uffff\uffff\u0000\u0094\u0086\u0001"+
		"\u0000\u0000\u0000\u0094\u0088\u0001\u0000\u0000\u0000\u0094\u008b\u0001"+
		"\u0000\u0000\u0000\u0094\u008d\u0001\u0000\u0000\u0000\u0094\u0090\u0001"+
		"\u0000\u0000\u0000\u0094\u0092\u0001\u0000\u0000\u0000\u0095\u0015\u0001"+
		"\u0000\u0000\u0000\u0096\u0097\u0005\u0001\u0000\u0000\u0097\u0098\u0003"+
		"\u0018\f\u0000\u0098\u009f\u0006\u000b\uffff\uffff\u0000\u0099\u009a\u0005"+
		"\u0001\u0000\u0000\u009a\u009b\u0003\u0018\f\u0000\u009b\u009c\u0006\u000b"+
		"\uffff\uffff\u0000\u009c\u009e\u0001\u0000\u0000\u0000\u009d\u0099\u0001"+
		"\u0000\u0000\u0000\u009e\u00a1\u0001\u0000\u0000\u0000\u009f\u009d\u0001"+
		"\u0000\u0000\u0000\u009f\u00a0\u0001\u0000\u0000\u0000\u00a0\u00a4\u0001"+
		"\u0000\u0000\u0000\u00a1\u009f\u0001\u0000\u0000\u0000\u00a2\u00a4\u0001"+
		"\u0000\u0000\u0000\u00a3\u0096\u0001\u0000\u0000\u0000\u00a3\u00a2\u0001"+
		"\u0000\u0000\u0000\u00a4\u0017\u0001\u0000\u0000\u0000\u00a5\u00a6\u0005"+
		"\b\u0000\u0000\u00a6\u00a7\u0005\u000e\u0000\u0000\u00a7\u00a8\u0003\u001a"+
		"\r\u0000\u00a8\u00af\u0006\f\uffff\uffff\u0000\u00a9\u00aa\u0005\u000e"+
		"\u0000\u0000\u00aa\u00ab\u0003\u001a\r\u0000\u00ab\u00ac\u0006\f\uffff"+
		"\uffff\u0000\u00ac\u00ae\u0001\u0000\u0000\u0000\u00ad\u00a9\u0001\u0000"+
		"\u0000\u0000\u00ae\u00b1\u0001\u0000\u0000\u0000\u00af\u00ad\u0001\u0000"+
		"\u0000\u0000\u00af\u00b0\u0001\u0000\u0000\u0000\u00b0\u00b3\u0001\u0000"+
		"\u0000\u0000\u00b1\u00af\u0001\u0000\u0000\u0000\u00b2\u00b4\u0003\"\u0011"+
		"\u0000\u00b3\u00b2\u0001\u0000\u0000\u0000\u00b3\u00b4\u0001\u0000\u0000"+
		"\u0000\u00b4\u00b5\u0001\u0000\u0000\u0000\u00b5\u00b6\u0006\f\uffff\uffff"+
		"\u0000\u00b6\u0104\u0001\u0000\u0000\u0000\u00b7\u00b8\u0005\u0007\u0000"+
		"\u0000\u00b8\u00bf\u0006\f\uffff\uffff\u0000\u00b9\u00ba\u0005\u000e\u0000"+
		"\u0000\u00ba\u00bb\u0003\u001a\r\u0000\u00bb\u00bc\u0006\f\uffff\uffff"+
		"\u0000\u00bc\u00be\u0001\u0000\u0000\u0000\u00bd\u00b9\u0001\u0000\u0000"+
		"\u0000\u00be\u00c1\u0001\u0000\u0000\u0000\u00bf\u00bd\u0001\u0000\u0000"+
		"\u0000\u00bf\u00c0\u0001\u0000\u0000\u0000\u00c0\u00c3\u0001\u0000\u0000"+
		"\u0000\u00c1\u00bf\u0001\u0000\u0000\u0000\u00c2\u00c4\u0003\"\u0011\u0000"+
		"\u00c3\u00c2\u0001\u0000\u0000\u0000\u00c3\u00c4\u0001\u0000\u0000\u0000"+
		"\u00c4\u00c5\u0001\u0000\u0000\u0000\u00c5\u0104\u0006\f\uffff\uffff\u0000"+
		"\u00c6\u00c7\u0003\u001a\r\u0000\u00c7\u00c9\u0006\f\uffff\uffff\u0000"+
		"\u00c8\u00ca\u0003\"\u0011\u0000\u00c9\u00c8\u0001\u0000\u0000\u0000\u00c9"+
		"\u00ca\u0001\u0000\u0000\u0000\u00ca\u00cb\u0001\u0000\u0000\u0000\u00cb"+
		"\u00cc\u0006\f\uffff\uffff\u0000\u00cc\u0104\u0001\u0000\u0000\u0000\u00cd"+
		"\u00ce\u0003\u001a\r\u0000\u00ce\u00d0\u0006\f\uffff\uffff\u0000\u00cf"+
		"\u00d1\u0005\u000e\u0000\u0000\u00d0\u00cf\u0001\u0000\u0000\u0000\u00d0"+
		"\u00d1\u0001\u0000\u0000\u0000\u00d1\u00d2\u0001\u0000\u0000\u0000\u00d2"+
		"\u00d3\u0003\u001c\u000e\u0000\u00d3\u00d4\u0006\f\uffff\uffff\u0000\u00d4"+
		"\u0104\u0001\u0000\u0000\u0000\u00d5\u00d6\u0003\u001a\r\u0000\u00d6\u00dd"+
		"\u0006\f\uffff\uffff\u0000\u00d7\u00d8\u0005\u000e\u0000\u0000\u00d8\u00d9"+
		"\u0003\u001a\r\u0000\u00d9\u00da\u0006\f\uffff\uffff\u0000\u00da\u00dc"+
		"\u0001\u0000\u0000\u0000\u00db\u00d7\u0001\u0000\u0000\u0000\u00dc\u00df"+
		"\u0001\u0000\u0000\u0000\u00dd\u00db\u0001\u0000\u0000\u0000\u00dd\u00de"+
		"\u0001\u0000\u0000\u0000\u00de\u00e0\u0001\u0000\u0000\u0000\u00df\u00dd"+
		"\u0001\u0000\u0000\u0000\u00e0\u00e1\u0005\u000e\u0000\u0000\u00e1\u00e2"+
		"\u0003\u001a\r\u0000\u00e2\u00e4\u0006\f\uffff\uffff\u0000\u00e3\u00e5"+
		"\u0003\"\u0011\u0000\u00e4\u00e3\u0001\u0000\u0000\u0000\u00e4\u00e5\u0001"+
		"\u0000\u0000\u0000\u00e5\u00e6\u0001\u0000\u0000\u0000\u00e6\u00e7\u0006"+
		"\f\uffff\uffff\u0000\u00e7\u0104\u0001\u0000\u0000\u0000\u00e8\u00e9\u0003"+
		"\u001a\r\u0000\u00e9\u00f0\u0006\f\uffff\uffff\u0000\u00ea\u00eb\u0005"+
		"\u000e\u0000\u0000\u00eb\u00ec\u0003\u001a\r\u0000\u00ec\u00ed\u0006\f"+
		"\uffff\uffff\u0000\u00ed\u00ef\u0001\u0000\u0000\u0000\u00ee\u00ea\u0001"+
		"\u0000\u0000\u0000\u00ef\u00f2\u0001\u0000\u0000\u0000\u00f0\u00ee\u0001"+
		"\u0000\u0000\u0000\u00f0\u00f1\u0001\u0000\u0000\u0000\u00f1\u00f3\u0001"+
		"\u0000\u0000\u0000\u00f2\u00f0\u0001\u0000\u0000\u0000\u00f3\u00f4\u0005"+
		"\u000e\u0000\u0000\u00f4\u00f5\u0003\u001a\r\u0000\u00f5\u00f7\u0006\f"+
		"\uffff\uffff\u0000\u00f6\u00f8\u0005\u000e\u0000\u0000\u00f7\u00f6\u0001"+
		"\u0000\u0000\u0000\u00f7\u00f8\u0001\u0000\u0000\u0000\u00f8\u00f9\u0001"+
		"\u0000\u0000\u0000\u00f9\u00fa\u0003\u001c\u000e\u0000\u00fa\u00fc\u0006"+
		"\f\uffff\uffff\u0000\u00fb\u00fd\u0003\"\u0011\u0000\u00fc\u00fb\u0001"+
		"\u0000\u0000\u0000\u00fc\u00fd\u0001\u0000\u0000\u0000\u00fd\u00fe\u0001"+
		"\u0000\u0000\u0000\u00fe\u00ff\u0006\f\uffff\uffff\u0000\u00ff\u0104\u0001"+
		"\u0000\u0000\u0000\u0100\u0101\u0003\u001c\u000e\u0000\u0101\u0102\u0006"+
		"\f\uffff\uffff\u0000\u0102\u0104\u0001\u0000\u0000\u0000\u0103\u00a5\u0001"+
		"\u0000\u0000\u0000\u0103\u00b7\u0001\u0000\u0000\u0000\u0103\u00c6\u0001"+
		"\u0000\u0000\u0000\u0103\u00cd\u0001\u0000\u0000\u0000\u0103\u00d5\u0001"+
		"\u0000\u0000\u0000\u0103\u00e8\u0001\u0000\u0000\u0000\u0103\u0100\u0001"+
		"\u0000\u0000\u0000\u0104\u0019\u0001\u0000\u0000\u0000\u0105\u0106\u0005"+
		" \u0000\u0000\u0106\u0108\u0005\u000f\u0000\u0000\u0107\u0109\u0003\u0012"+
		"\t\u0000\u0108\u0107\u0001\u0000\u0000\u0000\u0108\u0109\u0001\u0000\u0000"+
		"\u0000\u0109\u010a\u0001\u0000\u0000\u0000\u010a\u010b\u0005\u0010\u0000"+
		"\u0000\u010b\u010c\u0001\u0000\u0000\u0000\u010c\u0110\u0006\r\uffff\uffff"+
		"\u0000\u010d\u010e\u0005 \u0000\u0000\u010e\u0110\u0006\r\uffff\uffff"+
		"\u0000\u010f\u0105\u0001\u0000\u0000\u0000\u010f\u010d\u0001\u0000\u0000"+
		"\u0000\u0110\u001b\u0001\u0000\u0000\u0000\u0111\u0112\u0005\f\u0000\u0000"+
		"\u0112\u0113\u0003\u001e\u000f\u0000\u0113\u0114\u0006\u000e\uffff\uffff"+
		"\u0000\u0114\u0115\u0005\r\u0000\u0000\u0115\u001d\u0001\u0000\u0000\u0000"+
		"\u0116\u0117\u0003 \u0010\u0000\u0117\u011e\u0006\u000f\uffff\uffff\u0000"+
		"\u0118\u0119\u0005\u0015\u0000\u0000\u0119\u011a\u0003 \u0010\u0000\u011a"+
		"\u011b\u0006\u000f\uffff\uffff\u0000\u011b\u011d\u0001\u0000\u0000\u0000"+
		"\u011c\u0118\u0001\u0000\u0000\u0000\u011d\u0120\u0001\u0000\u0000\u0000"+
		"\u011e\u011c\u0001\u0000\u0000\u0000\u011e\u011f\u0001\u0000\u0000\u0000"+
		"\u011f\u001f\u0001\u0000\u0000\u0000\u0120\u011e\u0001\u0000\u0000\u0000"+
		"\u0121\u0122\u0005 \u0000\u0000\u0122\u012c\u0006\u0010\uffff\uffff\u0000"+
		"\u0123\u0124\u0005\u001b\u0000\u0000\u0124\u012c\u0006\u0010\uffff\uffff"+
		"\u0000\u0125\u0126\u0005\u001c\u0000\u0000\u0126\u012c\u0006\u0010\uffff"+
		"\uffff\u0000\u0127\u0128\u0005\u001d\u0000\u0000\u0128\u012c\u0006\u0010"+
		"\uffff\uffff\u0000\u0129\u012a\u0005\u001e\u0000\u0000\u012a\u012c\u0006"+
		"\u0010\uffff\uffff\u0000\u012b\u0121\u0001\u0000\u0000\u0000\u012b\u0123"+
		"\u0001\u0000\u0000\u0000\u012b\u0125\u0001\u0000\u0000\u0000\u012b\u0127"+
		"\u0001\u0000\u0000\u0000\u012b\u0129\u0001\u0000\u0000\u0000\u012c!\u0001"+
		"\u0000\u0000\u0000\u012d\u012e\u0003&\u0013\u0000\u012e\u012f\u0003$\u0012"+
		"\u0000\u012f\u0130\u0006\u0011\uffff\uffff\u0000\u0130#\u0001\u0000\u0000"+
		"\u0000\u0131\u0135\u0001\u0000\u0000\u0000\u0132\u0135\u0005\u001d\u0000"+
		"\u0000\u0133\u0135\u0005\u001e\u0000\u0000\u0134\u0131\u0001\u0000\u0000"+
		"\u0000\u0134\u0132\u0001\u0000\u0000\u0000\u0134\u0133\u0001\u0000\u0000"+
		"\u0000\u0135%\u0001\u0000\u0000\u0000\u0136\u0137\u0007\u0000\u0000\u0000"+
		"\u0137\'\u0001\u0000\u0000\u0000\u001f),147;Eknv\u0083\u0094\u009f\u00a3"+
		"\u00af\u00b3\u00bf\u00c3\u00c9\u00d0\u00dd\u00e4\u00f0\u00f7\u00fc\u0103"+
		"\u0108\u010f\u011e\u012b\u0134";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}