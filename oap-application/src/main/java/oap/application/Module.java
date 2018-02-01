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

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import oap.application.remote.RemoteLocation;
import oap.reflect.Coercions;
import oap.util.Strings;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import static java.util.Arrays.asList;

@EqualsAndHashCode
@ToString
public class Module {
    public static final String DEFAULT = Strings.DEFAULT;
    public static final ModuleConfiguration CONFIGURATION = new ModuleConfiguration();
    @SuppressWarnings( "unchecked" )
    static final Coercions coersions = Coercions.basic().withIdentity();
    @JsonAlias( { "name", "%name" } )
    public String name;
    @JsonAlias( { "dependsOn", "%dependsOn" } )
    public ArrayList<String> dependsOn = new ArrayList<>();
    @JsonAlias( { "extends", "%extends" } )
    public ArrayList<String> extendsModules = new ArrayList<>();
    @JsonAlias( { "abstract", "%abstract" } )
    public boolean isAbstract = false;
    @JsonAlias( { "services", "%services" } )
    public LinkedHashMap<String, Service> services = new LinkedHashMap<>();

    @JsonCreator
    public Module( String name ) {
        this.name = name;
    }

    @EqualsAndHashCode()
    @ToString()
    @Slf4j
    public static class Service {
        public String implementation;
        public LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
        public Supervision supervision = new Supervision();
        public ArrayList<String> dependsOn = new ArrayList<>();
        public String profile;
        public String name;
        public LinkedHashMap<String, String> listen = new LinkedHashMap<>();
        public RemoteLocation remote;
        public boolean enabled = true;

        @JsonIgnore
        public boolean isRemoteService() {
            return remote != null;
        }

    }

    @EqualsAndHashCode
    @ToString
    public static class Supervision {
        public boolean supervise;
        public boolean thread;
        public boolean schedule;
        public String startWith = "start";
        public List<String> stopWith = asList( "stop", "close" );
        public String reloadWith = "reload";
        public long delay; //ms
        public String cron; // http://www.quartz-scheduler.org/documentation/quartz-2.x/tutorials/crontrigger
    }

}
