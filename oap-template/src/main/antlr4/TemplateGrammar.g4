parser grammar TemplateGrammar;

options {
	language=Java;
	tokenVocab=TemplateLexer;
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
	public TemplateGrammar(TokenStream input, Map<String, List<Method>> builtInFunction, ErrorStrategy errorStrategy) {
		this(input);
		
		this.builtInFunction = builtInFunction;
		this.errorStrategy = errorStrategy;
	}

}

template[TemplateType parentType] returns [AstRoot rootAst]
	: elements[parentType] { $rootAst = new AstRoot($parentType); $rootAst.addChildren($elements.list); } EOF
	;

elements[TemplateType parentType] returns [ArrayList<Ast> list = new ArrayList<>()]
	: (element[parentType] { $list.add($element.ast); })*
	;

element[TemplateType parentType] returns [Ast ast]
	: t=text { $ast = new AstText($t.text); $ast.addChild(new AstPrint($ast.type, null)); }
	| expression[parentType] { $ast = $expression.ast.top; }
	;

text
    : TEXT+
    ;

expression[TemplateType parentType] returns [MaxMin ast]
    : STARTEXPR exps[parentType] { $ast = $exps.ast; } orExps[parentType, $ast] { $ast = $orExps.ast; } defaultValue? function? {
        if( $function.ctx != null ) {
          $ast.addToBottomChildrenAndSet( $function.func );
        }

        $ast.addLeafs( () -> getAst($ast.bottom.type, null, false, $defaultValue.ctx != null ? $defaultValue.v : null) );
      } RBRACE
    ;

defaultValue returns [String v]
    : DQUESTION defaultValueType { $v = $defaultValueType.v; }
    ;

defaultValueType returns [String v]
    : SSTRING { $v = sStringToDString( $SSTRING.text ); }
    | DSTRING { $v = $DSTRING.text; }
    | DECDIGITS { $v = $DECDIGITS.text; }
    | FLOAT { $v = $FLOAT.text; }
    ; 

function returns [Ast func]
    : SEMI ID LPAREN functionArgs? RPAREN { $func = getFunction( $ID.text, $functionArgs.ctx != null ? $functionArgs.ret : List.of() ); }
    ;

functionArgs returns [ArrayList<String> ret = new ArrayList<>()]
    : functionArg { $ret.add( $functionArg.ret ); } ( COMMA functionArg { $ret.add( $functionArg.ret ); } )*
    ;

functionArg returns [String ret]
    : DECDIGITS { $ret = $DECDIGITS.text; }
    | SSTRING { $ret = sStringToDString( $SSTRING.text ); }
    | DSTRING { $ret = $DSTRING.text; }
    ;

orExps [TemplateType parentType, MaxMin firstAst] returns [MaxMin ast]
    locals [ArrayList<MaxMin> list = new ArrayList<>();]
    : (PIPE exps[parentType] { $list.add(firstAst); $list.add($exps.ast); } ( PIPE exps[parentType] {$list.add($exps.ast);})*) {
        if( $list.isEmpty() ) $ast = firstAst;
        else {
            var or = new AstOr(parentType);
            for( var item : $list) {
              item.addToBottomChildrenAndSet(getAst(item.bottom.type, null, false));
            }
            or.addTry($list);
            $ast = new MaxMin(or);
        }    
    }
    | { $ast = firstAst; }
    ;

exps [TemplateType parentType] returns [MaxMin ast]
    : (exp[parentType] { $ast = $exp.ast; }  
        (DOT exp[$ast.bottom.type] {$ast.addToBottomChildrenAndSet($exp.ast);})*)
        DOT? concatenation[parentType]? { if( $concatenation.ctx != null ) $ast.addToBottomChildrenAndSet( $concatenation.ast ); }
        math[parentType]? { if( $math.ctx != null ) $ast.addToBottomChildrenAndSet( $math.ast ); }
    | concatenation[parentType] { $ast = new MaxMin( $concatenation.ast ); }
    ;

exp[TemplateType parentType] returns [MaxMin ast]
    : (ID LPAREN RPAREN) { $ast = getAst($parentType, $ID.text, true); }
    | ID { $ast = getAst($parentType, $ID.text, false); }
    ;

concatenation[TemplateType parentType] returns [AstConcatenation ast]
    : LBRACE citems[parentType] { $ast = new AstConcatenation(parentType, $citems.list); } RBRACE
    ;

citems[TemplateType parentType] returns [ArrayList<Ast> list = new ArrayList<Ast>()]
    : citem[parentType] { $list.add($citem.ast.top); $citem.ast.addToBottomChildrenAndSet(getAst($citem.ast.bottom.type, null, false)); } 
        ( COMMA citem[parentType] { $list.add($citem.ast.top); $citem.ast.addToBottomChildrenAndSet(getAst($citem.ast.bottom.type, null, false)); } )*
    ;
    
citem[TemplateType parentType] returns [MaxMin ast]
    : ID { $ast = getAst($parentType, $ID.text, false); }
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
    
