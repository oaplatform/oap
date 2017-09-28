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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import oap.application.remote.FST;
import oap.application.remote.RemoteLocation;
import oap.json.Binder;
import oap.reflect.Coercions;
import oap.reflect.Reflect;
import oap.reflect.Reflection;
import oap.util.Functions;
import oap.util.Stream;

import java.lang.reflect.Type;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.joining;

@EqualsAndHashCode
@ToString
public class Module {
    public static final ModuleConfiguration CONFIGURATION = new ModuleConfiguration();
    @SuppressWarnings( "unchecked" )
    static final Coercions coersions = Coercions.basic()
        .with( r -> r.underlying.isAssignableFrom( List.class ),
            ( r, list ) -> {
                if( list instanceof List<?>
                    && ( ( List<?> ) list ).stream().allMatch( o -> o instanceof Map<?, ?> ) ) {
                    return Stream.of( ( List<?> ) list )
                        .map( map -> Binder.json.unmarshal(
                            r.getCollectionComponentType().underlying, ( Map<String, Object> ) map ) )
                        .toList();
                } else return list;
            } )
        .with( r -> !r.assignableFrom( Map.class ),
            ( r, map ) -> map instanceof Map<?, ?> ? Binder.json.unmarshal( new TypeReference<Object>() {
                @Override
                public Type getType() {
                    return r.getType();
                }
            }, ( Map<String, Object> ) map ) : map )
        .withIdentity();
    public String name;
    public ArrayList<String> dependsOn = new ArrayList<>();
    public LinkedHashMap<String, Service> services = new LinkedHashMap<>();

    @EqualsAndHashCode( exclude = "remoteCache" )
    @ToString( exclude = "remoteCache" )
    @Slf4j
    public static class Service {
        public String implementation;
        public LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
        public Supervision supervision = new Supervision();
        public ArrayList<String> dependsOn = new ArrayList<>();
        public URI remoteUrl;
        public String remoteName;
        public Path certificateLocation;
        public String certificatePassword;
        public String profile;
        public String name;
        public Optional<Long> timeout = Optional.empty();
        public Optional<FST.SerializationMethod> serialization = Optional.empty();
        public LinkedHashMap<String, Object> listen = new LinkedHashMap<>();
        public RemoteLocation remote;
        public boolean enabled = true;
        @JsonIgnore
        private Supplier<RemoteLocation> remoteCache = Functions.memoize( () -> {
            log.warn( "service attributes remoteUrl, remoteName, cerrificateLocation, certificatePassword,"
                + " timeout, serialization are deprecated. Use remote { "
                + Reflect.reflect( RemoteLocation.class )
                .fields
                .values()
                .stream()
                .filter( f -> !f.isStatic() )
                .map( Reflection.Field::name )
                .collect( joining( ", " ) )
                + " } instead." );
            return new RemoteLocation( remoteUrl, remoteName, certificateLocation,
                certificatePassword, timeout.orElse( RemoteLocation.DEFAULT_TIMEOUT ),
                serialization.orElse( FST.SerializationMethod.DEFAULT ) );
        } );

        public RemoteLocation remoting() {
            return remoteName != null ? remoteCache.get() : remote;
        }

        @JsonIgnore
        public boolean isRemoteService() {
            return remoting() != null;
        }

    }

    @EqualsAndHashCode
    @ToString
    public static class Supervision {
        public boolean supervise;
        public boolean thread;
        public boolean schedule;
        public String startWith = "start";
        public List<String> stopWith = asList( "stop", "close" );
        public String reloadWith = "reload";
        public long delay; //ms
        public String cron; // http://www.quartz-scheduler.org/documentation/quartz-2.x/tutorials/crontrigger
    }

}
