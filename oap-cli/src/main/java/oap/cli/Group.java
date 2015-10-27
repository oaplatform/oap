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


import oap.util.Maps;
import oap.util.Pair;
import oap.util.Result;
import oap.util.Stream;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static java.util.stream.Collectors.toList;
import static oap.util.Pair.__;

public class Group {
    private final Map<String, Option<?>> options = new LinkedHashMap<>();
    public final String name;
    private final Consumer<Map<String, Object>> action;

    public Group( String name, Consumer<Map<String, Object>> action, Option<?>... options ) {
        this.name = name;
        this.action = action;
        for( Option option : options ) this.options.put( option.name, option );
    }

    boolean matches( List<Pair<String, String>> parameters ) {
        for( Pair<String, String> parameter : parameters )
            if( !options.containsKey( parameter._1 )
                    || !options.get( parameter._1 ).matches( parameter ) ) return false;
        List<String> parameterNames = parameters.stream().map( p -> p._1 ).collect( toList() );
        for( Option<?> option : options.values() )
            if( option.isRequired() && !parameterNames.contains( option.name ) ) return false;
        return true;
    }

    public Collection<Option<?>> options() {
        return options.values();
    }

    public Result<Void, String> act( List<Pair<String, String>> parameters ) {
        Result<Map<String, Object>, String> r = Stream.of( parameters )
                .tryMap( p -> options
                        .get( p._1 )
                        .parse( p._2 )
                        .mapSuccess( v -> __( p._1, v ) ) )
                .mapSuccess( Maps::of );
        if( r.isSuccess() ) {
            action.accept( r.successValue );
            return Result.success( null );
        } else return Result.failure( r.failureValue );
    }
}
