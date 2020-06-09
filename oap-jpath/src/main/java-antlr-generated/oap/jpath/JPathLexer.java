// Generated from JPath.g4 by ANTLR 4.8

package oap.jpath;

import java.util.List;
import java.util.ArrayList;

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
		T__0=1, T__1=2, T__2=3, T__3=4, T__4=5, Identifier=6, StringLiteral=7;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	private static String[] makeRuleNames() {
		return new String[] {
			"T__0", "T__1", "T__2", "T__3", "T__4", "Identifier", "Digit", "JavaLetter", 
			"JavaLetterOrDigit", "EscapeSequence", "StringLiteral", "StringCharacters", 
			"StringCharacter"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "'var'", "':'", "'.'", "'('", "')'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, null, null, null, null, null, "Identifier", "StringLiteral"
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
	public boolean sempred(RuleContext _localctx, int ruleIndex, int predIndex) {
		switch (ruleIndex) {
		case 7:
			return JavaLetter_sempred((RuleContext)_localctx, predIndex);
		case 8:
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
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2\tT\b\1\4\2\t\2\4"+
		"\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13\t"+
		"\13\4\f\t\f\4\r\t\r\4\16\t\16\3\2\3\2\3\2\3\2\3\3\3\3\3\4\3\4\3\5\3\5"+
		"\3\6\3\6\3\7\3\7\7\7,\n\7\f\7\16\7/\13\7\3\b\3\b\3\t\3\t\3\t\3\t\3\t\3"+
		"\t\5\t9\n\t\3\n\3\n\3\n\3\n\3\n\3\n\5\nA\n\n\3\13\3\13\3\13\3\f\3\f\5"+
		"\fH\n\f\3\f\3\f\3\r\6\rM\n\r\r\r\16\rN\3\16\3\16\5\16S\n\16\2\2\17\3\3"+
		"\5\4\7\5\t\6\13\7\r\b\17\2\21\2\23\2\25\2\27\t\31\2\33\2\3\2\n\3\2\62"+
		";\7\2&&//C\\aac|\4\2\2\u0081\ud802\udc01\3\2\ud802\udc01\3\2\udc02\ue001"+
		"\b\2&&//\62;C\\aac|\n\2$$))^^ddhhppttvv\6\2\f\f\17\17$$^^\2U\2\3\3\2\2"+
		"\2\2\5\3\2\2\2\2\7\3\2\2\2\2\t\3\2\2\2\2\13\3\2\2\2\2\r\3\2\2\2\2\27\3"+
		"\2\2\2\3\35\3\2\2\2\5!\3\2\2\2\7#\3\2\2\2\t%\3\2\2\2\13\'\3\2\2\2\r)\3"+
		"\2\2\2\17\60\3\2\2\2\218\3\2\2\2\23@\3\2\2\2\25B\3\2\2\2\27E\3\2\2\2\31"+
		"L\3\2\2\2\33R\3\2\2\2\35\36\7x\2\2\36\37\7c\2\2\37 \7t\2\2 \4\3\2\2\2"+
		"!\"\7<\2\2\"\6\3\2\2\2#$\7\60\2\2$\b\3\2\2\2%&\7*\2\2&\n\3\2\2\2\'(\7"+
		"+\2\2(\f\3\2\2\2)-\5\21\t\2*,\5\23\n\2+*\3\2\2\2,/\3\2\2\2-+\3\2\2\2-"+
		".\3\2\2\2.\16\3\2\2\2/-\3\2\2\2\60\61\t\2\2\2\61\20\3\2\2\2\629\t\3\2"+
		"\2\63\64\n\4\2\2\649\6\t\2\2\65\66\t\5\2\2\66\67\t\6\2\2\679\6\t\3\28"+
		"\62\3\2\2\28\63\3\2\2\28\65\3\2\2\29\22\3\2\2\2:A\t\7\2\2;<\n\4\2\2<A"+
		"\6\n\4\2=>\t\5\2\2>?\t\6\2\2?A\6\n\5\2@:\3\2\2\2@;\3\2\2\2@=\3\2\2\2A"+
		"\24\3\2\2\2BC\7^\2\2CD\t\b\2\2D\26\3\2\2\2EG\7$\2\2FH\5\31\r\2GF\3\2\2"+
		"\2GH\3\2\2\2HI\3\2\2\2IJ\7$\2\2J\30\3\2\2\2KM\5\33\16\2LK\3\2\2\2MN\3"+
		"\2\2\2NL\3\2\2\2NO\3\2\2\2O\32\3\2\2\2PS\n\t\2\2QS\5\25\13\2RP\3\2\2\2"+
		"RQ\3\2\2\2S\34\3\2\2\2\t\2-8@GNR\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}