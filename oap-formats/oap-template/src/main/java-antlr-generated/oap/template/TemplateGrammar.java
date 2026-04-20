// Generated from TemplateGrammar.g4 by ANTLR 4.13.0

package oap.template;

import oap.template.tree.*;
import oap.template.tree.BlockIfElement;
import oap.template.tree.BlockWithElement;

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
		STARTBLOCKWITH=1, STARTBLOCKIF_LTRIM=2, STARTBLOCKIF=3, STARTBLOCKELSE=4, 
		STARTBLOCKEND=5, STARTESCEXPR=6, STARTEXPR=7, STARTEXPR2_LTRIM=8, STARTEXPR2=9, 
		TEXT=10, LBRACE=11, RBRACE=12, EXPRESSION=13, LBRACE2=14, RBRACE2_RTRIM=15, 
		RBRACE2=16, EXPRESSION2=17, BLOCK_IF_CONTENT=18, BLOCK_IF_RBRACE=19;
	public static final int
		RULE_elements = 0, RULE_element = 1, RULE_blockIfElement = 2, RULE_blockWithElement = 3, 
		RULE_blockBody = 4, RULE_text = 5, RULE_comment = 6, RULE_expression = 7, 
		RULE_expressionContent = 8;
	private static String[] makeRuleNames() {
		return new String[] {
			"elements", "element", "blockIfElement", "blockWithElement", "blockBody", 
			"text", "comment", "expression", "expressionContent"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, null, null, null, null, null, null, null, "'{{-'", null, null, 
			null, null, null, null, "'-}}'", null, null, null, "'}}'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "STARTBLOCKWITH", "STARTBLOCKIF_LTRIM", "STARTBLOCKIF", "STARTBLOCKELSE", 
			"STARTBLOCKEND", "STARTESCEXPR", "STARTEXPR", "STARTEXPR2_LTRIM", "STARTEXPR2", 
			"TEXT", "LBRACE", "RBRACE", "EXPRESSION", "LBRACE2", "RBRACE2_RTRIM", 
			"RBRACE2", "EXPRESSION2", "BLOCK_IF_CONTENT", "BLOCK_IF_RBRACE"
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
			setState(23);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 1998L) != 0)) {
				{
				{
				setState(18);
				((ElementsContext)_localctx).element = element(aliases);
				 _localctx.ret.elements.add( ((ElementsContext)_localctx).element.ret ); 
				}
				}
				setState(25);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
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

	@SuppressWarnings("CheckReturnValue")
	public static class ElementContext extends ParserRuleContext {
		public Map<String,String> aliases;
		public Element ret;
		public TextContext t;
		public CommentContext comment;
		public ExpressionContext expression;
		public BlockIfElementContext blockIfElement;
		public BlockWithElementContext blockWithElement;
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
		public BlockWithElementContext blockWithElement() {
			return getRuleContext(BlockWithElementContext.class,0);
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
			setState(43);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case TEXT:
				enterOuterAlt(_localctx, 1);
				{
				setState(28);
				((ElementContext)_localctx).t = text();
				 ((ElementContext)_localctx).ret =  new TextElement( (((ElementContext)_localctx).t!=null?_input.getText(((ElementContext)_localctx).t.start,((ElementContext)_localctx).t.stop):null) ); 
				}
				break;
			case STARTESCEXPR:
				enterOuterAlt(_localctx, 2);
				{
				setState(31);
				((ElementContext)_localctx).comment = comment();
				 ((ElementContext)_localctx).ret =  new TextElement( (((ElementContext)_localctx).comment!=null?_input.getText(((ElementContext)_localctx).comment.start,((ElementContext)_localctx).comment.stop):null).substring(1) ); 
				}
				break;
			case STARTEXPR:
			case STARTEXPR2_LTRIM:
			case STARTEXPR2:
				enterOuterAlt(_localctx, 3);
				{
				setState(34);
				((ElementContext)_localctx).expression = expression(aliases);
				 ((ElementContext)_localctx).ret =  new ExpressionElement( ((ElementContext)_localctx).expression.ret, ((ElementContext)_localctx).expression.trimLeft, ((ElementContext)_localctx).expression.trimRight ); 
				}
				break;
			case STARTBLOCKIF_LTRIM:
			case STARTBLOCKIF:
				enterOuterAlt(_localctx, 4);
				{
				setState(37);
				((ElementContext)_localctx).blockIfElement = blockIfElement(aliases);
				 ((ElementContext)_localctx).ret =  ((ElementContext)_localctx).blockIfElement.ret; 
				}
				break;
			case STARTBLOCKWITH:
				enterOuterAlt(_localctx, 5);
				{
				setState(40);
				((ElementContext)_localctx).blockWithElement = blockWithElement(aliases);
				 ((ElementContext)_localctx).ret =  ((ElementContext)_localctx).blockWithElement.ret; 
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
		public TerminalNode BLOCK_IF_CONTENT() { return getToken(TemplateGrammar.BLOCK_IF_CONTENT, 0); }
		public TerminalNode BLOCK_IF_RBRACE() { return getToken(TemplateGrammar.BLOCK_IF_RBRACE, 0); }
		public TerminalNode STARTBLOCKEND() { return getToken(TemplateGrammar.STARTBLOCKEND, 0); }
		public TerminalNode STARTBLOCKIF() { return getToken(TemplateGrammar.STARTBLOCKIF, 0); }
		public TerminalNode STARTBLOCKIF_LTRIM() { return getToken(TemplateGrammar.STARTBLOCKIF_LTRIM, 0); }
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
			setState(45);
			_la = _input.LA(1);
			if ( !(_la==STARTBLOCKIF_LTRIM || _la==STARTBLOCKIF) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			setState(46);
			((BlockIfElementContext)_localctx).BLOCK_IF_CONTENT = match(BLOCK_IF_CONTENT);
			setState(47);
			match(BLOCK_IF_RBRACE);
			setState(48);
			((BlockIfElementContext)_localctx).thenBranch = blockBody(aliases);
			setState(51);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==STARTBLOCKELSE) {
				{
				setState(49);
				match(STARTBLOCKELSE);
				setState(50);
				((BlockIfElementContext)_localctx).elseBranch = blockBody(aliases);
				}
			}

			setState(53);
			match(STARTBLOCKEND);

				      ((BlockIfElementContext)_localctx).ret =  new BlockIfElement(
				          StringUtils.trim( (((BlockIfElementContext)_localctx).BLOCK_IF_CONTENT!=null?((BlockIfElementContext)_localctx).BLOCK_IF_CONTENT.getText():null) ),
				          ((BlockIfElementContext)_localctx).thenBranch.ret,
				          ((BlockIfElementContext)_localctx).elseBranch != null ? ((BlockIfElementContext)_localctx).elseBranch.ret : null,
				          _localctx.start.getType() == STARTBLOCKIF_LTRIM
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
	public static class BlockWithElementContext extends ParserRuleContext {
		public Map<String,String> aliases;
		public BlockWithElement ret;
		public Token BLOCK_IF_CONTENT;
		public BlockBodyContext body;
		public TerminalNode STARTBLOCKWITH() { return getToken(TemplateGrammar.STARTBLOCKWITH, 0); }
		public TerminalNode BLOCK_IF_CONTENT() { return getToken(TemplateGrammar.BLOCK_IF_CONTENT, 0); }
		public TerminalNode BLOCK_IF_RBRACE() { return getToken(TemplateGrammar.BLOCK_IF_RBRACE, 0); }
		public TerminalNode STARTBLOCKEND() { return getToken(TemplateGrammar.STARTBLOCKEND, 0); }
		public BlockBodyContext blockBody() {
			return getRuleContext(BlockBodyContext.class,0);
		}
		public BlockWithElementContext(ParserRuleContext parent, int invokingState) { super(parent, invokingState); }
		public BlockWithElementContext(ParserRuleContext parent, int invokingState, Map<String,String> aliases) {
			super(parent, invokingState);
			this.aliases = aliases;
		}
		@Override public int getRuleIndex() { return RULE_blockWithElement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarListener ) ((TemplateGrammarListener)listener).enterBlockWithElement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateGrammarListener ) ((TemplateGrammarListener)listener).exitBlockWithElement(this);
		}
	}

	public final BlockWithElementContext blockWithElement(Map<String,String> aliases) throws RecognitionException {
		BlockWithElementContext _localctx = new BlockWithElementContext(_ctx, getState(), aliases);
		enterRule(_localctx, 6, RULE_blockWithElement);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(56);
			match(STARTBLOCKWITH);
			setState(57);
			((BlockWithElementContext)_localctx).BLOCK_IF_CONTENT = match(BLOCK_IF_CONTENT);
			setState(58);
			match(BLOCK_IF_RBRACE);
			setState(59);
			((BlockWithElementContext)_localctx).body = blockBody(aliases);
			setState(60);
			match(STARTBLOCKEND);

				      ((BlockWithElementContext)_localctx).ret =  new BlockWithElement(
				          StringUtils.trim( (((BlockWithElementContext)_localctx).BLOCK_IF_CONTENT!=null?((BlockWithElementContext)_localctx).BLOCK_IF_CONTENT.getText():null) ),
				          ((BlockWithElementContext)_localctx).body.ret
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
		enterRule(_localctx, 8, RULE_blockBody);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(68);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 1998L) != 0)) {
				{
				{
				setState(63);
				((BlockBodyContext)_localctx).element = element(aliases);
				 if( ((BlockBodyContext)_localctx).element.ret != null ) _localctx.ret.elements.add( ((BlockBodyContext)_localctx).element.ret ); 
				}
				}
				setState(70);
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
		enterRule(_localctx, 10, RULE_text);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(72); 
			_errHandler.sync(this);
			_alt = 1;
			do {
				switch (_alt) {
				case 1:
					{
					{
					setState(71);
					match(TEXT);
					}
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(74); 
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
		enterRule(_localctx, 12, RULE_comment);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(76);
			match(STARTESCEXPR);
			setState(77);
			expressionContent();
			setState(78);
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
		public boolean trimLeft;
		public boolean trimRight;
		public ExpressionContentContext expressionContent;
		public ExpressionContentContext expressionContent() {
			return getRuleContext(ExpressionContentContext.class,0);
		}
		public TerminalNode STARTEXPR() { return getToken(TemplateGrammar.STARTEXPR, 0); }
		public TerminalNode STARTEXPR2() { return getToken(TemplateGrammar.STARTEXPR2, 0); }
		public TerminalNode STARTEXPR2_LTRIM() { return getToken(TemplateGrammar.STARTEXPR2_LTRIM, 0); }
		public TerminalNode RBRACE() { return getToken(TemplateGrammar.RBRACE, 0); }
		public TerminalNode RBRACE2() { return getToken(TemplateGrammar.RBRACE2, 0); }
		public TerminalNode RBRACE2_RTRIM() { return getToken(TemplateGrammar.RBRACE2_RTRIM, 0); }
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
		enterRule(_localctx, 14, RULE_expression);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(80);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 896L) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			setState(81);
			((ExpressionContext)_localctx).expressionContent = expressionContent();
			setState(82);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 102400L) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}

			        ((ExpressionContext)_localctx).ret =  StringUtils.trim( (((ExpressionContext)_localctx).expressionContent!=null?_input.getText(((ExpressionContext)_localctx).expressionContent.start,((ExpressionContext)_localctx).expressionContent.stop):null) );
			        ((ExpressionContext)_localctx).trimLeft =  _localctx.start.getType() == STARTEXPR2_LTRIM;
			        ((ExpressionContext)_localctx).trimRight =  _input.LT(-1).getType() == RBRACE2_RTRIM;
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
		enterRule(_localctx, 16, RULE_expressionContent);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(86); 
			_errHandler.sync(this);
			_alt = 1;
			do {
				switch (_alt) {
				case 1:
					{
					{
					setState(85);
					_la = _input.LA(1);
					if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 227328L) != 0)) ) {
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
				setState(88); 
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
		"\u0004\u0001\u0013[\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001\u0002"+
		"\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004\u0007\u0004\u0002"+
		"\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002\u0007\u0007\u0007\u0002"+
		"\b\u0007\b\u0001\u0000\u0001\u0000\u0001\u0000\u0005\u0000\u0016\b\u0000"+
		"\n\u0000\f\u0000\u0019\t\u0000\u0001\u0000\u0001\u0000\u0001\u0001\u0001"+
		"\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001"+
		"\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001"+
		"\u0001\u0001\u0001\u0003\u0001,\b\u0001\u0001\u0002\u0001\u0002\u0001"+
		"\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0003\u00024\b\u0002\u0001"+
		"\u0002\u0001\u0002\u0001\u0002\u0001\u0003\u0001\u0003\u0001\u0003\u0001"+
		"\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0004\u0001\u0004\u0001"+
		"\u0004\u0005\u0004C\b\u0004\n\u0004\f\u0004F\t\u0004\u0001\u0005\u0004"+
		"\u0005I\b\u0005\u000b\u0005\f\u0005J\u0001\u0006\u0001\u0006\u0001\u0006"+
		"\u0001\u0006\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007"+
		"\u0001\b\u0004\bW\b\b\u000b\b\f\bX\u0001\b\u0000\u0000\t\u0000\u0002\u0004"+
		"\u0006\b\n\f\u000e\u0010\u0000\u0004\u0001\u0000\u0002\u0003\u0001\u0000"+
		"\u0007\t\u0002\u0000\f\f\u000f\u0010\u0002\u0000\u000b\u000e\u0010\u0011"+
		"Z\u0000\u0017\u0001\u0000\u0000\u0000\u0002+\u0001\u0000\u0000\u0000\u0004"+
		"-\u0001\u0000\u0000\u0000\u00068\u0001\u0000\u0000\u0000\bD\u0001\u0000"+
		"\u0000\u0000\nH\u0001\u0000\u0000\u0000\fL\u0001\u0000\u0000\u0000\u000e"+
		"P\u0001\u0000\u0000\u0000\u0010V\u0001\u0000\u0000\u0000\u0012\u0013\u0003"+
		"\u0002\u0001\u0000\u0013\u0014\u0006\u0000\uffff\uffff\u0000\u0014\u0016"+
		"\u0001\u0000\u0000\u0000\u0015\u0012\u0001\u0000\u0000\u0000\u0016\u0019"+
		"\u0001\u0000\u0000\u0000\u0017\u0015\u0001\u0000\u0000\u0000\u0017\u0018"+
		"\u0001\u0000\u0000\u0000\u0018\u001a\u0001\u0000\u0000\u0000\u0019\u0017"+
		"\u0001\u0000\u0000\u0000\u001a\u001b\u0005\u0000\u0000\u0001\u001b\u0001"+
		"\u0001\u0000\u0000\u0000\u001c\u001d\u0003\n\u0005\u0000\u001d\u001e\u0006"+
		"\u0001\uffff\uffff\u0000\u001e,\u0001\u0000\u0000\u0000\u001f \u0003\f"+
		"\u0006\u0000 !\u0006\u0001\uffff\uffff\u0000!,\u0001\u0000\u0000\u0000"+
		"\"#\u0003\u000e\u0007\u0000#$\u0006\u0001\uffff\uffff\u0000$,\u0001\u0000"+
		"\u0000\u0000%&\u0003\u0004\u0002\u0000&\'\u0006\u0001\uffff\uffff\u0000"+
		"\',\u0001\u0000\u0000\u0000()\u0003\u0006\u0003\u0000)*\u0006\u0001\uffff"+
		"\uffff\u0000*,\u0001\u0000\u0000\u0000+\u001c\u0001\u0000\u0000\u0000"+
		"+\u001f\u0001\u0000\u0000\u0000+\"\u0001\u0000\u0000\u0000+%\u0001\u0000"+
		"\u0000\u0000+(\u0001\u0000\u0000\u0000,\u0003\u0001\u0000\u0000\u0000"+
		"-.\u0007\u0000\u0000\u0000./\u0005\u0012\u0000\u0000/0\u0005\u0013\u0000"+
		"\u000003\u0003\b\u0004\u000012\u0005\u0004\u0000\u000024\u0003\b\u0004"+
		"\u000031\u0001\u0000\u0000\u000034\u0001\u0000\u0000\u000045\u0001\u0000"+
		"\u0000\u000056\u0005\u0005\u0000\u000067\u0006\u0002\uffff\uffff\u0000"+
		"7\u0005\u0001\u0000\u0000\u000089\u0005\u0001\u0000\u00009:\u0005\u0012"+
		"\u0000\u0000:;\u0005\u0013\u0000\u0000;<\u0003\b\u0004\u0000<=\u0005\u0005"+
		"\u0000\u0000=>\u0006\u0003\uffff\uffff\u0000>\u0007\u0001\u0000\u0000"+
		"\u0000?@\u0003\u0002\u0001\u0000@A\u0006\u0004\uffff\uffff\u0000AC\u0001"+
		"\u0000\u0000\u0000B?\u0001\u0000\u0000\u0000CF\u0001\u0000\u0000\u0000"+
		"DB\u0001\u0000\u0000\u0000DE\u0001\u0000\u0000\u0000E\t\u0001\u0000\u0000"+
		"\u0000FD\u0001\u0000\u0000\u0000GI\u0005\n\u0000\u0000HG\u0001\u0000\u0000"+
		"\u0000IJ\u0001\u0000\u0000\u0000JH\u0001\u0000\u0000\u0000JK\u0001\u0000"+
		"\u0000\u0000K\u000b\u0001\u0000\u0000\u0000LM\u0005\u0006\u0000\u0000"+
		"MN\u0003\u0010\b\u0000NO\u0005\f\u0000\u0000O\r\u0001\u0000\u0000\u0000"+
		"PQ\u0007\u0001\u0000\u0000QR\u0003\u0010\b\u0000RS\u0007\u0002\u0000\u0000"+
		"ST\u0006\u0007\uffff\uffff\u0000T\u000f\u0001\u0000\u0000\u0000UW\u0007"+
		"\u0003\u0000\u0000VU\u0001\u0000\u0000\u0000WX\u0001\u0000\u0000\u0000"+
		"XV\u0001\u0000\u0000\u0000XY\u0001\u0000\u0000\u0000Y\u0011\u0001\u0000"+
		"\u0000\u0000\u0006\u0017+3DJX";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}