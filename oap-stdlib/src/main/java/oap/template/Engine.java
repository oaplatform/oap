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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.hash.Hashing;
import io.micrometer.core.instrument.Metrics;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import oap.concurrent.StringBuilderPool;
import oap.util.Dates;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static oap.template.Template.Line.line;
import static oap.template.TemplateStrategy.DEFAULT;

@Slf4j
/**
 *    metrics:
 *      - oap_templates_cache_size
 */
public class Engine implements Runnable {
    private final static HashMap<String, String> builtInFunction = new HashMap<>();

    static {
        builtInFunction.put( "urlencode", "oap.template.Macros.encode" );
    }

    public final Path tmpPath;
    public final long ttl;
    private final Cache<String, Template<?, ? extends Template.Line>> templates;


    public Engine( Path tmpPath ) {
        this( tmpPath, Dates.d( 30 ) );
    }

    public Engine( Path tmpPath, long ttl ) {
        this.tmpPath = tmpPath;
        this.ttl = ttl;

        templates = CacheBuilder.newBuilder()
            .expireAfterAccess( ttl, TimeUnit.MILLISECONDS )
            .build();

        Metrics.gauge( "oap_templates_cache_size", templates, Cache::size );
    }

    public static String getHashName( String template ) {
        var hashFunction = Hashing.murmur3_128();

        var hash = hashFunction.hashUnencodedChars( template ).asLong();
        return hashToName( hash );
    }

    private static String hashToName( long hash ) {
        return "template_" + ( hash >= 0 ? String.valueOf( hash ) : "_" + String.valueOf( hash ).substring( 1 ) );
    }

    public static <TLine extends Template.Line> String getHashName( List<TLine> pathAndDefault, String delimiter ) {
        var hashFunction = Hashing.murmur3_128();

        var hash = hashFunction.newHasher();


        for( var line : pathAndDefault ) {
            hash.putUnencodedChars( line.path );
        }

        hash.putUnencodedChars( delimiter );

        return hashToName( hash.hash().asLong() );
    }

    public long getCacheSize() {
        return templates.size();
    }

    @SuppressWarnings( "unchecked" )
    @SneakyThrows
    public <T, TLine extends Template.Line> Template<T, TLine> getTemplate( String name, Class<T> clazz,
                                                                            List<TLine> pathAndDefault, String delimiter,
                                                                            TemplateStrategy<TLine> map ) {
        if( pathAndDefault.size() == 1 && pathAndDefault.get( 0 ).path == null ) {
            return new ConstTemplate<>( pathAndDefault.get( 0 ).defaultValue );
        }

        var id = name + "_" + getHashName( pathAndDefault, delimiter );

        var func = ( Template<T, TLine> ) templates.getIfPresent( id );
        if( func == null ) {
            synchronized( id.intern() ) {
                func = ( Template<T, TLine> ) templates.get( id, () ->
                    new JavaCTemplate<>( id, clazz, pathAndDefault, delimiter, map, Map.of(), tmpPath ) );
            }
        }

        return func;
    }

    public <T> Template<T, Template.Line> getTemplate( String name, Class<T> clazz,
                                                       List<Template.Line> pathAndDefault, String delimiter ) {
        return getTemplate( name, clazz, pathAndDefault, delimiter, DEFAULT );
    }

    public <T> Template<T, Template.Line> getTemplate( String name, Class<T> clazz, String template ) {
        return getTemplate( name, clazz, template, Map.of(), DEFAULT );
    }

    public <T> Template<T, Template.Line> getTemplate( String name, Class<T> clazz, String template, Map<String, String> overrides ) {
        return getTemplate( name, clazz, template, overrides, DEFAULT );
    }

    @SuppressWarnings( "unchecked" )
    @SneakyThrows
    public <T> Template<T, Template.Line> getTemplate( String name, Class<T> clazz, String template,
                                                       Map<String, String> overrides, TemplateStrategy<Template.Line> map ) {

        var id = name + "_" + getHashName( template );

        var func = ( Template<T, Template.Line> ) templates.getIfPresent( id );
        if( func == null ) {
            synchronized( id.intern() ) {
                func = ( Template<T, Template.Line> ) templates.get( id, () -> {
                    var variable = false;
                    StringBuilder function = null;

                    var lines = new ArrayList<Template.Line>();
                    try( var sbp = StringBuilderPool.borrowObject() ) {
                        var text = sbp.getObject();

                        for( var i = 0; i < template.length(); i++ ) {
                            var ch = template.charAt( i );
                            switch( ch ) {
                                case '$':
                                    if( variable ) {
                                        variable = false;
                                        add( text, ch, function );
                                    } else {
                                        variable = true;
                                    }
                                    break;
                                case '{':
                                    if( variable )
                                        startVariable( lines, text );
                                    else
                                        add( text, ch, function );
                                    break;
                                case '}':
                                    if( variable ) {
                                        endVariable( lines, text, function, true );
                                        variable = false;
                                        function = null;
                                    } else add( text, ch, function );
                                    break;
                                case ';':
                                    if( variable ) {
                                        function = new StringBuilder();
                                    } else {
                                        add( text, ch, function );
                                    }
                                    break;
                                default:
                                    add( text, ch, function );
                            }
                        }

                        endVariable( lines, text, function, false );
                    }

                    if( lines.size() == 1 && lines.get( 0 ).path == null ) {
                        return new ConstTemplate<>( lines.get( 0 ).defaultValue );
                    }

                    return new JavaCTemplate<>( id, clazz, lines, null, map, overrides, tmpPath );
                } );
            }
        }

        return func;
    }

    private void add( StringBuilder text, char ch, StringBuilder function ) {
        if( function != null ) function.append( ch );
        else text.append( ch );

    }

    private void endVariable( ArrayList<Template.Line> lines, StringBuilder text, StringBuilder function, boolean variable ) {
        if( text.length() > 0 ) {
            lines.add( line( text.toString(),
                variable ? text.toString() : null,
                variable ? "" : text.toString(), function != null ? getFunction( function ) : null ) );

            text.replace( 0, text.length(), "" );
        }
    }

    private Template.Line.Function getFunction( CharSequence function ) {
        var args = false;
        var name = new StringBuilder();
        var arguments = new StringBuilder();

        for( int i = 0; i < function.length(); i++ ) {
            var ch = function.charAt( i );
            switch( ch ) {
                case '(' -> args = true;
                case ')' -> {
                    var funcName = StringUtils.trim( name.toString() );
                    funcName = builtInFunction.getOrDefault( funcName, funcName );
                    return new Template.Line.Function( funcName,
                        StringUtils.isBlank( arguments ) ? null : arguments.toString() );
                }
                default -> ( args ? arguments : name ).append( ch );
            }

        }

        log.trace( "function = {}", function );

        throw new IllegalArgumentException( "syntax error: " + function );
    }

    private void startVariable( ArrayList<Template.Line> lines, StringBuilder text ) {
        if( text.length() > 0 ) {
            lines.add( new Template.Line( "text", null, text.toString() ) );
            text.replace( 0, text.length(), "" );
        }
    }

    @Override
    public void run() {
        try {
            templates.cleanUp();

            var now = System.currentTimeMillis();
            Files.walk( tmpPath ).forEach( path -> {
                try {
                    if( now - Files.getLastModifiedTime( path ).toMillis() > ttl ) {
                        log.debug( "delete {}", path );
                        Files.deleteIfExists( path );
                    }
                } catch( IOException e ) {
                    log.error( e.getMessage() );
                }
            } );
        } catch( IOException e ) {
            log.error( e.getMessage(), e );
        }
    }
}
