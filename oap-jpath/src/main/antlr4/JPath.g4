grammar JPath;

@header {
package oap.jpath;

import java.util.List;
import java.util.ArrayList;
}

expr returns [Expression expression]
    : 'var' {$expression = new Expression(IdentifierType.VARIABLE);} ':' f=variableDeclaratorId {$expression.path.add(new PathNode(PathType.FIELD, $f.text));} ( '.' n=path {$expression.path.add($n.pathNode);} )*
    ; 

path returns [PathNode pathNode]
    : v=variableDeclaratorId {$pathNode = new PathNode(PathType.FIELD, $v.text); }
    | m=methodName {$pathNode = new PathNode(PathType.METHOD, $m.name); }
    ;
    

variableDeclaratorId
	:	identifier
	;


methodName returns [String name]
	:	i=identifier '(' ')' {$name = $i.text;}
	;


identifier
    :
        Identifier
    ;

Identifier
	:	JavaLetter JavaLetterOrDigit*
	;

fragment
Digit
	:	[0-9]
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
	:	'"' StringCharacters? '"'
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
