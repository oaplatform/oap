// Generated from TemplateGrammarExpression.g4 by ANTLR 4.9.2

package oap.template;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link TemplateGrammarExpression}.
 */
public interface TemplateGrammarExpressionListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link TemplateGrammarExpression#expression}.
	 * @param ctx the parse tree
	 */
	void enterExpression(TemplateGrammarExpression.ExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link TemplateGrammarExpression#expression}.
	 * @param ctx the parse tree
	 */
	void exitExpression(TemplateGrammarExpression.ExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link TemplateGrammarExpression#defaultValue}.
	 * @param ctx the parse tree
	 */
	void enterDefaultValue(TemplateGrammarExpression.DefaultValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link TemplateGrammarExpression#defaultValue}.
	 * @param ctx the parse tree
	 */
	void exitDefaultValue(TemplateGrammarExpression.DefaultValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link TemplateGrammarExpression#defaultValueType}.
	 * @param ctx the parse tree
	 */
	void enterDefaultValueType(TemplateGrammarExpression.DefaultValueTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link TemplateGrammarExpression#defaultValueType}.
	 * @param ctx the parse tree
	 */
	void exitDefaultValueType(TemplateGrammarExpression.DefaultValueTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link TemplateGrammarExpression#longRule}.
	 * @param ctx the parse tree
	 */
	void enterLongRule(TemplateGrammarExpression.LongRuleContext ctx);
	/**
	 * Exit a parse tree produced by {@link TemplateGrammarExpression#longRule}.
	 * @param ctx the parse tree
	 */
	void exitLongRule(TemplateGrammarExpression.LongRuleContext ctx);
	/**
	 * Enter a parse tree produced by {@link TemplateGrammarExpression#function}.
	 * @param ctx the parse tree
	 */
	void enterFunction(TemplateGrammarExpression.FunctionContext ctx);
	/**
	 * Exit a parse tree produced by {@link TemplateGrammarExpression#function}.
	 * @param ctx the parse tree
	 */
	void exitFunction(TemplateGrammarExpression.FunctionContext ctx);
	/**
	 * Enter a parse tree produced by {@link TemplateGrammarExpression#functionArgs}.
	 * @param ctx the parse tree
	 */
	void enterFunctionArgs(TemplateGrammarExpression.FunctionArgsContext ctx);
	/**
	 * Exit a parse tree produced by {@link TemplateGrammarExpression#functionArgs}.
	 * @param ctx the parse tree
	 */
	void exitFunctionArgs(TemplateGrammarExpression.FunctionArgsContext ctx);
	/**
	 * Enter a parse tree produced by {@link TemplateGrammarExpression#functionArg}.
	 * @param ctx the parse tree
	 */
	void enterFunctionArg(TemplateGrammarExpression.FunctionArgContext ctx);
	/**
	 * Exit a parse tree produced by {@link TemplateGrammarExpression#functionArg}.
	 * @param ctx the parse tree
	 */
	void exitFunctionArg(TemplateGrammarExpression.FunctionArgContext ctx);
	/**
	 * Enter a parse tree produced by {@link TemplateGrammarExpression#orExps}.
	 * @param ctx the parse tree
	 */
	void enterOrExps(TemplateGrammarExpression.OrExpsContext ctx);
	/**
	 * Exit a parse tree produced by {@link TemplateGrammarExpression#orExps}.
	 * @param ctx the parse tree
	 */
	void exitOrExps(TemplateGrammarExpression.OrExpsContext ctx);
	/**
	 * Enter a parse tree produced by {@link TemplateGrammarExpression#exps}.
	 * @param ctx the parse tree
	 */
	void enterExps(TemplateGrammarExpression.ExpsContext ctx);
	/**
	 * Exit a parse tree produced by {@link TemplateGrammarExpression#exps}.
	 * @param ctx the parse tree
	 */
	void exitExps(TemplateGrammarExpression.ExpsContext ctx);
	/**
	 * Enter a parse tree produced by {@link TemplateGrammarExpression#exp}.
	 * @param ctx the parse tree
	 */
	void enterExp(TemplateGrammarExpression.ExpContext ctx);
	/**
	 * Exit a parse tree produced by {@link TemplateGrammarExpression#exp}.
	 * @param ctx the parse tree
	 */
	void exitExp(TemplateGrammarExpression.ExpContext ctx);
	/**
	 * Enter a parse tree produced by {@link TemplateGrammarExpression#concatenation}.
	 * @param ctx the parse tree
	 */
	void enterConcatenation(TemplateGrammarExpression.ConcatenationContext ctx);
	/**
	 * Exit a parse tree produced by {@link TemplateGrammarExpression#concatenation}.
	 * @param ctx the parse tree
	 */
	void exitConcatenation(TemplateGrammarExpression.ConcatenationContext ctx);
	/**
	 * Enter a parse tree produced by {@link TemplateGrammarExpression#citems}.
	 * @param ctx the parse tree
	 */
	void enterCitems(TemplateGrammarExpression.CitemsContext ctx);
	/**
	 * Exit a parse tree produced by {@link TemplateGrammarExpression#citems}.
	 * @param ctx the parse tree
	 */
	void exitCitems(TemplateGrammarExpression.CitemsContext ctx);
	/**
	 * Enter a parse tree produced by {@link TemplateGrammarExpression#citem}.
	 * @param ctx the parse tree
	 */
	void enterCitem(TemplateGrammarExpression.CitemContext ctx);
	/**
	 * Exit a parse tree produced by {@link TemplateGrammarExpression#citem}.
	 * @param ctx the parse tree
	 */
	void exitCitem(TemplateGrammarExpression.CitemContext ctx);
	/**
	 * Enter a parse tree produced by {@link TemplateGrammarExpression#math}.
	 * @param ctx the parse tree
	 */
	void enterMath(TemplateGrammarExpression.MathContext ctx);
	/**
	 * Exit a parse tree produced by {@link TemplateGrammarExpression#math}.
	 * @param ctx the parse tree
	 */
	void exitMath(TemplateGrammarExpression.MathContext ctx);
	/**
	 * Enter a parse tree produced by {@link TemplateGrammarExpression#number}.
	 * @param ctx the parse tree
	 */
	void enterNumber(TemplateGrammarExpression.NumberContext ctx);
	/**
	 * Exit a parse tree produced by {@link TemplateGrammarExpression#number}.
	 * @param ctx the parse tree
	 */
	void exitNumber(TemplateGrammarExpression.NumberContext ctx);
	/**
	 * Enter a parse tree produced by {@link TemplateGrammarExpression#mathOperation}.
	 * @param ctx the parse tree
	 */
	void enterMathOperation(TemplateGrammarExpression.MathOperationContext ctx);
	/**
	 * Exit a parse tree produced by {@link TemplateGrammarExpression#mathOperation}.
	 * @param ctx the parse tree
	 */
	void exitMathOperation(TemplateGrammarExpression.MathOperationContext ctx);
}