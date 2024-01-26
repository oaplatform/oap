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

package oap.tsv.mapper;

import oap.reflect.Reflect;
import oap.util.Stream;

import java.util.List;
import java.util.function.Function;

import static oap.util.Pair.__;

public class Mapper<E> implements Function<List<String>, E> {
    private final Class<E> clazz;
    private final Configuration config;

    public Mapper( Class<E> clazz, Configuration config ) {
        this.clazz = clazz;
        this.config = config;
    }

    public static <E> Mapper<E> of( Class<E> clazz, Configuration config ) {
        return new Mapper<>( clazz, config );
    }

    @Override
    public E apply( List<String> line ) {
        return Reflect.reflect( clazz ).newInstance( Stream.of( config.columns )
            .<String, Object>mapToPairs( f -> __( f.name(), line.get( f.index() ) ) )
            .toMap() );
    }
}
