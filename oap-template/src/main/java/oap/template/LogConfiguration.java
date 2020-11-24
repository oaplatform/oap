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
import oap.dictionary.Configuration;
import oap.dictionary.Dictionary;
import oap.reflect.TypeRef;
import org.apache.commons.lang3.StringUtils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.StringJoiner;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static oap.dictionary.DictionaryParser.INCREMENTAL_ID_STRATEGY;
import static oap.template.ErrorStrategy.ERROR;
import static oap.template.TemplateAccumulators.STRING;

/**
 * Created by igor.petrenko on 2020-07-15.
 */
@Slf4j
public class LogConfiguration extends Configuration {
    public static final Predicate<Dictionary> FILTER_TAG_NE_SYSTEM = ( dictionary ) -> !dictionary.getTags().contains( "system" );

    private static final String STANDARD_DELIMITER = "\t";
    private final TemplateEngine engine;
    public boolean compact = false;

    public LogConfiguration( TemplateEngine engine ) {
        this( engine, null );
    }

    public LogConfiguration( TemplateEngine engine, Path mappingLocation ) {
        this( engine, mappingLocation, "logconfig" );
    }

    public LogConfiguration( TemplateEngine engine, Path mappingLocation, String resourceLocation ) {
        super( mappingLocation, resourceLocation, INCREMENTAL_ID_STRATEGY );
        this.engine = engine;
    }

    public String getStandardDelimiter() {
        return STANDARD_DELIMITER;
    }

    public <F> DictionaryTemplate<F> forType( TypeRef<F> clazz, String type ) {
        return forType( clazz, type, dictionary -> true );
    }

    public <F> DictionaryTemplate<F> forType( TypeRef<F> clazz, String type, Predicate<Dictionary> predicate ) {
        return forType( clazz, type, predicate, STRING );
    }

    public <F> DictionaryTemplate<F> forType( TypeRef<F> clazz, String type, Predicate<Dictionary> predicate,
                                              TemplateAccumulatorString templateAccumulator ) {
        var value = getLatestDictionary().getValue( type );

        if( value == null ) throw new IllegalArgumentException( "Unknown type " + type );

        var headers = new StringJoiner( "\t" );
        var cols = new ArrayList<String>();

        for( var field : value.getValues( predicate ) ) {
            if( !field.containsProperty( "path" ) ) continue;

            var id = field.getId();
            var path = ( String ) field.getProperty( "path" ).get();
            var defaultValue = field.getProperty( "default" )
                .orElseThrow( () -> new IllegalStateException( "default not found for " + type + "/" + id ) );

            var pDefaultValue = defaultValue instanceof String ? "\"" + ( ( String ) defaultValue ).replace( "\"", "\\\"" ) + '"' : defaultValue;
            cols.add( "${" + path + " ?? " + pDefaultValue + "}" );
            headers.add( id );
        }

        if( compact ) cols.sort( String::compareTo );

        var template = String.join( "\t", cols );
        var templateFunc = engine.getTemplate(
            "Log" + StringUtils.capitalize( type ),
            clazz,
            template,
            templateAccumulator,
            ERROR,
            compact ? CompactAstPostProcessor.INSTANCE : null );
        return new DictionaryTemplate<>( templateFunc, template, headers.toString() );
    }

    public TemplateEngine getEngine() {
        return engine;
    }

    public static class CompactAstPostProcessor implements Consumer<Ast> {
        public static final CompactAstPostProcessor INSTANCE = new CompactAstPostProcessor();

        @Override
        public void accept( Ast ast ) {
            var children = ast.children;

            for( var i = 0; i < children.size(); i++ ) {
                var left = children.get( i );
                if( left instanceof AstText ) continue;

                if( i >= children.size() - 1 ) return;
                i++;
                AstText astText = null;
                var right = children.get( i );
                if( right instanceof AstText ) {
                    astText = ( AstText ) right;
                    if( i >= children.size() - 1 ) return;
                    i++;
                    right = children.get( i );
                }

                if( merge( left, right, astText ) ) {
                    children.remove( i );
                    i--;

                    if( astText != null ) {
                        children.remove( i );
                        i--;
                    }

                    i--;
                }
            }

            log.trace( "\n --- compact ---\n" + ast.print() );
        }

        public boolean merge( Ast root, Ast ast, AstText optText ) {
            if( !( root.getClass().equals( ast.getClass() ) ) ) return false;

            if( root instanceof AstExpression ) {
                if( ast.children.size() == 1 && root.children.size() > 0 ) {
                    var c = ast.children.get( 0 );
                    var sc = root.children.get( 0 );
                    if( sc.equalsAst( c ) ) {
                        merge( sc, c, optText );
                        ( ( AstExpression ) root ).content.addAll( ( ( AstExpression ) ast ).content );
                        return true;
                    }
                }

                return false;
            } else if( root instanceof AstIfElse ) {
                var r = defaultMerge( root, ast, optText );

                if( optText != null ) {
                    var astIfElse = ( AstIfElse ) root;
                    if( astIfElse.printIfOptEmpty == null )
                        astIfElse.printIfOptEmpty = optText;
                    else astIfElse.printIfOptEmpty = new AstText( astIfElse.printIfOptEmpty.text + optText.text );
                }

                return r;
            } else {
                return defaultMerge( root, ast, optText );
            }
        }


        private boolean defaultMerge( Ast root, Ast ast, AstText optText ) {
            if( ast.children.size() == 1 && root.children.size() > 0 ) {
                var c = ast.children.get( 0 );
                var sc = root.children.get( 0 );
                if( sc.equalsAst( c ) ) {
                    merge( sc, c, optText );
                    return true;
                } else {
                    if( optText != null ) root.children.add( 0, optText );
                    root.children.add( 0, c );
                }
            }
            return false;
        }
    }
}
