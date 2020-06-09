// Generated from JPath.g4 by ANTLR 4.8

package oap.jpath;

import java.util.List;
import java.util.ArrayList;

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
	 * Enter a parse tree produced by {@link JPathParser#methodName}.
	 * @param ctx the parse tree
	 */
	void enterMethodName(JPathParser.MethodNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link JPathParser#methodName}.
	 * @param ctx the parse tree
	 */
	void exitMethodName(JPathParser.MethodNameContext ctx);
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