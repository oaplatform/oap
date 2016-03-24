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

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import oap.application.remote.RemoteInvocationHandler;
import oap.application.supervision.Supervisor;
import oap.io.Files;
import oap.json.Binder;
import oap.reflect.Reflect;
import oap.reflect.Reflection;
import oap.util.Maps;
import oap.util.Stream;
import org.apache.commons.collections4.CollectionUtils;

import java.net.URL;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static java.util.stream.Collectors.toSet;

@Slf4j
public class Kernel {
    private final List<URL> modules;
    private Supervisor supervisor = new Supervisor();

    public Kernel( List<URL> modules ) {
        log.debug( "modules = " + modules );
        this.modules = modules;
    }

    private Map<String, Module.Service> initializeServices( Map<String, Module.Service> services,
                                                            Set<String> initialized ) {

        HashMap<String, Module.Service> deferred = new HashMap<>();

        for( Map.Entry<String, Module.Service> entry : services.entrySet() ) {
            Module.Service service = entry.getValue();
            String serviceName = entry.getKey();
            if( initialized.containsAll( service.dependsOn ) ) {
                log.debug( "initializing " + serviceName );

                Reflection reflect = Reflect.reflect( service.implementation );

                Object instance;
                if( service.remoteUrl == null ) {
                    initializeServiceLinks( serviceName, service );
                    instance = reflect.newInstance( service.parameters );
                } else instance = RemoteInvocationHandler.proxy(
                    service.remoteUrl,
                    service.remoteName,
                    reflect.underlying
                );
                Application.register( serviceName, instance );
                if( service.supervision.supervise )
                    supervisor.startSupervised( serviceName, instance,
                        service.supervision.startWith,
                        service.supervision.stopWith );
                if( service.supervision.thread )
                    supervisor.startThread( serviceName, instance );
                else {
                    if( service.supervision.schedule && service.supervision.getDelay().isPresent() )
                        supervisor.scheduleWithFixedDelay( serviceName, ( Runnable ) instance,
                            service.supervision.getDelay().get(), TimeUnit.MILLISECONDS );
                    else if( service.supervision.schedule && service.supervision.cron != null )
                        supervisor.scheduleCron( serviceName, ( Runnable ) instance,
                            service.supervision.cron );
                }
                initialized.add( serviceName );
            } else {
                log.debug( "dependencies are not ready - deferring " + serviceName + ": "
                    + CollectionUtils.subtract( service.dependsOn, initialized ) );
                deferred.put( entry.getKey(), service );
            }
        }

        return deferred.size() == services.size() ? deferred : initializeServices( deferred, initialized );
    }

    @SuppressWarnings( "unchecked" )
    private void initializeServiceLinks( String name, Module.Service service ) {
        for( Map.Entry<String, Object> entry : service.parameters.entrySet() ) {
            final Object value = entry.getValue();
            final String key = entry.getKey();

            if( value instanceof String ) entry.setValue( resolve( name, key, value ) );
            else if( value instanceof List<?> ) {
                ListIterator<Object> it = ( ( List<Object> ) value ).listIterator();
                while( it.hasNext() ) it.set( resolve( name, key, it.next() ) );
            }
        }
    }

    private Object resolve( String name, String key, Object value ) {
        if( value instanceof String && ( ( String ) value ).startsWith( "@service:" ) ) {
            log.debug( "for " + name + " linking " + key + " -> " + value );
            Object link = Application.service( ( ( String ) value ).substring( "@service:".length() ) );
            if( link == null )
                throw new ApplicationException( "for " + name + " service link " + value + " is not found" );
            return link;
        }
        return value;
    }

    private Set<Module> initialize( Set<Module> modules, Set<String> initialized, Set<String> initializedServices ) {
        HashSet<Module> deferred = new HashSet<>();

        for( Module module : modules ) {
            log.debug( "initializing module " + module.name );
            if( initialized.containsAll( module.dependsOn ) ) {

                Map<String, Module.Service> def =
                    initializeServices( module.services, initializedServices );
                if( !def.isEmpty() ) {
                    List<String> names = Stream.of( def.entrySet().stream() ).map( Map.Entry::getKey ).toList();
                    log.error( "failed to initialize: " + names );
                    throw new ApplicationException( "failed to initialize services: " + names );
                }

                initialized.add( module.name );
            } else {
                log.debug( "dependencies are not ready - deferring " + module.name + ": "
                    + CollectionUtils.subtract( module.dependsOn, initialized ) );
                deferred.add( module );
            }
        }

        return deferred.size() == modules.size() ? deferred : initialize( deferred, initialized, initializedServices );
    }

    public void start() {
        start( Collections.emptyMap() );
    }

    @SuppressWarnings( "unchecked" )
    public void start( String config ) {
        start( ( Map<String, Map<String, Object>> ) Binder.hocon.unmarshal( Map.class, config ) );
    }

    public void start( Map<String, Map<String, Object>> config ) {
        log.debug( "initializing application kernel..." );

        Set<Module> moduleConfigs = Stream.of( modules )
            .map( m -> Module.parse( m, config ) )
            .collect( toSet() );
        log.trace( "modules = " + Stream.of( moduleConfigs ).map( m -> m.name ).toList() );

        Set<Module> def = initialize( moduleConfigs, new HashSet<>(), new HashSet<>() );
        if( !def.isEmpty() ) {
            log.error( "failed to initialize: " + Stream.of( def ).map( m -> m.name ).toList() );
            throw new ApplicationException( "failed to initialize modules" );
        }

        supervisor.start();
    }

    public void stop() {
        supervisor.stop();
        Application.unregisterServices();
    }

    public void start( Path configPath, Optional<Path> configDirectoryPath ) {
        configDirectoryPath.ifPresent( cdp -> log.info( "global configuration directory = {}", cdp ) );

        final String config = Files.readString( configPath );

        final String[] configs = configDirectoryPath.map(
            dir -> {
                ArrayList<Path> paths = Files.fastWildcard( dir, "*.conf" );

                log.info( "global configurations = {}", paths );

                return Stream
                    .of( paths.stream() )
                    .map( Files::readString )
                    .concat( Stream.of( config ) )
                    .toArray( String[]::new );
            }
        ).orElse( new String[]{ config } );

        log.info( "application configurations = {}", configPath );

        start( configPath.toFile().exists() ? toMap( configPath, configs ) : Maps.of() );
    }

    @SuppressWarnings( "unchecked" )
    private Map<String, Map<String, Object>> toMap( Path configPath, String[] configs ) {
        final Map<String, Object> map = Binder.hoconWithConfig( configs ).unmarshal(
            new TypeReference<Map<String, Object>>() {
            }, configPath );
        final Map<String, Object> filteredMap = com.google.common.collect.Maps.filterValues( map, ( v -> v instanceof Map ) );

        return ( Map<String, Map<String, Object>> ) ( Object ) filteredMap;
    }
}
