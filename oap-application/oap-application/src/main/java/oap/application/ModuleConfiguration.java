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

import oap.application.module.Module;
import oap.io.Resources;

import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Module declarations are discovered in {@code META-INF/}:
 * <ul>
 *     <li>{@code oap-module.(json|conf|yaml|yml|oap)} - main module declaration</li>
 *     <li>{@code oap-module-<suffix>.oap} - additional module declarations, each one is a separate module</li>
 * </ul>
 * {@code oap-module-ext.oap} is reserved by {@link oap.application.module.KernelExtConfiguration}.
 */
public class ModuleConfiguration extends Configuration<Module> {
    private static final Pattern ADDITIONAL_MODULE_FILE = Pattern.compile( "oap-module-[^/]+\\.oap" );
    private static final String RESERVED_EXT_FILE = "oap-module-ext.oap";

    public ModuleConfiguration() {
        super( Module.class, "oap-module" );
    }

    /**
     * @param fileName file name without directory, for example {@code oap-module-services.oap}
     */
    public static boolean isAdditionalModuleFileName( String fileName ) {
        return !RESERVED_EXT_FILE.equals( fileName ) && ADDITIONAL_MODULE_FILE.matcher( fileName ).matches();
    }

    @Override
    public List<URL> urlsFromClassPath() {
        var ret = new ArrayList<>( super.urlsFromClassPath() );

        var known = new LinkedHashSet<String>();
        for( var url : ret ) known.add( url.toString() );

        for( var url : Resources.urls( ModuleConfiguration::isAdditionalModuleResource ) ) {
            if( known.add( url.toString() ) ) ret.add( url );
        }

        return ret;
    }

    private static boolean isAdditionalModuleResource( String resourceName ) {
        return resourceName.startsWith( META_INF )
            && resourceName.indexOf( '/', META_INF.length() ) < 0
            && isAdditionalModuleFileName( resourceName.substring( META_INF.length() ) );
    }
}
