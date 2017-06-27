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


import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Optional;

import static oap.reflect.Types.isInstance;
import static oap.reflect.Types.isPrimitive;

/**
 * Created by igor.petrenko on 01.09.2016.
 */
public interface TemplateStrategy<TLine extends JavaCTemplate.Line> {
    TemplateStrategy<JavaCTemplate.Line> DEFAULT = new TemplateStrategy<JavaCTemplate.Line>() {};

    default void map( StringBuilder c, Type cc, TLine line, String field, String delimiter, Optional<Join> join ) {
        if( join.map( Join::isFirst ).orElse( false ) ) {
            mapFirstJoin( c, line );
        }

        if( join.isPresent() && field.startsWith( "\"" ) ) {
            mapInterJoin( c, cc, line, field );
        } else if( isInstance( Boolean.class, cc ) || isInstance( boolean.class, cc ) ) {
            mapBoolean( c, cc, line, field, join.isPresent() );
        } else if( isPrimitive( cc ) ) {
            mapPrimitive( c, cc, line, field, join.isPresent() );
        } else if( isInstance( Enum.class, cc ) )
            mapEnum( c, cc, line, field, join.isPresent() );
        else if( isInstance( Collection.class, cc ) ) {
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

    default void mapObject( StringBuilder c, Type cc, TLine line, String field, boolean isJoin ) {
        c.append( "acc.accept( " );
        function( c, line.function, () -> escape( c, () -> c.append( " String.valueOf( " ).append( field ).append( " )" ) ) );
        c.append( " );" );
    }

    default void mapInterJoin( StringBuilder c, Type cc, TLine line, String field ) {
        c.append( "acc.accept( " );
        function( c, line.function, () -> c.append( field ) );
        c.append( " );\n" );
    }

    default void mapPrimitive( StringBuilder c, Type cc, TLine line, String field, boolean isJoin ) {
        c.append( "acc.accept( " );
        function( c, line.function, () -> c.append( field ) );
        c.append( " );" );
    }

    default void mapString( StringBuilder c, Type cc, TLine line, String field, boolean isJoin ) {
        c.append( "acc.accept( " );
        function( c, line.function, () -> escape( c, () -> c.append( field ) ) );
        c.append( " );" );
    }

    default void mapCollection( StringBuilder c, Type cc, TLine line, String field ) {
        c.append( "{acc.accept( '[' ).accept( " );
        function( c, line.function, () -> escape( c, () -> c.append( " Strings.join( " ).append( field ).append( " )" ) ) );
        c.append( ").accept( ']' );}" );
    }

    default void mapEnum( StringBuilder c, Type cc, TLine line, String field, boolean isJoin ) {
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
        c.append( "CharMatcher.JAVA_ISO_CONTROL.removeFrom( " );
        run.run();
        c.append( " )" );
    }

    default StringBuilder mapBoolean( StringBuilder c, Type cc, TLine line, String field, boolean isJoin ) {
        c.append( "acc.accept( " );
        function( c, line.function, () -> c.append( field ) );
        c.append( " );" );
        return c;
    }

    default String pathNotFound( String path ) {
        return "";
    }

    default boolean printDelimiter() {
        return true;
    }

    default void beforeLine( StringBuilder c, TLine line, String delimiter ) {
    }

    default void afterLine( StringBuilder c, TLine line, String delimiter ) {
    }
}
