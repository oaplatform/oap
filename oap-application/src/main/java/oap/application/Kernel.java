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
import lombok.val;
import oap.application.remote.RemoteInvocationHandler;
import oap.application.supervision.Supervisor;
import oap.json.Binder;
import oap.metrics.Metrics;
import oap.reflect.Reflect;
import oap.reflect.ReflectException;
import oap.reflect.Reflection;
import oap.util.Sets;
import oap.util.Stream;
import oap.util.Strings;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
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
    private final List<URL> plugins;
    private final Set<String> profiles = Sets.empty();
    private final Set<Module> modules = Sets.empty();
    private final ConcurrentMap<String, Object> services = new ConcurrentHashMap<>();
    private final Supervisor supervisor = new Supervisor();
    private final List<DynamicConfig.Control> dynamicConfigurations = new ArrayList<>();
    private final Linker linker = new Linker( this );

    public Kernel( String name, List<URL> configurations, List<URL> plugins ) {
        this.name = name;
        this.configurations = configurations;
        this.plugins = plugins;
    }

    public Kernel( List<URL> configurations, List<URL> plugins ) {
        this( DEFAULT, configurations, plugins );
    }

    private Map<String, Module.Service> initializeServices( Map<String, Module.Service> services,
                                                            Set<String> initialized, ApplicationConfiguration config ) {

        HashMap<String, Module.Service> deferred = new HashMap<>();

        for( Map.Entry<String, Module.Service> entry : services.entrySet() ) {
            Module.Service service = entry.getValue();
            service.name = service.name != null ? service.name : entry.getKey();
            if( !service.enabled ) {
                initialized.add( service.name );
                log.debug( "service {} is disabled.", entry.getKey() );
                continue;
            }
            if( service.profile != null && !config.profiles.contains( service.profile ) ) {
                log.debug( "skipping " + entry.getKey() + " with profile " + service.profile );
                continue;
            }

            List<String> dependsOn = Stream.of( service.dependsOn ).filter( this::serviceEnabled ).toList();

            if( initialized.containsAll( dependsOn ) ) {
                log.debug( "initializing {} as {}", entry.getKey(), service.name );

                if( service.implementation == null ) {
                    throw new ApplicationException( "failed to initialize service: " + service.name + ". implementation == null" );
                }
                @SuppressWarnings( "unchecked" )
                Reflection reflect = Reflect.reflect( service.implementation, Module.coersions );

                Object instance;
                if( !service.isRemoteService() ) {
                    try {
                        instance = linker.link( service, () -> reflect.newInstance( service.parameters ) );
                        initializeDynamicConfigurations( reflect, instance );
                    } catch( ReflectException e ) {
                        log.info( "service name = {}, remote = {}, profile = {}", service.name, service.remote, service.profile );
                        throw e;
                    }
                } else instance = RemoteInvocationHandler.proxy(
                    service.remote,
                    reflect.underlying );
                register( service.name, instance );
                if( !service.name.equals( entry.getKey() ) )
                    register( entry.getKey(), instance );

                if( service.supervision.supervise )
                    supervisor.startSupervised( service.name, instance,
                        service.supervision.startWith,
                        service.supervision.stopWith,
                        service.supervision.reloadWith );
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
                initialized.add( service.name );
            } else {
                log.debug( "dependencies are not ready - deferring " + service.name + ": "
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
            .filter( field -> field.type().assignableFrom( DynamicConfig.class ) && !field.type().underlying.equals( Object.class ) )
            .forEach( f -> {
                DynamicConfig<?> config = ( DynamicConfig<?> ) f.get( instance );
                if( config.isUpdateable() ) dynamicConfigurations.add( config.control );
            } );
    }


    private Set<Module> initialize( Set<Module> modules, Set<String> initialized, Set<String> initializedServices, ApplicationConfiguration config ) {
        HashSet<Module> deferred = new HashSet<>();

        for( Module module : modules ) {
            log.debug( "initializing module " + module.name + " [abstract = " + module.isAbstract + "]" );

            if( module.isAbstract ) {
                initialized.add( module.name );
                continue;
            }

            if( initialized.containsAll( module.dependsOn ) ) {
                for( val ext : module.extendsModules ) {
                    val extModule = this.modules.stream().filter( m -> m.name.equals( ext ) ).findAny()
                        .orElseThrow( () -> new ApplicationException( "extends module " + ext + " not found." ) );

                    module.services.putAll( extModule.services );
                }

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

    @SneakyThrows
    public void start( String appConfigPath, String confd ) {
        val configURL =
            appConfigPath.startsWith( "classpath:" )
                ? Thread.currentThread().getContextClassLoader().getResource( appConfigPath.substring( 10 ) )
                : new File( appConfigPath ).toURI().toURL();

        Preconditions.checkNotNull( configURL, appConfigPath + " not found" );

        val confdPath = confd != null ? Paths.get( confd )
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

        initPlugins();

        this.supervisor.start();

        this.modules.add( new Module( Module.DEFAULT ) );
        log.debug( "application kernel started" );
    }

    private void initPlugins() {
        if( !plugins.isEmpty() )
            log.warn( "plugins are deprecated. Use standard kernel facilities" );
        for( val url : plugins ) {
            val plugin = Plugin.CONFIGURATION.fromHocon( url );

            for( val export : plugin.export ) {
                for( val serviceName : export.service ) {
                    val service = Application.service( serviceName );
                    Preconditions.checkNotNull( service, "Unknown service " + serviceName );
                    val reflect = Reflect.reflect( service.getClass() );
                    export.parameters.forEach( ( name, services ) -> {
                        val field = reflect.field( name );
                        if( field.type().assignableTo( List.class ) ) {
                            for( val refServiceName : services ) {
                                val refService = Application.service( refServiceName );
                                Preconditions.checkNotNull( service, "Unknown service " + serviceName );
                                ( ( List ) field.get( service ) ).add( refService );
                            }
                        } else {
                            Preconditions.checkArgument( services.size() == 1 );

                            val refService = Application.service( services.get( 0 ) );
                            Preconditions.checkNotNull( service, "Unknown service " + serviceName );
                            field.set( service, refService );
                        }
                    } );
                }
            }
        }
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

    @Deprecated
    public void reload() {
//@todo rethink this
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

    boolean serviceEnabled( String name ) {
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
