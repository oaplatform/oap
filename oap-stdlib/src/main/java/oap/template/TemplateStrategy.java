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


import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public interface TemplateStrategy<TLine extends JavaCTemplate.Line> {
    TemplateStrategy<JavaCTemplate.Line> DEFAULT = new TemplateStrategy<JavaCTemplate.Line>() {};

    default void map( StringBuilder c, FieldInfo cc, TLine line, String field, String delimiter, Optional<Join> join ) {
        if( join.map( Join::isFirst ).orElse( false ) ) {
            mapFirstJoin( c, line );
        }

        if( join.isPresent() && field.startsWith( "\"" ) ) {
            mapInterJoin( c, cc, line, field );
        } else if( cc.isInstance( Boolean.class ) || cc.isInstance( boolean.class ) ) {
            mapBoolean( c, cc, line, field, join.isPresent() );
        } else if( cc.isPrimitive() ) {
            mapPrimitive( c, cc, line, field, join.isPresent() );
        } else if( cc.isInstance( Enum.class ) )
            mapEnum( c, cc, line, field, join.isPresent() );
        else if( cc.isInstance( Collection.class ) ) {
            mapCollection( c, cc, line, field );
        } else if( !cc.equals( String.class ) ) {
            mapObject( c, cc, line, field, join.isPresent() );
        } else {
            mapString( c, cc, line, field, join.isPresent() );
        }

        if( join.map( Join::isLast ).orElse( false ) ) {
            mapLastJoin( c, line );

        }
    }

    default void mapFirstJoin( StringBuilder c, TLine line ) {}

    default void mapLastJoin( StringBuilder c, TLine line ) {}

    default void mapObject( StringBuilder c, FieldInfo cc, TLine line, String field, boolean isJoin ) {
        c.append( "acc.accept( " );
        function( c, line.function, () -> escape( c, () -> c.append( " String.valueOf( " ).append( field ).append( " )" ) ) );
        c.append( " );" );
    }

    default void mapInterJoin( StringBuilder c, FieldInfo cc, TLine line, String field ) {
        c.append( "acc.accept( " );
        function( c, line.function, () -> c.append( field ) );
        c.append( " );\n" );
    }

    default void mapPrimitive( StringBuilder c, FieldInfo cc, TLine line, String field, boolean isJoin ) {
        c.append( "acc.accept( " );
        function( c, line.function, () -> c.append( field ) );
        c.append( " );" );
    }

    default void mapString( StringBuilder c, FieldInfo cc, TLine line, String field, boolean isJoin ) {
        c.append( "acc.accept( " );
        function( c, line.function, () -> escape( c, () -> c.append( field ) ) );
        c.append( " );" );
    }

    default void mapCollection( StringBuilder c, FieldInfo cc, TLine line, String field ) {
        c.append( "{acc.accept( '[' ).accept( " );
        function( c, line.function, () -> escape( c, () -> c.append( " Strings.join( " ).append( field ).append( " )" ) ) );
        c.append( ").accept( ']' );}" );
    }

    default void mapEnum( StringBuilder c, FieldInfo cc, TLine line, String field, boolean isJoin ) {
        c.append( "acc.accept( " );
        function( c, line.function, () -> c.append( field ) );
        c.append( " );" );
    }

    default boolean ignoreDefaultValue() {
        return false;
    }

    default StringBuilder function( StringBuilder c, JavaCTemplate.Line.Function function, Runnable run ) {
        if( function != null ) {
            c.append( function.name ).append( "( " );
        }
        run.run();
        if( function != null ) {
            if( function.parameters != null )
                c.append( ", " ).append( function.parameters );
            c.append( " )" );
        }

        return c;
    }

    default void escape( StringBuilder c, Runnable run ) {
        c.append( "CharMatcher.javaIsoControl().removeFrom( " );
        run.run();
        c.append( " )" );
    }

    default StringBuilder mapBoolean( StringBuilder c, FieldInfo cc, TLine line, String field, boolean isJoin ) {
        c.append( "acc.accept( " );
        function( c, line.function, () -> c.append( field ) );
        c.append( " );" );
        return c;
    }

    default boolean ifPathNotFoundGetFromMapper() {
        return true;
    }

    default StringBuilder pathNotFound( StringBuilder c, String path ) {
        c.append( "/* Path '" ).append( path ).append( "' not found */" );
        return c;
    }

    default boolean printDelimiter() {
        return true;
    }

    default void beforeLine( StringBuilder c, TLine line, String delimiter ) {
    }

    default void afterLine( StringBuilder c, TLine line, String delimiter ) {
    }
}
