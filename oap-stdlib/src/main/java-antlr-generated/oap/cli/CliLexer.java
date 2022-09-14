// Generated from java-escape by ANTLR 4.11.1

package oap.cli;
import java.util.List;
import java.util.ArrayList;
import oap.util.Pair;
import static oap.util.Pair.__;

import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast", "CheckReturnValue"})
public class CliLexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.11.1", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		NAME=1, VALUE=2, STRVALUE=3, WS=4;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	private static String[] makeRuleNames() {
		return new String[] {
			"NAME", "VALUE", "STRVALUE", "ESC", "WS"
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
			null, "NAME", "VALUE", "STRVALUE", "WS"
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


	public CliLexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "Cli.g4"; }

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

	@Override
	public void action(RuleContext _localctx, int ruleIndex, int actionIndex) {
		switch (ruleIndex) {
		case 0:
			NAME_action((RuleContext)_localctx, actionIndex);
			break;
		case 1:
			VALUE_action((RuleContext)_localctx, actionIndex);
			break;
		case 2:
			STRVALUE_action((RuleContext)_localctx, actionIndex);
			break;
		}
	}
	private void NAME_action(RuleContext _localctx, int actionIndex) {
		switch (actionIndex) {
		case 0:
			setText(getText().substring(2));
			break;
		}
	}
	private void VALUE_action(RuleContext _localctx, int actionIndex) {
		switch (actionIndex) {
		case 1:
			setText(getText().substring(1));
			break;
		}
	}
	private void STRVALUE_action(RuleContext _localctx, int actionIndex) {
		switch (actionIndex) {
		case 2:

			        String s = getText();
			        s = s.substring(2, s.length() - 1);
			        s = s.replace("\\\"", "\"");
			        s = s.replace("\\\\", "\\");
			        setText(s);
			    
			break;
		}
	}

	public static final String _serializedATN =
		"\u0004\u0000\u00048\u0006\uffff\uffff\u0002\u0000\u0007\u0000\u0002\u0001"+
		"\u0007\u0001\u0002\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004"+
		"\u0007\u0004\u0001\u0000\u0001\u0000\u0001\u0000\u0001\u0000\u0004\u0000"+
		"\u0010\b\u0000\u000b\u0000\f\u0000\u0011\u0001\u0000\u0001\u0000\u0001"+
		"\u0001\u0001\u0001\u0004\u0001\u0018\b\u0001\u000b\u0001\f\u0001\u0019"+
		"\u0001\u0001\u0001\u0001\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002"+
		"\u0005\u0002\"\b\u0002\n\u0002\f\u0002%\t\u0002\u0001\u0002\u0001\u0002"+
		"\u0001\u0002\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0004\u0001\u0004"+
		"\u0001\u0004\u0001\u0004\u0003\u00041\b\u0004\u0004\u00043\b\u0004\u000b"+
		"\u0004\f\u00044\u0001\u0004\u0001\u0004\u0000\u0000\u0005\u0001\u0001"+
		"\u0003\u0002\u0005\u0003\u0007\u0000\t\u0004\u0001\u0000\u0006\u0005\u0000"+
		"--0:AZ__az\u0004\u0000\t\n\r\r  \"\"\u0004\u0000\n\n\r\r\"\"\\\\\u0002"+
		"\u0000\"\"\\\\\u0002\u0000\t\t  \u0002\u0000\n\n\r\r=\u0000\u0001\u0001"+
		"\u0000\u0000\u0000\u0000\u0003\u0001\u0000\u0000\u0000\u0000\u0005\u0001"+
		"\u0000\u0000\u0000\u0000\t\u0001\u0000\u0000\u0000\u0001\u000b\u0001\u0000"+
		"\u0000\u0000\u0003\u0015\u0001\u0000\u0000\u0000\u0005\u001d\u0001\u0000"+
		"\u0000\u0000\u0007)\u0001\u0000\u0000\u0000\t2\u0001\u0000\u0000\u0000"+
		"\u000b\f\u0005-\u0000\u0000\f\r\u0005-\u0000\u0000\r\u000f\u0001\u0000"+
		"\u0000\u0000\u000e\u0010\u0007\u0000\u0000\u0000\u000f\u000e\u0001\u0000"+
		"\u0000\u0000\u0010\u0011\u0001\u0000\u0000\u0000\u0011\u000f\u0001\u0000"+
		"\u0000\u0000\u0011\u0012\u0001\u0000\u0000\u0000\u0012\u0013\u0001\u0000"+
		"\u0000\u0000\u0013\u0014\u0006\u0000\u0000\u0000\u0014\u0002\u0001\u0000"+
		"\u0000\u0000\u0015\u0017\u0005=\u0000\u0000\u0016\u0018\b\u0001\u0000"+
		"\u0000\u0017\u0016\u0001\u0000\u0000\u0000\u0018\u0019\u0001\u0000\u0000"+
		"\u0000\u0019\u0017\u0001\u0000\u0000\u0000\u0019\u001a\u0001\u0000\u0000"+
		"\u0000\u001a\u001b\u0001\u0000\u0000\u0000\u001b\u001c\u0006\u0001\u0001"+
		"\u0000\u001c\u0004\u0001\u0000\u0000\u0000\u001d\u001e\u0005=\u0000\u0000"+
		"\u001e#\u0005\"\u0000\u0000\u001f\"\u0003\u0007\u0003\u0000 \"\b\u0002"+
		"\u0000\u0000!\u001f\u0001\u0000\u0000\u0000! \u0001\u0000\u0000\u0000"+
		"\"%\u0001\u0000\u0000\u0000#!\u0001\u0000\u0000\u0000#$\u0001\u0000\u0000"+
		"\u0000$&\u0001\u0000\u0000\u0000%#\u0001\u0000\u0000\u0000&\'\u0005\""+
		"\u0000\u0000\'(\u0006\u0002\u0002\u0000(\u0006\u0001\u0000\u0000\u0000"+
		")*\u0005\\\u0000\u0000*+\u0007\u0003\u0000\u0000+\b\u0001\u0000\u0000"+
		"\u0000,3\u0007\u0004\u0000\u0000-.\u0005\r\u0000\u0000.1\u0005\n\u0000"+
		"\u0000/1\u0007\u0005\u0000\u00000-\u0001\u0000\u0000\u00000/\u0001\u0000"+
		"\u0000\u000013\u0001\u0000\u0000\u00002,\u0001\u0000\u0000\u000020\u0001"+
		"\u0000\u0000\u000034\u0001\u0000\u0000\u000042\u0001\u0000\u0000\u0000"+
		"45\u0001\u0000\u0000\u000056\u0001\u0000\u0000\u000067\u0006\u0004\u0003"+
		"\u00007\n\u0001\u0000\u0000\u0000\b\u0000\u0011\u0019!#024\u0004\u0001"+
		"\u0000\u0000\u0001\u0001\u0001\u0001\u0002\u0002\u0006\u0000\u0000";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}