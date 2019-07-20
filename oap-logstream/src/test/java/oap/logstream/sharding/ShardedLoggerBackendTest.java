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

package oap.logstream.sharding;

import oap.logstream.AvailabilityReport;
import oap.logstream.LoggerBackend;
import oap.logstream.NoLoggerConfiguredForShardsException;
import oap.util.Stream;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

import static oap.logstream.AvailabilityReport.State.FAILED;
import static oap.logstream.AvailabilityReport.State.OPERATIONAL;
import static oap.logstream.AvailabilityReport.State.PARTIALLY_OPERATIONAL;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class ShardedLoggerBackendTest {

    ShardMapper mapper = mock( ShardMapper.class );

    @Test
    public void routing() {
        LoggerBackend log1 = mock( LoggerBackend.class );
        LoggerBackend log2 = mock( LoggerBackend.class );

        LoggerShardRange shard0To100 = new LoggerShardRange( log1, 0, 100 );
        LoggerShardRange shard100To200 = new LoggerShardRange( log2, 100, 200 );

        List<LoggerShardRange> shards = Arrays.asList( shard0To100, shard100To200 );
        ShardedLoggerBackend slb = new ShardedLoggerBackend( shards, mapper );
        when( mapper.getShardNumber( anyString(), eq( "34/df/file1" ), any( byte[].class ) ) ).thenReturn( 34 );
        when( mapper.getShardNumber( anyString(), eq( "142/345/file1" ), any( byte[].class ) ) ).thenReturn( 142 );

        slb.log( "localhost", "34/df/file1", "t1", 1, 1, "line1" );
        slb.log( "localhost", "142/345/file1", "t1", 1, 1, "line2" );

        verify( log1 ).log( "localhost", "34/df/file1", "t1", 1, 1, "line1\n".getBytes(), 0, "line1\n".getBytes().length );
        verify( log2 ).log( "localhost", "142/345/file1", "t1", 1, 1, "line2\n".getBytes(), 0, "line2\n".getBytes().length );
    }

    @Test( expectedExceptions = NoLoggerConfiguredForShardsException.class )
    public void unconfiguredShards() {
        LoggerBackend log1 = mock( LoggerBackend.class );

        LoggerShardRange shard0To100 = new LoggerShardRange( log1, 0, 100 );
        LoggerShardRange shard100To200 = new LoggerShardRange( log1, 110, 200 );

        List<LoggerShardRange> shards = Arrays.asList( shard0To100, shard100To200 );
        new ShardedLoggerBackend( shards, mapper );
    }

    @Test
    public void availability() {
        LoggerBackend log1 = mock( LoggerBackend.class );
        LoggerBackend log2 = mock( LoggerBackend.class );

        LoggerShardRange shard0To100 = new LoggerShardRange( log1, 0, 100 );
        LoggerShardRange shard100To200 = new LoggerShardRange( log2, 100, 200 );

        List<LoggerShardRange> shards = Arrays.asList( shard0To100, shard100To200 );
        ShardedLoggerBackend slb = new ShardedLoggerBackend( shards, mapper );


        when( log1.availabilityReport() ).thenReturn( new AvailabilityReport( OPERATIONAL ) );
        when( log2.availabilityReport() ).thenReturn( new AvailabilityReport( OPERATIONAL ) );
        assertEquals( slb.availabilityReport().state, OPERATIONAL );
        assertEquals( slb.availabilityReport().subsystemStates.size(), 2 );
        assertTrue( Stream.of( slb.availabilityReport().subsystemStates.values() ).allMatch( s -> s == OPERATIONAL ) );

        when( log1.availabilityReport() ).thenReturn( new AvailabilityReport( FAILED ) );
        when( log2.availabilityReport() ).thenReturn( new AvailabilityReport( FAILED ) );
        assertEquals( slb.availabilityReport().state, FAILED );
        assertEquals( slb.availabilityReport().subsystemStates.size(), 2 );
        assertTrue( Stream.of( slb.availabilityReport().subsystemStates.values() ).allMatch( s -> s == FAILED ) );

        when( log1.availabilityReport() ).thenReturn( new AvailabilityReport( OPERATIONAL ) );
        when( log2.availabilityReport() ).thenReturn( new AvailabilityReport( FAILED ) );
        assertEquals( slb.availabilityReport().state, PARTIALLY_OPERATIONAL );
        assertEquals( slb.availabilityReport().subsystemStates.size(), 2 );
        assertTrue( slb.availabilityReport().subsystemStates.values().contains( OPERATIONAL ) );
        assertTrue( slb.availabilityReport().subsystemStates.values().contains( FAILED ) );
    }
}
