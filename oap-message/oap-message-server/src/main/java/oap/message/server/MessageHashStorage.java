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

package oap.message.server;

import oap.util.Longs;
import org.apache.commons.codec.DecoderException;
import org.joda.time.DateTimeUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.nio.charset.StandardCharsets.UTF_8;

public class MessageHashStorage {
    final ConcurrentHashMap<Byte, ClientInfo> map = new ConcurrentHashMap<>();
    private final int size;

    public MessageHashStorage( int size ) {
        this.size = size;
    }

    public void load( Path path ) throws IOException, DecoderException {
        try( var stream = Files.lines( path ) ) {
            var clientId = 0L;
            var messageType = ( byte ) 0;
            var hmap = new LinkedHashMap<String, Long>();

            var it = stream.iterator();
            while( it.hasNext() ) {
                var line = it.next();
                if( line.isBlank() ) continue;

                if( line.startsWith( "---" ) ) {
                    if( clientId != 0 ) {
                        map.put( messageType, new ClientInfo( clientId, size, hmap ) );
                        hmap = new LinkedHashMap<>();
                        clientId = 0;
                        messageType = 0;
                    }
                } else {
                    var arr = line.split( " - " );

                    if( clientId == 0 ) {
                        messageType = Byte.parseByte( arr[0] );
                        clientId = Long.parseLong( arr[1] );
                    } else {
                        hmap.put( arr[0], Long.parseLong( arr[1] ) );
                    }
                }
            }

            if( clientId != 0 )
                map.put( messageType, new ClientInfo( clientId, size, hmap ) );
        }
    }

    public void store( Path path ) throws IOException {
        oap.io.Files.ensureFile( path );

        try( var fos = new FileOutputStream( path.toFile() );
             var sw = new OutputStreamWriter( fos, UTF_8 ) ) {

            List<Byte> keys = new ArrayList<>( map.keySet() );
            keys.sort( Byte::compareTo );

            for( var messageType : keys ) {
                var hmap = map.get( messageType );
                if( hmap == null ) continue;
                sw.write( "---\n" );
                sw.write( messageType + " - " + hmap.clientId + "\n" );
                hmap.store( sw );
            }
        }
    }

    public boolean contains( byte messageType, String md5 ) {
        var hmap = map.get( messageType );
        return hmap != null && hmap.containsKey( md5 );
    }

    public void add( byte messageType, long clientId, String md5 ) {
        var hmap = map.computeIfAbsent( messageType, cid -> new ClientInfo( clientId, size, new LinkedHashMap<>() ) );
        hmap.put( md5, DateTimeUtils.currentTimeMillis() );
    }

    public void update( long ttl ) {
        if( ttl <= 0 ) return;

        var now = DateTimeUtils.currentTimeMillis();

        map.entrySet().removeIf( entry -> {
            var hmap = entry.getValue();
            hmap.update( now, ttl );

            return hmap.isEmpty();
        } );
    }

    public long size() {
        return Longs.sum( map.values(), MessageHashStorage.ClientInfo::size );
    }

    public static final class ClientInfo {
        public final long clientId;
        public final LinkedHashMap<String, Long> map = new LinkedHashMap<>();
        private final int size;

        public ClientInfo( long clientId, int size, LinkedHashMap<String, Long> hmap ) {
            this.clientId = clientId;
            this.size = size;

            hmap.forEach( this::put );
        }

        public synchronized boolean containsKey( String md5 ) {
            return map.containsKey( md5 );
        }

        public synchronized boolean isEmpty() {
            return map.isEmpty();
        }

        public synchronized boolean put( String md5, long currentTimeMillis ) {
            if( containsKey( md5 ) ) return false;

            while( map.size() >= size ) {
                var firstKey = map.keySet().iterator().next();
                map.remove( firstKey );
            }
            map.put( md5, currentTimeMillis );
            return true;
        }

        public synchronized int size() {
            return map.size();
        }

        public synchronized void update( long now, long ttl ) {
            var it = map.entrySet().iterator();
            while( it.hasNext() ) {
                Map.Entry<String, Long> entry = it.next();
                var time = entry.getValue();
                if( now - time > ttl ) {
                    it.remove();
                }
            }
        }

        public synchronized void store( OutputStreamWriter sw ) throws IOException {
            for( var entry : map.entrySet() ) {
                var key = entry.getKey();
                var time = entry.getValue();

                sw.write( key + " - " + time + "\n" );
            }
        }
    }
}
