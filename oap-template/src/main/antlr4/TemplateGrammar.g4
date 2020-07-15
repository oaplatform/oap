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
	: es=elements[parentType] { $rootAst = new AstRoot($parentType); $rootAst.addChildren($es.list); } EOF
	;

elements[TemplateType parentType] returns [ArrayList<Ast> list = new ArrayList<>()]
	: (e=element[parentType] { $list.add($e.ast); })*
	;

element[TemplateType parentType] returns [Ast ast]
	: t=text { $ast = new AstText($t.text); $ast.addChild(new AstPrint($ast.type, null)); }
	| e=expression[parentType] { $ast = $e.ast.top; }
	;

text
    : TEXT+
    ;

expression[TemplateType parentType] returns [MinMax ast]
    : STARTEXPR exps[parentType] { $ast = $exps.ast; } orExps[parentType, $ast] { $ast = $orExps.ast; } defaultValue? function? {
        if( $function.ctx != null ) {
          $ast.addToBottomChildrenAndSet( $function.func );
        }

        var ap = getAst($ast.bottom.type, null, false, $defaultValue.ctx != null ? $defaultValue.v : null);
        $ast.addToBottomChildrenAndSet( ap );
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
    : fa=functionArg { $ret.add( $fa.ret ); } ( COMMA nfa=functionArg { $ret.add( $nfa.ret ); } )*
    ;

functionArg returns [String ret]
    : DECDIGITS { $ret = $DECDIGITS.text; }
    | SSTRING { $ret = sStringToDString( $SSTRING.text ); }
    | DSTRING { $ret = $DSTRING.text; }
    ;

orExps [TemplateType parentType, MinMax firstAst] returns [MinMax ast]
    locals [ArrayList<MinMax> list = new ArrayList<>();]
    : (PIPE e1=exps[parentType] { $list.add(firstAst); $list.add($e1.ast); } ( PIPE e2=exps[parentType] {$list.add($e2.ast);})*) {
        if( $list.isEmpty() ) $ast = firstAst;
        else {
            var or = new AstOr(parentType);
            for( var item : $list) {
              item.addToBottomChildrenAndSet(getAst(item.bottom.type, null, false));
            }
            or.addTry($list);
            $ast = new MinMax(or);
        }    
    }
    | { $ast = firstAst; }
    ;

exps [TemplateType parentType] returns [MinMax ast]
    : (id=exp[parentType] { $ast = $id.ast; }  (DOT nid=exp[$ast.bottom.type] {$ast.addToBottomChildrenAndSet($nid.ast);})*)
    ;

exp[TemplateType parentType] returns [MinMax ast]
    : (id=ID LPAREN RPAREN) { $ast = getAst($parentType, $id.text, true); }
    | id=ID { $ast = getAst($parentType, $id.text, false); }
    ;
