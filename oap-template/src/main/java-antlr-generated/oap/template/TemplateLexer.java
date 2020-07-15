// Generated from TemplateLexer.g4 by ANTLR 4.8

package oap.template;

import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class TemplateLexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.8", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		STARTEXPR=1, TEXT=2, RBRACE=3, PIPE=4, DOT=5, LPAREN=6, RPAREN=7, LBRACK=8, 
		RBRACK=9, DQUESTION=10, SEMI=11, ID=12, DSTRING=13, SSTRING=14, ERR_CHAR=15, 
		FUNCTIONNAME=16, FADECDIGITS=17, FADSTRING=18, FASSTRING=19, FACOMMA=20, 
		FALPAREN=21, FARPAREN=22, FAERR_CHAR=23;
	public static final int
		Expression=1, FunctionArgs=2;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE", "Expression", "FunctionArgs"
	};

	private static String[] makeRuleNames() {
		return new String[] {
			"Esc", "SQuote", "DQuote", "Underscore", "Comma", "Semi", "Pipe", "Dot", 
			"LParen", "RParen", "LBrace", "RBrace", "LBrack", "RBrack", "StartExpr", 
			"DQuestion", "NameChar", "EscSeq", "UnicodeEsc", "SQuoteLiteral", "DQuoteLiteral", 
			"HexDigit", "DecDigit", "DecDigits", "STARTEXPR", "TEXT", "RBRACE", "PIPE", 
			"DOT", "LPAREN", "RPAREN", "LBRACK", "RBRACK", "DQUESTION", "SEMI", "ID", 
			"DSTRING", "SSTRING", "ERR_CHAR", "FUNCTIONNAME", "FADECDIGITS", "FADSTRING", 
			"FASSTRING", "FACOMMA", "FALPAREN", "FARPAREN", "FAERR_CHAR"
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
			null, "STARTEXPR", "TEXT", "RBRACE", "PIPE", "DOT", "LPAREN", "RPAREN", 
			"LBRACK", "RBRACK", "DQUESTION", "SEMI", "ID", "DSTRING", "SSTRING", 
			"ERR_CHAR", "FUNCTIONNAME", "FADECDIGITS", "FADSTRING", "FASSTRING", 
			"FACOMMA", "FALPAREN", "FARPAREN", "FAERR_CHAR"
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
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2\31\u00ff\b\1\b\1"+
		"\b\1\4\2\t\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4"+
		"\n\t\n\4\13\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t"+
		"\21\4\22\t\22\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t"+
		"\30\4\31\t\31\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t"+
		"\37\4 \t \4!\t!\4\"\t\"\4#\t#\4$\t$\4%\t%\4&\t&\4\'\t\'\4(\t(\4)\t)\4"+
		"*\t*\4+\t+\4,\t,\4-\t-\4.\t.\4/\t/\4\60\t\60\3\2\3\2\3\3\3\3\3\4\3\4\3"+
		"\5\3\5\3\6\3\6\3\7\3\7\3\b\3\b\3\t\3\t\3\n\3\n\3\13\3\13\3\f\3\f\3\r\3"+
		"\r\3\16\3\16\3\17\3\17\3\20\3\20\3\20\3\21\3\21\3\21\3\22\3\22\3\22\5"+
		"\22\u0089\n\22\3\23\3\23\3\23\3\23\3\23\5\23\u0090\n\23\3\24\3\24\3\24"+
		"\3\24\3\24\5\24\u0097\n\24\5\24\u0099\n\24\5\24\u009b\n\24\5\24\u009d"+
		"\n\24\3\25\3\25\3\25\7\25\u00a2\n\25\f\25\16\25\u00a5\13\25\3\25\3\25"+
		"\3\26\3\26\3\26\7\26\u00ac\n\26\f\26\16\26\u00af\13\26\3\26\3\26\3\27"+
		"\3\27\3\30\3\30\3\31\6\31\u00b8\n\31\r\31\16\31\u00b9\3\32\3\32\3\32\3"+
		"\32\3\33\3\33\3\34\3\34\3\34\3\34\3\35\3\35\3\36\3\36\3\37\3\37\3 \3 "+
		"\3!\3!\3\"\3\"\3#\3#\3$\3$\3$\3$\3%\3%\6%\u00da\n%\r%\16%\u00db\3&\3&"+
		"\3\'\3\'\3(\3(\3(\3(\3)\3)\3)\7)\u00e9\n)\f)\16)\u00ec\13)\3*\3*\3+\3"+
		"+\3,\3,\3-\3-\3.\3.\3/\3/\3/\3/\3\60\3\60\3\60\3\60\2\2\61\5\2\7\2\t\2"+
		"\13\2\r\2\17\2\21\2\23\2\25\2\27\2\31\2\33\2\35\2\37\2!\2#\2%\2\'\2)\2"+
		"+\2-\2/\2\61\2\63\2\65\3\67\49\5;\6=\7?\bA\tC\nE\13G\fI\rK\16M\17O\20"+
		"Q\21S\22U\23W\24Y\25[\26]\27_\30a\31\5\2\3\4\t\17\2C\\c|\u00c2\u00d8\u00da"+
		"\u00f8\u00fa\u0301\u0372\u037f\u0381\u2001\u200e\u200f\u2072\u2191\u2c02"+
		"\u2ff1\u3003\ud801\uf902\ufdd1\ufdf2\uffff\5\2\u00b9\u00b9\u0302\u0371"+
		"\u2041\u2042\n\2$$))^^ddhhppttvv\6\2\f\f\17\17))^^\6\2\f\f\17\17$$^^\5"+
		"\2\62;CHch\3\2\62;\2\u00f6\2\65\3\2\2\2\2\67\3\2\2\2\39\3\2\2\2\3;\3\2"+
		"\2\2\3=\3\2\2\2\3?\3\2\2\2\3A\3\2\2\2\3C\3\2\2\2\3E\3\2\2\2\3G\3\2\2\2"+
		"\3I\3\2\2\2\3K\3\2\2\2\3M\3\2\2\2\3O\3\2\2\2\3Q\3\2\2\2\4S\3\2\2\2\4U"+
		"\3\2\2\2\4W\3\2\2\2\4Y\3\2\2\2\4[\3\2\2\2\4]\3\2\2\2\4_\3\2\2\2\4a\3\2"+
		"\2\2\5c\3\2\2\2\7e\3\2\2\2\tg\3\2\2\2\13i\3\2\2\2\rk\3\2\2\2\17m\3\2\2"+
		"\2\21o\3\2\2\2\23q\3\2\2\2\25s\3\2\2\2\27u\3\2\2\2\31w\3\2\2\2\33y\3\2"+
		"\2\2\35{\3\2\2\2\37}\3\2\2\2!\177\3\2\2\2#\u0082\3\2\2\2%\u0088\3\2\2"+
		"\2\'\u008a\3\2\2\2)\u0091\3\2\2\2+\u009e\3\2\2\2-\u00a8\3\2\2\2/\u00b2"+
		"\3\2\2\2\61\u00b4\3\2\2\2\63\u00b7\3\2\2\2\65\u00bb\3\2\2\2\67\u00bf\3"+
		"\2\2\29\u00c1\3\2\2\2;\u00c5\3\2\2\2=\u00c7\3\2\2\2?\u00c9\3\2\2\2A\u00cb"+
		"\3\2\2\2C\u00cd\3\2\2\2E\u00cf\3\2\2\2G\u00d1\3\2\2\2I\u00d3\3\2\2\2K"+
		"\u00d9\3\2\2\2M\u00dd\3\2\2\2O\u00df\3\2\2\2Q\u00e1\3\2\2\2S\u00e5\3\2"+
		"\2\2U\u00ed\3\2\2\2W\u00ef\3\2\2\2Y\u00f1\3\2\2\2[\u00f3\3\2\2\2]\u00f5"+
		"\3\2\2\2_\u00f7\3\2\2\2a\u00fb\3\2\2\2cd\7^\2\2d\6\3\2\2\2ef\7)\2\2f\b"+
		"\3\2\2\2gh\7$\2\2h\n\3\2\2\2ij\7a\2\2j\f\3\2\2\2kl\7.\2\2l\16\3\2\2\2"+
		"mn\7=\2\2n\20\3\2\2\2op\7~\2\2p\22\3\2\2\2qr\7\60\2\2r\24\3\2\2\2st\7"+
		"*\2\2t\26\3\2\2\2uv\7+\2\2v\30\3\2\2\2wx\7}\2\2x\32\3\2\2\2yz\7\177\2"+
		"\2z\34\3\2\2\2{|\7]\2\2|\36\3\2\2\2}~\7_\2\2~ \3\2\2\2\177\u0080\7&\2"+
		"\2\u0080\u0081\7}\2\2\u0081\"\3\2\2\2\u0082\u0083\7A\2\2\u0083\u0084\7"+
		"A\2\2\u0084$\3\2\2\2\u0085\u0089\t\2\2\2\u0086\u0089\5\13\5\2\u0087\u0089"+
		"\t\3\2\2\u0088\u0085\3\2\2\2\u0088\u0086\3\2\2\2\u0088\u0087\3\2\2\2\u0089"+
		"&\3\2\2\2\u008a\u008f\5\5\2\2\u008b\u0090\t\4\2\2\u008c\u0090\5)\24\2"+
		"\u008d\u0090\13\2\2\2\u008e\u0090\7\2\2\3\u008f\u008b\3\2\2\2\u008f\u008c"+
		"\3\2\2\2\u008f\u008d\3\2\2\2\u008f\u008e\3\2\2\2\u0090(\3\2\2\2\u0091"+
		"\u009c\7w\2\2\u0092\u009a\5/\27\2\u0093\u0098\5/\27\2\u0094\u0096\5/\27"+
		"\2\u0095\u0097\5/\27\2\u0096\u0095\3\2\2\2\u0096\u0097\3\2\2\2\u0097\u0099"+
		"\3\2\2\2\u0098\u0094\3\2\2\2\u0098\u0099\3\2\2\2\u0099\u009b\3\2\2\2\u009a"+
		"\u0093\3\2\2\2\u009a\u009b\3\2\2\2\u009b\u009d\3\2\2\2\u009c\u0092\3\2"+
		"\2\2\u009c\u009d\3\2\2\2\u009d*\3\2\2\2\u009e\u00a3\5\7\3\2\u009f\u00a2"+
		"\5\'\23\2\u00a0\u00a2\n\5\2\2\u00a1\u009f\3\2\2\2\u00a1\u00a0\3\2\2\2"+
		"\u00a2\u00a5\3\2\2\2\u00a3\u00a1\3\2\2\2\u00a3\u00a4\3\2\2\2\u00a4\u00a6"+
		"\3\2\2\2\u00a5\u00a3\3\2\2\2\u00a6\u00a7\5\7\3\2\u00a7,\3\2\2\2\u00a8"+
		"\u00ad\5\t\4\2\u00a9\u00ac\5\'\23\2\u00aa\u00ac\n\6\2\2\u00ab\u00a9\3"+
		"\2\2\2\u00ab\u00aa\3\2\2\2\u00ac\u00af\3\2\2\2\u00ad\u00ab\3\2\2\2\u00ad"+
		"\u00ae\3\2\2\2\u00ae\u00b0\3\2\2\2\u00af\u00ad\3\2\2\2\u00b0\u00b1\5\t"+
		"\4\2\u00b1.\3\2\2\2\u00b2\u00b3\t\7\2\2\u00b3\60\3\2\2\2\u00b4\u00b5\t"+
		"\b\2\2\u00b5\62\3\2\2\2\u00b6\u00b8\5\61\30\2\u00b7\u00b6\3\2\2\2\u00b8"+
		"\u00b9\3\2\2\2\u00b9\u00b7\3\2\2\2\u00b9\u00ba\3\2\2\2\u00ba\64\3\2\2"+
		"\2\u00bb\u00bc\5!\20\2\u00bc\u00bd\3\2\2\2\u00bd\u00be\b\32\2\2\u00be"+
		"\66\3\2\2\2\u00bf\u00c0\13\2\2\2\u00c08\3\2\2\2\u00c1\u00c2\5\33\r\2\u00c2"+
		"\u00c3\3\2\2\2\u00c3\u00c4\b\34\3\2\u00c4:\3\2\2\2\u00c5\u00c6\5\21\b"+
		"\2\u00c6<\3\2\2\2\u00c7\u00c8\5\23\t\2\u00c8>\3\2\2\2\u00c9\u00ca\5\25"+
		"\n\2\u00ca@\3\2\2\2\u00cb\u00cc\5\27\13\2\u00ccB\3\2\2\2\u00cd\u00ce\5"+
		"\35\16\2\u00ceD\3\2\2\2\u00cf\u00d0\5\37\17\2\u00d0F\3\2\2\2\u00d1\u00d2"+
		"\5#\21\2\u00d2H\3\2\2\2\u00d3\u00d4\5\17\7\2\u00d4\u00d5\3\2\2\2\u00d5"+
		"\u00d6\b$\4\2\u00d6J\3\2\2\2\u00d7\u00da\5%\22\2\u00d8\u00da\5\61\30\2"+
		"\u00d9\u00d7\3\2\2\2\u00d9\u00d8\3\2\2\2\u00da\u00db\3\2\2\2\u00db\u00d9"+
		"\3\2\2\2\u00db\u00dc\3\2\2\2\u00dcL\3\2\2\2\u00dd\u00de\5-\26\2\u00de"+
		"N\3\2\2\2\u00df\u00e0\5+\25\2\u00e0P\3\2\2\2\u00e1\u00e2\13\2\2\2\u00e2"+
		"\u00e3\3\2\2\2\u00e3\u00e4\b(\5\2\u00e4R\3\2\2\2\u00e5\u00ea\5%\22\2\u00e6"+
		"\u00e9\5%\22\2\u00e7\u00e9\5\61\30\2\u00e8\u00e6\3\2\2\2\u00e8\u00e7\3"+
		"\2\2\2\u00e9\u00ec\3\2\2\2\u00ea\u00e8\3\2\2\2\u00ea\u00eb\3\2\2\2\u00eb"+
		"T\3\2\2\2\u00ec\u00ea\3\2\2\2\u00ed\u00ee\5\63\31\2\u00eeV\3\2\2\2\u00ef"+
		"\u00f0\5-\26\2\u00f0X\3\2\2\2\u00f1\u00f2\5+\25\2\u00f2Z\3\2\2\2\u00f3"+
		"\u00f4\5\r\6\2\u00f4\\\3\2\2\2\u00f5\u00f6\5\25\n\2\u00f6^\3\2\2\2\u00f7"+
		"\u00f8\5\27\13\2\u00f8\u00f9\3\2\2\2\u00f9\u00fa\b/\2\2\u00fa`\3\2\2\2"+
		"\u00fb\u00fc\13\2\2\2\u00fc\u00fd\3\2\2\2\u00fd\u00fe\b\60\5\2\u00feb"+
		"\3\2\2\2\24\2\3\4\u0088\u008f\u0096\u0098\u009a\u009c\u00a1\u00a3\u00ab"+
		"\u00ad\u00b9\u00d9\u00db\u00e8\u00ea\6\4\3\2\4\2\2\4\4\2\b\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}