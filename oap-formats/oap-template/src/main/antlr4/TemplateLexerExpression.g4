lexer grammar TemplateLexerExpression;

import TemplateLexerBasic; 

fragment Ws				: Hws | Vws	;
fragment Hws			: [ \t]		;
fragment Vws			: [\r\n\f]	;

fragment BlockComment	: '/*'  .*? ('*/' | EOF)	;


fragment Esc			: '\\'	    ;
fragment SQuote			: '\''	    ;
fragment DQuote			: '"'	    ;
fragment Underscore		: '_'	    ;
fragment Comma			: ','	    ;
fragment Semi			: ';'	    ;
fragment Pipe			: '|'	    ;
fragment Dot			: '.'	    ;
fragment LParen			: '('	    ;
fragment RParen			: ')'	    ;
fragment LBrack			: '['	    ;
fragment RBrack			: ']'	    ;
fragment Star			: '*'	    ;
fragment Slash			: '/'	    ;
fragment Percent		: '%'	    ;
fragment Plus			: '+'	    ;
fragment Minus			: '-'	    ;
fragment DQuestion		: '??'	    ;
fragment LT             : '<'       ;
fragment GT             : '>'       ;
fragment Default        : 'default' ;

fragment NameChar
	:	[A-Z]
	|	[a-z]
	|	'\u00C0'..'\u00D6'
	|	'\u00D8'..'\u00F6'
	|	'\u00F8'..'\u02FF'
	|	'\u0370'..'\u037D'
	|	'\u037F'..'\u1FFF'
	|	'\u200C'..'\u200D'
	|	'\u2070'..'\u218F'
	|	'\u2C00'..'\u2FEF'
	|	'\u3001'..'\uD7FF'
	|	'\uF900'..'\uFDCF'
	|	'\uFDF0'..'\uFFFD'
	|	Underscore
	|	'\u00B7'
	|	'\u0300'..'\u036F'
	|	'\u203F'..'\u2040'
	;

fragment EscSeq
	:	Esc
		( [btnfr"'\\]	// The standard escaped character set such as tab, newline, etc.
		| UnicodeEsc	// A Unicode escape sequence
		| .				// Invalid escape character
		| EOF			// Incomplete at EOF
		)
	;

fragment UnicodeEsc
	:	'u' (HexDigit (HexDigit (HexDigit HexDigit?)?)?)?
	;

fragment SQuoteLiteral	: SQuote ( EscSeq | ~['\r\n\\] )* SQuote	;
fragment DQuoteLiteral	: DQuote ( EscSeq | ~["\r\n\\] )* DQuote	;
fragment BoolLiteral	: True | False								;


fragment HexDigit		: [0-9a-fA-F]	                ;
fragment DecDigit		: [0-9]			                ;

fragment DecDigits		: DecDigit+		                ;
fragment Float          : DecDigits Dot DecDigits?      ;

fragment True		 	: 'true'	                    ;
fragment False			: 'false'   	                ;


DEFAULT         : Pipe Hws* Default                     ;

BLOCK_COMMENT	: BlockComment;

HORZ_WS	        : Hws+		-> skip	;
VERT_WS	        : Vws+		-> skip	;

LBRACE          : LBrace -> pushMode(Concatenation)     ;

RBRACE		    : RBrace -> popMode                     ;

DOT			: Dot			                            ;
LPAREN		: LParen		                            ;
RPAREN		: RParen		                            ;
LBRACK		: LBrack			                        ;
RBRACK		: RBrack			                        ;
DQUESTION   : DQuestion                                 ;
SEMI        : Semi                                      ;
COMMA		: Comma                                     ;

STAR        : Star                                      ;
SLASH       : Slash                                     ;
PERCENT     : Percent                                   ;
PLUS        : Plus                                      ;
MINUS       : Minus                                     ;

DSTRING     : DQuoteLiteral                             ;
SSTRING     : SQuoteLiteral                             ;
DECDIGITS   : DecDigits                                 ;
FLOAT       : Float                                     ;
BOOLEAN     : BoolLiteral                               ;
ID			: NameChar (NameChar|DecDigit)*			    ;
CAST_TYPE   : LT (NameChar|DOT)+ CAST_TYPE? GT          ;


ERR_CHAR	: Hws	-> skip		                        ;

mode Concatenation                                      ;

C_HORZ_WS	: Hws+		-> skip	                        ;
C_VERT_WS	: Vws+		-> skip	                        ;

CRBRACE		: RBrace -> popMode, type(RBRACE)           ;
CCOMMA		: Comma -> type(COMMA)                      ;

CID			: NameChar (NameChar|DecDigit)* -> type(ID) ;
CDSTRING    : DQuoteLiteral -> type(DSTRING)            ;
CSSTRING    : SQuoteLiteral -> type(SSTRING)            ;
CDECDIGITS  : DecDigits -> type(DECDIGITS)              ;
CFLOAT      : Float -> type(FLOAT)                      ;

CERR_CHAR	: (' '|'\t')	-> skip		                ;
