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

import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public interface Template<T, TLine extends Template.Line> {
    Template<Object, Line> EMPTY = new Template<>() {
        @Override
        public Object render( Object source, Accumulator accumulator ) {
            accumulator.accept( source );
            return accumulator.build();
        }

        @Override
        public String renderString( Object source ) {
            return String.valueOf( source );
        }
    };

    @SuppressWarnings( "unchecked" )
    static <T> Template<T, Line> empty() {
        return ( Template<T, Line> ) EMPTY;
    }

    <R> R render( T source, Accumulator<R> accumulator );

    String renderString( T source );

    @ToString
    @EqualsAndHashCode
    class Line {
        public final String name;
        public final String path;
        public final Object defaultValue;
        public final Line.Function function;

        public Line( String name, String path, Object defaultValue ) {
            this( name, path, defaultValue, null );
        }

        public Line( String name, String path, Object defaultValue, JavaCTemplate.Line.Function function ) {
            this.name = name;
            this.path = path;
            this.defaultValue = defaultValue;
            this.function = function;
        }

        public static Line line( String name, String path, Object defaultValue ) {
            return new Line( name, path, defaultValue );
        }

        public static Line line( String name, String path, Object defaultValue, JavaCTemplate.Line.Function function ) {
            return new Line( name, path, defaultValue, function );
        }

        @ToString
        @EqualsAndHashCode
        public static class Function {
            public final String name;
            public final String parameters;

            public Function( String name, String parameters ) {
                this.name = name;
                this.parameters = parameters;
            }
        }
    }

    @Retention( RetentionPolicy.RUNTIME)
    @interface Nullable {
    }
}
