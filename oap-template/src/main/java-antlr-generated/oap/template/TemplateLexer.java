// Generated from TemplateLexer.g4 by ANTLR 4.13.0

package oap.template;

import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast", "CheckReturnValue"})
public class TemplateLexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.13.0", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		STARTESCEXPR=1, STARTEXPR=2, TEXT=3, LBRACE=4, RBRACE=5, EXPRESSION=6;
	public static final int
		Expression=1;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE", "Expression"
	};

	private static String[] makeRuleNames() {
		return new String[] {
			"StartEscExpr", "StartExpr", "STARTESCEXPR", "STARTEXPR", "TEXT", "LBrace", 
			"RBrace", "LBRACE", "RBRACE", "EXPRESSION"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "STARTESCEXPR", "STARTEXPR", "TEXT", "LBRACE", "RBRACE", "EXPRESSION"
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


	public TemplateLexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "TemplateLexer.g4"; }

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

	public static final String _serializedATN =
		"\u0004\u0000\u00065\u0006\uffff\uffff\u0006\uffff\uffff\u0002\u0000\u0007"+
		"\u0000\u0002\u0001\u0007\u0001\u0002\u0002\u0007\u0002\u0002\u0003\u0007"+
		"\u0003\u0002\u0004\u0007\u0004\u0002\u0005\u0007\u0005\u0002\u0006\u0007"+
		"\u0006\u0002\u0007\u0007\u0007\u0002\b\u0007\b\u0002\t\u0007\t\u0001\u0000"+
		"\u0001\u0000\u0001\u0000\u0001\u0000\u0001\u0001\u0001\u0001\u0001\u0001"+
		"\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0003\u0001\u0003"+
		"\u0001\u0003\u0001\u0003\u0001\u0004\u0001\u0004\u0001\u0005\u0001\u0005"+
		"\u0001\u0006\u0001\u0006\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007"+
		"\u0001\b\u0001\b\u0001\b\u0001\b\u0001\t\u0001\t\u0000\u0000\n\u0002\u0000"+
		"\u0004\u0000\u0006\u0001\b\u0002\n\u0003\f\u0000\u000e\u0000\u0010\u0004"+
		"\u0012\u0005\u0014\u0006\u0002\u0000\u0001\u0000/\u0000\u0006\u0001\u0000"+
		"\u0000\u0000\u0000\b\u0001\u0000\u0000\u0000\u0000\n\u0001\u0000\u0000"+
		"\u0000\u0001\u0010\u0001\u0000\u0000\u0000\u0001\u0012\u0001\u0000\u0000"+
		"\u0000\u0001\u0014\u0001\u0000\u0000\u0000\u0002\u0016\u0001\u0000\u0000"+
		"\u0000\u0004\u001a\u0001\u0000\u0000\u0000\u0006\u001d\u0001\u0000\u0000"+
		"\u0000\b!\u0001\u0000\u0000\u0000\n%\u0001\u0000\u0000\u0000\f\'\u0001"+
		"\u0000\u0000\u0000\u000e)\u0001\u0000\u0000\u0000\u0010+\u0001\u0000\u0000"+
		"\u0000\u0012/\u0001\u0000\u0000\u0000\u00143\u0001\u0000\u0000\u0000\u0016"+
		"\u0017\u0005$\u0000\u0000\u0017\u0018\u0005$\u0000\u0000\u0018\u0019\u0005"+
		"{\u0000\u0000\u0019\u0003\u0001\u0000\u0000\u0000\u001a\u001b\u0005$\u0000"+
		"\u0000\u001b\u001c\u0005{\u0000\u0000\u001c\u0005\u0001\u0000\u0000\u0000"+
		"\u001d\u001e\u0003\u0002\u0000\u0000\u001e\u001f\u0001\u0000\u0000\u0000"+
		"\u001f \u0006\u0002\u0000\u0000 \u0007\u0001\u0000\u0000\u0000!\"\u0003"+
		"\u0004\u0001\u0000\"#\u0001\u0000\u0000\u0000#$\u0006\u0003\u0000\u0000"+
		"$\t\u0001\u0000\u0000\u0000%&\t\u0000\u0000\u0000&\u000b\u0001\u0000\u0000"+
		"\u0000\'(\u0005{\u0000\u0000(\r\u0001\u0000\u0000\u0000)*\u0005}\u0000"+
		"\u0000*\u000f\u0001\u0000\u0000\u0000+,\u0003\f\u0005\u0000,-\u0001\u0000"+
		"\u0000\u0000-.\u0006\u0007\u0000\u0000.\u0011\u0001\u0000\u0000\u0000"+
		"/0\u0003\u000e\u0006\u000001\u0001\u0000\u0000\u000012\u0006\b\u0001\u0000"+
		"2\u0013\u0001\u0000\u0000\u000034\t\u0000\u0000\u00004\u0015\u0001\u0000"+
		"\u0000\u0000\u0002\u0000\u0001\u0002\u0005\u0001\u0000\u0004\u0000\u0000";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}