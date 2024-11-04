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
import oap.application.supervision.Supervisor;
import oap.json.Binder;
import oap.reflect.Reflect;
import oap.reflect.ReflectException;
import oap.reflect.Reflection;
import oap.util.Lists;
import oap.util.Result;
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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static oap.application.KernelHelper.fixLinksForConstructor;
import static oap.util.function.Functions.exception;

@Slf4j
@ToString( of = "name" )
/**
 * APPLICATION_STOP_DETECT_TIMEOUT - default 5s
 * APPLICATION_FORCE_ASYNC_AFTER_TIMEOUT - default false
 */
public class Kernel implements Closeable, AutoCloseable {
    public static final String DEFAULT = Strings.DEFAULT;

    static final ArrayList<AbstractKernelCommand<?>> commands = new ArrayList<>();

    static {
        commands.add( new LocationKernelCommand() );
        commands.add( new ServiceNameKernelCommand() );
        commands.add( new KernelKernelCommand() );
        commands.add( ServiceKernelCommand.INSTANCE );
    }

    public final ServiceTree services = new ServiceTree();
    final String name;
    private final List<URL> moduleConfigurations;
    private final Supervisor supervisor = new Supervisor();
    private ApplicationConfiguration applicationConfiguration = new ApplicationConfiguration();

    public Kernel( String name, List<URL> moduleConfigurations ) {
        this.name = name;
        this.moduleConfigurations = moduleConfigurations;
    }

    public Kernel( List<URL> moduleConfigurations ) {
        this( DEFAULT, moduleConfigurations );
    }

    @SuppressWarnings( "unchecked" )
    private static void configureModules( LinkedHashMap<String, ModuleWithLocation> modules,
                                          ApplicationConfiguration config,
                                          Map<Reference, Reference> implementations ) {
        for( ModuleWithLocation moduleLocation : modules.values() ) {
            Module module = moduleLocation.module;
            ApplicationConfigurationModule moduleConfig = config.services.get( module.name );
            if( moduleConfig == null ) {
                continue;
            }
            for( var entry : module.services.entrySet() ) {
                String serviceName = entry.getKey();
                Service service = entry.getValue();

                Object serviceConf = moduleConfig.get( serviceName );
                switch( serviceConf ) {
                    case null -> {
                    }
                    case Map<?, ?> map -> {
                        for( Object mapKey : map.keySet() ) {
                            Preconditions.checkArgument( mapKey instanceof String );
                        }

                        Binder.update( service, ( Map<String, Object> ) map );
                    }
                    case String ref -> {
                        if( !service.isAbstract() ) {
                            throw new IllegalArgumentException( "Service " + service + " is not abstract" );
                        }
                        implementations.put( new Reference( module.name, serviceName ), ServiceKernelCommand.INSTANCE.reference( ref, null ) );
                    }
                    default -> throw new ApplicationException( "Service " + module.name + "." + serviceName + " configuration must be of type Map<String,?> or it must be a reference to the implementation of an abstract service in the form of <modules.[module name].[service name]>" );
                }
            }
        }
    }

    private void linkListeners( ModuleItem.ServiceItem serviceItem, Object instance ) throws ApplicationException {
        serviceItem.service.listen.forEach( ( listener, reference ) -> {
            log.debug( "setting {} to listen to {} with listener {}", serviceItem.serviceName, reference, listener );

            String methodName = "add" + StringUtils.capitalize( listener ) + "Listener";
            Reference ref = ServiceKernelCommand.INSTANCE.reference( reference, serviceItem.moduleItem );
            LinkedHashMap<String, ModuleItem.ServiceItem> linkedModule = services.getServices( ref.module );
            ModuleItem.ServiceItem linked;
            if( linkedModule == null || ( ( linked = linkedModule.get( ref.service ) ) ) == null ) {
                throw new ApplicationException( "for " + serviceItem.serviceName + " listening object " + reference + " is not found" );
            }
            Reflection.Method m = Reflect.reflect( linked.instance.getClass() )
                .method( methodName )
                .orElse( null );
            if( m != null ) {
                m.invoke( linked.instance, instance );
            } else {
                throw new ApplicationException( "listener " + listener + " should have method " + methodName + " in " + reference );
            }
        } );
    }

    public void start() throws ApplicationException {
        applicationConfiguration = ApplicationConfiguration.load();
        start( applicationConfiguration );
    }

    public void start( String appConfigPath, String confd ) throws ApplicationException {
        try {
            URL configURL = appConfigPath.startsWith( "classpath:" )
                ? Thread.currentThread().getContextClassLoader().getResource( appConfigPath.substring( 10 ) )
                : new File( appConfigPath ).toURI().toURL();

            Preconditions.checkNotNull( configURL, appConfigPath + " not found" );

            Path confdPath = confd != null
                ? Paths.get( confd )
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
        log.debug( "initializing application kernel {} with config boot {}, services {}", name, config.boot, config.services.keySet() );

        if( config.boot.main.isEmpty() ) {
            throw new ApplicationException( "boot.main must contain at least one module name" );
        }

        LinkedHashMap<String, ModuleWithLocation> modules = new LinkedHashMap<>();
        LinkedHashMap<Reference, Reference> implementations = new LinkedHashMap<>();

        loadModules( modules );
        configureModules( modules, config, implementations );

        log.debug( "modules {} implementations {}", Lists.map( modules.values(), m -> m.module.name ), implementations );

        checkForUnknownServices( modules, config.services );

        log.debug( "init modules from main {}", config.boot.main );

        var map = new ModuleItemTree();

        ModuleHelper.init( map, modules, implementations, config.boot.main, config.boot.allowActiveByDefault );
        resolveImplementations( map, implementations );

        ServiceTree servicesMap = new ServiceTree();

        instantiateServices( servicesMap, map );
        registerServices( servicesMap );
        linkServices( servicesMap );
        startServices( servicesMap );

        supervisor.preStart();
        supervisor.start();

        log.debug( "application kernel started " + name );
    }

    private void resolveImplementations( ModuleItemTree map,
                                         LinkedHashMap<Reference, Reference> implementations ) {

        for( Map.Entry<Reference, Reference> implEntry : implementations.entrySet() ) {
            Reference interfaceReference = implEntry.getKey();
            Reference implementationReference = implEntry.getValue();

            ModuleItem interfaceModule = map.get( interfaceReference.module );
            ModuleItem implementationModule = map.get( implementationReference.module );

            ModuleItem.ServiceItem interfaceService = interfaceModule.services.get( interfaceReference.service );
            ModuleItem.ServiceItem implementationService = implementationModule.services.get( implementationReference.service );

            if( implementationService == null ) {
                throw new ApplicationException( "Unknown service " + implementationReference.service + " in reference <modules." + implementationReference + ">" );
            }

            interfaceService.abstractImplemenetaion = implementationService;
        }
    }

    private void loadModules( LinkedHashMap<String, ModuleWithLocation> modules ) {
        for( URL moduleConfiguration : moduleConfigurations ) {
            Module module = Module.CONFIGURATION.fromUrl( moduleConfiguration );
            if( StringUtils.isBlank( module.name ) ) {
                throw new ApplicationException( moduleConfiguration + ": module.name is blank" );
            }
            modules.put( module.name, new ModuleWithLocation( module, moduleConfiguration ) );
        }
    }

    private void checkForUnknownServices( LinkedHashMap<String, ModuleWithLocation> modules, Map<String, ApplicationConfigurationModule> services ) throws ApplicationException {
        services.forEach( ( moduleName, conf ) -> {
            if( !modules.containsKey( moduleName ) && conf.isEnabled() )
                throw new ApplicationException( "unknown application configuration module: " + moduleName );
        } );

        for( ModuleWithLocation module : modules.values() ) {
            ApplicationConfigurationModule moduleServices = services.get( module.module.name );
            if( moduleServices != null ) {
                var applicationConfigurationServices = new LinkedHashSet<>( moduleServices.keySet() );

                for( String serviceName : module.module.services.keySet() ) {
                    applicationConfigurationServices.remove( serviceName );
                }

                if( !applicationConfigurationServices.isEmpty() ) {
                    throw new ApplicationException( "unknown application configuration services: " + module.module.name + "." + applicationConfigurationServices );
                }
            }
        }
    }

    private void instantiateServices( ServiceTree servicesMap, ModuleItemTree map ) throws ApplicationException {
        for( ModuleItem.ServiceItem serviceItemOriginal : map.services ) {
            if( !serviceItemOriginal.moduleItem.isEnabled() ) continue;
            if( !serviceItemOriginal.isEnabled() ) continue;
            if( serviceItemOriginal.instance != null ) continue;

            ModuleItem.ServiceItem serviceItem = serviceItemOriginal.getImplementation();

            ModuleItem moduleItem = serviceItem.moduleItem;
            String moduleName = moduleItem.getName();

            Service service = serviceItem.service;
            String implName = serviceItem.serviceName;

            log.trace( "instantiating {}.{} as {} class:{} ...", moduleName, implName, serviceItem.serviceName, service.implementation );
            try {
                Reflection reflect = serviceItem.getReflection();

                for( Object ext : service.ext.values() ) {
                    if( ext instanceof ServiceKernelListener skl ) {
                        serviceItem.instance = skl.newInstance( this, servicesMap, serviceItem, reflect );
                        if( serviceItem.instance != null ) {
                            break;
                        }
                    }
                }

                if( serviceItem.instance == null ) {
                    KernelHelper.ServiceConfigurationParameters parametersWithoutLinks = fixLinksForConstructor( this, serviceItem, servicesMap );

                    var p = new LinkedHashMap<String, Object>();
                    p.putAll( parametersWithoutLinks.serviceReferenceParameters );
                    p.putAll( parametersWithoutLinks.configurationParameters );

                    if( reflect.isInterface() || reflect.isAbstract() ) {
                        if( !service.isAbstract() ) {
                            throw new ApplicationException( "Service <"
                                + serviceItem
                                + "> has an abstract implementation, but the \"abstract = true\" property is missing" );

                        } else {
                            ArrayList<String> services = new ArrayList<>();
                            for( ModuleItem.ServiceItem si : map.services ) {
                                Reflection siReflect = Reflect.reflect( si.service.implementation, Module.coersions );

                                if( !si.service.implementation.equals( service.implementation )
                                    && reflect.underlying.isAssignableFrom( siReflect.underlying ) ) {
                                    services.add( "<modules." + si.getModuleName() + "." + si.serviceName + ">" );
                                }
                            }

                            if( services.isEmpty() ) {
                                throw new ApplicationException( "No implementation has been declared for the abstract service <"
                                    + serviceItem
                                    + "> with interface " + service.implementation );
                            } else {
                                throw new ApplicationException( "No implementation specified for abstract service <"
                                    + serviceItem
                                    + "> with interface " + service.implementation + ". Available implementations ["
                                    + String.join( ",", services ) + "]" );
                            }
                        }
                    }
                    serviceItemOriginal.instance = serviceItem.instance = reflect.newInstance( p, parametersWithoutLinks.serviceReferenceParameters.keySet() );

                    service.parameters.putAll( parametersWithoutLinks.serviceReferenceParameters );
                } else if( serviceItemOriginal.instance == null ) {
                    serviceItemOriginal.instance = serviceItem.instance;
                }

                servicesMap.add( serviceItemOriginal );
            } catch( Exception e ) {
                log.error( "Cannot create/initialize service name = {}.{} class: {}",
                    moduleName, implName, service.implementation );
                if( e instanceof ApplicationException ae ) {
                    throw ae;
                }
                throw new ApplicationException( e );
            }
        }
    }

    private void registerServices( ServiceTree moduleServices ) {
        moduleServices.forEach( serviceItem -> register( serviceItem, serviceItem.serviceName ) );
    }

    private void linkServices( ServiceTree moduleServices ) {
        for( ModuleItem.ServiceItem si : moduleServices.values() ) {
            log.trace( "linking service {}...", si.serviceName );

            linkListeners( si, si.instance );
            linkLinks( si );
            si.service.parameters.forEach( ( parameter, value ) ->
                linkService( new FieldLinkReflection<>( si.getReflection(), si.instance, parameter ), value, si, true )
            );
        }
    }

    @SuppressWarnings( "unchecked" )
    private void linkLinks( ModuleItem.ServiceItem serviceItem ) {
        serviceItem.service.link.forEach( ( fieldName, serviceRef ) ->
            ServiceKernelCommand.INSTANCE.get( serviceRef, this, serviceItem, services )
                .ifSuccessOrElse(
                    service -> {
                        var reflect = Reflect.reflect( service.service.implementation );
                        var methodSuffix = StringUtils.capitalize( fieldName );

                        var linkMethod = reflect.method( "add" + methodSuffix ).orElse( null );
                        if( linkMethod == null ) {
                            linkMethod = reflect.method( "set" + methodSuffix ).orElse( null );
                        }
                        if( linkMethod == null ) {
                            linkMethod = reflect.method( "add" + methodSuffix + "Listener" ).orElse( null );
                        }

                        if( linkMethod != null && linkMethod.parameters.size() == 1 ) {
                            linkMethod.invoke( service.instance, serviceItem.instance );
                        } else {
                            var linkField = reflect.field( fieldName ).orElse( null );
                            if( linkField != null ) {
                                if( linkField.type().assignableTo( Collection.class ) ) {
                                    ( ( Collection<Object> ) linkField.get( service.instance ) )
                                        .add( serviceItem.instance );
                                } else {
                                    linkField.set( service.instance, serviceItem.instance );
                                }
                            } else {
                                exception( new ReflectException( "link to " + service.serviceName + "/" + service.service.implementation
                                    + " should have field " + fieldName
                                    + " for " + serviceItem.serviceName + "/" + serviceItem.service.implementation ) );
                            }
                        }
                    },
                    exception( _ -> new ApplicationException( "Unknown service link " + serviceRef + " in"
                        + "\n{\n\timplementation = " + serviceItem.service.implementation
                        + "\n\tlink." + fieldName + " = " + serviceRef
                        + "\n}" ) ) ) );
    }

    @SuppressWarnings( "unchecked" )
    private void linkService( LinkReflection lRef,
                              Object parameterValue,
                              ModuleItem.ServiceItem si,
                              boolean failIfNotFound ) throws ApplicationException {
        if( parameterValue instanceof List<?> parameterList ) {
            Object instance = lRef.get();
            if( instance instanceof List<?> ) {
                var instanceList = ( List<Object> ) instance;
                ListIterator<Object> instanceIterator = instanceList.listIterator();
                for( Object parameter : parameterList )
                    linkService( new ListLinkReflection<>( instanceIterator ), parameter, si, false );
            }
        } else if( parameterValue instanceof Map ) {
            var parameterMap = ( Map<Object, Object> ) parameterValue;
            Object instance = lRef.get();

            if( instance == null ) return;

            for( Map.Entry<Object, Object> entry : parameterMap.entrySet() ) {
                boolean isMap = instance instanceof Map;
                final LinkReflection<?> linkReflection = isMap
                    ? new MapLinkReflection<>( ( Map<Object, Object> ) instance, entry.getKey() )
                    : new FieldLinkReflection<>( Reflect.reflect( instance.getClass() ), instance, entry.getKey().toString() );

                linkService( linkReflection, entry.getValue(), si, !isMap );
            }
        } else if( ServiceKernelCommand.INSTANCE.matches( parameterValue ) ) {
            Result<Object, ServiceStorage.ErrorStatus> linkResult = ServiceKernelCommand.INSTANCE.getInstance( parameterValue, this, si, services );

            if( failIfNotFound && !linkResult.isSuccess() )
                throw new ApplicationException( "for " + si.serviceName + " linked object " + parameterValue + " is not found" );
            linkResult.ifSuccess( lRef::set );
        }
    }

    private void startServices( ServiceTree moduleServices ) {
        moduleServices.forEach( serviceItem -> {
            log.debug( "starting {} as {}...", serviceItem.serviceName, serviceItem.serviceName );

            startService( supervisor, serviceItem );
            log.debug( "starting {} as {}... Done", serviceItem.serviceName, serviceItem.serviceName );
        } );
    }

    private void startService( Supervisor supervisor, ModuleItem.ServiceItem si ) {
        Service service = si.service;
        Object instance = si.instance;
        if( service.supervision.supervise ) {
            supervisor.startSupervised( si.serviceName, instance,
                service.supervision.preStartWith,
                service.supervision.startWith,
                service.supervision.preStopWith,
                service.supervision.stopWith
            );
        }

        if( service.supervision.thread ) {
            supervisor.startThread( si.serviceName, instance, applicationConfiguration.shutdown );
        } else {
            if( service.supervision.schedule && service.supervision.cron != null )
                supervisor.scheduleCron( si.serviceName, ( Runnable ) instance,
                    service.supervision.cron );
            else if( service.supervision.schedule && service.supervision.delay != 0 )
                supervisor.scheduleWithFixedDelay( si.serviceName, ( Runnable ) instance,
                    service.supervision.delay, MILLISECONDS );
        }
    }

    public void register( ModuleItem.ServiceItem serviceItem, String serviceName ) throws ApplicationException {
        ModuleItem.ServiceItem registered;

        if( ( registered = services.putIfAbsent( serviceItem, serviceName, serviceItem ) ) != null )
            throw new ApplicationException( serviceItem.getModuleName() + ":" + serviceName + " Service " + serviceItem.serviceName + " is already registered [" + registered.instance.getClass() + "]" );
    }

    public void stop() {
        log.debug( "stopping application kernel {}...", name );
        supervisor.preStop( applicationConfiguration.shutdown );
        supervisor.stop( applicationConfiguration.shutdown );
        services.clear();
        log.debug( "application kernel stopped {}", name );
    }

    @SuppressWarnings( "unchecked" )
    public <T> Optional<T> service( String moduleName, String serviceName ) {
        if( ServiceStorage.ALL_MODULES.contains( moduleName ) ) {
            return services.findFirstServiceByName( serviceName ).map( si -> ( T ) si.instance );
        }

        ModuleItem.ServiceItem si = services.get( moduleName, serviceName );
        if( si == null ) {
            return Optional.empty();
        }

        return Optional.of( ( T ) si.instance );
    }

    private <T> Optional<T> service( Reference reference ) {
        return service( reference.module, reference.service );
    }

    public <T> Optional<T> service( String reference ) {
        Reference ref = ServiceKernelCommand.INSTANCE.reference(
            reference.startsWith( "<modules." ) ? reference : "<modules." + reference + ">", null );
        return service( ref );
    }

    @SuppressWarnings( "unchecked" )
    public <T> List<T> services( String moduleName, String serviceName ) {
        if( ServiceStorage.ALL_MODULES.contains( moduleName ) ) {
            return Lists.map( services.findAllServicesByName( serviceName ), si -> ( T ) si.instance );
        }

        ModuleItem.ServiceItem si = services.get( moduleName, serviceName );
        if( si == null ) return List.of();

        return List.of( ( T ) si.instance );
    }

    public <T> List<T> ofClass( Class<T> clazz ) {
        return ofClass( "*", clazz );
    }

    @SuppressWarnings( "unchecked" )
    public <T> List<T> ofClass( String moduleName, Class<T> clazz ) {
        var ret = new ArrayList<T>();

        this.services.forEach( serviceItem -> {
            if( ServiceStorage.ALL_MODULES.contains( moduleName ) || serviceItem.getModuleName().equals( moduleName ) )
                if( clazz.isInstance( serviceItem.instance ) ) ret.add( ( T ) serviceItem.instance );
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

    public <T> List<ServiceExt<T>> servicesByExt( String ext ) {
        var ret = new ArrayList<ServiceExt<T>>();

        for( ModuleItem.ServiceItem si : services.values() ) {
            T extConfiguration = si.service.getExt( ext );
            if( extConfiguration == null ) continue;

            ret.add( new ServiceExt<>( si.serviceName, si, extConfiguration ) );
        }

        return ret;
    }

    public void unregister( String moduleName, String serviceName ) {
        services.removeService( moduleName, serviceName );
    }

    public void unregisterServices() {
        services.clear();
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
