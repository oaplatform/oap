parser grammar TemplateGrammar;

options {
	language=Java;
	tokenVocab=TemplateLexer;
	superClass = TemplateGrammarAdaptor;
}

@header {
package oap.template;

import oap.template.tree.*;
import oap.template.tree.BlockIfElement;

import org.apache.commons.lang3.StringUtils;
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

elements[Map<String,String> aliases] returns [Elements ret = new Elements()]
	: (element[aliases] { $ret.elements.add( $element.ret ); })* EOF
	;

element[Map<String,String> aliases] returns [Element ret]
	: t=text { $ret = new TextElement( $t.text ); }
	| comment { $ret = new TextElement( $comment.text.substring(1) ); }
	| expression[aliases] { $ret = new ExpressionElement( $expression.ret ); }
	| blockIfElement[aliases] { $ret = $blockIfElement.ret; }
	;

blockIfElement[Map<String,String> aliases] returns [BlockIfElement ret]
	: STARTBLOCKIF BLOCK_IF_CONTENT BLOCK_IF_RBRACE
	  thenBranch=blockBody[aliases]
	  ( STARTBLOCKELSE elseBranch=blockBody[aliases] )?
	  STARTBLOCKEND
	  {
	      $ret = new BlockIfElement(
	          StringUtils.trim( $BLOCK_IF_CONTENT.text ),
	          $thenBranch.ret,
	          $elseBranch.ctx != null ? $elseBranch.ret : null
	      );
	  }
	;

blockBody[Map<String,String> aliases] returns [Elements ret = new Elements()]
	: ( element[aliases]
	      { if( $element.ret != null ) $ret.elements.add( $element.ret ); }
	  )*
	;

text
    : TEXT+
    ;

comment
    : STARTESCEXPR expressionContent RBRACE;


expression[Map<String,String> aliases] returns [String ret]
    : (STARTEXPR|STARTEXPR2) expressionContent (RBRACE|RBRACE2) {
        $ret = StringUtils.trim( $expressionContent.text );
        String alias = aliases.get( $ret );
        if( alias != null ) $ret = alias;
    };

expressionContent
    : (EXPRESSION|EXPRESSION2|LBRACE|RBRACE|LBRACE2|RBRACE2)+
    ;
