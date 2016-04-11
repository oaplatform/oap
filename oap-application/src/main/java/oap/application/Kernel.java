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
import oap.json.Binder;
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
               reflect.underlying,
               service.certificateLocation,
               service.certificatePassword,
               service.timeout //TODO refactor to have Remoting class with related properties
            );
            Application.register( serviceName, instance );
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
         .map( m -> Module.CONFIGURATION.fromHocon( m, config ) )
         .toSet();
      log.trace( "modules = " + Sets.map( moduleConfigs, m -> m.name ) );

      Set<Module> def = initialize( moduleConfigs, new HashSet<>(), new HashSet<>() );
      if( !def.isEmpty() ) {
         log.error( "failed to initialize: " + Sets.map( def, m -> m.name ) );
         throw new ApplicationException( "failed to initialize modules" );
      }

      supervisor.start();
   }

   public void stop() {
      supervisor.stop();
      Application.unregisterServices();
   }

   public void start( Path configPath, Path confd ) {
      start( ApplicationConfiguration.load( configPath, confd ) );
   }
}
