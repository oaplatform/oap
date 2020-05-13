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

import ch.qos.logback.classic.LoggerContext;
import oap.json.Binder;
import oap.reflect.TypeRef;
import oap.testng.TestDirectoryFixture;
import oap.util.Lists;
import oap.util.Try;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static oap.benchmark.Benchmark.benchmark;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by igor.petrenko on 2020-05-13.
 */
public class MessageProtocolPerformance {

    private static final int SAMPLES = 10000;
    private static final int EXPERIMENTS = 5;

    @Test
    public void testMessageProtocolPerf() {
        assertThat( run( 1, 1 ) ).isEqualTo( SAMPLES * EXPERIMENTS + SAMPLES );
        assertThat( run( 500, 1 ) ).isEqualTo( SAMPLES * EXPERIMENTS + SAMPLES );
        assertThat( run( 500, 2 ) ).isEqualTo( SAMPLES * EXPERIMENTS + SAMPLES );
        assertThat( run( 500, 32 ) ).isEqualTo( SAMPLES * EXPERIMENTS + SAMPLES );
        assertThat( run( 500, 128 ) ).isEqualTo( SAMPLES * EXPERIMENTS + SAMPLES );
    }

    public int run( int threads, int poolSize ) {
        LoggerContext loggerContext = ( LoggerContext ) LoggerFactory.getILoggerFactory();
        try {
            loggerContext.stop();

            var listener = new PerfMessageListener();

            var counter = new AtomicLong();

            try( var server = new MessageServer( TestDirectoryFixture.testPath( "controlStatePath.st" ), 0, List.of( listener ), -1 ) ) {
                server.start();
                try( var client1 = new MessageSender( "localhost", server.getPort(), TestDirectoryFixture.testPath( "tmp" ) );
                     var client2 = new MessageSender( "localhost", server.getPort(), TestDirectoryFixture.testPath( "tmp" ) ) ) {
                    client1.retryAfter = 1;
                    client2.retryAfter = 1;

                    client1.poolSize = poolSize;
                    client2.poolSize = poolSize;

                    client1.start();
                    client2.start();


                    var benchmark = benchmark( "msg-t" + threads + "-p" + poolSize, SAMPLES, ( c ) -> {
                        var res = new ArrayList<CompletableFuture<MessageStatus>>();
                        var client = c % 2 == 0 ? client1 : client2;
                        res.add( client.sendJson( PerfMessageListener.ID, Map.of( "a", 1, "b", counter.incrementAndGet() ), s -> null ) );

                        CompletableFuture.allOf( res.toArray( new CompletableFuture[0] ) ).get( 5, TimeUnit.MINUTES );

                        assertThat( Lists.map( res, Try.map( CompletableFuture::get ) ) ).containsOnly( MessageStatus.OK );
                    } ).experiments( EXPERIMENTS );

                    if( threads > 1 ) benchmark = benchmark.inThreads( threads, SAMPLES );

                    benchmark.run();
                }
            }

            return listener.sum.get();
        } finally {
            loggerContext.start();
        }
    }

    public static class PerfMessageListener implements MessageListener {
        public static final byte ID = 12;

        public final AtomicInteger sum = new AtomicInteger();

        @Override
        public byte getId() {
            return ID;
        }

        @Override
        public String getInfo() {
            return "perf-test";
        }

        @Override
        public short run( int version, String hostName, int size, byte[] data ) {

            var map = Binder.json.unmarshal( new TypeRef<Map>() {}, data );

            var count = ( Number ) map.get( "a" );

            sum.addAndGet( count.intValue() );

            return MessageProtocol.STATUS_OK;
        }
    }
}
