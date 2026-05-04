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

package oap.logstream.formats.rowbinary;

import com.google.common.base.Preconditions;
import lombok.experimental.ExtensionMethod;
import oap.dictionary.Dictionary;
import oap.dictionary.DictionaryRoot;
import oap.logstream.AbstractLoggerBackend;
import oap.logstream.AvailabilityReport;
import oap.logstream.LogStreamProtocol;
import oap.net.Inet;
import oap.reflect.TypeRef;
import oap.template.Template;
import oap.template.TemplateEngine;
import oap.template.TemplateException;
import oap.template.Types;
import oap.util.FastByteArrayOutputStream;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Objects.requireNonNull;
import static oap.template.ErrorStrategy.ERROR;

/**
 * Class for beans described in the datamodel via oap-logstream protocol.
 */
@ExtensionMethod( RowBinaryObjectLogger.DictionaryExtensions.class )
public class RowBinaryObjectLogger {
    public static final String COLLECTION_SUFFIX = "_ARRAY";
    public static final HashMap<String, TypeConfiguration> types = new HashMap<>();

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

    public final DictionaryRoot model;
    public final TemplateEngine engine;
    public final AbstractLoggerBackend backend;

    public RowBinaryObjectLogger( DictionaryRoot model, AbstractLoggerBackend backend, TemplateEngine engine ) {
        this.model = model;
        this.backend = backend;
        this.engine = engine;
    }

    public RowBinaryObjectLogger( DictionaryRoot model, AbstractLoggerBackend backend, @Nonnull Path diskCache, long ttl ) {
        this( model, backend, new TemplateEngine( diskCache, ttl ) );
    }

    private static String checkStringAndGet( Dictionary dictionary, String fieldName ) {
        Object fieldObject = dictionary.getProperty( fieldName ).orElseThrow( () -> new TemplateException( dictionary.getId() + ": type is required" ) );
        Preconditions.checkArgument( fieldObject instanceof String, dictionary.getId() + ": type must be String, but is " + fieldObject.getClass() );
        return ( String ) fieldObject;
    }

    public <D> TypedRowBinaryLogger<D> typed( TypeRef<D> typeRef, String id, boolean sortByPath ) {
        return typed( typeRef, id, sortByPath, null );
    }

    public <D> TypedRowBinaryLogger<D> typed( TypeRef<D> typeRef, String id, boolean sortByPath, @Nullable RowBinaryObjectListener listener ) {
        Dictionary value = requireNonNull( model.getValue( id ), "configuration for " + id + " is not found" );

        ArrayList<String> headers = new ArrayList<>();
        ArrayList<byte[]> rowTypes = new ArrayList<>();
        ArrayList<String> expressions = new ArrayList<>();

        List<Dictionary> fields = value.getValues( d -> d.containsProperty( "path" ) );

        if( sortByPath ) {
            fields.sort( ( o1, o2 ) -> {
                String path1 = o1.getPath();
                String path2 = o2.getPath();

                return path1.compareTo( path2 );
            } );
        }

        LinkedHashMap<String, Dictionary> rootFields = new LinkedHashMap<>();
        List<Dictionary> nestedFields = new ArrayList<>();

        for( Dictionary field : fields ) {
            String path = field.getPath();
            int dotIdx = path.indexOf( '.' );
            int orIndex = path.indexOf( '|' );
            if( orIndex > 0 ) {
                rootFields.put( path, field );
            } else if( dotIdx < 0 ) {
                rootFields.put( path, field );
            } else {
                nestedFields.add( field );
            }
        }

        rootFields.forEach( ( path, field ) -> {
            appendField( path, field, id, null, headers, rowTypes, expressions );
        } );

        processNestedFields( nestedFields, null, id, headers, rowTypes, expressions );

        String template = String.join( "", expressions );

        if( listener != null ) listener.javaCode( template );

        Template<D, byte[], FastByteArrayOutputStream, TemplateAccumulatorRowBinary, ?> renderer = engine.getTemplate(
            "Log" + StringUtils.capitalize( id ),
            typeRef,
            template,
            new TemplateAccumulatorRowBinary(),
            ERROR,
            null,
            null );
        return new TypedRowBinaryLogger<>( renderer, headers.toArray( new String[0] ), rowTypes.toArray( new byte[0][] ) );
    }

    private void appendField( String path, Dictionary field, String id, @Nullable String stripPrefix,
                              List<String> headers, List<byte[]> rowTypes, List<String> expressions ) {
        String name = field.getId();
        String fieldType = checkStringAndGet( field, "type" );
        Object format = field.getFormat();

        boolean collection = false;
        String idType = fieldType;
        if( idType.endsWith( COLLECTION_SUFFIX ) ) {
            collection = true;
            idType = idType.substring( 0, idType.length() - COLLECTION_SUFFIX.length() );
        }

        TypeConfiguration rowType = types.get( idType );
        Preconditions.checkNotNull( rowType, "unknown type " + idType );

        Object defaultValue = field.getDefault()
            .orElseThrow( () -> new IllegalStateException( "default not found for " + id + "/" + name ) );

        String templateFunction = format != null ? "; format(\"" + format + "\")" : "";
        String comment = "model " + id + " id " + name + " path " + path + " type " + fieldType + " defaultValue '" + defaultValue + "'";
        Object pDefaultValue =
            defaultValue instanceof String ? "\"" + ( ( String ) defaultValue ).replace( "\"", "\\\"" ) + '"'
                : defaultValue;

        String exprPath = stripPrefix != null ? path.substring( stripPrefix.length() + 1 ) : path;
        if( exprPath.startsWith( "{" ) && exprPath.endsWith( "}" ) )
            exprPath = exprPath.substring( 1, exprPath.length() - 1 ).trim();
        expressions.add( "{{ /* " + comment + " */" + toJavaType( rowType.javaType, collection ) + exprPath + " ?? " + pDefaultValue + templateFunction + " }}" );
        headers.add( name );
        if( collection ) {
            rowTypes.add( new byte[] { Types.LIST.id, rowType.templateType.id } );
        } else {
            rowTypes.add( new byte[] { rowType.templateType.id } );
        }
    }

    private void processNestedFields( List<Dictionary> fields, @Nullable String currentPrefix,
                                      String id, List<String> headers, List<byte[]> rowTypes, List<String> expressions ) {
        List<Dictionary> rootAtLevel = new ArrayList<>();
        Map<String, List<Dictionary>> subgroups = new LinkedHashMap<>();

        for( Dictionary field : fields ) {
            String absPath = field.getPath();
            String relPath = currentPrefix != null ? absPath.substring( currentPrefix.length() + 1 ) : absPath;
            int dotIdx = relPath.indexOf( '.' );
            if( dotIdx < 0 ) {
                rootAtLevel.add( field );
            } else {
                subgroups.computeIfAbsent( relPath.substring( 0, dotIdx ), k -> new ArrayList<>() ).add( field );
            }
        }

        for( Dictionary field : rootAtLevel ) {
            appendField( field.getPath(), field, id, currentPrefix, headers, rowTypes, expressions );
        }

        for( List<Dictionary> group : subgroups.values() ) {
            if( group.size() >= 2 ) {
                String commonAbsPrefix = longestCommonPathPrefix( group );
                String relWith = currentPrefix != null
                    ? commonAbsPrefix.substring( currentPrefix.length() + 1 )
                    : commonAbsPrefix;
                expressions.add( "{{% with " + relWith + " }}" );
                processNestedFields( group, commonAbsPrefix, id, headers, rowTypes, expressions );
                expressions.add( "{{% end }}" );
            } else {
                appendField( group.get( 0 ).getPath(), group.get( 0 ), id, currentPrefix, headers, rowTypes, expressions );
            }
        }
    }

    private static String longestCommonPathPrefix( List<Dictionary> fields ) {
        String[] first = fields.get( 0 ).getPath().split( "\\." );
        int common = first.length - 1;
        for( int i = 1; i < fields.size() && common > 0; i++ ) {
            String[] other = fields.get( i ).getPath().split( "\\." );
            int max = Math.min( common, other.length - 1 );
            int c = 0;
            while( c < max && first[c].equals( other[c] ) ) c++;
            common = c;
        }
        return String.join( ".", Arrays.copyOf( first, common ) );
    }

    public boolean isLoggingAvailable() {
        return backend.isLoggingAvailable();
    }

    public AvailabilityReport availabilityReport() {
        return backend.availabilityReport();
    }

    private String toJavaType( String javaType, boolean collection ) {
        StringBuilder sb = new StringBuilder( "<" );
        if( collection ) sb.append( "java.util.Collection<" );
        sb.append( javaType );
        if( collection ) sb.append( ">" );
        sb.append( ">" );
        return sb.toString();
    }

    public interface RowBinaryObjectListener {
        default void javaCode( String javaCode ) {
        }
    }

    public static class TypeConfiguration {
        public final String javaType;
        public final Types templateType;

        public TypeConfiguration( String javaType, Types templateType ) {
            this.javaType = javaType;
            this.templateType = templateType;
        }
    }

    public static class DictionaryExtensions {
        public static String getPath( Dictionary dictionary ) {
            return dictionary.<String>getProperty( "path" ).get();
        }

        @Nullable
        public static String getFormat( Dictionary dictionary ) {
            return dictionary.<String>getProperty( "format" ).orElse( null );
        }

        public static Optional<Object> getDefault( Dictionary dictionary ) {
            return dictionary.getProperty( "default" );
        }
    }

    public class TypedRowBinaryLogger<D> {
        public final String[] headers;
        public final byte[][] types;
        private final Template<D, byte[], FastByteArrayOutputStream, TemplateAccumulatorRowBinary, ?> renderer;

        public TypedRowBinaryLogger( Template<D, byte[], FastByteArrayOutputStream, TemplateAccumulatorRowBinary, ?> renderer, String[] headers, byte[][] types ) {
            this.renderer = renderer;

            this.headers = headers;
            this.types = types;
        }

        public void log( D data, String filePreffix, Map<String, String> properties, String logType ) {
            byte[] bytes = renderer.render( data, true ).getBytes();
            backend.log( LogStreamProtocol.ProtocolVersion.ROW_BINARY_V3, Inet.HOSTNAME, filePreffix, properties, logType, headers, types, bytes );
        }
    }
}
