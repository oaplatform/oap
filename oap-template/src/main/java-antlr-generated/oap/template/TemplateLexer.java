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
		STARTEXPR=1, TEXT=2, BLOCK_COMMENT=3, HORZ_WS=4, VERT_WS=5, LBRACE=6, 
		RBRACE=7, PIPE=8, DOT=9, LPAREN=10, RPAREN=11, LBRACK=12, RBRACK=13, DQUESTION=14, 
		SEMI=15, COMMA=16, STAR=17, SLASH=18, PERCENT=19, PLUS=20, MINUS=21, ID=22, 
		DSTRING=23, SSTRING=24, DECDIGITS=25, FLOAT=26, ERR_CHAR=27, C_HORZ_WS=28, 
		C_VERT_WS=29, CERR_CHAR=30;
	public static final int
		Expression=1, Concatenation=2;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE", "Expression", "Concatenation"
	};

	private static String[] makeRuleNames() {
		return new String[] {
			"Ws", "Hws", "Vws", "BlockComment", "Esc", "SQuote", "DQuote", "Underscore", 
			"Comma", "Semi", "Pipe", "Dot", "LParen", "RParen", "LBrace", "RBrace", 
			"LBrack", "RBrack", "Star", "Slash", "Percent", "Plus", "Minus", "StartExpr", 
			"DQuestion", "NameChar", "EscSeq", "UnicodeEsc", "SQuoteLiteral", "DQuoteLiteral", 
			"HexDigit", "DecDigit", "DecDigits", "Float", "STARTEXPR", "TEXT", "BLOCK_COMMENT", 
			"HORZ_WS", "VERT_WS", "LBRACE", "RBRACE", "PIPE", "DOT", "LPAREN", "RPAREN", 
			"LBRACK", "RBRACK", "DQUESTION", "SEMI", "COMMA", "STAR", "SLASH", "PERCENT", 
			"PLUS", "MINUS", "ID", "DSTRING", "SSTRING", "DECDIGITS", "FLOAT", "ERR_CHAR", 
			"C_HORZ_WS", "C_VERT_WS", "CRBRACE", "CCOMMA", "CID", "CDSTRING", "CSSTRING", 
			"CDECDIGITS", "CFLOAT", "CERR_CHAR"
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
			null, "STARTEXPR", "TEXT", "BLOCK_COMMENT", "HORZ_WS", "VERT_WS", "LBRACE", 
			"RBRACE", "PIPE", "DOT", "LPAREN", "RPAREN", "LBRACK", "RBRACK", "DQUESTION", 
			"SEMI", "COMMA", "STAR", "SLASH", "PERCENT", "PLUS", "MINUS", "ID", "DSTRING", 
			"SSTRING", "DECDIGITS", "FLOAT", "ERR_CHAR", "C_HORZ_WS", "C_VERT_WS", 
			"CERR_CHAR"
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
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2 \u0193\b\1\b\1\b"+
		"\1\4\2\t\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n"+
		"\t\n\4\13\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21"+
		"\4\22\t\22\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30"+
		"\4\31\t\31\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t\37"+
		"\4 \t \4!\t!\4\"\t\"\4#\t#\4$\t$\4%\t%\4&\t&\4\'\t\'\4(\t(\4)\t)\4*\t"+
		"*\4+\t+\4,\t,\4-\t-\4.\t.\4/\t/\4\60\t\60\4\61\t\61\4\62\t\62\4\63\t\63"+
		"\4\64\t\64\4\65\t\65\4\66\t\66\4\67\t\67\48\t8\49\t9\4:\t:\4;\t;\4<\t"+
		"<\4=\t=\4>\t>\4?\t?\4@\t@\4A\tA\4B\tB\4C\tC\4D\tD\4E\tE\4F\tF\4G\tG\4"+
		"H\tH\3\2\3\2\5\2\u0096\n\2\3\3\3\3\3\4\3\4\3\5\3\5\3\5\3\5\7\5\u00a0\n"+
		"\5\f\5\16\5\u00a3\13\5\3\5\3\5\3\5\5\5\u00a8\n\5\3\6\3\6\3\7\3\7\3\b\3"+
		"\b\3\t\3\t\3\n\3\n\3\13\3\13\3\f\3\f\3\r\3\r\3\16\3\16\3\17\3\17\3\20"+
		"\3\20\3\21\3\21\3\22\3\22\3\23\3\23\3\24\3\24\3\25\3\25\3\26\3\26\3\27"+
		"\3\27\3\30\3\30\3\31\3\31\3\31\3\32\3\32\3\32\3\33\3\33\3\33\5\33\u00d9"+
		"\n\33\3\34\3\34\3\34\3\34\3\34\5\34\u00e0\n\34\3\35\3\35\3\35\3\35\3\35"+
		"\5\35\u00e7\n\35\5\35\u00e9\n\35\5\35\u00eb\n\35\5\35\u00ed\n\35\3\36"+
		"\3\36\3\36\7\36\u00f2\n\36\f\36\16\36\u00f5\13\36\3\36\3\36\3\37\3\37"+
		"\3\37\7\37\u00fc\n\37\f\37\16\37\u00ff\13\37\3\37\3\37\3 \3 \3!\3!\3\""+
		"\6\"\u0108\n\"\r\"\16\"\u0109\3#\3#\3#\5#\u010f\n#\3$\3$\3$\3$\3%\3%\3"+
		"&\3&\3\'\6\'\u011a\n\'\r\'\16\'\u011b\3\'\3\'\3(\6(\u0121\n(\r(\16(\u0122"+
		"\3(\3(\3)\3)\3)\3)\3*\3*\3*\3*\3+\3+\3,\3,\3-\3-\3.\3.\3/\3/\3\60\3\60"+
		"\3\61\3\61\3\62\3\62\3\63\3\63\3\64\3\64\3\65\3\65\3\66\3\66\3\67\3\67"+
		"\38\38\39\39\39\79\u014e\n9\f9\169\u0151\139\3:\3:\3;\3;\3<\3<\3=\3=\3"+
		">\3>\3>\3>\3?\6?\u0160\n?\r?\16?\u0161\3?\3?\3@\6@\u0167\n@\r@\16@\u0168"+
		"\3@\3@\3A\3A\3A\3A\3A\3B\3B\3B\3B\3C\3C\3C\7C\u0179\nC\fC\16C\u017c\13"+
		"C\3C\3C\3D\3D\3D\3D\3E\3E\3E\3E\3F\3F\3F\3F\3G\3G\3G\3G\3H\3H\3H\3H\3"+
		"\u00a1\2I\5\2\7\2\t\2\13\2\r\2\17\2\21\2\23\2\25\2\27\2\31\2\33\2\35\2"+
		"\37\2!\2#\2%\2\'\2)\2+\2-\2/\2\61\2\63\2\65\2\67\29\2;\2=\2?\2A\2C\2E"+
		"\2G\2I\3K\4M\5O\6Q\7S\bU\tW\nY\13[\f]\r_\16a\17c\20e\21g\22i\23k\24m\25"+
		"o\26q\27s\30u\31w\32y\33{\34}\35\177\36\u0081\37\u0083\2\u0085\2\u0087"+
		"\2\u0089\2\u008b\2\u008d\2\u008f\2\u0091 \5\2\3\4\13\4\2\13\13\"\"\4\2"+
		"\f\f\16\17\17\2C\\c|\u00c2\u00d8\u00da\u00f8\u00fa\u0301\u0372\u037f\u0381"+
		"\u2001\u200e\u200f\u2072\u2191\u2c02\u2ff1\u3003\ud801\uf902\ufdd1\ufdf2"+
		"\uffff\5\2\u00b9\u00b9\u0302\u0371\u2041\u2042\n\2$$))^^ddhhppttvv\6\2"+
		"\f\f\17\17))^^\6\2\f\f\17\17$$^^\5\2\62;CHch\3\2\62;\2\u0188\2I\3\2\2"+
		"\2\2K\3\2\2\2\3M\3\2\2\2\3O\3\2\2\2\3Q\3\2\2\2\3S\3\2\2\2\3U\3\2\2\2\3"+
		"W\3\2\2\2\3Y\3\2\2\2\3[\3\2\2\2\3]\3\2\2\2\3_\3\2\2\2\3a\3\2\2\2\3c\3"+
		"\2\2\2\3e\3\2\2\2\3g\3\2\2\2\3i\3\2\2\2\3k\3\2\2\2\3m\3\2\2\2\3o\3\2\2"+
		"\2\3q\3\2\2\2\3s\3\2\2\2\3u\3\2\2\2\3w\3\2\2\2\3y\3\2\2\2\3{\3\2\2\2\3"+
		"}\3\2\2\2\4\177\3\2\2\2\4\u0081\3\2\2\2\4\u0083\3\2\2\2\4\u0085\3\2\2"+
		"\2\4\u0087\3\2\2\2\4\u0089\3\2\2\2\4\u008b\3\2\2\2\4\u008d\3\2\2\2\4\u008f"+
		"\3\2\2\2\4\u0091\3\2\2\2\5\u0095\3\2\2\2\7\u0097\3\2\2\2\t\u0099\3\2\2"+
		"\2\13\u009b\3\2\2\2\r\u00a9\3\2\2\2\17\u00ab\3\2\2\2\21\u00ad\3\2\2\2"+
		"\23\u00af\3\2\2\2\25\u00b1\3\2\2\2\27\u00b3\3\2\2\2\31\u00b5\3\2\2\2\33"+
		"\u00b7\3\2\2\2\35\u00b9\3\2\2\2\37\u00bb\3\2\2\2!\u00bd\3\2\2\2#\u00bf"+
		"\3\2\2\2%\u00c1\3\2\2\2\'\u00c3\3\2\2\2)\u00c5\3\2\2\2+\u00c7\3\2\2\2"+
		"-\u00c9\3\2\2\2/\u00cb\3\2\2\2\61\u00cd\3\2\2\2\63\u00cf\3\2\2\2\65\u00d2"+
		"\3\2\2\2\67\u00d8\3\2\2\29\u00da\3\2\2\2;\u00e1\3\2\2\2=\u00ee\3\2\2\2"+
		"?\u00f8\3\2\2\2A\u0102\3\2\2\2C\u0104\3\2\2\2E\u0107\3\2\2\2G\u010b\3"+
		"\2\2\2I\u0110\3\2\2\2K\u0114\3\2\2\2M\u0116\3\2\2\2O\u0119\3\2\2\2Q\u0120"+
		"\3\2\2\2S\u0126\3\2\2\2U\u012a\3\2\2\2W\u012e\3\2\2\2Y\u0130\3\2\2\2["+
		"\u0132\3\2\2\2]\u0134\3\2\2\2_\u0136\3\2\2\2a\u0138\3\2\2\2c\u013a\3\2"+
		"\2\2e\u013c\3\2\2\2g\u013e\3\2\2\2i\u0140\3\2\2\2k\u0142\3\2\2\2m\u0144"+
		"\3\2\2\2o\u0146\3\2\2\2q\u0148\3\2\2\2s\u014a\3\2\2\2u\u0152\3\2\2\2w"+
		"\u0154\3\2\2\2y\u0156\3\2\2\2{\u0158\3\2\2\2}\u015a\3\2\2\2\177\u015f"+
		"\3\2\2\2\u0081\u0166\3\2\2\2\u0083\u016c\3\2\2\2\u0085\u0171\3\2\2\2\u0087"+
		"\u0175\3\2\2\2\u0089\u017f\3\2\2\2\u008b\u0183\3\2\2\2\u008d\u0187\3\2"+
		"\2\2\u008f\u018b\3\2\2\2\u0091\u018f\3\2\2\2\u0093\u0096\5\7\3\2\u0094"+
		"\u0096\5\t\4\2\u0095\u0093\3\2\2\2\u0095\u0094\3\2\2\2\u0096\6\3\2\2\2"+
		"\u0097\u0098\t\2\2\2\u0098\b\3\2\2\2\u0099\u009a\t\3\2\2\u009a\n\3\2\2"+
		"\2\u009b\u009c\7\61\2\2\u009c\u009d\7,\2\2\u009d\u00a1\3\2\2\2\u009e\u00a0"+
		"\13\2\2\2\u009f\u009e\3\2\2\2\u00a0\u00a3\3\2\2\2\u00a1\u00a2\3\2\2\2"+
		"\u00a1\u009f\3\2\2\2\u00a2\u00a7\3\2\2\2\u00a3\u00a1\3\2\2\2\u00a4\u00a5"+
		"\7,\2\2\u00a5\u00a8\7\61\2\2\u00a6\u00a8\7\2\2\3\u00a7\u00a4\3\2\2\2\u00a7"+
		"\u00a6\3\2\2\2\u00a8\f\3\2\2\2\u00a9\u00aa\7^\2\2\u00aa\16\3\2\2\2\u00ab"+
		"\u00ac\7)\2\2\u00ac\20\3\2\2\2\u00ad\u00ae\7$\2\2\u00ae\22\3\2\2\2\u00af"+
		"\u00b0\7a\2\2\u00b0\24\3\2\2\2\u00b1\u00b2\7.\2\2\u00b2\26\3\2\2\2\u00b3"+
		"\u00b4\7=\2\2\u00b4\30\3\2\2\2\u00b5\u00b6\7~\2\2\u00b6\32\3\2\2\2\u00b7"+
		"\u00b8\7\60\2\2\u00b8\34\3\2\2\2\u00b9\u00ba\7*\2\2\u00ba\36\3\2\2\2\u00bb"+
		"\u00bc\7+\2\2\u00bc \3\2\2\2\u00bd\u00be\7}\2\2\u00be\"\3\2\2\2\u00bf"+
		"\u00c0\7\177\2\2\u00c0$\3\2\2\2\u00c1\u00c2\7]\2\2\u00c2&\3\2\2\2\u00c3"+
		"\u00c4\7_\2\2\u00c4(\3\2\2\2\u00c5\u00c6\7,\2\2\u00c6*\3\2\2\2\u00c7\u00c8"+
		"\7\61\2\2\u00c8,\3\2\2\2\u00c9\u00ca\7\'\2\2\u00ca.\3\2\2\2\u00cb\u00cc"+
		"\7-\2\2\u00cc\60\3\2\2\2\u00cd\u00ce\7/\2\2\u00ce\62\3\2\2\2\u00cf\u00d0"+
		"\7&\2\2\u00d0\u00d1\7}\2\2\u00d1\64\3\2\2\2\u00d2\u00d3\7A\2\2\u00d3\u00d4"+
		"\7A\2\2\u00d4\66\3\2\2\2\u00d5\u00d9\t\4\2\2\u00d6\u00d9\5\23\t\2\u00d7"+
		"\u00d9\t\5\2\2\u00d8\u00d5\3\2\2\2\u00d8\u00d6\3\2\2\2\u00d8\u00d7\3\2"+
		"\2\2\u00d98\3\2\2\2\u00da\u00df\5\r\6\2\u00db\u00e0\t\6\2\2\u00dc\u00e0"+
		"\5;\35\2\u00dd\u00e0\13\2\2\2\u00de\u00e0\7\2\2\3\u00df\u00db\3\2\2\2"+
		"\u00df\u00dc\3\2\2\2\u00df\u00dd\3\2\2\2\u00df\u00de\3\2\2\2\u00e0:\3"+
		"\2\2\2\u00e1\u00ec\7w\2\2\u00e2\u00ea\5A \2\u00e3\u00e8\5A \2\u00e4\u00e6"+
		"\5A \2\u00e5\u00e7\5A \2\u00e6\u00e5\3\2\2\2\u00e6\u00e7\3\2\2\2\u00e7"+
		"\u00e9\3\2\2\2\u00e8\u00e4\3\2\2\2\u00e8\u00e9\3\2\2\2\u00e9\u00eb\3\2"+
		"\2\2\u00ea\u00e3\3\2\2\2\u00ea\u00eb\3\2\2\2\u00eb\u00ed\3\2\2\2\u00ec"+
		"\u00e2\3\2\2\2\u00ec\u00ed\3\2\2\2\u00ed<\3\2\2\2\u00ee\u00f3\5\17\7\2"+
		"\u00ef\u00f2\59\34\2\u00f0\u00f2\n\7\2\2\u00f1\u00ef\3\2\2\2\u00f1\u00f0"+
		"\3\2\2\2\u00f2\u00f5\3\2\2\2\u00f3\u00f1\3\2\2\2\u00f3\u00f4\3\2\2\2\u00f4"+
		"\u00f6\3\2\2\2\u00f5\u00f3\3\2\2\2\u00f6\u00f7\5\17\7\2\u00f7>\3\2\2\2"+
		"\u00f8\u00fd\5\21\b\2\u00f9\u00fc\59\34\2\u00fa\u00fc\n\b\2\2\u00fb\u00f9"+
		"\3\2\2\2\u00fb\u00fa\3\2\2\2\u00fc\u00ff\3\2\2\2\u00fd\u00fb\3\2\2\2\u00fd"+
		"\u00fe\3\2\2\2\u00fe\u0100\3\2\2\2\u00ff\u00fd\3\2\2\2\u0100\u0101\5\21"+
		"\b\2\u0101@\3\2\2\2\u0102\u0103\t\t\2\2\u0103B\3\2\2\2\u0104\u0105\t\n"+
		"\2\2\u0105D\3\2\2\2\u0106\u0108\5C!\2\u0107\u0106\3\2\2\2\u0108\u0109"+
		"\3\2\2\2\u0109\u0107\3\2\2\2\u0109\u010a\3\2\2\2\u010aF\3\2\2\2\u010b"+
		"\u010c\5E\"\2\u010c\u010e\5\33\r\2\u010d\u010f\5E\"\2\u010e\u010d\3\2"+
		"\2\2\u010e\u010f\3\2\2\2\u010fH\3\2\2\2\u0110\u0111\5\63\31\2\u0111\u0112"+
		"\3\2\2\2\u0112\u0113\b$\2\2\u0113J\3\2\2\2\u0114\u0115\13\2\2\2\u0115"+
		"L\3\2\2\2\u0116\u0117\5\13\5\2\u0117N\3\2\2\2\u0118\u011a\5\7\3\2\u0119"+
		"\u0118\3\2\2\2\u011a\u011b\3\2\2\2\u011b\u0119\3\2\2\2\u011b\u011c\3\2"+
		"\2\2\u011c\u011d\3\2\2\2\u011d\u011e\b\'\3\2\u011eP\3\2\2\2\u011f\u0121"+
		"\5\t\4\2\u0120\u011f\3\2\2\2\u0121\u0122\3\2\2\2\u0122\u0120\3\2\2\2\u0122"+
		"\u0123\3\2\2\2\u0123\u0124\3\2\2\2\u0124\u0125\b(\3\2\u0125R\3\2\2\2\u0126"+
		"\u0127\5!\20\2\u0127\u0128\3\2\2\2\u0128\u0129\b)\4\2\u0129T\3\2\2\2\u012a"+
		"\u012b\5#\21\2\u012b\u012c\3\2\2\2\u012c\u012d\b*\5\2\u012dV\3\2\2\2\u012e"+
		"\u012f\5\31\f\2\u012fX\3\2\2\2\u0130\u0131\5\33\r\2\u0131Z\3\2\2\2\u0132"+
		"\u0133\5\35\16\2\u0133\\\3\2\2\2\u0134\u0135\5\37\17\2\u0135^\3\2\2\2"+
		"\u0136\u0137\5%\22\2\u0137`\3\2\2\2\u0138\u0139\5\'\23\2\u0139b\3\2\2"+
		"\2\u013a\u013b\5\65\32\2\u013bd\3\2\2\2\u013c\u013d\5\27\13\2\u013df\3"+
		"\2\2\2\u013e\u013f\5\25\n\2\u013fh\3\2\2\2\u0140\u0141\5)\24\2\u0141j"+
		"\3\2\2\2\u0142\u0143\5+\25\2\u0143l\3\2\2\2\u0144\u0145\5-\26\2\u0145"+
		"n\3\2\2\2\u0146\u0147\5/\27\2\u0147p\3\2\2\2\u0148\u0149\5\61\30\2\u0149"+
		"r\3\2\2\2\u014a\u014f\5\67\33\2\u014b\u014e\5\67\33\2\u014c\u014e\5C!"+
		"\2\u014d\u014b\3\2\2\2\u014d\u014c\3\2\2\2\u014e\u0151\3\2\2\2\u014f\u014d"+
		"\3\2\2\2\u014f\u0150\3\2\2\2\u0150t\3\2\2\2\u0151\u014f\3\2\2\2\u0152"+
		"\u0153\5?\37\2\u0153v\3\2\2\2\u0154\u0155\5=\36\2\u0155x\3\2\2\2\u0156"+
		"\u0157\5E\"\2\u0157z\3\2\2\2\u0158\u0159\5G#\2\u0159|\3\2\2\2\u015a\u015b"+
		"\t\2\2\2\u015b\u015c\3\2\2\2\u015c\u015d\b>\3\2\u015d~\3\2\2\2\u015e\u0160"+
		"\5\7\3\2\u015f\u015e\3\2\2\2\u0160\u0161\3\2\2\2\u0161\u015f\3\2\2\2\u0161"+
		"\u0162\3\2\2\2\u0162\u0163\3\2\2\2\u0163\u0164\b?\3\2\u0164\u0080\3\2"+
		"\2\2\u0165\u0167\5\t\4\2\u0166\u0165\3\2\2\2\u0167\u0168\3\2\2\2\u0168"+
		"\u0166\3\2\2\2\u0168\u0169\3\2\2\2\u0169\u016a\3\2\2\2\u016a\u016b\b@"+
		"\3\2\u016b\u0082\3\2\2\2\u016c\u016d\5#\21\2\u016d\u016e\3\2\2\2\u016e"+
		"\u016f\bA\5\2\u016f\u0170\bA\6\2\u0170\u0084\3\2\2\2\u0171\u0172\5\25"+
		"\n\2\u0172\u0173\3\2\2\2\u0173\u0174\bB\7\2\u0174\u0086\3\2\2\2\u0175"+
		"\u017a\5\67\33\2\u0176\u0179\5\67\33\2\u0177\u0179\5C!\2\u0178\u0176\3"+
		"\2\2\2\u0178\u0177\3\2\2\2\u0179\u017c\3\2\2\2\u017a\u0178\3\2\2\2\u017a"+
		"\u017b\3\2\2\2\u017b\u017d\3\2\2\2\u017c\u017a\3\2\2\2\u017d\u017e\bC"+
		"\b\2\u017e\u0088\3\2\2\2\u017f\u0180\5?\37\2\u0180\u0181\3\2\2\2\u0181"+
		"\u0182\bD\t\2\u0182\u008a\3\2\2\2\u0183\u0184\5=\36\2\u0184\u0185\3\2"+
		"\2\2\u0185\u0186\bE\n\2\u0186\u008c\3\2\2\2\u0187\u0188\5E\"\2\u0188\u0189"+
		"\3\2\2\2\u0189\u018a\bF\13\2\u018a\u008e\3\2\2\2\u018b\u018c\5G#\2\u018c"+
		"\u018d\3\2\2\2\u018d\u018e\bG\f\2\u018e\u0090\3\2\2\2\u018f\u0190\t\2"+
		"\2\2\u0190\u0191\3\2\2\2\u0191\u0192\bH\3\2\u0192\u0092\3\2\2\2\34\2\3"+
		"\4\u0095\u00a1\u00a7\u00d8\u00df\u00e6\u00e8\u00ea\u00ec\u00f1\u00f3\u00fb"+
		"\u00fd\u0109\u010e\u011b\u0122\u014d\u014f\u0161\u0168\u0178\u017a\r\7"+
		"\3\2\b\2\2\7\4\2\6\2\2\t\t\2\t\22\2\t\30\2\t\31\2\t\32\2\t\33\2\t\34\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}