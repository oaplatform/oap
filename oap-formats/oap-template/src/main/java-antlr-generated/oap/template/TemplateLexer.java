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
		STARTESCEXPR=1, STARTEXPR=2, STARTEXPR2=3, TEXT=4, LBRACE=5, RBRACE=6, 
		EXPRESSION=7, LBRACE2=8, RBRACE2=9, EXPRESSION2=10;
	public static final int
		Expression=1, Expression2=2;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE", "Expression", "Expression2"
	};

	private static String[] makeRuleNames() {
		return new String[] {
			"StartEscExpr", "StartExpr", "StartExpr2", "EndExpr2", "STARTESCEXPR", 
			"STARTEXPR", "STARTEXPR2", "TEXT", "LBrace", "RBrace", "LBRACE", "RBRACE", 
			"EXPRESSION", "LBRACE2", "RBRACE2", "EXPRESSION2"
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
			null, "STARTESCEXPR", "STARTEXPR", "STARTEXPR2", "TEXT", "LBRACE", "RBRACE", 
			"EXPRESSION", "LBRACE2", "RBRACE2", "EXPRESSION2"
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
		"\u0004\u0000\nV\u0006\uffff\uffff\u0006\uffff\uffff\u0006\uffff\uffff"+
		"\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001\u0002\u0002\u0007\u0002"+
		"\u0002\u0003\u0007\u0003\u0002\u0004\u0007\u0004\u0002\u0005\u0007\u0005"+
		"\u0002\u0006\u0007\u0006\u0002\u0007\u0007\u0007\u0002\b\u0007\b\u0002"+
		"\t\u0007\t\u0002\n\u0007\n\u0002\u000b\u0007\u000b\u0002\f\u0007\f\u0002"+
		"\r\u0007\r\u0002\u000e\u0007\u000e\u0002\u000f\u0007\u000f\u0001\u0000"+
		"\u0001\u0000\u0001\u0000\u0001\u0000\u0001\u0001\u0001\u0001\u0001\u0001"+
		"\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0003\u0001\u0003\u0001\u0003"+
		"\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0005\u0001\u0005"+
		"\u0001\u0005\u0001\u0005\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0006"+
		"\u0001\u0007\u0001\u0007\u0001\b\u0001\b\u0001\t\u0001\t\u0001\n\u0001"+
		"\n\u0001\n\u0001\n\u0001\u000b\u0001\u000b\u0001\u000b\u0001\u000b\u0001"+
		"\f\u0001\f\u0001\r\u0001\r\u0001\r\u0001\r\u0001\u000e\u0001\u000e\u0001"+
		"\u000e\u0001\u000e\u0001\u000f\u0001\u000f\u0000\u0000\u0010\u0003\u0000"+
		"\u0005\u0000\u0007\u0000\t\u0000\u000b\u0001\r\u0002\u000f\u0003\u0011"+
		"\u0004\u0013\u0000\u0015\u0000\u0017\u0005\u0019\u0006\u001b\u0007\u001d"+
		"\b\u001f\t!\n\u0003\u0000\u0001\u0002\u0000M\u0000\u000b\u0001\u0000\u0000"+
		"\u0000\u0000\r\u0001\u0000\u0000\u0000\u0000\u000f\u0001\u0000\u0000\u0000"+
		"\u0000\u0011\u0001\u0000\u0000\u0000\u0001\u0017\u0001\u0000\u0000\u0000"+
		"\u0001\u0019\u0001\u0000\u0000\u0000\u0001\u001b\u0001\u0000\u0000\u0000"+
		"\u0002\u001d\u0001\u0000\u0000\u0000\u0002\u001f\u0001\u0000\u0000\u0000"+
		"\u0002!\u0001\u0000\u0000\u0000\u0003#\u0001\u0000\u0000\u0000\u0005\'"+
		"\u0001\u0000\u0000\u0000\u0007*\u0001\u0000\u0000\u0000\t-\u0001\u0000"+
		"\u0000\u0000\u000b0\u0001\u0000\u0000\u0000\r4\u0001\u0000\u0000\u0000"+
		"\u000f8\u0001\u0000\u0000\u0000\u0011<\u0001\u0000\u0000\u0000\u0013>"+
		"\u0001\u0000\u0000\u0000\u0015@\u0001\u0000\u0000\u0000\u0017B\u0001\u0000"+
		"\u0000\u0000\u0019F\u0001\u0000\u0000\u0000\u001bJ\u0001\u0000\u0000\u0000"+
		"\u001dL\u0001\u0000\u0000\u0000\u001fP\u0001\u0000\u0000\u0000!T\u0001"+
		"\u0000\u0000\u0000#$\u0005$\u0000\u0000$%\u0005$\u0000\u0000%&\u0005{"+
		"\u0000\u0000&\u0004\u0001\u0000\u0000\u0000\'(\u0005$\u0000\u0000()\u0005"+
		"{\u0000\u0000)\u0006\u0001\u0000\u0000\u0000*+\u0005{\u0000\u0000+,\u0005"+
		"{\u0000\u0000,\b\u0001\u0000\u0000\u0000-.\u0005}\u0000\u0000./\u0005"+
		"}\u0000\u0000/\n\u0001\u0000\u0000\u000001\u0003\u0003\u0000\u000012\u0001"+
		"\u0000\u0000\u000023\u0006\u0004\u0000\u00003\f\u0001\u0000\u0000\u0000"+
		"45\u0003\u0005\u0001\u000056\u0001\u0000\u0000\u000067\u0006\u0005\u0000"+
		"\u00007\u000e\u0001\u0000\u0000\u000089\u0003\u0007\u0002\u00009:\u0001"+
		"\u0000\u0000\u0000:;\u0006\u0006\u0001\u0000;\u0010\u0001\u0000\u0000"+
		"\u0000<=\t\u0000\u0000\u0000=\u0012\u0001\u0000\u0000\u0000>?\u0005{\u0000"+
		"\u0000?\u0014\u0001\u0000\u0000\u0000@A\u0005}\u0000\u0000A\u0016\u0001"+
		"\u0000\u0000\u0000BC\u0003\u0013\b\u0000CD\u0001\u0000\u0000\u0000DE\u0006"+
		"\n\u0000\u0000E\u0018\u0001\u0000\u0000\u0000FG\u0003\u0015\t\u0000GH"+
		"\u0001\u0000\u0000\u0000HI\u0006\u000b\u0002\u0000I\u001a\u0001\u0000"+
		"\u0000\u0000JK\t\u0000\u0000\u0000K\u001c\u0001\u0000\u0000\u0000LM\u0003"+
		"\u0013\b\u0000MN\u0001\u0000\u0000\u0000NO\u0006\r\u0000\u0000O\u001e"+
		"\u0001\u0000\u0000\u0000PQ\u0003\t\u0003\u0000QR\u0001\u0000\u0000\u0000"+
		"RS\u0006\u000e\u0002\u0000S \u0001\u0000\u0000\u0000TU\t\u0000\u0000\u0000"+
		"U\"\u0001\u0000\u0000\u0000\u0003\u0000\u0001\u0002\u0003\u0005\u0001"+
		"\u0000\u0005\u0002\u0000\u0004\u0000\u0000";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}