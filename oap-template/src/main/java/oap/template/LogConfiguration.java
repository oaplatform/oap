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

import com.google.common.base.Preconditions;
import com.google.common.primitives.Primitives;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import oap.dictionary.Configuration;
import oap.dictionary.Dictionary;
import oap.reflect.TypeRef;
import oap.util.Lists;
import oap.util.Pair;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.ParameterizedType;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.function.Predicate;

import static oap.dictionary.DictionaryParser.INCREMENTAL_ID_STRATEGY;
import static oap.template.ErrorStrategy.ERROR;
import static oap.util.Pair.__;

/**
 * Created by igor.petrenko on 2020-07-15.
 * No longer needed, see the latest logstream
 */
@Slf4j
@Deprecated
public class LogConfiguration extends Configuration {
    public static final HashMap<String, TypeConfiguration> types = new HashMap<>();
    public static final String COLLECTION_SUFFIX = "_ARRAY";

    public static class TypeConfiguration {
        public final String javaType;
        public final Types templateType;

        public TypeConfiguration( String javaType, Types templateType ) {
            this.javaType = javaType;
            this.templateType = templateType;
        }
    }

    static {
        types.put( "DATETIME", new TypeConfiguration( "org.joda.time.DateTime", Types.DATETIME ) );
        types.put( "BOOLEAN", new TypeConfiguration( "java.lang.Boolean", Types.BOOLEAN ) );
        types.put( "ENUM", new TypeConfiguration( "java.lang.Enum", Types.STRING ) );
        types.put( "STRING", new TypeConfiguration( "java.lang.String", Types.STRING ) );
        types.put( "LONG", new TypeConfiguration( "java.lang.Long", Types.LONG ) );
        types.put( "INTEGER", new TypeConfiguration( "java.lang.Integer", Types.INTEGER ) );
        types.put( "SHORT", new TypeConfiguration( "java.lang.Short", Types.SHORT ) );
        types.put( "FLOAT", new TypeConfiguration( "java.lang.Float", Types.FLOAT ) );
        types.put( "DOUBLE", new TypeConfiguration( "java.lang.Double", Types.DOUBLE ) );
    }

    private static final String LOG_TAG = "LOG";

    public static final Predicate<Dictionary> FILTER_TAG_NE_SYSTEM = dictionary -> !dictionary.getTags().contains( "system" );
    public static final Predicate<Dictionary> FILTER_LOG_TAG = d -> d.getTags().contains( LOG_TAG );

    private static final String STANDARD_DELIMITER = "\t";
    private final TemplateEngine engine;
    public boolean typeValidation = true;

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

    public <F, TOut, TMutable, TA extends TemplateAccumulator<TOut, TMutable, TA>> DictionaryTemplate<F, TOut, TMutable, TA> forType(
        TypeRef<F> clazz, String type,
        TA templateAccumulator ) {
        return forType( clazz, type, d -> true, templateAccumulator );
    }

    public <F, TOut, TMutable, TA extends TemplateAccumulator<TOut, TMutable, TA>> DictionaryTemplate<F, TOut, TMutable, TA> forType(
        TypeRef<F> clazz, String type,
        Predicate<Dictionary> predicate,
        TA templateAccumulator ) {

        var value = getLatestDictionary().getValue( type );

        if( value == null ) throw new IllegalArgumentException( "Unknown type " + type );

        var headers = new ArrayList<String>();
        var rowTypes = new ArrayList<Byte[]>();
        var cols = new ArrayList<Pair<String, String>>();

        for( var field : value.getValues( predicate ) ) {
            if( !field.containsProperty( "path" ) ) continue;

            var id = field.getId();
            var path = checkStringAndGet( field, "path" );
            var fieldType = checkStringAndGet( field, "type" );
            var format = field.getProperty( "format" ).orElse( null );

            boolean collection = false;
            var idType = fieldType;
            if( idType.endsWith( COLLECTION_SUFFIX ) ) {
                collection = true;
                idType = idType.substring( 0, idType.length() - COLLECTION_SUFFIX.length() );
            }

            var rowType = types.get( idType );
            Preconditions.checkNotNull( rowType, "unknown type " + idType );
            var defaultValue = field.getProperty( "default" )
                .orElseThrow( () -> new IllegalStateException( "default not found for " + type + "/" + id ) );

            var templateFunction = format != null ? "; format(\"" + format + "\")" : "";

            var comment = "model '" + type + "' id '" + id + "' type '" + fieldType + "' defaultValue '" + defaultValue + "'";

            var pDefaultValue = defaultValue instanceof String ? "\"" + ( ( String ) defaultValue ).replace( "\"", "\\\"" ) + '"' : defaultValue;
            cols.add( __( path, "${/* " + comment + " */" + toJavaType( rowType.javaType, collection ) + path + " ?? " + pDefaultValue + templateFunction + "}" ) );
            headers.add( id );
            if( collection ) {
                rowTypes.add( new Byte[] { Types.LIST.id, rowType.templateType.id } );
            } else {
                rowTypes.add( new Byte[] { rowType.templateType.id } );
            }
        }

        var template = String.join( templateAccumulator.delimiter(), Lists.map( cols, p -> p._2 ) );
        var templateFunc = engine.getTemplate(
            "Log" + StringUtils.capitalize( type ),
            clazz,
            template,
            templateAccumulator,
            ERROR,
            null );
        return new DictionaryTemplate<>( templateFunc, template, headers, rowTypes );
    }

    private static String checkStringAndGet( Dictionary dictionary, String fieldName ) {
        Object fieldObject = dictionary.getProperty( fieldName ).orElseThrow( () -> new TemplateException( dictionary.getId() + ": type is required" ) );
        Preconditions.checkArgument( fieldObject instanceof String, dictionary.getId() + ": type must be String, but is " + fieldObject.getClass() );
        return ( String ) fieldObject;
    }

    private String toJavaType( String javaType, boolean collection ) {
        if( !typeValidation ) return "";

        StringBuilder sb = new StringBuilder( "<" );
        if( collection ) sb.append( "java.util.Collection<" );
        sb.append( javaType );
        if( collection ) sb.append( ">" );
        sb.append( ">" );
        return sb.toString();
    }

    public TemplateEngine getEngine() {
        return engine;
    }

    @ToString
    @EqualsAndHashCode
    public static class FieldType {
        public final Class<?> type;
        public final ArrayList<FieldType> parameters = new ArrayList<>();

        public FieldType( Class<?> type ) {
            this.type = type;
        }

        public FieldType( Class<?> type, Collection<FieldType> parameters ) {
            this.type = type;
            this.parameters.addAll( parameters );
        }

        public static FieldType parse( String type ) throws ClassNotFoundException {
            var generics = new ArrayList<FieldType>();
            String baseType = type;

            var genericIndex = type.indexOf( '<' );
            if( genericIndex > 0 ) {
                baseType = type.substring( 0, genericIndex );
                var endGenericIndex = type.lastIndexOf( '>' );
                var genericString = type.substring( genericIndex + 1, endGenericIndex );

                int lastIndex = 0;
                while( true ) {
                    int ch = genericString.indexOf( ',', lastIndex );
                    if( ch < 0 ) break;

                    String g = genericString.substring( lastIndex, ch );
                    lastIndex = ch + 1;

                    generics.add( parse( g ) );
                }

                generics.add( parse( genericString.substring( lastIndex ) ) );
            }
            return new FieldType( Class.forName( baseType ), generics );
        }

        public boolean isAssignableFrom( TemplateType templateType ) {
            if( !type.isAssignableFrom( Primitives.wrap( templateType.getTypeClass() ) ) ) return false;

            if( !parameters.isEmpty() || templateType.type instanceof ParameterizedType ) {
                if( !parameters.isEmpty() && !( templateType.type instanceof ParameterizedType ) ) return false;

                List<TemplateType> actualArguments = templateType.getActualArguments();
                if( actualArguments.size() != parameters.size() ) return false;

                for( int i = 0; i < parameters.size(); i++ ) {
                    if( !parameters.get( 0 ).isAssignableFrom( actualArguments.get( i ) ) ) return false;
                }
            }

            return true;
        }
    }
}
