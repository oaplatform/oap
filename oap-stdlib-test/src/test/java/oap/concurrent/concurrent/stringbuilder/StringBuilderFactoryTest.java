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

package oap.concurrent.concurrent.stringbuilder;

import oap.concurrent.Threads;
import oap.concurrent.stringbuilder.ConcurrentStringBuilderFactory;
import oap.concurrent.stringbuilder.StringBuilderFactory;
import oap.concurrent.stringbuilder.ThreadLocalStringBuilderFactory;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BinaryOperator;

import static oap.concurrent.Threads.joinSafely;
import static org.assertj.core.api.Assertions.assertThat;

public class StringBuilderFactoryTest {

    @DataProvider
    public Object[][] factories() {
        return new Object[][] {
            { new ConcurrentStringBuilderFactory() },
            { new ThreadLocalStringBuilderFactory() }
        };
    }

    @Test( dataProvider = "factories" )
    public void concurrent( StringBuilderFactory factory ) {
        Action action = new Action( factory );
        AtomicReference<String> resultA = new AtomicReference<>();
        AtomicReference<String> resultB = new AtomicReference<>();
        Thread threadA = new Thread( () -> resultA.set( action.twoStrings( "aaaa" ) ) );
        Thread threadB = new Thread( () -> resultB.set( action.twoStrings( "bbbb" ) ) );
        threadA.start();
        threadB.start();
        joinSafely( threadA, threadB );
        assertThat( resultA ).hasValue( "aaaaaaaa" );
        assertThat( resultB ).hasValue( "bbbbbbbb" );
        assertThat( action.callSequence ).hasValueMatching( s ->
            s.equals( "1S1S2F2F" )
                || s.equals( "11SS22FF" )
                || s.equals( "11SS2F2F" )
                || s.equals( "1S1S22FF" )
        );
    }

    public static class Action {
        private final StringBuilderFactory factory;
        private final AtomicReference<String> callSequence = new AtomicReference<>( "" );

        public Action( StringBuilderFactory factory ) {
            this.factory = factory;
        }

        public String twoStrings( String value ) {
            return factory.stringOf( sb -> {
                BinaryOperator<String> add = ( a, b ) -> a + b;
                callSequence.accumulateAndGet( "1", add );
                sb.append( value );
                callSequence.accumulateAndGet( "S", add );
                Threads.sleepSafely( 100 );
                callSequence.accumulateAndGet( "2", add );
                sb.append( value );
                callSequence.accumulateAndGet( "F", add );
            } );
        }
    }
}
