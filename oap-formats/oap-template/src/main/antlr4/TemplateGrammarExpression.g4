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
import oap.template.tree.WithCondition;
import oap.template.tree.ConditionExpr;
import oap.template.tree.FieldConditionExpr;
import oap.template.tree.AndConditionExpr;
import oap.template.tree.OrConditionExpr;
import oap.template.tree.NotConditionExpr;
import oap.template.tree.CompareConditionExpr;
import oap.template.tree.CompareValue;
import oap.template.tree.LiteralCompareValue;

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
    : (BLOCK_COMMENT)? (CAST_TYPE)? (ifCode | withCode | exprsCode) defaultValue? function? (IF ifCondition)? {
        $ret = new Expression(
          $BLOCK_COMMENT.text,
          $CAST_TYPE.text != null ? $CAST_TYPE.text.substring( 1, $CAST_TYPE.text.length() - 1 ) : null,
          $ifCode.ctx != null ? $ifCode.ret : null,
          $withCode.ctx != null ? $withCode.ret : null,
          $exprsCode.ctx != null ? $exprsCode.ret : null,
          $defaultValue.ctx != null ? $defaultValue.ret : null,
          $function.ctx != null ? $function.ret : null );
      }
    ;

ifCode returns [IfCondition ret]
    : IF ifCondition THEN thenCode=exprs (ELSE elseCode=exprs)? END {
        $ret = new IfCondition( $ifCondition.ret, $thenCode.ret, $elseCode.ctx != null ? $elseCode.ret : null );
      }
    ;

withCode returns [WithCondition ret]
    : scopePath=exprs LBRACE concatItems=concatBody RBRACE {
        Exprs bodyExprs = new Exprs();
        bodyExprs.concatenation = new Concatenation( $concatItems.ret );
        ArrayList<Exprs> body = new ArrayList<>();
        body.add( bodyExprs );
        $ret = new WithCondition( $scopePath.ret, body );
      }
    | scopePath=exprs LBRACE bodyExprs=exprsCode RBRACE {
        $ret = new WithCondition( $scopePath.ret, $bodyExprs.ret );
      }
    ;

concatBody returns [ArrayList<Object> ret = new ArrayList<>()]
    : citem { $ret.add( $citem.ret ); }
      ( PLUS citem { $ret.add( $citem.ret ); } )+
    ;

topLevelConcat returns [Exprs ret = new Exprs()]
    : first=citem {
        $ret.concatenation = new Concatenation( new ArrayList<>() );
        $ret.concatenation.items.add( $first.ret );
      }
      ( PLUS next=citem {
        $ret.concatenation.items.add( $next.ret );
      } )+
    ;

exprsCode returns [ArrayList<Exprs> ret = new ArrayList<>()]
    : topLevelConcat {
        $ret.add( $topLevelConcat.ret );
      }
    | exprs orExprs {
        $ret.add( $exprs.ret );
        $ret.addAll( $orExprs.ret );
      }
    ;

ifCondition returns [ConditionExpr ret]
    : conditionOr { $ret = $conditionOr.ret; }
    ;

conditionOr returns [ConditionExpr ret]
    : left=conditionAnd { $ret = $left.ret; }
      ( OR right=conditionAnd { $ret = new OrConditionExpr( $ret, $right.ret ); } )*
    ;

conditionAnd returns [ConditionExpr ret]
    : left=conditionNot { $ret = $left.ret; }
      ( AND right=conditionNot { $ret = new AndConditionExpr( $ret, $right.ret ); } )*
    ;

conditionNot returns [ConditionExpr ret]
    : ( NOT | BANG ) inner=conditionNot { $ret = new NotConditionExpr( $inner.ret ); }
    | conditionAtom { $ret = $conditionAtom.ret; }
    ;

conditionAtom returns [ConditionExpr ret]
    : LPAREN ifCondition RPAREN { $ret = $ifCondition.ret; }
    | left=exprs op=(EQEQ | EQ_KW | NEQ | NE_KW | GT_OP | LT_OP | GE_OP | LE_OP | EQI_KW | CONTAINS_KW) right=compareRhs {
        $ret = new CompareConditionExpr( $left.ret, $op.getText(), $right.ret );
      }
    | exprs { $ret = new FieldConditionExpr( $exprs.ret ); }
    ;

compareRhs returns [CompareValue ret]
    : SSTRING         { $ret = new LiteralCompareValue( sdStringToString( $SSTRING.text ) ); }
    | DSTRING         { $ret = new LiteralCompareValue( sdStringToString( $DSTRING.text ) ); }
    | DECDIGITS       { $ret = new LiteralCompareValue( $DECDIGITS.text ); }
    | MINUS DECDIGITS { $ret = new LiteralCompareValue( "-" + $DECDIGITS.text ); }
    | FLOAT           { $ret = new LiteralCompareValue( $FLOAT.text ); }
    | MINUS FLOAT     { $ret = new LiteralCompareValue( "-" + $FLOAT.text ); }
    | BOOLEAN         { $ret = new LiteralCompareValue( $BOOLEAN.text ); }
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
    : (DEFAULT exprs { $ret.add( $exprs.ret ); } ( DEFAULT exprs { $ret.add( $exprs.ret ); })*)
    |
    ;

exprs returns [Exprs ret = new Exprs()]
    : ROOT DOT expr { $ret.rootScoped = true; $ret.exprs.add( $expr.ret ); }
      (DOT expr { $ret.exprs.add( $expr.ret ); })*
      math? { if( $math.ctx != null ) $ret.math = $math.ret; }
    | VAR_ID { $ret.varName = $VAR_ID.text.substring( 1 ); }
      (DOT expr { $ret.exprs.add( $expr.ret ); })*
      math? { if( $math.ctx != null ) $ret.math = $math.ret; }
    | expr  { $ret.exprs.add( $expr.ret ); }
      math? { if( $math.ctx != null ) $ret.math = $math.ret; }
    | expr  { $ret.exprs.add( $expr.ret ); }
      DOT? concatenation { $ret.concatenation = $concatenation.ret; }
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
    ;

expr returns [Expr ret]
    : (ID LPAREN functionArgs? RPAREN) { $ret = new Expr($ID.text, true, $functionArgs.ctx != null ? $functionArgs.ret : List.of() ); }
    | ID { $ret = new Expr($ID.text, false, List.of() ); }
    ;

concatenation returns [Concatenation ret]
    : LBRACE citems { $ret = new Concatenation( $citems.ret ); } RBRACE
    ;

citems returns [ArrayList<Object> ret = new ArrayList<>()]
    : citem { $ret.add( $citem.ret ); }
        ( PLUS citem { $ret.add( $citem.ret ); } )*
    ;

citem returns [Object ret]
    : ID { $ret = new Expr( $ID.text, false, List.of() ); }
    | DSTRING  { $ret = sdStringToString( $DSTRING.text ); }
    | SSTRING { $ret = sdStringToString( $SSTRING.text ); }
    | DECDIGITS { $ret = new NumericLiteral( $DECDIGITS.text ); }
    | FLOAT { $ret = new NumericLiteral( $FLOAT.text ); }
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
    | MINUS
    ;
    
