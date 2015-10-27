// Generated from Cli.g4 by ANTLR 4.5

package oap.cli;

import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link CliParser}.
 */
public interface CliListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link CliParser#parameters}.
	 * @param ctx the parse tree
	 */
	void enterParameters(CliParser.ParametersContext ctx);
	/**
	 * Exit a parse tree produced by {@link CliParser#parameters}.
	 * @param ctx the parse tree
	 */
	void exitParameters(CliParser.ParametersContext ctx);
	/**
	 * Enter a parse tree produced by {@link CliParser#parameter}.
	 * @param ctx the parse tree
	 */
	void enterParameter(CliParser.ParameterContext ctx);
	/**
	 * Exit a parse tree produced by {@link CliParser#parameter}.
	 * @param ctx the parse tree
	 */
	void exitParameter(CliParser.ParameterContext ctx);
	/**
	 * Enter a parse tree produced by {@link CliParser#value}.
	 * @param ctx the parse tree
	 */
	void enterValue(CliParser.ValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link CliParser#value}.
	 * @param ctx the parse tree
	 */
	void exitValue(CliParser.ValueContext ctx);
}
