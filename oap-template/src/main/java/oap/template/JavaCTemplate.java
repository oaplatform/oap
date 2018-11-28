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

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import oap.tools.MemoryClassLoader;
import oap.util.Pair;
import oap.util.Try;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.commons.text.StringEscapeUtils;

import java.io.BufferedReader;
import java.io.StringReader;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.stream.Collectors.joining;
import static oap.reflect.Types.getOptionalArgumentType;
import static oap.reflect.Types.isPrimitive;
import static oap.reflect.Types.toJavaType;
import static oap.util.Pair.__;

/**
 * Created by igor.petrenko on 01.09.2016.
 */
@Slf4j
public class JavaCTemplate<T, TLine extends Template.Line> implements Template<T, TLine> {
    private static final String math = "/*+-%";
    private final Map<String, String> overrides;
    private final Map<String, Supplier<String>> mapper;
    private BiFunction<T, Accumulator, ?> func;
    private TemplateStrategy<TLine> map;

    @SneakyThrows
    @SuppressWarnings( "unchecked" )
    JavaCTemplate( String name, Class<T> clazz, List<TLine> pathAndDefault, String delimiter, TemplateStrategy<TLine> map,
                   Map<String, String> overrides, Map<String, Supplier<String>> mapper,
                   Path cacheFile ) {
        name = name.replaceAll( "[\\s%\\-;\\\\/:*?\"<>|]", "_" );
        this.map = map;
        this.overrides = overrides;
        this.mapper = mapper;
        StringBuilder c = new StringBuilder();

        try {
            AtomicInteger num = new AtomicInteger();
            FieldStack fields = new FieldStack();

            String className = clazz.getName().replace( '$', '.' );

            c.append( "package " ).append( getClass().getPackage().getName() ).append( ";\n"
                + "\n"
                + "import oap.util.Strings;\n"
                + "\n"
                + "import java.util.*;\n"
                + "import java.util.function.BiFunction;\n"
                + "import com.google.common.base.CharMatcher;\n"
                + "\n"
                + "public  class " ).append( name ).append( " implements BiFunction<" ).append( className ).append( ", Accumulator, Object> {\n"
                + "   @Override\n"
                + "   public Object apply( " ).append( className ).append( " s, Accumulator acc ) {\n"
                + "     StringBuilder jb = new StringBuilder();\n"
                + "\n" );

            int size = pathAndDefault.size();
            for( int x = 0; x < size; x++ ) {
                addPath( clazz, pathAndDefault.get( x ), delimiter, c, num, fields, x + 1 >= size );
            }


            c.append( "\n"
                + "     return acc.build();\n"
                + "   }\n"
                + "}" );

            AtomicInteger line = new AtomicInteger( 0 );
            log.trace( "\n{}", new BufferedReader( new StringReader( c.toString() ) )
                .lines()
                .map( l -> String.format( "%3d", line.incrementAndGet() ) + " " + l )
                .collect( joining( "\n" ) )
            );

            val fullTemplateName = getClass().getPackage().getName() + "." + name;
            MemoryClassLoader mcl = new MemoryClassLoader( fullTemplateName, c.toString(), cacheFile );
            func = ( BiFunction<T, Accumulator, ?> ) mcl.loadClass( fullTemplateName ).newInstance();

        } catch( Exception e ) {
            log.error( c.toString() );
            throw e;
        }
    }

    private void addPath( Class<T> clazz, TLine line, String delimiter, StringBuilder c,
                          AtomicInteger num, FieldStack fields,
                          boolean last ) throws NoSuchMethodException, NoSuchFieldException {
        AtomicInteger tab = new AtomicInteger( 5 );

        c.append( "\n" );
        tab( c, tab ).append( "// " ).append(
            line.path != null ? line.path
                : "\"" + StringEscapeUtils.escapeJava( line.defaultValue.toString() ) + "\"" ).append( "\n" );

        if( line.path == null ) {
            tab( c, tab ).append( "acc.accept( \"" ).append( StringEscapeUtils.escapeJava( line.defaultValue.toString() ) ).append( "\" );\n" );
        } else {
            if( pathExists( clazz, line ) ) {
                buildPath( clazz, line, delimiter, c, num, fields, last, tab, false );
            } else {
                log.warn( "path {} not found", line );
                map.pathNotFound( line.path );
            }
        }
    }

    private void buildPath( Class<T> clazz, TLine line, String delimiter, StringBuilder c,
                            AtomicInteger num, FieldStack fields, boolean last, AtomicInteger tab, boolean validation ) throws NoSuchFieldException, NoSuchMethodException {
        val orPath = StringUtils.split( line.path, '|' );
        int orIndex = 0;

        for( int i = 0; i < orPath.length; i++ ) {
            val path = orPath[i].trim();
            val newPath = overrides.get( path );
            orPath[i] = newPath != null ? newPath : path;
        }

        if( !validation ) map.beforeLine( c, line, delimiter );
        addPathOr( clazz, delimiter, c, num, fields, last, tab, orPath, orIndex, line );
        if( !validation ) map.afterLine( c, line, delimiter );
    }

    private boolean pathExists( Class<T> clazz, TLine line ) throws NoSuchFieldException, NoSuchMethodException {
        try {
            buildPath( clazz, line, "", new StringBuilder(), new AtomicInteger(),
                new FieldStack(), false, new AtomicInteger(), true );
        } catch( Throwable e ) {
            if( pathNotFound( e ) ) return false;
            throw e;
        }

        return true;
    }

    private boolean pathNotFound( Throwable e ) {
        if( e instanceof NoSuchFieldException || e instanceof NoSuchMethodException ) return true;
        if( e.getCause() != null ) return pathNotFound( e.getCause() );
        return false;
    }

    private void printDelimiter( String delimiter, StringBuilder c, boolean last, AtomicInteger tab ) {
        if( map.printDelimiter() && !last && StringUtils.isNotEmpty( delimiter ) )
            tab( c, tab ).append( "acc.accept('" ).append( delimiter ).append( "');\n" );
    }

    private void addPathOr( Class<T> clazz, String delimiter, StringBuilder c, AtomicInteger num,
                            FieldStack fields, boolean last, AtomicInteger tab,
                            String[] orPath, int orIndex, TLine line ) throws NoSuchFieldException, NoSuchMethodException {
        val currentPath = orPath[orIndex].trim();

        val m = mapper.get( currentPath );
        if( m != null ) {
            printDefaultValue( tab( c, tab ), m.get(), line );
        } else {

            int sp = 0;
            StringBuilder newPath = new StringBuilder( "s." );
            MutableObject<Type> lc = new MutableObject<>( clazz );
            AtomicInteger psp = new AtomicInteger( 0 );
            AtomicInteger opts = new AtomicInteger( 0 );
            while( sp >= 0 ) {
                sp = currentPath.indexOf( '.', sp + 1 );

                if( sp > 0 ) {
                    Pair<Type, String> pnp = fields.computeIfAbsent( currentPath.substring( 0, sp ), Try.map( ( key ) -> {
                        String prefix = StringUtils.trim( psp.get() > 1 ? key.substring( 0, psp.get() - 1 ) : "" );
                        String suffix = StringUtils.trim( key.substring( psp.get() ) );

                        boolean optional = isOptional( lc.getValue() );
                        Type declaredFieldType = getDeclaredFieldOrFunctionType(
                            optional ? getOptionalArgumentType( lc.getValue() ) : lc.getValue(), suffix );

                        String classType = toJavaType( declaredFieldType );

                        String field = "field" + num.incrementAndGet();

                        Optional<Pair<Type, String>> rfP = Optional.ofNullable( fields.get( prefix ) );
                        String rf = rfP.map( p -> p._2 + ( optional ? ".get()"
                            : "" ) + "." + suffix ).orElse( "s." + key );

                        if( optional ) {
                            tab( c, tab ).append( "if( " ).append( rfP.map( p -> p._2 ).orElse( "s" ) ).append( ".isPresent() ) {\n" );
                            opts.incrementAndGet();
                            tabInc( tab );
                            fields.up();
                        }

                        tab( c, tab ).append( " " ).append( classType ).append( " " ).append( field ).append( " = " ).append( rf ).append( ";\n" );

                        lc.setValue( declaredFieldType );
                        return __( lc.getValue(), field );
                    } ) );
                    newPath = new StringBuilder( pnp._2 + "." );
                    lc.setValue( pnp._1 );
                    psp.set( sp + 1 );
                } else {
                    newPath.append( currentPath.substring( psp.get() ) );
                }
            }

            int in = newPath.toString().lastIndexOf( '.' );
            String pField = in > 0 ? newPath.substring( 0, in ) : newPath.toString();
            String cField = newPath.substring( in + 1 );

            boolean isJoin = cField.startsWith( "{" );
            String[] cFields = isJoin
                ? StringUtils.split( cField.substring( 1, cField.length() - 1 ), ',' ) : new String[] { cField };

            Type parentClass = lc.getValue();
            boolean isOptionalParent = isOptional( parentClass ) && !cField.startsWith( "isPresent" );
            String optField = null;
            if( isOptionalParent ) {
                opts.incrementAndGet();
                tab( c, tab ).append( "if( " ).append( pField ).append( ".isPresent() ) {\n" );
                fields.up();
                tabInc( tab );

                parentClass = getOptionalArgumentType( parentClass );
                optField = "opt" + num.incrementAndGet();
                tab( c, tab ).append( " " ).append( toJavaType( parentClass ) ).append( " " ).append( optField ).append( " = " ).append( pField ).append( ".get();\n" );
            }


            for( int i = 0; i < cFields.length; i++ ) {
                cField = StringUtils.trim( cFields[i] );

                Optional<Join> join = isJoin ? Optional.of( new Join( i, cFields.length ) ) : Optional.empty();
                if( cField.startsWith( "\"" ) ) {
                    tab( c.append( "\n" ), tab );
                    map.map( c, String.class, line, cField, delimiter, join );
                } else {
                    val isParentMap = isMap( parentClass );

                    val ff = ( isParentMap ? "get( \"" + StringUtils.replace( cField, "((", "(" ) + "\" )" : cField );

                    if( isOptionalParent ) {
                        newPath = new StringBuilder( in > 0 ? optField + "." + ff : cField );
                    } else {
                        newPath = new StringBuilder( in > 0 ? pField + "." + ff : "s." + ff );
                    }

                    Type cc = in > 0 ? getDeclaredFieldOrFunctionType( parentClass, cField ) : parentClass;


                    add( c, num, newPath.toString(), cc, parentClass, true, tab, orPath, orIndex,
                        clazz, delimiter, fields, last || ( i < cFields.length - 1 ), line, join );
                }
            }


            c.append( "\n" );

            for( int i = 0; i < opts.get(); i++ ) {
                fields.down();
                tabDec( tab );
                tab( c, tab ).append( "} else {\n" );
                fields.up();
                tabInc( tab );

                if( orIndex + 1 < orPath.length ) {
                    addPathOr( clazz, delimiter, c, num, fields, last, new AtomicInteger( tab.get() + 2 ), orPath, orIndex + 1, line );
                } else {
                    printDefaultValue( c, line.defaultValue, line );
                    if( !map.ignoreDefaultValue() ) printDelimiter( delimiter, c, last, tab );
                }
                tabDec( tab );
                fields.down();
                tab( c, tab ).append( "}\n" );
            }
        }
    }

    private boolean isMap( Type parentClass ) {
        if( parentClass instanceof ParameterizedType )
            return isMap( ( ( ParameterizedType ) parentClass ).getRawType() );

        return ( ( Class ) parentClass ).isAssignableFrom( Map.class );
    }

    private void tabInc( AtomicInteger tab ) {
        tab.getAndUpdate( v -> v + 2 );
    }

    private void tabDec( AtomicInteger tab ) {
        tab.getAndUpdate( v -> v - 2 );
    }

    private StringBuilder tab( StringBuilder sb, AtomicInteger tab ) {
        for( int i = 0; i < tab.get(); i++ ) sb.append( ' ' );

        return sb;
    }

    @SuppressWarnings( "unchecked" )
    private Type getDeclaredFieldOrFunctionType( Type type, String field ) throws NoSuchFieldException, NoSuchMethodException {
        if( type instanceof ParameterizedType )
            return getDeclaredFieldOrFunctionType( ( ( ParameterizedType ) type ).getRawType(), field );

        int mi = StringUtils.indexOfAny( field, math );
        if( mi > 0 ) return getDeclaredFieldOrFunctionType( type, field.substring( 0, mi ) );

        Class type1 = ( Class ) type;
        try {
            int i = field.indexOf( '(' );
            while( i > 0 && field.charAt( i + 1 ) == '(' ) {
                i = field.indexOf( '(', i + 2 );
            }
            if( i < 0 ) {
                if( type1.isAssignableFrom( Map.class ) ) {
                    return Object.class;
                }
                return type1.getDeclaredField( field ).getGenericType();
            } else
                return type1.getDeclaredMethod( field.substring( 0, i ) ).getGenericReturnType();
        } catch( NoSuchMethodException | NoSuchFieldException e ) {
            if( !type1.getSuperclass().equals( Object.class ) )
                return getDeclaredFieldOrFunctionType( type1.getGenericSuperclass(), field );
            else throw e;
        }
    }

    private void add( StringBuilder c, AtomicInteger num, String newPath, Type cc, Type parentType,
                      boolean nullable, AtomicInteger tab,
                      String[] orPath, int orIndex, Class<T> clazz, String delimiter,
                      FieldStack fields, boolean last, TLine line, Optional<Join> join ) throws NoSuchFieldException, NoSuchMethodException {
        String pfield = newPath;
        boolean primitive = isPrimitive( cc );
        if( !primitive ) {
            pfield = "field" + num.incrementAndGet();
            tab( c, tab ).append( " " ).append( toJavaType( cc ) ).append( " " ).append( pfield ).append( " = " ).append( newPath ).append( ";\n" );
        }
        if( !primitive ) {
            if( isOptional( cc ) ) {
                Type cc1 = getOptionalArgumentType( cc );
                tab( c, tab ).append( "if( " ).append( pfield ).append( ".isPresent() ) {\n" );
                fields.up();
                add( c, num, pfield + ".get()", cc1, parentType, false, new AtomicInteger( tab.get() + 2 ),
                    orPath, orIndex, clazz, delimiter, fields, last, line, join );
                fields.down();
                tab( c, tab ).append( "} else {\n" );

                if( orIndex + 1 < orPath.length ) {
                    fields.up();
                    addPathOr( clazz, delimiter, c, num, fields, last, new AtomicInteger( tab.get() + 2 ), orPath, orIndex + 1, line );
                    fields.down();
                } else {
                    printDefaultValue( tab( c, tab ), line.defaultValue, line );
                    if( !map.ignoreDefaultValue() ) printDelimiter( delimiter, c, last, tab );
                }
                tab( c, tab ).append( "}\n" );
            } else {
                tab( c, tab );

                if( nullable ) c.append( "if( " ).append( pfield ).append( " != null ) { " );

                map.map( c, cc, line, pfield, delimiter, join );
                printDelimiter( delimiter, c, last, tab );

                if( nullable ) {
                    c.append( "} else {" );
                    printDefaultValue( c, line.defaultValue, line );
                    if( !map.ignoreDefaultValue() ) printDelimiter( delimiter, c, last, tab );
                    tab( c, tab ).append( "}\n" );
                }
            }
        } else {
            tab( c, tab );
            map.map( c, cc, line, pfield, delimiter, join );
            printDelimiter( delimiter, c, last, tab );
        }
    }

    private void printDefaultValue( StringBuilder c, Object pfield, TLine line ) {
        if( !map.ignoreDefaultValue() ) {
            c.append( "acc.accept( " );
            if( ClassUtils.isPrimitiveOrWrapper( pfield.getClass() ) ) c.append( pfield );
            else map.function( c, line.function, () -> c.append( "\"" ).append( pfield ).append( "\"" ) );
            c.append( " );\n" );
        } else c.append( "{}\n" );
    }

    private boolean isOptional( Type type ) {
        return type instanceof ParameterizedType
            && ( ( ParameterizedType ) type ).getRawType().equals( Optional.class );
    }

    @Override
    public <R> R render( T source, Accumulator<R> accumulator ) {
        return ( R ) func.apply( source, accumulator );
    }

    @Override
    public String renderString( T source ) {
        return render( source, new StringAccumulator() );
    }

    private static class FieldStack {
        private Stack<HashMap<String, Pair<Type, String>>> stack = new Stack<>();

        FieldStack() {
            stack.push( new HashMap<>() );
        }

        public FieldStack up() {
            stack.push( new HashMap<>( stack.peek() ) );

            return this;
        }

        public FieldStack down() {
            stack.pop();

            return this;
        }

        public Pair<Type, String> computeIfAbsent( String key, Function<String, Pair<Type, String>> func ) {
            Pair<Type, String> v = stack.peek().get( key );
            if( v == null ) {
                v = func.apply( key );
                stack.peek().put( key, v );
            }
            return v;
        }

        public Pair<Type, String> get( String key ) {
            return stack.peek().get( key );
        }
    }
}
