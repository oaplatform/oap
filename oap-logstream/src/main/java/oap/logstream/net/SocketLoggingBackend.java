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

package oap.logstream.net;

import lombok.extern.slf4j.Slf4j;
import oap.concurrent.scheduler.Scheduled;
import oap.concurrent.scheduler.Scheduler;
import oap.io.Closeables;
import oap.logstream.LoggingBackend;
import oap.metrics.Metrics;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Slf4j
public class SocketLoggingBackend implements LoggingBackend {

   private final String host;
   private final int port;
   private final Scheduled scheduled;
   protected int maxBuffers = 5000;
   protected long timeout = 5000;
   protected boolean blocking = true;
   private Connection connection;
   private Buffers buffers;
   private boolean loggingAvailable = true;
   private boolean closed = false;

   public SocketLoggingBackend( String host, int port, Path location, int bufferSize, long flushInterval ) {
      this.host = host;
      this.port = port;
      this.buffers = new Buffers( location, bufferSize );
      this.scheduled = Scheduler.scheduleWithFixedDelay( flushInterval, TimeUnit.MILLISECONDS, this::send );
      Metrics.measureGauge( Metrics.name( "logging.buffers_cache" ), () -> buffers.cache.size() );
   }

   public SocketLoggingBackend( String host, int port, Path location, int bufferSize ) {
      this( host, port, location, bufferSize, 5000 );
   }

   public synchronized void send() {
      if( !closed ) try {
         if( buffers.isEmpty() ) loggingAvailable = true;

         refreshConnection();

         log.debug( "sending data to server..." );

         buffers.forEachReadyData( buffer -> {
            if( !sendBuffer( buffer ) ) {
               log.debug( "send unsuccessful..." );
               refreshConnection();
               return sendBuffer( buffer );
            }
            return true;

         } );
         log.debug( "sending done" );
      } catch( Exception e ) {
         loggingAvailable = false;
         log.warn( e.getMessage() );
         log.trace( e.getMessage(), e );
         Closeables.close( connection );
      }

      if( !loggingAvailable ) log.debug( "logging unavailable" );

   }

   private void refreshConnection() {
      if( this.connection == null || !connection.isConnected() ) {
         Closeables.close( connection );
         log.debug( "opening connection..." );
         this.connection = blocking ? new SocketConnection( host, port, timeout ) : new ChannelConnection( host, port, timeout );
         log.debug( "connected!" );
      }
   }

   private Boolean sendBuffer( Buffer buffer ) {
      return Metrics.measureTimer( Metrics.name( "logging.buffer_send_time" ), () -> {
         try {
            log.trace( "sending {}", buffer );
            connection.write( buffer.data(), 0, buffer.length() );
            int size = connection.read();
            if( size <= 0 ) {
               loggingAvailable = false;
               log.error( "pong size {}. The end of the stream is reached.", size );
               return false;
            }
            Metrics.measureCounterIncrement( Metrics.name( "logging.socket" ), buffer.length() );
            loggingAvailable = true;
            return true;
         } catch( Exception e ) {
            loggingAvailable = false;
            if( log.isWarnEnabled() ) log.warn( e.getMessage() );
            else if( log.isTraceEnabled() ) log.warn( e.getMessage(), e );
            Closeables.close( connection );
            return false;
         }
      } );
   }

   @Override
   public void log( String hostName, String fileName, byte[] buffer, int offset, int length ) {
      buffers.put( fileName, buffer, offset, length );
   }

   @Override
   public synchronized void close() {
      closed = true;
      Scheduled.cancel( scheduled );
      Closeables.close( connection );
      Closeables.close( buffers );
   }

   @Override
   public boolean isLoggingAvailable() {
      return loggingAvailable && !closed && buffers.readyBuffers() < maxBuffers;
   }
}
