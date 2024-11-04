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
import oap.application.ApplicationConfiguration;
import oap.application.KernelHelper;
import oap.concurrent.Executors;
import oap.util.BiStream;
import oap.util.Dates;
import oap.util.Throwables;
import org.joda.time.DateTimeUtils;

import java.io.Closeable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
public class Supervisor {
    private final LinkedHashMap<String, StartableService> supervised = new LinkedHashMap<>();
    private final LinkedHashMap<String, WrapperService<?>> wrappers = new LinkedHashMap<>();

    private boolean stopped = false;

    private static void runAndDetectTimeout( String name, ShutdownConfiguration shutdownConfiguration, Runnable func ) {
        long serviceTimeout = shutdownConfiguration.shutdown.serviceTimeout;
        if( serviceTimeout > 0 ) {
            long start = DateTimeUtils.currentTimeMillis();
            Future<?> future = shutdownConfiguration.submit( func );
            try {
                future.get( serviceTimeout, TimeUnit.MILLISECONDS );
            } catch( InterruptedException e ) {
                log.trace( e.getMessage() );
            } catch( ExecutionException e ) {
                throw Throwables.propagate( e );
            } catch( TimeoutException e ) {
                log.warn( "APP_TIMEOUT_START service {} after {}", name, Dates.durationToString( serviceTimeout ) );

                try {
                    if( !shutdownConfiguration.shutdown.serviceAsyncShutdownAfterTimeout ) {
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

    public synchronized void startThread( String name, Object instance, ApplicationConfiguration.ModuleShutdown shutdown ) {
        this.wrappers.put( name, new ThreadService( name, ( Runnable ) instance, this, shutdown ) );
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

    public synchronized void preStop( ApplicationConfiguration.ModuleShutdown shutdown ) {
        if( !stopped ) {
            try( ShutdownConfiguration shutdownConfiguration = new ShutdownConfiguration( shutdown ) ) {
                log.debug( "pre stopping..." );

                BiStream.of( this.wrappers )
                    .reversed()
                    .forEach( ( name, service ) -> {
                        Runnable func = () -> {
                            log.debug( "[{}] pre stopping {}...", service.type(), name );
                            KernelHelper.setThreadNameSuffix( name );
                            try {
                                service.preStop();
                            } finally {
                                KernelHelper.restoreThreadName();
                            }
                            log.debug( "[{}] pre stopping {}... Done.", service.type(), name );
                        };

                        runAndDetectTimeout( name, shutdownConfiguration, func );
                    } );

                BiStream.of( this.supervised )
                    .reversed()
                    .forEach( ( name, service ) -> {
                        Runnable func = () -> {
                            log.debug( "pre stopping {}...", name );
                            KernelHelper.setThreadNameSuffix( name );
                            try {
                                service.preStop();
                            } finally {
                                KernelHelper.restoreThreadName();
                            }
                            log.debug( "pre stopping {}... Done.", name );
                        };

                        runAndDetectTimeout( name, shutdownConfiguration, func );
                    } );
            }
        }
    }

    public synchronized void stop( ApplicationConfiguration.ModuleShutdown shutdown ) {
        if( !stopped ) {
            try( ShutdownConfiguration shutdownConfiguration = new ShutdownConfiguration( shutdown ) ) {
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

    public synchronized void stop( String serviceName, ApplicationConfiguration.ModuleShutdown shutdown ) {
        if( !stopped ) {
            log.debug( "stopping..." );
            this.stopped = true;

            try( ShutdownConfiguration shutdownConfiguration = new ShutdownConfiguration( shutdown ) ) {
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
        public final ExecutorService threadPoolExecutor;
        private final ApplicationConfiguration.ModuleShutdown shutdown;

        public ShutdownConfiguration( ApplicationConfiguration.ModuleShutdown shutdown ) {
            this.shutdown = shutdown;

            threadPoolExecutor = shutdown.serviceTimeout > 0 ? Executors.newCachedThreadPool() : null;
        }

        @Override
        public void close() {
            if( threadPoolExecutor != null ) {
                threadPoolExecutor.shutdown();
            }
        }

        public Future<?> submit( Runnable func ) {
            if( threadPoolExecutor != null ) {
                return threadPoolExecutor.submit( func );
            } else {
                func.run();
                return CompletableFuture.completedFuture( null );
            }
        }
    }
}
