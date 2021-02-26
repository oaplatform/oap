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
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import oap.application.link.FieldLinkReflection;
import oap.application.link.LinkReflection;
import oap.application.link.ListLinkReflection;
import oap.application.link.MapLinkReflection;
import oap.application.remote.RemoteInvocationHandler;
import oap.application.supervision.Supervisor;
import oap.json.Binder;
import oap.reflect.Reflect;
import oap.reflect.ReflectException;
import oap.reflect.Reflection;
import oap.util.PrioritySet;
import oap.util.Sets;
import oap.util.Stream;
import oap.util.Strings;
import org.apache.commons.lang3.StringUtils;

import java.io.Closeable;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static oap.application.KernelHelper.fixLinksForConstructor;
import static oap.application.KernelHelper.isImplementations;
import static oap.application.KernelHelper.isServiceLink;
import static oap.application.KernelHelper.referenceName;

@Slf4j
@ToString( of = "name" )
public class Kernel implements Closeable {
    public static final String DEFAULT = Strings.DEFAULT;
    public final ConcurrentMap<String, Object> services = new ConcurrentHashMap<>();
    public final LinkedHashSet<String> profiles = new LinkedHashSet<>();
    final String name;
    private final List<URL> moduleConfigurations;
    private final LinkedHashSet<Module> modules = new LinkedHashSet<>();
    private final Supervisor supervisor = new Supervisor();

    public Kernel( String name, List<URL> moduleConfigurations ) {
        this.name = name;
        this.moduleConfigurations = moduleConfigurations;
    }

    public Kernel( List<URL> moduleConfigurations ) {
        this( DEFAULT, moduleConfigurations );
    }

    private void linkListeners( Module.Service service, Object instance ) throws ApplicationException {
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
    private void linkLinks( Module.Service service, Object instance ) throws ApplicationException {
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

    public void start() throws ApplicationException {
        start( ApplicationConfiguration.load() );
    }

    public void start( String appConfigPath, String confd ) throws ApplicationException {
        try {
            var configURL =
                appConfigPath.startsWith( "classpath:" )
                    ? Thread.currentThread().getContextClassLoader().getResource( appConfigPath.substring( 10 ) )
                    : new File( appConfigPath ).toURI().toURL();

            Preconditions.checkNotNull( configURL, appConfigPath + " not found" );

            var confdPath = confd != null ? Paths.get( confd )
                : new File( configURL.toURI() ).toPath().getParent().resolve( "conf.d" );

            start( ApplicationConfiguration.load( configURL, confdPath.toString() ) );
        } catch( MalformedURLException | URISyntaxException e ) {
            throw new ApplicationException( e );
        }
    }

    public void start( Path appConfigPath, Path confd ) throws ApplicationException {
        start( ApplicationConfiguration.load( appConfigPath, confd ) );
    }

    public void start( Path appConfigPath ) throws ApplicationException {
        start( appConfigPath, Map.of() );
    }

    public void start( Path appConfigPath, Map<Object, Object> properties ) throws ApplicationException {
        Map<Object, Object> map = new LinkedHashMap<>();
        map.putAll( System.getProperties() );
        map.putAll( properties );

        start( ApplicationConfiguration.loadWithProperties( appConfigPath, List.of( Binder.json.marshal( map ) ) ) );
    }

    public void start( ApplicationConfiguration config ) throws ApplicationException {
        log.debug( "initializing application kernel..." );
        log.debug( "application config {}", config );
        this.profiles.addAll( config.profiles );

        for( var moduleConfiguration : moduleConfigurations ) {
            var module = Module.CONFIGURATION.fromFile( moduleConfiguration, config.services );
            if( StringUtils.isBlank( module.name ) ) {
                throw new ApplicationException( moduleConfiguration + ": module.name is blank" );
            }
            this.modules.add( module );
        }
        log.debug( "modules = {}", Sets.map( this.modules, m -> m.name ) );
        log.trace( "modules configs = {}", this.modules );

        checkForUnknownServices( config.services );

        var modules = new Modules( this.modules, this.profiles );
        var map = instantiateServices( modules );
        registerServices( map );
        linkServices( map );
        startServices( map );

        this.supervisor.preStart();
        this.supervisor.start();

        this.modules.add( new Module( Module.DEFAULT ) );
        log.debug( "application kernel started" );
    }

    private void checkForUnknownServices( Map<String, Map<String, Object>> services ) throws ApplicationException {
        var applicationConfigurationServices = new HashSet<>( services.keySet() );

        for( var module : this.modules ) {
            for( var serviceName : module.services.keySet() ) {
                applicationConfigurationServices.remove( serviceName );
            }
        }

        if( !applicationConfigurationServices.isEmpty() ) {
            throw new ApplicationException( "unknown application configuration services: " + applicationConfigurationServices );
        }
    }

    private LinkedHashMap<String, ServiceInitialization> instantiateServices( Modules modules ) throws ApplicationException {
        var ret = new LinkedHashMap<String, ServiceInitialization>();

        for( var moduleItem : modules.map.values() ) {
            if( !moduleItem.enabled ) continue;

            for( var serviceEntry : moduleItem.services.entrySet() ) {
                var serviceItem = serviceEntry.getValue();
                if( !serviceItem.enabled ) continue;

                var service = serviceItem.service;
                var implName = serviceEntry.getKey();
                try {
                    var reflect = Reflect.reflect( service.implementation, Module.coersions );
                    Object instance;
                    if( !service.isRemoteService() ) {
                        var parametersWithoutLinks = fixLinksForConstructor( this, ret, service.parameters );
                        instance = reflect.newInstance( parametersWithoutLinks );
                        setServiceName( reflect, instance, service.name );
//                    updateLoggerIfExists( instance, implName );
                    } else {
                        instance = RemoteInvocationHandler.proxy( service.remote, reflect.underlying );
                    }
                    ret.put( service.name, new ServiceInitialization( implName, instance, moduleItem.module, service, reflect ) );
                } catch( ReflectException e ) {
                    log.info( "service name = {}:{}, remote = {}, profiles = {}",
                        moduleItem.getName(),
                        implName, service.remote, service.profiles );
                    throw new ApplicationException( e );
                }

            }
        }

        return ret;
    }

    private void setServiceName( Reflection reflect, Object instance, String serviceName ) throws ApplicationException {
        var fields = reflect.annotatedFields( ServiceName.class );
        for( var field : fields ) {
            if( !String.class.equals( field.underlying.getType() ) )
                throw new ApplicationException( "The " + serviceName + "#" + field.name() + " field must be of type 'String'." );

            field.set( instance, serviceName );
        }

        var methods = reflect.annotatedMethods( ServiceName.class );
        for( var method : methods ) {
            if( method.parameters.size() != 1 && !String.class.equals( method.parameters.get( 0 ).underlying.getType() ) ) {
                throw new ApplicationException( "The " + serviceName + "#" + method.name() + " method must be only one parameter of type 'String'." );
            }

            method.invoke( instance, serviceName );
        }
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
                              boolean failIfNotFound ) throws ApplicationException {
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
            var instance = lRef.get();
            if( instance instanceof List<?> ) {
                var instanceList = ( List<Object> ) instance;
                var instanceIterator = instanceList.listIterator();
                for( var parameter : parameterList ) {
                    linkService( new ListLinkReflection( instanceIterator ), parameter, si, false );
                }
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
        services.forEach( ( implName, si ) -> {
            log.debug( "starting {} as {}...", si.service.name, implName );

            startService( si );
            log.debug( "starting {} as {}... Done", si.service.name, implName );
        } );
    }

    private void startService( ServiceInitialization si ) {
        var service = si.service;
        var instance = si.instance;
        if( service.supervision.supervise ) {
            supervisor.startSupervised( service.name, instance,
                service.supervision.preStartWith,
                service.supervision.startWith,
                service.supervision.preStopWith,
                service.supervision.stopWith
            );
        }

        if( service.supervision.thread ) {
//            if( service.supervision.delay != 0 )
//                supervisor.startScheduledThread( service.name, instance, service.supervision.delay, MILLISECONDS );
//            else
            supervisor.startThread( service.name, instance );
        } else {
            if( service.supervision.schedule && service.supervision.cron != null )
                supervisor.scheduleCron( service.name, ( Runnable ) instance,
                    service.supervision.cron );
            else if( service.supervision.schedule && service.supervision.delay != 0 )
                supervisor.scheduleWithFixedDelay( service.name, ( Runnable ) instance,
                    service.supervision.delay, MILLISECONDS );
        }
    }

    public void register( String name, Object service ) throws ApplicationException {
        Object registered;
        if( ( registered = services.putIfAbsent( name, service ) ) != null )
            throw new ApplicationException( "Service " + service + " is already registered [" + registered.getClass() + "]" );
    }

    public void stop() {
        log.debug( "stopping application kernel {}...", name );
        supervisor.preStop();
        supervisor.stop();
        services.clear();
        log.debug( "application kernel stopped" );
    }

    @SuppressWarnings( "unchecked" )
    public <T> Optional<T> service( String name ) {
        return Optional.ofNullable( ( T ) services.get( name ) );
    }

    /**
     * @see #service(String)
     * @see oap.application.testng.KernelFixture#service(String)
     */
    @Deprecated( forRemoval = true )
    public <T> T serviceOrThrow( String name ) {
        return this.<T>service( name ).orElseThrow( () -> new ApplicationException( "service " + name + " is not found" ) );
    }

    @SuppressWarnings( "unchecked" )
    public <T> List<T> ofClass( Class<T> clazz ) {
        return Stream.of( services.values() )
            .filter( clazz::isInstance )
            .map( x -> ( T ) x )
            .toList();
    }

    public <T> Optional<T> serviceOfClass( Class<T> clazz ) {
        return this.ofClass( clazz ).stream().findAny();
    }

    /**
     * @see oap.application.testng.KernelFixture#service(Class)
     * @see #serviceOfClass(Class)
     */
    @Deprecated( forRemoval = true )
    public <T> T serviceOfClass2( Class<T> clazz ) {
        return this.ofClass( clazz ).stream().findAny().orElseThrow();
    }

    public void unregister( String name ) {
        services.remove( name );
    }

    public void unregisterServices() {
        services.clear();
    }

    public void enableProfile( String profile ) {
        this.profiles.add( profile );
    }

    @Override
    public void close() {
        stop();
    }
}
