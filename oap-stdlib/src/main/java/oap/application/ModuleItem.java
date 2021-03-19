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

import lombok.EqualsAndHashCode;
import oap.util.Lists;

import java.net.URL;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

/**
 * Created by igor.petrenko on 2021-02-15.
 */
class ModuleItem {
    final Module module;
    final LinkedHashMap<String, ServiceItem> services = new LinkedHashMap<>();
    private final boolean enabled;
    private final LinkedHashMap<String, ModuleReference> dependsOn;
    private final URL location;
    private boolean load = false;

    ModuleItem( Module module, URL location, boolean enabled, LinkedHashMap<String, ModuleReference> dependsOn ) {
        this.module = module;
        this.location = location;
        this.enabled = enabled;
        this.dependsOn = dependsOn;
    }

    final void setLoad() {
        load = true;
    }

    final boolean isEnabled() {
        return enabled && load;
    }

    final String getName() {
        return module.name;
    }

    public void addDependsOn( ModuleReference reference ) {
        dependsOn.compute( reference.moduleItem.getName(), ( name, ref ) -> {
            if( ref != null ) {
                ref.serviceLink.addAll( reference.serviceLink );
                return ref;
            }

            return reference;
        } );
    }

    @Override
    public boolean equals( Object o ) {
        if( this == o ) return true;
        if( o == null || getClass() != o.getClass() ) return false;

        var that = ( ModuleItem ) o;

        return module.name.equals( that.module.name );
    }

    @Override
    public int hashCode() {
        return module.name.hashCode();
    }

    final LinkedHashMap<String, ModuleReference> getDependsOn() {
        return dependsOn;
    }

    public URL getLocation() {
        return location;
    }

    @EqualsAndHashCode
    static class ModuleReference {
        final ModuleItem moduleItem;
        final LinkedHashSet<ServiceLink> serviceLink = new LinkedHashSet<>();

        ModuleReference( ModuleItem moduleItem ) {
            this.moduleItem = moduleItem;
        }

        ModuleReference( ModuleItem moduleItem, ServiceLink serviceLink ) {
            this.moduleItem = moduleItem;
            this.serviceLink.add( serviceLink );
        }

        @Override
        public String toString() {
            return "" + Lists.map( serviceLink, sl -> sl.from.moduleItem.getName() + ":" + sl.from.serviceName + "->" + sl.to.moduleItem.getName() + ":" + sl.to.serviceName );
        }

        @EqualsAndHashCode
        static class ServiceLink {
            final ServiceItem from;
            final ServiceItem to;

            ServiceLink( ServiceItem from, ServiceItem to ) {
                this.from = from;
                this.to = to;
            }
        }
    }

    static class ServiceItem {
        public final String serviceName;
        public final ModuleItem moduleItem;
        public final Module.Service service;
        public final boolean enabled;
        public final LinkedHashSet<ServiceReference> dependsOn = new LinkedHashSet<>();

        ServiceItem( String serviceName, ModuleItem moduleItem, Module.Service service, boolean enabled ) {
            this.serviceName = serviceName;
            this.moduleItem = moduleItem;
            this.service = service;
            this.enabled = enabled;
        }

        public void fixServiceName( String implName ) {
            service.name = service.name != null ? service.name : implName;
        }

        @Override
        public boolean equals( Object o ) {
            if( this == o ) return true;
            if( o == null || getClass() != o.getClass() ) return false;

            var that = ( ServiceItem ) o;

            if( !moduleItem.module.name.equals( that.moduleItem.module.name ) ) return false;
            return serviceName.equals( that.serviceName );
        }

        @Override
        public int hashCode() {
            int result = moduleItem.module.name.hashCode();
            result = 31 * result + serviceName.hashCode();
            return result;
        }

        public void addDependsOn( ServiceReference serviceReference ) {
            var found = Lists.find2( dependsOn, d -> d.equals( serviceReference ) );
            if( found == null || found.required ) {
                if( found != null ) dependsOn.remove( found );
                dependsOn.add( serviceReference );
            }
        }

        static class ServiceReference {
            public final ServiceItem serviceItem;
            public final boolean required;

            ServiceReference( ServiceItem serviceItem, boolean required ) {
                this.serviceItem = serviceItem;
                this.required = required;
            }

            @Override
            public boolean equals( Object o ) {
                if( this == o ) return true;
                if( o == null || getClass() != o.getClass() ) return false;

                var that = ( ServiceReference ) o;

                return serviceItem.service.name.equals( that.serviceItem.service.name );
            }

            @Override
            public int hashCode() {
                return serviceItem.service.name.hashCode();
            }
        }
    }
}
