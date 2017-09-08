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

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import oap.io.SafeFileOutputStream;
import oap.json.Binder;
import oap.net.Inet;
import oap.statsdb.RemoteStatsDB.Sync;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;

import static oap.io.IoStreams.Encoding.GZIP;

/**
 * Created by igor.petrenko on 05.09.2017.
 */
@Slf4j
public class StatsDBNode extends StatsDB<StatsDB.Database> {
    private final RemoteStatsDB master;
    volatile Sync sync = null;

    public StatsDBNode( RemoteStatsDB master, Path directory ) {
        super( directory, new TypeReference<Database>() {} );
        this.master = master;
    }

    @Override
    protected Database toDatabase( ConcurrentHashMap db ) {
        return new Database( db );
    }

    public synchronized void sync() {
        if( sync == null ) {
            sync = new Sync( db );
            db = new ConcurrentHashMap<>();
            fsync( false );
        }

        try {
            if( master.update( sync, Inet.hostname() ) ) {
                sync = null;
                fsync( true );
            }
        } catch( Exception e ) {
            log.error( e.getMessage(), e );
        }
    }

    private synchronized void fsync( boolean syncOnly ) {
        if( !syncOnly ) {
            fsync();
        }

        if( sync == null ) try {
            Files.deleteIfExists( directory.resolve( "sync.db.gz" ) );
        } catch( IOException e ) {
            log.error( e.getMessage(), e );
        }
        else {
            try( val sfos = new SafeFileOutputStream( directory.resolve( "sync" ), false, GZIP ) ) {
                Binder.json.marshal( sfos, sync );
            } catch( IOException e ) {
                log.error( e.getMessage(), e );
            }
        }
    }
}
