package oap.http;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;

import java.io.IOException;
import java.io.InputStream;

public class InputStreamRequestBody extends RequestBody {
    private final MediaType mediaType;
    private final InputStream inputStream;

    public InputStreamRequestBody( MediaType mediaType, InputStream inputStream ) {
        this.mediaType = mediaType;
        this.inputStream = inputStream;
    }

    @Override
    public MediaType contentType() {
        return mediaType;
    }

    // You can override contentLength() if you know the length in advance for better performance/features (e.g., S3 uploads).
    // If you don't override it, OkHttp will use chunked transfer encoding for large bodies.

    @Override
    public void writeTo( BufferedSink sink ) throws IOException {
        try( Source source = Okio.source( inputStream ) ) {
            sink.writeAll( source );
        }
    }
}
