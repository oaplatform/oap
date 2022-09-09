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

package oap.fs;

import lombok.extern.slf4j.Slf4j;
import oap.io.Files;
import oap.io.content.ContentReader;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import static oap.io.content.ContentWriter.ofBytes;

@Slf4j
public class LocalFileManager extends AbstractFileManager implements FileManager<Data> {

    public LocalFileManager( Map<String, Path> buckets ) {
        super( buckets );
    }

    @Override
    public String write( String bucket, Data data ) {
        var name = data.nameOrConstruct( cuid.next() );
        var path = getBucket( bucket ).resolve( name );
        Files.write( path, data.decoded(), ofBytes() );
        return name;
    }

    @Override
    public Optional<byte[]> read( String bucket, String relativePath ) {
        var path = getBucket( bucket ).resolve( relativePath );
        return Files.exists( path ) ? Optional.of( Files.read( path, ContentReader.ofBytes() ) ) : Optional.empty();
    }

    @Override
    public void copyFromTo( String src, String dist ) {
        log.debug( "Copy files from {} to {}", src, dist );
        Files.copyContent( Path.of( src ), Path.of( dist ) );
    }
}
