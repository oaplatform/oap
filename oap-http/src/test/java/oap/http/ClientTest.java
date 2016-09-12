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

package oap.http;

import lombok.extern.slf4j.Slf4j;
import oap.testng.AbstractTest;
import oap.testng.Env;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static oap.testng.Asserts.assertFile;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public class ClientTest extends AbstractTest {
   @Test
   public void download() {
      final Path path = Env.tmpPath( "new.file" );
      AtomicInteger progress = new AtomicInteger();
      final Optional<Path> download = Client.DEFAULT.download( "https://upload.wikimedia.org/wikipedia/commons/thumb/0/0d/OUN-r_Flag_1941.svg/250px-OUN-r_Flag_1941.svg.png",
         Optional.of( path ), progress::set );

      assertThat( download ).contains( path );
      assertThat( download ).isPresent();
      assertFile( path ).exists().hasSize( 560 );
      assertThat( progress.get() ).isEqualTo( 100 );
   }

   @Test
   public void downloadTempFile() {
      AtomicInteger progress = new AtomicInteger();
      final Optional<Path> download = Client.DEFAULT.download( "https://upload.wikimedia.org/wikipedia/commons/thumb/0/0d/OUN-r_Flag_1941.svg/250px-OUN-r_Flag_1941.svg.png", Optional.empty(), progress::set );
      assertThat( download ).isPresent();
      assertFile( download.get() ).exists().hasSize( 560 );
      assertThat( progress.get() ).isEqualTo( 100 );
   }
}
