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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.Cleaner;

import java.io.Closeable;
import java.io.File;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class Closeables {
    private static final Logger logger = LoggerFactory.getLogger( Closeables.class );


    private static Field cleanerField;

    static {
        init();
    }

    @SneakyThrows
    private static void init() {
        File tempFile = File.createTempFile( "test", "test" );
        try( RandomAccessFile file = new RandomAccessFile( tempFile, "rw" );
             FileChannel channel = file.getChannel() ) {
            MappedByteBuffer buffer =
                channel.map( FileChannel.MapMode.READ_WRITE, 0, 1 );
            cleanerField = buffer.getClass().getDeclaredField( "cleaner" );
            cleanerField.setAccessible( true );

            close( buffer );
        } finally {
            tempFile.delete();
        }
    }

    public static void close( Closeable closeable ) {
        try {
            if( closeable != null ) closeable.close();
        } catch( Exception e ) {
            logger.error( e.getMessage(), e );
        }
    }

    public static void close( ExecutorService service ) {
        try {
            if( service != null ) {
                service.shutdownNow();
                service.awaitTermination( 60, TimeUnit.SECONDS );
            }
        } catch( Exception e ) {
            logger.error( e.getMessage() );
        }
    }

    public static void close( MappedByteBuffer buffer ) {
        try {
            Cleaner cleaner = ( Cleaner ) cleanerField.get( buffer );
            cleaner.clean();
        } catch( IllegalAccessException e ) {
            logger.warn( e.getMessage(), e );
        }
    }
}
