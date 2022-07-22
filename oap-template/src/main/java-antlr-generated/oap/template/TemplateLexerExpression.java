// Generated from TemplateLexerExpression.g4 by ANTLR 4.9.3

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
	static { RuntimeMetaData.checkVersion("4.9.3", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		BLOCK_COMMENT=1, HORZ_WS=2, VERT_WS=3, LBRACE=4, RBRACE=5, PIPE=6, DOT=7, 
		LPAREN=8, RPAREN=9, LBRACK=10, RBRACK=11, DQUESTION=12, SEMI=13, COMMA=14, 
		STAR=15, SLASH=16, PERCENT=17, PLUS=18, MINUS=19, DSTRING=20, SSTRING=21, 
		DECDIGITS=22, FLOAT=23, BOOLEAN=24, ID=25, CAST_TYPE=26, ERR_CHAR=27, 
		C_HORZ_WS=28, C_VERT_WS=29, CERR_CHAR=30;
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
			"Star", "Slash", "Percent", "Plus", "Minus", "DQuestion", "LT", "GT", 
			"NameChar", "EscSeq", "UnicodeEsc", "SQuoteLiteral", "DQuoteLiteral", 
			"BoolLiteral", "HexDigit", "DecDigit", "DecDigits", "Float", "True", 
			"False", "BLOCK_COMMENT", "HORZ_WS", "VERT_WS", "LBRACE", "RBRACE", "PIPE", 
			"DOT", "LPAREN", "RPAREN", "LBRACK", "RBRACK", "DQUESTION", "SEMI", "COMMA", 
			"STAR", "SLASH", "PERCENT", "PLUS", "MINUS", "DSTRING", "SSTRING", "DECDIGITS", 
			"FLOAT", "BOOLEAN", "ID", "CAST_TYPE", "ERR_CHAR", "LBrace", "RBrace", 
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
			null, "BLOCK_COMMENT", "HORZ_WS", "VERT_WS", "LBRACE", "RBRACE", "PIPE", 
			"DOT", "LPAREN", "RPAREN", "LBRACK", "RBRACK", "DQUESTION", "SEMI", "COMMA", 
			"STAR", "SLASH", "PERCENT", "PLUS", "MINUS", "DSTRING", "SSTRING", "DECDIGITS", 
			"FLOAT", "BOOLEAN", "ID", "CAST_TYPE", "ERR_CHAR", "C_HORZ_WS", "C_VERT_WS", 
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
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2 \u01ae\b\1\b\1\4"+
		"\2\t\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n"+
		"\4\13\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22"+
		"\t\22\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31"+
		"\t\31\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t\37\4 \t"+
		" \4!\t!\4\"\t\"\4#\t#\4$\t$\4%\t%\4&\t&\4\'\t\'\4(\t(\4)\t)\4*\t*\4+\t"+
		"+\4,\t,\4-\t-\4.\t.\4/\t/\4\60\t\60\4\61\t\61\4\62\t\62\4\63\t\63\4\64"+
		"\t\64\4\65\t\65\4\66\t\66\4\67\t\67\48\t8\49\t9\4:\t:\4;\t;\4<\t<\4=\t"+
		"=\4>\t>\4?\t?\4@\t@\4A\tA\4B\tB\4C\tC\4D\tD\4E\tE\4F\tF\4G\tG\4H\tH\4"+
		"I\tI\4J\tJ\4K\tK\4L\tL\3\2\3\2\5\2\u009d\n\2\3\3\3\3\3\4\3\4\3\5\3\5\3"+
		"\5\3\5\7\5\u00a7\n\5\f\5\16\5\u00aa\13\5\3\5\3\5\3\5\5\5\u00af\n\5\3\6"+
		"\3\6\3\7\3\7\3\b\3\b\3\t\3\t\3\n\3\n\3\13\3\13\3\f\3\f\3\r\3\r\3\16\3"+
		"\16\3\17\3\17\3\20\3\20\3\21\3\21\3\22\3\22\3\23\3\23\3\24\3\24\3\25\3"+
		"\25\3\26\3\26\3\27\3\27\3\27\3\30\3\30\3\31\3\31\3\32\3\32\3\32\5\32\u00dd"+
		"\n\32\3\33\3\33\3\33\3\33\3\33\5\33\u00e4\n\33\3\34\3\34\3\34\3\34\3\34"+
		"\5\34\u00eb\n\34\5\34\u00ed\n\34\5\34\u00ef\n\34\5\34\u00f1\n\34\3\35"+
		"\3\35\3\35\7\35\u00f6\n\35\f\35\16\35\u00f9\13\35\3\35\3\35\3\36\3\36"+
		"\3\36\7\36\u0100\n\36\f\36\16\36\u0103\13\36\3\36\3\36\3\37\3\37\5\37"+
		"\u0109\n\37\3 \3 \3!\3!\3\"\6\"\u0110\n\"\r\"\16\"\u0111\3#\3#\3#\5#\u0117"+
		"\n#\3$\3$\3$\3$\3$\3%\3%\3%\3%\3%\3%\3&\3&\3\'\6\'\u0127\n\'\r\'\16\'"+
		"\u0128\3\'\3\'\3(\6(\u012e\n(\r(\16(\u012f\3(\3(\3)\3)\3)\3)\3*\3*\3*"+
		"\3*\3+\3+\3,\3,\3-\3-\3.\3.\3/\3/\3\60\3\60\3\61\3\61\3\62\3\62\3\63\3"+
		"\63\3\64\3\64\3\65\3\65\3\66\3\66\3\67\3\67\38\38\39\39\3:\3:\3;\3;\3"+
		"<\3<\3=\3=\3>\3>\3>\7>\u0165\n>\f>\16>\u0168\13>\3?\3?\6?\u016c\n?\r?"+
		"\16?\u016d\3?\3?\3@\3@\3@\3@\3A\3A\3B\3B\3C\6C\u017b\nC\rC\16C\u017c\3"+
		"C\3C\3D\6D\u0182\nD\rD\16D\u0183\3D\3D\3E\3E\3E\3E\3E\3F\3F\3F\3F\3G\3"+
		"G\3G\7G\u0194\nG\fG\16G\u0197\13G\3G\3G\3H\3H\3H\3H\3I\3I\3I\3I\3J\3J"+
		"\3J\3J\3K\3K\3K\3K\3L\3L\3L\3L\3\u00a8\2M\4\2\6\2\b\2\n\2\f\2\16\2\20"+
		"\2\22\2\24\2\26\2\30\2\32\2\34\2\36\2 \2\"\2$\2&\2(\2*\2,\2.\2\60\2\62"+
		"\2\64\2\66\28\2:\2<\2>\2@\2B\2D\2F\2H\2J\2L\3N\4P\5R\6T\7V\bX\tZ\n\\\13"+
		"^\f`\rb\16d\17f\20h\21j\22l\23n\24p\25r\26t\27v\30x\31z\32|\33~\34\u0080"+
		"\35\u0082\2\u0084\2\u0086\36\u0088\37\u008a\2\u008c\2\u008e\2\u0090\2"+
		"\u0092\2\u0094\2\u0096\2\u0098 \4\2\3\13\4\2\13\13\"\"\4\2\f\f\16\17\17"+
		"\2C\\c|\u00c2\u00d8\u00da\u00f8\u00fa\u0301\u0372\u037f\u0381\u2001\u200e"+
		"\u200f\u2072\u2191\u2c02\u2ff1\u3003\ud801\uf902\ufdd1\ufdf2\uffff\5\2"+
		"\u00b9\u00b9\u0302\u0371\u2041\u2042\n\2$$))^^ddhhppttvv\6\2\f\f\17\17"+
		"))^^\6\2\f\f\17\17$$^^\5\2\62;CHch\3\2\62;\2\u01a2\2L\3\2\2\2\2N\3\2\2"+
		"\2\2P\3\2\2\2\2R\3\2\2\2\2T\3\2\2\2\2V\3\2\2\2\2X\3\2\2\2\2Z\3\2\2\2\2"+
		"\\\3\2\2\2\2^\3\2\2\2\2`\3\2\2\2\2b\3\2\2\2\2d\3\2\2\2\2f\3\2\2\2\2h\3"+
		"\2\2\2\2j\3\2\2\2\2l\3\2\2\2\2n\3\2\2\2\2p\3\2\2\2\2r\3\2\2\2\2t\3\2\2"+
		"\2\2v\3\2\2\2\2x\3\2\2\2\2z\3\2\2\2\2|\3\2\2\2\2~\3\2\2\2\2\u0080\3\2"+
		"\2\2\3\u0086\3\2\2\2\3\u0088\3\2\2\2\3\u008a\3\2\2\2\3\u008c\3\2\2\2\3"+
		"\u008e\3\2\2\2\3\u0090\3\2\2\2\3\u0092\3\2\2\2\3\u0094\3\2\2\2\3\u0096"+
		"\3\2\2\2\3\u0098\3\2\2\2\4\u009c\3\2\2\2\6\u009e\3\2\2\2\b\u00a0\3\2\2"+
		"\2\n\u00a2\3\2\2\2\f\u00b0\3\2\2\2\16\u00b2\3\2\2\2\20\u00b4\3\2\2\2\22"+
		"\u00b6\3\2\2\2\24\u00b8\3\2\2\2\26\u00ba\3\2\2\2\30\u00bc\3\2\2\2\32\u00be"+
		"\3\2\2\2\34\u00c0\3\2\2\2\36\u00c2\3\2\2\2 \u00c4\3\2\2\2\"\u00c6\3\2"+
		"\2\2$\u00c8\3\2\2\2&\u00ca\3\2\2\2(\u00cc\3\2\2\2*\u00ce\3\2\2\2,\u00d0"+
		"\3\2\2\2.\u00d2\3\2\2\2\60\u00d5\3\2\2\2\62\u00d7\3\2\2\2\64\u00dc\3\2"+
		"\2\2\66\u00de\3\2\2\28\u00e5\3\2\2\2:\u00f2\3\2\2\2<\u00fc\3\2\2\2>\u0108"+
		"\3\2\2\2@\u010a\3\2\2\2B\u010c\3\2\2\2D\u010f\3\2\2\2F\u0113\3\2\2\2H"+
		"\u0118\3\2\2\2J\u011d\3\2\2\2L\u0123\3\2\2\2N\u0126\3\2\2\2P\u012d\3\2"+
		"\2\2R\u0133\3\2\2\2T\u0137\3\2\2\2V\u013b\3\2\2\2X\u013d\3\2\2\2Z\u013f"+
		"\3\2\2\2\\\u0141\3\2\2\2^\u0143\3\2\2\2`\u0145\3\2\2\2b\u0147\3\2\2\2"+
		"d\u0149\3\2\2\2f\u014b\3\2\2\2h\u014d\3\2\2\2j\u014f\3\2\2\2l\u0151\3"+
		"\2\2\2n\u0153\3\2\2\2p\u0155\3\2\2\2r\u0157\3\2\2\2t\u0159\3\2\2\2v\u015b"+
		"\3\2\2\2x\u015d\3\2\2\2z\u015f\3\2\2\2|\u0161\3\2\2\2~\u0169\3\2\2\2\u0080"+
		"\u0171\3\2\2\2\u0082\u0175\3\2\2\2\u0084\u0177\3\2\2\2\u0086\u017a\3\2"+
		"\2\2\u0088\u0181\3\2\2\2\u008a\u0187\3\2\2\2\u008c\u018c\3\2\2\2\u008e"+
		"\u0190\3\2\2\2\u0090\u019a\3\2\2\2\u0092\u019e\3\2\2\2\u0094\u01a2\3\2"+
		"\2\2\u0096\u01a6\3\2\2\2\u0098\u01aa\3\2\2\2\u009a\u009d\5\6\3\2\u009b"+
		"\u009d\5\b\4\2\u009c\u009a\3\2\2\2\u009c\u009b\3\2\2\2\u009d\5\3\2\2\2"+
		"\u009e\u009f\t\2\2\2\u009f\7\3\2\2\2\u00a0\u00a1\t\3\2\2\u00a1\t\3\2\2"+
		"\2\u00a2\u00a3\7\61\2\2\u00a3\u00a4\7,\2\2\u00a4\u00a8\3\2\2\2\u00a5\u00a7"+
		"\13\2\2\2\u00a6\u00a5\3\2\2\2\u00a7\u00aa\3\2\2\2\u00a8\u00a9\3\2\2\2"+
		"\u00a8\u00a6\3\2\2\2\u00a9\u00ae\3\2\2\2\u00aa\u00a8\3\2\2\2\u00ab\u00ac"+
		"\7,\2\2\u00ac\u00af\7\61\2\2\u00ad\u00af\7\2\2\3\u00ae\u00ab\3\2\2\2\u00ae"+
		"\u00ad\3\2\2\2\u00af\13\3\2\2\2\u00b0\u00b1\7^\2\2\u00b1\r\3\2\2\2\u00b2"+
		"\u00b3\7)\2\2\u00b3\17\3\2\2\2\u00b4\u00b5\7$\2\2\u00b5\21\3\2\2\2\u00b6"+
		"\u00b7\7a\2\2\u00b7\23\3\2\2\2\u00b8\u00b9\7.\2\2\u00b9\25\3\2\2\2\u00ba"+
		"\u00bb\7=\2\2\u00bb\27\3\2\2\2\u00bc\u00bd\7~\2\2\u00bd\31\3\2\2\2\u00be"+
		"\u00bf\7\60\2\2\u00bf\33\3\2\2\2\u00c0\u00c1\7*\2\2\u00c1\35\3\2\2\2\u00c2"+
		"\u00c3\7+\2\2\u00c3\37\3\2\2\2\u00c4\u00c5\7]\2\2\u00c5!\3\2\2\2\u00c6"+
		"\u00c7\7_\2\2\u00c7#\3\2\2\2\u00c8\u00c9\7,\2\2\u00c9%\3\2\2\2\u00ca\u00cb"+
		"\7\61\2\2\u00cb\'\3\2\2\2\u00cc\u00cd\7\'\2\2\u00cd)\3\2\2\2\u00ce\u00cf"+
		"\7-\2\2\u00cf+\3\2\2\2\u00d0\u00d1\7/\2\2\u00d1-\3\2\2\2\u00d2\u00d3\7"+
		"A\2\2\u00d3\u00d4\7A\2\2\u00d4/\3\2\2\2\u00d5\u00d6\7>\2\2\u00d6\61\3"+
		"\2\2\2\u00d7\u00d8\7@\2\2\u00d8\63\3\2\2\2\u00d9\u00dd\t\4\2\2\u00da\u00dd"+
		"\5\22\t\2\u00db\u00dd\t\5\2\2\u00dc\u00d9\3\2\2\2\u00dc\u00da\3\2\2\2"+
		"\u00dc\u00db\3\2\2\2\u00dd\65\3\2\2\2\u00de\u00e3\5\f\6\2\u00df\u00e4"+
		"\t\6\2\2\u00e0\u00e4\58\34\2\u00e1\u00e4\13\2\2\2\u00e2\u00e4\7\2\2\3"+
		"\u00e3\u00df\3\2\2\2\u00e3\u00e0\3\2\2\2\u00e3\u00e1\3\2\2\2\u00e3\u00e2"+
		"\3\2\2\2\u00e4\67\3\2\2\2\u00e5\u00f0\7w\2\2\u00e6\u00ee\5@ \2\u00e7\u00ec"+
		"\5@ \2\u00e8\u00ea\5@ \2\u00e9\u00eb\5@ \2\u00ea\u00e9\3\2\2\2\u00ea\u00eb"+
		"\3\2\2\2\u00eb\u00ed\3\2\2\2\u00ec\u00e8\3\2\2\2\u00ec\u00ed\3\2\2\2\u00ed"+
		"\u00ef\3\2\2\2\u00ee\u00e7\3\2\2\2\u00ee\u00ef\3\2\2\2\u00ef\u00f1\3\2"+
		"\2\2\u00f0\u00e6\3\2\2\2\u00f0\u00f1\3\2\2\2\u00f19\3\2\2\2\u00f2\u00f7"+
		"\5\16\7\2\u00f3\u00f6\5\66\33\2\u00f4\u00f6\n\7\2\2\u00f5\u00f3\3\2\2"+
		"\2\u00f5\u00f4\3\2\2\2\u00f6\u00f9\3\2\2\2\u00f7\u00f5\3\2\2\2\u00f7\u00f8"+
		"\3\2\2\2\u00f8\u00fa\3\2\2\2\u00f9\u00f7\3\2\2\2\u00fa\u00fb\5\16\7\2"+
		"\u00fb;\3\2\2\2\u00fc\u0101\5\20\b\2\u00fd\u0100\5\66\33\2\u00fe\u0100"+
		"\n\b\2\2\u00ff\u00fd\3\2\2\2\u00ff\u00fe\3\2\2\2\u0100\u0103\3\2\2\2\u0101"+
		"\u00ff\3\2\2\2\u0101\u0102\3\2\2\2\u0102\u0104\3\2\2\2\u0103\u0101\3\2"+
		"\2\2\u0104\u0105\5\20\b\2\u0105=\3\2\2\2\u0106\u0109\5H$\2\u0107\u0109"+
		"\5J%\2\u0108\u0106\3\2\2\2\u0108\u0107\3\2\2\2\u0109?\3\2\2\2\u010a\u010b"+
		"\t\t\2\2\u010bA\3\2\2\2\u010c\u010d\t\n\2\2\u010dC\3\2\2\2\u010e\u0110"+
		"\5B!\2\u010f\u010e\3\2\2\2\u0110\u0111\3\2\2\2\u0111\u010f\3\2\2\2\u0111"+
		"\u0112\3\2\2\2\u0112E\3\2\2\2\u0113\u0114\5D\"\2\u0114\u0116\5\32\r\2"+
		"\u0115\u0117\5D\"\2\u0116\u0115\3\2\2\2\u0116\u0117\3\2\2\2\u0117G\3\2"+
		"\2\2\u0118\u0119\7v\2\2\u0119\u011a\7t\2\2\u011a\u011b\7w\2\2\u011b\u011c"+
		"\7g\2\2\u011cI\3\2\2\2\u011d\u011e\7h\2\2\u011e\u011f\7c\2\2\u011f\u0120"+
		"\7n\2\2\u0120\u0121\7u\2\2\u0121\u0122\7g\2\2\u0122K\3\2\2\2\u0123\u0124"+
		"\5\n\5\2\u0124M\3\2\2\2\u0125\u0127\5\6\3\2\u0126\u0125\3\2\2\2\u0127"+
		"\u0128\3\2\2\2\u0128\u0126\3\2\2\2\u0128\u0129\3\2\2\2\u0129\u012a\3\2"+
		"\2\2\u012a\u012b\b\'\2\2\u012bO\3\2\2\2\u012c\u012e\5\b\4\2\u012d\u012c"+
		"\3\2\2\2\u012e\u012f\3\2\2\2\u012f\u012d\3\2\2\2\u012f\u0130\3\2\2\2\u0130"+
		"\u0131\3\2\2\2\u0131\u0132\b(\2\2\u0132Q\3\2\2\2\u0133\u0134\5\u0082A"+
		"\2\u0134\u0135\3\2\2\2\u0135\u0136\b)\3\2\u0136S\3\2\2\2\u0137\u0138\5"+
		"\u0084B\2\u0138\u0139\3\2\2\2\u0139\u013a\b*\4\2\u013aU\3\2\2\2\u013b"+
		"\u013c\5\30\f\2\u013cW\3\2\2\2\u013d\u013e\5\32\r\2\u013eY\3\2\2\2\u013f"+
		"\u0140\5\34\16\2\u0140[\3\2\2\2\u0141\u0142\5\36\17\2\u0142]\3\2\2\2\u0143"+
		"\u0144\5 \20\2\u0144_\3\2\2\2\u0145\u0146\5\"\21\2\u0146a\3\2\2\2\u0147"+
		"\u0148\5.\27\2\u0148c\3\2\2\2\u0149\u014a\5\26\13\2\u014ae\3\2\2\2\u014b"+
		"\u014c\5\24\n\2\u014cg\3\2\2\2\u014d\u014e\5$\22\2\u014ei\3\2\2\2\u014f"+
		"\u0150\5&\23\2\u0150k\3\2\2\2\u0151\u0152\5(\24\2\u0152m\3\2\2\2\u0153"+
		"\u0154\5*\25\2\u0154o\3\2\2\2\u0155\u0156\5,\26\2\u0156q\3\2\2\2\u0157"+
		"\u0158\5<\36\2\u0158s\3\2\2\2\u0159\u015a\5:\35\2\u015au\3\2\2\2\u015b"+
		"\u015c\5D\"\2\u015cw\3\2\2\2\u015d\u015e\5F#\2\u015ey\3\2\2\2\u015f\u0160"+
		"\5>\37\2\u0160{\3\2\2\2\u0161\u0166\5\64\32\2\u0162\u0165\5\64\32\2\u0163"+
		"\u0165\5B!\2\u0164\u0162\3\2\2\2\u0164\u0163\3\2\2\2\u0165\u0168\3\2\2"+
		"\2\u0166\u0164\3\2\2\2\u0166\u0167\3\2\2\2\u0167}\3\2\2\2\u0168\u0166"+
		"\3\2\2\2\u0169\u016b\5\60\30\2\u016a\u016c\5\64\32\2\u016b\u016a\3\2\2"+
		"\2\u016c\u016d\3\2\2\2\u016d\u016b\3\2\2\2\u016d\u016e\3\2\2\2\u016e\u016f"+
		"\3\2\2\2\u016f\u0170\5\62\31\2\u0170\177\3\2\2\2\u0171\u0172\t\2\2\2\u0172"+
		"\u0173\3\2\2\2\u0173\u0174\b@\2\2\u0174\u0081\3\2\2\2\u0175\u0176\7}\2"+
		"\2\u0176\u0083\3\2\2\2\u0177\u0178\7\177\2\2\u0178\u0085\3\2\2\2\u0179"+
		"\u017b\5\6\3\2\u017a\u0179\3\2\2\2\u017b\u017c\3\2\2\2\u017c\u017a\3\2"+
		"\2\2\u017c\u017d\3\2\2\2\u017d\u017e\3\2\2\2\u017e\u017f\bC\2\2\u017f"+
		"\u0087\3\2\2\2\u0180\u0182\5\b\4\2\u0181\u0180\3\2\2\2\u0182\u0183\3\2"+
		"\2\2\u0183\u0181\3\2\2\2\u0183\u0184\3\2\2\2\u0184\u0185\3\2\2\2\u0185"+
		"\u0186\bD\2\2\u0186\u0089\3\2\2\2\u0187\u0188\5\u0084B\2\u0188\u0189\3"+
		"\2\2\2\u0189\u018a\bE\4\2\u018a\u018b\bE\5\2\u018b\u008b\3\2\2\2\u018c"+
		"\u018d\5\24\n\2\u018d\u018e\3\2\2\2\u018e\u018f\bF\6\2\u018f\u008d\3\2"+
		"\2\2\u0190\u0195\5\64\32\2\u0191\u0194\5\64\32\2\u0192\u0194\5B!\2\u0193"+
		"\u0191\3\2\2\2\u0193\u0192\3\2\2\2\u0194\u0197\3\2\2\2\u0195\u0193\3\2"+
		"\2\2\u0195\u0196\3\2\2\2\u0196\u0198\3\2\2\2\u0197\u0195\3\2\2\2\u0198"+
		"\u0199\bG\7\2\u0199\u008f\3\2\2\2\u019a\u019b\5<\36\2\u019b\u019c\3\2"+
		"\2\2\u019c\u019d\bH\b\2\u019d\u0091\3\2\2\2\u019e\u019f\5:\35\2\u019f"+
		"\u01a0\3\2\2\2\u01a0\u01a1\bI\t\2\u01a1\u0093\3\2\2\2\u01a2\u01a3\5D\""+
		"\2\u01a3\u01a4\3\2\2\2\u01a4\u01a5\bJ\n\2\u01a5\u0095\3\2\2\2\u01a6\u01a7"+
		"\5F#\2\u01a7\u01a8\3\2\2\2\u01a8\u01a9\bK\13\2\u01a9\u0097\3\2\2\2\u01aa"+
		"\u01ab\t\2\2\2\u01ab\u01ac\3\2\2\2\u01ac\u01ad\bL\2\2\u01ad\u0099\3\2"+
		"\2\2\35\2\3\u009c\u00a8\u00ae\u00dc\u00e3\u00ea\u00ec\u00ee\u00f0\u00f5"+
		"\u00f7\u00ff\u0101\u0108\u0111\u0116\u0128\u012f\u0164\u0166\u016d\u017c"+
		"\u0183\u0193\u0195\f\b\2\2\7\3\2\6\2\2\t\7\2\t\20\2\t\33\2\t\26\2\t\27"+
		"\2\t\30\2\t\31\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}