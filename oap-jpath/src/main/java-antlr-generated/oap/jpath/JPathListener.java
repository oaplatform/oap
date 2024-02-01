// Generated from JPath.g4 by ANTLR 4.13.0

package oap.jpath;

import java.util.List;
import java.lang.Number;
import java.util.ArrayList;
import oap.util.Pair;
import static oap.util.Pair.__;

import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link JPathParser}.
 */
public interface JPathListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link JPathParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterExpr(JPathParser.ExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link JPathParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitExpr(JPathParser.ExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link JPathParser#path}.
	 * @param ctx the parse tree
	 */
	void enterPath(JPathParser.PathContext ctx);
	/**
	 * Exit a parse tree produced by {@link JPathParser#path}.
	 * @param ctx the parse tree
	 */
	void exitPath(JPathParser.PathContext ctx);
	/**
	 * Enter a parse tree produced by {@link JPathParser#variableDeclaratorId}.
	 * @param ctx the parse tree
	 */
	void enterVariableDeclaratorId(JPathParser.VariableDeclaratorIdContext ctx);
	/**
	 * Exit a parse tree produced by {@link JPathParser#variableDeclaratorId}.
	 * @param ctx the parse tree
	 */
	void exitVariableDeclaratorId(JPathParser.VariableDeclaratorIdContext ctx);
	/**
	 * Enter a parse tree produced by {@link JPathParser#array}.
	 * @param ctx the parse tree
	 */
	void enterArray(JPathParser.ArrayContext ctx);
	/**
	 * Exit a parse tree produced by {@link JPathParser#array}.
	 * @param ctx the parse tree
	 */
	void exitArray(JPathParser.ArrayContext ctx);
	/**
	 * Enter a parse tree produced by {@link JPathParser#method}.
	 * @param ctx the parse tree
	 */
	void enterMethod(JPathParser.MethodContext ctx);
	/**
	 * Exit a parse tree produced by {@link JPathParser#method}.
	 * @param ctx the parse tree
	 */
	void exitMethod(JPathParser.MethodContext ctx);
	/**
	 * Enter a parse tree produced by {@link JPathParser#methodParameters}.
	 * @param ctx the parse tree
	 */
	void enterMethodParameters(JPathParser.MethodParametersContext ctx);
	/**
	 * Exit a parse tree produced by {@link JPathParser#methodParameters}.
	 * @param ctx the parse tree
	 */
	void exitMethodParameters(JPathParser.MethodParametersContext ctx);
	/**
	 * Enter a parse tree produced by {@link JPathParser#methodParameter}.
	 * @param ctx the parse tree
	 */
	void enterMethodParameter(JPathParser.MethodParameterContext ctx);
	/**
	 * Exit a parse tree produced by {@link JPathParser#methodParameter}.
	 * @param ctx the parse tree
	 */
	void exitMethodParameter(JPathParser.MethodParameterContext ctx);
	/**
	 * Enter a parse tree produced by {@link JPathParser#identifier}.
	 * @param ctx the parse tree
	 */
	void enterIdentifier(JPathParser.IdentifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link JPathParser#identifier}.
	 * @param ctx the parse tree
	 */
	void exitIdentifier(JPathParser.IdentifierContext ctx);
}