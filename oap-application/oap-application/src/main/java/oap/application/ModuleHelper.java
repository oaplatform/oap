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
import oap.application.module.Service;
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

    private static void init( ModuleItemTree map, LinkedHashMap<String, Kernel.ModuleWithLocation> modules ) {
        initModules( map, modules );
        initServices( map );
    }

    public static void init( ModuleItemTree map,
                             LinkedHashMap<String, Kernel.ModuleWithLocation> modules,
                             LinkedHashMap<Reference, Reference> implementations,
                             LinkedHashSet<String> main,
                             boolean allowActiveByDefault ) throws ApplicationException {
        log.trace( "Init modules {} main {} allowActiveByDefault {}", modules, main, allowActiveByDefault );

        init( map, modules );
        loadOnlyMainModuleAndDependsOn( map, main, allowActiveByDefault );

        validateModuleName( map );
        validateServiceName( map );
        validateServiceImplementation( map );
        validateImplementations( map, implementations );

        initModuleDeps( map );
        initServicesDeps( map );
        initImplementationsDeps( map, implementations );

        validateModuleDeps( map );
        validateServiceDeps( map );

        sortModules( map );
        sortServices( map );

        removeDisabledModules( map );
        removeDisabledServices( map );

        validateServices( map );
    }

    private static void validateServiceName( ModuleItemTree map ) throws ApplicationException {
        for( ModuleItem moduleInfo : map.values() ) {
            for( String serviceName : moduleInfo.services.keySet() ) {
                if( !MODULE_SERVICE_NAME_PATTERN.matcher( serviceName ).matches() ) {
                    throw new ApplicationException( "service name " + serviceName + " does not match specified regex " + MODULE_SERVICE_NAME_PATTERN.pattern() );
                }
            }
        }
    }

    private static void validateModuleName( ModuleItemTree map ) throws ApplicationException {
        for( String moduleName : map.keySet() ) {
            if( !MODULE_SERVICE_NAME_PATTERN.matcher( moduleName ).matches() ) {
                throw new ApplicationException( "module name " + moduleName + " does not match specified regex " + MODULE_SERVICE_NAME_PATTERN.pattern() );
            }
        }
    }

    @SuppressWarnings( "checkstyle:ParameterAssignment" )
    private static Pair<ModuleItem, ModuleItem.ServiceItem> findService( ModuleItemTree map, String thisModuleName, String moduleName, String serviceName ) {
        var found = new ArrayList<Pair<ModuleItem, ModuleItem.ServiceItem>>();

        for( ModuleItem moduleInfo : map.values() ) {
            if( KernelHelper.THIS.contains( moduleName ) ) moduleName = thisModuleName;

            if( !moduleInfo.getName().equals( moduleName ) ) continue;

            for( Map.Entry<String, ModuleItem.ServiceItem> entry : moduleInfo.services.entrySet() ) {
                if( serviceName.equals( entry.getValue().serviceName ) || serviceName.equals( entry.getValue().serviceName ) ) {
                    found.add( __( moduleInfo, entry.getValue() ) );
                }
            }
        }

        if( found.isEmpty() ) return null;

        var enabled = Lists.find2( found, f -> f._1.isEnabled() && f._2.enabled == ServiceEnabledStatus.ENABLED );
        if( enabled != null ) return enabled;

        return Lists.head2( found );
    }

    private static void initModules( ModuleItemTree map, LinkedHashMap<String, Kernel.ModuleWithLocation> modules ) {
        for( Kernel.ModuleWithLocation module : modules.values() ) {
            ModuleItem moduleItem = new ModuleItem( module.module, module.location, new LinkedHashMap<>() );
            map.put( module.module.name, moduleItem );
        }
    }

    private static void initServices( ModuleItemTree map ) {
        for( ModuleItem moduleInfo : map.values() ) {
            for( Map.Entry<String, Service> serviceEntry : moduleInfo.module.services.entrySet() ) {
                String serviceName = serviceEntry.getKey();
                Service service = serviceEntry.getValue();
                ServiceEnabledStatus enabled = ServiceEnabledStatus.ENABLED;

                if( !service.enabled ) {
                    log.debug( "skipping service {}:{}, reason: enabled = false", moduleInfo.module.name, serviceName );
                    enabled = ServiceEnabledStatus.DISABLED_BY_FLAG;
                }

                moduleInfo.services.put( serviceName, new ModuleItem.ServiceItem( serviceName, moduleInfo, service, enabled ) );
            }
        }
    }

    private static void initServicesDeps( ModuleItemTree map ) {
        for( ModuleItem moduleItem : map.values() ) {
            if( !moduleItem.isEnabled() ) continue;

            moduleItem.services.forEach( ( serviceName, serviceItem ) -> {
                if( !serviceItem.isEnabled() ) return;

                for( var dService : serviceItem.service.dependsOn ) {
                    String dModuleName;
                    String dServiceName;
                    if( ServiceKernelCommand.INSTANCE.matches( dService ) ) {
                        Reference ref = ServiceKernelCommand.INSTANCE.reference( dService, moduleItem );
                        dModuleName = ref.module;
                        dServiceName = ref.service;
                    } else {
                        dModuleName = "this";
                        dServiceName = dService;
                    }

                    Pair<ModuleItem, ModuleItem.ServiceItem> moduleService = findService( map, moduleItem.getName(), dModuleName, dServiceName );
                    if( moduleService == null ) {
                        throw new ApplicationException( "[" + dModuleName + ":" + dServiceName + "] 'this:" + dService + "' not found" );
                    }

                    serviceItem.addDependsOn( new ServiceReference( moduleService._2, true ) );
                }

                for( String link : serviceItem.service.link.values() )
                    initDepsParameter( map, moduleItem, serviceName, link, true, serviceItem, true );

                for( Object value : serviceItem.service.parameters.values() ) {
                    initDepsParameter( map, moduleItem, serviceName, value, true, serviceItem, false );
                }
            } );
        }
    }

    private static void initImplementationsDeps( ModuleItemTree map, LinkedHashMap<Reference, Reference> implementations ) {
        for( Map.Entry<Reference, Reference> implEntry : implementations.entrySet() ) {
            Reference interfaceReference = implEntry.getKey();
            Reference implementationReference = implEntry.getValue();

            ModuleItem interfaceModule = map.get( interfaceReference.module );
            ModuleItem implementationModule = map.get( implementationReference.module );

            ModuleItem.ServiceItem interfaceService = interfaceModule.services.get( interfaceReference.service );
            ModuleItem.ServiceItem implementationService = implementationModule.services.get( implementationReference.service );

            interfaceService.addDependsOn( new ServiceReference( implementationService, true ) );
        }
    }

    private static void initDepsParameter( ModuleItemTree map,
                                           ModuleItem moduleItem, String serviceName,
                                           Object value, boolean required,
                                           ModuleItem.ServiceItem serviceItem,
                                           boolean reverse ) {
        if( ServiceKernelCommand.INSTANCE.matches( value ) ) {
            Reference reference = ServiceKernelCommand.INSTANCE.reference( ( String ) value, moduleItem );
            Pair<ModuleItem, ModuleItem.ServiceItem> moduleService = findService( map, moduleItem.getName(), reference.module, reference.service );
            if( moduleService == null ) {
                throw new ApplicationException( "[" + moduleItem.module.name + ":" + serviceName + "#" + reference + "] " + reference + "  not found" );
            }

            if( !reverse )
                serviceItem.addDependsOn( new ServiceReference( moduleService._2, required ) );
            else
                moduleService._2.addDependsOn( new ServiceReference( serviceItem, required ) );
        } else if( value instanceof List<?> )
            for( Object item : ( List<?> ) value )
                initDepsParameter( map, moduleItem, serviceName, item, false, serviceItem, reverse );
        else if( value instanceof Map<?, ?> ) {
            for( Object item : ( ( Map<?, ?> ) value ).values() )
                initDepsParameter( map, moduleItem, serviceName, item, false, serviceItem, reverse );
        }
    }

    private static void loadOnlyMainModuleAndDependsOn( ModuleItemTree map, LinkedHashSet<String> main,
                                                        boolean allowActiveByDefault ) {
        ModuleItemTree modules = map.clone();
        log.info( "loading main modules {}", main );
        loadOnlyMainModuleAndDependsOn( modules, main, allowActiveByDefault, new LinkedHashSet<>() );

        for( ModuleItem moduleItem : modules.values() ) {
            log.debug( "unload module {}", moduleItem.getName() );
            map.remove( moduleItem.getName() );
        }
    }

    private static void loadOnlyMainModuleAndDependsOn( ModuleItemTree modules,
                                                        final LinkedHashSet<String> main,
                                                        boolean allowActiveByDefault,
                                                        final LinkedHashSet<String> loaded ) {

        var mainWithAllowActiveByDefault = new LinkedHashSet<>( main );
        if( allowActiveByDefault ) {
            for( String moduleName : modules.keySet() ) {
                ModuleItem moduleItem = modules.get( moduleName );
                if( moduleItem.module.activation.activeByDefault ) {
                    mainWithAllowActiveByDefault.add( moduleName );
                }
            }
        }

        for( String module : mainWithAllowActiveByDefault ) {
            ModuleItem moduleItem = modules.get( module );

            if( moduleItem == null && !loaded.contains( module ) ) {
                throw new ApplicationException( "main.boot: unknown module name '" + module + "', already loaded: " + loaded );
            }

            if( moduleItem != null ) {
                log.trace( "Loading module: {}, already loaded: {}", moduleItem.getName(), loaded );
                moduleItem.setLoad();
                loaded.add( moduleItem.getName() );

                modules.remove( module );

                loadOnlyMainModuleAndDependsOn( modules, moduleItem.module.dependsOn, allowActiveByDefault, loaded );
            }
        }
    }

    private static void validateServices( ModuleItemTree map ) throws ApplicationException {
        var errors = new ArrayList<String>();

        for( ModuleItem moduleItem : map.values() ) {
            for( ModuleItem.ServiceItem serviceItem : moduleItem.services.values() ) {
                for( Object ext : serviceItem.service.ext.values() ) {
                    if( ext instanceof ServiceKernelListener skl ) {
                        errors.addAll( skl.validate( serviceItem ) );
                    }
                }
            }
        }

        if( !errors.isEmpty() ) {
            for( String message : errors ) {
                log.error( message );
            }

            throw new ApplicationException( "error: " + errors );
        }
    }

    private static void removeDisabledModules( ModuleItemTree map ) {
        map.values().removeIf( moduleInfo -> !moduleInfo.isEnabled() );
    }

    private static void removeDisabledServices( ModuleItemTree map ) {
        for( ModuleItem moduleInfo : map.values() ) {
            moduleInfo.services.values().removeIf( serviceInfo -> !serviceInfo.isEnabled() );
        }
    }

    private static void validateModuleDeps( ModuleItemTree map ) throws ApplicationException {
        for( ModuleItem moduleInfo : map.values() ) {
            if( !moduleInfo.isEnabled() ) continue;

            for( ModuleReference dModuleInfo : moduleInfo.getDependsOn().values() ) {
                if( !dModuleInfo.moduleItem.isEnabled() ) {
                    throw new ApplicationException( "[" + moduleInfo.module.name + ":*] dependencies are not enabled." );
                }
            }
        }
    }

    private static void validateServiceImplementation( ModuleItemTree map ) throws ApplicationException {
        for( ModuleItem moduleInfo : map.values() ) {
            if( !moduleInfo.isEnabled() ) continue;

            for( ModuleItem.ServiceItem serviceInfo : moduleInfo.services.values() ) {
                if( !serviceInfo.isEnabled() ) continue;

                if( serviceInfo.service.implementation == null )
                    throw new ApplicationException( "failed to initialize service: " + moduleInfo.module.name + ":" + serviceInfo.serviceName + ". implementation == null" );
            }
        }
    }

    private static void validateImplementations( ModuleItemTree map, LinkedHashMap<Reference, Reference> implementations ) throws ApplicationException {
        for( Reference implementationReference : implementations.values() ) {
            ModuleItem implementationModule = map.get( implementationReference.module );

            if( implementationModule == null ) {
                throw new ApplicationException( "Unknown module " + implementationReference.module + " in reference <modules." + implementationReference + ">" );
            }

            ModuleItem.ServiceItem implementationService = implementationModule.services.get( implementationReference.service );

            if( implementationService == null ) {
                throw new ApplicationException( "Unknown service " + implementationReference.service + " in reference <modules." + implementationReference + ">" );
            }
        }
    }

    private static void validateServiceDeps( ModuleItemTree map ) throws ApplicationException {
        for( ModuleItem moduleInfo : map.values() ) {
            if( !moduleInfo.isEnabled() ) continue;

            for( ModuleItem.ServiceItem serviceInfo : moduleInfo.services.values() ) {
                if( !serviceInfo.isEnabled() ) continue;

                for( ServiceReference dServiceReference : serviceInfo.dependsOn ) {
                    if( !dServiceReference.serviceItem.isEnabled() && dServiceReference.required ) {
                        throw new ApplicationException( "[" + moduleInfo.module.name + ":" + serviceInfo.serviceName + "] dependencies are not enabled. Required service [" + dServiceReference.serviceItem.serviceName + "] is disabled by "
                            + dServiceReference.serviceItem.enabled.toString() + "." );
                    }
                }
            }
        }
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
            ModuleItem moduleItem = noIncomingEdges.removeFirst();

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
            ModuleItem.ServiceItem serviceItem = noIncomingEdges.removeFirst();

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
            for( ModuleItem.ServiceItem node : graph ) {
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

    private static void initModuleDeps( ModuleItemTree map ) {
        for( ModuleItem moduleItem : map.values() ) {
            for( String d : moduleItem.module.dependsOn ) {
                ModuleItem dModule = map.findModule( moduleItem, d );
                if( !dModule.isEnabled() ) {
                    log.trace( "[module#{}]: skip dependsOn {}, module is not enabled", moduleItem.getName(), new LinkedHashSet<ModuleItem>() );
                    continue;
                }
                moduleItem.addDependsOn( new ModuleReference( dModule ) );
            }
        }
    }

}
