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

package oap.template;

import lombok.ToString;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.TokenStream;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

@SuppressWarnings( "checkstyle:AbstractClassName" )
abstract class TemplateGrammarAdaptor extends Parser {
    Map<String, List<Method>> builtInFunction;
    ErrorStrategy errorStrategy;

    TemplateGrammarAdaptor( TokenStream input ) {
        super( input );
    }

    String sStringToDString( String sstr ) {
        return '"' + sdStringToString( sstr ) + '"';
    }

    String sdStringToString( String sstr ) {
        return sstr.substring( 1, sstr.length() - 1 );
    }


    @ToString
    static class Function {
        public final String name;

        Function( String name ) {
            this.name = name;
        }
    }
}
