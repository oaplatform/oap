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
import org.testng.annotations.Test;

import java.net.URL;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class ModuleConfigurationTest {
    @Test
    public void testIsAdditionalModuleFileName() {
        assertThat( ModuleConfiguration.isAdditionalModuleFileName( "oap-module-services.oap" ) ).isTrue();
        assertThat( ModuleConfiguration.isAdditionalModuleFileName( "oap-module.oap" ) ).isFalse();
        assertThat( ModuleConfiguration.isAdditionalModuleFileName( "oap-module-ext.oap" ) ).isFalse();
        assertThat( ModuleConfiguration.isAdditionalModuleFileName( "oap-module-ext.conf" ) ).isFalse();
        assertThat( ModuleConfiguration.isAdditionalModuleFileName( "oap-module-services.conf" ) ).isFalse();
    }

    @Test
    public void testUrlsFromClassPathContainsAdditionalModules() {
        List<String> urls = Module.CONFIGURATION.urlsFromClassPath().stream().map( URL::toString ).toList();

        assertThat( urls )
            .anyMatch( u -> u.endsWith( "META-INF/oap-module-multi.oap" ) )
            .anyMatch( u -> u.endsWith( "META-INF/oap-module-multi-services.oap" ) )
            .noneMatch( u -> u.contains( "oap-module-ext" ) )
            .doesNotHaveDuplicates();
    }

    @Test
    public void testMultipleModulesInOneJavaModule() {
        try( Kernel kernel = new Kernel( Module.CONFIGURATION.urlsFromClassPath() ) ) {
            kernel.start( Map.of( "boot.main", "test-multi-services" ) );

            ServiceOne one = kernel.<ServiceOne>service( "test-multi-base", "one" ).orElseThrow();
            ServiceTwo two = kernel.<ServiceTwo>service( "test-multi-services", "two" ).orElseThrow();

            assertThat( two.one2 ).isSameAs( one );
        }
    }
}
