package oap.metrics;

import oap.net.Inet;

import java.util.concurrent.TimeUnit;

/**
 * Copyright (c) 2015 Igor Petrenko <igor.petrenko@madberry.net>
 */
public class InfluxReporter {
    protected String host;
    protected int port;
    protected String database;
    protected String login;
    protected String password;

    private InfluxDBReporter reporter;

    public void start() {
        reporter = InfluxDBReporter.forRegistry( Metrics.registry )
            .withTag( "host", Inet.HOSTNAME )
            .convertRatesTo( TimeUnit.MINUTES )
            .convertDurationsTo( TimeUnit.MICROSECONDS )
            .withConnect( host, port, database, login, password )
            .build();
        reporter.start( 1, TimeUnit.MINUTES );
    }

    public void stop() {
        reporter.stop();
    }
}
