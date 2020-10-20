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
	: t=text { $ast = new AstText($t.text); }
	| expression {
        var lexerExp = new TemplateLexerExpression( CharStreams.fromString( $expression.content ) );
        var grammarExp = new TemplateGrammarExpression( new BufferedTokenStream( lexerExp ), builtInFunction, errorStrategy );
        if( errorStrategy == ErrorStrategy.ERROR ) {
            lexerExp.addErrorListener( ThrowingErrorListener.INSTANCE );
            grammarExp.addErrorListener( ThrowingErrorListener.INSTANCE );
        }
        
	    try { 
            $ast = new AstExpression(grammarExp.expression( $parentType ).ast.top, $expression.content);
        } catch ( Exception e ) {
            throw new TemplateException( $expression.content, e ); 
        }
	 }
	;

text
    : TEXT+
    ;

expression returns [String content]
    : STARTEXPR expressionContent RBRACE { $content = $expressionContent.text; }
    ;

expressionContent
    : (EXPRESSION|LBRACE|RBRACE)+
    ;
