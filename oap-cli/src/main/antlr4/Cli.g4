grammar Cli;

@header {
package oap.cli;
import java.util.List;
import java.util.ArrayList;
import oap.util.Pair;
import static oap.util.Pair.__;
}

parameters returns [List<Pair<String, String>> list = new ArrayList<Pair<String, String>>()]
    :   (p=parameter {$list.add($p.p);})* EOF
    ;

parameter returns [Pair<String, String> p = null]
    :   n=NAME (v=value)? {$p = __($n.text, $v.text);}
    ;

value
    :   VALUE | STRVALUE
    ;


NAME
    :   '--' ('a'..'z' | 'A'..'Z' | '0'..'9' | ':' | '-' | '_')+ {setText(getText().substring(2));}
    ;
VALUE
    :   '=' (~('"'|' '|'\t' | '\n' | '\r'))+ {setText(getText().substring(1));}
    ;
STRVALUE
    :   '=' '"' (ESC | ~('"' | '\\'| '\n' | '\r'))* '\"' {
        String s = getText();
        s = s.substring(2, s.length() - 1);
        s = s.replace("\\\"", "\"");
        s = s.replace("\\\\", "\\");
        setText(s);
    }
    ;

fragment ESC
    :   '\\' ('"' | '\\')
    ;

WS
    :   (' ' | '\t' | ( '\r\n' | '\r' | '\n'))+ -> skip
    ;
