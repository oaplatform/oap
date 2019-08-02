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

import com.google.common.base.Preconditions;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import oap.application.link.FieldLinkReflection;
import oap.application.link.LinkReflection;
import oap.application.link.ListLinkReflection;
import oap.application.link.MapLinkReflection;
import oap.application.remote.RemoteInvocationHandler;
import oap.application.supervision.Supervisor;
import oap.json.Binder;
import oap.metrics.Metrics;
import oap.reflect.Reflect;
import oap.reflect.ReflectException;
import oap.reflect.Reflection;
import oap.util.Lists;
import oap.util.PrioritySet;
import oap.util.Sets;
import oap.util.Stream;
import oap.util.Strings;
import org.apache.commons.lang3.StringUtils;

import java.io.Closeable;
import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.util.Collections.emptyMap;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static oap.application.KernelHelper.fixLinksForConstructor;
import static oap.application.KernelHelper.forEachModule;
import static oap.application.KernelHelper.forEachService;
import static oap.application.KernelHelper.isImplementations;
import static oap.application.KernelHelper.isModuleEnabled;
import static oap.application.KernelHelper.isServiceEnabled;
import static oap.application.KernelHelper.isServiceLink;
import static oap.application.KernelHelper.referenceName;

@Slf4j
@ToString( of = "name" )
public class Kernel implements Closeable {
    public static final String DEFAULT = Strings.DEFAULT;
    public final ConcurrentMap<String, Object> services = new ConcurrentHashMap<>();
    public final LinkedHashSet<String> profiles = new LinkedHashSet<>();
    final String name;
    private final List<URL> configurations;
    private final LinkedHashSet<Module> modules = new LinkedHashSet<>();
    private final Supervisor supervisor = new Supervisor();

    public Kernel( String name, List<URL> configurations ) {
        this.name = name;
        this.configurations = configurations;
    }

    public Kernel( List<URL> configurations ) {
        this( DEFAULT, configurations );
    }

    private static void fixDepsParameter( Module module, Module.Service service,
                                          Object value, boolean optional ) {
        if( isServiceLink( value ) ) {
            if( !optional ) {
                String linkName = Module.Reference.of( value ).name;
                addDeps( module, service, Lists.of( linkName ) );
            }
        } else if( value instanceof List<?> )
            for( var item : ( List<?> ) value )
                fixDepsParameter( module, service, item, true );
        else if( value instanceof Map<?, ?> )
            for( var item : ( ( Map<?, ?> ) value ).values() )
                fixDepsParameter( module, service, item, false );
    }

    private static void addDeps( Module module, Module.Service service, List<String> list ) {
        log.trace( "service[{}].dependsOn.addAll({}); module={}", service.name, list, module.name );
        service.dependsOn.addAll( list );
    }

    private void fixDeps() {
        for( var module : modules ) {
            log.trace( "module {} services {}", module.name, module.services.keySet() );
            for( var serviceEntry : module.services.entrySet() ) {
                var service = serviceEntry.getValue();
                if( !isServiceEnabled( service, this.profiles ) ) continue;

                log.trace( "fix deps for {} in {}", serviceEntry.getKey(), module.name );
                service.parameters.forEach( ( key, value ) ->
                    fixDepsParameter( module, service, value, false )
                );
            }
        }
    }

    private void linkListeners( Module.Service service, Object instance ) {
        service.listen.forEach( ( listener, reference ) -> {
            log.debug( "setting {} to listen to {} with listener {}", service.name, reference, listener );
            String methodName = "add" + StringUtils.capitalize( listener ) + "Listener";
            Object linked = services.get( Module.Reference.of( reference ).name );
            if( linked == null )
                throw new ApplicationException( "for " + service.name + " listening object " + reference + " is not found" );
            var m = Reflect.reflect( linked.getClass() )
                .method( methodName )
                .orElse( null );
            if( m != null ) m.invoke( linked, instance );
            else
                throw new ReflectException( "listener " + listener + " should have method " + methodName + " in " + reference );
        } );
    }

    @SuppressWarnings( "unchecked" )
    private void linkLinks( Module.Service service, Object instance ) {
        service.link.forEach( ( field, reference ) -> {
            log.debug( "linking {} to {} into {}", service.name, reference, field );
            Module.Reference ref = Module.Reference.of( reference );
            Object linked = services.get( ref.name );
            if( linked == null )
                throw new ApplicationException( "for " + service.name + " linked object " + ref + " is not found" );
            var f = Reflect.reflect( linked.getClass() ).field( field ).orElse( null );
            if( f != null ) {
                Object value = f.get( linked );
                if( value instanceof PrioritySet<?> ) {
                    log.debug( "adding {} with priority {} to {} of {}", instance, ref.priority, field, ref.name );
                    ( ( PrioritySet<Object> ) value ).add( ref.priority, instance );
                } else if( value instanceof Collection<?> ) ( ( Collection<Object> ) value ).add( instance );
                else
                    throw new ApplicationException( "do not know how to link " + service.name + " to " + f.type().name() + " of " + ref.name );
            } else throw new ReflectException( "service " + ref.name + " should have field " + field );
        } );
    }

    public void start() {
        start( ApplicationConfiguration.load() );
    }

    @SneakyThrows
    public void start( String appConfigPath, String confd ) {
        var configURL =
            appConfigPath.startsWith( "classpath:" )
                ? Thread.currentThread().getContextClassLoader().getResource( appConfigPath.substring( 10 ) )
                : new File( appConfigPath ).toURI().toURL();

        Preconditions.checkNotNull( configURL, appConfigPath + " not found" );

        var confdPath = confd != null ? Paths.get( confd )
            : new File( configURL.toURI() ).toPath().getParent().resolve( "conf.d" );

        start( ApplicationConfiguration.load( configURL, confdPath.toString() ) );
    }

    public void start( Path appConfigPath, Path confd ) {
        start( ApplicationConfiguration.load( appConfigPath, confd ) );
    }

    public void start( Path appConfigPath ) {
        start( appConfigPath, emptyMap() );
    }

    public void start( Path appConfigPath, Map<Object, Object> properties ) {
        Map<Object, Object> map = new LinkedHashMap<>();
        map.putAll( System.getProperties() );
        map.putAll( properties );

        start( ApplicationConfiguration.load( appConfigPath, List.of( Binder.json.marshal( map ) ) ) );
    }

    void start( ApplicationConfiguration config ) {
        log.debug( "initializing application kernel..." );
        Application.register( this );
        log.debug( "application config {}", config );
        this.profiles.addAll( config.profiles );

        this.modules.addAll( Stream.of( configurations )
            .map( module -> Module.CONFIGURATION.fromFile( module, config.services ) )
            .toList() );
        log.debug( "modules = " + Sets.map( this.modules, m -> m.name ) );
        log.trace( "modules configs = " + this.modules );

        fixServiceName();
        fixDeps();
        var map = instantiateServices();
        registerServices( map );
        linkServices( map );
        startServices( map );

        this.supervisor.start();

        this.modules.add( new Module( Module.DEFAULT ) );
        log.debug( "application kernel started" );
    }

    private void fixServiceName() {
        for( var module : modules )
            module.services.forEach( ( implName, service ) ->
                service.name = service.name != null ? service.name : implName );
    }

    private Map<String, ServiceInitialization> instantiateServices() {
        var ret = new LinkedHashMap<String, ServiceInitialization>();

        var initializedServices = new LinkedHashSet<String>();
        forEachModule( modules, profiles, new LinkedHashSet<>(), module -> {
            if( !isModuleEnabled( module, this.profiles ) ) {
                log.debug( "skipping module {} with profiles {}", module.name, module.profiles );
                return;
            }

            forEachService( modules, module.services, initializedServices, ( implName, service ) -> {
                if( !service.enabled ) {
                    log.debug( "service {} is disabled.", implName );
                    return;
                }
                if( !isServiceEnabled( service, this.profiles ) ) {
                    log.debug( "skipping service {} with profiles {}", implName, service.profiles );
                    return;
                }

                log.debug( "initializing {} as {}", implName, service.name );

                if( service.implementation == null )
                    throw new ApplicationException( "failed to initialize service: " + service.name + ". implementation == null" );

                Reflection reflect = Reflect.reflect( service.implementation, Module.coersions );
                Object instance;
                if( !service.isRemoteService() ) try {
                    var parametersWithoutLinks = fixLinksForConstructor( this, ret, service.parameters );
                    instance = reflect.newInstance( parametersWithoutLinks );
                } catch( ReflectException e ) {
                    log.info( "service name = {}, remote = {}, profiles = {}", implName, service.remote, service.profiles );
                    throw e;
                }
                else instance = RemoteInvocationHandler.proxy( service.remote, reflect.underlying );

                ret.put( service.name, new ServiceInitialization( implName, instance, module, service, reflect ) );

                initializedServices.add( service.name );
            } );
        } );

        return ret;
    }

    private void registerServices( Map<String, ServiceInitialization> services ) {
        services.forEach( ( name, si ) -> {
            register( name, si.instance );
            if( !si.service.name.equals( name ) )
                register( si.service.name, si.instance );
        } );
    }

    private void linkServices( Map<String, ServiceInitialization> services ) {
        for( var si : services.values() ) {
            log.trace( "linking service {}...", si.implementationName );
            linkLinks( si.service, si.instance );
            linkListeners( si.service, si.instance );
            si.service.parameters.forEach( ( parameter, value ) -> linkService( new FieldLinkReflection( si.reflection,
                si.instance, parameter ), value, si, true ) );

        }
    }

    @SuppressWarnings( "unchecked" )
    private void linkService( LinkReflection lRef, Object parameterValue, ServiceInitialization si,
                              boolean failIfNotFound ) {
        if( isServiceLink( parameterValue ) ) {
            var linkName = referenceName( parameterValue );
            var linkService = service( linkName );

            if( failIfNotFound && linkService.isEmpty() )
                throw new ApplicationException( "for " + si.implementationName + " linked object " + linkName + " is not found" );
            linkService.ifPresent( lRef::set );
        } else if( isImplementations( parameterValue ) ) {
            var interfaceName = referenceName( parameterValue );
            try {
                var linkServices = ofClass( Class.forName( interfaceName ) );
                if( linkServices.isEmpty() ) {
                    if( failIfNotFound )
                        throw new ApplicationException( "for " + si.implementationName + " service link " + interfaceName + " is not found" );
                    lRef.set( null );
                } else {
                    for( var linkService : linkServices ) {
                        lRef.set( linkService );
                    }
                }
            } catch( ClassNotFoundException e ) {
                throw new ApplicationException( "interface " + interfaceName + " not found" );
            }

        } else if( parameterValue instanceof List<?> ) {
            var parameterList = ( List<?> ) parameterValue;
            var instanceList = ( List<Object> ) lRef.get();
            var instanceIterator = instanceList.listIterator();
            for( var parameter : parameterList ) {
                linkService( new ListLinkReflection( instanceIterator ), parameter, si, false );
            }
        } else if( parameterValue instanceof Map ) {
            var parameterMap = ( Map<Object, Object> ) parameterValue;
            var instance = lRef.get();

            if( instance == null ) return;

            for( var entry : parameterMap.entrySet() ) {
                var isMap = instance instanceof Map;
                final LinkReflection linkReflection = isMap
                    ? new MapLinkReflection( ( Map<Object, Object> ) instance, entry.getKey() )
                    : new FieldLinkReflection( Reflect.reflect( instance.getClass() ), instance, entry.getKey().toString() );

                linkService( linkReflection, entry.getValue(), si, !isMap );
            }
        }
    }

    private void startServices( Map<String, ServiceInitialization> services ) {
        Set<Module> def = startModules( this.modules, services, new LinkedHashSet<>(), new LinkedHashSet<>() );
        if( !def.isEmpty() ) {
            Set<String> names = Sets.map( def, m -> m.name );
            log.error( "failed to initialize: {} ", names );
            throw new ApplicationException( "failed to initialize modules: " + names );
        }
    }

    private Set<Module> startModules( Set<Module> modules, Map<String, ServiceInitialization> services,
                                      Set<String> initializedModules, Set<String> startedServices ) {

        return forEachModule( modules, true, profiles, initializedModules, module -> {
            Map<String, Module.Service> def = startModule( module.services, services, startedServices );
            if( !def.isEmpty() ) {
                var names = def.keySet();
                log.error( "failed to initialize: {}", names );
                throw new ApplicationException( "failed to initialize services: " + names );
            }
        } );
    }

    private Map<String, Module.Service> startModule( Map<String, Module.Service> services, Map<String, ServiceInitialization> sis,
                                                     Set<String> startedServices ) {

        return forEachService( modules, services, startedServices, ( implName, service ) -> {
            log.debug( "starting {} as {}", implName, service.name );

            var si = sis.get( service.name );
            if( si != null ) {
                startService( si );
            }
            startedServices.add( service.name );
        } );
    }

    private void startService( ServiceInitialization si ) {
        var service = si.service;
        var instance = si.instance;
        if( service.supervision.supervise ) {
            supervisor.startSupervised( service.name, instance,
                service.supervision.startWith,
                service.supervision.stopWith );
        }

        if( service.supervision.thread )
            supervisor.startThread( service.name, instance );
        else {
            if( service.supervision.schedule && service.supervision.cron != null )
                supervisor.scheduleCron( service.name, ( Runnable ) instance,
                    service.supervision.cron );
            else if( service.supervision.schedule && service.supervision.delay != 0 )
                supervisor.scheduleWithFixedDelay( service.name, ( Runnable ) instance,
                    service.supervision.delay, MILLISECONDS );
        }
    }

    public void register( String name, Object service ) {
        Object registered;
        if( ( registered = services.putIfAbsent( name, service ) ) != null )
            throw new ApplicationException( "Service " + service + " is already registered [" + registered.getClass() + "]" );
    }

    public void stop() {
        log.debug( "stopping application kernel {}...", name );
        supervisor.stop();
        services.clear();
        Metrics.resetAll();
        Application.unregister( this );
        log.debug( "application kernel stopped" );
    }

    @SuppressWarnings( "unchecked" )
    public <T> Optional<T> service( String name ) {
        return Optional.ofNullable( ( T ) services.get( name ) );
    }

    @SuppressWarnings( "unchecked" )
    public <T> T serviceOrThrow( String name ) {
        T service = ( T ) services.get( name );
        if( service == null ) throw new ApplicationException( "service " + name + " is not found" );
        return service;
    }

    @SuppressWarnings( "unchecked" )
    public <T> List<T> ofClass( Class<T> clazz ) {
        return Stream.of( services.values() )
            .filter( clazz::isInstance )
            .map( x -> ( T ) x )
            .toList();
    }

//    protected Object resolve( String serviceName, String field, String reference, boolean required ) {
//        var linkName = Module.Reference.of( reference ).name;
//        var linkedService = service( linkName );
//        log.debug( "for {} linking {} -> {} with {}", serviceName, field, reference, linkedService );
//        if( linkedService.isEmpty() && required && serviceEnabled( modules, linkName ) )
//            throw new ApplicationException( "for " + serviceName + " service link " + reference + " is not found" );
//        return linkedService.get();
//    }

    public void unregister( String name ) {
        services.remove( name );
    }

    public void unregisterServices() {
        services.clear();
    }

    public void enableProfiles( String profile ) {
        this.profiles.add( profile );
    }

    @Override
    public void close() {
        stop();
    }
}
