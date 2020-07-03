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
import oap.concurrent.StringBuilderPool;
import oap.json.ext.ExtDeserializer;
import oap.tools.MemoryClassLoaderJava13;
import oap.util.Functions;
import oap.util.Pair;
import oap.util.Try;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.commons.text.StringEscapeUtils;

import java.io.BufferedReader;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.stream.Collectors.joining;
import static oap.util.Pair.__;

@Slf4j
public class JavaCTemplate<T, L extends Template.Line> implements Template<T, L> {
    private static final String math = "/*+-%";
    private final Map<String, String> overrides;
    private final Functions.TriFunction<T, Map<String, Supplier<String>>, Accumulator<?>, ?> func;
    private final TemplateStrategy<L> map;

    @SneakyThrows
    @SuppressWarnings( "unchecked" )
    JavaCTemplate( String name, Class<T> clazz, List<L> pathAndDefault, String delimiter, TemplateStrategy<L> map,
                   Map<String, String> overrides,
                   Path cacheFile ) {
        String nameEscaped = name.replaceAll( "[\\s%\\-;\\\\/:*?\"<>|]", "_" );
        this.map = map;
        this.overrides = overrides;
        var c = new StringBuilder();

        try {
            var num = new AtomicInteger();
            var fields = new FieldStack();

            var className = clazz.getName().replace( '$', '.' );

            c.append( "package " ).append( getClass().getPackage().getName() ).append( ";\n"
                + "\n"
                + "import oap.util.Strings;\n"
                + "import oap.concurrent.StringBuilderPool;\n"
                + "\n"
                + "import java.util.*;\n"
                + "import oap.util.Functions.TriFunction;\n"
                + "import java.util.function.Supplier;\n"
                + "import com.google.common.base.CharMatcher;\n"
                + "\n"
                + "public  class " ).append( nameEscaped ).append( " implements TriFunction<" ).append( className ).append( ", Map<String, Supplier<String>>, Accumulator, Object> {\n"
                + "\n"
                + "  @Override\n"
                + "  public Object apply( " ).append( className ).append( " s, Map<String, Supplier<String>> m, Accumulator acc ) {\n"
                + "    try(var jbPool = StringBuilderPool.borrowObject()) {\n"
                + "      var jb = jbPool.getObject();\n"
                + "\n" );

            int size = pathAndDefault.size();
            for( int x = 0; x < size; x++ ) {
                addPath( clazz, pathAndDefault.get( x ), delimiter, c, num, fields, x + 1 >= size );
            }


            c.append( """

                      return acc.get();
                    }
                  }
                }""".stripIndent() );

            var line = new AtomicInteger( 0 );
            log.trace( "\n{}", new BufferedReader( new StringReader( c.toString() ) )
                .lines()
                .map( l -> String.format( "%3d", line.incrementAndGet() ) + " " + l )
                .collect( joining( "\n" ) )
            );

            var fullTemplateName = getClass().getPackage().getName() + "." + nameEscaped;
            var mcl = new MemoryClassLoaderJava13( fullTemplateName, c.toString(), cacheFile );
            func = ( Functions.TriFunction<T, Map<String, Supplier<String>>, Accumulator<?>, ?> ) mcl.loadClass( fullTemplateName ).getDeclaredConstructor().newInstance();

        } catch( Exception e ) {
            log.error( c.toString() );
            throw e;
        }
    }

    private void addPath( Class<T> clazz, L line, String delimiter, StringBuilder c,
                          AtomicInteger num, FieldStack fields,
                          boolean last ) throws NoSuchMethodException, NoSuchFieldException {
        var tab = new AtomicInteger( 6 );

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
                if( map.ifPathNotFoundGetFromMapper() ) {
                    var mNum = num.incrementAndGet();
                    tab( c, tab ).append( "// mapper\n" );
                    tab( c, tab ).append( "Supplier<String> mapperSupplier" ).append( mNum ).append( " = m.get( \"" ).append( line.path ).append( "\" );\n" );
                    tab( c, tab ).append( "if( mapperSupplier" ).append( mNum ).append( " == null ) {\n" );
                    tabInc( tab );
                    tab( c, tab );
                    map.pathNotFound( c, line.path ).append( "\n" );
                    tabDec( tab );
                    tab( c, tab ).append( "} else {\n" );
                    tabInc( tab );
                    __tab( c, tab ).append( "String mapper" ).append( mNum ).append( " = mapperSupplier" ).append( mNum ).append( ".get();\n" );
                    if( line.function != null ) {
                        __tab( c, tab ).append( "String mapperFunction" ).append( mNum ).append( " = " );
                        map.function( c, line.function, () -> c.append( "mapper" ).append( mNum ) ).append( ";\n" );
                    }
                    __tab( c, tab ).append( "acc.accept( mapper" );
                    if( line.function != null ) c.append( "Function" );
                    c.append( mNum ).append( " );\n" );
                    tabDec( tab );
                    tab( c, tab ).append( "}\n" );
                } else {
                    log.warn( "path {} not found", line );
                    tab( c, tab );
                    map.pathNotFound( c, line.path ).append( "\n" );
                }
            }
        }
    }

    private void buildPath( Class<T> clazz, L line, String delimiter, StringBuilder c,
                            AtomicInteger num, FieldStack fields, boolean last, AtomicInteger tab, boolean validation ) throws NoSuchFieldException, NoSuchMethodException {
        var orPath = StringUtils.split( line.path, '|' );
        var orIndex = 0;

        for( var i = 0; i < orPath.length; i++ ) {
            var path = orPath[i].trim();
            var newPath = overrides.get( path );
            orPath[i] = newPath != null ? newPath : path;
        }

        if( !validation ) map.beforeLine( c, line, delimiter );
        addPathOr( new FieldInfo( clazz ), delimiter, c, num, fields, last, tab, orPath, orIndex, line );
        if( !validation ) map.afterLine( c, line, delimiter );
    }

    private boolean pathExists( Class<T> clazz, L line ) throws NoSuchFieldException, NoSuchMethodException {
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

    private void addPathOr( FieldInfo clazz, String delimiter, StringBuilder c, AtomicInteger num,
                            FieldStack fields, boolean last, AtomicInteger tab,
                            String[] orPath, int orIndex, L line ) throws NoSuchFieldException, NoSuchMethodException {
        var currentPath = orPath[orIndex].trim();

        int sp = 0;
        StringBuilder newPath = new StringBuilder( "s." );
        MutableObject<FieldInfo> lc = new MutableObject<>( clazz );
        var psp = new AtomicInteger( 0 );
        var opts = new AtomicInteger( 0 );
        while( sp >= 0 ) {
            sp = currentPath.indexOf( '.', sp + 1 );

            if( sp > 0 ) {
                Pair<FieldInfo, String> pnp = fields.computeIfAbsent( currentPath.substring( 0, sp ), Try.map( ( key ) -> {
                    String prefix = StringUtils.trim( psp.get() > 1 ? key.substring( 0, psp.get() - 1 ) : "" );
                    String suffix = StringUtils.trim( key.substring( psp.get() ) );

                    boolean optional = lc.getValue().isOptional();
                    boolean nullable = lc.getValue().isNullable();
                    var declaredField = getDeclaredFieldOrFunctionType(
                        optional ? lc.getValue().getOptionalArgumentType() : lc.getValue(), suffix );

                    var classType = declaredField.toJavaType();

                    String field = "field" + num.incrementAndGet();

                    Optional<Pair<FieldInfo, String>> rfP = Optional.ofNullable( fields.get( prefix ) );
                    var rf = rfP.map( p -> p._2 + ( optional ? ".get()" : "" ) + "." + suffix ).orElse( "s." + key );

                    if( optional ) {
                        tab( c, tab ).append( "if( " ).append( rfP.map( p -> p._2 ).orElse( "s" ) ).append( ".isPresent() ) {\n" );
                        opts.incrementAndGet();
                        tabInc( tab );
                        fields.up();
                    } else if( nullable ) {
                        tab( c, tab ).append( "if( " ).append( rfP.map( p -> p._2 ).orElse( "s" ) ).append( " != null ) {\n" );
                        opts.incrementAndGet();
                        tabInc( tab );
                        fields.up();
                    }

                    tab( c, tab ).append( " " ).append( classType ).append( " " ).append( field ).append( " = " );
                    if(declaredField.typeCast) c.append( '(' ).append( classType ).append( ") " );
                    c.append( rf ).append( ";\n" );

                    lc.setValue( declaredField );
                    return __( declaredField, field );
                } ) );
                newPath = new StringBuilder( pnp._2 + "." );
                lc.setValue( pnp._1 );
                psp.set( sp + 1 );
            } else {
                newPath.append( currentPath.substring( psp.get() ) );
            }
        }

        var in = newPath.toString().lastIndexOf( '.' );
        String pField = in > 0 ? newPath.substring( 0, in ) : newPath.toString();
        String cField = newPath.substring( in + 1 );

        var isJoin = cField.startsWith( "{" );
        String[] cFields = isJoin
            ? StringUtils.split( cField.substring( 1, cField.length() - 1 ), ',' ) : new String[] { cField };

        FieldInfo parentClass = lc.getValue();
        var isOptionalParent = parentClass.isOptional() && !cField.startsWith( "isPresent" );
        var isNullableParent = parentClass.isNullable();
        String optField = null;
        if( isOptionalParent || isNullableParent ) {
            opts.incrementAndGet();
            tab( c, tab ).append( "if( " ).append( pField );
            if( isOptionalParent ) c.append( ".isPresent() ) {\n" );
            else c.append( " != null ) {\n" );
            fields.up();
            tabInc( tab );

            if( isOptionalParent )
                parentClass = parentClass.getOptionalArgumentType();
            optField = "opt" + num.incrementAndGet();
            tab( c, tab ).append( " " ).append( parentClass.toJavaType() ).append( " " ).append( optField ).append( " = " ).append( pField );
            if( isOptionalParent ) c.append( ".get();\n" );
            else c.append( ";\n" );
        }


        for( int i = 0; i < cFields.length; i++ ) {
            cField = StringUtils.trim( cFields[i] );

            Optional<Join> join = isJoin ? Optional.of( new Join( i, cFields.length ) ) : Optional.empty();
            if( cField.startsWith( "\"" ) ) {
                tab( c.append( "\n" ), tab );
                map.map( c, new FieldInfo( String.class ), line, cField, delimiter, join );
            } else {
                var isParentMap = parentClass.isMap();

                var ff = ( isParentMap ? "get( \"" + StringUtils.replace( cField, "((", "(" ) + "\" )" : cField );

                if( isOptionalParent || isNullableParent ) {
                    newPath = new StringBuilder( in > 0 ? optField + "." + ff : cField );
                } else {
                    newPath = new StringBuilder( in > 0 ? pField + "." + ff : "s." + ff );
                }

                var cc = in > 0 ? getDeclaredFieldOrFunctionType( parentClass, cField ) : parentClass;

                add( c, num, newPath.toString(), cc, true, tab, orPath, orIndex,
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
//        }
    }

    private void tabInc( AtomicInteger tab ) {
        tab.getAndUpdate( v -> v + 2 );
    }

    private void tabDec( AtomicInteger tab ) {
        tab.getAndUpdate( v -> v - 2 );
    }

    private StringBuilder __tab( StringBuilder sb, AtomicInteger tab ) {
        return tab( sb, tab );
    }

    private StringBuilder tab( StringBuilder sb, AtomicInteger tab ) {
        return sb.append( " ".repeat( Math.max( 0, tab.get() ) ) );
    }

    private FieldInfo getDeclaredFieldOrFunctionType( FieldInfo type, String field ) throws NoSuchFieldException, NoSuchMethodException {
        if( type.isParameterizedType() )
            return getDeclaredFieldOrFunctionType( type.getRawType(), field );

        int mi = StringUtils.indexOfAny( field, math );
        if( mi > 0 ) return getDeclaredFieldOrFunctionType( type, field.substring( 0, mi ) );

        Class<?> type1 = ( Class<?> ) type.type;
        try {
            int i = field.indexOf( '(' );
            while( i > 0 && field.charAt( i + 1 ) == '(' ) {
                i = field.indexOf( '(', i + 2 );
            }
            if( i < 0 ) {
                if( type1.isAssignableFrom( Map.class ) ) {
                    return new FieldInfo( field, Object.class, type.annotations );
                }
                var declaredField = type1.getDeclaredField( field );

                var extFieldInfo = getExt( type1, declaredField );
                if( extFieldInfo != null ) return extFieldInfo;

                return new FieldInfo( field, declaredField );
            } else {
                var f = field.substring( 0, i );
                var declaredMethod = type1.getDeclaredMethod( f );
                return new FieldInfo( f, declaredMethod.getGenericReturnType(), declaredMethod.getAnnotations() );
            }
        } catch( NoSuchMethodException | NoSuchFieldException e ) {
            if( type1.getSuperclass() != null && !type1.getSuperclass().equals( Object.class ) )
                return getDeclaredFieldOrFunctionType( new FieldInfo( field, type1.getGenericSuperclass(), type.annotations ), field );
            else
                throw e;
        }
    }

    private FieldInfo getExt( Class parent, Field declaredField ) {
        var extClass = ExtDeserializer.extensionOf( parent, declaredField.getName() );
        if( extClass == null ) return null;

        return new FieldInfo( declaredField.getName(), extClass, declaredField.getDeclaredAnnotations(), true );
    }

    private void add( StringBuilder c, AtomicInteger num, String newPath, FieldInfo cc,
                      boolean nullable, AtomicInteger tab,
                      String[] orPath, int orIndex, FieldInfo clazz, String delimiter,
                      FieldStack fields, boolean last, L line, Optional<Join> join ) throws NoSuchFieldException, NoSuchMethodException {
        String pfield = newPath;
        boolean primitive = cc.isPrimitive();
        if( !primitive ) {
            pfield = "field" + num.incrementAndGet();
            tab( c, tab ).append( " " ).append( cc.toJavaType() ).append( " " ).append( pfield ).append( " = " ).append( newPath ).append( ";\n" );
        }
        if( !primitive ) {
            if( cc.isOptional() || cc.isNullable() ) {
                FieldInfo cc1 = cc.getOptionalArgumentType();
                if( cc.isOptional() ) {
                    tab( c, tab ).append( "if( " ).append( pfield ).append( ".isPresent() ) {\n" );
                } else {
                    tab( c, tab ).append( "if( " ).append( pfield ).append( " != null ) {\n" );
                }
                fields.up();
                add( c, num, pfield + ".get()", cc1, false, new AtomicInteger( tab.get() + 2 ),
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

    private void printDefaultValue( StringBuilder c, Object pfield, L line ) {
        if( !map.ignoreDefaultValue() ) {
            c.append( "acc.accept( " );
            if( pfield == null ) c.append( "( Object ) null" );
            else if( ClassUtils.isPrimitiveOrWrapper( pfield.getClass() ) ) c.append( pfield );
            else map.function( c, line.function, () -> c.append( "\"" ).append( pfield ).append( "\"" ) );
            c.append( " );\n" );
        } else c.append( "{}\n" );
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public <R> R render( T source, Map<String, Supplier<String>> mapper, Accumulator<R> accumulator ) {
        return ( R ) func.apply( source, mapper, accumulator );
    }

    @Override
    public String renderString( T source, Map<String, Supplier<String>> mapper ) {
        try( var sbPool = StringBuilderPool.borrowObject() ) {
            return render( source, mapper, new StringAccumulator( sbPool.getObject() ) );
        }
    }

    private static class FieldStack {
        private Stack<HashMap<String, Pair<FieldInfo, String>>> stack = new Stack<>();

        FieldStack() {
            stack.push( new HashMap<>() );
        }

        public void up() {
            stack.push( new HashMap<>( stack.peek() ) );

        }

        public void down() {
            stack.pop();

        }

        public Pair<FieldInfo, String> computeIfAbsent( String key, Function<String, Pair<FieldInfo, String>> func ) {
            var v = stack.peek().get( key );
            if( v == null ) {
                v = func.apply( key );
                stack.peek().put( key, v );
            }
            return v;
        }

        public Pair<FieldInfo, String> get( String key ) {
            return stack.peek().get( key );
        }
    }

}
