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
import lombok.val;
import oap.testng.AbstractPerformance;
import org.testng.annotations.Test;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

public class UrlPerformance extends AbstractPerformance {

    public static final int SAMPLES = 1000000;

    @Test
    public void testEncode() throws UnsupportedEncodingException {
        val STRING = "sdihgjf sdkgh dsfkjgh?&skfjh ?&. \tdkjhgf&amp;";

        assertThat( URLEncoder.encode( STRING, StandardCharsets.UTF_8.name() ) )
            .isEqualTo( CharEscapers.escapeUri( STRING ) );

        benchmark( "URLEncoder.encode", SAMPLES, () -> {
            URLEncoder.encode( STRING, StandardCharsets.UTF_8.name() );
        } ).run();

        benchmark( "CharEscapers.escapeUri", SAMPLES, () -> {
            CharEscapers.escapeUri( STRING );
        } ).run();
    }

    @Test
    public void testDecode() throws UnsupportedEncodingException {
        val STRING = URLEncoder.encode( "sdihgjf sdkgh dsfkjgh?&skfjh ?&. \tdkjhgf&amp;", StandardCharsets.UTF_8.name() );

        assertThat( URLDecoder.decode( STRING, StandardCharsets.UTF_8.name() ) )
            .isEqualTo( CharEscapers.decodeUri( STRING ) );

        benchmark( "URLDecoder.decode", SAMPLES, () -> {
            URLDecoder.decode( STRING, StandardCharsets.UTF_8.name() );
        } ).run();

        benchmark( "CharEscapers.decodeUri", SAMPLES, () -> {
            CharEscapers.decodeUri( STRING );
        } ).run();
    }

}
