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
import oap.application.ModuleItem.ServiceItem.ServiceReference;
import oap.application.module.Reference;
import oap.util.Lists;
import oap.util.Pair;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static oap.util.Pair.__;


@Slf4j
class ModuleHelper {
    public static final Pattern MODULE_SERVICE_NAME_PATTERN = Pattern.compile( "^[A-Za-z\\-_0-9]++$" );

    private ModuleHelper() {
    }

    private static ModuleItemTree init( LinkedHashSet<Kernel.ModuleWithLocation> modules, LinkedHashSet<String> profiles ) {
        var map = initModules( modules, profiles );
        initServices( map, profiles );

        return map;
    }

    public static ModuleItemTree init( LinkedHashSet<Kernel.ModuleWithLocation> modules,
                                       LinkedHashSet<String> profiles,
                                       LinkedHashSet<String> main,
                                       Kernel kernel ) throws ApplicationException {
        log.trace( "Init modules: {}, profiles: {}, main: {}", modules, profiles, main );
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

    private static void validateServiceName( ModuleItemTree map ) throws ApplicationException {
        for( var moduleInfo : map.values() ) {
            for( var serviceName : moduleInfo.services.keySet() ) {
                if( !MODULE_SERVICE_NAME_PATTERN.matcher( serviceName ).matches() ) {
                    throw new ApplicationException( "service name " + serviceName + " does not match specified regex " + MODULE_SERVICE_NAME_PATTERN.pattern() );
                }
            }
        }
    }

    private static void validateModuleName( ModuleItemTree map ) throws ApplicationException {
        for( var moduleName : map.keySet() ) {
            if( !MODULE_SERVICE_NAME_PATTERN.matcher( moduleName ).matches() ) {
                throw new ApplicationException( "module name " + moduleName + " does not match specified regex " + MODULE_SERVICE_NAME_PATTERN.pattern() );
            }
        }
    }

    @SuppressWarnings( "checkstyle:ParameterAssignment" )
    private static Pair<ModuleItem, ModuleItem.ServiceItem> findService( ModuleItemTree map, String thisModuleName, String moduleName, String serviceName ) {
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

        var enabled = Lists.find2( found, f -> f._1.isEnabled() && f._2.enabled == ServiceEnabledStatus.ENABLED );
        if( enabled != null ) return enabled;

        return Lists.head2( found );
    }

    private static ModuleItemTree initModules( LinkedHashSet<Kernel.ModuleWithLocation> modules, LinkedHashSet<String> profiles ) {
        var map = new ModuleItemTree();

        for( var module : modules ) {
            var enabled = ServiceEnabledStatus.ENABLED;
            if( !KernelHelper.isModuleEnabled( module.module, profiles ) ) {
                log.debug( "skipping module {} with profiles {}", module.module.name, module.module.profiles );
                enabled = ServiceEnabledStatus.DISABLED_BY_PROFILE;
            }

            ModuleItem moduleItem = new ModuleItem( module.module, module.location, enabled, new LinkedHashMap<>() );
            map.put( module.module.name, moduleItem );
        }

        return map;
    }

    private static void initServices( ModuleItemTree map, LinkedHashSet<String> profiles ) {
        for( var moduleInfo : map.values() ) {
            for( var serviceEntry : moduleInfo.module.services.entrySet() ) {
                var serviceName = serviceEntry.getKey();
                var service = serviceEntry.getValue();
                var enabled = ServiceEnabledStatus.ENABLED;

                if( !KernelHelper.isServiceEnabled( service, profiles ) ) {
                    log.debug( "skipping service {}:{} with profiles {}", moduleInfo.module.name, serviceName, service.profiles );
                    enabled = ServiceEnabledStatus.DISABLED_BY_PROFILE;
                }

                if( !service.enabled ) {
                    log.debug( "skipping service {}:{}, reason: enabled = false", moduleInfo.module.name, serviceName );
                    enabled = ServiceEnabledStatus.DISABLED_BY_FLAG;
                }

                moduleInfo.services.put( serviceName, new ModuleItem.ServiceItem( serviceName, moduleInfo, service, enabled ) );
            }
        }
    }

    private static void initServicesDeps( ModuleItemTree map, Kernel kernel ) {
        for( var moduleItem : map.values() ) {
            if( !moduleItem.isEnabled() ) continue;

            moduleItem.services.forEach( ( serviceName, serviceItem ) -> {
                if( !serviceItem.isEnabled() ) return;

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
                    } else throw new ApplicationException( "Unknown deps format " + dService );

                    var moduleService = findService( map, moduleItem.getName(), dModuleName, dServiceName );
                    if( moduleService == null ) {
                        throw new ApplicationException( "[" + dModuleName + ":" + dServiceName + "] 'this:" + dService + "' not found" );
                    }

                    serviceItem.addDependsOn( new ServiceReference( moduleService._2, true ) );
                }

                for( var link : serviceItem.service.link.values() )
                    initDepsParameter( map, kernel, moduleItem, serviceName, link, true, serviceItem, true );

                for( var value : serviceItem.service.parameters.values() ) {
                    initDepsParameter( map, kernel, moduleItem, serviceName, value, true, serviceItem, false );
                }
            } );
        }
    }

    private static void initDepsParameter( ModuleItemTree map,
                                           Kernel kernel,
                                           ModuleItem moduleItem, String serviceName,
                                           Object value, boolean required,
                                           ModuleItem.ServiceItem serviceItem,
                                           boolean reverse ) {
        if( ServiceKernelCommand.INSTANCE.matches( value ) ) {
            var reference = ServiceKernelCommand.INSTANCE.reference( ( String ) value, moduleItem );
            var moduleService = findService( map, moduleItem.getName(), reference.module, reference.service );
            if( moduleService == null ) {
                throw new ApplicationException( "[" + moduleItem.module.name + ":" + serviceName + "#" + reference + "] " + reference + "  not found" );
            }

            if( !reverse )
                serviceItem.addDependsOn( new ServiceReference( moduleService._2, required ) );
            else
                moduleService._2.addDependsOn( new ServiceReference( serviceItem, required ) );
        } else if( value instanceof List<?> )
            for( var item : ( List<?> ) value )
                initDepsParameter( map, kernel, moduleItem, serviceName, item, false, serviceItem, reverse );
        else if( value instanceof Map<?, ?> ) {
            for( var item : ( ( Map<?, ?> ) value ).values() )
                initDepsParameter( map, kernel, moduleItem, serviceName, item, false, serviceItem, reverse );
        }
    }

    private static void loadOnlyMainModuleAndDependsOn( ModuleItemTree map, LinkedHashSet<String> main, LinkedHashSet<String> profiles ) {
        var modules = map.clone();
        log.info( "loading main modules: {} with profiles: {}", main, profiles );
        loadOnlyMainModuleAndDependsOn( modules, main, profiles, new LinkedHashSet<>() );

        for( var moduleItem : modules.values() ) {
            log.debug( "unload module {}", moduleItem.getName() );
            map.remove( moduleItem.getName() );
        }
    }

    private static void loadOnlyMainModuleAndDependsOn( ModuleItemTree modules,
                                                        final LinkedHashSet<String> main,
                                                        final LinkedHashSet<String> profiles,
                                                        final LinkedHashSet<String> loaded ) {
        for( var module : main ) {
            var moduleItem = modules.get( module );

            if( moduleItem == null && !loaded.contains( module ) )
                throw new ApplicationException( "main.boot: unknown module name '" + module + "', already loaded: " + loaded );

            if( moduleItem != null ) {
                log.trace( "Loading module: {}, already loaded: {}", moduleItem.getName(), loaded );
                moduleItem.setLoad();
                loaded.add( moduleItem.getName() );

                modules.remove( module );

                var dependsOn = new LinkedHashSet<String>();
                for( var depends : moduleItem.module.dependsOn ) {
                    if( KernelHelper.profileEnabled( depends.profiles, profiles ) ) {
                        log.trace( "dependant module {} enabled for module {}", depends.name, module );
                        dependsOn.add( depends.name );
                    } else {
                        log.trace( "dependant module {} disabled for module {}", depends.name, module );
                    }
                }
                loadOnlyMainModuleAndDependsOn( modules, dependsOn, profiles, loaded );
            }
        }
    }

    private static void validateRemoting( ModuleItemTree map ) throws ApplicationException {
        var invalidRemoting = new ArrayList<String>();

        for( var moduleItem : map.values() ) {
            for( var serviceItem : moduleItem.services.values() ) {
                if( !serviceItem.service.isRemoteService() ) continue;

                if( serviceItem.service.remote.url == null )
                    invalidRemoting.add( moduleItem.getName() + ":" + serviceItem.serviceName );
            }
        }

        if( !invalidRemoting.isEmpty() ) {
            log.error( "url == null, services " + invalidRemoting );
            throw new ApplicationException( "remoting: url == null, services " + invalidRemoting );
        }
    }

    private static void removeDisabled( ModuleItemTree map ) {
        removeDisabledModules( map );
        removeDisabledServices( map );
    }

    private static void removeDisabledModules( ModuleItemTree map ) {
        map.values().removeIf( moduleInfo -> !moduleInfo.isEnabled() );
    }

    private static void removeDisabledServices( ModuleItemTree map ) {
        for( var moduleInfo : map.values() ) {
            moduleInfo.services.values().removeIf( serviceInfo -> !serviceInfo.isEnabled() );
        }
    }

    private static void validateDeps( ModuleItemTree map ) throws ApplicationException {
        validateModuleDeps( map );
        validateServiceDeps( map );
    }

    private static void validateModuleDeps( ModuleItemTree map ) throws ApplicationException {
        for( var moduleInfo : map.values() ) {
            if( !moduleInfo.isEnabled() ) continue;

            for( var dModuleInfo : moduleInfo.getDependsOn().values() ) {
                if( !dModuleInfo.moduleItem.isEnabled() ) {
                    throw new ApplicationException( "[" + moduleInfo.module.name + ":*] dependencies are not enabled."
                            + " [" + dModuleInfo.moduleItem.module.name + "] is disabled by "
                            + dModuleInfo.moduleItem.getEnabled().toString() + "." );
                }
            }
        }
    }

    private static void validateImplementation( ModuleItemTree map ) throws ApplicationException {
        for( var moduleInfo : map.values() ) {
            if( !moduleInfo.isEnabled() ) continue;

            for( var serviceInfo : moduleInfo.services.values() ) {
                if( !serviceInfo.isEnabled() ) continue;

                if( serviceInfo.service.implementation == null )
                    throw new ApplicationException( "failed to initialize service: " + moduleInfo.module.name + ":" + serviceInfo.serviceName + ". implementation == null" );
            }
        }
    }

    private static void validateServiceDeps( ModuleItemTree map ) throws ApplicationException {
        for( var moduleInfo : map.values() ) {
            if( !moduleInfo.isEnabled() ) continue;

            for( var serviceInfo : moduleInfo.services.values() ) {
                if( !serviceInfo.isEnabled() ) continue;

                for( var dServiceReference : serviceInfo.dependsOn ) {
                    if( !dServiceReference.serviceItem.isEnabled() && dServiceReference.required ) {
                        throw new ApplicationException( "[" + moduleInfo.module.name + ":" + serviceInfo.service.name + "] dependencies are not enabled. Required service [" + dServiceReference.serviceItem.serviceName + "] is disabled by "
                                + dServiceReference.serviceItem.enabled.toString() + "." );
                    }
                }
            }
        }
    }

    private static void sort( ModuleItemTree map ) {
        sortModules( map );
        sortServices( map );
    }

    private static void sortModules( ModuleItemTree map ) {
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
            log.error( "cyclic dependency detected:" );
            for( var node : graph ) {
                log.error( "  module: {} dependsOn {}", node.module.name, Lists.map( node.getDependsOn().values(), d -> d.moduleItem.module.name ) );
            }

            throw new ApplicationException( "cyclic dependency detected" );
        }

        map.set( newMap );
        log.trace( "modules after sort: \n{}",
            String.join( "\n", Lists.map( map.keySet(), e -> "  " + e ) )
        );
    }

    private static void sortServices( ModuleItemTree map ) {
        var graph = new LinkedList<>( map.services );

        var newMap = new LinkedHashMap<Reference, ModuleItem.ServiceItem>();
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

            newMap.put( new Reference( serviceItem.getModuleName(), serviceItem.serviceName ), serviceItem );

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
            log.error( "services cyclic dependency detected:" );
            for( var node : graph ) {
                log.error( "  {}.{} dependsOn {}", node.getModuleName(), node.serviceName,
                    Lists.map( node.dependsOn, d -> d.serviceItem.getModuleName() + "." + d.serviceItem.serviceName ) );
            }

            throw new ApplicationException( "services cyclic dependency detected" );
        }

        map.setServices( newMap.values() );
        log.trace( "services after sort: \n{}",
            String.join( "\n", Lists.map( map.services, e -> "  " + e ) )
        );
    }

    private static void fixServiceName( ModuleItemTree map ) {
        for( var module : map.values() ) {
            module.services.forEach( ( implName, serviceItem ) ->
                serviceItem.fixServiceName( implName ) );
        }
    }

    private static void initDeps( ModuleItemTree map, LinkedHashSet<String> profiles, Kernel kernel ) {
        initModuleDeps( map, profiles );
        initServicesDeps( map, kernel );
    }

    private static void initModuleDeps( ModuleItemTree map, LinkedHashSet<String> profiles ) {
        for( var moduleItem : map.values() ) {
            for( var d : moduleItem.module.dependsOn ) {
                if ( !KernelHelper.profileEnabled( d.profiles, profiles ) ) {
                    log.trace( "[module#{}]: skip dependsOn {}, module profiles are not enabled", moduleItem.module.name, new LinkedHashSet<ModuleItem>() );
                    continue;
                }
                ModuleItem dModule  = map.findModule( moduleItem, d.name );
                if( !dModule.isEnabled() ) {
                    log.trace( "[module#{}]: skip dependsOn {}, module is not enabled", moduleItem.module.name, new LinkedHashSet<ModuleItem>() );
                    continue;
                }
                moduleItem.addDependsOn( new ModuleReference( dModule ) );
            }
        }
    }

}
