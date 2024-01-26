package oap.concurrent.atomic;

import oap.concurrent.Threads;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableLong;

import java.io.Closeable;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.UncheckedIOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Path;
import java.util.function.LongFunction;
import java.util.function.LongUnaryOperator;

/**
 * FileAtomicLong
 */
public class FileAtomicLong implements Closeable {
    protected final byte[] buffer = new byte[8];
    private final File sharedLockFile;
    private final long retryTimeMs;
    private final long initValue;
    private FileChannel channel;
    private FileLock lock;

    public FileAtomicLong( String sharedLockFile, long retryTimeMs, long initValue ) {
        this( new File( sharedLockFile ), retryTimeMs, initValue );
    }

    public FileAtomicLong( File sharedLockFile, long retryTimeMs, long initValue ) {
        this.sharedLockFile = sharedLockFile;
        this.retryTimeMs = retryTimeMs;
        this.initValue = initValue;
    }

    public FileAtomicLong( Path sharedLockFile, long retryTimeMs, long initValue ) {
        this( sharedLockFile.toFile(), retryTimeMs, initValue );
    }

    public final long get() throws UncheckedIOException {
        MutableLong ret = new MutableLong();
        editFile( fileValue -> {
            ret.setValue( fileValue );
            return null;
        } );
        return ret.getValue();
    }

    public final void set( long newValue ) throws UncheckedIOException {
        editFile( fileValue -> newValue );
    }

    public final long getAndSet( long newValue ) throws UncheckedIOException {
        MutableLong ret = new MutableLong();
        editFile( fileValue -> {
            ret.setValue( fileValue );

            return newValue;
        } );

        return ret.getValue();
    }

    public final boolean compareAndSet( long expectedValue, long newValue ) throws UncheckedIOException {
        MutableBoolean ret = new MutableBoolean();

        editFile( fileValue -> {
            if( fileValue != expectedValue ) {
                ret.setFalse();
                return null;
            } else {
                ret.setTrue();
                return newValue;
            }
        } );

        return ret.booleanValue();
    }

    public final long getAndIncrement() throws UncheckedIOException {
        return getAndAdd( 1 );
    }

    public final long getAndDecrement() throws UncheckedIOException {
        return getAndAdd( -1 );
    }

    public final long getAndAdd( long delta ) throws UncheckedIOException {
        MutableLong ret = new MutableLong();
        editFile( fileValue -> {
            ret.setValue( fileValue );

            return fileValue + delta;
        } );

        return ret.getValue();
    }

    public final long incrementAndGet() throws UncheckedIOException {
        return addAndGet( 1 );
    }

    public final long decrementAndGet() throws UncheckedIOException {
        return addAndGet( -1 );
    }

    public final long addAndGet( long delta ) throws UncheckedIOException {
        MutableLong ret = new MutableLong();
        editFile( fileValue -> {
            long value = fileValue + delta;
            ret.setValue( value );

            return value;
        } );

        return ret.getValue();
    }

    public final long getAndUpdate( LongUnaryOperator updateFunction ) throws UncheckedIOException {
        MutableLong ret = new MutableLong();
        editFile( fileValue -> {
            ret.setValue( fileValue );

            return updateFunction.applyAsLong( fileValue );
        } );

        return ret.getValue();
    }

    public final long updateAndGet( LongUnaryOperator updateFunction ) throws UncheckedIOException {
        MutableLong ret = new MutableLong();
        editFile( fileValue -> {
            long value = updateFunction.applyAsLong( fileValue );
            ret.setValue( value );

            return value;
        } );

        return ret.getValue();
    }

    private boolean lockFile() throws UncheckedIOException {
        try {
            // Try to get the lock
            channel = new RandomAccessFile( sharedLockFile, "rw" ).getChannel();
            lock = channel.tryLock();
            if( lock == null ) {
                // File is locked by other application
                channel.close();
                return false;
            }
            return true;
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }

    public void unlockFile() throws UncheckedIOException {
        try {
            if( lock != null ) {
                lock.release();
                channel.close();

                lock = null;
                channel = null;
            }
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }

    private void editFile( LongFunction<Long> func ) throws UncheckedIOException {
        try {
            while( !lockFile() ) {
                Threads.sleepSafely( retryTimeMs );
            }

            channel.position( 0 );
            long value = readLong( Channels.newInputStream( channel ), initValue );
            Long result = func.apply( value );
            if( result != null ) {
                channel.position( 0 );
                writeLong( Channels.newOutputStream( channel ), result );
            }
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        } finally {
            unlockFile();
        }
    }

    @Override
    public void close() throws UncheckedIOException {
        unlockFile();
    }

    protected void writeLong( OutputStream os, long v ) throws UncheckedIOException {
        try {
            buffer[0] = ( byte ) ( v >>> 56 );
            buffer[1] = ( byte ) ( v >>> 48 );
            buffer[2] = ( byte ) ( v >>> 40 );
            buffer[3] = ( byte ) ( v >>> 32 );
            buffer[4] = ( byte ) ( v >>> 24 );
            buffer[5] = ( byte ) ( v >>> 16 );
            buffer[6] = ( byte ) ( v >>> 8 );
            buffer[7] = ( byte ) ( v >>> 0 );

            os.write( buffer );
            os.flush();
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }

    protected long readLong( InputStream is, long initValue ) throws UncheckedIOException {
        try {
            IOUtils.readFully( is, buffer, 0, 8 );
        } catch( EOFException ignored ) {
            return initValue;
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }

        return ( ( long ) buffer[0] << 56 )
            + ( ( long ) ( buffer[1] & 255 ) << 48 )
            + ( ( long ) ( buffer[2] & 255 ) << 40 )
            + ( ( long ) ( buffer[3] & 255 ) << 32 )
            + ( ( long ) ( buffer[4] & 255 ) << 24 )
            + ( ( buffer[5] & 255 ) << 16 )
            + ( ( buffer[6] & 255 ) << 8 )
            + ( ( buffer[7] & 255 ) << 0 );
    }
}
