lexer grammar TemplateLexer;

import TemplateLexerBasic; 

fragment StartEscExpr   : '$${'  ;
fragment StartExpr      : '${'  ;
fragment StartExpr2     : '{{'  ;
fragment EndExpr2       : '}}'  ;




STARTBLOCKIF   : '{{%' [ \t]* 'if' [ \t]+    -> pushMode(BlockIfContent) ;
STARTBLOCKELSE : '{{%' [ \t]* 'else' [ \t]* '}}' ;
STARTBLOCKEND  : '{{%' [ \t]* 'end' [ \t]* '}}' ;

STARTESCEXPR: StartEscExpr -> pushMode(Expression)              ;
STARTEXPR   : StartExpr -> pushMode(Expression)                 ;
STARTEXPR2   : StartExpr2 -> pushMode(Expression2)              ;

TEXT		: .                                                 ;

mode Expression;

LBRACE      : LBrace -> pushMode(Expression)                    ;
RBRACE		: RBrace -> popMode                                 ;

EXPRESSION  : .                                                 ;

mode Expression2;

LBRACE2     : LBrace -> pushMode(Expression)                    ;
RBRACE2		: EndExpr2-> popMode                                ;

EXPRESSION2  : .                                                ;

mode BlockIfContent;
BLOCK_IF_CONTENT : ~[}]+ ;
BLOCK_IF_RBRACE  : '}}' -> popMode ;
