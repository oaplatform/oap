lexer grammar TemplateLexer;

import TemplateLexerBasic;

@members {
    TemplateConfiguration configuration = TemplateConfiguration.DEFAULT;
    private TemplateConfiguration.Expression _matchedCustomExpr;
    private final java.util.Deque<String> _customSuffixStack = new java.util.ArrayDeque<>();

    private boolean isAtCustomPrefix() {
        for( TemplateConfiguration.Expression expr : configuration.expressions ) {
            if( "{{".equals( expr.prefix ) || "${".equals( expr.prefix ) ) continue;
            if( matchesAhead( expr.prefix ) ) {
                _matchedCustomExpr = expr;
                return true;
            }
        }
        return false;
    }

    private boolean matchesAhead( String text ) {
        for( int i = 0; i < text.length(); i++ ) {
            if( _input.LA( i + 1 ) != text.charAt( i ) ) return false;
        }
        return true;
    }

    private void consumeCustomPrefix() {
        for( int i = 1; i < _matchedCustomExpr.prefix.length(); i++ ) _input.consume();
        _customSuffixStack.push( _matchedCustomExpr.suffix );
    }

    private boolean isAtCustomSuffix() {
        String suffix = _customSuffixStack.peek();
        if( suffix == null ) return false;
        return matchesAhead( suffix );
    }

    private void consumeCustomSuffix() {
        String suffix = _customSuffixStack.pop();
        for( int i = 1; i < suffix.length(); i++ ) _input.consume();
    }
}

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

STARTESCEXPR     : StartEscExpr -> pushMode(Expression)          ;
STARTEXPR        : StartExpr -> pushMode(Expression)             ;
STARTEXPR2_LTRIM : '{{-' -> pushMode(Expression2)               ;
STARTEXPR2       : StartExpr2 -> pushMode(Expression2)           ;

STARTCUSTOMEXPR2 : { isAtCustomPrefix() }? . { consumeCustomPrefix(); } -> type(STARTEXPR2), pushMode(Expression2) ;
TEXT             : .                                                      ;

mode Expression;

LBRACE      : LBrace -> pushMode(Expression)                    ;
RBRACE		: RBrace -> popMode                                 ;

EXPRESSION  : .                                                 ;

mode Expression2;

LBRACE2       : LBrace -> pushMode(Expression)                  ;
RBRACE2_RTRIM : '-}}' -> popMode                                ;
RBRACE2CUSTOM : { isAtCustomSuffix() }? . { consumeCustomSuffix(); } -> type(RBRACE2), popMode ;
RBRACE2       : EndExpr2 -> popMode                             ;

EXPRESSION2   : .                                               ;

mode BlockIfContent;
BLOCK_IF_CONTENT : ~[}]+ ;
BLOCK_IF_RBRACE  : '}}' -> popMode ;
