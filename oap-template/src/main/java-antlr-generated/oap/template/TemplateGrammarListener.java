// Generated from java-escape by ANTLR 4.11.1

package oap.template;

import oap.template.tree.*;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link TemplateGrammar}.
 */
public interface TemplateGrammarListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link TemplateGrammar#elements}.
	 * @param ctx the parse tree
	 */
	void enterElements(TemplateGrammar.ElementsContext ctx);
	/**
	 * Exit a parse tree produced by {@link TemplateGrammar#elements}.
	 * @param ctx the parse tree
	 */
	void exitElements(TemplateGrammar.ElementsContext ctx);
	/**
	 * Enter a parse tree produced by {@link TemplateGrammar#element}.
	 * @param ctx the parse tree
	 */
	void enterElement(TemplateGrammar.ElementContext ctx);
	/**
	 * Exit a parse tree produced by {@link TemplateGrammar#element}.
	 * @param ctx the parse tree
	 */
	void exitElement(TemplateGrammar.ElementContext ctx);
	/**
	 * Enter a parse tree produced by {@link TemplateGrammar#text}.
	 * @param ctx the parse tree
	 */
	void enterText(TemplateGrammar.TextContext ctx);
	/**
	 * Exit a parse tree produced by {@link TemplateGrammar#text}.
	 * @param ctx the parse tree
	 */
	void exitText(TemplateGrammar.TextContext ctx);
	/**
	 * Enter a parse tree produced by {@link TemplateGrammar#comment}.
	 * @param ctx the parse tree
	 */
	void enterComment(TemplateGrammar.CommentContext ctx);
	/**
	 * Exit a parse tree produced by {@link TemplateGrammar#comment}.
	 * @param ctx the parse tree
	 */
	void exitComment(TemplateGrammar.CommentContext ctx);
	/**
	 * Enter a parse tree produced by {@link TemplateGrammar#expression}.
	 * @param ctx the parse tree
	 */
	void enterExpression(TemplateGrammar.ExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link TemplateGrammar#expression}.
	 * @param ctx the parse tree
	 */
	void exitExpression(TemplateGrammar.ExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link TemplateGrammar#expressionContent}.
	 * @param ctx the parse tree
	 */
	void enterExpressionContent(TemplateGrammar.ExpressionContentContext ctx);
	/**
	 * Exit a parse tree produced by {@link TemplateGrammar#expressionContent}.
	 * @param ctx the parse tree
	 */
	void exitExpressionContent(TemplateGrammar.ExpressionContentContext ctx);
}