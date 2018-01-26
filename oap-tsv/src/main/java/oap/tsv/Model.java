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

package oap.tsv;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public abstract class Model<T> {
    protected final boolean withHeader;
    protected Predicate<List<String>> filter;

    protected Model( boolean withHeader ) {
        this.withHeader = withHeader;
    }

    public static <T> Complex<T> complex( Function<String, Model<T>> formats ) {
        return new Complex<>( formats );
    }

    public static TypedListModel typedList( boolean withHeader ) {
        return new TypedListModel( withHeader );
    }

    public static <T> MappingModel<T> mapping( boolean withHeader, Function<List<String>, T> mapper ) {
        return mapping( withHeader, Integer.MAX_VALUE, mapper );
    }

    public static <T> MappingModel<T> mapping( boolean withHeader, int maxOffset, Function<List<String>, T> mapper ) {
        return new MappingModel<>( withHeader, maxOffset, mapper );
    }

    public abstract int maxOffset();

    public Predicate<? super List<String>> filter() {
        return this.filter == null ? l -> true : this.filter;
    }

    public abstract T map( List<String> line );

    public static class Complex<T> {
        private Function<String, Model<T>> formats;

        private Complex( Function<String, Model<T>> formats ) {
            this.formats = formats;
        }

        public Model<T> modelFor( String path ) {
            return formats.apply( path );
        }
    }
}
