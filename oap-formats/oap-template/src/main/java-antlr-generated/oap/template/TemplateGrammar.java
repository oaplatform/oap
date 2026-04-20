// Generated from TemplateGrammar.g4 by ANTLR 4.13.0

package oap.template;

import oap.template.tree.*;
import oap.template.tree.BlockIfElement;

import org.apache.commons.lang3.StringUtils;
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
public class TemplateGrammar extends TemplateGrammarAdaptor {
	static { RuntimeMetaData.checkVersion("4.13.0", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		STARTBLOCKIF=1, STARTBLOCKELSE=2, STARTBLOCKEND=3, STARTESCEXPR=4, STARTEXPR=5, 
		STARTEXPR2=6, TEXT=7, LBRACE=8, RBRACE=9, EXPRESSION=10, LBRACE2=11, RBRACE2=12, 
		EXPRESSION2=13, BLOCK_IF_CONTENT=14, BLOCK_IF_RBRACE=15;
	public static final int
		RULE_elements = 0, RULE_element = 1, RULE_blockIfElement = 2, RULE_blockBody = 3, 
		RULE_text = 4, RULE_comment = 5, RULE_expression = 6, RULE_expressionContent = 7;
	private static String[] makeRuleNames() {
		return new String[] {
			"elements", "element", "blockIfElement", "blockBody", "text", "comment", 
			"expression", "expressionContent"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, "'}}'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "STARTBLOCKIF", "STARTBLOCKELSE", "STARTBLOCKEND", "STARTESCEXPR", 
			"STARTEXPR", "STARTEXPR2", "TEXT", "LBRACE", "RBRACE", "EXPRESSION", 
			"LBRACE2", "RBRACE2", "EXPRESSION2", "BLOCK_IF_CONTENT", "BLOCK_IF_RBRACE"
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

	@SuppressWarnings("CheckReturnValue")
	public static class ElementsContext extends ParserRuleContext {
		public Map<String,String> aliases;
		public Elements ret = new Elements();
		public ElementContext element;
		public TerminalNode EOF() { return getToken(TemplateGrammar.EOF, 0); }
		public List<ElementContext> element() {
			return getRuleContexts(ElementContext.class);
		}
		public ElementContext element(int i) {
			return getRuleContext(ElementContext.class,i);
		}
		public ElementsContext(ParserRuleContext parent, int invokingState) { super(parent, invokingState); }
		public ElementsContext(ParserRuleContext parent, int invokingState, Map<String,String> aliases) {
			super(parent, invokingState);
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

	public final ElementsContext elements(Map<String,String> aliases) throws RecognitionException {
		ElementsContext _localctx = new ElementsContext(_ctx, getState(), aliases);
		enterRule(_localctx, 0, RULE_elements);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(21);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 242L) != 0)) {
				{
				{
				setState(16);
				((ElementsContext)_localctx).element = element(aliases);
				 _localctx.ret.elements.add( ((ElementsContext)_localctx).element.ret ); 
				}
				}
				setState(23);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(24);
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

	@SuppressWarnings("CheckReturnValue")
	public static class ElementContext extends ParserRuleContext {
		public Map<String,String> aliases;
		public Element ret;
		public TextContext t;
		public CommentContext comment;
		public ExpressionContext expression;
		public BlockIfElementContext blockIfElement;
		public TextContext text() {
			return getRuleContext(TextContext.class,0);
		}
		public CommentContext comment() {
			return getRuleContext(CommentContext.class,0);
		}
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public BlockIfElementContext blockIfElement() {
			return getRuleContext(BlockIfElementContext.class,0);
		}
		public ElementContext(ParserRuleContext parent, int invokingState) { super(parent, invokingState); }
		public ElementContext(ParserRuleContext parent, int invokingState, Map<String,String> aliases) {
			super(parent, invokingState);
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

	public final ElementContext element(Map<String,String> aliases) throws RecognitionException {
		ElementContext _localctx = new ElementContext(_ctx, getState(), aliases);
		enterRule(_localctx, 2, RULE_element);
		try {
			setState(38);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case TEXT:
				enterOuterAlt(_localctx, 1);
				{
				setState(26);
				((ElementContext)_localctx).t = text();
				 ((ElementContext)_localctx).ret =  new TextElement( (((ElementContext)_localctx).t!=null?_input.getText(((ElementContext)_localctx).t.start,((ElementContext)_localctx).t.stop):null) ); 
				}
				break;
			case STARTESCEXPR:
				enterOuterAlt(_localctx, 2);
				{
				setState(29);
				((ElementContext)_localctx).comment = comment();
				 ((ElementContext)_localctx).ret =  new TextElement( (((ElementContext)_localctx).comment!=null?_input.getText(((ElementContext)_localctx).comment.start,((ElementContext)_localctx).comment.stop):null).substring(1) ); 
				}
				break;
			case STARTEXPR:
			case STARTEXPR2:
				enterOuterAlt(_localctx, 3);
				{
				setState(32);
				((ElementContext)_localctx).expression = expression(aliases);
				 ((ElementContext)_localctx).ret =  new ExpressionElement( ((ElementContext)_localctx).expression.ret ); 
				}
				break;
			case STARTBLOCKIF:
				enterOuterAlt(_localctx, 4);
				{
				setState(35);
				((ElementContext)_localctx).blockIfElement = blockIfElement(aliases);
				 ((ElementContext)_localctx).ret =  ((ElementContext)_localctx).blockIfElement.ret; 
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
	public static class BlockIfElementContext extends ParserRuleContext {
		public Map<String,String> aliases;
		public BlockIfElement ret;
		public Token BLOCK_IF_CONTENT;
		public BlockBodyContext thenBranch;
		public BlockBodyContext elseBranch;
		public TerminalNode STARTBLOCKIF() { return getToken(TemplateGrammar.STARTBLOCKIF, 0); }
		public TerminalNode BLOCK_IF_CONTENT() { return getToken(TemplateGrammar.BLOCK_IF_CONTENT, 0); }
		public TerminalNode BLOCK_IF_RBRACE() { return getToken(TemplateGrammar.BLOCK_IF_RBRACE, 0); }
		public TerminalNode STARTBLOCKEND() { return getToken(TemplateGrammar.STARTBLOCKEND, 0); }
		public List<BlockBodyContext> blockBody() {
			return getRuleContexts(BlockBodyContext.class);
		}
		public BlockBodyContext blockBody(int i) {
			return getRuleContext(BlockBodyContext.class,i);
		}
		public TerminalNode STARTBLOCKELSE() { return getToken(TemplateGrammar.STARTBLOCKELSE, 0); }
		public BlockIfElementContext(ParserRuleContext parent, int invokingState) { super(parent, invokingState); }
		public BlockIfElementContext(ParserRuleContext parent, int invokingState, Map<String,String> aliases) {
			super(parent, invokingState);
			this.aliases = aliases;
		}
		@Override public int getRuleIndex() { return RULE_blockIfElement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarListener ) ((TemplateGrammarListener)listener).enterBlockIfElement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarListener ) ((TemplateGrammarListener)listener).exitBlockIfElement(this);
		}
	}

	public final BlockIfElementContext blockIfElement(Map<String,String> aliases) throws RecognitionException {
		BlockIfElementContext _localctx = new BlockIfElementContext(_ctx, getState(), aliases);
		enterRule(_localctx, 4, RULE_blockIfElement);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(40);
			match(STARTBLOCKIF);
			setState(41);
			((BlockIfElementContext)_localctx).BLOCK_IF_CONTENT = match(BLOCK_IF_CONTENT);
			setState(42);
			match(BLOCK_IF_RBRACE);
			setState(43);
			((BlockIfElementContext)_localctx).thenBranch = blockBody(aliases);
			setState(46);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==STARTBLOCKELSE) {
				{
				setState(44);
				match(STARTBLOCKELSE);
				setState(45);
				((BlockIfElementContext)_localctx).elseBranch = blockBody(aliases);
				}
			}

			setState(48);
			match(STARTBLOCKEND);

				      ((BlockIfElementContext)_localctx).ret =  new BlockIfElement(
				          StringUtils.trim( (((BlockIfElementContext)_localctx).BLOCK_IF_CONTENT!=null?((BlockIfElementContext)_localctx).BLOCK_IF_CONTENT.getText():null) ),
				          ((BlockIfElementContext)_localctx).thenBranch.ret,
				          ((BlockIfElementContext)_localctx).elseBranch != null ? ((BlockIfElementContext)_localctx).elseBranch.ret : null
				      );
				  
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
	public static class BlockBodyContext extends ParserRuleContext {
		public Map<String,String> aliases;
		public Elements ret = new Elements();
		public ElementContext element;
		public List<ElementContext> element() {
			return getRuleContexts(ElementContext.class);
		}
		public ElementContext element(int i) {
			return getRuleContext(ElementContext.class,i);
		}
		public BlockBodyContext(ParserRuleContext parent, int invokingState) { super(parent, invokingState); }
		public BlockBodyContext(ParserRuleContext parent, int invokingState, Map<String,String> aliases) {
			super(parent, invokingState);
			this.aliases = aliases;
		}
		@Override public int getRuleIndex() { return RULE_blockBody; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarListener ) ((TemplateGrammarListener)listener).enterBlockBody(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarListener ) ((TemplateGrammarListener)listener).exitBlockBody(this);
		}
	}

	public final BlockBodyContext blockBody(Map<String,String> aliases) throws RecognitionException {
		BlockBodyContext _localctx = new BlockBodyContext(_ctx, getState(), aliases);
		enterRule(_localctx, 6, RULE_blockBody);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(56);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 242L) != 0)) {
				{
				{
				setState(51);
				((BlockBodyContext)_localctx).element = element(aliases);
				 if( ((BlockBodyContext)_localctx).element.ret != null ) _localctx.ret.elements.add( ((BlockBodyContext)_localctx).element.ret ); 
				}
				}
				setState(58);
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
		enterRule(_localctx, 8, RULE_text);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(60); 
			_errHandler.sync(this);
			_alt = 1;
			do {
				switch (_alt) {
				case 1:
					{
					{
					setState(59);
					match(TEXT);
					}
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(62); 
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,4,_ctx);
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

	@SuppressWarnings("CheckReturnValue")
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
		enterRule(_localctx, 10, RULE_comment);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(64);
			match(STARTESCEXPR);
			setState(65);
			expressionContent();
			setState(66);
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
	public static class ExpressionContext extends ParserRuleContext {
		public Map<String,String> aliases;
		public String ret;
		public ExpressionContentContext expressionContent;
		public ExpressionContentContext expressionContent() {
			return getRuleContext(ExpressionContentContext.class,0);
		}
		public TerminalNode STARTEXPR() { return getToken(TemplateGrammar.STARTEXPR, 0); }
		public TerminalNode STARTEXPR2() { return getToken(TemplateGrammar.STARTEXPR2, 0); }
		public TerminalNode RBRACE() { return getToken(TemplateGrammar.RBRACE, 0); }
		public TerminalNode RBRACE2() { return getToken(TemplateGrammar.RBRACE2, 0); }
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
		enterRule(_localctx, 12, RULE_expression);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(68);
			_la = _input.LA(1);
			if ( !(_la==STARTEXPR || _la==STARTEXPR2) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			setState(69);
			((ExpressionContext)_localctx).expressionContent = expressionContent();
			setState(70);
			_la = _input.LA(1);
			if ( !(_la==RBRACE || _la==RBRACE2) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}

			        ((ExpressionContext)_localctx).ret =  StringUtils.trim( (((ExpressionContext)_localctx).expressionContent!=null?_input.getText(((ExpressionContext)_localctx).expressionContent.start,((ExpressionContext)_localctx).expressionContent.stop):null) );
			        String alias = aliases.get( _localctx.ret );
			        if( alias != null ) ((ExpressionContext)_localctx).ret =  alias;
			    
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
	public static class ExpressionContentContext extends ParserRuleContext {
		public List<TerminalNode> EXPRESSION() { return getTokens(TemplateGrammar.EXPRESSION); }
		public TerminalNode EXPRESSION(int i) {
			return getToken(TemplateGrammar.EXPRESSION, i);
		}
		public List<TerminalNode> EXPRESSION2() { return getTokens(TemplateGrammar.EXPRESSION2); }
		public TerminalNode EXPRESSION2(int i) {
			return getToken(TemplateGrammar.EXPRESSION2, i);
		}
		public List<TerminalNode> LBRACE() { return getTokens(TemplateGrammar.LBRACE); }
		public TerminalNode LBRACE(int i) {
			return getToken(TemplateGrammar.LBRACE, i);
		}
		public List<TerminalNode> RBRACE() { return getTokens(TemplateGrammar.RBRACE); }
		public TerminalNode RBRACE(int i) {
			return getToken(TemplateGrammar.RBRACE, i);
		}
		public List<TerminalNode> LBRACE2() { return getTokens(TemplateGrammar.LBRACE2); }
		public TerminalNode LBRACE2(int i) {
			return getToken(TemplateGrammar.LBRACE2, i);
		}
		public List<TerminalNode> RBRACE2() { return getTokens(TemplateGrammar.RBRACE2); }
		public TerminalNode RBRACE2(int i) {
			return getToken(TemplateGrammar.RBRACE2, i);
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
		enterRule(_localctx, 14, RULE_expressionContent);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(74); 
			_errHandler.sync(this);
			_alt = 1;
			do {
				switch (_alt) {
				case 1:
					{
					{
					setState(73);
					_la = _input.LA(1);
					if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 16128L) != 0)) ) {
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
				setState(76); 
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,5,_ctx);
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
		"\u0004\u0001\u000fO\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001\u0002"+
		"\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004\u0007\u0004\u0002"+
		"\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002\u0007\u0007\u0007\u0001"+
		"\u0000\u0001\u0000\u0001\u0000\u0005\u0000\u0014\b\u0000\n\u0000\f\u0000"+
		"\u0017\t\u0000\u0001\u0000\u0001\u0000\u0001\u0001\u0001\u0001\u0001\u0001"+
		"\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001"+
		"\u0001\u0001\u0001\u0001\u0001\u0001\u0003\u0001\'\b\u0001\u0001\u0002"+
		"\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0003\u0002"+
		"/\b\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0003\u0001\u0003"+
		"\u0001\u0003\u0005\u00037\b\u0003\n\u0003\f\u0003:\t\u0003\u0001\u0004"+
		"\u0004\u0004=\b\u0004\u000b\u0004\f\u0004>\u0001\u0005\u0001\u0005\u0001"+
		"\u0005\u0001\u0005\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0001"+
		"\u0006\u0001\u0007\u0004\u0007K\b\u0007\u000b\u0007\f\u0007L\u0001\u0007"+
		"\u0000\u0000\b\u0000\u0002\u0004\u0006\b\n\f\u000e\u0000\u0003\u0001\u0000"+
		"\u0005\u0006\u0002\u0000\t\t\f\f\u0001\u0000\b\rN\u0000\u0015\u0001\u0000"+
		"\u0000\u0000\u0002&\u0001\u0000\u0000\u0000\u0004(\u0001\u0000\u0000\u0000"+
		"\u00068\u0001\u0000\u0000\u0000\b<\u0001\u0000\u0000\u0000\n@\u0001\u0000"+
		"\u0000\u0000\fD\u0001\u0000\u0000\u0000\u000eJ\u0001\u0000\u0000\u0000"+
		"\u0010\u0011\u0003\u0002\u0001\u0000\u0011\u0012\u0006\u0000\uffff\uffff"+
		"\u0000\u0012\u0014\u0001\u0000\u0000\u0000\u0013\u0010\u0001\u0000\u0000"+
		"\u0000\u0014\u0017\u0001\u0000\u0000\u0000\u0015\u0013\u0001\u0000\u0000"+
		"\u0000\u0015\u0016\u0001\u0000\u0000\u0000\u0016\u0018\u0001\u0000\u0000"+
		"\u0000\u0017\u0015\u0001\u0000\u0000\u0000\u0018\u0019\u0005\u0000\u0000"+
		"\u0001\u0019\u0001\u0001\u0000\u0000\u0000\u001a\u001b\u0003\b\u0004\u0000"+
		"\u001b\u001c\u0006\u0001\uffff\uffff\u0000\u001c\'\u0001\u0000\u0000\u0000"+
		"\u001d\u001e\u0003\n\u0005\u0000\u001e\u001f\u0006\u0001\uffff\uffff\u0000"+
		"\u001f\'\u0001\u0000\u0000\u0000 !\u0003\f\u0006\u0000!\"\u0006\u0001"+
		"\uffff\uffff\u0000\"\'\u0001\u0000\u0000\u0000#$\u0003\u0004\u0002\u0000"+
		"$%\u0006\u0001\uffff\uffff\u0000%\'\u0001\u0000\u0000\u0000&\u001a\u0001"+
		"\u0000\u0000\u0000&\u001d\u0001\u0000\u0000\u0000& \u0001\u0000\u0000"+
		"\u0000&#\u0001\u0000\u0000\u0000\'\u0003\u0001\u0000\u0000\u0000()\u0005"+
		"\u0001\u0000\u0000)*\u0005\u000e\u0000\u0000*+\u0005\u000f\u0000\u0000"+
		"+.\u0003\u0006\u0003\u0000,-\u0005\u0002\u0000\u0000-/\u0003\u0006\u0003"+
		"\u0000.,\u0001\u0000\u0000\u0000./\u0001\u0000\u0000\u0000/0\u0001\u0000"+
		"\u0000\u000001\u0005\u0003\u0000\u000012\u0006\u0002\uffff\uffff\u0000"+
		"2\u0005\u0001\u0000\u0000\u000034\u0003\u0002\u0001\u000045\u0006\u0003"+
		"\uffff\uffff\u000057\u0001\u0000\u0000\u000063\u0001\u0000\u0000\u0000"+
		"7:\u0001\u0000\u0000\u000086\u0001\u0000\u0000\u000089\u0001\u0000\u0000"+
		"\u00009\u0007\u0001\u0000\u0000\u0000:8\u0001\u0000\u0000\u0000;=\u0005"+
		"\u0007\u0000\u0000<;\u0001\u0000\u0000\u0000=>\u0001\u0000\u0000\u0000"+
		"><\u0001\u0000\u0000\u0000>?\u0001\u0000\u0000\u0000?\t\u0001\u0000\u0000"+
		"\u0000@A\u0005\u0004\u0000\u0000AB\u0003\u000e\u0007\u0000BC\u0005\t\u0000"+
		"\u0000C\u000b\u0001\u0000\u0000\u0000DE\u0007\u0000\u0000\u0000EF\u0003"+
		"\u000e\u0007\u0000FG\u0007\u0001\u0000\u0000GH\u0006\u0006\uffff\uffff"+
		"\u0000H\r\u0001\u0000\u0000\u0000IK\u0007\u0002\u0000\u0000JI\u0001\u0000"+
		"\u0000\u0000KL\u0001\u0000\u0000\u0000LJ\u0001\u0000\u0000\u0000LM\u0001"+
		"\u0000\u0000\u0000M\u000f\u0001\u0000\u0000\u0000\u0006\u0015&.8>L";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}