// Generated from JPath.g4 by ANTLR 4.8

package oap.jpath;

import java.util.List;
import java.lang.Number;
import java.util.ArrayList;
import oap.util.Pair;
import static oap.util.Pair.__;

import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class JPathParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.8", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, T__2=3, T__3=4, T__4=5, T__5=6, T__6=7, T__7=8, Identifier=9, 
		DecimalIntegerLiteral=10, StringLiteral=11, SPACE=12;
	public static final int
		RULE_expr = 0, RULE_path = 1, RULE_variableDeclaratorId = 2, RULE_array = 3, 
		RULE_method = 4, RULE_methodParameters = 5, RULE_methodParameter = 6, 
		RULE_identifier = 7;
	private static String[] makeRuleNames() {
		return new String[] {
			"expr", "path", "variableDeclaratorId", "array", "method", "methodParameters", 
			"methodParameter", "identifier"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "'var'", "':'", "'.'", "'['", "']'", "'('", "')'", "','"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, null, null, null, null, null, null, null, null, "Identifier", "DecimalIntegerLiteral", 
			"StringLiteral", "SPACE"
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
	public String getGrammarFileName() { return "JPath.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public JPathParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	public static class ExprContext extends ParserRuleContext {
		public Expression expression;
		public VariableDeclaratorIdContext f;
		public PathContext n;
		public VariableDeclaratorIdContext variableDeclaratorId() {
			return getRuleContext(VariableDeclaratorIdContext.class,0);
		}
		public List<PathContext> path() {
			return getRuleContexts(PathContext.class);
		}
		public PathContext path(int i) {
			return getRuleContext(PathContext.class,i);
		}
		public ExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_expr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JPathListener ) ((JPathListener)listener).enterExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JPathListener ) ((JPathListener)listener).exitExpr(this);
		}
	}

	public final ExprContext expr() throws RecognitionException {
		ExprContext _localctx = new ExprContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_expr);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(16);
			match(T__0);
			((ExprContext)_localctx).expression =  new Expression(IdentifierType.VARIABLE);
			setState(18);
			match(T__1);
			setState(19);
			((ExprContext)_localctx).f = variableDeclaratorId();
			_localctx.expression.path.add(new PathNodeField((((ExprContext)_localctx).f!=null?_input.getText(((ExprContext)_localctx).f.start,((ExprContext)_localctx).f.stop):null)));
			setState(27);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__2) {
				{
				{
				setState(21);
				match(T__2);
				setState(22);
				((ExprContext)_localctx).n = path();
				_localctx.expression.path.add(((ExprContext)_localctx).n.pathNode);
				}
				}
				setState(29);
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

	public static class PathContext extends ParserRuleContext {
		public PathNode pathNode;
		public VariableDeclaratorIdContext v;
		public MethodContext m;
		public ArrayContext a;
		public VariableDeclaratorIdContext variableDeclaratorId() {
			return getRuleContext(VariableDeclaratorIdContext.class,0);
		}
		public MethodContext method() {
			return getRuleContext(MethodContext.class,0);
		}
		public ArrayContext array() {
			return getRuleContext(ArrayContext.class,0);
		}
		public PathContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_path; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JPathListener ) ((JPathListener)listener).enterPath(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JPathListener ) ((JPathListener)listener).exitPath(this);
		}
	}

	public final PathContext path() throws RecognitionException {
		PathContext _localctx = new PathContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_path);
		try {
			setState(39);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,1,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(30);
				((PathContext)_localctx).v = variableDeclaratorId();
				((PathContext)_localctx).pathNode =  new PathNodeField((((PathContext)_localctx).v!=null?_input.getText(((PathContext)_localctx).v.start,((PathContext)_localctx).v.stop):null)); 
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(33);
				((PathContext)_localctx).m = method();
				((PathContext)_localctx).pathNode =  new PathNodeMethod(((PathContext)_localctx).m.nameWithParams._1, ((PathContext)_localctx).m.nameWithParams._2); 
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(36);
				((PathContext)_localctx).a = array();
				((PathContext)_localctx).pathNode =  new PathNodeArray(((PathContext)_localctx).a.arrayValue._1, ((PathContext)_localctx).a.arrayValue._2); 
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

	public static class VariableDeclaratorIdContext extends ParserRuleContext {
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public VariableDeclaratorIdContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_variableDeclaratorId; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JPathListener ) ((JPathListener)listener).enterVariableDeclaratorId(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JPathListener ) ((JPathListener)listener).exitVariableDeclaratorId(this);
		}
	}

	public final VariableDeclaratorIdContext variableDeclaratorId() throws RecognitionException {
		VariableDeclaratorIdContext _localctx = new VariableDeclaratorIdContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_variableDeclaratorId);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(41);
			identifier();
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

	public static class ArrayContext extends ParserRuleContext {
		public Pair<String,Integer> arrayValue;
		public IdentifierContext i;
		public Token n;
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public TerminalNode DecimalIntegerLiteral() { return getToken(JPathParser.DecimalIntegerLiteral, 0); }
		public ArrayContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_array; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JPathListener ) ((JPathListener)listener).enterArray(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JPathListener ) ((JPathListener)listener).exitArray(this);
		}
	}

	public final ArrayContext array() throws RecognitionException {
		ArrayContext _localctx = new ArrayContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_array);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(43);
			((ArrayContext)_localctx).i = identifier();
			setState(44);
			match(T__3);
			setState(45);
			((ArrayContext)_localctx).n = match(DecimalIntegerLiteral);
			setState(46);
			match(T__4);
			((ArrayContext)_localctx).arrayValue =  __((((ArrayContext)_localctx).i!=null?_input.getText(((ArrayContext)_localctx).i.start,((ArrayContext)_localctx).i.stop):null), Integer.parseInt((((ArrayContext)_localctx).n!=null?((ArrayContext)_localctx).n.getText():null)));
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

	public static class MethodContext extends ParserRuleContext {
		public Pair<String,List<Object>> nameWithParams;
		public IdentifierContext i;
		public MethodParametersContext p;
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public MethodParametersContext methodParameters() {
			return getRuleContext(MethodParametersContext.class,0);
		}
		public MethodContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_method; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JPathListener ) ((JPathListener)listener).enterMethod(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JPathListener ) ((JPathListener)listener).exitMethod(this);
		}
	}

	public final MethodContext method() throws RecognitionException {
		MethodContext _localctx = new MethodContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_method);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(49);
			((MethodContext)_localctx).i = identifier();
			setState(50);
			match(T__5);
			setState(51);
			((MethodContext)_localctx).p = methodParameters();
			setState(52);
			match(T__6);
			((MethodContext)_localctx).nameWithParams =  __((((MethodContext)_localctx).i!=null?_input.getText(((MethodContext)_localctx).i.start,((MethodContext)_localctx).i.stop):null), ((MethodContext)_localctx).p.arguments);
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

	public static class MethodParametersContext extends ParserRuleContext {
		public List<Object> arguments = new ArrayList<Object>();
		public MethodParameterContext mp;
		public MethodParameterContext mp2;
		public List<MethodParameterContext> methodParameter() {
			return getRuleContexts(MethodParameterContext.class);
		}
		public MethodParameterContext methodParameter(int i) {
			return getRuleContext(MethodParameterContext.class,i);
		}
		public MethodParametersContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_methodParameters; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JPathListener ) ((JPathListener)listener).enterMethodParameters(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JPathListener ) ((JPathListener)listener).exitMethodParameters(this);
		}
	}

	public final MethodParametersContext methodParameters() throws RecognitionException {
		MethodParametersContext _localctx = new MethodParametersContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_methodParameters);
		int _la;
		try {
			setState(67);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__6:
				enterOuterAlt(_localctx, 1);
				{
				}
				break;
			case DecimalIntegerLiteral:
			case StringLiteral:
				enterOuterAlt(_localctx, 2);
				{
				setState(56);
				((MethodParametersContext)_localctx).mp = methodParameter();
				_localctx.arguments.add(((MethodParametersContext)_localctx).mp.argument);
				setState(64);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==T__7) {
					{
					{
					setState(58);
					match(T__7);
					setState(59);
					((MethodParametersContext)_localctx).mp2 = methodParameter();
					_localctx.arguments.add(((MethodParametersContext)_localctx).mp2.argument);
					}
					}
					setState(66);
					_errHandler.sync(this);
					_la = _input.LA(1);
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

	public static class MethodParameterContext extends ParserRuleContext {
		public Object argument;
		public Token s;
		public Token di;
		public TerminalNode StringLiteral() { return getToken(JPathParser.StringLiteral, 0); }
		public TerminalNode DecimalIntegerLiteral() { return getToken(JPathParser.DecimalIntegerLiteral, 0); }
		public MethodParameterContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_methodParameter; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JPathListener ) ((JPathListener)listener).enterMethodParameter(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JPathListener ) ((JPathListener)listener).exitMethodParameter(this);
		}
	}

	public final MethodParameterContext methodParameter() throws RecognitionException {
		MethodParameterContext _localctx = new MethodParameterContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_methodParameter);
		try {
			setState(73);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case StringLiteral:
				enterOuterAlt(_localctx, 1);
				{
				setState(69);
				((MethodParameterContext)_localctx).s = match(StringLiteral);
				((MethodParameterContext)_localctx).argument =  (((MethodParameterContext)_localctx).s!=null?((MethodParameterContext)_localctx).s.getText():null);
				}
				break;
			case DecimalIntegerLiteral:
				enterOuterAlt(_localctx, 2);
				{
				setState(71);
				((MethodParameterContext)_localctx).di = match(DecimalIntegerLiteral);
				((MethodParameterContext)_localctx).argument =  Long.parseLong((((MethodParameterContext)_localctx).di!=null?((MethodParameterContext)_localctx).di.getText():null));
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

	public static class IdentifierContext extends ParserRuleContext {
		public TerminalNode Identifier() { return getToken(JPathParser.Identifier, 0); }
		public IdentifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_identifier; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JPathListener ) ((JPathListener)listener).enterIdentifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JPathListener ) ((JPathListener)listener).exitIdentifier(this);
		}
	}

	public final IdentifierContext identifier() throws RecognitionException {
		IdentifierContext _localctx = new IdentifierContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_identifier);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(75);
			match(Identifier);
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
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\3\16P\4\2\t\2\4\3\t"+
		"\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\3\2\3\2\3\2\3\2\3\2"+
		"\3\2\3\2\3\2\3\2\7\2\34\n\2\f\2\16\2\37\13\2\3\3\3\3\3\3\3\3\3\3\3\3\3"+
		"\3\3\3\3\3\5\3*\n\3\3\4\3\4\3\5\3\5\3\5\3\5\3\5\3\5\3\6\3\6\3\6\3\6\3"+
		"\6\3\6\3\7\3\7\3\7\3\7\3\7\3\7\3\7\7\7A\n\7\f\7\16\7D\13\7\5\7F\n\7\3"+
		"\b\3\b\3\b\3\b\5\bL\n\b\3\t\3\t\3\t\2\2\n\2\4\6\b\n\f\16\20\2\2\2M\2\22"+
		"\3\2\2\2\4)\3\2\2\2\6+\3\2\2\2\b-\3\2\2\2\n\63\3\2\2\2\fE\3\2\2\2\16K"+
		"\3\2\2\2\20M\3\2\2\2\22\23\7\3\2\2\23\24\b\2\1\2\24\25\7\4\2\2\25\26\5"+
		"\6\4\2\26\35\b\2\1\2\27\30\7\5\2\2\30\31\5\4\3\2\31\32\b\2\1\2\32\34\3"+
		"\2\2\2\33\27\3\2\2\2\34\37\3\2\2\2\35\33\3\2\2\2\35\36\3\2\2\2\36\3\3"+
		"\2\2\2\37\35\3\2\2\2 !\5\6\4\2!\"\b\3\1\2\"*\3\2\2\2#$\5\n\6\2$%\b\3\1"+
		"\2%*\3\2\2\2&\'\5\b\5\2\'(\b\3\1\2(*\3\2\2\2) \3\2\2\2)#\3\2\2\2)&\3\2"+
		"\2\2*\5\3\2\2\2+,\5\20\t\2,\7\3\2\2\2-.\5\20\t\2./\7\6\2\2/\60\7\f\2\2"+
		"\60\61\7\7\2\2\61\62\b\5\1\2\62\t\3\2\2\2\63\64\5\20\t\2\64\65\7\b\2\2"+
		"\65\66\5\f\7\2\66\67\7\t\2\2\678\b\6\1\28\13\3\2\2\29F\3\2\2\2:;\5\16"+
		"\b\2;B\b\7\1\2<=\7\n\2\2=>\5\16\b\2>?\b\7\1\2?A\3\2\2\2@<\3\2\2\2AD\3"+
		"\2\2\2B@\3\2\2\2BC\3\2\2\2CF\3\2\2\2DB\3\2\2\2E9\3\2\2\2E:\3\2\2\2F\r"+
		"\3\2\2\2GH\7\r\2\2HL\b\b\1\2IJ\7\f\2\2JL\b\b\1\2KG\3\2\2\2KI\3\2\2\2L"+
		"\17\3\2\2\2MN\7\13\2\2N\21\3\2\2\2\7\35)BEK";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}