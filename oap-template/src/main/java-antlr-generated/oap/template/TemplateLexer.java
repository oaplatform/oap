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
		STARTEXPR=1, TEXT=2, LBRACE=3, RBRACE=4, PIPE=5, DOT=6, LPAREN=7, RPAREN=8, 
		LBRACK=9, RBRACK=10, DQUESTION=11, SEMI=12, COMMA=13, ID=14, DSTRING=15, 
		SSTRING=16, DECDIGITS=17, FLOAT=18, ERR_CHAR=19, CRBRACE=20, CCOMMA=21, 
		CID=22, CDSTRING=23, CSSTRING=24, CDECDIGITS=25, CFLOAT=26, CERR_CHAR=27;
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
			"Esc", "SQuote", "DQuote", "Underscore", "Comma", "Semi", "Pipe", "Dot", 
			"LParen", "RParen", "LBrace", "RBrace", "LBrack", "RBrack", "StartExpr", 
			"DQuestion", "NameChar", "EscSeq", "UnicodeEsc", "SQuoteLiteral", "DQuoteLiteral", 
			"HexDigit", "DecDigit", "DecDigits", "Float", "STARTEXPR", "TEXT", "LBRACE", 
			"RBRACE", "PIPE", "DOT", "LPAREN", "RPAREN", "LBRACK", "RBRACK", "DQUESTION", 
			"SEMI", "COMMA", "ID", "DSTRING", "SSTRING", "DECDIGITS", "FLOAT", "ERR_CHAR", 
			"CRBRACE", "CCOMMA", "CID", "CDSTRING", "CSSTRING", "CDECDIGITS", "CFLOAT", 
			"CERR_CHAR"
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
			null, "STARTEXPR", "TEXT", "LBRACE", "RBRACE", "PIPE", "DOT", "LPAREN", 
			"RPAREN", "LBRACK", "RBRACK", "DQUESTION", "SEMI", "COMMA", "ID", "DSTRING", 
			"SSTRING", "DECDIGITS", "FLOAT", "ERR_CHAR", "CRBRACE", "CCOMMA", "CID", 
			"CDSTRING", "CSSTRING", "CDECDIGITS", "CFLOAT", "CERR_CHAR"
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
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2\35\u0118\b\1\b\1"+
		"\b\1\4\2\t\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4"+
		"\n\t\n\4\13\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t"+
		"\21\4\22\t\22\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t"+
		"\30\4\31\t\31\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t"+
		"\37\4 \t \4!\t!\4\"\t\"\4#\t#\4$\t$\4%\t%\4&\t&\4\'\t\'\4(\t(\4)\t)\4"+
		"*\t*\4+\t+\4,\t,\4-\t-\4.\t.\4/\t/\4\60\t\60\4\61\t\61\4\62\t\62\4\63"+
		"\t\63\4\64\t\64\4\65\t\65\3\2\3\2\3\3\3\3\3\4\3\4\3\5\3\5\3\6\3\6\3\7"+
		"\3\7\3\b\3\b\3\t\3\t\3\n\3\n\3\13\3\13\3\f\3\f\3\r\3\r\3\16\3\16\3\17"+
		"\3\17\3\20\3\20\3\20\3\21\3\21\3\21\3\22\3\22\3\22\5\22\u0093\n\22\3\23"+
		"\3\23\3\23\3\23\3\23\5\23\u009a\n\23\3\24\3\24\3\24\3\24\3\24\5\24\u00a1"+
		"\n\24\5\24\u00a3\n\24\5\24\u00a5\n\24\5\24\u00a7\n\24\3\25\3\25\3\25\7"+
		"\25\u00ac\n\25\f\25\16\25\u00af\13\25\3\25\3\25\3\26\3\26\3\26\7\26\u00b6"+
		"\n\26\f\26\16\26\u00b9\13\26\3\26\3\26\3\27\3\27\3\30\3\30\3\31\6\31\u00c2"+
		"\n\31\r\31\16\31\u00c3\3\32\3\32\3\32\5\32\u00c9\n\32\3\33\3\33\3\33\3"+
		"\33\3\34\3\34\3\35\3\35\3\35\3\35\3\36\3\36\3\36\3\36\3\37\3\37\3 \3 "+
		"\3!\3!\3\"\3\"\3#\3#\3$\3$\3%\3%\3&\3&\3\'\3\'\3(\3(\3(\7(\u00ee\n(\f"+
		"(\16(\u00f1\13(\3)\3)\3*\3*\3+\3+\3,\3,\3-\3-\3-\3-\3.\3.\3.\3.\3/\3/"+
		"\3\60\3\60\3\60\7\60\u0108\n\60\f\60\16\60\u010b\13\60\3\61\3\61\3\62"+
		"\3\62\3\63\3\63\3\64\3\64\3\65\3\65\3\65\3\65\2\2\66\5\2\7\2\t\2\13\2"+
		"\r\2\17\2\21\2\23\2\25\2\27\2\31\2\33\2\35\2\37\2!\2#\2%\2\'\2)\2+\2-"+
		"\2/\2\61\2\63\2\65\2\67\39\4;\5=\6?\7A\bC\tE\nG\13I\fK\rM\16O\17Q\20S"+
		"\21U\22W\23Y\24[\25]\26_\27a\30c\31e\32g\33i\34k\35\5\2\3\4\n\17\2C\\"+
		"c|\u00c2\u00d8\u00da\u00f8\u00fa\u0301\u0372\u037f\u0381\u2001\u200e\u200f"+
		"\u2072\u2191\u2c02\u2ff1\u3003\ud801\uf902\ufdd1\ufdf2\uffff\5\2\u00b9"+
		"\u00b9\u0302\u0371\u2041\u2042\n\2$$))^^ddhhppttvv\6\2\f\f\17\17))^^\6"+
		"\2\f\f\17\17$$^^\5\2\62;CHch\3\2\62;\4\2\13\13\"\"\2\u010f\2\67\3\2\2"+
		"\2\29\3\2\2\2\3;\3\2\2\2\3=\3\2\2\2\3?\3\2\2\2\3A\3\2\2\2\3C\3\2\2\2\3"+
		"E\3\2\2\2\3G\3\2\2\2\3I\3\2\2\2\3K\3\2\2\2\3M\3\2\2\2\3O\3\2\2\2\3Q\3"+
		"\2\2\2\3S\3\2\2\2\3U\3\2\2\2\3W\3\2\2\2\3Y\3\2\2\2\3[\3\2\2\2\4]\3\2\2"+
		"\2\4_\3\2\2\2\4a\3\2\2\2\4c\3\2\2\2\4e\3\2\2\2\4g\3\2\2\2\4i\3\2\2\2\4"+
		"k\3\2\2\2\5m\3\2\2\2\7o\3\2\2\2\tq\3\2\2\2\13s\3\2\2\2\ru\3\2\2\2\17w"+
		"\3\2\2\2\21y\3\2\2\2\23{\3\2\2\2\25}\3\2\2\2\27\177\3\2\2\2\31\u0081\3"+
		"\2\2\2\33\u0083\3\2\2\2\35\u0085\3\2\2\2\37\u0087\3\2\2\2!\u0089\3\2\2"+
		"\2#\u008c\3\2\2\2%\u0092\3\2\2\2\'\u0094\3\2\2\2)\u009b\3\2\2\2+\u00a8"+
		"\3\2\2\2-\u00b2\3\2\2\2/\u00bc\3\2\2\2\61\u00be\3\2\2\2\63\u00c1\3\2\2"+
		"\2\65\u00c5\3\2\2\2\67\u00ca\3\2\2\29\u00ce\3\2\2\2;\u00d0\3\2\2\2=\u00d4"+
		"\3\2\2\2?\u00d8\3\2\2\2A\u00da\3\2\2\2C\u00dc\3\2\2\2E\u00de\3\2\2\2G"+
		"\u00e0\3\2\2\2I\u00e2\3\2\2\2K\u00e4\3\2\2\2M\u00e6\3\2\2\2O\u00e8\3\2"+
		"\2\2Q\u00ea\3\2\2\2S\u00f2\3\2\2\2U\u00f4\3\2\2\2W\u00f6\3\2\2\2Y\u00f8"+
		"\3\2\2\2[\u00fa\3\2\2\2]\u00fe\3\2\2\2_\u0102\3\2\2\2a\u0104\3\2\2\2c"+
		"\u010c\3\2\2\2e\u010e\3\2\2\2g\u0110\3\2\2\2i\u0112\3\2\2\2k\u0114\3\2"+
		"\2\2mn\7^\2\2n\6\3\2\2\2op\7)\2\2p\b\3\2\2\2qr\7$\2\2r\n\3\2\2\2st\7a"+
		"\2\2t\f\3\2\2\2uv\7.\2\2v\16\3\2\2\2wx\7=\2\2x\20\3\2\2\2yz\7~\2\2z\22"+
		"\3\2\2\2{|\7\60\2\2|\24\3\2\2\2}~\7*\2\2~\26\3\2\2\2\177\u0080\7+\2\2"+
		"\u0080\30\3\2\2\2\u0081\u0082\7}\2\2\u0082\32\3\2\2\2\u0083\u0084\7\177"+
		"\2\2\u0084\34\3\2\2\2\u0085\u0086\7]\2\2\u0086\36\3\2\2\2\u0087\u0088"+
		"\7_\2\2\u0088 \3\2\2\2\u0089\u008a\7&\2\2\u008a\u008b\7}\2\2\u008b\"\3"+
		"\2\2\2\u008c\u008d\7A\2\2\u008d\u008e\7A\2\2\u008e$\3\2\2\2\u008f\u0093"+
		"\t\2\2\2\u0090\u0093\5\13\5\2\u0091\u0093\t\3\2\2\u0092\u008f\3\2\2\2"+
		"\u0092\u0090\3\2\2\2\u0092\u0091\3\2\2\2\u0093&\3\2\2\2\u0094\u0099\5"+
		"\5\2\2\u0095\u009a\t\4\2\2\u0096\u009a\5)\24\2\u0097\u009a\13\2\2\2\u0098"+
		"\u009a\7\2\2\3\u0099\u0095\3\2\2\2\u0099\u0096\3\2\2\2\u0099\u0097\3\2"+
		"\2\2\u0099\u0098\3\2\2\2\u009a(\3\2\2\2\u009b\u00a6\7w\2\2\u009c\u00a4"+
		"\5/\27\2\u009d\u00a2\5/\27\2\u009e\u00a0\5/\27\2\u009f\u00a1\5/\27\2\u00a0"+
		"\u009f\3\2\2\2\u00a0\u00a1\3\2\2\2\u00a1\u00a3\3\2\2\2\u00a2\u009e\3\2"+
		"\2\2\u00a2\u00a3\3\2\2\2\u00a3\u00a5\3\2\2\2\u00a4\u009d\3\2\2\2\u00a4"+
		"\u00a5\3\2\2\2\u00a5\u00a7\3\2\2\2\u00a6\u009c\3\2\2\2\u00a6\u00a7\3\2"+
		"\2\2\u00a7*\3\2\2\2\u00a8\u00ad\5\7\3\2\u00a9\u00ac\5\'\23\2\u00aa\u00ac"+
		"\n\5\2\2\u00ab\u00a9\3\2\2\2\u00ab\u00aa\3\2\2\2\u00ac\u00af\3\2\2\2\u00ad"+
		"\u00ab\3\2\2\2\u00ad\u00ae\3\2\2\2\u00ae\u00b0\3\2\2\2\u00af\u00ad\3\2"+
		"\2\2\u00b0\u00b1\5\7\3\2\u00b1,\3\2\2\2\u00b2\u00b7\5\t\4\2\u00b3\u00b6"+
		"\5\'\23\2\u00b4\u00b6\n\6\2\2\u00b5\u00b3\3\2\2\2\u00b5\u00b4\3\2\2\2"+
		"\u00b6\u00b9\3\2\2\2\u00b7\u00b5\3\2\2\2\u00b7\u00b8\3\2\2\2\u00b8\u00ba"+
		"\3\2\2\2\u00b9\u00b7\3\2\2\2\u00ba\u00bb\5\t\4\2\u00bb.\3\2\2\2\u00bc"+
		"\u00bd\t\7\2\2\u00bd\60\3\2\2\2\u00be\u00bf\t\b\2\2\u00bf\62\3\2\2\2\u00c0"+
		"\u00c2\5\61\30\2\u00c1\u00c0\3\2\2\2\u00c2\u00c3\3\2\2\2\u00c3\u00c1\3"+
		"\2\2\2\u00c3\u00c4\3\2\2\2\u00c4\64\3\2\2\2\u00c5\u00c6\5\63\31\2\u00c6"+
		"\u00c8\5\23\t\2\u00c7\u00c9\5\63\31\2\u00c8\u00c7\3\2\2\2\u00c8\u00c9"+
		"\3\2\2\2\u00c9\66\3\2\2\2\u00ca\u00cb\5!\20\2\u00cb\u00cc\3\2\2\2\u00cc"+
		"\u00cd\b\33\2\2\u00cd8\3\2\2\2\u00ce\u00cf\13\2\2\2\u00cf:\3\2\2\2\u00d0"+
		"\u00d1\5\31\f\2\u00d1\u00d2\3\2\2\2\u00d2\u00d3\b\35\3\2\u00d3<\3\2\2"+
		"\2\u00d4\u00d5\5\33\r\2\u00d5\u00d6\3\2\2\2\u00d6\u00d7\b\36\4\2\u00d7"+
		">\3\2\2\2\u00d8\u00d9\5\21\b\2\u00d9@\3\2\2\2\u00da\u00db\5\23\t\2\u00db"+
		"B\3\2\2\2\u00dc\u00dd\5\25\n\2\u00ddD\3\2\2\2\u00de\u00df\5\27\13\2\u00df"+
		"F\3\2\2\2\u00e0\u00e1\5\35\16\2\u00e1H\3\2\2\2\u00e2\u00e3\5\37\17\2\u00e3"+
		"J\3\2\2\2\u00e4\u00e5\5#\21\2\u00e5L\3\2\2\2\u00e6\u00e7\5\17\7\2\u00e7"+
		"N\3\2\2\2\u00e8\u00e9\5\r\6\2\u00e9P\3\2\2\2\u00ea\u00ef\5%\22\2\u00eb"+
		"\u00ee\5%\22\2\u00ec\u00ee\5\61\30\2\u00ed\u00eb\3\2\2\2\u00ed\u00ec\3"+
		"\2\2\2\u00ee\u00f1\3\2\2\2\u00ef\u00ed\3\2\2\2\u00ef\u00f0\3\2\2\2\u00f0"+
		"R\3\2\2\2\u00f1\u00ef\3\2\2\2\u00f2\u00f3\5-\26\2\u00f3T\3\2\2\2\u00f4"+
		"\u00f5\5+\25\2\u00f5V\3\2\2\2\u00f6\u00f7\5\63\31\2\u00f7X\3\2\2\2\u00f8"+
		"\u00f9\5\65\32\2\u00f9Z\3\2\2\2\u00fa\u00fb\t\t\2\2\u00fb\u00fc\3\2\2"+
		"\2\u00fc\u00fd\b-\5\2\u00fd\\\3\2\2\2\u00fe\u00ff\5\33\r\2\u00ff\u0100"+
		"\3\2\2\2\u0100\u0101\b.\4\2\u0101^\3\2\2\2\u0102\u0103\5\r\6\2\u0103`"+
		"\3\2\2\2\u0104\u0109\5%\22\2\u0105\u0108\5%\22\2\u0106\u0108\5\61\30\2"+
		"\u0107\u0105\3\2\2\2\u0107\u0106\3\2\2\2\u0108\u010b\3\2\2\2\u0109\u0107"+
		"\3\2\2\2\u0109\u010a\3\2\2\2\u010ab\3\2\2\2\u010b\u0109\3\2\2\2\u010c"+
		"\u010d\5-\26\2\u010dd\3\2\2\2\u010e\u010f\5+\25\2\u010ff\3\2\2\2\u0110"+
		"\u0111\5\63\31\2\u0111h\3\2\2\2\u0112\u0113\5\65\32\2\u0113j\3\2\2\2\u0114"+
		"\u0115\t\t\2\2\u0115\u0116\3\2\2\2\u0116\u0117\b\65\5\2\u0117l\3\2\2\2"+
		"\25\2\3\4\u0092\u0099\u00a0\u00a2\u00a4\u00a6\u00ab\u00ad\u00b5\u00b7"+
		"\u00c3\u00c8\u00ed\u00ef\u0107\u0109\6\7\3\2\7\4\2\6\2\2\b\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}