// Generated from TemplateLexer.g4 by ANTLR 4.13.0

package oap.template;

import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast", "CheckReturnValue"})
public class TemplateLexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.13.0", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		STARTBLOCKRANGE=1, STARTBLOCKWITH=2, STARTBLOCKIF_LTRIM=3, STARTBLOCKIF=4, 
		STARTBLOCKELSE=5, STARTBLOCKEND=6, STARTESCEXPR=7, STARTEXPR=8, STARTEXPR2_LTRIM=9, 
		STARTEXPR2=10, TEXT=11, LBRACE=12, RBRACE=13, EXPRESSION=14, LBRACE2=15, 
		RBRACE2_RTRIM=16, RBRACE2=17, EXPRESSION2=18, BLOCK_IF_CONTENT=19, BLOCK_IF_RBRACE=20;
	public static final int
		Expression=1, Expression2=2, BlockIfContent=3;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE", "Expression", "Expression2", "BlockIfContent"
	};

	private static String[] makeRuleNames() {
		return new String[] {
			"StartEscExpr", "StartExpr", "StartExpr2", "EndExpr2", "STARTBLOCKRANGE", 
			"STARTBLOCKWITH", "STARTBLOCKIF_LTRIM", "STARTBLOCKIF", "STARTBLOCKELSE", 
			"STARTBLOCKEND", "STARTESCEXPR", "STARTEXPR", "STARTEXPR2_LTRIM", "STARTEXPR2", 
			"STARTCUSTOMEXPR2", "TEXT", "LBrace", "RBrace", "LBRACE", "RBRACE", "EXPRESSION", 
			"LBRACE2", "RBRACE2_RTRIM", "RBRACE2CUSTOM", "RBRACE2", "EXPRESSION2", 
			"BLOCK_IF_CONTENT", "BLOCK_IF_RBRACE"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, null, null, null, null, null, null, null, null, "'{{-'", null, 
			null, null, null, null, null, "'-}}'", null, null, null, "'}}'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "STARTBLOCKRANGE", "STARTBLOCKWITH", "STARTBLOCKIF_LTRIM", "STARTBLOCKIF", 
			"STARTBLOCKELSE", "STARTBLOCKEND", "STARTESCEXPR", "STARTEXPR", "STARTEXPR2_LTRIM", 
			"STARTEXPR2", "TEXT", "LBRACE", "RBRACE", "EXPRESSION", "LBRACE2", "RBRACE2_RTRIM", 
			"RBRACE2", "EXPRESSION2", "BLOCK_IF_CONTENT", "BLOCK_IF_RBRACE"
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


	    TemplateConfiguration configuration = TemplateConfiguration.DEFAULT;
	    private TemplateConfiguration.Expression _matchedCustomExpr;
	    private final java.util.Deque<String> _customSuffixStack = new java.util.ArrayDeque<>();

	    private boolean isAtCustomPrefix() {
	        for( TemplateConfiguration.Expression expr : configuration.expressions ) {
	            if( "{{".equals( expr.prefix ) || "${".equals( expr.prefix ) ) continue;
	            if( matchesAhead( expr.prefix ) ) {
	                _matchedCustomExpr = expr;
	                return true;
	            }
	        }
	        return false;
	    }

	    private boolean matchesAhead( String text ) {
	        for( int i = 0; i < text.length(); i++ ) {
	            if( _input.LA( i + 1 ) != text.charAt( i ) ) return false;
	        }
	        return true;
	    }

	    private void consumeCustomPrefix() {
	        for( int i = 1; i < _matchedCustomExpr.prefix.length(); i++ ) _input.consume();
	        _customSuffixStack.push( _matchedCustomExpr.suffix );
	    }

	    private boolean isAtCustomSuffix() {
	        String suffix = _customSuffixStack.peek();
	        if( suffix == null ) return false;
	        return matchesAhead( suffix );
	    }

	    private void consumeCustomSuffix() {
	        String suffix = _customSuffixStack.pop();
	        for( int i = 1; i < suffix.length(); i++ ) _input.consume();
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

	@Override
	public void action(RuleContext _localctx, int ruleIndex, int actionIndex) {
		switch (ruleIndex) {
		case 14:
			STARTCUSTOMEXPR2_action((RuleContext)_localctx, actionIndex);
			break;
		case 23:
			RBRACE2CUSTOM_action((RuleContext)_localctx, actionIndex);
			break;
		}
	}
	private void STARTCUSTOMEXPR2_action(RuleContext _localctx, int actionIndex) {
		switch (actionIndex) {
		case 0:
			 consumeCustomPrefix(); 
			break;
		}
	}
	private void RBRACE2CUSTOM_action(RuleContext _localctx, int actionIndex) {
		switch (actionIndex) {
		case 1:
			 consumeCustomSuffix(); 
			break;
		}
	}
	@Override
	public boolean sempred(RuleContext _localctx, int ruleIndex, int predIndex) {
		switch (ruleIndex) {
		case 14:
			return STARTCUSTOMEXPR2_sempred((RuleContext)_localctx, predIndex);
		case 23:
			return RBRACE2CUSTOM_sempred((RuleContext)_localctx, predIndex);
		}
		return true;
	}
	private boolean STARTCUSTOMEXPR2_sempred(RuleContext _localctx, int predIndex) {
		switch (predIndex) {
		case 0:
			return  isAtCustomPrefix() ;
		}
		return true;
	}
	private boolean RBRACE2CUSTOM_sempred(RuleContext _localctx, int predIndex) {
		switch (predIndex) {
		case 1:
			return  isAtCustomSuffix() ;
		}
		return true;
	}

	public static final String _serializedATN =
		"\u0004\u0000\u0014\u0118\u0006\uffff\uffff\u0006\uffff\uffff\u0006\uffff"+
		"\uffff\u0006\uffff\uffff\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001"+
		"\u0002\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004\u0007\u0004"+
		"\u0002\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002\u0007\u0007\u0007"+
		"\u0002\b\u0007\b\u0002\t\u0007\t\u0002\n\u0007\n\u0002\u000b\u0007\u000b"+
		"\u0002\f\u0007\f\u0002\r\u0007\r\u0002\u000e\u0007\u000e\u0002\u000f\u0007"+
		"\u000f\u0002\u0010\u0007\u0010\u0002\u0011\u0007\u0011\u0002\u0012\u0007"+
		"\u0012\u0002\u0013\u0007\u0013\u0002\u0014\u0007\u0014\u0002\u0015\u0007"+
		"\u0015\u0002\u0016\u0007\u0016\u0002\u0017\u0007\u0017\u0002\u0018\u0007"+
		"\u0018\u0002\u0019\u0007\u0019\u0002\u001a\u0007\u001a\u0002\u001b\u0007"+
		"\u001b\u0001\u0000\u0001\u0000\u0001\u0000\u0001\u0000\u0001\u0001\u0001"+
		"\u0001\u0001\u0001\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0003\u0001"+
		"\u0003\u0001\u0003\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001"+
		"\u0004\u0005\u0004O\b\u0004\n\u0004\f\u0004R\t\u0004\u0001\u0004\u0001"+
		"\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0004"+
		"\u0004[\b\u0004\u000b\u0004\f\u0004\\\u0001\u0004\u0001\u0004\u0001\u0005"+
		"\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0005\u0005f\b\u0005"+
		"\n\u0005\f\u0005i\t\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005"+
		"\u0001\u0005\u0001\u0005\u0004\u0005q\b\u0005\u000b\u0005\f\u0005r\u0001"+
		"\u0005\u0001\u0005\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0001"+
		"\u0006\u0001\u0006\u0005\u0006}\b\u0006\n\u0006\f\u0006\u0080\t\u0006"+
		"\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0004\u0006\u0086\b\u0006"+
		"\u000b\u0006\f\u0006\u0087\u0001\u0006\u0001\u0006\u0001\u0007\u0001\u0007"+
		"\u0001\u0007\u0001\u0007\u0001\u0007\u0005\u0007\u0091\b\u0007\n\u0007"+
		"\f\u0007\u0094\t\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007"+
		"\u0004\u0007\u009a\b\u0007\u000b\u0007\f\u0007\u009b\u0001\u0007\u0001"+
		"\u0007\u0001\b\u0001\b\u0001\b\u0001\b\u0001\b\u0005\b\u00a5\b\b\n\b\f"+
		"\b\u00a8\t\b\u0001\b\u0001\b\u0001\b\u0001\b\u0001\b\u0001\b\u0005\b\u00b0"+
		"\b\b\n\b\f\b\u00b3\t\b\u0001\b\u0001\b\u0001\b\u0001\t\u0001\t\u0001\t"+
		"\u0001\t\u0001\t\u0005\t\u00bd\b\t\n\t\f\t\u00c0\t\t\u0001\t\u0001\t\u0001"+
		"\t\u0001\t\u0001\t\u0005\t\u00c7\b\t\n\t\f\t\u00ca\t\t\u0001\t\u0001\t"+
		"\u0001\t\u0001\n\u0001\n\u0001\n\u0001\n\u0001\u000b\u0001\u000b\u0001"+
		"\u000b\u0001\u000b\u0001\f\u0001\f\u0001\f\u0001\f\u0001\f\u0001\f\u0001"+
		"\r\u0001\r\u0001\r\u0001\r\u0001\u000e\u0001\u000e\u0001\u000e\u0001\u000e"+
		"\u0001\u000e\u0001\u000e\u0001\u000e\u0001\u000f\u0001\u000f\u0001\u0010"+
		"\u0001\u0010\u0001\u0011\u0001\u0011\u0001\u0012\u0001\u0012\u0001\u0012"+
		"\u0001\u0012\u0001\u0013\u0001\u0013\u0001\u0013\u0001\u0013\u0001\u0014"+
		"\u0001\u0014\u0001\u0015\u0001\u0015\u0001\u0015\u0001\u0015\u0001\u0016"+
		"\u0001\u0016\u0001\u0016\u0001\u0016\u0001\u0016\u0001\u0016\u0001\u0017"+
		"\u0001\u0017\u0001\u0017\u0001\u0017\u0001\u0017\u0001\u0017\u0001\u0017"+
		"\u0001\u0018\u0001\u0018\u0001\u0018\u0001\u0018\u0001\u0019\u0001\u0019"+
		"\u0001\u001a\u0004\u001a\u0110\b\u001a\u000b\u001a\f\u001a\u0111\u0001"+
		"\u001b\u0001\u001b\u0001\u001b\u0001\u001b\u0001\u001b\u0000\u0000\u001c"+
		"\u0004\u0000\u0006\u0000\b\u0000\n\u0000\f\u0001\u000e\u0002\u0010\u0003"+
		"\u0012\u0004\u0014\u0005\u0016\u0006\u0018\u0007\u001a\b\u001c\t\u001e"+
		"\n \u0000\"\u000b$\u0000&\u0000(\f*\r,\u000e.\u000f0\u00102\u00004\u0011"+
		"6\u00128\u0013:\u0014\u0004\u0000\u0001\u0002\u0003\u0002\u0002\u0000"+
		"\t\t  \u0001\u0000}}\u011b\u0000\f\u0001\u0000\u0000\u0000\u0000\u000e"+
		"\u0001\u0000\u0000\u0000\u0000\u0010\u0001\u0000\u0000\u0000\u0000\u0012"+
		"\u0001\u0000\u0000\u0000\u0000\u0014\u0001\u0000\u0000\u0000\u0000\u0016"+
		"\u0001\u0000\u0000\u0000\u0000\u0018\u0001\u0000\u0000\u0000\u0000\u001a"+
		"\u0001\u0000\u0000\u0000\u0000\u001c\u0001\u0000\u0000\u0000\u0000\u001e"+
		"\u0001\u0000\u0000\u0000\u0000 \u0001\u0000\u0000\u0000\u0000\"\u0001"+
		"\u0000\u0000\u0000\u0001(\u0001\u0000\u0000\u0000\u0001*\u0001\u0000\u0000"+
		"\u0000\u0001,\u0001\u0000\u0000\u0000\u0002.\u0001\u0000\u0000\u0000\u0002"+
		"0\u0001\u0000\u0000\u0000\u00022\u0001\u0000\u0000\u0000\u00024\u0001"+
		"\u0000\u0000\u0000\u00026\u0001\u0000\u0000\u0000\u00038\u0001\u0000\u0000"+
		"\u0000\u0003:\u0001\u0000\u0000\u0000\u0004<\u0001\u0000\u0000\u0000\u0006"+
		"@\u0001\u0000\u0000\u0000\bC\u0001\u0000\u0000\u0000\nF\u0001\u0000\u0000"+
		"\u0000\fI\u0001\u0000\u0000\u0000\u000e`\u0001\u0000\u0000\u0000\u0010"+
		"v\u0001\u0000\u0000\u0000\u0012\u008b\u0001\u0000\u0000\u0000\u0014\u009f"+
		"\u0001\u0000\u0000\u0000\u0016\u00b7\u0001\u0000\u0000\u0000\u0018\u00ce"+
		"\u0001\u0000\u0000\u0000\u001a\u00d2\u0001\u0000\u0000\u0000\u001c\u00d6"+
		"\u0001\u0000\u0000\u0000\u001e\u00dc\u0001\u0000\u0000\u0000 \u00e0\u0001"+
		"\u0000\u0000\u0000\"\u00e7\u0001\u0000\u0000\u0000$\u00e9\u0001\u0000"+
		"\u0000\u0000&\u00eb\u0001\u0000\u0000\u0000(\u00ed\u0001\u0000\u0000\u0000"+
		"*\u00f1\u0001\u0000\u0000\u0000,\u00f5\u0001\u0000\u0000\u0000.\u00f7"+
		"\u0001\u0000\u0000\u00000\u00fb\u0001\u0000\u0000\u00002\u0101\u0001\u0000"+
		"\u0000\u00004\u0108\u0001\u0000\u0000\u00006\u010c\u0001\u0000\u0000\u0000"+
		"8\u010f\u0001\u0000\u0000\u0000:\u0113\u0001\u0000\u0000\u0000<=\u0005"+
		"$\u0000\u0000=>\u0005$\u0000\u0000>?\u0005{\u0000\u0000?\u0005\u0001\u0000"+
		"\u0000\u0000@A\u0005$\u0000\u0000AB\u0005{\u0000\u0000B\u0007\u0001\u0000"+
		"\u0000\u0000CD\u0005{\u0000\u0000DE\u0005{\u0000\u0000E\t\u0001\u0000"+
		"\u0000\u0000FG\u0005}\u0000\u0000GH\u0005}\u0000\u0000H\u000b\u0001\u0000"+
		"\u0000\u0000IJ\u0005{\u0000\u0000JK\u0005{\u0000\u0000KL\u0005%\u0000"+
		"\u0000LP\u0001\u0000\u0000\u0000MO\u0007\u0000\u0000\u0000NM\u0001\u0000"+
		"\u0000\u0000OR\u0001\u0000\u0000\u0000PN\u0001\u0000\u0000\u0000PQ\u0001"+
		"\u0000\u0000\u0000QS\u0001\u0000\u0000\u0000RP\u0001\u0000\u0000\u0000"+
		"ST\u0005r\u0000\u0000TU\u0005a\u0000\u0000UV\u0005n\u0000\u0000VW\u0005"+
		"g\u0000\u0000WX\u0005e\u0000\u0000XZ\u0001\u0000\u0000\u0000Y[\u0007\u0000"+
		"\u0000\u0000ZY\u0001\u0000\u0000\u0000[\\\u0001\u0000\u0000\u0000\\Z\u0001"+
		"\u0000\u0000\u0000\\]\u0001\u0000\u0000\u0000]^\u0001\u0000\u0000\u0000"+
		"^_\u0006\u0004\u0000\u0000_\r\u0001\u0000\u0000\u0000`a\u0005{\u0000\u0000"+
		"ab\u0005{\u0000\u0000bc\u0005%\u0000\u0000cg\u0001\u0000\u0000\u0000d"+
		"f\u0007\u0000\u0000\u0000ed\u0001\u0000\u0000\u0000fi\u0001\u0000\u0000"+
		"\u0000ge\u0001\u0000\u0000\u0000gh\u0001\u0000\u0000\u0000hj\u0001\u0000"+
		"\u0000\u0000ig\u0001\u0000\u0000\u0000jk\u0005w\u0000\u0000kl\u0005i\u0000"+
		"\u0000lm\u0005t\u0000\u0000mn\u0005h\u0000\u0000np\u0001\u0000\u0000\u0000"+
		"oq\u0007\u0000\u0000\u0000po\u0001\u0000\u0000\u0000qr\u0001\u0000\u0000"+
		"\u0000rp\u0001\u0000\u0000\u0000rs\u0001\u0000\u0000\u0000st\u0001\u0000"+
		"\u0000\u0000tu\u0006\u0005\u0000\u0000u\u000f\u0001\u0000\u0000\u0000"+
		"vw\u0005{\u0000\u0000wx\u0005{\u0000\u0000xy\u0005%\u0000\u0000yz\u0005"+
		"-\u0000\u0000z~\u0001\u0000\u0000\u0000{}\u0007\u0000\u0000\u0000|{\u0001"+
		"\u0000\u0000\u0000}\u0080\u0001\u0000\u0000\u0000~|\u0001\u0000\u0000"+
		"\u0000~\u007f\u0001\u0000\u0000\u0000\u007f\u0081\u0001\u0000\u0000\u0000"+
		"\u0080~\u0001\u0000\u0000\u0000\u0081\u0082\u0005i\u0000\u0000\u0082\u0083"+
		"\u0005f\u0000\u0000\u0083\u0085\u0001\u0000\u0000\u0000\u0084\u0086\u0007"+
		"\u0000\u0000\u0000\u0085\u0084\u0001\u0000\u0000\u0000\u0086\u0087\u0001"+
		"\u0000\u0000\u0000\u0087\u0085\u0001\u0000\u0000\u0000\u0087\u0088\u0001"+
		"\u0000\u0000\u0000\u0088\u0089\u0001\u0000\u0000\u0000\u0089\u008a\u0006"+
		"\u0006\u0000\u0000\u008a\u0011\u0001\u0000\u0000\u0000\u008b\u008c\u0005"+
		"{\u0000\u0000\u008c\u008d\u0005{\u0000\u0000\u008d\u008e\u0005%\u0000"+
		"\u0000\u008e\u0092\u0001\u0000\u0000\u0000\u008f\u0091\u0007\u0000\u0000"+
		"\u0000\u0090\u008f\u0001\u0000\u0000\u0000\u0091\u0094\u0001\u0000\u0000"+
		"\u0000\u0092\u0090\u0001\u0000\u0000\u0000\u0092\u0093\u0001\u0000\u0000"+
		"\u0000\u0093\u0095\u0001\u0000\u0000\u0000\u0094\u0092\u0001\u0000\u0000"+
		"\u0000\u0095\u0096\u0005i\u0000\u0000\u0096\u0097\u0005f\u0000\u0000\u0097"+
		"\u0099\u0001\u0000\u0000\u0000\u0098\u009a\u0007\u0000\u0000\u0000\u0099"+
		"\u0098\u0001\u0000\u0000\u0000\u009a\u009b\u0001\u0000\u0000\u0000\u009b"+
		"\u0099\u0001\u0000\u0000\u0000\u009b\u009c\u0001\u0000\u0000\u0000\u009c"+
		"\u009d\u0001\u0000\u0000\u0000\u009d\u009e\u0006\u0007\u0000\u0000\u009e"+
		"\u0013\u0001\u0000\u0000\u0000\u009f\u00a0\u0005{\u0000\u0000\u00a0\u00a1"+
		"\u0005{\u0000\u0000\u00a1\u00a2\u0005%\u0000\u0000\u00a2\u00a6\u0001\u0000"+
		"\u0000\u0000\u00a3\u00a5\u0007\u0000\u0000\u0000\u00a4\u00a3\u0001\u0000"+
		"\u0000\u0000\u00a5\u00a8\u0001\u0000\u0000\u0000\u00a6\u00a4\u0001\u0000"+
		"\u0000\u0000\u00a6\u00a7\u0001\u0000\u0000\u0000\u00a7\u00a9\u0001\u0000"+
		"\u0000\u0000\u00a8\u00a6\u0001\u0000\u0000\u0000\u00a9\u00aa\u0005e\u0000"+
		"\u0000\u00aa\u00ab\u0005l\u0000\u0000\u00ab\u00ac\u0005s\u0000\u0000\u00ac"+
		"\u00ad\u0005e\u0000\u0000\u00ad\u00b1\u0001\u0000\u0000\u0000\u00ae\u00b0"+
		"\u0007\u0000\u0000\u0000\u00af\u00ae\u0001\u0000\u0000\u0000\u00b0\u00b3"+
		"\u0001\u0000\u0000\u0000\u00b1\u00af\u0001\u0000\u0000\u0000\u00b1\u00b2"+
		"\u0001\u0000\u0000\u0000\u00b2\u00b4\u0001\u0000\u0000\u0000\u00b3\u00b1"+
		"\u0001\u0000\u0000\u0000\u00b4\u00b5\u0005}\u0000\u0000\u00b5\u00b6\u0005"+
		"}\u0000\u0000\u00b6\u0015\u0001\u0000\u0000\u0000\u00b7\u00b8\u0005{\u0000"+
		"\u0000\u00b8\u00b9\u0005{\u0000\u0000\u00b9\u00ba\u0005%\u0000\u0000\u00ba"+
		"\u00be\u0001\u0000\u0000\u0000\u00bb\u00bd\u0007\u0000\u0000\u0000\u00bc"+
		"\u00bb\u0001\u0000\u0000\u0000\u00bd\u00c0\u0001\u0000\u0000\u0000\u00be"+
		"\u00bc\u0001\u0000\u0000\u0000\u00be\u00bf\u0001\u0000\u0000\u0000\u00bf"+
		"\u00c1\u0001\u0000\u0000\u0000\u00c0\u00be\u0001\u0000\u0000\u0000\u00c1"+
		"\u00c2\u0005e\u0000\u0000\u00c2\u00c3\u0005n\u0000\u0000\u00c3\u00c4\u0005"+
		"d\u0000\u0000\u00c4\u00c8\u0001\u0000\u0000\u0000\u00c5\u00c7\u0007\u0000"+
		"\u0000\u0000\u00c6\u00c5\u0001\u0000\u0000\u0000\u00c7\u00ca\u0001\u0000"+
		"\u0000\u0000\u00c8\u00c6\u0001\u0000\u0000\u0000\u00c8\u00c9\u0001\u0000"+
		"\u0000\u0000\u00c9\u00cb\u0001\u0000\u0000\u0000\u00ca\u00c8\u0001\u0000"+
		"\u0000\u0000\u00cb\u00cc\u0005}\u0000\u0000\u00cc\u00cd\u0005}\u0000\u0000"+
		"\u00cd\u0017\u0001\u0000\u0000\u0000\u00ce\u00cf\u0003\u0004\u0000\u0000"+
		"\u00cf\u00d0\u0001\u0000\u0000\u0000\u00d0\u00d1\u0006\n\u0001\u0000\u00d1"+
		"\u0019\u0001\u0000\u0000\u0000\u00d2\u00d3\u0003\u0006\u0001\u0000\u00d3"+
		"\u00d4\u0001\u0000\u0000\u0000\u00d4\u00d5\u0006\u000b\u0001\u0000\u00d5"+
		"\u001b\u0001\u0000\u0000\u0000\u00d6\u00d7\u0005{\u0000\u0000\u00d7\u00d8"+
		"\u0005{\u0000\u0000\u00d8\u00d9\u0005-\u0000\u0000\u00d9\u00da\u0001\u0000"+
		"\u0000\u0000\u00da\u00db\u0006\f\u0002\u0000\u00db\u001d\u0001\u0000\u0000"+
		"\u0000\u00dc\u00dd\u0003\b\u0002\u0000\u00dd\u00de\u0001\u0000\u0000\u0000"+
		"\u00de\u00df\u0006\r\u0002\u0000\u00df\u001f\u0001\u0000\u0000\u0000\u00e0"+
		"\u00e1\u0004\u000e\u0000\u0000\u00e1\u00e2\t\u0000\u0000\u0000\u00e2\u00e3"+
		"\u0006\u000e\u0003\u0000\u00e3\u00e4\u0001\u0000\u0000\u0000\u00e4\u00e5"+
		"\u0006\u000e\u0004\u0000\u00e5\u00e6\u0006\u000e\u0002\u0000\u00e6!\u0001"+
		"\u0000\u0000\u0000\u00e7\u00e8\t\u0000\u0000\u0000\u00e8#\u0001\u0000"+
		"\u0000\u0000\u00e9\u00ea\u0005{\u0000\u0000\u00ea%\u0001\u0000\u0000\u0000"+
		"\u00eb\u00ec\u0005}\u0000\u0000\u00ec\'\u0001\u0000\u0000\u0000\u00ed"+
		"\u00ee\u0003$\u0010\u0000\u00ee\u00ef\u0001\u0000\u0000\u0000\u00ef\u00f0"+
		"\u0006\u0012\u0001\u0000\u00f0)\u0001\u0000\u0000\u0000\u00f1\u00f2\u0003"+
		"&\u0011\u0000\u00f2\u00f3\u0001\u0000\u0000\u0000\u00f3\u00f4\u0006\u0013"+
		"\u0005\u0000\u00f4+\u0001\u0000\u0000\u0000\u00f5\u00f6\t\u0000\u0000"+
		"\u0000\u00f6-\u0001\u0000\u0000\u0000\u00f7\u00f8\u0003$\u0010\u0000\u00f8"+
		"\u00f9\u0001\u0000\u0000\u0000\u00f9\u00fa\u0006\u0015\u0001\u0000\u00fa"+
		"/\u0001\u0000\u0000\u0000\u00fb\u00fc\u0005-\u0000\u0000\u00fc\u00fd\u0005"+
		"}\u0000\u0000\u00fd\u00fe\u0005}\u0000\u0000\u00fe\u00ff\u0001\u0000\u0000"+
		"\u0000\u00ff\u0100\u0006\u0016\u0005\u0000\u01001\u0001\u0000\u0000\u0000"+
		"\u0101\u0102\u0004\u0017\u0001\u0000\u0102\u0103\t\u0000\u0000\u0000\u0103"+
		"\u0104\u0006\u0017\u0006\u0000\u0104\u0105\u0001\u0000\u0000\u0000\u0105"+
		"\u0106\u0006\u0017\u0007\u0000\u0106\u0107\u0006\u0017\u0005\u0000\u0107"+
		"3\u0001\u0000\u0000\u0000\u0108\u0109\u0003\n\u0003\u0000\u0109\u010a"+
		"\u0001\u0000\u0000\u0000\u010a\u010b\u0006\u0018\u0005\u0000\u010b5\u0001"+
		"\u0000\u0000\u0000\u010c\u010d\t\u0000\u0000\u0000\u010d7\u0001\u0000"+
		"\u0000\u0000\u010e\u0110\b\u0001\u0000\u0000\u010f\u010e\u0001\u0000\u0000"+
		"\u0000\u0110\u0111\u0001\u0000\u0000\u0000\u0111\u010f\u0001\u0000\u0000"+
		"\u0000\u0111\u0112\u0001\u0000\u0000\u0000\u01129\u0001\u0000\u0000\u0000"+
		"\u0113\u0114\u0005}\u0000\u0000\u0114\u0115\u0005}\u0000\u0000\u0115\u0116"+
		"\u0001\u0000\u0000\u0000\u0116\u0117\u0006\u001b\u0005\u0000\u0117;\u0001"+
		"\u0000\u0000\u0000\u0011\u0000\u0001\u0002\u0003P\\gr~\u0087\u0092\u009b"+
		"\u00a6\u00b1\u00be\u00c8\u0111\b\u0005\u0003\u0000\u0005\u0001\u0000\u0005"+
		"\u0002\u0000\u0001\u000e\u0000\u0007\n\u0000\u0004\u0000\u0000\u0001\u0017"+
		"\u0001\u0007\u0011\u0000";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}