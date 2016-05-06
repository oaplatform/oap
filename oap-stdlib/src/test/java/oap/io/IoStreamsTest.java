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

import oap.io.IoStreams.Encoding;
import oap.testng.AbstractTest;
import oap.testng.Asserts;
import oap.testng.Env;
import oap.util.Lists;
import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;

import static oap.io.IoStreams.Encoding.GZIP;
import static oap.io.IoStreams.Encoding.LZ4;
import static oap.io.IoStreams.Encoding.PLAIN;
import static oap.testng.Asserts.assertFile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.Assert.assertEquals;

public class IoStreamsTest extends AbstractTest {
   @Test
   public void emptyGz() throws IOException {
      Path path = Env.tmpPath( "test.gz" );
      OutputStream out = IoStreams.out( path, GZIP );
      out.flush();
      out.close();
      InputStream in = IoStreams.in( path, GZIP );
      assertThat( in.read() ).isEqualTo( -1 );
      in.close();
      assertFile( path ).hasContent( "", GZIP );
   }

   @Test
   public void append() throws IOException {
      Path path = Env.tmpPath( "test.gz" );
      for( Encoding encoding : Lists.of( PLAIN, GZIP ) ) {
         OutputStream out = IoStreams.out( path, encoding );
         out.write( "12345".getBytes() );
         out.flush();
         out.close();
         out = IoStreams.out( path, encoding, true );
         out.write( "12345".getBytes() );
         out.flush();
         out.close();
         assertFile( path ).hasContent( "1234512345", encoding );
      }
   }

   @Test
   public void testLZ4() throws IOException {
      Path path = Env.tmpPath( "test.lz4" );

      OutputStream out = IoStreams.out( path, LZ4 );
      out.write( "12345".getBytes() );
      out.flush();
      out.close();

      assertFile( path ).hasContent( "12345", LZ4 );
   }
}
