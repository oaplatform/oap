package oap.logstream.disk;

import lombok.ToString;
import oap.io.CompressionCodec;
import oap.util.Dates;

@ToString
public class TsvConfiguration extends AbstractWriterConfiguration {
    public final String dateTime32Format;

    public TsvConfiguration( CompressionCodec compressionCodec ) {
        this( Dates.PATTERN_FORMAT_SIMPLE_CLEAN, compressionCodec );
    }

    public TsvConfiguration( String dateTime32Format, CompressionCodec compressionCodec ) {
        super( compressionCodec );
        this.dateTime32Format = dateTime32Format;
    }
}
