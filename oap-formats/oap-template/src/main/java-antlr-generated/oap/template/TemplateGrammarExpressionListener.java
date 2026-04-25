// Generated from TemplateGrammarExpression.g4 by ANTLR 4.13.0

package oap.template;

import oap.template.tree.*;
import oap.template.tree.Math;
import oap.template.tree.WithCondition;
import oap.template.tree.ConditionExpr;
import oap.template.tree.FieldConditionExpr;
import oap.template.tree.AndConditionExpr;
import oap.template.tree.OrConditionExpr;
import oap.template.tree.NotConditionExpr;
import oap.template.tree.CompareConditionExpr;
import oap.template.tree.CompareValue;
import oap.template.tree.LiteralCompareValue;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import oap.util.Lists;


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
	 * Enter a parse tree produced by {@link TemplateGrammarExpression#ifCode}.
	 * @param ctx the parse tree
	 */
	void enterIfCode(TemplateGrammarExpression.IfCodeContext ctx);
	/**
	 * Exit a parse tree produced by {@link TemplateGrammarExpression#ifCode}.
	 * @param ctx the parse tree
	 */
	void exitIfCode(TemplateGrammarExpression.IfCodeContext ctx);
	/**
	 * Enter a parse tree produced by {@link TemplateGrammarExpression#withCode}.
	 * @param ctx the parse tree
	 */
	void enterWithCode(TemplateGrammarExpression.WithCodeContext ctx);
	/**
	 * Exit a parse tree produced by {@link TemplateGrammarExpression#withCode}.
	 * @param ctx the parse tree
	 */
	void exitWithCode(TemplateGrammarExpression.WithCodeContext ctx);
	/**
	 * Enter a parse tree produced by {@link TemplateGrammarExpression#concatBody}.
	 * @param ctx the parse tree
	 */
	void enterConcatBody(TemplateGrammarExpression.ConcatBodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link TemplateGrammarExpression#concatBody}.
	 * @param ctx the parse tree
	 */
	void exitConcatBody(TemplateGrammarExpression.ConcatBodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link TemplateGrammarExpression#exprsCode}.
	 * @param ctx the parse tree
	 */
	void enterExprsCode(TemplateGrammarExpression.ExprsCodeContext ctx);
	/**
	 * Exit a parse tree produced by {@link TemplateGrammarExpression#exprsCode}.
	 * @param ctx the parse tree
	 */
	void exitExprsCode(TemplateGrammarExpression.ExprsCodeContext ctx);
	/**
	 * Enter a parse tree produced by {@link TemplateGrammarExpression#ifCondition}.
	 * @param ctx the parse tree
	 */
	void enterIfCondition(TemplateGrammarExpression.IfConditionContext ctx);
	/**
	 * Exit a parse tree produced by {@link TemplateGrammarExpression#ifCondition}.
	 * @param ctx the parse tree
	 */
	void exitIfCondition(TemplateGrammarExpression.IfConditionContext ctx);
	/**
	 * Enter a parse tree produced by {@link TemplateGrammarExpression#conditionOr}.
	 * @param ctx the parse tree
	 */
	void enterConditionOr(TemplateGrammarExpression.ConditionOrContext ctx);
	/**
	 * Exit a parse tree produced by {@link TemplateGrammarExpression#conditionOr}.
	 * @param ctx the parse tree
	 */
	void exitConditionOr(TemplateGrammarExpression.ConditionOrContext ctx);
	/**
	 * Enter a parse tree produced by {@link TemplateGrammarExpression#conditionAnd}.
	 * @param ctx the parse tree
	 */
	void enterConditionAnd(TemplateGrammarExpression.ConditionAndContext ctx);
	/**
	 * Exit a parse tree produced by {@link TemplateGrammarExpression#conditionAnd}.
	 * @param ctx the parse tree
	 */
	void exitConditionAnd(TemplateGrammarExpression.ConditionAndContext ctx);
	/**
	 * Enter a parse tree produced by {@link TemplateGrammarExpression#conditionNot}.
	 * @param ctx the parse tree
	 */
	void enterConditionNot(TemplateGrammarExpression.ConditionNotContext ctx);
	/**
	 * Exit a parse tree produced by {@link TemplateGrammarExpression#conditionNot}.
	 * @param ctx the parse tree
	 */
	void exitConditionNot(TemplateGrammarExpression.ConditionNotContext ctx);
	/**
	 * Enter a parse tree produced by {@link TemplateGrammarExpression#conditionAtom}.
	 * @param ctx the parse tree
	 */
	void enterConditionAtom(TemplateGrammarExpression.ConditionAtomContext ctx);
	/**
	 * Exit a parse tree produced by {@link TemplateGrammarExpression#conditionAtom}.
	 * @param ctx the parse tree
	 */
	void exitConditionAtom(TemplateGrammarExpression.ConditionAtomContext ctx);
	/**
	 * Enter a parse tree produced by {@link TemplateGrammarExpression#compareRhs}.
	 * @param ctx the parse tree
	 */
	void enterCompareRhs(TemplateGrammarExpression.CompareRhsContext ctx);
	/**
	 * Exit a parse tree produced by {@link TemplateGrammarExpression#compareRhs}.
	 * @param ctx the parse tree
	 */
	void exitCompareRhs(TemplateGrammarExpression.CompareRhsContext ctx);
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
	 * Enter a parse tree produced by {@link TemplateGrammarExpression#orExprs}.
	 * @param ctx the parse tree
	 */
	void enterOrExprs(TemplateGrammarExpression.OrExprsContext ctx);
	/**
	 * Exit a parse tree produced by {@link TemplateGrammarExpression#orExprs}.
	 * @param ctx the parse tree
	 */
	void exitOrExprs(TemplateGrammarExpression.OrExprsContext ctx);
	/**
	 * Enter a parse tree produced by {@link TemplateGrammarExpression#exprs}.
	 * @param ctx the parse tree
	 */
	void enterExprs(TemplateGrammarExpression.ExprsContext ctx);
	/**
	 * Exit a parse tree produced by {@link TemplateGrammarExpression#exprs}.
	 * @param ctx the parse tree
	 */
	void exitExprs(TemplateGrammarExpression.ExprsContext ctx);
	/**
	 * Enter a parse tree produced by {@link TemplateGrammarExpression#expr}.
	 * @param ctx the parse tree
	 */
	void enterExpr(TemplateGrammarExpression.ExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link TemplateGrammarExpression#expr}.
	 * @param ctx the parse tree
	 */
	void exitExpr(TemplateGrammarExpression.ExprContext ctx);
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