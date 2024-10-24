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

import io.micrometer.core.instrument.util.NamedThreadFactory;
import lombok.extern.slf4j.Slf4j;
import oap.application.KernelHelper;
import oap.concurrent.Executors;
import oap.concurrent.ThreadPoolExecutor;
import oap.util.BiStream;
import oap.util.Dates;
import oap.util.Throwables;
import org.joda.time.DateTimeUtils;

import java.io.Closeable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
public class Supervisor {
    private final LinkedHashMap<String, StartableService> supervised = new LinkedHashMap<>();
    private final LinkedHashMap<String, WrapperService<?>> wrappers = new LinkedHashMap<>();

    private boolean stopped = false;

    private static void runAndDetectTimeout( String name, ShutdownConfiguration shutdownConfiguration, Runnable func ) {
        if( shutdownConfiguration.timeoutMs > 0 ) {
            long start = DateTimeUtils.currentTimeMillis();
            Future<?> future = shutdownConfiguration.submit( func );
            try {
                future.get( shutdownConfiguration.timeoutMs, TimeUnit.MILLISECONDS );
            } catch( InterruptedException e ) {
                log.trace( e.getMessage() );
            } catch( ExecutionException e ) {
                throw Throwables.propagate( e );
            } catch( TimeoutException e ) {
                log.warn( "APP_TIMEOUT_START service {} after {}", name, Dates.durationToString( shutdownConfiguration.timeoutMs ) );

                try {
                    if( !shutdownConfiguration.forceAsyncAfterTimeout ) {
                        future.get();

                        log.warn( "APP_TIMEOUT_END service {} done in {}", name, Dates.durationToString( DateTimeUtils.currentTimeMillis() - start ) );
                    } else {
                        log.warn( "APP_TIMEOUT_IGNORE service {}", name );
                    }
                } catch( InterruptedException ex ) {
                    log.trace( e.getMessage() );
                } catch( ExecutionException ex ) {
                    throw Throwables.propagate( e );
                }
            }
        } else {
            func.run();
        }
    }

    public synchronized void startSupervised( String name, Object service,
                                              List<String> preStartWith, List<String> startWith,
                                              List<String> preStopWith, List<String> stopWith ) {
        this.supervised.put( name, new StartableService( service, preStartWith, startWith, preStopWith, stopWith ) );
    }

//    public synchronized void startScheduledThread( String name, Object instance, long delay, TimeUnit milliseconds ) {
//        this.wrappers.put( name, new ThreadService( name, ( Runnable ) instance, this ) );
//    }

    public synchronized void startThread( String name, Object instance ) {
        this.wrappers.put( name, new ThreadService( name, ( Runnable ) instance, this ) );
    }

    public synchronized void scheduleWithFixedDelay( String name, Runnable service, long delay, TimeUnit unit ) {
        this.wrappers.put( name, new DelayScheduledService( service, delay, unit ) );
    }

    public synchronized void scheduleCron( String name, Runnable service, String cron ) {
        this.wrappers.put( name, new CronScheduledService( service, cron ) );
    }

    public synchronized void preStart() {
        log.debug( "pre starting..." );

        this.supervised.forEach( ( name, service ) -> {
            log.debug( "pre starting {}...", name );
            KernelHelper.setThreadNameSuffix( name );
            try {
                service.preStart();
            } finally {
                KernelHelper.restoreThreadName();
            }
        } );

        BiStream.of( this.wrappers )
            .reversed()
            .forEach( ( name, service ) -> {
                log.debug( "[{}] pre starting {}...", service.type(), name );
                KernelHelper.setThreadNameSuffix( name );
                try {
                    service.preStart();
                } finally {
                    KernelHelper.restoreThreadName();
                }
                log.debug( "[{}] pre starting {}... Done.", service.type(), name );
            } );
    }

    public synchronized void start() {
        log.debug( "starting..." );
        this.stopped = false;
        this.supervised.forEach( ( name, service ) -> {
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

        this.wrappers.forEach( ( name, service ) -> {
            log.debug( "[{}] starting {}...", service.type(), name );
            long start = System.currentTimeMillis();
            KernelHelper.setThreadNameSuffix( name );
            try {
                service.start();
            } finally {
                KernelHelper.restoreThreadName();
            }
            long end = System.currentTimeMillis();
            log.debug( "[{}] starting {}... Done. ({}ms)", service.type(), name, end - start );
        } );
    }

    public synchronized void preStop() {
        if( !stopped ) {
            log.debug( "pre stopping..." );

            BiStream.of( this.wrappers )
                .reversed()
                .forEach( ( name, service ) -> {
                    log.debug( "[{}] pre stopping {}...", service.type(), name );
                    KernelHelper.setThreadNameSuffix( name );
                    try {
                        service.preStop();
                    } finally {
                        KernelHelper.restoreThreadName();
                    }
                    log.debug( "[{}] pre stopping {}... Done.", service.type(), name );
                } );

            BiStream.of( this.supervised )
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

            try( ShutdownConfiguration shutdownConfiguration = new ShutdownConfiguration() ) {
                log.debug( "stopping..." );
                this.stopped = true;

                BiStream.of( this.wrappers )
                    .reversed()
                    .forEach( ( name, service ) -> {
                        Runnable func = () -> {
                            log.debug( "[{}] stopping {}...", service.type(), name );
                            KernelHelper.setThreadNameSuffix( name );
                            try {
                                service.stop();
                            } finally {
                                KernelHelper.restoreThreadName();
                            }
                            log.debug( "[{}] stopping {}... Done.", service.type(), name );
                        };

                        runAndDetectTimeout( name, shutdownConfiguration, func );
                    } );
                this.wrappers.clear();

                BiStream.of( this.supervised )
                    .reversed()
                    .forEach( ( name, service ) -> {
                        Runnable func = () -> {
                            log.debug( "stopping {}...", name );
                            KernelHelper.setThreadNameSuffix( name );
                            try {
                                service.stop();
                            } finally {
                                KernelHelper.restoreThreadName();
                            }
                            log.debug( "stopping {}... Done.", name );
                        };

                        runAndDetectTimeout( name, shutdownConfiguration, func );
                    } );
                this.supervised.clear();
            }
        }
    }

    public synchronized void stop( String serviceName ) {
        if( !stopped ) {
            log.debug( "stopping..." );
            this.stopped = true;

            try( ShutdownConfiguration shutdownConfiguration = new ShutdownConfiguration() ) {
                BiStream.of( this.wrappers )
                    .filter( ( name, _ ) -> name.equals( serviceName ) )
                    .forEach( ( name, service ) -> {
                        Runnable func = () -> {
                            log.debug( "[{}] stopping {}...", service.type(), name );
                            KernelHelper.setThreadNameSuffix( name );
                            try {
                                service.preStop();
                                service.stop();
                            } finally {
                                KernelHelper.restoreThreadName();
                            }
                            log.debug( "[{}] stopping {}... Done.", service.type(), name );
                        };

                        runAndDetectTimeout( name, shutdownConfiguration, func );
                    } );
                this.wrappers.clear();

                BiStream.of( this.supervised )
                    .filter( ( name, service ) -> name.equals( serviceName ) )
                    .forEach( ( name, service ) -> {
                        Runnable func = () -> {
                            log.debug( "stopping {}...", name );
                            KernelHelper.setThreadNameSuffix( name );
                            try {
                                service.preStop();
                                service.stop();
                            } finally {
                                KernelHelper.restoreThreadName();
                            }
                            log.debug( "stopping {}... Done.", name );
                        };

                        runAndDetectTimeout( name, shutdownConfiguration, func );
                    } );
            }
        }
    }

    public static class ShutdownConfiguration implements Closeable {
        private static final Set<String> on = Set.of( "on", "1", "true", "ON", "TRUE", "yes", "YES" );
        public final long timeoutMs;
        public final boolean forceAsyncAfterTimeout;
        public final ThreadPoolExecutor threadPoolExecutor = Executors.newFixedBlockingThreadPool( 1, new NamedThreadFactory( "stop" ) );

        public ShutdownConfiguration() {
            String timeoutMsStr = System.getenv( "APPLICATION_STOP_DETECT_TIMEOUT" );
            this.timeoutMs = timeoutMsStr != null ? Long.parseLong( timeoutMsStr ) : Dates.s( 5 );

            String forceAsyncAfterTimeoutStr = System.getenv( "APPLICATION_FORCE_ASYNC_AFTER_TIMEOUT" );
            this.forceAsyncAfterTimeout = forceAsyncAfterTimeoutStr != null && on.contains( forceAsyncAfterTimeoutStr );
        }

        @Override
        public void close() {
            threadPoolExecutor.shutdown();
        }

        public Future<?> submit( Runnable func ) {
            return threadPoolExecutor.submit( func );
        }
    }
}
