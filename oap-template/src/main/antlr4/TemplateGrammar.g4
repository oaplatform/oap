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

template[TemplateType parentType, Map<String,String> aliases] returns [AstRoot rootAst]
	: elements[parentType, aliases] { $rootAst = new AstRoot($parentType); $rootAst.addChildren($elements.list); } EOF
	;

elements[TemplateType parentType, Map<String,String> aliases] returns [ArrayList<Ast> list = new ArrayList<>()]
	: (element[parentType, aliases] { $list.add($element.ast); })*
	;

element[TemplateType parentType, Map<String,String> aliases] returns [Ast ast]
	: t=text { $ast = new AstText($t.text); }
	| comment { $ast = new AstText($comment.text.substring(1));}
	| expression[aliases] {
        var lexerExp = new TemplateLexerExpression( CharStreams.fromString( $expression.content ) );
        var grammarExp = new TemplateGrammarExpression( new BufferedTokenStream( lexerExp ), builtInFunction, errorStrategy );
        if( errorStrategy == ErrorStrategy.ERROR ) {
            lexerExp.addErrorListener( ThrowingErrorListener.INSTANCE );
            grammarExp.addErrorListener( ThrowingErrorListener.INSTANCE );
        }
        
	    try { 
            $ast = new AstExpression(grammarExp.expression( $parentType ).ast.top, $expression.content);
        } catch ( TemplateException e ) {
            throw e;
        } catch ( Exception e ) {
            throw new TemplateException( $expression.content, e );
        }
	 }
	;

text
    : TEXT+
    ;

comment
    : STARTESCEXPR expressionContent RBRACE;


expression[Map<String,String> aliases] returns [String content]
    : STARTEXPR expressionContent RBRACE { 
        $content = $expressionContent.text;
        var alias = aliases.get( $content );
        if( alias != null ) $content = alias;  
    };

expressionContent
    : (EXPRESSION|LBRACE|RBRACE)+
    ;
