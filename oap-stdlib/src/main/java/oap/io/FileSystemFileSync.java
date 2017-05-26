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

package oap.io;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * Created by igor.petrenko on 24.05.2017.
 */
@Slf4j
public class FileSystemFileSync extends FileSync {
    public FileSystemFileSync() {
        super( "file" );
    }

    @Override
    @SneakyThrows
    protected Optional<Path> download() {
        val remoteFile = Paths.get( uri );
        val localFile = this.localFile.toAbsolutePath();

        val remoteFileLastModifiedTime = java.nio.file.Files.getLastModifiedTime( remoteFile ).toMillis();
        val localFileLastModifiedTime = Files.exists( localFile )
            ? java.nio.file.Files.getLastModifiedTime( localFile ).toMillis()
            : Long.MIN_VALUE;

        if( remoteFileLastModifiedTime > localFileLastModifiedTime ) {
            try( InputStream in = IoStreams.in( uri.toURL().openStream(), IoStreams.Encoding.PLAIN ) ) {
                IoStreams.write( localFile, IoStreams.Encoding.PLAIN, in, false, true );
            }

            oap.io.Files.setLastModifiedTime( localFile, remoteFileLastModifiedTime );

            return Optional.of( localFile );
        } else return Optional.empty();
    }
}
