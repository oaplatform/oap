package oap.io;

import java.util.List;
import java.util.Optional;

public enum CompressionCodec {
    GZIP( "gz" ),
    ZSTD( "zstd" ),
    LZ4( "lz4" ),
    NONE();

    public final List<String> extensions;

    CompressionCodec() {
        this( List.of() );
    }

    CompressionCodec( String extension ) {
        this( List.of( extension ) );
    }

    CompressionCodec( List<String> extensions ) {
        this.extensions = extensions;
    }

    public Optional<String> getDefaultExtension() {
        return extensions.isEmpty() ? Optional.empty() : Optional.of( extensions.getFirst() );
    }

    public String getDefaultFileExt() {
        return extensions.isEmpty() ? "" : "." + getDefaultExtension().orElseThrow();
    }
}
