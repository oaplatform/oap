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

package oap.json.schema;

import lombok.val;
import oap.application.Application;
import oap.application.Kernel;
import oap.io.Resources;
import oap.json.Binder;
import oap.util.Stream;
import oap.util.Try;

import java.util.Map;

import static java.util.stream.Collectors.toList;
import static oap.util.Pair.__;

/**
 * Created by igor.petrenko on 23.10.2017.
 */
public final class TestJsonValidators {
    private static JsonValidators jsonValidators;

    private TestJsonValidators() {
    }

    public static JsonValidators jsonValidatos() {
        if( jsonValidators == null ) {
            synchronized( TestJsonValidators.class ) {
                if( jsonValidators == null ) {
                    jsonValidators = Application.containsKernel( Kernel.DEFAULT )
                        ? Application.instancesOf( JsonValidators.class ).findFirst().orElse( null )
                        : null;
                    if( jsonValidators == null ) {
                        val urls = Stream.of( Resources.urls( "/META-INF/oap-module.conf" ) )
                            .map( Try.map( url -> __( url, Binder.hocon.unmarshal( Map.class, url ) ) ) )
                            .filter( p -> "oap-validators".equals( p._2.get( "name" ) ) )
                            .map( p -> p._1 )
                            .collect( toList() );
                        val kernel = new Kernel( urls );
                        try {
                            kernel.start();

                            jsonValidators = Application.service( JsonValidators.class );

                        } finally {
                            kernel.stop();
                        }
                    }
                }
            }
        }

        return jsonValidators;
    }
}
