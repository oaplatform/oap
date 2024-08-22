package oap.logstream.disk;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.ToString;
import oap.io.CompressionCodec;

import java.util.LinkedHashMap;

@ToString
public class ParquetConfiguration extends AbstractWriterConfiguration {
    public final LinkedHashMap<String, String> excludeFieldsIfPropertiesExists = new LinkedHashMap<>();

    @JsonCreator
    public ParquetConfiguration( CompressionCodec compressionCodec ) {
        super( compressionCodec );
    }
}
