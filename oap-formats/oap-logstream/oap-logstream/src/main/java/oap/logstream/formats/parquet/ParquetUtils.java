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

package oap.logstream.formats.parquet;

import com.google.common.base.Preconditions;
import lombok.ToString;
import oap.dictionary.Dictionary;
import oap.dictionary.DictionaryParser;
import oap.dictionary.DictionaryRoot;
import oap.io.IoStreams;
import oap.template.Types;
import oap.tsv.Tsv;
import oap.tsv.TsvArray;
import oap.tsv.TsvStream;
import oap.util.Dates;
import oap.util.Lists;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.example.data.Group;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.hadoop.example.GroupWriteSupport;
import org.apache.parquet.hadoop.util.HadoopOutputFile;
import org.apache.parquet.schema.GroupType;
import org.apache.parquet.schema.LogicalTypeAnnotation;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.Type;
import org.apache.parquet.schema.Types.Builder;
import org.joda.time.DateTime;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Function;

import static org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName.BINARY;
import static org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName.BOOLEAN;
import static org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName.DOUBLE;
import static org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName.FLOAT;
import static org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName.INT32;
import static org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName.INT64;

/**
 * TSV to Parquet conversion utility
 */
public class ParquetUtils {
    private static final HashMap<Types, Function<List<Builder<?, ?>>, Builder<?, ?>>> types = new HashMap<>();

    static {
        types.put( Types.BOOLEAN, children -> org.apache.parquet.schema.Types.required( BOOLEAN ) );
        types.put( Types.BYTE, children -> org.apache.parquet.schema.Types.required( INT32 ).as( LogicalTypeAnnotation.intType( 8, true ) ) );
        types.put( Types.SHORT, children -> org.apache.parquet.schema.Types.required( INT32 ).as( LogicalTypeAnnotation.intType( 16, true ) ) );
        types.put( Types.INTEGER, children -> org.apache.parquet.schema.Types.required( INT32 ).as( LogicalTypeAnnotation.intType( 32, true ) ) );
        types.put( Types.LONG, children -> org.apache.parquet.schema.Types.required( INT64 ).as( LogicalTypeAnnotation.intType( 64, true ) ) );
        types.put( Types.FLOAT, children -> org.apache.parquet.schema.Types.required( FLOAT ) );
        types.put( Types.DOUBLE, children -> org.apache.parquet.schema.Types.required( DOUBLE ) );
        types.put( Types.STRING, children -> org.apache.parquet.schema.Types.required( BINARY ).as( LogicalTypeAnnotation.stringType() ) );
        types.put( Types.DATE, children -> org.apache.parquet.schema.Types.required( INT32 ).as( LogicalTypeAnnotation.dateType() ) );
        types.put( Types.DATETIME, children -> org.apache.parquet.schema.Types.required( INT64 ) );
//        types.put( Types.DADATETIME64, children -> org.apache.parquet.schema.Types.required( INT64 ).as( LogicalTypeAnnotation.timestampType( true, MILLIS ) ) );
        types.put( Types.LIST, children -> org.apache.parquet.schema.Types.requiredList().element( ( Type ) children.get( 0 ).named( "element" ) ) );
//        types.put( Types.ENUM, children -> org.apache.parquet.schema.Types.required( BINARY ).as( LogicalTypeAnnotation.stringType() ) );
    }

    public final Builder<?, ?> schema;
    protected final HashMap<String, FieldInfo> defaultValuesMap = new HashMap<>();
    protected final ArrayList<FieldInfo> defaultValuesList;

    public ParquetUtils( Dictionary dictionary ) {
        schema = org.apache.parquet.schema.Types.buildMessage();

        defaultValuesList = new ArrayList<>( dictionary.getValues().size() );
        var i = 0;

        var fields = new LinkedHashMap<String, Builder<?, ?>>();

        for( var col : dictionary.getValues() ) {
            Object typeObj = col.getProperty( "type" ).orElse( null );
            Preconditions.checkArgument( typeObj instanceof String || typeObj instanceof List,
                "[" + col.getId() + "] type must be string or list<string>" );
            List<String> type = typeObj instanceof List<?> ? ( List<String> ) typeObj : List.of( typeObj.toString() );
            Preconditions.checkArgument( type.size() > 0 );

            Builder<?, ?> fieldType = null;
            for( var typeIdx = type.size() - 1; typeIdx >= 0; typeIdx-- ) {
                var typeEnum = Types.valueOf( type.get( typeIdx ) );
                var func = types.get( typeEnum );
                fieldType = func.apply( fieldType != null ? List.of( fieldType ) : List.of() );
            }

            fields.put( col.getId(), fieldType );

            Object defaultValue = col.getProperty( "default" ).orElseThrow( () -> new IllegalArgumentException( col.getId() + ": default is required" ) );

            FieldInfo fieldInfo = new FieldInfo( defaultValue, fieldType, Lists.map( type, Types::valueOf ) );
            ParquetUtils.this.defaultValuesMap.put( col.getId(), fieldInfo );
            ParquetUtils.this.defaultValuesList.add( fieldInfo );
        }

        setFields( fields );
    }

    protected static Timestamp toTimestamp( Object value ) {
        if( value instanceof Timestamp valueTimestamp ) return valueTimestamp;
        else if( value instanceof DateTime valueDateTime )
            return new Timestamp( valueDateTime.getMillis() );
        else if( value instanceof Long longValue )
            return new Timestamp( longValue );
        else
            return new Timestamp( Dates.FORMAT_SIMPLE.parseMillis( value.toString() ) );
    }

    public static void main( String[] args ) throws IOException {
        String source = args[0];
        String datamodel = args[1];
        String type = args[2];
        String out = FilenameUtils.removeExtension( source ) + ".parquet";

        DictionaryRoot dictionaryRoot = DictionaryParser.parse( Paths.get( datamodel ), DictionaryParser.INCREMENTAL_ID_STRATEGY );
        var schema = new ParquetUtils( dictionaryRoot.getValue( type ) );

        Configuration conf = new Configuration();

        if( Files.exists( Paths.get( out ) ) )
            Files.delete( Paths.get( out ) );

        TsvStream tsvStream = Tsv.tsv.fromStream( IoStreams.lines( Paths.get( source ) ) ).withHeaders();
        var headers = tsvStream.headers();

        MessageType modelMessageType = ( MessageType ) schema.schema.named( "group" );
        org.apache.parquet.schema.Types.MessageTypeBuilder tsvMessageTypeBuilder = org.apache.parquet.schema.Types.buildMessage();

        for( var modelType : modelMessageType.getFields() ) {
            if( headers.contains( modelType.getName() ) )
                tsvMessageTypeBuilder.addField( modelType );
        }

        MessageType tsvMessageType = tsvMessageTypeBuilder.named( "tsv" );

        GroupWriteSupport.setSchema( tsvMessageType, conf );

        var select = Lists.map( modelMessageType.getFields(), Type::getName );

        try( ParquetWriter<Group> writer = new ParquetWriteBuilder( HadoopOutputFile.fromPath( new Path( out ), conf ) )
            .withConf( conf )
            .build() ) {

            try( var stream = tsvStream.select( select ).stripHeaders().toStream() ) {
                stream.forEach( cols -> {
                    try {
                        ParquetSimpleGroup simpleGroup = new ParquetSimpleGroup( tsvMessageType );

                        for( int i = 0; i < tsvMessageType.getFields().size(); i++ ) {
                            var header = tsvMessageType.getType( i ).getName();
                            schema.setString( simpleGroup, header, cols.get( i ) );
                        }
                        writer.write( simpleGroup );
                    } catch( Exception e ) {
                        e.printStackTrace();
                        throw new RuntimeException( e );
                    }
                } );
            }
        } finally {
            var name = FilenameUtils.getName( out );
            var parent = FilenameUtils.getFullPathNoEndSeparator( out );
            java.nio.file.Path crcPath = Paths.get( parent + "/." + name + ".crc" );
            if( Files.exists( crcPath ) )
                Files.delete( crcPath );
        }
    }

    protected void setFields( LinkedHashMap<String, Builder<?, ?>> fields ) {
        fields.forEach( ( n, b ) -> ( ( org.apache.parquet.schema.Types.MessageTypeBuilder ) schema ).addField( ( Type ) b.named( n ) ) );
    }

    public void setString( ParquetSimpleGroup group, String index, String value ) {
        int fieldIndex = group.getType().getFieldIndex( index );
        List<Types> types = defaultValuesList.get( fieldIndex ).type;

        setString( group, fieldIndex, value, types );
    }

    private void setString( Group group, int index, String value, List<Types> types ) {
        if( value == null ) return;

        switch( types.get( 0 ) ) {
            case BOOLEAN -> group.add( index, Byte.parseByte( value ) == 1 );
            case BYTE -> group.add( index, Byte.parseByte( value ) );
            case SHORT -> group.add( index, Short.parseShort( value ) );
            case INTEGER -> group.add( index, Integer.parseInt( value ) );
            case LONG -> group.add( index, Long.parseLong( value ) );
            case FLOAT -> group.add( index, Float.parseFloat( value ) );
            case DOUBLE -> group.add( index, Double.parseDouble( value ) );
            case STRING/*, ENUM*/ -> group.add( index, value );
            case DATE -> {
                long ms = Dates.FORMAT_DATE.parseMillis( value );
                group.add( index, ( int ) ( ms / 24L / 60 / 60 / 1000 ) );
            }
            case DATETIME -> {
                long ms = Dates.PARSER_MULTIPLE_DATETIME.parseMillis( value );
                group.add( index, ms / 1000 );
            }
//            case DATETIME64 -> group.add( index, Dates.PARSER_MULTIPLE_DATETIME.parseMillis( value ) );
            case LIST -> {
                var listType = types.subList( 1, types.size() );
                Group listGroup = group.addGroup( index );
                for( var item : TsvArray.parse( value ) ) {
                    setString( listGroup.addGroup( "list" ), 0, item, listType );
                }

            }
        }
    }

    public static String toString( Type type, ParquetSimpleGroup group, int x, int y ) {
        LogicalTypeAnnotation logicalTypeAnnotation = type.getLogicalTypeAnnotation();

        if( logicalTypeAnnotation instanceof LogicalTypeAnnotation.DateLogicalTypeAnnotation ) {
            return Dates.FORMAT_DATE.print( group.getInteger( x, y ) * 24L * 60 * 60 * 1000 );
        } else if( logicalTypeAnnotation instanceof LogicalTypeAnnotation.ListLogicalTypeAnnotation ) {
            var list = new ArrayList<>( group.getFieldRepetitionCount( x ) );
            for( var listIndex = 0; listIndex < group.getFieldRepetitionCount( x ); listIndex++ ) {
                var listItemType = ( ( GroupType ) type ).getType( 0 );
                LogicalTypeAnnotation listItemLogicalTypeAnnotation = listItemType.getLogicalTypeAnnotation();

                if( listItemLogicalTypeAnnotation instanceof LogicalTypeAnnotation.StringLogicalTypeAnnotation
                    || listItemLogicalTypeAnnotation instanceof LogicalTypeAnnotation.DateLogicalTypeAnnotation
                    || listItemLogicalTypeAnnotation instanceof LogicalTypeAnnotation.TimestampLogicalTypeAnnotation ) {
                    list.add( "'" + toString( listItemType, group, x, listIndex ) + "'" );
                }
            }
            return TsvArray.print( list, Dates.FORMAT_DATE );
        }

        return group.getValueToString( x, y );
    }

    protected String enumToString( Object value ) {
        if( value instanceof Enum<?> valueEnum ) return valueEnum.name();

        return toString( value );
    }

    @SuppressWarnings( "checkstyle:OverloadMethodsDeclarationOrder" )
    protected String toString( Object value ) {
        return value.toString();
    }

    protected double toDouble( Object value ) {
        return value instanceof Number ? ( ( Number ) value ).doubleValue() : Double.parseDouble( value.toString() );
    }

    @SuppressWarnings( "unchecked" )
    protected List<String> toList( Object value, List<Types> types ) {
        if( value instanceof List<?> ) return ( List<String> ) value;

        var arrayStr = value.toString().trim();
        var array = arrayStr.substring( 1, arrayStr.length() - 1 );

        var data = StringUtils.splitPreserveAllTokens( array, ',' );

        return List.of( data );
    }

    protected long toDate( Object value ) {
        if( value instanceof DateTime )
            return ( ( DateTime ) value ).getMillis() / 24 / 60 / 60 / 1000;
        else if( value instanceof Long )
            return ( long ) value;
        else
            return Dates.FORMAT_DATE.parseMillis( value.toString() ) / 24 / 60 / 60 / 1000;
    }

    protected short toShort( Object value ) {
        return value instanceof Number ? ( ( Number ) value ).shortValue() : Short.parseShort( value.toString() );
    }

    protected long toBoolean( Object value ) {
        if( value instanceof Boolean booleanValue ) return booleanValue ? 1 : 0;
        else return Boolean.parseBoolean( value.toString() ) ? 1 : 0;
    }

    protected int toByte( Object value ) {
        return value instanceof Number ? ( ( Number ) value ).byteValue() : Byte.parseByte( value.toString() );
    }

    protected int toInt( Object value ) {
        return value instanceof Number ? ( ( Number ) value ).intValue() : Integer.parseInt( value.toString() );
    }

    protected long toLong( Object value ) {
        return value instanceof Number ? ( ( Number ) value ).longValue() : Long.parseLong( value.toString() );
    }

    protected float toFloat( Object value ) {
        return value instanceof Number ? ( ( Number ) value ).floatValue() : Float.parseFloat( value.toString() );
    }

    @ToString
    protected static class FieldInfo {
        public final Object defaultValue;
        public final List<Types> type;
        public final org.apache.parquet.schema.Types.Builder<?, ?> schema;

        public FieldInfo( Object defaultValue, org.apache.parquet.schema.Types.Builder<?, ?> schema, List<Types> type ) {
            this.defaultValue = defaultValue;
            this.schema = schema;
            this.type = type;
        }
    }
}
