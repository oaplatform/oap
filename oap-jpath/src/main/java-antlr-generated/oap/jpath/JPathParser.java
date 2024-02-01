// Generated from JPath.g4 by ANTLR 4.13.0

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

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast", "CheckReturnValue"})
public class JPathParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.13.0", RuntimeMetaData.VERSION); }

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
			null, "'${'", "'.'", "'}'", "'['", "']'", "'('", "')'", "','"
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

	@SuppressWarnings("CheckReturnValue")
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
			((ExprContext)_localctx).f = variableDeclaratorId();
			_localctx.expression.path.add(new PathNodeField((((ExprContext)_localctx).f!=null?_input.getText(((ExprContext)_localctx).f.start,((ExprContext)_localctx).f.stop):null)));
			setState(26);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__1) {
				{
				{
				setState(20);
				match(T__1);
				setState(21);
				((ExprContext)_localctx).n = path();
				_localctx.expression.path.add(((ExprContext)_localctx).n.pathNode);
				}
				}
				setState(28);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(29);
			match(T__2);
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
	public static class PathContext extends ParserRuleContext {
		public AbstractPathNode pathNode;
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
			setState(40);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,1,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(31);
				((PathContext)_localctx).v = variableDeclaratorId();
				((PathContext)_localctx).pathNode =  new PathNodeField((((PathContext)_localctx).v!=null?_input.getText(((PathContext)_localctx).v.start,((PathContext)_localctx).v.stop):null)); 
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(34);
				((PathContext)_localctx).m = method();
				((PathContext)_localctx).pathNode =  new PathNodeMethod(((PathContext)_localctx).m.nameWithParams._1, ((PathContext)_localctx).m.nameWithParams._2); 
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(37);
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

	@SuppressWarnings("CheckReturnValue")
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
			setState(42);
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

	@SuppressWarnings("CheckReturnValue")
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
			setState(44);
			((ArrayContext)_localctx).i = identifier();
			setState(45);
			match(T__3);
			setState(46);
			((ArrayContext)_localctx).n = match(DecimalIntegerLiteral);
			setState(47);
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

	@SuppressWarnings("CheckReturnValue")
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
			setState(50);
			((MethodContext)_localctx).i = identifier();
			setState(51);
			match(T__5);
			setState(52);
			((MethodContext)_localctx).p = methodParameters();
			setState(53);
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

	@SuppressWarnings("CheckReturnValue")
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
			setState(68);
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
				setState(57);
				((MethodParametersContext)_localctx).mp = methodParameter();
				_localctx.arguments.add(((MethodParametersContext)_localctx).mp.argument);
				setState(65);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==T__7) {
					{
					{
					setState(59);
					match(T__7);
					setState(60);
					((MethodParametersContext)_localctx).mp2 = methodParameter();
					_localctx.arguments.add(((MethodParametersContext)_localctx).mp2.argument);
					}
					}
					setState(67);
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

	@SuppressWarnings("CheckReturnValue")
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
			setState(74);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case StringLiteral:
				enterOuterAlt(_localctx, 1);
				{
				setState(70);
				((MethodParameterContext)_localctx).s = match(StringLiteral);
				((MethodParameterContext)_localctx).argument =  (((MethodParameterContext)_localctx).s!=null?((MethodParameterContext)_localctx).s.getText():null);
				}
				break;
			case DecimalIntegerLiteral:
				enterOuterAlt(_localctx, 2);
				{
				setState(72);
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

	@SuppressWarnings("CheckReturnValue")
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
			setState(76);
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
		"\u0004\u0001\fO\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001\u0002"+
		"\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004\u0007\u0004\u0002"+
		"\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002\u0007\u0007\u0007\u0001"+
		"\u0000\u0001\u0000\u0001\u0000\u0001\u0000\u0001\u0000\u0001\u0000\u0001"+
		"\u0000\u0001\u0000\u0005\u0000\u0019\b\u0000\n\u0000\f\u0000\u001c\t\u0000"+
		"\u0001\u0000\u0001\u0000\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001"+
		"\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0003\u0001"+
		")\b\u0001\u0001\u0002\u0001\u0002\u0001\u0003\u0001\u0003\u0001\u0003"+
		"\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0004\u0001\u0004\u0001\u0004"+
		"\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0005\u0001\u0005\u0001\u0005"+
		"\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0005\u0005@\b\u0005"+
		"\n\u0005\f\u0005C\t\u0005\u0003\u0005E\b\u0005\u0001\u0006\u0001\u0006"+
		"\u0001\u0006\u0001\u0006\u0003\u0006K\b\u0006\u0001\u0007\u0001\u0007"+
		"\u0001\u0007\u0000\u0000\b\u0000\u0002\u0004\u0006\b\n\f\u000e\u0000\u0000"+
		"L\u0000\u0010\u0001\u0000\u0000\u0000\u0002(\u0001\u0000\u0000\u0000\u0004"+
		"*\u0001\u0000\u0000\u0000\u0006,\u0001\u0000\u0000\u0000\b2\u0001\u0000"+
		"\u0000\u0000\nD\u0001\u0000\u0000\u0000\fJ\u0001\u0000\u0000\u0000\u000e"+
		"L\u0001\u0000\u0000\u0000\u0010\u0011\u0005\u0001\u0000\u0000\u0011\u0012"+
		"\u0006\u0000\uffff\uffff\u0000\u0012\u0013\u0003\u0004\u0002\u0000\u0013"+
		"\u001a\u0006\u0000\uffff\uffff\u0000\u0014\u0015\u0005\u0002\u0000\u0000"+
		"\u0015\u0016\u0003\u0002\u0001\u0000\u0016\u0017\u0006\u0000\uffff\uffff"+
		"\u0000\u0017\u0019\u0001\u0000\u0000\u0000\u0018\u0014\u0001\u0000\u0000"+
		"\u0000\u0019\u001c\u0001\u0000\u0000\u0000\u001a\u0018\u0001\u0000\u0000"+
		"\u0000\u001a\u001b\u0001\u0000\u0000\u0000\u001b\u001d\u0001\u0000\u0000"+
		"\u0000\u001c\u001a\u0001\u0000\u0000\u0000\u001d\u001e\u0005\u0003\u0000"+
		"\u0000\u001e\u0001\u0001\u0000\u0000\u0000\u001f \u0003\u0004\u0002\u0000"+
		" !\u0006\u0001\uffff\uffff\u0000!)\u0001\u0000\u0000\u0000\"#\u0003\b"+
		"\u0004\u0000#$\u0006\u0001\uffff\uffff\u0000$)\u0001\u0000\u0000\u0000"+
		"%&\u0003\u0006\u0003\u0000&\'\u0006\u0001\uffff\uffff\u0000\')\u0001\u0000"+
		"\u0000\u0000(\u001f\u0001\u0000\u0000\u0000(\"\u0001\u0000\u0000\u0000"+
		"(%\u0001\u0000\u0000\u0000)\u0003\u0001\u0000\u0000\u0000*+\u0003\u000e"+
		"\u0007\u0000+\u0005\u0001\u0000\u0000\u0000,-\u0003\u000e\u0007\u0000"+
		"-.\u0005\u0004\u0000\u0000./\u0005\n\u0000\u0000/0\u0005\u0005\u0000\u0000"+
		"01\u0006\u0003\uffff\uffff\u00001\u0007\u0001\u0000\u0000\u000023\u0003"+
		"\u000e\u0007\u000034\u0005\u0006\u0000\u000045\u0003\n\u0005\u000056\u0005"+
		"\u0007\u0000\u000067\u0006\u0004\uffff\uffff\u00007\t\u0001\u0000\u0000"+
		"\u00008E\u0001\u0000\u0000\u00009:\u0003\f\u0006\u0000:A\u0006\u0005\uffff"+
		"\uffff\u0000;<\u0005\b\u0000\u0000<=\u0003\f\u0006\u0000=>\u0006\u0005"+
		"\uffff\uffff\u0000>@\u0001\u0000\u0000\u0000?;\u0001\u0000\u0000\u0000"+
		"@C\u0001\u0000\u0000\u0000A?\u0001\u0000\u0000\u0000AB\u0001\u0000\u0000"+
		"\u0000BE\u0001\u0000\u0000\u0000CA\u0001\u0000\u0000\u0000D8\u0001\u0000"+
		"\u0000\u0000D9\u0001\u0000\u0000\u0000E\u000b\u0001\u0000\u0000\u0000"+
		"FG\u0005\u000b\u0000\u0000GK\u0006\u0006\uffff\uffff\u0000HI\u0005\n\u0000"+
		"\u0000IK\u0006\u0006\uffff\uffff\u0000JF\u0001\u0000\u0000\u0000JH\u0001"+
		"\u0000\u0000\u0000K\r\u0001\u0000\u0000\u0000LM\u0005\t\u0000\u0000M\u000f"+
		"\u0001\u0000\u0000\u0000\u0005\u001a(ADJ";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}