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

package oap.logstream.net;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.regex.Pattern;

/**
 * Created by igor.petrenko on 26.01.2017.
 */
@ToString( callSuper = true )
@EqualsAndHashCode( callSuper = true )
public final class BufferConfigurationList extends ArrayList<BufferConfigurationList.BufferConfiguration> {
    private static final Pattern ALL = Pattern.compile( ".*" );

    private BufferConfigurationList() {
    }

    private BufferConfigurationList( BufferConfiguration bufferConfiguration ) {
        super( Collections.singletonList( bufferConfiguration ) );
    }

    private BufferConfigurationList( Collection<BufferConfiguration> c ) {
        super( c );
    }

    static BufferConfigurationList DEFAULT( int bufferSize ) {
        return new BufferConfigurationList( new BufferConfiguration( "DEFAULT", bufferSize, ALL ) );
    }

    public static BufferConfigurationList custom( BufferConfiguration... bufferConfiguration ) {
        return new BufferConfigurationList( Arrays.asList( bufferConfiguration ) );
    }

    @ToString
    @EqualsAndHashCode
    @AllArgsConstructor
    public static final class BufferConfiguration {
        public final String name;
        public final int bufferSize;
        public final Pattern pattern;
    }
}
