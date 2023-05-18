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
package oap.archive;

import lombok.SneakyThrows;
import oap.io.Files;
import oap.io.IoStreams;
import oap.reflect.Reflect;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.jar.JarArchiveInputStream;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import static oap.io.IoStreams.Encoding.GZIP;
import static oap.io.IoStreams.Encoding.PLAIN;

public class Archiver {
    private static final int BUFFER_SIZE = 4096;

    @SneakyThrows
    public void unpack( Path archive, Path dest, ArchiveType type ) {
        switch ( type ) {
            case TAR_GZ -> writeUnarchived( archive, dest, ArchiveType.TAR_GZ );
            case JAR -> writeUnarchived( archive, dest, ArchiveType.JAR );
            case ZIP -> writeUnarchived( archive, dest, ArchiveType.ZIP );
            case SEVEN_Z -> {
                try ( SevenZFile archiveInputStream = new SevenZFile( archive.toFile() ) ) {
                    byte[] buffer = new byte[ BUFFER_SIZE ];
                    ArchiveEntry archiveEntry;
                    while ( null != ( archiveEntry = archiveInputStream.getNextEntry() ) ) {
                        File file = new File( dest.toFile(), archiveEntry.getName() );
                        try ( FileOutputStream fileOutputStream = new FileOutputStream( file ) ) {
                            int length = -1;
                            while ( ( length = archiveInputStream.read( buffer ) ) != -1 ) {
                                fileOutputStream.write( buffer, 0, length );
                            }
                        }
                    }
                } catch ( IOException exception ) {
                    throw new IllegalStateException( "Cannot uncompress 7-zip from " + archive.toUri() + " to {}" + dest.toUri(), exception );
                }
            }
            default -> throw new IllegalArgumentException( String.valueOf( type ) );
        }
    }

    private void writeUnarchived( Path archive, Path dest, ArchiveType type ) throws IOException {
        InputStream in = IoStreams.in( archive, GZIP );
        try( var tar = type.create( in ) ) {
            ArchiveEntry entry;
            while( ( entry = tar.getNextEntry() ) != null ) {
                Path path = dest.resolve( entry.getName() );
                if( entry.isDirectory() ) Files.ensureDirectory( path );
                else IoStreams.write( path, PLAIN, tar );
            }
        }
    }

    public enum ArchiveType {
        TAR_GZ( TarArchiveInputStream.class ),
        JAR( JarArchiveInputStream.class ),
        ZIP( ZipArchiveInputStream.class ),
        SEVEN_Z( null );
        private final Class<? extends ArchiveInputStream> clazz;


        ArchiveType( Class<? extends ArchiveInputStream> archiveInputStreamClass ) {
            this.clazz = archiveInputStreamClass;
        }
        public ArchiveInputStream create( InputStream in ) {
            return Reflect.newInstance( clazz, new Object[] { in } );
        }
    }
}
