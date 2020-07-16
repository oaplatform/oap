// Generated from TemplateGrammar.g4 by ANTLR 4.8

package oap.template;

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
	 * Enter a parse tree produced by {@link TemplateGrammar#template}.
	 * @param ctx the parse tree
	 */
	void enterTemplate(TemplateGrammar.TemplateContext ctx);
	/**
	 * Exit a parse tree produced by {@link TemplateGrammar#template}.
	 * @param ctx the parse tree
	 */
	void exitTemplate(TemplateGrammar.TemplateContext ctx);
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
	 * Enter a parse tree produced by {@link TemplateGrammar#defaultValue}.
	 * @param ctx the parse tree
	 */
	void enterDefaultValue(TemplateGrammar.DefaultValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link TemplateGrammar#defaultValue}.
	 * @param ctx the parse tree
	 */
	void exitDefaultValue(TemplateGrammar.DefaultValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link TemplateGrammar#defaultValueType}.
	 * @param ctx the parse tree
	 */
	void enterDefaultValueType(TemplateGrammar.DefaultValueTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link TemplateGrammar#defaultValueType}.
	 * @param ctx the parse tree
	 */
	void exitDefaultValueType(TemplateGrammar.DefaultValueTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link TemplateGrammar#function}.
	 * @param ctx the parse tree
	 */
	void enterFunction(TemplateGrammar.FunctionContext ctx);
	/**
	 * Exit a parse tree produced by {@link TemplateGrammar#function}.
	 * @param ctx the parse tree
	 */
	void exitFunction(TemplateGrammar.FunctionContext ctx);
	/**
	 * Enter a parse tree produced by {@link TemplateGrammar#functionArgs}.
	 * @param ctx the parse tree
	 */
	void enterFunctionArgs(TemplateGrammar.FunctionArgsContext ctx);
	/**
	 * Exit a parse tree produced by {@link TemplateGrammar#functionArgs}.
	 * @param ctx the parse tree
	 */
	void exitFunctionArgs(TemplateGrammar.FunctionArgsContext ctx);
	/**
	 * Enter a parse tree produced by {@link TemplateGrammar#functionArg}.
	 * @param ctx the parse tree
	 */
	void enterFunctionArg(TemplateGrammar.FunctionArgContext ctx);
	/**
	 * Exit a parse tree produced by {@link TemplateGrammar#functionArg}.
	 * @param ctx the parse tree
	 */
	void exitFunctionArg(TemplateGrammar.FunctionArgContext ctx);
	/**
	 * Enter a parse tree produced by {@link TemplateGrammar#orExps}.
	 * @param ctx the parse tree
	 */
	void enterOrExps(TemplateGrammar.OrExpsContext ctx);
	/**
	 * Exit a parse tree produced by {@link TemplateGrammar#orExps}.
	 * @param ctx the parse tree
	 */
	void exitOrExps(TemplateGrammar.OrExpsContext ctx);
	/**
	 * Enter a parse tree produced by {@link TemplateGrammar#exps}.
	 * @param ctx the parse tree
	 */
	void enterExps(TemplateGrammar.ExpsContext ctx);
	/**
	 * Exit a parse tree produced by {@link TemplateGrammar#exps}.
	 * @param ctx the parse tree
	 */
	void exitExps(TemplateGrammar.ExpsContext ctx);
	/**
	 * Enter a parse tree produced by {@link TemplateGrammar#exp}.
	 * @param ctx the parse tree
	 */
	void enterExp(TemplateGrammar.ExpContext ctx);
	/**
	 * Exit a parse tree produced by {@link TemplateGrammar#exp}.
	 * @param ctx the parse tree
	 */
	void exitExp(TemplateGrammar.ExpContext ctx);
	/**
	 * Enter a parse tree produced by {@link TemplateGrammar#concatenation}.
	 * @param ctx the parse tree
	 */
	void enterConcatenation(TemplateGrammar.ConcatenationContext ctx);
	/**
	 * Exit a parse tree produced by {@link TemplateGrammar#concatenation}.
	 * @param ctx the parse tree
	 */
	void exitConcatenation(TemplateGrammar.ConcatenationContext ctx);
	/**
	 * Enter a parse tree produced by {@link TemplateGrammar#citems}.
	 * @param ctx the parse tree
	 */
	void enterCitems(TemplateGrammar.CitemsContext ctx);
	/**
	 * Exit a parse tree produced by {@link TemplateGrammar#citems}.
	 * @param ctx the parse tree
	 */
	void exitCitems(TemplateGrammar.CitemsContext ctx);
	/**
	 * Enter a parse tree produced by {@link TemplateGrammar#citem}.
	 * @param ctx the parse tree
	 */
	void enterCitem(TemplateGrammar.CitemContext ctx);
	/**
	 * Exit a parse tree produced by {@link TemplateGrammar#citem}.
	 * @param ctx the parse tree
	 */
	void exitCitem(TemplateGrammar.CitemContext ctx);
}