// Generated from JPath.g4 by ANTLR 4.8

package oap.jpath;

import java.util.List;
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
		T__0=1, T__1=2, T__2=3, T__3=4, T__4=5, T__5=6, Identifier=7, StringLiteral=8, 
		SPACE=9;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	private static String[] makeRuleNames() {
		return new String[] {
			"T__0", "T__1", "T__2", "T__3", "T__4", "T__5", "Identifier", "Digit", 
			"JavaLetter", "JavaLetterOrDigit", "EscapeSequence", "StringLiteral", 
			"StringCharacters", "StringCharacter", "SPACE"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "'var'", "':'", "'.'", "'('", "')'", "','"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, null, null, null, null, null, null, "Identifier", "StringLiteral", 
			"SPACE"
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
		case 11:
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
		case 8:
			return JavaLetter_sempred((RuleContext)_localctx, predIndex);
		case 9:
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
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2\13_\b\1\4\2\t\2\4"+
		"\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13\t"+
		"\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\3\2\3\2\3\2\3\2\3\3"+
		"\3\3\3\4\3\4\3\5\3\5\3\6\3\6\3\7\3\7\3\b\3\b\7\b\62\n\b\f\b\16\b\65\13"+
		"\b\3\t\3\t\3\n\3\n\3\n\3\n\3\n\3\n\5\n?\n\n\3\13\3\13\3\13\3\13\3\13\3"+
		"\13\5\13G\n\13\3\f\3\f\3\f\3\r\3\r\5\rN\n\r\3\r\3\r\3\r\3\16\6\16T\n\16"+
		"\r\16\16\16U\3\17\3\17\5\17Z\n\17\3\20\3\20\3\20\3\20\2\2\21\3\3\5\4\7"+
		"\5\t\6\13\7\r\b\17\t\21\2\23\2\25\2\27\2\31\n\33\2\35\2\37\13\3\2\13\3"+
		"\2\62;\7\2&&//C\\aac|\4\2\2\u0081\ud802\udc01\3\2\ud802\udc01\3\2\udc02"+
		"\ue001\b\2&&//\62;C\\aac|\n\2$$))^^ddhhppttvv\6\2\f\f\17\17$$^^\4\2\f"+
		"\f\"\"\2`\2\3\3\2\2\2\2\5\3\2\2\2\2\7\3\2\2\2\2\t\3\2\2\2\2\13\3\2\2\2"+
		"\2\r\3\2\2\2\2\17\3\2\2\2\2\31\3\2\2\2\2\37\3\2\2\2\3!\3\2\2\2\5%\3\2"+
		"\2\2\7\'\3\2\2\2\t)\3\2\2\2\13+\3\2\2\2\r-\3\2\2\2\17/\3\2\2\2\21\66\3"+
		"\2\2\2\23>\3\2\2\2\25F\3\2\2\2\27H\3\2\2\2\31K\3\2\2\2\33S\3\2\2\2\35"+
		"Y\3\2\2\2\37[\3\2\2\2!\"\7x\2\2\"#\7c\2\2#$\7t\2\2$\4\3\2\2\2%&\7<\2\2"+
		"&\6\3\2\2\2\'(\7\60\2\2(\b\3\2\2\2)*\7*\2\2*\n\3\2\2\2+,\7+\2\2,\f\3\2"+
		"\2\2-.\7.\2\2.\16\3\2\2\2/\63\5\23\n\2\60\62\5\25\13\2\61\60\3\2\2\2\62"+
		"\65\3\2\2\2\63\61\3\2\2\2\63\64\3\2\2\2\64\20\3\2\2\2\65\63\3\2\2\2\66"+
		"\67\t\2\2\2\67\22\3\2\2\28?\t\3\2\29:\n\4\2\2:?\6\n\2\2;<\t\5\2\2<=\t"+
		"\6\2\2=?\6\n\3\2>8\3\2\2\2>9\3\2\2\2>;\3\2\2\2?\24\3\2\2\2@G\t\7\2\2A"+
		"B\n\4\2\2BG\6\13\4\2CD\t\5\2\2DE\t\6\2\2EG\6\13\5\2F@\3\2\2\2FA\3\2\2"+
		"\2FC\3\2\2\2G\26\3\2\2\2HI\7^\2\2IJ\t\b\2\2J\30\3\2\2\2KM\7$\2\2LN\5\33"+
		"\16\2ML\3\2\2\2MN\3\2\2\2NO\3\2\2\2OP\7$\2\2PQ\b\r\2\2Q\32\3\2\2\2RT\5"+
		"\35\17\2SR\3\2\2\2TU\3\2\2\2US\3\2\2\2UV\3\2\2\2V\34\3\2\2\2WZ\n\t\2\2"+
		"XZ\5\27\f\2YW\3\2\2\2YX\3\2\2\2Z\36\3\2\2\2[\\\t\n\2\2\\]\3\2\2\2]^\b"+
		"\20\3\2^ \3\2\2\2\t\2\63>FMUY\4\3\r\2\b\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}