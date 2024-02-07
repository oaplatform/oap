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

import java.util.Map;
import java.util.Set;

public class AbstractSecurityRolesProvider implements SecurityRolesProvider {
    protected final Map<String, Set<String>> roles;

    protected AbstractSecurityRolesProvider( Map<String, Set<String>> roles ) {
        this.roles = roles;
    }

    public Set<String> permissionsOf( String role ) {
        return roles.getOrDefault( role, Set.of() );
    }

    public boolean granted( String role, String... permissions ) {
        Set<String> granted = permissionsOf( role );
        for( String permission : permissions ) {
            if( granted.contains( permission ) ) return true;
        }
        return false;
    }

    public Set<String> roles() {
        return roles.keySet();
    }
}
