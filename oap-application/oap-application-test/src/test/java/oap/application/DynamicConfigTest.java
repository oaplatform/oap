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

package oap.application;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import oap.io.Files;
import oap.testng.TestDirectoryFixture;
import org.testng.annotations.Test;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import static oap.io.content.ContentWriter.ofString;
import static oap.testng.Asserts.urlOfTestResource;
import static org.assertj.core.api.Assertions.assertThat;

public class DynamicConfigTest {

    @Test
    public void defaultConfig() throws MalformedURLException {
        Path update = TestDirectoryFixture.testPath( "update.conf" );

        DynamicConfig<Cfg> config = new DynamicConfig<>( 10, Cfg.class,
            urlOfTestResource( getClass(), "default.conf" ),
            update.toUri().toURL()
        );
        AtomicInteger updates = new AtomicInteger( 0 );
        config.addListener( updates::incrementAndGet );
        assertThat( config.value ).isEqualTo( new Cfg( "value1" ) );

        Files.write( update, "{parameter=\"valueUpdated\"}", ofString() );
        config.control.sync();
        config.control.sync();
        config.control.sync();
        config.control.sync();
        assertThat( config.value ).isEqualTo( new Cfg( "valueUpdated" ) );
        assertThat( updates.get() ).isEqualTo( 1 );
    }
}

@EqualsAndHashCode
@ToString
class Cfg {
    String parameter;

    @JsonCreator
    Cfg( String parameter ) {
        this.parameter = parameter;
    }
}
