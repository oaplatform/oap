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

package oap.remote;

import lombok.ToString;

import java.net.URI;

@ToString
public class RemoteLocation {
    public static final long DEFAULT_TIMEOUT = 5000L;
    public URI url;
    public String name;
    public long timeout = DEFAULT_TIMEOUT;
    public FST.SerializationMethod serialization = FST.SerializationMethod.DEFAULT;
    public int retry = 0;

    public RemoteLocation() {
    }

    public RemoteLocation( URI url,
                           String name,
                           long timeout,
                           FST.SerializationMethod serialization,
                           int retry ) {
        this.url = url;
        this.name = name;
        this.timeout = timeout;
        this.serialization = serialization;
        this.retry = retry;
    }
}
