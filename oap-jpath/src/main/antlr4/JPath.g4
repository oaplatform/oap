grammar JPath;

@header {
package oap.jpath;

import java.util.List;
import java.util.ArrayList;
import oap.util.Pair;
import static oap.util.Pair.__;
}

expr returns [Expression expression]
    : 'var' {$expression = new Expression(IdentifierType.VARIABLE);} ':' f=variableDeclaratorId {$expression.path.add(new PathNodeField(PathType.FIELD, $f.text));} ( '.' n=path {$expression.path.add($n.pathNode);} )*
    ; 

path returns [PathNode pathNode]
    : v=variableDeclaratorId {$pathNode = new PathNodeField(PathType.FIELD, $v.text); }
    | m=method {$pathNode = new PathNodeMethod(PathType.METHOD, $m.nameWithParams._1, $m.nameWithParams._2); }
    ;
    

variableDeclaratorId
	:	identifier
	;


method returns [Pair<String,List<Object>> nameWithParams]
	:	i=identifier '(' p=methodParameters ')' {$nameWithParams = __($i.text, $p.arguments);}
	;

methodParameters returns [List<Object> arguments = new ArrayList<Object>()]
    :
    | s=StringLiteral {$arguments.add($s.text);} (',' n=StringLiteral {$arguments.add($n.text);})*
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
