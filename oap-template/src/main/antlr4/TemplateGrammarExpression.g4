parser grammar TemplateGrammarExpression;

options {
	language=Java;
	tokenVocab=TemplateLexerExpression;
	superClass = TemplateGrammarAdaptor;
}

@header {
package oap.template;

import oap.template.tree.*;
import oap.template.tree.Math;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import oap.util.Lists;

}

@parser::members {
	public TemplateGrammarExpression(TokenStream input, Map<String, List<Method>> builtInFunction, ErrorStrategy errorStrategy) {
		this(input);
		
		this.builtInFunction = builtInFunction;
		this.errorStrategy = errorStrategy;
	}

}

expression returns [Expression ret]
    : (BLOCK_COMMENT)? (CAST_TYPE)? exprs orExprs defaultValue? function? {
        $ret = new Expression( $BLOCK_COMMENT.text,
                                $CAST_TYPE.text != null ? $CAST_TYPE.text.substring( 1, $CAST_TYPE.text.length() - 1 ) : null,
                                Lists.concat( List.of( $exprs.ret ), $orExprs.ret ),
                                $defaultValue.ctx != null ? $defaultValue.ret : null,
                                $function.ctx != null ? $function.ret : null );
      }
    ;
    

defaultValue returns [String ret]
    : DQUESTION defaultValueType { $ret = $defaultValueType.ret; }
    ;

defaultValueType returns [String ret]
    : SSTRING { $ret = sdStringToString( $SSTRING.text ); }
    | DSTRING { $ret = sdStringToString($DSTRING.text); }
    | longRule { $ret = $longRule.text; }
    | FLOAT { $ret = $FLOAT.text; }
    | BOOLEAN { $ret = $BOOLEAN.text; }
    | LBRACK RBRACK { $ret = "[]"; }
    ; 

longRule
    : MINUS? DECDIGITS
    ;

function returns [Func ret]
    : SEMI ID LPAREN functionArgs? RPAREN { $ret = new Func( $ID.text, $functionArgs.ctx != null ? $functionArgs.ret : List.of() ); }
    ;

functionArgs returns [ArrayList<String> ret = new ArrayList<>()]
    : functionArg { $ret.add( $functionArg.ret ); } ( COMMA functionArg { $ret.add( $functionArg.ret ); } )*
    ;

functionArg returns [String ret]
    : DECDIGITS { $ret = $DECDIGITS.text; }
    | MINUS DECDIGITS { $ret = "-" + $DECDIGITS.text; }
    | FLOAT { $ret = $FLOAT.text; }
    | MINUS FLOAT { $ret = "-" + $FLOAT.text; }
    | SSTRING { $ret = sStringToDString( $SSTRING.text ); }
    | DSTRING { $ret = $DSTRING.text; }
    ;

orExprs returns [ArrayList<Exprs> ret = new ArrayList<Exprs>() ]
    : (PIPE exprs { $ret.add( $exprs.ret ); } ( PIPE exprs { $ret.add( $exprs.ret ); })*)
    |
    ;

exprs returns [Exprs ret = new Exprs()]
    : expr  { $ret.exprs.add( $expr.ret ); }
      math? { if( $math.ctx != null ) $ret.math = $math.ret; }
    | expr  { $ret.exprs.add( $expr.ret ); }
      DOT? concatenation { $ret.concatenation =$concatenation.ret; }
    | expr { $ret.exprs.add( $expr.ret ); }
      (DOT expr {
        $ret.exprs.add( $expr.ret );
      })*
      DOT expr { $ret.exprs.add( $expr.ret ); }
      math? { if( $math.ctx != null ) $ret.math = $math.ret; }
    | expr { $ret.exprs.add( $expr.ret ); }
      (DOT expr { $ret.exprs.add( $expr.ret ); })*
      DOT expr { $ret.exprs.add( $expr.ret ); }
      DOT? concatenation { $ret.concatenation = $concatenation.ret; }
      math? { if( $math.ctx != null ) $ret.math = $math.ret; }
    | concatenation { $ret.concatenation = $concatenation.ret; }
    ;

expr returns [Expr ret]
    : (ID LPAREN functionArgs? RPAREN) { $ret = new Expr($ID.text, true, $functionArgs.ctx != null ? $functionArgs.ret : List.of() ); }
    | ID { $ret = new Expr($ID.text, false, List.of() ); }
    ;

concatenation returns [Concatenation ret]
    : LBRACE citems { $ret = new Concatenation( $citems.ret ); } RBRACE
    ;

citems returns [ArrayList<Object> ret = new ArrayList<>()]
    : citem { $ret.add($citem.ret); }
        ( COMMA citem { $ret.add($citem.ret); } )*
    ;

citem returns [Object ret]
    : ID { $ret = new Expr( $ID.text, false, List.of() ); }
    | DSTRING  { $ret = sdStringToString( $DSTRING.text ); }
    | SSTRING { $ret = sdStringToString( $SSTRING.text ); }
    | DECDIGITS { $ret = String.valueOf( $DECDIGITS.text ); }
    | FLOAT { $ret = String.valueOf( $FLOAT.text ); }
    ;

math returns [Math ret]
    : mathOperation number { $ret = new Math( $mathOperation.text, $number.text ); }
    ;

number:
    | DECDIGITS
    | FLOAT
    ;

mathOperation
    : STAR
    | SLASH
    | PERCENT
    | PLUS
    | MINUS
    ;
    
