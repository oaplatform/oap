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

package oap.ws.file;

import oap.io.Files;
import oap.io.content.ContentReader;
import oap.io.content.ContentWriter;
import oap.util.Cuid;
import oap.util.Strings;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class BucketManager {
    public static final String DEFAULT_BUCKET = Strings.DEFAULT;
    public final Map<String, Path> buckets = new LinkedHashMap<>();
    protected Cuid cuid = Cuid.UNIQUE;

    public BucketManager( Map<String, Path> buckets ) {
        this.buckets.putAll( buckets );
    }

    public String put( Data data ) {
        return put( DEFAULT_BUCKET, data );
    }

    public String put( String bucket, Data data ) {
        String name = data.nameOrConstruct( cuid.next() );
        Path path = getBucket( bucket ).resolve( name );
        Files.write( path, data.decoded(), ContentWriter.ofBytes() );
        return name;
    }

    public Path getBucket( String bucket ) {
        return buckets.getOrDefault( bucket, Paths.get( "/tmp" ) );
    }

    public Optional<byte[]> get( String bucket, String relativePath ) {
        Path path = getBucket( bucket ).resolve( relativePath );
        return Files.exists( path ) ? Optional.of( Files.read( path, ContentReader.ofBytes() ) ) : Optional.empty();
    }

    public Optional<byte[]> get( String relativePath ) {
        return get( DEFAULT_BUCKET, relativePath );
    }
}
