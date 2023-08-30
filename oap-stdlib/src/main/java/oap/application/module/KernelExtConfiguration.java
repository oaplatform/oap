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

package oap.application.module;

import lombok.extern.slf4j.Slf4j;
import oap.application.ApplicationException;
import oap.application.Configuration;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
public final class KernelExtConfiguration extends Configuration<KernelExt> {
    private static KernelExtConfiguration instance;

    private Map<String, KernelExt.ItemExt> modules = new LinkedHashMap<>();
    private Map<String, KernelExt.ItemExt> services = new LinkedHashMap<>();

    private KernelExtConfiguration() {
        super( KernelExt.class, "oap-module-ext" );

        var urls = urlsFromClassPath();

        for( var url : urls ) {
            var config = fromUrl( url );
            log.debug( "kernel: url {} found and config {} loading", url, config );

            config.modules.forEach( ( name, item ) -> {
                if( modules.putIfAbsent( name, item ) != null ) {
                    throw new ApplicationException( "Duplicate ext module configuration " + url + "#" + name );
                }
            } );

            config.services.forEach( ( name, item ) -> {
                if( services.putIfAbsent( name, item ) != null ) {
                    log.warn( "Duplicate ext service configuration " + url + "#" + name
                        + ". Loaded services: " + config.services );
//                    throw new ApplicationException( "Duplicate ext service configuration " + url + "#" + name
//                        + ". Loaded services: " + config.services );
                }
            } );
        }
    }

    public static KernelExtConfiguration getInstance() {
        synchronized( KernelExtConfiguration.class ) {
            if( instance == null ) {
                instance = new KernelExtConfiguration();
            }
            return instance;
        }
    }

    public Object deserializeModule( String key, Object value ) {
        var item = modules.get( key );
        if( item == null ) return value;

        return item.deserialize( value );
    }

    public Object deserializeService( String key, Object value ) {
        var item = services.get( key );
        if( item == null ) return value;

        return item.deserialize( value );
    }
}
