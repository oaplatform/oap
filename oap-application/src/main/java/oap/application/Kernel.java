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

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import oap.application.remote.RemoteInvocationHandler;
import oap.application.supervision.Supervisor;
import oap.json.Binder;
import oap.metrics.Metrics;
import oap.reflect.Reflect;
import oap.reflect.ReflectException;
import oap.reflect.Reflection;
import oap.util.Optionals;
import oap.util.Sets;
import oap.util.Stream;
import oap.util.Strings;
import org.apache.commons.lang3.StringUtils;

import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.util.Collections.emptyMap;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.apache.commons.collections4.CollectionUtils.subtract;

@Slf4j
@ToString( of = "name" )
public class Kernel implements Iterable<Map.Entry<String, Object>> {
    public static final String DEFAULT = Strings.DEFAULT;
    final String name;
    private final List<URL> configurations;
    private final Set<String> profiles = Sets.empty();
    private final Set<Module> modules = Sets.empty();
    private final ConcurrentMap<String, Object> services = new ConcurrentHashMap<>();
    private final Supervisor supervisor = new Supervisor();
    private final List<DynamicConfig.Control> dynamicConfigurations = new ArrayList<>();

    public Kernel( String name, List<URL> configurations ) {
        this.name = name;
        this.configurations = configurations;
    }

    public Kernel( List<URL> configurations ) {
        this( DEFAULT, configurations );
    }

    private Map<String, Module.Service> initializeServices( Map<String, Module.Service> services,
                                                            Set<String> initialized, ApplicationConfiguration config ) {

        HashMap<String, Module.Service> deferred = new HashMap<>();

        for( Map.Entry<String, Module.Service> entry : services.entrySet() ) {
            Module.Service service = entry.getValue();
            if( !service.enabled ) {
                initialized.add( service.name );
                log.debug( "service {} is disabled.", entry.getKey() );
                continue;
            }
            String serviceName = service.name != null ? service.name : entry.getKey();
            if( service.profile != null && !config.profiles.contains( service.profile ) ) {
                log.debug( "skipping " + entry.getKey() + " with profile " + service.profile );
                continue;
            }

            List<String> dependsOn = Stream.of( service.dependsOn ).filter( this::serviceEnabled ).toList();

            if( initialized.containsAll( dependsOn ) ) {
                log.debug( "initializing {} as {}", entry.getKey(), serviceName );

                if( service.implementation == null ) {
                    throw new ApplicationException( "failed to initialize service: " + serviceName + ". implementation == null" );
                }
                @SuppressWarnings( "unchecked" )
                Reflection reflect = Reflect.reflect( service.implementation, Module.coersions );

                Object instance;
                if( !service.isRemoteService() ) {
                    try {
                        initializeServiceLinks( serviceName, service );
                        instance = reflect.newInstance( service.parameters );
                        initializeDynamicConfigurations( reflect, instance );
                        initializeListeners( service.listen, instance );
                    } catch( ReflectException e ) {
                        log.info( "service name = {}, remoteName = {}, profile = {}", service.name, service.remoteName, service.profile );
                        throw e;
                    }
                } else instance = RemoteInvocationHandler.proxy(
                    service.remoting(),
                    reflect.underlying );
                register( serviceName, instance );
                if( !serviceName.equals( entry.getKey() ) )
                    register( entry.getKey(), instance );

                if( service.supervision.supervise )
                    supervisor.startSupervised( serviceName, instance,
                        service.supervision.startWith,
                        service.supervision.stopWith,
                        service.supervision.reloadWith );
                if( service.supervision.thread )
                    supervisor.startThread( serviceName, instance );
                else {
                    if( service.supervision.schedule && service.supervision.cron != null )
                        supervisor.scheduleCron( serviceName, ( Runnable ) instance,
                            service.supervision.cron );
                    else if( service.supervision.schedule && service.supervision.delay != 0 )
                        supervisor.scheduleWithFixedDelay( serviceName, ( Runnable ) instance,
                            service.supervision.delay, MILLISECONDS );
                }
                initialized.add( serviceName );
            } else {
                log.debug( "dependencies are not ready - deferring " + serviceName + ": "
                    + subtract( service.dependsOn, initialized ) );
                deferred.put( entry.getKey(), service );
            }
        }

        return deferred.size() == services.size() ? deferred
            : initializeServices( deferred, initialized, config );
    }

    private void initializeDynamicConfigurations( Reflection reflect, Object instance ) {
        reflect.fields
            .values()
            .stream()
            .filter( field -> field.type().assignableFrom( DynamicConfig.class ) )
            .forEach( f -> {
                DynamicConfig<?> config = ( DynamicConfig<?> ) f.get( instance );
                if( config.isUpdateable() ) dynamicConfigurations.add( config.control );
            } );
    }

    @SuppressWarnings( "unchecked" )
    private void initializeServiceLinks( String name, Module.Service service ) {
        initializeServiceLinks( name, service.parameters );
        initializeServiceLinks( name, service.listen );
    }

    @SuppressWarnings( "unchecked" )
    private void initializeServiceLinks( String name, LinkedHashMap<String, Object> map ) {
        for( Map.Entry<String, Object> entry : map.entrySet() ) {
            final Object value = entry.getValue();
            final String key = entry.getKey();

            if( value instanceof String ) entry.setValue( resolve( name, key, value, true ) );
            else if( value instanceof List<?> ) {
                ListIterator<Object> it = ( ( List<Object> ) value ).listIterator();
                while( it.hasNext() ) {
                    final Object link = resolve( name, key, it.next(), false );

                    if( link != null ) it.set( link );
                    else it.remove();
                }
            }
        }
    }

    private void initializeListeners( Map<String, Object> listeners, Object instance ) {
        listeners.forEach( ( listener, service ) -> {
            log.debug( "setting " + instance + " to listen to " + service + " with " + listener );
            String methodName = "add" + StringUtils.capitalize( listener ) + "Listener";
            Optionals.fork( Reflect.reflect( service.getClass() ).method( methodName ) )
                .ifPresent( m -> m.invoke( service, instance ) )
                .ifAbsentThrow( () -> new ReflectException( "listener " + listener
                    + " should have method " + methodName + " in " + service ) );
        } );
    }

    private Object resolve( String name, String key, Object value, boolean throwErrorIfNotFound ) {
        if( value instanceof String && ( ( String ) value ).startsWith( "@service:" ) ) {
            final String linkName = ( ( String ) value ).substring( "@service:".length() );
            Object link = service( linkName );
            log.debug( "for {} linking {} -> {} as {}", name, key, value, link );
            if( link == null && throwErrorIfNotFound && serviceEnabled( linkName ) )
                throw new ApplicationException( "for " + name + " service link " + value + " is not found" );
            return link;
        }
        return value;
    }

    private Set<Module> initialize( Set<Module> modules, Set<String> initialized, Set<String> initializedServices, ApplicationConfiguration config ) {
        HashSet<Module> deferred = new HashSet<>();

        for( Module module : modules ) {
            log.debug( "initializing module " + module.name );
            if( initialized.containsAll( module.dependsOn ) ) {

                Map<String, Module.Service> def =
                    initializeServices( module.services, initializedServices, config );
                if( !def.isEmpty() ) {
                    Set<String> names = Sets.map( def.entrySet(), Map.Entry::getKey );
                    log.error( "failed to initialize: " + names );
                    throw new ApplicationException( "failed to initialize services: " + names );
                }

                initialized.add( module.name );
            } else {
                log.debug( "dependencies are not ready - deferring " + module.name + ": "
                    + subtract( module.dependsOn, initialized ) );
                deferred.add( module );
            }
        }

        return deferred.size() == modules.size() ? deferred
            : initialize( deferred, initialized, initializedServices, config );
    }

    public void start() {
        start( new ApplicationConfiguration() );
    }

    public void start( Path appConfigPath, Path confd ) {
        start( ApplicationConfiguration.load( appConfigPath, confd ) );
    }

    public void start( Path appConfigPath ) {
        start( appConfigPath, emptyMap() );
    }

    public void start( Path appConfigPath, Map<Object, Object> properties ) {
        Map<Object, Object> map = new HashMap<>();
        map.putAll( System.getProperties() );
        map.putAll( properties );

        start( ApplicationConfiguration.load( appConfigPath, new String[] { Binder.json.marshal( map ) } ) );
    }

    private void start( ApplicationConfiguration config ) {
        log.debug( "initializing application kernel..." );
        Application.register( this );
        log.debug( "application config {}", config );
        this.profiles.addAll( config.profiles );

        this.modules.addAll( Stream.of( configurations )
            .map( module -> Module.CONFIGURATION.fromHocon( module, config.services ) )
            .toSet() );
        log.debug( "modules = " + Sets.map( this.modules, m -> m.name ) );
        log.trace( "modules configs = " + this.modules );

        Set<Module> def = initialize( this.modules, new HashSet<>(), new HashSet<>(), config );
        if( !def.isEmpty() ) {
            Set<String> names = Sets.map( def, m -> m.name );
            log.error( "failed to initialize: {} ", names );
            throw new ApplicationException( "failed to initialize modules: " + names );
        }

        this.dynamicConfigurations.forEach( DynamicConfig.Control::start );

        this.supervisor.start();

        this.modules.add( new Module( Module.DEFAULT ) );
        log.debug( "application kernel started" );
    }

    public void stop( String service ) {
        log.debug( "stopping {}...", service );
        supervisor.stop( service );
        unregister( service );
        log.debug( "{} stopped", service );
    }

    public void stop() {
        log.debug( "stopping application kernel " + name + "..." );
        this.dynamicConfigurations.forEach( DynamicConfig.Control::stop );
        supervisor.stop();
        services.clear();
        Metrics.resetAll();
        Application.unregister( this );
        log.debug( "application kernel stopped" );
    }

    public void reload() {
//@todo rething this
        log.debug( "reloading application kernel" + name + "..." );
        supervisor.reload();
        log.debug( "application kernel reloaded" );
    }

    public boolean profileEnabled( String profile ) {
        return profiles.contains( profile );
    }

    public void register( String name, Object service ) {
        Object registered;
        if( ( registered = services.putIfAbsent( name, service ) ) != null )
            throw new ApplicationException( "Service " + service + " is already registered [" + registered.getClass() + "]" );
    }

    public void unregister( String name ) {
        //@todo check for dependencies
        services.remove( name );
    }

    @SuppressWarnings( "unchecked" )
    public <T> T service( String name ) {
        return ( T ) services.get( name );
    }

    @SuppressWarnings( "unchecked" )
    public <T> List<T> ofClass( Class<T> clazz ) {
        return Stream.of( services.values() )
            .filter( clazz::isInstance )
            .map( x -> ( T ) x )
            .toList();
    }

    /**
     * @see #stop()
     */
    @Deprecated
    public void unregisterServices() {
        services.clear();
    }

    public void enableProfiles( String... profiles ) {
        this.profiles.addAll( Sets.of( profiles ) );
    }

    private boolean serviceEnabled( String name ) {
        for( Module module : this.modules ) {
            Module.Service service = module.services.get( name );
            if( service != null && !service.enabled ) return false;
        }

        return true;
    }

    @Override
    public Iterator<Map.Entry<String, Object>> iterator() {
        return services.entrySet().iterator();
    }
}
