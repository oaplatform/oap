// Generated from TemplateLexerExpression.g4 by ANTLR 4.8

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
public class TemplateLexerExpression extends Lexer {
	static { RuntimeMetaData.checkVersion("4.8", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		BLOCK_COMMENT=1, HORZ_WS=2, VERT_WS=3, LBRACE=4, RBRACE=5, PIPE=6, DOT=7, 
		LPAREN=8, RPAREN=9, LBRACK=10, RBRACK=11, DQUESTION=12, SEMI=13, COMMA=14, 
		STAR=15, SLASH=16, PERCENT=17, PLUS=18, MINUS=19, DSTRING=20, SSTRING=21, 
		DECDIGITS=22, FLOAT=23, BOOLEAN=24, ID=25, ERR_CHAR=26, C_HORZ_WS=27, 
		C_VERT_WS=28, CERR_CHAR=29;
	public static final int
		Concatenation=1;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE", "Concatenation"
	};

	private static String[] makeRuleNames() {
		return new String[] {
			"Ws", "Hws", "Vws", "BlockComment", "Esc", "SQuote", "DQuote", "Underscore", 
			"Comma", "Semi", "Pipe", "Dot", "LParen", "RParen", "LBrack", "RBrack", 
			"Star", "Slash", "Percent", "Plus", "Minus", "DQuestion", "NameChar", 
			"EscSeq", "UnicodeEsc", "SQuoteLiteral", "DQuoteLiteral", "BoolLiteral", 
			"HexDigit", "DecDigit", "DecDigits", "Float", "True", "False", "BLOCK_COMMENT", 
			"HORZ_WS", "VERT_WS", "LBRACE", "RBRACE", "PIPE", "DOT", "LPAREN", "RPAREN", 
			"LBRACK", "RBRACK", "DQUESTION", "SEMI", "COMMA", "STAR", "SLASH", "PERCENT", 
			"PLUS", "MINUS", "DSTRING", "SSTRING", "DECDIGITS", "FLOAT", "BOOLEAN", 
			"ID", "ERR_CHAR", "LBrace", "RBrace", "C_HORZ_WS", "C_VERT_WS", "CRBRACE", 
			"CCOMMA", "CID", "CDSTRING", "CSSTRING", "CDECDIGITS", "CFLOAT", "CERR_CHAR"
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
			null, "BLOCK_COMMENT", "HORZ_WS", "VERT_WS", "LBRACE", "RBRACE", "PIPE", 
			"DOT", "LPAREN", "RPAREN", "LBRACK", "RBRACK", "DQUESTION", "SEMI", "COMMA", 
			"STAR", "SLASH", "PERCENT", "PLUS", "MINUS", "DSTRING", "SSTRING", "DECDIGITS", 
			"FLOAT", "BOOLEAN", "ID", "ERR_CHAR", "C_HORZ_WS", "C_VERT_WS", "CERR_CHAR"
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


	public TemplateLexerExpression(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "TemplateLexerExpression.g4"; }

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
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2\37\u019c\b\1\b\1"+
		"\4\2\t\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t"+
		"\n\4\13\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4"+
		"\22\t\22\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4"+
		"\31\t\31\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t\37\4"+
		" \t \4!\t!\4\"\t\"\4#\t#\4$\t$\4%\t%\4&\t&\4\'\t\'\4(\t(\4)\t)\4*\t*\4"+
		"+\t+\4,\t,\4-\t-\4.\t.\4/\t/\4\60\t\60\4\61\t\61\4\62\t\62\4\63\t\63\4"+
		"\64\t\64\4\65\t\65\4\66\t\66\4\67\t\67\48\t8\49\t9\4:\t:\4;\t;\4<\t<\4"+
		"=\t=\4>\t>\4?\t?\4@\t@\4A\tA\4B\tB\4C\tC\4D\tD\4E\tE\4F\tF\4G\tG\4H\t"+
		"H\4I\tI\3\2\3\2\5\2\u0097\n\2\3\3\3\3\3\4\3\4\3\5\3\5\3\5\3\5\7\5\u00a1"+
		"\n\5\f\5\16\5\u00a4\13\5\3\5\3\5\3\5\5\5\u00a9\n\5\3\6\3\6\3\7\3\7\3\b"+
		"\3\b\3\t\3\t\3\n\3\n\3\13\3\13\3\f\3\f\3\r\3\r\3\16\3\16\3\17\3\17\3\20"+
		"\3\20\3\21\3\21\3\22\3\22\3\23\3\23\3\24\3\24\3\25\3\25\3\26\3\26\3\27"+
		"\3\27\3\27\3\30\3\30\3\30\5\30\u00d3\n\30\3\31\3\31\3\31\3\31\3\31\5\31"+
		"\u00da\n\31\3\32\3\32\3\32\3\32\3\32\5\32\u00e1\n\32\5\32\u00e3\n\32\5"+
		"\32\u00e5\n\32\5\32\u00e7\n\32\3\33\3\33\3\33\7\33\u00ec\n\33\f\33\16"+
		"\33\u00ef\13\33\3\33\3\33\3\34\3\34\3\34\7\34\u00f6\n\34\f\34\16\34\u00f9"+
		"\13\34\3\34\3\34\3\35\3\35\5\35\u00ff\n\35\3\36\3\36\3\37\3\37\3 \6 \u0106"+
		"\n \r \16 \u0107\3!\3!\3!\5!\u010d\n!\3\"\3\"\3\"\3\"\3\"\3#\3#\3#\3#"+
		"\3#\3#\3$\3$\3%\6%\u011d\n%\r%\16%\u011e\3%\3%\3&\6&\u0124\n&\r&\16&\u0125"+
		"\3&\3&\3\'\3\'\3\'\3\'\3(\3(\3(\3(\3)\3)\3*\3*\3+\3+\3,\3,\3-\3-\3.\3"+
		".\3/\3/\3\60\3\60\3\61\3\61\3\62\3\62\3\63\3\63\3\64\3\64\3\65\3\65\3"+
		"\66\3\66\3\67\3\67\38\38\39\39\3:\3:\3;\3;\3<\3<\3<\7<\u015b\n<\f<\16"+
		"<\u015e\13<\3=\3=\3=\3=\3>\3>\3?\3?\3@\6@\u0169\n@\r@\16@\u016a\3@\3@"+
		"\3A\6A\u0170\nA\rA\16A\u0171\3A\3A\3B\3B\3B\3B\3B\3C\3C\3C\3C\3D\3D\3"+
		"D\7D\u0182\nD\fD\16D\u0185\13D\3D\3D\3E\3E\3E\3E\3F\3F\3F\3F\3G\3G\3G"+
		"\3G\3H\3H\3H\3H\3I\3I\3I\3I\3\u00a2\2J\4\2\6\2\b\2\n\2\f\2\16\2\20\2\22"+
		"\2\24\2\26\2\30\2\32\2\34\2\36\2 \2\"\2$\2&\2(\2*\2,\2.\2\60\2\62\2\64"+
		"\2\66\28\2:\2<\2>\2@\2B\2D\2F\2H\3J\4L\5N\6P\7R\bT\tV\nX\13Z\f\\\r^\16"+
		"`\17b\20d\21f\22h\23j\24l\25n\26p\27r\30t\31v\32x\33z\34|\2~\2\u0080\35"+
		"\u0082\36\u0084\2\u0086\2\u0088\2\u008a\2\u008c\2\u008e\2\u0090\2\u0092"+
		"\37\4\2\3\13\4\2\13\13\"\"\4\2\f\f\16\17\17\2C\\c|\u00c2\u00d8\u00da\u00f8"+
		"\u00fa\u0301\u0372\u037f\u0381\u2001\u200e\u200f\u2072\u2191\u2c02\u2ff1"+
		"\u3003\ud801\uf902\ufdd1\ufdf2\uffff\5\2\u00b9\u00b9\u0302\u0371\u2041"+
		"\u2042\n\2$$))^^ddhhppttvv\6\2\f\f\17\17))^^\6\2\f\f\17\17$$^^\5\2\62"+
		";CHch\3\2\62;\2\u0191\2H\3\2\2\2\2J\3\2\2\2\2L\3\2\2\2\2N\3\2\2\2\2P\3"+
		"\2\2\2\2R\3\2\2\2\2T\3\2\2\2\2V\3\2\2\2\2X\3\2\2\2\2Z\3\2\2\2\2\\\3\2"+
		"\2\2\2^\3\2\2\2\2`\3\2\2\2\2b\3\2\2\2\2d\3\2\2\2\2f\3\2\2\2\2h\3\2\2\2"+
		"\2j\3\2\2\2\2l\3\2\2\2\2n\3\2\2\2\2p\3\2\2\2\2r\3\2\2\2\2t\3\2\2\2\2v"+
		"\3\2\2\2\2x\3\2\2\2\2z\3\2\2\2\3\u0080\3\2\2\2\3\u0082\3\2\2\2\3\u0084"+
		"\3\2\2\2\3\u0086\3\2\2\2\3\u0088\3\2\2\2\3\u008a\3\2\2\2\3\u008c\3\2\2"+
		"\2\3\u008e\3\2\2\2\3\u0090\3\2\2\2\3\u0092\3\2\2\2\4\u0096\3\2\2\2\6\u0098"+
		"\3\2\2\2\b\u009a\3\2\2\2\n\u009c\3\2\2\2\f\u00aa\3\2\2\2\16\u00ac\3\2"+
		"\2\2\20\u00ae\3\2\2\2\22\u00b0\3\2\2\2\24\u00b2\3\2\2\2\26\u00b4\3\2\2"+
		"\2\30\u00b6\3\2\2\2\32\u00b8\3\2\2\2\34\u00ba\3\2\2\2\36\u00bc\3\2\2\2"+
		" \u00be\3\2\2\2\"\u00c0\3\2\2\2$\u00c2\3\2\2\2&\u00c4\3\2\2\2(\u00c6\3"+
		"\2\2\2*\u00c8\3\2\2\2,\u00ca\3\2\2\2.\u00cc\3\2\2\2\60\u00d2\3\2\2\2\62"+
		"\u00d4\3\2\2\2\64\u00db\3\2\2\2\66\u00e8\3\2\2\28\u00f2\3\2\2\2:\u00fe"+
		"\3\2\2\2<\u0100\3\2\2\2>\u0102\3\2\2\2@\u0105\3\2\2\2B\u0109\3\2\2\2D"+
		"\u010e\3\2\2\2F\u0113\3\2\2\2H\u0119\3\2\2\2J\u011c\3\2\2\2L\u0123\3\2"+
		"\2\2N\u0129\3\2\2\2P\u012d\3\2\2\2R\u0131\3\2\2\2T\u0133\3\2\2\2V\u0135"+
		"\3\2\2\2X\u0137\3\2\2\2Z\u0139\3\2\2\2\\\u013b\3\2\2\2^\u013d\3\2\2\2"+
		"`\u013f\3\2\2\2b\u0141\3\2\2\2d\u0143\3\2\2\2f\u0145\3\2\2\2h\u0147\3"+
		"\2\2\2j\u0149\3\2\2\2l\u014b\3\2\2\2n\u014d\3\2\2\2p\u014f\3\2\2\2r\u0151"+
		"\3\2\2\2t\u0153\3\2\2\2v\u0155\3\2\2\2x\u0157\3\2\2\2z\u015f\3\2\2\2|"+
		"\u0163\3\2\2\2~\u0165\3\2\2\2\u0080\u0168\3\2\2\2\u0082\u016f\3\2\2\2"+
		"\u0084\u0175\3\2\2\2\u0086\u017a\3\2\2\2\u0088\u017e\3\2\2\2\u008a\u0188"+
		"\3\2\2\2\u008c\u018c\3\2\2\2\u008e\u0190\3\2\2\2\u0090\u0194\3\2\2\2\u0092"+
		"\u0198\3\2\2\2\u0094\u0097\5\6\3\2\u0095\u0097\5\b\4\2\u0096\u0094\3\2"+
		"\2\2\u0096\u0095\3\2\2\2\u0097\5\3\2\2\2\u0098\u0099\t\2\2\2\u0099\7\3"+
		"\2\2\2\u009a\u009b\t\3\2\2\u009b\t\3\2\2\2\u009c\u009d\7\61\2\2\u009d"+
		"\u009e\7,\2\2\u009e\u00a2\3\2\2\2\u009f\u00a1\13\2\2\2\u00a0\u009f\3\2"+
		"\2\2\u00a1\u00a4\3\2\2\2\u00a2\u00a3\3\2\2\2\u00a2\u00a0\3\2\2\2\u00a3"+
		"\u00a8\3\2\2\2\u00a4\u00a2\3\2\2\2\u00a5\u00a6\7,\2\2\u00a6\u00a9\7\61"+
		"\2\2\u00a7\u00a9\7\2\2\3\u00a8\u00a5\3\2\2\2\u00a8\u00a7\3\2\2\2\u00a9"+
		"\13\3\2\2\2\u00aa\u00ab\7^\2\2\u00ab\r\3\2\2\2\u00ac\u00ad\7)\2\2\u00ad"+
		"\17\3\2\2\2\u00ae\u00af\7$\2\2\u00af\21\3\2\2\2\u00b0\u00b1\7a\2\2\u00b1"+
		"\23\3\2\2\2\u00b2\u00b3\7.\2\2\u00b3\25\3\2\2\2\u00b4\u00b5\7=\2\2\u00b5"+
		"\27\3\2\2\2\u00b6\u00b7\7~\2\2\u00b7\31\3\2\2\2\u00b8\u00b9\7\60\2\2\u00b9"+
		"\33\3\2\2\2\u00ba\u00bb\7*\2\2\u00bb\35\3\2\2\2\u00bc\u00bd\7+\2\2\u00bd"+
		"\37\3\2\2\2\u00be\u00bf\7]\2\2\u00bf!\3\2\2\2\u00c0\u00c1\7_\2\2\u00c1"+
		"#\3\2\2\2\u00c2\u00c3\7,\2\2\u00c3%\3\2\2\2\u00c4\u00c5\7\61\2\2\u00c5"+
		"\'\3\2\2\2\u00c6\u00c7\7\'\2\2\u00c7)\3\2\2\2\u00c8\u00c9\7-\2\2\u00c9"+
		"+\3\2\2\2\u00ca\u00cb\7/\2\2\u00cb-\3\2\2\2\u00cc\u00cd\7A\2\2\u00cd\u00ce"+
		"\7A\2\2\u00ce/\3\2\2\2\u00cf\u00d3\t\4\2\2\u00d0\u00d3\5\22\t\2\u00d1"+
		"\u00d3\t\5\2\2\u00d2\u00cf\3\2\2\2\u00d2\u00d0\3\2\2\2\u00d2\u00d1\3\2"+
		"\2\2\u00d3\61\3\2\2\2\u00d4\u00d9\5\f\6\2\u00d5\u00da\t\6\2\2\u00d6\u00da"+
		"\5\64\32\2\u00d7\u00da\13\2\2\2\u00d8\u00da\7\2\2\3\u00d9\u00d5\3\2\2"+
		"\2\u00d9\u00d6\3\2\2\2\u00d9\u00d7\3\2\2\2\u00d9\u00d8\3\2\2\2\u00da\63"+
		"\3\2\2\2\u00db\u00e6\7w\2\2\u00dc\u00e4\5<\36\2\u00dd\u00e2\5<\36\2\u00de"+
		"\u00e0\5<\36\2\u00df\u00e1\5<\36\2\u00e0\u00df\3\2\2\2\u00e0\u00e1\3\2"+
		"\2\2\u00e1\u00e3\3\2\2\2\u00e2\u00de\3\2\2\2\u00e2\u00e3\3\2\2\2\u00e3"+
		"\u00e5\3\2\2\2\u00e4\u00dd\3\2\2\2\u00e4\u00e5\3\2\2\2\u00e5\u00e7\3\2"+
		"\2\2\u00e6\u00dc\3\2\2\2\u00e6\u00e7\3\2\2\2\u00e7\65\3\2\2\2\u00e8\u00ed"+
		"\5\16\7\2\u00e9\u00ec\5\62\31\2\u00ea\u00ec\n\7\2\2\u00eb\u00e9\3\2\2"+
		"\2\u00eb\u00ea\3\2\2\2\u00ec\u00ef\3\2\2\2\u00ed\u00eb\3\2\2\2\u00ed\u00ee"+
		"\3\2\2\2\u00ee\u00f0\3\2\2\2\u00ef\u00ed\3\2\2\2\u00f0\u00f1\5\16\7\2"+
		"\u00f1\67\3\2\2\2\u00f2\u00f7\5\20\b\2\u00f3\u00f6\5\62\31\2\u00f4\u00f6"+
		"\n\b\2\2\u00f5\u00f3\3\2\2\2\u00f5\u00f4\3\2\2\2\u00f6\u00f9\3\2\2\2\u00f7"+
		"\u00f5\3\2\2\2\u00f7\u00f8\3\2\2\2\u00f8\u00fa\3\2\2\2\u00f9\u00f7\3\2"+
		"\2\2\u00fa\u00fb\5\20\b\2\u00fb9\3\2\2\2\u00fc\u00ff\5D\"\2\u00fd\u00ff"+
		"\5F#\2\u00fe\u00fc\3\2\2\2\u00fe\u00fd\3\2\2\2\u00ff;\3\2\2\2\u0100\u0101"+
		"\t\t\2\2\u0101=\3\2\2\2\u0102\u0103\t\n\2\2\u0103?\3\2\2\2\u0104\u0106"+
		"\5>\37\2\u0105\u0104\3\2\2\2\u0106\u0107\3\2\2\2\u0107\u0105\3\2\2\2\u0107"+
		"\u0108\3\2\2\2\u0108A\3\2\2\2\u0109\u010a\5@ \2\u010a\u010c\5\32\r\2\u010b"+
		"\u010d\5@ \2\u010c\u010b\3\2\2\2\u010c\u010d\3\2\2\2\u010dC\3\2\2\2\u010e"+
		"\u010f\7v\2\2\u010f\u0110\7t\2\2\u0110\u0111\7w\2\2\u0111\u0112\7g\2\2"+
		"\u0112E\3\2\2\2\u0113\u0114\7h\2\2\u0114\u0115\7c\2\2\u0115\u0116\7n\2"+
		"\2\u0116\u0117\7u\2\2\u0117\u0118\7g\2\2\u0118G\3\2\2\2\u0119\u011a\5"+
		"\n\5\2\u011aI\3\2\2\2\u011b\u011d\5\6\3\2\u011c\u011b\3\2\2\2\u011d\u011e"+
		"\3\2\2\2\u011e\u011c\3\2\2\2\u011e\u011f\3\2\2\2\u011f\u0120\3\2\2\2\u0120"+
		"\u0121\b%\2\2\u0121K\3\2\2\2\u0122\u0124\5\b\4\2\u0123\u0122\3\2\2\2\u0124"+
		"\u0125\3\2\2\2\u0125\u0123\3\2\2\2\u0125\u0126\3\2\2\2\u0126\u0127\3\2"+
		"\2\2\u0127\u0128\b&\2\2\u0128M\3\2\2\2\u0129\u012a\5|>\2\u012a\u012b\3"+
		"\2\2\2\u012b\u012c\b\'\3\2\u012cO\3\2\2\2\u012d\u012e\5~?\2\u012e\u012f"+
		"\3\2\2\2\u012f\u0130\b(\4\2\u0130Q\3\2\2\2\u0131\u0132\5\30\f\2\u0132"+
		"S\3\2\2\2\u0133\u0134\5\32\r\2\u0134U\3\2\2\2\u0135\u0136\5\34\16\2\u0136"+
		"W\3\2\2\2\u0137\u0138\5\36\17\2\u0138Y\3\2\2\2\u0139\u013a\5 \20\2\u013a"+
		"[\3\2\2\2\u013b\u013c\5\"\21\2\u013c]\3\2\2\2\u013d\u013e\5.\27\2\u013e"+
		"_\3\2\2\2\u013f\u0140\5\26\13\2\u0140a\3\2\2\2\u0141\u0142\5\24\n\2\u0142"+
		"c\3\2\2\2\u0143\u0144\5$\22\2\u0144e\3\2\2\2\u0145\u0146\5&\23\2\u0146"+
		"g\3\2\2\2\u0147\u0148\5(\24\2\u0148i\3\2\2\2\u0149\u014a\5*\25\2\u014a"+
		"k\3\2\2\2\u014b\u014c\5,\26\2\u014cm\3\2\2\2\u014d\u014e\58\34\2\u014e"+
		"o\3\2\2\2\u014f\u0150\5\66\33\2\u0150q\3\2\2\2\u0151\u0152\5@ \2\u0152"+
		"s\3\2\2\2\u0153\u0154\5B!\2\u0154u\3\2\2\2\u0155\u0156\5:\35\2\u0156w"+
		"\3\2\2\2\u0157\u015c\5\60\30\2\u0158\u015b\5\60\30\2\u0159\u015b\5>\37"+
		"\2\u015a\u0158\3\2\2\2\u015a\u0159\3\2\2\2\u015b\u015e\3\2\2\2\u015c\u015a"+
		"\3\2\2\2\u015c\u015d\3\2\2\2\u015dy\3\2\2\2\u015e\u015c\3\2\2\2\u015f"+
		"\u0160\t\2\2\2\u0160\u0161\3\2\2\2\u0161\u0162\b=\2\2\u0162{\3\2\2\2\u0163"+
		"\u0164\7}\2\2\u0164}\3\2\2\2\u0165\u0166\7\177\2\2\u0166\177\3\2\2\2\u0167"+
		"\u0169\5\6\3\2\u0168\u0167\3\2\2\2\u0169\u016a\3\2\2\2\u016a\u0168\3\2"+
		"\2\2\u016a\u016b\3\2\2\2\u016b\u016c\3\2\2\2\u016c\u016d\b@\2\2\u016d"+
		"\u0081\3\2\2\2\u016e\u0170\5\b\4\2\u016f\u016e\3\2\2\2\u0170\u0171\3\2"+
		"\2\2\u0171\u016f\3\2\2\2\u0171\u0172\3\2\2\2\u0172\u0173\3\2\2\2\u0173"+
		"\u0174\bA\2\2\u0174\u0083\3\2\2\2\u0175\u0176\5~?\2\u0176\u0177\3\2\2"+
		"\2\u0177\u0178\bB\4\2\u0178\u0179\bB\5\2\u0179\u0085\3\2\2\2\u017a\u017b"+
		"\5\24\n\2\u017b\u017c\3\2\2\2\u017c\u017d\bC\6\2\u017d\u0087\3\2\2\2\u017e"+
		"\u0183\5\60\30\2\u017f\u0182\5\60\30\2\u0180\u0182\5>\37\2\u0181\u017f"+
		"\3\2\2\2\u0181\u0180\3\2\2\2\u0182\u0185\3\2\2\2\u0183\u0181\3\2\2\2\u0183"+
		"\u0184\3\2\2\2\u0184\u0186\3\2\2\2\u0185\u0183\3\2\2\2\u0186\u0187\bD"+
		"\7\2\u0187\u0089\3\2\2\2\u0188\u0189\58\34\2\u0189\u018a\3\2\2\2\u018a"+
		"\u018b\bE\b\2\u018b\u008b\3\2\2\2\u018c\u018d\5\66\33\2\u018d\u018e\3"+
		"\2\2\2\u018e\u018f\bF\t\2\u018f\u008d\3\2\2\2\u0190\u0191\5@ \2\u0191"+
		"\u0192\3\2\2\2\u0192\u0193\bG\n\2\u0193\u008f\3\2\2\2\u0194\u0195\5B!"+
		"\2\u0195\u0196\3\2\2\2\u0196\u0197\bH\13\2\u0197\u0091\3\2\2\2\u0198\u0199"+
		"\t\2\2\2\u0199\u019a\3\2\2\2\u019a\u019b\bI\2\2\u019b\u0093\3\2\2\2\34"+
		"\2\3\u0096\u00a2\u00a8\u00d2\u00d9\u00e0\u00e2\u00e4\u00e6\u00eb\u00ed"+
		"\u00f5\u00f7\u00fe\u0107\u010c\u011e\u0125\u015a\u015c\u016a\u0171\u0181"+
		"\u0183\f\b\2\2\7\3\2\6\2\2\t\7\2\t\20\2\t\33\2\t\26\2\t\27\2\t\30\2\t"+
		"\31\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}