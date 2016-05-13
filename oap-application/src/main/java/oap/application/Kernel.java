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

import lombok.extern.slf4j.Slf4j;
import oap.application.remote.RemoteInvocationHandler;
import oap.application.supervision.Supervisor;
import oap.reflect.Reflect;
import oap.reflect.Reflection;
import oap.util.Sets;
import oap.util.Stream;

import java.net.URL;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.apache.commons.collections4.CollectionUtils.subtract;

@Slf4j
public class Kernel {
   private final List<URL> modules;
   private Supervisor supervisor = new Supervisor();

   public Kernel( List<URL> modules ) {
      this.modules = modules;
   }

   private Map<String, Module.Service> initializeServices( Map<String, Module.Service> services,
                                                           Set<String> initialized, ApplicationConfiguration config ) {

      HashMap<String, Module.Service> deferred = new HashMap<>();

      for( Map.Entry<String, Module.Service> entry : services.entrySet() ) {
         Module.Service service = entry.getValue();
         String serviceName = service.name != null ? service.name : entry.getKey();
         if( service.profile != null && !config.profiles.contains( service.profile ) ) {
            log.debug( "skipping " + entry.getKey() + " with profile " + service.profile );
            continue;
         }
         if( initialized.containsAll( service.dependsOn ) ) {
            log.debug( "initializing {} as {}", entry.getKey(), serviceName );

            @SuppressWarnings( "unchecked" )
            Reflection reflect = Reflect.reflect( service.implementation, Module.coersions );

            Object instance;
            if( service.remoteUrl == null ) {
               initializeServiceLinks( serviceName, service );
               instance = reflect.newInstance( service.parameters );
            } else instance = RemoteInvocationHandler.proxy(
               service.remoteUrl,
               service.remoteName,
               reflect.underlying,
               service.certificateLocation,
               service.certificatePassword,
               service.timeout //TODO refactor to have Remoting class with related properties
            );
            Application.register( serviceName, instance );
            Application.register( entry.getKey(), instance );

            if( service.supervision.supervise )
               supervisor.startSupervised( serviceName, instance,
                  service.supervision.startWith,
                  service.supervision.stopWith );
            if( service.supervision.thread )
               supervisor.startThread( serviceName, instance );
            else {
               if( service.supervision.schedule && service.supervision.cron != null )
                  supervisor.scheduleCron( serviceName, ( Runnable ) instance,
                     service.supervision.cron );
               else if( service.supervision.schedule && service.supervision.delay != 0 )
                  supervisor.scheduleWithFixedDelay( serviceName, ( Runnable ) instance,
                     service.supervision.delay, TimeUnit.MILLISECONDS );
            }
            initialized.add( serviceName );
         } else {
            log.debug( "dependencies are not ready - deferring " + serviceName + ": "
               + subtract( service.dependsOn, initialized ) );
            deferred.put( entry.getKey(), service );
         }
      }

      return deferred.size() == services.size() ? deferred : initializeServices( deferred, initialized, config );
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
         Object link = Application.service( ( ( String ) value ).substring( "@service:".length() ) );
         log.debug( "for {} linking {} -> {} as {}", name, key, value, link );
         if( link == null )
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

      return deferred.size() == modules.size() ? deferred : initialize( deferred, initialized, initializedServices, config );
   }

   public void start() {
      start( new ApplicationConfiguration() );
   }

   public void start( Path appConfigPath, Path confd ) {
      start( ApplicationConfiguration.load( appConfigPath, confd ) );
   }

   public void start( Path appConfigPath ) {
      start( ApplicationConfiguration.load( appConfigPath ) );
   }

   private void start( ApplicationConfiguration config ) {
      log.debug( "initializing application kernel..." );
      log.debug( "Application config: ", config );

      Set<Module> moduleConfigs = Stream.of( modules )
         .map( module -> Module.CONFIGURATION.fromHocon( module, config.services ) )
         .toSet();
      log.debug( "modules = " + Sets.map( moduleConfigs, m -> m.name ) );
      log.trace( "modules configs = " + moduleConfigs );

      Set<Module> def = initialize( moduleConfigs, new HashSet<>(), new HashSet<>(), config );
      if( !def.isEmpty() ) {
         Set<String> names = Sets.map( def, m -> m.name );
         log.error( "failed to initialize: {} ", names );
         throw new ApplicationException( "failed to initialize modules: " + names );
      }

      supervisor.start();

      log.debug( "application kernel started" );
   }

   public void stop() {
      log.debug( "stopping application kernel..." );
      supervisor.stop();
      Application.unregisterServices();
      log.debug( "application kernel stopped" );
   }

}
