package oap.logstream.disk;

import lombok.ToString;
import oap.json.AbstractProperties;

@ToString
public class WriterConfiguration extends AbstractProperties {
    public WriterConfiguration() {
    }

    public WriterConfiguration( String format, AbstractWriterConfiguration configuration ) {
        putProperty( format, configuration );
    }

    public WriterConfiguration( String format, AbstractWriterConfiguration configuration, String format2, AbstractWriterConfiguration configuration2 ) {
        putProperty( format, configuration );
        putProperty( format2, configuration2 );
    }
}
