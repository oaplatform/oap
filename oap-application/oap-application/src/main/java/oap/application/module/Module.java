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
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import oap.application.ModuleConfiguration;
import oap.application.ModuleDependsDeserializer;
import oap.reflect.Coercions;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

@EqualsAndHashCode
@ToString
public class Module {
    public static final ModuleConfiguration CONFIGURATION = new ModuleConfiguration();
    public static final Coercions coersions = Coercions.basic().withStringToObject().withIdentity();

    @JsonDeserialize( contentUsing = ModuleDependsDeserializer.class )
    public final LinkedHashSet<Depends> dependsOn = new LinkedHashSet<>();
    @JsonAlias( { "service", "services" } )
    public final LinkedHashMap<String, Service> services = new LinkedHashMap<>();
    @JsonAlias( { "profile", "profiles" } )
    public final LinkedHashSet<String> profiles = new LinkedHashSet<>();
    public String name;
    @JsonIgnore
    public LinkedHashMap<String, Object> ext = new LinkedHashMap<>();

    public final ModuleActivation activation = new ModuleActivation();

    @JsonCreator
    public Module( String name ) {
        this.name = name;
    }

    @JsonAnySetter
    public void putUnknown( String key, Object val ) {
        ext.put( key, KernelExtConfiguration.getInstance().deserializeModule( key, val ) );
    }

    @JsonAnyGetter
    public Map<String, Object> getUnknown() {
        return ext;
    }

    @ToString
    public static class ModuleActivation {
        public boolean activeByDefault = false;
    }
}
