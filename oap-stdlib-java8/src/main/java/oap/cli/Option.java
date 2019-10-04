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
package oap.cli;

import oap.util.Pair;
import oap.util.Result;

import java.nio.file.Path;

public class Option<V> {
    public final String name;
    private boolean withArgument;
    private ValueParser<V> parser;
    private boolean required;
    private String description;

    private Option( String name ) {
        this.name = name;
    }

    public static Option<Path> path( String name ) {
        return Option.<Path>option( name ).argument( ValueParser.PATH );
    }

    public static Option<String> string( String name ) {
        return Option.<String>option( name ).argument( ValueParser.STRING );
    }

    public static Option<Integer> integer( String name, int min, int max ) {
        return Option.<Integer>option( name ).argument( ValueParser.INT( min, max ) );
    }

    public static <V> Option<V> option( String name ) {
        return new Option<>( name );
    }

    public static Option<Void> simple( String name ) {
        return new Option<>( name );
    }

    boolean matches( Pair<String, String> parameter ) {
        return name.equals( parameter._1 ) && ( !withArgument || parameter._2 != null );
    }

    public Option<V> argument( ValueParser<V> parser ) {
        this.parser = parser;
        this.withArgument = true;
        return this;
    }

    public Option<V> required() {
        this.required = true;
        return this;
    }

    public Option<V> description( String description ) {
        this.description = description;
        return this;
    }

    public boolean isRequired() {
        return required;
    }

    public String toString() {
        return "--" + name + ( withArgument ? "=<value>" : "" ) + ( description != null ? " - " + description : "" );
    }

    public static Pair<String, String> __( String key, String value ) {
        return Pair.__( key, value );
    }

    public static Pair<String, String> __( String key ) {
        return Pair.__( key, null );
    }

    public Result<V, String> parse( String value ) {
        return ( parser != null ? parser.parse( value ) : Result.<V, String>success( null ) )
            .filter( v -> v != null && withArgument || v == null )
            .orElse( Result.failure( name + " should contain value" ) )
            .filter( v -> v == null && !withArgument || v != null )
            .orElse( Result.failure( name + " should not contain a value" ) );
    }
}
