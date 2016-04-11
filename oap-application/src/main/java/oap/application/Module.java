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
import lombok.ToString;

import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Optional;

@EqualsAndHashCode
@ToString
public class Module {
   public static final ModuleConfiguration CONFIGURATION = new ModuleConfiguration();
   public String name;
   public ArrayList<String> dependsOn = new ArrayList<>();
   public LinkedHashMap<String, Service> services = new LinkedHashMap<>();

   @EqualsAndHashCode
   @ToString
   public static class Service {
      public String implementation;
      public LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
      public Supervision supervision = new Supervision();
      public ArrayList<String> dependsOn = new ArrayList<>();
      public URI remoteUrl;
      public String remoteName;
      public Path certificateLocation;
      public String certificatePassword;
      public String profile;
      public String name;
      public Optional<Long> timeout = Optional.empty();
   }

   @EqualsAndHashCode
   @ToString
   public static class Supervision {
      public boolean supervise;
      public boolean thread;
      public boolean schedule;
      public String startWith = "start";
      public String stopWith = "stop";
      public long delay; //ms
      public String cron; // http://www.quartz-scheduler.org/documentation/quartz-2.x/tutorials/crontrigger
   }
}
