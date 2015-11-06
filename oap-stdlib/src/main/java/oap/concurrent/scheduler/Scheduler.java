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

package oap.concurrent.scheduler;

import org.quartz.CronScheduleBuilder;
import org.quartz.JobDetail;
import org.quartz.ScheduleBuilder;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;

import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

public class Scheduler {
    static final org.quartz.Scheduler scheduler;
    static final FunctionJobFactory jobFactory;
    private static final AtomicLong ids = new AtomicLong();

    static {
        try {
            final StdSchedulerFactory sf = new StdSchedulerFactory();
            Properties props = new Properties();
            props.setProperty( StdSchedulerFactory.PROP_SCHED_SKIP_UPDATE_CHECK, "true" );
            props.setProperty( StdSchedulerFactory.PROP_JOB_STORE_CLASS, "org.quartz.simpl.RAMJobStore" );
            props.setProperty( "org.quartz.threadPool.threadCount",
                String.valueOf( Runtime.getRuntime().availableProcessors() ) );
            sf.initialize( props );

            scheduler = sf.getScheduler();
            scheduler.setJobFactory( jobFactory = new FunctionJobFactory() );
            scheduler.start();
        } catch( org.quartz.SchedulerException e ) {
            throw new SchedulerException( e );
        }
    }

    private Scheduler() {
    }

    public static Scheduled scheduleWithFixedDelay( long delay, TimeUnit unit, Runnable runnable ) {
        return schedule( runnable,
            SimpleScheduleBuilder.simpleSchedule()
                .withIntervalInMilliseconds( unit.toMillis( delay ) )
                .repeatForever() );
    }

    private static Scheduled schedule( Runnable runnable, ScheduleBuilder<?> scheduleBuilder ) {
        try {
            String identity = identity( runnable );

            JobDetail job = newJob( RunnableJob.class )
                .withIdentity( identity + "/job" )
                .storeDurably()
                .build();

            Trigger trigger = newTrigger()
                .withIdentity( identity + "/trigger" )
                .withSchedule( scheduleBuilder )
                .build();

            jobFactory.register( job, runnable );

            scheduler.scheduleJob( job, trigger );
            return new Scheduled( job );
        } catch( org.quartz.SchedulerException e ) {
            throw new SchedulerException( e );
        }
    }

    public static Scheduled scheduleCron( String cron, Runnable runnable ) {
        return schedule( runnable, CronScheduleBuilder.cronSchedule( cron ) );
    }

    private static String identity( Runnable runnable ) {
        return runnable.getClass().getName() + "/" + ids.incrementAndGet();
    }
}
