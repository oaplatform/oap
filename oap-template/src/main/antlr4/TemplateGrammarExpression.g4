parser grammar TemplateGrammarExpression;

options {
	language=Java;
	tokenVocab=TemplateLexerExpression;
	superClass = TemplateGrammarAdaptor;
}

@header {
package oap.template;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
}

@parser::members {
	public TemplateGrammarExpression(TokenStream input, Map<String, List<Method>> builtInFunction, ErrorStrategy errorStrategy) {
		this(input);
		
		this.builtInFunction = builtInFunction;
		this.errorStrategy = errorStrategy;
	}

}

expression[TemplateType parentType] returns [MaxMin ast]
    locals [String comment = null, String castType = null; ]
    : (BLOCK_COMMENT { $comment = $BLOCK_COMMENT.text; })? (CAST_TYPE { $castType = getCastType($CAST_TYPE.text); })? exps[parentType, $castType] { $ast = $exps.ast; } orExps[parentType, $castType, $ast] { $ast = $orExps.ast; } defaultValue? function? {
        if( $function.ctx != null ) {
          $ast.addToBottomChildrenAndSet( $function.func );
        }

        $ast.addLeafs( () -> getAst($ast.bottom.type, null, false, $defaultValue.ctx != null ? $defaultValue.v : null, null ) );

        if( $comment != null ) {
            $ast.setTop( new AstComment( parentType, $comment ) );
        }
      }
    ;
    

defaultValue returns [String v]
    : DQUESTION defaultValueType { $v = $defaultValueType.v; }
    ;

defaultValueType returns [String v]
    : SSTRING { $v = sStringToDString( $SSTRING.text ); }
    | DSTRING { $v = $DSTRING.text; }
    | longRule { $v = $longRule.text; }
    | FLOAT { $v = $FLOAT.text; }
    | BOOLEAN { $v = $BOOLEAN.text; }
    | LBRACK RBRACK { $v = "java.util.List.of()"; }
    ; 

longRule
    : MINUS? DECDIGITS
    ;

function returns [Ast func]
    : SEMI ID LPAREN functionArgs? RPAREN { $func = getFunction( $ID.text, $functionArgs.ctx != null ? $functionArgs.ret : List.of() ); }
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

orExps [TemplateType parentType, String castType, MaxMin firstAst] returns [MaxMin ast]
    locals [ArrayList<MaxMin> list = new ArrayList<>();]
    : (PIPE exps[parentType, castType] { $list.add(firstAst); $list.add($exps.ast); } ( PIPE exps[parentType, castType] {$list.add($exps.ast);})*) {
        if( $list.isEmpty() ) $ast = firstAst;
        else {
            var or = new AstOr(parentType);
            for( var item : $list) {
              item.addToBottomChildrenAndSet(getAst(item.bottom.type, null, false, null));
            }
            or.addTry($list);
            $ast = new MaxMin(or);
        }    
    }
    | { $ast = firstAst; }
    ;

exps [TemplateType parentType, String castType] returns [MaxMin ast]
    : exp[parentType, castType]  { $ast = $exp.ast; }
      math[$ast.bottom.type]? {
        if( $math.ctx != null ) $ast.addToBottomChildrenAndSet( $math.ast );
      }
    | exp[parentType, null]  { $ast = $exp.ast; }
      DOT? concatenation[$ast.bottom.type, castType] {
        $ast.addToBottomChildrenAndSet( $concatenation.ast );
      }
    | exp[parentType, null] { $ast = $exp.ast; }
      (DOT exp[$ast.bottom.type, null] {
        $ast.addToBottomChildrenAndSet($exp.ast);
      })*
      DOT exp[$ast.bottom.type, castType] { $ast.addToBottomChildrenAndSet($exp.ast); }
      math[$ast.bottom.type]? {
        if( $math.ctx != null ) $ast.addToBottomChildrenAndSet( $math.ast );
      }
    | exp[parentType, null] { $ast = $exp.ast; }
      (DOT exp[$ast.bottom.type, null] {
        $ast.addToBottomChildrenAndSet($exp.ast);
      })*
      DOT exp[$ast.bottom.type, null] { $ast.addToBottomChildrenAndSet($exp.ast); }
      DOT? concatenation[$ast.bottom.type, castType] {
        $ast.addToBottomChildrenAndSet( $concatenation.ast );
      }
      math[$ast.bottom.type]? {
        if( $math.ctx != null ) $ast.addToBottomChildrenAndSet( $math.ast );
      }
    | concatenation[parentType, castType] { $ast = new MaxMin( $concatenation.ast ); }
    ;

exp[TemplateType parentType, String castType] returns [MaxMin ast]
    : (ID LPAREN functionArgs? RPAREN) { $ast = getAst($parentType, $ID.text, true, $functionArgs.ctx != null ? $functionArgs.ret : List.of(), castType ); }
    | ID { $ast = getAst($parentType, $ID.text, false, castType ); }
    ;

concatenation[TemplateType parentType, String castType] returns [AstConcatenation ast]
    : LBRACE citems[parentType] {
        try {
        com.google.common.base.Preconditions.checkArgument(  $castType == null || oap.template.LogConfiguration.FieldType.parse( $castType ).equals( new oap.template.LogConfiguration.FieldType( String.class )));
        $ast = new AstConcatenation(parentType, $citems.list);
        } catch ( java.lang.ClassNotFoundException e) {
          throw new TemplateException( e.getMessage(), e );
        }
      } RBRACE
    ;

citems[TemplateType parentType] returns [ArrayList<Ast> list = new ArrayList<Ast>()]
    : citem[parentType] { $list.add($citem.ast.top); $citem.ast.addToBottomChildrenAndSet(getAst($citem.ast.bottom.type, null, false, null)); }
        ( COMMA citem[parentType] { $list.add($citem.ast.top); $citem.ast.addToBottomChildrenAndSet(getAst($citem.ast.bottom.type, null, false, null)); } )*
    ;
    
citem[TemplateType parentType] returns [MaxMin ast]
    : ID { $ast = getAst($parentType, $ID.text, false, null); }
    | DSTRING  { $ast = new MaxMin(new AstText(sdStringToString($DSTRING.text))); }
    | SSTRING { $ast = new MaxMin(new AstText(sdStringToString($SSTRING.text))); }
    | DECDIGITS { $ast = new MaxMin(new AstText(String.valueOf($DECDIGITS.text))); }
    | FLOAT { $ast = new MaxMin(new AstText(String.valueOf($FLOAT.text))); }
    ;

math[TemplateType parentType] returns [MaxMin ast]
    : mathOperation number { $ast = new MaxMin(new AstMath($parentType, $mathOperation.text, $number.text)); }
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
    
