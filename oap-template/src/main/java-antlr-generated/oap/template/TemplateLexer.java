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
		RBRACK=9, DQUESTION=10, SEMI=11, COMMA=12, ID=13, DSTRING=14, SSTRING=15, 
		DECDIGITS=16, FLOAT=17, ERR_CHAR=18;
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
			"Esc", "SQuote", "DQuote", "Underscore", "Comma", "Semi", "Pipe", "Dot", 
			"LParen", "RParen", "LBrace", "RBrace", "LBrack", "RBrack", "StartExpr", 
			"DQuestion", "NameChar", "EscSeq", "UnicodeEsc", "SQuoteLiteral", "DQuoteLiteral", 
			"HexDigit", "DecDigit", "DecDigits", "Float", "STARTEXPR", "TEXT", "RBRACE", 
			"PIPE", "DOT", "LPAREN", "RPAREN", "LBRACK", "RBRACK", "DQUESTION", "SEMI", 
			"COMMA", "ID", "DSTRING", "SSTRING", "DECDIGITS", "FLOAT", "ERR_CHAR"
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
			"LBRACK", "RBRACK", "DQUESTION", "SEMI", "COMMA", "ID", "DSTRING", "SSTRING", 
			"DECDIGITS", "FLOAT", "ERR_CHAR"
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
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2\24\u00e7\b\1\b\1"+
		"\4\2\t\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t"+
		"\n\4\13\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4"+
		"\22\t\22\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4"+
		"\31\t\31\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t\37\4"+
		" \t \4!\t!\4\"\t\"\4#\t#\4$\t$\4%\t%\4&\t&\4\'\t\'\4(\t(\4)\t)\4*\t*\4"+
		"+\t+\4,\t,\3\2\3\2\3\3\3\3\3\4\3\4\3\5\3\5\3\6\3\6\3\7\3\7\3\b\3\b\3\t"+
		"\3\t\3\n\3\n\3\13\3\13\3\f\3\f\3\r\3\r\3\16\3\16\3\17\3\17\3\20\3\20\3"+
		"\20\3\21\3\21\3\21\3\22\3\22\3\22\5\22\u0080\n\22\3\23\3\23\3\23\3\23"+
		"\3\23\5\23\u0087\n\23\3\24\3\24\3\24\3\24\3\24\5\24\u008e\n\24\5\24\u0090"+
		"\n\24\5\24\u0092\n\24\5\24\u0094\n\24\3\25\3\25\3\25\7\25\u0099\n\25\f"+
		"\25\16\25\u009c\13\25\3\25\3\25\3\26\3\26\3\26\7\26\u00a3\n\26\f\26\16"+
		"\26\u00a6\13\26\3\26\3\26\3\27\3\27\3\30\3\30\3\31\6\31\u00af\n\31\r\31"+
		"\16\31\u00b0\3\32\3\32\3\32\5\32\u00b6\n\32\3\33\3\33\3\33\3\33\3\34\3"+
		"\34\3\35\3\35\3\35\3\35\3\36\3\36\3\37\3\37\3 \3 \3!\3!\3\"\3\"\3#\3#"+
		"\3$\3$\3%\3%\3&\3&\3\'\3\'\3\'\7\'\u00d7\n\'\f\'\16\'\u00da\13\'\3(\3"+
		"(\3)\3)\3*\3*\3+\3+\3,\3,\3,\3,\2\2-\4\2\6\2\b\2\n\2\f\2\16\2\20\2\22"+
		"\2\24\2\26\2\30\2\32\2\34\2\36\2 \2\"\2$\2&\2(\2*\2,\2.\2\60\2\62\2\64"+
		"\2\66\38\4:\5<\6>\7@\bB\tD\nF\13H\fJ\rL\16N\17P\20R\21T\22V\23X\24\4\2"+
		"\3\t\17\2C\\c|\u00c2\u00d8\u00da\u00f8\u00fa\u0301\u0372\u037f\u0381\u2001"+
		"\u200e\u200f\u2072\u2191\u2c02\u2ff1\u3003\ud801\uf902\ufdd1\ufdf2\uffff"+
		"\5\2\u00b9\u00b9\u0302\u0371\u2041\u2042\n\2$$))^^ddhhppttvv\6\2\f\f\17"+
		"\17))^^\6\2\f\f\17\17$$^^\5\2\62;CHch\3\2\62;\2\u00dd\2\66\3\2\2\2\28"+
		"\3\2\2\2\3:\3\2\2\2\3<\3\2\2\2\3>\3\2\2\2\3@\3\2\2\2\3B\3\2\2\2\3D\3\2"+
		"\2\2\3F\3\2\2\2\3H\3\2\2\2\3J\3\2\2\2\3L\3\2\2\2\3N\3\2\2\2\3P\3\2\2\2"+
		"\3R\3\2\2\2\3T\3\2\2\2\3V\3\2\2\2\3X\3\2\2\2\4Z\3\2\2\2\6\\\3\2\2\2\b"+
		"^\3\2\2\2\n`\3\2\2\2\fb\3\2\2\2\16d\3\2\2\2\20f\3\2\2\2\22h\3\2\2\2\24"+
		"j\3\2\2\2\26l\3\2\2\2\30n\3\2\2\2\32p\3\2\2\2\34r\3\2\2\2\36t\3\2\2\2"+
		" v\3\2\2\2\"y\3\2\2\2$\177\3\2\2\2&\u0081\3\2\2\2(\u0088\3\2\2\2*\u0095"+
		"\3\2\2\2,\u009f\3\2\2\2.\u00a9\3\2\2\2\60\u00ab\3\2\2\2\62\u00ae\3\2\2"+
		"\2\64\u00b2\3\2\2\2\66\u00b7\3\2\2\28\u00bb\3\2\2\2:\u00bd\3\2\2\2<\u00c1"+
		"\3\2\2\2>\u00c3\3\2\2\2@\u00c5\3\2\2\2B\u00c7\3\2\2\2D\u00c9\3\2\2\2F"+
		"\u00cb\3\2\2\2H\u00cd\3\2\2\2J\u00cf\3\2\2\2L\u00d1\3\2\2\2N\u00d3\3\2"+
		"\2\2P\u00db\3\2\2\2R\u00dd\3\2\2\2T\u00df\3\2\2\2V\u00e1\3\2\2\2X\u00e3"+
		"\3\2\2\2Z[\7^\2\2[\5\3\2\2\2\\]\7)\2\2]\7\3\2\2\2^_\7$\2\2_\t\3\2\2\2"+
		"`a\7a\2\2a\13\3\2\2\2bc\7.\2\2c\r\3\2\2\2de\7=\2\2e\17\3\2\2\2fg\7~\2"+
		"\2g\21\3\2\2\2hi\7\60\2\2i\23\3\2\2\2jk\7*\2\2k\25\3\2\2\2lm\7+\2\2m\27"+
		"\3\2\2\2no\7}\2\2o\31\3\2\2\2pq\7\177\2\2q\33\3\2\2\2rs\7]\2\2s\35\3\2"+
		"\2\2tu\7_\2\2u\37\3\2\2\2vw\7&\2\2wx\7}\2\2x!\3\2\2\2yz\7A\2\2z{\7A\2"+
		"\2{#\3\2\2\2|\u0080\t\2\2\2}\u0080\5\n\5\2~\u0080\t\3\2\2\177|\3\2\2\2"+
		"\177}\3\2\2\2\177~\3\2\2\2\u0080%\3\2\2\2\u0081\u0086\5\4\2\2\u0082\u0087"+
		"\t\4\2\2\u0083\u0087\5(\24\2\u0084\u0087\13\2\2\2\u0085\u0087\7\2\2\3"+
		"\u0086\u0082\3\2\2\2\u0086\u0083\3\2\2\2\u0086\u0084\3\2\2\2\u0086\u0085"+
		"\3\2\2\2\u0087\'\3\2\2\2\u0088\u0093\7w\2\2\u0089\u0091\5.\27\2\u008a"+
		"\u008f\5.\27\2\u008b\u008d\5.\27\2\u008c\u008e\5.\27\2\u008d\u008c\3\2"+
		"\2\2\u008d\u008e\3\2\2\2\u008e\u0090\3\2\2\2\u008f\u008b\3\2\2\2\u008f"+
		"\u0090\3\2\2\2\u0090\u0092\3\2\2\2\u0091\u008a\3\2\2\2\u0091\u0092\3\2"+
		"\2\2\u0092\u0094\3\2\2\2\u0093\u0089\3\2\2\2\u0093\u0094\3\2\2\2\u0094"+
		")\3\2\2\2\u0095\u009a\5\6\3\2\u0096\u0099\5&\23\2\u0097\u0099\n\5\2\2"+
		"\u0098\u0096\3\2\2\2\u0098\u0097\3\2\2\2\u0099\u009c\3\2\2\2\u009a\u0098"+
		"\3\2\2\2\u009a\u009b\3\2\2\2\u009b\u009d\3\2\2\2\u009c\u009a\3\2\2\2\u009d"+
		"\u009e\5\6\3\2\u009e+\3\2\2\2\u009f\u00a4\5\b\4\2\u00a0\u00a3\5&\23\2"+
		"\u00a1\u00a3\n\6\2\2\u00a2\u00a0\3\2\2\2\u00a2\u00a1\3\2\2\2\u00a3\u00a6"+
		"\3\2\2\2\u00a4\u00a2\3\2\2\2\u00a4\u00a5\3\2\2\2\u00a5\u00a7\3\2\2\2\u00a6"+
		"\u00a4\3\2\2\2\u00a7\u00a8\5\b\4\2\u00a8-\3\2\2\2\u00a9\u00aa\t\7\2\2"+
		"\u00aa/\3\2\2\2\u00ab\u00ac\t\b\2\2\u00ac\61\3\2\2\2\u00ad\u00af\5\60"+
		"\30\2\u00ae\u00ad\3\2\2\2\u00af\u00b0\3\2\2\2\u00b0\u00ae\3\2\2\2\u00b0"+
		"\u00b1\3\2\2\2\u00b1\63\3\2\2\2\u00b2\u00b3\5\62\31\2\u00b3\u00b5\5\22"+
		"\t\2\u00b4\u00b6\5\62\31\2\u00b5\u00b4\3\2\2\2\u00b5\u00b6\3\2\2\2\u00b6"+
		"\65\3\2\2\2\u00b7\u00b8\5 \20\2\u00b8\u00b9\3\2\2\2\u00b9\u00ba\b\33\2"+
		"\2\u00ba\67\3\2\2\2\u00bb\u00bc\13\2\2\2\u00bc9\3\2\2\2\u00bd\u00be\5"+
		"\32\r\2\u00be\u00bf\3\2\2\2\u00bf\u00c0\b\35\3\2\u00c0;\3\2\2\2\u00c1"+
		"\u00c2\5\20\b\2\u00c2=\3\2\2\2\u00c3\u00c4\5\22\t\2\u00c4?\3\2\2\2\u00c5"+
		"\u00c6\5\24\n\2\u00c6A\3\2\2\2\u00c7\u00c8\5\26\13\2\u00c8C\3\2\2\2\u00c9"+
		"\u00ca\5\34\16\2\u00caE\3\2\2\2\u00cb\u00cc\5\36\17\2\u00ccG\3\2\2\2\u00cd"+
		"\u00ce\5\"\21\2\u00ceI\3\2\2\2\u00cf\u00d0\5\16\7\2\u00d0K\3\2\2\2\u00d1"+
		"\u00d2\5\f\6\2\u00d2M\3\2\2\2\u00d3\u00d8\5$\22\2\u00d4\u00d7\5$\22\2"+
		"\u00d5\u00d7\5\60\30\2\u00d6\u00d4\3\2\2\2\u00d6\u00d5\3\2\2\2\u00d7\u00da"+
		"\3\2\2\2\u00d8\u00d6\3\2\2\2\u00d8\u00d9\3\2\2\2\u00d9O\3\2\2\2\u00da"+
		"\u00d8\3\2\2\2\u00db\u00dc\5,\26\2\u00dcQ\3\2\2\2\u00dd\u00de\5*\25\2"+
		"\u00deS\3\2\2\2\u00df\u00e0\5\62\31\2\u00e0U\3\2\2\2\u00e1\u00e2\5\64"+
		"\32\2\u00e2W\3\2\2\2\u00e3\u00e4\13\2\2\2\u00e4\u00e5\3\2\2\2\u00e5\u00e6"+
		"\b,\4\2\u00e6Y\3\2\2\2\22\2\3\177\u0086\u008d\u008f\u0091\u0093\u0098"+
		"\u009a\u00a2\u00a4\u00b0\u00b5\u00d6\u00d8\5\4\3\2\4\2\2\b\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}