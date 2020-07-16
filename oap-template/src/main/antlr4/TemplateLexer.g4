lexer grammar TemplateLexer;

@header {
package oap.template;
}

fragment Esc			: '\\'	;
fragment SQuote			: '\''	;
fragment DQuote			: '"'	;
fragment Underscore		: '_'	;
fragment Comma			: ','	;
fragment Semi			: ';'	;
fragment Pipe			: '|'	;
fragment Dot			: '.'	;
fragment LParen			: '('	;
fragment RParen			: ')'	;
fragment LBrace			: '{'	;
fragment RBrace			: '}'	;
fragment LBrack			: '['	;
fragment RBrack			: ']'	;
fragment StartExpr      : '${'  ;
fragment DQuestion		: '??'	;


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


fragment HexDigit		: [0-9a-fA-F]	            ;
fragment DecDigit		: [0-9]			            ;

fragment DecDigits		: DecDigit+		            ;
fragment Float          :   DecDigits Dot DecDigits?;

STARTEXPR   : StartExpr -> pushMode(Expression)     ;   

TEXT		: .                                     ;

mode Expression                                     ;

LBRACE      : LBrace -> pushMode(Concatenation)     ;

RBRACE		: RBrace -> popMode                     ;

PIPE		: Pipe                                  ;
DOT			: Dot			                        ;
LPAREN		: LParen		                        ;
RPAREN		: RParen		                        ;
LBRACK		: LBrack			                    ;
RBRACK		: RBrack			                    ;
DQUESTION   : DQuestion                             ;
SEMI        : Semi                                  ;
COMMA		: Comma                                 ;

ID			: NameChar (NameChar|DecDigit)*			;
DSTRING     : DQuoteLiteral                         ;
SSTRING     : SQuoteLiteral                         ;
DECDIGITS   : DecDigits                             ;
FLOAT       : Float                                 ;


ERR_CHAR	: (' '|'\t')	-> skip		            ;

mode Concatenation                                  ;

CRBRACE		: RBrace -> popMode                     ;
CCOMMA		: Comma                                 ;

CID			: NameChar (NameChar|DecDigit)*			;
CDSTRING     : DQuoteLiteral                        ;
CSSTRING     : SQuoteLiteral                        ;
CDECDIGITS   : DecDigits                            ;
CFLOAT       : Float                                ;

CERR_CHAR	: (' '|'\t')	-> skip		            ;
