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
package oap.ws;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import oap.http.server.nio.NioHttpServer;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Optional;

@EqualsAndHashCode
@ToString
public class WsConfig {
    public boolean enabled = true;

    public final ArrayList<String> interceptors = new ArrayList<>();
    public LinkedHashSet<String> path = new LinkedHashSet<>();
    public boolean sessionAware;
    public boolean compression = true;
    public Optional<String> port = Optional.empty();
    public ArrayList<NioHttpServer.PortType> portType = new ArrayList<>();
    public boolean blocking = true;
}
