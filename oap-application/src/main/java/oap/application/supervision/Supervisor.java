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
import oap.util.BiStream;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

//@todo per module supervisor hierarchy
//@todo restart policy

@Slf4j
public class Supervisor {

    private LinkedHashMap<String, Supervised> supervised = new LinkedHashMap<>();
    private LinkedHashMap<String, Supervised> scheduled = new LinkedHashMap<>();
    private boolean stopped = false;

    public void startSupervised( String name, Object service, String startWith, List<String> stopWith, String reloadWith ) {
        this.supervised.put( name, new StartableService( service, startWith, stopWith, reloadWith ) );
    }

    public void startThread( String name, Object instance ) {
        this.supervised.put( name, new ThreadService( name, ( Runnable ) instance, this ) );
    }

    public void scheduleWithFixedDelay( String name, Runnable service, long delay, TimeUnit unit ) {
        this.scheduled.put( name, new DelayScheduledService( service, delay, unit ) );
    }

    public void scheduleCron( String name, Runnable service, String cron ) {
        this.scheduled.put( name, new CronScheduledService( service, cron ) );
    }

    public synchronized void start() {
        log.debug( "starting..." );
        this.stopped = false;
        this.supervised.forEach( ( name, service ) -> {
            log.debug( "starting {}...", name );
            long start = System.currentTimeMillis();
            service.start();
            long end = System.currentTimeMillis();
            log.debug( "starting {}... Done. ({}ms)", name, end - start );
        } );

        this.scheduled.forEach( ( name, service ) -> {
            log.debug( "schedule {}...", name );
            long start = System.currentTimeMillis();
            service.start();
            long end = System.currentTimeMillis();
            log.debug( "schedule {}... Done. ({}ms)", name, end - start );
        } );
    }

    public synchronized void reload() {
        this.supervised.forEach( ( name, service ) -> {
            log.debug( "reloading {}...", name );
            service.reload();
            log.debug( "reloaded {}", name );
        } );

        this.scheduled.forEach( ( name, service ) -> {
            log.debug( "reloading {}...", name );
            service.reload();
            log.debug( "reloaded {}", name );
        } );
    }

    public synchronized void stop() {
        if( !stopped ) {
            log.debug( "stopping..." );
            this.stopped = true;
            BiStream.of( this.scheduled )
                .reversed()
                .forEach( ( name, service ) -> {
                    log.debug( "stopping {}...", name );
                    service.stop();
                } );
            this.scheduled.clear();
            BiStream.of( this.supervised )
                .reversed()
                .forEach( ( name, service ) -> {
                    log.debug( "stopping {}...", name );
                    service.stop();
                } );
            this.supervised.clear();
        }
    }

    public synchronized void stop( String serviceName ) {
        if( !stopped ) {
            log.debug( "stopping..." );
            this.stopped = true;
            BiStream.of( this.scheduled )
                .filter( s -> s._1.equals( serviceName ) )
                .forEach( ( name, service ) -> {
                    log.debug( "stopping {}...", name );
                    service.stop();
                    log.debug( "stopped {}", name );
                } );
            this.scheduled.clear();
            BiStream.of( this.supervised )
                .filter( s -> s._1.equals( serviceName ) )
                .forEach( ( name, service ) -> {
                    log.debug( "stopping {}...", name );
                    service.stop();
                    log.debug( "stopped {}", name );
                } );
        }
    }
}
