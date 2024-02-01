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
package oap.application.cli;


import oap.util.Pair;
import oap.util.Result;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;


public class Group {
    public final String name;
    private final LinkedHashMap<String, Option<?>> options = new LinkedHashMap<>();
    private final Consumer<Map<String, Object>> action;

    public Group( String name, Consumer<Map<String, Object>> action, Option<?>... options ) {
        this.name = name;
        this.action = action;
        for( Option<?> option : options ) this.options.put( option.name, option );
    }

    boolean matches( List<Pair<String, String>> parameters ) {
        for( Pair<String, String> parameter : parameters ) {
            Option<?> option = options.get( parameter._1 );
            if( option != null && option.matches( parameter ) ) {
                continue;
            }
            return false;
        }
        Set<String> parameterNames = parameters.stream().map( p -> p._1 ).collect( Collectors.toSet() );
        for( Option<?> option : options.values() ) {
            if( option.isRequired() && !parameterNames.contains( option.name ) ) return false;
        }
        return true;
    }

    public Collection<Option<?>> options() {
        return options.values();
    }

    public Result<Void, String> act( List<Pair<String, String>> parameters ) {
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();

        for( Pair<String, String> parameter : parameters ) {
            Option<?> option = options.get( parameter._2 );
            Result<?, String> parsed = option.parse( parameter._1 );

            if( !parsed.isSuccess() ) {
                return Result.failure( parsed.failureValue );
            } else {
                result.put( parameter._1, parsed.successValue );
            }
        }

        action.accept( result );
        return Result.success( null );
    }
}
