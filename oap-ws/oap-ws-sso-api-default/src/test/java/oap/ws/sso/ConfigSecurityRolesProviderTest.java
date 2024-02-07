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

package oap.ws.sso;

import org.testng.annotations.Test;

import java.util.Map;
import java.util.Set;

import static oap.ws.sso.ConfigSecurityRolesProviderTest.Permissions.MANAGE;
import static oap.ws.sso.ConfigSecurityRolesProviderTest.Permissions.MEGATEST;
import static org.assertj.core.api.Assertions.assertThat;

public class ConfigSecurityRolesProviderTest {
    @Test
    public void merge() {
        ConfigSecurityRolesProvider provider = new ConfigSecurityRolesProvider();
        assertThat( provider.roles() ).containsOnly( "ADMIN", "MEGADMIN", "USER" );
        assertThat( provider.permissionsOf( "ADMIN" ) ).containsOnly( MEGATEST, MANAGE );
        assertThat( provider.permissionsOf( "USER" ) ).containsOnly( MANAGE );
        assertThat( provider.permissionsOf( "MEGADMIN" ) ).containsOnly( MEGATEST, MANAGE );
    }

    @Test
    public void granted() {
        ConfigSecurityRolesProvider provider = new ConfigSecurityRolesProvider( new ConfigSecurityRolesProvider.Config( Map.of( "USER", Set.of( "A", "B" ) ) ) );
        assertThat( provider.granted( "VISITOR", "A" ) ).isFalse();
        assertThat( provider.granted( "USER", "A" ) ).isTrue();
        assertThat( provider.granted( "USER", "C" ) ).isFalse();
        assertThat( provider.granted( "USER", "A", "C" ) ).isTrue();
        assertThat( provider.granted( "USER", "A", "B" ) ).isTrue();
    }

    @SuppressWarnings( "checkstyle:InterfaceIsType" )
    public interface Permissions {
        String MEGATEST = "generic:megatest";
        String MANAGE = "generic:manage";
    }
}
