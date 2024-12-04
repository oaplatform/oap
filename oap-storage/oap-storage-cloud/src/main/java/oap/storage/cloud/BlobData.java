package oap.storage.cloud;

import lombok.ToString;

import java.io.File;
import java.io.InputStream;
import java.io.Serial;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

@ToString
public class BlobData implements Serializable {
    @Serial
    private static final long serialVersionUID = -3448280826165820410L;

    public Object content;

    public Map<String, String> tags;
    public String contentType;
    public Long contentLength;

    public static BlobDataBuilder builder() {
        return new BlobDataBuilder();
    }

    public static class BlobDataBuilder {
        private final BlobData blobData = new BlobData();

        private BlobDataBuilder() {
        }

        public BlobDataBuilder contentLength( long contentLength ) {
            blobData.contentLength = contentLength;

            return this;
        }

        public BlobDataBuilder contentType( String contentType ) {
            blobData.contentType = contentType;

            return this;
        }

        public BlobDataBuilder content( Path path ) {
            blobData.content = path;

            return this;
        }

        public BlobDataBuilder content( File file ) {
            blobData.content = file;

            return this;
        }

        public BlobDataBuilder content( String str ) {
            blobData.content = str;

            return this;
        }

        public BlobDataBuilder content( byte[] bytes ) {
            blobData.content = bytes;

            return this;
        }

        public BlobDataBuilder content( ByteBuffer byteBuffer ) {
            blobData.content = byteBuffer;

            return this;
        }

        public BlobDataBuilder content( InputStream inputStream ) {
            blobData.content = inputStream;

            return this;
        }

        public BlobDataBuilder tags( Map<String, String> tags ) {
            blobData.tags = new LinkedHashMap<>( tags );

            return this;
        }

        public BlobDataBuilder tag( String tag, String value ) {
            if( blobData.tags == null ) {
                blobData.tags = new LinkedHashMap<>();
            }
            blobData.tags.put( tag, value );

            return this;
        }

        public BlobData build() {
            return blobData;
        }
    }
}
