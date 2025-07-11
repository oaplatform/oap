package oap.logstream.formats.rowbinary;

import lombok.SneakyThrows;
import oap.dictionary.Dictionary;
import oap.template.TemplateAccumulator;
import oap.template.TemplateAccumulatorBinary;
import oap.template.TemplateAccumulatorString;
import oap.template.TemplateException;
import oap.util.FastByteArrayOutputStream;
import oap.util.Strings;
import org.apache.commons.lang3.EnumUtils;
import org.joda.time.DateTime;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

/**
 * https://clickhouse.com/docs/interfaces/formats/RowBinary
 */
public class TemplateAccumulatorRowBinary implements TemplateAccumulator<byte[], FastByteArrayOutputStream, TemplateAccumulatorRowBinary> {
    private static final HashSet<Class<?>> numberClass = new HashSet<>();

    static {
        numberClass.add( Byte.class );
        numberClass.add( byte.class );
        numberClass.add( Short.class );
        numberClass.add( short.class );
        numberClass.add( Integer.class );
        numberClass.add( int.class );
        numberClass.add( Long.class );
        numberClass.add( long.class );
        numberClass.add( Float.class );
        numberClass.add( float.class );
        numberClass.add( Double.class );
        numberClass.add( double.class );
    }

    protected final FastByteArrayOutputStream baos;
    private final RowBinaryOutputStream bos;

    public TemplateAccumulatorRowBinary( FastByteArrayOutputStream baos ) {
        this.baos = baos;
        this.bos = new RowBinaryOutputStream( baos );
    }

    public TemplateAccumulatorRowBinary() {
        this( new FastByteArrayOutputStream() );
    }

    @SneakyThrows
    @Override
    public void acceptText( String text ) {
        bos.writeString( text == null ? "" : text );
    }

    @SneakyThrows
    @Override
    public void accept( String text ) {
        bos.writeString( text == null ? "" : text );
    }

    @SneakyThrows
    @Override
    public void accept( boolean b ) {
        bos.writeBoolean( b );
    }

    @SneakyThrows
    @Override
    public void accept( byte b ) {
        bos.writeByte( b );
    }

    @SneakyThrows
    @Override
    public void accept( short s ) {
        bos.writeShort( s );
    }

    @SneakyThrows
    @Override
    public void accept( int i ) {
        bos.writeInt( i );
    }

    @SneakyThrows
    @Override
    public void accept( long l ) {
        bos.writeLong( l );
    }

    @SneakyThrows
    @Override
    public void accept( float f ) {
        bos.writeFloat( f );
    }

    @SneakyThrows
    @Override
    public void accept( double d ) {
        bos.writeDouble( d );
    }

    @SneakyThrows
    @Override
    public void accept( DateTime jodaDateTime ) {
        bos.writeDateTime( jodaDateTime );
    }

    @SneakyThrows
    @Override
    public void accept( Date javaDate ) {
        bos.writeDate( javaDate );
    }

    @SneakyThrows
    @Override
    public void accept( Enum<?> e ) {
        bos.writeString( e.name() );
    }

    @SneakyThrows
    @Override
    public void accept( Collection<?> list ) {
        bos.writeList( list );
    }

    @SneakyThrows
    @Override
    public void accept( TemplateAccumulatorRowBinary acc ) {
        FastByteArrayOutputStream baos = acc.baos;

        bos.write( baos.array, 0, baos.length );
    }

    @SneakyThrows
    public void accept( TemplateAccumulatorString acc ) {
        bos.writeString( acc.get() );
    }

    @Override
    public void accept( Object obj ) {
        switch( obj ) {
            case String s -> accept( s );
            case Byte b -> accept( b );
            case Short s -> accept( s );
            case Integer i -> accept( i );
            case Long l -> accept( l );
            case Float f -> accept( f );
            case Double d -> accept( d );
            case Enum<?> e -> accept( e );
            case Dictionary d -> accept( d );
            case DateTime dt -> accept( dt );
            case Date d -> accept( d );
            case Collection<?> c -> accept( c );
            case TemplateAccumulatorBinary tab -> accept( tab );
            case TemplateAccumulatorString tab -> accept( tab );
            case null, default -> throw new IllegalArgumentException( "Unknown type " + obj.getClass() );
        }
    }

    @Override
    public void acceptNull( Class<?> type ) {
        throw new IllegalArgumentException( "type " + type );
    }

    @Override
    public String getDefault( Class<?> type ) {
        if( String.class.equals( type ) ) return "";
        else if( Boolean.class.equals( type ) || boolean.class.equals( type ) ) return "false";
        else if( numberClass.contains( type ) ) return "0";
        else if( Enum.class.isAssignableFrom( type ) ) {
            try {
                return Enum.valueOf( ( Class<Enum> ) type, Strings.UNKNOWN ).name();
            } catch( IllegalArgumentException ignored ) {
                List<Enum> enumList = EnumUtils.getEnumList( ( Class<Enum> ) type );
                return enumList.get( 0 ).name();
            }
        } else if( Collection.class.isAssignableFrom( type ) ) return "[]";
        else
            throw new TemplateException( new IllegalArgumentException( "class " + type + " unknown default value" ) );
    }

    @Override
    public boolean isEmpty() {
        return baos.length == 0;
    }

    @Override
    public TemplateAccumulatorRowBinary newInstance() {
        return new TemplateAccumulatorRowBinary();
    }

    @Override
    public TemplateAccumulatorRowBinary newInstance( FastByteArrayOutputStream mutable ) {
        return new TemplateAccumulatorRowBinary( mutable );
    }

    @Override
    public String getTypeName() {
        return "byte[]";
    }

    @Override
    public String delimiter() {
        return "";
    }

    @SneakyThrows
    @Override
    public TemplateAccumulatorRowBinary addEol( boolean eol ) {
        return this;
    }

    @Override
    public void reset() {
        baos.reset();
    }

    @SneakyThrows
    @Override
    public byte[] get() {
        return baos.toByteArray();
    }

    @Override
    public byte[] getBytes() {
        return get();
    }
}
