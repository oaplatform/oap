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

package oap.message;

import lombok.ToString;
import oap.message.MessageProtocol.ClientId;
import oap.util.ByteSequence;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.joda.time.DateTimeUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;

import static java.nio.charset.StandardCharsets.UTF_8;

public class MessageHashStorage {
    final ConcurrentHashMap<ClientId, ClientInfo> map = new ConcurrentHashMap<>();
    private final int size;

    public MessageHashStorage( int size ) {
        this.size = size;
    }

    public void load( Path path ) throws IOException, DecoderException {
        try( var stream = Files.lines( path ) ) {
            var clientId = 0L;
            var messageType = ( byte ) 0;
            var hmap = new ClientInfo( size );

            var it = stream.iterator();
            while( it.hasNext() ) {
                var line = it.next();
                if( line.isBlank() ) continue;

                if( line.startsWith( "---" ) ) {
                    if( clientId != 0 ) {
                        map.put( new ClientId( messageType, clientId ), hmap );
                        hmap = new ClientInfo( size );
                        clientId = 0;
                        messageType = 0;
                    }
                } else {
                    var arr = line.split( " - " );

                    if( clientId == 0 ) {
                        messageType = Byte.parseByte( arr[0] );
                        clientId = Long.parseLong( arr[1] );
                    } else {
                        hmap.put( ByteSequence.of( Hex.decodeHex( arr[0].toCharArray() ) ), Long.parseLong( arr[1] ) );
                    }
                }
            }

            if( clientId != 0 )
                map.put( new ClientId( messageType, clientId ), hmap );
        }
    }

    public void store( Path path ) throws IOException {
        oap.io.Files.ensureFile( path );

        try( var fos = new FileOutputStream( path.toFile() );
             var sw = new OutputStreamWriter( fos, UTF_8 ) ) {

            for( var entry : map.entrySet() ) {
                sw.write( "---\n" );

                var cid = entry.getKey();
                var hmap = entry.getValue();

                sw.write( cid.messageType + " - " + cid.clientId + "\n" );

                synchronized( hmap ) {
                    for( var hentry : hmap.list )
                        sw.write( Hex.encodeHexString( hentry.hash.bytes ) + " - " + hentry.time + "\n" );
                }
            }

        }

    }

    public boolean contains( int messageType, long clientId, byte[] md5 ) {
        var hmap = map.get( new ClientId( messageType, clientId ) );
        return hmap != null && hmap.containsKey( ByteSequence.of( md5 ) );
    }

    public void add( int messageType, long clientId, byte[] md5 ) {
        var hmap = map.computeIfAbsent( new ClientId( messageType, clientId ), cid -> new ClientInfo( size ) );
        hmap.put( ByteSequence.of( md5 ), DateTimeUtils.currentTimeMillis() );
    }

    public void update( long ttl ) {
        if( ttl <= 0 ) return;

        var now = DateTimeUtils.currentTimeMillis();

        map.entrySet().removeIf( entry -> {
            var hmap = entry.getValue();

            synchronized( hmap ) {
                var it = hmap.list.iterator();
                while( it.hasNext() ) {
                    var hashInfo = it.next();
                    if( now - hashInfo.time > ttl ) {
                        it.remove();
                        hmap.map.remove( hashInfo.hash );
                    }
                }
            }

            return hmap.isEmpty();
        } );
    }

    public long size() {
        return map.values().stream().mapToLong( MessageHashStorage.ClientInfo::size ).sum();
    }

    @ToString
    private static final class HashInfo {
        public final ByteSequence hash;
        public final long time;

        public HashInfo( ByteSequence hash, long time ) {
            this.hash = hash;
            this.time = time;
        }
    }

    public static final class ClientInfo {
        public final ConcurrentHashMap<ByteSequence, ByteSequence> map = new ConcurrentHashMap<>();
        public final LinkedList<HashInfo> list;
        private final int size;

        public ClientInfo( int size ) {
            list = new LinkedList<>();
            this.size = size;
        }

        public boolean containsKey( ByteSequence key ) {
            return map.containsKey( key );
        }

        public boolean isEmpty() {
            return map.isEmpty();
        }

        public synchronized void put( ByteSequence hash, long currentTimeMillis ) {
            if( list.size() == size ) {
                var rhash = list.removeFirst();
                map.remove( rhash.hash );
            }
            list.addLast( new HashInfo( hash, currentTimeMillis ) );
            map.put( hash, hash );
        }

        public int size() {
            return map.size();
        }
    }
}
