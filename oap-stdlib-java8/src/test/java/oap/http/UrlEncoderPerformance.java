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

import com.google.api.client.util.escape.CharEscapers;
import org.testng.annotations.Test;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static oap.benchmark.Benchmark.benchmark;
import static org.assertj.core.api.Assertions.assertThat;

public class UrlEncoderPerformance {

    public static final int SAMPLES = 1000000;

    @Test
    public void encode() throws UnsupportedEncodingException {
        String value = "sdihgjf sdkgh dsfkjgh?&skfjh ?&. \tdkjhgf&amp;";

        assertThat( URLEncoder.encode( value, StandardCharsets.UTF_8.name() ) )
            .isEqualTo( CharEscapers.escapeUri( value ) );

        benchmark( "URLEncoder.encode", SAMPLES,
            () -> URLEncoder.encode( value, StandardCharsets.UTF_8.name() )
        ).run();

        benchmark( "CharEscapers.escapeUri", SAMPLES,
            () -> CharEscapers.escapeUri( value )
        ).run();
    }

    @Test
    public void decode() throws UnsupportedEncodingException {
        String value = URLEncoder.encode( "sdihgjf sdkgh dsfkjgh?&skfjh ?&. \tdkjhgf&amp;", StandardCharsets.UTF_8.name() );

        assertThat( URLDecoder.decode( value, StandardCharsets.UTF_8.name() ) )
            .isEqualTo( CharEscapers.decodeUri( value ) );

        benchmark( "URLDecoder.decode", SAMPLES,
            () -> URLDecoder.decode( value, StandardCharsets.UTF_8.name() )
        ).run();

        benchmark( "CharEscapers.decodeUri", SAMPLES,
            () -> CharEscapers.decodeUri( value )
        ).run();
    }

}
