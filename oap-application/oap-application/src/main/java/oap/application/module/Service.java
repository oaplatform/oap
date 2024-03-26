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

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import oap.application.remote.RemoteLocation;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;


@EqualsAndHashCode
@ToString
public class Service {
    public static final String PROFILE_ENABLED = "enabled";
    public static final String PROFILE_DISABLED = "disabled";

    public final LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
    public final Supervision supervision = new Supervision();
    public final LinkedHashSet<Object> dependsOn = new LinkedHashSet<>();
    @JsonAlias( { "profile", "profiles" } )
    public final LinkedHashSet<String> profiles = new LinkedHashSet<>();
    public final LinkedHashMap<String, String> listen = new LinkedHashMap<>();
    public final LinkedHashMap<String, String> link = new LinkedHashMap<>();
    public String implementation;
    public String name;
    public RemoteLocation remote;
    @JsonIgnore
    public LinkedHashMap<String, Object> ext = new LinkedHashMap<>();

    @JsonAnySetter
    public void putUnknown( String key, Object val ) {
        ext.put( key, KernelExtConfiguration.getInstance().deserializeService( key, val ) );
    }

    @JsonAnyGetter
    public Map<String, Object> getUnknown() {
        return ext;
    }

    @JsonIgnore
    public boolean isRemoteService() {
        return remote != null;
    }

    @SuppressWarnings( "unchecked" )
    public <T> T getExt( String ext ) {
        return ( T ) this.ext.get( ext );
    }
}
