/*
 * The MIT License (MIT)
 *
 * Copyright (c) Open Application Platform Authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

// Generated from Cli.g4 by ANTLR 4.5

package oap.cli;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.RuntimeMetaData;
import org.antlr.v4.runtime.Vocabulary;
import org.antlr.v4.runtime.VocabularyImpl;
import org.antlr.v4.runtime.atn.ATN;
import org.antlr.v4.runtime.atn.ATNDeserializer;
import org.antlr.v4.runtime.atn.LexerATNSimulator;
import org.antlr.v4.runtime.atn.PredictionContextCache;
import org.antlr.v4.runtime.dfa.DFA;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class CliLexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.5", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		NAME=1, VALUE=2, STRVALUE=3, WS=4;
	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	public static final String[] ruleNames = {
		"NAME", "VALUE", "STRVALUE", "ESC", "WS"
	};

	private static final String[] _LITERAL_NAMES = {
	};
	private static final String[] _SYMBOLIC_NAMES = {
		null, "NAME", "VALUE", "STRVALUE", "WS"
	};
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
		"\3\u0430\ud6d1\u8206\uad2d\u4417\uaef1\u8d80\uaadd\2\6:\b\1\4\2\t\2\4"+
		"\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\3\2\3\2\3\2\3\2\6\2\22\n\2\r\2\16\2\23"+
		"\3\2\3\2\3\3\3\3\6\3\32\n\3\r\3\16\3\33\3\3\3\3\3\4\3\4\3\4\3\4\7\4$\n"+
		"\4\f\4\16\4\'\13\4\3\4\3\4\3\4\3\5\3\5\3\5\3\6\3\6\3\6\3\6\5\6\63\n\6"+
		"\6\6\65\n\6\r\6\16\6\66\3\6\3\6\2\2\7\3\3\5\4\7\5\t\2\13\6\3\2\b\7\2/"+
		"/\62<C\\aac|\6\2\13\f\17\17\"\"$$\6\2\f\f\17\17$$^^\4\2$$^^\4\2\13\13"+
		"\"\"\4\2\f\f\17\17?\2\3\3\2\2\2\2\5\3\2\2\2\2\7\3\2\2\2\2\13\3\2\2\2\3"+
		"\r\3\2\2\2\5\27\3\2\2\2\7\37\3\2\2\2\t+\3\2\2\2\13\64\3\2\2\2\r\16\7/"+
		"\2\2\16\17\7/\2\2\17\21\3\2\2\2\20\22\t\2\2\2\21\20\3\2\2\2\22\23\3\2"+
		"\2\2\23\21\3\2\2\2\23\24\3\2\2\2\24\25\3\2\2\2\25\26\b\2\2\2\26\4\3\2"+
		"\2\2\27\31\7?\2\2\30\32\n\3\2\2\31\30\3\2\2\2\32\33\3\2\2\2\33\31\3\2"+
		"\2\2\33\34\3\2\2\2\34\35\3\2\2\2\35\36\b\3\3\2\36\6\3\2\2\2\37 \7?\2\2"+
		" %\7$\2\2!$\5\t\5\2\"$\n\4\2\2#!\3\2\2\2#\"\3\2\2\2$\'\3\2\2\2%#\3\2\2"+
		"\2%&\3\2\2\2&(\3\2\2\2\'%\3\2\2\2()\7$\2\2)*\b\4\4\2*\b\3\2\2\2+,\7^\2"+
		"\2,-\t\5\2\2-\n\3\2\2\2.\65\t\6\2\2/\60\7\17\2\2\60\63\7\f\2\2\61\63\t"+
		"\7\2\2\62/\3\2\2\2\62\61\3\2\2\2\63\65\3\2\2\2\64.\3\2\2\2\64\62\3\2\2"+
		"\2\65\66\3\2\2\2\66\64\3\2\2\2\66\67\3\2\2\2\678\3\2\2\289\b\6\5\29\f"+
		"\3\2\2\2\n\2\23\33#%\62\64\66\6\3\2\2\3\3\3\3\4\4\b\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}
