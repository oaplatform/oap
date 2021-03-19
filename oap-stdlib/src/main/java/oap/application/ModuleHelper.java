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
import java.util.Set;
import java.util.regex.Pattern;

import static oap.util.Pair.__;

/**
 * Created by igor.petrenko on 2021-02-12.
 */
@Slf4j
class ModuleHelper {
    public static final Pattern MODULE_SERVICE_NAME_PATTERN = Pattern.compile( "^[A-Za-z\\-_0-9]+$" );

    private ModuleHelper() {
    }

    private static ModuleTree init( Set<Kernel.ModuleWithLocation> modules, Set<String> profiles ) {
        var map = initModules( modules, profiles );
        initServices( map, profiles );

        return map;
    }

    public static ModuleTree init( LinkedHashSet<Kernel.ModuleWithLocation> modules,
                                   Set<String> profiles, Set<String> main, Kernel kernel ) throws ApplicationException {
        var map = init( modules, profiles );

        loadOnlyMainModuleAndDependsOn( map, main, profiles );

        validateModuleName( map );
        validateServiceName( map );

        fixServiceName( map );
        initDeps( map, profiles, kernel );
        validateDeps( map );
        validateImplementation( map );
        sort( map );
        removeDisabled( map );
        validateRemoting( map );

        return map;
    }

    private static void validateServiceName( ModuleTree map ) throws ApplicationException {
        for( var moduleInfo : map.values() ) {
            for( var serviceName : moduleInfo.services.keySet() ) {
                if( !MODULE_SERVICE_NAME_PATTERN.matcher( serviceName ).matches() ) {
                    throw new ApplicationException( "service name " + serviceName + " does not match specified regex " + MODULE_SERVICE_NAME_PATTERN.pattern() );
                }
            }
        }
    }

    private static void validateModuleName( ModuleTree map ) throws ApplicationException {
        for( var moduleName : map.keySet() ) {
            if( !MODULE_SERVICE_NAME_PATTERN.matcher( moduleName ).matches() ) {
                throw new ApplicationException( "module name " + moduleName + " does not match specified regex " + MODULE_SERVICE_NAME_PATTERN.pattern() );
            }
        }
    }

    @SuppressWarnings( "checkstyle:ParameterAssignment" )
    private static Pair<ModuleItem, ModuleItem.ServiceItem> findService( ModuleTree map, String thisModuleName, String moduleName, String serviceName ) {
        var found = new ArrayList<Pair<ModuleItem, ModuleItem.ServiceItem>>();

        for( var moduleInfo : map.values() ) {
            if( KernelHelper.THIS.contains( moduleName ) ) moduleName = thisModuleName;

            if( !moduleInfo.getName().equals( moduleName ) ) continue;

            for( var entry : moduleInfo.services.entrySet() ) {
                if( serviceName.equals( entry.getValue().serviceName ) || serviceName.equals( entry.getValue().service.name ) ) {
                    found.add( __( moduleInfo, entry.getValue() ) );
                }
            }
        }

        if( found.isEmpty() ) return null;

        var enabled = Lists.find2( found, f -> f._1.isEnabled() && f._2.enabled );
        if( enabled != null ) return enabled;

        return Lists.head2( found );
    }

    private static ModuleTree initModules( Set<Kernel.ModuleWithLocation> modules, Set<String> profiles ) {
        var map = new ModuleTree();

        for( var module : modules ) {
            var enabled = true;
            if( !KernelHelper.isModuleEnabled( module.module, profiles ) ) {
                log.debug( "skipping module {} with profiles {}", module.module.name, module.module.profiles );
                enabled = false;
            }

            map.put( module.module.name, new ModuleItem( module.module, module.location, enabled, new LinkedHashMap<>() ) );
        }

        return map;
    }

    private static void initServices( ModuleTree map, Set<String> profiles ) {
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

    private static void initServicesDeps( ModuleTree map, Kernel kernel ) {
        for( var moduleItem : map.values() ) {
            if( !moduleItem.isEnabled() ) continue;

            moduleItem.services.forEach( ( serviceName, serviceItem ) -> {
                if( serviceItem.enabled ) {
                    for( var dService : serviceItem.service.dependsOn ) {

                        String dModuleName;
                        String dServiceName;
                        if( ServiceKernelCommand.INSTANCE.matches( dService ) ) {
                            var ref = ServiceKernelCommand.INSTANCE.reference( ( String ) dService, moduleItem );
                            dModuleName = ref.module;
                            dServiceName = ref.service;
                        } else if( dService instanceof String ) {
                            dModuleName = "this";
                            dServiceName = ( String ) dService;
                        } else {
                            throw new ApplicationException( "Unknown deps format " + dService );
                        }

                        var moduleService = findService( map, moduleItem.getName(), dModuleName, dServiceName );
                        if( moduleService == null ) {
                            throw new ApplicationException( "[" + dModuleName + ":" + dServiceName + "] 'this:" + dService + "' not found" );
                        }

                        if( !moduleItem.equals( moduleService._1 ) )
                            moduleItem.addDependsOn( new ModuleReference( moduleService._1, new ServiceLink( serviceItem, moduleService._2 ) ) );
                        else
                            serviceItem.addDependsOn( new ServiceReference( moduleService._2, true ) );
                    }

                    serviceItem.service.parameters.forEach( ( key, value ) ->
                        initDepsParameter( map, kernel, moduleItem, serviceName, value, true, serviceItem ) );
                }
            } );
        }
    }

    private static void initDepsParameter( ModuleTree map,
                                           Kernel kernel,
                                           ModuleItem moduleItem, String serviceName,
                                           Object value, boolean required,
                                           ModuleItem.ServiceItem serviceItem ) {
        if( ServiceKernelCommand.INSTANCE.matches( value ) ) {
            var reference = ServiceKernelCommand.INSTANCE.reference( ( String ) value, moduleItem );
            var moduleService = findService( map, moduleItem.getName(), reference.module, reference.service );
            if( moduleService == null ) {
                throw new ApplicationException( "[" + moduleItem.module.name + ":" + serviceName + "#" + reference + "] " + reference + "  not found" );
            }

            if( !moduleItem.equals( moduleService._1 ) )
                moduleItem.addDependsOn( new ModuleReference( moduleService._1, new ServiceLink( serviceItem, moduleService._2 ) ) );
            else
                serviceItem.addDependsOn( new ServiceReference( moduleService._2, required ) );
        } else if( GroupKernelCommand.INSTANCE.matches( value ) ) {
            var result = GroupKernelCommand.INSTANCE.get( value, kernel, moduleItem, map );
            if( result.isSuccess() ) {
                for( var dServiceItemObject : result.successValue ) {
                    var dServiceItem = ( ModuleItem.ServiceItem ) dServiceItemObject;
                    if( !moduleItem.equals( dServiceItem.moduleItem ) )
                        moduleItem.addDependsOn( new ModuleReference( dServiceItem.moduleItem, new ServiceLink( serviceItem, dServiceItem ) ) );
                    else
                        serviceItem.addDependsOn( new ServiceReference( dServiceItem, required ) );
                }
            }
        } else if( value instanceof List<?> )
            for( var item : ( List<?> ) value )
                initDepsParameter( map, kernel, moduleItem, serviceName, item, false, serviceItem );
        else if( value instanceof Map<?, ?> ) {
            for( var item : ( ( Map<?, ?> ) value ).values() )
                initDepsParameter( map, kernel, moduleItem, serviceName, item, false, serviceItem );
        }
    }

    private static void loadOnlyMainModuleAndDependsOn( ModuleTree map, Set<String> main, Set<String> profiles ) {
        var modules = map.clone();
        loadOnlyMainModuleAndDependsOn( modules, main, profiles, new LinkedHashSet<>() );

        for( var moduleItem : modules.values() ) {
            log.debug( "unload module {}", moduleItem.getName() );
            map.remove( moduleItem.getName() );
        }
    }

    private static void loadOnlyMainModuleAndDependsOn( ModuleTree modules, Set<String> main, Set<String> profiles, LinkedHashSet<String> loaded ) {
        for( var module : main ) {
            var moduleItem = modules.get( module );

            if( moduleItem == null && !loaded.contains( module ) )
                throw new ApplicationException( "main.boot: unknown module name '" + module + "'" );

            if( moduleItem != null ) {
                moduleItem.setLoad();
                loaded.add( moduleItem.getName() );

                modules.remove( module );

                var dependsOn = new LinkedHashSet<String>();
                for( var d : moduleItem.module.dependsOn ) {
                    if( KernelHelper.profileEnabled( d.profiles, profiles ) )
                        dependsOn.add( d.name );
                }

                loadOnlyMainModuleAndDependsOn( modules, dependsOn, profiles, loaded );
            }
        }
    }

    private static void validateRemoting( ModuleTree map ) throws ApplicationException {
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

    private static void removeDisabled( ModuleTree map ) {
        removeDisabledModules( map );
        removeDisabledServices( map );
    }

    private static void removeDisabledModules( ModuleTree map ) {
        map.values().removeIf( moduleInfo -> !moduleInfo.isEnabled() );
    }

    private static void removeDisabledServices( ModuleTree map ) {
        for( var moduleInfo : map.values() ) {
            moduleInfo.services.values().removeIf( serviceInfo -> !serviceInfo.enabled );
        }
    }

    private static void validateDeps( ModuleTree map ) throws ApplicationException {
        validateModuleDeps( map );
        validateServiceDeps( map );
    }

    private static void validateModuleDeps( ModuleTree map ) throws ApplicationException {
        for( var moduleInfo : map.values() ) {
            if( !moduleInfo.isEnabled() ) continue;

            for( var dModuleInfo : moduleInfo.getDependsOn().values() ) {
                if( !dModuleInfo.moduleItem.isEnabled() ) {
                    throw new ApplicationException( "[" + moduleInfo.module.name + ":*] dependencies are not enabled [" + dModuleInfo.moduleItem.module.name + "]" );
                }
            }
        }
    }

    private static void validateImplementation( ModuleTree map ) throws ApplicationException {
        for( var moduleInfo : map.values() ) {
            if( !moduleInfo.isEnabled() ) continue;

            for( var serviceInfo : moduleInfo.services.values() ) {
                if( !serviceInfo.enabled ) continue;

                if( serviceInfo.service.implementation == null )
                    throw new ApplicationException( "failed to initialize service: " + moduleInfo.module.name + ":" + serviceInfo.serviceName + ". implementation == null" );
            }
        }
    }

    private static void validateServiceDeps( ModuleTree map ) throws ApplicationException {
        for( var moduleInfo : map.values() ) {
            if( !moduleInfo.isEnabled() ) continue;

            for( var serviceInfo : moduleInfo.services.values() ) {
                if( !serviceInfo.enabled ) continue;

                for( var dServiceReference : serviceInfo.dependsOn ) {
                    if( !dServiceReference.serviceItem.enabled && dServiceReference.required ) {
                        throw new ApplicationException( "[" + moduleInfo.module.name + ":" + serviceInfo.service.name + "] dependencies are not enabled [" + dServiceReference.serviceItem.serviceName + "]" );
                    }
                }
            }
        }
    }

    private static void sort( ModuleTree map ) {
        sortModules( map );
        for( var moduleInfo : map.values() ) {
            sortServices( moduleInfo );
        }
    }

    private static void sortModules( ModuleTree map ) {
        log.trace( "modules: before sort: \n{}",
            String.join( "\n", Lists.map( map.entrySet(), e -> "  " + e.getKey() + ": " + e.getValue().getDependsOn() ) )
        );

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

        map.set( newMap );
        log.trace( "modules: after sort: \n{}",
            String.join( "\n", Lists.map( map.keySet(), e -> "  " + e ) )
        );
    }

    private static void sortServices( ModuleItem moduleInfo ) {
        log.trace( "{}: services: before sort: \n{}",
            moduleInfo.getName(),
            String.join( "\n", Lists.map( moduleInfo.services.entrySet(), e -> "  " + e.getKey() + ": " + Lists.map( e.getValue().dependsOn, d -> d.serviceItem.serviceName ) ) )
        );

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
                log.error( "  [{}]:{}", node.serviceName, Lists.map( node.dependsOn, d -> d.serviceItem.serviceName ) );
            }

            throw new ApplicationException( "[" + moduleInfo.module.name + "] graph has at least one cycle" );
        }

        moduleInfo.services.clear();
        moduleInfo.services.putAll( newMap );
        log.trace( "{}: services: after sort: \n{}",
            moduleInfo.getName(),
            String.join( "\n", Lists.map( moduleInfo.services.keySet(), e -> "  " + e ) )
        );
    }

    private static void fixServiceName( ModuleTree map ) {
        for( var module : map.values() ) {
            module.services.forEach( ( implName, serviceItem ) ->
                serviceItem.fixServiceName( implName ) );
        }
    }

    private static void initDeps( ModuleTree map, Set<String> profiles, Kernel kernel ) {
        initModuleDeps( map, profiles );
        initServicesDeps( map, kernel );
    }

    private static void initModuleDeps( ModuleTree map, Set<String> profiles ) {
        for( var moduleItem : map.values() ) {
            for( var d : moduleItem.module.dependsOn ) {
                ModuleItem dModule;
                if( KernelHelper.profileEnabled( d.profiles, profiles ) && ( dModule = map.findModule( moduleItem, d.name ) ).isEnabled() ) {
                    moduleItem.addDependsOn( new ModuleReference( dModule ) );
                } else {
                    log.trace( "[module#{}]: skip dependsOn {}", moduleItem.module.name, new LinkedHashSet<ModuleItem>() );
                }
            }
        }
    }

}
