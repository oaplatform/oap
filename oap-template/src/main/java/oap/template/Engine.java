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

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import oap.io.Files;
import org.apache.commons.lang3.StringUtils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static java.util.Collections.emptyMap;
import static oap.template.Template.Line.line;
import static oap.template.TemplateStrategy.DEFAULT;

/**
 * Created by igor.petrenko on 15.06.2017.
 */
@Slf4j
public class Engine {
    private final static HashMap<String, String> builtInFunction = new HashMap<>();

    static {
        builtInFunction.put( "urlencode", "oap.template.Macros.encode" );
    }

    public final Path tmpPath;
    public final boolean cleanTmpBeforeRun;

    public Engine( Path tmpPath, boolean cleanTmpBeforeRun ) {
        this.tmpPath = tmpPath;
        this.cleanTmpBeforeRun = cleanTmpBeforeRun;

        start();
    }

    private void start() {
        if( cleanTmpBeforeRun ) Files.cleanDirectory( tmpPath );
    }

    public <T, TLine extends Template.Line> Template<T, TLine> getTemplate( String name, Class<T> clazz, List<TLine> pathAndDefault, String delimiter,
                                                                            TemplateStrategy<TLine> map ) {
        return new Template<>( name, clazz, pathAndDefault, delimiter, map, emptyMap(), emptyMap(), tmpPath );
    }

    public <T> Template<T, Template.Line> getTemplate( String name, Class<T> clazz, List<Template.Line> pathAndDefault, String delimiter ) {
        return getTemplate( name, clazz, pathAndDefault, delimiter, DEFAULT );
    }

    public <T> Template<T, Template.Line> getTemplate( String name, Class<T> clazz, String template ) {
        return getTemplate( name, clazz, template, emptyMap(), emptyMap() );
    }

    public <T> Template<T, Template.Line> getTemplate( String name, Class<T> clazz, String template,
                                                       Map<String, String> overrides, Map<String, Supplier<String>> mapper ) {
        boolean variable = false;
        StringBuilder function = null;

        ArrayList<Template.Line> lines = new ArrayList<>();
        StringBuilder text = new StringBuilder();

        for( int i = 0; i < template.length(); i++ ) {
            val ch = template.charAt( i );
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

        return new Template<>( name, clazz, lines, null, DEFAULT, overrides, mapper, tmpPath );
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
        boolean args = false;
        val name = new StringBuilder();
        val arguments = new StringBuilder();

        for( int i = 0; i < function.length(); i++ ) {
            val ch = function.charAt( i );
            switch( ch ) {
                case '(':
                    args = true;
                    break;
                case ')':
                    String funcName = StringUtils.trim( name.toString() );
                    funcName = builtInFunction.getOrDefault( funcName, funcName );

                    return new Template.Line.Function( funcName,
                        StringUtils.isBlank( arguments ) ? null : arguments.toString() );
                default:
                    ( args ? arguments : name ).append( ch );
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
}
