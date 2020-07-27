lexer grammar TemplateLexer;

import TemplateLexerBasic; 

fragment StartExpr      : '${'  ;




STARTEXPR   : StartExpr -> pushMode(Expression)     ;   

TEXT		: .                                     ;

mode Expression;

LBRACE      : LBrace -> pushMode(Expression)        ;
RBRACE		: RBrace -> popMode                     ;

EXPRESSION  : .                                     ;
