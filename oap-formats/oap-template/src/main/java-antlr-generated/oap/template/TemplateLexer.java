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
		STARTBLOCKIF=1, STARTBLOCKELSE=2, STARTBLOCKEND=3, STARTESCEXPR=4, STARTEXPR=5, 
		STARTEXPR2=6, TEXT=7, LBRACE=8, RBRACE=9, EXPRESSION=10, LBRACE2=11, RBRACE2=12, 
		EXPRESSION2=13, BLOCK_IF_CONTENT=14, BLOCK_IF_RBRACE=15;
	public static final int
		Expression=1, Expression2=2, BlockIfContent=3;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE", "Expression", "Expression2", "BlockIfContent"
	};

	private static String[] makeRuleNames() {
		return new String[] {
			"StartEscExpr", "StartExpr", "StartExpr2", "EndExpr2", "STARTBLOCKIF", 
			"STARTBLOCKELSE", "STARTBLOCKEND", "STARTESCEXPR", "STARTEXPR", "STARTEXPR2", 
			"TEXT", "LBrace", "RBrace", "LBRACE", "RBRACE", "EXPRESSION", "LBRACE2", 
			"RBRACE2", "EXPRESSION2", "BLOCK_IF_CONTENT", "BLOCK_IF_RBRACE"
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
		"\u0004\u0000\u000f\u00ae\u0006\uffff\uffff\u0006\uffff\uffff\u0006\uffff"+
		"\uffff\u0006\uffff\uffff\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001"+
		"\u0002\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004\u0007\u0004"+
		"\u0002\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002\u0007\u0007\u0007"+
		"\u0002\b\u0007\b\u0002\t\u0007\t\u0002\n\u0007\n\u0002\u000b\u0007\u000b"+
		"\u0002\f\u0007\f\u0002\r\u0007\r\u0002\u000e\u0007\u000e\u0002\u000f\u0007"+
		"\u000f\u0002\u0010\u0007\u0010\u0002\u0011\u0007\u0011\u0002\u0012\u0007"+
		"\u0012\u0002\u0013\u0007\u0013\u0002\u0014\u0007\u0014\u0001\u0000\u0001"+
		"\u0000\u0001\u0000\u0001\u0000\u0001\u0001\u0001\u0001\u0001\u0001\u0001"+
		"\u0002\u0001\u0002\u0001\u0002\u0001\u0003\u0001\u0003\u0001\u0003\u0001"+
		"\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0005\u0004A\b"+
		"\u0004\n\u0004\f\u0004D\t\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001"+
		"\u0004\u0004\u0004J\b\u0004\u000b\u0004\f\u0004K\u0001\u0004\u0001\u0004"+
		"\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0005\u0005"+
		"U\b\u0005\n\u0005\f\u0005X\t\u0005\u0001\u0005\u0001\u0005\u0001\u0005"+
		"\u0001\u0005\u0001\u0005\u0001\u0005\u0005\u0005`\b\u0005\n\u0005\f\u0005"+
		"c\t\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0006\u0001\u0006"+
		"\u0001\u0006\u0001\u0006\u0001\u0006\u0005\u0006m\b\u0006\n\u0006\f\u0006"+
		"p\t\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0006"+
		"\u0005\u0006w\b\u0006\n\u0006\f\u0006z\t\u0006\u0001\u0006\u0001\u0006"+
		"\u0001\u0006\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\b\u0001"+
		"\b\u0001\b\u0001\b\u0001\t\u0001\t\u0001\t\u0001\t\u0001\n\u0001\n\u0001"+
		"\u000b\u0001\u000b\u0001\f\u0001\f\u0001\r\u0001\r\u0001\r\u0001\r\u0001"+
		"\u000e\u0001\u000e\u0001\u000e\u0001\u000e\u0001\u000f\u0001\u000f\u0001"+
		"\u0010\u0001\u0010\u0001\u0010\u0001\u0010\u0001\u0011\u0001\u0011\u0001"+
		"\u0011\u0001\u0011\u0001\u0012\u0001\u0012\u0001\u0013\u0004\u0013\u00a6"+
		"\b\u0013\u000b\u0013\f\u0013\u00a7\u0001\u0014\u0001\u0014\u0001\u0014"+
		"\u0001\u0014\u0001\u0014\u0000\u0000\u0015\u0004\u0000\u0006\u0000\b\u0000"+
		"\n\u0000\f\u0001\u000e\u0002\u0010\u0003\u0012\u0004\u0014\u0005\u0016"+
		"\u0006\u0018\u0007\u001a\u0000\u001c\u0000\u001e\b \t\"\n$\u000b&\f(\r"+
		"*\u000e,\u000f\u0004\u0000\u0001\u0002\u0003\u0002\u0002\u0000\t\t  \u0001"+
		"\u0000}}\u00ab\u0000\f\u0001\u0000\u0000\u0000\u0000\u000e\u0001\u0000"+
		"\u0000\u0000\u0000\u0010\u0001\u0000\u0000\u0000\u0000\u0012\u0001\u0000"+
		"\u0000\u0000\u0000\u0014\u0001\u0000\u0000\u0000\u0000\u0016\u0001\u0000"+
		"\u0000\u0000\u0000\u0018\u0001\u0000\u0000\u0000\u0001\u001e\u0001\u0000"+
		"\u0000\u0000\u0001 \u0001\u0000\u0000\u0000\u0001\"\u0001\u0000\u0000"+
		"\u0000\u0002$\u0001\u0000\u0000\u0000\u0002&\u0001\u0000\u0000\u0000\u0002"+
		"(\u0001\u0000\u0000\u0000\u0003*\u0001\u0000\u0000\u0000\u0003,\u0001"+
		"\u0000\u0000\u0000\u0004.\u0001\u0000\u0000\u0000\u00062\u0001\u0000\u0000"+
		"\u0000\b5\u0001\u0000\u0000\u0000\n8\u0001\u0000\u0000\u0000\f;\u0001"+
		"\u0000\u0000\u0000\u000eO\u0001\u0000\u0000\u0000\u0010g\u0001\u0000\u0000"+
		"\u0000\u0012~\u0001\u0000\u0000\u0000\u0014\u0082\u0001\u0000\u0000\u0000"+
		"\u0016\u0086\u0001\u0000\u0000\u0000\u0018\u008a\u0001\u0000\u0000\u0000"+
		"\u001a\u008c\u0001\u0000\u0000\u0000\u001c\u008e\u0001\u0000\u0000\u0000"+
		"\u001e\u0090\u0001\u0000\u0000\u0000 \u0094\u0001\u0000\u0000\u0000\""+
		"\u0098\u0001\u0000\u0000\u0000$\u009a\u0001\u0000\u0000\u0000&\u009e\u0001"+
		"\u0000\u0000\u0000(\u00a2\u0001\u0000\u0000\u0000*\u00a5\u0001\u0000\u0000"+
		"\u0000,\u00a9\u0001\u0000\u0000\u0000./\u0005$\u0000\u0000/0\u0005$\u0000"+
		"\u000001\u0005{\u0000\u00001\u0005\u0001\u0000\u0000\u000023\u0005$\u0000"+
		"\u000034\u0005{\u0000\u00004\u0007\u0001\u0000\u0000\u000056\u0005{\u0000"+
		"\u000067\u0005{\u0000\u00007\t\u0001\u0000\u0000\u000089\u0005}\u0000"+
		"\u00009:\u0005}\u0000\u0000:\u000b\u0001\u0000\u0000\u0000;<\u0005{\u0000"+
		"\u0000<=\u0005{\u0000\u0000=>\u0005%\u0000\u0000>B\u0001\u0000\u0000\u0000"+
		"?A\u0007\u0000\u0000\u0000@?\u0001\u0000\u0000\u0000AD\u0001\u0000\u0000"+
		"\u0000B@\u0001\u0000\u0000\u0000BC\u0001\u0000\u0000\u0000CE\u0001\u0000"+
		"\u0000\u0000DB\u0001\u0000\u0000\u0000EF\u0005i\u0000\u0000FG\u0005f\u0000"+
		"\u0000GI\u0001\u0000\u0000\u0000HJ\u0007\u0000\u0000\u0000IH\u0001\u0000"+
		"\u0000\u0000JK\u0001\u0000\u0000\u0000KI\u0001\u0000\u0000\u0000KL\u0001"+
		"\u0000\u0000\u0000LM\u0001\u0000\u0000\u0000MN\u0006\u0004\u0000\u0000"+
		"N\r\u0001\u0000\u0000\u0000OP\u0005{\u0000\u0000PQ\u0005{\u0000\u0000"+
		"QR\u0005%\u0000\u0000RV\u0001\u0000\u0000\u0000SU\u0007\u0000\u0000\u0000"+
		"TS\u0001\u0000\u0000\u0000UX\u0001\u0000\u0000\u0000VT\u0001\u0000\u0000"+
		"\u0000VW\u0001\u0000\u0000\u0000WY\u0001\u0000\u0000\u0000XV\u0001\u0000"+
		"\u0000\u0000YZ\u0005e\u0000\u0000Z[\u0005l\u0000\u0000[\\\u0005s\u0000"+
		"\u0000\\]\u0005e\u0000\u0000]a\u0001\u0000\u0000\u0000^`\u0007\u0000\u0000"+
		"\u0000_^\u0001\u0000\u0000\u0000`c\u0001\u0000\u0000\u0000a_\u0001\u0000"+
		"\u0000\u0000ab\u0001\u0000\u0000\u0000bd\u0001\u0000\u0000\u0000ca\u0001"+
		"\u0000\u0000\u0000de\u0005}\u0000\u0000ef\u0005}\u0000\u0000f\u000f\u0001"+
		"\u0000\u0000\u0000gh\u0005{\u0000\u0000hi\u0005{\u0000\u0000ij\u0005%"+
		"\u0000\u0000jn\u0001\u0000\u0000\u0000km\u0007\u0000\u0000\u0000lk\u0001"+
		"\u0000\u0000\u0000mp\u0001\u0000\u0000\u0000nl\u0001\u0000\u0000\u0000"+
		"no\u0001\u0000\u0000\u0000oq\u0001\u0000\u0000\u0000pn\u0001\u0000\u0000"+
		"\u0000qr\u0005e\u0000\u0000rs\u0005n\u0000\u0000st\u0005d\u0000\u0000"+
		"tx\u0001\u0000\u0000\u0000uw\u0007\u0000\u0000\u0000vu\u0001\u0000\u0000"+
		"\u0000wz\u0001\u0000\u0000\u0000xv\u0001\u0000\u0000\u0000xy\u0001\u0000"+
		"\u0000\u0000y{\u0001\u0000\u0000\u0000zx\u0001\u0000\u0000\u0000{|\u0005"+
		"}\u0000\u0000|}\u0005}\u0000\u0000}\u0011\u0001\u0000\u0000\u0000~\u007f"+
		"\u0003\u0004\u0000\u0000\u007f\u0080\u0001\u0000\u0000\u0000\u0080\u0081"+
		"\u0006\u0007\u0001\u0000\u0081\u0013\u0001\u0000\u0000\u0000\u0082\u0083"+
		"\u0003\u0006\u0001\u0000\u0083\u0084\u0001\u0000\u0000\u0000\u0084\u0085"+
		"\u0006\b\u0001\u0000\u0085\u0015\u0001\u0000\u0000\u0000\u0086\u0087\u0003"+
		"\b\u0002\u0000\u0087\u0088\u0001\u0000\u0000\u0000\u0088\u0089\u0006\t"+
		"\u0002\u0000\u0089\u0017\u0001\u0000\u0000\u0000\u008a\u008b\t\u0000\u0000"+
		"\u0000\u008b\u0019\u0001\u0000\u0000\u0000\u008c\u008d\u0005{\u0000\u0000"+
		"\u008d\u001b\u0001\u0000\u0000\u0000\u008e\u008f\u0005}\u0000\u0000\u008f"+
		"\u001d\u0001\u0000\u0000\u0000\u0090\u0091\u0003\u001a\u000b\u0000\u0091"+
		"\u0092\u0001\u0000\u0000\u0000\u0092\u0093\u0006\r\u0001\u0000\u0093\u001f"+
		"\u0001\u0000\u0000\u0000\u0094\u0095\u0003\u001c\f\u0000\u0095\u0096\u0001"+
		"\u0000\u0000\u0000\u0096\u0097\u0006\u000e\u0003\u0000\u0097!\u0001\u0000"+
		"\u0000\u0000\u0098\u0099\t\u0000\u0000\u0000\u0099#\u0001\u0000\u0000"+
		"\u0000\u009a\u009b\u0003\u001a\u000b\u0000\u009b\u009c\u0001\u0000\u0000"+
		"\u0000\u009c\u009d\u0006\u0010\u0001\u0000\u009d%\u0001\u0000\u0000\u0000"+
		"\u009e\u009f\u0003\n\u0003\u0000\u009f\u00a0\u0001\u0000\u0000\u0000\u00a0"+
		"\u00a1\u0006\u0011\u0003\u0000\u00a1\'\u0001\u0000\u0000\u0000\u00a2\u00a3"+
		"\t\u0000\u0000\u0000\u00a3)\u0001\u0000\u0000\u0000\u00a4\u00a6\b\u0001"+
		"\u0000\u0000\u00a5\u00a4\u0001\u0000\u0000\u0000\u00a6\u00a7\u0001\u0000"+
		"\u0000\u0000\u00a7\u00a5\u0001\u0000\u0000\u0000\u00a7\u00a8\u0001\u0000"+
		"\u0000\u0000\u00a8+\u0001\u0000\u0000\u0000\u00a9\u00aa\u0005}\u0000\u0000"+
		"\u00aa\u00ab\u0005}\u0000\u0000\u00ab\u00ac\u0001\u0000\u0000\u0000\u00ac"+
		"\u00ad\u0006\u0014\u0003\u0000\u00ad-\u0001\u0000\u0000\u0000\u000b\u0000"+
		"\u0001\u0002\u0003BKVanx\u00a7\u0004\u0005\u0003\u0000\u0005\u0001\u0000"+
		"\u0005\u0002\u0000\u0004\u0000\u0000";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}