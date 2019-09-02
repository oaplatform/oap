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

package oap.dictionary;

import oap.template.Engine;
import oap.template.Template;
import oap.template.TemplateStrategy;
import org.apache.commons.lang3.StringUtils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.function.Predicate;

import static oap.dictionary.DictionaryParser.INCREMENTAL_ID_STRATEGY;

public class LogConfiguration extends Configuration {
    public static final Predicate<Dictionary> FILTER_TAG_NE_SYSTEM = ( dictionary ) -> !dictionary.getTags().contains( "system" );

    public static final int MAX_VERSIONS_TO_LOAD = 5;
    private static final String STANDARD_DELIMITER = "\t";
    private final Engine engine;

    public LogConfiguration( Engine engine ) {
        this( engine, null );
    }

    public LogConfiguration( Engine engine, Path mappingLocation ) {
        this( engine, mappingLocation, MAX_VERSIONS_TO_LOAD );
    }

    public LogConfiguration( Engine engine, Path mappingLocation, int maxVersionsToLoad ) {
        this( engine, mappingLocation, "logconfig", maxVersionsToLoad );
    }

    public LogConfiguration( Engine engine, Path mappingLocation, String resourceLocation, int maxVersionsToLoad ) {
        super( mappingLocation, resourceLocation, maxVersionsToLoad, INCREMENTAL_ID_STRATEGY );
        this.engine = engine;
    }

    public LogConfiguration( Engine engine, Path mappingLocation, String resourceLocation ) {
        this( engine, mappingLocation, resourceLocation, MAX_VERSIONS_TO_LOAD );
    }

    public String getStandardDelimiter() {
        return LogConfiguration.STANDARD_DELIMITER;
    }

    public <F> DictionaryTemplate<F, Template.Line> forType( Class<F> clazz, String type ) {
        return forType( clazz, type, dictionary -> true );
    }

    public <F> DictionaryTemplate<F, Template.Line> forType( Class<F> clazz, String type, Predicate<Dictionary> predicate ) {
        return forType( clazz, type, predicate, TemplateStrategy.DEFAULT );
    }

    public <F> DictionaryTemplate<F, Template.Line> forType( Class<F> clazz, String type, Predicate<Dictionary> predicate, TemplateStrategy<Template.Line> strategy ) {
        final Dictionary value = getLatestDictionary().getValue( type );

        if( value == null ) throw new IllegalArgumentException( "Unknown type " + type );

        var lines = new ArrayList<Template.Line>();

        for( Dictionary field : value.getValues( predicate ) ) {
            if( !field.containsProperty( "path" ) ) continue;

            final String id = field.getId();
            final String path = ( String ) field.getProperty( "path" ).get();
            final Object defaultValue = field.getProperty( "default" )
                .orElseThrow( () -> new IllegalStateException( "default not found for " + type + "/" + id ) );

            lines.add( new Template.Line( id, path, defaultValue ) );
        }

        return new DictionaryTemplate<>( engine.getTemplate( "Log" + StringUtils.capitalize( type ), clazz, lines,
            getStandardDelimiter(), strategy ), lines );
    }
}
