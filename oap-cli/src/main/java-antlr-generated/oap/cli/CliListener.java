/*
 * The MIT License (MIT)
 *
 * Copyright (c) Open Application Platform Authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

// Generated from Cli.g4 by ANTLR 4.5

package oap.cli;

import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a template tree produced by
 * {@link CliParser}.
 */
public interface CliListener extends ParseTreeListener {
	/**
	 * Enter a template tree produced by {@link CliParser#parameters}.
	 * @param ctx the template tree
	 */
	void enterParameters(CliParser.ParametersContext ctx);
	/**
	 * Exit a template tree produced by {@link CliParser#parameters}.
	 * @param ctx the template tree
	 */
	void exitParameters(CliParser.ParametersContext ctx);
	/**
	 * Enter a template tree produced by {@link CliParser#parameter}.
	 * @param ctx the template tree
	 */
	void enterParameter(CliParser.ParameterContext ctx);
	/**
	 * Exit a template tree produced by {@link CliParser#parameter}.
	 * @param ctx the template tree
	 */
	void exitParameter(CliParser.ParameterContext ctx);
	/**
	 * Enter a template tree produced by {@link CliParser#value}.
	 * @param ctx the template tree
	 */
	void enterValue(CliParser.ValueContext ctx);
	/**
	 * Exit a template tree produced by {@link CliParser#value}.
	 * @param ctx the template tree
	 */
	void exitValue(CliParser.ValueContext ctx);
}
