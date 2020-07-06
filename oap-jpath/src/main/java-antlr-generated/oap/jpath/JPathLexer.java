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
			"DecimalIntegerLiteral", "DecimalNumeral", "Digits", "Digit", "NonZeroDigit", 
			"DigitsAndUnderscores", "DigitOrUnderscore", "Underscores", "JavaLetter", 
			"JavaLetterOrDigit", "EscapeSequence", "StringLiteral", "StringCharacters", 
			"StringCharacter", "SPACE"
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
		case 20:
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
		case 17:
			return JavaLetter_sempred((RuleContext)_localctx, predIndex);
		case 18:
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
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2\16\u009b\b\1\4\2"+
		"\t\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4"+
		"\13\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22"+
		"\t\22\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31"+
		"\t\31\3\2\3\2\3\2\3\3\3\3\3\4\3\4\3\5\3\5\3\6\3\6\3\7\3\7\3\b\3\b\3\t"+
		"\3\t\3\n\3\n\7\nG\n\n\f\n\16\nJ\13\n\3\13\3\13\3\f\3\f\3\f\5\fQ\n\f\3"+
		"\f\3\f\3\f\5\fV\n\f\5\fX\n\f\3\r\3\r\5\r\\\n\r\3\r\5\r_\n\r\3\16\3\16"+
		"\5\16c\n\16\3\17\3\17\3\20\6\20h\n\20\r\20\16\20i\3\21\3\21\5\21n\n\21"+
		"\3\22\6\22q\n\22\r\22\16\22r\3\23\3\23\3\23\3\23\3\23\3\23\5\23{\n\23"+
		"\3\24\3\24\3\24\3\24\3\24\3\24\5\24\u0083\n\24\3\25\3\25\3\25\3\26\3\26"+
		"\5\26\u008a\n\26\3\26\3\26\3\26\3\27\6\27\u0090\n\27\r\27\16\27\u0091"+
		"\3\30\3\30\5\30\u0096\n\30\3\31\3\31\3\31\3\31\2\2\32\3\3\5\4\7\5\t\6"+
		"\13\7\r\b\17\t\21\n\23\13\25\f\27\2\31\2\33\2\35\2\37\2!\2#\2%\2\'\2)"+
		"\2+\r-\2/\2\61\16\3\2\13\3\2\63;\7\2&&//C\\aac|\4\2\2\u0081\ud802\udc01"+
		"\3\2\ud802\udc01\3\2\udc02\ue001\b\2&&//\62;C\\aac|\n\2$$))^^ddhhpptt"+
		"vv\6\2\f\f\17\17$$^^\4\2\f\f\"\"\2\u009f\2\3\3\2\2\2\2\5\3\2\2\2\2\7\3"+
		"\2\2\2\2\t\3\2\2\2\2\13\3\2\2\2\2\r\3\2\2\2\2\17\3\2\2\2\2\21\3\2\2\2"+
		"\2\23\3\2\2\2\2\25\3\2\2\2\2+\3\2\2\2\2\61\3\2\2\2\3\63\3\2\2\2\5\66\3"+
		"\2\2\2\78\3\2\2\2\t:\3\2\2\2\13<\3\2\2\2\r>\3\2\2\2\17@\3\2\2\2\21B\3"+
		"\2\2\2\23D\3\2\2\2\25K\3\2\2\2\27W\3\2\2\2\31Y\3\2\2\2\33b\3\2\2\2\35"+
		"d\3\2\2\2\37g\3\2\2\2!m\3\2\2\2#p\3\2\2\2%z\3\2\2\2\'\u0082\3\2\2\2)\u0084"+
		"\3\2\2\2+\u0087\3\2\2\2-\u008f\3\2\2\2/\u0095\3\2\2\2\61\u0097\3\2\2\2"+
		"\63\64\7&\2\2\64\65\7}\2\2\65\4\3\2\2\2\66\67\7\60\2\2\67\6\3\2\2\289"+
		"\7\177\2\29\b\3\2\2\2:;\7]\2\2;\n\3\2\2\2<=\7_\2\2=\f\3\2\2\2>?\7*\2\2"+
		"?\16\3\2\2\2@A\7+\2\2A\20\3\2\2\2BC\7.\2\2C\22\3\2\2\2DH\5%\23\2EG\5\'"+
		"\24\2FE\3\2\2\2GJ\3\2\2\2HF\3\2\2\2HI\3\2\2\2I\24\3\2\2\2JH\3\2\2\2KL"+
		"\5\27\f\2L\26\3\2\2\2MX\7\62\2\2NU\5\35\17\2OQ\5\31\r\2PO\3\2\2\2PQ\3"+
		"\2\2\2QV\3\2\2\2RS\5#\22\2ST\5\31\r\2TV\3\2\2\2UP\3\2\2\2UR\3\2\2\2VX"+
		"\3\2\2\2WM\3\2\2\2WN\3\2\2\2X\30\3\2\2\2Y^\5\33\16\2Z\\\5\37\20\2[Z\3"+
		"\2\2\2[\\\3\2\2\2\\]\3\2\2\2]_\5\33\16\2^[\3\2\2\2^_\3\2\2\2_\32\3\2\2"+
		"\2`c\7\62\2\2ac\5\35\17\2b`\3\2\2\2ba\3\2\2\2c\34\3\2\2\2de\t\2\2\2e\36"+
		"\3\2\2\2fh\5!\21\2gf\3\2\2\2hi\3\2\2\2ig\3\2\2\2ij\3\2\2\2j \3\2\2\2k"+
		"n\5\33\16\2ln\7a\2\2mk\3\2\2\2ml\3\2\2\2n\"\3\2\2\2oq\7a\2\2po\3\2\2\2"+
		"qr\3\2\2\2rp\3\2\2\2rs\3\2\2\2s$\3\2\2\2t{\t\3\2\2uv\n\4\2\2v{\6\23\2"+
		"\2wx\t\5\2\2xy\t\6\2\2y{\6\23\3\2zt\3\2\2\2zu\3\2\2\2zw\3\2\2\2{&\3\2"+
		"\2\2|\u0083\t\7\2\2}~\n\4\2\2~\u0083\6\24\4\2\177\u0080\t\5\2\2\u0080"+
		"\u0081\t\6\2\2\u0081\u0083\6\24\5\2\u0082|\3\2\2\2\u0082}\3\2\2\2\u0082"+
		"\177\3\2\2\2\u0083(\3\2\2\2\u0084\u0085\7^\2\2\u0085\u0086\t\b\2\2\u0086"+
		"*\3\2\2\2\u0087\u0089\7$\2\2\u0088\u008a\5-\27\2\u0089\u0088\3\2\2\2\u0089"+
		"\u008a\3\2\2\2\u008a\u008b\3\2\2\2\u008b\u008c\7$\2\2\u008c\u008d\b\26"+
		"\2\2\u008d,\3\2\2\2\u008e\u0090\5/\30\2\u008f\u008e\3\2\2\2\u0090\u0091"+
		"\3\2\2\2\u0091\u008f\3\2\2\2\u0091\u0092\3\2\2\2\u0092.\3\2\2\2\u0093"+
		"\u0096\n\t\2\2\u0094\u0096\5)\25\2\u0095\u0093\3\2\2\2\u0095\u0094\3\2"+
		"\2\2\u0096\60\3\2\2\2\u0097\u0098\t\n\2\2\u0098\u0099\3\2\2\2\u0099\u009a"+
		"\b\31\3\2\u009a\62\3\2\2\2\22\2HPUW[^bimrz\u0082\u0089\u0091\u0095\4\3"+
		"\26\2\b\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}