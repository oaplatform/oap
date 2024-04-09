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

package oap.logstream.data.object;

import com.google.common.base.Preconditions;
import oap.dictionary.Dictionary;
import oap.dictionary.DictionaryRoot;
import oap.logstream.AbstractLoggerBackend;
import oap.logstream.AvailabilityReport;
import oap.net.Inet;
import oap.reflect.TypeRef;
import oap.template.Template;
import oap.template.TemplateAccumulatorBinary;
import oap.template.TemplateEngine;
import oap.template.TemplateException;
import oap.template.Types;
import oap.util.FastByteArrayOutputStream;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;
import static oap.logstream.LogStreamProtocol.CURRENT_PROTOCOL_VERSION;
import static oap.template.ErrorStrategy.ERROR;

/**
 * Class for beans described in the datamodel via oap-logstream protocol.
 */
public class BinaryObjectLogger {
    public static final String COLLECTION_SUFFIX = "_ARRAY";
    public final DictionaryRoot model;
    public final TemplateEngine engine;

    public final AbstractLoggerBackend backend;

    public static final HashMap<String, TypeConfiguration> types = new HashMap<>();

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

    public BinaryObjectLogger( DictionaryRoot model, AbstractLoggerBackend backend, TemplateEngine engine ) {
        this.model = model;
        this.backend = backend;
        this.engine = engine;
    }

    public BinaryObjectLogger( DictionaryRoot model, AbstractLoggerBackend backend, @Nonnull Path diskCache, long ttl ) {
        this( model, backend, new TemplateEngine( diskCache, ttl ) );
    }

    public <D> TypedBinaryLogger<D> typed( TypeRef<D> typeRef, String id ) {
        var value = requireNonNull( model.getValue( id ), "configuration for " + id + " is not found" );

        var headers = new ArrayList<String>();
        var rowTypes = new ArrayList<byte[]>();
        var expressions = new ArrayList<String>();

        for( var field : value.getValues( d -> d.containsProperty( "path" ) ) ) {
            var name = field.getId();
            var path = checkStringAndGet( field, "path" );
            var fieldType = checkStringAndGet( field, "type" );
            var format = field.getProperty( "format" ).orElse( null );

            boolean collection = false;
            var idType = fieldType;
            if( idType.endsWith( COLLECTION_SUFFIX ) ) {
                collection = true;
                idType = idType.substring( 0, idType.length() - COLLECTION_SUFFIX.length() );
            }

            TypeConfiguration rowType = types.get( idType );
            Preconditions.checkNotNull( rowType, "unknown type " + idType );

            var defaultValue = field.getProperty( "default" )
                .orElseThrow( () -> new IllegalStateException( "default not found for " + id + "/" + name ) );

            var templateFunction = format != null ? "; format(\"" + format + "\")" : "";
            var comment = "model " + id + " id " + name + " path " + path + " type " + fieldType + " defaultValue '" + defaultValue + "'";
            var pDefaultValue =
                defaultValue instanceof String ? "\"" + ( ( String ) defaultValue ).replace( "\"", "\\\"" ) + '"'
                    : defaultValue;

            expressions.add( "${/* " + comment + " */" + toJavaType( rowType.javaType, collection ) + path + " ?? " + pDefaultValue + templateFunction + "}" );
            headers.add( name );
            if( collection ) {
                rowTypes.add( new byte[] { Types.LIST.id, rowType.templateType.id } );
            } else {
                rowTypes.add( new byte[] { rowType.templateType.id } );
            }
        }

        var template = String.join( "", expressions );
        var renderer = engine.getTemplate(
            "Log" + StringUtils.capitalize( id ),
            typeRef,
            template,
            new TemplateAccumulatorBinary(),
            ERROR,
            null );
        return new TypedBinaryLogger<>( renderer, headers.toArray( new String[0] ), rowTypes.toArray( new byte[0][] ) );

    }

    public boolean isLoggingAvailable() {
        return backend.isLoggingAvailable();
    }

    public AvailabilityReport availabilityReport() {
        return backend.availabilityReport();
    }

    private static String checkStringAndGet( Dictionary dictionary, String fieldName ) {
        Object fieldObject = dictionary.getProperty( fieldName ).orElseThrow( () -> new TemplateException( dictionary.getId() + ": type is required" ) );
        Preconditions.checkArgument( fieldObject instanceof String, dictionary.getId() + ": type must be String, but is " + fieldObject.getClass() );
        return ( String ) fieldObject;
    }

    private String toJavaType( String javaType, boolean collection ) {
        StringBuilder sb = new StringBuilder( "<" );
        if( collection ) sb.append( "java.util.Collection<" );
        sb.append( javaType );
        if( collection ) sb.append( ">" );
        sb.append( ">" );
        return sb.toString();
    }

    public class TypedBinaryLogger<D> {
        private final Template<D, byte[], FastByteArrayOutputStream, TemplateAccumulatorBinary> renderer;
        public final String[] headers;
        public final byte[][] types;

        public TypedBinaryLogger( Template<D, byte[], FastByteArrayOutputStream, TemplateAccumulatorBinary> renderer, String[] headers, byte[][] types ) {
            this.renderer = renderer;

            this.headers = headers;
            this.types = types;
        }

        public void log( D data, String filePreffix, Map<String, String> properties, String logType ) {
            byte[] bytes = renderer.render( data, true ).getBytes();
            backend.log( CURRENT_PROTOCOL_VERSION, Inet.HOSTNAME, filePreffix, properties, logType, headers, types, bytes );
        }
    }
}
