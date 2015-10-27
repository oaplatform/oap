/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Volodymyr Kyrychenko <vladimir.kirichenko@gmail.com>
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
package oap.cli;


import oap.testng.AbstractTest;
import oap.util.Pair;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.testng.annotations.Test;

import java.util.List;

import static oap.cli.Option.__;
import static org.testng.Assert.assertEquals;

public class CliParserTest extends AbstractTest {
    @Test
    public void parse() throws Exception {
        CliParser parser = new CliParser(new CommonTokenStream(new CliLexer(new ANTLRInputStream("--help --about=th:'is --about=that\n"))));
        List<Pair<String, String>> parameters = parser.parameters().list;
        assertEquals(parameters.get(0), __("help"));
        assertEquals(parameters.get(1), __("about", "th:'is"));
        assertEquals(parameters.get(2), __("about", "that"));
    }

    @Test
    public void parseString() throws RecognitionException {
        CliParser parser = new CliParser(new CommonTokenStream(new CliLexer(new ANTLRInputStream("--help --about=\"th  is\" --about=that\n"))));
        List<Pair<String, String>> parameters = parser.parameters().list;
        assertEquals(parameters.get(0), __("help"));
        assertEquals(parameters.get(1), __("about", "th  is"));
        assertEquals(parameters.get(2), __("about", "that"));
    }

    @Test
    public void parseStringEscaped() throws Exception {
        CliParser parser = new CliParser(new CommonTokenStream(new CliLexer(new ANTLRInputStream("--help=\"\" --about=\"th\\\"is\" --about=\"th\\\\at\"\n"))));
        List<Pair<String, String>> parameters = parser.parameters().list;
        assertEquals(parameters.get(0), __("help", ""));
        assertEquals(parameters.get(1), __("about", "th\"is"));
        assertEquals(parameters.get(2), __("about", "th\\at"));
    }
}
