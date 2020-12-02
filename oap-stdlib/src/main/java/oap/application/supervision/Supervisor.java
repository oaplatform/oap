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
package oap.application.supervision;

import lombok.extern.slf4j.Slf4j;
import oap.application.KernelHelper;
import oap.util.BiStream;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
public class Supervisor {

    private final LinkedHashMap<String, Supervised> services = new LinkedHashMap<>();
//    private LinkedHashMap<String, Supervised> scheduled = new LinkedHashMap<>();
    private boolean stopped = false;

    public void startSupervised( String name, Object service,
                                 List<String> preStartWith, List<String> startWith,
                                 List<String> preStopWith, List<String> stopWith ) {
        this.services.put( name, new StartableService( service, preStartWith, startWith, preStopWith, stopWith ) );
    }

    public void startThread( String name, Object instance ) {
        this.services.put( name, new ThreadService( name, ( Runnable ) instance, this ) );
    }

    public void scheduleWithFixedDelay( String name, Runnable service, long delay, TimeUnit unit ) {
        this.services.put( name, new DelayScheduledService( service, delay, unit ) );
    }

    public void scheduleCron( String name, Runnable service, String cron ) {
        this.services.put( name, new CronScheduledService( service, cron ) );
    }

    public synchronized void preStart() {
        log.debug( "pre starting..." );

        this.services.forEach( ( name, service ) -> {
            log.debug( "pre starting {}...", name );
            KernelHelper.setThreadNameSuffix( name );
            try {
                service.preStart();
            } finally {
                KernelHelper.restoreThreadName();
            }
        } );
    }

    public synchronized void start() {
        log.debug( "starting..." );
        this.stopped = false;
        this.services.forEach( ( name, service ) -> {
            log.debug( "starting {}...", name );
            long start = System.currentTimeMillis();
            KernelHelper.setThreadNameSuffix( name );
            try {
                service.start();
            } finally {
                KernelHelper.restoreThreadName();
            }
            long end = System.currentTimeMillis();
            log.debug( "starting {}... Done. ({}ms)", name, end - start );
        } );

//        this.scheduled.forEach( ( name, service ) -> {
//            log.debug( "schedule {}...", name );
//            long start = System.currentTimeMillis();
//            KernelHelper.setThreadNameSuffix( name );
//            try {
//                service.start();
//            } finally {
//                KernelHelper.restoreThreadName();
//            }
//            long end = System.currentTimeMillis();
//            log.debug( "schedule {}... Done. ({}ms)", name, end - start );
//        } );
    }

    public synchronized void preStop() {
        if( !stopped ) {
            log.debug( "pre stopping..." );

//            BiStream.of( this.scheduled )
//                .reversed()
//                .forEach( ( name, service ) -> {
//                    log.debug( "pre stopping {}...", name );
//                    KernelHelper.setThreadNameSuffix( name );
//                    try {
//                        service.preStop();
//                    } finally {
//                        KernelHelper.restoreThreadName();
//                    }
//                    log.debug( "pre stopping {}... Done.", name );
//                } );
//            this.scheduled.clear();

            BiStream.of( this.services )
                .reversed()
                .forEach( ( name, service ) -> {
                    log.debug( "pre stopping {}...", name );
                    KernelHelper.setThreadNameSuffix( name );
                    try {
                        service.preStop();
                    } finally {
                        KernelHelper.restoreThreadName();
                    }
                    log.debug( "pre stopping {}... Done.", name );
                } );
        }
    }

    public synchronized void stop() {
        if( !stopped ) {
            log.debug( "stopping..." );
            this.stopped = true;

            BiStream.of( this.services )
                .reversed()
                .forEach( ( name, service ) -> {
                    log.debug( "stopping {}...", name );
                    KernelHelper.setThreadNameSuffix( name );
                    try {
                        service.stop();
                    } finally {
                        KernelHelper.restoreThreadName();
                    }
                    log.debug( "stopping {}... Done.", name );
                } );
            this.services.clear();
        }
    }

    public synchronized void stop( String serviceName ) {
        if( !stopped ) {
            log.debug( "stopping..." );
            this.stopped = true;

//            BiStream.of( this.scheduled )
//                .filter( s -> s._1.equals( serviceName ) )
//                .forEach( ( name, service ) -> {
//                    log.debug( "stopping {}...", name );
//                    KernelHelper.setThreadNameSuffix( name );
//                    try {
//                        service.preStop();
//                        service.stop();
//                    } finally {
//                        KernelHelper.restoreThreadName();
//                    }
//                    log.debug( "stopping {}... Done.", name );
//                } );
//            this.scheduled.clear();

            BiStream.of( this.services )
                .filter( s -> s._1.equals( serviceName ) )
                .forEach( ( name, service ) -> {
                    log.debug( "stopping {}...", name );
                    KernelHelper.setThreadNameSuffix( name );
                    try {
                        service.preStop();
                        service.stop();
                    } finally {
                        KernelHelper.restoreThreadName();
                    }
                    log.debug( "stopping {}... Done.", name );
                } );
        }
    }
}
