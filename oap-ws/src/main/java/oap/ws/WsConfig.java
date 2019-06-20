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

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import oap.application.Configuration;
import oap.http.Protocol;
import oap.http.cors.CorsPolicy;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

@EqualsAndHashCode
@ToString
public class WsConfig {
    public static final Configuration<WsConfig> CONFIGURATION = new Configuration<>( WsConfig.class, "oap-ws" );

    @JsonAlias( { "profile", "profiles" } )
    public final LinkedHashSet<String> profiles = new LinkedHashSet<>();

    public final LinkedHashMap<String, Service> services = new LinkedHashMap<>();
    public final LinkedHashMap<String, Service> handlers = new LinkedHashMap<>();
    public final ArrayList<String> interceptors = new ArrayList<>();

    public String name;

    @EqualsAndHashCode
    @ToString
    public static class Service {
        @JsonAlias( { "profile", "profiles" } )
        public final LinkedHashSet<String> profiles = new LinkedHashSet<>();
        public String service;
        public CorsPolicy corsPolicy = null;
        public Protocol protocol;
        public boolean sessionAware;
    }
}
