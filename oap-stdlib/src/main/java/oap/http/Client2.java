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

import lombok.SneakyThrows;
import oap.io.IoStreams;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.nio.file.Path;
import java.security.KeyStore;

import static oap.io.IoStreams.Encoding.PLAIN;

/**
 * Created by igor.petrenko on 2020-03-19.
 */
public class Client2 {
    @SneakyThrows
    public static SSLContext createSSLContext( Path certificateLocation, String certificatePassword ) {
        try( var inputStream = IoStreams.in( certificateLocation, PLAIN ) ) {
            KeyStore keyStore = KeyStore.getInstance( "JKS" );
            keyStore.load( inputStream, certificatePassword.toCharArray() );

            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance( TrustManagerFactory.getDefaultAlgorithm() );
            trustManagerFactory.init( keyStore );

            SSLContext sslContext = SSLContext.getInstance( "TLS" );
            sslContext.init( null, trustManagerFactory.getTrustManagers(), null );

            return sslContext;
        }
    }
}
