// Generated from JPath.g4 by ANTLR 4.8

package oap.jpath;

import java.util.List;
import java.lang.Number;
import java.util.ArrayList;
import oap.util.Pair;
import static oap.util.Pair.__;

import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class JPathLexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.8", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, T__2=3, T__3=4, T__4=5, T__5=6, T__6=7, T__7=8, Identifier=9, 
		DecimalIntegerLiteral=10, StringLiteral=11, SPACE=12;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	private static String[] makeRuleNames() {
		return new String[] {
			"T__0", "T__1", "T__2", "T__3", "T__4", "T__5", "T__6", "T__7", "Identifier", 
			"DecimalIntegerLiteral", "IntegerTypeSuffix", "DecimalNumeral", "Digits", 
			"Digit", "NonZeroDigit", "DigitsAndUnderscores", "DigitOrUnderscore", 
			"Underscores", "JavaLetter", "JavaLetterOrDigit", "EscapeSequence", "StringLiteral", 
			"StringCharacters", "StringCharacter", "SPACE"
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


	public JPathLexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "JPath.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public String[] getChannelNames() { return channelNames; }

	@Override
	public String[] getModeNames() { return modeNames; }

	@Override
	public ATN getATN() { return _ATN; }

	@Override
	public void action(RuleContext _localctx, int ruleIndex, int actionIndex) {
		switch (ruleIndex) {
		case 21:
			StringLiteral_action((RuleContext)_localctx, actionIndex);
			break;
		}
	}
	private void StringLiteral_action(RuleContext _localctx, int actionIndex) {
		switch (actionIndex) {
		case 0:
			setText(getText().substring(1, getText().length() - 1));
			break;
		}
	}
	@Override
	public boolean sempred(RuleContext _localctx, int ruleIndex, int predIndex) {
		switch (ruleIndex) {
		case 18:
			return JavaLetter_sempred((RuleContext)_localctx, predIndex);
		case 19:
			return JavaLetterOrDigit_sempred((RuleContext)_localctx, predIndex);
		}
		return true;
	}
	private boolean JavaLetter_sempred(RuleContext _localctx, int predIndex) {
		switch (predIndex) {
		case 0:
			return Character.isJavaIdentifierStart(_input.LA(-1));
		case 1:
			return Character.isJavaIdentifierStart(Character.toCodePoint((char)_input.LA(-2), (char)_input.LA(-1)));
		}
		return true;
	}
	private boolean JavaLetterOrDigit_sempred(RuleContext _localctx, int predIndex) {
		switch (predIndex) {
		case 2:
			return Character.isJavaIdentifierPart(_input.LA(-1));
		case 3:
			return Character.isJavaIdentifierPart(Character.toCodePoint((char)_input.LA(-2), (char)_input.LA(-1)));
		}
		return true;
	}

	public static final String _serializedATN =
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2\16\u00a2\b\1\4\2"+
		"\t\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4"+
		"\13\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22"+
		"\t\22\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31"+
		"\t\31\4\32\t\32\3\2\3\2\3\2\3\2\3\3\3\3\3\4\3\4\3\5\3\5\3\6\3\6\3\7\3"+
		"\7\3\b\3\b\3\t\3\t\3\n\3\n\7\nJ\n\n\f\n\16\nM\13\n\3\13\3\13\5\13Q\n\13"+
		"\3\f\3\f\3\r\3\r\3\r\5\rX\n\r\3\r\3\r\3\r\5\r]\n\r\5\r_\n\r\3\16\3\16"+
		"\5\16c\n\16\3\16\5\16f\n\16\3\17\3\17\5\17j\n\17\3\20\3\20\3\21\6\21o"+
		"\n\21\r\21\16\21p\3\22\3\22\5\22u\n\22\3\23\6\23x\n\23\r\23\16\23y\3\24"+
		"\3\24\3\24\3\24\3\24\3\24\5\24\u0082\n\24\3\25\3\25\3\25\3\25\3\25\3\25"+
		"\5\25\u008a\n\25\3\26\3\26\3\26\3\27\3\27\5\27\u0091\n\27\3\27\3\27\3"+
		"\27\3\30\6\30\u0097\n\30\r\30\16\30\u0098\3\31\3\31\5\31\u009d\n\31\3"+
		"\32\3\32\3\32\3\32\2\2\33\3\3\5\4\7\5\t\6\13\7\r\b\17\t\21\n\23\13\25"+
		"\f\27\2\31\2\33\2\35\2\37\2!\2#\2%\2\'\2)\2+\2-\r/\2\61\2\63\16\3\2\f"+
		"\4\2NNnn\3\2\63;\7\2&&//C\\aac|\4\2\2\u0081\ud802\udc01\3\2\ud802\udc01"+
		"\3\2\udc02\ue001\b\2&&//\62;C\\aac|\n\2$$))^^ddhhppttvv\6\2\f\f\17\17"+
		"$$^^\4\2\f\f\"\"\2\u00a6\2\3\3\2\2\2\2\5\3\2\2\2\2\7\3\2\2\2\2\t\3\2\2"+
		"\2\2\13\3\2\2\2\2\r\3\2\2\2\2\17\3\2\2\2\2\21\3\2\2\2\2\23\3\2\2\2\2\25"+
		"\3\2\2\2\2-\3\2\2\2\2\63\3\2\2\2\3\65\3\2\2\2\59\3\2\2\2\7;\3\2\2\2\t"+
		"=\3\2\2\2\13?\3\2\2\2\rA\3\2\2\2\17C\3\2\2\2\21E\3\2\2\2\23G\3\2\2\2\25"+
		"N\3\2\2\2\27R\3\2\2\2\31^\3\2\2\2\33`\3\2\2\2\35i\3\2\2\2\37k\3\2\2\2"+
		"!n\3\2\2\2#t\3\2\2\2%w\3\2\2\2\'\u0081\3\2\2\2)\u0089\3\2\2\2+\u008b\3"+
		"\2\2\2-\u008e\3\2\2\2/\u0096\3\2\2\2\61\u009c\3\2\2\2\63\u009e\3\2\2\2"+
		"\65\66\7x\2\2\66\67\7c\2\2\678\7t\2\28\4\3\2\2\29:\7<\2\2:\6\3\2\2\2;"+
		"<\7\60\2\2<\b\3\2\2\2=>\7]\2\2>\n\3\2\2\2?@\7_\2\2@\f\3\2\2\2AB\7*\2\2"+
		"B\16\3\2\2\2CD\7+\2\2D\20\3\2\2\2EF\7.\2\2F\22\3\2\2\2GK\5\'\24\2HJ\5"+
		")\25\2IH\3\2\2\2JM\3\2\2\2KI\3\2\2\2KL\3\2\2\2L\24\3\2\2\2MK\3\2\2\2N"+
		"P\5\31\r\2OQ\5\27\f\2PO\3\2\2\2PQ\3\2\2\2Q\26\3\2\2\2RS\t\2\2\2S\30\3"+
		"\2\2\2T_\7\62\2\2U\\\5\37\20\2VX\5\33\16\2WV\3\2\2\2WX\3\2\2\2X]\3\2\2"+
		"\2YZ\5%\23\2Z[\5\33\16\2[]\3\2\2\2\\W\3\2\2\2\\Y\3\2\2\2]_\3\2\2\2^T\3"+
		"\2\2\2^U\3\2\2\2_\32\3\2\2\2`e\5\35\17\2ac\5!\21\2ba\3\2\2\2bc\3\2\2\2"+
		"cd\3\2\2\2df\5\35\17\2eb\3\2\2\2ef\3\2\2\2f\34\3\2\2\2gj\7\62\2\2hj\5"+
		"\37\20\2ig\3\2\2\2ih\3\2\2\2j\36\3\2\2\2kl\t\3\2\2l \3\2\2\2mo\5#\22\2"+
		"nm\3\2\2\2op\3\2\2\2pn\3\2\2\2pq\3\2\2\2q\"\3\2\2\2ru\5\35\17\2su\7a\2"+
		"\2tr\3\2\2\2ts\3\2\2\2u$\3\2\2\2vx\7a\2\2wv\3\2\2\2xy\3\2\2\2yw\3\2\2"+
		"\2yz\3\2\2\2z&\3\2\2\2{\u0082\t\4\2\2|}\n\5\2\2}\u0082\6\24\2\2~\177\t"+
		"\6\2\2\177\u0080\t\7\2\2\u0080\u0082\6\24\3\2\u0081{\3\2\2\2\u0081|\3"+
		"\2\2\2\u0081~\3\2\2\2\u0082(\3\2\2\2\u0083\u008a\t\b\2\2\u0084\u0085\n"+
		"\5\2\2\u0085\u008a\6\25\4\2\u0086\u0087\t\6\2\2\u0087\u0088\t\7\2\2\u0088"+
		"\u008a\6\25\5\2\u0089\u0083\3\2\2\2\u0089\u0084\3\2\2\2\u0089\u0086\3"+
		"\2\2\2\u008a*\3\2\2\2\u008b\u008c\7^\2\2\u008c\u008d\t\t\2\2\u008d,\3"+
		"\2\2\2\u008e\u0090\7$\2\2\u008f\u0091\5/\30\2\u0090\u008f\3\2\2\2\u0090"+
		"\u0091\3\2\2\2\u0091\u0092\3\2\2\2\u0092\u0093\7$\2\2\u0093\u0094\b\27"+
		"\2\2\u0094.\3\2\2\2\u0095\u0097\5\61\31\2\u0096\u0095\3\2\2\2\u0097\u0098"+
		"\3\2\2\2\u0098\u0096\3\2\2\2\u0098\u0099\3\2\2\2\u0099\60\3\2\2\2\u009a"+
		"\u009d\n\n\2\2\u009b\u009d\5+\26\2\u009c\u009a\3\2\2\2\u009c\u009b\3\2"+
		"\2\2\u009d\62\3\2\2\2\u009e\u009f\t\13\2\2\u009f\u00a0\3\2\2\2\u00a0\u00a1"+
		"\b\32\3\2\u00a1\64\3\2\2\2\23\2KPW\\^beipty\u0081\u0089\u0090\u0098\u009c"+
		"\4\3\27\2\b\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}