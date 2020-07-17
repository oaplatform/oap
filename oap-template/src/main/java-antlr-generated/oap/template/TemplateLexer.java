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
		LBRACK=9, RBRACK=10, DQUESTION=11, SEMI=12, COMMA=13, STAR=14, SLASH=15, 
		PERCENT=16, PLUS=17, MINUS=18, ID=19, DSTRING=20, SSTRING=21, DECDIGITS=22, 
		FLOAT=23, ERR_CHAR=24, CERR_CHAR=25;
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
			"LParen", "RParen", "LBrace", "RBrace", "LBrack", "RBrack", "Star", "Slash", 
			"Percent", "Plus", "Minus", "StartExpr", "DQuestion", "NameChar", "EscSeq", 
			"UnicodeEsc", "SQuoteLiteral", "DQuoteLiteral", "HexDigit", "DecDigit", 
			"DecDigits", "Float", "STARTEXPR", "TEXT", "LBRACE", "RBRACE", "PIPE", 
			"DOT", "LPAREN", "RPAREN", "LBRACK", "RBRACK", "DQUESTION", "SEMI", "COMMA", 
			"STAR", "SLASH", "PERCENT", "PLUS", "MINUS", "ID", "DSTRING", "SSTRING", 
			"DECDIGITS", "FLOAT", "ERR_CHAR", "CRBRACE", "CCOMMA", "CID", "CDSTRING", 
			"CSSTRING", "CDECDIGITS", "CFLOAT", "CERR_CHAR"
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
			"RPAREN", "LBRACK", "RBRACK", "DQUESTION", "SEMI", "COMMA", "STAR", "SLASH", 
			"PERCENT", "PLUS", "MINUS", "ID", "DSTRING", "SSTRING", "DECDIGITS", 
			"FLOAT", "ERR_CHAR", "CERR_CHAR"
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
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2\33\u014d\b\1\b\1"+
		"\b\1\4\2\t\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4"+
		"\n\t\n\4\13\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t"+
		"\21\4\22\t\22\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t"+
		"\30\4\31\t\31\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t"+
		"\37\4 \t \4!\t!\4\"\t\"\4#\t#\4$\t$\4%\t%\4&\t&\4\'\t\'\4(\t(\4)\t)\4"+
		"*\t*\4+\t+\4,\t,\4-\t-\4.\t.\4/\t/\4\60\t\60\4\61\t\61\4\62\t\62\4\63"+
		"\t\63\4\64\t\64\4\65\t\65\4\66\t\66\4\67\t\67\48\t8\49\t9\4:\t:\4;\t;"+
		"\4<\t<\4=\t=\4>\t>\4?\t?\3\2\3\2\3\3\3\3\3\4\3\4\3\5\3\5\3\6\3\6\3\7\3"+
		"\7\3\b\3\b\3\t\3\t\3\n\3\n\3\13\3\13\3\f\3\f\3\r\3\r\3\16\3\16\3\17\3"+
		"\17\3\20\3\20\3\21\3\21\3\22\3\22\3\23\3\23\3\24\3\24\3\25\3\25\3\25\3"+
		"\26\3\26\3\26\3\27\3\27\3\27\5\27\u00b1\n\27\3\30\3\30\3\30\3\30\3\30"+
		"\5\30\u00b8\n\30\3\31\3\31\3\31\3\31\3\31\5\31\u00bf\n\31\5\31\u00c1\n"+
		"\31\5\31\u00c3\n\31\5\31\u00c5\n\31\3\32\3\32\3\32\7\32\u00ca\n\32\f\32"+
		"\16\32\u00cd\13\32\3\32\3\32\3\33\3\33\3\33\7\33\u00d4\n\33\f\33\16\33"+
		"\u00d7\13\33\3\33\3\33\3\34\3\34\3\35\3\35\3\36\6\36\u00e0\n\36\r\36\16"+
		"\36\u00e1\3\37\3\37\3\37\5\37\u00e7\n\37\3 \3 \3 \3 \3!\3!\3\"\3\"\3\""+
		"\3\"\3#\3#\3#\3#\3$\3$\3%\3%\3&\3&\3\'\3\'\3(\3(\3)\3)\3*\3*\3+\3+\3,"+
		"\3,\3-\3-\3.\3.\3/\3/\3\60\3\60\3\61\3\61\3\62\3\62\3\62\7\62\u0116\n"+
		"\62\f\62\16\62\u0119\13\62\3\63\3\63\3\64\3\64\3\65\3\65\3\66\3\66\3\67"+
		"\3\67\3\67\3\67\38\38\38\38\38\39\39\39\39\3:\3:\3:\7:\u0133\n:\f:\16"+
		":\u0136\13:\3:\3:\3;\3;\3;\3;\3<\3<\3<\3<\3=\3=\3=\3=\3>\3>\3>\3>\3?\3"+
		"?\3?\3?\2\2@\5\2\7\2\t\2\13\2\r\2\17\2\21\2\23\2\25\2\27\2\31\2\33\2\35"+
		"\2\37\2!\2#\2%\2\'\2)\2+\2-\2/\2\61\2\63\2\65\2\67\29\2;\2=\2?\2A\3C\4"+
		"E\5G\6I\7K\bM\tO\nQ\13S\fU\rW\16Y\17[\20]\21_\22a\23c\24e\25g\26i\27k"+
		"\30m\31o\32q\2s\2u\2w\2y\2{\2}\2\177\33\5\2\3\4\n\17\2C\\c|\u00c2\u00d8"+
		"\u00da\u00f8\u00fa\u0301\u0372\u037f\u0381\u2001\u200e\u200f\u2072\u2191"+
		"\u2c02\u2ff1\u3003\ud801\uf902\ufdd1\ufdf2\uffff\5\2\u00b9\u00b9\u0302"+
		"\u0371\u2041\u2042\n\2$$))^^ddhhppttvv\6\2\f\f\17\17))^^\6\2\f\f\17\17"+
		"$$^^\5\2\62;CHch\3\2\62;\4\2\13\13\"\"\2\u013f\2A\3\2\2\2\2C\3\2\2\2\3"+
		"E\3\2\2\2\3G\3\2\2\2\3I\3\2\2\2\3K\3\2\2\2\3M\3\2\2\2\3O\3\2\2\2\3Q\3"+
		"\2\2\2\3S\3\2\2\2\3U\3\2\2\2\3W\3\2\2\2\3Y\3\2\2\2\3[\3\2\2\2\3]\3\2\2"+
		"\2\3_\3\2\2\2\3a\3\2\2\2\3c\3\2\2\2\3e\3\2\2\2\3g\3\2\2\2\3i\3\2\2\2\3"+
		"k\3\2\2\2\3m\3\2\2\2\3o\3\2\2\2\4q\3\2\2\2\4s\3\2\2\2\4u\3\2\2\2\4w\3"+
		"\2\2\2\4y\3\2\2\2\4{\3\2\2\2\4}\3\2\2\2\4\177\3\2\2\2\5\u0081\3\2\2\2"+
		"\7\u0083\3\2\2\2\t\u0085\3\2\2\2\13\u0087\3\2\2\2\r\u0089\3\2\2\2\17\u008b"+
		"\3\2\2\2\21\u008d\3\2\2\2\23\u008f\3\2\2\2\25\u0091\3\2\2\2\27\u0093\3"+
		"\2\2\2\31\u0095\3\2\2\2\33\u0097\3\2\2\2\35\u0099\3\2\2\2\37\u009b\3\2"+
		"\2\2!\u009d\3\2\2\2#\u009f\3\2\2\2%\u00a1\3\2\2\2\'\u00a3\3\2\2\2)\u00a5"+
		"\3\2\2\2+\u00a7\3\2\2\2-\u00aa\3\2\2\2/\u00b0\3\2\2\2\61\u00b2\3\2\2\2"+
		"\63\u00b9\3\2\2\2\65\u00c6\3\2\2\2\67\u00d0\3\2\2\29\u00da\3\2\2\2;\u00dc"+
		"\3\2\2\2=\u00df\3\2\2\2?\u00e3\3\2\2\2A\u00e8\3\2\2\2C\u00ec\3\2\2\2E"+
		"\u00ee\3\2\2\2G\u00f2\3\2\2\2I\u00f6\3\2\2\2K\u00f8\3\2\2\2M\u00fa\3\2"+
		"\2\2O\u00fc\3\2\2\2Q\u00fe\3\2\2\2S\u0100\3\2\2\2U\u0102\3\2\2\2W\u0104"+
		"\3\2\2\2Y\u0106\3\2\2\2[\u0108\3\2\2\2]\u010a\3\2\2\2_\u010c\3\2\2\2a"+
		"\u010e\3\2\2\2c\u0110\3\2\2\2e\u0112\3\2\2\2g\u011a\3\2\2\2i\u011c\3\2"+
		"\2\2k\u011e\3\2\2\2m\u0120\3\2\2\2o\u0122\3\2\2\2q\u0126\3\2\2\2s\u012b"+
		"\3\2\2\2u\u012f\3\2\2\2w\u0139\3\2\2\2y\u013d\3\2\2\2{\u0141\3\2\2\2}"+
		"\u0145\3\2\2\2\177\u0149\3\2\2\2\u0081\u0082\7^\2\2\u0082\6\3\2\2\2\u0083"+
		"\u0084\7)\2\2\u0084\b\3\2\2\2\u0085\u0086\7$\2\2\u0086\n\3\2\2\2\u0087"+
		"\u0088\7a\2\2\u0088\f\3\2\2\2\u0089\u008a\7.\2\2\u008a\16\3\2\2\2\u008b"+
		"\u008c\7=\2\2\u008c\20\3\2\2\2\u008d\u008e\7~\2\2\u008e\22\3\2\2\2\u008f"+
		"\u0090\7\60\2\2\u0090\24\3\2\2\2\u0091\u0092\7*\2\2\u0092\26\3\2\2\2\u0093"+
		"\u0094\7+\2\2\u0094\30\3\2\2\2\u0095\u0096\7}\2\2\u0096\32\3\2\2\2\u0097"+
		"\u0098\7\177\2\2\u0098\34\3\2\2\2\u0099\u009a\7]\2\2\u009a\36\3\2\2\2"+
		"\u009b\u009c\7_\2\2\u009c \3\2\2\2\u009d\u009e\7,\2\2\u009e\"\3\2\2\2"+
		"\u009f\u00a0\7\61\2\2\u00a0$\3\2\2\2\u00a1\u00a2\7\'\2\2\u00a2&\3\2\2"+
		"\2\u00a3\u00a4\7-\2\2\u00a4(\3\2\2\2\u00a5\u00a6\7/\2\2\u00a6*\3\2\2\2"+
		"\u00a7\u00a8\7&\2\2\u00a8\u00a9\7}\2\2\u00a9,\3\2\2\2\u00aa\u00ab\7A\2"+
		"\2\u00ab\u00ac\7A\2\2\u00ac.\3\2\2\2\u00ad\u00b1\t\2\2\2\u00ae\u00b1\5"+
		"\13\5\2\u00af\u00b1\t\3\2\2\u00b0\u00ad\3\2\2\2\u00b0\u00ae\3\2\2\2\u00b0"+
		"\u00af\3\2\2\2\u00b1\60\3\2\2\2\u00b2\u00b7\5\5\2\2\u00b3\u00b8\t\4\2"+
		"\2\u00b4\u00b8\5\63\31\2\u00b5\u00b8\13\2\2\2\u00b6\u00b8\7\2\2\3\u00b7"+
		"\u00b3\3\2\2\2\u00b7\u00b4\3\2\2\2\u00b7\u00b5\3\2\2\2\u00b7\u00b6\3\2"+
		"\2\2\u00b8\62\3\2\2\2\u00b9\u00c4\7w\2\2\u00ba\u00c2\59\34\2\u00bb\u00c0"+
		"\59\34\2\u00bc\u00be\59\34\2\u00bd\u00bf\59\34\2\u00be\u00bd\3\2\2\2\u00be"+
		"\u00bf\3\2\2\2\u00bf\u00c1\3\2\2\2\u00c0\u00bc\3\2\2\2\u00c0\u00c1\3\2"+
		"\2\2\u00c1\u00c3\3\2\2\2\u00c2\u00bb\3\2\2\2\u00c2\u00c3\3\2\2\2\u00c3"+
		"\u00c5\3\2\2\2\u00c4\u00ba\3\2\2\2\u00c4\u00c5\3\2\2\2\u00c5\64\3\2\2"+
		"\2\u00c6\u00cb\5\7\3\2\u00c7\u00ca\5\61\30\2\u00c8\u00ca\n\5\2\2\u00c9"+
		"\u00c7\3\2\2\2\u00c9\u00c8\3\2\2\2\u00ca\u00cd\3\2\2\2\u00cb\u00c9\3\2"+
		"\2\2\u00cb\u00cc\3\2\2\2\u00cc\u00ce\3\2\2\2\u00cd\u00cb\3\2\2\2\u00ce"+
		"\u00cf\5\7\3\2\u00cf\66\3\2\2\2\u00d0\u00d5\5\t\4\2\u00d1\u00d4\5\61\30"+
		"\2\u00d2\u00d4\n\6\2\2\u00d3\u00d1\3\2\2\2\u00d3\u00d2\3\2\2\2\u00d4\u00d7"+
		"\3\2\2\2\u00d5\u00d3\3\2\2\2\u00d5\u00d6\3\2\2\2\u00d6\u00d8\3\2\2\2\u00d7"+
		"\u00d5\3\2\2\2\u00d8\u00d9\5\t\4\2\u00d98\3\2\2\2\u00da\u00db\t\7\2\2"+
		"\u00db:\3\2\2\2\u00dc\u00dd\t\b\2\2\u00dd<\3\2\2\2\u00de\u00e0\5;\35\2"+
		"\u00df\u00de\3\2\2\2\u00e0\u00e1\3\2\2\2\u00e1\u00df\3\2\2\2\u00e1\u00e2"+
		"\3\2\2\2\u00e2>\3\2\2\2\u00e3\u00e4\5=\36\2\u00e4\u00e6\5\23\t\2\u00e5"+
		"\u00e7\5=\36\2\u00e6\u00e5\3\2\2\2\u00e6\u00e7\3\2\2\2\u00e7@\3\2\2\2"+
		"\u00e8\u00e9\5+\25\2\u00e9\u00ea\3\2\2\2\u00ea\u00eb\b \2\2\u00ebB\3\2"+
		"\2\2\u00ec\u00ed\13\2\2\2\u00edD\3\2\2\2\u00ee\u00ef\5\31\f\2\u00ef\u00f0"+
		"\3\2\2\2\u00f0\u00f1\b\"\3\2\u00f1F\3\2\2\2\u00f2\u00f3\5\33\r\2\u00f3"+
		"\u00f4\3\2\2\2\u00f4\u00f5\b#\4\2\u00f5H\3\2\2\2\u00f6\u00f7\5\21\b\2"+
		"\u00f7J\3\2\2\2\u00f8\u00f9\5\23\t\2\u00f9L\3\2\2\2\u00fa\u00fb\5\25\n"+
		"\2\u00fbN\3\2\2\2\u00fc\u00fd\5\27\13\2\u00fdP\3\2\2\2\u00fe\u00ff\5\35"+
		"\16\2\u00ffR\3\2\2\2\u0100\u0101\5\37\17\2\u0101T\3\2\2\2\u0102\u0103"+
		"\5-\26\2\u0103V\3\2\2\2\u0104\u0105\5\17\7\2\u0105X\3\2\2\2\u0106\u0107"+
		"\5\r\6\2\u0107Z\3\2\2\2\u0108\u0109\5!\20\2\u0109\\\3\2\2\2\u010a\u010b"+
		"\5#\21\2\u010b^\3\2\2\2\u010c\u010d\5%\22\2\u010d`\3\2\2\2\u010e\u010f"+
		"\5\'\23\2\u010fb\3\2\2\2\u0110\u0111\5)\24\2\u0111d\3\2\2\2\u0112\u0117"+
		"\5/\27\2\u0113\u0116\5/\27\2\u0114\u0116\5;\35\2\u0115\u0113\3\2\2\2\u0115"+
		"\u0114\3\2\2\2\u0116\u0119\3\2\2\2\u0117\u0115\3\2\2\2\u0117\u0118\3\2"+
		"\2\2\u0118f\3\2\2\2\u0119\u0117\3\2\2\2\u011a\u011b\5\67\33\2\u011bh\3"+
		"\2\2\2\u011c\u011d\5\65\32\2\u011dj\3\2\2\2\u011e\u011f\5=\36\2\u011f"+
		"l\3\2\2\2\u0120\u0121\5?\37\2\u0121n\3\2\2\2\u0122\u0123\t\t\2\2\u0123"+
		"\u0124\3\2\2\2\u0124\u0125\b\67\5\2\u0125p\3\2\2\2\u0126\u0127\5\33\r"+
		"\2\u0127\u0128\3\2\2\2\u0128\u0129\b8\4\2\u0129\u012a\b8\6\2\u012ar\3"+
		"\2\2\2\u012b\u012c\5\r\6\2\u012c\u012d\3\2\2\2\u012d\u012e\b9\7\2\u012e"+
		"t\3\2\2\2\u012f\u0134\5/\27\2\u0130\u0133\5/\27\2\u0131\u0133\5;\35\2"+
		"\u0132\u0130\3\2\2\2\u0132\u0131\3\2\2\2\u0133\u0136\3\2\2\2\u0134\u0132"+
		"\3\2\2\2\u0134\u0135\3\2\2\2\u0135\u0137\3\2\2\2\u0136\u0134\3\2\2\2\u0137"+
		"\u0138\b:\b\2\u0138v\3\2\2\2\u0139\u013a\5\67\33\2\u013a\u013b\3\2\2\2"+
		"\u013b\u013c\b;\t\2\u013cx\3\2\2\2\u013d\u013e\5\65\32\2\u013e\u013f\3"+
		"\2\2\2\u013f\u0140\b<\n\2\u0140z\3\2\2\2\u0141\u0142\5=\36\2\u0142\u0143"+
		"\3\2\2\2\u0143\u0144\b=\13\2\u0144|\3\2\2\2\u0145\u0146\5?\37\2\u0146"+
		"\u0147\3\2\2\2\u0147\u0148\b>\f\2\u0148~\3\2\2\2\u0149\u014a\t\t\2\2\u014a"+
		"\u014b\3\2\2\2\u014b\u014c\b?\5\2\u014c\u0080\3\2\2\2\25\2\3\4\u00b0\u00b7"+
		"\u00be\u00c0\u00c2\u00c4\u00c9\u00cb\u00d3\u00d5\u00e1\u00e6\u0115\u0117"+
		"\u0132\u0134\r\7\3\2\7\4\2\6\2\2\b\2\2\t\6\2\t\17\2\t\25\2\t\26\2\t\27"+
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