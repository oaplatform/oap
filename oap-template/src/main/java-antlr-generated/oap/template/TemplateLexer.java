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
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2\b\67\b\1\b\1\4\2"+
		"\t\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4"+
		"\13\t\13\3\2\3\2\3\2\3\2\3\3\3\3\3\3\3\4\3\4\3\4\3\4\3\5\3\5\3\5\3\5\3"+
		"\6\3\6\3\7\3\7\3\b\3\b\3\t\3\t\3\t\3\t\3\n\3\n\3\n\3\n\3\13\3\13\2\2\f"+
		"\4\2\6\2\b\3\n\4\f\5\16\2\20\2\22\6\24\7\26\b\4\2\3\2\2\61\2\b\3\2\2\2"+
		"\2\n\3\2\2\2\2\f\3\2\2\2\3\22\3\2\2\2\3\24\3\2\2\2\3\26\3\2\2\2\4\30\3"+
		"\2\2\2\6\34\3\2\2\2\b\37\3\2\2\2\n#\3\2\2\2\f\'\3\2\2\2\16)\3\2\2\2\20"+
		"+\3\2\2\2\22-\3\2\2\2\24\61\3\2\2\2\26\65\3\2\2\2\30\31\7&\2\2\31\32\7"+
		"&\2\2\32\33\7}\2\2\33\5\3\2\2\2\34\35\7&\2\2\35\36\7}\2\2\36\7\3\2\2\2"+
		"\37 \5\4\2\2 !\3\2\2\2!\"\b\4\2\2\"\t\3\2\2\2#$\5\6\3\2$%\3\2\2\2%&\b"+
		"\5\2\2&\13\3\2\2\2\'(\13\2\2\2(\r\3\2\2\2)*\7}\2\2*\17\3\2\2\2+,\7\177"+
		"\2\2,\21\3\2\2\2-.\5\16\7\2./\3\2\2\2/\60\b\t\2\2\60\23\3\2\2\2\61\62"+
		"\5\20\b\2\62\63\3\2\2\2\63\64\b\n\3\2\64\25\3\2\2\2\65\66\13\2\2\2\66"+
		"\27\3\2\2\2\4\2\3\4\7\3\2\6\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}