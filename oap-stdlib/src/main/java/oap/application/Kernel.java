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
import oap.application.ApplicationConfiguration.ApplicationConfigurationModule;
import oap.application.link.FieldLinkReflection;
import oap.application.link.LinkReflection;
import oap.application.link.ListLinkReflection;
import oap.application.link.MapLinkReflection;
import oap.application.module.Module;
import oap.application.module.Reference;
import oap.application.module.Service;
import oap.application.module.ServiceExt;
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
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static oap.application.KernelHelper.fixLinksForConstructor;
import static oap.util.function.Functions.exception;

@Slf4j
@ToString( of = "name" )
public class Kernel implements Closeable {
    public static final String DEFAULT = Strings.DEFAULT;

    static final ArrayList<AbstractKernelCommand<?>> commands = new ArrayList<>();

    static {
        commands.add( new LocationKernelCommand() );
        commands.add( new KernelKernelCommand() );
        commands.add( ServiceKernelCommand.INSTANCE );
    }

    public final ServiceInitializationTree services = new ServiceInitializationTree();
    public final LinkedHashSet<String> profiles = new LinkedHashSet<>();
    final String name;
    private final List<URL> moduleConfigurations;
    private final LinkedHashSet<ModuleWithLocation> modules = new LinkedHashSet<>();
    private final Supervisor supervisor = new Supervisor();

    public Kernel( String name, List<URL> moduleConfigurations ) {
        this.name = name;
        this.moduleConfigurations = moduleConfigurations;
    }

    public Kernel( List<URL> moduleConfigurations ) {
        this( DEFAULT, moduleConfigurations );
    }

    private void linkListeners( ModuleItem moduleItem, Service service, Object instance ) throws ApplicationException {
        service.listen.forEach( ( listener, reference ) -> {
            log.debug( "setting {} to listen to {} with listener {}", service.name, reference, listener );
            var methodName = "add" + StringUtils.capitalize( listener ) + "Listener";
            var ref = ServiceKernelCommand.INSTANCE.reference( reference, moduleItem );
            var linkedModule = services.getServices( ref.module );
            ServiceInitialization linked;
            if( linkedModule == null || ( ( linked = linkedModule.get( ref.service ) ) ) == null )
                throw new ApplicationException( "for " + service.name + " listening object " + reference + " is not found" );
            var m = Reflect.reflect( linked.instance.getClass() )
                .method( methodName )
                .orElse( null );
            if( m != null ) m.invoke( linked.instance, instance );
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
        log.debug( "initializing application kernel  " + name + "..." );
        log.debug( "application config {}", config );

        this.profiles.addAll( config.getProfiles() );

        if( config.boot.main.isEmpty() ) throw new ApplicationException( "boot.main must contain at least one module name" );

        for( var moduleConfiguration : moduleConfigurations ) {
            var module = Module.CONFIGURATION.fromFile( moduleConfiguration, config.services );
            if( StringUtils.isBlank( module.name ) ) {
                throw new ApplicationException( moduleConfiguration + ": module.name is blank" );
            }
            this.modules.add( new ModuleWithLocation( module, moduleConfiguration ) );
        }
        log.debug( "modules = {}", Sets.map( this.modules, m -> m.module.name ) );
        log.trace( "modules configs = {}", this.modules );

        checkForUnknownServices( config.services );

        var map = ModuleHelper.init( this.modules, this.profiles, config.boot.main, this );

        var servicesMap = instantiateServices( map );
        registerServices( servicesMap );
        linkServices( servicesMap );
        startServices( servicesMap );

        supervisor.preStart();
        supervisor.start();

        log.debug( "application kernel started " + name );
    }

    private void checkForUnknownServices( Map<String, ApplicationConfigurationModule> services ) throws ApplicationException {
        services.forEach( ( moduleName, conf ) -> {
            if( !Lists.contains( this.modules, m -> m.module.name.equals( moduleName ) ) && conf.isEnabled() )
                throw new ApplicationException( "unknown application configuration module: " + moduleName );
        } );

        for( var module : this.modules ) {
            var moduleServices = services.get( module.module.name );
            if( moduleServices != null ) {
                var applicationConfigurationServices = new HashSet<>( moduleServices.keySet() );

                for( var serviceName : module.module.services.keySet() ) {
                    applicationConfigurationServices.remove( serviceName );
                }

                if( !applicationConfigurationServices.isEmpty() ) {
                    throw new ApplicationException( "unknown application configuration services: " + module.module.name + "." + applicationConfigurationServices );
                }
            }
        }
    }

    private ServiceInitializationTree instantiateServices( ModuleItemTree map ) throws ApplicationException {
        var retModules = new ServiceInitializationTree();

        for( var serviceItem : map.services ) {
            if( !serviceItem.moduleItem.isEnabled() ) continue;
            if( !serviceItem.enabled ) continue;

            var moduleItem = serviceItem.moduleItem;
            var moduleName = moduleItem.getName();

            var service = serviceItem.service;
            var implName = serviceItem.serviceName;
            log.trace( "instantiate {}.{} as {}...", moduleName, implName, service.name );
            try {
                var reflect = Reflect.reflect( service.implementation, Module.coersions );
                Object instance;
                if( !service.isRemoteService() ) {
                    var parametersWithoutLinks = fixLinksForConstructor( this, moduleItem, retModules, service.parameters );
                    instance = reflect.newInstance( parametersWithoutLinks );
                    setServiceName( reflect, instance, service.name );
//                    updateLoggerIfExists( instance, implName );
                } else {
                    instance = RemoteInvocationHandler.proxy( service.remote, reflect.underlying );
                }
                retModules.put( serviceItem, new ServiceInitialization( implName, instance, moduleItem, service, reflect ) );
            } catch( ReflectException e ) {
                log.info( "service name = {}.{}, remote = {}, profiles = {}",
                    moduleName, implName, service.remote, service.profiles );
                throw new ApplicationException( e );
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

    private void registerServices( ServiceInitializationTree moduleServices ) {
        moduleServices.forEach( ( serviceItem, si ) -> {
            register( serviceItem, serviceItem.serviceName, si );
            if( !si.service.name.equals( serviceItem.serviceName ) )
                register( serviceItem, si.service.name, si );
        } );
    }

    private void linkServices( ServiceInitializationTree moduleServices ) {
        for( var si : moduleServices.values() ) {
            log.trace( "linking service {}...", si.implementationName );

            linkListeners( si.module, si.service, si.instance );
            linkLinks( si );
            si.service.parameters.forEach( ( parameter, value ) ->
                linkService( new FieldLinkReflection( si.reflection, si.instance, parameter ), value, si, true )
            );
        }
    }

    @SuppressWarnings( "unchecked" )
    private void linkLinks( ServiceInitialization initialization ) {
        initialization.service.link.forEach( ( fieldName, serviceRef ) ->
            ServiceKernelCommand.INSTANCE.get( serviceRef, this, initialization.module, services )
                .ifSuccessOrElse(
                    service -> {
                        var reflect = Reflect.reflect( service.service.implementation );
                        var methodSuffix = StringUtils.capitalize( fieldName );

                        var linkMethod = reflect.method( "add" + methodSuffix ).orElse( null );
                        if( linkMethod == null ) linkMethod = reflect.method( "set" + methodSuffix ).orElse( null );
                        if( linkMethod == null ) linkMethod = reflect.method( "add" + methodSuffix + "Listener" ).orElse( null );

                        if( linkMethod != null && linkMethod.parameters.size() == 1 )
                            linkMethod.invoke( service.instance, initialization.instance );
                        else {
                            var linkField = reflect.field( fieldName ).orElse( null );
                            if( linkField != null )
                                if( linkField.type().assignableTo( Collection.class ) )
                                    ( ( Collection<Object> ) linkField.get( service.instance ) )
                                        .add( initialization.instance );
                                else linkField.set( service.instance, initialization.instance );
                            else exception( new ReflectException( "link to " + service.implementationName + "/" + service.service.implementation
                                + " should have field " + fieldName
                                + " for " + initialization.implementationName + "/" + initialization.service.implementation ) );
                        }
                    },
                    exception( e -> new ApplicationException( "Unknown service link " + serviceRef ) ) ) );
    }

    @SuppressWarnings( "unchecked" )
    private void linkService( LinkReflection lRef, Object parameterValue, ServiceInitialization si,
                              boolean failIfNotFound ) throws ApplicationException {
        if( parameterValue instanceof List<?> ) {
            var parameterList = ( List<?> ) parameterValue;
            var instance = lRef.get();
            if( instance instanceof List<?> ) {
                var instanceList = ( List<Object> ) instance;
                var instanceIterator = instanceList.listIterator();
                for( var parameter : parameterList )
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
        } else if( ServiceKernelCommand.INSTANCE.matches( parameterValue ) ) {
            var linkResult = ServiceKernelCommand.INSTANCE.getInstance( parameterValue, this, si.module, services );

            if( failIfNotFound && !linkResult.isSuccess() )
                throw new ApplicationException( "for " + si.implementationName + " linked object " + parameterValue + " is not found" );
            linkResult.ifSuccess( lRef::set );
        }
    }

    private void startServices( ServiceInitializationTree moduleServices ) {
        moduleServices.forEach( ( serviceItem, si ) -> {
            log.debug( "starting {} as {}...", si.service.name, serviceItem.serviceName );

            startService( supervisor, si );
            log.debug( "starting {} as {}... Done", si.service.name, serviceItem.serviceName );
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

    public void register( ModuleItem.ServiceItem serviceItem, String serviceName, ServiceInitialization si ) throws ApplicationException {
        ServiceInitialization registered;

        if( ( registered = services.putIfAbsent( serviceItem, serviceName, si ) ) != null )
            throw new ApplicationException( serviceItem.getModuleName() + ":" + serviceName + " Service " + si.implementationName + " is already registered [" + registered.instance.getClass() + "]" );
    }

    public void stop() {
        log.debug( "stopping application kernel {}...", name );
        supervisor.preStop();
        supervisor.stop();
        services.clear();
        log.debug( "application kernel stopped {}", name );
    }

    @SuppressWarnings( "unchecked" )
    public <T> Optional<T> service( String moduleName, String serviceName ) {
        if( ServiceStorage.ALL_MODULES.contains( moduleName ) ) {
            return services.findFirstServiceByName( serviceName ).map( si -> ( T ) si.instance );
        }

        var si = services.get( moduleName, serviceName );
        if( si == null ) return Optional.empty();

        return Optional.of( ( T ) si.instance );
    }

    private <T> Optional<T> service( Reference reference ) {
        return service( reference.module, reference.service );
    }

    public <T> Optional<T> service( String reference ) {
        var ref = ServiceKernelCommand.INSTANCE.reference( reference.startsWith( "modules." ) ? reference : "modules." + reference, null );
        return service( ref );
    }

    @SuppressWarnings( "unchecked" )
    public <T> List<T> services( String moduleName, String serviceName ) {
        if( ServiceStorage.ALL_MODULES.contains( moduleName ) ) {
            return Lists.map( services.findAllServicesByName( serviceName ), si -> ( T ) si.instance );
        }

        var si = services.get( moduleName, serviceName );
        if( si == null ) return List.of();

        return List.of( ( T ) si.instance );
    }

    public <T> List<T> ofClass( Class<T> clazz ) {
        return ofClass( "*", clazz );
    }

    @SuppressWarnings( "unchecked" )
    public <T> List<T> ofClass( String moduleName, Class<T> clazz ) {
        var ret = new ArrayList<T>();

        this.services.forEach( ( serviceItem, service ) -> {
            if( ServiceStorage.ALL_MODULES.contains( moduleName ) || serviceItem.getModuleName().equals( moduleName ) )
                if( clazz.isInstance( service.instance ) ) ret.add( ( T ) service.instance );
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

    public <T> List<ServiceExt<T>> servicesByExt( String ext, Class<T> clazz ) {
        var ret = new ArrayList<ServiceExt<T>>();

        for( var si : services.values() ) {
            var extConfiguration = si.service.<T>getExt( ext );
            if( extConfiguration == null ) continue;

            ret.add( new ServiceExt<T>( si.implementationName, si.module.module, si, extConfiguration ) );


        }

        return ret;
    }

    public void unregister( String moduleName, String serviceName ) {
        services.removeService( moduleName, serviceName );
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

    @ToString
    public static class ModuleWithLocation {
        public final Module module;
        public final URL location;

        public ModuleWithLocation( Module module, URL location ) {
            this.module = module;
            this.location = location;
        }
    }

}
