package oap.concurrent.scheduler;

import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.simpl.SimpleJobFactory;
import org.quartz.spi.TriggerFiredBundle;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Copyright (c) 2015 Igor Petrenko <igor.petrenko@madberry.net>
 */
public class FunctionJobFactory extends SimpleJobFactory {
    private ConcurrentHashMap<JobKey, RunnableJob> jobs = new ConcurrentHashMap<>();

    @Override
    public Job newJob( TriggerFiredBundle bundle, Scheduler scheduler ) throws SchedulerException {
        JobDetail jobDetail = bundle.getJobDetail();
        Job job = jobs.get( jobDetail.getKey() );
        return job != null ? job : super.newJob( bundle, scheduler );
    }

    public void register( JobDetail jobDetail, Runnable runnable ) {
        jobs.put( jobDetail.getKey(), new RunnableJob( runnable ) );
    }

    public void unregister( JobKey key ) {
        jobs.remove( key );
    }
}
