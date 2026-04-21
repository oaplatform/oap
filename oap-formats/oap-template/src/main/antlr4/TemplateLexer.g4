lexer grammar TemplateLexer;

import TemplateLexerBasic; 

fragment StartEscExpr   : '$${'  ;
fragment StartExpr      : '${'  ;
fragment StartExpr2     : '{{'  ;
fragment EndExpr2       : '}}'  ;




STARTBLOCKRANGE    : '{{%' [ \t]* 'range' [ \t]+ -> pushMode(BlockIfContent) ;
STARTBLOCKWITH     : '{{%' [ \t]* 'with' [ \t]+  -> pushMode(BlockIfContent) ;
STARTBLOCKIF_LTRIM : '{{%-' [ \t]* 'if' [ \t]+   -> pushMode(BlockIfContent) ;
STARTBLOCKIF       : '{{%' [ \t]* 'if' [ \t]+    -> pushMode(BlockIfContent) ;
STARTBLOCKELSE     : '{{%' [ \t]* 'else' [ \t]* '}}' ;
STARTBLOCKEND      : '{{%' [ \t]* 'end' [ \t]* '}}' ;

STARTESCEXPR    : StartEscExpr -> pushMode(Expression)          ;
STARTEXPR       : StartExpr -> pushMode(Expression)             ;
STARTEXPR2_LTRIM : '{{-' -> pushMode(Expression2)              ;
STARTEXPR2      : StartExpr2 -> pushMode(Expression2)           ;

TEXT		: .                                                 ;

mode Expression;

LBRACE      : LBrace -> pushMode(Expression)                    ;
RBRACE		: RBrace -> popMode                                 ;

EXPRESSION  : .                                                 ;

mode Expression2;

LBRACE2       : LBrace -> pushMode(Expression)                  ;
RBRACE2_RTRIM : '-}}' -> popMode                                ;
RBRACE2       : EndExpr2 -> popMode                             ;

EXPRESSION2   : .                                               ;

mode BlockIfContent;
BLOCK_IF_CONTENT : ~[}]+ ;
BLOCK_IF_RBRACE  : '}}' -> popMode ;
