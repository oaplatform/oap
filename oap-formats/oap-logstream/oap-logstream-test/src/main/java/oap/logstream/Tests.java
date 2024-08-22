package oap.logstream;

import oap.io.CompressionCodec;
import oap.logstream.disk.DiskLoggerBackend.FilePatternConfiguration;
import oap.logstream.disk.ParquetConfiguration;
import oap.logstream.disk.TsvConfiguration;
import oap.logstream.disk.WriterConfiguration;

public class Tests {
    public static final FilePatternConfiguration FILE_PATTERN_CONFIGURATION = new FilePatternConfiguration( "/<YEAR>-<MONTH>/<DAY>/<LOG_TYPE>_v<LOG_VERSION>_<CLIENT_HOST>-<YEAR>-<MONTH>-<DAY>-<HOUR>-<INTERVAL><EXT>", "tsv" );
    public static final WriterConfiguration CONFIGURATION = new WriterConfiguration(
        "tsv", new TsvConfiguration( CompressionCodec.GZIP ),
        "parquet", new ParquetConfiguration( CompressionCodec.ZSTD )
    );
}
