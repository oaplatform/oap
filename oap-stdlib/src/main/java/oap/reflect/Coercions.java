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
package oap.reflect;

import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import oap.json.Binder;
import oap.util.BiStream;
import oap.util.BitSet;
import oap.util.Dates;
import oap.util.Lists;
import oap.util.Maps;
import oap.util.Numbers;
import oap.util.Pair;
import oap.util.Sets;
import oap.util.Stream;
import oap.util.function.Try;
import org.apache.commons.io.IOUtils;
import org.apache.commons.text.StringSubstitutor;
import org.joda.time.DateTime;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;
import static oap.util.Pair.__;

@Slf4j
public final class Coercions {

    private static final HashMap<String, BiFunction<String, Reflection, Object>> functions = new HashMap<>();

    private static final HashMap<Class<?>, Function<Object, Object>> convertors = new HashMap<>();

    static {
        addFunction( Try.biMap( ( path, reflection ) -> reflection.assignableTo( String.class )
                ? java.nio.file.Files.readString( Path.of( renderPathTemplate( path ) ), UTF_8 )
                : Binder.Format.of( path, false ).binder.unmarshal( reflection, Path.of( renderPathTemplate( path ) ) ) ),
            "path" );
        addFunction( Try.biMap( ( path, reflection ) -> reflection.assignableTo( String.class )
                ? IOUtils.toString( new URL( path ), UTF_8 )
                : Binder.Format.of( path, false ).binder.unmarshal( reflection, new URL( path ) ) ),
            "url" );
        addFunction( Try.biMap( ( cp, reflection ) -> {
                URL url = Preconditions.checkNotNull( Coercions.class.getResource( cp ), "Resource not found: " + cp );
                return reflection.assignableTo( String.class )
                    ? IOUtils.toString( url, UTF_8 )
                    : Binder.Format.of( cp, false ).binder.unmarshal( reflection, url );
            } ),
            "classpath" );
        addFunction( Try.biMap( ( str, reflection ) -> str ),
            "str", "text", "plain" );
        addFunction( Try.biMap( ( str, reflection ) -> Binder.hoconWithoutSystemProperties.unmarshal( reflection, str ) ),
            "hocon" );
        addFunction( Try.biMap( ( str, reflection ) -> Binder.json.unmarshal( reflection, str ) ),
            "json" );
        addFunction( Try.biMap( ( str, reflection ) -> Binder.yaml.unmarshal( reflection, str ) ),
            "yaml" );

        convertors.put( Object.class, value -> value );

        convertors.put( String.class,
            value -> value instanceof String
                ? value
                : value instanceof byte[]
                    ? new String( ( byte[] ) value, UTF_8 )
                    : String.valueOf( value ) );

        convertors.put( Optional.class, Optional::ofNullable );

        var booleanConvertor = new BooleanConvertor();
        convertors.put( boolean.class, booleanConvertor );
        convertors.put( Boolean.class, booleanConvertor );

        var intConvertor = new IntConvertor();
        convertors.put( int.class, intConvertor );
        convertors.put( Integer.class, intConvertor );

        var longFunc = new LongConvertor();
        convertors.put( long.class, longFunc );
        convertors.put( Long.class, longFunc );

        var doubleFunc = new DoubleConvertor();
        convertors.put( double.class, doubleFunc );
        convertors.put( Double.class, doubleFunc );

        var charConvertor = new CharConvertor();
        convertors.put( char.class, charConvertor );
        convertors.put( Character.class, charConvertor );

        var shortConvertor = new ShortConvertor();
        convertors.put( short.class, shortConvertor );
        convertors.put( Short.class, shortConvertor );

        var byteConvertor = new ByteConvertor();
        convertors.put( byte.class, byteConvertor );
        convertors.put( Byte.class, byteConvertor );

        var floatConvertor = new FloatConvertor();
        convertors.put( float.class, floatConvertor );
        convertors.put( Float.class, floatConvertor );

        convertors.put( DateTime.class, new DateTimeConvertor() );
        convertors.put( Path.class, new PathConvertor() );
        convertors.put( URL.class, new URLConvertor() );
        convertors.put( URI.class, new URIConvertor() );
        convertors.put( Pattern.class, new PatternConvertor() );
        convertors.put( BitSet.class, new BitSetConvertor() );
        convertors.put( Class.class, Try.map( value -> Class.forName( ( String ) value ) ) );
    }

    private List<Pair<BiPredicate<Reflection, Object>, BiFunction<Reflection, Object, Object>>> coersions = Lists.empty();

    @SuppressWarnings( "unchecked" )
    private Coercions() {
        with( ( r, v ) -> r.isEnum(), ( r, v ) -> v instanceof Enum ? v : r.enumValue( ( String ) v ) );
        with( ( r, v ) -> !r.assignableTo( Map.class ) && v instanceof Map<?, ?>, ( r, v ) -> r.newInstance( ( Map<String, Object> ) v ) );
        with( ( r, v ) -> r.assignableTo( Collection.class ), ( r, v ) -> {
            if( v instanceof String ) {
                return castFunction( r, v );
            }

            Reflection componentType = r.getCollectionComponentType();
            return Stream.of( ( List<?> ) v )
                .map( o -> cast( componentType, o ) )
                .collect( Collectors.toCollection( () -> r.isInterface()
                    ? r.assignableTo( List.class ) ? Lists.of() : Sets.of()
                    : r.newInstance() ) );
        } );
        with( ( r, v ) -> r.assignableTo( Map.class ), ( r, v ) -> {
            Pair<Reflection, Reflection> componentType = r.getMapComponentsType();
            Objects.requireNonNull( componentType._1 );
            Objects.requireNonNull( componentType._2 );

            if( v instanceof String ) {
                return castFunction( r, v );
            }

            return BiStream.of( ( Map<?, ?> ) v )
                .map( ( k, o ) -> __( cast( componentType._1, k ), cast( componentType._2, o ) ) )
                .collect( Maps.Collectors.toMap( () -> r.isInterface()
                    ? r.implementationOf( ConcurrentMap.class ) ? new ConcurrentHashMap<>() : Maps.of()
                    : r.newInstance() ) );
        } );
    }

    private static void addFunction( BiFunction<String, Reflection, Object> func, String... names ) {
        for( var name : names ) {
            functions.put( name, func );
        }
    }

    private static String renderPathTemplate( String path ) {
        return new StringSubstitutor( key -> {
            String v = System.getProperty( key );
            if( v == null ) {
                v = System.getenv( key );
            }
            if( v == null ) {
                v = key;
            }
            return v;
        }, "${ENV.", "}", '\\' ).replace( path );
    }

    public static Coercions basic() {
        return new Coercions();
    }

    public static Object castFunction( Reflection r, Object value ) {
        var str = ( ( String ) value ).trim();
        var sv = str.indexOf( '(' );
        if( sv > 0 && str.endsWith( ")" ) ) {
            var funcName = str.substring( 0, sv );
            var func = functions.get( funcName );
            var funcValue = str.substring( sv + 1, str.length() - 1 ).trim();
            if( func != null ) {
                return func.apply( funcValue, r );
            }

            log.trace( "Unknown function '" + funcName + "'" );
        }
        if( r.assignableFrom( String.class ) ) {
            return value;
        } else {
            throw new ReflectException( "cannot cast " + value + " to " + r );
        }
    }

    public Object cast( Reflection target, Object value ) throws ReflectException {
        if( value == null ) return null;
        try {
            if( target.assignableFrom( value.getClass() ) ) {

                if( String.class.equals( value.getClass() ) ) {
                    return castFunction( target, value );
                }

                if( !target.assignableTo( Collection.class )
                    && !target.assignableTo( Map.class ) ) {
                    return value;
                }
            }

            Function<Object, Object> ff = convertors.getOrDefault( target.underlying, v -> {
                for( Pair<BiPredicate<Reflection, Object>, BiFunction<Reflection, Object, Object>> coersion : coersions ) {
                    if( coersion._1.test( target, value ) ) {
                        return coersion._2.apply( target, value );
                    }
                }
                throw new ReflectException( "cannot cast " + value + " to " + target );
            } );
            return ff.apply( value );
        } catch( ClassCastException | NumberFormatException e ) {
            throw new ReflectException( e );
        }
    }

    public Coercions with( BiPredicate<Reflection, Object> test, BiFunction<Reflection, Object, Object> coersion ) {
        coersions.add( __( test, coersion ) );
        return this;
    }

    public Coercions with( Predicate<Reflection> test, BiFunction<Reflection, Object, Object> coersion ) {
        return with( ( r, v ) -> test.test( r ), coersion );
    }

    public Coercions withStringToObject() {
        return with( ( r, v ) -> v instanceof String, Coercions::castFunction );
    }

    public Coercions withIdentity() {
        return with( ( r, v ) -> true, ( r, value ) -> value );
    }

    public static class LongConvertor implements Function<Object, Object> {
        public static final LongConvertor DEFAULT = new LongConvertor();
        private final String name;

        public LongConvertor() {
            this( "long" );
        }

        public LongConvertor( String name ) {
            this.name = name;
        }

        @Override
        public Object apply( Object value ) {
            if( value instanceof Number ) return ( ( Number ) value ).longValue();
            else if( value instanceof String ) try {
                return Numbers.parseLongWithUnits( ( String ) value );
            } catch( NumberFormatException e ) {
                throw new ReflectException( "cannot cast " + value + " to " + name );
            }
            else throw new ReflectException( "cannot cast " + value + " to " + name );
        }
    }

    private static class IntConvertor extends LongConvertor {
        IntConvertor() {
            super( "int" );
        }

        @Override
        public Object apply( Object value ) {
            return ( int ) ( long ) super.apply( value );
        }
    }

    private static class BooleanConvertor implements Function<Object, Object> {
        @Override
        public Object apply( Object value ) {
            if( value instanceof Boolean ) return value;
            else if( value instanceof String ) return Boolean.valueOf( ( String ) value );
            else throw new ReflectException( "cannot cast " + value + " to Boolean.class" );
        }
    }

    private static class DoubleConvertor implements Function<Object, Object> {
        @Override
        public Object apply( Object value ) {
            if( value instanceof Number ) return ( ( Number ) value ).doubleValue();
            else if( value instanceof String ) try {
                return Numbers.parseDoubleWithUnits( ( String ) value );
            } catch( NumberFormatException e ) {
                throw new ReflectException( "cannot cast " + value + " to Double.class" );
            }
            else throw new ReflectException( "cannot cast " + value + " to Double.class" );
        }
    }

    private static class CharConvertor implements Function<Object, Object> {
        @Override
        public Object apply( Object value ) {
            if( value instanceof Character ) return value;
            else if( value instanceof String && ( ( String ) value ).length() > 0 )
                return ( ( String ) value ).charAt( 0 );
            else throw new ReflectException( "cannot cast " + value + " to Character.class" );
        }
    }

    private static class ShortConvertor implements Function<Object, Object> {
        @Override
        public Object apply( Object value ) {
            if( value instanceof Number ) return ( ( Number ) value ).shortValue();
            else if( value instanceof String ) try {
                return Short.parseShort( ( String ) value );
            } catch( NumberFormatException e ) {
                throw new ReflectException( "cannot cast " + value + " to Short.class" );
            }
            else throw new ReflectException( "cannot cast " + value + " to Short.class" );
        }
    }

    private static class ByteConvertor implements Function<Object, Object> {
        @Override
        public Object apply( Object value ) {
            if( value instanceof Number ) return ( ( Number ) value ).byteValue();
            else if( value instanceof String ) try {
                return Byte.parseByte( ( String ) value );
            } catch( NumberFormatException e ) {
                throw new ReflectException( "cannot cast " + value + " to Byte.class" );
            }
            else throw new ReflectException( "cannot cast " + value + " to Byte.class" );
        }
    }

    private static class FloatConvertor implements Function<Object, Object> {
        @Override
        public Object apply( Object value ) {
            if( value instanceof Number ) return ( ( Number ) value ).floatValue();
            else if( value instanceof String ) try {
                return Float.parseFloat( ( String ) value );
            } catch( NumberFormatException e ) {
                throw new ReflectException( "cannot cast " + value + " to Float.class" );
            }
            else throw new ReflectException( "cannot cast " + value + " to Float.class" );
        }
    }

    private static class DateTimeConvertor implements Function<Object, Object> {
        @Override
        public Object apply( Object value ) {
            if( value instanceof DateTime ) return value;
            else if( value instanceof String ) return Dates.parseDate( ( String ) value )
                .mapSuccess( dt -> ( Object ) dt )
                .orElseThrow( e -> {
                    throw new ReflectException( "cannot cast " + value + " to DateTime.class" );
                } );
            else throw new ReflectException( "cannot cast " + value + " to DateTime.class" );
        }
    }

    private static class PathConvertor implements Function<Object, Object> {
        @Override
        public Object apply( Object value ) {
            if( value instanceof Path ) return value;
            else if( value instanceof String ) return Paths.get( ( String ) value );
            else throw new ReflectException( "cannot cast " + value + " to Path.class" );
        }
    }

    private static class URLConvertor implements Function<Object, Object> {
        @Override
        public Object apply( Object value ) {
            return switch( value ) {
                case URL url -> url;
                case String str -> {
                    try {
                        yield new URL( str );
                    } catch( MalformedURLException e ) {
                        if( str.startsWith( "classpath(" ) ) {
                            str = str.substring( 10, str.length() - 1 );
                        }

                        var url = getClass().getResource( str );
                        if( url != null ) yield url;

                        try {
                            yield Paths.get( str ).toUri().toURL();
                        } catch( MalformedURLException malformedURLException ) {
                            throw new ReflectException( "cannot cast " + value + " to URL.class" );
                        }
                    }
                }
                default -> throw new ReflectException( "cannot cast " + value + " to URL.class" );
            };
        }
    }

    private static class URIConvertor implements Function<Object, Object> {
        @Override
        public Object apply( Object value ) {
            if( value instanceof URI ) return value;
            else if( value instanceof String ) try {
                return new URI( ( String ) value );
            } catch( URISyntaxException e ) {
                throw new ReflectException( "cannot cast " + value + " to URI.class" );
            }
            else throw new ReflectException( "cannot cast " + value + " to URI.class" );
        }
    }

    private static class BitSetConvertor implements Function<Object, Object> {
        @Override
        public Object apply( Object value ) {
            if( value instanceof BitSet ) return value;
            else if( value instanceof String ) {
                return new BitSet( ( String ) value );
            } else throw new ReflectException( "cannot cast " + value + " to BitSet.class" );
        }
    }

    private static class PatternConvertor implements Function<Object, Object> {
        @Override
        public Object apply( Object value ) {
            if( value instanceof Pattern ) return value;
            else if( value instanceof String ) try {
                return Pattern.compile( ( String ) value );
            } catch( PatternSyntaxException e ) {
                throw new ReflectException( "cannot cast " + value + " to Pattern.class" );
            }
            else throw new ReflectException( "cannot cast " + value + " to Pattern.class" );
        }
    }
}
