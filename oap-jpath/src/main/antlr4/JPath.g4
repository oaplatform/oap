grammar JPath;

@header {
package oap.jpath;

import java.util.List;
import java.lang.Number;
import java.util.ArrayList;
import oap.util.Pair;
import static oap.util.Pair.__;
}

expr returns [Expression expression]
    : 'var' {$expression = new Expression(IdentifierType.VARIABLE);} ':' f=variableDeclaratorId {$expression.path.add(new PathNodeField($f.text));} ( '.' n=path {$expression.path.add($n.pathNode);} )*
    ; 

path returns [PathNode pathNode]
    : v=variableDeclaratorId {$pathNode = new PathNodeField($v.text); }
    | m=method {$pathNode = new PathNodeMethod($m.nameWithParams._1, $m.nameWithParams._2); }
    | a=array {$pathNode = new PathNodeArray($a.arrayValue._1, $a.arrayValue._2); }
    ;
    

variableDeclaratorId
	:	identifier
	;


array returns [Pair<String,Integer> arrayValue]
    :   i=identifier '[' n=DecimalIntegerLiteral ']' {$arrayValue = __($i.text, Integer.parseInt($n.text));}
    ;

method returns [Pair<String,List<Object>> nameWithParams]
	:	i=identifier '(' p=methodParameters ')' {$nameWithParams = __($i.text, $p.arguments);}
	;

methodParameters returns [List<Object> arguments = new ArrayList<Object>()]
    :
    | mp=methodParameter {$arguments.add($mp.argument);} ( ',' mp2=methodParameter {$arguments.add($mp2.argument);})*
    ;

methodParameter returns [Object argument]
    : s=StringLiteral {$argument = $s.text;}
    | di=DecimalIntegerLiteral {$argument = Long.parseLong($di.text);}
    ;

identifier
    : Identifier
    ;

Identifier
	:	JavaLetter JavaLetterOrDigit*
	;

DecimalIntegerLiteral
	:	DecimalNumeral
	;

fragment
DecimalNumeral
	:	'0'
	|	NonZeroDigit (Digits? | Underscores Digits)
	;

fragment
Digits
	:	Digit (DigitsAndUnderscores? Digit)?
	;
	
fragment
Digit
	:	'0'
	|	NonZeroDigit
	;
	
fragment
NonZeroDigit
	:	[1-9]
	;

fragment
DigitsAndUnderscores
	:	DigitOrUnderscore+
	;

fragment
DigitOrUnderscore
	:	Digit
	|	'_'
	;
		
fragment
Underscores
	:	'_'+
	;
	
fragment
JavaLetter	
	:	[a-zA-Z$_-] // these are the "java letters" below 0x7F
	|	// covers all characters above 0x7F which are not a surrogate
		~[\u0000-\u007F\uD800-\uDBFF]
		{Character.isJavaIdentifierStart(_input.LA(-1))}?
	|	// covers UTF-16 surrogate pairs encodings for U+10000 to U+10FFFF
		[\uD800-\uDBFF] [\uDC00-\uDFFF]
		{Character.isJavaIdentifierStart(Character.toCodePoint((char)_input.LA(-2), (char)_input.LA(-1)))}?
	;
		
fragment
JavaLetterOrDigit
	:	[a-zA-Z0-9$_-] // these are the "java letters or digits" below 0x7F
	|	// covers all characters above 0x7F which are not a surrogate
		~[\u0000-\u007F\uD800-\uDBFF]
		{Character.isJavaIdentifierPart(_input.LA(-1))}?
	|	// covers UTF-16 surrogate pairs encodings for U+10000 to U+10FFFF
		[\uD800-\uDBFF] [\uDC00-\uDFFF]
		{Character.isJavaIdentifierPart(Character.toCodePoint((char)_input.LA(-2), (char)_input.LA(-1)))}?
	;

fragment
EscapeSequence
	:	'\\' [btnfr"'\\]
	;

StringLiteral
	:	'"' StringCharacters? '"' {setText(getText().substring(1, getText().length() - 1));}
	;
fragment
StringCharacters
	:	StringCharacter+
	;

fragment
StringCharacter
	:	~["\\\r\n]
	|	EscapeSequence
	;

SPACE : (' '|'\n') -> skip;
