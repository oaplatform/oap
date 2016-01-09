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
import oap.http.Cors;
import oap.io.Resources;
import oap.json.Binder;
import oap.util.Stream;
import oap.util.Strings;
import org.apache.commons.collections4.ListUtils;

import java.net.URL;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

@EqualsAndHashCode
@ToString
public class WsConfig {
    public LinkedHashMap<String, Service> services = new LinkedHashMap<>();
    public LinkedHashMap<String, Service> handlers = new LinkedHashMap<>();

    public static List<WsConfig> fromClassPath() {
        return Stream.of( ListUtils.union(
            Resources.urls( "META-INF/oap-ws.json" ),
            Resources.urls( "META-INF/oap-ws.conf" )
        ) )
            .map( WsConfig::parse )
            .toList();
    }

    public static WsConfig parse( URL url ) {
        return parse( Strings.readString( url ) );
    }

    public static WsConfig parse( String json ) {
        Objects.nonNull( json );
        return Binder.hocon.unmarshal( WsConfig.class, json );
    }

    @EqualsAndHashCode
    @ToString
    public static class Service {
        public String service;
        public Cors cors = new Cors();
        public boolean local = false;
    }
}
