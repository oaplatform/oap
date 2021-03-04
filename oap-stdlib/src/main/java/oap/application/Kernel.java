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
import oap.util.Lists;
import oap.util.Sets;
import oap.util.Strings;
import org.apache.commons.lang3.StringUtils;

import java.io.Closeable;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static oap.application.KernelHelper.fixLinksForConstructor;
import static oap.application.KernelHelper.isImplementations;

@Slf4j
@ToString( of = "name" )
public class Kernel implements Closeable {
    public static final String DEFAULT = Strings.DEFAULT;
    public final LinkedHashMap<String, LinkedHashMap<String, Object>> services = new LinkedHashMap<>();
    public final LinkedHashSet<String> profiles = new LinkedHashSet<>();
    final String name;
    private final List<URL> moduleConfigurations;
    private final LinkedHashSet<Module> modules = new LinkedHashSet<>();
    private final LinkedHashMap<String, Supervisor> supervisor = new LinkedHashMap<>();

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
            var methodName = "add" + StringUtils.capitalize( listener ) + "Listener";
            var ref = Module.Reference.of( reference );
            var linkedModule = services.get( ref.module );
            Object linked;
            if( linkedModule == null || ( ( linked = linkedModule.get( ref.service ) ) ) == null )
                throw new ApplicationException( "for " + service.name + " listening object " + reference + " is not found" );
            var m = Reflect.reflect( linked.getClass() )
                .method( methodName )
                .orElse( null );
            if( m != null ) m.invoke( linked, instance );
            else
                throw new ReflectException( "listener " + listener + " should have method " + methodName + " in " + reference );
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

    public void start( Map<String, Object> properties ) {
        start( ApplicationConfiguration.load( properties ) );
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

        if( config.boot.main.isEmpty() ) throw new ApplicationException( "boot.main must contain at least one module name" );

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

        var modules = new Modules( this.modules, this.profiles, config.boot.main );

        for( var moduleName : modules.map.keySet() ) {
            var supervisor = new Supervisor();
            this.supervisor.put( moduleName, supervisor );
        }

        var map = instantiateServices( modules );
        registerServices( map );
        linkServices( map );
        startServices( map );

        for( var supervisor : this.supervisor.values() ) {
            supervisor.preStart();
            supervisor.start();
        }

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

    private LinkedHashMap<String, LinkedHashMap<String, ServiceInitialization>> instantiateServices( Modules modules ) throws ApplicationException {
        var retModules = new LinkedHashMap<String, LinkedHashMap<String, ServiceInitialization>>();

        for( var moduleItem : modules.map.values() ) {
            if( !moduleItem.isEnabled() ) continue;
            var moduleName = moduleItem.getName();

            var ret = new LinkedHashMap<String, ServiceInitialization>();
            retModules.put( moduleName, ret );

            for( var serviceEntry : moduleItem.services.entrySet() ) {
                var serviceItem = serviceEntry.getValue();
                if( !serviceItem.enabled ) continue;

                var service = serviceItem.service;
                var implName = serviceEntry.getKey();
                log.trace( "instantiate {}:{}...", moduleName, implName + " as " + service.name );
                try {
                    var reflect = Reflect.reflect( service.implementation, Module.coersions );
                    Object instance;
                    if( !service.isRemoteService() ) {
                        var parametersWithoutLinks = fixLinksForConstructor( this, moduleName, retModules, service.parameters );
                        instance = reflect.newInstance( parametersWithoutLinks );
                        setServiceName( reflect, instance, service.name );
//                    updateLoggerIfExists( instance, implName );
                    } else {
                        instance = RemoteInvocationHandler.proxy( service.remote, reflect.underlying );
                    }
                    ret.put( service.name, new ServiceInitialization( implName, instance, moduleItem.module, service, reflect ) );
                } catch( ReflectException e ) {
                    log.info( "service name = {}:{}, remote = {}, profiles = {}",
                        moduleName,
                        implName, service.remote, service.profiles );
                    throw new ApplicationException( e );
                }

            }
        }

        return retModules;
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

    private void registerServices( Map<String, LinkedHashMap<String, ServiceInitialization>> moduleServices ) {
        moduleServices.forEach( ( moduleName, services ) -> {
            services.forEach( ( serviceName, si ) -> {
                register( moduleName, serviceName, si.instance );
                if( !si.service.name.equals( serviceName ) )
                    register( moduleName, si.service.name, si.instance );
            } );
        } );
    }

    private void linkServices( Map<String, LinkedHashMap<String, ServiceInitialization>> moduleServices ) {
        for( var services : moduleServices.values() ) {
            for( var si : services.values() ) {
                log.trace( "linking service {}...", si.implementationName );

                linkListeners( si.service, si.instance );
                si.service.parameters.forEach( ( parameter, value ) -> linkService( new FieldLinkReflection( si.reflection,
                    si.instance, parameter ), value, si, true ) );

            }
        }
    }

    @SuppressWarnings( "unchecked" )
    private void linkService( LinkReflection lRef, Object parameterValue, ServiceInitialization si,
                              boolean failIfNotFound ) throws ApplicationException {
        if( Module.Reference.isServiceLink( parameterValue ) ) {
            var link = Module.Reference.of( parameterValue );
            var linkService = service( link );

            if( failIfNotFound && linkService.isEmpty() )
                throw new ApplicationException( "for " + si.implementationName + " linked object " + link + " is not found" );
            linkService.ifPresent( lRef::set );
        } else if( isImplementations( parameterValue ) ) {
            var interfaceName = Module.Reference.of( parameterValue ).service;
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

    private void startServices( Map<String, LinkedHashMap<String, ServiceInitialization>> moduleServices ) {
        moduleServices.forEach( ( moduleName, services ) -> {
            var supervisor = this.supervisor.get( moduleName );
            services.forEach( ( implName, si ) -> {
                log.debug( "starting {} as {}...", si.service.name, implName );

                startService( supervisor, si );
                log.debug( "starting {} as {}... Done", si.service.name, implName );
            } );
        } );
    }

    private void startService( Supervisor supervisor, ServiceInitialization si ) {
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

    public void register( String moduleName, String serviceName, Object service ) throws ApplicationException {
        Object registered;

        var module = services.computeIfAbsent( moduleName, mn -> new LinkedHashMap<>() );

        if( ( registered = module.putIfAbsent( serviceName, service ) ) != null )
            throw new ApplicationException( moduleName + ":" + serviceName + " Service " + service + " is already registered [" + registered.getClass() + "]" );
    }

    public void stop() {
        for( var supervisor : Lists.reverse( this.supervisor.values() ) ) {
            log.debug( "stopping application kernel {}...", name );
            supervisor.preStop();
            supervisor.stop();
            services.clear();
            log.debug( "application kernel stopped" );
        }
    }

    @SuppressWarnings( "unchecked" )
    public <T> Optional<T> service( String moduleName, String serviceName ) {
        if( "*".equals( moduleName ) ) {
            for( var services : services.values() ) {
                var service = services.get( serviceName );
                if( service != null ) return Optional.of( ( T ) service );
            }

            return Optional.empty();
        }

        var module = services.get( moduleName );
        if( module == null ) return Optional.empty();

        return Optional.ofNullable( ( T ) module.get( serviceName ) );
    }

    private <T> Optional<T> service( Module.Reference reference ) {
        return service( reference.module, reference.service );
    }

    public <T> Optional<T> service( String reference ) {
        var ref = Module.Reference.of( reference );
        return service( ref );
    }

    public <T> List<T> ofClass( Class<T> clazz ) {
        return ofClass( "*", clazz );
    }

    @SuppressWarnings( "unchecked" )
    public <T> List<T> ofClass( String moduleName, Class<T> clazz ) {
        var ret = new ArrayList<T>();

        this.services.forEach( ( module, services ) -> {
            if( "*".equals( moduleName ) || module.equals( moduleName ) )
                for( var service : services.values() ) {
                    if( clazz.isInstance( service ) ) ret.add( ( T ) service );
                }

        } );

        return ret;
    }

    public <T> Optional<T> serviceOfClass( Class<T> clazz ) {
        return serviceOfClass( "*", clazz );
    }

    public <T> Optional<T> serviceOfClass( String moduleName, Class<T> clazz ) {
        return Lists.headOf( ofClass( moduleName, clazz ) );
    }

    public <T> T serviceOfClass2( Class<T> clazz ) {
        return serviceOfClass2( "*", clazz );
    }

    public <T> T serviceOfClass2( String moduleName, Class<T> clazz ) {
        return Lists.head2( ofClass( moduleName, clazz ) );
    }

    public void unregister( String moduleName, String serviceName ) {
        var module = services.get( moduleName );
        if( module == null ) return;

        module.remove( name );
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
