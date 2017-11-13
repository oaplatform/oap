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

package oap.statsdb;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import oap.io.IoStreams;
import oap.io.IoStreams.Encoding;
import oap.json.Binder;
import oap.net.Inet;
import oap.statsdb.RemoteStatsDB.Sync;
import oap.storage.Storage;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by igor.petrenko on 05.09.2017.
 */
@Slf4j
public class StatsDBNode extends StatsDB<StatsDB.Database> implements Runnable, Closeable {
    private final Path directory;
    private final RemoteStatsDB master;
    protected boolean lastSyncSuccess = false;
    volatile Sync sync = null;

    public StatsDBNode( KeySchema schema, RemoteStatsDB master, Path directory, Storage<Node> storage ) {
        super( schema, storage );
        this.directory = directory;
        this.master = master;

        if( directory != null ) {
            val syncPath = directory.resolve( "sync.db.gz" );
            if( Files.exists( syncPath ) ) {
                log.info( "sync file = {}", sync );
                sync = Binder.json.unmarshal( Sync.class, syncPath );
            }
        }
    }

    @Override
    protected Database toDatabase( ConcurrentHashMap<String, Node> db ) {
        return new Database( db );
    }

    public synchronized void sync() {
        if( sync == null ) {
            sync = new Sync( storage.copyAndClean().toMap() );
            fsync( true );
        }

        try {
            if( master.update( sync, Inet.hostname() ) ) {
                sync = null;
                fsync( true );
            }
            lastSyncSuccess = true;
        } catch( Exception e ) {
            lastSyncSuccess = false;
            log.error( e.getMessage(), e );
        }
    }

    private synchronized void fsync( boolean syncOnly ) {
        if( !syncOnly ) {
            storage.fsync();
        }

        if( directory != null ) {
            val syncFile = directory.resolve( "sync.db.gz" );
            if( sync == null ) try {
                Files.deleteIfExists( syncFile );
            } catch( IOException e ) {
                log.error( e.getMessage(), e );
            }
            else {
                log.debug( "fsync {}", syncFile );
                oap.io.Files.ensureFile( syncFile );
                try( val sfos = IoStreams.out( syncFile, Encoding.from( syncFile ), IoStreams.DEFAULT_BUFFER, false, true ) ) {
                    Binder.json.marshal( sfos, sync );
                } catch( IOException e ) {
                    log.error( e.getMessage(), e );
                }
            }
        }
    }

    @Override
    public void run() {
        sync();
    }

    @Override
    public synchronized void removeAll() {
        super.removeAll();

        sync = null;
        fsync( false );
    }

    @Override
    public void close() {
        fsync( false );
    }
}
