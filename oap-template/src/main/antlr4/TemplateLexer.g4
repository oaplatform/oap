lexer grammar TemplateLexer;

import TemplateLexerBasic; 

fragment StartEscExpr   : '$${'  ;
fragment StartExpr      : '${'  ;




STARTESCEXPR: StartEscExpr -> pushMode(Expression)  ;   
STARTEXPR   : StartExpr -> pushMode(Expression)     ;   

TEXT		: .                                     ;

mode Expression;

LBRACE      : LBrace -> pushMode(Expression)        ;
RBRACE		: RBrace -> popMode                     ;

EXPRESSION  : .                                     ;
