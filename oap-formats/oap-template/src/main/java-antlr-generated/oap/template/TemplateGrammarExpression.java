// Generated from TemplateGrammarExpression.g4 by ANTLR 4.13.0

package oap.template;

import oap.template.tree.*;
import oap.template.tree.Math;

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
		IF=1, THEN=2, ELSE=3, END=4, AND=5, OR=6, NOT=7, BANG=8, EQ_KW=9, NE_KW=10, 
		EQI_KW=11, CONTAINS_KW=12, EQEQ=13, NEQ=14, GE_OP=15, LE_OP=16, GT_OP=17, 
		LT_OP=18, VAR_ID=19, ROOT=20, BLOCK_COMMENT=21, HORZ_WS=22, VERT_WS=23, 
		LBRACE=24, RBRACE=25, DOT=26, LPAREN=27, RPAREN=28, LBRACK=29, RBRACK=30, 
		DQUESTION=31, SEMI=32, COMMA=33, STAR=34, SLASH=35, PERCENT=36, PLUS=37, 
		MINUS=38, DSTRING=39, SSTRING=40, DECDIGITS=41, FLOAT=42, BOOLEAN=43, 
		ID=44, CAST_TYPE=45, ERR_CHAR=46, C_HORZ_WS=47, C_VERT_WS=48, CERR_CHAR=49;
	public static final int
		RULE_expression = 0, RULE_ifCode = 1, RULE_ifBranchCode = 2, RULE_withCode = 3, 
		RULE_concatBody = 4, RULE_topLevelConcat = 5, RULE_exprsCode = 6, RULE_ifCondition = 7, 
		RULE_conditionOr = 8, RULE_conditionAnd = 9, RULE_conditionNot = 10, RULE_conditionAtom = 11, 
		RULE_compareRhs = 12, RULE_defaultValue = 13, RULE_defaultValueType = 14, 
		RULE_longRule = 15, RULE_function = 16, RULE_functionArgs = 17, RULE_functionArg = 18, 
		RULE_exprs = 19, RULE_expr = 20, RULE_concatenation = 21, RULE_citems = 22, 
		RULE_citem = 23, RULE_math = 24, RULE_number = 25, RULE_mathOperation = 26;
	private static String[] makeRuleNames() {
		return new String[] {
			"expression", "ifCode", "ifBranchCode", "withCode", "concatBody", "topLevelConcat", 
			"exprsCode", "ifCondition", "conditionOr", "conditionAnd", "conditionNot", 
			"conditionAtom", "compareRhs", "defaultValue", "defaultValueType", "longRule", 
			"function", "functionArgs", "functionArg", "exprs", "expr", "concatenation", 
			"citems", "citem", "math", "number", "mathOperation"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "'if'", "'then'", "'else'", "'end'", "'and'", "'or'", "'not'", 
			"'!'", "'eq'", "'ne'", "'eqi'", "'contains'", "'=='", "'!='", "'>='", 
			"'<='", "'>'", "'<'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "IF", "THEN", "ELSE", "END", "AND", "OR", "NOT", "BANG", "EQ_KW", 
			"NE_KW", "EQI_KW", "CONTAINS_KW", "EQEQ", "NEQ", "GE_OP", "LE_OP", "GT_OP", 
			"LT_OP", "VAR_ID", "ROOT", "BLOCK_COMMENT", "HORZ_WS", "VERT_WS", "LBRACE", 
			"RBRACE", "DOT", "LPAREN", "RPAREN", "LBRACK", "RBRACK", "DQUESTION", 
			"SEMI", "COMMA", "STAR", "SLASH", "PERCENT", "PLUS", "MINUS", "DSTRING", 
			"SSTRING", "DECDIGITS", "FLOAT", "BOOLEAN", "ID", "CAST_TYPE", "ERR_CHAR", 
			"C_HORZ_WS", "C_VERT_WS", "CERR_CHAR"
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
			setState(55);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==BLOCK_COMMENT) {
				{
				setState(54);
				((ExpressionContext)_localctx).BLOCK_COMMENT = match(BLOCK_COMMENT);
				}
			}

			setState(58);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==CAST_TYPE) {
				{
				setState(57);
				((ExpressionContext)_localctx).CAST_TYPE = match(CAST_TYPE);
				}
			}

			setState(63);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,2,_ctx) ) {
			case 1:
				{
				setState(60);
				((ExpressionContext)_localctx).ifCode = ifCode();
				}
				break;
			case 2:
				{
				setState(61);
				((ExpressionContext)_localctx).withCode = withCode();
				}
				break;
			case 3:
				{
				setState(62);
				((ExpressionContext)_localctx).exprsCode = exprsCode();
				}
				break;
			}
			setState(66);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==DQUESTION) {
				{
				setState(65);
				((ExpressionContext)_localctx).defaultValue = defaultValue();
				}
			}

			setState(69);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==SEMI) {
				{
				setState(68);
				((ExpressionContext)_localctx).function = function();
				}
			}

			setState(73);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==IF) {
				{
				setState(71);
				match(IF);
				setState(72);
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
		public IfBranchCodeContext thenCode;
		public IfBranchCodeContext elseCode;
		public TerminalNode IF() { return getToken(TemplateGrammarExpression.IF, 0); }
		public IfConditionContext ifCondition() {
			return getRuleContext(IfConditionContext.class,0);
		}
		public TerminalNode THEN() { return getToken(TemplateGrammarExpression.THEN, 0); }
		public TerminalNode END() { return getToken(TemplateGrammarExpression.END, 0); }
		public List<IfBranchCodeContext> ifBranchCode() {
			return getRuleContexts(IfBranchCodeContext.class);
		}
		public IfBranchCodeContext ifBranchCode(int i) {
			return getRuleContext(IfBranchCodeContext.class,i);
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
			setState(77);
			match(IF);
			setState(78);
			((IfCodeContext)_localctx).ifCondition = ifCondition();
			setState(79);
			match(THEN);
			setState(80);
			((IfCodeContext)_localctx).thenCode = ifBranchCode();
			setState(83);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==ELSE) {
				{
				setState(81);
				match(ELSE);
				setState(82);
				((IfCodeContext)_localctx).elseCode = ifBranchCode();
				}
			}

			setState(85);
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
	public static class IfBranchCodeContext extends ParserRuleContext {
		public Exprs ret = new Exprs();
		public ExprsContext exprs;
		public Token SSTRING;
		public Token DSTRING;
		public Token DECDIGITS;
		public Token FLOAT;
		public ExprsContext exprs() {
			return getRuleContext(ExprsContext.class,0);
		}
		public TerminalNode SSTRING() { return getToken(TemplateGrammarExpression.SSTRING, 0); }
		public TerminalNode DSTRING() { return getToken(TemplateGrammarExpression.DSTRING, 0); }
		public TerminalNode DECDIGITS() { return getToken(TemplateGrammarExpression.DECDIGITS, 0); }
		public TerminalNode MINUS() { return getToken(TemplateGrammarExpression.MINUS, 0); }
		public TerminalNode FLOAT() { return getToken(TemplateGrammarExpression.FLOAT, 0); }
		public IfBranchCodeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_ifBranchCode; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarExpressionListener ) ((TemplateGrammarExpressionListener)listener).enterIfBranchCode(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarExpressionListener ) ((TemplateGrammarExpressionListener)listener).exitIfBranchCode(this);
		}
	}

	public final IfBranchCodeContext ifBranchCode() throws RecognitionException {
		IfBranchCodeContext _localctx = new IfBranchCodeContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_ifBranchCode);
		try {
			setState(105);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,7,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(88);
				((IfBranchCodeContext)_localctx).exprs = exprs();
				 ((IfBranchCodeContext)_localctx).ret =  ((IfBranchCodeContext)_localctx).exprs.ret; 
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(91);
				((IfBranchCodeContext)_localctx).SSTRING = match(SSTRING);
				 _localctx.ret.concatenation = new Concatenation( List.of( sdStringToString( (((IfBranchCodeContext)_localctx).SSTRING!=null?((IfBranchCodeContext)_localctx).SSTRING.getText():null) ) ) ); 
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(93);
				((IfBranchCodeContext)_localctx).DSTRING = match(DSTRING);
				 _localctx.ret.concatenation = new Concatenation( List.of( sdStringToString( (((IfBranchCodeContext)_localctx).DSTRING!=null?((IfBranchCodeContext)_localctx).DSTRING.getText():null) ) ) ); 
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(95);
				((IfBranchCodeContext)_localctx).DECDIGITS = match(DECDIGITS);
				 _localctx.ret.concatenation = new Concatenation( List.of( new NumericLiteral( (((IfBranchCodeContext)_localctx).DECDIGITS!=null?((IfBranchCodeContext)_localctx).DECDIGITS.getText():null) ) ) ); 
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(97);
				match(MINUS);
				setState(98);
				((IfBranchCodeContext)_localctx).DECDIGITS = match(DECDIGITS);
				 _localctx.ret.concatenation = new Concatenation( List.of( new NumericLiteral( "-" + (((IfBranchCodeContext)_localctx).DECDIGITS!=null?((IfBranchCodeContext)_localctx).DECDIGITS.getText():null) ) ) ); 
				}
				break;
			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(100);
				((IfBranchCodeContext)_localctx).FLOAT = match(FLOAT);
				 _localctx.ret.concatenation = new Concatenation( List.of( new NumericLiteral( (((IfBranchCodeContext)_localctx).FLOAT!=null?((IfBranchCodeContext)_localctx).FLOAT.getText():null) ) ) ); 
				}
				break;
			case 7:
				enterOuterAlt(_localctx, 7);
				{
				setState(102);
				match(MINUS);
				setState(103);
				((IfBranchCodeContext)_localctx).FLOAT = match(FLOAT);
				 _localctx.ret.concatenation = new Concatenation( List.of( new NumericLiteral( "-" + (((IfBranchCodeContext)_localctx).FLOAT!=null?((IfBranchCodeContext)_localctx).FLOAT.getText():null) ) ) ); 
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
	public static class WithCodeContext extends ParserRuleContext {
		public WithCondition ret;
		public ExprsContext scopePath;
		public ConcatBodyContext concatItems;
		public ExprsCodeContext bodyExprs;
		public TerminalNode LBRACE() { return getToken(TemplateGrammarExpression.LBRACE, 0); }
		public TerminalNode RBRACE() { return getToken(TemplateGrammarExpression.RBRACE, 0); }
		public ExprsContext exprs() {
			return getRuleContext(ExprsContext.class,0);
		}
		public ConcatBodyContext concatBody() {
			return getRuleContext(ConcatBodyContext.class,0);
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
		enterRule(_localctx, 6, RULE_withCode);
		try {
			setState(119);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,8,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(107);
				((WithCodeContext)_localctx).scopePath = exprs();
				setState(108);
				match(LBRACE);
				setState(109);
				((WithCodeContext)_localctx).concatItems = concatBody();
				setState(110);
				match(RBRACE);

				        Exprs bodyExprs = new Exprs();
				        bodyExprs.concatenation = new Concatenation( ((WithCodeContext)_localctx).concatItems.ret );
				        ArrayList<Exprs> body = new ArrayList<>();
				        body.add( bodyExprs );
				        ((WithCodeContext)_localctx).ret =  new WithCondition( ((WithCodeContext)_localctx).scopePath.ret, body );
				      
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(113);
				((WithCodeContext)_localctx).scopePath = exprs();
				setState(114);
				match(LBRACE);
				setState(115);
				((WithCodeContext)_localctx).bodyExprs = exprsCode();
				setState(116);
				match(RBRACE);

				        ((WithCodeContext)_localctx).ret =  new WithCondition( ((WithCodeContext)_localctx).scopePath.ret, ((WithCodeContext)_localctx).bodyExprs.ret );
				      
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
	public static class ConcatBodyContext extends ParserRuleContext {
		public ArrayList<Object> ret = new ArrayList<>();
		public CitemContext citem;
		public List<CitemContext> citem() {
			return getRuleContexts(CitemContext.class);
		}
		public CitemContext citem(int i) {
			return getRuleContext(CitemContext.class,i);
		}
		public List<TerminalNode> PLUS() { return getTokens(TemplateGrammarExpression.PLUS); }
		public TerminalNode PLUS(int i) {
			return getToken(TemplateGrammarExpression.PLUS, i);
		}
		public ConcatBodyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_concatBody; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarExpressionListener ) ((TemplateGrammarExpressionListener)listener).enterConcatBody(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarExpressionListener ) ((TemplateGrammarExpressionListener)listener).exitConcatBody(this);
		}
	}

	public final ConcatBodyContext concatBody() throws RecognitionException {
		ConcatBodyContext _localctx = new ConcatBodyContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_concatBody);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(121);
			((ConcatBodyContext)_localctx).citem = citem();
			 _localctx.ret.add( ((ConcatBodyContext)_localctx).citem.ret ); 
			setState(127); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(123);
				match(PLUS);
				setState(124);
				((ConcatBodyContext)_localctx).citem = citem();
				 _localctx.ret.add( ((ConcatBodyContext)_localctx).citem.ret ); 
				}
				}
				setState(129); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==PLUS );
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
	public static class TopLevelConcatContext extends ParserRuleContext {
		public Exprs ret = new Exprs();
		public CitemContext first;
		public CitemContext next;
		public List<CitemContext> citem() {
			return getRuleContexts(CitemContext.class);
		}
		public CitemContext citem(int i) {
			return getRuleContext(CitemContext.class,i);
		}
		public List<TerminalNode> PLUS() { return getTokens(TemplateGrammarExpression.PLUS); }
		public TerminalNode PLUS(int i) {
			return getToken(TemplateGrammarExpression.PLUS, i);
		}
		public TopLevelConcatContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_topLevelConcat; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarExpressionListener ) ((TemplateGrammarExpressionListener)listener).enterTopLevelConcat(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarExpressionListener ) ((TemplateGrammarExpressionListener)listener).exitTopLevelConcat(this);
		}
	}

	public final TopLevelConcatContext topLevelConcat() throws RecognitionException {
		TopLevelConcatContext _localctx = new TopLevelConcatContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_topLevelConcat);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(131);
			((TopLevelConcatContext)_localctx).first = citem();

			        _localctx.ret.concatenation = new Concatenation( new ArrayList<>() );
			        _localctx.ret.concatenation.items.add( ((TopLevelConcatContext)_localctx).first.ret );
			      
			setState(137); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(133);
				match(PLUS);
				setState(134);
				((TopLevelConcatContext)_localctx).next = citem();

				        _localctx.ret.concatenation.items.add( ((TopLevelConcatContext)_localctx).next.ret );
				      
				}
				}
				setState(139); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==PLUS );
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
		public TopLevelConcatContext topLevelConcat;
		public ExprsContext exprs;
		public TopLevelConcatContext topLevelConcat() {
			return getRuleContext(TopLevelConcatContext.class,0);
		}
		public ExprsContext exprs() {
			return getRuleContext(ExprsContext.class,0);
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
		enterRule(_localctx, 12, RULE_exprsCode);
		try {
			setState(147);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,11,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(141);
				((ExprsCodeContext)_localctx).topLevelConcat = topLevelConcat();

				        _localctx.ret.add( ((ExprsCodeContext)_localctx).topLevelConcat.ret );
				      
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(144);
				((ExprsCodeContext)_localctx).exprs = exprs();

				        _localctx.ret.add( ((ExprsCodeContext)_localctx).exprs.ret );
				      
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
	public static class IfConditionContext extends ParserRuleContext {
		public ConditionExpr ret;
		public ConditionOrContext conditionOr;
		public ConditionOrContext conditionOr() {
			return getRuleContext(ConditionOrContext.class,0);
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
		enterRule(_localctx, 14, RULE_ifCondition);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(149);
			((IfConditionContext)_localctx).conditionOr = conditionOr();
			 ((IfConditionContext)_localctx).ret =  ((IfConditionContext)_localctx).conditionOr.ret; 
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
	public static class ConditionOrContext extends ParserRuleContext {
		public ConditionExpr ret;
		public ConditionAndContext left;
		public ConditionAndContext right;
		public List<ConditionAndContext> conditionAnd() {
			return getRuleContexts(ConditionAndContext.class);
		}
		public ConditionAndContext conditionAnd(int i) {
			return getRuleContext(ConditionAndContext.class,i);
		}
		public List<TerminalNode> OR() { return getTokens(TemplateGrammarExpression.OR); }
		public TerminalNode OR(int i) {
			return getToken(TemplateGrammarExpression.OR, i);
		}
		public ConditionOrContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_conditionOr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarExpressionListener ) ((TemplateGrammarExpressionListener)listener).enterConditionOr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarExpressionListener ) ((TemplateGrammarExpressionListener)listener).exitConditionOr(this);
		}
	}

	public final ConditionOrContext conditionOr() throws RecognitionException {
		ConditionOrContext _localctx = new ConditionOrContext(_ctx, getState());
		enterRule(_localctx, 16, RULE_conditionOr);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(152);
			((ConditionOrContext)_localctx).left = conditionAnd();
			 ((ConditionOrContext)_localctx).ret =  ((ConditionOrContext)_localctx).left.ret; 
			setState(160);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==OR) {
				{
				{
				setState(154);
				match(OR);
				setState(155);
				((ConditionOrContext)_localctx).right = conditionAnd();
				 ((ConditionOrContext)_localctx).ret =  new OrConditionExpr( _localctx.ret, ((ConditionOrContext)_localctx).right.ret ); 
				}
				}
				setState(162);
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
	public static class ConditionAndContext extends ParserRuleContext {
		public ConditionExpr ret;
		public ConditionNotContext left;
		public ConditionNotContext right;
		public List<ConditionNotContext> conditionNot() {
			return getRuleContexts(ConditionNotContext.class);
		}
		public ConditionNotContext conditionNot(int i) {
			return getRuleContext(ConditionNotContext.class,i);
		}
		public List<TerminalNode> AND() { return getTokens(TemplateGrammarExpression.AND); }
		public TerminalNode AND(int i) {
			return getToken(TemplateGrammarExpression.AND, i);
		}
		public ConditionAndContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_conditionAnd; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarExpressionListener ) ((TemplateGrammarExpressionListener)listener).enterConditionAnd(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarExpressionListener ) ((TemplateGrammarExpressionListener)listener).exitConditionAnd(this);
		}
	}

	public final ConditionAndContext conditionAnd() throws RecognitionException {
		ConditionAndContext _localctx = new ConditionAndContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_conditionAnd);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(163);
			((ConditionAndContext)_localctx).left = conditionNot();
			 ((ConditionAndContext)_localctx).ret =  ((ConditionAndContext)_localctx).left.ret; 
			setState(171);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AND) {
				{
				{
				setState(165);
				match(AND);
				setState(166);
				((ConditionAndContext)_localctx).right = conditionNot();
				 ((ConditionAndContext)_localctx).ret =  new AndConditionExpr( _localctx.ret, ((ConditionAndContext)_localctx).right.ret ); 
				}
				}
				setState(173);
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
	public static class ConditionNotContext extends ParserRuleContext {
		public ConditionExpr ret;
		public ConditionNotContext inner;
		public ConditionAtomContext conditionAtom;
		public TerminalNode NOT() { return getToken(TemplateGrammarExpression.NOT, 0); }
		public TerminalNode BANG() { return getToken(TemplateGrammarExpression.BANG, 0); }
		public ConditionNotContext conditionNot() {
			return getRuleContext(ConditionNotContext.class,0);
		}
		public ConditionAtomContext conditionAtom() {
			return getRuleContext(ConditionAtomContext.class,0);
		}
		public ConditionNotContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_conditionNot; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarExpressionListener ) ((TemplateGrammarExpressionListener)listener).enterConditionNot(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarExpressionListener ) ((TemplateGrammarExpressionListener)listener).exitConditionNot(this);
		}
	}

	public final ConditionNotContext conditionNot() throws RecognitionException {
		ConditionNotContext _localctx = new ConditionNotContext(_ctx, getState());
		enterRule(_localctx, 20, RULE_conditionNot);
		int _la;
		try {
			setState(181);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case NOT:
			case BANG:
				enterOuterAlt(_localctx, 1);
				{
				setState(174);
				_la = _input.LA(1);
				if ( !(_la==NOT || _la==BANG) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(175);
				((ConditionNotContext)_localctx).inner = conditionNot();
				 ((ConditionNotContext)_localctx).ret =  new NotConditionExpr( ((ConditionNotContext)_localctx).inner.ret ); 
				}
				break;
			case VAR_ID:
			case ROOT:
			case LPAREN:
			case ID:
				enterOuterAlt(_localctx, 2);
				{
				setState(178);
				((ConditionNotContext)_localctx).conditionAtom = conditionAtom();
				 ((ConditionNotContext)_localctx).ret =  ((ConditionNotContext)_localctx).conditionAtom.ret; 
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
	public static class ConditionAtomContext extends ParserRuleContext {
		public ConditionExpr ret;
		public IfConditionContext ifCondition;
		public ExprsContext left;
		public Token op;
		public CompareRhsContext right;
		public ExprsContext exprs;
		public TerminalNode LPAREN() { return getToken(TemplateGrammarExpression.LPAREN, 0); }
		public IfConditionContext ifCondition() {
			return getRuleContext(IfConditionContext.class,0);
		}
		public TerminalNode RPAREN() { return getToken(TemplateGrammarExpression.RPAREN, 0); }
		public ExprsContext exprs() {
			return getRuleContext(ExprsContext.class,0);
		}
		public CompareRhsContext compareRhs() {
			return getRuleContext(CompareRhsContext.class,0);
		}
		public TerminalNode EQEQ() { return getToken(TemplateGrammarExpression.EQEQ, 0); }
		public TerminalNode EQ_KW() { return getToken(TemplateGrammarExpression.EQ_KW, 0); }
		public TerminalNode NEQ() { return getToken(TemplateGrammarExpression.NEQ, 0); }
		public TerminalNode NE_KW() { return getToken(TemplateGrammarExpression.NE_KW, 0); }
		public TerminalNode GT_OP() { return getToken(TemplateGrammarExpression.GT_OP, 0); }
		public TerminalNode LT_OP() { return getToken(TemplateGrammarExpression.LT_OP, 0); }
		public TerminalNode GE_OP() { return getToken(TemplateGrammarExpression.GE_OP, 0); }
		public TerminalNode LE_OP() { return getToken(TemplateGrammarExpression.LE_OP, 0); }
		public TerminalNode EQI_KW() { return getToken(TemplateGrammarExpression.EQI_KW, 0); }
		public TerminalNode CONTAINS_KW() { return getToken(TemplateGrammarExpression.CONTAINS_KW, 0); }
		public ConditionAtomContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_conditionAtom; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarExpressionListener ) ((TemplateGrammarExpressionListener)listener).enterConditionAtom(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarExpressionListener ) ((TemplateGrammarExpressionListener)listener).exitConditionAtom(this);
		}
	}

	public final ConditionAtomContext conditionAtom() throws RecognitionException {
		ConditionAtomContext _localctx = new ConditionAtomContext(_ctx, getState());
		enterRule(_localctx, 22, RULE_conditionAtom);
		int _la;
		try {
			setState(196);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,15,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(183);
				match(LPAREN);
				setState(184);
				((ConditionAtomContext)_localctx).ifCondition = ifCondition();
				setState(185);
				match(RPAREN);
				 ((ConditionAtomContext)_localctx).ret =  ((ConditionAtomContext)_localctx).ifCondition.ret; 
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(188);
				((ConditionAtomContext)_localctx).left = exprs();
				setState(189);
				((ConditionAtomContext)_localctx).op = _input.LT(1);
				_la = _input.LA(1);
				if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 523776L) != 0)) ) {
					((ConditionAtomContext)_localctx).op = (Token)_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(190);
				((ConditionAtomContext)_localctx).right = compareRhs();

				        ((ConditionAtomContext)_localctx).ret =  new CompareConditionExpr( ((ConditionAtomContext)_localctx).left.ret, ((ConditionAtomContext)_localctx).op.getText(), ((ConditionAtomContext)_localctx).right.ret );
				      
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(193);
				((ConditionAtomContext)_localctx).exprs = exprs();
				 ((ConditionAtomContext)_localctx).ret =  new FieldConditionExpr( ((ConditionAtomContext)_localctx).exprs.ret ); 
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
	public static class CompareRhsContext extends ParserRuleContext {
		public CompareValue ret;
		public Token SSTRING;
		public Token DSTRING;
		public Token DECDIGITS;
		public Token FLOAT;
		public Token BOOLEAN;
		public TerminalNode SSTRING() { return getToken(TemplateGrammarExpression.SSTRING, 0); }
		public TerminalNode DSTRING() { return getToken(TemplateGrammarExpression.DSTRING, 0); }
		public TerminalNode DECDIGITS() { return getToken(TemplateGrammarExpression.DECDIGITS, 0); }
		public TerminalNode MINUS() { return getToken(TemplateGrammarExpression.MINUS, 0); }
		public TerminalNode FLOAT() { return getToken(TemplateGrammarExpression.FLOAT, 0); }
		public TerminalNode BOOLEAN() { return getToken(TemplateGrammarExpression.BOOLEAN, 0); }
		public CompareRhsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_compareRhs; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarExpressionListener ) ((TemplateGrammarExpressionListener)listener).enterCompareRhs(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarExpressionListener ) ((TemplateGrammarExpressionListener)listener).exitCompareRhs(this);
		}
	}

	public final CompareRhsContext compareRhs() throws RecognitionException {
		CompareRhsContext _localctx = new CompareRhsContext(_ctx, getState());
		enterRule(_localctx, 24, RULE_compareRhs);
		try {
			setState(214);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,16,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(198);
				((CompareRhsContext)_localctx).SSTRING = match(SSTRING);
				 ((CompareRhsContext)_localctx).ret =  new LiteralCompareValue( sdStringToString( (((CompareRhsContext)_localctx).SSTRING!=null?((CompareRhsContext)_localctx).SSTRING.getText():null) ) ); 
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(200);
				((CompareRhsContext)_localctx).DSTRING = match(DSTRING);
				 ((CompareRhsContext)_localctx).ret =  new LiteralCompareValue( sdStringToString( (((CompareRhsContext)_localctx).DSTRING!=null?((CompareRhsContext)_localctx).DSTRING.getText():null) ) ); 
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(202);
				((CompareRhsContext)_localctx).DECDIGITS = match(DECDIGITS);
				 ((CompareRhsContext)_localctx).ret =  new LiteralCompareValue( (((CompareRhsContext)_localctx).DECDIGITS!=null?((CompareRhsContext)_localctx).DECDIGITS.getText():null) ); 
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(204);
				match(MINUS);
				setState(205);
				((CompareRhsContext)_localctx).DECDIGITS = match(DECDIGITS);
				 ((CompareRhsContext)_localctx).ret =  new LiteralCompareValue( "-" + (((CompareRhsContext)_localctx).DECDIGITS!=null?((CompareRhsContext)_localctx).DECDIGITS.getText():null) ); 
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(207);
				((CompareRhsContext)_localctx).FLOAT = match(FLOAT);
				 ((CompareRhsContext)_localctx).ret =  new LiteralCompareValue( (((CompareRhsContext)_localctx).FLOAT!=null?((CompareRhsContext)_localctx).FLOAT.getText():null) ); 
				}
				break;
			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(209);
				match(MINUS);
				setState(210);
				((CompareRhsContext)_localctx).FLOAT = match(FLOAT);
				 ((CompareRhsContext)_localctx).ret =  new LiteralCompareValue( "-" + (((CompareRhsContext)_localctx).FLOAT!=null?((CompareRhsContext)_localctx).FLOAT.getText():null) ); 
				}
				break;
			case 7:
				enterOuterAlt(_localctx, 7);
				{
				setState(212);
				((CompareRhsContext)_localctx).BOOLEAN = match(BOOLEAN);
				 ((CompareRhsContext)_localctx).ret =  new LiteralCompareValue( (((CompareRhsContext)_localctx).BOOLEAN!=null?((CompareRhsContext)_localctx).BOOLEAN.getText():null) ); 
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
		enterRule(_localctx, 26, RULE_defaultValue);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(216);
			match(DQUESTION);
			setState(217);
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
		enterRule(_localctx, 28, RULE_defaultValueType);
		try {
			setState(234);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case SSTRING:
				enterOuterAlt(_localctx, 1);
				{
				setState(220);
				((DefaultValueTypeContext)_localctx).SSTRING = match(SSTRING);
				 ((DefaultValueTypeContext)_localctx).ret =  sdStringToString( (((DefaultValueTypeContext)_localctx).SSTRING!=null?((DefaultValueTypeContext)_localctx).SSTRING.getText():null) ); 
				}
				break;
			case DSTRING:
				enterOuterAlt(_localctx, 2);
				{
				setState(222);
				((DefaultValueTypeContext)_localctx).DSTRING = match(DSTRING);
				 ((DefaultValueTypeContext)_localctx).ret =  sdStringToString((((DefaultValueTypeContext)_localctx).DSTRING!=null?((DefaultValueTypeContext)_localctx).DSTRING.getText():null)); 
				}
				break;
			case MINUS:
			case DECDIGITS:
				enterOuterAlt(_localctx, 3);
				{
				setState(224);
				((DefaultValueTypeContext)_localctx).longRule = longRule();
				 ((DefaultValueTypeContext)_localctx).ret =  (((DefaultValueTypeContext)_localctx).longRule!=null?_input.getText(((DefaultValueTypeContext)_localctx).longRule.start,((DefaultValueTypeContext)_localctx).longRule.stop):null); 
				}
				break;
			case FLOAT:
				enterOuterAlt(_localctx, 4);
				{
				setState(227);
				((DefaultValueTypeContext)_localctx).FLOAT = match(FLOAT);
				 ((DefaultValueTypeContext)_localctx).ret =  (((DefaultValueTypeContext)_localctx).FLOAT!=null?((DefaultValueTypeContext)_localctx).FLOAT.getText():null); 
				}
				break;
			case BOOLEAN:
				enterOuterAlt(_localctx, 5);
				{
				setState(229);
				((DefaultValueTypeContext)_localctx).BOOLEAN = match(BOOLEAN);
				 ((DefaultValueTypeContext)_localctx).ret =  (((DefaultValueTypeContext)_localctx).BOOLEAN!=null?((DefaultValueTypeContext)_localctx).BOOLEAN.getText():null); 
				}
				break;
			case LBRACK:
				enterOuterAlt(_localctx, 6);
				{
				setState(231);
				match(LBRACK);
				setState(232);
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
		enterRule(_localctx, 30, RULE_longRule);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(237);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==MINUS) {
				{
				setState(236);
				match(MINUS);
				}
			}

			setState(239);
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
		enterRule(_localctx, 32, RULE_function);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(241);
			match(SEMI);
			setState(242);
			((FunctionContext)_localctx).ID = match(ID);
			setState(243);
			match(LPAREN);
			setState(245);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 8521215115264L) != 0)) {
				{
				setState(244);
				((FunctionContext)_localctx).functionArgs = functionArgs();
				}
			}

			setState(247);
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
		enterRule(_localctx, 34, RULE_functionArgs);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(250);
			((FunctionArgsContext)_localctx).functionArg = functionArg();
			 _localctx.ret.add( ((FunctionArgsContext)_localctx).functionArg.ret ); 
			setState(258);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(252);
				match(COMMA);
				setState(253);
				((FunctionArgsContext)_localctx).functionArg = functionArg();
				 _localctx.ret.add( ((FunctionArgsContext)_localctx).functionArg.ret ); 
				}
				}
				setState(260);
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
		enterRule(_localctx, 36, RULE_functionArg);
		try {
			setState(275);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,21,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(261);
				((FunctionArgContext)_localctx).DECDIGITS = match(DECDIGITS);
				 ((FunctionArgContext)_localctx).ret =  (((FunctionArgContext)_localctx).DECDIGITS!=null?((FunctionArgContext)_localctx).DECDIGITS.getText():null); 
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(263);
				match(MINUS);
				setState(264);
				((FunctionArgContext)_localctx).DECDIGITS = match(DECDIGITS);
				 ((FunctionArgContext)_localctx).ret =  "-" + (((FunctionArgContext)_localctx).DECDIGITS!=null?((FunctionArgContext)_localctx).DECDIGITS.getText():null); 
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(266);
				((FunctionArgContext)_localctx).FLOAT = match(FLOAT);
				 ((FunctionArgContext)_localctx).ret =  (((FunctionArgContext)_localctx).FLOAT!=null?((FunctionArgContext)_localctx).FLOAT.getText():null); 
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(268);
				match(MINUS);
				setState(269);
				((FunctionArgContext)_localctx).FLOAT = match(FLOAT);
				 ((FunctionArgContext)_localctx).ret =  "-" + (((FunctionArgContext)_localctx).FLOAT!=null?((FunctionArgContext)_localctx).FLOAT.getText():null); 
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(271);
				((FunctionArgContext)_localctx).SSTRING = match(SSTRING);
				 ((FunctionArgContext)_localctx).ret =  sStringToDString( (((FunctionArgContext)_localctx).SSTRING!=null?((FunctionArgContext)_localctx).SSTRING.getText():null) ); 
				}
				break;
			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(273);
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
		enterRule(_localctx, 38, RULE_exprs);
		int _la;
		try {
			int _alt;
			setState(368);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,33,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(277);
				match(ROOT);
				setState(278);
				match(DOT);
				setState(279);
				((ExprsContext)_localctx).expr = expr();
				 _localctx.ret.rootScoped = true; _localctx.ret.exprs.add( ((ExprsContext)_localctx).expr.ret ); 
				setState(287);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==DOT) {
					{
					{
					setState(281);
					match(DOT);
					setState(282);
					((ExprsContext)_localctx).expr = expr();
					 _localctx.ret.exprs.add( ((ExprsContext)_localctx).expr.ret ); 
					}
					}
					setState(289);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(291);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 395136991232L) != 0)) {
					{
					setState(290);
					((ExprsContext)_localctx).math = math();
					}
				}

				 if( ((ExprsContext)_localctx).math != null ) _localctx.ret.math = ((ExprsContext)_localctx).math.ret; 
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(295);
				((ExprsContext)_localctx).VAR_ID = match(VAR_ID);
				 _localctx.ret.varName = (((ExprsContext)_localctx).VAR_ID!=null?((ExprsContext)_localctx).VAR_ID.getText():null).substring( 1 ); 
				setState(303);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==DOT) {
					{
					{
					setState(297);
					match(DOT);
					setState(298);
					((ExprsContext)_localctx).expr = expr();
					 _localctx.ret.exprs.add( ((ExprsContext)_localctx).expr.ret ); 
					}
					}
					setState(305);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(307);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 395136991232L) != 0)) {
					{
					setState(306);
					((ExprsContext)_localctx).math = math();
					}
				}

				 if( ((ExprsContext)_localctx).math != null ) _localctx.ret.math = ((ExprsContext)_localctx).math.ret; 
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(310);
				((ExprsContext)_localctx).expr = expr();
				 _localctx.ret.exprs.add( ((ExprsContext)_localctx).expr.ret ); 
				setState(313);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 395136991232L) != 0)) {
					{
					setState(312);
					((ExprsContext)_localctx).math = math();
					}
				}

				 if( ((ExprsContext)_localctx).math != null ) _localctx.ret.math = ((ExprsContext)_localctx).math.ret; 
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(317);
				((ExprsContext)_localctx).expr = expr();
				 _localctx.ret.exprs.add( ((ExprsContext)_localctx).expr.ret ); 
				setState(320);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==DOT) {
					{
					setState(319);
					match(DOT);
					}
				}

				setState(322);
				((ExprsContext)_localctx).concatenation = concatenation();
				 _localctx.ret.concatenation = ((ExprsContext)_localctx).concatenation.ret; 
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(325);
				((ExprsContext)_localctx).expr = expr();
				 _localctx.ret.exprs.add( ((ExprsContext)_localctx).expr.ret ); 
				setState(333);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,28,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(327);
						match(DOT);
						setState(328);
						((ExprsContext)_localctx).expr = expr();

						        _localctx.ret.exprs.add( ((ExprsContext)_localctx).expr.ret );
						      
						}
						} 
					}
					setState(335);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,28,_ctx);
				}
				setState(336);
				match(DOT);
				setState(337);
				((ExprsContext)_localctx).expr = expr();
				 _localctx.ret.exprs.add( ((ExprsContext)_localctx).expr.ret ); 
				setState(340);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 395136991232L) != 0)) {
					{
					setState(339);
					((ExprsContext)_localctx).math = math();
					}
				}

				 if( ((ExprsContext)_localctx).math != null ) _localctx.ret.math = ((ExprsContext)_localctx).math.ret; 
				}
				break;
			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(344);
				((ExprsContext)_localctx).expr = expr();
				 _localctx.ret.exprs.add( ((ExprsContext)_localctx).expr.ret ); 
				setState(352);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,30,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(346);
						match(DOT);
						setState(347);
						((ExprsContext)_localctx).expr = expr();
						 _localctx.ret.exprs.add( ((ExprsContext)_localctx).expr.ret ); 
						}
						} 
					}
					setState(354);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,30,_ctx);
				}
				setState(355);
				match(DOT);
				setState(356);
				((ExprsContext)_localctx).expr = expr();
				 _localctx.ret.exprs.add( ((ExprsContext)_localctx).expr.ret ); 
				setState(359);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==DOT) {
					{
					setState(358);
					match(DOT);
					}
				}

				setState(361);
				((ExprsContext)_localctx).concatenation = concatenation();
				 _localctx.ret.concatenation = ((ExprsContext)_localctx).concatenation.ret; 
				setState(364);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 395136991232L) != 0)) {
					{
					setState(363);
					((ExprsContext)_localctx).math = math();
					}
				}

				 if( ((ExprsContext)_localctx).math != null ) _localctx.ret.math = ((ExprsContext)_localctx).math.ret; 
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
		enterRule(_localctx, 40, RULE_expr);
		int _la;
		try {
			setState(380);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,35,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				{
				setState(370);
				((ExprContext)_localctx).ID = match(ID);
				setState(371);
				match(LPAREN);
				setState(373);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 8521215115264L) != 0)) {
					{
					setState(372);
					((ExprContext)_localctx).functionArgs = functionArgs();
					}
				}

				setState(375);
				match(RPAREN);
				}
				 ((ExprContext)_localctx).ret =  new Expr((((ExprContext)_localctx).ID!=null?((ExprContext)_localctx).ID.getText():null), true, ((ExprContext)_localctx).functionArgs != null ? ((ExprContext)_localctx).functionArgs.ret : List.of() ); 
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(378);
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
		enterRule(_localctx, 42, RULE_concatenation);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(382);
			match(LBRACE);
			setState(383);
			((ConcatenationContext)_localctx).citems = citems();
			 ((ConcatenationContext)_localctx).ret =  new Concatenation( ((ConcatenationContext)_localctx).citems.ret ); 
			setState(385);
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
		public List<TerminalNode> PLUS() { return getTokens(TemplateGrammarExpression.PLUS); }
		public TerminalNode PLUS(int i) {
			return getToken(TemplateGrammarExpression.PLUS, i);
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
		enterRule(_localctx, 44, RULE_citems);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(387);
			((CitemsContext)_localctx).citem = citem();
			 _localctx.ret.add( ((CitemsContext)_localctx).citem.ret ); 
			setState(395);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==PLUS) {
				{
				{
				setState(389);
				match(PLUS);
				setState(390);
				((CitemsContext)_localctx).citem = citem();
				 _localctx.ret.add( ((CitemsContext)_localctx).citem.ret ); 
				}
				}
				setState(397);
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
		public Token firstId;
		public Token ID;
		public List<Token> ids = new ArrayList<Token>();
		public Token DSTRING;
		public Token SSTRING;
		public Token DECDIGITS;
		public Token FLOAT;
		public List<TerminalNode> ID() { return getTokens(TemplateGrammarExpression.ID); }
		public TerminalNode ID(int i) {
			return getToken(TemplateGrammarExpression.ID, i);
		}
		public List<TerminalNode> DOT() { return getTokens(TemplateGrammarExpression.DOT); }
		public TerminalNode DOT(int i) {
			return getToken(TemplateGrammarExpression.DOT, i);
		}
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
		enterRule(_localctx, 46, RULE_citem);
		int _la;
		try {
			setState(416);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,38,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(398);
				((CitemContext)_localctx).firstId = match(ID);
				setState(401); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(399);
					match(DOT);
					setState(400);
					((CitemContext)_localctx).ID = match(ID);
					((CitemContext)_localctx).ids.add(((CitemContext)_localctx).ID);
					}
					}
					setState(403); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( _la==DOT );

				        Exprs p = new Exprs();
				        p.exprs.add( new Expr( (((CitemContext)_localctx).firstId!=null?((CitemContext)_localctx).firstId.getText():null), false, List.of() ) );
				        for( Token id : ((CitemContext)_localctx).ids ) p.exprs.add( new Expr( id.getText(), false, List.of() ) );
				        ((CitemContext)_localctx).ret =  p;
				      
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(406);
				((CitemContext)_localctx).ID = match(ID);
				 ((CitemContext)_localctx).ret =  new Expr( (((CitemContext)_localctx).ID!=null?((CitemContext)_localctx).ID.getText():null), false, List.of() ); 
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(408);
				((CitemContext)_localctx).DSTRING = match(DSTRING);
				 ((CitemContext)_localctx).ret =  sdStringToString( (((CitemContext)_localctx).DSTRING!=null?((CitemContext)_localctx).DSTRING.getText():null) ); 
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(410);
				((CitemContext)_localctx).SSTRING = match(SSTRING);
				 ((CitemContext)_localctx).ret =  sdStringToString( (((CitemContext)_localctx).SSTRING!=null?((CitemContext)_localctx).SSTRING.getText():null) ); 
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(412);
				((CitemContext)_localctx).DECDIGITS = match(DECDIGITS);
				 ((CitemContext)_localctx).ret =  new NumericLiteral( (((CitemContext)_localctx).DECDIGITS!=null?((CitemContext)_localctx).DECDIGITS.getText():null) ); 
				}
				break;
			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(414);
				((CitemContext)_localctx).FLOAT = match(FLOAT);
				 ((CitemContext)_localctx).ret =  new NumericLiteral( (((CitemContext)_localctx).FLOAT!=null?((CitemContext)_localctx).FLOAT.getText():null) ); 
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
		enterRule(_localctx, 48, RULE_math);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(418);
			((MathContext)_localctx).mathOperation = mathOperation();
			setState(419);
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
		enterRule(_localctx, 50, RULE_number);
		try {
			setState(425);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case EOF:
			case IF:
			case THEN:
			case ELSE:
			case END:
			case AND:
			case OR:
			case EQ_KW:
			case NE_KW:
			case EQI_KW:
			case CONTAINS_KW:
			case EQEQ:
			case NEQ:
			case GE_OP:
			case LE_OP:
			case GT_OP:
			case LT_OP:
			case LBRACE:
			case RBRACE:
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
				setState(423);
				match(DECDIGITS);
				}
				break;
			case FLOAT:
				enterOuterAlt(_localctx, 3);
				{
				setState(424);
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
		enterRule(_localctx, 52, RULE_mathOperation);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(427);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 395136991232L) != 0)) ) {
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
		"\u0004\u00011\u01ae\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001\u0002"+
		"\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004\u0007\u0004\u0002"+
		"\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002\u0007\u0007\u0007\u0002"+
		"\b\u0007\b\u0002\t\u0007\t\u0002\n\u0007\n\u0002\u000b\u0007\u000b\u0002"+
		"\f\u0007\f\u0002\r\u0007\r\u0002\u000e\u0007\u000e\u0002\u000f\u0007\u000f"+
		"\u0002\u0010\u0007\u0010\u0002\u0011\u0007\u0011\u0002\u0012\u0007\u0012"+
		"\u0002\u0013\u0007\u0013\u0002\u0014\u0007\u0014\u0002\u0015\u0007\u0015"+
		"\u0002\u0016\u0007\u0016\u0002\u0017\u0007\u0017\u0002\u0018\u0007\u0018"+
		"\u0002\u0019\u0007\u0019\u0002\u001a\u0007\u001a\u0001\u0000\u0003\u0000"+
		"8\b\u0000\u0001\u0000\u0003\u0000;\b\u0000\u0001\u0000\u0001\u0000\u0001"+
		"\u0000\u0003\u0000@\b\u0000\u0001\u0000\u0003\u0000C\b\u0000\u0001\u0000"+
		"\u0003\u0000F\b\u0000\u0001\u0000\u0001\u0000\u0003\u0000J\b\u0000\u0001"+
		"\u0000\u0001\u0000\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001"+
		"\u0001\u0001\u0001\u0003\u0001T\b\u0001\u0001\u0001\u0001\u0001\u0001"+
		"\u0001\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001"+
		"\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001"+
		"\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0003"+
		"\u0002j\b\u0002\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001"+
		"\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001"+
		"\u0003\u0001\u0003\u0003\u0003x\b\u0003\u0001\u0004\u0001\u0004\u0001"+
		"\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0004\u0004\u0080\b\u0004\u000b"+
		"\u0004\f\u0004\u0081\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0001"+
		"\u0005\u0001\u0005\u0004\u0005\u008a\b\u0005\u000b\u0005\f\u0005\u008b"+
		"\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0006"+
		"\u0003\u0006\u0094\b\u0006\u0001\u0007\u0001\u0007\u0001\u0007\u0001\b"+
		"\u0001\b\u0001\b\u0001\b\u0001\b\u0001\b\u0005\b\u009f\b\b\n\b\f\b\u00a2"+
		"\t\b\u0001\t\u0001\t\u0001\t\u0001\t\u0001\t\u0001\t\u0005\t\u00aa\b\t"+
		"\n\t\f\t\u00ad\t\t\u0001\n\u0001\n\u0001\n\u0001\n\u0001\n\u0001\n\u0001"+
		"\n\u0003\n\u00b6\b\n\u0001\u000b\u0001\u000b\u0001\u000b\u0001\u000b\u0001"+
		"\u000b\u0001\u000b\u0001\u000b\u0001\u000b\u0001\u000b\u0001\u000b\u0001"+
		"\u000b\u0001\u000b\u0001\u000b\u0003\u000b\u00c5\b\u000b\u0001\f\u0001"+
		"\f\u0001\f\u0001\f\u0001\f\u0001\f\u0001\f\u0001\f\u0001\f\u0001\f\u0001"+
		"\f\u0001\f\u0001\f\u0001\f\u0001\f\u0001\f\u0003\f\u00d7\b\f\u0001\r\u0001"+
		"\r\u0001\r\u0001\r\u0001\u000e\u0001\u000e\u0001\u000e\u0001\u000e\u0001"+
		"\u000e\u0001\u000e\u0001\u000e\u0001\u000e\u0001\u000e\u0001\u000e\u0001"+
		"\u000e\u0001\u000e\u0001\u000e\u0001\u000e\u0003\u000e\u00eb\b\u000e\u0001"+
		"\u000f\u0003\u000f\u00ee\b\u000f\u0001\u000f\u0001\u000f\u0001\u0010\u0001"+
		"\u0010\u0001\u0010\u0001\u0010\u0003\u0010\u00f6\b\u0010\u0001\u0010\u0001"+
		"\u0010\u0001\u0010\u0001\u0011\u0001\u0011\u0001\u0011\u0001\u0011\u0001"+
		"\u0011\u0001\u0011\u0005\u0011\u0101\b\u0011\n\u0011\f\u0011\u0104\t\u0011"+
		"\u0001\u0012\u0001\u0012\u0001\u0012\u0001\u0012\u0001\u0012\u0001\u0012"+
		"\u0001\u0012\u0001\u0012\u0001\u0012\u0001\u0012\u0001\u0012\u0001\u0012"+
		"\u0001\u0012\u0001\u0012\u0003\u0012\u0114\b\u0012\u0001\u0013\u0001\u0013"+
		"\u0001\u0013\u0001\u0013\u0001\u0013\u0001\u0013\u0001\u0013\u0001\u0013"+
		"\u0005\u0013\u011e\b\u0013\n\u0013\f\u0013\u0121\t\u0013\u0001\u0013\u0003"+
		"\u0013\u0124\b\u0013\u0001\u0013\u0001\u0013\u0001\u0013\u0001\u0013\u0001"+
		"\u0013\u0001\u0013\u0001\u0013\u0001\u0013\u0005\u0013\u012e\b\u0013\n"+
		"\u0013\f\u0013\u0131\t\u0013\u0001\u0013\u0003\u0013\u0134\b\u0013\u0001"+
		"\u0013\u0001\u0013\u0001\u0013\u0001\u0013\u0003\u0013\u013a\b\u0013\u0001"+
		"\u0013\u0001\u0013\u0001\u0013\u0001\u0013\u0001\u0013\u0003\u0013\u0141"+
		"\b\u0013\u0001\u0013\u0001\u0013\u0001\u0013\u0001\u0013\u0001\u0013\u0001"+
		"\u0013\u0001\u0013\u0001\u0013\u0001\u0013\u0005\u0013\u014c\b\u0013\n"+
		"\u0013\f\u0013\u014f\t\u0013\u0001\u0013\u0001\u0013\u0001\u0013\u0001"+
		"\u0013\u0003\u0013\u0155\b\u0013\u0001\u0013\u0001\u0013\u0001\u0013\u0001"+
		"\u0013\u0001\u0013\u0001\u0013\u0001\u0013\u0001\u0013\u0005\u0013\u015f"+
		"\b\u0013\n\u0013\f\u0013\u0162\t\u0013\u0001\u0013\u0001\u0013\u0001\u0013"+
		"\u0001\u0013\u0003\u0013\u0168\b\u0013\u0001\u0013\u0001\u0013\u0001\u0013"+
		"\u0003\u0013\u016d\b\u0013\u0001\u0013\u0001\u0013\u0003\u0013\u0171\b"+
		"\u0013\u0001\u0014\u0001\u0014\u0001\u0014\u0003\u0014\u0176\b\u0014\u0001"+
		"\u0014\u0001\u0014\u0001\u0014\u0001\u0014\u0001\u0014\u0003\u0014\u017d"+
		"\b\u0014\u0001\u0015\u0001\u0015\u0001\u0015\u0001\u0015\u0001\u0015\u0001"+
		"\u0016\u0001\u0016\u0001\u0016\u0001\u0016\u0001\u0016\u0001\u0016\u0005"+
		"\u0016\u018a\b\u0016\n\u0016\f\u0016\u018d\t\u0016\u0001\u0017\u0001\u0017"+
		"\u0001\u0017\u0004\u0017\u0192\b\u0017\u000b\u0017\f\u0017\u0193\u0001"+
		"\u0017\u0001\u0017\u0001\u0017\u0001\u0017\u0001\u0017\u0001\u0017\u0001"+
		"\u0017\u0001\u0017\u0001\u0017\u0001\u0017\u0001\u0017\u0003\u0017\u01a1"+
		"\b\u0017\u0001\u0018\u0001\u0018\u0001\u0018\u0001\u0018\u0001\u0019\u0001"+
		"\u0019\u0001\u0019\u0003\u0019\u01aa\b\u0019\u0001\u001a\u0001\u001a\u0001"+
		"\u001a\u0000\u0000\u001b\u0000\u0002\u0004\u0006\b\n\f\u000e\u0010\u0012"+
		"\u0014\u0016\u0018\u001a\u001c\u001e \"$&(*,.024\u0000\u0003\u0001\u0000"+
		"\u0007\b\u0001\u0000\t\u0012\u0002\u0000\"$&&\u01d7\u00007\u0001\u0000"+
		"\u0000\u0000\u0002M\u0001\u0000\u0000\u0000\u0004i\u0001\u0000\u0000\u0000"+
		"\u0006w\u0001\u0000\u0000\u0000\by\u0001\u0000\u0000\u0000\n\u0083\u0001"+
		"\u0000\u0000\u0000\f\u0093\u0001\u0000\u0000\u0000\u000e\u0095\u0001\u0000"+
		"\u0000\u0000\u0010\u0098\u0001\u0000\u0000\u0000\u0012\u00a3\u0001\u0000"+
		"\u0000\u0000\u0014\u00b5\u0001\u0000\u0000\u0000\u0016\u00c4\u0001\u0000"+
		"\u0000\u0000\u0018\u00d6\u0001\u0000\u0000\u0000\u001a\u00d8\u0001\u0000"+
		"\u0000\u0000\u001c\u00ea\u0001\u0000\u0000\u0000\u001e\u00ed\u0001\u0000"+
		"\u0000\u0000 \u00f1\u0001\u0000\u0000\u0000\"\u00fa\u0001\u0000\u0000"+
		"\u0000$\u0113\u0001\u0000\u0000\u0000&\u0170\u0001\u0000\u0000\u0000("+
		"\u017c\u0001\u0000\u0000\u0000*\u017e\u0001\u0000\u0000\u0000,\u0183\u0001"+
		"\u0000\u0000\u0000.\u01a0\u0001\u0000\u0000\u00000\u01a2\u0001\u0000\u0000"+
		"\u00002\u01a9\u0001\u0000\u0000\u00004\u01ab\u0001\u0000\u0000\u00006"+
		"8\u0005\u0015\u0000\u000076\u0001\u0000\u0000\u000078\u0001\u0000\u0000"+
		"\u00008:\u0001\u0000\u0000\u00009;\u0005-\u0000\u0000:9\u0001\u0000\u0000"+
		"\u0000:;\u0001\u0000\u0000\u0000;?\u0001\u0000\u0000\u0000<@\u0003\u0002"+
		"\u0001\u0000=@\u0003\u0006\u0003\u0000>@\u0003\f\u0006\u0000?<\u0001\u0000"+
		"\u0000\u0000?=\u0001\u0000\u0000\u0000?>\u0001\u0000\u0000\u0000@B\u0001"+
		"\u0000\u0000\u0000AC\u0003\u001a\r\u0000BA\u0001\u0000\u0000\u0000BC\u0001"+
		"\u0000\u0000\u0000CE\u0001\u0000\u0000\u0000DF\u0003 \u0010\u0000ED\u0001"+
		"\u0000\u0000\u0000EF\u0001\u0000\u0000\u0000FI\u0001\u0000\u0000\u0000"+
		"GH\u0005\u0001\u0000\u0000HJ\u0003\u000e\u0007\u0000IG\u0001\u0000\u0000"+
		"\u0000IJ\u0001\u0000\u0000\u0000JK\u0001\u0000\u0000\u0000KL\u0006\u0000"+
		"\uffff\uffff\u0000L\u0001\u0001\u0000\u0000\u0000MN\u0005\u0001\u0000"+
		"\u0000NO\u0003\u000e\u0007\u0000OP\u0005\u0002\u0000\u0000PS\u0003\u0004"+
		"\u0002\u0000QR\u0005\u0003\u0000\u0000RT\u0003\u0004\u0002\u0000SQ\u0001"+
		"\u0000\u0000\u0000ST\u0001\u0000\u0000\u0000TU\u0001\u0000\u0000\u0000"+
		"UV\u0005\u0004\u0000\u0000VW\u0006\u0001\uffff\uffff\u0000W\u0003\u0001"+
		"\u0000\u0000\u0000XY\u0003&\u0013\u0000YZ\u0006\u0002\uffff\uffff\u0000"+
		"Zj\u0001\u0000\u0000\u0000[\\\u0005(\u0000\u0000\\j\u0006\u0002\uffff"+
		"\uffff\u0000]^\u0005\'\u0000\u0000^j\u0006\u0002\uffff\uffff\u0000_`\u0005"+
		")\u0000\u0000`j\u0006\u0002\uffff\uffff\u0000ab\u0005&\u0000\u0000bc\u0005"+
		")\u0000\u0000cj\u0006\u0002\uffff\uffff\u0000de\u0005*\u0000\u0000ej\u0006"+
		"\u0002\uffff\uffff\u0000fg\u0005&\u0000\u0000gh\u0005*\u0000\u0000hj\u0006"+
		"\u0002\uffff\uffff\u0000iX\u0001\u0000\u0000\u0000i[\u0001\u0000\u0000"+
		"\u0000i]\u0001\u0000\u0000\u0000i_\u0001\u0000\u0000\u0000ia\u0001\u0000"+
		"\u0000\u0000id\u0001\u0000\u0000\u0000if\u0001\u0000\u0000\u0000j\u0005"+
		"\u0001\u0000\u0000\u0000kl\u0003&\u0013\u0000lm\u0005\u0018\u0000\u0000"+
		"mn\u0003\b\u0004\u0000no\u0005\u0019\u0000\u0000op\u0006\u0003\uffff\uffff"+
		"\u0000px\u0001\u0000\u0000\u0000qr\u0003&\u0013\u0000rs\u0005\u0018\u0000"+
		"\u0000st\u0003\f\u0006\u0000tu\u0005\u0019\u0000\u0000uv\u0006\u0003\uffff"+
		"\uffff\u0000vx\u0001\u0000\u0000\u0000wk\u0001\u0000\u0000\u0000wq\u0001"+
		"\u0000\u0000\u0000x\u0007\u0001\u0000\u0000\u0000yz\u0003.\u0017\u0000"+
		"z\u007f\u0006\u0004\uffff\uffff\u0000{|\u0005%\u0000\u0000|}\u0003.\u0017"+
		"\u0000}~\u0006\u0004\uffff\uffff\u0000~\u0080\u0001\u0000\u0000\u0000"+
		"\u007f{\u0001\u0000\u0000\u0000\u0080\u0081\u0001\u0000\u0000\u0000\u0081"+
		"\u007f\u0001\u0000\u0000\u0000\u0081\u0082\u0001\u0000\u0000\u0000\u0082"+
		"\t\u0001\u0000\u0000\u0000\u0083\u0084\u0003.\u0017\u0000\u0084\u0089"+
		"\u0006\u0005\uffff\uffff\u0000\u0085\u0086\u0005%\u0000\u0000\u0086\u0087"+
		"\u0003.\u0017\u0000\u0087\u0088\u0006\u0005\uffff\uffff\u0000\u0088\u008a"+
		"\u0001\u0000\u0000\u0000\u0089\u0085\u0001\u0000\u0000\u0000\u008a\u008b"+
		"\u0001\u0000\u0000\u0000\u008b\u0089\u0001\u0000\u0000\u0000\u008b\u008c"+
		"\u0001\u0000\u0000\u0000\u008c\u000b\u0001\u0000\u0000\u0000\u008d\u008e"+
		"\u0003\n\u0005\u0000\u008e\u008f\u0006\u0006\uffff\uffff\u0000\u008f\u0094"+
		"\u0001\u0000\u0000\u0000\u0090\u0091\u0003&\u0013\u0000\u0091\u0092\u0006"+
		"\u0006\uffff\uffff\u0000\u0092\u0094\u0001\u0000\u0000\u0000\u0093\u008d"+
		"\u0001\u0000\u0000\u0000\u0093\u0090\u0001\u0000\u0000\u0000\u0094\r\u0001"+
		"\u0000\u0000\u0000\u0095\u0096\u0003\u0010\b\u0000\u0096\u0097\u0006\u0007"+
		"\uffff\uffff\u0000\u0097\u000f\u0001\u0000\u0000\u0000\u0098\u0099\u0003"+
		"\u0012\t\u0000\u0099\u00a0\u0006\b\uffff\uffff\u0000\u009a\u009b\u0005"+
		"\u0006\u0000\u0000\u009b\u009c\u0003\u0012\t\u0000\u009c\u009d\u0006\b"+
		"\uffff\uffff\u0000\u009d\u009f\u0001\u0000\u0000\u0000\u009e\u009a\u0001"+
		"\u0000\u0000\u0000\u009f\u00a2\u0001\u0000\u0000\u0000\u00a0\u009e\u0001"+
		"\u0000\u0000\u0000\u00a0\u00a1\u0001\u0000\u0000\u0000\u00a1\u0011\u0001"+
		"\u0000\u0000\u0000\u00a2\u00a0\u0001\u0000\u0000\u0000\u00a3\u00a4\u0003"+
		"\u0014\n\u0000\u00a4\u00ab\u0006\t\uffff\uffff\u0000\u00a5\u00a6\u0005"+
		"\u0005\u0000\u0000\u00a6\u00a7\u0003\u0014\n\u0000\u00a7\u00a8\u0006\t"+
		"\uffff\uffff\u0000\u00a8\u00aa\u0001\u0000\u0000\u0000\u00a9\u00a5\u0001"+
		"\u0000\u0000\u0000\u00aa\u00ad\u0001\u0000\u0000\u0000\u00ab\u00a9\u0001"+
		"\u0000\u0000\u0000\u00ab\u00ac\u0001\u0000\u0000\u0000\u00ac\u0013\u0001"+
		"\u0000\u0000\u0000\u00ad\u00ab\u0001\u0000\u0000\u0000\u00ae\u00af\u0007"+
		"\u0000\u0000\u0000\u00af\u00b0\u0003\u0014\n\u0000\u00b0\u00b1\u0006\n"+
		"\uffff\uffff\u0000\u00b1\u00b6\u0001\u0000\u0000\u0000\u00b2\u00b3\u0003"+
		"\u0016\u000b\u0000\u00b3\u00b4\u0006\n\uffff\uffff\u0000\u00b4\u00b6\u0001"+
		"\u0000\u0000\u0000\u00b5\u00ae\u0001\u0000\u0000\u0000\u00b5\u00b2\u0001"+
		"\u0000\u0000\u0000\u00b6\u0015\u0001\u0000\u0000\u0000\u00b7\u00b8\u0005"+
		"\u001b\u0000\u0000\u00b8\u00b9\u0003\u000e\u0007\u0000\u00b9\u00ba\u0005"+
		"\u001c\u0000\u0000\u00ba\u00bb\u0006\u000b\uffff\uffff\u0000\u00bb\u00c5"+
		"\u0001\u0000\u0000\u0000\u00bc\u00bd\u0003&\u0013\u0000\u00bd\u00be\u0007"+
		"\u0001\u0000\u0000\u00be\u00bf\u0003\u0018\f\u0000\u00bf\u00c0\u0006\u000b"+
		"\uffff\uffff\u0000\u00c0\u00c5\u0001\u0000\u0000\u0000\u00c1\u00c2\u0003"+
		"&\u0013\u0000\u00c2\u00c3\u0006\u000b\uffff\uffff\u0000\u00c3\u00c5\u0001"+
		"\u0000\u0000\u0000\u00c4\u00b7\u0001\u0000\u0000\u0000\u00c4\u00bc\u0001"+
		"\u0000\u0000\u0000\u00c4\u00c1\u0001\u0000\u0000\u0000\u00c5\u0017\u0001"+
		"\u0000\u0000\u0000\u00c6\u00c7\u0005(\u0000\u0000\u00c7\u00d7\u0006\f"+
		"\uffff\uffff\u0000\u00c8\u00c9\u0005\'\u0000\u0000\u00c9\u00d7\u0006\f"+
		"\uffff\uffff\u0000\u00ca\u00cb\u0005)\u0000\u0000\u00cb\u00d7\u0006\f"+
		"\uffff\uffff\u0000\u00cc\u00cd\u0005&\u0000\u0000\u00cd\u00ce\u0005)\u0000"+
		"\u0000\u00ce\u00d7\u0006\f\uffff\uffff\u0000\u00cf\u00d0\u0005*\u0000"+
		"\u0000\u00d0\u00d7\u0006\f\uffff\uffff\u0000\u00d1\u00d2\u0005&\u0000"+
		"\u0000\u00d2\u00d3\u0005*\u0000\u0000\u00d3\u00d7\u0006\f\uffff\uffff"+
		"\u0000\u00d4\u00d5\u0005+\u0000\u0000\u00d5\u00d7\u0006\f\uffff\uffff"+
		"\u0000\u00d6\u00c6\u0001\u0000\u0000\u0000\u00d6\u00c8\u0001\u0000\u0000"+
		"\u0000\u00d6\u00ca\u0001\u0000\u0000\u0000\u00d6\u00cc\u0001\u0000\u0000"+
		"\u0000\u00d6\u00cf\u0001\u0000\u0000\u0000\u00d6\u00d1\u0001\u0000\u0000"+
		"\u0000\u00d6\u00d4\u0001\u0000\u0000\u0000\u00d7\u0019\u0001\u0000\u0000"+
		"\u0000\u00d8\u00d9\u0005\u001f\u0000\u0000\u00d9\u00da\u0003\u001c\u000e"+
		"\u0000\u00da\u00db\u0006\r\uffff\uffff\u0000\u00db\u001b\u0001\u0000\u0000"+
		"\u0000\u00dc\u00dd\u0005(\u0000\u0000\u00dd\u00eb\u0006\u000e\uffff\uffff"+
		"\u0000\u00de\u00df\u0005\'\u0000\u0000\u00df\u00eb\u0006\u000e\uffff\uffff"+
		"\u0000\u00e0\u00e1\u0003\u001e\u000f\u0000\u00e1\u00e2\u0006\u000e\uffff"+
		"\uffff\u0000\u00e2\u00eb\u0001\u0000\u0000\u0000\u00e3\u00e4\u0005*\u0000"+
		"\u0000\u00e4\u00eb\u0006\u000e\uffff\uffff\u0000\u00e5\u00e6\u0005+\u0000"+
		"\u0000\u00e6\u00eb\u0006\u000e\uffff\uffff\u0000\u00e7\u00e8\u0005\u001d"+
		"\u0000\u0000\u00e8\u00e9\u0005\u001e\u0000\u0000\u00e9\u00eb\u0006\u000e"+
		"\uffff\uffff\u0000\u00ea\u00dc\u0001\u0000\u0000\u0000\u00ea\u00de\u0001"+
		"\u0000\u0000\u0000\u00ea\u00e0\u0001\u0000\u0000\u0000\u00ea\u00e3\u0001"+
		"\u0000\u0000\u0000\u00ea\u00e5\u0001\u0000\u0000\u0000\u00ea\u00e7\u0001"+
		"\u0000\u0000\u0000\u00eb\u001d\u0001\u0000\u0000\u0000\u00ec\u00ee\u0005"+
		"&\u0000\u0000\u00ed\u00ec\u0001\u0000\u0000\u0000\u00ed\u00ee\u0001\u0000"+
		"\u0000\u0000\u00ee\u00ef\u0001\u0000\u0000\u0000\u00ef\u00f0\u0005)\u0000"+
		"\u0000\u00f0\u001f\u0001\u0000\u0000\u0000\u00f1\u00f2\u0005 \u0000\u0000"+
		"\u00f2\u00f3\u0005,\u0000\u0000\u00f3\u00f5\u0005\u001b\u0000\u0000\u00f4"+
		"\u00f6\u0003\"\u0011\u0000\u00f5\u00f4\u0001\u0000\u0000\u0000\u00f5\u00f6"+
		"\u0001\u0000\u0000\u0000\u00f6\u00f7\u0001\u0000\u0000\u0000\u00f7\u00f8"+
		"\u0005\u001c\u0000\u0000\u00f8\u00f9\u0006\u0010\uffff\uffff\u0000\u00f9"+
		"!\u0001\u0000\u0000\u0000\u00fa\u00fb\u0003$\u0012\u0000\u00fb\u0102\u0006"+
		"\u0011\uffff\uffff\u0000\u00fc\u00fd\u0005!\u0000\u0000\u00fd\u00fe\u0003"+
		"$\u0012\u0000\u00fe\u00ff\u0006\u0011\uffff\uffff\u0000\u00ff\u0101\u0001"+
		"\u0000\u0000\u0000\u0100\u00fc\u0001\u0000\u0000\u0000\u0101\u0104\u0001"+
		"\u0000\u0000\u0000\u0102\u0100\u0001\u0000\u0000\u0000\u0102\u0103\u0001"+
		"\u0000\u0000\u0000\u0103#\u0001\u0000\u0000\u0000\u0104\u0102\u0001\u0000"+
		"\u0000\u0000\u0105\u0106\u0005)\u0000\u0000\u0106\u0114\u0006\u0012\uffff"+
		"\uffff\u0000\u0107\u0108\u0005&\u0000\u0000\u0108\u0109\u0005)\u0000\u0000"+
		"\u0109\u0114\u0006\u0012\uffff\uffff\u0000\u010a\u010b\u0005*\u0000\u0000"+
		"\u010b\u0114\u0006\u0012\uffff\uffff\u0000\u010c\u010d\u0005&\u0000\u0000"+
		"\u010d\u010e\u0005*\u0000\u0000\u010e\u0114\u0006\u0012\uffff\uffff\u0000"+
		"\u010f\u0110\u0005(\u0000\u0000\u0110\u0114\u0006\u0012\uffff\uffff\u0000"+
		"\u0111\u0112\u0005\'\u0000\u0000\u0112\u0114\u0006\u0012\uffff\uffff\u0000"+
		"\u0113\u0105\u0001\u0000\u0000\u0000\u0113\u0107\u0001\u0000\u0000\u0000"+
		"\u0113\u010a\u0001\u0000\u0000\u0000\u0113\u010c\u0001\u0000\u0000\u0000"+
		"\u0113\u010f\u0001\u0000\u0000\u0000\u0113\u0111\u0001\u0000\u0000\u0000"+
		"\u0114%\u0001\u0000\u0000\u0000\u0115\u0116\u0005\u0014\u0000\u0000\u0116"+
		"\u0117\u0005\u001a\u0000\u0000\u0117\u0118\u0003(\u0014\u0000\u0118\u011f"+
		"\u0006\u0013\uffff\uffff\u0000\u0119\u011a\u0005\u001a\u0000\u0000\u011a"+
		"\u011b\u0003(\u0014\u0000\u011b\u011c\u0006\u0013\uffff\uffff\u0000\u011c"+
		"\u011e\u0001\u0000\u0000\u0000\u011d\u0119\u0001\u0000\u0000\u0000\u011e"+
		"\u0121\u0001\u0000\u0000\u0000\u011f\u011d\u0001\u0000\u0000\u0000\u011f"+
		"\u0120\u0001\u0000\u0000\u0000\u0120\u0123\u0001\u0000\u0000\u0000\u0121"+
		"\u011f\u0001\u0000\u0000\u0000\u0122\u0124\u00030\u0018\u0000\u0123\u0122"+
		"\u0001\u0000\u0000\u0000\u0123\u0124\u0001\u0000\u0000\u0000\u0124\u0125"+
		"\u0001\u0000\u0000\u0000\u0125\u0126\u0006\u0013\uffff\uffff\u0000\u0126"+
		"\u0171\u0001\u0000\u0000\u0000\u0127\u0128\u0005\u0013\u0000\u0000\u0128"+
		"\u012f\u0006\u0013\uffff\uffff\u0000\u0129\u012a\u0005\u001a\u0000\u0000"+
		"\u012a\u012b\u0003(\u0014\u0000\u012b\u012c\u0006\u0013\uffff\uffff\u0000"+
		"\u012c\u012e\u0001\u0000\u0000\u0000\u012d\u0129\u0001\u0000\u0000\u0000"+
		"\u012e\u0131\u0001\u0000\u0000\u0000\u012f\u012d\u0001\u0000\u0000\u0000"+
		"\u012f\u0130\u0001\u0000\u0000\u0000\u0130\u0133\u0001\u0000\u0000\u0000"+
		"\u0131\u012f\u0001\u0000\u0000\u0000\u0132\u0134\u00030\u0018\u0000\u0133"+
		"\u0132\u0001\u0000\u0000\u0000\u0133\u0134\u0001\u0000\u0000\u0000\u0134"+
		"\u0135\u0001\u0000\u0000\u0000\u0135\u0171\u0006\u0013\uffff\uffff\u0000"+
		"\u0136\u0137\u0003(\u0014\u0000\u0137\u0139\u0006\u0013\uffff\uffff\u0000"+
		"\u0138\u013a\u00030\u0018\u0000\u0139\u0138\u0001\u0000\u0000\u0000\u0139"+
		"\u013a\u0001\u0000\u0000\u0000\u013a\u013b\u0001\u0000\u0000\u0000\u013b"+
		"\u013c\u0006\u0013\uffff\uffff\u0000\u013c\u0171\u0001\u0000\u0000\u0000"+
		"\u013d\u013e\u0003(\u0014\u0000\u013e\u0140\u0006\u0013\uffff\uffff\u0000"+
		"\u013f\u0141\u0005\u001a\u0000\u0000\u0140\u013f\u0001\u0000\u0000\u0000"+
		"\u0140\u0141\u0001\u0000\u0000\u0000\u0141\u0142\u0001\u0000\u0000\u0000"+
		"\u0142\u0143\u0003*\u0015\u0000\u0143\u0144\u0006\u0013\uffff\uffff\u0000"+
		"\u0144\u0171\u0001\u0000\u0000\u0000\u0145\u0146\u0003(\u0014\u0000\u0146"+
		"\u014d\u0006\u0013\uffff\uffff\u0000\u0147\u0148\u0005\u001a\u0000\u0000"+
		"\u0148\u0149\u0003(\u0014\u0000\u0149\u014a\u0006\u0013\uffff\uffff\u0000"+
		"\u014a\u014c\u0001\u0000\u0000\u0000\u014b\u0147\u0001\u0000\u0000\u0000"+
		"\u014c\u014f\u0001\u0000\u0000\u0000\u014d\u014b\u0001\u0000\u0000\u0000"+
		"\u014d\u014e\u0001\u0000\u0000\u0000\u014e\u0150\u0001\u0000\u0000\u0000"+
		"\u014f\u014d\u0001\u0000\u0000\u0000\u0150\u0151\u0005\u001a\u0000\u0000"+
		"\u0151\u0152\u0003(\u0014\u0000\u0152\u0154\u0006\u0013\uffff\uffff\u0000"+
		"\u0153\u0155\u00030\u0018\u0000\u0154\u0153\u0001\u0000\u0000\u0000\u0154"+
		"\u0155\u0001\u0000\u0000\u0000\u0155\u0156\u0001\u0000\u0000\u0000\u0156"+
		"\u0157\u0006\u0013\uffff\uffff\u0000\u0157\u0171\u0001\u0000\u0000\u0000"+
		"\u0158\u0159\u0003(\u0014\u0000\u0159\u0160\u0006\u0013\uffff\uffff\u0000"+
		"\u015a\u015b\u0005\u001a\u0000\u0000\u015b\u015c\u0003(\u0014\u0000\u015c"+
		"\u015d\u0006\u0013\uffff\uffff\u0000\u015d\u015f\u0001\u0000\u0000\u0000"+
		"\u015e\u015a\u0001\u0000\u0000\u0000\u015f\u0162\u0001\u0000\u0000\u0000"+
		"\u0160\u015e\u0001\u0000\u0000\u0000\u0160\u0161\u0001\u0000\u0000\u0000"+
		"\u0161\u0163\u0001\u0000\u0000\u0000\u0162\u0160\u0001\u0000\u0000\u0000"+
		"\u0163\u0164\u0005\u001a\u0000\u0000\u0164\u0165\u0003(\u0014\u0000\u0165"+
		"\u0167\u0006\u0013\uffff\uffff\u0000\u0166\u0168\u0005\u001a\u0000\u0000"+
		"\u0167\u0166\u0001\u0000\u0000\u0000\u0167\u0168\u0001\u0000\u0000\u0000"+
		"\u0168\u0169\u0001\u0000\u0000\u0000\u0169\u016a\u0003*\u0015\u0000\u016a"+
		"\u016c\u0006\u0013\uffff\uffff\u0000\u016b\u016d\u00030\u0018\u0000\u016c"+
		"\u016b\u0001\u0000\u0000\u0000\u016c\u016d\u0001\u0000\u0000\u0000\u016d"+
		"\u016e\u0001\u0000\u0000\u0000\u016e\u016f\u0006\u0013\uffff\uffff\u0000"+
		"\u016f\u0171\u0001\u0000\u0000\u0000\u0170\u0115\u0001\u0000\u0000\u0000"+
		"\u0170\u0127\u0001\u0000\u0000\u0000\u0170\u0136\u0001\u0000\u0000\u0000"+
		"\u0170\u013d\u0001\u0000\u0000\u0000\u0170\u0145\u0001\u0000\u0000\u0000"+
		"\u0170\u0158\u0001\u0000\u0000\u0000\u0171\'\u0001\u0000\u0000\u0000\u0172"+
		"\u0173\u0005,\u0000\u0000\u0173\u0175\u0005\u001b\u0000\u0000\u0174\u0176"+
		"\u0003\"\u0011\u0000\u0175\u0174\u0001\u0000\u0000\u0000\u0175\u0176\u0001"+
		"\u0000\u0000\u0000\u0176\u0177\u0001\u0000\u0000\u0000\u0177\u0178\u0005"+
		"\u001c\u0000\u0000\u0178\u0179\u0001\u0000\u0000\u0000\u0179\u017d\u0006"+
		"\u0014\uffff\uffff\u0000\u017a\u017b\u0005,\u0000\u0000\u017b\u017d\u0006"+
		"\u0014\uffff\uffff\u0000\u017c\u0172\u0001\u0000\u0000\u0000\u017c\u017a"+
		"\u0001\u0000\u0000\u0000\u017d)\u0001\u0000\u0000\u0000\u017e\u017f\u0005"+
		"\u0018\u0000\u0000\u017f\u0180\u0003,\u0016\u0000\u0180\u0181\u0006\u0015"+
		"\uffff\uffff\u0000\u0181\u0182\u0005\u0019\u0000\u0000\u0182+\u0001\u0000"+
		"\u0000\u0000\u0183\u0184\u0003.\u0017\u0000\u0184\u018b\u0006\u0016\uffff"+
		"\uffff\u0000\u0185\u0186\u0005%\u0000\u0000\u0186\u0187\u0003.\u0017\u0000"+
		"\u0187\u0188\u0006\u0016\uffff\uffff\u0000\u0188\u018a\u0001\u0000\u0000"+
		"\u0000\u0189\u0185\u0001\u0000\u0000\u0000\u018a\u018d\u0001\u0000\u0000"+
		"\u0000\u018b\u0189\u0001\u0000\u0000\u0000\u018b\u018c\u0001\u0000\u0000"+
		"\u0000\u018c-\u0001\u0000\u0000\u0000\u018d\u018b\u0001\u0000\u0000\u0000"+
		"\u018e\u0191\u0005,\u0000\u0000\u018f\u0190\u0005\u001a\u0000\u0000\u0190"+
		"\u0192\u0005,\u0000\u0000\u0191\u018f\u0001\u0000\u0000\u0000\u0192\u0193"+
		"\u0001\u0000\u0000\u0000\u0193\u0191\u0001\u0000\u0000\u0000\u0193\u0194"+
		"\u0001\u0000\u0000\u0000\u0194\u0195\u0001\u0000\u0000\u0000\u0195\u01a1"+
		"\u0006\u0017\uffff\uffff\u0000\u0196\u0197\u0005,\u0000\u0000\u0197\u01a1"+
		"\u0006\u0017\uffff\uffff\u0000\u0198\u0199\u0005\'\u0000\u0000\u0199\u01a1"+
		"\u0006\u0017\uffff\uffff\u0000\u019a\u019b\u0005(\u0000\u0000\u019b\u01a1"+
		"\u0006\u0017\uffff\uffff\u0000\u019c\u019d\u0005)\u0000\u0000\u019d\u01a1"+
		"\u0006\u0017\uffff\uffff\u0000\u019e\u019f\u0005*\u0000\u0000\u019f\u01a1"+
		"\u0006\u0017\uffff\uffff\u0000\u01a0\u018e\u0001\u0000\u0000\u0000\u01a0"+
		"\u0196\u0001\u0000\u0000\u0000\u01a0\u0198\u0001\u0000\u0000\u0000\u01a0"+
		"\u019a\u0001\u0000\u0000\u0000\u01a0\u019c\u0001\u0000\u0000\u0000\u01a0"+
		"\u019e\u0001\u0000\u0000\u0000\u01a1/\u0001\u0000\u0000\u0000\u01a2\u01a3"+
		"\u00034\u001a\u0000\u01a3\u01a4\u00032\u0019\u0000\u01a4\u01a5\u0006\u0018"+
		"\uffff\uffff\u0000\u01a51\u0001\u0000\u0000\u0000\u01a6\u01aa\u0001\u0000"+
		"\u0000\u0000\u01a7\u01aa\u0005)\u0000\u0000\u01a8\u01aa\u0005*\u0000\u0000"+
		"\u01a9\u01a6\u0001\u0000\u0000\u0000\u01a9\u01a7\u0001\u0000\u0000\u0000"+
		"\u01a9\u01a8\u0001\u0000\u0000\u0000\u01aa3\u0001\u0000\u0000\u0000\u01ab"+
		"\u01ac\u0007\u0002\u0000\u0000\u01ac5\u0001\u0000\u0000\u0000(7:?BEIS"+
		"iw\u0081\u008b\u0093\u00a0\u00ab\u00b5\u00c4\u00d6\u00ea\u00ed\u00f5\u0102"+
		"\u0113\u011f\u0123\u012f\u0133\u0139\u0140\u014d\u0154\u0160\u0167\u016c"+
		"\u0170\u0175\u017c\u018b\u0193\u01a0\u01a9";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}