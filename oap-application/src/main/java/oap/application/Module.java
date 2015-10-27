/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Volodymyr Kyrychenko <vladimir.kirichenko@gmail.com>
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
import lombok.ToString;
import oap.io.Files;
import oap.io.Resources;
import oap.json.Binder;
import oap.util.Strings;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@EqualsAndHashCode
@ToString
public class Module {
    public String name;
    public ArrayList<String> dependsOn = new ArrayList<>();
    public ArrayList<Service> services = new ArrayList<>();

    public Module( String name, ArrayList<String> dependsOn, ArrayList<Service> services ) {
        this.name = name;
        this.dependsOn = dependsOn;
        this.services = services;
    }

    public Module() {
    }

    public static List<URL> fromClassPath() {
        return Resources.urls( "META-INF/oap-module.json" );
    }

    public static Module parse( Path path ) throws IOException {
        return parse( Files.readString( path ) );
    }

    public static Module parse( URL url ) {
        return parse( Strings.readString( url ) );
    }

    public static Module parse( String json ) {
        return Binder.unmarshal( Module.class, Strings.substitute( json,
            key -> key.startsWith( "env:" ) ? System.getenv( key.substring( 4 ) ) : System.getProperty( key ) ) );
    }

    @EqualsAndHashCode
    @ToString
    public static class Service {
        public String name;
        public String implementation;
        public LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
        public Supervision supervision = new Supervision();
        public ArrayList<String> dependsOn = new ArrayList<>();

        public Service() {
        }

        public Service( String name, String implementation, Map<String, Object> parameters, Supervision supervision,
            ArrayList<String> dependsOn ) {
            this.name = name;
            this.implementation = implementation;
            this.dependsOn = dependsOn;
            this.parameters = new LinkedHashMap<>( parameters );
            this.supervision = supervision;
        }

    }

    @EqualsAndHashCode
    @ToString
    public static class Supervision {
        public boolean supervise;
        public boolean thread;
        public boolean schedule;
        public long delay; //seconds
        public String cron; // http://www.quartz-scheduler.org/documentation/quartz-2.x/tutorials/crontrigger

        public Supervision() {
        }

        public Supervision( boolean supervise ) {
            this.supervise = supervise;
        }
    }
}
