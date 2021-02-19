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
import oap.application.ModuleItem.ModuleReference;
import oap.application.ModuleItem.ModuleReference.ServiceLink;
import oap.application.ModuleItem.ServiceItem.ServiceReference;
import oap.util.Lists;
import oap.util.Pair;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static oap.application.KernelHelper.isServiceLink;
import static oap.util.Pair.__;

/**
 * Created by igor.petrenko on 2021-02-12.
 */
@Slf4j
class Modules {
    final LinkedHashMap<String, ModuleItem> map = new LinkedHashMap<>();

    Modules( LinkedHashSet<Module> modules, LinkedHashSet<String> profiles ) {
        init( modules, profiles );
        fixServiceName();
        initDeps( profiles );
        validateDeps();
        validateImplementation();
        sort();
        removeDisabled();
        validateRemoting();
    }

    private static ModuleItem findModule( LinkedHashMap<String, ModuleItem> modules, ModuleItem fromModule, String name ) {
        var moduleItem = modules.get( name );

        if( moduleItem == null )
            throw new ApplicationException( "[" + fromModule.module.name + "]: dependsOn not found: " + name );

        return moduleItem;
    }

    private void validateRemoting() {
        var invalidRemoting = new ArrayList<String>();

        for( var moduleItem : map.values() ) {
            for( var serviceItem : moduleItem.services.values() ) {
                if( !serviceItem.service.isRemoteService() ) continue;

                if( serviceItem.service.remote.url == null )
                    invalidRemoting.add( moduleItem.getName() + ":" + serviceItem.serviceName );
            }
        }

        if( !invalidRemoting.isEmpty() ) {
            log.error( "uri == null, services " + invalidRemoting );
            throw new ApplicationException( "remoting: uri == null, services " + invalidRemoting );
        }
    }

    private void removeDisabled() {
        removeDisabledModules();
        removeDisabledServices();
    }

    private void removeDisabledModules() {
        map.values().removeIf( moduleInfo -> !moduleInfo.enabled );
    }

    private void removeDisabledServices() {
        for( var moduleInfo : map.values() ) {
            moduleInfo.services.values().removeIf( serviceInfo -> !serviceInfo.enabled );
        }
    }

    private void validateDeps() {
        validateModuleDeps();
        validateServiceDeps();
    }

    private void validateModuleDeps() {
        for( var moduleInfo : map.values() ) {
            if( !moduleInfo.enabled ) continue;

            for( var dModuleInfo : moduleInfo.getDependsOn().values() ) {
                if( !dModuleInfo.moduleItem.enabled ) {
                    throw new ApplicationException( "[" + moduleInfo.module.name + ":*] dependencies are not enabled [" + dModuleInfo.moduleItem.module.name + "]" );
                }
            }
        }
    }

    private void validateImplementation() {
        for( var moduleInfo : map.values() ) {
            if( !moduleInfo.enabled ) continue;

            for( var serviceInfo : moduleInfo.services.values() ) {
                if( !serviceInfo.enabled ) continue;

                if( serviceInfo.service.implementation == null )
                    throw new ApplicationException( "failed to initialize service: " + moduleInfo.module.name + ":" + serviceInfo.serviceName + ". implementation == null" );
            }
        }
    }

    private void validateServiceDeps() {
        for( var moduleInfo : map.values() ) {
            if( !moduleInfo.enabled ) continue;

            for( var serviceInfo : moduleInfo.services.values() ) {
                if( !serviceInfo.enabled ) continue;

                for( var dServiceReference : serviceInfo.dependsOn ) {
                    if( !dServiceReference.serviceItem.enabled && dServiceReference.required ) {
                        throw new ApplicationException( "[" + moduleInfo.module.name + ":" + serviceInfo.service.name + "] dependencies are not enabled [" + dServiceReference.serviceItem.service.name + "]" );
                    }
                }
            }
        }
    }

    private void sort() {
        sortModules();
        for( var moduleInfo : map.values() ) {
            sortServices( moduleInfo );
        }
    }

    private void sortModules() {
        var graph = new LinkedList<>( map.values() );

        var newMap = new LinkedHashMap<String, ModuleItem>();
        var noIncomingEdges = new LinkedList<ModuleItem>();


        graph.removeIf( moduleItem -> {
            if( moduleItem.getDependsOn().isEmpty() ) {
                noIncomingEdges.add( moduleItem );
                return true;
            }
            return false;
        } );

        while( !noIncomingEdges.isEmpty() ) {
            var moduleItem = noIncomingEdges.removeFirst();

            newMap.put( moduleItem.module.name, moduleItem );

            graph.removeIf( node -> {
                node.getDependsOn().remove( moduleItem.module.name );

                if( node.getDependsOn().isEmpty() ) {
                    noIncomingEdges.add( node );
                    return true;
                }

                return false;
            } );
        }

        if( !graph.isEmpty() ) {
            log.error( "graph has at least one cycle:" );
            for( var node : graph ) {
                log.error( "  module: [{}]:{}", node.module.name, Lists.map( node.getDependsOn().values(), d -> d.moduleItem.module.name ) );
                for( var d : node.getDependsOn().values() ) {
                    for( var sl : d.serviceLink ) {
                        log.error( "    service: {}:{} -> {}:{}", node.module.name, sl.from.serviceName, d.moduleItem.getName(), sl.to.serviceName );
                    }
                }
            }

            throw new ApplicationException( "graph has at least one cycle" );
        }

        map.clear();
        map.putAll( newMap );
    }

    private void sortServices( ModuleItem moduleInfo ) {
        var graph = new LinkedList<>( moduleInfo.services.values() );

        var newMap = new LinkedHashMap<String, ModuleItem.ServiceItem>();
        var noIncomingEdges = new LinkedList<ModuleItem.ServiceItem>();


        graph.removeIf( serviceItem -> {
            if( serviceItem.dependsOn.isEmpty() ) {
                noIncomingEdges.add( serviceItem );
                return true;
            }
            return false;
        } );

        while( !noIncomingEdges.isEmpty() ) {
            var serviceItem = noIncomingEdges.removeFirst();

            newMap.put( serviceItem.serviceName, serviceItem );

            graph.removeIf( node -> {
                node.dependsOn.removeIf( sr -> sr.serviceItem.equals( serviceItem ) );

                if( node.dependsOn.isEmpty() ) {
                    noIncomingEdges.add( node );
                    return true;
                }

                return false;
            } );
        }

        if( !graph.isEmpty() ) {
            log.error( "[{}] module graph has at least one cycle:", moduleInfo.module.name );
            for( var node : graph ) {
                log.error( "  [{}]:{}", node.service.name, Lists.map( node.dependsOn, d -> d.serviceItem.service.name ) );
            }

            throw new ApplicationException( "[" + moduleInfo.module.name + "] graph has at least one cycle" );
        }

        moduleInfo.services.clear();
        moduleInfo.services.putAll( newMap );
    }

    private void init( LinkedHashSet<Module> modules, LinkedHashSet<String> profiles ) {
        initModules( modules, profiles );
        initServices( profiles );
    }

    private void initModules( LinkedHashSet<Module> modules, LinkedHashSet<String> profiles ) {
        for( var module : modules ) {
            var enabled = true;
            if( !KernelHelper.isModuleEnabled( module, profiles ) ) {
                log.debug( "skipping module {} with profiles {}", module.name, module.profiles );
                enabled = false;
            }

            map.put( module.name, new ModuleItem( module, enabled, new LinkedHashMap<>() ) );
        }
    }

    private void initServices( LinkedHashSet<String> profiles ) {
        for( var moduleInfo : map.values() ) {
            for( var serviceEntry : moduleInfo.module.services.entrySet() ) {
                var serviceName = serviceEntry.getKey();
                var service = serviceEntry.getValue();
                var enabled = true;

                if( !KernelHelper.isServiceEnabled( service, profiles ) ) {
                    log.debug( "skipping service {}:{} with profiles {}", moduleInfo.module.name, serviceName, service.profiles );
                    enabled = false;
                }

                if( !service.enabled ) {
                    log.debug( "skipping service {}:{}, reason: enabled = false", moduleInfo.module.name, serviceName );
                    enabled = false;
                }

                moduleInfo.services.put( serviceName, new ModuleItem.ServiceItem( serviceName, moduleInfo, service, enabled ) );
            }
        }
    }

    private void fixServiceName() {
        for( var module : map.values() ) {
            module.services.forEach( ( implName, serviceItem ) ->
                serviceItem.fixServiceName( implName ) );
        }
    }

    private void initDeps( LinkedHashSet<String> profiles ) {
        initModuleDeps( profiles );
        initServicesDeps();
    }

    private void initServicesDeps() {
        for( var moduleItem : map.values() ) {
            if( !moduleItem.enabled ) continue;

            moduleItem.services.forEach( ( serviceName, serviceItem ) -> {
                if( serviceItem.enabled ) {
                    for( var dService : serviceItem.service.dependsOn ) {
                        var moduleService = findService( dService );
                        if( moduleService == null ) {
                            throw new ApplicationException( "[" + moduleItem.module.name + ":" + serviceName + "] " + dService + " not found" );
                        }

                        if( !moduleItem.equals( moduleService._1 ) )
                            moduleItem.addDependsOn( new ModuleReference( moduleService._1, new ServiceLink( serviceItem, moduleService._2 ) ) );
                        else
                            serviceItem.addDependsOn( new ServiceReference( moduleService._2, true ) );
                    }

                    serviceItem.service.parameters.forEach( ( key, value ) ->
                        initDepsParameter( moduleItem, serviceName, value, true, serviceItem ) );
                }
            } );
        }
    }

    private void initDepsParameter( ModuleItem moduleItem, String serviceName,
                                    Object value, boolean required,
                                    ModuleItem.ServiceItem serviceItem ) {
        if( isServiceLink( value ) ) {
            var linkName = Module.Reference.of( value ).name;
            var moduleService = findService( linkName );
            if( moduleService == null ) {
                throw new ApplicationException( "[" + moduleItem.module.name + ":" + serviceName + "#" + linkName + "] " + linkName + "  not found" );
            }

            if( !moduleItem.equals( moduleService._1 ) )
                moduleItem.addDependsOn( new ModuleReference( moduleService._1, new ServiceLink( serviceItem, moduleService._2 ) ) );
            else
                serviceItem.addDependsOn( new ServiceReference( moduleService._2, required ) );
        } else if( value instanceof List<?> )
            for( var item : ( List<?> ) value )
                initDepsParameter( moduleItem, serviceName, item, false, serviceItem );
        else if( value instanceof Map<?, ?> )
            for( var item : ( ( Map<?, ?> ) value ).values() )
                initDepsParameter( moduleItem, serviceName, item, false, serviceItem );
    }


    private Pair<ModuleItem, ModuleItem.ServiceItem> findService( String serviceName ) {
        var found = new ArrayList<Pair<ModuleItem, ModuleItem.ServiceItem>>();

        for( var moduleInfo : map.values() ) {
            for( var entry : moduleInfo.services.entrySet() ) {
                if( serviceName.equals( entry.getValue().service.name ) ) {
                    found.add( __( moduleInfo, entry.getValue() ) );
                }
            }
        }

        if( found.isEmpty() ) return null;

        var enabled = Lists.find2( found, f -> f._1.enabled && f._2.enabled );
        if( enabled != null ) return enabled;

        return Lists.head2( found );
    }

    private void initModuleDeps( LinkedHashSet<String> profiles ) {
        for( var moduleItem : map.values() ) {
            for( var d : moduleItem.module.dependsOn ) {
                ModuleItem dModule;
                if( KernelHelper.profileEnabled( d.profiles, profiles ) && ( dModule = findModule( map, moduleItem, d.name ) ).enabled ) {
                    moduleItem.addDependsOn( new ModuleReference( dModule ) );
                } else {
                    log.trace( "[module#{}]: skip dependsOn {}", moduleItem.module.name, new LinkedHashSet<ModuleItem>() );
                }
            }
        }
    }

}
