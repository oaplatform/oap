package oap.concurrent.scheduler;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * Copyright (c) 2015 Igor Petrenko <igor.petrenko@madberry.net>
 */
@DisallowConcurrentExecution
public class RunnableJob implements Job {
    final Runnable runnable;

    public RunnableJob( Runnable runnable ) {
        this.runnable = runnable;
    }

    @Override
    public void execute( JobExecutionContext context ) throws JobExecutionException {
        try {
            runnable.run();
        } catch( Exception e ) {
            throw new JobExecutionException( e );
        }
    }

}
