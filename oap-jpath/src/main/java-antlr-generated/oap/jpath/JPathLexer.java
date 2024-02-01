// Generated from JPath.g4 by ANTLR 4.13.0

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

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast", "CheckReturnValue"})
public class JPathLexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.13.0", RuntimeMetaData.VERSION); }

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
		"\u0004\u0000\f\u0099\u0006\uffff\uffff\u0002\u0000\u0007\u0000\u0002\u0001"+
		"\u0007\u0001\u0002\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004"+
		"\u0007\u0004\u0002\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002\u0007"+
		"\u0007\u0007\u0002\b\u0007\b\u0002\t\u0007\t\u0002\n\u0007\n\u0002\u000b"+
		"\u0007\u000b\u0002\f\u0007\f\u0002\r\u0007\r\u0002\u000e\u0007\u000e\u0002"+
		"\u000f\u0007\u000f\u0002\u0010\u0007\u0010\u0002\u0011\u0007\u0011\u0002"+
		"\u0012\u0007\u0012\u0002\u0013\u0007\u0013\u0002\u0014\u0007\u0014\u0002"+
		"\u0015\u0007\u0015\u0002\u0016\u0007\u0016\u0002\u0017\u0007\u0017\u0001"+
		"\u0000\u0001\u0000\u0001\u0000\u0001\u0001\u0001\u0001\u0001\u0002\u0001"+
		"\u0002\u0001\u0003\u0001\u0003\u0001\u0004\u0001\u0004\u0001\u0005\u0001"+
		"\u0005\u0001\u0006\u0001\u0006\u0001\u0007\u0001\u0007\u0001\b\u0001\b"+
		"\u0005\bE\b\b\n\b\f\bH\t\b\u0001\t\u0001\t\u0001\n\u0001\n\u0001\n\u0003"+
		"\nO\b\n\u0001\n\u0001\n\u0001\n\u0003\nT\b\n\u0003\nV\b\n\u0001\u000b"+
		"\u0001\u000b\u0003\u000bZ\b\u000b\u0001\u000b\u0003\u000b]\b\u000b\u0001"+
		"\f\u0001\f\u0003\fa\b\f\u0001\r\u0001\r\u0001\u000e\u0004\u000ef\b\u000e"+
		"\u000b\u000e\f\u000eg\u0001\u000f\u0001\u000f\u0003\u000fl\b\u000f\u0001"+
		"\u0010\u0004\u0010o\b\u0010\u000b\u0010\f\u0010p\u0001\u0011\u0001\u0011"+
		"\u0001\u0011\u0001\u0011\u0001\u0011\u0001\u0011\u0003\u0011y\b\u0011"+
		"\u0001\u0012\u0001\u0012\u0001\u0012\u0001\u0012\u0001\u0012\u0001\u0012"+
		"\u0003\u0012\u0081\b\u0012\u0001\u0013\u0001\u0013\u0001\u0013\u0001\u0014"+
		"\u0001\u0014\u0003\u0014\u0088\b\u0014\u0001\u0014\u0001\u0014\u0001\u0014"+
		"\u0001\u0015\u0004\u0015\u008e\b\u0015\u000b\u0015\f\u0015\u008f\u0001"+
		"\u0016\u0001\u0016\u0003\u0016\u0094\b\u0016\u0001\u0017\u0001\u0017\u0001"+
		"\u0017\u0001\u0017\u0000\u0000\u0018\u0001\u0001\u0003\u0002\u0005\u0003"+
		"\u0007\u0004\t\u0005\u000b\u0006\r\u0007\u000f\b\u0011\t\u0013\n\u0015"+
		"\u0000\u0017\u0000\u0019\u0000\u001b\u0000\u001d\u0000\u001f\u0000!\u0000"+
		"#\u0000%\u0000\'\u0000)\u000b+\u0000-\u0000/\f\u0001\u0000\t\u0001\u0000"+
		"19\u0005\u0000$$--AZ__az\u0002\u0000\u0000\u007f\u8000\ud800\u8000\udbff"+
		"\u0001\u0000\u8000\ud800\u8000\udbff\u0001\u0000\u8000\udc00\u8000\udfff"+
		"\u0006\u0000$$--09AZ__az\b\u0000\"\"\'\'\\\\bbffnnrrtt\u0004\u0000\n\n"+
		"\r\r\"\"\\\\\u0002\u0000\n\n  \u009d\u0000\u0001\u0001\u0000\u0000\u0000"+
		"\u0000\u0003\u0001\u0000\u0000\u0000\u0000\u0005\u0001\u0000\u0000\u0000"+
		"\u0000\u0007\u0001\u0000\u0000\u0000\u0000\t\u0001\u0000\u0000\u0000\u0000"+
		"\u000b\u0001\u0000\u0000\u0000\u0000\r\u0001\u0000\u0000\u0000\u0000\u000f"+
		"\u0001\u0000\u0000\u0000\u0000\u0011\u0001\u0000\u0000\u0000\u0000\u0013"+
		"\u0001\u0000\u0000\u0000\u0000)\u0001\u0000\u0000\u0000\u0000/\u0001\u0000"+
		"\u0000\u0000\u00011\u0001\u0000\u0000\u0000\u00034\u0001\u0000\u0000\u0000"+
		"\u00056\u0001\u0000\u0000\u0000\u00078\u0001\u0000\u0000\u0000\t:\u0001"+
		"\u0000\u0000\u0000\u000b<\u0001\u0000\u0000\u0000\r>\u0001\u0000\u0000"+
		"\u0000\u000f@\u0001\u0000\u0000\u0000\u0011B\u0001\u0000\u0000\u0000\u0013"+
		"I\u0001\u0000\u0000\u0000\u0015U\u0001\u0000\u0000\u0000\u0017W\u0001"+
		"\u0000\u0000\u0000\u0019`\u0001\u0000\u0000\u0000\u001bb\u0001\u0000\u0000"+
		"\u0000\u001de\u0001\u0000\u0000\u0000\u001fk\u0001\u0000\u0000\u0000!"+
		"n\u0001\u0000\u0000\u0000#x\u0001\u0000\u0000\u0000%\u0080\u0001\u0000"+
		"\u0000\u0000\'\u0082\u0001\u0000\u0000\u0000)\u0085\u0001\u0000\u0000"+
		"\u0000+\u008d\u0001\u0000\u0000\u0000-\u0093\u0001\u0000\u0000\u0000/"+
		"\u0095\u0001\u0000\u0000\u000012\u0005$\u0000\u000023\u0005{\u0000\u0000"+
		"3\u0002\u0001\u0000\u0000\u000045\u0005.\u0000\u00005\u0004\u0001\u0000"+
		"\u0000\u000067\u0005}\u0000\u00007\u0006\u0001\u0000\u0000\u000089\u0005"+
		"[\u0000\u00009\b\u0001\u0000\u0000\u0000:;\u0005]\u0000\u0000;\n\u0001"+
		"\u0000\u0000\u0000<=\u0005(\u0000\u0000=\f\u0001\u0000\u0000\u0000>?\u0005"+
		")\u0000\u0000?\u000e\u0001\u0000\u0000\u0000@A\u0005,\u0000\u0000A\u0010"+
		"\u0001\u0000\u0000\u0000BF\u0003#\u0011\u0000CE\u0003%\u0012\u0000DC\u0001"+
		"\u0000\u0000\u0000EH\u0001\u0000\u0000\u0000FD\u0001\u0000\u0000\u0000"+
		"FG\u0001\u0000\u0000\u0000G\u0012\u0001\u0000\u0000\u0000HF\u0001\u0000"+
		"\u0000\u0000IJ\u0003\u0015\n\u0000J\u0014\u0001\u0000\u0000\u0000KV\u0005"+
		"0\u0000\u0000LS\u0003\u001b\r\u0000MO\u0003\u0017\u000b\u0000NM\u0001"+
		"\u0000\u0000\u0000NO\u0001\u0000\u0000\u0000OT\u0001\u0000\u0000\u0000"+
		"PQ\u0003!\u0010\u0000QR\u0003\u0017\u000b\u0000RT\u0001\u0000\u0000\u0000"+
		"SN\u0001\u0000\u0000\u0000SP\u0001\u0000\u0000\u0000TV\u0001\u0000\u0000"+
		"\u0000UK\u0001\u0000\u0000\u0000UL\u0001\u0000\u0000\u0000V\u0016\u0001"+
		"\u0000\u0000\u0000W\\\u0003\u0019\f\u0000XZ\u0003\u001d\u000e\u0000YX"+
		"\u0001\u0000\u0000\u0000YZ\u0001\u0000\u0000\u0000Z[\u0001\u0000\u0000"+
		"\u0000[]\u0003\u0019\f\u0000\\Y\u0001\u0000\u0000\u0000\\]\u0001\u0000"+
		"\u0000\u0000]\u0018\u0001\u0000\u0000\u0000^a\u00050\u0000\u0000_a\u0003"+
		"\u001b\r\u0000`^\u0001\u0000\u0000\u0000`_\u0001\u0000\u0000\u0000a\u001a"+
		"\u0001\u0000\u0000\u0000bc\u0007\u0000\u0000\u0000c\u001c\u0001\u0000"+
		"\u0000\u0000df\u0003\u001f\u000f\u0000ed\u0001\u0000\u0000\u0000fg\u0001"+
		"\u0000\u0000\u0000ge\u0001\u0000\u0000\u0000gh\u0001\u0000\u0000\u0000"+
		"h\u001e\u0001\u0000\u0000\u0000il\u0003\u0019\f\u0000jl\u0005_\u0000\u0000"+
		"ki\u0001\u0000\u0000\u0000kj\u0001\u0000\u0000\u0000l \u0001\u0000\u0000"+
		"\u0000mo\u0005_\u0000\u0000nm\u0001\u0000\u0000\u0000op\u0001\u0000\u0000"+
		"\u0000pn\u0001\u0000\u0000\u0000pq\u0001\u0000\u0000\u0000q\"\u0001\u0000"+
		"\u0000\u0000ry\u0007\u0001\u0000\u0000st\b\u0002\u0000\u0000ty\u0004\u0011"+
		"\u0000\u0000uv\u0007\u0003\u0000\u0000vw\u0007\u0004\u0000\u0000wy\u0004"+
		"\u0011\u0001\u0000xr\u0001\u0000\u0000\u0000xs\u0001\u0000\u0000\u0000"+
		"xu\u0001\u0000\u0000\u0000y$\u0001\u0000\u0000\u0000z\u0081\u0007\u0005"+
		"\u0000\u0000{|\b\u0002\u0000\u0000|\u0081\u0004\u0012\u0002\u0000}~\u0007"+
		"\u0003\u0000\u0000~\u007f\u0007\u0004\u0000\u0000\u007f\u0081\u0004\u0012"+
		"\u0003\u0000\u0080z\u0001\u0000\u0000\u0000\u0080{\u0001\u0000\u0000\u0000"+
		"\u0080}\u0001\u0000\u0000\u0000\u0081&\u0001\u0000\u0000\u0000\u0082\u0083"+
		"\u0005\\\u0000\u0000\u0083\u0084\u0007\u0006\u0000\u0000\u0084(\u0001"+
		"\u0000\u0000\u0000\u0085\u0087\u0005\"\u0000\u0000\u0086\u0088\u0003+"+
		"\u0015\u0000\u0087\u0086\u0001\u0000\u0000\u0000\u0087\u0088\u0001\u0000"+
		"\u0000\u0000\u0088\u0089\u0001\u0000\u0000\u0000\u0089\u008a\u0005\"\u0000"+
		"\u0000\u008a\u008b\u0006\u0014\u0000\u0000\u008b*\u0001\u0000\u0000\u0000"+
		"\u008c\u008e\u0003-\u0016\u0000\u008d\u008c\u0001\u0000\u0000\u0000\u008e"+
		"\u008f\u0001\u0000\u0000\u0000\u008f\u008d\u0001\u0000\u0000\u0000\u008f"+
		"\u0090\u0001\u0000\u0000\u0000\u0090,\u0001\u0000\u0000\u0000\u0091\u0094"+
		"\b\u0007\u0000\u0000\u0092\u0094\u0003\'\u0013\u0000\u0093\u0091\u0001"+
		"\u0000\u0000\u0000\u0093\u0092\u0001\u0000\u0000\u0000\u0094.\u0001\u0000"+
		"\u0000\u0000\u0095\u0096\u0007\b\u0000\u0000\u0096\u0097\u0001\u0000\u0000"+
		"\u0000\u0097\u0098\u0006\u0017\u0001\u0000\u00980\u0001\u0000\u0000\u0000"+
		"\u0010\u0000FNSUY\\`gkpx\u0080\u0087\u008f\u0093\u0002\u0001\u0014\u0000"+
		"\u0006\u0000\u0000";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}